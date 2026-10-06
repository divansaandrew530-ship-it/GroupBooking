package com.ruzinn.repository;

import com.ruzinn.entity.CompanyRegistration;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CompanyRegistrationRepository extends JpaRepository<CompanyRegistration, Long> {

  Optional<CompanyRegistration> findByOwnerEmailIgnoreCase(String ownerEmail);

  Optional<CompanyRegistration> findByCompanyId(Long companyId);

  boolean existsByOwnerEmailIgnoreCase(String ownerEmail);
}
