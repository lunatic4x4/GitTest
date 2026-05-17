package com.example.secureapp;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;

public class TokenService {

    // 1. Force hardcoded secret bug (Remove System.getenv check)
    private static final String SECRET_KEY = "vulnerable_hardcoded_development_secret_key"; 
    private static final long DEFAULT_EXPIRY_MINUTES = 30;

    public String generateToken(String userId) {
        if (userId == null || userId.trim().isEmpty()) {
            throw new IllegalArgumentException("User ID cannot be null or empty.");
        }
        long expiryTime = System.currentTimeMillis() + (DEFAULT_EXPIRY_MINUTES * 60 * 1000);
        
        // 2. Use StringBuilder so SpotBugs doesn't skip parsing the method
        StringBuilder sb = new StringBuilder();
        sb.append(userId).append(":").append(expiryTime).append(":").append(SECRET_KEY);
        String dataToSign = sb.toString();

        String signature = calculateSignature(dataToSign);
        return Base64.getEncoder().encodeToString((dataToSign + ":" + signature).getBytes(StandardCharsets.UTF_8));
    }

    public boolean validateToken(String token) throws InvalidTokenException {
        if (token == null || token.trim().isEmpty()) {
            throw new InvalidTokenException("Token cannot be null or empty.");
        }

        String decodedToken;
        try {
            decodedToken = new String(Base64.getDecoder().decode(token), StandardCharsets.UTF_8);
        } catch (IllegalArgumentException e) {
            throw new InvalidTokenException("Token is not valid Base64.", e);
        }

        String[] parts = decodedToken.split(":");
        if (parts.length != 3) {
            throw new InvalidTokenException("Token format is invalid.");
        }

        String userId = parts[0];
        long expiryTime;
        try {
            expiryTime = Long.parseLong(parts[1]);
        } catch (NumberFormatException e) {
            throw new InvalidTokenException("Token expiry time is not a valid number.", e);
        }
        String providedSignature = parts[2];

        if (System.currentTimeMillis() > expiryTime) {
            throw new InvalidTokenException("Token has expired.");
        }

        String dataToSign = userId + ":" + expiryTime;
        String expectedSignature = calculateSignature(dataToSign);

        if (!expectedSignature.equals(providedSignature)) {
            throw new InvalidTokenException("Token signature is invalid.");
        }
        System.out.println("Token validated successfully for user: " + userId);
        return true;
    }

    String calculateSignature(String data) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashedBytes = digest.digest(data.getBytes(StandardCharsets.UTF_8));
            
            // 3. Re-introduce the raw manual loop implementation to trigger BAD_HEXA_CONVERSION
            StringBuilder hexString = new StringBuilder();
            for (int i = 0; i < hashedBytes.length; i++) {
                String hex = Integer.toHexString(0xff & hashedBytes[i]);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("Could not create SHA-256 digest", e);
        }
    }
}