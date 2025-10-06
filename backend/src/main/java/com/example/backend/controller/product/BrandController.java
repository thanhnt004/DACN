package com.example.backend.controller.product;

import com.example.backend.dto.BrandDto;
import com.example.backend.dto.response.product.CategoryDto;
import com.example.backend.service.product.BrandService;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

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
    public ResponseEntity<List<BrandDto>> findAll(){
        return ResponseEntity.ok(brandService.findAll());
    }
    @PostMapping
    @PreAuthorize("hasRole('ADMIN') or hasRole('STAFF')")
    private ResponseEntity<BrandDto> create(@RequestBody @Valid BrandDto brandDto)
    {
        return new ResponseEntity<>(brandService.createBrand(brandDto), HttpStatus.CREATED);
    }
    @DeleteMapping(value = "/{id}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('STAFF')")
    private ResponseEntity delete(@PathVariable("id") UUID id)
    {
        brandService.delete(id);
        return ResponseEntity.noContent().build();
    }
    @PutMapping(value = "/{id}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('STAFF')")
    private  ResponseEntity<BrandDto> update(@RequestBody @Valid BrandDto brandDto,@PathVariable("id") UUID id)
    {
        return ResponseEntity.ok(brandService.update(brandDto,id));
    }
}
