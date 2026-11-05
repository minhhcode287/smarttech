package com.smarttech.repository;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Sorts;
import com.smarttech.config.MongoDBConnection;
import com.smarttech.model.Coupon;
import org.bson.Document;
import org.bson.conversions.Bson;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Repository thao tác với cơ sở dữ liệu MongoDB cho thực thể Coupon (Collection: "coupons").
 * Quản lý vòng đời mã voucher, tìm kiếm, cập nhật số lượt sử dụng và lọc mã theo trạng thái.
 *
 * Lưu ý: Kết nối CSDL chuẩn MongoDBConnection.getDatabase().getCollection("coupons").
 *
 * @author Senior Java Developer & Software Architect
 * @project SmartTech
 */
public class CouponRepository {

    public static final String COLLECTION_NAME = "coupons";
    private final MongoCollection<Document> collection;

    public CouponRepository() {
        // Kết nối chuẩn theo kiến trúc Enterprise: MongoDBConnection.getDatabase().getCollection("coupons")
        this.collection = MongoDBConnection.getDatabase().getCollection(COLLECTION_NAME);
    }

    /**
     * Thêm mới một mã Coupon vào cơ sở dữ liệu.
     *
     * @param coupon Đối tượng Coupon cần lưu
     * @return true nếu thêm thành công
     */
    public boolean save(Coupon coupon) {
        if (coupon == null || coupon.getCode() == null || coupon.getCode().trim().isEmpty()) {
            return false;
        }
        try {
            Document doc = coupon.toDocument();
            collection.insertOne(doc);
            return true;
        } catch (Exception e) {
            System.err.println("[CouponRepository] Lỗi save coupon (" + coupon.getCode() + "): " + e.getMessage());
            return false;
        }
    }

    /**
     * Cập nhật thông tin mã Coupon đã tồn tại.
     *
     * @param coupon Đối tượng Coupon cần cập nhật
     * @return true nếu cập nhật thành công
     */
    public boolean update(Coupon coupon) {
        if (coupon == null || coupon.getId() == null) {
            return false;
        }
        try {
            Document doc = coupon.toDocument();
            doc.remove("_id"); // Không cập nhật trường khóa chính _id

            Bson filter = Filters.or(
                    Filters.eq("_id", coupon.getId()),
                    Filters.eq("code", coupon.getCode())
            );

            return collection.updateOne(filter, new Document("$set", doc)).getMatchedCount() > 0;
        } catch (Exception e) {
            System.err.println("[CouponRepository] Lỗi update coupon (" + coupon.getCode() + "): " + e.getMessage());
            return false;
        }
    }

    /**
     * Tìm kiếm mã giảm giá theo ký tự Code (không phân biệt hoa thường).
     *
     * @param code Chuỗi mã voucher (ví dụ: "SUMMER2026")
     * @return Đối tượng Coupon hoặc null nếu không tồn tại
     */
    public Coupon findByCode(String code) {
        if (code == null || code.trim().isEmpty()) {
            return null;
        }
        try {
            Bson filter = Filters.regex("code", "^" + Pattern.quote(code.trim()) + "$", "i");
            Document doc = collection.find(filter).first();
            if (doc != null) {
                return Coupon.fromDocument(doc);
            }
        } catch (Exception e) {
            System.err.println("[CouponRepository] Lỗi findByCode (" + code + "): " + e.getMessage());
        }
        return null;
    }

    /**
     * Tìm kiếm mã coupon theo ID khóa chính (_id).
     *
     * @param id Khóa chính của coupon
     * @return Đối tượng Coupon hoặc null
     */
    public Coupon findById(String id) {
        if (id == null || id.trim().isEmpty()) {
            return null;
        }
        try {
            Document doc = collection.find(Filters.eq("_id", id.trim())).first();
            if (doc != null) {
                return Coupon.fromDocument(doc);
            }
        } catch (Exception e) {
            System.err.println("[CouponRepository] Lỗi findById (" + id + "): " + e.getMessage());
        }
        return null;
    }

    /**
     * Lấy toàn bộ danh sách mã giảm giá trong hệ thống.
     *
     * @return Danh sách coupon
     */
    public List<Coupon> findAll() {
        List<Coupon> list = new ArrayList<>();
        try {
            for (Document doc : collection.find().sort(Sorts.descending("startDate", "_id"))) {
                Coupon c = Coupon.fromDocument(doc);
                if (c != null) {
                    list.add(c);
                }
            }
        } catch (Exception e) {
            System.err.println("[CouponRepository] Lỗi findAll: " + e.getMessage());
        }
        return list;
    }

    /**
     * Xóa một mã giảm giá theo ID khóa chính hoặc Code.
     *
     * @param id Khóa chính hoặc mã code
     * @return true nếu xóa thành công
     */
    public boolean delete(String id) {
        if (id == null || id.trim().isEmpty()) {
            return false;
        }
        try {
            Bson filter = Filters.or(
                    Filters.eq("_id", id.trim()),
                    Filters.eq("code", id.trim().toUpperCase())
            );
            return collection.deleteOne(filter).getDeletedCount() > 0;
        } catch (Exception e) {
            System.err.println("[CouponRepository] Lỗi delete (" + id + "): " + e.getMessage());
            return false;
        }
    }

    public boolean deleteById(String id) {
        return delete(id);
    }

    /**
     * Tăng số lượt sử dụng của một mã coupon khi đơn hàng thanh toán thành công.
     * Tự động chuyển trạng thái sang "EXPIRED" nếu số lượt dùng đạt hoặc vượt ngưỡng tối đa (usageLimit).
     *
     * @param code Mã giảm giá được áp dụng
     * @return true nếu cập nhật thành công
     */
    public boolean incrementUsedCount(String code) {
        if (code == null || code.trim().isEmpty()) {
            return false;
        }
        try {
            Coupon coupon = findByCode(code);
            if (coupon == null) {
                return false;
            }

            int newCount = coupon.getUsedCount() + 1;
            coupon.setUsedCount(newCount);

            if (newCount >= coupon.getUsageLimit()) {
                coupon.setStatus(Coupon.STATUS_EXPIRED);
            }

            return update(coupon);
        } catch (Exception e) {
            System.err.println("[CouponRepository] Lỗi incrementUsedCount (" + code + "): " + e.getMessage());
            return false;
        }
    }

    /**
     * Lấy danh sách các mã giảm giá đang còn hiệu lực kinh doanh
     * (status = "ACTIVE", trong khoảng [startDate, endDate], usedCount < usageLimit).
     *
     * @return Danh sách mã coupon khả dụng
     */
    public List<Coupon> findActiveCoupons() {
        List<Coupon> list = new ArrayList<>();
        try {
            LocalDate today = LocalDate.now();
            Bson filter = Filters.eq("status", Coupon.STATUS_ACTIVE);

            for (Document doc : collection.find(filter)) {
                Coupon c = Coupon.fromDocument(doc);
                if (c != null && c.isValid()) {
                    list.add(c);
                }
            }
        } catch (Exception e) {
            System.err.println("[CouponRepository] Lỗi findActiveCoupons: " + e.getMessage());
        }
        return list;
    }

    /**
     * Phương thức tương thích ngược với phiên bản trước.
     */
    public List<Coupon> findValidCoupons() {
        return findActiveCoupons();
    }

    /**
     * Tìm kiếm mã coupon theo từ khóa (Mã code, Tên chương trình, Trạng thái).
     *
     * @param keyword Từ khóa tìm kiếm
     * @return Danh sách coupon khớp từ khóa
     */
    public List<Coupon> search(String keyword) {
        if (keyword == null || keyword.trim().isEmpty()) {
            return findAll();
        }
        try {
            Pattern pattern = Pattern.compile(Pattern.quote(keyword.trim()), Pattern.CASE_INSENSITIVE);
            Bson filter = Filters.or(
                    Filters.regex("code", pattern),
                    Filters.regex("title", pattern),
                    Filters.regex("status", pattern),
                    Filters.regex("discountType", pattern)
            );

            List<Coupon> list = new ArrayList<>();
            for (Document doc : collection.find(filter).sort(Sorts.descending("startDate", "_id"))) {
                Coupon c = Coupon.fromDocument(doc);
                if (c != null) {
                    list.add(c);
                }
            }
            return list;
        } catch (Exception e) {
            System.err.println("[CouponRepository] Lỗi search: " + e.getMessage());
            return findAll();
        }
    }
}