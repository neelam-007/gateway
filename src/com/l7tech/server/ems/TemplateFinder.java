package com.l7tech.server.ems;

import com.l7tech.common.util.ExceptionUtils;
import com.l7tech.common.util.SyspropUtil;
import freemarker.template.Configuration;
import freemarker.template.ObjectWrapper;
import freemarker.template.Template;
import freemarker.template.TemplateExceptionHandler;
import org.restlet.Context;
import org.restlet.Finder;
import org.restlet.data.MediaType;
import org.restlet.data.Request;
import org.restlet.data.Response;
import org.restlet.ext.freemarker.TemplateRepresentation;
import org.restlet.resource.Representation;
import org.restlet.resource.Resource;
import org.restlet.resource.ResourceException;
import org.restlet.resource.Variant;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.ApplicationContext;
import org.springframework.beans.BeansException;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.MissingResourceException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * A Finder implementation that provides access to a classpath directory full of FreeMarker templates.
 * Expects a request attribute named "templateName" to be set to the name of a template to execute.
 * This template name will be matched against a whitelist of known template names.
 */
public class TemplateFinder extends Finder implements ApplicationContextAware {
    private static final Logger logger = Logger.getLogger(TemplateFinder.class.getName());
    private static final int TEMPLATE_UPDATE_DELAY = SyspropUtil.getInteger("com.l7tech.ems.templateUpdateDelay", 2);

    private final Map<String, Template> templates;
    private HashMap<String, Object> dataModel;

    public TemplateFinder(Context context, HashMap<String, Object> dataModel, String templatesResourcePath, String[] allowedTemplateNames) {
        super(context);

        // Initialize template engine
        Configuration config = new Configuration();
        config.setClassForTemplateLoading(getClass(), templatesResourcePath);
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
                throw (MissingResourceException)new MissingResourceException("Unable to initialize template \"" + name +
                        "\": " + ExceptionUtils.getMessage(e), Template.class.getName(), name).initCause(e);
            }
        }
    }

    @Override
    public Resource createTarget(Request request, Response response) {
        Object templateNameObj = request.getAttributes().get("templateName");
        String templateName = templateNameObj == null ? null : templateNameObj.toString();
        Template template = templates.get(templateName);
        if (null == template) {
            logger.log(Level.WARNING, "Request for unrecognized template name " + templateName);
            return null;
        }

        return new TemplateResource(getContext(), dataModel, template, request, response);
    }

    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        // Make the application context available to templates
        dataModel.put("applicationContext", applicationContext);
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
}
