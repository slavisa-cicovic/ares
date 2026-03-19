package com.sc.ares.features.client.controller;

import com.sc.ares.common.api.PagedResponse;
import com.sc.ares.features.client.dto.ClientResponse;
import com.sc.ares.features.client.dto.UpsertClientRequest;
import com.sc.ares.features.client.service.ClientService;
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
@RequestMapping("/clients")
public class ClientController {

    private final ClientService clientService;

    public ClientController(ClientService clientService) {
        this.clientService = clientService;
    }

    @GetMapping
    public PagedResponse<ClientResponse> listClients(
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "20") int size,
        @RequestParam(defaultValue = "name,asc") String sort,
        @RequestParam(required = false) String status,
        @RequestParam(required = false) String search
    ) {
        return clientService.listClients(page, size, sort, status, search);
    }

    @GetMapping("/{clientId}")
    public ClientResponse getClient(@PathVariable UUID clientId) {
        return clientService.getClient(clientId);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ClientResponse createClient(@Valid @RequestBody UpsertClientRequest request) {
        return clientService.createClient(request);
    }

    @PutMapping("/{clientId}")
    public ClientResponse updateClient(@PathVariable UUID clientId, @Valid @RequestBody UpsertClientRequest request) {
        return clientService.updateClient(clientId, request);
    }

    @DeleteMapping("/{clientId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteClient(@PathVariable UUID clientId) {
        clientService.deleteClient(clientId);
    }
}
