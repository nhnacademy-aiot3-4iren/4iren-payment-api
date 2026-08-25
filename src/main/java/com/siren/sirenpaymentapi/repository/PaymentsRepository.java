package com.siren.sirenpaymentapi.repository;

import com.siren.sirenpaymentapi.domain.entity.Payments;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PaymentsRepository extends JpaRepository<Payments, Long>, PaymentsRepositoryCustom {
}
