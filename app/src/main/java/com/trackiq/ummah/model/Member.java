package com.trackiq.ummah.model;

import com.google.firebase.database.Exclude;
import java.util.HashMap;
import java.util.Map;

public class Member {

    // Internal ID (Firebase Node Key) - Excluded from being saved as a duplicate field
    private String id; 
    
    // Database Fields
    private String displayId; // e.g., MBR-A1B2
    private String name;
    private String phone;
    private String email;
    private String address;
    private String notes;
    private String status;
    private long createdAt;
    private long updatedAt;
    private String joinDate;

    // Required empty constructor for Firebase DataSnapshot deserialization
    public Member() {
    }

    // --- GETTERS & SETTERS ---

    @Exclude
    public String getId() { return id; }
    @Exclude
    public void setId(String id) { this.id = id; }

    public String getDisplayId() { return displayId; }
    public void setDisplayId(String displayId) { this.displayId = displayId; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public long getCreatedAt() { return createdAt; }
    public void setCreatedAt(long createdAt) { this.createdAt = createdAt; }

    public long getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(long updatedAt) { this.updatedAt = updatedAt; }

    public String getJoinDate() { return joinDate; }
    public void setJoinDate(String joinDate) { this.joinDate = joinDate; }

    // --- FIREBASE UPDATE MAPPING ---
    
    @Exclude
    public Map<String, Object> toMap() {
        HashMap<String, Object> result = new HashMap<>();
        result.put("displayId", displayId);
        result.put("name", name);
        result.put("phone", phone);
        result.put("email", email);
        result.put("address", address);
        result.put("notes", notes);
        result.put("status", status);
        result.put("createdAt", createdAt);
        result.put("updatedAt", updatedAt);
        result.put("joinDate", joinDate);
        return result;
    }
}
