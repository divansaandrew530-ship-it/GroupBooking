package com.ruzinn.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

  private final AuthInterceptor authInterceptor;
  private final RequestLoggingInterceptor requestLoggingInterceptor;
  private final String[] corsAllowedOrigins;

  public WebConfig(
      AuthInterceptor authInterceptor,
      RequestLoggingInterceptor requestLoggingInterceptor,
      @Value("${ruzinn.cors.allowed-origins:http://localhost:*}") String[] corsAllowedOrigins) {
    this.authInterceptor = authInterceptor;
    this.requestLoggingInterceptor = requestLoggingInterceptor;
    this.corsAllowedOrigins = corsAllowedOrigins;
  }

  @Override
  public void addInterceptors(InterceptorRegistry registry) {
    // Registered first so it also times (and logs) calls the auth check rejects.
    registry
        .addInterceptor(requestLoggingInterceptor)
        .addPathPatterns("/api/**", "/admin/**", "/company/**", "/companies/**");
    registry
        .addInterceptor(authInterceptor)
        .addPathPatterns("/admin/**", "/company/**", "/api/admin/**", "/api/company/**", "/api/parks/**");
  }

  /**
   * Browser origins allowed to call this API with the session cookie: the Angular dev server
   * (ng serve on its own port) and the public site domains. Configured by ruzinn.cors.allowed-origins.
   */
  @Override
  public void addCorsMappings(CorsRegistry registry) {
    registry
        .addMapping("/**")
        .allowedOriginPatterns(corsAllowedOrigins)
        .allowedMethods("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS")
        .allowedHeaders("*")
        .allowCredentials(true);
  }
}
