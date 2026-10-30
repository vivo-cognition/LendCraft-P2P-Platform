package com.lendcraft.blockchain.dto;

import com.lendcraft.blockchain.model.ProposalType;
import java.math.BigDecimal;

public class LoanFilterDto {
    private ProposalType proposalType;
    private Boolean isFiat;
    private BigDecimal minAmount;
    private BigDecimal maxAmount;
    private Integer minDurationDays;
    private Integer maxDurationDays;
    private BigDecimal minRate;
    private BigDecimal maxRate;

    private int page = 0;
    private int size = 10;
    private String sortBy = "loanId";
    private String sortDirection = "ASC";

    public ProposalType getProposalType() { return proposalType; }
    public void setProposalType(ProposalType proposalType) { this.proposalType = proposalType; }
    public Boolean getIsFiat() { return isFiat; }
    public void setIsFiat(Boolean isFiat) { this.isFiat = isFiat; }
    public BigDecimal getMinAmount() { return minAmount; }
    public void setMinAmount(BigDecimal minAmount) { this.minAmount = minAmount; }
    public BigDecimal getMaxAmount() { return maxAmount; }
    public void setMaxAmount(BigDecimal maxAmount) { this.maxAmount = maxAmount; }
    public Integer getMinDurationDays() { return minDurationDays; }
    public void setMinDurationDays(Integer minDurationDays) { this.minDurationDays = minDurationDays; }
    public Integer getMaxDurationDays() { return maxDurationDays; }
    public void setMaxDurationDays(Integer maxDurationDays) { this.maxDurationDays = maxDurationDays; }
    public BigDecimal getMinRate() { return minRate; }
    public void setMinRate(BigDecimal minRate) { this.minRate = minRate; }
    public BigDecimal getMaxRate() { return maxRate; }
    public void setMaxRate(BigDecimal maxRate) { this.maxRate = maxRate; }
    public int getPage() { return page; }
    public void setPage(int page) { this.page = page; }
    public int getSize() { return size; }
    public void setSize(int size) { this.size = size; }
    public String getSortBy() { return sortBy; }
    public void setSortBy(String sortBy) { this.sortBy = sortBy; }
    public String getSortDirection() { return sortDirection; }
    public void setSortDirection(String sortDirection) { this.sortDirection = sortDirection; }
}
