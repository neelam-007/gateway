package com.l7tech.console.panels;

import com.l7tech.gui.util.RunOnChangeListener;
import com.l7tech.gui.util.Utilities;
import com.l7tech.util.ValidationUtils;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.text.MessageFormat;
import java.util.ResourceBundle;
import java.util.logging.Logger;


public class ScheduledTaskEditCronIntervalDialog extends JDialog {
    private final Logger logger = Logger.getLogger(ScheduledTaskEditCronIntervalDialog.class.getName());
    private static final ResourceBundle resource = ResourceBundle.getBundle(ScheduledTaskEditCronIntervalDialog.class.getName());

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
        super(parent, resource.getString("dialog.title"), true);

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
        } else if(cronExpressionFragment.startsWith("*/")){
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
        } else if (ValidationUtils.isValidInteger(cronExpressionFragment, false, 0, Integer.MAX_VALUE)) {
            exactRadioButton.setSelected(true);
            exactTextField.setText(cronExpressionFragment);
        }else {
            otherRadioButton.setSelected(true);
            expressionTextField.setText(cronExpressionFragment);
        }
    }

    private void setLabels(ScheduledTaskPropertiesDialog.ScheduledTaskBasicInterval interval){
        if(interval.equals(ScheduledTaskPropertiesDialog.ScheduledTaskBasicInterval.EVERY_SECOND)){
                stepWidthLabel.setText(resource.getString("label.second.step"));
                everyRadioButton.setText(resource.getString("label.second.every"));
                exactLabel.setText(resource.getString("label.second.exact"));
                exactRadioButton.setText(resource.getString("label.second.radio.exact"));
                everyRadioButton.setToolTipText(MessageFormat.format(resource.getString("tooltip.radio.every"),resource.getString("label.second.exact")));
                inAStepWidthRadioButton.setToolTipText(MessageFormat.format(resource.getString("tooltip.radio.step"),resource.getString("label.second.step")));
                exactRadioButton.setToolTipText(MessageFormat.format(resource.getString("tooltip.radio.at"),resource.getString("label.second.exact")));
        } else if(interval.equals(ScheduledTaskPropertiesDialog.ScheduledTaskBasicInterval.EVERY_MINUTE)){
            stepWidthLabel.setText(resource.getString("label.minute.step"));
            everyRadioButton.setText(resource.getString("label.minute.every"));
            exactLabel.setText(resource.getString("label.minute.exact"));
            exactRadioButton.setText(resource.getString("label.minute.radio.exact"));
            everyRadioButton.setToolTipText(MessageFormat.format(resource.getString("tooltip.radio.every"),resource.getString("label.minute.exact")));
            inAStepWidthRadioButton.setToolTipText(MessageFormat.format(resource.getString("tooltip.radio.step"),resource.getString("label.minute.step")));
            exactRadioButton.setToolTipText(MessageFormat.format(resource.getString("tooltip.radio.at"),resource.getString("label.minute.exact")));
        } else if (interval.equals(ScheduledTaskPropertiesDialog.ScheduledTaskBasicInterval.EVERY_HOUR)){
            stepWidthLabel.setText(resource.getString("label.hour.step"));
            everyRadioButton.setText(resource.getString("label.hour.every"));
            exactLabel.setText(resource.getString("label.hour.exact"));
            exactRadioButton.setText(resource.getString("label.hour.radio.exact"));
            everyRadioButton.setToolTipText(MessageFormat.format(resource.getString("tooltip.radio.every"),resource.getString("label.hour.exact")));
            inAStepWidthRadioButton.setToolTipText(MessageFormat.format(resource.getString("tooltip.radio.step"),resource.getString("label.hour.step")));
            exactRadioButton.setToolTipText(MessageFormat.format(resource.getString("tooltip.radio.at"),resource.getString("label.hour.exact")));
        } else if (interval.equals(ScheduledTaskPropertiesDialog.ScheduledTaskBasicInterval.EVERY_DAY)){
            stepWidthLabel.setText(resource.getString("label.day.step"));
            everyRadioButton.setText(resource.getString("label.day.every"));
            exactLabel.setText(resource.getString("label.day.exact"));
            exactRadioButton.setText(resource.getString("label.day.radio.exact"));
            everyRadioButton.setToolTipText(MessageFormat.format(resource.getString("tooltip.radio.every"),resource.getString("label.day.exact")));
            inAStepWidthRadioButton.setToolTipText(MessageFormat.format(resource.getString("tooltip.radio.step"),resource.getString("label.day.step")));
            exactRadioButton.setToolTipText(MessageFormat.format(resource.getString("tooltip.radio.at"),resource.getString("label.day.exact")));
        } else if (interval.equals(ScheduledTaskPropertiesDialog.ScheduledTaskBasicInterval.EVERY_MONTH)){
            stepWidthLabel.setText(resource.getString("label.month.step"));
            everyRadioButton.setText(resource.getString("label.month.every"));
            exactLabel.setText(resource.getString("label.month.exact"));
            exactRadioButton.setText(resource.getString("label.month.radio.exact"));
            everyRadioButton.setToolTipText(MessageFormat.format(resource.getString("tooltip.radio.every"),resource.getString("label.month.exact")));
            inAStepWidthRadioButton.setToolTipText(MessageFormat.format(resource.getString("tooltip.radio.step"),resource.getString("label.month.step")));
            exactRadioButton.setToolTipText(MessageFormat.format(resource.getString("tooltip.radio.at"),resource.getString("label.month.exact")));
        } else if (interval.equals(ScheduledTaskPropertiesDialog.ScheduledTaskBasicInterval.EVERY_WEEK)){
            stepWidthLabel.setText(resource.getString("label.weekday.step"));
            everyRadioButton.setText(resource.getString("label.weekday.every"));
            exactLabel.setText(resource.getString("label.weekday.exact"));
            exactRadioButton.setText(resource.getString("label.weekday.radio.exact"));
            everyRadioButton.setToolTipText(MessageFormat.format(resource.getString("tooltip.radio.every"),resource.getString("label.weekday.exact")));
            inAStepWidthRadioButton.setToolTipText(MessageFormat.format(resource.getString("tooltip.radio.step"),resource.getString("label.weekday.step")));
            exactRadioButton.setToolTipText(MessageFormat.format(resource.getString("tooltip.radio.at"),resource.getString("label.weekday.exact")));
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
