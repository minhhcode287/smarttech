package com.smarttech.model;

import org.bson.Document;

/**
 * Lớp Category đại diện cho Danh mục phân loại sản phẩm trong hệ thống SmartTech.
 * Ví dụ: Laptop, Máy ảnh & Ống kính, Máy chiếu, Thiết bị VR/AR, Phụ kiện âm thanh.
 *
 * @author Senior Java Developer &amp; Software Architect
 * @project SmartTech
 */
public class Category {

    private String id;              // Mã danh mục (Khoá chính, _id trong MongoDB)
    private String categoryName;    // Tên danh mục (ví dụ: "Laptop Cao Cấp")
    private String description;     // Mô tả danh mục
    private boolean active;         // Trạng thái hiển thị / hoạt động

    /**
     * Constructor mặc định không tham số.
     */
    public Category() {
        this.active = true;
    }

    /**
     * Constructor tạo mới Category (chưa có ID từ CSDL).
     */
    public Category(String categoryName, String description, boolean active) {
        this.categoryName = categoryName;
        this.description = description;
        this.active = active;
    }

    /**
     * Constructor đầy đủ tham số bao gồm ID.
     */
    public Category(String id, String categoryName, String description, boolean active) {
        this.id = id;
        this.categoryName = categoryName;
        this.description = description;
        this.active = active;
    }

    // ================= GETTERS & SETTERS =================

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getCategoryName() {
        return categoryName;
    }

    public void setCategoryName(String categoryName) {
        this.categoryName = categoryName;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    // ================= MAPPING LOGIC (BSON DOCUMENT) =================

    /**
     * Chuyển đổi đối tượng Category sang org.bson.Document để lưu trữ vào MongoDB.
     *
     * @return Document chứa dữ liệu của Category
     */
    public Document toDocument() {
        Document doc = new Document();
        if (this.id != null && !this.id.trim().isEmpty()) {
            doc.append("_id", this.id);
        }
        doc.append("categoryName", this.categoryName != null ? this.categoryName : "")
                .append("description", this.description != null ? this.description : "")
                .append("active", this.active);
        return doc;
    }

    /**
     * Tạo đối tượng Category từ org.bson.Document của MongoDB (Null-safe).
     *
     * @param doc Document chứa dữ liệu từ MongoDB
     * @return Đối tượng Category hoàn chỉnh hoặc null nếu doc rỗng
     */
    public static Category fromDocument(Document doc) {
        if (doc == null) {
            return null;
        }

        Category category = new Category();

        // Xử lý an toàn trường _id (hỗ trợ cả ObjectId và String)
        if (doc.containsKey("_id") && doc.get("_id") != null) {
            category.setId(doc.get("_id").toString());
        } else if (doc.containsKey("id") && doc.get("id") != null) {
            category.setId(doc.get("id").toString());
        }

        category.setCategoryName(doc.getString("categoryName"));
        category.setDescription(doc.getString("description"));

        // Xử lý active an toàn
        if (doc.containsKey("active") && doc.get("active") != null) {
            category.setActive(doc.getBoolean("active", true));
        } else {
            category.setActive(true);
        }

        return category;
    }

    @Override
    public String toString() {
        return "Category{" +
                "id='" + id + '\'' +
                ", categoryName='" + categoryName + '\'' +
                ", description='" + description + '\'' +
                ", active=" + active +
                '}';
    }
}
