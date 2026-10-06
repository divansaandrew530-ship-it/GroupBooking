package com.ruzinn.serviceimpl;

import com.ruzinn.dto.AuthSession;
import com.ruzinn.dto.ParkDayPlan;
import com.ruzinn.dto.ParkQuote;
import com.ruzinn.dto.ParkQuoteLine;
import com.ruzinn.dto.ParkQuoteRequest;
import com.ruzinn.dto.QuoteDetailView;
import com.ruzinn.dto.QuoteMainView;
import com.ruzinn.entity.AppUser;
import com.ruzinn.entity.ItemMaster;
import com.ruzinn.entity.QuoteDetail;
import com.ruzinn.entity.QuoteMain;
import com.ruzinn.enums.EditableTable;
import com.ruzinn.exception.QuoteNotFoundException;
import com.ruzinn.exception.UnauthorizedException;
import com.ruzinn.repository.AppUserRepository;
import com.ruzinn.repository.ItemMasterRepository;
import com.ruzinn.repository.QuoteMainRepository;
import com.ruzinn.service.ChangeHistoryService;
import com.ruzinn.service.QuoteRecordService;
import com.ruzinn.service.RowHistoryService;
import com.ruzinn.util.BookingPresentation;
import com.ruzinn.util.QuotePresentation;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.ObjectMapper;

@Service
public class QuoteRecordServiceImpl implements QuoteRecordService {

  private static final Logger log = LoggerFactory.getLogger(QuoteRecordServiceImpl.class);

  static final String DEFAULT_COMPANY_NAME = "Direct";
  static final String DEFAULT_COMPANY_ID = "DIRECT";

  private final QuoteMainRepository quoteMainRepository;
  private final ChangeHistoryService changeHistoryService;
  private final RowHistoryService rowHistoryService;
  private final ItemMasterRepository itemMasterRepository;
  private final AppUserRepository userRepository;
  private final ObjectMapper objectMapper;

  public QuoteRecordServiceImpl(
      QuoteMainRepository quoteMainRepository,
      ChangeHistoryService changeHistoryService,
      RowHistoryService rowHistoryService,
      ItemMasterRepository itemMasterRepository,
      AppUserRepository userRepository,
      ObjectMapper objectMapper) {
    this.quoteMainRepository = quoteMainRepository;
    this.changeHistoryService = changeHistoryService;
    this.rowHistoryService = rowHistoryService;
    this.itemMasterRepository = itemMasterRepository;
    this.userRepository = userRepository;
    this.objectMapper = objectMapper;
  }

  @Override
  @Transactional
  public QuoteMainView saveGenerated(ParkQuote quote, ParkQuoteRequest request, AuthSession auth) {
    QuoteMain saved = persist(quote, request, auth, null);
    log.info(
        "Quote {} saved for company {} ({}) by {}",
        saved.getQuoteRefNum(), saved.getCompanyId(), saved.getCompanyName(), auth == null ? "guest" : auth.email());
    return toView(saved);
  }

  @Override
  @Transactional
  public QuoteMainView markSent(ParkQuote quote, ParkQuoteRequest request, AuthSession auth, String sentTo) {
    QuoteMain saved = persist(quote, request, auth, QuotePresentation.SENT);
    String recipient = firstNonBlank(sentTo, request == null ? null : request.getEmail());
    if (!recipient.isBlank() && !sameText(saved.getSentTo(), recipient)) {
      rowHistoryService.archive(
          EditableTable.QUOTE_MAIN, saved.getQuoteRefNum(), ChangeHistoryService.STATUS_CHANGE, "sent_to");
      String previous = saved.getSentTo();
      saved.setSentTo(recipient);
      saved.setSentAt(Instant.now());
      saved.setUpdatedDate(Instant.now());
      saved = quoteMainRepository.save(saved);
      changeHistoryService.record(
          ChangeHistoryService.QUOTE_MAIN,
          saved.getQuoteRefNum(),
          saved.getQuoteRefNum(),
          ChangeHistoryService.STATUS_CHANGE,
          "sent_to",
          previous,
          recipient,
          saved.getCompanyId(),
          saved.getCompanyName());
    } else if (saved.getSentAt() == null) {
      saved.setSentTo(firstNonBlank(saved.getSentTo(), recipient));
      saved.setSentAt(Instant.now());
      saved.setUpdatedDate(Instant.now());
      saved = quoteMainRepository.save(saved);
    }
    log.info("Quote {} marked SENT to {}", saved.getQuoteRefNum(), saved.getSentTo());
    return toView(saved);
  }

  @Override
  @Transactional
  public QuoteMainView updateStatus(String quoteRefNum, String status) {
    QuoteMain main = requiredQuote(quoteRefNum);
    String next = requireEditableStatus(status);
    // A booking is created only after payment, so ACCEPTED comes from the booking checkout
    // (see markAccepted), never from a plain status change.
    if (QuotePresentation.ACCEPTED.equals(next) && !hasText(main.getBookingRefNum())) {
      throw new IllegalArgumentException(
          "Complete the booking checkout to accept this quote. It is accepted once payment succeeds.");
    }
    return toView(applyStatus(main, next));
  }

  @Override
  @Transactional
  public QuoteMain requireAcceptable(AuthSession auth, String quoteRefNum) {
    if (auth == null) {
      throw new UnauthorizedException("Sign in to accept this quote");
    }
    QuoteMain main = requireVisibleEntity(auth, quoteRefNum);
    if (hasText(main.getBookingRefNum())) {
      throw new IllegalArgumentException(
          "Quote " + main.getQuoteRefNum() + " is already booked as " + main.getBookingRefNum() + ".");
    }
    return main;
  }

  @Override
  @Transactional
  public QuoteMainView markAccepted(String quoteRefNum, String bookingRefNum) {
    QuoteMain main = requiredQuote(quoteRefNum);
    if (hasText(main.getBookingRefNum())) {
      throw new IllegalArgumentException(
          "Quote " + main.getQuoteRefNum() + " is already booked as " + main.getBookingRefNum() + ".");
    }
    rowHistoryService.archive(
        EditableTable.QUOTE_MAIN, main.getQuoteRefNum(), ChangeHistoryService.STATUS_CHANGE, "booking_ref_num");
    main.setBookingRefNum(bookingRefNum);
    changeHistoryService.record(
        ChangeHistoryService.QUOTE_MAIN,
        main.getQuoteRefNum(),
        firstNonBlank(bookingRefNum, main.getQuoteRefNum()),
        ChangeHistoryService.STATUS_CHANGE,
        "booking_ref_num",
        null,
        bookingRefNum,
        main.getCompanyId(),
        main.getCompanyName());
    log.info("Quote {} accepted after payment: booking {}", main.getQuoteRefNum(), bookingRefNum);
    return toView(applyStatus(main, QuotePresentation.ACCEPTED));
  }

  private QuoteMain applyStatus(QuoteMain main, String next) {
    String previous = main.getStatus();
    if (!sameText(previous, next)) {
      rowHistoryService.archive(
          EditableTable.QUOTE_MAIN, main.getQuoteRefNum(), ChangeHistoryService.STATUS_CHANGE, "quote_status");
    }
    main.setStatus(next);
    main.setUpdatedDate(Instant.now());
    changeHistoryService.record(
        ChangeHistoryService.QUOTE_MAIN,
        main.getQuoteRefNum(),
        firstNonBlank(main.getBookingRefNum(), main.getQuoteRefNum()),
        ChangeHistoryService.STATUS_CHANGE,
        "quote_status",
        previous,
        next,
        main.getCompanyId(),
        main.getCompanyName());
    log.info("Quote {} status {} -> {}", main.getQuoteRefNum(), previous, next);
    return quoteMainRepository.save(main);
  }

  @Override
  @Transactional
  public QuoteMainView updateFollowUp(String quoteRefNum, String followUp) {
    QuoteMain main = requiredQuote(quoteRefNum);
    String note = followUp == null ? "" : followUp.trim();
    if (note.length() > 500) {
      throw new IllegalArgumentException("Follow-up must be 500 characters or fewer");
    }
    String previous = main.getFollowUp();
    if (!sameText(previous, note)) {
      rowHistoryService.archive(
          EditableTable.QUOTE_MAIN, main.getQuoteRefNum(), ChangeHistoryService.FOLLOW_UP, "follow_up");
    }
    main.setFollowUp(note);
    main.setUpdatedDate(Instant.now());
    changeHistoryService.record(
        ChangeHistoryService.QUOTE_MAIN,
        main.getQuoteRefNum(),
        main.getQuoteRefNum(),
        ChangeHistoryService.FOLLOW_UP,
        "follow_up",
        previous,
        note,
        main.getCompanyId(),
        main.getCompanyName());
    log.info("Quote {} follow-up updated ({} chars)", main.getQuoteRefNum(), note.length());
    return toView(quoteMainRepository.save(main));
  }

  @Override
  @Transactional(readOnly = true)
  public List<QuoteMainView> listByCompany(String companyId) {
    if (companyId == null || companyId.isBlank()) {
      return List.of();
    }
    return quoteMainRepository.findByCompanyIdOrderByCreatedDateDesc(companyId.trim()).stream()
        .map(this::toView)
        .toList();
  }

  @Override
  @Transactional
  public List<QuoteMainView> listVisibleTo(AuthSession auth) {
    if (auth == null) {
      return List.of();
    }
    Set<String> viewerKeys = viewerUserKeys(auth);
    Map<String, QuoteMain> found = new LinkedHashMap<>();
    if (hasText(auth.companyId())) {
      for (QuoteMain quote : quoteMainRepository.findByCompanyIdOrderByCreatedDateDesc(auth.companyId().trim())) {
        if (canView(quote, auth, viewerKeys)) {
          found.put(quote.getQuoteRefNum(), quote);
        }
      }
    }
    if (!viewerKeys.isEmpty()) {
      for (QuoteMain quote : quoteMainRepository.findByNormalizedUserIdIn(viewerKeys)) {
        if (canView(quote, auth, viewerKeys) && !found.containsKey(quote.getQuoteRefNum())) {
          found.put(quote.getQuoteRefNum(), quote);
        }
      }
    }
    List<QuoteMainView> views = new ArrayList<>();
    found.values().stream()
        .sorted(Comparator.comparing(QuoteMain::getCreatedDate, Comparator.nullsLast(Comparator.reverseOrder())))
        .forEach(
            quote -> {
              reclaimOwner(quote, auth);
              views.add(toView(quote).forViewer(auth.companyId()));
            });
    return views;
  }

  @Override
  @Transactional
  public QuoteMainView requireVisible(AuthSession auth, String quoteRef) {
    return toView(requireVisibleEntity(auth, quoteRef)).forViewer(auth == null ? null : auth.companyId());
  }

  @Override
  @Transactional
  public QuoteMain requireVisibleEntity(AuthSession auth, String quoteRef) {
    QuoteMain main = requiredQuote(quoteRef);
    if (!canView(main, auth, viewerUserKeys(auth))) {
      throw new UnauthorizedException("Quote not found");
    }
    reclaimOwner(main, auth);
    return main;
  }

  @Override
  @Transactional(readOnly = true)
  public List<QuoteMainView> listAll() {
    return quoteMainRepository.findAllByOrderByCreatedDateDesc().stream().map(this::toView).toList();
  }

  @Override
  @Transactional(readOnly = true)
  public QuoteMainView get(String quoteRefNum) {
    return toView(requiredQuote(quoteRefNum));
  }

  @Override
  @Transactional(readOnly = true)
  public QuoteMainView getForContentEdit(String quoteRefNum) {
    QuoteMainView quote = get(quoteRefNum);
    if (quote.details() == null || quote.details().isEmpty()) {
      return quote;
    }
    return quote.withDetails(quote.details().stream().map(this::seedDisplayContent).toList());
  }

  private QuoteMain persist(ParkQuote quote, ParkQuoteRequest request, AuthSession auth, String forcedStatus) {
    if (quote == null || quote.getQuoteNumber() == null || quote.getQuoteNumber().isBlank()) {
      throw new IllegalArgumentException("Quote number is required");
    }
    if (quote.getItems() == null || quote.getItems().isEmpty()) {
      throw new IllegalArgumentException("Nothing to quote");
    }
    Instant now = Instant.now();
    String quoteRef = quote.getQuoteNumber().trim().toUpperCase(Locale.ROOT);
    QuoteMain existing = quoteMainRepository.findById(quoteRef).orElse(null);
    boolean created = existing == null;
    QuoteMain main = existing == null ? new QuoteMain() : existing;
    String previousStatus = main.getStatus();
    String previousValue = main.getValue() == null ? "" : main.getValue().toPlainString();
    String previousItinerary = itinerarySummary(main);

    String userName = resolveUserName(request, auth, quote);
    String userId = resolveUserId(request, auth);
    String companyName = resolveCompanyName(auth, quote);
    String companyId = resolveCompanyId(auth);
    String customerName = firstNonBlank(request == null ? null : request.getLeadName());
    String customerEmail = firstNonBlank(request == null ? null : request.getEmail());
    String customerPhone = firstNonBlank(request == null ? null : request.getPhone());

    List<QuoteDetail> previousDetails = created ? List.of() : List.copyOf(main.getDetails());
    List<DetailSnapshot> previousSnapshots = snapshotDetails(previousDetails);
    if (!created) {
      if (hasText(main.getBookingRefNum())
          || QuotePresentation.ACCEPTED.equals(QuotePresentation.normalize(main.getStatus()))
          || QuotePresentation.BOOKED.equals(QuotePresentation.normalize(main.getStatus()))) {
        throw new IllegalStateException("This quote already has a booking and cannot be edited.");
      }
      if (!canView(main, auth, viewerUserKeys(auth))) {
        throw new UnauthorizedException("Quote not found");
      }
      reclaimOwner(main, auth);
      rowHistoryService.archive(
          EditableTable.QUOTE_MAIN, main.getQuoteRefNum(), ChangeHistoryService.QUOTE_EDIT, "quote_value,itinerary");
      for (QuoteDetail detail : previousDetails) {
        if (detail.getId() != null) {
          rowHistoryService.archive(
              EditableTable.QUOTE_DETAIL, detail.getId(), ChangeHistoryService.QUOTE_EDIT, "item_content");
        }
      }
    }

    main.setQuoteRefNum(quoteRef);
    if (created) {
      main.setUserName(userName);
      main.setUserId(userId);
    }
    main.setCompanyName(companyName);
    main.setCompanyId(companyId);
    boolean incomingDirect = quote.isDirectCustomer() || (request != null && request.isDirectCustomer());
    boolean direct = incomingDirect || main.isDirectCustomer();
    if (created || incomingDirect) {
      main.setCustomerName(customerName);
      main.setCustomerEmail(customerEmail);
      main.setCustomerPhone(customerPhone);
    }
    main.setValue(money(quote.getAedTotal()));
    main.setCurrency(firstNonBlank(quote.getCurrency(), "AED"));
    main.setRoeRate(rateMoney(quote.getRoeRate()));
    main.setConvertedValue(money(quote.getConvertedValue()));
    main.setMarkupPercent(money(quote.getMarkupPercent()));
    main.setSubtotal(money(quote.getAedSubtotal()));
    main.setMarkupAmount(money(quote.getAedMarkupAmount()));
    main.setDirectCustomer(direct);
    if (incomingDirect) {
      main.setPackageInclusions(firstNonBlank(quote.getPackageInclusions()));
      main.setPackageExclusions(firstNonBlank(quote.getPackageExclusions()));
      main.setGuestInformation(firstNonBlank(quote.getGuestInformation()));
      main.setDayPlansJson(writeDayPlans(quote.getDayPlans()));
    }
    main.setUpdatedDate(now);
    LocalDate expire =
        request != null && request.getExpireDate() != null
            ? request.getExpireDate()
            : quote.getExpireDate();
    if (created) {
      main.setCreatedDate(now);
      main.setExpireDate(BookingPresentation.quoteExpireDate(expire, now));
      main.setStatus(QuotePresentation.normalize(forcedStatus == null ? QuotePresentation.DRAFT : forcedStatus));
    } else {
      if (forcedStatus != null) {
        main.setStatus(QuotePresentation.normalize(forcedStatus));
      } else if (main.getStatus() == null || main.getStatus().isBlank()) {
        main.setStatus(QuotePresentation.DRAFT);
      }
      if (expire != null) {
        main.setExpireDate(expire);
      } else if (main.getExpireDate() == null) {
        main.setExpireDate(main.resolvedExpireDate());
      }
    }

    main.getDetails().clear();
    boolean[] usedPrevious = new boolean[previousDetails.size()];
    for (ParkQuoteLine line : quote.getItems()) {
      main.addDetail(detailFrom(line, now, takePrevious(previousDetails, usedPrevious, line)));
    }

    QuoteMain saved = quoteMainRepository.save(main);
    if (created) {
      changeHistoryService.record(
          ChangeHistoryService.QUOTE_MAIN,
          saved.getQuoteRefNum(),
          saved.getQuoteRefNum(),
          ChangeHistoryService.CREATE,
          "quote_status",
          "",
          saved.getStatus(),
          saved.getCompanyId(),
          saved.getCompanyName());
      for (QuoteDetail detail : saved.getDetails()) {
        changeHistoryService.record(
            ChangeHistoryService.QUOTE_DETAIL,
            String.valueOf(detail.getId()),
            saved.getQuoteRefNum(),
            ChangeHistoryService.CREATE,
            "item_name",
            "",
            detail.getItemName(),
            saved.getCompanyId(),
            saved.getCompanyName());
      }
    } else {
      String nextValue = saved.getValue() == null ? "" : saved.getValue().toPlainString();
      String nextItinerary = itinerarySummary(saved);
      if (!sameText(previousValue, nextValue)) {
        changeHistoryService.record(
            ChangeHistoryService.QUOTE_MAIN,
            saved.getQuoteRefNum(),
            saved.getQuoteRefNum(),
            ChangeHistoryService.QUOTE_EDIT,
            "quote_value",
            previousValue,
            nextValue,
            saved.getCompanyId(),
            saved.getCompanyName());
      }
      if (!sameText(previousItinerary, nextItinerary)) {
        changeHistoryService.record(
            ChangeHistoryService.QUOTE_MAIN,
            saved.getQuoteRefNum(),
            saved.getQuoteRefNum(),
            ChangeHistoryService.QUOTE_EDIT,
            "itinerary",
            previousItinerary,
            nextItinerary,
            saved.getCompanyId(),
            saved.getCompanyName());
      }
      recordItemFieldDiffs(previousSnapshots, saved.getDetails(), saved);
      if (forcedStatus != null && !sameText(previousStatus, saved.getStatus())) {
        changeHistoryService.record(
            ChangeHistoryService.QUOTE_MAIN,
            saved.getQuoteRefNum(),
            saved.getQuoteRefNum(),
            ChangeHistoryService.STATUS_CHANGE,
            "quote_status",
            previousStatus,
            saved.getStatus(),
            saved.getCompanyId(),
            saved.getCompanyName());
      }
    }
    return saved;
  }

  private QuoteMain requiredQuote(String quoteRefNum) {
    if (quoteRefNum == null || quoteRefNum.isBlank()) {
      throw new QuoteNotFoundException("");
    }
    return quoteMainRepository
        .findById(quoteRefNum.trim().toUpperCase(Locale.ROOT))
        .orElseThrow(() -> new QuoteNotFoundException(quoteRefNum));
  }

  private static String requireEditableStatus(String status) {
    String normalized = QuotePresentation.normalize(status);
    if (!QuotePresentation.isEditable(normalized)) {
      throw new IllegalArgumentException("Unsupported quote status");
    }
    return normalized;
  }

  @Override
  public QuoteMainView toView(QuoteMain main) {
    List<QuoteDetailView> details =
        main.getDetails() == null
            ? List.of()
            : main.getDetails().stream().map(detail -> toDetailView(main, detail)).toList();
    return new QuoteMainView(
        main.getQuoteRefNum(),
        main.getUserName(),
        main.getUserId(),
        main.getCompanyName(),
        main.getCompanyId(),
        main.getValue(),
        main.getCurrency(),
        main.getCreatedDate(),
        main.getUpdatedDate(),
        QuotePresentation.normalize(main.getStatus()),
        main.getCustomerName(),
        main.getCustomerEmail(),
        main.getCustomerPhone(),
        main.getFollowUp(),
        main.getSentTo(),
        main.getSentAt(),
        main.getMarkupPercent(),
        main.getSubtotal(),
        main.getMarkupAmount(),
        main.getRoeRate(),
        main.getConvertedValue(),
        main.getBookingRefNum(),
        canEditItems(main),
        details,
        main.getPackageInclusions(),
        main.getPackageExclusions(),
        main.getGuestInformation(),
        readDayPlans(main.getDayPlansJson()),
        main.isDirectCustomer(),
        main.resolvedExpireDate());
  }

  private static boolean canEditItems(QuoteMain main) {
    if (main == null) {
      return false;
    }
    String status = QuotePresentation.normalize(main.getStatus());
    return !hasText(main.getBookingRefNum())
        && !QuotePresentation.ACCEPTED.equals(status)
        && !QuotePresentation.BOOKED.equals(status);
  }

  private static QuoteDetailView toDetailView(QuoteMain main, QuoteDetail detail) {
    return new QuoteDetailView(
        detail.getId(),
        main.getQuoteRefNum(),
        detail.getItemId(),
        detail.getItemName(),
        detail.getKind(),
        detail.getCategory(),
        detail.getVisitDate(),
        detail.getCheckoutDate(),
        detail.getAdultCount(),
        detail.getChildCount(),
        detail.getRooms(),
        detail.getRoomType(),
        detail.getTimeSlot(),
        detail.getLineTotal(),
        detail.getAdultRate(),
        detail.getChildRate(),
        detail.getActualLineTotal(),
        detail.getActualAdultRate(),
        detail.getActualChildRate(),
        detail.getDescription(),
        detail.getTermsAndCondition(),
        detail.getIncluded(),
        detail.getExcluded(),
        detail.getRoeRate(),
        detail.getConvertedRate(),
        detail.getActualConvertedRate(),
        main.getCurrency());
  }

  private static String itinerarySummary(QuoteMain main) {
    if (main == null || main.getDetails() == null || main.getDetails().isEmpty()) {
      return "";
    }
    StringBuilder summary = new StringBuilder();
    for (QuoteDetail detail : main.getDetails()) {
      if (summary.length() > 0) {
        summary.append("; ");
      }
      summary.append(firstNonBlank(detail.getItemName(), detail.getItemId(), "Item"));
      if (detail.getVisitDate() != null) {
        summary.append(" ").append(BookingPresentation.formatTravel(detail.getVisitDate()));
      }
      if (detail.getCheckoutDate() != null) {
        summary.append(" → ").append(BookingPresentation.formatTravel(detail.getCheckoutDate()));
      }
    }
    return summary.toString();
  }

  private QuoteDetail detailFrom(ParkQuoteLine line, Instant created, QuoteDetail previous) {
    QuoteDetail detail = new QuoteDetail();
    detail.setItemId(firstNonBlank(line.getItemId(), "UNKNOWN"));
    detail.setItemName(itemName(line));
    detail.setKind(firstNonBlank(line.getKind()));
    detail.setCategory(firstNonBlank(line.getCategory()));
    detail.setVisitDate(parseDate(line.getVisitDate()));
    detail.setCheckoutDate(parseDate(line.getCheckOut()));
    detail.setAdultCount(Math.max(0, line.getAdults()));
    detail.setChildCount(Math.max(0, line.getChildren()));
    detail.setRooms(Math.max(0, line.getRooms()));
    detail.setRoomType(firstNonBlank(line.getRoomType()));
    detail.setTimeSlot(firstNonBlank(line.getTimeSlot()));
    detail.setLineTotal(money(line.getAedLineTotal()));
    detail.setRoeRate(rateMoney(line.getRoeRate()));
    detail.setConvertedRate(money(line.getConvertedRate()));
    detail.setAdultRate(
        rateFrom(
            line.getAedAdultPrice() != null ? line.getAedAdultPrice() : line.getAdultPrice(),
            previous == null ? null : previous.getAdultRate()));
    detail.setChildRate(
        rateFrom(
            line.getAedChildPrice() != null ? line.getAedChildPrice() : line.getChildPrice(),
            previous == null ? null : previous.getChildRate()));
    ItemMaster master = findMaster(line.getItemId(), line.getName());
    detail.setActualAdultRate(
        firstRate(
            rateFrom(
                line.getAedActualAdultPrice() != null
                    ? line.getAedActualAdultPrice()
                    : line.getActualAdultPrice(),
                previous == null ? null : previous.getActualAdultRate()),
            master == null ? null : master.getAdultRate(),
            detail.getAdultRate()));
    detail.setActualChildRate(
        firstRate(
            rateFrom(
                line.getAedActualChildPrice() != null
                    ? line.getAedActualChildPrice()
                    : line.getActualChildPrice(),
                previous == null ? null : previous.getActualChildRate()),
            master == null ? null : master.getChildRate(),
            detail.getChildRate()));
    detail.setActualLineTotal(
        firstRate(
            moneyOrNull(line.getAedActualLineTotal()),
            previous == null ? null : previous.getActualLineTotal(),
            detail.getLineTotal()));
    detail.setActualConvertedRate(
        firstRate(
            moneyOrNull(line.getActualLineTotal()),
            previous == null ? null : previous.getActualConvertedRate(),
            detail.getConvertedRate()));
    detail.setDescription(
        firstNonBlank(
            line.getDescription(),
            previous == null ? null : previous.getDescription(),
            master == null ? null : master.getDescription()));
    detail.setTermsAndCondition(
        firstNonBlank(
            line.getTermsAndCondition(),
            previous == null ? null : previous.getTermsAndCondition(),
            master == null ? null : master.getTermsAndCondition()));
    detail.setIncluded(
        firstNonBlank(
            line.getIncluded(),
            previous == null ? null : previous.getIncluded(),
            master == null ? null : master.getIncluded()));
    detail.setExcluded(
        firstNonBlank(
            line.getExcluded(),
            previous == null ? null : previous.getExcluded(),
            master == null ? null : master.getExcluded()));
    detail.setCreatedDate(created);
    return detail;
  }

  private QuoteDetailView seedDisplayContent(QuoteDetailView detail) {
    ItemMaster master = findMaster(detail.itemId(), detail.itemName());
    if (master == null) {
      return detail;
    }
    return detail.withContentDefaults(
        master.getDescription(),
        master.getTermsAndCondition(),
        master.getIncluded(),
        master.getExcluded(),
        master.getAdultRate(),
        master.getChildRate());
  }

  private ItemMaster findMaster(String itemId, String itemName) {
    if (itemId != null && !itemId.isBlank()) {
      ItemMaster byId = itemMasterRepository.findById(itemId.trim()).orElse(null);
      if (byId != null) {
        return byId;
      }
    }
    if (itemName == null || itemName.isBlank()) {
      return null;
    }
    String name = itemName.trim();
    int dash = name.indexOf(" — ");
    if (dash > 0) {
      name = name.substring(0, dash).trim();
    }
    int slot = name.indexOf(" · ");
    if (slot > 0) {
      name = name.substring(0, slot).trim();
    }
    return itemMasterRepository.findByItemNameIgnoreCase(name).orElse(null);
  }

  private static QuoteDetail takePrevious(List<QuoteDetail> previous, boolean[] used, ParkQuoteLine line) {
    if (previous == null || previous.isEmpty() || line == null) {
      return null;
    }
    LocalDate visit = parseDate(line.getVisitDate());
    for (int i = 0; i < previous.size(); i++) {
      if (used[i]) {
        continue;
      }
      QuoteDetail candidate = previous.get(i);
      if (sameText(candidate.getItemId(), line.getItemId()) && Objects.equals(candidate.getVisitDate(), visit)) {
        used[i] = true;
        return candidate;
      }
    }
    for (int i = 0; i < previous.size(); i++) {
      if (used[i]) {
        continue;
      }
      QuoteDetail candidate = previous.get(i);
      if (sameText(candidate.getItemId(), line.getItemId())) {
        used[i] = true;
        return candidate;
      }
    }
    return null;
  }

  private void recordItemFieldDiffs(List<DetailSnapshot> previous, List<QuoteDetail> next, QuoteMain saved) {
    if (saved == null || next == null || next.isEmpty() || previous == null || previous.isEmpty()) {
      return;
    }
    boolean[] used = new boolean[previous.size()];
    for (QuoteDetail detail : next) {
      DetailSnapshot before = takeSnapshot(previous, used, detail);
      if (before == null) {
        continue;
      }
      String item = firstNonBlank(detail.getItemName(), before.itemName(), "Item");
      recordIfChanged(saved, item, "description", before.description(), detail.getDescription());
      recordIfChanged(saved, item, "terms_and_condition", before.terms(), detail.getTermsAndCondition());
      recordIfChanged(saved, item, "included", before.included(), detail.getIncluded());
      recordIfChanged(saved, item, "excluded", before.excluded(), detail.getExcluded());
      recordIfChanged(saved, item, "adult_rate", before.adultRate(), moneyText(detail.getAdultRate()));
      recordIfChanged(saved, item, "child_rate", before.childRate(), moneyText(detail.getChildRate()));
      recordIfChanged(saved, item, "line_total", before.lineTotal(), moneyText(detail.getLineTotal()));
    }
  }

  private void recordIfChanged(QuoteMain saved, String itemName, String field, String oldValue, String newValue) {
    String from = clip(oldValue);
    String to = clip(newValue);
    if (sameText(from, to)) {
      return;
    }
    changeHistoryService.record(
        ChangeHistoryService.QUOTE_MAIN,
        saved.getQuoteRefNum(),
        saved.getQuoteRefNum(),
        ChangeHistoryService.QUOTE_EDIT,
        quoteFieldName(itemName, field),
        from,
        to,
        saved.getCompanyId(),
        saved.getCompanyName());
  }

  private static List<DetailSnapshot> snapshotDetails(List<QuoteDetail> details) {
    if (details == null || details.isEmpty()) {
      return List.of();
    }
    List<DetailSnapshot> rows = new ArrayList<>();
    for (QuoteDetail detail : details) {
      rows.add(
          new DetailSnapshot(
              detail.getItemId(),
              detail.getVisitDate(),
              firstNonBlank(detail.getItemName(), detail.getItemId(), "Item"),
              detail.getDescription(),
              detail.getTermsAndCondition(),
              detail.getIncluded(),
              detail.getExcluded(),
              moneyText(detail.getAdultRate()),
              moneyText(detail.getChildRate()),
              moneyText(detail.getLineTotal())));
    }
    return rows;
  }

  private static DetailSnapshot takeSnapshot(List<DetailSnapshot> previous, boolean[] used, QuoteDetail detail) {
    if (previous == null || previous.isEmpty() || detail == null) {
      return null;
    }
    for (int i = 0; i < previous.size(); i++) {
      if (used[i]) {
        continue;
      }
      DetailSnapshot candidate = previous.get(i);
      if (sameText(candidate.itemId(), detail.getItemId())
          && Objects.equals(candidate.visitDate(), detail.getVisitDate())) {
        used[i] = true;
        return candidate;
      }
    }
    for (int i = 0; i < previous.size(); i++) {
      if (used[i]) {
        continue;
      }
      DetailSnapshot candidate = previous.get(i);
      if (sameText(candidate.itemId(), detail.getItemId())) {
        used[i] = true;
        return candidate;
      }
    }
    return null;
  }

  private static String quoteFieldName(String itemName, String field) {
    String item = firstNonBlank(itemName, "Item");
    int maxItem = Math.max(8, 80 - 3 - field.length());
    if (item.length() > maxItem) {
      item = item.substring(0, maxItem - 1) + "…";
    }
    return item + " · " + field;
  }

  private static String moneyText(BigDecimal value) {
    return value == null ? "" : value.stripTrailingZeros().toPlainString();
  }

  private static String clip(String value) {
    String text = value == null ? "" : value;
    return text.length() <= 2000 ? text : text.substring(0, 1997) + "...";
  }

  private record DetailSnapshot(
      String itemId,
      LocalDate visitDate,
      String itemName,
      String description,
      String terms,
      String included,
      String excluded,
      String adultRate,
      String childRate,
      String lineTotal) {}

  private static BigDecimal rateFrom(Double value, BigDecimal previous) {
    if (value != null) {
      return money(value);
    }
    return previous;
  }

  private static BigDecimal moneyOrNull(double value) {
    if (value == 0) {
      return null;
    }
    return money(value);
  }

  private static BigDecimal firstRate(BigDecimal... values) {
    if (values == null) {
      return null;
    }
    for (BigDecimal value : values) {
      if (value != null) {
        return value;
      }
    }
    return null;
  }

  private static String itemName(ParkQuoteLine line) {
    StringBuilder name = new StringBuilder(firstNonBlank(line.getName(), line.getItemId(), "Item"));
    if (line.getRoomType() != null && !line.getRoomType().isBlank()) {
      name.append(" — ").append(line.getRoomType().trim());
    }
    if (line.getTimeSlot() != null && !line.getTimeSlot().isBlank()) {
      name.append(" · ").append(line.getTimeSlot().trim());
    }
    return name.toString();
  }

  private static String resolveUserName(ParkQuoteRequest request, AuthSession auth, ParkQuote quote) {
    return firstNonBlank(
        request == null ? null : request.getOwnerUserName(),
        quote == null ? null : quote.getPreparedBy(),
        auth == null ? null : auth.userName(),
        auth == null ? null : auth.email(),
        "Guest");
  }

  private static String resolveUserId(ParkQuoteRequest request, AuthSession auth) {
    return firstNonBlank(
        request == null ? null : request.getUserId(),
        auth == null ? null : auth.email(),
        auth == null ? null : auth.userId(),
        "GUEST");
  }

  private static String resolveCompanyName(AuthSession auth, ParkQuote quote) {
    String companyName =
        firstNonBlank(
            auth == null ? null : auth.companyName(),
            quote == null ? null : quote.getCompanyName());
    if (!companyName.isBlank()) {
      return companyName;
    }
    return auth == null ? DEFAULT_COMPANY_NAME : firstNonBlank(auth.companyName(), DEFAULT_COMPANY_NAME);
  }

  private static String resolveCompanyId(AuthSession auth) {
    return firstNonBlank(auth == null ? null : auth.companyId(), DEFAULT_COMPANY_ID);
  }

  private boolean canView(QuoteMain main, AuthSession auth, Set<String> viewerKeys) {
    if (auth == null || main == null) {
      return false;
    }
    if (createdByViewerCompany(main, viewerKeys)) {
      return true;
    }
    return sameText(main.getCompanyId(), auth.companyId()) && !hasText(main.getUserId());
  }

  private boolean createdByViewerCompany(QuoteMain main, Set<String> viewerKeys) {
    if (main == null || !hasText(main.getUserId()) || viewerKeys == null || viewerKeys.isEmpty()) {
      return false;
    }
    return viewerKeys.contains(main.getUserId().trim().toLowerCase(Locale.ROOT));
  }

  private void reclaimOwner(QuoteMain main, AuthSession auth) {
    if (main == null || auth == null) {
      return;
    }
    String companyId = resolveCompanyId(auth);
    String companyName = firstNonBlank(auth.companyName());
    boolean idChanged = hasText(companyId) && !sameText(main.getCompanyId(), companyId);
    boolean nameChanged = hasText(companyName) && !sameText(main.getCompanyName(), companyName);
    if (!idChanged && !nameChanged) {
      return;
    }
    main.setCompanyId(companyId);
    if (nameChanged) {
      main.setCompanyName(companyName);
    }
    quoteMainRepository.save(main);
  }

  private Set<String> viewerUserKeys(AuthSession auth) {
    Set<String> keys = new LinkedHashSet<>();
    addKey(keys, auth == null ? null : auth.email());
    addKey(keys, auth == null ? null : auth.userId());
    Long companyPk = tryParseCompanyId(auth == null ? null : auth.companyId());
    if (companyPk != null) {
      for (AppUser user : userRepository.findByCompanyIdOrderByCreatedDateAsc(companyPk)) {
        if (user == null) {
          continue;
        }
        addKey(keys, user.getEmail());
        addKey(keys, user.getUserId());
      }
    }
    return keys;
  }

  private static void addKey(Set<String> keys, String value) {
    if (keys == null || !hasText(value)) {
      return;
    }
    keys.add(value.trim().toLowerCase(Locale.ROOT));
  }

  private static Long tryParseCompanyId(String value) {
    if (!hasText(value)) {
      return null;
    }
    try {
      return Long.valueOf(value.trim());
    } catch (NumberFormatException ex) {
      return null;
    }
  }

  private static LocalDate parseDate(String value) {
    if (value == null || value.isBlank()) {
      return null;
    }
    try {
      return LocalDate.parse(value.trim());
    } catch (Exception ex) {
      return null;
    }
  }

  private static BigDecimal money(double value) {
    return BigDecimal.valueOf(value).setScale(2, RoundingMode.HALF_UP);
  }

  private static BigDecimal rateMoney(double value) {
    return BigDecimal.valueOf(value).setScale(6, RoundingMode.HALF_UP);
  }

  private static boolean sameText(String left, String right) {
    return Objects.equals(left == null ? "" : left.trim(), right == null ? "" : right.trim());
  }

  private static boolean hasText(String value) {
    return value != null && !value.isBlank();
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

  private String writeDayPlans(List<ParkDayPlan> plans) {
    if (plans == null || plans.isEmpty()) {
      return "";
    }
    try {
      return objectMapper.writeValueAsString(plans);
    } catch (Exception ex) {
      return "";
    }
  }

  private List<ParkDayPlan> readDayPlans(String json) {
    if (json == null || json.isBlank()) {
      return List.of();
    }
    try {
      ParkDayPlan[] plans = objectMapper.readValue(json, ParkDayPlan[].class);
      return plans == null ? List.of() : Arrays.asList(plans);
    } catch (Exception ex) {
      return List.of();
    }
  }
}
