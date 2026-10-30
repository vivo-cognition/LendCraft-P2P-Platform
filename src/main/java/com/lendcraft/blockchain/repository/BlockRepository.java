package com.lendcraft.blockchain.repository;

import com.lendcraft.blockchain.dto.BlockStatusDto;
import com.lendcraft.blockchain.entity.BlockEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import org.springframework.data.domain.Pageable;
import java.util.List;

@Repository
public interface BlockRepository extends JpaRepository<BlockEntity, String> {
    @Query("SELECT new com.lendcraft.blockchain.dto.BlockStatusDto(b.hash, b.difficulty) " +
            "FROM BlockEntity b ORDER BY b.timestamp DESC")
    List<BlockStatusDto> findLatestBlockStatusRaw(Pageable pageable);
}
