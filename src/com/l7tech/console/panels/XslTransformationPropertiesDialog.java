package com.l7tech.console.panels;

import com.l7tech.common.gui.util.Utilities;
import com.l7tech.common.util.XmlUtil;
import com.l7tech.console.action.Actions;
import com.l7tech.console.event.PolicyEvent;
import com.l7tech.console.event.PolicyListener;
import com.l7tech.policy.AssertionPath;
import com.l7tech.policy.assertion.Assertion;
import com.l7tech.policy.assertion.xml.XslTransformation;
import org.apache.xml.serialize.OutputFormat;
import org.apache.xml.serialize.XMLSerializer;
import org.syntax.jedit.JEditTextArea;
import org.syntax.jedit.SyntaxDocument;
import org.syntax.jedit.tokenmarker.XMLTokenMarker;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.SAXException;

import javax.swing.*;
import javax.swing.event.EventListenerList;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;
import java.io.*;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.EventListener;
import java.util.Locale;
import java.util.ResourceBundle;
import java.util.logging.Level;
import java.util.logging.Logger;


/**
 * A dialog to view / configure the properties of a xslt assertion.
 * <p/>
 * <br/><br/>
 * LAYER 7 TECHNOLOGIES, INC<br/>
 * User: flascell<br/>
 * Date: Feb 6, 2004<br/>
 * $Id$<br/>
 */
public class XslTransformationPropertiesDialog extends JDialog {

    /**
     * modless construction
     */
    public XslTransformationPropertiesDialog(Frame owner, boolean modal, XslTransformation assertion) {
        super(owner, modal);
        subject = assertion;
        initialize();
    }

    private void initialize() {

        initResources();
        setTitle(resources.getString("window.title"));

        // create controls
        allocControls();

        // do layout stuff
        Container contents = getContentPane();
        contents.setLayout(new BorderLayout(0, 0));
        contents.add(constructCentralPanel(), BorderLayout.CENTER);
        contents.add(constructBottomButtonsPanel(), BorderLayout.SOUTH);
        Actions.setEscKeyStrokeDisposes(this);
             
        // create callbacks
        okButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                ok();
            }
        });
        cancelButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                cancel();
            }
        });
        helpButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                Actions.invokeHelp(XslTransformationPropertiesDialog.this);
            }
        });
        resolveButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                readFromUrl();
            }
        });

        loadFromFile.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                readFromFile();
            }
        });
    }

    private void ok() {
        // validate the contents of the xml control
        String contents = xmlTextArea.getText();
        if (contents == null || contents.length() < 1 || !docIsXsl(contents)) {
            displayError(resources.getString("error.notxslt"), null);
            return;
        }
        // save new xslt
        subject.setXslSrc(contents);
        // save the name
        String name = nameTxtFld.getText();
        subject.setTransformName(name);
        // save the direction
        Object selectedDirection = directionCombo.getSelectedItem();
        if (directions[0].equals(selectedDirection)) {
            log.finest("selected request direction");
            subject.setDirection(XslTransformation.APPLY_TO_REQUEST);
        } else if (directions[1].equals(selectedDirection)) {
            log.finest("selected response direction");
            subject.setDirection(XslTransformation.APPLY_TO_RESPONSE);
        } else {
            log.warning("cannot get direction!");
        }
        fireEventAssertionChanged(subject);
        // exit
        XslTransformationPropertiesDialog.this.dispose();
    }

    /**
     * notfy the listeners
     *
     * @param a the assertion
     */
    private void fireEventAssertionChanged(final Assertion a) {
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                int[] indices = new int[a.getParent().getChildren().indexOf(a)];
                PolicyEvent event = new
                  PolicyEvent(this, new AssertionPath(a.getPath()), indices, new Assertion[]{a});
                EventListener[] listeners = listenerList.getListeners(PolicyListener.class);
                for (int i = 0; i < listeners.length; i++) {
                    ((PolicyListener)listeners[i]).assertionsChanged(event);
                }
            }
        });
    }

    private void readFromFile() {
        JFileChooser dlg = Utilities.createJFileChooser();

        if (JFileChooser.APPROVE_OPTION != dlg.showOpenDialog(this)) {
            return;
        }
        FileInputStream fis = null;
        String filename = dlg.getSelectedFile().getAbsolutePath();
        try {
            fis = new FileInputStream(dlg.getSelectedFile());
        } catch (FileNotFoundException e) {
            log.log(Level.FINE, "cannot open file" + filename, e);
            return;
        }

        // try to get document
        Document doc = null;
        try {
            doc = XmlUtil.parse(fis);
        } catch (SAXException e) {
            displayError(resources.getString("error.noxmlaturl") + " " + filename, null);
            log.log(Level.FINE, "cannot parse " + filename, e);
            return;
        } catch (IOException e) {
            displayError(resources.getString("error.noxmlaturl") + " " + filename, null);
            log.log(Level.FINE, "cannot parse " + filename, e);
            return;
        }
        // check if it's a xslt
        if (docIsXsl(doc)) {
            // set the new xslt
            String printedxml = null;
            try {
                printedxml = doc2String(doc);
            } catch (IOException e) {
                String msg = "error serializing document";
                displayError(msg, null);
                log.log(Level.FINE, msg, e);
                return;
            }
            xmlTextArea.setText(printedxml);
            //okButton.setEnabled(true);
        } else {
            displayError(resources.getString("error.urlnoxslt") + " " + filename, null);
        }
    }

    /**
     * add the PolicyListener
     *
     * @param listener the PolicyListener
     */
    public void addPolicyListener(PolicyListener listener) {
        listenerList.add(PolicyListener.class, listener);
    }

    /**
     * remove the the PolicyListener
     *
     * @param listener the PolicyListener
     */
    public void removePolicyListener(PolicyListener listener) {
        listenerList.remove(PolicyListener.class, listener);
    }

    private boolean docIsXsl(String str) {
        if (str == null || str.length() < 1) {
            log.finest("empty doc");
            return false;
        }
        Document doc = stringToDoc(str);
        if (doc == null) return false;
        return docIsXsl(doc);
    }

    private boolean docIsXsl(Document doc) {
        Element rootEl = doc.getDocumentElement();

        if (!XSL_NS.equals(rootEl.getNamespaceURI())) {
            log.log(Level.WARNING, "document is not valid xslt (namespace is not + " + XSL_NS + ")");
            return false;
        }

        if (XSL_TOPEL_NAME.equals(rootEl.getLocalName())) {
            return true;
        } else if (XSL_TOPEL_NAME2.equals(rootEl.getLocalName())) {
            return true;
        }
        log.log(Level.WARNING, "document is not xslt (top element " + rootEl.getLocalName() +
          " is not " + XSL_TOPEL_NAME + " or " + XSL_TOPEL_NAME2 + ")");
        return false;
    }

    private Document stringToDoc(String str) {
        Document doc = null;
        try {
            doc = XmlUtil.stringToDocument(str);
        } catch (SAXException e) {
            log.log(Level.WARNING, "cannot parse doc", e);
            return null;
        } catch (IOException e) {
            log.log(Level.WARNING, "cannot parse doc", e);
            return null;
        }
        return doc;
    }

    private String reformatxml(String input) {
        Document doc = stringToDoc(input);
        try {
            return doc2String(doc);
        } catch (IOException e) {
            log.log(Level.INFO, "reformat could not serialize", e);
            return null;
        }
    }

    private void cancel() {
        XslTransformationPropertiesDialog.this.dispose();
    }

    private void readFromUrl() {
        // get url
        String urlstr = urlTxtFld.getText();
        if (urlstr == null || urlstr.length() < 1) {
            displayError(resources.getString("error.nourl"), null);
            return;
        }
        // compose input source
        URL url = null;
        try {
            url = new URL(urlstr);
        } catch (MalformedURLException e) {
            displayError(urlstr + " " + resources.getString("error.badurl"), null);
            log.log(Level.FINE, "malformed url", e);
            return;
        }
        // try to get document
        InputStream is = null;
        try {
            is = url.openStream();
        } catch (IOException e) {
            displayError(resources.getString("error.urlnocontent") + " " + urlstr, null);
            return;
        }
        Document doc = null;
        try {
            doc = XmlUtil.parse(is);
        } catch (SAXException e) {
            displayError(resources.getString("error.noxmlaturl") + " " + urlstr, null);
            log.log(Level.FINE, "cannot parse " + urlstr, e);
            return;
        } catch (IOException e) {
            displayError(resources.getString("error.noxmlaturl") + " " + urlstr, null);
            log.log(Level.FINE, "cannot parse " + urlstr, e);
            return;
        }
        // check if it's a xslt
        if (docIsXsl(doc)) {
            // set the new xslt
            String printedxml = null;
            try {
                printedxml = doc2String(doc);
            } catch (IOException e) {
                String msg = "error serializing document";
                displayError(msg, null);
                log.log(Level.FINE, msg, e);
                return;
            }
            xmlTextArea.setText(printedxml);
            //okButton.setEnabled(true);
        } else {
            displayError(resources.getString("error.urlnoxslt") + " " + urlstr, null);
        }
    }

    private String doc2String(Document doc) throws IOException {
        final StringWriter sw = new StringWriter(512);
        XMLSerializer xmlSerializer = new XMLSerializer();
        xmlSerializer.setOutputCharStream(sw);
        OutputFormat of = new OutputFormat();
        of.setIndent(4);
        xmlSerializer.setOutputFormat(of);
        xmlSerializer.serialize(doc);
        return sw.toString();
    }

    private void displayError(String msg, String title) {
        if (title == null) title = resources.getString("error.window.title");
        JOptionPane.showMessageDialog(this, msg, title, JOptionPane.ERROR_MESSAGE);
    }

    private JPanel constructCentralPanel() {
        // panel that contains the read from wsdl and the read from url
        JPanel toppanel = new JPanel();
        toppanel.setLayout(new BoxLayout(toppanel, BoxLayout.Y_AXIS));
        toppanel.add(constructDirectionPanel());
        toppanel.add(constructLoadFromUrlPanel());
        toppanel.add(loadFromFilePanel());
        toppanel.add(transformNamePanel());

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

    private JPanel transformNamePanel() {
        JPanel blah = new JPanel();
        blah.setLayout(new BorderLayout());
        blah.add(new JLabel(resources.getString("xmlTransformName.name") + " "), BorderLayout.WEST);
        blah.add(nameTxtFld, BorderLayout.CENTER);
        if (subject != null && subject.getTransformName() != null) {
            nameTxtFld.setText(subject.getTransformName());
        }

        // wrap this with border settings
        GridBagConstraints constraints = new GridBagConstraints();
        constraints.fill = GridBagConstraints.BOTH;
        constraints.weightx = 1.0;
        constraints.weighty = 1.0;
        constraints.insets = new Insets(BORDER_PADDING, BORDER_PADDING, BORDER_PADDING, BORDER_PADDING);
        JPanel bordered = new JPanel();
        bordered.setLayout(new GridBagLayout());
        bordered.add(blah, constraints);

        return bordered;
    }

    private JPanel loadFromFilePanel() {
        JPanel blah = new JPanel();
        blah.setLayout(new BorderLayout());
        blah.add(loadFromFile, BorderLayout.WEST);

        // wrap this with border settings
        GridBagConstraints constraints = new GridBagConstraints();
        constraints.fill = GridBagConstraints.BOTH;
        constraints.weightx = 1.0;
        constraints.weighty = 1.0;
        constraints.insets = new Insets(BORDER_PADDING, BORDER_PADDING, BORDER_PADDING, BORDER_PADDING);
        JPanel bordered = new JPanel();
        bordered.setLayout(new GridBagLayout());
        bordered.add(blah, constraints);

        return bordered;
    }

    private JPanel constructXmlDisplayPanel() {
        JPanel xmldisplayPanel = new JPanel();
        xmldisplayPanel.setLayout(new BorderLayout(0, CONTROL_SPACING));
        JLabel xmlTitle = new JLabel(resources.getString("xmldisplayPanel.name"));
        xmldisplayPanel.add(xmlTitle, BorderLayout.NORTH);

        if (subject != null && subject.getXslSrc() != null) {
            xmlTextArea.setText(reformatxml(subject.getXslSrc()));
        } /*else {
            okButton.setEnabled(false);
        }*/
        xmlTextArea.setCaretPosition(0);
        xmlTextArea.setCaretVisible(false);
        xmldisplayPanel.add(xmlTextArea, BorderLayout.CENTER);

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

    private JPanel constructDirectionPanel() {
        // align to the left
        JPanel loadfromwsdlpanel = new JPanel();
        loadfromwsdlpanel.setLayout(new FlowLayout(FlowLayout.LEADING, 0, 0));
        JLabel label = new JLabel(resources.getString("directionCombo.prefix") + "   ");
        loadfromwsdlpanel.add(label);
        loadfromwsdlpanel.add(directionCombo);
        if (subject != null) {
            if (subject.getDirection() == XslTransformation.APPLY_TO_RESPONSE) {
                directionCombo.setSelectedIndex(1);
            } else if (subject.getDirection() == XslTransformation.APPLY_TO_REQUEST) {
                directionCombo.setSelectedIndex(0);
            }
        }
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
        JLabel loadfromurllabel = new JLabel(resources.getString("loadFromUrlPanel.name"));
        loadFromUrlPanel.add(loadfromurllabel, BorderLayout.WEST);
        loadFromUrlPanel.add(urlTxtFld, BorderLayout.CENTER);
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

    private void allocControls() {
        // construct buttons
        helpButton = new JButton();
        helpButton.setText(resources.getString("helpButton.name"));
        okButton = new JButton();
        okButton.setText(resources.getString("okButton.name"));
        cancelButton = new JButton();
        cancelButton.setText(resources.getString("cancelButton.name"));
        directions = new String[]{resources.getString("directionCombo.request"),
                                  resources.getString("directionCombo.response")};
        directionCombo = new JComboBox(directions);
        urlTxtFld = new JTextField();
        nameTxtFld = new JTextField();
        resolveButton = new JButton();
        resolveButton.setText(resources.getString("resolveButton.name"));
        xmlTextArea = new JEditTextArea();
        attachMouseWhellSupport(xmlTextArea);
        xmlTextArea.setDocument(new SyntaxDocument());
        xmlTextArea.setEditable(true);
        xmlTextArea.setTokenMarker(new XMLTokenMarker());
        loadFromFile = new JButton();
        loadFromFile.setText(resources.getString("loadFromFile.name"));
    }

    /**
     * todo: Find some place for this. gui.Utilities is shared with ssb
     * @param xmlTextArea
     */
    private void attachMouseWhellSupport(JEditTextArea xmlTextArea) {
        xmlTextArea.addMouseWheelListener(new MouseWheelListener() {
            public void mouseWheelMoved(MouseWheelEvent e) {
                JEditTextArea textArea = (JEditTextArea)e.getSource();
                if (e.isAltDown()) {
                    moveCaret(textArea, e.getWheelRotation(),
                      e.isShiftDown() || e.isControlDown());
                } else if (e.isShiftDown())
                    scrollPage(textArea, e.getWheelRotation());
                else if (e.isControlDown())
                    scrollLine(textArea, e.getWheelRotation());
                else if (e.getScrollType() == MouseWheelEvent.WHEEL_UNIT_SCROLL)
                    scrollLine(textArea, e.getUnitsToScroll());
                else
                    scrollLine(textArea, 3 * e.getWheelRotation());
            }

            private void scrollLine(JEditTextArea textArea, int amt) {
                final int line = textArea.getFirstLine() + amt;
                if (line < 0 || line > textArea.getLineCount() - textArea.getVisibleLines()) return;
                textArea.setFirstLine(line);
            }

            private void scrollPage(JEditTextArea textArea, int amt) {
                if (amt > 0)
                    scrollDownPage(textArea);
                else
                    scrollUpPage(textArea);
            }

            private void scrollUpPage(JEditTextArea textArea) {
                final int line = textArea.getFirstLine() - textArea.getVisibleLines();
                if (line < textArea.getFirstLine()) return;
                textArea.setFirstLine(line);

            }

            private void scrollDownPage(JEditTextArea textArea) {
                final int line = textArea.getFirstLine() + textArea.getVisibleLines();
                if (line > textArea.getLineCount()) return;
                textArea.setFirstLine(line);
            }

            private void moveCaret(JEditTextArea textArea, int amt, boolean select) {
                if (amt < 0)
                    goToPrevLine(textArea, select);
                else
                    goToNextLine(textArea, select);
            }

            private void goToNextLine(JEditTextArea textArea, boolean select) {
                final int line = textArea.getFirstLine() + 1;
                if (line > textArea.getLineCount()) return;
                textArea.setFirstLine(line);

            }

            private void goToPrevLine(JEditTextArea textArea, boolean select) {
                final int line = textArea.getFirstLine() - 1;
                if (line < textArea.getFirstLine()) return;
                textArea.setFirstLine(line);
            }
        });
    }

    private void initResources() {
        Locale locale = Locale.getDefault();
        resources = ResourceBundle.getBundle("com.l7tech.console.resources.XslTransformationPropertiesDialog", locale);
    }

    public static void main(String[] args) {
        XslTransformationPropertiesDialog dlg = new XslTransformationPropertiesDialog(null, true, null);
        dlg.pack();
        dlg.show();
        System.exit(0);
    }

    private XslTransformation subject;

    private JButton helpButton;
    private JButton okButton;
    private JButton cancelButton;
    private JButton loadFromFile;
    private JComboBox directionCombo;
    private JButton resolveButton;
    private JTextField urlTxtFld;
    private JTextField nameTxtFld;
    private JEditTextArea xmlTextArea;
    private String[] directions;

    private ResourceBundle resources;

    private final Logger log = Logger.getLogger(getClass().getName());
    private final EventListenerList listenerList = new EventListenerList();

    private static int BORDER_PADDING = 20;
    private static int CONTROL_SPACING = 5;

    private static final String XSL_TOPEL_NAME = "transform";
    private static final String XSL_TOPEL_NAME2 = "stylesheet";
    private static final String XSL_NS = "http://www.w3.org/1999/XSL/Transform";
}
