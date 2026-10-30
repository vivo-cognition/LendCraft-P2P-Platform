package com.lendcraft.blockchain.mapper;

import com.lendcraft.blockchain.entity.*;
import com.lendcraft.blockchain.model.*;
import org.springframework.stereotype.Component;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
public class BlockchainMapper {
    public BlockEntity toEntity(Block block) {
        if (block == null) return null;

        List<TransactionEntity> txEntities = block.getTransactions().stream()
                .map(this::toEntity)
                .collect(Collectors.toList());

        return new BlockEntity(
                block.getHash(),
                block.getPreviousHash(),
                block.getMerkleRoot(),
                block.getTimestamp(),
                block.getNonce(),
                block.getDifficulty(),
                txEntities
        );
    }

    public TransactionEntity toEntity(Transaction tx) {
        if (tx == null) return null;

        List<TxInputEntity> inputEntities = tx.getInputs().stream()
                .map(input -> new TxInputEntity(
                        input.getId(),
                        input.getAmount(),
                        input.getOwnerAddress()
                ))
                .collect(Collectors.toList());

        List<TxOutputEntity> outputEntities = tx.getOutputs().stream()
                .map(output -> new TxOutputEntity(
                        output.getAddress(),
                        output.getAmount()
                ))
                .collect(Collectors.toList());

        return new TransactionEntity(
                tx.getTxId(),
                tx.getTimestamp(),
                tx.getSignature(),
                tx.getPublicKeyString(),
                inputEntities,
                outputEntities
        );
    }

    public Block toModel(BlockEntity entity) {
        if (entity == null) return null;

        List<Transaction> transactions = new ArrayList<>();
        if (entity.getTransactions() != null) {
            for (TransactionEntity txEntity : entity.getTransactions()) {
                transactions.add(toModel(txEntity));
            }
        }

        Block block = new Block(entity.getPreviousHash(), transactions, entity.getDifficulty());

        block.setHash(entity.getHash());
        block.setTimestamp(entity.getTimestamp());
        block.setNonce(entity.getNonce());
        block.setMerkleRoot(entity.getMerkleRoot());

        return block;
    }

    public Transaction toModel(TransactionEntity txEntity) {
        if (txEntity == null) return null;
        List<UTXO> inputs = new ArrayList<>();
        if (txEntity.getInputs() != null) {
            for (TxInputEntity inputEntity : txEntity.getInputs()) {
                String utxoId = inputEntity.getUtxoId();
                String txIdParam = txEntity.getTxId();
                int indexParam = 0;


                if (utxoId != null && utxoId.contains(":")) {
                    int colonIndex = utxoId.indexOf(":");
                    txIdParam =  utxoId.substring(0, colonIndex);
                    try {
                        indexParam = Integer.parseInt( utxoId.substring(colonIndex + 1));
                    } catch (NumberFormatException ignored) {}
                }

                inputs.add(new UTXO(txIdParam, indexParam, inputEntity.getOwnerAddress(), inputEntity.getAmount()));
            }
        }

        List<Transaction.Output> outputs = new ArrayList<>();
        if (txEntity.getOutputs() != null) {
            for (TxOutputEntity outputEntity : txEntity.getOutputs()) {
                outputs.add(new Transaction.Output(outputEntity.getAddress(), outputEntity.getAmount()));
            }
        }
        Transaction tx = new Transaction(inputs, outputs);

        try {
            setPrivateField(tx, "txId", txEntity.getTxId());
            setPrivateField(tx, "timestamp", txEntity.getTimestamp());
            setPrivateField(tx, "signature", txEntity.getSignature());
            setPrivateField(tx, "publicKeyString", txEntity.getPublicKeyString());
        } catch (Exception e) {
            System.err.println("Ошибка восстановления полей транзакции через рефлексию: " + e.getMessage());
        }

        return tx;
    }

    private void setPrivateField(Object target, String fieldName, Object value) throws Exception {
        Field field = target.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(target, value);
    }
    public com.lendcraft.blockchain.entity.LoanEntity toEntity(com.lendcraft.blockchain.model.Loan model) {
        if (model == null) return null;
        com.lendcraft.blockchain.entity.LoanEntity entity = new com.lendcraft.blockchain.entity.LoanEntity();
        entity.setLoanId(model.getLoanId());
        entity.setProposalType(model.getProposalType());
        entity.setBorrowerAddress(model.getBorrowerAddress());
        entity.setLenderAddress(model.getLenderAddress());

        entity.setLoanFiatAmount(model.getLoanFiatAmount());
        entity.setLoanTokenAmount(model.getLoanTokenAmount());

        entity.setCollateralFiatAmount(model.getCollateralFiatAmount());
        entity.setCollateralTokenAmount(model.getCollateralTokenAmount());
        entity.setCollateralDocument(model.getCollateralDocument());

        entity.setInterestRate(model.getInterestRate());
        entity.setDurationInSeconds(model.getDurationInSeconds());
        entity.setStartTime(model.getStartTime());

        entity.setRemainingFiatDebt(model.getRemainingFiatDebt());
        entity.setRemainingTokenDebt(model.getRemainingTokenDebt());

        entity.setStatus(model.getStatus());
        return entity;
    }

    public com.lendcraft.blockchain.model.Loan toModel(com.lendcraft.blockchain.entity.LoanEntity entity) {
        if (entity == null) return null;

        String creator = entity.getProposalType() == com.lendcraft.blockchain.model.ProposalType.BORROW ?
                entity.getBorrowerAddress() : entity.getLenderAddress();

        com.lendcraft.blockchain.model.Loan loan = new com.lendcraft.blockchain.model.Loan(
                entity.getLoanId(), entity.getProposalType(), creator,
                entity.getLoanFiatAmount(), entity.getLoanTokenAmount(),
                entity.getCollateralFiatAmount(), entity.getCollateralTokenAmount(),
                entity.getCollateralDocument(), entity.getInterestRate(), entity.getDurationInSeconds()
        );

        loan.setLenderAddress(entity.getLenderAddress());
        loan.setBorrowerAddress(entity.getBorrowerAddress());
        loan.setStartTime(entity.getStartTime());

        loan.setRemainingFiatDebt(entity.getRemainingFiatDebt());
        loan.setRemainingTokenDebt(entity.getRemainingTokenDebt());

        loan.setStatus(entity.getStatus());
        return loan;
    }
    @SuppressWarnings("unchecked")
    public Transaction toTransactionModel(Map<String, Object> payload) {
        if (payload == null) return null;

        List<UTXO> inputs = new ArrayList<>();
        List<Map<String, Object>> rawInputs = (List<Map<String, Object>>) payload.get("inputs");
        if (rawInputs != null) {
            for (Map<String, Object> inputMap : rawInputs) {
                String txId = (String) inputMap.get("txId");
                int index = ((Number) inputMap.get("index")).intValue();
                String ownerAddress = (String) inputMap.get("ownerAddress");
                java.math.BigDecimal amount = new java.math.BigDecimal(inputMap.get("amount").toString());
                inputs.add(new UTXO(txId, index, ownerAddress, amount));
            }
        }

        List<Transaction.Output> outputs = new ArrayList<>();
        List<Map<String, Object>> rawOutputs = (List<Map<String, Object>>) payload.get("outputs");
        if (rawOutputs != null) {
            for (Map<String, Object> outputMap : rawOutputs) {
                String address = (String) outputMap.get("address");
                java.math.BigDecimal amount = new java.math.BigDecimal(outputMap.get("amount").toString());
                outputs.add(new Transaction.Output(address, amount));
            }
        }

        com.lendcraft.blockchain.model.TransactionType txType = com.lendcraft.blockchain.model.TransactionType.REGULAR;
        if (payload.get("type") != null) {
            txType = com.lendcraft.blockchain.model.TransactionType.valueOf((String) payload.get("type"));
        }

        com.lendcraft.blockchain.model.Loan loanModel = null;
        Map<String, Object> loanMap = (Map<String, Object>) payload.get("loanData");
        if (loanMap != null) {
            String loanId = (String) loanMap.get("loanId");
            com.lendcraft.blockchain.model.ProposalType pType = com.lendcraft.blockchain.model.ProposalType.valueOf((String) loanMap.get("proposalType"));
            String creator = (String) loanMap.get("creator");
            java.math.BigDecimal loanFiat = new java.math.BigDecimal(loanMap.get("loanFiatAmount").toString());
            java.math.BigDecimal loanToken = new java.math.BigDecimal(loanMap.get("loanTokenAmount").toString());
            java.math.BigDecimal collateralFiat = new java.math.BigDecimal(loanMap.get("collateralFiatAmount").toString());
            java.math.BigDecimal collateralToken = new java.math.BigDecimal(loanMap.get("collateralTokenAmount").toString());
            String collateralDoc = (String) loanMap.get("collateralDocument");
            java.math.BigDecimal rate = new java.math.BigDecimal(loanMap.get("interestRate").toString());
            long duration = ((Number) loanMap.get("durationInSeconds")).longValue();

            loanModel = new com.lendcraft.blockchain.model.Loan(
                    loanId, pType, creator, loanFiat, loanToken, collateralFiat, collateralToken, collateralDoc, rate, duration
            );
            if (loanMap.get("status") != null) {
                loanModel.setStatus(com.lendcraft.blockchain.model.LoanStatus.valueOf((String) loanMap.get("status")));
            }
            if (loanMap.get("borrowerAddress") != null) loanModel.setBorrowerAddress((String) loanMap.get("borrowerAddress"));
            if (loanMap.get("lenderAddress") != null) loanModel.setLenderAddress((String) loanMap.get("lenderAddress"));
            if (loanMap.get("startTime") != null) loanModel.setStartTime(((Number) loanMap.get("startTime")).longValue());
        }

        Transaction tx = new Transaction(inputs, outputs, txType, loanModel);

        try {
            if (payload.get("txId") != null) {
                setPrivateField(tx, "txId", payload.get("txId"));
            }
            if (payload.get("timestamp") != null) {
                setPrivateField(tx, "timestamp", ((Number) payload.get("timestamp")).longValue());
            }
            if (payload.get("publicKeyString") != null) {
                setPrivateField(tx, "publicKeyString", payload.get("publicKeyString"));
            }

            if (payload.get("signature") != null) {
                byte[] sigBytes = java.util.Base64.getDecoder().decode((String) payload.get("signature"));
                setPrivateField(tx, "signature", sigBytes);
            }
        } catch (Exception e) {
            System.err.println("Ошибка десериализации системных полей транзакции: " + e.getMessage());
        }

        return tx;
    }

}
