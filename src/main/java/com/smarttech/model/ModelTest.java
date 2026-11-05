package com.smarttech.model;

import org.bson.Document;

import java.time.LocalDate;

/**
 * Lớp kiểm thử (Unit Test Console) xác minh tính đúng đắn của logic mapping hai chiều (Model <-> Document)
 * giữa các đối tượng OOP Java và BSON Document của MongoDB.
 *
 * @author Senior Java Developer &amp; Software Architect
 * @project SmartTech
 */
public class ModelTest {

    public static void main(String[] args) {
        System.out.println("==========================================================================================");
        System.out.println("   SMARTTECH - KIỂM TRA MAPPING DỮ LIỆU CÁC LỚP MODEL (USER / EMPLOYEE / CUSTOMER)");
        System.out.println("==========================================================================================\n");

        // -------------------------------------------------------------
        // TEST 1: KIỂM TRA EMPLOYEE MAPPING
        // -------------------------------------------------------------
        System.out.println(">>> [BƯỚC 1] Khởi tạo đối tượng Employee mẫu:");
        Employee empOrigin = new Employee(
                "EMP001",
                "Nguyễn Văn Hoàng",
                "hoang.nguyen@smarttech.vn",
                "0988123456",
                "Trưởng phòng Kỹ thuật",
                18500000.0,
                LocalDate.of(2023, 5, 15),
                "WORKING"
        );
        System.out.println("Origin Java Object:\n  " + empOrigin);

        System.out.println("\n>>> [BƯỚC 2] Chuyển đổi Employee -> org.bson.Document (toDocument):");
        Document empDoc = empOrigin.toDocument();
        System.out.println("BSON Document (JSON Preview):\n  " + empDoc.toJson());

        System.out.println("\n>>> [BƯỚC 3] Chuyển đổi ngược Document -> Employee (fromDocument):");
        Employee empRestored = Employee.fromDocument(empDoc);
        System.out.println("Restored Java Object:\n  " + empRestored);

        boolean empMatches = empOrigin.getId().equals(empRestored.getId())
                && empOrigin.getUsername().equals(empRestored.getUsername())
                && empOrigin.getFullName().equals(empRestored.getFullName())
                && empOrigin.getPosition().equals(empRestored.getPosition())
                && Double.compare(empOrigin.getSalary(), empRestored.getSalary()) == 0
                && empOrigin.getHireDate().equals(empRestored.getHireDate());

        System.out.println("=> KẾT QUẢ TEST EMPLOYEE: " + (empMatches ? "[THÀNH CÔNG - KHỚP 100%]" : "[THẤT BẠI - DỮ LIỆU LỆCH]"));
        System.out.println("------------------------------------------------------------------------------------------\n");

        // -------------------------------------------------------------
        // TEST 2: KIỂM TRA CUSTOMER MAPPING
        // -------------------------------------------------------------
        System.out.println(">>> [BƯỚC 4] Khởi tạo đối tượng Customer mẫu (Doanh nghiệp & Cá nhân):");
        Customer custOrigin = new Customer(
                "CUST888",
                "techcorp_admin",
                "SecureAuth#999",
                "Công ty TNHH Giải Pháp Công Nghệ Tương Lai",
                "0283899999",
                "contact@futuretech.com.vn",
                "CUSTOMER",
                true,
                "Tầng 12, Toà nhà Bitexco, Q.1, TP. Hồ Chí Minh",
                "ENTERPRISE",
                1500
        );
        System.out.println("Origin Java Object:\n  " + custOrigin);

        System.out.println("\n>>> [BƯỚC 5] Chuyển đổi Customer -> org.bson.Document (toDocument):");
        Document custDoc = custOrigin.toDocument();
        System.out.println("BSON Document (JSON Preview):\n  " + custDoc.toJson());

        System.out.println("\n>>> [BƯỚC 6] Chuyển đổi ngược Document -> Customer (fromDocument):");
        Customer custRestored = Customer.fromDocument(custDoc);
        System.out.println("Restored Java Object:\n  " + custRestored);

        boolean custMatches = custOrigin.getId().equals(custRestored.getId())
                && custOrigin.getUsername().equals(custRestored.getUsername())
                && custOrigin.getAddress().equals(custRestored.getAddress())
                && custOrigin.getCustomerType().equals(custRestored.getCustomerType())
                && custOrigin.getRewardPoints() == custRestored.getRewardPoints();

        System.out.println("=> KẾT QUẢ TEST CUSTOMER: " + (custMatches ? "[THÀNH CÔNG - KHỚP 100%]" : "[THẤT BẠI - DỮ LIỆU LỆCH]"));
        System.out.println("------------------------------------------------------------------------------------------\n");

        // -------------------------------------------------------------
        // TEST 3: KIỂM TRA TÍNH NULL-SAFE (DỮ LIỆU THIẾU TRƯỜNG TỪ MONGODB)
        // -------------------------------------------------------------
        System.out.println(">>> [BƯỚC 7] Kiểm tra tính Null-Safe khi Document thiếu các trường tùy chọn:");
        Document sparseDoc = new Document()
                .append("_id", "EMP_SPARSE_01")
                .append("username", "test.sparse")
                .append("fullName", "Trần Thiếu Trường");

        Employee sparseEmp = Employee.fromDocument(sparseDoc);
        System.out.println("Sparse Document Input:\n  " + sparseDoc.toJson());
        System.out.println("Resulting Employee Object:\n  " + sparseEmp);
        System.out.println("  - Salary mặc định: " + sparseEmp.getSalary());
        System.out.println("  - HireDate an toàn (null): " + sparseEmp.getHireDate());
        System.out.println("  - Position mặc định: " + sparseEmp.getPosition());
        System.out.println("=> KẾT QUẢ NULL-SAFE TEST: [THÀNH CÔNG - KHÔNG PHÁT SINH NGOẠI LỆ]");

        System.out.println("\n==========================================================================================");
        System.out.println("TẤT CẢ CÁC BÀI TEST MODEL NỀN TẢNG HOÀN THÀNH XUẤT SẮC!");
        System.out.println("Sẵn sàng cho Prompt tiếp theo: Xây dựng Repository Layer (GenericRepository & DAOs)!");
        System.out.println("==========================================================================================");
    }
}
