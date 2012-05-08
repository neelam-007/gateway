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

    private static final String SEC_CONTEXT =
            "YIIEvQYJKoZIhvcSAQICAQBuggSsMIIEqKADAgEFoQMCAQ6iBwMFAAAAAACjggPAYYIDvDCCA7ig" +
            "AwIBBaEPGw1RQVdJTjIwMDMuQ09NoiQwIqADAgEAoRswGRsEaHR0cBsRYXdpdHJpc25hLWRlc2t0" +
            "b3CjggN4MIIDdKADAgEXoQMCAQKiggNmBIIDYjNPT7QpmyXglk8UHYhUqvQD7E5iPN/kJtGC41Jl" +
            "3I34iPGohDdYa77ND0gKOww6lyxZNKpcCnHoHccJxFsnAiilU0WUpJmgZsGwUOD34uUL1u7HRW9x" +
            "V/f5Od8ChJcdG10coWEy3/j5h23zH5srrt55JX/3zVj6nzhT/iYld6cvSScj5PCzYVqslsMnmU/t" +
            "kEC5nbbRGWJBT+VQWn+NBm6xZ9hQXD1UqJ/FckF3rLrLlnNKENmUBAbfx2ZO5r7Uc2EBTv8hh9UV" +
            "PS0nWHE5IgLEoH4hhNSOeOR2c/wvuieu+ApgXl7iPG91XCvMV+uWnjES8rsrJS5SWp6ljttjJCZf" +
            "CMQ2OwzaVtu3TMVjuj/LSqAhI0qeuk58pUJCZUWoc7yiy2dEaArZb9afAtQxOqerGJVFTA+qk1xN" +
            "vHf8oncfND077jYcjcGlYDaGHIcOD4OU9JXlx9aP1I3PmiBfaWe84bmV5wDKcYMDmyaFqtM614Wm" +
            "/i+w0T2bOLNXHW67wUrjhgGwX2vnB8RGgCFWatSIako8nELkdIwC6lcOt/0VIsEhbWPUSsDl5T5X" +
            "qaAPXo3S+xOkbjHS9dJ7UND/OH1p9XGn63XxPcx3wKdGmL9aQ71ulDaMt5uafxkZ6zjb+WXf00/E" +
            "UuARcoGJTamnS/3LqEpnxO3NdPOUDCaQCDqOZukftrFxVA+jlKfdbSZyElX2B7vd86gPvzUvIqLZ" +
            "UY45gJ5yZOzr22Nib+Zob5LEfggqCcamQNBpNvdDaqSELBJJI+BnZ+P4eV7NK7f+oF6jIJ65XcY5" +
            "GheFfl2m06fAb03pDZU/VGD4AwNYrYin9uNk0xL45zJ7TyRzaxnfp/f3wE+nvdFJ3C/AlTaBaMMp" +
            "34PlgPcvG8/2PmkFbHMde9pyevhcitppGVQGjpUvu3kM3Ib+CBtDcb9kvJDhYFzbJ7ukXZ97P56H" +
            "rBxPxAk5reCOZuPGnn+e6O3bG7R8t3Ft4AqI02TYD4N6+09V0ji8Uh7zFiO78rLq2vRf5HvVb5ib" +
            "9SBs4xeV5qEFSxzU5WQmmvl+r9YYx+jkYh0amnpjCH1aS/VgS6lSbS+Np+q/qh/BC76v59k4ZGJB" +
            "n4EoAGT3IjyER1deU8IgVYGXdDFdVuJnx8zGgZuOeJCJdC5i+YbVpIHOMIHLoAMCAReigcMEgcDK" +
            "gk5rcpqPignPQMwRufrF81exw57DxHWZB/O214cUCg3VLn1bxa3IaWi6PROpRHrWCnxpSllTYfip" +
            "SDpSLXxkcI6cUlqqqZOaEMuM/vPb0kXWL1Vjb4INyRFbmso9Sr16LyfZFznBK3+OL0LmY7Ibqkzi" +
            "Hs33BdQNak+2Ia+fl9M/Iccyw5xOyWIQWvGRaBZf+ApuajwL8X2U3mT9SFDfaSZmmDhKrDYuSLYP" +
            "yFTba3yWqycanoQV+6urHd6aaHE=";
    
    private static final String TICKET = 
            "rO0ABXNyACtqYXZheC5zZWN1cml0eS5hdXRoLmtlcmJlcm9zLktlcmJlcm9zVGlja2V0ZqGBbXB3" +
            "w7sCAApbAAxhc24xRW5jb2Rpbmd0AAJbQkwACGF1dGhUaW1ldAAQTGphdmEvdXRpbC9EYXRlO0wA" +
            "BmNsaWVudHQAMExqYXZheC9zZWN1cml0eS9hdXRoL2tlcmJlcm9zL0tlcmJlcm9zUHJpbmNpcGFs" +
            "O1sAD2NsaWVudEFkZHJlc3Nlc3QAF1tMamF2YS9uZXQvSW5ldEFkZHJlc3M7TAAHZW5kVGltZXEA" +
            "fgACWwAFZmxhZ3N0AAJbWkwACXJlbmV3VGlsbHEAfgACTAAGc2VydmVycQB+AANMAApzZXNzaW9u" +
            "S2V5dAAmTGphdmF4L3NlY3VyaXR5L2F1dGgva2VyYmVyb3MvS2V5SW1wbDtMAAlzdGFydFRpbWVx" +
            "AH4AAnhwdXIAAltCrPMX+AYIVOACAAB4cAAAA8BhggO8MIIDuKADAgEFoQ8bDVFBV0lOMjAwMy5D" +
            "T02iJDAioAMCAQChGzAZGwRodHRwGxFhd2l0cmlzbmEtZGVza3RvcKOCA3gwggN0oAMCARehAwIB" +
            "AqKCA2YEggNiFlV0Aop9DBaxDjXTpxGb2vgGy/dLUt8mo604G8b+Isd3My59IQ22+SUgi7PzMZ8u" +
            "K1kLTEq/MDf4WYRDWijaw0DKm1lFRCuSYa9mQSlNnSbNEj2z11nQ20D0rW0jbxFSFhIW01kMHUyi" +
            "O6gbQKkmiBGhvCkjiSI6RLA9FgKgnidxXknUGtFbpsIYTQbCG0PxwJKtV1KyCCxV/ae0jz8CPZaW" +
            "nYQmKNR5pSB4OEgGLGxiYtybZV+nKIqvFl1OeE+RIUoA75fibXCZ6aWAsAmSG98YGM/JG9hN+Qgm" +
            "JvdaG//uEpUgb/Jo2YEd3yHZhmeiWCliS5awKIWBkz5iutsYupsCNRWW83rkzTgd4nSPPO6JvHLf" +
            "/U/LX+Z1qq++kEfA47sR4QCVKePFeqODuXUP5eo0QawRx8Q74uXfSGK6Ta3Q8YgtPX39lkG84Cl6" +
            "zBUCZFBxWinrViILNnuT9RfqMrQeE5hIgHBzj5D/XVa6rYBmPeAetn2LM2KjoX6NyOTbwa9tZk4r" +
            "sjXJ02f4u02couyC9XdaZ3HhV2bc9vCHk34vGSKLCQJJTsXomnt5TounGvMCJZ6t4ug4bZyfEEfJ" +
            "Wsn4k+sJDmRId6jZQmTu95utz8GESL0a0dRy+QZ0EH+W2B9DbY+spGgAnwyya7AmuNhM+pJsdFN5" +
            "ZrxjRaAsOmHKTfTNIVL3tp89u/hpQxhAkRKZiOW5ZmERVJBXmhfsyW4S4sv6WiVhQ8SQ7XjESpPq" +
            "Vl4UliSc+wW00KJW65O7RqO6Dulz6JfuOtDoVvll/77gPttIqdTByyUSzCwXpL/A6X0DOpNJre+u" +
            "06lSZSL+rl5NYTnAQE+8QqrnRxsbqnoDda5ydEGHJ15gUm8v82K7sK45k3+eNvmXdpN1rKMgtSTk" +
            "aG6DfqISHZBCGUBK0E7CMRhQ9kFnWngAcz11OFRpAdThyBTxxB4HoISM4wZNuLsGfHIfKYiG3XDu" +
            "Pi9jPcrystOY/cxsS8qVYkJWf98LEyU+T34m1lIDXigQ3ea09Uw2qQskPYEGLZbC5jsfeR1x3Pwr" +
            "c5kVtlMg55hsaWCrjBQXGF7WaM+/q2aDhRGbMM0aRxmg3MPGeNr3rHRSNZfQ23Kd3+h1/nkj/z8+" +
            "GKnbgJhf64yHHGYWsnMZ/ZwBAPNzcgAOamF2YS51dGlsLkRhdGVoaoEBS1l0GQMAAHhwdwgAAAE3" +
            "FGG6EHhzcgAuamF2YXguc2VjdXJpdHkuYXV0aC5rZXJiZXJvcy5LZXJiZXJvc1ByaW5jaXBhbJmn" +
            "fV0PHjMpAwAAeHB1cQB+AAgAAAAkMCKgAwIBAaEbMBkbBGh0dHAbEWF3aXRyaXNuYS1kZXNrdG9w" +
            "dXEAfgAIAAAADxsNUUFXSU4yMDAzLkNPTXhwc3EAfgAKdwgAAAE3FocLEHh1cgACW1pXjyA5FLhd" +
            "4gIAAHhwAAAAIAAAAAAAAAAAAAABAAAAAAAAAAAAAAAAAAAAAAAAAAAAcHNxAH4ADHVxAH4ACAAA" +
            "ACQwIqADAgECoRswGRsEaHR0cBsRYXdpdHJpc25hLWRlc2t0b3B1cQB+AAgAAAAPGw1RQVdJTjIw" +
            "MDMuQ09NeHNyACRqYXZheC5zZWN1cml0eS5hdXRoLmtlcmJlcm9zLktleUltcGySg4boPK9L1wMA" +
            "AHhwdXEAfgAIAAAAGzAZoAMCARehEgQQlYF2YDx4tC8tDKNEMqUlGnhzcQB+AAp3CAAAATcUYboQ" +
            "eA==";

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
