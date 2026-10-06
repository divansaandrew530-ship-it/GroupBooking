package com.ruzinn.serviceimpl;

import com.ruzinn.dto.CatalogCountry;
import com.ruzinn.dto.Experience;
import com.ruzinn.dto.ExperienceCatalog;
import com.ruzinn.dto.ItemCountry;
import com.ruzinn.dto.RoomRate;
import com.ruzinn.entity.ItemMaster;
import com.ruzinn.repository.ItemMasterRepository;
import com.ruzinn.service.ParkCatalogService;
import jakarta.annotation.PostConstruct;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import tools.jackson.databind.ObjectMapper;

@Service
public class ParkCatalogServiceImpl implements ParkCatalogService {

  private static final Logger log = LoggerFactory.getLogger(ParkCatalogServiceImpl.class);

  private final ObjectMapper objectMapper;
  private final ItemMasterRepository itemMasterRepository;
  /**
   * When false, only items that exist in Item Master are offered: price-list entries without a
   * matching Item Master row (by id or name) are hidden. Set by ruzinn.catalog.include-price-list.
   */
  private final boolean includePriceList;
  private ExperienceCatalog catalog;
  private Map<String, Experience> byId;
  /** The price-list experiences from {@code experiences.json}; Item Master additions are appended per read. */
  private List<Experience> baseExperiences;
  /** The price-list extras from {@code experiences.json}. */
  private List<Experience> baseExtras;

  public ParkCatalogServiceImpl(
      ObjectMapper objectMapper,
      ItemMasterRepository itemMasterRepository,
      @Value("${ruzinn.catalog.include-price-list:true}") boolean includePriceList) {
    this.objectMapper = objectMapper;
    this.itemMasterRepository = itemMasterRepository;
    this.includePriceList = includePriceList;
  }

  @PostConstruct
  void load() throws IOException {
    ClassPathResource resource = new ClassPathResource("data/experiences.json");
    try (InputStream in = resource.getInputStream()) {
      catalog = objectMapper.readValue(in, ExperienceCatalog.class);
    }
    baseExperiences = catalog.getExperiences() == null ? List.of() : List.copyOf(catalog.getExperiences());
    baseExtras = catalog.getExtras() == null ? null : List.copyOf(catalog.getExtras());
    applyItemCountries();
    log.info(
        "Catalog loaded: {} price-list experience(s) ({}), {} offered now, {} extra(s)",
        baseExperiences.size(),
        includePriceList ? "all offered" : "only those in Item Master offered",
        catalog.getExperiences().size(),
        catalog.getExtras() == null ? 0 : catalog.getExtras().size());
  }

  @Override
  public ExperienceCatalog getCatalog() {
    applyItemCountries();
    return catalog;
  }

  @Override
  public List<Experience> experiences() {
    return getCatalog().getExperiences();
  }

  @Override
  public List<Experience> extras() {
    return getCatalog().getExtras();
  }

  @Override
  public Experience require(String id) {
    applyItemCountries();
    return find(id).orElseThrow(() -> new IllegalArgumentException("Unknown experience: " + id));
  }

  @Override
  public Optional<Experience> find(String id) {
    applyItemCountries();
    return Optional.ofNullable(byId.get(id));
  }

  /**
   * Syncs the catalog with Item Master: price-list items take their country from the matching
   * master row, and master items with no price-list entry are added so newly created items show
   * up in the booking screen (and removed ones disappear).
   */
  private synchronized void applyItemCountries() {
    List<ItemMaster> items = itemMasterRepository.findAll();
    Map<String, String> byName = new LinkedHashMap<>();
    for (ItemMaster item : items) {
      if (item.getItemName() == null || item.getItemName().isBlank()) {
        continue;
      }
      String country = item.getCountry();
      if (country != null && !country.isBlank()) {
        byName.put(nameKey(item.getItemName()), ItemCountry.normalize(country));
      }
    }
    java.util.Set<String> inMaster = new java.util.HashSet<>();
    for (ItemMaster item : items) {
      if (item.getItemName() != null && !item.getItemName().isBlank()) {
        inMaster.add(item.getItemId());
        inMaster.add(nameKey(item.getItemName()));
      }
    }
    List<Experience> priceList = offered(baseExperiences, inMaster);
    catalog.setExtras(baseExtras == null ? null : offered(baseExtras, inMaster));
    List<Experience> experiences = new ArrayList<>(priceList);
    java.util.Set<String> known = new java.util.HashSet<>();
    for (Experience experience : priceList) {
      known.add(experience.getId());
      known.add(nameKey(experience.getName()));
    }
    if (catalog.getExtras() != null) {
      for (Experience extra : catalog.getExtras()) {
        known.add(extra.getId());
        known.add(nameKey(extra.getName()));
      }
    }
    for (ItemMaster item : items) {
      if (item.getItemName() == null || item.getItemName().isBlank()) {
        continue;
      }
      if (known.contains(item.getItemId()) || known.contains(nameKey(item.getItemName()))) {
        continue;
      }
      experiences.add(fromItemMaster(item));
      known.add(item.getItemId());
      known.add(nameKey(item.getItemName()));
      log.debug("Catalog: added Item Master item {} '{}'", item.getItemId(), item.getItemName());
    }
    catalog.setExperiences(experiences);
    applyList(catalog.getExperiences(), byName);
    applyList(catalog.getExtras(), byName);
    Map<String, Experience> index = new LinkedHashMap<>();
    index(index, catalog.getExperiences());
    index(index, catalog.getExtras());
    byId = index;
    catalog.setCountries(summarizeCountries());
  }

  /** The price-list entries to offer: all of them, or only those with an Item Master row. */
  private List<Experience> offered(List<Experience> priceList, java.util.Set<String> inMaster) {
    if (includePriceList) {
      return new ArrayList<>(priceList);
    }
    List<Experience> offered = new ArrayList<>();
    for (Experience experience : priceList) {
      if (inMaster.contains(experience.getId()) || inMaster.contains(nameKey(experience.getName()))) {
        offered.add(experience);
      }
    }
    return offered;
  }

  /** A bookable experience for an Item Master row that is not in the price list. */
  private static Experience fromItemMaster(ItemMaster item) {
    Experience experience = new Experience();
    experience.setId(item.getItemId());
    experience.setName(item.getItemName().trim());
    experience.setCategory(item.getCategory() == null ? "" : item.getCategory());
    experience.setCountry(item.getCountry());
    experience.setCity(item.getCountry());
    experience.setDated(true);
    experience.setDatedType(
        ItemMaster.DATED_TYPE_DT.equalsIgnoreCase(item.getDatedType())
            ? ItemMaster.DATED_TYPE_DT
            : ItemMaster.DATED_TYPE_DA);
    experience.setApiTimeSlot(item.isApiTimeslot());
    experience.setApiBooking(item.isApiBooking());
    experience.setAdultPrice(item.getAdultRate() == null ? null : item.getAdultRate().doubleValue());
    experience.setChildPrice(item.getChildRate() == null ? null : item.getChildRate().doubleValue());
    experience.setPriceOnRequest(item.getAdultRate() == null);
    boolean hotel = isHotelCategory(item.getCategory());
    experience.setKind(
        hotel ? "stay" : "transfer".equalsIgnoreCase(item.getCategory()) ? "transfer" : "experience");
    experience.setBookingInfo(item.getBookingInfo());
    List<String> fields = new ArrayList<>();
    for (String field :
        List.of(
            item.getField1(), item.getField2(), item.getField3(), item.getField4(), item.getField5(),
            item.getField6(), item.getField7(), item.getField8(), item.getField9(), item.getField10(),
            item.getField11(), item.getField12(), item.getField13(), item.getField14(), item.getField15())) {
      if (field != null && !field.isBlank()) {
        fields.add(field.trim());
      }
    }
    experience.setPassengerFields(fields);
    return experience;
  }

  /** A hotel with no room types offers one Standard room at its adult rate, so it stays bookable. */
  private static void defaultRoomRate(Experience experience) {
    boolean hotel = "stay".equals(experience.getKind()) || isHotelCategory(experience.getCategory());
    boolean noRooms = experience.getRoomRates() == null || experience.getRoomRates().isEmpty();
    if (hotel && noRooms && experience.getAdultPrice() != null) {
      experience.setRoomRates(List.of(new RoomRate("Standard", experience.getAdultPrice())));
    }
  }

  private static boolean isHotelCategory(String category) {
    return "hotels".equalsIgnoreCase(category) || "hotel".equalsIgnoreCase(category);
  }

  private void applyList(List<Experience> items, Map<String, String> byName) {
    if (items == null) {
      return;
    }
    for (Experience experience : items) {
      defaultRoomRate(experience);
      String fromMaster = byName.get(nameKey(experience.getName()));
      if (fromMaster != null) {
        experience.setCountry(fromMaster);
      } else if (experience.getCountry() == null || experience.getCountry().isBlank()) {
        experience.setCountry(fallbackCountry(experience.getCity()));
      } else {
        experience.setCountry(ItemCountry.normalize(experience.getCountry()));
      }
    }
  }

  private List<CatalogCountry> summarizeCountries() {
    Map<String, Integer> counts = new LinkedHashMap<>();
    count(catalog.getExperiences(), counts);
    count(catalog.getExtras(), counts);
    List<CatalogCountry> countries = new ArrayList<>();
    for (ItemCountry option : ItemCountry.OPTIONS) {
      int total = counts.getOrDefault(option.code(), 0);
      if (total > 0) {
        countries.add(CatalogCountry.from(option, total));
      }
    }
    return countries;
  }

  private static void count(List<Experience> items, Map<String, Integer> counts) {
    if (items == null) {
      return;
    }
    for (Experience experience : items) {
      String country = experience.getCountry() == null ? ItemCountry.UAE : experience.getCountry();
      counts.merge(country, 1, Integer::sum);
    }
  }

  private static String fallbackCountry(String city) {
    if (city == null || city.isBlank()) {
      return ItemCountry.UAE;
    }
    try {
      return ItemCountry.normalize(city);
    } catch (IllegalArgumentException ex) {
      return ItemCountry.UAE;
    }
  }

  private static String nameKey(String name) {
    if (name == null) {
      return "";
    }
    return name.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]+", " ").trim();
  }

  private static void index(Map<String, Experience> index, List<Experience> items) {
    if (items == null) {
      return;
    }
    for (Experience experience : items) {
      index.put(experience.getId(), experience);
    }
  }
}
