package com.smarttech.service;

import com.smarttech.model.User;
import com.smarttech.repository.UserRepository;

/**
 * Service xử lý các nghiệp vụ xác thực danh tính (Authentication),
 * đăng xuất, đổi mật khẩu và quản lý bảo mật phiên làm việc.
 *
 * @author Senior Java Developer &amp; Software Architect
 * @project SmartTech
 */
public class AuthService {

    private final UserRepository userRepository;

    public AuthService() {
        this.userRepository = new UserRepository();
    }

    public AuthService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    /**
     * Xử lý đăng nhập hệ thống với tên tài khoản và mật khẩu.
     *
     * @param username Tên đăng nhập
     * @param password Mật khẩu
     * @return true nếu đăng nhập thành công và nạp vào UserSession, ngược lại false
     */
    public boolean login(String username, String password) {
        if (username == null || username.trim().isEmpty() || password == null || password.isEmpty()) {
            System.err.println("[AuthService] Tên đăng nhập và mật khẩu không được để trống!");
            return false;
        }

        User user = userRepository.findByUsername(username.trim());
        if (user == null) {
            System.err.println("[AuthService] Tài khoản không tồn tại trên hệ thống: " + username);
            return false;
        }

        if (!user.isActive()) {
            System.err.println("[AuthService] Tài khoản '" + username + "' đã bị vô hiệu hóa hoặc khóa tạm thời!");
            return false;
        }

        // So khớp mật khẩu
        if (!password.equals(user.getPassword())) {
            System.err.println("[AuthService] Mật khẩu không chính xác cho tài khoản: " + username);
            return false;
        }

        // Đăng nhập thành công -> Nạp vào UserSession
        UserSession.getInstance().startSession(user);
        System.out.println("[AuthService] Đăng nhập thành công! Xin chào " + user.getFullName() + " (" + user.getRole() + ")");
        return true;
    }

    /**
     * Đăng xuất khỏi hệ thống và xoá bỏ UserSession.
     */
    public void logout() {
        if (UserSession.getInstance().isLoggedIn()) {
            System.out.println("[AuthService] Người dùng '" + UserSession.getInstance().getCurrentUser().getUsername() + "' đã đăng xuất.");
        }
        UserSession.getInstance().clearSession();
    }

    /**
     * Đổi mật khẩu cho người dùng hiện đang đăng nhập.
     *
     * @param currentPassword Mật khẩu cũ
     * @param newPassword     Mật khẩu mới (tối thiểu 6 ký tự)
     * @return true nếu đổi thành công
     */
    public boolean changePassword(String currentPassword, String newPassword) {
        if (!UserSession.getInstance().isLoggedIn()) {
            System.err.println("[AuthService] Yêu cầu đăng nhập để thực hiện đổi mật khẩu!");
            return false;
        }

        if (newPassword == null || newPassword.trim().length() < 6) {
            System.err.println("[AuthService] Mật khẩu mới phải có độ dài tối thiểu 6 ký tự!");
            return false;
        }

        User currentUser = UserSession.getInstance().getCurrentUser();
        if (!currentUser.getPassword().equals(currentPassword)) {
            System.err.println("[AuthService] Mật khẩu cũ không chính xác!");
            return false;
        }

        currentUser.setPassword(newPassword.trim());
        boolean updated = userRepository.update(currentUser);
        if (updated) {
            System.out.println("[AuthService] Đổi mật khẩu thành công cho tài khoản: " + currentUser.getUsername());
        }
        return updated;
    }
}
