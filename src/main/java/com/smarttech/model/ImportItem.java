package com.smarttech.model;

import org.bson.Document;

/**
 * Model đại diện cho từng đơn vị Thiết bị / Serial phần cứng cụ thể trong phiếu nhập kho.
 *
 * @author Senior Java Developer & Software Architect
 * @project SmartTech
 */
public class ImportItem {

    private String productId;       // Mã dòng sản phẩm liên kết
    private String productName;     // Tên sản phẩm hiển thị
    private String serialId;        // Mã Serial duy nhất của phần cứng
    private double importPrice;     // Đơn giá nhập kho (VNĐ)

    public ImportItem() {
    }

    public ImportItem(String productId, String productName, String serialId, double importPrice) {
        this.productId = productId;
        this.productName = productName;
        this.serialId = serialId != null ? serialId.trim().toUpperCase() : "";
        this.importPrice = importPrice;
    }

    public Document toDocument() {
        Document doc = new Document();
        doc.append("productId", productId != null ? productId : "")
                .append("productName", productName != null ? productName : "")
                .append("serialId", serialId != null ? serialId.toUpperCase() : "")
                .append("importPrice", importPrice);
        return doc;
    }

    public static ImportItem fromDocument(Document doc) {
        if (doc == null) return null;
        ImportItem item = new ImportItem();
        item.setProductId(doc.getString("productId"));
        item.setProductName(doc.getString("productName"));
        item.setSerialId(doc.getString("serialId"));
        item.setImportPrice(doc.getDouble("importPrice") != null ? doc.getDouble("importPrice") : 0.0);
        return item;
    }

    // ================= GETTERS & SETTERS =================

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

    public String getSerialId() {
        return serialId;
    }

    public void setSerialId(String serialId) {
        this.serialId = serialId != null ? serialId.trim().toUpperCase() : "";
    }

    public double getImportPrice() {
        return importPrice;
    }

    public void setImportPrice(double importPrice) {
        this.importPrice = importPrice;
    }
}
