package com.ruzinn.repository;

import com.ruzinn.entity.AppUser;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AppUserRepository extends JpaRepository<AppUser, String> {

  Optional<AppUser> findByEmailIgnoreCase(String email);

  List<AppUser> findByCompanyIdOrderByCreatedDateAsc(Long companyId);

  boolean existsByEmailIgnoreCase(String email);
}
