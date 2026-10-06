package com.ruzinn.serviceimpl;

import com.ruzinn.dto.Experience;
import com.ruzinn.dto.ItemCountry;
import com.ruzinn.dto.ParkDayPlan;
import com.ruzinn.dto.ParkLineRequest;
import com.ruzinn.dto.ParkQuote;
import com.ruzinn.dto.ParkQuoteLine;
import com.ruzinn.dto.ParkQuoteRequest;
import com.ruzinn.service.ParkCatalogService;
import com.ruzinn.service.ParkQuoteService;
import com.ruzinn.service.RoeService;
import com.ruzinn.util.BookingPresentation;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.security.SecureRandom;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class ParkQuoteServiceImpl implements ParkQuoteService {

  private static final Logger log = LoggerFactory.getLogger(ParkQuoteServiceImpl.class);

  private static final DateTimeFormatter ISSUED_ON = BookingPresentation.DISPLAY_DATE;
  private static final DateTimeFormatter QUOTE_DAY = DateTimeFormatter.ofPattern("yyMMdd");
  private static final SecureRandom RANDOM = new SecureRandom();

  private final ParkCatalogService catalogService;
  private final RoeService roeService;

  public ParkQuoteServiceImpl(ParkCatalogService catalogService, RoeService roeService) {
    this.catalogService = catalogService;
    this.roeService = roeService;
  }

  @Override
  public ParkQuote quote(ParkQuoteRequest request) {
    if (request == null) {
      throw new IllegalArgumentException("Quote request is required");
    }
    int adults = Math.max(0, request.getAdults());
    int children = Math.max(0, request.getChildren());
    if (adults + children < 1) {
      throw new IllegalArgumentException("At least one guest is required");
    }
    if (request.getItems() == null || request.getItems().isEmpty()) {
      throw new IllegalArgumentException("Add at least one park, ticket, or extra");
    }

    log.debug(
        "Pricing quote {}: {} adult(s), {} child(ren), {} item(s), markup {}%, currency {}",
        request.getQuoteNumber(), adults, children, request.getItems().size(), request.getMarkupPercent(),
        request.getCurrency());
    ParkQuote quote = new ParkQuote();
    quote.setCurrency("AED");
    quote.setAdults(adults);
    quote.setChildren(children);
    quote.setMarkupPercent(Math.max(0, request.getMarkupPercent()));
    quote.setQuoteNumber(existingQuoteNumber(request.getQuoteNumber()));
    quote.setIssuedOn(LocalDate.now().format(ISSUED_ON));
    quote.setPreparedBy(blankToNull(request.getOwnerUserName()));
    quote.setCompanyName(blankToNull(request.getCompanyName()));
    quote.setExpireDate(request.getExpireDate());

    String selectedCountry = selectedCountry(request);
    quote.setCountry(selectedCountry);

    List<ParkQuoteLine> lines = new ArrayList<>();
    boolean hasOnRequest = false;
    double subtotal = 0;

    for (ParkLineRequest item : request.getItems()) {
      if (item == null || item.getItemId() == null || item.getItemId().isBlank()) {
        throw new IllegalArgumentException("Each itinerary row needs an experience");
      }
      Experience experience = catalogService.require(item.getItemId().trim());
      if (!selectedCountry.equals(itemCountry(experience))) {
        throw new IllegalArgumentException(
            experience.getName()
                + " is not available for "
                + ItemCountry.of(selectedCountry).name());
      }
      int lineAdults = item.getAdults() == null ? adults : Math.max(0, item.getAdults());
      int lineChildren = item.getChildren() == null ? children : Math.max(0, item.getChildren());
      if (lineAdults + lineChildren < 1) {
        throw new IllegalArgumentException("Guest count required for " + experience.getName());
      }

      ParkQuoteLine line = new ParkQuoteLine();
      line.setItemId(experience.getId());
      line.setName(experience.getName());
      line.setKind(experience.getKind());
      line.setCategory(experience.getCategory());
      line.setCity(experience.getCity());
      line.setVisitDate(normalizeDate(item.getVisitDate(), experience));
      line.setCheckOut(blankToNull(item.getCheckOut()));
      line.setRooms(item.getRooms() == null ? 0 : Math.max(0, item.getRooms()));
      line.setRoomType(item.getRoomType());
      line.setAdults(lineAdults);
      line.setChildren(lineChildren);
      line.setAdultPrice(experience.getAdultPrice());
      line.setChildPrice(experience.getChildPrice());
      line.setPriceOnRequest(experience.isPriceOnRequest());
      line.setCurrency("AED");
      line.setTimeSlot(timeSlotFor(experience, item.getTimeSlot()));
      line.setBookingInfo(experience.getBookingInfo());
      line.setRequiresPassengerDetails(experience.requiresPassengerDetails());
      line.setPassengerFields(experience.passengerFormFields());
      if (item.getPassengers() != null) {
        line.setPassengers(item.getPassengers());
      }
      applyContent(line, item);

      double lineTotal = 0;
      double actualLineTotal = 0;
      if (experience.isPriceOnRequest()) {
        hasOnRequest = true;
      } else if ("stay".equals(experience.getKind())) {
        if (item.getRoomType() == null || item.getRoomType().isBlank()) {
          throw new IllegalArgumentException("Select a room type for " + experience.getName() + ".");
        }
        Double catalogRate = experience.roomRate(item.getRoomType());
        if (catalogRate == null) {
          throw new IllegalArgumentException("Unknown room type for " + experience.getName());
        }
        Double quoteRate = item.getAdultRate() != null ? item.getAdultRate() : catalogRate;
        int rooms = item.getRooms() == null ? 1 : Math.max(1, item.getRooms());
        int nights = nightsForStay(experience, line.getVisitDate(), item.getCheckOut());
        line.setRooms(rooms);
        line.setActualAdultPrice(catalogRate);
        line.setAdultPrice(quoteRate);
        line.setCheckOut(item.getCheckOut().trim());
        actualLineTotal = money(catalogRate * rooms * nights);
        lineTotal = money(quoteRate * rooms * nights);
      } else if (!experience.isPriceOnRequest()) {
        double catalogAdult = experience.getAdultPrice() == null ? 0 : experience.getAdultPrice();
        double catalogChild = experience.getChildPrice() == null ? catalogAdult : experience.getChildPrice();
        double quoteAdult = item.getAdultRate() != null ? item.getAdultRate() : catalogAdult;
        double quoteChild = item.getChildRate() != null ? item.getChildRate() : catalogChild;
        line.setActualAdultPrice(catalogAdult);
        line.setActualChildPrice(catalogChild);
        line.setAdultPrice(quoteAdult);
        line.setChildPrice(quoteChild);
        actualLineTotal = money(catalogAdult * lineAdults + catalogChild * lineChildren);
        lineTotal = money(quoteAdult * lineAdults + quoteChild * lineChildren);
      }
      line.setLineTotal(lineTotal);
      line.setAedLineTotal(lineTotal);
      line.setAedAdultPrice(line.getAdultPrice());
      line.setAedChildPrice(line.getChildPrice());
      line.setActualLineTotal(actualLineTotal);
      line.setAedActualLineTotal(actualLineTotal);
      line.setAedActualAdultPrice(line.getActualAdultPrice());
      line.setAedActualChildPrice(line.getActualChildPrice());
      line.setRoeRate(1);
      line.setConvertedRate(lineTotal);
      subtotal += lineTotal;
      lines.add(line);
    }

    quote.setItems(lines);
    quote.setHasOnRequest(hasOnRequest);
    quote.setSubtotal(money(subtotal));
    quote.setMarkupAmount(money(quote.getSubtotal() * quote.getMarkupPercent() / 100.0));
    quote.setTotal(money(quote.getSubtotal() + quote.getMarkupAmount()));
    quote.setAedSubtotal(quote.getSubtotal());
    quote.setAedMarkupAmount(quote.getMarkupAmount());
    quote.setAedTotal(quote.getTotal());
    quote.setRoeRate(1);
    quote.setConvertedValue(quote.getTotal());
    applyDirectPackage(quote, request);
    roeService.apply(quote, request.getCurrency());
    return quote;
  }

  private static int nightsForStay(Experience experience, String checkInValue, String checkOutValue) {
    LocalDate checkIn = LocalDate.parse(checkInValue);
    if (checkOutValue == null || checkOutValue.isBlank()) {
      throw new IllegalArgumentException("Check-out date is required for " + experience.getName());
    }
    LocalDate checkOut;
    try {
      checkOut = LocalDate.parse(checkOutValue.trim());
    } catch (Exception ex) {
      throw new IllegalArgumentException("Invalid check-out date for " + experience.getName());
    }
    if (!checkOut.isAfter(checkIn)) {
      throw new IllegalArgumentException("Check-out must be after check-in for " + experience.getName());
    }
    return (int) ChronoUnit.DAYS.between(checkIn, checkOut);
  }

  private static String timeSlotFor(Experience experience, String requested) {
    if (!experience.requiresTimeSlot()) {
      return null;
    }
    if (requested == null || requested.isBlank()) {
      throw new IllegalArgumentException("Select a timeslot for " + experience.getName() + ".");
    }
    return requested.trim();
  }

  private static String blankToNull(String value) {
    return value == null || value.isBlank() ? null : value.trim();
  }

  private static String normalizeDate(String visitDate, Experience experience) {
    boolean needsDate =
        "experience".equals(experience.getKind())
            || "stay".equals(experience.getKind())
            || experience.isDated();
    if (visitDate == null || visitDate.isBlank()) {
      if (needsDate) {
        throw new IllegalArgumentException("Visit date required for " + experience.getName());
      }
      return null;
    }
    try {
      return LocalDate.parse(visitDate.trim()).toString();
    } catch (Exception ex) {
      throw new IllegalArgumentException("Invalid visit date for " + experience.getName());
    }
  }

  static double money(double value) {
    return BigDecimal.valueOf(value).setScale(2, RoundingMode.HALF_UP).doubleValue();
  }

  private static String nextQuoteNumber() {
    String suffix = Integer.toString(RANDOM.nextInt(0xFFFF), 16).toUpperCase(Locale.ROOT);
    suffix = String.format("%4s", suffix).replace(' ', '0');
    return "QTE" + LocalDate.now().format(QUOTE_DAY) + suffix;
  }

  private static String existingQuoteNumber(String requested) {
    if (requested == null || requested.isBlank()) {
      return nextQuoteNumber();
    }
    return requested.trim().toUpperCase(Locale.ROOT);
  }

  private String selectedCountry(ParkQuoteRequest request) {
    if (request.getCountry() != null && !request.getCountry().isBlank()) {
      return ItemCountry.normalize(request.getCountry());
    }
    String inferred = null;
    for (ParkLineRequest item : request.getItems()) {
      if (item == null || item.getItemId() == null || item.getItemId().isBlank()) {
        continue;
      }
      String country = itemCountry(catalogService.require(item.getItemId().trim()));
      if (inferred == null) {
        inferred = country;
      } else if (!inferred.equals(country)) {
        throw new IllegalArgumentException(
            "Choose one country. This itinerary mixes items from different countries.");
      }
    }
    return inferred == null ? ItemCountry.UAE : inferred;
  }

  private static String itemCountry(Experience experience) {
    if (experience == null || experience.getCountry() == null || experience.getCountry().isBlank()) {
      return ItemCountry.UAE;
    }
    return ItemCountry.normalize(experience.getCountry());
  }

  private static void applyContent(ParkQuoteLine line, ParkLineRequest item) {
    if (item == null) {
      return;
    }
    if (item.getDescription() != null) {
      line.setDescription(item.getDescription());
    }
    if (item.getTermsAndCondition() != null) {
      line.setTermsAndCondition(item.getTermsAndCondition());
    }
    if (item.getIncluded() != null) {
      line.setIncluded(item.getIncluded());
    }
    if (item.getExcluded() != null) {
      line.setExcluded(item.getExcluded());
    }
  }

  private static void applyDirectPackage(ParkQuote quote, ParkQuoteRequest request) {
    boolean direct =
        request.isDirectCustomer()
            || hasText(request.getPackageInclusions())
            || hasText(request.getPackageExclusions())
            || hasText(request.getGuestInformation())
            || hasDayNarrative(request.getDayPlans());
    quote.setDirectCustomer(direct);
    quote.setCustomerName(blankToEmpty(request.getLeadName()));
    quote.setCustomerEmail(blankToEmpty(request.getEmail()));
    quote.setCustomerPhone(blankToEmpty(request.getPhone()));
    quote.setPackageInclusions(blankToEmpty(request.getPackageInclusions()));
    quote.setPackageExclusions(blankToEmpty(request.getPackageExclusions()));
    quote.setGuestInformation(blankToEmpty(request.getGuestInformation()));
    quote.setDayPlans(mergeDayPlans(request.getDayPlans(), quote.getItems()));
  }

  private static List<ParkDayPlan> mergeDayPlans(List<ParkDayPlan> requested, List<ParkQuoteLine> items) {
    Map<String, ParkDayPlan> byDate = new LinkedHashMap<>();
    if (requested != null) {
      for (ParkDayPlan plan : requested) {
        if (plan == null || !hasText(plan.getDate())) {
          continue;
        }
        String date = plan.getDate().trim();
        ParkDayPlan copy = new ParkDayPlan();
        copy.setDate(date);
        copy.setTitle(blankToEmpty(plan.getTitle()));
        copy.setNote(blankToEmpty(plan.getNote()));
        byDate.put(date, copy);
      }
    }
    if (items != null) {
      for (ParkQuoteLine line : items) {
        if (line == null || line.isStay() || !hasText(line.getVisitDate())) {
          continue;
        }
        ParkDayPlan plan =
            byDate.computeIfAbsent(
                line.getVisitDate(),
                date -> {
                  ParkDayPlan next = new ParkDayPlan();
                  next.setDate(date);
                  return next;
                });
        if (!hasText(plan.getTitle()) && hasText(line.getName())) {
          plan.setTitle(line.getName());
        }
      }
    }
    List<ParkDayPlan> plans = new ArrayList<>(byDate.values());
    plans.sort(Comparator.comparing(ParkDayPlan::getDate, Comparator.nullsLast(String::compareTo)));
    return plans;
  }

  private static boolean hasDayNarrative(List<ParkDayPlan> plans) {
    if (plans == null) {
      return false;
    }
    return plans.stream().anyMatch(plan -> plan != null && (hasText(plan.getNote()) || hasText(plan.getTitle())));
  }

  private static boolean hasText(String value) {
    return value != null && !value.isBlank();
  }

  private static String blankToEmpty(String value) {
    return value == null ? "" : value.trim();
  }
}
