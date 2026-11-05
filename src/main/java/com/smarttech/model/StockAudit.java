package com.smarttech.model;

import org.bson.Document;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * Model đại diện cho Phiếu Kiểm Kê & Cân Bằng Kho Thiết Bị (StockAudit).
 * Theo dõi kết quả tổng thể của một đợt kiểm kê, số lượng thiết bị khớp và số lượng sai lệch (lệch trạng thái, thất thoát, hỏng hóc).
 *
 * @author Senior Java Developer & Software Architect
 * @project SmartTech
 */
public class StockAudit {

    public static final String STATUS_DRAFT = "DRAFT";
    public static final String STATUS_COMPLETED = "COMPLETED";
    public static final String STATUS_CANCELLED = "CANCELLED";

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE;

    private String id;                          // Mã phiếu kiểm kê (ví dụ: "AUDIT-20261105-01")
    private String auditName;                   // Tên đợt kiểm kê (ví dụ: "Kiểm kê định kỳ Kho Thiết Bị Q4/2026")
    private String employeeId;                  // Mã nhân viên kiểm kê
    private String employeeName;                // Tên nhân viên kiểm kê
    private LocalDate auditDate;                // Ngày kiểm kê
    private List<StockAuditDetail> details;     // Danh sách chi tiết kiểm kê từng thiết bị
    private int totalSystemCount;               // Tổng số máy trên CSDL
    private int totalActualCount;               // Tổng số máy thực tế kiểm đếm
    private int totalDiscrepancy;               // Số máy có sai lệch trạng thái hoặc mất mát
    private String status;                      // Trạng thái phiếu: "DRAFT", "COMPLETED", "CANCELLED"
    private String note;                        // Ghi chú tổng kết đợt kiểm kê

    public StockAudit() {
        this.details = new ArrayList<>();
        this.auditDate = LocalDate.now();
        this.status = STATUS_COMPLETED;
    }

    public StockAudit(String id, String auditName, String employeeId, String employeeName,
                      LocalDate auditDate, List<StockAuditDetail> details,
                      int totalSystemCount, int totalActualCount, int totalDiscrepancy,
                      String status, String note) {
        this.id = id;
        this.auditName = auditName;
        this.employeeId = employeeId;
        this.employeeName = employeeName;
        this.auditDate = auditDate != null ? auditDate : LocalDate.now();
        this.details = details != null ? details : new ArrayList<>();
        this.totalSystemCount = totalSystemCount;
        this.totalActualCount = totalActualCount;
        this.totalDiscrepancy = totalDiscrepancy;
        this.status = status != null ? status : STATUS_COMPLETED;
        this.note = note != null ? note : "";
    }

    /**
     * Tự động tính toán lại tổng số máy, số máy khớp và số máy sai lệch từ danh sách chi tiết.
     */
    public void recalculateCounts() {
        if (details == null) {
            this.totalSystemCount = 0;
            this.totalActualCount = 0;
            this.totalDiscrepancy = 0;
            return;
        }

        this.totalSystemCount = details.size();
        this.totalActualCount = details.size();
        int discrepancy = 0;
        for (StockAuditDetail d : details) {
            if (d.hasDiscrepancy()) {
                discrepancy++;
            }
        }
        this.totalDiscrepancy = discrepancy;
    }

    public Document toDocument() {
        Document doc = new Document();
        if (id != null && !id.trim().isEmpty()) {
            doc.append("_id", id.trim());
        }
        doc.append("auditName", auditName != null ? auditName : "")
                .append("employeeId", employeeId != null ? employeeId : "")
                .append("employeeName", employeeName != null ? employeeName : "")
                .append("auditDate", auditDate != null ? auditDate.format(DATE_FORMATTER) : null);

        List<Document> detailDocs = new ArrayList<>();
        if (details != null) {
            for (StockAuditDetail d : details) {
                if (d != null) detailDocs.add(d.toDocument());
            }
        }
        doc.append("details", detailDocs)
                .append("totalSystemCount", totalSystemCount)
                .append("totalActualCount", totalActualCount)
                .append("totalDiscrepancy", totalDiscrepancy)
                .append("status", status != null ? status : STATUS_COMPLETED)
                .append("note", note != null ? note : "");
        return doc;
    }

    @SuppressWarnings("unchecked")
    public static StockAudit fromDocument(Document doc) {
        if (doc == null) return null;
        StockAudit audit = new StockAudit();
        Object rawId = doc.get("_id");
        audit.setId(rawId != null ? rawId.toString() : null);
        audit.setAuditName(doc.getString("auditName"));
        audit.setEmployeeId(doc.getString("employeeId"));
        audit.setEmployeeName(doc.getString("employeeName"));

        String dateStr = doc.getString("auditDate");
        if (dateStr != null && !dateStr.trim().isEmpty()) {
            try {
                audit.setAuditDate(LocalDate.parse(dateStr, DATE_FORMATTER));
            } catch (Exception ex) {
                audit.setAuditDate(LocalDate.now());
            }
        } else {
            audit.setAuditDate(LocalDate.now());
        }

        List<StockAuditDetail> detailList = new ArrayList<>();
        List<Document> detailDocs = (List<Document>) doc.get("details");
        if (detailDocs != null) {
            for (Document dDoc : detailDocs) {
                StockAuditDetail d = StockAuditDetail.fromDocument(dDoc);
                if (d != null) detailList.add(d);
            }
        }
        audit.setDetails(detailList);

        audit.setTotalSystemCount(doc.getInteger("totalSystemCount", 0));
        audit.setTotalActualCount(doc.getInteger("totalActualCount", 0));
        audit.setTotalDiscrepancy(doc.getInteger("totalDiscrepancy", 0));
        audit.setStatus(doc.getString("status") != null ? doc.getString("status") : STATUS_COMPLETED);
        audit.setNote(doc.getString("note"));
        return audit;
    }

    // ================= GETTERS & SETTERS =================

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getAuditName() {
        return auditName;
    }

    public void setAuditName(String auditName) {
        this.auditName = auditName;
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

    public LocalDate getAuditDate() {
        return auditDate;
    }

    public void setAuditDate(LocalDate auditDate) {
        this.auditDate = auditDate;
    }

    public List<StockAuditDetail> getDetails() {
        return details;
    }

    public void setDetails(List<StockAuditDetail> details) {
        this.details = details;
    }

    public int getTotalSystemCount() {
        return totalSystemCount;
    }

    public void setTotalSystemCount(int totalSystemCount) {
        this.totalSystemCount = totalSystemCount;
    }

    public int getTotalActualCount() {
        return totalActualCount;
    }

    public void setTotalActualCount(int totalActualCount) {
        this.totalActualCount = totalActualCount;
    }

    public int getTotalDiscrepancy() {
        return totalDiscrepancy;
    }

    public void setTotalDiscrepancy(int totalDiscrepancy) {
        this.totalDiscrepancy = totalDiscrepancy;
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
}
