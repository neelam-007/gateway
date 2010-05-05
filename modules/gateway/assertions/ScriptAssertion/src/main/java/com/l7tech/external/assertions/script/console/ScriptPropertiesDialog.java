package com.l7tech.external.assertions.script.console;

import com.l7tech.console.panels.AssertionPropertiesEditorSupport;
import com.l7tech.console.util.Registry;
import com.l7tech.external.assertions.script.ScriptAssertion;
import com.l7tech.gateway.common.custom.CustomAssertionsRegistrar;
import com.l7tech.gui.util.DialogDisplayer;
import com.l7tech.gui.util.FileChooserUtil;
import com.l7tech.gui.util.Utilities;
import com.l7tech.util.ExceptionUtils;
import com.l7tech.util.FileUtils;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Properties for ScriptAssertion.
 */
public class ScriptPropertiesDialog extends AssertionPropertiesEditorSupport<ScriptAssertion> implements ActionListener {
    private static final Logger logger = Logger.getLogger(ScriptPropertiesDialog.class.getName());

    private JPanel topPanel;
    private JButton okButton;
    private JButton cancelButton;
    private JTextArea scriptSource;
    private JComboBox languageComboBox;
    private JButton compileButton;

    private boolean confirmed = false;

    public ScriptPropertiesDialog(Window owner, ScriptAssertion rla) throws HeadlessException {
        super(owner, "Custom Script Properties");
        initialize(rla);
    }

    private void initialize(ScriptAssertion rla) {
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

        compileButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                onCompileButton();
            }
        });

        Utilities.attachDefaultContextMenu(scriptSource);
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

    /**
     * @return true if the dialog was dismissed by the user pressing the Ok button.
     */
    public boolean isConfirmed() {
        return confirmed;
    }

    @Override
    protected void configureView() {
        okButton.setEnabled( !isReadOnly() );
    }

    private void onCompileButton() {
        if (!checkValidity())
            return;
        DialogDisplayer.showInputDialog(this, "Enter assertion name without Assertion suffix, eg \"EnhancedLeafRaker\"", "Enter Assertion Name", JOptionPane.QUESTION_MESSAGE, null, null, null, new DialogDisplayer.InputListener() {
            @Override
            public void reportResult(Object option) {
                if (option == null)
                    return;

                String assCamelName = option.toString();
                if (!assCamelName.matches("^[a-zA-Z][a-zA-Z0-9]{0,30}$")) {
                    DialogDisplayer.showMessageDialog(ScriptPropertiesDialog.this, "The name may be up to 30 letters and numbers with no spaces or special characters.", "Invalid name", JOptionPane.ERROR_MESSAGE, null);
                    return;
                }

                try {
                    byte[] bsfjarBytes = getResource("AAR-INF/lib/bsf-2.4.0-l7p1.jar");
                    byte[] jsJarBytes = getResource("AAR-INF/lib/js-1.6R7.jar");
                    byte[] ssaSupportClassBytes = getResource("com/l7tech/external/assertions/script/server/ServerScriptAssertionSupport.class");
                    byte[] ssaSupportCssaClassBytes = getResource("com/l7tech/external/assertions/script/server/ServerScriptAssertionSupport$CompiledScriptServerAssertion.class");

                    byte[] aarBytes = CompiledScriptAarFileGenerator.generateCompiledScriptAarFile(assCamelName, "javascript", scriptSource.getText().trim(),
                            bsfjarBytes,
                            jsJarBytes,
                            ssaSupportClassBytes,
                            ssaSupportCssaClassBytes);

                    FileChooserUtil.saveSingleFileWithOverwriteConfirmation(ScriptPropertiesDialog.this,
                            "Save .AAR File", FileChooserUtil.buildFilter(".aar", "(*.aar) Modular Assertion Archive"), ".aar",
                            new FileUtils.ByteSaver(aarBytes));

                } catch (IOException e) {
                    final String msg = "Unable to compile script: " + ExceptionUtils.getMessage(e);
                    logger.log(Level.WARNING, msg, e);
                    DialogDisplayer.showMessageDialog(ScriptPropertiesDialog.this, msg, "Error", JOptionPane.ERROR_MESSAGE, null);
                }
            }
        });
    }

    private byte[] getResource(String resource) throws IOException {
        CustomAssertionsRegistrar creg = Registry.getDefault().getCustomAssertionsRegistrar();
        byte[] got = creg.getAssertionResourceBytes(resource);
        if (got == null)
            throw new IOException("Resource not found on Gateway: " + resource);
        return got;
    }

}
