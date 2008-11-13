package com.l7tech.server.ems.pages;

import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.Radio;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.markup.html.form.PasswordTextField;
import org.apache.wicket.markup.html.form.RadioGroup;
import org.apache.wicket.markup.html.form.upload.FileUploadField;
import org.apache.wicket.markup.html.form.upload.FileUpload;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.AjaxEventBehavior;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.CompoundPropertyModel;
import org.apache.wicket.Component;
import org.apache.wicket.util.lang.Bytes;
import org.apache.wicket.spring.injection.annot.SpringBean;

import java.util.logging.Logger;
import java.util.logging.Level;
import java.util.Collections;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.UnrecoverableKeyException;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.io.IOException;
import java.io.Serializable;

import com.l7tech.server.ems.SetupManager;
import com.l7tech.server.ems.SetupException;
import com.l7tech.util.SyspropUtil;

/**
 * Panel for generation / upload of SSL key / certificate.
 *
 * TODO [steve] form feedback and validation messaegs.
 */
public class SslEditPanel extends Panel {

    private static final Logger logger = Logger.getLogger(SslEditPanel.class.getName());
    private static final int MAX_KEYSTORE_FILE_UPLOAD_BYTES = SyspropUtil.getInteger("com.l7tech.ems.keystoreFile.maxBytes", 1024 * 500);

    @SuppressWarnings({"UnusedDeclaration"})
    @SpringBean
    private SetupManager setupManager;

    private String successScript = null;

    public SslEditPanel( final String id ) {
        super( id );

        final SslFormModel sslFormModel = new SslFormModel();

        Radio sslGenerate = new Radio("sslRadioGen", new Model("gen"));
        Radio sslLoad = new Radio("sslRadioLoad", new Model("load"));
        final RadioGroup group = new RadioGroup("sslGroup", new Model("gen"));

        TextField hostname = new TextField("hostname");

        final FileUploadField keystore = new FileUploadField("keystore");

        PasswordTextField password = new PasswordTextField("password");
        password.setResetPassword(false);

        TextField alias = new TextField("alias");

        final Component[] generateComponents = new Component[]{ hostname };
        final Component[] keystoreComponents = new Component[]{ keystore, password, alias };

        sslLoad.add( new AjaxEventBehavior("onchange"){
            @Override
            protected void onEvent( final AjaxRequestTarget ajaxRequestTarget ) {
                enableComponents( ajaxRequestTarget, generateComponents, false );
                enableComponents( ajaxRequestTarget, keystoreComponents, true );
            }
        });

        sslGenerate.add( new AjaxEventBehavior("onchange"){
            @Override
            protected void onEvent( final AjaxRequestTarget ajaxRequestTarget ) {
                enableComponents( ajaxRequestTarget, generateComponents, true );
                enableComponents( ajaxRequestTarget, keystoreComponents, false );
            }
        });

        Form sslForm = new YuiFileUploadForm("sslForm", new CompoundPropertyModel(sslFormModel)){
            @Override
            @SuppressWarnings({"UnusedDeclaration", "SuspiciousSystemArraycopy"})
            protected void onSubmit( final AjaxRequestTarget target ) {
                logger.info("Processing SSL update.");

                if ( successScript != null && target != null) {
                    target.prependJavascript(successScript);
                }

                String alias = null;
                if ( "gen".equals( group.getModelObjectAsString() ) ) {
                    String hostValue = sslFormModel.getHostname();
                    try {
                        alias = setupManager.generateSsl( hostValue );
                    } catch ( SetupException se ) {
                        logger.log( Level.WARNING, "Error configuring ssl for hostname '"+hostValue+"'.", se );
                    }
                } else {
                    String passwordValue = sslFormModel.getPassword();
                    String aliasValue = sslFormModel.getAlias();
                    try {
                        final FileUpload upload = keystore.getFileUpload();
                        if ( upload != null ) {
                            KeyStore keystore = KeyStore.getInstance("PKCS12");
                            keystore.load( upload.getInputStream(), passwordValue.toCharArray() );

                            if ( aliasValue == null || aliasValue.trim().isEmpty() ) {
                                aliasValue = "";
                                for ( String keystoreAlias : Collections.list(keystore.aliases()) ) {
                                    if ( !aliasValue.isEmpty() ) throw new SetupException("Alias not specified and multiple values present.");
                                    aliasValue = keystoreAlias;
                                }
                            }

                            Certificate[] certs = keystore.getCertificateChain(aliasValue);
                            X509Certificate[] xcerts = new X509Certificate[certs.length];
                            System.arraycopy(certs, 0, xcerts, 0, certs.length);                            

                             alias = setupManager.saveSsl( (PrivateKey)keystore.getKey(aliasValue, passwordValue.toCharArray()), xcerts );
                        } else {
                            logger.warning("Keystore not present in upload!");
                        }
                    } catch ( SetupException se ) {
                        logger.log( Level.WARNING, "Error configuring ssl with keystore.", se );
                    } catch ( KeyStoreException ke ) {
                        logger.log( Level.WARNING, "Error configuring ssl with keystore.", ke );
                    } catch (IOException e) {
                        logger.log( Level.WARNING, "Error configuring ssl with keystore.", e );
                    } catch (NoSuchAlgorithmException e) {
                        logger.log( Level.WARNING, "Error configuring ssl with keystore.", e );
                    } catch (CertificateException e) {
                        logger.log( Level.WARNING, "Error configuring ssl with keystore.", e );
                    } catch (UnrecoverableKeyException e) {
                        logger.log( Level.WARNING, "Error configuring ssl with keystore.", e );
                    }
                }

                if ( alias != null ) {
                    try {
                        setupManager.setSslAlias( alias );
                        Thread.sleep(100L); // default key invalidation occurs in another thread ...
                    } catch ( SetupException se ) {
                        logger.log( Level.WARNING, "Error configuring ssl with keystore.", se );
                    } catch ( InterruptedException ie ) {
                        Thread.currentThread().interrupt();
                    }
                }

                SslEditPanel.this.onSubmit( target );
            }
        };

        group.add( sslGenerate.setOutputMarkupId(true) );
        group.add( sslLoad.setOutputMarkupId(true) );
        group.add( hostname.setOutputMarkupId(true) );
        group.add( keystore.setOutputMarkupId(true).setEnabled(false) );
        group.add( password.setOutputMarkupId(true).setEnabled(false) );
        group.add( alias.setOutputMarkupId(true).setEnabled(false) );

        sslForm.setMaxSize( Bytes.bytes(MAX_KEYSTORE_FILE_UPLOAD_BYTES) );
        sslForm.setOutputMarkupId( true );
        sslForm.add( group );

        add( sslForm );
    }

    public void setSuccessScript( final String script ) {
        this.successScript = script;    
    }

    /**
     * Override to customize onSubmit behaviour.
     */
    @SuppressWarnings({"UnusedDeclaration"})
    protected void onSubmit( final AjaxRequestTarget target ) {
    }

    private void enableComponents(  final AjaxRequestTarget ajaxRequestTarget, final Component[] components, final boolean enable ) {
        for ( Component component : components ) {
            component.setEnabled(enable);
            ajaxRequestTarget.appendJavascript("document.getElementById('"+component.getMarkupId()+"').disabled = "+!enable+";");
        }
    }

    private static final class SslFormModel implements Serializable {
        private String hostname = "";
        private String password = "";
        private String alias = "";

        public String getAlias() {
            return alias;
        }

        public void setAlias(String alias) {
            this.alias = alias;
        }

        public String getPassword() {
            return password;
        }

        public void setPassword(String password) {
            this.password = password;
        }

        public String getHostname() {
            return hostname;
        }

        public void setHostname(String hostname) {
            this.hostname = hostname;
        }
    }
}