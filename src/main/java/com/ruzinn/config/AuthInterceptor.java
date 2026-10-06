package com.ruzinn.config;

import com.ruzinn.dto.AuthSession;
import com.ruzinn.entity.AppUser;
import com.ruzinn.exception.UnauthorizedException;
import com.ruzinn.repository.AppUserRepository;
import com.ruzinn.service.AccountService;
import com.ruzinn.util.AuditActor;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

/** Session checks for REST endpoints; denials become 401 JSON responses via ApiExceptionHandler. */
@Component
public class AuthInterceptor implements HandlerInterceptor {

  private final AppUserRepository userRepository;

  public AuthInterceptor(AppUserRepository userRepository) {
    this.userRepository = userRepository;
  }

  @Override
  public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
    if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
      // CORS preflight requests never carry the session cookie; let Spring's CORS filter answer them.
      return true;
    }
    String path = request.getRequestURI();
    HttpSession session = request.getSession(false);
    AuthSession auth = session == null ? null : (AuthSession) session.getAttribute(AccountService.SESSION_KEY);
    if (auth != null && isDisabled(auth)) {
      session.invalidate();
      throw new UnauthorizedException("This user is disabled");
    }
    if (auth != null) {
      AuditActor.bind(auth);
    }
    if (path.startsWith("/admin") || path.startsWith("/api/admin")) {
      if (auth == null || !auth.superAdmin()) {
        throw new UnauthorizedException("Super admin access required");
      }
      return true;
    }
    if (path.startsWith("/company") || path.startsWith("/api/company")) {
      if (auth == null) {
        throw new UnauthorizedException("Sign in required");
      }
      if (!auth.companyActive()) {
        throw new UnauthorizedException("Company is not Active yet");
      }
    }
    return true;
  }

  private boolean isDisabled(AuthSession auth) {
    if (auth == null || auth.userId() == null || auth.userId().isBlank()) {
      return false;
    }
    return userRepository
        .findById(auth.userId())
        .map(user -> AppUser.STATUS_DISABLED.equals(user.getStatus()))
        .orElse(true);
  }

  @Override
  public void afterCompletion(
      HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
    AuditActor.clear();
  }
}
