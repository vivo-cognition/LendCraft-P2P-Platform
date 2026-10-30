package com.lendcraft.blockchain.model;

import org.springframework.stereotype.Component;
import java.math.BigDecimal;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.HashMap;
import java.util.Map;

@Component
public class PaymentServer {
    private Blockchain blockchain;
    private Map<String, Wallet> wallets;

    public PaymentServer(Blockchain blockchain) {
        this.blockchain = blockchain;
        this.wallets = new HashMap<>();
    }

    public void registerWallet(PrivateKey privateKey, PublicKey publicKey) {
        WalletCache cache = new WalletCache(blockchain);
        Wallet wallet = new Wallet(privateKey, publicKey, blockchain, cache);

        String address = wallet.getAddress();
        wallets.put(address, wallet);

        System.out.println("Кошелек успешно зарегистрирован! Адрес: " + address);
    }

    public boolean processPayment(String fromAddress, String toAddress, BigDecimal amount) {
        System.out.println("\n" + "=".repeat(60));
        System.out.println("ПОСТУПИЛ ЗАПРОС НА ПЛАТЕЖ");
        System.out.println("=".repeat(60));

        if (!wallets.containsKey(fromAddress)) {
            System.out.println("Кошелек отправителя не найден в системе!");
            return false;
        }

        Wallet wallet = wallets.get(fromAddress);
        BigDecimal realBalance = wallet.getRealBalance();

        System.out.println(" Отправитель: " + fromAddress);
        System.out.println(" Получатель: " + toAddress);
        System.out.println(" Сумма: " + amount);
        System.out.println(" Реальный баланс отправителя: " + realBalance);

        if (realBalance.compareTo(amount) < 0) {
            System.out.println(" ПЛАТЕЖ ОТКЛОНЕН: Недостаточно средств!");
            System.out.println(" Нужно: " + amount + ", доступно: " + realBalance);
            return false;
        }

        try {
            Transaction tx = wallet.createTransaction(toAddress, amount);

            if (!tx.isValid()) {
                System.out.println("ПЛАТЕЖ ОТКЛОНЕН: Созданная транзакция невалидна!");
                return false;
            }

            boolean addedToMemPool = blockchain.sendTransaction(tx);

            if (addedToMemPool) {
                System.out.println(" ПЛАТЕЖ ОДОБРЕН!");
                System.out.println(" Транзакция успешно добавлена в глобальный мемпул сети.");
                System.out.println(" ID транзакции: " + tx.getTxId());
                return true;
            } else {
                System.out.println(" ПЛАТЕЖ ОТКЛОНЕН: Мемпул отверг транзакцию (возможно, Double Spend).");
                return false;
            }

        } catch (Exception e) {
            System.out.println(" ПЛАТЕЖ ОТКЛОНЕН (Исключение): " + e.getMessage());
            return false;
        }
    }

    public void minePendingTransactions(String minerAddress) {
        if (blockchain.getMemPool().getPendingTransactions().isEmpty()) {
            System.out.println("Нет транзакций в мемпуле для майнинга.");
            return;
        }

        System.out.println("\n Запуск майнинга блока для транзакций из мемпула...");

        try {
            Block minedBlock = blockchain.mineBlock(minerAddress);

            if (minedBlock != null) {
                for (Wallet wallet : wallets.values()) {
                    wallet.checkRealBalance();
                }
                System.out.println("Майнинг завершен успешно.");
            }
        } catch (IllegalStateException e) {
            System.err.println(" Critical Error во время майнинга: " + e.getMessage());
        }
    }

    public void showAllWallets() {
        System.out.println("\n" + "=".repeat(60));
        System.out.println(" СОСТОЯНИЕ КОШЕЛЬКОВ НА СЕРВЕРЕ");
        System.out.println("=".repeat(60));

        if (wallets.isEmpty()) {
            System.out.println(" Кошельки отсутствуют.");
            return;
        }

        for (Map.Entry<String, Wallet> entry : wallets.entrySet()) {
            String address = entry.getKey();
            Wallet wallet = entry.getValue();

            var utxoList = blockchain.getUtxoSet().getUTXOsForAddress(address);
            int utxoCount = (utxoList != null) ? utxoList.size() : 0;

            System.out.printf(" Адрес: %s%n", address);
            System.out.printf(" • Реальный баланс: %,.8f%n", wallet.getRealBalance());
            System.out.printf(" • Отображаемый (кэш): %,.8f%n", wallet.getDisplayBalance());
            System.out.printf(" • Количество UTXO: %,d%n", utxoCount);
            System.out.println("-".repeat(60));
        }
    }

    public Wallet getWalletFromAddress(String address) {
        if (!wallets.containsKey(address)) {
            throw new IllegalStateException("Кошелек с адресом " + address + " не зарегистрирован на сервере!");
        }
        return wallets.get(address);
    }
    public boolean hasWallet(String address) {
        return wallets.containsKey(address);
    }
}
