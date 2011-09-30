package com.l7tech.server.policy.variable;

import com.l7tech.policy.variable.Syntax.SyntaxErrorHandler;
import com.l7tech.util.BuildInfo;

/**
 * Context variable object for build (version) information.
 */
final class BuildVersionContext {

    //- PUBLIC

    @Override
    public String toString() {
        return "";
    }

    //- PACKAGE

    static final BuildVersionContext BUILD_VERSION_CONTEXT = new BuildVersionContext();

    static final class BuildVersionContextSelector implements ExpandVariables.Selector<BuildVersionContext> {
        @Override
        public Selection select( final String contextName,
                                 final BuildVersionContext context,
                                 final String name,
                                 final SyntaxErrorHandler handler, final boolean strict ) {
            if ( SUFFIX_LABEL.equalsIgnoreCase( name ) ) {
                return new Selection( BuildInfo.getProductVersion() );
            } else if ( SUFFIX_DETAIL.equalsIgnoreCase( name ) ) {
                return new Selection( BuildInfo.getLongBuildString() );
            } else if ( SUFFIX_NUMBER.equalsIgnoreCase( name ) ) {
                return new Selection( BuildInfo.getBuildNumber() );
            } else if ( SUFFIX_VERSION.equalsIgnoreCase( name ) ) {
                return new Selection( BuildInfo.getFormalProductVersion() );
            } else if ( SUFFIX_VERSION_MAJOR.equalsIgnoreCase( name ) ) {
                return new Selection( BuildInfo.getProductVersionMajor() );
            } else if ( SUFFIX_VERSION_MINOR.equalsIgnoreCase( name ) ) {
                return new Selection( BuildInfo.getProductVersionMinor() );
            } else if ( SUFFIX_VERSION_SUBMINOR.equalsIgnoreCase( name ) ) {
                return new Selection( BuildInfo.getProductVersionSubMinor() );
            } else {
                String msg = handler.handleBadVariable(name + " for build version");
                if (strict) throw new IllegalArgumentException(msg);
                return null;
            }
        }

        @Override
        public Class<BuildVersionContext> getContextObjectClass() {
            return BuildVersionContext.class;
        }
    }

    //- PRIVATE

    private static final String SUFFIX_LABEL = "label";
    private static final String SUFFIX_DETAIL = "detail";
    private static final String SUFFIX_NUMBER = "number";
    private static final String SUFFIX_VERSION = "version";
    private static final String SUFFIX_VERSION_MAJOR = "version.major";
    private static final String SUFFIX_VERSION_MINOR = "version.minor";
    private static final String SUFFIX_VERSION_SUBMINOR = "version.subminor";

    private BuildVersionContext() {
    }

}
