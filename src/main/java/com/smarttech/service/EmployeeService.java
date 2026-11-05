package com.smarttech.service;

import com.smarttech.model.Employee;
import com.smarttech.model.User;
import com.smarttech.repository.EmployeeRepository;
import com.smarttech.repository.UserRepository;

import java.util.List;

/**
 * Service xử lý các nghiệp vụ quản lý Nhân sự, Cấp phát Tài khoản và Phân quyền RBAC.
 * Đảm bảo tính toàn vẹn dữ liệu giữa collection "employees" và "users".
 *
 * @author Senior Java Developer &amp; Software Architect
 * @project SmartTech
 */
public class EmployeeService {

    private final EmployeeRepository employeeRepository;
    private final UserRepository userRepository;

    public EmployeeService() {
        this.employeeRepository = new EmployeeRepository();
        this.userRepository = new UserRepository();
    }

    public List<Employee> getAllEmployees() {
        return employeeRepository.findAll();
    }

    public Employee getEmployeeById(String id) {
        if (id == null || id.trim().isEmpty()) {
            return null;
        }
        return employeeRepository.findById(id.trim());
    }

    public List<Employee> searchEmployees(String keyword) {
        return employeeRepository.search(keyword);
    }

    public User getUserByEmployeeId(String employeeId) {
        if (employeeId == null || employeeId.trim().isEmpty()) {
            return null;
        }
        return userRepository.findByEmployeeId(employeeId.trim());
    }

    public List<User> getAllUsers() {
        return userRepository.findAll();
    }

    public boolean saveOrUpdateEmployeeWithAccount(Employee employee, String username, String password, String role) {
        if (employee == null || employee.getId() == null || employee.getId().trim().isEmpty()) {
            return false;
        }

        // 1. Lưu hoặc Cập nhật vào collection "employees"
        Employee existingEmp = employeeRepository.findById(employee.getId().trim());
        boolean empSaved;
        if (existingEmp != null) {
            empSaved = employeeRepository.update(employee);
        } else {
            empSaved = employeeRepository.save(employee);
        }

        if (!empSaved) {
            return false;
        }

        // 2. Xử lý tài khoản người dùng đăng nhập trong collection "users"
        if (username != null && !username.trim().isEmpty()) {
            String sanitizedUsername = username.trim();
            String assignedRole = (role != null && !role.trim().isEmpty()) ? role.trim().toUpperCase() : "EMPLOYEE";

            User linkedUser = userRepository.findByEmployeeId(employee.getId().trim());
            if (linkedUser == null) {
                User userByUsername = userRepository.findByUsername(sanitizedUsername);
                if (userByUsername != null && !employee.getId().trim().equals(userByUsername.getEmployeeId())) {
                    System.err.println("[EmployeeService] Tên đăng nhập '" + sanitizedUsername + "' đã tồn tại!");
                    return false;
                }

                if (password == null || password.trim().length() < 6) {
                    System.err.println("[EmployeeService] Mật khẩu khởi tạo phải có tối thiểu 6 ký tự!");
                    return false;
                }

                User newUser = new User(
                        sanitizedUsername,
                        password.trim(),
                        employee.getFullName(),
                        employee.getPhone(),
                        employee.getEmail(),
                        assignedRole,
                        employee.getId().trim(),
                        "ACTIVE"
                );
                userRepository.save(newUser);
            } else {
                linkedUser.setUsername(sanitizedUsername);
                linkedUser.setFullName(employee.getFullName());
                linkedUser.setPhone(employee.getPhone());
                linkedUser.setEmail(employee.getEmail());
                linkedUser.setRole(assignedRole);
                if (password != null && password.trim().length() >= 6) {
                    linkedUser.setPassword(password.trim());
                }
                userRepository.update(linkedUser);
            }
        }

        return true;
    }

    public boolean resetPassword(String userId, String newPassword) {
        if (userId == null || newPassword == null || newPassword.trim().length() < 6) {
            return false;
        }

        User user = userRepository.findById(userId.trim());
        if (user == null) {
            user = userRepository.findByEmployeeId(userId.trim());
        }

        if (user == null) {
            return false;
        }

        return userRepository.updatePassword(user.getId(), newPassword.trim());
    }

    public boolean toggleAccountLock(String userId) {
        if (userId == null || userId.trim().isEmpty()) {
            return false;
        }

        User user = userRepository.findById(userId.trim());
        if (user == null) {
            user = userRepository.findByEmployeeId(userId.trim());
        }

        if (user == null) {
            return false;
        }

        String currentStatus = user.getStatus();
        String newStatus = "ACTIVE".equalsIgnoreCase(currentStatus) ? "LOCKED" : "ACTIVE";
        return userRepository.updateStatus(user.getId(), newStatus);
    }

    public boolean deleteEmployee(String employeeId) {
        if (employeeId == null || employeeId.trim().isEmpty()) {
            return false;
        }

        User linkedUser = userRepository.findByEmployeeId(employeeId.trim());
        if (linkedUser != null) {
            userRepository.delete(linkedUser.getId());
        }

        return employeeRepository.delete(employeeId.trim());
    }
}