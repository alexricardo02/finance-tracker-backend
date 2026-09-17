package com.example.dataTransferObjects;

import com.example.models.PaymentMethod;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class DtoValidationTest {

    private static Validator validator;

    @BeforeAll
    static void setUpValidator() {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    // ── UserRegistrationDTO ──────────────────────────────────────────────────

    @Test
    void userRegistrationDTO_validData_hasNoViolations() {
        UserRegistrationDTO dto = new UserRegistrationDTO("john_doe", "Password123", "john@example.com");
        Set<ConstraintViolation<UserRegistrationDTO>> violations = validator.validate(dto);
        assertThat(violations).isEmpty();
    }

    @Test
    void userRegistrationDTO_blankFields_hasViolations() {
        UserRegistrationDTO dto = new UserRegistrationDTO("", "", "");
        Set<ConstraintViolation<UserRegistrationDTO>> violations = validator.validate(dto);
        assertThat(violations).hasSizeGreaterThanOrEqualTo(3);
    }

    @Test
    void userRegistrationDTO_passwordWithoutDigit_failsPattern() {
        UserRegistrationDTO dto = new UserRegistrationDTO("john_doe", "OnlyLettersPass", "john@example.com");
        Set<ConstraintViolation<UserRegistrationDTO>> violations = validator.validate(dto);
        assertThat(violations).anyMatch(v -> v.getPropertyPath().toString().equals("password"));
    }

    @Test
    void userRegistrationDTO_passwordTooShort_failsPattern() {
        UserRegistrationDTO dto = new UserRegistrationDTO("john_doe", "Pass1", "john@example.com");
        Set<ConstraintViolation<UserRegistrationDTO>> violations = validator.validate(dto);
        assertThat(violations).anyMatch(v -> v.getPropertyPath().toString().equals("password"));
    }

    // ── ExpenseRequestDTO ────────────────────────────────────────────────────

    @Test
    void expenseRequestDTO_validData_hasNoViolations() {
        ExpenseRequestDTO dto = new ExpenseRequestDTO();
        dto.setAmount(100.0);
        dto.setDate(LocalDate.now());
        dto.setCategoryId(1);
        dto.setPaymentMethod(PaymentMethod.CASH);

        Set<ConstraintViolation<ExpenseRequestDTO>> violations = validator.validate(dto);
        assertThat(violations).isEmpty();
    }

    @Test
    void expenseRequestDTO_negativeOrZeroAmount_failsPositiveConstraint() {
        ExpenseRequestDTO dto = new ExpenseRequestDTO();
        dto.setAmount(-10.0);
        dto.setDate(LocalDate.now());
        dto.setCategoryId(1);
        dto.setPaymentMethod(PaymentMethod.CASH);

        Set<ConstraintViolation<ExpenseRequestDTO>> violations = validator.validate(dto);
        assertThat(violations).anyMatch(v -> v.getPropertyPath().toString().equals("amount"));

        dto.setAmount(0.0);
        violations = validator.validate(dto);
        assertThat(violations).anyMatch(v -> v.getPropertyPath().toString().equals("amount"));
    }

    @Test
    void expenseRequestDTO_missingRequiredFields_failsNotNullConstraints() {
        ExpenseRequestDTO dto = new ExpenseRequestDTO();
        dto.setAmount(50.0);

        Set<ConstraintViolation<ExpenseRequestDTO>> violations = validator.validate(dto);
        assertThat(violations).extracting(v -> v.getPropertyPath().toString())
                .contains("date", "categoryId", "paymentMethod");
    }

    // ── IncomeRequestDTO ─────────────────────────────────────────────────────

    @Test
    void incomeRequestDTO_validData_hasNoViolations() {
        IncomeRequestDTO dto = new IncomeRequestDTO();
        dto.setAmount(500.0);
        dto.setCurrency("USD");
        dto.setDate(LocalDate.now());
        dto.setCategoryId(2);
        dto.setPaymentMethod(PaymentMethod.BANK_TRANSFER);

        Set<ConstraintViolation<IncomeRequestDTO>> violations = validator.validate(dto);
        assertThat(violations).isEmpty();
    }

    @Test
    void incomeRequestDTO_nullOrNegativeAmount_failsConstraints() {
        IncomeRequestDTO dto = new IncomeRequestDTO();
        dto.setAmount(null);
        dto.setCurrency("USD");
        dto.setDate(LocalDate.now());
        dto.setCategoryId(2);
        dto.setPaymentMethod(PaymentMethod.BANK_TRANSFER);

        Set<ConstraintViolation<IncomeRequestDTO>> violations = validator.validate(dto);
        assertThat(violations).anyMatch(v -> v.getPropertyPath().toString().equals("amount"));

        dto.setAmount(-5.0);
        violations = validator.validate(dto);
        assertThat(violations).anyMatch(v -> v.getPropertyPath().toString().equals("amount"));
    }

    @Test
    void incomeRequestDTO_missingCurrency_failsNotNull() {
        IncomeRequestDTO dto = new IncomeRequestDTO();
        dto.setAmount(100.0);
        dto.setCurrency(null);
        dto.setDate(LocalDate.now());
        dto.setCategoryId(2);
        dto.setPaymentMethod(PaymentMethod.BANK_TRANSFER);

        Set<ConstraintViolation<IncomeRequestDTO>> violations = validator.validate(dto);
        assertThat(violations).anyMatch(v -> v.getPropertyPath().toString().equals("currency"));
    }

    // ── TransactionImportRowDTO ──────────────────────────────────────────────

    @Test
    void transactionImportRowDTO_validData_hasNoViolations() {
        TransactionImportRowDTO dto = new TransactionImportRowDTO();
        dto.setKind("income");
        dto.setAmount(200.0);
        dto.setCurrency("USD");
        dto.setDate(LocalDate.now());
        dto.setCategoryName("Salary");

        Set<ConstraintViolation<TransactionImportRowDTO>> violations = validator.validate(dto);
        assertThat(violations).isEmpty();
    }

    @Test
    void transactionImportRowDTO_invalidKind_failsPatternConstraint() {
        TransactionImportRowDTO dto = new TransactionImportRowDTO();
        dto.setKind("transfer"); // Only "income" or "expense" allowed
        dto.setAmount(200.0);
        dto.setCurrency("USD");
        dto.setDate(LocalDate.now());
        dto.setCategoryName("Salary");

        Set<ConstraintViolation<TransactionImportRowDTO>> violations = validator.validate(dto);
        assertThat(violations).anyMatch(v -> v.getPropertyPath().toString().equals("kind"));
    }

    @Test
    void transactionImportRowDTO_blankRequiredFields_hasViolations() {
        TransactionImportRowDTO dto = new TransactionImportRowDTO();
        dto.setKind("");
        dto.setAmount(null);
        dto.setCurrency("");
        dto.setDate(null);
        dto.setCategoryName("");

        Set<ConstraintViolation<TransactionImportRowDTO>> violations = validator.validate(dto);
        assertThat(violations).hasSizeGreaterThanOrEqualTo(5);
    }

    // ── LoginRequestDTO ──────────────────────────────────────────────────────

    @Test
    void loginRequestDTO_valid_hasNoViolations() {
        LoginRequestDTO dto = new LoginRequestDTO("john", "password123");
        Set<ConstraintViolation<LoginRequestDTO>> violations = validator.validate(dto);
        assertThat(violations).isEmpty();
    }

    @Test
    void loginRequestDTO_blank_hasViolations() {
        LoginRequestDTO dto = new LoginRequestDTO("", "");
        Set<ConstraintViolation<LoginRequestDTO>> violations = validator.validate(dto);
        assertThat(violations).hasSize(2);
    }

    // ── ResetPasswordRequestDTO ──────────────────────────────────────────────

    @Test
    void resetPasswordRequestDTO_valid_hasNoViolations() {
        ResetPasswordRequestDTO dto = new ResetPasswordRequestDTO();
        dto.setToken("reset-token-uuid");
        dto.setNewPassword("SecurePass2026");

        Set<ConstraintViolation<ResetPasswordRequestDTO>> violations = validator.validate(dto);
        assertThat(violations).isEmpty();
    }

    @Test
    void resetPasswordRequestDTO_weakPassword_failsPattern() {
        ResetPasswordRequestDTO dto = new ResetPasswordRequestDTO();
        dto.setToken("reset-token-uuid");
        dto.setNewPassword("weak");

        Set<ConstraintViolation<ResetPasswordRequestDTO>> violations = validator.validate(dto);
        assertThat(violations).anyMatch(v -> v.getPropertyPath().toString().equals("newPassword"));
    }

    // ── DeleteAccountRequestDTO ──────────────────────────────────────────────

    @Test
    void deleteAccountRequestDTO_blankPassword_hasViolation() {
        DeleteAccountRequestDTO dto = new DeleteAccountRequestDTO();
        dto.setPassword("");

        Set<ConstraintViolation<DeleteAccountRequestDTO>> violations = validator.validate(dto);
        assertThat(violations).anyMatch(v -> v.getPropertyPath().toString().equals("password"));
    }

    // ── UserSettingsDTO ──────────────────────────────────────────────────────

    @Test
    void userSettingsDTO_validAndBlank_validatesCorrectly() {
        UserSettingsDTO valid = new UserSettingsDTO("USD");
        assertThat(validator.validate(valid)).isEmpty();

        UserSettingsDTO blank = new UserSettingsDTO("");
        assertThat(validator.validate(blank)).anyMatch(v -> v.getPropertyPath().toString().equals("primaryCurrency"));
    }
}
