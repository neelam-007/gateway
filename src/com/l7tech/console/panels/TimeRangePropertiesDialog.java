package com.l7tech.console.panels;

import com.l7tech.console.action.Actions;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;

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

    public TimeRangePropertiesDialog(Frame owner, boolean modal) {
        super(owner, modal);
        initialize();
        // todo constructor to pass actual assertion
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
        startDay.setEnabled(enable);
        endDay.setEnabled(enable);
    }

    /**
     * enable disable time of day controls
     */
    private void toggleTimeOfDay() {
        boolean enable = false;
        if (enableTimeOfDay.isSelected()) {
            enable = true;
        }
        startHr.setEnabled(enable);
        endHr.setEnabled(enable);
        startMin.setEnabled(enable);
        endMin.setEnabled(enable);
        startSec.setEnabled(enable);
        endSec.setEnabled(enable);
    }

    /**
     * create controls and layout
     */
    private void initialize() {
        setTitle("Time Range Assertion Properties");
        Container contents = getContentPane();
        contents.setLayout(new BorderLayout(0,0));
        contents.add(makeTabPanel(), BorderLayout.CENTER);
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
    }

    private JComponent makeTabPanel() {
        JTabbedPane tabbedPane = new JTabbedPane(JTabbedPane.TOP, JTabbedPane.SCROLL_TAB_LAYOUT);
        tabbedPane.addTab("Time of day restriction", makeTimeOfDayPanel());
        tabbedPane.addTab("Day of week restriction", makeDayOfWeekPanel());
        return wrapAroundBorders(tabbedPane, CONTROL_SPACING, CONTROL_SPACING, 0, BORDER_PADDING);
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

    private JComponent makeTimeOfDayPanel() {
        JPanel timeOfDayPanel = new JPanel();
        timeOfDayPanel.setLayout(new GridLayout(3, 1, CONTROL_SPACING, CONTROL_SPACING));
        JPanel titlePanel = new JPanel();
        titlePanel.setLayout(new FlowLayout(FlowLayout.LEADING, CONTROL_SPACING, 0));
        enableTimeOfDay = new JCheckBox("Restrict time of day");
        titlePanel.add(enableTimeOfDay);
        enableTimeOfDay.setSelected(true);
        timeOfDayPanel.add(titlePanel);
        timeOfDayPanel.add(startTimePanel());
        timeOfDayPanel.add(endTimePanel());
        // wrapper
        GridBagConstraints constraints = new GridBagConstraints();
        constraints.anchor = GridBagConstraints.NORTHWEST;
        constraints.weightx = 1.0;
        constraints.weighty = 1.0;
        constraints.insets = new Insets(CONTROL_SPACING, CONTROL_SPACING, CONTROL_SPACING, CONTROL_SPACING);
        JPanel bordered = new JPanel();
        bordered.setLayout(new GridBagLayout());
        bordered.add(timeOfDayPanel, constraints);
        return bordered;
    }

    private JPanel startTimePanel() {
        JPanel startTimePanel = new JPanel();
        startTimePanel.setLayout(new FlowLayout(FlowLayout.LEADING, CONTROL_SPACING, 0));
        startTimePanel.add(new JLabel("between"));
        startHr = new JSpinner(new SpinnerNumberModel(8, 0, 23, 1));
        startTimePanel.add(startHr);
        startTimePanel.add(new JLabel("hr"));
        startMin = new JSpinner(new SpinnerNumberModel(0, 0, 59, 1));
        startTimePanel.add(startMin);
        startTimePanel.add(new JLabel("min"));
        startSec = new JSpinner(new SpinnerNumberModel(0, 0, 59, 1));
        startTimePanel.add(startSec);
        startTimePanel.add(new JLabel("sec"));
        return startTimePanel;
    }

    private JPanel endTimePanel() {
        JPanel endTimePanel = new JPanel();
        endTimePanel.setLayout(new FlowLayout(FlowLayout.LEADING, CONTROL_SPACING, 0));
        endTimePanel.add(new JLabel("and"));
        endHr = new JSpinner(new SpinnerNumberModel(17, 0, 23, 1));
        endTimePanel.add(endHr);
        endTimePanel.add(new JLabel("hr"));
        endMin = new JSpinner(new SpinnerNumberModel(0, 0, 59, 1));
        endTimePanel.add(endMin);
        endTimePanel.add(new JLabel("min"));
        endSec = new JSpinner(new SpinnerNumberModel(0, 0, 59, 1));
        endTimePanel.add(endSec);
        endTimePanel.add(new JLabel("sec"));
        return endTimePanel;
    }

    private JPanel startDayPanel() {
        JPanel startDayPanel = new JPanel();
        startDayPanel.setLayout(new FlowLayout(FlowLayout.LEADING, CONTROL_SPACING, 0));
        startDayPanel.add(new JLabel("between"));
        startDay = new JSpinner(weekModel());
        startDayPanel.add(startDay);
        return startDayPanel;
    }

    private JPanel endDayPanel() {
        JPanel endDayPanel = new JPanel();
        endDayPanel.setLayout(new FlowLayout(FlowLayout.LEADING, CONTROL_SPACING, 0));
        endDayPanel.add(new JLabel("and"));
        endDay = new JSpinner(weekModel());
        endDayPanel.add(endDay);
        return endDayPanel;
    }

    private JComponent makeDayOfWeekPanel() {
        JPanel dayOfWeekPanel = new JPanel();
        dayOfWeekPanel.setLayout(new GridLayout(3, 1, CONTROL_SPACING, CONTROL_SPACING));
        JPanel titlePanel = new JPanel();
        titlePanel.setLayout(new FlowLayout(FlowLayout.LEADING, CONTROL_SPACING, 0));
        enableDayOfWeek = new JCheckBox("Restrict day of week");
        titlePanel.add(enableDayOfWeek);
        enableDayOfWeek.setSelected(true);
        dayOfWeekPanel.add(titlePanel);
        dayOfWeekPanel.add(startDayPanel());
        dayOfWeekPanel.add(endDayPanel());
        // wrapper
        GridBagConstraints constraints = new GridBagConstraints();
        constraints.anchor = GridBagConstraints.NORTHWEST;
        constraints.weightx = 1.0;
        constraints.weighty = 1.0;
        constraints.insets = new Insets(CONTROL_SPACING, CONTROL_SPACING, CONTROL_SPACING, CONTROL_SPACING);
        JPanel bordered = new JPanel();
        bordered.setLayout(new GridBagLayout());
        bordered.add(dayOfWeekPanel, constraints);
        return bordered;
    }

    private JPanel makeBottomButtonsPanel() {
        // construct buttons
        helpButton = new JButton();
        helpButton.setText("Help");
        okButton = new JButton();
        okButton.setText("Ok");
        cancelButton = new JButton();
        cancelButton.setText("Cancel");

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

    private SpinnerListModel weekModel() {
        SpinnerListModel model = new SpinnerListModel(week);
        model.setValue(week[3]);
        return model;
    }

    /**
     * for dev purposes only, to view dlg's layout
     */
    public static void main(String[] args) {
        TimeRangePropertiesDialog me = new TimeRangePropertiesDialog(null, true);
        me.pack();
        me.show();
        System.exit(0);
    }

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

    private JCheckBox enableDayOfWeek;
    private JSpinner startDay;
    private JSpinner endDay;
    private static final String[] week = new String[] {"Sunday", "Monday",
                                                       "Tuesday", "Wednesday",
                                                       "Thursday", "Friday",
                                                       "Saturday"};

    private final static int BORDER_PADDING = 20;
    private final static int CONTROL_SPACING = 5;
}
