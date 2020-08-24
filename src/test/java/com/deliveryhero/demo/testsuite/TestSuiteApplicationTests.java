package com.deliveryhero.demo.testsuite;

import com.deliveryhero.demo.testsuite.util.db.PostgresUtil;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.testcontainers.containers.KafkaContainer;
import org.testcontainers.containers.PostgreSQLContainer;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest
class TestSuiteApplicationTests {
	@Autowired
	private PostgresUtil postgresUtil;

	@Rule
	public KafkaContainer kafka  = new KafkaContainer();

	@Rule
	public PostgreSQLContainer postgres = new PostgreSQLContainer()
			.withDatabaseName("integration-tests-db")
			.withUsername("sa")
			.withPassword("sa");

	@BeforeEach
	public void setUp() {
		kafka.start();
		postgres.start();

		//create db table
		postgresUtil.init(
				postgres.getJdbcUrl(),
				postgres.getUsername(),
				postgres.getPassword()
		);
	}

	@Test
	void contextLoads() {
		String a = "aaaa";
		String b = "bbb";
		assertEquals(a, b);
	}

}
