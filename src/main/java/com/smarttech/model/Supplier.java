package com.smarttech.model;

import org.bson.Document;
import org.bson.types.ObjectId;

/**
 * Model đại diện cho Nhà Cung Cấp (Supplier) cung ứng thiết bị phần cứng cho hệ thống SmartTech.
 *
 * @author Senior Java Developer & Software Architect
 * @project SmartTech
 */
public class Supplier {

    public static final String STATUS_ACTIVE = "ACTIVE";
    public static final String STATUS_INACTIVE = "INACTIVE";

    private String id;          // Mã NCC, ví dụ: "SUP-001" (hoặc ObjectId hex)
    private String name;        // Tên Công ty / Nhà cung cấp
    private String phone;       // Số điện thoại liên hệ
    private String email;       // Email giao dịch
    private String address;     // Địa chỉ trụ sở công ty
    private String taxCode;     // Mã số thuế doanh nghiệp
    private String status;      // Trạng thái: "ACTIVE", "INACTIVE"

    public Supplier() {
        this.status = STATUS_ACTIVE;
    }

    public Supplier(String id, String name, String phone, String email, String address, String taxCode, String status) {
        this.id = id;
        this.name = name;
        this.phone = phone;
        this.email = email;
        this.address = address;
        this.taxCode = taxCode;
        this.status = status != null ? status : STATUS_ACTIVE;
    }

    public Supplier(String name, String phone, String email, String address, String taxCode, String status) {
        this.name = name;
        this.phone = phone;
        this.email = email;
        this.address = address;
        this.taxCode = taxCode;
        this.status = status != null ? status : STATUS_ACTIVE;
    }

    public Document toDocument() {
        Document doc = new Document();
        if (id != null && !id.trim().isEmpty()) {
            try {
                doc.append("_id", new ObjectId(id));
            } catch (Exception ex) {
                doc.append("_id", id);
            }
        }
        doc.append("name", name != null ? name.trim() : "")
                .append("phone", phone != null ? phone.trim() : "")
                .append("email", email != null ? email.trim() : "")
                .append("address", address != null ? address.trim() : "")
                .append("taxCode", taxCode != null ? taxCode.trim() : "")
                .append("status", status != null ? status : STATUS_ACTIVE);
        return doc;
    }

    public static Supplier fromDocument(Document doc) {
        if (doc == null) return null;
        Supplier s = new Supplier();
        Object rawId = doc.get("_id");
        s.setId(rawId != null ? rawId.toString() : null);
        s.setName(doc.getString("name"));
        s.setPhone(doc.getString("phone"));
        s.setEmail(doc.getString("email"));
        s.setAddress(doc.getString("address"));
        s.setTaxCode(doc.getString("taxCode"));
        s.setStatus(doc.getString("status") != null ? doc.getString("status") : STATUS_ACTIVE);
        return s;
    }

    // ================= GETTERS & SETTERS =================

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getTaxCode() {
        return taxCode;
    }

    public void setTaxCode(String taxCode) {
        this.taxCode = taxCode;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public boolean isActive() {
        return STATUS_ACTIVE.equalsIgnoreCase(status);
    }

    @Override
    public String toString() {
        return name + " (" + (phone != null ? phone : "Chưa có SĐT") + ")";
    }
}
