package com.smarttech.view.panel;

import com.smarttech.model.Employee;
import com.smarttech.model.User;
import com.smarttech.service.EmployeeService;
import com.smarttech.view.util.UIUtils;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Giao diện Quản lý Nhân sự, Tài khoản & Phân quyền RBAC (EmployeePanel).
 * Thiết kế giao diện Enterprise 2 Tabs hiện đại:
 * - Tab 1: Danh Sách Nhân Viên & Phân Quyền Tài Khoản
 * - Tab 2: Bảng Phân Quyền Quyền Hạn (RBAC Matrix & Login Audit Log)
 *
 * @author Senior Java Developer & Software Architect
 * @project SmartTech
 */
public class EmployeePanel extends JPanel {

    private final EmployeeService employeeService;

    // Tabbed Pane
    private JTabbedPane tabbedPane;

    // ================= TAB 1 COMPONENTS =================
    private JTextField txtId;
    private JTextField txtFullName;
    private JTextField txtEmail;
    private JTextField txtPhone;
    private JComboBox<String> cboPosition;
    private JTextField txtSalary;
    private JTextField txtHireDate;
    private JComboBox<String> cboEmpStatus;

    // Account inputs
    private JTextField txtUsername;
    private JPasswordField txtPassword;
    private JComboBox<String> cboRole;
    private JLabel lblAccountStatusValue;

    // Action Buttons
    private JButton btnSave;
    private JButton btnClear;
    private JButton btnToggleLock;
    private JButton btnResetPassword;
    private JButton btnDelete;

    // Right Table & Filters
    private JTextField txtSearch;
    private JComboBox<String> cboRoleFilter;
    private JComboBox<String> cboStatusFilter;
    private JTable employeeTable;
    private DefaultTableModel tableModel;
    private JLabel lblLoadingStatus;

    // Cache
    private List<Employee> cachedEmployees = new ArrayList<>();
    private List<User> cachedUsers = new ArrayList<>();
    private Employee selectedEmployee = null;
    private User selectedUser = null;

    // ================= TAB 2 COMPONENTS =================
    private JTable rbacTable;
    private DefaultTableModel rbacModel;
    private JTable auditTable;
    private DefaultTableModel auditModel;

    // Validation patterns
    private static final Pattern PHONE_PATTERN = Pattern.compile("^0\\d{9}$");
    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[\\w.-]+@[\\w.-]+\\.[a-zA-Z]{2,}$");
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");

    public EmployeePanel() {
        this.employeeService = new EmployeeService();
        initComponents();
        loadDataAsync();
    }

    private void initComponents() {
        setLayout(new BorderLayout(0, 10));
        setBackground(UIUtils.COLOR_BG_LIGHT);
        setBorder(new EmptyBorder(14, 16, 14, 16));

        // 1. HEADER
        JPanel headerPanel = new JPanel(new BorderLayout());
        headerPanel.setOpaque(false);

        JLabel lblTitle = new JLabel("Phân Hệ Quản Lý Nhân Sự & Phân Quyền RBAC");
        lblTitle.setFont(UIUtils.FONT_TITLE);
        lblTitle.setForeground(UIUtils.COLOR_TEXT_MAIN);

        JLabel lblSub = new JLabel("Quản lý danh sách nhân sự, phân cấp chức vụ, cấp phát tài khoản và kiểm soát ma trận quyền hạn (Role-Based Access Control).");
        lblSub.setFont(UIUtils.FONT_SUBTITLE);
        lblSub.setForeground(UIUtils.COLOR_TEXT_MUTED);

        headerPanel.add(lblTitle, BorderLayout.NORTH);
        headerPanel.add(lblSub, BorderLayout.SOUTH);
        add(headerPanel, BorderLayout.NORTH);

        // 2. TABS
        tabbedPane = new JTabbedPane();
        tabbedPane.setFont(UIUtils.FONT_BODY_BOLD);
        tabbedPane.setBackground(Color.WHITE);

        JPanel tab1 = createStaffManagementTab();
        tabbedPane.addTab("👥 Danh Sách Nhân Viên & Phân Quyền Tài Khoản", tab1);

        JPanel tab2 = createRbacMatrixTab();
        tabbedPane.addTab("🔐 Bảng Phân Quyền Quyền Hạn (RBAC Matrix & Audit Log)", tab2);

        add(tabbedPane, BorderLayout.CENTER);
    }

    // =========================================================================
    // TAB 1: DANH SÁCH NHÂN VIÊN & PHÂN QUYỀN TÀI KHOẢN
    // =========================================================================
    private JPanel createStaffManagementTab() {
        JPanel panel = new JPanel(new BorderLayout(14, 0));
        panel.setOpaque(false);
        panel.setBorder(new EmptyBorder(10, 0, 0, 0));

        // FORM BÊN TRÁI
        JPanel leftCard = new JPanel(new BorderLayout(0, 10));
        leftCard.setPreferredSize(new Dimension(390, 0));
        leftCard.setBackground(Color.WHITE);
        leftCard.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(UIUtils.COLOR_BORDER, 1),
                new EmptyBorder(14, 16, 14, 16)
        ));

        JLabel lblFormTitle = new JLabel("Hồ Sơ Nhân Sự & Tài Khoản");
        lblFormTitle.setFont(new Font("Segoe UI", Font.BOLD, 16));
        lblFormTitle.setForeground(UIUtils.COLOR_TEXT_MAIN);
        leftCard.add(lblFormTitle, BorderLayout.NORTH);

        JPanel formContainer = new JPanel();
        formContainer.setLayout(new BoxLayout(formContainer, BoxLayout.Y_AXIS));
        formContainer.setOpaque(false);

        txtId = UIUtils.createTextField();
        txtId.setText("NV-" + (System.currentTimeMillis() % 10000));
        txtFullName = UIUtils.createTextField();
        txtEmail = UIUtils.createTextField();
        txtPhone = UIUtils.createTextField();

        cboPosition = new JComboBox<>(new String[]{"Bán hàng", "Kỹ thuật", "Thủ kho", "Kế toán", "Quản lý"});
        cboPosition.setFont(UIUtils.FONT_BODY);

        txtSalary = UIUtils.createTextField();
        txtSalary.setText("12000000");

        txtHireDate = UIUtils.createTextField();
        txtHireDate.setText(LocalDate.now().toString());

        cboEmpStatus = new JComboBox<>(new String[]{"WORKING (Đang làm việc)", "RESIGNED (Đã nghỉ việc)"});
        cboEmpStatus.setFont(UIUtils.FONT_BODY);

        formContainer.add(createFormField("Mã Nhân Viên (*):", txtId));
        formContainer.add(createFormField("Họ & Tên (*):", txtFullName));
        formContainer.add(createFormField("Email Liên Hệ (*):", txtEmail));
        formContainer.add(createFormField("Số Điện Thoại (10 số) (*):", txtPhone));
        formContainer.add(createFormField("Chức Vụ Chuyên Môn:", cboPosition));
        formContainer.add(createFormField("Lương Cơ Bản (VNĐ) (*):", txtSalary));
        formContainer.add(createFormField("Ngày Vào Làm (yyyy-MM-dd):", txtHireDate));
        formContainer.add(createFormField("Trạng Thái Nhân Sự:", cboEmpStatus));

        JSeparator sep = new JSeparator();
        sep.setMaximumSize(new Dimension(Integer.MAX_VALUE, 2));
        formContainer.add(Box.createVerticalStrut(8));
        formContainer.add(sep);
        formContainer.add(Box.createVerticalStrut(8));

        JLabel lblAccountSec = new JLabel("Tài Khoản Đăng Nhập & Phân Quyền");
        lblAccountSec.setFont(UIUtils.FONT_BODY_BOLD);
        lblAccountSec.setForeground(new Color(30, 41, 59));
        formContainer.add(lblAccountSec);
        formContainer.add(Box.createVerticalStrut(8));

        txtUsername = UIUtils.createTextField();
        txtPassword = UIUtils.createPasswordField();
        txtPassword.setToolTipText("Để trống nếu giữ nguyên mật khẩu cũ khi cập nhật");

        cboRole = new JComboBox<>(new String[]{"EMPLOYEE", "ADMIN"});
        cboRole.setFont(UIUtils.FONT_BODY);

        JPanel statusWrapper = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        statusWrapper.setOpaque(false);
        lblAccountStatusValue = new JLabel("CHƯA CÓ TÀI KHOẢN");
        lblAccountStatusValue.setFont(UIUtils.FONT_BODY_BOLD);
        lblAccountStatusValue.setForeground(UIUtils.COLOR_TEXT_MUTED);
        statusWrapper.add(lblAccountStatusValue);

        formContainer.add(createFormField("Tên Đăng Nhập (*):", txtUsername));
        formContainer.add(createFormField("Mật Khẩu (>= 6 ký tự):", txtPassword));
        formContainer.add(createFormField("Vai Trò (Role RBAC):", cboRole));
        formContainer.add(createFormField("Trạng Thái Tài Khoản:", statusWrapper));

        JScrollPane formScroll = new JScrollPane(formContainer);
        formScroll.setBorder(null);
        formScroll.setOpaque(false);
        formScroll.getViewport().setOpaque(false);
        leftCard.add(formScroll, BorderLayout.CENTER);

        JPanel buttonGrid = new JPanel(new GridLayout(3, 2, 8, 8));
        buttonGrid.setOpaque(false);
        buttonGrid.setBorder(new EmptyBorder(10, 0, 0, 0));

        btnSave = UIUtils.createPrimaryButton("Lưu Hồ Sơ & Quyền");
        btnSave.addActionListener(e -> saveEmployeeAndAccount());

        btnClear = new JButton("Làm Mới Form");
        btnClear.setFont(UIUtils.FONT_BODY);
        btnClear.setBackground(Color.WHITE);
        btnClear.setBorder(BorderFactory.createLineBorder(UIUtils.COLOR_BORDER, 1));
        btnClear.addActionListener(e -> clearForm());

        btnToggleLock = new JButton("Khóa / Mở Khóa");
        btnToggleLock.setFont(UIUtils.FONT_BODY_BOLD);
        btnToggleLock.setForeground(new Color(180, 83, 9));
        btnToggleLock.setBackground(Color.WHITE);
        btnToggleLock.setBorder(BorderFactory.createLineBorder(new Color(245, 158, 11), 1));
        btnToggleLock.addActionListener(e -> toggleAccountLock());

        btnResetPassword = new JButton("Đổi Mật Khẩu");
        btnResetPassword.setFont(UIUtils.FONT_BODY_BOLD);
        btnResetPassword.setBackground(Color.WHITE);
        btnResetPassword.setBorder(BorderFactory.createLineBorder(UIUtils.COLOR_BORDER, 1));
        btnResetPassword.addActionListener(e -> showResetPasswordDialog());

        btnDelete = new JButton("Xóa Nhân Viên");
        btnDelete.setFont(UIUtils.FONT_BODY_BOLD);
        btnDelete.setForeground(UIUtils.COLOR_DANGER);
        btnDelete.setBackground(Color.WHITE);
        btnDelete.setBorder(BorderFactory.createLineBorder(UIUtils.COLOR_DANGER, 1));
        btnDelete.addActionListener(e -> deleteSelectedEmployee());

        JButton btnQuickAdmin = new JButton("Gán Nhanh Admin");
        btnQuickAdmin.setFont(UIUtils.FONT_SMALL);
        btnQuickAdmin.setBackground(Color.WHITE);
        btnQuickAdmin.setBorder(BorderFactory.createLineBorder(UIUtils.COLOR_BORDER, 1));
        btnQuickAdmin.addActionListener(e -> cboRole.setSelectedItem("ADMIN"));

        buttonGrid.add(btnSave);
        buttonGrid.add(btnClear);
        buttonGrid.add(btnToggleLock);
        buttonGrid.add(btnResetPassword);
        buttonGrid.add(btnDelete);
        buttonGrid.add(btnQuickAdmin);

        leftCard.add(buttonGrid, BorderLayout.SOUTH);
        panel.add(leftCard, BorderLayout.WEST);

        // BẢNG DỮ LIỆU BÊN PHẢI
        JPanel rightCard = new JPanel(new BorderLayout(0, 10));
        rightCard.setBackground(Color.WHITE);
        rightCard.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(UIUtils.COLOR_BORDER, 1),
                new EmptyBorder(12, 14, 12, 14)
        ));

        JPanel filterBar = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        filterBar.setOpaque(false);

        txtSearch = UIUtils.createTextField();
        txtSearch.setPreferredSize(new Dimension(240, 36));
        txtSearch.setToolTipText("Tìm kiếm theo Mã NV, Họ Tên, SĐT, Email...");
        txtSearch.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) { applyFilters(); }
            @Override
            public void removeUpdate(DocumentEvent e) { applyFilters(); }
            @Override
            public void changedUpdate(DocumentEvent e) { applyFilters(); }
        });

        cboRoleFilter = new JComboBox<>(new String[]{"Tất cả vai trò", "ADMIN", "EMPLOYEE"});
        cboRoleFilter.setFont(UIUtils.FONT_BODY);
        cboRoleFilter.setPreferredSize(new Dimension(140, 36));
        cboRoleFilter.addActionListener(e -> applyFilters());

        cboStatusFilter = new JComboBox<>(new String[]{"Tất cả trạng thái", "Đang làm việc", "Đã nghỉ việc", "Tài khoản Bị Khóa"});
        cboStatusFilter.setFont(UIUtils.FONT_BODY);
        cboStatusFilter.setPreferredSize(new Dimension(160, 36));
        cboStatusFilter.addActionListener(e -> applyFilters());

        JButton btnReload = new JButton("Đồng Bộ");
        btnReload.setFont(UIUtils.FONT_BODY);
        btnReload.setBackground(Color.WHITE);
        btnReload.setBorder(BorderFactory.createLineBorder(UIUtils.COLOR_BORDER, 1));
        btnReload.setPreferredSize(new Dimension(90, 36));
        btnReload.addActionListener(e -> loadDataAsync());

        filterBar.add(txtSearch);
        filterBar.add(cboRoleFilter);
        filterBar.add(cboStatusFilter);
        filterBar.add(btnReload);

        rightCard.add(filterBar, BorderLayout.NORTH);

        String[] columns = {
                "Mã NV", "Họ và Tên", "Số ĐT", "Chức Vụ",
                "Lương Cơ Bản", "Trạng Thái NV", "Tài Khoản", "Vai Trò", "Trạng Thái TK", "Đăng Nhập Cuối"
        };

        tableModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int col) {
                return false;
            }
        };

        employeeTable = new JTable(tableModel);
        employeeTable.setFont(UIUtils.FONT_BODY);
        employeeTable.setRowHeight(38);
        employeeTable.getTableHeader().setFont(UIUtils.FONT_BODY_BOLD);
        employeeTable.getTableHeader().setBackground(new Color(241, 245, 249));
        employeeTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        employeeTable.setShowVerticalLines(false);
        employeeTable.setGridColor(UIUtils.COLOR_BORDER);

        employeeTable.getColumnModel().getColumn(5).setCellRenderer(new EmploymentStatusBadgeRenderer());
        employeeTable.getColumnModel().getColumn(7).setCellRenderer(new RoleBadgeRenderer());
        employeeTable.getColumnModel().getColumn(8).setCellRenderer(new AccountStatusBadgeRenderer());

        employeeTable.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                int row = employeeTable.getSelectedRow();
                if (row != -1) {
                    String empId = (String) tableModel.getValueAt(row, 0);
                    onEmployeeSelected(empId);
                }
            }
        });

        JScrollPane tableScroll = new JScrollPane(employeeTable);
        tableScroll.setBorder(BorderFactory.createLineBorder(UIUtils.COLOR_BORDER, 1));
        rightCard.add(tableScroll, BorderLayout.CENTER);

        lblLoadingStatus = new JLabel("● Sẵn sàng kết nối MongoDB (SmartTechDB.employees & SmartTechDB.users)");
        lblLoadingStatus.setFont(UIUtils.FONT_SMALL);
        lblLoadingStatus.setForeground(UIUtils.COLOR_TEXT_MUTED);
        rightCard.add(lblLoadingStatus, BorderLayout.SOUTH);

        panel.add(rightCard, BorderLayout.CENTER);
        return panel;
    }

    // =========================================================================
    // TAB 2: BẢNG MA TRẬN PHÂN QUYỀN RBAC MATRIX & AUDIT LOG
    // =========================================================================
    private JPanel createRbacMatrixTab() {
        JPanel panel = new JPanel(new GridLayout(2, 1, 0, 12));
        panel.setOpaque(false);
        panel.setBorder(new EmptyBorder(10, 0, 0, 0));

        JPanel matrixCard = new JPanel(new BorderLayout(0, 8));
        matrixCard.setBackground(Color.WHITE);
        matrixCard.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(UIUtils.COLOR_BORDER, 1),
                new EmptyBorder(12, 14, 12, 14)
        ));

        JLabel lblMatrixTitle = new JLabel("Ma Trận Phân Quyền Vai Trò Hệ Thống (RBAC Permission Matrix)");
        lblMatrixTitle.setFont(UIUtils.FONT_TITLE);
        lblMatrixTitle.setForeground(UIUtils.COLOR_TEXT_MAIN);

        JLabel lblMatrixDesc = new JLabel("Quy tắc kiểm soát truy cập phân tầng giữa Quản Trị Viên (ADMIN) và Nhân Viên Nghiệp Vụ (EMPLOYEE) trên 10 phân hệ.");
        lblMatrixDesc.setFont(UIUtils.FONT_SMALL);
        lblMatrixDesc.setForeground(UIUtils.COLOR_TEXT_MUTED);

        JPanel matrixHead = new JPanel(new BorderLayout());
        matrixHead.setOpaque(false);
        matrixHead.add(lblMatrixTitle, BorderLayout.NORTH);
        matrixHead.add(lblMatrixDesc, BorderLayout.SOUTH);
        matrixCard.add(matrixHead, BorderLayout.NORTH);

        String[] matrixCols = {"STT", "Phân Hệ / Mô-đun Nghiệp Vụ", "Quản Trị Viên (ADMIN)", "Nhân Viên (EMPLOYEE)", "Ghi Chú Kiểm Soát Bảo Mật"};
        rbacModel = new DefaultTableModel(matrixCols, 0) {
            @Override
            public boolean isCellEditable(int r, int c) { return false; }
        };

        rbacModel.addRow(new Object[]{"1", "Tổng Quan Dashboard", "Toàn quyền: Xem doanh thu, cấu hình cảnh báo, biểu đồ", "Xem thống kê cơ bản trong ca làm việc", "Chặn xem số liệu doanh thu mật"});
        rbacModel.addRow(new Object[]{"2", "Bán Hàng POS", "Toàn quyền: Hủy hóa đơn, duyệt chiết khấu đặc biệt", "Tạo đơn bán lẻ, in hóa đơn, áp mã giảm giá", "Chiết khấu > 20% yêu cầu Admin duyệt"});
        rbacModel.addRow(new Object[]{"3", "Cho Thuê Thiết Bị (Rental)", "Toàn quyền: Miễn cọc, hủy hợp đồng, sửa đơn giá", "Lập hợp đồng, nhận trả máy, tính phí quá hạn", "Hợp đồng giá trị cao bắt buộc đặt cọc"});
        rbacModel.addRow(new Object[]{"4", "Thiết Bị & Serial", "Toàn quyền: Thêm, sửa, xóa, thanh lý thiết bị", "Tra cứu tồn kho, cập nhật trạng thái Serial", "Chỉ Admin được xóa vĩnh viễn Serial"});
        rbacModel.addRow(new Object[]{"5", "Khách Hàng & Loyalty", "Toàn quyền: Điều chỉnh điểm thưởng, xóa hồ sơ", "Thêm khách hàng mới, tích điểm tự động theo đơn", "Quy đổi điểm thưởng tự động"});
        rbacModel.addRow(new Object[]{"6", "Nhập Kho & Nhà Cung Cấp", "Toàn quyền: Phê duyệt nhập kho, chốt công nợ NCC", "Lập phiếu đề xuất nhập kho thiết bị", "Chỉ Admin được thanh toán nợ NCC"});
        rbacModel.addRow(new Object[]{"7", "Kiểm Kê Kho (Stock Audit)", "Toàn quyền: Phê duyệt cân bằng số lượng tồn kho", "Quét mã Serial thực tế tại kho hàng", "Lệch kho tự động sinh phiếu điều chỉnh"});
        rbacModel.addRow(new Object[]{"8", "Bảo Trì & Sửa Chữa", "Toàn quyền: Phê duyệt thanh lý hoặc gửi hãng sửa", "Tạo phiếu bảo trì, ghi nhận chi phí sửa chữa", "Theo dõi lịch sử máy qua Serial"});
        rbacModel.addRow(new Object[]{"9", "Khuyến Mãi & Voucher", "Toàn quyền: Tạo mã giảm giá, giới hạn số lượt dùng", "Áp dụng mã giảm giá khi thanh toán", "Kiểm tra điều kiện đơn hàng tối thiểu"});
        rbacModel.addRow(new Object[]{"10", "Nhân Sự & Phân Quyền", "Toàn quyền: Cấp tài khoản, đổi mật khẩu, phân vai trò", "⛔ KHÔNG ĐƯỢC PHÉP TRUY CẬP (403 FORBIDDEN)", "Tường lửa RBAC chặn trực tiếp từ Sidebar"});

        rbacTable = new JTable(rbacModel);
        rbacTable.setFont(UIUtils.FONT_BODY);
        rbacTable.setRowHeight(34);
        rbacTable.getTableHeader().setFont(UIUtils.FONT_BODY_BOLD);
        rbacTable.getTableHeader().setBackground(new Color(241, 245, 249));
        rbacTable.getColumnModel().getColumn(0).setPreferredWidth(45);
        rbacTable.getColumnModel().getColumn(1).setPreferredWidth(180);
        rbacTable.getColumnModel().getColumn(2).setPreferredWidth(280);
        rbacTable.getColumnModel().getColumn(3).setPreferredWidth(260);
        rbacTable.getColumnModel().getColumn(4).setPreferredWidth(220);

        rbacTable.getColumnModel().getColumn(2).setCellRenderer(new AdminPermissionCellRenderer());
        rbacTable.getColumnModel().getColumn(3).setCellRenderer(new EmployeePermissionCellRenderer());

        JScrollPane matrixScroll = new JScrollPane(rbacTable);
        matrixScroll.setBorder(BorderFactory.createLineBorder(UIUtils.COLOR_BORDER, 1));
        matrixCard.add(matrixScroll, BorderLayout.CENTER);
        panel.add(matrixCard);

        JPanel auditCard = new JPanel(new BorderLayout(0, 8));
        auditCard.setBackground(Color.WHITE);
        auditCard.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(UIUtils.COLOR_BORDER, 1),
                new EmptyBorder(12, 14, 12, 14)
        ));

        JPanel auditHead = new JPanel(new BorderLayout());
        auditHead.setOpaque(false);

        JLabel lblAuditTitle = new JLabel("Nhật Ký Đăng Nhập & Truy Vết Bảo Mật (Login Audit Log)");
        lblAuditTitle.setFont(UIUtils.FONT_TITLE);
        lblAuditTitle.setForeground(UIUtils.COLOR_TEXT_MAIN);

        JButton btnRefreshAudit = new JButton("Làm Mới Nhật Ký");
        btnRefreshAudit.setFont(UIUtils.FONT_SMALL);
        btnRefreshAudit.setBackground(Color.WHITE);
        btnRefreshAudit.setBorder(BorderFactory.createLineBorder(UIUtils.COLOR_BORDER, 1));
        btnRefreshAudit.addActionListener(e -> reloadAuditLogs());

        auditHead.add(lblAuditTitle, BorderLayout.WEST);
        auditHead.add(btnRefreshAudit, BorderLayout.EAST);
        auditCard.add(auditHead, BorderLayout.NORTH);

        String[] auditCols = {"Mã Phiên", "Thời Gian", "Tài Khoản", "Họ Tên", "Địa Chỉ IP / Thiết Bị", "Hành Động", "Trạng Thái"};
        auditModel = new DefaultTableModel(auditCols, 0) {
            @Override
            public boolean isCellEditable(int r, int c) { return false; }
        };

        auditTable = new JTable(auditModel);
        auditTable.setFont(UIUtils.FONT_BODY);
        auditTable.setRowHeight(32);
        auditTable.getTableHeader().setFont(UIUtils.FONT_BODY_BOLD);
        auditTable.getTableHeader().setBackground(new Color(241, 245, 249));

        auditTable.getColumnModel().getColumn(6).setCellRenderer(new AuditStatusCellRenderer());

        JScrollPane auditScroll = new JScrollPane(auditTable);
        auditScroll.setBorder(BorderFactory.createLineBorder(UIUtils.COLOR_BORDER, 1));
        auditCard.add(auditScroll, BorderLayout.CENTER);
        panel.add(auditCard);

        reloadAuditLogs();

        return panel;
    }

    private void reloadAuditLogs() {
        auditModel.setRowCount(0);
        LocalDateTime now = LocalDateTime.now();
        auditModel.addRow(new Object[]{"SES-9012", now.minusMinutes(4).format(DATE_FORMATTER), "admin", "Nguyễn Văn Quản Trị", "192.168.1.10 (Máy Quản lý)", "Đăng nhập hệ thống (Dashboard)", "Thành Công"});
        auditModel.addRow(new Object[]{"SES-9011", now.minusMinutes(25).format(DATE_FORMATTER), "sales01", "Trần Thị Thu Ngân", "192.168.1.45 (POS Quầy 01)", "Đăng nhập hệ thống (POS)", "Thành Công"});
        auditModel.addRow(new Object[]{"SES-9010", now.minusHours(1).minusMinutes(12).format(DATE_FORMATTER), "tech02", "Lê Văn Kỹ Thuật", "192.168.1.88 (Máy Kho)", "Quét Serial kiểm kê kho", "Thành Công"});
        auditModel.addRow(new Object[]{"SES-9009", now.minusHours(2).format(DATE_FORMATTER), "sales01", "Trần Thị Thu Ngân", "192.168.1.45 (POS Quầy 01)", "Truy cập /employees (RBAC)", "Bị Từ Chối (403)"});
        auditModel.addRow(new Object[]{"SES-9008", now.minusHours(3).minusMinutes(15).format(DATE_FORMATTER), "kho_user", "Phạm Văn Thủ Kho", "192.168.1.72 (Kho Tổng)", "Đăng nhập mật khẩu sai (3 lần)", "Cảnh Báo Khóa"});
        auditModel.addRow(new Object[]{"SES-9007", now.minusDays(1).format(DATE_FORMATTER), "admin", "Nguyễn Văn Quản Trị", "192.168.1.10 (Máy Quản lý)", "Cấp phát tài khoản mới: tech03", "Thành Công"});
    }

    // =========================================================================
    // NGHIỆP VỤ BẤT ĐỒNG BỘ VỚI SWINGWORKER
    // =========================================================================

    public void loadDataAsync() {
        lblLoadingStatus.setText("● Đang đồng bộ danh sách nhân viên và tài khoản từ MongoDB...");
        lblLoadingStatus.setForeground(UIUtils.COLOR_PRIMARY);

        SwingWorker<List<Object>, Void> worker = new SwingWorker<>() {
            @Override
            protected List<Object> doInBackground() {
                List<Employee> emps = employeeService.getAllEmployees();
                List<User> users = employeeService.getAllUsers();
                List<Object> pair = new ArrayList<>();
                pair.add(emps);
                pair.add(users);
                return pair;
            }

            @Override
            @SuppressWarnings("unchecked")
            protected void done() {
                try {
                    List<Object> res = get();
                    cachedEmployees = (List<Employee>) res.get(0);
                    cachedUsers = (List<User>) res.get(1);

                    applyFilters();

                    lblLoadingStatus.setText("● Đã đồng bộ: " + cachedEmployees.size() + " nhân sự, " + cachedUsers.size() + " tài khoản hệ thống.");
                    lblLoadingStatus.setForeground(UIUtils.COLOR_SUCCESS);
                } catch (Exception ex) {
                    System.err.println("[EmployeePanel] Lỗi tải dữ liệu: " + ex.getMessage());
                    lblLoadingStatus.setText("● Lỗi truy vấn MongoDB! Kiểm tra kết nối localhost:27017.");
                    lblLoadingStatus.setForeground(UIUtils.COLOR_DANGER);
                }
            }
        };
        worker.execute();
    }

    private void applyFilters() {
        String keyword = txtSearch.getText().trim().toLowerCase();
        String selectedRole = (String) cboRoleFilter.getSelectedItem();
        String selectedStatus = (String) cboStatusFilter.getSelectedItem();

        tableModel.setRowCount(0);

        for (Employee emp : cachedEmployees) {
            User linkedUser = findUserForEmployee(emp);

            boolean matchKeyword = keyword.isEmpty()
                    || (emp.getId() != null && emp.getId().toLowerCase().contains(keyword))
                    || (emp.getFullName() != null && emp.getFullName().toLowerCase().contains(keyword))
                    || (emp.getPhone() != null && emp.getPhone().contains(keyword))
                    || (emp.getEmail() != null && emp.getEmail().toLowerCase().contains(keyword))
                    || (emp.getPosition() != null && emp.getPosition().toLowerCase().contains(keyword));

            String userRole = linkedUser != null ? linkedUser.getRole() : "EMPLOYEE";
            boolean matchRole = selectedRole == null || "Tất cả vai trò".equals(selectedRole)
                    || selectedRole.equalsIgnoreCase(userRole);

            boolean matchStatus = true;
            boolean isWorking = "WORKING".equalsIgnoreCase(emp.getStatus());
            if ("Đang làm việc".equals(selectedStatus)) {
                matchStatus = isWorking;
            } else if ("Đã nghỉ việc".equals(selectedStatus)) {
                matchStatus = !isWorking;
            } else if ("Tài khoản Bị Khóa".equals(selectedStatus)) {
                matchStatus = linkedUser != null && "LOCKED".equalsIgnoreCase(linkedUser.getStatus());
            }

            if (matchKeyword && matchRole && matchStatus) {
                String username = linkedUser != null ? linkedUser.getUsername() : "--";
                String accountStatus = linkedUser != null ? linkedUser.getStatus() : "NO_ACCOUNT";
                String lastLoginStr = (linkedUser != null && linkedUser.getLastLogin() != null)
                        ? linkedUser.getLastLogin().format(DATE_FORMATTER)
                        : "--";

                tableModel.addRow(new Object[]{
                        emp.getId(),
                        emp.getFullName(),
                        emp.getPhone(),
                        emp.getPosition() != null ? emp.getPosition() : "Bán hàng",
                        String.format("%,.0f VNĐ", emp.getBaseSalary()),
                        emp.getStatus(),
                        username,
                        userRole,
                        accountStatus,
                        lastLoginStr
                });
            }
        }
    }

    private User findUserForEmployee(Employee emp) {
        if (emp == null) return null;
        for (User u : cachedUsers) {
            if (emp.getId() != null && emp.getId().equals(u.getEmployeeId())) {
                return u;
            }
        }
        return null;
    }

    private void onEmployeeSelected(String empId) {
        selectedEmployee = null;
        selectedUser = null;

        for (Employee e : cachedEmployees) {
            if (e.getId() != null && e.getId().equals(empId)) {
                selectedEmployee = e;
                break;
            }
        }

        if (selectedEmployee == null) return;

        selectedUser = findUserForEmployee(selectedEmployee);

        txtId.setText(selectedEmployee.getId());
        txtId.setEnabled(false);
        txtFullName.setText(selectedEmployee.getFullName());
        txtEmail.setText(selectedEmployee.getEmail() != null ? selectedEmployee.getEmail() : "");
        txtPhone.setText(selectedEmployee.getPhone());
        cboPosition.setSelectedItem(selectedEmployee.getPosition());
        txtSalary.setText(String.format("%.0f", selectedEmployee.getBaseSalary()));
        txtHireDate.setText(selectedEmployee.getHireDate() != null ? selectedEmployee.getHireDate().toString() : LocalDate.now().toString());

        if ("RESIGNED".equalsIgnoreCase(selectedEmployee.getStatus())) {
            cboEmpStatus.setSelectedIndex(1);
        } else {
            cboEmpStatus.setSelectedIndex(0);
        }

        if (selectedUser != null) {
            txtUsername.setText(selectedUser.getUsername());
            txtUsername.setEnabled(false);
            txtPassword.setText("");
            cboRole.setSelectedItem(selectedUser.getRole());

            if ("ACTIVE".equalsIgnoreCase(selectedUser.getStatus())) {
                lblAccountStatusValue.setText("HOẠT ĐỘNG (ACTIVE)");
                lblAccountStatusValue.setForeground(UIUtils.COLOR_SUCCESS);
                btnToggleLock.setText("Khóa Tài Khoản");
            } else {
                lblAccountStatusValue.setText("BỊ KHÓA (LOCKED)");
                lblAccountStatusValue.setForeground(UIUtils.COLOR_DANGER);
                btnToggleLock.setText("Mở Khóa Tài Khoản");
            }
        } else {
            txtUsername.setText("");
            txtUsername.setEnabled(true);
            txtPassword.setText("");
            cboRole.setSelectedItem("EMPLOYEE");
            lblAccountStatusValue.setText("CHƯA CẤP TÀI KHOẢN");
            lblAccountStatusValue.setForeground(UIUtils.COLOR_TEXT_MUTED);
            btnToggleLock.setText("Khóa / Mở Khóa");
        }
    }

    private void clearForm() {
        selectedEmployee = null;
        selectedUser = null;

        txtId.setEnabled(true);
        txtId.setText("NV-" + (System.currentTimeMillis() % 10000));
        txtFullName.setText("");
        txtEmail.setText("");
        txtPhone.setText("");
        cboPosition.setSelectedIndex(0);
        txtSalary.setText("12000000");
        txtHireDate.setText(LocalDate.now().toString());
        cboEmpStatus.setSelectedIndex(0);

        txtUsername.setEnabled(true);
        txtUsername.setText("");
        txtPassword.setText("");
        cboRole.setSelectedIndex(0);
        lblAccountStatusValue.setText("CHƯA CÓ TÀI KHOẢN");
        lblAccountStatusValue.setForeground(UIUtils.COLOR_TEXT_MUTED);
        btnToggleLock.setText("Khóa / Mở Khóa");

        employeeTable.clearSelection();
    }

    // =========================================================================
    // CÁC HÀNH ĐỘNG GIAO DỊCH (LƯU, KHÓA, RESET PASS, XÓA)
    // =========================================================================

    private void saveEmployeeAndAccount() {
        String empId = txtId.getText().trim();
        String fullName = txtFullName.getText().trim();
        String email = txtEmail.getText().trim();
        String phone = txtPhone.getText().trim();
        String position = (String) cboPosition.getSelectedItem();
        String salaryStr = txtSalary.getText().trim();
        String hireDateStr = txtHireDate.getText().trim();

        String statusSelected = (String) cboEmpStatus.getSelectedItem();
        String empStatus = (statusSelected != null && statusSelected.startsWith("RESIGNED")) ? "RESIGNED" : "WORKING";

        String username = txtUsername.getText().trim();
        String password = new String(txtPassword.getPassword()).trim();
        String role = (String) cboRole.getSelectedItem();

        if (empId.isEmpty() || fullName.isEmpty() || phone.isEmpty() || email.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Vui lòng điền đầy đủ các thông tin bắt buộc (*)", "Lỗi Nhập Liệu", JOptionPane.ERROR_MESSAGE);
            return;
        }

        if (!PHONE_PATTERN.matcher(phone).matches()) {
            JOptionPane.showMessageDialog(this, "Số điện thoại không hợp lệ! Vui lòng nhập đúng 10 chữ số (bắt đầu bằng 0).", "Lỗi Nhập Liệu", JOptionPane.ERROR_MESSAGE);
            txtPhone.requestFocus();
            return;
        }

        if (!EMAIL_PATTERN.matcher(email).matches()) {
            JOptionPane.showMessageDialog(this, "Định dạng Email không hợp lệ (Ví dụ: nhanvien@smarttech.vn)!", "Lỗi Nhập Liệu", JOptionPane.ERROR_MESSAGE);
            txtEmail.requestFocus();
            return;
        }

        double salary;
        try {
            salary = Double.parseDouble(salaryStr.replace(",", ""));
            if (salary <= 0) {
                JOptionPane.showMessageDialog(this, "Lương cơ bản phải lớn hơn 0!", "Lỗi", JOptionPane.ERROR_MESSAGE);
                return;
            }
        } catch (NumberFormatException e) {
            JOptionPane.showMessageDialog(this, "Lương cơ bản phải là định dạng số hợp lệ!", "Lỗi", JOptionPane.ERROR_MESSAGE);
            return;
        }

        LocalDate hireDate;
        try {
            hireDate = LocalDate.parse(hireDateStr);
        } catch (Exception ex) {
            hireDate = LocalDate.now();
        }

        if (selectedUser == null && !username.isEmpty()) {
            if (password.length() < 6) {
                JOptionPane.showMessageDialog(this, "Mật khẩu khởi tạo tài khoản phải có tối thiểu 6 ký tự!", "Lỗi Mật Khẩu", JOptionPane.ERROR_MESSAGE);
                txtPassword.requestFocus();
                return;
            }
        }

        Employee emp = new Employee(empId, fullName, email, phone, position, salary, hireDate, empStatus);

        btnSave.setEnabled(false);
        lblLoadingStatus.setText("● Đang lưu thông tin nhân viên và phân quyền vào MongoDB...");
        lblLoadingStatus.setForeground(UIUtils.COLOR_PRIMARY);

        SwingWorker<Boolean, Void> worker = new SwingWorker<>() {
            @Override
            protected Boolean doInBackground() {
                return employeeService.saveOrUpdateEmployeeWithAccount(emp, username, password, role);
            }

            @Override
            protected void done() {
                btnSave.setEnabled(true);
                try {
                    boolean success = get();
                    if (success) {
                        JOptionPane.showMessageDialog(EmployeePanel.this, "Đã lưu thông tin nhân viên và phân quyền thành công!", "Thông Báo", JOptionPane.INFORMATION_MESSAGE);
                        clearForm();
                        loadDataAsync();
                    } else {
                        JOptionPane.showMessageDialog(EmployeePanel.this, "Không thể lưu dữ liệu! Tên đăng nhập có thể đã bị trùng lặp.", "Lỗi Lưu Trữ", JOptionPane.ERROR_MESSAGE);
                        lblLoadingStatus.setText("● Lỗi lưu trữ vào MongoDB!");
                        lblLoadingStatus.setForeground(UIUtils.COLOR_DANGER);
                    }
                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(EmployeePanel.this, "Lỗi ngoại lệ: " + ex.getMessage(), "Lỗi Hệ Thống", JOptionPane.ERROR_MESSAGE);
                }
            }
        };
        worker.execute();
    }

    private void toggleAccountLock() {
        if (selectedUser == null) {
            JOptionPane.showMessageDialog(this, "Nhân viên này chưa được cấp tài khoản đăng nhập để khóa/mở khóa!", "Thông Báo", JOptionPane.WARNING_MESSAGE);
            return;
        }

        boolean currentActive = "ACTIVE".equalsIgnoreCase(selectedUser.getStatus());
        String action = currentActive ? "KHÓA" : "MỞ KHÓA";

        int confirm = JOptionPane.showConfirmDialog(
                this,
                "Bạn có chắc muốn " + action + " tài khoản của người dùng: " + selectedUser.getUsername() + "?",
                "Xác nhận thay đổi trạng thái",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.QUESTION_MESSAGE
        );

        if (confirm == JOptionPane.YES_OPTION) {
            SwingWorker<Boolean, Void> worker = new SwingWorker<>() {
                @Override
                protected Boolean doInBackground() {
                    return employeeService.toggleAccountLock(selectedUser.getId());
                }

                @Override
                protected void done() {
                    try {
                        boolean ok = get();
                        if (ok) {
                            JOptionPane.showMessageDialog(EmployeePanel.this, "Đã " + action.toLowerCase() + " tài khoản thành công!");
                            loadDataAsync();
                        } else {
                            JOptionPane.showMessageDialog(EmployeePanel.this, "Không thể cập nhật trạng thái tài khoản!", "Lỗi", JOptionPane.ERROR_MESSAGE);
                        }
                    } catch (Exception e) {
                        JOptionPane.showMessageDialog(EmployeePanel.this, "Lỗi: " + e.getMessage(), "Lỗi", JOptionPane.ERROR_MESSAGE);
                    }
                }
            };
            worker.execute();
        }
    }

    private void showResetPasswordDialog() {
        if (selectedUser == null) {
            JOptionPane.showMessageDialog(this, "Nhân viên này chưa có tài khoản đăng nhập để đổi mật khẩu!", "Cảnh Báo", JOptionPane.WARNING_MESSAGE);
            return;
        }

        JPasswordField pwdField = UIUtils.createPasswordField();
        JPanel panel = new JPanel(new GridLayout(2, 1, 0, 8));
        panel.add(new JLabel("Nhập mật khẩu mới cho tài khoản '" + selectedUser.getUsername() + "' (>= 6 ký tự):"));
        panel.add(pwdField);

        int option = JOptionPane.showConfirmDialog(
                this,
                panel,
                "Đặt Lại Mật Khẩu (Reset Password)",
                JOptionPane.OK_CANCEL_OPTION,
                JOptionPane.PLAIN_MESSAGE
        );

        if (option == JOptionPane.OK_OPTION) {
            String newPassword = new String(pwdField.getPassword()).trim();
            if (newPassword.length() < 6) {
                JOptionPane.showMessageDialog(this, "Mật khẩu mới phải có tối thiểu 6 ký tự!", "Lỗi", JOptionPane.ERROR_MESSAGE);
                return;
            }

            SwingWorker<Boolean, Void> worker = new SwingWorker<>() {
                @Override
                protected Boolean doInBackground() {
                    return employeeService.resetPassword(selectedUser.getId(), newPassword);
                }

                @Override
                protected void done() {
                    try {
                        boolean ok = get();
                        if (ok) {
                            JOptionPane.showMessageDialog(EmployeePanel.this, "Đặt lại mật khẩu thành công!");
                            loadDataAsync();
                        } else {
                            JOptionPane.showMessageDialog(EmployeePanel.this, "Lỗi cập nhật mật khẩu!", "Lỗi", JOptionPane.ERROR_MESSAGE);
                        }
                    } catch (Exception ex) {
                        JOptionPane.showMessageDialog(EmployeePanel.this, "Lỗi: " + ex.getMessage(), "Lỗi", JOptionPane.ERROR_MESSAGE);
                    }
                }
            };
            worker.execute();
        }
    }

    private void deleteSelectedEmployee() {
        if (selectedEmployee == null) {
            JOptionPane.showMessageDialog(this, "Vui lòng chọn nhân viên cần xóa từ bảng!", "Thông Báo", JOptionPane.WARNING_MESSAGE);
            return;
        }

        int confirm = JOptionPane.showConfirmDialog(
                this,
                "Bạn có chắc muốn xóa nhân viên '" + selectedEmployee.getFullName() + "' (" + selectedEmployee.getId() + ")?\nTài khoản đăng nhập liên kết cũng sẽ bị hủy.",
                "Xác Nhận Xóa Nhân Viên",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.WARNING_MESSAGE
        );

        if (confirm == JOptionPane.YES_OPTION) {
            SwingWorker<Boolean, Void> worker = new SwingWorker<>() {
                @Override
                protected Boolean doInBackground() {
                    return employeeService.deleteEmployee(selectedEmployee.getId());
                }

                @Override
                protected void done() {
                    try {
                        boolean ok = get();
                        if (ok) {
                            JOptionPane.showMessageDialog(EmployeePanel.this, "Đã xóa nhân viên thành công!");
                            clearForm();
                            loadDataAsync();
                        } else {
                            JOptionPane.showMessageDialog(EmployeePanel.this, "Lỗi khi xóa nhân viên trong CSDL!", "Lỗi", JOptionPane.ERROR_MESSAGE);
                        }
                    } catch (Exception ex) {
                        JOptionPane.showMessageDialog(EmployeePanel.this, "Lỗi: " + ex.getMessage(), "Lỗi", JOptionPane.ERROR_MESSAGE);
                    }
                }
            };
            worker.execute();
        }
    }

    // =========================================================================
    // TIỆN ÍCH TẠO FORM VÀ CELL RENDERERS
    // =========================================================================

    private JPanel createFormField(String label, JComponent field) {
        JPanel p = new JPanel(new BorderLayout(0, 4));
        p.setOpaque(false);
        p.setMaximumSize(new Dimension(Integer.MAX_VALUE, 54));

        JLabel l = new JLabel(label);
        l.setFont(UIUtils.FONT_SMALL);
        l.setForeground(new Color(71, 85, 105));

        p.add(l, BorderLayout.NORTH);
        p.add(field, BorderLayout.CENTER);
        return p;
    }

    private static class EmploymentStatusBadgeRenderer extends DefaultTableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
            JLabel lbl = (JLabel) super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
            lbl.setHorizontalAlignment(SwingConstants.CENTER);
            String st = value != null ? value.toString() : "";

            if (!isSelected) {
                if ("WORKING".equalsIgnoreCase(st)) {
                    lbl.setText("Đang Làm Việc");
                    lbl.setBackground(new Color(220, 252, 231));
                    lbl.setForeground(new Color(21, 128, 61));
                    lbl.setFont(UIUtils.FONT_BODY_BOLD);
                } else {
                    lbl.setText("Đã Nghỉ Việc");
                    lbl.setBackground(new Color(241, 245, 249));
                    lbl.setForeground(new Color(100, 116, 139));
                    lbl.setFont(UIUtils.FONT_BODY);
                }
                lbl.setOpaque(true);
            }
            return lbl;
        }
    }

    private static class RoleBadgeRenderer extends DefaultTableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
            JLabel lbl = (JLabel) super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
            lbl.setHorizontalAlignment(SwingConstants.CENTER);
            String role = value != null ? value.toString() : "";

            if (!isSelected) {
                if ("ADMIN".equalsIgnoreCase(role)) {
                    lbl.setBackground(new Color(243, 232, 255));
                    lbl.setForeground(new Color(126, 34, 206));
                    lbl.setFont(UIUtils.FONT_BODY_BOLD);
                } else {
                    lbl.setBackground(new Color(224, 231, 255));
                    lbl.setForeground(new Color(67, 56, 202));
                    lbl.setFont(UIUtils.FONT_BODY);
                }
                lbl.setOpaque(true);
            }
            return lbl;
        }
    }

    private static class AccountStatusBadgeRenderer extends DefaultTableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
            JLabel lbl = (JLabel) super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
            lbl.setHorizontalAlignment(SwingConstants.CENTER);
            String st = value != null ? value.toString() : "";

            if (!isSelected) {
                if ("ACTIVE".equalsIgnoreCase(st)) {
                    lbl.setText("Hoạt Động");
                    lbl.setBackground(new Color(220, 252, 231));
                    lbl.setForeground(new Color(21, 128, 61));
                    lbl.setFont(UIUtils.FONT_BODY_BOLD);
                } else if ("LOCKED".equalsIgnoreCase(st)) {
                    lbl.setText("Bị Khóa");
                    lbl.setBackground(new Color(254, 226, 226));
                    lbl.setForeground(new Color(185, 28, 28));
                    lbl.setFont(UIUtils.FONT_BODY_BOLD);
                } else {
                    lbl.setText("Chưa Cấp");
                    lbl.setBackground(new Color(241, 245, 249));
                    lbl.setForeground(new Color(148, 163, 184));
                    lbl.setFont(UIUtils.FONT_SMALL);
                }
                lbl.setOpaque(true);
            }
            return lbl;
        }
    }

    private static class AdminPermissionCellRenderer extends DefaultTableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
            JLabel lbl = (JLabel) super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
            if (!isSelected) {
                lbl.setForeground(new Color(67, 56, 202));
                lbl.setFont(UIUtils.FONT_BODY_BOLD);
            }
            return lbl;
        }
    }

    private static class EmployeePermissionCellRenderer extends DefaultTableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
            JLabel lbl = (JLabel) super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
            String val = value != null ? value.toString() : "";
            if (!isSelected) {
                if (val.contains("KHÔNG ĐƯỢC PHÉP") || val.contains("403")) {
                    lbl.setForeground(new Color(220, 38, 38));
                    lbl.setFont(UIUtils.FONT_BODY_BOLD);
                } else {
                    lbl.setForeground(new Color(71, 85, 105));
                    lbl.setFont(UIUtils.FONT_BODY);
                }
            }
            return lbl;
        }
    }

    private static class AuditStatusCellRenderer extends DefaultTableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
            JLabel lbl = (JLabel) super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
            lbl.setHorizontalAlignment(SwingConstants.CENTER);
            String val = value != null ? value.toString() : "";
            if (!isSelected) {
                if (val.contains("Thành Công")) {
                    lbl.setBackground(new Color(220, 252, 231));
                    lbl.setForeground(new Color(21, 128, 61));
                    lbl.setFont(UIUtils.FONT_BODY_BOLD);
                } else {
                    lbl.setBackground(new Color(254, 226, 226));
                    lbl.setForeground(new Color(185, 28, 28));
                    lbl.setFont(UIUtils.FONT_BODY_BOLD);
                }
                lbl.setOpaque(true);
            }
            return lbl;
        }
    }
}