package com.mahal.model;

import java.time.LocalDate;
import java.time.LocalTime;

/**
 * Stores daily prayer times for a specific date.
 * All dates are interpreted in Indian Standard Time (IST).
 */
public class PrayerTime {
    private Long id;
    private String userId;
    private LocalDate date;
    private LocalTime fajr;
    private LocalTime sunrise;
    private LocalTime dhuhr;
    private LocalTime asr;
    private LocalTime maghrib;
    private LocalTime isha;

    public PrayerTime() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public LocalDate getDate() {
        return date;
    }

    public void setDate(LocalDate date) {
        this.date = date;
    }

    public LocalTime getFajr() {
        return fajr;
    }

    public void setFajr(LocalTime fajr) {
        this.fajr = fajr;
    }

    public LocalTime getSunrise() {
        return sunrise;
    }

    public void setSunrise(LocalTime sunrise) {
        this.sunrise = sunrise;
    }

    public LocalTime getDhuhr() {
        return dhuhr;
    }

    public void setDhuhr(LocalTime dhuhr) {
        this.dhuhr = dhuhr;
    }

    public LocalTime getAsr() {
        return asr;
    }

    public void setAsr(LocalTime asr) {
        this.asr = asr;
    }

    public LocalTime getMaghrib() {
        return maghrib;
    }

    public void setMaghrib(LocalTime maghrib) {
        this.maghrib = maghrib;
    }

    public LocalTime getIsha() {
        return isha;
    }

    public void setIsha(LocalTime isha) {
        this.isha = isha;
    }
}
