package com.l7tech.gateway.common.resources;

import com.l7tech.common.mime.ContentTypeHeader;
import org.junit.Test;

import java.io.IOException;

/**
 *
 */
public class ResourceTypeTest {

    @Test
    public void testMimeTypes() throws IOException {
        for ( final ResourceType resourceType : ResourceType.values() ) {
            ContentTypeHeader.parseValue( resourceType.getMimeType() );
        }
    }
}
