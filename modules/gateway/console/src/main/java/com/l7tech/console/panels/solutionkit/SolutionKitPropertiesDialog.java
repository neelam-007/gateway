package com.l7tech.console.panels.solutionkit;

import com.l7tech.gateway.api.Item;
import com.l7tech.gateway.api.Mappings;
import com.l7tech.gateway.api.impl.MarshallingUtils;
import com.l7tech.gateway.common.solutionkit.SolutionKit;
import com.l7tech.gui.util.Utilities;
import com.l7tech.util.ExceptionUtils;

import javax.swing.*;
import javax.xml.transform.stream.StreamSource;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.io.StringReader;
import java.util.Date;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Dialog used to view the properties of installed solution kit.
 */
public class SolutionKitPropertiesDialog extends JDialog {
    private static final Logger logger = Logger.getLogger(SolutionKitPropertiesDialog.class.getName());

    private JPanel mainPanel;
    private JLabel idFieldLabel;
    private JLabel versionFieldLabel;
    private JLabel nameFieldLabel;
    private JLabel descriptionFieldLabel;
    private JLabel createdTimeFieldLabel;
    private JLabel lastUpdatedFieldLabel;
    private SolutionKitMappingsPanel solutionKitMappingsPanel;
    private JButton closeButton;
    private JLabel instanceModifierLabel;

    /**
     * Create dialog.
     *
     * @param owner the owner of the dialog
     * @param solutionKit the solution kit
     */
    public SolutionKitPropertiesDialog(Dialog owner, SolutionKit solutionKit) {
        super(owner, "Solution Kit Properties", true);

        initialize();
        populateFields(solutionKit);
    }

    private void initialize() {
        solutionKitMappingsPanel.hideNameColumn();
        solutionKitMappingsPanel.hideErrorTypeColumn();
        solutionKitMappingsPanel.hideResolvedColumn();

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
        instanceModifierLabel.setText(solutionKit.getProperty(SolutionKit.SK_PROP_INSTANCE_MODIFIER_KEY));
        nameFieldLabel.setText(solutionKit.getName());
        descriptionFieldLabel.setText(solutionKit.getProperty(SolutionKit.SK_PROP_DESC_KEY));
        createdTimeFieldLabel.setText(solutionKit.getProperty(SolutionKit.SK_PROP_TIMESTAMP_KEY));

        lastUpdatedFieldLabel.setText(new Date(solutionKit.getLastUpdateTime()).toString());
        try {
            Item item = MarshallingUtils.unmarshal(Item.class, new StreamSource(new StringReader(solutionKit.getMappings())));
            Mappings mappings = (Mappings)item.getContent();
            solutionKitMappingsPanel.setData(mappings);
        } catch (IOException e) {
            logger.log(Level.WARNING, ExceptionUtils.getMessage(e), ExceptionUtils.getDebugException(e));
        }
    }

    private void onClose() {
        dispose();
    }
}