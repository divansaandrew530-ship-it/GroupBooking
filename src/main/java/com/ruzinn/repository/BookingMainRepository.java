package com.ruzinn.repository;

import com.ruzinn.entity.BookingMain;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BookingMainRepository extends JpaRepository<BookingMain, String> {

  List<BookingMain> findByCompanyIdOrderByCreatedDateDesc(String companyId);

  List<BookingMain> findAllByOrderByCreatedDateDesc();
}
