package com.smarttech.service;

import com.smarttech.model.User;

/**
 * Lớp quản lý phiên làm việc hiện tại của người dùng sau khi đăng nhập (UserSession).
 * Áp dụng mô hình thiết kế Singleton để chia sẻ trạng thái đăng nhập xuyên suốt các màn hình Swing.
 *
 * @author Senior Java Developer &amp; Software Architect
 * @project SmartTech
 */
public class UserSession {

    private static volatile UserSession instance = null;

    // Đối tượng User (hoặc Employee) hiện đang đăng nhập
    private User currentUser;

    // Thời điểm bắt đầu phiên đăng nhập
    private long loginTimestamp;

    private UserSession() {
        this.currentUser = null;
        this.loginTimestamp = 0;
    }

    /**
     * Lấy instance duy nhất của UserSession (Thread-safe).
     *
     * @return UserSession instance
     */
    public static UserSession getInstance() {
        if (instance == null) {
            synchronized (UserSession.class) {
                if (instance == null) {
                    instance = new UserSession();
                }
            }
        }
        return instance;
    }

    /**
     * Khởi tạo phiên đăng nhập với người dùng đã được xác thực.
     *
     * @param user Thông tin người dùng đăng nhập
     */
    public void startSession(User user) {
        this.currentUser = user;
        this.loginTimestamp = System.currentTimeMillis();
    }

    /**
     * Huỷ bỏ phiên làm việc khi người dùng đăng xuất.
     */
    public void clearSession() {
        this.currentUser = null;
        this.loginTimestamp = 0;
    }

    /**
     * Kiểm tra trạng thái đã đăng nhập vào hệ thống hay chưa.
     *
     * @return true nếu đã đăng nhập hợp lệ
     */
    public boolean isLoggedIn() {
        return this.currentUser != null && this.currentUser.isActive();
    }

    /**
     * Lấy người dùng hiện tại trong phiên.
     *
     * @return User hoặc null nếu chưa đăng nhập
     */
    public User getCurrentUser() {
        return this.currentUser;
    }

    /**
     * Kiểm tra người dùng hiện tại có vai trò Quản trị viên (ADMIN) không.
     *
     * @return true nếu là ADMIN
     */
    public boolean isAdmin() {
        return isLoggedIn() && "ADMIN".equalsIgnoreCase(this.currentUser.getRole());
    }

    /**
     * Kiểm tra người dùng có quyền cụ thể hay không.
     *
     * @param role Tên quyền cần kiểm tra (ADMIN, EMPLOYEE, CUSTOMER)
     * @return true nếu khớp quyền
     */
    public boolean hasRole(String role) {
        return isLoggedIn() && this.currentUser.getRole() != null
                && this.currentUser.getRole().equalsIgnoreCase(role);
    }

    public long getLoginTimestamp() {
        return loginTimestamp;
    }
}
