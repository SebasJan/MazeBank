package w.mazebank.cucumberglue;


import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import org.apache.hc.client5.http.classic.HttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClientBuilder;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.client.RestTemplate;
import w.mazebank.enums.RoleType;
import w.mazebank.models.User;
import w.mazebank.models.requests.UserPatchRequest;
import w.mazebank.services.JwtService;

import java.time.LocalDate;
import java.time.LocalDateTime;

public class UsersStepDefinitions extends BaseStepDefinitions{

    private String token;

    private JwtService jwtService;
    private PasswordEncoder passwordEncoder;


    @Given("^I have a valid token for role \"([^\"]*)\"$")
    public void Test(String role){
        switch (role) {
            case "customer" -> token = VALID_TOKEN_USER;
            case "employee" -> token = VALID_TOKEN_ADMIN;
            default -> throw new IllegalArgumentException("No such role");
        }
    }

    @When("I call the users endpoint")
    public void iCallTheUsersEndpoint() {
        // User user1 = new User(2, "user1@example.com", 123456789, "John", "Doe", "$2a$10$CHn7sYgipDQqx4yvV.X59.c07V9sTDiGmKfnlEBz48yznkDm7o6a.", "1234567890", RoleType.EMPLOYEE, LocalDate.now().minusYears(25), LocalDateTime.now(), 1000, 100, false, null);
        User user3 = new User(4, "user3@example.com", 456123789, "Jim", "John", "$2a$10$CHn7sYgipDQqx4yvV.X59.c07V9sTDiGmKfnlEBz48yznkDm7o6a.", "0987654321", RoleType.EMPLOYEE, LocalDate.now().minusYears(30), LocalDateTime.now(), 5000, 200, false, null);

        // create good token
        jwtService = new JwtService();
        token = jwtService.generateToken(user3);

        httpHeaders.clear();
        httpHeaders.add("Authorization", "Bearer " + token);
        httpHeaders.add("Content-Type", "application/json");

        // Create the HTTP entity with the request body and headers
        HttpEntity<Object> requestEntity = new HttpEntity<>(null, httpHeaders);

        // Send the request
        RestTemplate restTemplate = new RestTemplate();
        lastResponse = restTemplate.exchange(
            "http://localhost:" + port + "/users",
            HttpMethod.GET, // Adjust the HTTP method if necessary
            requestEntity,
            String.class
        );
    }

    @Then("the result is a list of user of size {int}")
    public void theResultIsAListOfUserOfSize(int size) {
        assert lastResponse.getBody() != null;
        System.out.println(lastResponse.getBody());
    }

    @When("I call the users endpoint with a patch request")
    public void iCallTheUsersEndpointWithAPatchRequest() {
        User user3 = new User(4, "user3@example.com", 456123789, "Jim", "John", "$2a$10$CHn7sYgipDQqx4yvV.X59.c07V9sTDiGmKfnlEBz48yznkDm7o6a.", "0987654321", RoleType.EMPLOYEE, LocalDate.now().minusYears(30), LocalDateTime.now(), 5000, 200, false, null);

        // create good token
        jwtService = new JwtService();
        token = jwtService.generateToken(user3);

        httpHeaders.clear();
        httpHeaders.add("Authorization", "Bearer " + token);
        httpHeaders.add("Content-Type", "application/json");

        // allow patch requests
        httpHeaders.add("Access-Control-Allow-Methods", "PATCH");

        // create userPatchRequest
        UserPatchRequest userPatchRequest = new UserPatchRequest();
        userPatchRequest.setDayLimit(10000.00);


        // Create the HTTP entity with the request body and headers
        HttpEntity<Object> requestEntity = new HttpEntity<>(userPatchRequest, httpHeaders);

        // Send the request
        HttpClient client = HttpClientBuilder.create().build();

        RestTemplate restTemplate = new RestTemplate();
        restTemplate.setRequestFactory(new HttpComponentsClientHttpRequestFactory(client));
        lastResponse = restTemplate.exchange(
            "http://localhost:" + port + "/users/2",
            HttpMethod.PATCH, // Adjust the HTTP method if necessary
            requestEntity,
            String.class
        );
    }


    @Then("the result is a user with a daylimit of {int}")
    public void theResultIsAUserWithADaylimitOf(int expectedDayLimit) {
        assert lastResponse.getBody() != null;
        System.out.println(lastResponse.getBody());
    }
}