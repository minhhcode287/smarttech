package com.smarttech.model;

import org.bson.Document;

/**
 * Chi tiết từng món hàng (thiết bị) trong một đơn hàng bán lẻ.
 *
 * @author Senior Java Developer &amp; Software Architect
 * @project SmartTech
 */
public class OrderItem {

    private String productId;
    private String productName;
    private String serialId;       // Mã Serial phần cứng duy nhất của máy được bán
    private int quantity;
    private double unitPrice;
    private double subtotal;

    public OrderItem() {
        this.quantity = 1;
    }

    public OrderItem(String productId, String productName, String serialId, int quantity, double unitPrice) {
        this.productId = productId;
        this.productName = productName;
        this.serialId = serialId;
        this.quantity = quantity;
        this.unitPrice = unitPrice;
        this.subtotal = unitPrice * quantity;
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

    public String getSerialId() {
        return serialId;
    }

    public void setSerialId(String serialId) {
        this.serialId = serialId;
    }

    public int getQuantity() {
        return quantity;
    }

    public void setQuantity(int quantity) {
        this.quantity = quantity;
        this.subtotal = this.unitPrice * quantity;
    }

    public double getUnitPrice() {
        return unitPrice;
    }

    public void setUnitPrice(double unitPrice) {
        this.unitPrice = unitPrice;
        this.subtotal = unitPrice * this.quantity;
    }

    public double getSubtotal() {
        return subtotal;
    }

    public void setSubtotal(double subtotal) {
        this.subtotal = subtotal;
    }

    public Document toDocument() {
        return new Document()
                .append("productId", productId != null ? productId : "")
                .append("productName", productName != null ? productName : "")
                .append("serialId", serialId != null ? serialId : "")
                .append("quantity", quantity)
                .append("unitPrice", unitPrice)
                .append("subtotal", subtotal);
    }

    public static OrderItem fromDocument(Document doc) {
        if (doc == null) return null;
        OrderItem item = new OrderItem();
        item.setProductId(doc.getString("productId"));
        item.setProductName(doc.getString("productName"));
        item.setSerialId(doc.getString("serialId"));

        Number q = doc.get("quantity", Number.class);
        item.setQuantity(q != null ? q.intValue() : 1);

        Number p = doc.get("unitPrice", Number.class);
        item.setUnitPrice(p != null ? p.doubleValue() : 0.0);

        Number s = doc.get("subtotal", Number.class);
        item.setSubtotal(s != null ? s.doubleValue() : 0.0);

        return item;
    }
}
