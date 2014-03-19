package com.l7tech.external.assertions.gatewaymanagement.tools;

import com.l7tech.external.assertions.gatewaymanagement.server.rest.resource.ChoiceParam;
import com.l7tech.util.Functions;
import com.sun.research.ws.wadl.*;
import com.sun.research.ws.wadl.Application;
import com.sun.research.ws.wadl.Request;
import com.sun.research.ws.wadl.Response;
import org.glassfish.jersey.server.model.Parameter;
import org.glassfish.jersey.server.model.ResourceMethod;
import org.glassfish.jersey.server.wadl.WadlGenerator;
import org.glassfish.jersey.server.wadl.internal.ApplicationDescription;
import org.glassfish.jersey.server.wadl.internal.WadlUtils;
import org.glassfish.jersey.server.wadl.internal.generators.resourcedoc.ResourceDocAccessor;
import org.glassfish.jersey.server.wadl.internal.generators.resourcedoc.model.MethodDocType;
import org.glassfish.jersey.server.wadl.internal.generators.resourcedoc.model.ParamDocType;
import org.glassfish.jersey.server.wadl.internal.generators.resourcedoc.model.ResourceDocType;
import org.glassfish.jersey.server.wadl.internal.generators.resourcedoc.model.ResponseDocType;
import org.jetbrains.annotations.NotNull;

import javax.inject.Provider;
import javax.validation.constraints.Pattern;
import javax.ws.rs.core.*;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.namespace.QName;
import javax.xml.parsers.SAXParserFactory;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * This is used to add more info to the wadl generated from our resources.
 */
public class ExtendedWadlGenerator implements org.glassfish.jersey.server.wadl.WadlGenerator {
    @Context
    private Provider<SAXParserFactory> saxFactoryProvider;

    private WadlGenerator delegate;
    private InputStream resourceDocStream;
    private ResourceDocAccessor resourceDocAccessor;


    @Override
    public void setWadlGeneratorDelegate(WadlGenerator delegate) {
        this.delegate = delegate;
    }

    /**
     * This will set the resourceDocStream. This is called by Jersey using reflection
     *
     * @param resourceDocStream the resourcedoc stream to set.
     */
    @SuppressWarnings("UnusedDeclaration")
    public void setResourceDocStream(InputStream resourceDocStream) {
        this.resourceDocStream = resourceDocStream;
    }

    public void init() throws Exception {
        if (resourceDocStream == null) {
            throw new IllegalStateException("The resourceDocStream is not set, it is required.");
        }
        delegate.init();

        final ResourceDocType resourceDocType = WadlUtils.unmarshall(resourceDocStream, saxFactoryProvider.get(), ResourceDocType.class);
        resourceDocAccessor = new ResourceDocAccessor(resourceDocType);
    }

    @Override
    public String getRequiredJaxbContextPath() {
        return delegate.getRequiredJaxbContextPath();
    }

    @Override
    public Application createApplication() {
        Application application = delegate.createApplication();

        //Add the grammars
        Include include = new Include();
        include.setHref("gateway-management.xsd");

        Grammars grammars = new Grammars();
        grammars.getInclude().add(include);

        application.setGrammars(grammars);

        return application;
    }

    @Override
    public Resources createResources() {
        return delegate.createResources();
    }

    @Override
    public Resource createResource(org.glassfish.jersey.server.model.Resource r, String path) {
        return delegate.createResource(r, path);
    }

    @Override
    public Method createMethod(org.glassfish.jersey.server.model.Resource r, ResourceMethod m) {
        return delegate.createMethod(r, m);
    }

    @Override
    public Request createRequest(org.glassfish.jersey.server.model.Resource r, ResourceMethod m) {

        Request request = delegate.createRequest(r, m);

        List<Parameter> parameters = m.getInvocable().getParameters();
        for (int i = 0; i < parameters.size(); i++) {
            Parameter p = parameters.get(i);
            if (p.getSource() == Parameter.Source.ENTITY) {
                Representation representation = new Representation();
                representation.setElement(findEntityQName(p.getRawType()));
                representation.setMediaType(MediaType.APPLICATION_XML);

                final ParamDocType paramDoc = getParamDoc(m.getInvocable().getDefinitionMethod().getDeclaringClass(),
                        m.getInvocable().getDefinitionMethod(), i);
                if (paramDoc != null && paramDoc.getCommentText() != null && !paramDoc.getCommentText().isEmpty()) {
                    final Doc doc = new Doc();
                    doc.getContent().add(paramDoc.getCommentText());
                    representation.getDoc().add(doc);
                }

                request.getRepresentation().add(representation);
            }
        }

        return request;
    }

    /**
     * This will return the appropriate QName
     *
     * @param rawType The type of the parameter
     * @return The QName of the parameter
     */
    private QName findEntityQName(@NotNull Class<?> rawType) {
        if (String.class.equals(rawType)) {
            return new QName("http://www.w3.org/2001/XMLSchema", "string");
        }

        if(StreamingOutput.class.equals(rawType)) {
            return null;
        }

        XmlRootElement rootElemAnnotation = rawType.getAnnotation(XmlRootElement.class);
        if (rootElemAnnotation == null) {
            throw new IllegalStateException("Found an entity parameter that is not annotated with @XmlRootElement: " + rawType.toString());
        }

        return new QName("http://ns.l7tech.com/2010/04/gateway-management", rootElemAnnotation.name());
    }

    public ParamDocType getParamDoc(Class<?> resourceClass, java.lang.reflect.Method method,
                                    int pIndex) {
        final MethodDocType methodDoc = resourceDocAccessor.getMethodDoc(resourceClass, method);
        if (methodDoc != null && methodDoc.getParamDocs().size() > pIndex) {
            return methodDoc.getParamDocs().get(pIndex);
        }
        return null;
    }

    @Override
    public Representation createRequestRepresentation(org.glassfish.jersey.server.model.Resource r, ResourceMethod m, MediaType mediaType) {
        return delegate.createRequestRepresentation(r, m, mediaType);
    }

    @Override
    public List<Response> createResponses(org.glassfish.jersey.server.model.Resource r, ResourceMethod m) {
        List<Response> responses = delegate.createResponses(r, m);

        //the WadlGeneratorResourceDocSupport generator does not add the @return java doc comment to the response.
        final ResponseDocType responseDoc = resourceDocAccessor.getResponse(m.getInvocable().getDefinitionMethod().getDeclaringClass(),
                m.getInvocable().getDefinitionMethod());
        if (responseDoc != null && responseDoc.getReturnDoc() != null && !responseDoc.getReturnDoc().isEmpty()) {
            for (Response response : responses) {
                if (response.getStatus().isEmpty()) {
                    response.getStatus().add(200L);
                }
                if (response.getStatus().contains(200L)) {
                    if (response.getDoc().isEmpty()) {
                        final Doc doc = new Doc();
                        doc.getContent().add(responseDoc.getReturnDoc());
                        response.getDoc().add(doc);
                    }

                    Class<?> rawResponseType = m.getInvocable().getRawResponseType();
                    if (rawResponseType != null && !javax.ws.rs.core.Response.class.equals(rawResponseType)) {
                        if (response.getRepresentation().isEmpty()) {
                            Representation representation = new Representation();
                            response.getRepresentation().add(representation);
                        }
                        for (Representation representation : response.getRepresentation()) {
                            if (representation.getElement() == null || representation.getElement().getLocalPart().isEmpty())
                                representation.setElement(findEntityQName(rawResponseType));
                        }
                    }
                }
            }
        }
        return responses;
    }

    @Override
    public Param createParam(org.glassfish.jersey.server.model.Resource r, ResourceMethod m, Parameter p) {
        Param param = delegate.createParam(r, m, p);

        //testing if the parameter is a list type
        if (List.class.isAssignableFrom(p.getRawType())) {
            param.setRepeating(true);
        } else if (p.getRawType().isEnum()) {
            ArrayList<Option> options = new ArrayList<>();
            for (Object obj : p.getRawType().getEnumConstants()) {
                Enum enumValue = (Enum) obj;
                Option option = new Option();
                option.setValue(enumValue.name());
                options.add(option);
            }
            param.getOption().addAll(options);
        } else if (p.getAnnotation(Pattern.class) != null) {
            Pattern pattern = p.getAnnotation(Pattern.class);
            if (pattern.regexp().matches("^[a-zA-Z0-9_\\|]+$") && !pattern.regexp().contains("||")) {
                //this is a simple OR pattern
                ArrayList<Option> options = new ArrayList<>();
                for (String optionValue : pattern.regexp().split("\\|")) {
                    Option option = new Option();
                    option.setValue(optionValue);
                    options.add(option);
                }
                param.getOption().addAll(options);
            }
        } else if (p.getAnnotation(ChoiceParam.class) != null) {
            ChoiceParam choiceParam = p.getAnnotation(ChoiceParam.class);
            param.getOption().addAll(Functions.map(Arrays.asList(choiceParam.value()), new Functions.Unary<Option, String>() {
                @Override
                public Option call(String param) {
                    Option option = new Option();
                    option.setValue(param);
                    return option;
                }
            }));
        }
        return param;
    }

    @Override
    public ExternalGrammarDefinition createExternalGrammar() {
        return new ExternalGrammarDefinition();
    }

    @Override
    public void attachTypes(ApplicationDescription description) {
        delegate.attachTypes(description);
    }
}
