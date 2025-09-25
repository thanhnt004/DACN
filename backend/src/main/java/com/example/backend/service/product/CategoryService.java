package com.example.backend.service.product;

import com.example.backend.dto.request.product.CategoryCreateRequest;
import com.example.backend.dto.request.product.CategoryUpdateRequest;
import com.example.backend.dto.response.product.CategoryDto;
import com.example.backend.excepton.BadRequestException;
import com.example.backend.excepton.ConflictException;
import com.example.backend.excepton.NotFoundException;
import com.example.backend.mapper.CategoryMapper;
import com.example.backend.model.product.Category;
import com.example.backend.repository.product.CategoryRepository;
import com.example.backend.repository.product.ProductRepository;
import com.example.backend.util.SlugUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CategoryService {
    private final CategoryRepository categoryRepository;
    private final CategoryMapper categoryMapper;

    private boolean ensureSlugUnique(String slug,UUID excludeId){
        var exists = categoryRepository.existsBySlugAndIdNot(slug,excludeId);
        return !exists;
    }
    private void validateNoCircularReference(UUID categoryId, UUID parentId) {
        if (categoryId != null && categoryId.equals(parentId)) {
            throw new ConflictException("Category cannot be parent of itself");
        }
        // Check for circular reference by traversing up the parent chain
        UUID currentParentId = parentId;
        Set<UUID> visited = new HashSet<>();

        while (currentParentId != null) {
            if (visited.contains(currentParentId)) {
                throw new ConflictException("Circular reference detected in category hierarchy");
            }

            if (categoryId != null && categoryId.equals(currentParentId)) {
                throw new ConflictException("Creating circular reference in category hierarchy");
            }

            visited.add(currentParentId);

            Optional<Category> parent = categoryRepository.findById(currentParentId);
            currentParentId = parent.map(Category::getParentId).orElse(null);
        }
    }

    public CategoryDto createCateGory(CategoryCreateRequest request)
    {
        //Tạo slug nếu trùng
        String slug = request.getSlug();
        slug = SlugUtil.uniqueSlug(slug, e->this.ensureSlugUnique(e,null));

        UUID parentId = request.getParentId();
        if (parentId != null)
        {
            if (!categoryRepository.existsById(parentId))
                throw new NotFoundException("Parent category not found");
        }
        Category category = Category.builder()
                .name(request.getName())
                .slug(slug)
                .description(request.getDescription())
                .parentId(request.getParentId())
                .build();
        category = categoryRepository.save(category);
        return categoryMapper.toDto(category);
    }
    @Transactional
    public CategoryDto update(UUID id, CategoryUpdateRequest req) {
        var c = categoryRepository.findById(id).orElseThrow(() -> new NotFoundException("Category not found"));

        Category parent = null;
        if (req.getParentId() != null) parent = categoryRepository.findById(req.getParentId())
                .orElseThrow(() -> new NotFoundException("Parent category not found"));
        validateNoCircularReference(id,req.getParentId());

        categoryMapper.updateFromDto(req,c);
        if (req.getSlug()!=null)
        {
           c.setSlug(SlugUtil.uniqueSlug(req.getSlug(),e->this.ensureSlugUnique(e,id)));
        }

        return categoryMapper.toDto(c);
    }

    @Transactional
    public void delete(UUID categoryId,Boolean force,UUID reassignTo)
    {
        var c = categoryRepository.findById(categoryId).orElseThrow(() -> new NotFoundException("Category not found"));
        if(categoryRepository.existsByParentId(categoryId))
            throw new ConflictException("Cannot delete category with subcategories");
        if(!force)
        {
            long productCount = categoryRepository.countProduct(categoryId);
            if (productCount>0)
                throw new ConflictException("Cannot delete category with products");
        }
        if(reassignTo != null)
        {
            if (!categoryRepository.existsById(reassignTo)) throw new NotFoundException("reassignTo not found");
            //chuyen product sang category khac
            throw new NotFoundException("ssss");
        }
        categoryRepository.delete(c);
    }
    @Async
    @Transactional
    public void move(UUID categoryId,UUID newParentId)
    {
        Category cat = categoryRepository.findById(categoryId)
                .orElseThrow(() -> new NotFoundException("Category not found"));
        if (Objects.equals(categoryId, newParentId)) {
            throw new BadRequestException("A category cannot be its own parent");
        }
        if (newParentId != null) {
            categoryRepository.findById(newParentId)
                    .orElseThrow(() -> new NotFoundException("New parent not found"));
            validateNoCircularReference(categoryId, newParentId);
        }
        UUID oldParentId = cat.getParentId();
        boolean sameParent = Objects.equals(oldParentId, newParentId);
        if(sameParent)
            throw new BadRequestException("Same parent!");
        cat.setParentId(newParentId);
        categoryRepository.save(cat);
    }
    @Transactional(readOnly = true)
    public Page<CategoryDto> list(UUID parentId, Boolean isDeleted, String q, Pageable pageable) {
    // Simple Specification
        var spec = (Specification<Category>) (root, query, cb) -> {
            List<jakarta.persistence.criteria.Predicate> ps = new ArrayList<>();
            if (parentId == null) ps.add(cb.isNull(root.get("parent")));
            else ps.add(cb.equal(root.get("parent").get("id"), parentId));
            if (isDeleted != null)
            {
                if (isDeleted)
                    ps.add(cb.isNull(root.get("deletedAt")));
                else
                    ps.add(cb.isNotNull(root.get("deletedAt")));
            }
            if (q != null && !q.isBlank()) {
                String like = "%" + q.trim().toLowerCase(Locale.ROOT) + "%";
                ps.add(cb.or(cb.like(cb.lower(root.get("name")), like), cb.like(cb.lower(root.get("slug")), like)));
            }
            assert query != null;
            query.orderBy(cb.desc(root.get("name")));
            return cb.and(ps.toArray(new jakarta.persistence.criteria.Predicate[0]));
        };
        return categoryRepository.findAll(spec, pageable).map(categoryMapper::toDto);
    }
    public CategoryDto getById(UUID id)  {
        Category category = categoryRepository.findById(id).orElseThrow(()->new NotFoundException("Category not found"));
        return categoryMapper.toDto(category);
    }
    @Transactional(readOnly = true)
    public CategoryDto getCategoryTree(Boolean includeDeleted, Integer depth, UUID rootId) {
        if (depth <= 0||depth>=10) depth = 5;
        Category root;
        if (rootId != null) {
            root = categoryRepository.findById(rootId).orElseThrow(()->new NotFoundException("Root category not found!"));
        }
        else
        {
            Integer finalDepth = depth;
            List<CategoryDto> rootsDto = categoryRepository.findRootCategory(includeDeleted)
                    .stream().map(c->buildCategoryTree(c, finalDepth -1,includeDeleted))
                    .toList();
            return CategoryDto.builder()
                    .id(null)
                    .name("ROOT")
                    .slug("root")
                    .children(rootsDto)
                    .build();
        }
        return buildCategoryTree(root,depth-1,includeDeleted);
    }
    private CategoryDto buildCategoryTree(Category category,int depth,Boolean includeInactive)
    {
        CategoryDto dto = categoryMapper.toDto(category);
        if(depth>0)
        {
            dto.setChildren(
                    categoryRepository.findChildren(category.getId(),includeInactive)
                            .stream()
                            .map(ch->buildCategoryTree(ch,depth-1,includeInactive))
                            .collect(Collectors.toList())
            );
        }
        return dto;
    }
}
