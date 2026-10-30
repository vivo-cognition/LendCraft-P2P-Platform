package com.lendcraft.blockchain.entity;

import com.lendcraft.blockchain.model.LoanStatus;
import com.lendcraft.blockchain.model.ProposalType;
import jakarta.persistence.*;
import java.math.BigDecimal;

@Entity
@Table(name = "loans")
public class LoanEntity {

    @Id
    @Column(name = "loan_id")
    private String loanId;

    @Enumerated(EnumType.STRING)
    private ProposalType proposalType;

    @Column(columnDefinition = "TEXT")
    private String borrowerAddress;

    @Column(columnDefinition = "TEXT")
    private String lenderAddress;

    private BigDecimal loanFiatAmount;
    private BigDecimal loanTokenAmount;

    private BigDecimal collateralFiatAmount;
    private BigDecimal collateralTokenAmount;

    @Column(columnDefinition = "TEXT")
    private String collateralDocument;

    private BigDecimal interestRate;
    private long durationInSeconds;
    private long startTime;

    private BigDecimal remainingFiatDebt;
    private BigDecimal remainingTokenDebt;

    @Enumerated(EnumType.STRING)
    private LoanStatus status;

    public LoanEntity() {}

    public String getLoanId() {
        return loanId;
    }

    public void setLoanId(String loanId) {
        this.loanId = loanId;
    }

    public ProposalType getProposalType() {
        return proposalType;
    }

    public void setProposalType(ProposalType proposalType) {
        this.proposalType = proposalType;
    }

    public String getBorrowerAddress() {
        return borrowerAddress;
    }

    public void setBorrowerAddress(String borrowerAddress) {
        this.borrowerAddress = borrowerAddress;
    }

    public String getLenderAddress() {
        return lenderAddress;
    }

    public void setLenderAddress(String lenderAddress) {
        this.lenderAddress = lenderAddress;
    }

    public BigDecimal getLoanFiatAmount() {
        return loanFiatAmount;
    }

    public void setLoanFiatAmount(BigDecimal loanFiatAmount) {
        this.loanFiatAmount = loanFiatAmount;
    }

    public BigDecimal getLoanTokenAmount() {
        return loanTokenAmount;
    }

    public void setLoanTokenAmount(BigDecimal loanTokenAmount) {
        this.loanTokenAmount = loanTokenAmount;
    }

    public BigDecimal getCollateralFiatAmount() {
        return collateralFiatAmount;
    }

    public void setCollateralFiatAmount(BigDecimal collateralFiatAmount) {
        this.collateralFiatAmount = collateralFiatAmount;
    }

    public BigDecimal getCollateralTokenAmount() {
        return collateralTokenAmount;
    }

    public void setCollateralTokenAmount(BigDecimal collateralTokenAmount) {
        this.collateralTokenAmount = collateralTokenAmount;
    }

    public String getCollateralDocument() {
        return collateralDocument;
    }

    public void setCollateralDocument(String collateralDocument) {
        this.collateralDocument = collateralDocument;
    }

    public BigDecimal getInterestRate() {
        return interestRate;
    }

    public void setInterestRate(BigDecimal interestRate) {
        this.interestRate = interestRate;
    }

    public long getDurationInSeconds() {
        return durationInSeconds;
    }

    public void setDurationInSeconds(long durationInSeconds) {
        this.durationInSeconds = durationInSeconds;
    }

    public long getStartTime() {
        return startTime;
    }

    public void setStartTime(long startTime) {
        this.startTime = startTime;
    }

    public BigDecimal getRemainingFiatDebt() {
        return remainingFiatDebt;
    }

    public void setRemainingFiatDebt(BigDecimal remainingFiatDebt) {
        this.remainingFiatDebt = remainingFiatDebt;
    }

    public BigDecimal getRemainingTokenDebt() {
        return remainingTokenDebt;
    }

    public void setRemainingTokenDebt(BigDecimal remainingTokenDebt) {
        this.remainingTokenDebt = remainingTokenDebt;
    }

    public LoanStatus getStatus() {
        return status;
    }

    public void setStatus(LoanStatus status) {
        this.status = status;
    }
}
