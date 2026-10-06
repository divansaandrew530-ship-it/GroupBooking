package com.ruzinn.config;

import com.ruzinn.dto.AuthSession;
import com.ruzinn.util.SessionUser;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * One log line per API call, for every controller:
 *
 * <pre>
 * POST /api/admin/companies/12/approve -> 409 in 35 ms [CompanyController.approve] user=admin@rathin.com
 * </pre>
 *
 * Successful calls log at INFO, 4xx at WARN and 5xx at ERROR. The error message itself is logged by
 * {@link com.ruzinn.exception.ApiExceptionHandler}.
 */
@Component
public class RequestLoggingInterceptor implements HandlerInterceptor {

  private static final Logger log = LoggerFactory.getLogger("com.ruzinn.api");
  private static final String START = RequestLoggingInterceptor.class.getName() + ".start";

  @Override
  public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
    request.setAttribute(START, System.currentTimeMillis());
    return true;
  }

  @Override
  public void afterCompletion(
      HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
    if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
      return;
    }
    Object start = request.getAttribute(START);
    long millis = start instanceof Long started ? System.currentTimeMillis() - started : -1;
    int status = response.getStatus();
    String path = request.getRequestURI() + (request.getQueryString() == null ? "" : "?" + request.getQueryString());

    String line = "{} {} -> {} in {} ms [{}] user={}";
    Object[] args = {request.getMethod(), path, status, millis, handlerName(handler), currentUser(request)};
    if (ex != null || status >= 500) {
      log.error(line, args);
    } else if (status >= 400) {
      log.warn(line, args);
    } else {
      log.info(line, args);
    }
  }

  private static String handlerName(Object handler) {
    if (handler instanceof HandlerMethod method) {
      return method.getBeanType().getSimpleName() + "." + method.getMethod().getName();
    }
    return "-";
  }

  private static String currentUser(HttpServletRequest request) {
    HttpSession session = request.getSession(false);
    AuthSession user = session == null ? null : SessionUser.current(session);
    return user == null ? "anonymous" : user.email();
  }
}
