package w.mazebank.services;

import org.modelmapper.ModelMapper;
import org.modelmapper.TypeMap;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import w.mazebank.enums.AccountType;
import w.mazebank.enums.RoleType;
import w.mazebank.enums.TransactionType;
import w.mazebank.exceptions.*;
import w.mazebank.models.Account;
import w.mazebank.models.Transaction;
import w.mazebank.models.User;
import w.mazebank.models.requests.AccountPatchRequest;
import w.mazebank.models.requests.AccountRequest;
import w.mazebank.models.responses.AccountResponse;
import w.mazebank.models.responses.IbanResponse;
import w.mazebank.models.responses.TransactionResponse;
import w.mazebank.models.responses.UserResponse;
import w.mazebank.repositories.AccountRepository;
import w.mazebank.repositories.TransactionRepository;
import w.mazebank.utils.IbanGenerator;

import java.util.ArrayList;
import java.util.List;

@Service
public class AccountServiceJpa extends BaseServiceJpa {
    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private TransactionServiceJpa transactionServiceJpa;

    @Autowired
    private UserServiceJpa userServiceJpa;

    @Autowired
    private TransactionRepository transactionRepository;

    private final ModelMapper mapper = new ModelMapper();

    public void addAccount(Account account) {
        accountRepository.save(account);
    }

    public List<AccountResponse> getAllAccounts(int pageNumber, int pageSize, String sort, String search) {
        List<Account> accounts = findAllPaginationAndSort(pageNumber, pageSize, sort, search, accountRepository);

        // parse users to user responses
        List<AccountResponse> accountResponses = new ArrayList<>();
        for (Account account : accounts) {

            // if account is bankaccount, skip
            if (account.getIban().equals("NL01INHO0000000001")) continue;

            AccountResponse accountResponse = AccountResponse.builder()
                .id(account.getId())
                .accountType(account.getAccountType().getValue())
                .iban(account.getIban())
                // get user response
                .user(UserResponse.builder()
                    .id(account.getUser().getId())
                    .firstName(account.getUser().getFirstName())
                    .lastName(account.getUser().getLastName())
                    .build())
                .balance(account.getBalance())
                .absoluteLimit(account.getAbsoluteLimit())
                .active(account.isActive())
                .timestamp(account.getCreatedAt())
                .build();
            accountResponses.add(accountResponse);
        }

        return accountResponses;
    }


    public Account getAccountById(Long id) throws AccountNotFoundException {
        return accountRepository.findById(id)
            .orElseThrow(() -> new AccountNotFoundException("Account with id: " + id + " not found"));
    }

    public Account getAccountByIban(String iban) throws AccountNotFoundException {
        return accountRepository.findByIban(iban)
            .orElseThrow(() -> new AccountNotFoundException("Account with iban: " + iban + " not found"));
    }

    public Account getAccountAndValidate(Long accountId, User user) throws AccountNotFoundException {
        Account account = getAccountById(accountId);
        validateAccountOwner(user, account);
        return account;
    }

    public AccountResponse createAccount(AccountRequest body) throws UserNotFoundException, AccountCreationLimitReachedException {
        User user = userServiceJpa.getUserById(body.getUserId());
        Account account = Account.builder()
            .accountType(body.getAccountType())
            .iban(IbanGenerator.generate())
            .isActive(body.isActive())
            .user(user)
            .absoluteLimit(body.getAbsoluteLimit())
            .balance(0.0)
            .build();

        List<Account> accounts = user.getAccounts();
        int checkingAccounts = (int) accounts.stream().filter(a -> a.getAccountType() == AccountType.CHECKING).count();
        int savingsAccounts = (int) accounts.stream().filter(a -> a.getAccountType() == AccountType.SAVINGS).count();

        // check if account creation limit has been reached
        if (account.getAccountType() == AccountType.SAVINGS && savingsAccounts >= 1)
            throw new AccountCreationLimitReachedException("Savings account creation limit reached");
        if (account.getAccountType() == AccountType.CHECKING && checkingAccounts >= 1)
            throw new AccountCreationLimitReachedException("Checking account creation limit reached");

        // save account to database
        Account newAccount = accountRepository.save(account);

        // map account to account response
        TypeMap<Account, AccountResponse> propertyMapper = mapper.typeMap(Account.class, AccountResponse.class);
        // cast accountType to integer with AccountResponse::setAccountType
        propertyMapper.addMapping(Account::getAccountType, AccountResponse::setAccountType);
        return mapper.map(newAccount, AccountResponse.class);
    }

    public AccountResponse updateAccount(long id, AccountPatchRequest body) throws AccountNotFoundException {
        Account account = getAccountById(id);

        // if the account is a bank account, throw exception
        if (account.getIban().equals("NL01INHO0000000001"))
            throw new UnauthorizedAccountAccessException("Unauthorized access to bank account");

        if (body.getAbsoluteLimit() != null) {
            account.setAbsoluteLimit(body.getAbsoluteLimit());
        }

        Account updatedAccount = accountRepository.save(account);

        // map account to account response
        TypeMap<Account, AccountResponse> propertyMapper = mapper.typeMap(Account.class, AccountResponse.class);
        // cast accountType to integer with AccountResponse::setAccountType
        propertyMapper.addMapping(Account::getAccountType, AccountResponse::setAccountType);
        return mapper.map(updatedAccount, AccountResponse.class);
    }

    public TransactionResponse deposit(Long accountId, double amount, User userDetails) throws AccountNotFoundException, InvalidAccountTypeException, TransactionFailedException {
        // get account from database and validate owner
        Account account = getAccountAndValidate(accountId, userDetails);

        // use transaction service to deposit money
        return transactionServiceJpa.atmAction(account, amount, TransactionType.DEPOSIT, userDetails);
    }


    public TransactionResponse withdraw(Long accountId, double amount, User userDetails) throws AccountNotFoundException, InvalidAccountTypeException, TransactionFailedException {
        // get account from database and validate owner
        Account account = getAccountAndValidate(accountId, userDetails);


        // CHECKS:
        // check if it is a checking account

        // use transaction service to withdraw money
        return transactionServiceJpa.atmAction(account, amount, TransactionType.WITHDRAWAL, userDetails);
    }

    private static void verifySufficientFunds(double amount, Account account) {
        // check if account has enough money
        if (account.getBalance() < amount) {
            throw new InsufficientFundsException("Not enough funds in account");
        }
    }

    private void validateAccountOwner(User user, Account account) {

        System.out.println(user.getId());
        System.out.println(account.getUser().getId());

        // check if current user is the same as account owner or if current user is an employee
        if (user.getRole() != RoleType.EMPLOYEE && user.getId() != account.getUser().getId()) {
            throw new UnauthorizedAccountAccessException("You are not authorized to access this account");
        }
    }

    public Account lockAccount(Long id) throws AccountNotFoundException, AccountLockOrUnlockStatusException {

        if(id == 1) throw new UnauthorizedAccountAccessException("Unauthorized access to bank account");
        if (!getAccountById(id).isActive()) {
            throw new AccountLockOrUnlockStatusException("Account is already locked");
        }
        Account account = getAccountById(id);
        account.setActive(false);

        accountRepository.save(account);
        return account;
    }

    public Account unlockAccount(Long id) throws AccountNotFoundException, AccountLockOrUnlockStatusException {
        if(id == 1) throw new UnauthorizedAccountAccessException("Unauthorized access to bank account");

        if (getAccountById(id).isActive()) {
            throw new AccountLockOrUnlockStatusException("Account is already unlocked");
        }

        Account account = getAccountById(id);
        account.setActive(true);

        accountRepository.save(account);
        return account;
    }

    public List<IbanResponse> getAccountsByName(String name) {
        String[] names = name.split(" ");
        return names.length == 2
            ? getAccountsByFirstNameAndLastName(names[0], names[1])
            : getAccountsByOneName(name);
    }

    private List<IbanResponse> getAccountsByOneName(String name) {
        List<Account> accounts = accountRepository.findAccountsByOneName(name);
        List<IbanResponse> ibanResponses = new ArrayList<>();
        for (Account account : accounts) {
            ibanResponses.add(IbanResponse.builder()
                .iban(account.getIban())
                .firstName(account.getUser().getFirstName())
                .lastName(account.getUser().getLastName())
                .build());
        }
        return ibanResponses;
    }

    private List<IbanResponse> getAccountsByFirstNameAndLastName(String firstName, String lastName) {
        List<Account> accounts = accountRepository.findAccountsByFirstNameAndLastName(firstName, lastName);
        List<IbanResponse> ibanResponses = new ArrayList<>();
        for (Account account : accounts) {
            ibanResponses.add(IbanResponse.builder()
                .iban(account.getIban())
                .firstName(account.getUser().getFirstName())
                .lastName(account.getUser().getLastName())
                .build());
        }
        return ibanResponses;
    }

    public List<TransactionResponse> getTransactionsFromAccount(int offset, int limit, String sort, User user, Long accountId) throws AccountNotFoundException {
        if(accountId == 1) throw new UnauthorizedAccountAccessException("Unauthorized access to bank account");

        getAccountAndValidate(accountId, user);

        Sort sortObject = Sort.by(Sort.Direction.fromString(sort), "timestamp");
        Pageable pageable = PageRequest.of(offset, limit, sortObject);

        List<Transaction> transactions = transactionRepository.findBySenderIdOrReceiverId(accountId, accountId, pageable);

        // parse transactions to transaction responses
        List<TransactionResponse> transactionResponses = new ArrayList<>();
        for (Transaction transaction : transactions) {
            TransactionResponse response = TransactionResponse.builder()
                .id(transaction.getId())
                .amount(transaction.getAmount())
                .description(transaction.getDescription())
                .sender(transaction.getSender() != null ? transaction.getSender().getIban() : null)
                .receiver(transaction.getReceiver() != null ? transaction.getReceiver().getIban() : null)
                .type(transaction.getTransactionType().name())
                .timestamp(transaction.getTimestamp().toString())
                .build();
            transactionResponses.add(response);
        }
        return transactionResponses;
    }
}