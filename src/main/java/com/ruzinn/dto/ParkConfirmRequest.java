package com.ruzinn.dto;

/**
 * Request body of {@code POST /api/parks/confirm}: the itinerary (with passengers nested per line
 * item) plus how the booking is paid - company credit or card.
 */
public class ParkConfirmRequest extends ParkQuoteRequest {

  /** {@code "CREDIT"} or {@code "CARD"} (case-insensitive). */
  private String paymentMethod;

  private String cardName;
  private String cardNumber;
  private String cardExpiry;
  private String cardCvc;

  /**
   * True when this checkout accepts the saved quote {@code quoteNumber}: the quote is marked
   * ACCEPTED and linked to the booking only after payment succeeds.
   */
  private boolean acceptQuote;

  public boolean isAcceptQuote() {
    return acceptQuote;
  }

  public void setAcceptQuote(boolean acceptQuote) {
    this.acceptQuote = acceptQuote;
  }

  public String getPaymentMethod() {
    return paymentMethod;
  }

  public void setPaymentMethod(String paymentMethod) {
    this.paymentMethod = paymentMethod;
  }

  public String getCardName() {
    return cardName;
  }

  public void setCardName(String cardName) {
    this.cardName = cardName;
  }

  public String getCardNumber() {
    return cardNumber;
  }

  public void setCardNumber(String cardNumber) {
    this.cardNumber = cardNumber;
  }

  public String getCardExpiry() {
    return cardExpiry;
  }

  public void setCardExpiry(String cardExpiry) {
    this.cardExpiry = cardExpiry;
  }

  public String getCardCvc() {
    return cardCvc;
  }

  public void setCardCvc(String cardCvc) {
    this.cardCvc = cardCvc;
  }
}
