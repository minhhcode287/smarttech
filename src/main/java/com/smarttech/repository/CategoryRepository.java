package com.smarttech.repository;

import com.smarttech.model.Category;
import org.bson.Document;

import java.util.List;

/**
 * Repository quản lý Danh mục sản phẩm công nghệ trong collection "categories".
 *
 * @author Senior Java Developer &amp; Software Architect
 * @project SmartTech
 */
public class CategoryRepository extends AbstractMongoRepository<Category> {

    public CategoryRepository() {
        super("categories");
    }

    @Override
    protected Document toDocument(Category entity) {
        return entity != null ? entity.toDocument() : null;
    }

    @Override
    protected Category fromDocument(Document doc) {
        return Category.fromDocument(doc);
    }

    @Override
    protected String getEntityId(Category entity) {
        return entity != null ? entity.getId() : null;
    }

    /**
     * Tìm danh mục theo tên chính xác.
     *
     * @param categoryName Tên danh mục
     * @return Đối tượng Category hoặc null
     */
    public Category findByName(String categoryName) {
        if (categoryName == null || categoryName.trim().isEmpty()) {
            return null;
        }
        List<Category> list = findByField("categoryName", categoryName.trim());
        return list.isEmpty() ? null : list.get(0);
    }
}
