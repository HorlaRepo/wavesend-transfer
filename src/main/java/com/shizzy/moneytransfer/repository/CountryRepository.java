package com.shizzy.moneytransfer.repository;

import com.shizzy.moneytransfer.model.Country;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface CountryRepository extends JpaRepository<Country, Integer> {
    Optional<Country> getCountryByName(String name);

    @Query("SELECT c FROM Countries c WHERE c.acronym = :acronym")
    Optional<Country> findByAcronym(@Param("acronym") String acronym);

    List<Country> findCountriesByRating(Integer rating);

    @Query("SELECT c FROM Countries c WHERE c.rating <= 3")
    List<Country> getHighRiskCountries();
}
