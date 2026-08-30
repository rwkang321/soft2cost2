package com.soft2cost2;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import static org.junit.jupiter.api.Assertions.assertNotNull;

@SpringBootTest
@ActiveProfiles("test")
class Soft2cost2ApplicationTests {

    @Test
    void applicationEntryPointClassIsAvailable() {
        assertNotNull(Soft2cost2Application.class);
    }

}
