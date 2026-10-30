package com.lendcraft.blockchain.model;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.List;
import java.util.ArrayList;

public class Transaction {
    public static final String ESCROW_ADDRESS = "SYSTEM_ESCROW_HOLDER";
    
    private String txId;
    private List<UTXO> inputs;
    private List<Output> outputs;
    private long timestamp;
    private byte[] signature;
    private String publicKeyString;

    private TransactionType type;
    private Loan loanData;

    public static class Output {
        private String address;
        private BigDecimal amount;

        public Output(String address, BigDecimal amount) {
            this.address = address;
            this.amount = amount.setScale(8, RoundingMode.HALF_UP);
        }

        public String getAddress() { return address; }
        public BigDecimal getAmount() { return amount; }
    }
    public Transaction(List<UTXO> inputs, List<Output> outputs) {
        this(inputs, outputs, TransactionType.REGULAR, null);
    }
    public Transaction(List<UTXO> inputs, List<Output> outputs, TransactionType type, Loan loanData) {
        this.inputs = inputs != null ? inputs : new ArrayList<>();
        this.outputs = outputs != null ? outputs : new ArrayList<>();
        this.timestamp = System.currentTimeMillis();
        this.type = type != null ? type : TransactionType.REGULAR;
        this.loanData = loanData;
    }

    public void sign(PrivateKey privateKey, PublicKey publicKey) {
        String data = getDataForSigning();
        this.signature = CryptoUtils.sign(privateKey, data);
        this.publicKeyString = CryptoUtils.keyToString(publicKey);
        this.txId = calculateHash();
    }

    public boolean verifySignature() {
        if (signature == null || publicKeyString == null) {
            System.out.println("Нет подписи или публичного ключа");
            return false;
        }
        try {
            PublicKey publicKey = CryptoUtils.stringToPublicKey(publicKeyString);
            String data = getDataForSigning();
            boolean isValid = CryptoUtils.verify(publicKey, data, signature);
            if (!isValid) {
                System.out.println("Неверная подпись транзакции!");
            }
            return isValid;
        } catch (Exception e) {
            System.out.println("Ошибка проверки подписи: " + e.getMessage());
            return false;
        }
    }

    private String getDataForSigning() {
        StringBuilder sb = new StringBuilder();
        sb.append(type.name());
        if (loanData != null) {
            sb.append(loanData.getLoanId());
        }
        for (UTXO input : inputs) {
            sb.append(input.getId());
        }
        for (Output output : outputs) {
            sb.append(output.getAddress()).append(output.getAmount());
        }
        sb.append(timestamp);
        return sb.toString();
    }

    public String calculateHash() {
        try {
            StringBuilder sb = new StringBuilder();
            sb.append(getDataForSigning());
            if (signature != null) {
                sb.append(new String(signature, StandardCharsets.ISO_8859_1));
            }

            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashBytes = digest.digest(sb.toString().getBytes(StandardCharsets.UTF_8));
            StringBuilder hexString = new StringBuilder();
            for (byte b : hashBytes) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public boolean isValid() {
        if (type == TransactionType.LOAN_DEFAULT) {
            return txId != null && type == TransactionType.LOAN_DEFAULT;
        }
        boolean isFiatLoanEvent = (type == TransactionType.LOAN_ACCEPT || type == TransactionType.LOAN_REPAY)
                && loanData != null
                && loanData.getLoanTokenAmount().compareTo(BigDecimal.ZERO) == 0
                && loanData.getCollateralTokenAmount().compareTo(BigDecimal.ZERO) == 0;

        if (isFiatLoanEvent) {
            return txId != null;
        }
        if (inputs.isEmpty() || outputs.isEmpty()) {
            System.out.println("Нет входов или выходов");
            return false;
        }
        if (!verifySignature()) {
            return false;
        }
        String ownerAddress = publicKeyString;
        for (UTXO input : inputs) {
            if (!input.getOwnerAddress().equals(ownerAddress)) {
                System.out.println("Вход не принадлежит подписанту!");
                return false;
            }
        }

        BigDecimal inputSum = inputs.stream()
                .map(UTXO::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal outputSum = outputs.stream()
                .map(Output::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        if (inputSum.compareTo(outputSum) != 0) {
            System.out.println("Сумма входов != сумме выходов");
            System.out.println("Входы: " + inputSum);
            System.out.println("Выходы: " + outputSum);
            return false;
        }

        for (Output output : outputs) {
            if (output.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
                System.out.println("Сумма выхода должна быть положительной");
                return false;
            }
        }
        return true;
    }
    public static Transaction createLoanTransaction(
            List<UTXO> inputs, List<Output> outputs, TransactionType type, Loan loan, PrivateKey privateKey, PublicKey publicKey) {
        Transaction tx = new Transaction(inputs, outputs, type, loan);
        tx.sign(privateKey, publicKey);
        if (!tx.isValid()) {
            throw new IllegalStateException("Кредитная транзакция не прошла валидацию!");
        }
        return tx;
    }
    public static Transaction createSignedTransaction(
            List<UTXO> inputs, List<Output> outputs, PrivateKey privateKey, PublicKey publicKey) {
        Transaction tx = new Transaction(inputs, outputs);
        tx.sign(privateKey, publicKey);
        if (!tx.isValid()) {
            throw new IllegalStateException("Созданная транзакция не прошла валидацию!");
        }
        return tx;
    }
    public static Transaction createCoinbaseTransaction(String toAddress, BigDecimal amount) {
        List<UTXO> inputs = new ArrayList<>();
        List<Output> outputs = List.of(new Output(toAddress, amount));
        Transaction coinbaseTx = new Transaction(inputs, outputs);
        coinbaseTx.publicKeyString = "COINBASE";
        coinbaseTx.signature = new byte[0];
        coinbaseTx.txId = coinbaseTx.calculateHash();
        return coinbaseTx;
    }

    public String getTxId() { return txId; }
    public List<UTXO> getInputs() { return inputs; }
    public List<Output> getOutputs() { return outputs; }
    public byte[] getSignature() { return signature; }
    public String getPublicKeyString() { return publicKeyString; }
    public long getTimestamp() {
        return timestamp;
    }
    public TransactionType getType() { return type; }
    public void setType(TransactionType type) { this.type = type; }
    public Loan getLoanData() { return loanData; }
    public void setLoanData(Loan loanData) { this.loanData = loanData; }

    public BigDecimal getAmount() {
        if (outputs == null || outputs.isEmpty()) return BigDecimal.ZERO;
        return outputs.stream().map(Output::getAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
    }
    public void setPublicKeyString(String publicKeyString) {
        this.publicKeyString = publicKeyString;
    }

    public void setSignature(byte[] signature) {
        this.signature = signature;
    }

    public void setTxId(String txId) {
        this.txId = txId;
    }

}
