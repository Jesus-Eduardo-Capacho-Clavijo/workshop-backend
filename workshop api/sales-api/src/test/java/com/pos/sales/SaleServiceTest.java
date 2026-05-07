package com.pos.sales;

import com.pos.sales.dto.*;
import com.pos.sales.exception.*;
import com.pos.sales.model.Sale;
import com.pos.sales.model.SaleItem;
import com.pos.sales.model.enums.PaymentType;
import com.pos.sales.model.enums.SaleStatus;
import com.pos.sales.repository.SaleItemRepository;
import com.pos.sales.repository.SaleRepository;
import com.pos.sales.service.SaleService;
import com.pos.sales.service.client.CustomerApiClient;
import com.pos.sales.service.client.ProductApiClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SaleServiceTest {

    @Mock
    private SaleRepository saleRepository;
    
    @Mock
    private SaleItemRepository saleItemRepository;

    @Mock
    private ProductApiClient productApiClient;

    @Mock
    private CustomerApiClient customerApiClient;

    private SaleService saleService;

    @BeforeEach
    void setUp() {
        saleService = new SaleService(saleRepository, saleItemRepository, productApiClient, customerApiClient, new BigDecimal("0.19"), 120);
    }

    @Test
    void createSale_ShouldReturnActiveSale() {
        SaleRequestDto req = new SaleRequestDto("TERM1", null);
        
        Sale savedSale = new Sale();
        savedSale.setId(1L);
        savedSale.setTerminalId("TERM1");
        savedSale.setStatus(SaleStatus.ACTIVE);
        savedSale.setSubtotal(BigDecimal.ZERO);
        savedSale.setTax(BigDecimal.ZERO);
        savedSale.setDiscount(BigDecimal.ZERO);
        savedSale.setTotal(BigDecimal.ZERO);
        
        when(saleRepository.save(any(Sale.class))).thenReturn(savedSale);

        SaleResponseDto res = saleService.createSale(req, "CASHIER1");

        assertNotNull(res);
        assertEquals(SaleStatus.ACTIVE, res.status());
        assertEquals("TERM1", res.terminalId());
    }

    @Test
    void addItem_ShouldIncreaseTotalAndStockCheck() {
        Sale sale = new Sale();
        sale.setId(1L);
        sale.setStatus(SaleStatus.ACTIVE);
        
        when(saleRepository.findById(1L)).thenReturn(Optional.of(sale));

        ProductDto product = new ProductDto("P1", "Apple", "123", new BigDecimal("10"), 50, "Fruit");
        when(productApiClient.getProductById("P1")).thenReturn(Optional.of(product));
        when(saleRepository.save(any(Sale.class))).thenAnswer(i -> i.getArguments()[0]);

        SaleItemRequestDto req = new SaleItemRequestDto("P1", null, 2);

        SaleResponseDto res = saleService.addItemToSale(1L, req);

        assertEquals(1, res.items().size());
        assertEquals(new BigDecimal("20"), res.subtotal());
    }

    @Test
    void checkoutCredit_ValidCustomer_ShouldComplete() {
        Sale sale = new Sale();
        sale.setId(1L);
        sale.setStatus(SaleStatus.ACTIVE);
        sale.setCustomerId("C1");
        sale.setSubtotal(new BigDecimal("100"));
        sale.setTax(new BigDecimal("19"));
        sale.setDiscount(BigDecimal.ZERO);
        sale.setTotal(new BigDecimal("119"));
        
        SaleItem item = new SaleItem();
        item.setId(1L);
        item.setProductId("P1");
        item.setQuantity(2);
        sale.addItem(item);

        when(saleRepository.findById(1L)).thenReturn(Optional.of(sale));
        when(productApiClient.getProductById("P1")).thenReturn(Optional.of(new ProductDto("P1", "Apple", "123", BigDecimal.TEN, 10, "Fruit")));
        
        CustomerDto customer = new CustomerDto("C1", "John", "ID", "123", "APPROVED");
        when(customerApiClient.getCustomerById("C1")).thenReturn(Optional.of(customer));
        when(saleRepository.save(any(Sale.class))).thenAnswer(i -> i.getArguments()[0]);

        CheckoutRequestDto req = new CheckoutRequestDto(PaymentType.CREDIT, null, null);

        ReceiptDto receipt = saleService.checkout(1L, req);

        assertNotNull(receipt);
        assertEquals(SaleStatus.COMPLETED, sale.getStatus());
        assertNotNull(sale.getCreditReference());
        verify(productApiClient, times(1)).decrementStock("P1", 2);
    }
    
    @Test
    void checkoutCredit_CustomerRejected_ShouldThrowException() {
        Sale sale = new Sale();
        sale.setId(1L);
        sale.setStatus(SaleStatus.ACTIVE);
        sale.setCustomerId("C1");
        
        SaleItem item = new SaleItem();
        item.setId(1L);
        item.setProductId("P1");
        item.setQuantity(2);
        sale.addItem(item);

        when(saleRepository.findById(1L)).thenReturn(Optional.of(sale));
        CustomerDto customer = new CustomerDto("C1", "John", "ID", "123", "REJECTED");
        when(customerApiClient.getCustomerById("C1")).thenReturn(Optional.of(customer));

        CheckoutRequestDto req = new CheckoutRequestDto(PaymentType.CREDIT, null, null);

        assertThrows(BusinessValidationException.class, () -> saleService.checkout(1L, req));
    }
}
