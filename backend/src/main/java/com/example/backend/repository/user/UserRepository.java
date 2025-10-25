package com.example.backend.repository.user;

import com.example.backend.dto.response.user.UserAuthenDTO;
import com.example.backend.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface UserRepository extends JpaRepository<User, UUID> {
    Optional<User> findByEmail(String email);
    Optional<User>  findByEmailIgnoreCase(String email);

    Optional<User> findByPhone(String identifier);

    @Query("SELECT new com.example.backend.dto.response.user.UserAuthenDTO(u.id," +
            " u.email," +
            " u.passwordHash," +
            " u.role," +
            " CASE WHEN u.status = com.example.backend.model.enumrator.UserStatus.LOCKED THEN true ELSE false END,\n" +
            " CASE WHEN u.status != com.example.backend.model.enumrator.UserStatus.DISABLED THEN true ELSE false END) " +
            "FROM User u WHERE u.email = :identifier")
    Optional<UserAuthenDTO> findAuthByEmail(@Param("identifier") String email);

    @Query("SELECT new com.example.backend.dto.response.user.UserAuthenDTO(u.id," +
            " u.email," +
            " u.passwordHash," +
            " u.role," +
            " CASE WHEN u.status = com.example.backend.model.enumrator.UserStatus.LOCKED THEN true ELSE false END,\n" +
            " CASE WHEN u.status != com.example.backend.model.enumrator.UserStatus.DISABLED THEN true ELSE false END) " +
            "FROM User u WHERE u.phone = :identifier")
    Optional<UserAuthenDTO> findAuthByPhone(@Param("identifier") String phone);
}
