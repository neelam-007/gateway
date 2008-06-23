package com.l7tech.server.ems;

import com.l7tech.common.util.ExceptionUtils;
import com.l7tech.common.util.SyspropUtil;
import freemarker.template.Configuration;
import freemarker.template.ObjectWrapper;
import freemarker.template.Template;
import freemarker.template.TemplateExceptionHandler;
import org.restlet.*;
import org.restlet.data.LocalReference;
import org.restlet.data.MediaType;
import org.restlet.data.Request;
import org.restlet.data.Response;
import org.restlet.ext.freemarker.TemplateRepresentation;
import org.restlet.resource.Representation;
import org.restlet.resource.Resource;
import org.restlet.resource.ResourceException;
import org.restlet.resource.Variant;
import org.springframework.context.ApplicationContext;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.MissingResourceException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * The Enterprise Manager Server application.
 */
public class EmsApplication extends Application {
    private static final Logger logger = Logger.getLogger(EmsApplication.class.getName());

    private static final String PROP_PREFIX = "com.l7tech.server.ems.";
    private static final String TEMPLATES_RESOURCE_PATH = "/com/l7tech/server/ems/resources/templates";
    private static final int TEMPLATE_UPDATE_DELAY = SyspropUtil.getInteger(PROP_PREFIX + "templateUpdateDelay", 2);

    private static final String[] TEMPLATE_NAMES = {
            "Setup.html",
            "Gateways.html",
            "Audits.html",
            "Backup.html",
            "Help.html",
            "Login.html",
            "Logs.html",
            "Notifications.html",
            "PolicyApproval.html",
            "PolicyMapping.html",
            "PolicyMigration.html",
            "PolicySubmission.html",
            "Reports.html",
            "Restore.html",
            "Setup.html",
            "SystemSettings.html",
            "TestWebService.html",
            "UserSettings.html"
    };


    private final ApplicationContext springContext;

    public EmsApplication(Context parentContext, ApplicationContext springContext) {
        super(parentContext);
        if (springContext == null) throw new NullPointerException("springContext");
        this.springContext = springContext;
    }

    @Override
    public Restlet createRoot() {
        HashMap<String, Object> dataModel = new HashMap<String, Object>();
        dataModel.put("emsVersion", "0.1");

        Router router = new Router(getContext());
        router.attach("/setup", SetupResource.class);
        router.attach("/license", LicenseResource.class);
        router.attach("/images/", new StripQueryFilter(clapDir(getContext(), "clap://thread/com/l7tech/server/ems/resources/images")));
        router.attach("/ems/", new TemplateFinder(getContext(), dataModel, TEMPLATE_NAMES));
        router.attachDefault(EmsHomePage.class);
        return router;
    }

    private static Directory clapDir(Context context, String clapUrl) {
        final Directory dir = new Directory(context, new LocalReference(clapUrl));
        dir.setNegotiateContent(false); // (classpath isn't listable)
        return dir;
    }

    private static class TemplateFinder extends Finder {
        private final Map<String, Template> templates;
        private HashMap<String, Object> dataModel;

        public TemplateFinder(Context context, HashMap<String, Object> dataModel, String[] allowedTemplateNames) {
            super(context);

            // Initialize template engine
            Configuration config = new Configuration();
            config.setClassForTemplateLoading(getClass(), TEMPLATES_RESOURCE_PATH);
            config.setObjectWrapper(ObjectWrapper.BEANS_WRAPPER);
            config.setDefaultEncoding("UTF-8");
            config.setOutputEncoding("UTF-8");
            config.setTemplateUpdateDelay(TEMPLATE_UPDATE_DELAY);
            config.setTemplateExceptionHandler(TemplateExceptionHandler.HTML_DEBUG_HANDLER);

            this.dataModel = dataModel;

            this.templates = new HashMap<String, Template>();
            for (String name : allowedTemplateNames) {
                try {
                    templates.put(name, config.getTemplate(name));
                } catch (IOException e) {
                    throw (MissingResourceException)new MissingResourceException("Unable to initialize template \"" + name + "\": " + ExceptionUtils.getMessage(e), Template.class.getName(), name).initCause(e);
                }
            }

            this.dataModel = new HashMap<String, Object>();
            dataModel.put("name", "Amazing Name");
        }

        @Override
        public Resource createTarget(Request request, Response response) {
            String templateName = request.getResourceRef().getLastSegment(true) + ".html";
            Template template = templates.get(templateName);
            if (null == template) {
                logger.log(Level.WARNING, "Request for unrecognized template name " + templateName);
                return null;
            }

            return new TemplateResource(getContext(), dataModel, template, request, response);
        }
    }

    private static class TemplateResource extends Resource {
        private final Template template;
        private final Object dataModel;

        private TemplateResource(Context context, Object dataModel, Template template, Request request, Response response) {
            super(context, request, response);
            this.template = template;
            this.dataModel = dataModel;
            getVariants().add(new Variant(MediaType.TEXT_HTML));
        }

        @Override
        public Representation represent(Variant variant) throws ResourceException {
            return new TemplateRepresentation(template, dataModel, variant.getMediaType());
        }
    }

    /**
     * Get the Spring COntext for this application.
     *
     * @return the Spring ApplicationContext.  Never null.
     */
    public ApplicationContext getSpringContext() {
        return springContext;
    }

    /**
     * A Filter that strips any query string from every request URL.
     */
    private class StripQueryFilter extends Filter {
        public StripQueryFilter(Restlet restlet) {
            super(EmsApplication.this.getContext(), restlet);
        }

        @Override
        protected int beforeHandle(Request request, Response response) {
            request.getResourceRef().setQuery(null);
            return Filter.CONTINUE;
        }
    }
}
