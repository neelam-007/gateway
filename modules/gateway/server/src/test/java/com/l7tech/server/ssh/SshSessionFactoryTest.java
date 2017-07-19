package com.l7tech.server.ssh;

import com.jcraft.jsch.HostKeyRepository;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.Session;
import com.l7tech.util.Either;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.runners.MockitoJUnitRunner;

import java.util.Base64;
import java.util.List;

import static com.l7tech.util.CollectionUtils.list;
import static com.l7tech.util.Functions.grep;
import static com.l7tech.util.Functions.map;
import static com.l7tech.util.TextUtils.isNotEmpty;
import static com.l7tech.util.TextUtils.trim;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

/**
 * SshSessionFactoryTest provides unit tests for SshSessionFactory
 */
@RunWith(MockitoJUnitRunner.class)
public class SshSessionFactoryTest {
    @Spy
    private JSch jsch;

    @Mock
    private Session session;

    private SshSessionFactory sshSessionFactory = new SshSessionFactory();

    private static final String HOST = "111.22.33.44";
    private static final int DEFAULT_PORT = 22;
    private static final int NON_DEFAULT_PORT = 34567;
    private static final int SOCKET_TIMEOUT = 31 * 1000;
    private static final String USER_NAME = "layer7";
    private static final String PASSWORD = "7layer";
    private static final byte[] PUBLIC_KEY = Base64.getDecoder().decode("AAAAB3NzaC1yc2EAAAADAQABAAABAQDEUtBLnPKNSxIkgY1Q+9tokFMSoWQT6fsuw/T5W0Ug2bwgrLHpkP5chAwgd+Fmwsy59W180MuwYyps43uW2toVxCSjQxV811Op72qzxHGlEsUAR9NN6734R7nPx/vd0xVGHOObTmXuSynUOPgCRNuVlo5ysKP/WBdYkgZIpQOlfDF+gj93lF9BPodDLIX+ssVqQP2CJ5PvHP4mFXYIxuqYn4UO2StWc+nsnmyNgjtVjdL6tAOihQuQxVHt77BR6TIlifggRQDfcs0UdrzVnRDiEEOrawizmBnHbk0CaX26NT+lU/NXqrTKwnqsCJKx9GfxZgSzcWlUO70bl8GKfNkr");
    private static final String FINGERPRINT = "fe:9d:a7:33:4b:ef:7e:43:df:73:69:b6:1e:89:d7:54";
    private static final int CONNECTION_TIMEOUT = 30 * 1000;

    private static final String DEFAULT_CIPHER_ORDER = "aes128-ctr, aes128-cbc, 3des-cbc, blowfish-cbc, aes192-ctr, aes192-cbc, aes256-ctr, aes256-cbc";
    private static final String DEFAULT_MAC_ORDER = "hmac-md5,hmac-sha1,hmac-sha2-256,hmac-sha1-96,hmac-md5-96";
    private static final String DEFAULT_COMPRESSION_ORDER = "none";

    private static final List<String> CIPHERS = grep(map(list(DEFAULT_CIPHER_ORDER.split("\\s*,\\s*")), trim()), isNotEmpty());
    private static final List<String> MACS = grep(map(list(DEFAULT_MAC_ORDER.split("\\s*,\\s*")), trim()), isNotEmpty());
    private static final List<String> COMPRESSIONS = grep(map(list(DEFAULT_COMPRESSION_ORDER.split("\\s*,\\s*")), trim()), isNotEmpty());

    @Before
    public void setup() throws Exception {
        when(jsch.getSession(anyString(), anyString(), anyInt())).thenReturn(session);
    }

    /**
     * Test that the host key is added to the HostKeyRepository when the fingerPrint exists
     * expect 1. host key repo is added with a host, key
     *        2. "StrictHostKeyChecking" set to "yes"
     */
    @Test
    public void testHostKeyAdded() throws Exception {
        // Using standard ssh port 22
        SshSessionKey key = new SshSessionKey(USER_NAME, HOST, DEFAULT_PORT, Either.left(PASSWORD),
                CONNECTION_TIMEOUT, SOCKET_TIMEOUT, FINGERPRINT, CIPHERS, MACS, COMPRESSIONS);
        sshSessionFactory.makeObject(key, jsch);
        HostKeyRepository hostKeyRepository = jsch.getHostKeyRepository();

        assertEquals(HostKeyRepository.OK, hostKeyRepository.check(HOST, PUBLIC_KEY));
        verify(session, times(1)).setConfig("StrictHostKeyChecking", "yes");
    }

    /**
     * Test when fingerprint is specified and using non-default ssh port
     * expect 1. host key repo added with a [host]:port, key
     *        2. "StrictHostKeyChecking" set to "yes"
     */
    @Test
    public void testHostKeyAddedForNonDefaultSSHPort() throws Exception {
        SshSessionKey key = new SshSessionKey(USER_NAME, HOST, NON_DEFAULT_PORT, Either.left(PASSWORD),
                CONNECTION_TIMEOUT, SOCKET_TIMEOUT, FINGERPRINT, CIPHERS, MACS, COMPRESSIONS);
        sshSessionFactory.makeObject(key, jsch);
        HostKeyRepository hostKeyRepository = jsch.getHostKeyRepository();

        assertEquals(HostKeyRepository.OK, hostKeyRepository.check("[" + HOST + "]:" + NON_DEFAULT_PORT, PUBLIC_KEY));
        verify(session, times(1)).setConfig("StrictHostKeyChecking", "yes");
    }

    /**
     * Test when the fingerprint is not specified
     * expect: the session should be set with config "StrictHostKeyChecking" to "no"
     */
    @Test
    public void testNoHostKeyAdded() throws Exception {
        SshSessionKey key = new SshSessionKey(USER_NAME, HOST, DEFAULT_PORT, Either.left(PASSWORD),
                CONNECTION_TIMEOUT, SOCKET_TIMEOUT, null, CIPHERS, MACS, COMPRESSIONS);
        sshSessionFactory.makeObject(key, jsch);
        verify(session, times(1)).setConfig("StrictHostKeyChecking", "no");
    }
}
