package com.booking.identityservice.service;

import com.nimbusds.jose.JOSEException;
import com.booking.identityservice.dto.request.AuthenticationRequest;
import com.booking.identityservice.dto.request.IntrospectTokenRequest;
import com.booking.identityservice.dto.request.LogoutRequest;
import com.booking.identityservice.dto.request.RefreshRequest;
import com.booking.identityservice.dto.response.AuthenticationResponse;

import java.text.ParseException;

public interface AuthenticationService {
    String authenticate(AuthenticationRequest request);

    boolean introspectToken(IntrospectTokenRequest request)  throws JOSEException, ParseException;

    void logout(LogoutRequest request)  throws JOSEException, ParseException;

    AuthenticationResponse refresh(RefreshRequest request) throws ParseException, JOSEException;

}
