package com.ruzinn.controller;

import com.ruzinn.dto.AuthSession;
import com.ruzinn.dto.QuoteMainView;
import com.ruzinn.dto.QuoteWorkspace;
import com.ruzinn.exception.UnauthorizedException;
import com.ruzinn.service.AccountService;
import com.ruzinn.service.QuoteRecordService;
import com.ruzinn.util.QuotePresentation;
import com.ruzinn.util.SessionUser;
import jakarta.servlet.http.HttpSession;
import java.util.List;
import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Saved quotes of the signed-in company. Quotes are created with {@code POST /api/parks/quote};
 * a quote becomes {@code ACCEPTED} (and a booking) only through a paid checkout,
 * {@code POST /api/parks/confirm} with {@code acceptQuote: true}.
 */
@RestController
@RequestMapping("/api/company/quotes")
public class QuoteController {

  private final AccountService accountService;
  private final QuoteRecordService quoteService;

  public QuoteController(AccountService accountService, QuoteRecordService quoteService) {
    this.accountService = accountService;
    this.quoteService = quoteService;
  }

  @GetMapping
  public List<QuoteMainView> list(@RequestParam(required = false) String companyId, HttpSession session) {
    AuthSession auth = accountService.requireSignedIn(SessionUser.current(session));
    if (companyId != null && !companyId.isBlank() && !companyId.trim().equals(auth.companyId())) {
      throw new UnauthorizedException("You can only view quotes for your company");
    }
    return quoteService.listVisibleTo(auth);
  }

  @GetMapping("/{quoteRef}")
  public QuoteMainView get(@PathVariable String quoteRef, HttpSession session) {
    AuthSession auth = accountService.requireSignedIn(SessionUser.current(session));
    return quoteService.requireVisible(auth, quoteRef);
  }

  /** Quote detail screen: the quote plus what the viewer may see and do with it. */
  @GetMapping("/{quoteRef}/workspace")
  public QuoteWorkspace workspace(@PathVariable String quoteRef, HttpSession session) {
    AuthSession auth = accountService.requireSignedIn(SessionUser.current(session));
    QuoteMainView quote = quoteService.requireVisible(auth, quoteRef);
    boolean canManage = auth.canManageQuotes();
    return new QuoteWorkspace(
        quote, auth.parentCompany(), canManage, canManage ? QuotePresentation.EDITABLE_STATUSES : List.of());
  }

  /** Body: {@code {"status": "PENDING" | "ACCEPTED" | ...}}. */
  @PostMapping("/{quoteRef}/status")
  public QuoteMainView updateStatus(
      @PathVariable String quoteRef, @RequestBody Map<String, String> body, HttpSession session) {
    AuthSession auth = requireQuoteManager(session, quoteRef);
    return quoteService.updateStatus(quoteRef, body == null ? null : body.get("status")).forViewer(auth.companyId());
  }

  /** Body: {@code {"followUp": "Call customer tomorrow"}}. */
  @PostMapping("/{quoteRef}/follow-up")
  public QuoteMainView updateFollowUp(
      @PathVariable String quoteRef, @RequestBody(required = false) Map<String, String> body, HttpSession session) {
    AuthSession auth = requireQuoteManager(session, quoteRef);
    return quoteService.updateFollowUp(quoteRef, body == null ? null : body.get("followUp")).forViewer(auth.companyId());
  }

  private AuthSession requireQuoteManager(HttpSession session, String quoteRef) {
    AuthSession auth = accountService.requireSignedIn(SessionUser.current(session));
    quoteService.requireVisible(auth, quoteRef);
    if (!auth.canManageQuotes()) {
      throw new UnauthorizedException("You cannot update this quote.");
    }
    return auth;
  }
}
