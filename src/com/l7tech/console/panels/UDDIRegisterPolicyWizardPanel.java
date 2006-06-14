package com.l7tech.console.panels;

import org.systinet.uddi.client.v3.struct.*;
import org.systinet.uddi.client.v3.*;
import org.systinet.uddi.InvalidParameterException;

import javax.swing.*;
import javax.xml.soap.SOAPException;
import java.awt.*;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import java.util.logging.Logger;
import java.util.logging.Level;

/**
 * Wizard step in the PublishPolicyToUDDIWizard wizard that
 * completes the Policy publication process.
 * <p/>
 * <p/>
 * <br/><br/>
 * LAYER 7 TECHNOLOGIES, INC<br/>
 * User: flascell<br/>
 * Date: Jun 12, 2006<br/>
 */
public class UDDIRegisterPolicyWizardPanel extends WizardStepPanel {
    private JPanel mainPanel;
    private JTextPane progressLabel;
    private PublishPolicyToUDDIWizard.Data data;
    private JButton registerButton;
    private static final Logger logger = Logger.getLogger(UDDIRegisterPolicyWizardPanel.class.getName());
    private boolean done = false;

    public UDDIRegisterPolicyWizardPanel(WizardStepPanel next) {
        super(next);
        initialize();
    }

    public String getDescription() {
        return "Complete policy publication";
    }

    public String getStepLabel() {
        return "Complete policy publication";
    }

    private void initialize() {
        setLayout(new BorderLayout());
        add(mainPanel);
        registerButton.addActionListener(new ActionListener(){
            public void actionPerformed(ActionEvent e) {
                SwingUtilities.invokeLater(new Runnable() {
                    public void run() {
                        UDDIRegisterPolicyWizardPanel.this.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
                        publishPolicyReferenceToSystinet65Directory();
                        UDDIRegisterPolicyWizardPanel.this.setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
                    }
                });
            }
        });
    }

    public boolean canAdvance() {
        return done;
    }

    public boolean canFinish() {
        return done;
    }

    public boolean onNextButton() {
        return done;
    }

    private void publishPolicyReferenceToSystinet65Directory() {
        // create a tmodel to save
        setProgress("Constructing tModel to save...", false);
        TModel tmodel = new TModel();
        try {
            CategoryBag cbag = new CategoryBag();
            cbag.addKeyedReference(new KeyedReference("uddi:schemas.xmlsoap.org:policytypes:2003_03", "policy", "policy"));
            cbag.addKeyedReference(new KeyedReference("uddi:schemas.xmlsoap.org:remotepolicyreference:2003_03",
                                                      data.getCapturedPolicyURL(),
                                                      "policy reference"
                                                      ));
            tmodel.setCategoryBag(cbag);
            tmodel.setName(new Name(data.getPolicyName()));
            DescriptionArrayList dal = new DescriptionArrayList();
            dal.add(new Description(data.getPolicyDescription()));
            OverviewDocArrayList odal = new OverviewDocArrayList();
            OverviewDoc odoc = new OverviewDoc();
            odoc.setOverviewURL(new OverviewURL(data.getCapturedPolicyURL()));
            odal.add(odoc);
            tmodel.setOverviewDocArrayList(odal);
            tmodel.setDescriptionArrayList(dal);
        } catch (InvalidParameterException e) {
            logger.log(Level.WARNING, "cannot construct tmodel to save", e);
            setProgress("Error constructing tmodel: " + e.getMessage(), true);
            return;
        }
        // setup stuff needed to save it
        String registryURL = data.getUddiurl();
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
        setProgress("Getting authentication token...", false);
        String authInfo;
        try {
            UDDI_Security_PortType security = UDDISecurityStub.getInstance(registryURL + "security");
            authInfo = security.get_authToken(new Get_authToken(data.getAccountName(), data.getAccountPasswd())).getAuthInfo();
        } catch (SOAPException e) {
            logger.log(Level.WARNING, "cannot get security token from " + registryURL + "security", e);
            setProgress("ERROR cannot get security token from " + registryURL + "security. " + e.getMessage(), true);
            return;
        } catch (UDDIException e) {
            logger.log(Level.WARNING, "cannot get security token from " + registryURL + "security", e);
            setProgress("ERROR cannot get security token from " + registryURL + "security. " + e.getMessage(), true);
            return;
        } catch (InvalidParameterException e) {
            logger.log(Level.WARNING, "cannot get security token from " + registryURL + "security", e);
            setProgress("ERROR cannot get security token from " + registryURL + "security. " + e.getMessage(), true);
            return;
        } catch (Throwable e) {
            logger.log(Level.WARNING, "cannot get security token from " + registryURL + "security", e);
            setProgress("ERROR cannot get security token from " + registryURL + "security. " + getRootCauseMsg(e), true);
            return;
        }
        Save_tModel stm = new Save_tModel();
        stm.setAuthInfo(authInfo);
        data.setAuthInfo(authInfo);
        setProgress("Saving policy tModel...", false);
        try {
            TModelArrayList tmal = new TModelArrayList();
            tmal.add(tmodel);
            stm.setTModelArrayList(tmal);
            UDDI_Publication_PortType publishing = UDDIPublishStub.getInstance(registryURL + "publishing");
            TModelDetail tModelDetail = publishing.save_tModel(stm);
            TModel saved = tModelDetail.getTModelArrayList().get(0);
            String msg = "Publication successful, policy tModel key: " + saved.getTModelKey() +
                         "\nclick 'Next' below to associate this policy tModel to\n" +
                         "a business service or 'Finish' to end.";
            setProgress(msg, false);
            data.setPolicytModelKey(saved.getTModelKey());
            done = true;
            // this causes wizard's finish or next button to become enabled (because we're now ready to continue)
            notifyListeners();
            registerButton.setEnabled(false);
        } catch (SOAPException e) {
            logger.log(Level.WARNING, "cannot save tModel at " + registryURL + "publishing", e);
            setProgress("ERROR cannot save tModel at " + registryURL + "publishing. " + e.getMessage(), true);
        } catch (UDDIException e) {
            logger.log(Level.WARNING, "cannot save tModel at " + registryURL + "publishing", e);
            setProgress("ERROR cannot save tModel at " + registryURL + "publishing. " + e.getMessage(), true);
        } catch (InvalidParameterException e) {
            logger.log(Level.WARNING, "cannot save tModel at " + registryURL + "publishing", e);
            setProgress("ERROR cannot save tModel at " + registryURL + "publishing. " + e.getMessage(), true);
        }  catch (Throwable e) {
            logger.log(Level.WARNING, "cannot save tModel at " + registryURL + "publishing", e);
            setProgress("ERROR cannot save tModel at " + registryURL + "publishing. " + getRootCauseMsg(e), true);
        }
    }

    private void setProgress(String msg, boolean error) {
        // todo, this foreground does not do anything with this control. maybe use html tags instead
        if (error) {
            progressLabel.setForeground(Color.RED);
        } else {
            progressLabel.setForeground(Color.BLACK);
        }
        progressLabel.setText(msg);
    }

    private String getRootCauseMsg(Throwable e) {
        Throwable root = e;
        while (root.getCause() != null) root = root.getCause();
        return root.getMessage();
    }

    public void readSettings(Object settings) throws IllegalArgumentException {
        data = (PublishPolicyToUDDIWizard.Data)settings;
    }
}
