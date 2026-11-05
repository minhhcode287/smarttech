package com.smarttech.service;

import com.smarttech.model.Equipment;
import com.smarttech.model.EquipmentLog;
import com.smarttech.repository.EquipmentLogRepository;
import com.smarttech.repository.EquipmentRepository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

/**
 * Service quản lý các đơn vị Thiết bị thực tế (Equipment) theo Số Serial phần cứng,
 * và điều phối chu trình bảo trì, sửa chữa, thay đổi trạng thái kèm Audit Log.
 *
 * @author Senior Java Developer & Software Architect
 * @project SmartTech
 */
public class EquipmentService {

    private final EquipmentRepository equipmentRepository;
    private final EquipmentLogRepository logRepository;

    public EquipmentService() {
        this.equipmentRepository = new EquipmentRepository();
        this.logRepository = new EquipmentLogRepository();
    }

    public EquipmentService(EquipmentRepository equipmentRepository, EquipmentLogRepository logRepository) {
        this.equipmentRepository = equipmentRepository;
        this.logRepository = logRepository;
    }

    public List<Equipment> getAllEquipments() {
        return equipmentRepository.findAll();
    }

    public Equipment getEquipmentBySerial(String serialId) {
        if (serialId == null || serialId.trim().isEmpty()) {
            return null;
        }
        return equipmentRepository.findById(serialId.trim());
    }

    /**
     * Nhập mới thiết bị vào kho kèm mã Serial phần cứng duy nhất.
     *
     * @param equipment Đối tượng thiết bị thực tế
     * @return true nếu thêm thành công
     */
    public boolean addEquipment(Equipment equipment) {
        if (equipment == null || equipment.getId() == null || equipment.getId().trim().isEmpty()) {
            System.err.println("[EquipmentService] Mã Serial thiết bị không được để trống!");
            return false;
        }

        // Kiểm tra xem Serial đã tồn tại trong kho chưa
        Equipment existing = equipmentRepository.findById(equipment.getId().trim());
        if (existing != null) {
            System.err.println("[EquipmentService] Serial Number '" + equipment.getId() + "' đã tồn tại trong kho!");
            return false;
        }

        boolean saved = equipmentRepository.save(equipment);
        if (saved) {
            // Tự động ghi nhận log khởi tạo thiết bị vào kho
            String currentUser = getCurrentUsername();
            EquipmentLog log = new EquipmentLog(
                    equipment.getId(),
                    equipment.getProductId(),
                    EquipmentLog.ACTION_STATUS_CHANGE,
                    "NEW",
                    equipment.getStatus() != null ? equipment.getStatus() : Equipment.STATUS_AVAILABLE,
                    "Kho SmartTech",
                    0.0,
                    0.0,
                    "Nhập thiết bị mới vào kho",
                    equipment.getConditionNote(),
                    currentUser
            );
            logRepository.save(log);
        }
        return saved;
    }

    /**
     * Cập nhật trạng thái vòng đời của thiết bị (AVAILABLE, RENTED, SOLD, MAINTENANCE, BROKEN).
     *
     * @param serialId Mã Serial thiết bị
     * @param newStatus Trạng thái mới
     * @param note     Ghi chú bổ sung (nếu có)
     * @return true nếu cập nhật thành công
     */
    public boolean changeEquipmentStatus(String serialId, String newStatus, String note) {
        Equipment equipment = getEquipmentBySerial(serialId);
        if (equipment == null) {
            System.err.println("[EquipmentService] Không tìm thấy thiết bị có Serial: " + serialId);
            return false;
        }

        String oldStatus = equipment.getStatus();
        equipment.setStatus(newStatus);
        if (note != null && !note.trim().isEmpty()) {
            equipment.setConditionNote(note.trim());
        }

        boolean updated = equipmentRepository.update(equipment);
        if (updated) {
            System.out.println("[EquipmentService] Đã chuyển trạng thái thiết bị [" + serialId + "] sang: " + newStatus);
            // Ghi nhận Audit Log
            String currentUser = getCurrentUsername();
            EquipmentLog log = new EquipmentLog(
                    serialId,
                    equipment.getProductId(),
                    EquipmentLog.ACTION_STATUS_CHANGE,
                    oldStatus,
                    newStatus,
                    "Nội bộ",
                    0.0,
                    0.0,
                    note != null ? note : "Đổi trạng thái vận hành",
                    "",
                    currentUser
            );
            logRepository.save(log);
        }
        return updated;
    }

    /**
     * Nghiệp vụ 1: Gửi thiết bị đi bảo dưỡng / sửa chữa.
     * Kiểm tra trạng thái máy phải là AVAILABLE, cập nhật sang MAINTENANCE,
     * và tự động lưu phiếu biến động vào Collection 'equipment_logs'.
     *
     * @param serialId Mã Serial thiết bị
     * @param vendor Đơn vị sửa chữa / bảo hành
     * @param reason Lý do hỏng hóc hoặc nội dung bảo trì
     * @param estimatedCost Chi phí ước tính (VNĐ)
     * @param expectedDate Ngày dự kiến hoàn tất
     * @return true nếu gửi bảo trì thành công
     */
    public boolean sendToMaintenance(String serialId, String vendor, String reason, double estimatedCost, LocalDate expectedDate) {
        if (serialId == null || serialId.trim().isEmpty()) {
            System.err.println("[EquipmentService] Mã Serial không được để trống!");
            return false;
        }

        Equipment equipment = getEquipmentBySerial(serialId);
        if (equipment == null) {
            System.err.println("[EquipmentService] Không tìm thấy thiết bị: " + serialId);
            return false;
        }

        if (!Equipment.STATUS_AVAILABLE.equalsIgnoreCase(equipment.getStatus())) {
            System.err.println("[EquipmentService] Thiết bị đang ở trạng thái '" + equipment.getStatus() + "', không thể gửi bảo trì!");
            return false;
        }

        String oldStatus = equipment.getStatus();
        equipment.setStatus(Equipment.STATUS_MAINTENANCE);
        String maintenanceNote = (vendor != null ? vendor.trim() : "Bảo hành") + " | Lý do: " + (reason != null ? reason.trim() : "");
        equipment.setConditionNote(maintenanceNote);

        boolean updated = equipmentRepository.update(equipment);
        if (updated) {
            String currentUser = getCurrentUsername();
            String noteRepair = (expectedDate != null) ? "Dự kiến hoàn thành: " + expectedDate : "Đang chờ xử lý linh kiện";

            EquipmentLog log = new EquipmentLog(
                    equipment.getId(),
                    equipment.getProductId(),
                    EquipmentLog.ACTION_SEND_MAINTENANCE,
                    oldStatus,
                    Equipment.STATUS_MAINTENANCE,
                    vendor != null ? vendor.trim() : "Đơn vị ủy quyền",
                    Math.max(0.0, estimatedCost),
                    0.0,
                    reason != null ? reason.trim() : "Bảo dưỡng định kỳ",
                    noteRepair,
                    currentUser
            );
            logRepository.save(log);
            System.out.println("[EquipmentService] Thiết bị [" + serialId + "] đã gửi bảo trì tại " + vendor);
            return true;
        }
        return false;
    }

    /**
     * Nghiệp vụ 2: Hoàn tất quy trình bảo trì, đưa thiết bị trở về kho sẵn sàng (AVAILABLE).
     *
     * @param serialId Mã Serial thiết bị
     * @param actualCost Chi phí thực tế đã thanh toán (VNĐ)
     * @param repairNotes Ghi chú kết quả sửa chữa, linh kiện đã thay
     * @return true nếu hoàn tất bảo trì thành công
     */
    public boolean completeMaintenance(String serialId, double actualCost, String repairNotes) {
        if (serialId == null || serialId.trim().isEmpty()) {
            System.err.println("[EquipmentService] Mã Serial không được để trống!");
            return false;
        }

        Equipment equipment = getEquipmentBySerial(serialId);
        if (equipment == null) {
            System.err.println("[EquipmentService] Không tìm thấy thiết bị: " + serialId);
            return false;
        }

        if (!Equipment.STATUS_MAINTENANCE.equalsIgnoreCase(equipment.getStatus())) {
            System.err.println("[EquipmentService] Thiết bị không ở trạng thái MAINTENANCE, không thể hoàn tất bảo trì!");
            return false;
        }

        String oldStatus = equipment.getStatus();
        equipment.setStatus(Equipment.STATUS_AVAILABLE);
        String conditionAfter = "Đã bảo trì tốt: " + (repairNotes != null ? repairNotes.trim() : "Hoàn tất");
        equipment.setConditionNote(conditionAfter);

        boolean updated = equipmentRepository.update(equipment);
        if (updated) {
            String currentUser = getCurrentUsername();
            EquipmentLog log = new EquipmentLog(
                    equipment.getId(),
                    equipment.getProductId(),
                    EquipmentLog.ACTION_COMPLETE_MAINTENANCE,
                    oldStatus,
                    Equipment.STATUS_AVAILABLE,
                    "Đã nghiệm thu",
                    0.0,
                    Math.max(0.0, actualCost),
                    "Hoàn tất bảo trì, nghiệm thu phần cứng",
                    repairNotes != null ? repairNotes.trim() : "Hoàn tất đạt chuẩn hoạt động",
                    currentUser
            );
            logRepository.save(log);
            System.out.println("[EquipmentService] Thiết bị [" + serialId + "] đã bảo trì xong và trở lại kho AVAILABLE.");
            return true;
        }
        return false;
    }

    /**
     * Nghiệp vụ 3: Lấy danh sách nhật ký Audit Log của một Serial (hoặc toàn bộ nếu serialId rỗng/null).
     *
     * @param serialId Mã Serial hoặc null/rỗng nếu muốn lấy toàn bộ
     * @return Danh sách lịch sử biến động
     */
    public List<EquipmentLog> getEquipmentAuditLogs(String serialId) {
        if (serialId != null && !serialId.trim().isEmpty()) {
            return logRepository.findBySerialId(serialId.trim());
        }
        return logRepository.findAll();
    }

    /**
     * Lấy đối tượng Log Repository để thao tác nâng cao nếu cần.
     */
    public EquipmentLogRepository getLogRepository() {
        return logRepository;
    }

    /**
     * Lấy danh sách các máy thực tế sẵn sàng giao dịch (AVAILABLE) cho một mẫu sản phẩm.
     *
     * @param productId Mã sản phẩm
     * @return Danh sách thiết bị sẵn sàng cho thuê hoặc bán
     */
    public List<Equipment> getAvailableEquipments(String productId) {
        return equipmentRepository.findAvailableByProductId(productId);
    }

    /**
     * Lấy toàn bộ danh sách thiết bị thực tế thuộc một mẫu sản phẩm (mọi trạng thái).
     *
     * @param productId Mã mẫu sản phẩm
     * @return Danh sách thiết bị
     */
    public List<Equipment> getEquipmentsByProductId(String productId) {
        if (productId == null || productId.trim().isEmpty()) {
            return Collections.emptyList();
        }
        return equipmentRepository.findByProductId(productId.trim());
    }

    /**
     * Xóa thiết bị khỏi kho theo Serial Number.
     *
     * @param serialId Mã Serial thiết bị
     * @return true nếu xóa thành công
     */
    public boolean deleteEquipment(String serialId) {
        if (serialId == null || serialId.trim().isEmpty()) {
            return false;
        }
        return equipmentRepository.deleteById(serialId.trim());
    }

    /**
     * Kiểm tra xem một mẫu sản phẩm có còn thiết bị khả dụng trong kho hay không.
     *
     * @param productId Mã sản phẩm
     * @return true nếu còn ít nhất 1 thiết bị AVAILABLE
     */
    public boolean isProductInStock(String productId) {
        List<Equipment> available = getAvailableEquipments(productId);
        return available != null && !available.isEmpty();
    }

    private String getCurrentUsername() {
        if (UserSession.getInstance().isLoggedIn() && UserSession.getInstance().getCurrentUser() != null) {
            return UserSession.getInstance().getCurrentUser().getUsername();
        }
        return "SYSTEM";
    }
}