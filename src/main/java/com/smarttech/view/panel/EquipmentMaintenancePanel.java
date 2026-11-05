package com.smarttech.view.panel;

import com.smarttech.model.Equipment;
import com.smarttech.model.EquipmentLog;
import com.smarttech.model.Product;
import com.smarttech.service.EquipmentService;
import com.smarttech.service.ProductService;
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
 * Giao diện Quản Lý Bảo Trì Thiết Bị & Nhật Ký Biến Động Phần Cứng (Equipment Maintenance & Audit Log Panel).
 * Toàn bộ các tác vụ truy vấn cơ sở dữ liệu MongoDB đều thực thi bất đồng bộ qua SwingWorker
 * nhằm đảm bảo Event Dispatch Thread (EDT) luôn mượt mà 100%.
 *
 * @author Senior Java Developer & Software Architect
 * @project SmartTech
 */
public class EquipmentMaintenancePanel extends JPanel {

    private final EquipmentService equipmentService;
    private final ProductService productService;

    // Cache danh sách sản phẩm để tra cứu tên sản phẩm theo productId nhanh chóng
    private final Map<String, Product> productCache;

    // Dữ liệu bộ nhớ đệm cho 2 Tab
    private final List<Equipment> equipmentListCache;
    private final List<EquipmentLog> auditLogCache;

    // Các thành phần điều khiển Toolbar
    private JTextField txtSearchSerial;
    private JComboBox<String> cboStatusFilter;
    private JButton btnSendMaintenance;
    private JButton btnCompleteMaintenance;
    private JButton btnRefresh;

    // Bảng dữ liệu Tab 1: Danh sách Thiết bị
    private JTable tblEquipment;
    private DefaultTableModel modelEquipment;

    // Bảng dữ liệu Tab 2: Nhật ký Audit Log
    private JTable tblAuditLog;
    private DefaultTableModel modelAuditLog;

    // Tabbed container
    private JTabbedPane tabbedPane;

    private static final DecimalFormat CURRENCY_FORMAT = new DecimalFormat("#,##0 VNĐ");
    private static final DateTimeFormatter DATETIME_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");

    public EquipmentMaintenancePanel() {
        this.equipmentService = new EquipmentService();
        this.productService = new ProductService();
        this.productCache = new HashMap<>();
        this.equipmentListCache = new ArrayList<>();
        this.auditLogCache = new ArrayList<>();

        initComponents();
        loadAllDataAsync();
    }

    private void initComponents() {
        setLayout(new BorderLayout(0, 16));
        setBackground(UIUtils.COLOR_BG_LIGHT);
        setBorder(new EmptyBorder(16, 20, 16, 20));

        // 1. TOP HEADER & TOOLBAR
        JPanel topContainer = new JPanel(new BorderLayout(0, 12));
        topContainer.setOpaque(false);

        // Header Title Panel
        JPanel titlePanel = new JPanel(new GridLayout(2, 1, 0, 4));
        titlePanel.setOpaque(false);

        JLabel lblTitle = new JLabel("QUẢN LÝ BẢO TRÌ & BẢO DƯỠNG THIẾT BỊ");
        lblTitle.setFont(UIUtils.FONT_TITLE);
        lblTitle.setForeground(UIUtils.COLOR_TEXT_MAIN);

        JLabel lblSubtitle = new JLabel("Theo dõi vòng đời phần cứng, gửi/hoàn tất bảo hành và giám sát nhật ký biến động (Audit Log) toàn hệ thống");
        lblSubtitle.setFont(UIUtils.FONT_SUBTITLE);
        lblSubtitle.setForeground(UIUtils.COLOR_TEXT_MUTED);

        titlePanel.add(lblTitle);
        titlePanel.add(lblSubtitle);
        topContainer.add(titlePanel, BorderLayout.NORTH);

        // Toolbar Action Panel
        JPanel toolbarPanel = createToolbarPanel();
        topContainer.add(toolbarPanel, BorderLayout.SOUTH);

        add(topContainer, BorderLayout.NORTH);

        // 2. MAIN CONTENT (JTabbedPane gồm 2 Tabs)
        tabbedPane = new JTabbedPane();
        tabbedPane.setFont(UIUtils.FONT_BODY_BOLD);
        tabbedPane.setBackground(Color.WHITE);

        // Tab 1: Thiết bị cần / đang bảo trì
        JPanel tabEquipmentPanel = createEquipmentTab();
        tabbedPane.addTab("🛠️ Danh Sách Thiết Bị Cần / Đang Bảo Trì", tabEquipmentPanel);

        // Tab 2: Lịch sử Audit Log
        JPanel tabAuditPanel = createAuditLogTab();
        tabbedPane.addTab("📜 Nhật Ký Biến Động & Lịch Sử Thiết Bị (Audit Log)", tabAuditPanel);

        add(tabbedPane, BorderLayout.CENTER);
    }

    /**
     * Tạo Toolbar tìm kiếm, lọc trạng thái và các nút hành động nghiệp vụ.
     */
    private JPanel createToolbarPanel() {
        JPanel toolbar = new JPanel(new BorderLayout(12, 0));
        toolbar.setBackground(Color.WHITE);
        toolbar.setBorder(new CompoundBorder(
                new LineBorder(UIUtils.COLOR_BORDER, 1, true),
                new EmptyBorder(10, 16, 10, 16)
        ));

        // Bên trái: Ô tìm kiếm và ComboBox lọc
        JPanel leftControls = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 0));
        leftControls.setOpaque(false);

        JLabel lblSearch = new JLabel("Tìm kiếm:");
        lblSearch.setFont(UIUtils.FONT_BODY_BOLD);
        lblSearch.setForeground(UIUtils.COLOR_TEXT_MAIN);

        txtSearchSerial = new JTextField(18);
        txtSearchSerial.setFont(UIUtils.FONT_BODY);
        txtSearchSerial.setPreferredSize(new Dimension(220, 36));
        txtSearchSerial.setBorder(new CompoundBorder(
                new LineBorder(UIUtils.COLOR_BORDER, 1, true),
                new EmptyBorder(0, 10, 0, 10)
        ));
        txtSearchSerial.setToolTipText("Nhập Mã Serial hoặc Tên sản phẩm để tìm kiếm...");

        // Bắt sự kiện gõ phím real-time
        txtSearchSerial.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) { applyFilters(); }
            @Override
            public void removeUpdate(DocumentEvent e) { applyFilters(); }
            @Override
            public void changedUpdate(DocumentEvent e) { applyFilters(); }
        });

        JLabel lblFilter = new JLabel("Lọc trạng thái:");
        lblFilter.setFont(UIUtils.FONT_BODY_BOLD);
        lblFilter.setForeground(UIUtils.COLOR_TEXT_MAIN);

        cboStatusFilter = new JComboBox<>(new String[]{
                "Tất Cả Trạng Thái",
                "Đang Bảo Trì (MAINTENANCE)",
                "Sẵn Sàng (AVAILABLE)",
                "Đang Cho Thuê (RENTED)",
                "Hỏng Hóc (BROKEN)"
        });
        cboStatusFilter.setFont(UIUtils.FONT_BODY);
        cboStatusFilter.setPreferredSize(new Dimension(220, 36));
        cboStatusFilter.setBackground(Color.WHITE);
        cboStatusFilter.addActionListener(e -> applyFilters());

        leftControls.add(lblSearch);
        leftControls.add(txtSearchSerial);
        leftControls.add(Box.createHorizontalStrut(10));
        leftControls.add(lblFilter);
        leftControls.add(cboStatusFilter);

        // Bên phải: Các nút Gửi Bảo Trì, Hoàn Tất Bảo Trì, Làm Mới
        JPanel rightButtons = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        rightButtons.setOpaque(false);

        btnSendMaintenance = new JButton("+ Gửi Bảo Trì");
        btnSendMaintenance.setFont(UIUtils.FONT_BODY_BOLD);
        btnSendMaintenance.setBackground(new Color(217, 119, 6)); // Amber 600
        btnSendMaintenance.setForeground(Color.WHITE);
        btnSendMaintenance.setFocusPainted(false);
        btnSendMaintenance.setBorderPainted(false);
        btnSendMaintenance.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btnSendMaintenance.setPreferredSize(new Dimension(140, 36));
        btnSendMaintenance.addActionListener(e -> openSendMaintenanceDialog());

        btnCompleteMaintenance = new JButton("✓ Hoàn Tất Bảo Trì");
        btnCompleteMaintenance.setFont(UIUtils.FONT_BODY_BOLD);
        btnCompleteMaintenance.setBackground(UIUtils.COLOR_SUCCESS);
        btnCompleteMaintenance.setForeground(Color.WHITE);
        btnCompleteMaintenance.setFocusPainted(false);
        btnCompleteMaintenance.setBorderPainted(false);
        btnCompleteMaintenance.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btnCompleteMaintenance.setPreferredSize(new Dimension(160, 36));
        btnCompleteMaintenance.addActionListener(e -> openCompleteMaintenanceDialog());

        btnRefresh = new JButton("Làm Mới");
        btnRefresh.setFont(UIUtils.FONT_BODY);
        btnRefresh.setBackground(Color.WHITE);
        btnRefresh.setForeground(UIUtils.COLOR_TEXT_MAIN);
        btnRefresh.setBorder(new LineBorder(UIUtils.COLOR_BORDER, 1));
        btnRefresh.setFocusPainted(false);
        btnRefresh.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btnRefresh.setPreferredSize(new Dimension(95, 36));
        btnRefresh.addActionListener(e -> loadAllDataAsync());

        rightButtons.add(btnSendMaintenance);
        rightButtons.add(btnCompleteMaintenance);
        rightButtons.add(btnRefresh);

        toolbar.add(leftControls, BorderLayout.WEST);
        toolbar.add(rightButtons, BorderLayout.EAST);
        return toolbar;
    }

    /**
     * Tạo Tab 1: Bảng danh sách thiết bị cần / đang bảo trì.
     */
    private JPanel createEquipmentTab() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(Color.WHITE);
        panel.setBorder(new EmptyBorder(12, 12, 12, 12));

        String[] headers = {
                "STT", "Mã Serial", "Mã SP", "Tên Sản Phẩm",
                "Trạng Thái Hiện Tại", "Tình Trạng / Đơn Vị Phụ Trách", "Hoạt Động"
        };

        modelEquipment = new DefaultTableModel(headers, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        tblEquipment = new JTable(modelEquipment);
        styleTable(tblEquipment);

        // Tùy biến cột Trạng thái với Badge màu sinh động
        tblEquipment.getColumnModel().getColumn(4).setCellRenderer(new StatusBadgeCellRenderer());

        // Căn chỉnh kích thước cột
        tblEquipment.getColumnModel().getColumn(0).setPreferredWidth(50);
        tblEquipment.getColumnModel().getColumn(1).setPreferredWidth(140);
        tblEquipment.getColumnModel().getColumn(2).setPreferredWidth(100);
        tblEquipment.getColumnModel().getColumn(3).setPreferredWidth(230);
        tblEquipment.getColumnModel().getColumn(4).setPreferredWidth(160);
        tblEquipment.getColumnModel().getColumn(5).setPreferredWidth(320);
        tblEquipment.getColumnModel().getColumn(6).setPreferredWidth(90);

        JScrollPane scrollPane = new JScrollPane(tblEquipment);
        scrollPane.setBorder(new LineBorder(UIUtils.COLOR_BORDER, 1));
        scrollPane.getViewport().setBackground(Color.WHITE);

        panel.add(scrollPane, BorderLayout.CENTER);
        return panel;
    }

    /**
     * Tạo Tab 2: Bảng lịch sử biến động Audit Log thiết bị.
     */
    private JPanel createAuditLogTab() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(Color.WHITE);
        panel.setBorder(new EmptyBorder(12, 12, 12, 12));

        String[] headers = {
                "STT", "Thời Gian", "Mã Serial", "Hành Động",
                "Biến Động (Cũ ➔ Mới)", "Đơn Vị Phụ Trách",
                "Chi Phí Dự Kiến", "Chi Phí Thực Tế", "Lý Do / Ghi Chú Sửa Chữa", "Người Thực Hiện"
        };

        modelAuditLog = new DefaultTableModel(headers, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        tblAuditLog = new JTable(modelAuditLog);
        styleTable(tblAuditLog);

        // Render cột Hành động với Badge màu
        tblAuditLog.getColumnModel().getColumn(3).setCellRenderer(new ActionBadgeCellRenderer());

        tblAuditLog.getColumnModel().getColumn(0).setPreferredWidth(45);
        tblAuditLog.getColumnModel().getColumn(1).setPreferredWidth(145);
        tblAuditLog.getColumnModel().getColumn(2).setPreferredWidth(125);
        tblAuditLog.getColumnModel().getColumn(3).setPreferredWidth(160);
        tblAuditLog.getColumnModel().getColumn(4).setPreferredWidth(170);
        tblAuditLog.getColumnModel().getColumn(5).setPreferredWidth(160);
        tblAuditLog.getColumnModel().getColumn(6).setPreferredWidth(110);
        tblAuditLog.getColumnModel().getColumn(7).setPreferredWidth(120);
        tblAuditLog.getColumnModel().getColumn(8).setPreferredWidth(260);
        tblAuditLog.getColumnModel().getColumn(9).setPreferredWidth(110);

        JScrollPane scrollPane = new JScrollPane(tblAuditLog);
        scrollPane.setBorder(new LineBorder(UIUtils.COLOR_BORDER, 1));
        scrollPane.getViewport().setBackground(Color.WHITE);

        panel.add(scrollPane, BorderLayout.CENTER);
        return panel;
    }

    /**
     * Tinh chỉnh phong cách hiển thị JTable hiện đại, phẳng, sạch sẽ.
     */
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
     * Tải toàn bộ dữ liệu CSDL MongoDB bất đồng bộ qua SwingWorker
     * để giao diện không bao giờ bị đơ (Not Responding).
     */
    public void loadAllDataAsync() {
        btnRefresh.setEnabled(false);
        btnRefresh.setText("Đang tải...");

        SwingWorker<Void, Void> worker = new SwingWorker<>() {
            private List<Product> fetchedProducts;
            private List<Equipment> fetchedEquipments;
            private List<EquipmentLog> fetchedLogs;

            @Override
            protected Void doInBackground() {
                // 1. Tải danh sách sản phẩm
                fetchedProducts = productService.getAllProducts();
                // 2. Tải toàn bộ thiết bị phần cứng
                fetchedEquipments = equipmentService.getAllEquipments();
                // 3. Tải toàn bộ nhật ký Audit Log
                fetchedLogs = equipmentService.getEquipmentAuditLogs(null);
                return null;
            }

            @Override
            protected void done() {
                try {
                    get(); // Bắt lỗi nếu có ngoại lệ trong luồng ngầm

                    productCache.clear();
                    if (fetchedProducts != null) {
                        for (Product p : fetchedProducts) {
                            productCache.put(p.getId(), p);
                        }
                    }

                    equipmentListCache.clear();
                    if (fetchedEquipments != null) {
                        equipmentListCache.addAll(fetchedEquipments);
                    }

                    auditLogCache.clear();
                    if (fetchedLogs != null) {
                        auditLogCache.addAll(fetchedLogs);
                    }

                    applyFilters();
                } catch (Exception e) {
                    System.err.println("[EquipmentMaintenancePanel] Lỗi tải dữ liệu: " + e.getMessage());
                } finally {
                    btnRefresh.setEnabled(true);
                    btnRefresh.setText("Làm Mới");
                }
            }
        };

        worker.execute();
    }

    /**
     * Áp dụng bộ lọc tìm kiếm và trạng thái trên dữ liệu cache và đổ lên JTable.
     */
    private void applyFilters() {
        String keyword = txtSearchSerial != null ? txtSearchSerial.getText().trim().toLowerCase() : "";
        String statusFilter = cboStatusFilter != null ? (String) cboStatusFilter.getSelectedItem() : "Tất Cả Trạng Thái";

        // Cập nhật Tab 1: Equipment Table
        modelEquipment.setRowCount(0);
        int stt = 1;
        for (Equipment eq : equipmentListCache) {
            String pName = "Không xác định";
            if (eq.getProductId() != null && productCache.containsKey(eq.getProductId())) {
                pName = productCache.get(eq.getProductId()).getProductName();
            }

            // Kiểm tra bộ lọc từ khóa
            boolean matchKeyword = keyword.isEmpty()
                    || eq.getId().toLowerCase().contains(keyword)
                    || (eq.getProductId() != null && eq.getProductId().toLowerCase().contains(keyword))
                    || pName.toLowerCase().contains(keyword);

            // Kiểm tra bộ lọc trạng thái
            boolean matchStatus = true;
            if ("Đang Bảo Trì (MAINTENANCE)".equals(statusFilter)) {
                matchStatus = Equipment.STATUS_MAINTENANCE.equalsIgnoreCase(eq.getStatus());
            } else if ("Sẵn Sàng (AVAILABLE)".equals(statusFilter)) {
                matchStatus = Equipment.STATUS_AVAILABLE.equalsIgnoreCase(eq.getStatus());
            } else if ("Đang Cho Thuê (RENTED)".equals(statusFilter)) {
                matchStatus = Equipment.STATUS_RENTED.equalsIgnoreCase(eq.getStatus());
            } else if ("Hỏng Hóc (BROKEN)".equals(statusFilter)) {
                matchStatus = Equipment.STATUS_BROKEN.equalsIgnoreCase(eq.getStatus());
            }

            if (matchKeyword && matchStatus) {
                modelEquipment.addRow(new Object[]{
                        stt++,
                        eq.getId(),
                        eq.getProductId() != null ? eq.getProductId() : "",
                        pName,
                        eq.getStatus() != null ? eq.getStatus() : Equipment.STATUS_AVAILABLE,
                        eq.getConditionNote() != null ? eq.getConditionNote() : "",
                        eq.isActive() ? "Hoạt Động" : "Ngưng"
                });
            }
        }

        // Cập nhật Tab 2: Audit Log Table
        modelAuditLog.setRowCount(0);
        int logStt = 1;
        for (EquipmentLog log : auditLogCache) {
            boolean matchKeyword = keyword.isEmpty()
                    || (log.getSerialId() != null && log.getSerialId().toLowerCase().contains(keyword))
                    || (log.getVendor() != null && log.getVendor().toLowerCase().contains(keyword))
                    || (log.getReason() != null && log.getReason().toLowerCase().contains(keyword))
                    || (log.getRepairNotes() != null && log.getRepairNotes().toLowerCase().contains(keyword));

            if (matchKeyword) {
                String timeStr = log.getTimestamp() != null ? log.getTimestamp().format(DATETIME_FORMATTER) : "N/A";
                String transition = (log.getOldStatus() != null ? log.getOldStatus() : "-") + " ➔ " + (log.getNewStatus() != null ? log.getNewStatus() : "-");

                modelAuditLog.addRow(new Object[]{
                        logStt++,
                        timeStr,
                        log.getSerialId() != null ? log.getSerialId() : "",
                        log.getActionType() != null ? log.getActionType() : "",
                        transition,
                        log.getVendor() != null ? log.getVendor() : "",
                        CURRENCY_FORMAT.format(log.getEstimatedCost()),
                        CURRENCY_FORMAT.format(log.getActualCost()),
                        (log.getReason() != null ? log.getReason() : "") + (log.getRepairNotes() != null && !log.getRepairNotes().isEmpty() ? " | " + log.getRepairNotes() : ""),
                        log.getPerformedBy() != null ? log.getPerformedBy() : "SYSTEM"
                });
            }
        }
    }

    /**
     * Mở Dialog "Gửi Thiết Bị Bảo Trì".
     */
    private void openSendMaintenanceDialog() {
        // Lấy danh sách các thiết bị đang AVAILABLE để chọn
        List<Equipment> availableList = new ArrayList<>();
        for (Equipment eq : equipmentListCache) {
            if (Equipment.STATUS_AVAILABLE.equalsIgnoreCase(eq.getStatus())) {
                availableList.add(eq);
            }
        }

        if (availableList.isEmpty()) {
            JOptionPane.showMessageDialog(this,
                    "Hiện không có thiết bị nào đang ở trạng thái SẴN SÀNG (AVAILABLE) để gửi bảo trì!",
                    "Thông báo", JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        JDialog dialog = new JDialog((Frame) SwingUtilities.getWindowAncestor(this), "Phiếu Gửi Thiết Bị Đi Bảo Trì / Sửa Chữa", true);
        dialog.setSize(520, 480);
        dialog.setLocationRelativeTo(this);
        dialog.setResizable(false);

        JPanel panel = new JPanel(new BorderLayout(0, 16));
        panel.setBackground(Color.WHITE);
        panel.setBorder(new EmptyBorder(20, 24, 20, 24));

        // Form Fields Container
        JPanel form = new JPanel(new GridLayout(5, 2, 10, 14));
        form.setOpaque(false);

        // 1. Serial Selector
        JComboBox<String> cboSerial = new JComboBox<>();
        cboSerial.setFont(UIUtils.FONT_BODY);
        cboSerial.setBackground(Color.WHITE);
        for (Equipment eq : availableList) {
            String pName = productCache.containsKey(eq.getProductId()) ? productCache.get(eq.getProductId()).getProductName() : eq.getProductId();
            cboSerial.addItem(eq.getId() + " - " + pName);
        }

        // Nếu người dùng đang chọn 1 dòng trên bảng tblEquipment, ưu tiên chọn dòng đó
        int selectedRow = tblEquipment.getSelectedRow();
        if (selectedRow >= 0) {
            String selectedSerial = (String) modelEquipment.getValueAt(selectedRow, 1);
            for (int i = 0; i < cboSerial.getItemCount(); i++) {
                if (cboSerial.getItemAt(i).startsWith(selectedSerial)) {
                    cboSerial.setSelectedIndex(i);
                    break;
                }
            }
        }

        // 2. Vendor
        JTextField txtVendor = new JTextField("Trung Tâm Bảo Hành Apple / Dell");
        txtVendor.setFont(UIUtils.FONT_BODY);

        // 3. Reason
        JTextField txtReason = new JTextField();
        txtReason.setFont(UIUtils.FONT_BODY);

        // 4. Estimated Cost
        JTextField txtEstimatedCost = new JTextField("0");
        txtEstimatedCost.setFont(UIUtils.FONT_BODY);

        // 5. Expected Completion Date
        JTextField txtExpectedDays = new JTextField("7"); // Mặc định 7 ngày
        txtExpectedDays.setFont(UIUtils.FONT_BODY);

        form.add(new JLabel("Chọn Thiết Bị (AVAILABLE):*"));
        form.add(cboSerial);
        form.add(new JLabel("Đơn Vị Bảo Hành / Sửa Chữa:*"));
        form.add(txtVendor);
        form.add(new JLabel("Lý Do / Triệu Chứng Hỏng:*"));
        form.add(txtReason);
        form.add(new JLabel("Chi Phí Dự Kiến (VNĐ):"));
        form.add(txtEstimatedCost);
        form.add(new JLabel("Số Ngày Dự Kiến Hoàn Tất:"));
        form.add(txtExpectedDays);

        panel.add(form, BorderLayout.CENTER);

        // Bottom Actions
        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 12, 0));
        btnPanel.setOpaque(false);

        JButton btnCancel = new JButton("Hủy Bỏ");
        btnCancel.setFont(UIUtils.FONT_BODY);
        btnCancel.addActionListener(e -> dialog.dispose());

        JButton btnSubmit = new JButton("Xác Nhận Gửi Bảo Trì");
        btnSubmit.setFont(UIUtils.FONT_BODY_BOLD);
        btnSubmit.setBackground(new Color(217, 119, 6)); // Amber
        btnSubmit.setForeground(Color.WHITE);
        btnSubmit.setFocusPainted(false);
        btnSubmit.setCursor(new Cursor(Cursor.HAND_CURSOR));

        btnSubmit.addActionListener(e -> {
            String selectedItem = (String) cboSerial.getSelectedItem();
            if (selectedItem == null) return;
            String serialId = selectedItem.split(" - ")[0].trim();
            String vendor = txtVendor.getText().trim();
            String reason = txtReason.getText().trim();
            String costStr = txtEstimatedCost.getText().trim().replace(",", "").replace(".", "");
            String daysStr = txtExpectedDays.getText().trim();

            if (vendor.isEmpty()) {
                JOptionPane.showMessageDialog(dialog, "Vui lòng nhập tên Đơn vị tiếp nhận bảo hành!", "Cảnh báo", JOptionPane.WARNING_MESSAGE);
                return;
            }

            if (reason.isEmpty()) {
                JOptionPane.showMessageDialog(dialog, "Vui lòng nhập lý do hỏng hóc hoặc nội dung bảo trì!", "Cảnh báo", JOptionPane.WARNING_MESSAGE);
                return;
            }

            double estimatedCost;
            try {
                estimatedCost = Double.parseDouble(costStr);
                if (estimatedCost < 0) throw new NumberFormatException();
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(dialog, "Chi phí dự kiến phải là số không âm!", "Lỗi định dạng", JOptionPane.ERROR_MESSAGE);
                return;
            }

            int days = 7;
            try {
                days = Integer.parseInt(daysStr);
            } catch (Exception ignored) {}
            LocalDate expectedDate = LocalDate.now().plusDays(Math.max(1, days));

            // Thực thi gửi bảo trì ngầm qua SwingWorker
            btnSubmit.setEnabled(false);
            btnSubmit.setText("Đang ghi nhận...");

            SwingWorker<Boolean, Void> worker = new SwingWorker<>() {
                @Override
                protected Boolean doInBackground() {
                    return equipmentService.sendToMaintenance(serialId, vendor, reason, estimatedCost, expectedDate);
                }

                @Override
                protected void done() {
                    try {
                        boolean ok = get();
                        if (ok) {
                            JOptionPane.showMessageDialog(EquipmentMaintenancePanel.this,
                                    "Đã chuyển thiết bị [" + serialId + "] sang trạng thái ĐANG BẢO TRÌ thành công!\nPhiếu Audit Log đã được lưu vào MongoDB.",
                                    "Thành công", JOptionPane.INFORMATION_MESSAGE);
                            dialog.dispose();
                            loadAllDataAsync();
                        } else {
                            JOptionPane.showMessageDialog(dialog, "Không thể chuyển trạng thái thiết bị!", "Thất bại", JOptionPane.ERROR_MESSAGE);
                            btnSubmit.setEnabled(true);
                            btnSubmit.setText("Xác Nhận Gửi Bảo Trì");
                        }
                    } catch (Exception ex) {
                        JOptionPane.showMessageDialog(dialog, "Lỗi: " + ex.getMessage(), "Lỗi hệ thống", JOptionPane.ERROR_MESSAGE);
                        btnSubmit.setEnabled(true);
                        btnSubmit.setText("Xác Nhận Gửi Bảo Trì");
                    }
                }
            };
            worker.execute();
        });

        btnPanel.add(btnCancel);
        btnPanel.add(btnSubmit);
        panel.add(btnPanel, BorderLayout.SOUTH);

        dialog.setContentPane(panel);
        dialog.setVisible(true);
    }

    /**
     * Mở Dialog "Hoàn Tất Bảo Trì" để đưa thiết bị trở về trạng thái AVAILABLE.
     */
    private void openCompleteMaintenanceDialog() {
        // Lấy danh sách các thiết bị đang MAINTENANCE
        List<Equipment> maintenanceList = new ArrayList<>();
        for (Equipment eq : equipmentListCache) {
            if (Equipment.STATUS_MAINTENANCE.equalsIgnoreCase(eq.getStatus())) {
                maintenanceList.add(eq);
            }
        }

        if (maintenanceList.isEmpty()) {
            JOptionPane.showMessageDialog(this,
                    "Hiện không có thiết bị nào đang trong trạng thái ĐANG BẢO TRÌ (MAINTENANCE)!",
                    "Thông báo", JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        JDialog dialog = new JDialog((Frame) SwingUtilities.getWindowAncestor(this), "Phiếu Nghiệm Thu & Hoàn Tất Sửa Chữa Thiết Bị", true);
        dialog.setSize(520, 460);
        dialog.setLocationRelativeTo(this);
        dialog.setResizable(false);

        JPanel panel = new JPanel(new BorderLayout(0, 16));
        panel.setBackground(Color.WHITE);
        panel.setBorder(new EmptyBorder(20, 24, 20, 24));

        JPanel form = new JPanel(new GridLayout(4, 2, 10, 14));
        form.setOpaque(false);

        // 1. Selector Serial
        JComboBox<String> cboSerial = new JComboBox<>();
        cboSerial.setFont(UIUtils.FONT_BODY);
        cboSerial.setBackground(Color.WHITE);
        for (Equipment eq : maintenanceList) {
            String pName = productCache.containsKey(eq.getProductId()) ? productCache.get(eq.getProductId()).getProductName() : eq.getProductId();
            cboSerial.addItem(eq.getId() + " - " + pName);
        }

        // Chọn dòng đang active nếu máy đó đang bảo trì
        int selectedRow = tblEquipment.getSelectedRow();
        if (selectedRow >= 0) {
            String selectedSerial = (String) modelEquipment.getValueAt(selectedRow, 1);
            for (int i = 0; i < cboSerial.getItemCount(); i++) {
                if (cboSerial.getItemAt(i).startsWith(selectedSerial)) {
                    cboSerial.setSelectedIndex(i);
                    break;
                }
            }
        }

        // 2. Chi phí thực tế
        JTextField txtActualCost = new JTextField("0");
        txtActualCost.setFont(UIUtils.FONT_BODY);

        // 3. Ghi chú kết quả sửa chữa
        JTextField txtRepairNotes = new JTextField("Đã thay linh kiện, test hoạt động ổn định");
        txtRepairNotes.setFont(UIUtils.FONT_BODY);

        form.add(new JLabel("Chọn Thiết Bị Đang Sửa:*"));
        form.add(cboSerial);
        form.add(new JLabel("Chi Phí Thực Tế (VNĐ):*"));
        form.add(txtActualCost);
        form.add(new JLabel("Kết Quả & Ghi Chú Nghiệm Thu:*"));
        form.add(txtRepairNotes);
        form.add(new JLabel("Trạng Thái Sau Xử Lý:"));
        JLabel lblTarget = new JLabel("AVAILABLE (Đưa về kho)");
        lblTarget.setFont(UIUtils.FONT_BODY_BOLD);
        lblTarget.setForeground(UIUtils.COLOR_SUCCESS);
        form.add(lblTarget);

        panel.add(form, BorderLayout.CENTER);

        // Bottom Actions
        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 12, 0));
        btnPanel.setOpaque(false);

        JButton btnCancel = new JButton("Hủy Bỏ");
        btnCancel.setFont(UIUtils.FONT_BODY);
        btnCancel.addActionListener(e -> dialog.dispose());

        JButton btnConfirm = new JButton("✓ Xác Nhận Đưa Về Kho");
        btnConfirm.setFont(UIUtils.FONT_BODY_BOLD);
        btnConfirm.setBackground(UIUtils.COLOR_SUCCESS);
        btnConfirm.setForeground(Color.WHITE);
        btnConfirm.setFocusPainted(false);
        btnConfirm.setCursor(new Cursor(Cursor.HAND_CURSOR));

        btnConfirm.addActionListener(e -> {
            String selectedItem = (String) cboSerial.getSelectedItem();
            if (selectedItem == null) return;
            String serialId = selectedItem.split(" - ")[0].trim();
            String costStr = txtActualCost.getText().trim().replace(",", "").replace(".", "");
            String repairNotes = txtRepairNotes.getText().trim();

            double actualCost;
            try {
                actualCost = Double.parseDouble(costStr);
                if (actualCost < 0) throw new NumberFormatException();
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(dialog, "Chi phí thực tế phải là số không âm!", "Lỗi", JOptionPane.ERROR_MESSAGE);
                return;
            }

            if (repairNotes.isEmpty()) {
                JOptionPane.showMessageDialog(dialog, "Vui lòng nhập ghi chú nghiệm thu phần cứng!", "Cảnh báo", JOptionPane.WARNING_MESSAGE);
                return;
            }

            btnConfirm.setEnabled(false);
            btnConfirm.setText("Đang xử lý...");

            SwingWorker<Boolean, Void> worker = new SwingWorker<>() {
                @Override
                protected Boolean doInBackground() {
                    return equipmentService.completeMaintenance(serialId, actualCost, repairNotes);
                }

                @Override
                protected void done() {
                    try {
                        boolean ok = get();
                        if (ok) {
                            JOptionPane.showMessageDialog(EquipmentMaintenancePanel.this,
                                    "Thiết bị [" + serialId + "] đã hoàn tất sửa chữa và sẵn sàng phục vụ tại kho!",
                                    "Thành công", JOptionPane.INFORMATION_MESSAGE);
                            dialog.dispose();
                            loadAllDataAsync();
                        } else {
                            JOptionPane.showMessageDialog(dialog, "Không thể hoàn tất bảo trì cho thiết bị này!", "Thất bại", JOptionPane.ERROR_MESSAGE);
                            btnConfirm.setEnabled(true);
                            btnConfirm.setText("✓ Xác Nhận Đưa Về Kho");
                        }
                    } catch (Exception ex) {
                        JOptionPane.showMessageDialog(dialog, "Lỗi: " + ex.getMessage(), "Lỗi hệ thống", JOptionPane.ERROR_MESSAGE);
                        btnConfirm.setEnabled(true);
                        btnConfirm.setText("✓ Xác Nhận Đưa Về Kho");
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

    // ================= CUSTOM TABLE CELL RENDERERS =================

    /**
     * Renderer hiển thị Huy hiệu (Badge) màu sắc cho trạng thái thiết bị.
     */
    private static class StatusBadgeCellRenderer extends DefaultTableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(JTable table, Object value,
                                                       boolean isSelected, boolean hasFocus, int row, int column) {
            JLabel label = (JLabel) super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
            label.setHorizontalAlignment(SwingConstants.CENTER);
            label.setFont(new Font("Segoe UI", Font.BOLD, 12));

            String status = value != null ? value.toString() : "";
            if (Equipment.STATUS_MAINTENANCE.equalsIgnoreCase(status)) {
                label.setText(" ĐANG BẢO TRÌ ");
                label.setBackground(new Color(254, 243, 199)); // Amber 100
                label.setForeground(new Color(180, 83, 9));    // Amber 700
            } else if (Equipment.STATUS_AVAILABLE.equalsIgnoreCase(status)) {
                label.setText(" SẴN SÀNG ");
                label.setBackground(new Color(220, 252, 231)); // Green 100
                label.setForeground(new Color(21, 128, 61));   // Green 700
            } else if (Equipment.STATUS_RENTED.equalsIgnoreCase(status)) {
                label.setText(" ĐANG CHO THUÊ ");
                label.setBackground(new Color(224, 231, 255)); // Indigo 100
                label.setForeground(new Color(67, 56, 202));   // Indigo 700
            } else if (Equipment.STATUS_BROKEN.equalsIgnoreCase(status)) {
                label.setText(" HỎNG HÓC ");
                label.setBackground(new Color(254, 226, 226)); // Red 100
                label.setForeground(new Color(185, 28, 28));   // Red 700
            } else if (Equipment.STATUS_SOLD.equalsIgnoreCase(status)) {
                label.setText(" ĐÃ BÁN ");
                label.setBackground(new Color(241, 245, 249)); // Slate 100
                label.setForeground(new Color(71, 85, 105));   // Slate 600
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
     * Renderer hiển thị Badge màu cho Hành động trong bảng Audit Log.
     */
    private static class ActionBadgeCellRenderer extends DefaultTableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(JTable table, Object value,
                                                       boolean isSelected, boolean hasFocus, int row, int column) {
            JLabel label = (JLabel) super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
            label.setHorizontalAlignment(SwingConstants.CENTER);
            label.setFont(new Font("Segoe UI", Font.BOLD, 11));

            String action = value != null ? value.toString() : "";
            if (EquipmentLog.ACTION_SEND_MAINTENANCE.equalsIgnoreCase(action)) {
                label.setText(" GỬI BẢO TRÌ ");
                label.setBackground(new Color(254, 243, 199)); // Amber 100
                label.setForeground(new Color(180, 83, 9));    // Amber 700
            } else if (EquipmentLog.ACTION_COMPLETE_MAINTENANCE.equalsIgnoreCase(action)) {
                label.setText(" HOÀN TẤT BẢO TRÌ ");
                label.setBackground(new Color(220, 252, 231)); // Green 100
                label.setForeground(new Color(21, 128, 61));   // Green 700
            } else {
                label.setText(" ĐỔI TRẠNG THÁI ");
                label.setBackground(new Color(224, 242, 254)); // Sky 100
                label.setForeground(new Color(3, 105, 161));   // Sky 700
            }

            label.setOpaque(true);
            return label;
        }
    }
}