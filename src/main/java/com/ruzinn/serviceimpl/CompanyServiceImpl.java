package com.ruzinn.serviceimpl;

import com.ruzinn.dto.AccountDocument;
import com.ruzinn.dto.AuthSession;
import com.ruzinn.dto.CompanyView;
import com.ruzinn.dto.CompanyWithUsers;
import com.ruzinn.entity.AppUser;
import com.ruzinn.entity.Company;
import com.ruzinn.enums.EditableTable;
import com.ruzinn.exception.UnauthorizedException;
import com.ruzinn.repository.AppUserRepository;
import com.ruzinn.repository.CompanyRepository;
import com.ruzinn.service.AccountService;
import com.ruzinn.service.ChangeHistoryService;
import com.ruzinn.service.CompanyLogoStore;
import com.ruzinn.service.CompanyService;
import com.ruzinn.service.RowHistoryService;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
public class CompanyServiceImpl implements CompanyService {

  private static final Logger log = LoggerFactory.getLogger(CompanyServiceImpl.class);

  private final CompanyRepository companyRepository;
  private final AppUserRepository userRepository;
  private final AccountService accountService;
  private final ChangeHistoryService changeHistoryService;
  private final RowHistoryService rowHistoryService;
  private final CompanyLogoStore companyLogoStore;

  public CompanyServiceImpl(
      CompanyRepository companyRepository,
      AppUserRepository userRepository,
      AccountService accountService,
      ChangeHistoryService changeHistoryService,
      RowHistoryService rowHistoryService,
      CompanyLogoStore companyLogoStore) {
    this.companyRepository = companyRepository;
    this.userRepository = userRepository;
    this.accountService = accountService;
    this.changeHistoryService = changeHistoryService;
    this.rowHistoryService = rowHistoryService;
    this.companyLogoStore = companyLogoStore;
  }

  // ---- list -------------------------------------------------------------------------------------

  @Override
  @Transactional(readOnly = true)
  public List<CompanyView> listCompanies(AuthSession actor) {
    accountService.requireSuperAdmin(actor);
    List<CompanyView> companies =
        companyRepository.findAllByOrderByCreatedDateDesc().stream()
            // Registrations whose email OTP was never verified are not shown until they are.
            .filter(Company::isEmailVerified)
            .map(accountService::toCompanyView)
            .toList();
    log.debug("Listed {} companies for {}", companies.size(), actor.email());
    return companies;
  }

  @Override
  @Transactional(readOnly = true)
  public List<CompanyWithUsers> listCompaniesWithUsers(AuthSession actor) {
    // User rows are built by AccountService, which owns the user view.
    return accountService.listCompaniesWithUsers(actor);
  }

  // ---- approve / reject -------------------------------------------------------------------------

  @Override
  @Transactional
  public CompanyView approve(AuthSession actor, String companyId, String note, BigDecimal creditLimit) {
    accountService.requireSuperAdmin(actor);
    log.info("Approve company {} requested by {} (creditLimit={})", companyId, actor.email(), creditLimit);

    Company company = findCompany(companyId);
    requireEmailVerified(company);

    String oldStatus = company.getStatus();
    BigDecimal oldLimit = company.getCreditLimit();
    String oldNote = company.getReviewNote();
    rowHistoryService.archive(
        EditableTable.COMPANY, company.getCompanyId(), ChangeHistoryService.COMPANY_REVIEW, "status,credit_limit,review_note");

    company.setStatus(Company.STATUS_ACTIVE);
    company.setReviewedDate(Instant.now());
    company.setReviewedBy(actor.userId());
    company.setReviewNote(blankToEmpty(note));
    if (creditLimit != null && creditLimit.compareTo(BigDecimal.ZERO) >= 0) {
      company.setCreditLimit(creditLimit.setScale(2, RoundingMode.HALF_UP));
    }

    int activated = 0;
    for (AppUser user : usersOf(company)) {
      if (user.isEmailVerified()) {
        changeUserStatus(actor, company, user, AppUser.STATUS_ACTIVE);
        activated++;
      }
    }

    recordCompanyChange(actor, company, "status", oldStatus, company.getStatus());
    recordCompanyChange(actor, company, "credit_limit", oldLimit, company.getCreditLimit());
    recordCompanyChange(actor, company, "review_note", oldNote, company.getReviewNote());

    log.info(
        "Company {} ({}) approved by {}: status {} -> ACTIVE, credit limit {}, {} user(s) activated",
        company.getCompanyId(), company.getCompanyName(), actor.email(), oldStatus, company.getCreditLimit(), activated);
    return accountService.toCompanyView(company);
  }

  @Override
  @Transactional
  public CompanyView reject(AuthSession actor, String companyId, String note) {
    accountService.requireSuperAdmin(actor);
    log.info("Reject company {} requested by {}", companyId, actor.email());

    Company company = findCompany(companyId);
    requireEmailVerified(company);

    String oldStatus = company.getStatus();
    String oldNote = company.getReviewNote();
    rowHistoryService.archive(
        EditableTable.COMPANY, company.getCompanyId(), ChangeHistoryService.COMPANY_REVIEW, "status,credit_limit,review_note");

    company.setStatus(Company.STATUS_REJECTED);
    company.setReviewedDate(Instant.now());
    company.setReviewedBy(actor.userId());
    company.setReviewNote(blankToEmpty(note));

    int disabled = 0;
    for (AppUser user : usersOf(company)) {
      changeUserStatus(actor, company, user, AppUser.STATUS_DISABLED);
      disabled++;
    }

    recordCompanyChange(actor, company, "status", oldStatus, company.getStatus());
    recordCompanyChange(actor, company, "review_note", oldNote, company.getReviewNote());

    log.info(
        "Company {} ({}) rejected by {}: status {} -> REJECTED, {} user(s) disabled",
        company.getCompanyId(), company.getCompanyName(), actor.email(), oldStatus, disabled);
    return accountService.toCompanyView(company);
  }

  // ---- logo and documents -----------------------------------------------------------------------

  @Override
  @Transactional
  public CompanyView saveLogo(AuthSession actor, String companyId, MultipartFile logo) {
    accountService.requireSuperAdmin(actor);
    if (logo == null || logo.isEmpty()) {
      log.warn("Logo upload for company {} by {} rejected: no file", companyId, actor.email());
      throw new IllegalArgumentException("Choose a JPG or PNG company logo");
    }
    Company company = findCompany(companyId);
    String oldPath = blankToEmpty(company.getLogoPath());
    rowHistoryService.archive(
        EditableTable.COMPANY, company.getCompanyId(), ChangeHistoryService.DOCUMENT_UPLOAD, "logo_path");

    company.setLogoPath(companyLogoStore.save(company.getCompanyId(), logo));
    company.setLogoFileName(fileName(logo.getOriginalFilename(), "company-logo"));
    company.setLogoContentType(blankToEmpty(logo.getContentType()));
    companyRepository.save(company);

    changeHistoryService.record(
        ChangeHistoryService.COMPANY,
        String.valueOf(company.getCompanyId()),
        null,
        ChangeHistoryService.DOCUMENT_UPLOAD,
        "logo_path",
        oldPath,
        blankToEmpty(company.getLogoPath()),
        String.valueOf(company.getCompanyId()),
        company.getCompanyName(),
        actor);
    log.info(
        "Logo saved for company {} by {} ({}, {} bytes)",
        company.getCompanyId(), actor.email(), logo.getOriginalFilename(), logo.getSize());
    return accountService.toCompanyView(company);
  }

  @Override
  @Transactional(readOnly = true)
  public Optional<AccountDocument> findDocument(AuthSession actor, String companyId, String kind) {
    accountService.requireSuperAdmin(actor);
    Company company = findCompany(companyId);
    String type = kind == null ? "" : kind.trim().toLowerCase(Locale.ROOT);

    if (type.equals("vat") && company.hasVatCertificate()) {
      return Optional.of(
          new AccountDocument(
              "vat",
              fileName(company.getVatFileName(), "vat-certificate"),
              company.getVatContentType(),
              company.getVatBytes()));
    }
    if ((type.equals("licence") || type.equals("license")) && company.hasLicence()) {
      return Optional.of(
          new AccountDocument(
              "licence",
              fileName(company.getLicenceFileName(), "company-licence"),
              company.getLicenceContentType(),
              company.getLicenceBytes()));
    }
    log.info("No {} document for company {}", type, company.getCompanyId());
    return Optional.empty();
  }

  @Override
  @Transactional(readOnly = true)
  public Optional<AccountDocument> findLogo(AuthSession actor, String companyId) {
    accountService.requireSignedIn(actor);
    Company company = findCompany(companyId);
    boolean ownCompany = String.valueOf(company.getCompanyId()).equals(actor.companyId());
    if (!actor.parentCompany() && !ownCompany) {
      log.warn("User {} tried to read the logo of company {}", actor.email(), company.getCompanyId());
      throw new UnauthorizedException("You can only view the logo for your company");
    }
    if (!company.hasLogo() || !companyLogoStore.exists(company.getLogoPath())) {
      return Optional.empty();
    }
    return Optional.of(
        new AccountDocument(
            "logo",
            fileName(company.getLogoFileName(), "company-logo"),
            companyLogoStore.contentType(company.getLogoPath()),
            companyLogoStore.read(company.getLogoPath())));
  }

  // ---- helpers ----------------------------------------------------------------------------------

  private Company findCompany(String companyId) {
    Long id = toCompanyId(companyId);
    Company company = companyRepository.findById(id).orElse(null);
    if (company == null) {
      log.warn("Company {} not found", id);
      throw new IllegalArgumentException("Company not found");
    }
    return company;
  }

  private static Long toCompanyId(String companyId) {
    if (companyId == null || companyId.isBlank()) {
      throw new IllegalArgumentException("Company ID is required");
    }
    try {
      return Long.valueOf(companyId.trim());
    } catch (NumberFormatException ex) {
      log.warn("Invalid company ID '{}'", companyId);
      throw new IllegalArgumentException("Company ID must be a number");
    }
  }

  /** A company can be reviewed only after its registration email was verified with the OTP. */
  private static void requireEmailVerified(Company company) {
    if (!company.isEmailVerified()) {
      log.warn(
          "Company {} ({}) cannot be reviewed: email {} is not verified",
          company.getCompanyId(), company.getCompanyName(), company.getEmail());
      throw new IllegalStateException("Company email must be verified before review");
    }
  }

  private List<AppUser> usersOf(Company company) {
    return userRepository.findByCompanyIdOrderByCreatedDateAsc(company.getCompanyId());
  }

  private void changeUserStatus(AuthSession actor, Company company, AppUser user, String newStatus) {
    String oldStatus = user.getStatus();
    if (!newStatus.equals(oldStatus)) {
      rowHistoryService.archive(EditableTable.APP_USER, user.getUserId(), ChangeHistoryService.COMPANY_REVIEW, "status");
    }
    user.setStatus(newStatus);
    changeHistoryService.record(
        ChangeHistoryService.APP_USER,
        user.getUserId(),
        null,
        ChangeHistoryService.COMPANY_REVIEW,
        "status",
        oldStatus,
        newStatus,
        String.valueOf(company.getCompanyId()),
        company.getCompanyName(),
        actor);
    log.debug("User {} status {} -> {}", user.getEmail(), oldStatus, newStatus);
  }

  private void recordCompanyChange(AuthSession actor, Company company, String field, Object oldValue, Object newValue) {
    changeHistoryService.record(
        ChangeHistoryService.COMPANY,
        String.valueOf(company.getCompanyId()),
        null,
        ChangeHistoryService.COMPANY_REVIEW,
        field,
        oldValue,
        newValue,
        String.valueOf(company.getCompanyId()),
        company.getCompanyName(),
        actor);
  }

  private static String blankToEmpty(String value) {
    return value == null ? "" : value.trim();
  }

  private static String fileName(String value, String fallback) {
    return value == null || value.isBlank() ? fallback : value;
  }
}
