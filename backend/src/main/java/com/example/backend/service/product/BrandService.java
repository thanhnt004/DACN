package com.example.backend.service.product;

import com.example.backend.dto.BrandDto;
import com.example.backend.excepton.NotFoundException;
import com.example.backend.mapper.BrandMapper;
import com.example.backend.model.product.Brand;
import com.example.backend.repository.product.BrandRepository;
import com.example.backend.util.SlugUtil;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE,makeFinal = true)
public class BrandService {
    BrandRepository brandRepository;
    BrandMapper brandMapper;

    private boolean ensureSlugUnique(String slug, UUID excludeId){
        var exists = brandRepository.existsBySlugAndIdNot(slug,excludeId);
        return !exists;
    }

    public BrandDto createBrand(BrandDto brandDto)
    {
        String slug = SlugUtil.uniqueSlug(brandDto.getSlug(),(v)->this.ensureSlugUnique(v,null));
        brandDto.setSlug(slug);
        Brand brand = brandMapper.toEntity(brandDto);
        brandRepository.save(brand);
        return brandMapper.toDto(brand);
    }
    public void delete(UUID brandId)
    {
        Brand brand = brandRepository.findById(brandId).orElseThrow(()-> new NotFoundException("Brand not found!"));
        brandRepository.delete(brand);
    }
    public BrandDto update(BrandDto brandDto,UUID brandId)
    {
        Brand brand = brandRepository.findById(brandId).orElseThrow(()-> new NotFoundException("Brand not found!"));
        if (StringUtils.hasText(brandDto.getSlug()))
            brandDto.setSlug(SlugUtil.uniqueSlug(brandDto.getSlug(),(v)->this.ensureSlugUnique(v,null)));
        brandMapper.updateFromDto(brand,brandDto);
        brandRepository.save(brand);
        return brandMapper.toDto(brand);
    }
    public BrandDto findById(UUID brandId)
    {
        Brand brand = brandRepository.findById(brandId).orElseThrow(()-> new NotFoundException("Brand not found!"));
        return brandMapper.toDto(brand);
    }
    public List<BrandDto> findAll()
    {
        return brandMapper.toDto(brandRepository.findAll());
    }

}
