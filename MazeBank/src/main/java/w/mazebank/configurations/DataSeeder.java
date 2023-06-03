package w.mazebank.configurations;


import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import w.mazebank.enums.AccountType;
import w.mazebank.enums.RoleType;
import w.mazebank.enums.TransactionType;
import w.mazebank.models.Account;
import w.mazebank.models.Transaction;
import w.mazebank.models.User;
import w.mazebank.services.AccountServiceJpa;
import w.mazebank.services.TransactionServiceJpa;
import w.mazebank.services.UserServiceJpa;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Component
public class DataSeeder implements ApplicationRunner {
    @Autowired
    private UserServiceJpa userService;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private AccountServiceJpa accountService;

    @Autowired
    private TransactionServiceJpa transactionService;

    @Override
    public void run(ApplicationArguments args) throws Exception {

        // Create some sample users
        User bank = new User(1, "info@mazebank.com", 123456784, "Maze", "Bank", passwordEncoder.encode("1234"), "1234567890", RoleType.EMPLOYEE, LocalDate.now().minusYears(25), LocalDateTime.now(), 5000.00, 5000.00, false, null);
        User user1 = new User(2, "user1@example.com", 123456789, "John", "Doe", passwordEncoder.encode("1234"), "1234567890", RoleType.CUSTOMER, LocalDate.now().minusYears(25), LocalDateTime.now(), 5000.00, 5000.00, false, null);
        User user2 = new User(3, "user2@example.com", 987654321, "Jane", "Smith", passwordEncoder.encode("1234"), "0987654321", RoleType.CUSTOMER, LocalDate.now().minusYears(30), LocalDateTime.now(), 5000.00, 5000.00, false, null);
        User user3 = new User(4, "user3@example.com", 456123789, "Jim", "John", passwordEncoder.encode("1234"), "0987654321", RoleType.EMPLOYEE, LocalDate.now().minusYears(30), LocalDateTime.now(), 5000.00, 5000.00, false, null);
        User user4 = new User(5, "user4@example.com", 456123759, "Tim", "Brad", passwordEncoder.encode("1234"), "0987654321", RoleType.CUSTOMER, LocalDate.now().minusYears(30), LocalDateTime.now(), 5000.00, 5000.00, false, null);


        userService.addUser(bank);
        userService.addUser(user1);
        userService.addUser(user2);
        userService.addUser(user3);
        userService.addUser(user4);

        // Create some sample accounts for user1
        Account bankAccount = new Account(1, "NL01INHO0000000001", AccountType.CHECKING, 1000000.0, bank, true, LocalDateTime.now(), 0, null, null);
        Account account1 = new Account(2, "NL76INHO0493458014", AccountType.CHECKING, 1000.0, user1, true, LocalDateTime.now(), -10, null, null);
        Account account2 = new Account(3, "NL45INHO0328598538", AccountType.SAVINGS, 5000.0, user1, true, LocalDateTime.now(), 0, null, null);

        accountService.addAccount(bankAccount);
        accountService.addAccount(account1);
        accountService.addAccount(account2);

        // Create some sample accounts for user2
        Account account3 = new Account(4, "NL76INHO0493458018", AccountType.CHECKING, 2000.0, user2, true, LocalDateTime.now(), -1500.0, null, null);
        Account account4 = new Account(5, "NL29INHO0165148974", AccountType.SAVINGS, 10000.0, user2, true, LocalDateTime.now(), -5000.0, null, null);

        Account account5 = new Account(6, "NL45INHO0328598536", AccountType.CHECKING, 2000.0, user3, true, LocalDateTime.now(), -1500.0, null, null);
        Account account6 = new Account(6, "NL76INHO0493458015", AccountType.SAVINGS, 2000.0, user3, true, LocalDateTime.now(), -1500.0, null, null);

        accountService.addAccount(account3);
        accountService.addAccount(account4);
        accountService.addAccount(account5);
        accountService.addAccount(account6);

        LocalDateTime date = LocalDateTime.of(2023, 5, 30, 10, 30, 0);
        LocalDateTime date2 = LocalDateTime.of(2023, 5, 31, 10, 30, 0);
        LocalDateTime date3 = LocalDateTime.of(2024, 5, 31, 10, 30, 0);
        LocalDateTime wrongDate = LocalDateTime.of(2022, 5, 31, 10, 30, 0);
        // Perform some transactions between the accounts
        transactionService.saveTransaction(new Transaction(1, "Transfer from account1 to account3", 500.0, user1, account1, account3, TransactionType.TRANSFER, date));
        transactionService.saveTransaction(new Transaction(6, "Transfer from account1 to account3", 500.0, user1, account1, account3, TransactionType.TRANSFER, date));
        transactionService.saveTransaction(new Transaction(5, "Transfer from account1 to account3", 500.0, user1, account1, account3, TransactionType.TRANSFER, date3));
        transactionService.saveTransaction(new Transaction(2, "Transfer from account2 to account4", 2000.0, user1, account2, account4, TransactionType.TRANSFER, date2));

        // perform transaction between account 1 and 2
        transactionService.saveTransaction(new Transaction(3, "Transfer from same user", 500.0, user1, account1, account2, TransactionType.TRANSFER, wrongDate));
        transactionService.saveTransaction(new Transaction(4, "Transfer from same user", 500.0, user1, account1, account2, TransactionType.TRANSFER, date3));
    }
}


