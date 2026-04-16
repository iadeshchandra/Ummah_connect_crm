package com.trackiq.ummah.model;

/**
 * Member Model - Represents a Masjid Member (MBR-XXXX)
 * 
 * Firebase Path: /members/{memberId}
 * ID Format: MBR-XXXX (auto-generated)
 */
public class Member {
    
    private String id;              // MBR-XXXX
    private String name;            // Full name
    private String phone;           // Phone number
    private String email;           // Email (optional)
    private String address;         // Address
    private String status;          // active, inactive, vip
    private String joinDate;        // ISO date
    private long createdAt;         // Timestamp
    private long updatedAt;         // Timestamp
    private String notes;           // Additional notes

    // Required empty constructor for Firebase
    public Member() {
    }

    public Member(String name, String phone, String email, String address) {
        this.name = name;
        this.phone = phone;
        this.email = email;
        this.address = address;
        this.status = "active";
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

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getJoinDate() {
        return joinDate;
    }

    public void setJoinDate(String joinDate) {
        this.joinDate = joinDate;
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

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    /**
     * Get display ID with MBR- prefix
     */
    public String getDisplayId() {
        if (id != null && !id.startsWith("MBR-")) {
            return "MBR-" + id;
        }
        return id;
    }

    /**
     * Get status color resource
     */
    public int getStatusColor() {
        switch (status != null ? status.toLowerCase() : "active") {
            case "active":
                return android.R.color.holo_green_dark;
            case "inactive":
                return android.R.color.darker_gray;
            case "vip":
                return android.R.color.holo_orange_dark;
            default:
                return android.R.color.holo_green_dark;
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
        map.put("address", address);
        map.put("status", status);
        map.put("notes", notes);
        map.put("joinDate", joinDate);
        map.put("updatedAt", System.currentTimeMillis());
        return map;
    }
}
