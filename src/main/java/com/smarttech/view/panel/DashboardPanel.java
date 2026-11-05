package com.smarttech.view.panel;

import com.smarttech.model.Customer;
import com.smarttech.model.Equipment;
import com.smarttech.model.Order;
import com.smarttech.model.Product;
import com.smarttech.model.RentalContract;
import com.smarttech.repository.CustomerRepository;
import com.smarttech.repository.EquipmentRepository;
import com.smarttech.repository.OrderRepository;
import com.smarttech.repository.ProductRepository;
import com.smarttech.repository.RentalContractRepository;
import com.smarttech.view.util.UIUtils;

import javax.swing.*;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import javax.swing.event.AncestorEvent;
import javax.swing.event.AncestorListener;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.JTableHeader;
import java.awt.*;
import java.text.DecimalFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.List;

/**
 * Bảng Điều Khiển Tổng Quan &amp; Trung Tâm Cảnh Báo Thông Minh (Dashboard Analytics Real-Time &amp; Smart Alert Center).
 * Cập nhật chỉ số KPI kinh doanh real-time, cảnh báo hợp đồng quá hạn/sắp hết hạn và cảnh báo thiết bị sắp hết hàng.
 * Toàn bộ các câu lệnh MongoDB đều chạy ngầm bằng SwingWorker đảm bảo không gây gián đoạn Event Dispatch Thread (EDT).
 *
 * @author Senior Java Developer &amp; Software Architect
 * @project SmartTech
 */
public class DashboardPanel extends JPanel {

    // Repositories truy xuất dữ liệu MongoDB
    private final OrderRepository orderRepository;
    private final RentalContractRepository contractRepository;
    private final EquipmentRepository equipmentRepository;
    private final ProductRepository productRepository;
    private final CustomerRepository customerRepository;

    // Các thành phần giao diện Header & Toolbar
    private JLabel lblLastUpdated;
    private JButton btnRefresh;

    // 4 Thẻ KPI Summary Cards
    private StatCard cardMonthlyRevenue;
    private StatCard cardActiveRentals;
    private StatCard cardMaintenance;
    private StatCard cardCustomerPoints;

    // Bảng 1: Trung Tâm Cảnh Báo Thông Minh (Smart Alert Center)
    private JTable tblAlerts;
    private DefaultTableModel modelAlerts;

    // Bảng 2: Hoạt Động Giao Dịch Mới Nhất (Recent Activity Log)
    private JTable tblRecentActivities;
    private DefaultTableModel modelRecentActivities;

    private static final DecimalFormat CURRENCY_FORMAT = new DecimalFormat("#,##0 VNĐ");
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm:ss");
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    public DashboardPanel() {
        this.orderRepository = new OrderRepository();
        this.contractRepository = new RentalContractRepository();
        this.equipmentRepository = new EquipmentRepository();
        this.productRepository = new ProductRepository();
        this.customerRepository = new CustomerRepository();

        initComponents();

        // Tự động tải dữ liệu khi Tab Dashboard được hiển thị lên màn hình
        addAncestorListener(new AncestorListener() {
            @Override
            public void ancestorAdded(AncestorEvent event) {
                loadDashboardDataAsync();
            }

            @Override
            public void ancestorRemoved(AncestorEvent event) {}

            @Override
            public void ancestorMoved(AncestorEvent event) {}
        });

        // Tải dữ liệu ban đầu
        loadDashboardDataAsync();
    }

    private void initComponents() {
        setLayout(new BorderLayout(0, 16));
        setBackground(UIUtils.COLOR_BG_LIGHT);
        setBorder(new EmptyBorder(16, 20, 16, 20));

        // 1. TOP HEADER BAR
        JPanel topHeader = createTopHeader();
        add(topHeader, BorderLayout.NORTH);

        // 2. MAIN WORKSPACE CONTAINER
        JPanel workspace = new JPanel(new BorderLayout(0, 16));
        workspace.setOpaque(false);

        // Hàng 4 Thẻ KPI Cards
        JPanel cardsContainer = new JPanel(new GridLayout(1, 4, 16, 0));
        cardsContainer.setOpaque(false);

        cardMonthlyRevenue = new StatCard("DOANH THU THÁNG", "0 VNĐ", "Đang tính toán...", UIUtils.COLOR_PRIMARY);
        cardActiveRentals = new StatCard("HỢP ĐỒNG ĐANG CHO THUÊ", "0 Hợp Đồng", "0 máy ngoài thị trường", UIUtils.COLOR_ACCENT);
        cardMaintenance = new StatCard("BẢO TRÌ & THIẾT BỊ HỎNG", "0 Thiết Bị", "Kho hàng ổn định", UIUtils.COLOR_SUCCESS);
        cardCustomerPoints = new StatCard("KHÁCH HÀNG & ĐIỂM THƯỞNG", "0 Thành Viên", "0 điểm đang lưu hành", new Color(99, 102, 241)); // Indigo

        cardsContainer.add(cardMonthlyRevenue);
        cardsContainer.add(cardActiveRentals);
        cardsContainer.add(cardMaintenance);
        cardsContainer.add(cardCustomerPoints);

        workspace.add(cardsContainer, BorderLayout.NORTH);

        // Khối 2 Bảng Thông Tin (Cảnh Báo Thông Minh bên trên, Giao Dịch Gần Nhất bên dưới)
        JPanel tablesPanel = new JPanel(new GridLayout(2, 1, 0, 16));
        tablesPanel.setOpaque(false);

        // Bảng 1: Smart Alert Center
        JPanel pnlAlerts = createAlertCenterCard();
        tablesPanel.add(pnlAlerts);

        // Bảng 2: Recent Activity Log
        JPanel pnlRecent = createRecentActivityCard();
        tablesPanel.add(pnlRecent);

        workspace.add(tablesPanel, BorderLayout.CENTER);
        add(workspace, BorderLayout.CENTER);
    }

    /**
     * Tạo thanh Top Bar Header kèm nút Refresh và thời gian cập nhật.
     */
    private JPanel createTopHeader() {
        JPanel header = new JPanel(new BorderLayout());
        header.setOpaque(false);

        JPanel titleContainer = new JPanel(new GridLayout(2, 1, 0, 4));
        titleContainer.setOpaque(false);

        JLabel lblTitle = new JLabel("TRUNG TÂM ĐIỀU HÀNH & BÁO CÁO REAL-TIME");
        lblTitle.setFont(UIUtils.FONT_TITLE);
        lblTitle.setForeground(UIUtils.COLOR_PRIMARY);

        JLabel lblSubtitle = new JLabel("Tổng quan chỉ số kinh doanh, cảnh báo tồn kho và trạng thái hợp đồng thuê theo thời gian thực.");
        lblSubtitle.setFont(UIUtils.FONT_SUBTITLE);
        lblSubtitle.setForeground(UIUtils.COLOR_TEXT_MUTED);

        titleContainer.add(lblTitle);
        titleContainer.add(lblSubtitle);

        // Controls bên phải
        JPanel rightControls = new JPanel(new FlowLayout(FlowLayout.RIGHT, 12, 0));
        rightControls.setOpaque(false);

        lblLastUpdated = new JLabel("Cập nhật: Chưa tải");
        lblLastUpdated.setFont(UIUtils.FONT_SMALL);
        lblLastUpdated.setForeground(UIUtils.COLOR_TEXT_MUTED);

        btnRefresh = new JButton("🔄  Tải Lại Dữ Liệu");
        btnRefresh.setFont(UIUtils.FONT_BODY_BOLD);
        btnRefresh.setBackground(Color.WHITE);
        btnRefresh.setForeground(UIUtils.COLOR_PRIMARY);
        btnRefresh.setBorder(new CompoundBorder(
                new LineBorder(UIUtils.COLOR_PRIMARY, 1, true),
                new EmptyBorder(6, 14, 6, 14)
        ));
        btnRefresh.setFocusPainted(false);
        btnRefresh.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btnRefresh.addActionListener(e -> loadDashboardDataAsync());

        rightControls.add(lblLastUpdated);
        rightControls.add(btnRefresh);

        header.add(titleContainer, BorderLayout.WEST);
        header.add(rightControls, BorderLayout.EAST);
        return header;
    }

    /**
     * Tạo Card Trung Tâm Cảnh Báo Thông Minh (Smart Alert Center).
     */
    private JPanel createAlertCenterCard() {
        JPanel card = new JPanel(new BorderLayout(0, 10));
        card.setBackground(Color.WHITE);
        card.setBorder(new CompoundBorder(
                new LineBorder(UIUtils.COLOR_BORDER, 1, true),
                new EmptyBorder(14, 16, 14, 16)
        ));

        JLabel lblTitle = new JLabel("⚠️  TRUNG TÂM CẢNH BÁO HỆ THỐNG (SMART ALERT CENTER)");
        lblTitle.setFont(UIUtils.FONT_HEADER);
        lblTitle.setForeground(new Color(220, 38, 38)); // Red 600

        String[] headers = {"Mức Độ", "Phân Loại Cảnh Báo", "Nội Dung Chi Tiết Sự Cố", "Hành Động Đề Xuất"};
        modelAlerts = new DefaultTableModel(headers, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        tblAlerts = new JTable(modelAlerts);
        styleTable(tblAlerts);

        // Áp dụng Badge Renderer cho cột Mức Độ
        tblAlerts.getColumnModel().getColumn(0).setCellRenderer(new AlertLevelCellRenderer());
        tblAlerts.getColumnModel().getColumn(0).setPreferredWidth(120);
        tblAlerts.getColumnModel().getColumn(1).setPreferredWidth(160);
        tblAlerts.getColumnModel().getColumn(2).setPreferredWidth(450);
        tblAlerts.getColumnModel().getColumn(3).setPreferredWidth(270);

        JScrollPane scrollPane = new JScrollPane(tblAlerts);
        scrollPane.setBorder(new LineBorder(UIUtils.COLOR_BORDER, 1));
        scrollPane.getViewport().setBackground(Color.WHITE);

        card.add(lblTitle, BorderLayout.NORTH);
        card.add(scrollPane, BorderLayout.CENTER);
        return card;
    }

    /**
     * Tạo Card Bảng Hoạt Động Giao Dịch Mới Nhất (Recent Activity Log).
     */
    private JPanel createRecentActivityCard() {
        JPanel card = new JPanel(new BorderLayout(0, 10));
        card.setBackground(Color.WHITE);
        card.setBorder(new CompoundBorder(
                new LineBorder(UIUtils.COLOR_BORDER, 1, true),
                new EmptyBorder(14, 16, 14, 16)
        ));

        JLabel lblTitle = new JLabel("⏱️  GIỜ GIAO DỊCH GẦN NHẤT (10 GIAO DỊCH GẦN ĐÂY)");
        lblTitle.setFont(UIUtils.FONT_HEADER);
        lblTitle.setForeground(UIUtils.COLOR_TEXT_MAIN);

        String[] headers = {"Thời Gian", "Mã Giao Dịch", "Loại Nghiệp Vụ", "Khách Hàng", "Giá Trị Đơn", "Trạng Thái"};
        modelRecentActivities = new DefaultTableModel(headers, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        tblRecentActivities = new JTable(modelRecentActivities);
        styleTable(tblRecentActivities);

        // Áp dụng Badge Renderer cho cột Loại Nghiệp Vụ
        tblRecentActivities.getColumnModel().getColumn(2).setCellRenderer(new TransactionTypeCellRenderer());
        tblRecentActivities.getColumnModel().getColumn(0).setPreferredWidth(130);
        tblRecentActivities.getColumnModel().getColumn(1).setPreferredWidth(140);
        tblRecentActivities.getColumnModel().getColumn(2).setPreferredWidth(130);
        tblRecentActivities.getColumnModel().getColumn(3).setPreferredWidth(240);
        tblRecentActivities.getColumnModel().getColumn(4).setPreferredWidth(150);
        tblRecentActivities.getColumnModel().getColumn(5).setPreferredWidth(150);

        JScrollPane scrollPane = new JScrollPane(tblRecentActivities);
        scrollPane.setBorder(new LineBorder(UIUtils.COLOR_BORDER, 1));
        scrollPane.getViewport().setBackground(Color.WHITE);

        card.add(lblTitle, BorderLayout.NORTH);
        card.add(scrollPane, BorderLayout.CENTER);
        return card;
    }

    /**
     * Tinh chỉnh giao diện bảng JTable phẳng, hiện đại.
     */
    private void styleTable(JTable table) {
        table.setFont(UIUtils.FONT_BODY);
        table.setRowHeight(34);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.setShowGrid(false);
        table.setIntercellSpacing(new Dimension(0, 0));
        table.setSelectionBackground(new Color(239, 246, 255));
        table.setSelectionForeground(UIUtils.COLOR_TEXT_MAIN);

        JTableHeader header = table.getTableHeader();
        header.setFont(UIUtils.FONT_BODY_BOLD);
        header.setBackground(new Color(241, 245, 249));
        header.setForeground(new Color(51, 65, 85));
        header.setPreferredSize(new Dimension(0, 36));
        header.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, UIUtils.COLOR_BORDER));
    }

    /**
     * Kích hoạt tác vụ tải dữ liệu Dashboard ngầm qua SwingWorker.
     */
    public void loadDashboardDataAsync() {
        btnRefresh.setEnabled(false);
        btnRefresh.setText("Đang đồng bộ...");
        lblLastUpdated.setText("Cập nhật: Đang kết nối MongoDB...");

        DashboardLoaderTask task = new DashboardLoaderTask();
        task.execute();
    }

    // =========================================================================
    // DTO VÀ SWINGWORKER BẤT ĐỒNG BỘ
    // =========================================================================

    /**
     * Đối tượng vận chuyển dữ liệu (DTO) đóng gói tất cả các chỉ số thống kê.
     */
    private static class DashboardData {
        double currentMonthRevenue;
        double prevMonthRevenue;
        double revenueGrowthPercent;

        long activeContractsCount;
        long rentedEquipmentsCount;

        long maintenanceEquipmentsCount;
        long brokenEquipmentsCount;

        long totalCustomersCount;
        long totalRewardPoints;

        List<AlertItem> alertItems = new ArrayList<>();
        List<RecentActivityItem> recentActivities = new ArrayList<>();
    }

    private static class AlertItem {
        String level;      // "CẦN XỬ LÝ GẤP", "CẢNH BÁO", "AN TOÀN"
        String type;       // "HỢP ĐỒNG QUÁ HẠN", "SẮP HẾT HẠN THUÊ", "TỒN KHO THẤP"
        String detail;
        String action;

        public AlertItem(String level, String type, String detail, String action) {
            this.level = level;
            this.type = type;
            this.detail = detail;
            this.action = action;
        }
    }

    private static class RecentActivityItem implements Comparable<RecentActivityItem> {
        String time;
        String code;
        String type; // "BÁN HÀNG" hoặc "CHO THUÊ"
        String customer;
        double amount;
        String status;
        LocalDateTime sortTimestamp;

        public RecentActivityItem(String time, String code, String type, String customer,
                                  double amount, String status, LocalDateTime sortTimestamp) {
            this.time = time;
            this.code = code;
            this.type = type;
            this.customer = customer;
            this.amount = amount;
            this.status = status;
            this.sortTimestamp = sortTimestamp != null ? sortTimestamp : LocalDateTime.now();
        }

        @Override
        public int compareTo(RecentActivityItem o) {
            return o.sortTimestamp.compareTo(this.sortTimestamp); // Giảm dần
        }
    }

    /**
     * SwingWorker xử lý toàn bộ truy vấn MongoDB trên background worker thread,
     * sau đó cập nhật toàn bộ components trên Swing Event Dispatch Thread (EDT).
     */
    private class DashboardLoaderTask extends SwingWorker<DashboardData, Void> {

        @Override
        protected DashboardData doInBackground() {
            DashboardData data = new DashboardData();

            LocalDate now = LocalDate.now();
            int currentMonth = now.getMonthValue();
            int currentYear = now.getYear();

            LocalDate prevMonthDate = now.minusMonths(1);
            int prevMonth = prevMonthDate.getMonthValue();
            int prevYear = prevMonthDate.getYear();

            // 1. Doanh thu tháng này & tháng trước
            double salesCur = orderRepository.getMonthlySalesRevenue(currentMonth, currentYear);
            double rentCur = contractRepository.getMonthlyRentalRevenue(currentMonth, currentYear);
            data.currentMonthRevenue = salesCur + rentCur;

            double salesPrev = orderRepository.getMonthlySalesRevenue(prevMonth, prevYear);
            double rentPrev = contractRepository.getMonthlyRentalRevenue(prevMonth, prevYear);
            data.prevMonthRevenue = salesPrev + rentPrev;

            if (data.prevMonthRevenue > 0) {
                data.revenueGrowthPercent = ((data.currentMonthRevenue - data.prevMonthRevenue) / data.prevMonthRevenue) * 100.0;
            } else {
                data.revenueGrowthPercent = 0.0;
            }

            // 2. Hợp đồng cho thuê & thiết bị RENTED
            data.activeContractsCount = contractRepository.countActiveContracts();
            data.rentedEquipmentsCount = equipmentRepository.countEquipmentsByStatus(Equipment.STATUS_RENTED);

            // 3. Thiết bị bảo trì & hỏng
            data.maintenanceEquipmentsCount = equipmentRepository.countEquipmentsByStatus(Equipment.STATUS_MAINTENANCE);
            data.brokenEquipmentsCount = equipmentRepository.countEquipmentsByStatus(Equipment.STATUS_BROKEN);

            // 4. Khách hàng & Điểm thưởng
            data.totalCustomersCount = customerRepository.countCustomers();
            data.totalRewardPoints = customerRepository.sumRewardPoints();

            // 5. Cảnh báo thông minh (Smart Alerts)
            // A. Cảnh báo hợp đồng quá hạn & sắp hết hạn
            List<RentalContract> warningContracts = contractRepository.getOverdueAndExpiringContracts();
            for (RentalContract c : warningContracts) {
                if (c.getExpectedEndDate() != null) {
                    if (c.getExpectedEndDate().isBefore(now)) {
                        long overdueDays = ChronoUnit.DAYS.between(c.getExpectedEndDate(), now);
                        data.alertItems.add(new AlertItem(
                                "CẦN XỬ LÝ GẤP",
                                "HỢP ĐỒNG QUÁ HẠN",
                                "HĐ [" + c.getId() + "] - Khách: " + c.getCustomerName() + " (" + c.getCustomerPhone() + ") quá hạn " + overdueDays + " ngày!",
                                "Liên hệ ngay thu hồi thiết bị & tính phí phạt quá hạn 150%"
                        ));
                    } else {
                        long daysLeft = ChronoUnit.DAYS.between(now, c.getExpectedEndDate());
                        String when = (daysLeft == 0) ? "trong hôm nay!" : "vào ngày mai!";
                        data.alertItems.add(new AlertItem(
                                "CẢNH BÁO",
                                "SẮP HẾT HẠN THUÊ",
                                "HĐ [" + c.getId() + "] - Khách: " + c.getCustomerName() + " (" + c.getCustomerPhone() + ") đến hạn trả " + when,
                                "Gọi điện nhắc nhở trả máy hoặc hỗ trợ làm thủ tục gia hạn"
                        ));
                    }
                }
            }

            // B. Cảnh báo tồn kho thấp (AVAILABLE <= 2 máy)
            List<Map.Entry<String, Long>> lowStockList = equipmentRepository.getLowStockProducts(2);
            for (Map.Entry<String, Long> entry : lowStockList) {
                Product p = productRepository.findById(entry.getKey());
                String pName = (p != null) ? p.getProductName() : entry.getKey();
                data.alertItems.add(new AlertItem(
                        "CẢNH BÁO",
                        "TỒN KHO THẤP",
                        "Sản phẩm [" + pName + "] (Mã: " + entry.getKey() + ") chỉ còn " + entry.getValue() + " máy AVAILABLE trong kho!",
                        "Kế hoạch nhập thêm thiết bị hoặc nghiệm thu máy từ khu bảo trì"
                ));
            }

            // Nếu không có bất kỳ cảnh báo nào
            if (data.alertItems.isEmpty()) {
                data.alertItems.add(new AlertItem(
                        "AN TOÀN",
                        "HỆ THỐNG ỔN ĐỊNH",
                        "Không có hợp đồng trễ hạn và không có sản phẩm nào chạm ngưỡng tồn kho tối thiểu.",
                        "Tiếp tục duy trì quy trình kiểm tra định kỳ"
                ));
            }

            // 6. Hoạt động giao dịch mới nhất (10 giao dịch gần đây)
            List<Order> recentOrders = orderRepository.getRecentOrders(10);
            for (Order o : recentOrders) {
                String dateStr = o.getOrderDate() != null ? o.getOrderDate().format(DATE_FORMATTER) : "Hôm nay";
                LocalDateTime ts = o.getOrderDate() != null ? o.getOrderDate().atStartOfDay() : LocalDateTime.now();
                data.recentActivities.add(new RecentActivityItem(
                        dateStr,
                        o.getId(),
                        "BÁN HÀNG",
                        o.getCustomerName() != null ? o.getCustomerName() : "Khách lẻ",
                        o.getFinalAmount(),
                        "ĐÃ THANH TOÁN",
                        ts
                ));
            }

            List<RentalContract> recentContracts = contractRepository.getRecentContracts(10);
            for (RentalContract c : recentContracts) {
                String dateStr = c.getStartDate() != null ? c.getStartDate().format(DATE_FORMATTER) : "Hôm nay";
                LocalDateTime ts = c.getStartDate() != null ? c.getStartDate().atStartOfDay() : LocalDateTime.now();
                data.recentActivities.add(new RecentActivityItem(
                        dateStr,
                        c.getId(),
                        "CHO THUÊ",
                        c.getCustomerName() != null ? c.getCustomerName() : "Khách thuê",
                        c.getTotalRentalFee(),
                        c.getStatus() != null ? c.getStatus() : RentalContract.STATUS_ACTIVE,
                        ts
                ));
            }

            // Sắp xếp giảm dần và lấy tối đa 10 giao dịch
            Collections.sort(data.recentActivities);
            if (data.recentActivities.size() > 10) {
                data.recentActivities = data.recentActivities.subList(0, 10);
            }

            return data;
        }

        @Override
        protected void done() {
            try {
                DashboardData data = get();

                // 1. Cập nhật Thẻ 1: Doanh Thu
                cardMonthlyRevenue.setValue(CURRENCY_FORMAT.format(data.currentMonthRevenue));
                if (data.prevMonthRevenue > 0) {
                    String sign = data.revenueGrowthPercent >= 0 ? "+" : "";
                    String growthStr = sign + String.format("%.1f%%", data.revenueGrowthPercent) + " so với tháng trước";
                    Color growthColor = data.revenueGrowthPercent >= 0 ? UIUtils.COLOR_SUCCESS : UIUtils.COLOR_DANGER;
                    cardMonthlyRevenue.setSubText(growthStr, growthColor);
                } else {
                    cardMonthlyRevenue.setSubText("Tháng đầu tiên ghi nhận", UIUtils.COLOR_TEXT_MUTED);
                }

                // 2. Cập nhật Thẻ 2: Hợp Đồng Đang Thuê
                cardActiveRentals.setValue(data.activeContractsCount + " Hợp Đồng");
                cardActiveRentals.setSubText(data.rentedEquipmentsCount + " thiết bị ngoài thị trường", UIUtils.COLOR_TEXT_MUTED);

                // 3. Cập nhật Thẻ 3: Bảo Trì & Thiết Bị Hỏng
                cardMaintenance.setValue(data.maintenanceEquipmentsCount + " Máy Bảo Trì");
                if (data.maintenanceEquipmentsCount > 0 || data.brokenEquipmentsCount > 0) {
                    cardMaintenance.setSubText(data.brokenEquipmentsCount + " máy hỏng hóc cần xử lý", new Color(217, 119, 6)); // Amber
                } else {
                    cardMaintenance.setSubText("Kho hàng đạt chuẩn vận hành", UIUtils.COLOR_SUCCESS);
                }

                // 4. Cập nhật Thẻ 4: Khách Hàng & Điểm Thưởng
                cardCustomerPoints.setValue(data.totalCustomersCount + " Khách Hàng");
                cardCustomerPoints.setSubText(String.format("%,d", data.totalRewardPoints) + " điểm đang lưu hành", UIUtils.COLOR_TEXT_MUTED);

                // 5. Cập nhật Bảng Cảnh Báo
                modelAlerts.setRowCount(0);
                for (AlertItem alert : data.alertItems) {
                    modelAlerts.addRow(new Object[]{
                            alert.level,
                            alert.type,
                            alert.detail,
                            alert.action
                    });
                }

                // 6. Cập nhật Bảng Giao Dịch Gần Nhất
                modelRecentActivities.setRowCount(0);
                for (RecentActivityItem item : data.recentActivities) {
                    modelRecentActivities.addRow(new Object[]{
                            item.time,
                            item.code,
                            item.type,
                            item.customer,
                            CURRENCY_FORMAT.format(item.amount),
                            item.status
                    });
                }

                lblLastUpdated.setText("Cập nhật: " + LocalDateTime.now().format(TIME_FORMATTER));
            } catch (Exception e) {
                System.err.println("[DashboardPanel] Lỗi cập nhật giao diện Dashboard: " + e.getMessage());
                lblLastUpdated.setText("Cập nhật: Lỗi tải dữ liệu");
            } finally {
                btnRefresh.setEnabled(true);
                btnRefresh.setText("🔄  Tải Lại Dữ Liệu");
            }
        }
    }

    // =========================================================================
    // CUSTOM CARD & TABLE CELL RENDERERS
    // =========================================================================

    /**
     * Thẻ thống kê KPI Card có hỗ trợ thay đổi nội dung linh hoạt.
     */
    private static class StatCard extends JPanel {
        private final JLabel lblVal;
        private final JLabel lblSub;

        public StatCard(String label, String value, String subText, Color accentColor) {
            setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
            setBackground(Color.WHITE);
            setBorder(new CompoundBorder(
                    new LineBorder(UIUtils.COLOR_BORDER, 1, true),
                    new EmptyBorder(16, 18, 16, 18)
            ));

            JLabel lblLabel = new JLabel(label);
            lblLabel.setFont(new Font("Segoe UI", Font.BOLD, 11));
            lblLabel.setForeground(UIUtils.COLOR_TEXT_MUTED);

            lblVal = new JLabel(value);
            lblVal.setFont(new Font("Segoe UI", Font.BOLD, 20));
            lblVal.setForeground(accentColor);

            lblSub = new JLabel(subText);
            lblSub.setFont(UIUtils.FONT_SMALL);
            lblSub.setForeground(new Color(71, 85, 105));

            add(lblLabel);
            add(Box.createVerticalStrut(6));
            add(lblVal);
            add(Box.createVerticalStrut(4));
            add(lblSub);
        }

        public void setValue(String value) {
            lblVal.setText(value);
        }

        public void setSubText(String subText, Color color) {
            lblSub.setText(subText);
            lblSub.setForeground(color);
        }
    }

    /**
     * Renderer hiển thị Badge màu theo mức độ cảnh báo của Smart Alert Center.
     */
    private static class AlertLevelCellRenderer extends DefaultTableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(JTable table, Object value,
                                                       boolean isSelected, boolean hasFocus, int row, int column) {
            JLabel label = (JLabel) super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
            label.setHorizontalAlignment(SwingConstants.CENTER);
            label.setFont(new Font("Segoe UI", Font.BOLD, 11));

            String text = value != null ? value.toString() : "";
            if ("CẦN XỬ LÝ GẤP".equalsIgnoreCase(text)) {
                label.setText(" 🚨 CẦN XỬ LÝ GẤP ");
                label.setBackground(new Color(254, 226, 226)); // Red 100
                label.setForeground(new Color(185, 28, 28));   // Red 700
            } else if ("CẢNH BÁO".equalsIgnoreCase(text)) {
                label.setText(" ⚠️ CẢNH BÁO ");
                label.setBackground(new Color(254, 243, 199)); // Amber 100
                label.setForeground(new Color(180, 83, 9));    // Amber 700
            } else {
                label.setText(" ✅ AN TOÀN ");
                label.setBackground(new Color(220, 252, 231)); // Green 100
                label.setForeground(new Color(21, 128, 61));   // Green 700
            }

            label.setOpaque(true);
            return label;
        }
    }

    /**
     * Renderer hiển thị Badge màu cho loại giao dịch ("BÁN HÀNG" hoặc "CHO THUÊ").
     */
    private static class TransactionTypeCellRenderer extends DefaultTableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(JTable table, Object value,
                                                       boolean isSelected, boolean hasFocus, int row, int column) {
            JLabel label = (JLabel) super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
            label.setHorizontalAlignment(SwingConstants.CENTER);
            label.setFont(new Font("Segoe UI", Font.BOLD, 11));

            String text = value != null ? value.toString() : "";
            if ("BÁN HÀNG".equalsIgnoreCase(text)) {
                label.setText(" BÁN HÀNG ");
                label.setBackground(new Color(220, 252, 231)); // Green 100
                label.setForeground(new Color(21, 128, 61));   // Green 700
            } else if ("CHO THUÊ".equalsIgnoreCase(text)) {
                label.setText(" CHO THUÊ ");
                label.setBackground(new Color(224, 231, 255)); // Indigo 100
                label.setForeground(new Color(67, 56, 202));   // Indigo 700
            } else {
                label.setText(" " + text + " ");
                label.setBackground(new Color(241, 245, 249));
                label.setForeground(new Color(71, 85, 105));
            }

            label.setOpaque(true);
            return label;
        }
    }
}
