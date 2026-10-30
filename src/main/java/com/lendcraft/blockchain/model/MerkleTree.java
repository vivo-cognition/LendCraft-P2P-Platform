package com.lendcraft.blockchain.model;

import java.util.ArrayList;
import java.util.List;

public class MerkleTree {
    private List<String> merkleRoots;
    private List<List<String>> treeLevels;

    public MerkleTree(List<Transaction> transactions) {
        this.merkleRoots = new ArrayList<>();
        this.treeLevels = new ArrayList<>();
        buildTree(transactions);
    }

    private void buildTree(List<Transaction> transactions) {
        List<String> currentLevel = new ArrayList<>();
        for(Transaction tx : transactions) {
            currentLevel.add(tx.getTxId());
        }
        treeLevels.add(currentLevel);

        while (currentLevel.size() > 1) {
            List<String> nextLevel = new ArrayList<>();
            if (currentLevel.size() % 2 == 1) {
                currentLevel.add(currentLevel.get(currentLevel.size() - 1));
            }
            for (int i = 0; i < currentLevel.size(); i += 2) {
                String combined = currentLevel.get(i) + currentLevel.get(i + 1);
                String hash = Sha256Hasher.computeHash(combined);
                nextLevel.add(hash);
            }
            treeLevels.add(nextLevel);
            currentLevel = nextLevel;
        }

        if (!treeLevels.isEmpty()) {
            merkleRoots.add(treeLevels.get(treeLevels.size()-1).get(0));
        }
    }
    public String getMerkleRoot() {
        return merkleRoots.isEmpty() ? "" : merkleRoots.get(0);
    }
    public MerklePath getPathForTransaction(String txId) {
        List<String> level0 = treeLevels.get(0);
        int index = -1;
        for (int i = 0; i < level0.size(); i++) {
            if (level0.get(i).equals(txId)) {
                index = i;
                break;
            }
        }

        if (index == -1) {
            throw new IllegalArgumentException("Транзакция не найдена");
        }

        List<String> hashes = new ArrayList<>();
        List<Boolean> directions = new ArrayList<>();

        for (int level = 0; level < treeLevels.size() - 1; level++) {
            List<String> currentLevel = treeLevels.get(level);

            if (currentLevel.size() % 2 == 1) {
                currentLevel.add(currentLevel.get(currentLevel.size() - 1));
            }

            int pairIndex;
            boolean isRight;

            if (index % 2 == 0) {
                pairIndex = index + 1;
                isRight = true;
            } else {
                pairIndex = index - 1;
                isRight = false;
            }

            hashes.add(currentLevel.get(pairIndex));
            directions.add(isRight);
            index = index / 2;
        }
        return new MerklePath(hashes, directions);
    }

    public boolean verifyTransaction(String txId, String root, MerklePath path) {
        String currentHash = txId;
        List<String> hashes = path.getHashes();
        List<Boolean> directions = path.getDirections();

        for (int i = 0; i < hashes.size(); i++) {
            if (directions.get(i)) {
                currentHash = Sha256Hasher.computeHash(currentHash + hashes.get(i));
            } else  {
                currentHash = Sha256Hasher.computeHash(hashes.get(i) + currentHash);
            }
        }
        return currentHash.equals(root);
    }

    public static class MerklePath {
        private List<String> hashes;
        private List<Boolean> directions;

        public MerklePath(List<String> hashes, List<Boolean> directions) {
            this.hashes = hashes;
            this.directions = directions;
        }

        public List<String> getHashes() {return hashes;}
        public List<Boolean> getDirections() {return directions;}
    }


}
