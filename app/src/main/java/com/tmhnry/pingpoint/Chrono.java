package com.tmhnry.pingpoint;

import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

public class Chrono {
    public static Chrono instance;
    private Calendar calendar;

    public Chrono(Locale locale) {
        calendar = Calendar.getInstance(locale);
    }

    public static Chrono Create(Locale locale) {
        if (instance == null) {
            instance = new Chrono(locale);
        }
        return instance;
    }

    public static Date now(){
        return instance.calendar.getTime();
    }
}
