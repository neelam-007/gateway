/**
 * LAYER 7 TECHNOLOGIES, INC<br/>
 *
 * User: flascell<br/>
 * Date: Apr 1, 2005<br/>
 */
package com.l7tech.console.panels;

import com.l7tech.console.action.Actions;
import com.l7tech.gui.MaxLengthDocument;
import com.l7tech.gui.util.RunOnChangeListener;
import com.l7tech.policy.assertion.sla.CounterPresetInfo;
import com.l7tech.policy.assertion.sla.ThroughputQuota;
import org.apache.commons.lang.StringUtils;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.util.Hashtable;
import java.util.Vector;

/**
 * A dialog for editing a ThroughputQuota dialog.
 *
 * @author flascelles@layer7-tech.com
 */
public class ThroughputQuotaForm extends LegacyAssertionPropertyDialog {
    private JPanel mainPanel;
    private JButton okButton;
    private JButton cancelButton;
    private JButton helpButton;
    private JTextField quotaValueField;
    private JComboBox quotaUnitCombo;
    private JComboBox quotaOptionsComboBox;
    private JTextField counterNameTextField;

    private static final String[] TIME_UNITS = {"second", "minute", "hour", "day", "month"};

    private ThroughputQuota subject;
    private boolean oked = false;
    private JRadioButton alwaysIncrementRadio;
    private JRadioButton decrementRadio;
    private JRadioButton incrementOnSuccessRadio;
    private JPanel varPrefixFieldPanel;
    private JCheckBox logOnlyCheckBox;
    private JRadioButton resetRadio;
    private JTextField byField;
    private JSlider performanceSlider;
    private JLabel performanceDescriptionLabel;
    private JCheckBox byCheckBox;
    private TargetVariablePanel varPrefixField;

    private String uuid[] = {CounterPresetInfo.makeUuid()};
    private String expr = "";

    public ThroughputQuotaForm(Frame owner, ThroughputQuota subject, boolean readOnly) {
        super(owner, subject, true);
        this.subject = subject;
        initComponents(readOnly);
    }

    protected void initComponents(final boolean readOnly) {
        getContentPane().add(mainPanel);

        DefaultComboBoxModel model = (DefaultComboBoxModel)quotaUnitCombo.getModel();
        for (String TIME_UNIT : TIME_UNITS) {
            model.addElement(TIME_UNIT);
        }

        varPrefixField = new TargetVariablePanel();
        varPrefixFieldPanel.setLayout(new BorderLayout());
        varPrefixFieldPanel.add(varPrefixField, BorderLayout.CENTER);
        varPrefixField.addChangeListener(new ChangeListener(){
            @Override
            public void stateChanged(ChangeEvent e) {
                okButton.setEnabled(!readOnly && varPrefixField.isEntryValid());
            }
        });

        varPrefixField.setVariable(subject.getVariablePrefix());
        varPrefixField.setSuffixes(subject.getVariableSuffixes());
        varPrefixField.setAcceptEmpty(true);

        quotaOptionsComboBox.setModel(new DefaultComboBoxModel(new Vector<String>(ThroughputQuota.COUNTER_NAME_TYPES.keySet())));
        quotaOptionsComboBox.addActionListener(new RunOnChangeListener(new Runnable() {
            @Override
            public void run() {
                updateCounterName();
            }
        }));

        Hashtable labelTable = new Hashtable();
        labelTable.put(new Integer(1), new JLabel("Consistency"));
        labelTable.put(new Integer(2), new JLabel(""));
        labelTable.put(new Integer(3), new JLabel("Scalability"));

        performanceSlider.setLabelTable(labelTable);
        performanceSlider.addChangeListener(new ChangeListener() {
            @Override
            public void stateChanged(ChangeEvent e) {
                if (!performanceSlider.getValueIsAdjusting()) {
                    int value = performanceSlider.getValue();
                    updatePerformanceLabel(value);
                }
            }
        });
        counterNameTextField.setDocument(new MaxLengthDocument(255));

        decrementRadio.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                byCheckBox.setEnabled(true);
                if(byCheckBox.isSelected()) {
                    byField.setEnabled(true);
                }
                quotaValueField.setEnabled(false);
                quotaUnitCombo.setEnabled(false);
                logOnlyCheckBox.setSelected(false);
                logOnlyCheckBox.setEnabled(false);
                performanceSlider.setEnabled(true);
            }
        });
        alwaysIncrementRadio.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                byCheckBox.setEnabled(true);
                if(byCheckBox.isSelected()) {
                    byField.setEnabled(true);
                }
                quotaValueField.setEnabled(true);
                quotaUnitCombo.setEnabled(true);
                logOnlyCheckBox.setEnabled(true);
                performanceSlider.setEnabled(true);
            }
        });
        incrementOnSuccessRadio.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                byCheckBox.setEnabled(true);
                if(byCheckBox.isSelected()) {
                    byField.setEnabled(true);
                }
                quotaValueField.setEnabled(true);
                quotaUnitCombo.setEnabled(true);
                logOnlyCheckBox.setEnabled(true);
                performanceSlider.setEnabled(true);
            }
        });
        byCheckBox.addItemListener(new ItemListener() {
            @Override
            public void itemStateChanged(ItemEvent e) {
                if(e.getStateChange() == ItemEvent.SELECTED) {
                    byField.setEnabled(true);
                } else {
                    byField.setEnabled(false);
                }
            }
        });
        resetRadio.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                byCheckBox.setEnabled(false);
                byField.setEnabled(false);
                quotaValueField.setEnabled(false);
                quotaUnitCombo.setEnabled(false);
                logOnlyCheckBox.setSelected(false);
                logOnlyCheckBox.setEnabled(false);
                performanceSlider.setEnabled(false);
                byCheckBox.setSelected(false);
                byField.setText("");

            }
        });
        okButton.setEnabled(!readOnly);
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

        helpButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                help();
            }
        });
    }

    private void updatePerformanceLabel(int value) {
        if(value == 1) {
            performanceDescriptionLabel.setText("Read and writes are performed synchronously with highest enforcement of quota.");
            //change text on the description
        } else if (value == 2) {
            //change text on the description
            performanceDescriptionLabel.setText("Improves scalability but may permit brief quota overflows under rare conditions.");
        } else {
            //change text on the description
            performanceDescriptionLabel.setText("Improves scalability further but also permits quota overflows.");
        }
    }

    /**
     * When switching quota options, the counter name should be updated automatically.
     */
    private void updateCounterName() {
        final String quotaOption = (String)quotaOptionsComboBox.getSelectedItem();
        final String counterName = counterNameTextField.getText().trim();

        counterNameTextField.setEditable(ThroughputQuota.PRESET_CUSTOM.equals(quotaOption));

        if (ThroughputQuota.PRESET_CUSTOM.equals(quotaOption)) {
            if (counterName == null || counterName.length() < 1) {
                counterNameTextField.setText(CounterPresetInfo.makeDefaultCustomExpr(uuid[0], expr));
            }
        } else {
            expr = ThroughputQuota.COUNTER_NAME_TYPES.get(quotaOption);
            if (CounterPresetInfo.isDefaultCustomExpr(counterName, ThroughputQuota.COUNTER_NAME_TYPES)) {
                counterNameTextField.setText(CounterPresetInfo.makeDefaultCustomExpr(uuid[0], expr));
            } else {
                String rawCounterName = CounterPresetInfo.findRawCounterName(
                        quotaOption, uuid[0], counterNameTextField.getText().trim(),
                        ThroughputQuota.PRESET_CUSTOM, ThroughputQuota.COUNTER_NAME_TYPES
                );

                CounterPresetInfo.findCounterNameKey(rawCounterName, uuid, ThroughputQuota.PRESET_CUSTOM, ThroughputQuota.COUNTER_NAME_TYPES);
                counterNameTextField.setText(CounterPresetInfo.makeDefaultCustomExpr(uuid[0], expr));
            }
        }
    }

    private void ok() {
        if(getData(subject)==null)
            return;
        oked = true;
        dispose();
    }
    public boolean wasOKed() {
        return oked;
    }

    private void cancel() {
        dispose();
    }

    private void help() {
        Actions.invokeHelp(ThroughputQuotaForm.this);
    }

    public static void main(String[] args) throws Exception {
        ThroughputQuota ass = new ThroughputQuota();
        for (int i = 0; i < 5; i++) {
            ThroughputQuotaForm me = new ThroughputQuotaForm(null, ass, false);
            me.pack();
            me.setVisible(true);
        }
        System.exit(0);
    }

    public ThroughputQuota getData(ThroughputQuota assertion) {
        String quota = quotaValueField.getText();
        String error = ThroughputQuota.validateQuota(quota);
        if (error != null) {
            JOptionPane.showMessageDialog(okButton, error, "Invalid value", JOptionPane.ERROR_MESSAGE);
            return null;
        }

        String quotaOption = (String) quotaOptionsComboBox.getSelectedItem();
        String counter = CounterPresetInfo.findRawCounterName(
                quotaOption, uuid[0], counterNameTextField.getText().trim(),
                ThroughputQuota.PRESET_CUSTOM, ThroughputQuota.COUNTER_NAME_TYPES
        );
        if (counter == null || counter.length() < 1) {
            JOptionPane.showMessageDialog(this,
                    "Please enter a counter name",
                    "Invalid value", JOptionPane.ERROR_MESSAGE);
            return null;
        }

        assertion.setCounterName(counter);
        assertion.setQuota(quota);
        assertion.setGlobal(true); // Note: do NOT remove this line.  Setting global as true is for backwards compatibility.  For more details, please check the definition of the variable, "global".
        assertion.setTimeUnit(quotaUnitCombo.getSelectedIndex() + 1);
        assertion.setVariablePrefix(varPrefixField.getVariable());

        int counterStrategy = 0;
        String byValueError = null;
        String byValueToStore = null;

        if(incrementOnSuccessRadio.isSelected()) {
            counterStrategy = ThroughputQuota.INCREMENT_ON_SUCCESS;
            if(byCheckBox.isSelected()) {
                byValueToStore = byField.getText();
                byValueError = ThroughputQuota.validateByValue(byValueToStore, counterStrategy);
            }
        } else if (alwaysIncrementRadio.isSelected()) {
            counterStrategy = ThroughputQuota.ALWAYS_INCREMENT;
            if(byCheckBox.isSelected()) {
                byValueToStore = byField.getText();
                byValueError = ThroughputQuota.validateByValue(byValueToStore, counterStrategy);
            }

        } else if (decrementRadio.isSelected()) {
            counterStrategy = ThroughputQuota.DECREMENT;
            if(byCheckBox.isSelected()) {
                byValueToStore = byField.getText();
                byValueError = ThroughputQuota.validateByValue(byValueToStore, counterStrategy);
            }

        } else if (resetRadio.isSelected()) {
            counterStrategy = ThroughputQuota.RESET;
        }

        if (!resetRadio.isSelected() && byCheckBox.isSelected() && (StringUtils.isBlank(byValueToStore) || byValueError != null)) {
            JOptionPane.showMessageDialog(this, byValueError, "Invalid value", JOptionPane.ERROR_MESSAGE);
            return null;
        }

        assertion.setCounterStrategy(counterStrategy);
        assertion.setByValue(byValueToStore);
        assertion.setLogOnly(logOnlyCheckBox.isSelected());

        assertion.setSynchronous((performanceSlider.getValue() != 2 && performanceSlider.getValue() != 3));
        assertion.setReadSynchronous(performanceSlider.getValue() != 3);

        return assertion;
    }

    public void setData(ThroughputQuota assertion) {
        this.subject = assertion;
        quotaValueField.setText(subject.getQuota());
        logOnlyCheckBox.setSelected(subject.isLogOnly());

        boolean writeSynchronous = subject.isSynchronous();
        boolean readSynchronous = subject.isReadSynchronous();

        if(writeSynchronous && readSynchronous) {
            performanceSlider.setValue(1);
            updatePerformanceLabel(1);
        } else if (readSynchronous) {
            performanceSlider.setValue(2);
            updatePerformanceLabel(2);
        } else {
            performanceSlider.setValue(3);
            updatePerformanceLabel(3);
        }

        DefaultComboBoxModel model = (DefaultComboBoxModel)quotaUnitCombo.getModel();
        model.setSelectedItem(TIME_UNITS[subject.getTimeUnit()-1]);

        varPrefixField.setAssertion(subject,getPreviousAssertion());

        if(StringUtils.isNotBlank(subject.getByValue())) {
            byCheckBox.setSelected(true);
            byField.setText(subject.getByValue());
        }

        String rawCounterName = subject.getCounterName();

        // Check if the assertion is in pre-6.2 version.  If so, set Custom option and display the counter name in Counter ID text field.
        if (subject.isGlobal() || ThroughputQuota.DEFAULT_COUNTER_NAME.equals(rawCounterName)) {
            if (new ThroughputQuota().getCounterName().equals(rawCounterName)) {
                rawCounterName = CounterPresetInfo.findRawCounterName(ThroughputQuota.PRESET_DEFAULT, uuid[0] = CounterPresetInfo.makeUuid(),
                        null, ThroughputQuota.PRESET_CUSTOM, ThroughputQuota.COUNTER_NAME_TYPES);
            }

            String quotaOption = CounterPresetInfo.findCounterNameKey(rawCounterName, uuid, ThroughputQuota.PRESET_CUSTOM, ThroughputQuota.COUNTER_NAME_TYPES);
            if (quotaOption == null) {
                quotaOptionsComboBox.setSelectedItem(ThroughputQuota.PRESET_CUSTOM);
                counterNameTextField.setText(rawCounterName);
            } else {
                quotaOptionsComboBox.setSelectedItem(quotaOption);
                expr = ThroughputQuota.COUNTER_NAME_TYPES.get(quotaOption);
                counterNameTextField.setText(CounterPresetInfo.makeDefaultCustomExpr(uuid[0], expr));
            }
        } else {
            quotaOptionsComboBox.setSelectedItem(ThroughputQuota.PRESET_CUSTOM);
            counterNameTextField.setText(rawCounterName);
        }

        switch (subject.getCounterStrategy()) {
            case ThroughputQuota.ALWAYS_INCREMENT:
                alwaysIncrementRadio.setSelected(true);
                break;
            case ThroughputQuota.INCREMENT_ON_SUCCESS:
                incrementOnSuccessRadio.setSelected(true);
                break;
            case ThroughputQuota.DECREMENT:
                decrementRadio.setSelected(true);
                quotaValueField.setEnabled(false);
                quotaUnitCombo.setEnabled(false);
                logOnlyCheckBox.setEnabled(false);
                break;
            case ThroughputQuota.RESET:
                resetRadio.setSelected(true);
                byCheckBox.setEnabled(false);
                performanceSlider.setEnabled(false);
                quotaValueField.setEnabled(false);
                quotaUnitCombo.setEnabled(false);
                logOnlyCheckBox.setEnabled(false);
        }
    }

    /**
     * Helper method which will create the UI components.
     */
    private void createUIComponents() {
        performanceDescriptionLabel = new JLabel();
    }
}
