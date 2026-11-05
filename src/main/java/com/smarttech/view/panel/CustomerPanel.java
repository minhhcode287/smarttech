package com.smarttech.view.panel;

import com.smarttech.model.Customer;
import com.smarttech.service.CustomerService;
import com.smarttech.view.util.UIUtils;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.List;

/**
 * Giao diện Quản lý Khách hàng và Điểm thưởng thành viên (CustomerPanel).
 * Tích hợp tìm kiếm theo Số điện thoại / Tên, lọc theo phân loại (Cá nhân / Doanh nghiệp),
 * và hộp thoại Thêm / Sửa / Xóa khách hàng trực tiếp vào MongoDB.
 *
 * @author Senior Java Developer &amp; Software Architect
 * @project SmartTech
 */
public class CustomerPanel extends JPanel {

    private final CustomerService customerService;

    // Components
    private JTextField txtSearch;
    private JComboBox<String> cboTypeFilter;
    private JTable customerTable;
    private DefaultTableModel tableModel;

    private Customer selectedCustomer = null;

    public CustomerPanel() {
        this.customerService = new CustomerService();
        initComponents();
        loadCustomers();
    }

    private void initComponents() {
        setLayout(new BorderLayout(0, 16));
        setBackground(UIUtils.COLOR_BG_LIGHT);
        setBorder(new EmptyBorder(20, 24, 20, 24));

        // ================= 1. HEADER =================
        JPanel headerPanel = new JPanel(new BorderLayout());
        headerPanel.setOpaque(false);

        JLabel lblTitle = new JLabel("Quản Lý Khách Hàng & Điểm Thưởng");
        lblTitle.setFont(UIUtils.FONT_TITLE);
        lblTitle.setForeground(UIUtils.COLOR_TEXT_MAIN);

        JLabel lblSub = new JLabel("Lưu trữ hồ sơ khách hàng, phân hạng Cá nhân / Doanh nghiệp và tích lũy điểm thưởng thành viên.");
        lblSub.setFont(UIUtils.FONT_SUBTITLE);
        lblSub.setForeground(UIUtils.COLOR_TEXT_MUTED);

        headerPanel.add(lblTitle, BorderLayout.NORTH);
        headerPanel.add(lblSub, BorderLayout.SOUTH);
        add(headerPanel, BorderLayout.NORTH);

        // ================= 2. TOOLBAR =================
        JPanel toolBar = new JPanel(new BorderLayout());
        toolBar.setOpaque(false);

        // Filter / Search panel
        JPanel searchBox = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        searchBox.setOpaque(false);

        txtSearch = UIUtils.createTextField();
        txtSearch.setPreferredSize(new Dimension(260, 36));
        txtSearch.setToolTipText("Tìm theo Số điện thoại hoặc Tên khách hàng...");

        JButton btnSearch = new JButton("Tìm Kiếm");
        btnSearch.setFont(UIUtils.FONT_BODY_BOLD);
        btnSearch.setBackground(Color.WHITE);
        btnSearch.setBorder(BorderFactory.createLineBorder(UIUtils.COLOR_BORDER, 1));
        btnSearch.setPreferredSize(new Dimension(90, 36));
        btnSearch.addActionListener(e -> executeSearch());

        cboTypeFilter = new JComboBox<>(new String[]{"Tất Cả Loại", "INDIVIDUAL", "ENTERPRISE"});
        cboTypeFilter.setFont(UIUtils.FONT_BODY);
        cboTypeFilter.setPreferredSize(new Dimension(160, 36));
        cboTypeFilter.addActionListener(e -> executeFilter());

        JButton btnRefresh = new JButton("Làm Mới");
        btnRefresh.setFont(UIUtils.FONT_BODY);
        btnRefresh.setBackground(Color.WHITE);
        btnRefresh.setBorder(BorderFactory.createLineBorder(UIUtils.COLOR_BORDER, 1));
        btnRefresh.setPreferredSize(new Dimension(90, 36));
        btnRefresh.addActionListener(e -> {
            txtSearch.setText("");
            cboTypeFilter.setSelectedIndex(0);
            loadCustomers();
        });

        searchBox.add(txtSearch);
        searchBox.add(btnSearch);
        searchBox.add(cboTypeFilter);
        searchBox.add(btnRefresh);

        // Action buttons
        JPanel actionBox = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        actionBox.setOpaque(false);

        JButton btnAddCustomer = UIUtils.createPrimaryButton("+ Đăng Ký Khách Hàng");
        btnAddCustomer.setPreferredSize(new Dimension(180, 36));
        btnAddCustomer.addActionListener(e -> showCustomerDialog(null));

        JButton btnEditCustomer = new JButton("Sửa Khách Hàng");
        btnEditCustomer.setFont(UIUtils.FONT_BODY_BOLD);
        btnEditCustomer.setBackground(Color.WHITE);
        btnEditCustomer.setBorder(BorderFactory.createLineBorder(UIUtils.COLOR_BORDER, 1));
        btnEditCustomer.setPreferredSize(new Dimension(140, 36));
        btnEditCustomer.addActionListener(e -> {
            if (selectedCustomer == null) {
                JOptionPane.showMessageDialog(this, "Vui lòng chọn khách hàng từ bảng để sửa!", "Thông báo", JOptionPane.WARNING_MESSAGE);
                return;
            }
            showCustomerDialog(selectedCustomer);
        });

        JButton btnDeleteCustomer = new JButton("Xóa");
        btnDeleteCustomer.setFont(UIUtils.FONT_BODY_BOLD);
        btnDeleteCustomer.setForeground(UIUtils.COLOR_DANGER);
        btnDeleteCustomer.setBackground(Color.WHITE);
        btnDeleteCustomer.setBorder(BorderFactory.createLineBorder(UIUtils.COLOR_DANGER, 1));
        btnDeleteCustomer.setPreferredSize(new Dimension(75, 36));
        btnDeleteCustomer.addActionListener(e -> deleteSelectedCustomer());

        actionBox.add(btnAddCustomer);
        actionBox.add(btnEditCustomer);
        actionBox.add(btnDeleteCustomer);

        toolBar.add(searchBox, BorderLayout.WEST);
        toolBar.add(actionBox, BorderLayout.EAST);

        // ================= 3. BẢNG DỮ LIỆU KHÁCH HÀNG =================
        JPanel tableCard = new JPanel(new BorderLayout());
        tableCard.setBackground(Color.WHITE);
        tableCard.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(UIUtils.COLOR_BORDER, 1),
                new EmptyBorder(12, 14, 12, 14)
        ));

        String[] columns = {"Mã KH", "Họ & Tên Khách Hàng", "Số Điện Thoại", "Email Liên Hệ", "Địa Chỉ", "Phân Loại", "Điểm Tích Luỹ", "Trạng Thái"};
        tableModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        customerTable = new JTable(tableModel);
        customerTable.setFont(UIUtils.FONT_BODY);
        customerTable.setRowHeight(38);
        customerTable.getTableHeader().setFont(UIUtils.FONT_BODY_BOLD);
        customerTable.getTableHeader().setBackground(new Color(241, 245, 249));
        customerTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        customerTable.setShowVerticalLines(false);
        customerTable.setGridColor(UIUtils.COLOR_BORDER);

        customerTable.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                int row = customerTable.getSelectedRow();
                if (row != -1) {
                    String custId = (String) tableModel.getValueAt(row, 0);
                    selectedCustomer = customerService.getCustomerById(custId);
                } else {
                    selectedCustomer = null;
                }
            }
        });

        JScrollPane scrollPane = new JScrollPane(customerTable);
        scrollPane.setBorder(BorderFactory.createLineBorder(UIUtils.COLOR_BORDER, 1));
        tableCard.add(scrollPane, BorderLayout.CENTER);

        JPanel centerWrapper = new JPanel(new BorderLayout(0, 14));
        centerWrapper.setOpaque(false);
        centerWrapper.add(toolBar, BorderLayout.NORTH);
        centerWrapper.add(tableCard, BorderLayout.CENTER);

        add(centerWrapper, BorderLayout.CENTER);
    }

    // ================= DỮ LIỆU & NGHIỆP VỤ =================

    public void loadCustomers() {
        tableModel.setRowCount(0);
        List<Customer> list = customerService.getAllCustomers();
        renderCustomers(list);
    }

    private void renderCustomers(List<Customer> list) {
        tableModel.setRowCount(0);
        for (Customer c : list) {
            tableModel.addRow(new Object[]{
                    c.getId(),
                    c.getFullName(),
                    c.getPhone(),
                    c.getEmail(),
                    c.getAddress(),
                    "ENTERPRISE".equalsIgnoreCase(c.getCustomerType()) ? "Doanh Nghiệp" : "Cá Nhân",
                    c.getRewardPoints() + " Điểm",
                    c.isActive() ? "Kích Hoạt" : "Khóa"
            });
        }
        selectedCustomer = null;
    }

    private void executeSearch() {
        String keyword = txtSearch.getText().trim();
        if (keyword.isEmpty()) {
            loadCustomers();
            return;
        }

        // Tìm theo số điện thoại trước
        Customer byPhone = customerService.getCustomerByPhone(keyword);
        if (byPhone != null) {
            renderCustomers(List.of(byPhone));
            return;
        }

        // Hoặc lọc danh sách theo tên
        List<Customer> all = customerService.getAllCustomers();
        List<Customer> matched = all.stream()
                .filter(c -> (c.getFullName() != null && c.getFullName().toLowerCase().contains(keyword.toLowerCase()))
                        || (c.getPhone() != null && c.getPhone().contains(keyword)))
                .toList();

        renderCustomers(matched);
    }

    private void executeFilter() {
        String type = (String) cboTypeFilter.getSelectedItem();
        if (type == null || "Tất Cả Loại".equals(type)) {
            loadCustomers();
            return;
        }

        List<Customer> all = customerService.getAllCustomers();
        List<Customer> filtered = all.stream()
                .filter(c -> type.equalsIgnoreCase(c.getCustomerType()))
                .toList();
        renderCustomers(filtered);
    }

    private void deleteSelectedCustomer() {
        if (selectedCustomer == null) {
            JOptionPane.showMessageDialog(this, "Vui lòng chọn khách hàng cần xóa!", "Cảnh báo", JOptionPane.WARNING_MESSAGE);
            return;
        }

        int confirm = JOptionPane.showConfirmDialog(
                this,
                "Bạn có chắc chắn muốn xóa khách hàng: " + selectedCustomer.getFullName() + "?",
                "Xác nhận xóa",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.WARNING_MESSAGE
        );

        if (confirm == JOptionPane.YES_OPTION) {
            boolean success = customerService.deleteCustomer(selectedCustomer.getId());
            if (success) {
                JOptionPane.showMessageDialog(this, "Đã xóa khách hàng thành công!");
                loadCustomers();
            } else {
                JOptionPane.showMessageDialog(this, "Không thể xóa khách hàng!", "Lỗi", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    // ================= FORM DIALOG THÊM / SỬA KHÁCH HÀNG =================

    private void showCustomerDialog(Customer customerToEdit) {
        boolean isEdit = (customerToEdit != null);
        JDialog dialog = new JDialog((Frame) SwingUtilities.getWindowAncestor(this), isEdit ? "Cập Nhật Khách Hàng" : "Đăng Ký Khách Hàng Mới", true);
        dialog.setSize(460, 480);
        dialog.setLocationRelativeTo(this);
        dialog.setLayout(new BorderLayout());

        JPanel form = new JPanel(new GridLayout(6, 2, 10, 16));
        form.setBorder(new EmptyBorder(24, 24, 20, 24));
        form.setBackground(Color.WHITE);

        JTextField txtId = UIUtils.createTextField();
        JTextField txtFullName = UIUtils.createTextField();
        JTextField txtPhone = UIUtils.createTextField();
        JTextField txtEmail = UIUtils.createTextField();
        JTextField txtAddress = UIUtils.createTextField();

        JComboBox<String> cboType = new JComboBox<>(new String[]{"INDIVIDUAL", "ENTERPRISE"});
        cboType.setFont(UIUtils.FONT_BODY);

        if (isEdit) {
            txtId.setText(customerToEdit.getId());
            txtId.setEnabled(false);
            txtFullName.setText(customerToEdit.getFullName());
            txtPhone.setText(customerToEdit.getPhone());
            txtEmail.setText(customerToEdit.getEmail());
            txtAddress.setText(customerToEdit.getAddress());
            cboType.setSelectedItem(customerToEdit.getCustomerType());
        } else {
            txtId.setText("CUST-" + System.currentTimeMillis() % 100000);
        }

        form.add(new JLabel("Mã Khách Hàng:"));
        form.add(txtId);
        form.add(new JLabel("Họ & Tên (*):"));
        form.add(txtFullName);
        form.add(new JLabel("Số Điện Thoại (*):"));
        form.add(txtPhone);
        form.add(new JLabel("Email:"));
        form.add(txtEmail);
        form.add(new JLabel("Địa Chỉ:"));
        form.add(txtAddress);
        form.add(new JLabel("Loại Khách Hàng:"));
        form.add(cboType);

        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 12, 12));
        JButton btnSave = UIUtils.createPrimaryButton("Lưu Thông Tin");
        JButton btnCancel = new JButton("Hủy Bỏ");
        btnCancel.setFont(UIUtils.FONT_BODY);
        btnCancel.addActionListener(e -> dialog.dispose());

        btnSave.addActionListener(e -> {
            String name = txtFullName.getText().trim();
            String phone = txtPhone.getText().trim();

            if (name.isEmpty() || phone.isEmpty()) {
                JOptionPane.showMessageDialog(dialog, "Họ tên và Số điện thoại không được để trống!", "Lỗi", JOptionPane.ERROR_MESSAGE);
                return;
            }

            if (isEdit) {
                customerToEdit.setFullName(name);
                customerToEdit.setPhone(phone);
                customerToEdit.setEmail(txtEmail.getText().trim());
                customerToEdit.setAddress(txtAddress.getText().trim());
                customerToEdit.setCustomerType((String) cboType.getSelectedItem());

                boolean ok = customerService.updateCustomer(customerToEdit);
                if (ok) {
                    JOptionPane.showMessageDialog(this, "Cập nhật thông tin khách hàng thành công!");
                    dialog.dispose();
                    loadCustomers();
                } else {
                    JOptionPane.showMessageDialog(dialog, "Lỗi khi cập nhật vào cơ sở dữ liệu!", "Lỗi", JOptionPane.ERROR_MESSAGE);
                }
            } else {
                Customer newCust = new Customer(
                        txtId.getText().trim(),
                        "user_" + phone,
                        "123456",
                        name,
                        phone,
                        txtEmail.getText().trim(),
                        "CUSTOMER",
                        true,
                        txtAddress.getText().trim(),
                        (String) cboType.getSelectedItem(),
                        0
                );

                boolean ok = customerService.registerCustomer(newCust);
                if (ok) {
                    JOptionPane.showMessageDialog(this, "Đăng ký khách hàng mới thành công!");
                    dialog.dispose();
                    loadCustomers();
                } else {
                    JOptionPane.showMessageDialog(dialog, "Số điện thoại đã tồn tại hoặc xảy ra lỗi!", "Lỗi", JOptionPane.ERROR_MESSAGE);
                }
            }
        });

        btnPanel.add(btnCancel);
        btnPanel.add(btnSave);

        dialog.add(form, BorderLayout.CENTER);
        dialog.add(btnPanel, BorderLayout.SOUTH);
        dialog.setVisible(true);
    }
}
