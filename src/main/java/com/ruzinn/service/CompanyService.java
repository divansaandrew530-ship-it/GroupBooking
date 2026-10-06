package com.ruzinn.service;

import com.ruzinn.dto.AccountDocument;
import com.ruzinn.dto.AuthSession;
import com.ruzinn.dto.CompanyView;
import com.ruzinn.dto.CompanyWithUsers;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import org.springframework.web.multipart.MultipartFile;

/** Super Admin company management: list, approve/reject, logo and registration documents. */
public interface CompanyService {

  List<CompanyView> listCompanies(AuthSession actor);

  /** Every company together with its users. */
  List<CompanyWithUsers> listCompaniesWithUsers(AuthSession actor);

  /** Sets the company ACTIVE (with an optional credit limit) and activates its verified users. */
  CompanyView approve(AuthSession actor, String companyId, String note, BigDecimal creditLimit);

  /** Sets the company REJECTED and disables all its users. */
  CompanyView reject(AuthSession actor, String companyId, String note);

  CompanyView saveLogo(AuthSession actor, String companyId, MultipartFile logo);

  /** Registration document; {@code kind} is {@code vat} or {@code licence}. */
  Optional<AccountDocument> findDocument(AuthSession actor, String companyId, String kind);

  Optional<AccountDocument> findLogo(AuthSession actor, String companyId);
}
