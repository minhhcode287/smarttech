package com.smarttech.service;

import com.smarttech.model.Customer;
import com.smarttech.repository.CustomerRepository;

import java.util.List;

/**
 * Service xử lý các nghiệp vụ liên quan đến Khách hàng (Customer),
 * bao gồm quản lý hồ sơ, phân hạng khách hàng và tích lũy / quy đổi điểm thưởng (Reward Points).
 *
 * @author Senior Java Developer &amp; Software Architect
 * @project SmartTech
 */
public class CustomerService {

    // Tỷ lệ quy đổi: 100.000 VNĐ chi tiêu = 1 điểm thưởng
    public static final double SPEND_AMOUNT_PER_POINT = 100000.0;

    private final CustomerRepository customerRepository;

    public CustomerService() {
        this.customerRepository = new CustomerRepository();
    }

    public CustomerService(CustomerRepository customerRepository) {
        this.customerRepository = customerRepository;
    }

    public List<Customer> getAllCustomers() {
        return customerRepository.findAll();
    }

    public Customer getCustomerById(String id) {
        if (id == null || id.trim().isEmpty()) {
            return null;
        }
        return customerRepository.findById(id.trim());
    }

    public Customer getCustomerByPhone(String phone) {
        if (phone == null || phone.trim().isEmpty()) {
            return null;
        }
        return customerRepository.findByPhone(phone.trim());
    }

    /**
     * Đăng ký khách hàng mới với validation hợp lệ.
     *
     * @param customer Đối tượng khách hàng
     * @return true nếu thêm thành công
     */
    public boolean registerCustomer(Customer customer) {
        if (customer == null) {
            System.err.println("[CustomerService] Thông tin khách hàng không được để trống!");
            return false;
        }

        if (customer.getFullName() == null || customer.getFullName().trim().isEmpty()) {
            System.err.println("[CustomerService] Tên khách hàng không được để trống!");
            return false;
        }

        if (customer.getPhone() == null || customer.getPhone().trim().isEmpty()) {
            System.err.println("[CustomerService] Số điện thoại khách hàng không được để trống!");
            return false;
        }

        // Kiểm tra số điện thoại đã tồn tại chưa
        Customer existing = customerRepository.findByPhone(customer.getPhone().trim());
        if (existing != null) {
            System.err.println("[CustomerService] Số điện thoại '" + customer.getPhone() + "' đã được đăng ký trước đó!");
            return false;
        }

        return customerRepository.save(customer);
    }

    /**
     * Cập nhật thông tin khách hàng.
     *
     * @param customer Đối tượng khách hàng
     * @return true nếu thành công
     */
    public boolean updateCustomer(Customer customer) {
        if (customer == null || customer.getId() == null) {
            return false;
        }
        return customerRepository.update(customer);
    }

    /**
     * Tích điểm thưởng cho khách hàng từ giá trị đơn hàng bán hoặc hợp đồng thuê thiết bị.
     *
     * @param customerId Mã khách hàng
     * @param orderTotal Tổng số tiền đơn hàng (VNĐ)
     * @return true nếu tích điểm thành công
     */
    public boolean accumulatePointsFromOrder(String customerId, double orderTotal) {
        if (orderTotal <= 0) {
            return false;
        }

        Customer customer = getCustomerById(customerId);
        if (customer == null) {
            System.err.println("[CustomerService] Không tìm thấy khách hàng với ID: " + customerId);
            return false;
        }

        int pointsEarned = (int) (orderTotal / SPEND_AMOUNT_PER_POINT);
        if (pointsEarned > 0) {
            customer.setRewardPoints(customer.getRewardPoints() + pointsEarned);
            recalculateCustomerTier(customer);
            boolean updated = customerRepository.update(customer);
            if (updated) {
                System.out.println("[CustomerService] Tích lũy +" + pointsEarned + " điểm thành công cho khách hàng: "
                        + customer.getFullName() + " (Tổng điểm: " + customer.getRewardPoints() + ", Hạng: " + customer.getCustomerType() + ")");
            }
            return updated;
        }
        return true;
    }

    /**
     * Khấu trừ điểm thưởng của khách hàng (dùng điểm để giảm giá hóa đơn).
     *
     * @param customerId Mã khách hàng
     * @param pointsToUse Số điểm muốn khấu trừ
     * @return true nếu khấu trừ thành công
     */
    public boolean deductPoints(String customerId, int pointsToUse) {
        if (pointsToUse <= 0) {
            return true;
        }

        Customer customer = getCustomerById(customerId);
        if (customer == null) {
            return false;
        }

        if (customer.getRewardPoints() < pointsToUse) {
            System.err.println("[CustomerService] Khách hàng không đủ điểm thưởng! Hiện có: "
                    + customer.getRewardPoints() + " | Yêu cầu: " + pointsToUse);
            return false;
        }

        customer.setRewardPoints(customer.getRewardPoints() - pointsToUse);
        recalculateCustomerTier(customer);
        return customerRepository.update(customer);
    }

    public static final String TIER_BRONZE = "BRONZE";
    public static final String TIER_SILVER = "SILVER";
    public static final String TIER_GOLD = "GOLD";
    public static final String TIER_PLATINUM = "PLATINUM";

    /**
     * Tự động tính toán lại phân hạng thành viên (BRONZE, SILVER, GOLD, PLATINUM)
     * dựa trên số điểm thưởng tích lũy hiện có của khách hàng.
     *
     * @param customer Đối tượng khách hàng
     * @return Hạng thành viên mới
     */
    public String recalculateCustomerTier(Customer customer) {
        if (customer == null) {
            return TIER_BRONZE;
        }

        int points = customer.getRewardPoints();
        String tier = TIER_BRONZE;

        if (points >= 1000) {
            tier = TIER_PLATINUM;
        } else if (points >= 500) {
            tier = TIER_GOLD;
        } else if (points >= 100) {
            tier = TIER_SILVER;
        }

        customer.setCustomerType(tier);
        customerRepository.update(customer);
        return tier;
    }

    /**
     * Lấy tỷ lệ % chiết khấu ưu đãi dựa trên phân hạng thành viên của khách hàng:
     * - BRONZE: 0%
     * - SILVER: 2%
     * - GOLD: 5%
     * - PLATINUM: 8%
     *
     * @param customer Khách hàng
     * @return Tỷ lệ % giảm giá (ví dụ 5.0 nghĩa là 5%)
     */
    public double getTierDiscountPercent(Customer customer) {
        if (customer == null) {
            return 0.0;
        }

        String tier = customer.getCustomerType();
        if (tier == null || tier.isEmpty()) {
            tier = recalculateCustomerTier(customer);
        }

        if (TIER_PLATINUM.equalsIgnoreCase(tier)) {
            return 8.0;
        } else if (TIER_GOLD.equalsIgnoreCase(tier)) {
            return 5.0;
        } else if (TIER_SILVER.equalsIgnoreCase(tier)) {
            return 2.0;
        }
        return 0.0;
    }

    /**
     * Cho phép Quản trị viên điều chỉnh điểm thưởng thủ công (cộng hoặc trừ) kèm lý do.
     *
     * @param customerId  Mã khách hàng
     * @param deltaPoints Số điểm thay đổi (dương để cộng, âm để trừ)
     * @param reason      Lý do điều chỉnh (phục vụ Audit Log)
     * @return true nếu điều chỉnh và cập nhật thành công
     */
    public boolean adjustPoints(String customerId, int deltaPoints, String reason) {
        if (customerId == null || customerId.trim().isEmpty()) {
            return false;
        }

        Customer customer = getCustomerById(customerId);
        if (customer == null) {
            System.err.println("[CustomerService] Không tìm thấy khách hàng ID: " + customerId);
            return false;
        }

        int newPoints = Math.max(0, customer.getRewardPoints() + deltaPoints);
        customer.setRewardPoints(newPoints);
        recalculateCustomerTier(customer);

        boolean updated = customerRepository.update(customer);
        if (updated) {
            System.out.println("[CustomerService] Điều chỉnh điểm thủ công thành công cho KH ["
                    + customer.getFullName() + "]: delta=" + deltaPoints + ", Điểm mới=" + newPoints
                    + ", Hạng=" + customer.getCustomerType() + ", Lý do: " + reason);
        }
        return updated;
    }

    /**
     * Xóa khách hàng khỏi hệ thống theo ID.
     *
     * @param id Mã khách hàng
     * @return true nếu xóa thành công
     */
    public boolean deleteCustomer(String id) {
        if (id == null || id.trim().isEmpty()) {
            return false;
        }
        return customerRepository.deleteById(id.trim());
    }
}
