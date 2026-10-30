package com.lendcraft.blockchain.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import com.lendcraft.blockchain.entity.LoanEntity;
import com.lendcraft.blockchain.model.LoanStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface LoanRepository extends JpaRepository<LoanEntity, String>,  JpaSpecificationExecutor<LoanEntity> {
    List<LoanEntity> findAllByStatus(LoanStatus status);
    long countByBorrowerAddressAndLenderAddressAndStatus(String borrower, String lender, LoanStatus status);
}
