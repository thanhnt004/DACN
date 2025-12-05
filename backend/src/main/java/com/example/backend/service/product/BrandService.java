package com.example.backend.service.product;

import com.example.backend.controller.catalog.category.fillter.BrandFilter;
import com.example.backend.dto.response.catalog.BrandDto;
import com.example.backend.dto.response.wraper.PageResponse;
import com.example.backend.exception.NotFoundException;
import com.example.backend.mapper.BrandMapper;
import com.example.backend.model.product.Brand;
import com.example.backend.repository.catalog.brand.BrandRepository;
import com.example.backend.repository.catalog.brand.BrandRepositoryCustom;
import com.example.backend.util.SlugUtil;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
 import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE,makeFinal = true)
public class BrandService {
    BrandRepository brandRepository;
    BrandMapper brandMapper;
    BrandRepositoryCustom brandRepositoryCustom;
    com.example.backend.config.CacheConfig cacheConfig;

    private boolean slugExist(String slug, UUID excludeId){
        return brandRepository.existsBySlugAndIdNot(slug,excludeId);
    }

    @CacheEvict(value = "#{@cacheConfig.brandCache}", allEntries = true)
    public BrandDto createBrand(BrandDto brandDto)
    {
        String slug = SlugUtil.uniqueSlug(brandDto.getSlug(),(v)->this.slugExist(v,null));
        brandDto.setSlug(slug);
        Brand brand = brandMapper.toEntity(brandDto);
        brandRepository.save(brand);
        return brandMapper.toDto(brand);
    }
    @Transactional
    @CacheEvict(value = "#{@cacheConfig.brandCache}", allEntries = true)
    public void delete(UUID brandId)
    {
        Brand brand = brandRepository.findById(brandId).orElseThrow(()-> new NotFoundException("Brand not found!"));
        brandRepository.delete(brand);
    }
    @CacheEvict(value = "#{@cacheConfig.brandCache}", allEntries = true)
    public BrandDto update(BrandDto brandDto,UUID brandId)
    {
        Brand brand = brandRepository.findById(brandId).orElseThrow(()-> new NotFoundException("Brand not found!"));
        if (StringUtils.hasText(brandDto.getSlug()))
            brandDto.setSlug(SlugUtil.uniqueSlug(brandDto.getSlug(),(v)->this.slugExist(v,brandId)));
        brandMapper.updateFromDto(brand,brandDto);
        brandRepository.save(brand);
        return brandMapper.toDto(brand);
    }
    @Cacheable(
        value = "#{@cacheConfig.brandCache}",
        key = "'detail:' + #brandId",
        unless = "#result == null"
    )
    public BrandDto findById(UUID brandId)
    {
        Brand brand = brandRepository.findById(brandId).orElseThrow(()-> new NotFoundException("Brand not found!"));
        return brandMapper.toDto(brand);
    }

    @Cacheable(
        value = "#{@cacheConfig.brandCache}",
        key = "'list:' + #filter.toString() + ':' + #pageable.pageNumber + ':' + #pageable.pageSize",
        unless = "#result == null"
    )
    public PageResponse<BrandDto> search(BrandFilter filter, Pageable pageable) {
        Page<BrandDto> brandPage =  brandRepositoryCustom.listWithFilter(filter,pageable);
//        Page<BrandDto> brandDtoPage = brandPage.map(brandMapper::toDto);
        return new PageResponse<>(brandPage);
    }

    @Cacheable(
        value = "#{@cacheConfig.brandCache}",
        key = "'slug:' + #slug",
        unless = "#result == null"
    )
    public BrandDto findBySlug(String slug) {
        Brand brand = brandRepository.findBySlug(slug).orElseThrow(()-> new NotFoundException("Brand not found!"));
        return brandMapper.toDto(brand);
    }

    @Cacheable(
        value = "#{@cacheConfig.brandCache}",
        key = "'detail:' + #slugOrId",
        unless = "#result == null"
    )
    public BrandDto findBySlugOrId(String slugOrId) {
        try{
            UUID id = UUID.fromString(slugOrId);
            return findById(id);
        }catch (Exception e)
        {
            return findBySlug(slugOrId);
        }
    }
}
