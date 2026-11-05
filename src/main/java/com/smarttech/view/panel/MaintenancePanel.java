package com.smarttech.view.panel;

import com.smarttech.model.Equipment;
import com.smarttech.model.MaintenanceTicket;
import com.smarttech.model.Product;
import com.smarttech.service.EquipmentService;
import com.smarttech.service.MaintenanceService;
import com.smarttech.service.ProductService;
import com.smarttech.service.UserSession;
import com.smarttech.view.util.UIUtils;

import javax.swing.*;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.JTableHeader;
import java.awt.*;
import java.text.DecimalFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.List;

/**
 * Phân Hệ Quản Lý Bảo Trì, Sửa Chữa & Nhật Ký Thiết Bị Phần Cứng (MaintenancePanel).
 * Cung cấp giải pháp toàn diện: Tiếp nhận thiết bị hỏng hóc, bàn giao đơn vị bảo hành,
 * theo dõi tiến độ sửa chữa, nghiệm thu đưa lại về kho và báo cáo phân tích chi phí.
 * Toàn bộ luồng thao tác CSDL MongoDB thực hiện bất đồng bộ 100% qua SwingWorker.
 *
 * @author Senior Java Developer & Software Architect
 * @project SmartTech
 */
public class MaintenancePanel extends JPanel {

    private final MaintenanceService maintenanceService;
    private final EquipmentService equipmentService;
    private final ProductService productService;

    // Bộ nhớ đệm dữ liệu
    private final Map<String, Product> productCache;
    private final List<Equipment> availableEquipmentCache;
    private final List<MaintenanceTicket> activeTicketsCache;
    private final List<MaintenanceTicket> allTicketsCache;

    // Giao diện chính: JTabbedPane
    private JTabbedPane tabbedPane;

    // Tab 1: Tiếp nhận & Đang sửa chữa
    private JComboBox<String> cboFormSerial;
    private JTextField txtFormProductName;
    private JTextArea txtFormIssue;
    private JComboBox<String> cboFormVendor;
    private JTextField txtFormCost;
    private JTextField txtFormDays;
    private JButton btnSubmitTicket;

    private JTextField txtSearchActive;
    private JTable tblActiveTickets;
    private DefaultTableModel modelActiveTickets;
    private JButton btnCompleteMaintenance;
    private JButton btnRefreshActive;

    // Tab 2: Lịch sử & Thống kê
    private JLabel lblStatTotalCount;
    private JLabel lblStatActiveCount;
    private JLabel lblStatCompletedCount;
    private JLabel lblStatTotalCost;

    private JTextField txtSearchHistory;
    private JComboBox<String> cboStatusHistory;
    private JTable tblHistoryTickets;
    private DefaultTableModel modelHistoryTickets;
    private JButton btnRefreshHistory;

    private static final DecimalFormat CURRENCY_FORMAT = new DecimalFormat("#,##0 VNĐ");
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    public MaintenancePanel() {
        this.maintenanceService = new MaintenanceService();
        this.equipmentService = new EquipmentService();
        this.productService = new ProductService();

        this.productCache = new HashMap<>();
        this.availableEquipmentCache = new ArrayList<>();
        this.activeTicketsCache = new ArrayList<>();
        this.allTicketsCache = new ArrayList<>();

        initComponents();
        loadAllDataAsync();
    }

    private void initComponents() {
        setLayout(new BorderLayout(0, 16));
        setBackground(UIUtils.COLOR_BG_LIGHT);
        setBorder(new EmptyBorder(16, 20, 16, 20));

        // 1. TOP HEADER BANNER
        JPanel headerPanel = createHeaderPanel();
        add(headerPanel, BorderLayout.NORTH);

        // 2. TABS CONTAINER
        tabbedPane = new JTabbedPane();
        tabbedPane.setFont(UIUtils.FONT_BODY_BOLD);
        tabbedPane.setBackground(Color.WHITE);

        // Tab 1: Danh sách máy đang bảo trì & Form tiếp nhận mới
        JPanel tabActivePanel = createActiveAndReceptionTab();
        tabbedPane.addTab("🛠️ Thiết Bị Đang Bảo Trì & Tiếp Nhận Mới", tabActivePanel);

        // Tab 2: Lịch sử bảo dưỡng & Thống kê chi phí
        JPanel tabHistoryPanel = createHistoryAndStatsTab();
        tabbedPane.addTab("📜 Lịch Sử Bảo Trì & Thống Kê Chi Phí", tabHistoryPanel);

        add(tabbedPane, BorderLayout.CENTER);
    }

    /**
     * Tạo Header Banner thanh lịch, chuẩn giao diện ERP chuyên nghiệp.
     */
    private JPanel createHeaderPanel() {
        JPanel panel = new JPanel(new BorderLayout(16, 0));
        panel.setOpaque(false);

        JPanel textPanel = new JPanel(new GridLayout(2, 1, 0, 4));
        textPanel.setOpaque(false);

        JLabel lblTitle = new JLabel("PHÂN HỆ QUẢN LÝ BẢO TRÌ & SỬA CHỮA PHẦN CỨNG");
        lblTitle.setFont(UIUtils.FONT_TITLE);
        lblTitle.setForeground(UIUtils.COLOR_TEXT_MAIN);

        JLabel lblSubtitle = new JLabel("Theo dõi vòng đời thiết bị, tạo phiếu gửi bảo hành, nghiệm thu nhập kho & giám sát chi phí bảo dưỡng");
        lblSubtitle.setFont(UIUtils.FONT_SUBTITLE);
        lblSubtitle.setForeground(UIUtils.COLOR_TEXT_MUTED);

        textPanel.add(lblTitle);
        textPanel.add(lblSubtitle);
        panel.add(textPanel, BorderLayout.WEST);

        return panel;
    }

    // =========================================================================
    // TAB 1: TIẾP NHẬN BẢO TRÌ MỚI & BẢNG THIẾT BỊ ĐANG BẢO TRÌ (IN_PROGRESS)
    // =========================================================================

    private JPanel createActiveAndReceptionTab() {
        JPanel mainPanel = new JPanel(new BorderLayout(16, 0));
        mainPanel.setOpaque(false);

        // --- CỘT BÊN TRÁI: FORM TIẾP NHẬN BẢO TRÌ MỚI ---
        JPanel leftFormContainer = createReceptionFormPanel();
        leftFormContainer.setPreferredSize(new Dimension(390, 0));

        // --- CỘT BÊN PHẢI: BẢNG CÁC PHIẾU ĐANG XỬ LÝ (IN_PROGRESS) ---
        JPanel rightTableContainer = createActiveTicketsTablePanel();

        mainPanel.add(leftFormContainer, BorderLayout.WEST);
        mainPanel.add(rightTableContainer, BorderLayout.CENTER);

        return mainPanel;
    }

    private JPanel createReceptionFormPanel() {
        JPanel card = new JPanel(new BorderLayout(0, 14));
        card.setBackground(Color.WHITE);
        card.setBorder(new CompoundBorder(
                new LineBorder(UIUtils.COLOR_BORDER, 1, true),
                new EmptyBorder(18, 18, 18, 18)
        ));

        // Tiêu đề card form
        JLabel lblFormTitle = new JLabel("📋 Tiếp Nhận Phiếu Bảo Trì Mới");
        lblFormTitle.setFont(UIUtils.FONT_HEADER);
        lblFormTitle.setForeground(UIUtils.COLOR_PRIMARY);
        card.add(lblFormTitle, BorderLayout.NORTH);

        // Thân form
        JPanel formBody = new JPanel();
        formBody.setLayout(new BoxLayout(formBody, BoxLayout.Y_AXIS));
        formBody.setOpaque(false);

        // 1. Chọn số Serial phần cứng
        formBody.add(createFieldLabel("1. Chọn Thiết Bị / Số Serial Phần Cứng:*"));
        cboFormSerial = new JComboBox<>();
        cboFormSerial.setFont(UIUtils.FONT_BODY);
        cboFormSerial.setBackground(Color.WHITE);
        cboFormSerial.setPreferredSize(new Dimension(Integer.MAX_VALUE, 36));
        cboFormSerial.setMaximumSize(new Dimension(Integer.MAX_VALUE, 36));
        cboFormSerial.addActionListener(e -> onSerialSelected());
        formBody.add(cboFormSerial);
        formBody.add(Box.createVerticalStrut(10));

        // 2. Tên Sản Phẩm (Tự động hiển thị)
        formBody.add(createFieldLabel("Tên Dòng Sản Phẩm:"));
        txtFormProductName = new JTextField();
        txtFormProductName.setFont(UIUtils.FONT_BODY);
        txtFormProductName.setEditable(false);
        txtFormProductName.setBackground(new Color(248, 250, 252));
        txtFormProductName.setBorder(new CompoundBorder(new LineBorder(UIUtils.COLOR_BORDER, 1), new EmptyBorder(4, 8, 4, 8)));
        txtFormProductName.setPreferredSize(new Dimension(Integer.MAX_VALUE, 34));
        txtFormProductName.setMaximumSize(new Dimension(Integer.MAX_VALUE, 34));
        formBody.add(txtFormProductName);
        formBody.add(Box.createVerticalStrut(10));

        // 3. Đơn vị sửa chữa
        formBody.add(createFieldLabel("2. Đơn Vị Tiếp Nhận Sửa Chữa:*"));
        cboFormVendor = new JComboBox<>(new String[]{
                "Nội Bộ SmartTech (Kỹ Thuật Trụ Sở)",
                "Trung Tâm Bảo Hành Apple Care",
                "Trung Tâm Bảo Hành Dell Enterprise",
                "Trung Tâm Bảo Hành Asus / ROG Hub",
                "Trung Tâm Bảo Hành HP Authorized",
                "Trung Tâm Sửa Chữa Chuyên Sâu TTBH Ngoài"
        });
        cboFormVendor.setFont(UIUtils.FONT_BODY);
        cboFormVendor.setBackground(Color.WHITE);
        cboFormVendor.setPreferredSize(new Dimension(Integer.MAX_VALUE, 36));
        cboFormVendor.setMaximumSize(new Dimension(Integer.MAX_VALUE, 36));
        formBody.add(cboFormVendor);
        formBody.add(Box.createVerticalStrut(10));

        // 4. Chi phí dự toán & Ngày dự kiến
        JPanel dualRow = new JPanel(new GridLayout(1, 2, 10, 0));
        dualRow.setOpaque(false);
        dualRow.setMaximumSize(new Dimension(Integer.MAX_VALUE, 60));

        JPanel colCost = new JPanel(new BorderLayout(0, 4));
        colCost.setOpaque(false);
        colCost.add(createFieldLabel("Chi Phí Dự Toán (VNĐ):"), BorderLayout.NORTH);
        txtFormCost = new JTextField("0");
        txtFormCost.setFont(UIUtils.FONT_BODY);
        txtFormCost.setBorder(new CompoundBorder(new LineBorder(UIUtils.COLOR_BORDER, 1), new EmptyBorder(4, 8, 4, 8)));
        colCost.add(txtFormCost, BorderLayout.CENTER);

        JPanel colDays = new JPanel(new BorderLayout(0, 4));
        colDays.setOpaque(false);
        colDays.add(createFieldLabel("Số Ngày Dự Kiến:"), BorderLayout.NORTH);
        txtFormDays = new JTextField("3");
        txtFormDays.setFont(UIUtils.FONT_BODY);
        txtFormDays.setBorder(new CompoundBorder(new LineBorder(UIUtils.COLOR_BORDER, 1), new EmptyBorder(4, 8, 4, 8)));
        colDays.add(txtFormDays, BorderLayout.CENTER);

        dualRow.add(colCost);
        dualRow.add(colDays);
        formBody.add(dualRow);
        formBody.add(Box.createVerticalStrut(10));

        // 5. Mô tả sự cố / Lý do bảo dưỡng
        formBody.add(createFieldLabel("3. Mô Tả Sự Cố / Triệu Chứng Hỏng Hóc:*"));
        txtFormIssue = new JTextArea(4, 20);
        txtFormIssue.setFont(UIUtils.FONT_BODY);
        txtFormIssue.setLineWrap(true);
        txtFormIssue.setWrapStyleWord(true);
        JScrollPane scrollIssue = new JScrollPane(txtFormIssue);
        scrollIssue.setBorder(new LineBorder(UIUtils.COLOR_BORDER, 1));
        scrollIssue.setPreferredSize(new Dimension(Integer.MAX_VALUE, 80));
        scrollIssue.setMaximumSize(new Dimension(Integer.MAX_VALUE, 80));
        formBody.add(scrollIssue);
        formBody.add(Box.createVerticalStrut(16));

        // Nút Gửi Phiếu Bảo Trì Mới
        btnSubmitTicket = new JButton("+ 🚀 Tạo Phiếu Bảo Trì Mới");
        btnSubmitTicket.setFont(UIUtils.FONT_BODY_BOLD);
        btnSubmitTicket.setBackground(new Color(217, 119, 6)); // Amber 600
        btnSubmitTicket.setForeground(Color.WHITE);
        btnSubmitTicket.setFocusPainted(false);
        btnSubmitTicket.setBorderPainted(false);
        btnSubmitTicket.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btnSubmitTicket.setPreferredSize(new Dimension(Integer.MAX_VALUE, 42));
        btnSubmitTicket.setMaximumSize(new Dimension(Integer.MAX_VALUE, 42));
        btnSubmitTicket.addActionListener(e -> submitNewMaintenanceTicket());
        formBody.add(btnSubmitTicket);

        card.add(formBody, BorderLayout.CENTER);
        return card;
    }

    private JPanel createActiveTicketsTablePanel() {
        JPanel card = new JPanel(new BorderLayout(0, 12));
        card.setBackground(Color.WHITE);
        card.setBorder(new CompoundBorder(
                new LineBorder(UIUtils.COLOR_BORDER, 1, true),
                new EmptyBorder(16, 16, 16, 16)
        ));

        // Top Toolbar của Bảng Đang Sửa Chữa
        JPanel toolbar = new JPanel(new BorderLayout(10, 0));
        toolbar.setOpaque(false);

        JPanel searchBox = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        searchBox.setOpaque(false);
        JLabel lblSearch = new JLabel("Tìm nhanh:");
        lblSearch.setFont(UIUtils.FONT_BODY_BOLD);
        txtSearchActive = new JTextField(16);
        txtSearchActive.setFont(UIUtils.FONT_BODY);
        txtSearchActive.setPreferredSize(new Dimension(200, 34));
        txtSearchActive.setBorder(new CompoundBorder(new LineBorder(UIUtils.COLOR_BORDER, 1, true), new EmptyBorder(0, 8, 0, 8)));
        txtSearchActive.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) { filterActiveTickets(); }
            @Override
            public void removeUpdate(DocumentEvent e) { filterActiveTickets(); }
            @Override
            public void changedUpdate(DocumentEvent e) { filterActiveTickets(); }
        });
        searchBox.add(lblSearch);
        searchBox.add(txtSearchActive);

        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        actions.setOpaque(false);

        btnCompleteMaintenance = new JButton("✅ Nghiệm Thu & Đưa Về Kho");
        btnCompleteMaintenance.setFont(UIUtils.FONT_BODY_BOLD);
        btnCompleteMaintenance.setBackground(UIUtils.COLOR_SUCCESS);
        btnCompleteMaintenance.setForeground(Color.WHITE);
        btnCompleteMaintenance.setFocusPainted(false);
        btnCompleteMaintenance.setBorderPainted(false);
        btnCompleteMaintenance.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btnCompleteMaintenance.setPreferredSize(new Dimension(220, 36));
        btnCompleteMaintenance.addActionListener(e -> openCompleteDialogForSelectedRow());

        btnRefreshActive = new JButton("Làm Mới");
        btnRefreshActive.setFont(UIUtils.FONT_BODY);
        btnRefreshActive.setBackground(Color.WHITE);
        btnRefreshActive.setForeground(UIUtils.COLOR_TEXT_MAIN);
        btnRefreshActive.setBorder(new LineBorder(UIUtils.COLOR_BORDER, 1));
        btnRefreshActive.setFocusPainted(false);
        btnRefreshActive.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btnRefreshActive.setPreferredSize(new Dimension(90, 36));
        btnRefreshActive.addActionListener(e -> loadAllDataAsync());

        actions.add(btnCompleteMaintenance);
        actions.add(btnRefreshActive);

        toolbar.add(searchBox, BorderLayout.WEST);
        toolbar.add(actions, BorderLayout.EAST);
        card.add(toolbar, BorderLayout.NORTH);

        // Bảng dữ liệu Active Tickets
        String[] columns = {
                "STT", "Mã Phiếu", "Mã Serial", "Tên Sản Phẩm",
                "Đơn Vị Sửa Chữa", "Ngày Gửi", "Dự Kiến Xong", "Chi Phí Dự Toán", "Người Lập"
        };

        modelActiveTickets = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        tblActiveTickets = new JTable(modelActiveTickets);
        styleTable(tblActiveTickets);

        tblActiveTickets.getColumnModel().getColumn(0).setPreferredWidth(45);
        tblActiveTickets.getColumnModel().getColumn(1).setPreferredWidth(130);
        tblActiveTickets.getColumnModel().getColumn(2).setPreferredWidth(110);
        tblActiveTickets.getColumnModel().getColumn(3).setPreferredWidth(170);
        tblActiveTickets.getColumnModel().getColumn(4).setPreferredWidth(160);
        tblActiveTickets.getColumnModel().getColumn(5).setPreferredWidth(95);
        tblActiveTickets.getColumnModel().getColumn(6).setPreferredWidth(95);
        tblActiveTickets.getColumnModel().getColumn(7).setPreferredWidth(110);
        tblActiveTickets.getColumnModel().getColumn(8).setPreferredWidth(110);

        JScrollPane scrollTable = new JScrollPane(tblActiveTickets);
        scrollTable.setBorder(new LineBorder(UIUtils.COLOR_BORDER, 1));
        scrollTable.getViewport().setBackground(Color.WHITE);
        card.add(scrollTable, BorderLayout.CENTER);

        return card;
    }

    // =========================================================================
    // TAB 2: LỊCH SỬ BẢO TRÌ & THỐNG KÊ CHI PHÍ
    // =========================================================================

    private JPanel createHistoryAndStatsTab() {
        JPanel mainPanel = new JPanel(new BorderLayout(0, 16));
        mainPanel.setOpaque(false);

        // 1. CARDS THỐNG KÊ TỔNG QUAN
        JPanel statsPanel = createSummaryCardsPanel();
        mainPanel.add(statsPanel, BorderLayout.NORTH);

        // 2. BẢNG LỊCH SỬ VÀ BỘ LỌC
        JPanel tableCard = new JPanel(new BorderLayout(0, 12));
        tableCard.setBackground(Color.WHITE);
        tableCard.setBorder(new CompoundBorder(
                new LineBorder(UIUtils.COLOR_BORDER, 1, true),
                new EmptyBorder(16, 16, 16, 16)
        ));

        // Toolbar tìm kiếm & lọc trạng thái
        JPanel toolbar = new JPanel(new BorderLayout(12, 0));
        toolbar.setOpaque(false);

        JPanel leftFilters = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 0));
        leftFilters.setOpaque(false);

        JLabel lblSearch = new JLabel("Tìm kiếm:");
        lblSearch.setFont(UIUtils.FONT_BODY_BOLD);

        txtSearchHistory = new JTextField(20);
        txtSearchHistory.setFont(UIUtils.FONT_BODY);
        txtSearchHistory.setPreferredSize(new Dimension(240, 36));
        txtSearchHistory.setBorder(new CompoundBorder(new LineBorder(UIUtils.COLOR_BORDER, 1, true), new EmptyBorder(0, 10, 0, 10)));
        txtSearchHistory.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) { filterHistoryTickets(); }
            @Override
            public void removeUpdate(DocumentEvent e) { filterHistoryTickets(); }
            @Override
            public void changedUpdate(DocumentEvent e) { filterHistoryTickets(); }
        });

        JLabel lblStatus = new JLabel("Trạng thái:");
        lblStatus.setFont(UIUtils.FONT_BODY_BOLD);

        cboStatusHistory = new JComboBox<>(new String[]{
                "Tất Cả Trạng Thái",
                "Đang Bảo Trì (IN_PROGRESS)",
                "Đã Hoàn Tất (COMPLETED)",
                "Đã Hủy Bỏ (CANCELLED)"
        });
        cboStatusHistory.setFont(UIUtils.FONT_BODY);
        cboStatusHistory.setBackground(Color.WHITE);
        cboStatusHistory.setPreferredSize(new Dimension(200, 36));
        cboStatusHistory.addActionListener(e -> filterHistoryTickets());

        leftFilters.add(lblSearch);
        leftFilters.add(txtSearchHistory);
        leftFilters.add(lblStatus);
        leftFilters.add(cboStatusHistory);

        JPanel rightActions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        rightActions.setOpaque(false);

        btnRefreshHistory = new JButton("Làm Mới");
        btnRefreshHistory.setFont(UIUtils.FONT_BODY);
        btnRefreshHistory.setBackground(Color.WHITE);
        btnRefreshHistory.setForeground(UIUtils.COLOR_TEXT_MAIN);
        btnRefreshHistory.setBorder(new LineBorder(UIUtils.COLOR_BORDER, 1));
        btnRefreshHistory.setFocusPainted(false);
        btnRefreshHistory.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btnRefreshHistory.setPreferredSize(new Dimension(100, 36));
        btnRefreshHistory.addActionListener(e -> loadAllDataAsync());
        rightActions.add(btnRefreshHistory);

        toolbar.add(leftFilters, BorderLayout.WEST);
        toolbar.add(rightActions, BorderLayout.EAST);
        tableCard.add(toolbar, BorderLayout.NORTH);

        // Bảng lịch sử toàn bộ tickets
        String[] columns = {
                "STT", "Mã Phiếu", "Mã Serial", "Tên Sản Phẩm",
                "Đơn Vị Sửa Chữa", "Ngày Gửi", "Ngày Hoàn Tất", "Trạng Thái",
                "Chi Phí (VNĐ)", "Kết Quả / Ghi Chú Sửa Chữa", "Người Phụ Trách"
        };

        modelHistoryTickets = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        tblHistoryTickets = new JTable(modelHistoryTickets);
        styleTable(tblHistoryTickets);

        // Render cột trạng thái với badge màu
        tblHistoryTickets.getColumnModel().getColumn(7).setCellRenderer(new TicketStatusCellRenderer());

        tblHistoryTickets.getColumnModel().getColumn(0).setPreferredWidth(45);
        tblHistoryTickets.getColumnModel().getColumn(1).setPreferredWidth(130);
        tblHistoryTickets.getColumnModel().getColumn(2).setPreferredWidth(100);
        tblHistoryTickets.getColumnModel().getColumn(3).setPreferredWidth(160);
        tblHistoryTickets.getColumnModel().getColumn(4).setPreferredWidth(150);
        tblHistoryTickets.getColumnModel().getColumn(5).setPreferredWidth(90);
        tblHistoryTickets.getColumnModel().getColumn(6).setPreferredWidth(90);
        tblHistoryTickets.getColumnModel().getColumn(7).setPreferredWidth(130);
        tblHistoryTickets.getColumnModel().getColumn(8).setPreferredWidth(110);
        tblHistoryTickets.getColumnModel().getColumn(9).setPreferredWidth(220);
        tblHistoryTickets.getColumnModel().getColumn(10).setPreferredWidth(110);

        JScrollPane scrollPane = new JScrollPane(tblHistoryTickets);
        scrollPane.setBorder(new LineBorder(UIUtils.COLOR_BORDER, 1));
        scrollPane.getViewport().setBackground(Color.WHITE);
        tableCard.add(scrollPane, BorderLayout.CENTER);

        mainPanel.add(tableCard, BorderLayout.CENTER);
        return mainPanel;
    }

    private JPanel createSummaryCardsPanel() {
        JPanel container = new JPanel(new GridLayout(1, 4, 16, 0));
        container.setOpaque(false);

        lblStatTotalCount = new JLabel("0");
        lblStatActiveCount = new JLabel("0");
        lblStatCompletedCount = new JLabel("0");
        lblStatTotalCost = new JLabel("0 VNĐ");

        container.add(createStatCard("Tổng Lượt Bảo Trì", lblStatTotalCount, new Color(59, 130, 246))); // Blue
        container.add(createStatCard("Đang Sửa Chữa", lblStatActiveCount, new Color(217, 119, 6)));      // Amber
        container.add(createStatCard("Đã Nghiệm Thu", lblStatCompletedCount, new Color(22, 163, 74)));  // Green
        container.add(createStatCard("Tổng Chi Phí Tích Lũy", lblStatTotalCost, new Color(220, 38, 38))); // Red

        return container;
    }

    private JPanel createStatCard(String title, JLabel valueLabel, Color accentColor) {
        JPanel card = new JPanel(new BorderLayout(0, 6));
        card.setBackground(Color.WHITE);
        card.setBorder(new CompoundBorder(
                new LineBorder(UIUtils.COLOR_BORDER, 1, true),
                new EmptyBorder(14, 18, 14, 18)
        ));

        JLabel lblTitle = new JLabel(title);
        lblTitle.setFont(UIUtils.FONT_SMALL);
        lblTitle.setForeground(UIUtils.COLOR_TEXT_MUTED);

        valueLabel.setFont(new Font("Segoe UI", Font.BOLD, 20));
        valueLabel.setForeground(accentColor);

        card.add(lblTitle, BorderLayout.NORTH);
        card.add(valueLabel, BorderLayout.CENTER);
        return card;
    }

    // =========================================================================
    // NGHIỆP VỤ BẤT ĐỒNG BỘ (SWINGWORKER & EVENT HANDLERS)
    // =========================================================================

    /**
     * Tải toàn bộ dữ liệu CSDL MongoDB ngầm qua SwingWorker.
     */
    public void loadAllDataAsync() {
        btnRefreshActive.setEnabled(false);
        btnRefreshActive.setText("Đang tải...");
        btnRefreshHistory.setEnabled(false);

        SwingWorker<Void, Void> worker = new SwingWorker<>() {
            private List<Product> fetchedProducts;
            private List<Equipment> fetchedEquipments;
            private List<MaintenanceTicket> fetchedActiveTickets;
            private List<MaintenanceTicket> fetchedAllTickets;

            @Override
            protected Void doInBackground() {
                fetchedProducts = productService.getAllProducts();
                fetchedEquipments = equipmentService.getAllEquipments();
                fetchedActiveTickets = maintenanceService.getActiveTickets();
                fetchedAllTickets = maintenanceService.getAllTickets();
                return null;
            }

            @Override
            protected void done() {
                try {
                    get();

                    productCache.clear();
                    if (fetchedProducts != null) {
                        for (Product p : fetchedProducts) {
                            productCache.put(p.getId(), p);
                        }
                    }

                    availableEquipmentCache.clear();
                    if (fetchedEquipments != null) {
                        for (Equipment eq : fetchedEquipments) {
                            // Chỉ cho phép đưa đi bảo trì các thiết bị AVAILABLE hoặc BROKEN
                            if (Equipment.STATUS_AVAILABLE.equalsIgnoreCase(eq.getStatus())
                                    || Equipment.STATUS_BROKEN.equalsIgnoreCase(eq.getStatus())) {
                                availableEquipmentCache.add(eq);
                            }
                        }
                    }

                    activeTicketsCache.clear();
                    if (fetchedActiveTickets != null) {
                        activeTicketsCache.addAll(fetchedActiveTickets);
                    }

                    allTicketsCache.clear();
                    if (fetchedAllTickets != null) {
                        allTicketsCache.addAll(fetchedAllTickets);
                    }

                    // Cập nhật các thành phần giao diện
                    updateFormEquipmentCombo();
                    filterActiveTickets();
                    filterHistoryTickets();
                    updateSummaryStats();

                } catch (Exception e) {
                    System.err.println("[MaintenancePanel] Lỗi tải dữ liệu: " + e.getMessage());
                } finally {
                    btnRefreshActive.setEnabled(true);
                    btnRefreshActive.setText("Làm Mới");
                    btnRefreshHistory.setEnabled(true);
                }
            }
        };

        worker.execute();
    }

    /**
     * Cập nhật danh sách Serial vào Form tiếp nhận bảo trì.
     */
    private void updateFormEquipmentCombo() {
        cboFormSerial.removeAllItems();
        if (availableEquipmentCache.isEmpty()) {
            cboFormSerial.addItem("-- Hiện không có thiết bị khả dụng --");
            txtFormProductName.setText("");
            btnSubmitTicket.setEnabled(false);
            return;
        }

        btnSubmitTicket.setEnabled(true);
        for (Equipment eq : availableEquipmentCache) {
            String pName = (eq.getProductId() != null && productCache.containsKey(eq.getProductId()))
                    ? productCache.get(eq.getProductId()).getProductName()
                    : (eq.getProductId() != null ? eq.getProductId() : "N/A");
            cboFormSerial.addItem(eq.getId() + " - " + pName + " (" + eq.getStatus() + ")");
        }

        if (cboFormSerial.getItemCount() > 0) {
            cboFormSerial.setSelectedIndex(0);
            onSerialSelected();
        }
    }

    private void onSerialSelected() {
        String selected = (String) cboFormSerial.getSelectedItem();
        if (selected == null || selected.startsWith("--")) {
            txtFormProductName.setText("");
            return;
        }

        String serialId = selected.split(" - ")[0].trim();
        for (Equipment eq : availableEquipmentCache) {
            if (eq.getId().equalsIgnoreCase(serialId)) {
                String pName = productCache.containsKey(eq.getProductId())
                        ? productCache.get(eq.getProductId()).getProductName()
                        : eq.getProductId();
                txtFormProductName.setText(pName);
                break;
            }
        }
    }

    /**
     * Bắt sự kiện tạo phiếu tiếp nhận bảo trì mới.
     */
    private void submitNewMaintenanceTicket() {
        String selected = (String) cboFormSerial.getSelectedItem();
        if (selected == null || selected.startsWith("--")) {
            JOptionPane.showMessageDialog(this, "Vui lòng chọn thiết bị hợp lệ cần bảo trì!", "Thông báo", JOptionPane.WARNING_MESSAGE);
            return;
        }

        String serialId = selected.split(" - ")[0].trim();
        String vendor = (String) cboFormVendor.getSelectedItem();
        String issue = txtFormIssue.getText().trim();
        String costStr = txtFormCost.getText().trim().replace(",", "").replace(".", "");
        String daysStr = txtFormDays.getText().trim();

        if (issue.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Vui lòng nhập mô tả sự cố hoặc triệu chứng hỏng hóc!", "Cảnh báo", JOptionPane.WARNING_MESSAGE);
            txtFormIssue.requestFocus();
            return;
        }

        double estimatedCost;
        try {
            estimatedCost = Double.parseDouble(costStr);
            if (estimatedCost < 0) throw new NumberFormatException();
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Chi phí dự toán phải là số không âm!", "Lỗi định dạng", JOptionPane.ERROR_MESSAGE);
            txtFormCost.requestFocus();
            return;
        }

        int days = 3;
        try {
            days = Integer.parseInt(daysStr);
            if (days <= 0) days = 1;
        } catch (Exception ignored) {}

        LocalDate startDate = LocalDate.now();
        LocalDate expectedDate = startDate.plusDays(days);

        // Tìm productId và productName
        String productId = "";
        String productName = txtFormProductName.getText().trim();
        for (Equipment eq : availableEquipmentCache) {
            if (eq.getId().equalsIgnoreCase(serialId)) {
                productId = eq.getProductId();
                break;
            }
        }

        MaintenanceTicket ticket = new MaintenanceTicket();
        ticket.setSerialId(serialId);
        ticket.setProductId(productId);
        ticket.setProductName(productName);
        ticket.setIssueDescription(issue);
        ticket.setRepairVendor(vendor);
        ticket.setRepairCost(estimatedCost);
        ticket.setStartDate(startDate);
        ticket.setExpectedDate(expectedDate);

        btnSubmitTicket.setEnabled(false);
        btnSubmitTicket.setText("Đang ghi nhận...");

        SwingWorker<Boolean, Void> worker = new SwingWorker<>() {
            @Override
            protected Boolean doInBackground() {
                return maintenanceService.createMaintenanceTicket(ticket);
            }

            @Override
            protected void done() {
                try {
                    boolean success = get();
                    if (success) {
                        JOptionPane.showMessageDialog(MaintenancePanel.this,
                                "Tạo phiếu bảo trì [" + ticket.getId() + "] cho thiết bị " + serialId + " thành công!\nTrạng thái thiết bị đã chuyển sang 'MAINTENANCE'.",
                                "Tiếp Nhận Thành Công", JOptionPane.INFORMATION_MESSAGE);
                        txtFormIssue.setText("");
                        txtFormCost.setText("0");
                        loadAllDataAsync();
                    } else {
                        JOptionPane.showMessageDialog(MaintenancePanel.this, "Không thể tạo phiếu bảo trì!", "Lỗi", JOptionPane.ERROR_MESSAGE);
                    }
                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(MaintenancePanel.this, "Lỗi: " + ex.getMessage(), "Lỗi hệ thống", JOptionPane.ERROR_MESSAGE);
                } finally {
                    btnSubmitTicket.setEnabled(true);
                    btnSubmitTicket.setText("+ 🚀 Tạo Phiếu Bảo Trì Mới");
                }
            }
        };

        worker.execute();
    }

    /**
     * Mở Dialog Nghiệm Thu & Hoàn Tất Bảo Trì cho dòng được chọn.
     */
    private void openCompleteDialogForSelectedRow() {
        int selectedRow = tblActiveTickets.getSelectedRow();
        if (selectedRow < 0) {
            JOptionPane.showMessageDialog(this, "Vui lòng chọn một phiếu bảo trì đang sửa chữa trên bảng để nghiệm thu!", "Chưa chọn dòng", JOptionPane.WARNING_MESSAGE);
            return;
        }

        String ticketId = (String) modelActiveTickets.getValueAt(selectedRow, 1);
        String serialId = (String) modelActiveTickets.getValueAt(selectedRow, 2);
        String productName = (String) modelActiveTickets.getValueAt(selectedRow, 3);
        String vendor = (String) modelActiveTickets.getValueAt(selectedRow, 4);

        JDialog dialog = new JDialog((Frame) SwingUtilities.getWindowAncestor(this), "Nghiệm Thu Hoàn Tất Bảo Trì: " + ticketId, true);
        dialog.setSize(520, 480);
        dialog.setLocationRelativeTo(this);
        dialog.setResizable(false);

        JPanel panel = new JPanel(new BorderLayout(0, 16));
        panel.setBackground(Color.WHITE);
        panel.setBorder(new EmptyBorder(20, 24, 20, 24));

        JPanel form = new JPanel(new GridLayout(5, 2, 10, 14));
        form.setOpaque(false);

        JLabel lblTicketInfo = new JLabel(ticketId + " (" + serialId + ")");
        lblTicketInfo.setFont(UIUtils.FONT_BODY_BOLD);
        lblTicketInfo.setForeground(UIUtils.COLOR_PRIMARY);

        JLabel lblProductInfo = new JLabel(productName);
        lblProductInfo.setFont(UIUtils.FONT_BODY);

        JTextField txtActualCost = new JTextField("0");
        txtActualCost.setFont(UIUtils.FONT_BODY);

        JTextField txtResolution = new JTextField("Đã thay linh kiện, chạy thử nghiệm đạt tiêu chuẩn");
        txtResolution.setFont(UIUtils.FONT_BODY);

        JComboBox<String> cboTargetStatus = new JComboBox<>(new String[]{
                "AVAILABLE (Sẵn sàng trong kho để bán / cho thuê)",
                "BROKEN (Hỏng hóc nặng, không thể sửa chữa)",
                "DAMAGED (Còn sử dụng được nhưng giảm phẩm cấp)"
        });
        cboTargetStatus.setFont(UIUtils.FONT_BODY);
        cboTargetStatus.setBackground(Color.WHITE);

        form.add(new JLabel("Mã Phiếu & Serial:"));
        form.add(lblTicketInfo);
        form.add(new JLabel("Tên Sản Phẩm:"));
        form.add(lblProductInfo);
        form.add(new JLabel("Chi Phí Thực Tế (VNĐ):*"));
        form.add(txtActualCost);
        form.add(new JLabel("Kết Quả Sửa Chữa / Linh Kiện:*"));
        form.add(txtResolution);
        form.add(new JLabel("Trạng Thái Trả Về Kho:*"));
        form.add(cboTargetStatus);

        panel.add(form, BorderLayout.CENTER);

        // Buttons
        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 12, 0));
        btnPanel.setOpaque(false);

        JButton btnCancel = new JButton("Đóng");
        btnCancel.setFont(UIUtils.FONT_BODY);
        btnCancel.addActionListener(e -> dialog.dispose());

        JButton btnConfirm = new JButton("✓ Xác Nhận Nghiệm Thu");
        btnConfirm.setFont(UIUtils.FONT_BODY_BOLD);
        btnConfirm.setBackground(UIUtils.COLOR_SUCCESS);
        btnConfirm.setForeground(Color.WHITE);
        btnConfirm.setFocusPainted(false);
        btnConfirm.setCursor(new Cursor(Cursor.HAND_CURSOR));

        btnConfirm.addActionListener(e -> {
            String costRaw = txtActualCost.getText().trim().replace(",", "").replace(".", "");
            String resolution = txtResolution.getText().trim();
            String statusChoice = (String) cboTargetStatus.getSelectedItem();

            double actualCost;
            try {
                actualCost = Double.parseDouble(costRaw);
                if (actualCost < 0) throw new NumberFormatException();
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(dialog, "Chi phí thực tế phải là số không âm!", "Lỗi định dạng", JOptionPane.ERROR_MESSAGE);
                return;
            }

            if (resolution.isEmpty()) {
                JOptionPane.showMessageDialog(dialog, "Vui lòng nhập kết quả xử lý và linh kiện thay thế!", "Cảnh báo", JOptionPane.WARNING_MESSAGE);
                return;
            }

            String targetStatus = Equipment.STATUS_AVAILABLE;
            if (statusChoice != null && statusChoice.startsWith("BROKEN")) {
                targetStatus = Equipment.STATUS_BROKEN;
            }

            final String finalTargetStatus = targetStatus;
            btnConfirm.setEnabled(false);
            btnConfirm.setText("Đang xử lý...");

            SwingWorker<Boolean, Void> worker = new SwingWorker<>() {
                @Override
                protected Boolean doInBackground() {
                    return maintenanceService.completeMaintenance(ticketId, resolution, actualCost, finalTargetStatus);
                }

                @Override
                protected void done() {
                    try {
                        boolean ok = get();
                        if (ok) {
                            JOptionPane.showMessageDialog(MaintenancePanel.this,
                                    "Đã nghiệm thu phiếu bảo trì [" + ticketId + "] thành công!\nThiết bị đã chuyển sang trạng thái: " + finalTargetStatus,
                                    "Hoàn Tất Nghiệm Thu", JOptionPane.INFORMATION_MESSAGE);
                            dialog.dispose();
                            loadAllDataAsync();
                        } else {
                            JOptionPane.showMessageDialog(dialog, "Không thể hoàn tất bảo trì phiếu này!", "Thất bại", JOptionPane.ERROR_MESSAGE);
                            btnConfirm.setEnabled(true);
                            btnConfirm.setText("✓ Xác Nhận Nghiệm Thu");
                        }
                    } catch (Exception ex) {
                        JOptionPane.showMessageDialog(dialog, "Lỗi: " + ex.getMessage(), "Lỗi hệ thống", JOptionPane.ERROR_MESSAGE);
                        btnConfirm.setEnabled(true);
                        btnConfirm.setText("✓ Xác Nhận Nghiệm Thu");
                    }
                }
            };
            worker.execute();
        });

        btnPanel.add(btnCancel);
        btnPanel.add(btnConfirm);
        panel.add(btnPanel, BorderLayout.SOUTH);

        dialog.setContentPane(panel);
        dialog.setVisible(true);
    }

    /**
     * Lọc và đổ dữ liệu lên bảng Phiếu Đang Xử Lý (IN_PROGRESS).
     */
    private void filterActiveTickets() {
        String keyword = txtSearchActive != null ? txtSearchActive.getText().trim().toLowerCase() : "";
        modelActiveTickets.setRowCount(0);
        int stt = 1;

        for (MaintenanceTicket t : activeTicketsCache) {
            boolean match = keyword.isEmpty()
                    || (t.getId() != null && t.getId().toLowerCase().contains(keyword))
                    || (t.getSerialId() != null && t.getSerialId().toLowerCase().contains(keyword))
                    || (t.getProductName() != null && t.getProductName().toLowerCase().contains(keyword))
                    || (t.getRepairVendor() != null && t.getRepairVendor().toLowerCase().contains(keyword))
                    || (t.getEmployeeName() != null && t.getEmployeeName().toLowerCase().contains(keyword));

            if (match) {
                String startStr = t.getStartDate() != null ? t.getStartDate().format(DATE_FORMATTER) : "N/A";
                String expStr = t.getExpectedDate() != null ? t.getExpectedDate().format(DATE_FORMATTER) : "N/A";

                modelActiveTickets.addRow(new Object[]{
                        stt++,
                        t.getId(),
                        t.getSerialId(),
                        t.getProductName() != null ? t.getProductName() : "",
                        t.getRepairVendor() != null ? t.getRepairVendor() : "",
                        startStr,
                        expStr,
                        CURRENCY_FORMAT.format(t.getRepairCost()),
                        t.getEmployeeName() != null ? t.getEmployeeName() : ""
                });
            }
        }
    }

    /**
     * Lọc và đổ dữ liệu lên bảng Toàn Bộ Lịch Sử Bảo Trì.
     */
    private void filterHistoryTickets() {
        String keyword = txtSearchHistory != null ? txtSearchHistory.getText().trim().toLowerCase() : "";
        String statusFilter = cboStatusHistory != null ? (String) cboStatusHistory.getSelectedItem() : "Tất Cả Trạng Thái";

        modelHistoryTickets.setRowCount(0);
        int stt = 1;

        for (MaintenanceTicket t : allTicketsCache) {
            boolean matchKeyword = keyword.isEmpty()
                    || (t.getId() != null && t.getId().toLowerCase().contains(keyword))
                    || (t.getSerialId() != null && t.getSerialId().toLowerCase().contains(keyword))
                    || (t.getProductName() != null && t.getProductName().toLowerCase().contains(keyword))
                    || (t.getRepairVendor() != null && t.getRepairVendor().toLowerCase().contains(keyword))
                    || (t.getResolutionNote() != null && t.getResolutionNote().toLowerCase().contains(keyword))
                    || (t.getEmployeeName() != null && t.getEmployeeName().toLowerCase().contains(keyword));

            boolean matchStatus = true;
            if ("Đang Bảo Trì (IN_PROGRESS)".equals(statusFilter)) {
                matchStatus = MaintenanceTicket.STATUS_IN_PROGRESS.equalsIgnoreCase(t.getStatus());
            } else if ("Đã Hoàn Tất (COMPLETED)".equals(statusFilter)) {
                matchStatus = MaintenanceTicket.STATUS_COMPLETED.equalsIgnoreCase(t.getStatus());
            } else if ("Đã Hủy Bỏ (CANCELLED)".equals(statusFilter)) {
                matchStatus = MaintenanceTicket.STATUS_CANCELLED.equalsIgnoreCase(t.getStatus());
            }

            if (matchKeyword && matchStatus) {
                String startStr = t.getStartDate() != null ? t.getStartDate().format(DATE_FORMATTER) : "N/A";
                String compStr = t.getCompletionDate() != null ? t.getCompletionDate().format(DATE_FORMATTER) : "-";

                modelHistoryTickets.addRow(new Object[]{
                        stt++,
                        t.getId(),
                        t.getSerialId(),
                        t.getProductName() != null ? t.getProductName() : "",
                        t.getRepairVendor() != null ? t.getRepairVendor() : "",
                        startStr,
                        compStr,
                        t.getStatus() != null ? t.getStatus() : MaintenanceTicket.STATUS_IN_PROGRESS,
                        CURRENCY_FORMAT.format(t.getRepairCost()),
                        (t.getResolutionNote() != null && !t.getResolutionNote().isEmpty()) ? t.getResolutionNote() : (t.getIssueDescription() != null ? t.getIssueDescription() : "-"),
                        t.getEmployeeName() != null ? t.getEmployeeName() : ""
                });
            }
        }
    }

    /**
     * Cập nhật các chỉ số thống kê tổng hợp trên Tab 2.
     */
    private void updateSummaryStats() {
        int total = allTicketsCache.size();
        int active = 0;
        int completed = 0;
        double totalCost = 0.0;

        for (MaintenanceTicket t : allTicketsCache) {
            if (MaintenanceTicket.STATUS_IN_PROGRESS.equalsIgnoreCase(t.getStatus())) {
                active++;
            } else if (MaintenanceTicket.STATUS_COMPLETED.equalsIgnoreCase(t.getStatus())) {
                completed++;
                totalCost += t.getRepairCost();
            }
        }

        lblStatTotalCount.setText(String.valueOf(total));
        lblStatActiveCount.setText(String.valueOf(active));
        lblStatCompletedCount.setText(String.valueOf(completed));
        lblStatTotalCost.setText(CURRENCY_FORMAT.format(totalCost));
    }

    // =========================================================================
    // TIỆN ÍCH GIAO DIỆN & RENDERERS
    // =========================================================================

    private JLabel createFieldLabel(String text) {
        JLabel label = new JLabel(text);
        label.setFont(UIUtils.FONT_BODY_BOLD);
        label.setForeground(UIUtils.COLOR_TEXT_MAIN);
        return label;
    }

    private void styleTable(JTable table) {
        table.setFont(UIUtils.FONT_BODY);
        table.setRowHeight(36);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.setShowGrid(false);
        table.setIntercellSpacing(new Dimension(0, 0));
        table.setSelectionBackground(new Color(239, 246, 255)); // Blue 50
        table.setSelectionForeground(UIUtils.COLOR_TEXT_MAIN);

        JTableHeader header = table.getTableHeader();
        header.setFont(UIUtils.FONT_BODY_BOLD);
        header.setBackground(new Color(241, 245, 249)); // Slate 100
        header.setForeground(new Color(51, 65, 85));     // Slate 700
        header.setPreferredSize(new Dimension(0, 40));
        header.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, UIUtils.COLOR_BORDER));

        DefaultTableCellRenderer centerRenderer = new DefaultTableCellRenderer();
        centerRenderer.setHorizontalAlignment(SwingConstants.CENTER);
        table.getColumnModel().getColumn(0).setCellRenderer(centerRenderer);
    }

    /**
     * Renderer hiển thị Badge màu sắc cho trạng thái phiếu bảo trì.
     */
    private static class TicketStatusCellRenderer extends DefaultTableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(JTable table, Object value,
                                                       boolean isSelected, boolean hasFocus, int row, int column) {
            JLabel label = (JLabel) super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
            label.setHorizontalAlignment(SwingConstants.CENTER);
            label.setFont(new Font("Segoe UI", Font.BOLD, 12));

            String status = value != null ? value.toString() : "";
            if (MaintenanceTicket.STATUS_IN_PROGRESS.equalsIgnoreCase(status)) {
                label.setText(" ĐANG SỬA CHỮA ");
                label.setBackground(new Color(254, 243, 199)); // Amber 100
                label.setForeground(new Color(180, 83, 9));    // Amber 700
            } else if (MaintenanceTicket.STATUS_COMPLETED.equalsIgnoreCase(status)) {
                label.setText(" ĐÃ NGHIỆM THU ");
                label.setBackground(new Color(220, 252, 231)); // Green 100
                label.setForeground(new Color(21, 128, 61));   // Green 700
            } else if (MaintenanceTicket.STATUS_CANCELLED.equalsIgnoreCase(status)) {
                label.setText(" ĐÃ HỦY BỎ ");
                label.setBackground(new Color(254, 226, 226)); // Red 100
                label.setForeground(new Color(185, 28, 28));   // Red 700
            } else {
                label.setText(" " + status + " ");
                label.setBackground(new Color(241, 245, 249));
                label.setForeground(new Color(71, 85, 105));
            }

            label.setOpaque(true);
            return label;
        }
    }
}
