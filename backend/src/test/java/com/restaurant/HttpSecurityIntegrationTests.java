package com.restaurant;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.CookieManager;
import java.net.CookiePolicy;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.regex.Pattern;
import javax.sql.DataSource;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.env.Environment;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;

/** Exercises actual servlet forwards, which MockMvc's forwarded-URL checks do not dispatch. */
@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    properties = "spring.datasource.url=jdbc:h2:mem:http-security;MODE=MySQL;DB_CLOSE_DELAY=-1")
@ActiveProfiles("test")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class HttpSecurityIntegrationTests {
  @Autowired DataSource dataSource;
  @Autowired JdbcTemplate jdbc;
  @Autowired PasswordEncoder encoder;
  @Autowired Environment environment;
  private String base;

  @BeforeAll
  void fixture() throws Exception {
    String sql =
        Files.readString(Path.of("..", "database", "schema.sql"))
            .replace(" ENGINE=InnoDB", "")
            .replace(" STORED", "");
    new ResourceDatabasePopulator(new ByteArrayResource(sql.getBytes(StandardCharsets.UTF_8)))
        .execute(dataSource);
    jdbc.update(
        "INSERT INTO Employee(employee_id,full_name,position) VALUES(9801,'TEST HTTP"
            + " Waiter','WAITER')");
    jdbc.update(
        "INSERT INTO Account(employee_id,username,password_hash,role,is_active)"
            + " VALUES(9801,'TEST_HTTP_WAITER',?,'WAITER',true)",
        encoder.encode("TEST_ONLY_HTTP_2026"));
    base = "http://127.0.0.1:" + environment.getRequiredProperty("local.server.port");
  }

  private HttpClient client() {
    return HttpClient.newBuilder()
        .cookieHandler(new CookieManager(null, CookiePolicy.ACCEPT_ALL))
        .followRedirects(HttpClient.Redirect.NORMAL)
        .build();
  }

  private HttpResponse<String> get(HttpClient client, String path) throws Exception {
    return client.send(
        HttpRequest.newBuilder(URI.create(base + path)).GET().build(),
        HttpResponse.BodyHandlers.ofString());
  }

  private HttpResponse<String> post(HttpClient client, String path, String form) throws Exception {
    return client.send(
        HttpRequest.newBuilder(URI.create(base + path))
            .header("Content-Type", "application/x-www-form-urlencoded")
            .POST(HttpRequest.BodyPublishers.ofString(form))
            .build(),
        HttpResponse.BodyHandlers.ofString());
  }

  private String csrf(String html) {
    var match = Pattern.compile("name=\"_csrf\"[^>]*value=\"([^\"]+)\"").matcher(html);
    assertThat(match.find()).as("Rendered CSRF field").isTrue();
    return "_csrf=" + URLEncoder.encode(match.group(1), StandardCharsets.UTF_8);
  }

  @Test
  void forbiddenPostRenders403ThroughTheRealServletForward() throws Exception {
    try (var client = client()) {
      var login = get(client, "/login");
      var signedIn =
          post(
              client,
              "/login",
              csrf(login.body()) + "&username=TEST_HTTP_WAITER&password=TEST_ONLY_HTTP_2026");
      assertThat(signedIn.uri().getPath()).isEqualTo("/home");
      var denied = post(client, "/menu", csrf(signedIn.body()));
      assertThat(denied.statusCode()).isEqualTo(403);
      assertThat(denied.body()).contains("Access denied");
      var deferred = post(client, "/serving/4100", csrf(signedIn.body()));
      assertThat(deferred.statusCode()).isEqualTo(403);
    }
  }

  @Test
  void anonymousPostWithoutCsrfAlsoReturns403RatherThanATemplateError() throws Exception {
    try (var client = client()) {
      var denied = post(client, "/menu", "name=TEST_CSRF_REJECTED");
      assertThat(denied.statusCode()).isEqualTo(403);
    }
  }
}
