package com.booking.identityservice.configuration;

import com.nimbusds.jwt.SignedJWT;
import com.booking.identityservice.service.AuthenticationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.stereotype.Component;

import java.text.ParseException;

@Component
public class CustomJWTDecoder implements JwtDecoder {
    @Value("${jwt.signerkey}")
    private String signerKey;

    @Autowired
    private AuthenticationService authenticationService;

    private NimbusJwtDecoder nimbusJwtDecoder = null;

    @Override
    public Jwt decode(String token) throws JwtException {
//        try {
//            var response = authenticationService.introspectToken(IntrospectTokenRequest
//                    .builder()
//                    .token(token)
//                    .build());
//            if(!response.getData().isValid()){
//                throw new JwtException("Token invalid");
//            }
//        } catch (JOSEException e) {
//            throw new RuntimeException(e);
//        } catch (ParseException e) {
//            throw new RuntimeException(e);
//        }
//
//        if(Objects.isNull(nimbusJwtDecoder)){
//            SecretKeySpec secretKeySpec = new SecretKeySpec(signerKey.getBytes(), "HS512");
//            nimbusJwtDecoder = NimbusJwtDecoder
//                    .withSecretKey(secretKeySpec)
//                    .macAlgorithm(MacAlgorithm.HS512)
//                    .build();
//        }
//        return nimbusJwtDecoder.decode(token);
        try {
            SignedJWT jwt = SignedJWT.parse(token);
            return new Jwt(token,
                    jwt.getJWTClaimsSet().getIssueTime().toInstant(),
                    jwt.getJWTClaimsSet().getExpirationTime().toInstant(),
                    jwt.getHeader().toJSONObject(),
                    jwt.getJWTClaimsSet().toJSONObject()
            );
        } catch (ParseException e) {
            throw new RuntimeException(e);
        }
    }
}