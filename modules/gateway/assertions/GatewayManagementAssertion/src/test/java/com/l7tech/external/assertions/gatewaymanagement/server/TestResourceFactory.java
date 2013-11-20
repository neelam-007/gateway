package com.l7tech.external.assertions.gatewaymanagement.server;

import com.l7tech.common.io.XmlUtil;
import com.l7tech.gateway.api.AccessibleObject;
import com.l7tech.gateway.api.impl.AccessorSupport;
import com.l7tech.objectmodel.EntityType;
import com.l7tech.util.ExceptionUtils;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

import java.util.*;

/**
 * ResourceFactory for testing custom methods
 */
@ResourceFactory.ResourceType(type=TestResourceFactory.TestResource.class)
public class TestResourceFactory implements ResourceFactory<Object> {

    //- PUBLIC

    @AccessorSupport.AccessibleResource(name="testResources")
    public static class TestResource extends AccessibleObject {
    }

    @Override
    public EntityType getType() {
        return null;
    }

    @Override
    public boolean isReadOnly() {
        return false;
    }

    @Override
    public boolean isCreateSupported() {
        return !isReadOnly();
    }

    @Override
    public boolean isDeleteSupported() {
        return !isReadOnly();
    }

    @Override
    public boolean isUpdateSupported() {
        return !isReadOnly();
    }

    @Override
    public Set<String> getSelectors() {
        return Collections.singleton( "id" );
    }

    @Override
    public Map<String, String> createResource( final Object resource ) throws InvalidResourceException {
        throw new InvalidResourceException(InvalidResourceException.ExceptionType.UNEXPECTED_TYPE, "create not supported");
    }

    public Map<String,String> createResource(String id, Object resource) throws InvalidResourceException {
        throw new InvalidResourceException(InvalidResourceException.ExceptionType.UNEXPECTED_TYPE, "create not supported");
    }

    @Override
    public Object getResource( final Map<String, String> selectorMap ) throws ResourceNotFoundException {
        throw new ResourceNotFoundException("Resource not found " + selectorMap);
    }

    @Override
    public Object putResource( final Map<String, String> selectorMap,
                               final Object resource ) throws ResourceNotFoundException, InvalidResourceException {
        throw new ResourceNotFoundException("Resource not found " + selectorMap);
    }

    @Override
    public String deleteResource( final Map<String, String> selectorMap ) throws ResourceNotFoundException {        
        throw new ResourceNotFoundException("Resource not found " + selectorMap);
    }

    @Override
    public Collection<Map<String, String>> getResources() {
        return null;
    }

    @Override
    public List<Map<String, String>> getResources(int offset, int windowSize) {
        return null;
    }

    @ResourceFactory.ResourceMethod(name="Create")
    public Object customCreate() {
        return newResource();
    }

    @ResourceFactory.ResourceMethod(name="Create2", resource=true)
    public Object customCreate( final Object resource ) {
        return resource;
    }

    @SuppressWarnings({"UnusedDeclaration"})
    @ResourceMethod(name="Get", selectors=true)
    public Object customGet( final Map<String,String> selectorMap ) throws InvalidResourceSelectors {
        final String id = selectorMap.get( "id" );
        if ( id == null ) throw new InvalidResourceSelectors();
        return newResource();
    }

    @SuppressWarnings({"UnusedDeclaration"})
    @ResourceFactory.ResourceMethod(name="Put", selectors=true, resource=true)
    public Object customPut( final Map<String,String> selectorMap, final Object resource ) throws InvalidResourceSelectors {
        final String id = selectorMap.get( "id" );
        if ( id == null ) throw new InvalidResourceSelectors();
        return resource;
    }

    //- PRIVATE

    /**
     * This test uses a document for simplicity but usually any POJO is returned (JAXB converts to XML) 
     */
    private Document newResource() {
        try {
            return XmlUtil.parse( "<TestResource>Test resource text</TestResource>" );
        } catch (SAXException e) {
            throw ExceptionUtils.wrap(e);
        }
    }

}
