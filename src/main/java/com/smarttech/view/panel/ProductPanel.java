package com.smarttech.view.panel;

import com.smarttech.model.Category;
import com.smarttech.model.Equipment;
import com.smarttech.model.Product;
import com.smarttech.service.CategoryService;
import com.smarttech.service.EquipmentService;
import com.smarttech.service.ProductService;
import com.smarttech.view.util.UIUtils;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.List;

/**
 * Giao diện Quản lý Sản phẩm &amp; Thiết bị phần cứng (ProductPanel).
 * Tích hợp đầy đủ CRUD Sản phẩm, lọc theo danh mục, tìm kiếm và quản lý Serial phần cứng tương ứng.
 *
 * @author Senior Java Developer &amp; Software Architect
 * @project SmartTech
 */
public class ProductPanel extends JPanel {

    private final ProductService productService;
    private final CategoryService categoryService;
    private final EquipmentService equipmentService;

    // Components
    private JTextField txtSearch;
    private JComboBox<CategoryItem> cboCategories;
    private JTable productTable;
    private DefaultTableModel productTableModel;

    private JTable equipmentTable;
    private DefaultTableModel equipmentTableModel;
    private JLabel lblEquipmentHeader;

    private Product selectedProduct = null;

    public ProductPanel() {
        this.productService = new ProductService();
        this.categoryService = new CategoryService();
        this.equipmentService = new EquipmentService();

        initComponents();
        loadCategories();
        loadProducts();
    }

    private void initComponents() {
        setLayout(new BorderLayout(0, 16));
        setBackground(UIUtils.COLOR_BG_LIGHT);
        setBorder(new EmptyBorder(20, 24, 20, 24));

        // ================= 1. HEADER =================
        JPanel headerPanel = new JPanel(new BorderLayout());
        headerPanel.setOpaque(false);

        JLabel lblTitle = new JLabel("Quản Lý Sản Phẩm & Thiết Bị Phần Cứng");
        lblTitle.setFont(UIUtils.FONT_TITLE);
        lblTitle.setForeground(UIUtils.COLOR_TEXT_MAIN);

        JLabel lblSub = new JLabel("Quản lý danh mục thiết bị, thông số kỹ thuật, bảng giá và theo dõi trạng thái từng Serial trong kho.");
        lblSub.setFont(UIUtils.FONT_SUBTITLE);
        lblSub.setForeground(UIUtils.COLOR_TEXT_MUTED);

        headerPanel.add(lblTitle, BorderLayout.NORTH);
        headerPanel.add(lblSub, BorderLayout.SOUTH);
        add(headerPanel, BorderLayout.NORTH);

        // ================= 2. TOP TOOLBAR =================
        JPanel toolBar = new JPanel(new BorderLayout());
        toolBar.setOpaque(false);

        // Thanh tìm kiếm và lọc
        JPanel filterPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        filterPanel.setOpaque(false);

        txtSearch = UIUtils.createTextField();
        txtSearch.setPreferredSize(new Dimension(240, 36));
        txtSearch.setToolTipText("Nhập tên sản phẩm cần tìm...");

        JButton btnSearch = new JButton("Tìm Kiếm");
        btnSearch.setFont(UIUtils.FONT_BODY_BOLD);
        btnSearch.setBackground(Color.WHITE);
        btnSearch.setBorder(BorderFactory.createLineBorder(UIUtils.COLOR_BORDER, 1));
        btnSearch.setPreferredSize(new Dimension(90, 36));
        btnSearch.addActionListener(e -> executeSearch());

        cboCategories = new JComboBox<>();
        cboCategories.setFont(UIUtils.FONT_BODY);
        cboCategories.setPreferredSize(new Dimension(190, 36));
        cboCategories.addActionListener(e -> filterByCategory());

        JButton btnRefresh = new JButton("Làm Mới");
        btnRefresh.setFont(UIUtils.FONT_BODY);
        btnRefresh.setBackground(Color.WHITE);
        btnRefresh.setBorder(BorderFactory.createLineBorder(UIUtils.COLOR_BORDER, 1));
        btnRefresh.setPreferredSize(new Dimension(90, 36));
        btnRefresh.addActionListener(e -> {
            txtSearch.setText("");
            if (cboCategories.getItemCount() > 0) {
                cboCategories.setSelectedIndex(0);
            }
            loadProducts();
        });

        filterPanel.add(txtSearch);
        filterPanel.add(btnSearch);
        filterPanel.add(cboCategories);
        filterPanel.add(btnRefresh);

        // Các nút thêm / sửa / xóa sản phẩm
        JPanel actionPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        actionPanel.setOpaque(false);

        JButton btnAddProduct = UIUtils.createPrimaryButton("+ Thêm Sản Phẩm");
        btnAddProduct.setPreferredSize(new Dimension(160, 36));
        btnAddProduct.addActionListener(e -> showProductDialog(null));

        JButton btnEditProduct = new JButton("Sửa Sản Phẩm");
        btnEditProduct.setFont(UIUtils.FONT_BODY_BOLD);
        btnEditProduct.setBackground(Color.WHITE);
        btnEditProduct.setBorder(BorderFactory.createLineBorder(UIUtils.COLOR_BORDER, 1));
        btnEditProduct.setPreferredSize(new Dimension(130, 36));
        btnEditProduct.addActionListener(e -> {
            if (selectedProduct == null) {
                JOptionPane.showMessageDialog(this, "Vui lòng chọn một sản phẩm từ bảng để sửa!", "Thông báo", JOptionPane.WARNING_MESSAGE);
                return;
            }
            showProductDialog(selectedProduct);
        });

        JButton btnDeleteProduct = new JButton("Xóa");
        btnDeleteProduct.setFont(UIUtils.FONT_BODY_BOLD);
        btnDeleteProduct.setForeground(UIUtils.COLOR_DANGER);
        btnDeleteProduct.setBackground(Color.WHITE);
        btnDeleteProduct.setBorder(BorderFactory.createLineBorder(UIUtils.COLOR_DANGER, 1));
        btnDeleteProduct.setPreferredSize(new Dimension(75, 36));
        btnDeleteProduct.addActionListener(e -> deleteSelectedProduct());

        actionPanel.add(btnAddProduct);
        actionPanel.add(btnEditProduct);
        actionPanel.add(btnDeleteProduct);

        toolBar.add(filterPanel, BorderLayout.WEST);
        toolBar.add(actionPanel, BorderLayout.EAST);

        // ================= 3. SPLIT WORKSPACE (TABLE PRODUCT TRÊN + TABLE SERIAL DƯỚI) =================
        JSplitPane splitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT);
        splitPane.setResizeWeight(0.55);
        splitPane.setDividerSize(8);
        splitPane.setBorder(null);

        // --- BẢNG SẢN PHẨM (TRÊN) ---
        JPanel productCard = new JPanel(new BorderLayout(0, 8));
        productCard.setBackground(Color.WHITE);
        productCard.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(UIUtils.COLOR_BORDER, 1),
                new EmptyBorder(12, 14, 12, 14)
        ));

        JLabel lblProductTableTitle = new JLabel("Danh Sách Mẫu Sản Phẩm Công Nghệ");
        lblProductTableTitle.setFont(UIUtils.FONT_HEADER);
        lblProductTableTitle.setForeground(UIUtils.COLOR_TEXT_MAIN);
        productCard.add(lblProductTableTitle, BorderLayout.NORTH);

        String[] prodColumns = {"Mã SP", "Tên Sản Phẩm", "Danh Mục", "Thương Hiệu", "Giá Bán Lẻ (VNĐ)", "Đơn Giá Thuê/Ngày", "Cấu Hình / Thông Số", "Trạng Thái"};
        productTableModel = new DefaultTableModel(prodColumns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        productTable = new JTable(productTableModel);
        productTable.setFont(UIUtils.FONT_BODY);
        productTable.setRowHeight(36);
        productTable.getTableHeader().setFont(UIUtils.FONT_BODY_BOLD);
        productTable.getTableHeader().setBackground(new Color(241, 245, 249));
        productTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        productTable.setShowVerticalLines(false);
        productTable.setGridColor(UIUtils.COLOR_BORDER);

        // Sự kiện khi người dùng chọn 1 dòng sản phẩm
        productTable.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                int row = productTable.getSelectedRow();
                if (row != -1) {
                    String prodId = (String) productTableModel.getValueAt(row, 0);
                    selectedProduct = productService.getProductById(prodId);
                    loadEquipmentsForSelectedProduct();
                }
            }
        });

        JScrollPane prodScroll = new JScrollPane(productTable);
        prodScroll.setBorder(BorderFactory.createLineBorder(UIUtils.COLOR_BORDER, 1));
        productCard.add(prodScroll, BorderLayout.CENTER);

        // --- BẢNG SERIAL THIẾT BỊ (DƯỚI) ---
        JPanel equipmentCard = new JPanel(new BorderLayout(0, 8));
        equipmentCard.setBackground(Color.WHITE);
        equipmentCard.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(UIUtils.COLOR_BORDER, 1),
                new EmptyBorder(12, 14, 12, 14)
        ));

        JPanel equipHeader = new JPanel(new BorderLayout());
        equipHeader.setOpaque(false);

        lblEquipmentHeader = new JLabel("Danh Sách Thiết Bị Cụ Thể (Vui lòng chọn sản phẩm phía trên)");
        lblEquipmentHeader.setFont(UIUtils.FONT_HEADER);
        lblEquipmentHeader.setForeground(UIUtils.COLOR_TEXT_MAIN);

        JPanel equipActionPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        equipActionPanel.setOpaque(false);

        JButton btnAddSerial = new JButton("+ Nhập Serial Mới");
        btnAddSerial.setFont(UIUtils.FONT_BODY_BOLD);
        btnAddSerial.setBackground(UIUtils.COLOR_ACCENT);
        btnAddSerial.setForeground(Color.WHITE);
        btnAddSerial.setFocusPainted(false);
        btnAddSerial.setPreferredSize(new Dimension(145, 30));
        btnAddSerial.addActionListener(e -> showAddSerialDialog());

        JButton btnChangeStatus = new JButton("Đổi Trạng Thái");
        btnChangeStatus.setFont(UIUtils.FONT_BODY);
        btnChangeStatus.setBackground(Color.WHITE);
        btnChangeStatus.setBorder(BorderFactory.createLineBorder(UIUtils.COLOR_BORDER, 1));
        btnChangeStatus.setPreferredSize(new Dimension(115, 30));
        btnChangeStatus.addActionListener(e -> showChangeStatusDialog());

        JButton btnDeleteSerial = new JButton("Xóa Serial");
        btnDeleteSerial.setFont(UIUtils.FONT_BODY);
        btnDeleteSerial.setForeground(UIUtils.COLOR_DANGER);
        btnDeleteSerial.setBackground(Color.WHITE);
        btnDeleteSerial.setBorder(BorderFactory.createLineBorder(UIUtils.COLOR_DANGER, 1));
        btnDeleteSerial.setPreferredSize(new Dimension(90, 30));
        btnDeleteSerial.addActionListener(e -> deleteSelectedEquipment());

        equipActionPanel.add(btnAddSerial);
        equipActionPanel.add(btnChangeStatus);
        equipActionPanel.add(btnDeleteSerial);

        equipHeader.add(lblEquipmentHeader, BorderLayout.WEST);
        equipHeader.add(equipActionPanel, BorderLayout.EAST);
        equipmentCard.add(equipHeader, BorderLayout.NORTH);

        String[] equipColumns = {"Mã Serial Phần Cứng", "Mã Sản Phẩm", "Trạng Thái Hoạt Động", "Ghi Chú Tình Trạng Thiết Bị"};
        equipmentTableModel = new DefaultTableModel(equipColumns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        equipmentTable = new JTable(equipmentTableModel);
        equipmentTable.setFont(UIUtils.FONT_BODY);
        equipmentTable.setRowHeight(32);
        equipmentTable.getTableHeader().setFont(UIUtils.FONT_BODY_BOLD);
        equipmentTable.getTableHeader().setBackground(new Color(241, 245, 249));
        equipmentTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        equipmentTable.setShowVerticalLines(false);
        equipmentTable.setGridColor(UIUtils.COLOR_BORDER);

        JScrollPane equipScroll = new JScrollPane(equipmentTable);
        equipScroll.setBorder(BorderFactory.createLineBorder(UIUtils.COLOR_BORDER, 1));
        equipmentCard.add(equipScroll, BorderLayout.CENTER);

        splitPane.setTopComponent(productCard);
        splitPane.setBottomComponent(equipmentCard);

        JPanel centerContainer = new JPanel(new BorderLayout(0, 12));
        centerContainer.setOpaque(false);
        centerContainer.add(toolBar, BorderLayout.NORTH);
        centerContainer.add(splitPane, BorderLayout.CENTER);

        add(centerContainer, BorderLayout.CENTER);
    }

    // ================= DỮ LIỆU & NGHIỆP VỤ =================

    private void loadCategories() {
        cboCategories.removeAllItems();
        cboCategories.addItem(new CategoryItem("", "Tất Cả Danh Mục"));
        List<Category> list = categoryService.getAllCategories();
        for (Category cat : list) {
            cboCategories.addItem(new CategoryItem(cat.getId(), cat.getCategoryName()));
        }
    }

    public void loadProducts() {
        productTableModel.setRowCount(0);
        List<Product> products = productService.getAllProducts();
        renderProducts(products);
    }

    private void renderProducts(List<Product> products) {
        productTableModel.setRowCount(0);
        for (Product p : products) {
            productTableModel.addRow(new Object[]{
                    p.getId(),
                    p.getProductName(),
                    p.getCategoryId(),
                    p.getBrand(),
                    String.format("%,.0f đ", p.getBasePrice()),
                    String.format("%,.0f đ/ngày", p.getRentalRatePerDay()),
                    p.getSpecifications(),
                    p.isActive() ? "Đang Kinh Doanh" : "Ngưng Kinh Doanh"
            });
        }
        equipmentTableModel.setRowCount(0);
        selectedProduct = null;
        lblEquipmentHeader.setText("Danh Sách Thiết Bị Cụ Thể (Vui lòng chọn sản phẩm phía trên)");
    }

    private void executeSearch() {
        String keyword = txtSearch.getText().trim();
        if (keyword.isEmpty()) {
            loadProducts();
            return;
        }
        List<Product> list = productService.searchByName(keyword);
        renderProducts(list);
    }

    private void filterByCategory() {
        CategoryItem item = (CategoryItem) cboCategories.getSelectedItem();
        if (item == null || item.getId().isEmpty()) {
            loadProducts();
            return;
        }
        List<Product> list = productService.getProductsByCategory(item.getId());
        renderProducts(list);
    }

    private void loadEquipmentsForSelectedProduct() {
        if (selectedProduct == null) {
            return;
        }
        lblEquipmentHeader.setText("Kho Serial Của Sản Phẩm: " + selectedProduct.getProductName() + " (" + selectedProduct.getId() + ")");
        equipmentTableModel.setRowCount(0);

        List<Equipment> equipments = equipmentService.getEquipmentsByProductId(selectedProduct.getId());
        for (Equipment eq : equipments) {
            equipmentTableModel.addRow(new Object[]{
                    eq.getId(),
                    eq.getProductId(),
                    formatStatus(eq.getStatus()),
                    eq.getConditionNote()
            });
        }
    }

    private String formatStatus(String status) {
        if (Equipment.STATUS_AVAILABLE.equals(status)) return "Sẵn Sàng (AVAILABLE)";
        if (Equipment.STATUS_RENTED.equals(status)) return "Đang Thuê (RENTED)";
        if (Equipment.STATUS_SOLD.equals(status)) return "Đã Bán (SOLD)";
        if (Equipment.STATUS_MAINTENANCE.equals(status)) return "Bảo Trì (MAINTENANCE)";
        if (Equipment.STATUS_BROKEN.equals(status)) return "Hỏng Hóc (BROKEN)";
        return status;
    }

    private void deleteSelectedProduct() {
        if (selectedProduct == null) {
            JOptionPane.showMessageDialog(this, "Vui lòng chọn sản phẩm cần xóa!", "Cảnh báo", JOptionPane.WARNING_MESSAGE);
            return;
        }

        int confirm = JOptionPane.showConfirmDialog(
                this,
                "Bạn có chắc chắn muốn xóa sản phẩm '" + selectedProduct.getProductName() + "'?\n(Lưu ý: Không thể hoàn tác)",
                "Xác nhận xóa",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.WARNING_MESSAGE
        );

        if (confirm == JOptionPane.YES_OPTION) {
            boolean success = productService.deleteProduct(selectedProduct.getId());
            if (success) {
                JOptionPane.showMessageDialog(this, "Đã xóa sản phẩm thành công!");
                loadProducts();
            } else {
                JOptionPane.showMessageDialog(this, "Xóa sản phẩm thất bại!", "Lỗi", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void deleteSelectedEquipment() {
        int row = equipmentTable.getSelectedRow();
        if (row == -1) {
            JOptionPane.showMessageDialog(this, "Vui lòng chọn một Serial thiết bị từ bảng dưới!", "Cảnh báo", JOptionPane.WARNING_MESSAGE);
            return;
        }

        String serialId = (String) equipmentTableModel.getValueAt(row, 0);
        int confirm = JOptionPane.showConfirmDialog(
                this,
                "Bạn có chắc muốn xóa thiết bị có Serial '" + serialId + "'?",
                "Xác nhận xóa",
                JOptionPane.YES_NO_OPTION
        );

        if (confirm == JOptionPane.YES_OPTION) {
            boolean ok = equipmentService.deleteEquipment(serialId);
            if (ok) {
                JOptionPane.showMessageDialog(this, "Đã xóa thiết bị thành công!");
                loadEquipmentsForSelectedProduct();
            }
        }
    }

    // ================= FORM DIALOGS =================

    private void showProductDialog(Product productToEdit) {
        boolean isEdit = (productToEdit != null);
        JDialog dialog = new JDialog((Frame) SwingUtilities.getWindowAncestor(this), isEdit ? "Sửa Sản Phẩm" : "Thêm Sản Phẩm Mới", true);
        dialog.setSize(480, 520);
        dialog.setLocationRelativeTo(this);
        dialog.setLayout(new BorderLayout());

        JPanel form = new JPanel(new GridLayout(7, 2, 10, 14));
        form.setBorder(new EmptyBorder(20, 24, 20, 24));
        form.setBackground(Color.WHITE);

        JTextField txtId = UIUtils.createTextField();
        JTextField txtName = UIUtils.createTextField();
        JTextField txtBrand = UIUtils.createTextField();
        JTextField txtPrice = UIUtils.createTextField();
        JTextField txtRentalRate = UIUtils.createTextField();
        JTextField txtSpecs = UIUtils.createTextField();

        JComboBox<CategoryItem> cboCatDialog = new JComboBox<>();
        cboCatDialog.setFont(UIUtils.FONT_BODY);
        List<Category> catList = categoryService.getAllCategories();
        for (Category c : catList) {
            cboCatDialog.addItem(new CategoryItem(c.getId(), c.getCategoryName()));
        }

        if (isEdit) {
            txtId.setText(productToEdit.getId());
            txtId.setEnabled(false);
            txtName.setText(productToEdit.getProductName());
            txtBrand.setText(productToEdit.getBrand());
            txtPrice.setText(String.valueOf(productToEdit.getBasePrice()));
            txtRentalRate.setText(String.valueOf(productToEdit.getRentalRatePerDay()));
            txtSpecs.setText(productToEdit.getSpecifications());

            for (int i = 0; i < cboCatDialog.getItemCount(); i++) {
                if (cboCatDialog.getItemAt(i).getId().equals(productToEdit.getCategoryId())) {
                    cboCatDialog.setSelectedIndex(i);
                    break;
                }
            }
        } else {
            txtId.setText("PROD-" + System.currentTimeMillis() % 100000);
        }

        form.add(new JLabel("Mã Sản Phẩm:"));
        form.add(txtId);
        form.add(new JLabel("Tên Sản Phẩm (*):"));
        form.add(txtName);
        form.add(new JLabel("Danh Mục:"));
        form.add(cboCatDialog);
        form.add(new JLabel("Thương Hiệu:"));
        form.add(txtBrand);
        form.add(new JLabel("Giá Bán Lẻ (VNĐ):"));
        form.add(txtPrice);
        form.add(new JLabel("Đơn Giá Thuê/Ngày (VNĐ):"));
        form.add(txtRentalRate);
        form.add(new JLabel("Cấu Hình Chi Tiết:"));
        form.add(txtSpecs);

        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 12, 12));
        JButton btnSave = UIUtils.createPrimaryButton("Lưu Dữ Liệu");
        JButton btnCancel = new JButton("Hủy Bỏ");
        btnCancel.setFont(UIUtils.FONT_BODY);
        btnCancel.addActionListener(e -> dialog.dispose());

        btnSave.addActionListener(e -> {
            String name = txtName.getText().trim();
            if (name.isEmpty()) {
                JOptionPane.showMessageDialog(dialog, "Tên sản phẩm không được để trống!", "Lỗi", JOptionPane.ERROR_MESSAGE);
                return;
            }

            double basePrice = 0.0;
            double rentalRate = 0.0;
            try {
                basePrice = Double.parseDouble(txtPrice.getText().trim());
                rentalRate = Double.parseDouble(txtRentalRate.getText().trim());
            } catch (NumberFormatException ex) {
                JOptionPane.showMessageDialog(dialog, "Giá bán và đơn giá thuê phải là số hợp lệ!", "Lỗi", JOptionPane.ERROR_MESSAGE);
                return;
            }

            CategoryItem selCat = (CategoryItem) cboCatDialog.getSelectedItem();
            String catId = selCat != null ? selCat.getId() : "";

            if (isEdit) {
                productToEdit.setProductName(name);
                productToEdit.setCategoryId(catId);
                productToEdit.setBrand(txtBrand.getText().trim());
                productToEdit.setBasePrice(basePrice);
                productToEdit.setRentalRatePerDay(rentalRate);
                productToEdit.setSpecifications(txtSpecs.getText().trim());

                if (productService.updateProduct(productToEdit)) {
                    JOptionPane.showMessageDialog(this, "Cập nhật sản phẩm thành công!");
                    dialog.dispose();
                    loadProducts();
                }
            } else {
                Product newProd = new Product(
                        txtId.getText().trim(),
                        catId,
                        name,
                        txtBrand.getText().trim(),
                        basePrice,
                        rentalRate,
                        txtSpecs.getText().trim(),
                        true
                );

                if (productService.createProduct(newProd)) {
                    JOptionPane.showMessageDialog(this, "Thêm sản phẩm mới thành công!");
                    dialog.dispose();
                    loadProducts();
                }
            }
        });

        btnPanel.add(btnCancel);
        btnPanel.add(btnSave);

        dialog.add(form, BorderLayout.CENTER);
        dialog.add(btnPanel, BorderLayout.SOUTH);
        dialog.setVisible(true);
    }

    private void showAddSerialDialog() {
        if (selectedProduct == null) {
            JOptionPane.showMessageDialog(this, "Vui lòng chọn một sản phẩm ở bảng trên trước khi thêm Serial!", "Cảnh báo", JOptionPane.WARNING_MESSAGE);
            return;
        }

        JDialog dialog = new JDialog((Frame) SwingUtilities.getWindowAncestor(this), "Nhập Serial Thiết Bị Mới", true);
        dialog.setSize(420, 280);
        dialog.setLocationRelativeTo(this);
        dialog.setLayout(new BorderLayout());

        JPanel form = new JPanel(new GridLayout(3, 2, 10, 16));
        form.setBorder(new EmptyBorder(24, 24, 20, 24));
        form.setBackground(Color.WHITE);

        JTextField txtSerial = UIUtils.createTextField();
        txtSerial.setText("SN-" + System.currentTimeMillis() % 1000000);

        JTextField txtNote = UIUtils.createTextField();
        txtNote.setText("Máy mới 100%, nguyên phụ kiện");

        JComboBox<String> cboStatus = new JComboBox<>(new String[]{
                Equipment.STATUS_AVAILABLE,
                Equipment.STATUS_MAINTENANCE
        });
        cboStatus.setFont(UIUtils.FONT_BODY);

        form.add(new JLabel("Mã Serial Phần Cứng (*):"));
        form.add(txtSerial);
        form.add(new JLabel("Trạng Thái Ban Đầu:"));
        form.add(cboStatus);
        form.add(new JLabel("Ghi Chú Tình Trạng Máy:"));
        form.add(txtNote);

        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 12));
        JButton btnSave = UIUtils.createPrimaryButton("Thêm Serial");
        JButton btnCancel = new JButton("Hủy");
        btnCancel.addActionListener(e -> dialog.dispose());

        btnSave.addActionListener(e -> {
            String serial = txtSerial.getText().trim();
            if (serial.isEmpty()) {
                JOptionPane.showMessageDialog(dialog, "Mã Serial không được rỗng!", "Lỗi", JOptionPane.ERROR_MESSAGE);
                return;
            }

            Equipment eq = new Equipment(
                    serial,
                    selectedProduct.getId(),
                    (String) cboStatus.getSelectedItem(),
                    txtNote.getText().trim(),
                    true
            );

            boolean success = equipmentService.addEquipment(eq);
            if (success) {
                JOptionPane.showMessageDialog(this, "Đã nhập Serial thành công vào kho!");
                dialog.dispose();
                loadEquipmentsForSelectedProduct();
            } else {
                JOptionPane.showMessageDialog(dialog, "Mã Serial đã tồn tại hoặc xảy ra lỗi!", "Lỗi", JOptionPane.ERROR_MESSAGE);
            }
        });

        btnPanel.add(btnCancel);
        btnPanel.add(btnSave);

        dialog.add(form, BorderLayout.CENTER);
        dialog.add(btnPanel, BorderLayout.SOUTH);
        dialog.setVisible(true);
    }

    private void showChangeStatusDialog() {
        int row = equipmentTable.getSelectedRow();
        if (row == -1) {
            JOptionPane.showMessageDialog(this, "Vui lòng chọn thiết bị Serial cần đổi trạng thái!", "Cảnh báo", JOptionPane.WARNING_MESSAGE);
            return;
        }

        String serialId = (String) equipmentTableModel.getValueAt(row, 0);

        JComboBox<String> cboNewStatus = new JComboBox<>(new String[]{
                Equipment.STATUS_AVAILABLE,
                Equipment.STATUS_RENTED,
                Equipment.STATUS_MAINTENANCE,
                Equipment.STATUS_BROKEN,
                Equipment.STATUS_SOLD
        });

        JTextField txtNote = new JTextField();

        JPanel panel = new JPanel(new GridLayout(2, 2, 10, 10));
        panel.add(new JLabel("Trạng Thái Mới:"));
        panel.add(cboNewStatus);
        panel.add(new JLabel("Ghi Chú Bổ Sung:"));
        panel.add(txtNote);

        int result = JOptionPane.showConfirmDialog(
                this, panel, "Cập Nhật Trạng Thái Cho Serial: " + serialId, JOptionPane.OK_CANCEL_OPTION
        );

        if (result == JOptionPane.OK_OPTION) {
            String newStatus = (String) cboNewStatus.getSelectedItem();
            boolean ok = equipmentService.changeEquipmentStatus(serialId, newStatus, txtNote.getText().trim());
            if (ok) {
                JOptionPane.showMessageDialog(this, "Đã đổi trạng thái thiết bị thành công!");
                loadEquipmentsForSelectedProduct();
            }
        }
    }

    // Helper item cho ComboBox Category
    private static class CategoryItem {
        private final String id;
        private final String name;

        public CategoryItem(String id, String name) {
            this.id = id;
            this.name = name;
        }

        public String getId() {
            return id;
        }

        @Override
        public String toString() {
            return name;
        }
    }
}
