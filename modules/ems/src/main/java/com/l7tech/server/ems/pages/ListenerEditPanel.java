package com.l7tech.server.ems.pages;

import com.l7tech.server.ems.SetupManager;
import com.l7tech.server.ems.SetupException;
import com.l7tech.util.Config;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.RequiredTextField;
import org.apache.wicket.markup.html.panel.FeedbackPanel;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.IObjectClassAwareModel;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.apache.wicket.validation.validator.NumberValidator;
import org.apache.wicket.validation.validator.PatternValidator;

import java.util.logging.Logger;
import java.util.logging.Level;

/**
 * Panel for editing of server listen IP address / port.
 *
 * TODO [steve] form validation messaegs.
 */
public class ListenerEditPanel extends Panel {

    private static final Logger logger = Logger.getLogger(ListenerEditPanel.class.getName());

    @SuppressWarnings({"UnusedDeclaration"})
    @SpringBean
    private SetupManager setupManager;

    @SuppressWarnings({"UnusedDeclaration"})
    @SpringBean(name="serverConfig")
    private Config config;

    public ListenerEditPanel( final String id ) {
        super( id );

        String listenerAddr = config.getProperty("em.server.listenaddr", "*");
        final String listenerPort = config.getProperty("em.server.listenport", "8182");

        if ( "0.0.0.0".equals(listenerAddr) ) {
            listenerAddr = "*";
        }

        final FeedbackPanel feedback = new FeedbackPanel("feedback");
        final RequiredTextField addr = new RequiredTextField("addr", new Model(listenerAddr));
        final RequiredTextField port = new RequiredTextField("port", new IObjectClassAwareModel(){
            private int value = Integer.parseInt(listenerPort);
            @Override
            public Class getObjectClass() { return Integer.class; }
            @Override
            public Object getObject() { return value; }
            @Override
            public void setObject(Object object) { value = (Integer) object; }
            @Override
            public void detach() { }
        });

        addr.add( new PatternValidator("^(?:(?:25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\\.){3}(?:25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)$|^*$") );
        port.add( new NumberValidator.RangeValidator(1024, 65535) );

        Form listenerForm = new Form("listenerForm"){
            @Override
            protected void onSubmit() {
                logger.info("Processing HTTPS listener update.");
                try {
                    setupManager.configureListener( addr.getModelObjectAsString(), (Integer)port.getModelObject() );
                } catch ( SetupException se ) {
                    logger.log( Level.WARNING, "Error configuring listener.", se );                
                }
            }
        };
        listenerForm.setOutputMarkupId(true);

        listenerForm.add( feedback );
        listenerForm.add( addr );
        listenerForm.add( port );
        
        add( listenerForm );    }
}
