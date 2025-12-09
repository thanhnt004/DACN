package com.example.backend.controller.catalog.category;

import com.example.backend.dto.response.catalog.BrandDto;
import com.example.backend.service.product.BrandService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class BrandControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private BrandService brandService;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @WithMockUser(roles = "ADMIN")
    void createBrand() throws Exception {
        BrandDto brandDto = new BrandDto();
        brandDto.setName("Test Brand");
        brandDto.setSlug("test-brand");

        when(brandService.createBrand(any(BrandDto.class))).thenReturn(brandDto);

        mockMvc.perform(post("/api/v1/brands")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(brandDto)))
                .andExpect(status().isCreated());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void updateBrand() throws Exception {
        BrandDto brandDto = new BrandDto();
        brandDto.setName("Updated Brand");
        brandDto.setSlug("updated-brand");
        UUID brandId = UUID.randomUUID();

        when(brandService.update(any(BrandDto.class), any(UUID.class))).thenReturn(brandDto);

        mockMvc.perform(put("/api/v1/brands/" + brandId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(brandDto)))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void deleteBrand() throws Exception {
        UUID brandId = UUID.randomUUID();
        mockMvc.perform(delete("/api/v1/brands/" + brandId))
                .andExpect(status().isNoContent());
    }

    @Test
    void getBrand() throws Exception {
        UUID brandId = UUID.randomUUID();
        BrandDto brandDto = new BrandDto();
        brandDto.setId(brandId);
        brandDto.setName("Test Brand");
        brandDto.setSlug("test-brand");

        when(brandService.findBySlugOrId(brandId.toString())).thenReturn(brandDto);

        mockMvc.perform(get("/api/v1/brands/" + brandId))
                .andExpect(status().isOk());
    }
}
