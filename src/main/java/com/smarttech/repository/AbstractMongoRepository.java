package com.smarttech.repository;

import com.mongodb.MongoException;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.ReplaceOptions;
import com.mongodb.client.result.DeleteResult;
import com.mongodb.client.result.InsertOneResult;
import com.mongodb.client.result.UpdateResult;
import com.smarttech.config.MongoDBConnection;
import org.bson.Document;
import org.bson.conversions.Bson;

import java.util.ArrayList;
import java.util.List;

/**
 * Lớp trừu tượng cài đặt GenericRepository dành riêng cho MongoDB (Driver 5.x).
 * Quản lý các thao tác cơ sở dữ liệu chuẩn hóa, bắt ngoại lệ và tái sử dụng mã nguồn.
 *
 * @param <T> Kiểu thực thể Model
 * @author Senior Java Developer &amp; Software Architect
 * @project SmartTech
 */
public abstract class AbstractMongoRepository<T> implements GenericRepository<T> {

    protected final String collectionName;
    protected final MongoCollection<Document> collection;

    /**
     * Khởi tạo Repository với tên collection cụ thể trong SmartTechDB.
     *
     * @param collectionName Tên collection trong MongoDB
     */
    public AbstractMongoRepository(String collectionName) {
        this.collectionName = collectionName;
        this.collection = MongoDBConnection.getDatabase().getCollection(collectionName);
    }

    /**
     * Phương thức trừu tượng chuyển đổi từ Java Entity sang BSON Document.
     *
     * @param entity Đối tượng thực thể Java
     * @return Document đại diện lưu vào MongoDB
     */
    protected abstract Document toDocument(T entity);

    /**
     * Phương thức trừu tượng chuyển đổi từ BSON Document sang Java Entity.
     *
     * @param doc Document lấy từ MongoDB
     * @return Đối tượng thực thể Java tương ứng
     */
    protected abstract T fromDocument(Document doc);

    /**
     * Lấy ID (Khoá chính) của thực thể để phục vụ thao tác update/delete.
     *
     * @param entity Đối tượng thực thể
     * @return Khóa chính dạng chuỗi
     */
    protected abstract String getEntityId(T entity);

    @Override
    public List<T> findAll() {
        List<T> list = new ArrayList<>();
        try {
            for (Document doc : collection.find()) {
                T entity = fromDocument(doc);
                if (entity != null) {
                    list.add(entity);
                }
            }
        } catch (MongoException e) {
            System.err.println("[MongoDB] Lỗi khi thực hiện findAll trên collection '" + collectionName + "': " + e.getMessage());
        } catch (Exception e) {
            System.err.println("[Hệ Thống] Lỗi ngoại lệ trong findAll: " + e.getMessage());
        }
        return list;
    }

    @Override
    public T findById(String id) {
        if (id == null || id.trim().isEmpty()) {
            return null;
        }
        try {
            Bson filter = Filters.eq("_id", id);
            Document doc = collection.find(filter).first();
            if (doc != null) {
                return fromDocument(doc);
            }
        } catch (MongoException e) {
            System.err.println("[MongoDB] Lỗi khi tìm kiếm findById (" + id + ") trên '" + collectionName + "': " + e.getMessage());
        }
        return null;
    }

    @Override
    public boolean save(T entity) {
        if (entity == null) {
            return false;
        }
        try {
            Document doc = toDocument(entity);
            InsertOneResult result = collection.insertOne(doc);
            return result.wasAcknowledged();
        } catch (MongoException e) {
            System.err.println("[MongoDB] Lỗi khi thêm mới (save) vào '" + collectionName + "': " + e.getMessage());
            return false;
        }
    }

    @Override
    public boolean update(T entity) {
        if (entity == null) {
            return false;
        }
        String id = getEntityId(entity);
        if (id == null || id.trim().isEmpty()) {
            System.err.println("[MongoDB] Không thể update thực thể vì ID rỗng!");
            return false;
        }

        try {
            Bson filter = Filters.eq("_id", id);
            Document doc = toDocument(entity);
            // Replace toàn bộ document theo ID mà không upsert mới nếu không tìm thấy
            UpdateResult result = collection.replaceOne(filter, doc, new ReplaceOptions().upsert(false));
            return result.getMatchedCount() > 0;
        } catch (MongoException e) {
            System.err.println("[MongoDB] Lỗi khi cập nhật (update) ID '" + id + "' trên '" + collectionName + "': " + e.getMessage());
            return false;
        }
    }

    @Override
    public boolean deleteById(String id) {
        if (id == null || id.trim().isEmpty()) {
            return false;
        }
        try {
            Bson filter = Filters.eq("_id", id);
            DeleteResult result = collection.deleteOne(filter);
            return result.getDeletedCount() > 0;
        } catch (MongoException e) {
            System.err.println("[MongoDB] Lỗi khi xóa (deleteById) ID '" + id + "' trên '" + collectionName + "': " + e.getMessage());
            return false;
        }
    }

    @Override
    public List<T> findByField(String fieldName, Object value) {
        List<T> list = new ArrayList<>();
        if (fieldName == null || fieldName.trim().isEmpty()) {
            return list;
        }

        try {
            Bson filter = Filters.eq(fieldName, value);
            for (Document doc : collection.find(filter)) {
                T entity = fromDocument(doc);
                if (entity != null) {
                    list.add(entity);
                }
            }
        } catch (MongoException e) {
            System.err.println("[MongoDB] Lỗi khi lọc findByField (" + fieldName + "=" + value + ") trên '" + collectionName + "': " + e.getMessage());
        }
        return list;
    }

    /**
     * Lấy đối tượng collection gốc để mở rộng các truy vấn phức tạp (Aggregation, Index,...).
     *
     * @return MongoCollection<Document>
     */
    public MongoCollection<Document> getCollection() {
        return this.collection;
    }
}
