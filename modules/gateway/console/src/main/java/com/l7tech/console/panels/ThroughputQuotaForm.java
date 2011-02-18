/**
 * LAYER 7 TECHNOLOGIES, INC<br/>
 *
 * User: flascell<br/>
 * Date: Apr 1, 2005<br/>
 */
package com.l7tech.console.panels;

import com.l7tech.console.action.Actions;
import com.l7tech.console.util.Registry;
import com.l7tech.gateway.common.service.ServiceAdmin;
import com.l7tech.objectmodel.FindException;
import com.l7tech.policy.assertion.Assertion;
import com.l7tech.policy.assertion.composite.CompositeAssertion;
import com.l7tech.policy.assertion.sla.ThroughputQuota;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.logging.Level;
import java.util.logging.Logger;

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
    private JComboBox counterNameCombo;
    private JRadioButton perRequestorRadio;
    private JRadioButton globalRadio;

    private static final String[] TIME_UNITS = {"second", "minute", "hour", "day", "month"};
    private static final ArrayList<String> counterNameInSessionOnly = new ArrayList<String>();

    private ThroughputQuota subject;
    private boolean oked = false;
    private final Logger logger = Logger.getLogger(ThroughputQuotaForm.class.getName());
    private JRadioButton alwaysIncrementRadio;
    private JRadioButton decrementRadio;
    private JRadioButton incrementOnSuccessRadio;
    private JPanel varPrefixFieldPanel;
    private TargetVariablePanel varPrefixField;

    public ThroughputQuotaForm(Frame owner, ThroughputQuota subject) {
        super(owner, subject, true);
        this.subject = subject;
        initComponents();
    }

    protected void initComponents() {
        getContentPane().add(mainPanel);

        // initial values

        DefaultComboBoxModel model = (DefaultComboBoxModel)quotaUnitCombo.getModel();
        for (String TIME_UNIT : TIME_UNITS) {
            model.addElement(TIME_UNIT);
        }
        counterNameCombo.setEditable(true);

        varPrefixField = new TargetVariablePanel();
        varPrefixFieldPanel.setLayout(new BorderLayout());
        varPrefixFieldPanel.add(varPrefixField, BorderLayout.CENTER);
        varPrefixField.addChangeListener(new ChangeListener(){
            @Override
            public void stateChanged(ChangeEvent e) {
                okButton.setEnabled(varPrefixField .isEntryValid());
            }
        });

        varPrefixField.setVariable(subject.getVariablePrefix());
        varPrefixField.setSuffixes(subject.getVariableSuffixes());
        varPrefixField.setAcceptEmpty(true);

        ButtonGroup bg = new ButtonGroup();
        bg.add(perRequestorRadio);
        bg.add(globalRadio);


        bg = new ButtonGroup();
        bg.add(alwaysIncrementRadio);
        bg.add(decrementRadio);
        bg.add(incrementOnSuccessRadio);

        decrementRadio.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                quotaValueField.setEnabled(false);
                quotaUnitCombo.setEnabled(false);
            }
        });
        alwaysIncrementRadio.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                quotaValueField.setEnabled(true);
                quotaUnitCombo.setEnabled(true);
            }
        });
        incrementOnSuccessRadio.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                quotaValueField.setEnabled(true);
                quotaUnitCombo.setEnabled(true);
            }
        });

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

    private void populateExistingCounterNamesFromPolicy(Assertion toInspect, java.util.List container) {
        if (toInspect == subject) {
            return; // skip us
        } else if (toInspect instanceof CompositeAssertion) {
            CompositeAssertion ca = (CompositeAssertion)toInspect;
            for (Iterator i = ca.children(); i.hasNext();) {
                Assertion a = (Assertion)i.next();
                populateExistingCounterNamesFromPolicy(a, container);
            }
        } else if (toInspect instanceof ThroughputQuota) {
            ThroughputQuota tq = (ThroughputQuota)toInspect;
            String maybenew = tq.getCounterName();
            if (maybenew != null && !container.contains(maybenew)) {
                container.add(maybenew);
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
            ThroughputQuotaForm me = new ThroughputQuotaForm(null, ass);
            me.pack();
            me.setVisible(true);
        }
        System.exit(0);
    }

    public ThroughputQuota getData(ThroughputQuota assertion){
        String quota = quotaValueField.getText();
        String error = ThroughputQuota.validateQuota(quota);
        if (error != null) {
            JOptionPane.showMessageDialog(okButton,
                                          error,
                                          "Invalid value", JOptionPane.ERROR_MESSAGE);
            return null;
        }

        String counter = (String)counterNameCombo.getSelectedItem();
        if (counter == null || counter.length() < 1) {
            JOptionPane.showMessageDialog(okButton,
                              "Please enter a counter name",
                              "Invalid value", JOptionPane.ERROR_MESSAGE);
            return null;
        }
        
        boolean newCounterName = counterNameCombo.getSelectedIndex() == -1;
        assertion.setCounterName(counter);
        assertion.setQuota(quota);
        assertion.setGlobal(globalRadio.isSelected());
        assertion.setTimeUnit(quotaUnitCombo.getSelectedIndex()+1);
        assertion.setVariablePrefix(varPrefixField.getVariable());

        int counterStrategy = ThroughputQuota.INCREMENT_ON_SUCCESS;
        if (alwaysIncrementRadio.isSelected()) {
            counterStrategy = ThroughputQuota.ALWAYS_INCREMENT;
        } else if (decrementRadio.isSelected()) {
            counterStrategy = ThroughputQuota.DECREMENT;
        }
        assertion.setCounterStrategy(counterStrategy);
        if (newCounterName) {
            // remember this as part of this admin session
            counterNameInSessionOnly.add(counter);
        }
        return assertion;
    }

    public void setData(ThroughputQuota assertion) {
        this.subject = assertion;
        quotaValueField.setText(subject.getQuota());

        DefaultComboBoxModel model = (DefaultComboBoxModel)quotaUnitCombo.getModel();
        model.setSelectedItem(TIME_UNITS[subject.getTimeUnit()-1]);

        varPrefixField.setAssertion(subject,getPreviousAssertion());
        // add the counters that are not on gateway but are in the policy
        Assertion root = subject.getParent();
        Assertion prevRoot = getPreviousAssertion()!=null ? getPreviousAssertion().getParent(): null;
        if (root == null && prevRoot != null) {
            root = prevRoot;
        } else {
            if (root != null){
                while (root.getParent() != null) {
                    root = root.getParent();
                }
            }
        }

        // get counter names from other sla assertions here
        ArrayList<String> listofexistingcounternames = new ArrayList<String>();
        // start by the counters that are already defined on gateway
        ServiceAdmin serviceAdmin = Registry.getDefault().getServiceManager();
        try {
            String[] gatewayCounter = serviceAdmin.listExistingCounterNames();
            listofexistingcounternames.addAll(Arrays.asList(gatewayCounter));
        } catch (FindException e) {
            logger.log(Level.WARNING, "cannot get counters from gateway", e);
        }
        // add session counternames
        for (String counterName : counterNameInSessionOnly) {
            if (!listofexistingcounternames.contains(counterName)) listofexistingcounternames.add(counterName);

        }
        populateExistingCounterNamesFromPolicy(root, listofexistingcounternames);
        String thisValue = subject.getCounterName();
        if (thisValue != null && !listofexistingcounternames.contains(thisValue)) {
            listofexistingcounternames.add(thisValue);
        }
        model = (DefaultComboBoxModel)counterNameCombo.getModel();
        for (String listofexistingcountername : listofexistingcounternames) {
            model.addElement(listofexistingcountername);
        }
        if (thisValue != null) {
            model.setSelectedItem(thisValue);
        }
        if (subject.isGlobal()) {
            globalRadio.setSelected(true);
        } else {
            perRequestorRadio.setSelected(true);
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
                break;
        }
    }

}
