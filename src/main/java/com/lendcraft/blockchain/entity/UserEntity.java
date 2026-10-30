package com.lendcraft.blockchain.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;

@Entity
@Table(name = "users")
public class UserEntity {
    @Id
    private String passportNumber;

    private String username;
    private String password;
    private String email;

    @Column(precision = 18, scale = 8)
    private BigDecimal fiatBalance;

    private int trustRating;

    @Column(columnDefinition = "TEXT")
    private String publicKey;

    @Column(length = 500)
    private String bio;

    public UserEntity() {}

    public UserEntity(String passportNumber, String username, String password, BigDecimal fiatBalance, int trustRating, String publicKey) {
        this.passportNumber = passportNumber;
        this.username = username;
        this.password = password;
        this.fiatBalance = fiatBalance;
        this.trustRating = trustRating;
        this.publicKey = publicKey;
    }
    public String getPassportNumber() { return passportNumber; }
    public void setPassportNumber(String passportNumber) { this.passportNumber = passportNumber; }
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
    public BigDecimal getFiatBalance() { return fiatBalance; }
    public void setFiatBalance(BigDecimal fiatBalance) { this.fiatBalance = fiatBalance; }
    public int getTrustRating() { return trustRating; }
    public void setTrustRating(int trustRating) { this.trustRating = trustRating; }
    public String getPublicKey() { return publicKey; }
    public void setPublicKey(String publicKey) { this.publicKey = publicKey; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getBio() { return bio; }
    public void setBio(String bio) { this.bio = bio; }

}
