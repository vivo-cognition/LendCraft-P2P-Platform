package com.lendcraft.blockchain.dto;

public class BlockStatusDto {
    private String hash;
    private int difficulty;

    public BlockStatusDto(String hash, int difficulty) {
        this.hash = hash;
        this.difficulty = difficulty;
    }

    public String getHash() {
        return hash;
    }

    public int getDifficulty() {
        return difficulty;
    }
}
