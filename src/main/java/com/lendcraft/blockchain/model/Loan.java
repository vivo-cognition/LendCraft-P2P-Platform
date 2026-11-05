package com.lendcraft.blockchain.model;

import java.math.BigDecimal;

public class Loan {
    private String loanId;
    private ProposalType proposalType;
    private String borrowerAddress;
    private String lenderAddress;

    private BigDecimal loanFiatAmount;
    private BigDecimal loanTokenAmount;

    private BigDecimal collateralFiatAmount;
    private BigDecimal collateralTokenAmount;
    private String collateralDocument;

    private BigDecimal interestRate;
    private long durationInSeconds;
    private long startTime;

    private BigDecimal remainingFiatDebt;
    private BigDecimal remainingTokenDebt;

    private LoanStatus status;

    public Loan(String loanId, ProposalType proposalType, String creatorAddress,
                BigDecimal loanFiatAmount, BigDecimal loanTokenAmount,
                BigDecimal collateralFiatAmount, BigDecimal collateralTokenAmount,
                String collateralDocument, BigDecimal interestRate, long durationInSeconds) {
        this.loanId = loanId;
        this.proposalType = proposalType;

        if (proposalType == ProposalType.BORROW) {
            this.borrowerAddress = creatorAddress;
            this.lenderAddress = null;
        } else {
            this.lenderAddress = creatorAddress;
            this.borrowerAddress = null;
        }

        this.loanFiatAmount = loanFiatAmount != null ? loanFiatAmount : BigDecimal.ZERO;
        this.loanTokenAmount = loanTokenAmount != null ? loanTokenAmount : BigDecimal.ZERO;
        this.collateralFiatAmount = collateralFiatAmount != null ? collateralFiatAmount : BigDecimal.ZERO;
        this.collateralTokenAmount = collateralTokenAmount != null ? collateralTokenAmount : BigDecimal.ZERO;
        this.collateralDocument = collateralDocument;
        this.interestRate = interestRate;
        this.durationInSeconds = durationInSeconds;
        this.status = LoanStatus.REQUESTED;
        this.remainingFiatDebt = BigDecimal.ZERO;
        this.remainingTokenDebt = BigDecimal.ZERO;
        this.startTime = 0;
    }

    public boolean isExpired(long currentTime) {
        if (this.status != LoanStatus.ACTIVE) return false;
        long durationMillis = Math.multiplyExact(durationInSeconds, 1000L);
        return currentTime >= startTime + durationMillis;
    }

    public String getLoanId() { return loanId; }
    public void setLoanId(String loanId) { this.loanId = loanId; }
    public ProposalType getProposalType() { return proposalType; }
    public void setProposalType(ProposalType proposalType) { this.proposalType = proposalType; }
    public String getBorrowerAddress() { return borrowerAddress; }
    public void setBorrowerAddress(String borrowerAddress) { this.borrowerAddress = borrowerAddress; }
    public String getLenderAddress() { return lenderAddress; }
    public void setLenderAddress(String lenderAddress) { this.lenderAddress = lenderAddress; }
    public BigDecimal getLoanFiatAmount() { return loanFiatAmount; }
    public void setLoanFiatAmount(BigDecimal loanFiatAmount) { this.loanFiatAmount = loanFiatAmount; }
    public BigDecimal getLoanTokenAmount() { return loanTokenAmount; }
    public void setLoanTokenAmount(BigDecimal loanTokenAmount) { this.loanTokenAmount = loanTokenAmount; }
    public BigDecimal getCollateralFiatAmount() { return collateralFiatAmount; }
    public void setCollateralFiatAmount(BigDecimal collateralFiatAmount) { this.collateralFiatAmount = collateralFiatAmount; }
    public BigDecimal getCollateralTokenAmount() { return collateralTokenAmount; }
    public void setCollateralTokenAmount(BigDecimal collateralTokenAmount) { this.collateralTokenAmount = collateralTokenAmount; }
    public String getCollateralDocument() { return collateralDocument; }
    public void setCollateralDocument(String collateralDocument) { this.collateralDocument = collateralDocument; }
    public BigDecimal getInterestRate() { return interestRate; }
    public void setInterestRate(BigDecimal interestRate) { this.interestRate = interestRate; }
    public long getDurationInSeconds() { return durationInSeconds; }
    public void setDurationInSeconds(long durationInSeconds) { this.durationInSeconds = durationInSeconds; }
    public long getStartTime() { return startTime; }
    public void setStartTime(long startTime) { this.startTime = startTime; }
    public BigDecimal getRemainingFiatDebt() { return remainingFiatDebt; }
    public void setRemainingFiatDebt(BigDecimal remainingFiatDebt) { this.remainingFiatDebt = remainingFiatDebt; }
    public BigDecimal getRemainingTokenDebt() { return remainingTokenDebt; }
    public void setRemainingTokenDebt(BigDecimal remainingTokenDebt) { this.remainingTokenDebt = remainingTokenDebt; }
    public LoanStatus getStatus() { return status; }
    public void setStatus(LoanStatus status) { this.status = status; }
}
