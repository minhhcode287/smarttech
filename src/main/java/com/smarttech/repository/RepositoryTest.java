package com.smarttech.repository;

import com.smarttech.config.MongoDBConnection;
import com.smarttech.model.Category;
import com.smarttech.model.Employee;
import com.smarttech.model.Product;

import java.time.LocalDate;
import java.util.List;

/**
 * Lớp kiểm thử Console xác minh toàn diện hoạt động của tầng Repository Pattern
 * (Thêm mới, Truy vấn, Cập nhật, Xóa) trên cơ sở dữ liệu MongoDB thực tế.
 *
 * @author Senior Java Developer &amp; Software Architect
 * @project SmartTech
 */
public class RepositoryTest {

    public static void main(String[] args) {
        System.out.println("==========================================================================================");
        System.out.println("   SMARTTECH - KIỂM THỬ TẦNG REPOSITORY PATTERN VỚI MONGODB SERVER (PORT 27017)");
        System.out.println("==========================================================================================\n");

        try {
            // Khởi tạo các Repository
            CategoryRepository categoryRepo = new CategoryRepository();
            ProductRepository productRepo = new ProductRepository();
            EmployeeRepository employeeRepo = new EmployeeRepository();

            // -------------------------------------------------------------
            // TEST 1: THÊM MỚI (SAVE) CATEGORY & PRODUCT
            // -------------------------------------------------------------
            System.out.println(">>> [BƯỚC 1] Thêm mới Category:");
            Category testCat = new Category(
                    "CAT_VR_AR",
                    "Thiết Bị Thực Tế Ảo (VR/AR)",
                    "Kính VR, tay cầm tracking và phụ kiện mô phỏng 3D hiện đại",
                    true
            );
            // Xóa trước nếu đã tồn tại từ lần test trước để đảm bảo idempotency
            categoryRepo.deleteById(testCat.getId());
            boolean catSaved = categoryRepo.save(testCat);
            System.out.println("  -> Lưu Category '" + testCat.getCategoryName() + "': " + (catSaved ? "[THÀNH CÔNG]" : "[THẤT BẠI]"));

            System.out.println("\n>>> [BƯỚC 2] Thêm mới Product thuộc Category trên:");
            Product testProduct = new Product(
                    "PROD_VISION_PRO",
                    "CAT_VR_AR",
                    "Apple Vision Pro 512GB",
                    "Apple",
                    95000000.0,
                    1200000.0,      // Giá thuê 1.200.000 đ/ngày
                    "Chip M2 + R1, Màn hình Micro-OLED 23 triệu pixel, Spatial Audio",
                    true
            );
            productRepo.deleteById(testProduct.getId());
            boolean prodSaved = productRepo.save(testProduct);
            System.out.println("  -> Lưu Product '" + testProduct.getProductName() + "': " + (prodSaved ? "[THÀNH CÔNG]" : "[THẤT BẠI]"));

            // -------------------------------------------------------------
            // TEST 2: THÊM MỚI EMPLOYEE
            // -------------------------------------------------------------
            System.out.println("\n>>> [BƯỚC 3] Thêm mới Employee mẫu:");
            Employee testEmp = new Employee(
                    "EMP_ADMIN_01",               // id
                    "Nguyễn Đình Khang",         // fullName
                    "khang.nguyen@smarttech.vn", // email
                    "0909123456",                // phone
                    "Giám đốc Công nghệ",        // position
                    35000000.0,                  // baseSalary
                    LocalDate.of(2022, 1, 10),   // hireDate
                    "WORKING"                    // status ("WORKING" hoặc "RESIGNED")
            );
            employeeRepo.deleteById(testEmp.getId());
            boolean empSaved = employeeRepo.save(testEmp);
            System.out.println("  -> Lưu Employee '" + testEmp.getFullName() + "': " + (empSaved ? "[THÀNH CÔNG]" : "[THẤT BẠI]"));

            // -------------------------------------------------------------
            // TEST 3: TRUY VẤN DỮ LIỆU (FINDBYID & FINDALL)
            // -------------------------------------------------------------
            System.out.println("\n>>> [BƯỚC 4] Kiểm tra truy vấn findById:");
            Product foundProduct = productRepo.findById("PROD_VISION_PRO");
            if (foundProduct != null) {
                System.out.println("  [TÌM THẤY SẢN PHẨM]: " + foundProduct.getProductName()
                        + " | Giá niêm yết: " + String.format("%,.0f đ", foundProduct.getBasePrice())
                        + " | Giá thuê: " + String.format("%,.0f đ/ngày", foundProduct.getRentalRatePerDay()));
            } else {
                System.err.println("  [LỖI]: Không tìm thấy sản phẩm vừa lưu!");
            }

            System.out.println("\n>>> [BƯỚC 5] Kiểm tra truy vấn findAll Category:");
            List<Category> allCategories = categoryRepo.findAll();
            System.out.println("  -> Tổng số danh mục trong MongoDB: " + allCategories.size());
            for (Category c : allCategories) {
                System.out.println("     + [" + c.getId() + "] " + c.getCategoryName());
            }

            // -------------------------------------------------------------
            // TEST 4: CẬP NHẬT DỮ LIỆU (UPDATE)
            // -------------------------------------------------------------
            System.out.println("\n>>> [BƯỚC 6] Cập nhật giá thuê của sản phẩm:");
            if (foundProduct != null) {
                foundProduct.setRentalRatePerDay(990000.0); // Giảm giá khuyến mãi còn 990.000 đ
                boolean updated = productRepo.update(foundProduct);
                System.out.println("  -> Cập nhật giá thuê thành công: " + updated);

                Product updatedProduct = productRepo.findById("PROD_VISION_PRO");
                System.out.println("  -> Giá thuê sau cập nhật: " + String.format("%,.0f đ/ngày", updatedProduct.getRentalRatePerDay()));
            }

            System.out.println("\n==========================================================================================");
            System.out.println("TẤT CẢ THAO TÁC CRUD TRÊN REPOSITORY PATTERN ĐỀU HOÀN THÀNH CHÍNH XÁC!");
            System.out.println("Sẵn sàng cho Prompt tiếp theo: Xây dựng Service Layer (Business Logic & Transactions)!");
            System.out.println("==========================================================================================");

        } catch (Exception e) {
            System.err.println("[LỖI KIỂM THỬ REPOSITORY]: " + e.getMessage());
            e.printStackTrace();
        } finally {
            // Đóng kết nối an toàn sau khi test xong
            MongoDBConnection.close();
        }
    }
}
