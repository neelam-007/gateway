package com.l7tech.server;

import com.l7tech.common.mime.ContentTypeHeader;
import com.l7tech.util.ConfigFactory;
import com.l7tech.util.ExceptionUtils;
import com.l7tech.util.TextUtils;
import org.springframework.beans.factory.InitializingBean;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Class to put cluster property event handling code for monitoring cluster properties where no other bean exists which
 * can manage the cluster property.
 * The usage is to configure this class to receive events when a cluster property changes and to handle the event. This
 * can involve calling into non server module code to provide runtime configuration.
 * <p/>
 * Configure this bean via the ServerConfig in the application context with the properties to receive events for.
 * If this class gets many usages beyond it's original use case it can be made into more of a generic bean where the
 * event processing code could be specified via spring configuration as a map of cluster properties to the class to process the
 * events.
 *
 * @author darmstrong
 */
public class SimplePropertyChangeHandler implements PropertyChangeListener, InitializingBean {

    @Override
    public void afterPropertiesSet() throws Exception {
        setConfiguredContentTypes();
    }

    @Override
    public void propertyChange(PropertyChangeEvent event) {
        if ( ServerConfigParams.PARAM_OTHER_TEXTUAL_CONTENT_TYPES.equals(event.getPropertyName())) {
            //Configurable content-types see bug 8884
            setConfiguredContentTypes();
        }
    }

    private void setConfiguredContentTypes() {
        final ContentTypeHeader[] headers = getConfiguredContentTypes();
        ContentTypeHeader.setConfigurableTextualContentTypes(headers);
    }

    // - PRIVATE
    private final Logger logger = Logger.getLogger(getClass().getName());

    /**
     * Get any content types which have been configured as textual via a cluster property.
     *
     * @return List of ContentTypeHeaders. Never null.
     */
    private ContentTypeHeader[] getConfiguredContentTypes() {
        final String otherTypes = ConfigFactory.getProperty( ServerConfigParams.PARAM_OTHER_TEXTUAL_CONTENT_TYPES );

        List<String> types = TextUtils.getTokensFromString(otherTypes, "\n\r\f");
        List<ContentTypeHeader> returnList = new ArrayList<ContentTypeHeader>();
        for (String type : types) {
            try {
                returnList.add(ContentTypeHeader.parseValue(type));
            } catch (IOException e) {
                if (logger.isLoggable(Level.INFO)) {
                    logger.log(Level.INFO, "Cannot parse content-type value '" + type + "' from cluster property. " +
                            "Reason: " + ExceptionUtils.getMessage(e), ExceptionUtils.getDebugException(e));
                }
            }
        }

        return returnList.toArray(new ContentTypeHeader[returnList.size()]);
    }
}

