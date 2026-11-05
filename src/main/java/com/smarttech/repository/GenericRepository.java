package com.smarttech.repository;

import java.util.List;

/**
 * Interface Generic đại diện cho các thao tác CRUD cơ bản trên tầng lưu trữ dữ liệu (Repository Pattern).
 *
 * @param <T> Kiểu thực thể Model
 * @author Senior Java Developer &amp; Software Architect
 * @project SmartTech
 */
public interface GenericRepository<T> {

    /**
     * Lấy toàn bộ danh sách thực thể từ cơ sở dữ liệu.
     *
     * @return Danh sách các thực thể T
     */
    List<T> findAll();

    /**
     * Tìm kiếm một thực thể theo định danh khoá chính (ID / _id).
     *
     * @param id Khóa chính của thực thể
     * @return Thực thể T hoặc null nếu không tìm thấy
     */
    T findById(String id);

    /**
     * Thêm mới một thực thể vào cơ sở dữ liệu.
     *
     * @param entity Thực thể cần lưu
     * @return true nếu lưu thành công, ngược lại false
     */
    boolean save(T entity);

    /**
     * Cập nhật thông tin thực thể đã tồn tại trong cơ sở dữ liệu.
     *
     * @param entity Thực thể cần cập nhật
     * @return true nếu cập nhật thành công, ngược lại false
     */
    boolean update(T entity);

    /**
     * Xóa một thực thể khỏi cơ sở dữ liệu theo ID.
     *
     * @param id Khóa chính của thực thể cần xóa
     * @return true nếu xóa thành công, ngược lại false
     */
    boolean deleteById(String id);

    /**
     * Tìm kiếm danh sách thực thể theo một trường bất kỳ và giá trị tương ứng.
     *
     * @param fieldName Tên trường cần lọc (ví dụ: "brand", "role", "status")
     * @param value     Giá trị tương ứng cần tìm kiếm
     * @return Danh sách các thực thể khớp điều kiện
     */
    List<T> findByField(String fieldName, Object value);
}
