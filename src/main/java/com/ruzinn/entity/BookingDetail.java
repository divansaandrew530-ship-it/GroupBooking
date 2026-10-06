package com.ruzinn.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "booking_details")
public class BookingDetail {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "booking_ref_num", nullable = false)
    private BookingMain booking;

    @Column(name = "service_name", nullable = false, length = 160)
    private String serviceName;

    @Column(name = "service_id", nullable = false, length = 80)
    private String serviceId;

    @Column(name = "item_name", nullable = false, length = 240)
    private String itemName;

    @Column(name = "show_date")
    private LocalDate showDate;

    @Column(name = "checkin_date")
    private LocalDate checkinDate;

    @Column(name = "checkout_date")
    private LocalDate checkoutDate;

    @Column(name = "property_id", length = 80)
    private String propertyId;

    @Column(name = "adult_count", nullable = false)
    private int adultCount;

    @Column(name = "child_count", nullable = false)
    private int childCount;

    @Column(name = "infant_count", nullable = false)
    private int infantCount;

    @Column(name = "toddler_count", nullable = false)
    private int toddlerCount;

    @Column(name = "total_rate", nullable = false, precision = 12, scale = 2)
    private BigDecimal totalRate;

    @Column(name = "actual_total_rate", precision = 12, scale = 2)
    private BigDecimal actualTotalRate;

    @Column(name = "roe_rate", precision = 12, scale = 6)
    private BigDecimal roeRate;

    @Column(name = "converted_rate", precision = 12, scale = 2)
    private BigDecimal convertedRate;

    @Column(name = "actual_converted_rate", precision = 12, scale = 2)
    private BigDecimal actualConvertedRate;

    @Column(name = "booked_on", nullable = false)
    private Instant bookedOn;

    @Column(name = "booked_by", nullable = false, length = 160)
    private String bookedBy;

    @Column(name = "user_id", nullable = false, length = 160)
    private String userId;

    @Column(name = "company_name", nullable = false, length = 160)
    private String companyName;

    @Column(name = "company_id", nullable = false, length = 80)
    private String companyId;

    @Column(name = "inventory_type", nullable = false, length = 40)
    private String inventoryType;

    @Column(name = "supplier_name", nullable = false, length = 160)
    private String supplierName;

    @Column(name = "supplier_id", nullable = false, length = 80)
    private String supplierId;

    @Column(name = "item_status", length = 32)
    private String status = "PENDING";

    @Column(name = "follow_up", length = 500)
    private String followUp;

    @Column(name = "voucher_file_name", length = 255)
    private String voucherFileName;

    @Column(name = "voucher_content_type", length = 120)
    private String voucherContentType;

    @JdbcTypeCode(SqlTypes.VARBINARY)
    @Column(name = "voucher_bytes", length = 16_777_216)
    private byte[] voucherBytes;

    @Column(name = "file_path", length = 512)
    private String filePath;

    /**
     * Guests entered at checkout, as a JSON array of flat objects, e.g.
     * {@code [{"type":"ADULT","firstName":"Ravi","lastName":"Kumar","passport":"P123","dateOfBirth":"1990-05-17","age":"36"}]}.
     */
    @Column(name = "passengers", columnDefinition = "text")
    private String passengers;

    @Column(name = "ticket_file_name", length = 255)
    private String ticketFileName;

    @Column(name = "ticket_content_type", length = 120)
    private String ticketContentType;

    @JdbcTypeCode(SqlTypes.VARBINARY)
    @Column(name = "ticket_bytes", length = 16_777_216)
    private byte[] ticketBytes;

    public Long getId() {
        return id;
    }

    public BookingMain getBooking() {
        return booking;
    }

    public void setBooking(BookingMain booking) {
        this.booking = booking;
    }

    public String getServiceName() {
        return serviceName;
    }

    public void setServiceName(String serviceName) {
        this.serviceName = serviceName;
    }

    public String getServiceId() {
        return serviceId;
    }

    public void setServiceId(String serviceId) {
        this.serviceId = serviceId;
    }

    public String getItemName() {
        return itemName;
    }

    public void setItemName(String itemName) {
        this.itemName = itemName;
    }

    public LocalDate getShowDate() {
        return showDate;
    }

    public void setShowDate(LocalDate showDate) {
        this.showDate = showDate;
    }

    public LocalDate getCheckinDate() {
        return checkinDate;
    }

    public void setCheckinDate(LocalDate checkinDate) {
        this.checkinDate = checkinDate;
    }

    public LocalDate getCheckoutDate() {
        return checkoutDate;
    }

    public void setCheckoutDate(LocalDate checkoutDate) {
        this.checkoutDate = checkoutDate;
    }

    public String getPropertyId() {
        return propertyId;
    }

    public void setPropertyId(String propertyId) {
        this.propertyId = propertyId;
    }

    public int getAdultCount() {
        return adultCount;
    }

    public void setAdultCount(int adultCount) {
        this.adultCount = adultCount;
    }

    public int getChildCount() {
        return childCount;
    }

    public void setChildCount(int childCount) {
        this.childCount = childCount;
    }

    public int getInfantCount() {
        return infantCount;
    }

    public void setInfantCount(int infantCount) {
        this.infantCount = infantCount;
    }

    public int getToddlerCount() {
        return toddlerCount;
    }

    public void setToddlerCount(int toddlerCount) {
        this.toddlerCount = toddlerCount;
    }

    public BigDecimal getTotalRate() {
        return totalRate;
    }

    public void setTotalRate(BigDecimal totalRate) {
        this.totalRate = totalRate;
    }

    public BigDecimal getActualTotalRate() {
        return actualTotalRate;
    }

    public void setActualTotalRate(BigDecimal actualTotalRate) {
        this.actualTotalRate = actualTotalRate;
    }

    public BigDecimal getRoeRate() {
        return roeRate;
    }

    public void setRoeRate(BigDecimal roeRate) {
        this.roeRate = roeRate;
    }

    public BigDecimal getConvertedRate() {
        return convertedRate;
    }

    public void setConvertedRate(BigDecimal convertedRate) {
        this.convertedRate = convertedRate;
    }

    public BigDecimal getActualConvertedRate() {
        return actualConvertedRate;
    }

    public void setActualConvertedRate(BigDecimal actualConvertedRate) {
        this.actualConvertedRate = actualConvertedRate;
    }

    public Instant getBookedOn() {
        return bookedOn;
    }

    public void setBookedOn(Instant bookedOn) {
        this.bookedOn = bookedOn;
    }

    public String getBookedBy() {
        return bookedBy;
    }

    public void setBookedBy(String bookedBy) {
        this.bookedBy = bookedBy;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getCompanyName() {
        return companyName;
    }

    public void setCompanyName(String companyName) {
        this.companyName = companyName;
    }

    public String getCompanyId() {
        return companyId;
    }

    public void setCompanyId(String companyId) {
        this.companyId = companyId;
    }

    public String getInventoryType() {
        return inventoryType;
    }

    public void setInventoryType(String inventoryType) {
        this.inventoryType = inventoryType;
    }

    public String getSupplierName() {
        return supplierName;
    }

    public void setSupplierName(String supplierName) {
        this.supplierName = supplierName;
    }

    public String getSupplierId() {
        return supplierId;
    }

    public void setSupplierId(String supplierId) {
        this.supplierId = supplierId;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getFollowUp() {
        return followUp;
    }

    public void setFollowUp(String followUp) {
        this.followUp = followUp;
    }

    public String getVoucherFileName() {
        return voucherFileName;
    }

    public void setVoucherFileName(String voucherFileName) {
        this.voucherFileName = voucherFileName;
    }

    public String getVoucherContentType() {
        return voucherContentType;
    }

    public void setVoucherContentType(String voucherContentType) {
        this.voucherContentType = voucherContentType;
    }

    public byte[] getVoucherBytes() {
        return voucherBytes;
    }

    public void setVoucherBytes(byte[] voucherBytes) {
        this.voucherBytes = voucherBytes;
    }

    public String getFilePath() {
        return filePath;
    }

    public void setFilePath(String filePath) {
        this.filePath = filePath;
    }

    public String getPassengers() {
        return passengers;
    }

    public void setPassengers(String passengers) {
        this.passengers = passengers;
    }

    public String getTicketFileName() {
        return ticketFileName;
    }

    public void setTicketFileName(String ticketFileName) {
        this.ticketFileName = ticketFileName;
    }

    public String getTicketContentType() {
        return ticketContentType;
    }

    public void setTicketContentType(String ticketContentType) {
        this.ticketContentType = ticketContentType;
    }

    public byte[] getTicketBytes() {
        return ticketBytes;
    }

    public void setTicketBytes(byte[] ticketBytes) {
        this.ticketBytes = ticketBytes;
    }
}
