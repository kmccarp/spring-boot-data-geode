/*
 * Copyright 2017-present the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing
 * permissions and limitations under the License.
 */
package example.app.crm.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.InetSocketAddress;
import java.util.Arrays;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.cassandra.CassandraProperties;
import org.springframework.boot.autoconfigure.cassandra.CqlSessionBuilderCustomizer;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.core.env.Environment;
import org.springframework.data.cassandra.core.CassandraTemplate;
import org.springframework.lang.NonNull;

import com.datastax.oss.driver.api.core.CqlSession;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.CassandraContainer;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.output.Slf4jLogConsumer;
import org.testcontainers.utility.DockerImageName;

import example.app.crm.model.Customer;

/**
 * Spring {@link @Configuration} for Apache Cassandra using Testcontainers.
 *
 * @author John Blum
 * @see java.net.InetSocketAddress
 * @see com.datastax.oss.driver.api.core.CqlSession
 * @see org.springframework.boot.autoconfigure.cassandra.CassandraProperties
 * @see org.springframework.boot.autoconfigure.cassandra.CqlSessionBuilderCustomizer
 * @see org.springframework.boot.autoconfigure.domain.EntityScan
 * @see org.springframework.context.annotation.Bean
 * @see org.springframework.context.annotation.Configuration
 * @see org.springframework.context.annotation.Profile
 * @see org.springframework.core.env.Environment
 * @see org.springframework.data.cassandra.core.CassandraTemplate
 * @see org.testcontainers.containers.CassandraContainer
 * @see org.testcontainers.containers.GenericContainer
 * @see org.testcontainers.utility.DockerImageName
 * @since 1.1.0
 */
@Configuration
@Profile("inline-caching-cassandra")
@EntityScan(basePackageClasses = Customer.class)
@SuppressWarnings("unused")
public class TestcontainersCassandraConfiguration extends TestCassandraConfiguration {

	// Apache Cassandra Constants
	private static final String CASSANDRA_VERSION = System.getProperty("cassandra.version", "3.11.15");
	private static final String LOCAL_DATACENTER_NAME = System.getProperty("cassandra.datacenter.name", "datacenter1");

	// Java (JRE/JVM) Constants
	private static final String SPRING_JAVA_VERSION = System.getProperty("spring.java.version", "17.0.6_10-jdk-focal");

	// Testcontainers Constants
	private static final String TESTCONTAINERS_REGISTRY = "harbor-repo.vmware.com/";
	private static final String TESTCONTAINERS_REPOSITORY = "dockerhub-proxy-cache/";
	private static final String TESTCONTAINERS_HUB_IMAGE_NAME_PREFIX = TESTCONTAINERS_REGISTRY.concat(TESTCONTAINERS_REPOSITORY);
	private static final String TESTCONTAINERS_SPRINGCI_HUB_IMAGE_NAME_PREFIX = TESTCONTAINERS_HUB_IMAGE_NAME_PREFIX.concat("springci/");
	private static final String TESTCONTAINERS_HTTPS_PROXY = String.format("https://%s", TESTCONTAINERS_HUB_IMAGE_NAME_PREFIX);
	private static final String TESTCONTAINERS_PULL_PAUSE_TIMEOUT = "5"; // 5 seconds
	private static final String TESTCONTAINERS_RYUK_DISABLED = "true";
	private static final String TESTCONTAINERS_RYUK_ENABLED = "false";

	//private static final DockerImageName CASSANDRA_DOCKER_IMAGE_NAME = DockerImageName.parse("cassandra:latest");
	private static final DockerImageName CASSANDRA_DOCKER_IMAGE_NAME =
	DockerImageName.parse(String.format("cassandra:%s", CASSANDRA_VERSION));
	//private static final DockerImageName CASSANDRA_DOCKER_IMAGE_NAME =
	//	DockerImageName.parse(String.format("%1$sspring-data-with-cassandra-3.11:%2$s",
	//		TESTCONTAINERS_SPRINGCI_HUB_IMAGE_NAME_PREFIX, SPRING_JAVA_VERSION))
	//		.asCompatibleSubstituteFor("cassandra");

	private final Logger logger = LoggerFactory.getLogger(getClass());

	@Bean("CassandraContainer")
	GenericContainer<?> cassandraContainer(Environment environment) {

		GenericContainer<?> cassandraContainer = newEnvironmentCustomizedCassandraContainer(environment);

		cassandraContainer.start();
		cassandraContainer.followOutput(new Slf4jLogConsumer(getLogger()));

		return logContainerConfiguration(withCassandraServer(cassandraContainer, environment));
	}

	@Bean
	CqlSessionBuilderCustomizer cqlSessionBuilderCustomizer(CassandraProperties properties,
	@Qualifier("CassandraContainer") GenericContainer<?> cassandraContainer) {

		return cqlSessionBuilder -> cqlSessionBuilder
		.addContactPoint(resolveContactPoint(cassandraContainer))
		.withLocalDatacenter(properties.getLocalDatacenter())
		.withKeyspace(properties.getKeyspaceName());
	}

	protected @NonNull Logger getLogger() {
		return this.logger;
	}

	protected @NonNull GenericContainer<?> logContainerConfiguration(@NonNull GenericContainer<?> cassandraContainer) {

		logInfo("Is Jenkins Environment [{}]", isJenkinsEnvironment());
		logToSystemOut("Is Jenkins Environment [%s]", isNotJenkinsEnvironment());
		logInfo("Cassandra Testcontainer Environment Configuration:");
		logToSystemOut("Cassandra Testcontainer Environment Configuration:");

		cassandraContainer.getEnvMap().forEach((key, value) -> {
			logInfo("{} = [{}]", key, value);
			logToSystemOut("%s = %s", key, value);
		});

		return cassandraContainer;
	}

	protected void logInfo(String message, Object... arguments) {

		Logger logger = getLogger();

		if (logger.isInfoEnabled()) {
			logger.info(String.format(message, arguments), arguments);
		}
	}

	protected void logToSystemOut(String message, Object... arguments) {
		System.out.printf(message, arguments);
		System.out.flush();
	}

	private @NonNull GenericContainer<?> newCassandraContainer(@NonNull Environment environment) {

		return new CassandraContainer<>(CASSANDRA_DOCKER_IMAGE_NAME)
		.withInitScript(CASSANDRA_SCHEMA_CQL)
		//.withInitScript(CASSANDRA_INIT_CQL)
		.withExposedPorts(CASSANDRA_DEFAULT_PORT)
		.withReuse(true);
	}

	private @NonNull GenericContainer<?> newEnvironmentCustomizedCassandraContainer(@NonNull Environment environment) {
		return withCassandraEnvironmentConfiguration(newCassandraContainer(environment), environment);
	}

	private @NonNull CassandraTemplate newCassandraTemplate(@NonNull CqlSession session) {
		return new CassandraTemplate(session);
	}

	private @NonNull CqlSession newCqlSession(@NonNull GenericContainer<?> cassandraContainer) {

		return CqlSession.builder()
		.addContactPoint(resolveContactPoint(cassandraContainer))
		.withLocalDatacenter(LOCAL_DATACENTER_NAME)
		.build();
	}

	private boolean isJenkinsEnvironment() {
		return Boolean.getBoolean("jenkins");
	}

	private boolean isNotJenkinsEnvironment() {
		return !isJenkinsEnvironment();
	}

	private @NonNull GenericContainer<?> withCassandraEnvironmentConfiguration(
	@NonNull GenericContainer<?> cassandraContainer, @NonNull Environment environment) {

		return isNotJenkinsEnvironment()
		? cassandraContainer.withEnv("TESTCONTAINERS_RYUK_DISABLED", TESTCONTAINERS_RYUK_DISABLED)
		.withEnv("TESTCONTAINERS_PULL_PAUSE_TIMEOUT", TESTCONTAINERS_PULL_PAUSE_TIMEOUT)
		: cassandraContainer.withEnv("TESTCONTAINERS_HUB_IMAGE_NAME_PREFIX", TESTCONTAINERS_SPRINGCI_HUB_IMAGE_NAME_PREFIX)
		.withEnv("TESTCONTAINERS_PULL_PAUSE_TIMEOUT", TESTCONTAINERS_PULL_PAUSE_TIMEOUT);
	}

	private @NonNull GenericContainer<?> withCassandraServer(@NonNull GenericContainer<?> cassandraContainer,
	@NonNull Environment environment) {

		if (Arrays.asList(environment.getActiveProfiles()).contains(DEBUGGING_PROFILE)) {
			cassandraContainer = initializeCassandraServer(cassandraContainer);
			cassandraContainer = assertCassandraServerSetup(cassandraContainer);
		}

		return cassandraContainer;
	}

	private @NonNull GenericContainer<?> initializeCassandraServer(@NonNull GenericContainer<?> cassandraContainer) {

		try (CqlSession session = newCqlSession(cassandraContainer)) {
			newKeyspacePopulator(newCassandraSchemaCqlScriptResource()).populate(session);
		}

		return cassandraContainer;
	}

	private @NonNull GenericContainer<?> assertCassandraServerSetup(@NonNull GenericContainer<?> cassandraContainer) {

		try (CqlSession session = newCqlSession(cassandraContainer)) {

			session.getMetadata().getKeyspace(KEYSPACE_NAME)
			.map(keyspaceMetadata -> {

				assertThat(keyspaceMetadata.getName().toString()).isEqualToIgnoringCase(KEYSPACE_NAME);

				keyspaceMetadata.getTable(TABLE_NAME)
			.map(tableMetadata -> {

				assertThat(tableMetadata.getName().toString()).isEqualTo(TABLE_NAME);
				assertThat(tableMetadata.getKeyspace().toString()).isEqualToIgnoringCase(KEYSPACE_NAME);
				//assertCustomersTableHasSizeOne(session);

				return tableMetadata;
			})
			.orElseThrow(() -> new IllegalStateException(String.format("Table [%s] not found", TABLE_NAME)));

				return keyspaceMetadata;
			})
			.orElseThrow(() -> new IllegalStateException(String.format("Keyspace [%s] not found", KEYSPACE_NAME)));
		}

		return cassandraContainer;
	}

	private void assertCustomersTableHasSizeOne(@NonNull CqlSession session) {

		CassandraTemplate template = newCassandraTemplate(session);

		assertThat(template.getCqlOperations().execute(String.format("USE %s;", KEYSPACE_NAME))).isTrue();
		assertThat(template.getCqlOperations().queryForObject("SELECT count(*) FROM \"Customers\"", Long.class)).isOne();
		//assertThat(template.count(Customer.class)).isOne(); // Table Customers not found; needs to use the Keyspace
	}

	private @NonNull InetSocketAddress resolveContactPoint(@NonNull GenericContainer<?> cassandraContainer) {
		return new InetSocketAddress(cassandraContainer.getHost(),
		cassandraContainer.getMappedPort(CASSANDRA_DEFAULT_PORT));
	}
}
