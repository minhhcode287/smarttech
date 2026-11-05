package com.smarttech.service;

import com.smarttech.model.Equipment;
import com.smarttech.model.EquipmentLog;
import com.smarttech.model.MaintenanceTicket;
import com.smarttech.model.Product;
import com.smarttech.model.User;
import com.smarttech.repository.EquipmentLogRepository;
import com.smarttech.repository.EquipmentRepository;
import com.smarttech.repository.MaintenanceRepository;
import com.smarttech.repository.ProductRepository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Random;

/**
 * Service xử lý toàn bộ nghiệp vụ Quản Lý Bảo Trì, Sửa Chữa Thiết Bị & Nhật Ký Phần Cứng (MaintenanceService).
 * Đảm bảo tính nhất quán (Atomicity / Consistency) giữa các collection:
 * `maintenance_tickets`, `equipments`, và `equipment_logs`.
 *
 * @author Senior Java Developer & Software Architect
 * @project SmartTech
 */
public class MaintenanceService {

    private final MaintenanceRepository maintenanceRepository;
    private final EquipmentRepository equipmentRepository;
    private final EquipmentLogRepository equipmentLogRepository;
    private final ProductRepository productRepository;

    private static final DateTimeFormatter CODE_DATE_FORMAT = DateTimeFormatter.ofPattern("yyyyMMdd");
    private final Random random = new Random();

    public MaintenanceService() {
        this.maintenanceRepository = new MaintenanceRepository();
        this.equipmentRepository = new EquipmentRepository();
        this.equipmentLogRepository = new EquipmentLogRepository();
        this.productRepository = new ProductRepository();
    }

    public MaintenanceService(MaintenanceRepository maintenanceRepository,
                              EquipmentRepository equipmentRepository,
                              EquipmentLogRepository equipmentLogRepository,
                              ProductRepository productRepository) {
        this.maintenanceRepository = maintenanceRepository;
        this.equipmentRepository = equipmentRepository;
        this.equipmentLogRepository = equipmentLogRepository;
        this.productRepository = productRepository;
    }

    /**
     * Sinh mã phiếu bảo trì tự động theo cấu trúc chuẩn ERP: MAIN-yyyyMMdd-XXXX
     *
     * @return Mã phiếu bảo trì duy nhất
     */
    public synchronized String generateTicketId() {
        String datePart = LocalDate.now().format(CODE_DATE_FORMAT);
        for (int i = 0; i < 50; i++) {
            int seq = 1000 + random.nextInt(9000);
            String candidateId = "MAIN-" + datePart + "-" + seq;
            if (maintenanceRepository.findById(candidateId) == null) {
                return candidateId;
            }
        }
        return "MAIN-" + datePart + "-" + System.currentTimeMillis() % 10000;
    }

    /**
     * Tiếp nhận thiết bị & Tạo phiếu bảo trì / sửa chữa mới.
     * Quy trình liên kết 3 bảng CSDL:
     * 1. Lưu MaintenanceTicket vào collection `maintenance_tickets`.
     * 2. Cập nhật trạng thái Equipment sang "MAINTENANCE" trong collection `equipments`.
     * 3. Ghi vết biến động phần cứng vào collection `equipment_logs` với actionType = "SEND_MAINTENANCE".
     *
     * @param ticket Thông tin phiếu bảo trì
     * @return true nếu tiếp nhận bảo trì thành công
     */
    public boolean createMaintenanceTicket(MaintenanceTicket ticket) {
        if (ticket == null || ticket.getSerialId() == null || ticket.getSerialId().trim().isEmpty()) {
            System.err.println("[MaintenanceService] Không thể tạo phiếu: Mã Serial không hợp lệ!");
            return false;
        }

        String serialId = ticket.getSerialId().trim().toUpperCase();
        ticket.setSerialId(serialId);

        // 1. Kiểm tra thiết bị thực tế trong kho
        Equipment equipment = equipmentRepository.findById(serialId);
        String oldStatus = (equipment != null && equipment.getStatus() != null)
                ? equipment.getStatus()
                : Equipment.STATUS_AVAILABLE;

        String productId = (equipment != null && equipment.getProductId() != null)
                ? equipment.getProductId()
                : (ticket.getProductId() != null ? ticket.getProductId() : "");
        ticket.setProductId(productId);

        // Bổ sung tên sản phẩm nếu chưa có
        if (ticket.getProductName() == null || ticket.getProductName().trim().isEmpty()) {
            if (!productId.isEmpty()) {
                Product p = productRepository.findById(productId);
                if (p != null) {
                    ticket.setProductName(p.getProductName());
                } else {
                    ticket.setProductName("Thiết bị " + serialId);
                }
            } else {
                ticket.setProductName("Thiết bị " + serialId);
            }
        }

        // Tự động sinh mã phiếu nếu chưa có
        if (ticket.getId() == null || ticket.getId().trim().isEmpty()) {
            ticket.setId(generateTicketId());
        }

        // Bổ sung thông tin người lập nếu chưa có
        User currentUser = UserSession.getInstance().getCurrentUser();
        if (ticket.getEmployeeName() == null || ticket.getEmployeeName().trim().isEmpty()) {
            ticket.setEmployeeName(currentUser != null ? currentUser.getFullName() : "Nhân Viên Kỹ Thuật");
        }
        if (ticket.getEmployeeId() == null || ticket.getEmployeeId().trim().isEmpty()) {
            ticket.setEmployeeId(currentUser != null ? currentUser.getId() : "EMP-SYSTEM");
        }

        ticket.setStatus(MaintenanceTicket.STATUS_IN_PROGRESS);
        if (ticket.getStartDate() == null) {
            ticket.setStartDate(LocalDate.now());
        }
        if (ticket.getExpectedDate() == null) {
            ticket.setExpectedDate(LocalDate.now().plusDays(3));
        }

        // Bước 1: Lưu Ticket
        boolean ticketSaved = maintenanceRepository.save(ticket);
        if (!ticketSaved) {
            System.err.println("[MaintenanceService] Lỗi khi lưu phiếu bảo trì vào MongoDB!");
            return false;
        }

        // Bước 2: Cập nhật trạng thái Equipment sang "MAINTENANCE"
        if (equipment != null) {
            equipment.setStatus(Equipment.STATUS_MAINTENANCE);
            equipment.setConditionNote("Đang gửi bảo trì: " + (ticket.getIssueDescription() != null ? ticket.getIssueDescription() : "Bảo dưỡng định kỳ"));
            equipmentRepository.update(equipment);
        }

        // Bước 3: Ghi nhận vết Audit Log
        try {
            EquipmentLog log = new EquipmentLog();
            log.setSerialId(serialId);
            log.setProductId(productId);
            log.setActionType(EquipmentLog.ACTION_SEND_MAINTENANCE);
            log.setOldStatus(oldStatus);
            log.setNewStatus(Equipment.STATUS_MAINTENANCE);
            log.setVendor(ticket.getRepairVendor() != null ? ticket.getRepairVendor() : "Nội Bộ SmartTech");
            log.setEstimatedCost(ticket.getRepairCost());
            log.setActualCost(0.0);
            log.setReason(ticket.getIssueDescription() != null ? ticket.getIssueDescription() : "Gửi bảo trì theo phiếu " + ticket.getId());
            log.setRepairNotes("Phiếu bảo trì: " + ticket.getId() + " - Dự kiến hoàn tất: " + ticket.getExpectedDate());
            log.setPerformedBy(ticket.getEmployeeName());
            log.setTimestamp(LocalDateTime.now());
            equipmentLogRepository.save(log);
        } catch (Exception e) {
            System.err.println("[MaintenanceService] Cảnh báo lỗi ghi EquipmentLog (SEND_MAINTENANCE): " + e.getMessage());
        }

        return true;
    }

    /**
     * Nghiệm thu & Hoàn tất quá trình bảo trì thiết bị.
     * Quy trình xử lý:
     * 1. Cập nhật phiếu MaintenanceTicket sang trạng thái "COMPLETED", cập nhật ngày hoàn thành, chi phí thực tế và ghi chú sửa chữa.
     * 2. Cập nhật thiết bị Equipment sang trạng thái mới (mặc định "AVAILABLE" - Sẵn sàng cho thuê/bán, hoặc "DAMAGED"/"BROKEN" nếu không thể phục hồi).
     * 3. Ghi vết biến động vào collection `equipment_logs` với actionType = "COMPLETE_MAINTENANCE".
     *
     * @param ticketId Mã phiếu bảo trì cần hoàn tất
     * @param resolutionNote Ghi chú kết quả sửa chữa, linh kiện thay thế
     * @param finalCost Chi phí thực tế thanh toán (VNĐ)
     * @param newEquipmentStatus Trạng thái trả về kho của thiết bị (mặc định AVAILABLE)
     * @return true nếu hoàn tất thành công
     */
    public boolean completeMaintenance(String ticketId, String resolutionNote, double finalCost, String newEquipmentStatus) {
        if (ticketId == null || ticketId.trim().isEmpty()) {
            return false;
        }

        MaintenanceTicket ticket = maintenanceRepository.findById(ticketId.trim());
        if (ticket == null) {
            System.err.println("[MaintenanceService] Không tìm thấy phiếu bảo trì ID: " + ticketId);
            return false;
        }

        // Cập nhật ticket
        ticket.setStatus(MaintenanceTicket.STATUS_COMPLETED);
        ticket.setCompletionDate(LocalDate.now());
        ticket.setRepairCost(Math.max(0.0, finalCost));
        ticket.setResolutionNote(resolutionNote != null ? resolutionNote.trim() : "Đã hoàn tất bảo trì.");

        boolean ticketUpdated = maintenanceRepository.update(ticket);
        if (!ticketUpdated) {
            return false;
        }

        String targetStatus = (newEquipmentStatus != null && !newEquipmentStatus.trim().isEmpty())
                ? newEquipmentStatus.trim()
                : Equipment.STATUS_AVAILABLE;

        // Cập nhật Equipment
        if (ticket.getSerialId() != null && !ticket.getSerialId().isEmpty()) {
            Equipment eq = equipmentRepository.findById(ticket.getSerialId());
            if (eq != null) {
                eq.setStatus(targetStatus);
                eq.setConditionNote("Bảo trì xong (" + ticket.getId() + "): " + (resolutionNote != null ? resolutionNote : "Hoạt động tốt"));
                equipmentRepository.update(eq);
            }
        }

        // Ghi Audit Log
        try {
            User currentUser = UserSession.getInstance().getCurrentUser();
            String performer = currentUser != null ? currentUser.getFullName() : "Nhân Viên Kỹ Thuật";

            EquipmentLog log = new EquipmentLog();
            log.setSerialId(ticket.getSerialId());
            log.setProductId(ticket.getProductId());
            log.setActionType(EquipmentLog.ACTION_COMPLETE_MAINTENANCE);
            log.setOldStatus(Equipment.STATUS_MAINTENANCE);
            log.setNewStatus(targetStatus);
            log.setVendor(ticket.getRepairVendor());
            log.setEstimatedCost(ticket.getRepairCost());
            log.setActualCost(finalCost);
            log.setReason("Nghiệm thu hoàn tất phiếu bảo trì " + ticket.getId());
            log.setRepairNotes(resolutionNote != null ? resolutionNote : "Đã kiểm tra chất lượng phần cứng đạt yêu cầu.");
            log.setPerformedBy(performer);
            log.setTimestamp(LocalDateTime.now());
            equipmentLogRepository.save(log);
        } catch (Exception e) {
            System.err.println("[MaintenanceService] Cảnh báo lỗi ghi EquipmentLog (COMPLETE_MAINTENANCE): " + e.getMessage());
        }

        return true;
    }

    /**
     * Hủy bỏ phiếu bảo trì (Ví dụ thiết bị được trả về hoặc không cần sửa nữa).
     *
     * @param ticketId Mã phiếu bảo trì
     * @param cancelReason Lý do hủy
     * @return true nếu hủy thành công
     */
    public boolean cancelMaintenance(String ticketId, String cancelReason) {
        if (ticketId == null || ticketId.trim().isEmpty()) return false;
        MaintenanceTicket ticket = maintenanceRepository.findById(ticketId.trim());
        if (ticket == null) return false;

        ticket.setStatus(MaintenanceTicket.STATUS_CANCELLED);
        ticket.setCompletionDate(LocalDate.now());
        ticket.setResolutionNote("Đã hủy bỏ: " + (cancelReason != null ? cancelReason : "Theo yêu cầu quản lý"));

        boolean updated = maintenanceRepository.update(ticket);
        if (updated && ticket.getSerialId() != null) {
            Equipment eq = equipmentRepository.findById(ticket.getSerialId());
            if (eq != null && Equipment.STATUS_MAINTENANCE.equalsIgnoreCase(eq.getStatus())) {
                eq.setStatus(Equipment.STATUS_AVAILABLE);
                eq.setConditionNote("Hủy bảo trì (" + ticket.getId() + ")");
                equipmentRepository.update(eq);
            }
        }
        return updated;
    }

    /**
     * Lấy toàn bộ phiếu bảo trì trong hệ thống.
     */
    public List<MaintenanceTicket> getAllTickets() {
        return maintenanceRepository.findAll();
    }

    /**
     * Lấy các phiếu bảo trì đang được sửa chữa (IN_PROGRESS).
     */
    public List<MaintenanceTicket> getActiveTickets() {
        return maintenanceRepository.findActiveTickets();
    }

    /**
     * Tìm kiếm phiếu bảo trì theo từ khóa.
     */
    public List<MaintenanceTicket> searchTickets(String keyword) {
        return maintenanceRepository.search(keyword);
    }

    /**
     * Tìm phiếu bảo trì theo mã định danh.
     */
    public MaintenanceTicket getTicketById(String id) {
        return maintenanceRepository.findById(id);
    }

    /**
     * Lấy danh sách lịch sử sửa chữa của 1 số Serial cụ thể.
     */
    public List<MaintenanceTicket> getTicketsBySerialId(String serialId) {
        return maintenanceRepository.findBySerialId(serialId);
    }

    /**
     * Tính tổng chi phí bảo trì tích lũy.
     */
    public double getTotalRepairCost() {
        return maintenanceRepository.getTotalRepairCost();
    }
}
