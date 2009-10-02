package com.l7tech.wsdl;

import com.l7tech.common.io.ResourceMapEntityResolver;

import java.util.Map;
import java.util.HashMap;
import java.util.Collections;

/**
 * Entity resolver that can be used to resolve well known WSDL resources.
 */
public class WsdlEntityResolver extends ResourceMapEntityResolver {

    //- PUBLIC

    public WsdlEntityResolver() {
        this( false );
    }

    public WsdlEntityResolver( final boolean allowMissingResource ) {
        super( publicIdsToResources,
               systemIdsToResources,
               WsdlEntityResolver.class.getClassLoader(),
               allowMissingResource );
    }

    //- PRIVATE

    private static final Map<String,String> publicIdsToResources;
    private static final Map<String,String> systemIdsToResources;

    static {
        Map<String,String> pres = new HashMap<String,String>();
        pres.put( "-//W3C//DTD XMLSCHEMA 200102//EN", "com/l7tech/wsdl/resources/XMLSchema.dtd" );
        publicIdsToResources = Collections.unmodifiableMap( pres );

        Map<String,String> sres = new HashMap<String,String>();
        sres.put( "http://www.w3.org/2001/XMLSchema.dtd", "com/l7tech/wsdl/resources/XMLSchema.dtd" );
        sres.put( "http://www.w3.org/2001/datatypes.dtd", "com/l7tech/wsdl/resources/datatypes.dtd" );
        systemIdsToResources = Collections.unmodifiableMap( sres );
    }
}
