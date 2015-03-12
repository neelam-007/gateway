package com.l7tech.console.panels;

import com.l7tech.gui.util.Utilities;
import com.l7tech.gui.widgets.OkCancelDialog;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.logging.Logger;


public class ScheduledTaskEditCronIntervalDialog extends JDialog {
    private final Logger logger = Logger.getLogger(ScheduledTaskEditCronIntervalDialog.class.getName());

    private JPanel mainPanel;
    private JButton okButton;
    private JButton cancelButton;
    private JRadioButton everyRadioButton;
    private JRadioButton inAStepWidthRadioButton;
    private JRadioButton inARangeRadioButton;
    private JTextField stepWidthTextField;
    private JTextField fromTextField;
    private JTextField toTextField;
    private JRadioButton exactRadioButton;
    private JTextField exactTextField;
    private JRadioButton otherRadioButton;
    private JTextField expressionTextField;
    private JLabel stepWidthLabel;
    private JLabel fromLabel;
    private JLabel toLabel;
    private JLabel exactLabel;
    private JLabel expressionLabel;


    private boolean dataLoaded = false;
    private boolean confirmed = false;

    private String cronExpressionFragment;

    public String getCronExpressionFragment() {
        return cronExpressionFragment;
    }

    public ScheduledTaskEditCronIntervalDialog(Dialog parent, ScheduledTaskPropertiesDialog.ScheduledTaskBasicInterval interval, String cronExpressionFragment) {
        super(parent, "Edit Cron Field", true);

        this.cronExpressionFragment = cronExpressionFragment;

        initComponents(interval);

    }

    private void initComponents(ScheduledTaskPropertiesDialog.ScheduledTaskBasicInterval interval) {
        setContentPane(mainPanel);

        setLabels(interval);

        ButtonGroup buttonGroup = new ButtonGroup();
        buttonGroup.add(everyRadioButton);
        buttonGroup.add(inAStepWidthRadioButton);
        buttonGroup.add(inARangeRadioButton);
        buttonGroup.add(exactRadioButton);
        buttonGroup.add(otherRadioButton);

        everyRadioButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                enableDisableTextFields();
            }
        });
        inAStepWidthRadioButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                enableDisableTextFields();
            }
        });
        inARangeRadioButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                enableDisableTextFields();
            }
        });
        exactRadioButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                enableDisableTextFields();
            }
        });
        otherRadioButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                enableDisableTextFields();
            }
        });

        setRadioButtonSelection();

        enableDisableTextFields();

        Utilities.setEscAction(this, cancelButton);
        getRootPane().setDefaultButton(okButton);
        setMinimumSize(getContentPane().getMinimumSize());

        okButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                ok();
            }
        });

        cancelButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                cancel();
            }
        });

        this.addWindowListener(new WindowAdapter() {
            @Override
            public void windowOpened(WindowEvent e) {
                if (!dataLoaded) {
                   // resetData();
                }
            }
        });

        pack();
        Utilities.centerOnScreen(this);
    }

    private void setRadioButtonSelection(){
        if(cronExpressionFragment.equals("*")){
           everyRadioButton.setSelected(true);
        } else if(cronExpressionFragment.contains("/")){
           inAStepWidthRadioButton.setSelected(true);
           stepWidthTextField.setText(cronExpressionFragment.substring(2));
        } else if (cronExpressionFragment.contains("-")){
            inARangeRadioButton.setSelected(true);
            String[] fromTo = cronExpressionFragment.split("-");
            fromTextField.setText(fromTo[0]);
            toTextField.setText(fromTo[1]);
        } else if (cronExpressionFragment.contains(",")){
            otherRadioButton.setSelected(true);
            expressionTextField.setText(cronExpressionFragment);
        } else {
            exactRadioButton.setSelected(true);
            exactTextField.setText(cronExpressionFragment);
        }
    }

    private void setLabels(ScheduledTaskPropertiesDialog.ScheduledTaskBasicInterval interval){
        if(interval.equals(ScheduledTaskPropertiesDialog.ScheduledTaskBasicInterval.EVERY_SECOND)){
                stepWidthLabel.setText("Seconds:");
                everyRadioButton.setText("Every second");
                exactLabel.setText("Second: ");
                exactRadioButton.setText("At an exact second");
        } else if(interval.equals(ScheduledTaskPropertiesDialog.ScheduledTaskBasicInterval.EVERY_MINUTE)){
                stepWidthLabel.setText("Minutes:");
                everyRadioButton.setText("Every minute");
                exactLabel.setText("Minute: ");
                exactRadioButton.setText("At an exact minute");
        } else if (interval.equals(ScheduledTaskPropertiesDialog.ScheduledTaskBasicInterval.EVERY_HOUR)){
                stepWidthLabel.setText("Hours:");
                everyRadioButton.setText("Every Hour");
                exactLabel.setText("Hour: ");
                exactRadioButton.setText("At an exact hour");
        } else if (interval.equals(ScheduledTaskPropertiesDialog.ScheduledTaskBasicInterval.EVERY_DAY)){
                stepWidthLabel.setText("Days:");
                everyRadioButton.setText("Every day");
                exactLabel.setText("Day: ");
                exactRadioButton.setText("On a day");
        } else if (interval.equals(ScheduledTaskPropertiesDialog.ScheduledTaskBasicInterval.EVERY_MONTH)){
                stepWidthLabel.setText("Months:");
                everyRadioButton.setText("Every month");
                exactLabel.setText("Month: ");
                exactRadioButton.setText("In a month");
        } else if (interval.equals(ScheduledTaskPropertiesDialog.ScheduledTaskBasicInterval.EVERY_WEEK)){
                stepWidthLabel.setText("Weekdays:");
                everyRadioButton.setText("Every weekday");
                exactLabel.setText("Weekday: ");
                exactRadioButton.setText("On a weekday");
        }
    }


    private void enableDisableTextFields(){
        if (everyRadioButton.isSelected()){
            stepWidthTextField.setEnabled(false);
            fromTextField.setEnabled(false);
            toTextField.setEnabled(false);
            exactTextField.setEnabled(false);
            expressionTextField.setEnabled(false);
        }else if (inAStepWidthRadioButton.isSelected()){
            stepWidthTextField.setEnabled(true);
            fromTextField.setEnabled(false);
            toTextField.setEnabled(false);
            exactTextField.setEnabled(false);
            expressionTextField.setEnabled(false);
        } else if (inARangeRadioButton.isSelected()){
            stepWidthTextField.setEnabled(false);
            fromTextField.setEnabled(true);
            toTextField.setEnabled(true);
            exactTextField.setEnabled(false);
            expressionTextField.setEnabled(false);
        } else if (exactRadioButton.isSelected()){
            stepWidthTextField.setEnabled(false);
            fromTextField.setEnabled(false);
            toTextField.setEnabled(false);
            exactTextField.setEnabled(true);
            expressionTextField.setEnabled(false);
        } else if (otherRadioButton.isSelected()){
            stepWidthTextField.setEnabled(false);
            fromTextField.setEnabled(false);
            toTextField.setEnabled(false);
            exactTextField.setEnabled(false);
            expressionTextField.setEnabled(true);
        }
    }


    private void cancel() {
        dispose();
    }


    private void displayError(final String msg,
                              String title) {
        if (title == null) title = "Error";

        final int width = Utilities.computeStringWidth(this.getFontMetrics(this.getFont()), msg);
        final Object messageObject;
        if (width > 600) {
            messageObject = Utilities.getTextDisplayComponent(msg, 600, 100, -1, -1);
        } else {
            messageObject = msg;
        }

        JOptionPane.showMessageDialog(
                this,
                messageObject,
                title,
                JOptionPane.ERROR_MESSAGE);
    }


    private void ok() {

        try{
          setCronFragmentFromTextFields();

        }catch(Exception e){
            e.printStackTrace();
        }

        confirmed = true;
        dispose();
    }

    public boolean isConfirmed() {
        return confirmed;
    }

    private void setCronFragmentFromTextFields () {
        if(everyRadioButton.isSelected()){
            cronExpressionFragment = "*";
        } else if (inAStepWidthRadioButton.isSelected()){
            cronExpressionFragment = "*/" + stepWidthTextField.getText();
        } else if (inARangeRadioButton.isSelected()){
            cronExpressionFragment = fromTextField.getText() + "-" + toTextField.getText();
        } else if (exactRadioButton.isSelected()){
            cronExpressionFragment = exactTextField.getText();
        } else if (otherRadioButton.isSelected()){
            cronExpressionFragment = expressionTextField.getText();
        }
    }

    public void setData(ScheduledTaskEditCronIntervalDialog data) {
    }

    public void getData(ScheduledTaskEditCronIntervalDialog data) {
    }

    public boolean isModified(ScheduledTaskEditCronIntervalDialog data) {
        return false;
    }
}
