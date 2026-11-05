package com.smarttech.repository;

import com.mongodb.MongoException;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Sorts;
import com.smarttech.model.EquipmentLog;
import org.bson.Document;
import org.bson.conversions.Bson;

import java.util.ArrayList;
import java.util.List;

/**
 * Repository thao tác với cơ sở dữ liệu MongoDB cho thực thể EquipmentLog (Collection: equipment_logs).
 * Lưu trữ toàn bộ lịch sử biến động phần cứng, nhật ký bảo dưỡng và sửa chữa thiết bị công nghệ.
 *
 * @author Senior Java Developer & Software Architect
 * @project SmartTech
 */
public class EquipmentLogRepository extends AbstractMongoRepository<EquipmentLog> {

    public static final String COLLECTION_NAME = "equipment_logs";

    public EquipmentLogRepository() {
        super(COLLECTION_NAME);
    }

    @Override
    protected Document toDocument(EquipmentLog entity) {
        return entity != null ? entity.toDocument() : null;
    }

    @Override
    protected EquipmentLog fromDocument(Document doc) {
        return EquipmentLog.fromDocument(doc);
    }

    @Override
    protected String getEntityId(EquipmentLog entity) {
        return entity != null ? entity.getId() : null;
    }

    /**
     * Tìm kiếm toàn bộ nhật ký biến động của một thiết bị theo số Serial phần cứng.
     * Kết quả trả về được sắp xếp theo thời gian mới nhất lên đầu.
     *
     * @param serialId Mã Serial thiết bị cần tra cứu
     * @return Danh sách nhật ký biến động
     */
    public List<EquipmentLog> findBySerialId(String serialId) {
        List<EquipmentLog> list = new ArrayList<>();
        if (serialId == null || serialId.trim().isEmpty()) {
            return list;
        }

        try {
            Bson filter = Filters.eq("serialId", serialId.trim());
            for (Document doc : collection.find(filter).sort(Sorts.descending("timestamp"))) {
                EquipmentLog log = fromDocument(doc);
                if (log != null) {
                    list.add(log);
                }
            }
        } catch (MongoException e) {
            System.err.println("[EquipmentLogRepository] Lỗi findBySerialId (" + serialId + "): " + e.getMessage());
        } catch (Exception e) {
            System.err.println("[EquipmentLogRepository] Lỗi ngoại lệ trong findBySerialId: " + e.getMessage());
        }
        return list;
    }

    /**
     * Lấy toàn bộ danh sách nhật ký biến động của mọi thiết bị trong hệ thống,
     * sắp xếp theo thời gian giảm dần (sự kiện mới nhất hiển thị đầu tiên).
     *
     * @return Danh sách toàn bộ nhật ký hệ thống
     */
    @Override
    public List<EquipmentLog> findAll() {
        List<EquipmentLog> list = new ArrayList<>();
        try {
            for (Document doc : collection.find().sort(Sorts.descending("timestamp"))) {
                EquipmentLog log = fromDocument(doc);
                if (log != null) {
                    list.add(log);
                }
            }
        } catch (MongoException e) {
            System.err.println("[EquipmentLogRepository] Lỗi findAll: " + e.getMessage());
        } catch (Exception e) {
            System.err.println("[EquipmentLogRepository] Lỗi ngoại lệ trong findAll: " + e.getMessage());
        }
        return list;
    }
}