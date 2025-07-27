package com.baro13.readfast.admin.authlog.adapter.out.archive.compression;

import com.baro13.readfast.admin.policy.domain.model.vo.ArchivingStrategy.CompressionType;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class GzipCompression implements Compression{
    @Override
    public byte[] compress(byte[] data) throws IOException {
        try (ByteArrayOutputStream bos = new ByteArrayOutputStream();
            GZIPOutputStream gzip = new GZIPOutputStream(bos)) {
            gzip.write(data);
            gzip.finish();
            return bos.toByteArray();
        }
    }

    @Override
    public byte[] decompress(byte[] compressedData) throws IOException {
        try (ByteArrayInputStream bis = new ByteArrayInputStream(compressedData);
            GZIPInputStream gzip = new GZIPInputStream(bis)) {
            return gzip.readAllBytes();
        }
    }

    @Override
    public CompressionType getCompressionType() {
        return CompressionType.GZIP;
    }
}
