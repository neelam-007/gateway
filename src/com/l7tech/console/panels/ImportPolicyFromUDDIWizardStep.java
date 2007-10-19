package com.l7tech.console.panels;

import org.w3c.dom.Document;
import org.xml.sax.SAXException;

import javax.swing.*;
import javax.swing.event.ListSelectionListener;
import javax.swing.event.ListSelectionEvent;
import java.awt.*;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.Collection;
import java.net.URL;
import java.io.IOException;

import com.l7tech.common.util.XmlUtil;
import com.l7tech.common.util.SoapUtil;
import com.l7tech.common.util.TextUtils;
import com.l7tech.common.util.ExceptionUtils;
import com.l7tech.common.util.Functions;
import com.l7tech.common.uddi.UDDIException;
import com.l7tech.common.uddi.UDDINamedEntity;
import com.l7tech.common.gui.widgets.TextListCellRenderer;
import com.l7tech.policy.wsp.WspConstants;

/**
 * Second step in the ImportPolicyFromUDDIWizard.
 * <p/>
 * <p/>
 * <br/><br/>
 * LAYER 7 TECHNOLOGIES, INC<br/>
 * User: flascell<br/>
 * Date: Jun 16, 2006<br/>
 */
public class ImportPolicyFromUDDIWizardStep extends WizardStepPanel {
    private JPanel mainPanel;
    private JTextField nameField;
    private JButton searchButton;
    private JList policyList;
    private ImportPolicyFromUDDIWizard.Data data;
    private static final Logger logger = Logger.getLogger(ImportPolicyFromUDDIWizardStep.class.getName());
    private boolean readyToAdvance = false;

    public ImportPolicyFromUDDIWizardStep(WizardStepPanel next) {
        super(next);
        initialize();
    }

    public String getDescription() {
        return "Search UDDI Directory for Policy and Import it";
    }

    public String getStepLabel() {
        return "Select Policy From UDDI";
    }

    public boolean canAdvance() {
        return readyToAdvance;
    }

    public boolean canFinish() {
        return false;
    }

    public boolean onNextButton() {
        boolean res = false;
        ImportPolicyFromUDDIWizardStep.this.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
        try {
            String tModelKey = ((UDDINamedEntity)policyList.getSelectedValue()).getKey();
            res = importPolicy(tModelKey);
        } finally {
            ImportPolicyFromUDDIWizardStep.this.setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
        }

        return res;
    }

    private void initialize() {
        setLayout(new BorderLayout());
        add(mainPanel);

        policyList.setCellRenderer(new TextListCellRenderer(new TextListCellProvider(true), new TextListCellProvider(false), false));

        searchButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                ImportPolicyFromUDDIWizardStep.this.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
                try {
                    find();
                } finally {
                    ImportPolicyFromUDDIWizardStep.this.setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
                }
            }
        });
        policyList.addListSelectionListener(new ListSelectionListener() {
            public void valueChanged(ListSelectionEvent e) {
                if (policyList.getSelectedIndex() < 0) {
                    readyToAdvance = false;
                    // this causes wizard's finish or next button to become enabled (because we're now ready to continue)
                    notifyListeners();
                } else {
                    readyToAdvance = true;
                    // this causes wizard's finish or next button to become enabled (because we're now ready to continue)
                    notifyListeners();
                }
            }
        });
    }

    private void find() {
        String filter = nameField.getText();
        try {
            Collection<UDDINamedEntity> policies = data.getUddi().listPolicies(filter, null);
            policyList.setListData(policies.toArray());
        } catch (Throwable e) {
            logger.log(Level.WARNING, "Error getting find result", e);
            showError("Error getting find result . " + UDDIPolicyDetailsWizardStep.getRootCauseMsg(e));
            return;
        }
    }

    public void readSettings(Object settings) throws IllegalArgumentException {
        data = (ImportPolicyFromUDDIWizard.Data)settings;
    }

    private void showError(String err) {
        JOptionPane.showMessageDialog(this, TextUtils.breakOnMultipleLines(err, 30),
                                      "Error", JOptionPane.ERROR_MESSAGE);
    }

    private boolean importPolicy(String tModelKey) {
        boolean imported = false;
        String policyURL = null;

        try {
            policyURL = data.getUddi().getPolicyUrl(tModelKey);
        }
        catch(UDDIException iuie)  {
            logger.log(Level.WARNING, "Error list policy urls", iuie);
            showError(ExceptionUtils.getMessage(iuie));
        }

        if ( policyURL != null ) {
            // try to get a policy document
            try {
                Document doc = XmlUtil.parse(new URL(policyURL).openStream());

                if (XmlUtil.findFirstChildElementByName(doc,
                                                        new String[] {WspConstants.L7_POLICY_NS,
                                                                      SoapUtil.WSP_NAMESPACE,
                                                                      SoapUtil.WSP_NAMESPACE2},
                                                        WspConstants.POLICY_ELNAME) != null) {
                    String output;
                    try {
                        output = XmlUtil.nodeToFormattedString(doc);
                    } catch (IOException e) {
                        // cannot happen
                        throw new RuntimeException(e);
                    }
                    data.setPolicyXML(output);
                    imported = true;
                } else {
                    logger.info("xml document resolved from " + policyURL + " was not a policy: " +
                                XmlUtil.nodeToFormattedString(doc));

                    showError("Referenced document was not a policy");
                }
            } catch (IOException e) {
                logger.log(Level.WARNING, "cannot get xml document from url " + policyURL);
                showError("Error accessing reference: " + ExceptionUtils.getMessage(e));
            } catch (SAXException e) {
                logger.log(Level.WARNING, "cannot get xml document from url " + policyURL);
                showError("Error accessing reference: " + ExceptionUtils.getMessage(e));
            }
        }

        return imported;
    }

    /**
     * Function for accessing either the display or tooltip text.
     */
    private static final class TextListCellProvider implements Functions.Unary<String, Object> {
        private final boolean showName;

        private TextListCellProvider(final boolean showName) {
            this.showName = showName;    
        }

        public String call(final Object entityObject) {
            UDDINamedEntity uddiNamedEntity = (UDDINamedEntity) entityObject;
            String text;

            if ( showName ) {
                // generate the entry text
                String tModelName = uddiNamedEntity.getName();
                String tModelKey = uddiNamedEntity.getKey();
                text = tModelName + " [" + tModelKey + "]";
            } else {
                // generate the tooltip text
                text = uddiNamedEntity.getPolicyUrl();
            }

            return text;
        }
    }
}
