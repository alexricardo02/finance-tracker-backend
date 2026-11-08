package com.example.integration;

import com.example.service.ExpenseService;
import com.example.service.IncomeService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.serializer.JdkSerializationRedisSerializer;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Supplier;

import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static org.assertj.core.api.Assertions.assertThat;

class CacheBehaviourIT extends AbstractApiIntegrationTest {

    private static final LocalDate TODAY = LocalDate.of(2026, 7, 2);

    @Autowired private IncomeService incomeService;
    @Autowired private ExpenseService expenseService;

    private String monthKey(ApiUser user) {
        return "incomes_month::" + user.username() + "_JULY";
    }

    private String totalJuly(ApiUser user) {
        return call(HttpMethod.GET, "/api/incomes/total-month?month=JULY", user, null).getBody();
    }

    private void changeAmountsBehindTheCache(ApiUser user) {
        jdbc.update("UPDATE incomes SET amount_primary_currency = amount_primary_currency + 1 WHERE user_id = ?", user.id());
        jdbc.update("UPDATE expenses SET amount_primary_currency = amount_primary_currency + 1 WHERE user_id = ?", user.id());
    }

    @Test
    void totalMonth_secondCallIsServedFromRedis() {
        ApiUser user = newUser();
        createIncome(user, 100, "x");

        assertThat(totalJuly(user)).isEqualTo("100.0");
        assertThat(redisTemplate.hasKey(monthKey(user))).isTrue();

        changeAmountsBehindTheCache(user);

        assertThat(totalJuly(user)).isEqualTo("100.0");
    }

    @Test
    void cachedTotals_areIsolatedPerUser() {
        ApiUser alice = newUser();
        ApiUser bob = newUser();
        createIncome(alice, 100, "x");
        createIncome(bob, 7, "y");

        assertThat(totalJuly(alice)).isEqualTo("100.0");
        assertThat(totalJuly(bob)).isEqualTo("7.0");
        assertThat(totalJuly(alice)).isEqualTo("100.0");
    }

    enum Mutation { CREATE_INCOME, UPDATE_INCOME, DELETE_INCOME, CREATE_EXPENSE, UPDATE_EXPENSE, DELETE_EXPENSE, IMPORT, CHANGE_CURRENCY }

    @ParameterizedTest
    @EnumSource(Mutation.class)
    void everyMutation_evictsCallersFinancialCache(Mutation mutation) {
        ApiUser user = newUser();
        int income = createIncome(user, 100, "x");
        int expense = createExpense(user, 10, "y");
        stubFrankfurter("USD", "EUR", TX_DATE, 0.5);
        totalJuly(user);
        assertThat(redisTemplate.hasKey(monthKey(user))).isTrue();

        ResponseEntity<String> response = switch (mutation) {
            case CREATE_INCOME -> call(HttpMethod.POST, "/api/incomes", user, incomeBody(1, "USD", TX_DATE, "n"));
            case UPDATE_INCOME -> call(HttpMethod.PUT, "/api/incomes/" + income, user, incomeBody(1, "USD", TX_DATE, "n"));
            case DELETE_INCOME -> call(HttpMethod.DELETE, "/api/incomes/" + income, user, null);
            case CREATE_EXPENSE -> call(HttpMethod.POST, "/api/expenses", user, expenseBody(1, "USD", TX_DATE, "n"));
            case UPDATE_EXPENSE -> call(HttpMethod.PUT, "/api/expenses/" + expense, user, expenseBody(1, "USD", TX_DATE, "n"));
            case DELETE_EXPENSE -> call(HttpMethod.DELETE, "/api/expenses/" + expense, user, null);
            case IMPORT -> call(HttpMethod.POST, "/api/imports/transactions", user,
                    "[{\"kind\":\"income\",\"amount\":5.0,\"currency\":\"USD\",\"date\":\"" + TX_DATE + "\",\"categoryName\":\"Salary\"}]");
            case CHANGE_CURRENCY -> call(HttpMethod.PUT, "/api/settings/currency", user, Map.of("primaryCurrency", "EUR"));
        };

        assertThat(response.getStatusCode().is2xxSuccessful()).as(response.getBody()).isTrue();
        assertThat(redisTemplate.hasKey(monthKey(user))).isFalse();
    }

    @Test
    void mutation_leavesOtherUsersCacheInPlace() {
        ApiUser alice = newUser();
        ApiUser bob = newUser();
        createIncome(bob, 7, "y");
        totalJuly(bob);

        createIncome(alice, 100, "x");

        assertThat(redisTemplate.hasKey(monthKey(bob))).isTrue();
    }

    @Test
    void mutation_leavesCacheOfUserWhoseNameContainsCallersName() {
        String name = "u" + System.nanoTime();
        ApiUser shortName = newUser(name);
        ApiUser longerName = newUser(name + "x");
        createIncome(longerName, 7, "y");
        totalJuly(longerName);

        createIncome(shortName, 100, "x");

        assertThat(redisTemplate.hasKey(monthKey(longerName))).isTrue();
    }

    @Test
    void mutation_byUserNamedLikeRedisPrefix_doesNotUnrevokeTokens() {
        ApiUser revoked = newUser();
        call(HttpMethod.POST, "/api/users/logout", null, null, Map.of("Cookie", revoked.cookie()));
        assertThat(call(HttpMethod.GET, "/api/settings", revoked, null).getStatusCode().value()).isEqualTo(401);
        ApiUser attacker = newUser("blacklist");

        createIncome(attacker, 1, "x");

        assertThat(call(HttpMethod.GET, "/api/settings", revoked, null).getStatusCode().value()).isEqualTo(401);
    }

    @Test
    void mutation_byUserNamedWithGlobCharacter_doesNotWipeOtherUsersCache() {
        ApiUser victim = newUser();
        createIncome(victim, 7, "y");
        totalJuly(victim);
        ApiUser attacker = newUser("*");

        createIncome(attacker, 1, "x");

        assertThat(redisTemplate.hasKey(monthKey(victim))).isTrue();
    }

    @Test
    void mutation_keepsCallersOwnMutationBucket() {
        ApiUser user = newUser();
        createIncome(user, 1, "x");
        assertThat(redisTemplate.hasKey("mutation_limit:" + user.username())).isTrue();

        createIncome(user, 2, "y");

        assertThat(redisTemplate.hasKey("mutation_limit:" + user.username())).isTrue();
    }

    @Test
    void mutation_byUserNamedLikeCurrency_keepsFxCache() {
        redisTemplate.opsForValue().set("fx_rate::EUR_USD_" + TX_DATE, "2.0");
        ApiUser user = newUser("EUR");

        createIncome(user, 1, "x");

        assertThat(redisTemplate.hasKey("fx_rate::EUR_USD_" + TX_DATE)).isTrue();
    }

    @Test
    void everyCacheableTotal_roundTripsThroughRedis_includingLocalDateKeys() {
        ApiUser user = newUser();
        String u = user.username();
        createIncome(user, 100, "x");
        createExpense(user, 10, "y");

        Map<String, Supplier<Double>> cached = new LinkedHashMap<>();
        cached.put("incomes_type::" + u + "_Salary", () -> incomeService.getTotalIncomeAmountByType("Salary", u));
        cached.put("incomes_month::" + u + "_JULY", () -> incomeService.getTotalIncomeAmountByMonth("JULY", u));
        cached.put("incomes_year::" + u + "_2026", () -> incomeService.getTotalIncomeAmountByYear(2026, u));
        cached.put("incomes_day::" + u + "_1", () -> incomeService.getTotalIncomeAmounByDay(1, u));
        cached.put("incomes_last_7_days::" + u + "_" + TODAY, () -> incomeService.getTotalIncomesLast7DaysInclusive(TODAY, u));
        cached.put("incomes_last_month::" + u + "_" + TODAY, () -> incomeService.getTotalIncomesLastMonths(TODAY, u));
        cached.put("incomes_last_3_month::" + u + "_" + TODAY, () -> incomeService.getTotalIncomesLast3Months(TODAY, u));
        cached.put("incomes_last_6_months::" + u + "_" + TODAY, () -> incomeService.getTotalIncomesLast6Months(TODAY, u));
        cached.put("incomes_last_year::" + u + "_" + TODAY, () -> incomeService.getTotalIncomesLastYear(TODAY, u));
        cached.put("expenses_month::" + u + "_JULY", () -> expenseService.getTotalExpenseAmounByMonthAndUser("JULY", u));
        cached.put("expenses_year::" + u + "_2026", () -> expenseService.getTotalExpenseAmounByYearAndUser(2026, u));
        cached.put("expenses_day::" + u + "_1", () -> expenseService.getTotalExpenseAmounByDayAndUser(1, u));
        cached.put("expenses_last_7_days::" + u + "_" + TODAY, () -> expenseService.getTotalExpensesLast7DaysInclusiveAndUser(TODAY, u));
        cached.put("expenses_last_months::" + u + "_" + TODAY, () -> expenseService.getTotalExpensesLastMonthsAndUser(TODAY, u));
        cached.put("expenses_last_3_months::" + u + "_" + TODAY, () -> expenseService.getTotalExpensesLast3MonthsAndUser(TODAY, u));
        cached.put("expenses_last_6_months::" + u + "_" + TODAY, () -> expenseService.getTotalExpensesLast6MonthsAndUser(TODAY, u));
        cached.put("expenses_last_year::" + u + "_" + TODAY, () -> expenseService.getTotalExpensesLastYearAndUser(TODAY, u));

        Map<String, Double> firstValues = new LinkedHashMap<>();
        cached.forEach((key, loader) -> firstValues.put(key, loader.get()));

        changeAmountsBehindTheCache(user);

        cached.forEach((key, loader) -> {
            assertThat(redisTemplate.hasKey(key)).as(key).isTrue();
            assertThat(loader.get()).as(key).isEqualTo(firstValues.get(key));
        });
    }

    @Test
    void financialCacheEntries_haveATtl() {
        ApiUser user = newUser();
        totalJuly(user);

        assertThat(redisTemplate.getExpire(monthKey(user))).isPositive();
    }

    @Test
    void cachedRate_survivesJacksonRoundTripAsTheDto() {
        stubDolarOficial(1000.0);
        ApiUser user = newUser();

        call(HttpMethod.GET, "/api/exchange-rate/usd-ars", user, null);

        assertThat(redisTemplate.opsForValue().get("usd_ars_oficial::rate"))
                .contains("ExchangeRateResponseDTO", "1000.0");
        assertThat(json(call(HttpMethod.GET, "/api/exchange-rate/usd-ars", user, null)).get("casa").asText()).isEqualTo("oficial");
    }

    @Test
    void legacyJdkSerializedEntry_isTreatedAsMissAndReplacedWithExpiringEntry() {
        ApiUser user = newUser();
        createIncome(user, 100, "x");
        byte[] key = monthKey(user).getBytes(StandardCharsets.UTF_8);
        try (var connection = redisTemplate.getConnectionFactory().getConnection()) {
            connection.stringCommands().set(key, new JdkSerializationRedisSerializer().serialize(555.0));
        }
        assertThat(redisTemplate.getExpire(monthKey(user))).isEqualTo(-1);

        ResponseEntity<String> response = call(HttpMethod.GET, "/api/incomes/total-month?month=JULY", user, null);

        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(response.getBody()).isEqualTo("100.0");
        assertThat(redisTemplate.getExpire(monthKey(user))).isPositive();
    }

    @Test
    void usdArsRate_isFetchedOnceThenServedFromCache() {
        stubDolarOficial(1000.0);
        ApiUser user = newUser();

        ResponseEntity<String> first = call(HttpMethod.GET, "/api/exchange-rate/usd-ars", user, null);
        ResponseEntity<String> second = call(HttpMethod.GET, "/api/exchange-rate/usd-ars", user, null);

        assertThat(json(first).get("venta").asDouble()).isEqualTo(1000.0);
        assertThat(json(second).get("venta").asDouble()).isEqualTo(1000.0);
        FX_API.verify(1, getRequestedFor(urlPathEqualTo("/v1/dolares/oficial")));
        assertThat(redisTemplate.getExpire("usd_ars_oficial::rate")).isPositive();
    }

    @Test
    void fxRate_isCachedPerCurrencyPairAndDate() {
        stubFrankfurter("EUR", "USD", TX_DATE, 2.0);
        ApiUser user = newUser();

        call(HttpMethod.POST, "/api/incomes", user, incomeBody(1, "EUR", TX_DATE, "a"));
        ResponseEntity<String> second = call(HttpMethod.POST, "/api/incomes", user, incomeBody(3, "EUR", TX_DATE, "b"));

        assertThat(json(second).get("amountPrimaryCurrency").asDouble()).isEqualTo(6.0);
        FX_API.verify(1, getRequestedFor(urlPathEqualTo("/v1/" + TX_DATE)));
        assertThat(redisTemplate.hasKey("fx_rate::EUR_USD_" + TX_DATE)).isTrue();
    }
}
