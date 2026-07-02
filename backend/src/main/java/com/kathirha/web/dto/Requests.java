package com.kathirha.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;

/** Request bodies (records) with bean-validation. */
public final class Requests {
    private Requests() {}

    public record RegisterRequest(
            @NotBlank String phone, String displayName, String email,
            @NotBlank @Size(min = 6, message = "must be at least 6 characters") String password) {}

    public record LoginRequest(@NotBlank String phone, @NotBlank String password) {}

    public record VerifyOtpRequest(@NotBlank String phone, @NotBlank String code) {}

    public record CreateGoalRequest(
            @NotBlank String name,
            @NotNull @Positive BigDecimal targetAmount,
            @NotNull LocalDate targetDate) {}

    public record ContributeRequest(@NotNull @Positive BigDecimal amount) {}

    public record AnswerRequest(@NotNull Integer index) {}

    public record RescueRequest(@NotBlank String option) {}

    public record ReplyRequest(@NotBlank String body) {}

    public record UpdatePhoneRequest(@NotBlank String phone) {}

    public record BankLinkRequest(@NotBlank String institutionId) {}

    public record BankCompleteRequest(@NotBlank String requisitionId) {}

    public record ImportRequest(BigDecimal monthlyIncome, Integer months, String preset) {}
}
