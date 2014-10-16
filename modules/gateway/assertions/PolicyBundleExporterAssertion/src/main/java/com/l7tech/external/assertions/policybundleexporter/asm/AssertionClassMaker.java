package com.l7tech.external.assertions.policybundleexporter.asm;

import org.objectweb.asm.*;

import java.io.IOException;

/**
 * Generate the Assertion class for the Policy Bundle Installer (using ASM byte code library).
 */
public class AssertionClassMaker extends AbstractClassMaker {


    public AssertionClassMaker(final String subPackageInstallerName, final String installerCamelName, final String camelName) {
        super(subPackageInstallerName, installerCamelName, camelName);
    }

    /*
     L7 replaced:
      - "com/l7tech/external/assertions/simplepolicybundleinstaller/SimplePolicyBundleInstallerAssertion" with assertionClassName
      - "(Lcom/l7tech/external/assertions/simplepolicybundleinstaller/SimplePolicyBundleInstallerAssertion;)V" with "(L" + assertionClassName + ";)V"
      - "Lcom/l7tech/external/assertions/simplepolicybundleinstaller/SimplePolicyBundleInstallerAssertion;" with "L" + assertionClassName + ";"
      - "com/l7tech/external/assertions/simplepolicybundleinstaller/SimplePolicyBundleInstallerAssertion$1" with assertion$1ClassName
      - "com.l7tech.external.assertions.simplepolicybundleinstaller.console.SimplePolicyBundleInstallerAction" with actionDotClassName
      - "com.l7tech.external.assertions.simplepolicybundleinstaller.SimplePolicyBundleInstallerAdminImpl" with adminImplDotClassName
      - Simple Policy Bundle with " + name + "

      Also customized changes tag with ***L7 CUSTOMIZED CHANGE START*** / ***L7 CUSTOMIZED CHANGE END***
    */

    /*
      The following code was generated: java -cp ./lib/repository/asm/asm-5.0.3.jar;./lib/tools/asm-util-5.0.3.jar;
      ./idea-classes/production/SimplePolicyBundleInstallerAssertion org.objectweb.asm.util.ASMifier
      com.l7tech.external.assertions.simplepolicybundleinstaller.SimplePolicyBundleInstallerAssertion
    */
    public byte[] generate() throws IOException {
        ClassWriter cw = new ClassWriter(0);
        FieldVisitor fv;
        MethodVisitor mv;

        cw.visit(V1_7, ACC_PUBLIC + ACC_SUPER, assertionClassName, null, "com/l7tech/policy/assertion/Assertion", null);

        cw.visitInnerClass(assertion$1ClassName, null, null, 0);

        {
            fv = cw.visitField(ACC_PROTECTED + ACC_FINAL + ACC_STATIC, "logger", "Ljava/util/logging/Logger;", null, null);
            fv.visitEnd();
        }
        {
            fv = cw.visitField(ACC_PRIVATE + ACC_FINAL + ACC_STATIC, "META_INITIALIZED", "Ljava/lang/String;", null, null);
            fv.visitEnd();
        }
        {
            mv = cw.visitMethod(ACC_PUBLIC, "<init>", "()V", null, null);
            mv.visitCode();
            mv.visitVarInsn(ALOAD, 0);
            mv.visitMethodInsn(INVOKESPECIAL, "com/l7tech/policy/assertion/Assertion", "<init>", "()V", false);
            mv.visitInsn(RETURN);
            mv.visitMaxs(1, 1);
            mv.visitEnd();
        }
        {
            mv = cw.visitMethod(ACC_PUBLIC, "meta", "()Lcom/l7tech/policy/assertion/AssertionMetadata;", null, null);
            mv.visitCode();
            mv.visitVarInsn(ALOAD, 0);
            mv.visitMethodInsn(INVOKESPECIAL, "com/l7tech/policy/assertion/Assertion", "defaultMeta", "()Lcom/l7tech/policy/assertion/DefaultAssertionMetadata;", false);
            mv.visitVarInsn(ASTORE, 1);
            mv.visitFieldInsn(GETSTATIC, "java/lang/Boolean", "TRUE", "Ljava/lang/Boolean;");
            mv.visitVarInsn(ALOAD, 1);
            mv.visitFieldInsn(GETSTATIC, assertionClassName, "META_INITIALIZED", "Ljava/lang/String;");
            mv.visitMethodInsn(INVOKEVIRTUAL, "com/l7tech/policy/assertion/DefaultAssertionMetadata", "get", "(Ljava/lang/String;)Ljava/lang/Object;", false);
            mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/Boolean", "equals", "(Ljava/lang/Object;)Z", false);
            Label l0 = new Label();
            mv.visitJumpInsn(IFEQ, l0);
            mv.visitVarInsn(ALOAD, 1);
            mv.visitInsn(ARETURN);
            mv.visitLabel(l0);
            mv.visitFrame(F_APPEND,1, new Object[] {"com/l7tech/policy/assertion/DefaultAssertionMetadata"}, 0, null);
            mv.visitVarInsn(ALOAD, 1);
            mv.visitLdcInsn("extensionInterfacesFactory");
            mv.visitTypeInsn(NEW, assertion$1ClassName);
            mv.visitInsn(DUP);
            mv.visitVarInsn(ALOAD, 0);
            mv.visitMethodInsn(INVOKESPECIAL, assertion$1ClassName, "<init>", "(L" + assertionClassName + ";)V", false);
            mv.visitMethodInsn(INVOKEVIRTUAL, "com/l7tech/policy/assertion/DefaultAssertionMetadata", "put", "(Ljava/lang/String;Ljava/lang/Object;)V", false);
            mv.visitVarInsn(ALOAD, 1);
            mv.visitLdcInsn("globalActionClassnames");
            mv.visitInsn(ICONST_1);
            mv.visitTypeInsn(ANEWARRAY, "java/lang/String");
            mv.visitInsn(DUP);
            mv.visitInsn(ICONST_0);
            mv.visitLdcInsn(actionDotClassName);
            mv.visitInsn(AASTORE);
            mv.visitMethodInsn(INVOKEVIRTUAL, "com/l7tech/policy/assertion/DefaultAssertionMetadata", "put", "(Ljava/lang/String;Ljava/lang/Object;)V", false);
            mv.visitVarInsn(ALOAD, 1);
            mv.visitLdcInsn("moduleLoadListenerClassname");
            mv.visitLdcInsn(adminImplDotClassName);
            mv.visitMethodInsn(INVOKEVIRTUAL, "com/l7tech/policy/assertion/DefaultAssertionMetadata", "put", "(Ljava/lang/String;Ljava/lang/Object;)V", false);
            mv.visitVarInsn(ALOAD, 1);
            mv.visitLdcInsn("featureSetName");
            mv.visitLdcInsn("set:modularAssertions");
            mv.visitMethodInsn(INVOKEVIRTUAL, "com/l7tech/policy/assertion/DefaultAssertionMetadata", "put", "(Ljava/lang/String;Ljava/lang/Object;)V", false);
            mv.visitVarInsn(ALOAD, 1);
            mv.visitFieldInsn(GETSTATIC, assertionClassName, "META_INITIALIZED", "Ljava/lang/String;");
            mv.visitFieldInsn(GETSTATIC, "java/lang/Boolean", "TRUE", "Ljava/lang/Boolean;");
            mv.visitMethodInsn(INVOKEVIRTUAL, "com/l7tech/policy/assertion/DefaultAssertionMetadata", "put", "(Ljava/lang/String;Ljava/lang/Object;)V", false);
            mv.visitVarInsn(ALOAD, 1);
            mv.visitInsn(ARETURN);
            mv.visitMaxs(6, 2);
            mv.visitEnd();
        }
        {
            mv = cw.visitMethod(ACC_STATIC, "<clinit>", "()V", null, null);
            mv.visitCode();
            mv.visitLdcInsn(Type.getType("L" + assertionClassName + ";"));
            mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/Class", "getName", "()Ljava/lang/String;", false);
            mv.visitMethodInsn(INVOKESTATIC, "java/util/logging/Logger", "getLogger", "(Ljava/lang/String;)Ljava/util/logging/Logger;", false);
            mv.visitFieldInsn(PUTSTATIC, assertionClassName, "logger", "Ljava/util/logging/Logger;");
            mv.visitTypeInsn(NEW, "java/lang/StringBuilder");
            mv.visitInsn(DUP);
            mv.visitMethodInsn(INVOKESPECIAL, "java/lang/StringBuilder", "<init>", "()V", false);
            mv.visitLdcInsn(Type.getType("L" + assertionClassName + ";"));
            mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/Class", "getName", "()Ljava/lang/String;", false);
            mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/StringBuilder", "append", "(Ljava/lang/String;)Ljava/lang/StringBuilder;", false);
            mv.visitLdcInsn(".metadataInitialized");
            mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/StringBuilder", "append", "(Ljava/lang/String;)Ljava/lang/StringBuilder;", false);
            mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/StringBuilder", "toString", "()Ljava/lang/String;", false);
            mv.visitFieldInsn(PUTSTATIC, assertionClassName, "META_INITIALIZED", "Ljava/lang/String;");
            mv.visitInsn(RETURN);
            mv.visitMaxs(2, 0);
            mv.visitEnd();
        }
        cw.visitEnd();

        return cw.toByteArray();
    }

    /*
      The following code was generated: java -cp ./lib/repository/asm/asm-5.0.3.jar;./lib/tools/asm-util-5.0.3.jar;
      ./idea-classes/production/SimplePolicyBundleInstallerAssertion org.objectweb.asm.util.ASMifier
      com.l7tech.external.assertions.simplepolicybundleinstaller.SimplePolicyBundleInstallerAssertion
    */
    public byte[] generate$1() throws IOException {
        ClassWriter cw = new ClassWriter(0);
        FieldVisitor fv;
        MethodVisitor mv;

        cw.visit(V1_7, ACC_SUPER, assertion$1ClassName, "Ljava/lang/Object;Lcom/l7tech/util/Functions$Unary<Ljava/util/Collection<Lcom/l7tech/policy/assertion/ExtensionInterfaceBinding;>;Lorg/springframework/context/ApplicationContext;>;", "java/lang/Object", new String[] { "com/l7tech/util/Functions$Unary" });

        cw.visitOuterClass(assertionClassName, "meta", "()Lcom/l7tech/policy/assertion/AssertionMetadata;");

        cw.visitInnerClass(assertion$1ClassName, null, null, 0);

        cw.visitInnerClass("com/l7tech/gateway/common/admin/PolicyBundleInstallerAdmin$PolicyBundleInstallerException", "com/l7tech/gateway/common/admin/PolicyBundleInstallerAdmin", "PolicyBundleInstallerException", ACC_PUBLIC + ACC_STATIC);

        cw.visitInnerClass("com/l7tech/util/Functions$Unary", "com/l7tech/util/Functions", "Unary", ACC_PUBLIC + ACC_STATIC + ACC_ABSTRACT + ACC_INTERFACE);

        {
            fv = cw.visitField(ACC_FINAL + ACC_SYNTHETIC, "this$0", "L" + assertionClassName + ";", null, null);
            fv.visitEnd();
        }
        {
            mv = cw.visitMethod(0, "<init>", "(L" + assertionClassName + ";)V", null, null);
            mv.visitCode();
            mv.visitVarInsn(ALOAD, 0);
            mv.visitVarInsn(ALOAD, 1);
            mv.visitFieldInsn(PUTFIELD, assertion$1ClassName, "this$0", "L" + assertionClassName + ";");
            mv.visitVarInsn(ALOAD, 0);
            mv.visitMethodInsn(INVOKESPECIAL, "java/lang/Object", "<init>", "()V", false);
            mv.visitInsn(RETURN);
            mv.visitMaxs(2, 2);
            mv.visitEnd();
        }
        {
            mv = cw.visitMethod(ACC_PUBLIC, "call", "(Lorg/springframework/context/ApplicationContext;)Ljava/util/Collection;", "(Lorg/springframework/context/ApplicationContext;)Ljava/util/Collection<Lcom/l7tech/policy/assertion/ExtensionInterfaceBinding;>;", null);
            mv.visitCode();
            Label l0 = new Label();
            Label l1 = new Label();
            Label l2 = new Label();
            mv.visitTryCatchBlock(l0, l1, l2, "com/l7tech/gateway/common/admin/PolicyBundleInstallerAdmin$PolicyBundleInstallerException");
            mv.visitLabel(l0);
            mv.visitTypeInsn(NEW, adminImplClassName);
            mv.visitInsn(DUP);
            mv.visitLdcInsn("/com/l7tech/external/assertions/" + subPackageInstallerName.toLowerCase() + "/bundles/");

            // *** L7 CUSTOMIZED CHANGE START***
            // TODO make this use logic in com.l7tech.external.assertions.policybundleinstaller.export.AarFileGenerator.addPolicyBundleInfo(...)
            String policyBundleName = camelName + "PolicyBundle";
            if (camelName.contains("PolicyBundle")) {
                policyBundleName = camelName;
            }
            mv.visitLdcInsn(policyBundleName + "Info.xml");
            mv.visitLdcInsn("http://ns.l7tech.com/2013/10/" + policyBundleName.toLowerCase());
            // *** L7 CUSTOMIZED CHANGE END***

            mv.visitVarInsn(ALOAD, 1);
            mv.visitMethodInsn(INVOKESPECIAL, adminImplClassName, "<init>", "(Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;Lorg/springframework/context/ApplicationEventPublisher;)V", false);
            mv.visitVarInsn(ASTORE, 2);
            mv.visitVarInsn(ALOAD, 1);
            mv.visitLdcInsn("injector");
            mv.visitLdcInsn(Type.getType("Lcom/l7tech/server/util/Injector;"));
            mv.visitMethodInsn(INVOKEINTERFACE, "org/springframework/context/ApplicationContext", "getBean", "(Ljava/lang/String;Ljava/lang/Class;)Ljava/lang/Object;", true);
            mv.visitTypeInsn(CHECKCAST, "com/l7tech/server/util/Injector");
            mv.visitVarInsn(ASTORE, 3);
            mv.visitVarInsn(ALOAD, 3);
            mv.visitVarInsn(ALOAD, 2);
            mv.visitMethodInsn(INVOKEINTERFACE, "com/l7tech/server/util/Injector", "inject", "(Ljava/lang/Object;)V", true);
            mv.visitLabel(l1);
            Label l3 = new Label();
            mv.visitJumpInsn(GOTO, l3);
            mv.visitLabel(l2);
            mv.visitFrame(F_SAME1, 0, null, 1, new Object[] {"com/l7tech/gateway/common/admin/PolicyBundleInstallerAdmin$PolicyBundleInstallerException"});
            mv.visitVarInsn(ASTORE, 3);
            mv.visitFieldInsn(GETSTATIC, assertionClassName, "logger", "Ljava/util/logging/Logger;");
            mv.visitTypeInsn(NEW, "java/lang/StringBuilder");
            mv.visitInsn(DUP);
            mv.visitMethodInsn(INVOKESPECIAL, "java/lang/StringBuilder", "<init>", "()V", false);
            mv.visitLdcInsn("Could not load " + camelName + " Installer: ");  // TODO add space between words, e.g. "Simple Policy Bundle"
            mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/StringBuilder", "append", "(Ljava/lang/String;)Ljava/lang/StringBuilder;", false);
            mv.visitVarInsn(ALOAD, 3);
            mv.visitMethodInsn(INVOKESTATIC, "com/l7tech/util/ExceptionUtils", "getMessage", "(Ljava/lang/Throwable;)Ljava/lang/String;", false);
            mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/StringBuilder", "append", "(Ljava/lang/String;)Ljava/lang/StringBuilder;", false);
            mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/StringBuilder", "toString", "()Ljava/lang/String;", false);
            mv.visitMethodInsn(INVOKEVIRTUAL, "java/util/logging/Logger", "warning", "(Ljava/lang/String;)V", false);
            mv.visitTypeInsn(NEW, "java/lang/RuntimeException");
            mv.visitInsn(DUP);
            mv.visitVarInsn(ALOAD, 3);
            mv.visitMethodInsn(INVOKESPECIAL, "java/lang/RuntimeException", "<init>", "(Ljava/lang/Throwable;)V", false);
            mv.visitInsn(ATHROW);
            mv.visitLabel(l3);
            mv.visitFrame(F_APPEND,1, new Object[] {"com/l7tech/gateway/common/admin/PolicyBundleInstallerAdmin"}, 0, null);
            mv.visitTypeInsn(NEW, "com/l7tech/policy/assertion/ExtensionInterfaceBinding");
            mv.visitInsn(DUP);
            mv.visitLdcInsn(Type.getType("Lcom/l7tech/gateway/common/admin/PolicyBundleInstallerAdmin;"));
            mv.visitLdcInsn(Type.getType("L" + assertionClassName + ";"));
            mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/Class", "getName", "()Ljava/lang/String;", false);
            mv.visitVarInsn(ALOAD, 2);
            mv.visitMethodInsn(INVOKESPECIAL, "com/l7tech/policy/assertion/ExtensionInterfaceBinding", "<init>", "(Ljava/lang/Class;Ljava/lang/String;Ljava/lang/Object;)V", false);
            mv.visitVarInsn(ASTORE, 3);
            mv.visitVarInsn(ALOAD, 3);
            mv.visitMethodInsn(INVOKESTATIC, "java/util/Collections", "singletonList", "(Ljava/lang/Object;)Ljava/util/List;", false);
            mv.visitInsn(ARETURN);
            mv.visitMaxs(6, 4);
            mv.visitEnd();
        }
        {
            mv = cw.visitMethod(ACC_PUBLIC + ACC_BRIDGE + ACC_SYNTHETIC, "call", "(Ljava/lang/Object;)Ljava/lang/Object;", null, null);
            mv.visitCode();
            mv.visitVarInsn(ALOAD, 0);
            mv.visitVarInsn(ALOAD, 1);
            mv.visitTypeInsn(CHECKCAST, "org/springframework/context/ApplicationContext");
            mv.visitMethodInsn(INVOKEVIRTUAL, assertion$1ClassName, "call", "(Lorg/springframework/context/ApplicationContext;)Ljava/util/Collection;", false);
            mv.visitInsn(ARETURN);
            mv.visitMaxs(2, 2);
            mv.visitEnd();
        }
        cw.visitEnd();

        return cw.toByteArray();
    }
}
