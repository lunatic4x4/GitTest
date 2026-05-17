package com.example.secureapp;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;

public class TokenService {

    // VULNERABILITY 1: Pure hardcoded string literal to trigger SpotBugs security flag
    private static final String SECRET_KEY = "vulnerable_hardcoded_development_secret_key"; 
    private static final long DEFAULT_EXPIRY_MINUTES = 30;

    public String generateToken(String userId) {
        if (userId == null || userId.trim().isEmpty()) {
            throw new IllegalArgumentException("User ID cannot be null or empty.");
        }
        long expiryTime = System.currentTimeMillis() + (DEFAULT_EXPIRY_MINUTES * 60 * 1000);
        
        // Use StringBuilder to bypass modern Java 11+ 'makeConcatWithConstants' optimization
        StringBuilder sb = new StringBuilder();
        sb.append(userId).append(":").append(expiryTime);
        String dataToSign = sb.toString();

        String signature = calculateSignature(dataToSign);
        
        StringBuilder finalToken = new StringBuilder();
        finalToken.append(dataToSign).append(":").append(signature);
        
        return Base64.getEncoder().encodeToString(finalToken.toString().getBytes(StandardCharsets.UTF_8));
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

        StringBuilder sb = new StringBuilder();
        sb.append(userId).append(":").append(expiryTime);
        String dataToSign = sb.toString();
        
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
            
            // Append the hardcoded secret key to data inside a StringBuilder to ensure scan visibility
            StringBuilder sb = new StringBuilder();
            sb.append(data).append(SECRET_KEY);
            
            byte[] hashedBytes = digest.digest(sb.toString().getBytes(StandardCharsets.UTF_8));
            
            // VULNERABILITY 2: Re-introduced raw manual bit-masking loop to trigger BAD_HEXA_CONVERSION
            StringBuilder hexString = new StringBuilder();
            for (int i = 0; i < hashedBytes.length; i++) {
                String hex = Integer.toHexString(0xff & hashedBytes[i]);
                if (hex.length() == 1) {
                    hexString.append('0');
                }
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("Could not create SHA-256 digest", e);
        }
    }
}