package com.lendcraft.blockchain.model;
import com.lendcraft.blockchain.service.LoanService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;

import org.springframework.stereotype.Component;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.HashSet;

@Component
public class Blockchain {
    private List<Block> chain;
    private UTXOSet utxoSet;
    private MemPool memPool;
    private int currentDifficulty;
    private final int targetBlockTime;
    private final int adjustmentInterval;
    private LoanService loanService;

    @Autowired
    public Blockchain(
            @Value("${blockchain.difficulty:4}") int initialDifficulty,
            @Value("${blockchain.target-time:10}") int targetBlockTime,
            @Value("${blockchain.interval:10}") int adjustmentInterval,
            LoanService loanService) {

        this.targetBlockTime = targetBlockTime;
        this.adjustmentInterval = adjustmentInterval;
        this.currentDifficulty = initialDifficulty;
        this.loanService = loanService;

        this.chain = new ArrayList<>();
        this.utxoSet = new UTXOSet();
        this.memPool = new MemPool(utxoSet);

        createGenesisBlock();
    }


    private void createGenesisBlock() {
        Transaction genesisTx = Transaction.createCoinbaseTransaction("genesis", new BigDecimal("1000"));
        List<Transaction> genesisTxs = List.of(genesisTx);
        Block genesisBlock = new Block("0", genesisTxs, currentDifficulty);
        chain.add(genesisBlock);
        utxoSet.processTransaction(genesisTx);
        System.out.println("🌍 Генезис-блок создан! Баланс genesis: " + utxoSet.getBalance("genesis"));
    }
    public boolean sendTransaction(Transaction tx) {
        return memPool.addTransaction(tx);
    }
    public Block mineBlock(String minerAddress) {
        List<Transaction> rawTxs = memPool.selectForMining(100);
        if (rawTxs.isEmpty()) {
            System.out.println("⏸ Нет транзакций в мемпуле для майнинга.");
            return null;
        }
        List<Transaction> validTxs = new ArrayList<>();
        Set<String> blockUsedUTXOs = new HashSet<>();

        for (Transaction tx : rawTxs) {
            try {
                validateTransaction(tx, blockUsedUTXOs);
                validTxs.add(tx);
            } catch (IllegalStateException e) {
                System.out.println("❌ Транзакция " + tx.getTxId() + " отклонена при майнинге: " + e.getMessage());
                memPool.removeTransaction(tx.getTxId());
            }
        }

        if (validTxs.isEmpty()) {
            System.out.println("⏸ Все транзакции из мемпула оказались невалидными.");
            return null;
        }
        Transaction rewardTx = Transaction.createCoinbaseTransaction(minerAddress, BigDecimal.valueOf(12.5));

        List<Transaction> allTransactions = new ArrayList<>();
        allTransactions.add(rewardTx);
        allTransactions.addAll(validTxs);
        Block previousBlock = getLatestBlock();
        Block newBlock = new Block(previousBlock.getHash(), allTransactions, currentDifficulty);
        if (!newBlock.isValid(utxoSet)) {
            throw new IllegalStateException("Критическая ошибка: Созданный блок невалиден!");
        }
        chain.add(newBlock);
        for (Transaction tx : allTransactions) {
            utxoSet.processTransaction(tx);
        }
        for (Transaction tx : validTxs) {
            if (tx.getType() == TransactionType.LOAN_ACCEPT && tx.getLoanData() != null) {
                loanService.processBlockchainAccept(tx.getLoanData(), tx.getPublicKeyString());
            } else if (tx.getType() == TransactionType.LOAN_REPAY && tx.getLoanData() != null) {
                loanService.processBlockchainRepay(tx.getLoanData(), tx.getAmount(), java.math.BigDecimal.ZERO);
            }
        }
        for (Transaction tx : validTxs) {
            memPool.removeTransaction(tx.getTxId());
        }
        adjustDifficulty();
        System.out.println("📦 Блок #" + (chain.size() - 1) + " успешно добавлен в цепочку!");
        System.out.println("💰 Майнер " + minerAddress + " получил 12.5 монет.");
        return newBlock;
    }

    public void adjustDifficulty() {
        int newDifficulty = calculateRequiredDifficulty(chain.size(), this.currentDifficulty);
        if (newDifficulty != this.currentDifficulty) {
            System.out.println("\nКОРРЕКТИРОВКА СЛОЖНОСТИ: " + this.currentDifficulty + " -> " + newDifficulty);
            this.currentDifficulty = newDifficulty;
        }
    }

    public int calculateRequiredDifficulty(int checkSize, int currentDifficulty) {
        if (checkSize == 0 || checkSize % adjustmentInterval != 0) {
            return currentDifficulty;
        }

        int lastBlockIndex = checkSize - 1;
        int firstBlockIndex = checkSize - adjustmentInterval;

        Block firstBlock = chain.get(firstBlockIndex);
        Block lastBlock = chain.get(lastBlockIndex);

        long actualTime = lastBlock.getTimestamp() - firstBlock.getTimestamp();
        long expectedTime = (long) targetBlockTime * adjustmentInterval;

        if (actualTime < expectedTime / 2) {
            return currentDifficulty + 1;
        } else if (actualTime > expectedTime * 2) {
            return Math.max(1, currentDifficulty - 1);
        }
        return currentDifficulty;
    }

    private void validateTransaction(Transaction tx, Set<String> blockUsedUTXOs) {
        if (tx.getInputs() != null && !tx.getInputs().isEmpty()) {
            if (!tx.verifySignature()) {
                throw new IllegalStateException("Неверная подпись транзакции!");
            }
            for (UTXO input : tx.getInputs()) {
                String key = input.getId();
                if (blockUsedUTXOs.contains(key)) {
                    throw new IllegalStateException("Double spending detected!");
                }
                blockUsedUTXOs.add(key);

                UTXO existing = utxoSet.getAllUTXOs().get(key);
                if (existing == null) {
                    throw new IllegalStateException("UTXO не найден в базе: " + key);
                }
            }
        }
    }
    public boolean isChainValid() {
        if (chain.isEmpty()) return false;
        UTXOSet tempUtxoSet = new UTXOSet();
        Block genesis = chain.get(0);
        for (Transaction tx : genesis.getTransactions()) {
            tempUtxoSet.processTransaction(tx);
        }
        int expectedDifficulty = genesis.getDifficulty();
        for (int i = 1; i < chain.size(); i++) {
            Block current = chain.get(i);
            Block previous = chain.get(i - 1);
            if (!current.getPreviousHash().equals(previous.getHash())) {
                System.out.println("❌ Блок №" + i + " ссылается на неверный предыдущий блок");
                return false;
            }
            if (current.getDifficulty() != expectedDifficulty) {
                System.out.println("❌ Атака на сложность в блоке №" + i);
                return false;
            }
            Set<String> validationSet = new HashSet<>();
            for (Transaction tx : current.getTransactions()) {
                if (tx.getInputs() != null && !tx.getInputs().isEmpty()) {
                    if (!tx.verifySignature()) {
                        System.out.println("❌ Неверная подпись в блоке №" + i + ", транзакция: " + tx.getTxId());
                        return false;
                    }
                    for (UTXO input : tx.getInputs()) {
                        if (validationSet.contains(input.getId()) || !tempUtxoSet.getAllUTXOs().containsKey(input.getId())) {
                            System.out.println("❌ Транзакция в блоке №" + i + " нарушает правила UTXO (Double Spend / Нет входа)");
                            return false;
                        }
                        validationSet.add(input.getId());
                    }
                }
                tempUtxoSet.processTransaction(tx);
            }
            if (!current.isValid(tempUtxoSet)) {
                System.out.println("❌ Блок №" + i + " не валиден (ошибка структуры/PoW)");
                return false;
            }
            expectedDifficulty = calculateRequiredDifficulty(i + 1, expectedDifficulty);
        }
        return true;
    }
    public BigDecimal getRealBalance(String address) {
        if (address == null || address.isEmpty()) {
            return BigDecimal.ZERO;
        }
        // Просто перенаправляем запрос в UTXOSet
        return utxoSet.getBalance(address);
    }
    public Transaction createSystemEscrowTransaction(String toAddress, BigDecimal amount,
                                                     TransactionType type, Loan loan) {
        List<UTXO> inputs = new ArrayList<>();
        BigDecimal accumulatedAmount = BigDecimal.ZERO;
        String escrowAddress = "SYSTEM_ESCROW_HOLDER";

        for (UTXO utxo : utxoSet.getAllUTXOs().values()) {
            if (utxo.getOwnerAddress().equals(escrowAddress)) {
                inputs.add(utxo);
                accumulatedAmount = accumulatedAmount.add(utxo.getAmount());
                if (accumulatedAmount.compareTo(amount) >= 0) {
                    break;
                }
            }
        }

        if (accumulatedAmount.compareTo(amount) < 0) {
            throw new IllegalStateException("Критическая ошибка: В Escrow-фонде не хватает заблокированных монет!");
        }

        List<Transaction.Output> outputs = new ArrayList<>();
        outputs.add(new Transaction.Output(toAddress, amount));

        BigDecimal change = accumulatedAmount.subtract(amount);
        if (change.compareTo(BigDecimal.ZERO) > 0) {
            outputs.add(new Transaction.Output(escrowAddress, change));
        }

        Transaction tx = new Transaction(inputs, outputs, type, loan);
        tx.setPublicKeyString(escrowAddress);
        tx.setSignature(new byte[0]);
        tx.setTxId(tx.calculateHash());

        return tx;
    }

    public Block getLatestBlock() {
        return chain.get(chain.size() - 1);
    }

    public List<Block> getChain() { return chain; }
    public UTXOSet getUtxoSet() { return utxoSet; }
    public MemPool getMemPool() { return memPool; }
    public int getCurrentDifficulty() {
        return currentDifficulty;
    }
    public BigDecimal getBalance(String address) {
        return utxoSet.getBalance(address);
    }
}
