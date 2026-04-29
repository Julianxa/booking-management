package com.example.utils;


import com.google.gson.JsonParser;
import com.nimbusds.jose.JOSEException;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtParser;
import io.jsonwebtoken.Jwts;

import java.io.IOException;
import java.security.PublicKey;
import java.net.URL;
import java.text.ParseException;

import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;

public class CognitoJwtParser {
    public static String getUserSub(String idToken, String userPoolId, String region) throws IOException, ParseException, JOSEException {
        String jwksUrl = String.format("https://cognito-idp.%s.amazonaws.com/%s/.well-known/jwks.json", region, userPoolId);
        JWKSet jwkSet = JWKSet.load(new URL(jwksUrl));
        RSAKey rsaKey = (RSAKey) jwkSet.getKeyByKeyId(getKidFromToken(idToken));
        PublicKey publicKey = rsaKey.toPublicKey();

        JwtParser jwtParser = Jwts.parser()
                .setSigningKey(publicKey)
                .build();
        Claims claims = jwtParser.parseClaimsJws(idToken).getBody();
        String userSub = claims.getSubject();
        if (userSub == null) {
            throw new IllegalArgumentException("ID token missing 'sub' claim");
        }
        return userSub;
    }

    private static String getKidFromToken(String idToken) {
        String[] parts = idToken.split("\\.");
        String headerJson = new String(java.util.Base64.getDecoder().decode(parts[0]));
        return new JsonParser().parse(headerJson).getAsJsonObject().get("kid").getAsString();
    }
}
