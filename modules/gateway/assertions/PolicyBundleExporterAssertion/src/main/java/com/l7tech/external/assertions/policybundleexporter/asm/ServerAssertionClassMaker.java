package com.l7tech.external.assertions.policybundleexporter.asm;

import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.MethodVisitor;

import java.io.IOException;

/**
 * Generate the Server Assertion class for the Policy Bundle Installer (using ASM byte code library).
 */
public class ServerAssertionClassMaker extends AbstractClassMaker {

    public ServerAssertionClassMaker(final String subPackageInstallerName, final String installerCamelName, final String camelName) {
        super(subPackageInstallerName, installerCamelName, camelName);
    }

    /*
     L7 replaced:
        - "com/l7tech/external/assertions/simplepolicybundleinstaller/server/ServerSimplePolicyBundleInstallerAssertion" with serverAssertionClassName
        - <Lcom/l7tech/external/assertions/simplepolicybundleinstaller/SimplePolicyBundleInstallerAssertion;> with <L" + assertionClassName + ";>
        - "(Lcom/l7tech/external/assertions/simplepolicybundleinstaller/SimplePolicyBundleInstallerAssertion;)V" with "(L" + assertionClassName + ";)V"
     */

    /*
      The following code was generated: java -cp ./lib/repository/asm/asm-5.0.3.jar;./lib/tools/asm-util-5.0.3.jar;
      ./idea-classes/production/SimplePolicyBundleInstallerAssertion org.objectweb.asm.util.ASMifier
      com.l7tech.external.assertions.simplepolicybundleinstaller.server.ServerSimplePolicyBundleInstallerAssertion
    */
    public byte[] generate() throws IOException {
        ClassWriter cw = new ClassWriter(0);
        MethodVisitor mv;

        cw.visit(V1_7, ACC_PUBLIC + ACC_SUPER, serverAssertionClassName, "Lcom/l7tech/server/policy/assertion/AbstractServerAssertion<L" + assertionClassName + ";>;", "com/l7tech/server/policy/assertion/AbstractServerAssertion", null);

        {
            mv = cw.visitMethod(ACC_PUBLIC, "<init>", "(L" + assertionClassName + ";)V", null, new String[] { "com/l7tech/policy/assertion/PolicyAssertionException" });
            mv.visitCode();
            mv.visitVarInsn(ALOAD, 0);
            mv.visitVarInsn(ALOAD, 1);
            mv.visitMethodInsn(INVOKESPECIAL, "com/l7tech/server/policy/assertion/AbstractServerAssertion", "<init>", "(Lcom/l7tech/policy/assertion/Assertion;)V", false);
            mv.visitInsn(RETURN);
            mv.visitMaxs(2, 2);
            mv.visitEnd();
        }
        {
            mv = cw.visitMethod(ACC_PUBLIC, "checkRequest", "(Lcom/l7tech/server/message/PolicyEnforcementContext;)Lcom/l7tech/policy/assertion/AssertionStatus;", null, new String[] { "java/io/IOException", "com/l7tech/policy/assertion/PolicyAssertionException" });
            mv.visitCode();
            mv.visitFieldInsn(GETSTATIC, "com/l7tech/policy/assertion/AssertionStatus", "FAILED", "Lcom/l7tech/policy/assertion/AssertionStatus;");
            mv.visitInsn(ARETURN);
            mv.visitMaxs(1, 2);
            mv.visitEnd();
        }
        cw.visitEnd();

        return cw.toByteArray();
    }
}
