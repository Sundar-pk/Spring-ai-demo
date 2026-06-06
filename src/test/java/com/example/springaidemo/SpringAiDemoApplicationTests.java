package com.example.springaidemo;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

/**
 * Basic context load test — verifies the Spring application wires up correctly.
 * LLM calls are skipped in test by pointing at a non-existent URL.
 */
@SpringBootTest
@TestPropertySource(properties = {
    "spring.ai.openai.base-url=http://localhost:9999",  // no real LLM needed for context load
    "spring.ai.openai.api-key=test-key"
})
class SpringAiDemoApplicationTests {

    @Test
    void contextLoads() {
        // Verifies all beans wire up correctly without calling the LLM
    }
}
