package com.l7tech.console.panels.licensing;

import com.l7tech.console.util.TopComponents;
import com.l7tech.gateway.common.licensing.FeatureLicense;
import com.l7tech.gateway.common.licensing.LicenseUtils;
import com.l7tech.gui.widgets.WrappingLabel;
import com.l7tech.util.BuildInfo;
import com.l7tech.util.DateUtils;

import javax.swing.*;
import java.awt.*;
import java.util.Date;
import java.util.Set;

/**
 * @author Jamie Williams - wilja33 - jamie.williams2@ca.com
 */
public class FeatureLicenseDetailsPanel extends JPanel {
    public static final String LICENSE_STATUS_VALID = "   VALID   ";
    public static final String LICENSE_STATUS_INVALID = "  INVALID  ";
    public static final String LICENSE_STATUS_EXPIRED = "  EXPIRED  ";

    private JPanel rootPanel;
    private JPanel grantsPanel;
    private JLabel gatewayField;
    private JLabel statusField;
    private JLabel idField;
    private JLabel descriptionField;
    private JLabel licenseeField;
    private JLabel contactEmailField;
    private JLabel startDateField;
    private JLabel expiryDateField;
    private JLabel issuerField;
    private JTextArea attributesTextArea;

    private final FeatureLicense featureLicense;

    public FeatureLicenseDetailsPanel(FeatureLicense featureLicense) {
        this.featureLicense = featureLicense;
        init();
    }

    private void init() {
        setLayout(new BorderLayout());
        add(rootPanel);

        setStatus();

        gatewayField.setText(TopComponents.getInstance().ssgURL().getHost());
        idField.setText(Long.toString(featureLicense.getId()));
        descriptionField.setText(featureLicense.getDescription());
        licenseeField.setText(featureLicense.getLicenseeName());
        contactEmailField.setText(featureLicense.getLicenseeContactEmail());

        issuerField.setText(featureLicense.hasTrustedIssuer()
                ? featureLicense.getTrustedIssuer().getSubjectDN().getName()
                : "<No trusted issuer signature>");

        formatAndSetDate(startDateField, featureLicense.getStartDate(), true);
        formatAndSetDate(expiryDateField, featureLicense.getExpiryDate(), false);


        attributesTextArea.setFont(idField.getFont());
        setAttributes();

        setGrants();
    }

    private void setAttributes() {
        StringBuilder sb = new StringBuilder();
        Set<String> attrList = featureLicense.getAttributes();

        int i = 0;

        for (String attr : attrList) {
            sb.append(attr);

            if (i++ < attrList.size() - 1) {
                sb.append("\n");
            }
        }

        attributesTextArea.setText(sb.toString());
    }

    private void formatAndSetDate(JLabel label, Date date, boolean skipIfPassed) {
        if (date == null) {
            label.setText("");
            label.setVisible(false);
            return;
        }

        String m = DateUtils.makeRelativeDateMessage(date, skipIfPassed);

        if (m != null && m.length() > 0) {
            m = " (" + m + ")";
        }

        label.setText(date.toString() + m);
        label.setVisible(true);
    }

    private void setGrants() {
        final String grants = LicenseUtils.getGrantsAsEnglish(featureLicense);

        grantsPanel.removeAll();
        grantsPanel.setLayout(new BoxLayout(grantsPanel, BoxLayout.Y_AXIS));
        grantsPanel.add(new JScrollPane(new WrappingLabel(grants, (grants.length() / 70) + 2),
                JScrollPane.VERTICAL_SCROLLBAR_ALWAYS,
                JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED));
    }

    private void setStatus() {
        statusField.setOpaque(true);
        statusField.setBorder(BorderFactory.createLineBorder(Color.BLACK, 2));

        // check for malformed, unsigned, wrong product/version, or not started yet
        if (null == featureLicense || !featureLicense.hasTrustedIssuer() ||
                !featureLicense.isProductEnabled(BuildInfo.getProductName()) ||
                !featureLicense.isVersionEnabled(BuildInfo.getProductVersionMajor(), BuildInfo.getProductVersionMinor()) ||
                !featureLicense.isLicensePeriodStartBefore(System.currentTimeMillis())) {
            statusField.setText(LICENSE_STATUS_INVALID);
            statusField.setForeground(Color.WHITE);
            statusField.setBackground(new Color(255, 92, 92)); // dark red
        } else if (!featureLicense.isLicensePeriodExpiryAfter(System.currentTimeMillis())) { // check for expired
            statusField.setText(LICENSE_STATUS_EXPIRED);
            statusField.setForeground(Color.WHITE);
            statusField.setBackground(new Color(255, 92, 92)); // dark red
        } else { // otherwise, show license as valid
            statusField.setText(LICENSE_STATUS_VALID);
            statusField.setForeground(Color.BLACK);
            statusField.setBackground(new Color(128, 255, 128)); // bright green
        }
    }
}
