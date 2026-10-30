package com.lendcraft.blockchain.model;

import java.math.BigDecimal;

public class UTXO {
    private String txId;
    private int outputIndex;
    private String ownerAddress;
    private BigDecimal amount;

    public UTXO(String txId, int outputIndex, String ownerAddress, BigDecimal amount) {
        this.txId=txId;
        this.outputIndex=outputIndex;
        this.ownerAddress=ownerAddress;
        this.amount=amount;
    }

    public String getId() {
        return txId+":"+outputIndex;
    }
    public String getTxId() {
        return txId;
    }
    public int getOutputIndex() {
        return outputIndex;
    }
    public String getOwnerAddress() {
        return ownerAddress;
    }
    public BigDecimal getAmount() {
        return amount;
    }

    public void setOwnerAdderess(String ownerAdderess) {
        this.ownerAddress = ownerAdderess;
    }
    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }
}
