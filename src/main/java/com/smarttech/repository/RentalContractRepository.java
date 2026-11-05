package com.smarttech.repository;

import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Sorts;
import com.smarttech.model.RentalContract;
import org.bson.Document;
import org.bson.conversions.Bson;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Repository quản lý hợp đồng cho thuê thiết bị trong collection "rental_contracts".
 *
 * @author Senior Java Developer &amp; Software Architect
 * @project SmartTech
 */
public class RentalContractRepository extends AbstractMongoRepository<RentalContract> {

    public RentalContractRepository() {
        super("rental_contracts");
    }

    @Override
    protected Document toDocument(RentalContract entity) {
        return entity != null ? entity.toDocument() : null;
    }

    @Override
    protected RentalContract fromDocument(Document doc) {
        return RentalContract.fromDocument(doc);
    }

    @Override
    protected String getEntityId(RentalContract entity) {
        return entity != null ? entity.getId() : null;
    }

    /**
     * Lọc hợp đồng theo trạng thái (ACTIVE, RETURNED, OVERDUE, CANCELLED).
     */
    public List<RentalContract> findByStatus(String status) {
        return findByField("status", status);
    }

    /**
     * Lấy các hợp đồng thuộc một khách hàng.
     */
    public List<RentalContract> findByCustomerId(String customerId) {
        return findByField("customerId", customerId);
    }

    /**
     * Tính tổng doanh thu tiền thuê từ các hợp đồng khởi tạo trong tháng/năm chỉ định.
     *
     * @param month Tháng (1 - 12)
     * @param year  Năm (ví dụ 2026)
     * @return Tổng doanh thu thuê máy
     */
    public double getMonthlyRentalRevenue(int month, int year) {
        double total = 0.0;
        String prefix = String.format("%04d-%02d", year, month);

        try {
            Bson filter = Filters.and(
                    Filters.regex("startDate", "^" + prefix),
                    Filters.ne("status", RentalContract.STATUS_CANCELLED)
            );
            for (Document doc : collection.find(filter)) {
                Number num = doc.get("totalRentalFee", Number.class);
                if (num != null) {
                    total += num.doubleValue();
                }
            }
        } catch (Exception e) {
            System.err.println("[RentalContractRepository] Lỗi tính getMonthlyRentalRevenue (" + prefix + "): " + e.getMessage());
        }
        return total;
    }

    /**
     * Đếm tổng số lượng hợp đồng cho thuê đang hoạt động (ACTIVE).
     *
     * @return Số lượng hợp đồng ACTIVE
     */
    public long countActiveContracts() {
        try {
            return collection.countDocuments(Filters.eq("status", RentalContract.STATUS_ACTIVE));
        } catch (Exception e) {
            System.err.println("[RentalContractRepository] Lỗi countActiveContracts: " + e.getMessage());
            return 0;
        }
    }

    /**
     * Lấy danh sách hợp đồng cho thuê đang ở trạng thái "ACTIVE" mà ngày trả dự kiến
     * (expectedEndDate) đã quá hạn so với hôm nay HOẶC chỉ còn <= 1 ngày là đến hạn trả máy.
     *
     * @return Danh sách các hợp đồng cần cảnh báo thu hồi
     */
    public List<RentalContract> getOverdueAndExpiringContracts() {
        List<RentalContract> list = new ArrayList<>();
        try {
            LocalDate today = LocalDate.now();
            LocalDate tomorrow = today.plusDays(1);

            Bson filter = Filters.eq("status", RentalContract.STATUS_ACTIVE);
            for (Document doc : collection.find(filter)) {
                RentalContract contract = fromDocument(doc);
                if (contract != null && contract.getExpectedEndDate() != null) {
                    // Nếu ngày trả dự kiến <= ngày mai thì rơi vào diện quá hạn hoặc sắp đến hạn
                    if (!contract.getExpectedEndDate().isAfter(tomorrow)) {
                        list.add(contract);
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("[RentalContractRepository] Lỗi getOverdueAndExpiringContracts: " + e.getMessage());
        }
        return list;
    }

    /**
     * Lấy danh sách N hợp đồng cho thuê mới nhất, sắp xếp theo thời gian khởi tạo giảm dần.
     *
     * @param limit Số lượng bản ghi tối đa
     * @return Danh sách hợp đồng mới nhất
     */
    public List<RentalContract> getRecentContracts(int limit) {
        List<RentalContract> list = new ArrayList<>();
        try {
            for (Document doc : collection.find().sort(Sorts.descending("_id")).limit(limit)) {
                RentalContract contract = fromDocument(doc);
                if (contract != null) {
                    list.add(contract);
                }
            }
        } catch (Exception e) {
            System.err.println("[RentalContractRepository] Lỗi getRecentContracts: " + e.getMessage());
        }
        return list;
    }
}
