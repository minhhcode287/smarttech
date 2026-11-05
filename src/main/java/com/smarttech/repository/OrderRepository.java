package com.smarttech.repository;

import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Sorts;
import com.smarttech.model.Order;
import org.bson.Document;
import org.bson.conversions.Bson;

import java.util.ArrayList;
import java.util.List;

/**
 * Repository quản lý đơn hàng bán lẻ trong collection "orders".
 *
 * @author Senior Java Developer &amp; Software Architect
 * @project SmartTech
 */
public class OrderRepository extends AbstractMongoRepository<Order> {

    public OrderRepository() {
        super("orders");
    }

    @Override
    protected Document toDocument(Order entity) {
        return entity != null ? entity.toDocument() : null;
    }

    @Override
    protected Order fromDocument(Document doc) {
        return Order.fromDocument(doc);
    }

    @Override
    protected String getEntityId(Order entity) {
        return entity != null ? entity.getId() : null;
    }

    /**
     * Lấy danh sách hóa đơn theo mã khách hàng.
     */
    public List<Order> findByCustomerId(String customerId) {
        return findByField("customerId", customerId);
    }

    /**
     * Tính tổng doanh thu bán hàng phát sinh trong tháng và năm chỉ định.
     *
     * @param month Tháng (1 - 12)
     * @param year  Năm (ví dụ 2026)
     * @return Tổng doanh thu bán hàng thực thu (finalAmount)
     */
    public double getMonthlySalesRevenue(int month, int year) {
        double total = 0.0;
        String prefix = String.format("%04d-%02d", year, month);

        try {
            Bson filter = Filters.and(
                    Filters.regex("orderDate", "^" + prefix),
                    Filters.ne("status", "CANCELLED")
            );
            for (Document doc : collection.find(filter)) {
                Number num = doc.get("finalAmount", Number.class);
                if (num != null) {
                    total += num.doubleValue();
                }
            }
        } catch (Exception e) {
            System.err.println("[OrderRepository] Lỗi tính getMonthlySalesRevenue (" + prefix + "): " + e.getMessage());
        }
        return total;
    }

    /**
     * Lấy danh sách N đơn hàng mới nhất, sắp xếp theo thời gian khởi tạo giảm dần.
     *
     * @param limit Số lượng bản ghi tối đa
     * @return Danh sách đơn hàng mới nhất
     */
    public List<Order> getRecentOrders(int limit) {
        List<Order> list = new ArrayList<>();
        try {
            for (Document doc : collection.find().sort(Sorts.descending("_id")).limit(limit)) {
                Order order = fromDocument(doc);
                if (order != null) {
                    list.add(order);
                }
            }
        } catch (Exception e) {
            System.err.println("[OrderRepository] Lỗi getRecentOrders: " + e.getMessage());
        }
        return list;
    }
}
