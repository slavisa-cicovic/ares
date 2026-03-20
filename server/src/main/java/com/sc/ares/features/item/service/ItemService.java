package com.sc.ares.features.item.service;

import com.sc.ares.common.api.ApiBadRequestException;
import com.sc.ares.common.api.BusinessConflictException;
import com.sc.ares.common.api.PagedResponse;
import com.sc.ares.common.api.ResourceNotFoundException;
import com.sc.ares.features.company.entity.Company;
import com.sc.ares.features.company.repository.CompanyRepository;
import com.sc.ares.features.item.dto.ItemResponse;
import com.sc.ares.features.item.dto.UpsertItemRequest;
import com.sc.ares.features.item.entity.Item;
import com.sc.ares.features.item.entity.ItemStatus;
import com.sc.ares.features.item.repository.ItemRepository;
import com.sc.ares.features.user.entity.AppUser;
import com.sc.ares.features.user.repository.UserRepository;
import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ItemService {

    private static final int DEFAULT_PAGE_SIZE = 20;
    private static final int MAX_PAGE_SIZE = 100;
    private static final Map<String, String> SORT_FIELD_MAPPING = Map.of(
        "name", "name",
        "code", "code",
        "status", "status",
        "price", "price",
        "createdAt", "createdAt"
    );

    private final ItemRepository itemRepository;
    private final CompanyRepository companyRepository;
    private final UserRepository userRepository;
    private final ItemAccessService itemAccessService;

    public ItemService(
        ItemRepository itemRepository,
        CompanyRepository companyRepository,
        UserRepository userRepository,
        ItemAccessService itemAccessService
    ) {
        this.itemRepository = itemRepository;
        this.companyRepository = companyRepository;
        this.userRepository = userRepository;
        this.itemAccessService = itemAccessService;
    }

    @Transactional(readOnly = true)
    public PagedResponse<ItemResponse> listItems(
        int page,
        int size,
        String sort,
        String status,
        String search
    ) {
        UUID companyId = itemAccessService.getRequiredReadableCompanyId();
        PageRequest pageRequest = PageRequest.of(
            normalizePage(page),
            normalizeSize(size),
            parseSort(sort)
        );

        Page<Item> resultPage = itemRepository.findPageByCompanyIdAndFilters(
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
    public ItemResponse getItem(UUID itemId) {
        UUID companyId = itemAccessService.getRequiredReadableCompanyId();
        return toResponse(getRequiredItem(itemId, companyId));
    }

    @Transactional
    public ItemResponse createItem(UpsertItemRequest request) {
        UUID companyId = itemAccessService.getRequiredWritableCompanyId();
        Company company = companyRepository.findByIdAndDeletedAtIsNull(companyId)
            .orElseThrow(() -> new ResourceNotFoundException("Company not found."));
        AppUser createdByUser = getRequiredCurrentUser(companyId);

        String normalizedCode = normalizeOptional(request.code());
        ensureCodeAvailableForCreate(companyId, normalizedCode);

        Item item = Item.create(
            company,
            createdByUser,
            normalizeRequired(request.name()),
            normalizedCode,
            normalizeOptional(request.description()),
            normalizeRequired(request.unit()),
            normalizeMoney(request.price()),
            normalizeTaxRate(request.taxRate()),
            parseRequiredStatus(request.status())
        );

        return toResponse(itemRepository.save(item));
    }

    @Transactional
    public ItemResponse updateItem(UUID itemId, UpsertItemRequest request) {
        UUID companyId = itemAccessService.getRequiredWritableCompanyId();
        Item item = getRequiredItem(itemId, companyId);
        itemAccessService.ensureCanModify(item);

        String normalizedCode = normalizeOptional(request.code());
        ensureCodeAvailableForUpdate(companyId, itemId, normalizedCode);

        item.update(
            normalizeRequired(request.name()),
            normalizedCode,
            normalizeOptional(request.description()),
            normalizeRequired(request.unit()),
            normalizeMoney(request.price()),
            normalizeTaxRate(request.taxRate()),
            parseRequiredStatus(request.status())
        );

        return toResponse(itemRepository.save(item));
    }

    @Transactional
    public void deleteItem(UUID itemId) {
        UUID companyId = itemAccessService.getRequiredWritableCompanyId();
        Item item = getRequiredItem(itemId, companyId);
        itemAccessService.ensureCanModify(item);
        item.softDelete();
        itemRepository.save(item);
    }

    private Item getRequiredItem(UUID itemId, UUID companyId) {
        return itemRepository.findByIdAndCompanyIdAndDeletedAtIsNull(itemId, companyId)
            .orElseThrow(() -> new ResourceNotFoundException("Item not found."));
    }

    private AppUser getRequiredCurrentUser(UUID companyId) {
        UUID userId = itemAccessService.getRequiredCreatePrincipal().userId();

        return userRepository.findByIdAndCompanyIdAndDeletedAtIsNull(userId, companyId)
            .orElseThrow(() -> new ResourceNotFoundException("User not found."));
    }

    private void ensureCodeAvailableForCreate(UUID companyId, String code) {
        if (code != null && itemRepository.existsByCompanyIdAndCodeIgnoreCaseAndDeletedAtIsNull(companyId, code)) {
            throw new BusinessConflictException("Item code already exists.");
        }
    }

    private void ensureCodeAvailableForUpdate(UUID companyId, UUID itemId, String code) {
        if (code != null && itemRepository.existsByCompanyIdAndCodeIgnoreCaseAndDeletedAtIsNullAndIdNot(companyId, code, itemId)) {
            throw new BusinessConflictException("Item code already exists.");
        }
    }

    private ItemResponse toResponse(Item item) {
        return new ItemResponse(
            item.getId(),
            item.getName(),
            item.getCode(),
            item.getDescription(),
            item.getUnit(),
            item.getPrice(),
            item.getTaxRate(),
            item.getStatus().name()
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

    private ItemStatus parseOptionalStatus(String status) {
        if (status == null || status.isBlank()) {
            return null;
        }

        return parseRequiredStatus(status);
    }

    private ItemStatus parseRequiredStatus(String status) {
        try {
            return ItemStatus.valueOf(status.trim().toUpperCase());
        } catch (IllegalArgumentException exception) {
            throw new ApiBadRequestException("Invalid item status.");
        }
    }

    private String normalizeRequired(String value) {
        String normalized = normalizeOptional(value);

        if (normalized == null) {
            throw new ApiBadRequestException("Required item field is missing.");
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
        String normalized = normalizeOptional(search);
        return normalized == null ? "" : normalized;
    }

    private BigDecimal normalizeMoney(BigDecimal value) {
        if (value == null) {
            throw new ApiBadRequestException("Required item field is missing.");
        }

        return value.stripTrailingZeros().scale() < 0 ? value.setScale(0) : value;
    }

    private BigDecimal normalizeTaxRate(BigDecimal value) {
        BigDecimal normalized = normalizeMoney(value);

        if (normalized.compareTo(new BigDecimal("100.00")) > 0) {
            throw new ApiBadRequestException("Invalid tax rate.");
        }

        return normalized;
    }
}
