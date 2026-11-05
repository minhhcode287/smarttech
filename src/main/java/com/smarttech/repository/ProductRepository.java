package com.smarttech.repository;

import com.mongodb.client.model.Filters;
import com.smarttech.model.Product;
import org.bson.Document;
import org.bson.conversions.Bson;

import java.util.ArrayList;
import java.util.List;

/**
 * Repository quản lý Mẫu/Dòng sản phẩm thiết bị trong collection "products".
 *
 * @author Senior Java Developer &amp; Software Architect
 * @project SmartTech
 */
public class ProductRepository extends AbstractMongoRepository<Product> {

    public ProductRepository() {
        super("products");
    }

    @Override
    protected Document toDocument(Product entity) {
        return entity != null ? entity.toDocument() : null;
    }

    @Override
    protected Product fromDocument(Document doc) {
        return Product.fromDocument(doc);
    }

    @Override
    protected String getEntityId(Product entity) {
        return entity != null ? entity.getId() : null;
    }

    /**
     * Lọc danh sách sản phẩm thuộc một danh mục cụ thể.
     *
     * @param categoryId Mã danh mục
     * @return Danh sách sản phẩm thuộc danh mục
     */
    public List<Product> findByCategoryId(String categoryId) {
        return findByField("categoryId", categoryId);
    }

    /**
     * Lọc sản phẩm theo thương hiệu (Brand).
     *
     * @param brand Tên thương hiệu (Apple, Sony, Dell,...)
     * @return Danh sách sản phẩm của thương hiệu
     */
    public List<Product> findByBrand(String brand) {
        return findByField("brand", brand);
    }

    /**
     * Tìm kiếm sản phẩm theo từ khóa tên (Regex không phân biệt hoa thường).
     *
     * @param keyword Từ khóa tìm kiếm
     * @return Danh sách sản phẩm khớp từ khóa
     */
    public List<Product> searchByName(String keyword) {
        List<Product> list = new ArrayList<>();
        if (keyword == null || keyword.trim().isEmpty()) {
            return list;
        }

        try {
            Bson filter = Filters.regex("productName", keyword.trim(), "i");
            for (Document doc : collection.find(filter)) {
                Product p = fromDocument(doc);
                if (p != null) {
                    list.add(p);
                }
            }
        } catch (Exception e) {
            System.err.println("[ProductRepository] Lỗi tìm kiếm sản phẩm: " + e.getMessage());
        }
        return list;
    }
}
