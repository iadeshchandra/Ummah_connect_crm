package com.trackiq.ummah.ui.reports;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.itextpdf.kernel.colors.ColorConstants;
import com.itextpdf.kernel.colors.DeviceRgb;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.kernel.pdf.WriterProperties;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.borders.Border;
import com.itextpdf.layout.element.Cell;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.element.Table;
import com.itextpdf.layout.properties.HorizontalAlignment;
import com.itextpdf.layout.properties.TextAlignment;
import com.itextpdf.layout.properties.UnitValue;
import com.trackiq.ummah.R;
import com.trackiq.ummah.databinding.ActivityPdfReportsBinding;
import com.trackiq.ummah.model.Member;
import com.trackiq.ummah.model.Transaction;
import com.trackiq.ummah.utils.AuditLogger;

import java.io.File;
import java.io.FileNotFoundException;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * PdfReportsActivity - Generate offline PDF reports with iText7
 * 
 * Features:
 * - 100% offline PDF generation
 * - Islamic branding (Green/Gold/Blue)
 * - Bismillah header
 * - Member directory and financial reports
 */
public class PdfReportsActivity extends AppCompatActivity {

    private ActivityPdfReportsBinding binding;
    private DatabaseReference membersRef;
    private DatabaseReference ledgerRef;
    private static final int REQUEST_PERMISSION = 1001;
    private static final DeviceRgb ISLAMIC_GREEN = new DeviceRgb(30, 86, 49);
    private static final DeviceRgb ISLAMIC_GOLD = new DeviceRgb(212, 175, 55);
    private static final DeviceRgb ISLAMIC_BLUE = new DeviceRgb(31, 71, 136);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityPdfReportsBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        membersRef = FirebaseDatabase.getInstance().getReference("members");
        ledgerRef = FirebaseDatabase.getInstance().getReference("ledger");

        setupToolbar();
        checkPermissions();
        setupButtons();
    }

    private void setupToolbar() {
        setSupportActionBar(binding.toolbar);
        getSupportActionBar().setTitle("PDF Reports");
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        binding.toolbar.setNavigationOnClickListener(v -> finish());
    }

    private void checkPermissions() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                    != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, REQUEST_PERMISSION);
            }
        }
    }

    private void setupButtons() {
        binding.btnMemberReport.setOnClickListener(v -> generateMemberReport());
        binding.btnLedgerReport.setOnClickListener(v -> generateLedgerReport());
        binding.btnDonationReport.setOnClickListener(v -> generateDonationReport());
    }

    /**
     * Generate Member Directory PDF
     */
    private void generateMemberReport() {
        binding.progressBar.setVisibility(View.VISIBLE);

        membersRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                List<Member> members = new ArrayList<>();
                for (DataSnapshot child : snapshot.getChildren()) {
                    Member member = child.getValue(Member.class);
                    if (member != null) {
                        member.setId(child.getKey());
                        members.add(member);
                    }
                }

                try {
                    String fileName = "Ummah_Members_" + System.currentTimeMillis() + ".pdf";
                    File file = new File(Environment.getExternalStoragePublicDirectory(
                            Environment.DIRECTORY_DOWNLOADS), fileName);

                    PdfWriter writer = new PdfWriter(file, new WriterProperties().setFullCompressionMode(true));
                    PdfDocument pdfDoc = new PdfDocument(writer);
                    Document document = new Document(pdfDoc);

                    // Header with Bismillah
                    addIslamicHeader(document, "Member Directory");

                    // Summary
                    document.add(new Paragraph("Total Members: " + members.size())
                            .setFontSize(12)
                            .setMarginBottom(20));

                    // Table
                    Table table = new Table(UnitValue.createPercentArray(new float[]{2, 4, 3, 2}))
                            .useAllAvailableWidth();

                    // Headers
                    addTableHeader(table, "ID");
                    addTableHeader(table, "Name");
                    addTableHeader(table, "Phone");
                    addTableHeader(table, "Status");

                    // Data
                    for (Member member : members) {
                        table.addCell(createCell(member.getDisplayId()));
                        table.addCell(createCell(member.getName()));
                        table.addCell(createCell(member.getPhone() != null ? member.getPhone() : "-"));
                        table.addCell(createCell(member.getStatus() != null ? 
                                member.getStatus().toUpperCase() : "ACTIVE"));
                    }

                    document.add(table);

                    // Footer
                    addFooter(document);

                    document.close();

                    binding.progressBar.setVisibility(View.GONE);
                    Toast.makeText(PdfReportsActivity.this, 
                            "Report saved: " + file.getAbsolutePath(), Toast.LENGTH_LONG).show();

                    AuditLogger.log(PdfReportsActivity.this, AuditLogger.ACTION_PDF_GENERATED,
                            "Member directory PDF: " + fileName);

                } catch (FileNotFoundException e) {
                    binding.progressBar.setVisibility(View.GONE);
                    Toast.makeText(PdfReportsActivity.this, 
                            "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                binding.progressBar.setVisibility(View.GONE);
                Toast.makeText(PdfReportsActivity.this, 
                        "Error loading data", Toast.LENGTH_SHORT).show();
            }
        });
    }

    /**
     * Generate Financial Ledger PDF
     */
    private void generateLedgerReport() {
        binding.progressBar.setVisibility(View.VISIBLE);

        ledgerRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                List<Transaction> transactions = new ArrayList<>();
                double totalIncome = 0;
                double totalExpense = 0;

                for (DataSnapshot child : snapshot.getChildren()) {
                    Transaction transaction = child.getValue(Transaction.class);
                    if (transaction != null) {
                        transactions.add(transaction);
                        if (Transaction.CATEGORY_INCOME.equals(transaction.getCategory())) {
                            totalIncome += transaction.getAmount();
                        } else {
                            totalExpense += transaction.getAmount();
                        }
                    }
                }

                try {
                    String fileName = "Ummah_Ledger_" + System.currentTimeMillis() + ".pdf";
                    File file = new File(Environment.getExternalStoragePublicDirectory(
                            Environment.DIRECTORY_DOWNLOADS), fileName);

                    PdfWriter writer = new PdfWriter(file);
                    PdfDocument pdfDoc = new PdfDocument(writer);
                    Document document = new Document(pdfDoc);

                    // Header
                    addIslamicHeader(document, "Financial Statement");

                    // Summary
                    NumberFormat currency = NumberFormat.getCurrencyInstance(Locale.US);
                    document.add(new Paragraph("Period: All Time")
                            .setFontSize(12));
                    document.add(new Paragraph("Total Income: " + currency.format(totalIncome))
                            .setFontColor(ISLAMIC_GREEN)
                            .setFontSize(12));
                    document.add(new Paragraph("Total Expenses: " + currency.format(totalExpense))
                            .setFontColor(ColorConstants.RED)
                            .setFontSize(12));
                    document.add(new Paragraph("Net Balance: " + currency.format(totalIncome - totalExpense))
                            .setFontSize(14)
                            .setBold()
                            .setMarginBottom(20));

                    // Table
                    Table table = new Table(UnitValue.createPercentArray(
                            new float[]{2, 2, 3, 2, 2}))
                            .useAllAvailableWidth();

                    addTableHeader(table, "Date");
                    addTableHeader(table, "Type");
                    addTableHeader(table, "Description");
                    addTableHeader(table, "Amount");
                    addTableHeader(table, "Category");

                    SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
                    for (Transaction t : transactions) {
                        table.addCell(createCell(t.getDate()));
                        table.addCell(createCell(t.getTypeDisplay()));
                        table.addCell(createCell(t.getDescription()));
                        
                        Cell amountCell = createCell(currency.format(t.getAmount()));
                        if (Transaction.CATEGORY_INCOME.equals(t.getCategory())) {
                            amountCell.setFontColor(ISLAMIC_GREEN);
                        } else {
                            amountCell.setFontColor(ColorConstants.RED);
                        }
                        table.addCell(amountCell);
                        
                        table.addCell(createCell(t.getCategory()));
                    }

                    document.add(table);
                    addFooter(document);
                    document.close();

                    binding.progressBar.setVisibility(View.GONE);
                    Toast.makeText(PdfReportsActivity.this, 
                            "Report saved: " + file.getAbsolutePath(), Toast.LENGTH_LONG).show();

                    AuditLogger.log(PdfReportsActivity.this, AuditLogger.ACTION_PDF_GENERATED,
                            "Ledger PDF: " + fileName);

                } catch (FileNotFoundException e) {
                    binding.progressBar.setVisibility(View.GONE);
                    Toast.makeText(PdfReportsActivity.this, 
                            "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                binding.progressBar.setVisibility(View.GONE);
                Toast.makeText(PdfReportsActivity.this, 
                        "Error loading data", Toast.LENGTH_SHORT).show();
            }
        });
    }

    /**
     * Generate Donation Summary PDF
     */
    private void generateDonationReport() {
        binding.progressBar.setVisibility(View.VISIBLE);

        ledgerRef.orderByChild("category").equalTo("income")
                .addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                List<Transaction> donations = new ArrayList<>();
                double totalSadaqah = 0;
                double totalZakat = 0;

                for (DataSnapshot child : snapshot.getChildren()) {
                    Transaction t = child.getValue(Transaction.class);
                    if (t != null) {
                        donations.add(t);
                        if (Transaction.TYPE_SADAQAH.equals(t.getType())) {
                            totalSadaqah += t.getAmount();
                        } else if (Transaction.TYPE_ZAKAT.equals(t.getType())) {
                            totalZakat += t.getAmount();
                        }
                    }
                }

                try {
                    String fileName = "Ummah_Donations_" + System.currentTimeMillis() + ".pdf";
                    File file = new File(Environment.getExternalStoragePublicDirectory(
                            Environment.DIRECTORY_DOWNLOADS), fileName);

                    PdfWriter writer = new PdfWriter(file);
                    PdfDocument pdfDoc = new PdfDocument(writer);
                    Document document = new Document(pdfDoc);

                    addIslamicHeader(document, "Donation Summary");

                    NumberFormat currency = NumberFormat.getCurrencyInstance(Locale.US);
                    document.add(new Paragraph("Total Sadaqah: " + currency.format(totalSadaqah))
                            .setFontColor(ISLAMIC_GREEN)
                            .setFontSize(12));
                    document.add(new Paragraph("Total Zakat: " + currency.format(totalZakat))
                            .setFontColor(ISLAMIC_GOLD)
                            .setFontSize(12));
                    document.add(new Paragraph("Grand Total: " + currency.format(totalSadaqah + totalZakat))
                            .setBold()
                            .setMarginBottom(20));

                    Table table = new Table(UnitValue.createPercentArray(new float[]{2, 2, 3, 3, 2}))
                            .useAllAvailableWidth();

                    addTableHeader(table, "Date");
                    addTableHeader(table, "Type");
                    addTableHeader(table, "Donor");
                    addTableHeader(table, "Description");
                    addTableHeader(table, "Amount");

                    for (Transaction t : donations) {
                        table.addCell(createCell(t.getDate()));
                        table.addCell(createCell(t.getTypeDisplay()));
                        table.addCell(createCell(t.getPersonName() != null ? t.getPersonName() : "Anonymous"));
                        table.addCell(createCell(t.getDescription()));
                        table.addCell(createCell(currency.format(t.getAmount())));
                    }

                    document.add(table);
                    addFooter(document);
                    document.close();

                    binding.progressBar.setVisibility(View.GONE);
                    Toast.makeText(PdfReportsActivity.this, 
                            "Report saved: " + file.getAbsolutePath(), Toast.LENGTH_LONG).show();

                    AuditLogger.log(PdfReportsActivity.this, AuditLogger.ACTION_PDF_GENERATED,
                            "Donation PDF: " + fileName);

                } catch (FileNotFoundException e) {
                    binding.progressBar.setVisibility(View.GONE);
                    Toast.makeText(PdfReportsActivity.this, 
                            "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                binding.progressBar.setVisibility(View.GONE);
                Toast.makeText(PdfReportsActivity.this, 
                        "Error loading data", Toast.LENGTH_SHORT).show();
            }
        });
    }

    /**
     * Add Islamic header with Bismillah
     */
    private void addIslamicHeader(Document document, String title) {
        // Bismillah
        document.add(new Paragraph("Bismillah ir-Rahman ir-Rahim")
                .setTextAlignment(TextAlignment.CENTER)
                .setFontSize(14)
                .setItalic()
                .setFontColor(ISLAMIC_GREEN)
                .setMarginBottom(10));

        // App Name
        document.add(new Paragraph("Ummah Connect CRM")
                .setTextAlignment(TextAlignment.CENTER)
                .setFontSize(10)
                .setFontColor(ISLAMIC_GOLD));

        // Title
        document.add(new Paragraph(title)
                .setTextAlignment(TextAlignment.CENTER)
                .setFontSize(20)
                .setBold()
                .setFontColor(ISLAMIC_BLUE)
                .setMarginBottom(20));
    }

    /**
     * Add table header cell
     */
    private void addTableHeader(Table table, String text) {
        Cell cell = new Cell().add(new Paragraph(text).setBold());
        cell.setBackgroundColor(ISLAMIC_GREEN);
        cell.setFontColor(ColorConstants.WHITE);
        cell.setPadding(5);
        table.addHeaderCell(cell);
    }

    /**
     * Create standard table cell
     */
    private Cell createCell(String text) {
        Cell cell = new Cell().add(new Paragraph(text != null ? text : "-"));
        cell.setPadding(5);
        cell.setBorder(Border.NO_BORDER);
        return cell;
    }

    /**
     * Add footer with timestamp
     */
    private void addFooter(Document document) {
        document.add(new Paragraph("\n"));
        document.add(new Paragraph("Generated: " + new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US)
                .format(new Date()))
                .setTextAlignment(TextAlignment.CENTER)
                .setFontSize(8)
                .setFontColor(ColorConstants.GRAY));
        document.add(new Paragraph("Ummah Connect CRM - Serving the Community")
                .setTextAlignment(TextAlignment.CENTER)
                .setFontSize(8)
                .setFontColor(ISLAMIC_GOLD));
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, 
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_PERMISSION) {
            if (grantResults.length > 0 && grantResults[0] != PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "Storage permission required for PDF export", 
                        Toast.LENGTH_LONG).show();
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        binding = null;
    }
}
