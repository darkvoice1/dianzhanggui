package com.darkvoice1.dianzhanggui.auth.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.darkvoice1.dianzhanggui.auth.config.AuthProperties;
import com.darkvoice1.dianzhanggui.auth.mapper.RefreshTokenMapper;
import com.darkvoice1.dianzhanggui.auth.mapper.UserAccountMapper;
import com.darkvoice1.dianzhanggui.auth.model.LoginRequest;
import com.darkvoice1.dianzhanggui.auth.model.RefreshToken;
import com.darkvoice1.dianzhanggui.auth.model.RefreshTokenRequest;
import com.darkvoice1.dianzhanggui.auth.model.RegisterRequest;
import com.darkvoice1.dianzhanggui.auth.model.TokenResponse;
import com.darkvoice1.dianzhanggui.auth.model.UserAccount;
import com.darkvoice1.dianzhanggui.common.ErrorCode;
import com.darkvoice1.dianzhanggui.infrastructure.exception.BusinessException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.HexFormat;
import java.util.Locale;
import java.util.UUID;

/** 提供用户注册、登录、令牌刷新和退出能力。 */
@Service
public class AuthService {

    private final UserAccountMapper userAccountMapper;
    private final RefreshTokenMapper refreshTokenMapper;
    private final PasswordEncoder passwordEncoder;
    private final JwtEncoder jwtEncoder;
    private final AuthProperties authProperties;

    /** 创建认证服务并注入账号、令牌和安全组件。 */
    public AuthService(UserAccountMapper userAccountMapper, RefreshTokenMapper refreshTokenMapper,
                       PasswordEncoder passwordEncoder, JwtEncoder jwtEncoder, AuthProperties authProperties) {
        this.userAccountMapper = userAccountMapper;
        this.refreshTokenMapper = refreshTokenMapper;
        this.passwordEncoder = passwordEncoder;
        this.jwtEncoder = jwtEncoder;
        this.authProperties = authProperties;
    }

    /** 注册新用户并创建首次登录会话。 */
    @Transactional
    public TokenResponse register(RegisterRequest request) {
        String email = normalizeEmail(request.email());
        if (findUserByEmail(email) != null) {
            throw new BusinessException(ErrorCode.USER_ALREADY_EXISTS);
        }

        UserAccount user = new UserAccount();
        user.setEmail(email);
        user.setPasswordHash(passwordEncoder.encode(request.password()));
        userAccountMapper.insert(user);
        return createTokenResponse(user);
    }

    /** 校验登录密码并创建新的会话。 */
    @Transactional
    public TokenResponse login(LoginRequest request) {
        UserAccount user = findUserByEmail(normalizeEmail(request.email()));
        if (user == null || !passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw new BusinessException(ErrorCode.LOGIN_FAILED);
        }
        return createTokenResponse(user);
    }

    /** 使用有效刷新令牌换取新的一组令牌，并注销旧令牌。 */
    @Transactional
    public TokenResponse refresh(RefreshTokenRequest request) {
        RefreshToken storedToken = refreshTokenMapper.selectOne(new LambdaQueryWrapper<RefreshToken>()
                .eq(RefreshToken::getTokenHash, hashToken(request.refreshToken())));
        if (storedToken == null || storedToken.getRevokedAt() != null
                || !storedToken.getExpiresAt().isAfter(LocalDateTime.now(ZoneOffset.UTC))) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }

        UserAccount user = userAccountMapper.selectById(storedToken.getUserId());
        if (user == null) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }

        // 刷新成功后立即注销旧刷新令牌，避免同一个令牌重复使用。
        storedToken.setRevokedAt(LocalDateTime.now(ZoneOffset.UTC));
        refreshTokenMapper.updateById(storedToken);
        return createTokenResponse(user);
    }

    /** 注销指定刷新令牌，使其不能再获取新的访问令牌。 */
    @Transactional
    public void logout(RefreshTokenRequest request) {
        RefreshToken storedToken = refreshTokenMapper.selectOne(new LambdaQueryWrapper<RefreshToken>()
                .eq(RefreshToken::getTokenHash, hashToken(request.refreshToken())));
        if (storedToken != null && storedToken.getRevokedAt() == null) {
            storedToken.setRevokedAt(LocalDateTime.now(ZoneOffset.UTC));
            refreshTokenMapper.updateById(storedToken);
        }
    }

    /** 创建短期访问令牌和长期刷新令牌。 */
    private TokenResponse createTokenResponse(UserAccount user) {
        Instant issuedAt = Instant.now();
        Instant expiresAt = issuedAt.plus(authProperties.accessTokenExpiration());
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer(authProperties.issuer())
                .issuedAt(issuedAt)
                .expiresAt(expiresAt)
                .subject(user.getId().toString())
                .claim("email", user.getEmail())
                .build();
        String accessToken = jwtEncoder.encode(JwtEncoderParameters.from(
                JwsHeader.with(MacAlgorithm.HS256).build(), claims)).getTokenValue();

        String refreshToken = UUID.randomUUID().toString();
        RefreshToken refreshTokenRecord = new RefreshToken();
        refreshTokenRecord.setUserId(user.getId());
        refreshTokenRecord.setTokenHash(hashToken(refreshToken));
        refreshTokenRecord.setExpiresAt(LocalDateTime.ofInstant(
                issuedAt.plus(authProperties.refreshTokenExpiration()), ZoneOffset.UTC));
        refreshTokenMapper.insert(refreshTokenRecord);
        return new TokenResponse(accessToken, refreshToken, "Bearer", authProperties.accessTokenExpiration().toSeconds());
    }

    /** 根据邮箱查询用户。 */
    private UserAccount findUserByEmail(String email) {
        return userAccountMapper.selectOne(new LambdaQueryWrapper<UserAccount>()
                .eq(UserAccount::getEmail, email));
    }

    /** 统一邮箱大小写，避免同一邮箱重复注册。 */
    private String normalizeEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }

    /** 计算刷新令牌哈希，避免在数据库中保存明文令牌。 */
    private String hashToken(String token) {
        try {
            byte[] hash = MessageDigest.getInstance("SHA-256").digest(token.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("当前 JDK 不支持 SHA-256", exception);
        }
    }
}
