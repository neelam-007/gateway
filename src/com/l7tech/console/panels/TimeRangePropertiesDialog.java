package com.l7tech.console.panels;

import com.l7tech.console.action.Actions;
import com.l7tech.policy.assertion.TimeRange;
import com.l7tech.policy.assertion.TimeOfDayRange;
import com.l7tech.policy.assertion.TimeOfDay;

import javax.swing.*;
import javax.swing.event.ChangeListener;
import javax.swing.event.ChangeEvent;
import java.awt.*;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import java.util.*;

/**
 * Dialog to view or edit the properties of a TimeRange assertion.
 *
 * <br/><br/>
 * LAYER 7 TECHNOLOGIES, INC<br/>
 * User: flascell<br/>
 * Date: Feb 16, 2004<br/>
 * $Id$<br/>
 *
 */
public class TimeRangePropertiesDialog extends JDialog {

    public TimeRangePropertiesDialog(Frame owner, boolean modal, TimeRange assertion) {
        super(owner, modal);
        this.assertion = assertion;
        initialize();
    }

    private void ok() {
        // todo, validate and save data
        cancel();
    }

    private void cancel() {
        TimeRangePropertiesDialog.this.dispose();
    }

    /**
     * enable disable day of week controls
     */
    private void toggleDayOfWeek() {
        boolean enable = false;
        if (enableDayOfWeek.isSelected()) {
            enable = true;
        }
        for (Iterator iterator = itemsToToggleForDayOfWeek.iterator(); iterator.hasNext();) {
            JComponent component = (JComponent) iterator.next();
            component.setEnabled(enable);
        }
    }

    /**
     * enable disable time of day controls
     */
    private void toggleTimeOfDay() {
        boolean enable = false;
        if (enableTimeOfDay.isSelected()) {
            enable = true;
        }
        for (Iterator iterator = itemsToToggleForTimeOfDay.iterator(); iterator.hasNext();) {
            JComponent component = (JComponent) iterator.next();
            component.setEnabled(enable);
        }
    }

    private void refreshStartUTCLabel() {
        // calculate corresponding UTC start time
        SpinnerNumberModel spinModel = (SpinnerNumberModel)startHr.getModel();
        int localhr = spinModel.getNumber().intValue();
        spinModel = (SpinnerNumberModel)startMin.getModel();
        int localmin = spinModel.getNumber().intValue();
        spinModel = (SpinnerNumberModel)startSec.getModel();
        int localsec = spinModel.getNumber().intValue();
        int utchr = localhr - hroffset;
        int utcmin = localmin - minoffset;
        while (utcmin >= 60) {
            ++utchr;
            utcmin -= 60;
        }
        while (utchr >= 24) {
            utchr -= 24;
        }
        String utclabelvalue = (utchr < 10 ? "0" : "") + utchr +
                               (utcmin < 10 ? ":0" : ":") + utcmin +
                               (localsec < 10 ? ":0" : ":") + localsec;
        utcStartTime.setText(utclabelvalue);
    }

    private void refreshEndUTCLabel() {
        // calculate corresponding UTC end time
        SpinnerNumberModel spinModel = (SpinnerNumberModel)endHr.getModel();
        int localhr = spinModel.getNumber().intValue();
        spinModel = (SpinnerNumberModel)endMin.getModel();
        int localmin = spinModel.getNumber().intValue();
        spinModel = (SpinnerNumberModel)endSec.getModel();
        int localsec = spinModel.getNumber().intValue();
        int utchr = localhr - hroffset;
        int utcmin = localmin - minoffset;
        while (utcmin >= 60) {
            ++utchr;
            utcmin -= 60;
        }
        while (utchr >= 24) {
            utchr -= 24;
        }
        String utclabelvalue = (utchr < 10 ? "0" : "") + utchr +
                               (utcmin < 10 ? ":0" : ":") + utcmin +
                               (localsec < 10 ? ":0" : ":") + localsec;
        utcEndTime.setText(utclabelvalue);
    }

    /**
     * create controls and layout
     */
    private void initialize() {
        initResources();

        itemsToToggleForTimeOfDay.clear();
        itemsToToggleForDayOfWeek.clear();

        // calculate UTC offset
        int totOffsetInMin = Calendar.getInstance().getTimeZone().getRawOffset() / (1000*60);
        hroffset = totOffsetInMin/60;
        minoffset = totOffsetInMin%60;

        week = new String[] {resources.getString("week.sunday"),
                             resources.getString("week.monday"),
                             resources.getString("week.tuesday"),
                             resources.getString("week.wednesday"),
                             resources.getString("week.thursday"),
                             resources.getString("week.friday"),
                             resources.getString("week.saturday")};

        setTitle(resources.getString("window.title"));
        Container contents = getContentPane();
        contents.setLayout(new BorderLayout(0,0));
        contents.add(makeGlobalPanel(), BorderLayout.CENTER);
        contents.add(makeBottomButtonsPanel(), BorderLayout.SOUTH);

        // create callbacks
        okButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                ok();
            }
        });
        cancelButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                cancel();
            }
        });
        helpButton.addActionListener( new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                Actions.invokeHelp(TimeRangePropertiesDialog.this);
            }
        });

        enableTimeOfDay.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                toggleTimeOfDay();
            }
        });

        enableDayOfWeek.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                toggleDayOfWeek();
            }
        });
        startHr.addChangeListener(new ChangeListener() {
            public void stateChanged(ChangeEvent e) {
                refreshStartUTCLabel();
            }
        });
        endHr.addChangeListener(new ChangeListener() {
            public void stateChanged(ChangeEvent e) {
                refreshEndUTCLabel();
            }
        });
        startMin.addChangeListener(new ChangeListener() {
            public void stateChanged(ChangeEvent e) {
                refreshStartUTCLabel();
            }
        });
        endMin.addChangeListener(new ChangeListener() {
            public void stateChanged(ChangeEvent e) {
                refreshEndUTCLabel();
            }
        });
        startSec.addChangeListener(new ChangeListener() {
            public void stateChanged(ChangeEvent e) {
                refreshStartUTCLabel();
            }
        });
        endSec.addChangeListener(new ChangeListener() {
            public void stateChanged(ChangeEvent e) {
                refreshEndUTCLabel();
            }
        });

        setValuesToAssertion();
    }

    /**
     * use values stored in this.assertion to populate controls of the dialog.
     */
    private void setValuesToAssertion() {
        if (assertion != null) {
            enableTimeOfDay.setSelected(assertion.isControlTime());
            enableDayOfWeek.setSelected(assertion.isControlDay());
            int day = assertion.getStartDayOfWeek();
            SpinnerListModel spinModel = (SpinnerListModel)startDay.getModel();
            switch (day) {
                case Calendar.SUNDAY:
                    spinModel.setValue(week[0]);
                    break;
                case Calendar.MONDAY:
                    spinModel.setValue(week[1]);
                    break;
                case Calendar.TUESDAY:
                    spinModel.setValue(week[2]);
                    break;
                case Calendar.WEDNESDAY:
                    spinModel.setValue(week[3]);
                    break;
                case Calendar.THURSDAY:
                    spinModel.setValue(week[4]);
                    break;
                case Calendar.FRIDAY:
                    spinModel.setValue(week[5]);
                    break;
                case Calendar.SATURDAY:
                    spinModel.setValue(week[6]);
                    break;
            }
            day = assertion.getEndDayOfWeek();
            spinModel = (SpinnerListModel)endDay.getModel();
            switch (day) {
                case Calendar.SUNDAY:
                    spinModel.setValue(week[0]);
                    break;
                case Calendar.MONDAY:
                    spinModel.setValue(week[1]);
                    break;
                case Calendar.TUESDAY:
                    spinModel.setValue(week[2]);
                    break;
                case Calendar.WEDNESDAY:
                    spinModel.setValue(week[3]);
                    break;
                case Calendar.THURSDAY:
                    spinModel.setValue(week[4]);
                    break;
                case Calendar.FRIDAY:
                    spinModel.setValue(week[5]);
                    break;
                case Calendar.SATURDAY:
                    spinModel.setValue(week[6]);
                    break;
            }
            toggleTimeOfDay();
            toggleDayOfWeek();
            TimeOfDayRange timeRange = assertion.getTimeRange();
            if (timeRange != null) {
                int hr, min, sec;
                hr = timeRange.getFrom().getHour();
                min = timeRange.getFrom().getMinute();
                sec = timeRange.getFrom().getSecond();
                String utclabelvalue = (hr < 10 ? "0" : "") + hr +
                                       (min < 10 ? ":0" : ":") + min +
                                       (sec < 10 ? ":0" : ":") + sec;
                utcStartTime.setText(utclabelvalue);
                hr += hroffset;
                min += minoffset;
                while (min >= 60) {
                    ++hr;
                    min -= 60;
                }
                while (hr >= 24) {
                    hr -= 24;
                }
                while (min < 0) {
                    --hr;
                    min += 60;
                }
                while (hr < 0) {
                    hr += 24;
                }
                SpinnerNumberModel spinNrModel = (SpinnerNumberModel)startHr.getModel();
                spinNrModel.setValue(new Integer(hr));
                spinNrModel = (SpinnerNumberModel)startMin.getModel();
                spinNrModel.setValue(new Integer(min));
                spinNrModel = (SpinnerNumberModel)startSec.getModel();
                spinNrModel.setValue(new Integer(sec));


                hr = timeRange.getTo().getHour();
                min = timeRange.getTo().getMinute();
                sec = timeRange.getTo().getSecond();
                utclabelvalue = (hr < 10 ? "0" : "") + hr +
                                (min < 10 ? ":0" : ":") + min +
                                (sec < 10 ? ":0" : ":") + sec;
                utcEndTime.setText(utclabelvalue);
                hr += hroffset;
                min += minoffset;
                while (min >= 60) {
                    ++hr;
                    min -= 60;
                }
                while (hr >= 24) {
                    hr -= 24;
                }
                while (min < 0) {
                    --hr;
                    min += 60;
                }
                while (hr < 0) {
                    hr += 24;
                }
                spinNrModel = (SpinnerNumberModel)endHr.getModel();
                spinNrModel.setValue(new Integer(hr));
                spinNrModel = (SpinnerNumberModel)endMin.getModel();
                spinNrModel.setValue(new Integer(min));
                spinNrModel = (SpinnerNumberModel)endSec.getModel();
                spinNrModel.setValue(new Integer(sec));
            }
        }
        // display UTC corresponding times
        refreshStartUTCLabel();
        refreshEndUTCLabel();
    }

    private void initResources() {
        Locale locale = Locale.getDefault();
        resources = ResourceBundle.getBundle("com.l7tech.console.resources.TimeRangePropertiesDialog", locale);
    }

    private JComponent makeGlobalPanel() {
        JComponent globalPane = mainPanel();
        return wrapAroundBorders(globalPane, CONTROL_SPACING, CONTROL_SPACING, 0, BORDER_PADDING);
    }

    private JPanel wrapAroundBorders(JComponent src, int top, int left, int bottom, int right) {
        GridBagConstraints constraints = new GridBagConstraints();
        constraints.fill = GridBagConstraints.BOTH;
        constraints.weightx = 1.0;
        constraints.weighty = 1.0;
        constraints.insets = new Insets(top, left, bottom, right);
        JPanel bordered = new JPanel();
        bordered.setLayout(new GridBagLayout());
        bordered.add(src, constraints);
        return bordered;
    }

    private JComponent makeGriddedTimeOfDaySubPanel() {
        JPanel timeOfDayPanel = new JPanel();

        timeOfDayPanel.setLayout(new GridBagLayout());

        double weightx = 0.33;
        int nfill = GridBagConstraints.NONE;
        int fill = GridBagConstraints.HORIZONTAL;
        int dir = GridBagConstraints.WEST;
        Insets insets = new Insets(0, 0, 0, CONTROL_SPACING);

        JLabel toto = new JLabel(resources.getString("general.between"));
        itemsToToggleForTimeOfDay.add(toto);
        timeOfDayPanel.add(toto, new GridBagConstraints(0, 0, 1, 1, 0, 0, dir, nfill, insets, 0, 0));
        startHr = new JSpinner(new SpinnerNumberModel(8, 0, 23, 1));
        itemsToToggleForTimeOfDay.add(startHr);
        itemsToToggleForTimeOfDay.add(startHr);
        timeOfDayPanel.add(startHr, new GridBagConstraints(1, 0, 1, 1, weightx, 0, dir, fill, insets, 0, 0));
        toto = new JLabel(resources.getString("general.hr"));
        itemsToToggleForTimeOfDay.add(toto);
        timeOfDayPanel.add(toto, new GridBagConstraints(2, 0, 1, 1, 0, 0, dir, nfill, insets, 0, 0));
        startMin = new JSpinner(new SpinnerNumberModel(0, 0, 59, 1));
        itemsToToggleForTimeOfDay.add(startMin);
        timeOfDayPanel.add(startMin, new GridBagConstraints(3, 0, 1, 1, weightx, 0, dir, fill, insets, 0, 0));

        toto = new JLabel(resources.getString("general.min"));
        itemsToToggleForTimeOfDay.add(toto);
        timeOfDayPanel.add(toto, new GridBagConstraints(4, 0, 1, 1, 0, 0, dir, nfill, insets, 0, 0));
        startSec = new JSpinner(new SpinnerNumberModel(0, 0, 59, 1));
        itemsToToggleForTimeOfDay.add(startSec);
        timeOfDayPanel.add(startSec, new GridBagConstraints(5, 0, 1, 1, weightx, 0, dir, fill, insets, 0, 0));
        toto = new JLabel(resources.getString("general.sec"));
        itemsToToggleForTimeOfDay.add(toto);
        timeOfDayPanel.add(toto, new GridBagConstraints(6, 0, 1, 1, 0, 0, dir, nfill, insets, 0, 0));

        toto = new JLabel(resources.getString("general.and"));
        itemsToToggleForTimeOfDay.add(toto);
        timeOfDayPanel.add(toto, new GridBagConstraints(0, 1, 1, 1, 0, 0, dir, nfill, insets, 0, 0));
        endHr = new JSpinner(new SpinnerNumberModel(17, 0, 23, 1));
        itemsToToggleForTimeOfDay.add(endHr);
        timeOfDayPanel.add(endHr, new GridBagConstraints(1, 1, 1, 1, weightx, 0, dir, fill, insets, 0, 0));
        toto = new JLabel(resources.getString("general.hr"));
        itemsToToggleForTimeOfDay.add(toto);
        timeOfDayPanel.add(toto, new GridBagConstraints(2, 1, 1, 1, 0, 0, dir, nfill, insets, 0, 0));
        endMin = new JSpinner(new SpinnerNumberModel(0, 0, 59, 1));
        itemsToToggleForTimeOfDay.add(endMin);
        timeOfDayPanel.add(endMin, new GridBagConstraints(3, 1, 1, 1, weightx, 0, dir, fill, insets, 0, 0));
        toto = new JLabel(resources.getString("general.min"));
        itemsToToggleForTimeOfDay.add(toto);
        timeOfDayPanel.add(toto, new GridBagConstraints(4, 1, 1, 1, 0, 0, dir, nfill, insets, 0, 0));
        endSec = new JSpinner(new SpinnerNumberModel(0, 0, 59, 1));
        itemsToToggleForTimeOfDay.add(endSec);
        timeOfDayPanel.add(endSec, new GridBagConstraints(5, 1, 1, 1, weightx, 0, dir, fill, insets, 0, 0));
        toto = new JLabel(resources.getString("general.sec"));
        itemsToToggleForTimeOfDay.add(toto);
        timeOfDayPanel.add(toto, new GridBagConstraints(6, 1, 1, 1, 0, 0, dir, nfill, insets, 0, 0));

        return timeOfDayPanel;
    }

    private JPanel griddedDayOfWeekPanel() {
        JPanel dayOfWeekPanel = new JPanel();
        dayOfWeekPanel.setLayout(new GridBagLayout());

        int nfill = GridBagConstraints.NONE;
        int fill = GridBagConstraints.HORIZONTAL;
        int dir = GridBagConstraints.WEST;
        double weightx = 1.0;
        Insets insets = new Insets(0, 0, 0, CONTROL_SPACING);

        JLabel toto = new JLabel(resources.getString("general.between"));
        itemsToToggleForDayOfWeek.add(toto);
        dayOfWeekPanel.add(toto, new GridBagConstraints(0, 0, 1, 1, 0, 0, dir, nfill, insets, 0, 0));
        startDay = new JSpinner(weekModel(1));
        itemsToToggleForDayOfWeek.add(startDay);
        dayOfWeekPanel.add(startDay, new GridBagConstraints(1, 0, 1, 1, weightx, 0, dir, fill, insets, 0, 0));

        toto = new JLabel(resources.getString("general.and"));
        itemsToToggleForDayOfWeek.add(toto);
        dayOfWeekPanel.add(toto, new GridBagConstraints(0, 1, 1, 1, 0, 0, dir, nfill, insets, 0, 0));
        endDay = new JSpinner(weekModel(5));
        itemsToToggleForDayOfWeek.add(endDay);
        dayOfWeekPanel.add(endDay, new GridBagConstraints(1, 1, 1, 1, weightx, 0, dir, fill, insets, 0, 0));

        return dayOfWeekPanel;
    }

    private JPanel utcConversionPanel() {
        JPanel utcConversionPanel = new JPanel();
        utcConversionPanel.setLayout(new GridBagLayout());

        int nfill = GridBagConstraints.NONE;
        int fill = GridBagConstraints.HORIZONTAL;
        int dir = GridBagConstraints.WEST;
        double weightx = 1.0;
        Insets insets = new Insets(0, 0, 0, CONTROL_SPACING*3);

        JLabel toto = new JLabel(resources.getString("general.between"));
        itemsToToggleForTimeOfDay.add(toto);
        utcConversionPanel.add(toto, new GridBagConstraints(0, 0, 1, 1, 0, 0, dir, nfill, insets, 0, 0));
        utcStartTime = new JLabel("06:00:00");
        itemsToToggleForTimeOfDay.add(utcStartTime);
        utcConversionPanel.add(utcStartTime, new GridBagConstraints(1, 0, 1, 1, 0, 0, dir, nfill, insets, 0, 0));
        toto = new JLabel(resources.getString("utc.label"));
        itemsToToggleForTimeOfDay.add(toto);
        utcConversionPanel.add(toto, new GridBagConstraints(2, 0, 1, 1, weightx, 0, dir, fill, insets, 0, 0));

        toto = new JLabel(resources.getString("general.and"));
        itemsToToggleForTimeOfDay.add(toto);
        utcConversionPanel.add(toto, new GridBagConstraints(0, 1, 1, 1, 0, 0, dir, nfill, insets, 0, 0));
        utcEndTime = new JLabel("21:00:00");
        itemsToToggleForTimeOfDay.add(utcEndTime);
        utcConversionPanel.add(utcEndTime, new GridBagConstraints(1, 1, 1, 1, 0, 0, dir, nfill, insets, 0, 0));
        toto = new JLabel(resources.getString("utc.label"));
        itemsToToggleForTimeOfDay.add(toto);
        utcConversionPanel.add(toto, new GridBagConstraints(2, 1, 1, 1, weightx, 0, dir, fill, insets, 0, 0));

        return utcConversionPanel;
    }

    private JComponent mainPanel() {

        JPanel timeOfDayPanel = new JPanel();
        timeOfDayPanel.setLayout(new GridLayout(6, 1, 0, 0));
        JPanel titlePanel = new JPanel();
        titlePanel.setLayout(new FlowLayout(FlowLayout.LEADING, 0, 0));
        enableTimeOfDay = new JCheckBox(resources.getString("enableTimeOfDay.name"));
        titlePanel.add(enableTimeOfDay);
        enableTimeOfDay.setSelected(true);
        timeOfDayPanel.add(titlePanel);
        timeOfDayPanel.add(makeGriddedTimeOfDaySubPanel());
        timeOfDayPanel.add(utcConversionPanel());
        timeOfDayPanel.add(new JLabel(""));

        titlePanel = new JPanel();
        titlePanel.setLayout(new FlowLayout(FlowLayout.LEADING, CONTROL_SPACING, 0));
        enableDayOfWeek = new JCheckBox(resources.getString("enableDayOfWeek.name"));
        titlePanel.add(enableDayOfWeek);
        enableDayOfWeek.setSelected(true);
        timeOfDayPanel.add(titlePanel);
        timeOfDayPanel.add(griddedDayOfWeekPanel());

        // wrapper
        GridBagConstraints constraints = new GridBagConstraints();
        constraints.anchor = GridBagConstraints.NORTHWEST;
        constraints.weightx = 1.0;
        constraints.weighty = 1.0;
        constraints.insets = new Insets(CONTROL_SPACING, CONTROL_SPACING, CONTROL_SPACING, CONTROL_SPACING);
        constraints.fill = GridBagConstraints.BOTH;
        JPanel bordered = new JPanel();
        bordered.setLayout(new GridBagLayout());
        bordered.add(timeOfDayPanel, constraints);
        return bordered;
    }

    private JPanel makeBottomButtonsPanel() {
        // construct buttons
        helpButton = new JButton();
        helpButton.setText(resources.getString("helpButton.name"));
        okButton = new JButton();
        okButton.setText(resources.getString("okButton.name"));
        cancelButton = new JButton();
        cancelButton.setText(resources.getString("cancelButton.name"));

        // construct the bottom panel and wrap it with a border
        JPanel buttonsPanel = new JPanel();
        buttonsPanel.setLayout(new FlowLayout(FlowLayout.TRAILING, CONTROL_SPACING, 0));
        buttonsPanel.add(helpButton);
        buttonsPanel.add(okButton);
        buttonsPanel.add(cancelButton);

        //  make this panel align to the right
        JPanel rightPanel = new JPanel();
        rightPanel.setLayout(new BorderLayout());
        rightPanel.add(buttonsPanel, BorderLayout.EAST);

        // wrap this with border settings
        JPanel output = new JPanel();
        output.setLayout(new FlowLayout(FlowLayout.TRAILING, BORDER_PADDING-CONTROL_SPACING, BORDER_PADDING));
        output.add(rightPanel);

        return output;
    }

    private SpinnerListModel weekModel(int selectedDay) {
        SpinnerListModel model = new SpinnerListModel(week);
        model.setValue(week[selectedDay]);
        return model;
    }

    /**
     * for dev purposes only, to view dlg's layout
     */
    public static void main(String[] args) {
        TimeRange assertion = new TimeRange();
        //assertion.setStartDayOfWeek(Calendar.WEDNESDAY);
        assertion.setTimeRange(new TimeOfDayRange(new TimeOfDay(8,0,0), new TimeOfDay(17,0,0)));
        TimeRangePropertiesDialog me = new TimeRangePropertiesDialog(null, true, assertion);
        me.pack();
        me.show();
        System.exit(0);
    }

    private TimeRange assertion;

    private JButton helpButton;
    private JButton okButton;
    private JButton cancelButton;
    private JCheckBox enableTimeOfDay;
    private JSpinner startHr;
    private JSpinner endHr;
    private JSpinner startMin;
    private JSpinner endMin;
    private JSpinner startSec;
    private JSpinner endSec;
    private JLabel utcStartTime;
    private JLabel utcEndTime;

    private JCheckBox enableDayOfWeek;
    private JSpinner startDay;
    private JSpinner endDay;
    private String[] week;

    private final Collection itemsToToggleForTimeOfDay = new ArrayList();
    private final Collection itemsToToggleForDayOfWeek = new ArrayList();
    private int hroffset;
    private int minoffset;

    private ResourceBundle resources;

    private final static int BORDER_PADDING = 20;
    private final static int CONTROL_SPACING = 5;
}
