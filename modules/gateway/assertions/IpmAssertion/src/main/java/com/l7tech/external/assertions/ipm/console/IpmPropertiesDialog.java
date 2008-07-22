package com.l7tech.external.assertions.ipm.console;

import com.l7tech.gui.util.InputValidator;
import com.l7tech.gui.util.Utilities;
import com.l7tech.gui.widgets.SquigglyTextField;
import com.l7tech.util.ExceptionUtils;
import com.l7tech.common.io.XmlUtil;
import com.l7tech.console.panels.AssertionPropertiesEditorSupport;
import com.l7tech.external.assertions.ipm.IpmAssertion;
import org.xml.sax.SAXException;

import javax.swing.*;
import javax.swing.text.JTextComponent;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Properties dialog for {@link IpmAssertion}.
 */
public class IpmPropertiesDialog extends AssertionPropertiesEditorSupport<IpmAssertion> {
    protected static final Logger logger = Logger.getLogger(IpmPropertiesDialog.class.getName());
    private static final String TITLE = "IPM To XML Properties";

    private boolean confirmed = false;
    private JPanel mainPanel;
    private JButton okButton;
    private JButton cancelButton;
    private JTextPane templateField;
    private SquigglyTextField sourceVarField;
    private SquigglyTextField destVarField;
    private JRadioButton defaultRequestRadioButton;
    private JRadioButton defaultResponseRadioButton;
    private JRadioButton contextVariableRadioButton;

    /** @noinspection ThisEscapedInObjectConstruction*/
    private final InputValidator validator = new InputValidator(this, TITLE);

    public IpmPropertiesDialog(Frame owner) {
        super(owner, TITLE, true);
        initialize();
    }

    public IpmPropertiesDialog(Dialog owner) {
        super(owner, TITLE, true);
        initialize();
    }

    private void initialize() {
        setContentPane(mainPanel);
        getRootPane().setDefaultButton(okButton);
        Utilities.setEscKeyStrokeDisposes(this);
        Utilities.equalizeButtonSizes(new JButton[] { okButton, cancelButton });

        validator.attachToButton(okButton, new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                confirmed = true;
                dispose();
            }
        });

        cancelButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                dispose();
            }
        });


        ActionListener updateListener = new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                updateEnableState();
            }
        };
        contextVariableRadioButton.addActionListener(updateListener);
        defaultRequestRadioButton.addActionListener(updateListener);
        defaultResponseRadioButton.addActionListener(updateListener);
        Utilities.enableGrayOnDisabled(destVarField);

        validator.constrainTextFieldToBeNonEmpty("Source Variable Name", sourceVarField, null);
        validator.constrainTextFieldToBeNonEmpty("Destination Variable Name", destVarField, null);
        validator.constrainTextFieldToBeNonEmpty("Template XML", templateField, new InputValidator.ComponentValidationRule(templateField) {
            public String getValidationError() {
                try {
                    XmlUtil.stringToDocument(templateField.getText());
                    return null;
                } catch (SAXException e) {
                    return "The template is not well-formed XML: " + ExceptionUtils.getMessage(e);
                }
            }
        });

        Utilities.attachDefaultContextMenu(sourceVarField);
        Utilities.attachDefaultContextMenu(destVarField);
        templateField.setFont(new JTextArea().getFont());
        templateField.addMouseListener(Utilities.createContextMenuMouseListener(templateField, new Utilities.DefaultContextMenuFactory() {
            @Override
            public JPopupMenu createContextMenu(final JTextComponent tc) {
                final JPopupMenu m = super.createContextMenu(tc);
                if (tc.isEditable()) {
                    m.add(new JSeparator());
                    JMenuItem reformXml = new JMenuItem("Reformat All XML");
                    reformXml.addActionListener(new ActionListener() {
                        public void actionPerformed(ActionEvent e) {
                            String xml = tc.getText();
                            try {
                                tc.setText(XmlUtil.nodeToFormattedString(XmlUtil.stringToDocument(xml)));
                            } catch (IOException e1) {
                                logger.log(Level.SEVERE, "Unable to reformat XML", e1); // Can't happen
                            } catch (SAXException e1) {
                                logger.log(Level.INFO, "Unable to reformat XML", e1); // Oh well
                            }
                        }
                    });
                    m.add(reformXml);
                }
                return m;
            }
        }));
    }

    public boolean isConfirmed() {
        return confirmed;
    }

    private void updateEnableState() {
        destVarField.setEnabled(contextVariableRadioButton.isSelected());
    }

    public void setData(IpmAssertion assertion) {
        templateField.setText(assertion.template());
        sourceVarField.setText(assertion.getSourceVariableName());
        final String targetVar = assertion.getTargetVariableName();
        if (targetVar == null) {
            if (assertion.isUseResponse()) {
                defaultResponseRadioButton.setSelected(true);
            } else {
                defaultRequestRadioButton.setSelected(true);
            }
            destVarField.setText("");
        } else {
            contextVariableRadioButton.setSelected(true);
            destVarField.setText(targetVar);
        }
        updateEnableState();
    }

    public IpmAssertion getData(IpmAssertion assertion) {
        assertion.template(templateField.getText());
        assertion.setSourceVariableName(sourceVarField.getText());
        if (contextVariableRadioButton.isSelected()) {
            assertion.setTargetVariableName(destVarField.getText());
        } else {
            assertion.setTargetVariableName(null);
            assertion.setUseResponse(defaultResponseRadioButton.isSelected());
        }
        return assertion;
    }

    public static void main(String[] args) {
        JDialog dlg = new IpmPropertiesDialog((JFrame)null);
        dlg.pack();
        dlg.setVisible(true);
        System.exit(0);
    }
}
