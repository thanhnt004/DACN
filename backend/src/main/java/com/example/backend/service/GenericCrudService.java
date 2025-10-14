package com.example.backend.service;

import com.example.backend.excepton.NotFoundException;
import com.example.backend.mapper.GenericMapper;
import com.example.backend.repository.product.GenericRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.swing.plaf.SpinnerUI;
import java.util.List;
import java.util.UUID;

@Service
public abstract class GenericCrudService<E,D> {
    private final GenericMapper<E,D> genericMapper;
    private final GenericRepository<E> genericRepository;

    protected GenericCrudService(GenericMapper<E, D> genericMapper, GenericRepository<E> genericRepository) {
        this.genericMapper = genericMapper;
        this.genericRepository = genericRepository;
    }
    @Transactional
    public D create(D dto)
    {
        E newEntity = genericMapper.toEntity(dto);
        beforeCreate(newEntity);
        newEntity = genericRepository.save(newEntity);
        //after
        return genericMapper.toDto(newEntity);
    };
    public List<D> findAll()
    {
        return genericMapper.toDto(genericRepository.findAll());
    }
    public D findById(UUID id)
    {
        E exist = genericRepository.findById(id).orElseThrow(()->new NotFoundException("Not found"));
        return genericMapper.toDto(exist);
    }
    @Transactional
    public D update(D dto, UUID id){
        E exist = genericRepository.findById(id).orElseThrow(()->new NotFoundException("Not found"));
        beforeUpdate(exist,dto);
        genericMapper.updateFromDto(dto,exist);
        return genericMapper.toDto(genericRepository.save(exist));
    }
    public void delete(UUID id)
    {
        E exist = genericRepository.findById(id).orElseThrow(()->new NotFoundException("Not found"));
        beforeDelete(exist);
        genericRepository.delete(exist);
    }

    protected abstract void beforeUpdate(E exist,D dto);
    protected abstract void beforeCreate(E newEntity);
    protected abstract void beforeDelete(E exist);

}
