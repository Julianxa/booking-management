package com.example.utils;


import com.example.exception.user.InvalidTokenException;
import com.google.gson.JsonParser;
import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtParser;
import io.jsonwebtoken.Jwts;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.security.PublicKey;
import java.text.ParseException;

public class CognitoJwtParser {
    public static String getUserSub(String idToken, String userPoolId, String region) {
        Claims claims = parseTokenClaims(idToken, userPoolId, region);
        String userSub = claims.getSubject();
        if (userSub == null) {
            throw new IllegalArgumentException("ID token missing 'sub' claim");
        }
        return userSub;
    }

    public static Claims parseTokenClaims(String jwtToken, String userPoolId, String region) {
        try {
            String jwksUrl = String.format("https://cognito-idp.%s.amazonaws.com/%s/.well-known/jwks.json", region, userPoolId);
            JWKSet jwkSet = JWKSet.load(new URL(jwksUrl));
            RSAKey rsaKey = (RSAKey) jwkSet.getKeyByKeyId(getKidFromToken(jwtToken));
            PublicKey publicKey = rsaKey.toPublicKey();

            JwtParser jwtParser = Jwts.parser()
                    .setSigningKey(publicKey)
                    .build();
            return jwtParser.parseClaimsJws(jwtToken).getBody();
        } catch(ParseException | JOSEException | IOException e) {
            throw new InvalidTokenException("Invalid JWKS URL is provided");
        }
    }

    private static String getKidFromToken(String idToken) {
        String[] parts = idToken.split("\\.");
        String headerJson = new String(java.util.Base64.getDecoder().decode(parts[0]));
        return new JsonParser().parse(headerJson).getAsJsonObject().get("kid").getAsString();
    }
}
