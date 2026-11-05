package com.smarttech.repository;

import com.mongodb.client.model.Filters;
import com.smarttech.model.Equipment;
import org.bson.Document;
import org.bson.conversions.Bson;

import java.util.*;

/**
 * Repository quản lý từng Thiết bị thực tế theo Số Serial phần cứng trong collection "equipments".
 *
 * @author Senior Java Developer &amp; Software Architect
 * @project SmartTech
 */
public class EquipmentRepository extends AbstractMongoRepository<Equipment> {

    public EquipmentRepository() {
        super("equipments");
    }

    @Override
    protected Document toDocument(Equipment entity) {
        return entity != null ? entity.toDocument() : null;
    }

    @Override
    protected Equipment fromDocument(Document doc) {
        return Equipment.fromDocument(doc);
    }

    @Override
    protected String getEntityId(Equipment entity) {
        return entity != null ? entity.getId() : null;
    }

    /**
     * Lấy danh sách thiết bị thực tế thuộc một mẫu sản phẩm.
     *
     * @param productId Mã mẫu sản phẩm
     * @return Danh sách các máy thực tế
     */
    public List<Equipment> findByProductId(String productId) {
        return findByField("productId", productId);
    }

    /**
     * Lấy danh sách thiết bị theo trạng thái (AVAILABLE, RENTED, SOLD, MAINTENANCE, BROKEN).
     *
     * @param status Trạng thái cần tìm
     * @return Danh sách thiết bị
     */
    public List<Equipment> findByStatus(String status) {
        return findByField("status", status);
    }

    /**
     * Lấy danh sách thiết bị thực tế còn khả dụng (AVAILABLE) của một sản phẩm để cho thuê hoặc bán.
     *
     * @param productId Mã sản phẩm
     * @return Danh sách máy sẵn sàng giao dịch
     */
    public List<Equipment> findAvailableByProductId(String productId) {
        List<Equipment> list = new ArrayList<>();
        if (productId == null || productId.trim().isEmpty()) {
            return list;
        }

        try {
            Bson filter = Filters.and(
                    Filters.eq("productId", productId),
                    Filters.eq("status", Equipment.STATUS_AVAILABLE),
                    Filters.eq("active", true)
            );
            for (Document doc : collection.find(filter)) {
                Equipment eq = fromDocument(doc);
                if (eq != null) {
                    list.add(eq);
                }
            }
        } catch (Exception e) {
            System.err.println("[EquipmentRepository] Lỗi truy vấn thiết bị khả dụng: " + e.getMessage());
        }
        return list;
    }

    /**
     * Đếm số lượng Serial máy theo trạng thái ("AVAILABLE", "RENTED", "MAINTENANCE", "SOLD", "BROKEN").
     *
     * @param status Trạng thái thiết bị cần đếm
     * @return Số lượng thiết bị
     */
    public long countEquipmentsByStatus(String status) {
        try {
            Bson filter = (status != null && !status.trim().isEmpty())
                    ? Filters.eq("status", status.trim())
                    : new Document();
            return collection.countDocuments(filter);
        } catch (Exception e) {
            System.err.println("[EquipmentRepository] Lỗi countEquipmentsByStatus (" + status + "): " + e.getMessage());
            return 0;
        }
    }

    /**
     * Thống kê các Mẫu Sản Phẩm (productId) có số lượng thiết bị phần cứng ở trạng thái "AVAILABLE"
     * ít hơn hoặc bằng ngưỡng quy định (threshold, ví dụ <= 2 máy).
     *
     * @param threshold Ngưỡng tồn kho tối thiểu cảnh báo
     * @return Danh sách cặp [Mã sản phẩm -> Số lượng máy khả dụng hiện tại]
     */
    public List<Map.Entry<String, Long>> getLowStockProducts(int threshold) {
        List<Map.Entry<String, Long>> result = new ArrayList<>();
        try {
            // Gom nhóm đếm số lượng máy AVAILABLE theo productId
            Map<String, Long> countMap = new HashMap<>();
            Bson filter = Filters.and(
                    Filters.eq("status", Equipment.STATUS_AVAILABLE),
                    Filters.eq("active", true)
            );

            // Thu thập toàn bộ productId xuất hiện trong hệ thống thiết bị
            Set<String> allProductIds = new HashSet<>();
            for (Document doc : collection.find()) {
                String pid = doc.getString("productId");
                if (pid != null && !pid.trim().isEmpty()) {
                    allProductIds.add(pid);
                }
            }

            // Khởi tạo count = 0 cho tất cả
            for (String pid : allProductIds) {
                countMap.put(pid, 0L);
            }

            // Đếm các máy AVAILABLE
            for (Document doc : collection.find(filter)) {
                String pid = doc.getString("productId");
                if (pid != null && !pid.trim().isEmpty()) {
                    countMap.put(pid, countMap.getOrDefault(pid, 0L) + 1);
                }
            }

            // Lọc các sản phẩm có số lượng <= threshold
            for (Map.Entry<String, Long> entry : countMap.entrySet()) {
                if (entry.getValue() <= threshold) {
                    result.add(new AbstractMap.SimpleEntry<>(entry.getKey(), entry.getValue()));
                }
            }
        } catch (Exception e) {
            System.err.println("[EquipmentRepository] Lỗi getLowStockProducts: " + e.getMessage());
        }
        return result;
    }

    /**
     * Phương thức tiện ích trả về dạng Map cho việc tra cứu nhanh.
     */
    public Map<String, Long> getLowStockProductsMap(int threshold) {
        Map<String, Long> map = new HashMap<>();
        for (Map.Entry<String, Long> entry : getLowStockProducts(threshold)) {
            map.put(entry.getKey(), entry.getValue());
        }
        return map;
    }
}
