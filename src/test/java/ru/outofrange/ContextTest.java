package ru.outofrange;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
@SpringBootTest
public class ContextTest {

    @Test
    public void contextLoads() {
        try {
            Thread.sleep(100_000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}
