package com.pos.sales.service.client;

import com.pos.sales.dto.ProductDto;
import com.pos.sales.exception.ExternalServiceException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

@Service
public class ProductApiClient {

    private final RestTemplate restTemplate;
    private final String productApiUrl;

    public ProductApiClient(RestTemplate restTemplate, @Value("${api.product.url}") String productApiUrl) {
        this.restTemplate = restTemplate;
        this.productApiUrl = productApiUrl;
    }

    public List<ProductDto> searchProductsByName(String name) {
        try {
            String url = UriComponentsBuilder.fromHttpUrl(productApiUrl)
                    .pathSegment("search")
                    .queryParam("name", name)
                    .toUriString();
            ResponseEntity<ProductDto[]> response = restTemplate.getForEntity(url, ProductDto[].class);
            return Arrays.asList(response.getBody());
        } catch (RestClientException e) {
            throw new ExternalServiceException("Product API is unavailable: " + e.getMessage());
        }
    }

    public Optional<ProductDto> getProductByBarcode(String barcode) {
        try {
            String url = UriComponentsBuilder.fromHttpUrl(productApiUrl)
                    .pathSegment("barcode", barcode)
                    .toUriString();
            ResponseEntity<ProductDto> response = restTemplate.getForEntity(url, ProductDto.class);
            return Optional.ofNullable(response.getBody());
        } catch (RestClientException e) {
            // Can be 404 Not Found, returning Optional.empty()
            return Optional.empty();
        }
    }

    public Optional<ProductDto> getProductById(String id) {
        try {
            String url = UriComponentsBuilder.fromHttpUrl(productApiUrl)
                    .pathSegment(id)
                    .toUriString();
            ResponseEntity<ProductDto> response = restTemplate.getForEntity(url, ProductDto.class);
            return Optional.ofNullable(response.getBody());
        } catch (RestClientException e) {
            return Optional.empty();
        }
    }

    public void decrementStock(String productId, int quantity) {
        try {
            String url = UriComponentsBuilder.fromHttpUrl(productApiUrl)
                    .pathSegment(productId, "decrement-stock")
                    .queryParam("quantity", quantity)
                    .toUriString();
            restTemplate.postForLocation(url, null);
        } catch (RestClientException e) {
            throw new ExternalServiceException("Failed to update stock in Product API: " + e.getMessage());
        }
    }

    public void incrementStock(String productId, int quantity) {
        try {
            String url = UriComponentsBuilder.fromHttpUrl(productApiUrl)
                    .pathSegment(productId, "increment-stock")
                    .queryParam("quantity", quantity)
                    .toUriString();
            restTemplate.postForLocation(url, null);
        } catch (RestClientException e) {
            throw new ExternalServiceException("Failed to update stock in Product API: " + e.getMessage());
        }
    }
}
