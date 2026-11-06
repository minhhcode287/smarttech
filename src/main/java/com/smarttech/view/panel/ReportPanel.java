package com.smarttech.view.panel;

import com.smarttech.service.ReportService;
import com.smarttech.service.ReportService.DailyRevenuePoint;
import com.smarttech.service.ReportService.ReportTransaction;
import com.smarttech.service.ReportService.RevenueReportData;
import com.smarttech.service.ReportService.TopProductReport;
import com.smarttech.view.util.UIUtils;

import javax.swing.*;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.JTableHeader;
import java.awt.*;
import java.io.File;
import java.text.DecimalFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * Phân hệ Báo Cáo Thống Kê, Biểu Đồ Doanh Thu & Xuất Báo Cáo (Prompt 21 - Final Delivery).
 * Kiến trúc Enterprise chuyên sâu:
 * - 4 Stat Cards tổng hợp KPI: Doanh thu Bán hàng, Cho thuê, Chi phí, Lợi nhuận gộp.
 * - Tab 1: Biểu đồ trực quan hóa doanh thu bằng Custom Swing Graphics 2D & Bảng Top 5 sản phẩm chạy nhất.
 * - Tab 2: Bảng chi tiết toàn bộ giao dịch kế toán có thanh lọc và phân tích chênh lệch.
 * - Xuất file CSV UTF-8 (BOM \uFEFF) tương thích 100% Microsoft Excel.
 * - 100% thao tác tổng hợp dữ liệu chạy qua SwingWorker không gây nghẽn giao diện.
 *
 * @author Senior Java Developer & Software Architect (30 years ERP/POS experience)
 * @project SmartTech
 */
public class ReportPanel extends JPanel {

    private static final DecimalFormat CURRENCY_FORMAT = new DecimalFormat("#,##0 VNĐ");
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter DISPLAY_DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private final ReportService reportService;

    // Filter Controls
    private JComboBox<String> cboTimePreset;
    private JTextField txtFromDate;
    private JTextField txtToDate;
    private JButton btnFilter;
    private JButton btnExport;
    private JLabel lblLoadingStatus;

    // 4 Stat Cards KPI
    private StatCard cardSales;
    private StatCard cardRental;
    private StatCard cardCosts;
    private StatCard cardProfit;

    // Tab 1: Chart & Top 5
    private RevenueChartPanel revenueChartPanel;
    private JTable tblTopProducts;
    private DefaultTableModel modelTopProducts;

    // Tab 2: Detail Table
    private JTable tblDetails;
    private DefaultTableModel modelDetails;
    private JTextField txtSearchDetail;

    // Cache dữ liệu hiện tại để phục vụ xuất file
    private RevenueReportData currentReportData;
    private List<TopProductReport> currentTopProducts;

    public ReportPanel() {
        this.reportService = new ReportService();

        initComponents();
        setupDefaultDates();
        loadReportDataAsync();
    }

    private void initComponents() {
        setLayout(new BorderLayout(0, 16));
        setBackground(UIUtils.COLOR_BG_LIGHT);
        setBorder(new EmptyBorder(16, 16, 16, 16));

        // 1. TOP HEADER & FILTER BAR
        JPanel topContainer = createHeaderAndFilterBar();
        add(topContainer, BorderLayout.NORTH);

        // 2. CENTER WORKSPACE (Stat Cards + Tabs)
        JPanel centerContainer = new JPanel(new BorderLayout(0, 16));
        centerContainer.setOpaque(false);

        // 4 Stat Cards KPI
        JPanel statCardsPanel = createStatCardsPanel();
        centerContainer.add(statCardsPanel, BorderLayout.NORTH);

        // Tabbed Pane (Biểu Đồ & Bảng Số Liệu Chi Tiết)
        JTabbedPane tabbedPane = new JTabbedPane();
        tabbedPane.setFont(UIUtils.FONT_BODY_BOLD);
        tabbedPane.setBackground(Color.WHITE);

        // Tab 1: Biểu Đồ & Thống Kê Tổng Hợp
        tabbedPane.addTab("📈  Biểu Đồ & Thống Kê Tổng Hợp", createChartAndTopPanel());

        // Tab 2: Bảng Số Liệu Chi Tiết
        tabbedPane.addTab("📋  Bảng Số Liệu Chi Tiết Đơn Hàng & Doanh Thu", createDetailTablePanel());

        centerContainer.add(tabbedPane, BorderLayout.CENTER);
        add(centerContainer, BorderLayout.CENTER);
    }

    /**
     * Tạo phần Header thương hiệu và thanh công cụ bộ lọc thời gian.
     */
    private JPanel createHeaderAndFilterBar() {
        JPanel container = new JPanel(new BorderLayout(0, 12));
        container.setOpaque(false);

        // Tiêu đề phân hệ
        JPanel titlePanel = new JPanel(new BorderLayout());
        titlePanel.setOpaque(false);

        JLabel lblTitle = new JLabel("PHÂN HỆ BÁO CÁO THỐNG KÊ & PHÂN TÍCH DOANH THU");
        lblTitle.setFont(UIUtils.FONT_TITLE);
        lblTitle.setForeground(UIUtils.COLOR_TEXT_MAIN);

        JLabel lblSub = new JLabel("Hệ thống tổng hợp chỉ số kinh doanh, phân tích cơ cấu doanh thu và xuất báo cáo kiểm toán doanh nghiệp.");
        lblSub.setFont(UIUtils.FONT_SUBTITLE);
        lblSub.setForeground(UIUtils.COLOR_TEXT_MUTED);

        titlePanel.add(lblTitle, BorderLayout.NORTH);
        titlePanel.add(lblSub, BorderLayout.SOUTH);
        container.add(titlePanel, BorderLayout.NORTH);

        // Thanh công cụ bộ lọc
        JPanel filterBar = new JPanel(new BorderLayout());
        filterBar.setBackground(Color.WHITE);
        filterBar.setBorder(new CompoundBorder(
                new LineBorder(UIUtils.COLOR_BORDER, 1, true),
                new EmptyBorder(10, 16, 10, 16)
        ));

        // Nhóm bên trái: Chọn kỳ và khoảng ngày
        JPanel leftFilter = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 0));
        leftFilter.setOpaque(false);

        JLabel lblPreset = new JLabel("Kỳ Báo Cáo:");
        lblPreset.setFont(UIUtils.FONT_BODY_BOLD);
        lblPreset.setForeground(UIUtils.COLOR_TEXT_MAIN);

        cboTimePreset = new JComboBox<>(new String[]{"Tháng Này", "7 Ngày Qua", "Hôm Nay", "Tùy Chọn Ngày"});
        cboTimePreset.setFont(UIUtils.FONT_BODY);
        cboTimePreset.setPreferredSize(new Dimension(140, 34));
        cboTimePreset.addActionListener(e -> onPresetChanged());

        JLabel lblFrom = new JLabel("Từ ngày:");
        lblFrom.setFont(UIUtils.FONT_BODY);
        txtFromDate = new JTextField(10);
        txtFromDate.setFont(UIUtils.FONT_BODY);
        txtFromDate.setPreferredSize(new Dimension(105, 34));

        JLabel lblTo = new JLabel("Đến ngày:");
        lblTo.setFont(UIUtils.FONT_BODY);
        txtToDate = new JTextField(10);
        txtToDate.setFont(UIUtils.FONT_BODY);
        txtToDate.setPreferredSize(new Dimension(105, 34));

        btnFilter = new JButton("📊  Xem Báo Cáo");
        btnFilter.setFont(UIUtils.FONT_BODY_BOLD);
        btnFilter.setBackground(UIUtils.COLOR_PRIMARY);
        btnFilter.setForeground(Color.WHITE);
        btnFilter.setFocusPainted(false);
        btnFilter.setBorderPainted(false);
        btnFilter.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btnFilter.setPreferredSize(new Dimension(140, 34));
        btnFilter.addActionListener(e -> loadReportDataAsync());

        leftFilter.add(lblPreset);
        leftFilter.add(cboTimePreset);
        leftFilter.add(Box.createHorizontalStrut(6));
        leftFilter.add(lblFrom);
        leftFilter.add(txtFromDate);
        leftFilter.add(lblTo);
        leftFilter.add(txtToDate);
        leftFilter.add(btnFilter);

        // Nhóm bên phải: Nút Xuất CSV và Label Loading
        JPanel rightFilter = new JPanel(new FlowLayout(FlowLayout.RIGHT, 12, 0));
        rightFilter.setOpaque(false);

        lblLoadingStatus = new JLabel("● Sẵn sàng");
        lblLoadingStatus.setFont(UIUtils.FONT_SMALL);
        lblLoadingStatus.setForeground(UIUtils.COLOR_SUCCESS);

        btnExport = new JButton("📥  Xuất Báo Cáo (CSV/Excel)");
        btnExport.setFont(UIUtils.FONT_BODY_BOLD);
        btnExport.setBackground(new Color(22, 163, 74)); // Green 600
        btnExport.setForeground(Color.WHITE);
        btnExport.setFocusPainted(false);
        btnExport.setBorderPainted(false);
        btnExport.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btnExport.setPreferredSize(new Dimension(210, 34));
        btnExport.addActionListener(e -> exportReportData());

        rightFilter.add(lblLoadingStatus);
        rightFilter.add(btnExport);

        filterBar.add(leftFilter, BorderLayout.WEST);
        filterBar.add(rightFilter, BorderLayout.EAST);
        container.add(filterBar, BorderLayout.SOUTH);

        return container;
    }

    /**
     * Thiết lập các mốc ngày mặc định ban đầu.
     */
    private void setupDefaultDates() {
        LocalDate now = LocalDate.now();
        LocalDate firstDay = now.withDayOfMonth(1);
        txtFromDate.setText(firstDay.format(DATE_FORMATTER));
        txtToDate.setText(now.format(DATE_FORMATTER));
    }

    /**
     * Tự động điều chỉnh khoảng ngày theo lựa chọn trong Preset.
     */
    private void onPresetChanged() {
        String selected = (String) cboTimePreset.getSelectedItem();
        LocalDate now = LocalDate.now();

        if ("Hôm Nay".equals(selected)) {
            txtFromDate.setText(now.format(DATE_FORMATTER));
            txtToDate.setText(now.format(DATE_FORMATTER));
        } else if ("7 Ngày Qua".equals(selected)) {
            txtFromDate.setText(now.minusDays(7).format(DATE_FORMATTER));
            txtToDate.setText(now.format(DATE_FORMATTER));
        } else if ("Tháng Này".equals(selected)) {
            txtFromDate.setText(now.withDayOfMonth(1).format(DATE_FORMATTER));
            txtToDate.setText(now.format(DATE_FORMATTER));
        }
        // "Tùy Chọn Ngày" cho phép người dùng tự do nhập
    }

    /**
     * Tạo 4 Thẻ KPI Summary Cards với thiết kế UI chuẩn doanh nghiệp.
     */
    private JPanel createStatCardsPanel() {
        JPanel panel = new JPanel(new GridLayout(1, 4, 16, 0));
        panel.setOpaque(false);

        cardSales = new StatCard("💰  Doanh Thu Bán Hàng", "0 VNĐ", "Đang cập nhật...", new Color(22, 163, 74), new Color(240, 253, 244));
        cardRental = new StatCard("🔄  Doanh Thu Cho Thuê", "0 VNĐ", "Đang cập nhật...", UIUtils.COLOR_PRIMARY, new Color(239, 246, 255));
        cardCosts = new StatCard("🛠️  Chi Phí (Bảo Trì & Nhập)", "0 VNĐ", "Đang cập nhật...", new Color(220, 38, 38), new Color(254, 242, 242));
        cardProfit = new StatCard("📈  Lợi Nhuận Ước Tính", "0 VNĐ", "Đang cập nhật...", new Color(124, 58, 237), new Color(245, 243, 255));

        panel.add(cardSales);
        panel.add(cardRental);
        panel.add(cardCosts);
        panel.add(cardProfit);

        return panel;
    }

    /**
     * Tab 1: Giao diện trực quan gồm Biểu đồ Swing 2D và Bảng Top 5 sản phẩm.
     */
    private JPanel createChartAndTopPanel() {
        JPanel panel = new JPanel(new BorderLayout(16, 16));
        panel.setBackground(Color.WHITE);
        panel.setBorder(new EmptyBorder(16, 16, 16, 16));

        // Phía trên hoặc bên trái: Biểu đồ xu hướng
        revenueChartPanel = new RevenueChartPanel();
        revenueChartPanel.setPreferredSize(new Dimension(0, 280));
        revenueChartPanel.setBorder(new CompoundBorder(
                new LineBorder(UIUtils.COLOR_BORDER, 1, true),
                new EmptyBorder(12, 16, 12, 16)
        ));

        // Phía dưới: Khung Top 5 Sản Phẩm Bán/Cho Thuê Chạy Nhất
        JPanel topProductsPanel = new JPanel(new BorderLayout(0, 8));
        topProductsPanel.setOpaque(false);

        JLabel lblTopTitle = new JLabel("🏆  TOP 5 SẢN PHẨM & THIẾT BỊ CÔNG NGHỆ DOANH THU CAO NHẤT");
        lblTopTitle.setFont(new Font("Segoe UI", Font.BOLD, 14));
        lblTopTitle.setForeground(UIUtils.COLOR_TEXT_MAIN);
        topProductsPanel.add(lblTopTitle, BorderLayout.NORTH);

        String[] headers = {"Xếp Hạng", "Mã Sản Phẩm", "Tên Thiết Bị / Dòng Máy", "Thương Hiệu", "SL Bán Đứt", "Lượt Cho Thuê", "Tổng Doanh Thu"};
        modelTopProducts = new DefaultTableModel(headers, 0) {
            @Override
            public boolean isCellEditable(int row, int column) { return false; }
        };

        tblTopProducts = new JTable(modelTopProducts);
        tblTopProducts.setFont(UIUtils.FONT_BODY);
        tblTopProducts.setRowHeight(32);
        tblTopProducts.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        tblTopProducts.setShowGrid(true);
        tblTopProducts.setGridColor(UIUtils.COLOR_BORDER);

        JTableHeader th = tblTopProducts.getTableHeader();
        th.setFont(UIUtils.FONT_BODY_BOLD);
        th.setBackground(new Color(241, 245, 249));
        th.setForeground(UIUtils.COLOR_TEXT_MAIN);
        th.setPreferredSize(new Dimension(0, 36));

        // Căn lề các cột
        DefaultTableCellRenderer centerRenderer = new DefaultTableCellRenderer();
        centerRenderer.setHorizontalAlignment(SwingConstants.CENTER);
        tblTopProducts.getColumnModel().getColumn(0).setCellRenderer(centerRenderer);
        tblTopProducts.getColumnModel().getColumn(1).setCellRenderer(centerRenderer);
        tblTopProducts.getColumnModel().getColumn(4).setCellRenderer(centerRenderer);
        tblTopProducts.getColumnModel().getColumn(5).setCellRenderer(centerRenderer);

        DefaultTableCellRenderer rightRenderer = new DefaultTableCellRenderer();
        rightRenderer.setHorizontalAlignment(SwingConstants.RIGHT);
        tblTopProducts.getColumnModel().getColumn(6).setCellRenderer(rightRenderer);

        tblTopProducts.getColumnModel().getColumn(0).setPreferredWidth(70);
        tblTopProducts.getColumnModel().getColumn(1).setPreferredWidth(100);
        tblTopProducts.getColumnModel().getColumn(2).setPreferredWidth(280);

        JScrollPane scrollPane = new JScrollPane(tblTopProducts);
        scrollPane.setBorder(new LineBorder(UIUtils.COLOR_BORDER, 1, true));
        scrollPane.setPreferredSize(new Dimension(0, 180));
        topProductsPanel.add(scrollPane, BorderLayout.CENTER);

        panel.add(revenueChartPanel, BorderLayout.CENTER);
        panel.add(topProductsPanel, BorderLayout.SOUTH);

        return panel;
    }

    /**
     * Tab 2: Bảng số liệu chi tiết từng đơn hàng, hợp đồng và chi phí.
     */
    private JPanel createDetailTablePanel() {
        JPanel panel = new JPanel(new BorderLayout(0, 12));
        panel.setBackground(Color.WHITE);
        panel.setBorder(new EmptyBorder(16, 16, 16, 16));

        // Thanh tìm kiếm nhanh trên bảng chi tiết
        JPanel toolbar = new JPanel(new BorderLayout());
        toolbar.setOpaque(false);

        JLabel lblDetailTitle = new JLabel("NHẬT KÝ CHI TIẾT CÁC PHIẾU GIAO DỊCH KINH DOANH TRONG KỲ");
        lblDetailTitle.setFont(new Font("Segoe UI", Font.BOLD, 14));
        lblDetailTitle.setForeground(UIUtils.COLOR_TEXT_MAIN);

        JPanel searchBox = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        searchBox.setOpaque(false);
        JLabel lblSearch = new JLabel("Lọc nhanh:");
        lblSearch.setFont(UIUtils.FONT_BODY);
        txtSearchDetail = new JTextField(16);
        txtSearchDetail.setFont(UIUtils.FONT_BODY);
        txtSearchDetail.setPreferredSize(new Dimension(180, 30));
        txtSearchDetail.addActionListener(e -> filterDetailTable());

        searchBox.add(lblSearch);
        searchBox.add(txtSearchDetail);

        toolbar.add(lblDetailTitle, BorderLayout.WEST);
        toolbar.add(searchBox, BorderLayout.EAST);
        panel.add(toolbar, BorderLayout.NORTH);

        // Bảng dữ liệu
        String[] headers = {"STT", "Ngày Phát Sinh", "Loại Nghiệp Vụ", "Mã Chứng Từ", "Khách Hàng / Đối Tác", "Doanh Thu Thu Được", "Chi Phí Phát Sinh", "Lợi Nhuận Ghi Nhận"};
        modelDetails = new DefaultTableModel(headers, 0) {
            @Override
            public boolean isCellEditable(int row, int column) { return false; }
        };

        tblDetails = new JTable(modelDetails);
        tblDetails.setFont(UIUtils.FONT_BODY);
        tblDetails.setRowHeight(30);
        tblDetails.setShowGrid(true);
        tblDetails.setGridColor(UIUtils.COLOR_BORDER);

        JTableHeader th = tblDetails.getTableHeader();
        th.setFont(UIUtils.FONT_BODY_BOLD);
        th.setBackground(new Color(241, 245, 249));
        th.setForeground(UIUtils.COLOR_TEXT_MAIN);
        th.setPreferredSize(new Dimension(0, 36));

        DefaultTableCellRenderer centerRenderer = new DefaultTableCellRenderer();
        centerRenderer.setHorizontalAlignment(SwingConstants.CENTER);
        tblDetails.getColumnModel().getColumn(0).setCellRenderer(centerRenderer);
        tblDetails.getColumnModel().getColumn(1).setCellRenderer(centerRenderer);
        tblDetails.getColumnModel().getColumn(2).setCellRenderer(centerRenderer);
        tblDetails.getColumnModel().getColumn(3).setCellRenderer(centerRenderer);

        DefaultTableCellRenderer rightRenderer = new DefaultTableCellRenderer();
        rightRenderer.setHorizontalAlignment(SwingConstants.RIGHT);
        tblDetails.getColumnModel().getColumn(5).setCellRenderer(rightRenderer);
        tblDetails.getColumnModel().getColumn(6).setCellRenderer(rightRenderer);
        tblDetails.getColumnModel().getColumn(7).setCellRenderer(rightRenderer);

        tblDetails.getColumnModel().getColumn(0).setPreferredWidth(50);
        tblDetails.getColumnModel().getColumn(1).setPreferredWidth(110);
        tblDetails.getColumnModel().getColumn(2).setPreferredWidth(110);
        tblDetails.getColumnModel().getColumn(3).setPreferredWidth(140);
        tblDetails.getColumnModel().getColumn(4).setPreferredWidth(240);

        JScrollPane scrollPane = new JScrollPane(tblDetails);
        scrollPane.setBorder(new LineBorder(UIUtils.COLOR_BORDER, 1, true));
        panel.add(scrollPane, BorderLayout.CENTER);

        return panel;
    }

    /**
     * Nạp dữ liệu báo cáo bất đồng bộ qua SwingWorker đảm bảo không khóa luồng EDT.
     */
    private void loadReportDataAsync() {
        final LocalDate fromDate;
        final LocalDate toDate;

        try {
            fromDate = LocalDate.parse(txtFromDate.getText().trim(), DATE_FORMATTER);
            toDate = LocalDate.parse(txtToDate.getText().trim(), DATE_FORMATTER);
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this,
                    "Định dạng ngày không hợp lệ! Vui lòng nhập đúng chuẩn yyyy-MM-dd (Ví dụ: 2026-11-01).",
                    "Lỗi Định Dạng", JOptionPane.WARNING_MESSAGE);
            return;
        }

        lblLoadingStatus.setText("● Đang tổng hợp dữ liệu...");
        lblLoadingStatus.setForeground(new Color(234, 88, 12)); // Orange
        btnFilter.setEnabled(false);
        btnExport.setEnabled(false);

        SwingWorker<ReportWorkerResult, Void> worker = new SwingWorker<>() {
            @Override
            protected ReportWorkerResult doInBackground() {
                RevenueReportData reportData = reportService.getRevenueReport(fromDate, toDate);
                List<TopProductReport> topProducts = reportService.getTopSellingProducts(5);
                return new ReportWorkerResult(reportData, topProducts);
            }

            @Override
            protected void done() {
                try {
                    ReportWorkerResult result = get();
                    currentReportData = result.reportData;
                    currentTopProducts = result.topProducts;

                    updateUIWithReportData(currentReportData, currentTopProducts);

                    lblLoadingStatus.setText("● Cập nhật thành công lúc: " + java.time.LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss")));
                    lblLoadingStatus.setForeground(UIUtils.COLOR_SUCCESS);

                } catch (Exception ex) {
                    lblLoadingStatus.setText("● Lỗi khi tải dữ liệu");
                    lblLoadingStatus.setForeground(UIUtils.COLOR_DANGER);
                    System.err.println("[ReportPanel] Lỗi tải báo cáo: " + ex.getMessage());
                } finally {
                    btnFilter.setEnabled(true);
                    btnExport.setEnabled(true);
                }
            }
        };

        worker.execute();
    }

    /**
     * Cập nhật dữ liệu đã tính toán lên các thành phần giao diện.
     */
    private void updateUIWithReportData(RevenueReportData data, List<TopProductReport> topProducts) {
        if (data == null) return;

        // 1. Cập nhật 4 Stat Cards
        cardSales.setValue(CURRENCY_FORMAT.format(data.getTotalSaleRevenue()));
        cardSales.setSubtitle(data.getTotalOrdersCount() + " hóa đơn bán lẻ đã thanh toán");

        cardRental.setValue(CURRENCY_FORMAT.format(data.getTotalRentalRevenue()));
        cardRental.setSubtitle(data.getTotalRentalsCount() + " hợp đồng cho thuê thiết bị");

        cardCosts.setValue(CURRENCY_FORMAT.format(data.getTotalCost()));
        cardCosts.setSubtitle(data.getTotalMaintenanceCount() + " lần bảo trì | " + data.getTotalImportsCount() + " đợt nhập");

        cardProfit.setValue(CURRENCY_FORMAT.format(data.getGrossProfit()));
        cardProfit.setSubtitle("Doanh thu ròng - Chi phí trực tiếp");

        // 2. Cập nhật Biểu đồ 2D
        revenueChartPanel.setData(data.getDailyPoints());

        // 3. Cập nhật Bảng Top 5 Sản Phẩm
        modelTopProducts.setRowCount(0);
        if (topProducts != null) {
            int rank = 1;
            for (TopProductReport p : topProducts) {
                modelTopProducts.addRow(new Object[]{
                        "#" + rank++,
                        p.getProductId(),
                        p.getProductName(),
                        p.getBrand(),
                        p.getQuantitySold() + " máy",
                        p.getRentalCount() + " lượt",
                        CURRENCY_FORMAT.format(p.getTotalRevenue())
                });
            }
        }

        // 4. Cập nhật Bảng Chi Tiết Giao Dịch
        populateDetailTable(data.getTransactions());
    }

    private void populateDetailTable(List<ReportTransaction> list) {
        modelDetails.setRowCount(0);
        if (list == null) return;

        int stt = 1;
        for (ReportTransaction tx : list) {
            modelDetails.addRow(new Object[]{
                    stt++,
                    tx.getDateStr(),
                    tx.getType(),
                    tx.getCode(),
                    tx.getCustomerName(),
                    tx.getRevenue() > 0 ? CURRENCY_FORMAT.format(tx.getRevenue()) : "-",
                    tx.getCost() > 0 ? CURRENCY_FORMAT.format(tx.getCost()) : "-",
                    CURRENCY_FORMAT.format(tx.getProfit())
            });
        }
    }

    /**
     * Lọc bảng chi tiết theo từ khóa nhập trong ô tìm kiếm.
     */
    private void filterDetailTable() {
        if (currentReportData == null) return;
        String kw = txtSearchDetail.getText().trim().toLowerCase();
        if (kw.isEmpty()) {
            populateDetailTable(currentReportData.getTransactions());
            return;
        }

        List<ReportTransaction> filtered = new ArrayList<>();
        for (ReportTransaction tx : currentReportData.getTransactions()) {
            if (tx.getCode().toLowerCase().contains(kw) ||
                    tx.getCustomerName().toLowerCase().contains(kw) ||
                    tx.getType().toLowerCase().contains(kw) ||
                    tx.getDateStr().contains(kw)) {
                filtered.add(tx);
            }
        }
        populateDetailTable(filtered);
    }

    /**
     * Thực hiện xuất file báo cáo dữ liệu chuẩn CSV/Excel UTF-8.
     */
    private void exportReportData() {
        if (currentReportData == null || currentReportData.getTransactions().isEmpty()) {
            JOptionPane.showMessageDialog(this, "Không có dữ liệu báo cáo trong kỳ này để xuất!", "Thông Báo", JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("Chọn Thư Mục Lưu Báo Cáo Doanh Thu (CSV)");
        String defaultFileName = "SmartTech_BaoCaoDoanhThu_" + LocalDate.now().format(DATE_FORMATTER) + ".csv";
        fileChooser.setSelectedFile(new File(defaultFileName));

        int userSelection = fileChooser.showSaveDialog(this);
        if (userSelection == JFileChooser.APPROVE_OPTION) {
            File fileToSave = fileChooser.getSelectedFile();
            if (!fileToSave.getName().toLowerCase().endsWith(".csv")) {
                fileToSave = new File(fileToSave.getParentFile(), fileToSave.getName() + ".csv");
            }

            // Chuẩn bị dữ liệu CSV
            List<Object[]> rows = new ArrayList<>();
            rows.add(new Object[]{"BÁO CÁO DOANH THU & TÀI CHÍNH DOANH NGHIỆP SMARTTECH ENTERPRISE"});
            rows.add(new Object[]{"Thời gian xuất:", LocalDate.now().format(DATE_FORMATTER)});
            rows.add(new Object[]{"Kỳ báo cáo:", txtFromDate.getText() + " đến " + txtToDate.getText()});
            rows.add(new Object[]{""});

            rows.add(new Object[]{"TỔNG HỢP CHỈ SỐ KINH DOANH"});
            rows.add(new Object[]{"Tổng Doanh Thu Bán Hàng (VNĐ):", currentReportData.getTotalSaleRevenue()});
            rows.add(new Object[]{"Tổng Doanh Thu Cho Thuê (VNĐ):", currentReportData.getTotalRentalRevenue()});
            rows.add(new Object[]{"Tổng Chi Phí Bảo Trì & Nhập Kho (VNĐ):", currentReportData.getTotalCost()});
            rows.add(new Object[]{"Lợi Nhuận Gộp Ước Tính (VNĐ):", currentReportData.getGrossProfit()});
            rows.add(new Object[]{""});

            rows.add(new Object[]{"DANH SÁCH CHI TIẾT CÁC PHIẾU GIAO DỊCH"});
            rows.add(new Object[]{"STT", "Ngày Phát Sinh", "Loại Nghiệp Vụ", "Mã Chứng Từ", "Khách Hàng / Đối Tác", "Doanh Thu", "Chi Phí", "Lợi Nhuận"});

            int stt = 1;
            for (ReportTransaction tx : currentReportData.getTransactions()) {
                rows.add(new Object[]{
                        stt++,
                        tx.getDateStr(),
                        tx.getType(),
                        tx.getCode(),
                        tx.getCustomerName(),
                        tx.getRevenue(),
                        tx.getCost(),
                        tx.getProfit()
                });
            }

            boolean success = reportService.exportReportToCSV(fileToSave, rows);
            if (success) {
                JOptionPane.showMessageDialog(this,
                        "Xuất báo cáo thành công ra tập tin:\n" + fileToSave.getAbsolutePath(),
                        "Xuất Báo Cáo Thành Công", JOptionPane.INFORMATION_MESSAGE);
            } else {
                JOptionPane.showMessageDialog(this,
                        "Có lỗi xảy ra khi ghi file báo cáo!",
                        "Lỗi Xuất File", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private static class ReportWorkerResult {
        final RevenueReportData reportData;
        final List<TopProductReport> topProducts;

        ReportWorkerResult(RevenueReportData reportData, List<TopProductReport> topProducts) {
            this.reportData = reportData;
            this.topProducts = topProducts;
        }
    }

    // =========================================================================
    // INNER CLASSES: CUSTOM STAT CARD & REVENUE CHART PANEL
    // =========================================================================

    /**
     * Card hiển thị chỉ số KPI chuyên nghiệp.
     */
    private static class StatCard extends JPanel {
        private final JLabel lblVal;
        private final JLabel lblSub;

        public StatCard(String title, String initialValue, String subtitle, Color themeColor, Color bgColor) {
            setLayout(new BorderLayout(0, 6));
            setBackground(bgColor);
            setBorder(new CompoundBorder(
                    new LineBorder(new Color(themeColor.getRed(), themeColor.getGreen(), themeColor.getBlue(), 60), 1, true),
                    new EmptyBorder(14, 16, 14, 16)
            ));

            JLabel lblTitle = new JLabel(title);
            lblTitle.setFont(new Font("Segoe UI", Font.BOLD, 12));
            lblTitle.setForeground(themeColor);

            lblVal = new JLabel(initialValue);
            lblVal.setFont(new Font("Segoe UI", Font.BOLD, 18));
            lblVal.setForeground(UIUtils.COLOR_TEXT_MAIN);

            lblSub = new JLabel(subtitle);
            lblSub.setFont(UIUtils.FONT_SMALL);
            lblSub.setForeground(UIUtils.COLOR_TEXT_MUTED);

            add(lblTitle, BorderLayout.NORTH);
            add(lblVal, BorderLayout.CENTER);
            add(lblSub, BorderLayout.SOUTH);
        }

        public void setValue(String val) { lblVal.setText(val); }
        public void setSubtitle(String sub) { lblSub.setText(sub); }
    }

    /**
     * Biểu đồ cột/đường vẽ bằng Custom Swing Graphics 2D trực tiếp trên JPanel.
     */
    private static class RevenueChartPanel extends JPanel {
        private List<DailyRevenuePoint> points = new ArrayList<>();

        public RevenueChartPanel() {
            setBackground(Color.WHITE);
        }

        public void setData(List<DailyRevenuePoint> data) {
            this.points = data != null ? data : new ArrayList<>();
            repaint();
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g.create();

            // Khử răng cưa Antialiasing
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

            int width = getWidth();
            int height = getHeight();

            int padLeft = 70;
            int padRight = 30;
            int padTop = 35;
            int padBottom = 45;

            // Tiêu đề & Chú thích (Legend)
            g2.setFont(new Font("Segoe UI", Font.BOLD, 13));
            g2.setColor(UIUtils.COLOR_TEXT_MAIN);
            g2.drawString("XU HƯỚNG DOANH THU THEO MỐC THỜI GIAN", padLeft, 22);

            // Legend
            int legX = width - 260;
            g2.setFont(UIUtils.FONT_SMALL);

            g2.setColor(new Color(22, 163, 74)); // Green
            g2.fillRect(legX, 12, 12, 12);
            g2.setColor(UIUtils.COLOR_TEXT_MAIN);
            g2.drawString("Doanh Thu Bán", legX + 16, 22);

            g2.setColor(UIUtils.COLOR_PRIMARY); // Blue
            g2.fillRect(legX + 120, 12, 12, 12);
            g2.setColor(UIUtils.COLOR_TEXT_MAIN);
            g2.drawString("Doanh Thu Thuê", legX + 136, 22);

            int plotW = width - padLeft - padRight;
            int plotH = height - padTop - padBottom;

            if (plotW <= 0 || plotH <= 0) {
                g2.dispose();
                return;
            }

            // Tìm giá trị max để scale trục Y
            double maxVal = 1000000.0;
            for (DailyRevenuePoint pt : points) {
                if (pt.getTotalRevenue() > maxVal) {
                    maxVal = pt.getTotalRevenue();
                }
            }
            // Làm tròn maxVal lên mốc đẹp
            maxVal = Math.ceil(maxVal / 1000000.0) * 1000000.0;
            if (maxVal == 0) maxVal = 1000000.0;

            // Vẽ lưới trục Y (Gridlines)
            int yTicks = 4;
            g2.setFont(new Font("Segoe UI", Font.PLAIN, 10));
            DecimalFormat compactDf = new DecimalFormat("#,##0 Tr");

            for (int i = 0; i <= yTicks; i++) {
                int y = padTop + (int) ((double) (yTicks - i) / yTicks * plotH);
                double val = (maxVal / yTicks) * i;

                g2.setColor(new Color(241, 245, 249));
                g2.drawLine(padLeft, y, width - padRight, y);

                g2.setColor(UIUtils.COLOR_TEXT_MUTED);
                String label = (val >= 1000000) ? compactDf.format(val / 1000000.0) : String.valueOf((int) val);
                FontMetrics fm = g2.getFontMetrics();
                g2.drawString(label, padLeft - fm.stringWidth(label) - 8, y + 4);
            }

            // Trục tọa độ X
            g2.setColor(UIUtils.COLOR_BORDER);
            g2.drawLine(padLeft, padTop + plotH, width - padRight, padTop + plotH);

            if (points.isEmpty()) {
                g2.setFont(UIUtils.FONT_BODY);
                g2.setColor(UIUtils.COLOR_TEXT_MUTED);
                String msg = "Không có giao dịch nào phát sinh trong khoảng thời gian đã chọn.";
                FontMetrics fm = g2.getFontMetrics();
                g2.drawString(msg, padLeft + (plotW - fm.stringWidth(msg)) / 2, padTop + plotH / 2);
                g2.dispose();
                return;
            }

            // Vẽ các cột Doanh Thu
            int n = points.size();
            double slotW = (double) plotW / n;
            int barWidth = Math.max(6, Math.min(28, (int) (slotW * 0.35)));

            for (int i = 0; i < n; i++) {
                DailyRevenuePoint pt = points.get(i);
                int slotCenterX = padLeft + (int) (i * slotW + slotW / 2);

                int saleBarH = (int) ((pt.getSaleRevenue() / maxVal) * plotH);
                int rentBarH = (int) ((pt.getRentalRevenue() / maxVal) * plotH);

                int saleX = slotCenterX - barWidth - 1;
                int saleY = padTop + plotH - saleBarH;

                int rentX = slotCenterX + 1;
                int rentY = padTop + plotH - rentBarH;

                // Cột bán hàng (Xanh lá)
                if (saleBarH > 0) {
                    g2.setColor(new Color(34, 197, 94));
                    g2.fillRect(saleX, saleY, barWidth, saleBarH);
                }

                // Cột cho thuê (Xanh dương)
                if (rentBarH > 0) {
                    g2.setColor(new Color(59, 130, 246));
                    g2.fillRect(rentX, rentY, barWidth, rentBarH);
                }

                // Nhãn ngày tháng trục X
                g2.setColor(UIUtils.COLOR_TEXT_MUTED);
                g2.setFont(new Font("Segoe UI", Font.PLAIN, 10));
                FontMetrics fm = g2.getFontMetrics();
                String lbl = pt.getDateLabel();

                // Chỉ vẽ nhãn nếu không bị chồng chéo
                int skip = Math.max(1, n / 12);
                if (i % skip == 0 || i == n - 1) {
                    g2.drawString(lbl, slotCenterX - fm.stringWidth(lbl) / 2, padTop + plotH + 16);
                }
            }

            g2.dispose();
        }
    }
}