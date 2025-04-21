package com.shizzy.moneytransfer.repository;

import com.shizzy.moneytransfer.model.CountryPaymentMethod;
import com.shizzy.moneytransfer.model.CountryPaymentMethodId;
import com.shizzy.moneytransfer.model.PaymentMethod;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface CountryPaymentMethodRepository extends JpaRepository<CountryPaymentMethod, CountryPaymentMethodId> {
    @Query("SELECT cpm.paymentMethod FROM CountryPaymentMethod cpm " +
            "WHERE cpm.country.acronym = :acronym")
    List<PaymentMethod> findPaymentMethodsByCountryAcronym(@Param("acronym") String acronym);
}
