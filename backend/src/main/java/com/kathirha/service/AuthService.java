package com.kathirha.service;

import com.kathirha.domain.IntegrityStatus;
import com.kathirha.domain.MessageCategory;
import com.kathirha.domain.Role;
import com.kathirha.domain.User;
import com.kathirha.repository.UserRepository;
import com.kathirha.security.JwtService;
import com.kathirha.service.email.EmailService;
import com.kathirha.service.whatsapp.WhatsAppService;
import com.kathirha.web.ApiExceptions;
import com.kathirha.web.dto.Views;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;

@Service
public class AuthService {

    private final UserRepository users;
    private final PasswordEncoder encoder;
    private final JwtService jwt;
    private final WhatsAppService whatsapp;
    private final AccountService accounts;
    private final EmailService emailService;
    private final SecureRandom random = new SecureRandom();

    public AuthService(UserRepository users, PasswordEncoder encoder, JwtService jwt,
                       WhatsAppService whatsapp, AccountService accounts, EmailService emailService) {
        this.users = users;
        this.encoder = encoder;
        this.jwt = jwt;
        this.whatsapp = whatsapp;
        this.accounts = accounts;
        this.emailService = emailService;
    }

    public record RegisterResult(String token, Views.UserView user) {}

    @Transactional
    public RegisterResult register(String phone, String displayName, String email, String password) {
        if (users.existsByPhone(phone)) {
            throw new ApiExceptions.BadRequestException("That phone number is already registered");
        }
        User u = new User();
        u.setPhone(phone);
        u.setDisplayName(displayName);
        u.setEmail(email);
        u.setPasswordHash(encoder.encode(password));
        u.setRole(Role.USER);
        u.setIntegrityStatus(IntegrityStatus.NEW);
        String otp = String.format("%04d", random.nextInt(10000));
        u.setOtpCode(otp);
        users.save(u);

        whatsapp.send(u, MessageCategory.OTP, "Your Kathirha verification code is " + otp + " 🔐", null);
        whatsapp.send(u, MessageCategory.WELCOME,
                "Ahlan " + (displayName == null ? "" : displayName) + "! Welcome to Kathirha 🌱 "
                        + "Link your bank to get your AI savings coach and join the fairness leaderboard.", null);
        emailService.sendWelcome(u);

        // NOTE: the OTP is delivered via the (mock) WhatsApp channel only — never returned in the
        // API response, so phone verification can't be bypassed by reading the HTTP body.
        String token = jwt.generateToken(u);
        return new RegisterResult(token, accounts.meView(u));
    }

    public Views.AuthResponse login(String phone, String password) {
        User u = users.findByPhone(phone)
                .orElseThrow(() -> new ApiExceptions.UnauthorizedException("Invalid phone or password"));
        if (!encoder.matches(password, u.getPasswordHash())) {
            throw new ApiExceptions.UnauthorizedException("Invalid phone or password");
        }
        String token = jwt.generateToken(u);
        return new Views.AuthResponse(token, accounts.meView(u));
    }

    /** Update the current user's WhatsApp number (the login id). Re-issues a token since phone is the JWT subject. */
    @Transactional
    public Views.AuthResponse updatePhone(User user, String phone) {
        String p = phone == null ? "" : phone.trim();
        if (p.isBlank()) throw new ApiExceptions.BadRequestException("Phone number is required");
        users.findByPhone(p).ifPresent(other -> {
            if (!other.getId().equals(user.getId())) {
                throw new ApiExceptions.BadRequestException("That number is already in use");
            }
        });
        user.setPhone(p);
        users.save(user);
        return new Views.AuthResponse(jwt.generateToken(user), accounts.meView(user));
    }

    @Transactional
    public Views.AuthResponse verifyOtp(String phone, String code) {
        User u = users.findByPhone(phone)
                .orElseThrow(() -> new ApiExceptions.NotFoundException("Account not found"));
        if (u.getOtpCode() == null || !u.getOtpCode().equals(code)) {
            throw new ApiExceptions.BadRequestException("Incorrect verification code");
        }
        u.setPhoneVerified(true);
        u.setOtpCode(null);
        users.save(u);
        return new Views.AuthResponse(jwt.generateToken(u), accounts.meView(u));
    }
}
