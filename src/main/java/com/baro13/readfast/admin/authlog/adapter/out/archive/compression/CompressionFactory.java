package com.baro13.readfast.admin.authlog.adapter.out.archive.compression;

import com.baro13.readfast.admin.authlog.domain.port.DataRetentionPolicyProvider;
import com.baro13.readfast.admin.policy.domain.model.vo.ArchivingStrategy.CompressionType;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class CompressionFactory {

    private final Map<CompressionType, Compression> compressionMap;
    private final DataRetentionPolicyProvider policyProvider;

    public CompressionFactory(List<Compression> compressions, DataRetentionPolicyProvider policyProvider) {
        this.policyProvider = policyProvider;
        this.compressionMap = compressions.stream()
            .collect(Collectors.toMap(Compression::getCompressionType, Function.identity()));
    }

    public Compression resolve() {
        var type = policyProvider.getCurrentPolicy().getArchivingStrategy().getCompressionType();
        return Optional.ofNullable(compressionMap.get(type))
            .orElseThrow(() -> new IllegalStateException("지원하지 않는 압축 방법: " + type));
    }

}
