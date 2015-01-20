package com.l7tech.external.assertions.gatewaymanagement.server.rest.resource.impl;

import com.l7tech.common.mime.ContentTypeHeader;
import com.l7tech.common.mime.NoSuchPartException;
import com.l7tech.external.assertions.gatewaymanagement.server.ServerRESTGatewayManagementAssertion;
import com.l7tech.external.assertions.gatewaymanagement.server.rest.RbacAccessService;
import com.l7tech.external.assertions.gatewaymanagement.server.rest.resource.RestManVersion;
import com.l7tech.external.assertions.gatewaymanagement.server.rest.resource.Since;
import com.l7tech.gateway.common.LicenseException;
import com.l7tech.gateway.common.admin.PolicyBundleInstallerAdmin;
import com.l7tech.gateway.rest.SpringBean;
import com.l7tech.identity.User;
import com.l7tech.identity.UserBean;
import com.l7tech.message.AbstractHttpResponseKnob;
import com.l7tech.message.HttpRequestKnob;
import com.l7tech.message.HttpRequestKnobAdapter;
import com.l7tech.message.Message;
import com.l7tech.policy.assertion.Assertion;
import com.l7tech.policy.assertion.PolicyAssertionException;
import com.l7tech.policy.wsp.WspReader;
import com.l7tech.security.token.http.HttpBasicToken;
import com.l7tech.server.identity.AuthenticationResult;
import com.l7tech.server.message.PolicyEnforcementContext;
import com.l7tech.server.message.PolicyEnforcementContextFactory;
import com.l7tech.server.policy.ServerPolicyFactory;
import com.l7tech.server.policy.assertion.ServerAssertion;
import com.l7tech.server.util.JaasUtils;
import com.l7tech.util.ExceptionUtils;
import com.l7tech.util.IOUtils;
import org.apache.commons.lang.StringUtils;

import javax.inject.Singleton;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.*;
import javax.ws.rs.ext.Provider;
import java.io.IOException;
import java.io.OutputStream;
import java.text.MessageFormat;
import java.util.logging.Logger;

/* NOTE: The java docs in this class get converted to API documentation seen by customers! */

/**
 * This resource exposes the Policy Bundle Installer for no-GUI use cases (e.g. support auto provisioning scripting to install OAuth).
 */
@Provider
@Path(ServerRESTGatewayManagementAssertion.Version1_0_URI + "policyBundleInstallers")
@Singleton
@Since(RestManVersion.VERSION_1_0_1)
public class PolicyBundleInstallerResource {
    private static final Logger logger = Logger.getLogger(PolicyBundleInstallerResource.class.getName());
    private static final String POLICY_BUNDLE_INSTALLER_POLICY_XML_TEMPLATE = "<wsp:Policy xmlns:L7p=\"http://www.layer7tech.com/ws/policy\" xmlns:wsp=\"http://schemas.xmlsoap.org/ws/2002/12/policy\">\n" +
            "    <wsp:All wsp:Usage=\"Required\">\n" +
            "        <L7p:{0}/>\n" + // installer assertion name
            "    </wsp:All>\n" +
            "</wsp:Policy>\n";

    @SpringBean
    private WspReader wspReader;

    @SpringBean
    private ServerPolicyFactory serverPolicyFactory;

    @SpringBean
    private RbacAccessService rbacAccessService;

    @Context
    protected UriInfo uriInfo;

    public PolicyBundleInstallerResource() {}

    /**
     * <p>Executes a Policy Bundle Installer action.  Choose the installer instance by setting HTTP parameter <code>installer_name</code> (e.g. set to <code>OAuthInstaller</code>).</p>
     *
     * <p>The following actions are supported: list components, restman get migration bundle XML, wsman dry run install, wsman install and custom.
     * For restman bundle, get migration bundle.  For wsman bundle, execute dry run and install.</p>
     *
     * Provides actions through the following HTTP parameters:
     * <h5>
     * 	List components
     * </h5>
     * <p>
     * 	List component id(s) available for this installer bundle.  This is the default if no action parameter is provided.
     * </p>
     * <p>
     * 	Input HTTP parameter: set <code>action</code> to <code>list</code>
     * </p>
     * <p>
     * 	Output: List of component id(s), ";" separated. For example:
     * </p>
     * <pre>1c2a2874-df8d-4e1d-b8b0-099b576407e1;ba525763-6e55-4748-9376-76055247c8b1;f69c7d15-4999-4761-ab26-d29d58c0dd57;b082274b-f00e-4fbf-bbb7-395a95ca2a35;a07924c0-0265-42ea-90f1-2428e31ae5ae;
     * </pre>
     * <h5>
     * 	Restman get migration bundle
     * </h5>
     * <p>
     * 	Get the restman migration bundle XML for the given component(s).
     * </p>
     * <p>
     * 	Input HTTP parameters:
     * </p>
     * <ul>
     * 	<li>
     * 		Set <code>action</code> to <code>restman_get</code>
     * 	</li>
     * 	<li>
     * 		Set <code>component_ids</code> to a ";" separated list of component ids. Or set to <code>all</code> to specify all available installer components.
     * 		This is equivalent to all component id(s) from the <code>list</code> action.
     * 	</li>
     * 	<li>
     * 		Optionally set <code>version_modifier</code> - optional version modifier
     * 	</li>
     * 	<li>
     * 		Optionally set <code>folder_goid</code> - optional install folder (if not set, defaults to root folder)
     * 	</li>
     * </ul>
     * <p>
     * 	Output: The restman migration bundle XML for each component. For example:
     * </p>
     * <pre>&lt;l7:Bundle xmlns:l7="<a rel="nofollow" href="http://ns.l7tech.com/2010/04/gateway-management">http://ns.l7tech.com/2010/04/gateway-management</a>"&gt;
     *     &lt;l7:References&gt;
     *         &lt;l7:Item&gt;
     *          ...
     *         &lt;/l7:Mapping&gt;
     *     &lt;/l7:Mappings&gt;
     * &lt;/l7:Bundle&gt;
     * </pre>
     * <h5>
     * 	Wsman dry run install
     * </h5>
     * <p>
     * 	Execute wsman dry run install for given component(s).
     * </p>
     * <p>
     * 	Input HTTP parameters:
     * </p>
     * <ul>
     * 	<li>
     * 		Set <code>action</code> to <code>wsman_dry_run</code>
     * 	</li>
     * 	<li>
     * 		Set <code>component_ids</code> to a ";" separated list of component ids. Or set to <code>all</code> to specify all available installer components.
     * 		This is equivalent to all component id(s) from the <code>list</code> action.
     * 	</li>
     * 	<li>
     * 		Optionally set <code>version_modifier</code> - optional version modifier
     * 	</li>
     * 	<li>
     * 		Optionally map JDBC connection name to new name
     * 		<ul>
     * 			<li>
     * 				Set <code>jdbc_connection.&lt;component_id&gt;.name</code> with existing name in bundle (e.g. OAuth)
     * 			</li>
     * 			<li>
     * 				Set <code>jdbc_connection.&lt;component_id&gt;.new_name</code> with a new desired name (e.g. OAuth Dev)
     * 			</li>
     * 		</ul>
     * 	</li>
     * </ul>
     * <p>
     * 	Output: The conflicts for each component. For example:
     * </p>
     * <pre>ComponentId: 1c2a2874-df8d-4e1d-b8b0-099b576407e1
     * ServiceConflict: /auth/oauth/v1/token;/auth/oauth/v1/authorize/website;/oauth/v1/client;/protected/resource;/auth/oauth/v1/authorize;/auth/oauth/v1/request;/auth/oauth/v1/*
     * PolicyConflict: OAuth 1.0 Context Variables;Require OAuth 1.0 Token;getClientSignature;Authenticate OAuth 1.0 Parameter;Token Lifetime Context Variables;GenerateOAuthToken;OAuth Client Token Store Context Variables
     * CertificateConflict:
     * JdbcConnectionsThatDontExist:
     * MissingAssertions:
     * EncapsulatedAssertionConflict:
     * ...
     * </pre>
     * <p>
     * 	It's recommended to <strong>not continue</strong> with installation if any conflicts are detected. Continuing the installation with detected conflicts may
     * 	result in partial installation of components. Components with detected conflicts will <strong>not</strong> be installed. As a result, references to
     * 	conflicted components may incorrectly reference the previous version of the component already on the Gateway (if one exists).
     * </p>
     * <h5>
     * 	Wsman install
     * </h5>
     * <p>
     * 	Execute wsman install for given component(s).
     * </p>
     * <p>
     * 	Input HTTP parameters:
     * </p>
     * <ul>
     * 	<li>
     * 		Set <code>action</code> to <code>wsman_install</code>
     * 	</li>
     * 	<li>
     * 		Set <code>component_ids</code> to a ";" separated list of component ids. Or set to <code>all</code> to specify all available installer components.
     * 		This is equivalent to all component id(s) from the <code>list</code> action.
     * 	</li>
     * 	<li>
     * 		Optionally set <code>version_modifier</code> - optional version modifier
     * 	</li>
     * 	<li>
     * 		Optionally set <code>folder_goid</code> - optional install folder (if not set, defaults to root folder)
     * 	</li>
     * 	<li>
     * 		Optionally map JDBC connection name to new name
     * 		<ul>
     * 			<li>
     * 				Set <code>jdbc_connection.&lt;component_id&gt;.name</code> with existing name in bundle (e.g. OAuth)
     * 			</li>
     * 			<li>
     * 				Set <code>jdbc_connection.&lt;component_id&gt;.new_name</code> with a new desired name (e.g. OAuth Dev)
     * 			</li>
     * 		</ul>
     * 	</li>
     * </ul>
     * <p>
     * 	Output: List of installed component id(s), ";" separated. For example:
     * </p>
     * <pre>1c2a2874-df8d-4e1d-b8b0-099b576407e1;ba525763-6e55-4748-9376-76055247c8b1;f69c7d15-4999-4761-ab26-d29d58c0dd57;b082274b-f00e-4fbf-bbb7-395a95ca2a35;a07924c0-0265-42ea-90f1-2428e31ae5ae
     * </pre>
     * <h5>
     * 	Custom
     * </h5>
     * <p>
     * 	An installer can implement custom action, custom wsman dry run and install logic. For example the OAuth installer implements its own custom action to get a
     * 	database schema. It also executes custom logic to integrate API Portal for wsman dry run and install.
     * </p>
     * <p>
     * 	Input: Set HTTP parameter <code>action</code> to <code>custom</code> in order to choose a custom action. <strong>And</strong> set HTTP parameter(s)
     * 	required for the custom installer.
     * </p>
     *
     * @param installerName name of installer instance to use (e.g. OAuthInstaller)
     * @return The stream output from the installer.
     * @throws com.l7tech.gateway.common.admin.PolicyBundleInstallerAdmin.PolicyBundleInstallerException
     */
    @GET
    @Produces(MediaType.TEXT_XML)
    public Response execute(@QueryParam("installer_name") final String installerName) throws PolicyBundleInstallerAdmin.PolicyBundleInstallerException {
        if (StringUtils.isEmpty(installerName)) {
            return Response.status(Response.Status.BAD_REQUEST).entity("Mandatory parameter missing: installer_name.  Installer name can't be empty.").build();
        }

        rbacAccessService.validateFullAdministrator();

        final ServerAssertion installerServerAssertion;
        try {
            Assertion installerAssertion = wspReader.parseStrictly(MessageFormat.format(POLICY_BUNDLE_INSTALLER_POLICY_XML_TEMPLATE, installerName), WspReader.Visibility.omitDisabled);
            installerServerAssertion = serverPolicyFactory.compilePolicy(installerAssertion, false);
        } catch (IOException e) {
            final String msg = "Installer name not found: " + installerName + ". ";
            logger.fine(msg + ExceptionUtils.getMessage(e));
            return Response.status(Response.Status.NOT_FOUND).entity(msg).build();
        } catch (LicenseException | PolicyAssertionException e) {
            throw new PolicyBundleInstallerAdmin.PolicyBundleInstallerException("Unable to create installer",  ExceptionUtils.getDebugException(e));
        }

        final PolicyEnforcementContext context;
        try {
            context = getContext();
            installerServerAssertion.checkRequest(context);
        } catch (IOException | PolicyAssertionException e) {
            throw new PolicyBundleInstallerAdmin.PolicyBundleInstallerException("Installer error", ExceptionUtils.getDebugException(e));
        }

        return Response.ok(new StreamingOutput() {
            public void write(OutputStream output) throws IOException {
                try {
                    IOUtils.copyStream(context.getResponse().getMimeKnob().getEntireMessageBodyAsInputStream(), output);
                } catch (NoSuchPartException e) {
                    throw new IOException(e);
                }
            }
        }).build();
    }

    /**
     * Get an initialized context for the installer.
     */
    private PolicyEnforcementContext getContext() throws IOException {
        final Message request = new Message();
        request.initialize(ContentTypeHeader.XML_DEFAULT, new byte[0]);
        final HttpRequestKnob requestKnob = new HttpRequestKnobAdapter();
        request.attachHttpRequestKnob(requestKnob);

        final Message response = new Message();
        response.attachHttpResponseKnob(new AbstractHttpResponseKnob() {});

        final PolicyEnforcementContext context = PolicyEnforcementContextFactory.createPolicyEnforcementContext(request, response);

        // copy user to authentication context
        final User currentUser = JaasUtils.getCurrentUser();
        if (currentUser != null) {
            // convert logged on user into a UserBean as if the user was authenticated via policy.
            final UserBean userBean = new UserBean(currentUser.getProviderId(), currentUser.getLogin());
            userBean.setUniqueIdentifier(currentUser.getId());
            context.getDefaultAuthenticationContext().addAuthenticationResult(new AuthenticationResult(
                    userBean,
                    new HttpBasicToken(currentUser.getLogin(), "".toCharArray()), null, false)
            );
        } else {
            // no action will be allowed, this will result in permission denied later
            logger.warning("No administrative user found. Request to installer will fail.");
        }

        // copy parameters to context variables
        final MultivaluedMap<String, String> queryParameters = uriInfo.getQueryParameters();
        for (final String name : queryParameters.keySet()) {
            context.setVariable(name, queryParameters.getFirst(name));
        }

        return context;
    }
}
