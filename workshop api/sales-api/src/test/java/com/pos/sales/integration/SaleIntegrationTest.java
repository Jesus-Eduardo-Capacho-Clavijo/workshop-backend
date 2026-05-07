package com.pos.sales.integration;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.pos.sales.dto.*;
import com.pos.sales.model.enums.PaymentType;
import com.pos.sales.model.enums.SaleStatus;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.math.BigDecimal;

import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.options;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static com.github.tomakehurst.wiremock.client.WireMock.*;

@SpringBootTest
@AutoConfigureMockMvc
class SaleIntegrationTest {

    private static WireMockServer productMockServer;
    private static WireMockServer customerMockServer;

    @Autowired
    private MockMvc mockMvc;

    @BeforeAll
    static void startWireMocks() {
        productMockServer = new WireMockServer(options().dynamicPort());
        productMockServer.start();
        customerMockServer = new WireMockServer(options().dynamicPort());
        customerMockServer.start();
        // Override URLs via system properties for the test context
        System.setProperty("api.product.url", "http://localhost:" + productMockServer.port() + "/api/products");
        System.setProperty("api.customer.url", "http://localhost:" + customerMockServer.port() + "/api/customers");
    }

    @AfterAll
    static void stopWireMocks() {
        productMockServer.stop();
        customerMockServer.stop();
    }

    @BeforeEach
    void setupStubs() {
        // Stub product lookup by ID
        ProductDto product = new ProductDto("P1", "Apple", "123456", new BigDecimal("10.00"), 100, "Fruit");
        productMockServer.stubFor(get(urlPathEqualTo("/api/products/P1"))
                .willReturn(aResponse()
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"id\":\"P1\",\"name\":\"Apple\",\"barcode\":\"123456\",\"unitPrice\":10.00,\"availableStock\":100,\"category\":\"Fruit\"}")));
        // Stub decrement stock (no body needed)
        productMockServer.stubFor(post(urlPathMatching("/api/products/P1/decrement-stock.*"))
                .willReturn(aResponse().withStatus(200)));

        // Stub customer lookup (approved credit)
        CustomerDto customer = new CustomerDto("C1", "John Doe", "ID", "12345678", "APPROVED");
        customerMockServer.stubFor(get(urlPathEqualTo("/api/customers/C1"))
                .willReturn(aResponse()
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"id\":\"C1\",\"fullName\":\"John Doe\",\"documentType\":\"ID\",\"documentNumber\":\"12345678\",\"creditStatus\":\"APPROVED\"}")));
    }

    @Test
    void fullCashSaleFlow() throws Exception {
        // 1️⃣ Create sale
        SaleRequestDto createDto = new SaleRequestDto("TERM-1", null);
        MvcResult createResult = mockMvc.perform(post("/api/sales")
                .contentType(MediaType.APPLICATION_JSON)
                .content(asJson(createDto)))
                .andExpect(status().isCreated())
                .andReturn();
        SaleResponseDto created = fromJson(createResult.getResponse().getContentAsString(), SaleResponseDto.class);
        Long saleId = created.id();
        assertThat(created.status(), is(SaleStatus.ACTIVE));

        // 2️⃣ Add item (2 units)
        SaleItemRequestDto itemDto = new SaleItemRequestDto("P1", null, 2);
        mockMvc.perform(post("/api/sales/" + saleId + "/items")
                .contentType(MediaType.APPLICATION_JSON)
                .content(asJson(itemDto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].productId", is("P1")))
                .andExpect(jsonPath("$.items[0].quantity", is(2))
                .andExpect(jsonPath("$.subtotal", is(20.00)));

        // 3️⃣ Checkout (cash)
        CheckoutRequestDto checkoutDto = new CheckoutRequestDto(PaymentType.CASH, new BigDecimal("25.00"), null);
        MvcResult checkoutResult = mockMvc.perform(post("/api/sales/" + saleId + "/checkout")
                .contentType(MediaType.APPLICATION_JSON)
                .content(asJson(checkoutDto)))
                .andExpect(status().isOk())
                .andReturn();
        ReceiptDto receipt = fromJson(checkoutResult.getResponse().getContentAsString(), ReceiptDto.class);
        assertThat(receipt.paymentType(), is(PaymentType.CASH));
        assertThat(receipt.changeAmount(), is(new BigDecimal("5.00")));
        assertThat(receipt.total(), is(new BigDecimal("20.00")));

        // Verify product stock decrement was called with quantity 2
        productMockServer.verify(postRequestedFor(urlPathEqualTo("/api/products/P1/decrement-stock"))
                .withQueryParam("quantity", equalTo("2")));
    }

    // Helper methods for JSON (using Jackson ObjectMapper)
    private static final com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
    private static String asJson(Object obj) throws com.fasterxml.jackson.core.JsonProcessingException {
        return mapper.writeValueAsString(obj);
    }
    private static <T> T fromJson(String json, Class<T> type) throws java.io.IOException {
        return mapper.readValue(json, type);
    }
}
