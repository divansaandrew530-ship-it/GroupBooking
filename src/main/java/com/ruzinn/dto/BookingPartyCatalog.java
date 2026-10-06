package com.ruzinn.dto;

import java.util.List;

public record BookingPartyCatalog(
    boolean parentCompany,
    String companyId,
    String companyName,
    String contactName,
    String contactEmail,
    List<BookingPartyCompany> companies,
    List<BookingPartyUser> owners) {

  public static BookingPartyCatalog forChild(AuthSession auth) {
    return new BookingPartyCatalog(
        false,
        auth == null ? "" : auth.companyId(),
        auth == null ? "" : auth.companyName(),
        auth == null ? "" : auth.userName(),
        auth == null ? "" : auth.email(),
        List.of(),
        List.of());
  }
}
