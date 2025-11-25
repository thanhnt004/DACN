package com.example.backend.service.user;

import com.example.backend.dto.response.auth.CustomUserDetail;
import com.example.backend.dto.response.user.OauthProviderResponse;
import com.example.backend.dto.response.user.UserAddress;
import com.example.backend.dto.response.user.UserProfileDto;
import com.example.backend.dto.response.wraper.PageResponse;
import com.example.backend.exception.NotFoundException;
import com.example.backend.mapper.AddressMapper;
import com.example.backend.mapper.UserMapper;
import com.example.backend.model.Address;
import com.example.backend.model.User;
import com.example.backend.model.User_;
import com.example.backend.model.enumrator.Role;
import com.example.backend.model.enumrator.UserStatus;
import com.example.backend.repository.user.AddressRepository;
import com.example.backend.repository.auth.OAuthAccountRepository;
import com.example.backend.repository.user.UserRepository;
import com.example.backend.service.auth.RefreshTokenService;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Service
@RequiredArgsConstructor
public class UserManagerService {
    private final UserRepository userRepository;
    private final OAuthAccountRepository oAuthAccountRepository;
    @Value("${app.oauth2.provider}")
    private final Set<String> providers;
    private final AddressRepository addressRepository;
    private final AddressMapper addressMapper;
    private final UserMapper userMapper;
    private final RefreshTokenService refreshTokenService;
    public PageResponse<UserProfileDto> getUserList(Role role,Boolean isActive, Pageable pageable)
    {
        Specification<User> specification = (r,query,criteriaBuilder)->{
            List<Predicate> predicates = new ArrayList<>();
            if (role != null)
                predicates.add(criteriaBuilder.equal(r.get("role"),role));
            if (isActive != null)
                predicates.add(criteriaBuilder.equal(r.get(User_.STATUS),isActive? UserStatus.ACTIVE:UserStatus.DISABLED));
            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        };
        Page<User> page = userRepository.findAll(specification,pageable);
        return new PageResponse<>(page.map(userMapper::toUserProfile));
    }

    public UserProfileDto getUserProfile(CustomUserDetail userDetail) {
        User currentUser = userRepository.findById(userDetail.getId()).orElseThrow(()->new NotFoundException("User not found!"));
        return userMapper.toUserProfile(currentUser);
    }

    public List<UserAddress> getAddressList(CustomUserDetail userDetail) {
        User currentUser = userRepository.findById(userDetail.getId()).orElseThrow(()->new NotFoundException("User not found!"));
        return addressMapper.toDto(addressRepository.getAddressByUser(currentUser));
    }
    public List<OauthProviderResponse> getLinkedProvider(CustomUserDetail userDetail)
    {
        User currentUser = userRepository.findById(userDetail.getId()).orElseThrow(()->new NotFoundException("User not found!"));
        List<OauthProviderResponse> response = new ArrayList<>();
        for (String provider:providers)
            if (oAuthAccountRepository.existsByUserIdAndProvider(currentUser.getId(),provider))
            {
                response.add(new OauthProviderResponse(provider));
            }
        return response;
    }
    @Transactional
    public void addAddress(CustomUserDetail userDetail, UserAddress address) {
        User currentUser = userRepository.findById(userDetail.getId()).orElseThrow(()->new NotFoundException("User not found!"));
        Address newAddress = addressMapper.toEntity(address);
        if (address.isDefaultShipping())
            addressRepository.setDefaultShippingFalse(currentUser.getId());
        currentUser.addAddress(newAddress);
        userRepository.save(currentUser);
    }
    @Transactional
    public void updateProfile(CustomUserDetail userDetail,UserProfileDto dto)
    {
        User currentUser = userRepository.findById(userDetail.getId()).orElseThrow(()->new NotFoundException("User not found!"));
        userMapper.updateFromDto(currentUser,dto);
        userRepository.save(currentUser);
    }
    @Transactional
    public void updateAddress(UserAddress dto, UUID addressId) {
        Address address = addressRepository.findById(addressId).orElseThrow(()->new NotFoundException("Cannot found this address"));
        if (dto.isDefaultShipping())
            addressRepository.setDefaultShippingFalse(address.getUser().getId());
        addressMapper.updateFromDto(dto,address);
        addressRepository.save(address);
    }
    public Optional<Address> getDefaultAddress(User user) {
        return addressRepository.findByUserAndIsDefaultShipping(user,true);
    }
    public void deleteAddress(UUID addressId) {
        addressRepository.deleteById(addressId);
    }

    public void band(UUID userId) {
        User user = userRepository.findById(userId).orElseThrow(()->new NotFoundException("User not found!"));
        user.setStatus(UserStatus.DISABLED);
        userRepository.save(user);
        refreshTokenService.revokeAllByUser(user,"Banned!");
    }

    public void restoreUser(UUID userId) {
        User user = userRepository.findById(userId).orElseThrow(()->new NotFoundException("User not found!"));
        user.setStatus(UserStatus.ACTIVE);
        userRepository.save(user);
    }

}
