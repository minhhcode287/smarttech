package com.smarttech.repository;

import com.smarttech.model.Customer;
import org.bson.Document;

import java.util.List;

/**
 * Repository quản lý dữ liệu Khách hàng trong collection "customers".
 *
 * @author Senior Java Developer &amp; Software Architect
 * @project SmartTech
 */
public class CustomerRepository extends AbstractMongoRepository<Customer> {

    public CustomerRepository() {
        super("customers");
    }

    @Override
    protected Document toDocument(Customer entity) {
        return entity != null ? entity.toDocument() : null;
    }

    @Override
    protected Customer fromDocument(Document doc) {
        return Customer.fromDocument(doc);
    }

    @Override
    protected String getEntityId(Customer entity) {
        return entity != null ? entity.getId() : null;
    }

    /**
     * Tìm kiếm khách hàng theo số điện thoại (dùng phổ biến khi lập đơn hàng).
     *
     * @param phone Số điện thoại khách hàng
     * @return Customer hoặc null
     */
    public Customer findByPhone(String phone) {
        if (phone == null || phone.trim().isEmpty()) {
            return null;
        }
        List<Customer> list = findByField("phone", phone.trim());
        return list.isEmpty() ? null : list.get(0);
    }

    /**
     * Lọc danh sách khách hàng theo phân loại: INDIVIDUAL (Cá nhân) hoặc ENTERPRISE (Doanh nghiệp).
     *
     * @param customerType Loại khách hàng
     * @return Danh sách khách hàng phù hợp
     */
    public List<Customer> findByCustomerType(String customerType) {
        return findByField("customerType", customerType);
    }

    /**
     * Đếm tổng số lượng khách hàng trong hệ thống.
     */
    public long countCustomers() {
        try {
            return collection.countDocuments();
        } catch (Exception e) {
            System.err.println("[CustomerRepository] Lỗi countCustomers: " + e.getMessage());
            return 0;
        }
    }

    /**
     * Tính tổng số điểm thưởng tích lũy của toàn bộ khách hàng.
     */
    public long sumRewardPoints() {
        long totalPoints = 0;
        try {
            for (Document doc : collection.find()) {
                Number pts = doc.get("rewardPoints", Number.class);
                if (pts != null) {
                    totalPoints += pts.longValue();
                }
            }
        } catch (Exception e) {
            System.err.println("[CustomerRepository] Lỗi sumRewardPoints: " + e.getMessage());
        }
        return totalPoints;
    }
}
