package com.l7tech.external.assertions.script.console;

import com.l7tech.common.gui.util.Utilities;
import com.l7tech.common.gui.util.DialogDisplayer;
import com.l7tech.console.panels.AssertionPropertiesEditor;
import com.l7tech.external.assertions.script.ScriptAssertion;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

/**
 * Properties for ScriptAssertion.
 */
public class ScriptPropertiesDialog extends JDialog implements AssertionPropertiesEditor<ScriptAssertion>, ActionListener {
    private JPanel topPanel;
    private JButton okButton;
    private JButton cancelButton;
    private JTextPane scriptSource;
    private JComboBox languageComboBox;

    private boolean confirmed = false;

    public ScriptPropertiesDialog(Frame owner, ScriptAssertion rla) throws HeadlessException {
        super(owner, true);
        initialize(rla);
    }

    public ScriptPropertiesDialog(Dialog owner, ScriptAssertion rla) throws HeadlessException {
        super(owner, true);
        initialize(rla);
    }

    private void initialize(ScriptAssertion rla) {
        setTitle("Custom Script Properties");
        setContentPane(topPanel);

        okButton.addActionListener(this);
        cancelButton.addActionListener(this);

        languageComboBox.setModel(new DefaultComboBoxModel(ScriptAssertion.Language.values()));
        languageComboBox.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                ScriptAssertion.Language language = (ScriptAssertion.Language)languageComboBox.getSelectedItem();
                if (language != null && scriptSource.getText().length() < 1) {
                    scriptSource.setText(language.getSampleScript());
                }
            }
        });

        Utilities.setEscKeyStrokeDisposes(this);
        getRootPane().setDefaultButton(okButton);
        Utilities.equalizeButtonSizes(new JButton[]{okButton, cancelButton});
        pack();
        if (rla != null) setData(rla);
    }

    public void actionPerformed(ActionEvent e) {
        if (e.getActionCommand().equals(okButton.getActionCommand())) {
            if (!checkValidity())
                return;
            confirmed = true;
        }
        dispose();
    }

    private boolean checkValidity() {
        String err = null;

        if (scriptSource.getText().trim().length() < 1)
            err = "Script is empty or all whitespace";

        if (languageComboBox.getSelectedItem() == null)
            err = "No language selected";

        if (err != null)
            DialogDisplayer.showMessageDialog(this, err, "Error", JOptionPane.ERROR_MESSAGE, null);

        return null == err;
    }

    public void setData(ScriptAssertion rla) {
        languageComboBox.setSelectedItem(rla.getLanguage());
        scriptSource.setText(rla.decodeScript());
    }

    public ScriptAssertion getData(ScriptAssertion rla) {
        ScriptAssertion.Language language = (ScriptAssertion.Language)languageComboBox.getSelectedItem();
        if (language == null) language = ScriptAssertion.Language.JAVASCRIPT;
        rla.setLanguage(language);
        rla.encodeScript(scriptSource.getText());
        return rla;
    }

    public JDialog getDialog() {
        return this;
    }

    /**
     * @return true if the dialog was dismissed by the user pressing the Ok button.
     */
    public boolean isConfirmed() {
        return confirmed;
    }
}
