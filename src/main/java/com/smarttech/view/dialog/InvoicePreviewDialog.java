package com.smarttech.view.dialog;

import com.smarttech.service.ExportService;
import com.smarttech.view.util.UIUtils;

import javax.swing.*;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.awt.print.PrinterException;
import java.io.File;

/**
 * Hộp thoại Xem trước Bản in &amp; Xuất Tài Liệu (Invoice &amp; Rental Contract Print Preview Dialog).
 * Hiển thị mã HTML render trực quan, hỗ trợ gọi lệnh in trực tiếp qua máy in hệ thống
 * hoặc xuất file HTML / Text chuẩn bảng mã UTF-8 không lỗi font tiếng Việt.
 *
 * @author Senior Java Developer &amp; Software Architect
 * @project SmartTech
 */
public class InvoicePreviewDialog extends JDialog {

    private final String documentTitle;
    private final String htmlContent;
    private final ExportService exportService;

    private JEditorPane editorPane;
    private JButton btnPrint;
    private JButton btnExportHtml;
    private JButton btnExportTxt;
    private JButton btnClose;

    public InvoicePreviewDialog(Frame parent, String documentTitle, String htmlContent) {
        super(parent, "Bản In & Xuất Tài Liệu - " + documentTitle, true);
        this.documentTitle = documentTitle != null ? documentTitle : "Tài liệu SmartTech";
        this.htmlContent = htmlContent != null ? htmlContent : "<html><body>Không có nội dung.</body></html>";
        this.exportService = new ExportService();

        initComponents();
    }

    private void initComponents() {
        setSize(800, 840);
        setMinimumSize(new Dimension(680, 600));
        setLocationRelativeTo(getParent());
        setLayout(new BorderLayout());

        JPanel rootPanel = new JPanel(new BorderLayout());
        rootPanel.setBackground(UIUtils.COLOR_BG_LIGHT);

        // 1. TOP HEADER BAR
        JPanel topHeader = createTopHeader();
        rootPanel.add(topHeader, BorderLayout.NORTH);

        // 2. CENTER PREVIEW AREA
        editorPane = new JEditorPane();
        editorPane.setContentType("text/html; charset=UTF-8");
        editorPane.setText(htmlContent);
        editorPane.setEditable(false);
        editorPane.setCaretPosition(0);

        JScrollPane scrollPane = new JScrollPane(editorPane);
        scrollPane.setBorder(new CompoundBorder(
                new EmptyBorder(12, 16, 12, 16),
                new LineBorder(UIUtils.COLOR_BORDER, 1)
        ));
        scrollPane.getViewport().setBackground(Color.WHITE);
        rootPanel.add(scrollPane, BorderLayout.CENTER);

        // 3. BOTTOM ACTION BAR
        JPanel bottomBar = createBottomBar();
        rootPanel.add(bottomBar, BorderLayout.SOUTH);

        setContentPane(rootPanel);
    }

    /**
     * Tạo Top Header của Dialog.
     */
    private JPanel createTopHeader() {
        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(Color.WHITE);
        header.setBorder(new CompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, UIUtils.COLOR_BORDER),
                new EmptyBorder(14, 20, 14, 20)
        ));

        JPanel titleContainer = new JPanel(new GridLayout(2, 1, 0, 3));
        titleContainer.setOpaque(false);

        JLabel lblTitle = new JLabel("XEM TRƯỚC BẢN IN & XUẤT TÀI LIỆU (PRINT PREVIEW)");
        lblTitle.setFont(UIUtils.FONT_HEADER);
        lblTitle.setForeground(UIUtils.COLOR_TEXT_MAIN);

        JLabel lblSubtitle = new JLabel(documentTitle + " • Kiểm tra thông tin tài liệu trước khi in hoặc lưu trữ");
        lblSubtitle.setFont(UIUtils.FONT_SMALL);
        lblSubtitle.setForeground(UIUtils.COLOR_TEXT_MUTED);

        titleContainer.add(lblTitle);
        titleContainer.add(lblSubtitle);

        header.add(titleContainer, BorderLayout.WEST);
        return header;
    }

    /**
     * Tạo Bottom Action Bar chứa các nút In, Xuất file HTML/TXT và Đóng.
     */
    private JPanel createBottomBar() {
        JPanel bottom = new JPanel(new FlowLayout(FlowLayout.RIGHT, 12, 14));
        bottom.setBackground(Color.WHITE);
        bottom.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, UIUtils.COLOR_BORDER));

        btnPrint = new JButton("🖨️  In Tài Liệu Directly");
        btnPrint.setFont(UIUtils.FONT_BODY_BOLD);
        btnPrint.setBackground(UIUtils.COLOR_PRIMARY);
        btnPrint.setForeground(Color.WHITE);
        btnPrint.setFocusPainted(false);
        btnPrint.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btnPrint.setPreferredSize(new Dimension(175, 38));
        btnPrint.addActionListener(e -> executePrint());

        btnExportHtml = new JButton("🌐  Xuất File HTML");
        btnExportHtml.setFont(UIUtils.FONT_BODY);
        btnExportHtml.setBackground(Color.WHITE);
        btnExportHtml.setForeground(UIUtils.COLOR_TEXT_MAIN);
        btnExportHtml.setBorder(new LineBorder(UIUtils.COLOR_BORDER, 1));
        btnExportHtml.setFocusPainted(false);
        btnExportHtml.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btnExportHtml.setPreferredSize(new Dimension(145, 38));
        btnExportHtml.addActionListener(e -> executeExportHtml());

        btnExportTxt = new JButton("📄  Xuất File Text");
        btnExportTxt.setFont(UIUtils.FONT_BODY);
        btnExportTxt.setBackground(Color.WHITE);
        btnExportTxt.setForeground(UIUtils.COLOR_TEXT_MAIN);
        btnExportTxt.setBorder(new LineBorder(UIUtils.COLOR_BORDER, 1));
        btnExportTxt.setFocusPainted(false);
        btnExportTxt.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btnExportTxt.setPreferredSize(new Dimension(135, 38));
        btnExportTxt.addActionListener(e -> executeExportTxt());

        btnClose = new JButton("Đóng Window");
        btnClose.setFont(UIUtils.FONT_BODY);
        btnClose.setFocusPainted(false);
        btnClose.setPreferredSize(new Dimension(115, 38));
        btnClose.addActionListener(e -> dispose());

        bottom.add(btnPrint);
        bottom.add(btnExportHtml);
        bottom.add(btnExportTxt);
        bottom.add(btnClose);

        return bottom;
    }

    /**
     * Kích hoạt hộp thoại in tiêu chuẩn của hệ điều hành.
     */
    private void executePrint() {
        try {
            boolean complete = editorPane.print(null, null, true, null, null, true);
            if (complete) {
                JOptionPane.showMessageDialog(this,
                        "Lệnh in đã được chuyển tới máy in hệ thống thành công!",
                        "Thông Báo In", JOptionPane.INFORMATION_MESSAGE);
            }
        } catch (PrinterException pe) {
            JOptionPane.showMessageDialog(this,
                    "Không thể kết nối hoặc thực hiện in: " + pe.getMessage(),
                    "Lỗi In Ấn", JOptionPane.ERROR_MESSAGE);
        }
    }

    /**
     * Xuất ra file .html có hộp thoại chọn vị trí lưu thông minh.
     */
    private void executeExportHtml() {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("Lưu Tài Liệu HTML Chuẩn In");
        fileChooser.setFileFilter(new FileNameExtensionFilter("Tệp HTML (*.html)", "html"));

        String safeName = documentTitle.replaceAll("[^a-zA-Z0-9\\-_]", "_");
        fileChooser.setSelectedFile(new File(safeName + ".html"));

        int userSelection = fileChooser.showSaveDialog(this);
        if (userSelection == JFileChooser.APPROVE_OPTION) {
            File fileToSave = fileChooser.getSelectedFile();
            // Đảm bảo đuôi file .html nếu người dùng quên gõ
            if (!fileToSave.getName().toLowerCase().endsWith(".html")) {
                fileToSave = new File(fileToSave.getParentFile(), fileToSave.getName() + ".html");
            }

            boolean ok = exportService.exportHtmlToFile(htmlContent, fileToSave);
            if (ok) {
                JOptionPane.showMessageDialog(this,
                        "Đã xuất file HTML thành công tại:\n" + fileToSave.getAbsolutePath(),
                        "Xuất File Thành Công", JOptionPane.INFORMATION_MESSAGE);
            } else {
                JOptionPane.showMessageDialog(this,
                        "Không thể ghi file HTML!",
                        "Lỗi Xuất File", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    /**
     * Xuất ra file văn bản thuần .txt từ nội dung đã bóc tách HTML.
     */
    private void executeExportTxt() {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("Lưu Tài Liệu Dạng Văn Bản Thuần (TXT)");
        fileChooser.setFileFilter(new FileNameExtensionFilter("Tệp Văn Bản (*.txt)", "txt"));

        String safeName = documentTitle.replaceAll("[^a-zA-Z0-9\\-_]", "_");
        fileChooser.setSelectedFile(new File(safeName + ".txt"));

        int userSelection = fileChooser.showSaveDialog(this);
        if (userSelection == JFileChooser.APPROVE_OPTION) {
            File fileToSave = fileChooser.getSelectedFile();
            // Đảm bảo đuôi file .txt nếu người dùng quên gõ
            if (!fileToSave.getName().toLowerCase().endsWith(".txt")) {
                fileToSave = new File(fileToSave.getParentFile(), fileToSave.getName() + ".txt");
            }

            // Bóc tách văn bản thô từ JEditorPane
            String plainText = editorPane.getText();
            try {
                plainText = editorPane.getDocument().getText(0, editorPane.getDocument().getLength());
            } catch (Exception ignored) {}

            boolean ok = exportService.exportToTextFile(plainText, fileToSave);
            if (ok) {
                JOptionPane.showMessageDialog(this,
                        "Đã xuất file văn bản Text thành công tại:\n" + fileToSave.getAbsolutePath(),
                        "Xuất File Thành Công", JOptionPane.INFORMATION_MESSAGE);
            } else {
                JOptionPane.showMessageDialog(this,
                        "Không thể ghi file Text!",
                        "Lỗi Xuất File", JOptionPane.ERROR_MESSAGE);
            }
        }
    }
}
