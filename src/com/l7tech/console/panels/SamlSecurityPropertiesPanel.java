package com.l7tech.console.panels;

import com.l7tech.policy.assertion.xmlsec.SamlSecurity;
import com.l7tech.policy.assertion.Assertion;
import com.l7tech.policy.AssertionPath;
import com.l7tech.console.event.PolicyListener;
import com.l7tech.console.event.PolicyEvent;

import javax.swing.*;
import javax.swing.event.EventListenerList;
import java.awt.*;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import java.awt.event.KeyListener;
import java.awt.event.KeyEvent;
import java.util.EventListener;

/**
 * Dialog to view/edit the properties of a SamlSecurity assertion.
 * <p/>
 * <br/><br/>
 * LAYER 7 TECHNOLOGIES, INC<br/>
 * <p/>
 * User: flascell<br/>
 * Date: Nov 26, 2004<br/>
 */
public class SamlSecurityPropertiesPanel extends JDialog {
    private JButton okbutton;
    private JButton cancelbutton;
    private JButton helpbutton;
    private JComboBox confirmationMethodComboBox;
    private JPanel mainPanel;
    private SamlSecurity subject;
    private final EventListenerList listenerList = new EventListenerList();

    public SamlSecurityPropertiesPanel(Frame owner, SamlSecurity assertion) {
        super(owner, true);
        subject = assertion;
        initialize();
    }

    private void initialize() {
        setContentPane(mainPanel);
        setTitle("Saml Security Properties");

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
                help();
            }
        });

        confirmationMethodComboBox.setModel(new DefaultComboBoxModel(new String[]{"Holder-of-key",
                                                                                  "Sender-vouches",
                                                                                  "Any"}));
        confirmationMethodComboBox.setSelectedIndex(subject.getConfirmationMethodType());

        // implement default behavior for esc and enter keys
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

        okbutton.addKeyListener(defBehaviorKeyListener);
        cancelbutton.addKeyListener(defBehaviorKeyListener);
        helpbutton.addKeyListener(defBehaviorKeyListener);
        confirmationMethodComboBox.addKeyListener(defBehaviorKeyListener);
    }

    private void ok() {
        subject.setConfirmationMethodType(confirmationMethodComboBox.getSelectedIndex());
        fireEventAssertionChanged(subject);
        SamlSecurityPropertiesPanel.this.dispose();
    }

    private void help() {
        // todo
    }

    private void cancel() {
        SamlSecurityPropertiesPanel.this.dispose();
    }

    public void addPolicyListener(PolicyListener listener) {
        listenerList.add(PolicyListener.class, listener);
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

    public static void main(String[] args) {
        SamlSecurity assertion = new SamlSecurity();
        assertion.setConfirmationMethodType(SamlSecurity.CONFIRMATION_METHOD_SENDER_VOUCHES);
        SamlSecurityPropertiesPanel me = new SamlSecurityPropertiesPanel(null, assertion);
        me.pack();
        me.show();
        System.exit(0);
    }
}
