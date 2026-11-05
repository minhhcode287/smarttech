package com.smarttech.view;

import com.smarttech.service.AuthService;
import com.smarttech.view.util.UIUtils;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;

/**
 * Màn hình Đăng nhập (LoginFrame) được thiết kế hiện đại theo phong cách Split Screen:
 * - Nửa bên trái: Banner thương hiệu SmartTech với gradient xanh Navy, thông điệp hệ thống và điểm nổi bật.
 * - Nửa bên phải: Form đăng nhập với bố cục GridBagLayout chuẩn mực, trường nhập liệu bo viền và nút bấm ấn tượng.
 *
 * @author Senior Java Developer &amp; Software Architect
 * @project SmartTech
 */
public class LoginFrame extends JFrame {

    private JTextField txtUsername;
    private JPasswordField txtPassword;
    private JCheckBox chkRemember;
    private JButton btnLogin;
    private JLabel lblErrorMessage;

    private final AuthService authService;

    public LoginFrame() {
        this.authService = new AuthService();
        initComponents();
    }

    private void initComponents() {
        setTitle("SmartTech - Đăng Nhập Hệ Thống");
        setSize(880, 540);
        setMinimumSize(new Dimension(800, 500));
        setLocationRelativeTo(null);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setResizable(false);

        // Container chính sử dụng GridLayout 1 hàng 2 cột tỉ lệ 50-50
        JPanel container = new JPanel(new GridLayout(1, 2));

        // -------------------------------------------------------------
        // CỘT TRÁI: BRANDING BANNER
        // -------------------------------------------------------------
        JPanel leftBanner = createBrandBannerPanel();
        container.add(leftBanner);

        // -------------------------------------------------------------
        // CỘT PHẢI: FORM ĐĂNG NHẬP
        // -------------------------------------------------------------
        JPanel rightForm = createLoginFormPanel();
        container.add(rightForm);

        setContentPane(container);
    }

    /**
     * Tạo Panel thương hiệu bên trái với nền Gradient Deep Blue và các tính năng chính của hệ thống.
     */
    private JPanel createBrandBannerPanel() {
        JPanel bannerPanel = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2d = (Graphics2D) g.create();
                g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                // Gradient từ Navy tối sang Deep Blue
                GradientPaint gradient = new GradientPaint(
                        0, 0, new Color(15, 23, 42),
                        getWidth(), getHeight(), new Color(30, 58, 138)
                );
                g2d.setPaint(gradient);
                g2d.fillRect(0, 0, getWidth(), getHeight());
                g2d.dispose();
            }
        };

        bannerPanel.setLayout(new BoxLayout(bannerPanel, BoxLayout.Y_AXIS));
        bannerPanel.setBorder(new EmptyBorder(60, 40, 60, 40));

        // Nhãn Logo thương hiệu
        JLabel lblLogo = new JLabel("SMARTTECH");
        lblLogo.setFont(new Font("Segoe UI", Font.BOLD, 30));
        lblLogo.setForeground(Color.WHITE);
        lblLogo.setAlignmentX(Component.LEFT_ALIGNMENT);

        // Slogan hệ thống
        JLabel lblSlogan = new JLabel("HỆ THỐNG QUẢN LÝ THIẾT BỊ CÔNG NGHỆ");
        lblSlogan.setFont(new Font("Segoe UI", Font.BOLD, 12));
        lblSlogan.setForeground(new Color(56, 189, 248)); // Sky 400
        lblSlogan.setAlignmentX(Component.LEFT_ALIGNMENT);

        // Đoạn giới thiệu
        JLabel lblIntro = new JLabel("<html><p style='width: 260px; line-height: 1.4; color: #cbd5e1; margin-top: 15px;'>"
                + "Giải pháp phần mềm quản trị toàn diện cho các chuỗi phân phối bán lẻ và cho thuê thiết bị điện tử, laptop, studio, VR/AR hàng đầu."
                + "</p></html>");
        lblIntro.setFont(UIUtils.FONT_BODY);
        lblIntro.setAlignmentX(Component.LEFT_ALIGNMENT);

        // Các tính năng cốt lõi
        JPanel featuresPanel = new JPanel();
        featuresPanel.setLayout(new BoxLayout(featuresPanel, BoxLayout.Y_AXIS));
        featuresPanel.setOpaque(false);
        featuresPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
        featuresPanel.setBorder(new EmptyBorder(25, 0, 0, 0));

        String[] highlights = {
                "Quản lý thiết bị theo Serial phần cứng",
                "Hợp đồng thuê thiết bị & Tính giá linh hoạt",
                "Cơ sở dữ liệu MongoDB hiệu năng cao",
                "Báo cáo doanh thu & Điểm thưởng thành viên"
        };

        for (String item : highlights) {
            JLabel lblItem = new JLabel("• " + item);
            lblItem.setFont(new Font("Segoe UI", Font.PLAIN, 12));
            lblItem.setForeground(new Color(241, 245, 249));
            lblItem.setBorder(new EmptyBorder(4, 0, 4, 0));
            featuresPanel.add(lblItem);
        }

        bannerPanel.add(lblLogo);
        bannerPanel.add(Box.createVerticalStrut(4));
        bannerPanel.add(lblSlogan);
        bannerPanel.add(lblIntro);
        bannerPanel.add(featuresPanel);
        bannerPanel.add(Box.createVerticalGlue());

        JLabel lblCopyright = new JLabel("Phiên bản 1.0 Enterprise • Java 21");
        lblCopyright.setFont(UIUtils.FONT_SMALL);
        lblCopyright.setForeground(new Color(148, 163, 184));
        lblCopyright.setAlignmentX(Component.LEFT_ALIGNMENT);
        bannerPanel.add(lblCopyright);

        return bannerPanel;
    }

    /**
     * Tạo Panel Form Đăng nhập bên phải với bố cục GridBagLayout chuyên nghiệp.
     */
    private JPanel createLoginFormPanel() {
        JPanel formPanel = new JPanel(new GridBagLayout());
        formPanel.setBackground(Color.WHITE);
        formPanel.setBorder(new EmptyBorder(40, 45, 40, 45));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(6, 0, 6, 0);
        gbc.weightx = 1.0;

        // 1. Tiêu đề Form
        JLabel lblHeading = new JLabel("Đăng Nhập");
        lblHeading.setFont(UIUtils.FONT_TITLE);
        lblHeading.setForeground(UIUtils.COLOR_TEXT_MAIN);
        gbc.gridx = 0;
        gbc.gridy = 0;
        formPanel.add(lblHeading, gbc);

        // 2. Phụ đề
        JLabel lblSub = new JLabel("Nhập tài khoản để truy cập hệ thống quản trị");
        lblSub.setFont(UIUtils.FONT_SUBTITLE);
        lblSub.setForeground(UIUtils.COLOR_TEXT_MUTED);
        gbc.gridy = 1;
        gbc.insets = new Insets(0, 0, 16, 0);
        formPanel.add(lblSub, gbc);

        // 3. Thông báo lỗi (Ẩn mặc định)
        lblErrorMessage = new JLabel(" ");
        lblErrorMessage.setFont(UIUtils.FONT_BODY_BOLD);
        lblErrorMessage.setForeground(UIUtils.COLOR_DANGER);
        gbc.gridy = 2;
        gbc.insets = new Insets(0, 0, 8, 0);
        formPanel.add(lblErrorMessage, gbc);

        // 4. Label Username
        JLabel lblUsername = new JLabel("Tên đăng nhập");
        lblUsername.setFont(UIUtils.FONT_BODY_BOLD);
        lblUsername.setForeground(UIUtils.COLOR_TEXT_MAIN);
        gbc.gridy = 3;
        gbc.insets = new Insets(4, 0, 4, 0);
        formPanel.add(lblUsername, gbc);

        // 5. Input Username
        txtUsername = UIUtils.createTextField();
        txtUsername.setText("admin"); // Giá trị mẫu gợi ý
        gbc.gridy = 4;
        gbc.insets = new Insets(0, 0, 12, 0);
        formPanel.add(txtUsername, gbc);

        // 6. Label Password
        JLabel lblPassword = new JLabel("Mật khẩu");
        lblPassword.setFont(UIUtils.FONT_BODY_BOLD);
        lblPassword.setForeground(UIUtils.COLOR_TEXT_MAIN);
        gbc.gridy = 5;
        gbc.insets = new Insets(4, 0, 4, 0);
        formPanel.add(lblPassword, gbc);

        // 7. Input Password
        txtPassword = UIUtils.createPasswordField();
        txtPassword.setText("admin123"); // Giá trị mẫu gợi ý
        gbc.gridy = 6;
        gbc.insets = new Insets(0, 0, 10, 0);
        formPanel.add(txtPassword, gbc);

        // 8. Remember Me Checkbox
        chkRemember = new JCheckBox("Ghi nhớ tài khoản trên thiết bị này");
        chkRemember.setFont(UIUtils.FONT_BODY);
        chkRemember.setForeground(UIUtils.COLOR_TEXT_MUTED);
        chkRemember.setBackground(Color.WHITE);
        chkRemember.setFocusPainted(false);
        chkRemember.setSelected(true);
        gbc.gridy = 7;
        gbc.insets = new Insets(0, 0, 16, 0);
        formPanel.add(chkRemember, gbc);

        // 9. Nút Đăng nhập
        btnLogin = UIUtils.createPrimaryButton("ĐĂNG NHẬP");
        gbc.gridy = 8;
        gbc.insets = new Insets(8, 0, 10, 0);
        formPanel.add(btnLogin, gbc);

        // ---------------- SỰ KIỆN XỬ LÝ ----------------
        btnLogin.addActionListener(e -> executeLogin());

        // Lắng nghe phím Enter trên ô Username và Password
        KeyAdapter enterListener = new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                    executeLogin();
                }
            }
        };
        txtUsername.addKeyListener(enterListener);
        txtPassword.addKeyListener(enterListener);

        return formPanel;
    }

    /**
     * Thực hiện kiểm tra tính hợp lệ và xác thực đăng nhập.
     */
    private void executeLogin() {
        String username = txtUsername.getText().trim();
        String password = new String(txtPassword.getPassword());

        if (username.isEmpty() || password.isEmpty()) {
            showError("Vui lòng nhập đầy đủ Tên đăng nhập và Mật khẩu!");
            return;
        }

        lblErrorMessage.setText("Đang kiểm tra thông tin đăng nhập...");
        lblErrorMessage.setForeground(UIUtils.COLOR_PRIMARY);

        // Gọi AuthService xử lý xác thực
        boolean isAuthenticated = authService.login(username, password);

        if (isAuthenticated) {
            lblErrorMessage.setText("Đăng nhập thành công! Đang chuyển hướng...");
            lblErrorMessage.setForeground(UIUtils.COLOR_SUCCESS);

            // Đóng LoginFrame và mở MainFrame
            SwingUtilities.invokeLater(() -> {
                this.dispose();
                MainFrame mainFrame = new MainFrame();
                mainFrame.setVisible(true);
            });
        } else {
            showError("Tài khoản hoặc mật khẩu không chính xác!");
            txtPassword.requestFocus();
            txtPassword.selectAll();
        }
    }

    /**
     * Hiển thị thông báo lỗi trực quan trên giao diện form.
     */
    private void showError(String message) {
        lblErrorMessage.setText(message);
        lblErrorMessage.setForeground(UIUtils.COLOR_DANGER);
    }
}
