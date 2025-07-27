package com.baro13.readfast.admin.authlog.adapter.out.archive.compression;

import com.baro13.readfast.admin.policy.domain.model.vo.ArchivingStrategy.CompressionType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 압축하지 않는 압축 구현체
 * 압축이 비활성화된 경우 사용됨
 */
@Slf4j
@Component
public class NoCompression implements Compression {

    @Override
    public byte[] compress(byte[] input) {
        log.debug("압축 없음 모드: {} bytes 원본 반환", input.length);
        return input;
    }

    @Override
    public byte[] decompress(byte[] input) {
        log.debug("압축 없음 모드: {} bytes 원본 반환", input.length);
        return input;
    }

    @Override
    public CompressionType getCompressionType() {
        return CompressionType.NONE;
    }
}