package com.l7tech.external.assertions.gatewaymanagement.tools;

import com.sun.javadoc.*;
import org.glassfish.jersey.server.wadl.internal.generators.resourcedoc.model.ClassDocType;
import org.glassfish.jersey.server.wadl.internal.generators.resourcedoc.model.MethodDocType;
import org.glassfish.jersey.server.wadl.internal.generators.resourcedoc.model.ParamDocType;
import org.glassfish.jersey.wadl.doclet.DocProcessor;

import java.util.Arrays;
import java.util.List;

/**
 * This is used when processing the javadoc. It is used to collect extra information from the javadocs. currently the
 * only extra info it collects in from @title tags
 */
public class L7DocProcessor implements DocProcessor {
    /**
     * Add the ResourceDocProperty class to the jaxbContext classes.
     */
    @Override
    public Class<?>[] getRequiredJaxbContextClasses() {
        List<Class<?>> classes = Arrays.<Class<?>>asList(ResourceDocProperty.class);
        return classes.toArray(new Class<?>[classes.size()]);
    }

    @Override
    public String[] getCDataElements() {
        return new String[0];
    }

    /**
     * Adds any @title tags to the the class doc.
     */
    @Override
    public void processClassDoc(ClassDoc classDoc, ClassDocType classDocType) {
        Tag[] titleTags = classDoc.tags("title");
        if (titleTags != null && titleTags.length > 0) {
            classDocType.getAny().add(new ResourceDocProperty("title", titleTags[0].text()));
        }
    }

    /**
     * Adds any @title tags to the the method doc.
     */
    @Override
    public void processMethodDoc(MethodDoc methodDoc, MethodDocType methodDocType) {
        Tag[] titleTags = methodDoc.tags("title");
        if (titleTags != null && titleTags.length > 0) {
            methodDocType.getAny().add(new ResourceDocProperty("title", titleTags[0].text()));
        }
    }

    @Override
    public void processParamTag(ParamTag paramTag, Parameter parameter, ParamDocType paramDocType) {
    }
}
