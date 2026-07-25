package com.example.demo;

import org.junit.jupiter.api.Disabled;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.test.context.ActiveProfiles;
import org.junit.jupiter.api.Test;

import org.springframework.boot.test.context.SpringBootTest;

import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@Disabled("Deshabilitado temporalmente por problemas de latencia con DB en CI/CD")
@ActiveProfiles("test")
class DemoApplicationTests {

	@MockBean
	private RedisConnectionFactory redisConnectionFactory;
	@MockBean
	private ConnectionFactory rabbitConnectionFactory;
	
	@Test
	void contextLoads() {
	}

}
