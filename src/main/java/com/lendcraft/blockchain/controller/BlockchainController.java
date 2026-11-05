package com.lendcraft.blockchain.controller;

import com.lendcraft.blockchain.dto.BlockStatusDto;
import com.lendcraft.blockchain.entity.BlockEntity;
import com.lendcraft.blockchain.entity.LoanEntity;
import com.lendcraft.blockchain.mapper.BlockchainMapper;
import com.lendcraft.blockchain.model.Block;
import com.lendcraft.blockchain.model.Blockchain;
import com.lendcraft.blockchain.model.PaymentServer;
import com.lendcraft.blockchain.model.Transaction;
import com.lendcraft.blockchain.repository.BlockRepository;
import com.lendcraft.blockchain.repository.LoanRepository;
import com.lendcraft.blockchain.service.LoanService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Map;
import com.lendcraft.blockchain.entity.UserEntity;
import com.lendcraft.blockchain.repository.UserRepository;
import com.lendcraft.blockchain.security.JwtUtils;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import java.math.BigDecimal;
import org.springframework.beans.factory.annotation.Autowired;

@RestController
@RequestMapping("/api/blockchain")
public class BlockchainController {

    private final Blockchain blockchain;
    private final BlockRepository blockRepository;
    private final BlockchainMapper mapper;
    private final UserRepository userRepository;
    private final JwtUtils jwtUtils;
    private final com.lendcraft.blockchain.service.LoanService loanService;
    private final com.lendcraft.blockchain.model.PaymentServer paymentServer;
    private final com.lendcraft.blockchain.repository.LoanRepository loanRepository;

    @Autowired
    public BlockchainController(BlockRepository blockRepository,
                                BlockchainMapper mapper,
                                UserRepository userRepository,
                                JwtUtils jwtUtils,
                                LoanService loanService,
                                PaymentServer paymentServer,
                                LoanRepository loanRepository,
                                Blockchain blockchain) {
        this.blockRepository = blockRepository;
        this.mapper = mapper;
        this.userRepository = userRepository;
        this.jwtUtils = jwtUtils;
        this.loanService = loanService;
        this.paymentServer = paymentServer;
        this.loanRepository = loanRepository;

        this.blockchain = blockchain;
        if (blockRepository.count() == 0) {
            System.out.println("🔗 База данных пуста. Сохраняем Генезис-блок в PostgreSQL...");
            if (blockchain.getChain() != null && !blockchain.getChain().isEmpty()) {
                Block genesisBlock = blockchain.getChain().get(0);
                BlockEntity genesisEntity = mapper.toEntity(genesisBlock);
                blockRepository.save(genesisEntity);
            }
        } else {
            System.out.println("💾 Найдена существующая цепь в БД. Загружаем блоки в память ядра...");
            blockchain.getChain().clear();
            blockchain.getUtxoSet().clear();
            List<BlockEntity> entities = blockRepository.findAll();

            for(BlockEntity entity : entities) {
                Block block = mapper.toModel(entity);
                blockchain.getChain().add(block);
                if (block.getTransactions() != null) {
                    for (Transaction tx : block.getTransactions()) {
                        blockchain.getUtxoSet().processTransaction(tx);
                    }
                }
            }
            System.out.println("✅ Ядро блокчейна успешно восстановлено! Загружено блоков: " + blockchain.getChain().size());
            System.out.println("📈 Актуальная сложность сети: " + blockchain.getCurrentDifficulty());
        }

    }

    @GetMapping("/balance")
    public ResponseEntity<?> getUserTokenBalance(HttpServletRequest request) {
        Cookie[] cookies = request.getCookies();
        if (cookies == null) {
            return ResponseEntity.status(401).body("Пользователь не авторизован");
        }

        for (Cookie cookie : cookies) {
            if ("accessToken".equals(cookie.getName())) {
                try {
                    String token = cookie.getValue();
                    String username = jwtUtils.validateTokenAndGetUsername(token);

                    UserEntity user = userRepository.findByUsername(username).orElse(null);
                    if (user == null) {
                        return ResponseEntity.badRequest().body("Пользователь не найден");
                    }

                    String publicKey = user.getPublicKey();
                    BigDecimal balance = blockchain.getBalance(publicKey);
                    return ResponseEntity.ok(Map.of(
                            "username", username,
                            "publicKey", publicKey,
                            "tokenBalance", balance
                    ));

                } catch (Exception e) {
                    return ResponseEntity.status(401).body("Неверный токен авторизации или сессия истекла");
                }
            }
        }
        return ResponseEntity.status(401).body("Пользователь не авторизован");
    }

    @GetMapping("/blocks")
    public List<BlockEntity> getBlocksFromDb() {
        return blockRepository.findAll();
    }

    @PostMapping("/mine")
    public ResponseEntity<?> mineNewBlock(@RequestParam(defaultValue = "SYSTEM_MINER") String minerAddress) {
        try {
            Block newBlock = blockchain.mineBlock(minerAddress);
            if (newBlock == null) {
                return ResponseEntity.badRequest().body("Нет транзакций в мемпуле для майнинга!");
            }
            BlockEntity blockEntity = mapper.toEntity(newBlock);
            blockRepository.save(blockEntity);
            return ResponseEntity.ok(blockEntity);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Критическая ошибка при майнинге: " + e.getMessage());
        }
    }

    @GetMapping("/status")
    public ResponseEntity<Map<String, String>> getNodeStatus() {
        return ResponseEntity.ok(Map.of("status", "ONLINE"));
    }

    @GetMapping("/last-block")
    public ResponseEntity<BlockStatusDto> getLastBlockNumber() {
        com.lendcraft.blockchain.model.Block latestBlock = blockchain.getLatestBlock();

        if (latestBlock != null) {
            String fullHash = latestBlock.getHash();
            int difficulty = latestBlock.getDifficulty();
            String cleanHash = "{Без хэша}";

            if (fullHash != null) {
                String noZeros = fullHash.replaceFirst("^0+", "");
                if (noZeros.length() >= 8) {
                    cleanHash = noZeros.substring(0, 8).toUpperCase() + "...";
                } else {
                    cleanHash = noZeros.toUpperCase() + "...";
                }
            }
            return ResponseEntity.ok(new BlockStatusDto(cleanHash, difficulty));
        }
        return ResponseEntity.ok(new BlockStatusDto("{Цепь пуста}", 0));
    }

    @PostMapping("/loans/proposal")
    public ResponseEntity<?> createLoanProposal(@RequestBody Map<String, Object> payload, HttpServletRequest request) {
        String username = getUsernameFromRequest(request);
        if (username == null) return ResponseEntity.status(401).body("Пользователь не авторизован");

        UserEntity user = userRepository.findByUsername(username).orElse(null);
        if (user == null) return ResponseEntity.badRequest().body("Пользователь не найден");

        try {
            com.lendcraft.blockchain.model.ProposalType pType = com.lendcraft.blockchain.model.ProposalType.valueOf((String) payload.get("proposalType"));

            java.math.BigDecimal loanFiat = payload.get("loanFiatAmount") != null ? new java.math.BigDecimal(payload.get("loanFiatAmount").toString()) : java.math.BigDecimal.ZERO;
            java.math.BigDecimal loanToken = payload.get("loanTokenAmount") != null ? new java.math.BigDecimal(payload.get("loanTokenAmount").toString()) : java.math.BigDecimal.ZERO;

            java.math.BigDecimal collateralFiat = payload.get("collateralFiatAmount") != null ? new java.math.BigDecimal(payload.get("collateralFiatAmount").toString()) : java.math.BigDecimal.ZERO;
            java.math.BigDecimal collateralToken = payload.get("collateralTokenAmount") != null ? new java.math.BigDecimal(payload.get("collateralTokenAmount").toString()) : java.math.BigDecimal.ZERO;

            String collateralDocument = (String) payload.get("collateralDocument");
            java.math.BigDecimal interestRate = new java.math.BigDecimal(payload.get("interestRate").toString());
            long durationInSeconds = Long.parseLong(payload.get("durationInSeconds").toString());

            com.lendcraft.blockchain.entity.LoanEntity loan = loanService.createProposal(
                    user.getPublicKey(), pType, loanFiat, loanToken,
                    collateralFiat, collateralToken, collateralDocument, interestRate, durationInSeconds
            );
            return ResponseEntity.ok(loan);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Ошибка создания предложения: " + e.getMessage());
        }
    }

    // Вспомогательный метод для чтения кук
    private String getUsernameFromRequest(HttpServletRequest request) {
        Cookie[] cookies = request.getCookies();
        if (cookies != null) {
            for (Cookie cookie : cookies) {
                if ("accessToken".equals(cookie.getName())) {
                    try {
                        return jwtUtils.validateTokenAndGetUsername(cookie.getValue());
                    } catch (Exception e) {
                        return null;
                    }
                }
            }
        }
        return null;
    }

    @GetMapping("/loans/my")
    public ResponseEntity<?> getMyLoans(HttpServletRequest request) {
        String username = getUsernameFromRequest(request);
        if (username == null) return ResponseEntity.status(401).body("Пользователь не авторизован");

        UserEntity user = userRepository.findByUsername(username).orElse(null);
        if (user == null) return ResponseEntity.badRequest().body("Пользователь не найден");

        String publicKey = user.getPublicKey();

        List myLoans = loanRepository.findAll().stream()
                .filter(l -> publicKey.equals(l.getBorrowerAddress()) || publicKey.equals(l.getLenderAddress()))
                .toList();

        return ResponseEntity.ok(myLoans);
    }

    @GetMapping("/transactions/my")
    public ResponseEntity<?> getMyTransactions(HttpServletRequest request) {
        String username = getUsernameFromRequest(request);
        if (username == null) return ResponseEntity.status(401).body("Пользователь не авторизован");

        UserEntity user = userRepository.findByUsername(username).orElse(null);
        if (user == null) return ResponseEntity.badRequest().body("Пользователь не найден");

        String publicKey = user.getPublicKey();
        List<com.lendcraft.blockchain.model.Transaction> userTxs = new java.util.ArrayList<>();

        for (com.lendcraft.blockchain.model.Block block : blockchain.getChain()) {
            for (com.lendcraft.blockchain.model.Transaction tx : block.getTransactions()) {

                boolean isInput = tx.getInputs().stream().anyMatch(in -> publicKey.equals(in.getOwnerAddress()));
                boolean isOutput = tx.getOutputs().stream().anyMatch(out -> publicKey.equals(out.getAddress()));

                if (isInput || isOutput || publicKey.equals(tx.getPublicKeyString())) {
                    userTxs.add(tx);
                }
            }
        }

        return ResponseEntity.ok(userTxs);
    }

    @GetMapping("/utxos")
    public ResponseEntity<?> getUserUtxos(HttpServletRequest request) {
        String username = getUsernameFromRequest(request);
        if (username == null) return ResponseEntity.status(401).body("Пользователь не авторизован");

        UserEntity user = userRepository.findByUsername(username).orElse(null);
        if (user == null) return ResponseEntity.badRequest().body("Пользователь не найден");

        var utxos = blockchain.getUtxoSet().getUTXOsForAddress(user.getPublicKey());
        return ResponseEntity.ok(utxos != null ? utxos : List.of());
    }

    @PostMapping("/transactions/broadcast")
    public ResponseEntity<?> broadcastTransaction(@RequestBody Map<String, Object> payload) {
        try {
            Transaction tx = mapper.toTransactionModel(payload);
            if (!tx.isValid()) {
                return ResponseEntity.badRequest().body("Транзакция невалидна: неверная подпись или структура!");
            }

            boolean added = blockchain.sendTransaction(tx);
            if (!added) {
                return ResponseEntity.badRequest().body("Транзакция отклонена мемпулом (возможно, Double Spend)");
            }

            return ResponseEntity.ok(Map.of(
                    "message", "Транзакция успешно подписана клиентом и отправлена в мемпул сети",
                    "txId", tx.getTxId()
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Ошибка трансляции транзакции: " + e.getMessage());
        }
    }

    //Автоматическое принятие фиатного займа системой (Майнер собирает блок сам)
    @PostMapping("/loans/accept-fiat")
    public ResponseEntity<?> acceptFiatLoanAutomated(@RequestParam String loanId, HttpServletRequest request) {
        String username = getUsernameFromRequest(request);
        if (username == null) return ResponseEntity.status(401).body("Пользователь не авторизован");

        UserEntity responderUser = userRepository.findByUsername(username).orElse(null);
        if (responderUser == null) return ResponseEntity.badRequest().body("Пользователь не найден");

        try {
            com.lendcraft.blockchain.entity.LoanEntity loanEntity = loanRepository.findById(loanId)
                    .orElseThrow(() -> new IllegalArgumentException("Займ не найден"));

            if (loanEntity.getStatus() != com.lendcraft.blockchain.model.LoanStatus.REQUESTED) {
                return ResponseEntity.badRequest().body("Заявка уже не в статусе ожидания");
            }

            if (loanEntity.getProposalType() == com.lendcraft.blockchain.model.ProposalType.BORROW) {
                loanEntity.setLenderAddress(responderUser.getPublicKey());
            } else {
                loanEntity.setBorrowerAddress(responderUser.getPublicKey());
            }

            com.lendcraft.blockchain.model.Loan loanModel = mapper.toModel(loanEntity);

            Transaction systemTx = new Transaction(new java.util.ArrayList<>(), new java.util.ArrayList<>(),
                    com.lendcraft.blockchain.model.TransactionType.LOAN_ACCEPT, loanModel);
            systemTx.setPublicKeyString(responderUser.getPublicKey());
            systemTx.setSignature(new byte[0]);
            systemTx.setTxId(systemTx.calculateHash());

            blockchain.sendTransaction(systemTx);
            blockchain.mineBlock("SYSTEM_MINER");

            return ResponseEntity.ok(loanRepository.findById(loanId).orElse(loanEntity));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Ошибка автоматического майнинга системы: " + e.getMessage());
        }
    }

    @GetMapping("/loans/my/borrowed")
    public ResponseEntity<?> getMyBorrowedLoans(HttpServletRequest request) {
        String username = getUsernameFromRequest(request);
        if (username == null) return ResponseEntity.status(401).body("Пользователь не авторизован");

        UserEntity user = userRepository.findByUsername(username).orElse(null);
        if (user == null) return ResponseEntity.badRequest().body("Пользователь не найден");

        String publicKey = user.getPublicKey();

        List borrowed = loanRepository.findAll().stream()
                .filter(l -> publicKey.equals(l.getBorrowerAddress()))
                .toList();

        return ResponseEntity.ok(borrowed);
    }

    @GetMapping("/loans/my/lent")
    public ResponseEntity<?> getMyLentLoans(HttpServletRequest request) {
        String username = getUsernameFromRequest(request);
        if (username == null) return ResponseEntity.status(401).body("Пользователь не авторизован");

        UserEntity user = userRepository.findByUsername(username).orElse(null);
        if (user == null) return ResponseEntity.badRequest().body("Пользователь не найден");

        String publicKey = user.getPublicKey();

        List lent = loanRepository.findAll().stream()
                .filter(l -> publicKey.equals(l.getLenderAddress()))
                .toList();

        return ResponseEntity.ok(lent);
    }

    @PostMapping("/loans/cancel")
    public ResponseEntity<?> cancelLoanProposal(@RequestParam String loanId, HttpServletRequest request) {
        String username = getUsernameFromRequest(request);
        if (username == null) return ResponseEntity.status(401).body("Пользователь не авторизован");

        UserEntity user = userRepository.findByUsername(username).orElse(null);
        if (user == null) return ResponseEntity.badRequest().body("Пользователь не найден");

        try {
            com.lendcraft.blockchain.entity.LoanEntity cancelledLoan = loanService.cancelProposal(loanId, user.getPublicKey());
            return ResponseEntity.ok(cancelledLoan);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Ошибка отмены предложения: " + e.getMessage());
        }
    }
}
