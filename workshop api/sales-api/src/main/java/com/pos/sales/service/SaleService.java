package com.pos.sales.service;

import com.pos.sales.dto.*;
import com.pos.sales.exception.*;
import com.pos.sales.model.Sale;
import com.pos.sales.model.SaleItem;
import com.pos.sales.model.enums.PaymentType;
import com.pos.sales.model.enums.SaleStatus;
import com.pos.sales.repository.SaleItemRepository;
import com.pos.sales.repository.SaleRepository;
import com.pos.sales.service.client.CustomerApiClient;
import com.pos.sales.service.client.ProductApiClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class SaleService {

    private final SaleRepository saleRepository;
    private final SaleItemRepository saleItemRepository;
    private final ProductApiClient productApiClient;
    private final CustomerApiClient customerApiClient;
    private final BigDecimal taxRate;
    private final int frozenExpirationMinutes;

    public SaleService(
            SaleRepository saleRepository,
            SaleItemRepository saleItemRepository,
            ProductApiClient productApiClient,
            CustomerApiClient customerApiClient,
            @Value("${pos.tax-rate:0.19}") BigDecimal taxRate,
            @Value("${pos.frozen-sale-expiration-minutes:120}") int frozenExpirationMinutes) {
        this.saleRepository = saleRepository;
        this.saleItemRepository = saleItemRepository;
        this.productApiClient = productApiClient;
        this.customerApiClient = customerApiClient;
        this.taxRate = taxRate;
        this.frozenExpirationMinutes = frozenExpirationMinutes;
    }

    @Transactional
    public SaleResponseDto createSale(SaleRequestDto requestDto, String cashierId) {
        Sale sale = new Sale();
        sale.setTerminalId(requestDto.terminalId());
        sale.setCashierId(cashierId);
        sale.setCustomerId(requestDto.customerId());
        sale.setStatus(SaleStatus.ACTIVE);
        sale.setSubtotal(BigDecimal.ZERO);
        sale.setTax(BigDecimal.ZERO);
        sale.setDiscount(BigDecimal.ZERO);
        sale.setTotal(BigDecimal.ZERO);
        return mapToDto(saleRepository.save(sale));
    }

    @Transactional
    public SaleResponseDto addItemToSale(Long saleId, SaleItemRequestDto requestDto) {
        Sale sale = getSaleById(saleId);
        validateStatus(sale, SaleStatus.ACTIVE);

        ProductDto product;
        if (requestDto.productId() != null) {
            product = productApiClient.getProductById(requestDto.productId())
                    .orElseThrow(() -> new ResourceNotFoundException("Product not found"));
        } else if (requestDto.barcode() != null) {
            product = productApiClient.getProductByBarcode(requestDto.barcode())
                    .orElseThrow(() -> new ResourceNotFoundException("Product not found"));
        } else {
            throw new BusinessValidationException("Must provide either productId or barcode");
        }

        Optional<SaleItem> existingItemOpt = sale.getItems().stream()
                .filter(i -> i.getProductId().equals(product.id()))
                .findFirst();

        int newQuantity = existingItemOpt.map(item -> item.getQuantity() + requestDto.quantity())
                .orElse(requestDto.quantity());

        if (newQuantity > product.availableStock()) {
            throw new InsufficientStockException("Requested quantity exceeds available stock");
        }

        if (existingItemOpt.isPresent()) {
            SaleItem existingItem = existingItemOpt.get();
            existingItem.setQuantity(newQuantity);
            existingItem.setLineTotal(product.unitPrice().multiply(BigDecimal.valueOf(newQuantity)));
        } else {
            SaleItem newItem = new SaleItem();
            newItem.setProductId(product.id());
            newItem.setBarcode(product.barcode());
            newItem.setProductName(product.name());
            newItem.setUnitPrice(product.unitPrice());
            newItem.setQuantity(requestDto.quantity());
            newItem.setLineTotal(product.unitPrice().multiply(BigDecimal.valueOf(requestDto.quantity())));
            newItem.setReturnedQuantity(0);
            sale.addItem(newItem);
        }

        recalculateTotals(sale);
        return mapToDto(saleRepository.save(sale));
    }
    
    @Transactional
    public SaleResponseDto removeItem(Long saleId, Long itemId) {
        Sale sale = getSaleById(saleId);
        validateStatus(sale, SaleStatus.ACTIVE);
        
        SaleItem itemToRemove = sale.getItems().stream()
                .filter(i -> i.getId().equals(itemId))
                .findFirst()
                .orElseThrow(() -> new ResourceNotFoundException("Item not found in sale"));
                
        sale.removeItem(itemToRemove);
        recalculateTotals(sale);
        return mapToDto(saleRepository.save(sale));
    }

    @Transactional
    public ReceiptDto checkout(Long saleId, CheckoutRequestDto requestDto) {
        Sale sale = getSaleById(saleId);
        validateStatus(sale, SaleStatus.ACTIVE);

        if (sale.getItems().isEmpty()) {
            throw new BusinessValidationException("Cannot checkout an empty sale");
        }

        if (requestDto.customerId() != null) {
            sale.setCustomerId(requestDto.customerId());
        }

        if (requestDto.paymentType() == PaymentType.CREDIT) {
            if (sale.getCustomerId() == null) {
                throw new BusinessValidationException("Customer association is mandatory for credit sales");
            }
            CustomerDto customer = customerApiClient.getCustomerById(sale.getCustomerId())
                    .orElseThrow(() -> new ResourceNotFoundException("Customer not found"));

            if (!"APPROVED".equalsIgnoreCase(customer.creditStatus())) {
                throw new BusinessValidationException("Customer credit status is not APPROVED");
            }
            sale.setCreditReference("CRED-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase());
            sale.setAmountReceived(sale.getTotal());
            sale.setChangeAmount(BigDecimal.ZERO);
        } else if (requestDto.paymentType() == PaymentType.CASH) {
            if (requestDto.amountReceived() == null || requestDto.amountReceived().compareTo(sale.getTotal()) < 0) {
                throw new BusinessValidationException("Amount received is less than total");
            }
            sale.setAmountReceived(requestDto.amountReceived());
            sale.setChangeAmount(requestDto.amountReceived().subtract(sale.getTotal()));
        }

        for (SaleItem item : sale.getItems()) {
            ProductDto p = productApiClient.getProductById(item.getProductId())
                    .orElseThrow(() -> new ResourceNotFoundException("Product " + item.getProductId() + " not found"));
            if (p.availableStock() < item.getQuantity()) {
                throw new InsufficientStockException("Product " + p.name() + " has insufficient stock");
            }
        }

        for (SaleItem item : sale.getItems()) {
            productApiClient.decrementStock(item.getProductId(), item.getQuantity());
        }

        sale.setPaymentType(requestDto.paymentType());
        sale.setStatus(SaleStatus.COMPLETED);
        sale.setTransactionId("TXN-" + UUID.randomUUID().toString());
        sale.setCompletedAt(LocalDateTime.now());

        saleRepository.save(sale);
        return generateReceipt(sale, false);
    }

    @Transactional
    public SaleResponseDto cancelSale(Long saleId, CancelRequestDto requestDto) {
        Sale sale = getSaleById(saleId);
        if (sale.getStatus() != SaleStatus.ACTIVE && sale.getStatus() != SaleStatus.FROZEN) {
            throw new InvalidSaleStateException("Only ACTIVE or FROZEN sales can be cancelled");
        }
        sale.setStatus(SaleStatus.CANCELLED);
        sale.setCancellationReason(requestDto.reason());
        return mapToDto(saleRepository.save(sale));
    }

    @Transactional
    public SaleResponseDto freezeSale(Long saleId) {
        Sale sale = getSaleById(saleId);
        validateStatus(sale, SaleStatus.ACTIVE);
        sale.setStatus(SaleStatus.FROZEN);
        sale.setFrozenAt(LocalDateTime.now());
        return mapToDto(saleRepository.save(sale));
    }

    @Transactional
    public SaleResponseDto resumeSale(Long saleId) {
        Sale sale = getSaleById(saleId);
        validateStatus(sale, SaleStatus.FROZEN);
        sale.setStatus(SaleStatus.ACTIVE);
        sale.setFrozenAt(null);
        return mapToDto(saleRepository.save(sale));
    }
    
    public List<SaleResponseDto> getFrozenSales(String terminalId) {
        return saleRepository.findByTerminalIdAndStatus(terminalId, SaleStatus.FROZEN).stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    @Transactional
    public ReceiptDto processReturn(Long saleId, ReturnRequestDto requestDto) {
        Sale sale = getSaleById(saleId);
        if (sale.getStatus() != SaleStatus.COMPLETED && sale.getStatus() != SaleStatus.PARTIALLY_RETURNED) {
            throw new InvalidSaleStateException("Only COMPLETED or PARTIALLY_RETURNED sales can be returned");
        }

        boolean isFullReturn = requestDto.items() == null || requestDto.items().isEmpty();
        
        if (isFullReturn) {
            if (sale.getStatus() == SaleStatus.PARTIALLY_RETURNED) {
                 throw new InvalidSaleStateException("Cannot perform a full return on a partially returned sale");
            }
            for (SaleItem item : sale.getItems()) {
                int qtyToReturn = item.getQuantity() - item.getReturnedQuantity();
                if (qtyToReturn > 0) {
                    item.setReturnedQuantity(item.getQuantity());
                    productApiClient.incrementStock(item.getProductId(), qtyToReturn);
                }
            }
            sale.setStatus(SaleStatus.RETURNED);
        } else {
            for (ReturnItemRequestDto returnItemReq : requestDto.items()) {
                SaleItem item = sale.getItems().stream()
                        .filter(i -> i.getId().equals(returnItemReq.saleItemId()))
                        .findFirst()
                        .orElseThrow(() -> new BusinessValidationException("Item not found in sale"));

                if (returnItemReq.quantity() > (item.getQuantity() - item.getReturnedQuantity())) {
                    throw new BusinessValidationException("Cannot return more than purchased quantity for item " + item.getProductName());
                }

                item.setReturnedQuantity(item.getReturnedQuantity() + returnItemReq.quantity());
                productApiClient.incrementStock(item.getProductId(), returnItemReq.quantity());
            }

            boolean allReturned = sale.getItems().stream()
                    .allMatch(i -> i.getQuantity().equals(i.getReturnedQuantity()));

            if (allReturned) {
                sale.setStatus(SaleStatus.RETURNED);
            } else {
                sale.setStatus(SaleStatus.PARTIALLY_RETURNED);
            }
        }
        
        sale.setCancellationReason(requestDto.reason()); 

        saleRepository.save(sale);
        return generateReceipt(sale, true);
    }
    
    @Transactional
    @Scheduled(fixedRate = 60000) // Runs every minute
    public void cleanupExpiredFrozenSales() {
        LocalDateTime expiryTime = LocalDateTime.now().minusMinutes(frozenExpirationMinutes);
        List<Sale> expiredSales = saleRepository.findByStatusAndFrozenAtBefore(SaleStatus.FROZEN, expiryTime);
        for (Sale sale : expiredSales) {
            sale.setStatus(SaleStatus.CANCELLED);
            sale.setCancellationReason("Automatically cancelled due to freeze expiration");
            saleRepository.save(sale);
        }
    }

    private void recalculateTotals(Sale sale) {
        BigDecimal subtotal = sale.getItems().stream()
                .map(SaleItem::getLineTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        sale.setSubtotal(subtotal);
        sale.setTax(subtotal.multiply(taxRate));
        sale.setTotal(sale.getSubtotal().add(sale.getTax()).subtract(sale.getDiscount()));
    }

    private Sale getSaleById(Long saleId) {
        return saleRepository.findById(saleId)
                .orElseThrow(() -> new ResourceNotFoundException("Sale not found"));
    }

    private void validateStatus(Sale sale, SaleStatus expectedStatus) {
        if (sale.getStatus() != expectedStatus) {
            throw new InvalidSaleStateException("Expected status " + expectedStatus + " but was " + sale.getStatus());
        }
    }

    private SaleResponseDto mapToDto(Sale sale) {
        return new SaleResponseDto(
                sale.getId(),
                sale.getTerminalId(),
                sale.getCashierId(),
                sale.getCustomerId(),
                sale.getStatus(),
                sale.getPaymentType(),
                sale.getSubtotal(),
                sale.getTax(),
                sale.getDiscount(),
                sale.getTotal(),
                sale.getAmountReceived(),
                sale.getChangeAmount(),
                sale.getCreditReference(),
                sale.getTransactionId(),
                sale.getCancellationReason(),
                sale.getCompletedAt(),
                sale.getItems().stream().map(this::mapItemToDto).collect(Collectors.toList())
        );
    }

    private SaleItemResponseDto mapItemToDto(SaleItem item) {
        return new SaleItemResponseDto(
                item.getId(),
                item.getProductId(),
                item.getBarcode(),
                item.getProductName(),
                item.getUnitPrice(),
                item.getQuantity(),
                item.getLineTotal(),
                item.getReturnedQuantity()
        );
    }
    
    private ReceiptDto generateReceipt(Sale sale, boolean isReturn) {
        String customerName = null;
        if (sale.getCustomerId() != null) {
            Optional<CustomerDto> customer = customerApiClient.getCustomerById(sale.getCustomerId());
            if (customer.isPresent()) {
                customerName = customer.get().fullName();
            }
        }
        
        return new ReceiptDto(
                "Supermarket POS",
                sale.getTerminalId(),
                sale.getCashierId(),
                sale.getCompletedAt() != null ? sale.getCompletedAt() : LocalDateTime.now(),
                sale.getCustomerId(),
                customerName,
                sale.getItems().stream()
                        .map(this::mapItemToDto)
                        .collect(Collectors.toList()),
                sale.getSubtotal(),
                sale.getTax(),
                sale.getDiscount(),
                sale.getTotal(),
                sale.getPaymentType(),
                sale.getAmountReceived(),
                sale.getChangeAmount(),
                isReturn ? "RET-" + sale.getTransactionId() : sale.getTransactionId(),
                sale.getCreditReference(),
                isReturn,
                isReturn ? sale.getTransactionId() : null
        );
    }
}
