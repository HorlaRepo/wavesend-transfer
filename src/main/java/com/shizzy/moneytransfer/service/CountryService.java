package com.shizzy.moneytransfer.service;

import com.shizzy.moneytransfer.api.ApiResponse;
import com.shizzy.moneytransfer.dto.CreateCountryRequestBody;
import com.shizzy.moneytransfer.model.Country;
import com.shizzy.moneytransfer.model.MobileMoneyOption;

import java.util.List;
import java.util.Set;

public interface CountryService {
    ApiResponse<Set<MobileMoneyOption>> getMobileMoneyOptionsByCountryAcronym(String acronym);
    ApiResponse<Country> getCountryByName(String countryName);
    ApiResponse<List<Country>> getAllSupportedCountries();
    ApiResponse<Country> addSupportedCountry(CreateCountryRequestBody requestBody);
}
