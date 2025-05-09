package com.shizzy.moneytransfer.serviceimpl;



import com.shizzy.moneytransfer.api.ApiResponse;
import com.shizzy.moneytransfer.model.PaymentMethod;
import com.shizzy.moneytransfer.repository.CountryPaymentMethodRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;





@ExtendWith(MockitoExtension.class)
class CountryPaymentMethodServiceImplTest {

    @Mock
    private CountryPaymentMethodRepository paymentMethodRepository;

    @InjectMocks
    private CountryPaymentMethodServiceImpl countryPaymentMethodService;

    @Test
    void findPaymentMethodsByCountryAcronym_ShouldReturnPaymentMethods() {
        // Arrange
        String acronym = "US";
        List<PaymentMethod> expectedPaymentMethods = Arrays.asList(
                new PaymentMethod(),
                new PaymentMethod()
        );
        
        when(paymentMethodRepository.findPaymentMethodsByCountryAcronym(acronym))
                .thenReturn(expectedPaymentMethods);

        // Act
        ApiResponse<List<PaymentMethod>> response = countryPaymentMethodService.findPaymentMethodsByCountryAcronym(acronym);

        // Assert
        assertTrue(response.isSuccess());
        assertEquals("Payment methods retrieved successfully", response.getMessage());
        assertEquals(expectedPaymentMethods, response.getData());
        verify(paymentMethodRepository).findPaymentMethodsByCountryAcronym(acronym);
    }

    @Test
    void findPaymentMethodsByCountryAcronym_WithEmptyList_ShouldReturnEmptyResponse() {
        // Arrange
        String acronym = "XX";
        List<PaymentMethod> emptyList = Collections.emptyList();
        
        when(paymentMethodRepository.findPaymentMethodsByCountryAcronym(acronym))
                .thenReturn(emptyList);

        // Act
        ApiResponse<List<PaymentMethod>> response = countryPaymentMethodService.findPaymentMethodsByCountryAcronym(acronym);

        // Assert
        assertTrue(response.isSuccess());
        assertEquals("Payment methods retrieved successfully", response.getMessage());
        assertTrue(response.getData().isEmpty());
        verify(paymentMethodRepository).findPaymentMethodsByCountryAcronym(acronym);
    }
}