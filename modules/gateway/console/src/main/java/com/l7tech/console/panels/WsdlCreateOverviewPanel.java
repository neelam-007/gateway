package com.l7tech.console.panels;

import javax.swing.*;
import java.awt.*;

/**
 * The WSDL create overview panel. This is a support class for the
 * <i>WsdlCreateOverview.form</i>
 * <p/>
 *
 * @author <a href="mailto:emarceta@layer7-tech.com">Emil Marceta</a>
 * @version 1.0
 */
public class WsdlCreateOverviewPanel extends WizardStepPanel {

    private JPanel mainPanel;
    private JLabel panelHeader;
    private JTextPane overviewTextPane;

    public WsdlCreateOverviewPanel(WizardStepPanel next) {
        super(next);
        setShowDescriptionPanel(false);
        setLayout(new BorderLayout());
        /** Set content pane */
        add(mainPanel, BorderLayout.CENTER);
        panelHeader.setFont(new java.awt.Font("Dialog", 1, 16));

        overviewTextPane.setText(
                "<html>\n" +
                "<body>\n" +
                "<p><b>This Wizard will guide you through the steps required to generate WSDL for a new Web service.</b></p>\n" +
                "<p><b>You will be asked to provide:</b></p>\n" +
                "<table>\n" +
                "<tr><td valign=top><p><b>-</b></p></td><td valign=top><p><b>A service definition</b></p></td></tr>\n" +
                "<tr><td valign=top><p><b>-</b></p></td><td valign=top><p><b>The operations, operations messages, and message parts involved in the Web service</b></p></td></tr>\n" +
                "<tr><td valign=top><p><b>-</b></p></td><td valign=top><p><b>A service endpoint address and the SOAPAction operation attributes</b></p></td></tr>\n" +
                "</table>\n" +
                "</body>\n" +
                "</html>");
    }

    /**
     * @return the wizard step label
     */
    public String getStepLabel() {
        return "Overview";
    }

}
