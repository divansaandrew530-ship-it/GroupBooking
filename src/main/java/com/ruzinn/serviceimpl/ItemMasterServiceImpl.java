package com.ruzinn.serviceimpl;

import com.ruzinn.dto.AuthSession;
import com.ruzinn.dto.ChangeHistoryView;
import com.ruzinn.dto.ItemImportResult;
import com.ruzinn.dto.ItemMasterForm;
import com.ruzinn.dto.ItemMasterView;
import com.ruzinn.entity.ItemMaster;
import com.ruzinn.enums.EditableTable;
import com.ruzinn.repository.ItemMasterRepository;
import com.ruzinn.service.AccountService;
import com.ruzinn.service.ChangeHistoryService;
import com.ruzinn.service.ItemMasterExcelService;
import com.ruzinn.service.ItemMasterMediaStore;
import com.ruzinn.service.ItemMasterService;
import com.ruzinn.service.RowHistoryService;
import java.io.InputStream;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ItemMasterServiceImpl implements ItemMasterService {

  private static final Logger log = LoggerFactory.getLogger(ItemMasterServiceImpl.class);

  private final ItemMasterRepository items;
  private final ItemMasterExcelService excelService;
  private final ItemMasterMediaStore mediaStore;
  private final AccountService accountService;
  private final ChangeHistoryService changeHistoryService;
  private final RowHistoryService rowHistoryService;

  public ItemMasterServiceImpl(
      ItemMasterRepository items,
      ItemMasterExcelService excelService,
      ItemMasterMediaStore mediaStore,
      AccountService accountService,
      ChangeHistoryService changeHistoryService,
      RowHistoryService rowHistoryService) {
    this.items = items;
    this.excelService = excelService;
    this.mediaStore = mediaStore;
    this.accountService = accountService;
    this.changeHistoryService = changeHistoryService;
    this.rowHistoryService = rowHistoryService;
  }

  @Override
  @Transactional(readOnly = true)
  public List<ItemMasterView> list(AuthSession actor, String query) {
    accountService.requireSuperAdmin(actor);
    List<ItemMaster> rows =
        query == null || query.isBlank()
            ? items.findAllByOrderByItemNameAsc()
            : items.search(query.trim());
    return rows.stream().map(ItemMasterView::from).toList();
  }

  @Override
  @Transactional(readOnly = true)
  public ItemMasterView get(AuthSession actor, String itemId) {
    return ItemMasterView.from(requireItem(actor, itemId));
  }

  @Override
  @Transactional(readOnly = true)
  public ItemMasterForm form(AuthSession actor, String itemId) {
    if (itemId == null || itemId.isBlank()) {
      accountService.requireSuperAdmin(actor);
      return ItemMasterForm.blank();
    }
    return ItemMasterForm.from(requireItem(actor, itemId));
  }

  @Override
  @Transactional(readOnly = true)
  public List<ChangeHistoryView> history(AuthSession actor, String itemId) {
    requireItem(actor, itemId);
    return changeHistoryService.listForEntity(ChangeHistoryService.ITEM_MASTER, itemId);
  }

  @Override
  @Transactional
  public ItemMasterView save(AuthSession actor, ItemMasterForm form) {
    return save(actor, form, null);
  }

  @Override
  @Transactional
  public ItemMasterView save(AuthSession actor, ItemMasterForm form, String forcedAction) {
    accountService.requireSuperAdmin(actor);
    boolean creating = form.getItemId() == null || form.getItemId().isBlank();
    ItemMaster item =
        creating
            ? newItem(actor)
            : requireItem(actor, form.getItemId());
    String name = required(form.getItemName(), "Experience / Ticket");
    items
        .findByItemNameIgnoreCase(name)
        .filter(existing -> creating || !existing.getItemId().equals(item.getItemId()))
        .ifPresent(
            existing -> {
              throw new IllegalArgumentException("An item already exists for " + name);
            });
    Snapshot before = Snapshot.from(item);
    apply(item, form, name);
    applyImages(item, form);
    Instant now = Instant.now();
    item.setUpdatedDate(now);
    item.setUpdatedBy(actor.userId());
    String action =
        forcedAction == null || forcedAction.isBlank()
            ? (creating ? ChangeHistoryService.CREATE : ChangeHistoryService.ITEM_SAVE)
            : forcedAction;
    String fields = creating ? "item_name" : changedFields(before, item);
    if (!creating) {
      rowHistoryService.archive(EditableTable.ITEM_MASTER, item.getItemId(), action, fields.isBlank() ? "item_name" : fields);
    }
    items.saveAndFlush(item);
    if (creating) {
      rowHistoryService.archive(EditableTable.ITEM_MASTER, item.getItemId(), action, fields);
    }
    recordChanges(actor, item, before, action);
    log.info(
        "Item {} '{}' {} by {} ({}); changed: {}",
        item.getItemId(), item.getItemName(), creating ? "created" : "updated", actor.email(), action, fields);
    return ItemMasterView.from(item);
  }

  @Override
  @Transactional(readOnly = true)
  public MediaResource image(AuthSession actor, String itemId, String kind) {
    ItemMaster item = requireItem(actor, itemId);
    String stored =
        ItemMasterMediaStore.KIND_TICKET.equals(ItemMasterMediaStore.normalizeKind(kind))
            ? item.getImageTicket()
            : item.getImagePdf();
    if (!mediaStore.exists(stored)) {
      throw new IllegalArgumentException("Image not found");
    }
    return new MediaResource(mediaStore.read(stored), mediaStore.contentType(stored));
  }

  @Override
  @Transactional
  public void delete(AuthSession actor, String itemId) {
    ItemMaster item = requireItem(actor, itemId);
    rowHistoryService.archive(
        EditableTable.ITEM_MASTER, item.getItemId(), ChangeHistoryService.ITEM_DELETE, "item_name");
    changeHistoryService.record(
        ChangeHistoryService.ITEM_MASTER,
        item.getItemId(),
        null,
        ChangeHistoryService.ITEM_DELETE,
        "item_name",
        item.getItemName(),
        "",
        actor.companyId(),
        actor.companyName(),
        actor);
    items.delete(item);
    mediaStore.deleteItem(item.getItemId());
    log.info("Item {} '{}' deleted by {}", item.getItemId(), item.getItemName(), actor.email());
  }

  @Override
  public byte[] template(AuthSession actor) {
    accountService.requireSuperAdmin(actor);
    return excelService.template();
  }

  @Override
  @Transactional
  public ItemImportResult importExcel(AuthSession actor, InputStream input) {
    accountService.requireSuperAdmin(actor);
    List<ItemMasterExcelService.ParsedRow> rows = excelService.parse(input);
    int created = 0;
    int updated = 0;
    int skipped = 0;
    List<String> errors = new ArrayList<>();
    for (ItemMasterExcelService.ParsedRow row : rows) {
      try {
        ItemMasterForm form = row.form();
        String name = required(form.getItemName(), "Experience / Ticket");
        ItemMaster existing = items.findByItemNameIgnoreCase(name).orElse(null);
        if (existing == null) {
          form.setItemId("");
          save(actor, form, ChangeHistoryService.ITEM_IMPORT);
          created++;
        } else {
          form.setItemId(existing.getItemId());
          save(actor, form, ChangeHistoryService.ITEM_IMPORT);
          updated++;
        }
      } catch (RuntimeException ex) {
        skipped++;
        errors.add("Row " + row.excelRow() + ": " + ex.getMessage());
        log.warn("Item import row {} skipped: {}", row.excelRow(), ex.getMessage());
      }
    }
    log.info(
        "Item Excel import by {}: {} row(s) read, {} created, {} updated, {} skipped",
        actor.email(), rows.size(), created, updated, skipped);
    return new ItemImportResult(created, updated, skipped, List.copyOf(errors));
  }

  private ItemMaster requireItem(AuthSession actor, String itemId) {
    accountService.requireSuperAdmin(actor);
    return items
        .findById(required(itemId, "Item ID"))
        .orElseThrow(() -> new IllegalArgumentException("Item not found"));
  }

  private ItemMaster newItem(AuthSession actor) {
    Instant now = Instant.now();
    ItemMaster item = new ItemMaster();
    item.setItemId(newId());
    item.setCreatedDate(now);
    item.setCreatedBy(actor.userId());
    item.setUpdatedDate(now);
    item.setUpdatedBy(actor.userId());
    return item;
  }

  private void apply(ItemMaster item, ItemMasterForm form, String name) {
    item.setItemName(name);
    item.setCountry(com.ruzinn.dto.ItemCountry.normalize(form.getCountry()));
    item.setDatedType(normalizeDatedType(form.getDatedType()));
    item.setCategory(blank(form.getCategory()));
    item.setApiTimeslot(form.isApiTimeslot());
    item.setApiBooking(form.isApiBooking());
    item.setAdultRate(form.parsedAdultRate());
    item.setChildRate(form.parsedChildRate());
    item.setBookingInfo(normalizeBookingInfo(form.getBookingInfo()));
    item.setExtraFields(form.getExtraFields());
    item.setDescription(blank(form.getDescription()));
    item.setTermsAndCondition(blank(form.getTermsAndCondition()));
    item.setIncluded(blank(form.getIncluded()));
    item.setExcluded(blank(form.getExcluded()));
    item.setImagePdf(blank(form.getImagePdf()));
    item.setImageTicket(blank(form.getImageTicket()));
  }

  private void applyImages(ItemMaster item, ItemMasterForm form) {
    String pdfPath = mediaStore.save(item.getItemId(), ItemMasterMediaStore.KIND_PDF, form.getImagePdfFile());
    if (pdfPath != null) {
      item.setImagePdf(pdfPath);
    }
    String ticketPath =
        mediaStore.save(item.getItemId(), ItemMasterMediaStore.KIND_TICKET, form.getImageTicketFile());
    if (ticketPath != null) {
      item.setImageTicket(ticketPath);
    }
  }

  private void recordChanges(AuthSession actor, ItemMaster item, Snapshot before, String action) {
    record(actor, item, action, "item_name", before.itemName(), item.getItemName());
    record(actor, item, action, "country", before.country(), item.getCountry());
    record(actor, item, action, "dated_type", before.datedType(), item.getDatedType());
    record(actor, item, action, "category", before.category(), item.getCategory());
    record(actor, item, action, "api_timeslot", before.apiTimeslot(), item.isApiTimeslot());
    record(actor, item, action, "api_booking", before.apiBooking(), item.isApiBooking());
    record(actor, item, action, "adult_rate", before.adultRate(), item.getAdultRate());
    record(actor, item, action, "child_rate", before.childRate(), item.getChildRate());
    record(actor, item, action, "booking_info", before.bookingInfo(), item.getBookingInfo());
    record(actor, item, action, "description", before.description(), item.getDescription());
    record(actor, item, action, "terms_and_condition", before.termsAndCondition(), item.getTermsAndCondition());
    record(actor, item, action, "included", before.included(), item.getIncluded());
    record(actor, item, action, "excluded", before.excluded(), item.getExcluded());
    record(actor, item, action, "image_pdf", before.imagePdf(), item.getImagePdf());
    record(actor, item, action, "image_ticket", before.imageTicket(), item.getImageTicket());
    List<String> previousFields = before.extraFields();
    List<String> nextFields = item.extraFields();
    for (int i = 0; i < 15; i++) {
      record(actor, item, action, "field" + (i + 1), previousFields.get(i), nextFields.get(i));
    }
  }

  private void record(
      AuthSession actor, ItemMaster item, String action, String field, Object oldValue, Object newValue) {
    changeHistoryService.record(
        ChangeHistoryService.ITEM_MASTER,
        item.getItemId(),
        null,
        action,
        field,
        stringify(oldValue),
        stringify(newValue),
        actor.companyId(),
        actor.companyName(),
        actor);
  }

  private static String changedFields(Snapshot before, ItemMaster item) {
    List<String> fields = new ArrayList<>();
    if (!Objects.equals(before.itemName(), item.getItemName())) {
      fields.add("item_name");
    }
    if (!Objects.equals(before.country(), item.getCountry())) {
      fields.add("country");
    }
    if (!Objects.equals(before.datedType(), item.getDatedType())) {
      fields.add("dated_type");
    }
    if (!Objects.equals(before.category(), item.getCategory())) {
      fields.add("category");
    }
    if (before.apiTimeslot() != item.isApiTimeslot()) {
      fields.add("api_timeslot");
    }
    if (before.apiBooking() != item.isApiBooking()) {
      fields.add("api_booking");
    }
    if (!Objects.equals(stringify(before.adultRate()), stringify(item.getAdultRate()))) {
      fields.add("adult_rate");
    }
    if (!Objects.equals(stringify(before.childRate()), stringify(item.getChildRate()))) {
      fields.add("child_rate");
    }
    if (!Objects.equals(before.bookingInfo(), item.getBookingInfo())) {
      fields.add("booking_info");
    }
    if (!Objects.equals(stringify(before.description()), stringify(item.getDescription()))) {
      fields.add("description");
    }
    if (!Objects.equals(stringify(before.termsAndCondition()), stringify(item.getTermsAndCondition()))) {
      fields.add("terms_and_condition");
    }
    if (!Objects.equals(stringify(before.included()), stringify(item.getIncluded()))) {
      fields.add("included");
    }
    if (!Objects.equals(stringify(before.excluded()), stringify(item.getExcluded()))) {
      fields.add("excluded");
    }
    if (!Objects.equals(stringify(before.imagePdf()), stringify(item.getImagePdf()))) {
      fields.add("image_pdf");
    }
    if (!Objects.equals(stringify(before.imageTicket()), stringify(item.getImageTicket()))) {
      fields.add("image_ticket");
    }
    List<String> previousFields = before.extraFields();
    List<String> nextFields = item.extraFields();
    for (int i = 0; i < 15; i++) {
      if (!Objects.equals(stringify(previousFields.get(i)), stringify(nextFields.get(i)))) {
        fields.add("field" + (i + 1));
      }
    }
    return String.join(",", fields);
  }

  private static String normalizeDatedType(String value) {
    String normalized = required(value, "Dated Type").trim().toUpperCase(Locale.ROOT);
    if (ItemMaster.DATED_TYPE_DA.equals(normalized) || ItemMaster.DATED_TYPE_DT.equals(normalized)) {
      return normalized;
    }
    throw new IllegalArgumentException("Dated Type must be DA or DT");
  }

  private static String normalizeBookingInfo(String value) {
    String normalized = value == null ? "" : value.trim().toUpperCase(Locale.ROOT).replace(" ", "").replace("-", "_");
    if (normalized.isBlank() || "DFT".equals(normalized) || "DEFAULT".equals(normalized)) {
      return ItemMaster.BOOKING_INFO_DFT;
    }
    if ("ADDON".equals(normalized) || "ADD_ON".equals(normalized) || "ADDONINFO".equals(normalized)) {
      return ItemMaster.BOOKING_INFO_ADD_ON;
    }
    throw new IllegalArgumentException("Booking_info must be DFT or ADD_ON");
  }

  private static String required(String value, String label) {
    if (value == null || value.isBlank()) {
      throw new IllegalArgumentException(label + " is required");
    }
    return value.trim();
  }

  private static String blank(String value) {
    return value == null ? "" : value.trim();
  }

  private static String stringify(Object value) {
    if (value == null) {
      return "";
    }
    if (value instanceof BigDecimal amount) {
      return amount.stripTrailingZeros().toPlainString();
    }
    return value.toString().trim();
  }

  private static String newId() {
    return ("ITM" + UUID.randomUUID().toString().replace("-", "")).substring(0, 12).toUpperCase(Locale.ROOT);
  }

  private record Snapshot(
      String itemName,
      String country,
      String datedType,
      String category,
      boolean apiTimeslot,
      boolean apiBooking,
      BigDecimal adultRate,
      BigDecimal childRate,
      String bookingInfo,
      List<String> extraFields,
      String description,
      String termsAndCondition,
      String included,
      String excluded,
      String imagePdf,
      String imageTicket) {
    static Snapshot from(ItemMaster item) {
      return new Snapshot(
          item.getItemName(),
          item.getCountry(),
          item.getDatedType(),
          item.getCategory(),
          item.isApiTimeslot(),
          item.isApiBooking(),
          item.getAdultRate(),
          item.getChildRate(),
          item.getBookingInfo(),
          item.extraFields(),
          item.getDescription(),
          item.getTermsAndCondition(),
          item.getIncluded(),
          item.getExcluded(),
          item.getImagePdf(),
          item.getImageTicket());
    }
  }
}
