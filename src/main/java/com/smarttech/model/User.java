package com.smarttech.model;

import org.bson.Document;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Date;

/**
 * Lớp User đại diện cho thực thể tài khoản người dùng đăng nhập và phân quyền (RBAC) trong hệ thống SmartTech.
 * Hỗ trợ lưu trữ trực tiếp vào collection "users" hoặc làm lớp cơ sở cho các thực thể người dùng khác.
 *
 * @author Senior Java Developer &amp; Software Architect
 * @project SmartTech
 */
public class User {

    protected String id;
    protected String username;
    protected String password;
    protected String fullName;
    protected String phone;
    protected String email;
    protected String role;             // "ADMIN", "EMPLOYEE", "CUSTOMER"
    protected String employeeId;        // Mã nhân viên liên kết (ví dụ: "NV-001", "EMP-1001")
    protected String status;            // "ACTIVE", "LOCKED"
    protected LocalDateTime lastLogin;  // Thời điểm đăng nhập gần nhất
    protected boolean active;

    private static final DateTimeFormatter ISO_FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

    /**
     * Constructor mặc định không tham số.
     */
    public User() {
        this.role = "EMPLOYEE";
        this.status = "ACTIVE";
        this.active = true;
    }

    /**
     * Constructor tạo mới tài khoản không kèm ID.
     */
    public User(String username, String password, String fullName, String phone, String email,
                String role, String employeeId, String status) {
        this.username = username;
        this.password = password;
        this.fullName = fullName;
        this.phone = phone;
        this.email = email;
        this.role = role != null ? role : "EMPLOYEE";
        this.employeeId = employeeId;
        this.status = status != null ? status : "ACTIVE";
        this.active = "ACTIVE".equalsIgnoreCase(this.status);
    }

    /**
     * Constructor đầy đủ tham số cơ bản (backward compatibility).
     */
    public User(String username, String password, String fullName, String phone, String email, String role, boolean active) {
        this.username = username;
        this.password = password;
        this.fullName = fullName;
        this.phone = phone;
        this.email = email;
        this.role = role != null ? role : "EMPLOYEE";
        this.active = active;
        this.status = active ? "ACTIVE" : "LOCKED";
    }

    /**
     * Constructor đầy đủ tham số bao gồm ID (backward compatibility).
     */
    public User(String id, String username, String password, String fullName, String phone, String email, String role, boolean active) {
        this.id = id;
        this.username = username;
        this.password = password;
        this.fullName = fullName;
        this.phone = phone;
        this.email = email;
        this.role = role != null ? role : "EMPLOYEE";
        this.active = active;
        this.status = active ? "ACTIVE" : "LOCKED";
    }

    /**
     * Constructor đầy đủ toàn bộ thuộc tính của User RBAC.
     */
    public User(String id, String username, String password, String fullName, String phone, String email,
                String role, String employeeId, String status, LocalDateTime lastLogin) {
        this.id = id;
        this.username = username;
        this.password = password;
        this.fullName = fullName;
        this.phone = phone;
        this.email = email;
        this.role = role != null ? role : "EMPLOYEE";
        this.employeeId = employeeId;
        this.status = status != null ? status : "ACTIVE";
        this.active = "ACTIVE".equalsIgnoreCase(this.status);
        this.lastLogin = lastLogin;
    }

    // ================= GETTERS & SETTERS =================

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public String getEmployeeId() {
        return employeeId;
    }

    public void setEmployeeId(String employeeId) {
        this.employeeId = employeeId;
    }

    public String getStatus() {
        if (status == null || status.trim().isEmpty()) {
            return active ? "ACTIVE" : "LOCKED";
        }
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
        this.active = "ACTIVE".equalsIgnoreCase(status);
    }

    public LocalDateTime getLastLogin() {
        return lastLogin;
    }

    public void setLastLogin(LocalDateTime lastLogin) {
        this.lastLogin = lastLogin;
    }

    public boolean isActive() {
        if (status != null) {
            return "ACTIVE".equalsIgnoreCase(status);
        }
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
        this.status = active ? "ACTIVE" : "LOCKED";
    }

    // ================= MAPPING LOGIC (BSON DOCUMENT) =================

    public Document toDocument() {
        Document doc = new Document();
        populateBaseDocument(doc);
        return doc;
    }

    protected Document populateBaseDocument(Document doc) {
        if (this.id != null && !this.id.trim().isEmpty()) {
            doc.append("_id", this.id);
        }
        doc.append("username", this.username != null ? this.username : "")
                .append("password", this.password != null ? this.password : "")
                .append("fullName", this.fullName != null ? this.fullName : "")
                .append("phone", this.phone != null ? this.phone : "")
                .append("email", this.email != null ? this.email : "")
                .append("role", this.role != null ? this.role : "EMPLOYEE")
                .append("employeeId", this.employeeId != null ? this.employeeId : "")
                .append("status", getStatus())
                .append("active", isActive())
                .append("lastLogin", this.lastLogin != null ? this.lastLogin.format(ISO_FORMATTER) : null);
        return doc;
    }

    protected void loadBaseFromDocument(Document doc) {
        if (doc == null) {
            return;
        }

        if (doc.containsKey("_id") && doc.get("_id") != null) {
            this.id = doc.get("_id").toString();
        } else if (doc.containsKey("id") && doc.get("id") != null) {
            this.id = doc.get("id").toString();
        }

        this.username = doc.getString("username");
        this.password = doc.getString("password");
        this.fullName = doc.getString("fullName");
        this.phone = doc.getString("phone");
        this.email = doc.getString("email");
        this.role = doc.getString("role");
        this.employeeId = doc.getString("employeeId");

        if (doc.containsKey("status") && doc.get("status") != null) {
            this.status = doc.getString("status");
            this.active = "ACTIVE".equalsIgnoreCase(this.status);
        } else if (doc.containsKey("active") && doc.get("active") != null) {
            this.active = doc.getBoolean("active", true);
            this.status = this.active ? "ACTIVE" : "LOCKED";
        } else {
            this.status = "ACTIVE";
            this.active = true;
        }

        if (doc.containsKey("lastLogin") && doc.get("lastLogin") != null) {
            Object lastLoginObj = doc.get("lastLogin");
            if (lastLoginObj instanceof String) {
                try {
                    this.lastLogin = LocalDateTime.parse((String) lastLoginObj, ISO_FORMATTER);
                } catch (DateTimeParseException e) {
                    try {
                        this.lastLogin = LocalDateTime.parse((String) lastLoginObj);
                    } catch (Exception ignored) {
                        this.lastLogin = null;
                    }
                }
            } else if (lastLoginObj instanceof Date) {
                this.lastLogin = ((Date) lastLoginObj).toInstant()
                        .atZone(java.time.ZoneId.systemDefault())
                        .toLocalDateTime();
            }
        }
    }

    public static User fromDocument(Document doc) {
        if (doc == null) {
            return null;
        }
        User user = new User();
        user.loadBaseFromDocument(doc);
        return user;
    }

    @Override
    public String toString() {
        return "User{" +
                "id='" + id + '\'' +
                ", username='" + username + '\'' +
                ", fullName='" + fullName + '\'' +
                ", role='" + role + '\'' +
                ", employeeId='" + employeeId + '\'' +
                ", status='" + status + '\'' +
                ", lastLogin=" + lastLogin +
                '}';
    }
}