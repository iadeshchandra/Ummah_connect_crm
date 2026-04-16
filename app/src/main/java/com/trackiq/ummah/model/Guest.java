package com.trackiq.ummah.model;

/**
 * Guest Model - Represents Guests/Musallees (GST-XXXX)
 * 
 * Firebase Path: /guests/{guestId}
 * ID Format: GST-XXXX (auto-generated)
 */
public class Guest {
    
    private String id;              // GST-XXXX
    private String name;            // Full name
    private String phone;           // Phone number
    private String email;           // Email (optional)
    private String type;            // regular, visitor, new_muslim
    private String firstVisitDate;  // ISO date
    private String lastVisitDate;   // ISO date
    private int visitCount;         // Number of visits
    private String notes;           // Additional notes
    private long createdAt;         // Timestamp
    private long updatedAt;         // Timestamp

    // Required empty constructor for Firebase
    public Guest() {
    }

    public Guest(String name, String phone, String type) {
        this.name = name;
        this.phone = phone;
        this.type = type != null ? type : "regular";
        this.visitCount = 1;
        this.createdAt = System.currentTimeMillis();
        this.updatedAt = System.currentTimeMillis();
    }

    // Getters and Setters
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getFirstVisitDate() {
        return firstVisitDate;
    }

    public void setFirstVisitDate(String firstVisitDate) {
        this.firstVisitDate = firstVisitDate;
    }

    public String getLastVisitDate() {
        return lastVisitDate;
    }

    public void setLastVisitDate(String lastVisitDate) {
        this.lastVisitDate = lastVisitDate;
    }

    public int getVisitCount() {
        return visitCount;
    }

    public void setVisitCount(int visitCount) {
        this.visitCount = visitCount;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public long getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(long createdAt) {
        this.createdAt = createdAt;
    }

    public long getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(long updatedAt) {
        this.updatedAt = updatedAt;
    }

    /**
     * Get display ID with GST- prefix
     */
    public String getDisplayId() {
        if (id != null && !id.startsWith("GST-")) {
            return "GST-" + id;
        }
        return id;
    }

    /**
     * Get type display name
     */
    public String getTypeDisplay() {
        if (type == null) return "Regular";
        switch (type.toLowerCase()) {
            case "visitor":
                return "Visitor";
            case "new_muslim":
                return "New Muslim";
            case "regular":
            default:
                return "Regular Musallee";
        }
    }

    /**
     * Get type color resource
     */
    public int getTypeColor() {
        if (type == null) return android.R.color.holo_blue_dark;
        switch (type.toLowerCase()) {
            case "visitor":
                return android.R.color.holo_orange_dark;
            case "new_muslim":
                return android.R.color.holo_green_dark;
            case "regular":
            default:
                return android.R.color.holo_blue_dark;
        }
    }

    /**
     * Convert to Map for Firebase update
     */
    public java.util.Map<String, Object> toMap() {
        java.util.Map<String, Object> map = new java.util.HashMap<>();
        map.put("name", name);
        map.put("phone", phone);
        map.put("email", email);
        map.put("type", type);
        map.put("notes", notes);
        map.put("firstVisitDate", firstVisitDate);
        map.put("lastVisitDate", lastVisitDate);
        map.put("visitCount", visitCount);
        map.put("updatedAt", System.currentTimeMillis());
        return map;
    }
}
