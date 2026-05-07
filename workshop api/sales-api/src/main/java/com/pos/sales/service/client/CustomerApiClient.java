package com.pos.sales.service.client;

import com.pos.sales.dto.CustomerDto;
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
public class CustomerApiClient {

    private final RestTemplate restTemplate;
    private final String customerApiUrl;

    public CustomerApiClient(RestTemplate restTemplate, @Value("${api.customer.url}") String customerApiUrl) {
        this.restTemplate = restTemplate;
        this.customerApiUrl = customerApiUrl;
    }

    public List<CustomerDto> searchCustomersByName(String name) {
        try {
            String url = UriComponentsBuilder.fromHttpUrl(customerApiUrl)
                    .pathSegment("search")
                    .queryParam("name", name)
                    .toUriString();
            ResponseEntity<CustomerDto[]> response = restTemplate.getForEntity(url, CustomerDto[].class);
            return Arrays.asList(response.getBody());
        } catch (RestClientException e) {
            throw new ExternalServiceException("Customer API is unavailable: " + e.getMessage());
        }
    }

    public Optional<CustomerDto> getCustomerByDocument(String documentNumber) {
        try {
            String url = UriComponentsBuilder.fromHttpUrl(customerApiUrl)
                    .pathSegment("document", documentNumber)
                    .toUriString();
            ResponseEntity<CustomerDto> response = restTemplate.getForEntity(url, CustomerDto.class);
            return Optional.ofNullable(response.getBody());
        } catch (RestClientException e) {
            return Optional.empty();
        }
    }

    public Optional<CustomerDto> getCustomerById(String id) {
        try {
            String url = UriComponentsBuilder.fromHttpUrl(customerApiUrl)
                    .pathSegment(id)
                    .toUriString();
            ResponseEntity<CustomerDto> response = restTemplate.getForEntity(url, CustomerDto.class);
            return Optional.ofNullable(response.getBody());
        } catch (RestClientException e) {
            return Optional.empty();
        }
    }
}
