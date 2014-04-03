package com.l7tech.external.assertions.gatewaymanagement.tools;

import com.l7tech.external.assertions.gatewaymanagement.server.rest.resource.RestEntityResource;
import com.sun.research.ws.wadl.*;
import junit.framework.Assert;
import org.jetbrains.annotations.NotNull;
import org.junit.Test;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import javax.xml.transform.stream.StreamSource;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.regex.Pattern;

public class WadlTest {

    @Test
    public void test() throws IOException, JAXBException {
        InputStream wadlStream = RestEntityResource.class.getResourceAsStream("/com/l7tech/external/assertions/gatewaymanagement/server/rest/resource/restAPI.wadl");

        JAXBContext context = JAXBContext.newInstance(Application.class);
        final StreamSource source = new StreamSource(wadlStream);
        final Unmarshaller unmarshaller = context.createUnmarshaller();
        Application applicationWadl = unmarshaller.unmarshal(source, Application.class).getValue();

        //test the wadl to make sure it is properly and completely documented!
        for (Resources resources : applicationWadl.getResources()) {
            for (Resource resource : resources.getResource()) {
                validateResource(resource);
            }
        }
    }

    private void validateResource(Resource resource) {
        validateDoc("Invalid doc for resource at path: " + resource.getPath(), resource.getDoc());
        validateDoc("Invalid title doc for resource at path: " + resource.getPath(), Pattern.compile("title.*"), resource.getDoc());
        for (Object methodOrResource : resource.getMethodOrResource()) {
            if (methodOrResource instanceof Method) {
                Method method = (Method) methodOrResource;
                validateMethod(resource, method);
            } else if (methodOrResource instanceof Resource) {
                validateResource((Resource) methodOrResource);
            }
        }
    }

    private void validateMethod(Resource resource, Method method) {
        validateDoc("Invalid doc for method with id: '" + method.getId() + "' at resource path: " + resource.getPath(), method.getDoc());
        validateDoc("Invalid title doc for method with id: '" + method.getId() + "' at resource path: " + resource.getPath(), Pattern.compile("title.*"), resource.getDoc());
        validateRequest(resource, method, method.getRequest());
        for (Response response : method.getResponse()) {
            validateResponse(resource, method, response);
        }
    }

    private void validateResponse(Resource resource, Method method, Response response) {
        validateDoc("Invalid doc for response on method with id: '" + method.getId() + "' at resource path: " + resource.getPath(), method.getDoc());
    }

    private void validateRequest(Resource resource, Method method, Request request) {
        if (request == null) return;
        for (Param param : request.getParam()) {
            validateParam(resource, method, request, param);
        }
    }

    private void validateParam(Resource resource, Method method, Request request, Param param) {
        Assert.assertNotNull("Invalid name for param on request on method with id: '" + method.getId() + "' at resource path: " + resource.getPath(), param.getName());
        Assert.assertTrue("Invalid name for param on request on method with id: '" + method.getId() + "' at resource path: " + resource.getPath(), !param.getName().isEmpty());
        Assert.assertNotNull("Invalid style for param '" + param.getName() + "' on request on method with id: '" + method.getId() + "' at resource path: " + resource.getPath(), param.getStyle());
        Assert.assertNotNull("Invalid type for param '" + param.getName() + "' on request on method with id: '" + method.getId() + "' at resource path: " + resource.getPath(), param.getType());
        validateDoc("Invalid doc for param '" + param.getName() + "' on request on method with id: '" + method.getId() + "' at resource path: " + resource.getPath(), param.getDoc());
    }

    private void validateDoc(String errorMessage, List<Doc> docs) {
        Assert.assertNotNull(errorMessage, docs);
        Assert.assertTrue(errorMessage, !docs.isEmpty());
        Assert.assertNotNull(errorMessage, docs.get(0).getContent());
        Assert.assertTrue(errorMessage, !docs.get(0).getContent().isEmpty());
        Assert.assertNotNull(errorMessage, docs.get(0).getContent().get(0));
        Assert.assertNotNull(errorMessage, docs.get(0).getContent().get(0).toString());
        Assert.assertTrue(errorMessage, !docs.get(0).getContent().get(0).toString().isEmpty());
    }

    private void validateDoc(String errorMessage, @NotNull Pattern docTitle, List<Doc> docs) {
        Assert.assertNotNull(errorMessage, docs);
        Assert.assertTrue(errorMessage, !docs.isEmpty());
        for(Doc doc : docs) {
            if(doc.getTitle() != null && docTitle.matcher(doc.getTitle()).matches()) {
                Assert.assertNotNull(errorMessage, docs.get(0).getContent());
                Assert.assertTrue(errorMessage, !docs.get(0).getContent().isEmpty());
                Assert.assertNotNull(errorMessage, docs.get(0).getContent().get(0));
                Assert.assertNotNull(errorMessage, docs.get(0).getContent().get(0).toString());
                Assert.assertTrue(errorMessage, !docs.get(0).getContent().get(0).toString().isEmpty());
                return;
            }
        }
        Assert.fail(errorMessage);

    }
}
