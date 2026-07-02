package com.kathirha.web;

import com.kathirha.security.AppUserDetails;
import com.kathirha.service.AccountService;
import com.kathirha.service.AuthService;
import com.kathirha.web.dto.Requests;
import com.kathirha.web.dto.Views;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService auth;
    private final AccountService accounts;

    public AuthController(AuthService auth, AccountService accounts) {
        this.auth = auth;
        this.accounts = accounts;
    }

    @PostMapping("/register")
    public AuthService.RegisterResult register(@Valid @RequestBody Requests.RegisterRequest req) {
        return auth.register(req.phone(), req.displayName(), req.email(), req.password());
    }

    @PostMapping("/login")
    public Views.AuthResponse login(@Valid @RequestBody Requests.LoginRequest req) {
        return auth.login(req.phone(), req.password());
    }

    @PostMapping("/verify-otp")
    public Views.AuthResponse verifyOtp(@Valid @RequestBody Requests.VerifyOtpRequest req) {
        return auth.verifyOtp(req.phone(), req.code());
    }

    @GetMapping("/me")
    public Views.UserView me(@AuthenticationPrincipal AppUserDetails principal) {
        return accounts.meView(accounts.require(principal));
    }

    /** Point the current account at a real WhatsApp number (for live Twilio testing). */
    @PostMapping("/phone")
    public Views.AuthResponse updatePhone(@AuthenticationPrincipal AppUserDetails principal,
                                          @Valid @RequestBody Requests.UpdatePhoneRequest req) {
        return auth.updatePhone(accounts.require(principal), req.phone());
    }
}
