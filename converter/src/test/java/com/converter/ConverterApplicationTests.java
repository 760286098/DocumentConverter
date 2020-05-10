package com.converter;

import com.converter.core.ConvertManager;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@Slf4j
@SpringBootTest
class ConverterApplicationTests {
    @Test
    void test() {
        log.info("开始");
        ConvertManager.addMissions("test", false);
        log.info("结束");
    }

    @AfterEach
    void after() throws InterruptedException {
        Thread.sleep(10000000);
    }
}
