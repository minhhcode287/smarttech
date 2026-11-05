package com.smarttech.repository;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Sorts;
import com.smarttech.config.MongoDBConnection;
import com.smarttech.model.StockAudit;
import org.bson.Document;
import org.bson.conversions.Bson;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Repository xử lý lưu trữ và truy vấn các Đợt Kiểm Kê Kho (StockAudit) trên MongoDB collection "stock_audits".
 *
 * @author Senior Java Developer & Software Architect
 * @project SmartTech
 */
public class StockAuditRepository {

    private final MongoCollection<Document> collection;

    public StockAuditRepository() {
        // ĐÃ SỬA: Đổi "import_orders" thành "stock_audits"
        this.collection = MongoDBConnection.getInstance().getCollection("stock_audits");
    }

    public List<StockAudit> findAll() {
        List<StockAudit> list = new ArrayList<>();
        // Sắp xếp theo ngày kiểm kê mới nhất
        for (Document doc : collection.find().sort(Sorts.descending("auditDate", "_id"))) {
            StockAudit audit = StockAudit.fromDocument(doc);
            if (audit != null) list.add(audit);
        }
        return list;
    }

    public StockAudit findById(String id) {
        if (id == null || id.trim().isEmpty()) return null;
        Document doc = collection.find(Filters.eq("_id", id.trim())).first();
        return StockAudit.fromDocument(doc);
    }

    public List<StockAudit> search(String keyword) {
        if (keyword == null || keyword.trim().isEmpty()) {
            return findAll();
        }
        Pattern pattern = Pattern.compile(Pattern.quote(keyword.trim()), Pattern.CASE_INSENSITIVE);
        Bson filter = Filters.or(
                Filters.regex("_id", pattern),
                Filters.regex("auditName", pattern),
                Filters.regex("employeeName", pattern),
                Filters.regex("note", pattern),
                Filters.regex("status", pattern)
        );

        List<StockAudit> list = new ArrayList<>();
        for (Document doc : collection.find(filter).sort(Sorts.descending("auditDate", "_id"))) {
            StockAudit audit = StockAudit.fromDocument(doc);
            if (audit != null) list.add(audit);
        }
        return list;
    }

    public boolean save(StockAudit audit) {
        if (audit == null || audit.getId() == null || audit.getId().trim().isEmpty()) return false;
        Document doc = audit.toDocument();
        collection.insertOne(doc);
        return true;
    }

    public boolean update(StockAudit audit) {
        if (audit == null || audit.getId() == null) return false;
        Document doc = audit.toDocument();
        doc.remove("_id");
        return collection.updateOne(Filters.eq("_id", audit.getId()), new Document("$set", doc)).getModifiedCount() > 0;
    }
}