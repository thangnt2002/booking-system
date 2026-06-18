package com.booking.identityservice.service.impl;

import com.nimbusds.jose.*;
import com.nimbusds.jose.crypto.MACSigner;
import com.nimbusds.jose.crypto.MACVerifier;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import com.booking.identityservice.dto.ApiResponse;
import com.booking.identityservice.dto.request.AuthenticationRequest;
import com.booking.identityservice.dto.request.IntrospectTokenRequest;
import com.booking.identityservice.dto.request.LogoutRequest;
import com.booking.identityservice.dto.request.RefreshRequest;
import com.booking.identityservice.dto.response.AuthenticationResponse;
import com.booking.identityservice.entity.InvalidateJWT;
import com.booking.identityservice.entity.User;
import com.booking.identityservice.exception.ErrorCode;
import com.booking.identityservice.exception.NotFoundException;
import com.booking.identityservice.exception.UnauthorizedException;
import com.booking.identityservice.repository.InvalidateJWTRepository;
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

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
public class AuthenticationServiceImpl implements AuthenticationService {

    UserRepository userRepository;
    InvalidateJWTRepository tokenRepository;

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
    public String authenticate(AuthenticationRequest request) {
        PasswordEncoder passwordEncoder = new BCryptPasswordEncoder(10);
        User user = userRepository.findByUsername(request.getUsername());
        if(user == null){
            throw new NotFoundException(404);
        }
        boolean isAuthenticated = passwordEncoder.matches(request.getPassword(), user.getPassword());

        if(!isAuthenticated) {
            throw new UnauthorizedException(401);
        }
        return generateToken(user);
    }

    @Override
    public boolean introspectToken(IntrospectTokenRequest request)
     throws JOSEException, ParseException
    {
        String token = request.getToken();
        try {
            verifyToken(token, false);
        }catch (UnauthorizedException e){
            log.error(e.getMessage());
            return false;
        }
        return true;
    }

    private String generateToken(User user) {
        JWSHeader header = new JWSHeader(JWSAlgorithm.HS512);
        Date now = new Date();
        JWTClaimsSet claimsSet = new JWTClaimsSet
                .Builder()
                .subject(user.getId())
                .issuer("thangnt.com")
                .issueTime(now)
                .expirationTime(Date.from(now.toInstant().plus(VALID_DURATION, ChronoUnit.SECONDS)))
                .claim("scope", buildScope(user))
                .jwtID(UUID.randomUUID().toString())
                .build();

        Payload payload = new Payload(claimsSet.toJSONObject());
        JWSObject jwsObject = new JWSObject(header, payload);
        try {
            jwsObject.sign(new MACSigner(SIGNER_KEY.getBytes()));
        }catch (JOSEException e){
            log.error("Cannot crate token");
            throw new RuntimeException(e);
        }
        return jwsObject.serialize();
    }

    private String buildScope(User user){
        if(!CollectionUtils.isEmpty(user.getRoles())){
            StringJoiner stringJoiner = new StringJoiner(" ");
            user.getRoles().forEach(role ->{
                    stringJoiner.add("ROLE_"+role.getName());
                    if(!CollectionUtils.isEmpty(role.getPermissions())){
                        role.getPermissions().forEach(p -> {
                            stringJoiner.add(p.getName());
                        });
                    }
            });
            return stringJoiner.toString();
        }
        return null;
    }

    private SignedJWT verifyToken(String token, boolean isRefresh) throws JOSEException, ParseException {
        JWSVerifier verifier = new MACVerifier(SIGNER_KEY.getBytes());
        SignedJWT signedJWT = SignedJWT.parse(token);
        Date expiryTime =  (isRefresh)
                            ? new Date (signedJWT.getJWTClaimsSet().getIssueTime().toInstant()
                            .plus(REFRESHABLE_DURATION, ChronoUnit.SECONDS).toEpochMilli())
                            : signedJWT.getJWTClaimsSet().getExpirationTime();
        boolean verified = signedJWT.verify(verifier);
        if(!(verified && expiryTime.after(new Date()))){
            throw new UnauthorizedException(ErrorCode.UN_AUTHORIZE.getCode());
        }

        if(tokenRepository.existsById(signedJWT.getJWTClaimsSet().getJWTID())){
            throw new UnauthorizedException(ErrorCode.UN_AUTHORIZE.getCode());
        }
        return signedJWT;
    }

    @Override
    public void logout(LogoutRequest request) throws ParseException, JOSEException {
        invalidateToken(request.getToken());
    }

    @Override
    public AuthenticationResponse refresh(RefreshRequest request) throws ParseException, JOSEException {
        String token =request.getToken();
        invalidateToken(token);
        SignedJWT signedJWT = verifyToken(token, true);
        String userId = signedJWT.getJWTClaimsSet().getSubject();
        var user = userRepository.findById(userId).orElseThrow(
                () -> new NotFoundException(ErrorCode.USER_NOT_FOUND.getCode()));
        var newToken = generateToken(user);

       return AuthenticationResponse
                        .builder()
                        .isAuthenticated(true)
                        .token(newToken)
                        .build();

    }

    private void invalidateToken(String tokenString) throws ParseException, JOSEException {
        SignedJWT token = verifyToken(tokenString, true);
        String jti = token.getJWTClaimsSet().getJWTID();
        Date expiryTime = token.getJWTClaimsSet().getExpirationTime();
        tokenRepository.save(InvalidateJWT.builder()
                .id(jti)
                .expiryTime(expiryTime)
                .build());
    }

}
