package com.ruzinn.controller;

import com.ruzinn.dto.CreateUserRequest;
import com.ruzinn.dto.UserView;
import com.ruzinn.service.AccountService;
import com.ruzinn.util.SessionUser;
import jakarta.servlet.http.HttpSession;
import java.util.List;
import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Users: company admins manage their own users; the Super Admin manages everyone. */
@RestController
public class UserController {

  private final AccountService accountService;

  public UserController(AccountService accountService) {
    this.accountService = accountService;
  }

  /** Users of the signed-in company (parent companies may pass another {@code companyId}). */
  @GetMapping("/api/company/users")
  public List<UserView> companyUsers(@RequestParam(required = false) String companyId, HttpSession session) {
    return accountService.listCompanyUsers(SessionUser.current(session), companyId);
  }

  @PostMapping("/api/company/users")
  public UserView createUser(@RequestBody CreateUserRequest request, HttpSession session) {
    return accountService.createUser(SessionUser.current(session), request);
  }

  @GetMapping("/api/admin/users")
  public List<UserView> allUsers(HttpSession session) {
    return accountService.listAllUsers(SessionUser.current(session));
  }

  @PostMapping("/api/admin/users/{userId}/block")
  public UserView block(@PathVariable String userId, HttpSession session) {
    return accountService.setUserBlocked(SessionUser.current(session), userId, true);
  }

  @PostMapping("/api/admin/users/{userId}/unblock")
  public UserView unblock(@PathVariable String userId, HttpSession session) {
    return accountService.setUserBlocked(SessionUser.current(session), userId, false);
  }

  /** Body: {@code {"password": "NewPass123"}}. */
  @PostMapping("/api/admin/users/{userId}/password")
  public UserView resetPassword(@PathVariable String userId, @RequestBody Map<String, String> body, HttpSession session) {
    return accountService.resetUserPassword(SessionUser.current(session), userId, body == null ? null : body.get("password"));
  }
}
