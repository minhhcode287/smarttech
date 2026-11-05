package com.smarttech.repository;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.ReplaceOptions;
import com.mongodb.client.model.Updates;
import com.smarttech.config.MongoDBConnection;
import com.smarttech.model.User;
import org.bson.Document;
import org.bson.conversions.Bson;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * Repository quản lý tập trung tài khoản người dùng và phân quyền RBAC trong collection "users".
 * Sử dụng kết nối chuẩn: MongoDBConnection.getDatabase().getCollection("users").
 *
 * @author Senior Java Developer &amp; Software Architect
 * @project SmartTech
 */
public class UserRepository extends AbstractMongoRepository<User> {

    private final MongoCollection<Document> userCollection;

    public UserRepository() {
        super("users");
        this.userCollection = MongoDBConnection.getDatabase().getCollection("users");
    }

    @Override
    protected Document toDocument(User entity) {
        return entity != null ? entity.toDocument() : null;
    }

    @Override
    protected User fromDocument(Document doc) {
        return User.fromDocument(doc);
    }

    @Override
    protected String getEntityId(User entity) {
        return entity != null ? entity.getId() : null;
    }

    @Override
    public boolean save(User user) {
        if (user == null) {
            return false;
        }
        try {
            Document doc = toDocument(user);
            if (user.getId() != null && !user.getId().trim().isEmpty()) {
                Bson filter = Filters.or(
                        Filters.eq("_id", user.getId().trim()),
                        Filters.eq("id", user.getId().trim())
                );
                userCollection.replaceOne(filter, doc, new ReplaceOptions().upsert(true));
            } else {
                userCollection.insertOne(doc);
                if (doc.containsKey("_id")) {
                    user.setId(doc.get("_id").toString());
                }
            }
            return true;
        } catch (Exception e) {
            System.err.println("[UserRepository] Lỗi khi lưu User: " + e.getMessage());
            return false;
        }
    }

    @Override
    public boolean update(User user) {
        if (user == null || user.getId() == null) {
            return false;
        }
        try {
            Bson filter = Filters.or(
                    Filters.eq("_id", user.getId().trim()),
                    Filters.eq("id", user.getId().trim())
            );
            Document doc = toDocument(user);
            var result = userCollection.replaceOne(filter, doc);
            return result.getMatchedCount() > 0 || result.getModifiedCount() > 0;
        } catch (Exception e) {
            System.err.println("[UserRepository] Lỗi cập nhật User: " + e.getMessage());
            return false;
        }
    }

    public User findByUsername(String username) {
        if (username == null || username.trim().isEmpty()) {
            return null;
        }
        try {
            Document doc = userCollection.find(Filters.eq("username", username.trim())).first();
            return doc != null ? fromDocument(doc) : null;
        } catch (Exception e) {
            System.err.println("[UserRepository] Lỗi findByUsername: " + e.getMessage());
            return null;
        }
    }

    @Override
    public User findById(String id) {
        if (id == null || id.trim().isEmpty()) {
            return null;
        }
        try {
            Bson filter = Filters.or(
                    Filters.eq("_id", id.trim()),
                    Filters.eq("id", id.trim())
            );
            Document doc = userCollection.find(filter).first();
            return doc != null ? fromDocument(doc) : null;
        } catch (Exception e) {
            System.err.println("[UserRepository] Lỗi findById: " + e.getMessage());
            return null;
        }
    }

    public User findByEmployeeId(String employeeId) {
        if (employeeId == null || employeeId.trim().isEmpty()) {
            return null;
        }
        try {
            Document doc = userCollection.find(Filters.eq("employeeId", employeeId.trim())).first();
            return doc != null ? fromDocument(doc) : null;
        } catch (Exception e) {
            System.err.println("[UserRepository] Lỗi findByEmployeeId: " + e.getMessage());
            return null;
        }
    }

    @Override
    public List<User> findAll() {
        List<User> list = new ArrayList<>();
        try {
            for (Document doc : userCollection.find()) {
                User u = fromDocument(doc);
                if (u != null) {
                    list.add(u);
                }
            }
        } catch (Exception e) {
            System.err.println("[UserRepository] Lỗi findAll: " + e.getMessage());
        }
        return list;
    }

    public boolean updateStatus(String userId, String status) {
        if (userId == null || status == null) {
            return false;
        }
        try {
            boolean active = "ACTIVE".equalsIgnoreCase(status);
            Bson filter = Filters.or(
                    Filters.eq("_id", userId.trim()),
                    Filters.eq("id", userId.trim())
            );
            Bson update = Updates.combine(
                    Updates.set("status", status.toUpperCase()),
                    Updates.set("active", active)
            );
            var res = userCollection.updateOne(filter, update);
            return res.getMatchedCount() > 0;
        } catch (Exception e) {
            System.err.println("[UserRepository] Lỗi updateStatus: " + e.getMessage());
            return false;
        }
    }

    public boolean updatePassword(String userId, String newPassword) {
        if (userId == null || newPassword == null || newPassword.trim().isEmpty()) {
            return false;
        }
        try {
            Bson filter = Filters.or(
                    Filters.eq("_id", userId.trim()),
                    Filters.eq("id", userId.trim())
            );
            var res = userCollection.updateOne(filter, Updates.set("password", newPassword.trim()));
            return res.getMatchedCount() > 0;
        } catch (Exception e) {
            System.err.println("[UserRepository] Lỗi updatePassword: " + e.getMessage());
            return false;
        }
    }

    public boolean updateLastLogin(String userId) {
        if (userId == null || userId.trim().isEmpty()) {
            return false;
        }
        try {
            Bson filter = Filters.or(
                    Filters.eq("_id", userId.trim()),
                    Filters.eq("id", userId.trim())
            );
            String nowIso = LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
            var res = userCollection.updateOne(filter, Updates.set("lastLogin", nowIso));
            return res.getMatchedCount() > 0;
        } catch (Exception e) {
            System.err.println("[UserRepository] Lỗi updateLastLogin: " + e.getMessage());
            return false;
        }
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
            var result = userCollection.deleteOne(filter);
            return result.getDeletedCount() > 0;
        } catch (Exception e) {
            System.err.println("[UserRepository] Lỗi xóa User: " + e.getMessage());
            return false;
        }
    }

    public User authenticate(String username, String password) {
        if (username == null || password == null) {
            return null;
        }
        try {
            Bson filter = Filters.and(
                    Filters.eq("username", username.trim()),
                    Filters.eq("password", password),
                    Filters.or(
                            Filters.eq("status", "ACTIVE"),
                            Filters.eq("active", true)
                    )
            );
            Document doc = userCollection.find(filter).first();
            if (doc != null) {
                User user = fromDocument(doc);
                if (user != null) {
                    updateLastLogin(user.getId());
                }
                return user;
            }
            return null;
        } catch (Exception e) {
            System.err.println("[UserRepository] Lỗi xác thực người dùng: " + e.getMessage());
            return null;
        }
    }
}