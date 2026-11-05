package com.smarttech.view;

import com.smarttech.model.User;
import com.smarttech.service.AuthService;
import com.smarttech.service.UserSession;
import com.smarttech.view.panel.*;
import com.smarttech.view.util.UIUtils;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.util.HashMap;
import java.util.Map;

/**
 * Giao diện Quản trị Chính của hệ thống SmartTech (MainFrame).
 * Kiến trúc Enterprise gồm 3 phân vùng chính kết hợp kiểm soát truy cập dựa trên vai trò (RBAC):
 * 1. Sidebar (Trái): Menu điều hướng. Phân quyền ẩn hoặc vô hiệu hóa đối với EMPLOYEE.
 * 2. Top Header (Trên): Nhận diện thương hiệu, thông tin tài khoản đăng nhập từ UserSession, Huy hiệu quyền hạn và nút Đăng xuất.
 * 3. Main Content Panel (Giữa): Container sử dụng CardLayout để hiển thị các phân hệ. Bắt chặn truy cập trái phép.
 *
 * @author Senior Java Developer & Software Architect
 * @project SmartTech
 */
public class MainFrame extends JFrame {

    // Khóa định danh cho các Card trong CardLayout
    public static final String CARD_DASHBOARD = "CARD_DASHBOARD";
    public static final String CARD_PRODUCTS = "CARD_PRODUCTS";
    public static final String CARD_CUSTOMERS = "CARD_CUSTOMERS";
    public static final String CARD_ORDERS = "CARD_ORDERS";
    public static final String CARD_IMPORT = "CARD_IMPORT";
    public static final String CARD_MAINTENANCE = "CARD_MAINTENANCE";
    public static final String CARD_STOCK_AUDIT = "STOCK_AUDIT";
    public static final String CARD_PROMOTION = "CARD_PROMOTION";
    public static final String CARD_EMPLOYEES = "CARD_EMPLOYEES";
    public static final String CARD_REPORTS = "CARD_REPORTS";

    private final AuthService authService;
    private final CardLayout cardLayout;
    private final JPanel mainContentPanel;

    // Quản lý danh sách các nút trên Sidebar để xử lý hiệu ứng Active và phân quyền RBAC
    private final Map<String, JButton> sidebarButtons;
    private JButton currentActiveButton;

    public MainFrame() {
        this.authService = new AuthService();
        this.cardLayout = new CardLayout();
        this.mainContentPanel = new JPanel(this.cardLayout);
        this.sidebarButtons = new HashMap<>();

        initComponents();
    }

    private void initComponents() {
        setTitle("SmartTech Enterprise - Hệ Thống Quản Lý Bán Hàng & Cho Thuê Thiết Bị Công Nghệ");
        setSize(1320, 820);
        setMinimumSize(new Dimension(1080, 680));
        setLocationRelativeTo(null);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        // Container chính
        JPanel rootPanel = new JPanel(new BorderLayout());
        rootPanel.setBackground(UIUtils.COLOR_BG_LIGHT);

        // -------------------------------------------------------------
        // 1. THANH ĐIỀU HƯỚNG BÊN TRÁI (SIDEBAR)
        // -------------------------------------------------------------
        JPanel sidebarPanel = createSidebarPanel();
        rootPanel.add(sidebarPanel, BorderLayout.WEST);

        // -------------------------------------------------------------
        // 2. KHU VỰC CHÍNH BÊN PHẢI (GỒM TOP HEADER VÀ CONTENT WORKSPACE)
        // -------------------------------------------------------------
        JPanel rightContainer = new JPanel(new BorderLayout());
        rightContainer.setOpaque(false);

        // Top Header
        JPanel topHeader = createTopHeaderPanel();
        rightContainer.add(topHeader, BorderLayout.NORTH);

        // Khởi tạo các Panels chức năng vào CardLayout
        mainContentPanel.add(new DashboardPanel(), CARD_DASHBOARD);
        mainContentPanel.add(new ProductPanel(), CARD_PRODUCTS);
        mainContentPanel.add(new CustomerPanel(), CARD_CUSTOMERS);
        mainContentPanel.add(new OrderRentalPanel(), CARD_ORDERS);
        mainContentPanel.add(new ImportPanel(), CARD_IMPORT);
        mainContentPanel.add(new MaintenancePanel(), CARD_MAINTENANCE);
        mainContentPanel.add(new StockAuditPanel(), CARD_STOCK_AUDIT);
        mainContentPanel.add(new PromotionManagementPanel(), CARD_PROMOTION);
        mainContentPanel.add(new EmployeePanel(), CARD_EMPLOYEES);
        mainContentPanel.add(new ReportPanel(), CARD_REPORTS);

        rightContainer.add(mainContentPanel, BorderLayout.CENTER);
        rootPanel.add(rightContainer, BorderLayout.CENTER);

        setContentPane(rootPanel);

        // Mặc định kích hoạt tab Dashboard đầu tiên
        switchTab(CARD_DASHBOARD);
    }

    /**
     * Tạo Sidebar điều hướng chuyên nghiệp và áp dụng phân quyền RBAC trực tiếp trên Menu items.
     */
    private JPanel createSidebarPanel() {
        JPanel sidebar = new JPanel(new BorderLayout());
        sidebar.setPreferredSize(new Dimension(250, 0));
        sidebar.setBackground(UIUtils.COLOR_DARK_SIDEBAR);

        // Phần Logo & Tên ứng dụng ở trên cùng Sidebar
        JPanel brandPanel = new JPanel(new BorderLayout());
        brandPanel.setOpaque(false);
        brandPanel.setBorder(new EmptyBorder(24, 20, 24, 20));

        JLabel lblBrand = new JLabel("SMARTTECH");
        lblBrand.setFont(new Font("Segoe UI", Font.BOLD, 22));
        lblBrand.setForeground(Color.WHITE);

        JLabel lblTagline = new JLabel("ENTERPRISE v1.0 • RBAC");
        lblTagline.setFont(new Font("Segoe UI", Font.BOLD, 10));
        lblTagline.setForeground(new Color(56, 189, 248)); // Sky 400

        brandPanel.add(lblBrand, BorderLayout.NORTH);
        brandPanel.add(lblTagline, BorderLayout.SOUTH);
        sidebar.add(brandPanel, BorderLayout.NORTH);

        // Danh sách các Menu Tabs ở giữa
        JPanel menuContainer = new JPanel();
        menuContainer.setLayout(new BoxLayout(menuContainer, BoxLayout.Y_AXIS));
        menuContainer.setOpaque(false);
        menuContainer.setBorder(new EmptyBorder(10, 12, 10, 12));

        // Phân quyền RBAC: Đọc vai trò người dùng từ UserSession
        boolean isAdmin = UserSession.getInstance().isAdmin();

        // 1. Các Menu dùng chung cho cả ADMIN và EMPLOYEE
        addMenuItem(menuContainer, "  Tổng Quan (Dashboard)", CARD_DASHBOARD, true);
        addMenuItem(menuContainer, "  Sản Phẩm & Thiết Bị", CARD_PRODUCTS, true);
        addMenuItem(menuContainer, "  Quản Lý Khách Hàng", CARD_CUSTOMERS, true);
        addMenuItem(menuContainer, "  Bán Hàng & Cho Thuê", CARD_ORDERS, true);
        addMenuItem(menuContainer, "  Quản Lý Nhập Kho", CARD_IMPORT, true);
        addMenuItem(menuContainer, "  Bảo Trì & Nhật Ký", CARD_MAINTENANCE, true);
        addMenuItem(menuContainer, "  Kiểm Kê Kho", CARD_STOCK_AUDIT, true);
        addMenuItem(menuContainer, "  Chương Trình Khuyến Mãi", CARD_PROMOTION, true);

        // 2. Các Menu ĐẶC QUYỀN dành riêng cho ADMIN
        addMenuItem(menuContainer, "  Quản Lý Nhân Sự", CARD_EMPLOYEES, isAdmin);
        addMenuItem(menuContainer, "  Báo Cáo Thống Kê", CARD_REPORTS, isAdmin);

        sidebar.add(menuContainer, BorderLayout.CENTER);

        // Chân trang Sidebar
        JPanel footerPanel = new JPanel(new BorderLayout());
        footerPanel.setOpaque(false);
        footerPanel.setBorder(new EmptyBorder(16, 20, 16, 20));

        JLabel lblDbStatus = new JLabel("● MongoDB: Sẵn Sàng Vận Hành");
        lblDbStatus.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        lblDbStatus.setForeground(new Color(74, 222, 128)); // Green 400
        footerPanel.add(lblDbStatus, BorderLayout.CENTER);

        sidebar.add(footerPanel, BorderLayout.SOUTH);
        return sidebar;
    }

    /**
     * Tạo nút bấm mục Menu trên Sidebar và gắn sự kiện click kèm phân quyền RBAC.
     */
    private void addMenuItem(JPanel container, String title, String cardKey, boolean isAuthorized) {
        JButton btn = new JButton(title);
        btn.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        btn.setForeground(isAuthorized ? new Color(203, 213, 225) : new Color(100, 116, 139));
        btn.setBackground(UIUtils.COLOR_DARK_SIDEBAR);
        btn.setFocusPainted(false);
        btn.setBorderPainted(false);
        btn.setHorizontalAlignment(SwingConstants.LEFT);
        btn.setMaximumSize(new Dimension(Integer.MAX_VALUE, 42));
        btn.setPreferredSize(new Dimension(226, 42));
        btn.setCursor(isAuthorized ? new Cursor(Cursor.HAND_CURSOR) : new Cursor(Cursor.DEFAULT_CURSOR));
        btn.setBorder(new EmptyBorder(0, 12, 0, 12));

        if (!isAuthorized) {
            btn.setEnabled(false);
            btn.setVisible(false);
        } else {
            btn.addActionListener(e -> switchTab(cardKey));
        }

        sidebarButtons.put(cardKey, btn);
        container.add(btn);
        container.add(Box.createVerticalStrut(6));
    }

    /**
     * Chuyển đổi tab trên CardLayout, kiểm tra tường lửa bảo vệ RBAC
     * và đổi màu highlight của nút Sidebar tương ứng.
     */
    public void switchTab(String cardKey) {
        boolean isAdmin = UserSession.getInstance().isAdmin();

        // TƯỜNG LỬA BẢO VỆ PHÂN QUYỀN (RBAC INTERCEPTOR)
        if (!isAdmin && (CARD_EMPLOYEES.equals(cardKey) || CARD_REPORTS.equals(cardKey))) {
            JOptionPane.showMessageDialog(
                    this,
                    "Bạn không có quyền truy cập chức năng này!\nChỉ Quản trị viên (ADMIN) mới được phép xem Quản lý Nhân sự & Báo cáo Doanh thu.",
                    "Từ Chối Truy Cập (403 Forbidden)",
                    JOptionPane.WARNING_MESSAGE
            );
            cardKey = CARD_DASHBOARD;
        }

        cardLayout.show(mainContentPanel, cardKey);

        // Cập nhật phong cách nút Active
        JButton targetBtn = sidebarButtons.get(cardKey);
        if (targetBtn != null && targetBtn.isEnabled()) {
            if (currentActiveButton != null) {
                currentActiveButton.setBackground(UIUtils.COLOR_DARK_SIDEBAR);
                currentActiveButton.setForeground(new Color(203, 213, 225));
                currentActiveButton.setFont(new Font("Segoe UI", Font.PLAIN, 13));
            }

            targetBtn.setBackground(UIUtils.COLOR_PRIMARY);
            targetBtn.setForeground(Color.WHITE);
            targetBtn.setFont(new Font("Segoe UI", Font.BOLD, 13));
            currentActiveButton = targetBtn;
        }
    }

    /**
     * Tạo thanh Top Header Bar chứa thông tin cá nhân và nút Đăng xuất.
     */
    private JPanel createTopHeaderPanel() {
        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(Color.WHITE);
        header.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, UIUtils.COLOR_BORDER),
                new EmptyBorder(12, 28, 12, 28)
        ));

        JLabel lblHeaderTitle = new JLabel("HỆ THỐNG QUẢN TRỊ DOANH NGHIỆP SMARTTECH");
        lblHeaderTitle.setFont(new Font("Segoe UI", Font.BOLD, 14));
        lblHeaderTitle.setForeground(new Color(71, 85, 105));

        JPanel rightInfoPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 14, 0));
        rightInfoPanel.setOpaque(false);

        User currentUser = UserSession.getInstance().getCurrentUser();
        String fullName = currentUser != null ? currentUser.getFullName() : "Quản Trị Viên";
        String role = currentUser != null ? currentUser.getRole() : "ADMIN";

        JLabel lblName = new JLabel(fullName);
        lblName.setFont(UIUtils.FONT_BODY_BOLD);
        lblName.setForeground(UIUtils.COLOR_TEXT_MAIN);

        JLabel lblRoleBadge = new JLabel(" " + role + " ");
        lblRoleBadge.setFont(new Font("Segoe UI", Font.BOLD, 11));
        lblRoleBadge.setOpaque(true);
        if ("ADMIN".equalsIgnoreCase(role)) {
            lblRoleBadge.setBackground(new Color(254, 226, 226));
            lblRoleBadge.setForeground(new Color(185, 28, 28));
        } else {
            lblRoleBadge.setBackground(new Color(224, 231, 255));
            lblRoleBadge.setForeground(new Color(67, 56, 202));
        }
        lblRoleBadge.setBorder(BorderFactory.createEmptyBorder(2, 6, 2, 6));

        JButton btnLogout = new JButton("Đăng Xuất");
        btnLogout.setFont(UIUtils.FONT_BODY);
        btnLogout.setForeground(UIUtils.COLOR_DANGER);
        btnLogout.setBackground(Color.WHITE);
        btnLogout.setBorder(BorderFactory.createLineBorder(UIUtils.COLOR_DANGER, 1));
        btnLogout.setFocusPainted(false);
        btnLogout.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btnLogout.setPreferredSize(new Dimension(95, 32));

        btnLogout.addActionListener(e -> {
            int confirm = JOptionPane.showConfirmDialog(
                    this,
                    "Bạn có chắc chắn muốn đăng xuất khỏi hệ thống?",
                    "Xác nhận đăng xuất",
                    JOptionPane.YES_NO_OPTION,
                    JOptionPane.QUESTION_MESSAGE
            );

            if (confirm == JOptionPane.YES_OPTION) {
                authService.logout();
                this.dispose();
                new LoginFrame().setVisible(true);
            }
        });

        rightInfoPanel.add(lblName);
        rightInfoPanel.add(lblRoleBadge);
        rightInfoPanel.add(Box.createHorizontalStrut(8));
        rightInfoPanel.add(btnLogout);

        header.add(lblHeaderTitle, BorderLayout.WEST);
        header.add(rightInfoPanel, BorderLayout.EAST);
        return header;
    }
}