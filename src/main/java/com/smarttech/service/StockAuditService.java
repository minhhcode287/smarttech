package com.smarttech.service;

import com.smarttech.model.Equipment;
import com.smarttech.model.EquipmentLog;
import com.smarttech.model.StockAudit;
import com.smarttech.model.StockAuditDetail;
import com.smarttech.repository.EquipmentLogRepository;
import com.smarttech.repository.EquipmentRepository;
import com.smarttech.repository.StockAuditRepository;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Service xử lý toàn bộ quy trình Kiểm Kê Kho & Cân Bằng Tồn Kho Tự Động (Stock Audit & Adjustment Service).
 * Khi chốt kiểm kê, hệ thống sẽ tự động đối chiếu các thiết bị bị lệch trạng thái (ví dụ: máy hỏng, thất thoát,
 * cần bảo trì) và cập nhật trực tiếp vào MongoDB collection "equipments", đồng thời ghi nhận Audit Log.
 *
 * @author Senior Java Developer & Software Architect
 * @project SmartTech
 */
public class StockAuditService {

    private final StockAuditRepository auditRepository;
    private final EquipmentRepository equipmentRepository;
    private final EquipmentLogRepository logRepository;

    public StockAuditService() {
        this.auditRepository = new StockAuditRepository();
        this.equipmentRepository = new EquipmentRepository();
        this.logRepository = new EquipmentLogRepository();
    }

    public StockAuditService(StockAuditRepository auditRepository,
                             EquipmentRepository equipmentRepository,
                             EquipmentLogRepository logRepository) {
        this.auditRepository = auditRepository;
        this.equipmentRepository = equipmentRepository;
        this.logRepository = logRepository;
    }

    /**
     * Xử lý chốt kiểm kê và tự động cân bằng tồn kho phần cứng:
     * 1. Lưu/Cập nhật phiếu kiểm kê vào collection "stock_audits" với trạng thái COMPLETED.
     * 2. Quét qua từng StockAuditDetail: nếu actualStatus khác systemStatus, cập nhật Equipment trong CSDL
     *    và tạo EquipmentLog ghi nhận lý do điều chỉnh tồn kho.
     *
     * @param audit Phiếu kiểm kê cần xử lý
     * @return true nếu cân bằng kho thành công
     */
    public boolean processStockAuditAndAdjust(StockAudit audit) {
        if (audit == null || audit.getDetails() == null || audit.getDetails().isEmpty()) {
            System.err.println("[StockAuditService] Phiếu kiểm kê không hợp lệ hoặc không có dữ liệu thiết bị!");
            return false;
        }

        // Tự sinh mã phiếu nếu chưa có
        if (audit.getId() == null || audit.getId().trim().isEmpty()) {
            audit.setId(generateAuditId());
        }

        // Cập nhật lại số liệu thống kê
        audit.setStatus(StockAudit.STATUS_COMPLETED);
        audit.recalculateCounts();

        // 1. Lưu vào CSDL
        boolean saved = auditRepository.findById(audit.getId()) != null
                ? auditRepository.update(audit)
                : auditRepository.save(audit);

        if (!saved) {
            System.err.println("[StockAuditService] Lưu phiếu kiểm kê thất bại!");
            return false;
        }

        // 2. Tự động cân bằng trạng thái các thiết bị có sai lệch
        String performer = (audit.getEmployeeName() != null && !audit.getEmployeeName().trim().isEmpty())
                ? audit.getEmployeeName() : "SYSTEM";

        for (StockAuditDetail detail : audit.getDetails()) {
            if (detail.hasDiscrepancy()) {
                String serial = detail.getSerialId();
                Equipment equipment = equipmentRepository.findById(serial);
                if (equipment != null) {
                    String oldStatus = equipment.getStatus();
                    String newStatus = detail.getActualStatus();

                    // Cập nhật trạng thái phần cứng
                    equipment.setStatus(newStatus);
                    String adjustmentNote = "Điều chỉnh qua Đợt kiểm kê [" + audit.getId() + " - " + audit.getAuditName() + "]: "
                            + (detail.getNote() != null ? detail.getNote() : "Sai lệch trạng thái thực tế");
                    equipment.setConditionNote(adjustmentNote);

                    equipmentRepository.update(equipment);

                    // Ghi nhận Audit Log biến động phần cứng
                    EquipmentLog log = new EquipmentLog(
                            serial,
                            detail.getProductId(),
                            EquipmentLog.ACTION_STATUS_CHANGE,
                            oldStatus,
                            newStatus,
                            "Kho SmartTech Trung Tâm",
                            0.0,
                            0.0,
                            "Cân bằng tồn kho kiểm kê: " + audit.getId(),
                            adjustmentNote,
                            performer
                    );
                    logRepository.save(log);
                }
            }
        }

        return true;
    }

    /**
     * Lưu phiếu kiểm kê ở dạng Bản Nháp (DRAFT), chưa cập nhật trạng thái thiết bị trong kho.
     */
    public boolean saveDraftAudit(StockAudit audit) {
        if (audit == null) return false;
        if (audit.getId() == null || audit.getId().trim().isEmpty()) {
            audit.setId(generateAuditId());
        }
        audit.setStatus(StockAudit.STATUS_DRAFT);
        audit.recalculateCounts();

        if (auditRepository.findById(audit.getId()) != null) {
            return auditRepository.update(audit);
        } else {
            return auditRepository.save(audit);
        }
    }

    /**
     * Tạo mã phiếu kiểm kê tự động có tiền tố AUDIT-yyyyMMdd-xxxx (ví dụ: AUDIT-20261105-849201).
     */
    public String generateAuditId() {
        String datePart = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        long randomSuffix = System.currentTimeMillis() % 1000000;
        return "AUDIT-" + datePart + "-" + randomSuffix;
    }

    public List<StockAudit> getAllAudits() {
        return auditRepository.findAll();
    }

    public List<StockAudit> searchAudits(String keyword) {
        return auditRepository.search(keyword);
    }

    public StockAudit getAuditById(String id) {
        return auditRepository.findById(id);
    }
}
