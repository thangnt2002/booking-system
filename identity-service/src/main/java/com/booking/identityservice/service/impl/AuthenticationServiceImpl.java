package com.booking.identityservice.service.impl;

import com.booking.identityservice.service.TokenService;
import com.nimbusds.jose.*;
import com.nimbusds.jose.crypto.MACSigner;
import com.nimbusds.jose.crypto.MACVerifier;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import com.booking.identityservice.dto.request.AuthenticationRequest;
import com.booking.identityservice.dto.request.IntrospectTokenRequest;
import com.booking.identityservice.dto.request.LogoutRequest;
import com.booking.identityservice.dto.request.RefreshRequest;
import com.booking.identityservice.dto.response.AuthenticationResponse;
import com.booking.identityservice.entity.Token;
import com.booking.identityservice.entity.User;
import com.booking.identityservice.exception.ErrorCode;
import com.booking.identityservice.exception.NotFoundException;
import com.booking.identityservice.exception.UnauthorizedException;
import com.booking.identityservice.repository.TokenRepository;
import com.booking.identityservice.repository.UserRepository;
import com.booking.identityservice.service.AuthenticationService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.experimental.NonFinal;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.text.ParseException;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.StringJoiner;
import java.util.UUID;

import static com.booking.identityservice.common.Constant.*;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
public class AuthenticationServiceImpl implements AuthenticationService {

    UserRepository userRepository;
    TokenRepository tokenRepository;
    private final TokenService tokenService;

    @NonFinal
    @Value("${jwt.signerkey}")
    protected String SIGNER_KEY;

    @NonFinal
    @Value("${jwt.valid-duration}")
    protected long VALID_DURATION;

    @NonFinal
    @Value("${jwt.refreshable-duration}")
    protected long REFRESHABLE_DURATION;

    @Override
    public AuthenticationResponse authenticate(AuthenticationRequest request) {
        PasswordEncoder passwordEncoder = new BCryptPasswordEncoder(10);
        User user = userRepository.findByUsername(request.getUsername());
        if (user == null) {
            throw new NotFoundException(404);
        }
        log.info("User {} is logging in...", user.getUsername());
        boolean isAuthenticated = passwordEncoder.matches(request.getPassword(), user.getPassword());

        if (!isAuthenticated) {
            throw new UnauthorizedException(401);
        }
        return generateResponseToken(user);
    }

    @Override
    public boolean introspectToken(IntrospectTokenRequest request)
            throws JOSEException, ParseException {
        try {
            String accessToken = request.getAccessToken();
            String refreshToken = request.getRefreshToken();
            if(accessToken != null){
                verifyToken(accessToken);
            }
            if(accessToken != null){
                verifyToken(refreshToken);
            }
        } catch (UnauthorizedException e) {
            log.error(e.getMessage());
            return false;
        }
        return true;
    }

    private String generateAccessToken(User user) {
        JWSHeader header = new JWSHeader(JWSAlgorithm.HS512);
        Date now = new Date();
        JWTClaimsSet claimsSet = new JWTClaimsSet
                .Builder()
                .subject(user.getId())
                .issuer("thangnt.com")
                .issueTime(now)
                .expirationTime(Date.from(now.toInstant().plus(VALID_DURATION, ChronoUnit.SECONDS)))
                .claim("scope", buildScope(user))
                .claim(TOKEN_TYPE, ACCESS)
                .jwtID(UUID.randomUUID().toString())
                .build();

        Payload payload = new Payload(claimsSet.toJSONObject());
        JWSObject jwsObject = new JWSObject(header, payload);
        try {
            jwsObject.sign(new MACSigner(SIGNER_KEY.getBytes()));
        } catch (JOSEException e) {
            log.error("Cannot crate token");
            throw new RuntimeException(e);
        }
        return jwsObject.serialize();
    }


    private String generateRefreshToken(User user) {
        JWSHeader header = new JWSHeader(JWSAlgorithm.HS512);
        Date now = new Date();
        JWTClaimsSet claimsSet = new JWTClaimsSet
                .Builder()
                .subject(user.getId())
                .issuer("thangnt.com")
                .issueTime(now)
                .expirationTime(Date.from(now.toInstant().plus(REFRESHABLE_DURATION, ChronoUnit.DAYS)))
                .claim(TOKEN_TYPE, REFRESH)
                .jwtID(UUID.randomUUID().toString())
                .build();

        Payload payload = new Payload(claimsSet.toJSONObject());
        JWSObject jwsObject = new JWSObject(header, payload);
        try {
            jwsObject.sign(new MACSigner(SIGNER_KEY.getBytes()));
        } catch (JOSEException e) {
            log.error("Cannot crate token");
            throw new RuntimeException(e);
        }
        return jwsObject.serialize();
    }

    private String buildScope(User user) {
        if (!CollectionUtils.isEmpty(user.getRoles())) {
            StringJoiner stringJoiner = new StringJoiner(" ");
            user.getRoles().forEach(role -> {
                stringJoiner.add("ROLE_" + role.getName());
                if (!CollectionUtils.isEmpty(role.getPermissions())) {
                    role.getPermissions().forEach(p -> {
                        stringJoiner.add(p.getName());
                    });
                }
            });
            return stringJoiner.toString();
        }
        return null;
    }

    private SignedJWT verifyToken(String token) throws JOSEException, ParseException {
        JWSVerifier verifier = new MACVerifier(SIGNER_KEY.getBytes());
        SignedJWT signedJWT = SignedJWT.parse(token);
        Date expiryTime = signedJWT.getJWTClaimsSet().getExpirationTime();
        boolean verified = signedJWT.verify(verifier);
        if (!(verified || expiryTime.before(new Date()))) {
            throw new UnauthorizedException(ErrorCode.UN_AUTHORIZE.getCode());
        }

        String userId = signedJWT.getJWTClaimsSet().getSubject();
        if (!userRepository.existsById(userId)) {
            throw new NotFoundException(ErrorCode.USER_NOT_FOUND.getCode());
        }

        String tokenType = signedJWT.getJWTClaimsSet().getClaimAsString(TOKEN_TYPE);
        if (REFRESH.equals(tokenType)) {
            //TODO check black list in redis
            Token refreshToken = tokenRepository.findByJti(signedJWT.getJWTClaimsSet().getJWTID())
                    .orElseThrow(() -> new UnauthorizedException(ErrorCode.UN_AUTHORIZE.getCode()));
            if (refreshToken.isRevoked()) {
                log.info("REVOKED TOKEN: {} IS USING...", refreshToken.getJti());
                throw new UnauthorizedException(ErrorCode.UN_AUTHORIZE.getCode());
            }
        }


        return signedJWT;
    }

    @Override
    public void logout(LogoutRequest request) throws ParseException, JOSEException {
        invalidateToken(request.getAccessToken());
        invalidateToken(request.getRefreshToken());
    }

    @Override
    public AuthenticationResponse refresh(RefreshRequest request) throws ParseException, JOSEException {
        String accessToken = request.getAccessToken();
        String refreshToken = request.getRefreshToken();
        verifyToken(accessToken);
        verifyToken(refreshToken);
        invalidateToken(accessToken);
        invalidateToken(refreshToken);

        SignedJWT signedJWT = SignedJWT.parse(accessToken);
        String userId = signedJWT.getJWTClaimsSet().getSubject();
        User user = userRepository.findById(userId).orElseThrow(() ->
                new NotFoundException(ErrorCode.USER_NOT_FOUND.getCode()));
        return generateResponseToken(user);
    }

    private void invalidateToken(String tokenString) throws ParseException, JOSEException {
        SignedJWT token = verifyToken(tokenString);
        String tokenType = token.getJWTClaimsSet().getClaimAsString(TOKEN_TYPE);
        if(ACCESS.equals(tokenType)){
            //TODO add access token to redis
        } else {
            String jti = token.getJWTClaimsSet().getJWTID();
            tokenRepository.save(Token.builder()
                    .id(jti)
                    .isRevoked(true)
                    .revokedAt(new Date())
                    .build());
        }
    }

    private AuthenticationResponse generateResponseToken(User user) {
        try {
            String refreshToken = generateRefreshToken(user);
            SignedJWT signedRefreshToken;
            signedRefreshToken = SignedJWT.parse(refreshToken);
            tokenService.createRefreshToken(signedRefreshToken.getJWTClaimsSet().getJWTID(), user);
            return new AuthenticationResponse(true, generateAccessToken(user), refreshToken);
        } catch (ParseException e) {
            log.error("GENERATE TOKEN RESPONSE ERR: {}", e.getMessage());
            throw new RuntimeException(e);
        }
    }

}
