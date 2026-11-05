package com.smarttech.model;

import org.bson.Document;

import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeParseException;
import java.util.Date;

/**
 * Lớp Employee đại diện cho thực thể Nhân viên trong hệ thống SmartTech.
 * Quản lý hồ sơ nhân sự, chức vụ, mức lương cơ bản, ngày vào làm và trạng thái công tác.
 *
 * @author Senior Java Developer &amp; Software Architect
 * @project SmartTech
 */
public class Employee extends User {

    private String position;        // Chức vụ: "Bán hàng", "Kỹ thuật", "Thủ kho", "Kế toán", "Quản lý"
    private double baseSalary;      // Lương cơ bản
    private LocalDate hireDate;     // Ngày vào làm
    private String employeeStatus;  // "WORKING" - Đang làm việc, "RESIGNED" - Đã nghỉ việc

    public Employee() {
        super();
        this.role = "EMPLOYEE";
        this.position = "Bán hàng";
        this.baseSalary = 10000000.0;
        this.hireDate = LocalDate.now();
        this.employeeStatus = "WORKING";
    }

    public Employee(String id, String fullName, String email, String phone,
                    String position, double baseSalary, LocalDate hireDate, String status) {
        super();
        this.id = id;
        this.fullName = fullName;
        this.email = email;
        this.phone = phone;
        this.position = position;
        this.baseSalary = baseSalary;
        this.hireDate = hireDate != null ? hireDate : LocalDate.now();
        this.employeeStatus = status != null ? status : "WORKING";
    }

    public Employee(String id, String username, String password, String fullName, String phone,
                    String email, String role, boolean active, String position, double baseSalary,
                    LocalDate hireDate, String employeeStatus) {
        super(id, username, password, fullName, phone, email, role, active);
        this.position = position;
        this.baseSalary = baseSalary;
        this.hireDate = hireDate != null ? hireDate : LocalDate.now();
        this.employeeStatus = employeeStatus != null ? employeeStatus : "WORKING";
    }

    // ================= GETTERS & SETTERS =================

    public String getPosition() {
        return position;
    }

    public void setPosition(String position) {
        this.position = position;
    }

    public double getBaseSalary() {
        return baseSalary;
    }

    public void setBaseSalary(double baseSalary) {
        this.baseSalary = baseSalary;
    }

    public double getSalary() {
        return baseSalary;
    }

    public void setSalary(double salary) {
        this.baseSalary = salary;
    }

    public LocalDate getHireDate() {
        return hireDate;
    }

    public void setHireDate(LocalDate hireDate) {
        this.hireDate = hireDate;
    }

    public String getEmployeeStatus() {
        return employeeStatus != null ? employeeStatus : "WORKING";
    }

    public void setEmployeeStatus(String employeeStatus) {
        this.employeeStatus = employeeStatus;
    }

    @Override
    public String getStatus() {
        if (employeeStatus != null && !employeeStatus.trim().isEmpty()) {
            return employeeStatus;
        }
        return super.getStatus();
    }

    @Override
    public void setStatus(String status) {
        if ("WORKING".equalsIgnoreCase(status) || "RESIGNED".equalsIgnoreCase(status)) {
            this.employeeStatus = status;
        } else {
            super.setStatus(status);
        }
    }

    public boolean isWorking() {
        return "WORKING".equalsIgnoreCase(getEmployeeStatus());
    }

    // ================= MAPPING LOGIC (BSON DOCUMENT) =================

    @Override
    public Document toDocument() {
        Document doc = new Document();
        if (this.id != null && !this.id.trim().isEmpty()) {
            doc.append("_id", this.id);
        }
        doc.append("fullName", this.fullName != null ? this.fullName : "")
                .append("email", this.email != null ? this.email : "")
                .append("phone", this.phone != null ? this.phone : "")
                .append("position", this.position != null ? this.position : "Bán hàng")
                .append("baseSalary", this.baseSalary)
                .append("salary", this.baseSalary)
                .append("hireDate", this.hireDate != null ? this.hireDate.toString() : LocalDate.now().toString())
                .append("status", getEmployeeStatus())
                .append("employeeStatus", getEmployeeStatus());

        if (this.username != null && !this.username.trim().isEmpty()) {
            doc.append("username", this.username)
                    .append("password", this.password != null ? this.password : "")
                    .append("role", this.role != null ? this.role : "EMPLOYEE")
                    .append("active", isActive());
        }

        return doc;
    }

    public static Employee fromDocument(Document doc) {
        if (doc == null) {
            return null;
        }

        Employee employee = new Employee();

        if (doc.containsKey("_id") && doc.get("_id") != null) {
            employee.setId(doc.get("_id").toString());
        } else if (doc.containsKey("id") && doc.get("id") != null) {
            employee.setId(doc.get("id").toString());
        }

        employee.setFullName(doc.getString("fullName"));
        employee.setEmail(doc.getString("email"));
        employee.setPhone(doc.getString("phone"));
        employee.setPosition(doc.getString("position"));

        if (doc.containsKey("baseSalary") && doc.get("baseSalary") != null) {
            Object obj = doc.get("baseSalary");
            employee.setBaseSalary(obj instanceof Number ? ((Number) obj).doubleValue() : 0.0);
        } else if (doc.containsKey("salary") && doc.get("salary") != null) {
            Object obj = doc.get("salary");
            employee.setBaseSalary(obj instanceof Number ? ((Number) obj).doubleValue() : 0.0);
        }

        if (doc.containsKey("hireDate") && doc.get("hireDate") != null) {
            Object dateObj = doc.get("hireDate");
            if (dateObj instanceof String) {
                try {
                    employee.setHireDate(LocalDate.parse((String) dateObj));
                } catch (DateTimeParseException e) {
                    employee.setHireDate(LocalDate.now());
                }
            } else if (dateObj instanceof Date) {
                employee.setHireDate(((Date) dateObj).toInstant()
                        .atZone(ZoneId.systemDefault())
                        .toLocalDate());
            }
        }

        if (doc.containsKey("employeeStatus") && doc.get("employeeStatus") != null) {
            employee.setEmployeeStatus(doc.getString("employeeStatus"));
        } else if (doc.containsKey("status") && doc.get("status") != null) {
            String st = doc.getString("status");
            if ("WORKING".equalsIgnoreCase(st) || "RESIGNED".equalsIgnoreCase(st)) {
                employee.setEmployeeStatus(st);
            } else {
                employee.setEmployeeStatus("WORKING");
            }
        }

        employee.setUsername(doc.getString("username"));
        employee.setPassword(doc.getString("password"));
        employee.setRole(doc.getString("role") != null ? doc.getString("role") : "EMPLOYEE");
        if (doc.containsKey("active") && doc.get("active") != null) {
            employee.setActive(doc.getBoolean("active", true));
        }

        return employee;
    }
}