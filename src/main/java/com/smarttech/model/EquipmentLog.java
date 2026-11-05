package com.smarttech.model;

import org.bson.Document;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

/**
 * Lớp EquipmentLog đại diện cho Nhật ký biến động và theo dõi chu trình bảo trì,
 * bảo dưỡng, sửa chữa hoặc thay đổi trạng thái phần cứng của thiết bị (Audit Log).
 *
 * @author Senior Java Developer & Software Architect
 * @project SmartTech
 */
public class EquipmentLog {

    // Các hằng số định nghĩa phân loại hành động (Action Type)
    public static final String ACTION_SEND_MAINTENANCE = "SEND_MAINTENANCE";         // Đưa thiết bị đi bảo dưỡng/sửa chữa
    public static final String ACTION_COMPLETE_MAINTENANCE = "COMPLETE_MAINTENANCE"; // Hoàn tất sửa chữa, nghiệm thu về kho
    public static final String ACTION_STATUS_CHANGE = "STATUS_CHANGE";               // Thay đổi trạng thái khác

    private static final DateTimeFormatter ISO_FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

    private String id;                       // Mã định danh nhật ký (_id trong MongoDB)
    private String serialId;                 // Mã Serial thiết bị vật lý (ví dụ: "SN-140807")
    private String productId;                // Mã mẫu sản phẩm (liên kết với Product)
    private String actionType;               // Loại biến động: SEND_MAINTENANCE, COMPLETE_MAINTENANCE, STATUS_CHANGE
    private String oldStatus;                // Trạng thái cũ: AVAILABLE, RENTED, SOLD, MAINTENANCE, BROKEN
    private String newStatus;                // Trạng thái mới sau biến động
    private String vendor;                   // Đơn vị/Đối tác sửa chữa (ví dụ: "Trung Tâm Bảo Hành Apple")
    private double estimatedCost;            // Chi phí dự kiến (VNĐ)
    private double actualCost;               // Chi phí thực tế thanh toán (VNĐ)
    private String reason;                   // Lý do hỏng hóc hoặc mục đích bảo dưỡng
    private String repairNotes;              // Ghi chú chi tiết về kết quả sửa chữa, linh kiện thay thế
    private String performedBy;              // Tên tài khoản nhân viên thực hiện thao tác
    private LocalDateTime timestamp;         // Thời điểm phát sinh sự kiện

    /**
     * Constructor mặc định không tham số.
     */
    public EquipmentLog() {
        this.id = "LOG-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        this.actionType = ACTION_STATUS_CHANGE;
        this.estimatedCost = 0.0;
        this.actualCost = 0.0;
        this.timestamp = LocalDateTime.now();
    }

    /**
     * Constructor đầy đủ tham số (Full-args).
     */
    public EquipmentLog(String id, String serialId, String productId, String actionType,
                        String oldStatus, String newStatus, String vendor,
                        double estimatedCost, double actualCost, String reason,
                        String repairNotes, String performedBy, LocalDateTime timestamp) {
        this.id = (id != null && !id.trim().isEmpty()) ? id : "LOG-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        this.serialId = serialId;
        this.productId = productId;
        this.actionType = actionType != null ? actionType : ACTION_STATUS_CHANGE;
        this.oldStatus = oldStatus;
        this.newStatus = newStatus;
        this.vendor = vendor;
        this.estimatedCost = Math.max(0.0, estimatedCost);
        this.actualCost = Math.max(0.0, actualCost);
        this.reason = reason;
        this.repairNotes = repairNotes;
        this.performedBy = performedBy;
        this.timestamp = timestamp != null ? timestamp : LocalDateTime.now();
    }

    /**
     * Constructor tạo nhanh nhật ký biến động.
     */
    public EquipmentLog(String serialId, String productId, String actionType,
                        String oldStatus, String newStatus, String vendor,
                        double estimatedCost, double actualCost, String reason,
                        String repairNotes, String performedBy) {
        this(null, serialId, productId, actionType, oldStatus, newStatus, vendor,
                estimatedCost, actualCost, reason, repairNotes, performedBy, LocalDateTime.now());
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
        this.serialId = serialId;
    }

    public String getProductId() {
        return productId;
    }

    public void setProductId(String productId) {
        this.productId = productId;
    }

    public String getActionType() {
        return actionType;
    }

    public void setActionType(String actionType) {
        this.actionType = actionType;
    }

    public String getOldStatus() {
        return oldStatus;
    }

    public void setOldStatus(String oldStatus) {
        this.oldStatus = oldStatus;
    }

    public String getNewStatus() {
        return newStatus;
    }

    public void setNewStatus(String newStatus) {
        this.newStatus = newStatus;
    }

    public String getVendor() {
        return vendor;
    }

    public void setVendor(String vendor) {
        this.vendor = vendor;
    }

    public double getEstimatedCost() {
        return estimatedCost;
    }

    public void setEstimatedCost(double estimatedCost) {
        this.estimatedCost = Math.max(0.0, estimatedCost);
    }

    public double getActualCost() {
        return actualCost;
    }

    public void setActualCost(double actualCost) {
        this.actualCost = Math.max(0.0, actualCost);
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public String getRepairNotes() {
        return repairNotes;
    }

    public void setRepairNotes(String repairNotes) {
        this.repairNotes = repairNotes;
    }

    public String getPerformedBy() {
        return performedBy;
    }

    public void setPerformedBy(String performedBy) {
        this.performedBy = performedBy;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp != null ? timestamp : LocalDateTime.now();
    }

    // ================= MAPPING LOGIC (BSON DOCUMENT) =================

    /**
     * Chuyển đổi đối tượng EquipmentLog sang org.bson.Document để lưu trữ vào MongoDB.
     *
     * @return BSON Document
     */
    public Document toDocument() {
        Document doc = new Document();
        if (this.id != null && !this.id.trim().isEmpty()) {
            doc.append("_id", this.id);
        } else {
            this.id = "LOG-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
            doc.append("_id", this.id);
        }

        doc.append("serialId", this.serialId != null ? this.serialId : "")
                .append("productId", this.productId != null ? this.productId : "")
                .append("actionType", this.actionType != null ? this.actionType : ACTION_STATUS_CHANGE)
                .append("oldStatus", this.oldStatus != null ? this.oldStatus : "")
                .append("newStatus", this.newStatus != null ? this.newStatus : "")
                .append("vendor", this.vendor != null ? this.vendor : "")
                .append("estimatedCost", this.estimatedCost)
                .append("actualCost", this.actualCost)
                .append("reason", this.reason != null ? this.reason : "")
                .append("repairNotes", this.repairNotes != null ? this.repairNotes : "")
                .append("performedBy", this.performedBy != null ? this.performedBy : "SYSTEM")
                .append("timestamp", this.timestamp != null ? this.timestamp.format(ISO_FORMATTER) : LocalDateTime.now().format(ISO_FORMATTER));

        return doc;
    }

    /**
     * Tạo đối tượng EquipmentLog từ org.bson.Document (Null-safe).
     *
     * @param doc BSON Document lấy từ MongoDB
     * @return Đối tượng EquipmentLog hoàn chỉnh
     */
    public static EquipmentLog fromDocument(Document doc) {
        if (doc == null) {
            return null;
        }

        EquipmentLog log = new EquipmentLog();

        if (doc.containsKey("_id") && doc.get("_id") != null) {
            log.setId(doc.get("_id").toString());
        } else if (doc.containsKey("id") && doc.get("id") != null) {
            log.setId(doc.get("id").toString());
        }

        log.setSerialId(doc.getString("serialId"));
        log.setProductId(doc.getString("productId"));
        log.setActionType(doc.getString("actionType"));
        log.setOldStatus(doc.getString("oldStatus"));
        log.setNewStatus(doc.getString("newStatus"));
        log.setVendor(doc.getString("vendor"));

        Number est = doc.get("estimatedCost", Number.class);
        log.setEstimatedCost(est != null ? est.doubleValue() : 0.0);

        Number act = doc.get("actualCost", Number.class);
        log.setActualCost(act != null ? act.doubleValue() : 0.0);

        log.setReason(doc.getString("reason"));
        log.setRepairNotes(doc.getString("repairNotes"));
        log.setPerformedBy(doc.getString("performedBy"));

        String timeStr = doc.getString("timestamp");
        if (timeStr != null && !timeStr.trim().isEmpty()) {
            try {
                log.setTimestamp(LocalDateTime.parse(timeStr, ISO_FORMATTER));
            } catch (Exception e) {
                try {
                    log.setTimestamp(LocalDateTime.parse(timeStr));
                } catch (Exception ignored) {
                    log.setTimestamp(LocalDateTime.now());
                }
            }
        } else {
            log.setTimestamp(LocalDateTime.now());
        }

        return log;
    }

    @Override
    public String toString() {
        return "EquipmentLog{" +
                "id='" + id + '\'' +
                ", serialId='" + serialId + '\'' +
                ", productId='" + productId + '\'' +
                ", actionType='" + actionType + '\'' +
                ", oldStatus='" + oldStatus + '\'' +
                ", newStatus='" + newStatus + '\'' +
                ", vendor='" + vendor + '\'' +
                ", estimatedCost=" + estimatedCost +
                ", actualCost=" + actualCost +
                ", reason='" + reason + '\'' +
                ", performedBy='" + performedBy + '\'' +
                ", timestamp=" + timestamp +
                '}';
    }
}