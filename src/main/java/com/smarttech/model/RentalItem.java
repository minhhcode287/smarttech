package com.smarttech.model;

import org.bson.Document;

/**
 * Chi tiết từng thiết bị cho thuê trong một Hợp đồng cho thuê.
 *
 * @author Senior Java Developer & Software Architect
 * @project SmartTech
 */
public class RentalItem {

    private String productId;
    private String productName;
    private String serialId;            // Mã Serial máy cụ thể được cho thuê
    private double rentalRatePerDay;    // Đơn giá thuê theo ngày
    private double depositAmount;       // Tiền cọc giữ máy của thiết bị này

    public RentalItem() {}

    public RentalItem(String productId, String productName, String serialId, double rentalRatePerDay, double depositAmount) {
        this.productId = productId;
        this.productName = productName;
        this.serialId = serialId;
        this.rentalRatePerDay = rentalRatePerDay;
        this.depositAmount = depositAmount;
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

    public double getRentalRatePerDay() {
        return rentalRatePerDay;
    }

    public void setRentalRatePerDay(double rentalRatePerDay) {
        this.rentalRatePerDay = rentalRatePerDay;
    }

    public double getDepositAmount() {
        return depositAmount;
    }

    public void setDepositAmount(double depositAmount) {
        this.depositAmount = depositAmount;
    }

    public Document toDocument() {
        return new Document()
                .append("productId", productId != null ? productId : "")
                .append("productName", productName != null ? productName : "")
                .append("serialId", serialId != null ? serialId : "")
                .append("rentalRatePerDay", rentalRatePerDay)
                .append("depositAmount", depositAmount);
    }

    public static RentalItem fromDocument(Document doc) {
        if (doc == null) return null;
        RentalItem item = new RentalItem();
        item.setProductId(doc.getString("productId"));
        item.setProductName(doc.getString("productName"));
        item.setSerialId(doc.getString("serialId"));

        Number rate = doc.get("rentalRatePerDay", Number.class);
        item.setRentalRatePerDay(rate != null ? rate.doubleValue() : 0.0);

        Number dep = doc.get("depositAmount", Number.class);
        item.setDepositAmount(dep != null ? dep.doubleValue() : 0.0);

        return item;
    }
}