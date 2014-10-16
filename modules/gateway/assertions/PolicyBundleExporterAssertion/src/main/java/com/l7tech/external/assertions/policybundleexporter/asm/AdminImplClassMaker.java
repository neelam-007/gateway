package com.l7tech.external.assertions.policybundleexporter.asm;

import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;

import java.io.IOException;

/**
 * Generate the AdminImpl class for the Policy Bundle Exporter (using ASM byte code library).
 */
public class AdminImplClassMaker extends AbstractClassMaker {

    public AdminImplClassMaker (final String subPackageInstallerName, final String installerCamelName, final String camelName) {
        super(subPackageInstallerName, installerCamelName, camelName);
    }

    /*
      L7 replaced:
       - "com/l7tech/external/assertions/simplepolicybundleinstaller/SimplePolicyBundleInstallerAdminImpl" with adminImplClassName
       - com/l7tech/external/assertions/simplepolicybundleinstaller/SimplePolicyBundleInstallerAdminImpl with " + adminImplClassName + "
       - Simple Policy Bundle with " + name + "
     */

    /*
      The following code was generated: java -cp ./lib/repository/asm/asm-5.0.3.jar;./lib/tools/asm-util-5.0.3.jar;
      ./idea-classes/production/SimplePolicyBundleInstallerAssertion org.objectweb.asm.util.ASMifier
      com.l7tech.external.assertions.simplepolicybundleinstaller.SimplePolicyBundleInstallerAdminImpl
    */
    public byte[] generate() throws IOException {
        ClassWriter cw = new ClassWriter(0);
        MethodVisitor mv;
        AnnotationVisitor av0;

        cw.visit(V1_7, ACC_PUBLIC + ACC_SUPER, adminImplClassName, null, "com/l7tech/server/policy/bundle/PolicyBundleInstallerAdminAbstractImpl", new String[] { "com/l7tech/gateway/common/admin/PolicyBundleInstallerAdmin" });

        cw.visitInnerClass("com/l7tech/gateway/common/admin/PolicyBundleInstallerAdmin$PolicyBundleInstallerException", "com/l7tech/gateway/common/admin/PolicyBundleInstallerAdmin", "PolicyBundleInstallerException", ACC_PUBLIC + ACC_STATIC);

        {
            mv = cw.visitMethod(ACC_PUBLIC, "<init>", "(Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;Lorg/springframework/context/ApplicationEventPublisher;)V", null, new String[] { "com/l7tech/gateway/common/admin/PolicyBundleInstallerAdmin$PolicyBundleInstallerException" });
            mv.visitCode();
            mv.visitVarInsn(ALOAD, 0);
            mv.visitVarInsn(ALOAD, 1);
            mv.visitVarInsn(ALOAD, 2);
            mv.visitVarInsn(ALOAD, 3);
            mv.visitVarInsn(ALOAD, 4);
            mv.visitMethodInsn(INVOKESPECIAL, "com/l7tech/server/policy/bundle/PolicyBundleInstallerAdminAbstractImpl", "<init>", "(Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;Lorg/springframework/context/ApplicationEventPublisher;)V", false);
            mv.visitInsn(RETURN);
            mv.visitMaxs(5, 5);
            mv.visitEnd();
        }
        {
            mv = cw.visitMethod(ACC_PROTECTED, "getInstallerName", "()Ljava/lang/String;", null, null);
            {
                av0 = mv.visitAnnotation("Lorg/jetbrains/annotations/NotNull;", false);
                av0.visitEnd();
            }
            mv.visitCode();
            mv.visitLdcInsn(camelName);   // TODO add space between words, e.g. "Simple Policy Bundle"
            mv.visitInsn(DUP);
            Label l0 = new Label();
            mv.visitJumpInsn(IFNONNULL, l0);
            mv.visitTypeInsn(NEW, "java/lang/IllegalStateException");
            mv.visitInsn(DUP);
            mv.visitLdcInsn("@NotNull method " + adminImplClassName + ".getInstallerName must not return null");
            mv.visitMethodInsn(INVOKESPECIAL, "java/lang/IllegalStateException", "<init>", "(Ljava/lang/String;)V", false);
            mv.visitInsn(ATHROW);
            mv.visitLabel(l0);
            mv.visitFrame(F_SAME1, 0, null, 1, new Object[] {"java/lang/String"});
            mv.visitInsn(ARETURN);
            mv.visitMaxs(4, 1);
            mv.visitEnd();
        }
        cw.visitEnd();

        return cw.toByteArray();
    }
}
