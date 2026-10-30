package com.lendcraft.blockchain.model;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class MemPool {
    private Map<String, Transaction> pendingTransactions;
    private UTXOSet utxoSet;

    public MemPool(UTXOSet utxoSet) {
        this.pendingTransactions = new ConcurrentHashMap<>();
        this.utxoSet = utxoSet;
    }

    public boolean addTransaction(Transaction tx) {
        if (!tx.isValid()) {
            System.out.println("Транзакция не прошла валидацию");
            return false;
        }

        if (tx.getInputs() != null) {
            for (UTXO input : tx.getInputs()) {
                UTXO existing = utxoSet.getAllUTXOs().get(input.getId());
                if (existing == null) {
                    System.out.println("UTXO не найден: " + input.getId());
                    return false;
                }
                if (isUTXOSpentInPending(input.getId())) {
                    System.out.println("UTXO уже потрачен в ожидающей транзакции");
                    return false;
                }
            }
        }

        pendingTransactions.put(tx.getTxId(), tx);
        System.out.println("Транзакция добавлена в мемпул: " + tx.getTxId());
        return true;
    }

    private boolean isUTXOSpentInPending(String utxoId) {
        for (Transaction tx : pendingTransactions.values()) {
            if (tx.getInputs() != null) {
                for (UTXO input : tx.getInputs()) {
                    if (input.getId().equals(utxoId)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    public List<Transaction> getPendingTransactions() {
        return new ArrayList<>(pendingTransactions.values());
    }

    public void clear() {
        pendingTransactions.clear();
        System.out.println("Мемпул очищен");
    }

    public void removeTransaction(String txId) {
        pendingTransactions.remove(txId);
    }

    public int size() {
        return pendingTransactions.size();
    }

    public List<Transaction> selectForMining(int maxCount) {
        List<Transaction> selected = new ArrayList<>(pendingTransactions.values());

        selected.sort((t1, t2) -> Long.compare(t1.getTimestamp(), t2.getTimestamp()));

        if (selected.size() > maxCount) {
            selected = new ArrayList<>(selected.subList(0, maxCount));
        }
        return selected;
    }
}
