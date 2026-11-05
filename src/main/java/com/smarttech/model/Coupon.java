package com.smarttech.model;

import org.bson.Document;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

/**
 * Model Coupon đại diện cho Chương trình khuyến mãi, Voucher & Mã giảm giá trong hệ thống SmartTech.
 * Hỗ trợ 2 hình thức giảm giá: Giảm theo tỷ lệ phần trăm (PERCENT) và Giảm số tiền cố định (FIXED_AMOUNT).
 *
 * @author Senior Java Developer & Software Architect
 * @project SmartTech
 */
public class Coupon {

    // Loại hình chiết khấu
    public static final String TYPE_PERCENT = "PERCENT";               // Giảm theo % giá trị đơn
    public static final String TYPE_FIXED_AMOUNT = "FIXED_AMOUNT";     // Giảm tiền mặt cố định (VNĐ)

    // Trạng thái vòng đời của mã khuyến mãi
    public static final String STATUS_ACTIVE = "ACTIVE";               // Đang hoạt động / Khả dụng
    public static final String STATUS_EXPIRED = "EXPIRED";             // Đã hết hạn hoặc hết lượt dùng
    public static final String STATUS_DISABLED = "DISABLED";           // Tạm dừng / Bị vô hiệu hóa bởi Admin

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE;

    private String id;                       // Khóa chính (_id trong MongoDB)
    private String code;                     // Mã voucher (ví dụ: "SUMMER2026", "VIPPRO50K" - Viết hoa, không dấu)
    private String title;                    // Tên chương trình (ví dụ: "Tri ân khách hàng Hè 2026")
    private String discountType;             // "PERCENT" hoặc "FIXED_AMOUNT"
    private double discountValue;            // Giá trị giảm: 10%, 15% hoặc 50,000 VNĐ
    private double minOrderAmount;           // Giá trị đơn hàng tối thiểu để áp dụng mã (VNĐ)
    private double maxDiscountAmount;        // Mức giảm tối đa VNĐ đối với loại giảm %
    private LocalDate startDate;             // Ngày bắt đầu hiệu lực
    private LocalDate endDate;               // Ngày hết hạn
    private int usageLimit;                  // Tổng số lượt sử dụng tối đa (ví dụ: 100 lượt)
    private int usedCount;                   // Số lượt đã được áp dụng thực tế
    private String status;                   // "ACTIVE", "EXPIRED", "DISABLED"

    /**
     * Constructor mặc định không tham số.
     */
    public Coupon() {
        this.id = "CPN-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        this.title = "Chương trình ưu đãi SmartTech";
        this.discountType = TYPE_PERCENT;
        this.discountValue = 10.0;
        this.minOrderAmount = 500000.0;
        this.maxDiscountAmount = 500000.0;
        this.startDate = LocalDate.now();
        this.endDate = LocalDate.now().plusMonths(1);
        this.usageLimit = 100;
        this.usedCount = 0;
        this.status = STATUS_ACTIVE;
    }

    /**
     * Constructor đầy đủ tham số (Full-args).
     */
    public Coupon(String id, String code, String title, String discountType, double discountValue,
                  double minOrderAmount, double maxDiscountAmount, LocalDate startDate,
                  LocalDate endDate, int usageLimit, int usedCount, String status) {
        this.id = (id != null && !id.trim().isEmpty()) ? id.trim() : (code != null ? code.trim().toUpperCase() : "CPN-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase());
        this.code = code != null ? code.trim().toUpperCase().replaceAll("\\s+", "") : "";
        this.title = title != null ? title.trim() : "Ưu đãi khách hàng";
        this.discountType = discountType != null ? discountType.trim().toUpperCase() : TYPE_PERCENT;
        this.discountValue = Math.max(0.0, discountValue);
        this.minOrderAmount = Math.max(0.0, minOrderAmount);
        this.maxDiscountAmount = Math.max(0.0, maxDiscountAmount);
        this.startDate = startDate != null ? startDate : LocalDate.now();
        this.endDate = endDate != null ? endDate : LocalDate.now().plusMonths(1);
        this.usageLimit = Math.max(1, usageLimit);
        this.usedCount = Math.max(0, usedCount);
        this.status = status != null ? status.trim().toUpperCase() : STATUS_ACTIVE;
    }

    /**
     * Constructor tạo nhanh khi phát hành voucher mới.
     */
    public Coupon(String code, String title, String discountType, double discountValue,
                  double minOrderAmount, double maxDiscountAmount, LocalDate startDate,
                  LocalDate endDate, int usageLimit) {
        this(null, code, title, discountType, discountValue, minOrderAmount, maxDiscountAmount, startDate, endDate, usageLimit, 0, STATUS_ACTIVE);
    }

    /**
     * Constructor tương thích ngược với phiên bản trước.
     */
    public Coupon(String code, double discountPercent, double maxDiscountAmount,
                  double minOrderAmount, LocalDate expirationDate, int usageLimit) {
        this(null, code, "Chương trình ưu đãi " + code, TYPE_PERCENT, discountPercent,
                minOrderAmount, maxDiscountAmount, LocalDate.now(), expirationDate, usageLimit, 0, STATUS_ACTIVE);
    }

    // ================= GETTERS & SETTERS =================

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code != null ? code.trim().toUpperCase().replaceAll("\\s+", "") : "";
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDiscountType() {
        return discountType;
    }

    public void setDiscountType(String discountType) {
        this.discountType = discountType != null ? discountType.trim().toUpperCase() : TYPE_PERCENT;
    }

    public double getDiscountValue() {
        return discountValue;
    }

    public void setDiscountValue(double discountValue) {
        this.discountValue = Math.max(0.0, discountValue);
    }

    public double getMinOrderAmount() {
        return minOrderAmount;
    }

    public void setMinOrderAmount(double minOrderAmount) {
        this.minOrderAmount = Math.max(0.0, minOrderAmount);
    }

    public double getMaxDiscountAmount() {
        return maxDiscountAmount;
    }

    public void setMaxDiscountAmount(double maxDiscountAmount) {
        this.maxDiscountAmount = Math.max(0.0, maxDiscountAmount);
    }

    public LocalDate getStartDate() {
        return startDate;
    }

    public void setStartDate(LocalDate startDate) {
        this.startDate = startDate;
    }

    public LocalDate getEndDate() {
        return endDate;
    }

    public void setEndDate(LocalDate endDate) {
        this.endDate = endDate;
    }

    public int getUsageLimit() {
        return usageLimit;
    }

    public void setUsageLimit(int usageLimit) {
        this.usageLimit = Math.max(1, usageLimit);
    }

    public int getUsedCount() {
        return usedCount;
    }

    public void setUsedCount(int usedCount) {
        this.usedCount = Math.max(0, usedCount);
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status != null ? status.trim().toUpperCase() : STATUS_ACTIVE;
    }

    // ================= TƯƠNG THÍCH NGƯỢC (LEGACY GETTERS/SETTERS) =================

    public double getDiscountPercent() {
        return TYPE_PERCENT.equalsIgnoreCase(discountType) ? discountValue : 0.0;
    }

    public void setDiscountPercent(double discountPercent) {
        this.discountValue = discountPercent;
        this.discountType = TYPE_PERCENT;
    }

    public LocalDate getExpirationDate() {
        return endDate;
    }

    public void setExpirationDate(LocalDate expirationDate) {
        this.endDate = expirationDate;
    }

    public boolean isActive() {
        return STATUS_ACTIVE.equalsIgnoreCase(status);
    }

    public void setActive(boolean active) {
        this.status = active ? STATUS_ACTIVE : STATUS_DISABLED;
    }

    // ================= TIỆN ÍCH NGHIỆP VỤ (BUSINESS LOGIC) =================

    /**
     * Kiểm tra tính hợp lệ của mã giảm giá đối với giá trị đơn hàng cụ thể.
     * Thỏa mãn:
     * 1. Trạng thái mã là "ACTIVE"
     * 2. Thời điểm hiện tại nằm trong khoảng [startDate, endDate]
     * 3. Số lượt đã dùng < Giới hạn lượt dùng (usedCount < usageLimit)
     * 4. Giá trị đơn hàng đạt mức tối thiểu (orderAmount >= minOrderAmount)
     *
     * @param orderAmount Giá trị đơn hàng cần áp dụng
     * @return true nếu mã hoàn toàn hợp lệ
     */
    public boolean isValid(double orderAmount) {
        if (!STATUS_ACTIVE.equalsIgnoreCase(this.status)) {
            return false;
        }

        LocalDate today = LocalDate.now();
        if (startDate != null && today.isBefore(startDate)) {
            return false;
        }
        if (endDate != null && today.isAfter(endDate)) {
            return false;
        }

        if (usedCount >= usageLimit) {
            return false;
        }

        return orderAmount >= minOrderAmount;
    }

    /**
     * Overload kiểm tra mã còn khả dụng tổng thể (không ràng buộc giá trị đơn).
     *
     * @return true nếu mã còn hoạt động, trong hạn và còn lượt dùng
     */
    public boolean isValid() {
        return isValid(minOrderAmount);
    }

    /**
     * Tính toán số tiền thực tế được giảm giá cho đơn hàng.
     *
     * @param orderAmount Giá trị đơn hàng
     * @return Số tiền được chiết khấu (VNĐ), trả về 0 nếu mã không hợp lệ
     */
    public double calculateDiscount(double orderAmount) {
        if (!isValid(orderAmount)) {
            return 0.0;
        }

        if (TYPE_FIXED_AMOUNT.equalsIgnoreCase(discountType)) {
            // Giảm số tiền cố định (không vượt quá tổng giá trị đơn)
            return Math.min(discountValue, orderAmount);
        } else {
            // Giảm theo % giá trị đơn hàng
            double calculated = orderAmount * (discountValue / 100.0);
            if (maxDiscountAmount > 0) {
                calculated = Math.min(calculated, maxDiscountAmount);
            }
            return calculated;
        }
    }

    // ================= MAPPING LOGIC (BSON DOCUMENT) =================

    /**
     * Chuyển đổi thực thể Coupon sang BSON Document để lưu trữ vào MongoDB collection "coupons".
     */
    public Document toDocument() {
        Document doc = new Document();
        if (this.id != null && !this.id.trim().isEmpty()) {
            doc.append("_id", this.id);
        } else {
            this.id = this.code != null ? this.code : "CPN-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
            doc.append("_id", this.id);
        }

        doc.append("code", this.code != null ? this.code : "")
                .append("title", this.title != null ? this.title : "Ưu đãi khách hàng")
                .append("discountType", this.discountType != null ? this.discountType : TYPE_PERCENT)
                .append("discountValue", this.discountValue)
                .append("minOrderAmount", this.minOrderAmount)
                .append("maxDiscountAmount", this.maxDiscountAmount)
                .append("startDate", this.startDate != null ? this.startDate.format(DATE_FORMATTER) : LocalDate.now().format(DATE_FORMATTER))
                .append("endDate", this.endDate != null ? this.endDate.format(DATE_FORMATTER) : LocalDate.now().plusMonths(1).format(DATE_FORMATTER))
                .append("usageLimit", this.usageLimit)
                .append("usedCount", this.usedCount)
                .append("status", this.status != null ? this.status : STATUS_ACTIVE)
                // Hỗ trợ trường legacy
                .append("discountPercent", getDiscountPercent())
                .append("expirationDate", this.endDate != null ? this.endDate.format(DATE_FORMATTER) : "")
                .append("active", isActive());

        return doc;
    }

    /**
     * Tạo đối tượng Coupon từ BSON Document của MongoDB (Null-safe & Tương thích ngược).
     */
    public static Coupon fromDocument(Document doc) {
        if (doc == null) {
            return null;
        }

        Coupon coupon = new Coupon();

        // Xử lý ID
        if (doc.containsKey("_id") && doc.get("_id") != null) {
            coupon.setId(doc.get("_id").toString());
        } else if (doc.containsKey("id") && doc.get("id") != null) {
            coupon.setId(doc.get("id").toString());
        }

        coupon.setCode(doc.getString("code"));
        coupon.setTitle(doc.getString("title") != null ? doc.getString("title") : "Chương trình ưu đãi " + coupon.getCode());

        // Loại giảm giá
        String type = doc.getString("discountType");
        coupon.setDiscountType(type != null ? type : TYPE_PERCENT);

        // Giá trị giảm giá
        if (doc.containsKey("discountValue") && doc.get("discountValue") != null) {
            Number val = doc.get("discountValue", Number.class);
            coupon.setDiscountValue(val != null ? val.doubleValue() : 0.0);
        } else if (doc.containsKey("discountPercent") && doc.get("discountPercent") != null) {
            Number percent = doc.get("discountPercent", Number.class);
            coupon.setDiscountValue(percent != null ? percent.doubleValue() : 0.0);
            coupon.setDiscountType(TYPE_PERCENT);
        }

        Number minOrder = doc.get("minOrderAmount", Number.class);
        coupon.setMinOrderAmount(minOrder != null ? minOrder.doubleValue() : 0.0);

        Number maxDisc = doc.get("maxDiscountAmount", Number.class);
        coupon.setMaxDiscountAmount(maxDisc != null ? maxDisc.doubleValue() : 0.0);

        // Ngày bắt đầu
        String startStr = doc.getString("startDate");
        if (startStr != null && !startStr.trim().isEmpty()) {
            try {
                coupon.setStartDate(LocalDate.parse(startStr));
            } catch (Exception e) {
                coupon.setStartDate(LocalDate.now());
            }
        } else {
            coupon.setStartDate(LocalDate.now());
        }

        // Ngày hết hạn
        String endStr = doc.getString("endDate");
        if (endStr == null || endStr.trim().isEmpty()) {
            endStr = doc.getString("expirationDate");
        }
        if (endStr != null && !endStr.trim().isEmpty()) {
            try {
                coupon.setEndDate(LocalDate.parse(endStr));
            } catch (Exception e) {
                coupon.setEndDate(LocalDate.now().plusMonths(1));
            }
        } else {
            coupon.setEndDate(LocalDate.now().plusMonths(1));
        }

        Number limit = doc.get("usageLimit", Number.class);
        coupon.setUsageLimit(limit != null ? limit.intValue() : 100);

        Number used = doc.get("usedCount", Number.class);
        coupon.setUsedCount(used != null ? used.intValue() : 0);

        // Trạng thái
        String st = doc.getString("status");
        if (st != null && !st.trim().isEmpty()) {
            coupon.setStatus(st);
        } else {
            Boolean act = doc.getBoolean("active");
            coupon.setStatus((act == null || act) ? STATUS_ACTIVE : STATUS_DISABLED);
        }

        return coupon;
    }

    @Override
    public String toString() {
        return "Coupon{" +
                "code='" + code + '\'' +
                ", title='" + title + '\'' +
                ", discountType='" + discountType + '\'' +
                ", discountValue=" + discountValue +
                ", minOrderAmount=" + minOrderAmount +
                ", maxDiscountAmount=" + maxDiscountAmount +
                ", startDate=" + startDate +
                ", endDate=" + endDate +
                ", usage=" + usedCount + "/" + usageLimit +
                ", status='" + status + '\'' +
                '}';
    }
}