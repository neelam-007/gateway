package com.l7tech.gateway.rest;

import org.glassfish.jersey.server.ContainerResponse;
import org.glassfish.jersey.server.ResourceConfig;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;

import javax.ws.rs.core.Application;
import javax.ws.rs.core.SecurityContext;
import javax.ws.rs.ext.RuntimeDelegate;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.lang.annotation.Annotation;
import java.net.URI;
import java.security.PrivilegedActionException;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * This is the implementation of the RestAgent. It is {@link ApplicationContextAware} so that it can be used as a spring
 * bean.
 * <p/>
 * After properties are set call {@link #init()} to initialize the rest handler. Once the handler has been initialized
 * you can handle requests by calling {@link #handleRequest(java.lang.String, java.net.URI, java.net.URI, String, String, java.io.InputStream, javax.ws.rs.core.SecurityContext, java.util.Map)}
 */
public final class RestAgentImpl implements RestAgent, ApplicationContextAware {
    private static final Logger logger = Logger.getLogger(RestAgentImpl.class.getName());

    private RestHandler handler;
    private static final int BUFFER_SIZE = 8192;
    private ApplicationContext applicationContext;
    //Additional resource classes to add to the rest application
    private Collection<Class<?>> additionalResourceClasses;
    //The list of components to find in the application context by annotation scanning.
    private Collection<Class<? extends Annotation>> autoScannedComponentAnnotations;
    //Additional component objects to add to the rest application.
    private Collection<?> additionalComponentObjects;
    //Additional properties to give the rest application.
    private Map<String, Object> resourceConfigProperties;

    /**
     * This initializes the rest agent. It will create a new jersey application.
     */
    public void init() {
        checkInitialized();
        logger.log(Level.INFO, "Initializing a RestAgent");

        final Set<Class<?>> resourceClassSet = new HashSet<>();
        //add the provider class so that the RestHandler can be created.
        resourceClassSet.add(RestHandler.RestHandlerProvider.class);
        if (additionalResourceClasses != null) {
            resourceClassSet.addAll(additionalResourceClasses);
        }
        //resourceClassSet.add(SpringBeanInjectionResolver.SpringBeanInjectionFeature.class);
        //creates the resource config for the jersey application
        ResourceConfig resourceConfig = ResourceConfig.forApplication(new Application() {
            @Override
            public Set<Class<?>> getClasses() {
                return resourceClassSet;
            }
        });

        //Add any additional properties to the rest application.
        if(resourceConfigProperties != null && !resourceConfigProperties.isEmpty()) {
            for(Map.Entry<String, Object> entry : resourceConfigProperties.entrySet()){
                resourceConfig.property(entry.getKey(), entry.getValue());
            }
        }

        //add the injection resolver feature so that @SpringBean annotations will be properly processed.
        if (applicationContext != null) {
            resourceConfig.register(new CustomInjectionResolverFeature<SpringBean,SpringBeanInjectionResolver>(new SpringBeanInjectionResolver(applicationContext), SpringBean.class){});

            //Scan the application context for any components that are annotated with one of the autoScannedComponentAnnotations. Add these components to the rest application
            if (autoScannedComponentAnnotations != null) {
                for (Class<? extends Annotation> annotation : autoScannedComponentAnnotations) {
                    Map<String, Object> beans = applicationContext.getBeansWithAnnotation(annotation);
                    for (Object bean : beans.values()) {
                        resourceConfig.register(bean);
                    }
                }
            }
        }

        //Add any additional components to the rest application
        if (additionalComponentObjects != null) {
            for (Object component : additionalComponentObjects) {
                resourceConfig.register(component);
            }
        }

        // Initialized and create the jersey application and handler.
        handler = RuntimeDelegate.getInstance().createEndpoint(resourceConfig, RestHandler.class);
        logger.log(Level.FINE, "RestAgent initialized.");
    }

    /**
     * This will throw an exception if the handler has already been initialized.
     *
     * @throws IllegalStateException Thrown if the handler has already been initialized.
     */
    private void checkInitialized() throws IllegalStateException {
        if (handler != null) {
            throw new IllegalStateException("Cannot perform action. The rest handler has already been initialized.");
        }
    }

    /**
     * Handles the rest request.
     *
     * @param requesterHost   This is the host address of the requester. It is used for audit messages
     * @param baseUri         This is the base uri of the server. This should include the host name, and port. For
     *                        example: 'https://restman-demo.l7tech.com:8443/rest/1.0/'
     * @param uri             The uri of the request. This should be the full uri of the request. Including the base Uri
     *                        and the query params. For example: 'https://restman-demo.l7tech.com:8443/rest/1.0/policies?name=myPolicy'
     * @param httpMethod      The http method used for the request.
     * @param contentType     The request content type
     * @param body            The request body stream
     * @param securityContext The security context that this call is made in. The principle user should be set.
     * @param properties      These are properties that will be set in the Jersey request.
     * @return The response returned from processing the request.
     * @throws PrivilegedActionException
     * @throws RequestProcessingException
     */
    @Override
    public RestResponse handleRequest(@Nullable final String requesterHost, @NotNull URI baseUri, @NotNull URI uri, @NotNull String httpMethod, @Nullable String contentType, @NotNull InputStream body, @Nullable SecurityContext securityContext, @Nullable Map<String,Object> properties) throws PrivilegedActionException, RequestProcessingException {
        if (handler == null) {
            throw new RequestProcessingException("The Rest handler has not yet been initialized. Cannot process requests until it have been.");
        }

        ByteArrayOutputStream bout = new ByteArrayOutputStream(BUFFER_SIZE);
        ContainerResponse response = handler.handle(requesterHost, baseUri, uri, httpMethod, contentType, body, securityContext, bout, properties);
        final ByteArrayInputStream bin = new ByteArrayInputStream(bout.toByteArray());
        return new RestResponse(bin, response.getMediaType() != null ? response.getMediaType().toString() : null, response.getStatus(), response.getHeaders());
    }

    /**
     * Sets the application context. Setting this will enable {@link SpringBean} injection to work.
     *
     * @param applicationContext The application context
     * @throws BeansException
     */
    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        checkInitialized();
        this.applicationContext = applicationContext;
    }

    /**
     * Returns the collection of additional resource classes to add to the rest application.
     *
     * @return The collection of additional resource classes to add to the rest application
     */
    public Collection<Class<?>> getAdditionalResourceClasses() {
        return Collections.unmodifiableCollection(additionalResourceClasses);
    }

    /**
     * Sets any additional classes to add to the rest application. These classes will automatically be initialized by
     * Jersey.
     *
     * @param additionalResourceClasses additional classes to add to the rest application
     */
    public void setAdditionalResourceClasses(Collection<Class<?>> additionalResourceClasses) {
        checkInitialized();
        this.additionalResourceClasses = additionalResourceClasses;
    }

    /**
     * Returns the collection of annotation classes to auto scan for in the application context. Any beans found with
     * these annotations will be added to the rest application.
     *
     * @return The collection of annotation classes to scan for in the application context.
     */
    public Collection<Class<? extends Annotation>> getAutoScannedComponentAnnotations() {
        return Collections.unmodifiableCollection(autoScannedComponentAnnotations);
    }

    /**
     * Sets the collection of annotation classes to scan for in the application context. Any bean annotated with these
     * annotations will be added to the rest application.
     *
     * @param autoScannedComponentAnnotations
     *         This is the collection of class annotations to scan for in the application context.
     */
    public void setAutoScannedComponentAnnotations(Collection<Class<? extends Annotation>> autoScannedComponentAnnotations) {
        checkInitialized();
        this.autoScannedComponentAnnotations = autoScannedComponentAnnotations;
    }

    /**
     * Returns the collection of additional objects to add to rest application.
     *
     * @return The collection of additional component objects to add to the rest application
     */
    public Collection<?> getAdditionalComponentObjects() {
        return Collections.unmodifiableCollection(additionalComponentObjects);
    }

    /**
     * Sets the collection of additional components to add to the rest application
     *
     * @param additionalComponentObjects The set of additional components to add to the rest application
     */
    public void setAdditionalComponentObjects(Collection<?> additionalComponentObjects) {
        checkInitialized();
        this.additionalComponentObjects = additionalComponentObjects;
    }

    /**
     * Returns the Map of additional properties to add to the rest application
     * @return The Map of additional properties to add to the rest application
     */
    public Map<String, Object> getResourceConfigProperties() {
        return Collections.unmodifiableMap(resourceConfigProperties);
    }

    /**
     * Sets the Map of additional properties to add to the rest application
     * @param resourceConfigProperties The Map of additional properties to add to the rest application
     */
    public void setResourceConfigProperties(Map<String, Object> resourceConfigProperties) {
        checkInitialized();
        this.resourceConfigProperties = resourceConfigProperties;
    }
}
