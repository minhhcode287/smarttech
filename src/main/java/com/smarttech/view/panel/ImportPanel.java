package com.smarttech.view.panel;

import com.smarttech.model.ImportItem;
import com.smarttech.model.ImportOrder;
import com.smarttech.model.Product;
import com.smarttech.model.Supplier;
import com.smarttech.service.ImportService;
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
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.text.DecimalFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Phân Hệ Nhập Kho & Quản Lý Nhà Cung Cấp (ImportPanel).
 * Cung cấp giải pháp quản trị nhập hàng phần cứng công nghệ hoàn chỉnh:
 * - Tab 1: Lập Phiếu Nhập Kho Thiết Bị (Cấp phát mã Serial phần cứng, tự sinh Serial, nhập giá và kích hoạt vào kho)
 * - Tab 2: Lịch Sử & Nhật Ký Nhập Kho (Theo dõi toàn bộ các đợt nhập hàng, xem chi tiết từng mã Serial đã nhập)
 * - Tab 3: Quản Lý Nhà Cung Cấp (Quản lý hồ sơ đối tác cung ứng, MST, SĐT, Email, trạng thái hợp tác)
 *
 * @author Senior Java Developer & Software Architect
 * @project SmartTech
 */
public class ImportPanel extends JPanel {

    private final ImportService importService;
    private final ProductService productService;

    // Cache dữ liệu
    private final List<Supplier> supplierCache = new ArrayList<>();
    private final List<Product> productCache = new ArrayList<>();
    private final List<ImportOrder> importOrderCache = new ArrayList<>();

    // Formatters
    private static final DecimalFormat CURRENCY_FORMAT = new DecimalFormat("#,##0 VNĐ");
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    // ================= COMPONENTS TAB 1: LẬP PHIẾU NHẬP =================
    private JComboBox<SupplierItem> cboSupplier;
    private JComboBox<ProductItem> cboProduct;
    private JTextField txtSerialId;
    private JTextField txtImportPrice;
    private JTable tblImportCart;
    private DefaultTableModel modelImportCart;
    private final List<ImportItem> currentCartItems = new ArrayList<>();
    private JLabel lblTotalQuantity;
    private JLabel lblTotalAmount;
    private JTextField txtImportNote;
    private JButton btnConfirmImport;

    // ================= COMPONENTS TAB 2: LỊCH SỬ NHẬP =================
    private JTextField txtSearchHistory;
    private JTable tblHistory;
    private DefaultTableModel modelHistory;

    // ================= COMPONENTS TAB 3: NHÀ CUNG CẤP =================
    private JTextField txtSearchSupplier;
    private JTable tblSuppliers;
    private DefaultTableModel modelSuppliers;

    public ImportPanel() {
        this.importService = new ImportService();
        this.productService = new ProductService();

        initComponents();
        loadInitialDataAsync();
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

        tabbedPane.addTab("📦  Lập Phiếu Nhập Kho Thiết Bị", createImportEntryTab());
        tabbedPane.addTab("📜  Lịch Sử & Nhật Ký Nhập Kho", createHistoryTab());
        tabbedPane.addTab("🏢  Quản Lý Nhà Cung Cấp", createSupplierTab());

        add(tabbedPane, BorderLayout.CENTER);
    }

    /**
     * Tạo thanh tiêu đề chuẩn Enterprise.
     */
    private JPanel createTopHeaderBar() {
        JPanel header = new JPanel(new BorderLayout());
        header.setOpaque(false);

        JPanel titlePanel = new JPanel(new GridLayout(2, 1, 0, 4));
        titlePanel.setOpaque(false);

        JLabel lblTitle = new JLabel("QUẢN LÝ NHẬP KHO & NHÀ CUNG CẤP THIẾT BỊ");
        lblTitle.setFont(UIUtils.FONT_TITLE);
        lblTitle.setForeground(UIUtils.COLOR_PRIMARY);

        JLabel lblSubtitle = new JLabel("Lập phiếu nhập kho, cấp phát mã Serial phần cứng độc bản và quản trị danh bạ đối tác cung ứng công nghệ.");
        lblSubtitle.setFont(UIUtils.FONT_SUBTITLE);
        lblSubtitle.setForeground(UIUtils.COLOR_TEXT_MUTED);

        titlePanel.add(lblTitle);
        titlePanel.add(lblSubtitle);

        header.add(titlePanel, BorderLayout.WEST);
        return header;
    }

    // =========================================================================
    // TAB 1: LẬP PHIẾU NHẬP KHO THIẾT BỊ
    // =========================================================================

    private JPanel createImportEntryTab() {
        JPanel panel = new JPanel(new BorderLayout(16, 16));
        panel.setBackground(UIUtils.COLOR_BG_LIGHT);
        panel.setBorder(new EmptyBorder(14, 14, 14, 14));

        // KHUNG FORM NHẬP CHI TIẾT (BÊN TRÁI)
        JPanel leftPanel = new JPanel(new BorderLayout(0, 14));
        leftPanel.setPreferredSize(new Dimension(380, 0));
        leftPanel.setBackground(Color.WHITE);
        leftPanel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(UIUtils.COLOR_BORDER, 1),
                new EmptyBorder(16, 18, 16, 18)
        ));

        JLabel lblLeftTitle = new JLabel("THÔNG TIN NHẬP KHO", SwingConstants.CENTER);
        lblLeftTitle.setFont(UIUtils.FONT_HEADER);
        lblLeftTitle.setForeground(UIUtils.COLOR_PRIMARY);
        leftPanel.add(lblLeftTitle, BorderLayout.NORTH);

        JPanel formFields = new JPanel(new GridLayout(6, 1, 0, 10));
        formFields.setOpaque(false);

        // 1. Chọn Nhà Cung Cấp
        cboSupplier = new JComboBox<>();
        cboSupplier.setFont(UIUtils.FONT_BODY);

        // 2. Chọn Mẫu Sản Phẩm
        cboProduct = new JComboBox<>();
        cboProduct.setFont(UIUtils.FONT_BODY);
        cboProduct.addActionListener(e -> onProductSelected());

        // 3. Serial Number kèm Nút Tự Sinh
        JPanel serialPanel = new JPanel(new BorderLayout(6, 0));
        serialPanel.setOpaque(false);
        txtSerialId = UIUtils.createTextField();
        txtSerialId.setFont(UIUtils.FONT_BODY);
        txtSerialId.setToolTipText("Ví dụ: SN-MAC-839210");

        JButton btnGenerateSerial = new JButton("⚡ Sinh Mã");
        btnGenerateSerial.setFont(new Font("Segoe UI", Font.BOLD, 12));
        btnGenerateSerial.setBackground(new Color(241, 245, 249));
        btnGenerateSerial.setForeground(UIUtils.COLOR_PRIMARY);
        btnGenerateSerial.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btnGenerateSerial.setFocusPainted(false);
        btnGenerateSerial.addActionListener(e -> generateRandomSerial());

        serialPanel.add(txtSerialId, BorderLayout.CENTER);
        serialPanel.add(btnGenerateSerial, BorderLayout.EAST);

        // 4. Giá Nhập Kho
        txtImportPrice = UIUtils.createTextField();
        txtImportPrice.setFont(UIUtils.FONT_BODY);
        txtImportPrice.setToolTipText("Nhập giá mua từ NCC (VNĐ)");

        // 5. Nút Thêm vào phiếu
        JButton btnAddSerial = new JButton("➕  Thêm Serial Vào Phiếu Nhập");
        btnAddSerial.setFont(UIUtils.FONT_BODY_BOLD);
        btnAddSerial.setBackground(UIUtils.COLOR_PRIMARY);
        btnAddSerial.setForeground(Color.WHITE);
        btnAddSerial.setFocusPainted(false);
        btnAddSerial.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btnAddSerial.setPreferredSize(new Dimension(0, 40));
        btnAddSerial.addActionListener(e -> addSerialToImportCart());

        formFields.add(createFieldContainer("1. Nhà Cung Cấp (*):", cboSupplier));
        formFields.add(createFieldContainer("2. Dòng Sản Phẩm (*):", cboProduct));
        formFields.add(createFieldContainer("3. Số Serial Máy (*):", serialPanel));
        formFields.add(createFieldContainer("4. Giá Nhập Kho (VNĐ) (*):", txtImportPrice));
        formFields.add(Box.createVerticalStrut(4));
        formFields.add(btnAddSerial);

        leftPanel.add(formFields, BorderLayout.CENTER);

        // KHUNG DANH SÁCH SERIAL CHỜ NHẬP & THANH TOÁN (BÊN PHẢI)
        JPanel rightPanel = new JPanel(new BorderLayout(0, 12));
        rightPanel.setOpaque(false);

        // Bảng Giỏ hàng nhập
        String[] columns = {"STT", "Mã Sản Phẩm", "Tên Thiết Bị", "Mã Serial Phần Cứng", "Đơn Giá Nhập"};
        modelImportCart = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        tblImportCart = new JTable(modelImportCart);
        styleTable(tblImportCart);

        JScrollPane scrollCart = new JScrollPane(tblImportCart);
        scrollCart.setBorder(BorderFactory.createLineBorder(UIUtils.COLOR_BORDER, 1));

        // Nút xóa dòng
        JPanel cartActionPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        cartActionPanel.setOpaque(false);

        JButton btnRemoveCartItem = new JButton("Xóa Dòng Chọn");
        btnRemoveCartItem.setFont(UIUtils.FONT_BODY);
        btnRemoveCartItem.setForeground(UIUtils.COLOR_DANGER);
        btnRemoveCartItem.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btnRemoveCartItem.addActionListener(e -> removeSelectedCartItem());

        JButton btnClearCart = new JButton("Xóa Toàn Bộ");
        btnClearCart.setFont(UIUtils.FONT_BODY);
        btnClearCart.setForeground(UIUtils.COLOR_TEXT_MUTED);
        btnClearCart.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btnClearCart.addActionListener(e -> clearImportCart());

        cartActionPanel.add(btnClearCart);
        cartActionPanel.add(btnRemoveCartItem);

        // Thanh thanh toán / xác nhận nhập
        JPanel checkoutPanel = new JPanel(new BorderLayout(16, 10));
        checkoutPanel.setBackground(Color.WHITE);
        checkoutPanel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(UIUtils.COLOR_BORDER, 1),
                new EmptyBorder(14, 18, 14, 18)
        ));

        JPanel summaryInfo = new JPanel(new GridLayout(2, 2, 12, 6));
        summaryInfo.setOpaque(false);

        lblTotalQuantity = new JLabel("Tổng số lượng: 0 thiết bị");
        lblTotalQuantity.setFont(UIUtils.FONT_BODY_BOLD);

        lblTotalAmount = new JLabel("Tổng tiền nhập: 0 VNĐ");
        lblTotalAmount.setFont(new Font("Segoe UI", Font.BOLD, 18));
        lblTotalAmount.setForeground(UIUtils.COLOR_PRIMARY);

        txtImportNote = UIUtils.createTextField();
        txtImportNote.setFont(UIUtils.FONT_BODY);
        txtImportNote.setText("Nhập lô thiết bị mới phục vụ kinh doanh");

        summaryInfo.add(lblTotalQuantity);
        summaryInfo.add(lblTotalAmount);
        summaryInfo.add(new JLabel("Ghi chú lô hàng:") {{ setFont(UIUtils.FONT_BODY_BOLD); }});
        summaryInfo.add(txtImportNote);

        btnConfirmImport = new JButton("✅  XÁC NHẬN NHẬP KHO & LƯU CSDL");
        btnConfirmImport.setFont(UIUtils.FONT_BODY_BOLD);
        btnConfirmImport.setBackground(UIUtils.COLOR_SUCCESS);
        btnConfirmImport.setForeground(Color.WHITE);
        btnConfirmImport.setFocusPainted(false);
        btnConfirmImport.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btnConfirmImport.setPreferredSize(new Dimension(280, 44));
        btnConfirmImport.addActionListener(e -> executeConfirmImport());

        checkoutPanel.add(summaryInfo, BorderLayout.CENTER);
        checkoutPanel.add(btnConfirmImport, BorderLayout.EAST);

        rightPanel.add(cartActionPanel, BorderLayout.NORTH);
        rightPanel.add(scrollCart, BorderLayout.CENTER);
        rightPanel.add(checkoutPanel, BorderLayout.SOUTH);

        panel.add(leftPanel, BorderLayout.WEST);
        panel.add(rightPanel, BorderLayout.CENTER);
        return panel;
    }

    // =========================================================================
    // TAB 2: LỊCH SỬ & NHẬT KÝ NHẬP KHO
    // =========================================================================

    private JPanel createHistoryTab() {
        JPanel panel = new JPanel(new BorderLayout(0, 12));
        panel.setBackground(Color.WHITE);
        panel.setBorder(new EmptyBorder(14, 16, 14, 16));

        // Toolbar tra cứu
        JPanel toolbar = new JPanel(new BorderLayout(12, 0));
        toolbar.setOpaque(false);

        JPanel leftSearch = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        leftSearch.setOpaque(false);

        JLabel lblSearch = new JLabel("Tìm phiếu:");
        lblSearch.setFont(UIUtils.FONT_BODY_BOLD);

        txtSearchHistory = UIUtils.createTextField();
        txtSearchHistory.setPreferredSize(new Dimension(280, 36));
        txtSearchHistory.setToolTipText("Tìm theo Mã phiếu nhập, Tên NCC hoặc Ghi chú...");
        txtSearchHistory.getDocument().addDocumentListener(new DocumentListener() {
            @Override public void insertUpdate(DocumentEvent e) { filterHistoryAsync(); }
            @Override public void removeUpdate(DocumentEvent e) { filterHistoryAsync(); }
            @Override public void changedUpdate(DocumentEvent e) { filterHistoryAsync(); }
        });

        JButton btnRefreshHistory = new JButton("🔄 Làm Mới");
        btnRefreshHistory.setFont(UIUtils.FONT_BODY);
        btnRefreshHistory.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btnRefreshHistory.addActionListener(e -> loadHistoryDataAsync());

        leftSearch.add(lblSearch);
        leftSearch.add(txtSearchHistory);
        leftSearch.add(btnRefreshHistory);

        JPanel rightActions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        rightActions.setOpaque(false);

        JButton btnViewDetail = new JButton("👁️  Xem Chi Tiết Phiếu Nhập");
        btnViewDetail.setFont(UIUtils.FONT_BODY_BOLD);
        btnViewDetail.setBackground(UIUtils.COLOR_PRIMARY);
        btnViewDetail.setForeground(Color.WHITE);
        btnViewDetail.setFocusPainted(false);
        btnViewDetail.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btnViewDetail.setPreferredSize(new Dimension(220, 36));
        btnViewDetail.addActionListener(e -> showSelectedOrderDetailDialog());

        rightActions.add(btnViewDetail);

        toolbar.add(leftSearch, BorderLayout.WEST);
        toolbar.add(rightActions, BorderLayout.EAST);

        // Bảng lịch sử
        String[] columns = {"Mã Phiếu", "Nhà Cung Cấp", "Ngày Nhập", "Số Lượng Thiết Bị", "Tổng Tiền Nhập", "Người Lập", "Trạng Thái"};
        modelHistory = new DefaultTableModel(columns, 0) {
            @Override public boolean isCellEditable(int row, int col) { return false; }
        };
        tblHistory = new JTable(modelHistory);
        styleTable(tblHistory);
        tblHistory.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    showSelectedOrderDetailDialog();
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
    // TAB 3: QUẢN LÝ DANH SÁCH NHÀ CUNG CẤP
    // =========================================================================

    private JPanel createSupplierTab() {
        JPanel panel = new JPanel(new BorderLayout(0, 12));
        panel.setBackground(Color.WHITE);
        panel.setBorder(new EmptyBorder(14, 16, 14, 16));

        // Toolbar quản lý NCC
        JPanel toolbar = new JPanel(new BorderLayout(12, 0));
        toolbar.setOpaque(false);

        JPanel leftSearch = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        leftSearch.setOpaque(false);

        JLabel lblSearch = new JLabel("Tìm đối tác:");
        lblSearch.setFont(UIUtils.FONT_BODY_BOLD);

        txtSearchSupplier = UIUtils.createTextField();
        txtSearchSupplier.setPreferredSize(new Dimension(280, 36));
        txtSearchSupplier.setToolTipText("Tìm theo Tên NCC, SĐT, Email, MST...");
        txtSearchSupplier.getDocument().addDocumentListener(new DocumentListener() {
            @Override public void insertUpdate(DocumentEvent e) { filterSupplierAsync(); }
            @Override public void removeUpdate(DocumentEvent e) { filterSupplierAsync(); }
            @Override public void changedUpdate(DocumentEvent e) { filterSupplierAsync(); }
        });

        JButton btnRefreshSupplier = new JButton("🔄 Làm Mới");
        btnRefreshSupplier.setFont(UIUtils.FONT_BODY);
        btnRefreshSupplier.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btnRefreshSupplier.addActionListener(e -> loadSuppliersAsync());

        leftSearch.add(lblSearch);
        leftSearch.add(txtSearchSupplier);
        leftSearch.add(btnRefreshSupplier);

        JPanel rightActions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        rightActions.setOpaque(false);

        JButton btnAddSupplier = new JButton("➕  Thêm Nhà Cung Cấp");
        btnAddSupplier.setFont(UIUtils.FONT_BODY_BOLD);
        btnAddSupplier.setBackground(UIUtils.COLOR_PRIMARY);
        btnAddSupplier.setForeground(Color.WHITE);
        btnAddSupplier.setFocusPainted(false);
        btnAddSupplier.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btnAddSupplier.setPreferredSize(new Dimension(190, 36));
        btnAddSupplier.addActionListener(e -> showSupplierDialog(null));

        JButton btnEditSupplier = new JButton("✏️ Sửa");
        btnEditSupplier.setFont(UIUtils.FONT_BODY);
        btnEditSupplier.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btnEditSupplier.setPreferredSize(new Dimension(90, 36));
        btnEditSupplier.addActionListener(e -> editSelectedSupplier());

        JButton btnToggleStatus = new JButton("🔒 Khóa / Mở");
        btnToggleStatus.setFont(UIUtils.FONT_BODY);
        btnToggleStatus.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btnToggleStatus.setPreferredSize(new Dimension(120, 36));
        btnToggleStatus.addActionListener(e -> toggleSupplierStatus());

        rightActions.add(btnAddSupplier);
        rightActions.add(btnEditSupplier);
        rightActions.add(btnToggleStatus);

        toolbar.add(leftSearch, BorderLayout.WEST);
        toolbar.add(rightActions, BorderLayout.EAST);

        // Bảng Nhà cung cấp
        String[] columns = {"Mã NCC", "Tên Nhà Cung Cấp", "Số Điện Thoại", "Email Liên Hệ", "Địa Chỉ Trụ Sở", "Mã Số Thuế", "Trạng Thái"};
        modelSuppliers = new DefaultTableModel(columns, 0) {
            @Override public boolean isCellEditable(int row, int col) { return false; }
        };
        tblSuppliers = new JTable(modelSuppliers);
        styleTable(tblSuppliers);

        JScrollPane scrollPane = new JScrollPane(tblSuppliers);
        scrollPane.setBorder(BorderFactory.createLineBorder(UIUtils.COLOR_BORDER, 1));

        panel.add(toolbar, BorderLayout.NORTH);
        panel.add(scrollPane, BorderLayout.CENTER);
        return panel;
    }

    // =========================================================================
    // XỬ LÝ NGHIỆP VỤ & TƯƠNG TÁC GIAO DIỆN TAB 1
    // =========================================================================

    private void onProductSelected() {
        ProductItem item = (ProductItem) cboProduct.getSelectedItem();
        if (item != null && item.getProduct() != null) {
            double basePrice = item.getProduct().getBasePrice();
            // Gợi ý giá nhập = 70% giá bán lẻ đề xuất, làm tròn tới hàng chục nghìn
            double suggestedPrice = Math.round((basePrice * 0.70) / 10000.0) * 10000.0;
            if (suggestedPrice <= 0) suggestedPrice = 1000000.0;
            txtImportPrice.setText(String.format("%.0f", suggestedPrice));
        }
    }

    private void generateRandomSerial() {
        ProductItem item = (ProductItem) cboProduct.getSelectedItem();
        String brandPrefix = "ST";
        if (item != null && item.getProduct() != null && item.getProduct().getBrand() != null) {
            String b = item.getProduct().getBrand().replaceAll("[^A-Za-z0-9]", "").toUpperCase();
            if (!b.isEmpty()) {
                brandPrefix = b.length() > 4 ? b.substring(0, 4) : b;
            }
        }
        int randomDigits = 100000 + new Random().nextInt(900000);
        txtSerialId.setText("SN-" + brandPrefix + "-" + randomDigits);
    }

    private void addSerialToImportCart() {
        SupplierItem supItem = (SupplierItem) cboSupplier.getSelectedItem();
        if (supItem == null || supItem.getSupplier() == null) {
            JOptionPane.showMessageDialog(this, "Vui lòng chọn Nhà Cung Cấp!", "Cảnh báo", JOptionPane.WARNING_MESSAGE);
            return;
        }

        ProductItem prodItem = (ProductItem) cboProduct.getSelectedItem();
        if (prodItem == null || prodItem.getProduct() == null) {
            JOptionPane.showMessageDialog(this, "Vui lòng chọn Dòng Sản Phẩm!", "Cảnh báo", JOptionPane.WARNING_MESSAGE);
            return;
        }

        String serial = txtSerialId.getText().trim().toUpperCase();
        if (serial.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Mã Serial thiết bị không được để trống!", "Cảnh báo", JOptionPane.WARNING_MESSAGE);
            txtSerialId.requestFocus();
            return;
        }

        // Kiểm tra trùng lặp trong giỏ nhập hiện tại
        for (ImportItem item : currentCartItems) {
            if (item.getSerialId().equalsIgnoreCase(serial)) {
                JOptionPane.showMessageDialog(this, "Mã Serial '" + serial + "' đã có trong danh sách chờ nhập kho!", "Trùng Lặp", JOptionPane.WARNING_MESSAGE);
                return;
            }
        }

        // Kiểm tra xem Serial đã có trong CSDL kho chưa
        if (importService.isSerialExists(serial)) {
            JOptionPane.showMessageDialog(this, "Mã Serial '" + serial + "' đã tồn tại sẵn trong kho hệ thống!", "Serial Đã Tồn Tại", JOptionPane.ERROR_MESSAGE);
            return;
        }

        double importPrice;
        try {
            importPrice = Double.parseDouble(txtImportPrice.getText().trim());
            if (importPrice <= 0) throw new NumberFormatException();
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Đơn giá nhập kho phải là một số dương hợp lệ!", "Lỗi Định Dạng", JOptionPane.ERROR_MESSAGE);
            txtImportPrice.requestFocus();
            return;
        }

        ImportItem item = new ImportItem(
                prodItem.getProduct().getId(),
                prodItem.getProduct().getProductName(),
                serial,
                importPrice
        );

        currentCartItems.add(item);
        modelImportCart.addRow(new Object[]{
                currentCartItems.size(),
                item.getProductId(),
                item.getProductName(),
                item.getSerialId(),
                CURRENCY_FORMAT.format(item.getImportPrice())
        });

        updateCartTotals();

        // Tự động sinh mã tiếp theo cho tiện nhập liên tục
        generateRandomSerial();
    }

    private void removeSelectedCartItem() {
        int row = tblImportCart.getSelectedRow();
        if (row >= 0 && row < currentCartItems.size()) {
            currentCartItems.remove(row);
            modelImportCart.removeRow(row);
            // Cập nhật lại cột STT
            for (int i = 0; i < modelImportCart.getRowCount(); i++) {
                modelImportCart.setValueAt(i + 1, i, 0);
            }
            updateCartTotals();
        } else {
            JOptionPane.showMessageDialog(this, "Vui lòng chọn dòng Serial cần xóa khỏi phiếu!", "Thông báo", JOptionPane.WARNING_MESSAGE);
        }
    }

    private void clearImportCart() {
        if (currentCartItems.isEmpty()) return;
        int confirm = JOptionPane.showConfirmDialog(this, "Bạn có chắc chắn muốn xóa toàn bộ danh sách chờ nhập kho này không?", "Xác Nhận Xóa", JOptionPane.YES_NO_OPTION);
        if (confirm == JOptionPane.YES_OPTION) {
            currentCartItems.clear();
            modelImportCart.setRowCount(0);
            updateCartTotals();
        }
    }

    private void updateCartTotals() {
        int qty = currentCartItems.size();
        double total = 0.0;
        for (ImportItem it : currentCartItems) {
            total += it.getImportPrice();
        }
        lblTotalQuantity.setText("Tổng số lượng: " + qty + " thiết bị");
        lblTotalAmount.setText("Tổng tiền nhập: " + CURRENCY_FORMAT.format(total));
    }

    private void executeConfirmImport() {
        if (currentCartItems.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Danh sách thiết bị nhập kho đang rỗng! Hãy thêm ít nhất 1 Serial.", "Cảnh báo", JOptionPane.WARNING_MESSAGE);
            return;
        }

        SupplierItem supItem = (SupplierItem) cboSupplier.getSelectedItem();
        if (supItem == null || supItem.getSupplier() == null) {
            JOptionPane.showMessageDialog(this, "Vui lòng chọn Nhà Cung Cấp!", "Cảnh báo", JOptionPane.WARNING_MESSAGE);
            return;
        }

        double total = 0.0;
        for (ImportItem it : currentCartItems) total += it.getImportPrice();

        int confirm = JOptionPane.showConfirmDialog(
                this,
                "Xác nhận nhập kho lô hàng gồm " + currentCartItems.size() + " thiết bị phần cứng?\n"
                        + "Nhà cung cấp: " + supItem.getSupplier().getName() + "\n"
                        + "Tổng chi phí nhập: " + CURRENCY_FORMAT.format(total) + "\n\n"
                        + "Hệ thống sẽ tự động lưu phiếu và kích hoạt " + currentCartItems.size() + " Serial mới sang trạng thái AVAILABLE.",
                "Xác Nhận Nhập Kho",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.QUESTION_MESSAGE
        );

        if (confirm != JOptionPane.YES_OPTION) {
            return;
        }

        btnConfirmImport.setEnabled(false);
        setCursor(new Cursor(Cursor.WAIT_CURSOR));

        String importId = importService.generateImportOrderId();
        String empId = UserSession.getInstance().isLoggedIn() ?
                UserSession.getInstance().getCurrentUser().getId() : "EMP_SYSTEM";

        ImportOrder order = new ImportOrder(
                importId,
                supItem.getSupplier().getId(),
                supItem.getSupplier().getName(),
                empId,
                LocalDate.now(),
                new ArrayList<>(currentCartItems),
                total,
                ImportOrder.STATUS_COMPLETED,
                txtImportNote.getText().trim()
        );

        new SwingWorker<Boolean, Void>() {
            @Override
            protected Boolean doInBackground() {
                return importService.processImportOrder(order);
            }

            @Override
            protected void done() {
                btnConfirmImport.setEnabled(true);
                setCursor(new Cursor(Cursor.DEFAULT_CURSOR));
                try {
                    boolean success = get();
                    if (success) {
                        JOptionPane.showMessageDialog(
                                ImportPanel.this,
                                "🎉 Nhập kho thành công!\n"
                                        + "Mã phiếu nhập: " + importId + "\n"
                                        + "Đã đưa " + order.getItemCount() + " thiết bị vào kho sẵn sàng bán / cho thuê.",
                                "Hoàn Tất Nhập Kho",
                                JOptionPane.INFORMATION_MESSAGE
                        );
                        // Reset giỏ
                        currentCartItems.clear();
                        modelImportCart.setRowCount(0);
                        updateCartTotals();
                        // Tải lại lịch sử
                        loadHistoryDataAsync();
                    } else {
                        JOptionPane.showMessageDialog(ImportPanel.this, "Quá trình nhập kho gặp sự cố! Vui lòng kiểm tra lại dữ liệu.", "Lỗi Xử Lý", JOptionPane.ERROR_MESSAGE);
                    }
                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(ImportPanel.this, "Lỗi kết nối CSDL: " + ex.getMessage(), "Lỗi Hệ Thống", JOptionPane.ERROR_MESSAGE);
                }
            }
        }.execute();
    }

    // =========================================================================
    // XỬ LÝ NGHIỆP VỤ & TƯƠNG TÁC GIAO DIỆN TAB 2 (LỊCH SỬ NHẬP KHO)
    // =========================================================================

    private void loadHistoryDataAsync() {
        new SwingWorker<List<ImportOrder>, Void>() {
            @Override
            protected List<ImportOrder> doInBackground() {
                return importService.getAllImportOrders();
            }

            @Override
            protected void done() {
                try {
                    List<ImportOrder> list = get();
                    importOrderCache.clear();
                    importOrderCache.addAll(list);
                    renderHistoryTable(importOrderCache);
                } catch (Exception ex) {
                    System.err.println("[ImportPanel] Lỗi tải lịch sử nhập kho: " + ex.getMessage());
                }
            }
        }.execute();
    }

    private void filterHistoryAsync() {
        String keyword = txtSearchHistory.getText().trim().toLowerCase();
        if (keyword.isEmpty()) {
            renderHistoryTable(importOrderCache);
            return;
        }
        List<ImportOrder> filtered = new ArrayList<>();
        for (ImportOrder o : importOrderCache) {
            boolean match = (o.getId() != null && o.getId().toLowerCase().contains(keyword))
                    || (o.getSupplierName() != null && o.getSupplierName().toLowerCase().contains(keyword))
                    || (o.getNote() != null && o.getNote().toLowerCase().contains(keyword))
                    || (o.getEmployeeId() != null && o.getEmployeeId().toLowerCase().contains(keyword));
            if (match) filtered.add(o);
        }
        renderHistoryTable(filtered);
    }

    private void renderHistoryTable(List<ImportOrder> list) {
        modelHistory.setRowCount(0);
        for (ImportOrder o : list) {
            String dateFormatted = o.getImportDate() != null ? o.getImportDate().format(DATE_FORMATTER) : "N/A";
            modelHistory.addRow(new Object[]{
                    o.getId(),
                    o.getSupplierName(),
                    dateFormatted,
                    o.getItemCount() + " thiết bị",
                    CURRENCY_FORMAT.format(o.getTotalAmount()),
                    o.getEmployeeId(),
                    o.getStatus()
            });
        }
    }

    private void showSelectedOrderDetailDialog() {
        int row = tblHistory.getSelectedRow();
        if (row < 0) {
            JOptionPane.showMessageDialog(this, "Vui lòng chọn một phiếu nhập kho từ bảng để xem chi tiết!", "Thông báo", JOptionPane.WARNING_MESSAGE);
            return;
        }

        String orderId = (String) modelHistory.getValueAt(row, 0);
        ImportOrder order = null;
        for (ImportOrder o : importOrderCache) {
            if (o.getId().equals(orderId)) {
                order = o;
                break;
            }
        }
        if (order == null) {
            order = importService.getImportOrderById(orderId);
        }
        if (order == null) {
            JOptionPane.showMessageDialog(this, "Không tìm thấy dữ liệu phiếu nhập kho: " + orderId, "Lỗi", JOptionPane.ERROR_MESSAGE);
            return;
        }

        // Mở Dialog chi tiết phiếu nhập
        Frame parent = (Frame) SwingUtilities.getWindowAncestor(this);
        ImportOrderDetailDialog dialog = new ImportOrderDetailDialog(parent, order);
        dialog.setVisible(true);
    }

    // =========================================================================
    // XỬ LÝ NGHIỆP VỤ & TƯƠNG TÁC GIAO DIỆN TAB 3 (NHÀ CUNG CẤP)
    // =========================================================================

    private void loadSuppliersAsync() {
        new SwingWorker<List<Supplier>, Void>() {
            @Override
            protected List<Supplier> doInBackground() {
                return importService.getAllSuppliers();
            }

            @Override
            protected void done() {
                try {
                    List<Supplier> list = get();
                    supplierCache.clear();
                    supplierCache.addAll(list);
                    renderSupplierTable(supplierCache);
                    reloadSupplierComboBox();
                } catch (Exception ex) {
                    System.err.println("[ImportPanel] Lỗi tải danh sách NCC: " + ex.getMessage());
                }
            }
        }.execute();
    }

    private void reloadSupplierComboBox() {
        cboSupplier.removeAllItems();
        for (Supplier s : supplierCache) {
            if (s.isActive()) {
                cboSupplier.addItem(new SupplierItem(s));
            }
        }
    }

    private void filterSupplierAsync() {
        String keyword = txtSearchSupplier.getText().trim().toLowerCase();
        if (keyword.isEmpty()) {
            renderSupplierTable(supplierCache);
            return;
        }
        List<Supplier> filtered = new ArrayList<>();
        for (Supplier s : supplierCache) {
            boolean match = (s.getName() != null && s.getName().toLowerCase().contains(keyword))
                    || (s.getPhone() != null && s.getPhone().contains(keyword))
                    || (s.getEmail() != null && s.getEmail().toLowerCase().contains(keyword))
                    || (s.getTaxCode() != null && s.getTaxCode().contains(keyword))
                    || (s.getAddress() != null && s.getAddress().toLowerCase().contains(keyword));
            if (match) filtered.add(s);
        }
        renderSupplierTable(filtered);
    }

    private void renderSupplierTable(List<Supplier> list) {
        modelSuppliers.setRowCount(0);
        for (Supplier s : list) {
            modelSuppliers.addRow(new Object[]{
                    s.getId() != null ? s.getId() : "N/A",
                    s.getName(),
                    s.getPhone(),
                    s.getEmail(),
                    s.getAddress(),
                    s.getTaxCode() != null ? s.getTaxCode() : "N/A",
                    s.getStatus()
            });
        }
    }

    private void editSelectedSupplier() {
        int row = tblSuppliers.getSelectedRow();
        if (row < 0) {
            JOptionPane.showMessageDialog(this, "Vui lòng chọn một Nhà Cung Cấp để sửa!", "Thông báo", JOptionPane.WARNING_MESSAGE);
            return;
        }
        String supId = (String) modelSuppliers.getValueAt(row, 0);
        Supplier supplier = null;
        for (Supplier s : supplierCache) {
            if (s.getId() != null && s.getId().equals(supId)) {
                supplier = s;
                break;
            }
        }
        if (supplier != null) {
            showSupplierDialog(supplier);
        }
    }

    private void toggleSupplierStatus() {
        int row = tblSuppliers.getSelectedRow();
        if (row < 0) {
            JOptionPane.showMessageDialog(this, "Vui lòng chọn một Nhà Cung Cấp để Khóa / Mở Khóa!", "Thông báo", JOptionPane.WARNING_MESSAGE);
            return;
        }
        String supId = (String) modelSuppliers.getValueAt(row, 0);
        Supplier supplier = null;
        for (Supplier s : supplierCache) {
            if (s.getId() != null && s.getId().equals(supId)) {
                supplier = s;
                break;
            }
        }
        if (supplier == null) return;

        String newStatus = supplier.isActive() ? Supplier.STATUS_INACTIVE : Supplier.STATUS_ACTIVE;
        String actionName = supplier.isActive() ? "Khóa đối tác" : "Kích hoạt lại đối tác";

        int confirm = JOptionPane.showConfirmDialog(
                this,
                "Bạn có chắc muốn " + actionName + ": " + supplier.getName() + "?",
                "Xác Nhận Thay Đổi Trạng Thái",
                JOptionPane.YES_NO_OPTION
        );

        if (confirm == JOptionPane.YES_OPTION) {
            supplier.setStatus(newStatus);

            // Khai báo biến final để SwingWorker có thể sử dụng an toàn
            final Supplier targetSupplier = supplier;

            new SwingWorker<Boolean, Void>() {
                @Override
                protected Boolean doInBackground() {
                    // Sử dụng targetSupplier thay cho supplier
                    return importService.updateSupplier(targetSupplier);
                }

                @Override
                protected void done() {
                    loadSuppliersAsync();
                }
            }.execute();
        }
    }

    private void showSupplierDialog(Supplier supplierToEdit) {
        boolean isEdit = (supplierToEdit != null);
        JDialog dialog = new JDialog((Frame) SwingUtilities.getWindowAncestor(this),
                isEdit ? "CẬP NHẬT THÔNG TIN NHÀ CUNG CẤP" : "THÊM MỚI NHÀ CUNG CẤP", true);
        dialog.setSize(520, 480);
        dialog.setLocationRelativeTo(this);
        dialog.setLayout(new BorderLayout());

        JPanel form = new JPanel(new GridLayout(6, 1, 0, 10));
        form.setBorder(new EmptyBorder(20, 24, 16, 24));
        form.setBackground(Color.WHITE);

        JTextField txtName = UIUtils.createTextField();
        JTextField txtPhone = UIUtils.createTextField();
        JTextField txtEmail = UIUtils.createTextField();
        JTextField txtAddress = UIUtils.createTextField();
        JTextField txtTaxCode = UIUtils.createTextField();
        JComboBox<String> cboStatus = new JComboBox<>(new String[]{Supplier.STATUS_ACTIVE, Supplier.STATUS_INACTIVE});
        cboStatus.setFont(UIUtils.FONT_BODY);

        if (isEdit) {
            txtName.setText(supplierToEdit.getName());
            txtPhone.setText(supplierToEdit.getPhone());
            txtEmail.setText(supplierToEdit.getEmail());
            txtAddress.setText(supplierToEdit.getAddress());
            txtTaxCode.setText(supplierToEdit.getTaxCode());
            cboStatus.setSelectedItem(supplierToEdit.getStatus());
        }

        form.add(createFieldContainer("1. Tên Công Ty / Nhà Cung Cấp (*):", txtName));
        form.add(createFieldContainer("2. Số Điện Thoại Liên Hệ (*):", txtPhone));
        form.add(createFieldContainer("3. Email Giao Dịch:", txtEmail));
        form.add(createFieldContainer("4. Địa Chỉ Trụ Sở:", txtAddress));
        form.add(createFieldContainer("5. Mã Số Thuế:", txtTaxCode));
        form.add(createFieldContainer("6. Trạng Thái Hợp Tác:", cboStatus));

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 12, 12));
        buttonPanel.setBackground(new Color(248, 250, 252));

        JButton btnCancel = new JButton("Hủy Bỏ");
        btnCancel.setFont(UIUtils.FONT_BODY);
        btnCancel.addActionListener(e -> dialog.dispose());

        JButton btnSave = new JButton(isEdit ? "Lưu Cập Nhật" : "Tạo Mới");
        btnSave.setFont(UIUtils.FONT_BODY_BOLD);
        btnSave.setBackground(UIUtils.COLOR_PRIMARY);
        btnSave.setForeground(Color.WHITE);
        btnSave.setFocusPainted(false);
        btnSave.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btnSave.addActionListener(e -> {
            String name = txtName.getText().trim();
            String phone = txtPhone.getText().trim();
            if (name.isEmpty()) {
                JOptionPane.showMessageDialog(dialog, "Tên Nhà Cung Cấp không được để trống!", "Cảnh báo", JOptionPane.WARNING_MESSAGE);
                txtName.requestFocus();
                return;
            }
            if (phone.isEmpty()) {
                JOptionPane.showMessageDialog(dialog, "Số điện thoại không được để trống!", "Cảnh báo", JOptionPane.WARNING_MESSAGE);
                txtPhone.requestFocus();
                return;
            }

            if (isEdit) {
                supplierToEdit.setName(name);
                supplierToEdit.setPhone(phone);
                supplierToEdit.setEmail(txtEmail.getText().trim());
                supplierToEdit.setAddress(txtAddress.getText().trim());
                supplierToEdit.setTaxCode(txtTaxCode.getText().trim());
                supplierToEdit.setStatus((String) cboStatus.getSelectedItem());

                new SwingWorker<Boolean, Void>() {
                    @Override protected Boolean doInBackground() { return importService.updateSupplier(supplierToEdit); }
                    @Override protected void done() {
                        dialog.dispose();
                        loadSuppliersAsync();
                    }
                }.execute();
            } else {
                Supplier newSup = new Supplier(name, phone, txtEmail.getText().trim(),
                        txtAddress.getText().trim(), txtTaxCode.getText().trim(), (String) cboStatus.getSelectedItem());

                new SwingWorker<Boolean, Void>() {
                    @Override protected Boolean doInBackground() { return importService.saveSupplier(newSup); }
                    @Override protected void done() {
                        dialog.dispose();
                        loadSuppliersAsync();
                    }
                }.execute();
            }
        });

        buttonPanel.add(btnCancel);
        buttonPanel.add(btnSave);

        dialog.add(form, BorderLayout.CENTER);
        dialog.add(buttonPanel, BorderLayout.SOUTH);
        dialog.setVisible(true);
    }

    // =========================================================================
    // TẢI DỮ LIỆU BAN ĐẦU ASYNC
    // =========================================================================

    private void loadInitialDataAsync() {
        new SwingWorker<Void, Void>() {
            private List<Supplier> suppliers;
            private List<Product> products;
            private List<ImportOrder> history;

            @Override
            protected Void doInBackground() {
                suppliers = importService.getAllSuppliers();
                products = productService.getAllProducts();
                history = importService.getAllImportOrders();
                return null;
            }

            @Override
            protected void done() {
                // Đổ Supplier
                if (suppliers != null) {
                    supplierCache.clear();
                    supplierCache.addAll(suppliers);
                    renderSupplierTable(supplierCache);
                    reloadSupplierComboBox();
                }

                // Đổ Sản phẩm
                if (products != null) {
                    productCache.clear();
                    productCache.addAll(products);
                    cboProduct.removeAllItems();
                    for (Product p : productCache) {
                        if (p.isActive()) {
                            cboProduct.addItem(new ProductItem(p));
                        }
                    }
                    onProductSelected();
                    generateRandomSerial();
                }

                // Đổ Lịch sử nhập
                if (history != null) {
                    importOrderCache.clear();
                    importOrderCache.addAll(history);
                    renderHistoryTable(importOrderCache);
                }
            }
        }.execute();
    }

    // =========================================================================
    // UI UTILS & HELPERS
    // =========================================================================

    private JPanel createFieldContainer(String labelText, JComponent field) {
        JPanel p = new JPanel(new BorderLayout(0, 4));
        p.setOpaque(false);
        JLabel lbl = new JLabel(labelText);
        lbl.setFont(UIUtils.FONT_BODY_BOLD);
        p.add(lbl, BorderLayout.NORTH);
        p.add(field, BorderLayout.CENTER);
        return p;
    }

    private void styleTable(JTable table) {
        table.setFont(UIUtils.FONT_BODY);
        table.setRowHeight(34);
        table.setGridColor(UIUtils.COLOR_BORDER);
        table.setShowGrid(true);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.setSelectionBackground(new Color(224, 242, 254)); // Sky 100
        table.setSelectionForeground(UIUtils.COLOR_TEXT_MAIN);

        JTableHeader header = table.getTableHeader();
        header.setFont(UIUtils.FONT_BODY_BOLD);
        header.setBackground(new Color(241, 245, 249)); // Slate 100
        header.setForeground(UIUtils.COLOR_TEXT_MAIN);
        header.setPreferredSize(new Dimension(0, 38));

        DefaultTableCellRenderer centerRenderer = new DefaultTableCellRenderer();
        centerRenderer.setHorizontalAlignment(SwingConstants.CENTER);
        table.getColumnModel().getColumn(0).setCellRenderer(centerRenderer);
    }

    // ComboBox Wrappers
    private static class SupplierItem {
        private final Supplier supplier;
        public SupplierItem(Supplier supplier) { this.supplier = supplier; }
        public Supplier getSupplier() { return supplier; }
        @Override
        public String toString() {
            return supplier.getName() + " (" + supplier.getPhone() + ")";
        }
    }

    private static class ProductItem {
        private final Product product;
        public ProductItem(Product product) { this.product = product; }
        public Product getProduct() { return product; }
        @Override
        public String toString() {
            return product.getProductName() + " [" + product.getBrand() + "] - Bán: "
                    + CURRENCY_FORMAT.format(product.getBasePrice());
        }
    }

    // =========================================================================
    // DIALOG XEM CHI TIẾT PHIẾU NHẬP KHO
    // =========================================================================

    private static class ImportOrderDetailDialog extends JDialog {

        public ImportOrderDetailDialog(Frame parent, ImportOrder order) {
            super(parent, "CHI TIẾT PHIẾU NHẬP KHO: " + order.getId(), true);
            setSize(780, 520);
            setLocationRelativeTo(parent);
            setLayout(new BorderLayout(0, 14));
            getContentPane().setBackground(UIUtils.COLOR_BG_LIGHT);
            ((JPanel) getContentPane()).setBorder(new EmptyBorder(16, 20, 16, 20));

            // Header tóm tắt
            JPanel headerPanel = new JPanel(new GridLayout(2, 2, 12, 8));
            headerPanel.setBackground(Color.WHITE);
            headerPanel.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(UIUtils.COLOR_BORDER, 1),
                    new EmptyBorder(12, 16, 12, 16)
            ));

            String dateStr = order.getImportDate() != null ? order.getImportDate().format(DATE_FORMATTER) : "N/A";
            headerPanel.add(new JLabel("Mã phiếu: " + order.getId()) {{ setFont(UIUtils.FONT_BODY_BOLD); }});
            headerPanel.add(new JLabel("Ngày nhập: " + dateStr) {{ setFont(UIUtils.FONT_BODY); }});
            headerPanel.add(new JLabel("Nhà cung cấp: " + order.getSupplierName()) {{ setFont(UIUtils.FONT_BODY); }});
            headerPanel.add(new JLabel("Tổng tiền hàng: " + CURRENCY_FORMAT.format(order.getTotalAmount())) {{
                setFont(UIUtils.FONT_BODY_BOLD);
                setForeground(UIUtils.COLOR_PRIMARY);
            }});

            // Bảng danh sách thiết bị
            String[] cols = {"STT", "Mã Sản Phẩm", "Tên Sản Phẩm", "Mã Serial Phần Cứng", "Đơn Giá Nhập"};
            DefaultTableModel model = new DefaultTableModel(cols, 0) {
                @Override public boolean isCellEditable(int r, int c) { return false; }
            };

            List<ImportItem> items = order.getItems();
            if (items != null) {
                for (int i = 0; i < items.size(); i++) {
                    ImportItem it = items.get(i);
                    model.addRow(new Object[]{
                            i + 1,
                            it.getProductId(),
                            it.getProductName(),
                            it.getSerialId(),
                            CURRENCY_FORMAT.format(it.getImportPrice())
                    });
                }
            }

            JTable table = new JTable(model);
            table.setFont(UIUtils.FONT_BODY);
            table.setRowHeight(32);
            table.getTableHeader().setFont(UIUtils.FONT_BODY_BOLD);
            table.getTableHeader().setBackground(new Color(241, 245, 249));

            JScrollPane scrollPane = new JScrollPane(table);
            scrollPane.setBorder(BorderFactory.createLineBorder(UIUtils.COLOR_BORDER, 1));

            // Footer đóng
            JPanel footer = new JPanel(new FlowLayout(FlowLayout.RIGHT));
            footer.setOpaque(false);
            JButton btnClose = UIUtils.createPrimaryButton("Đóng Cửa Sổ");
            btnClose.setPreferredSize(new Dimension(140, 36));
            btnClose.addActionListener(e -> dispose());
            footer.add(btnClose);

            add(headerPanel, BorderLayout.NORTH);
            add(scrollPane, BorderLayout.CENTER);
            add(footer, BorderLayout.SOUTH);
        }
    }
}
