package com.smarttech.repository;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Sorts;
import com.smarttech.config.MongoDBConnection;
import com.smarttech.model.Supplier;
import org.bson.Document;
import org.bson.conversions.Bson;
import org.bson.types.ObjectId;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Repository xử lý các thao tác CRUD dữ liệu Nhà Cung Cấp (Supplier) trên MongoDB.
 *
 * @author Senior Java Developer & Software Architect
 * @project SmartTech
 */
public class SupplierRepository {

    private final MongoCollection<Document> collection;

    public SupplierRepository() {
        this.collection = MongoDBConnection.getInstance().getCollection("suppliers");
        initDefaultSuppliersIfEmpty();
    }

    /**
     * Khởi tạo một số Nhà Cung Cấp công nghệ mẫu nếu Database đang rỗng.
     */
    private void initDefaultSuppliersIfEmpty() {
        if (collection.countDocuments() == 0) {
            List<Supplier> defaultList = List.of(
                    new Supplier("FPT Synnex Corporation", "19006508", "contact@synnexfpt.com.vn", "Tòa nhà FPT Cầu Giấy, Hà Nội", "0101778163", Supplier.STATUS_ACTIVE),
                    new Supplier("Digiworld Corporation (DGW)", "02839290059", "info@dgw.com.vn", "195 Điện Biên Phủ, P.15, Bình Thạnh, TP.HCM", "0302861742", Supplier.STATUS_ACTIVE),
                    new Supplier("Viễn Sơn Petrosetco Distribution", "02838326085", "support@microstar.vn", "162B Bùi Thị Xuân, Q.1, TP.HCM", "0305096503", Supplier.STATUS_ACTIVE),
                    new Supplier("Phong Vũ Enterprise Partner", "18006865", "b2b@phongvu.vn", "216 Huỳnh Tấn Phát, Tân Thuận Tây, Q.7, TP.HCM", "0302781498", Supplier.STATUS_ACTIVE)
            );
            for (Supplier s : defaultList) {
                save(s);
            }
        }
    }

    public List<Supplier> findAll() {
        List<Supplier> list = new ArrayList<>();
        for (Document doc : collection.find().sort(Sorts.ascending("name"))) {
            Supplier s = Supplier.fromDocument(doc);
            if (s != null) list.add(s);
        }
        return list;
    }

    public List<Supplier> findActiveSuppliers() {
        List<Supplier> list = new ArrayList<>();
        Bson filter = Filters.eq("status", Supplier.STATUS_ACTIVE);
        for (Document doc : collection.find(filter).sort(Sorts.ascending("name"))) {
            Supplier s = Supplier.fromDocument(doc);
            if (s != null) list.add(s);
        }
        return list;
    }

    public Supplier findById(String id) {
        if (id == null || id.trim().isEmpty()) return null;
        Bson filter;
        try {
            filter = Filters.eq("_id", new ObjectId(id));
        } catch (Exception ex) {
            filter = Filters.eq("_id", id);
        }
        Document doc = collection.find(filter).first();
        return Supplier.fromDocument(doc);
    }

    public List<Supplier> search(String keyword) {
        if (keyword == null || keyword.trim().isEmpty()) {
            return findAll();
        }
        Pattern pattern = Pattern.compile(Pattern.quote(keyword.trim()), Pattern.CASE_INSENSITIVE);
        Bson filter = Filters.or(
                Filters.regex("name", pattern),
                Filters.regex("phone", pattern),
                Filters.regex("taxCode", pattern),
                Filters.regex("email", pattern)
        );

        List<Supplier> list = new ArrayList<>();
        for (Document doc : collection.find(filter).sort(Sorts.ascending("name"))) {
            Supplier s = Supplier.fromDocument(doc);
            if (s != null) list.add(s);
        }
        return list;
    }

    public boolean save(Supplier supplier) {
        if (supplier == null) return false;
        Document doc = supplier.toDocument();
        collection.insertOne(doc);
        Object genId = doc.get("_id");
        if (genId != null) {
            supplier.setId(genId.toString());
        }
        return true;
    }

    public boolean update(Supplier supplier) {
        if (supplier == null || supplier.getId() == null) return false;
        Bson filter;
        try {
            filter = Filters.eq("_id", new ObjectId(supplier.getId()));
        } catch (Exception ex) {
            filter = Filters.eq("_id", supplier.getId());
        }

        Document doc = supplier.toDocument();
        doc.remove("_id"); // Không update trường _id

        return collection.updateOne(filter, new Document("$set", doc)).getModifiedCount() > 0;
    }

    public boolean delete(String id) {
        if (id == null || id.trim().isEmpty()) return false;
        Bson filter;
        try {
            filter = Filters.eq("_id", new ObjectId(id));
        } catch (Exception ex) {
            filter = Filters.eq("_id", id);
        }
        return collection.deleteOne(filter).getDeletedCount() > 0;
    }
}
