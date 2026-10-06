package com.ruzinn.repository;

import com.ruzinn.entity.Company;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CompanyRepository extends JpaRepository<Company, Long> {

  Optional<Company> findByEmailIgnoreCase(String email);

  List<Company> findAllByOrderByCreatedDateDesc();

  List<Company> findByStatusOrderByCreatedDateDesc(String status);
}
