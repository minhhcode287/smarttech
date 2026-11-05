package com.smarttech.view.panel;

import com.smarttech.model.*;
import com.smarttech.repository.CouponRepository;
import com.smarttech.repository.OrderRepository;
import com.smarttech.repository.RentalContractRepository;
import com.smarttech.service.CustomerService;
import com.smarttech.service.EquipmentService;
import com.smarttech.service.ExportService;
import com.smarttech.service.ProductService;
import com.smarttech.service.UserSession;
import com.smarttech.view.dialog.InvoicePreviewDialog;
import com.smarttech.view.util.UIUtils;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.event.AncestorEvent;
import javax.swing.event.AncestorListener;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

/**
 * Phân hệ Bán Hàng & Cho Thuê Thiết Bị Công Nghệ (OrderRentalPanel).
 * Đã khắc phục triệt để các lỗi UI, tính điểm real-time, áp dụng coupon tự động
 * và xác nhận đơn hàng trước khi lưu CSDL với 3 lựa chọn.
 *
 * @author Senior Java Developer & Software Architect
 * @project SmartTech
 */
public class OrderRentalPanel extends JPanel {

    private final ProductService productService;
    private final EquipmentService equipmentService;
    private final CustomerService customerService;
    private final OrderRepository orderRepository;
    private final RentalContractRepository contractRepository;
    private final ExportService exportService;
    private final CouponRepository couponRepository;

    // ================= COMPONENTS TAB BÁN HÀNG =================
    private JComboBox<CustomerItem> cboSaleCustomer;
    private JComboBox<ProductItem> cboSaleProduct;
    private JComboBox<String> cboSaleSerial;
    private JLabel lblSaleUnitPrice;
    private JTable saleCartTable;
    private DefaultTableModel saleCartModel;
    private final List<OrderItem> saleCartItems = new ArrayList<>();

    private JLabel lblSaleTotal;
    private JTextField txtSalePointsUse;
    private JTextField txtSaleCoupon;
    private Coupon appliedSaleCoupon = null;
    private JLabel lblSaleDiscount;
    private JLabel lblSaleFinalAmount;
    private JComboBox<String> cboSalePayment;

    // ================= COMPONENTS TAB CHO THUÊ =================
    private JComboBox<CustomerItem> cboRentCustomer;
    private JSpinner spnRentalDays;
    private JComboBox<ProductItem> cboRentProduct;
    private JComboBox<String> cboRentSerial;
    private JLabel lblRentRate;
    private JTextField txtRentDeposit;
    private JTable rentCartTable;
    private DefaultTableModel rentCartModel;
    private final List<RentalItem> rentCartItems = new ArrayList<>();

    private JLabel lblRentTotalDays;
    private JLabel lblRentTotalFee;
    private JLabel lblRentTotalDeposit;
    private JTextField txtRentCoupon;
    private Coupon appliedRentCoupon = null;
    private JLabel lblRentTotalPay;

    // ================= COMPONENTS TAB TRA CỨU & TRẢ MÁY =================
    private JTable contractTable;
    private DefaultTableModel contractModel;
    private JTextField txtSearchContract;

    public OrderRentalPanel() {
        this.productService = new ProductService();
        this.equipmentService = new EquipmentService();
        this.customerService = new CustomerService();
        this.orderRepository = new OrderRepository();
        this.contractRepository = new RentalContractRepository();
        this.exportService = new ExportService();
        this.couponRepository = new CouponRepository();

        initComponents();
        loadAllComboBoxes();
        loadContracts();

        // Tự động đồng bộ dữ liệu mới nhất từ CSDL khi chuyển sang tab OrderRentalPanel
        this.addAncestorListener(new AncestorListener() {
            @Override
            public void ancestorAdded(AncestorEvent event) {
                loadAllComboBoxes();
                loadContracts();
            }

            @Override
            public void ancestorRemoved(AncestorEvent event) {}

            @Override
            public void ancestorMoved(AncestorEvent event) {}
        });
    }

    private void initComponents() {
        setLayout(new BorderLayout());
        setBackground(UIUtils.COLOR_BG_LIGHT);

        JTabbedPane tabbedPane = new JTabbedPane();
        tabbedPane.setFont(UIUtils.FONT_BODY_BOLD);

        // TAB 1: BÁN HÀNG
        tabbedPane.addTab("🛒  Lập Hóa Đơn Bán Hàng", createSalePanel());

        // TAB 2: CHO THUÊ
        tabbedPane.addTab("📋  Lập Hợp Đồng Cho Thuê", createRentalPanel());

        // TAB 3: TRA CỨU & TRẢ THIẾT BỊ
        tabbedPane.addTab("🔄  Tra Cứu & Trả Thiết Bị", createContractListPanel());

        add(tabbedPane, BorderLayout.CENTER);
    }

    // =========================================================================
    // 1. GIAO DIỆN TAB LẬP HÓA ĐƠN BÁN HÀNG
    // =========================================================================
    private JPanel createSalePanel() {
        JPanel panel = new JPanel(new BorderLayout(16, 16));
        panel.setBackground(UIUtils.COLOR_BG_LIGHT);
        panel.setBorder(new EmptyBorder(16, 20, 16, 20));

        // KHUNG CHỌN SẢN PHẨM & SERIAL (BÊN TRÁI)
        JPanel leftPanel = new JPanel(new BorderLayout(0, 14));
        leftPanel.setPreferredSize(new Dimension(360, 0));
        leftPanel.setBackground(Color.WHITE);
        leftPanel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(UIUtils.COLOR_BORDER, 1),
                new EmptyBorder(16, 18, 16, 18)
        ));

        JPanel formLeft = new JPanel(new GridLayout(6, 1, 0, 10));
        formLeft.setOpaque(false);

        // Chọn khách hàng
        cboSaleCustomer = new JComboBox<>();
        cboSaleCustomer.setFont(UIUtils.FONT_BODY);
        cboSaleCustomer.addActionListener(e -> updateSaleTotal());

        // Chọn sản phẩm
        cboSaleProduct = new JComboBox<>();
        cboSaleProduct.setFont(UIUtils.FONT_BODY);
        cboSaleProduct.addActionListener(e -> onSaleProductChanged());

        // Chọn Serial khả dụng
        cboSaleSerial = new JComboBox<>();
        cboSaleSerial.setFont(UIUtils.FONT_BODY);

        lblSaleUnitPrice = new JLabel("Đơn giá: 0 đ");
        lblSaleUnitPrice.setFont(UIUtils.FONT_BODY_BOLD);
        lblSaleUnitPrice.setForeground(UIUtils.COLOR_PRIMARY);

        JButton btnAddToCart = UIUtils.createPrimaryButton("+ Thêm Vào Đơn Hàng");
        btnAddToCart.addActionListener(e -> addSaleItemToCart());

        formLeft.add(createFieldWrapper("1. Khách Hàng (*):", cboSaleCustomer));
        formLeft.add(createFieldWrapper("2. Sản Phẩm Bán:", cboSaleProduct));
        formLeft.add(createFieldWrapper("3. Số Serial Máy Khả Dụng (*):", cboSaleSerial));
        formLeft.add(lblSaleUnitPrice);
        formLeft.add(Box.createVerticalStrut(4));
        formLeft.add(btnAddToCart);

        leftPanel.add(new JLabel("THÔNG TIN ĐƠN BÁN HÀNG", SwingConstants.CENTER) {{
            setFont(UIUtils.FONT_HEADER);
            setForeground(UIUtils.COLOR_PRIMARY);
        }}, BorderLayout.NORTH);
        leftPanel.add(formLeft, BorderLayout.CENTER);

        // KHUNG GIỎ HÀNG & THANH TOÁN (BÊN PHẢI)
        JPanel rightPanel = new JPanel(new BorderLayout(0, 14));
        rightPanel.setOpaque(false);

        // Bảng Giỏ hàng
        String[] columns = {"Mã SP", "Tên Thiết Bị", "Serial Máy", "Đơn Giá", "Thành Tiền", "Thao Tác"};
        saleCartModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        saleCartTable = new JTable(saleCartModel);
        saleCartTable.setFont(UIUtils.FONT_BODY);
        saleCartTable.setRowHeight(34);
        saleCartTable.getTableHeader().setFont(UIUtils.FONT_BODY_BOLD);
        saleCartTable.getTableHeader().setBackground(new Color(241, 245, 249));

        JScrollPane scrollPane = new JScrollPane(saleCartTable);
        scrollPane.setBorder(BorderFactory.createLineBorder(UIUtils.COLOR_BORDER, 1));

        // Nút xóa món khỏi giỏ hàng
        JPanel cartActionPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        cartActionPanel.setOpaque(false);
        JButton btnRemoveSaleItem = new JButton("Xóa Món Chọn");
        btnRemoveSaleItem.setForeground(UIUtils.COLOR_DANGER);
        btnRemoveSaleItem.addActionListener(e -> {
            int row = saleCartTable.getSelectedRow();
            if (row != -1) {
                saleCartItems.remove(row);
                saleCartModel.removeRow(row);
                updateSaleTotal();
            }
        });
        cartActionPanel.add(btnRemoveSaleItem);

        // Thanh thanh toán chi tiết
        JPanel checkoutPanel = new JPanel(new GridLayout(2, 4, 16, 10));
        checkoutPanel.setBackground(Color.WHITE);
        checkoutPanel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(UIUtils.COLOR_BORDER, 1),
                new EmptyBorder(14, 18, 14, 18)
        ));

        lblSaleTotal = new JLabel("Tổng tiền: 0 đ");
        lblSaleTotal.setFont(UIUtils.FONT_BODY_BOLD);

        // Ô Dùng Điểm với DocumentListener tự động cập nhật Real-Time
        txtSalePointsUse = new JTextField("0", 6);
        txtSalePointsUse.setFont(UIUtils.FONT_BODY);
        txtSalePointsUse.getDocument().addDocumentListener(createRealtimeCalcListener(this::updateSaleTotal));

        // Ô Coupon với DocumentListener tự động tính Real-Time
        txtSaleCoupon = new JTextField(8);
        txtSaleCoupon.setFont(UIUtils.FONT_BODY);
        txtSaleCoupon.getDocument().addDocumentListener(createRealtimeCalcListener(this::updateSaleTotal));

        lblSaleDiscount = new JLabel("Giảm giá: 0 đ");
        lblSaleDiscount.setFont(UIUtils.FONT_BODY);
        lblSaleDiscount.setForeground(UIUtils.COLOR_SUCCESS);

        lblSaleFinalAmount = new JLabel("Cần thu: 0 đ");
        lblSaleFinalAmount.setFont(new Font("Segoe UI", Font.BOLD, 18));
        lblSaleFinalAmount.setForeground(UIUtils.COLOR_PRIMARY);

        cboSalePayment = new JComboBox<>(new String[]{"CASH (Tiền mặt)", "BANK_TRANSFER (Chuyển khoản)"});
        cboSalePayment.setFont(UIUtils.FONT_BODY);

        // Cấu hình Nút Xác Nhận Thanh Toán (Ép màu xanh lá)
        JButton btnCheckoutSale = UIUtils.createPrimaryButton("XÁC NHẬN BÁN");
        btnCheckoutSale.setBackground(new Color(22, 163, 74));
        btnCheckoutSale.setForeground(Color.WHITE);
        btnCheckoutSale.setOpaque(true);
        btnCheckoutSale.setFocusPainted(false);
        // Ghi đè style của FlatLaf
        btnCheckoutSale.putClientProperty("FlatLaf.style", "background: #16a34a; foreground: #ffffff; font: bold 13 $font.family");
        btnCheckoutSale.setPreferredSize(new Dimension(240, 40));
        btnCheckoutSale.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btnCheckoutSale.addActionListener(e -> executeSaleOrder());

        // Row 1
        checkoutPanel.add(lblSaleTotal);
        checkoutPanel.add(new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 0)) {{
            setOpaque(false);
            add(new JLabel("Dùng điểm:"));
            add(txtSalePointsUse);
        }});
        checkoutPanel.add(new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 0)) {{
            setOpaque(false);
            add(new JLabel("Coupon:"));
            add(txtSaleCoupon);
        }});
        checkoutPanel.add(lblSaleDiscount);

        // Row 2
        checkoutPanel.add(new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 0)) {{
            setOpaque(false);
            add(new JLabel("Phương thức:"));
            add(cboSalePayment);
        }});
        checkoutPanel.add(new JLabel(" "));
        checkoutPanel.add(lblSaleFinalAmount);
        checkoutPanel.add(btnCheckoutSale);

        rightPanel.add(scrollPane, BorderLayout.CENTER);
        rightPanel.add(cartActionPanel, BorderLayout.NORTH);
        rightPanel.add(checkoutPanel, BorderLayout.SOUTH);

        panel.add(leftPanel, BorderLayout.WEST);
        panel.add(rightPanel, BorderLayout.CENTER);
        return panel;
    }

    // =========================================================================
    // 2. GIAO DIỆN TAB LẬP HỢP ĐỒNG CHO THUÊ
    // =========================================================================
    private JPanel createRentalPanel() {
        JPanel panel = new JPanel(new BorderLayout(16, 16));
        panel.setBackground(UIUtils.COLOR_BG_LIGHT);
        panel.setBorder(new EmptyBorder(16, 20, 16, 20));

        // KHUNG NHẬP HỢP ĐỒNG (TRÁI)
        JPanel leftPanel = new JPanel(new BorderLayout(0, 14));
        leftPanel.setPreferredSize(new Dimension(380, 0));
        leftPanel.setBackground(Color.WHITE);
        leftPanel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(UIUtils.COLOR_BORDER, 1),
                new EmptyBorder(16, 18, 16, 18)
        ));

        JPanel formLeft = new JPanel(new GridLayout(7, 1, 0, 10));
        formLeft.setOpaque(false);

        cboRentCustomer = new JComboBox<>();
        cboRentCustomer.setFont(UIUtils.FONT_BODY);
        cboRentCustomer.addActionListener(e -> updateRentTotal());

        spnRentalDays = new JSpinner(new SpinnerNumberModel(3, 1, 90, 1));
        spnRentalDays.setFont(UIUtils.FONT_BODY);
        spnRentalDays.addChangeListener(e -> updateRentTotal());

        cboRentProduct = new JComboBox<>();
        cboRentProduct.setFont(UIUtils.FONT_BODY);
        cboRentProduct.addActionListener(e -> onRentProductChanged());

        cboRentSerial = new JComboBox<>();
        cboRentSerial.setFont(UIUtils.FONT_BODY);

        lblRentRate = new JLabel("Đơn giá thuê: 0 đ/ngày");
        lblRentRate.setFont(UIUtils.FONT_BODY_BOLD);
        lblRentRate.setForeground(UIUtils.COLOR_ACCENT);

        txtRentDeposit = new JTextField("5000000");
        txtRentDeposit.setFont(UIUtils.FONT_BODY);

        JButton btnAddRentItem = new JButton("+ Thêm Thiết Bị Vào Hợp Đồng");
        btnAddRentItem.setFont(UIUtils.FONT_BODY_BOLD);
        btnAddRentItem.setBackground(UIUtils.COLOR_ACCENT);
        btnAddRentItem.setForeground(Color.WHITE);
        btnAddRentItem.setFocusPainted(false);
        btnAddRentItem.addActionListener(e -> addRentalItemToCart());

        formLeft.add(createFieldWrapper("1. Khách Hàng Thuê (*):", cboRentCustomer));
        formLeft.add(createFieldWrapper("2. Số Ngày Thuê (Ngày):", spnRentalDays));
        formLeft.add(createFieldWrapper("3. Mẫu Sản Phẩm:", cboRentProduct));
        formLeft.add(createFieldWrapper("4. Serial Máy Trong Kho:", cboRentSerial));
        formLeft.add(lblRentRate);
        formLeft.add(createFieldWrapper("5. Tiền Cọc Giữ Máy (VNĐ):", txtRentDeposit));
        formLeft.add(btnAddRentItem);

        leftPanel.add(new JLabel("LẬP HỢP ĐỒNG THUÊ THIẾT BỊ", SwingConstants.CENTER) {{
            setFont(UIUtils.FONT_HEADER);
            setForeground(UIUtils.COLOR_ACCENT);
        }}, BorderLayout.NORTH);
        leftPanel.add(formLeft, BorderLayout.CENTER);

        // KHUNG DANH SÁCH THIẾT BỊ THUÊ & THANH TOÁN (PHẢI)
        JPanel rightPanel = new JPanel(new BorderLayout(0, 14));
        rightPanel.setOpaque(false);

        String[] rentColumns = {"Mã SP", "Tên Thiết Bị", "Serial Thuê", "Giá Thuê/Ngày", "Tiền Cọc Máy"};
        rentCartModel = new DefaultTableModel(rentColumns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        rentCartTable = new JTable(rentCartModel);
        rentCartTable.setFont(UIUtils.FONT_BODY);
        rentCartTable.setRowHeight(34);
        rentCartTable.getTableHeader().setFont(UIUtils.FONT_BODY_BOLD);
        rentCartTable.getTableHeader().setBackground(new Color(241, 245, 249));

        JScrollPane scrollPane = new JScrollPane(rentCartTable);
        scrollPane.setBorder(BorderFactory.createLineBorder(UIUtils.COLOR_BORDER, 1));

        JPanel checkoutRent = new JPanel(new GridLayout(2, 3, 16, 10));
        checkoutRent.setBackground(Color.WHITE);
        checkoutRent.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(UIUtils.COLOR_BORDER, 1),
                new EmptyBorder(14, 18, 14, 18)
        ));

        lblRentTotalDays = new JLabel("Thời gian thuê: 3 Ngày");
        lblRentTotalDays.setFont(UIUtils.FONT_BODY_BOLD);

        txtRentCoupon = new JTextField(8);
        txtRentCoupon.setFont(UIUtils.FONT_BODY);
        txtRentCoupon.getDocument().addDocumentListener(createRealtimeCalcListener(this::updateRentTotal));

        lblRentTotalFee = new JLabel("Tổng tiền thuê: 0 đ");
        lblRentTotalFee.setFont(UIUtils.FONT_BODY_BOLD);

        lblRentTotalDeposit = new JLabel("Tổng tiền cọc: 0 đ");
        lblRentTotalDeposit.setFont(UIUtils.FONT_BODY_BOLD);
        lblRentTotalDeposit.setForeground(UIUtils.COLOR_ACCENT);

        lblRentTotalPay = new JLabel("Tổng cần cọc & trả: 0 đ");
        lblRentTotalPay.setFont(new Font("Segoe UI", Font.BOLD, 17));
        lblRentTotalPay.setForeground(UIUtils.COLOR_PRIMARY);

        JButton btnSubmitContract = UIUtils.createPrimaryButton("LẬP HỢP ĐỒNG THUÊ");
        btnSubmitContract.setPreferredSize(new Dimension(220, 38));
        btnSubmitContract.addActionListener(e -> executeRentalContract());

        checkoutRent.add(lblRentTotalDays);
        checkoutRent.add(new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 0)) {{
            setOpaque(false);
            add(new JLabel("Coupon thuê:"));
            add(txtRentCoupon);
        }});
        checkoutRent.add(lblRentTotalFee);

        checkoutRent.add(lblRentTotalDeposit);
        checkoutRent.add(lblRentTotalPay);
        checkoutRent.add(btnSubmitContract);

        rightPanel.add(scrollPane, BorderLayout.CENTER);
        rightPanel.add(checkoutRent, BorderLayout.SOUTH);

        panel.add(leftPanel, BorderLayout.WEST);
        panel.add(rightPanel, BorderLayout.CENTER);
        return panel;
    }

    // =========================================================================
    // 3. GIAO DIỆN TAB TRA CỨU HỢP ĐỒNG & TRẢ THIẾT BỊ
    // =========================================================================
    private JPanel createContractListPanel() {
        JPanel panel = new JPanel(new BorderLayout(0, 14));
        panel.setBackground(UIUtils.COLOR_BG_LIGHT);
        panel.setBorder(new EmptyBorder(16, 20, 16, 20));

        // Toolbar tra cứu
        JPanel toolBar = new JPanel(new BorderLayout());
        toolBar.setOpaque(false);

        JPanel searchPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        searchPanel.setOpaque(false);

        txtSearchContract = UIUtils.createTextField();
        txtSearchContract.setPreferredSize(new Dimension(280, 36));
        txtSearchContract.setToolTipText("Tìm theo Mã hợp đồng hoặc SĐT khách hàng...");

        JButton btnSearchContract = new JButton("Tìm Kiếm");
        btnSearchContract.setFont(UIUtils.FONT_BODY_BOLD);
        btnSearchContract.addActionListener(e -> searchContracts());

        JButton btnRefreshContracts = new JButton("Làm Mới");
        btnRefreshContracts.setFont(UIUtils.FONT_BODY);
        btnRefreshContracts.addActionListener(e -> loadContracts());

        searchPanel.add(txtSearchContract);
        searchPanel.add(btnSearchContract);
        searchPanel.add(btnRefreshContracts);

        JPanel actionPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        actionPanel.setOpaque(false);

        JButton btnPrintSelectedContract = new JButton("🖨️  In Lại Hợp Đồng");
        btnPrintSelectedContract.setFont(UIUtils.FONT_BODY_BOLD);
        btnPrintSelectedContract.setBackground(Color.WHITE);
        btnPrintSelectedContract.setForeground(UIUtils.COLOR_TEXT_MAIN);
        btnPrintSelectedContract.setBorder(BorderFactory.createLineBorder(UIUtils.COLOR_BORDER, 1));
        btnPrintSelectedContract.setFocusPainted(false);
        btnPrintSelectedContract.setPreferredSize(new Dimension(175, 36));
        btnPrintSelectedContract.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btnPrintSelectedContract.addActionListener(e -> printSelectedContract());

        JButton btnReturnEquipment = new JButton("🔄  Trả Thiết Bị & Thanh Lý Hợp Đồng");
        btnReturnEquipment.setFont(UIUtils.FONT_BODY_BOLD);
        btnReturnEquipment.setBackground(UIUtils.COLOR_PRIMARY);
        btnReturnEquipment.setForeground(Color.WHITE);
        btnReturnEquipment.setFocusPainted(false);
        btnReturnEquipment.setPreferredSize(new Dimension(280, 36));
        btnReturnEquipment.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btnReturnEquipment.addActionListener(e -> executeReturnEquipment());

        actionPanel.add(btnPrintSelectedContract);
        actionPanel.add(btnReturnEquipment);

        toolBar.add(searchPanel, BorderLayout.WEST);
        toolBar.add(actionPanel, BorderLayout.EAST);

        // Bảng hợp đồng
        String[] contractCols = {"Mã HĐ", "Khách Hàng", "SĐT", "Ngày Bắt Đầu", "Hạn Trả Dự Kiến", "Ngày Trả Thực Tế", "Số Thiết Bị", "Tiền Thuê", "Tiền Cọc", "Trạng Thái"};
        contractModel = new DefaultTableModel(contractCols, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        contractTable = new JTable(contractModel);
        contractTable.setFont(UIUtils.FONT_BODY);
        contractTable.setRowHeight(36);
        contractTable.getTableHeader().setFont(UIUtils.FONT_BODY_BOLD);
        contractTable.getTableHeader().setBackground(new Color(241, 245, 249));

        JScrollPane scrollPane = new JScrollPane(contractTable);
        scrollPane.setBorder(BorderFactory.createLineBorder(UIUtils.COLOR_BORDER, 1));

        panel.add(toolBar, BorderLayout.NORTH);
        panel.add(scrollPane, BorderLayout.CENTER);
        return panel;
    }

    // =========================================================================
    // HELPER & LOGIC TÍNH TOÁN THEO THỜI GIAN THỰC (REAL-TIME LOGIC)
    // =========================================================================

    private DocumentListener createRealtimeCalcListener(Runnable action) {
        return new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) { action.run(); }
            @Override
            public void removeUpdate(DocumentEvent e) { action.run(); }
            @Override
            public void changedUpdate(DocumentEvent e) { action.run(); }
        };
    }

    private void loadAllComboBoxes() {
        if (cboSaleCustomer == null || cboRentCustomer == null || cboSaleProduct == null || cboRentProduct == null) {
            return;
        }

        cboSaleCustomer.removeAllItems();
        cboRentCustomer.removeAllItems();
        List<Customer> customers = customerService.getAllCustomers();
        for (Customer c : customers) {
            CustomerItem item = new CustomerItem(c);
            cboSaleCustomer.addItem(item);
            cboRentCustomer.addItem(item);
        }

        cboSaleProduct.removeAllItems();
        cboRentProduct.removeAllItems();
        List<Product> products = productService.getAllProducts();
        for (Product p : products) {
            ProductItem item = new ProductItem(p);
            cboSaleProduct.addItem(item);
            cboRentProduct.addItem(item);
        }

        onSaleProductChanged();
        onRentProductChanged();
    }

    private void onSaleProductChanged() {
        ProductItem item = (ProductItem) cboSaleProduct.getSelectedItem();
        cboSaleSerial.removeAllItems();
        if (item != null) {
            lblSaleUnitPrice.setText("Đơn giá bán: " + String.format("%,.0f đ", item.getProduct().getBasePrice()));
            List<Equipment> avail = equipmentService.getAvailableEquipments(item.getProduct().getId());
            for (Equipment eq : avail) {
                cboSaleSerial.addItem(eq.getId());
            }
            if (avail.isEmpty()) {
                cboSaleSerial.addItem("-- Hết máy sẵn sàng trong kho --");
            }
        }
    }

    private void onRentProductChanged() {
        ProductItem item = (ProductItem) cboRentProduct.getSelectedItem();
        cboRentSerial.removeAllItems();
        if (item != null) {
            lblRentRate.setText("Đơn giá thuê: " + String.format("%,.0f đ/ngày", item.getProduct().getRentalRatePerDay()));
            List<Equipment> avail = equipmentService.getAvailableEquipments(item.getProduct().getId());
            for (Equipment eq : avail) {
                cboRentSerial.addItem(eq.getId());
            }
            if (avail.isEmpty()) {
                cboRentSerial.addItem("-- Hết máy sẵn sàng cho thuê --");
            }
            double suggestedDeposit = Math.round((item.getProduct().getBasePrice() * 0.20) / 100000.0) * 100000.0;
            txtRentDeposit.setText(String.format("%.0f", Math.max(suggestedDeposit, 1000000.0)));
        }
    }

    private void addSaleItemToCart() {
        ProductItem prodItem = (ProductItem) cboSaleProduct.getSelectedItem();
        String serial = (String) cboSaleSerial.getSelectedItem();

        if (prodItem == null || serial == null || serial.startsWith("--")) {
            JOptionPane.showMessageDialog(this, "Vui lòng chọn sản phẩm có mã Serial hợp lệ trong kho!", "Cảnh báo", JOptionPane.WARNING_MESSAGE);
            return;
        }

        for (OrderItem oi : saleCartItems) {
            if (oi.getSerialId().equals(serial)) {
                JOptionPane.showMessageDialog(this, "Thiết bị có Serial này đã có trong đơn hàng!", "Trùng lặp", JOptionPane.WARNING_MESSAGE);
                return;
            }
        }

        OrderItem item = new OrderItem(
                prodItem.getProduct().getId(),
                prodItem.getProduct().getProductName(),
                serial,
                1,
                prodItem.getProduct().getBasePrice()
        );

        saleCartItems.add(item);
        saleCartModel.addRow(new Object[]{
                item.getProductId(),
                item.getProductName(),
                item.getSerialId(),
                String.format("%,.0f đ", item.getUnitPrice()),
                String.format("%,.0f đ", item.getSubtotal()),
                "Đã thêm"
        });

        updateSaleTotal();
    }

    /**
     * Cập nhật số tiền bán hàng tự động theo Real-Time khi nhập Điểm thưởng hoặc Coupon.
     */
    private void updateSaleTotal() {
        double subtotal = 0.0;
        for (OrderItem it : saleCartItems) {
            subtotal += it.getSubtotal();
        }

        CustomerItem custItem = (CustomerItem) cboSaleCustomer.getSelectedItem();
        Customer customer = custItem != null ? custItem.getCustomer() : null;

        // 1. Chiết khấu theo hạng thành viên (Loyalty Tier)
        double tierPercent = customer != null ? customerService.getTierDiscountPercent(customer) : 0.0;
        double tierDiscount = (subtotal * tierPercent) / 100.0;

        // 2. Chiết khấu theo Mã Giảm Giá (Coupon Real-Time Validation)
        double couponDiscount = 0.0;
        String couponCode = txtSaleCoupon != null ? txtSaleCoupon.getText().trim() : "";
        if (!couponCode.isEmpty()) {
            Coupon coupon = couponRepository.findByCode(couponCode);
            if (coupon != null && coupon.isValid(subtotal)) {
                appliedSaleCoupon = coupon;
                couponDiscount = coupon.calculateDiscount(subtotal);
            } else {
                appliedSaleCoupon = null;
            }
        } else {
            appliedSaleCoupon = null;
        }

        // 3. Giảm giá từ Điểm thưởng (1 điểm = 1.000 VNĐ Real-Time Validation)
        int points = 0;
        if (txtSalePointsUse != null) {
            try {
                points = Integer.parseInt(txtSalePointsUse.getText().trim());
            } catch (Exception ignored) {}
        }
        if (customer != null && points > customer.getRewardPoints()) {
            points = customer.getRewardPoints();
            SwingUtilities.invokeLater(() -> txtSalePointsUse.setText(String.valueOf(customer.getRewardPoints())));
        }
        if (points < 0) {
            points = 0;
            SwingUtilities.invokeLater(() -> txtSalePointsUse.setText("0"));
        }
        double pointsDiscount = points * 1000.0;

        double totalDiscount = tierDiscount + couponDiscount + pointsDiscount;
        double finalAmount = Math.max(subtotal - totalDiscount, 0.0);

        lblSaleTotal.setText("Tổng hàng: " + String.format("%,.0f đ", subtotal));
        String tierName = customer != null && customer.getCustomerType() != null ? customer.getCustomerType() : "BRONZE";

        StringBuilder discountText = new StringBuilder(String.format("Giảm: -%,.0f đ [VIP %s (%.0f%%)", totalDiscount, tierName, tierPercent));
        if (appliedSaleCoupon != null) {
            discountText.append(String.format(" + Mã %s (-%,.0f đ)", appliedSaleCoupon.getCode(), couponDiscount));
        }
        if (pointsDiscount > 0) {
            discountText.append(String.format(" + %,d pts (-%,.0f đ)", points, pointsDiscount));
        }
        discountText.append("]");

        lblSaleDiscount.setText(discountText.toString());
        lblSaleFinalAmount.setText("Cần thu: " + String.format("%,.0f đ", finalAmount));
    }

    /**
     * Xác nhận đơn hàng BÁN LẺ với 3 tùy chọn: LƯU & IN, CHỈ LƯU, HỦY BỎ.
     */
    private void executeSaleOrder() {
        if (saleCartItems.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Giỏ hàng bán đang rỗng!", "Cảnh báo", JOptionPane.WARNING_MESSAGE);
            return;
        }

        CustomerItem custItem = (CustomerItem) cboSaleCustomer.getSelectedItem();
        if (custItem == null) {
            JOptionPane.showMessageDialog(this, "Vui lòng chọn khách hàng!", "Cảnh báo", JOptionPane.WARNING_MESSAGE);
            return;
        }

        double total = 0.0;
        for (OrderItem it : saleCartItems) total += it.getSubtotal();

        Customer customer = custItem.getCustomer();
        double tierPercent = customerService.getTierDiscountPercent(customer);
        double tierDiscount = (total * tierPercent) / 100.0;

        double couponDiscount = 0.0;
        if (appliedSaleCoupon != null && appliedSaleCoupon.isValid(total)) {
            couponDiscount = appliedSaleCoupon.calculateDiscount(total);
        }

        int pointsUsed = 0;
        try { pointsUsed = Integer.parseInt(txtSalePointsUse.getText().trim()); } catch (Exception ignored) {}
        if (pointsUsed > customer.getRewardPoints()) pointsUsed = customer.getRewardPoints();
        if (pointsUsed < 0) pointsUsed = 0;
        double pointsDiscount = pointsUsed * 1000.0;

        double totalDiscount = tierDiscount + couponDiscount + pointsDiscount;
        double finalAmount = Math.max(total - totalDiscount, 0.0);

        // HỘP THOẠI 3 LỰA CHỌN XÁC NHẬN
        Object[] options = {"Lưu & In Hóa Đơn", "Chỉ Lưu (Không In)", "Hủy Bỏ"};
        int choice = JOptionPane.showOptionDialog(
                this,
                "XÁC NHẬN TẠO ĐƠN HÀNG BÁN HÀNG?\n\n"
                        + "• Khách hàng: " + customer.getFullName() + " (" + customer.getPhone() + ")\n"
                        + "• Tổng tiền hàng: " + String.format("%,.0f đ", total) + "\n"
                        + "• Tổng chiết khấu giảm giá: -" + String.format("%,.0f đ", totalDiscount) + "\n"
                        + "• Số tiền thực thu: " + String.format("%,.0f đ", finalAmount) + "\n"
                        + "• Phương thức thanh toán: " + cboSalePayment.getSelectedItem() + "\n\n"
                        + "Chọn thao tác tiếp theo bên dưới:",
                "Xác Nhận Thanh Toán",
                JOptionPane.YES_NO_CANCEL_OPTION,
                JOptionPane.QUESTION_MESSAGE,
                null,
                options,
                options[0]
        );

        // Bấm Hủy Bỏ (Index 2) hoặc Bấm X Tắt Dialog (CLOSED_OPTION) -> KHÔNG LÀM GÌ CẢ
        if (choice == 2 || choice == JOptionPane.CLOSED_OPTION) {
            return;
        }

        // Bấm Lưu (Kể cả In hay Không In) -> Bắt đầu tiến trình lưu CSDL
        String orderId = "HDB-" + System.currentTimeMillis() % 1000000;
        String empId = UserSession.getInstance().isLoggedIn() ? UserSession.getInstance().getCurrentUser().getId() : "EMP_SYSTEM";

        Order order = new Order(
                orderId,
                customer.getId(),
                customer.getFullName(),
                customer.getPhone(),
                empId,
                LocalDate.now(),
                new ArrayList<>(saleCartItems),
                total,
                pointsUsed,
                totalDiscount,
                finalAmount,
                (String) cboSalePayment.getSelectedItem(),
                "PAID"
        );

        boolean saved = orderRepository.save(order);
        if (saved) {
            if (appliedSaleCoupon != null) {
                appliedSaleCoupon.setUsedCount(appliedSaleCoupon.getUsedCount() + 1);
                couponRepository.update(appliedSaleCoupon);
                appliedSaleCoupon = null;
            }

            for (OrderItem it : saleCartItems) {
                equipmentService.changeEquipmentStatus(it.getSerialId(), Equipment.STATUS_SOLD, "Bán theo hóa đơn: " + orderId);
            }

            if (pointsUsed > 0) {
                customerService.deductPoints(customer.getId(), pointsUsed);
            }
            customerService.accumulatePointsFromOrder(customer.getId(), finalAmount);

            // NẾU CHỌN LƯU & IN HÓA ĐƠN (Index 0) -> Mở Preview
            if (choice == 0) {
                Employee employee = null;
                if (UserSession.getInstance().isLoggedIn() && UserSession.getInstance().getCurrentUser() instanceof Employee) {
                    employee = (Employee) UserSession.getInstance().getCurrentUser();
                }
                String htmlInvoice = exportService.generateSaleInvoiceHtml(order, customer, employee);
                Frame parentFrame = (Frame) SwingUtilities.getWindowAncestor(this);
                InvoicePreviewDialog dialog = new InvoicePreviewDialog(parentFrame, "Hóa Đơn Bán Hàng - " + orderId, htmlInvoice);
                dialog.setVisible(true);
            } else {
                // NẾU CHỌN CHỈ LƯU (Index 1) -> Hiện thông báo nhỏ xác nhận
                JOptionPane.showMessageDialog(this, "✅ Đã lưu hóa đơn thành công! (Không xuất hóa đơn)", "Thành công", JOptionPane.INFORMATION_MESSAGE);
            }

            saleCartItems.clear();
            saleCartModel.setRowCount(0);
            txtSalePointsUse.setText("0");
            txtSaleCoupon.setText("");
            loadAllComboBoxes();
            updateSaleTotal();
        }
    }

    private void addRentalItemToCart() {
        ProductItem prodItem = (ProductItem) cboRentProduct.getSelectedItem();
        String serial = (String) cboRentSerial.getSelectedItem();

        if (prodItem == null || serial == null || serial.startsWith("--")) {
            JOptionPane.showMessageDialog(this, "Vui lòng chọn thiết bị có Serial sẵn sàng cho thuê!", "Cảnh báo", JOptionPane.WARNING_MESSAGE);
            return;
        }

        double deposit = 0.0;
        try {
            deposit = Double.parseDouble(txtRentDeposit.getText().trim());
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Tiền cọc phải là số hợp lệ!", "Lỗi", JOptionPane.ERROR_MESSAGE);
            return;
        }

        for (RentalItem ri : rentCartItems) {
            if (ri.getSerialId().equals(serial)) {
                JOptionPane.showMessageDialog(this, "Serial này đã được thêm vào hợp đồng!", "Trùng", JOptionPane.WARNING_MESSAGE);
                return;
            }
        }

        RentalItem item = new RentalItem(
                prodItem.getProduct().getId(),
                prodItem.getProduct().getProductName(),
                serial,
                prodItem.getProduct().getRentalRatePerDay(),
                deposit
        );

        rentCartItems.add(item);
        rentCartModel.addRow(new Object[]{
                item.getProductId(),
                item.getProductName(),
                item.getSerialId(),
                String.format("%,.0f đ/ngày", item.getRentalRatePerDay()),
                String.format("%,.0f đ", item.getDepositAmount())
        });

        updateRentTotal();
    }

    private void updateRentTotal() {
        int days = (Integer) spnRentalDays.getValue();
        double totalFeePerDay = 0.0;
        double totalDeposit = 0.0;

        for (RentalItem ri : rentCartItems) {
            totalFeePerDay += ri.getRentalRatePerDay();
            totalDeposit += ri.getDepositAmount();
        }

        double rawRentFee = totalFeePerDay * days;

        CustomerItem custItem = (CustomerItem) cboRentCustomer.getSelectedItem();
        Customer customer = custItem != null ? custItem.getCustomer() : null;

        double tierPercent = customer != null ? customerService.getTierDiscountPercent(customer) : 0.0;
        double tierDiscount = (rawRentFee * tierPercent) / 100.0;

        double couponDiscount = 0.0;
        String couponCode = txtRentCoupon != null ? txtRentCoupon.getText().trim() : "";
        if (!couponCode.isEmpty()) {
            Coupon coupon = couponRepository.findByCode(couponCode);
            if (coupon != null && coupon.isValid(rawRentFee)) {
                appliedRentCoupon = coupon;
                couponDiscount = coupon.calculateDiscount(rawRentFee);
            } else {
                appliedRentCoupon = null;
            }
        } else {
            appliedRentCoupon = null;
        }

        double totalRentDiscount = tierDiscount + couponDiscount;
        double finalRentFee = Math.max(rawRentFee - totalRentDiscount, 0.0);
        double totalPay = finalRentFee + totalDeposit;

        lblRentTotalDays.setText("Thời gian thuê: " + days + " Ngày");
        lblRentTotalFee.setText(String.format("Tiền thuê: %,.0f đ (Giảm: -%,.0f đ)", finalRentFee, totalRentDiscount));
        lblRentTotalDeposit.setText("Tổng tiền cọc: " + String.format("%,.0f đ", totalDeposit));
        lblRentTotalPay.setText("Tổng cần cọc & trả: " + String.format("%,.0f đ", totalPay));
    }

    /**
     * Xác nhận đơn THUÊ MÁY với 3 tùy chọn: LƯU & IN, CHỈ LƯU, HỦY BỎ.
     */
    private void executeRentalContract() {
        if (rentCartItems.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Chưa chọn thiết bị nào cho hợp đồng thuê!", "Cảnh báo", JOptionPane.WARNING_MESSAGE);
            return;
        }

        CustomerItem custItem = (CustomerItem) cboRentCustomer.getSelectedItem();
        if (custItem == null) {
            JOptionPane.showMessageDialog(this, "Vui lòng chọn khách hàng thuê!", "Cảnh báo", JOptionPane.WARNING_MESSAGE);
            return;
        }

        int days = (Integer) spnRentalDays.getValue();
        double totalFeePerDay = 0.0;
        double totalDeposit = 0.0;
        for (RentalItem ri : rentCartItems) {
            totalFeePerDay += ri.getRentalRatePerDay();
            totalDeposit += ri.getDepositAmount();
        }
        double rawRentFee = totalFeePerDay * days;

        Customer customer = custItem.getCustomer();
        double tierPercent = customerService.getTierDiscountPercent(customer);
        double tierDiscount = (rawRentFee * tierPercent) / 100.0;

        double couponDiscount = 0.0;
        if (appliedRentCoupon != null && appliedRentCoupon.isValid(rawRentFee)) {
            couponDiscount = appliedRentCoupon.calculateDiscount(rawRentFee);
        }
        double totalRentDiscount = tierDiscount + couponDiscount;
        double finalRentFee = Math.max(rawRentFee - totalRentDiscount, 0.0);

        // HỘP THOẠI 3 TÙY CHỌN XÁC NHẬN HỢP ĐỒNG THUÊ
        Object[] options = {"Lưu & In Hợp Đồng", "Chỉ Lưu (Không In)", "Hủy Bỏ"};
        int confirmChoice = JOptionPane.showOptionDialog(
                this,
                "XÁC NHẬN LẬP HỢP ĐỒNG CHO THUÊ THIẾT BỊ?\n\n"
                        + "• Khách thuê: " + customer.getFullName() + " (" + customer.getPhone() + ")\n"
                        + "• Số ngày thuê: " + days + " ngày\n"
                        + "• Tiền cọc giữ máy: " + String.format("%,.0f đ", totalDeposit) + "\n"
                        + "• Tiền thuê sau chiết khấu: " + String.format("%,.0f đ", finalRentFee) + "\n\n"
                        + "Chọn thao tác tiếp theo bên dưới:",
                "Xác Nhận Hợp Đồng Thuê",
                JOptionPane.YES_NO_CANCEL_OPTION,
                JOptionPane.QUESTION_MESSAGE,
                null,
                options,
                options[0]
        );

        if (confirmChoice == 2 || confirmChoice == JOptionPane.CLOSED_OPTION) {
            return;
        }

        String contractId = "HDT-" + System.currentTimeMillis() % 1000000;
        String empId = UserSession.getInstance().isLoggedIn() ? UserSession.getInstance().getCurrentUser().getId() : "EMP_SYSTEM";

        RentalContract contract = new RentalContract(
                contractId,
                customer.getId(),
                customer.getFullName(),
                customer.getPhone(),
                empId,
                LocalDate.now(),
                LocalDate.now().plusDays(days),
                days,
                new ArrayList<>(rentCartItems),
                finalRentFee,
                totalDeposit,
                RentalContract.STATUS_ACTIVE,
                "Hợp đồng thuê thiết bị " + days + " ngày (Đã giảm: " + String.format("%,.0f đ", totalRentDiscount) + ")"
        );

        boolean saved = contractRepository.save(contract);
        if (saved) {
            if (appliedRentCoupon != null) {
                appliedRentCoupon.setUsedCount(appliedRentCoupon.getUsedCount() + 1);
                couponRepository.update(appliedRentCoupon);
                appliedRentCoupon = null;
            }

            for (RentalItem ri : rentCartItems) {
                equipmentService.changeEquipmentStatus(ri.getSerialId(), Equipment.STATUS_RENTED, "Cho thuê theo hợp đồng: " + contractId);
            }

            if (confirmChoice == 0) {
                Employee employee = null;
                if (UserSession.getInstance().isLoggedIn() && UserSession.getInstance().getCurrentUser() instanceof Employee) {
                    employee = (Employee) UserSession.getInstance().getCurrentUser();
                }
                String htmlContract = exportService.generateRentalContractHtml(contract, customer, employee);
                Frame parentFrame = (Frame) SwingUtilities.getWindowAncestor(this);
                InvoicePreviewDialog dialog = new InvoicePreviewDialog(parentFrame, "Hợp Đồng Cho Thuê - " + contractId, htmlContract);
                dialog.setVisible(true);
            } else {
                JOptionPane.showMessageDialog(this, "✅ Đã lưu Hợp đồng thành công! (Không xuất hợp đồng)", "Thành công", JOptionPane.INFORMATION_MESSAGE);
            }

            rentCartItems.clear();
            rentCartModel.setRowCount(0);
            txtRentCoupon.setText("");
            updateRentTotal();
            onRentProductChanged();
            loadContracts();
        }
    }

    private void loadContracts() {
        contractModel.setRowCount(0);
        List<RentalContract> list = contractRepository.findAll();
        for (RentalContract c : list) {
            contractModel.addRow(new Object[]{
                    c.getId(),
                    c.getCustomerName(),
                    c.getCustomerPhone(),
                    c.getStartDate(),
                    c.getExpectedEndDate(),
                    c.getActualEndDate() != null ? c.getActualEndDate() : "Chưa trả",
                    c.getItems() != null ? c.getItems().size() + " máy" : "0",
                    String.format("%,.0f đ", c.getTotalRentalFee()),
                    String.format("%,.0f đ", c.getTotalDeposit()),
                    c.getStatus()
            });
        }
    }

    private void searchContracts() {
        String keyword = txtSearchContract.getText().trim();
        if (keyword.isEmpty()) {
            loadContracts();
            return;
        }

        contractModel.setRowCount(0);
        List<RentalContract> all = contractRepository.findAll();
        for (RentalContract c : all) {
            if ((c.getId() != null && c.getId().contains(keyword))
                    || (c.getCustomerPhone() != null && c.getCustomerPhone().contains(keyword))
                    || (c.getCustomerName() != null && c.getCustomerName().toLowerCase().contains(keyword.toLowerCase()))) {
                contractModel.addRow(new Object[]{
                        c.getId(),
                        c.getCustomerName(),
                        c.getCustomerPhone(),
                        c.getStartDate(),
                        c.getExpectedEndDate(),
                        c.getActualEndDate() != null ? c.getActualEndDate() : "Chưa trả",
                        c.getItems() != null ? c.getItems().size() + " máy" : "0",
                        String.format("%,.0f đ", c.getTotalRentalFee()),
                        String.format("%,.0f đ", c.getTotalDeposit()),
                        c.getStatus()
                });
            }
        }
    }

    private void executeReturnEquipment() {
        int row = contractTable.getSelectedRow();
        if (row == -1) {
            JOptionPane.showMessageDialog(this, "Vui lòng chọn một Hợp đồng từ bảng để làm thủ tục trả máy!", "Thông báo", JOptionPane.WARNING_MESSAGE);
            return;
        }

        String contractId = (String) contractModel.getValueAt(row, 0);
        RentalContract contract = contractRepository.findById(contractId);

        if (contract == null) {
            JOptionPane.showMessageDialog(this, "Không tìm thấy hợp đồng!", "Lỗi", JOptionPane.ERROR_MESSAGE);
            return;
        }

        if (RentalContract.STATUS_RETURNED.equals(contract.getStatus())) {
            JOptionPane.showMessageDialog(this, "Hợp đồng này đã được hoàn tất và trả máy trước đó!", "Thông báo", JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        LocalDate now = LocalDate.now();
        long overdueDays = 0;
        if (contract.getExpectedEndDate() != null && now.isAfter(contract.getExpectedEndDate())) {
            overdueDays = ChronoUnit.DAYS.between(contract.getExpectedEndDate(), now);
        }

        double extraPenalty = overdueDays * (contract.getTotalRentalFee() / Math.max(contract.getRentalDays(), 1));
        double refundAmount = Math.max(contract.getTotalDeposit() - extraPenalty, 0.0);

        int confirm = JOptionPane.showConfirmDialog(
                this,
                "Xác nhận nhận lại thiết bị cho hợp đồng: " + contractId + "?\n"
                        + "Khách hàng: " + contract.getCustomerName() + "\n"
                        + "Quá hạn trả máy: " + overdueDays + " ngày (Phí phụ trội: " + String.format("%,.0f đ", extraPenalty) + ")\n"
                        + "Tiền cọc cần hoàn trả lại cho khách: " + String.format("%,.0f đ", refundAmount),
                "Thủ Tục Trả Thiết Bị & Thanh Lý Hợp Đồng",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.QUESTION_MESSAGE
        );

        if (confirm == JOptionPane.YES_OPTION) {
            contract.setStatus(RentalContract.STATUS_RETURNED);
            contract.setActualEndDate(now);
            contract.setPenaltyFee(extraPenalty);
            contract.setRefundDeposit(refundAmount);
            contractRepository.update(contract);

            if (contract.getItems() != null) {
                for (RentalItem ri : contract.getItems()) {
                    equipmentService.changeEquipmentStatus(ri.getSerialId(), Equipment.STATUS_AVAILABLE, "Đã nhận lại máy từ hợp đồng: " + contractId);
                }
            }

            customerService.accumulatePointsFromOrder(contract.getCustomerId(), contract.getTotalRentalFee() + extraPenalty);

            JOptionPane.showMessageDialog(this, "Thanh lý hợp đồng và nhận lại thiết bị thành công!", "Hoàn Tất", JOptionPane.INFORMATION_MESSAGE);
            loadContracts();
            onRentProductChanged();
            onSaleProductChanged();
        }
    }

    private void printSelectedContract() {
        int selectedRow = contractTable.getSelectedRow();
        if (selectedRow < 0) {
            JOptionPane.showMessageDialog(this,
                    "Vui lòng chọn một hợp đồng trên bảng để in!",
                    "Chưa Chọn Hợp Đồng", JOptionPane.WARNING_MESSAGE);
            return;
        }

        String contractId = (String) contractModel.getValueAt(selectedRow, 0);
        RentalContract contract = contractRepository.findById(contractId);
        if (contract == null) {
            JOptionPane.showMessageDialog(this,
                    "Không tìm thấy dữ liệu hợp đồng: " + contractId,
                    "Lỗi Truy Vấn", JOptionPane.ERROR_MESSAGE);
            return;
        }

        Customer customer = customerService.getCustomerById(contract.getCustomerId());
        Employee employee = null;
        if (UserSession.getInstance().isLoggedIn() && UserSession.getInstance().getCurrentUser() instanceof Employee) {
            employee = (Employee) UserSession.getInstance().getCurrentUser();
        }

        String htmlContract = exportService.generateRentalContractHtml(contract, customer, employee);
        Frame parentFrame = (Frame) SwingUtilities.getWindowAncestor(this);
        InvoicePreviewDialog dialog = new InvoicePreviewDialog(parentFrame, "Hợp Đồng - " + contractId, htmlContract);
        dialog.setVisible(true);
    }

    private JPanel createFieldWrapper(String label, JComponent comp) {
        JPanel p = new JPanel(new BorderLayout(0, 4));
        p.setOpaque(false);
        JLabel lbl = new JLabel(label);
        lbl.setFont(UIUtils.FONT_BODY_BOLD);
        p.add(lbl, BorderLayout.NORTH);
        p.add(comp, BorderLayout.CENTER);
        return p;
    }

    private static class CustomerItem {
        private final Customer customer;
        public CustomerItem(Customer customer) { this.customer = customer; }
        public Customer getCustomer() { return customer; }
        @Override
        public String toString() {
            String tier = customer.getCustomerType() != null ? customer.getCustomerType() : "BRONZE";
            return customer.getFullName() + " (" + customer.getPhone() + ") [" + tier + " • " + customer.getRewardPoints() + " pts]";
        }
    }

    private static class ProductItem {
        private final Product product;
        public ProductItem(Product product) { this.product = product; }
        public Product getProduct() { return product; }
        @Override
        public String toString() {
            return product.getProductName() + " [" + product.getBrand() + "]";
        }
    }
}