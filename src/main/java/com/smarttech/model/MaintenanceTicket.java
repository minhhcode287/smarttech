package com.smarttech.model;

import org.bson.Document;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

/**
 * Model đại diện cho Phiếu Quản Lý Bảo Trì & Sửa Chữa Thiết Bị (MaintenanceTicket).
 * Quản lý vòng đời sửa chữa phần cứng: từ lúc phát hiện sự cố, gửi đi bảo hành,
 * dự toán chi phí, theo dõi tiến độ đến khi nghiệm thu hoàn tất và nhập lại kho.
 *
 * @author Senior Java Developer & Software Architect
 * @project SmartTech
 */
public class MaintenanceTicket {

    public static final String STATUS_IN_PROGRESS = "IN_PROGRESS";
    public static final String STATUS_COMPLETED = "COMPLETED";
    public static final String STATUS_CANCELLED = "CANCELLED";

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE;

    private String id;                      // Mã phiếu bảo trì (ví dụ: "MAIN-20261105-01")
    private String serialId;                // Mã Serial thiết bị gửi sửa/bảo trì
    private String productId;               // Mã dòng sản phẩm
    private String productName;            // Tên sản phẩm hiển thị
    private String issueDescription;        // Mô tả sự cố / Lý do bảo dưỡng
    private String repairVendor;            // Đơn vị sửa chữa: "Nội Bộ SmartTech" hoặc Tên TTBH ngoài
    private double repairCost;              // Chi phí sửa chữa (VNĐ)
    private LocalDate startDate;            // Ngày gửi bảo trì
    private LocalDate expectedDate;         // Ngày dự kiến hoàn thành
    private LocalDate completionDate;       // Ngày hoàn thành thực tế
    private String status;                  // Trạng thái phiếu: "IN_PROGRESS", "COMPLETED", "CANCELLED"
    private String resolutionNote;          // Kết quả xử lý / Linh kiện đã thay thế
    private String employeeId;              // Mã nhân viên lập phiếu
    private String employeeName;            // Tên nhân viên lập phiếu

    public MaintenanceTicket() {
        this.status = STATUS_IN_PROGRESS;
        this.startDate = LocalDate.now();
        this.expectedDate = LocalDate.now().plusDays(3);
        this.repairVendor = "Nội Bộ SmartTech";
        this.repairCost = 0.0;
    }

    public MaintenanceTicket(String id, String serialId, String productId, String productName,
                             String issueDescription, String repairVendor, double repairCost,
                             LocalDate startDate, LocalDate expectedDate, LocalDate completionDate,
                             String status, String resolutionNote,
                             String employeeId, String employeeName) {
        this.id = id;
        this.serialId = serialId != null ? serialId.trim().toUpperCase() : "";
        this.productId = productId;
        this.productName = productName;
        this.issueDescription = issueDescription;
        this.repairVendor = repairVendor != null ? repairVendor : "Nội Bộ SmartTech";
        this.repairCost = Math.max(0.0, repairCost);
        this.startDate = startDate != null ? startDate : LocalDate.now();
        this.expectedDate = expectedDate != null ? expectedDate : LocalDate.now().plusDays(3);
        this.completionDate = completionDate;
        this.status = status != null ? status : STATUS_IN_PROGRESS;
        this.resolutionNote = resolutionNote != null ? resolutionNote : "";
        this.employeeId = employeeId;
        this.employeeName = employeeName;
    }

    public Document toDocument() {
        Document doc = new Document();
        if (id != null && !id.trim().isEmpty()) {
            doc.append("_id", id.trim());
        }
        doc.append("serialId", serialId != null ? serialId.toUpperCase() : "")
                .append("productId", productId != null ? productId : "")
                .append("productName", productName != null ? productName : "")
                .append("issueDescription", issueDescription != null ? issueDescription : "")
                .append("repairVendor", repairVendor != null ? repairVendor : "Nội Bộ SmartTech")
                .append("repairCost", repairCost)
                .append("startDate", startDate != null ? startDate.format(DATE_FORMATTER) : null)
                .append("expectedDate", expectedDate != null ? expectedDate.format(DATE_FORMATTER) : null)
                .append("completionDate", completionDate != null ? completionDate.format(DATE_FORMATTER) : null)
                .append("status", status != null ? status : STATUS_IN_PROGRESS)
                .append("resolutionNote", resolutionNote != null ? resolutionNote : "")
                .append("employeeId", employeeId != null ? employeeId : "")
                .append("employeeName", employeeName != null ? employeeName : "");
        return doc;
    }

    public static MaintenanceTicket fromDocument(Document doc) {
        if (doc == null) return null;
        MaintenanceTicket ticket = new MaintenanceTicket();
        Object rawId = doc.get("_id");
        ticket.setId(rawId != null ? rawId.toString() : null);
        ticket.setSerialId(doc.getString("serialId"));
        ticket.setProductId(doc.getString("productId"));
        ticket.setProductName(doc.getString("productName"));
        ticket.setIssueDescription(doc.getString("issueDescription"));
        ticket.setRepairVendor(doc.getString("repairVendor"));
        ticket.setRepairCost(doc.getDouble("repairCost") != null ? doc.getDouble("repairCost") : 0.0);

        String startStr = doc.getString("startDate");
        if (startStr != null && !startStr.trim().isEmpty()) {
            try { ticket.setStartDate(LocalDate.parse(startStr, DATE_FORMATTER)); }
            catch (Exception ex) { ticket.setStartDate(LocalDate.now()); }
        }

        String expectedStr = doc.getString("expectedDate");
        if (expectedStr != null && !expectedStr.trim().isEmpty()) {
            try { ticket.setExpectedDate(LocalDate.parse(expectedStr, DATE_FORMATTER)); }
            catch (Exception ex) { ticket.setExpectedDate(LocalDate.now().plusDays(3)); }
        }

        String compStr = doc.getString("completionDate");
        if (compStr != null && !compStr.trim().isEmpty()) {
            try { ticket.setCompletionDate(LocalDate.parse(compStr, DATE_FORMATTER)); }
            catch (Exception ignored) {}
        }

        ticket.setStatus(doc.getString("status") != null ? doc.getString("status") : STATUS_IN_PROGRESS);
        ticket.setResolutionNote(doc.getString("resolutionNote"));
        ticket.setEmployeeId(doc.getString("employeeId"));
        ticket.setEmployeeName(doc.getString("employeeName"));
        return ticket;
    }

    // ================= GETTERS & SETTERS =================

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

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

    public String getIssueDescription() {
        return issueDescription;
    }

    public void setIssueDescription(String issueDescription) {
        this.issueDescription = issueDescription;
    }

    public String getRepairVendor() {
        return repairVendor;
    }

    public void setRepairVendor(String repairVendor) {
        this.repairVendor = repairVendor;
    }

    public double getRepairCost() {
        return repairCost;
    }

    public void setRepairCost(double repairCost) {
        this.repairCost = Math.max(0.0, repairCost);
    }

    public LocalDate getStartDate() {
        return startDate;
    }

    public void setStartDate(LocalDate startDate) {
        this.startDate = startDate;
    }

    public LocalDate getExpectedDate() {
        return expectedDate;
    }

    public void setExpectedDate(LocalDate expectedDate) {
        this.expectedDate = expectedDate;
    }

    public LocalDate getCompletionDate() {
        return completionDate;
    }

    public void setCompletionDate(LocalDate completionDate) {
        this.completionDate = completionDate;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getResolutionNote() {
        return resolutionNote;
    }

    public void setResolutionNote(String resolutionNote) {
        this.resolutionNote = resolutionNote;
    }

    public String getEmployeeId() {
        return employeeId;
    }

    public void setEmployeeId(String employeeId) {
        this.employeeId = employeeId;
    }

    public String getEmployeeName() {
        return employeeName;
    }

    public void setEmployeeName(String employeeName) {
        this.employeeName = employeeName;
    }

    public boolean isInProgress() {
        return STATUS_IN_PROGRESS.equalsIgnoreCase(status);
    }
}
