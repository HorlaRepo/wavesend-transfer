package com.shizzy.moneytransfer.serviceimpl;



import com.shizzy.moneytransfer.api.ApiResponse;
import com.shizzy.moneytransfer.model.Country;
import com.shizzy.moneytransfer.repository.CountryRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;





class CountryServiceImplTest {

    @Mock
    private CountryRepository countryRepository;

    @InjectMocks
    private CountryServiceImpl countryService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void getAllSupportedCountries_WhenNoCountriesExist_ReturnsFailureResponse() {
        // Arrange
        when(countryRepository.findAll()).thenReturn(new ArrayList<>());

        // Act
        ApiResponse<List<Country>> response = countryService.getAllSupportedCountries();

        // Assert
        assertFalse(response.isSuccess());
        assertEquals("No countries found", response.getMessage());
        assertNull(response.getData());
        verify(countryRepository, times(1)).findAll();
    }

    @Test
    void getAllSupportedCountries_WhenCountriesExist_ReturnsSuccessResponse() {
        // Arrange
        List<Country> countries = Arrays.asList(
                Country.builder().name("Nigeria").build(),
                Country.builder().name("Ghana").build()
        );
        when(countryRepository.findAll()).thenReturn(countries);

        // Act
        ApiResponse<List<Country>> response = countryService.getAllSupportedCountries();

        // Assert
        assertTrue(response.isSuccess());
        assertEquals("Countries found", response.getMessage());
        assertEquals(countries, response.getData());
        assertEquals(2, response.getData().size());
        verify(countryRepository, times(1)).findAll();
    }
}