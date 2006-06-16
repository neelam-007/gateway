package com.l7tech.console.panels;

import org.systinet.uddi.client.v3.struct.*;
import org.systinet.uddi.client.v3.*;
import org.systinet.uddi.InvalidParameterException;

import javax.swing.*;
import javax.xml.soap.SOAPException;
import java.awt.*;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import java.awt.event.KeyListener;
import java.awt.event.KeyEvent;
import java.util.logging.Logger;
import java.util.logging.Level;
import java.net.URL;
import java.net.MalformedURLException;

/**
 * Wizard step in the PublishPolicyToUDDIWizard wizard pertaining
 * to describing the policy to publish a reference to.
 * <p/>
 * <p/>
 * <br/><br/>
 * LAYER 7 TECHNOLOGIES, INC<br/>
 * User: flascell<br/>
 * Date: Jun 12, 2006<br/>
 */
public class UDDIPolicyDetailsWizardStep extends WizardStepPanel {
    private static final Logger logger = Logger.getLogger(UDDIPolicyDetailsWizardStep.class.getName());
    private JPanel mainPanel;
    private JTextField policyNameField;
    private JTextField policyDescField;
    private JTextField policyURLField;
    private final String policyURL;
    private final String serviceName;
    private boolean done = false;
    private JButton registerButton;
    private PublishPolicyToUDDIWizard.Data data;

    public UDDIPolicyDetailsWizardStep(WizardStepPanel next, String policyURL, String serviceName) {
        super(next);
        this.policyURL = policyURL;
        this.serviceName = serviceName;
        initialize();
    }

    public String getDescription() {
        return "Provide UDDI details for this policy and create a tModel for it";
    }

    public String getStepLabel() {
        return "Policy Details";
    }

    public boolean canAdvance() {
        return done;
    }

    public boolean canFinish() {
        return done;
    }

    private void initialize() {
        setLayout(new BorderLayout());
        add(mainPanel);
        policyNameField.setText(serviceName);

        KeyListener validValuesPolice = new KeyListener() {
            public void keyTyped(KeyEvent e) {
                registerButton.setEnabled(validateValues(true));
            }
            public void keyPressed(KeyEvent e) {}
            public void keyReleased(KeyEvent e) {}
        };
        policyNameField.addKeyListener(validValuesPolice);
        policyURLField.setText(policyURL);
        policyURLField.addKeyListener(validValuesPolice);
        policyDescField.setText("A policy for service " + serviceName);
        policyDescField.addKeyListener(validValuesPolice);
        registerButton.addActionListener(new ActionListener(){
            public void actionPerformed(ActionEvent e) {
                if (validateValues(false)) {
                    SwingUtilities.invokeLater(new Runnable() {
                        public void run() {
                            UDDIPolicyDetailsWizardStep.this.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
                            publishPolicyReferenceToSystinet65Directory();
                            UDDIPolicyDetailsWizardStep.this.setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
                        }
                    });
                }
            }
        });
    }

    public boolean validateValues(boolean silent) {
        // make sure values are legal
        String tmp = policyNameField.getText();
        if (tmp == null || tmp.length() < 1) {
            if (!silent) showError("Policy name cannot be empty");
            return false;
        }
        tmp = policyDescField.getText();
        if (tmp == null || tmp.length() < 1) {
            if (!silent) showError("Policy description cannot be empty");
            return false;
        }
        tmp = policyURLField.getText();
        if (tmp == null || tmp.length() < 1) {
            if (!silent) showError("Policy URL cannot be empty");
            return false;
        } else {
            try {
                new URL(tmp);
            } catch (MalformedURLException e) {
                if (!silent) showError(tmp + " is not a valid URL");
                return false;
            }
        }
        return true;
    }

    public void storeSettings(Object settings) throws IllegalArgumentException {
        PublishPolicyToUDDIWizard.Data data = (PublishPolicyToUDDIWizard.Data)settings;
        data.setPolicyName(policyNameField.getText());
        data.setPolicyDescription(policyURLField.getText());
        data.setCapturedPolicyURL(policyURLField.getText());
    }

    private void showError(String err) {
        JOptionPane.showMessageDialog(this, breakOnMultipleLines(err, 30), "Error", JOptionPane.ERROR_MESSAGE);
    }

    private void publishPolicyReferenceToSystinet65Directory() {
        // create a tmodel to save
        TModel tmodel = new TModel();
        try {
            CategoryBag cbag = new CategoryBag();
            cbag.addKeyedReference(new KeyedReference("uddi:schemas.xmlsoap.org:policytypes:2003_03", "policy", "policy"));
            cbag.addKeyedReference(new KeyedReference("uddi:schemas.xmlsoap.org:remotepolicyreference:2003_03",
                                                      policyURLField.getText(),
                                                      "policy reference"
                                                      ));
            tmodel.setCategoryBag(cbag);
            tmodel.setName(new Name(policyNameField.getText()));
            DescriptionArrayList dal = new DescriptionArrayList();
            dal.add(new Description(policyURLField.getText()));
            OverviewDocArrayList odal = new OverviewDocArrayList();
            OverviewDoc odoc = new OverviewDoc();
            odoc.setOverviewURL(new OverviewURL(policyURLField.getText()));
            odal.add(odoc);
            tmodel.setOverviewDocArrayList(odal);
            tmodel.setDescriptionArrayList(dal);
        } catch (InvalidParameterException e) {
            logger.log(Level.WARNING, "cannot construct tmodel to save", e);
            showError("Error constructing tmodel: " + e.getMessage());
            return;
        }
        // setup stuff needed to save it
        String registryURL = data.getUddiurl();
        /*
        if (registryURL.indexOf("/uddi") < 1) {
            if (registryURL.endsWith("/")) {
                registryURL = registryURL + "uddi/";
            } else {
                registryURL = registryURL + "/uddi/";
            }
        }
        if (!registryURL.endsWith("/")) {
            registryURL = registryURL + "/";
        }
        // remember the url once it's been 'normalized'
        data.setUddiurl(registryURL);
        */
        String authInfo;
        try {
            UDDI_Security_PortType security = UDDISecurityStub.getInstance(registryURL + "security");
            authInfo = security.get_authToken(new Get_authToken(data.getAccountName(), data.getAccountPasswd())).getAuthInfo();
        } catch (SOAPException e) {
            logger.log(Level.WARNING, "cannot get security token from " + registryURL + "security", e);
            showError("ERROR cannot get security token from " + registryURL + "security. " + e.getMessage());
            return;
        } catch (UDDIException e) {
            logger.log(Level.WARNING, "cannot get security token from " + registryURL + "security", e);
            showError("ERROR cannot get security token from " + registryURL + "security. " + e.getMessage());
            return;
        } catch (InvalidParameterException e) {
            logger.log(Level.WARNING, "cannot get security token from " + registryURL + "security", e);
            showError("ERROR cannot get security token from " + registryURL + "security. " + e.getMessage());
            return;
        } catch (Throwable e) {
            logger.log(Level.WARNING, "cannot get security token from " + registryURL + "security", e);
            showError("ERROR cannot get security token from " + registryURL + "security. " + getRootCauseMsg(e));
            return;
        }
        Save_tModel stm = new Save_tModel();
        stm.setAuthInfo(authInfo);
        data.setAuthInfo(authInfo);
        try {
            TModelArrayList tmal = new TModelArrayList();
            tmal.add(tmodel);
            stm.setTModelArrayList(tmal);
            UDDI_Publication_PortType publishing = UDDIPublishStub.getInstance(registryURL + "publishing");
            TModelDetail tModelDetail = publishing.save_tModel(stm);
            TModel saved = tModelDetail.getTModelArrayList().get(0);
            String msg = "Publication successful, policy tModel key: " + saved.getTModelKey() +
                         " choose 'Next' below to associate this policy tModel to " +
                         "a business service or 'Finish' to end.";
            JOptionPane.showConfirmDialog(this, breakOnMultipleLines(msg, 30), "Success", JOptionPane.DEFAULT_OPTION);
            data.setPolicytModelKey(saved.getTModelKey());
            done = true;
            // this causes wizard's finish or next button to become enabled (because we're now ready to continue)
            notifyListeners();
            registerButton.setEnabled(false);
        } catch (SOAPException e) {
            logger.log(Level.WARNING, "cannot save tModel at " + registryURL + "publishing", e);
            showError("ERROR cannot save tModel at " + registryURL + "publishing. " + e.getMessage());
        } catch (UDDIException e) {
            logger.log(Level.WARNING, "cannot save tModel at " + registryURL + "publishing", e);
            showError("ERROR cannot save tModel at " + registryURL + "publishing. " + e.getMessage());
        } catch (InvalidParameterException e) {
            logger.log(Level.WARNING, "cannot save tModel at " + registryURL + "publishing", e);
            showError("ERROR cannot save tModel at " + registryURL + "publishing. " + e.getMessage());
        }  catch (Throwable e) {
            logger.log(Level.WARNING, "cannot save tModel at " + registryURL + "publishing", e);
            showError("ERROR cannot save tModel at " + registryURL + "publishing. " + getRootCauseMsg(e));
        }
    }

    public static String getRootCauseMsg(Throwable e) {
        Throwable root = e;
        while (root.getCause() != null) root = root.getCause();
        return root.getMessage();
    }

    /**
     * Breaks input stream on a multiple lines breaking at space characters
     * to form lines of maximum length passed.
     */
    public static String breakOnMultipleLines(String input, int maxlinelength) {
        input = input.trim();
        if (input.length() <= maxlinelength) return input;
        StringBuffer output = new StringBuffer();
        int pos = 0;
        while ((input.length() - pos) > maxlinelength) {
            int tmp = input.indexOf(' ', pos);
            if (tmp < 0) break;
            int lastspace = tmp;
            while (true) if ((lastspace - pos) > maxlinelength) {
                output.append(input.substring(pos, lastspace)).append("\n");
                pos = lastspace + 1;
                break;
            } else if (tmp < 0 && lastspace > 0) {
                output.append(input.substring(pos, lastspace)).append("\n");
                pos = lastspace + 1;
                break;
            } else if ((tmp - pos) == maxlinelength) {
                output.append(input.substring(pos, tmp)).append("\n");
                pos = tmp + 1;
                break;
            } else if ((tmp - pos) > maxlinelength && (lastspace - pos) < maxlinelength) {
                output.append(input.substring(pos, lastspace)).append("\n");
                pos = lastspace + 1;
                break;
            } else {
                lastspace = tmp;
                tmp = input.indexOf(' ', tmp + 1);
            }
        }
        output.append(input.substring(pos));
        return output.toString();
    }

    public void readSettings(Object settings) throws IllegalArgumentException {
        data = (PublishPolicyToUDDIWizard.Data)settings;
    }
}
