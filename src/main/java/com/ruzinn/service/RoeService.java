package com.ruzinn.service;

import com.ruzinn.dto.AuthSession;
import com.ruzinn.dto.ParkQuote;
import com.ruzinn.dto.RoeRateView;
import com.ruzinn.entity.RoeMaster;
import java.math.BigDecimal;
import java.util.List;

public interface RoeService {

  String AED = "AED";
  String USD = "USD";
  String INR = "INR";
  List<String> MASTER_CURRENCIES = List.of(USD, INR);
  List<String> QUOTE_CURRENCIES = List.of(AED, USD, INR);
  BigDecimal DEFAULT_USD = new BigDecimal("0.272300");
  BigDecimal DEFAULT_INR = new BigDecimal("22.700000");

  void seedDefaults();

  List<RoeMaster> listMaster();

  List<RoeRateView> quoteRates();

  String normalizeQuoteCurrency(String currency);

  BigDecimal rateFor(String currency);

  void apply(ParkQuote quote, String requestedCurrency);

  RoeMaster update(String currency, BigDecimal rate, AuthSession auth);

  void updateMaster(BigDecimal usdRate, BigDecimal inrRate, AuthSession auth);
}
