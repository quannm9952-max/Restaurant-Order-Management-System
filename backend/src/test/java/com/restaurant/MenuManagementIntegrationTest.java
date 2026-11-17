package com.restaurant;

import static org.assertj.core.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import com.restaurant.exception.BusinessException;
import com.restaurant.exception.NotFoundException;
import com.restaurant.service.MenuImageService;
import com.restaurant.service.MenuService;
import com.restaurant.service.OrderService;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import javax.imageio.ImageIO;
import javax.sql.DataSource;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.security.test.context.TestSecurityContextHolder;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

@SpringBootTest(
    properties = "spring.datasource.url=jdbc:h2:mem:menu-management;MODE=MySQL;DB_CLOSE_DELAY=-1")
@AutoConfigureMockMvc
@ActiveProfiles("test")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@Transactional
@WithMockUser(username = "DEMO_FIXTURE_MANAGER", roles = "MANAGER")
class MenuManagementIntegrationTest {
  @Autowired
  DataSource dataSource;
  @Autowired
  JdbcTemplate jdbc;
  @Autowired
  MockMvc mvc;
  @Autowired
  MenuService menu;
  @Autowired
  MenuImageService images;
  @Autowired
  OrderService orders;
  @Autowired
  PlatformTransactionManager transactions;

  @BeforeAll
  void fixture() throws Exception {
    var populator = new ResourceDatabasePopulator();
    for (String name : List.of("schema.sql", "test-data.sql",
        "demo-data.sql")) {
      String sql = Files
          .readString(Path.of("..", "database", name), StandardCharsets.UTF_8)
          .replace(" ENGINE=InnoDB", "").replace(" STORED", "")
          .replace("START TRANSACTION;", "").replace("COMMIT;", "");
      populator.addScript(
          new ByteArrayResource(sql.getBytes(StandardCharsets.UTF_8)));
    }
    populator.execute(dataSource);
  }

  private byte[] image(String format) throws Exception {
    var bytes = new ByteArrayOutputStream();
    ImageIO.write(new BufferedImage(4, 3, BufferedImage.TYPE_INT_RGB), format,
        bytes);
    return bytes.toByteArray();
  }

  private MockMultipartFile photo(String format) throws Exception {
    return new MockMultipartFile("image", "photo." + format, "image/" + format,
        image(format));
  }

  private long category(String name) {
    menu.saveCategory(null, name);
    return jdbc.queryForObject(
        "SELECT category_id FROM Menu_category WHERE category_name=?",
        Long.class, name);
  }

  private void restoreAuthentication() {
    SecurityContextHolder.setContext(TestSecurityContextHolder.getContext());
  }

  @Test
  void emptyCategoryCanBeDeletedAndRecreatedWithTheSameIdentity()
      throws Exception {
    long id = category("TEST Empty Category");
    mvc.perform(post("/menu/categories/" + id + "/delete").with(csrf()))
        .andExpect(redirectedUrl("/menu"))
        .andExpect(flash().attribute("success", "Category deleted."));
    restoreAuthentication();
    assertThat(menu.categories()).doesNotContain("TEST Empty Category");
    assertThat(jdbc.queryForObject(
        "SELECT is_active FROM Menu_category WHERE category_id=?",
        Boolean.class, id)).isFalse();
    menu.saveCategory(null, "TEST Empty Category");
    assertThat(jdbc.queryForObject(
        "SELECT category_id FROM Menu_category WHERE category_name=?",
        Long.class, "TEST Empty Category")).isEqualTo(id);
    assertThat(menu.categories()).contains("TEST Empty Category");
  }

  @Test
  void categoryWithDiscontinuedItemsStillRequiresMovingOrDeletingThem() {
    long id = category("TEST Nonempty Category");
    long item = menu.addMenu("TEST Discontinued Dish", "TEST Nonempty Category",
        "", new BigDecimal("4.50"), null);
    menu.toggleMenuAvailability(item, false);
    assertThatThrownBy(() -> menu.deleteCategory(id))
        .isInstanceOf(BusinessException.class)
        .hasMessageContaining("Move or delete all items");
    assertThat(menu.categories()).contains("TEST Nonempty Category");
  }

  @Test
  void removingDishesAndCategoryPreservesAllHistoricalOrderLines() {
    var before = jdbc
        .queryForList("SELECT * FROM Order_Detail ORDER BY order_detail_id");
    long itemCount = jdbc.queryForObject("SELECT COUNT(*) FROM Menu_item",
        Long.class);
    for (Long id : jdbc.queryForList(
        "SELECT item_id FROM Menu_item WHERE category_id=9001 AND is_active=true",
        Long.class))
      menu.deleteMenu(id);
    menu.deleteCategory(9001);
    assertThat(jdbc
        .queryForList("SELECT * FROM Order_Detail ORDER BY order_detail_id"))
        .isEqualTo(before);
    assertThat(
        jdbc.queryForObject("SELECT COUNT(*) FROM Menu_item", Long.class))
        .isEqualTo(itemCount);
    assertThat(menu.categories()).doesNotContain("DEMO Main courses");
    assertThat(orders.listAvailableItems())
        .noneMatch(item -> item.id() == 9001);
  }

  @Test
  void deletedCategoryCannotReceiveANewDishWithoutBeingRestored() {
    long id = category("TEST Deleted Category");
    menu.deleteCategory(id);
    assertThatThrownBy(() -> menu.addMenu("TEST Rejected Dish",
        "TEST Deleted Category", "", BigDecimal.ONE))
        .isInstanceOf(NotFoundException.class).hasMessageContaining("deleted");
    assertThat(menu.searchMenu("TEST Rejected Dish", null, null, null, null))
        .isEmpty();
  }

  @Test
  void deleteDishEndpointRemovesItFromMenuAndOrderingButKeepsItsRow()
      throws Exception {
    long id = menu.addMenu("TEST Delete Dish", "TEST New Category", "",
        BigDecimal.ONE, null);
    mvc.perform(post("/menu/" + id + "/delete").with(csrf())
        .param("search", "TEST Delete Dish")
        .param("category", "TEST New Category")
        .param("availability", "available").param("priceRange", "under_10")
        .param("sortBy", "price_asc"))
        .andExpect(redirectedUrl("/menu?search=TEST+Delete+Dish"
            + "&category=TEST+New+Category&availability=available"
            + "&priceRange=under_10&sortBy=price_asc"))
        .andExpect(flash().attributeExists("success"));
    restoreAuthentication();
    assertThat(menu.searchMenu("TEST Delete Dish", null, null, null, null))
        .isEmpty();
    assertThat(orders.listAvailableItems())
        .noneMatch(item -> item.id().longValue() == id);
    assertThat(jdbc.queryForObject(
        "SELECT COUNT(*) FROM Menu_item WHERE item_id=? AND is_active=false AND is_available=false",
        Integer.class, id)).isEqualTo(1);
    mvc.perform(get("/menu/" + id + "/json")).andExpect(status().isNotFound());
    mvc.perform(post("/menu/" + id + "/delete").with(csrf()).param("category",
        "TEST New Category"))
        .andExpect(redirectedUrl("/menu?category=TEST+New+Category"))
        .andExpect(flash().attributeExists("error"));
  }

  @Test
  void uploadedPhotoIsUsedByMenuAndOrderingAndServedAsItsActualType()
      throws Exception {
    var file = photo("png");
    long id = menu.addMenu("TEST Photo Dish", "TEST Photo Category", "",
        BigDecimal.TEN, file);
    String url = menu.getMenuItem(id).get("image_url").toString();
    assertThat(url).startsWith("/images/menu/uploads/" + id);
    assertThat(orders.listAvailableItems()).anyMatch(
        item -> item.id().longValue() == id && url.equals(item.imageUrl()));
    mvc.perform(get(url)).andExpect(status().isOk())
        .andExpect(content().contentType("image/png"))
        .andExpect(content().bytes(file.getBytes()));
  }

  @Test
  void editingWithoutAnUploadRetainsThePhotoAndANewUploadReplacesIt()
      throws Exception {
    var original = photo("png");
    long id = menu.addMenu("TEST Keep Photo", "TEST Photo Category", "",
        BigDecimal.TEN, original);
    menu.updateMenu(id, "TEST Renamed Photo", "TEST Photo Category", "",
        BigDecimal.ONE, true);
    assertThat(images.read(id).bytes()).isEqualTo(original.getBytes());
    var replacement = photo("jpeg");
    menu.updateMenu(id, "TEST Renamed Photo", "TEST Photo Category", "",
        BigDecimal.ONE, true, replacement);
    assertThat(images.read(id).bytes()).isEqualTo(replacement.getBytes());
    assertThat(images.read(id).contentType()).isEqualTo("image/jpeg");
  }

  @ParameterizedTest
  @ValueSource(strings = {"text", "svg", "gif", "empty", "large", "wide"})
  void invalidUploadsDoNotCreateDishesOrCategories(String kind)
      throws Exception {
    byte[] bytes = switch (kind) {
      case "text" -> "not an image".getBytes(StandardCharsets.UTF_8);
      case "svg" -> "<svg xmlns='http://www.w3.org/2000/svg'/>"
          .getBytes(StandardCharsets.UTF_8);
      case "gif" -> image("gif");
      case "empty" -> new byte[0];
      case "large" -> new byte[5 * 1024 * 1024 + 1];
      default -> {
        var output = new ByteArrayOutputStream();
        ImageIO.write(new BufferedImage(8001, 1, BufferedImage.TYPE_INT_RGB),
            "png", output);
        yield output.toByteArray();
      }
    };
    long items = jdbc.queryForObject("SELECT COUNT(*) FROM Menu_item",
        Long.class);
    long categories = jdbc.queryForObject("SELECT COUNT(*) FROM Menu_category",
        Long.class);
    var file = new MockMultipartFile("image", "fake.png", "image/png", bytes);
    assertThatThrownBy(() -> menu.addMenu("TEST Invalid Image",
        "TEST Invalid Category", "", BigDecimal.ONE, file))
        .isInstanceOf(IllegalArgumentException.class);
    assertThat(
        jdbc.queryForObject("SELECT COUNT(*) FROM Menu_item", Long.class))
        .isEqualTo(items);
    assertThat(
        jdbc.queryForObject("SELECT COUNT(*) FROM Menu_category", Long.class))
        .isEqualTo(categories);
  }

  @Test
  void originalFilenameCannotControlTheStoredPath() throws Exception {
    var file = new MockMultipartFile("image", "../../outside.jpg",
        "application/octet-stream", image("png"));
    long id = menu.addMenu("TEST Safe Filename", "TEST Photo Category", "",
        BigDecimal.ONE, file);
    assertThat(menu.getMenuItem(id).get("image_url").toString())
        .startsWith("/images/menu/uploads/" + id)
        .doesNotContain("outside", "..");
    assertThat(images.read(id).contentType()).isEqualTo("image/png");
  }

  @Test
  @Transactional(propagation = Propagation.NOT_SUPPORTED)
  void rollingBackAnUpdateRestoresThePreviousPhotoAndMetadata()
      throws Exception {
    var original = photo("png");
    var replacement = photo("jpeg");
    long id = menu.addMenu("TEST Rollback Photo", "DEMO Main courses", "",
        BigDecimal.ONE, original);
    try {
      new TransactionTemplate(transactions).execute(status -> {
        menu.updateMenu(id, "TEST Rolled Back Name", "DEMO Main courses", "",
            BigDecimal.TEN, true, replacement);
        status.setRollbackOnly();
        return null;
      });
      assertThat(menu.getMenuItem(id).get("name"))
          .isEqualTo("TEST Rollback Photo");
      assertThat(images.read(id).bytes()).isEqualTo(original.getBytes());
    } finally {
      jdbc.update("DELETE FROM Menu_item WHERE item_id=?", id);
    }
  }

  @Test
  void controllerRetainsDraftAndExistingPhotoAfterAnInvalidUpload()
      throws Exception {
    var original = photo("png");
    long id = menu.addMenu("TEST Existing Photo", "TEST Photo Category", "",
        BigDecimal.ONE, original);
    String url = menu.getMenuItem(id).get("image_url").toString();
    var failure = mvc
        .perform(multipart("/menu/" + id + "/update")
            .file(new MockMultipartFile("image", "bad.png", "image/png",
                new byte[]{1, 2, 3}))
            .with(csrf()).param("name", "TEST Retained Draft")
            .param("category", "TEST Photo Category")
            .param("description", "Draft description").param("price", "2.00")
            .param("available", "true"))
        .andExpect(redirectedUrl("/menu"))
        .andExpect(flash().attributeExists("menuDraft", "error")).andReturn();
    var body = mvc.perform(get("/menu").flashAttrs(failure.getFlashMap()))
        .andExpect(status().isOk()).andReturn().getResponse()
        .getContentAsString();
    assertThat(body).contains("TEST Retained Draft", url.replace("&", "&amp;"));
    restoreAuthentication();
    assertThat(menu.getMenuItem(id).get("name"))
        .isEqualTo("TEST Existing Photo");
    assertThat(images.read(id).bytes()).isEqualTo(original.getBytes());
  }

  @Test
  @WithMockUser(username = "TEST_WAITER", roles = "WAITER")
  void waiterCannotUploadOrDeleteMenuData() throws Exception {
    mvc.perform(multipart("/menu").file(photo("png")).with(csrf())
        .param("name", "Forbidden").param("category", "Forbidden")
        .param("price", "1.00")).andExpect(status().isForbidden());
    mvc.perform(post("/menu/9001/delete").with(csrf()))
        .andExpect(status().isForbidden());
    mvc.perform(post("/menu/categories/9001/delete").with(csrf()))
        .andExpect(status().isForbidden());
  }
}
