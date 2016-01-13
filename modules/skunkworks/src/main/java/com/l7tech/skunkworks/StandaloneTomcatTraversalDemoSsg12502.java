package com.l7tech.skunkworks;

import org.apache.catalina.Engine;
import org.apache.catalina.Host;
import org.apache.catalina.connector.Connector;
import org.apache.catalina.core.StandardContext;
import org.apache.catalina.core.StandardWrapper;
import org.apache.catalina.loader.WebappClassLoader;
import org.apache.catalina.loader.WebappLoader;
import org.apache.catalina.servlets.DefaultServlet;
import org.apache.catalina.session.ManagerBase;
import org.apache.catalina.startup.Embedded;
import org.apache.naming.NamingContextBindingsEnumeration;
import org.apache.naming.NamingContextEnumeration;
import org.apache.naming.NamingEntry;
import org.apache.naming.resources.BaseDirContext;
import org.apache.naming.resources.FileDirContext;
import org.apache.naming.resources.Resource;
import org.apache.naming.resources.ResourceAttributes;
import org.apache.tomcat.util.IntrospectionUtils;

import javax.naming.*;
import javax.naming.directory.Attributes;
import javax.naming.directory.DirContext;
import javax.naming.directory.ModificationItem;
import javax.naming.directory.SearchControls;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Logger;

/**
 * A standalone demo server for the directory traversal issue.
 */
public class StandaloneTomcatTraversalDemoSsg12502 {
    protected static final Logger logger = Logger.getLogger( StandaloneTomcatTraversalDemoSsg12502.class.getName() );

    static final String RESOURCE_PREFIX = "com/l7tech/server/resources/";
    private Embedded embedded;
    private StandardContext context;

    public static void main( String[] args ) throws Exception {
        // Prereqs:  mkdir -p Gateway/runtime/web/ssg
        new StandaloneTomcatTraversalDemoSsg12502().start();
    }

    void start() throws Exception {
        embedded = new Embedded();
        Engine engine = embedded.createEngine();
        engine.setName("ssg");
        engine.setDefaultHost( "localhost" );
        embedded.addEngine(engine);

        File inf = new File( "web" );
        if (!inf.exists() || !inf.isDirectory())
            throw new RuntimeException("No such directory: " + inf.getPath());

        final String s = inf.getAbsolutePath();

        Host host = embedded.createHost( "localhost", s);
        engine.addChild(host);

        context = (StandardContext)embedded.createContext("", s);

        // Disable persistent session support
        context.setManager(new ManagerBase() {
            AtomicInteger rejects = new AtomicInteger(0);

            @Override
            public int getRejectedSessions() {
                return rejects.get();
            }

            @Override
            public void setRejectedSessions(int i) {
                rejects.set(i);
            }

            @Override
            public void load() throws ClassNotFoundException, IOException {
            }

            @Override
            public void unload() throws IOException {
            }
        });

        context.setName("");
        context.setResources( createHybridDirContext( inf ) );
        context.addMimeMapping("gif", "image/gif");
        context.addMimeMapping("png", "image/png");
        context.addMimeMapping("jpg", "image/jpeg");
        context.addMimeMapping("jpeg", "image/jpeg");
        context.addMimeMapping("htm", "text/html");
        context.addMimeMapping("html", "text/html");
        context.addMimeMapping("xml", "text/xml");
        context.addMimeMapping("txt", "text/plain");
        context.addMimeMapping("css", "text/css");

        StandardWrapper dflt = (StandardWrapper)context.createWrapper();
        dflt.setServletClass( DefaultServlet.class.getName() );
        dflt.setName("default");
        dflt.setLoadOnStartup(1);
        context.addChild(dflt);
        context.addServletMapping("/", "default");

        context.setParentClassLoader(getClass().getClassLoader());
        WebappLoader webappLoader = new WebappLoader(context.getParentClassLoader());
        webappLoader.setDelegate(context.getDelegate());
        webappLoader.setLoaderClass( WebappClassLoaderEx.class.getName() );
        context.setLoader(webappLoader);

        host.addChild(context);


        // Start server
        embedded.start();
        context.start();

        // Add HTTP connector
        Connector c = embedded.createConnector( (String)null, 8080, "http" );
        c.setEnableLookups(false);

        c.setAttribute( "disableUploadTimeout", "true" );
        c.setAttribute( "acceptCount", "100" );
        c.setAttribute( "connectionTimeout", "20000" );

        embedded.addConnector(c);
        c.start();

        // Wait forever
        Object o = new Object();
        synchronized ( o ) {
            o.wait();
        }
    }



    public static final class WebappClassLoaderEx extends WebappClassLoader {
        public WebappClassLoaderEx() {
        }

        public WebappClassLoaderEx( final ClassLoader parent ) {
            super( parent );
        }

        /**
         * Overridden to prevent cleanup that is not necessary in our environment.
         */
        @Override
        protected void clearReferences() {
            IntrospectionUtils.clear();
            org.apache.juli.logging.LogFactory.release(this);
            java.beans.Introspector.flushCaches();
        }
    }
    /**
     * Build the hybrid virtual/real DirContext that gets WEB-INF virtually from the class path
     * but everything else from the specified inf directory on disk.
     *
     * @param inf  the /ssg/etc/inf directory.  required
     * @return     a new DirContext that will contain a virtual WEB-INF in addition to the real contents of /ssg/etc/inf/ssg
     */
    private DirContext createHybridDirContext(File inf) {
        // Splice the real on-disk ssg/ subdirectory into our virtual filesystem under /ssg
        File ssgFile = new File(inf, "ssg");
        FileDirContext ssgFileContext = new FileDirContext();
        ssgFileContext.setDocBase(ssgFile.getAbsolutePath());
        VirtualDirContext ssgContext = new VirtualDirContext("ssg", ssgFileContext);

        // Set up our virtual WEB-INF subdirectory
        List<VirtualDirEntry> webinfEntries = new ArrayList<VirtualDirEntry>();

        byte[] webXmlBytes = loadWebXmlBytesFromClassPathResource();
        webinfEntries.add( new VirtualDirEntryImpl( "web.xml", webXmlBytes ) );

        VirtualDirContext webInfContext = new VirtualDirContext("WEB-INF", webinfEntries.toArray(new VirtualDirEntry[webinfEntries.size()]));

        // Splice it all together
        return new VirtualDirContext("VirtualInf", webInfContext, ssgContext);
    }


    private byte[] loadWebXmlBytesFromClassPathResource() {
        // TODO In the actual product, the web.xml file contents are loaded as a class path resource from
        //      an effectively-static path, built at server init time as below.
        //      For purposes of this single-source-file standalone demo we will simulate this
        //      by just using a hardcoded web.xml file in the source.
        // TODO we would normally use something like this:
        //   String resourcePath = RESOURCE_PREFIX + "web.xml";
        //   InputStream is = getClass().getClassLoader().getResourceAsStream( resourcePath );
        //   byte[] resourceBytes = readEntireInputStream( is );

        // For demo purposes, use a simplified web.xml.  Pieces omitted to make demo simple.
        // The com.l7tech.server.transport.http.HttpNamespaceFilter is relevant to why
        // creating a /* service works around the issue.
        String webxml =
                "<?xml version=\"1.0\" encoding=\"ISO-8859-1\"?>\n" +
                "<!-- The ssg (aka UneasyRooster) web application DD -->\n" +
                "<!-- The ssg (aka UneasyRooster) web application DD -->\n" +
                "<web-app\n" +
                "    xmlns=\"http://java.sun.com/xml/ns/javaee\"\n" +
                "    xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"\n" +
                "    xsi:schemaLocation=\"http://java.sun.com/xml/ns/javaee http://java.sun.com/xml/ns/javaee/web-app_3_0.xsd\"\n" +
                "    version=\"3.0\">\n" +
                "    <display-name>Layer7 Secure Span Gateway</display-name>\n" +
                "\n" +
                "    <!--  Spring configuration file\n" +
                "          used by the org.springframework.web.context.ContextLoaderListener\n" +
                "    -->\n" +
                "    <context-param>\n" +
                "        <param-name>contextConfigLocation</param-name>\n" +
                "        <param-value>\n" +
                "            classpath:com/l7tech/server/transport/http/resources/webApplicationContext.xml\n" +
                "        </param-value>\n" +
                "    </context-param>\n" +
//                "\n" +
//                "    <filter>\n" +
//                "        <filter-name>diagnostic-context</filter-name>\n" +
//                "        <filter-class>com.l7tech.server.log.HybridDiagnosticContextServletFilter</filter-class>\n" +
//                "    </filter>\n" +
//                "\n" +
//                "    <!--\n" +
//                "    Map all requests that would otherwise be 404s to the SOAP servlet, for\n" +
//                "    transparency purposes\n" +
//                "    -->\n" +
//                "    <filter>\n" +
//                "        <filter-name>namespace</filter-name>\n" +
//                "        <filter-class>com.l7tech.server.transport.http.HttpNamespaceFilter</filter-class>\n" +
//                "        <init-param>\n" +
//                "            <param-name>passthroughPrefixes</param-name>\n" +
//                "            <param-value>/ssg</param-value>\n" +
//                "        </init-param>\n" +
//                "    </filter>\n" +
//                "\n" +
//                "    <filter>\n" +
//                "        <filter-name>input-timeout</filter-name>\n" +
//                "        <filter-class>com.l7tech.server.transport.http.InputTimeoutFilter</filter-class>\n" +
//                "        <init-param> <!-- how long to wait on a read -->\n" +
//                "            <param-name>blocked-read-timeout</param-name>\n" +
//                "            <param-value>30000</param-value>\n" +
//                "        </init-param>\n" +
//                "        <init-param> <!-- milliseconds before slow read check comes into effect -->\n" +
//                "            <param-name>slow-read-timeout</param-name>\n" +
//                "            <param-value>30000</param-value>\n" +
//                "        </init-param> \n" +
//                "        <init-param><!-- in bytes per second -->\n" +
//                "            <param-name>slow-read-throughput</param-name>\n" +
//                "            <param-value>1024</param-value>\n" +
//                "        </init-param>\n" +
//                "    </filter>\n" +
//                "\n" +
//                "    <filter>\n" +
//                "        <filter-name>managerRemotingFilter</filter-name>\n" +
//                "        <filter-class>com.l7tech.gateway.common.spring.remoting.http.SecureHttpFilter</filter-class>\n" +
//                "        <init-param>\n" +
//                "            <param-name>endpoint-names</param-name>\n" +
//                "            <param-value>ADMIN_APPLET, ADMIN_REMOTE</param-value>\n" +
//                "        </init-param>\n" +
//                "    </filter>\n" +
//                "\n" +
//                "    <filter>\n" +
//                "        <filter-name>clusterRemotingFilter</filter-name>\n" +
//                "        <filter-class>com.l7tech.gateway.common.spring.remoting.http.SecureHttpFilter</filter-class>\n" +
//                "        <init-param>\n" +
//                "            <param-name>endpoint-names</param-name>\n" +
//                "            <param-value>NODE_COMMUNICATION</param-value>\n" +
//                "        </init-param>\n" +
//                "    </filter>\n" +
//                "\n" +
//                "    <filter>\n" +
//                "        <filter-name>remotingFilter</filter-name>\n" +
//                "        <filter-class>com.l7tech.gateway.common.spring.remoting.http.SecureHttpFilter</filter-class>\n" +
//                "    </filter>\n" +
//                "\n" +
//                "    <filter>\n" +
//                "        <filter-name>wsdlFilter</filter-name>\n" +
//                "        <filter-class>com.l7tech.server.WsdlFilter</filter-class>\n" +
//                "        <init-param>\n" +
//                "            <param-name>passthroughPrefixes</param-name>\n" +
//                "            <param-value>/ssg/</param-value>\n" +
//                "        </init-param>\n" +
//                "        <init-param>\n" +
//                "            <param-name>wsdl-forward-uri</param-name>\n" +
//                "            <param-value>/ssg/wsdl?uri={0}</param-value>\n" +
//                "        </init-param>\n" +
//                "    </filter>\n" +
//                "\n" +
//                "    <filter>\n" +
//                "        <filter-name>connectionIdFilter</filter-name>\n" +
//                "        <filter-class>com.l7tech.server.transport.http.ConnectionIdFilter</filter-class>\n" +
//                "    </filter>\n" +
//                "\n" +
//                "    <filter>\n" +
//                "        <filter-name>managerAppletFilter</filter-name>\n" +
//                "        <filter-class>com.l7tech.server.admin.ManagerAppletFilter</filter-class>\n" +
//                "    </filter>\n" +
//                "\n" +
//                "   <filter>\n" +
//                "        <filter-name>allCxfApiFilter</filter-name>\n" +
//                "        <filter-class>com.l7tech.gateway.common.transport.http.FeatureFilter</filter-class>\n" +
//                "        <init-param>\n" +
//                "            <param-name>endpoint-names</param-name>\n" +
//                "            <param-value>ADMIN_REMOTE_ESM, PC_NODE_API</param-value>\n" +
//                "        </init-param>\n" +
//                "    </filter>\n" +
//                "\n" +
//                "    <filter>\n" +
//                "        <filter-name>esmApiFilter</filter-name>\n" +
//                "        <filter-class>com.l7tech.gateway.common.transport.http.FeatureFilter</filter-class>\n" +
//                "        <init-param>\n" +
//                "            <param-name>endpoint-names</param-name>\n" +
//                "            <param-value>ADMIN_REMOTE_ESM</param-value>\n" +
//                "        </init-param>\n" +
//                "    </filter>\n" +
//                "\n" +
//                "    <filter>\n" +
//                "        <filter-name>pcNodeApiFilter</filter-name>\n" +
//                "        <filter-class>com.l7tech.gateway.common.transport.http.FeatureFilter</filter-class>\n" +
//                "        <init-param>\n" +
//                "            <param-name>endpoint-names</param-name>\n" +
//                "            <param-value>PC_NODE_API</param-value>\n" +
//                "        </init-param>\n" +
//                "    </filter>\n" +
//                "\n" +
//                "    <filter-mapping>\n" +
//                "        <filter-name>diagnostic-context</filter-name>\n" +
//                "        <url-pattern>/*</url-pattern>\n" +
//                "    </filter-mapping>\n" +
//                "\n" +
//                "    <filter-mapping>\n" +
//                "        <filter-name>input-timeout</filter-name>\n" +
//                "        <url-pattern>/*</url-pattern>\n" +
//                "    </filter-mapping>\n" +
//                "\n" +
//                "    <filter-mapping>\n" +
//                "        <filter-name>connectionIdFilter</filter-name>\n" +
//                "        <url-pattern>/*</url-pattern>\n" +
//                "    </filter-mapping>\n" +
//                "\n" +
//                "    <filter-mapping>\n" +
//                "        <filter-name>wsdlFilter</filter-name>\n" +
//                "        <url-pattern>/*</url-pattern>\n" +
//                "    </filter-mapping>\n" +
//                "\n" +
//                "    <filter-mapping>\n" +
//                "        <filter-name>namespace</filter-name>\n" +
//                "        <url-pattern>/*</url-pattern>\n" +
//                "    </filter-mapping>\n" +
//                "\n" +
//                "    <filter-mapping>\n" +
//                "        <filter-name>managerRemotingFilter</filter-name>\n" +
//                "        <url-pattern>/ssg/manager/*</url-pattern>\n" +
//                "    </filter-mapping>\n" +
//                "\n" +
//                "    <filter-mapping>\n" +
//                "        <filter-name>clusterRemotingFilter</filter-name>\n" +
//                "        <url-pattern>/ssg/cluster/*</url-pattern>\n" +
//                "    </filter-mapping>\n" +
//                "\n" +
//                "    <filter-mapping>\n" +
//                "        <filter-name>remotingFilter</filter-name>\n" +
//                "        <url-pattern>/ssg/services/*</url-pattern>\n" +
//                "    </filter-mapping>\n" +
//                "\n" +
//                "    <filter-mapping>\n" +
//                "        <filter-name>managerAppletFilter</filter-name>\n" +
//                "        <servlet-name>managerAppletServlet</servlet-name>\n" +
//                "    </filter-mapping>\n" +
//                "\n" +
//                "    <filter-mapping>\n" +
//                "        <filter-name>managerAppletFilter</filter-name>\n" +
//                "        <url-pattern>/ssg/webadmin/*</url-pattern>\n" +
//                "    </filter-mapping>\n" +
//                "\n" +
//                "    <filter-mapping>\n" +
//                "        <filter-name>allCxfApiFilter</filter-name>\n" +
//                "        <url-pattern>/ssg/services/*</url-pattern>\n" +
//                "    </filter-mapping>\n" +
//                "\n" +
//                "    <filter-mapping>\n" +
//                "        <filter-name>pcNodeApiFilter</filter-name>\n" +
//                "        <url-pattern>/ssg/services/processControllerNodeApi</url-pattern>\n" +
//                "    </filter-mapping>\n" +
//                "\n" +
//                "    <filter-mapping>\n" +
//                "        <filter-name>esmApiFilter</filter-name>\n" +
//                "        <url-pattern>/ssg/services/gatewayApi</url-pattern>\n" +
//                "    </filter-mapping>\n" +
//                "\n" +
//                "    <filter-mapping>\n" +
//                "        <filter-name>esmApiFilter</filter-name>\n" +
//                "        <url-pattern>/ssg/services/reportApi</url-pattern>\n" +
//                "    </filter-mapping>\n" +
//                "\n" +
//                "    <filter-mapping>\n" +
//                "        <filter-name>esmApiFilter</filter-name>\n" +
//                "        <url-pattern>/ssg/services/migrationApi</url-pattern>\n" +
//                "    </filter-mapping>\n" +
//                "\n" +
//                "    <!--\n" +
//                "         Spring context loader listener that loads the spring beans.\n" +
//                "         This version ensures that the parent context is the Gateway's global ApplicationContext.\n" +
//                "     -->\n" +
//                "    <listener>\n" +
//                "        <listener-class>com.l7tech.server.transport.http.SsgContextLoaderListener</listener-class>\n" +
//                "    </listener>\n" +
//                "\n" +
//                "    <servlet>\n" +
//                "        <servlet-name>error</servlet-name>\n" +
//                "        <servlet-class>com.l7tech.server.ErrorServlet</servlet-class>\n" +
//                "    </servlet>\n" +
//                "\n" +
//                "    <servlet>\n" +
//                "        <servlet-name>ping</servlet-name>\n" +
//                "        <servlet-class>com.l7tech.server.PingServlet</servlet-class>\n" +
//                "    </servlet>\n" +
//                "\n" +
//                "    <servlet>\n" +
//                "        <servlet-name>SnmpQueryServlet</servlet-name>\n" +
//                "        <servlet-class>com.l7tech.server.SnmpQueryServlet</servlet-class>\n" +
//                "    </servlet>\n" +
//                "\n" +
//                "    <servlet>\n" +
//                "        <servlet-name>PolicyServlet</servlet-name>\n" +
//                "        <servlet-class>com.l7tech.server.policy.PolicyServlet</servlet-class>\n" +
//                "    </servlet>\n" +
//                "\n" +
//                "    <servlet>\n" +
//                "        <servlet-name>CSRServlet</servlet-name>\n" +
//                "        <servlet-class>com.l7tech.server.identity.cert.CSRHandler</servlet-class>\n" +
//                "    </servlet>\n" +
//                "\n" +
//                "    <servlet>\n" +
//                "        <servlet-name>SoapMessageProcessingServlet</servlet-name>\n" +
//                "        <servlet-class>com.l7tech.server.SoapMessageProcessingServlet</servlet-class>\n" +
//                "\n" +
//                "        <init-param>\n" +
//                "            <param-name>PolicyServletUri</param-name>\n" +
//                "            <param-value>/ssg/policy/disco?serviceoid=</param-value>\n" +
//                "        </init-param>\n" +
//                "    </servlet>\n" +
//                "\n" +
//                "    <servlet>\n" +
//                "        <servlet-name>WsdlProxy</servlet-name>\n" +
//                "        <servlet-class>com.l7tech.server.WsdlProxyServlet</servlet-class>\n" +
//                "    </servlet>\n" +
//                "\n" +
//                "    <servlet>\n" +
//                "        <servlet-name>TokenService</servlet-name>\n" +
//                "        <servlet-class>com.l7tech.server.TokenServiceServlet</servlet-class>\n" +
//                "    </servlet>\n" +
//                "\n" +
//                "    <servlet>\n" +
//                "        <servlet-name>PasswdService</servlet-name>\n" +
//                "        <servlet-class>com.l7tech.server.PasswdServlet</servlet-class>\n" +
//                "    </servlet>\n" +
//                "\n" +
//                "    <servlet>\n" +
//                "        <servlet-name>admin</servlet-name>\n" +
//                "        <servlet-class>org.springframework.web.servlet.DispatcherServlet</servlet-class>\n" +
//                "        <init-param>\n" +
//                "            <param-name>contextConfigLocation</param-name>\n" +
//                "            <param-value>classpath:com/l7tech/server/resources/admin-servlet.xml</param-value>\n" +
//                "        </init-param>\n" +
//                "    </servlet>\n" +
//                "\n" +
//                "    <servlet>\n" +
//                "        <servlet-name>cluster</servlet-name>\n" +
//                "        <servlet-class>org.springframework.web.servlet.DispatcherServlet</servlet-class>\n" +
//                "        <init-param>\n" +
//                "            <param-name>contextConfigLocation</param-name>\n" +
//                "            <param-value>classpath:com/l7tech/server/resources/cluster-servlet.xml</param-value>\n" +
//                "        </init-param>\n" +
//                "    </servlet>\n" +
//                "\n" +
//                "    <servlet>\n" +
//                "        <servlet-name>managerAppletServlet</servlet-name>\n" +
//                "        <servlet-class>com.l7tech.server.admin.ManagerAppletServlet</servlet-class>\n" +
//                "    </servlet>\n" +
//                "\n" +
//                "    <servlet>\n" +
//                "        <servlet-name>backup</servlet-name>\n" +
//                "        <servlet-class>com.l7tech.server.BackupServlet</servlet-class>\n" +
//                "    </servlet>\n" +
//                "\n" +
//                "    <servlet>\n" +
//                "        <servlet-name>ssgLoginFormServlet</servlet-name>\n" +
//                "        <servlet-class>com.l7tech.server.admin.SSGLoginFormServlet</servlet-class>\n" +
//                "        <init-param>\n" +
//                "            <param-name>loginPage</param-name>\n" +
//                "            <param-value>/com/l7tech/server/resources/ssglogin.html</param-value>\n" +
//                "        </init-param>\n" +
//                "    </servlet>\n" +
//                "\n" +
//                "    <!-- Servlet to serve changing the password and login in one pass -->\n" +
//                "    <servlet>\n" +
//                "        <servlet-name>ssgLoginAndPasswordUpdateFormServlet</servlet-name>\n" +
//                "        <servlet-class>com.l7tech.server.admin.SSGLoginFormServlet</servlet-class>\n" +
//                "        <init-param>\n" +
//                "            <param-name>loginPage</param-name>\n" +
//                "            <param-value>/com/l7tech/server/resources/ssglogin_passwordupdate.html</param-value>\n" +
//                "        </init-param>\n" +
//                "    </servlet>\n" +
//                "\n" +
//                "    <servlet>\n" +
//                "        <servlet-name>fileDownloadServlet</servlet-name>\n" +
//                "        <servlet-class>com.l7tech.server.FileDownloadServlet</servlet-class>\n" +
//                "    </servlet>\n" +
//                "\n" +
//                "    <servlet>\n" +
//                "        <servlet-name>esmTrustServlet</servlet-name>\n" +
//                "        <servlet-class>com.l7tech.server.EsmTrustServlet</servlet-class>\n" +
//                "    </servlet>\n" +
//                "\n" +
//                "    <servlet>\n" +
//                "        <servlet-name>CXFServlet</servlet-name>\n" +
//                "        <display-name>CXF Servlet</display-name>\n" +
//                "        <init-param>\n" +
//                "            <param-name>hide-service-list-page</param-name>\n" +
//                "            <param-value>true</param-value>\n" +
//                "        </init-param>\n" +
//                "        <servlet-class>\n" +
//                "            org.apache.cxf.transport.servlet.CXFServlet\n" +
//                "        </servlet-class>\n" +
//                "        <load-on-startup>1</load-on-startup>\n" +
//                "    </servlet>\n" +
//                "\n" +
//                "    <servlet-mapping>\n" +
//                "        <servlet-name>fileDownloadServlet</servlet-name>\n" +
//                "        <url-pattern>/ssg/webadmin/filedownload</url-pattern>\n" +
//                "    </servlet-mapping>\n" +
//                "\n" +
//                "    <servlet-mapping>\n" +
//                "        <servlet-name>admin</servlet-name>\n" +
//                "        <url-pattern>/ssg/manager/*</url-pattern>\n" +
//                "    </servlet-mapping>\n" +
//                "\n" +
//                "    <servlet-mapping>\n" +
//                "        <servlet-name>cluster</servlet-name>\n" +
//                "        <url-pattern>/ssg/cluster/*</url-pattern>\n" +
//                "    </servlet-mapping>\n" +
//                "\n" +
//                "    <servlet-mapping>\n" +
//                "        <servlet-name>error</servlet-name>\n" +
//                "        <url-pattern>/ssg/error</url-pattern>\n" +
//                "    </servlet-mapping>\n" +
//                "\n" +
//                "    <servlet-mapping>\n" +
//                "        <servlet-name>error</servlet-name>\n" +
//                "        <url-pattern>/ssg</url-pattern>\n" +
//                "    </servlet-mapping>\n" +
//                "\n" +
//                "    <servlet-mapping>\n" +
//                "        <servlet-name>SoapMessageProcessingServlet</servlet-name>\n" +
//                "        <url-pattern>/ssg/servlet/soap</url-pattern>\n" +
//                "    </servlet-mapping>\n" +
//                "\n" +
//                "    <servlet-mapping>\n" +
//                "        <servlet-name>SoapMessageProcessingServlet</servlet-name>\n" +
//                "        <url-pattern>/ssg/soap</url-pattern>\n" +
//                "    </servlet-mapping>\n" +
//                "\n" +
//                "    <servlet-mapping>\n" +
//                "        <servlet-name>ping</servlet-name>\n" +
//                "        <url-pattern>/ssg/ping</url-pattern>\n" +
//                "    </servlet-mapping>\n" +
//                "\n" +
//                "    <!--\n" +
//                "        NOTE: Make sure ping servlet has a mapping for every url-pattern defined in com.l7tech.server.PingServlet.\n" +
//                "    -->\n" +
//                "    <servlet-mapping>\n" +
//                "        <servlet-name>ping</servlet-name>\n" +
//                "        <url-pattern>/ssg/ping/systemInfo</url-pattern>\n" +
//                "    </servlet-mapping>\n" +
//                "\n" +
//                "    <servlet-mapping>\n" +
//                "        <servlet-name>SnmpQueryServlet</servlet-name>\n" +
//                "        <url-pattern>/ssg/management/*</url-pattern>\n" +
//                "    </servlet-mapping>\n" +
//                "\n" +
//                "    <servlet-mapping>\n" +
//                "        <servlet-name>PolicyServlet</servlet-name>\n" +
//                "        <!--\n" +
//                "        NB: Update the PolicyServletUri <init-param> in the SoapMessageProcessingServlet\n" +
//                "        above when you change this <url-pattern>!\n" +
//                "        -->\n" +
//                "        <url-pattern>/ssg/policy/disco/*</url-pattern>\n" +
//                "    </servlet-mapping>\n" +
//                "\n" +
//                "    <!-- A PolicyServlet bound to disco.modulator for server cert disco and pre-3.2 SSB policy disco. -->\n" +
//                "    <servlet-mapping>\n" +
//                "        <servlet-name>PolicyServlet</servlet-name>\n" +
//                "        <url-pattern>/ssg/policy/disco.modulator/*</url-pattern>\n" +
//                "    </servlet-mapping>\n" +
//                "\n" +
//                "    <servlet-mapping>\n" +
//                "        <servlet-name>CSRServlet</servlet-name>\n" +
//                "        <url-pattern>/ssg/csr/*</url-pattern>\n" +
//                "    </servlet-mapping>\n" +
//                "\n" +
//                "    <servlet-mapping>\n" +
//                "        <servlet-name>WsdlProxy</servlet-name>\n" +
//                "        <url-pattern>/ssg/wsdl</url-pattern>\n" +
//                "    </servlet-mapping>\n" +
//                "\n" +
//                "    <servlet-mapping>\n" +
//                "        <servlet-name>WsdlProxy</servlet-name>\n" +
//                "        <url-pattern>/ssg/wsdl/*</url-pattern>\n" +
//                "    </servlet-mapping>\n" +
//                "\n" +
//                "    <servlet-mapping>\n" +
//                "        <servlet-name>PasswdService</servlet-name>\n" +
//                "        <url-pattern>/ssg/passwd</url-pattern>\n" +
//                "    </servlet-mapping>\n" +
//                "\n" +
//                "    <servlet-mapping>\n" +
//                "        <servlet-name>WsdlProxy</servlet-name>\n" +
//                "        <url-pattern>/ssg/wsil</url-pattern>\n" +
//                "    </servlet-mapping>\n" +
//                "\n" +
//                "    <servlet-mapping>\n" +
//                "        <servlet-name>TokenService</servlet-name>\n" +
//                "        <url-pattern>/ssg/token</url-pattern>\n" +
//                "    </servlet-mapping>\n" +
//                "\n" +
//                "    <servlet-mapping>\n" +
//                "        <servlet-name>managerAppletServlet</servlet-name>\n" +
//                "        <url-pattern>/ssg/webadmin</url-pattern>\n" +
//                "    </servlet-mapping>\n" +
//                "\n" +
//                "    <servlet-mapping>\n" +
//                "        <servlet-name>managerAppletServlet</servlet-name>\n" +
//                "        <url-pattern>/ssg/webadmin/</url-pattern>\n" +
//                "    </servlet-mapping>\n" +
//                "\n" +
//                "    <servlet-mapping>\n" +
//                "        <servlet-name>backup</servlet-name>\n" +
//                "        <url-pattern>/ssg/backup</url-pattern>\n" +
//                "    </servlet-mapping>\n" +
//                "\n" +
//                "    <servlet-mapping>\n" +
//                "        <servlet-name>esmTrustServlet</servlet-name>\n" +
//                "        <url-pattern>/ssg/esmtrust</url-pattern>\n" +
//                "    </servlet-mapping>\n" +
//                "\n" +
//                "    <servlet-mapping>\n" +
//                "        <servlet-name>esmTrustServlet</servlet-name>\n" +
//                "        <url-pattern>/ssg/esmtrust/*</url-pattern>\n" +
//                "    </servlet-mapping>\n" +
//                "\n" +
//                "    <servlet-mapping>\n" +
//                "        <servlet-name>CXFServlet</servlet-name>\n" +
//                "        <url-pattern>/ssg/services/*</url-pattern>\n" +
//                "    </servlet-mapping>\n" +
//                "\n" +
                "    <welcome-file-list>\n" +
                "        <welcome-file>index.html</welcome-file>\n" +
                "    </welcome-file-list>\n" +
                "\n" +
                "    <error-page>\n" +
                "        <error-code>400</error-code>\n" +
                "        <location>/ssg/error</location>\n" +
                "    </error-page>\n" +
                "    <error-page>\n" +
                "        <error-code>401</error-code>\n" +
                "        <location>/ssg/error</location>\n" +
                "    </error-page>\n" +
                "    <error-page>\n" +
                "        <error-code>402</error-code>\n" +
                "        <location>/ssg/error</location>\n" +
                "    </error-page>\n" +
                "    <error-page>\n" +
                "        <error-code>403</error-code>\n" +
                "        <location>/ssg/error</location>\n" +
                "    </error-page>\n" +
                "    <error-page>\n" +
                "        <error-code>404</error-code>\n" +
                "        <location>/ssg/error</location>\n" +
                "    </error-page>\n" +
                "    <error-page>\n" +
                "        <error-code>405</error-code>\n" +
                "        <location>/ssg/error</location>\n" +
                "    </error-page>\n" +
                "    <error-page>\n" +
                "        <error-code>406</error-code>\n" +
                "        <location>/ssg/error</location>\n" +
                "    </error-page>\n" +
                "    <error-page>\n" +
                "        <error-code>407</error-code>\n" +
                "        <location>/ssg/error</location>\n" +
                "    </error-page>\n" +
                "    <error-page>\n" +
                "        <error-code>408</error-code>\n" +
                "        <location>/ssg/error</location>\n" +
                "    </error-page>\n" +
                "    <error-page>\n" +
                "        <error-code>409</error-code>\n" +
                "        <location>/ssg/error</location>\n" +
                "    </error-page>\n" +
                "    <error-page>\n" +
                "        <error-code>410</error-code>\n" +
                "        <location>/ssg/error</location>\n" +
                "    </error-page>\n" +
                "    <error-page>\n" +
                "        <error-code>411</error-code>\n" +
                "        <location>/ssg/error</location>\n" +
                "    </error-page>\n" +
                "    <error-page>\n" +
                "        <error-code>412</error-code>\n" +
                "        <location>/ssg/error</location>\n" +
                "    </error-page>\n" +
                "    <error-page>\n" +
                "        <error-code>413</error-code>\n" +
                "        <location>/ssg/error</location>\n" +
                "    </error-page>\n" +
                "    <error-page>\n" +
                "        <error-code>414</error-code>\n" +
                "        <location>/ssg/error</location>\n" +
                "    </error-page>\n" +
                "    <error-page>\n" +
                "        <error-code>415</error-code>\n" +
                "        <location>/ssg/error</location>\n" +
                "    </error-page>\n" +
                "    <error-page>\n" +
                "        <error-code>416</error-code>\n" +
                "        <location>/ssg/error</location>\n" +
                "    </error-page>\n" +
                "    <error-page>\n" +
                "        <error-code>417</error-code>\n" +
                "        <location>/ssg/error</location>\n" +
                "    </error-page>\n" +
                "    <error-page>\n" +
                "        <error-code>422</error-code>\n" +
                "        <location>/ssg/error</location>\n" +
                "    </error-page>\n" +
                "    <error-page>\n" +
                "        <error-code>423</error-code>\n" +
                "        <location>/ssg/error</location>\n" +
                "    </error-page>\n" +
                "    <error-page>\n" +
                "        <error-code>500</error-code>\n" +
                "        <location>/ssg/error</location>\n" +
                "    </error-page>\n" +
                "    <error-page>\n" +
                "        <error-code>501</error-code>\n" +
                "        <location>/ssg/error</location>\n" +
                "    </error-page>\n" +
                "    <error-page>\n" +
                "        <error-code>502</error-code>\n" +
                "        <location>/ssg/error</location>\n" +
                "    </error-page>\n" +
                "    <error-page>\n" +
                "        <error-code>503</error-code>\n" +
                "        <location>/ssg/error</location>\n" +
                "    </error-page>\n" +
                "    <error-page>\n" +
                "        <error-code>504</error-code>\n" +
                "        <location>/ssg/error</location>\n" +
                "    </error-page>\n" +
                "    <error-page>\n" +
                "        <error-code>505</error-code>\n" +
                "        <location>/ssg/error</location>\n" +
                "    </error-page>\n" +
                "    <error-page>\n" +
                "        <error-code>507</error-code>\n" +
                "        <location>/ssg/error</location>\n" +
                "    </error-page>\n" +
                "    <error-page>\n" +
                "        <exception-type>java.lang.Exception</exception-type>\n" +
                "        <location>/ssg/error</location>\n" +
                "    </error-page>\n" +
                "\n" +
//                "    <env-entry>\n" +
//                "        <description>List of concrete ServiceResolver implementations</description>\n" +
//                "        <env-entry-name>ServiceResolvers</env-entry-name>\n" +
//                "        <env-entry-value>\n" +
//                "            com.l7tech.server.service.resolution.SoapActionResolver\n" +
//                "            com.l7tech.server.service.resolution.UrnResolver\n" +
//                "            com.l7tech.server.service.resolution.UriResolver\n" +
//                "        </env-entry-value>\n" +
//                "        <env-entry-type>java.lang.String</env-entry-type>\n" +
//                "    </env-entry>\n" +
                "\n" +
                "</web-app>\n";

        return webxml.getBytes( StandardCharsets.UTF_8 );
    }


    // Represents a directory in our virtual file system
    public static class VirtualDirContext extends BaseDirContext implements VirtualDirEntry {
        private final Map<String, VirtualDirEntry> entries;
        private final DirContext delegate;
        private final VirtualDirEntryImpl thisEntry;

        /**
         * Create a VirtualDirContext that provides access to the specified virtual directory entries.
         *
         * @param localName the local name of this entry within its parent directory, ie "lib", or the empty
         *                  string if this is a new virtual filesystem root directory.
         * @param entries zero or more directory entries to make available in this virtual directory.
         */
        public VirtualDirContext(String localName, VirtualDirEntry... entries) {
            this.entries = new LinkedHashMap<String, VirtualDirEntry>();
            for (VirtualDirEntry entry : entries) {
                this.entries.put(entry.getLocalName(), entry);
                entry.setParent(this);
            }
            this.delegate = null;
            this.thisEntry = new VirtualDirEntryImpl(localName, this);
        }

        /**
         * Adapt the specified delegate DirContext into a VirtualDirContext so it can be mounted underneath
         * another VirtualDirContext instance.
         *
         * @param localName the local name of this entry within its parent directory, ie "lib"
         * @param delegate the DirContext from which files and subdirectories will be taken
         */
        public VirtualDirContext(String localName, DirContext delegate) {
            this.entries = null;
            this.delegate = delegate;
            this.thisEntry = new VirtualDirEntryImpl(localName, this);
        }

        public Object lookup(String name) throws NamingException {
            return lookup(new CompositeName(name));
        }

        public Object lookup(Name name) throws NamingException {
            if (name.isEmpty())
                return this;
            VirtualDirEntry entry = treeLookup(name);
            if (entry == null)
                throw new NamingException("Resource not found: " + name);
            if (entry instanceof DirContext)
                return entry;
            else
                return entry.getFileResource();
        }

        private VirtualDirEntry treeLookup(Name name) {
            if (name.isEmpty())
                return this;
            VirtualDirEntry currentEntry = this;
            for (int i = 0; i < name.size(); i++) {
                if (name.get(i).length() == 0)
                    continue;
                if (!currentEntry.isDirectory())
                    return null;
                VirtualDirContext dir = currentEntry.getDirectory();
                currentEntry = dir.getImmediateChild(name.get(i));
                if (currentEntry == null)
                    return null;
            }
            return currentEntry;
        }

        private VirtualDirEntry getImmediateChild(final String kidLocalName) {
            if (entries != null)
                return entries.get(kidLocalName);
            if (delegate == null)
                throw new IllegalStateException("VirtualDirContext has neither entries nor a delegate");

            VirtualDirEntry ret = null;
            try {
                final Object got = delegate.lookup(kidLocalName);
                ret = makeEntry(kidLocalName, got);
            } catch (NamingException e) {
                // FALLTHROUGH and return null
            }
            return ret;
        }

        private VirtualDirEntry makeEntry(final String objectsLocalName, final Object lookedUpObject) {
            VirtualDirEntry ret = null;
            if (lookedUpObject instanceof Resource ) {
                ret = new VirtualDirEntryImpl(objectsLocalName) {
                    protected Resource findResource() {
                        return (Resource)lookedUpObject;
                    }

                    protected Attributes findAttributes() throws NamingException {
                        return delegate.getAttributes(objectsLocalName);
                    }
                };
            } else if (lookedUpObject instanceof DirContext) {
                ret = new VirtualDirContext(objectsLocalName, (DirContext)lookedUpObject);
            }
            return ret;
        }

        public NamingEnumeration list(Name name) throws NamingException {
            if (name.isEmpty())
                return new NamingContextEnumeration(list(this).iterator());
            VirtualDirEntry entry = treeLookup(name);
            if (entry == null)
                throw new NamingException
                        (sm.getString("resources.notFound", name));

            return new NamingContextEnumeration(list(entry).iterator());
        }

        public NamingEnumeration listBindings(Name name) throws NamingException {
            if (name.isEmpty())
                return new NamingContextBindingsEnumeration(list(this).iterator(), this);
            VirtualDirEntry entry = treeLookup(name);
            if (entry == null)
                throw new NamingException(sm.getString("resources.notFound", name));
            return new NamingContextBindingsEnumeration(list(entry).iterator(), this);
        }

        public void unbind(String name) throws NamingException {
            throw new OperationNotSupportedException();
        }

        public void rename(String oldName, String newName) throws NamingException {
            throw new OperationNotSupportedException();
        }

        public NamingEnumeration list(String name) throws NamingException {
            return list(new CompositeName(name));
        }

        public NamingEnumeration listBindings(String name) throws NamingException {
            return listBindings(new CompositeName(name));
        }

        public void destroySubcontext(String name) throws NamingException {
            throw new OperationNotSupportedException();
        }

        public Object lookupLink(String name) throws NamingException {
            // Links not supported here; turn into normal lookup
            return lookup(name);
        }

        public String getNameInNamespace() throws NamingException {
            if (docBase == null) {
                VirtualDirContext parent = getParent();
                if (parent != null) {
                    docBase = parent.getNameInNamespace() + "/" + getLocalName();
                }
            }
            return docBase;
        }

        public Attributes getAttributes(String name, String[] attrIds) throws NamingException {
            return getAttributes(new CompositeName(name), attrIds);
        }

        public Attributes getAttributes(Name name, String[] attrIds)
                throws NamingException {

            VirtualDirEntry entry;
            if (name.isEmpty())
                entry = this;
            else
                entry = treeLookup(name);
            if (entry == null)
                throw new NamingException(sm.getString("resources.notFound", name));

            return entry.getAttributes();
        }

        public void modifyAttributes(String name, int mod_op, Attributes attrs) throws NamingException {
            throw new OperationNotSupportedException();
        }

        public void modifyAttributes(String name, ModificationItem[] mods) throws NamingException {
            throw new OperationNotSupportedException();
        }

        public void bind(String name, Object obj, Attributes attrs) throws NamingException {
            throw new OperationNotSupportedException();
        }

        public void rebind(String name, Object obj, Attributes attrs) throws NamingException {
            throw new OperationNotSupportedException();
        }

        public DirContext createSubcontext(String name, Attributes attrs) throws NamingException {
            throw new OperationNotSupportedException();
        }

        public DirContext getSchema(String name) throws NamingException {
            throw new OperationNotSupportedException();
        }

        public DirContext getSchemaClassDefinition(String name) throws NamingException {
            throw new OperationNotSupportedException();
        }

        public NamingEnumeration search(String name, Attributes matchingAttributes, String[] attributesToReturn) throws NamingException {
            throw new OperationNotSupportedException();
        }

        public NamingEnumeration search(String name, Attributes matchingAttributes) throws NamingException {
            throw new OperationNotSupportedException();
        }

        public NamingEnumeration search(String name, String filter, SearchControls cons) throws NamingException {
            throw new OperationNotSupportedException();
        }

        public NamingEnumeration search(String name, String filterExpr, Object[] filterArgs, SearchControls cons) throws NamingException {
            throw new OperationNotSupportedException();
        }

        protected ArrayList list(VirtualDirEntry entry) throws NamingException {
            ArrayList<NamingEntry> entries = new ArrayList<NamingEntry>();
            if (!(entry instanceof VirtualDirContext))
                return entries;

            VirtualDirContext vdc = (VirtualDirContext)entry;

            VirtualDirEntry[] children;
            if (vdc.entries != null) {
                children = vdc.entries.values().toArray(new VirtualDirEntry[0]);
                Arrays.sort(children);
            } else if (vdc.delegate != null) {
                List<VirtualDirEntry> kids = new ArrayList<VirtualDirEntry>();
                NamingEnumeration<Binding> bindings = vdc.delegate.listBindings("");
                while (bindings.hasMoreElements()) {
                    Binding binding = bindings.nextElement();
                    String name = binding.getName();
                    if (binding.isRelative()) {
                        CompositeName cn = new CompositeName(name);
                        if (cn.size() > 0)
                            name = cn.get(cn.size() - 1);
                    }
                    kids.add(makeEntry(name, binding.getObject()));
                }
                children = kids.toArray(new VirtualDirEntry[0]);
            } else
                throw new IllegalStateException("VirtualDirContext has neither entries nor a delegate");

            NamingEntry namingEntry;

            for (VirtualDirEntry current : children) {
                Object object = current instanceof DirContext ? current : current.getFileResource();
                namingEntry = new NamingEntry(current.getLocalName(), object, NamingEntry.ENTRY);
                entries.add(namingEntry);
            }
            return entries;
        }

        public String getLocalName() {
            return thisEntry.getLocalName();
        }

        public boolean isDirectory() {
            return true;
        }

        public Resource getFileResource() {
            return null;
        }

        public VirtualDirContext getDirectory() {
            return this;
        }

        public VirtualDirContext getParent() {
            return thisEntry.getParent();
        }

        public void setParent(VirtualDirContext parent) {
            thisEntry.setParent(parent);
        }

        public Attributes getAttributes() throws NamingException {
            return thisEntry.getAttributes();
        }

        public int compareTo(Object o) {
            return thisEntry.compareTo(o);
        }
    }


    // An entry in our virtual file system
    interface VirtualDirEntry extends Comparable {
        /**
         * Get the local part of the name of this entry.
         *
         * @return the local part of the name of this entry, ie "blah.xml".  Never null.
         */
        String getLocalName();

        /**
         * Check if this entry represents a directory.
         *
         * @return true if this is a subdirectory entry.
         */
        boolean isDirectory();

        /**
         * Get the Resource represented by this entry, if it is a file entry.
         *
         * @return the Resource, or null if this entry is a directory.
         */
        Resource getFileResource();

        /**
         * Get the VirtualDirContext represented by this entry, if it is a directory entry.
         *
         * @return a VirtualDirContext, or null if this entry isn't a directory.
         */
        VirtualDirContext getDirectory();

        /**
         * Get the parent directory of this entry, if known.
         *
         * @return the parent directory of this entry or null if unknown or this is the root directory.
         */
        VirtualDirContext getParent();

        /**
         * Set the parent directory of this entry.
         *
         * @param parent the new parent directory.  May be null.
         */
        void setParent( VirtualDirContext parent);

        /**
         * Get any additional attributes for this resource.
         *
         * @return the attributes or null to request that default attributes be created.
         * @throws javax.naming.NamingException if there is a problem fetching the attributes
         */
        Attributes getAttributes() throws NamingException;
    }


    static class VirtualDirEntryImpl implements VirtualDirEntry {
        protected final String localName;
        protected Resource resource;
        protected Attributes attrs;
        protected boolean isDirectory = false;
        protected VirtualDirContext directory;
        protected VirtualDirContext parent;

        /**
         * Create a VirtualDirEntry with the specified name.
         *
         * @param localName the local part of the name, not fully qualified, ie "blah.xml".  Use empty string
         *                  if this resource is a root directory.
         */
        public VirtualDirEntryImpl(String localName) {
            this.localName = localName;
        }

        /**
         * Create a VirtualDirEntry with the specified name that represents a subdirectory entry.
         *
         * @param localName the local part of the name, not fully qualified, ie "blah.xml".  Use empty string
         *                  if this resource is a root directory.
         * @param subdir subdirectory represented by this directory entry.  Required.
         */
        public VirtualDirEntryImpl( String localName, VirtualDirContext subdir ) {
            if (localName == null) throw new NullPointerException();
            if (subdir == null) throw new NullPointerException();
            this.localName = localName;
            this.isDirectory = true;
            this.directory = subdir;
            ResourceAttributes rats = new ResourceAttributes();
            this.attrs = rats;
            rats.setName(localName);
            rats.setCollection(true);
        }

        /**
         * Create a VirtualDirEntry with the specified name and file contents.
         *
         * @param localName the local part of the name, not fully qualified, ie "blah.xml".  Use empty string
         *                  if this resource is a root directory.
         * @param resourceBytes the bytes of the file to keep at this name.
         */
        public VirtualDirEntryImpl(String localName, byte[] resourceBytes) {
            this.localName = localName;
            this.resource = new Resource(resourceBytes);
            ResourceAttributes rats = new ResourceAttributes();
            this.attrs = rats;
            rats.setName(localName);
            rats.setContentLength(resourceBytes.length);
        }

        /**
         * Create a VirtualDirEntry for the specified resource with the specified attributes.
         * The local name will be extracted from the attributes.
         *
         * @param resource  resource to create.  required
         * @param attrs     attributes to use.  required
         */
        public VirtualDirEntryImpl(Resource resource, ResourceAttributes attrs) {
            this.resource = resource;
            this.attrs = attrs;
            this.localName = attrs.getName();
        }

        public String getLocalName() {
            return localName;
        }

        public boolean isDirectory() {
            return isDirectory;
        }

        public Resource getFileResource() {
            if (isDirectory())
                return null;
            if (resource == null)
                resource = findResource();
            return resource;
        }

        public VirtualDirContext getDirectory() {
            if (!isDirectory())
                return null;
            if (directory == null)
                directory = findDirectory();
            return directory;
        }

        public VirtualDirContext getParent() {
            return parent;
        }

        public void setParent( VirtualDirContext parent) {
            this.parent = parent;
        }

        /**
         * Subclasses can override this to support lazily creating the VirtualDirContext instance.
         *
         * @return a VirtualDirContext instance.  Never null.
         */
        protected VirtualDirContext findDirectory() {
            throw new IllegalStateException("no VirtualDirContext set for virtual directory resource");
        }

        public void setResource(Resource resource) {
            this.resource = resource;
        }

        public Attributes getAttributes() throws NamingException {
            if (attrs == null) {
                attrs = findAttributes();
                initAttributes(attrs);
            }
            return attrs;
        }

        public void setAttributes(ResourceAttributes attrs) {
            this.attrs = attrs;
        }

        /**
         * Subclasses can override this to support lazily creating the Resource instance.
         * <p/>
         * If not overridden, this method always throws IllegalStateException.
         *
         * @return a Resource instance.  Never null.
         */
        protected Resource findResource() {
            throw new IllegalStateException("no Resource set for virtual file resource");
        }

        /**
         * Subclasses can override this to support lazily creating the ResourceAttributes instance.
         * <p/>
         * If not overridden, this method always returns a new ResourceAttributes instance.
         *
         * @return a ResourceAttributes instance.  Never null.
         * @throws javax.naming.NamingException if the attributes can't be found
         */
        protected Attributes findAttributes() throws NamingException {
            return new ResourceAttributes();
        }

        /**
         * Subclasses can override this to support lazily populating a newly-created ResourceAttributes instance.
         * <p/>
         * If not overridden, this method always sets the NAME attribute and sets Collection to true
         * if this is a directory entry.
         *
         * @param attrs the newly-created ResourceAttributes instance.  Required.
         */
        protected void initAttributes(Attributes attrs) {
            if (attrs instanceof ResourceAttributes) {
                ResourceAttributes rats = (ResourceAttributes)attrs;
                rats.setName(localName);
                rats.setCollection(isDirectory());
            }
        }

        public int compareTo(Object o) {
            return getLocalName().compareTo(((VirtualDirEntry)o).getLocalName());
        }
    }

}
