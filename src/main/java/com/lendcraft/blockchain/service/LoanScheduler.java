package com.lendcraft.blockchain.service;

import com.lendcraft.blockchain.entity.LoanEntity;
import com.lendcraft.blockchain.entity.UserEntity;
import com.lendcraft.blockchain.model.*;
import com.lendcraft.blockchain.repository.LoanRepository;
import com.lendcraft.blockchain.repository.UserRepository;
import com.lendcraft.blockchain.mapper.BlockchainMapper;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Component
public class LoanScheduler {

    private final LoanRepository loanRepository;
    private final UserRepository userRepository;
    private final BlockchainMapper mapper;
    private final Blockchain blockchain;

    public LoanScheduler(LoanRepository loanRepository, UserRepository userRepository,
                         BlockchainMapper mapper, Blockchain blockchain) {
        this.loanRepository = loanRepository;
        this.userRepository = userRepository;
        this.mapper = mapper;
        this.blockchain = blockchain;
    }

    @Scheduled(fixedRate = 10000)
    @Transactional
    public void checkExpiredLoans() {
        List<LoanEntity> activeLoans = loanRepository.findAllByStatus(LoanStatus.ACTIVE);
        if (activeLoans.isEmpty()) {
            return;
        }

        long currentTime = System.currentTimeMillis();

        for (LoanEntity entity : activeLoans) {
            Loan loanModel = mapper.toModel(entity);

            if (loanModel.isExpired(currentTime)) {
                System.out.println("\n🚨 ВОРКЕР: Обнаружена просрочка по займу! ID: " + entity.getLoanId());

                if (entity.getCollateralFiatAmount().compareTo(BigDecimal.ZERO) > 0) {
                    UserEntity lender = userRepository.findByPublicKey(entity.getLenderAddress()).orElseThrow();
                    lender.setFiatBalance(lender.getFiatBalance().add(entity.getCollateralFiatAmount()));
                    userRepository.save(lender);
                    System.out.println("💵 ФИАТ: Залог в размере " + entity.getCollateralFiatAmount() + " рублей передан Кредитору.");
                }

                if (entity.getCollateralTokenAmount().compareTo(BigDecimal.ZERO) > 0) {
                    Transaction defaultTx = blockchain.createSystemEscrowTransaction(
                            entity.getLenderAddress(),
                            entity.getCollateralTokenAmount(),
                            TransactionType.LOAN_DEFAULT,
                            loanModel
                    );
                    blockchain.sendTransaction(defaultTx);
                    System.out.println("🪙 БЛОКЧЕЙН: Залог в размере " + entity.getCollateralTokenAmount() + " токенов передан Кредитору.");
                }

                if (entity.getCollateralDocument() != null && !entity.getCollateralDocument().trim().isEmpty()) {
                    System.out.println("📄 ДОКУМЕНТ: Юридические права на '" + entity.getCollateralDocument() + "' закреплены за Кредитором.");
                }

                entity.setStatus(LoanStatus.DEFAULTED);
                loanRepository.save(entity);
                System.out.println("✅ Статус займа успешно изменен на DEFAULTED.");
            }
        }
    }
}
