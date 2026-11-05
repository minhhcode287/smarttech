package com.smarttech.view.util;

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import java.awt.*;

/**
 * Lớp tiện ích cấu hình Giao diện người dùng Swing (UIUtils).
 * Cung cấp Look and Feel hiện đại (FlatLaf hoặc System Look &amp; Feel),
 * bảng màu chuẩn UI/UX doanh nghiệp (Deep Navy / Modern Blue / Cyan Teal) và font chữ sắc nét.
 *
 * @author Senior Java Developer &amp; Software Architect
 * @project SmartTech
 */
public class UIUtils {

    // ================= BẢNG MÀU THIẾT KẾ DOANH NGHIỆP =================
    public static final Color COLOR_PRIMARY = new Color(26, 86, 219);       // Xanh dương chủ đạo (#1A56DB)
    public static final Color COLOR_PRIMARY_HOVER = new Color(30, 66, 159); // Xanh đậm hover (#1E429F)
    public static final Color COLOR_ACCENT = new Color(13, 148, 136);       // Xanh mòng két Teal (#0D9488)

    public static final Color COLOR_DARK_SIDEBAR = new Color(15, 23, 42);   // Xanh đen Sidebar (#0F172A)
    public static final Color COLOR_BANNER_BG = new Color(30, 41, 59);      // Nền Banner bên trái (#1E293B)
    public static final Color COLOR_BG_LIGHT = new Color(248, 250, 252);    // Nền sáng Slate 50 (#F8FAFC)
    public static final Color COLOR_CARD_BG = Color.WHITE;                  // Nền thẻ trắng

    public static final Color COLOR_TEXT_MAIN = new Color(15, 23, 42);      // Chữ chính Slate 900
    public static final Color COLOR_TEXT_MUTED = new Color(100, 116, 139);  // Chữ phụ Slate 500
    public static final Color COLOR_BORDER = new Color(226, 232, 240);      // Viền nhẹ Slate 200
    public static final Color COLOR_DANGER = new Color(220, 38, 38);        // Đỏ cảnh báo (#DC2626)
    public static final Color COLOR_SUCCESS = new Color(22, 163, 74);       // Xanh lá thành công (#16A34A)

    // ================= FONT CHỮ CHUẨN =================
    public static final Font FONT_TITLE = new Font("Segoe UI", Font.BOLD, 24);
    public static final Font FONT_SUBTITLE = new Font("Segoe UI", Font.PLAIN, 14);
    public static final Font FONT_HEADER = new Font("Segoe UI", Font.BOLD, 16);
    public static final Font FONT_BODY = new Font("Segoe UI", Font.PLAIN, 13);
    public static final Font FONT_BODY_BOLD = new Font("Segoe UI", Font.BOLD, 13);
    public static final Font FONT_SMALL = new Font("Segoe UI", Font.PLAIN, 11);

    /**
     * Khởi tạo Look and Feel hiện đại.
     * Tự động phát hiện FlatLaf trong classpath hoặc dự phòng sang System Look and Feel.
     */
    public static void setupLookAndFeel() {
        try {
            // Thử khởi tạo FlatLaf Light qua Reflection để đảm bảo tương thích mọi môi trường
            Class<?> flatLightLafClass = Class.forName("com.formdev.flatlaf.FlatLightLaf");
            flatLightLafClass.getMethod("setup").invoke(null);
            System.out.println("[UIUtils] Khởi tạo thành công FlatLaf Light Look & Feel.");
        } catch (Throwable e) {
            try {
                // Fallback sang giao diện gốc của hệ điều hành (Windows / macOS / Linux GTK)
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
                System.out.println("[UIUtils] Áp dụng System Look and Feel: " + UIManager.getLookAndFeel().getName());
            } catch (Exception ex) {
                System.err.println("[UIUtils] Không thể cài đặt Look and Feel: " + ex.getMessage());
            }
        }

        // Tinh chỉnh Antialiasing giúp chữ hiển thị mượt mà
        System.setProperty("awt.useSystemAAFontSettings", "on");
        System.setProperty("swing.aatext", "true");
    }

    /**
     * Tạo nút bấm chính với màu Primary (Blue) và bo viền êm ái.
     */
    public static JButton createPrimaryButton(String text) {
        JButton button = new JButton(text);
        button.setFont(FONT_BODY_BOLD);
        button.setBackground(COLOR_PRIMARY);
        button.setForeground(Color.WHITE);
        button.setFocusPainted(false);
        button.setBorderPainted(false);
        button.setCursor(new Cursor(Cursor.HAND_CURSOR));
        button.setPreferredSize(new Dimension(160, 42));

        // Hiệu ứng hover chuột
        button.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseEntered(java.awt.event.MouseEvent evt) {
                button.setBackground(COLOR_PRIMARY_HOVER);
            }

            @Override
            public void mouseExited(java.awt.event.MouseEvent evt) {
                button.setBackground(COLOR_PRIMARY);
            }
        });

        return button;
    }

    /**
     * Tạo ô nhập liệu có padding lề trong và viền xám nhẹ thanh thoát.
     */
    public static JTextField createTextField() {
        JTextField textField = new JTextField();
        textField.setFont(FONT_BODY);
        textField.setForeground(COLOR_TEXT_MAIN);
        textField.setCaretColor(COLOR_PRIMARY);
        textField.setPreferredSize(new Dimension(280, 40));

        Border lineBorder = new LineBorder(COLOR_BORDER, 1, true);
        Border paddingBorder = new EmptyBorder(8, 12, 8, 12);
        textField.setBorder(new CompoundBorder(lineBorder, paddingBorder));

        return textField;
    }

    /**
     * Tạo ô nhập mật khẩu có padding lề trong và viền xám thanh thoát.
     */
    public static JPasswordField createPasswordField() {
        JPasswordField passwordField = new JPasswordField();
        passwordField.setFont(FONT_BODY);
        passwordField.setForeground(COLOR_TEXT_MAIN);
        passwordField.setCaretColor(COLOR_PRIMARY);
        passwordField.setPreferredSize(new Dimension(280, 40));

        Border lineBorder = new LineBorder(COLOR_BORDER, 1, true);
        Border paddingBorder = new EmptyBorder(8, 12, 8, 12);
        passwordField.setBorder(new CompoundBorder(lineBorder, paddingBorder));

        return passwordField;
    }
}
