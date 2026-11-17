package com.restaurant.web;

import java.util.Map;

/** Local catalog photos that were visually checked; unknown dishes use a neutral placeholder. */
public final class MenuImages {
  private MenuImages() {}

  private static final Map<String, String> PHOTOS =
      Map.ofEntries(
          Map.entry("grilled-chicken", "roasted-chicken-supreme"),
          Map.entry("roasted-chicken-supreme", "roasted-chicken-supreme"),
          Map.entry("salad", "premium-seafood-salad"),
          Map.entry("premium-seafood-salad", "premium-seafood-salad"),
          Map.entry("japanese-a5-wagyu-steak", "japanese-a5-wagyu-steak"),
          Map.entry("lobster-thermidor", "vietnamese-grilled-lobster-with-butter"),
          Map.entry("grilled-whole-lobster", "vietnamese-grilled-lobster-with-butter"),
          Map.entry(
              "vietnamese-grilled-lobster-with-butter", "vietnamese-grilled-lobster-with-butter"),
          Map.entry("belgian-chocolate-fondant", "belgian-chocolate-fondant"),
          Map.entry("classic-mojito-mocktail", "classic-mojito-mocktail"),
          Map.entry("premium-jasmine-tea", "premium-jasmine-tea"),
          Map.entry("new-york-cheesecake", "new-york-cheesecake"),
          Map.entry("japanese-matcha-latte", "japanese-matcha-latte"),
          Map.entry("premium-seasonal-fruit-platter", "premium-seasonal-fruit-platter"),
          Map.entry("seafood-risotto", "seafood-risotto"),
          Map.entry("italian-sparkling-water", "italian-sparkling-water"));

  public static String forItemName(String name) {
    String slug =
        name.toLowerCase(java.util.Locale.ROOT)
            .replaceAll("[^a-z0-9]+", "-")
            .replaceAll("^-|-$", "")
            .replaceFirst("^demo-", "");
    String photo = PHOTOS.get(slug);
    return photo == null ? null : "/images/menu/" + photo + ".jpg";
  }
}
