package com.l7tech.external.assertions.xmppassertion.server;

import com.l7tech.common.mime.NoSuchPartException;
import com.l7tech.common.mime.StashManager;
import com.l7tech.server.StashManagerFactory;

import java.io.IOException;
import java.io.InputStream;

/**
 * User: rseminoff
 * Date: 24/05/12
 *
 * Copied from com.l7tech.external.assertions.as2protocol.server.MockStashManagerFactory
 */
public class MockStashManagerFactory implements StashManagerFactory {
    @Override
    public StashManager createStashManager() {

        return new StashManager() {
            @Override
            // TODO: THIS MUST BE IMPLEMENTED FOR THE XMPP TLS Methods
            public void stash(int ordinal, InputStream in) throws IOException {
                System.out.println("** used: stash(int, InputStream)");
            }

            @Override
            public void stash(int ordinal, byte[] in) throws IOException {
                System.out.println("** used: stash(int, byte[])");
            }

            @Override
            public void stash(int ordinal, byte[] in, int offset, int length) throws IOException {
                System.out.println("** used: stash(int, byte[], int, int)");
            }

            @Override
            public void unstash(int ordinal) {
                System.out.println("** used: unstash(int)");
            }

            @Override
            public long getSize(int ordinal) {
                System.out.println("** used: getSize(int)");
                return 0;
            }

            @Override
            public InputStream recall(int ordinal) throws IOException, NoSuchPartException {
                System.out.println("** used: recall(int)");
                return null;
            }

            @Override
            public boolean isByteArrayAvailable(int ordinal) {
                System.out.println("** used: isByteArrayAvailable(int)");
                return false;
            }

            @Override
            // TODO: THIS MUST BE IMPLEMENTED FOR THE XMPP TLS Storage
            public byte[] recallBytes(int ordinal) throws NoSuchPartException {
                System.out.println("** used: recallBytes(int)");
                return new byte[0];  //To change body of implemented methods use File | Settings | File Templates.
            }

            @Override
            public boolean peek(int ordinal) {
                System.out.println("** used: peek(int)");
                return false;
            }

            @Override
            public int getMaxOrdinal() {
                System.out.println("** used: getMaxOrdinal()");
                return 0;
            }

            @Override
            public void close() {
                System.out.println("** used: close()");
            }
        };

    }
}
