package com.l7tech.console.panels;

import com.l7tech.console.action.SecureAction;
import com.l7tech.console.event.CertEvent;
import com.l7tech.console.event.CertListener;
import com.l7tech.console.event.WizardAdapter;
import com.l7tech.console.event.WizardEvent;
import com.l7tech.console.util.Registry;
import com.l7tech.gateway.common.security.TrustedCertAdmin;
import com.l7tech.gateway.common.security.rbac.AttemptedCreate;
import com.l7tech.gui.util.DialogDisplayer;
import com.l7tech.gui.util.Utilities;
import com.l7tech.objectmodel.DuplicateObjectException;
import com.l7tech.objectmodel.ObjectModelException;
import com.l7tech.objectmodel.SaveException;
import com.l7tech.objectmodel.VersionException;
import com.l7tech.security.cert.TrustedCert;
import com.l7tech.util.ExceptionUtils;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.security.cert.CertificateExpiredException;
import java.security.cert.CertificateNotYetValidException;
import java.util.Locale;
import java.util.ResourceBundle;

import static com.l7tech.objectmodel.EntityType.TRUSTED_CERT;

/**
 * Action for addition of trusted certificates.
 *
 * @author Steve Jones
 */
public class NewTrustedCertificateAction extends SecureAction {

    //- PUBLIC

    /**
     * Create a listener with the given (perhaps internationalized) name.
     *
     * @param listener The listener for certificate events
     * @param name The name for the action
     */
    public NewTrustedCertificateAction(CertListener listener, String name) {
        super(new AttemptedCreate(TRUSTED_CERT));
        this.listener = listener;
        this.name = name;
        putValue(Action.NAME, name);
    }

    /**
     * Get the name for this action.
     *
     * @return The name
     */
    public String getName() {
        return name;
    }

    //- PROTECTED

    /**
     *
     */
    protected void performAction() {
    }

    /**
     * Implement performAction, use the source components containing Window as parent. 
     */
    protected void performAction(final ActionEvent actionEvent) {
        Object source = actionEvent.getSource();

        final Window parent;
        if (source instanceof Component) {
            parent = SwingUtilities.getWindowAncestor((Component) source);
        } else {
            parent = null;
        }

        SwingUtilities.invokeLater(new Runnable() {
            public void run() {

                CertImportMethodsPanel importPanel = new CertImportMethodsPanel(new CertDetailsPanel(new CertUsagePanel(new CertValidationPanel(null))), true);
                Wizard w = parent instanceof Frame ?
                        new AddCertificateWizard((Frame) parent, importPanel) :
                        new AddCertificateWizard((Dialog) parent, importPanel);
                w.addWizardListener(new NewTrustedCertificateWizardListener(parent, listener));

                w.pack();
                Utilities.centerOnParentWindow(w);
                DialogDisplayer.display(w);
            }
        });
    }

    //- PRIVATE

    private static final ResourceBundle resources = ResourceBundle.getBundle("com.l7tech.console.resources.CertificateDialog", Locale.getDefault());

    private final CertListener listener;
    private final String name;

    /**
     * The callback for saving the new cert to the database
     */
    private static class NewTrustedCertificateWizardListener extends WizardAdapter {
        private final Component source;
        private final CertListener listener;

        private NewTrustedCertificateWizardListener(Component source, CertListener listener) {
            this.source = source;
            this.listener = listener;
        }

        /**
         * Retrieve the object reference of the Trusted Cert Admin service
         *
         * @return TrustedCertAdmin  - The object reference.
         * @throws RuntimeException if the object reference of the Trusted Cert Admin service is not found.
         */
        private TrustedCertAdmin getTrustedCertAdmin() throws RuntimeException {
            return Registry.getDefault().getTrustedCertManager();
        }

        /**
         * Invoked when the wizard has finished.
         *
         * @param we the event describing the wizard finish
         */
        public void wizardFinished(WizardEvent we) {
            // update the provider
            Wizard w = (Wizard) we.getSource();
            Object o = w.getWizardInput();

            if (o instanceof TrustedCert) {

                final TrustedCert tc = (TrustedCert) o;

                SwingUtilities.invokeLater(new Runnable() {
                    public void run() {

                        try {
                            tc.setGoid(getTrustedCertAdmin().saveCert(tc));

                            // reload all certs from server
                            if (listener != null) {
                                listener.certSelected(new CertEvent(source, tc));
                            }
                        } catch (ObjectModelException e) {
                            if (ExceptionUtils.causedBy(e, CertificateExpiredException.class)) {
                                JOptionPane.showMessageDialog(source, resources.getString("cert.expired.error"),
                                        resources.getString("save.error.title"),
                                        JOptionPane.ERROR_MESSAGE);
                            } else if (ExceptionUtils.causedBy(e, DuplicateObjectException.class)) {
                                JOptionPane.showMessageDialog(source, resources.getString("cert.duplicate.error"),
                                        resources.getString("save.error.title"),
                                        JOptionPane.ERROR_MESSAGE);
                            } else if (ExceptionUtils.causedBy(e, CertificateNotYetValidException.class)) {
                                JOptionPane.showMessageDialog(source, resources.getString("cert.notyetvalid.error"),
                                        resources.getString("save.error.title"),
                                        JOptionPane.ERROR_MESSAGE);
                            } else if (e instanceof SaveException) {
                                JOptionPane.showMessageDialog(source, resources.getString("cert.save.error"),
                                        resources.getString("save.error.title"),
                                        JOptionPane.ERROR_MESSAGE);
                            } else {
                                JOptionPane.showMessageDialog(source, resources.getString("cert.update.error"),
                                        resources.getString("save.error.title"),
                                        JOptionPane.ERROR_MESSAGE);
                            }
                        } catch (VersionException e) {
                            JOptionPane.showMessageDialog(source, resources.getString("cert.version.error"),
                                    resources.getString("save.error.title"),
                                    JOptionPane.ERROR_MESSAGE);
                        } 
                    }
                });
            }
        }
    }
}
