package com.lendcraft.blockchain.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;

@Entity
@Table(name = "tx_outputs")
public class TxOutputEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String address;

    @Column(precision = 18, scale = 8)
    private BigDecimal amount;

    public TxOutputEntity() {}
    public TxOutputEntity(String address, BigDecimal amount) {
        this.address = address;
        this.amount = amount;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }
    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }

}
