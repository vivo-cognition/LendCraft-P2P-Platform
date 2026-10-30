package com.lendcraft.blockchain.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.util.List;

@Entity
@Table(name = "transactions")
public class TransactionEntity {

    @Id
    @Column(name = "tx_id")
    private String txId;

    private long timestamp;

    @Lob
    private byte[] signature;

    @Column(columnDefinition = "TEXT")
    private String publicKeyString;

    @OneToMany(cascade = CascadeType.ALL, fetch = FetchType.EAGER)
    @JoinColumn(name = "tx_id")
    private List<TxInputEntity> inputs;

    @OneToMany(cascade = CascadeType.ALL, fetch = FetchType.EAGER)
    @JoinColumn(name = "tx_id")
    private List<TxOutputEntity> outputs;

    public TransactionEntity() {}

    public TransactionEntity(String txId, long timestamp, byte[] signature, String publicKeyString,
                             List<TxInputEntity> inputs, List<TxOutputEntity> outputs) {
        this.txId = txId;
        this.timestamp = timestamp;
        this.signature = signature;
        this.publicKeyString = publicKeyString;
        this.inputs = inputs;
        this.outputs = outputs;
    }

    public String getTxId() { return txId; }
    public void setTxId(String txId) { this.txId = txId; }
    public long getTimestamp() { return timestamp; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }
    public byte[] getSignature() { return signature; }
    public void setSignature(byte[] signature) { this.signature = signature; }
    public String getPublicKeyString() { return publicKeyString; }
    public void setPublicKeyString(String publicKeyString) { this.publicKeyString = publicKeyString; }
    public List<TxInputEntity> getInputs() { return inputs; }
    public void setInputs(List<TxInputEntity> inputs) { this.inputs = inputs; }
    public List<TxOutputEntity> getOutputs() { return outputs; }
    public void setOutputs(List<TxOutputEntity> outputs) { this.outputs = outputs; }
}
