package com.l7tech.console.panels;

import com.l7tech.console.action.Actions;
import com.l7tech.console.event.PolicyListener;
import com.l7tech.console.event.PolicyEvent;
import com.l7tech.policy.assertion.RemoteIpRange;
import com.l7tech.policy.assertion.Assertion;
import com.l7tech.policy.AssertionPath;

import javax.swing.*;
import javax.swing.event.EventListenerList;
import javax.swing.text.NumberFormatter;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.StringTokenizer;
import java.util.EventListener;
import java.util.Locale;
import java.util.ResourceBundle;

/**
 * Dialog for viewing and editing a RemoteIpRange assertion.
 *
 * <br/><br/>
 * LAYER 7 TECHNOLOGIES, INC<br/>
 * User: flascell<br/>
 * Date: Feb 20, 2004<br/>
 * $Id$<br/>
 *
 */
public class RemoteIpRangePropertiesDialog extends JDialog {
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
        initResources();
        setTitle(resources.getString("window.title"));
        Container contents = getContentPane();
        contents.setLayout(new BorderLayout(0,0));
        contents.add(makeGlobalPanel(), BorderLayout.CENTER);
        contents.add(makeBottomButtonsPanel(), BorderLayout.SOUTH);
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
                  for (int i = 0; i < listeners.length; i++) {
                      ((PolicyListener)listeners[i]).assertionsChanged(event);
                  }
              }
          });
    }

    private void bark(String woof) {
        JOptionPane.showMessageDialog(this, woof, resources.getString("window.title"), JOptionPane.ERROR_MESSAGE);
    }

    private void setInitialValues() {
        add1.setText("8888");
        Dimension preferredSize = add1.getPreferredSize();
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
        }
        // resize equally
        add1.setPreferredSize(preferredSize);
        add2.setPreferredSize(preferredSize);
        add3.setPreferredSize(preferredSize);
        add4.setPreferredSize(preferredSize);
        suffix.setPreferredSize(preferredSize);
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
        helpButton.addActionListener( new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                Actions.invokeHelp(RemoteIpRangePropertiesDialog.this);
            }
        });
    }

    private JPanel makeGlobalPanel() {
        JPanel mainPanel = new JPanel();
        mainPanel.setLayout(new GridLayout(3, 1, 0, CONTROL_SPACING));

        mainPanel.add(makeRulePanel());
        mainPanel.add(makeNotePanel());
        mainPanel.add(makeIPRangePanel());

        GridBagConstraints constraints = new GridBagConstraints();
        constraints.fill = GridBagConstraints.BOTH;
        constraints.weightx = 1.0;
        constraints.weighty = 1.0;
        constraints.insets = new Insets(BORDER_PADDING, BORDER_PADDING, 0, BORDER_PADDING);
        JPanel bordered = new JPanel();
        bordered.setLayout(new GridBagLayout());
        bordered.add(mainPanel, constraints);
        return bordered;
    }

    private JPanel makeRulePanel() {
        JPanel panel = new JPanel();
        panel.setLayout(new FlowLayout(FlowLayout.LEADING, 0, 0));
        includeExcludeCombo = new JComboBox(new String[] {resources.getString("includeExcludeCombo.include"),
                                                          resources.getString("includeExcludeCombo.exclude")});
        panel.add(includeExcludeCombo);
        return panel;
    }

    private JPanel makeNotePanel() {
        JPanel panel = new JPanel();
        panel.setLayout(new FlowLayout(FlowLayout.LEADING, 0, 0));
        panel.add(new JLabel(resources.getString("notePanel.name"), null, JLabel.LEFT));
        return panel;
    }

    private JPanel makeIPRangePanel() {
        NumberFormatter formatter = new NumberFormatter();
        formatter.setMaximum(new Integer(255));
        formatter.setMinimum(new Integer(0));

        add1 = new JFormattedTextField(formatter);
        add2 = new JFormattedTextField(formatter);
        add3 = new JFormattedTextField(formatter);
        add4 = new JFormattedTextField(formatter);
        formatter = new NumberFormatter();
        formatter.setMaximum(new Integer(32));
        formatter.setMinimum(new Integer(0));
        suffix = new JFormattedTextField(formatter);

        JPanel panel = new JPanel();
        panel.setLayout(new GridBagLayout());
        Insets insets = new Insets(0, 0, 0, CONTROL_SPACING);
        int anchor = GridBagConstraints.WEST;
        int fill = GridBagConstraints.HORIZONTAL;

        panel.add(add1, new GridBagConstraints(0,0,1,1,0.2,0,anchor,fill,insets,0,0));
        panel.add(new JLabel("."), new GridBagConstraints(1,0,1,1,0,0,anchor,fill,insets,0,0));
        panel.add(add2, new GridBagConstraints(2,0,1,1,0.2,0,anchor,fill,insets,0,0));
        panel.add(new JLabel("."), new GridBagConstraints(3,0,1,1,0,0,anchor,fill,insets,0,0));
        panel.add(add3, new GridBagConstraints(4,0,1,1,0.2,0,anchor,fill,insets,0,0));
        panel.add(new JLabel("."), new GridBagConstraints(5,0,1,1,0,0,anchor,fill,insets,0,0));
        panel.add(add4, new GridBagConstraints(6,0,1,1,0.2,0,anchor,fill,insets,0,0));
        panel.add(new JLabel("/"), new GridBagConstraints(7,0,1,1,0,0,anchor,fill,insets,0,0));
        panel.add(suffix, new GridBagConstraints(8,0,1,1,0.2,0,anchor,fill,insets,0,0));

        return panel;
    }

    private JPanel makeBottomButtonsPanel() {
        // construct buttons
        helpButton = new JButton();
        helpButton.setText(resources.getString("helpButton.name"));
        okButton = new JButton();
        okButton.setText(resources.getString("okButton.name"));
        cancelButton = new JButton();
        cancelButton.setText(resources.getString("cancelButton.name"));

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

    /**
     * for dev purposes only, to view dlg's layout
     */
    public static void main(String[] args) {
        RemoteIpRange toto = new RemoteIpRange();
        for (int i = 0; i < 3; i++) {
            RemoteIpRangePropertiesDialog me = new RemoteIpRangePropertiesDialog(null, true, toto);
            me.pack();
            me.show();
        }
        System.exit(0);
    }

    private ResourceBundle resources;

    private JButton helpButton;
    private JButton okButton;
    private JButton cancelButton;
    private boolean oked;
    private JComboBox includeExcludeCombo;
    private JFormattedTextField add1, add2, add3, add4, suffix;

    private RemoteIpRange subject;

    private final EventListenerList listenerList = new EventListenerList();

    private final static int BORDER_PADDING = 20;
    private final static int CONTROL_SPACING = 5;
}
