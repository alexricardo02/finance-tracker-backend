package com.example.demo;

import com.example.Application;
import io.github.bucket4j.distributed.proxy.ProxyManager;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest(classes = Application.class)
@ActiveProfiles("test")
class DemoApplicationTests {

	@MockBean
	private RedisConnectionFactory redisConnectionFactory;

	@MockBean
	private ConnectionFactory rabbitConnectionFactory;

	@MockBean
	private ProxyManager<byte[]> proxyManager;

	@MockBean
	private JavaMailSender mailSender;

	@Test
	void contextLoads() {
	}
}
