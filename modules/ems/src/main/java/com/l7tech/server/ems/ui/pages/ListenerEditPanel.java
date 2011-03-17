package com.l7tech.server.ems.ui.pages;

import com.l7tech.util.InetAddressUtil;
import com.l7tech.server.ems.setup.SetupManager;
import com.l7tech.server.ems.setup.SetupException;
import com.l7tech.util.Config;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.RequiredTextField;
import org.apache.wicket.markup.html.panel.FeedbackPanel;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.IObjectClassAwareModel;
import javax.inject.Inject;
import org.apache.wicket.validation.validator.PatternValidator;
import org.apache.wicket.validation.validator.AbstractValidator;
import org.apache.wicket.validation.validator.RangeValidator;
import org.apache.wicket.validation.IValidatable;

import java.util.logging.Logger;
import java.util.logging.Level;
import java.net.NetworkInterface;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.net.SocketException;

/**
 * Panel for editing of server listen IP address / port.
 */
public class ListenerEditPanel extends Panel {

    private static final Logger logger = Logger.getLogger(ListenerEditPanel.class.getName());

    @Inject
    private SetupManager setupManager;

    @Inject
    private Config config;

    public ListenerEditPanel( final String id ) {
        super( id );

        String listenerAddr = config.getProperty("em.server.listenaddr", "*");
        final String listenerPort = config.getProperty("em.server.listenport", "8182");

        if (InetAddressUtil.isAnyHostAddress(listenerAddr) ) {
            listenerAddr = "*";
        }

        final FeedbackPanel feedback = new FeedbackPanel("feedback");
        final RequiredTextField<String> addr = new RequiredTextField<String>("addr", new Model<String>(listenerAddr));
        final RequiredTextField<Integer> port = new RequiredTextField<Integer>("port", new IObjectClassAwareModel<Integer>(){
            private int value = Integer.parseInt(listenerPort);
            @Override
            public Class<Integer> getObjectClass() { return Integer.class; }
            @Override
            public Integer getObject() { return value; }
            @Override
            public void setObject(Integer object) { value = object; }
            @Override
            public void detach() { }
        });

        addr.add( new PatternValidator("^(?:(?:(?:25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\\.){3}(?:25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)|\\*)$") );
        addr.add( new LocalIpAddressValidator() );
        port.add( new RangeValidator<Integer>(1024, 65535) );

        Form listenerForm = new Form("listenerForm"){
            @Override
            protected void onSubmit() {
                logger.fine("Processing HTTPS listener update.");
                try {
                    setupManager.configureListener( addr.getConvertedInput(), port.getConvertedInput() );
                } catch ( SetupException se ) {
                    logger.log( Level.WARNING, "Error configuring listener.", se );                
                }
            }
        };
        listenerForm.setOutputMarkupId(true);

        listenerForm.add( feedback );
        listenerForm.add( addr );
        listenerForm.add( port );
        
        add( listenerForm );
    }

    private static final class LocalIpAddressValidator extends AbstractValidator<String> {
        @Override
        protected void onValidate( final IValidatable<String> validatable ) {
            String ipAddress = validatable.getValue();

            boolean isOk = false;
            try {
                isOk = "*".equals(ipAddress) || NetworkInterface.getByInetAddress( InetAddress.getByName(ipAddress) ) != null;
            } catch (UnknownHostException uhe) {
                // not ok
            } catch (SocketException e) {
                // not ok
            }

            if ( !isOk ) {
                error( validatable );
            }
        }
    }
}
