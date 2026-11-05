package com.smarttech.view.panel;

import com.smarttech.model.Coupon;
import com.smarttech.model.Order;
import com.smarttech.repository.OrderRepository;
import com.smarttech.service.CouponService;
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
import java.util.ArrayList;
import java.util.List;

/**
 * Phân Hệ Quản Lý Chương Trình Khuyến Mãi, Mã Giảm Giá & Voucher Khách Hàng (PromotionManagementPanel).
 * Cung cấp giải pháp toàn diện:
 * - Tab 1: Danh Sách Mã Giảm Giá & Tạo Mới Voucher (Form nhập liệu + Bảng quản lý real-time).
 * - Tab 2: Thống Kê Hiệu Quả & Nhật Ký Áp Dụng Mã (KPI Cards + Bảng hiệu suất voucher & Đơn hàng).
 *
 * Toàn bộ các thao tác truy vấn và cập nhật CSDL MongoDB đều chạy ngầm bằng SwingWorker.
 *
 * @author Senior Java Developer & Software Architect
 * @project SmartTech
 */
public class PromotionManagementPanel extends JPanel {

    private final CouponService couponService;
    private final OrderRepository orderRepository;

    // Cache dữ liệu
    private final List<Coupon> couponListCache = new ArrayList<>();
    private final List<Order> orderDiscountListCache = new ArrayList<>();

    // Components Tab 1: Form Thêm / Cập Nhật Mã
    private JTextField txtCode;
    private JTextField txtTitle;
    private JComboBox<String> cboType;
    private JTextField txtValue;
    private JTextField txtMinOrder;
    private JTextField txtMaxDiscount;
    private JTextField txtStartDate;
    private JTextField txtEndDate;
    private JTextField txtLimit;
    private JButton btnGenerateCode;
    private JButton btnSaveCoupon;
    private JButton btnResetForm;

    // Components Tab 1: Bảng Danh Sách Voucher
    private JTextField txtSearchCoupon;
    private JComboBox<String> cboFilterStatus;
    private JTable tblCoupons;
    private DefaultTableModel modelCoupons;
    private JButton btnToggleStatus;
    private JButton btnDeleteCoupon;
    private JButton btnRefreshCoupons;

    // Components Tab 2: Thống Kê & Nhật Ký
    private JLabel lblStatActiveCoupons;
    private JLabel lblStatTotalUsedCount;
    private JLabel lblStatTotalDiscountGiven;
    private JLabel lblStatTotalPrograms;

    private JTable tblPerformance;
    private DefaultTableModel modelPerformance;
    private JTable tblOrderLogs;
    private DefaultTableModel modelOrderLogs;
    private JButton btnRefreshStats;

    private static final DecimalFormat CURRENCY_FORMAT = new DecimalFormat("#,##0 VNĐ");
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter ISO_FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE;

    public PromotionManagementPanel() {
        this.couponService = new CouponService();
        this.orderRepository = new OrderRepository();

        initComponents();
        loadAllDataAsync();
    }

    private void initComponents() {
        setLayout(new BorderLayout(0, 16));
        setBackground(UIUtils.COLOR_BG_LIGHT);
        setBorder(new EmptyBorder(16, 20, 16, 20));

        // 1. TOP HEADER BAR
        JPanel topHeader = createTopHeader();
        add(topHeader, BorderLayout.NORTH);

        // 2. MAIN TABBED PANE
        JTabbedPane tabbedPane = new JTabbedPane();
        tabbedPane.setFont(UIUtils.FONT_BODY_BOLD);
        tabbedPane.setBackground(Color.WHITE);

        // Tab 1: Danh Sách & Tạo Mới Voucher
        tabbedPane.addTab("🎁  Danh Sách Mã Giảm Giá & Tạo Mới Voucher", createCouponManagementTab());

        // Tab 2: Thống Kê Hiệu Quả & Nhật Ký Áp Dụng Mã
        tabbedPane.addTab("📊  Thống Kê Hiệu Quả & Nhật Ký Áp Dụng Mã", createStatsAndLogsTab());

        add(tabbedPane, BorderLayout.CENTER);
    }

    /**
     * Tạo Header của màn hình Quản trị Khuyến Mãi.
     */
    private JPanel createTopHeader() {
        JPanel header = new JPanel(new BorderLayout());
        header.setOpaque(false);

        JPanel titleContainer = new JPanel(new GridLayout(2, 1, 0, 4));
        titleContainer.setOpaque(false);

        JLabel lblTitle = new JLabel("QUẢN LÝ CHƯƠNG TRÌNH KHUYẾN MÃI & MÃ GIẢM GIÁ");
        lblTitle.setFont(UIUtils.FONT_TITLE);
        lblTitle.setForeground(UIUtils.COLOR_PRIMARY);

        JLabel lblSubtitle = new JLabel("Phát hành voucher ưu đãi, cấu hình trần chiết khấu, giới hạn lượt dùng và giám sát chi phí khuyến mãi");
        lblSubtitle.setFont(UIUtils.FONT_SUBTITLE);
        lblSubtitle.setForeground(UIUtils.COLOR_TEXT_MUTED);

        titleContainer.add(lblTitle);
        titleContainer.add(lblSubtitle);

        header.add(titleContainer, BorderLayout.WEST);
        return header;
    }

    // =========================================================================
    // TAB 1: FORM TẠO MÃ & BẢNG VOUCHER REAL-TIME
    // =========================================================================

    private JPanel createCouponManagementTab() {
        JPanel panel = new JPanel(new BorderLayout(16, 0));
        panel.setOpaque(false);

        // --- KHUNG BÊN TRÁI: FORM THÊM / CẬP NHẬT MÃ KHUYẾN MÃI ---
        JPanel leftFormContainer = createCouponFormPanel();
        leftFormContainer.setPreferredSize(new Dimension(410, 0));

        // --- KHUNG BÊN PHẢI: BẢNG DANH SÁCH MÃ KHUYẾN MÃI ---
        JPanel rightTableContainer = createCouponTablePanel();

        panel.add(leftFormContainer, BorderLayout.WEST);
        panel.add(rightTableContainer, BorderLayout.CENTER);

        return panel;
    }

    /**
     * Tạo Khung Form Bên Trái (Thêm / Cập Nhật Mã Khuyến Mãi).
     */
    private JPanel createCouponFormPanel() {
        JPanel card = new JPanel(new BorderLayout(0, 12));
        card.setBackground(Color.WHITE);
        card.setBorder(new CompoundBorder(
                new LineBorder(UIUtils.COLOR_BORDER, 1, true),
                new EmptyBorder(16, 18, 16, 18)
        ));

        JLabel lblFormTitle = new JLabel("🎟️  Thiết Lập Mã Giảm Giá Mới");
        lblFormTitle.setFont(UIUtils.FONT_HEADER);
        lblFormTitle.setForeground(UIUtils.COLOR_PRIMARY);
        card.add(lblFormTitle, BorderLayout.NORTH);

        JPanel formBody = new JPanel();
        formBody.setLayout(new BoxLayout(formBody, BoxLayout.Y_AXIS));
        formBody.setOpaque(false);

        // 1. Mã Voucher & Nút Sinh Mã Ngẫu Nhiên
        formBody.add(createFieldLabel("1. Mã Voucher (Viết liền, không dấu):*"));
        JPanel codeRow = new JPanel(new BorderLayout(8, 0));
        codeRow.setOpaque(false);
        codeRow.setMaximumSize(new Dimension(Integer.MAX_VALUE, 36));

        txtCode = new JTextField();
        txtCode.setFont(new Font("Segoe UI", Font.BOLD, 13));
        txtCode.setBorder(new CompoundBorder(new LineBorder(UIUtils.COLOR_BORDER, 1), new EmptyBorder(4, 8, 4, 8)));

        btnGenerateCode = new JButton("⚡ Tự Động");
        btnGenerateCode.setFont(UIUtils.FONT_SMALL);
        btnGenerateCode.setBackground(new Color(241, 245, 249));
        btnGenerateCode.setForeground(UIUtils.COLOR_PRIMARY);
        btnGenerateCode.setFocusPainted(false);
        btnGenerateCode.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btnGenerateCode.addActionListener(e -> txtCode.setText(couponService.generateRandomCouponCode("SMART")));

        codeRow.add(txtCode, BorderLayout.CENTER);
        codeRow.add(btnGenerateCode, BorderLayout.EAST);
        formBody.add(codeRow);
        formBody.add(Box.createVerticalStrut(8));

        // 2. Tên chương trình
        formBody.add(createFieldLabel("2. Tên Chương Trình Khuyến Mãi:*"));
        txtTitle = new JTextField();
        txtTitle.setFont(UIUtils.FONT_BODY);
        txtTitle.setBorder(new CompoundBorder(new LineBorder(UIUtils.COLOR_BORDER, 1), new EmptyBorder(4, 8, 4, 8)));
        txtTitle.setMaximumSize(new Dimension(Integer.MAX_VALUE, 34));
        formBody.add(txtTitle);
        formBody.add(Box.createVerticalStrut(8));

        // 3. Loại giảm giá & Mức giảm
        JPanel dualRow1 = new JPanel(new GridLayout(1, 2, 10, 0));
        dualRow1.setOpaque(false);
        dualRow1.setMaximumSize(new Dimension(Integer.MAX_VALUE, 58));

        JPanel colType = new JPanel(new BorderLayout(0, 4));
        colType.setOpaque(false);
        colType.add(createFieldLabel("Loại Giảm Giá:*"), BorderLayout.NORTH);
        cboType = new JComboBox<>(new String[]{"Giảm Phần Trăm (%)", "Giảm Tiền Trực Tiếp (VNĐ)"});
        cboType.setFont(UIUtils.FONT_BODY);
        cboType.setBackground(Color.WHITE);
        cboType.addActionListener(e -> updateMaxDiscountState());
        colType.add(cboType, BorderLayout.CENTER);

        JPanel colValue = new JPanel(new BorderLayout(0, 4));
        colValue.setOpaque(false);
        colValue.add(createFieldLabel("Mức Giảm (% / VNĐ):*"), BorderLayout.NORTH);
        txtValue = new JTextField("10");
        txtValue.setFont(UIUtils.FONT_BODY);
        txtValue.setBorder(new CompoundBorder(new LineBorder(UIUtils.COLOR_BORDER, 1), new EmptyBorder(4, 8, 4, 8)));
        colValue.add(txtValue, BorderLayout.CENTER);

        dualRow1.add(colType);
        dualRow1.add(colValue);
        formBody.add(dualRow1);
        formBody.add(Box.createVerticalStrut(8));

        // 4. Đơn hàng tối thiểu & Mức giảm tối đa
        JPanel dualRow2 = new JPanel(new GridLayout(1, 2, 10, 0));
        dualRow2.setOpaque(false);
        dualRow2.setMaximumSize(new Dimension(Integer.MAX_VALUE, 58));

        JPanel colMin = new JPanel(new BorderLayout(0, 4));
        colMin.setOpaque(false);
        colMin.add(createFieldLabel("Đơn Tối Thiểu (VNĐ):"), BorderLayout.NORTH);
        txtMinOrder = new JTextField("500000");
        txtMinOrder.setFont(UIUtils.FONT_BODY);
        txtMinOrder.setBorder(new CompoundBorder(new LineBorder(UIUtils.COLOR_BORDER, 1), new EmptyBorder(4, 8, 4, 8)));
        colMin.add(txtMinOrder, BorderLayout.CENTER);

        JPanel colMax = new JPanel(new BorderLayout(0, 4));
        colMax.setOpaque(false);
        colMax.add(createFieldLabel("Giảm Tối Đa (VNĐ):"), BorderLayout.NORTH);
        txtMaxDiscount = new JTextField("300000");
        txtMaxDiscount.setFont(UIUtils.FONT_BODY);
        txtMaxDiscount.setBorder(new CompoundBorder(new LineBorder(UIUtils.COLOR_BORDER, 1), new EmptyBorder(4, 8, 4, 8)));
        colMax.add(txtMaxDiscount, BorderLayout.CENTER);

        dualRow2.add(colMin);
        dualRow2.add(colMax);
        formBody.add(dualRow2);
        formBody.add(Box.createVerticalStrut(8));

        // 5. Ngày bắt đầu & Ngày hết hạn
        JPanel dualRow3 = new JPanel(new GridLayout(1, 2, 10, 0));
        dualRow3.setOpaque(false);
        dualRow3.setMaximumSize(new Dimension(Integer.MAX_VALUE, 58));

        JPanel colStart = new JPanel(new BorderLayout(0, 4));
        colStart.setOpaque(false);
        colStart.add(createFieldLabel("Ngày Bắt Đầu:"), BorderLayout.NORTH);
        txtStartDate = new JTextField(LocalDate.now().format(ISO_FORMATTER));
        txtStartDate.setFont(UIUtils.FONT_BODY);
        txtStartDate.setBorder(new CompoundBorder(new LineBorder(UIUtils.COLOR_BORDER, 1), new EmptyBorder(4, 8, 4, 8)));
        colStart.add(txtStartDate, BorderLayout.CENTER);

        JPanel colEnd = new JPanel(new BorderLayout(0, 4));
        colEnd.setOpaque(false);
        colEnd.add(createFieldLabel("Ngày Hết Hạn:"), BorderLayout.NORTH);
        txtEndDate = new JTextField(LocalDate.now().plusMonths(1).format(ISO_FORMATTER));
        txtEndDate.setFont(UIUtils.FONT_BODY);
        txtEndDate.setBorder(new CompoundBorder(new LineBorder(UIUtils.COLOR_BORDER, 1), new EmptyBorder(4, 8, 4, 8)));
        colEnd.add(txtEndDate, BorderLayout.CENTER);

        dualRow3.add(colStart);
        dualRow3.add(colEnd);
        formBody.add(dualRow3);
        formBody.add(Box.createVerticalStrut(8));

        // 6. Tổng số lượt sử dụng
        formBody.add(createFieldLabel("3. Giới Hạn Lượt Dùng Toàn Hệ Thống:*"));
        txtLimit = new JTextField("100");
        txtLimit.setFont(UIUtils.FONT_BODY);
        txtLimit.setBorder(new CompoundBorder(new LineBorder(UIUtils.COLOR_BORDER, 1), new EmptyBorder(4, 8, 4, 8)));
        txtLimit.setMaximumSize(new Dimension(Integer.MAX_VALUE, 34));
        formBody.add(txtLimit);
        formBody.add(Box.createVerticalStrut(14));

        // 7. Buttons: Lưu & Nhập Lại
        JPanel btnRow = new JPanel(new GridLayout(1, 2, 10, 0));
        btnRow.setOpaque(false);
        btnRow.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));

        btnResetForm = new JButton("🔄 Nhập Lại");
        btnResetForm.setFont(UIUtils.FONT_BODY);
        btnResetForm.setBackground(Color.WHITE);
        btnResetForm.setForeground(UIUtils.COLOR_TEXT_MAIN);
        btnResetForm.setBorder(new LineBorder(UIUtils.COLOR_BORDER, 1));
        btnResetForm.setFocusPainted(false);
        btnResetForm.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btnResetForm.addActionListener(e -> resetForm());

        btnSaveCoupon = new JButton("+ 🚀 Lưu Mã Khuyến Mãi");
        btnSaveCoupon.setFont(UIUtils.FONT_BODY_BOLD);
        btnSaveCoupon.setBackground(UIUtils.COLOR_PRIMARY);
        btnSaveCoupon.setForeground(Color.WHITE);
        btnSaveCoupon.setFocusPainted(false);
        btnSaveCoupon.setBorderPainted(false);
        btnSaveCoupon.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btnSaveCoupon.addActionListener(e -> submitCouponForm());

        btnRow.add(btnResetForm);
        btnRow.add(btnSaveCoupon);
        formBody.add(btnRow);

        card.add(formBody, BorderLayout.CENTER);
        return card;
    }

    private void updateMaxDiscountState() {
        boolean isPercent = cboType.getSelectedIndex() == 0;
        txtMaxDiscount.setEnabled(isPercent);
        if (!isPercent) {
            txtMaxDiscount.setBackground(new Color(241, 245, 249));
        } else {
            txtMaxDiscount.setBackground(Color.WHITE);
        }
    }

    /**
     * Tạo Khung Bảng Danh Sách Bên Phải.
     */
    private JPanel createCouponTablePanel() {
        JPanel card = new JPanel(new BorderLayout(0, 12));
        card.setBackground(Color.WHITE);
        card.setBorder(new CompoundBorder(
                new LineBorder(UIUtils.COLOR_BORDER, 1, true),
                new EmptyBorder(14, 16, 14, 16)
        ));

        // Toolbar
        JPanel toolbar = new JPanel(new BorderLayout(10, 0));
        toolbar.setOpaque(false);

        JPanel leftFilters = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        leftFilters.setOpaque(false);

        JLabel lblSearch = new JLabel("Tìm kiếm:");
        lblSearch.setFont(UIUtils.FONT_BODY_BOLD);

        txtSearchCoupon = new JTextField(14);
        txtSearchCoupon.setFont(UIUtils.FONT_BODY);
        txtSearchCoupon.setPreferredSize(new Dimension(170, 36));
        txtSearchCoupon.setBorder(new CompoundBorder(new LineBorder(UIUtils.COLOR_BORDER, 1, true), new EmptyBorder(0, 8, 0, 8)));
        txtSearchCoupon.getDocument().addDocumentListener(new DocumentListener() {
            @Override public void insertUpdate(DocumentEvent e) { filterCoupons(); }
            @Override public void removeUpdate(DocumentEvent e) { filterCoupons(); }
            @Override public void changedUpdate(DocumentEvent e) { filterCoupons(); }
        });

        JLabel lblFilter = new JLabel("Trạng thái:");
        lblFilter.setFont(UIUtils.FONT_BODY_BOLD);

        cboFilterStatus = new JComboBox<>(new String[]{
                "Tất Cả Trạng Thái",
                "Đang Hoạt Động (ACTIVE)",
                "Đã Hết Hạn (EXPIRED)",
                "Tạm Dừng (DISABLED)"
        });
        cboFilterStatus.setFont(UIUtils.FONT_BODY);
        cboFilterStatus.setBackground(Color.WHITE);
        cboFilterStatus.setPreferredSize(new Dimension(180, 36));
        cboFilterStatus.addActionListener(e -> filterCoupons());

        leftFilters.add(lblSearch);
        leftFilters.add(txtSearchCoupon);
        leftFilters.add(lblFilter);
        leftFilters.add(cboFilterStatus);

        JPanel rightActions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        rightActions.setOpaque(false);

        btnToggleStatus = new JButton("🔒 Bật / Tắt Mã");
        btnToggleStatus.setFont(UIUtils.FONT_BODY_BOLD);
        btnToggleStatus.setBackground(new Color(241, 245, 249));
        btnToggleStatus.setForeground(UIUtils.COLOR_TEXT_MAIN);
        btnToggleStatus.setBorder(new LineBorder(UIUtils.COLOR_BORDER, 1));
        btnToggleStatus.setFocusPainted(false);
        btnToggleStatus.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btnToggleStatus.setPreferredSize(new Dimension(125, 36));
        btnToggleStatus.addActionListener(e -> toggleSelectedCoupon());

        btnDeleteCoupon = new JButton("🗑️ Xóa");
        btnDeleteCoupon.setFont(UIUtils.FONT_BODY_BOLD);
        btnDeleteCoupon.setBackground(new Color(254, 226, 226));
        btnDeleteCoupon.setForeground(UIUtils.COLOR_DANGER);
        btnDeleteCoupon.setBorder(new LineBorder(new Color(252, 165, 165), 1));
        btnDeleteCoupon.setFocusPainted(false);
        btnDeleteCoupon.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btnDeleteCoupon.setPreferredSize(new Dimension(85, 36));
        btnDeleteCoupon.addActionListener(e -> deleteSelectedCoupon());

        btnRefreshCoupons = new JButton("Làm Mới");
        btnRefreshCoupons.setFont(UIUtils.FONT_BODY);
        btnRefreshCoupons.setBackground(Color.WHITE);
        btnRefreshCoupons.setForeground(UIUtils.COLOR_TEXT_MAIN);
        btnRefreshCoupons.setBorder(new LineBorder(UIUtils.COLOR_BORDER, 1));
        btnRefreshCoupons.setFocusPainted(false);
        btnRefreshCoupons.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btnRefreshCoupons.setPreferredSize(new Dimension(85, 36));
        btnRefreshCoupons.addActionListener(e -> loadAllDataAsync());

        rightActions.add(btnToggleStatus);
        rightActions.add(btnDeleteCoupon);
        rightActions.add(btnRefreshCoupons);

        toolbar.add(leftFilters, BorderLayout.WEST);
        toolbar.add(rightActions, BorderLayout.EAST);
        card.add(toolbar, BorderLayout.NORTH);

        // Bảng dữ liệu Coupons
        String[] columns = {
                "STT", "Mã Code", "Tên Chương Trình", "Mức Giảm",
                "Đơn Tối Thiểu", "Thời Hạn", "Lượt Dùng", "Trạng Thái"
        };

        modelCoupons = new DefaultTableModel(columns, 0) {
            @Override public boolean isCellEditable(int row, int column) { return false; }
        };

        tblCoupons = new JTable(modelCoupons);
        styleTable(tblCoupons);

        tblCoupons.getColumnModel().getColumn(7).setCellRenderer(new CouponStatusBadgeRenderer());
        tblCoupons.getColumnModel().getColumn(0).setPreferredWidth(45);
        tblCoupons.getColumnModel().getColumn(1).setPreferredWidth(125);
        tblCoupons.getColumnModel().getColumn(2).setPreferredWidth(190);
        tblCoupons.getColumnModel().getColumn(3).setPreferredWidth(110);
        tblCoupons.getColumnModel().getColumn(4).setPreferredWidth(115);
        tblCoupons.getColumnModel().getColumn(5).setPreferredWidth(170);
        tblCoupons.getColumnModel().getColumn(6).setPreferredWidth(95);
        tblCoupons.getColumnModel().getColumn(7).setPreferredWidth(130);

        JScrollPane scrollPane = new JScrollPane(tblCoupons);
        scrollPane.setBorder(new LineBorder(UIUtils.COLOR_BORDER, 1));
        scrollPane.getViewport().setBackground(Color.WHITE);
        card.add(scrollPane, BorderLayout.CENTER);

        return card;
    }

    // =========================================================================
    // TAB 2: THỐNG KÊ HIỆU QUẢ & NHẬT KÝ ÁP DỤNG MÃ
    // =========================================================================

    private JPanel createStatsAndLogsTab() {
        JPanel mainPanel = new JPanel(new BorderLayout(0, 16));
        mainPanel.setOpaque(false);

        // 1. CARDS THỐNG KÊ KPI
        JPanel statsPanel = createSummaryCardsPanel();
        mainPanel.add(statsPanel, BorderLayout.NORTH);

        // 2. KHU VỰC BẢNG: HIỆU SUẤT VOUCHER & NHẬT KÝ ĐƠN HÀNG
        JPanel tablesContainer = new JPanel(new GridLayout(2, 1, 0, 14));
        tablesContainer.setOpaque(false);

        // Bảng 1: Hiệu suất & Tỷ lệ lấp đầy của các Voucher
        JPanel performanceCard = new JPanel(new BorderLayout(0, 10));
        performanceCard.setBackground(Color.WHITE);
        performanceCard.setBorder(new CompoundBorder(
                new LineBorder(UIUtils.COLOR_BORDER, 1, true),
                new EmptyBorder(12, 16, 12, 16)
        ));

        JPanel perfHeader = new JPanel(new BorderLayout());
        perfHeader.setOpaque(false);
        JLabel lblPerfTitle = new JLabel("📊  Hiệu Suất Phát Hành & Tỷ Lệ Sử Dụng Voucher");
        lblPerfTitle.setFont(UIUtils.FONT_HEADER);
        lblPerfTitle.setForeground(UIUtils.COLOR_TEXT_MAIN);

        btnRefreshStats = new JButton("Làm Mới Số Liệu");
        btnRefreshStats.setFont(UIUtils.FONT_BODY);
        btnRefreshStats.setBackground(Color.WHITE);
        btnRefreshStats.setBorder(new LineBorder(UIUtils.COLOR_BORDER, 1));
        btnRefreshStats.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btnRefreshStats.addActionListener(e -> loadAllDataAsync());

        perfHeader.add(lblPerfTitle, BorderLayout.WEST);
        perfHeader.add(btnRefreshStats, BorderLayout.EAST);
        performanceCard.add(perfHeader, BorderLayout.NORTH);

        String[] perfColumns = {
                "STT", "Mã Voucher", "Chương Trình Khuyến Mãi", "Hình Thức Giảm",
                "Mức Ưu Đãi", "Đã Dùng / Tổng Lượt", "Tỷ Lệ Chuyển Đổi (%)", "Đánh Giá Hiệu Quả"
        };
        modelPerformance = new DefaultTableModel(perfColumns, 0) {
            @Override public boolean isCellEditable(int row, int column) { return false; }
        };
        tblPerformance = new JTable(modelPerformance);
        styleTable(tblPerformance);
        tblPerformance.getColumnModel().getColumn(7).setCellRenderer(new PerformanceRatingCellRenderer());

        JScrollPane scrollPerf = new JScrollPane(tblPerformance);
        scrollPerf.setBorder(new LineBorder(UIUtils.COLOR_BORDER, 1));
        scrollPerf.getViewport().setBackground(Color.WHITE);
        performanceCard.add(scrollPerf, BorderLayout.CENTER);

        // Bảng 2: Lịch sử các đơn hàng thực tế đã được hưởng chiết khấu
        JPanel ordersCard = new JPanel(new BorderLayout(0, 10));
        ordersCard.setBackground(Color.WHITE);
        ordersCard.setBorder(new CompoundBorder(
                new LineBorder(UIUtils.COLOR_BORDER, 1, true),
                new EmptyBorder(12, 16, 12, 16)
        ));

        JLabel lblOrderLogs = new JLabel("📑  Nhật Ký Các Đơn Hàng Áp Dụng Chiết Khấu / Ưu Đãi Thành Công");
        lblOrderLogs.setFont(UIUtils.FONT_HEADER);
        lblOrderLogs.setForeground(UIUtils.COLOR_TEXT_MAIN);
        ordersCard.add(lblOrderLogs, BorderLayout.NORTH);

        String[] orderLogColumns = {
                "STT", "Mã Đơn Hàng", "Ngày Giao Dịch", "Khách Hàng",
                "Tổng Tiền Gốc", "Số Tiền Được Giảm (VNĐ)", "Thực Thu (VNĐ)", "Hình Thức Thanh Toán"
        };
        modelOrderLogs = new DefaultTableModel(orderLogColumns, 0) {
            @Override public boolean isCellEditable(int row, int column) { return false; }
        };
        tblOrderLogs = new JTable(modelOrderLogs);
        styleTable(tblOrderLogs);

        JScrollPane scrollOrders = new JScrollPane(tblOrderLogs);
        scrollOrders.setBorder(new LineBorder(UIUtils.COLOR_BORDER, 1));
        scrollOrders.getViewport().setBackground(Color.WHITE);
        ordersCard.add(scrollOrders, BorderLayout.CENTER);

        tablesContainer.add(performanceCard);
        tablesContainer.add(ordersCard);

        mainPanel.add(tablesContainer, BorderLayout.CENTER);
        return mainPanel;
    }

    private JPanel createSummaryCardsPanel() {
        JPanel container = new JPanel(new GridLayout(1, 4, 16, 0));
        container.setOpaque(false);

        lblStatActiveCoupons = new JLabel("0");
        lblStatTotalUsedCount = new JLabel("0 Lượt");
        lblStatTotalDiscountGiven = new JLabel("0 VNĐ");
        lblStatTotalPrograms = new JLabel("0");

        container.add(createStatCard("Tổng Số Mã Đang Chạy", lblStatActiveCoupons, UIUtils.COLOR_SUCCESS));
        container.add(createStatCard("Lượt Khách Đã Dùng Mã", lblStatTotalUsedCount, new Color(59, 130, 246))); // Blue
        container.add(createStatCard("Tổng Tiền Ưu Đãi Đã Giảm", lblStatTotalDiscountGiven, UIUtils.COLOR_DANGER));
        container.add(createStatCard("Tổng Số Chương Trình", lblStatTotalPrograms, new Color(124, 58, 237))); // Purple

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
    // NGHIỆP VỤ BẤT ĐỒNG BỘ (SWINGWORKER & HANDLERS)
    // =========================================================================

    /**
     * Tải toàn bộ dữ liệu CSDL MongoDB ngầm qua SwingWorker.
     */
    public void loadAllDataAsync() {
        btnRefreshCoupons.setEnabled(false);
        btnRefreshCoupons.setText("Đang tải...");
        btnRefreshStats.setEnabled(false);

        SwingWorker<Void, Void> worker = new SwingWorker<>() {
            private List<Coupon> fetchedCoupons;
            private List<Order> fetchedOrders;

            @Override
            protected Void doInBackground() {
                fetchedCoupons = couponService.getAllCoupons();
                fetchedOrders = orderRepository.findAll();
                return null;
            }

            @Override
            protected void done() {
                try {
                    get();

                    couponListCache.clear();
                    if (fetchedCoupons != null) {
                        couponListCache.addAll(fetchedCoupons);
                    }

                    orderDiscountListCache.clear();
                    if (fetchedOrders != null) {
                        for (Order ord : fetchedOrders) {
                            if (ord.getDiscountAmount() > 0) {
                                orderDiscountListCache.add(ord);
                            }
                        }
                    }

                    filterCoupons();
                    updatePerformanceAndStats();

                } catch (Exception e) {
                    System.err.println("[PromotionManagementPanel] Lỗi tải dữ liệu: " + e.getMessage());
                } finally {
                    btnRefreshCoupons.setEnabled(true);
                    btnRefreshCoupons.setText("Làm Mới");
                    btnRefreshStats.setEnabled(true);
                }
            }
        };

        worker.execute();
    }

    /**
     * Lọc và đổ danh sách Coupons lên JTable ở Tab 1.
     */
    private void filterCoupons() {
        String keyword = txtSearchCoupon != null ? txtSearchCoupon.getText().trim().toUpperCase() : "";
        String statusFilter = cboFilterStatus != null ? (String) cboFilterStatus.getSelectedItem() : "Tất Cả Trạng Thái";

        modelCoupons.setRowCount(0);
        int stt = 1;

        LocalDate today = LocalDate.now();

        for (Coupon c : couponListCache) {
            boolean matchKeyword = keyword.isEmpty()
                    || c.getCode().contains(keyword)
                    || (c.getTitle() != null && c.getTitle().toUpperCase().contains(keyword));

            // Xác định trạng thái động
            String dynamicStatus = c.getStatus() != null ? c.getStatus() : Coupon.STATUS_ACTIVE;
            if (c.getEndDate() != null && today.isAfter(c.getEndDate())) {
                dynamicStatus = Coupon.STATUS_EXPIRED;
            } else if (c.getUsedCount() >= c.getUsageLimit()) {
                dynamicStatus = Coupon.STATUS_EXPIRED;
            }

            boolean matchStatus = true;
            if ("Đang Hoạt Động (ACTIVE)".equals(statusFilter)) {
                matchStatus = Coupon.STATUS_ACTIVE.equalsIgnoreCase(dynamicStatus);
            } else if ("Đã Hết Hạn (EXPIRED)".equals(statusFilter)) {
                matchStatus = Coupon.STATUS_EXPIRED.equalsIgnoreCase(dynamicStatus);
            } else if ("Tạm Dừng (DISABLED)".equals(statusFilter)) {
                matchStatus = Coupon.STATUS_DISABLED.equalsIgnoreCase(dynamicStatus);
            }

            if (matchKeyword && matchStatus) {
                String discountDisplay;
                if (Coupon.TYPE_FIXED_AMOUNT.equalsIgnoreCase(c.getDiscountType())) {
                    discountDisplay = CURRENCY_FORMAT.format(c.getDiscountValue());
                } else {
                    discountDisplay = (int) c.getDiscountValue() + "%";
                    if (c.getMaxDiscountAmount() > 0) {
                        discountDisplay += " (max " + CURRENCY_FORMAT.format(c.getMaxDiscountAmount()) + ")";
                    }
                }

                String period = (c.getStartDate() != null ? c.getStartDate().format(DATE_FORMATTER) : "N/A")
                        + " ➔ " + (c.getEndDate() != null ? c.getEndDate().format(DATE_FORMATTER) : "Vô hạn");

                String usage = c.getUsedCount() + " / " + c.getUsageLimit();

                modelCoupons.addRow(new Object[]{
                        stt++,
                        c.getCode(),
                        c.getTitle() != null ? c.getTitle() : "Ưu đãi khách hàng",
                        discountDisplay,
                        CURRENCY_FORMAT.format(c.getMinOrderAmount()),
                        period,
                        usage,
                        dynamicStatus
                });
            }
        }
    }

    /**
     * Cập nhật số liệu KPI và đổ danh sách bảng hiệu suất ở Tab 2.
     */
    private void updatePerformanceAndStats() {
        int totalPrograms = couponListCache.size();
        int activePrograms = 0;
        int totalUsed = 0;
        double totalDiscountFromCoupons = 0.0;

        LocalDate today = LocalDate.now();

        modelPerformance.setRowCount(0);
        int perfStt = 1;

        for (Coupon c : couponListCache) {
            totalUsed += c.getUsedCount();

            boolean isActive = Coupon.STATUS_ACTIVE.equalsIgnoreCase(c.getStatus())
                    && (c.getStartDate() == null || !today.isBefore(c.getStartDate()))
                    && (c.getEndDate() == null || !today.isAfter(c.getEndDate()))
                    && c.getUsedCount() < c.getUsageLimit();

            if (isActive) {
                activePrograms++;
            }

            // Tính tỷ lệ chuyển đổi sử dụng voucher
            double conversionRate = c.getUsageLimit() > 0 ? ((double) c.getUsedCount() / c.getUsageLimit()) * 100.0 : 0.0;

            String rating;
            if (conversionRate >= 75.0) {
                rating = "⭐ RẤT CAO";
            } else if (conversionRate >= 40.0) {
                rating = "TIỀM NĂNG";
            } else if (conversionRate > 0.0) {
                rating = "TRUNG BÌNH";
            } else {
                rating = "CHƯA DÙNG";
            }

            String discountDesc = Coupon.TYPE_FIXED_AMOUNT.equalsIgnoreCase(c.getDiscountType())
                    ? CURRENCY_FORMAT.format(c.getDiscountValue())
                    : (int) c.getDiscountValue() + "%";

            modelPerformance.addRow(new Object[]{
                    perfStt++,
                    c.getCode(),
                    c.getTitle(),
                    Coupon.TYPE_FIXED_AMOUNT.equalsIgnoreCase(c.getDiscountType()) ? "Cố Định (VNĐ)" : "Phần Trăm (%)",
                    discountDesc,
                    c.getUsedCount() + " / " + c.getUsageLimit(),
                    String.format("%.1f%%", conversionRate),
                    rating
            });
        }

        // Tính tổng tiền giảm giá từ danh sách đơn hàng thực tế
        for (Order ord : orderDiscountListCache) {
            totalDiscountFromCoupons += ord.getDiscountAmount();
        }

        lblStatActiveCoupons.setText(String.valueOf(activePrograms));
        lblStatTotalUsedCount.setText(totalUsed + " Lượt");
        lblStatTotalDiscountGiven.setText(CURRENCY_FORMAT.format(totalDiscountFromCoupons));
        lblStatTotalPrograms.setText(String.valueOf(totalPrograms));

        // Đổ bảng đơn hàng hưởng chiết khấu
        modelOrderLogs.setRowCount(0);
        int ordStt = 1;
        for (Order ord : orderDiscountListCache) {
            String dateStr = ord.getOrderDate() != null ? ord.getOrderDate().format(DATE_FORMATTER) : "N/A";
            modelOrderLogs.addRow(new Object[]{
                    ordStt++,
                    ord.getId(),
                    dateStr,
                    ord.getCustomerName() != null ? ord.getCustomerName() : "Khách Lẻ",
                    CURRENCY_FORMAT.format(ord.getTotalAmount()),
                    CURRENCY_FORMAT.format(ord.getDiscountAmount()),
                    CURRENCY_FORMAT.format(ord.getFinalAmount()),
                    ord.getPaymentMethod() != null ? ord.getPaymentMethod() : "TIỀN MẶT"
            });
        }
    }

    /**
     * Xử lý gửi Form tạo mã khuyến mãi mới.
     */
    private void submitCouponForm() {
        String code = txtCode.getText().trim().toUpperCase().replaceAll("\\s+", "");
        String title = txtTitle.getText().trim();
        boolean isPercent = cboType.getSelectedIndex() == 0;
        String valStr = txtValue.getText().trim().replace(",", "").replace(".", "");
        String minOrderStr = txtMinOrder.getText().trim().replace(",", "").replace(".", "");
        String maxDiscStr = txtMaxDiscount.getText().trim().replace(",", "").replace(".", "");
        String startStr = txtStartDate.getText().trim();
        String endStr = txtEndDate.getText().trim();
        String limitStr = txtLimit.getText().trim();

        if (code.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Vui lòng nhập Mã Voucher!", "Thiếu Dữ Liệu", JOptionPane.WARNING_MESSAGE);
            txtCode.requestFocus();
            return;
        }

        if (title.isEmpty()) {
            title = "Chương trình ưu đãi " + code;
        }

        double value, minOrder, maxDiscount = 0.0;
        int limit;
        LocalDate startDate, endDate;

        try {
            value = Double.parseDouble(valStr);
            if (value <= 0) throw new NumberFormatException();
            if (isPercent && value > 100) {
                JOptionPane.showMessageDialog(this, "Tỷ lệ giảm phần trăm không được vượt quá 100%!", "Lỗi Giá Trị", JOptionPane.WARNING_MESSAGE);
                txtValue.requestFocus();
                return;
            }

            minOrder = minOrderStr.isEmpty() ? 0.0 : Double.parseDouble(minOrderStr);
            if (isPercent) {
                maxDiscount = maxDiscStr.isEmpty() ? 0.0 : Double.parseDouble(maxDiscStr);
            }

            limit = Integer.parseInt(limitStr);
            if (limit <= 0) throw new NumberFormatException();

            startDate = LocalDate.parse(startStr);
            endDate = LocalDate.parse(endStr);
            if (endDate.isBefore(startDate)) {
                JOptionPane.showMessageDialog(this, "Ngày hết hạn không được trước ngày bắt đầu!", "Lỗi Thời Gian", JOptionPane.WARNING_MESSAGE);
                txtEndDate.requestFocus();
                return;
            }

        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this,
                    "Thông số nhập liệu hoặc định dạng ngày tháng (YYYY-MM-DD) không hợp lệ!\nVui lòng kiểm tra lại các trường số và ngày.",
                    "Lỗi Định Dạng", JOptionPane.ERROR_MESSAGE);
            return;
        }

        Coupon coupon = new Coupon();
        coupon.setCode(code);
        coupon.setTitle(title);
        coupon.setDiscountType(isPercent ? Coupon.TYPE_PERCENT : Coupon.TYPE_FIXED_AMOUNT);
        coupon.setDiscountValue(value);
        coupon.setMinOrderAmount(minOrder);
        coupon.setMaxDiscountAmount(maxDiscount);
        coupon.setStartDate(startDate);
        coupon.setEndDate(endDate);
        coupon.setUsageLimit(limit);
        coupon.setUsedCount(0);
        coupon.setStatus(Coupon.STATUS_ACTIVE);

        btnSaveCoupon.setEnabled(false);
        btnSaveCoupon.setText("Đang lưu...");

        SwingWorker<Boolean, Void> worker = new SwingWorker<>() {
            @Override
            protected Boolean doInBackground() {
                return couponService.createCoupon(coupon);
            }

            @Override
            protected void done() {
                try {
                    boolean ok = get();
                    if (ok) {
                        JOptionPane.showMessageDialog(PromotionManagementPanel.this,
                                "Tạo mới mã khuyến mãi [" + code + "] thành công!\nMã đã sẵn sàng áp dụng tại phân hệ Bán hàng & Cho thuê.",
                                "Thành Công", JOptionPane.INFORMATION_MESSAGE);
                        resetForm();
                        loadAllDataAsync();
                    } else {
                        JOptionPane.showMessageDialog(PromotionManagementPanel.this,
                                "Không thể tạo mã! Mã [" + code + "] có thể đã tồn tại trong hệ thống.",
                                "Trùng Mã Khuyến Mãi", JOptionPane.ERROR_MESSAGE);
                    }
                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(PromotionManagementPanel.this, "Lỗi: " + ex.getMessage(), "Lỗi Hệ Thống", JOptionPane.ERROR_MESSAGE);
                } finally {
                    btnSaveCoupon.setEnabled(true);
                    btnSaveCoupon.setText("+ 🚀 Lưu Mã Khuyến Mãi");
                }
            }
        };

        worker.execute();
    }

    /**
     * Bật / Tắt trạng thái mã được chọn trên bảng.
     */
    private void toggleSelectedCoupon() {
        int row = tblCoupons.getSelectedRow();
        if (row < 0) {
            JOptionPane.showMessageDialog(this, "Vui lòng chọn một mã coupon trên bảng để chuyển đổi trạng thái!", "Chưa Chọn Dòng", JOptionPane.WARNING_MESSAGE);
            return;
        }

        String code = (String) modelCoupons.getValueAt(row, 1);
        btnToggleStatus.setEnabled(false);

        SwingWorker<Boolean, Void> worker = new SwingWorker<>() {
            @Override
            protected Boolean doInBackground() {
                return couponService.toggleCouponStatus(code);
            }

            @Override
            protected void done() {
                try {
                    boolean ok = get();
                    if (ok) {
                        JOptionPane.showMessageDialog(PromotionManagementPanel.this,
                                "Đã chuyển đổi trạng thái mã [" + code + "] thành công!",
                                "Cập Nhật Thành Công", JOptionPane.INFORMATION_MESSAGE);
                        loadAllDataAsync();
                    } else {
                        JOptionPane.showMessageDialog(PromotionManagementPanel.this, "Không thể chuyển đổi trạng thái!", "Lỗi", JOptionPane.ERROR_MESSAGE);
                    }
                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(PromotionManagementPanel.this, "Lỗi: " + ex.getMessage(), "Lỗi", JOptionPane.ERROR_MESSAGE);
                } finally {
                    btnToggleStatus.setEnabled(true);
                }
            }
        };

        worker.execute();
    }

    /**
     * Xóa mã khuyến mãi được chọn.
     */
    private void deleteSelectedCoupon() {
        int row = tblCoupons.getSelectedRow();
        if (row < 0) {
            JOptionPane.showMessageDialog(this, "Vui lòng chọn một mã coupon trên bảng để xóa!", "Chưa Chọn Dòng", JOptionPane.WARNING_MESSAGE);
            return;
        }

        String code = (String) modelCoupons.getValueAt(row, 1);
        int confirm = JOptionPane.showConfirmDialog(this,
                "Bạn có chắc chắn muốn xóa vĩnh viễn mã khuyến mãi [" + code + "] khỏi cơ sở dữ liệu?",
                "Xác Nhận Xóa", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);

        if (confirm == JOptionPane.YES_OPTION) {
            btnDeleteCoupon.setEnabled(false);

            SwingWorker<Boolean, Void> worker = new SwingWorker<>() {
                @Override
                protected Boolean doInBackground() {
                    return couponService.deleteCoupon(code);
                }

                @Override
                protected void done() {
                    try {
                        boolean ok = get();
                        if (ok) {
                            JOptionPane.showMessageDialog(PromotionManagementPanel.this,
                                    "Đã xóa mã khuyến mãi [" + code + "] thành công!",
                                    "Đã Xóa", JOptionPane.INFORMATION_MESSAGE);
                            loadAllDataAsync();
                        } else {
                            JOptionPane.showMessageDialog(PromotionManagementPanel.this, "Không thể xóa mã khuyến mãi này!", "Lỗi", JOptionPane.ERROR_MESSAGE);
                        }
                    } catch (Exception ex) {
                        JOptionPane.showMessageDialog(PromotionManagementPanel.this, "Lỗi: " + ex.getMessage(), "Lỗi", JOptionPane.ERROR_MESSAGE);
                    } finally {
                        btnDeleteCoupon.setEnabled(true);
                    }
                }
            };

            worker.execute();
        }
    }

    private void resetForm() {
        txtCode.setText("");
        txtTitle.setText("");
        cboType.setSelectedIndex(0);
        txtValue.setText("10");
        txtMinOrder.setText("500000");
        txtMaxDiscount.setText("300000");
        txtStartDate.setText(LocalDate.now().format(ISO_FORMATTER));
        txtEndDate.setText(LocalDate.now().plusMonths(1).format(ISO_FORMATTER));
        txtLimit.setText("100");
        updateMaxDiscountState();
    }

    // =========================================================================
    // UI HELPERS & CUSTOM CELL RENDERERS
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
        header.setPreferredSize(new Dimension(0, 38));
        header.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, UIUtils.COLOR_BORDER));

        DefaultTableCellRenderer centerRenderer = new DefaultTableCellRenderer();
        centerRenderer.setHorizontalAlignment(SwingConstants.CENTER);
        table.getColumnModel().getColumn(0).setCellRenderer(centerRenderer);
    }

    /**
     * Renderer hiển thị Badge màu sắc cho trạng thái Voucher.
     */
    private static class CouponStatusBadgeRenderer extends DefaultTableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(JTable table, Object value,
                                                       boolean isSelected, boolean hasFocus, int row, int column) {
            JLabel label = (JLabel) super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
            label.setHorizontalAlignment(SwingConstants.CENTER);
            label.setFont(new Font("Segoe UI", Font.BOLD, 11));

            String status = value != null ? value.toString() : "";
            if (Coupon.STATUS_ACTIVE.equalsIgnoreCase(status)) {
                label.setText(" ĐANG HOẠT ĐỘNG ");
                label.setBackground(new Color(220, 252, 231)); // Green 100
                label.setForeground(new Color(21, 128, 61));   // Green 700
            } else if (Coupon.STATUS_EXPIRED.equalsIgnoreCase(status)) {
                label.setText(" ĐÃ HẾT HẠN ");
                label.setBackground(new Color(254, 243, 199)); // Amber 100
                label.setForeground(new Color(180, 83, 9));    // Amber 700
            } else if (Coupon.STATUS_DISABLED.equalsIgnoreCase(status)) {
                label.setText(" TẠM DỪNG ");
                label.setBackground(new Color(241, 245, 249)); // Slate 100
                label.setForeground(new Color(100, 116, 139));  // Slate 500
            } else {
                label.setText(" " + status + " ");
                label.setBackground(new Color(241, 245, 249));
                label.setForeground(new Color(71, 85, 105));
            }

            label.setOpaque(true);
            return label;
        }
    }

    /**
     * Renderer hiển thị Đánh giá hiệu quả voucher.
     */
    private static class PerformanceRatingCellRenderer extends DefaultTableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(JTable table, Object value,
                                                       boolean isSelected, boolean hasFocus, int row, int column) {
            JLabel label = (JLabel) super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
            label.setHorizontalAlignment(SwingConstants.CENTER);
            label.setFont(new Font("Segoe UI", Font.BOLD, 11));

            String rating = value != null ? value.toString() : "";
            if (rating.contains("RẤT CAO")) {
                label.setForeground(new Color(21, 128, 61));   // Green
            } else if (rating.contains("TIỀM NĂNG")) {
                label.setForeground(new Color(2, 132, 199));   // Sky
            } else if (rating.contains("TRUNG BÌNH")) {
                label.setForeground(new Color(217, 119, 6));   // Amber
            } else {
                label.setForeground(new Color(148, 163, 184)); // Slate
            }

            return label;
        }
    }
}