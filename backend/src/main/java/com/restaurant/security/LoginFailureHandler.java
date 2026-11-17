package com.restaurant.security;

import com.restaurant.repository.AccountRepository;
import com.restaurant.support.Usernames;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationFailureHandler;
import org.springframework.stereotype.Component;

@Component
public class LoginFailureHandler extends SimpleUrlAuthenticationFailureHandler {
  public static final String ERROR_ATTRIBUTE = "loginErrorMessage";
  public static final String INCORRECT_CREDENTIALS = "Incorrect username or password.";
  public static final String LOCKED_ACCOUNT = "Your account has been locked.";
  private final AccountRepository accounts;
  private final PasswordEncoder encoder;

  public LoginFailureHandler(AccountRepository accounts,
      PasswordEncoder encoder) {
    super("/login?error");
    this.accounts = accounts;
    this.encoder = encoder;
  }

  @Override
  public void onAuthenticationFailure(HttpServletRequest request,
      HttpServletResponse response, AuthenticationException exception)
      throws IOException, ServletException {
    request.getSession().setAttribute(ERROR_ATTRIBUTE,
        message(request, exception));
    super.onAuthenticationFailure(request, response, exception);
  }

  private String message(HttpServletRequest request,
      AuthenticationException exception) {
    String username = request.getParameter("username");
    String password = request.getParameter("password");
    boolean missingUsername = username == null || username.isEmpty();
    boolean missingPassword = password == null || password.isEmpty();
    if (missingUsername && missingPassword)
      return "Please enter your username and password.";
    if (missingUsername)
      return "Please enter your username.";
    if (missingPassword)
      return "Please enter your password.";
    // Spring checks disabled status before the password. Verify credentials
    // before showing the account status.
    if (exception instanceof DisabledException && Usernames.isValid(username)
        && password.getBytes(StandardCharsets.UTF_8).length <= 72
        && accounts.findByUsername(username).filter(a -> !a.isActive()
            && encoder.matches(password, a.getPasswordHash())).isPresent())
      return LOCKED_ACCOUNT;
    return INCORRECT_CREDENTIALS;
  }
}
