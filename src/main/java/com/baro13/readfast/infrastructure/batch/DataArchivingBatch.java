package com.baro13.readfast.infrastructure.batch;

import com.baro13.readfast.domain.AuthLog;
import com.baro13.readfast.infrastructure.AuthLogMapper;
import com.baro13.readfast.infrastructure.batch.config.DataRetentionProperties;
import com.baro13.readfast.infrastructure.jpa.AuthLogEntity;
import com.baro13.readfast.infrastructure.jpa.AuthQueryJpaRepository;
import com.baro13.readfast.infrastructure.storage.StorageService;
import com.baro13.readfast.infrastructure.storage.AnalyticsStorageFactory;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.batch.item.ItemReader;
import org.springframework.batch.item.ItemWriter;
import org.springframework.batch.item.data.builder.RepositoryItemReaderBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.Sort;
import org.springframework.transaction.PlatformTransactionManager;

@Slf4j
@Configuration
@RequiredArgsConstructor
public class DataArchivingBatch {
    
    private final JobRepository jobRepository;
    private final PlatformTransactionManager transactionManager;
    private final AuthQueryJpaRepository authQueryJpaRepository;
    private final StorageService storageService;
    private final DataRetentionProperties properties;
    private final AnalyticsStorageFactory analyticsStorageFactory;
    
    @Bean
    public Job dataArchivingJob() {
        return new JobBuilder("dataArchivingJob", jobRepository)
                .start(archiveStep())
                .next(convertToSQLiteStep())
                .next(cleanupStep())
                .build();
    }
    
    @Bean
    public Step archiveStep() {
        return new StepBuilder("archiveStep", jobRepository)
                .<AuthLogEntity, AuthLog>chunk(properties.getBatchSize(), transactionManager)
                .reader(archiveItemReader())
                .processor(archiveItemProcessor())
                .writer(archiveItemWriter())
                .build();
    }
    
    @Bean
    public Step convertToSQLiteStep() {
        return new StepBuilder("convertToSQLiteStep", jobRepository)
                .tasklet((contribution, chunkContext) -> {
                    LocalDate cutoffDate = LocalDate.now().minusDays(properties.getDbRetentionDays());
                    LocalDate endDate = LocalDate.now().minusDays(1);
                    
                    if (properties.isEnableSqliteConversion()) {
                        // 압축 파일을 SQLite로 변환
                        cutoffDate.datesUntil(endDate.plusDays(1))
                                .forEach(date -> {
                                    try {
                                        java.nio.file.Path compressedFilePath = getCompressedFilePath(date);
                                        if (java.nio.file.Files.exists(compressedFilePath)) {
                                            analyticsStorageFactory.convertCompressedFile(compressedFilePath, date);
                                        }
                                    } catch (Exception e) {
                                        log.error("{}일자 SQLite 변환 실패", date, e);
                                    }
                                });
                        
                        // 분석용 통합 SQLite DB 생성
                        analyticsStorageFactory.createAnalyticsDatabase(cutoffDate, endDate);
                        
                        log.info("SQLite 변환 및 통합 DB 생성 완료: {} ~ {}", cutoffDate, endDate);
                    } else {
                        log.info("SQLite 변환이 비활성화되어 있습니다.");
                    }
                    
                    return org.springframework.batch.repeat.RepeatStatus.FINISHED;
                }, transactionManager)
                .build();
    }
    
    @Bean
    public Step cleanupStep() {
        return new StepBuilder("cleanupStep", jobRepository)
                .<AuthLogEntity, AuthLogEntity>chunk(properties.getBatchSize(), transactionManager)
                .reader(cleanupItemReader())
                .writer(cleanupItemWriter())
                .build();
    }
    
    @Bean
    public ItemReader<AuthLogEntity> archiveItemReader() {
        Instant cutoffDate = Instant.now().minus(properties.getDbRetentionDays(), ChronoUnit.DAYS);
        
        return new RepositoryItemReaderBuilder<AuthLogEntity>()
                .name("archiveItemReader")
                .repository(authQueryJpaRepository)
                .methodName("findByDateBefore")
                .arguments(cutoffDate)
                .sorts(Collections.singletonMap("date", Sort.Direction.ASC))
                .pageSize(properties.getBatchSize())
                .build();
    }
    
    @Bean
    public ItemProcessor<AuthLogEntity, AuthLog> archiveItemProcessor() {
        return AuthLogMapper::toDomain;
    }
    
    @Bean
    public ItemWriter<AuthLog> archiveItemWriter() {
        return items -> {
            if (items.isEmpty()) {
                return;
            }
            
            List<AuthLog> itemList = new ArrayList<>();
            items.forEach(itemList::add);
            
            // 날짜별로 그룹화
            Map<LocalDate, List<AuthLog>> groupedByDate = itemList.stream()
                    .collect(java.util.stream.Collectors.groupingBy(
                            authLog -> authLog.getDate().atZone(ZoneId.systemDefault()).toLocalDate()
                    ));
            
            // 각 날짜별로 저장
            groupedByDate.forEach((date, authLogs) -> {
                try {
                    storageService.storeData(authLogs, date);
                    log.info("{}일자 {}개 레코드 아카이빙 완료", date, authLogs.size());
                } catch (Exception e) {
                    log.error("{}일자 데이터 아카이빙 실패", date, e);
                    throw new RuntimeException("데이터 아카이빙 실패", e);
                }
            });
        };
    }
    
    @Bean
    public ItemReader<AuthLogEntity> cleanupItemReader() {
        if (!properties.isEnableDataDeletion()) {
            return new EmptyItemReader<>();
        }
        
        Instant cutoffDate = Instant.now().minus(properties.getDbRetentionDays(), ChronoUnit.DAYS);
        
        return new RepositoryItemReaderBuilder<AuthLogEntity>()
                .name("cleanupItemReader")
                .repository(authQueryJpaRepository)
                .methodName("findByDateBefore")
                .arguments(cutoffDate)
                .sorts(Collections.singletonMap("date", Sort.Direction.ASC))
                .pageSize(properties.getBatchSize())
                .build();
    }
    
    @Bean
    public ItemWriter<AuthLogEntity> cleanupItemWriter() {
        return items -> {
            if (items.isEmpty()) {
                return;
            }
            
            authQueryJpaRepository.deleteAll(items);
            log.info("데이터베이스에서 이전 레코드 {}  삭제 완료", items.size());
        };
    }
    
    private static class EmptyItemReader<T> implements ItemReader<T> {
        @Override
        public T read() {
            return null;
        }
    }
    
    private java.nio.file.Path getCompressedFilePath(LocalDate date) {
        String dateStr = date.format(java.time.format.DateTimeFormatter.ofPattern(properties.getArchiveFileFormat()));
        String extension = getCompressedFileExtension();
        String fileName = dateStr + extension;
        return java.nio.file.Paths.get(properties.getArchiveBasePath(), fileName);
    }
    
    private String getCompressedFileExtension() {
        String dataFormat = properties.getArchiveDataFormat();
        boolean isCompressed = properties.isEnableCompression() && "gzip".equals(properties.getCompressionFormat());
        
        if ("csv".equals(dataFormat)) {
            return isCompressed ? ".csv.gz" : ".csv";
        } else if ("json".equals(dataFormat)) {
            return isCompressed ? ".json.gz" : ".json";
        }
        return isCompressed ? ".json.gz" : ".json";
    }
}