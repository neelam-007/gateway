package com.l7tech.console.panels;

import com.l7tech.common.gui.util.*;
import com.l7tech.common.gui.util.SwingWorker;
import com.l7tech.common.io.IOExceptionThrowingReader;
import com.l7tech.common.util.*;
import com.l7tech.common.xml.DocumentReferenceProcessor;
import com.l7tech.common.xml.Wsdl;
import com.l7tech.console.SsmApplication;
import com.l7tech.console.event.WsdlEvent;
import com.l7tech.console.event.WsdlListener;
import com.l7tech.console.util.Registry;
import com.l7tech.console.util.TopComponents;
import com.l7tech.console.util.WsdlUtils;
import com.l7tech.objectmodel.FindException;
import com.l7tech.service.ServiceAdmin;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.filechooser.FileFilter;
import javax.wsdl.WSDLException;
import javax.wsdl.xml.WSDLLocator;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.*;
import java.lang.reflect.InvocationTargetException;
import java.net.*;
import java.security.AccessControlException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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

    public static final String SYSPROP_NO_WSDL_IMPORTS = "com.l7tech.console.noWsdlImports";

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
     * Get the total number of WSDL documents.
     *
     * <p>This includes the main document and any imports.</p>
     *
     * @return The count.
     */
    public int getWsdlCount() {
        return wsdlResources==null ? 0 : wsdlResources.size();
    }

    /**
     * Get the URI for the WSDL at the given index.
     *
     * <p>Index is zero based, with zero being the top-level WSDL.</p>
     *
     * @param index The index of the wsdl.
     * @return The URI.
     */
    public String getWsdlUri(int index) {
        return wsdlResources.toArray(new ResourceTrackingWSDLLocator.WSDLResource[wsdlResources.size()])[index].getUri();
    }

    /**
     * Get the content for the WSDL at the given index.
     *
     * <p>Index is zero based, with zero being the top-level WSDL.</p>
     *
     * @param index The index of the wsdl.
     * @return The wsdl document.
     */
    public String getWsdlContent(int index) {
        return wsdlResources.toArray(new ResourceTrackingWSDLLocator.WSDLResource[wsdlResources.size()])[index].getWsdl();
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
     * @param urlOrPath the URL / path.
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
    private Collection<ResourceTrackingWSDLLocator.WSDLResource> wsdlResources;
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
            DialogDisplayer.display(swd);
        } catch (FindException ex) {
            JOptionPane.showMessageDialog(mainWindow,
                    ex.getMessage(),
                    "Error",
                    JOptionPane.ERROR_MESSAGE);
        }
    }

    private void selectFile() {
        SsmApplication.doWithJFileChooser(new FileChooserUtil.FileChooserUser() {
            public void useFileChooser(JFileChooser fc) {
                doSelectFile(fc);
            }
        });
    }

    private void doSelectFile(JFileChooser fc) {
        fc.setDialogTitle("Select WSDL or WSIL.");
        fc.setDialogType(JFileChooser.OPEN_DIALOG);
        FileFilter fileFilter = new FileFilter() {
            @Override
            public boolean accept(File f) {
                return  f.isDirectory() ||
                        f.getName().toLowerCase().endsWith(".wsdl") ||
                        f.getName().toLowerCase().endsWith(".wsil");
            }
            @Override
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
        boolean isFile;

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
        WSDLLocator locator;
        if (isUrlOk(wsdlUrl, null) && !isUrlOk(wsdlUrl, "file")) { // then it is http or https so the Gateway should resolve it
            locator = gatewayHttpWSDLLocator(wsdlUrl);
        }
        else { // it is a file url or is invalid
           locator = fileWSDLLocator(wsdlUrl);
        }

        // WSDLLocator may point to a WSIL document
        final boolean[] reprocess =  new boolean[1];
        final WSDLLocator wsdlLocator = locator;
        final CancelableOperationDialog dlg =
                CancelableOperationDialog.newCancelableOperationDialog(this, "Resolving target", "Please wait, resolving target...");

        SwingWorker worker = new SwingWorker() {
            @Override
            public Object construct() {
                try {
                    new URI(wsdlLocator.getBaseURI()); // ensure valid url
                    final Document resolvedDoc = XmlUtil.parse(wsdlLocator.getBaseInputSource(), true);

                    // is this a WSIL?
                    Element root = resolvedDoc.getDocumentElement();
                    if (root.getLocalName().equals("inspection") &&
                        root.getNamespaceURI().equals("http://schemas.xmlsoap.org/ws/2001/10/inspection/")) {

                        try {
                            SwingUtilities.invokeAndWait(new Runnable(){
                                public void run() {
                                    // hide cancel dialog
                                    dlg.setVisible(false);

                                    // parse wsil and choose the wsdl url
                                    WSILSelectorPanel chooser = ownerd!=null ?
                                            new WSILSelectorPanel(ownerd, resolvedDoc) :
                                            new WSILSelectorPanel(ownerf, resolvedDoc);

                                    chooser.pack();
                                    Utilities.centerOnScreen(chooser);
                                    chooser.setVisible(true); // TODO change to use DialogDisplayer
                                    if (!chooser.wasCancelled() && chooser.selectedWSDLURL() != null) {
                                        String chooserUrlStr = chooser.selectedWSDLURL();
                                        // If previous url contained userinfo stuff but the wsil target does
                                        // not, modify new url so the userinfo is added
                                        //
                                        if (wsdlUrl.startsWith("http")) {
                                            try {
                                                URL currentUrl = new URL(wsdlUrl);
                                                URL newUrl = new URL(chooserUrlStr);
                                                if (newUrl.getUserInfo() == null && currentUrl.getUserInfo() != null) {
                                                    StringBuffer combinedurl = new StringBuffer(newUrl.toString());
                                                    combinedurl.insert(newUrl.getProtocol().length()+3, currentUrl.getUserInfo() + "@");
                                                    chooserUrlStr = new URL(combinedurl.toString()).toString();
                                                }
                                            }
                                            catch(MalformedURLException murle) {
                                                throw new RuntimeException(murle);
                                            }
                                        }
                                        wsdlUrlTextField.setText(chooserUrlStr);
                                        reprocess[0] = true;
                                    }
                                }
                            });
                        }
                        catch(InterruptedException ie) {
                            return null;
                        }
                        catch(InvocationTargetException ite) {
                            throw new CausedIOException(ite.getCause());
                        }
                    } else {
                        String baseUri = wsdlUrl;
                        if (!isUrlOk(baseUri, null)) {
                            baseUri = new File(baseUri).toURI().toString();
                        }
                        Wsdl wsdl;
                        if (SyspropUtil.getBoolean(SYSPROP_NO_WSDL_IMPORTS)) {
                            // Old technique, don't process imports correctly
                            String wsdlStr = XmlUtil.nodeToString(resolvedDoc);
                            wsdl = Wsdl.newInstance(WsdlUtils.getWSDLFactory(), baseUri, new StringReader(wsdlStr), false);
                            wsdlDocument = resolvedDoc;
                            wsdlResources = new ArrayList<ResourceTrackingWSDLLocator.WSDLResource>();
                            wsdlResources.add(new ResourceTrackingWSDLLocator.WSDLResource(baseUri, "text/xml", wsdlStr));
                        } else {
                            // New technique, process imports via SSG and save them for later
                            String baseDoc = XmlUtil.nodeToString(resolvedDoc);
                            DocumentReferenceProcessor processor = new DocumentReferenceProcessor();
                            Map<String,String> urisToResources =
                                    processor.processDocument( baseUri, new GatewayResourceResolver(logger, baseUri, baseDoc) );

                            wsdl = Wsdl.newInstance(WsdlUtils.getWSDLFactory(), Wsdl.getWSDLLocator(baseUri, urisToResources, logger));

                            Collection<ResourceTrackingWSDLLocator.WSDLResource> wsdls = ResourceTrackingWSDLLocator.toWSDLResources(baseUri, urisToResources, true, false, false);

                            // parse in a way that sets the base URI for the document
                            InputSource parseSource = new InputSource(baseUri);
                            parseSource.setCharacterStream( new StringReader(baseDoc) );
                            wsdlDocument = resolvedDoc;
                            wsdlResources = wsdls;
                        }
                        return wsdl;
                    }
                } catch (WsdlUtils.WSDLFactoryNotTrustedException wfnte) {
                    SwingUtilities.invokeLater(new Runnable(){
                        public void run() {
                            if(dlg.isVisible()) {
                                dlg.setVisible(false);
                                TopComponents.getInstance().showNoPrivilegesErrorMessage();
                            }
                        }
                    });
                } catch (final WSDLException e1) {
                    SwingUtilities.invokeLater(new Runnable(){
                        public void run() {
                            if(dlg.isVisible()) {
                                dlg.setVisible(false);
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
                            }
                        }
                    });
                } catch (final MalformedURLException e1) {
                    SwingUtilities.invokeLater(new Runnable(){
                        public void run() {
                            if(dlg.isVisible()) {
                                logger.log(Level.INFO, "Could not parse URL.", e1);
                                dlg.setVisible(false);
                                JOptionPane.showMessageDialog(null,
                                                              "Illegal URL string '" + wsdlUrl + "'\n",
                                                              "Error",
                                                              JOptionPane.ERROR_MESSAGE);
                            }
                        }
                    });
                } catch (final URISyntaxException e1) {
                    SwingUtilities.invokeLater(new Runnable(){
                        public void run() {
                            if(dlg.isVisible()) {
                                logger.log(Level.INFO, "Could not parse URL.", e1);
                                dlg.setVisible(false);
                                JOptionPane.showMessageDialog(null,
                                                              "Illegal URL string '" + wsdlUrl + "'\n",
                                                              "Error",
                                                              JOptionPane.ERROR_MESSAGE);
                            }
                        }
                    });
                } catch (final FileNotFoundException e1) {
                    SwingUtilities.invokeLater(new Runnable(){
                        public void run() {
                            if(dlg.isVisible()) {
                                logger.log(Level.INFO, "IO Error.", e1);
                                dlg.setVisible(false);
                                JOptionPane.showMessageDialog(null,
                                                              "Could not parse the WSDL at location '" + wsdlUrlTextField.getText() +
                                                              "':\n  '" + e1.getMessage() +"'",
                                                              "Error",
                                                              JOptionPane.ERROR_MESSAGE);
                            }
                        }
                    });
                } catch (final IOException e1) {
                    SwingUtilities.invokeLater(new Runnable(){
                        public void run() {
                            if(dlg.isVisible()) {
                                logger.log(Level.INFO, "IO Error.", e1);
                                dlg.setVisible(false);
                                if (ExceptionUtils.causedBy(e1, SocketException.class)) {
                                    JOptionPane.showMessageDialog(null,
                                                                  "Could not fetch the WSDL at location '" + wsdlUrlTextField.getText() +
                                                                  "'\n",
                                                                  "Error",
                                                                  JOptionPane.ERROR_MESSAGE);
                                } else {
                                    JOptionPane.showMessageDialog(null,
                                                                  "Unable to parse the WSDL at location '" + wsdlUrlTextField.getText() +
                                                                  "'\n",
                                                                  "Error",
                                                                  JOptionPane.ERROR_MESSAGE);
                                }
                            }
                        }
                    });
                } catch (final SAXException e1) {
                    SwingUtilities.invokeLater(new Runnable(){
                        public void run() {
                            if(dlg.isVisible()) {
                                logger.log(Level.INFO, "XML parsing error.", e1);
                                dlg.setVisible(false);
                                JOptionPane.showMessageDialog(null,
                                                              "Unable to parse the WSDL at location '" + wsdlUrlTextField.getText() +
                                                              "'\n",
                                                              "Error",
                                                              JOptionPane.ERROR_MESSAGE);
                            }
                        }
                    });
                }

                return null;
            }

            @Override
            public void finished() {
                dlg.setVisible(false);
            }
        };

        worker.start();
        dlg.setVisible(true);
        if (reprocess[0]) {
            return processWsdlLocation();
        } else if (dlg.wasCancelled()) {
            worker.interrupt(); // cancel
            return null;
        }

        return (Wsdl) worker.get();
    }

    private static String gatewayFetchWsdlUrl(final String wsdlUrl) throws IOException {
        ServiceAdmin manager = Registry.getDefault().getServiceManager();

        if (manager == null)
            throw new IOException("Service not available.");

        return manager.resolveWsdlTarget(wsdlUrl);
    }

    /**
     * Get a WSDLLocator suitable for retrieval of a single document (will not work for includes)
     */
    private WSDLLocator gatewayHttpWSDLLocator(final String wsdlUrl) {
        InputSource is = new InputSource() {
            @Override
            public Reader getCharacterStream() {
                try {
                    return new StringReader(gatewayFetchWsdlUrl(wsdlUrl));
                }
                catch(IOException ioe) {
                    return new IOExceptionThrowingReader(ioe);
                }
            }

            @Override
            public String getSystemId() {
                return wsdlUrl;
            }
        };

        return Wsdl.getWSDLLocator(is, false);
    }

    /**
     * Get a WSDLLocator suitable for retrieval of a single document (will not work for includes)
     */
    private WSDLLocator fileWSDLLocator(final String wsdlUrl) {
        FileInputStream fin = null;
        InputSource is = new InputSource();
        try {
            File wsdlFile;
            if (isUrlOk(wsdlUrl, "file")) {
                wsdlFile = new File(new URI(wsdlUrl));
            }
            else {
                wsdlFile = new File(wsdlUrl);
            }

            if (wsdlFile.length() > 8000000) throw new IOException("File is too large.");

            is.setSystemId(wsdlFile.toURI().toString());
            fin = new FileInputStream(wsdlFile);
            is.setByteStream(new ByteArrayInputStream(HexUtils.slurpStream(fin, 8000000)));
        }
        catch (IOException ioe) {
            is.setCharacterStream(new IOExceptionThrowingReader(ioe, false));
        }
        catch (URISyntaxException uise) {
            //noinspection ThrowableInstanceNeverThrown
            is.setCharacterStream(new IOExceptionThrowingReader(new CausedIOException(uise), false));
        }
        finally {
            ResourceUtils.closeQuietly(fin);
        }

        return Wsdl.getWSDLLocator(is, false);
    }


    /**
     * WSDLLocator that accesses HTTP URI's via the gateway.
     */
    private static class GatewayResourceResolver implements DocumentReferenceProcessor.ResourceResolver {
        private final Logger logger;
        private final String baseUri;
        private final String baseResource;

        private GatewayResourceResolver(final Logger logger, final String baseUri, final String baseResource ) {
            this.logger = logger;
            this.baseUri = baseUri;
            this.baseResource = baseResource;
        }

        public String resolve( final String importLocation) throws IOException {
            String resource = null;

            logger.log(Level.INFO, "Processing import from location '" + importLocation + "'.");


            if ( baseUri.equals(importLocation) ) {
                resource = ResourceTrackingWSDLLocator.processResource(baseUri, baseResource, false, true);
            } else {
                if ( importLocation.startsWith("http") ) {
                    resource = ResourceTrackingWSDLLocator.processResource(importLocation, gatewayFetchWsdlUrl(importLocation), false, true);
                }
            }

            if ( resource == null) {
                throw new FileNotFoundException("Resource not found '"+importLocation+"'.");
            }

            return resource;
        }
    }

}
