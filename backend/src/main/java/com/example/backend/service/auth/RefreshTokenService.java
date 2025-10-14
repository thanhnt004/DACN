package com.example.backend.service.auth;

import com.example.backend.excepton.AuthenticationException;
import com.example.backend.model.RefreshToken;
import com.example.backend.model.User;
import com.example.backend.repository.RefreshTokenRepository;
import com.example.backend.repository.UserRepository;
import com.example.backend.util.CryptoUtils;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@Slf4j
@RequiredArgsConstructor
public class RefreshTokenService {
    @Value("${REFRESH_TOKEN_EXPIRATION}")
    private int expiration;
    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    public RefreshToken findByRawToken(String token)
    {
        return refreshTokenRepository.findByTokenHash(CryptoUtils.hash(token)).orElseThrow(
                ()->new AuthenticationException(401,"Refresh token is not found!")
        );
    }
    public String createToken(User user)
    {
        String rawToken = CryptoUtils.generateSecureTokenRaw();
        log.info("expiration: {}", expiration);
        LocalDateTime expiry = LocalDateTime.now().plusSeconds(expiration);

        String tokenHash = CryptoUtils.hash(rawToken);

        RefreshToken refreshToken = RefreshToken.builder()
                .expiresAt(expiry)
                .tokenHash(tokenHash)
                .user(user)
                .build();
        refreshTokenRepository.save(refreshToken);

        return rawToken;
    }

    public void verifyToken(RefreshToken token) {
        if (!token.isValid())
           throw new AuthenticationException(401,"Refresh token is invalid!");
    }
    public String rotateToken(RefreshToken refreshToken)
    {
        refreshToken.markRotated();
        refreshTokenRepository.save(refreshToken);
        return createToken(refreshToken.getUser());
    }
    @Transactional
    public int deleteByUserId(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new AuthenticationException(401,"User not found with id: " + userId));
        return refreshTokenRepository.deleteByUser(user);
    }
    public boolean revokeToken(RefreshToken refreshToken,String reason){
        return refreshTokenRepository.revokeByToken(refreshToken.getTokenHash(),reason)>0;
    }
}
