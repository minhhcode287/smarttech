package com.smarttech.model;

import org.bson.Document;

/**
 * Lớp Customer đại diện cho khách hàng (mua hàng hoặc thuê thiết bị) trong hệ thống SmartTech.
 * Kế thừa các thuộc tính và hành vi cơ sở từ lớp User.
 *
 * @author Senior Java Developer &amp; Software Architect
 * @project SmartTech
 */
public class Customer extends User {

    private String address;          // Địa chỉ giao nhận/hộ khẩu khách hàng
    private String customerType;     // INDIVIDUAL (Cá nhân), ENTERPRISE (Doanh nghiệp)
    private int rewardPoints;        // Điểm tích lũy thành viên

    /**
     * Constructor mặc định không tham số.
     */
    public Customer() {
        super();
        this.role = "CUSTOMER";
        this.customerType = "INDIVIDUAL";
        this.rewardPoints = 0;
    }

    /**
     * Constructor tạo mới Customer (chưa có ID từ CSDL).
     */
    public Customer(String username, String password, String fullName, String phone,
                    String email, boolean active, String address, String customerType, int rewardPoints) {
        super(username, password, fullName, phone, email, "CUSTOMER", active);
        this.address = address;
        this.customerType = customerType != null ? customerType : "INDIVIDUAL";
        this.rewardPoints = rewardPoints;
    }

    /**
     * Constructor đầy đủ toàn bộ tham số bao gồm ID.
     */
    public Customer(String id, String username, String password, String fullName, String phone,
                    String email, String role, boolean active, String address, String customerType, int rewardPoints) {
        super(id, username, password, fullName, phone, email, role, active);
        this.address = address;
        this.customerType = customerType != null ? customerType : "INDIVIDUAL";
        this.rewardPoints = rewardPoints;
    }

    // ================= GETTERS & SETTERS =================

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getCustomerType() {
        return customerType;
    }

    public void setCustomerType(String customerType) {
        this.customerType = customerType;
    }

    public int getRewardPoints() {
        return rewardPoints;
    }

    public void setRewardPoints(int rewardPoints) {
        this.rewardPoints = rewardPoints;
    }

    // ================= MAPPING LOGIC (BSON DOCUMENT) =================

    /**
     * Chuyển đổi đối tượng Customer sang org.bson.Document để lưu trữ vào MongoDB.
     *
     * @return Document chứa toàn bộ thông tin của User và Customer
     */
    @Override
    public Document toDocument() {
        Document doc = new Document();
        // Điền các trường thuộc User
        populateBaseDocument(doc);

        // Điền các trường đặc thù của Customer
        doc.append("address", this.address != null ? this.address : "")
                .append("customerType", this.customerType != null ? this.customerType : "INDIVIDUAL")
                .append("rewardPoints", this.rewardPoints);

        return doc;
    }

    /**
     * Tạo đối tượng Customer từ org.bson.Document của MongoDB (Null-safe).
     *
     * @param doc Document chứa dữ liệu từ MongoDB
     * @return Đối tượng Customer hoàn chỉnh hoặc null nếu doc rỗng
     */
    public static Customer fromDocument(Document doc) {
        if (doc == null) {
            return null;
        }

        Customer customer = new Customer();
        // Nạp các trường cơ sở của User
        customer.loadBaseFromDocument(doc);

        // Nạp address
        customer.setAddress(doc.getString("address"));

        // Nạp customerType với giá trị mặc định là INDIVIDUAL
        String type = doc.getString("customerType");
        customer.setCustomerType(type != null ? type : "INDIVIDUAL");

        // Nạp rewardPoints an toàn với các kiểu số (Integer, Long, Double)
        if (doc.containsKey("rewardPoints") && doc.get("rewardPoints") != null) {
            Object pointsObj = doc.get("rewardPoints");
            if (pointsObj instanceof Number) {
                customer.setRewardPoints(((Number) pointsObj).intValue());
            } else {
                try {
                    customer.setRewardPoints(Integer.parseInt(pointsObj.toString()));
                } catch (NumberFormatException e) {
                    customer.setRewardPoints(0);
                }
            }
        } else {
            customer.setRewardPoints(0);
        }

        return customer;
    }

    @Override
    public String toString() {
        return "Customer{" +
                "id='" + id + '\'' +
                ", username='" + username + '\'' +
                ", fullName='" + fullName + '\'' +
                ", phone='" + phone + '\'' +
                ", email='" + email + '\'' +
                ", role='" + role + '\'' +
                ", active=" + active +
                ", address='" + address + '\'' +
                ", customerType='" + customerType + '\'' +
                ", rewardPoints=" + rewardPoints +
                '}';
    }
}
