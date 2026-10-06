package com.ruzinn.serviceimpl;

import com.ruzinn.dto.AuthSession;
import com.ruzinn.dto.ParkQuote;
import com.ruzinn.dto.ParkQuoteLine;
import com.ruzinn.dto.RoeRateView;
import com.ruzinn.entity.RoeMaster;
import com.ruzinn.repository.RoeMasterRepository;
import com.ruzinn.service.RoeService;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RoeServiceImpl implements RoeService {

  private static final Logger log = LoggerFactory.getLogger(RoeServiceImpl.class);

  private final RoeMasterRepository repository;

  public RoeServiceImpl(RoeMasterRepository repository) {
    this.repository = repository;
  }

  @Override
  @Transactional
  public void seedDefaults() {
    seed(USD, DEFAULT_USD);
    seed(INR, DEFAULT_INR);
  }

  @Override
  @Transactional(readOnly = true)
  public List<RoeMaster> listMaster() {
    List<RoeMaster> rows = new ArrayList<>();
    for (String currency : MASTER_CURRENCIES) {
      repository.findById(currency).ifPresent(rows::add);
    }
    return rows;
  }

  @Override
  @Transactional(readOnly = true)
  public List<RoeRateView> quoteRates() {
    List<RoeRateView> rates = new ArrayList<>();
    rates.add(RoeRateView.aed());
    for (RoeMaster row : listMaster()) {
      rates.add(RoeRateView.from(row));
    }
    return rates;
  }

  @Override
  public String normalizeQuoteCurrency(String currency) {
    if (currency == null || currency.isBlank()) {
      return AED;
    }
    String code = currency.trim().toUpperCase(Locale.ROOT);
    if (QUOTE_CURRENCIES.contains(code)) {
      return code;
    }
    throw new IllegalArgumentException("Quote currency must be AED, USD, or INR");
  }

  @Override
  @Transactional(readOnly = true)
  public BigDecimal rateFor(String currency) {
    String code = normalizeQuoteCurrency(currency);
    if (AED.equals(code)) {
      return BigDecimal.ONE.setScale(6, RoundingMode.HALF_UP);
    }
    RoeMaster row = repository.findById(code).orElse(null);
    if (row == null) {
      log.error("ROE is not configured for {}; quotes in {} cannot be priced", code, code);
      throw new IllegalStateException("ROE is not configured for " + code);
    }
    return row.getRate();
  }

  @Override
  @Transactional(readOnly = true)
  public void apply(ParkQuote quote, String requestedCurrency) {
    if (quote == null) {
      return;
    }
    String currency = normalizeQuoteCurrency(requestedCurrency);
    BigDecimal rate = rateFor(currency);
    double rateValue = rate.doubleValue();
    quote.setCurrency(currency);
    quote.setRoeRate(rateValue);
    quote.setSubtotal(convert(quote.getAedSubtotal(), rate));
    quote.setMarkupAmount(convert(quote.getAedMarkupAmount(), rate));
    quote.setTotal(convert(quote.getAedTotal(), rate));
    quote.setConvertedValue(quote.getTotal());
    if (quote.getItems() == null) {
      return;
    }
    for (ParkQuoteLine line : quote.getItems()) {
      if (line == null) {
        continue;
      }
      line.setCurrency(currency);
      line.setRoeRate(rateValue);
      double converted = convert(line.getAedLineTotal(), rate);
      line.setConvertedRate(converted);
      line.setLineTotal(converted);
      if (line.getAedAdultPrice() != null) {
        line.setAdultPrice(convert(line.getAedAdultPrice(), rate));
      }
      if (line.getAedChildPrice() != null) {
        line.setChildPrice(convert(line.getAedChildPrice(), rate));
      }
      line.setActualLineTotal(convert(line.getAedActualLineTotal(), rate));
      if (line.getAedActualAdultPrice() != null) {
        line.setActualAdultPrice(convert(line.getAedActualAdultPrice(), rate));
      }
      if (line.getAedActualChildPrice() != null) {
        line.setActualChildPrice(convert(line.getAedActualChildPrice(), rate));
      }
    }
  }

  @Override
  @Transactional
  public RoeMaster update(String currency, BigDecimal rate, AuthSession auth) {
    String code = currency == null ? "" : currency.trim().toUpperCase(Locale.ROOT);
    if (!MASTER_CURRENCIES.contains(code)) {
      throw new IllegalArgumentException("ROE master is only for USD and INR");
    }
    if (rate == null || rate.compareTo(BigDecimal.ZERO) <= 0) {
      throw new IllegalArgumentException("Enter a rate greater than 0");
    }
    RoeMaster row = repository.findById(code).orElseGet(RoeMaster::new);
    BigDecimal previous = row.getRate();
    row.setCurrency(code);
    row.setRate(rate.setScale(6, RoundingMode.HALF_UP));
    row.setUpdatedDate(Instant.now());
    row.setUpdatedBy(auth == null || auth.email() == null || auth.email().isBlank() ? "system" : auth.email());
    RoeMaster saved = repository.save(row);
    log.info("ROE AED->{} changed {} -> {} by {}", code, previous, saved.getRate(), saved.getUpdatedBy());
    return saved;
  }

  @Override
  @Transactional
  public void updateMaster(BigDecimal usdRate, BigDecimal inrRate, AuthSession auth) {
    update(USD, usdRate, auth);
    update(INR, inrRate, auth);
  }

  public static double convert(double aedAmount, BigDecimal rate) {
    BigDecimal amount = BigDecimal.valueOf(aedAmount).setScale(2, RoundingMode.HALF_UP);
    BigDecimal multiplier = rate == null ? BigDecimal.ONE : rate;
    return amount.multiply(multiplier).setScale(2, RoundingMode.HALF_UP).doubleValue();
  }

  private void seed(String currency, BigDecimal rate) {
    if (repository.existsById(currency)) {
      return;
    }
    RoeMaster row = new RoeMaster();
    row.setCurrency(currency);
    row.setRate(rate);
    row.setUpdatedDate(Instant.now());
    row.setUpdatedBy("system");
    repository.save(row);
  }
}
