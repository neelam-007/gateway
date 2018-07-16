package com.l7tech.example.solutionkit.simple.v01_03.console;

import com.l7tech.example.solutionkit.simple.v01_03.SimpleOtherSolutionKitManagerCallback;
import com.l7tech.policy.solutionkit.SolutionKitManagerUi;
import com.l7tech.xml.xpath.XpathUtil;
import org.apache.commons.lang.StringUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

/**
 * Simple example of a customized UI for the Solution Kit Manager.
 */
public class SimpleOtherSolutionKitManagerUi extends SolutionKitManagerUi {

    @Override
    public JButton createButton(final JPanel parentPanel) {
        // read metadata
        final Document solutionKitMetadata = getContext().getSolutionKitMetadata();
        final java.util.List<Element> nameElements = XpathUtil.findElements(solutionKitMetadata.getDocumentElement(), "//l7:SolutionKit/l7:Name", SimpleOtherSolutionKitManagerCallback.getNamespaceMap());
        final String solutionKitName = nameElements.size() > 0 ? nameElements.get(0).getTextContent() : "";

        // read bundle
        final Document restmanBundle = getContext().getMigrationBundle();
        final java.util.List<Element> itemElements = XpathUtil.findElements(restmanBundle.getDocumentElement(), "//l7:Bundle/l7:References/l7:Item", SimpleOtherSolutionKitManagerCallback.getNamespaceMap());
        final java.util.List<Element> mappingElements = XpathUtil.findElements(restmanBundle.getDocumentElement(), "//l7:Bundle/l7:Mappings/l7:Mapping", SimpleOtherSolutionKitManagerCallback.getNamespaceMap());

        JButton button = new JButton("Custom UI: " + StringUtils.substring(solutionKitName, 0, 14));
        button.setLayout(new BorderLayout());

        button.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                String inputText = JOptionPane.showInputDialog(parentPanel,
                        "<html>About this bundle: # item(s): " + itemElements.size() + ", # mapping(s): " + mappingElements.size() + ".<br>Instance modifier: " + getContext().getInstanceModifier() + "<br><br>Prefix with customization text below.<html>", "Hello!");

                // pass user input text to the callback e.g. to SimpleSolutionKitManagerCallback.preMigrationBundleImport()
                if (inputText != null) {
                    getContext().getKeyValues().put(SimpleOtherSolutionKitManagerCallback.MY_INPUT_TEXT_KEY, inputText);
                }

                // Metadata and bundle are read-only, any modifications are ignored. Make modifications to the metadata and bundle in SimpleSolutionKitManagerCallback.
                // Solution Kit Manager does not allow modification here to ensures that both the GUI and the headless interface executes the same SimpleSolutionKitManagerCallback.preMigrationBundleImport().
            }
        });

        // pass flag to callback indicating if button was created
        getContext().getKeyValues().put(SimpleOtherSolutionKitManagerCallback.MY_WAS_BUTTON_CREATED_KEY, Boolean.TRUE.toString());

        return button;
    }
}
