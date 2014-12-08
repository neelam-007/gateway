package com.l7tech.console.panels.solutionkit;

import com.japisoft.xmlpad.XMLContainer;
import com.l7tech.console.util.XMLContainerFactory;
import com.l7tech.gateway.common.solutionkit.SolutionKit;
import com.l7tech.gui.util.Utilities;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

/**
 *
 */
public class SolutionKitPropertiesDialog extends JDialog {
    private JPanel mainPanel;
    private JLabel idFieldLabel;
    private JLabel versionFieldLabel;
    private JLabel nameFieldLabel;
    private JLabel descriptionFieldLabel;
    private JLabel createdTimeFieldLabel;
    private JPanel mappingsPanel;
    private JButton closeButton;

    private XMLContainer mappingsXmlContainer;

    public SolutionKitPropertiesDialog(Dialog owner, SolutionKit solutionKit) {
        super(owner, "Solution Kit Properties", true);

        initialize();
        populateFields(solutionKit);
    }

    private void initialize() {
        mappingsXmlContainer = XMLContainerFactory.createXmlContainer(true);
        mappingsXmlContainer.getUIAccessibility().setPopupAvailable(false);
        mappingsXmlContainer.setEditableDocumentMode(false);
        mappingsXmlContainer.setEditable(false);
        mappingsPanel.setLayout(new BorderLayout());
        mappingsPanel.add(mappingsXmlContainer.getView(), BorderLayout.CENTER);

        closeButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                onClose();
            }
        });

        Utilities.setEscKeyStrokeDisposes(this);
        setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        setContentPane(mainPanel);
    }

    private void populateFields(SolutionKit solutionKit) {
        idFieldLabel.setText(solutionKit.getSolutionKitGuid());
        versionFieldLabel.setText(solutionKit.getSolutionKitVersion());
        nameFieldLabel.setText(solutionKit.getName());
        descriptionFieldLabel.setText(solutionKit.getProperty(SolutionKit.SK_PROP_DESC_KEY));
        createdTimeFieldLabel.setText(solutionKit.getProperty(SolutionKit.SK_PROP_TIMESTAMP_KEY));

        // todo (kpak) - change to table format.
        mappingsXmlContainer.getAccessibility().setText(solutionKit.getMappings());
    }

    private void onClose() {
        dispose();
    }
}