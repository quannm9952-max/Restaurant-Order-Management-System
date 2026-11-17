package com.restaurant.support;

import java.util.regex.Pattern;

public final class Usernames {
  public static final String INVALID_MESSAGE = "Username must be 1-50 characters using only A-Z, a-z, 0-9 and underscore (_).";
  private static final Pattern ALLOWED = Pattern.compile("[A-Za-z0-9_]{1,50}");

  private Usernames() {
  }

  public static boolean isValid(String username) {
    return username != null && ALLOWED.matcher(username).matches();
  }
}
