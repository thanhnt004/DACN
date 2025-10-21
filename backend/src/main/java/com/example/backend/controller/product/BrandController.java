package com.example.backend.controller.product;

import com.example.backend.dto.BrandDto;
import com.example.backend.dto.response.wraper.PageResponse;
import com.example.backend.service.product.BrandService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.data.domain.Pageable;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/brands")
@RequiredArgsConstructor
public class BrandController {
    private final BrandService brandService;

    @GetMapping("/{id}")
    public ResponseEntity<BrandDto> getById(@PathVariable UUID id)
    {
        BrandDto brandDto = brandService.findById(id);
        return ResponseEntity.ok(brandDto);
    }
    @GetMapping
    public ResponseEntity<PageResponse<BrandDto>> list(
            @RequestParam(defaultValue="0") int page,
            @RequestParam(defaultValue="20") int size,
            @RequestParam(required=false) String q,
            @RequestParam(required = false) String sort
    ) {
        Pageable pageable = PageRequest.of(page,size);
        PageResponse<BrandDto> brandDtoPage = brandService.search(q,pageable,sort);
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
    public  ResponseEntity<BrandDto> update(@RequestBody @Valid BrandDto brandDto,@PathVariable("id") UUID id)
    {
        return ResponseEntity.ok(brandService.update(brandDto,id));
    }
}
