package com.sc.ares.features.item.controller;

import com.sc.ares.common.api.PagedResponse;
import com.sc.ares.features.item.dto.ItemResponse;
import com.sc.ares.features.item.dto.UpsertItemRequest;
import com.sc.ares.features.item.service.ItemService;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/items")
public class ItemController {

    private final ItemService itemService;

    public ItemController(ItemService itemService) {
        this.itemService = itemService;
    }

    @GetMapping
    public PagedResponse<ItemResponse> listItems(
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "20") int size,
        @RequestParam(defaultValue = "name,asc") String sort,
        @RequestParam(required = false) String status,
        @RequestParam(required = false) String search
    ) {
        return itemService.listItems(page, size, sort, status, search);
    }

    @GetMapping("/{itemId}")
    public ItemResponse getItem(@PathVariable UUID itemId) {
        return itemService.getItem(itemId);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ItemResponse createItem(@Valid @RequestBody UpsertItemRequest request) {
        return itemService.createItem(request);
    }

    @PutMapping("/{itemId}")
    public ItemResponse updateItem(@PathVariable UUID itemId, @Valid @RequestBody UpsertItemRequest request) {
        return itemService.updateItem(itemId, request);
    }

    @DeleteMapping("/{itemId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteItem(@PathVariable UUID itemId) {
        itemService.deleteItem(itemId);
    }
}
