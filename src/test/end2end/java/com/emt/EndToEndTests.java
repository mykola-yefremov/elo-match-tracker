package com.emt;

import static java.net.http.HttpClient.newHttpClient;
import static java.net.http.HttpRequest.BodyPublishers.ofString;
import static java.net.http.HttpRequest.newBuilder;
import static java.net.http.HttpResponse.BodyHandlers.ofString;
import static java.time.Duration.of;
import static java.time.temporal.ChronoUnit.SECONDS;
import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.Network;
import org.testcontainers.containers.output.Slf4jLogConsumer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.MountableFile;

@Slf4j
@Testcontainers
public class EndToEndTests {

  private static final Integer API_PORT = 8080;
  private static final Integer SERVICE_PORT = 9090;
  private static final Integer POSTGRES_PORT = 5432;
  private static final Network NETWORK = Network.newNetwork();

  @Container
  @SuppressWarnings("resource")
  private static final GenericContainer<?> POSTGRES =
      new GenericContainer<>("postgres:15.1")
          .withNetwork(NETWORK)
          .withNetworkAliases("postgres")
          .withEnv("POSTGRES_PASSWORD", "postgres")
          .withCopyFileToContainer(
              MountableFile.forClasspathResource("docker/database/init_db.sql"),
              "/docker-entrypoint-initdb.d/init_db.sql")
          .withExposedPorts(POSTGRES_PORT)
          .waitingFor(Wait.forLogMessage(".*database system is ready to accept connections.*\\s", 1))
          .withLogConsumer(new Slf4jLogConsumer(log).withPrefix("postgres"));

  @Container
  @SuppressWarnings("resource")
  private static final GenericContainer<?> APP =
      new GenericContainer<>("docker.io/library/elo-match-tracker:latest")
          .dependsOn(POSTGRES)
          .withNetwork(NETWORK)
          .withEnv("SPRING_PROFILES_ACTIVE", "e2etest")
          .withExposedPorts(API_PORT, SERVICE_PORT)
          .waitingFor(
              Wait.forHttp("/actuator/health")
                  .forPort(SERVICE_PORT)
                  .forStatusCode(200)
                  .withStartupTimeout(Duration.ofSeconds(90)))
          .withLogConsumer(new Slf4jLogConsumer(log).withPrefix("app"));

  @Test
  void containerIsRunning() {
    assertThat(APP.isRunning()).isTrue();
    assertThat(POSTGRES.isRunning()).isTrue();
  }

  @Test
  void metricsAreAvailable() {
    HttpResponse<String> httpResponse = getString("/actuator/metrics", SERVICE_PORT);

    assertThat(httpResponse.statusCode()).isEqualTo(200);
    assertThat(httpResponse.body()).contains("jvm.memory.used");
  }

  @Test
  void actuatorInfoIsAvailable() {
    HttpResponse<String> httpResponse = getString("/actuator/info", SERVICE_PORT);

    assertThat(httpResponse.statusCode()).isEqualTo(200);
    assertThat(httpResponse.body()).contains("elo_match_tracker");
  }

  @Test
  void swaggerWorks() {
    HttpResponse<String> httpResponse = getString("/swagger-ui/index.html");

    assertThat(httpResponse.statusCode()).isEqualTo(200);
    assertThat(httpResponse.body()).contains("Swagger UI");
  }

  private HttpResponse<String> getString(String path) {
    return getString(path, API_PORT);
  }

  private HttpResponse<String> getString(String path, Integer port) {
    try {
      URI uri = new URI("http", null, APP.getHost(), APP.getMappedPort(port), path, null, null);

      HttpRequest request = newBuilder(uri).GET().timeout(of(10, SECONDS)).build();
      log.debug("Request: {}", request);

      HttpResponse<String> httpResponse = newHttpClient().send(request, ofString());
      log.info("Response: {}", httpResponse);

      return httpResponse;
    } catch (URISyntaxException | IOException | InterruptedException e) {
      throw new RuntimeException(e);
    }
  }

  private HttpResponse<String> postString(String path, String body) {
    return postString(path, API_PORT, body, "application/json");
  }

  private HttpResponse<String> postString(
      String path, Integer port, String body, String contentType) {

    try {
      URI uri = new URI("http", null, APP.getHost(), APP.getMappedPort(port), path, null, null);

      HttpRequest request =
          newBuilder(uri)
              .POST(ofString(body))
              .timeout(of(10, SECONDS))
              .header("Content-Type", contentType)
              .build();
      log.debug("Request: {}", request);

      HttpResponse<String> httpResponse = newHttpClient().send(request, ofString());
      log.info("Response: {}", httpResponse);
      log.debug("Response body: {}", httpResponse.body());

      return httpResponse;
    } catch (URISyntaxException | IOException | InterruptedException e) {
      throw new RuntimeException(e);
    }
  }
}
