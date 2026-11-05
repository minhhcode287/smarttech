package com.smarttech.service;

import com.smarttech.model.Product;
import com.smarttech.repository.ProductRepository;

import java.util.ArrayList;
import java.util.List;

/**
 * Service xử lý các nghiệp vụ quản lý Mẫu sản phẩm thiết bị công nghệ (Product),
 * hỗ trợ tìm kiếm đa tiêu chí (theo tên, thương hiệu, danh mục, khoảng giá bán và giá thuê).
 *
 * @author Senior Java Developer &amp; Software Architect
 * @project SmartTech
 */
public class ProductService {

    private final ProductRepository productRepository;

    public ProductService() {
        this.productRepository = new ProductRepository();
    }

    public ProductService(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    public List<Product> getAllProducts() {
        return productRepository.findAll();
    }

    public Product getProductById(String id) {
        if (id == null || id.trim().isEmpty()) {
            return null;
        }
        return productRepository.findById(id.trim());
    }

    public boolean createProduct(Product product) {
        if (product == null) {
            System.err.println("[ProductService] Thông tin sản phẩm không được để trống!");
            return false;
        }

        if (product.getProductName() == null || product.getProductName().trim().isEmpty()) {
            System.err.println("[ProductService] Tên sản phẩm không được để trống!");
            return false;
        }

        if (product.getBasePrice() < 0 || product.getRentalRatePerDay() < 0) {
            System.err.println("[ProductService] Giá bán và đơn giá thuê không được âm!");
            return false;
        }

        return productRepository.save(product);
    }

    public boolean updateProduct(Product product) {
        if (product == null || product.getId() == null) {
            return false;
        }
        return productRepository.update(product);
    }

    public boolean deleteProduct(String id) {
        return productRepository.deleteById(id);
    }

    /**
     * Tìm kiếm sản phẩm theo tên (không phân biệt chữ hoa, chữ thường).
     *
     * @param keyword Từ khóa tìm kiếm
     * @return Danh sách sản phẩm phù hợp
     */
    public List<Product> searchByName(String keyword) {
        return productRepository.searchByName(keyword);
    }

    /**
     * Lọc sản phẩm theo danh mục.
     *
     * @param categoryId Mã danh mục
     * @return Danh sách sản phẩm trong danh mục
     */
    public List<Product> getProductsByCategory(String categoryId) {
        return productRepository.findByCategoryId(categoryId);
    }

    /**
     * Lọc sản phẩm theo khoảng giá bán.
     *
     * @param minPrice Giá thấp nhất
     * @param maxPrice Giá cao nhất
     * @return Danh sách sản phẩm trong tầm giá
     */
    public List<Product> getProductsByPriceRange(double minPrice, double maxPrice) {
        List<Product> result = new ArrayList<>();
        List<Product> all = getAllProducts();
        for (Product p : all) {
            if (p.getBasePrice() >= minPrice && p.getBasePrice() <= maxPrice) {
                result.add(p);
            }
        }
        return result;
    }
}
