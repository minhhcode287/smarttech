package com.smarttech.model;

import org.bson.Document;

/**
 * Lớp Product đại diện cho Mẫu/Dòng sản phẩm thiết bị công nghệ trong hệ thống SmartTech.
 * Ví dụ: "MacBook Pro 16 inch M3 Max", "Sony Alpha A7 IV", "Epson EB-X06".
 * Mỗi sản phẩm có giá niêm yết bán và đơn giá thuê theo ngày.
 *
 * @author Senior Java Developer &amp; Software Architect
 * @project SmartTech
 */
public class Product {

    private String id;                 // Mã sản phẩm (Khoá chính, _id trong MongoDB)
    private String categoryId;         // Mã danh mục liên kết (Foreign key tới Category)
    private String productName;        // Tên sản phẩm thiết bị
    private String brand;              // Thương hiệu (Apple, Sony, Dell, Canon, v.v.)
    private double basePrice;          // Giá bán lẻ đề xuất (VND)
    private double rentalRatePerDay;   // Giá cho thuê theo ngày (VND/ngày)
    private String specifications;     // Thông số kỹ thuật / cấu hình chi tiết
    private boolean active;            // Trạng thái kinh doanh (Đang bán/cho thuê hay ngưng)

    /**
     * Constructor mặc định không tham số.
     */
    public Product() {
        this.active = true;
        this.basePrice = 0.0;
        this.rentalRatePerDay = 0.0;
    }

    /**
     * Constructor tạo mới Product (chưa có ID từ CSDL).
     */
    public Product(String categoryId, String productName, String brand,
                   double basePrice, double rentalRatePerDay, String specifications, boolean active) {
        this.categoryId = categoryId;
        this.productName = productName;
        this.brand = brand;
        this.basePrice = basePrice;
        this.rentalRatePerDay = rentalRatePerDay;
        this.specifications = specifications;
        this.active = active;
    }

    /**
     * Constructor đầy đủ tham số bao gồm ID.
     */
    public Product(String id, String categoryId, String productName, String brand,
                   double basePrice, double rentalRatePerDay, String specifications, boolean active) {
        this.id = id;
        this.categoryId = categoryId;
        this.productName = productName;
        this.brand = brand;
        this.basePrice = basePrice;
        this.rentalRatePerDay = rentalRatePerDay;
        this.specifications = specifications;
        this.active = active;
    }

    // ================= GETTERS & SETTERS =================

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getCategoryId() {
        return categoryId;
    }

    public void setCategoryId(String categoryId) {
        this.categoryId = categoryId;
    }

    public String getProductName() {
        return productName;
    }

    public void setProductName(String productName) {
        this.productName = productName;
    }

    public String getBrand() {
        return brand;
    }

    public void setBrand(String brand) {
        this.brand = brand;
    }

    public double getBasePrice() {
        return basePrice;
    }

    public void setBasePrice(double basePrice) {
        this.basePrice = basePrice;
    }

    public double getRentalRatePerDay() {
        return rentalRatePerDay;
    }

    public void setRentalRatePerDay(double rentalRatePerDay) {
        this.rentalRatePerDay = rentalRatePerDay;
    }

    public String getSpecifications() {
        return specifications;
    }

    public void setSpecifications(String specifications) {
        this.specifications = specifications;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    // ================= MAPPING LOGIC (BSON DOCUMENT) =================

    /**
     * Chuyển đổi đối tượng Product sang org.bson.Document để lưu trữ vào MongoDB.
     *
     * @return Document chứa dữ liệu của Product
     */
    public Document toDocument() {
        Document doc = new Document();
        if (this.id != null && !this.id.trim().isEmpty()) {
            doc.append("_id", this.id);
        }
        doc.append("categoryId", this.categoryId != null ? this.categoryId : "")
                .append("productName", this.productName != null ? this.productName : "")
                .append("brand", this.brand != null ? this.brand : "")
                .append("basePrice", this.basePrice)
                .append("rentalRatePerDay", this.rentalRatePerDay)
                .append("specifications", this.specifications != null ? this.specifications : "")
                .append("active", this.active);
        return doc;
    }

    /**
     * Tạo đối tượng Product từ org.bson.Document của MongoDB (Null-safe).
     *
     * @param doc Document chứa dữ liệu từ MongoDB
     * @return Đối tượng Product hoàn chỉnh hoặc null nếu doc rỗng
     */
    public static Product fromDocument(Document doc) {
        if (doc == null) {
            return null;
        }

        Product product = new Product();

        // Xử lý an toàn trường _id
        if (doc.containsKey("_id") && doc.get("_id") != null) {
            product.setId(doc.get("_id").toString());
        } else if (doc.containsKey("id") && doc.get("id") != null) {
            product.setId(doc.get("id").toString());
        }

        product.setCategoryId(doc.getString("categoryId"));
        product.setProductName(doc.getString("productName"));
        product.setBrand(doc.getString("brand"));
        product.setSpecifications(doc.getString("specifications"));

        // Xử lý basePrice an toàn với Number hoặc String
        if (doc.containsKey("basePrice") && doc.get("basePrice") != null) {
            Object priceObj = doc.get("basePrice");
            if (priceObj instanceof Number) {
                product.setBasePrice(((Number) priceObj).doubleValue());
            } else {
                try {
                    product.setBasePrice(Double.parseDouble(priceObj.toString()));
                } catch (NumberFormatException e) {
                    product.setBasePrice(0.0);
                }
            }
        } else {
            product.setBasePrice(0.0);
        }

        // Xử lý rentalRatePerDay an toàn với Number hoặc String
        if (doc.containsKey("rentalRatePerDay") && doc.get("rentalRatePerDay") != null) {
            Object rateObj = doc.get("rentalRatePerDay");
            if (rateObj instanceof Number) {
                product.setRentalRatePerDay(((Number) rateObj).doubleValue());
            } else {
                try {
                    product.setRentalRatePerDay(Double.parseDouble(rateObj.toString()));
                } catch (NumberFormatException e) {
                    product.setRentalRatePerDay(0.0);
                }
            }
        } else {
            product.setRentalRatePerDay(0.0);
        }

        // Xử lý active an toàn
        if (doc.containsKey("active") && doc.get("active") != null) {
            product.setActive(doc.getBoolean("active", true));
        } else {
            product.setActive(true);
        }

        return product;
    }

    @Override
    public String toString() {
        return "Product{" +
                "id='" + id + '\'' +
                ", categoryId='" + categoryId + '\'' +
                ", productName='" + productName + '\'' +
                ", brand='" + brand + '\'' +
                ", basePrice=" + basePrice +
                ", rentalRatePerDay=" + rentalRatePerDay +
                ", specifications='" + specifications + '\'' +
                ", active=" + active +
                '}';
    }
}
