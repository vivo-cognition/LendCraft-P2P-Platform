package com.lendcraft.blockchain.entity;

import jakarta.persistence.*;
import java.util.List;

@Entity
@Table(name = "blocks")
public class BlockEntity {

    @Id
    @Column(name = "hash")
    private String hash;

    @Column(name = "previous_hash")
    private String previousHash;

    @Column(name = "merkle_root")
    private String merkleRoot;

    private long timestamp;
    private int nonce;
    private int difficulty;

    @OneToMany(cascade = CascadeType.ALL, fetch = FetchType.EAGER)
    @JoinColumn(name = "block_hash")
    @OrderColumn(name = "tx_order")
    private List<TransactionEntity> transactions;

    public BlockEntity() {
    }

    public BlockEntity(String hash, String previousHash, String merkleRoot, long timestamp,
                       int nonce, int difficulty, List<TransactionEntity> transactions) {
        this.hash = hash;
        this.previousHash = previousHash;
        this.merkleRoot = merkleRoot;
        this.timestamp = timestamp;
        this.nonce = nonce;
        this.difficulty = difficulty;
        this.transactions = transactions;
    }

    public String getHash() { return hash; }
    public void setHash(String hash) { this.hash = hash; }
    public String getPreviousHash() { return previousHash; }
    public void setPreviousHash(String previousHash) { this.previousHash = previousHash; }
    public String getMerkleRoot() { return merkleRoot; }
    public void setMerkleRoot(String merkleRoot) { this.merkleRoot = merkleRoot; }
    public long getTimestamp() { return timestamp; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }
    public int getNonce() { return nonce; }
    public void setNonce(int nonce) { this.nonce = nonce; }
    public int getDifficulty() { return difficulty; }
    public void setDifficulty(int difficulty) { this.difficulty = difficulty; }
    public List<TransactionEntity> getTransactions() { return transactions; }
    public void setTransactions(List<TransactionEntity> transactions) { this.transactions = transactions; }
}
