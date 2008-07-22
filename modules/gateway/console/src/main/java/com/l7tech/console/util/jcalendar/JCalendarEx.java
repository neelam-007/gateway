/**
 * Copyright (C) 2007 Layer 7 Technologies Inc.
 */
package com.l7tech.console.util.jcalendar;

import com.toedter.calendar.JCalendar;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

/**
 * Extension of {@link JCalendar} bean with addition of a "Today" button and time zone setter/getter.
 *
 * @rmak
 * @since SecureSpan 4.2
 */
public class JCalendarEx extends JCalendar {

    protected JButton todayButton;

    protected boolean todayButtonVisible = true;

    public JCalendarEx() {
        this(null, null, true, false);
    }

    public JCalendarEx(Date date) {
        this(date, null, true, false);
    }

    public JCalendarEx(Date date, Locale locale, boolean monthSpinner, boolean weekOfYearVisible) {
        super(date, locale, monthSpinner, weekOfYearVisible);

        setName("JCalendarEx");

        todayButton = new JButton();
        setTodayButtonLocale(getLocale());
        todayButton.setVisible(todayButtonVisible);
        todayButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                setDate(new Date());
            }
        });

        final Box todayBox = Box.createHorizontalBox();
        todayBox.add(Box.createHorizontalGlue());
        todayBox.add(todayButton);
        todayBox.add(Box.createHorizontalGlue());

        add(todayBox, BorderLayout.SOUTH);
    }

    public boolean isTodayButtonVisible() {
        return todayButtonVisible;
    }

    public void setTodayButtonVisible(boolean b) {
        this.todayButtonVisible = b;
        todayButton.setVisible(b);
    }

    public void setTodayButtonLocale(Locale locale) {
        String text = "Today";  // Default
        if (locale.getLanguage().equals("ar")) {
            text = "\u0627\u0644\u064a\u0648\u0645";
        } else if (locale.getLanguage().equals("bg")) {
            text = "\u0414\u043d\u0435\u0441";
        } else if (locale.getLanguage().equals("zh")) {
            if (locale.getCountry().equals("CN")) {
                text = "\u4eca\u5929";
            } else {
                text = "\u4eca\u5929";
            }
        } else if (locale.getLanguage().equals("cs")) {
            text = "Dnes";
        } else if (locale.getLanguage().equals("da")) {
            text = "I dag";
        } else if (locale.getLanguage().equals("nl")) {
            text = "Vandaag";
        } else if (locale.getLanguage().equals("en")) {
            text = "Today";
        } else if (locale.getLanguage().equals("et")) {
            text = "T\u00e4na";
        } else if (locale.getLanguage().equals("fi")) {
            text = "T\u00e4n\u00e4\u00e4n";
        } else if (locale.getLanguage().equals("fr")) {
            text = "Aujourd'hui";
        } else if (locale.getLanguage().equals("de")) {
            text = "Heute um";
        } else if (locale.getLanguage().equals("el")) {
            text = "\u03a3\u03ae\u03bc\u03b5\u03c1\u03b1";
        } else if (locale.getLanguage().equals("he")) {
            text = "\u05d4\u05d9\u05d5\u05dd";
        } else if (locale.getLanguage().equals("hu")) {
            text = "Ma";
        } else if (locale.getLanguage().equals("it")) {
            text = "Oggi";
        } else if (locale.getLanguage().equals("ja")) {
            text = "\u4eca\u65e5";
        } else if (locale.getLanguage().equals("ko")) {
            text = "\uc624\ub298";
        } else if (locale.getLanguage().equals("lt")) {
            text = "\u0160iandien";
        } else if (locale.getLanguage().equals("no")) {
            text = "I dag";
        } else if (locale.getLanguage().equals("pl")) {
            text = "Dzisiaj";
        } else if (locale.getLanguage().equals("pt")) {
            if (locale.getCountry().equals("BR")) {
                text = "Hoje";
            } else if (locale.getCountry().equals("PT")) {
                text = "Hoje";
            } else {
                text = "Hoje";
            }
        } else if (locale.getLanguage().equals("ro")) {
            text = "Ast\u0103zi";
        } else if (locale.getLanguage().equals("ru")) {
            text = "\u0421\u0435\u0433\u043e\u0434\u043d\u044f";
        } else if (locale.getLanguage().equals("es")) {
            text = "Hoy";
        } else if (locale.getLanguage().equals("sv")) {
            text = "Idag";
        } else if (locale.getLanguage().equals("tr")) {
            text = "Bug\u00fcn";
        }
        todayButton.setText(text);
    }

    public TimeZone getTimeZone() {
        return getCalendar().getTimeZone();
    }

    public void setTimeZone(TimeZone timeZone) {
        final Calendar calendar = getCalendar();
        calendar.setTimeZone(timeZone);
        setCalendar(calendar);
    }

    @Override
    public void setLocale(Locale locale) {
        super.setLocale(locale);
        setTodayButtonLocale(locale);
    }
}
