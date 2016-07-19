package com.l7tech.server.service;

import com.l7tech.common.mime.ContentTypeHeader;
import com.l7tech.common.mime.NoSuchPartException;
import com.l7tech.gateway.common.LicenseException;
import com.l7tech.identity.User;
import com.l7tech.identity.UserBean;
import com.l7tech.message.AbstractHttpResponseKnob;
import com.l7tech.message.Message;
import com.l7tech.objectmodel.Goid;
import com.l7tech.policy.assertion.Assertion;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.policy.assertion.PolicyAssertionException;
import com.l7tech.policy.wsp.WspReader;
import com.l7tech.security.token.OpaqueSecurityToken;
import com.l7tech.server.StashManagerFactory;
import com.l7tech.server.event.AdminInfo;
import com.l7tech.server.identity.AuthenticationResult;
import com.l7tech.server.message.PolicyEnforcementContext;
import com.l7tech.server.message.PolicyEnforcementContextFactory;
import com.l7tech.server.policy.ServerAssertionRegistry;
import com.l7tech.server.policy.ServerPolicyException;
import com.l7tech.server.policy.ServerPolicyFactory;
import com.l7tech.server.policy.assertion.ServerAssertion;
import com.l7tech.server.policy.module.AssertionModuleScanCompletedEvent;
import com.l7tech.server.util.PostStartupApplicationListener;
import com.l7tech.util.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.context.ApplicationEvent;

import javax.inject.Inject;
import java.io.*;
import java.util.Arrays;
import java.util.Comparator;
import java.util.concurrent.Callable;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Install a RESTMAN migration bundle or SKAR file on startup, if one is found on-disk on startup in a special directory.
 */
public class MigrationBundleBootstrapService implements PostStartupApplicationListener {
    private static final Logger logger = Logger.getLogger( MigrationBundleBootstrapService.class.getName() );

    @Inject
    private ServerPolicyFactory serverPolicyFactory;

    @Inject
    private WspReader wspReader;

    @Inject
    private StashManagerFactory stashManagerFactory;

    @Inject
    private ServerAssertionRegistry serverAssertionRegistry;

    // TODO find some way to detect when bundles should not be loaded on startup perhaps because they are already loaded
    private boolean shouldLoadBundles = true;

    private static String BOOTSTRAP_BUNDLE_FOLDER = ConfigFactory.getProperty("bootstrap.folder.bundle");

    static void loadFolder(){ // for test coverage
        BOOTSTRAP_BUNDLE_FOLDER = ConfigFactory.getProperty("bootstrap.folder.bundle");
    }

    @Override
    public void onApplicationEvent( ApplicationEvent event ) {
        if ( event instanceof AssertionModuleScanCompletedEvent ) {
            installBootstrapBundles();
        }
    }

    private void installBootstrapBundles() {
        try {
            // Do nothing unless the RESTMAN assertion has been registered
            Assertion restman = serverAssertionRegistry.findByClassName( "com.l7tech.external.assertions.gatewaymanagement.RESTGatewayManagementAssertion" );
            if ( null == restman ) {
                logger.log( Level.INFO, "RESTMAN assertio not (yet) available, will not (yet) attempt to install bootstrap bundles" );
                return;
            }

            // TODO have a way to look up or configure admin user instead of hardcoding "admin" from internal ID provider
            final UserBean adminUser = new UserBean( new Goid( 0, -2 ), "admin" );
            adminUser.setUniqueIdentifier( new Goid( 0, 3 ).toString() );

            AdminInfo.find( false ).wrapCallable( new Callable<Boolean>() {
                @Override
                public Boolean call() throws Exception {

                    if ( shouldLoadBundles ) {
                        // Stop trying to load bundles as soon as we make one attempt to do so
                        shouldLoadBundles = false;

                        File bundleFolder = new File( BOOTSTRAP_BUNDLE_FOLDER );
                        if ( !bundleFolder.exists() ) return false;
                        if ( !bundleFolder.isDirectory() ) return false;
                        File[] bundleFiles = bundleFolder.listFiles();
                        if ( bundleFiles == null ) return false;

                        //sort in alphabetical order
                        Arrays.sort(bundleFiles, new Comparator<File>() {
                            @Override
                            public int compare(File o1, File o2) {
                                return o1.getName().compareTo(o2.getName());
                            }
                        });

                        for ( File bundleFile : bundleFiles ) {
                            installBundleFile( bundleFile, adminUser );
                        }
                    }

                    return false;
                }
            } ).call();


        } catch ( FatalBundleInstallFailedException e ) {
            throw new RuntimeException( "At least one required bundle failed to install: " +  ExceptionUtils.getMessageWithCause( e ), e );
        } catch ( Exception e ) {
            logger.log( Level.WARNING, "Unable to install bundles: " + ExceptionUtils.getMessageWithCause( e ), e );
        }
    }

    private void installBundleFile( File bundleFile, User adminUser ) throws BundleInstallFailedException {
        boolean required = false;
        try {
            String fullName = bundleFile.getName();
            String baseName = fullName.replaceAll( "\\..*", "" );
            required = fullName.matches( ".*\\.req\\b.*" );
            boolean skarMultipart = fullName.endsWith( ".skmult" );
            boolean bundle = fullName.endsWith( ".bundle" );
            if ( !skarMultipart && !bundle )
                throw new IllegalArgumentException( "Bundle file name must end in either .skmult or .bundle: " + fullName );

            String thing = skarMultipart ? "RESTMAN multipart parameterized solution kit" : "RESTMAN migration bundle";
            logger.info("Installing " + thing + " from: " + bundleFile.getCanonicalPath());
            final String multipartBoundary;
            //if this is a skmult file we need to read the multipart/form-data boundary from the first line in the input
            // stream so that it can be added to the multipart/form-data header
            if(skarMultipart) {
                try(BufferedReader brTest = new BufferedReader(new FileReader(bundleFile))) {
                    //Note we need to strip out the first 2 characters
                    multipartBoundary = brTest.readLine().substring(2);
                }
            } else {
                multipartBoundary=null;
            }
            try ( InputStream fis = new FileInputStream( bundleFile ) ) {
                installSkarOrBundle( fis, skarMultipart, multipartBoundary, adminUser );
            }

        } catch ( Exception e ) {
            logger.log( Level.WARNING, "Unable to install bundle file: " + bundleFile + ": " + ExceptionUtils.getMessageWithCause( e ), e );
            if ( required ) {
                throw new FatalBundleInstallFailedException( "Required bundle failed to install: " + ExceptionUtils.getMessage( e ), e );
            }
        }
    }


    private void installSkarOrBundle(@NotNull InputStream bundleInputStream,
                       final boolean isSkarMultipart, final String multipartBoundary,
                       @NotNull User adminUser) throws IOException, BundleInstallFailedException
    {

        String policyXml =
                "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                        "<wsp:Policy xmlns:L7p=\"http://www.layer7tech.com/ws/policy\" xmlns:wsp=\"http://schemas.xmlsoap.org/ws/2002/12/policy\">\n" +
                        "    <wsp:All wsp:Usage=\"Required\">\n" +
                        "        <L7p:RESTGatewayManagement>\n" +
                        "            <L7p:OtherTargetMessageVariable stringValue=\"mess\"/>\n" +
                        "            <L7p:Target target=\"OTHER\"/>\n" +
                        "        </L7p:RESTGatewayManagement>\n" +
                        "    </wsp:All>\n" +
                        "</wsp:Policy>\n";
        Assertion assertion = wspReader.parseStrictly( policyXml, WspReader.Visibility.omitDisabled );


        ServerAssertion sph = null;
        try ( PolicyEnforcementContext context = PolicyEnforcementContextFactory.createPolicyEnforcementContext( new Message(), new Message() ) ) {
            sph = serverPolicyFactory.compilePolicy( assertion, false );
            Message mess = new Message();

            if ( isSkarMultipart ) {
                // SKAR bundle
                logger.log( Level.INFO, "SKAR bundle detected");

                ContentTypeHeader formDataContentType = ContentTypeHeader.parseValue( "multipart/form-data; boundary=" + multipartBoundary );
                mess.initialize( stashManagerFactory.createStashManager(), formDataContentType, bundleInputStream );
                context.setVariable( "restGatewayMan.action", "POST" );
                context.setVariable( "restGatewayMan.uri", "1.0/solutionKitManagers" );
            } else {
                // restman bundle
                mess.initialize( stashManagerFactory.createStashManager(), ContentTypeHeader.XML_DEFAULT, bundleInputStream );
                context.setVariable("restGatewayMan.action", "PUT");
                context.setVariable( "restGatewayMan.uri", "1.0/bundle" );
            }

            context.getResponse().attachHttpResponseKnob( new AbstractHttpResponseKnob() {} );
            context.setVariable( "mess", mess );
            context.getDefaultAuthenticationContext().addAuthenticationResult( new AuthenticationResult( adminUser, new OpaqueSecurityToken() ) );

            AssertionStatus result = sph.checkRequest( context );
            int httpStatus = context.getResponse().getHttpResponseKnob().getStatus();

            String resp = new String( IOUtils.slurpStream( context.getResponse().getMimeKnob().getEntireMessageBodyAsInputStream() ), Charsets.UTF8 );

            logger.log( Level.INFO, "Bundle installation response from local RESTMAN bundle install: result=" + result + " httpStatus=" + httpStatus + " body:\n" + resp );

            if ( !AssertionStatus.NONE.equals( result ) || httpStatus != 200 ) {
                throw new BundleInstallFailedException( "RESTMAN failed with result=" + result + " httpStatus=" + httpStatus + ": " + resp );
            }

        } catch ( ServerPolicyException | LicenseException e ) {
            throw new BundleInstallFailedException( "Unable to prepare RESTMAN policy: " + ExceptionUtils.getMessage( e ), e );
        } catch ( PolicyAssertionException |NoSuchPartException e ) {
            throw new BundleInstallFailedException( "Unable to invoke RESTMAN policy: " + ExceptionUtils.getMessage( e ), e );
        } finally {
            ResourceUtils.closeQuietly( sph );
        }
    }

    private static class BundleInstallFailedException extends Exception {
        BundleInstallFailedException( String message ) {
            super( message );
        }

        BundleInstallFailedException( String message, Throwable cause ) {
            super( message, cause );
        }
    }

    private static class FatalBundleInstallFailedException extends BundleInstallFailedException {
        FatalBundleInstallFailedException( String message, Throwable cause ) {
            super( message, cause );
        }
    }

}
