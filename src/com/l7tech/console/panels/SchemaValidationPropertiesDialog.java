package com.l7tech.console.panels;

import com.l7tech.console.tree.policy.SchemaValidationTreeNode;

import javax.swing.*;
import java.awt.*;

import org.syntax.jedit.JEditTextArea;
import org.syntax.jedit.SyntaxDocument;
import org.syntax.jedit.tokenmarker.XMLTokenMarker;

/**
 * A dialog to view / configure the properties of a schema validation assertion
 *
 * <br/><br/>
 * LAYER 7 TECHNOLOGIES, INC<br/>
 * User: flascell<br/>
 * Date: Feb 6, 2004<br/>
 * $Id$<br/>
 *
 */
public class SchemaValidationPropertiesDialog extends JDialog {

    public SchemaValidationPropertiesDialog(Frame owner, SchemaValidationTreeNode node) {
        super(owner, false);
        setTitle("Schema validation properties");
        initialize();
    }

    private void initialize() {
        // Make the dialog controls
        Container contents = getContentPane();
        contents.setLayout(new BorderLayout(0,0));
        contents.add(constructCentralPanel(), BorderLayout.CENTER);
        contents.add(constructBottomButtonsPanel(), BorderLayout.SOUTH);
    }

    /**
     * the central panel contains the populate from schema button, the load from schema panel
     * and the schema xml display
     */
    private JPanel constructCentralPanel() {

        // panel that contains the read from wsdl and the read from url
        JPanel toppanel = new JPanel();
        toppanel.setLayout(new BoxLayout(toppanel, BoxLayout.Y_AXIS));
        toppanel.add(constructLoadFromWsdlPanel());
        toppanel.add(constructLoadFromUrlPanel());

        // panel that contains the xml display
        JPanel centerpanel = new JPanel();
        centerpanel.setLayout(new BorderLayout());
        centerpanel.add(constructXmlDisplayPanel(), BorderLayout.CENTER);

        JPanel output = new JPanel();
        output.setLayout(new BorderLayout());

        output.add(toppanel, BorderLayout.NORTH);
        output.add(centerpanel, BorderLayout.CENTER);

        return output;
    }

    private JPanel constructXmlDisplayPanel() {
        JPanel xmldisplayPanel = new JPanel();
        xmldisplayPanel.setLayout(new BorderLayout(0, CONTROL_SPACING));
        JLabel schemaTitle = new JLabel("Schema to validate against:");
        xmldisplayPanel.add(schemaTitle, BorderLayout.NORTH);

        wsdlTextArea = new JEditTextArea();
        wsdlTextArea.setDocument(new SyntaxDocument());
        wsdlTextArea.setEditable(false);
        wsdlTextArea.setTokenMarker(new XMLTokenMarker());
        //wsdlTextArea.setText(ps.getWsdlXml());
        wsdlTextArea.setCaretPosition(0);
        xmldisplayPanel.add(wsdlTextArea, BorderLayout.CENTER);

        // wrap this with border settings
        GridBagConstraints constraints = new GridBagConstraints();
        constraints.fill = GridBagConstraints.BOTH;
        constraints.weightx = 1.0;
        constraints.weighty = 1.0;
        constraints.insets = new Insets(0, BORDER_PADDING, 0, BORDER_PADDING);
        JPanel bordered = new JPanel();
        bordered.setLayout(new GridBagLayout());
        bordered.add(xmldisplayPanel, constraints);

        return bordered;
    }

    private JPanel constructLoadFromWsdlPanel() {
        // construct button
        readFromWsdlButton = new JButton();
        readFromWsdlButton.setText("Populate schema from WSDL");

        // align to the left
        JPanel loadfromwsdlpanel = new JPanel();
        loadfromwsdlpanel.setLayout(new BorderLayout());
        loadfromwsdlpanel.add(readFromWsdlButton, BorderLayout.WEST);

        // add border
        GridBagConstraints constraints = new GridBagConstraints();
        constraints.fill = GridBagConstraints.BOTH;
        constraints.weightx = 1.0;
        constraints.weighty = 1.0;
        constraints.insets = new Insets(BORDER_PADDING, BORDER_PADDING, 0, BORDER_PADDING);
        JPanel bordered = new JPanel();
        bordered.setLayout(new GridBagLayout());
        bordered.add(loadfromwsdlpanel, constraints);

        return bordered;
    }

    private JPanel constructLoadFromUrlPanel() {
        // make controls
        JPanel loadFromUrlPanel = new JPanel();
        loadFromUrlPanel.setLayout(new BorderLayout(CONTROL_SPACING, 0));
        JLabel loadfromurllabel = new JLabel("Load schema from URL:");
        loadFromUrlPanel.add(loadfromurllabel, BorderLayout.WEST);
        urlTxtFld = new JTextField();
        loadFromUrlPanel.add(urlTxtFld, BorderLayout.CENTER);
        resolveButton = new JButton();
        resolveButton.setText("Resolve");
        loadFromUrlPanel.add(resolveButton, BorderLayout.EAST);

        // wrap this with border settings
        GridBagConstraints constraints = new GridBagConstraints();
        constraints.fill = GridBagConstraints.BOTH;
        constraints.weightx = 1.0;
        constraints.weighty = 1.0;
        constraints.insets = new Insets(BORDER_PADDING, BORDER_PADDING, BORDER_PADDING, BORDER_PADDING);
        JPanel bordered = new JPanel();
        bordered.setLayout(new GridBagLayout());
        bordered.add(loadFromUrlPanel, constraints);

        return bordered;
    }

    private JPanel constructBottomButtonsPanel() {
        // construct buttons
        helpButton = new JButton();
        helpButton.setText("Help");
        okButton = new JButton();
        okButton.setText("Ok");
        cancelButton = new JButton();
        cancelButton.setText("Cancel");

        // construct the bottom panel and wrap it with a border
        JPanel buttonsPanel = new JPanel();
        buttonsPanel.setLayout(new FlowLayout(FlowLayout.TRAILING, CONTROL_SPACING, 0));
        buttonsPanel.add(helpButton);
        buttonsPanel.add(okButton);
        buttonsPanel.add(cancelButton);

        //  make this panel align to the right
        JPanel rightPanel = new JPanel();
        rightPanel.setLayout(new BorderLayout());
        rightPanel.add(buttonsPanel, BorderLayout.EAST);

        // wrap this with border settings
        JPanel output = new JPanel();
        output.setLayout(new FlowLayout(FlowLayout.TRAILING, BORDER_PADDING, BORDER_PADDING));
        output.add(rightPanel);

        return output;
    }

    // show the dialog for test purposes
    public static void main(String[] args) {
        SchemaValidationPropertiesDialog dlg = new SchemaValidationPropertiesDialog(null, null);
        dlg.pack();
        dlg.show();
        //System.exit(0);
    }

    private JButton helpButton;
    private JButton okButton;
    private JButton cancelButton;
    private JButton readFromWsdlButton;
    private JButton resolveButton;
    private JTextField urlTxtFld;
    private JEditTextArea wsdlTextArea;

    private static int BORDER_PADDING = 20;
    private static int CONTROL_SPACING = 5;
}
