package com.baro13.readfast.admin.authlog.adapter.out.archive.compression;

import com.baro13.readfast.admin.policy.domain.model.vo.ArchivingStrategy.CompressionType;
import java.io.IOException;

public interface Compression {
    byte[] compress(byte[] input) throws IOException;;
    byte[] decompress(byte[] input) throws IOException;;

    CompressionType getCompressionType();
}
