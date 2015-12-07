package com.l7tech.external.assertions.extensiblesocketconnectorassertion.console;

import com.l7tech.external.assertions.extensiblesocketconnectorassertion.codecconfigurations.CodecConfiguration;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

/**
 * Created by IntelliJ IDEA.
 * User: njordan
 * Date: 29/03/12
 * Time: 2:27 PM
 * To change this template use File | Settings | File Templates.
 */
public class ViewCodecSettingsDialog extends JDialog {
    private JPanel mainPanel;
    private JPanel codecSettingsPaneContainer;
    private JButton closeButton;

    private CodecConfiguration codecConfig;

    public ViewCodecSettingsDialog(Frame parent, CodecConfiguration codecConfig, CodecSettingsPanel codecSettingsPanel) {
        super(parent, "Codec Settings", true);

        this.codecConfig = codecConfig;
        initComponents(codecSettingsPanel);
    }

    private void initComponents(CodecSettingsPanel codecSettingsPanel) {
        codecSettingsPanel.updateView(codecConfig);
        codecSettingsPaneContainer.add(codecSettingsPanel.getPanel());

        closeButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                setVisible(false);
            }
        });

        setContentPane(mainPanel);
        pack();
    }
}
