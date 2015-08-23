package com.l7tech.example.solutionkit.simple.v01_01.console;

import com.l7tech.example.solutionkit.simple.v01_01.SimpleSolutionKitManagerCallback;
import com.l7tech.policy.solutionkit.SolutionKitManagerUi;
import com.l7tech.xml.xpath.XpathUtil;
import org.apache.commons.lang.StringUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import static com.l7tech.example.solutionkit.simple.v01_01.SimpleSolutionKitManagerCallback.getNamespaceMap;

/**
 * Simple example of a customized UI for the Solution Kit Manager.
 */
public class SimpleSolutionKitManagerUi extends SolutionKitManagerUi {

    @Override
    public JButton createButton(final JPanel parentPanel) {
        // read metadata
        final Document solutionKitMetadata = getContext().getSolutionKitMetadata();
        final java.util.List<Element> nameElements = XpathUtil.findElements(solutionKitMetadata.getDocumentElement(), "//l7:SolutionKit/l7:Name", getNamespaceMap());
        final String solutionKitName = nameElements.size() > 0 ? nameElements.get(0).getTextContent() : "";

        // read bundle
        final Document restmanBundle = getContext().getMigrationBundle();
        final java.util.List<Element> itemElements = XpathUtil.findElements(restmanBundle.getDocumentElement(), "//l7:Bundle/l7:References/l7:Item", getNamespaceMap());
        final java.util.List<Element> mappingElements = XpathUtil.findElements(restmanBundle.getDocumentElement(), "//l7:Bundle/l7:Mappings/l7:Mapping", getNamespaceMap());

        JButton button = new JButton("Custom UI: " + StringUtils.substring(solutionKitName, 0, 14));
        button.setLayout(new BorderLayout());

        button.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                String inputText = JOptionPane.showInputDialog(parentPanel,
                        "<html>About this bundle: # item(s): " + itemElements.size() + ", # mapping(s): " + mappingElements.size() + ".<br><br>Prefix with customization text below.<html>", "CUSTOMIZED!");

                // pass user input text to the callback e.g. to SimpleSolutionKitManagerCallback.preMigrationBundleImport()
                if (inputText != null) {
                    getContext().getKeyValues().put(SimpleSolutionKitManagerCallback.MY_INPUT_TEXT_KEY, inputText);
                }

                // Metadata and bundle are read-only, any modifications are ignored. Make modifications to the metadata and bundle in SimpleSolutionKitManagerCallback.
                // Solution Kit Manager does not allow modification here to ensures that both the GUI and the headless interface executes the same SimpleSolutionKitManagerCallback.preMigrationBundleImport().
            }
        });

        return button;
    }
}
