/**
 * LAYER 7 TECHNOLOGIES, INC<br/>
 *
 * User: flascell<br/>
 * Date: Apr 1, 2005<br/>
 */
package com.l7tech.external.assertions.throughputquota.console;

import com.l7tech.console.action.Actions;
import com.l7tech.console.panels.LegacyAssertionPropertyDialog;
import com.l7tech.console.panels.TargetVariablePanel;
import com.l7tech.external.assertions.throughputquota.ThroughputQuotaAssertion;
import com.l7tech.gui.MaxLengthDocument;
import com.l7tech.gui.util.RunOnChangeListener;
import com.l7tech.policy.assertion.sla.CounterPresetInfo;
import org.apache.commons.lang.StringUtils;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ItemEvent;
import java.util.Hashtable;
import java.util.Vector;

/**
 * A dialog for editing a ThroughputQuotaAssertion dialog.
 *
 * @author flascelles@layer7-tech.com
 */
public class ThroughputQuotaForm extends LegacyAssertionPropertyDialog {
    private static final String MSG_DLG_TITLE_INVALID_VAL = "Invalid value";
    private JPanel mainPanel;
    private JButton okButton;
    private JButton cancelButton;
    private JButton helpButton;
    private JTextField quotaValueField;
    private JComboBox quotaUnitCombo;
    private JComboBox quotaOptionsComboBox;
    private JTextField counterNameTextField;

    private static final String[] TIME_UNITS = {"second", "minute", "hour", "day", "month"};

    private ThroughputQuotaAssertion subject;
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

    private String[] uuid = {CounterPresetInfo.makeUuid()};
    private String expr = "";

    public ThroughputQuotaForm(Frame owner, ThroughputQuotaAssertion subject, boolean readOnly) {
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
        varPrefixField.addChangeListener(e -> okButton.setEnabled(!readOnly && varPrefixField.isEntryValid()));

        varPrefixField.setVariable(subject.getVariablePrefix());
        varPrefixField.setSuffixes(subject.getVariableSuffixes());
        varPrefixField.setAcceptEmpty(true);

        quotaOptionsComboBox.setModel(new DefaultComboBoxModel(new Vector<>(ThroughputQuotaAssertion.getCounterNameTypes().keySet())));
        quotaOptionsComboBox.addActionListener(new RunOnChangeListener(this::updateCounterName));

        Hashtable labelTable = new Hashtable();
        labelTable.put(1, new JLabel("Consistency"));
        labelTable.put(2, new JLabel(""));
        labelTable.put(3, new JLabel("Scalability"));

        performanceSlider.setLabelTable(labelTable);
        performanceSlider.addChangeListener(e -> {
            if (!performanceSlider.getValueIsAdjusting()) {
                int value = performanceSlider.getValue();
                updatePerformanceLabel(value);
            }
        });
        counterNameTextField.setDocument(new MaxLengthDocument(255));

        decrementRadio.addActionListener(e -> {
            byCheckBox.setEnabled(true);
            if(byCheckBox.isSelected()) {
                byField.setEnabled(true);
            }
            quotaValueField.setEnabled(false);
            quotaUnitCombo.setEnabled(false);
            logOnlyCheckBox.setSelected(false);
            logOnlyCheckBox.setEnabled(false);
            performanceSlider.setEnabled(true);
        });
        alwaysIncrementRadio.addActionListener(e -> {
            byCheckBox.setEnabled(true);
            if(byCheckBox.isSelected()) {
                byField.setEnabled(true);
            }
            quotaValueField.setEnabled(true);
            quotaUnitCombo.setEnabled(true);
            logOnlyCheckBox.setEnabled(true);
            performanceSlider.setEnabled(true);
        });
        incrementOnSuccessRadio.addActionListener(e -> {
            byCheckBox.setEnabled(true);
            if(byCheckBox.isSelected()) {
                byField.setEnabled(true);
            }
            quotaValueField.setEnabled(true);
            quotaUnitCombo.setEnabled(true);
            logOnlyCheckBox.setEnabled(true);
            performanceSlider.setEnabled(true);
        });
        byCheckBox.addItemListener(e -> {
            if(e.getStateChange() == ItemEvent.SELECTED) {
                byField.setEnabled(true);
            } else {
                byField.setEnabled(false);
            }
        });
        resetRadio.addActionListener(e -> {
            byCheckBox.setEnabled(false);
            byField.setEnabled(false);
            quotaValueField.setEnabled(false);
            quotaUnitCombo.setEnabled(false);
            logOnlyCheckBox.setSelected(false);
            logOnlyCheckBox.setEnabled(false);
            performanceSlider.setEnabled(false);
            byCheckBox.setSelected(false);
            byField.setText("");

        });
        okButton.setEnabled(!readOnly);
        okButton.addActionListener(e -> ok());

        cancelButton.addActionListener(e -> cancel());

        helpButton.addActionListener(e -> help());
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

        counterNameTextField.setEditable(ThroughputQuotaAssertion.PRESET_CUSTOM.equals(quotaOption));

        if (ThroughputQuotaAssertion.PRESET_CUSTOM.equals(quotaOption)) {
            if (counterName == null || counterName.length() < 1) {
                counterNameTextField.setText(CounterPresetInfo.makeDefaultCustomExpr(uuid[0], expr));
            }
        } else {
            expr = ThroughputQuotaAssertion.getCounterNameTypes().get(quotaOption);
            if (CounterPresetInfo.isDefaultCustomExpr(counterName, ThroughputQuotaAssertion.getCounterNameTypes())) {
                counterNameTextField.setText(CounterPresetInfo.makeDefaultCustomExpr(uuid[0], expr));
            } else {
                String rawCounterName = CounterPresetInfo.findRawCounterName(
                        quotaOption, uuid[0], counterNameTextField.getText().trim(),
                        ThroughputQuotaAssertion.PRESET_CUSTOM, ThroughputQuotaAssertion.getCounterNameTypes()
                );

                CounterPresetInfo.findCounterNameKey(rawCounterName, uuid, ThroughputQuotaAssertion.PRESET_CUSTOM, ThroughputQuotaAssertion.getCounterNameTypes());
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
        ThroughputQuotaAssertion ass = new ThroughputQuotaAssertion();
        for (int i = 0; i < 5; i++) {
            ThroughputQuotaForm me = new ThroughputQuotaForm(null, ass, false);
            me.pack();
            me.setVisible(true);
        }
        System.exit(0);
    }

    public ThroughputQuotaAssertion getData(ThroughputQuotaAssertion assertion) {
        String quota = quotaValueField.getText();
        String error = ThroughputQuotaAssertion.validateQuota(quota);
        if (error != null) {
            JOptionPane.showMessageDialog(okButton, error, MSG_DLG_TITLE_INVALID_VAL, JOptionPane.ERROR_MESSAGE);
            return null;
        }

        String quotaOption = (String) quotaOptionsComboBox.getSelectedItem();
        String counter = CounterPresetInfo.findRawCounterName(
                quotaOption, uuid[0], counterNameTextField.getText().trim(),
                ThroughputQuotaAssertion.PRESET_CUSTOM, ThroughputQuotaAssertion.getCounterNameTypes()
        );
        if (StringUtils.isBlank(counter)) {
            JOptionPane.showMessageDialog(this,
                    "Please enter a counter name",
                    MSG_DLG_TITLE_INVALID_VAL, JOptionPane.ERROR_MESSAGE);
            return null;
        }

        assertion.setCounterName(counter);
        assertion.setQuota(quota);
        assertion.setGlobal(true); // Note: do NOT remove this line.  Setting global as true is for backwards compatibility.  For more details, please check the definition of the variable, "global".
        assertion.setTimeUnit(quotaUnitCombo.getSelectedIndex() + 1);
        assertion.setVariablePrefix(varPrefixField.getVariable());

        int counterStrategy = getCounterStrategy();
        String byValueError = null;
        String byValueToStore = null;

        if ((counterStrategy == ThroughputQuotaAssertion.INCREMENT_ON_SUCCESS ||
                counterStrategy == ThroughputQuotaAssertion.ALWAYS_INCREMENT ||
                counterStrategy == ThroughputQuotaAssertion.DECREMENT) &&
                byCheckBox.isSelected()){
            byValueToStore = byField.getText();
            byValueError = ThroughputQuotaAssertion.validateByValue(byValueToStore, counterStrategy);
        }

        if (!resetRadio.isSelected() && byCheckBox.isSelected() && (StringUtils.isBlank(byValueToStore) || byValueError != null)) {
            JOptionPane.showMessageDialog(this, byValueError, MSG_DLG_TITLE_INVALID_VAL, JOptionPane.ERROR_MESSAGE);
            return null;
        }

        assertion.setCounterStrategy(counterStrategy);
        assertion.setByValue(byValueToStore);
        assertion.setLogOnly(logOnlyCheckBox.isSelected());

        assertion.setSynchronous((performanceSlider.getValue() != 2 && performanceSlider.getValue() != 3));
        assertion.setReadSynchronous(performanceSlider.getValue() != 3);

        return assertion;
    }

    private int getCounterStrategy() {
        int counterStrategy = 0;

        if(incrementOnSuccessRadio.isSelected()) {
            counterStrategy = ThroughputQuotaAssertion.INCREMENT_ON_SUCCESS;
        } else if (alwaysIncrementRadio.isSelected()) {
            counterStrategy = ThroughputQuotaAssertion.ALWAYS_INCREMENT;
        } else if (decrementRadio.isSelected()) {
            counterStrategy = ThroughputQuotaAssertion.DECREMENT;
        } else if (resetRadio.isSelected()) {
            counterStrategy = ThroughputQuotaAssertion.RESET;
        }

        return counterStrategy;
    }

    public void setData(ThroughputQuotaAssertion assertion) {
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
        if (subject.isGlobal() || ThroughputQuotaAssertion.DEFAULT_COUNTER_NAME.equals(rawCounterName)) {
            if (new ThroughputQuotaAssertion().getCounterName().equals(rawCounterName)) {
                uuid[0] = CounterPresetInfo.makeUuid();
                rawCounterName = CounterPresetInfo.findRawCounterName(ThroughputQuotaAssertion.PRESET_DEFAULT, uuid[0],
                        null, ThroughputQuotaAssertion.PRESET_CUSTOM, ThroughputQuotaAssertion.getCounterNameTypes());
            }

            String quotaOption = CounterPresetInfo.findCounterNameKey(rawCounterName, uuid, ThroughputQuotaAssertion.PRESET_CUSTOM, ThroughputQuotaAssertion.getCounterNameTypes());
            if (quotaOption == null) {
                quotaOptionsComboBox.setSelectedItem(ThroughputQuotaAssertion.PRESET_CUSTOM);
                counterNameTextField.setText(rawCounterName);
            } else {
                quotaOptionsComboBox.setSelectedItem(quotaOption);
                expr = ThroughputQuotaAssertion.getCounterNameTypes().get(quotaOption);
                counterNameTextField.setText(CounterPresetInfo.makeDefaultCustomExpr(uuid[0], expr));
            }
        } else {
            quotaOptionsComboBox.setSelectedItem(ThroughputQuotaAssertion.PRESET_CUSTOM);
            counterNameTextField.setText(rawCounterName);
        }

        switch (subject.getCounterStrategy()) {
            case ThroughputQuotaAssertion.ALWAYS_INCREMENT:
                alwaysIncrementRadio.setSelected(true);
                break;
            case ThroughputQuotaAssertion.INCREMENT_ON_SUCCESS:
                incrementOnSuccessRadio.setSelected(true);
                break;
            case ThroughputQuotaAssertion.DECREMENT:
                decrementRadio.setSelected(true);
                quotaValueField.setEnabled(false);
                quotaUnitCombo.setEnabled(false);
                logOnlyCheckBox.setEnabled(false);
                break;
            case ThroughputQuotaAssertion.RESET:
                resetRadio.setSelected(true);
                byCheckBox.setEnabled(false);
                performanceSlider.setEnabled(false);
                quotaValueField.setEnabled(false);
                quotaUnitCombo.setEnabled(false);
                logOnlyCheckBox.setEnabled(false);
                break;
            default:
                throw new IllegalStateException("Unknown counter strategy: " + subject.getCounterStrategy());
        }
    }

    /**
     * Helper method which will create the UI components.
     */
    private void createUIComponents() {
        performanceDescriptionLabel = new JLabel();
    }
}
