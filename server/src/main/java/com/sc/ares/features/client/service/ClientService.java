package com.sc.ares.features.client.service;

import com.sc.ares.common.api.ApiBadRequestException;
import com.sc.ares.common.api.PagedResponse;
import com.sc.ares.common.api.ResourceNotFoundException;
import com.sc.ares.features.client.dto.ClientResponse;
import com.sc.ares.features.client.dto.UpsertClientRequest;
import com.sc.ares.features.client.entity.Client;
import com.sc.ares.features.client.entity.ClientStatus;
import com.sc.ares.features.client.repository.ClientRepository;
import com.sc.ares.features.company.entity.Company;
import com.sc.ares.features.company.repository.CompanyRepository;
import java.util.Map;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ClientService {

    private static final int DEFAULT_PAGE_SIZE = 20;
    private static final int MAX_PAGE_SIZE = 100;
    private static final Map<String, String> SORT_FIELD_MAPPING = Map.of(
        "name", "name",
        "email", "email",
        "status", "status",
        "createdAt", "createdAt"
    );

    private final ClientRepository clientRepository;
    private final CompanyRepository companyRepository;
    private final ClientAccessService clientAccessService;

    public ClientService(
        ClientRepository clientRepository,
        CompanyRepository companyRepository,
        ClientAccessService clientAccessService
    ) {
        this.clientRepository = clientRepository;
        this.companyRepository = companyRepository;
        this.clientAccessService = clientAccessService;
    }

    @Transactional(readOnly = true)
    public PagedResponse<ClientResponse> listClients(
        int page,
        int size,
        String sort,
        String status,
        String search
    ) {
        UUID companyId = clientAccessService.getRequiredReadableCompanyId();
        PageRequest pageRequest = PageRequest.of(
            normalizePage(page),
            normalizeSize(size),
            parseSort(sort)
        );

        Page<Client> resultPage = clientRepository.findPageByCompanyIdAndFilters(
            companyId,
            parseOptionalStatus(status),
            normalizeSearch(search),
            pageRequest
        );

        return new PagedResponse<>(
            resultPage.getContent().stream().map(this::toResponse).toList(),
            resultPage.getNumber(),
            resultPage.getSize(),
            resultPage.getTotalElements(),
            resultPage.getTotalPages()
        );
    }

    @Transactional(readOnly = true)
    public ClientResponse getClient(UUID clientId) {
        UUID companyId = clientAccessService.getRequiredReadableCompanyId();
        return toResponse(getRequiredClient(clientId, companyId));
    }

    @Transactional
    public ClientResponse createClient(UpsertClientRequest request) {
        UUID companyId = clientAccessService.getRequiredWritableCompanyId();
        Company company = companyRepository.findByIdAndDeletedAtIsNull(companyId)
            .orElseThrow(() -> new ResourceNotFoundException("Company not found."));

        Client client = Client.create(
            company,
            normalizeRequired(request.name()),
            normalizeOptional(request.pib()),
            normalizeOptional(request.mb()),
            normalizeOptional(request.email()),
            normalizeOptional(request.phone()),
            normalizeOptional(request.address()),
            normalizeOptional(request.city()),
            normalizeOptional(request.postalCode()),
            normalizeOptional(request.country()),
            parseRequiredStatus(request.status())
        );

        return toResponse(clientRepository.save(client));
    }

    @Transactional
    public ClientResponse updateClient(UUID clientId, UpsertClientRequest request) {
        UUID companyId = clientAccessService.getRequiredWritableCompanyId();
        Client client = getRequiredClient(clientId, companyId);

        client.update(
            normalizeRequired(request.name()),
            normalizeOptional(request.pib()),
            normalizeOptional(request.mb()),
            normalizeOptional(request.email()),
            normalizeOptional(request.phone()),
            normalizeOptional(request.address()),
            normalizeOptional(request.city()),
            normalizeOptional(request.postalCode()),
            normalizeOptional(request.country()),
            parseRequiredStatus(request.status())
        );

        return toResponse(clientRepository.save(client));
    }

    @Transactional
    public void deleteClient(UUID clientId) {
        UUID companyId = clientAccessService.getRequiredWritableCompanyId();
        Client client = getRequiredClient(clientId, companyId);
        client.softDelete();
        clientRepository.save(client);
    }

    private Client getRequiredClient(UUID clientId, UUID companyId) {
        return clientRepository.findByIdAndCompanyIdAndDeletedAtIsNull(clientId, companyId)
            .orElseThrow(() -> new ResourceNotFoundException("Client not found."));
    }

    private ClientResponse toResponse(Client client) {
        return new ClientResponse(
            client.getId(),
            client.getName(),
            client.getPib(),
            client.getMb(),
            client.getEmail(),
            client.getPhone(),
            client.getAddress(),
            client.getCity(),
            client.getPostalCode(),
            client.getCountry(),
            client.getStatus().name()
        );
    }

    private int normalizePage(int page) {
        return Math.max(page, 0);
    }

    private int normalizeSize(int size) {
        if (size <= 0) {
            return DEFAULT_PAGE_SIZE;
        }

        return Math.min(size, MAX_PAGE_SIZE);
    }

    private Sort parseSort(String sort) {
        String requestedSort = sort == null || sort.isBlank() ? "name,asc" : sort.trim();
        String[] parts = requestedSort.split(",", 2);
        String field = parts[0].trim();
        String mappedField = SORT_FIELD_MAPPING.get(field);

        if (mappedField == null) {
            throw new ApiBadRequestException("Invalid sort field.");
        }

        Sort.Direction direction = Sort.Direction.ASC;

        if (parts.length == 2) {
            try {
                direction = Sort.Direction.fromString(parts[1].trim());
            } catch (IllegalArgumentException exception) {
                throw new ApiBadRequestException("Invalid sort direction.");
            }
        }

        return Sort.by(direction, mappedField);
    }

    private ClientStatus parseOptionalStatus(String status) {
        if (status == null || status.isBlank()) {
            return null;
        }

        return parseRequiredStatus(status);
    }

    private ClientStatus parseRequiredStatus(String status) {
        try {
            return ClientStatus.valueOf(status.trim().toUpperCase());
        } catch (IllegalArgumentException exception) {
            throw new ApiBadRequestException("Invalid client status.");
        }
    }

    private String normalizeRequired(String value) {
        String normalized = normalizeOptional(value);

        if (normalized == null) {
            throw new ApiBadRequestException("Required client field is missing.");
        }

        return normalized;
    }

    private String normalizeOptional(String value) {
        if (value == null) {
            return null;
        }

        String normalized = value.trim();
        return normalized.isEmpty() ? null : normalized;
    }

    private String normalizeSearch(String search) {
        return normalizeOptional(search);
    }
}
