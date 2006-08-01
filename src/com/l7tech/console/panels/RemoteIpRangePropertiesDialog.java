package com.l7tech.console.panels;

import com.l7tech.console.action.Actions;
import com.l7tech.console.event.PolicyEvent;
import com.l7tech.console.event.PolicyListener;
import com.l7tech.policy.AssertionPath;
import com.l7tech.policy.assertion.Assertion;
import com.l7tech.policy.assertion.RemoteIpRange;
import com.l7tech.common.gui.util.Utilities;

import javax.swing.*;
import javax.swing.event.EventListenerList;
import javax.swing.text.NumberFormatter;
import javax.swing.text.DefaultFormatterFactory;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.EventListener;
import java.util.Locale;
import java.util.ResourceBundle;
import java.util.StringTokenizer;

/**
 * Dialog for viewing and editing a RemoteIpRange assertion.
 *
 * <br/><br/>
 * LAYER 7 TECHNOLOGIES, INC<br/>
 * User: flascell<br/>
 * Date: Feb 20, 2004<br/>
 */
public class RemoteIpRangePropertiesDialog extends JDialog {
    private JButton okButton;
    private JButton cancelButton;
    private JButton helpButton;
    private JFormattedTextField add1;
    private JFormattedTextField add2;
    private JFormattedTextField add3;
    private JFormattedTextField add4;
    private JFormattedTextField suffix;
    private JComboBox includeExcludeCombo;
    private JPanel mainPanel;

    private ResourceBundle resources;
    private boolean oked;
    private RemoteIpRange subject;
    private final EventListenerList listenerList = new EventListenerList();
    private JRadioButton tcpRadio;
    private JRadioButton contextVarRadio;
    private JTextField contextVarField;

    public RemoteIpRangePropertiesDialog(Frame owner, boolean modal, RemoteIpRange subject) {
        super(owner, modal);
        this.subject = subject;
        initialize();
        oked = false;
    }

    public void addPolicyListener(PolicyListener listener) {
        listenerList.add(PolicyListener.class, listener);
    }

    /**
     * @return false if the dialog was dismissed or canceled, true if it was oked.
     */
    public boolean wasOked() {
        return oked;
    }

    private void initialize() {
        setModal(true);
        initResources();
        setContentPane(mainPanel);
        Utilities.equalizeButtonSizes(new AbstractButton[] {okButton, cancelButton, helpButton});
        setTitle(resources.getString("window.title"));
        Utilities.setEscKeyStrokeDisposes(this);

        includeExcludeCombo.setModel(new DefaultComboBoxModel(new String[] {
                                                          resources.getString("includeExcludeCombo.include"),
                                                          resources.getString("includeExcludeCombo.exclude")}));

        NumberFormatter formatter = new NumberFormatter();
        formatter.setMaximum(255);
        formatter.setMinimum(0);
        add1.setFormatterFactory(new DefaultFormatterFactory(formatter));
        add2.setFormatterFactory(new DefaultFormatterFactory(formatter));
        add3.setFormatterFactory(new DefaultFormatterFactory(formatter));
        add4.setFormatterFactory(new DefaultFormatterFactory(formatter));
        formatter = new NumberFormatter();
        formatter.setMaximum(32);
        formatter.setMinimum(0);
        suffix.setFormatterFactory(new DefaultFormatterFactory(formatter));

        setCallbacks();
        setInitialValues();
    }

    private void initResources() {
        Locale locale = Locale.getDefault();
        resources = ResourceBundle.getBundle("com.l7tech.console.resources.RemoteIpRangePropertiesDialog", locale);
    }

    private void cancel() {
        RemoteIpRangePropertiesDialog.this.dispose();
    }

    private void ok() {
        String contextval = null;
        if (contextVarRadio.isSelected()) {
            contextval = contextVarField.getText();
            if (contextval == null || contextval.length() < 1) {
                bark("The source for the requestor ip should either be tcp or be a context variable");
                return;
            }
        }
        // get rule
        int index = includeExcludeCombo.getSelectedIndex();
        // get address
        String add1Str = add1.getText();
        if (add1Str == null || add1Str.length() < 1) {
            bark(resources.getString("error.badaddress"));
            return;
        }
        String add2Str = add2.getText();
        if (add2Str == null || add2Str.length() < 1) {
            bark(resources.getString("error.badaddress"));
            return;
        }
        String add3Str = add3.getText();
        if (add3Str == null || add3Str.length() < 1) {
            bark(resources.getString("error.badaddress"));
            return;
        }
        String add4Str = add4.getText();
        if (add4Str == null || add4Str.length() < 1) {
            bark(resources.getString("error.badaddress"));
            return;
        }
        String newaddress = add1Str + "." + add2Str + "." + add3Str + "." + add4Str;
        // get mask
        String suffixStr = suffix.getText();
        if (suffixStr == null || suffixStr.length() < 1) {
            bark(resources.getString("error.badmask"));
            return;
        }
        if (subject != null) {
            // all is good. record values and get out o here
            switch (index) {
                case 0:
                    subject.setAllowRange(true);
                    break;
                case 1:
                    subject.setAllowRange(false);
                    break;
            }
            subject.setStartIp(newaddress);
            subject.setNetworkMask(Integer.parseInt(suffixStr));
            subject.setIpSourceContextVariable(contextval);
            fireEventAssertionChanged(subject);
            oked = true;
        }
        RemoteIpRangePropertiesDialog.this.dispose();
    }

    private void fireEventAssertionChanged(final Assertion a) {
        SwingUtilities.invokeLater(
          new Runnable() {
              public void run() {
                  int[] indices = new int[a.getParent().getChildren().indexOf(a)];
                  PolicyEvent event = new
                          PolicyEvent(this, new AssertionPath(a.getPath()), indices, new Assertion[]{a});
                  EventListener[] listeners = listenerList.getListeners(PolicyListener.class);
                  for (EventListener listener : listeners) {
                      ((PolicyListener) listener).assertionsChanged(event);
                  }
              }
          });
    }

    private void bark(String woof) {
        JOptionPane.showMessageDialog(this, woof, resources.getString("window.title"), JOptionPane.ERROR_MESSAGE);
    }

    private void setInitialValues() {
        contextVarRadio.setSelected(false);
        tcpRadio.setSelected(true);
        contextVarField.setEnabled(false);
        if (subject != null) {
            // get values to populate with
            int index = subject.isAllowRange() ? 0 : 1;
            includeExcludeCombo.setSelectedIndex(index);
            includeExcludeCombo.setPreferredSize(new Dimension(100, 25));
            int[] address = decomposeAddress(subject.getStartIp());
            add1.setText("" + address[0]);
            add2.setText("" + address[1]);
            add3.setText("" + address[2]);
            add4.setText("" + address[3]);
            suffix.setText("" + subject.getNetworkMask());
            if (subject.getIpSourceContextVariable() == null) {
                contextVarRadio.setSelected(false);
                tcpRadio.setSelected(true);
                contextVarField.setEnabled(false);
            } else {
                contextVarRadio.setSelected(true);
                tcpRadio.setSelected(false);
                contextVarField.setEnabled(true);
                contextVarField.setText(subject.getIpSourceContextVariable());
            }
        }
    }

    private int[] decomposeAddress(String arg) {
        StringTokenizer st = new StringTokenizer(arg, ".");
        int[] output = new int[4];
        output[0] = Integer.parseInt((String) st.nextElement());
        output[1] = Integer.parseInt((String) st.nextElement());
        output[2] = Integer.parseInt((String) st.nextElement());
        output[3] = Integer.parseInt(((String) st.nextElement()).trim());
        return output;
    }

    private void setCallbacks() {
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
                Actions.invokeHelp(RemoteIpRangePropertiesDialog.this);
            }
        });
        tcpRadio.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                contextVarRadio.setSelected(!tcpRadio.isSelected());
                contextVarField.setEnabled(!tcpRadio.isSelected());
                addDefaultCntxVar();
            }
        });
        contextVarRadio.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                tcpRadio.setSelected(!contextVarRadio.isSelected());
                contextVarField.setEnabled(contextVarRadio.isSelected());
                addDefaultCntxVar();
            }
        });
    }

    private void addDefaultCntxVar() {
        if (contextVarField.isEnabled() && (contextVarField.getText() == null || contextVarField.getText().length() < 1)) {
            contextVarField.setText("request.http.header.remoteip");
        }
    }
}
