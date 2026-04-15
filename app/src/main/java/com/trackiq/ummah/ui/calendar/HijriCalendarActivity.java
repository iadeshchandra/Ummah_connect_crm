package com.trackiq.ummah.ui.calendar;

import android.os.Bundle;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.github.msarhan.ummalqura.calendar.UmmalquraCalendar;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.trackiq.ummah.R;
import com.trackiq.ummah.databinding.ActivityHijriCalendarBinding;
import com.trackiq.ummah.model.CalendarEvent;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.Locale;

/**
 * HijriCalendarActivity - Hijri/Gregorian calendar with events
 * 
 * Features:
 * - Dual calendar display (Hijri and Gregorian)
 * - Prayer time events
 * - Masjid-specific events
 * - Monthly navigation
 */
public class HijriCalendarActivity extends AppCompatActivity {

    private ActivityHijriCalendarBinding binding;
    private DatabaseReference eventsRef;
    private UmmalquraCalendar hijriCal;
    private GregorianCalendar gregorianCal;
    private SimpleDateFormat hijriFormat;
    private SimpleDateFormat gregorianFormat;
    private List<CalendarEvent> events;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityHijriCalendarBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        eventsRef = FirebaseDatabase.getInstance().getReference("calendar_events");
        
        // Initialize calendars
        hijriCal = new UmmalquraCalendar();
        gregorianCal = new GregorianCalendar();
        
        hijriFormat = new SimpleDateFormat("MMMM yyyy", new Locale("ar"));
        gregorianFormat = new SimpleDateFormat("MMMM yyyy", Locale.US);
        
        events = new ArrayList<>();

        setupToolbar();
        setupCalendar();
        loadEvents();
    }

    private void setupToolbar() {
        setSupportActionBar(binding.toolbar);
        getSupportActionBar().setTitle("Hijri Calendar");
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        binding.toolbar.setNavigationOnClickListener(v -> finish());
    }

    private void setupCalendar() {
        updateCalendarDisplay();
        
        // Navigation buttons
        binding.btnPrevMonth.setOnClickListener(v -> {
            hijriCal.add(Calendar.MONTH, -1);
            gregorianCal.add(Calendar.MONTH, -1);
            updateCalendarDisplay();
            loadEvents();
        });
        
        binding.btnNextMonth.setOnClickListener(v -> {
            hijriCal.add(Calendar.MONTH, 1);
            gregorianCal.add(Calendar.MONTH, 1);
            updateCalendarDisplay();
            loadEvents();
        });
        
        binding.btnToday.setOnClickListener(v -> {
            hijriCal = new UmmalquraCalendar();
            gregorianCal = new GregorianCalendar();
            updateCalendarDisplay();
            loadEvents();
        });
    }

    private void updateCalendarDisplay() {
        // Update month headers
        binding.tvHijriMonth.setText(hijriFormat.format(hijriCal.getTime()));
        binding.tvGregorianMonth.setText(gregorianFormat.format(gregorianCal.getTime()));
        
        // Update date info
        SimpleDateFormat dateFormat = new SimpleDateFormat("EEEE, MMMM dd", Locale.US);
        binding.tvTodayGregorian.setText("Gregorian: " + dateFormat.format(gregorianCal.getTime()));
        
        // Hijri date
        String hijriDate = String.format(Locale.US, "%d %s %d",
                hijriCal.get(Calendar.DAY_OF_MONTH),
                hijriCal.getDisplayName(Calendar.MONTH, Calendar.LONG, Locale.US),
                hijriCal.get(Calendar.YEAR));
        binding.tvTodayHijri.setText("Hijri: " + hijriDate);
    }

    private void loadEvents() {
        // Load prayer times and events for current month
        String monthKey = new SimpleDateFormat("yyyy-MM", Locale.US).format(gregorianCal.getTime());
        
        eventsRef.child(monthKey).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                events.clear();
                
                // Add default prayer times
                addDefaultPrayerTimes();
                
                // Add custom events from Firebase
                for (DataSnapshot child : snapshot.getChildren()) {
                    CalendarEvent event = child.getValue(CalendarEvent.class);
                    if (event != null) {
                        events.add(event);
                    }
                }
                
                displayEvents();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                // Use default prayer times only
                addDefaultPrayerTimes();
                displayEvents();
            }
        });
    }

    private void addDefaultPrayerTimes() {
        // Default daily prayer times (would be calculated based on location in production)
        String[] prayers = {"Fajr", "Dhuhr", "Asr", "Maghrib", "Isha"};
        String[] times = {"5:30 AM", "1:15 PM", "4:45 PM", "6:30 PM", "7:45 PM"};
        
        for (int i = 0; i < prayers.length; i++) {
            CalendarEvent event = new CalendarEvent();
            event.setTitle(prayers[i]);
            event.setTime(times[i]);
            event.setType("prayer");
            events.add(event);
        }
        
        // Add Jumuah for Fridays
        if (gregorianCal.get(Calendar.DAY_OF_WEEK) == Calendar.FRIDAY) {
            CalendarEvent jumuah = new CalendarEvent();
            jumuah.setTitle("Jumuah Prayer");
            jumuah.setTime("1:00 PM");
            jumuah.setType("jumuah");
            events.add(jumuah);
        }
    }

    private void displayEvents() {
        // Display events in RecyclerView or list
        StringBuilder sb = new StringBuilder();
        for (CalendarEvent event : events) {
            sb.append("• ").append(event.getTitle())
              .append(" - ").append(event.getTime()).append("\n");
        }
        binding.tvEvents.setText(sb.toString());
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        binding = null;
    }
}
