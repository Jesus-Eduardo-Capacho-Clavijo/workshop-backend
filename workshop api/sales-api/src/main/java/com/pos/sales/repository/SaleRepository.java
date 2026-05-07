package com.pos.sales.repository;

import com.pos.sales.model.Sale;
import com.pos.sales.model.enums.SaleStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface SaleRepository extends JpaRepository<Sale, Long> {
    List<Sale> findByTerminalIdAndStatus(String terminalId, SaleStatus status);
    List<Sale> findByStatusAndFrozenAtBefore(SaleStatus status, LocalDateTime time);
}
