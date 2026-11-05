package com.smarttech.repository;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Sorts;
import com.smarttech.config.MongoDBConnection;
import com.smarttech.model.ImportOrder;
import org.bson.Document;
import org.bson.conversions.Bson;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Repository xử lý các thao tác lưu trữ và truy vấn Phiếu Nhập Kho (ImportOrder) trên MongoDB.
 *
 * @author Senior Java Developer & Software Architect
 * @project SmartTech
 */
public class ImportOrderRepository {

    private final MongoCollection<Document> collection;

    public ImportOrderRepository() {
        this.collection = MongoDBConnection.getInstance().getCollection("import_orders");
    }

    public List<ImportOrder> findAll() {
        List<ImportOrder> list = new ArrayList<>();
        // Sắp xếp mới nhất lên đầu
        for (Document doc : collection.find().sort(Sorts.descending("importDate", "_id"))) {
            ImportOrder order = ImportOrder.fromDocument(doc);
            if (order != null) list.add(order);
        }
        return list;
    }

    public ImportOrder findById(String id) {
        if (id == null || id.trim().isEmpty()) return null;
        Document doc = collection.find(Filters.eq("_id", id.trim())).first();
        return ImportOrder.fromDocument(doc);
    }

    public List<ImportOrder> findBySupplierId(String supplierId) {
        List<ImportOrder> list = new ArrayList<>();
        if (supplierId == null || supplierId.trim().isEmpty()) return list;

        Bson filter = Filters.eq("supplierId", supplierId.trim());
        for (Document doc : collection.find(filter).sort(Sorts.descending("importDate"))) {
            ImportOrder order = ImportOrder.fromDocument(doc);
            if (order != null) list.add(order);
        }
        return list;
    }

    public List<ImportOrder> search(String keyword) {
        if (keyword == null || keyword.trim().isEmpty()) {
            return findAll();
        }
        Pattern pattern = Pattern.compile(Pattern.quote(keyword.trim()), Pattern.CASE_INSENSITIVE);
        Bson filter = Filters.or(
                Filters.regex("_id", pattern),
                Filters.regex("supplierName", pattern),
                Filters.regex("employeeId", pattern),
                Filters.regex("note", pattern)
        );

        List<ImportOrder> list = new ArrayList<>();
        for (Document doc : collection.find(filter).sort(Sorts.descending("importDate", "_id"))) {
            ImportOrder order = ImportOrder.fromDocument(doc);
            if (order != null) list.add(order);
        }
        return list;
    }

    public boolean save(ImportOrder order) {
        if (order == null || order.getId() == null || order.getId().trim().isEmpty()) return false;
        Document doc = order.toDocument();
        collection.insertOne(doc);
        return true;
    }

    public boolean update(ImportOrder order) {
        if (order == null || order.getId() == null) return false;
        Document doc = order.toDocument();
        doc.remove("_id");
        return collection.updateOne(Filters.eq("_id", order.getId()), new Document("$set", doc)).getModifiedCount() > 0;
    }
}
