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
    private JLabel progressLabel;
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
                        publishPolicyReferenceToSystinet65Directory();
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
        return canFinish();
    }

    private void publishPolicyReferenceToSystinet65Directory() {
        // create a tmodel to save
        TModel tmodel = new TModel();
        try {
            CategoryBag cbag = new CategoryBag();
            cbag.addKeyedReference(new KeyedReference("uddi:schemas.xmlsoap.org:policytypes:2003_03", "policy", "policy"));
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
            progressLabel.setText("Error constructing tmodel: " + e.getMessage());
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
        String authInfo = null;
        try {
            UDDI_Security_PortType security = UDDISecurityStub.getInstance(registryURL + "security");
            authInfo = security.get_authToken(new Get_authToken(data.getAccountName(), data.getAccountPasswd())).getAuthInfo();
        } catch (SOAPException e) {
            logger.log(Level.WARNING, "cannot get security token from " + registryURL + "security", e);
            progressLabel.setText("ERROR cannot get security token from " + registryURL + "security. " + e.getMessage());
            return;
        } catch (UDDIException e) {
            logger.log(Level.WARNING, "cannot get security token from " + registryURL + "security", e);
            progressLabel.setText("ERROR cannot get security token from " + registryURL + "security. " + e.getMessage());
            return;
        } catch (InvalidParameterException e) {
            logger.log(Level.WARNING, "cannot get security token from " + registryURL + "security", e);
            progressLabel.setText("ERROR cannot get security token from " + registryURL + "security. " + e.getMessage());
            return;
        } catch (Throwable e) {
            logger.log(Level.WARNING, "cannot get security token from " + registryURL + "security", e);
            progressLabel.setText("ERROR cannot get security token from " + registryURL + "security. " + getRootCauseMsg(e));
            return;
        }
        Save_tModel stm = new Save_tModel();
        stm.setAuthInfo(authInfo);

        try {
            TModelArrayList tmal = new TModelArrayList();
            tmal.add(tmodel);
            stm.setTModelArrayList(tmal);
            UDDI_Publication_PortType publishing = UDDIPublishStub.getInstance(registryURL + "publishing");
            TModelDetail tModelDetail = publishing.save_tModel(stm);
            TModel saved = tModelDetail.getTModelArrayList().get(0);
            progressLabel.setText("Publication successful. tModel key: " + saved.getTModelKey());
            done = true;
        } catch (SOAPException e) {
            logger.log(Level.WARNING, "cannot save token at " + registryURL + "publishing", e);
            progressLabel.setText("ERROR cannot save token at " + registryURL + "publishing. " + e.getMessage());
            return;
        } catch (UDDIException e) {
            logger.log(Level.WARNING, "cannot save token at " + registryURL + "publishing", e);
            progressLabel.setText("ERROR cannot save token at " + registryURL + "publishing. " + e.getMessage());
            return;
        } catch (InvalidParameterException e) {
            logger.log(Level.WARNING, "cannot save token at " + registryURL + "publishing", e);
            progressLabel.setText("ERROR cannot save token at " + registryURL + "publishing. " + e.getMessage());
            return;
        }  catch (Throwable e) {
            logger.log(Level.WARNING, "cannot save token at " + registryURL + "publishing", e);
            progressLabel.setText("ERROR cannot save token at " + registryURL + "publishing. " + getRootCauseMsg(e));
            return;
        }
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
