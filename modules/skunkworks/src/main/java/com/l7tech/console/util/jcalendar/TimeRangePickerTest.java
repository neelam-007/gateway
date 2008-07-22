/**
 * Copyright (C) 2007 Layer 7 Technologies Inc.
 */
package com.l7tech.console.util.jcalendar;

import com.l7tech.gui.util.Utilities;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.event.*;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Locale;
import java.util.TimeZone;

/**
 * Tests and demonstrates {@link TimeRangePicker}.
 *
 * @author rmak
 * @since SecureSpan 4.2
 */
public class TimeRangePickerTest extends JFrame {
    private JPanel contentPane;
    private JPanel _timeRangePickerPanel;
    private final TimeRangePicker _timeRangePicker;
    private JComboBox _localeComboBox;
    private JButton _getLocaleButton;
    private JButton _setLocaleButton;
    private JTextField _timeZoneTextField;
    private JLabel _timeZoneListenerLabel;
    private JButton _getTimeZoneButton;
    private JButton _setTimeZoneButton;
    private JTextField _startTimeTextField;
    private JLabel _startTimeTimeZoneLabel;
    private JLabel _startTimeListenerLabel;
    private JButton _getStartTimeButton;
    private JButton _setStartTimeButton;
    private JTextField _endTimeTextField;
    private JLabel _endTimeTimeZoneLabel;
    private JLabel _endTimeListenerLabel;
    private JButton _getEndTimeButton;
    private JButton _setEndTimeButton;
    private JButton _packButton;

    private static String DATE_TIME_FORMAT = "yyyy-MM-dd hh:mm:ss a";
    private static String DATE_TIME_FORMAT_WITH_ZONE = "yyyy-MM-dd hh:mm:ss.SSS a Z";

    /** Current time zone from {@link #_timeZoneTextField}. */
    private TimeZone _textFieldTimeZone;

    public TimeRangePickerTest() {
        setTitle("TimeRangePicker Test");
        setContentPane(contentPane);

        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent e) {
                dispose();
            }
        });

        // Adds menu bar.
        final JMenuBar menuBar = new JMenuBar();
        setJMenuBar(menuBar);

        // Adds Look & Feel menu.
        final JMenu lafMenu = new JMenu("Look & Feel");
        menuBar.add(lafMenu);
        final ActionListener lafActionListener = new ActionListener() {
            public void actionPerformed(ActionEvent event) {
                try {
                    UIManager.setLookAndFeel(event.getActionCommand());
                    SwingUtilities.updateComponentTreeUI(TimeRangePickerTest.this);
                    pack();
                } catch (ClassNotFoundException e) {
                } catch (InstantiationException e) {
                } catch (IllegalAccessException e) {
                } catch (UnsupportedLookAndFeelException e) {
                }
            }
        };
        final ButtonGroup lafButtonGroup = new ButtonGroup();
        for (UIManager.LookAndFeelInfo lafInfo : UIManager.getInstalledLookAndFeels()) {
            final JRadioButtonMenuItem menuItem = new JRadioButtonMenuItem(lafInfo.getName());
            menuItem.setActionCommand(lafInfo.getClassName());
            menuItem.addActionListener(lafActionListener);
            lafMenu.add(menuItem);
            lafButtonGroup.add(menuItem);
            if (lafInfo.getClassName().equals(UIManager.getSystemLookAndFeelClassName())) {
                // Because menuItem.setSelected(true) does not trigger action events, we have to do it this way:
                menuItem.setSelected(false);
                menuItem.doClick();
            }
        }

        // Cannot use nested form because ant target "compile-test-forms" does not include source tree "src".
        _timeRangePicker = new TimeRangePicker();
        _timeRangePicker.addPropertyChangeListener(new PropertyChangeListener() {
            public void propertyChange(PropertyChangeEvent evt) {
                final TimeZone timeZone = _timeRangePicker.getTimeZone();
                final SimpleDateFormat fmt = new SimpleDateFormat(DATE_TIME_FORMAT_WITH_ZONE);
                fmt.setTimeZone(timeZone);
                if ("startTime".equals(evt.getPropertyName())) {
                    _startTimeListenerLabel.setText(fmt.format(_timeRangePicker.getStartTime()));
                } else if ("endTime".equals(evt.getPropertyName())) {
                    _endTimeListenerLabel.setText(fmt.format(_timeRangePicker.getEndTime()));
                } else if ("timeZone".equals(evt.getPropertyName())) {
                    _timeZoneListenerLabel.setText(timeZone.getID());
                    _startTimeListenerLabel.setText(fmt.format(_timeRangePicker.getStartTime()));
                    _endTimeListenerLabel.setText(fmt.format(_timeRangePicker.getEndTime()));
                }
            }
        });

        _timeRangePickerPanel.add(_timeRangePicker.mainPanel);

        final Locale[] locales = Locale.getAvailableLocales();
        Arrays.sort(locales, new Comparator<Locale>() {
            public int compare(Locale a, Locale b) {
                return a.toString().compareTo(b.toString());
            }
        });
        for (Locale locale : locales) {
            final LocaleItem localeItem = new LocaleItem(locale);
            _localeComboBox.addItem(localeItem);
        }
        _localeComboBox.setSelectedIndex(-1);

        _getLocaleButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                for (int i = 0; i < _localeComboBox.getItemCount(); ++ i) {
                    final LocaleItem item = (LocaleItem)_localeComboBox.getItemAt(i);
                    if (item.getLocale().equals(_timeRangePicker.getLocale())) {
                        _localeComboBox.setSelectedItem(item);
                    }
                }
            }
        });
        _setLocaleButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                _timeRangePicker.setLocale(((LocaleItem)_localeComboBox.getSelectedItem()).getLocale());
            }
        });

        parseTextFieldTimeZone();
        _timeZoneTextField.addKeyListener(new KeyAdapter() {
            public void keyReleased(KeyEvent e) {
                super.keyReleased(e);
                if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                    parseTextFieldTimeZone();
                }
            }
        });
        _timeZoneTextField.addFocusListener(new FocusAdapter() {
            public void focusLost(FocusEvent e) {
                super.focusLost(e);
                parseTextFieldTimeZone();
            }
        });
        _timeZoneTextField.getDocument().addDocumentListener(new DocumentListener() {
            public void insertUpdate(DocumentEvent e) {
                parseTextFieldTimeZone();
            }
            public void removeUpdate(DocumentEvent e) {
                parseTextFieldTimeZone();
            }
            public void changedUpdate(DocumentEvent e) {
                parseTextFieldTimeZone();
            }
        });

        _getTimeZoneButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                _timeZoneTextField.setText(_timeRangePicker.getTimeZone().getID());
            }
        });
        _setTimeZoneButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                _timeRangePicker.setTimeZone(TimeZone.getTimeZone(_timeZoneTextField.getText()), true);
            }
        });

        _getStartTimeButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent evt) {
                final SimpleDateFormat fmt = new SimpleDateFormat(DATE_TIME_FORMAT);
                fmt.setTimeZone(TimeZone.getTimeZone(_timeZoneTextField.getText()));
                _startTimeTextField.setText(fmt.format(_timeRangePicker.getStartTime()));
            }
        });
        _setStartTimeButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent evt) {
                final SimpleDateFormat fmt = new SimpleDateFormat(DATE_TIME_FORMAT);
                fmt.setTimeZone(_textFieldTimeZone);
                try {
                    _timeRangePicker.setStartTime(fmt.parse(_startTimeTextField.getText()));
                } catch (ParseException e) {
                    JOptionPane.showMessageDialog(TimeRangePickerTest.this, e.toString(), "Start Time Error", JOptionPane.ERROR_MESSAGE);
                }
            }
        });

        _getEndTimeButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent evt) {
                final SimpleDateFormat fmt = new SimpleDateFormat(DATE_TIME_FORMAT);
                fmt.setTimeZone(_textFieldTimeZone);
                _endTimeTextField.setText(fmt.format(_timeRangePicker.getEndTime()));
            }
        });
        _setEndTimeButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent evt) {
                final SimpleDateFormat fmt = new SimpleDateFormat(DATE_TIME_FORMAT);
                fmt.setTimeZone(_textFieldTimeZone);
                try {
                    _timeRangePicker.setEndTime(fmt.parse(_endTimeTextField.getText()));
                } catch (ParseException e) {
                    JOptionPane.showMessageDialog(TimeRangePickerTest.this, e.toString(), "End Time Error", JOptionPane.ERROR_MESSAGE);
                }
            }
        });

        _packButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                TimeRangePickerTest.this.pack();
            }
        });
    }

    private void parseTextFieldTimeZone() {
        _textFieldTimeZone = TimeZone.getTimeZone(_timeZoneTextField.getText());
        final String timeZoneStr = formatTimeZone(_textFieldTimeZone);
        _startTimeTimeZoneLabel.setText(timeZoneStr);
        _endTimeTimeZoneLabel.setText(timeZoneStr);
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                TimeRangePickerTest frame = new TimeRangePickerTest();
                frame.pack();
                Utilities.centerOnScreen(frame);
                frame.setVisible(true);
                frame.toFront();
            }
        });
    }

    private static String formatTimeZone(TimeZone timeZone) {
        final StringBuilder sb = new StringBuilder();
        sb.append(formatTimeZoneOffset(timeZone.getRawOffset()));
        if (timeZone.useDaylightTime()) {
            sb.append("/");
            sb.append(formatTimeZoneOffset(timeZone.getRawOffset() + timeZone.getDSTSavings()));
        }
        return sb.toString();
    }

    private static String formatTimeZoneOffset(final int offset) {
        int tmp = Math.abs(offset);
        final int hours = tmp / 3600000;
        final int minutes = (tmp - hours * 3600000) / 60000;
        return String.format("%c%02d:%02d", (offset >= 0 ? '+' : '-'), hours, minutes);
    }

    private static class LocaleItem{
        private final Locale _locale;
        public LocaleItem(final Locale locale) {
            _locale = locale;
        }
        public Locale getLocale() {
            return _locale;
        }
        public String toString() {
            return _locale.getDisplayName(_locale);
        }
    }
}
