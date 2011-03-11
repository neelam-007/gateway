package com.l7tech.external.assertions.script.console;

import org.aspectj.apache.bcel.Constants;
import org.aspectj.apache.bcel.classfile.ConstantPool;
import org.aspectj.apache.bcel.generic.*;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

/**
 *
 */
public class CompiledScriptAssertionMaker implements Constants {
    public static byte[] generateAssertionBeanClassBytes(String name, String paletteFolder) throws IOException {
        ClassGen _cg = new ClassGen("com.l7tech.external.assertions.compiledscript." + name.toLowerCase() + "." + name + "Assertion",
                "com.l7tech.policy.assertion.Assertion",
                name + "Assertion.java",
                ACC_PUBLIC | ACC_SUPER,
                new String[] {  });
        ConstantPool _cp = _cg.getConstantPool();
        InstructionFactory _factory = new InstructionFactory(_cg, _cp);

        // Constructor
        {
            InstructionList il = new InstructionList();
            MethodGen method = new MethodGen(ACC_PUBLIC, Type.VOID, Type.NO_ARGS, new String[] {  }, "<init>",
                    "com.l7tech.external.assertions.script." + name + "Assertion", il, _cp);

            il.append(InstructionFactory.createLoad(Type.OBJECT, 0));
            il.append(_factory.createInvoke("com.l7tech.policy.assertion.Assertion", "<init>", Type.VOID, Type.NO_ARGS, Constants.INVOKESPECIAL));
            il.append(InstructionFactory.createReturn(Type.VOID));
            method.setMaxStack();
            method.setMaxLocals();
            _cg.addMethod(method.getMethod());
            il.dispose();
        }

        // meta()
        {
            InstructionList il = new InstructionList();
            MethodGen method = new MethodGen(ACC_PUBLIC, new ObjectType("com.l7tech.policy.assertion.AssertionMetadata"), Type.NO_ARGS, new String[] {  },
                    "meta", "com.l7tech.external.assertions.script.SampleCsAssertion", il, _cp);

            il.append(InstructionFactory.createLoad(Type.OBJECT, 0));
            il.append(_factory.createInvoke("com.l7tech.policy.assertion.Assertion", "defaultMeta", new ObjectType("com.l7tech.policy.assertion.DefaultAssertionMetadata"), Type.NO_ARGS, Constants.INVOKESPECIAL));
            il.append(InstructionFactory.createStore(Type.OBJECT, 1));
            il.append(InstructionFactory.createLoad(Type.OBJECT, 1));
            il.append(InstructionFactory.PUSH(_cp, "paletteFolders"));
            il.append(InstructionFactory.PUSH(_cp, 1));
            il.append(_factory.createNewArray(Type.STRING, (short) 1));
            il.append(InstructionConstants.DUP);
            il.append(InstructionFactory.PUSH(_cp, 0));
            il.append(InstructionFactory.PUSH(_cp, paletteFolder));
            il.append(InstructionConstants.AASTORE);
            il.append(_factory.createInvoke("com.l7tech.policy.assertion.DefaultAssertionMetadata", "put", Type.VOID, new Type[] { Type.STRING, Type.OBJECT }, Constants.INVOKEVIRTUAL));
            il.append(InstructionFactory.createLoad(Type.OBJECT, 1));
            il.append(InstructionFactory.createReturn(Type.OBJECT));
            method.setMaxStack();
            method.setMaxLocals();
            _cg.addMethod(method.getMethod());
            il.dispose();
        }

        ByteArrayOutputStream os = new ByteArrayOutputStream();
        _cg.getJavaClass().dump(os);
        return os.toByteArray();
    }

    public static byte[] generateServerAssertionClassBytes(String name, String bsfLanguageName, String script) throws IOException {
        final String serverPackage = "com.l7tech.external.assertions.compiledscript." + name.toLowerCase() + ".server";
        final String serverClassName = serverPackage + ".Server" + name + "Assertion";
        ClassGen _cg = new ClassGen(serverClassName,
                "java.lang.Object",
                "Server" + name + "Assertion.java",
                ACC_PUBLIC | ACC_SUPER,
                new String[] { "com.l7tech.server.policy.assertion.ServerAssertion" });
        ConstantPool _cp = _cg.getConstantPool();
        InstructionFactory _factory = new InstructionFactory(_cg, _cp);

        FieldGen field = new FieldGen(ACC_PRIVATE | ACC_FINAL, new ObjectType("com.l7tech.server.policy.assertion.ServerAssertion"), "delegate", _cp);
        _cg.addField(field.getField());

        // Constructor
        {
            InstructionList il = new InstructionList();
            MethodGen method = new MethodGen(ACC_PUBLIC, Type.VOID, new Type[] { new ObjectType("com.l7tech.external.assertions.compiledscript." + name.toLowerCase() + "." + name + "Assertion"),
                    new ObjectType("org.springframework.context.ApplicationContext") }, new String[] { "assertion", "applicationContext"  },
                    "<init>", serverClassName, il, _cp);
            il.append(InstructionFactory.createLoad(Type.OBJECT, 0));
            il.append(_factory.createInvoke("java.lang.Object", "<init>", Type.VOID, Type.NO_ARGS, Constants.INVOKESPECIAL));
            il.append(InstructionFactory.createLoad(Type.OBJECT, 0));
            il.append(InstructionFactory.createLoad(Type.OBJECT, 1));
            il.append(InstructionFactory.PUSH(_cp, name));
            il.append(InstructionFactory.PUSH(_cp, bsfLanguageName));
            il.append(InstructionFactory.PUSH(_cp, script));
            il.append(InstructionFactory.createLoad(Type.OBJECT, 2));
            il.append(_factory.createInvoke("com.l7tech.external.assertions.script.server.ServerScriptAssertionSupport", "createServerAssertion",
                    new ObjectType("com.l7tech.server.policy.assertion.ServerAssertion"), new Type[] { new ObjectType("com.l7tech.policy.assertion.Assertion"),
                            Type.STRING, Type.STRING, Type.STRING, new ObjectType("org.springframework.context.ApplicationContext") }, Constants.INVOKESTATIC));
            il.append(_factory.createFieldAccess(serverClassName, "delegate", new ObjectType("com.l7tech.server.policy.assertion.ServerAssertion"), Constants.PUTFIELD));
            il.append(InstructionFactory.createReturn(Type.VOID));
            method.setMaxStack();
            method.setMaxLocals();
            _cg.addMethod(method.getMethod());
            il.dispose();
        }

        // checkRequest()
        {
            InstructionList il = new InstructionList();
            MethodGen method = new MethodGen(ACC_PUBLIC, new ObjectType("com.l7tech.policy.assertion.AssertionStatus"),
                    new Type[] { new ObjectType("com.l7tech.server.message.PolicyEnforcementContext") }, new String[] { "context" }, "checkRequest",
                    serverClassName, il, _cp);

            il.append(InstructionFactory.createLoad(Type.OBJECT, 0));
            il.append(_factory.createFieldAccess(serverClassName, "delegate", new ObjectType("com.l7tech.server.policy.assertion.ServerAssertion"), Constants.GETFIELD));
            il.append(InstructionFactory.createLoad(Type.OBJECT, 1));
            il.append(_factory.createInvoke("com.l7tech.server.policy.assertion.ServerAssertion", "checkRequest",
                    new ObjectType("com.l7tech.policy.assertion.AssertionStatus"), new Type[] { new ObjectType("com.l7tech.server.message.PolicyEnforcementContext") }, Constants.INVOKEINTERFACE));
            il.append(InstructionFactory.createReturn(Type.OBJECT));
            method.setMaxStack();
            method.setMaxLocals();
            _cg.addMethod(method.getMethod());
            il.dispose();
        }

        // getAssertion()
        {
            InstructionList il = new InstructionList();
            MethodGen method = new MethodGen(ACC_PUBLIC, new ObjectType("com.l7tech.policy.assertion.Assertion"), Type.NO_ARGS,
                    new String[] {  }, "getAssertion", serverClassName, il, _cp);

            il.append(InstructionFactory.createLoad(Type.OBJECT, 0));
            il.append(_factory.createFieldAccess(serverClassName, "delegate", new ObjectType("com.l7tech.server.policy.assertion.ServerAssertion"), Constants.GETFIELD));
            il.append(_factory.createInvoke("com.l7tech.server.policy.assertion.ServerAssertion", "getAssertion", new ObjectType("com.l7tech.policy.assertion.Assertion"),
                    Type.NO_ARGS, Constants.INVOKEINTERFACE));
            il.append(InstructionFactory.createReturn(Type.OBJECT));
            method.setMaxStack();
            method.setMaxLocals();
            _cg.addMethod(method.getMethod());
            il.dispose();
        }

        // close()
        {
            InstructionList il = new InstructionList();
            MethodGen method = new MethodGen(ACC_PUBLIC, Type.VOID, Type.NO_ARGS, new String[] {  }, "close", serverClassName, il, _cp);

            il.append(InstructionFactory.createLoad(Type.OBJECT, 0));
            il.append(_factory.createFieldAccess(serverClassName, "delegate", new ObjectType("com.l7tech.server.policy.assertion.ServerAssertion"), Constants.GETFIELD));
            il.append(_factory.createInvoke("com.l7tech.server.policy.assertion.ServerAssertion", "close", Type.VOID, Type.NO_ARGS, Constants.INVOKEINTERFACE));
            il.append(InstructionFactory.createReturn(Type.VOID));
            method.setMaxStack();
            method.setMaxLocals();
            _cg.addMethod(method.getMethod());
            il.dispose();
        }

        ByteArrayOutputStream os = new ByteArrayOutputStream();
        _cg.getJavaClass().dump(os);
        return os.toByteArray();
    }
}
