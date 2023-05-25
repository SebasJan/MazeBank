package w.mazebank.controllers;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;
import w.mazebank.enums.AccountType;
import w.mazebank.enums.RoleType;
import w.mazebank.exceptions.UserNotFoundException;
import w.mazebank.models.Account;
import w.mazebank.models.User;
import w.mazebank.models.responses.AccountResponse;
import w.mazebank.repositories.UserRepository;
import w.mazebank.services.AccountServiceJpa;
import w.mazebank.services.AuthService;
import w.mazebank.services.JwtService;
import w.mazebank.services.UserServiceJpa;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(SpringExtension.class)
@WebMvcTest({AccountController.class})
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class AccountControllerTest {
    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private PasswordEncoder passwordEncoder;

    @MockBean
    private AccountServiceJpa accountService;

    @MockBean
    private AuthService authService;

    @MockBean
    private JwtService jwtService;

    @MockBean
    private UserRepository userRepository;

    @MockBean
    private UserServiceJpa userServiceJpa;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private User authUser;
    private String token;

    @BeforeAll
    void setUp() throws UserNotFoundException {
        authUser = new User(3, "user3@example.com", 456123789, "Jim", "John", passwordEncoder.encode("1234"), "0987654321", RoleType.EMPLOYEE, LocalDate.now().minusYears(30), LocalDateTime.now(), 5000, 200, false, null);

        when(userRepository.findByEmail(Mockito.anyString())).thenReturn(Optional.of(authUser));
        when(userServiceJpa.getUserById(Mockito.anyLong())).thenReturn(authUser);

        token = new JwtService().generateToken(authUser);
    }

    @Test
    void postAcccountShouldReturnStatusCreatedAndObject() throws Exception {
        Account account = Account.builder()
            .accountType(AccountType.CHECKING)
            .balance(0.0)
            .isActive(true)
            .absoluteLimit(0.0)
            .createdAt(null)
            .build();
        AccountResponse accountResponse = AccountResponse.builder()
            .id(1)
            .accountType(AccountType.CHECKING.getValue())
            .balance(0.0)
            .active(true)
            .iban("NL01INHO123456789")
            .absoluteLimit(0.0)
            .build();

        // when(userServiceJpa.getUserById(Mockito.anyLong())).thenReturn(authUser);
        when(accountService.createAccount(Mockito.any())).thenReturn(accountResponse);

        mockMvc.perform(post("/accounts")
                .header("Authorization", "Bearer " + token)
                .with(csrf())
                .with(user(authUser))
                .contentType("application/json")
                .content(objectMapper.writeValueAsString(account))
            ).andDo(print())
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.id").value(1))
            .andExpect(jsonPath("$.accountType").value(AccountType.CHECKING.getValue()))
            .andExpect(jsonPath("$.balance").value(0.0))
            .andExpect(jsonPath("$.active").value(true))
            .andExpect(jsonPath("$.iban").value("NL01INHO123456789"))
            .andExpect(jsonPath("$.absoluteLimit").value(0.0));
    }

    @Test
    void getAccountByIdShouldReturnStatusOkAndObject() throws Exception {
        User user = User.builder()
            .id(1)
            .firstName("John")
            .lastName("Doe")
            .role(RoleType.CUSTOMER)
            .blocked(false)
            .createdAt(LocalDateTime.now())
            .build();

        Account account = Account.builder()
            .id(1)
            .iban("NL01INHO0123456789")
            .accountType(AccountType.CHECKING)
            .balance(1000.0)
            .user(user)
            .isActive(true)
            .absoluteLimit(0.0)
            .createdAt(LocalDateTime.of(2023, 1, 1, 0, 0, 0))
            .build();

        // when(userRepository.findByEmail(Mockito.anyString())).thenReturn(Optional.of(authUser));

        when(accountService.getAccountAndValidate(Mockito.any(), Mockito.any()))
            .thenReturn(account);

        mockMvc.perform(get("/accounts/1")
                .header("Authorization", "Bearer " + token)
                .with(user(authUser))
            ).andDo(print())
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(1))
            .andExpect(jsonPath("$.iban").value("NL01INHO0123456789"))
            .andExpect(jsonPath("$.accountType").value(AccountType.CHECKING.getValue()))
            .andExpect(jsonPath("$.balance").value(1000.0))
            .andExpect(jsonPath("$.user.id").value(1))
            .andExpect(jsonPath("$.user.firstName").value("John"))
            .andExpect(jsonPath("$.user.lastName").value("Doe"))
            .andExpect(jsonPath("$.active").value(true))
            // .andExpect(jsonPath("$.createdAt").value("2023-01-01T00:00:00"))
            .andExpect(jsonPath("$.absoluteLimit").value(0.0));
    }
}