package com.smarttech.service;

import com.smarttech.model.*;

import javax.swing.*;
import javax.swing.table.TableModel;
import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.text.DecimalFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Service xử lý xuất bản và tạo mẫu in cho Hóa Đơn Bán Hàng &amp; Hợp Đồng Cho Thuê Thiết Bị.
 * Hỗ trợ tạo HTML/CSS Responsive chuẩn in POS, xuất file định dạng HTML, Text (.txt) và CSV có UTF-8 BOM.
 *
 * @author Senior Java Developer &amp; Software Architect
 * @project SmartTech
 */
public class ExportService {

    private static final DecimalFormat CURRENCY_FORMAT = new DecimalFormat("#,##0 VNĐ");
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter DATETIME_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");

    public ExportService() {
    }

    /**
     * Tạo mã HTML mẫu Hóa đơn Bán lẻ thiết bị công nghệ chuẩn POS hiện đại.
     *
     * @param order    Thông tin đơn hàng
     * @param customer Thông tin khách hàng
     * @param employee Thông tin nhân viên lập đơn
     * @return Chuỗi HTML hoàn chỉnh sẵn sàng in và hiển thị trên JEditorPane / Trình duyệt
     */
    public String generateSaleInvoiceHtml(Order order, Customer customer, Employee employee) {
        if (order == null) {
            return "<html><body><h3>Không tìm thấy dữ liệu hóa đơn!</h3></body></html>";
        }

        String orderId = order.getId() != null ? order.getId() : "N/A";
        String orderDateStr = order.getOrderDate() != null ? order.getOrderDate().format(DATE_FORMATTER) : LocalDate.now().format(DATE_FORMATTER);
        String nowStr = LocalDateTime.now().format(DATETIME_FORMATTER);

        String custName = customer != null ? customer.getFullName() : (order.getCustomerName() != null ? order.getCustomerName() : "Khách lẻ");
        String custPhone = customer != null ? customer.getPhone() : (order.getCustomerPhone() != null ? order.getCustomerPhone() : "Chưa cập nhật");
        String custAddress = customer != null && customer.getAddress() != null && !customer.getAddress().isEmpty() ? customer.getAddress() : "Tại cửa hàng";
        String custType = customer != null && customer.getCustomerType() != null ? customer.getCustomerType() : "INDIVIDUAL";
        int rewardPoints = customer != null ? customer.getRewardPoints() : 0;

        String empName = employee != null ? employee.getFullName() : (order.getEmployeeId() != null ? order.getEmployeeId() : "Thu ngân SmartTech");
        String paymentMethod = "CASH".equalsIgnoreCase(order.getPaymentMethod()) ? "Tiền mặt (CASH)" : "Chuyển khoản (BANK_TRANSFER)";

        StringBuilder sb = new StringBuilder();
        sb.append("<!DOCTYPE html>\n")
                .append("<html lang=\"vi\">\n")
                .append("<head>\n")
                .append("<meta charset=\"UTF-8\">\n")
                .append("<title>Hóa Đơn Bán Hàng - ").append(orderId).append("</title>\n")
                .append("<style>\n")
                .append("  body { font-family: 'Segoe UI', Arial, sans-serif; background-color: #ffffff; color: #1e293b; margin: 0; padding: 20px; line-height: 1.5; }\n")
                .append("  .invoice-container { max-width: 680px; margin: 0 auto; padding: 24px; border: 1px solid #cbd5e1; border-radius: 8px; background: #ffffff; }\n")
                .append("  .header { text-align: center; border-bottom: 2px dashed #94a3b8; padding-bottom: 14px; margin-bottom: 16px; }\n")
                .append("  .company-title { font-size: 20px; font-weight: bold; color: #1e40af; text-transform: uppercase; margin: 0; }\n")
                .append("  .company-meta { font-size: 12px; color: #64748b; margin-top: 4px; }\n")
                .append("  .invoice-heading { font-size: 22px; font-weight: bold; color: #0f172a; text-transform: uppercase; margin: 14px 0 4px 0; }\n")
                .append("  .invoice-code { font-size: 14px; font-weight: bold; color: #2563eb; margin: 0; }\n")
                .append("  .info-table { width: 100%; border-collapse: collapse; margin-bottom: 16px; font-size: 13px; }\n")
                .append("  .info-table td { padding: 4px 8px; vertical-align: top; }\n")
                .append("  .items-table { width: 100%; border-collapse: collapse; margin-bottom: 18px; font-size: 13px; }\n")
                .append("  .items-table th { background-color: #f1f5f9; color: #334155; font-weight: bold; border: 1px solid #cbd5e1; padding: 8px 10px; text-align: left; }\n")
                .append("  .items-table td { border: 1px solid #cbd5e1; padding: 8px 10px; }\n")
                .append("  .items-table .text-center { text-align: center; }\n")
                .append("  .items-table .text-right { text-align: right; }\n")
                .append("  .summary-box { width: 100%; border-collapse: collapse; font-size: 13px; margin-bottom: 20px; }\n")
                .append("  .summary-box td { padding: 4px 8px; }\n")
                .append("  .final-total { font-size: 18px; font-weight: bold; color: #dc2626; }\n")
                .append("  .paid-badge { display: inline-block; background-color: #dcfce7; color: #15803d; font-weight: bold; padding: 2px 10px; border-radius: 12px; font-size: 11px; border: 1px solid #86efac; }\n")
                .append("  .footer { text-align: center; border-top: 2px dashed #94a3b8; padding-top: 14px; font-size: 12px; color: #64748b; }\n")
                .append("  .qr-box { display: inline-block; border: 1px solid #cbd5e1; padding: 8px 16px; border-radius: 4px; background: #f8fafc; font-family: monospace; font-weight: bold; margin-bottom: 8px; color: #0f172a; }\n")
                .append("</style>\n")
                .append("</head>\n")
                .append("<body>\n")
                .append("<div class=\"invoice-container\">\n")
                .append("  <div class=\"header\">\n")
                .append("    <div class=\"company-title\">SMARTTECH ENTERPRISE - HỆ THỐNG CÔNG NGHỆ CAO</div>\n")
                .append("    <div class=\"company-meta\">Tòa nhà SmartTech, Khu Công Nghệ Cao, TP. Hồ Chí Minh</div>\n")
                .append("    <div class=\"company-meta\">Hotline: 1900 6868 | Email: contact@smarttech.vn | Website: www.smarttech.vn</div>\n")
                .append("    <div class=\"invoice-heading\">HÓA ĐƠN BÁN HÀNG LẺ</div>\n")
                .append("    <div class=\"invoice-code\">Mã đơn: ").append(orderId).append("</div>\n")
                .append("    <div class=\"company-meta\">Thời gian in: ").append(nowStr).append("</div>\n")
                .append("  </div>\n");

        // Bảng thông tin giao dịch
        sb.append("  <table class=\"info-table\">\n")
                .append("    <tr>\n")
                .append("      <td style=\"width: 50%;\"><strong>Khách hàng:</strong> ").append(custName).append("</td>\n")
                .append("      <td style=\"width: 50%;\"><strong>Nhân viên bán hàng:</strong> ").append(empName).append("</td>\n")
                .append("    </tr>\n")
                .append("    <tr>\n")
                .append("      <td><strong>Số điện thoại:</strong> ").append(custPhone).append("</td>\n")
                .append("      <td><strong>Phương thức TT:</strong> ").append(paymentMethod).append("</td>\n")
                .append("    </tr>\n")
                .append("    <tr>\n")
                .append("      <td><strong>Địa chỉ:</strong> ").append(custAddress).append("</td>\n")
                .append("      <td><strong>Trạng thái:</strong> <span class=\"paid-badge\">ĐÃ THANH TOÁN</span></td>\n")
                .append("    </tr>\n")
                .append("    <tr>\n")
                .append("      <td><strong>Loại khách / Điểm:</strong> ").append(custType).append(" (").append(rewardPoints).append(" điểm)</td>\n")
                .append("      <td><strong>Ngày mua:</strong> ").append(orderDateStr).append("</td>\n")
                .append("    </tr>\n")
                .append("  </table>\n");

        // Bảng chi tiết sản phẩm
        sb.append("  <table class=\"items-table\">\n")
                .append("    <thead>\n")
                .append("      <tr>\n")
                .append("        <th class=\"text-center\" style=\"width: 35px;\">STT</th>\n")
                .append("        <th style=\"width: 80px;\">Mã SP</th>\n")
                .append("        <th>Tên Sản Phẩm / Thiết Bị</th>\n")
                .append("        <th style=\"width: 120px;\">Số Serial</th>\n")
                .append("        <th class=\"text-right\" style=\"width: 95px;\">Đơn Giá</th>\n")
                .append("        <th class=\"text-right\" style=\"width: 105px;\">Thành Tiền</th>\n")
                .append("      </tr>\n")
                .append("    </thead>\n")
                .append("    <tbody>\n");

        int stt = 1;
        if (order.getItems() != null && !order.getItems().isEmpty()) {
            for (OrderItem it : order.getItems()) {
                sb.append("      <tr>\n")
                        .append("        <td class=\"text-center\">").append(stt++).append("</td>\n")
                        .append("        <td>").append(it.getProductId() != null ? it.getProductId() : "").append("</td>\n")
                        .append("        <td><strong>").append(it.getProductName() != null ? it.getProductName() : "").append("</strong></td>\n")
                        .append("        <td><code style=\"color: #0284c7; font-weight: bold;\">").append(it.getSerialId() != null ? it.getSerialId() : "").append("</code></td>\n")
                        .append("        <td class=\"text-right\">").append(CURRENCY_FORMAT.format(it.getUnitPrice())).append("</td>\n")
                        .append("        <td class=\"text-right\"><strong>").append(CURRENCY_FORMAT.format(it.getSubtotal())).append("</strong></td>\n")
                        .append("      </tr>\n");
            }
        } else {
            sb.append("      <tr><td colspan=\"6\" class=\"text-center\">Không có sản phẩm nào trong đơn hàng.</td></tr>\n");
        }

        sb.append("    </tbody>\n")
                .append("  </table>\n");

        // Bảng tổng hợp thanh toán
        sb.append("  <table class=\"summary-box\">\n")
                .append("    <tr>\n")
                .append("      <td style=\"text-align: right; width: 75%;\">Tổng tiền hàng:</td>\n")
                .append("      <td style=\"text-align: right; width: 25%; font-weight: bold;\">").append(CURRENCY_FORMAT.format(order.getTotalAmount())).append("</td>\n")
                .append("    </tr>\n");

        if (order.getPointsUsed() > 0) {
            sb.append("    <tr>\n")
                    .append("      <td style=\"text-align: right; color: #059669;\">Khấu trừ điểm thưởng (").append(order.getPointsUsed()).append(" điểm):</td>\n")
                    .append("      <td style=\"text-align: right; color: #059669; font-weight: bold;\">-").append(CURRENCY_FORMAT.format(order.getDiscountAmount())).append("</td>\n")
                    .append("    </tr>\n");
        }

        sb.append("    <tr>\n")
                .append("      <td style=\"text-align: right; border-top: 1px solid #cbd5e1; padding-top: 8px; font-weight: bold;\">TỔNG TIỀN THỰC THU:</td>\n")
                .append("      <td style=\"text-align: right; border-top: 1px solid #cbd5e1; padding-top: 8px;\" class=\"final-total\">")
                .append(CURRENCY_FORMAT.format(order.getFinalAmount())).append("</td>\n")
                .append("    </tr>\n")
                .append("  </table>\n");

        // Footer
        sb.append("  <div class=\"footer\">\n")
                .append("    <div class=\"qr-box\">[ QR TRA CỨU BẢO HÀNH: ").append(orderId).append(" ]</div>\n")
                .append("    <div>Cảm ơn Quý khách đã tin tưởng và đồng hành cùng <strong>SmartTech</strong>!</div>\n")
                .append("    <div>Thiết bị được kích hoạt bảo hành điện tử chính hãng theo số <strong>Serial Number</strong>.</div>\n")
                .append("  </div>\n")
                .append("</div>\n")
                .append("</body>\n")
                .append("</html>");

        return sb.toString();
    }

    /**
     * Tạo mã HTML mẫu Hợp đồng Cho thuê thiết bị công nghệ chuẩn pháp lý doanh nghiệp.
     *
     * @param contract Thông tin hợp đồng thuê
     * @param customer Thông tin khách hàng thuê
     * @param employee Thông tin nhân viên đại diện SmartTech
     * @return Chuỗi HTML chuẩn pháp lý, bố cục 2 bên và bảng danh sách bàn giao thiết bị
     */
    public String generateRentalContractHtml(RentalContract contract, Customer customer, Employee employee) {
        if (contract == null) {
            return "<html><body><h3>Không tìm thấy dữ liệu hợp đồng thuê!</h3></body></html>";
        }

        String contractId = contract.getId() != null ? contract.getId() : "N/A";
        String startDateStr = contract.getStartDate() != null ? contract.getStartDate().format(DATE_FORMATTER) : LocalDate.now().format(DATE_FORMATTER);
        String expectedDateStr = contract.getExpectedEndDate() != null ? contract.getExpectedEndDate().format(DATE_FORMATTER) : LocalDate.now().plusDays(3).format(DATE_FORMATTER);

        String custName = customer != null ? customer.getFullName() : (contract.getCustomerName() != null ? contract.getCustomerName() : "Khách hàng thuê máy");
        String custPhone = customer != null ? customer.getPhone() : (contract.getCustomerPhone() != null ? contract.getCustomerPhone() : "Chưa cập nhật");
        String custAddress = customer != null && customer.getAddress() != null && !customer.getAddress().isEmpty() ? customer.getAddress() : "Tại nơi cư trú";

        String empName = employee != null ? employee.getFullName() : "Đại diện kinh doanh SmartTech";
        String empPosition = employee != null && employee.getPosition() != null ? employee.getPosition() : "Chuyên viên tư vấn & Quản lý thiết bị";

        double totalRent = contract.getTotalRentalFee();
        double totalDeposit = contract.getTotalDeposit();
        double grandTotal = totalRent + totalDeposit;

        StringBuilder sb = new StringBuilder();
        sb.append("<!DOCTYPE html>\n")
                .append("<html lang=\"vi\">\n")
                .append("<head>\n")
                .append("<meta charset=\"UTF-8\">\n")
                .append("<title>Hợp Đồng Cho Thuê Thiết Bị - ").append(contractId).append("</title>\n")
                .append("<style>\n")
                .append("  body { font-family: 'Segoe UI', Arial, sans-serif; background-color: #ffffff; color: #0f172a; margin: 0; padding: 24px; line-height: 1.6; }\n")
                .append("  .contract-container { max-width: 720px; margin: 0 auto; padding: 30px; border: 1px solid #cbd5e1; border-radius: 8px; background: #ffffff; }\n")
                .append("  .national-header { text-align: center; margin-bottom: 20px; }\n")
                .append("  .country-title { font-size: 14px; font-weight: bold; text-transform: uppercase; margin: 0; }\n")
                .append("  .motto { font-size: 13px; font-weight: bold; margin-top: 2px; }\n")
                .append("  .divider-center { width: 140px; margin: 6px auto; border-top: 1px solid #475569; }\n")
                .append("  .contract-title { font-size: 20px; font-weight: bold; color: #1e3a8a; text-transform: uppercase; text-align: center; margin: 18px 0 4px 0; }\n")
                .append("  .contract-code { font-size: 13px; text-align: center; color: #475569; margin-bottom: 22px; }\n")
                .append("  .section-title { font-size: 14px; font-weight: bold; color: #1e293b; margin: 16px 0 8px 0; text-transform: uppercase; border-left: 3px solid #2563eb; padding-left: 8px; }\n")
                .append("  .party-table { width: 100%; border-collapse: collapse; font-size: 13px; margin-bottom: 12px; }\n")
                .append("  .party-table td { padding: 3px 6px; vertical-align: top; }\n")
                .append("  .items-table { width: 100%; border-collapse: collapse; margin: 12px 0 16px 0; font-size: 13px; }\n")
                .append("  .items-table th { background-color: #f8fafc; color: #1e293b; font-weight: bold; border: 1px solid #cbd5e1; padding: 8px; text-align: left; }\n")
                .append("  .items-table td { border: 1px solid #cbd5e1; padding: 8px; }\n")
                .append("  .text-center { text-align: center; }\n")
                .append("  .text-right { text-align: right; }\n")
                .append("  .terms { font-size: 12.5px; color: #334155; margin-bottom: 12px; text-align: justify; }\n")
                .append("  .terms li { margin-bottom: 4px; }\n")
                .append("  .signature-box { width: 100%; border-collapse: collapse; margin-top: 36px; text-align: center; font-size: 13px; }\n")
                .append("  .signature-box td { width: 50%; vertical-align: top; padding: 10px; }\n")
                .append("  .sign-role { font-weight: bold; text-transform: uppercase; margin-bottom: 4px; }\n")
                .append("  .sign-note { font-style: italic; font-size: 11.5px; color: #64748b; margin-bottom: 70px; }\n")
                .append("</style>\n")
                .append("</head>\n")
                .append("<body>\n")
                .append("<div class=\"contract-container\">\n")
                .append("  <div class=\"national-header\">\n")
                .append("    <div class=\"country-title\">CỘNG HÒA XÃ HỘI CHỦ NGHĨA VIỆT NAM</div>\n")
                .append("    <div class=\"motto\">Độc lập - Tự do - Hạnh phúc</div>\n")
                .append("    <div class=\"divider-center\"></div>\n")
                .append("    <div class=\"contract-title\">HỢP ĐỒNG CHO THUÊ VÀ BÀN GIAO THIẾT BỊ CÔNG NGHỆ</div>\n")
                .append("    <div class=\"contract-code\">Số HĐ: <strong>").append(contractId).append("</strong> | Ngày lập: ").append(startDateStr).append("</div>\n")
                .append("  </div>\n");

        // Điều 1: Đại diện hai bên
        sb.append("  <div class=\"section-title\">ĐIỀU 1: ĐẠI DIỆN CÁC BÊN THAM GIA HỢP ĐỒNG</div>\n")
                .append("  <table class=\"party-table\">\n")
                .append("    <tr>\n")
                .append("      <td colspan=\"2\"><strong>BÊN CHO THUÊ (BÊN A): CÔNG TY CÔNG NGHỆ SMARTTECH ENTERPRISE</strong></td>\n")
                .append("    </tr>\n")
                .append("    <tr>\n")
                .append("      <td style=\"width: 50%;\">• Đại diện: <strong>").append(empName).append("</strong></td>\n")
                .append("      <td style=\"width: 50%;\">• Chức vụ: ").append(empPosition).append("</td>\n")
                .append("    </tr>\n")
                .append("    <tr>\n")
                .append("      <td>• Địa chỉ: Tòa nhà SmartTech, Khu Công Nghệ Cao, TP.HCM</td>\n")
                .append("      <td>• Hotline hỗ trợ: 1900 6868</td>\n")
                .append("    </tr>\n")
                .append("    <tr>\n")
                .append("      <td colspan=\"2\" style=\"padding-top: 8px;\"><strong>BÊN THUÊ THIẾT BỊ (BÊN B):</strong></td>\n")
                .append("    </tr>\n")
                .append("    <tr>\n")
                .append("      <td>• Họ và tên: <strong>").append(custName).append("</strong></td>\n")
                .append("      <td>• Số điện thoại: <strong>").append(custPhone).append("</strong></td>\n")
                .append("    </tr>\n")
                .append("    <tr>\n")
                .append("      <td colspan=\"2\">• Địa chỉ liên hệ: ").append(custAddress).append("</td>\n")
                .append("    </tr>\n")
                .append("  </table>\n");

        // Điều 2: Nội dung & danh sách thiết bị bàn giao
        sb.append("  <div class=\"section-title\">ĐIỀU 2: NỘI DUNG THUÊ &amp; THIẾT BỊ BÀN GIAO THỰC TẾ</div>\n")
                .append("  <div class=\"terms\">\n")
                .append("    Bên A đồng ý bàn giao và Bên B đồng ý thuê các thiết bị công nghệ nguyên tem bảo hành với danh mục chi tiết sau:\n")
                .append("  </div>\n")
                .append("  <table class=\"items-table\">\n")
                .append("    <thead>\n")
                .append("      <tr>\n")
                .append("        <th class=\"text-center\" style=\"width: 35px;\">STT</th>\n")
                .append("        <th>Tên Thiết Bị / Mẫu Máy</th>\n")
                .append("        <th style=\"width: 130px;\">Mã Serial Bàn Giao</th>\n")
                .append("        <th class=\"text-right\" style=\"width: 105px;\">Giá Thuê/Ngày</th>\n")
                .append("        <th class=\"text-right\" style=\"width: 110px;\">Tiền Cọc Máy</th>\n")
                .append("      </tr>\n")
                .append("    </thead>\n")
                .append("    <tbody>\n");

        int rentStt = 1;
        if (contract.getItems() != null && !contract.getItems().isEmpty()) {
            for (RentalItem ri : contract.getItems()) {
                sb.append("      <tr>\n")
                        .append("        <td class=\"text-center\">").append(rentStt++).append("</td>\n")
                        .append("        <td><strong>").append(ri.getProductName() != null ? ri.getProductName() : "").append("</strong></td>\n")
                        .append("        <td><code style=\"color: #0284c7; font-weight: bold;\">").append(ri.getSerialId() != null ? ri.getSerialId() : "").append("</code></td>\n")
                        .append("        <td class=\"text-right\">").append(CURRENCY_FORMAT.format(ri.getRentalRatePerDay())).append("</td>\n")
                        .append("        <td class=\"text-right\">").append(CURRENCY_FORMAT.format(ri.getDepositAmount())).append("</td>\n")
                        .append("      </tr>\n");
            }
        }

        sb.append("    </tbody>\n")
                .append("  </table>\n")
                .append("  <table class=\"party-table\">\n")
                .append("    <tr>\n")
                .append("      <td>• <strong>Thời gian thuê cam kết:</strong> ").append(contract.getRentalDays()).append(" Ngày</td>\n")
                .append("      <td>• <strong>Ngày nhận bàn giao:</strong> ").append(startDateStr).append("</td>\n")
                .append("    </tr>\n")
                .append("    <tr>\n")
                .append("      <td colspan=\"2\">• <strong>Hạn hoàn trả thiết bị dự kiến:</strong> <span style=\"color: #dc2626; font-weight: bold;\">").append(expectedDateStr).append("</span></td>\n")
                .append("    </tr>\n")
                .append("  </table>\n");

        // Điều 3: Chi phí & tiền cọc
        sb.append("  <div class=\"section-title\">ĐIỀU 3: CHI PHÍ THUÊ &amp; TIỀN ĐẶT CỌC BẢO ĐẢM</div>\n")
                .append("  <table class=\"party-table\">\n")
                .append("    <tr>\n")
                .append("      <td style=\"width: 65%;\">1. Tổng tiền thuê dự kiến (").append(contract.getRentalDays()).append(" ngày):</td>\n")
                .append("      <td style=\"width: 35%; text-align: right; font-weight: bold;\">").append(CURRENCY_FORMAT.format(totalRent)).append("</td>\n")
                .append("    </tr>\n")
                .append("    <tr>\n")
                .append("      <td>2. Tổng tiền đặt cọc giữ thiết bị:</td>\n")
                .append("      <td style=\"text-align: right; font-weight: bold; color: #0284c7;\">").append(CURRENCY_FORMAT.format(totalDeposit)).append("</td>\n")
                .append("    </tr>\n")
                .append("    <tr>\n")
                .append("      <td style=\"border-top: 1px solid #cbd5e1; padding-top: 6px; font-weight: bold;\">TỔNG TIỀN BÊN B ĐÃ THANH TOÁN KHI NHẬN MÁY:</td>\n")
                .append("      <td style=\"border-top: 1px solid #cbd5e1; padding-top: 6px; text-align: right; font-size: 16px; font-weight: bold; color: #16a34a;\">")
                .append(CURRENCY_FORMAT.format(grandTotal)).append("</td>\n")
                .append("    </tr>\n")
                .append("  </table>\n");

        // Điều 4: Trách nhiệm & bồi thường
        sb.append("  <div class=\"section-title\">ĐIỀU 4: TRÁCH NHIỆM BẢO QUẢN &amp; QUY ĐỊNH BỒI THƯỜNG</div>\n")
                .append("  <ol class=\"terms\">\n")
                .append("    <li>Bên B có trách nhiệm kiểm tra kỹ tình trạng phần cứng, màn hình, phụ kiện kèm theo và số Serial của thiết bị trước khi ký nhận bàn giao.</li>\n")
                .append("    <li>Bên B không được tự ý tháo dỡ, can thiệp phần cứng, làm rách tem niêm phong hoặc cài đặt các phần mềm gây hại cho thiết bị.</li>\n")
                .append("    <li><strong>Quy định quá hạn:</strong> Nếu trả thiết bị trễ hạn so với ngày cam kết (").append(expectedDateStr).append("), Bên B chịu phí phát sinh bằng 150% đơn giá ngày nhân với số ngày trễ.</li>\n")
                .append("    <li><strong>Hoàn cọc:</strong> Khi nhận lại thiết bị đầy đủ, nguyên vẹn hoạt động tốt, Bên A có trách nhiệm hoàn trả 100% tiền đặt cọc (").append(CURRENCY_FORMAT.format(totalDeposit)).append(") cho Bên B ngay tại thời điểm thanh lý.</li>\n")
                .append("  </ol>\n");

        // Điều 5 & Chữ ký
        sb.append("  <div class=\"section-title\">ĐIỀU 5: HIỆU LỰC &amp; XÁC NHẬN KÝ BÀN GIAO</div>\n")
                .append("  <div class=\"terms\">\n")
                .append("    Hợp đồng được lập thành 02 bản có giá trị pháp lý như nhau, mỗi bên giữ 01 bản để thực hiện.\n")
                .append("  </div>\n")
                .append("  <table class=\"signature-box\">\n")
                .append("    <tr>\n")
                .append("      <td>\n")
                .append("        <div class=\"sign-role\">ĐẠI DIỆN BÊN CHO THUÊ (BÊN A)</div>\n")
                .append("        <div class=\"sign-note\">(Ký, ghi rõ họ tên &amp; đóng dấu)</div>\n")
                .append("        <div style=\"font-weight: bold;\">").append(empName).append("</div>\n")
                .append("      </td>\n")
                .append("      <td>\n")
                .append("        <div class=\"sign-role\">ĐẠI DIỆN BÊN THUÊ (BÊN B)</div>\n")
                .append("        <div class=\"sign-note\">(Ký &amp; ghi rõ họ tên)</div>\n")
                .append("        <div style=\"font-weight: bold;\">").append(custName).append("</div>\n")
                .append("      </td>\n")
                .append("    </tr>\n")
                .append("  </table>\n")
                .append("</div>\n")
                .append("</body>\n")
                .append("</html>");

        return sb.toString();
    }

    /**
     * Xuất nội dung HTML ra file định dạng .html chuẩn UTF-8.
     *
     * @param htmlContent Nội dung mã HTML
     * @param targetFile  File đích cần ghi
     * @return true nếu ghi file thành công
     */
    public boolean exportHtmlToFile(String htmlContent, File targetFile) {
        if (htmlContent == null || targetFile == null) {
            return false;
        }

        try (OutputStreamWriter writer = new OutputStreamWriter(new FileOutputStream(targetFile), StandardCharsets.UTF_8)) {
            writer.write(htmlContent);
            writer.flush();
            System.out.println("[ExportService] Đã xuất file HTML thành công: " + targetFile.getAbsolutePath());
            return true;
        } catch (Exception e) {
            System.err.println("[ExportService] Lỗi khi xuất HTML: " + e.getMessage());
            return false;
        }
    }

    /**
     * Xuất nội dung văn bản thuần túy ra file .txt chuẩn UTF-8.
     *
     * @param textContent Nội dung chuỗi text
     * @param targetFile  File đích
     * @return true nếu ghi file thành công
     */
    public boolean exportToTextFile(String textContent, File targetFile) {
        if (textContent == null || targetFile == null) {
            return false;
        }

        try (OutputStreamWriter writer = new OutputStreamWriter(new FileOutputStream(targetFile), StandardCharsets.UTF_8)) {
            writer.write(textContent);
            writer.flush();
            System.out.println("[ExportService] Đã xuất file Text thành công: " + targetFile.getAbsolutePath());
            return true;
        } catch (Exception e) {
            System.err.println("[ExportService] Lỗi khi xuất Text: " + e.getMessage());
            return false;
        }
    }

    /**
     * Xuất dữ liệu từ JTable ra file .csv có chèn mã nhận dạng UTF-8 BOM (\uFEFF)
     * giúp Microsoft Excel và các phần mềm bảng tính tự động mở đúng tiếng Việt có dấu 100%.
     *
     * @param table      JTable chứa dữ liệu
     * @param targetFile File đích .csv
     * @return true nếu xuất bảng thành công
     */
    public boolean exportTableToCsv(JTable table, File targetFile) {
        if (table == null || targetFile == null) {
            return false;
        }

        try (OutputStreamWriter writer = new OutputStreamWriter(new FileOutputStream(targetFile), StandardCharsets.UTF_8)) {
            // Chèn ký tự UTF-8 BOM
            writer.write("\uFEFF");

            TableModel model = table.getModel();
            int colCount = model.getColumnCount();
            int rowCount = model.getRowCount();

            // Ghi dòng tiêu đề Header
            for (int col = 0; col < colCount; col++) {
                String colName = model.getColumnName(col);
                writer.write(escapeCsv(colName));
                if (col < colCount - 1) {
                    writer.write(",");
                }
            }
            writer.write("\r\n");

            // Ghi từng dòng dữ liệu
            for (int row = 0; row < rowCount; row++) {
                for (int col = 0; col < colCount; col++) {
                    Object val = model.getValueAt(row, col);
                    String text = val != null ? val.toString() : "";
                    writer.write(escapeCsv(text));
                    if (col < colCount - 1) {
                        writer.write(",");
                    }
                }
                writer.write("\r\n");
            }

            writer.flush();
            System.out.println("[ExportService] Đã xuất file CSV thành công: " + targetFile.getAbsolutePath());
            return true;
        } catch (Exception e) {
            System.err.println("[ExportService] Lỗi khi xuất CSV: " + e.getMessage());
            return false;
        }
    }

    /**
     * Thoát ký tự đặc biệt cho chuẩn định dạng CSV (dấu phẩy, ngoặc kép, xuống dòng).
     */
    private String escapeCsv(String input) {
        if (input == null) return "\"\"";
        String s = input.trim();
        if (s.contains(",") || s.contains("\"") || s.contains("\n") || s.contains("\r")) {
            s = s.replace("\"", "\"\"");
            return "\"" + s + "\"";
        }
        return "\"" + s + "\"";
    }
}
