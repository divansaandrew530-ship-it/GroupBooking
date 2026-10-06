package com.ruzinn.serviceimpl;

import com.ruzinn.config.AuthProperties;
import com.ruzinn.dto.AccountDocument;
import com.ruzinn.dto.AuthSession;
import com.ruzinn.dto.BookingAccount;
import com.ruzinn.dto.BookingPartyCatalog;
import com.ruzinn.dto.BookingPartyCompany;
import com.ruzinn.dto.BookingPartyUser;
import com.ruzinn.dto.CompanyBrand;
import com.ruzinn.dto.CompanyView;
import com.ruzinn.dto.CompanyWithUsers;
import com.ruzinn.dto.CreateUserRequest;
import com.ruzinn.dto.ParkQuoteRequest;
import com.ruzinn.dto.RegisterRequest;
import com.ruzinn.dto.RegisterResponse;
import com.ruzinn.dto.UserView;
import com.ruzinn.entity.AppUser;
import com.ruzinn.entity.Company;
import com.ruzinn.entity.CompanyRegistration;
import com.ruzinn.entity.EmailOtp;
import com.ruzinn.enums.EditableTable;
import com.ruzinn.exception.UnauthorizedException;
import com.ruzinn.repository.AppUserRepository;
import com.ruzinn.repository.CompanyRegistrationRepository;
import com.ruzinn.repository.CompanyRepository;
import com.ruzinn.service.AccountService;
import com.ruzinn.service.ChangeHistoryService;
import com.ruzinn.service.CompanyLogoStore;
import com.ruzinn.service.OtpMailService;
import com.ruzinn.service.OtpService;
import com.ruzinn.service.RowHistoryService;
import com.ruzinn.util.CompanyLocations;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
public class AccountServiceImpl implements AccountService {

  private static final Logger log = LoggerFactory.getLogger(AccountServiceImpl.class);
  private static final String SUPER_ADMIN_USER_ID = "USR-SUPER";
  private static final Long SUPER_ADMIN_COMPANY_ID = 1L;
  private static final List<String> LEGACY_SUPER_ADMIN_EMAILS = List.of("admin@rathin.local");

  private final CompanyRepository companyRepository;
  private final CompanyRegistrationRepository registrationRepository;
  private final AppUserRepository userRepository;
  private final OtpService otpService;
  private final PasswordEncoder passwordEncoder;
  private final AuthProperties authProperties;
  private final ChangeHistoryService changeHistoryService;
  private final RowHistoryService rowHistoryService;
  private final CompanyLogoStore companyLogoStore;
  private final OtpMailService otpMailService;

  public AccountServiceImpl(
      CompanyRepository companyRepository,
      CompanyRegistrationRepository registrationRepository,
      AppUserRepository userRepository,
      OtpService otpService,
      PasswordEncoder passwordEncoder,
      AuthProperties authProperties,
      ChangeHistoryService changeHistoryService,
      RowHistoryService rowHistoryService,
      CompanyLogoStore companyLogoStore,
      OtpMailService otpMailService) {
    this.companyRepository = companyRepository;
    this.registrationRepository = registrationRepository;
    this.userRepository = userRepository;
    this.otpService = otpService;
    this.passwordEncoder = passwordEncoder;
    this.authProperties = authProperties;
    this.changeHistoryService = changeHistoryService;
    this.rowHistoryService = rowHistoryService;
    this.companyLogoStore = companyLogoStore;
    this.otpMailService = otpMailService;
  }

  @Override
  @Transactional
  public void seedSuperAdmin() {
    String email = authProperties.getSuperAdminEmail().trim().toLowerCase(Locale.ROOT);
    String name = authProperties.getSuperAdminName();
    String password = authProperties.getSuperAdminPassword();
    Instant now = Instant.now();

    AppUser admin = userRepository.findByEmailIgnoreCase(email).orElse(null);
    if (admin == null) {
      admin = userRepository.findById(SUPER_ADMIN_USER_ID).orElse(null);
    }
    if (admin == null) {
      for (String legacy : LEGACY_SUPER_ADMIN_EMAILS) {
        admin = userRepository.findByEmailIgnoreCase(legacy).orElse(null);
        if (admin != null) {
          break;
        }
      }
    }

    Company company = null;
    if (admin != null && admin.getCompanyId() != null) {
      company = companyRepository.findById(admin.getCompanyId()).orElse(null);
    }
    if (company == null) {
      company = companyRepository.findById(SUPER_ADMIN_COMPANY_ID).orElse(null);
    }
    if (company == null) {
      company = new Company();
      company.setPhone("");
      company.setCreatedDate(now);
      company.setReviewedDate(now);
      company.setReviewedBy("system");
      company.setCreditLimit(new BigDecimal("100000.00"));
      company.setCreditUsed(BigDecimal.ZERO);
    }
    // The parent company used to be called WeegooTour; rename it (once) to Rathin.
    if (company.getCompanyName() == null
        || company.getCompanyName().isBlank()
        || "WeegooTour".equalsIgnoreCase(company.getCompanyName().trim())) {
      log.info("Parent company {} renamed '{}' -> 'Rathin'", company.getCompanyId(), company.getCompanyName());
      company.setCompanyName("Rathin");
    }
    company.setEmail(email);
    company.setStatus(Company.STATUS_ACTIVE);
    company.setEmailVerified(true);
    company.setCompanyType(Company.TYPE_PARENT);
    company = companyRepository.save(company);
    ensureCompanyTypes(company.getCompanyId());

    if (admin == null) {
      admin = new AppUser();
      admin.setUserId(SUPER_ADMIN_USER_ID);
      admin.setCreatedDate(now);
      admin.setCreatedBy("system");
    }
    admin.setUserName(name);
    admin.setEmail(email);
    admin.setPasswordHash(passwordEncoder.encode(password));
    admin.setRole(AppUser.ROLE_SUPER_ADMIN);
    admin.setCompanyId(company.getCompanyId());
    admin.setStatus(AppUser.STATUS_ACTIVE);
    admin.setEmailVerified(true);
    userRepository.save(admin);
    log.info("Super admin {} ready in parent company {} ({})", email, company.getCompanyId(), company.getCompanyName());
  }

  @Override
  @Transactional
  public RegisterResponse register(RegisterRequest request) {
    return register(request, false, null, null, null);
  }

  @Override
  @Transactional
  public RegisterResponse register(
      RegisterRequest request, boolean requireProfile, AccountDocument vatCertificate, AccountDocument licence) {
    return register(request, requireProfile, vatCertificate, licence, null);
  }

  @Override
  @Transactional
  public RegisterResponse register(
      RegisterRequest request,
      boolean requireProfile,
      AccountDocument vatCertificate,
      AccountDocument licence,
      MultipartFile logo) {
    String companyName = required(request == null ? null : request.companyName(), "Company name");
    String userName = required(request == null ? null : request.userName(), "Owner name");
    String email = normalizeEmail(request == null ? null : request.email());
    String password = required(request == null ? null : request.password(), "Password");
    if (password.length() < 8) {
      throw new IllegalArgumentException("Password must be at least 8 characters");
    }
    if (requireProfile) {
      required(request.registrationNo(), "Company registration no");
      required(request.address(), "Address");
      required(request.country(), "Country");
      required(request.city(), "City");
      required(request.zipCode(), "Zip code");
      required(request.phone(), "Mobile no");
      required(request.ownerTelephone(), "Telephone no");
      required(request.operationsStaffName(), "Operations staff name");
      required(request.operationsStaffNumber(), "Operation staff number");
      required(request.financeName(), "Finance contact name");
      required(request.financeMobile(), "Finance mobile no");
      required(request.financeTelephone(), "Finance telephone no");
      required(request.financeEmail(), "Finance email address");
      CompanyLocations.requireCountryAndCity(request.country().trim(), request.city().trim());
      normalizeEmail(request.financeEmail());
    }
    // A registration whose email OTP was never verified is not an account yet: "Send OTP" again
    // updates that same company/user and sends a new code instead of failing as a duplicate.
    AppUser existingUser = userRepository.findByEmailIgnoreCase(email).orElse(null);
    Company existingCompany = companyRepository.findByEmailIgnoreCase(email).orElse(null);
    if ((existingUser != null && existingUser.isEmailVerified())
        || (existingCompany != null && existingCompany.isEmailVerified())) {
      log.warn("Registration refused for {}: a verified account already exists", email);
      throw new IllegalArgumentException("An account already exists for " + email);
    }
    if (existingCompany == null && existingUser != null && existingUser.getCompanyId() != null) {
      existingCompany = companyRepository.findById(existingUser.getCompanyId()).orElse(null);
    }
    boolean retry = existingUser != null || existingCompany != null;

    Instant now = Instant.now();
    Company company = existingCompany != null ? existingCompany : new Company();
    if (existingCompany == null) {
      company.setCreatedDate(now);
      company.setCreditLimit(BigDecimal.ZERO);
      company.setCreditUsed(BigDecimal.ZERO);
    }
    company.setCompanyName(companyName);
    company.setEmail(email);
    company.setPhone(blankToEmpty(request.phone()));
    company.setStatus(Company.STATUS_PENDING);
    company.setCompanyType(Company.TYPE_CHILD);
    company.setEmailVerified(false);
    company.setRegistrationNo(blankToEmpty(request.registrationNo()));
    company.setAddress(blankToEmpty(request.address()));
    company.setWebsite(blankToEmpty(request.website()));
    company.setCountry(blankToEmpty(request.country()));
    company.setCity(blankToEmpty(request.city()));
    company.setZipCode(blankToEmpty(request.zipCode()));
    company.setOwnerName(userName);
    company.setOwnerTelephone(blankToEmpty(request.ownerTelephone()));
    company.setOperationsStaffName(blankToEmpty(request.operationsStaffName()));
    company.setOperationsStaffNumber(blankToEmpty(request.operationsStaffNumber()));
    company.setFinanceName(blankToEmpty(request.financeName()));
    company.setFinanceMobile(blankToEmpty(request.financeMobile()));
    company.setFinanceTelephone(blankToEmpty(request.financeTelephone()));
    company.setFinanceEmail(blankToEmpty(request.financeEmail()));
    applyDocument(company, "vat", vatCertificate);
    applyDocument(company, "licence", licence);
    company = companyRepository.saveAndFlush(company);
    applyLogo(company, logo);
    if (company.hasLogo()) {
      company = companyRepository.save(company);
    }

    AppUser user = existingUser != null ? existingUser : new AppUser();
    if (existingUser == null) {
      user.setUserId(newId("USR"));
      user.setCreatedDate(now);
      user.setCreatedBy(user.getUserId());
    }
    user.setUserName(userName);
    user.setEmail(email);
    user.setPasswordHash(passwordEncoder.encode(password));
    user.setRole(AppUser.ROLE_COMPANY_ADMIN);
    user.setCompanyId(company.getCompanyId());
    user.setStatus(AppUser.STATUS_PENDING);
    user.setEmailVerified(false);
    userRepository.save(user);

    String otp = otpService.issue(email, EmailOtp.PURPOSE_REGISTER);
    otpMailService.sendRegistrationOtp(email, otp, authProperties.getOtpTtlMinutes());
    log.info(
        "{} company {} ({}) with admin user {} ({}); status PENDING, waiting for email OTP",
        retry ? "Re-sent registration for unverified" : "Registered",
        company.getCompanyId(), companyName, user.getUserId(), email);
    return new RegisterResponse(
        company.getCompanyId(),
        user.getUserId(),
        email,
        company.getStatus(),
        authProperties.isOtpEcho() ? otp : null);
  }

  @Override
  @Transactional
  public AuthSession verifyRegistration(String email, String code) {
    String normalized = normalizeEmail(email);
    otpService.consume(normalized, EmailOtp.PURPOSE_REGISTER, code);
    AppUser user =
        userRepository
            .findByEmailIgnoreCase(normalized)
            .orElseThrow(() -> new IllegalArgumentException("No registration found for this email"));
    Company company =
        companyRepository
            .findById(user.getCompanyId())
            .orElseThrow(() -> new IllegalArgumentException("Company not found"));
    user.setEmailVerified(true);
    company.setEmailVerified(true);
    saveSubmittedRegistration(company);
    log.info(
        "Email verified for {} (company {} {}); company now waits for Super Admin approval",
        normalized, company.getCompanyId(), company.getCompanyName());
    return toSession(user, company);
  }

  @Override
  @Transactional(readOnly = true)
  public AuthSession login(String email, String password) {
    if (email == null || email.isBlank() || password == null || password.isBlank()) {
      throw new UnauthorizedException("Invalid email or password");
    }
    AppUser user = userRepository.findByEmailIgnoreCase(normalizeEmail(email)).orElse(null);
    if (user == null) {
      log.warn("Login failed for {}: no such user", email);
      throw new UnauthorizedException("Invalid email or password");
    }
    if (!passwordEncoder.matches(password, user.getPasswordHash())) {
      log.warn("Login failed for {}: wrong password", email);
      throw new UnauthorizedException("Invalid email or password");
    }
    if (!user.isEmailVerified() && !AppUser.ROLE_SUPER_ADMIN.equals(user.getRole())) {
      log.warn("Login blocked for {}: email not verified", email);
      throw new UnauthorizedException("Verify your email OTP before signing in");
    }
    if (AppUser.STATUS_DISABLED.equals(user.getStatus())) {
      log.warn("Login blocked for {}: user disabled", email);
      throw new UnauthorizedException("This user is disabled");
    }
    Company company = companyRepository.findById(user.getCompanyId()).orElse(null);
    if (company == null) {
      log.warn("Login blocked for {}: company {} not found", email, user.getCompanyId());
      throw new UnauthorizedException("Company not found");
    }
    if (Company.STATUS_REJECTED.equals(company.getStatus())) {
      log.warn("Login blocked for {}: company {} was rejected", email, company.getCompanyId());
      throw new UnauthorizedException("This company registration was rejected");
    }
    log.info("Login ok for {} (company {}, role {})", email, company.getCompanyId(), user.getRole());
    return toSession(user, company);
  }

  @Override
  @Transactional
  public UserView createUser(AuthSession actor, CreateUserRequest request) {
    requireCompanyAdmin(actor);
    if (!actor.companyActive()) {
      throw new IllegalStateException("Company must be Active before adding users");
    }
    String userName = required(request == null ? null : request.userName(), "User name");
    String email = normalizeEmail(request == null ? null : request.email());
    String password = required(request == null ? null : request.password(), "Password");
    if (password.length() < 8) {
      throw new IllegalArgumentException("Password must be at least 8 characters");
    }
    if (userRepository.existsByEmailIgnoreCase(email)) {
      throw new IllegalArgumentException("An account already exists for " + email);
    }
    Long companyId =
        actor.superAdmin() && request.companyId() != null && !request.companyId().isBlank()
            ? parseCompanyId(request.companyId())
            : parseCompanyId(actor.companyId());
    Company company =
        companyRepository
            .findById(companyId)
            .orElseThrow(() -> new IllegalArgumentException("Company not found"));
    if (!actor.superAdmin() && !companyId.equals(parseCompanyId(actor.companyId()))) {
      throw new UnauthorizedException("You can only add users to your company");
    }
    if (!Company.STATUS_ACTIVE.equals(company.getStatus())) {
      throw new IllegalStateException("Company must be Active before adding users");
    }
    String role = request.role() == null || request.role().isBlank()
        ? AppUser.ROLE_COMPANY_USER
        : request.role().trim().toUpperCase(Locale.ROOT);
    if (!AppUser.ROLE_COMPANY_ADMIN.equals(role) && !AppUser.ROLE_COMPANY_USER.equals(role)) {
      throw new IllegalArgumentException("Role must be COMPANY_ADMIN or COMPANY_USER");
    }
    AppUser user = new AppUser();
    user.setUserId(newId("USR"));
    user.setUserName(userName);
    user.setEmail(email);
    user.setPasswordHash(passwordEncoder.encode(password));
    user.setRole(role);
    user.setCompanyId(companyId);
    user.setStatus(AppUser.STATUS_ACTIVE);
    user.setEmailVerified(true);
    user.setCreatedDate(Instant.now());
    user.setCreatedBy(actor.userId());
    userRepository.save(user);
    changeHistoryService.record(
        ChangeHistoryService.APP_USER,
        user.getUserId(),
        null,
        ChangeHistoryService.USER_CREATE,
        "email",
        "",
        user.getEmail(),
        String.valueOf(company.getCompanyId()),
        company.getCompanyName(),
        actor);
    log.info(
        "User {} ({}, {}) created in company {} by {}",
        user.getUserId(), email, role, company.getCompanyId(), actor.email());
    return toUserView(user, company);
  }

  @Override
  @Transactional(readOnly = true)
  public BigDecimal remainingCredit(AuthSession actor, String companyId) {
    if (actor == null || actor.companyId() == null) {
      return BigDecimal.ZERO;
    }
    return creditCompany(actor, companyId).map(Company::remainingCredit).orElse(BigDecimal.ZERO);
  }

  @Override
  @Transactional(readOnly = true)
  public boolean canUseCredit(AuthSession actor, String companyId) {
    if (actor == null || actor.companyId() == null) {
      return false;
    }
    return creditCompany(actor, companyId).map(company -> Company.STATUS_ACTIVE.equals(company.getStatus())).orElse(false);
  }

  @Override
  @Transactional
  public void chargeCredit(AuthSession actor, String companyId, BigDecimal amount) {
    requireSignedIn(actor);
    if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
      return;
    }
    Company company =
        creditCompany(actor, companyId).orElseThrow(() -> new IllegalArgumentException("Company not found"));
    if (!Company.STATUS_ACTIVE.equals(company.getStatus())) {
      throw new IllegalStateException("Company credit is available after Super Admin approval");
    }
    BigDecimal charge = amount.setScale(2, RoundingMode.HALF_UP);
    if (company.remainingCredit().compareTo(charge) < 0) {
      log.warn(
          "Credit charge AED {} refused for company {}: only AED {} left",
          charge, company.getCompanyId(), company.remainingCredit());
      throw new IllegalStateException(
          "Insufficient company credit. Remaining AED " + company.remainingCredit() + ".");
    }
    BigDecimal previousUsed = company.getCreditUsed();
    rowHistoryService.archive(
        EditableTable.COMPANY, company.getCompanyId(), ChangeHistoryService.CREDIT_CHARGE, "credit_used");
    company.setCreditUsed(company.getCreditUsed().add(charge));
    changeHistoryService.record(
        ChangeHistoryService.COMPANY,
        String.valueOf(company.getCompanyId()),
        null,
        ChangeHistoryService.CREDIT_CHARGE,
        "credit_used",
        previousUsed,
        company.getCreditUsed(),
        String.valueOf(company.getCompanyId()),
        company.getCompanyName(),
        actor);
    log.info(
        "Charged AED {} to company {} ({}) by {}: credit used {} -> {}, remaining {}",
        charge, company.getCompanyId(), company.getCompanyName(), actor.email(),
        previousUsed, company.getCreditUsed(), company.remainingCredit());
  }

  @Override
  @Transactional(readOnly = true)
  public List<CompanyWithUsers> listCompaniesWithUsers(AuthSession actor) {
    requireSuperAdmin(actor);
    return companyRepository.findAllByOrderByCreatedDateDesc().stream()
        // Registrations whose email OTP was never verified are not shown until they are.
        .filter(Company::isEmailVerified)
        .map(
            company ->
                new CompanyWithUsers(
                    toCompanyView(company),
                    userRepository.findByCompanyIdOrderByCreatedDateAsc(company.getCompanyId()).stream()
                        .map(user -> toUserView(user, company))
                        .toList()))
        .toList();
  }

  @Override
  @Transactional(readOnly = true)
  public BookingAccount resolveBookingAccount(AuthSession auth, String guestEmail) {
    String username = auth != null && auth.email() != null && !auth.email().isBlank()
        ? auth.email().trim()
        : (guestEmail == null ? "" : guestEmail.trim());
    if (username.isBlank()) {
      return BookingAccount.direct("");
    }
    return userRepository
        .findByEmailIgnoreCase(username)
        .flatMap(
            user ->
                companyRepository
                    .findById(user.getCompanyId())
                    .map(
                        company ->
                            new BookingAccount(
                                user.getEmail(),
                                company.getCompanyName(),
                                String.valueOf(company.getCompanyId()))))
        .orElseGet(() -> BookingAccount.direct(username));
  }

  @Override
  @Transactional(readOnly = true)
  public BookingPartyCatalog bookingParty(AuthSession actor) {
    // No sign-in required: anonymous callers get the full parent-company view.
    if (actor != null && !actor.parentCompany()) {
      return BookingPartyCatalog.forChild(actor);
    }
    List<Company> companies = companyRepository.findAllByOrderByCreatedDateDesc();
    List<BookingPartyCompany> children =
        companies.stream()
            .filter(company -> !company.parentCompany())
            // Only approved companies can be booked for; pending ones wait for Super Admin review.
            .filter(company -> Company.STATUS_ACTIVE.equals(company.getStatus()))
            .sorted(
                Comparator.comparing(
                    company -> blankToEmpty(company.getCompanyName()), String.CASE_INSENSITIVE_ORDER))
            .map(
                company ->
                    new BookingPartyCompany(
                        String.valueOf(company.getCompanyId()),
                        company.getCompanyName(),
                        partyUsers(company.getCompanyId())))
            .toList();
    List<BookingPartyUser> owners =
        companies.stream()
            .filter(Company::parentCompany)
            .flatMap(company -> partyUsers(company.getCompanyId()).stream())
            .toList();
    return new BookingPartyCatalog(
        true,
        actor == null ? "" : actor.companyId(),
        actor == null ? "" : actor.companyName(),
        actor == null ? "" : actor.userName(),
        actor == null ? "" : actor.email(),
        children,
        owners);
  }

  @Override
  @Transactional(readOnly = true)
  public void applyTransactionParty(ParkQuoteRequest request, AuthSession auth) {
    if (request == null || auth == null) {
      return;
    }
    if (!auth.parentCompany()) {
      applyChildTransactionParty(request, auth);
      return;
    }
    applyParentTransactionParty(request, auth);
  }

  @Override
  @Transactional(readOnly = true)
  public List<UserView> listCompanyUsers(AuthSession actor, String companyId) {
    requireSignedIn(actor);
    Long resolved =
        parseCompanyId(
            actor.parentCompany() && companyId != null && !companyId.isBlank()
                ? companyId.trim()
                : actor.companyId());
    if (!actor.parentCompany() && !resolved.equals(parseCompanyId(actor.companyId()))) {
      throw new UnauthorizedException("You can only view users in your company");
    }
    Company company =
        companyRepository
            .findById(resolved)
            .orElseThrow(() -> new IllegalArgumentException("Company not found"));
    return userRepository.findByCompanyIdOrderByCreatedDateAsc(resolved).stream()
        .map(user -> toUserView(user, company))
        .toList();
  }

  @Override
  @Transactional(readOnly = true)
  public List<UserView> listAllUsers(AuthSession actor) {
    requireSuperAdmin(actor);
    Map<Long, Company> companies =
        companyRepository.findAll().stream()
            .filter(company -> company.getCompanyId() != null)
            .collect(Collectors.toMap(Company::getCompanyId, company -> company, (left, right) -> left));
    return userRepository.findAll().stream()
        .sorted(
            Comparator.comparing((AppUser user) -> companyName(companies.get(user.getCompanyId())), String.CASE_INSENSITIVE_ORDER)
                .thenComparing(user -> blankToEmpty(user.getUserName()), String.CASE_INSENSITIVE_ORDER)
                .thenComparing(user -> blankToEmpty(user.getEmail()), String.CASE_INSENSITIVE_ORDER))
        .map(user -> toUserView(user, companies.get(user.getCompanyId())))
        .toList();
  }

  @Override
  @Transactional
  public UserView setUserBlocked(AuthSession actor, String userId, boolean blocked) {
    requireSuperAdmin(actor);
    AppUser user =
        userRepository
            .findById(required(userId, "User ID"))
            .orElseThrow(() -> new IllegalArgumentException("User not found"));
    if (AppUser.ROLE_SUPER_ADMIN.equals(user.getRole())) {
      throw new IllegalArgumentException("Super Admin accounts cannot be blocked");
    }
    if (blocked && Objects.equals(actor.userId(), user.getUserId())) {
      throw new IllegalArgumentException("You cannot block your own account");
    }
    Company company = user.getCompanyId() == null ? null : companyRepository.findById(user.getCompanyId()).orElse(null);
    String previousStatus = user.getStatus();
    String nextStatus =
        blocked
            ? AppUser.STATUS_DISABLED
            : (user.isEmailVerified() ? AppUser.STATUS_ACTIVE : AppUser.STATUS_PENDING);
    if (Objects.equals(previousStatus, nextStatus)) {
      return toUserView(user, company);
    }
    rowHistoryService.archive(
        EditableTable.APP_USER, user.getUserId(), ChangeHistoryService.STATUS_CHANGE, "status");
    user.setStatus(nextStatus);
    userRepository.save(user);
    changeHistoryService.record(
        ChangeHistoryService.APP_USER,
        user.getUserId(),
        null,
        ChangeHistoryService.STATUS_CHANGE,
        "status",
        previousStatus,
        user.getStatus(),
        user.getCompanyId() == null ? "" : String.valueOf(user.getCompanyId()),
        company == null ? "" : company.getCompanyName(),
        actor);
    log.info(
        "User {} ({}) {} by {}: status {} -> {}",
        user.getUserId(), user.getEmail(), blocked ? "blocked" : "unblocked", actor.email(), previousStatus, nextStatus);
    return toUserView(user, company);
  }

  @Override
  @Transactional
  public UserView resetUserPassword(AuthSession actor, String userId, String password) {
    requireSuperAdmin(actor);
    AppUser user =
        userRepository
            .findById(required(userId, "User ID"))
            .orElseThrow(() -> new IllegalArgumentException("User not found"));
    if (AppUser.ROLE_SUPER_ADMIN.equals(user.getRole())) {
      throw new IllegalArgumentException("Super Admin passwords cannot be reset here");
    }
    String nextPassword = required(password, "Password");
    if (nextPassword.length() < 8) {
      throw new IllegalArgumentException("Password must be at least 8 characters");
    }
    Company company = user.getCompanyId() == null ? null : companyRepository.findById(user.getCompanyId()).orElse(null);
    rowHistoryService.archive(
        EditableTable.APP_USER, user.getUserId(), ChangeHistoryService.PASSWORD_RESET, "password_hash");
    user.setPasswordHash(passwordEncoder.encode(nextPassword));
    userRepository.save(user);
    changeHistoryService.record(
        ChangeHistoryService.APP_USER,
        user.getUserId(),
        null,
        ChangeHistoryService.PASSWORD_RESET,
        "password",
        "",
        "reset",
        user.getCompanyId() == null ? "" : String.valueOf(user.getCompanyId()),
        company == null ? "" : company.getCompanyName(),
        actor);
    log.info("Password reset for user {} ({}) by {}", user.getUserId(), user.getEmail(), actor.email());
    return toUserView(user, company);
  }

  @Override
  @Transactional(readOnly = true)
  public AuthSession requireSignedIn(AuthSession actor) {
    if (actor == null) {
      throw new UnauthorizedException("Sign in required");
    }
    return actor;
  }

  @Override
  public void requireSuperAdmin(AuthSession actor) {
    requireSignedIn(actor);
    if (!actor.superAdmin()) {
      throw new UnauthorizedException("Super admin access required");
    }
  }

  @Override
  public void requireCompanyAdmin(AuthSession actor) {
    requireSignedIn(actor);
    if (!actor.companyAdmin()) {
      throw new UnauthorizedException("Company admin access required");
    }
  }

  @Override
  public void requireCompanyAccess(AuthSession actor, String companyId) {
    requireSignedIn(actor);
    if (actor.parentCompany()) {
      return;
    }
    if (companyId == null || !parseCompanyId(companyId).equals(parseCompanyId(actor.companyId()))) {
      throw new UnauthorizedException("You can only view bookings for your company");
    }
  }

  @Override
  @Transactional(readOnly = true)
  public CompanyBrand brandingFor(AuthSession auth) {
    if (auth == null || auth.companyId() == null || auth.companyId().isBlank()) {
      return CompanyBrand.rathin();
    }
    return brandingForCompany(auth.companyId());
  }

  @Override
  @Transactional(readOnly = true)
  public CompanyBrand brandingForCompany(String companyId) {
    if (companyId == null || companyId.isBlank()) {
      return CompanyBrand.rathin();
    }
    Company company = companyRepository.findById(parseCompanyId(companyId)).orElse(null);
    if (company == null) {
      return CompanyBrand.rathin();
    }
    boolean hasLogo = company.hasLogo() && companyLogoStore.exists(company.getLogoPath());
    return new CompanyBrand(
        String.valueOf(company.getCompanyId()),
        blankToEmpty(company.getCompanyName()),
        blankToEmpty(company.getAddress()),
        blankToEmpty(company.getEmail()),
        firstNonBlank(company.getPhone(), company.getOwnerTelephone()),
        blankToEmpty(company.getWebsite()),
        placeLine(company),
        hasLogo,
        hasLogo ? "/companies/" + company.getCompanyId() + "/logo" : "");
  }

  @Override
  public byte[] logoBytes(CompanyBrand brand) {
    if (brand == null || !brand.hasLogo() || brand.companyId() == null || brand.companyId().isBlank()) {
      return null;
    }
    Company company = companyRepository.findById(parseCompanyId(brand.companyId())).orElse(null);
    if (company == null || !company.hasLogo()) {
      return null;
    }
    return companyLogoStore.readIfPresent(company.getLogoPath());
  }

  private void applyLogo(Company company, MultipartFile logo) {
    if (company == null || logo == null || logo.isEmpty()) {
      return;
    }
    String path = companyLogoStore.save(company.getCompanyId(), logo);
    company.setLogoPath(path);
    company.setLogoFileName(fileName(logo.getOriginalFilename(), "company-logo"));
    company.setLogoContentType(blankToEmpty(logo.getContentType()));
  }

  private static String placeLine(Company company) {
    String city = blankToEmpty(company.getCity());
    String country = blankToEmpty(company.getCountry());
    String zip = blankToEmpty(company.getZipCode());
    StringBuilder place = new StringBuilder();
    if (!city.isBlank()) {
      place.append(city);
    }
    if (!country.isBlank()) {
      if (!place.isEmpty()) {
        place.append(", ");
      }
      place.append(country);
    }
    if (!zip.isBlank()) {
      if (!place.isEmpty()) {
        place.append(" ");
      }
      place.append(zip);
    }
    return place.toString();
  }

  @Override
  public CompanyView toCompanyView(Company company) {
    return new CompanyView(
        company.getCompanyId(),
        company.getCompanyName(),
        company.getEmail(),
        company.getPhone(),
        company.getStatus(),
        company.getCompanyType(),
        company.isEmailVerified(),
        company.getCreatedDate(),
        company.getReviewedDate(),
        company.getReviewedBy(),
        company.getReviewNote(),
        company.getCreditLimit(),
        company.getCreditUsed(),
        company.getRegistrationNo(),
        company.getAddress(),
        company.getWebsite(),
        company.getCountry(),
        company.getCity(),
        company.getZipCode(),
        company.getOwnerName(),
        company.getOwnerTelephone(),
        company.getOperationsStaffName(),
        company.getOperationsStaffNumber(),
        company.getFinanceName(),
        company.getFinanceMobile(),
        company.getFinanceTelephone(),
        company.getFinanceEmail(),
        company.hasVatCertificate(),
        company.getVatFileName(),
        company.hasLicence(),
        company.getLicenceFileName(),
        company.hasLogo() && companyLogoStore.exists(company.getLogoPath()),
        company.getLogoFileName(),
        company.hasLogo() ? "/companies/" + company.getCompanyId() + "/logo" : "");
  }

  private UserView toUserView(AppUser user, Company company) {
    return new UserView(
        user.getUserId(),
        user.getUserName(),
        user.getEmail(),
        user.getRole(),
        user.getCompanyId(),
        company == null ? "" : company.getCompanyName(),
        company == null ? "" : company.getStatus(),
        user.getStatus(),
        user.isEmailVerified(),
        user.getCreatedDate());
  }

  private static String companyName(Company company) {
    return company == null || company.getCompanyName() == null ? "" : company.getCompanyName();
  }

  private AuthSession toSession(AppUser user, Company company) {
    return new AuthSession(
        user.getUserId(),
        user.getUserName(),
        user.getEmail(),
        user.getRole(),
        String.valueOf(user.getCompanyId()),
        company.getCompanyName(),
        company.getStatus(),
        company.getCompanyType(),
        user.isEmailVerified());
  }

  private void ensureCompanyTypes(Long parentCompanyId) {
    if (parentCompanyId == null) {
      return;
    }
    for (Company existing : companyRepository.findAll()) {
      String expected =
          parentCompanyId.equals(existing.getCompanyId()) ? Company.TYPE_PARENT : Company.TYPE_CHILD;
      if (!expected.equals(existing.getCompanyType())) {
        existing.setCompanyType(expected);
        companyRepository.save(existing);
      }
    }
  }

  private void applyChildTransactionParty(ParkQuoteRequest request, AuthSession auth) {
    Company company =
        companyRepository.findById(parseCompanyId(auth.companyId())).orElse(null);
    request.setCompanyId(auth.companyId());
    request.setCompanyName(firstNonBlank(auth.companyName(), company == null ? null : company.getCompanyName()));
    if (!request.isDirectCustomer()) {
      request.setLeadName(firstNonBlank(auth.userName(), auth.email()));
      request.setEmail(firstNonBlank(auth.email()));
      if (isBlank(request.getPhone()) && company != null) {
        request.setPhone(company.getPhone());
      }
      request.setContactUserId(auth.userId());
    }
    request.setUserId(firstNonBlank(auth.email(), auth.userId()));
    request.setOwnerUserId(auth.userId());
    request.setOwnerUserName(firstNonBlank(auth.userName(), auth.email()));
  }

  private void applyParentTransactionParty(ParkQuoteRequest request, AuthSession auth) {
    Company company = resolveParentSelectedCompany(request, auth);
    request.setCompanyId(String.valueOf(company.getCompanyId()));
    request.setCompanyName(company.getCompanyName());

    if (hasText(request.getContactUserId())) {
      AppUser contact =
          userRepository
              .findById(request.getContactUserId().trim())
              .orElseThrow(() -> new IllegalArgumentException("Contact person not found"));
      if (contact.getCompanyId() == null || !contact.getCompanyId().equals(company.getCompanyId())) {
        throw new IllegalArgumentException("Contact person must belong to the selected company");
      }
      if (AppUser.STATUS_DISABLED.equals(contact.getStatus())) {
        throw new IllegalArgumentException("Contact person is disabled");
      }
      request.setLeadName(firstNonBlank(contact.getUserName(), contact.getEmail()));
      request.setEmail(contact.getEmail());
      if (isBlank(request.getPhone())) {
        request.setPhone(company.getPhone());
      }
    }

    if (hasText(request.getOwnerUserId())) {
      AppUser owner =
          userRepository
              .findById(request.getOwnerUserId().trim())
              .orElseThrow(() -> new IllegalArgumentException("Internal responsible person not found"));
      Company ownerCompany =
          owner.getCompanyId() == null ? null : companyRepository.findById(owner.getCompanyId()).orElse(null);
      if (ownerCompany == null || !ownerCompany.parentCompany()) {
        throw new IllegalArgumentException("Internal responsible person must belong to the parent company");
      }
      if (AppUser.STATUS_DISABLED.equals(owner.getStatus())) {
        throw new IllegalArgumentException("Internal responsible person is disabled");
      }
      request.setUserId(firstNonBlank(owner.getEmail(), owner.getUserId()));
      request.setOwnerUserName(firstNonBlank(owner.getUserName(), owner.getEmail()));
    } else {
      request.setUserId(firstNonBlank(request.getUserId(), auth.email(), auth.userId()));
      request.setOwnerUserName(firstNonBlank(request.getOwnerUserName(), auth.userName(), auth.email()));
    }
  }

  /**
   * The company whose credit a booking uses: the actor's own company, or, for a parent-company
   * user, the child company it books for. Child-company users can only use their own credit.
   */
  private Optional<Company> creditCompany(AuthSession actor, String companyId) {
    Long ownId = tryParseCompanyId(actor.companyId());
    Long targetId = hasText(companyId) ? tryParseCompanyId(companyId) : ownId;
    if (targetId == null) {
      return Optional.empty();
    }
    if (!targetId.equals(ownId)) {
      if (!actor.parentCompany()) {
        throw new UnauthorizedException("Company access denied");
      }
      Optional<Company> child = companyRepository.findById(targetId);
      if (child.isPresent() && child.get().parentCompany()) {
        return companyRepository.findById(ownId);
      }
      return child;
    }
    return companyRepository.findById(targetId);
  }

  private Company resolveParentSelectedCompany(ParkQuoteRequest request, AuthSession auth) {
    Company actorCompany =
        companyRepository
            .findById(parseCompanyId(auth.companyId()))
            .orElseThrow(() -> new IllegalArgumentException("Company not found"));
    if (!hasText(request.getCompanyId())) {
      return actorCompany;
    }
    Long companyId = tryParseCompanyId(request.getCompanyId());
    if (companyId == null) {
      throw new IllegalArgumentException("Select a child company");
    }
    Company company =
        companyRepository
            .findById(companyId)
            .orElseThrow(() -> new IllegalArgumentException("Company not found"));
    if (company.parentCompany()) {
      return actorCompany;
    }
    if (!Company.STATUS_ACTIVE.equals(company.getStatus())) {
      throw new IllegalStateException(company.getCompanyName() + " is not approved yet");
    }
    return company;
  }

  private List<BookingPartyUser> partyUsers(Long companyId) {
    if (companyId == null) {
      return List.of();
    }
    return userRepository.findByCompanyIdOrderByCreatedDateAsc(companyId).stream()
        .filter(user -> !AppUser.STATUS_DISABLED.equals(user.getStatus()))
        .map(user -> new BookingPartyUser(user.getUserId(), user.getUserName(), user.getEmail()))
        .toList();
  }

  private static Long tryParseCompanyId(String value) {
    if (value == null || value.isBlank()) {
      return null;
    }
    try {
      return Long.parseLong(value.trim());
    } catch (NumberFormatException ex) {
      return null;
    }
  }

  private static boolean hasText(String value) {
    return value != null && !value.isBlank();
  }

  private static boolean isBlank(String value) {
    return value == null || value.isBlank();
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

  private static String newId(String prefix) {
    return (prefix + UUID.randomUUID().toString().replace("-", "")).substring(0, 12).toUpperCase(Locale.ROOT);
  }

  private static String normalizeEmail(String email) {
    return required(email, "Email").toLowerCase(Locale.ROOT);
  }

  private static String required(String value, String label) {
    if (value == null || value.isBlank()) {
      throw new IllegalArgumentException(label + " is required");
    }
    return value.trim();
  }

  private static Long parseCompanyId(String value) {
    String normalized = required(value, "Company ID");
    try {
      return Long.parseLong(normalized);
    } catch (NumberFormatException ex) {
      throw new IllegalArgumentException("Company ID must be a number");
    }
  }

  private static String blankToEmpty(String value) {
    return value == null ? "" : value.trim();
  }

  private static void applyDocument(Company company, String kind, AccountDocument document) {
    if (document == null || document.bytes() == null || document.bytes().length == 0) {
      return;
    }
    String fileName = blankToEmpty(document.fileName());
    String contentType = blankToEmpty(document.contentType());
    if ("vat".equals(kind)) {
      company.setVatFileName(fileName.isBlank() ? "vat-certificate" : fileName);
      company.setVatContentType(contentType);
      company.setVatBytes(document.bytes());
      return;
    }
    company.setLicenceFileName(fileName.isBlank() ? "company-licence" : fileName);
    company.setLicenceContentType(contentType);
    company.setLicenceBytes(document.bytes());
  }

  private static String fileName(String value, String fallback) {
    return value == null || value.isBlank() ? fallback : value;
  }

  private void saveSubmittedRegistration(Company company) {
    Instant now = Instant.now();
    CompanyRegistration registration =
        registrationRepository
            .findByOwnerEmailIgnoreCase(company.getEmail())
            .or(() -> registrationRepository.findByCompanyId(company.getCompanyId()))
            .orElseGet(CompanyRegistration::new);
    if (registration.getCreatedDate() == null) {
      registration.setCreatedDate(now);
    } else if (registration.getId() != null) {
      rowHistoryService.archive(
          EditableTable.COMPANY_REGISTRATION,
          registration.getId(),
          ChangeHistoryService.COMPANY_REVIEW,
          "company_registration");
    }
    registration.setCompanyId(company.getCompanyId());
    registration.setCompanyName(blankToEmpty(company.getCompanyName()));
    registration.setRegistrationNo(blankToEmpty(company.getRegistrationNo()));
    registration.setAddress(blankToEmpty(company.getAddress()));
    registration.setWebsite(blankToEmpty(company.getWebsite()));
    registration.setCountry(blankToEmpty(company.getCountry()));
    registration.setCity(blankToEmpty(company.getCity()));
    registration.setZipCode(blankToEmpty(company.getZipCode()));
    registration.setOwnerName(blankToEmpty(company.getOwnerName()));
    registration.setOwnerMobile(blankToEmpty(company.getPhone()));
    registration.setOwnerTelephone(blankToEmpty(company.getOwnerTelephone()));
    registration.setOwnerEmail(blankToEmpty(company.getEmail()));
    registration.setOperationsStaffName(blankToEmpty(company.getOperationsStaffName()));
    registration.setOperationsStaffNumber(blankToEmpty(company.getOperationsStaffNumber()));
    registration.setFinanceName(blankToEmpty(company.getFinanceName()));
    registration.setFinanceMobile(blankToEmpty(company.getFinanceMobile()));
    registration.setFinanceTelephone(blankToEmpty(company.getFinanceTelephone()));
    registration.setFinanceEmail(blankToEmpty(company.getFinanceEmail()));
    registration.setVatFileName(company.getVatFileName());
    registration.setVatContentType(company.getVatContentType());
    registration.setVatBytes(company.getVatBytes());
    registration.setLicenceFileName(company.getLicenceFileName());
    registration.setLicenceContentType(company.getLicenceContentType());
    registration.setLicenceBytes(company.getLicenceBytes());
    registration.setOtpVerified(true);
    registration.setStatus(CompanyRegistration.STATUS_SUBMITTED);
    registration.setSubmittedDate(now);
    registrationRepository.save(registration);
  }
}
