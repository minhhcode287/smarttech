package com.smarttech.view.panel;

import com.smarttech.model.Order;
import com.smarttech.model.OrderItem;
import com.smarttech.model.RentalContract;
import com.smarttech.model.RentalItem;
import com.smarttech.repository.OrderRepository;
import com.smarttech.repository.RentalContractRepository;
import com.smarttech.view.util.UIUtils;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.List;
import java.util.*;

/**
 * Phân hệ Báo Cáo Thống Kê & Phân Tích Doanh Thu (ReportPanel).
 * Tích hợp tính toán thời gian thực theo 4 kỳ báo cáo, 4 thẻ KPI Summary Cards,
 * JTabbedPane chi tiết Đơn bán / Hợp đồng thuê, Top 5 Thiết bị chạy nhất
 * và chức năng Xuất báo cáo CSV chuẩn UTF-8 qua JFileChooser.
 *
 * @author Senior Java Developer & Software Architect
 * @project SmartTech
 */
public class ReportPanel extends JPanel {

    private final OrderRepository orderRepository;
    private final RentalContractRepository rentalContractRepository;

    // Filter controls
    private JComboBox<String> cboPeriod;
    private JLabel lblLoadingStatus;

    // 4 KPI Summary Cards
    private JLabel lblTotalSaleRevenue;
    private JLabel lblTotalRentalRevenue;
    private JLabel lblActiveRentalCount;
    private JLabel lblTotalDevicesProcessed;

    // Tab 1: Chi tiết Bán hàng
    private JTable saleReportTable;
    private DefaultTableModel saleReportModel;

    // Tab 2: Chi tiết Cho thuê
    private JTable rentalReportTable;
    private DefaultTableModel rentalReportModel;

    // Tab 3: Top 5 Thiết bị thịnh hành
    private JTable topDevicesTable;
    private DefaultTableModel topDevicesModel;

    // Cache kết quả thống kê
    private double currentSaleRev = 0.0;
    private double currentRentRev = 0.0;
    private int currentActiveRentals = 0;
    private int currentDevicesTotal = 0;

    public ReportPanel() {
        this.orderRepository = new OrderRepository();
        this.rentalContractRepository = new RentalContractRepository();

        initComponents();
        loadAnalyticsAsync();
    }

    private void initComponents() {
        setLayout(new BorderLayout(0, 16));
        setBackground(UIUtils.COLOR_BG_LIGHT);
        setBorder(new EmptyBorder(15, 15, 15, 15));

        // ================= 1. HEADER & FILTER TOOLBAR =================
        JPanel topContainer = new JPanel(new BorderLayout(0, 10));
        topContainer.setOpaque(false);

        // Header Title
        JPanel headerTitle = new JPanel(new BorderLayout());
        headerTitle.setOpaque(false);
        JLabel lblTitle = new JLabel("Báo Cáo Thống Kê & Doanh Thu (Analytics)");
        lblTitle.setFont(UIUtils.FONT_TITLE);
        lblTitle.setForeground(UIUtils.COLOR_TEXT_MAIN);

        JLabel lblSub = new JLabel("Theo dõi cơ cấu doanh thu từ bán lẻ và dịch vụ cho thuê thiết bị theo kỳ kế toán.");
        lblSub.setFont(UIUtils.FONT_SUBTITLE);
        lblSub.setForeground(UIUtils.COLOR_TEXT_MUTED);

        headerTitle.add(lblTitle, BorderLayout.NORTH);
        headerTitle.add(lblSub, BorderLayout.SOUTH);
        topContainer.add(headerTitle, BorderLayout.NORTH);

        // Filter Bar
        JPanel filterBar = new JPanel(new BorderLayout());
        filterBar.setOpaque(false);

        JPanel leftFilter = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        leftFilter.setOpaque(false);

        cboPeriod = new JComboBox<>(new String[]{"Hôm nay", "7 ngày qua", "Tháng này", "Năm nay"});
        cboPeriod.setFont(UIUtils.FONT_BODY);
        cboPeriod.setPreferredSize(new Dimension(160, 36));
        cboPeriod.addActionListener(e -> loadAnalyticsAsync());

        JButton btnReload = new JButton("Tải Lại Dữ Liệu");
        btnReload.setFont(UIUtils.FONT_BODY_BOLD);
        btnReload.setBackground(Color.WHITE);
        btnReload.setBorder(BorderFactory.createLineBorder(UIUtils.COLOR_BORDER, 1));
        btnReload.setPreferredSize(new Dimension(130, 36));
        btnReload.addActionListener(e -> loadAnalyticsAsync());

        lblLoadingStatus = new JLabel("● Sẵn sàng");
        lblLoadingStatus.setFont(UIUtils.FONT_SMALL);
        lblLoadingStatus.setForeground(UIUtils.COLOR_SUCCESS);

        leftFilter.add(new JLabel("Kỳ Báo Cáo:"));
        leftFilter.add(cboPeriod);
        leftFilter.add(btnReload);
        leftFilter.add(lblLoadingStatus);

        JPanel rightAction = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        rightAction.setOpaque(false);

        JButton btnExportCSV = new JButton("📥  Xuất Báo Cáo CSV (UTF-8)");
        btnExportCSV.setFont(UIUtils.FONT_BODY_BOLD);
        btnExportCSV.setBackground(new Color(22, 101, 52)); // Green 800
        btnExportCSV.setForeground(Color.WHITE);
        btnExportCSV.setFocusPainted(false);
        btnExportCSV.setBorderPainted(false);
        btnExportCSV.setPreferredSize(new Dimension(220, 36));
        btnExportCSV.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btnExportCSV.addActionListener(e -> exportReportToCSV());

        rightAction.add(btnExportCSV);

        filterBar.add(leftFilter, BorderLayout.WEST);
        filterBar.add(rightAction, BorderLayout.EAST);
        topContainer.add(filterBar, BorderLayout.SOUTH);

        add(topContainer, BorderLayout.NORTH);

        // ================= 2. TOP 4 KPI CARDS =================
        JPanel cardsContainer = new JPanel(new GridLayout(1, 4, 16, 0));
        cardsContainer.setOpaque(false);
        cardsContainer.setPreferredSize(new Dimension(0, 95));

        lblTotalSaleRevenue = new JLabel("0 VNĐ");
        lblTotalRentalRevenue = new JLabel("0 VNĐ");
        lblActiveRentalCount = new JLabel("0");
        lblTotalDevicesProcessed = new JLabel("0 Thiết Bị");

        cardsContainer.add(createKpiCard("DOANH THU BÁN HÀNG", lblTotalSaleRevenue, "Tổng từ hóa đơn bán lẻ", UIUtils.COLOR_PRIMARY));
        cardsContainer.add(createKpiCard("DOANH THU CHO THUÊ", lblTotalRentalRevenue, "Tổng tiền thuê đã thu", UIUtils.COLOR_ACCENT));
        cardsContainer.add(createKpiCard("HỢP ĐỒNG ĐANG THUÊ", lblActiveRentalCount, "Số hợp đồng trạng thái ACTIVE", new Color(217, 119, 6))); // Amber
        cardsContainer.add(createKpiCard("THIẾT BỊ ĐÃ XỬ LÝ", lblTotalDevicesProcessed, "Tổng máy bán & cho thuê", UIUtils.COLOR_SUCCESS));

        // ================= 3. DETAILED ANALYTICS TABBED PANE =================
        JTabbedPane tabbedPane = new JTabbedPane();
        tabbedPane.setFont(UIUtils.FONT_BODY_BOLD);

        // TAB 1: CHI TIẾT DOANH THU BÁN HÀNG
        JPanel tabSale = new JPanel(new BorderLayout(0, 10));
        tabSale.setBackground(Color.WHITE);
        tabSale.setBorder(new EmptyBorder(12, 14, 12, 14));

        String[] saleCols = {"Mã Đơn", "Ngày Bán", "Khách Hàng", "SĐT", "Số Thiết Bị", "Tổng Tiền Hàng", "Điểm Dùng", "Thực Thu (VNĐ)", "PT Thanh Toán"};
        saleReportModel = new DefaultTableModel(saleCols, 0) {
            @Override
            public boolean isCellEditable(int r, int c) { return false; }
        };
        saleReportTable = new JTable(saleReportModel);
        setupTableStyle(saleReportTable);
        tabSale.add(new JScrollPane(saleReportTable), BorderLayout.CENTER);

        // TAB 2: CHI TIẾT HỢP ĐỒNG CHO THUÊ
        JPanel tabRental = new JPanel(new BorderLayout(0, 10));
        tabRental.setBackground(Color.WHITE);
        tabRental.setBorder(new EmptyBorder(12, 14, 12, 14));

        String[] rentCols = {"Mã HĐ", "Khách Hàng", "SĐT", "Ngày Bắt Đầu", "Hạn Trả", "Số Máy Thuê", "Tiền Thuê", "Tiền Cọc", "Trạng Thái"};
        rentalReportModel = new DefaultTableModel(rentCols, 0) {
            @Override
            public boolean isCellEditable(int r, int c) { return false; }
        };
        rentalReportTable = new JTable(rentalReportModel);
        setupTableStyle(rentalReportTable);
        tabRental.add(new JScrollPane(rentalReportTable), BorderLayout.CENTER);

        // TAB 3: TOP 5 THIẾT BỊ BÁN CHẠY & CHO THUÊ NHIỀU NHẤT
        JPanel tabTopDevices = new JPanel(new BorderLayout(0, 10));
        tabTopDevices.setBackground(Color.WHITE);
        tabTopDevices.setBorder(new EmptyBorder(12, 14, 12, 14));

        String[] topCols = {"Xếp Hạng", "Mẫu Sản Phẩm Thiết Bị", "Số Lần Giao Dịch Bán", "Số Lần Cho Thuê", "Tổng Lượt Khai Thác"};
        topDevicesModel = new DefaultTableModel(topCols, 0) {
            @Override
            public boolean isCellEditable(int r, int c) { return false; }
        };
        topDevicesTable = new JTable(topDevicesModel);
        setupTableStyle(topDevicesTable);
        tabTopDevices.add(new JScrollPane(topDevicesTable), BorderLayout.CENTER);

        tabbedPane.addTab("🛒  Chi Tiết Doanh Thu Bán Hàng", tabSale);
        tabbedPane.addTab("📋  Chi Tiết Hợp Đồng Cho Thuê", tabRental);
        tabbedPane.addTab("🔥  Top Thiết Bị Khai Thác Tốt Nhất", tabTopDevices);

        JPanel centerWrapper = new JPanel(new BorderLayout(0, 16));
        centerWrapper.setOpaque(false);
        centerWrapper.add(cardsContainer, BorderLayout.NORTH);
        centerWrapper.add(tabbedPane, BorderLayout.CENTER);

        add(centerWrapper, BorderLayout.CENTER);
    }

    private void setupTableStyle(JTable table) {
        table.setFont(UIUtils.FONT_BODY);
        table.setRowHeight(34);
        table.getTableHeader().setFont(UIUtils.FONT_BODY_BOLD);
        table.getTableHeader().setBackground(new Color(241, 245, 249));
        table.setShowVerticalLines(false);
        table.setGridColor(UIUtils.COLOR_BORDER);
    }

    private JPanel createKpiCard(String title, JLabel lblValue, String subtitle, Color color) {
        JPanel card = new JPanel();
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setBackground(Color.WHITE);
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(UIUtils.COLOR_BORDER, 1),
                new EmptyBorder(12, 16, 12, 16)
        ));

        JLabel lblT = new JLabel(title);
        lblT.setFont(new Font("Segoe UI", Font.BOLD, 11));
        lblT.setForeground(UIUtils.COLOR_TEXT_MUTED);

        lblValue.setFont(new Font("Segoe UI", Font.BOLD, 18));
        lblValue.setForeground(color);

        JLabel lblSub = new JLabel(subtitle);
        lblSub.setFont(UIUtils.FONT_SMALL);
        lblSub.setForeground(new Color(100, 116, 139));

        card.add(lblT);
        card.add(Box.createVerticalStrut(4));
        card.add(lblValue);
        card.add(Box.createVerticalStrut(2));
        card.add(lblSub);

        return card;
    }

    // ================= TÍNH TOÁN BẤT ĐỒNG BỘ VỚI SWINGWORKER =================

    public void loadAnalyticsAsync() {
        lblLoadingStatus.setText("● Đang tính toán số liệu...");
        lblLoadingStatus.setForeground(UIUtils.COLOR_PRIMARY);

        String period = (String) cboPeriod.getSelectedItem();
        LocalDate now = LocalDate.now();

        LocalDate startDate;
        if ("Hôm nay".equals(period)) {
            startDate = now;
        } else if ("7 ngày qua".equals(period)) {
            startDate = now.minusDays(7);
        } else if ("Tháng này".equals(period)) {
            startDate = now.withDayOfMonth(1);
        } else {
            // Năm nay
            startDate = now.withDayOfYear(1);
        }

        SwingWorker<AnalyticsResult, Void> worker = new SwingWorker<>() {
            @Override
            protected AnalyticsResult doInBackground() {
                AnalyticsResult result = new AnalyticsResult();

                // 1. Phân tích Đơn hàng Bán lẻ
                List<Order> allOrders = orderRepository.findAll();
                for (Order o : allOrders) {
                    if (o.getOrderDate() != null && !o.getOrderDate().isBefore(startDate)) {
                        result.filteredOrders.add(o);
                        result.totalSaleRevenue += o.getFinalAmount();
                        if (o.getItems() != null) {
                            result.totalSoldDevices += o.getItems().size();
                            for (OrderItem item : o.getItems()) {
                                result.productSaleCounts.put(item.getProductName(),
                                        result.productSaleCounts.getOrDefault(item.getProductName(), 0) + item.getQuantity());
                            }
                        }
                    }
                }

                // 2. Phân tích Hợp đồng cho thuê
                List<RentalContract> allContracts = rentalContractRepository.findAll();
                for (RentalContract c : allContracts) {
                    if (c.getStartDate() != null && !c.getStartDate().isBefore(startDate)) {
                        result.filteredContracts.add(c);
                        result.totalRentalRevenue += c.getTotalRentalFee();
                        if (RentalContract.STATUS_ACTIVE.equals(c.getStatus())) {
                            result.activeRentals++;
                        }
                        if (c.getItems() != null) {
                            result.totalRentedDevices += c.getItems().size();
                            for (RentalItem rItem : c.getItems()) {
                                result.productRentCounts.put(rItem.getProductName(),
                                        result.productRentCounts.getOrDefault(rItem.getProductName(), 0) + 1);
                            }
                        }
                    }
                }

                return result;
            }

            @Override
            protected void done() {
                try {
                    AnalyticsResult res = get();

                    // Cập nhật thẻ KPI
                    currentSaleRev = res.totalSaleRevenue;
                    currentRentRev = res.totalRentalRevenue;
                    currentActiveRentals = res.activeRentals;
                    currentDevicesTotal = res.totalSoldDevices + res.totalRentedDevices;

                    lblTotalSaleRevenue.setText(String.format("%,.0f VNĐ", currentSaleRev));
                    lblTotalRentalRevenue.setText(String.format("%,.0f VNĐ", currentRentRev));
                    lblActiveRentalCount.setText(currentActiveRentals + " Hợp Đồng");
                    lblTotalDevicesProcessed.setText(currentDevicesTotal + " Thiết Bị");

                    // Đổ dữ liệu Tab Bán Hàng
                    saleReportModel.setRowCount(0);
                    for (Order o : res.filteredOrders) {
                        saleReportModel.addRow(new Object[]{
                                o.getId(),
                                o.getOrderDate(),
                                o.getCustomerName(),
                                o.getCustomerPhone(),
                                o.getItems() != null ? o.getItems().size() : 0,
                                String.format("%,.0f đ", o.getTotalAmount()),
                                o.getPointsUsed(),
                                String.format("%,.0f đ", o.getFinalAmount()),
                                o.getPaymentMethod()
                        });
                    }

                    // Đổ dữ liệu Tab Cho Thuê
                    rentalReportModel.setRowCount(0);
                    for (RentalContract c : res.filteredContracts) {
                        rentalReportModel.addRow(new Object[]{
                                c.getId(),
                                c.getCustomerName(),
                                c.getCustomerPhone(),
                                c.getStartDate(),
                                c.getExpectedEndDate(),
                                c.getItems() != null ? c.getItems().size() : 0,
                                String.format("%,.0f đ", c.getTotalRentalFee()),
                                String.format("%,.0f đ", c.getTotalDeposit()),
                                c.getStatus()
                        });
                    }

                    // Đổ dữ liệu Tab Top 5 Thiết bị
                    topDevicesModel.setRowCount(0);
                    Set<String> allProductNames = new HashSet<>();
                    allProductNames.addAll(res.productSaleCounts.keySet());
                    allProductNames.addAll(res.productRentCounts.keySet());

                    List<ProductRanking> rankingList = new ArrayList<>();
                    for (String prodName : allProductNames) {
                        int saleQty = res.productSaleCounts.getOrDefault(prodName, 0);
                        int rentQty = res.productRentCounts.getOrDefault(prodName, 0);
                        rankingList.add(new ProductRanking(prodName, saleQty, rentQty));
                    }
                    rankingList.sort((a, b) -> Integer.compare(b.totalCount, a.totalCount));

                    int rank = 1;
                    for (ProductRanking pr : rankingList) {
                        if (rank > 5) break;
                        topDevicesModel.addRow(new Object[]{
                                "Top " + rank,
                                pr.productName,
                                pr.saleCount + " lần",
                                pr.rentCount + " lần",
                                pr.totalCount + " lượt"
                        });
                        rank++;
                    }

                    lblLoadingStatus.setText("● Cập nhật thành công lúc " + java.time.LocalTime.now().toString().substring(0, 8));
                    lblLoadingStatus.setForeground(UIUtils.COLOR_SUCCESS);

                } catch (Exception ex) {
                    System.err.println("[ReportPanel] Lỗi tính toán báo cáo: " + ex.getMessage());
                    lblLoadingStatus.setText("● Lỗi khi tải số liệu thống kê!");
                    lblLoadingStatus.setForeground(UIUtils.COLOR_DANGER);
                }
            }
        };
        worker.execute();
    }

    // ================= XUẤT BÁO CÁO CSV (UTF-8) =================

    private void exportReportToCSV() {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("Chọn thư mục lưu báo cáo thống kê CSV");
        fileChooser.setSelectedFile(new File("BaoCao_SmartTech_" + LocalDate.now() + ".csv"));

        int userSelection = fileChooser.showSaveDialog(this);
        if (userSelection == JFileChooser.APPROVE_OPTION) {
            File fileToSave = fileChooser.getSelectedFile();
            if (!fileToSave.getName().toLowerCase().endsWith(".csv")) {
                fileToSave = new File(fileToSave.getAbsolutePath() + ".csv");
            }

            try (PrintWriter writer = new PrintWriter(new OutputStreamWriter(new FileOutputStream(fileToSave), StandardCharsets.UTF_8))) {
                // Viết BOM UTF-8 để Microsoft Excel hiển thị tiếng Việt không bị lỗi font
                writer.write('\ufeff');

                writer.println("==========================================================================");
                writer.println("SMARTTECH ENTERPRISE - BÁO CÁO DOANH THU & HIỆU SUẤT HOẠT ĐỘNG");
                writer.println("Thời điểm xuất báo cáo: " + LocalDate.now() + " | Kỳ: " + cboPeriod.getSelectedItem());
                writer.println("==========================================================================");
                writer.println();

                // KPI Tóm tắt
                writer.println("--- 1. TỔNG QUAN CHỈ SỐ KINH DOANH (KPI) ---");
                writer.println("Tổng doanh thu bán lẻ (VNĐ)," + String.format("%.0f", currentSaleRev));
                writer.println("Tổng doanh thu cho thuê (VNĐ)," + String.format("%.0f", currentRentRev));
                writer.println("Hợp đồng thuê đang chạy (Active)," + currentActiveRentals);
                writer.println("Tổng số thiết bị đã giao dịch," + currentDevicesTotal);
                writer.println();

                // Chi tiết Bán hàng
                writer.println("--- 2. CHI TIẾT HÓA ĐƠN BÁN HÀNG ---");
                writer.println("Mã Đơn,Ngày Bán,Khách Hàng,Số Điện Thoại,Số Thiết Bị,Thực Thu (VNĐ),PT Thanh Toán");
                for (int i = 0; i < saleReportModel.getRowCount(); i++) {
                    writer.println(String.format("%s,%s,\"%s\",%s,%s,%s,%s",
                            saleReportModel.getValueAt(i, 0),
                            saleReportModel.getValueAt(i, 1),
                            saleReportModel.getValueAt(i, 2),
                            saleReportModel.getValueAt(i, 3),
                            saleReportModel.getValueAt(i, 4),
                            saleReportModel.getValueAt(i, 7).toString().replace(" đ", "").replace(",", ""),
                            saleReportModel.getValueAt(i, 8)
                    ));
                }
                writer.println();

                // Chi tiết Cho thuê
                writer.println("--- 3. CHI TIẾT HỢP ĐỒNG CHO THUÊ ---");
                writer.println("Mã Hợp Đồng,Khách Hàng,Số Điện Thoại,Ngày Thuê,Hạn Trả,Tiền Thuê (VNĐ),Tiền Cọc (VNĐ),Trạng Thái");
                for (int i = 0; i < rentalReportModel.getRowCount(); i++) {
                    writer.println(String.format("%s,\"%s\",%s,%s,%s,%s,%s,%s",
                            rentalReportModel.getValueAt(i, 0),
                            rentalReportModel.getValueAt(i, 1),
                            rentalReportModel.getValueAt(i, 2),
                            rentalReportModel.getValueAt(i, 3),
                            rentalReportModel.getValueAt(i, 4),
                            rentalReportModel.getValueAt(i, 6).toString().replace(" đ", "").replace(",", ""),
                            rentalReportModel.getValueAt(i, 7).toString().replace(" đ", "").replace(",", ""),
                            rentalReportModel.getValueAt(i, 8)
                    ));
                }

                JOptionPane.showMessageDialog(this,
                        "Xuất báo cáo thành công ra file:\n" + fileToSave.getAbsolutePath(),
                        "Thành Công",
                        JOptionPane.INFORMATION_MESSAGE);

            } catch (Exception ex) {
                System.err.println("[ReportPanel] Lỗi xuất CSV: " + ex.getMessage());
                JOptionPane.showMessageDialog(this, "Không thể xuất file CSV: " + ex.getMessage(), "Lỗi Xuất Báo Cáo", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    // Helper classes cho việc tổng hợp dữ liệu
    private static class AnalyticsResult {
        double totalSaleRevenue = 0.0;
        double totalRentalRevenue = 0.0;
        int activeRentals = 0;
        int totalSoldDevices = 0;
        int totalRentedDevices = 0;

        List<Order> filteredOrders = new ArrayList<>();
        List<RentalContract> filteredContracts = new ArrayList<>();
        Map<String, Integer> productSaleCounts = new HashMap<>();
        Map<String, Integer> productRentCounts = new HashMap<>();
    }

    private static class ProductRanking {
        String productName;
        int saleCount;
        int rentCount;
        int totalCount;

        ProductRanking(String name, int sale, int rent) {
            this.productName = name;
            this.saleCount = sale;
            this.rentCount = rent;
            this.totalCount = sale + rent;
        }
    }
}