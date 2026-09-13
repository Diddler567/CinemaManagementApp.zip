package com.example.backend.repository;

import com.example.backend.entities.AuthenticationToken;
import com.example.backend.entities.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface AuthenticationTokenRepository extends JpaRepository<AuthenticationToken, Long> {

    Optional<AuthenticationToken> findByTokenValue(String tokenValue);

    Optional<AuthenticationToken> findByTokenValueAndIsValidTrue(String tokenValue);

    @Modifying
    @Query("update AuthenticationToken t set t.isValid = false where t.user = :user and t.isValid = true")
    int invalidateAllForUser(@Param("user") User user);

    @Modifying
    @Query("update AuthenticationToken t set t.isValid = false where t.tokenValue = :tokenValue and t.isValid = true")
    int invalidateByTokenValue(@Param("tokenValue") String tokenValue);

    //ΓΙΑ HARD DELETE ΧΡΉΣΤΗ
    void deleteAllByUser(User user);
}


