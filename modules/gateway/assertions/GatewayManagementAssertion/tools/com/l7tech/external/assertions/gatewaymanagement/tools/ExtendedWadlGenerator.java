package com.l7tech.external.assertions.gatewaymanagement.tools;

import com.l7tech.external.assertions.gatewaymanagement.server.rest.resource.ChoiceParam;
import com.l7tech.external.assertions.gatewaymanagement.server.rest.resource.RestManVersion;
import com.l7tech.external.assertions.gatewaymanagement.server.rest.resource.Since;
import com.l7tech.external.assertions.gatewaymanagement.server.rest.resource.impl.WadlResource;
import com.l7tech.util.Functions;
import com.sun.research.ws.wadl.*;
import org.apache.commons.lang.WordUtils;
import org.glassfish.jersey.server.ContainerRequest;
import org.glassfish.jersey.server.internal.LocalizationMessages;
import org.glassfish.jersey.server.model.Parameter;
import org.glassfish.jersey.server.model.ResourceMethod;
import org.glassfish.jersey.server.wadl.WadlGenerator;
import org.glassfish.jersey.server.wadl.internal.ApplicationDescription;
import org.glassfish.jersey.server.wadl.internal.generators.resourcedoc.ResourceDocAccessor;
import org.glassfish.jersey.server.wadl.internal.generators.resourcedoc.model.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.xml.sax.InputSource;

import javax.inject.Provider;
import javax.validation.constraints.Pattern;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.ProcessingException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.StreamingOutput;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.namespace.QName;
import javax.xml.parsers.SAXParserFactory;
import javax.xml.transform.sax.SAXSource;
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

    @Context
    private ContainerRequest containerRequest;

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

        final JAXBContext jaxbContext;
        try {
            //Add the ResourceDocProperty class to the jaxbContext It is added when parsing the javadocs.
            jaxbContext = JAXBContext.newInstance(ResourceDocType.class, ResourceDocProperty.class);
        } catch (JAXBException ex) {
            throw new ProcessingException(LocalizationMessages.ERROR_WADL_JAXB_CONTEXT(), ex);
        }
        final SAXSource source = new SAXSource(saxFactoryProvider.get().newSAXParser().getXMLReader(), new InputSource(resourceDocStream));
        final Unmarshaller unmarshaller = jaxbContext.createUnmarshaller();
        final ResourceDocType resourceDocType = (ResourceDocType) unmarshaller.unmarshal(source);

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
        include.setHref(WadlResource.GATEWAY_URL + "1.0/gateway-management.xsd");

        Grammars grammars = new Grammars();
        grammars.getInclude().add(include);

        application.setGrammars(grammars);

        //add a documentation reference
        final Doc doc = new Doc();
        doc.setTitle("api-documentation-url");
        doc.getContent().add(WadlResource.GATEWAY_URL + "1.0/doc");
        application.getAny().add(doc);

        return application;
    }

    @Override
    public Resources createResources() {
        return delegate.createResources();
    }

    @Override
    public Resource createResource(org.glassfish.jersey.server.model.Resource r, String path) {
        Resource resource = delegate.createResource(r, path);
        for (Class<?> resourceClass : r.getHandlerClasses()) {
            //Create title from resource class name. Only use class name from our resources
            if ("com.l7tech.external.assertions.gatewaymanagement.server.rest.resource.impl".equals(resourceClass.getPackage().getName())) {
                {
                    final Doc doc = new Doc();
                    doc.setTitle("title-src");
                    //Remove the last 'Resource' from the name. Most class names are '<Entity>Resource' so the resource part is unneeded
                    //Then split the name
                    doc.getContent().add(splitCamelCase((resourceClass.getSimpleName().replaceAll("Resource$", ""))));
                    resource.getDoc().add(doc);
                }
                //Find title from javaDocs
                final ClassDocType classDoc = resourceDocAccessor.getClassDoc(resourceClass);
                if (classDoc != null && !classDoc.getAny().isEmpty()) {
                    for (Object any : classDoc.getAny()) {
                        if (any instanceof ResourceDocProperty && "title".equals(((ResourceDocProperty) any).getName())) {
                            final Doc doc = new Doc();
                            doc.setTitle("title-javadoc");
                            doc.getContent().add(((ResourceDocProperty) any).getValue());
                            resource.getDoc().add(doc);
                        }
                    }
                }

                //add the since annotation value to the docs
                if (resourceClass.getAnnotation(Since.class) != null) {
                    Since sinceParam = resourceClass.getAnnotation(Since.class);
                    final Doc doc = new Doc();
                    doc.setTitle("since");
                    doc.getContent().add(sinceParam.value().getStringRepresentation());
                    resource.getDoc().add(doc);
                }
            }
        }
        return resource;
    }

    @Override
    public Method createMethod(org.glassfish.jersey.server.model.Resource r, ResourceMethod m) {
        Method method = delegate.createMethod(r, m);
        //null the method ID otherwise we will have many duplicate ids
        method.setId(null);

        final java.lang.reflect.Method realMethod = m.getInvocable().getDefinitionMethod();
        {
            final Doc doc = new Doc();
            doc.setTitle("title-src");
            doc.getContent().add(splitCamelCase(realMethod.getName()));
            method.getDoc().add(doc);
        }

        final MethodDocType methodDoc = resourceDocAccessor.getMethodDoc(realMethod.getDeclaringClass(), realMethod);
        if (methodDoc != null && !methodDoc.getAny().isEmpty()) {
            for (Object any : methodDoc.getAny()) {
                if (any instanceof ResourceDocProperty && "title".equals(((ResourceDocProperty) any).getName())) {
                    final Doc doc = new Doc();
                    doc.setTitle("title-javadoc");
                    doc.getContent().add(((ResourceDocProperty) any).getValue());
                    method.getDoc().add(doc);
                }
            }
        }

        //add the since annotation value to the docs
        if (realMethod.getAnnotation(Since.class) != null) {
            Since sinceParam = realMethod.getAnnotation(Since.class);
            final Doc doc = new Doc();
            doc.setTitle("since");
            doc.getContent().add(sinceParam.value().getStringRepresentation());
            method.getDoc().add(doc);
        }
        return method;
    }

    private static String splitCamelCase(@NotNull String s) {
        //This nicely splits Camel case names into human readable names
        // Found format string here: http://stackoverflow.com/questions/2559759/how-do-i-convert-camelcase-into-human-readable-names-in-java
        return WordUtils.capitalizeFully(s.replaceAll(
                String.format("%s|%s|%s",
                        "(?<=[A-Z])(?=[A-Z][a-z])",
                        "(?<=[^A-Z])(?=[A-Z])",
                        "(?<=[A-Za-z])(?=[^A-Za-z])"
                ),
                " "
        ));
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

        if (StreamingOutput.class.equals(rawType)) {
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
        }
        if (p.getRawType().isEnum()) {
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
        } else if (p.getAnnotation(Since.class) != null) {
            //add the since annotation value to the docs
            Since sinceParam = p.getAnnotation(Since.class);
            Doc doc = new Doc();
            doc.setTitle("since");
            doc.getContent().add(sinceParam.value().getStringRepresentation());
            param.getDoc().add(doc);
        } else if (wadlRequestVersionAtLeast(RestManVersion.VERSION_1_0_1) && p.getAnnotation(DefaultValue.class) != null) {
            DefaultValue defaultValueParam = p.getAnnotation(DefaultValue.class);
            param.setDefault(defaultValueParam.value());
        }
        return param;
    }

    private boolean wadlRequestVersionAtLeast(@Nullable RestManVersion version) {
        return version == null || (containerRequest.getProperty("RestManVersion") != null && containerRequest.getProperty("RestManVersion") instanceof RestManVersion && version.compareTo((RestManVersion)containerRequest.getProperty("RestManVersion")) <= 0);
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
