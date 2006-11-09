package com.l7tech.console.panels;

import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.StringReader;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.rmi.RemoteException;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.security.AccessControlException;
import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.filechooser.FileFilter;
import javax.wsdl.WSDLException;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.SAXException;

import com.l7tech.common.gui.util.FontUtil;
import com.l7tech.common.gui.util.SwingWorker;
import com.l7tech.common.gui.util.Utilities;
import com.l7tech.common.util.ExceptionUtils;
import com.l7tech.common.util.ResourceUtils;
import com.l7tech.common.util.XmlUtil;
import com.l7tech.common.xml.Wsdl;
import com.l7tech.console.event.WsdlEvent;
import com.l7tech.console.event.WsdlListener;
import com.l7tech.console.util.Registry;
import com.l7tech.console.util.TopComponents;
import com.l7tech.objectmodel.FindException;

/**
 * Panel for use loading a Wsdl from a file or URL.
 *
 * <p>This panel manages the UDDI browser and File selection dialogs and also
 * resolution of a WSDL from a WSIL.</p>
 *
 * @author Steve Jones, $Author$
 * @version $Revision$
 */
public class WsdlLocationPanel extends JPanel {

    //- PUBLIC

    /**
     * Create a panel with the given owner and logger.
     *
     * @param owner the owner of this panel (used when creating other dialogs).
     * @param logger the logger to log to
     */
    public WsdlLocationPanel(JDialog owner, Logger logger, boolean enableFileSelection, boolean enableUddiSelection) {
        this.ownerd = owner;
        this.logger = logger;
        this.allowFile = enableFileSelection;
        this.allowUddi = enableUddiSelection;
        initComponents();
    }

    /**
     * Create a panel with the given owner and logger.
     *
     * @param owner the owner of this panel (used when creating other dialogs).
     * @param logger the logger to log to
     */
    public WsdlLocationPanel(JFrame owner, Logger logger, boolean enableFileSelection, boolean enableUddiSelection) {
        this.ownerf = owner;
        this.logger = logger;
        this.allowFile = enableFileSelection;
        this.allowUddi = enableUddiSelection;
        initComponents();
    }

    /**
     * Add a listener for URL / path changes (property name "wsdlUrl")
     *
     * @param listener The listener to add
     * @param name     The name (only "wsdlUrl" is allowed)
     */
    public void addPropertyListener(PropertyChangeListener listener, String name) {
        if (!"wsdlUrl".equals(name)) throw new IllegalArgumentException("No such property");
        if (pcl != null) throw new IllegalStateException("Multiple listeners not supported.");

        pcl = listener;
    }

    /**
     * Process the location and return the Wsdl.
     *
     * <p>This will notify the user of any processing error.</p>
     *
     * @return The Wsdl or null.
     */
    public Wsdl getWsdl() {
        return processWsdlLocation();
    }

    /**
     * Get the Document for the last Wsdl.
     *
     * @return The wsdl Document or null.
     */
    public Document getWsdlDocument() {
        return wsdlDocument;
    }

    /**
     * Get the WSDL URL.
     *
     * @return the URL / path.
     */
    public String getWsdlUrl() {
        return wsdlUrlTextField.getText();
    }

    /**
     * Set the WSDL URL.
     *
     * @return the URL / path.
     */
    public void setWsdlUrl(String urlOrPath) {
        wsdlUrlTextField.setText(urlOrPath);
    }

    /**
     * Check if the currently entered location is valid.
     *
     * @return true if valid
     */
    public boolean isLocationValid() {
        String location = wsdlUrlTextField.getText();
        return isUrlOk(location, null) || isFileOk(location);
    }

    //- PRIVATE

    private static final String EXAMPLE_PATH_NIX     = "   /opt/services/purchase.wsdl";
    private static final String EXAMPLE_PATH_WINDOWS = "   C:\\My Documents\\Services\\purchase.wsdl";

    private JDialog ownerd;
    private JFrame ownerf;
    private Logger logger;
    private Document wsdlDocument;
    private PropertyChangeListener pcl;
    private final boolean allowFile;
    private final boolean allowUddi;

    private JButton wsdlUrlBrowseButton;
    private JButton wsdlFileButton;
    private JPanel mainPanel;
    private JLabel exampleFileLabel;
    private JLabel exampleUrlLabel;
    private JTextField wsdlUrlTextField;

    /**
     * This method is called from within the constructor to initialize the form.
     */
    private void initComponents() {
        setLayout(new BorderLayout());
        add(mainPanel);

        // example labels
        FontUtil.resizeFont(exampleUrlLabel, 0.84);
        FontUtil.resizeFont(exampleFileLabel, 0.84);
        if (allowFile) {
            boolean isWindows = System.getProperty("os.name", "win").toLowerCase().indexOf("win") >= 0;
            exampleFileLabel.setText(isWindows ? EXAMPLE_PATH_WINDOWS : EXAMPLE_PATH_NIX);
        } else {
            exampleFileLabel.setText("");    
        }

        // url field
        wsdlUrlTextField.addMouseListener(Utilities.createContextMenuMouseListener(
                wsdlUrlTextField,
                new Utilities.DefaultContextMenuFactory()));
        wsdlUrlTextField.getDocument().addDocumentListener(createWsdlUrlDocumentListener());

        // buttons
        if (!allowUddi) wsdlUrlBrowseButton.setVisible(false);
        wsdlUrlBrowseButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                selectUddi();
            }
        });

        if (!allowFile) wsdlFileButton.setVisible(false);
        wsdlFileButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                selectFile();
            }
        });
    }

    private void selectUddi() {
        final Frame mainWindow = TopComponents.getInstance().getTopParent();

        try {
            // open UDDI browser
            SearchWsdlDialog swd = ownerd!=null ? new SearchWsdlDialog(ownerd) : new SearchWsdlDialog(ownerf);
            swd.addWsdlListener(new WsdlListener() {

                public void wsdlSelected(WsdlEvent event) {
                    String wsdlURL = event.getWsdlInfo().getWsdlUrl();

                    // update the wsdlUrlTestField
                    if(wsdlURL != null) wsdlUrlTextField.setText(wsdlURL);
                }
            });
            swd.setSize(700, 500);
            swd.setModal(true);
            swd.setVisible(true);
        } catch (RemoteException ex) {
            JOptionPane.showMessageDialog(mainWindow,
                    ex.getMessage(),
                    "Error",
                    JOptionPane.ERROR_MESSAGE);
        } catch (FindException ex) {
            JOptionPane.showMessageDialog(mainWindow,
                    ex.getMessage(),
                    "Error",
                    JOptionPane.ERROR_MESSAGE);
        }
    }

    private void selectFile() {
        final JFileChooser fc = Utilities.createJFileChooser();
        fc.setDialogTitle("Select WSDL or WSIL.");
        fc.setDialogType(JFileChooser.OPEN_DIALOG);
        FileFilter fileFilter = new FileFilter() {
            public boolean accept(File f) {
                return  f.isDirectory() ||
                        f.getName().toLowerCase().endsWith(".wsdl") ||
                        f.getName().toLowerCase().endsWith(".wsil");
            }
            public String getDescription() {
                return "(*.wsdl/*.wsil) Web Service description files.";
            }
        };
        fc.addChoosableFileFilter(fileFilter);
        fc.setMultiSelectionEnabled(false);
        int r = fc.showDialog(TopComponents.getInstance().getTopParent(), "Open");
        if(r == JFileChooser.APPROVE_OPTION) {
            File file = fc.getSelectedFile();
            if(file!=null) {
                wsdlUrlTextField.setText(file.getAbsolutePath());
            }
        }
    }

    private DocumentListener createWsdlUrlDocumentListener() {
        return new DocumentListener() {
            public void insertUpdate(DocumentEvent e) { urlChanged(); }
            public void removeUpdate(DocumentEvent e) { urlChanged(); }
            public void changedUpdate(DocumentEvent e) { urlChanged(); }
        };
    }

    private void urlChanged() {
        if (pcl != null)
            pcl.propertyChange(new PropertyChangeEvent(this, "wsdlUrl", getWsdlUrl(), getWsdlUrl()));
    }

    private boolean isUrlOk(String urlStr, String requiredProtocol) {
        boolean urlOk = false;
        try {
            URL url = new URL(urlStr);
            String urlProto = url.getProtocol();
            if (urlProto != null) {
                if ("http".equals(urlProto) ||
                    "https".equals(urlProto) ||
                    "file".equals(urlProto)) {
                    urlOk = requiredProtocol == null || requiredProtocol.equals(urlProto);
                }
            }
        } catch (MalformedURLException e) {
            //invalid url
        }

        return urlOk;
    }

    private boolean isFileOk(String filePath) {
        boolean isFile = false;

        File wsdlFile = new File(filePath);
        try {
            isFile = wsdlFile.isFile();
        } catch (AccessControlException e) {
            // We're probably running as an unsigned applet
            return false;
        }

        return isFile;
    }


    /**
     * Attempt to resolve the WSDL.  Returns true if we have a valid one, or false otherwise.
     * Will pester the user with a dialog box if the WSDL could not be fetched.
     *
     * @return true iff. the WSDL URL in the URL: text field was downloaded successfully.
     */
    private Wsdl processWsdlLocation() {
        urlChanged();

        final String wsdlUrl = wsdlUrlTextField.getText();
        try {

            Object result = null;
            if (isUrlOk(wsdlUrl, null) && !isUrlOk(wsdlUrl, "file")) { // then it is http or https so the Gateway should resolve it
                result = gatewayFetchWsdlUrl(wsdlUrl);
            }
            else { // it is a file url or
                result = readFile(wsdlUrl);
            }
            if (result == null)
                // canceled
                return null;
            if (!(result instanceof String) && !(result instanceof Document)) {
                String msg = "Unable to parse the WSDL at location '" + wsdlUrlTextField.getText() + "'";
                if (result instanceof Throwable) {
                    Throwable t = (Throwable)result;
                    logger.log(Level.WARNING, msg, (Throwable)result);
                    msg = "Unable to parse the WSDL at this location: \n" + ExceptionUtils.getMessage(t);
                }
                final Frame mainWindow = TopComponents.getInstance().getTopParent();
                JOptionPane.showMessageDialog(mainWindow,
                                              msg + "\n",
                                              "Error",
                                              JOptionPane.ERROR_MESSAGE);
                return null;
            }


            Document resolvedDoc = null;

            if (result instanceof String) {
                // this can be either a WSDL or a WSIL document
                String xmlResult = (String) result;
                resolvedDoc = XmlUtil.stringToDocument(xmlResult);
            }
            else {
                resolvedDoc = (Document) result;
            }

            // is this a WSIL?
            Element root = resolvedDoc.getDocumentElement();
            if (root.getLocalName().equals("inspection") &&
                root.getNamespaceURI().equals("http://schemas.xmlsoap.org/ws/2001/10/inspection/")) {
                // parse wsil and choose the wsdl url
                WSILSelectorPanel chooser = ownerd!=null ? new WSILSelectorPanel(ownerd, resolvedDoc) : new WSILSelectorPanel(ownerf, resolvedDoc);
                chooser.pack();
                Utilities.centerOnScreen(chooser);
                chooser.setVisible(true);
                if (!chooser.wasCancelled() && chooser.selectedWSDLURL() != null) {
                    String chooserUrlStr = chooser.selectedWSDLURL();
                    // If previous url contained userinfo stuff but the wsil target does
                    // not, modify new url so the userinfo is added
                    //
                    if (wsdlUrl.startsWith("http")) {
                        URL currentUrl = new URL(wsdlUrl);
                        URL newUrl = new URL(chooserUrlStr);
                        if (newUrl.getUserInfo() == null && currentUrl.getUserInfo() != null) {
                            StringBuffer combinedurl = new StringBuffer(newUrl.toString());
                            combinedurl.insert(newUrl.getProtocol().length()+3, currentUrl.getUserInfo() + "@");
                            chooserUrlStr = new URL(combinedurl.toString()).toString();
                        }
                    }
                    wsdlUrlTextField.setText(chooserUrlStr);
                    return processWsdlLocation();
                }
            } else {
                String baseUri = Wsdl.extractBaseURI(wsdlUrl);
                if (!wsdlUrl.startsWith("http")) {
                    baseUri = "local file"; // looks odd but makes the error messages nicer ...
                }
                Wsdl wsdl = Wsdl.newInstance(baseUri, new StringReader(XmlUtil.nodeToString(resolvedDoc)), false);
                wsdlDocument = resolvedDoc;
                return wsdl;
            }
        } catch (WSDLException e1) {
            logger.log(Level.INFO, "Could not parse WSDL.", e1); // this used to do e.printStackTrace() this is slightly better.
            String message = ExceptionUtils.getMessage(e1);
            Pattern messageCleanup = Pattern.compile("WSDLException(?: \\(at [a-zA-Z0-9_\\-:/]{0,1024}\\)){0,1}: faultCode=[a-zA-Z0-9_\\-]{0,256}: (.*)");
            Matcher messageMatcher = messageCleanup.matcher(message);
            if (messageMatcher.matches()) {
                message = messageMatcher.group(1);
            }
            JOptionPane.showMessageDialog(null,
              "Unable to parse the WSDL at location '" + wsdlUrl + "'\nError detail: " + message + "\n",
              "Error",
              JOptionPane.ERROR_MESSAGE);
        } catch (MalformedURLException e1) {
            logger.log(Level.INFO, "Could not parse URL.", e1); // this used to do e.printStackTrace() this is slightly better.
            JOptionPane.showMessageDialog(null,
              "Illegal URL string '" + wsdlUrl + "'\n",
              "Error",
              JOptionPane.ERROR_MESSAGE);
        } catch (IOException e1) {
            logger.log(Level.INFO, "IO Error.", e1); // this used to do e.printStackTrace() this is slightly better.
            JOptionPane.showMessageDialog(null,
                                          "Unable to parse the WSDL at location '" + wsdlUrlTextField.getText() +
                                          "'\n",
                                          "Error",
                                          JOptionPane.ERROR_MESSAGE);
        } catch (SAXException e1) {
            logger.log(Level.INFO, "XML parsing error.", e1); // this used to do e.printStackTrace() this is slightly better.
            JOptionPane.showMessageDialog(null,
                                          "Unable to parse the WSDL at location '" + wsdlUrlTextField.getText() +
                                          "'\n",
                                          "Error",
                                          JOptionPane.ERROR_MESSAGE);
        }

        return null;
    }

    private Object gatewayFetchWsdlUrl(final String wsdlUrl) {
        Dialog rootDialog = (Dialog) SwingUtilities.getWindowAncestor(this);
        final CancelableOperationDialog dlg = new CancelableOperationDialog(rootDialog,
                                                                            "Resolving target",
                                                                            "Please wait, resolving target...");
        SwingWorker worker = new SwingWorker() {
            public Object construct() {
                try {
                    return Registry.getDefault().getServiceManager().resolveWsdlTarget(wsdlUrl);
                } catch (RemoteException e) {
                    return e;
                } catch (MalformedURLException e) {
                    return e;
                } catch (IOException e) {
                    return e;
                } catch (Exception e) {
                    return e;
                }
            }

            public void finished() {
                dlg.setVisible(false);
            }
        };
        worker.start();
        dlg.setVisible(true);
        worker.interrupt();
        return worker.get();
    }

    /**
     * Returns a Document or an Exception
     */
    private Object readFile(final String wsdlUrl) {
        File wsdlFile = null;
        FileInputStream fin = null;
        try {
            if (isUrlOk(wsdlUrl, "file")) {
                wsdlFile = new File(new URI(wsdlUrl));
            }
            else {
                wsdlFile = new File(wsdlUrl);
            }

            if (wsdlFile.length() > 8000000) throw new IllegalStateException("File is too large.");

            fin = new FileInputStream(wsdlFile);
            return XmlUtil.parse(fin, true);
        }
        catch(Exception e) {
            return e;
        }
        finally {
            ResourceUtils.closeQuietly(fin);
        }
    }

}
