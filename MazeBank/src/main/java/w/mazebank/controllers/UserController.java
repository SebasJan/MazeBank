package w.mazebank.controllers;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import w.mazebank.exceptions.UserNotFoundException;
import w.mazebank.models.User;
import w.mazebank.utils.ResponseHandler;
import w.mazebank.models.responses.UserResponse;
import w.mazebank.services.UserServiceJpa;

import java.util.List;

@RestController
@RequestMapping("/users")
public class UserController {
    @Autowired
    private UserServiceJpa userService;

    @GetMapping("/{id}")
    public ResponseEntity<User> getUserById(@PathVariable Long id) throws UserNotFoundException {
        User user = userService.getUserById(id);
        return ResponseEntity.ok(user);
    }

    @PatchMapping("/{id}")
    public ResponseEntity<User> patchUserById(@PathVariable long id) throws UserNotFoundException {
        User user = userService.patchUserById(id);
        return ResponseEntity.ok(user);
    }

    // GET/users/{userId}/accounts
    @GetMapping("/{userId}/accounts")
    public ResponseEntity<Object> getAccountsByUserId(@PathVariable Long userId) throws UserNotFoundException {
        List<UserResponse> userResponses = userService.getAccountsByUserId(userId); 
        return ResponseEntity.ok(userResponses);
    }

    @GetMapping
    public ResponseEntity<Object> getAllUsers() {
        return ResponseEntity.ok(userService.getAllUsers());
    }
}