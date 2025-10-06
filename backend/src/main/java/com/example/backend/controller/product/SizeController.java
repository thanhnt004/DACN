package com.example.backend.controller.product;


import com.example.backend.dto.ColorDto;
import com.example.backend.dto.SizeDto;
import com.example.backend.service.product.SizeService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/sizes")
@PreAuthorize("hasRole('ADMIN') or hasRole('STAFF')")
@RequiredArgsConstructor
public class SizeController
{
    private final SizeService sizeService;

    @GetMapping
    public ResponseEntity<List<SizeDto>> findAll()
    {
        return ResponseEntity.ok(sizeService.findAll());
    }
    @GetMapping(value = "/{id}")
    public ResponseEntity<SizeDto> findById(@PathVariable(value = "id") UUID id)
    {
        return ResponseEntity.ok(sizeService.findById(id));
    }
    @PostMapping
    public ResponseEntity<SizeDto> create(@RequestBody @Valid SizeDto sizeDto)
    {
        return ResponseEntity.ok(sizeService.create(sizeDto));
    }
    @PutMapping(value = "/{id}")
    public ResponseEntity<SizeDto> update(@PathVariable("id") UUID id,
                                           @RequestBody SizeDto sizeDto){
        SizeDto updated = sizeService.update(sizeDto,id);
        return ResponseEntity.ok(updated);
    }
    @DeleteMapping(value = "/{id}")
    public ResponseEntity<?> delete(@PathVariable("id") UUID id)
    {
        sizeService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
