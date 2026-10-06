package com.ruzinn.serviceimpl;

import com.ruzinn.dto.AuditEditView;
import com.ruzinn.dto.AuditReport;
import com.ruzinn.dto.AuthSession;
import com.ruzinn.dto.ChangeHistoryView;
import com.ruzinn.entity.AppUser;
import com.ruzinn.entity.BookingDetail;
import com.ruzinn.entity.BookingMain;
import com.ruzinn.entity.Company;
import com.ruzinn.entity.ItemMaster;
import com.ruzinn.entity.LoginHomePage;
import com.ruzinn.entity.QuoteMain;
import com.ruzinn.enums.EditableTable;
import com.ruzinn.exception.BookingNotFoundException;
import com.ruzinn.exception.QuoteNotFoundException;
import com.ruzinn.exception.UnauthorizedException;
import com.ruzinn.repository.AppUserRepository;
import com.ruzinn.repository.BookingMainRepository;
import com.ruzinn.repository.CompanyRepository;
import com.ruzinn.repository.ItemMasterRepository;
import com.ruzinn.repository.LoginHomePageRepository;
import com.ruzinn.repository.QuoteMainRepository;
import com.ruzinn.service.AuditReportService;
import com.ruzinn.service.ChangeHistoryService;
import com.ruzinn.service.RowHistoryService;
import com.ruzinn.util.BookingPresentation;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuditReportServiceImpl implements AuditReportService {

  private static final Logger log = LoggerFactory.getLogger(AuditReportServiceImpl.class);

  private final BookingMainRepository bookingMainRepository;
  private final QuoteMainRepository quoteMainRepository;
  private final CompanyRepository companyRepository;
  private final AppUserRepository userRepository;
  private final LoginHomePageRepository loginHomePages;
  private final ItemMasterRepository itemRepository;
  private final ChangeHistoryService changeHistoryService;
  private final RowHistoryService rowHistoryService;

  public AuditReportServiceImpl(
      BookingMainRepository bookingMainRepository,
      QuoteMainRepository quoteMainRepository,
      CompanyRepository companyRepository,
      AppUserRepository userRepository,
      LoginHomePageRepository loginHomePages,
      ItemMasterRepository itemRepository,
      ChangeHistoryService changeHistoryService,
      RowHistoryService rowHistoryService) {
    this.bookingMainRepository = bookingMainRepository;
    this.quoteMainRepository = quoteMainRepository;
    this.companyRepository = companyRepository;
    this.userRepository = userRepository;
    this.loginHomePages = loginHomePages;
    this.itemRepository = itemRepository;
    this.changeHistoryService = changeHistoryService;
    this.rowHistoryService = rowHistoryService;
  }

  @Override
  @Transactional(readOnly = true)
  public AuditReport booking(AuthSession actor, String bookingRefNum) {
    BookingMain booking = visibleBooking(actor, bookingRefNum);
    return new AuditReport(
        booking.getBookingRefNum(),
        ChangeHistoryService.BOOKING_MAIN,
        booking.getBookingRefNum(),
        formatWhen(booking.getCreatedDate()),
        firstNonBlank(booking.getUserName(), booking.getUserId(), "System"),
        toEdits(changeHistoryService.listForBooking(booking.getBookingRefNum())),
        rowHistoryService.count(EditableTable.BOOKING_MAIN, booking.getBookingRefNum()));
  }

  @Override
  @Transactional(readOnly = true)
  public AuditReport bookingItem(AuthSession actor, String bookingRefNum, Long itemId) {
    BookingMain booking = visibleBooking(actor, bookingRefNum);
    BookingDetail item =
        booking.getDetails().stream()
            .filter(detail -> itemId != null && itemId.equals(detail.getId()))
            .findFirst()
            .orElseThrow(() -> new BookingNotFoundException(booking.getBookingRefNum() + " item " + itemId));
    return new AuditReport(
        item.getItemName(),
        ChangeHistoryService.BOOKING_DETAIL,
        String.valueOf(item.getId()),
        formatWhen(item.getBookedOn()),
        firstNonBlank(item.getBookedBy(), item.getUserId(), booking.getUserName(), "System"),
        toEdits(changeHistoryService.listForEntity(ChangeHistoryService.BOOKING_DETAIL, String.valueOf(item.getId()))),
        rowHistoryService.count(EditableTable.BOOKING_DETAIL, item.getId()));
  }

  @Override
  @Transactional(readOnly = true)
  public AuditReport company(AuthSession actor, String companyId) {
    Company company = visibleCompany(actor, companyId);
    return new AuditReport(
        company.getCompanyName(),
        ChangeHistoryService.COMPANY,
        String.valueOf(company.getCompanyId()),
        formatWhen(company.getCreatedDate()),
        firstNonBlank(company.getOwnerName(), company.getEmail(), "System"),
        toEdits(changeHistoryService.listForEntity(ChangeHistoryService.COMPANY, String.valueOf(company.getCompanyId()))),
        rowHistoryService.count(EditableTable.COMPANY, company.getCompanyId()));
  }

  @Override
  @Transactional(readOnly = true)
  public AuditReport user(AuthSession actor, String userId) {
    AppUser user =
        userRepository.findById(required(userId, "User ID")).orElseThrow(() -> new IllegalArgumentException("User not found"));
    Company company =
        companyRepository
            .findById(user.getCompanyId())
            .orElseThrow(() -> new IllegalArgumentException("Company not found"));
    requireCompanyScope(actor, String.valueOf(company.getCompanyId()));
    return new AuditReport(
        user.getUserName(),
        ChangeHistoryService.APP_USER,
        user.getUserId(),
        formatWhen(user.getCreatedDate()),
        resolveUserName(user.getCreatedBy(), user.getUserName()),
        toEdits(changeHistoryService.listForEntity(ChangeHistoryService.APP_USER, user.getUserId())),
        rowHistoryService.count(EditableTable.APP_USER, user.getUserId()));
  }

  @Override
  @Transactional(readOnly = true)
  public AuditReport loginHome(AuthSession actor) {
    if (actor == null || !actor.superAdmin()) {
      throw new UnauthorizedException("Super admin access required");
    }
    LoginHomePage page =
        loginHomePages.findById(LoginHomePage.SINGLETON_ID).orElseGet(LoginHomePage::new);
    List<ChangeHistoryView> history = changeHistoryService.listForEntityType(ChangeHistoryService.LOGIN_HOME);
    Instant createdAt = page.getUpdatedDate();
    if (!history.isEmpty()) {
      ChangeHistoryView first = history.get(history.size() - 1);
      createdAt = first.changedAt() == null ? createdAt : first.changedAt();
    }
    int archived =
        rowHistoryService.count(EditableTable.LOGIN_HOME_PAGE, LoginHomePage.SINGLETON_ID)
            + page.getSlides().stream()
                .mapToInt(slide -> rowHistoryService.count(EditableTable.LOGIN_HOME_SLIDE, slide.getId()))
                .sum()
            + page.getFeatures().stream()
                .mapToInt(feature -> rowHistoryService.count(EditableTable.LOGIN_HOME_FEATURE, feature.getId()))
                .sum();
    return new AuditReport(
        firstNonBlank(page.getProductName(), "Login home"),
        ChangeHistoryService.LOGIN_HOME,
        String.valueOf(LoginHomePage.SINGLETON_ID),
        formatWhen(createdAt),
        "System",
        toEdits(history),
        archived);
  }

  @Override
  @Transactional(readOnly = true)
  public AuditReport item(AuthSession actor, String itemId) {
    if (actor == null || !actor.superAdmin()) {
      throw new UnauthorizedException("Super admin access required");
    }
    String id = required(itemId, "Item ID");
    ItemMaster item = itemRepository.findById(id).orElse(null);
    List<ChangeHistoryView> history = changeHistoryService.listForEntity(ChangeHistoryService.ITEM_MASTER, id);
    int archived = rowHistoryService.count(EditableTable.ITEM_MASTER, id);
    if (item == null && history.isEmpty() && archived == 0) {
      throw new IllegalArgumentException("Item not found");
    }
    String title = item == null ? "Deleted item" : item.getItemName();
    Instant createdAt = item == null ? null : item.getCreatedDate();
    String createdBy = item == null ? "" : firstNonBlank(item.getCreatedBy(), "System");
    for (int i = history.size() - 1; i >= 0; i--) {
      ChangeHistoryView row = history.get(i);
      if (createdAt == null && row.changedAt() != null) {
        createdAt = row.changedAt();
      }
      if (createdBy.isBlank() || "System".equals(createdBy)) {
        createdBy = firstNonBlank(row.actorUserName(), row.actorEmail(), createdBy);
      }
      if ("item_name".equals(row.fieldName())) {
        title = firstNonBlank(row.newValue(), row.oldValue(), title);
      }
    }
    if (item != null) {
      createdBy = resolveUserName(item.getCreatedBy(), createdBy);
    }
    return new AuditReport(
        firstNonBlank(title, "Item"),
        ChangeHistoryService.ITEM_MASTER,
        id,
        formatWhen(createdAt),
        firstNonBlank(createdBy, "System"),
        toEdits(history),
        archived);
  }

  @Override
  @Transactional(readOnly = true)
  public AuditReport quote(AuthSession actor, String quoteRefNum) {
    QuoteMain quote = visibleQuote(actor, quoteRefNum);
    return new AuditReport(
        quote.getQuoteRefNum(),
        ChangeHistoryService.QUOTE_MAIN,
        quote.getQuoteRefNum(),
        formatWhen(quote.getCreatedDate()),
        firstNonBlank(quote.getUserName(), quote.getUserId(), "System"),
        toEdits(changeHistoryService.listForBooking(quote.getQuoteRefNum())),
        rowHistoryService.count(EditableTable.QUOTE_MAIN, quote.getQuoteRefNum()));
  }

  private BookingMain visibleBooking(AuthSession actor, String bookingRefNum) {
    requireSignedIn(actor);
    if (bookingRefNum == null || bookingRefNum.isBlank()) {
      throw new BookingNotFoundException("");
    }
    BookingMain booking =
        bookingMainRepository
            .findById(bookingRefNum.trim().toUpperCase())
            .orElseThrow(() -> new BookingNotFoundException(bookingRefNum));
    if (!actor.parentCompany()
        && (booking.getCompanyId() == null || !booking.getCompanyId().equals(actor.companyId()))) {
      log.warn(
          "Audit denied: {} (company {}) tried to read booking {} of company {}",
          actor.email(), actor.companyId(), bookingRefNum, booking.getCompanyId());
      throw new UnauthorizedException("You can only view bookings for your company");
    }
    return booking;
  }

  private QuoteMain visibleQuote(AuthSession actor, String quoteRefNum) {
    requireSignedIn(actor);
    if (quoteRefNum == null || quoteRefNum.isBlank()) {
      throw new QuoteNotFoundException("");
    }
    QuoteMain quote =
        quoteMainRepository
            .findById(quoteRefNum.trim().toUpperCase())
            .orElseThrow(() -> new QuoteNotFoundException(quoteRefNum));
    if (quote.getCompanyId() == null
        || (!quote.getCompanyId().equals(actor.companyId())
            && !createdByActor(quote, actor))) {
      log.warn(
          "Audit denied: {} (company {}) tried to read quote {} of company {}",
          actor.email(), actor.companyId(), quoteRefNum, quote.getCompanyId());
      throw new UnauthorizedException("You can only view quotes for your company");
    }
    return quote;
  }

  private static boolean createdByActor(QuoteMain quote, AuthSession actor) {
    if (quote == null || actor == null || quote.getUserId() == null || quote.getUserId().isBlank()) {
      return false;
    }
    String creator = quote.getUserId().trim();
    return creator.equalsIgnoreCase(actor.email() == null ? "" : actor.email().trim())
        || creator.equalsIgnoreCase(actor.userId() == null ? "" : actor.userId().trim());
  }

  private Company visibleCompany(AuthSession actor, String companyId) {
    requireSignedIn(actor);
    Long id;
    try {
      id = Long.valueOf(required(companyId, "Company ID"));
    } catch (NumberFormatException ex) {
      throw new IllegalArgumentException("Company not found");
    }
    Company company = companyRepository.findById(id).orElseThrow(() -> new IllegalArgumentException("Company not found"));
    requireCompanyScope(actor, String.valueOf(company.getCompanyId()));
    return company;
  }

  private void requireCompanyScope(AuthSession actor, String companyId) {
    requireSignedIn(actor);
    if (actor.parentCompany()) {
      return;
    }
    if (companyId == null || !companyId.equals(actor.companyId())) {
      log.warn("Audit denied: {} (company {}) tried to read company {}", actor.email(), actor.companyId(), companyId);
      throw new UnauthorizedException("You can only view audit data for your company");
    }
  }

  private static void requireSignedIn(AuthSession actor) {
    if (actor == null) {
      throw new UnauthorizedException("Sign in required");
    }
  }

  private String resolveUserName(String userId, String fallback) {
    if (userId == null || userId.isBlank()) {
      return firstNonBlank(fallback, "System");
    }
    return userRepository
        .findById(userId)
        .map(AppUser::getUserName)
        .orElseGet(() -> firstNonBlank(userId, fallback, "System"));
  }

  private static List<AuditEditView> toEdits(List<ChangeHistoryView> history) {
    List<AuditEditView> edits = new ArrayList<>();
    for (ChangeHistoryView row : history) {
      if (row.fieldName() != null && row.fieldName().equals("item_content")) {
        continue;
      }
      edits.add(
          new AuditEditView(
              row.summary(),
              row.valueChangeLabel(),
              row.changedAtLabel(),
              row.actorLabel(),
              blankToNull(row.oldValue()),
              blankToNull(row.newValue())));
    }
    return edits;
  }

  private static String formatWhen(Instant instant) {
    return BookingPresentation.formatCreated(instant);
  }

  private static String blankToNull(String value) {
    return value == null || value.isBlank() ? null : value;
  }

  private static String required(String value, String label) {
    if (value == null || value.isBlank()) {
      throw new IllegalArgumentException(label + " is required");
    }
    return value.trim();
  }

  private static String firstNonBlank(String... values) {
    if (values == null) {
      return "";
    }
    for (String value : values) {
      if (value != null && !value.isBlank()) {
        return value.trim();
      }
    }
    return "";
  }
}
