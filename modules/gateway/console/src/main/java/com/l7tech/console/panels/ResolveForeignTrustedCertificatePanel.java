package com.l7tech.console.panels;

import com.l7tech.console.event.CertEvent;
import com.l7tech.console.event.CertListener;
import com.l7tech.console.event.CertListenerAdapter;
import com.l7tech.console.table.TrustedCertTableSorter;
import com.l7tech.console.table.TrustedCertsTable;
import com.l7tech.console.util.Registry;
import com.l7tech.gui.util.DialogDisplayer;
import com.l7tech.gui.util.Utilities;
import com.l7tech.objectmodel.FindException;
import com.l7tech.objectmodel.Goid;
import com.l7tech.policy.exporter.TrustedCertReference;
import com.l7tech.security.cert.TrustedCert;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Logger;

/**
 * User: Mike
 * Date: Aug 18, 2008
 */
public class ResolveForeignTrustedCertificatePanel extends WizardStepPanel {
    private final Logger logger = Logger.getLogger(ResolveForeignTrustedCertificatePanel.class.getName());

    private TrustedCertReference certReference;
    private JPanel mainPanel;
    private JTextField certName;
    private JTextField issuerDn;
    private JTextField serialNumber;
    private JRadioButton changeRadio;
    private JRadioButton deleteRadio;
    private JRadioButton ignoreRadio;
    private JButton selectCert;
    private JButton createCert;
    private JButton importCert;
    private JScrollPane selectCertScrollPane;
    private TrustedCertsTable trustedCertTable;

    private List<TrustedCert> certList;
    private TrustedCert selectedServerCert;

    public ResolveForeignTrustedCertificatePanel(WizardStepPanel next, TrustedCertReference trustedCertReference) {
        super(next);
        this.certReference = trustedCertReference;
        try {
            initialize();
        } catch (FindException e) {
            throw new RuntimeException("Error while initializing the trusted certs resolution wizard. " + e);
        }
    }

    public void initialize() throws FindException {
        setLayout(new BorderLayout());
        add(mainPanel);
        // Show details of the unresolved reference
        certName.setText(certReference.getCertName() == null? "<Not Found>" : certReference.getCertName());
        issuerDn.setText(certReference.getCertIssuerDn() == null? "<Not Found>" : certReference.getCertIssuerDn());
        serialNumber.setText(certReference.getCertSerial() == null? "<Not Found>" : certReference.getCertSerial().toString());

        changeRadio.setSelected(true);

        Utilities.enableGrayOnDisabled(selectCertScrollPane);
        
        certList = new ArrayList<TrustedCert>();
        trustedCertTable = new TrustedCertsTable();
        trustedCertTable.getTableSorter().setData(certList);

        selectCertScrollPane.setViewportView(trustedCertTable);
        selectCertScrollPane.getViewport().setBackground(Color.white);

        // Hide the cert issuer column
        trustedCertTable.hideColumn(TrustedCertTableSorter.CERT_TABLE_ISSUER_NAME_COLUMN_INDEX);
        trustedCertTable.setEnabled(true);

        createCert.setEnabled(true);
        createCert.addActionListener( new NewTrustedCertificateAction(certListener, "Add"));
        importCert.addActionListener( new ImportTrustedCertificateAction(certListener, "Import"));
        selectCert.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                CertSearchPanel sp = new CertSearchPanel(owner, false, true);
                sp.addCertListener(certListener);
                sp.pack();
                Utilities.centerOnScreen(sp);
                DialogDisplayer.display(sp);
            }
        });

        Utilities.equalizeButtonSizes(selectCert, createCert);

        ActionListener updateEnableStates = new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                enableDisableComponents();
            }
        };

        for (AbstractButton b : Arrays.asList(changeRadio, deleteRadio, ignoreRadio))
            b.addActionListener(updateEnableStates);

        populateTrustedCerts();
    }

    private void populateTrustedCerts() throws FindException {
        certList = Registry.getDefault().getTrustedCertManager().findAllCerts();
        updateTrustedCertTable();

    }

    private void enableDisableComponents() {
        selectCertScrollPane.setEnabled(changeRadio.isSelected());
        selectCert.setEnabled(changeRadio.isSelected());
        createCert.setEnabled(changeRadio.isSelected());
        notifyListeners();
    }

    @Override
    public String getDescription() {
        return getStepLabel();
    }

    @Override
    public boolean canAdvance() {
        return !changeRadio.isSelected() || selectedServerCert != null;
    }

    @Override
    public boolean canFinish() {
        return !hasNextPanel() && canAdvance();
    }

    @Override
    public String getStepLabel() {
        return "Unresolved Trusted certificate " + certReference.getCertName();
    }

    @Override
    public boolean onNextButton() {
        // collect actions details and store in the reference for resolution
        if (changeRadio.isSelected()) {
            Goid newCertObjectId = selectedServerCert.getGoid();
            if (newCertObjectId == null) {
                // this cannot happen
                logger.severe("Could not get trusted certificate ID.");
                return false;
            }
            certReference.setLocalizeReplace(newCertObjectId);
        } else if (deleteRadio.isSelected()) {
            certReference.setLocalizeDelete();
        } else if (ignoreRadio.isSelected()) {
            certReference.setLocalizeIgnore();
        }
        return true;
    }

    private void updateTrustedCertTable() {
        certList.clear();

        if (selectedServerCert != null) certList.add(selectedServerCert);
        trustedCertTable.getTableSorter().setData(certList);
        trustedCertTable.getTableSorter().fireTableDataChanged();
    }

    private CertListener certListener = new CertListenerAdapter() {
        @Override
        public void certSelected(CertEvent e) {
            selectedServerCert = e.getCert();
            if (selectedServerCert != null) updateTrustedCertTable();
            notifyListeners();
        }
    };
}
