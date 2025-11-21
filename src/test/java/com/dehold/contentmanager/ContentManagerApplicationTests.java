package com.dehold.contentmanager;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Import(TestProfileConfig.class)
@ActiveProfiles("test")
public class ContentManagerApplicationTests {

	@Test
	void contextLoads() {
	}

}
