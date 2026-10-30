package com.lendcraft.blockchain.model;

import java.math.BigDecimal;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class WalletCache {
    private final Map<String, BigDecimal> cachedBalances;
    private final Blockchain blockchain;

    public WalletCache(Blockchain blockchain) {
        this.blockchain = blockchain;
        this.cachedBalances = new ConcurrentHashMap<>();
    }

    public void refreshFromBlockchain() {
        System.out.println("Обновление кэша кошельков из блокчейна...");
        cachedBalances.clear();

        Map<String, UTXO> allUtxos = blockchain.getUtxoSet().getAllUTXOs();
        for (UTXO utxo : allUtxos.values()) {
            String ownerAddress = utxo.getOwnerAddress();
            BigDecimal amount = utxo.getAmount();

            if (amount != null) {
                cachedBalances.merge(ownerAddress, amount, BigDecimal::add);
            } else {
                System.err.println("⚠️ КРИТИЧЕСКАЯ ОШИБКА: Обнаружен UTXO без суммы! ID: " + utxo.getId() + " Владелец: " + ownerAddress);
            }
        }
        System.out.println("Кэш кошельков успешно обновлен. Активных адресов: " + cachedBalances.size());
    }

    public BigDecimal getDisplayBalance(String address) {
        return cachedBalances.computeIfAbsent(address, key -> blockchain.getRealBalance(key));
    }

    public void updateAddress(String address) {
        if (address == null || address.isEmpty()) return;

        BigDecimal realBalance = blockchain.getRealBalance(address);
        cachedBalances.put(address, realBalance);
        System.out.println("Кэш: Баланс " + address + " синхронизирован: " + realBalance);
    }
}
