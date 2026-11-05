package com.smarttech.model;

import org.bson.Document;

/**
 * Model đại diện cho từng dòng chi tiết kiểm đếm một thiết bị phần cứng cụ thể trong Phiếu Kiểm Kê.
 * Theo dõi sự sai lệch giữa Trạng thái trên CSDL (systemStatus) và Trạng thái kiểm tra thực tế tại kho (actualStatus).
 *
 * @author Senior Java Developer & Software Architect
 * @project SmartTech
 */
public class StockAuditDetail {

    private String serialId;        // Mã Serial phần cứng kiểm kê
    private String productId;       // Mã mẫu sản phẩm
    private String productName;     // Tên sản phẩm hiển thị
    private String systemStatus;    // Trạng thái trên hệ thống (AVAILABLE, RENTED, MAINTENANCE, SOLD, LOST, DAMAGED)
    private String actualStatus;    // Trạng thái thực tế kiểm tra (AVAILABLE, LOST, DAMAGED, NEED_MAINTENANCE, RENTED)
    private String note;            // Ghi chú chi tiết sai lệch / mô tả tình trạng hư hỏng

    public StockAuditDetail() {
    }

    public StockAuditDetail(String serialId, String productId, String productName,
                            String systemStatus, String actualStatus, String note) {
        this.serialId = serialId != null ? serialId.trim().toUpperCase() : "";
        this.productId = productId;
        this.productName = productName;
        this.systemStatus = systemStatus != null ? systemStatus : Equipment.STATUS_AVAILABLE;
        this.actualStatus = actualStatus != null ? actualStatus : this.systemStatus;
        this.note = note != null ? note : "";
    }

    /**
     * Kiểm tra xem thiết bị này có bị sai lệch trạng thái giữa CSDL và thực tế kiểm đếm hay không.
     *
     * @return true nếu actualStatus khác systemStatus
     */
    public boolean hasDiscrepancy() {
        if (actualStatus == null || systemStatus == null) return false;
        return !actualStatus.equalsIgnoreCase(systemStatus);
    }

    public Document toDocument() {
        Document doc = new Document();
        doc.append("serialId", serialId != null ? serialId.toUpperCase() : "")
                .append("productId", productId != null ? productId : "")
                .append("productName", productName != null ? productName : "")
                .append("systemStatus", systemStatus != null ? systemStatus : "")
                .append("actualStatus", actualStatus != null ? actualStatus : "")
                .append("note", note != null ? note : "");
        return doc;
    }

    public static StockAuditDetail fromDocument(Document doc) {
        if (doc == null) return null;
        StockAuditDetail detail = new StockAuditDetail();
        detail.setSerialId(doc.getString("serialId"));
        detail.setProductId(doc.getString("productId"));
        detail.setProductName(doc.getString("productName"));
        detail.setSystemStatus(doc.getString("systemStatus"));
        detail.setActualStatus(doc.getString("actualStatus"));
        detail.setNote(doc.getString("note"));
        return detail;
    }

    // ================= GETTERS & SETTERS =================

    public String getSerialId() {
        return serialId;
    }

    public void setSerialId(String serialId) {
        this.serialId = serialId != null ? serialId.trim().toUpperCase() : "";
    }

    public String getProductId() {
        return productId;
    }

    public void setProductId(String productId) {
        this.productId = productId;
    }

    public String getProductName() {
        return productName;
    }

    public void setProductName(String productName) {
        this.productName = productName;
    }

    public String getSystemStatus() {
        return systemStatus;
    }

    public void setSystemStatus(String systemStatus) {
        this.systemStatus = systemStatus;
    }

    public String getActualStatus() {
        return actualStatus;
    }

    public void setActualStatus(String actualStatus) {
        this.actualStatus = actualStatus;
    }

    public String getNote() {
        return note;
    }

    public void setNote(String note) {
        this.note = note;
    }
}
