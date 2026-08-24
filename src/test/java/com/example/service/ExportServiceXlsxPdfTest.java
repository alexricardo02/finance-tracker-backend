package com.example.service;

import com.example.models.Category;
import com.example.models.Expense;
import com.example.models.Income;
import com.example.models.PaymentMethod;
import com.example.models.User;
import com.example.repository.ExpenseRepository;
import com.example.repository.IncomeRepository;
import com.example.repository.UserRepository;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.data.jpa.domain.Specification;

import java.io.ByteArrayInputStream;
import java.time.LocalDate;
import java.util.Date;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

// WHY LENIENT: userRepository.findByUsername() is stubbed once in @BeforeEach and is
// consumed by every test via ExportService.buildRows(). Mockito strict mode cannot
// always detect @BeforeEach stubs that are consumed per-test when the stub is set up
// before the test method runs, so we relax strictness only for this shared-stub class.
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ExportServiceXlsxPdfTest {

    @Mock private IncomeRepository incomeRepository;
    @Mock private ExpenseRepository expenseRepository;
    @Mock private UserRepository userRepository;

    @InjectMocks private ExportService exportService;

    private User user;

    @BeforeEach
    void setUp() {
        user = new User("john", "john@test.com", "hash", new Date());
        user.setUser_id(1);
        user.setPrimaryCurrency("USD");
        when(userRepository.findByUsername("john")).thenReturn(Optional.of(user));
    }

    private Income buildIncome() {
        Category category = new Category("Salary", "income", user);
        Income income = new Income();
        income.setIncomeId(1);
        income.setAmount(1200.0);
        income.setCurrency("USD");
        income.setDate(LocalDate.of(2026, 7, 10));
        income.setCategory(category);
        income.setDescription("July paycheck");
        income.setPaymentMethod(PaymentMethod.BANK_TRANSFER);
        income.setAmountPrimaryCurrency(1200.0);
        return income;
    }

    private Expense buildExpense() {
        Category category = new Category("Food", "expense", user);
        Expense expense = new Expense();
        expense.setExpenseID(2);
        expense.setExpenseAmount(45.5);
        expense.setCurrency("USD");
        expense.setExpenseDate(LocalDate.of(2026, 7, 12));
        expense.setCategory(category);
        expense.setExpenseDescription("Groceries");
        expense.setPaymentMethod(PaymentMethod.CASH);
        expense.setAmountPrimaryCurrency(45.5);
        return expense;
    }

    // ---------- XLSX ----------

    @Test
    void export_xlsx_hasHeaderRowWithExpectedColumns() throws Exception {
        when(incomeRepository.findAll(any(Specification.class))).thenReturn(List.of(buildIncome()));
        when(expenseRepository.findAll(any(Specification.class))).thenReturn(List.of());

        byte[] result = exportService.export("john", "xlsx", null, null, null, null, "INCOME");

        try (XSSFWorkbook workbook = new XSSFWorkbook(new ByteArrayInputStream(result))) {
            Sheet sheet = workbook.getSheet("Transactions");
            Row header = sheet.getRow(0);

            assertThat(header.getCell(0).getStringCellValue()).isEqualTo("Date");
            assertThat(header.getCell(1).getStringCellValue()).isEqualTo("Type");
            assertThat(header.getCell(5).getStringCellValue()).isEqualTo("Amount");
        }
    }

    @Test
    void export_xlsx_dataRow_matchesIncomeValues() throws Exception {
        when(incomeRepository.findAll(any(Specification.class))).thenReturn(List.of(buildIncome()));
        when(expenseRepository.findAll(any(Specification.class))).thenReturn(List.of());

        byte[] result = exportService.export("john", "xlsx", null, null, null, null, "INCOME");

        try (XSSFWorkbook workbook = new XSSFWorkbook(new ByteArrayInputStream(result))) {
            Sheet sheet = workbook.getSheet("Transactions");
            Row dataRow = sheet.getRow(1);

            assertThat(dataRow.getCell(0).getStringCellValue()).isEqualTo("2026-07-10");
            assertThat(dataRow.getCell(1).getStringCellValue()).isEqualTo("INCOME");
            assertThat(dataRow.getCell(2).getStringCellValue()).isEqualTo("Salary");
            assertThat(dataRow.getCell(3).getStringCellValue()).isEqualTo("July paycheck");
            assertThat(dataRow.getCell(5).getNumericCellValue()).isEqualTo(1200.0);
        }
    }

    @Test
    void export_xlsx_expenseWithNullDescription_writesEmptyCell_doesNotThrow() throws Exception {
        Expense expense = buildExpense();
        expense.setExpenseDescription(null);
        when(incomeRepository.findAll(any(Specification.class))).thenReturn(List.of());
        when(expenseRepository.findAll(any(Specification.class))).thenReturn(List.of(expense));

        byte[] result = exportService.export("john", "xlsx", null, null, null, null, "EXPENSE");

        try (XSSFWorkbook workbook = new XSSFWorkbook(new ByteArrayInputStream(result))) {
            Sheet sheet = workbook.getSheet("Transactions");
            Cell descCell = sheet.getRow(1).getCell(3);
            assertThat(descCell.getStringCellValue()).isEmpty();
        }
    }

    @Test
    void export_xlsx_multipleRows_rowCountMatchesTransactionCount() throws Exception {
        when(incomeRepository.findAll(any(Specification.class))).thenReturn(List.of(buildIncome()));
        when(expenseRepository.findAll(any(Specification.class))).thenReturn(List.of(buildExpense()));

        byte[] result = exportService.export("john", "xlsx", null, null, null, null, "ALL");

        try (XSSFWorkbook workbook = new XSSFWorkbook(new ByteArrayInputStream(result))) {
            Sheet sheet = workbook.getSheet("Transactions");
            // header (row 0) + 2 data rows
            assertThat(sheet.getLastRowNum()).isEqualTo(2);
        }
    }

    // ---------- PDF ----------

    @Test
    void export_pdf_containsTitleAndTransactionData() throws Exception {
        when(incomeRepository.findAll(any(Specification.class))).thenReturn(List.of(buildIncome()));
        when(expenseRepository.findAll(any(Specification.class))).thenReturn(List.of());

        byte[] result = exportService.export("john", "pdf", null, null, null, null, "INCOME");

        try (PDDocument document = org.apache.pdfbox.Loader.loadPDF(result)) {
            String text = new PDFTextStripper().getText(document);

            assertThat(text).contains("Transactions Report");
            assertThat(text).contains("2026-07-10");
            assertThat(text).contains("INCOME");
            assertThat(document.getNumberOfPages()).isEqualTo(1);
        }
    }

    @Test
    void export_pdf_longDescription_isTruncatedWithEllipsis() throws Exception {
        Expense expense = buildExpense();
        expense.setExpenseDescription("This is an extremely long description that must be truncated for the PDF layout");
        when(incomeRepository.findAll(any(Specification.class))).thenReturn(List.of());
        when(expenseRepository.findAll(any(Specification.class))).thenReturn(List.of(expense));

        byte[] result = exportService.export("john", "pdf", null, null, null, null, "EXPENSE");

        try (PDDocument document = org.apache.pdfbox.Loader.loadPDF(result)) {
            String text = new PDFTextStripper().getText(document);
            assertThat(text).contains("…");
            assertThat(text).doesNotContain("This is an extremely long description that must be truncated for the PDF layout");
        }
    }

    @Test
    void export_pdf_emptyTransactions_stillProducesValidSinglePageDocument() throws Exception {
        when(incomeRepository.findAll(any(Specification.class))).thenReturn(List.of());
        when(expenseRepository.findAll(any(Specification.class))).thenReturn(List.of());

        byte[] result = exportService.export("john", "pdf", null, null, null, null, "ALL");

        try (PDDocument document = org.apache.pdfbox.Loader.loadPDF(result)) {
            assertThat(document.getNumberOfPages()).isEqualTo(1);
            String text = new PDFTextStripper().getText(document);
            assertThat(text).contains("Date").contains("Type").contains("Amount");
        }
    }

    @Test
    void export_unknownFormat_defaultsToCsv() throws Exception {
        when(incomeRepository.findAll(any(Specification.class))).thenReturn(List.of(buildIncome()));
        when(expenseRepository.findAll(any(Specification.class))).thenReturn(List.of());

        byte[] result = exportService.export("john", "unknownformat", null, null, null, null, "INCOME");
        String content = new String(result, java.nio.charset.StandardCharsets.UTF_8);

        assertThat(content).startsWith("Date,Type,Category");
    }
}
