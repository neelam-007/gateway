/**
 * LAYER 7 TECHNOLOGIES, INC<br/>
 *
 * User: flascell<br/>
 * Date: Apr 1, 2005<br/>
 */
package com.l7tech.console.panels;

import com.l7tech.policy.assertion.sla.ThroughputQuota;
import com.l7tech.policy.assertion.Assertion;
import com.l7tech.policy.assertion.composite.CompositeAssertion;
import com.l7tech.console.action.Actions;
import com.l7tech.console.util.Registry;
import com.l7tech.service.ServiceAdmin;
import com.l7tech.objectmodel.FindException;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import java.util.Iterator;
import java.util.ArrayList;
import java.util.logging.Logger;
import java.util.logging.Level;

/**
 * A dialog for editing a ThroughputQuota dialog.
 *
 * @author flascelles@layer7-tech.com
 */
public class ThroughputQuotaForm extends JDialog {
    private JPanel mainPanel;
    private JButton okButton;
    private JButton cancelButton;
    private JButton helpButton;
    private JTextField quotaValueField;
    private JComboBox quotaUnitCombo;
    private JComboBox counterNameCombo;
    private JRadioButton perRequestorRadio;
    private JRadioButton globalRadio;

    private static final String[] TIME_UNITS = {"second", "hour", "day", "month"};
    private static final ArrayList counterNameInSessionOnly = new ArrayList();

    private ThroughputQuota subject;
    private boolean oked = false;
    //private PublishedService service;
    private Assertion policyRoot;
    private final Logger logger = Logger.getLogger(ThroughputQuotaForm.class.getName());
    private JRadioButton alwaysIncrementRadio;
    private JRadioButton decrementRadio;
    private JRadioButton incrementOnSuccessRadio;

    public ThroughputQuotaForm(Frame owner, ThroughputQuota subject, Assertion policyRoot) {
        super(owner, true);
        setTitle("Throughput Quota Assertion");
        if (subject == null) throw new IllegalArgumentException("subject cannot be null");
        this.subject = subject;
        this.policyRoot = policyRoot;
        initialize();
    }

    private void initialize() {
        getContentPane().add(mainPanel);

        // initial values
        quotaValueField.setText(Long.toString(subject.getQuota()));
        DefaultComboBoxModel model = (DefaultComboBoxModel)quotaUnitCombo.getModel();
        for (int i = 0; i < TIME_UNITS.length; i++) {
            model.addElement(TIME_UNITS[i]);
        }
        model.setSelectedItem(TIME_UNITS[subject.getTimeUnit()-1]);
        counterNameCombo.setEditable(true);

        // get counter names from other sla assertions here
        ArrayList listofexistingcounternames = new ArrayList();
        // start by the counters that are already defined on gateway
        ServiceAdmin serviceAdmin = Registry.getDefault().getServiceManager();
        try {
            String[] gatewayCounter = serviceAdmin.listExistingCounterNames();
            for (int i = 0; i < gatewayCounter.length; i++) {
                listofexistingcounternames.add(gatewayCounter[i]);
            }
        } catch (FindException e) {
            logger.log(Level.WARNING, "cannot get counters from gateway", e);
        }
        // add session counternames
        for (Iterator iterator = counterNameInSessionOnly.iterator(); iterator.hasNext();) {
            String s = (String) iterator.next();
            if (!listofexistingcounternames.contains(s)) listofexistingcounternames.add(s);

        }
        // add the counters that are not on gateway but are in the policy
        Assertion root = subject.getParent();
        if (root == null && policyRoot != null) {
            root = policyRoot;
        } else {
            if (root != null){
                while (root.getParent() != null) {
                    root = root.getParent();
                }
            }
        }
        populateExistingCounterNamesFromPolicy(root, listofexistingcounternames);
        String thisValue = subject.getCounterName();
        if (thisValue != null && !listofexistingcounternames.contains(thisValue)) {
            listofexistingcounternames.add(thisValue);
        }
        model = (DefaultComboBoxModel)counterNameCombo.getModel();
        for (Iterator iterator = listofexistingcounternames.iterator(); iterator.hasNext();) {
            String s = (String) iterator.next();
            model.addElement(s);
        }
        if (thisValue != null) {
            model.setSelectedItem(thisValue);
        }

        ButtonGroup bg = new ButtonGroup();
        bg.add(perRequestorRadio);
        bg.add(globalRadio);
        if (subject.isGlobal()) {
            globalRadio.setSelected(true);
        } else {
            perRequestorRadio.setSelected(true);
        }

        bg = new ButtonGroup();
        bg.add(alwaysIncrementRadio);
        bg.add(decrementRadio);
        bg.add(incrementOnSuccessRadio);
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

        decrementRadio.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                quotaValueField.setEnabled(false);
                quotaUnitCombo.setEnabled(false);
            }
        });
        alwaysIncrementRadio.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                quotaValueField.setEnabled(true);
                quotaUnitCombo.setEnabled(true);
            }
        });
        incrementOnSuccessRadio.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                quotaValueField.setEnabled(true);
                quotaUnitCombo.setEnabled(true);
            }
        });

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

        helpButton.addActionListener(new ActionListener() {
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
        String tmp = quotaValueField.getText();
        if (tmp == null || tmp.length() < 1) {
            JOptionPane.showMessageDialog(okButton,
                                          "Please enter a quota value between 1 and 100000",
                                          "Invalid value", JOptionPane.ERROR_MESSAGE);
            return;
        }
        long qval = 0;
        try {
            qval = Long.parseLong(tmp);
        } catch (NumberFormatException e) {
            JOptionPane.showMessageDialog(okButton,
                                          "Please enter a quota value between 1 and 100000",
                                          "Invalid value", JOptionPane.ERROR_MESSAGE);
            return;
        }
        if (qval < 1 || qval > 100000) {
            JOptionPane.showMessageDialog(okButton,
                                          "Please enter a quota value between 1 and 100000",
                                          "Invalid value", JOptionPane.ERROR_MESSAGE);
            return;
        }
        tmp = (String)counterNameCombo.getSelectedItem();
        if (tmp == null || tmp.length() < 1) {
            JOptionPane.showMessageDialog(okButton,
                                          "Please enter a counter name",
                                          "Invalid value", JOptionPane.ERROR_MESSAGE);
            return;
        }
        boolean newCounterName = counterNameCombo.getSelectedIndex() == -1;
        subject.setCounterName(tmp);
        subject.setQuota(qval);
        subject.setGlobal(globalRadio.isSelected());
        subject.setTimeUnit(quotaUnitCombo.getSelectedIndex()+1);

        int counterStrategy = ThroughputQuota.INCREMENT_ON_SUCCESS;
        if (alwaysIncrementRadio.isSelected()) {
            counterStrategy = ThroughputQuota.ALWAYS_INCREMENT;
        } else if (decrementRadio.isSelected()) {
            counterStrategy = ThroughputQuota.DECREMENT;
        }
        subject.setCounterStrategy(counterStrategy);
        oked = true;
        if (newCounterName) {
            // remember this as part of this admin session
            counterNameInSessionOnly.add(tmp);
        }
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
            ThroughputQuotaForm me = new ThroughputQuotaForm(null, ass, null);
            me.pack();
            me.setVisible(true);
        }
        System.exit(0);
    }
}
