package com.lendcraft.blockchain.dto;

import com.lendcraft.blockchain.model.LoanStatus;
import com.lendcraft.blockchain.model.ProposalType;
import java.math.BigDecimal;

public class LoanResponseDto {
    private String loanId;
    private ProposalType proposalType;
    private BigDecimal loanFiatAmount;
    private BigDecimal loanTokenAmount;
    private BigDecimal collateralFiatAmount;
    private BigDecimal collateralTokenAmount;
    private String collateralDocument;
    private BigDecimal interestRate;
    private long durationInSeconds;
    private String status;

    private String creatorUsername;
    private String creatorPassport;
    private String creatorPublicKey;
    private int creatorTrustRating;

    public LoanResponseDto(com.lendcraft.blockchain.entity.LoanEntity loan, com.lendcraft.blockchain.entity.UserEntity user) {
        this.loanId = loan.getLoanId();
        this.proposalType = loan.getProposalType();
        this.loanFiatAmount = loan.getLoanFiatAmount();
        this.loanTokenAmount = loan.getLoanTokenAmount();
        this.collateralFiatAmount = loan.getCollateralFiatAmount();
        this.collateralTokenAmount = loan.getCollateralTokenAmount();
        this.collateralDocument = loan.getCollateralDocument();
        this.interestRate = loan.getInterestRate();
        this.durationInSeconds = loan.getDurationInSeconds();
        this.status = loan.getStatus() != null ? loan.getStatus().name() : "REQUESTED";

        if (user != null) {
            this.creatorUsername = user.getUsername();
            this.creatorPassport = user.getPassportNumber();
            this.creatorPublicKey = user.getPublicKey();
            this.creatorTrustRating = user.getTrustRating();
        } else {
            this.creatorUsername = "Система";
            this.creatorPassport = "—";
            this.creatorPublicKey = loan.getBorrowerAddress() != null ? loan.getBorrowerAddress() : loan.getLenderAddress();
            this.creatorTrustRating = 500;
        }
    }

    public String getLoanId() { return loanId; }
    public ProposalType getProposalType() { return proposalType; }
    public BigDecimal getLoanFiatAmount() { return loanFiatAmount; }
    public BigDecimal getLoanTokenAmount() { return loanTokenAmount; }
    public BigDecimal getCollateralFiatAmount() { return collateralFiatAmount; }
    public BigDecimal getCollateralTokenAmount() { return collateralTokenAmount; }
    public String getCollateralDocument() { return collateralDocument; }
    public BigDecimal getInterestRate() { return interestRate; }
    public long getDurationInSeconds() { return durationInSeconds; }
    public String getStatus() { return status; }
    public String getCreatorUsername() { return creatorUsername; }
    public String getCreatorPassport() { return creatorPassport; }
    public String getCreatorPublicKey() { return creatorPublicKey; }
    public int getCreatorTrustRating() { return creatorTrustRating; }
}
