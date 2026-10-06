package com.ruzinn.service;

import com.ruzinn.dto.AccountDocument;
import com.ruzinn.dto.AuthSession;
import com.ruzinn.dto.BookingAccount;
import com.ruzinn.dto.BookingPartyCatalog;
import com.ruzinn.dto.CompanyBrand;
import com.ruzinn.dto.CompanyView;
import com.ruzinn.dto.CompanyWithUsers;
import com.ruzinn.dto.CreateUserRequest;
import com.ruzinn.dto.ParkQuoteRequest;
import com.ruzinn.dto.RegisterRequest;
import com.ruzinn.dto.RegisterResponse;
import com.ruzinn.dto.UserView;
import com.ruzinn.entity.Company;
import java.math.BigDecimal;
import java.util.List;
import org.springframework.web.multipart.MultipartFile;

public interface AccountService {

  String SESSION_KEY = "authUser";

  void seedSuperAdmin();

  RegisterResponse register(RegisterRequest request);

  RegisterResponse register(
      RegisterRequest request, boolean requireProfile, AccountDocument vatCertificate, AccountDocument licence);

  RegisterResponse register(
      RegisterRequest request,
      boolean requireProfile,
      AccountDocument vatCertificate,
      AccountDocument licence,
      MultipartFile logo);

  AuthSession verifyRegistration(String email, String code);

  AuthSession login(String email, String password);

  UserView createUser(AuthSession actor, CreateUserRequest request);

  /**
   * Credit left for {@code companyId} (a parent-company user checking a child company it books
   * for), or for the actor's own company when {@code companyId} is blank.
   */
  BigDecimal remainingCredit(AuthSession actor, String companyId);

  /** Whether {@code companyId} (or the actor's own company when blank) may pay with credit. */
  boolean canUseCredit(AuthSession actor, String companyId);

  /** Charges the booked company's credit; {@code companyId} is the transaction party already resolved. */
  void chargeCredit(AuthSession actor, String companyId, BigDecimal amount);

  List<CompanyWithUsers> listCompaniesWithUsers(AuthSession actor);

  BookingAccount resolveBookingAccount(AuthSession auth, String guestEmail);

  BookingPartyCatalog bookingParty(AuthSession actor);

  void applyTransactionParty(ParkQuoteRequest request, AuthSession auth);

  List<UserView> listCompanyUsers(AuthSession actor, String companyId);

  List<UserView> listAllUsers(AuthSession actor);

  UserView setUserBlocked(AuthSession actor, String userId, boolean blocked);

  UserView resetUserPassword(AuthSession actor, String userId, String password);

  AuthSession requireSignedIn(AuthSession actor);

  void requireSuperAdmin(AuthSession actor);

  void requireCompanyAdmin(AuthSession actor);

  void requireCompanyAccess(AuthSession actor, String companyId);

  CompanyBrand brandingFor(AuthSession auth);

  CompanyBrand brandingForCompany(String companyId);

  byte[] logoBytes(CompanyBrand brand);

  CompanyView toCompanyView(Company company);
}
