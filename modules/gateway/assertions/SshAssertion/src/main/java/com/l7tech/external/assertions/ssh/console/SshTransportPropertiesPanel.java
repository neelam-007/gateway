package com.l7tech.external.assertions.ssh.console;

import com.l7tech.console.panels.CustomTransportPropertiesPanel;
import com.l7tech.external.assertions.ssh.SshRouteAssertion;
import com.l7tech.external.assertions.ssh.keyprovider.PemSshHostKeyProvider;
import com.l7tech.gui.util.DialogDisplayer;
import com.l7tech.gui.util.Utilities;
import com.l7tech.util.SyspropUtil;
import org.apache.commons.lang.StringUtils;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.HashMap;
import java.util.Map;

/**
 *
 */
public class SshTransportPropertiesPanel extends CustomTransportPropertiesPanel {
    private JPanel mainPanel;
    private JCheckBox scpCheckBox;
    private JCheckBox sftpCheckBox;
    private JTextField maxConcurrentSessionsPerUserField;
    private JTextField hostPrivateKeyTypeField;
    private JButton setHostPrivateKeyButton;

    private String hostPrivateKey;

    public SshTransportPropertiesPanel() {
        setLayout(new BorderLayout());
        add(mainPanel, BorderLayout.CENTER);
        initComponents();
    }

    private boolean getBooleanProp(Map<String, String> map, String key, boolean dflt) {
        String val = map.get(key);
        if (val == null)
            return dflt;
        return Boolean.parseBoolean(val);
    }

    private String getStringProp(Map<String, String> map, String key, String dflt) {
        String val = map.get(key);
        if (val == null)
            return dflt;
        return val;
    }

    @Override
    public void setData(Map<String, String> props) {
        scpCheckBox.setSelected(getBooleanProp(props, SshRouteAssertion.LISTEN_PROP_ENABLE_SCP,
                SyspropUtil.getBoolean("com.l7tech.external.assertions.ssh.defaultEnableScp", true)));
        sftpCheckBox.setSelected(getBooleanProp(props, SshRouteAssertion.LISTEN_PROP_ENABLE_SFTP,
                SyspropUtil.getBoolean("com.l7tech.external.assertions.ssh.defaultEnableSftp", true)));
        maxConcurrentSessionsPerUserField.setText(getStringProp(props, SshRouteAssertion.LISTEN_PROP_MAX_CONCURRENT_SESSIONS_PER_USER,
                SyspropUtil.getString("com.l7tech.external.assertions.ssh.defaultMaxConcurrentSessionsPerUser", "10")));

        hostPrivateKey = props.get(SshRouteAssertion.LISTEN_PROP_HOST_PRIVATE_KEY);
        if (!StringUtils.isEmpty(hostPrivateKey)) {
            hostPrivateKeyTypeField.setText(PemSshHostKeyProvider.PEM + " " + PemSshHostKeyProvider.getPemAlgorithm(hostPrivateKey));
        }
    }

    @Override
    public Map<String, String> getData() {
        Map<String, String> data = new HashMap<String, String>();
        data.put(SshRouteAssertion.LISTEN_PROP_ENABLE_SCP, String.valueOf(scpCheckBox.isSelected()));
        data.put(SshRouteAssertion.LISTEN_PROP_ENABLE_SFTP, String.valueOf(sftpCheckBox.isSelected()));
        data.put(SshRouteAssertion.LISTEN_PROP_MAX_CONCURRENT_SESSIONS_PER_USER, maxConcurrentSessionsPerUserField.getText());
        data.put(SshRouteAssertion.LISTEN_PROP_HOST_PRIVATE_KEY, hostPrivateKey);
        return data;
    }

    @Override
    public String getValidationError() {
        return null;
    }

    @Override
    public String[] getAdvancedPropertyNamesUsedByGui() {
        return new String[] {
            SshRouteAssertion.LISTEN_PROP_ENABLE_SCP,
            SshRouteAssertion.LISTEN_PROP_ENABLE_SFTP,
            SshRouteAssertion.LISTEN_PROP_MAX_CONCURRENT_SESSIONS_PER_USER,
            SshRouteAssertion.LISTEN_PROP_HOST_PRIVATE_KEY,
        };
    }

    protected void initComponents() {
        setHostPrivateKeyButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                final HostKeyDialog dialog = new HostKeyDialog(SwingUtilities.getWindowAncestor(SshTransportPropertiesPanel.this), hostPrivateKey);
                Utilities.centerOnScreen(dialog);
                DialogDisplayer.display(dialog, new Runnable() {
                    @Override
                    public void run() {
                        if (dialog.isConfirmed()) {
                            hostPrivateKey = dialog.getHostKey();
                            if (!StringUtils.isEmpty(hostPrivateKey)) {
                                hostPrivateKeyTypeField.setText(PemSshHostKeyProvider.PEM + " " + PemSshHostKeyProvider.getPemAlgorithm(hostPrivateKey));
                            }
                        }
                    }
                });
            }
        });
    }
}
