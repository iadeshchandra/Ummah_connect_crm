package com.trackiq.ummah.model;

/**
 * Transaction Model - Financial ledger entries
 * 
 * Firebase Path: /ledger/{transactionId}
 * Types: sadaqah, zakat, maintenance, iftar, other
 */
public class Transaction {
    
    public static final String TYPE_SADAQAH = "sadaqah";
    public static final String TYPE_ZAKAT = "zakat";
    public static final String TYPE_MAINTENANCE = "maintenance";
    public static final String TYPE_IFTAR = "iftar";
    public static final String TYPE_OTHER = "other";
    
    public static final String CATEGORY_INCOME = "income";
    public static final String CATEGORY_EXPENSE = "expense";
    
    private String id;              // Auto-generated
    private String type;            // Transaction type
    private String category;        // income or expense
    private double amount;          // Amount
    private String description;     // Description
    private String date;            // ISO date (yyyy-MM-dd)
    private String personId;          // MBR-XXXX or GST-XXXX (optional, for donations)
    private String personName;      // Name of donor/payee
    private String recordedBy;      // Staff/Admin who recorded
    private long timestamp;         // Created timestamp
    private long updatedAt;         // Updated timestamp

    // Required empty constructor for Firebase
    public Transaction() {
    }

    public Transaction(String type, String category, double amount, 
                       String description, String date, String recordedBy) {
        this.type = type;
        this.category = category;
        this.amount = amount;
        this.description = description;
        this.date = date;
        this.recordedBy = recordedBy;
        this.timestamp = System.currentTimeMillis();
        this.updatedAt = System.currentTimeMillis();
    }

    // Getters and Setters
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public double getAmount() {
        return amount;
    }

    public void setAmount(double amount) {
        this.amount = amount;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getDate() {
        return date;
    }

    public void setDate(String date) {
        this.date = date;
    }

    public String getPersonId() {
        return personId;
    }

    public void setPersonId(String personId) {
        this.personId = personId;
    }

    public String getPersonName() {
        return personName;
    }

    public void setPersonName(String personName) {
        this.personName = personName;
    }

    public String getRecordedBy() {
        return recordedBy;
    }

    public void setRecordedBy(String recordedBy) {
        this.recordedBy = recordedBy;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    public long getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(long updatedAt) {
        this.updatedAt = updatedAt;
    }

    /**
     * Get type display name
     */
    public String getTypeDisplay() {
        if (type == null) return "Other";
        switch (type.toLowerCase()) {
            case TYPE_SADAQAH:
                return "Sadaqah";
            case TYPE_ZAKAT:
                return "Zakat";
            case TYPE_MAINTENANCE:
                return "Masjid Maintenance";
            case TYPE_IFTAR:
                return "Iftar Program";
            case TYPE_OTHER:
            default:
                return "Other";
        }
    }

    /**
     * Check if this is a donation (income with person link)
     */
    public boolean isDonation() {
        return CATEGORY_INCOME.equals(category) && personId != null;
    }

    /**
     * Convert to Map for Firebase update
     */
    public java.util.Map<String, Object> toMap() {
        java.util.Map<String, Object> map = new java.util.HashMap<>();
        map.put("type", type);
        map.put("category", category);
        map.put("amount", amount);
        map.put("description", description);
        map.put("date", date);
        map.put("personId", personId);
        map.put("personName", personName);
        map.put("recordedBy", recordedBy);
        map.put("timestamp", timestamp);
        map.put("updatedAt", updatedAt);
        return map;
    }
}
