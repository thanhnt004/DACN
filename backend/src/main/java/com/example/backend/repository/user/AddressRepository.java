package com.example.backend.repository.user;

import com.example.backend.model.Address;
import com.example.backend.model.User;
import com.example.backend.repository.GenericRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface AddressRepository extends GenericRepository<Address> {
    List<Address> getAddressByUser(User user);

    @Modifying
    @Query("UPDATE Address add SET add.isDefaultShipping = false where add.isDefaultShipping = true")
    void setDefaultShippingFalse();
}
