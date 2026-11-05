package com.smarttech.config;

import com.mongodb.ConnectionString;
import com.mongodb.MongoClientSettings;
import com.mongodb.MongoException;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.MongoIterable;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Lớp quản trị kết nối cơ sở dữ liệu MongoDB (Singleton Pattern).
 * Đảm bảo chỉ khởi tạo một MongoClient duy nhất trong toàn bộ vòng đời của ứng dụng.
 *
 * @author Senior Java Developer &amp; Enterprise Architect
 * @project SmartTech
 */
public class MongoDBConnection {

    // Chuỗi URI kết nối tới MongoDB Local mặc định
    private static final String CONNECTION_URI = "mongodb://localhost:27017";

    // Tên cơ sở dữ liệu chính của dự án SmartTech
    private static final String DATABASE_NAME = "SmartTechDB";

    // Instance duy nhất của MongoClient (Singleton)
    private static volatile MongoClient mongoClient = null;

    /**
     * Khởi tạo private constructor để ngăn chặn việc tạo đối tượng từ bên ngoài (Singleton Pattern).
     */
    private MongoDBConnection() {
        if (mongoClient != null) {
            throw new IllegalStateException("MongoDBConnection đã được khởi tạo!");
        }
    }

    /**
     * Khởi tạo và trả về instance MongoClient duy nhất (Thread-safe với Double-Checked Locking).
     */
    public static MongoClient getClient() {
        if (mongoClient == null) {
            synchronized (MongoDBConnection.class) {
                if (mongoClient == null) {
                    try {
                        ConnectionString connString = new ConnectionString(CONNECTION_URI);
                        MongoClientSettings settings = MongoClientSettings.builder()
                                .applyConnectionString(connString)
                                .applyToSocketSettings(builder ->
                                        builder.connectTimeout(5000, TimeUnit.MILLISECONDS)
                                                .readTimeout(10000, TimeUnit.MILLISECONDS))
                                .applyToClusterSettings(builder ->
                                        builder.serverSelectionTimeout(5000, TimeUnit.MILLISECONDS))
                                .build();

                        mongoClient = MongoClients.create(settings);

                        // Đăng ký Shutdown Hook tự động đóng kết nối khi tắt JVM
                        Runtime.getRuntime().addShutdownHook(new Thread(MongoDBConnection::close));

                    } catch (Exception e) {
                        System.err.println("[SmartTech] Lỗi trong quá trình khởi tạo MongoClient: " + e.getMessage());
                        throw e;
                    }
                }
            }
        }
        return mongoClient;
    }

    /**
     * Phương thức getInstance() hỗ trợ các Repository gọi trực tiếp lấy MongoDatabase.
     * VD: MongoDBConnection.getInstance().getCollection("...")
     */
    public static MongoDatabase getInstance() {
        return getClient().getDatabase(DATABASE_NAME);
    }

    /**
     * Lấy instance Database của hệ thống SmartTech (SmartTechDB).
     */
    public static MongoDatabase getDatabase() {
        return getClient().getDatabase(DATABASE_NAME);
    }

    /**
     * Đóng kết nối MongoClient an toàn khi ứng dụng kết thúc.
     */
    public static void close() {
        if (mongoClient != null) {
            synchronized (MongoDBConnection.class) {
                if (mongoClient != null) {
                    try {
                        mongoClient.close();
                        System.out.println("[SmartTech] Đã đóng kết nối MongoClient an toàn.");
                    } catch (Exception e) {
                        System.err.println("[SmartTech] Lỗi khi đóng kết nối MongoClient: " + e.getMessage());
                    } finally {
                        mongoClient = null;
                    }
                }
            }
        }
    }
}