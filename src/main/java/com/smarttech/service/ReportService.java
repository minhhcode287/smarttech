package com.smarttech.service;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Sorts;
import com.smarttech.config.MongoDBConnection;
import com.smarttech.model.ImportOrder;
import com.smarttech.model.MaintenanceTicket;
import com.smarttech.model.Order;
import com.smarttech.model.OrderItem;
import com.smarttech.model.Product;
import com.smarttech.model.RentalContract;
import com.smarttech.model.RentalItem;
import com.smarttech.repository.ProductRepository;
import org.bson.Document;
import org.bson.conversions.Bson;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * Service xử lý toàn diện các nghiệp vụ Báo Cáo Thống Kê, Phân Tích Doanh Thu
 * và Xuất Dữ Liệu Kiểm Toán (Reports, Analytics Dashboard & Data Export System).
 * Tuân thủ chuẩn kết nối MongoDBConnection.getDatabase().getCollection(...).
 *
 * @author Senior Java Developer & Software Architect (30 years ERP/POS experience)
 * @project SmartTech
 */
public class ReportService {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    private final MongoCollection<Document> orderCollection;
    private final MongoCollection<Document> rentalCollection;
    private final MongoCollection<Document> maintenanceCollection;
    private final MongoCollection<Document> importCollection;
    private final ProductRepository productRepository;

    public ReportService() {
        this.orderCollection = MongoDBConnection.getDatabase().getCollection("orders");
        this.rentalCollection = MongoDBConnection.getDatabase().getCollection("rental_contracts");
        this.maintenanceCollection = MongoDBConnection.getDatabase().getCollection("maintenance_tickets");
        this.importCollection = MongoDBConnection.getDatabase().getCollection("import_orders");
        this.productRepository = new ProductRepository();
    }

    // =========================================================================
    // DTOs & DATA STRUCTURES
    // =========================================================================

    /**
     * DTO tổng hợp toàn bộ số liệu thống kê tài chính trong một khoảng thời gian.
     */
    public static class RevenueReportData {
        private double totalSaleRevenue;
        private double totalRentalRevenue;
        private double totalMaintenanceCost;
        private double totalImportCost;
        private double grossProfit;

        private int totalOrdersCount;
        private int totalRentalsCount;
        private int totalMaintenanceCount;
        private int totalImportsCount;

        private List<DailyRevenuePoint> dailyPoints = new ArrayList<>();
        private List<ReportTransaction> transactions = new ArrayList<>();

        public double getTotalSaleRevenue() { return totalSaleRevenue; }
        public void setTotalSaleRevenue(double totalSaleRevenue) { this.totalSaleRevenue = totalSaleRevenue; }

        public double getTotalRentalRevenue() { return totalRentalRevenue; }
        public void setTotalRentalRevenue(double totalRentalRevenue) { this.totalRentalRevenue = totalRentalRevenue; }

        public double getTotalMaintenanceCost() { return totalMaintenanceCost; }
        public void setTotalMaintenanceCost(double totalMaintenanceCost) { this.totalMaintenanceCost = totalMaintenanceCost; }

        public double getTotalImportCost() { return totalImportCost; }
        public void setTotalImportCost(double totalImportCost) { this.totalImportCost = totalImportCost; }

        public double getGrossProfit() { return grossProfit; }
        public void setGrossProfit(double grossProfit) { this.grossProfit = grossProfit; }

        public double getTotalRevenue() { return totalSaleRevenue + totalRentalRevenue; }
        public double getTotalCost() { return totalMaintenanceCost + totalImportCost; }

        public int getTotalOrdersCount() { return totalOrdersCount; }
        public void setTotalOrdersCount(int totalOrdersCount) { this.totalOrdersCount = totalOrdersCount; }

        public int getTotalRentalsCount() { return totalRentalsCount; }
        public void setTotalRentalsCount(int totalRentalsCount) { this.totalRentalsCount = totalRentalsCount; }

        public int getTotalMaintenanceCount() { return totalMaintenanceCount; }
        public void setTotalMaintenanceCount(int totalMaintenanceCount) { this.totalMaintenanceCount = totalMaintenanceCount; }

        public int getTotalImportsCount() { return totalImportsCount; }
        public void setTotalImportsCount(int totalImportsCount) { this.totalImportsCount = totalImportsCount; }

        public List<DailyRevenuePoint> getDailyPoints() { return dailyPoints; }
        public void setDailyPoints(List<DailyRevenuePoint> dailyPoints) { this.dailyPoints = dailyPoints; }

        public List<ReportTransaction> getTransactions() { return transactions; }
        public void setTransactions(List<ReportTransaction> transactions) { this.transactions = transactions; }
    }

    /**
     * DTO điểm dữ liệu xu hướng doanh thu theo ngày để vẽ biểu đồ Graphics.
     */
    public static class DailyRevenuePoint {
        private String dateLabel;
        private LocalDate date;
        private double saleRevenue;
        private double rentalRevenue;
        private double totalRevenue;

        public DailyRevenuePoint(String dateLabel, LocalDate date, double saleRevenue, double rentalRevenue) {
            this.dateLabel = dateLabel;
            this.date = date;
            this.saleRevenue = saleRevenue;
            this.rentalRevenue = rentalRevenue;
            this.totalRevenue = saleRevenue + rentalRevenue;
        }

        public String getDateLabel() { return dateLabel; }
        public LocalDate getDate() { return date; }
        public double getSaleRevenue() { return saleRevenue; }
        public double getRentalRevenue() { return rentalRevenue; }
        public double getTotalRevenue() { return totalRevenue; }
    }

    /**
     * DTO thông tin từng giao dịch chi tiết phục vụ bảng số liệu Tab 2.
     */
    public static class ReportTransaction {
        private String dateStr;
        private String type;         // "Bán Hàng", "Cho Thuê", "Bảo Trì", "Nhập Kho"
        private String code;         // ID hóa đơn/hợp đồng
        private String customerName; // Tên KH hoặc Nhà cung cấp
        private double revenue;
        private double cost;
        private double profit;

        public ReportTransaction(String dateStr, String type, String code, String customerName, double revenue, double cost, double profit) {
            this.dateStr = dateStr;
            this.type = type;
            this.code = code;
            this.customerName = customerName;
            this.revenue = revenue;
            this.cost = cost;
            this.profit = profit;
        }

        public String getDateStr() { return dateStr; }
        public String getType() { return type; }
        public String getCode() { return code; }
        public String getCustomerName() { return customerName; }
        public double getRevenue() { return revenue; }
        public double getCost() { return cost; }
        public double getProfit() { return profit; }
    }

    /**
     * DTO bảng xếp hạng Top sản phẩm bán/cho thuê chạy nhất.
     */
    public static class TopProductReport {
        private String productId;
        private String productName;
        private String brand;
        private int quantitySold;
        private int rentalCount;
        private double totalRevenue;

        public TopProductReport(String productId, String productName, String brand, int quantitySold, int rentalCount, double totalRevenue) {
            this.productId = productId;
            this.productName = productName;
            this.brand = brand;
            this.quantitySold = quantitySold;
            this.rentalCount = rentalCount;
            this.totalRevenue = totalRevenue;
        }

        public String getProductId() { return productId; }
        public String getProductName() { return productName; }
        public String getBrand() { return brand; }
        public int getQuantitySold() { return quantitySold; }
        public int getRentalCount() { return rentalCount; }
        public double getTotalRevenue() { return totalRevenue; }
    }

    // =========================================================================
    // CORE AGGREGATION & REPORT METHODS
    // =========================================================================

    /**
     * Tổng hợp báo cáo doanh thu, chi phí, lợi nhuận và chi tiết giao dịch
     * trong khoảng thời gian từ fromDate đến toDate (bao gồm cả 2 mốc).
     */
    public RevenueReportData getRevenueReport(LocalDate fromDate, LocalDate toDate) {
        RevenueReportData report = new RevenueReportData();

        if (fromDate == null) fromDate = LocalDate.now().minusDays(30);
        if (toDate == null) toDate = LocalDate.now();
        if (fromDate.isAfter(toDate)) {
            LocalDate temp = fromDate;
            fromDate = toDate;
            toDate = temp;
        }

        String fromStr = fromDate.format(DATE_FORMATTER);
        String toStr = toDate.format(DATE_FORMATTER);

        double totalSale = 0.0;
        double totalRental = 0.0;
        double totalMaintenance = 0.0;
        double totalImport = 0.0;

        int orderCount = 0;
        int rentalCount = 0;
        int maintenanceCount = 0;
        int importCount = 0;

        Map<String, Double> dailySalesMap = new LinkedHashMap<>();
        Map<String, Double> dailyRentalsMap = new LinkedHashMap<>();

        // 1. Doanh Thu Bán Hàng từ collection 'orders'
        try {
            Bson orderFilter = Filters.and(
                    Filters.gte("orderDate", fromStr),
                    Filters.lte("orderDate", toStr),
                    Filters.ne("status", "CANCELLED")
            );

            for (Document doc : orderCollection.find(orderFilter).sort(Sorts.descending("orderDate"))) {
                Order order = Order.fromDocument(doc);
                if (order != null) {
                    double finalAmt = order.getFinalAmount();
                    totalSale += finalAmt;
                    orderCount++;

                    String dateKey = order.getOrderDate() != null ? order.getOrderDate().format(DATE_FORMATTER) : fromStr;
                    dailySalesMap.put(dateKey, dailySalesMap.getOrDefault(dateKey, 0.0) + finalAmt);

                    report.getTransactions().add(new ReportTransaction(
                            dateKey,
                            "Bán Hàng",
                            order.getId(),
                            order.getCustomerName() != null ? order.getCustomerName() : "Khách lẻ",
                            finalAmt,
                            0.0,
                            finalAmt
                    ));
                }
            }
        } catch (Exception e) {
            System.err.println("[ReportService] Lỗi đọc đơn hàng: " + e.getMessage());
        }

        // 2. Doanh Thu Cho Thuê từ collection 'rental_contracts'
        try {
            Bson rentalFilter = Filters.and(
                    Filters.gte("startDate", fromStr),
                    Filters.lte("startDate", toStr),
                    Filters.ne("status", "CANCELLED")
            );

            for (Document doc : rentalCollection.find(rentalFilter).sort(Sorts.descending("startDate"))) {
                RentalContract contract = RentalContract.fromDocument(doc);
                if (contract != null) {
                    double fee = contract.getTotalRentalFee();
                    totalRental += fee;
                    rentalCount++;

                    String dateKey = contract.getStartDate() != null ? contract.getStartDate().format(DATE_FORMATTER) : fromStr;
                    dailyRentalsMap.put(dateKey, dailyRentalsMap.getOrDefault(dateKey, 0.0) + fee);

                    report.getTransactions().add(new ReportTransaction(
                            dateKey,
                            "Cho Thuê",
                            contract.getId(),
                            contract.getCustomerName() != null ? contract.getCustomerName() : "Khách thuê",
                            fee,
                            0.0,
                            fee
                    ));
                }
            }
        } catch (Exception e) {
            System.err.println("[ReportService] Lỗi đọc hợp đồng thuê: " + e.getMessage());
        }

        // 3. Chi Phí Bảo Trì từ collection 'maintenance_tickets'
        try {
            Bson mntFilter = Filters.and(
                    Filters.gte("startDate", fromStr),
                    Filters.lte("startDate", toStr),
                    Filters.ne("status", "CANCELLED")
            );

            for (Document doc : maintenanceCollection.find(mntFilter).sort(Sorts.descending("startDate"))) {
                MaintenanceTicket ticket = MaintenanceTicket.fromDocument(doc);
                if (ticket != null) {
                    double cost = ticket.getRepairCost();
                    totalMaintenance += cost;
                    maintenanceCount++;

                    String dateKey = ticket.getStartDate() != null ? ticket.getStartDate().format(DATE_FORMATTER) : fromStr;
                    report.getTransactions().add(new ReportTransaction(
                            dateKey,
                            "Bảo Trì",
                            ticket.getId(),
                            ticket.getRepairVendor() != null ? ticket.getRepairVendor() : "Đơn vị bảo trì",
                            0.0,
                            cost,
                            -cost
                    ));
                }
            }
        } catch (Exception e) {
            System.err.println("[ReportService] Lỗi đọc phiếu bảo trì: " + e.getMessage());
        }

        // 4. Chi Phí Nhập Kho từ collection 'import_orders'
        try {
            Bson impFilter = Filters.and(
                    Filters.gte("importDate", fromStr),
                    Filters.lte("importDate", toStr),
                    Filters.ne("status", "CANCELLED")
            );

            for (Document doc : importCollection.find(impFilter).sort(Sorts.descending("importDate"))) {
                ImportOrder imp = ImportOrder.fromDocument(doc);
                if (imp != null) {
                    double cost = imp.getTotalAmount();
                    totalImport += cost;
                    importCount++;

                    String dateKey = imp.getImportDate() != null ? imp.getImportDate().format(DATE_FORMATTER) : fromStr;
                    report.getTransactions().add(new ReportTransaction(
                            dateKey,
                            "Nhập Kho",
                            imp.getId(),
                            imp.getSupplierName() != null ? imp.getSupplierName() : "Nhà cung cấp",
                            0.0,
                            cost,
                            -cost
                    ));
                }
            }
        } catch (Exception e) {
            System.err.println("[ReportService] Lỗi đọc phiếu nhập kho: " + e.getMessage());
        }

        // 5. Tính toán các chỉ số tổng hợp
        report.setTotalSaleRevenue(totalSale);
        report.setTotalRentalRevenue(totalRental);
        report.setTotalMaintenanceCost(totalMaintenance);
        report.setTotalImportCost(totalImport);
        report.setGrossProfit((totalSale + totalRental) - (totalMaintenance + totalImport));

        report.setTotalOrdersCount(orderCount);
        report.setTotalRentalsCount(rentalCount);
        report.setTotalMaintenanceCount(maintenanceCount);
        report.setTotalImportsCount(importCount);

        // 6. Xây dựng danh sách điểm biểu đồ xu hướng (DailyRevenuePoints)
        List<DailyRevenuePoint> dailyPoints = new ArrayList<>();
        LocalDate curr = fromDate;
        while (!curr.isAfter(toDate)) {
            String key = curr.format(DATE_FORMATTER);
            double sale = dailySalesMap.getOrDefault(key, 0.0);
            double rent = dailyRentalsMap.getOrDefault(key, 0.0);
            dailyPoints.add(new DailyRevenuePoint(curr.format(DateTimeFormatter.ofPattern("dd/MM")), curr, sale, rent));
            curr = curr.plusDays(1);
        }
        report.setDailyPoints(dailyPoints);

        // Sắp xếp chi tiết giao dịch mới nhất lên đầu
        report.getTransactions().sort((a, b) -> b.getDateStr().compareTo(a.getDateStr()));

        return report;
    }

    /**
     * Lấy danh sách Top sản phẩm có doanh thu và số lượt bán/cho thuê cao nhất.
     *
     * @param limit Giới hạn số lượng kết quả (ví dụ: 5)
     * @return Danh sách TopProductReport
     */
    public List<TopProductReport> getTopSellingProducts(int limit) {
        Map<String, TopProductReport> productStats = new HashMap<>();

        // Nạp tất cả sản phẩm hiện có
        try {
            List<Product> allProducts = productRepository.findAll();
            for (Product p : allProducts) {
                productStats.put(p.getId(), new TopProductReport(
                        p.getId(),
                        p.getProductName(),
                        p.getBrand() != null ? p.getBrand() : "SmartTech",
                        0,
                        0,
                        0.0
                ));
            }
        } catch (Exception e) {
            System.err.println("[ReportService] Lỗi tải sản phẩm: " + e.getMessage());
        }

        // Duyệt đơn hàng bán lẻ
        try {
            for (Document doc : orderCollection.find(Filters.ne("status", "CANCELLED"))) {
                Order order = Order.fromDocument(doc);
                if (order != null && order.getItems() != null) {
                    for (OrderItem item : order.getItems()) {
                        String pid = item.getProductId();
                        TopProductReport stat = productStats.get(pid);
                        if (stat == null) {
                            stat = new TopProductReport(pid, item.getProductName(), "Thiết bị", 0, 0, 0.0);
                            productStats.put(pid, stat);
                        }
                        stat.quantitySold += 1;
                        stat.totalRevenue += item.getUnitPrice();
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("[ReportService] Lỗi thống kê đơn hàng: " + e.getMessage());
        }

        // Duyệt hợp đồng cho thuê
        try {
            for (Document doc : rentalCollection.find(Filters.ne("status", "CANCELLED"))) {
                RentalContract contract = RentalContract.fromDocument(doc);
                if (contract != null && contract.getItems() != null) {
                    int days = Math.max(1, contract.getRentalDays());
                    for (RentalItem item : contract.getItems()) {
                        String pid = item.getProductId();
                        TopProductReport stat = productStats.get(pid);
                        if (stat == null) {
                            stat = new TopProductReport(pid, item.getProductName(), "Thiết bị", 0, 0, 0.0);
                            productStats.put(pid, stat);
                        }
                        stat.rentalCount += 1;
                        stat.totalRevenue += (item.getRentalRatePerDay() * days);
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("[ReportService] Lỗi thống kê hợp đồng thuê: " + e.getMessage());
        }

        // Sắp xếp giảm dần theo tổng doanh thu
        List<TopProductReport> resultList = new ArrayList<>(productStats.values());
        resultList.sort((a, b) -> Double.compare(b.getTotalRevenue(), a.getTotalRevenue()));

        if (limit > 0 && resultList.size() > limit) {
            return new ArrayList<>(resultList.subList(0, limit));
        }
        return resultList;
    }

    /**
     * Xuất dữ liệu báo cáo ra tập tin CSV định dạng UTF-8 (có BOM \uFEFF)
     * đảm bảo tương thích 100% với Microsoft Excel và các phần mềm ERP.
     *
     * @param file       File đích do JFileChooser cung cấp
     * @param reportData Danh sách các dòng dữ liệu (mỗi dòng là một Object[] gồm các cột)
     * @return true nếu xuất thành công, ngược lại false
     */
    public boolean exportReportToCSV(File file, List<Object[]> reportData) {
        if (file == null || reportData == null) {
            return false;
        }

        try (FileOutputStream fos = new FileOutputStream(file);
             OutputStreamWriter osw = new OutputStreamWriter(fos, StandardCharsets.UTF_8);
             PrintWriter writer = new PrintWriter(osw)) {

            // Ghi Byte Order Mark (BOM) UTF-8 để Microsoft Excel mở trực tiếp không bị vỡ font tiếng Việt
            writer.print('\uFEFF');

            for (Object[] row : reportData) {
                StringBuilder sb = new StringBuilder();
                for (int i = 0; i < row.length; i++) {
                    Object val = row[i];
                    String cell = val != null ? val.toString() : "";
                    // Thoát ký tự dấu nháy kép và bao quanh chuỗi
                    if (cell.contains(",") || cell.contains("\"") || cell.contains("\n")) {
                        cell = "\"" + cell.replace("\"", "\"\"") + "\"";
                    }
                    sb.append(cell);
                    if (i < row.length - 1) {
                        sb.append(",");
                    }
                }
                writer.println(sb);
            }
            writer.flush();
            return true;

        } catch (Exception e) {
            System.err.println("[ReportService] Lỗi xuất CSV: " + e.getMessage());
            return false;
        }
    }
}
