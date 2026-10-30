package com.lendcraft.blockchain.model;

import java.math.BigDecimal;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class UTXOSet {
    private Map<String, UTXO> utxoMap;
    private Stack<UTXOSnapshot> snapshots;

    public UTXOSet() {
        this.utxoMap=new ConcurrentHashMap<>();
        this.snapshots = new Stack<>();
    }
    public void snapshot() {
        UTXOSnapshot snapshot = new UTXOSnapshot(new HashMap<>(utxoMap));
        snapshots.push(snapshot);
    }
    public void rollback() {
        if (snapshots.isEmpty()) {
            throw new IllegalStateException("Нет снапшотов для отката");
        }
        UTXOSnapshot snapshot = snapshots.pop();
        this.utxoMap = snapshot.getState();
    }
    public void commit() {
        if (snapshots.isEmpty()) {
            throw new IllegalStateException("Нет снапшотов для подтверждения");
        }
        snapshots.pop();
    }
    public boolean processTransactionWithRollback(Transaction tx) {
        try {
            snapshot();
            processTransaction(tx);
            commit();
            return true;
        } catch (Exception e) {
            rollback();
            System.out.println("Транзакция откачена: " + e.getMessage());
            return false;
        }
    }
    private static class UTXOSnapshot {
        private Map<String, UTXO> state;
        public UTXOSnapshot(Map<String, UTXO> state) {
            this.state = state;
        }
        public Map<String, UTXO> getState() { return state; }
    }
    public void addUTXO(UTXO utxo) {
        utxoMap.put(utxo.getId(), utxo);
    }
    public void removeUTXO(String txId, int outputIndex) {
        String key=txId+":"+outputIndex;
        utxoMap.remove(key);
    }
    public List<UTXO> getUTXOsForAddress(String address) {
        List<UTXO> result=new ArrayList<>();
        for(UTXO utxo : utxoMap.values()) {
            if(utxo.getOwnerAddress().equals(address)) {
                result.add(utxo);
            }
        }
        return result;
    }
    public BigDecimal getBalance(String address) {
        return getUTXOsForAddress(address).stream().map(UTXO::getAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
    }
    public List<UTXO> findSpendableUTXOs(String addres, BigDecimal amount) {
        List<UTXO> available=getUTXOsForAddress(addres);
        List<UTXO> selected=new ArrayList<>();
        BigDecimal total=BigDecimal.ZERO;

        available.sort((u1,u2)-> u1.getAmount().compareTo(u2.getAmount()));

        for(UTXO utxo : available) {
            selected.add(utxo);
            total=total.add(utxo.getAmount());
            if(total.compareTo(amount)>=0) {
                break;
            }
        }
        if(total.compareTo(amount)<0) {
            throw new IllegalStateException("Недостаточно средств! Доступно: "+total+", нужно: "+amount);
        }
        return selected;
    }
    public void processTransaction(Transaction tx) {
        for(UTXO input : tx.getInputs()) {
            removeUTXO(input.getTxId(), input.getOutputIndex());
        }
        for(int i=0; i<tx.getOutputs().size();i++) {
            Transaction.Output output=tx.getOutputs().get(i);
            UTXO newUTXO=new UTXO(tx.getTxId(),i,output.getAddress(),output.getAmount());
            addUTXO(newUTXO);
        }
    }
    public Map<String, UTXO> getAllUTXOs(){
        return new HashMap<>(utxoMap);
    }

    public void clear() {
        this.utxoMap.clear();
    }

}
