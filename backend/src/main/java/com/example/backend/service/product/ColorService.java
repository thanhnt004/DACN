package com.example.backend.service.product;

import com.example.backend.dto.ColorDto;
import com.example.backend.excepton.BadRequestException;
import com.example.backend.excepton.ConflictException;
import com.example.backend.mapper.ColorMapper;
import com.example.backend.model.product.Color;
import com.example.backend.repository.product.ColorRepository;
import com.example.backend.service.GenericCrudService;
import org.springframework.stereotype.Service;

@Service
public class ColorService extends GenericCrudService<Color, ColorDto> {
    private final ColorRepository colorRepository;
    public final ColorMapper colorMapper;

    public ColorService(ColorRepository colorRepository, ColorMapper colorMapper) {
        super(colorMapper,colorRepository);
        this.colorRepository = colorRepository;
        this.colorMapper = colorMapper;
    }

    @Override
    protected void beforeUpdate(Color exist,ColorDto dto) {
        if (colorRepository.existsByNameAndIdNot(dto.getName(), exist.getId()))
            throw new ConflictException("Color name existed!");
    }

    @Override
    protected void beforeCreate(Color newEntity) {
        if (colorRepository.existsByName(newEntity.getName()))
            throw new ConflictException("Color name existed!");
    }

    @Override
    protected void beforeDelete(Color exist) {
        if (colorRepository.usedByProduct(exist.getId()))
            throw new BadRequestException("Cannot delete color used by products!");
    }
}
