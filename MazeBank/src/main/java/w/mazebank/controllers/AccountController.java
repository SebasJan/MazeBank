package w.mazebank.controllers;

import jakarta.validation.Valid;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.annotation.Secured;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import w.mazebank.exceptions.AccountCreationLimitReachedException;
import w.mazebank.exceptions.AccountNotFoundException;
import w.mazebank.exceptions.UserNotFoundException;
import w.mazebank.models.Account;
import w.mazebank.models.User;
import w.mazebank.models.requests.AccountPatchRequest;
import w.mazebank.models.requests.AccountRequest;
import w.mazebank.models.requests.AtmRequest;
import w.mazebank.models.responses.AccountResponse;
import w.mazebank.models.responses.LockedResponse;
import w.mazebank.models.responses.AtmResponse;
import w.mazebank.services.AccountServiceJpa;

import java.util.List;

@RestController
@RequestMapping("/accounts")
public class AccountController {
    @Autowired
    private AccountServiceJpa accountServiceJpa;

    private final ModelMapper mapper = new ModelMapper();

    @GetMapping
    @Secured("ROLE_EMPLOYEE")
    public ResponseEntity<Object> getAllAccounts(
        @RequestParam(defaultValue = "0") int offset,
        @RequestParam(defaultValue = "10") int limit,
        @RequestParam(defaultValue = "asc") String sort,
        @RequestParam(required = false) String search
    ){
    List<AccountResponse> accounts = accountServiceJpa.getAllAccounts(offset, limit, sort, search);
        return ResponseEntity.ok(accounts);
    }

    @GetMapping("/{accountId}")
    @Secured("ROLE_EMPLOYEE")
    public ResponseEntity<AccountResponse> getAccountById(
        @PathVariable Long accountId,
        @AuthenticationPrincipal User user
    ) throws AccountNotFoundException {
        Account account = accountServiceJpa.getAccountAndValidate(accountId, user);
        return ResponseEntity.ok(mapper.map(account, AccountResponse.class));
    }

    @PostMapping
    @Secured("ROLE_EMPLOYEE")
    public ResponseEntity<Object> createAccount(@RequestBody @Valid AccountRequest body) throws UserNotFoundException, AccountCreationLimitReachedException {
        AccountResponse account = accountServiceJpa.createAccount(body);
        return ResponseEntity.status(HttpStatus.CREATED).body(account);
    }

    @PatchMapping("/{accountId}")
    @Secured("ROLE_EMPLOYEE")
    public ResponseEntity<AccountResponse> updateAccount(
        @PathVariable Long accountId,
        @RequestBody @Valid AccountPatchRequest body
    ) throws AccountNotFoundException {
        AccountResponse account = accountServiceJpa.updateAccount(accountId, body);
        return ResponseEntity.ok(account);
    }

    @PostMapping("/{accountId}/deposit")
    public ResponseEntity<AtmResponse> deposit(
        @PathVariable("accountId") Long accountId,
        @RequestBody AtmRequest atmRequest,
        @AuthenticationPrincipal User user
    ) throws AccountNotFoundException {

        // create deposit transaction
        accountServiceJpa.deposit(accountId, atmRequest.getAmount(), user);

        // create transaction response and return it
        AtmResponse depositWithdrawResponse = AtmResponse.builder()
            .message("Deposit successful")
            .build();
        return ResponseEntity.ok(depositWithdrawResponse);
    }

    @PostMapping("/{accountId}/withdraw")
    public ResponseEntity<AtmResponse> withdraw(@PathVariable Long accountId, @RequestBody AtmRequest atmRequest, @AuthenticationPrincipal User user) throws AccountNotFoundException {
        // create withdraw transaction
        accountServiceJpa.withdraw(accountId, atmRequest.getAmount(), user);

        // create transaction response and return it
        AtmResponse depositWithdrawResponse = AtmResponse.builder()
            .message("Withdraw successful")
            .build();
        return ResponseEntity.ok(depositWithdrawResponse);
    }

    @PutMapping("/{id}/lock")
    @Secured("ROLE_EMPLOYEE")
    public ResponseEntity<LockedResponse> blockUser(@PathVariable Long id) throws AccountNotFoundException {
        accountServiceJpa.lockAccount(id);
        return ResponseEntity.ok(new LockedResponse(true));
    }

    @PutMapping("/{id}/unlock")
    @Secured("ROLE_EMPLOYEE")
    public ResponseEntity<LockedResponse> unblockUser(@PathVariable Long id) throws AccountNotFoundException {
        accountServiceJpa.unlockAccount(id);
        return ResponseEntity.ok(new LockedResponse(false));
    }
}
