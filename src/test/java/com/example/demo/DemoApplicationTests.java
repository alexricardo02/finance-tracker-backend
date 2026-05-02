package com.example.demo;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import org.springframework.boot.test.context.SpringBootTest;

import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
@Disabled("Deshabilitado temporalmente por problemas de latencia con DB en CI/CD")
class DemoApplicationTests {

	@Test
	void contextLoads() {
	}

}
