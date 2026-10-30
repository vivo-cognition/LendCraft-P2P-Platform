package com.lendcraft.blockchain.model;

import java.math.BigDecimal;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.ArrayList;
import java.util.List;

public class Wallet {
    private String address;
    private PrivateKey privateKey;
    private PublicKey publicKey;
    private Blockchain blockchain;
    private WalletCache cache;

    public Wallet(PrivateKey privateKey, PublicKey publicKey, Blockchain blockchain, WalletCache cache) {
        this.privateKey = privateKey;
        this.publicKey = publicKey;
        this.blockchain = blockchain;
        this.cache = cache;
        this.address = CryptoUtils.keyToString(publicKey);
    }
    public Transaction createLoanTransaction(String toAddress, BigDecimal amount, TransactionType type, Loan loan) {
        BigDecimal realBalance = getRealBalance();
        if (realBalance.compareTo(amount) < 0) {
            throw new IllegalStateException("Недостаточно средств для кредитной операции! Доступно: " + realBalance);
        }

        List<UTXO> spendable = blockchain.getUtxoSet().findSpendableUTXOs(address, amount);

        List<Transaction.Output> outputs = new ArrayList<>();
        outputs.add(new Transaction.Output(toAddress, amount));

        BigDecimal totalInput = spendable.stream()
                .map(UTXO::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal change = totalInput.subtract(amount);
        if (change.compareTo(BigDecimal.ZERO) > 0) {
            outputs.add(new Transaction.Output(address, change));
        }

        Transaction tx = new Transaction(spendable, outputs, type, loan);
        tx.sign(privateKey, publicKey);

        if (!tx.isValid()) {
            throw new IllegalStateException("Созданная кредитная транзакция не прошла валидацию!");
        }
        return tx;
    }

    public BigDecimal getDisplayBalance() {
        return cache.getDisplayBalance(address);
    }

    public BigDecimal getRealBalance() {
        return blockchain.getRealBalance(address);
    }

    public Transaction createTransaction(String toAddress, BigDecimal amount) {
        System.out.println("\n--- СОЗДАНИЕ И ПОДПИСЬ ТРАНЗАКЦИИ ---");
        System.out.println(" От (Адрес/PubKey): " + address);
        System.out.println(" Кому: " + toAddress);
        System.out.println(" Сумма: " + amount);

        BigDecimal realBalance = getRealBalance();
        System.out.println(" Реальный баланс: " + realBalance);

        if (realBalance.compareTo(amount) < 0) {
            throw new IllegalStateException("Недостаточно средств! Доступно: " + realBalance);
        }

        List<UTXO> spendable = blockchain.getUtxoSet().findSpendableUTXOs(address, amount);

        List<Transaction.Output> outputs = new ArrayList<>();
        outputs.add(new Transaction.Output(toAddress, amount));

        BigDecimal totalInput = spendable.stream()
                .map(UTXO::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal change = totalInput.subtract(amount);
        if (change.compareTo(BigDecimal.ZERO) > 0) {
            outputs.add(new Transaction.Output(address, change));
            System.out.println(" Сдача: " + change + " возвращается на " + address);
        }

        Transaction tx = Transaction.createSignedTransaction(spendable, outputs, privateKey, publicKey);

        System.out.println("Транзакция успешно подписана и создана! ID: " + tx.getTxId());
        return tx;
    }

    public void checkRealBalance() {
        BigDecimal balance = getRealBalance();
        System.out.println("\nРеальный баланс " + address + ": " + balance);
        var utxos = blockchain.getUtxoSet().getUTXOsForAddress(address);
        int utxoCount = (utxos != null) ? utxos.size() : 0;
        System.out.println(" (проверено по " + utxoCount + " UTXO)");
    }

    public String getAddress() {
        return address;
    }

    public PublicKey getPublicKey() {
        return publicKey;
    }

    public PrivateKey getPrivateKey() {
        return privateKey;
    }
}
