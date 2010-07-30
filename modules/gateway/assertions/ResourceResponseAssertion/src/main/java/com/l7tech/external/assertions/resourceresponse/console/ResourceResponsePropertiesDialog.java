package com.l7tech.external.assertions.resourceresponse.console;

import com.l7tech.common.io.ByteOrderMarkInputStream;
import com.l7tech.console.SsmApplication;
import com.l7tech.console.panels.AssertionPropertiesOkCancelSupport;
import com.l7tech.console.util.Registry;
import com.l7tech.external.assertions.resourceresponse.ResourceResponseAssertion;
import com.l7tech.gateway.common.service.ServiceAdmin;
import com.l7tech.gui.util.*;
import com.l7tech.gui.widgets.TextListCellRenderer;
import com.l7tech.util.Charsets;
import com.l7tech.util.ExceptionUtils;
import com.l7tech.util.Functions;
import com.l7tech.util.IOUtils;
import com.l7tech.util.ResourceUtils;
import com.l7tech.wsdl.ResourceTrackingWSDLLocator;
import com.l7tech.wsdl.WsdlEntityResolver;
import com.l7tech.xml.DocumentReferenceProcessor;
import org.xml.sax.EntityResolver;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import javax.swing.*;
import javax.swing.filechooser.FileFilter;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.StringReader;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * 
 */
public class ResourceResponsePropertiesDialog extends AssertionPropertiesOkCancelSupport<ResourceResponseAssertion> {

    //- PUBLIC

    public ResourceResponsePropertiesDialog( final Window parent,
                                             final ResourceResponseAssertion assertion ) {
        super( ResourceResponseAssertion.class, parent, assertion, true );
        initComponents();
        setData(assertion);
    }

    @Override
    public ResourceResponseAssertion getData( final ResourceResponseAssertion assertion ) throws ValidationException {
        final Collection<ResourceResponseAssertion.ResourceData> resources = new ArrayList<ResourceResponseAssertion.ResourceData>();
        final ListModel model = resourceList.getModel();
        for ( int i=0; i<model.getSize(); i++ ) {
            resources.add( (ResourceResponseAssertion.ResourceData) model.getElementAt( i ) );
        }
        assertion.setResources( resources.toArray( new ResourceResponseAssertion.ResourceData[resources.size()] ));
        assertion.setUsePath( includeResourceNameInCheckBox.isSelected() );
        assertion.setFailOnMissing( failAssertionIfResourceCheckBox.isSelected() );

        return assertion;
    }

    @Override
    public void setData( final ResourceResponseAssertion assertion ) {
        resourceList.setModel( buildModel( assertion.getResources() ) );
        includeResourceNameInCheckBox.setSelected( assertion.isUsePath() );
        failAssertionIfResourceCheckBox.setSelected( assertion.isFailOnMissing() );
    }

    //- PROTECTED

    @Override
    protected JPanel createPropertyPanel() {
        return mainPanel;
    }

        @Override
    protected void initComponents() {
        super.initComponents();

        resourceList.setCellRenderer( new TextListCellRenderer<ResourceResponseAssertion.ResourceData>(
            new Functions.Unary<String,ResourceResponseAssertion.ResourceData>(){
                @Override
                public String call( final ResourceResponseAssertion.ResourceData resourceData ) {
                    return resourceData.getUri();
                }
            }
        ));

        final RunOnChangeListener enableDisableListener = new RunOnChangeListener() {
            @Override
            public void run() {
                enableDisableControls();
            }
        };

        locationTextField.getDocument().addDocumentListener( enableDisableListener );

        setResourceButton.addActionListener( new ActionListener(){
            @Override
            public void actionPerformed( final ActionEvent e ) {
                selectResource();
            }
        } );

        fileButton.addActionListener( new ActionListener(){
            @Override
            public void actionPerformed( final ActionEvent e ) {
                selectFile();
            }
        } );
    }

    //- PRIVATE

    private static final Logger logger = Logger.getLogger( ResourceResponsePropertiesDialog.class.getName() );

    private JPanel mainPanel;
    private JCheckBox failAssertionIfResourceCheckBox;
    private JCheckBox includeResourceNameInCheckBox;
    private JList resourceList;
    private JButton setResourceButton;
    private JTextField locationTextField;
    private JButton fileButton;

    private ListModel buildModel( final ResourceResponseAssertion.ResourceData[] resources ) {
        final DefaultListModel model = new DefaultListModel();
        if ( resources != null ) {
            int index = 0;
            for ( final ResourceResponseAssertion.ResourceData resource : resources ) {
                model.add( index++, resource );
            }
        }
        return model;
    }

    private void enableDisableControls() {
        boolean enableReset = locationTextField.getText().trim().length() > 0 && !isReadOnly();
        setResourceButton.setEnabled( enableReset );
    }

    private void selectFile() {
        SsmApplication.doWithJFileChooser(new FileChooserUtil.FileChooserUser() {
            @Override
            public void useFileChooser(JFileChooser fc) {
                doSelectFile(fc);
            }
        });
    }

    private void doSelectFile( final JFileChooser fc) {
        fc.setDialogTitle("Select WSDL or XML Schema.");
        fc.setDialogType(JFileChooser.OPEN_DIALOG);
        final FileFilter fileFilter = new FileFilter() {
            @Override
            public boolean accept( File f) {
                return  f.isDirectory() ||
                        f.getName().toLowerCase().endsWith(".wsdl") ||
                        f.getName().toLowerCase().endsWith(".xsd");
            }
            @Override
            public String getDescription() {
                return "(*.wsdl/*.xsd) resource files.";
            }
        };
        fc.addChoosableFileFilter(fileFilter);
        fc.setMultiSelectionEnabled(false);
        final int r = fc.showDialog( this, "Open");
        if( r == JFileChooser.APPROVE_OPTION ) {
            final File file = fc.getSelectedFile();
            if( file != null ) {
                locationTextField.setText( file.toURI().toString() );
            }
        }
    }

    private void selectResource() {
        final String resourceUrl = locationTextField.getText().trim();

        if ( isUrlOk(resourceUrl) ) { // then it is http or https so the Gateway should resolve it
            try {
                final boolean permitLocal = resourceUrl.startsWith( "file:" );
                final EntityResolver entityResolver = new GatewayEntityResolver( permitLocal );
                final String resource = fetchUrl( resourceUrl, permitLocal, entityResolver );

                if ( resource != null ) {
                    final DocumentReferenceProcessor processor = new DocumentReferenceProcessor();
                    final Map<String,String> urisToResources =
                            processor.processDocument( resourceUrl, new GatewayResourceResolver(logger, entityResolver, resourceUrl, resource) );

                    final Collection<ResourceResponseAssertion.ResourceData> resources = new ArrayList<ResourceResponseAssertion.ResourceData>();

                    resources.add( new ResourceResponseAssertion.ResourceData(
                            UUID.randomUUID().toString(),
                            resourceUrl,
                            "text/xml",
                            resource ) );

                    for ( final Map.Entry<String,String> uriToResource : urisToResources.entrySet() ) {
                        if ( resourceUrl.equals( uriToResource.getKey() )) continue;
                        resources.add( new ResourceResponseAssertion.ResourceData(
                                UUID.randomUUID().toString(),
                                uriToResource.getKey(),
                                "text/xml",
                                uriToResource.getValue() ) );
                    }

                    resourceList.setModel( buildModel( resources.toArray( new ResourceResponseAssertion.ResourceData[resources.size()] )) );                    
                }
            } catch ( IOException ioe ) {
                logger.log( Level.WARNING, "Error processing resource", ioe );
                DialogDisplayer.showMessageDialog( this, "Resource Access Error", "Error processing resource:\n\n  "+ ExceptionUtils.getMessage( ioe ), null );
            }
        }
    }

    private boolean isUrlOk( final String urlStr ) {
        boolean urlOk = false;
        try {
            final URL url = new URL(urlStr);
            final String protocol = url.getProtocol();
            if (protocol != null) {
                if ("http".equals(protocol) ||
                    "https".equals(protocol) ||
                    "file".equals(protocol)) {
                    // Check if the file path is a valid URI
                    if ("file".equals(protocol)) new File(new URI(urlStr));

                    urlOk = true;
                }
            }
        } catch (MalformedURLException e) {
            //invalid url
        } catch (URISyntaxException e) {
            //invalid uri
        } catch (IllegalArgumentException e) {
            // invalid file uri
        }

        return urlOk;
    }

    private static String fetchUrl( final String resourceUrl,
                                    final boolean permitLocal,
                                    final EntityResolver entityResolver ) throws IOException {
        String resource = null;

        if ( resourceUrl.startsWith("http") ) {
            resource = ResourceTrackingWSDLLocator.processResource(resourceUrl, gatewayFetchUrl(resourceUrl), entityResolver, false, true);
        } else if ( permitLocal ) {
            resource = ResourceTrackingWSDLLocator.processResource(resourceUrl, localFetchUrl(resourceUrl), entityResolver, false, true);
        }

        return resource;
    }

    private static String gatewayFetchUrl( final String resourceUrl ) throws IOException {
        ServiceAdmin manager = Registry.getDefault().getServiceManager();

        if  (manager == null )
            throw new IOException("Service not available.");

        return manager.resolveWsdlTarget(resourceUrl);
    }

    private static String localFetchUrl( final String resourceUrl ) throws IOException {
        ByteOrderMarkInputStream inputStream = null;
        try {
            inputStream = new ByteOrderMarkInputStream(new URL(resourceUrl).openStream());
            final byte [] data = IOUtils.slurpStream(inputStream);
            return inputStream.getEncoding() == null ? new String( data, Charsets.UTF8 ) : new String( data, inputStream.getEncoding() );
        } finally {
            ResourceUtils.closeQuietly(inputStream);
        }
    }

    private static class GatewayEntityResolver implements EntityResolver {
        private final boolean permitLocalFiles;
        private final EntityResolver catalogResolver;

        private GatewayEntityResolver( final boolean permitLocalFiles ) {
            this.permitLocalFiles = permitLocalFiles;
            this.catalogResolver = new WsdlEntityResolver(true);
        }

        @Override
        public InputSource resolveEntity( final String publicId, final String systemId ) throws SAXException, IOException {
            InputSource inputSource = catalogResolver.resolveEntity( publicId, systemId );

            if ( inputSource == null ) {
                String resource = null;
                if ( systemId.startsWith("http:") ) {
                    resource = gatewayFetchUrl( systemId) ;
                } else if ( systemId.startsWith("file:") && permitLocalFiles ) {
                    resource = localFetchUrl( systemId );
                }

                if ( resource == null ) {
                    throw new IOException("Could not resolve entity '"+systemId+"'.");
                }

                inputSource = new InputSource();
                inputSource.setPublicId( publicId );
                inputSource.setSystemId( systemId );
                inputSource.setCharacterStream( new StringReader(resource) );
            }

            return inputSource;
        }
    }

    /**
     * GatewayResourceResolver that accesses HTTP URI's via the gateway.
     */
    private static class GatewayResourceResolver implements DocumentReferenceProcessor.ResourceResolver {
        private final Logger logger;
        private final EntityResolver entityResolver;
        private final String baseUri;
        private final String baseResource;

        private GatewayResourceResolver(final Logger logger,
                                        final EntityResolver entityResolver,
                                        final String baseUri,
                                        final String baseResource ) {
            this.logger = logger;
            this.entityResolver = entityResolver;
            this.baseUri = baseUri;
            this.baseResource = baseResource;
        }

        @Override
        public String resolve( final String importLocation ) throws IOException {
            String resource = null;

            logger.log(Level.INFO, "Processing import from location '" + importLocation + "'.");


            if ( baseUri.equals(importLocation) ) {
                resource = ResourceTrackingWSDLLocator.processResource(baseUri, baseResource, entityResolver, false, true);
            } else {
                if ( importLocation.startsWith("http") ) {
                    resource = ResourceTrackingWSDLLocator.processResource(importLocation, gatewayFetchUrl(importLocation), entityResolver, false, true);
                } else if ( importLocation.startsWith("file") && baseUri.startsWith("file")) {
                    resource = ResourceTrackingWSDLLocator.processResource(importLocation, localFetchUrl(importLocation), entityResolver, false, true);
                }
            }

            if ( resource == null) {
                throw new FileNotFoundException("Resource not found '"+importLocation+"'.");
            }

            return resource;
        }
    }
}
