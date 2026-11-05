package com.smarttech.repository;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.ReplaceOptions;
import com.smarttech.config.MongoDBConnection;
import com.smarttech.model.Employee;
import org.bson.Document;
import org.bson.conversions.Bson;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Repository quản lý chuyên biệt dữ liệu Nhân viên trong collection "employees".
 * Sử dụng kết nối chuẩn: MongoDBConnection.getDatabase().getCollection("employees").
 *
 * @author Senior Java Developer &amp; Software Architect
 * @project SmartTech
 */
public class EmployeeRepository extends AbstractMongoRepository<Employee> {

    private final MongoCollection<Document> employeeCollection;

    public EmployeeRepository() {
        super("employees");
        this.employeeCollection = MongoDBConnection.getDatabase().getCollection("employees");
    }

    @Override
    protected Document toDocument(Employee entity) {
        return entity != null ? entity.toDocument() : null;
    }

    @Override
    protected Employee fromDocument(Document doc) {
        return Employee.fromDocument(doc);
    }

    @Override
    protected String getEntityId(Employee entity) {
        return entity != null ? entity.getId() : null;
    }

    @Override
    public boolean save(Employee employee) {
        if (employee == null) {
            return false;
        }
        try {
            Document doc = toDocument(employee);
            if (employee.getId() != null && !employee.getId().trim().isEmpty()) {
                Bson filter = Filters.or(
                        Filters.eq("_id", employee.getId().trim()),
                        Filters.eq("id", employee.getId().trim())
                );
                employeeCollection.replaceOne(filter, doc, new ReplaceOptions().upsert(true));
            } else {
                employeeCollection.insertOne(doc);
                if (doc.containsKey("_id")) {
                    employee.setId(doc.get("_id").toString());
                }
            }
            return true;
        } catch (Exception e) {
            System.err.println("[EmployeeRepository] Lỗi khi lưu nhân viên: " + e.getMessage());
            return false;
        }
    }

    @Override
    public boolean update(Employee employee) {
        if (employee == null || employee.getId() == null) {
            return false;
        }
        try {
            Bson filter = Filters.or(
                    Filters.eq("_id", employee.getId().trim()),
                    Filters.eq("id", employee.getId().trim())
            );
            Document doc = toDocument(employee);
            var result = employeeCollection.replaceOne(filter, doc);
            return result.getMatchedCount() > 0 || result.getModifiedCount() > 0;
        } catch (Exception e) {
            System.err.println("[EmployeeRepository] Lỗi khi cập nhật nhân viên: " + e.getMessage());
            return false;
        }
    }

    @Override
    public Employee findById(String id) {
        if (id == null || id.trim().isEmpty()) {
            return null;
        }
        try {
            Bson filter = Filters.or(
                    Filters.eq("_id", id.trim()),
                    Filters.eq("id", id.trim())
            );
            Document doc = employeeCollection.find(filter).first();
            return doc != null ? fromDocument(doc) : null;
        } catch (Exception e) {
            System.err.println("[EmployeeRepository] Lỗi findById: " + e.getMessage());
            return null;
        }
    }

    @Override
    public List<Employee> findAll() {
        List<Employee> list = new ArrayList<>();
        try {
            for (Document doc : employeeCollection.find()) {
                Employee emp = fromDocument(doc);
                if (emp != null) {
                    list.add(emp);
                }
            }
        } catch (Exception e) {
            System.err.println("[EmployeeRepository] Lỗi findAll: " + e.getMessage());
        }
        return list;
    }

    public List<Employee> search(String keyword) {
        if (keyword == null || keyword.trim().isEmpty()) {
            return findAll();
        }
        String regex = "(?i)" + Pattern.quote(keyword.trim());
        Bson filter = Filters.or(
                Filters.regex("_id", regex),
                Filters.regex("fullName", regex),
                Filters.regex("phone", regex),
                Filters.regex("email", regex),
                Filters.regex("position", regex),
                Filters.regex("username", regex)
        );

        List<Employee> list = new ArrayList<>();
        try {
            for (Document doc : employeeCollection.find(filter)) {
                Employee emp = fromDocument(doc);
                if (emp != null) {
                    list.add(emp);
                }
            }
        } catch (Exception e) {
            System.err.println("[EmployeeRepository] Lỗi search: " + e.getMessage());
        }
        return list;
    }

    public boolean delete(String id) {
        if (id == null || id.trim().isEmpty()) {
            return false;
        }
        try {
            Bson filter = Filters.or(
                    Filters.eq("_id", id.trim()),
                    Filters.eq("id", id.trim())
            );
            var result = employeeCollection.deleteOne(filter);
            return result.getDeletedCount() > 0;
        } catch (Exception e) {
            System.err.println("[EmployeeRepository] Lỗi khi xóa nhân viên: " + e.getMessage());
            return false;
        }
    }

    @Override
    public boolean deleteById(String id) {
        return delete(id);
    }

    public Employee findByUsername(String username) {
        if (username == null || username.trim().isEmpty()) {
            return null;
        }
        try {
            Document doc = employeeCollection.find(Filters.eq("username", username.trim())).first();
            return doc != null ? fromDocument(doc) : null;
        } catch (Exception e) {
            System.err.println("[EmployeeRepository] Lỗi findByUsername: " + e.getMessage());
            return null;
        }
    }
}