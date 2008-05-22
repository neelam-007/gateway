package com.l7tech.server.ems;

import org.restlet.*;
import org.restlet.data.LocalReference;
import org.restlet.data.Request;
import org.restlet.data.Response;
import org.springframework.context.ApplicationContext;

/**
 * The Enterprise Manager Server application.
 */
public class EmsApplication extends Application {
    private final ApplicationContext springContext;

    public EmsApplication(Context parentContext, ApplicationContext springContext) {
        super(parentContext);
        if (springContext == null) throw new NullPointerException("springContext");
        this.springContext = springContext;
    }

    public Restlet createRoot() {
        Router router = new Router(getContext());
        final Directory cdir = new Directory(getContext(), new LocalReference("clap://thread/com/l7tech/server/ems/resources/public"));
        cdir.setNegotiateContent(false); // (classpath isn't listable)
        router.attach("/license", LicenseResource.class);
        router.attach("/images/", new Filter(getContext(), cdir) {
            // Strip query string since it will cause Directory to fail to match, since matches to clap: locations must be exact
            protected int beforeHandle(Request request, Response response) {
                request.getResourceRef().setQuery(null);
                return Filter.CONTINUE;
            }
        });
        router.attachDefault(EmsHomePage.class);
        return router;
    }

    /**
     * Get the Spring COntext for this application.
     *
     * @return the Spring ApplicationContext.  Never null.
     */
    public ApplicationContext getSpringContext() {
        return springContext;
    }
}
