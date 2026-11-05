package com.smarttech.repository;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Sorts;
import com.smarttech.config.MongoDBConnection;
import com.smarttech.model.MaintenanceTicket;
import org.bson.Document;
import org.bson.conversions.Bson;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Repository xử lý lưu trữ và truy vấn các Phiếu Quản Lý Bảo Trì & Sửa Chữa Thiết Bị (MaintenanceTicket)
 * trong MongoDB collection "maintenance_tickets".
 *
 * @author Senior Java Developer & Software Architect
 * @project SmartTech
 */
public class MaintenanceRepository {

    public static final String COLLECTION_NAME = "maintenance_tickets";
    private final MongoCollection<Document> collection;

    public MaintenanceRepository() {
        this.collection = MongoDBConnection.getDatabase().getCollection(COLLECTION_NAME);
    }

    /**
     * Lưu phiếu bảo trì mới vào cơ sở dữ liệu MongoDB.
     *
     * @param ticket Phiếu bảo trì cần lưu
     * @return true nếu lưu thành công, false nếu thất bại
     */
    public boolean save(MaintenanceTicket ticket) {
        if (ticket == null || ticket.getId() == null || ticket.getId().trim().isEmpty()) {
            return false;
        }
        try {
            Document doc = ticket.toDocument();
            collection.insertOne(doc);
            return true;
        } catch (Exception e) {
            System.err.println("[MaintenanceRepository] Lỗi save ticket (" + ticket.getId() + "): " + e.getMessage());
            return false;
        }
    }

    /**
     * Cập nhật thông tin phiếu bảo trì đã tồn tại.
     *
     * @param ticket Phiếu bảo trì với dữ liệu mới
     * @return true nếu cập nhật thành công
     */
    public boolean update(MaintenanceTicket ticket) {
        if (ticket == null || ticket.getId() == null || ticket.getId().trim().isEmpty()) {
            return false;
        }
        try {
            Document doc = ticket.toDocument();
            doc.remove("_id"); // Không ghi đè trường khóa chính _id
            return collection.updateOne(
                    Filters.eq("_id", ticket.getId().trim()),
                    new Document("$set", doc)
            ).getModifiedCount() >= 0;
        } catch (Exception e) {
            System.err.println("[MaintenanceRepository] Lỗi update ticket (" + ticket.getId() + "): " + e.getMessage());
            return false;
        }
    }

    /**
     * Tìm phiếu bảo trì theo mã định danh (ID).
     *
     * @param id Mã phiếu bảo trì
     * @return MaintenanceTicket hoặc null nếu không tìm thấy
     */
    public MaintenanceTicket findById(String id) {
        if (id == null || id.trim().isEmpty()) return null;
        try {
            Document doc = collection.find(Filters.eq("_id", id.trim())).first();
            return MaintenanceTicket.fromDocument(doc);
        } catch (Exception e) {
            System.err.println("[MaintenanceRepository] Lỗi findById (" + id + "): " + e.getMessage());
            return null;
        }
    }

    /**
     * Lấy toàn bộ danh sách phiếu bảo trì, sắp xếp theo ngày gửi mới nhất lên đầu.
     *
     * @return Danh sách toàn bộ phiếu bảo trì
     */
    public List<MaintenanceTicket> findAll() {
        List<MaintenanceTicket> list = new ArrayList<>();
        try {
            for (Document doc : collection.find().sort(Sorts.descending("startDate", "_id"))) {
                MaintenanceTicket ticket = MaintenanceTicket.fromDocument(doc);
                if (ticket != null) {
                    list.add(ticket);
                }
            }
        } catch (Exception e) {
            System.err.println("[MaintenanceRepository] Lỗi findAll: " + e.getMessage());
        }
        return list;
    }

    /**
     * Lấy danh sách các phiếu bảo trì đang xử lý (trạng thái "IN_PROGRESS").
     *
     * @return Danh sách phiếu bảo trì đang hoạt động
     */
    public List<MaintenanceTicket> findActiveTickets() {
        List<MaintenanceTicket> list = new ArrayList<>();
        try {
            Bson filter = Filters.eq("status", MaintenanceTicket.STATUS_IN_PROGRESS);
            for (Document doc : collection.find(filter).sort(Sorts.descending("startDate", "_id"))) {
                MaintenanceTicket ticket = MaintenanceTicket.fromDocument(doc);
                if (ticket != null) {
                    list.add(ticket);
                }
            }
        } catch (Exception e) {
            System.err.println("[MaintenanceRepository] Lỗi findActiveTickets: " + e.getMessage());
        }
        return list;
    }

    /**
     * Tìm kiếm lịch sử bảo trì của một thiết bị cụ thể theo số Serial phần cứng.
     *
     * @param serialId Số Serial thiết bị
     * @return Danh sách phiếu bảo trì của thiết bị đó
     */
    public List<MaintenanceTicket> findBySerialId(String serialId) {
        List<MaintenanceTicket> list = new ArrayList<>();
        if (serialId == null || serialId.trim().isEmpty()) return list;
        try {
            Bson filter = Filters.eq("serialId", serialId.trim().toUpperCase());
            for (Document doc : collection.find(filter).sort(Sorts.descending("startDate", "_id"))) {
                MaintenanceTicket ticket = MaintenanceTicket.fromDocument(doc);
                if (ticket != null) {
                    list.add(ticket);
                }
            }
        } catch (Exception e) {
            System.err.println("[MaintenanceRepository] Lỗi findBySerialId (" + serialId + "): " + e.getMessage());
        }
        return list;
    }

    /**
     * Tìm kiếm phiếu bảo trì theo từ khóa đa năng (Mã phiếu, Serial, Tên SP, Đơn vị sửa, Trạng thái, Nhân viên).
     *
     * @param keyword Từ khóa tìm kiếm
     * @return Danh sách phiếu khớp từ khóa
     */
    public List<MaintenanceTicket> search(String keyword) {
        if (keyword == null || keyword.trim().isEmpty()) {
            return findAll();
        }
        try {
            Pattern pattern = Pattern.compile(Pattern.quote(keyword.trim()), Pattern.CASE_INSENSITIVE);
            Bson filter = Filters.or(
                    Filters.regex("_id", pattern),
                    Filters.regex("serialId", pattern),
                    Filters.regex("productId", pattern),
                    Filters.regex("productName", pattern),
                    Filters.regex("repairVendor", pattern),
                    Filters.regex("issueDescription", pattern),
                    Filters.regex("resolutionNote", pattern),
                    Filters.regex("status", pattern),
                    Filters.regex("employeeName", pattern)
            );

            List<MaintenanceTicket> list = new ArrayList<>();
            for (Document doc : collection.find(filter).sort(Sorts.descending("startDate", "_id"))) {
                MaintenanceTicket ticket = MaintenanceTicket.fromDocument(doc);
                if (ticket != null) {
                    list.add(ticket);
                }
            }
            return list;
        } catch (Exception e) {
            System.err.println("[MaintenanceRepository] Lỗi search: " + e.getMessage());
            return findAll();
        }
    }

    /**
     * Tính tổng chi phí bảo trì tích lũy cho toàn bộ các phiếu đã hoàn tất (COMPLETED).
     *
     * @return Tổng chi phí sửa chữa (VNĐ)
     */
    public double getTotalRepairCost() {
        double total = 0.0;
        try {
            Bson filter = Filters.eq("status", MaintenanceTicket.STATUS_COMPLETED);
            for (Document doc : collection.find(filter)) {
                Number cost = doc.get("repairCost", Number.class);
                if (cost != null) {
                    total += cost.doubleValue();
                }
            }
        } catch (Exception e) {
            System.err.println("[MaintenanceRepository] Lỗi getTotalRepairCost: " + e.getMessage());
        }
        return total;
    }
}
