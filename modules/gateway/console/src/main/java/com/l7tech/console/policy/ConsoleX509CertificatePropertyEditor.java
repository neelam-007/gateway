package com.l7tech.console.policy;

import com.l7tech.console.event.WizardAdapter;
import com.l7tech.console.event.WizardEvent;
import com.l7tech.console.panels.AddCertificateWizard;
import com.l7tech.console.panels.CertDetailsPanel;
import com.l7tech.console.panels.CertImportMethodsPanel;
import com.l7tech.gui.util.DialogDisplayer;
import com.l7tech.gui.util.ImageCache;
import com.l7tech.gui.util.Utilities;
import com.l7tech.security.cert.TrustedCert;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.beans.PropertyEditor;
import java.security.cert.X509Certificate;

/**
 * Property editor for an X509 certificate literal that shows the "select certificate" dialog to choose a cert.
 * <p/>
 * This uses the SecureSpan Manager's CertImportMethodsPanel and so only works within the SSM.
 */
public class ConsoleX509CertificatePropertyEditor extends JPanel implements PropertyEditor {

    private final JTextField dnLabel = new JTextField();
    private final JButton changeCertButton = new JButton("", ImageCache.getInstance().getIconAsIcon("com/l7tech/console/resources/cert16.gif"));
    private X509Certificate cert;

    public ConsoleX509CertificatePropertyEditor() {
        setLayout(new BorderLayout());
        add(dnLabel, BorderLayout.CENTER);
        add(changeCertButton, BorderLayout.EAST);
        dnLabel.setEditable(false);
        changeCertButton.setMargin(new Insets(0, 0, 0, 0));
        changeCertButton.setVerticalTextPosition(SwingConstants.BOTTOM);
        changeCertButton.setHorizontalTextPosition(SwingConstants.CENTER);
        addPropertyChangeListener(new PropertyChangeListener() {
            @Override
            public void propertyChange(PropertyChangeEvent evt) {
                if (evt.getPropertyName() != null)
                    firePropertyChange(null, evt.getOldValue(), evt.getNewValue());
            }
        });
        changeCertButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                final CertImportMethodsPanel sp = new CertImportMethodsPanel(
                    new CertDetailsPanel(null) {
                        @Override
                        public boolean canFinish() {
                            return true;
                        }
                    }, false);

                final AddCertificateWizard w = new AddCertificateWizard(SwingUtilities.getWindowAncestor(changeCertButton), sp);
                w.setTitle("Configure Certificate");
                w.addWizardListener(new WizardAdapter() {
                    @Override
                    public void wizardFinished(WizardEvent we) {
                        Object o = w.getWizardInput();

                        if (o == null) return;
                        if (!(o instanceof TrustedCert)) {
                            // shouldn't happen
                            throw new IllegalStateException("Wizard returned a " + o.getClass().getName() + ", was expecting a " + TrustedCert.class.getName());
                        }

                        X509Certificate[] chain = sp.getCertChain();
                        if (chain.length < 1 || chain[0] == null)
                            return;

                        setCertificate(chain[0]);
                    }
                });

                w.pack();
                Utilities.centerOnParentWindow(w);
                DialogDisplayer.display(w);
            }
        });
        setCertificate(null);
    }

    private void setCertificate(@Nullable X509Certificate newCert) {
        X509Certificate oldCert = this.cert;
        this.cert = newCert;
        dnLabel.setText(newCert == null ? "<Not Set>" : newCert.getSubjectDN().getName());
        firePropertyChange(null, oldCert, newCert);
    }

    @Override
    public void setValue(Object value) {
        if (value instanceof X509Certificate) {
            setCertificate((X509Certificate) value);
        }
    }

    @Override
    public Object getValue() {
        return cert;
    }

    @Override
    public boolean isPaintable() {
        return false;
    }

    @Override
    public void paintValue(Graphics gfx, Rectangle box) {
    }

    @Override
    public String getJavaInitializationString() {
        return "???";
    }

    @Override
    public String getAsText() {
        return null;
    }

    @Override
    public void setAsText(String text) throws IllegalArgumentException {
        throw new IllegalArgumentException();
    }

    @Override
    public String[] getTags() {
        return null;
    }

    @Override
    public Component getCustomEditor() {
        return this;
    }

    @Override
    public boolean supportsCustomEditor() {
        return true;
    }
}
