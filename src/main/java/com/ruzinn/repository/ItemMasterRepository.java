package com.ruzinn.repository;

import com.ruzinn.entity.ItemMaster;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ItemMasterRepository extends JpaRepository<ItemMaster, String> {

  Optional<ItemMaster> findByItemNameIgnoreCase(String itemName);

  boolean existsByItemNameIgnoreCase(String itemName);

  List<ItemMaster> findAllByOrderByItemNameAsc();

  @Query(
      """
      select i from ItemMaster i
      where lower(i.itemName) like lower(concat('%', :query, '%'))
         or lower(coalesce(i.category, '')) like lower(concat('%', :query, '%'))
         or lower(coalesce(i.country, '')) like lower(concat('%', :query, '%'))
      order by i.itemName
      """)
  List<ItemMaster> search(@Param("query") String query);
}
