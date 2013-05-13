package com.l7tech.console.panels;

import com.japisoft.xmlpad.UIAccessibility;
import com.japisoft.xmlpad.XMLContainer;
import com.l7tech.common.io.SchemaUtil;
import com.l7tech.common.io.XmlUtil;
import com.l7tech.console.SsmApplication;
import com.l7tech.console.action.Actions;
import com.l7tech.console.util.Registry;
import com.l7tech.console.util.SecurityZoneWidget;
import com.l7tech.console.util.XMLContainerFactory;
import com.l7tech.gateway.common.resources.ResourceAdmin;
import com.l7tech.gateway.common.resources.ResourceEntry;
import com.l7tech.gateway.common.resources.ResourceType;
import com.l7tech.gateway.common.security.rbac.OperationType;
import com.l7tech.gui.util.DialogDisplayer;
import com.l7tech.gui.util.FileChooserUtil;
import com.l7tech.gui.util.Utilities;
import com.l7tech.gui.widgets.OkCancelDialog;
import com.l7tech.objectmodel.EntityType;
import com.l7tech.objectmodel.EntityUtil;
import com.l7tech.common.io.DtdUtils;
import com.l7tech.util.ExceptionUtils;
import com.l7tech.util.IOUtils;
import com.l7tech.util.TextUtils;
import com.l7tech.util.ValidationUtils;
import org.w3c.dom.Document;
import org.xml.sax.EntityResolver;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.io.IOException;
import java.io.StringReader;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Editor for XML and text resource types.
 */
public class ResourceEntryEditor extends JDialog {
    private final Logger logger = Logger.getLogger(ResourceEntryEditor.class.getName());

    private JPanel xmlDisplayPanel;
    private JTextField systemIdTextField;
    private JButton helpButton;
    private JButton cancelButton;
    private JButton okButton;
    private JPanel mainPanel;
    private JButton uploadFromURLBut;
    private JButton uploadFromFileBut;
    private JTextField publicIdTextField;
    private JLabel publicIdLabel;
    private JTextArea contentTextArea;
    private JTextField descriptionTextField;
    private JLabel descriptionLabel;
    private SecurityZoneWidget zoneControl;

    private XMLContainer xmlContainer;
    private UIAccessibility uiAccessibility;

    private final ResourceEntry resourceEntry;
    private final EntityResolver entityResolver;
    private final boolean contentOnly;
    private final boolean canEdit;
    private final boolean warnForDoctype;
    private boolean dataLoaded = false;
    private boolean success = false;

    public ResourceEntryEditor( final Window owner,
                                final ResourceEntry resourceEntry,
                                final EntityResolver entityResolver,
                                final boolean contentOnly,
                                final boolean canEditEntry,
                                final boolean warnForDoctype ) {
        super( owner, JDialog.DEFAULT_MODALITY_TYPE );
        this.resourceEntry = resourceEntry;
        this.entityResolver = entityResolver;
        this.contentOnly = contentOnly;
        this.canEdit = canEditEntry;
        this.warnForDoctype = warnForDoctype;
        initialize();
        DialogDisplayer.suppressSheetDisplay(this); // incompatible with xml pad
    }

    public boolean wasOk() {
        return success;
    }

    private void initialize() {
        setContentPane(mainPanel);
        setTitle(canEdit?"Edit Resource":"View Resource");

        if ( resourceEntry.getType() == ResourceType.XML_SCHEMA ) {
            // set xml view
            xmlContainer = XMLContainerFactory.createXmlContainer(true);
            uiAccessibility = xmlContainer.getUIAccessibility();

            xmlDisplayPanel.removeAll();
            xmlDisplayPanel.setLayout(new BorderLayout());
            final JComponent view = xmlContainer.getView();
            view.setMinimumSize( new Dimension( 200, 100 ) );
            xmlDisplayPanel.add(view, BorderLayout.CENTER);
        }

        // button callbacks
        helpButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                help();
            }
        });
        cancelButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                cancel();
            }
        });
        okButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                ok();
            }
        });

        uploadFromURLBut.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                uploadFromURL();
            }
        });

        uploadFromFileBut.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                uploadFromFile();
            }
        });

        Utilities.setMaxLength( systemIdTextField.getDocument(), EntityUtil.getMaxFieldLength(ResourceEntry.class, "uri", 4096));
        Utilities.setMaxLength( publicIdTextField.getDocument(), EntityUtil.getMaxFieldLength(ResourceEntry.class, "resourceKey1", 4096));
        Utilities.setMaxLength( descriptionTextField.getDocument(), EntityUtil.getMaxFieldLength(ResourceEntry.class, "description", 255));

        // support Enter and Esc keys
        Utilities.setEscAction(this, cancelButton);
        getRootPane().setDefaultButton(okButton);
        Utilities.setButtonAccelerator(this, helpButton, KeyEvent.VK_F1);
        setMinimumSize( getContentPane().getMinimumSize() );

        zoneControl.configure(EntityType.RESOURCE_ENTRY,
                resourceEntry.getOid() == ResourceEntry.DEFAULT_OID ? OperationType.CREATE : canEdit ? OperationType.UPDATE : OperationType.READ,
                resourceEntry.getSecurityZone());

        enableDisableComponents();

        this.addWindowListener(new WindowAdapter() {
            @Override
            public void windowOpened( WindowEvent e) {
                if (!dataLoaded ) {
                    resetData();
                }
            }
        });

        pack();
        Utilities.centerOnParentWindow( this );
    }

    private void setResourceContents( final String content ) {
        if ( uiAccessibility != null ) {
            uiAccessibility.getEditor().setText(content);
            uiAccessibility.getEditor().setLineNumber(1);
        } else {
            contentTextArea.setText( content );
            contentTextArea.setCaretPosition( 0 );
        }
    }

    private String getResourceContents() {
        return uiAccessibility == null ? contentTextArea.getText() : uiAccessibility.getEditor().getText();
    }

    private void uploadFromURL() {
        final OkCancelDialog dlg = new OkCancelDialog<String>(this, "Load Resource From URL",
                                                true, new UrlPanel("Enter the URL for the resource", null));
        dlg.pack();
        Utilities.centerOnScreen(dlg);
        DialogDisplayer.display(dlg, new Runnable() {
            @Override
            public void run() {
                String url = (String)dlg.getValue();
                if (url != null) {
                    readFromUrl(url);
                }
            }
        });
    }

    private void readFromUrl( final String url ) {
        if (url == null || url.length() < 1) {
            displayError("A URL was not provided", null);
            return;
        }

        // compose input source
        try {
            new URL(url);
        } catch ( MalformedURLException e) {
            displayError(url + " is not a valid url", null);
            return;
        }

        final ResourceAdmin resourceAdmin;
        final Registry reg = Registry.getDefault();
        if (reg == null || reg.getResourceAdmin() == null) {
            throw new RuntimeException("No access to registry. Cannot download resource.");
        } else {
            resourceAdmin = reg.getResourceAdmin();
        }

        final String resourceContent;
        try {
            resourceContent = resourceAdmin.resolveResource(url);
        } catch ( IOException e) {
            //this is likely to be a GenericHttpException
            final String errorMsg = "Cannot download resource: " + ExceptionUtils.getMessage(e);
            displayError(errorMsg, "Error Downloading Resource");
            return;
        }

        boolean contentUpdated = false;
        if ( resourceEntry.getType() == ResourceType.XML_SCHEMA ) {
            Document doc;
            try {
                doc = XmlUtil.parse(new InputSource(new StringReader(resourceContent)), entityResolver);
            } catch ( SAXException e) {
                displayError("Error parsing schema from " + TextUtils.truncateStringAtEnd( url, 128 ) + " :\n" +
                        ExceptionUtils.getMessage( e ), null);
                return;
            } catch ( IOException e ) {
                displayError("Error processing schema from " + TextUtils.truncateStringAtEnd( url, 128 ) + " :\n" +
                        ExceptionUtils.getMessage( e ), null);
                return;
            }

            // check if it's a schema
            if (docIsSchema(doc)) {
                // set the new schema
                String printedSchema;
                try {
                    printedSchema = XmlUtil.nodeToFormattedString(doc);
                } catch (IOException e) {
                    String msg = "error serializing document";
                    displayError(msg, null);
                    return;
                }
                setResourceContents(printedSchema);
                contentUpdated = true;
            } else {
                displayError("No XML Schema could be parsed from " + url, null);
            }
        } else {
            setResourceContents(resourceContent);
            contentUpdated = true;
        }

        if ( contentUpdated ) {
            perhapsSetSystemId( url );
        }
    }

    private boolean docIsSchema(Document doc) {
        return SchemaUtil.isSchema( doc );
    }

    private void displayError( final String msg,
                               String title) {
        if (title == null) title = "Error";

        final int width = Utilities.computeStringWidth(this.getFontMetrics(this.getFont()), msg);
        final Object messageObject;
        if(width > 600){
            messageObject = Utilities.getTextDisplayComponent(msg, 600, 100, -1, -1);
        }else{
            messageObject = msg;
        }

        JOptionPane.showMessageDialog(
                this,
                messageObject,
                title,
                JOptionPane.ERROR_MESSAGE);
    }

    private void uploadFromFile() {
        SsmApplication.doWithJFileChooser(new FileChooserUtil.FileChooserUser() {
            @Override
            public void useFileChooser(JFileChooser fc) {
                doUpload(fc);
            }
        });
    }

    private void doUpload( final JFileChooser dlg ) {
        if (JFileChooser.APPROVE_OPTION != dlg.showOpenDialog(this)) {
            return;
        }

        final File file = dlg.getSelectedFile();
        final String filename = file.getAbsolutePath();
        boolean contentUpdated = false;
        final String content;
        try {
            final byte[] data = IOUtils.slurpFile( file );
            final String charset = XmlUtil.getEncoding( data );
            content = new String( data, charset );
        } catch (IOException e) {
            displayError("Unable to read resource from:\n" + filename + "\n" + ExceptionUtils.getMessage( e ), "Error Accessing Resource");
            logger.log(Level.FINE, "Error reading resource " + filename, e);
            return;
        }

        if ( resourceEntry.getType() == ResourceType.XML_SCHEMA ) {
            // try to get document
            Document doc;
            try {
                InputSource inputSource = new InputSource();
                inputSource.setSystemId( file.toURI().toString() );
                inputSource.setCharacterStream( new StringReader( content ) );
                doc = XmlUtil.parse(inputSource, entityResolver);
            } catch (SAXException e) {
                displayError("Error parsing schema from " + filename + ":\n" + ExceptionUtils.getMessage( e ), null);
                logger.log(Level.FINE, "cannot parse " + filename, e);
                return;
            } catch (IOException e) {
                displayError("Error processing schema from " + filename + ":\n" + ExceptionUtils.getMessage( e ), null);
                logger.log(Level.FINE, "cannot parse " + filename, e);
                return;
            }

            // check if it's a schema
            if (docIsSchema(doc)) {
                // set the new schema
                String printedSchema;
                try {
                    printedSchema = XmlUtil.nodeToFormattedString(doc);
                } catch (IOException e) {
                    String msg = "error serializing document";
                    displayError(msg, null);
                    logger.log(Level.FINE, msg, e);
                    return;
                }
                setResourceContents(printedSchema);
                contentUpdated = true;
            } else {
                displayError("An XML Schema could not be read from " + filename, null);
            }
        } else {
            setResourceContents( content );
            contentUpdated = true;
        }

        if ( contentUpdated ) {
            final String systemId = dlg.getSelectedFile().toURI().toString();
            perhapsSetSystemId( systemId );
        }
    }

    /**
     * Set the system identifier in the UI if there is not currently a value set
     */
    private void perhapsSetSystemId( final String systemId ) {
        if ( systemIdTextField.getText() == null || systemIdTextField.getText().length() < 1 ) {
            systemIdTextField.setText(systemId);
            systemIdTextField.setCaretPosition(0);
        }
    }

    private void help() {
        Actions.invokeHelp(this);
    }

    private void cancel() {
        dispose();
    }

    private void ok() {
        if (canEdit) {
            final String contents = getResourceContents();

            // Validate system identifier
            final String systemId = systemIdTextField.getText();
            if (systemId == null || systemId.trim().length() < 1) {
                displayError( "You must enter a System ID for the resource.", "Invalid Resource" );
                return;
            }

            try {
                final URI uri = new URI(systemId);
                if ( uri.getScheme() == null ) {
                    // Nag user to fix the URI
                    int option = JOptionPane.showConfirmDialog(this,
                                                        "This resource has a relative System ID (URI), using absolute URIs is\n" +
                                                        "recommended. An absolute URI starts with a scheme, for example 'http:'.\n\n" +
                                                        "A relative System ID is more likely to conflict with other resources\n" +
                                                        "and can cause resolution failures.",
                                                        "Resource Warning",
                                                        JOptionPane.OK_CANCEL_OPTION,
                                                        JOptionPane.WARNING_MESSAGE);
                    if ( option == JOptionPane.CANCEL_OPTION ) {
                        return;
                    }
                }
            } catch ( URISyntaxException e ) {
                displayError( "Invalid System ID:\n" + ExceptionUtils.getMessage( e ), "Invalid Resource" );
                return;
            }

            // save it
            if ( (resourceEntry.getType() == ResourceType.DTD && setDTDProperties( resourceEntry )) ||
                 (resourceEntry.getType() == ResourceType.XML_SCHEMA && setSchemaProperties( resourceEntry, systemId.trim(), contents )) ) {
                resourceEntry.setUri( systemId.trim() );
                if ( descriptionTextField.getText() != null && descriptionTextField.getText().length() > 0 ) {
                    resourceEntry.setDescription( descriptionTextField.getText() );
                } else {
                    resourceEntry.setDescription( null );
                }
                resourceEntry.setContent( contents );
                resourceEntry.setContentType( resourceEntry.getType().getMimeType() );
                resourceEntry.setSecurityZone(zoneControl.getSelectedZone());
                success = true;
                dispose();
            }
        }

    }

    private boolean setDTDProperties( final ResourceEntry resourceEntry ) {
        boolean valid = true;

        // Validate public identifier
        String publicId = publicIdTextField.getText();
        if ( publicId != null ) {
            publicId = DtdUtils.normalizePublicId( publicId );    
        }
        if ( publicId != null &&
             !ValidationUtils.isValidCharacters( publicId, DtdUtils.PUBLIC_ID_CHARACTERS ) ) {
            displayError( "The Public ID must contain only the following characters:\n" +
                          "a-z A-Z 0-9 - ' ( ) + , . / : = ? ; ! * # @ $ _ %",
                          "Invalid Resource" );
            valid = false;
        }

        if ( valid ) {
            // Set properties
            if ( publicId != null && !publicId.isEmpty() ) {
                resourceEntry.setResourceKey1( publicId );
            } else {
                resourceEntry.setResourceKey1( null );
            }
        }

        return valid;
    }

    private boolean setSchemaProperties( final ResourceEntry resourceEntry,
                                         final String uri,
                                         final String contents ) {
        String tns;
        try {
            tns = XmlUtil.getSchemaTNS(uri, contents, entityResolver);
        } catch (XmlUtil.BadSchemaException e) {
            logger.log( Level.WARNING, "Error parsing schema", e);
            displayError( "This is not a valid xml schema: " + ExceptionUtils.getMessage(e),
                          "Invalid Schema" );
            return false;
        }

        if ( warnForDoctype && XmlUtil.hasDoctype( contents )) {
            final int choice = JOptionPane.showOptionDialog(
                    this,
                    "The schema has a document type declaration and support is currently\ndisabled (schema.allowDoctype cluster property)",
                    "Schema Warning",
                    JOptionPane.YES_NO_OPTION,
                    JOptionPane.WARNING_MESSAGE,
                    null,
                    new String[]{ "Save", "Cancel" },
                    "Cancel");
            if ( choice == JOptionPane.NO_OPTION ) {
                return false;
            }
        }

        resourceEntry.setResourceKey1( tns );

        return true;
    }

    private void resetData() {
        systemIdTextField.setText( resourceEntry.getUri());
        systemIdTextField.setCaretPosition( 0 );
        descriptionTextField.setText( resourceEntry.getDescription()==null ? "" : resourceEntry.getDescription() );
        descriptionTextField.setCaretPosition( 0 );

        if ( resourceEntry.getType() == ResourceType.DTD ) {
            publicIdTextField.setText( resourceEntry.getResourceKey1() );
            publicIdTextField.setCaretPosition( 0 );
        }

        final String content = resourceEntry.getContent();
        if (content != null) {
            setResourceContents( content );
        }
        dataLoaded = true;
    }

    private void enableDisableComponents() {
        if ( contentOnly ) {
            publicIdLabel.setVisible( false );
            publicIdTextField.setVisible( false );
            descriptionLabel.setVisible( false );
            descriptionTextField.setVisible( false );
        } else {
            publicIdTextField.setEditable(canEdit);
            descriptionTextField.setEditable(canEdit);

            boolean enablePublicId = resourceEntry.getType() == ResourceType.DTD;
            publicIdTextField.setVisible(enablePublicId);
            publicIdLabel.setVisible(enablePublicId);
        }
        systemIdTextField.setEditable(canEdit);
        uploadFromFileBut.setEnabled(canEdit);
        uploadFromURLBut.setEnabled(canEdit);

        if ( xmlContainer != null ) {
            xmlContainer.setEditable(canEdit);
        } else {
            contentTextArea.setEditable(canEdit);
        }

        okButton.setEnabled( canEdit );
    }
}
