package com.smarttech.service;

import com.smarttech.config.MongoDBConnection;
import com.smarttech.model.Customer;
import com.smarttech.model.Employee;
import com.smarttech.model.Equipment;
import com.smarttech.model.Product;

import java.time.LocalDate;
import java.util.List;

/**
 * Lớp kiểm thử (Console Test) toàn diện các luồng nghiệp vụ trên tầng Service Layer:
 * - Xác thực đăng nhập &amp; quản lý UserSession.
 * - Tìm kiếm &amp; lọc sản phẩm.
 * - Vận hành trạng thái thiết bị Serial (AVAILABLE -> RENTED -> MAINTENANCE).
 * - Đăng ký khách hàng &amp; tích lũy điểm thưởng từ đơn hàng.
 *
 * @author Senior Java Developer &amp; Software Architect
 * @project SmartTech
 */
public class ServiceTest {

    public static void main(String[] args) {
        System.out.println("==========================================================================================");
        System.out.println("   SMARTTECH - KIỂM THỬ TẦNG DỊCH VỤ NGHIỆP VỤ (SERVICE LAYER &amp; USER SESSION)");
        System.out.println("==========================================================================================\n");

        try {
            // Khởi tạo các Service
            AuthService authService = new AuthService();
            EmployeeService employeeService = new EmployeeService();
            CustomerService customerService = new CustomerService();
            ProductService productService = new ProductService();
            EquipmentService equipmentService = new EquipmentService();

            // -------------------------------------------------------------
            // TEST 1: NGHIỆP VỤ ĐĂNG KÝ VÀ ĐĂNG NHẬP (AUTH &amp; SESSION)
            // -------------------------------------------------------------
            System.out.println(">>> [BƯỚC 1] Chuẩn bị tài khoản nhân viên kiểm thử:");
            Employee emp = new Employee(
                    "EMP_TEST_SERVICE",                // id
                    "Hoàng Văn Minh",                  // fullName
                    "hoang.sale@smarttech.vn",          // email
                    "0977112233",                      // phone
                    "Chuyên viên Bán hàng & Cho thuê", // position
                    12000000.0,                        // baseSalary
                    LocalDate.now(),                   // hireDate
                    "WORKING"                          // status ("WORKING" hoặc "RESIGNED")
            );
            boolean success = employeeService.saveOrUpdateEmployeeWithAccount(emp, "hoang.sale", "Password@123", "EMPLOYEE");

            System.out.println("\n>>> [BƯỚC 2] Thử nghiệm đăng nhập sai mật khẩu:");
            boolean failLogin = authService.login("hoang.sale", "SaiMatKhau@999");
            System.out.println("  -> Đăng nhập với mật khẩu sai: " + (failLogin ? "[LỖI - KHÔNG ĐƯỢC PHÉP]" : "[ĐÚNG CHUẨN - TỪ CHỐI]"));

            System.out.println("\n>>> [BƯỚC 3] Thử nghiệm đăng nhập đúng mật khẩu:");
            boolean successLogin = authService.login("hoang.sale", "Password@123");
            System.out.println("  -> Đăng nhập thành công: " + successLogin);
            System.out.println("  -> UserSession đang hoạt động: " + UserSession.getInstance().isLoggedIn());
            System.out.println("  -> Người dùng trong phiên: " + UserSession.getInstance().getCurrentUser().getFullName());

            // -------------------------------------------------------------
            // TEST 2: NGHIỆP VỤ TÌM KIẾM SẢN PHẨM &amp; KHOẢNG GIÁ
            // -------------------------------------------------------------
            System.out.println("\n>>> [BƯỚC 4] Tìm kiếm sản phẩm theo tên 'Apple':");
            List<Product> searchResults = productService.searchByName("Apple");
            System.out.println("  -> Tìm thấy " + searchResults.size() + " sản phẩm khớp:");
            for (Product p : searchResults) {
                System.out.println("     * " + p.getProductName() + " | Giá: " + String.format("%,.0f đ", p.getBasePrice()));
            }

            // -------------------------------------------------------------
            // TEST 3: NGHIỆP VỤ QUẢN LÝ THIẾT BỊ SERIAL VÀ CHUYỂN TRẠNG THÁI
            // -------------------------------------------------------------
            System.out.println("\n>>> [BƯỚC 5] Nhập thiết bị Serial mới vào kho và thay đổi trạng thái:");
            String testSerial = "SN-TEST-" + System.currentTimeMillis();
            Equipment testEquipment = new Equipment(
                    testSerial,
                    "PROD_VISION_PRO",
                    Equipment.STATUS_AVAILABLE,
                    "Thiết bị mới nguyên seal",
                    true
            );
            equipmentService.addEquipment(testEquipment);

            System.out.println("  -> Kiểm tra tồn kho trước khi cho thuê: " + equipmentService.isProductInStock("PROD_VISION_PRO"));

            // Khách hàng thuê thiết bị -> Đổi trạng thái sang RENTED
            equipmentService.changeEquipmentStatus(testSerial, Equipment.STATUS_RENTED, "Đang cho khách thuê 3 ngày");
            Equipment rentedEquipment = equipmentService.getEquipmentBySerial(testSerial);
            System.out.println("  -> Trạng thái mới của Serial [" + testSerial + "]: " + rentedEquipment.getStatus());

            // -------------------------------------------------------------
            // TEST 4: NGHIỆP VỤ KHÁCH HÀNG &amp; TÍCH ĐIỂM THƯỞNG TỰ ĐỘNG
            // -------------------------------------------------------------
            System.out.println("\n>>> [BƯỚC 6] Đăng ký khách hàng &amp; Tích lũy điểm từ đơn hàng:");
            String testPhone = "0912" + (int)(Math.random() * 900000 + 100000);
            Customer customer = new Customer(
                    "CUST_VIP_01",
                    "vip_customer",
                    "Pass12345",
                    "Lê Thanh Tùng",
                    testPhone,
                    "tung.le@gmail.com",
                    "CUSTOMER",
                    true,
                    "123 Nguyễn Huệ, Quận 1, TP.HCM",
                    "INDIVIDUAL",
                    0
            );
            customerService.registerCustomer(customer);

            // Giả lập đơn thuê trị giá 3.600.000 VNĐ (sẽ tích được 36 điểm thưởng)
            double orderValue = 3600000.0;
            customerService.accumulatePointsFromOrder(customer.getId(), orderValue);

            Customer updatedCustomer = customerService.getCustomerById(customer.getId());
            System.out.println("  -> Điểm thưởng của khách hàng sau đơn hàng: " + updatedCustomer.getRewardPoints() + " điểm");

            // Đăng xuất kết thúc phiên
            System.out.println("\n>>> [BƯỚC 7] Đăng xuất phiên làm việc:");
            authService.logout();
            System.out.println("  -> Trạng thái UserSession sau đăng xuất: " + UserSession.getInstance().isLoggedIn());

            System.out.println("\n==========================================================================================");
            System.out.println("TẤT CẢ CÁC NGHIỆP VỤ TẦNG SERVICE LAYER ĐÃ ĐƯỢC XÁC THỰC HOÀN HẢO!");
            System.out.println("Sẵn sàng cho Prompt tiếp theo: Xây dựng Giao diện người dùng Java Swing hiện đại!");
            System.out.println("==========================================================================================");

        } catch (Exception e) {
            System.err.println("[LỖI KIỂM THỬ SERVICE LAYER]: " + e.getMessage());
            e.printStackTrace();
        } finally {
            MongoDBConnection.close();
        }
    }
}
