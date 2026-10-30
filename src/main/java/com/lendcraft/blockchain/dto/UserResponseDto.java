package com.lendcraft.blockchain.dto;

import java.math.BigDecimal;

public class UserResponseDto {
    private String passportNumber;
    private String username;
    private int trustRating;
    private BigDecimal fiatBalance;
    private String publicKey;
    private String email;
    private String bio;

    public UserResponseDto(String passportNumber, String username, int trustRating,
                           BigDecimal fiatBalance, String publicKey, String email, String bio) {
        this.passportNumber = passportNumber;
        this.username = username;
        this.trustRating = trustRating;
        this.fiatBalance = fiatBalance;
        this.publicKey = publicKey;
        this.email = email != null ? email : "";
        this.bio = bio != null ? bio : "";
    }

    public String getPassportNumber() { return passportNumber; }
    public String getUsername() { return username; }
    public int getTrustRating() { return trustRating; }
    public BigDecimal getFiatBalance() { return fiatBalance; }
    public String getPublicKey() { return publicKey; }
    public String getEmail() { return email; }
    public String getBio() { return bio; }
}
