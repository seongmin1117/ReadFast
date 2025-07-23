package com.baro13.readfast.admin.authlog.adapter.out.archive;

import com.baro13.readfast.admin.authlog.adapter.out.archive.storage.DataStorage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthLogArchiveStorage {
    private final DataStorage dataStorage;
}