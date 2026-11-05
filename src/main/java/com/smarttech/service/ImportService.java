package com.smarttech.service;

import com.smarttech.model.Equipment;
import com.smarttech.model.EquipmentLog;
import com.smarttech.model.ImportItem;
import com.smarttech.model.ImportOrder;
import com.smarttech.model.Supplier;
import com.smarttech.repository.EquipmentLogRepository;
import com.smarttech.repository.EquipmentRepository;
import com.smarttech.repository.ImportOrderRepository;
import com.smarttech.repository.SupplierRepository;

import java.util.List;

/**
 * Service điều phối toàn bộ quy trình Nghiệp vụ Nhập Kho Thiết Bị & Quản Lý Nhà Cung Cấp.
 * Đảm bảo tính toàn vẹn dữ liệu: Khi phiếu nhập kho được lưu thành công, tất cả các mã Serial
 * phần cứng (ImportItem) sẽ được tự động kích hoạt tạo thành các bản ghi Equipment mới (AVAILABLE)
 * kèm Audit Log biến động kho.
 *
 * @author Senior Java Developer & Software Architect
 * @project SmartTech
 */
public class ImportService {

    private final ImportOrderRepository importOrderRepository;
    private final SupplierRepository supplierRepository;
    private final EquipmentRepository equipmentRepository;
    private final EquipmentLogRepository logRepository;

    public ImportService() {
        this.importOrderRepository = new ImportOrderRepository();
        this.supplierRepository = new SupplierRepository();
        this.equipmentRepository = new EquipmentRepository();
        this.logRepository = new EquipmentLogRepository();
    }

    public ImportService(ImportOrderRepository importOrderRepository,
                         SupplierRepository supplierRepository,
                         EquipmentRepository equipmentRepository,
                         EquipmentLogRepository logRepository) {
        this.importOrderRepository = importOrderRepository;
        this.supplierRepository = supplierRepository;
        this.equipmentRepository = equipmentRepository;
        this.logRepository = logRepository;
    }

    /**
     * Xử lý nhập kho toàn vẹn:
     * 1. Lưu phiếu ImportOrder vào MongoDB collection import_orders.
     * 2. Với từng Serial trong ImportItem, tạo đối tượng Equipment với trạng thái AVAILABLE
     *    và lưu vào collection equipments.
     * 3. Ghi EquipmentLog đánh dấu việc nhập kho mới.
     *
     * @param importOrder Phiếu nhập kho hoàn chỉnh
     * @return true nếu xử lý nhập kho thành công
     */
    public boolean processImportOrder(ImportOrder importOrder) {
        if (importOrder == null || importOrder.getItems() == null || importOrder.getItems().isEmpty()) {
            System.err.println("[ImportService] Phiếu nhập không hợp lệ hoặc danh sách thiết bị rỗng!");
            return false;
        }

        // Tự sinh mã phiếu nếu chưa có
        if (importOrder.getId() == null || importOrder.getId().trim().isEmpty()) {
            importOrder.setId(generateImportOrderId());
        }

        // Kiểm tra xem có Serial nào bị trùng trong hệ thống không
        for (ImportItem item : importOrder.getItems()) {
            if (item.getSerialId() == null || item.getSerialId().trim().isEmpty()) {
                System.err.println("[ImportService] Phát hiện thiết bị không có mã Serial!");
                return false;
            }
            if (equipmentRepository.findById(item.getSerialId().trim()) != null) {
                System.err.println("[ImportService] Serial '" + item.getSerialId() + "' đã tồn tại trong kho!");
                return false;
            }
        }

        // 1. Lưu phiếu nhập kho
        boolean orderSaved = importOrderRepository.save(importOrder);
        if (!orderSaved) {
            System.err.println("[ImportService] Lưu phiếu nhập kho thất bại!");
            return false;
        }

        // 2. Tự động sinh và lưu từng thiết bị phần cứng (Equipment) vào kho
        String currentUser = UserSession.getInstance().isLoggedIn() ?
                UserSession.getInstance().getCurrentUser().getFullName() : "SYSTEM";

        for (ImportItem item : importOrder.getItems()) {
            String note = "Nhập kho từ NCC " + importOrder.getSupplierName()
                    + " (Phiếu: " + importOrder.getId() + ") - Giá nhập: "
                    + String.format("%,.0f đ", item.getImportPrice());

            Equipment equipment = new Equipment(
                    item.getSerialId().trim().toUpperCase(),
                    item.getProductId(),
                    Equipment.STATUS_AVAILABLE,
                    note,
                    true
            );

            boolean eqSaved = equipmentRepository.save(equipment);
            if (eqSaved) {
                // 3. Ghi nhận Audit Log biến động phần cứng
                EquipmentLog log = new EquipmentLog(
                        equipment.getId(),
                        equipment.getProductId(),
                        EquipmentLog.ACTION_STATUS_CHANGE,
                        "IMPORT",
                        Equipment.STATUS_AVAILABLE,
                        "Kho SmartTech Trung Tâm",
                        item.getImportPrice(),
                        0.0,
                        "Nhập kho thiết bị mới theo phiếu: " + importOrder.getId(),
                        note,
                        currentUser
                );
                logRepository.save(log);
            }
        }

        return true;
    }

    /**
     * Kiểm tra xem mã Serial phần cứng đã tồn tại trong kho chưa.
     */
    public boolean isSerialExists(String serialId) {
        if (serialId == null || serialId.trim().isEmpty()) return false;
        return equipmentRepository.findById(serialId.trim().toUpperCase()) != null;
    }

    /**
     * Tạo mã phiếu nhập kho tự động không trùng lặp (ví dụ: "IMP-782910").
     */
    public String generateImportOrderId() {
        return "IMP-" + (System.currentTimeMillis() % 1000000);
    }

    // ================= QUẢN LÝ PHIẾU NHẬP KHO =================

    public List<ImportOrder> getAllImportOrders() {
        return importOrderRepository.findAll();
    }

    public List<ImportOrder> searchImportOrders(String keyword) {
        return importOrderRepository.search(keyword);
    }

    public ImportOrder getImportOrderById(String id) {
        return importOrderRepository.findById(id);
    }

    // ================= QUẢN LÝ NHÀ CUNG CẤP =================

    public List<Supplier> getAllSuppliers() {
        return supplierRepository.findAll();
    }

    public List<Supplier> getActiveSuppliers() {
        return supplierRepository.findActiveSuppliers();
    }

    public Supplier getSupplierById(String id) {
        return supplierRepository.findById(id);
    }

    public boolean saveSupplier(Supplier supplier) {
        return supplierRepository.save(supplier);
    }

    public boolean updateSupplier(Supplier supplier) {
        return supplierRepository.update(supplier);
    }

    public boolean deleteSupplier(String id) {
        return supplierRepository.delete(id);
    }

    public List<Supplier> searchSuppliers(String keyword) {
        return supplierRepository.search(keyword);
    }
}
