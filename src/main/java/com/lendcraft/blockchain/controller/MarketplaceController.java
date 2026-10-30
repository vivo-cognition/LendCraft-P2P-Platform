package com.lendcraft.blockchain.controller;

import com.lendcraft.blockchain.dto.LoanFilterDto;
import com.lendcraft.blockchain.entity.LoanEntity;
import com.lendcraft.blockchain.model.LoanStatus;
import com.lendcraft.blockchain.repository.LoanRepository;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/marketplace")
public class MarketplaceController {

    private final LoanRepository loanRepository;

    public MarketplaceController(LoanRepository loanRepository) {
        this.loanRepository = loanRepository;
    }

    @PostMapping("/proposals")
    public ResponseEntity<?> getFilteredProposals(@RequestBody LoanFilterDto filter) {

        Specification<LoanEntity> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            predicates.add(cb.equal(root.get("status").as(String.class), "REQUESTED"));

            if (filter.getProposalType() != null) {
                predicates.add(cb.equal(root.get("proposalType"), filter.getProposalType()));
            }

            if (filter.getIsFiat() != null) {
                if (filter.getIsFiat()) {
                    predicates.add(cb.greaterThan(root.get("loanFiatAmount"), java.math.BigDecimal.ZERO));
                } else {
                    predicates.add(cb.greaterThan(root.get("loanTokenAmount"), java.math.BigDecimal.ZERO));
                }
            }

            if (filter.getMinAmount() != null) {
                predicates.add(cb.or(
                        cb.greaterThanOrEqualTo(root.get("loanFiatAmount"), filter.getMinAmount()),
                        cb.greaterThanOrEqualTo(root.get("loanTokenAmount"), filter.getMinAmount())
                ));
            }
            if (filter.getMaxAmount() != null) {
                predicates.add(cb.and(
                        cb.lessThanOrEqualTo(root.get("loanFiatAmount"), filter.getMaxAmount()),
                        cb.lessThanOrEqualTo(root.get("loanTokenAmount"), filter.getMaxAmount())
                ));
            }

            if (filter.getMinRate() != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("interestRate"), filter.getMinRate()));
            }
            if (filter.getMaxRate() != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("interestRate"), filter.getMaxRate()));
            }

            if (filter.getMinDurationDays() != null) {
                long minSecs = (long) filter.getMinDurationDays() * 24 * 60 * 60;
                predicates.add(cb.greaterThanOrEqualTo(root.get("durationInSeconds"), minSecs));
            }
            if (filter.getMaxDurationDays() != null) {
                long maxSecs = (long) filter.getMaxDurationDays() * 24 * 60 * 60;
                predicates.add(cb.lessThanOrEqualTo(root.get("durationInSeconds"), maxSecs));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };

        Sort sort = Sort.by(Sort.Direction.fromString(filter.getSortDirection()), filter.getSortBy());

        Pageable pageable = PageRequest.of(filter.getPage(), filter.getSize(), sort);

        Page<LoanEntity> resultPage = loanRepository.findAll(spec, pageable);

        return ResponseEntity.ok(Map.of(
                "content", resultPage.getContent(),
                "totalPages", resultPage.getTotalPages(),
                "totalElements", resultPage.getTotalElements(),
                "currentPage", resultPage.getNumber()
        ));
    }
}
