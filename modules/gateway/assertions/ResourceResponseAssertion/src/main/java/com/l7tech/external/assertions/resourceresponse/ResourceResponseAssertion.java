package com.l7tech.external.assertions.resourceresponse;

import com.l7tech.policy.assertion.AssertionMetadata;
import com.l7tech.policy.assertion.DefaultAssertionMetadata;
import com.l7tech.policy.assertion.RoutingAssertion;
import com.l7tech.policy.assertion.RoutingAssertionDoesNotRoute;
import com.l7tech.policy.wsp.ArrayTypeMapping;
import com.l7tech.policy.wsp.BeanTypeMapping;
import com.l7tech.policy.wsp.SimpleTypeMappingFinder;
import com.l7tech.policy.wsp.TypeMapping;
import com.l7tech.util.HexUtils;

import java.util.ArrayList;
import java.util.Collection;

import static com.l7tech.policy.assertion.AssertionMetadata.WSP_SUBTYPE_FINDER;

/**
 * 
 */
public class ResourceResponseAssertion extends RoutingAssertion implements RoutingAssertionDoesNotRoute {

    //- PUBLIC

    public ResourceResponseAssertion(){        
    }

    /**
     * Get the resources for the assertion.
     *
     * @return The resources (may be null)
     */
    public ResourceData[] getResources() {
        return resources;
    }

    public void setResources( final ResourceData[] resources ) {
        this.resources = resources;
    }

    /**
     * Should a "friendly" resource name be added to the path.
     *
     * @return True to include a "friendly" name.
     */
    public boolean isUsePath() {
        return usePath;
    }

    public void setUsePath( final boolean usePath ) {
        this.usePath = usePath;
    }

    /**
     * Should the assertion fail if the resource is not found.
     *
     * @return True to fail, False for a 404 response.
     */
    public boolean isFailOnMissing() {
        return failOnMissing;
    }

    public void setFailOnMissing( final boolean failOnMissing ) {
        this.failOnMissing = failOnMissing;
    }

    @Override
    public AssertionMetadata meta() {
        DefaultAssertionMetadata meta = super.defaultMeta();
        if (Boolean.TRUE.equals(meta.get(META_INITIALIZED)))
            return meta;

        meta.put(AssertionMetadata.SHORT_NAME, "Resource Response");
        meta.put(AssertionMetadata.DESCRIPTION, "Return a response message initialized from a static resource.");
        meta.put(AssertionMetadata.PALETTE_FOLDERS, new String[] { "routing" });
        meta.put(AssertionMetadata.POLICY_ADVICE_CLASSNAME, "auto");
        meta.put(AssertionMetadata.POLICY_NODE_ICON, "com/l7tech/console/resources/MessageLength-16x16.gif");

        Collection<TypeMapping> mappings = new ArrayList<TypeMapping>();
        mappings.add(new ArrayTypeMapping(new ResourceData[0], "resources"));
        mappings.add(new BeanTypeMapping(ResourceData.class, "resourceData"));
        meta.put(WSP_SUBTYPE_FINDER, new SimpleTypeMappingFinder(mappings));

        meta.put(AssertionMetadata.FEATURE_SET_NAME, "set:modularAssertions");

        meta.put(META_INITIALIZED, Boolean.TRUE);
        return meta;
    }

    public static class ResourceData {
        private String id;
        private String uri;
        private String contentType;
        private String content;
        private String contentHash;

        public ResourceData(){
        }

        public ResourceData( final String id,
                             final String uri,
                             final String contentType,
                             final String content ) {
            this();
            setId( id );
            setUri( uri );
            setContentType( contentType );
            setContent( content );
        }

        public String getId() {
            return id;
        }

        public void setId( final String id ) {
            this.id = id;
        }

        public String getUri() {
            return uri;
        }

        public void setUri( final String uri ) {
            this.uri = uri;
        }

        public String getContentType() {
            return contentType;
        }

        public void setContentType( final String contentType ) {
            this.contentType = contentType;
        }

        public String getContent() {
            return content;
        }

        public void setContent( final String content ) {
            this.content = content;
            this.contentHash = null;
        }

        public String contentHash() {
            String hash = contentHash;

            if ( hash == null ) {
                hash = HexUtils.encodeMd5Digest(HexUtils.getMd5Digest(content==null ? new byte[0] : content.getBytes()));
                contentHash = hash;
            }

            return hash;
        }
    }

    //- PRIVATE

    private static final String META_INITIALIZED = ResourceResponseAssertion.class.getName() + ".metadataInitialized";

    private ResourceData[] resources;
    private boolean usePath = false;
    private boolean failOnMissing = true;
}
