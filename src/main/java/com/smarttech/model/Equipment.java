package com.smarttech.model;

import org.bson.Document;

/**
 * Lớp Equipment đại diện cho từng đơn vị Thiết bị thực tế cụ thể trong kho.
 * Mỗi thiết bị có một Số Serial (id) duy nhất được khắc trên phần cứng để theo dõi trạng thái
 * (Sẵn sàng cho thuê/bán, Đang cho thuê, Đã bán, Bảo trì, Hỏng hóc).
 *
 * @author Senior Java Developer &amp; Software Architect
 * @project SmartTech
 */
public class Equipment {

    // Các hằng số định nghĩa trạng thái vòng đời của thiết bị thực tế
    public static final String STATUS_AVAILABLE = "AVAILABLE";      // Có sẵn trong kho
    public static final String STATUS_RENTED = "RENTED";            // Đang được khách hàng thuê
    public static final String STATUS_SOLD = "SOLD";                // Đã bán đứt cho khách
    public static final String STATUS_MAINTENANCE = "MAINTENANCE";  // Đang bảo dưỡng / vệ sinh / sửa chữa
    public static final String STATUS_BROKEN = "BROKEN";            // Hư hỏng không thể sử dụng

    private String id;              // Mã Serial Number phần cứng duy nhất (Khoá chính, _id trong MongoDB)
    private String productId;       // Mã sản phẩm liên kết (Foreign key tới Product)
    private String status;          // Trạng thái hiện tại của máy (AVAILABLE, RENTED, SOLD, MAINTENANCE, BROKEN)
    private String conditionNote;   // Ghi chú chi tiết về tình trạng (ví dụ: "Mới 99%, kèm sạc zin và túi chống sốc")
    private boolean active;         // Trạng thái hoạt động trong hệ thống quản lý

    /**
     * Constructor mặc định không tham số.
     */
    public Equipment() {
        this.status = STATUS_AVAILABLE;
        this.active = true;
    }

    /**
     * Constructor tạo mới Equipment với Serial và ProductId.
     */
    public Equipment(String id, String productId, String status, String conditionNote, boolean active) {
        this.id = id;
        this.productId = productId;
        this.status = status != null ? status : STATUS_AVAILABLE;
        this.conditionNote = conditionNote;
        this.active = active;
    }

    /**
     * Constructor không truyền Serial (thường ít dùng do thiết bị thực tế luôn có Serial Number phần cứng).
     */
    public Equipment(String productId, String status, String conditionNote, boolean active) {
        this.productId = productId;
        this.status = status != null ? status : STATUS_AVAILABLE;
        this.conditionNote = conditionNote;
        this.active = active;
    }

    // ================= GETTERS & SETTERS =================

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getProductId() {
        return productId;
    }

    public void setProductId(String productId) {
        this.productId = productId;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getConditionNote() {
        return conditionNote;
    }

    public void setConditionNote(String conditionNote) {
        this.conditionNote = conditionNote;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    // ================= MAPPING LOGIC (BSON DOCUMENT) =================

    /**
     * Chuyển đổi đối tượng Equipment sang org.bson.Document để lưu trữ vào MongoDB.
     * Trường `_id` được gán chính là mã Serial Number của thiết bị vật lý.
     *
     * @return Document chứa dữ liệu của Equipment
     */
    public Document toDocument() {
        Document doc = new Document();
        if (this.id != null && !this.id.trim().isEmpty()) {
            doc.append("_id", this.id);
        }
        doc.append("productId", this.productId != null ? this.productId : "")
                .append("status", this.status != null ? this.status : STATUS_AVAILABLE)
                .append("conditionNote", this.conditionNote != null ? this.conditionNote : "")
                .append("active", this.active);
        return doc;
    }

    /**
     * Tạo đối tượng Equipment từ org.bson.Document của MongoDB (Null-safe).
     *
     * @param doc Document chứa dữ liệu từ MongoDB
     * @return Đối tượng Equipment hoàn chỉnh hoặc null nếu doc rỗng
     */
    public static Equipment fromDocument(Document doc) {
        if (doc == null) {
            return null;
        }

        Equipment equipment = new Equipment();

        // Xử lý an toàn trường _id (Serial Number)
        if (doc.containsKey("_id") && doc.get("_id") != null) {
            equipment.setId(doc.get("_id").toString());
        } else if (doc.containsKey("id") && doc.get("id") != null) {
            equipment.setId(doc.get("id").toString());
        }

        equipment.setProductId(doc.getString("productId"));

        // Xử lý trạng thái mặc định an toàn là AVAILABLE
        String statusVal = doc.getString("status");
        equipment.setStatus(statusVal != null ? statusVal : STATUS_AVAILABLE);

        equipment.setConditionNote(doc.getString("conditionNote"));

        // Xử lý active an toàn
        if (doc.containsKey("active") && doc.get("active") != null) {
            equipment.setActive(doc.getBoolean("active", true));
        } else {
            equipment.setActive(true);
        }

        return equipment;
    }

    @Override
    public String toString() {
        return "Equipment{" +
                "serialId='" + id + '\'' +
                ", productId='" + productId + '\'' +
                ", status='" + status + '\'' +
                ", conditionNote='" + conditionNote + '\'' +
                ", active=" + active +
                '}';
    }
}
