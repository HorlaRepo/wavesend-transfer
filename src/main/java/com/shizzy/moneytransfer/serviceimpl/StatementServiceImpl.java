package com.shizzy.moneytransfer.serviceimpl;

import com.lowagie.text.*;
import com.lowagie.text.Font;
import com.lowagie.text.pdf.*;
import com.shizzy.moneytransfer.api.ApiResponse;
import com.shizzy.moneytransfer.exception.ResourceNotFoundException;
import com.shizzy.moneytransfer.model.Transaction;
import com.shizzy.moneytransfer.model.Wallet;
import com.shizzy.moneytransfer.repository.TransactionRepository;
import com.shizzy.moneytransfer.repository.WalletRepository;
import com.shizzy.moneytransfer.service.StatementService;
import com.shizzy.moneytransfer.util.FmtLocalDateTime;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.supercsv.cellprocessor.FmtDate;
import org.supercsv.cellprocessor.Optional;
import org.supercsv.cellprocessor.constraint.NotNull;
import org.supercsv.cellprocessor.ift.CellProcessor;
import org.supercsv.io.CsvBeanWriter;
import org.supercsv.io.ICsvBeanWriter;
import org.supercsv.prefs.CsvPreference;


import java.awt.*;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@RequiredArgsConstructor
@Service
public class StatementServiceImpl implements StatementService {

    private final TransactionRepository transactionRepository;
    private final WalletRepository walletRepository;

    @Override
    public ApiResponse<byte[]> generateStatement(Authentication connectedUser, LocalDateTime startDate, LocalDateTime endDate, String format) throws IOException {

        if(connectedUser == null){
            throw new IllegalArgumentException("Connected user is null");
        }
        String userId = connectedUser.getName();
        Wallet wallet = walletRepository.findWalletByCreatedBy(userId).orElseThrow(()-> new ResourceNotFoundException("User wallet not found"));

        try {
            // Fetch transactions from the database
            List<Transaction> transactions = transactionRepository.findTransactionsByWalletAndTransactionDateBetween(wallet, startDate, endDate);
            System.out.println( "Transactions fetched: " + transactions.size() + " for user: " + userId + " from " + startDate + " to " + endDate + " in format: " + format + "...");

            // Define custom headers and field mapping
            String[] headers = {"Transaction Date", "Description", "Amount ($)", "Type", "Status"};
            String[] fieldMapping = {"transactionDate", "description", "amount", "transactionType", "currentStatus"};

            // Use ByteArrayOutputStream to capture the statement content
            ByteArrayOutputStream baos = new ByteArrayOutputStream();

            // Generate statement based on format
            if (format.equalsIgnoreCase("csv")) {
                generateCsv(transactions, startDate, endDate, baos, headers, fieldMapping);
            } else if (format.equalsIgnoreCase("pdf")) {
                generatePdf(transactions, baos, startDate, endDate);
            } else {
                return ApiResponse.<byte[]>builder()
                        .success(false)
                        .message("Invalid format: " + format + ". Use 'csv' or 'pdf'.")
                        .build();
            }

            // Convert the output to a byte array and return it in ApiResponse
            byte[] data = baos.toByteArray();
            return ApiResponse.<byte[]>builder()
                    .success(true)
                    .message("Statement generated successfully")
                    .data(data)
                    .build();

        } catch (Exception e) {
            // Handle any errors during statement generation
            return ApiResponse.<byte[]>builder()
                    .success(false)
                    .message("Error generating statement: " + e.getMessage())
                    .build();
        }
    }

    private void generateCsv(List<Transaction> transactions, LocalDateTime start, LocalDateTime end, OutputStream outputStream, String[] headers, String[] fieldMapping) throws IOException {
        // Wrap the OutputStream in a CsvBeanWriter with standard CSV preferences
        try (ICsvBeanWriter csvWriter = new CsvBeanWriter(new OutputStreamWriter(outputStream), CsvPreference.STANDARD_PREFERENCE)) {
            // Write a comment with the date range for reference
            csvWriter.writeComment("Statement from " + start.format(DateTimeFormatter.ofPattern("MMM dd yyyy")) +
                    " to " + end.format(DateTimeFormatter.ofPattern("MMM dd yyyy")));

            // Write the custom headers to the CSV
            csvWriter.writeHeader(headers);

            // Define cell processors for each column
            CellProcessor[] processors = {
                    new FmtLocalDateTime("MMM dd yyyy"), // Formats LocalDateTime to a readable string
                    new Optional(),                     // Allows null values for description
                    new NotNull(),                      // Ensures amount is non-null
                    new Optional(),                     // Allows null values for transactionType
                    new Optional()                      // Allows null values for currentStatus
            };

            // Write each transaction to the CSV using the field mapping and processors
            for (Transaction transaction : transactions) {
                csvWriter.write(transaction, fieldMapping, processors);
            }
        }
    }

    private void generatePdf(List<Transaction> transactions, OutputStream outputStream, LocalDateTime start, LocalDateTime end) throws IOException {
        try (Document document = new Document()) {
            PdfWriter writer = PdfWriter.getInstance(document, outputStream);

            // Set the background color for each page
            writer.setPageEvent(new PdfPageEventHelper() {
                @Override
                public void onEndPage(PdfWriter writer, Document document) {
                    PdfContentByte canvas = writer.getDirectContentUnder();
                    canvas.setColorFill(new Color(232, 249, 239)); // Very light green
                    canvas.rectangle(0, 0, document.getPageSize().getWidth(), document.getPageSize().getHeight());
                    canvas.fill();
                }
            });
            document.open();

            // Define fonts for title, headers, and cells
            Font titleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 14);
            Font headerFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12);
            Font cellFont = FontFactory.getFont(FontFactory.HELVETICA, 11);

            // Add title with date range
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MMM dd, yyyy");
            String titleText = "STATEMENT FROM " + start.format(formatter) + " - " + end.format(formatter);
            Paragraph title = new Paragraph(titleText, titleFont);
            title.setAlignment(Element.ALIGN_CENTER);
            document.add(title);

            // Add spacing after the title
            document.add(new Paragraph(" "));

            // Create a table with 5 columns
            PdfPTable table = new PdfPTable(5);
            table.setWidthPercentage(100); // Full page width

            // Add styled headers
            String[] headers = {"Date", "Description", "Amount (USD)", "Type", "Status"};
            for (String header : headers) {
                PdfPCell cell = new PdfPCell(new Phrase(header, headerFont));
                cell.setBackgroundColor(Color.LIGHT_GRAY);
                cell.setPadding(5);
                cell.setBorderWidth(1);
                table.addCell(cell);
            }

            // Add transaction data with alternating row colors
            boolean alternate = false;
            for (Transaction transaction : transactions) {
                PdfPCell dateCell = createCell(formatter.format(transaction.getTransactionDate()), cellFont, alternate);
                PdfPCell descCell = createCell(transaction.getDescription(), cellFont, alternate);
                PdfPCell amountCell = createCell(String.valueOf(transaction.getAmount()), cellFont, alternate);
                PdfPCell typeCell = createCell(String.valueOf(transaction.getTransactionType()), cellFont, alternate);
                PdfPCell statusCell = createCell(String.valueOf(transaction.getCurrentStatus()), cellFont, alternate);

                table.addCell(dateCell);
                table.addCell(descCell);
                table.addCell(amountCell);
                table.addCell(typeCell);
                table.addCell(statusCell);

                alternate = !alternate; // Toggle row color
            }

            document.add(table);
        } catch (DocumentException e) {
            throw new IOException("Error generating PDF: " + e.getMessage(), e);
        }
    }

    // Helper method to create styled cells
    private PdfPCell createCell(String content, Font font, boolean alternate) {
        PdfPCell cell = new PdfPCell(new Phrase(content, font));
        cell.setPadding(5);
        cell.setBorderWidth(1);
        if (alternate) {
            cell.setBackgroundColor(Color.LIGHT_GRAY); // Light gray for alternating rows
        }
        return cell;
    }


}

