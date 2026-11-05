package com.smarttech.model;

import org.bson.Document;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * Model đại diện cho Phiếu Nhập Kho Thiết Bị (ImportOrder) từ Nhà Cung Cấp.
 *
 * @author Senior Java Developer & Software Architect
 * @project SmartTech
 */
public class ImportOrder {

    public static final String STATUS_COMPLETED = "COMPLETED";
    public static final String STATUS_CANCELLED = "CANCELLED";

    private String id;                  // Mã phiếu nhập (Khóa chính _id, ví dụ "IMP-782910")
    private String supplierId;          // Mã nhà cung cấp
    private String supplierName;        // Tên nhà cung cấp
    private String employeeId;          // Mã nhân viên lập phiếu
    private LocalDate importDate;       // Ngày thực hiện nhập kho
    private List<ImportItem> items;     // Danh sách các thiết bị/serial nhập kho
    private double totalAmount;         // Tổng giá trị tiền hàng nhập kho
    private String status;              // Trạng thái phiếu: "COMPLETED", "CANCELLED"
    private String note;                // Ghi chú chi tiết đợt nhập kho

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE;

    public ImportOrder() {
        this.items = new ArrayList<>();
        this.status = STATUS_COMPLETED;
        this.importDate = LocalDate.now();
    }

    public ImportOrder(String id, String supplierId, String supplierName, String employeeId,
                       LocalDate importDate, List<ImportItem> items, double totalAmount,
                       String status, String note) {
        this.id = id;
        this.supplierId = supplierId;
        this.supplierName = supplierName;
        this.employeeId = employeeId;
        this.importDate = importDate != null ? importDate : LocalDate.now();
        this.items = items != null ? items : new ArrayList<>();
        this.totalAmount = totalAmount;
        this.status = status != null ? status : STATUS_COMPLETED;
        this.note = note;
    }

    public Document toDocument() {
        Document doc = new Document();
        if (id != null && !id.trim().isEmpty()) {
            doc.append("_id", id.trim());
        }
        doc.append("supplierId", supplierId != null ? supplierId : "")
                .append("supplierName", supplierName != null ? supplierName : "")
                .append("employeeId", employeeId != null ? employeeId : "")
                .append("importDate", importDate != null ? importDate.format(DATE_FORMATTER) : null);

        List<Document> itemDocs = new ArrayList<>();
        if (items != null) {
            for (ImportItem item : items) {
                if (item != null) itemDocs.add(item.toDocument());
            }
        }
        doc.append("items", itemDocs)
                .append("totalAmount", totalAmount)
                .append("status", status != null ? status : STATUS_COMPLETED)
                .append("note", note != null ? note : "");
        return doc;
    }

    @SuppressWarnings("unchecked")
    public static ImportOrder fromDocument(Document doc) {
        if (doc == null) return null;
        ImportOrder order = new ImportOrder();
        Object rawId = doc.get("_id");
        order.setId(rawId != null ? rawId.toString() : null);
        order.setSupplierId(doc.getString("supplierId"));
        order.setSupplierName(doc.getString("supplierName"));
        order.setEmployeeId(doc.getString("employeeId"));

        String dateStr = doc.getString("importDate");
        if (dateStr != null && !dateStr.trim().isEmpty()) {
            try {
                order.setImportDate(LocalDate.parse(dateStr, DATE_FORMATTER));
            } catch (Exception ex) {
                order.setImportDate(LocalDate.now());
            }
        } else {
            order.setImportDate(LocalDate.now());
        }

        List<ImportItem> itemList = new ArrayList<>();
        List<Document> itemDocs = (List<Document>) doc.get("items");
        if (itemDocs != null) {
            for (Document itemDoc : itemDocs) {
                ImportItem it = ImportItem.fromDocument(itemDoc);
                if (it != null) itemList.add(it);
            }
        }
        order.setItems(itemList);

        order.setTotalAmount(doc.getDouble("totalAmount") != null ? doc.getDouble("totalAmount") : 0.0);
        order.setStatus(doc.getString("status") != null ? doc.getString("status") : STATUS_COMPLETED);
        order.setNote(doc.getString("note"));
        return order;
    }

    // ================= GETTERS & SETTERS =================

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getSupplierId() {
        return supplierId;
    }

    public void setSupplierId(String supplierId) {
        this.supplierId = supplierId;
    }

    public String getSupplierName() {
        return supplierName;
    }

    public void setSupplierName(String supplierName) {
        this.supplierName = supplierName;
    }

    public String getEmployeeId() {
        return employeeId;
    }

    public void setEmployeeId(String employeeId) {
        this.employeeId = employeeId;
    }

    public LocalDate getImportDate() {
        return importDate;
    }

    public void setImportDate(LocalDate importDate) {
        this.importDate = importDate;
    }

    public List<ImportItem> getItems() {
        return items;
    }

    public void setItems(List<ImportItem> items) {
        this.items = items;
    }

    public double getTotalAmount() {
        return totalAmount;
    }

    public void setTotalAmount(double totalAmount) {
        this.totalAmount = totalAmount;
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

    public int getItemCount() {
        return items != null ? items.size() : 0;
    }
}
