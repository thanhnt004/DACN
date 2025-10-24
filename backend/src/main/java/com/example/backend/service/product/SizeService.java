package com.example.backend.service.product;

import com.example.backend.dto.response.catalog.SizeDto;
import com.example.backend.excepton.BadRequestException;
import com.example.backend.excepton.ConflictException;
import com.example.backend.mapper.SizeMapper;
import com.example.backend.model.product.Size;
import com.example.backend.repository.catalog.product.SizeRepository;
import com.example.backend.service.GenericCrudService;
import org.springframework.stereotype.Service;

@Service
public class SizeService extends GenericCrudService<Size, SizeDto> {
    private final SizeRepository sizeRepository;
    private final SizeMapper sizeMapper;

    public SizeService(SizeRepository sizeRepository, SizeMapper sizeMapper) {
        super(sizeMapper,sizeRepository);
        this.sizeRepository = sizeRepository;
        this.sizeMapper = sizeMapper;
    }

    @Override
    protected void beforeUpdate(Size exist, SizeDto dto) {
        if (sizeRepository.existsByCodeAndIdNot(dto.getCode(), exist.getId()))
            throw new ConflictException("Code is existed!");
    }

    @Override
    protected void beforeCreate(Size newEntity) {
        if (sizeRepository.existsByCodeAndIdNot(newEntity.getCode(),null))
            throw new ConflictException("Code is existed!");
    }

    @Override
    protected void beforeDelete(Size exist) {
        if (sizeRepository.usedByProduct(exist.getId()))
            throw new BadRequestException("Cannot delete size used by product!");
    }
}
