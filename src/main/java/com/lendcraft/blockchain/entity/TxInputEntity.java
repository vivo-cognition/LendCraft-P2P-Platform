package com.lendcraft.blockchain.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;

@Entity
@Table(name = "tx_inputs")
public class TxInputEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String utxoId;

    @Column(precision = 18, scale = 8)
    private BigDecimal amount;

    private String ownerAddress;

    public TxInputEntity() {}
    public TxInputEntity(String utxoId, BigDecimal amount, String ownerAddress) {
        this.utxoId = utxoId;
        this.amount = amount;
        this.ownerAddress = ownerAddress;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getUtxoId() { return utxoId; }
    public void setUtxoId(String utxoId) { this.utxoId = utxoId; }
    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }
    public String getOwnerAddress() { return ownerAddress; }
    public void setOwnerAddress(String ownerAddress) { this.ownerAddress = ownerAddress; }

}
