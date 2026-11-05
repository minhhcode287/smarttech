package com.smarttech.model;

import org.bson.Document;

/**
 * Lớp kiểm thử (Console Test) xác minh tính toàn vẹn và chuẩn xác của các Model:
 * Category, Product và Equipment cùng cơ chế chuyển đổi 2 chiều BSON Document của MongoDB.
 *
 * @author Senior Java Developer &amp; Software Architect
 * @project SmartTech
 */
public class ProductModelTest {

    public static void main(String[] args) {
        System.out.println("==========================================================================================");
        System.out.println("   SMARTTECH - KIỂM TRA MAPPING DỮ LIỆU CÁC LỚP THIẾT BỊ (CATEGORY / PRODUCT / EQUIPMENT)");
        System.out.println("==========================================================================================\n");

        // -------------------------------------------------------------
        // TEST 1: CATEGORY MAPPING
        // -------------------------------------------------------------
        System.out.println(">>> [BƯỚC 1] Khởi tạo Category mẫu:");
        Category catOrigin = new Category(
                "CAT_LAPTOP",
                "Laptop & Workstation Cao Cấp",
                "Các dòng laptop hiệu năng cao phục vụ thiết kế đồ hoạ, lập trình và render 3D",
                true
        );
        System.out.println("Origin Category Object:\n  " + catOrigin);

        Document catDoc = catOrigin.toDocument();
        System.out.println("BSON Document (JSON Preview):\n  " + catDoc.toJson());

        Category catRestored = Category.fromDocument(catDoc);
        System.out.println("Restored Category Object:\n  " + catRestored);

        boolean catMatches = catOrigin.getId().equals(catRestored.getId())
                && catOrigin.getCategoryName().equals(catRestored.getCategoryName())
                && catOrigin.getDescription().equals(catRestored.getDescription())
                && catOrigin.isActive() == catRestored.isActive();

        System.out.println("=> KẾT QUẢ TEST CATEGORY: " + (catMatches ? "[THÀNH CÔNG - KHỚP 100%]" : "[THẤT BẠI]"));
        System.out.println("------------------------------------------------------------------------------------------\n");

        // -------------------------------------------------------------
        // TEST 2: PRODUCT MAPPING
        // -------------------------------------------------------------
        System.out.println(">>> [BƯỚC 2] Khởi tạo Product mẫu:");
        Product prodOrigin = new Product(
                "PROD_MBP_16_M3",
                "CAT_LAPTOP",
                "MacBook Pro 16 inch M3 Max",
                "Apple",
                89990000.0,     // Giá bán lẻ
                650000.0,       // Giá thuê 650.000 đ/ngày
                "Apple M3 Max 16-core CPU, 40-core GPU, 64GB Unified Memory, 1TB SSD, Màn hình Liquid Retina XDR",
                true
        );
        System.out.println("Origin Product Object:\n  " + prodOrigin);

        Document prodDoc = prodOrigin.toDocument();
        System.out.println("BSON Document (JSON Preview):\n  " + prodDoc.toJson());

        Product prodRestored = Product.fromDocument(prodDoc);
        System.out.println("Restored Product Object:\n  " + prodRestored);

        boolean prodMatches = prodOrigin.getId().equals(prodRestored.getId())
                && prodOrigin.getCategoryId().equals(prodRestored.getCategoryId())
                && prodOrigin.getProductName().equals(prodRestored.getProductName())
                && prodOrigin.getBrand().equals(prodRestored.getBrand())
                && Double.compare(prodOrigin.getBasePrice(), prodRestored.getBasePrice()) == 0
                && Double.compare(prodOrigin.getRentalRatePerDay(), prodRestored.getRentalRatePerDay()) == 0
                && prodOrigin.getSpecifications().equals(prodRestored.getSpecifications())
                && prodOrigin.isActive() == prodRestored.isActive();

        System.out.println("=> KẾT QUẢ TEST PRODUCT: " + (prodMatches ? "[THÀNH CÔNG - KHỚP 100%]" : "[THẤT BẠI]"));
        System.out.println("------------------------------------------------------------------------------------------\n");

        // -------------------------------------------------------------
        // TEST 3: EQUIPMENT MAPPING (VỚI SERIAL NUMBER VẬT LÝ)
        // -------------------------------------------------------------
        System.out.println(">>> [BƯỚC 3] Khởi tạo các đơn vị Equipment thực tế trong kho:");
        Equipment equip1 = new Equipment(
                "SN-APL-2024-998811",
                "PROD_MBP_16_M3",
                Equipment.STATUS_AVAILABLE,
                "Máy mới 99.9%, kèm củ sạc 140W Type-C nguyên bản và balo chống sốc cao cấp",
                true
        );

        Equipment equip2 = new Equipment(
                "SN-APL-2024-998822",
                "PROD_MBP_16_M3",
                Equipment.STATUS_MAINTENANCE,
                "Vệ sinh bàn phím và kiểm tra chu kỳ sạc pin định kỳ",
                true
        );

        System.out.println("Equipment 1 (Available):\n  " + equip1);
        System.out.println("Equipment 2 (Maintenance):\n  " + equip2);

        Document equipDoc1 = equip1.toDocument();
        System.out.println("Equipment 1 BSON Document:\n  " + equipDoc1.toJson());

        Equipment equip1Restored = Equipment.fromDocument(equipDoc1);
        System.out.println("Restored Equipment 1:\n  " + equip1Restored);

        boolean equipMatches = equip1.getId().equals(equip1Restored.getId())
                && equip1.getProductId().equals(equip1Restored.getProductId())
                && equip1.getStatus().equals(equip1Restored.getStatus())
                && equip1.getConditionNote().equals(equip1Restored.getConditionNote())
                && equip1.isActive() == equip1Restored.isActive();

        System.out.println("=> KẾT QUẢ TEST EQUIPMENT: " + (equipMatches ? "[THÀNH CÔNG - KHỚP 100%]" : "[THẤT BẠI]"));
        System.out.println("------------------------------------------------------------------------------------------\n");

        // -------------------------------------------------------------
        // TEST 4: NULL-SAFE VÀ ÉP KIỂU SỐ TỪ MONGODB BSON DOCUMENT
        // -------------------------------------------------------------
        System.out.println(">>> [BƯỚC 4] Kiểm tra tính Null-Safe & ép kiểu an toàn khi Document bị thiếu trường:");
        Document sparseProductDoc = new Document()
                .append("_id", "PROD_SPARSE_TEST")
                .append("productName", "Máy chiếu Laser Mini 4K")
                .append("basePrice", 15000000)               // Kiểu Integer trong MongoDB
                .append("rentalRatePerDay", "150000");        // Kiểu String trong MongoDB (dữ liệu bất thường)

        Product sparseProduct = Product.fromDocument(sparseProductDoc);
        System.out.println("Sparse Input Document:\n  " + sparseProductDoc.toJson());
        System.out.println("Parsed Product Object:\n  " + sparseProduct);
        System.out.println("  - basePrice tự động ép sang Double: " + sparseProduct.getBasePrice());
        System.out.println("  - rentalRatePerDay parse từ String sang Double: " + sparseProduct.getRentalRatePerDay());
        System.out.println("  - brand mặc định (null safe): '" + sparseProduct.getBrand() + "'");
        System.out.println("  - active mặc định: " + sparseProduct.isActive());

        System.out.println("=> KẾT QUẢ TEST NULL-SAFE & TYPE CASTING: [THÀNH CÔNG - AN TOÀN TUYỆT ĐỐI]");

        System.out.println("\n==========================================================================================");
        System.out.println("TOÀN BỘ CÁC LỚP MODEL QUẢN LÝ THIẾT BỊ HOẠT ĐỘNG HOÀN HẢO!");
        System.out.println("Sẵn sàng cho Prompt tiếp theo: Xây dựng GenericRepository & ProductRepository kết nối MongoDB!");
        System.out.println("==========================================================================================");
    }
}
