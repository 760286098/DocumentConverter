package com.converter;

import com.converter.core.ConvertManager;
import com.converter.pojo.ConvertInfo;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.HashMap;

@Slf4j
@SpringBootTest
class ConverterApplicationTests {
    @Test
    void test() throws InterruptedException {
        log.info("开始");
        ConvertManager.addMissions("C:\\Users\\Evan\\Desktop\\1", false);
//        Thread.sleep(30000);
        HashMap<ConvertInfo, Integer> map = ConvertManager.getAllConvertInfo();
        Thread.sleep(30000);
        ConvertManager.addMissions("C:\\Users\\Evan\\Desktop\\2", false);
        map = ConvertManager.getAllConvertInfo();
        String json = ConvertManager.getAllConvertInfoOfJson();
        log.warn(map.toString());
        log.info("结束");
    }

    @AfterEach
    void after() throws InterruptedException {
        Thread.sleep(10000000);
    }
}
