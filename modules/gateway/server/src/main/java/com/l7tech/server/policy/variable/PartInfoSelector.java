package com.l7tech.server.policy.variable;

import com.l7tech.common.mime.ContentTypeHeader;
import com.l7tech.common.mime.MimeHeader;
import com.l7tech.common.mime.NoSuchPartException;
import com.l7tech.common.mime.PartInfo;
import com.l7tech.policy.variable.Syntax;
import com.l7tech.server.ServerConfigParams;
import com.l7tech.util.ConfigFactory;
import com.l7tech.util.IOUtils;
import com.l7tech.util.ResourceUtils;

import java.io.IOException;
import java.io.InputStream;

/**
 * Selector for MIME part properties.
 */
class PartInfoSelector implements ExpandVariables.Selector<PartInfo> {

    //- PUBLIC

    @Override
    public Selection select( final String contextName, 
                             final PartInfo partInfo,
                             final String name,
                             final Syntax.SyntaxErrorHandler handler,
                             final boolean strict ) {
        final String lowerCaseName = name.toLowerCase();
        final Selection selection;

        if ( lowerCaseName.equals( BODY_NAME ) ) {
            selection = selectBody( partInfo, handler, strict );
        } else if ( lowerCaseName.equals( CONTENT_TYPE ) ) {
            selection = selectContentType( partInfo );
        } else if ( lowerCaseName.startsWith( HEADER_PREFIX ) ) {
            selection = selectHeader( name(contextName,HEADER_PREFIX), partInfo, name.substring( HEADER_PREFIX.length() ), handler, strict );
        } else if ( lowerCaseName.equals( SIZE_NAME ) ) {
            selection = selectSize( contextName, partInfo, name, handler, strict );
        } else if ( strict ) {
            String msg = handler.handleBadVariable(name + " in " + contextName);
            throw new IllegalArgumentException(msg);
        } else {
            handler.handleBadVariable(name + " in " + contextName);
            selection = null;
        }

        return selection;
    }

    @Override
    public Class<PartInfo> getContextObjectClass() {
        return PartInfo.class;
    }

    //- PRIVATE

    private static final String BODY_NAME = "body";
    private static final String CONTENT_TYPE = "contenttype";
    private static final String HEADER_PREFIX = "header.";
    private static final String SIZE_NAME = "size";

    private static final int DEFAULT_PART_MAX_SIZE = 1024 * 1024;

    private static int getPartMaxSize() {
        return ConfigFactory.getIntProperty(ServerConfigParams.PARAM_TEMPLATE_PART_MAX_SIZE, DEFAULT_PART_MAX_SIZE);
    }

    private String name( final String root, final String suffix ) {
        String name = root +  "." + suffix;

        if ( name.endsWith( "." )) {
            name = name.substring( 0, name.length()-1 );
        }

        return name;
    }

    private Selection selectBody( final PartInfo partInfo,
                                  final Syntax.SyntaxErrorHandler handler,
                                  final boolean strict ) {
        InputStream in = null;
        try {
            final ContentTypeHeader contentTypeHeader = partInfo.getContentType();
            if ( contentTypeHeader.isTextualContentType() ) {
                byte[] bytes = IOUtils.slurpStream( in = partInfo.getInputStream(false), getPartMaxSize() );
                return new Selection(new String(bytes, contentTypeHeader.getEncoding()));
            } else {
                String msg = handler.handleBadVariable("Part is not text");
                if (strict) throw new IllegalArgumentException(msg);
                return null;
            }
        } catch ( NoSuchPartException e ) {
            String msg = handler.handleBadVariable("Part not available");
            if (strict) throw new IllegalArgumentException(msg);
            return null;
        } catch ( IOException e ) {
            String msg = handler.handleBadVariable("Part cannot be read");
            if (strict) throw new IllegalArgumentException(msg);
            return null;
        } finally {
            ResourceUtils.closeQuietly( in );
        }
    }

    private Selection selectContentType( final PartInfo partInfo ) {
        return new Selection( partInfo.getContentType().getFullValue() );        
    }

    private Selection selectHeader( final String contextName,
                                    final PartInfo partInfo,
                                    final String name,
                                    final Syntax.SyntaxErrorHandler handler,
                                    final boolean strict ) {
        final MimeHeader header = partInfo.getHeader(name);
        if ( header != null ) {
            return new Selection(header.getFullValue());
        } else {
            String msg = handler.handleBadVariable(name + " in " + contextName);
            if (strict) throw new IllegalArgumentException(msg);
            return null;
        }
    }

    private Selection selectSize( final String contextName,
                                  final PartInfo partInfo,
                                  final String name,
                                  final Syntax.SyntaxErrorHandler handler,
                                  final boolean strict ) {
        final Selection selection;
        Selection sizeSelection = null;
        String msg = null;
        try {
            sizeSelection = new Selection(partInfo.getActualContentLength());
        } catch ( IOException e) {
            msg = handler.handleBadVariable( name + " in " + contextName, e );
        } catch ( NoSuchPartException e ) {
            msg = handler.handleBadVariable( name + " in " + contextName, e );
        }
        if (strict && msg != null) throw new IllegalArgumentException("Unable to determine part length: " + msg);
        selection = sizeSelection;
        return selection;
    }

}
