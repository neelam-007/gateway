package com.ca.siteminder.util;

public final class MockAgentAPITestConstants {

    public static final String SSO_TOKEN = "token";
    public static final String RESDEF_AGENT = "agent";
    public static final String RESDEF_SERVER = "server";
    public static final String RESDEF_PRIVATE_RESOURCE = "/private_resource";
    public static final String RESDEF_PUBLIC_RESOURCE = "/public_resource";
    public static final String RESDEF_RESOURCE = RESDEF_PRIVATE_RESOURCE;
    public static final String RESDEF_ACTION = "POST";

    public static final String USER_IP = "127.0.0.1";
    public static final String AUTHN_USER_NAME = "user";
    public static final String AUTHN_PASSWORD = "password";

    public static final int IDLE_TIMEOUT = 30 * 1000;
    public static final int MAX_TIMEOUT = 30 * 60 * 1000;
    public static final String ACO_NAME = "agent_config_object";

    private MockAgentAPITestConstants() {
        // Disallow instance creation
    }

}
