package com.lendcraft.blockchain.service;

import com.lendcraft.blockchain.entity.LoanEntity;
import com.lendcraft.blockchain.entity.UserEntity;
import com.lendcraft.blockchain.model.*;
import com.lendcraft.blockchain.repository.LoanRepository;
import com.lendcraft.blockchain.repository.UserRepository;
import com.lendcraft.blockchain.mapper.BlockchainMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.UUID;

@Service
public class LoanService {

    public static final String ESCROW_ADDRESS = "SYSTEM_ESCROW_HOLDER";

    private final LoanRepository loanRepository;
    private final BlockchainMapper mapper;
    private final UserRepository userRepository;

    public LoanService(LoanRepository loanRepository, BlockchainMapper mapper,
                       UserRepository userRepository) {
        this.loanRepository = loanRepository;
        this.mapper = mapper;
        this.userRepository = userRepository;
    }

    @Transactional
    public LoanEntity createProposal(String creatorAddress, ProposalType proposalType,
                                     BigDecimal loanFiat, BigDecimal loanToken,
                                     BigDecimal collateralFiat, BigDecimal collateralToken,
                                     String collateralDocument, BigDecimal interestRate, long durationInSeconds) {

        String loanId = UUID.randomUUID().toString();

        LoanEntity entity = new LoanEntity();
        entity.setLoanId(loanId);
        entity.setProposalType(proposalType);
        if (proposalType == ProposalType.BORROW) {
            entity.setBorrowerAddress(creatorAddress);
        } else {
            entity.setLenderAddress(creatorAddress);
        }
        entity.setLoanFiatAmount(loanFiat);
        entity.setLoanTokenAmount(loanToken);
        entity.setCollateralFiatAmount(collateralFiat);
        entity.setCollateralTokenAmount(collateralToken);
        entity.setCollateralDocument(collateralDocument);
        entity.setInterestRate(interestRate);
        entity.setDurationInSeconds(durationInSeconds);
        entity.setStatus(LoanStatus.REQUESTED);
        entity.setRemainingFiatDebt(BigDecimal.ZERO);
        entity.setRemainingTokenDebt(BigDecimal.ZERO);

        UserEntity creatorUser = userRepository.findByPublicKey(creatorAddress)
                .orElseThrow(() -> new IllegalArgumentException("Пользователь-создатель не найден"));

        BigDecimal fiatToLock = (proposalType == ProposalType.BORROW) ? collateralFiat : loanFiat;

        if (fiatToLock.compareTo(BigDecimal.ZERO) > 0) {
            if (creatorUser.getFiatBalance().compareTo(fiatToLock) < 0) {
                throw new IllegalStateException("Недостаточно фиатных средств! Требуется: " + fiatToLock);
            }
            creatorUser.setFiatBalance(creatorUser.getFiatBalance().subtract(fiatToLock));
            userRepository.save(creatorUser);
        }
        return loanRepository.save(entity);
    }

    @Transactional
    public void processBlockchainAccept(Loan loanModel, String responderAddress) {
        LoanEntity entity = loanRepository.findById(loanModel.getLoanId())
                .orElse(null);
        if (entity == null || entity.getStatus() != LoanStatus.REQUESTED) return;

        if (entity.getProposalType() == ProposalType.BORROW) {
            entity.setLenderAddress(responderAddress);
        } else {
            entity.setBorrowerAddress(responderAddress);
        }

        UserEntity borrower = userRepository.findByPublicKey(entity.getBorrowerAddress()).orElse(null);
        UserEntity lender = userRepository.findByPublicKey(entity.getLenderAddress()).orElse(null);
        if (borrower == null || lender == null) return;

        if (entity.getLoanFiatAmount().compareTo(BigDecimal.ZERO) > 0) {
            if (entity.getProposalType() == ProposalType.BORROW) {
                lender.setFiatBalance(lender.getFiatBalance().subtract(entity.getLoanFiatAmount()));
            }
            borrower.setFiatBalance(borrower.getFiatBalance().add(entity.getLoanFiatAmount()));
        }

        BigDecimal interestMultiplier = BigDecimal.ONE.add(entity.getInterestRate());
        entity.setRemainingFiatDebt(entity.getLoanFiatAmount().multiply(interestMultiplier));
        entity.setRemainingTokenDebt(entity.getLoanTokenAmount().multiply(interestMultiplier));

        entity.setStartTime(System.currentTimeMillis());
        entity.setStatus(LoanStatus.ACTIVE);

        userRepository.save(borrower);
        userRepository.save(lender);
        loanRepository.save(entity);
        System.out.println("🔗 Блокчейн-активация: Займ " + entity.getLoanId() + " переведен в статус ACTIVE!");
    }

    @Transactional
    public void processBlockchainRepay(Loan loanModel, BigDecimal tokenPayAmount, BigDecimal fiatPayAmount) {
        LoanEntity entity = loanRepository.findById(loanModel.getLoanId()).orElse(null);
        if (entity == null || entity.getStatus() != LoanStatus.ACTIVE) return;

        UserEntity borrower = userRepository.findByPublicKey(entity.getBorrowerAddress()).orElse(null);
        UserEntity lender = userRepository.findByPublicKey(entity.getLenderAddress()).orElse(null);
        if (borrower == null || lender == null) return;

        if (fiatPayAmount != null && fiatPayAmount.compareTo(BigDecimal.ZERO) > 0) {
            borrower.setFiatBalance(borrower.getFiatBalance().subtract(fiatPayAmount));
            lender.setFiatBalance(lender.getFiatBalance().add(fiatPayAmount));
            entity.setRemainingFiatDebt(entity.getRemainingFiatDebt().subtract(fiatPayAmount));
        }

        if (tokenPayAmount != null && tokenPayAmount.compareTo(BigDecimal.ZERO) > 0) {
            entity.setRemainingTokenDebt(entity.getRemainingTokenDebt().subtract(tokenPayAmount));
        }
        boolean fiatPaid = entity.getRemainingFiatDebt().compareTo(BigDecimal.ZERO) <= 0;
        boolean tokenPaid = entity.getRemainingTokenDebt().compareTo(BigDecimal.ZERO) <= 0;

        if (fiatPaid && tokenPaid && entity.getStatus() == LoanStatus.ACTIVE) {
            entity.setStatus(LoanStatus.REPAID);
            borrower.setFiatBalance(borrower.getFiatBalance().add(entity.getCollateralFiatAmount()));
        }
        userRepository.save(borrower);
        userRepository.save(lender);
        loanRepository.save(entity);
    }

    @Transactional
    public LoanEntity cancelProposal(String loanId, String creatorAddress) {
        LoanEntity entity = loanRepository.findById(loanId)
                .orElseThrow(() -> new IllegalArgumentException("Заявка на заем не найдена"));

        if (!"REQUESTED".equals(entity.getStatus().name())) {
            throw new IllegalStateException("Нельзя отменить активный, закрытый или уже отмененный заем");
        }
        String ownerAddress = entity.getProposalType() == ProposalType.BORROW
                ? entity.getBorrowerAddress()
                : entity.getLenderAddress();
        if (!creatorAddress.equals(ownerAddress)) {
            throw new SecurityException("Можно отменить только собственную заявку");
        }

        UserEntity creatorUser = userRepository.findByPublicKey(creatorAddress)
                .orElseThrow(() -> new IllegalArgumentException("Пользователь-создатель не найден"));

        BigDecimal fiatToRelease = (entity.getProposalType() == ProposalType.BORROW)
                ? entity.getCollateralFiatAmount()
                : entity.getLoanFiatAmount();

        if (fiatToRelease != null && fiatToRelease.compareTo(BigDecimal.ZERO) > 0) {
            creatorUser.setFiatBalance(creatorUser.getFiatBalance().add(fiatToRelease));
            userRepository.save(creatorUser);
            System.out.println("🔄 ВОЗВРАТ ФИАТА: Пользователю @" + creatorUser.getUsername() + " возвращено " + fiatToRelease + " рублей.");
        }

        try {
            entity.setStatus(com.lendcraft.blockchain.model.LoanStatus.CANCELLED);
        } catch (Exception e) {
            System.out.println("Смена статуса через системный маппинг");
        }

        System.out.println("❌ ОТМЕНА: Заявка " + loanId + " успешно аннулирована создателем.");
        return loanRepository.save(entity);
    }

}
