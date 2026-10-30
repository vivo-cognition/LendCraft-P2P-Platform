package com.lendcraft.blockchain.dto;

public class UserRegisterDto {
    private String username;
    private String password;
    private String passportNumber;

    public UserRegisterDto() {}

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
    public String getPassportNumber() { return passportNumber; }
    public void setPassportNumber(String passportNumber) { this.passportNumber = passportNumber; }

}
