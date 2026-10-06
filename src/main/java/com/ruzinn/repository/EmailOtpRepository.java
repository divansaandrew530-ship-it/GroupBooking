package com.ruzinn.repository;

import com.ruzinn.entity.EmailOtp;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EmailOtpRepository extends JpaRepository<EmailOtp, Long> {

  List<EmailOtp> findByEmailIgnoreCaseAndPurposeAndConsumedAtIsNullOrderByCreatedDateDesc(
      String email, String purpose);

  Optional<EmailOtp> findFirstByEmailIgnoreCaseAndPurposeAndConsumedAtIsNullOrderByCreatedDateDesc(
      String email, String purpose);
}
