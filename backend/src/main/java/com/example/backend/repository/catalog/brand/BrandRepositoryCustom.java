package com.example.backend.repository.catalog.brand;

import com.example.backend.controller.catalog.category.fillter.BrandFilter;
import com.example.backend.dto.response.catalog.BrandDto;
import com.example.backend.model.product.Brand;
import com.example.backend.model.product.Product;
import io.netty.util.NetUtil;
import jakarta.persistence.*;
import jakarta.persistence.criteria.*;
import jakarta.persistence.metamodel.EntityType;
import jakarta.persistence.metamodel.Metamodel;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class BrandRepositoryCustom {
    private final EntityManager em;
    public Page<BrandDto> listWithFilter(BrandFilter filter, Pageable pageable)
    {
        Metamodel metamodel = em.getMetamodel();
        EntityType<Brand> Brand_ = metamodel.entity(Brand.class);
        CriteriaBuilder cb = em.getCriteriaBuilder();

        CriteriaQuery<Tuple> cq = cb.createTupleQuery();
        Root<Brand> brand = cq.from(Brand_);

        Join<Object, Object> prodJoin = brand.join("products", JoinType.LEFT);

        List<Predicate> predicates = new ArrayList<>();

        predicates.add(cb.isNull(brand.get("deletedAt")));

        if (filter != null) {
            if (filter.getName() != null && !filter.getName().isBlank()) {
                predicates.add(cb.like(cb.lower(brand.get("name")), "%" + filter.getName().toLowerCase() + "%"));
            }
        }
        cq.multiselect(
                brand.get("id").alias("id"),
                brand.get("name").alias("name"),
                brand.get("slug").alias("slug"),
                brand.get("description").alias("description"),
                cb.count(prodJoin).alias("productsCount"),
                brand.get("createdAt").alias("createdAt"),
                brand.get("updatedAt").alias("updatedAt"),
                brand.get("deletedAt").alias("deletedAt")
        );

        cq.where(predicates.toArray(new Predicate[0]));
        cq.groupBy(
                brand.get("id"),
                brand.get("name"),
                brand.get("slug"),
                brand.get("description"),
                brand.get("createdAt"),
                brand.get("updatedAt"),
                brand.get("deletedAt")
        );
        List<Predicate> having = new ArrayList<>();
        if (filter != null) {
            if (filter.getMinProduct() != null) {
                having.add(cb.greaterThanOrEqualTo(cb.count(prodJoin), filter.getMinProduct().longValue()));
            }
            if (filter.getMaxProduct() != null) {
                having.add(cb.lessThanOrEqualTo(cb.count(prodJoin), filter.getMaxProduct().longValue()));
            }
        }
        if (!having.isEmpty()) {
            cq.having(having.toArray(new Predicate[0]));
        }
        String sortBy = filter != null ? filter.getSortBy() : null;
        String dir = filter != null ? filter.getDirection() : null;

        // Validate sort field
        if (sortBy == null || !BrandFilter.ALLOWED_SORT_FIELDS.contains(sortBy)) {
            // fallback default
            sortBy = "name";
        }
        boolean asc = !"desc".equalsIgnoreCase(dir);

        // If sorting by productsCount -> order by COUNT(prodJoin)
        if ("productsCount".equals(sortBy)) {
            Expression<Long> countExpr = cb.count(prodJoin);
            cq.orderBy(asc ? cb.asc(countExpr) : cb.desc(countExpr));
        } else {
            // other fields exist on brand root
            Path<?> sortPath = brand.get(sortBy);
            cq.orderBy(asc ? cb.asc(sortPath) : cb.desc(sortPath));
        }

        TypedQuery<Tuple> query = em.createQuery(cq);
        // pagination
        int pageNumber = pageable.getPageNumber();
        int pageSize = pageable.getPageSize();
        query.setFirstResult(pageNumber * pageSize);
        query.setMaxResults(pageSize);

        List<Tuple> tuples = query.getResultList();

        // map tuples -> BrandDto
        List<BrandDto> content = new ArrayList<>();
        for (Tuple t : tuples) {
            UUID id = t.get("id", UUID.class);
            String name = t.get("name", String.class);
            String slug = t.get("slug", String.class);
            String description = t.get("description", String.class);
            Long cnt = t.get("productsCount", Long.class);
            Integer productsCount = cnt == null ? 0 : cnt.intValue();
            LocalDateTime createdAt = t.get("createdAt", LocalDateTime.class);
            LocalDateTime updatedAt = t.get("updatedAt", LocalDateTime.class);
            BrandDto dto = new BrandDto(id, name, slug, description, productsCount, createdAt, updatedAt);
            // you can set other fields on dto if you add setters or a richer constructor
            content.add(dto);
        }

        // --- Count query for total elements (distinct brands after predicates & having) ---
        CriteriaQuery<Long> countCq = cb.createQuery(Long.class);
        Root<Brand> brandCount = countCq.from(Brand.class);
        Join<Object, Object> prodCountJoin = brandCount.join("products", JoinType.LEFT);

        countCq.select(cb.countDistinct(brandCount.get("id")));
        List<Predicate> countPreds = new ArrayList<>();
        countPreds.add(cb.isNull(brandCount.get("deletedAt")));

        if (filter != null) {
            if (filter.getName() != null && !filter.getName().isBlank()) {
                countPreds.add(cb.like(cb.lower(brandCount.get("name")), "%" + filter.getName().toLowerCase() + "%"));
            }
        }
        countCq.where(countPreds.toArray(new Predicate[0]));
        // For min/max product we need to replicate grouping + having and count distinct by wrapping:
        // JPA Criteria doesn't allow countDistinct with having easily; simplest approach is to create a subquery that selects brand ids after grouping then count them.
        Subquery<UUID> sub = countCq.subquery(UUID.class);
        Root<Brand> subBrand = sub.from(Brand.class);
        Join<Object, Object> subProd = subBrand.join("products", JoinType.LEFT);

        sub.select(subBrand.get("id"));
        List<Predicate> subPreds = new ArrayList<>();
        subPreds.add(cb.isNull(subBrand.get("deletedAt")));
        if (filter != null) {
            if (filter.getName() != null && !filter.getName().isBlank()) {
                subPreds.add(cb.like(cb.lower(subBrand.get("name")), "%" + filter.getName().toLowerCase() + "%"));
            }
        }
        sub.where(subPreds.toArray(new Predicate[0]));
        sub.groupBy(subBrand.get("id"));

        List<Predicate> subHaving = new ArrayList<>();
        if (filter != null) {
            if (filter.getMinProduct() != null) {
                subHaving.add(cb.greaterThanOrEqualTo(cb.count(subProd), filter.getMinProduct().longValue()));
            }
            if (filter.getMaxProduct() != null) {
                subHaving.add(cb.lessThanOrEqualTo(cb.count(subProd), filter.getMaxProduct().longValue()));
            }
        }
        if (!subHaving.isEmpty()) {
            sub.having(subHaving.toArray(new Predicate[0]));
        }

        // final count: count of subquery results
        CriteriaQuery<Long> finalCount = cb.createQuery(Long.class);
        Root<Brand> dummy = finalCount.from(Brand.class); // just to build query
        finalCount.select(cb.count(dummy));
        finalCount.where(cb.in(dummy.get("id")).value(sub));

        Long total = em.createQuery(finalCount).getSingleResult();

        return new PageImpl<>(content, pageable, total == null ? 0L : total);
    }
}


