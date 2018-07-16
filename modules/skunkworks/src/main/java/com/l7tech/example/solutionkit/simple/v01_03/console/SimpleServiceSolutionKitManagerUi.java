package com.l7tech.example.solutionkit.simple.v01_03.console;

import com.l7tech.example.solutionkit.simple.v01_03.SimpleOtherSolutionKitManagerCallback;
import com.l7tech.example.solutionkit.simple.v01_03.SimpleServiceSolutionKitManagerCallback;
import com.l7tech.policy.solutionkit.SolutionKitManagerContext;
import com.l7tech.policy.solutionkit.SolutionKitManagerUi;
import com.l7tech.util.Pair;
import com.l7tech.xml.xpath.XpathUtil;
import org.apache.commons.lang.StringUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

/**
 * Simple example of a customized UI accessing the context from other kits.
 */
public class SimpleServiceSolutionKitManagerUi extends SolutionKitManagerUi {

    @Override
    public JButton createButton(final JPanel parentPanel) {
        final String mySolutionKitName = getSolutionKitName(getContext().getSolutionKitMetadata());

        JButton button = new JButton("Custom UI: " + StringUtils.substring(mySolutionKitName, 0, 14));
        button.setLayout(new BorderLayout());

        button.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                final SolutionKitManagerContext otherContext = getContextMap().get(SimpleServiceSolutionKitManagerCallback.OTHER_GUID);
                if (otherContext == null) {
                    throw new RuntimeException("Failed to retrieve context for other solution kit: " + SimpleServiceSolutionKitManagerCallback.OTHER_GUID);
                }

                // read metadata
                final String otherSolutionKitName = getSolutionKitName(otherContext.getSolutionKitMetadata());

                // read bundle
                final Pair<java.util.List<Element>, java.util.List<Element>> myBundleItemsAndMappingsPair = getBundleItemsAndMappingsPair(getContext().getMigrationBundle());
                final Pair<java.util.List<Element>, java.util.List<Element>> otherBundleItemsAndMappingsPair = getBundleItemsAndMappingsPair(otherContext.getMigrationBundle());
                final java.util.List<Element> myItemElements = myBundleItemsAndMappingsPair.left;
                final java.util.List<Element> myMappingElements = myBundleItemsAndMappingsPair.right;
                final java.util.List<Element> otherItemElements = otherBundleItemsAndMappingsPair.left;
                final java.util.List<Element> otherMappingElements = otherBundleItemsAndMappingsPair.right;

                //read keyValues
                String otherTest;
                final String otherInput = otherContext.getKeyValues().get(SimpleOtherSolutionKitManagerCallback.MY_INPUT_TEXT_KEY);
                if (otherInput == null) {
                    otherTest = "<p/><p style=\"border:1px dotted red;\">PREREQUISITES NOT MET:<br/>Solution kit '" + otherSolutionKitName + "' must be configured first.<br/>" +
                            "Please highlight the solution kit row and use the 'Custom UI: ...' button to enter some text.</p>";
                } else {
                    // if other solution kit UI has been processed set that our has as well
                    getContext().getKeyValues().put(SimpleServiceSolutionKitManagerCallback.MY_WAS_UI_PROCESSED, Boolean.TRUE.toString());

                    otherTest = "<p/><p style=\"border:1px dotted green;\">You may proceed with the installation.</p>";
                }

                JOptionPane.showMessageDialog(
                        parentPanel,
                        "<html>" +
                                "<p style=\"border:1px dotted black;\">About this bundle: # item(s): " + myItemElements.size() + ", # mapping(s): " + myMappingElements.size() + ".<br>Instance modifier: " + getContext().getInstanceModifier() + "</p>" +
                                "<p/><p style=\"border:1px dotted black;\">About " + otherSolutionKitName + " bundle: # item(s): " + otherItemElements.size() + ", # mapping(s): " + otherMappingElements.size() + ".<br>Instance modifier: " + otherContext.getInstanceModifier() + "<br><br>Prefixed with customization text: " + otherInput + ".</p>" +
                                otherTest +
                        "<html>"
                );
            }
        });

        // pass flag to callback indicating if button was created
        getContext().getKeyValues().put(SimpleServiceSolutionKitManagerCallback.MY_WAS_BUTTON_CREATED_KEY, Boolean.TRUE.toString());

        return button;
    }

    private static String getSolutionKitName(final Document solutionKitMetadata) {
        final java.util.List<Element> nameElements = XpathUtil.findElements(solutionKitMetadata.getDocumentElement(), "//l7:SolutionKit/l7:Name", SimpleServiceSolutionKitManagerCallback.getNamespaceMap());
        return nameElements.size() > 0 ? nameElements.get(0).getTextContent() : "";
    }

    private static Pair<java.util.List<Element>, java.util.List<Element>> getBundleItemsAndMappingsPair(final Document restmanBundle) {
        return Pair.pair(
                XpathUtil.findElements(restmanBundle.getDocumentElement(), "//l7:Bundle/l7:References/l7:Item", SimpleServiceSolutionKitManagerCallback.getNamespaceMap()),
                XpathUtil.findElements(restmanBundle.getDocumentElement(), "//l7:Bundle/l7:Mappings/l7:Mapping", SimpleServiceSolutionKitManagerCallback.getNamespaceMap())
        );
    }
}
