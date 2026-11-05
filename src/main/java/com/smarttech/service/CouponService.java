package com.smarttech.service;

import com.smarttech.model.Coupon;
import com.smarttech.repository.CouponRepository;

import java.text.DecimalFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Random;

/**
 * Service xử lý toàn bộ logic nghiệp vụ Quản Lý Chương Trình Khuyến Mãi, Mã Giảm Giá & Voucher (CouponService).
 * Cung cấp chức năng thẩm định tính hợp lệ của mã (validateCoupon), tự động tính số tiền chiết khấu
 * và cập nhật số lượt sử dụng thực tế (useCoupon).
 *
 * @author Senior Java Developer & Software Architect
 * @project SmartTech
 */
public class CouponService {

    private final CouponRepository couponRepository;
    private static final DecimalFormat CURRENCY_FORMAT = new DecimalFormat("#,##0 VNĐ");
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private final Random random = new Random();

    public CouponService() {
        this.couponRepository = new CouponRepository();
    }

    public CouponService(CouponRepository couponRepository) {
        this.couponRepository = couponRepository;
    }

    /**
     * Lớp đóng gói kết quả thẩm định mã giảm giá (CouponValidationResult).
     */
    public static class CouponValidationResult {
        private boolean valid;
        private double discountAmount;
        private String message;
        private Coupon coupon;

        public CouponValidationResult() {
            this.valid = false;
            this.discountAmount = 0.0;
            this.message = "";
            this.coupon = null;
        }

        public CouponValidationResult(boolean valid, double discountAmount, String message, Coupon coupon) {
            this.valid = valid;
            this.discountAmount = discountAmount;
            this.message = message;
            this.coupon = coupon;
        }

        public boolean isValid() {
            return valid;
        }

        public void setValid(boolean valid) {
            this.valid = valid;
        }

        public double getDiscountAmount() {
            return discountAmount;
        }

        public void setDiscountAmount(double discountAmount) {
            this.discountAmount = discountAmount;
        }

        public String getMessage() {
            return message;
        }

        public void setMessage(String message) {
            this.message = message;
        }

        public Coupon getCoupon() {
            return coupon;
        }

        public void setCoupon(Coupon coupon) {
            this.coupon = coupon;
        }

        @Override
        public String toString() {
            return "CouponValidationResult{" +
                    "valid=" + valid +
                    ", discountAmount=" + discountAmount +
                    ", message='" + message + '\'' +
                    '}';
        }
    }

    /**
     * Thẩm định tính hợp lệ của mã giảm giá khi khách hàng áp dụng tại quầy POS bán hàng hoặc cho thuê.
     *
     * @param code Mã voucher (không phân biệt hoa thường)
     * @param orderTotal Tổng tiền giá trị đơn hàng hiện tại
     * @return Đối tượng CouponValidationResult chứa trạng thái, số tiền được giảm và thông điệp chi tiết
     */
    public CouponValidationResult validateCoupon(String code, double orderTotal) {
        if (code == null || code.trim().isEmpty()) {
            return new CouponValidationResult(false, 0.0, "Vui lòng nhập mã giảm giá!", null);
        }

        String normalizedCode = code.trim().toUpperCase();
        Coupon coupon = couponRepository.findByCode(normalizedCode);

        // 1. Kiểm tra tồn tại mã
        if (coupon == null) {
            return new CouponValidationResult(false, 0.0, "Mã giảm giá [" + normalizedCode + "] không tồn tại trên hệ thống!", null);
        }

        // 2. Kiểm tra trạng thái kích hoạt của mã
        if (Coupon.STATUS_DISABLED.equalsIgnoreCase(coupon.getStatus())) {
            return new CouponValidationResult(false, 0.0, "Mã giảm giá [" + normalizedCode + "] đang tạm thời bị khóa bởi Ban Quản Trị!", coupon);
        }

        if (Coupon.STATUS_EXPIRED.equalsIgnoreCase(coupon.getStatus())) {
            return new CouponValidationResult(false, 0.0, "Mã giảm giá [" + normalizedCode + "] đã hết hiệu lực sử dụng!", coupon);
        }

        // 3. Kiểm tra khoảng thời gian hiệu lực
        LocalDate today = LocalDate.now();
        if (coupon.getStartDate() != null && today.isBefore(coupon.getStartDate())) {
            return new CouponValidationResult(false, 0.0,
                    "Chương trình khuyến mãi [" + coupon.getTitle() + "] chưa bắt đầu (Có hiệu lực từ ngày " + coupon.getStartDate().format(DATE_FORMATTER) + ")!",
                    coupon);
        }

        if (coupon.getEndDate() != null && today.isAfter(coupon.getEndDate())) {
            // Tự động chuyển status sang EXPIRED nếu đã quá ngày
            coupon.setStatus(Coupon.STATUS_EXPIRED);
            couponRepository.update(coupon);
            return new CouponValidationResult(false, 0.0,
                    "Mã giảm giá [" + normalizedCode + "] đã hết hạn vào ngày " + coupon.getEndDate().format(DATE_FORMATTER) + "!",
                    coupon);
        }

        // 4. Kiểm tra giới hạn số lượt sử dụng
        if (coupon.getUsedCount() >= coupon.getUsageLimit()) {
            coupon.setStatus(Coupon.STATUS_EXPIRED);
            couponRepository.update(coupon);
            return new CouponValidationResult(false, 0.0,
                    "Mã giảm giá [" + normalizedCode + "] đã hết lượt sử dụng (" + coupon.getUsedCount() + "/" + coupon.getUsageLimit() + " lượt)!",
                    coupon);
        }

        // 5. Kiểm tra điều kiện đơn hàng tối thiểu
        if (orderTotal < coupon.getMinOrderAmount()) {
            return new CouponValidationResult(false, 0.0,
                    "Đơn hàng chưa đạt giá trị tối thiểu " + CURRENCY_FORMAT.format(coupon.getMinOrderAmount()) + " để áp dụng mã này (Hiện tại: " + CURRENCY_FORMAT.format(orderTotal) + ")!",
                    coupon);
        }

        // 6. Tính toán số tiền chiết khấu thực tế
        double discountAmount = coupon.calculateDiscount(orderTotal);
        String discountDesc = Coupon.TYPE_PERCENT.equalsIgnoreCase(coupon.getDiscountType())
                ? (int) coupon.getDiscountValue() + "% (Tối đa " + CURRENCY_FORMAT.format(coupon.getMaxDiscountAmount()) + ")"
                : CURRENCY_FORMAT.format(coupon.getDiscountValue());

        String successMsg = "Áp dụng mã thành công! Bạn được giảm: " + CURRENCY_FORMAT.format(discountAmount) + " (" + discountDesc + ").";

        return new CouponValidationResult(true, discountAmount, successMsg, coupon);
    }

    /**
     * Ghi nhận việc sử dụng mã khi hoàn tất thanh toán đơn hàng.
     * Tăng số lượt usedCount lên 1. Nếu usedCount >= usageLimit thì tự động chuyển sang EXPIRED.
     *
     * @param code Mã giảm giá đã sử dụng
     * @return true nếu ghi nhận thành công
     */
    public boolean useCoupon(String code) {
        if (code == null || code.trim().isEmpty()) {
            return false;
        }
        return couponRepository.incrementUsedCount(code.trim().toUpperCase());
    }

    /**
     * Lấy danh sách các mã giảm giá đang còn hiệu lực kinh doanh.
     *
     * @return Danh sách coupon ACTIVE và còn hạn
     */
    public List<Coupon> getActiveCoupons() {
        return couponRepository.findActiveCoupons();
    }

    /**
     * Lấy toàn bộ danh sách mã giảm giá trong hệ thống.
     *
     * @return Danh sách toàn bộ coupon
     */
    public List<Coupon> getAllCoupons() {
        return couponRepository.findAll();
    }

    /**
     * Tạo mới mã khuyến mãi với xác thực nghiệp vụ chặt chẽ.
     *
     * @param coupon Thông tin mã cần tạo
     * @return true nếu tạo thành công
     */
    public boolean createCoupon(Coupon coupon) {
        if (coupon == null || coupon.getCode() == null || coupon.getCode().trim().isEmpty()) {
            return false;
        }

        String normalizedCode = coupon.getCode().trim().toUpperCase().replaceAll("\\s+", "");
        coupon.setCode(normalizedCode);

        // Kiểm tra trùng lặp mã code
        if (couponRepository.findByCode(normalizedCode) != null) {
            System.err.println("[CouponService] Mã coupon [" + normalizedCode + "] đã tồn tại!");
            return false;
        }

        // Tự động gán ngày nếu chưa có
        if (coupon.getStartDate() == null) {
            coupon.setStartDate(LocalDate.now());
        }
        if (coupon.getEndDate() == null) {
            coupon.setEndDate(LocalDate.now().plusMonths(1));
        }
        if (coupon.getStatus() == null || coupon.getStatus().trim().isEmpty()) {
            coupon.setStatus(Coupon.STATUS_ACTIVE);
        }

        return couponRepository.save(coupon);
    }

    /**
     * Cập nhật thông tin chương trình khuyến mãi.
     */
    public boolean updateCoupon(Coupon coupon) {
        if (coupon == null || coupon.getCode() == null) {
            return false;
        }
        return couponRepository.update(coupon);
    }

    /**
     * Xóa mã khuyến mãi.
     */
    public boolean deleteCoupon(String id) {
        return couponRepository.delete(id);
    }

    /**
     * Bật hoặc tắt trạng thái kích hoạt của mã (ACTIVE <-> DISABLED).
     *
     * @param code Mã giảm giá cần đổi trạng thái
     * @return true nếu cập nhật thành công
     */
    public boolean toggleCouponStatus(String code) {
        if (code == null || code.trim().isEmpty()) return false;
        Coupon coupon = couponRepository.findByCode(code.trim().toUpperCase());
        if (coupon == null) return false;

        if (Coupon.STATUS_ACTIVE.equalsIgnoreCase(coupon.getStatus())) {
            coupon.setStatus(Coupon.STATUS_DISABLED);
        } else {
            coupon.setStatus(Coupon.STATUS_ACTIVE);
        }

        return couponRepository.update(coupon);
    }

    /**
     * Tìm kiếm mã coupon theo từ khóa.
     */
    public List<Coupon> searchCoupons(String keyword) {
        return couponRepository.search(keyword);
    }

    /**
     * Tìm mã coupon theo Code.
     */
    public Coupon getCouponByCode(String code) {
        if (code == null || code.trim().isEmpty()) return null;
        return couponRepository.findByCode(code.trim().toUpperCase());
    }

    /**
     * Sinh mã voucher ngẫu nhiên tiện lợi cho Admin khi phát hành đợt ưu đãi mới.
     * Ví dụ: SMART2026, VIP7482, SUMMER8291
     *
     * @param prefix Tiền tố mã (ví dụ: "SMART", "VIP", "TECH")
     * @return Mã voucher ngẫu nhiên duy nhất
     */
    public String generateRandomCouponCode(String prefix) {
        String basePrefix = (prefix != null && !prefix.trim().isEmpty()) ? prefix.trim().toUpperCase() : "SMART";
        for (int i = 0; i < 50; i++) {
            int randomNum = 1000 + random.nextInt(9000);
            String candidate = basePrefix + randomNum;
            if (couponRepository.findByCode(candidate) == null) {
                return candidate;
            }
        }
        return basePrefix + System.currentTimeMillis() % 10000;
    }
}