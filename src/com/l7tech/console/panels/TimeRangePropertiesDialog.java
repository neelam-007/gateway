package com.l7tech.console.panels;

import com.l7tech.console.action.Actions;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import java.util.Collection;
import java.util.ArrayList;
import java.util.Iterator;

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

    /**
     * create controls and layout
     */
    private void initialize() {
        itemsToToggleForTimeOfDay.clear();

        setTitle("Time Range Assertion Properties");
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

        JLabel toto = new JLabel("between");
        itemsToToggleForTimeOfDay.add(toto);
        timeOfDayPanel.add(toto, new GridBagConstraints(0, 0, 1, 1, 0, 0, dir, nfill, insets, 0, 0));
        startHr = new JSpinner(new SpinnerNumberModel(8, 0, 23, 1));
        itemsToToggleForTimeOfDay.add(startHr);
        itemsToToggleForTimeOfDay.add(startHr);
        timeOfDayPanel.add(startHr, new GridBagConstraints(1, 0, 1, 1, weightx, 0, dir, fill, insets, 0, 0));
        toto = new JLabel("hr");
        itemsToToggleForTimeOfDay.add(toto);
        timeOfDayPanel.add(toto, new GridBagConstraints(2, 0, 1, 1, 0, 0, dir, nfill, insets, 0, 0));
        startMin = new JSpinner(new SpinnerNumberModel(0, 0, 59, 1));
        itemsToToggleForTimeOfDay.add(startMin);
        timeOfDayPanel.add(startMin, new GridBagConstraints(3, 0, 1, 1, weightx, 0, dir, fill, insets, 0, 0));
        toto = new JLabel("min");
        itemsToToggleForTimeOfDay.add(toto);
        timeOfDayPanel.add(toto, new GridBagConstraints(4, 0, 1, 1, 0, 0, dir, nfill, insets, 0, 0));
        startSec = new JSpinner(new SpinnerNumberModel(0, 0, 59, 1));
        itemsToToggleForTimeOfDay.add(startSec);
        timeOfDayPanel.add(startSec, new GridBagConstraints(5, 0, 1, 1, weightx, 0, dir, fill, insets, 0, 0));
        toto = new JLabel("sec");
        itemsToToggleForTimeOfDay.add(toto);
        timeOfDayPanel.add(toto, new GridBagConstraints(6, 0, 1, 1, 0, 0, dir, nfill, insets, 0, 0));

        toto = new JLabel("and");
        itemsToToggleForTimeOfDay.add(toto);
        timeOfDayPanel.add(toto, new GridBagConstraints(0, 1, 1, 1, 0, 0, dir, nfill, insets, 0, 0));
        endHr = new JSpinner(new SpinnerNumberModel(17, 0, 23, 1));
        itemsToToggleForTimeOfDay.add(endHr);
        timeOfDayPanel.add(endHr, new GridBagConstraints(1, 1, 1, 1, weightx, 0, dir, fill, insets, 0, 0));
        toto = new JLabel("hr");
        itemsToToggleForTimeOfDay.add(toto);
        timeOfDayPanel.add(toto, new GridBagConstraints(2, 1, 1, 1, 0, 0, dir, nfill, insets, 0, 0));
        endMin = new JSpinner(new SpinnerNumberModel(0, 0, 59, 1));
        itemsToToggleForTimeOfDay.add(endMin);
        timeOfDayPanel.add(endMin, new GridBagConstraints(3, 1, 1, 1, weightx, 0, dir, fill, insets, 0, 0));
        toto = new JLabel("min");
        itemsToToggleForTimeOfDay.add(toto);
        timeOfDayPanel.add(toto, new GridBagConstraints(4, 1, 1, 1, 0, 0, dir, nfill, insets, 0, 0));
        endSec = new JSpinner(new SpinnerNumberModel(0, 0, 59, 1));
        itemsToToggleForTimeOfDay.add(endSec);
        timeOfDayPanel.add(endSec, new GridBagConstraints(5, 1, 1, 1, weightx, 0, dir, fill, insets, 0, 0));
        toto = new JLabel("sec");
        itemsToToggleForTimeOfDay.add(toto);
        timeOfDayPanel.add(toto, new GridBagConstraints(6, 1, 1, 1, 0, 0, dir, nfill, insets, 0, 0));

        return timeOfDayPanel;
    }

    public JPanel griddedDayOfWeekPanel() {
        JPanel dayOfWeekPanel = new JPanel();
        dayOfWeekPanel.setLayout(new GridBagLayout());

        int nfill = GridBagConstraints.NONE;
        int fill = GridBagConstraints.HORIZONTAL;
        int dir = GridBagConstraints.WEST;
        double weightx = 1.0;
        Insets insets = new Insets(0, 0, 0, CONTROL_SPACING);

        JLabel toto = new JLabel("between");
        itemsToToggleForDayOfWeek.add(toto);
        dayOfWeekPanel.add(toto, new GridBagConstraints(0, 0, 1, 1, 0, 0, dir, nfill, insets, 0, 0));
        startDay = new JSpinner(weekModel(1));
        itemsToToggleForDayOfWeek.add(startDay);
        dayOfWeekPanel.add(startDay, new GridBagConstraints(1, 0, 1, 1, weightx, 0, dir, fill, insets, 0, 0));

        toto = new JLabel("and");
        itemsToToggleForDayOfWeek.add(toto);
        dayOfWeekPanel.add(toto, new GridBagConstraints(0, 1, 1, 1, 0, 0, dir, nfill, insets, 0, 0));
        endDay = new JSpinner(weekModel(5));
        itemsToToggleForDayOfWeek.add(endDay);
        dayOfWeekPanel.add(endDay, new GridBagConstraints(1, 1, 1, 1, weightx, 0, dir, fill, insets, 0, 0));

        return dayOfWeekPanel;
    }

    private JComponent mainPanel() {

        JPanel timeOfDayPanel = new JPanel();
        timeOfDayPanel.setLayout(new GridLayout(5, 1, CONTROL_SPACING, 0));
        JPanel titlePanel = new JPanel();
        titlePanel.setLayout(new FlowLayout(FlowLayout.LEADING, CONTROL_SPACING, 0));
        enableTimeOfDay = new JCheckBox("Restrict time of day");
        titlePanel.add(enableTimeOfDay);
        enableTimeOfDay.setSelected(true);
        timeOfDayPanel.add(titlePanel);
        timeOfDayPanel.add(makeGriddedTimeOfDaySubPanel());
        timeOfDayPanel.add(new JLabel(""));

        titlePanel = new JPanel();
        titlePanel.setLayout(new FlowLayout(FlowLayout.LEADING, CONTROL_SPACING, 0));
        enableDayOfWeek = new JCheckBox("Restrict day of week");
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

    private SpinnerListModel weekModel(int selectedDay) {
        SpinnerListModel model = new SpinnerListModel(week);
        model.setValue(week[selectedDay]);
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
    private final Collection itemsToToggleForTimeOfDay = new ArrayList();
    private final Collection itemsToToggleForDayOfWeek = new ArrayList();

    private final static int BORDER_PADDING = 20;
    private final static int CONTROL_SPACING = 5;
}
