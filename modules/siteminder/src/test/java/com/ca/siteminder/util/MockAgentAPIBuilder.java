package com.ca.siteminder.util;

import com.ca.siteminder.SiteMinderApiClassException;
import netegrity.siteminder.javaagent.*;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import java.util.Enumeration;

import static org.mockito.Matchers.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * To build mock version of SiteMinder AgentAPI instance.
 */
public class MockAgentAPIBuilder {

    private final AgentAPI agentAPI = mock(AgentAPI.class);

    public MockAgentAPIBuilder withDefaultResources() {
        when(agentAPI.isProtectedEx(anyString(), any(), any(), any())).thenAnswer(new Answer<Integer>() {
            @Override
            public Integer answer(InvocationOnMock invocationOnMock) throws Throwable {
                final ResourceContextDef resDef = invocationOnMock.getArgumentAt(1, ResourceContextDef.class);
                final RealmDef realmDef = invocationOnMock.getArgumentAt(2, RealmDef.class);

                if (resDef.resource.equals(MockAgentAPITestConstants.RESDEF_PRIVATE_RESOURCE)) {
                    realmDef.oid = "roid123";
                    realmDef.name = "realm123";
                    return AgentAPI.YES;
                } else {
                    return AgentAPI.NO;
                }
            }
        });

        return this;
    }

    public MockAgentAPIBuilder basicAuth() {
        when(agentAPI.loginEx(anyString(), any(), any(), any(), any(), any(), anyString())).thenAnswer(new Answer<Integer>() {
            @Override
            public Integer answer(InvocationOnMock invocationOnMock) throws Throwable {
                final UserCredentials credentials = invocationOnMock.getArgumentAt(3, UserCredentials.class);
                final boolean matches = MockAgentAPITestConstants.AUTHN_USER_NAME.equals(credentials.name) &&
                        MockAgentAPITestConstants.AUTHN_PASSWORD.equals(credentials.password);

                if (matches) {
                    final SessionDef sessionDef = invocationOnMock.getArgumentAt(4, SessionDef.class);

                    sessionDef.id = invocationOnMock.getArgumentAt(6, String.class);
                    sessionDef.idleTimeout = MockAgentAPITestConstants.IDLE_TIMEOUT;
                    sessionDef.maxTimeout = MockAgentAPITestConstants.MAX_TIMEOUT;

                    return AgentAPI.YES;
                } else {
                    return AgentAPI.NO;
                }
            }
        });

        when(agentAPI.createSSOToken(any(), any(), any())).thenAnswer(new Answer<Integer>() {
            @Override
            public Integer answer(InvocationOnMock invocationOnMock) throws Throwable {
                final SessionDef sessionDef = invocationOnMock.getArgumentAt(0, SessionDef.class);
                final StringBuffer tokenBuffer = invocationOnMock.getArgumentAt(2, StringBuffer.class);

                tokenBuffer.setLength(0);
                tokenBuffer.append(MockAgentAPITestConstants.SSO_TOKEN);
                tokenBuffer.append(";" + sessionDef.id);
                tokenBuffer.append(";" + sessionDef.idleTimeout);
                tokenBuffer.append(";" + sessionDef.maxTimeout);

                return AgentAPI.SUCCESS;
            }
        });

        return this;
    }

    public MockAgentAPIBuilder tokenAuth() {
        when(agentAPI.login(anyString(), any(), any(), any(), any(), any())).thenAnswer(new Answer<Integer>() {
            @Override
            public Integer answer(InvocationOnMock invocationOnMock) throws Throwable {
                final UserCredentials credentials = invocationOnMock.getArgumentAt(3, UserCredentials.class);
                final boolean matches = MockAgentAPITestConstants.AUTHN_USER_NAME.equals(credentials.name) &&
                        MockAgentAPITestConstants.AUTHN_PASSWORD.equals(credentials.password);

                if (matches) {
                    final SessionDef sessionDef = invocationOnMock.getArgumentAt(4, SessionDef.class);
                    final AttributeList attributeList = invocationOnMock.getArgumentAt(5, AttributeList.class);

                    sessionDef.id = new String(findAttribute(attributeList, AgentAPI.ATTR_SESSIONID).value);
                    sessionDef.idleTimeout = Integer.parseInt(new String(findAttribute(attributeList, AgentAPI.ATTR_IDLESESSIONTIMEOUT).value));
                    sessionDef.maxTimeout = Integer.parseInt(new String(findAttribute(attributeList, AgentAPI.ATTR_MAXSESSIONTIMEOUT).value));

                    return AgentAPI.YES;
                } else {
                    return AgentAPI.NO;
                }
            }
        });

        when(agentAPI.decodeSSOToken(anyString(), any(), any(), anyBoolean(), any())).thenAnswer(new Answer<Integer>() {
            @Override
            public Integer answer(InvocationOnMock invocationOnMock) throws Throwable {
                final String token = invocationOnMock.getArgumentAt(0, String.class);
                if (token.startsWith(MockAgentAPITestConstants.SSO_TOKEN)) {
                    final AttributeList attributeList = invocationOnMock.getArgumentAt(2, AttributeList.class);
                    final String[] subTokens = token.split(";");

                    attributeList.addAttribute(new Attribute(AgentAPI.ATTR_SESSIONID, 0, 0, "", subTokens[1].getBytes()));
                    attributeList.addAttribute(new Attribute(AgentAPI.ATTR_IDLESESSIONTIMEOUT, 0, 0, "", subTokens[2].getBytes()));
                    attributeList.addAttribute(new Attribute(AgentAPI.ATTR_MAXSESSIONTIMEOUT, 0, 0, "", subTokens[3].getBytes()));
                    attributeList.addAttribute(new Attribute(AgentAPI.ATTR_STARTSESSIONTIME, 0, 0, "",
                            Integer.toString(SiteMinderUtil.safeLongToInt(System.currentTimeMillis() / 1000) - MockAgentAPITestConstants.IDLE_TIMEOUT / 2).getBytes()));
                    attributeList.addAttribute(new Attribute(AgentAPI.ATTR_LASTSESSIONTIME, 0, 0, "",
                            Integer.toString(SiteMinderUtil.safeLongToInt(System.currentTimeMillis() / 1000)).getBytes()));
                    return AgentAPI.SUCCESS;
                } else {
                    return AgentAPI.FAILURE;
                }
            }
        });
        return this;
    }

    public MockAgentAPIBuilder withAco(final String[] params) {
        when(agentAPI.getAgentConfig(anyString(), any())).thenAnswer(new Answer<Integer>() {
            @Override
            public Integer answer(InvocationOnMock invocationOnMock) throws Throwable {
                final AttributeList attributeList = invocationOnMock.getArgumentAt(1, AttributeList.class);

                for (String param : params) {
                    attributeList.addAttribute(new Attribute(0, 0, 0, "", param.getBytes()));
                }

                return AgentAPI.YES;
            }
        });

        return this;
    }

    public AgentAPI build() throws SiteMinderApiClassException {
        return agentAPI;
    }

    public static MockAgentAPIBuilder getDefaultBuilder() {
        return new MockAgentAPIBuilder().withDefaultResources().basicAuth();
    }

    private Attribute findAttribute(final AttributeList attrList, final int attrId) {
        final Enumeration<Attribute> it = attrList.attributes();

        while (it.hasMoreElements()) {
            final Attribute attr = it.nextElement();
            if (attr.id == attrId) {
                return attr;
            }
        }

        return null;
    }
}
