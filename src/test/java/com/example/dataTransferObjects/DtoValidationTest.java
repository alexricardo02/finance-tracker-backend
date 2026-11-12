package com.example.dataTransferObjects;

import com.example.models.PaymentMethod;
import com.example.validation.SupportedCurrencyValidator;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.time.LocalDate;
import java.util.Set;
import java.util.stream.Collectors;

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
        dto.setCurrency("USD");
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
                .contains("currency", "date", "categoryId", "paymentMethod");
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

    // ── Currency ─────────────────────────────────────────────────────────────

    private static final String[] INVALID_CURRENCIES = {"usd", "USD'; --", "USD&symbols=X", "XXX", "", " USD"};

    @ParameterizedTest
    @ValueSource(strings = {"USD", "EUR", "GBP", "ARS", "JPY"})
    void supportedCurrency_allFiveCodes_areValidEverywhere(String code) {
        assertThat(validator.validate(expense(e -> e.setCurrency(code)))).isEmpty();
        assertThat(validator.validate(income(i -> i.setCurrency(code)))).isEmpty();
        assertThat(validator.validate(importRow(r -> r.setCurrency(code)))).isEmpty();
        assertThat(validator.validate(new UserSettingsDTO(code))).isEmpty();
    }

    @ParameterizedTest
    @ValueSource(strings = {"usd", "USD'; --", "USD&symbols=X", "XXX", " USD"})
    void supportedCurrency_invalidCode_isRejectedEverywhere(String code) {
        assertThat(messages(validator.validate(expense(e -> e.setCurrency(code))), "currency")).contains("Unsupported currency");
        assertThat(messages(validator.validate(income(i -> i.setCurrency(code))), "currency")).contains("Unsupported currency");
        assertThat(messages(validator.validate(importRow(r -> r.setCurrency(code))), "currency")).contains("Unsupported currency");
        assertThat(messages(validator.validate(new UserSettingsDTO(code)), "primaryCurrency")).contains("Unsupported currency");
    }

    @Test
    void supportedCurrencyValidator_null_isValid() {
        assertThat(new SupportedCurrencyValidator().isValid(null, null)).isTrue();
        assertThat(INVALID_CURRENCIES).noneMatch(code -> new SupportedCurrencyValidator().isValid(code, null));
    }

    @Test
    void expenseRequestDTO_missingCurrency_isRequired() {
        assertThat(messages(validator.validate(expense(e -> e.setCurrency(null))), "currency"))
                .containsExactly("Currency is required");
    }

    // ── Amount ───────────────────────────────────────────────────────────────

    @Test
    void amount_atUpperLimit_isValid() {
        assertThat(validator.validate(expense(e -> e.setAmount(1_000_000_000_000.0)))).isEmpty();
        assertThat(validator.validate(income(i -> i.setAmount(1_000_000_000_000.0)))).isEmpty();
        assertThat(validator.validate(importRow(r -> r.setAmount(1_000_000_000_000.0)))).isEmpty();
    }

    @Test
    void amount_aboveUpperLimit_isTooLarge() {
        double tooLarge = 1_000_000_000_000.01;
        assertThat(messages(validator.validate(expense(e -> e.setAmount(tooLarge))), "amount")).containsExactly("Amount is too large");
        assertThat(messages(validator.validate(income(i -> i.setAmount(tooLarge))), "amount")).containsExactly("Amount is too large");
        assertThat(messages(validator.validate(importRow(r -> r.setAmount(tooLarge))), "amount")).containsExactly("Amount is too large");
    }

    @Test
    void expenseRequestDTO_nullAmount_isRequired() {
        assertThat(messages(validator.validate(expense(e -> e.setAmount(null))), "amount"))
                .containsExactly("Amount is required");
    }

    // ── Text limits ──────────────────────────────────────────────────────────

    @ParameterizedTest
    @NullSource
    @ValueSource(ints = {0, 255})
    void description_upTo255Characters_isValid(Integer length) {
        String description = length == null ? null : "d".repeat(length);
        assertThat(validator.validate(expense(e -> e.setDescription(description)))).isEmpty();
        assertThat(validator.validate(income(i -> i.setDescription(description)))).isEmpty();
        assertThat(validator.validate(importRow(r -> r.setDescription(description)))).isEmpty();
    }

    @Test
    void description_256Characters_isRejected() {
        String description = "d".repeat(256);
        String expected = "Description must be at most 255 characters";
        assertThat(messages(validator.validate(expense(e -> e.setDescription(description))), "description")).containsExactly(expected);
        assertThat(messages(validator.validate(income(i -> i.setDescription(description))), "description")).containsExactly(expected);
        assertThat(messages(validator.validate(importRow(r -> r.setDescription(description))), "description")).containsExactly(expected);
    }

    @Test
    void transactionImportRowDTO_categoryNameLength_isLimitedTo100() {
        assertThat(validator.validate(importRow(r -> r.setCategoryName("c".repeat(100))))).isEmpty();
        assertThat(messages(validator.validate(importRow(r -> r.setCategoryName("c".repeat(101)))), "categoryName"))
                .containsExactly("Category name must be at most 100 characters");
    }

    // ── Username and email ───────────────────────────────────────────────────

    @ParameterizedTest
    @ValueSource(strings = {"abc", "john.doe", "john_doe-1", "A1.b_c-D"})
    void username_matchingPattern_isValid(String username) {
        assertThat(validator.validate(new UserRegistrationDTO(username, "Password123", "john@example.com"))).isEmpty();
        assertThat(validator.validate(new UserUpdateDTO(username, null))).isEmpty();
    }

    @ParameterizedTest
    @ValueSource(strings = {"ab", "john doe", "john\"doe", "jöhn", "john@doe", "<script>",
            "a12345678901234567890123456789012345678901234567890"})
    void username_notMatchingPattern_isRejected(String username) {
        String expected = "Username must be 3-50 characters: letters, digits, '.', '_' or '-'";
        assertThat(messages(validator.validate(new UserRegistrationDTO(username, "Password123", "john@example.com")), "username"))
                .containsExactly(expected);
        assertThat(messages(validator.validate(new UserUpdateDTO(username, null)), "username")).containsExactly(expected);
    }

    @Test
    void userUpdateDTO_nullFields_areValid_andEmailIsChecked() {
        assertThat(validator.validate(new UserUpdateDTO(null, null))).isEmpty();
        assertThat(messages(validator.validate(new UserUpdateDTO(null, "not-an-email")), "email")).isNotEmpty();
    }

    @ParameterizedTest
    @ValueSource(strings = {"not-an-email", "a@b", "john doe@example.com", "john@@example.com", "@example.com"})
    void userRegistrationDTO_malformedEmail_isRejected(String email) {
        assertThat(messages(validator.validate(new UserRegistrationDTO("john_doe", "Password123", email)), "email"))
                .contains("Invalid email format");
    }

    @Test
    void userRegistrationDTO_emailLength_isLimitedTo254() {
        // Built from 63-character labels because @Email also enforces the RFC local-part and label limits.
        String domain = "a".repeat(63) + "." + "b".repeat(63) + "." + "c".repeat(63) + "." + "d".repeat(53) + ".com";
        String atLimit = "john@" + domain;
        assertThat(atLimit).hasSize(254);
        assertThat(validator.validate(new UserRegistrationDTO("john_doe", "Password123", atLimit))).isEmpty();
        assertThat(messages(validator.validate(new UserRegistrationDTO("john_doe", "Password123", "a" + atLimit)), "email"))
                .containsExactly("Email must be at most 254 characters");
    }

    @Test
    void loginRequestDTO_legacyUsername_isNotPatternChecked_butLengthIsLimited() {
        assertThat(validator.validate(new LoginRequestDTO("legacy user@example", "password123"))).isEmpty();
        assertThat(validator.validate(new LoginRequestDTO("u".repeat(255), "password123"))).isEmpty();
        assertThat(messages(validator.validate(new LoginRequestDTO("u".repeat(256), "password123")), "username"))
                .containsExactly("Username must be at most 255 characters");
    }

    // ── CategoryDTO ──────────────────────────────────────────────────────────

    @ParameterizedTest
    @ValueSource(strings = {"income", "expense"})
    void categoryDTO_validTypes_haveNoViolations(String type) {
        assertThat(validator.validate(new CategoryDTO(null, "Food", type))).isEmpty();
        assertThat(validator.validate(new CategoryDTO(null, "n".repeat(100), type))).isEmpty();
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = {"", "Income", "EXPENSE", "transfer"})
    void categoryDTO_invalidType_isRejected(String type) {
        assertThat(messages(validator.validate(new CategoryDTO(null, "Food", type)), "type")).isNotEmpty();
    }

    @Test
    void categoryDTO_invalidName_isRejected() {
        assertThat(messages(validator.validate(new CategoryDTO(null, null, "income")), "name")).containsExactly("Category name is required");
        assertThat(messages(validator.validate(new CategoryDTO(null, " ", "income")), "name")).containsExactly("Category name is required");
        assertThat(messages(validator.validate(new CategoryDTO(null, "n".repeat(101), "income")), "name"))
                .containsExactly("Category name must be at most 100 characters");
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    private static ExpenseRequestDTO expense(java.util.function.Consumer<ExpenseRequestDTO> change) {
        ExpenseRequestDTO dto = new ExpenseRequestDTO();
        dto.setAmount(100.0);
        dto.setCurrency("USD");
        dto.setDate(LocalDate.now());
        dto.setCategoryId(1);
        dto.setPaymentMethod(PaymentMethod.CASH);
        change.accept(dto);
        return dto;
    }

    private static IncomeRequestDTO income(java.util.function.Consumer<IncomeRequestDTO> change) {
        IncomeRequestDTO dto = new IncomeRequestDTO();
        dto.setAmount(100.0);
        dto.setCurrency("USD");
        dto.setDate(LocalDate.now());
        dto.setCategoryId(1);
        dto.setPaymentMethod(PaymentMethod.CASH);
        change.accept(dto);
        return dto;
    }

    private static TransactionImportRowDTO importRow(java.util.function.Consumer<TransactionImportRowDTO> change) {
        TransactionImportRowDTO dto = new TransactionImportRowDTO();
        dto.setKind("income");
        dto.setAmount(100.0);
        dto.setCurrency("USD");
        dto.setDate(LocalDate.now());
        dto.setCategoryName("Salary");
        change.accept(dto);
        return dto;
    }

    private static <T> Set<String> messages(Set<ConstraintViolation<T>> violations, String field) {
        return violations.stream()
                .filter(v -> v.getPropertyPath().toString().equals(field))
                .map(ConstraintViolation::getMessage)
                .collect(Collectors.toSet());
    }
}
