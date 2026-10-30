package com.lendcraft.blockchain.model;

import java.util.ArrayList;
import java.util.List;
import java.math.BigDecimal;


public class Block {
    private String hash;
    private String previousHash;
    private List<Transaction> transactions;
    private String merkleRoot;
    private long timestamp;
    private int nonce=0;
    private int difficulty=4;

    public Block(String previousHash, List<Transaction> transactions, int difficulty) {
        this.previousHash=previousHash;
        this.transactions=transactions != null ? transactions : new ArrayList<>();
        this.timestamp=System.currentTimeMillis();
        this.nonce=0;
        this.difficulty=difficulty;
        MerkleTree merkleTree = new MerkleTree(transactions);
        mineBlock();
    }
    public Block() {
        this.transactions = new ArrayList<>();
    }

    public void mineBlock() {
        String target="0".repeat(difficulty);
        long startTime=System.currentTimeMillis();

        System.out.println("Старт майнинга блока");
        System.out.println("Цель: "+target+"...");

        this.hash=calculateHash();
        while(!this.hash.startsWith(target)) {
            nonce++;
            this.hash=calculateHash();
            if(nonce%100000==0){
                System.out.println("Майнинг запущен: "+nonce+" , текущий хеш: "+ calculateHash());
            }
            if(nonce==Integer.MAX_VALUE) {
                throw new RuntimeException("Nonce переполнен! Увеличиваем сложность?");
            }
        }
        long totalTime=System.currentTimeMillis()-startTime;
        System.out.println("Блок создан! nonce: "+nonce+", хеш: "+hash);
        System.out.printf(" Nonce: %d, время: %.2f сек%n", nonce, totalTime / 1000.0);
    }
    public boolean isProofValid() {
        String target="0".repeat(difficulty);
        return this.hash != null && this.hash.equals(calculateHash()) && this.hash.startsWith(target);
    }
    public String calculateHash() {
        String input = previousHash + merkleRoot + timestamp + nonce;
        return Sha256Hasher.computeHash(input);
    }

    public boolean isValid(UTXOSet utxoSet) {
        if(!hash.equals((calculateHash()))) return false;

        if (!isProofValid()) {
            System.out.println("Proof-of-Work не пройден!");
            return false;
        }

        if (transactions == null || transactions.isEmpty()) {
            System.out.println("Блок пуст! Нет даже награды майнеру.");
            return false;
        }

        Transaction coinbaseTx = transactions.get(0);

        if (coinbaseTx.getInputs() != null && !coinbaseTx.getInputs().isEmpty()) {
            System.out.println("Мошенничество: У Coinbase-транзакции обнаружены входы!");
            return false;
        }

        BigDecimal maxReward = BigDecimal.valueOf(1000);
        if (coinbaseTx.getAmount().compareTo(maxReward) > 0) {
            System.out.println("Мошенничество: Награда майнера превышает лимит сети!");
            return false;
        }

        for(int i = 1; i < transactions.size(); i++) {
            Transaction tx = transactions.get(i);
            if (!tx.isValid()) {return false;}
            for (UTXO input : tx.getInputs()) {
                UTXO existing = utxoSet.getAllUTXOs().get(input.getId());
                if (existing == null) {
                    System.out.println("UTXO не найден: " + input.getId());
                    return false;
                }
                if (!existing.getOwnerAddress().equals(input.getOwnerAddress())) {
                    System.out.println("Владелец UTXO не совпадает!");
                    return false;
                }
            }
        }
        return true;
    }
    public void addTransaction(Transaction tx) {
        if(tx.isValid()) {
            transactions.add(tx);
            this.hash=calculateHash();
        }
    }
    public String getPreviousHash() {
        return previousHash;
    }
    public String getHash(){
        return this.hash;
    }
    public List<Transaction> getTransactions() {
        return transactions;
    }
    public int getNonce() {
        return nonce;
    }

    public void setHash(String hash) {
        this.hash = hash;
    }

    public void setPreviousHash(String previousHash) {
        this.previousHash = previousHash;
    }

    public void setTransactions(List<Transaction> transactions) {
        this.transactions = transactions;
    }

    public void setMerkleRoot(String merkleRoot) {
        this.merkleRoot = merkleRoot;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    public void setNonce(int nonce) {
        this.nonce = nonce;
    }

    public void setDifficulty(int difficulty) {
        this.difficulty = difficulty;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public int getDifficulty() {
        return difficulty;
    }

    public boolean verifyTransaction(String txId, MerkleTree.MerklePath path) {
        MerkleTree tree = new MerkleTree(transactions);
        return tree.verifyTransaction(txId, merkleRoot, path);
    }
    public String getMerkleRoot() {
        return this.merkleRoot;
    }


    @Override
    public String toString() {
        if (hash != null && hash.length() >= 8) {
            return "{" + hash.substring(0, 8) + "...}";
        }
        return "{Без хэша}";
    }

}
