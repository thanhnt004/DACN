package com.example.backend.controller.product;

import com.example.backend.dto.ColorDto;
import com.example.backend.service.product.ColorService;
import jakarta.validation.Valid;
import jdk.dynalink.linker.LinkerServices;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController(value = "/api/v1/color")
@PreAuthorize("hasRole('ADMIN') or hasRole('STAFF')")
@RequiredArgsConstructor
public class ColorController
{
    private final ColorService colorService;
    @GetMapping
    public ResponseEntity<List<ColorDto>> findAll()
    {
        return ResponseEntity.ok(colorService.findAll());
    }
    @GetMapping(value = "/{id}")
    public ResponseEntity<ColorDto> findById(@PathVariable(value = "id")UUID id)
    {
        return ResponseEntity.ok(colorService.findById(id));
    }
    @PostMapping
    public ResponseEntity<ColorDto> create(@RequestBody @Valid ColorDto colorDto)
    {
        return ResponseEntity.ok(colorService.create(colorDto));
    }
    @PutMapping(value = "/{id}")
    public ResponseEntity<ColorDto> update(@PathVariable("id") UUID id,
                                           @RequestBody ColorDto colorDto){
        ColorDto updated = colorService.update(colorDto,id);
        return ResponseEntity.ok(updated);
    }
    @DeleteMapping(value = "/{id}")
    public ResponseEntity<?> delete(@PathVariable("id") UUID id)
    {
        colorService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
