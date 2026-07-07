package com.example.service;

import com.example.dataTransferObjects.TransactionExportRow;
import com.example.models.Expense;
import com.example.models.Income;
import com.example.models.PaymentMethod;
import com.example.models.User;
import com.example.repository.ExpenseRepository;
import com.example.repository.IncomeRepository;
import com.example.repository.UserRepository;
import jakarta.persistence.EntityNotFoundException;
import jakarta.persistence.criteria.Predicate;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.apache.pdfbox.pdmodel.*;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Service
public class ExportService {

    @Autowired private IncomeRepository incomeRepository;
    @Autowired private ExpenseRepository expenseRepository;
    @Autowired private UserRepository userRepository;

    public byte[] export(String username, String format, LocalDate startDate, LocalDate endDate,
                          Long categoryId, PaymentMethod paymentMethod, String kind) throws IOException {
        List<TransactionExportRow> rows = buildRows(username, startDate, endDate, categoryId, paymentMethod, kind);
        return switch (format.toLowerCase()) {
            case "xlsx" -> toExcel(rows);
            case "pdf" -> toPdf(rows);
            default -> toCsv(rows);
        };
    }

    private List<TransactionExportRow> buildRows(String username, LocalDate startDate, LocalDate endDate,
                                                  Long categoryId, PaymentMethod paymentMethod, String kind) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new EntityNotFoundException("User not found"));

        List<TransactionExportRow> rows = new ArrayList<>();

        if (!"EXPENSE".equalsIgnoreCase(kind)) {
            Specification<Income> spec = (root, query, cb) -> {
                List<Predicate> predicates = new ArrayList<>();
                predicates.add(cb.equal(root.get("user").get("username"), username));
                if (startDate != null) predicates.add(cb.greaterThanOrEqualTo(root.get("date"), startDate));
                if (endDate != null) predicates.add(cb.lessThanOrEqualTo(root.get("date"), endDate));
                if (categoryId != null) predicates.add(cb.equal(root.get("category").get("categoryId"), categoryId));
                if (paymentMethod != null) predicates.add(cb.equal(root.get("paymentMethod"), paymentMethod));
                return cb.and(predicates.toArray(new Predicate[0]));
            };
            for (Income i : incomeRepository.findAll(spec)) {
                rows.add(new TransactionExportRow(i.getDate(), "INCOME",
                        i.getCategory() != null ? i.getCategory().getName() : "Uncategorized",
                        i.getDescription(), i.getPaymentMethod() != null ? i.getPaymentMethod().name() : "",
                        i.getAmount(), i.getCurrency(), i.getAmountPrimaryCurrency(), user.getPrimaryCurrency()));
            }
        }

        if (!"INCOME".equalsIgnoreCase(kind)) {
            Specification<Expense> spec = (root, query, cb) -> {
                List<Predicate> predicates = new ArrayList<>();
                predicates.add(cb.equal(root.get("user").get("username"), username));
                if (startDate != null) predicates.add(cb.greaterThanOrEqualTo(root.get("date"), startDate));
                if (endDate != null) predicates.add(cb.lessThanOrEqualTo(root.get("date"), endDate));
                if (categoryId != null) predicates.add(cb.equal(root.get("category").get("categoryId"), categoryId));
                if (paymentMethod != null) predicates.add(cb.equal(root.get("paymentMethod"), paymentMethod));
                return cb.and(predicates.toArray(new Predicate[0]));
            };
            for (Expense e : expenseRepository.findAll(spec)) {
                rows.add(new TransactionExportRow(e.getExpenseDate(), "EXPENSE",
                        e.getCategory() != null ? e.getCategory().getName() : "Uncategorized",
                        e.getExpenseDescription(), e.getPaymentMethod() != null ? e.getPaymentMethod().name() : "",
                        e.getExpenseAmount(), e.getCurrency(), e.getAmountPrimaryCurrency(), user.getPrimaryCurrency()));
            }
        }

        rows.sort(Comparator.comparing(TransactionExportRow::getDate).reversed());
        return rows;
    }

    private byte[] toCsv(List<TransactionExportRow> rows) {
        StringBuilder sb = new StringBuilder();
        sb.append("Date,Type,Category,Description,Payment Method,Amount,Currency,Amount (Primary),Primary Currency\n");
        for (TransactionExportRow r : rows) {
            sb.append(r.getDate()).append(",")
              .append(r.getKind()).append(",")
              .append(escapeCsv(r.getCategory())).append(",")
              .append(escapeCsv(r.getDescription())).append(",")
              .append(r.getPaymentMethod()).append(",")
              .append(r.getAmount()).append(",")
              .append(r.getCurrency()).append(",")
              .append(r.getAmountPrimaryCurrency()).append(",")
              .append(r.getPrimaryCurrency()).append("\n");
        }
        return sb.toString().getBytes(StandardCharsets.UTF_8);
    }

    private String escapeCsv(String value) {
        if (value == null) return "";
        String escaped = value.replace("\"", "\"\"");
        return (escaped.contains(",") || escaped.contains("\n") || escaped.contains("\""))
                ? "\"" + escaped + "\"" : escaped;
    }

    private byte[] toExcel(List<TransactionExportRow> rows) throws IOException {
        try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet("Transactions");
            String[] headers = {"Date", "Type", "Category", "Description", "Payment Method", "Amount", "Currency", "Amount (Primary)", "Primary Currency"};

            CellStyle headerStyle = workbook.createCellStyle();
            Font boldFont = workbook.createFont();
            boldFont.setBold(true);
            headerStyle.setFont(boldFont);

            Row headerRow = sheet.createRow(0);
            for (int i = 0; i < headers.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(headerStyle);
            }

            int rowIdx = 1;
            for (TransactionExportRow r : rows) {
                Row row = sheet.createRow(rowIdx++);
                row.createCell(0).setCellValue(r.getDate().toString());
                row.createCell(1).setCellValue(r.getKind());
                row.createCell(2).setCellValue(r.getCategory());
                row.createCell(3).setCellValue(r.getDescription() != null ? r.getDescription() : "");
                row.createCell(4).setCellValue(r.getPaymentMethod());
                row.createCell(5).setCellValue(r.getAmount());
                row.createCell(6).setCellValue(r.getCurrency());
                row.createCell(7).setCellValue(r.getAmountPrimaryCurrency() != null ? r.getAmountPrimaryCurrency() : 0);
                row.createCell(8).setCellValue(r.getPrimaryCurrency());
            }
            for (int i = 0; i < headers.length; i++) sheet.autoSizeColumn(i);

            workbook.write(out);
            return out.toByteArray();
        }
    }

    private byte[] toPdf(List<TransactionExportRow> rows) throws IOException {
        try (PDDocument document = new PDDocument(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            PDPage page = new PDPage(PDRectangle.A4);
            document.addPage(page);

            PDType1Font font = new PDType1Font(Standard14Fonts.FontName.HELVETICA);
            PDType1Font boldFont = new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD);

            float margin = 40;
            float yStart = page.getMediaBox().getHeight() - margin;
            float rowHeight = 16f;
            float y = yStart;

            String[] headers = {"Date", "Type", "Category", "Description", "Method", "Amount"};
            float[] colWidths = {70, 55, 90, 140, 80, 80};

            PDPageContentStream cs = new PDPageContentStream(document, page);
            cs.setFont(boldFont, 14);
            cs.beginText();
            cs.newLineAtOffset(margin, y);
            cs.showText("Transactions Report");
            cs.endText();
            y -= 30;

            writeRow(cs, headers, colWidths, margin, y, boldFont, 9);
            y -= rowHeight;

            for (TransactionExportRow r : rows) {
                if (y < margin + rowHeight) {
                    cs.close();
                    PDPage newPage = new PDPage(PDRectangle.A4);
                    document.addPage(newPage);
                    cs = new PDPageContentStream(document, newPage);
                    y = yStart;
                    writeRow(cs, headers, colWidths, margin, y, boldFont, 9);
                    y -= rowHeight;
                }
                String amountStr = String.format("%.2f %s", r.getAmount(), r.getCurrency());
                String[] values = {
                        String.valueOf(r.getDate()), r.getKind(),
                        truncate(r.getCategory(), 18), truncate(r.getDescription(), 28),
                        r.getPaymentMethod() != null ? r.getPaymentMethod().replace("_", " ") : "",
                        amountStr
                };
                writeRow(cs, values, colWidths, margin, y, font, 8);
                y -= rowHeight;
            }
            cs.close();
            document.save(out);
            return out.toByteArray();
        }
    }

    private void writeRow(PDPageContentStream cs, String[] values, float[] colWidths,
                           float x, float y, PDType1Font font, int fontSize) throws IOException {
        cs.setFont(font, fontSize);
        float cx = x;
        for (int i = 0; i < values.length; i++) {
            cs.beginText();
            cs.newLineAtOffset(cx, y);
            cs.showText(values[i] != null ? values[i] : "");
            cs.endText();
            cx += colWidths[i];
        }
    }

    private String truncate(String value, int max) {
        if (value == null) return "";
        return value.length() > max ? value.substring(0, max - 1) + "…" : value;
    }
}