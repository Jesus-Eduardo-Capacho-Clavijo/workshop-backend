package com.pos.sales.controller;

import com.pos.sales.dto.*;
import com.pos.sales.service.SaleService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/sales")
public class SaleController {

    private final SaleService saleService;

    public SaleController(SaleService saleService) {
        this.saleService = saleService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public SaleResponseDto createSale(@Valid @RequestBody SaleRequestDto requestDto, 
                                      @RequestHeader(value = "X-Cashier-ID", defaultValue = "CASHIER-1") String cashierId) {
        return saleService.createSale(requestDto, cashierId);
    }

    @PostMapping("/{id}/items")
    public SaleResponseDto addItem(@PathVariable Long id, @Valid @RequestBody SaleItemRequestDto requestDto) {
        return saleService.addItemToSale(id, requestDto);
    }
    
    @DeleteMapping("/{saleId}/items/{itemId}")
    public SaleResponseDto removeItem(@PathVariable Long saleId, @PathVariable Long itemId) {
        return saleService.removeItem(saleId, itemId);
    }

    @PostMapping("/{id}/checkout")
    public ReceiptDto checkout(@PathVariable Long id, @Valid @RequestBody CheckoutRequestDto requestDto) {
        return saleService.checkout(id, requestDto);
    }

    @PostMapping("/{id}/cancel")
    public SaleResponseDto cancelSale(@PathVariable Long id, @Valid @RequestBody CancelRequestDto requestDto) {
        return saleService.cancelSale(id, requestDto);
    }

    @PostMapping("/{id}/freeze")
    public SaleResponseDto freezeSale(@PathVariable Long id) {
        return saleService.freezeSale(id);
    }

    @PostMapping("/{id}/resume")
    public SaleResponseDto resumeSale(@PathVariable Long id) {
        return saleService.resumeSale(id);
    }

    @GetMapping("/frozen")
    public List<SaleResponseDto> getFrozenSales(@RequestParam String terminalId) {
        return saleService.getFrozenSales(terminalId);
    }

    @PostMapping("/{id}/return")
    public ReceiptDto processReturn(@PathVariable Long id, @Valid @RequestBody ReturnRequestDto requestDto) {
        return saleService.processReturn(id, requestDto);
    }
}
