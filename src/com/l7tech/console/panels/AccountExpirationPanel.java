package com.l7tech.console.panels;

import com.l7tech.console.action.Actions;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import java.awt.*;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import java.awt.event.KeyListener;
import java.awt.event.KeyEvent;
import java.text.DateFormatSymbols;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;

/**
 * A dialog to view/edit the expiration of a user account. This is invoked by the
 * user properties dialog (GenerixUserPanel.java).
 * <p/>
 * <br/><br/>
 * LAYER 7 TECHNOLOGIES, INC<br/>
 * <p/>
 * User: flascell<br/>
 * Date: Dec 21, 2004<br/>
 */
public class AccountExpirationPanel extends JDialog {
    private JPanel mainPanel;
    private JSpinner dayspinner;
    private JSpinner monthspinner;
    private JSpinner yearspinner;
    private JRadioButton expireradio;
    private JRadioButton noexpireradio;
    private JButton helpbutton;
    private JButton cancelbutton;
    private JButton okbutton;
    private int startYear;
    public java.util.List months;
    private boolean dayNeverSet;
    private boolean canceled;
    private long expirationValue = -2;
    private JLabel daylabel;
    private JLabel monthlabel;
    private JLabel yearlabel;

    public AccountExpirationPanel(JDialog parent, long initialValue) {
        super(parent, true);
        initialize(initialValue);
    }

    public AccountExpirationPanel(Frame parent, long initialValue) {
        super(parent, true);
        initialize(initialValue);
    }

    /**
     * @return true if the dialog was cancelled, false otherwise.
     */
    public boolean wasCancelled() {
        return canceled;
    }

    /**
     * The value captured from the user of the dialog. This value means nothing if wasCencelled() returns true.
     * @return the date for the account expiration or -1 if the account is to not expire.
     */
    public long getExpirationValue() {
        return expirationValue;
    }

    private void initialize(long initialValue) {
        setContentPane(mainPanel);
        setTitle("Account expiration");
        canceled = false;

        okbutton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                ok();
            }
        });
        cancelbutton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                cancel();
            }
        });
        helpbutton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                Actions.invokeHelp(AccountExpirationPanel.this);
            }
        });

        ButtonGroup bg = new ButtonGroup();
        bg.add(noexpireradio);
        bg.add(expireradio);
        ActionListener radiolistener = new ActionListener(){
            public void actionPerformed(ActionEvent e) {
                enableControls();
            }
        };
        noexpireradio.addActionListener(radiolistener);
        expireradio.addActionListener(radiolistener);
        if (initialValue == -1) {
            noexpireradio.setSelected(true);
        } else {
            expireradio.setSelected(true);
        }
        enableControls();

        Calendar initialCalValue = Calendar.getInstance();
        initialCalValue.setTimeInMillis(initialValue);

        // populate year spinner
        startYear = Calendar.getInstance().get(Calendar.YEAR);
        ArrayList yearlist = new ArrayList();
        for (int i = 0; i < 11; i++) {
            yearlist.add(Integer.toString(startYear+i));
        }
        SpinnerListModel ysm = new SpinnerListModel(yearlist);
        if (initialValue == -1) {
            ysm.setValue(yearlist.get(1));
        } else {
            int initYear = initialCalValue.get(Calendar.YEAR);
            if (initYear < startYear) initYear = startYear;
            if (initYear > startYear+10) initYear = startYear+10;
            ysm.setValue(yearlist.get(initYear-startYear));
        }
        yearspinner.setModel(ysm);

        // populate month spinner
        String[] dfsmonths = (new DateFormatSymbols()).getMonths();
        months = Arrays.asList(dfsmonths).subList(0, 12);
        SpinnerListModel msm = new SpinnerListModel(months);
        if (initialValue == -1) {
            msm.setValue(months.get(Calendar.getInstance().get(Calendar.MONTH)));
        } else {
            int initMonth = initialCalValue.get(Calendar.MONTH);
            msm.setValue(months.get(initMonth));
        }
        monthspinner.setModel(msm);

        if (initialValue == -1) {
            dayNeverSet = true;
        } else {
            dayspinner.setValue(new Integer(initialCalValue.get(Calendar.DAY_OF_MONTH)));
        }
        populateDaySpinner();

        ChangeListener updateDaysOfMonthActionListener = new ChangeListener() {
            public void stateChanged(ChangeEvent e) {
                populateDaySpinner();
            }
        };
        monthspinner.addChangeListener(updateDaysOfMonthActionListener);
        yearspinner.addChangeListener(updateDaysOfMonthActionListener);

        // set default keyboard listeners
        KeyListener defBehaviorKeyListener = new KeyListener() {
            public void keyPressed(KeyEvent e) {}
            public void keyReleased(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ESCAPE) {
                    cancel();
                } else if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                    ok();
                }
            }
            public void keyTyped(KeyEvent e) {}
        };
        dayspinner.addKeyListener(defBehaviorKeyListener);
        monthspinner.addKeyListener(defBehaviorKeyListener);
        yearspinner.addKeyListener(defBehaviorKeyListener);
        expireradio.addKeyListener(defBehaviorKeyListener);
        noexpireradio.addKeyListener(defBehaviorKeyListener);
        helpbutton.addKeyListener(defBehaviorKeyListener);
        cancelbutton.addKeyListener(defBehaviorKeyListener);
        okbutton.addKeyListener(defBehaviorKeyListener);
    }

    private void enableControls() {
        if (noexpireradio.isSelected()) {
            dayspinner.setEnabled(false);
            monthspinner.setEnabled(false);
            yearspinner.setEnabled(false);
            daylabel.setEnabled(false);
            monthlabel.setEnabled(false);
            yearlabel.setEnabled(false);
        } else if (expireradio.isSelected()) {
            dayspinner.setEnabled(true);
            monthspinner.setEnabled(true);
            yearspinner.setEnabled(true);
            daylabel.setEnabled(true);
            monthlabel.setEnabled(true);
            yearlabel.setEnabled(true);
        } else {
            throw new RuntimeException("one of the radios should be selected"); // should not happen
        }
    }

    private void populateDaySpinner() {
        // figure out last day for that particular month
        Calendar cal = Calendar.getInstance();
        int currentlychosenyear = Integer.parseInt((String)yearspinner.getValue());
        int currentlychosenmonth = months.indexOf(monthspinner.getValue());
        // go to first day of next month
        if (currentlychosenmonth >= 11) {
            currentlychosenmonth = 0;
            ++currentlychosenyear;
        } else {
            ++currentlychosenmonth;
        }
        cal.set(Calendar.YEAR, currentlychosenyear);
        cal.set(Calendar.MONTH, currentlychosenmonth);
        cal.set(Calendar.DAY_OF_MONTH, 1);
        cal.add(Calendar.DAY_OF_MONTH, -1);
        int lastdayofmonth = cal.get(Calendar.DAY_OF_MONTH);
        // determine which date should be set
        int daywhichshouldbeselected;
        if (dayNeverSet) {
            daywhichshouldbeselected = Calendar.getInstance().get(Calendar.DAY_OF_MONTH);
            dayNeverSet = false;
        } else {
            daywhichshouldbeselected = ((Integer)dayspinner.getValue()).intValue();
        }
        if (daywhichshouldbeselected > lastdayofmonth) {
            daywhichshouldbeselected = lastdayofmonth;
        }
        // reset spinner model
        SpinnerNumberModel dsm = new SpinnerNumberModel(daywhichshouldbeselected, 1, lastdayofmonth, 1);
        dayspinner.setModel(dsm);
    }

    private void cancel() {
        // just remember we canceled and exit
        canceled = true;
        AccountExpirationPanel.this.dispose();
    }

    private void ok() {
        // save captured value
        if (noexpireradio.isSelected()) {
            expirationValue = -1;
        } else {
            int currentlychosenyear = Integer.parseInt((String)yearspinner.getValue());
            int currentlychosenmonth = months.indexOf(monthspinner.getValue());
            int currentlychosenday = ((Integer)dayspinner.getValue()).intValue();
            Calendar cal = Calendar.getInstance();
            cal.set(Calendar.YEAR, currentlychosenyear);
            cal.set(Calendar.MONTH, currentlychosenmonth);
            cal.set(Calendar.DAY_OF_MONTH, currentlychosenday);
            cal.set(Calendar.HOUR_OF_DAY, 0);
            cal.set(Calendar.MINUTE, 0);
            cal.set(Calendar.SECOND, 1);
            expirationValue = cal.getTimeInMillis();
        }
        // exit
        AccountExpirationPanel.this.dispose();
    }

    /*// just for testing
    public static final void main(String[] args) throws Exception {
        long value = -1;
        for (int i = 0; i < 5; i++) {
            AccountExpirationPanel exirationChooser = new AccountExpirationPanel((Frame)null, value);
            exirationChooser.pack();
            Utilities.centerOnScreen(exirationChooser);
            exirationChooser.show();
            if (!exirationChooser.wasCancelled()) {
                value = exirationChooser.getExpirationValue();
            }
        }

    }*/
}
