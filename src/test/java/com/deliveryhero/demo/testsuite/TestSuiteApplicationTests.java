package com.deliveryhero.demo.testsuite;

import com.deliveryhero.demo.testsuite.util.db.PostgresUtil;
import groovy.util.logging.Slf4j;
import io.restassured.RestAssured;
import org.junit.Rule;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.KafkaContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.containers.output.Slf4jLogConsumer;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest
@Slf4j
class TestSuiteApplicationTests {
	Logger logger = LoggerFactory.getLogger(this.getClass().getName());
	Slf4jLogConsumer logConsumer = new Slf4jLogConsumer(logger);

	private final static String KAFKA_TOPIC_ENV_NAME = "KAFKA_TOPIC";
	private final static String KAFKA_CONSUMER_GROUP_ENV_NAME = "KAFKA_CONSUMER_GROUP";
	private final static String KAFKA_URL_ENV_NAME = "KAFKA_URL";

	private final static String KAFKA_TOPIC_ENV_VALUE = "com.dh.dummy-topic";
	private final static String KAFKA_CONSUMER_GROUP_ENV_VALUE = "dummy_consumer";

	private final static String POSTGRES_JDBC_URL_ENV_NAME = "POSTGRES_JDBC_URL";
	private final static String POSTGRES_USERNAME_ENV_NAME = "POSTGRES_USERNAME";
	private final static String POSTGRES_PASSWORD_ENV_NAME = "POSTGRES_PASSWORD";

	private final static String REQUEST_USER_PARAM = "user";
	private final static String REQUEST_PASSWORD_HASH_PARAM = "passwordHash";
	private final static String REQUEST_AUTH_ENDPOINT = "/api/v1/auth";


	@Autowired
	private PostgresUtil postgresUtil;

	@Rule
	public KafkaContainer kafka  = new KafkaContainer();

	@Rule
	public PostgreSQLContainer postgres = new PostgreSQLContainer()
			.withDatabaseName("integration-tests-db")
			.withUsername("sa")
			.withPassword("sa");

	private final static String PRODUCER_CONTAINER_NAME = "hurricheck/producer-api";
	public GenericContainer producer;

	private final static String CONSUMER_CONTAINER_NAME = "hurricheck/consumer";
	public GenericContainer consumer;

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
		//setup kafka
		String kafkaURL = kafka.getContainerIpAddress() + ":" + kafka.getMappedPort(9093);


		consumer = new GenericContainer(CONSUMER_CONTAINER_NAME)
				.withEnv(KAFKA_TOPIC_ENV_NAME, KAFKA_TOPIC_ENV_VALUE)
				.withEnv(KAFKA_URL_ENV_NAME, kafkaURL)
				.withEnv(KAFKA_CONSUMER_GROUP_ENV_NAME, KAFKA_CONSUMER_GROUP_ENV_VALUE)
				.withEnv(POSTGRES_JDBC_URL_ENV_NAME, postgres.getJdbcUrl())
				.withEnv(POSTGRES_USERNAME_ENV_NAME, postgres.getUsername())
				.withEnv(POSTGRES_PASSWORD_ENV_NAME, postgres.getPassword());
		consumer.start();
		consumer.followOutput(logConsumer);

		uglyWait();
		producer = new GenericContainer(PRODUCER_CONTAINER_NAME)
				.withEnv(KAFKA_TOPIC_ENV_NAME, KAFKA_TOPIC_ENV_VALUE)
				.withEnv(KAFKA_URL_ENV_NAME, kafkaURL);
		producer.addExposedPort(8090);
		producer.start();
		producer.followOutput(logConsumer);
	}

	public String getProducerURL() {
		return producer.getContainerIpAddress() + ":"+producer.getMappedPort(8090);
	}

//	@Test
//	void contextLoads() {
//		String a = "aaaa";
//		String b = "bbb";
//		assertEquals(a, b);
//	}

	@Test
	public void restAssuredTestA() {
		String url = getProducerURL() + REQUEST_AUTH_ENDPOINT;
		String user = "usernameA";
		String passwordHash = "afeDWQweqdd124";

		RestAssured
				.given()
					.param(REQUEST_USER_PARAM, user)
					.param(REQUEST_PASSWORD_HASH_PARAM, passwordHash)
				.get("http://"+ url)
				.then()
					.statusCode(200);

		List<Integer> savedIds = postgresUtil.getIDs(user, passwordHash);
		assertEquals(1, savedIds.size());
		assertEquals(0, savedIds.get(0));
	}

	@Test
	public void restAssuredTestB() {
		String url = getProducerURL() + REQUEST_AUTH_ENDPOINT;
		String user = "usernameB";
		String passwordHash = "afeDWQweqdd124";

		RestAssured
				.given()
				.param(REQUEST_USER_PARAM, user)
				.param(REQUEST_PASSWORD_HASH_PARAM, passwordHash)
				.get("http://" + url)
				.then()
				.statusCode(200);
		List<Integer> savedIds = postgresUtil.getIDs(user, passwordHash);
		assertEquals(1, savedIds.size());
		assertEquals(0, savedIds.get(0));

	}

	public void uglyWait() {
		try {
			Thread.sleep(10000);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}

	@AfterEach
	public void shutDownContainers() {
		kafka.stop();
		postgres.stop();
		producer.stop();
		consumer.stop();
	}

}
