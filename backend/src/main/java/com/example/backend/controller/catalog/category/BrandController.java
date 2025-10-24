package com.example.backend.controller.catalog.category;

import com.example.backend.controller.catalog.category.fillter.BrandFilter;
import com.example.backend.dto.response.catalog.BrandDto;
import com.example.backend.dto.response.wraper.PageResponse;
import com.example.backend.service.product.BrandService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.data.domain.Pageable;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/brands")
@RequiredArgsConstructor
public class BrandController {
    private final BrandService brandService;

    @GetMapping("/{slugOrId}")
    public ResponseEntity<BrandDto> getBySlugOrId(@PathVariable String slugOrId)
    {
        BrandDto brandDto = brandService.findBySlugOrId(slugOrId);
        return ResponseEntity.ok(brandDto);
    }
    @GetMapping
    public ResponseEntity<PageResponse<BrandDto>> list(
            @ModelAttribute BrandFilter filter,
            @PageableDefault(page = 0, size = 5)
            Pageable pageable) {
        PageResponse<BrandDto> brandDtoPage = brandService.search(filter,pageable);
        return ResponseEntity.ok(brandDtoPage);
    }
    @PostMapping
    @PreAuthorize("hasRole('ADMIN') or hasRole('STAFF')")
    public ResponseEntity<BrandDto> create(@Valid @RequestBody  BrandDto brandDto)
    {
        return new ResponseEntity<>(brandService.createBrand(brandDto), HttpStatus.CREATED);
    }
    @DeleteMapping(value = "/{id}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('STAFF')")
    public ResponseEntity<?> delete(@PathVariable("id") UUID id)
    {
        brandService.delete(id);
        return ResponseEntity.noContent().build();
    }
    @PutMapping(value = "/{id}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('STAFF')")
    public  ResponseEntity<BrandDto> update(@RequestBody @Valid BrandDto brandDto, @PathVariable("id") UUID id)
    {
        return ResponseEntity.ok(brandService.update(brandDto,id));
    }
}
