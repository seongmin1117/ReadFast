package com.baro13.readfast.admin.authlog.adapter.out.archive.storage;

import com.baro13.readfast.admin.authlog.domain.port.DataRetentionPolicyProvider;
import com.baro13.readfast.admin.policy.domain.model.vo.ArchivingStrategy.ArchiveFormat;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class StorageFactory {
    private final Map<ArchiveFormat, Storage> storageMap;
    private final DataRetentionPolicyProvider policyProvider;

    public StorageFactory(List<Storage> storages, DataRetentionPolicyProvider policyProvider) {
        this.policyProvider = policyProvider;
        this.storageMap = storages.stream()
            .collect(Collectors.toMap(Storage::getArchiveFormat, Function.identity()));
    }

    public Storage resolve() {
        var type = policyProvider.getCurrentPolicy().getArchivingStrategy().getArchiveFormat();
        return Optional.ofNullable(storageMap.get(type))
            .orElseThrow(() -> new IllegalStateException("지원하지 않는 스토리지 타입: " + type));
    }
}