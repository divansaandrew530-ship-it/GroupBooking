package com.ruzinn;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

/** Request builders shared by the REST API tests. */
public final class TestApi {

  private TestApi() {}

  /** {@code POST /api/auth/login}; add {@code .session(session)} to keep the signed-in session. */
  public static MockHttpServletRequestBuilder apiLogin(String email, String password) {
    return post("/api/auth/login")
        .contentType(MediaType.APPLICATION_JSON)
        .content("{\"email\":" + json(email) + ",\"password\":" + json(password) + "}");
  }

  private static String json(String value) {
    return value == null ? "null" : "\"" + value.replace("\\", "\\\\").replace("\"", "\\\"") + "\"";
  }
}
