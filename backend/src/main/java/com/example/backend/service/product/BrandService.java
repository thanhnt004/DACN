package com.example.backend.service.product;

import com.example.backend.dto.BrandDto;
import com.example.backend.dto.response.wraper.PageResponse;
import com.example.backend.excepton.BadRequestException;
import com.example.backend.excepton.NotFoundException;
import com.example.backend.mapper.BrandMapper;
import com.example.backend.model.product.Brand;
import com.example.backend.repository.catalog.brand.BrandRepository;
import com.example.backend.util.SlugUtil;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE,makeFinal = true)
public class BrandService {
    BrandRepository brandRepository;
    BrandMapper brandMapper;

    private static final List<String> ALLOWED_SORT_FIELDS = List.of("name", "createdAt", "updatedAt", "productsCount");
    private boolean slugExist(String slug, UUID excludeId){
        return brandRepository.existsBySlugAndIdNot(slug,excludeId);
    }

    public BrandDto createBrand(BrandDto brandDto)
    {
        String slug = SlugUtil.uniqueSlug(brandDto.getSlug(),(v)->this.slugExist(v,null));
        brandDto.setSlug(slug);
        Brand brand = brandMapper.toEntity(brandDto);
        brandRepository.save(brand);
        return brandMapper.toDto(brand);
    }
    @Transactional
    public void delete(UUID brandId)
    {
        Brand brand = brandRepository.findById(brandId).orElseThrow(()-> new NotFoundException("Brand not found!"));
        brandRepository.delete(brand);
    }
    public BrandDto update(BrandDto brandDto,UUID brandId)
    {
        Brand brand = brandRepository.findById(brandId).orElseThrow(()-> new NotFoundException("Brand not found!"));
        if (StringUtils.hasText(brandDto.getSlug()))
            brandDto.setSlug(SlugUtil.uniqueSlug(brandDto.getSlug(),(v)->this.slugExist(v,brandId)));
        brandMapper.updateFromDto(brand,brandDto);
        brandRepository.save(brand);
        return brandMapper.toDto(brand);
    }
    public BrandDto findById(UUID brandId)
    {
        Brand brand = brandRepository.findById(brandId).orElseThrow(()-> new NotFoundException("Brand not found!"));
        return brandMapper.toDto(brand);
    }

    public PageResponse<BrandDto> search(String q, Pageable pageable,String sort) {
        Specification<Brand> spec = buildSpecification(q, sort);

        Page<Brand> brandPage =  brandRepository.findAll(spec, pageable);
        Page<BrandDto> brandDtoPage = brandPage.map(brandMapper::toDto);
        return new PageResponse<>(brandDtoPage);
    }

    private Specification<Brand> buildSpecification(String q, String sort) {
        return (root,query,cb)->{
            var predicates = cb.conjunction();
            if(StringUtils.hasText(q))
            {
                String pattern = "%" + q.toLowerCase() + "%";
                predicates = cb.and(predicates,cb.or(
                        cb.like(cb.lower(root.get("name")),pattern),
                        cb.like(cb.lower(root.get("description")),pattern)
                ));
            }

            if (StringUtils.hasText(sort))
            {
                String[] sortParams = sort.split(",");
                if(sortParams.length == 2)
                {
                    String sortField = sortParams[0];
                    String sortDirection = sortParams[1];
                    if (!ALLOWED_SORT_FIELDS.contains(sortField))
                        throw new BadRequestException("Invalid sort field: " + sortField);
                    if(sortDirection.equalsIgnoreCase("asc"))
                    {
                        query.orderBy(cb.asc(root.get(sortField)));
                    }
                    else if(sortDirection.equalsIgnoreCase("desc"))
                    {
                        query.orderBy(cb.desc(root.get(sortField)));
                    }
                }
            }
            return predicates;
        };
    }
}
