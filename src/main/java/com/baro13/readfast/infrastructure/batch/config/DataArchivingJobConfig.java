package com.baro13.readfast.infrastructure.batch.config;

import com.baro13.readfast.infrastructure.batch.service.DataArchivingService;
import com.baro13.readfast.infrastructure.db.jpa.AuthLogEntity;
import com.baro13.readfast.infrastructure.policy.DataRetentionProperties;
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
import org.springframework.batch.item.support.ListItemReader;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.Page;
import org.springframework.transaction.PlatformTransactionManager;

import java.util.ArrayList;
import java.util.List;

/**
 * 데이터 아카이빙 배치 작업 설정
 * 비즈니스 로직은 DataArchivingService에 위임
 */
@Slf4j
@Configuration
@RequiredArgsConstructor
public class DataArchivingJobConfig {
    
    private final JobRepository jobRepository;
    private final PlatformTransactionManager transactionManager;
    private final DataArchivingService dataArchivingService;
    private final DataRetentionProperties properties;
    
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
                .<AuthLogEntity, AuthLogEntity>chunk(properties.getBatchSize(), transactionManager)
                .reader(archiveItemReader())
                .processor(archiveItemProcessor())
                .writer(archiveItemWriter())
                .build();
    }
    
    @Bean
    public Step convertToSQLiteStep() {
        return new StepBuilder("convertToSQLiteStep", jobRepository)
                .tasklet((contribution, chunkContext) -> {
                    dataArchivingService.convertToSQLite();
                    return org.springframework.batch.repeat.RepeatStatus.FINISHED;
                }, transactionManager)
                .build();
    }
    
    @Bean
    public Step cleanupStep() {
        return new StepBuilder("cleanupStep", jobRepository)
                .<AuthLogEntity, AuthLogEntity>chunk(properties.getBatchSize(), transactionManager)
                .reader(cleanupItemReader())
                .processor(cleanupItemProcessor())
                .writer(cleanupItemWriter())
                .build();
    }
    
    @Bean
    public ItemReader<AuthLogEntity> archiveItemReader() {
        return new PagingItemReader<>(dataArchivingService);
    }
    
    @Bean
    public ItemProcessor<AuthLogEntity, AuthLogEntity> archiveItemProcessor() {
        return item -> item; // 단순 통과 (비즈니스 로직은 Writer에서 처리)
    }
    
    @Bean
    public ItemWriter<AuthLogEntity> archiveItemWriter() {
        return items -> {
            if (!items.isEmpty()) {
                List<AuthLogEntity> itemList = new ArrayList<>(items.getItems());
                dataArchivingService.archiveData(itemList);
            }
        };
    }
    
    @Bean
    public ItemReader<AuthLogEntity> cleanupItemReader() {
        return new PagingItemReader<>(dataArchivingService);
    }
    
    @Bean
    public ItemProcessor<AuthLogEntity, AuthLogEntity> cleanupItemProcessor() {
        return item -> item;
    }
    
    @Bean
    public ItemWriter<AuthLogEntity> cleanupItemWriter() {
        return items -> {
            if (!items.isEmpty()) {
                List<AuthLogEntity> itemList = new ArrayList<>(items.getItems());
                dataArchivingService.cleanupArchivedData(itemList);
            }
        };
    }
    
    /**
     * 페이징 기반 커스텀 ItemReader
     */
    private static class PagingItemReader<T> implements ItemReader<T> {
        private final DataArchivingService service;
        private ListItemReader<T> currentPageReader;
        private int currentPage = 0;
        private boolean hasMorePages = true;
        
        public PagingItemReader(DataArchivingService service) {
            this.service = service;
        }
        
        @Override
        public T read() throws Exception {
            if (currentPageReader == null || currentPageReader.read() == null) {
                if (!hasMorePages) {
                    return null;
                }
                
                loadNextPage();
                if (currentPageReader == null) {
                    return null;
                }
            }
            
            return currentPageReader.read();
        }
        
        @SuppressWarnings("unchecked")
        private void loadNextPage() {
            var pageable = org.springframework.data.domain.PageRequest.of(
                currentPage, 
                service.createDefaultPageable().getPageSize(),
                service.createDefaultPageable().getSort()
            );
            
            Page<AuthLogEntity> page = service.getArchiveTargetData(pageable);
            
            if (page.hasContent()) {
                currentPageReader = new ListItemReader<>((java.util.List<T>) page.getContent());
                currentPage++;
                hasMorePages = page.hasNext();
            } else {
                hasMorePages = false;
                currentPageReader = null;
            }
        }
    }
}