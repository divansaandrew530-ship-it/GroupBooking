package com.ruzinn.serviceimpl;

import com.ruzinn.dto.BookingMailModel;
import com.ruzinn.dto.OutgoingMail;
import com.ruzinn.dto.ParkBooking;
import com.ruzinn.dto.ParkQuoteRequest;
import com.ruzinn.dto.SentBookingMail;
import com.ruzinn.entity.BookingMain;
import com.ruzinn.entity.QuoteMain;
import com.ruzinn.service.BookingConfirmationMailService;
import com.ruzinn.service.MailService;
import com.ruzinn.service.MailTemplateRenderer;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@Service
public class BookingConfirmationMailServiceImpl implements BookingConfirmationMailService {

  private static final Logger log = LoggerFactory.getLogger(BookingConfirmationMailServiceImpl.class);

  private final MailTemplateRenderer templates;
  private final MailService mailService;
  private final CopyOnWriteArrayList<SentBookingMail> sent = new CopyOnWriteArrayList<>();

  public BookingConfirmationMailServiceImpl(MailTemplateRenderer templates, MailService mailService) {
    this.templates = templates;
    this.mailService = mailService;
  }

  @Override
  public void sendAfterCommit(ParkBooking booking, ParkQuoteRequest request, String paymentNote) {
    runAfterCommit(() -> send(booking, request, paymentNote));
  }

  @Override
  public void sendAfterCommit(BookingMain booking, QuoteMain quote) {
    runAfterCommit(() -> send(booking, quote));
  }

  @Override
  public SentBookingMail send(ParkBooking booking, ParkQuoteRequest request, String paymentNote) {
    BookingMailModel model = templates.model(booking, request, paymentNote);
    String html = templates.renderBooking(booking, request, paymentNote);
    return deliver(model, html);
  }

  @Override
  public SentBookingMail send(BookingMain booking, QuoteMain quote) {
    BookingMailModel model = templates.model(booking, quote);
    String html = templates.renderBooking(booking, quote);
    return deliver(model, html);
  }

  @Override
  public List<SentBookingMail> sentMails() {
    return new ArrayList<>(sent);
  }

  @Override
  public void clearSent() {
    sent.clear();
  }

  private SentBookingMail deliver(BookingMailModel model, String html) {
    String to = model.guestEmail();
    if (to == null || !to.matches("(?i)^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$")) {
      log.info("Skip booking confirmation email; no valid guest address for {}", model.bookingRef());
      return null;
    }
    String subject =
        "Rathin booking "
            + model.bookingRef()
            + (model.confirmed() ? " confirmed — " : " received, waiting for approval — ")
            + model.currency()
            + " "
            + model.total();
    String transportName = mailService.transportName();
    try {
      mailService.send(OutgoingMail.html(to, subject, html));
    } catch (Exception ex) {
      log.warn("Booking {} confirmation email failed for {}: {}", model.bookingRef(), to, ex.getMessage());
      return null;
    }
    SentBookingMail mail =
        new SentBookingMail(
            model.bookingRef(),
            model.confirmationId(),
            to,
            subject,
            html,
            Instant.now(),
            transportName);
    sent.add(mail);
    return mail;
  }

  private static void runAfterCommit(Runnable action) {
    if (TransactionSynchronizationManager.isSynchronizationActive()) {
      TransactionSynchronizationManager.registerSynchronization(
          new TransactionSynchronization() {
            @Override
            public void afterCommit() {
              action.run();
            }
          });
      return;
    }
    action.run();
  }
}
