package com.restaurant;

import static org.assertj.core.api.Assertions.assertThat;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.net.CookieManager;
import java.net.CookiePolicy;
import java.net.URI;
import java.net.URLEncoder;
import java.net.URLDecoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Pattern;
import javax.sql.DataSource;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.env.Environment;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;

/**
 * Exercises actual servlet forwards, which MockMvc's forwarded-URL checks do
 * not dispatch.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    properties = "spring.datasource.url=jdbc:h2:mem:http"
        + "-security;MODE=MySQL;DB_CLOSE_DELAY=-1")
@ActiveProfiles("test")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class HttpSecurityTest {
  @Autowired
  DataSource dataSource;
  @Autowired
  JdbcTemplate jdbc;
  @Autowired
  PasswordEncoder encoder;
  @Autowired
  Environment environment;
  private String base;

  @BeforeAll
  void fixture() throws Exception {
    String sql = Files.readString(Path.of("..", "database", "schema.sql"))
        .replace(" ENGINE=InnoDB", "").replace(" STORED", "");
    new ResourceDatabasePopulator(
        new ByteArrayResource(sql.getBytes(StandardCharsets.UTF_8)))
        .execute(dataSource);
    jdbc.update("INSERT INTO Employee(employee_id,full"
        + "_name,position) VALUES(9801,'TEST HTTP" + " Waiter','WAITER')");
    jdbc.update(
        "INSERT INTO Account(employee_id,username,password_hash,role,is_active)"
            + " VALUES(9801,'TEST_HTTP_WAITER',?,'WAITER',true)",
        encoder.encode("TEST_ONLY_HTTP_2026"));
    jdbc.update("INSERT INTO Employee(employee_id,full_name,position)"
        + " VALUES(9802,'TEST HTTP Locked Chef','CHEF')");
    jdbc.update(
        "INSERT INTO Account(employee_id,username,password_hash,role,is_active)"
            + " VALUES(9802,'TEST_HTTP_LOCKED',?,'CHEF',false)",
        encoder.encode("TEST_ONLY_HTTP_2026"));
    jdbc.update("INSERT INTO Employee(employee_id,full_name,position)"
        + " VALUES(9803,'TEST HTTP Manager','MANAGER')");
    jdbc.update(
        "INSERT INTO Account(employee_id,username,password_hash,role,is_active)"
            + " VALUES(9803,'TEST_HTTP_MANAGER',?,'MANAGER',true)",
        encoder.encode("TEST_ONLY_HTTP_2026"));
    base = "http://127.0.0.1:"
        + environment.getRequiredProperty("local.server.port");
  }

  private HttpClient client() {
    return HttpClient.newBuilder()
        .cookieHandler(new CookieManager(null, CookiePolicy.ACCEPT_ALL))
        .followRedirects(HttpClient.Redirect.NORMAL).build();
  }

  private HttpResponse<String> get(HttpClient client, String path)
      throws Exception {
    return client.send(
        HttpRequest.newBuilder(URI.create(base + path)).GET().build(),
        HttpResponse.BodyHandlers.ofString());
  }

  private HttpResponse<String> post(HttpClient client, String path, String form)
      throws Exception {
    return client.send(
        HttpRequest.newBuilder(URI.create(base + path))
            .header("Content-Type", "application/x-www-form-urlencoded")
            .POST(HttpRequest.BodyPublishers.ofString(form)).build(),
        HttpResponse.BodyHandlers.ofString());
  }

  private String csrf(String html) {
    var match = Pattern.compile("name=\"_csrf\"[^>]*value=\"([^\"]+)\"")
        .matcher(html);
    assertThat(match.find()).as("Rendered CSRF field").isTrue();
    return "_csrf=" + URLEncoder.encode(match.group(1), StandardCharsets.UTF_8);
  }

  private HttpResponse<String> upload(HttpClient client, String token,
      String name, byte[] image) throws Exception {
    String boundary = "test-" + UUID.randomUUID();
    var body = new ByteArrayOutputStream();
    var fields = new java.util.LinkedHashMap<>(Map.of("name", name, "category",
        "TEST HTTP Category", "price", "12.50"));
    if (token != null)
      fields.put("_csrf", token);
    for (var field : fields.entrySet()) {
      body.write(
          ("--" + boundary + "\r\nContent-Disposition: form-data; name=\""
              + field.getKey() + "\"\r\n\r\n" + field.getValue() + "\r\n")
              .getBytes(StandardCharsets.UTF_8));
    }
    body.write(("--" + boundary
        + "\r\nContent-Disposition: form-data; name=\"image\"; filename=\"photo.png\""
        + "\r\nContent-Type: image/png\r\n\r\n")
        .getBytes(StandardCharsets.UTF_8));
    body.write(image);
    body.write(
        ("\r\n--" + boundary + "--\r\n").getBytes(StandardCharsets.UTF_8));
    return client.send(HttpRequest.newBuilder(URI.create(base + "/menu"))
        .timeout(Duration.ofSeconds(20))
        .header("Content-Type", "multipart/form-data; boundary=" + boundary)
        .POST(HttpRequest.BodyPublishers.ofByteArray(body.toByteArray()))
        .build(), HttpResponse.BodyHandlers.ofString());
  }

  private HttpClient managerClient() throws Exception {
    var client = client();
    var login = get(client, "/login");
    var signedIn = post(client, "/login", csrf(login.body())
        + "&username=TEST_HTTP_MANAGER&password=TEST_ONLY_HTTP_2026");
    assertThat(signedIn.uri().getPath()).isEqualTo("/home");
    return client;
  }

  @Test
  void realMultipartFormUploadsPhotoChecksCsrfAndRejectsFakeImages()
      throws Exception {
    try (var client = managerClient()) {
      var bytes = new ByteArrayOutputStream();
      ImageIO.write(new BufferedImage(3, 2, BufferedImage.TYPE_INT_RGB), "png",
          bytes);
      String token = URLDecoder.decode(
          csrf(get(client, "/menu").body()).substring(6),
          StandardCharsets.UTF_8);
      long before = jdbc.queryForObject("SELECT COUNT(*) FROM Menu_item",
          Long.class);
      var saved = upload(client, token, "TEST HTTP Món ảnh",
          bytes.toByteArray());
      assertThat(saved.statusCode()).isEqualTo(200);
      assertThat(saved.body()).contains("TEST HTTP Món ảnh",
          "/images/menu/uploads/");
      long id = jdbc.queryForObject(
          "SELECT item_id FROM Menu_item WHERE item_name=?", Long.class,
          "TEST HTTP Món ảnh");
      var photo = client.send(HttpRequest
          .newBuilder(URI.create(base + "/images/menu/uploads/" + id)).GET()
          .build(), HttpResponse.BodyHandlers.ofByteArray());
      assertThat(photo.statusCode()).isEqualTo(200);
      assertThat(photo.headers().firstValue("Content-Type"))
          .contains("image/png");
      assertThat(photo.body()).isEqualTo(bytes.toByteArray());
      var noToken = upload(client, null, "TEST HTTP No Csrf",
          bytes.toByteArray());
      assertThat(noToken.statusCode()).isEqualTo(403);
      token = URLDecoder.decode(csrf(get(client, "/menu").body()).substring(6),
          StandardCharsets.UTF_8);
      var invalid = upload(client, token, "TEST HTTP Fake Image",
          "not an image".getBytes(StandardCharsets.UTF_8));
      assertThat(invalid.statusCode()).isEqualTo(200);
      assertThat(invalid.body()).contains("Choose a valid JPG or PNG image.");
      assertThat(
          jdbc.queryForObject("SELECT COUNT(*) FROM Menu_item", Long.class))
          .isEqualTo(before + 1);
    }
  }

  @Test
  void oversizedMultipartShowsAFriendlyMessageWithoutSavingAnything()
      throws Exception {
    try (var client = managerClient()) {
      String token = URLDecoder.decode(
          csrf(get(client, "/menu").body()).substring(6),
          StandardCharsets.UTF_8);
      long before = jdbc.queryForObject("SELECT COUNT(*) FROM Menu_item",
          Long.class);
      var rejected = upload(client, token, "TEST HTTP Oversized Photo",
          new byte[5 * 1024 * 1024 + 1]);
      assertThat(rejected.statusCode()).isEqualTo(200);
      assertThat(rejected.body())
          .contains("Choose a JPG or PNG image up to 5 MB.");
      assertThat(
          jdbc.queryForObject("SELECT COUNT(*) FROM Menu_item", Long.class))
          .isEqualTo(before);
    }
  }

  @Test
  void forbiddenPostRenders403ThroughTheRealServletForward() throws Exception {
    try (var client = client()) {
      var login = get(client, "/login");
      var signedIn = post(client, "/login", csrf(login.body())
          + "&username=TEST_HTTP_WAITER&password=TEST_ONLY_HTTP_2026");
      assertThat(signedIn.uri().getPath()).isEqualTo("/home");
      var denied = post(client, "/menu", csrf(signedIn.body()));
      assertThat(denied.statusCode()).isEqualTo(403);
      assertThat(denied.body()).contains("Access denied");
      var chefOnly = post(client, "/kitchen/orders/1100/items/4100/start",
          csrf(signedIn.body()));
      assertThat(chefOnly.statusCode()).isEqualTo(403);
    }
  }

  @Test
  void anonymousPostWithoutCsrfAlsoReturns403RatherThanATemplateError()
      throws Exception {
    try (var client = client()) {
      var denied = post(client, "/menu", "name=TEST_CSRF_REJECTED");
      assertThat(denied.statusCode()).isEqualTo(403);
    }
  }

  @Test
  void realHttpOnlyShowsLockedAfterVerifyingThePassword() throws Exception {
    for (String password : new String[]{"wrong", "TEST_ONLY_HTTP_2026"}) {
      try (var client = client()) {
        var login = get(client, "/login");
        var failed = post(client, "/login", csrf(login.body())
            + "&username=TEST_HTTP_LOCKED&password=" + password);
        assertThat(failed.uri().getPath()).isEqualTo("/login");
        assertThat(failed.statusCode()).isEqualTo(200);
        if (password.equals("wrong"))
          assertThat(failed.body()).contains("Incorrect username or password.")
              .doesNotContain("Your account has been locked.");
        else
          assertThat(failed.body()).contains("Your account has been locked.")
              .doesNotContain("Incorrect username or password.");
      }
    }
  }

  @Test
  void realHttpMissingFieldsUseEnglishMessages() throws Exception {
    for (String[] scenario : new String[][]{
        {"", "", "Please enter your username and password."},
        {"", "TEST_ONLY_HTTP_2026", "Please enter your username."},
        {"TEST_HTTP_LOCKED", "", "Please enter your password."}}) {
      try (var client = client()) {
        var login = get(client, "/login");
        var failed = post(client, "/login", csrf(login.body()) + "&username="
            + scenario[0] + "&password=" + scenario[1]);
        assertThat(failed.statusCode()).isEqualTo(200);
        assertThat(failed.body()).contains(scenario[2])
            .doesNotContain("Your account has been locked.");
      }
    }
  }
}
