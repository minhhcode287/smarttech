package com.smarttech.model;

import org.bson.Document;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Lớp RentalContract đại diện cho Hợp đồng cho thuê thiết bị công nghệ theo ngày.
 *
 * @author Senior Java Developer & Software Architect
 * @project SmartTech
 */
public class RentalContract {

    public static final String STATUS_ACTIVE = "ACTIVE";       // Hợp đồng đang cho thuê
    public static final String STATUS_RETURNED = "RETURNED";   // Khách đã trả máy và thanh toán hoàn tất
    public static final String STATUS_OVERDUE = "OVERDUE";     // Quá hạn trả máy
    public static final String STATUS_CANCELLED = "CANCELLED"; // Hủy hợp đồng

    private String id;                       // Mã hợp đồng thuê (Khoá chính, _id trong MongoDB)
    private String customerId;               // Mã khách hàng thuê
    private String customerName;             // Tên khách hàng
    private String customerPhone;            // Số điện thoại khách hàng
    private String employeeId;               // Mã nhân viên lập hợp đồng
    private LocalDate startDate;             // Ngày bắt đầu thuê máy
    private LocalDate expectedEndDate;       // Ngày dự kiến trả máy
    private LocalDate actualEndDate;         // Ngày trả máy thực tế
    private int rentalDays;                  // Số ngày thuê
    private List<RentalItem> items;          // Danh sách các thiết bị thuê
    private double totalRentalFee;           // Tổng tiền thuê thiết bị
    private double totalDeposit;             // Tổng tiền đặt cọc giữ máy
    private double penaltyFee;               // Tiền phạt (nếu trễ hạn hoặc hỏng máy)
    private double refundDeposit;            // Số tiền hoàn trả cọc lại cho khách
    private String status;                   // ACTIVE, RETURNED, OVERDUE, CANCELLED
    private String note;                     // Ghi chú hợp đồng

    public RentalContract() {
        this.items = new ArrayList<>();
        this.startDate = LocalDate.now();
        this.expectedEndDate = LocalDate.now().plusDays(3);
        this.rentalDays = 3;
        this.status = STATUS_ACTIVE;
        this.penaltyFee = 0.0;
        this.refundDeposit = 0.0;
    }

    public RentalContract(String id, String customerId, String customerName, String customerPhone,
                          String employeeId, LocalDate startDate, LocalDate expectedEndDate,
                          int rentalDays, List<RentalItem> items, double totalRentalFee, double totalDeposit,
                          String status, String note) {
        this.id = id;
        this.customerId = customerId;
        this.customerName = customerName;
        this.customerPhone = customerPhone;
        this.employeeId = employeeId;
        this.startDate = startDate != null ? startDate : LocalDate.now();
        this.expectedEndDate = expectedEndDate != null ? expectedEndDate : LocalDate.now().plusDays(1);
        this.rentalDays = rentalDays;
        this.items = items != null ? items : new ArrayList<>();
        this.totalRentalFee = totalRentalFee;
        this.totalDeposit = totalDeposit;
        this.status = status != null ? status : STATUS_ACTIVE;
        this.note = note;
    }

    // ================= GETTERS & SETTERS =================

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getCustomerId() {
        return customerId;
    }

    public void setCustomerId(String customerId) {
        this.customerId = customerId;
    }

    public String getCustomerName() {
        return customerName;
    }

    public void setCustomerName(String customerName) {
        this.customerName = customerName;
    }

    public String getCustomerPhone() {
        return customerPhone;
    }

    public void setCustomerPhone(String customerPhone) {
        this.customerPhone = customerPhone;
    }

    public String getEmployeeId() {
        return employeeId;
    }

    public void setEmployeeId(String employeeId) {
        this.employeeId = employeeId;
    }

    public LocalDate getStartDate() {
        return startDate;
    }

    public void setStartDate(LocalDate startDate) {
        this.startDate = startDate;
    }

    public LocalDate getExpectedEndDate() {
        return expectedEndDate;
    }

    public void setExpectedEndDate(LocalDate expectedEndDate) {
        this.expectedEndDate = expectedEndDate;
    }

    public LocalDate getActualEndDate() {
        return actualEndDate;
    }

    public void setActualEndDate(LocalDate actualEndDate) {
        this.actualEndDate = actualEndDate;
    }

    public int getRentalDays() {
        return rentalDays;
    }

    public void setRentalDays(int rentalDays) {
        this.rentalDays = rentalDays;
    }

    public List<RentalItem> getItems() {
        return items;
    }

    public void setItems(List<RentalItem> items) {
        this.items = items;
    }

    public double getTotalRentalFee() {
        return totalRentalFee;
    }

    public void setTotalRentalFee(double totalRentalFee) {
        this.totalRentalFee = totalRentalFee;
    }

    public double getTotalDeposit() {
        return totalDeposit;
    }

    public void setTotalDeposit(double totalDeposit) {
        this.totalDeposit = totalDeposit;
    }

    public double getPenaltyFee() {
        return penaltyFee;
    }

    public void setPenaltyFee(double penaltyFee) {
        this.penaltyFee = penaltyFee;
    }

    public double getRefundDeposit() {
        return refundDeposit;
    }

    public void setRefundDeposit(double refundDeposit) {
        this.refundDeposit = refundDeposit;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getNote() {
        return note;
    }

    public void setNote(String note) {
        this.note = note;
    }

    // ================= MAPPING LOGIC =================

    public Document toDocument() {
        Document doc = new Document();
        if (this.id != null && !this.id.trim().isEmpty()) {
            doc.append("_id", this.id);
        }
        doc.append("customerId", this.customerId != null ? this.customerId : "")
                .append("customerName", this.customerName != null ? this.customerName : "")
                .append("customerPhone", this.customerPhone != null ? this.customerPhone : "")
                .append("employeeId", this.employeeId != null ? this.employeeId : "")
                .append("startDate", this.startDate != null ? this.startDate.toString() : LocalDate.now().toString())
                .append("expectedEndDate", this.expectedEndDate != null ? this.expectedEndDate.toString() : LocalDate.now().toString())
                .append("actualEndDate", this.actualEndDate != null ? this.actualEndDate.toString() : null)
                .append("rentalDays", this.rentalDays)
                .append("totalRentalFee", this.totalRentalFee)
                .append("totalDeposit", this.totalDeposit)
                .append("penaltyFee", this.penaltyFee)
                .append("refundDeposit", this.refundDeposit)
                .append("status", this.status != null ? this.status : STATUS_ACTIVE)
                .append("note", this.note != null ? this.note : "");

        List<Document> itemDocs = new ArrayList<>();
        if (this.items != null) {
            for (RentalItem item : this.items) {
                itemDocs.add(item.toDocument());
            }
        }
        doc.append("items", itemDocs);

        return doc;
    }

    public static RentalContract fromDocument(Document doc) {
        if (doc == null) return null;

        RentalContract c = new RentalContract();
        if (doc.containsKey("_id") && doc.get("_id") != null) {
            c.setId(doc.get("_id").toString());
        }

        c.setCustomerId(doc.getString("customerId"));
        c.setCustomerName(doc.getString("customerName"));
        c.setCustomerPhone(doc.getString("customerPhone"));
        c.setEmployeeId(doc.getString("employeeId"));

        String sDate = doc.getString("startDate");
        if (sDate != null) {
            try { c.setStartDate(LocalDate.parse(sDate)); } catch (Exception ignored) {}
        }

        String eDate = doc.getString("expectedEndDate");
        if (eDate != null) {
            try { c.setExpectedEndDate(LocalDate.parse(eDate)); } catch (Exception ignored) {}
        }

        String aDate = doc.getString("actualEndDate");
        if (aDate != null) {
            try { c.setActualEndDate(LocalDate.parse(aDate)); } catch (Exception ignored) {}
        }

        Number days = doc.get("rentalDays", Number.class);
        c.setRentalDays(days != null ? days.intValue() : 1);

        Number fee = doc.get("totalRentalFee", Number.class);
        c.setTotalRentalFee(fee != null ? fee.doubleValue() : 0.0);

        Number dep = doc.get("totalDeposit", Number.class);
        c.setTotalDeposit(dep != null ? dep.doubleValue() : 0.0);

        Number pen = doc.get("penaltyFee", Number.class);
        c.setPenaltyFee(pen != null ? pen.doubleValue() : 0.0);

        Number ref = doc.get("refundDeposit", Number.class);
        c.setRefundDeposit(ref != null ? ref.doubleValue() : 0.0);

        c.setStatus(doc.getString("status"));
        c.setNote(doc.getString("note"));

        List<RentalItem> list = new ArrayList<>();
        List<Document> itemDocs = doc.getList("items", Document.class);
        if (itemDocs != null) {
            for (Document d : itemDocs) {
                list.add(RentalItem.fromDocument(d));
            }
        }
        c.setItems(list);

        return c;
    }
}