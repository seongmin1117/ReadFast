package com.baro13.readfast;

import com.baro13.readfast.config.TestConfig;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

@SpringBootTest
@Import(TestConfig.class)
class ReadFastApplicationTests {

    @Test
    void contextLoads() {
    }

}
