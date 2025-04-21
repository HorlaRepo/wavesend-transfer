package com.shizzy.moneytransfer.controller;

import com.shizzy.moneytransfer.api.ApiResponse;
import com.shizzy.moneytransfer.dto.CreateCountryRequestBody;
import com.shizzy.moneytransfer.model.Country;
import com.shizzy.moneytransfer.model.MobileMoneyOption;
import com.shizzy.moneytransfer.service.CountryService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Set;

@RequiredArgsConstructor
@RestController
@RequestMapping("countries")
public class CountryController {

    private final CountryService countryService;

    @GetMapping("/{countryName}")
    public ApiResponse<Country> getCountryByName(@PathVariable String countryName) {
        return countryService.getCountryByName(countryName);
    }

    @GetMapping()
    public ApiResponse<List<Country>> getAllSupportedCountries() {
        return countryService.getAllSupportedCountries();
    }

    @GetMapping("/mobile-money-options/{acronym}")
    public ApiResponse<Set<MobileMoneyOption>> getAllMobileMoneyOptions(@PathVariable String acronym) {
        return countryService.getMobileMoneyOptionsByCountryAcronym(acronym);
    }

    @PostMapping()
    public ApiResponse<Country> addNewCountry (@RequestBody CreateCountryRequestBody country) {
        return countryService.addSupportedCountry(country);
    }

}
