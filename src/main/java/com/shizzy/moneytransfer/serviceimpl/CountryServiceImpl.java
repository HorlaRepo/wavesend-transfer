package com.shizzy.moneytransfer.serviceimpl;

import com.shizzy.moneytransfer.api.ApiResponse;
import com.shizzy.moneytransfer.dto.CreateCountryRequestBody;
import com.shizzy.moneytransfer.exception.IllegalArgumentException;
import com.shizzy.moneytransfer.exception.ResourceNotFoundException;
import com.shizzy.moneytransfer.model.Country;
import com.shizzy.moneytransfer.model.MobileMoneyOption;
import com.shizzy.moneytransfer.repository.CountryRepository;
import com.shizzy.moneytransfer.service.CountryService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Set;

@RequiredArgsConstructor
@Service
public class CountryServiceImpl implements CountryService {

    private final CountryRepository countryRepository;

    @Override
    public ApiResponse<Country> addSupportedCountry(CreateCountryRequestBody requestBody) {

        if(requestBody == null ) {
            throw new IllegalArgumentException("Required body not found");
        }

        Country newCountry = Country.builder()
                        .name(requestBody.name())
                                .rating(requestBody.rating())
                                        .build();

        countryRepository.save(newCountry);

        return new ApiResponse<>(true, "New country added", newCountry);
    }

    @Override
    public ApiResponse<List<Country>> getAllSupportedCountries() {
        List<Country> countries = countryRepository.findAll();
        if(countries.isEmpty()){
            return ApiResponse.<List<Country>>builder()
                    .success(false)
                    .message("No countries found")
                    .build();
        }
        return ApiResponse.<List<Country>>builder()
                .success(true)
                .message("Countries found")
                .data(countries)
                .build();
    }

    @Override
    public ApiResponse<Country> getCountryByName(String countryName) {

        Country country = countryRepository.getCountryByName(countryName)
                .orElseThrow(()-> new  ResourceNotFoundException("Country not supported"));

        return ApiResponse.<Country>builder()
                .success(true)
                .message("Country found")
                .data(country)
                .build();
    }

    @Override
    public ApiResponse<Set<MobileMoneyOption>> getMobileMoneyOptionsByCountryAcronym(String acronym) {
        Country country = countryRepository.findByAcronym(acronym).orElseThrow(()-> new ResourceNotFoundException("Country not supported"));
        return  ApiResponse.<Set<MobileMoneyOption>>builder()
                .success(true)
                .message("Mobile money options found")
                .data(country.getMobileMoneyOption())
                .build();
    }
}
