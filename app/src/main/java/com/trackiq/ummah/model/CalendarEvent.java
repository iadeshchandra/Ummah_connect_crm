package com.trackiq.ummah.model;

/**
 * CalendarEvent Model - Events for Hijri/Gregorian calendar
 * 
 * Firebase Path: /calendar_events/{yyyy-MM}/{eventId}
 */
public class CalendarEvent {
    
    public static final String TYPE_PRAYER = "prayer";
    public static final String TYPE_JUMUAH = "jumuah";
    public static final String TYPE_EID = "eid";
    public static final String TYPE_IFTAR = "iftar";
    public static final String TYPE_CUSTOM = "custom";
    
    private String id;
    private String title;           // Event title
    private String description;     // Event description
    private String date;            // ISO date (yyyy-MM-dd)
    private String time;            // Time string (HH:mm)
    private String type;            // prayer, jumuah, eid, iftar, custom
    private String location;        // Location (optional)
    private long timestamp;         // Created timestamp
    private String createdBy;       // Who created the event

    // Required empty constructor for Firebase
    public CalendarEvent() {
    }

    public CalendarEvent(String title, String date, String time, String type) {
        this.title = title;
        this.date = date;
        this.time = time;
        this.type = type;
        this.timestamp = System.currentTimeMillis();
    }

    // Getters and Setters
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
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

    public String getTime() {
        return time;
    }

    public void setTime(String time) {
        this.time = time;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    public String getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(String createdBy) {
        this.createdBy = createdBy;
    }

    /**
     * Get icon based on type
     */
    public String getIcon() {
        switch (type != null ? type : TYPE_CUSTOM) {
            case TYPE_PRAYER:
                return "🕐";
            case TYPE_JUMUAH:
                return "🕌";
            case TYPE_EID:
                return "🎉";
            case TYPE_IFTAR:
                return "🍽";
            default:
                return "📅";
        }
    }

    /**
     * Get color resource based on type
     */
    public int getColorResource() {
        switch (type != null ? type : TYPE_CUSTOM) {
            case TYPE_PRAYER:
                return android.R.color.holo_blue_dark;
            case TYPE_JUMUAH:
                return android.R.color.holo_green_dark;
            case TYPE_EID:
                return android.R.color.holo_orange_dark;
            case TYPE_IFTAR:
                return android.R.color.holo_purple;
            default:
                return android.R.color.darker_gray;
        }
    }
}
