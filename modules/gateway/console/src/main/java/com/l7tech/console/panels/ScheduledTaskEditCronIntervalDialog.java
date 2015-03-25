package com.l7tech.console.panels;

import com.l7tech.gui.util.RunOnChangeListener;
import com.l7tech.gui.util.Utilities;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
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
    private JLabel toLabel;
    private JLabel exactLabel;


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

        RunOnChangeListener changeListener = new RunOnChangeListener(new Runnable(){
            @Override
            public void run() {
                enableDisableComponents();
            }
        });
        everyRadioButton.addActionListener(changeListener);
        inAStepWidthRadioButton.addActionListener(changeListener);
        inARangeRadioButton.addActionListener(changeListener);
        exactRadioButton.addActionListener(changeListener);
        otherRadioButton.addActionListener(changeListener);

        stepWidthTextField.getDocument().addDocumentListener(changeListener);
        fromTextField.getDocument().addDocumentListener(changeListener);
        toTextField.getDocument().addDocumentListener(changeListener);
        exactTextField.getDocument().addDocumentListener(changeListener);
        expressionTextField.getDocument().addDocumentListener(changeListener);

        setRadioButtonSelection();
        enableDisableComponents();

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
                stepWidthLabel.setText("seconds");
                everyRadioButton.setText("Every second");
                exactLabel.setText("second");
                exactRadioButton.setText("At an exact second");
        } else if(interval.equals(ScheduledTaskPropertiesDialog.ScheduledTaskBasicInterval.EVERY_MINUTE)){
                stepWidthLabel.setText("minutes");
                everyRadioButton.setText("Every minute");
                exactLabel.setText("minute");
                exactRadioButton.setText("At an exact minute");
        } else if (interval.equals(ScheduledTaskPropertiesDialog.ScheduledTaskBasicInterval.EVERY_HOUR)){
                stepWidthLabel.setText("hours");
                everyRadioButton.setText("Every Hour");
                exactLabel.setText("hour");
                exactRadioButton.setText("At an exact hour");
        } else if (interval.equals(ScheduledTaskPropertiesDialog.ScheduledTaskBasicInterval.EVERY_DAY)){
                stepWidthLabel.setText("days");
                everyRadioButton.setText("Every day");
                exactLabel.setText("day");
                exactRadioButton.setText("On a day");
        } else if (interval.equals(ScheduledTaskPropertiesDialog.ScheduledTaskBasicInterval.EVERY_MONTH)){
                stepWidthLabel.setText("months");
                everyRadioButton.setText("Every month");
                exactLabel.setText("month");
                exactRadioButton.setText("In a month");
        } else if (interval.equals(ScheduledTaskPropertiesDialog.ScheduledTaskBasicInterval.EVERY_WEEK)){
                stepWidthLabel.setText("day of the week");
                everyRadioButton.setText("Every day of the week");
                exactLabel.setText("day of the week");
                exactRadioButton.setText("On a day of the week");
        }
    }


    private void enableDisableComponents(){
        if (everyRadioButton.isSelected()){
            stepWidthTextField.setEnabled(false);
            fromTextField.setEnabled(false);
            toTextField.setEnabled(false);
            exactTextField.setEnabled(false);
            expressionTextField.setEnabled(false);
            okButton.setEnabled(true);
        }else if (inAStepWidthRadioButton.isSelected()){
            stepWidthTextField.setEnabled(true);
            fromTextField.setEnabled(false);
            toTextField.setEnabled(false);
            exactTextField.setEnabled(false);
            expressionTextField.setEnabled(false);
            okButton.setEnabled(stepWidthTextField.getText()!=null && !stepWidthTextField.getText().isEmpty());
        } else if (inARangeRadioButton.isSelected()){
            stepWidthTextField.setEnabled(false);
            fromTextField.setEnabled(true);
            toTextField.setEnabled(true);
            exactTextField.setEnabled(false);
            expressionTextField.setEnabled(false);
            okButton.setEnabled(fromTextField.getText()!=null && !fromTextField.getText().isEmpty() && toTextField.getText()!=null && !toTextField.getText().isEmpty());
        } else if (exactRadioButton.isSelected()){
            stepWidthTextField.setEnabled(false);
            fromTextField.setEnabled(false);
            toTextField.setEnabled(false);
            exactTextField.setEnabled(true);
            expressionTextField.setEnabled(false);
            okButton.setEnabled(exactTextField.getText()!=null && !exactTextField.getText().isEmpty());
        } else if (otherRadioButton.isSelected()){
            stepWidthTextField.setEnabled(false);
            fromTextField.setEnabled(false);
            toTextField.setEnabled(false);
            exactTextField.setEnabled(false);
            expressionTextField.setEnabled(true);
            okButton.setEnabled(expressionTextField.getText()!=null && !expressionTextField.getText().isEmpty());
        }
    }


    private void cancel() {
        dispose();
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
}
