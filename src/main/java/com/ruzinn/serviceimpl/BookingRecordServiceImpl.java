package com.ruzinn.serviceimpl;

import com.ruzinn.dto.BookingDetailView;
import com.ruzinn.dto.BookingDocument;
import com.ruzinn.dto.BookingMainView;
import com.ruzinn.dto.ParkPassenger;
import com.ruzinn.dto.ParkQuote;
import com.ruzinn.dto.ParkQuoteLine;
import com.ruzinn.dto.ParkQuoteRequest;
import com.ruzinn.entity.BookingDetail;
import com.ruzinn.entity.BookingMain;
import com.ruzinn.entity.QuoteDetail;
import com.ruzinn.entity.QuoteMain;
import com.ruzinn.enums.EditableTable;
import com.ruzinn.exception.BookingNotFoundException;
import com.ruzinn.repository.BookingMainRepository;
import com.ruzinn.repository.CompanyRepository;
import com.ruzinn.service.BookingConfirmationMailService;
import com.ruzinn.service.BookingDocumentStore;
import com.ruzinn.service.BookingRecordService;
import com.ruzinn.service.ChangeHistoryService;
import com.ruzinn.service.RowHistoryService;
import com.ruzinn.util.BookingPresentation;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;

@Service
public class BookingRecordServiceImpl implements BookingRecordService {

  private static final Logger log = LoggerFactory.getLogger(BookingRecordServiceImpl.class);

  static final String DEFAULT_COMPANY_NAME = "Direct";
  static final String DEFAULT_COMPANY_ID = "DIRECT";
  static final String SUPPLIER_NAME = "Rathin";
  static final String SUPPLIER_ID = "WGT";

  private final BookingMainRepository bookingMainRepository;
  private final BookingDocumentStore documentStore;
  private final ChangeHistoryService changeHistoryService;
  private final RowHistoryService rowHistoryService;
  private final CompanyRepository companyRepository;
  private final ObjectMapper objectMapper;
  private final BookingConfirmationMailService bookingMailService;

  public BookingRecordServiceImpl(
      BookingMainRepository bookingMainRepository,
      BookingDocumentStore documentStore,
      ChangeHistoryService changeHistoryService,
      RowHistoryService rowHistoryService,
      CompanyRepository companyRepository,
      ObjectMapper objectMapper,
      BookingConfirmationMailService bookingMailService) {
    this.bookingMainRepository = bookingMainRepository;
    this.documentStore = documentStore;
    this.changeHistoryService = changeHistoryService;
    this.rowHistoryService = rowHistoryService;
    this.companyRepository = companyRepository;
    this.objectMapper = objectMapper;
    this.bookingMailService = bookingMailService;
  }

  /** Emails the guest "your booking is confirmed" when the booking has just become CONFIRMED. */
  private void mailIfJustConfirmed(BookingMain main, String previousStatus) {
    boolean wasConfirmed = BookingPresentation.CONFIRMED.equals(BookingPresentation.normalize(previousStatus));
    boolean isConfirmed = BookingPresentation.CONFIRMED.equals(BookingPresentation.normalize(main.getStatus()));
    if (isConfirmed && !wasConfirmed) {
      bookingMailService.sendAfterCommit(main, null);
      log.info("Booking {} confirmed; confirmation mail queued", main.getBookingRefNum());
    }
  }

  @Override
  @Transactional
  public BookingMain saveConfirmed(String bookingRefNum, ParkQuote quote, ParkQuoteRequest request) {
    if (bookingRefNum == null || bookingRefNum.isBlank()) {
      throw new IllegalArgumentException("Booking reference is required");
    }
    if (quote == null || quote.getItems() == null || quote.getItems().isEmpty()) {
      throw new IllegalArgumentException("Nothing to book");
    }
    Instant now = Instant.now();
    String userName = requiredName(request);
    String userId = resolveUserId(request);
    String companyName = firstNonBlank(request == null ? null : request.getCompanyName(), DEFAULT_COMPANY_NAME);
    String companyId = firstNonBlank(request == null ? null : request.getCompanyId(), DEFAULT_COMPANY_ID);

    BookingMain main = new BookingMain();
    main.setBookingRefNum(bookingRefNum.trim().toUpperCase());
    main.setUserName(userName);
    main.setUserId(userId);
    main.setCompanyName(companyName);
    main.setCompanyId(companyId);
    main.setValue(money(quote.getAedTotal()));
    main.setCurrency(firstNonBlank(quote.getCurrency(), "AED"));
    main.setRoeRate(rateMoney(quote.getRoeRate()));
    main.setConvertedValue(money(quote.getConvertedValue()));
    main.setLeadEmail(firstNonBlank(request == null ? null : request.getEmail(), looksLikeEmail(userId) ? userId : ""));
    main.setLeadPhone(firstNonBlank(request == null ? null : request.getPhone()));
    main.setCreatedDate(now);

    for (ParkQuoteLine line : quote.getItems()) {
      BookingDetail detail = detailFrom(line, now, userName, userId, companyName, companyId);
      detail.setPassengers(passengersJson(line));
      main.addDetail(detail);
    }
    log.info(
        "Saving booking {} for company {} ({}), {} item(s), value AED {}",
        main.getBookingRefNum(), companyId, companyName, main.getDetails().size(), main.getValue());
    String initialStatus = BookingPresentation.PENDING;
    main.setStatus(initialStatus);
    for (BookingDetail detail : main.getDetails()) {
      detail.setStatus(initialStatus);
    }
    BookingMain saved = bookingMainRepository.save(main);
    changeHistoryService.record(
        ChangeHistoryService.BOOKING_MAIN,
        saved.getBookingRefNum(),
        saved.getBookingRefNum(),
        ChangeHistoryService.CREATE,
        "booking_status",
        "",
        saved.getStatus(),
        saved.getCompanyId(),
        saved.getCompanyName());
    for (BookingDetail detail : saved.getDetails()) {
      changeHistoryService.record(
          ChangeHistoryService.BOOKING_DETAIL,
          String.valueOf(detail.getId()),
          saved.getBookingRefNum(),
          ChangeHistoryService.CREATE,
          "item_name",
          "",
          detail.getItemName(),
          saved.getCompanyId(),
          saved.getCompanyName());
    }
    return saved;
  }

  @Override
  @Transactional
  public BookingMain saveFromQuote(QuoteMain quote) {
    if (quote == null || quote.getDetails() == null || quote.getDetails().isEmpty()) {
      throw new IllegalArgumentException("Add items before accepting this quote");
    }
    if (quote.getBookingRefNum() != null && !quote.getBookingRefNum().isBlank()) {
      return requiredBooking(quote.getBookingRefNum());
    }
    Instant now = Instant.now();
    String userName = firstNonBlank(quote.getCustomerName(), quote.getUserName(), "Guest");
    String userId = firstNonBlank(quote.getUserId(), quote.getCustomerEmail(), quote.getSentTo(), "GUEST");
    String companyName = firstNonBlank(quote.getCompanyName(), DEFAULT_COMPANY_NAME);
    String companyId = firstNonBlank(quote.getCompanyId(), DEFAULT_COMPANY_ID);

    BookingMain main = new BookingMain();
    main.setBookingRefNum(newBookingRef());
    main.setUserName(userName);
    main.setUserId(userId);
    main.setCompanyName(companyName);
    main.setCompanyId(companyId);
    main.setValue(quote.getValue() == null ? BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP) : quote.getValue());
    main.setCurrency(firstNonBlank(quote.getCurrency(), "AED"));
    main.setRoeRate(quote.getRoeRate());
    main.setConvertedValue(
        quote.getConvertedValue() == null ? main.getValue() : quote.getConvertedValue());
    main.setLeadEmail(firstNonBlank(quote.getCustomerEmail(), quote.getSentTo(), looksLikeEmail(userId) ? userId : ""));
    main.setLeadPhone(firstNonBlank(quote.getCustomerPhone()));
    main.setCreatedDate(now);

    for (QuoteDetail line : quote.getDetails()) {
      if (line == null) {
        continue;
      }
      main.addDetail(detailFromQuote(line, now, userName, userId, companyName, companyId));
    }
    if (main.getDetails().isEmpty()) {
      throw new IllegalArgumentException("Add items before accepting this quote");
    }
    log.info(
        "Saving booking {} from quote {} for company {} ({}), {} item(s)",
        main.getBookingRefNum(), quote.getQuoteRefNum(), companyId, companyName, main.getDetails().size());
    String initialStatus = BookingPresentation.PENDING;
    main.setStatus(initialStatus);
    for (BookingDetail detail : main.getDetails()) {
      detail.setStatus(initialStatus);
    }
    BookingMain saved = bookingMainRepository.save(main);
    changeHistoryService.record(
        ChangeHistoryService.BOOKING_MAIN,
        saved.getBookingRefNum(),
        saved.getBookingRefNum(),
        ChangeHistoryService.CREATE,
        "booking_status",
        "",
        saved.getStatus(),
        saved.getCompanyId(),
        saved.getCompanyName());
    for (BookingDetail detail : saved.getDetails()) {
      changeHistoryService.record(
          ChangeHistoryService.BOOKING_DETAIL,
          String.valueOf(detail.getId()),
          saved.getBookingRefNum(),
          ChangeHistoryService.CREATE,
          "item_name",
          "",
          detail.getItemName(),
          saved.getCompanyId(),
          saved.getCompanyName());
    }
    return saved;
  }

  @Override
  @Transactional
  public BookingMainView updateStatus(String bookingRefNum, String status) {
    BookingMain main = requiredBooking(bookingRefNum);
    String normalized = requireEditableStatus(status);
    String previousMain = main.getStatus();
    for (BookingDetail detail : main.getDetails()) {
      String previousItem = detail.getStatus();
      if (!sameText(previousItem, normalized)) {
        rowHistoryService.archive(
            EditableTable.BOOKING_DETAIL, detail.getId(), ChangeHistoryService.STATUS_CHANGE, "item_status");
      }
      detail.setStatus(normalized);
      recordItemStatus(main, detail, previousItem, normalized);
    }
    if (!sameText(previousMain, normalized)) {
      rowHistoryService.archive(
          EditableTable.BOOKING_MAIN, main.getBookingRefNum(), ChangeHistoryService.STATUS_CHANGE, "booking_status");
    }
    main.setStatus(normalized);
    recordMainStatus(main, previousMain, normalized);
    log.info(
        "Booking {} status {} -> {} ({} item(s) updated)",
        main.getBookingRefNum(), previousMain, normalized, main.getDetails().size());
    mailIfJustConfirmed(main, previousMain);
    return toView(bookingMainRepository.save(main));
  }

  @Override
  @Transactional
  public BookingMainView updateItemStatus(String bookingRefNum, Long itemId, String status) {
    BookingMain main = requiredBooking(bookingRefNum);
    BookingDetail item = requiredItem(main, itemId);
    String previousItem = item.getStatus();
    String previousMain = main.getStatus();
    String nextItem = requireEditableStatus(status);
    if (!sameText(previousItem, nextItem)) {
      rowHistoryService.archive(
          EditableTable.BOOKING_DETAIL, item.getId(), ChangeHistoryService.STATUS_CHANGE, "item_status");
    }
    item.setStatus(nextItem);
    syncRollup(main);
    if (!sameText(previousMain, main.getStatus())) {
      rowHistoryService.archive(
          EditableTable.BOOKING_MAIN, main.getBookingRefNum(), ChangeHistoryService.STATUS_CHANGE, "booking_status");
    }
    recordItemStatus(main, item, previousItem, item.getStatus());
    recordMainStatus(main, previousMain, main.getStatus());
    log.info(
        "Booking {} item {} status {} -> {}; booking status {} -> {}",
        main.getBookingRefNum(), item.getId(), previousItem, item.getStatus(), previousMain, main.getStatus());
    mailIfJustConfirmed(main, previousMain);
    return toView(bookingMainRepository.save(main));
  }

  @Override
  @Transactional
  public BookingMainView updateItemFollowUp(String bookingRefNum, Long itemId, String followUp) {
    BookingMain main = requiredBooking(bookingRefNum);
    BookingDetail item = requiredItem(main, itemId);
    String note = followUp == null ? "" : followUp.trim();
    if (note.length() > 500) {
      throw new IllegalArgumentException("Follow-up must be 500 characters or fewer");
    }
    String previous = item.getFollowUp();
    if (!sameText(previous, note)) {
      rowHistoryService.archive(
          EditableTable.BOOKING_DETAIL, item.getId(), ChangeHistoryService.FOLLOW_UP, "follow_up");
    }
    item.setFollowUp(note);
    changeHistoryService.record(
        ChangeHistoryService.BOOKING_DETAIL,
        String.valueOf(item.getId()),
        main.getBookingRefNum(),
        ChangeHistoryService.FOLLOW_UP,
        "follow_up",
        previous,
        note,
        main.getCompanyId(),
        main.getCompanyName());
    log.info("Booking {} item {} follow-up updated ({} chars)", main.getBookingRefNum(), item.getId(), note.length());
    return toView(bookingMainRepository.save(main));
  }

  @Override
  @Transactional
  public BookingMainView storeDocument(String bookingRefNum, String kind, String fileName, String contentType, byte[] bytes) {
    BookingMain main = requiredBooking(bookingRefNum);
    BookingDetail item = main.getDetails().isEmpty() ? null : main.getDetails().get(0);
    applyDocument(main, item, kind, fileName, contentType, bytes);
    log.info(
        "Booking {} document '{}' stored ({}, {} bytes)",
        main.getBookingRefNum(), kind, fileName, bytes == null ? 0 : bytes.length);
    return toView(bookingMainRepository.save(main));
  }

  @Override
  @Transactional
  public BookingMainView storeItemDocument(
      String bookingRefNum, Long itemId, String kind, String fileName, String contentType, byte[] bytes) {
    BookingMain main = requiredBooking(bookingRefNum);
    applyDocument(main, requiredItem(main, itemId), kind, fileName, contentType, bytes);
    log.info(
        "Booking {} item {} document '{}' stored ({}, {} bytes)",
        main.getBookingRefNum(), itemId, kind, fileName, bytes == null ? 0 : bytes.length);
    return toView(bookingMainRepository.save(main));
  }

  @Override
  @Transactional(readOnly = true)
  public BookingDocument loadDocument(String bookingRefNum, String kind) {
    BookingMain main = requiredBooking(bookingRefNum);
    BookingDocument document = findDocument(main, null, kind);
    if (document == null) {
      throw new BookingNotFoundException(bookingRefNum + " " + normalizeKind(kind));
    }
    return document;
  }

  @Override
  @Transactional(readOnly = true)
  public BookingDocument loadItemDocument(String bookingRefNum, Long itemId, String kind) {
    BookingMain main = requiredBooking(bookingRefNum);
    BookingDocument document = findDocument(main, requiredItem(main, itemId), kind);
    if (document == null) {
      throw new BookingNotFoundException(bookingRefNum + " item " + itemId + " " + normalizeKind(kind));
    }
    return document;
  }

  @Override
  @Transactional(readOnly = true)
  public java.util.Optional<BookingDocument> findPreferredDocument(String bookingRefNum) {
    BookingMain main = requiredBooking(bookingRefNum);
    BookingDocument ticket = findDocument(main, null, "ticket");
    if (ticket != null) {
      return java.util.Optional.of(ticket);
    }
    BookingDocument voucher = findDocument(main, null, "voucher");
    return java.util.Optional.ofNullable(voucher);
  }

  @Override
  @Transactional(readOnly = true)
  public java.util.Optional<BookingDocument> findPreferredItemDocument(String bookingRefNum, Long itemId) {
    BookingMain main = requiredBooking(bookingRefNum);
    BookingDetail item = requiredItem(main, itemId);
    return java.util.Optional.ofNullable(
        documentFromItem(item, BookingPresentation.documentKind(item.getInventoryType())));
  }

  private BookingMain requiredBooking(String bookingRefNum) {
    if (bookingRefNum == null || bookingRefNum.isBlank()) {
      throw new BookingNotFoundException("");
    }
    return bookingMainRepository
        .findById(bookingRefNum.trim().toUpperCase())
        .orElseThrow(() -> new BookingNotFoundException(bookingRefNum));
  }

  private static BookingDetail requiredItem(BookingMain main, Long itemId) {
    if (itemId == null) {
      throw new BookingNotFoundException(main.getBookingRefNum() + " item");
    }
    return main.getDetails().stream()
        .filter(detail -> itemId.equals(detail.getId()))
        .findFirst()
        .orElseThrow(() -> new BookingNotFoundException(main.getBookingRefNum() + " item " + itemId));
  }

  private static String requireEditableStatus(String status) {
    String normalized = BookingPresentation.normalize(status);
    if (!BookingPresentation.isEditable(normalized)) {
      throw new IllegalArgumentException("Unsupported booking status");
    }
    return normalized;
  }

  private void recordMainStatus(BookingMain main, String previous, String next) {
    changeHistoryService.record(
        ChangeHistoryService.BOOKING_MAIN,
        main.getBookingRefNum(),
        main.getBookingRefNum(),
        ChangeHistoryService.STATUS_CHANGE,
        "booking_status",
        previous,
        next,
        main.getCompanyId(),
        main.getCompanyName());
  }

  private void recordItemStatus(BookingMain main, BookingDetail item, String previous, String next) {
    changeHistoryService.record(
        ChangeHistoryService.BOOKING_DETAIL,
        String.valueOf(item.getId()),
        main.getBookingRefNum(),
        ChangeHistoryService.STATUS_CHANGE,
        "item_status",
        previous,
        next,
        main.getCompanyId(),
        main.getCompanyName());
  }

  private void applyDocument(
      BookingMain main, BookingDetail item, String kind, String fileName, String contentType, byte[] bytes) {
    String resolvedKind = normalizeKind(kind);
    if (item != null) {
      String expected = BookingPresentation.documentKind(item.getInventoryType());
      if (!expected.equals(resolvedKind)) {
        throw new IllegalArgumentException(
            "Upload a " + BookingPresentation.documentLabel(expected).toLowerCase() + " for this item");
      }
    }
    String safeName = sanitizeFileName(fileName);
    String type = firstNonBlank(contentType, "application/octet-stream");
    if (bytes == null || bytes.length == 0) {
      throw new IllegalArgumentException("Choose a file to upload");
    }
    if (bytes.length > 8 * 1024 * 1024) {
      throw new IllegalArgumentException("File must be 8 MB or smaller");
    }
    if (!allowedFile(safeName, type)) {
      throw new IllegalArgumentException("Upload a PDF, PNG, JPG, or WEBP file");
    }
    String parkKey =
        item == null
            ? "booking"
            : firstNonBlank(item.getServiceId(), item.getItemName(), "park");
    Long itemId = item == null ? null : item.getId();
    if (item != null) {
      documentStore.deleteIfPresent(item.getFilePath());
    }
    String storedPath =
        documentStore.save(main.getBookingRefNum(), parkKey, itemId, resolvedKind, safeName, bytes);
    if (BookingPresentation.storesAsTicket(resolvedKind)) {
      String previous = item != null ? item.getTicketFileName() : main.getTicketFileName();
      archiveDocumentRows(main, item, previous, safeName, "ticket_file_name");
      if (item != null) {
        item.setTicketFileName(safeName);
        item.setTicketContentType(type);
        item.setTicketBytes(null);
        item.setFilePath(storedPath);
      }
      main.setTicketFileName(safeName);
      main.setTicketContentType(type);
      recordDocument(main, item, "ticket_file_name", previous, safeName);
    } else {
      String previous = item != null ? item.getVoucherFileName() : main.getVoucherFileName();
      archiveDocumentRows(main, item, previous, safeName, "voucher_file_name");
      if (item != null) {
        item.setVoucherFileName(safeName);
        item.setVoucherContentType(type);
        item.setVoucherBytes(null);
        item.setFilePath(storedPath);
      }
      main.setVoucherFileName(safeName);
      main.setVoucherContentType(type);
      recordDocument(main, item, "voucher_file_name", previous, safeName);
    }
  }

  private void archiveDocumentRows(
      BookingMain main, BookingDetail item, String previous, String next, String field) {
    if (sameText(previous, next)) {
      return;
    }
    if (item != null) {
      rowHistoryService.archive(
          EditableTable.BOOKING_DETAIL, item.getId(), ChangeHistoryService.DOCUMENT_UPLOAD, field);
    }
    rowHistoryService.archive(
        EditableTable.BOOKING_MAIN, main.getBookingRefNum(), ChangeHistoryService.DOCUMENT_UPLOAD, field);
  }

  private void recordDocument(BookingMain main, BookingDetail item, String field, String previous, String next) {
    changeHistoryService.record(
        item == null ? ChangeHistoryService.BOOKING_MAIN : ChangeHistoryService.BOOKING_DETAIL,
        item == null ? main.getBookingRefNum() : String.valueOf(item.getId()),
        main.getBookingRefNum(),
        ChangeHistoryService.DOCUMENT_UPLOAD,
        field,
        previous,
        next,
        main.getCompanyId(),
        main.getCompanyName());
  }

  private BookingDocument findDocument(BookingMain main, BookingDetail preferredItem, String kind) {
    String resolvedKind = normalizeKind(kind);
    if (preferredItem != null) {
      return documentFromItem(preferredItem, resolvedKind);
    }
    if (main.getDetails() != null) {
      for (BookingDetail detail : main.getDetails()) {
        BookingDocument itemDocument = documentFromItem(detail, resolvedKind);
        if (itemDocument != null) {
          return itemDocument;
        }
      }
    }
    return documentFromMain(main, resolvedKind);
  }

  private BookingDocument documentFromItem(BookingDetail detail, String kind) {
    String resolvedKind = normalizeKind(kind);
    if (documentStore.exists(detail.getFilePath())) {
      boolean ticketKind = BookingPresentation.storesAsTicket(resolvedKind);
      String storedName = ticketKind ? detail.getTicketFileName() : detail.getVoucherFileName();
      String storedType = ticketKind ? detail.getTicketContentType() : detail.getVoucherContentType();
      if (storedName != null && !storedName.isBlank()) {
        return new BookingDocument(
            resolvedKind,
            storedName,
            firstNonBlank(storedType, "application/pdf"),
            documentStore.read(detail.getFilePath()));
      }
    }
    if (BookingPresentation.storesAsTicket(resolvedKind)) {
      if (detail.getTicketBytes() == null || detail.getTicketBytes().length == 0) {
        return null;
      }
      return new BookingDocument(
          resolvedKind,
          firstNonBlank(detail.getTicketFileName(), "ticket.pdf"),
          firstNonBlank(detail.getTicketContentType(), "application/pdf"),
          detail.getTicketBytes());
    }
    if (detail.getVoucherBytes() == null || detail.getVoucherBytes().length == 0) {
      return null;
    }
    return new BookingDocument(
        resolvedKind,
        firstNonBlank(detail.getVoucherFileName(), "visa".equals(resolvedKind) ? "visa-copy.pdf" : "voucher.pdf"),
        firstNonBlank(detail.getVoucherContentType(), "application/pdf"),
        detail.getVoucherBytes());
  }

  private static BookingDocument documentFromMain(BookingMain main, String kind) {
    String resolvedKind = normalizeKind(kind);
    if (BookingPresentation.storesAsTicket(resolvedKind)) {
      if (main.getTicketBytes() == null || main.getTicketBytes().length == 0) {
        return null;
      }
      return new BookingDocument(
          resolvedKind,
          firstNonBlank(main.getTicketFileName(), "ticket.pdf"),
          firstNonBlank(main.getTicketContentType(), "application/pdf"),
          main.getTicketBytes());
    }
    if (main.getVoucherBytes() == null || main.getVoucherBytes().length == 0) {
      return null;
    }
    return new BookingDocument(
        resolvedKind,
        firstNonBlank(main.getVoucherFileName(), "visa".equals(resolvedKind) ? "visa-copy.pdf" : "voucher.pdf"),
        firstNonBlank(main.getVoucherContentType(), "application/pdf"),
        main.getVoucherBytes());
  }

  private static void syncRollup(BookingMain main) {
    List<String> statuses =
        main.getDetails().stream()
            .map(detail -> storedItemStatus(detail, main))
            .toList();
    main.setStatus(BookingPresentation.rollupStatus(statuses));
  }

  private static String storedStatus(BookingMain main) {
    if (main.getDetails() != null && !main.getDetails().isEmpty()) {
      List<String> statuses =
          main.getDetails().stream().map(detail -> storedItemStatus(detail, main)).toList();
      if (statuses.stream().anyMatch(status -> status != null && !status.isBlank())) {
        return BookingPresentation.rollupStatus(statuses);
      }
    }
    if (main.getStatus() != null && !main.getStatus().isBlank()) {
      return BookingPresentation.normalize(main.getStatus());
    }
    return BookingPresentation.deriveStatus(main);
  }

  private static String storedItemStatus(BookingDetail detail, BookingMain main) {
    if (detail.getStatus() != null && !detail.getStatus().isBlank()) {
      return BookingPresentation.normalize(detail.getStatus());
    }
    if (main.getStatus() != null && !main.getStatus().isBlank()) {
      return BookingPresentation.normalize(main.getStatus());
    }
    return BookingPresentation.deriveStatus(main);
  }

  private static String normalizeKind(String kind) {
    return BookingPresentation.normalizeDocumentKind(kind);
  }

  private static String sanitizeFileName(String fileName) {
    String name = firstNonBlank(fileName, "document.pdf");
    int slash = Math.max(name.lastIndexOf('/'), name.lastIndexOf('\\'));
    if (slash >= 0) {
      name = name.substring(slash + 1);
    }
    name = name.replaceAll("[^A-Za-z0-9._-]", "_");
    return name.isBlank() ? "document.pdf" : name;
  }

  private static boolean allowedFile(String fileName, String contentType) {
    String lower = fileName.toLowerCase();
    String type = contentType == null ? "" : contentType.toLowerCase();
    return lower.endsWith(".pdf")
        || lower.endsWith(".png")
        || lower.endsWith(".jpg")
        || lower.endsWith(".jpeg")
        || lower.endsWith(".webp")
        || type.contains("pdf")
        || type.contains("png")
        || type.contains("jpeg")
        || type.contains("jpg")
        || type.contains("webp");
  }

  private static boolean looksLikeEmail(String value) {
    return value != null && value.contains("@");
  }

  @Override
  @Transactional(readOnly = true)
  public List<BookingMainView> listByCompany(String companyId) {
    if (companyId == null || companyId.isBlank()) {
      return List.of();
    }
    return bookingMainRepository.findByCompanyIdOrderByCreatedDateDesc(companyId.trim()).stream()
        .map(this::toView)
        .toList();
  }

  @Override
  @Transactional(readOnly = true)
  public List<BookingMainView> listAll() {
    return bookingMainRepository.findAllByOrderByCreatedDateDesc().stream().map(this::toView).toList();
  }

  @Override
  @Transactional(readOnly = true)
  public BookingMainView get(String bookingRefNum) {
    if (bookingRefNum == null || bookingRefNum.isBlank()) {
      throw new BookingNotFoundException("");
    }
    return bookingMainRepository
        .findById(bookingRefNum.trim().toUpperCase())
        .map(this::toView)
        .orElseThrow(() -> new BookingNotFoundException(bookingRefNum));
  }

  @Override
  public BookingMainView toView(BookingMain main) {
    List<BookingDetailView> details =
        main.getDetails().stream().map(detail -> toDetailView(main, detail, readPassengers(detail))).toList();
    BigDecimal actualValue = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
    BigDecimal actualConverted = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
    for (BookingDetailView detail : details) {
      if (detail.actualTotalRate() != null) {
        actualValue = actualValue.add(detail.actualTotalRate());
      } else if (detail.totalRate() != null) {
        actualValue = actualValue.add(detail.totalRate());
      }
      if (detail.actualConvertedRate() != null) {
        actualConverted = actualConverted.add(detail.actualConvertedRate());
      } else if (detail.convertedRate() != null) {
        actualConverted = actualConverted.add(detail.convertedRate());
      } else if (detail.totalRate() != null) {
        actualConverted = actualConverted.add(detail.totalRate());
      }
    }
    return new BookingMainView(
        main.getBookingRefNum(),
        main.getUserName(),
        main.getUserId(),
        main.getCompanyName(),
        main.getCompanyId(),
        main.getValue(),
        firstNonBlank(main.getCurrency(), "AED"),
        main.getRoeRate(),
        main.getConvertedValue(),
        main.getCreatedDate(),
        storedStatus(main),
        firstNonBlank(main.getLeadEmail(), looksLikeEmail(main.getUserId()) ? main.getUserId() : ""),
        firstNonBlank(main.getLeadPhone()),
        main.getVoucherFileName(),
        main.getTicketFileName(),
        details,
        actualValue,
        actualConverted,
        createdByChild(main.getCompanyId()),
        false,
        false);
  }

  // ---- passengers (stored as JSON in booking_details.passengers) ---------------------------------

  /** The guests entered for this line, as flat maps without blank values; {@code null} when none. */
  private String passengersJson(ParkQuoteLine line) {
    if (line.getPassengers() == null || line.getPassengers().isEmpty()) {
      return null;
    }
    List<Map<String, String>> rows = new ArrayList<>();
    for (ParkPassenger passenger : line.getPassengers()) {
      if (passenger == null) {
        continue;
      }
      Map<String, String> row = new LinkedHashMap<>();
      putIfPresent(row, "type", passenger.getType());
      putIfPresent(row, "firstName", passenger.getFirstName());
      putIfPresent(row, "lastName", passenger.getLastName());
      putIfPresent(row, "email", passenger.getEmail());
      putIfPresent(row, "phone", passenger.getPhone());
      passenger.getExtra().forEach((key, value) -> putIfPresent(row, key, value));
      if (!row.isEmpty()) {
        rows.add(row);
      }
    }
    if (rows.isEmpty()) {
      return null;
    }
    log.debug("Saving {} passenger(s) for {}", rows.size(), line.getItemId());
    return objectMapper.writeValueAsString(rows);
  }

  private List<Map<String, String>> readPassengers(BookingDetail detail) {
    String json = detail.getPassengers();
    if (json == null || json.isBlank()) {
      return List.of();
    }
    try {
      return objectMapper.readValue(json, new TypeReference<List<Map<String, String>>>() {});
    } catch (RuntimeException ex) {
      log.warn("Could not read passengers of booking item {}: {}", detail.getId(), ex.getMessage());
      return List.of();
    }
  }

  private static void putIfPresent(Map<String, String> row, String key, String value) {
    if (key != null && !key.isBlank() && value != null && !value.isBlank()) {
      row.put(key, value.trim());
    }
  }

  private static BookingDetailView toDetailView(
      BookingMain main, BookingDetail detail, List<Map<String, String>> passengers) {
    return new BookingDetailView(
        detail.getId(),
        main.getBookingRefNum(),
        detail.getServiceName(),
        detail.getServiceId(),
        detail.getItemName(),
        detail.getShowDate(),
        detail.getCheckinDate(),
        detail.getCheckoutDate(),
        detail.getPropertyId(),
        detail.getAdultCount(),
        detail.getChildCount(),
        detail.getInfantCount(),
        detail.getToddlerCount(),
        detail.getTotalRate(),
        detail.getRoeRate(),
        detail.getConvertedRate(),
        firstNonBlank(main.getCurrency(), "AED"),
        detail.getBookedOn(),
        detail.getBookedBy(),
        detail.getUserId(),
        detail.getCompanyName(),
        detail.getCompanyId(),
        detail.getInventoryType(),
        detail.getSupplierName(),
        detail.getSupplierId(),
        storedItemStatus(detail, main),
        detail.getFollowUp(),
        firstNonBlank(detail.getVoucherFileName()),
        firstNonBlank(detail.getTicketFileName()),
        detail.getActualTotalRate(),
        detail.getActualConvertedRate(),
        false,
        false,
        passengers);
  }

  private static BookingDetail detailFrom(
      ParkQuoteLine line,
      Instant bookedOn,
      String bookedBy,
      String userId,
      String companyName,
      String companyId) {
    boolean hotel = isHotel(line);
    LocalDate visitDate = parseDate(line.getVisitDate());
    LocalDate checkout = parseDate(line.getCheckOut());
    if (hotel && visitDate != null && checkout == null) {
      checkout = visitDate.plusDays(1);
    }

    BookingDetail detail = new BookingDetail();
    detail.setServiceName(firstNonBlank(line.getCategory(), line.getKind(), "Attraction"));
    detail.setServiceId(firstNonBlank(line.getItemId(), "UNKNOWN"));
    detail.setItemName(itemName(line));
    detail.setShowDate(hotel ? null : visitDate);
    detail.setCheckinDate(hotel ? visitDate : null);
    detail.setCheckoutDate(hotel ? checkout : null);
    detail.setPropertyId(hotel ? line.getItemId() : null);
    detail.setAdultCount(Math.max(0, line.getAdults()));
    detail.setChildCount(Math.max(0, line.getChildren()));
    detail.setInfantCount(0);
    detail.setToddlerCount(0);
    detail.setTotalRate(money(line.getAedLineTotal()));
    detail.setActualTotalRate(money(line.getAedActualLineTotal() != 0 ? line.getAedActualLineTotal() : line.getAedLineTotal()));
    detail.setRoeRate(rateMoney(line.getRoeRate()));
    detail.setConvertedRate(money(line.getConvertedRate()));
    detail.setActualConvertedRate(
        money(line.getActualLineTotal() != 0 ? line.getActualLineTotal() : line.getConvertedRate()));
    detail.setBookedOn(bookedOn);
    detail.setBookedBy(bookedBy);
    detail.setUserId(userId);
    detail.setCompanyName(companyName);
    detail.setCompanyId(companyId);
    detail.setInventoryType(inventoryType(line));
    detail.setSupplierName(SUPPLIER_NAME);
    detail.setSupplierId(SUPPLIER_ID);
    detail.setStatus(BookingPresentation.PENDING);
    return detail;
  }

  private static BookingDetail detailFromQuote(
      QuoteDetail line, Instant bookedOn, String bookedBy, String userId, String companyName, String companyId) {
    boolean hotel = isHotel(line);
    LocalDate visitDate = line.getVisitDate();
    LocalDate checkout = line.getCheckoutDate();
    if (hotel && visitDate != null && checkout == null) {
      checkout = visitDate.plusDays(1);
    }

    BookingDetail detail = new BookingDetail();
    detail.setServiceName(firstNonBlank(line.getCategory(), line.getKind(), "Attraction"));
    detail.setServiceId(firstNonBlank(line.getItemId(), "UNKNOWN"));
    detail.setItemName(firstNonBlank(line.getItemName(), line.getItemId(), "Item"));
    detail.setShowDate(hotel ? null : visitDate);
    detail.setCheckinDate(hotel ? visitDate : null);
    detail.setCheckoutDate(hotel ? checkout : null);
    detail.setPropertyId(hotel ? firstNonBlank(line.getItemId()) : null);
    detail.setAdultCount(Math.max(0, line.getAdultCount()));
    detail.setChildCount(Math.max(0, line.getChildCount()));
    detail.setInfantCount(0);
    detail.setToddlerCount(0);
    detail.setTotalRate(
        line.getLineTotal() == null ? BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP) : line.getLineTotal());
    detail.setActualTotalRate(
        line.getActualLineTotal() == null ? detail.getTotalRate() : line.getActualLineTotal());
    detail.setRoeRate(line.getRoeRate());
    detail.setConvertedRate(
        line.getConvertedRate() == null ? detail.getTotalRate() : line.getConvertedRate());
    detail.setActualConvertedRate(
        line.getActualConvertedRate() == null ? detail.getConvertedRate() : line.getActualConvertedRate());
    detail.setBookedOn(bookedOn);
    detail.setBookedBy(bookedBy);
    detail.setUserId(userId);
    detail.setCompanyName(companyName);
    detail.setCompanyId(companyId);
    detail.setInventoryType(inventoryType(line));
    detail.setSupplierName(SUPPLIER_NAME);
    detail.setSupplierId(SUPPLIER_ID);
    detail.setStatus(BookingPresentation.PENDING);
    return detail;
  }

  private String newBookingRef() {
    String ref;
    do {
      ref = ("PRK" + UUID.randomUUID().toString().replace("-", "")).substring(0, 12).toUpperCase();
    } while (bookingMainRepository.existsById(ref));
    return ref;
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

  private static String inventoryType(ParkQuoteLine line) {
    return inventoryType(line.getKind(), line.getCategory());
  }

  private static String inventoryType(QuoteDetail line) {
    return inventoryType(line.getKind(), line.getCategory());
  }

  private static String inventoryType(String kindValue, String categoryValue) {
    String kind = kindValue == null ? "" : kindValue.trim().toLowerCase();
    String category = categoryValue == null ? "" : categoryValue.trim().toLowerCase();
    if ("stay".equals(kind) || "hotels".equals(category) || "hotel".equals(category)) {
      return "HOTEL";
    }
    if ("visa".equals(kind) || "visa".equals(category) || "visas".equals(category)) {
      return "VISA";
    }
    if ("transfer".equals(kind) || "transfer".equals(category) || "transfers".equals(category)) {
      return "TRANSFER";
    }
    return "ATTRACTION";
  }

  private static boolean isHotel(ParkQuoteLine line) {
    return "HOTEL".equals(inventoryType(line));
  }

  private static boolean isHotel(QuoteDetail line) {
    return "HOTEL".equals(inventoryType(line));
  }

  private static String requiredName(ParkQuoteRequest request) {
    String name = request == null ? null : request.getLeadName();
    if (name == null || name.isBlank()) {
      throw new IllegalArgumentException("Lead guest name is required");
    }
    return name.trim();
  }

  private static String resolveUserId(ParkQuoteRequest request) {
    if (request == null) {
      return "GUEST";
    }
    return firstNonBlank(request.getUserId(), request.getEmail(), "GUEST");
  }

  private static LocalDate parseDate(String value) {
    if (value == null || value.isBlank()) {
      return null;
    }
    return LocalDate.parse(value.trim());
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

  private boolean createdByChild(String companyId) {
    Long id = parseCompanyId(companyId);
    if (id == null) {
      return false;
    }
    return companyRepository.findById(id).map(company -> !company.parentCompany()).orElse(false);
  }

  private static Long parseCompanyId(String value) {
    if (value == null || value.isBlank()) {
      return null;
    }
    try {
      return Long.valueOf(value.trim());
    } catch (NumberFormatException ex) {
      return null;
    }
  }
}
