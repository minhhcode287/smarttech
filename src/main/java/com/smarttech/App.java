package com.smarttech;

import com.smarttech.model.Employee;
import com.smarttech.repository.EmployeeRepository;
import com.smarttech.repository.UserRepository;
import com.smarttech.view.LoginFrame;
import com.smarttech.view.util.UIUtils;

import javax.swing.*;
import java.time.LocalDate;

public class App {

    public static void main(String[] args) {
        System.out.println("======================================================================");
        System.out.println("   KHỞI ĐỘNG HỆ THỐNG SMARTTECH - QUẢN LÝ BÁN HÀNG & CHO THUÊ");
        System.out.println("   PHIÊN BẢN: 1.0 ENTERPRISE • JAVA 21 & SWING MODERN UI");
        System.out.println("======================================================================");

        // 1. Cấu hình Look and Feel
        UIUtils.setupLookAndFeel();

        // 2. Ép tạo tài khoản Admin chuẩn
        initDefaultAdminAccount();

        // 3. Khởi chạy giao diện Đăng nhập
        SwingUtilities.invokeLater(() -> {
            try {
                LoginFrame loginFrame = new LoginFrame();
                loginFrame.setVisible(true);
                System.out.println("[App] Màn hình đăng nhập đã sẵn sàng tiếp nhận người dùng.");
            } catch (Exception e) {
                System.err.println("[App] Lỗi khi khởi động giao diện Swing: " + e.getMessage());
            }
        });
    }

    private static void initDefaultAdminAccount() {
        try {
            UserRepository userRepo = new UserRepository();

            // Khởi tạo đối tượng admin
            // Khởi tạo đối tượng admin (chuẩn 8 tham số của Employee)
            Employee admin = new Employee(
                    "EMP_ADMIN_01",             // id
                    "Quản Trị Viên Hệ Thống",  // fullName
                    "admin@smarttech.vn",       // email
                    "0909123456",               // phone
                    "Giám đốc công nghệ",       // position
                    25000000.0,                 // baseSalary
                    LocalDate.now(),            // hireDate
                    "WORKING"                   // status
            );


            // Xóa tài khoản admin cũ nếu có để tránh lỗi trùng ID
            userRepo.deleteById("EMP_ADMIN_01");

            // Lưu admin vào collection 'users' (nơi AuthService thực hiện đăng nhập)
            boolean saved = userRepo.save(admin);
            if (saved) {
                System.out.println("[App] >>> ĐÃ TẠO TÀI KHOẢN ADMIN VÀO USERS THÀNH CÔNG: admin / admin123 <<<");
            }
        } catch (Exception e) {
            System.err.println("[App] Lỗi khởi tạo Admin: " + e.getMessage());
        }
    }
}