package com.ruzinn.parks;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.ruzinn.dto.Experience;
import com.ruzinn.entity.ItemMaster;
import com.ruzinn.repository.ItemMasterRepository;
import com.ruzinn.serviceimpl.ParkCatalogServiceImpl;
import java.lang.reflect.Method;
import java.util.List;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;

class ParkCatalogItemMasterOnlyTest {

  @Test
  void withoutPriceListOnlyItemMasterItemsAreOffered() throws Exception {
    ItemMasterRepository repository = mock(ItemMasterRepository.class);
    when(repository.findAll())
        .thenReturn(List.of(item("aya-universe-dubai", "AYA Universe (Dubai)"), item("ITM-NEW", "Desert Safari Deluxe")));

    ParkCatalogServiceImpl service = load(repository, false);

    List<String> ids = service.getCatalog().getExperiences().stream().map(Experience::getId).toList();
    assertThat(ids).containsExactlyInAnyOrder("aya-universe-dubai", "ITM-NEW");
    assertThat(service.getCatalog().getExtras()).isEmpty();
    assertThat(service.find("transfer-half-day")).isEmpty();
  }

  @Test
  void withPriceListAllBundledItemsAreOffered() throws Exception {
    ItemMasterRepository repository = mock(ItemMasterRepository.class);
    when(repository.findAll()).thenReturn(List.of(item("ITM-NEW", "Desert Safari Deluxe")));

    ParkCatalogServiceImpl service = load(repository, true);

    assertThat(service.getCatalog().getExperiences()).hasSizeGreaterThan(2);
    assertThat(service.find("transfer-half-day")).isPresent();
    assertThat(service.find("ITM-NEW")).isPresent();
  }

  private static ParkCatalogServiceImpl load(ItemMasterRepository repository, boolean includePriceList)
      throws Exception {
    ParkCatalogServiceImpl service =
        new ParkCatalogServiceImpl(JsonMapper.builder().build(), repository, includePriceList);
    Method load = ParkCatalogServiceImpl.class.getDeclaredMethod("load");
    load.setAccessible(true);
    load.invoke(service);
    return service;
  }

  private static ItemMaster item(String id, String name) {
    ItemMaster item = new ItemMaster();
    item.setItemId(id);
    item.setItemName(name);
    item.setCountry("UAE");
    return item;
  }
}
