package com.dehold.contentmanager;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Import(TestProfileConfig.class)
public class ContentManagerApplicationTests {

	@Test
	void contextLoads() {
	}

}
