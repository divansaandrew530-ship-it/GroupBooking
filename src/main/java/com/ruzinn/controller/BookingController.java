package com.ruzinn.controller;

import com.ruzinn.dto.AuthSession;
import com.ruzinn.dto.BookingDocument;
import com.ruzinn.dto.BookingMainView;
import com.ruzinn.exception.BookingNotFoundException;
import com.ruzinn.exception.UnauthorizedException;
import com.ruzinn.service.AccountService;
import com.ruzinn.service.ParkBookingService;
import com.ruzinn.util.FileResponses;
import com.ruzinn.util.SessionUser;
import jakarta.servlet.http.HttpSession;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/**
 * Bookings of the signed-in company.
 *
 * <p>Rules: child companies see only their own bookings and cannot change them; parent-company
 * users see every booking and manage statuses, follow-ups and documents. Bookings of another
 * company answer 404. Bookings are created with {@code POST /api/parks/confirm}.
 */
@RestController
public class BookingController {

  private final AccountService accountService;
  private final ParkBookingService bookingService;

  public BookingController(AccountService accountService, ParkBookingService bookingService) {
    this.accountService = accountService;
    this.bookingService = bookingService;
  }

  /** Parent companies may filter by {@code companyId}; child companies always get their own. */
  @GetMapping("/api/company/bookings")
  public List<BookingMainView> list(@RequestParam(required = false) String companyId, HttpSession session) {
    AuthSession auth = accountService.requireSignedIn(SessionUser.current(session));
    List<BookingMainView> bookings;
    if (!auth.parentCompany()) {

      accountService.requireCompanyAccess(auth, auth.companyId());
      bookings = bookingService.listCompanyBookings(auth.companyId());
    } else if (companyId != null && !companyId.isBlank()) {
      accountService.requireCompanyAccess(auth, companyId.trim());
      bookings = bookingService.listCompanyBookings(companyId.trim());
    } else {
      bookings = bookingService.listAllBookings();
    }
    return bookings.stream().map(booking -> booking.forViewer(auth.parentCompany())).toList();
  }

  @GetMapping("/api/company/bookings/{bookingRef}")
  public BookingMainView get(@PathVariable String bookingRef, HttpSession session) {
    return visibleBooking(accountService.requireSignedIn(SessionUser.current(session)), bookingRef);
  }

  /**
   * Company credit available for checkout with {@code paymentMethod: CREDIT}. Parent-company users
   * pass {@code companyId} to see the credit of the child company they are booking for.
   */
  @GetMapping("/api/company/credit")
  public Map<String, Object> credit(@RequestParam(required = false) String companyId, HttpSession session) {
    AuthSession auth = accountService.requireSignedIn(SessionUser.current(session));
    return Map.of(
        "remaining", accountService.remainingCredit(auth, companyId),
        "canUseCredit", accountService.canUseCredit(auth, companyId));
  }

  // ---- changes (parent-company users only) ---------------------------------------------------

  /** Body: {@code {"status": "CONFIRMED"}}. */
  @PostMapping("/api/company/bookings/{bookingRef}/status")
  public BookingMainView updateStatus(
      @PathVariable String bookingRef, @RequestBody Map<String, String> body, HttpSession session) {
    AuthSession auth = requireBookingManager(session, bookingRef);
    return bookingService.updateStatus(bookingRef, value(body, "status")).forViewer(auth.parentCompany());
  }

  /** Body: {@code {"status": "CONFIRMED"}}. */
  @PostMapping("/api/company/bookings/{bookingRef}/items/{itemId}/status")
  public BookingMainView updateItemStatus(
      @PathVariable String bookingRef,
      @PathVariable Long itemId,
      @RequestBody Map<String, String> body,
      HttpSession session) {
    AuthSession auth = requireBookingManager(session, bookingRef);
    return bookingService.updateItemStatus(bookingRef, itemId, value(body, "status")).forViewer(auth.parentCompany());
  }

  /** Body: {@code {"followUp": "Waiting on supplier voucher"}}. */
  @PostMapping("/api/company/bookings/{bookingRef}/items/{itemId}/follow-up")
  public BookingMainView updateItemFollowUp(
      @PathVariable String bookingRef,
      @PathVariable Long itemId,
      @RequestBody(required = false) Map<String, String> body,
      HttpSession session) {
    AuthSession auth = requireBookingManager(session, bookingRef);
    return bookingService.updateItemFollowUp(bookingRef, itemId, value(body, "followUp")).forViewer(auth.parentCompany());
  }

  /** Multipart: {@code kind} (e.g. voucher, ticket) and {@code file}. */
  @PostMapping(value = "/api/company/bookings/{bookingRef}/documents", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  public Map<String, Object> uploadDocument(
      @PathVariable String bookingRef,
      @RequestParam String kind,
      @RequestParam("file") MultipartFile file,
      HttpSession session) {
    requireBookingManager(session, bookingRef);
    byte[] bytes = FileResponses.requireBytes(file);
    bookingService.storeDocument(bookingRef, kind, file.getOriginalFilename(), file.getContentType(), bytes);
    return uploaded(kind, file);
  }

  /** Multipart: {@code kind} (ticket first, then voucher/visa) and {@code file}. */
  @PostMapping(
      value = "/api/company/bookings/{bookingRef}/items/{itemId}/documents",
      consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  public Map<String, Object> uploadItemDocument(
      @PathVariable String bookingRef,
      @PathVariable Long itemId,
      @RequestParam String kind,
      @RequestParam("file") MultipartFile file,
      HttpSession session) {
    requireBookingManager(session, bookingRef);
    byte[] bytes = FileResponses.requireBytes(file);
    bookingService.storeItemDocument(bookingRef, itemId, kind, file.getOriginalFilename(), file.getContentType(), bytes);
    return uploaded(kind, file);
  }

  // ---- downloads (any user who can see the booking) -------------------------------------------

  /** {@code kind} is a document kind, or {@code preferred} for the best available one. */
  @GetMapping("/company/bookings/{bookingRef}/documents/{kind}")
  public ResponseEntity<byte[]> downloadDocument(
      @PathVariable String bookingRef, @PathVariable String kind, HttpSession session) {
    visibleBooking(accountService.requireSignedIn(SessionUser.current(session)), bookingRef);
    Optional<BookingDocument> document =
        isPreferred(kind)
            ? bookingService.findPreferredDocument(bookingRef)
            : Optional.of(bookingService.loadDocument(bookingRef, kind));
    return toDownload(document);
  }

  /** {@code kind} is a document kind, or {@code preferred} for the best available one. */
  @GetMapping("/company/bookings/{bookingRef}/items/{itemId}/documents/{kind}")
  public ResponseEntity<byte[]> downloadItemDocument(
      @PathVariable String bookingRef, @PathVariable Long itemId, @PathVariable String kind, HttpSession session) {
    visibleBooking(accountService.requireSignedIn(SessionUser.current(session)), bookingRef);
    Optional<BookingDocument> document =
        isPreferred(kind)
            ? bookingService.findPreferredItemDocument(bookingRef, itemId)
            : Optional.of(bookingService.loadItemDocument(bookingRef, itemId, kind));
    return toDownload(document);
  }

  // ---- helpers --------------------------------------------------------------------------------

  /** The booking as this user may see it; bookings of another company are "not found". */
  private BookingMainView visibleBooking(AuthSession auth, String bookingRef) {
    BookingMainView booking = bookingService.getRecord(bookingRef);
    if (auth.parentCompany()) {
      return booking.forViewer(true);
    }
    if (booking.companyId() != null && booking.companyId().equals(auth.companyId())) {
      return booking.forViewer(false);
    }
    throw new BookingNotFoundException(bookingRef);
  }

  private AuthSession requireBookingManager(HttpSession session, String bookingRef) {
    AuthSession auth = accountService.requireSignedIn(SessionUser.current(session));
    visibleBooking(auth, bookingRef);
    if (!auth.canManageBookings()) {
      throw new UnauthorizedException("Only parent company users can update this booking.");
    }
    return auth;
  }

  private static ResponseEntity<byte[]> toDownload(Optional<BookingDocument> document) {
    return document
        .map(found -> FileResponses.download(found.fileName(), found.contentType(), found.bytes()))
        .orElseGet(() -> ResponseEntity.notFound().build());
  }

  private static boolean isPreferred(String kind) {
    return kind != null && "preferred".equalsIgnoreCase(kind.trim());
  }

  private static String value(Map<String, String> body, String key) {
    return body == null ? null : body.get(key);
  }

  private static Map<String, Object> uploaded(String kind, MultipartFile file) {
    return Map.of("status", "uploaded", "kind", kind, "fileName", file.getOriginalFilename());
  }
}
