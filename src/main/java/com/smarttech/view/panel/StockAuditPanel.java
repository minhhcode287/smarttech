package com.smarttech.view.panel;

import com.smarttech.model.Category;
import com.smarttech.model.Equipment;
import com.smarttech.model.Product;
import com.smarttech.model.StockAudit;
import com.smarttech.model.StockAuditDetail;
import com.smarttech.service.CategoryService;
import com.smarttech.service.EquipmentService;
import com.smarttech.service.ProductService;
import com.smarttech.service.StockAuditService;
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
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Phân Hệ Kiểm Kê Kho, Cân Bằng Tồn Kho & Xử Lý Sự Cố Phần Cứng (StockAuditPanel).
 * Cung cấp giải pháp đối chiếu tồn kho thực tế và tự động cân bằng trạng thái thiết bị trong CSDL:
 * - Tab 1: Thực Hiện Kiểm Kê Kho Tải Dữ Liệu Real-Time (Đối chiếu từng Serial, chọn trạng thái thực tế và chốt cân bằng)
 * - Tab 2: Lịch Sử Đợt Kiểm Kê & Nhật Ký Điều Chỉnh (Tra cứu các đợt kiểm kê, xem chi tiết các thiết bị bị lệch trạng thái)
 *
 * @author Senior Java Developer & Software Architect
 * @project SmartTech
 */
public class StockAuditPanel extends JPanel {

    private final StockAuditService auditService;
    private final EquipmentService equipmentService;
    private final ProductService productService;
    private final CategoryService categoryService;

    // Cache dữ liệu
    private final List<StockAuditDetail> currentAuditDetails = new ArrayList<>();
    private final List<StockAudit> auditHistoryCache = new ArrayList<>();
    private final Map<String, Product> productMap = new HashMap<>();

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    // Các trạng thái thực tế người kiểm kê có thể lựa chọn (Đã bổ sung SOLD)
    public static final String[] ACTUAL_STATUS_OPTIONS = {
            Equipment.STATUS_AVAILABLE,
            Equipment.STATUS_RENTED,
            Equipment.STATUS_SOLD,
            "NEED_MAINTENANCE",
            "DAMAGED",
            "LOST"
    };

    // ================= COMPONENTS TAB 1 =================
    private JTextField txtAuditName;
    private JComboBox<String> cboCategoryFilter;
    private JTable tblAuditGrid;
    private DefaultTableModel modelAuditGrid;
    private JLabel lblTotalSystem;
    private JLabel lblTotalMatched;
    private JLabel lblTotalDiscrepancy;
    private JTextField txtAuditNote;
    private JButton btnExecuteAudit;
    private JButton btnReloadEquipment;

    // ================= COMPONENTS TAB 2 =================
    private JTextField txtSearchHistory;
    private JTable tblHistory;
    private DefaultTableModel modelHistory;

    public StockAuditPanel() {
        this.auditService = new StockAuditService();
        this.equipmentService = new EquipmentService();
        this.productService = new ProductService();
        this.categoryService = new CategoryService();

        initComponents();
        loadCategoriesAndInitialDataAsync();
    }

    private void initComponents() {
        setLayout(new BorderLayout(0, 16));
        setBackground(UIUtils.COLOR_BG_LIGHT);
        setBorder(new EmptyBorder(16, 20, 16, 20));

        // 1. TOP HEADER BAR
        JPanel topHeader = createTopHeaderBar();
        add(topHeader, BorderLayout.NORTH);

        // 2. MAIN TABBED PANE
        JTabbedPane tabbedPane = new JTabbedPane();
        tabbedPane.setFont(UIUtils.FONT_BODY_BOLD);
        tabbedPane.setBackground(Color.WHITE);

        tabbedPane.addTab("📋  Thực Hiện Kiểm Kê Kho Real-Time", createAuditExecutionTab());
        tabbedPane.addTab("📜  Lịch Sử Đợt Kiểm Kê & Sai Lệch", createAuditHistoryTab());

        add(tabbedPane, BorderLayout.CENTER);
    }

    private JPanel createTopHeaderBar() {
        JPanel header = new JPanel(new BorderLayout());
        header.setOpaque(false);

        JPanel titlePanel = new JPanel(new GridLayout(2, 1, 0, 4));
        titlePanel.setOpaque(false);

        JLabel lblTitle = new JLabel("QUẢN LÝ KIỂM KÊ KHO & CÂN BẰNG TỒN KHO THIẾT BỊ");
        lblTitle.setFont(UIUtils.FONT_TITLE);
        lblTitle.setForeground(UIUtils.COLOR_PRIMARY);

        JLabel lblSubtitle = new JLabel("Kiểm đếm thiết bị thực tế, phát hiện sai lệch phần cứng (hư hỏng, thất thoát) và tự động cân bằng trạng thái kho CSDL.");
        lblSubtitle.setFont(UIUtils.FONT_SUBTITLE);
        lblSubtitle.setForeground(UIUtils.COLOR_TEXT_MUTED);

        titlePanel.add(lblTitle);
        titlePanel.add(lblSubtitle);

        header.add(titlePanel, BorderLayout.WEST);
        return header;
    }

    // =========================================================================
    // TAB 1: THỰC HIỆN KIỂM KÊ KHO TẢI DỮ LIỆU REAL-TIME
    // =========================================================================

    private JPanel createAuditExecutionTab() {
        JPanel panel = new JPanel(new BorderLayout(0, 12));
        panel.setBackground(Color.WHITE);
        panel.setBorder(new EmptyBorder(14, 16, 14, 16));

        // 1. Top Control Bar
        JPanel topBar = new JPanel(new BorderLayout(12, 0));
        topBar.setOpaque(false);

        JPanel leftInputs = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        leftInputs.setOpaque(false);

        JLabel lblName = new JLabel("Tên đợt kiểm kê (*):");
        lblName.setFont(UIUtils.FONT_BODY_BOLD);

        txtAuditName = UIUtils.createTextField();
        txtAuditName.setPreferredSize(new Dimension(300, 36));
        txtAuditName.setText("Kiểm kê định kỳ Kho Thiết Bị Tháng " + LocalDate.now().getMonthValue() + "/" + LocalDate.now().getYear());

        JLabel lblCat = new JLabel("Lọc danh mục:");
        lblCat.setFont(UIUtils.FONT_BODY_BOLD);

        cboCategoryFilter = new JComboBox<>(new String[]{"-- Tất cả danh mục --"});
        cboCategoryFilter.setFont(UIUtils.FONT_BODY);
        cboCategoryFilter.setPreferredSize(new Dimension(200, 36));
        cboCategoryFilter.addActionListener(e -> filterGridByCategory());

        leftInputs.add(lblName);
        leftInputs.add(txtAuditName);
        leftInputs.add(lblCat);
        leftInputs.add(cboCategoryFilter);

        JPanel rightActions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        rightActions.setOpaque(false);

        btnReloadEquipment = new JButton("🔄  Tải Lại Kho CSDL");
        btnReloadEquipment.setFont(UIUtils.FONT_BODY_BOLD);
        btnReloadEquipment.setBackground(new Color(241, 245, 249));
        btnReloadEquipment.setForeground(UIUtils.COLOR_PRIMARY);
        btnReloadEquipment.setFocusPainted(false);
        btnReloadEquipment.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btnReloadEquipment.setPreferredSize(new Dimension(180, 36));
        btnReloadEquipment.addActionListener(e -> loadEquipmentsFromDatabaseAsync());

        rightActions.add(btnReloadEquipment);

        topBar.add(leftInputs, BorderLayout.WEST);
        topBar.add(rightActions, BorderLayout.EAST);

        // 2. Center Audit Grid
        String[] columns = {"STT", "Mã Serial Phần Cứng", "Tên Sản Phẩm", "Trạng Thái CSDL", "Trạng Thái Thực Tế (*)", "Ghi Chú Sai Lệch / Sự Cố"};
        modelAuditGrid = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                // Chỉ cho phép sửa cột "Trạng Thái Thực Tế" (cột 4) và "Ghi Chú Sai Lệch" (cột 5)
                return column == 4 || column == 5;
            }
        };
        tblAuditGrid = new JTable(modelAuditGrid);
        styleAuditGrid(tblAuditGrid);

        // Đặt JComboBox Cell Editor cho cột "Trạng Thái Thực Tế" (cột index 4)
        JComboBox<String> cboStatusEditor = new JComboBox<>(ACTUAL_STATUS_OPTIONS);
        cboStatusEditor.setFont(UIUtils.FONT_BODY);
        tblAuditGrid.getColumnModel().getColumn(4).setCellEditor(new DefaultCellEditor(cboStatusEditor));

        // Lắng nghe thay đổi dữ liệu trên bảng để cập nhật thẻ thống kê
        modelAuditGrid.addTableModelListener(e -> updateAuditStatisticsCards());

        JScrollPane scrollPane = new JScrollPane(tblAuditGrid);
        scrollPane.setBorder(BorderFactory.createLineBorder(UIUtils.COLOR_BORDER, 1));

        // 3. Bottom Summary & Confirmation Panel
        JPanel bottomPanel = new JPanel(new BorderLayout(16, 12));
        bottomPanel.setBackground(new Color(248, 250, 252));
        bottomPanel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(UIUtils.COLOR_BORDER, 1),
                new EmptyBorder(14, 18, 14, 18)
        ));

        // 3 Thẻ thống kê thời gian thực
        JPanel statsCardsPanel = new JPanel(new GridLayout(1, 3, 14, 0));
        statsCardsPanel.setOpaque(false);

        lblTotalSystem = createStatCard("TỔNG MÁY HỆ THỐNG", "0 máy", UIUtils.COLOR_PRIMARY);
        lblTotalMatched = createStatCard("MÁY KHỚP TRẠNG THÁI", "0 máy", UIUtils.COLOR_SUCCESS);
        lblTotalDiscrepancy = createStatCard("MÁY SAI LỆCH CẦN CÂN BẰNG", "0 máy", UIUtils.COLOR_DANGER);

        statsCardsPanel.add(lblTotalSystem.getParent());
        statsCardsPanel.add(lblTotalMatched.getParent());
        statsCardsPanel.add(lblTotalDiscrepancy.getParent());

        // Thanh thao tác xác nhận
        JPanel actionRow = new JPanel(new BorderLayout(14, 0));
        actionRow.setOpaque(false);

        JPanel notePanel = new JPanel(new BorderLayout(6, 0));
        notePanel.setOpaque(false);
        JLabel lblNote = new JLabel("Ghi chú tổng kết:");
        lblNote.setFont(UIUtils.FONT_BODY_BOLD);
        txtAuditNote = UIUtils.createTextField();
        txtAuditNote.setText("Đợt kiểm kê định kỳ thiết bị kho SmartTech");
        notePanel.add(lblNote, BorderLayout.WEST);
        notePanel.add(txtAuditNote, BorderLayout.CENTER);

        // Nút bấm đã sửa màu chữ màu trắng đậm rõ nét
        // Khởi tạo nút xác nhận chốt kiểm kê với Custom Painting (Bảo đảm 100% màu xanh lá đậm & chữ trắng sắc nét)
        btnExecuteAudit = new JButton("⚡  XÁC NHẬN CHỐT KIỂM KÊ & CÂN BẰNG KHO") {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                if (isEnabled()) {
                    g2.setColor(getModel().isPressed() ? new Color(21, 128, 61) : new Color(22, 163, 74));
                } else {
                    g2.setColor(new Color(203, 213, 225));
                }
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 8, 8);
                g2.dispose();
                super.paintComponent(g);
            }
        };
        btnExecuteAudit.setFont(UIUtils.FONT_BODY_BOLD);
        btnExecuteAudit.setForeground(Color.WHITE);
        btnExecuteAudit.setContentAreaFilled(false);
        btnExecuteAudit.setFocusPainted(false);
        btnExecuteAudit.setBorderPainted(false);
        btnExecuteAudit.setOpaque(false);
        btnExecuteAudit.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btnExecuteAudit.setPreferredSize(new Dimension(360, 42));
        btnExecuteAudit.addActionListener(e -> executeConfirmStockAudit());

        actionRow.add(notePanel, BorderLayout.CENTER);
        actionRow.add(btnExecuteAudit, BorderLayout.EAST);

        bottomPanel.add(statsCardsPanel, BorderLayout.NORTH);
        bottomPanel.add(actionRow, BorderLayout.SOUTH);

        panel.add(topBar, BorderLayout.NORTH);
        panel.add(scrollPane, BorderLayout.CENTER);
        panel.add(bottomPanel, BorderLayout.SOUTH);
        return panel;
    }

    // =========================================================================
    // TAB 2: LỊCH SỬ ĐỢT KIỂM KÊ & NHẬT KÝ ĐIỀU CHỈNH
    // =========================================================================

    private JPanel createAuditHistoryTab() {
        JPanel panel = new JPanel(new BorderLayout(0, 12));
        panel.setBackground(Color.WHITE);
        panel.setBorder(new EmptyBorder(14, 16, 14, 16));

        // Toolbar
        JPanel toolbar = new JPanel(new BorderLayout(12, 0));
        toolbar.setOpaque(false);

        JPanel leftSearch = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        leftSearch.setOpaque(false);

        JLabel lblSearch = new JLabel("Tìm đợt kiểm kê:");
        lblSearch.setFont(UIUtils.FONT_BODY_BOLD);

        txtSearchHistory = UIUtils.createTextField();
        txtSearchHistory.setPreferredSize(new Dimension(280, 36));
        txtSearchHistory.setToolTipText("Tìm theo Mã phiếu, Tên đợt kiểm kê, Người kiểm...");
        txtSearchHistory.getDocument().addDocumentListener(new DocumentListener() {
            @Override public void insertUpdate(DocumentEvent e) { filterHistoryAsync(); }
            @Override public void removeUpdate(DocumentEvent e) { filterHistoryAsync(); }
            @Override public void changedUpdate(DocumentEvent e) { filterHistoryAsync(); }
        });

        JButton btnRefreshHistory = new JButton("🔄 Làm Mới");
        btnRefreshHistory.setFont(UIUtils.FONT_BODY);
        btnRefreshHistory.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btnRefreshHistory.addActionListener(e -> loadAuditHistoryAsync());

        leftSearch.add(lblSearch);
        leftSearch.add(txtSearchHistory);
        leftSearch.add(btnRefreshHistory);

        JPanel rightActions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        rightActions.setOpaque(false);

        JButton btnViewDiscrepancy = new JButton("👁️  Xem Chi Tiết Sai Lệch & Cân Bằng");
        btnViewDiscrepancy.setFont(UIUtils.FONT_BODY_BOLD);
        btnViewDiscrepancy.setBackground(UIUtils.COLOR_PRIMARY);
        btnViewDiscrepancy.setForeground(Color.WHITE);
        btnViewDiscrepancy.setFocusPainted(false);
        btnViewDiscrepancy.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btnViewDiscrepancy.setPreferredSize(new Dimension(280, 36));
        btnViewDiscrepancy.addActionListener(e -> showSelectedAuditDetailDialog());

        rightActions.add(btnViewDiscrepancy);

        toolbar.add(leftSearch, BorderLayout.WEST);
        toolbar.add(rightActions, BorderLayout.EAST);

        // Bảng lịch sử
        String[] columns = {"Mã Phiếu Audit", "Tên Đợt Kiểm Kê", "Ngày Kiểm", "Người Lập", "Tổng Máy", "Sai Lệch Đã Cân Bằng", "Trạng Thái"};
        modelHistory = new DefaultTableModel(columns, 0) {
            @Override public boolean isCellEditable(int row, int col) { return false; }
        };
        tblHistory = new JTable(modelHistory);
        styleGeneralTable(tblHistory);
        tblHistory.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    showSelectedAuditDetailDialog();
                }
            }
        });

        JScrollPane scrollPane = new JScrollPane(tblHistory);
        scrollPane.setBorder(BorderFactory.createLineBorder(UIUtils.COLOR_BORDER, 1));

        panel.add(toolbar, BorderLayout.NORTH);
        panel.add(scrollPane, BorderLayout.CENTER);
        return panel;
    }

    // =========================================================================
    // XỬ LÝ NGHIỆP VỤ & TƯƠNG TÁC GIAO DIỆN TAB 1
    // =========================================================================

    private void loadEquipmentsFromDatabaseAsync() {
        btnReloadEquipment.setEnabled(false);
        setCursor(new Cursor(Cursor.WAIT_CURSOR));

        new SwingWorker<List<StockAuditDetail>, Void>() {
            @Override
            protected List<StockAuditDetail> doInBackground() {
                List<StockAuditDetail> details = new ArrayList<>();
                List<Equipment> allEquipments = equipmentService.getAllEquipments();

                // Đảm bảo cache productMap có sẵn
                if (productMap.isEmpty()) {
                    for (Product p : productService.getAllProducts()) {
                        productMap.put(p.getId(), p);
                    }
                }

                for (Equipment eq : allEquipments) {
                    Product prod = productMap.get(eq.getProductId());
                    String prodName = prod != null ? prod.getProductName() : "Thiết Bị [" + eq.getProductId() + "]";
                    StockAuditDetail detail = new StockAuditDetail(
                            eq.getId(),
                            eq.getProductId(),
                            prodName,
                            eq.getStatus(),
                            eq.getStatus(), // Mặc định trạng thái thực tế bằng trạng thái CSDL
                            eq.getConditionNote() != null ? eq.getConditionNote() : ""
                    );
                    details.add(detail);
                }
                return details;
            }

            @Override
            protected void done() {
                btnReloadEquipment.setEnabled(true);
                setCursor(new Cursor(Cursor.DEFAULT_CURSOR));
                try {
                    List<StockAuditDetail> list = get();
                    currentAuditDetails.clear();
                    currentAuditDetails.addAll(list);
                    renderAuditGrid(currentAuditDetails);
                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(StockAuditPanel.this, "Lỗi khi tải dữ liệu thiết bị: " + ex.getMessage(), "Lỗi", JOptionPane.ERROR_MESSAGE);
                }
            }
        }.execute();
    }

    private void filterGridByCategory() {
        int selectedIndex = cboCategoryFilter.getSelectedIndex();
        if (selectedIndex <= 0) {
            renderAuditGrid(currentAuditDetails);
            return;
        }

        String selectedCatName = (String) cboCategoryFilter.getSelectedItem();
        List<StockAuditDetail> filtered = new ArrayList<>();
        for (StockAuditDetail d : currentAuditDetails) {
            Product prod = productMap.get(d.getProductId());
            if (prod != null) {
                Category cat = categoryService.getCategoryById(prod.getCategoryId());
                if (cat != null && cat.getCategoryName().equalsIgnoreCase(selectedCatName)) {
                    filtered.add(d);
                }
            }
        }
        renderAuditGrid(filtered);
    }

    private void renderAuditGrid(List<StockAuditDetail> list) {
        modelAuditGrid.setRowCount(0);
        for (int i = 0; i < list.size(); i++) {
            StockAuditDetail d = list.get(i);
            modelAuditGrid.addRow(new Object[]{
                    i + 1,
                    d.getSerialId(),
                    d.getProductName(),
                    d.getSystemStatus(),
                    d.getActualStatus(),
                    d.getNote()
            });
        }
        updateAuditStatisticsCards();
    }

    private void updateAuditStatisticsCards() {
        int total = modelAuditGrid.getRowCount();
        int discrepancy = 0;

        for (int i = 0; i < total; i++) {
            String sysStatus = (String) modelAuditGrid.getValueAt(i, 3);
            String actStatus = (String) modelAuditGrid.getValueAt(i, 4);
            if (sysStatus != null && actStatus != null && !sysStatus.equalsIgnoreCase(actStatus)) {
                discrepancy++;
            }
        }

        int matched = total - discrepancy;

        lblTotalSystem.setText(total + " máy");
        lblTotalMatched.setText(matched + " máy");
        lblTotalDiscrepancy.setText(discrepancy + " máy");
    }

    private void executeConfirmStockAudit() {
        int rowCount = modelAuditGrid.getRowCount();
        if (rowCount == 0) {
            JOptionPane.showMessageDialog(this, "Chưa có dữ liệu thiết bị để kiểm kê! Hãy bấm 'Tải Lại Kho CSDL'.", "Cảnh báo", JOptionPane.WARNING_MESSAGE);
            return;
        }

        String auditName = txtAuditName.getText().trim();
        if (auditName.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Vui lòng nhập Tên đợt kiểm kê!", "Cảnh báo", JOptionPane.WARNING_MESSAGE);
            txtAuditName.requestFocus();
            return;
        }

        // Đồng bộ dữ liệu từ bảng vào danh sách StockAuditDetail
        List<StockAuditDetail> detailsToSave = new ArrayList<>();
        int discrepancyCount = 0;

        for (int i = 0; i < rowCount; i++) {
            String serialId = (String) modelAuditGrid.getValueAt(i, 1);
            String prodName = (String) modelAuditGrid.getValueAt(i, 2);
            String sysStatus = (String) modelAuditGrid.getValueAt(i, 3);
            String actStatus = (String) modelAuditGrid.getValueAt(i, 4);
            String note = (String) modelAuditGrid.getValueAt(i, 5);

            String prodId = "";
            for (StockAuditDetail d : currentAuditDetails) {
                if (d.getSerialId().equalsIgnoreCase(serialId)) {
                    prodId = d.getProductId();
                    break;
                }
            }

            StockAuditDetail detail = new StockAuditDetail(serialId, prodId, prodName, sysStatus, actStatus, note);
            if (detail.hasDiscrepancy()) {
                discrepancyCount++;
            }
            detailsToSave.add(detail);
        }

        Object[] options = {"Xác Nhận Cân Bằng CSDL", "Chỉ Lưu Nháp (DRAFT)", "Hủy Bỏ"};
        int choice = JOptionPane.showOptionDialog(
                this,
                "KẾT QUẢ ĐỐI CHIẾU KIỂM KÊ:\n"
                        + "• Tổng số thiết bị kiểm kê: " + rowCount + " máy\n"
                        + "• Số máy khớp trạng thái: " + (rowCount - discrepancyCount) + " máy\n"
                        + "• Số máy sai lệch trạng thái: " + discrepancyCount + " máy\n\n"
                        + (discrepancyCount > 0
                        ? "⚠️ Phát hiện " + discrepancyCount + " máy có sai lệch trạng thái thực tế!\n"
                        + "Nếu chọn 'Xác Nhận Cân Bằng CSDL', hệ thống sẽ tự động cập nhật trạng thái mới của " + discrepancyCount + " thiết bị này vào kho và ghi nhận Audit Log.\n\n"
                        : "✅ 100% thiết bị khớp hoàn toàn với CSDL.\n\n")
                        + "Bạn muốn thực hiện thao tác nào?",
                "Chốt Kiểm Kê & Cân Bằng Kho",
                JOptionPane.YES_NO_CANCEL_OPTION,
                JOptionPane.QUESTION_MESSAGE,
                null,
                options,
                options[0]
        );

        if (choice == 2 || choice == JOptionPane.CLOSED_OPTION) {
            return; // Hủy bỏ
        }

        boolean isBalanceDatabase = (choice == 0);

        String auditId = auditService.generateAuditId();
        String empId = UserSession.getInstance().isLoggedIn() ? UserSession.getInstance().getCurrentUser().getId() : "EMP_SYSTEM";
        String empName = UserSession.getInstance().isLoggedIn() ? UserSession.getInstance().getCurrentUser().getFullName() : "Hệ Thống";

        StockAudit audit = new StockAudit(
                auditId,
                auditName,
                empId,
                empName,
                LocalDate.now(),
                detailsToSave,
                rowCount,
                rowCount,
                discrepancyCount,
                isBalanceDatabase ? StockAudit.STATUS_COMPLETED : StockAudit.STATUS_DRAFT,
                txtAuditNote.getText().trim()
        );

        btnExecuteAudit.setEnabled(false);
        setCursor(new Cursor(Cursor.WAIT_CURSOR));

        new SwingWorker<Boolean, Void>() {
            @Override
            protected Boolean doInBackground() {
                if (isBalanceDatabase) {
                    return auditService.processStockAuditAndAdjust(audit);
                } else {
                    return auditService.saveDraftAudit(audit);
                }
            }

            @Override
            protected void done() {
                btnExecuteAudit.setEnabled(true);
                setCursor(new Cursor(Cursor.DEFAULT_CURSOR));
                try {
                    boolean success = get();
                    if (success) {
                        String msg = isBalanceDatabase
                                ? "🎉 Chốt đợt kiểm kê & Cân bằng tồn kho thành công!\n"
                                + "Mã phiếu kiểm kê: " + auditId + "\n"
                                + "Đã tự động cập nhật " + audit.getTotalDiscrepancy() + " thiết bị bị lệch trạng thái vào CSDL."
                                : "Đã lưu bản nháp đợt kiểm kê: " + auditId;
                        JOptionPane.showMessageDialog(StockAuditPanel.this, msg, "Hoàn Tất", JOptionPane.INFORMATION_MESSAGE);

                        // Làm mới danh sách thiết bị và tải lại lịch sử
                        loadEquipmentsFromDatabaseAsync();
                        loadAuditHistoryAsync();
                    } else {
                        JOptionPane.showMessageDialog(StockAuditPanel.this, "Xử lý kiểm kê thất bại!", "Lỗi", JOptionPane.ERROR_MESSAGE);
                    }
                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(StockAuditPanel.this, "Lỗi: " + ex.getMessage(), "Lỗi", JOptionPane.ERROR_MESSAGE);
                }
            }
        }.execute();
    }

    // =========================================================================
    // XỬ LÝ NGHIỆP VỤ & TƯƠNG TÁC GIAO DIỆN TAB 2 (LỊCH SỬ KIỂM KÊ)
    // =========================================================================

    private void loadAuditHistoryAsync() {
        new SwingWorker<List<StockAudit>, Void>() {
            @Override
            protected List<StockAudit> doInBackground() {
                return auditService.getAllAudits();
            }

            @Override
            protected void done() {
                try {
                    List<StockAudit> list = get();
                    auditHistoryCache.clear();
                    auditHistoryCache.addAll(list);
                    renderHistoryTable(auditHistoryCache);
                } catch (Exception ex) {
                    System.err.println("[StockAuditPanel] Lỗi tải lịch sử kiểm kê: " + ex.getMessage());
                }
            }
        }.execute();
    }

    private void filterHistoryAsync() {
        String keyword = txtSearchHistory.getText().trim().toLowerCase();
        if (keyword.isEmpty()) {
            renderHistoryTable(auditHistoryCache);
            return;
        }

        List<StockAudit> filtered = new ArrayList<>();
        for (StockAudit a : auditHistoryCache) {
            boolean match = (a.getId() != null && a.getId().toLowerCase().contains(keyword))
                    || (a.getAuditName() != null && a.getAuditName().toLowerCase().contains(keyword))
                    || (a.getEmployeeName() != null && a.getEmployeeName().toLowerCase().contains(keyword))
                    || (a.getStatus() != null && a.getStatus().toLowerCase().contains(keyword));
            if (match) filtered.add(a);
        }
        renderHistoryTable(filtered);
    }

    private void renderHistoryTable(List<StockAudit> list) {
        modelHistory.setRowCount(0);
        for (StockAudit a : list) {
            String dateFormatted = a.getAuditDate() != null ? a.getAuditDate().format(DATE_FORMATTER) : "N/A";
            modelHistory.addRow(new Object[]{
                    a.getId(),
                    a.getAuditName(),
                    dateFormatted,
                    a.getEmployeeName(),
                    a.getTotalSystemCount() + " máy",
                    a.getTotalDiscrepancy() + " máy",
                    a.getStatus()
            });
        }
    }

    private void showSelectedAuditDetailDialog() {
        int selectedRow = tblHistory.getSelectedRow();
        if (selectedRow < 0) {
            JOptionPane.showMessageDialog(this, "Vui lòng chọn một Đợt kiểm kê từ bảng để xem chi tiết sai lệch!", "Thông báo", JOptionPane.WARNING_MESSAGE);
            return;
        }

        String auditId = (String) modelHistory.getValueAt(selectedRow, 0);
        StockAudit audit = null;
        for (StockAudit a : auditHistoryCache) {
            if (a.getId().equals(auditId)) {
                audit = a;
                break;
            }
        }
        if (audit == null) {
            audit = auditService.getAuditById(auditId);
        }
        if (audit == null) {
            JOptionPane.showMessageDialog(this, "Không tìm thấy dữ liệu phiếu kiểm kê: " + auditId, "Lỗi", JOptionPane.ERROR_MESSAGE);
            return;
        }

        Frame parent = (Frame) SwingUtilities.getWindowAncestor(this);
        StockAuditDetailDialog dialog = new StockAuditDetailDialog(parent, audit);
        dialog.setVisible(true);
    }

    // =========================================================================
    // TẢI DANH MỤC & DỮ LIỆU BAN ĐẦU ASYNC
    // =========================================================================

    private void loadCategoriesAndInitialDataAsync() {
        new SwingWorker<Void, Void>() {
            private List<Category> categories;
            private List<Product> products;

            @Override
            protected Void doInBackground() {
                categories = categoryService.getAllCategories();
                products = productService.getAllProducts();
                return null;
            }

            @Override
            protected void done() {
                if (products != null) {
                    productMap.clear();
                    for (Product p : products) {
                        productMap.put(p.getId(), p);
                    }
                }

                if (categories != null) {
                    cboCategoryFilter.removeAllItems();
                    cboCategoryFilter.addItem("-- Tất cả danh mục --");
                    for (Category c : categories) {
                        cboCategoryFilter.addItem(c.getCategoryName());
                    }
                }

                // Tải máy trong kho vào lưới Tab 1 và nạp lịch sử Tab 2
                loadEquipmentsFromDatabaseAsync();
                loadAuditHistoryAsync();
            }
        }.execute();
    }

    // =========================================================================
    // UI UTILS & HELPERS
    // =========================================================================

    private JLabel createStatCard(String title, String initialValue, Color accentColor) {
        JPanel card = new JPanel(new GridLayout(2, 1, 0, 4));
        card.setBackground(Color.WHITE);
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(UIUtils.COLOR_BORDER, 1),
                new EmptyBorder(10, 14, 10, 14)
        ));

        JLabel lblTitle = new JLabel(title);
        lblTitle.setFont(new Font("Segoe UI", Font.BOLD, 11));
        lblTitle.setForeground(UIUtils.COLOR_TEXT_MUTED);

        JLabel lblValue = new JLabel(initialValue);
        lblValue.setFont(new Font("Segoe UI", Font.BOLD, 20));
        lblValue.setForeground(accentColor);

        card.add(lblTitle);
        card.add(lblValue);
        return lblValue;
    }

    private void styleAuditGrid(JTable table) {
        table.setFont(UIUtils.FONT_BODY);
        table.setRowHeight(36);
        table.setGridColor(UIUtils.COLOR_BORDER);
        table.setShowGrid(true);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.setSelectionBackground(new Color(224, 242, 254));
        table.setSelectionForeground(UIUtils.COLOR_TEXT_MAIN);

        JTableHeader header = table.getTableHeader();
        header.setFont(UIUtils.FONT_BODY_BOLD);
        header.setBackground(new Color(241, 245, 249));
        header.setForeground(UIUtils.COLOR_TEXT_MAIN);
        header.setPreferredSize(new Dimension(0, 38));

        // Căn giữa cột STT
        DefaultTableCellRenderer centerRenderer = new DefaultTableCellRenderer();
        centerRenderer.setHorizontalAlignment(SwingConstants.CENTER);
        table.getColumnModel().getColumn(0).setPreferredWidth(50);
        table.getColumnModel().getColumn(0).setMaxWidth(60);
        table.getColumnModel().getColumn(0).setCellRenderer(centerRenderer);

        // Highlight dòng sai lệch trạng thái
        table.setDefaultRenderer(Object.class, new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable tbl, Object value, boolean isSelected, boolean hasFocus, int row, int col) {
                Component c = super.getTableCellRendererComponent(tbl, value, isSelected, hasFocus, row, col);
                if (!isSelected) {
                    String sysStatus = (String) tbl.getValueAt(row, 3);
                    String actStatus = (String) tbl.getValueAt(row, 4);
                    if (sysStatus != null && actStatus != null && !sysStatus.equalsIgnoreCase(actStatus)) {
                        c.setBackground(new Color(254, 242, 242)); // Đỏ nhạt cảnh báo
                        c.setForeground(new Color(185, 28, 28)); // Chữ đỏ đậm
                    } else {
                        c.setBackground(Color.WHITE);
                        c.setForeground(UIUtils.COLOR_TEXT_MAIN);
                    }
                }
                return c;
            }
        });
    }

    private void styleGeneralTable(JTable table) {
        table.setFont(UIUtils.FONT_BODY);
        table.setRowHeight(34);
        table.setGridColor(UIUtils.COLOR_BORDER);
        table.setShowGrid(true);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.setSelectionBackground(new Color(224, 242, 254));
        table.setSelectionForeground(UIUtils.COLOR_TEXT_MAIN);

        JTableHeader header = table.getTableHeader();
        header.setFont(UIUtils.FONT_BODY_BOLD);
        header.setBackground(new Color(241, 245, 249));
        header.setForeground(UIUtils.COLOR_TEXT_MAIN);
        header.setPreferredSize(new Dimension(0, 38));
    }

    // =========================================================================
    // DIALOG XEM CHI TIẾT SAI LỆCH CỦA ĐỢT KIỂM KÊ
    // =========================================================================

    private static class StockAuditDetailDialog extends JDialog {

        public StockAuditDetailDialog(Frame parent, StockAudit audit) {
            super(parent, "CHI TIẾT SAI LỆCH ĐỢT KIỂM KÊ: " + audit.getId(), true);
            setSize(860, 560);
            setLocationRelativeTo(parent);
            setLayout(new BorderLayout(0, 14));
            getContentPane().setBackground(UIUtils.COLOR_BG_LIGHT);
            ((JPanel) getContentPane()).setBorder(new EmptyBorder(16, 20, 16, 20));

            // Header tóm tắt
            JPanel headerPanel = new JPanel(new GridLayout(2, 2, 14, 8));
            headerPanel.setBackground(Color.WHITE);
            headerPanel.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(UIUtils.COLOR_BORDER, 1),
                    new EmptyBorder(12, 16, 12, 16)
            ));

            String dateStr = audit.getAuditDate() != null ? audit.getAuditDate().format(DATE_FORMATTER) : "N/A";
            headerPanel.add(new JLabel("Mã phiếu: " + audit.getId()) {{ setFont(UIUtils.FONT_BODY_BOLD); }});
            headerPanel.add(new JLabel("Tên đợt: " + audit.getAuditName()) {{ setFont(UIUtils.FONT_BODY_BOLD); }});
            headerPanel.add(new JLabel("Ngày kiểm kê: " + dateStr + " (Bởi: " + audit.getEmployeeName() + ")") {{ setFont(UIUtils.FONT_BODY); }});
            headerPanel.add(new JLabel("Số máy sai lệch: " + audit.getTotalDiscrepancy() + " / " + audit.getTotalSystemCount() + " máy") {{
                setFont(UIUtils.FONT_BODY_BOLD);
                setForeground(audit.getTotalDiscrepancy() > 0 ? UIUtils.COLOR_DANGER : UIUtils.COLOR_SUCCESS);
            }});

            // Bảng danh sách các máy có sai lệch
            String[] cols = {"Mã Serial", "Tên Sản Phẩm", "Trạng Thái CSDL Cũ", "Trạng Thái Thực Tế Điều Chỉnh", "Ghi Chú Lý Do"};
            DefaultTableModel model = new DefaultTableModel(cols, 0) {
                @Override public boolean isCellEditable(int r, int c) { return false; }
            };

            List<StockAuditDetail> list = audit.getDetails();
            int countDiscrepancy = 0;
            if (list != null) {
                for (StockAuditDetail d : list) {
                    if (d.hasDiscrepancy()) {
                        countDiscrepancy++;
                        model.addRow(new Object[]{
                                d.getSerialId(),
                                d.getProductName(),
                                d.getSystemStatus(),
                                "→ " + d.getActualStatus(),
                                d.getNote()
                        });
                    }
                }
            }

            JTable table = new JTable(model);
            table.setFont(UIUtils.FONT_BODY);
            table.setRowHeight(32);
            table.getTableHeader().setFont(UIUtils.FONT_BODY_BOLD);
            table.getTableHeader().setBackground(new Color(241, 245, 249));

            JScrollPane scrollPane = new JScrollPane(table);
            scrollPane.setBorder(BorderFactory.createLineBorder(UIUtils.COLOR_BORDER, 1));

            // Footer
            JPanel footer = new JPanel(new BorderLayout());
            footer.setOpaque(false);

            JLabel lblFooterInfo = new JLabel(countDiscrepancy > 0
                    ? "Danh sách hiển thị " + countDiscrepancy + " thiết bị có sai lệch trạng thái đã được cân bằng vào CSDL."
                    : "Đợt kiểm kê này hoàn hảo: 100% thiết bị thực tế khớp với CSDL.");
            lblFooterInfo.setFont(UIUtils.FONT_BODY);
            lblFooterInfo.setForeground(countDiscrepancy > 0 ? UIUtils.COLOR_DANGER : UIUtils.COLOR_SUCCESS);

            JButton btnClose = UIUtils.createPrimaryButton("Đóng Cửa Sổ");
            btnClose.setPreferredSize(new Dimension(140, 36));
            btnClose.addActionListener(e -> dispose());

            footer.add(lblFooterInfo, BorderLayout.WEST);
            footer.add(btnClose, BorderLayout.EAST);

            add(headerPanel, BorderLayout.NORTH);
            add(scrollPane, BorderLayout.CENTER);
            add(footer, BorderLayout.SOUTH);
        }
    }
}