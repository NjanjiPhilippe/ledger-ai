package com.np3.ledgerai.web.controller;

import com.np3.ledgerai.application.account.command.useCase.CreateAccountUseCase;
import com.np3.ledgerai.application.account.query.GetAccountBalanceQuery;
import com.np3.ledgerai.application.account.query.GetAccountQuery;
import com.np3.ledgerai.application.account.query.SearchAccountsQuery;
import com.np3.ledgerai.application.account.command.useCase.UpdateAccountUseCase;
import com.np3.ledgerai.domain.port.criteria.AccountSearchCriteria;
import com.np3.ledgerai.domain.port.criteria.PageRequest;
import com.np3.ledgerai.domain.valueobject.AccountId;
import com.np3.ledgerai.domain.valueobject.AccountType;
import com.np3.ledgerai.web.dto.PagedResponse;
import com.np3.ledgerai.web.dto.account.AccountResponse;
import com.np3.ledgerai.web.dto.account.BalanceResponse;
import com.np3.ledgerai.web.dto.account.CreateAccountRequest;
import com.np3.ledgerai.web.dto.account.UpdateAccountRequest;
import com.np3.ledgerai.web.mapper.AccountWebMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/accounts")
@RequiredArgsConstructor
public class AccountController {

    private final CreateAccountUseCase createAccountUseCase;
    private final UpdateAccountUseCase updateAccountUseCase;
    private final GetAccountQuery getAccountQuery;
    private final SearchAccountsQuery searchAccountsQuery;
    private final GetAccountBalanceQuery getAccountBalanceQuery;


    @PostMapping
    public ResponseEntity<AccountResponse> create(@RequestBody CreateAccountRequest request) {
        var account = createAccountUseCase.execute(AccountWebMapper.toCommand(request));
        return ResponseEntity.status(HttpStatus.CREATED).body(AccountWebMapper.toResponse(account));
    }

    @PutMapping("/{id}")
    public ResponseEntity<AccountResponse> update(@PathVariable UUID id, @RequestBody UpdateAccountRequest request) {
        var command = new UpdateAccountCommand(AccountId.of(id), request.name(), request.active());
        var account = updateAccountUseCase.execute(command);
        return ResponseEntity.ok(AccountWebMapper.toResponse(account));
    }

    @GetMapping("/{id}")
    public ResponseEntity<AccountResponse> get(@PathVariable UUID id) {
        return getAccountQuery.execute(AccountId.of(id))
                .map(AccountWebMapper::toResponse)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @GetMapping
    public ResponseEntity<PagedResponse<AccountResponse>> search(
            @RequestParam(required = false) AccountType type,
            @RequestParam(required = false) Boolean active,
            @RequestParam(required = false) String name,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        var criteria = new AccountSearchCriteria(type, active, name);
        var result = searchAccountsQuery.execute(criteria, new PageRequest(page, size));
        return ResponseEntity.ok(PagedResponse.from(result, AccountWebMapper::toResponse));
    }

    @GetMapping("/{id}/balance")
    public ResponseEntity<BalanceResponse> balance(@PathVariable UUID id) {
        var balance = getAccountBalanceQuery.execute(AccountId.of(id));
        return ResponseEntity.ok(new BalanceResponse(balance.amount(), balance.currency().getCurrencyCode()));
    }
}