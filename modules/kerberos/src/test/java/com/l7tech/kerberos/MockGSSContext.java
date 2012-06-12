package com.l7tech.kerberos;

import org.apache.commons.codec.binary.Base64;
import org.ietf.jgss.*;

import javax.security.auth.Subject;
import java.io.*;
import java.security.AccessControlContext;
import java.security.AccessController;

/**
 * Mock object for the GSSContext
 * User: awitrisna
 */
public class MockGSSContext implements GSSContext {

    private static final String SEC_CONTEXT = "YIIEngYJKoZIhvcSAQICAQBuggSNMIIEiaADAgEFoQMCAQ6iBwMFAAAAAACjggOmYYIDojCCA56gAwIBBaEMGwpMN1RFQ0guU1VQoiIwIKADAgEAoRkwFxsEaHR0cBsPc3NnMy5sN3RlY2guc3Vwo4IDYzCCA1+gAwIBF6EDAgEEooIDUQSCA00YrMYC9YxuVvTl9w+j7qP8pp5DBo9Ahr7V3NIL5t+2VA7CILtj9TjpWiphB5KGOyddVrNDmctOwyncKYUBBVfGE2sgU4cb4N5gSM/NQMRd7D8eM8eHZOehbc5YBlhGwzJwAd77cWwSyQQka5WQXjfeemnHAbmgWfyY3qkRJq27ZK+Yl5+D88SfnGtNyZr8Rjn04VJ3L6wgC6J79dJvwj+VGgHpZUCIqHCE9tIAX5f0DpfeZwOzvjaADbS3jK22R2Df2ePRaTgE4qxy/yFs3kaLnXl7XJky/M5o3Glt2ZHejXpmiJnGP2i4dsPC0KP0HlcLgsRgXUxYU9YYaIH7ZSJxK9f0jYPUFNPg0h9aNjpnyN/gaN8hB+nQaJUUgluEipQJU0kKaDfp6nli2pD4UyWBzHglCAVV0DNGVa10QAtjrBdTYwIGjshzvi1KcLk30zrKn7He7Ho6DxoW0+GaDPPTIkBkX1Rd2Qq9DgsmsRutZEFyU6zOjJPFswQcpkjAU6oXn4U4n2W/YPgoIJMIdrDLJNftsuFj5uESdFPGJCZvFPg2IUFKEU5YyYXr0AzJFqk16pKHSeGgdbqsERR6vg1T80QmDg4p7Zw4sJ28sLVKqizeiHwztEoydEkN9BuuUoemDVrm1GUyWIyaaMJxJE6ApxJj1f6zxGp9704mOkikcUiEmf3sPfDUsNkX+1sHDPUcBk0BPMjsBLcgfeQUzk3hDqBG0/A/L655zUGQJXqWa35G8zNwDEXs/3BDfP3Uy7xx3Kx2bTTiUXDWDihGoqV/2DJcWbfGnlk6L+hPN9D2R2W4MENu/7GCJTPyrW45iXZ8kcfeHF2CZTqUZPP7Azv0xS96un1AFaobMto5Uf6c7xgq854qrEjC0sWyncmRBbu/wNEJejccj3dAoL5NZxtanbjwnbM3AzEGDvblrji+Vg+atnjbkIGb8NFhLeazzlxL5mNbuXBO90U+3bRowMikPdAnzY9rYNdLuIhKecxTdI1OA18qTv8gycB4TWFExHgyn5XFm+WshCA9I/2swwrNoDKfeX8kC6KlUYAX3qhTLuoiM/H/kgWjK27vHjzpRdGcLKO5aEMUjngZrKk76yd6n1CRjMtB3Qohj7QM7aSByTCBxqADAgEXooG+BIG7d4StNmG6un2yyxfJrAtuFrvsr6/eyfOP3mg9/ylYHPubF1F0SRzUXaTLkoc8WkLwiyRyTeEoS7jslLuc3NL51rCEnjeGGsfTWl+XReQrZBEoRVzhto3KjVYsjWPrTNp9NDE8YpTPB7ONU7nJn2DpFoyCuF/3oMZmo6gltmprY60v2w6ENQk7sJkt+m4MYx30A65Xlhf2aeejFYQqsiAq9dM470hF+IGgcKQtr9X9e5FkQnO54GKe18+h1w==";

    private static final String TICKET = "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";

    private GSSContext context;

    public MockGSSContext(GSSContext context) {
        this.context = context;
    }

    @Override
    public byte[] initSecContext(byte[] inputBuf, int offset, int len) throws GSSException {
        AccessControlContext accessControlContext = AccessController.getContext();
        Subject subject = Subject.getSubject(accessControlContext);
        try {
            subject.getPrivateCredentials().add(KerberosClientTest.decode(TICKET));
        } catch (Exception e) {
            throw new GSSException(GSSException.FAILURE);
        }
        return Base64.decodeBase64(SEC_CONTEXT);
    }

    @Override
    public int initSecContext(InputStream inStream, OutputStream outStream) throws GSSException {
        return context.initSecContext(inStream, outStream);
    }

    @Override
    public byte[] acceptSecContext(byte[] inToken, int offset, int len) throws GSSException {
        return context.acceptSecContext(inToken, offset, len);
    }

    @Override
    public void acceptSecContext(InputStream inStream, OutputStream outStream) throws GSSException {
        context.acceptSecContext(inStream, outStream);
    }

    @Override
    public boolean isEstablished() {
        return context.isEstablished();
    }

    @Override
    public void dispose() throws GSSException {
        context.dispose();
    }

    @Override
    public int getWrapSizeLimit(int qop, boolean confReq, int maxTokenSize) throws GSSException {
        return context.getWrapSizeLimit(qop, confReq, maxTokenSize);
    }

    @Override
    public byte[] wrap(byte[] inBuf, int offset, int len, MessageProp msgProp) throws GSSException {
        return context.wrap(inBuf, offset, len, msgProp);
    }

    @Override
    public void wrap(InputStream inStream, OutputStream outStream, MessageProp msgProp) throws GSSException {
        context.wrap(inStream, outStream, msgProp);
    }

    @Override
    public byte[] unwrap(byte[] inBuf, int offset, int len, MessageProp msgProp) throws GSSException {
        return context.unwrap(inBuf, offset, len, msgProp);
    }

    @Override
    public void unwrap(InputStream inStream, OutputStream outStream, MessageProp msgProp) throws GSSException {
        context.unwrap(inStream, outStream, msgProp);
    }

    @Override
    public byte[] getMIC(byte[] inMsg, int offset, int len, MessageProp msgProp) throws GSSException {
        return getMIC(inMsg, offset, len, msgProp);
    }

    @Override
    public void getMIC(InputStream inStream, OutputStream outStream, MessageProp msgProp) throws GSSException {
        context.getMIC(inStream, outStream, msgProp);
    }

    @Override
    public void verifyMIC(byte[] inToken, int tokOffset, int tokLen, byte[] inMsg, int msgOffset, int msgLen, MessageProp msgProp) throws GSSException {
        context.verifyMIC(inToken, tokOffset, tokLen, inMsg, msgOffset, msgLen, msgProp);
    }

    @Override
    public void verifyMIC(InputStream tokStream, InputStream msgStream, MessageProp msgProp) throws GSSException {
        context.verifyMIC(tokStream, msgStream, msgProp);
    }

    @Override
    public byte[] export() throws GSSException {
        return context.export();
    }

    @Override
    public void requestMutualAuth(boolean state) throws GSSException {
        context.requestMutualAuth(state);
    }

    @Override
    public void requestReplayDet(boolean state) throws GSSException {
        context.requestReplayDet(state);
    }

    @Override
    public void requestSequenceDet(boolean state) throws GSSException {
        context.requestSequenceDet(state);
    }

    @Override
    public void requestCredDeleg(boolean state) throws GSSException {
        context.requestCredDeleg(state);
    }

    @Override
    public void requestAnonymity(boolean state) throws GSSException {
        context.requestAnonymity(state);
    }

    @Override
    public void requestConf(boolean state) throws GSSException {
        context.requestConf(state);
    }

    @Override
    public void requestInteg(boolean state) throws GSSException {
        context.requestInteg(state);
    }

    @Override
    public void requestLifetime(int lifetime) throws GSSException {
        context.requestLifetime(lifetime);
    }

    @Override
    public void setChannelBinding(ChannelBinding cb) throws GSSException {
        context.setChannelBinding(cb);
    }

    @Override
    public boolean getCredDelegState() {
        return context.getCredDelegState();
    }

    @Override
    public boolean getMutualAuthState() {
        return context.getMutualAuthState();
    }

    @Override
    public boolean getReplayDetState() {
        return context.getReplayDetState();
    }

    @Override
    public boolean getSequenceDetState() {
        return context.getSequenceDetState();
    }

    @Override
    public boolean getAnonymityState() {
        return context.getAnonymityState();
    }

    @Override
    public boolean isTransferable() throws GSSException {
        return context.isTransferable();
    }

    @Override
    public boolean isProtReady() {
        return context.isProtReady();
    }

    @Override
    public boolean getConfState() {
        return context.getConfState();
    }

    @Override
    public boolean getIntegState() {
        return context.getIntegState();
    }

    @Override
    public int getLifetime() {
        return context.getLifetime();
    }

    @Override
    public GSSName getSrcName() throws GSSException {
        return context.getSrcName();
    }

    @Override
    public GSSName getTargName() throws GSSException {
        return context.getTargName();
    }

    @Override
    public Oid getMech() throws GSSException {
        return context.getMech();
    }

    @Override
    public GSSCredential getDelegCred() throws GSSException {
        return context.getDelegCred();
    }

    @Override
    public boolean isInitiator() throws GSSException {
        return context.isInitiator();
    }
}
