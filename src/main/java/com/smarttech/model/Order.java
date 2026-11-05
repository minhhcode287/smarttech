package com.smarttech.model;

import org.bson.Document;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Lớp Order đại diện cho Hóa đơn bán lẻ thiết bị công nghệ trong hệ thống SmartTech.
 *
 * @author Senior Java Developer &amp; Software Architect
 * @project SmartTech
 */
public class Order {

    private String id;                 // Mã đơn hàng (Khoá chính, _id trong MongoDB)
    private String customerId;         // Mã khách hàng
    private String customerName;       // Họ tên khách hàng
    private String customerPhone;      // Số điện thoại khách hàng
    private String employeeId;         // Mã nhân viên lập đơn
    private LocalDate orderDate;       // Ngày lập đơn hàng
    private List<OrderItem> items;     // Danh sách các thiết bị trong đơn
    private double totalAmount;        // Tổng tiền hàng trước chiết khấu
    private int pointsUsed;            // Số điểm thưởng khách hàng sử dụng để giảm giá
    private double discountAmount;     // Số tiền được giảm giá (Quy đổi từ điểm thưởng)
    private double finalAmount;        // Số tiền thực thu cuối cùng
    private String paymentMethod;      // CASH (Tiền mặt), BANK_TRANSFER (Chuyển khoản)
    private String status;             // PAID (Đã thanh toán), CANCELLED (Đã hủy)

    public Order() {
        this.items = new ArrayList<>();
        this.orderDate = LocalDate.now();
        this.status = "PAID";
        this.paymentMethod = "CASH";
    }

    public Order(String id, String customerId, String customerName, String customerPhone,
                 String employeeId, LocalDate orderDate, List<OrderItem> items, double totalAmount,
                 int pointsUsed, double discountAmount, double finalAmount, String paymentMethod, String status) {
        this.id = id;
        this.customerId = customerId;
        this.customerName = customerName;
        this.customerPhone = customerPhone;
        this.employeeId = employeeId;
        this.orderDate = orderDate != null ? orderDate : LocalDate.now();
        this.items = items != null ? items : new ArrayList<>();
        this.totalAmount = totalAmount;
        this.pointsUsed = pointsUsed;
        this.discountAmount = discountAmount;
        this.finalAmount = finalAmount;
        this.paymentMethod = paymentMethod;
        this.status = status;
    }

    // ================= GETTERS & SETTERS =================

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getCustomerId() {
        return customerId;
    }

    public void setCustomerId(String customerId) {
        this.customerId = customerId;
    }

    public String getCustomerName() {
        return customerName;
    }

    public void setCustomerName(String customerName) {
        this.customerName = customerName;
    }

    public String getCustomerPhone() {
        return customerPhone;
    }

    public void setCustomerPhone(String customerPhone) {
        this.customerPhone = customerPhone;
    }

    public String getEmployeeId() {
        return employeeId;
    }

    public void setEmployeeId(String employeeId) {
        this.employeeId = employeeId;
    }

    public LocalDate getOrderDate() {
        return orderDate;
    }

    public void setOrderDate(LocalDate orderDate) {
        this.orderDate = orderDate;
    }

    public List<OrderItem> getItems() {
        return items;
    }

    public void setItems(List<OrderItem> items) {
        this.items = items;
    }

    public double getTotalAmount() {
        return totalAmount;
    }

    public void setTotalAmount(double totalAmount) {
        this.totalAmount = totalAmount;
    }

    public int getPointsUsed() {
        return pointsUsed;
    }

    public void setPointsUsed(int pointsUsed) {
        this.pointsUsed = pointsUsed;
    }

    public double getDiscountAmount() {
        return discountAmount;
    }

    public void setDiscountAmount(double discountAmount) {
        this.discountAmount = discountAmount;
    }

    public double getFinalAmount() {
        return finalAmount;
    }

    public void setFinalAmount(double finalAmount) {
        this.finalAmount = finalAmount;
    }

    public String getPaymentMethod() {
        return paymentMethod;
    }

    public void setPaymentMethod(String paymentMethod) {
        this.paymentMethod = paymentMethod;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    // ================= MAPPING LOGIC =================

    public Document toDocument() {
        Document doc = new Document();
        if (this.id != null && !this.id.trim().isEmpty()) {
            doc.append("_id", this.id);
        }
        doc.append("customerId", this.customerId != null ? this.customerId : "")
                .append("customerName", this.customerName != null ? this.customerName : "")
                .append("customerPhone", this.customerPhone != null ? this.customerPhone : "")
                .append("employeeId", this.employeeId != null ? this.employeeId : "")
                .append("orderDate", this.orderDate != null ? this.orderDate.toString() : LocalDate.now().toString())
                .append("totalAmount", this.totalAmount)
                .append("pointsUsed", this.pointsUsed)
                .append("discountAmount", this.discountAmount)
                .append("finalAmount", this.finalAmount)
                .append("paymentMethod", this.paymentMethod != null ? this.paymentMethod : "CASH")
                .append("status", this.status != null ? this.status : "PAID");

        List<Document> itemDocs = new ArrayList<>();
        if (this.items != null) {
            for (OrderItem item : this.items) {
                itemDocs.add(item.toDocument());
            }
        }
        doc.append("items", itemDocs);

        return doc;
    }

    public static Order fromDocument(Document doc) {
        if (doc == null) return null;

        Order order = new Order();
        if (doc.containsKey("_id") && doc.get("_id") != null) {
            order.setId(doc.get("_id").toString());
        }

        order.setCustomerId(doc.getString("customerId"));
        order.setCustomerName(doc.getString("customerName"));
        order.setCustomerPhone(doc.getString("customerPhone"));
        order.setEmployeeId(doc.getString("employeeId"));

        String dateStr = doc.getString("orderDate");
        if (dateStr != null) {
            try {
                order.setOrderDate(LocalDate.parse(dateStr));
            } catch (Exception e) {
                order.setOrderDate(LocalDate.now());
            }
        }

        Number tot = doc.get("totalAmount", Number.class);
        order.setTotalAmount(tot != null ? tot.doubleValue() : 0.0);

        Number pu = doc.get("pointsUsed", Number.class);
        order.setPointsUsed(pu != null ? pu.intValue() : 0);

        Number disc = doc.get("discountAmount", Number.class);
        order.setDiscountAmount(disc != null ? disc.doubleValue() : 0.0);

        Number fin = doc.get("finalAmount", Number.class);
        order.setFinalAmount(fin != null ? fin.doubleValue() : 0.0);

        order.setPaymentMethod(doc.getString("paymentMethod"));
        order.setStatus(doc.getString("status"));

        List<OrderItem> list = new ArrayList<>();
        List<Document> itemDocs = doc.getList("items", Document.class);
        if (itemDocs != null) {
            for (Document d : itemDocs) {
                list.add(OrderItem.fromDocument(d));
            }
        }
        order.setItems(list);

        return order;
    }
}
