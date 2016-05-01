/*
 *  Licensed to the Apache Software Foundation (ASF) under one
 *  or more contributor license agreements.  See the NOTICE file
 *  distributed with this work for additional information
 *  regarding copyright ownership.  The ASF licenses this file
 *  to you under the Apache License, Version 2.0 (the
 *  "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 */
package org.codehaus.groovy.parser.antlr4;

import groovy.lang.Closure;
import groovy.lang.IntRange;
import org.antlr.v4.runtime.*;
import org.antlr.v4.runtime.atn.ATN;
import org.antlr.v4.runtime.atn.ATNConfigSet;
import org.antlr.v4.runtime.atn.ParserATNSimulator;
import org.antlr.v4.runtime.atn.PredictionContextCache;
import org.antlr.v4.runtime.dfa.DFA;
import org.antlr.v4.runtime.tree.*;
import org.codehaus.groovy.GroovyBugError;
import org.codehaus.groovy.antlr.EnumHelper;
import org.codehaus.groovy.ast.*;
import org.codehaus.groovy.ast.expr.*;
import org.codehaus.groovy.ast.stmt.*;
import org.codehaus.groovy.control.CompilationFailedException;
import org.codehaus.groovy.control.CompilePhase;
import org.codehaus.groovy.control.SourceUnit;
import org.codehaus.groovy.control.messages.SyntaxErrorMessage;
import org.codehaus.groovy.parser.antlr4.util.StringUtil;
import org.codehaus.groovy.runtime.DefaultGroovyMethods;
import org.codehaus.groovy.runtime.MethodClosure;
import org.codehaus.groovy.syntax.Numbers;
import org.codehaus.groovy.syntax.SyntaxException;
import org.codehaus.groovy.syntax.Types;
import org.objectweb.asm.Opcodes;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

import static org.codehaus.groovy.runtime.DefaultGroovyMethods.*;

@SuppressWarnings("all")
public class ASTBuilder {

    public static final String GROOVY_TRANSFORM_TRAIT = "groovy.transform.Trait";
    public static final String KW_ABSTRACT_STR = "abstract";

    public ASTBuilder(final SourceUnit sourceUnit, ClassLoader classLoader) {
        this.classLoader = classLoader;
        this.sourceUnit = sourceUnit;
        this.moduleNode = new ModuleNode(sourceUnit);

        String text = this.readSourceCode(sourceUnit);

        if (log.isLoggable(Level.FINE)) {
            this.logTokens(text);
        }

        GroovyScanner scanner = new GroovyScanner(new ANTLRInputStream(text));
        GroovyParser parser = new GroovyParser(new CommonTokenStream(scanner));

        // Workaround for ever-growing cache in org.codehaus.groovy.parser.antlr4.GroovyParser#_decisionToDFA
        ATN atn = parser.getATN();
        DFA[] decisionToDFA = new DFA[atn.getNumberOfDecisions()];
        for(int i = 0; i < decisionToDFA.length; ++i) {
            decisionToDFA[i] = new DFA(atn.getDecisionState(i), i);
        }
        parser.setInterpreter(new ParserATNSimulator(
                                    parser, atn, decisionToDFA, new PredictionContextCache()));

        this.setupErrorListener(parser);

        this.startParsing(parser);
    }

    // comments are treated as NL
    public static final Set<Integer> EMPTY_CONTENT_TOKEN_TYPE_SET = new HashSet<Integer>(Arrays.asList(GroovyParser.NL, GroovyParser.EOF, GroovyParser.SEMICOLON));


    /**
     * Check whether the source file just contains newlines, comments and semi colon
     *
     * @param tree
     * @return
     */
    public boolean isEmpty(GroovyParser.CompilationUnitContext tree) {
        for(ParseTree parseTree : tree.children) {
            if (!(parseTree instanceof TerminalNode)) {
                return false;
            }

            if (parseTree instanceof TerminalNode) {
                int tokenType = ((TerminalNode) parseTree).getSymbol().getType();

                if (!EMPTY_CONTENT_TOKEN_TYPE_SET.contains(tokenType)) {
                    return false;
                }
            }
        }

        return true;
    }

    private void startParsing(GroovyParser parser) {
        GroovyParser.CompilationUnitContext tree = parser.compilationUnit();

        if (log.isLoggable(Level.FINE)) {
            this.logTreeStr(tree);
        }

        if (isEmpty(tree)) {
            moduleNode.addStatement(new ReturnStatement(new ConstantExpression(null)));
            return;
        }

        try {
            DefaultGroovyMethods.each(tree.importStatement(), new MethodClosure(this, "parseImportStatement"));
            DefaultGroovyMethods.each(tree.children, new Closure<ClassNode>(this, this) {
                public ClassNode doCall(ParseTree it) {
                    if (it instanceof GroovyParser.ClassDeclarationContext)
                        return parseClassDeclaration((GroovyParser.ClassDeclarationContext)it);
                    else if (it instanceof GroovyParser.PackageDefinitionContext)
                        parsePackageDefinition((GroovyParser.PackageDefinitionContext)it);
                    return null;
                }
            });
            for (GroovyParser.ScriptPartContext part : tree.scriptPart()) {
                if (part.statement() != null) {
                    unpackStatement(moduleNode, parseStatement(part.statement()));
                } else {
                    moduleNode.addMethod(parseScriptMethod(part.methodDeclaration()));
                }
            }
        } catch (CompilationFailedException e) {
            log.severe(createExceptionMessage(e));

            throw e;
        }
    }

    public void parseImportStatement(GroovyParser.ImportStatementContext ctx) {
        ImportNode node;
        List<TerminalNode> qualifiedClassName = new ArrayList<TerminalNode>(ctx.IDENTIFIER());
        boolean isStar = ctx.MULT() != null;
        boolean isStatic = ctx.KW_STATIC() != null;
        String alias = (ctx.KW_AS() != null) ? DefaultGroovyMethods.pop(qualifiedClassName).getText() : null;
        List<AnnotationNode> annotations = parseAnnotations(ctx.annotationClause());

        if (isStar) {
            if (isStatic) {
                // import is like "import static foo.Bar.*"
                // packageName is actually a className in this case
                ClassNode type = ClassHelper.make(DefaultGroovyMethods.join(qualifiedClassName, "."));
                moduleNode.addStaticStarImport(type.getText(), type, annotations);

                node = DefaultGroovyMethods.last(moduleNode.getStaticStarImports().values());
            } else {
                // import is like "import foo.*"
                moduleNode.addStarImport(DefaultGroovyMethods.join(qualifiedClassName, ".") + ".", annotations);

                node = DefaultGroovyMethods.last(moduleNode.getStarImports());
            }

            if (alias != null) throw new GroovyBugError(
                    "imports like 'import foo.* as Bar' are not " +
                            "supported and should be caught by the grammar");
        } else {
            if (isStatic) {
                // import is like "import static foo.Bar.method"
                // packageName is really class name in this case
                String fieldName = DefaultGroovyMethods.pop(qualifiedClassName).getText();
                ClassNode type = ClassHelper.make(DefaultGroovyMethods.join(qualifiedClassName, "."));
                moduleNode.addStaticImport(type, fieldName, alias != null ? alias : fieldName, annotations);

                node = DefaultGroovyMethods.last(moduleNode.getStaticImports().values());
            } else {
                // import is like "import foo.Bar"
                ClassNode type = ClassHelper.make(DefaultGroovyMethods.join(qualifiedClassName, "."));
                if (alias == null) {
                    alias = DefaultGroovyMethods.last(qualifiedClassName).getText();
                }
                moduleNode.addImport(alias, type, annotations);

                node = DefaultGroovyMethods.last(moduleNode.getImports());
            }

        }

        setupNodeLocation(node, ctx);
    }


    public void parsePackageDefinition(GroovyParser.PackageDefinitionContext ctx) {
        moduleNode.setPackageName(DefaultGroovyMethods.join(ctx.IDENTIFIER(), ".") + ".");
        attachAnnotations(moduleNode.getPackage(), ctx.annotationClause());
        setupNodeLocation(moduleNode.getPackage(), ctx);
    }

    private void unpackStatement(ModuleNode destination, Statement stmt) {
        if (stmt instanceof DeclarationList) {
            for (DeclarationExpression decl : ((DeclarationList)stmt).declarations) {
                destination.addStatement(setupNodeLocation(new ExpressionStatement(decl), decl));
            }
        } else {
            destination.addStatement(stmt);
        }
    }

    private void unpackStatement(BlockStatement blockStatement, Statement stmt) {
        if (stmt instanceof DeclarationList) {
            String label = stmt.getStatementLabel();

            for (DeclarationExpression decl : ((DeclarationList)stmt).declarations) {
                Statement declarationStatement = new ExpressionStatement(decl);

                if (null != label) {
                    declarationStatement.setStatementLabel(label);
                }

                blockStatement.addStatement(setupNodeLocation(declarationStatement, decl));
            }
        } else {
            blockStatement.addStatement(stmt);
        }
    }

    /**
     *
     * @param isAnnotationDeclaration   whether the method is defined in an annotation
     * @param hasAnnotation             whether the method declaration has annotations
     * @param hasVisibilityModifier     whether the method declaration contains visibility modifier(e.g. public, protected, private)
     * @param hasModifier               whether the method declaration has modifier(e.g. visibility modifier, final, static and so on)
     * @param hasReturnType             whether the method declaration has an return type(e.g. String, generic types)
     * @param hasDef                    whether the method declaration using def keyword
     * @return                          the result
     *
     */
    private boolean isSyntheticPublic(boolean isAnnotationDeclaration, boolean hasAnnotation, boolean hasVisibilityModifier, boolean hasModifier, boolean hasReturnType, boolean hasDef) {

        if (hasVisibilityModifier) {
            return false;
        }

        if (isAnnotationDeclaration) {
            return true;
        }

        if (hasDef && hasReturnType) {
            return true;
        }

        if (hasModifier || hasAnnotation || !hasReturnType) {
            return true;
        }

        return false;
    }

    private MethodNode parseMethodDeclaration(ClassNode classNode, GroovyParser.MethodDeclarationContext ctx, Closure<MethodNode> createMethodNode) {
        //noinspection GroovyAssignabilityCheck
        final Iterator<Object> iterator = parseModifiers(ctx.memberModifier(), Opcodes.ACC_PUBLIC).iterator();
        int modifiers = ((Integer)(iterator.hasNext() ? iterator.next() : Opcodes.ACC_PUBLIC));

        boolean isAnnotationDeclaration = null != classNode
                && ClassHelper.Annotation_TYPE.equals(classNode.getInterfaces().length > 0 ? classNode.getInterfaces()[0] : null);
        boolean hasVisibilityModifier = ((Boolean)(iterator.hasNext() ? iterator.next() : false));
        boolean hasModifier = 0 != ctx.memberModifier().size();
        boolean hasAnnotation = 0 != ctx.annotationClause().size();
        boolean hasReturnType = (asBoolean(ctx.typeDeclaration()) && !"def".equals(ctx.typeDeclaration().getText()))
                || asBoolean(ctx.genericClassNameExpression());
        boolean hasDef = asBoolean(ctx.KW_DEF);

        innerClassesDefinedInMethod.add(new ArrayList<InnerClassNode>());
        Statement statement = asBoolean(ctx.methodBody())
                ? parseStatement(ctx.methodBody().blockStatement())
                : null;
        List<InnerClassNode> innerClassesDeclared = innerClassesDefinedInMethod.pop();

        Parameter[] params = parseParameters(ctx.argumentDeclarationList());

        ClassNode returnType = asBoolean(ctx.typeDeclaration())
                ? parseTypeDeclaration(ctx.typeDeclaration())
                : asBoolean(ctx.genericClassNameExpression())
                ? parseExpression(ctx.genericClassNameExpression())
                : ClassHelper.OBJECT_TYPE;

        ClassNode[] exceptions = parseThrowsClause(ctx.throwsClause());


        String methodName = (null != ctx.IDENTIFIER()) ? ctx.IDENTIFIER().getText() : parseString(ctx.STRING());

        final MethodNode methodNode = createMethodNode.call(classNode, ctx, methodName, modifiers, returnType, params, exceptions, statement);

        DefaultGroovyMethods.each(innerClassesDeclared, new Closure<MethodNode>(this, this) {
            public MethodNode doCall(InnerClassNode it) {
                it.setEnclosingMethod(methodNode);
                return methodNode;
            }
        });

        setupNodeLocation(methodNode, ctx);
        attachAnnotations(methodNode, ctx.annotationClause());
        methodNode.setSyntheticPublic(isSyntheticPublic(isAnnotationDeclaration, hasAnnotation, hasVisibilityModifier, hasModifier, hasReturnType, hasDef));
        methodNode.setSynthetic(false); // user-defined method are not synthetic

        return methodNode;
    }


    public MethodNode parseScriptMethod(final GroovyParser.MethodDeclarationContext ctx) {

        return parseMethodDeclaration(null, ctx, new Closure<MethodNode>(this, this) {
                    public MethodNode doCall(ClassNode classNode, GroovyParser.MethodDeclarationContext ctx, String methodName, int modifiers, ClassNode returnType, Parameter[] params, ClassNode[] exceptions, Statement statement) {

                        final MethodNode methodNode = new MethodNode(methodName, modifiers, returnType, params, exceptions, statement);
                        methodNode.setGenericsTypes(parseGenericDeclaration(ctx.genericDeclarationList()));
                        methodNode.setAnnotationDefault(true);

                        return methodNode;
                    }
                }
        );
    }

    public ClassNode parseClassDeclaration(final GroovyParser.ClassDeclarationContext ctx) {
        boolean isEnum = asBoolean(ctx.KW_ENUM());

        final ClassNode outerClass = asBoolean(classes) ? classes.peek() : null;
        ClassNode[] interfaces = asBoolean(ctx.implementsClause())
                ? DefaultGroovyMethods.asType(collect(ctx.implementsClause().genericClassNameExpression(), new Closure<ClassNode>(this, this) {
                                                                                                                public ClassNode doCall(GroovyParser.GenericClassNameExpressionContext it) {
                                                                                                                    return parseExpression(it);
                                                                                                                }
                                                                                                            }),
                                                ClassNode[].class)
                : new ClassNode[0];

        ClassNode classNode;
        String packageName = moduleNode.getPackageName();
        packageName = packageName != null && asBoolean(packageName) ? packageName : "";

        if (isEnum) {
            classNode = EnumHelper.makeEnumNode(asBoolean(outerClass) ? ctx.IDENTIFIER().getText() : packageName + ctx.IDENTIFIER().getText(), Modifier.PUBLIC, interfaces, outerClass);
        } else {
            if (outerClass != null) {
                String name = outerClass.getName() + "$" + String.valueOf(ctx.IDENTIFIER());
                classNode = new InnerClassNode(outerClass, name, Modifier.PUBLIC, ClassHelper.OBJECT_TYPE);
            } else {
                classNode = new ClassNode(packageName + String.valueOf(ctx.IDENTIFIER()), Modifier.PUBLIC, ClassHelper.OBJECT_TYPE);
            }
        }

        setupNodeLocation(classNode, ctx);
        attachAnnotations(classNode, ctx.annotationClause());

        if (asBoolean(ctx.KW_TRAIT())) {
            attachTraitTransformAnnotation(classNode);
        }

        moduleNode.addClass(classNode);
        if (asBoolean(ctx.extendsClause())) {
            if (asBoolean(ctx.KW_INTERFACE()) && !asBoolean(ctx.AT())) { // interface(NOT annotation)
                List<ClassNode> interfaceList = new LinkedList<ClassNode>();
                for (GroovyParser.GenericClassNameExpressionContext genericClassNameExpressionContext : ctx.extendsClause().genericClassNameExpression()) {
                    interfaceList.add(parseExpression(genericClassNameExpressionContext));
                }
                (classNode).setInterfaces(interfaceList.toArray(new ClassNode[0]));
                (classNode).setSuperClass(ClassHelper.OBJECT_TYPE);
            } else {
                (classNode).setSuperClass(parseExpression(ctx.extendsClause().genericClassNameExpression(0)));
            }

        }

        if (asBoolean(ctx.implementsClause())) {
            (classNode).setInterfaces(interfaces);
        }


        if (!isEnum) {
            (classNode).setGenericsTypes(parseGenericDeclaration(ctx.genericDeclarationList()));
            (classNode).setUsingGenerics((classNode.getGenericsTypes() != null && classNode.getGenericsTypes().length != 0) || (classNode).getSuperClass().isUsingGenerics() || DefaultGroovyMethods.any(classNode.getInterfaces(), new Closure<Boolean>(this, this) {
                public Boolean doCall(ClassNode it) {return it.isUsingGenerics();}
            }));
        }


        classNode.setModifiers(parseClassModifiers(ctx.classModifier()) |
                (isEnum ? (Opcodes.ACC_ENUM | Opcodes.ACC_FINAL)
                        : ((asBoolean(ctx.KW_INTERFACE())
                        ? Opcodes.ACC_INTERFACE | Opcodes.ACC_ABSTRACT
                        : 0)
                )
                )
        );

        classNode.setSyntheticPublic((classNode.getModifiers() & Opcodes.ACC_SYNTHETIC) != 0);
        classNode.setModifiers(classNode.getModifiers() & ~Opcodes.ACC_SYNTHETIC);// FIXME Magic with synthetic modifier.


        if (asBoolean(ctx.AT())) {
            classNode.addInterface(ClassHelper.Annotation_TYPE);
            classNode.setModifiers(classNode.getModifiers() | Opcodes.ACC_ANNOTATION);
        }


        classes.add(classNode);
        parseClassBody(classNode, ctx.classBody());
        classes.pop();

        if (classNode.isInterface()) { // FIXME why interface has null mixin
            try {
                // FIXME Hack with visibility.
                Field field = classNode.getClass().getDeclaredField("mixins");
                field.setAccessible(true);
                field.set(classNode, null);
            } catch (IllegalAccessException e) {
                log.warning(createExceptionMessage(e));
            } catch (NoSuchFieldException e) {
                log.warning(createExceptionMessage(e));
            }
        }
        return classNode;
    }

    private Expression createEnumConstantInitExpression(GroovyParser.ArgumentListContext ctx) {
        if (!asBoolean(ctx)) {
            return null;
        }

        TupleExpression argumentListExpression = (TupleExpression) createArgumentList(ctx);
        List<Expression> expressions = argumentListExpression.getExpressions();

        if (expressions.size() == 1) {
            return expressions.get(0);
        }

        ListExpression listExpression = new ListExpression(expressions);
        listExpression.setWrapped(true);

        return listExpression;
    }

    public void parseClassBody(ClassNode classNode, GroovyParser.ClassBodyContext ctx) {
        for(GroovyParser.EnumConstantContext node : ctx.enumConstant()) {

            setupNodeLocation(EnumHelper.addEnumConstant(classNode, node.IDENTIFIER().getText(), createEnumConstantInitExpression(node.argumentList())), node.IDENTIFIER().getSymbol());
        }

        parseMembers(classNode, ctx.classMember());
    }

    public void parseMembers(ClassNode classNode, List<GroovyParser.ClassMemberContext> ctx) {
        for (GroovyParser.ClassMemberContext member : ctx) {
            ParseTree memberContext = DefaultGroovyMethods.last(member.children);

            ASTNode memberNode = null;
            if (memberContext instanceof GroovyParser.ClassDeclarationContext)
                memberNode = parseClassDeclaration(DefaultGroovyMethods.asType(memberContext, GroovyParser.ClassDeclarationContext.class));
            else if (memberContext instanceof GroovyParser.ConstructorDeclarationContext)
                memberNode = parseMember(classNode, (GroovyParser.ConstructorDeclarationContext)memberContext);
            else if (memberContext instanceof GroovyParser.MethodDeclarationContext)
                memberNode = parseMember(classNode, (GroovyParser.MethodDeclarationContext)memberContext);
            else if (memberContext instanceof GroovyParser.FieldDeclarationContext)
                memberNode = parseMember(classNode, (GroovyParser.FieldDeclarationContext)memberContext);
            else if (memberContext instanceof GroovyParser.ObjectInitializerContext)
                parseMember(classNode, (GroovyParser.ObjectInitializerContext)memberContext);
            else if (memberContext instanceof GroovyParser.ClassInitializerContext)
                parseMember(classNode, (GroovyParser.ClassInitializerContext)memberContext);
            else
                assert false : "Unknown class member type.";
            if (asBoolean(memberNode)) setupNodeLocation(memberNode, member);
            if (member.getChildCount() > 1) {
                assert memberNode != null;
                for (int i = 0; i < member.children.size() - 2; i++) {
                    ParseTree annotationCtx = member.children.get(i);
                    assert annotationCtx instanceof GroovyParser.AnnotationClauseContext;
                    ((AnnotatedNode)memberNode).addAnnotation(parseAnnotation((GroovyParser.AnnotationClauseContext)annotationCtx));
                }

            }

        }

    }

    private boolean isTrait(ClassNode classNode) {
        return classNode.getAnnotations(ClassHelper.make(GROOVY_TRANSFORM_TRAIT)).size() > 0;
    }


    public AnnotatedNode parseMember(ClassNode classNode, GroovyParser.MethodDeclarationContext ctx) {
        if (isTrait(classNode)) {
            if (null == ctx.methodBody() && !ctx.modifierAndDefSet.contains(KW_ABSTRACT_STR)) {
                throw createParsingFailedException(new InvalidSyntaxException("You defined a method without body. Try adding a body, or declare it abstract.", ctx));
            }
        }

        return parseMethodDeclaration(classNode, ctx, new Closure<MethodNode>(this, this) {
                    public MethodNode doCall(ClassNode classNode, GroovyParser.MethodDeclarationContext ctx, String methodName, int modifiers, ClassNode returnType, Parameter[] params, ClassNode[] exceptions, Statement statement) {
                        modifiers |= classNode.isInterface() ? Opcodes.ACC_ABSTRACT : 0;

                        if (ctx.KW_DEFAULT() != null) {
                            statement = new ExpressionStatement(parseExpression(ctx.annotationParameter()));
                        }

                        final MethodNode methodNode = classNode.addMethod(methodName, modifiers, returnType, params, exceptions, statement);
                        methodNode.setGenericsTypes(parseGenericDeclaration(ctx.genericDeclarationList()));


                        if (ctx.KW_DEFAULT() != null) {
                            methodNode.setAnnotationDefault(true);
                        }


                        return methodNode;
                    }
                }
        );
    }

    public AnnotatedNode parseMember(ClassNode classNode, GroovyParser.FieldDeclarationContext ctx) {
        //noinspection GroovyAssignabilityCheck
        final Iterator<Object> iterator = parseModifiers(ctx.memberModifier()).iterator();
        int modifiers = ((Integer)(iterator.hasNext() ? iterator.next() : null));
        boolean hasVisibilityModifier = ((Boolean)(iterator.hasNext() ? iterator.next() : null));

        modifiers |= classNode.isInterface() ? Opcodes.ACC_STATIC | Opcodes.ACC_FINAL : 0;


        AnnotatedNode node = null;
        List<GroovyParser.SingleDeclarationContext> variables = ctx.singleDeclaration();
        for (GroovyParser.SingleDeclarationContext variableCtx : variables) {
            GroovyParser.ExpressionContext initExprContext = variableCtx.expression();
            Expression initialierExpression = asBoolean(initExprContext)
                    ? parseExpression(initExprContext)
                    : null;
            ClassNode typeDeclaration = asBoolean(ctx.genericClassNameExpression())
                    ? parseExpression(ctx.genericClassNameExpression())
                    : ClassHelper.OBJECT_TYPE;
            Expression initialValue = classNode.isInterface() && !typeDeclaration.equals(ClassHelper.OBJECT_TYPE)
                    ? new ConstantExpression(initialExpressionForType(typeDeclaration))
                    : initialierExpression;
            if (classNode.isInterface() || hasVisibilityModifier) {
                modifiers |= classNode.isInterface() ? Opcodes.ACC_PUBLIC : 0;

                FieldNode field = classNode.addField(variableCtx.IDENTIFIER().getText(), modifiers, typeDeclaration, initialValue);
                attachAnnotations(field, ctx.annotationClause());
                node = setupNodeLocation(field, variables.size() == 1 ? ctx : variableCtx);
            } else {// no visibility specified. Generate property node.
                Integer propertyModifier = modifiers | Opcodes.ACC_PUBLIC;
                PropertyNode propertyNode = classNode.addProperty(variableCtx.IDENTIFIER().getText(), propertyModifier, typeDeclaration, initialValue, null, null);
                propertyNode.getField().setModifiers(modifiers | Opcodes.ACC_PRIVATE);
                propertyNode.getField().setSynthetic(!classNode.isInterface());
                node = setupNodeLocation(propertyNode.getField(), variables.size() == 1 ? ctx : variableCtx);
                attachAnnotations(propertyNode.getField(), ctx.annotationClause());
                setupNodeLocation(propertyNode, variables.size() == 1 ? ctx : variableCtx);
            }
        }
        return node;
    }

    public void parseMember(ClassNode classNode, GroovyParser.ClassInitializerContext ctx) {
        unpackStatement((BlockStatement)getOrCreateClinitMethod(classNode).getCode(), parseStatement(ctx.blockStatement()));
    }

    public void parseMember(ClassNode classNode, GroovyParser.ObjectInitializerContext ctx) {
        BlockStatement statement = new BlockStatement();
        unpackStatement(statement, parseStatement(ctx.blockStatement()));
        classNode.addObjectInitializerStatements(statement);
    }

    public AnnotatedNode parseMember(ClassNode classNode, GroovyParser.ConstructorDeclarationContext ctx) {
        int modifiers = asBoolean(ctx.VISIBILITY_MODIFIER())
                ? parseVisibilityModifiers(ctx.VISIBILITY_MODIFIER())
                : Opcodes.ACC_PUBLIC;

        ClassNode[] exceptions = parseThrowsClause(ctx.throwsClause());
        this.innerClassesDefinedInMethod.add(new ArrayList<InnerClassNode>());
        final ConstructorNode constructorNode = classNode.addConstructor(modifiers, parseParameters(ctx.argumentDeclarationList()), exceptions, parseStatement(DefaultGroovyMethods.asType(ctx.blockStatement(), GroovyParser.BlockStatementContext.class)));
        DefaultGroovyMethods.each(this.innerClassesDefinedInMethod.pop(), new Closure<ConstructorNode>(null, null) {
            public ConstructorNode doCall(InnerClassNode it) {
                it.setEnclosingMethod(constructorNode);
                return constructorNode;
            }
        });
        setupNodeLocation(constructorNode, ctx);
        constructorNode.setSyntheticPublic(ctx.VISIBILITY_MODIFIER() == null);
        return constructorNode;
    }

    private static class DeclarationList extends Statement{
        List<DeclarationExpression> declarations;

        DeclarationList(List<DeclarationExpression> declarations) {
            this.declarations = declarations;
        }
    }

    public Statement parseStatement(GroovyParser.StatementContext ctx) {
        if (ctx instanceof GroovyParser.IfStatementContext)
            return parseStatement((GroovyParser.IfStatementContext)ctx);
        if (ctx instanceof GroovyParser.NewArrayStatementContext)
            return parseStatement((GroovyParser.NewArrayStatementContext)ctx);
        if (ctx instanceof GroovyParser.TryCatchFinallyStatementContext)
            return parseStatement((GroovyParser.TryCatchFinallyStatementContext)ctx);
        if (ctx instanceof GroovyParser.ThrowStatementContext)
            return parseStatement((GroovyParser.ThrowStatementContext)ctx);
        if (ctx instanceof GroovyParser.ClassicForStatementContext)
            return parseStatement((GroovyParser.ClassicForStatementContext)ctx);
        if (ctx instanceof GroovyParser.DeclarationStatementContext)
            return parseStatement((GroovyParser.DeclarationStatementContext)ctx);
        if (ctx instanceof GroovyParser.ReturnStatementContext)
            return parseStatement((GroovyParser.ReturnStatementContext)ctx);
        if (ctx instanceof GroovyParser.ExpressionStatementContext)
            return parseStatement((GroovyParser.ExpressionStatementContext)ctx);
        if (ctx instanceof GroovyParser.ForInStatementContext)
            return parseStatement((GroovyParser.ForInStatementContext)ctx);
        if (ctx instanceof GroovyParser.ForColonStatementContext)
            return parseStatement((GroovyParser.ForColonStatementContext)ctx);
        if (ctx instanceof GroovyParser.SwitchStatementContext)
            return parseStatement((GroovyParser.SwitchStatementContext)ctx);
        if (ctx instanceof GroovyParser.WhileStatementContext)
            return parseStatement((GroovyParser.WhileStatementContext)ctx);
        if (ctx instanceof GroovyParser.ControlStatementContext)
            return parseStatement((GroovyParser.ControlStatementContext)ctx);
        if (ctx instanceof GroovyParser.CommandExpressionStatementContext)
            return parseStatement((GroovyParser.CommandExpressionStatementContext)ctx);
        if (ctx instanceof GroovyParser.NewInstanceStatementContext)
            return parseStatement((GroovyParser.NewInstanceStatementContext)ctx);
        if (ctx instanceof GroovyParser.AssertStatementContext)
            return parseStatement((GroovyParser.AssertStatementContext)ctx);
        if (ctx instanceof GroovyParser.LabeledStatementContext)
            return parseStatement((GroovyParser.LabeledStatementContext)ctx);
        if (ctx instanceof GroovyParser.SynchronizedStatementContext)
            return parseStatement((GroovyParser.SynchronizedStatementContext)ctx);

        throw createParsingFailedException(new InvalidSyntaxException("Unsupported statement type! " + ctx.getText(), ctx));
    }

    public Statement parseStatement(GroovyParser.BlockStatementContext ctx) {
        final BlockStatement statement = new BlockStatement();
        if (!asBoolean(ctx)) return statement;

        DefaultGroovyMethods.each(ctx.statement(), new Closure<Object>(null, null) {
            public void doCall(GroovyParser.StatementContext it) {
                unpackStatement(statement, parseStatement(it));
            }
        });
        return setupNodeLocation(statement, ctx);
    }

    public Statement parseStatement(GroovyParser.ExpressionStatementContext ctx) {
        return setupNodeLocation(new ExpressionStatement(parseExpression(ctx.expression())), ctx);
    }

    public Statement parseStatement(GroovyParser.IfStatementContext ctx) {
        Statement trueBranch = parse(ctx.statementBlock(0));
        Statement falseBranch = asBoolean(ctx.KW_ELSE())
                ? parse(ctx.statementBlock(1))
                : EmptyStatement.INSTANCE;
        BooleanExpression expression = new BooleanExpression(parseExpression(ctx.expression()));
        return setupNodeLocation(new IfStatement(expression, trueBranch, falseBranch), ctx);
    }

    public Statement parseStatement(GroovyParser.WhileStatementContext ctx) {
        return setupNodeLocation(new WhileStatement(new BooleanExpression(parseExpression(ctx.expression())), parse(ctx.statementBlock())), ctx);
    }

    public Expression parseExpression(GroovyParser.DeclarationRuleContext ctx) {
        List<?> declarations = parseDeclaration(ctx);

        if (declarations.size() == 1) {
            return setupNodeLocation((Expression) declarations.get(0), ctx);
        } else {
            return new ClosureListExpression((List<Expression>)declarations);
        }
    }

    public Statement parseStatement(GroovyParser.ClassicForStatementContext ctx) {
        ClosureListExpression expression = new ClosureListExpression();

        Boolean captureNext = false;
        for (ParseTree c : ctx.children) {
            // FIXME terrible logic.
            Boolean isSemicolon = c instanceof TerminalNode && (((TerminalNode)c).getSymbol().getText().equals(";") || ((TerminalNode)c).getSymbol().getText().equals("(") || ((TerminalNode)c).getSymbol().getText().equals(")"));

            if (captureNext) {
                if (isSemicolon) {
                    expression.addExpression(EmptyExpression.INSTANCE);
                } else if (c instanceof GroovyParser.ExpressionContext) {
                    expression.addExpression(parseExpression((GroovyParser.ExpressionContext)c));
                } else if (c instanceof GroovyParser.DeclarationRuleContext) {
                    expression.addExpression(parseExpression((GroovyParser.DeclarationRuleContext) c));
                }
            }

            captureNext = isSemicolon;
        }

        Parameter parameter = ForStatement.FOR_LOOP_DUMMY;
        return setupNodeLocation(new ForStatement(parameter, expression, parse(ctx.statementBlock())), ctx);
    }

    public Statement parseStatement(GroovyParser.ForInStatementContext ctx) {
        Parameter parameter = new Parameter(parseTypeDeclaration(ctx.typeDeclaration()), ctx.IDENTIFIER().getText());
        parameter = setupNodeLocation(parameter, ctx.IDENTIFIER().getSymbol());

        return setupNodeLocation(new ForStatement(parameter, parseExpression(ctx.expression()), parse(ctx.statementBlock())), ctx);
    }

    public Statement parseStatement(GroovyParser.ForColonStatementContext ctx) {
        if (!asBoolean(ctx.typeDeclaration()))
            throw createParsingFailedException(new InvalidSyntaxException("Classic for statement require type to be declared.", ctx));

        Parameter parameter = new Parameter(parseTypeDeclaration(ctx.typeDeclaration()), ctx.IDENTIFIER().getText());
        parameter = setupNodeLocation(parameter, ctx.IDENTIFIER().getSymbol());

        return setupNodeLocation(new ForStatement(parameter, parseExpression(ctx.expression()), parse(ctx.statementBlock())), ctx);
    }

    public Statement parse(GroovyParser.StatementBlockContext ctx) {
        if (asBoolean(ctx.statement()))
            return setupNodeLocation(parseStatement(ctx.statement()), ctx.statement());
        else return parseStatement(ctx.blockStatement());
    }

    public Statement parseStatement(GroovyParser.SwitchStatementContext ctx) {
        List<CaseStatement> caseStatements = new ArrayList<CaseStatement>();
        for (GroovyParser.CaseStatementContext caseStmt : ctx.caseStatement()) {

            BlockStatement stmt =  new BlockStatement();// #BSC
            for (GroovyParser.StatementContext st : caseStmt.statement()) {
                unpackStatement (stmt, parseStatement(st));
            }

            caseStatements.add(setupNodeLocation(new CaseStatement(parseExpression(caseStmt.expression()), asBoolean(stmt.getStatements()) ? stmt : EmptyStatement.INSTANCE), caseStmt.KW_CASE().getSymbol()));// There only 'case' kw was highlighted in parser old version.
        }

        Statement defaultStatement;
        if (asBoolean(ctx.KW_DEFAULT())) {

            defaultStatement = new BlockStatement();// #BSC
            for (GroovyParser.StatementContext stmt : ctx.statement())
                unpackStatement((BlockStatement)defaultStatement,parseStatement(stmt));
        } else
            defaultStatement = EmptyStatement.INSTANCE;// TODO Refactor empty stataements and expressions.

        return new SwitchStatement(parseExpression(ctx.expression()), caseStatements, defaultStatement);
    }

    public Statement parseStatement(GroovyParser.DeclarationStatementContext ctx) {
        List<DeclarationExpression> declarations = parseDeclaration(ctx.declarationRule());
        return setupNodeLocation(new DeclarationList(declarations), ctx);
    }

    public Statement parseStatement(GroovyParser.NewArrayStatementContext ctx) {
        return setupNodeLocation(new ExpressionStatement(parse(ctx.newArrayRule())), ctx);
    }

    public Statement parseStatement(GroovyParser.NewInstanceStatementContext ctx) {
        return setupNodeLocation(new ExpressionStatement(parse(ctx.newInstanceRule())), ctx);
    }

    public Statement parseStatement(GroovyParser.ControlStatementContext ctx) {
        // TODO check validity. Labeling support.
        // Fake inspection result should be suppressed.
        //noinspection GroovyConditionalWithIdenticalBranches
        return setupNodeLocation(asBoolean(ctx.KW_BREAK())
                ? new BreakStatement()
                : new ContinueStatement(), ctx);
    }

    public Statement parseStatement(GroovyParser.ReturnStatementContext ctx) {
        GroovyParser.ExpressionContext expression = ctx.expression();

        return setupNodeLocation(new ReturnStatement(asBoolean(expression)
                ? parseExpression(expression)
                : new ConstantExpression(null)), ctx);
    }


    public Statement parseStatement(GroovyParser.AssertStatementContext ctx) {
        Expression conditionExpression = parseExpression(ctx.expression(0));
        BooleanExpression booleanConditionExpression = new BooleanExpression(conditionExpression);

        if (ctx.expression().size() == 1) {
            return setupNodeLocation(new AssertStatement(booleanConditionExpression), ctx);
        } else {
            Expression errorMessage = parseExpression(ctx.expression(1));
            return setupNodeLocation(new AssertStatement(booleanConditionExpression, errorMessage), ctx);
        }
    }

    public Statement parseStatement(GroovyParser.LabeledStatementContext ctx) {
        Statement statement = parse(ctx.statementBlock());

        statement.setStatementLabel(ctx.IDENTIFIER().getText());

        return setupNodeLocation(statement, ctx);
    }

    public Statement parseStatement(GroovyParser.SynchronizedStatementContext ctx) {
        Expression expression = parseExpression(ctx.expression());
        Statement statementBlock = parse(ctx.statementBlock());

        return setupNodeLocation(new SynchronizedStatement(expression, statementBlock), ctx);
    }

    public Statement parseStatement(GroovyParser.ThrowStatementContext ctx) {
        return setupNodeLocation(new ThrowStatement(parseExpression(ctx.expression())), ctx);
    }

    public Statement parseStatement(GroovyParser.TryCatchFinallyStatementContext ctx) {
        Object finallyStatement;

        GroovyParser.BlockStatementContext finallyBlockStatement = ctx.finallyBlock() != null ? ctx.finallyBlock().blockStatement() : null;
        if (finallyBlockStatement != null) {
            BlockStatement fbs = new BlockStatement();
            unpackStatement(fbs, parseStatement(finallyBlockStatement));
            finallyStatement = setupNodeLocation(fbs, finallyBlockStatement);

        } else finallyStatement = EmptyStatement.INSTANCE;

        final TryCatchStatement statement = new TryCatchStatement(parseStatement(DefaultGroovyMethods.asType(ctx.tryBlock().blockStatement(), GroovyParser.BlockStatementContext.class)), (Statement)finallyStatement);
        DefaultGroovyMethods.each(ctx.catchBlock(), new Closure<List<GroovyParser.ClassNameExpressionContext>>(null, null) {
            public List<GroovyParser.ClassNameExpressionContext> doCall(GroovyParser.CatchBlockContext it) {
                final Statement catchBlock = parseStatement(DefaultGroovyMethods.asType(it.blockStatement(), GroovyParser.BlockStatementContext.class));
                final String var = it.IDENTIFIER().getText();

                List<GroovyParser.ClassNameExpressionContext> classNameExpression = it.classNameExpression();
                if (!asBoolean(classNameExpression))
                    statement.addCatch(setupNodeLocation(new CatchStatement(new Parameter(ClassHelper.OBJECT_TYPE, var), catchBlock), it));
                else {
                    DefaultGroovyMethods.each(classNameExpression, new Closure<Object>(null, null) {
                        public void doCall(GroovyParser.ClassNameExpressionContext it) {
                            statement.addCatch(setupNodeLocation(new CatchStatement(new Parameter(parseExpression(DefaultGroovyMethods.asType(it, GroovyParser.ClassNameExpressionContext.class)), var), catchBlock), it));
                        }
                    });
                }
                return null;
            }
        });
        return statement;
    }

    public Statement parseStatement(GroovyParser.CommandExpressionStatementContext ctx) {
        GroovyParser.CmdExpressionRuleContext cmdExpressionRuleContext = ctx.cmdExpressionRule();

        boolean hasExpression = asBoolean(cmdExpressionRuleContext.expression());
        boolean hasPropertyAccess = asBoolean(cmdExpressionRuleContext.prop);
        List<ParseTree> children = cmdExpressionRuleContext.children;
        int childrenSize = children.size();

        int firstIdentifierIndex = -1;
        ParseTree firstIdentifier = null;
        for (int i = 0; i < childrenSize; i++) {
            ParseTree t = children.get(i);
            if (t instanceof TerminalNode
                    && ((TerminalNode) t).getSymbol().getType() == GroovyParser.IDENTIFIER) {

                firstIdentifierIndex = i;
                firstIdentifier = t;
                break;
            }
        }

        Expression expression = hasExpression ? parseExpression(cmdExpressionRuleContext.expression()) : VariableExpression.THIS_EXPRESSION;
        List<List<ParseTree>> nameAndArgumentPairList = DefaultGroovyMethods.collate(
                                                            children.subList(firstIdentifierIndex, hasPropertyAccess ? childrenSize - 1 : childrenSize),
                                                            2
                                                        );

        for (List<ParseTree> nameAndArgumentPair : nameAndArgumentPairList) {
            ParseTree nameNode = nameAndArgumentPair.get(0);
            ParseTree argumentListNode = nameAndArgumentPair.get(1);

            expression = new MethodCallExpression(expression, ((TerminalNode)nameNode).getText(), createArgumentList((GroovyParser.ArgumentListContext)argumentListNode));

            if (nameNode == firstIdentifier) {
                ((MethodCallExpression)expression).setImplicitThis(!hasExpression);
            } else {
                ((MethodCallExpression)expression).setImplicitThis(false);
            }

            if (hasExpression) {
                Token op = cmdExpressionRuleContext.op;
                ((MethodCallExpression)expression).setSpreadSafe(op.getType() == GroovyParser.STAR_DOT);
                ((MethodCallExpression)expression).setSafe(op.getType() == GroovyParser.SAFE_DOT);
            }
        }

        if (hasPropertyAccess) {
            expression = new PropertyExpression(expression, cmdExpressionRuleContext.prop.getText());
        }

        return setupNodeLocation(new ExpressionStatement(expression), ctx);
    }

    /**
     * Parse path expression.
     *
     * @param ctx
     * @return tuple of 3 values: Expression, String methodName and boolean implicitThis flag.
     */
    public ArrayList<Object> parsePathExpression(GroovyParser.PathExpressionContext ctx) {
        Expression expression;
        List<TerminalNode> identifiers = ctx.IDENTIFIER();
        switch (identifiers.size()) {
            case 1:
                expression = VariableExpression.THIS_EXPRESSION;
                break;
            case 2:
                expression = new VariableExpression(identifiers.get(0).getText());
                break;
            default:
                expression = DefaultGroovyMethods.inject(identifiers.subList(1, identifiers.size() - 1), new VariableExpression(identifiers.get(0).getText()), new Closure<PropertyExpression>(null, null) {
                    public PropertyExpression doCall(Expression expr, Object prop) {
                        return new PropertyExpression(expr, ((TerminalNode)prop).getText());
                    }

                });
                log.info(expression.getText());
                break;
        }
        return new ArrayList<Object>(Arrays.asList(expression, DefaultGroovyMethods.last(identifiers).getSymbol().getText(), identifiers.size() == 1));
    }

    public Expression parseExpression(GroovyParser.ExpressionContext ctx) {
        if (ctx instanceof GroovyParser.ParenthesisExpressionContext)
            return parseExpression((GroovyParser.ParenthesisExpressionContext)ctx);
        else if (ctx instanceof GroovyParser.ConstantIntegerExpressionContext)
            return parseExpression((GroovyParser.ConstantIntegerExpressionContext)ctx);
        else if (ctx instanceof GroovyParser.PostfixExpressionContext)
            return parseExpression((GroovyParser.PostfixExpressionContext)ctx);
        else if (ctx instanceof GroovyParser.ClosureExpressionContext)
            return parseExpression((GroovyParser.ClosureExpressionContext)ctx);
        else if (ctx instanceof GroovyParser.AssignmentExpressionContext)
            return parseExpression((GroovyParser.AssignmentExpressionContext)ctx);
        else if (ctx instanceof GroovyParser.ConstantDecimalExpressionContext)
            return parseExpression((GroovyParser.ConstantDecimalExpressionContext)ctx);
        else if (ctx instanceof GroovyParser.TernaryExpressionContext)
            return parseExpression((GroovyParser.TernaryExpressionContext)ctx);
        else if (ctx instanceof GroovyParser.CallExpressionContext)
            return parseExpression((GroovyParser.CallExpressionContext)ctx);
        else if (ctx instanceof GroovyParser.CastExpressionContext)
            return parseExpression((GroovyParser.CastExpressionContext)ctx);
        else if (ctx instanceof GroovyParser.ElvisExpressionContext)
            return parseExpression((GroovyParser.ElvisExpressionContext)ctx);
        else if (ctx instanceof GroovyParser.BinaryExpressionContext)
            return parseExpression((GroovyParser.BinaryExpressionContext)ctx);
        else if (ctx instanceof GroovyParser.NullExpressionContext)
            return parseExpression((GroovyParser.NullExpressionContext)ctx);
        else if (ctx instanceof GroovyParser.ListConstructorContext)
            return parseExpression((GroovyParser.ListConstructorContext)ctx);
        else if (ctx instanceof GroovyParser.PrefixExpressionContext)
            return parseExpression((GroovyParser.PrefixExpressionContext)ctx);
        else if (ctx instanceof GroovyParser.ConstantExpressionContext)
            return parseExpression((GroovyParser.ConstantExpressionContext)ctx);
        else if (ctx instanceof GroovyParser.NewArrayExpressionContext)
            return parseExpression((GroovyParser.NewArrayExpressionContext)ctx);
        else if (ctx instanceof GroovyParser.FieldAccessExpressionContext)
            return parseExpression((GroovyParser.FieldAccessExpressionContext)ctx);
        else if (ctx instanceof GroovyParser.VariableExpressionContext)
            return parseExpression((GroovyParser.VariableExpressionContext)ctx);
        else if (ctx instanceof GroovyParser.NewInstanceExpressionContext)
            return parseExpression((GroovyParser.NewInstanceExpressionContext)ctx);
        else if (ctx instanceof GroovyParser.BoolExpressionContext)
            return parseExpression((GroovyParser.BoolExpressionContext)ctx);
        else if (ctx instanceof GroovyParser.ConstructorCallExpressionContext)
            return parseExpression((GroovyParser.ConstructorCallExpressionContext)ctx);
        else if (ctx instanceof GroovyParser.UnaryExpressionContext)
            return parseExpression((GroovyParser.UnaryExpressionContext)ctx);
        else if (ctx instanceof GroovyParser.MapConstructorContext)
            return parseExpression((GroovyParser.MapConstructorContext)ctx);
        else if (ctx instanceof GroovyParser.GstringExpressionContext)
            return parseExpression((GroovyParser.GstringExpressionContext)ctx);
        if (ctx instanceof GroovyParser.IndexExpressionContext)
            return parseExpression((GroovyParser.IndexExpressionContext)ctx);
        if (ctx instanceof GroovyParser.SpreadExpressionContext)
            return parseExpression((GroovyParser.SpreadExpressionContext)ctx);
        if (ctx instanceof GroovyParser.ThisExpressionContext)
            return parseExpression((GroovyParser.ThisExpressionContext)ctx);
        if (ctx instanceof GroovyParser.SuperExpressionContext)
            return parseExpression((GroovyParser.SuperExpressionContext)ctx);

        throw createParsingFailedException(new InvalidSyntaxException("Unsupported expression type! " + String.valueOf(ctx), ctx));
    }

    public Expression parseExpression(GroovyParser.NewArrayExpressionContext ctx) {
        return parse(ctx.newArrayRule());
    }

    public Expression parseExpression(GroovyParser.NewInstanceExpressionContext ctx) {
        return parse(ctx.newInstanceRule());
    }

    public Expression parseExpression(GroovyParser.ParenthesisExpressionContext ctx) {
        return parseExpression(ctx.expression());
    }

    public Expression parseExpression(GroovyParser.ListConstructorContext ctx) {
        ListExpression expression = new ListExpression(collect(ctx.expression(), new MethodClosure(this, "parseExpression")));
        return setupNodeLocation(expression, ctx);
    }

    public Expression parseExpression(GroovyParser.MapConstructorContext ctx) {
        final List collect = collect(ctx.mapEntry(), new MethodClosure(this, "parseExpression"));
        return setupNodeLocation(new MapExpression(asBoolean(collect)
                ? collect
                : new ArrayList()), ctx);
    }

    public MapEntryExpression parseExpression(GroovyParser.MapEntryContext ctx) {
        Expression keyExpr;
        Expression valueExpr;
        List<GroovyParser.ExpressionContext> expressions = ctx.expression();
        if (expressions.size() == 1) {
            valueExpr = parseExpression(expressions.get(0));
            if (asBoolean(ctx.MULT())) {
                // This is really a spread map entry.
                // This is an odd construct, SpreadMapExpression does not extend MapExpression, so we workaround
                keyExpr = setupNodeLocation(new SpreadMapExpression(valueExpr), ctx);
            } else {
                if (asBoolean(ctx.STRING())) {
                    keyExpr = new ConstantExpression(parseString(ctx.STRING()));
                } else if (asBoolean(ctx.selectorName())) {
                    keyExpr = new ConstantExpression(ctx.selectorName().getText());
                } else if (asBoolean(ctx.gstring())) {
                    keyExpr = parseExpression(ctx.gstring());
                } else if (asBoolean(ctx.INTEGER())) {
                    keyExpr = parseInteger(ctx.INTEGER().getText(), ctx);
                } else if (asBoolean(ctx.DECIMAL())) {
                    keyExpr = parseDecimal(ctx.DECIMAL().getText(), ctx);
                } else {
                    throw createParsingFailedException(new InvalidSyntaxException("Unsupported map key type! " + String.valueOf(ctx), ctx));
                }
            }
        } else {
            keyExpr = parseExpression(expressions.get(0));
            valueExpr = parseExpression(expressions.get(1));
        }

        return setupNodeLocation(new MapEntryExpression(keyExpr, valueExpr), ctx);
    }

    public Expression parseExpression(GroovyParser.ClosureExpressionContext ctx) {
        return parseExpression(ctx.closureExpressionRule());
    }

    public Expression parseExpression(GroovyParser.ClosureExpressionRuleContext ctx) {
        final Parameter[] parameters1 = parseParameters(ctx.argumentDeclarationList());
        Parameter[] parameters = asBoolean(ctx.argumentDeclarationList()) ? (
                asBoolean(parameters1)
                        ? parameters1
                        : null) : (new Parameter[0]);

        Statement statement = parseStatement(DefaultGroovyMethods.asType(ctx.blockStatement(), GroovyParser.BlockStatementContext.class));
        return setupNodeLocation(new ClosureExpression(parameters, statement), ctx);
    }

    public Expression parseExpression(GroovyParser.BinaryExpressionContext ctx) {
        int i = 1;

        // ignore newlines
        for (ParseTree t = ctx.getChild(i); t instanceof TerminalNode && ((TerminalNode) t).getSymbol().getType() == GroovyParser.NL; t = ctx.getChild(i)) {
            i++;
        }

        TerminalNode c = DefaultGroovyMethods.asType(ctx.getChild(i), TerminalNode.class);

        for (ParseTree next = ctx.getChild(i + 1); next instanceof TerminalNode && ((TerminalNode)next).getSymbol().getType() == GroovyParser.GT; next = ctx.getChild(i + 1)) {
            i++;
        }

        org.codehaus.groovy.syntax.Token op = createToken(c, c.getSymbol().getType() == GroovyParser.GT ? i : 1);

        Object expression;
        Expression left = parseExpression(ctx.expression(0));
        Expression right = null;// Will be initialized later, in switch. We should handle as and instanceof creating
        // ClassExpression for given IDENTIFIERS. So, switch should fall through.
        //noinspection GroovyFallthrough
        switch (op.getType()) {
            case Types.RANGE_OPERATOR:
                right = parseExpression(ctx.expression(1));
                expression = new RangeExpression(left, right, !op.getText().endsWith("<"));
                break;
            case Types.KEYWORD_AS:
                ClassNode classNode = setupNodeLocation(parseExpression(ctx.genericClassNameExpression()), ctx.genericClassNameExpression());
                expression = CastExpression.asExpression(classNode, left);
                break;
            case Types.KEYWORD_INSTANCEOF:
                ClassNode rhClass = setupNodeLocation(parseExpression(ctx.genericClassNameExpression()), ctx.genericClassNameExpression());
                right = new ClassExpression(rhClass);
            default:
                if (!asBoolean(right)) right = parseExpression(ctx.expression(1));
                expression = new BinaryExpression(left, op, right);
                break;
        }

        ((Expression)expression).setColumnNumber(op.getStartColumn());
        ((Expression)expression).setLastColumnNumber(op.getStartColumn() + op.getText().length());
        ((Expression)expression).setLineNumber(op.getStartLine());
        ((Expression)expression).setLastLineNumber(op.getStartLine());
        return ((Expression)(expression));
    }

    public Expression parseExpression(GroovyParser.CastExpressionContext ctx) {
        Expression left = parseExpression(ctx.expression());
        ClassNode classNode = setupNodeLocation(parseExpression(ctx.genericClassNameExpression()), ctx.genericClassNameExpression());
        CastExpression expression = new CastExpression(classNode, left);
        return setupNodeLocation(expression, ctx);
    }

    public Expression parseExpression(GroovyParser.TernaryExpressionContext ctx) {
        BooleanExpression boolExpr = new BooleanExpression(parseExpression(ctx.expression(0)));
        Expression trueExpr = parseExpression(ctx.expression(1));
        Expression falseExpr = parseExpression(ctx.expression(2));
        return setupNodeLocation(new TernaryExpression(boolExpr, trueExpr, falseExpr), ctx);
    }

    public Expression parseExpression(GroovyParser.ElvisExpressionContext ctx) {
        Expression baseExpr = parseExpression(ctx.expression(0));
        Expression falseExpr = parseExpression(ctx.expression(1));
        return setupNodeLocation(new ElvisOperatorExpression(baseExpr, falseExpr), ctx);
    }

    protected Expression unaryMinusExpression(GroovyParser.ExpressionContext ctx) {
        // if we are a number literal then let's just parse it
        // as the negation operator on MIN_INT causes rounding to a long
        if (ctx instanceof GroovyParser.ConstantDecimalExpressionContext) {
            return parseDecimal('-' + ((GroovyParser.ConstantDecimalExpressionContext)ctx).DECIMAL().getText(), ctx);
        } else if (ctx instanceof GroovyParser.ConstantIntegerExpressionContext) {
            return parseInteger('-' + ((GroovyParser.ConstantIntegerExpressionContext)ctx).INTEGER().getText(), ctx);
        } else {
            return new UnaryMinusExpression(parseExpression(ctx));
        }
    }

    protected Expression unaryPlusExpression(GroovyParser.ExpressionContext ctx) {
        if (ctx instanceof GroovyParser.ConstantDecimalExpressionContext || ctx instanceof GroovyParser.ConstantIntegerExpressionContext) {
            return parseExpression(ctx);
        } else {
            return new UnaryPlusExpression(parseExpression(ctx));
        }
    }

    public Expression parseExpression(GroovyParser.UnaryExpressionContext ctx) {
        Object node = null;
        TerminalNode op = DefaultGroovyMethods.asType(ctx.getChild(0), TerminalNode.class);
        if (DefaultGroovyMethods.isCase("-", op.getText())) {
            node = unaryMinusExpression(ctx.expression());
        } else if (DefaultGroovyMethods.isCase("+", op.getText())) {
            node = unaryPlusExpression(ctx.expression());
        } else if (DefaultGroovyMethods.isCase("!", op.getText())) {
            node = new NotExpression(parseExpression(ctx.expression()));
        } else if (DefaultGroovyMethods.isCase("~", op.getText())) {
            node = new BitwiseNegationExpression(parseExpression(ctx.expression()));
        } else {
            assert false : "There is no " + op.getText() + " handler.";
        }

        ((Expression)node).setColumnNumber(op.getSymbol().getCharPositionInLine() + 1);
        ((Expression)node).setLineNumber(op.getSymbol().getLine());
        ((Expression)node).setLastLineNumber(op.getSymbol().getLine());
        ((Expression)node).setLastColumnNumber(op.getSymbol().getCharPositionInLine() + 1 + op.getText().length());
        return ((Expression)(node));
    }

    public SpreadExpression parseExpression(GroovyParser.SpreadExpressionContext ctx) {
        SpreadExpression expression = new SpreadExpression(parseExpression(ctx.expression()));
        return setupNodeLocation(expression, ctx);
    }


    public Expression parseExpression(GroovyParser.AnnotationParameterContext ctx) {
        if (ctx instanceof GroovyParser.AnnotationParamArrayExpressionContext) {
            GroovyParser.AnnotationParamArrayExpressionContext c = DefaultGroovyMethods.asType(ctx, GroovyParser.AnnotationParamArrayExpressionContext.class);
            return setupNodeLocation(new ListExpression(collect(c.annotationParameter(), new Closure<Expression>(null, null) {
                public Expression doCall(GroovyParser.AnnotationParameterContext it) {return parseExpression(it);}
            })), c);
        } else if (ctx instanceof GroovyParser.AnnotationParamBoolExpressionContext) {
            return parseExpression((GroovyParser.AnnotationParamBoolExpressionContext)ctx);
        } else if (ctx instanceof GroovyParser.AnnotationParamClassExpressionContext) {
            return setupNodeLocation(new ClassExpression(parseExpression((DefaultGroovyMethods.asType(ctx, GroovyParser.AnnotationParamClassExpressionContext.class)).genericClassNameExpression())), ctx);
        } else if (ctx instanceof GroovyParser.AnnotationParamDecimalExpressionContext) {
            return parseExpression((GroovyParser.AnnotationParamDecimalExpressionContext)ctx);
        } else if (ctx instanceof GroovyParser.AnnotationParamIntegerExpressionContext) {
            return parseExpression((GroovyParser.AnnotationParamIntegerExpressionContext)ctx);
        } else if (ctx instanceof GroovyParser.AnnotationParamNullExpressionContext) {
            return parseExpression((GroovyParser.AnnotationParamNullExpressionContext)ctx);
        } else if (ctx instanceof GroovyParser.AnnotationParamPathExpressionContext) {
            GroovyParser.AnnotationParamPathExpressionContext c = DefaultGroovyMethods.asType(ctx, GroovyParser.AnnotationParamPathExpressionContext.class);
            return collectPathExpression(c.pathExpression());
        } else if (ctx instanceof GroovyParser.AnnotationParamStringExpressionContext) {
            return parseExpression((GroovyParser.AnnotationParamStringExpressionContext)ctx);
        } else if (ctx instanceof GroovyParser.AnnotationParamClosureExpressionContext) {
            return parseExpression((GroovyParser.AnnotationParamClosureExpressionContext)ctx);
        }


        throw createParsingFailedException(new IllegalStateException(String.valueOf(ctx) + " is prohibited inside annotations."));
    }

    public Expression parseExpression(GroovyParser.VariableExpressionContext ctx) {
        return setupNodeLocation(new VariableExpression(ctx.IDENTIFIER().getText()), ctx);
    }

    public Expression parseExpression(GroovyParser.FieldAccessExpressionContext ctx) {
        Token op = ctx.op;
        Expression left = parseExpression(ctx.expression());

        GroovyParser.SelectorNameContext fieldName = ctx.selectorName();
        Expression right = fieldName != null ? new ConstantExpression(fieldName.getText()) :
                (ctx.STRING() != null ? parseConstantStringToken(ctx.STRING().getSymbol()) : parseExpression(ctx.gstring())
                );
        Expression node = null;
        switch (op.getType()) {
            case GroovyParser.ATTR_DOT:
                node = new AttributeExpression(left, right);
                break;
            case GroovyParser.MEMBER_POINTER:
                node = new MethodPointerExpression(left, right);
                break;
            case GroovyParser.SAFE_DOT:
                node = new PropertyExpression(left, right, true);
                break;
            case GroovyParser.STAR_DOT:
                node = new PropertyExpression(left, right, true /* For backwards compatibility! */);
                ((PropertyExpression)node).setSpreadSafe(true);
                break;
            default:
                // Normal dot
                node = new PropertyExpression(left, right, false);
                break;
        }
        return setupNodeLocation(node, ctx);
    }

    public PrefixExpression parseExpression(GroovyParser.PrefixExpressionContext ctx) {
        return setupNodeLocation(new PrefixExpression(createToken(DefaultGroovyMethods.asType(ctx.getChild(0), TerminalNode.class)), parseExpression(ctx.expression())), ctx);
    }

    public PostfixExpression parseExpression(GroovyParser.PostfixExpressionContext ctx) {
        return setupNodeLocation(new PostfixExpression(parseExpression(ctx.expression()), createToken(DefaultGroovyMethods.asType(ctx.getChild(1), TerminalNode.class))), ctx);
    }

    public ConstantExpression parseDecimal(String text, ParserRuleContext ctx) {
        return setupNodeLocation(new ConstantExpression(Numbers.parseDecimal(text), !text.startsWith("-")), ctx);// Why 10 is int but -10 is Integer?
    }

    public ConstantExpression parseExpression(GroovyParser.AnnotationParamDecimalExpressionContext ctx) {
        return parseDecimal(ctx.DECIMAL().getText(), ctx);
    }

    public ConstantExpression parseExpression(GroovyParser.ConstantDecimalExpressionContext ctx) {
        return parseDecimal(ctx.DECIMAL().getText(), ctx);
    }

    public ConstantExpression parseInteger(String text, ParserRuleContext ctx) {
        return setupNodeLocation(new ConstantExpression(Numbers.parseInteger(text), !text.startsWith("-")), ctx);//Why 10 is int but -10 is Integer?
    }

    public ConstantExpression parseInteger(String text, Token ctx) {
        return setupNodeLocation(new ConstantExpression(Numbers.parseInteger(text), !text.startsWith("-")), ctx);//Why 10 is int but -10 is Integer?
    }

    public ConstantExpression parseExpression(GroovyParser.ConstantIntegerExpressionContext ctx) {
        return parseInteger(ctx.INTEGER().getText(), ctx);
    }

    public ConstantExpression parseExpression(GroovyParser.AnnotationParamIntegerExpressionContext ctx) {
        return parseInteger(ctx.INTEGER().getText(), ctx);
    }

    public ConstantExpression parseExpression(GroovyParser.BoolExpressionContext ctx) {
        return setupNodeLocation(new ConstantExpression(!asBoolean(ctx.KW_FALSE()), true), ctx);
    }

    public ConstantExpression parseExpression(GroovyParser.AnnotationParamBoolExpressionContext ctx) {
        return setupNodeLocation(new ConstantExpression(!asBoolean(ctx.KW_FALSE()), true), ctx);
    }

    public ConstantExpression cleanConstantStringLiteral(String text) {
        int slashyType = text.startsWith("/") ? StringUtil.SLASHY :
                text.startsWith("$/") ? StringUtil.DOLLAR_SLASHY : StringUtil.NONE_SLASHY;

        if (text.startsWith("'''") || text.startsWith("\"\"\"")) {
            text = StringUtil.removeCR(text); // remove CR in the multiline string

            text = text.length() == 6 ? "" : text.substring(3, text.length() - 3);
        } else if (text.startsWith("'") || text.startsWith("/") || text.startsWith("\"")) {
            text = text.length() == 2 ? "" : text.substring(1, text.length() - 1);
        } else if (text.startsWith("$/")) {
            text = StringUtil.removeCR(text);

            text = text.length() == 4 ? "" : text.substring(2, text.length() - 2);
        }

        //handle escapes.
        text = StringUtil.replaceEscapes(text, slashyType);

        return new ConstantExpression(text, true);
    }

    public ConstantExpression parseConstantString(ParserRuleContext ctx) {
        return setupNodeLocation(cleanConstantStringLiteral(ctx.getText()), ctx);
    }

    public ConstantExpression parseConstantStringToken(Token token) {
        return setupNodeLocation(cleanConstantStringLiteral(token.getText()), token);
    }

    public ConstantExpression parseExpression(GroovyParser.ConstantExpressionContext ctx) {
        return parseConstantString(ctx);
    }

    public Expression parseExpression(GroovyParser.SuperExpressionContext ctx) {
        return setupNodeLocation(new VariableExpression(ctx.KW_SUPER().getText()), ctx);
    }

    public Expression parseExpression(GroovyParser.ThisExpressionContext ctx) {
        return setupNodeLocation(new VariableExpression(ctx.KW_THIS().getText()), ctx);
    }

    public ConstantExpression parseExpression(GroovyParser.AnnotationParamStringExpressionContext ctx) {
        return parseConstantString(ctx);
    }

    public Expression parseExpression(GroovyParser.AnnotationParamClosureExpressionContext ctx) {
        return setupNodeLocation(parseExpression(ctx.closureExpressionRule()), ctx);
    }

    public Expression parseExpression(GroovyParser.GstringExpressionContext ctx) {
        return parseExpression(ctx.gstring());
    }

    public Expression parseExpression(GroovyParser.GstringContext ctx) {
        String gstringStartText = ctx.GSTRING_START().getText();
        final int slashyType = gstringStartText.startsWith("/") ? StringUtil.SLASHY :
                gstringStartText.startsWith("$/") ? StringUtil.DOLLAR_SLASHY : StringUtil.NONE_SLASHY;

        Closure<String> clearStart = new Closure<String>(null, null) {
            public String doCall(String it) {

                if (it.startsWith("\"\"\"")) {
                    it = StringUtil.removeCR(it);

                    it = it.substring(2); // translate leading """ to "
                } else if (it.startsWith("$/")) {
                    it = StringUtil.removeCR(it);

                    it = "\"" + it.substring(2); // translate leading $/ to "

                }

                it = StringUtil.replaceEscapes(it, slashyType);

                return (it.length() == 2)
                        ? ""
                        : DefaultGroovyMethods.getAt(it, new IntRange(true, 1, -2));
            }

        };
        final Closure<String> clearPart = new Closure<String>(null, null) {
            public String doCall(String it) {

                it = StringUtil.removeCR(it);

                it = StringUtil.replaceEscapes(it, slashyType);

                return it.length() == 1
                        ? ""
                        : DefaultGroovyMethods.getAt(it, new IntRange(true, 0, -2));
            }

        };
        Closure<String> clearEnd = new Closure<String>(null, null) {
            public String doCall(String it) {

                if (it.endsWith("\"\"\"")) {
                    it = StringUtil.removeCR(it);

                    it = DefaultGroovyMethods.getAt(it, new IntRange(true, 0, -3)); // translate tailing """ to "
                } else if (it.endsWith("/$")) {
                    it = StringUtil.removeCR(it);

                    it = DefaultGroovyMethods.getAt(it, new IntRange(true, 0, -3)) + "\""; // translate tailing /$ to "
                }

                it = StringUtil.replaceEscapes(it, slashyType);

                return (it.length() == 1)
                        ? ""
                        : DefaultGroovyMethods.getAt(it, new IntRange(true, 0, -2));
            }

        };
        Collection<String> strings = DefaultGroovyMethods.plus(DefaultGroovyMethods.plus(new ArrayList<String>(Arrays.asList(clearStart.call(ctx.GSTRING_START().getText()))), collect(ctx.GSTRING_PART(), new Closure<String>(null, null) {
            public String doCall(TerminalNode it) {return clearPart.call(it.getText());}
        })), new ArrayList<String>(Arrays.asList(clearEnd.call(ctx.GSTRING_END().getText()))));
        final List<Expression> expressions = new ArrayList<Expression>();

        final List<ParseTree> children = ctx.children;
        DefaultGroovyMethods.eachWithIndex(children, new Closure<Collection>(null, null) {
            public Collection doCall(Object it, Integer i) {
                if (!(it instanceof GroovyParser.GstringExpressionBodyContext)) {
                    return expressions;
                }

                GroovyParser.GstringExpressionBodyContext gstringExpressionBodyContext = (GroovyParser.GstringExpressionBodyContext) it;

                if (asBoolean(gstringExpressionBodyContext.gstringPathExpression())) {
                    expressions.add(collectPathExpression(gstringExpressionBodyContext.gstringPathExpression()));
                    return expressions;
                } else if (asBoolean(gstringExpressionBodyContext.closureExpressionRule())) {
                    GroovyParser.ClosureExpressionRuleContext closureExpressionRule = gstringExpressionBodyContext.closureExpressionRule();
                    Expression expression = parseExpression(closureExpressionRule);

                    if (!asBoolean(closureExpressionRule.CLOSURE_ARG_SEPARATOR())) {

                        MethodCallExpression methodCallExpression = new MethodCallExpression(expression, "call", new ArgumentListExpression());

                        expressions.add(setupNodeLocation(methodCallExpression, expression));
                        return expressions;
                    }

                    expressions.add(expression);
                    return expressions;
                } else {
                    if (asBoolean(gstringExpressionBodyContext.expression())) {
                        // We can guarantee, that it will be at least fallback ExpressionContext multimethod overloading, that can handle such situation.
                        //noinspection GroovyAssignabilityCheck
                        expressions.add(parseExpression(gstringExpressionBodyContext.expression()));
                        return expressions;
                    } else { // handle empty expression e.g. "GString ${}"
                        expressions.add(new ConstantExpression(null));
                        return expressions;
                    }
                }

            }

        });
        GStringExpression gstringNode = new GStringExpression(ctx.getText(), collect(strings, new Closure<ConstantExpression>(null, null) {
            public ConstantExpression doCall(String it) {return new ConstantExpression(it);}
        }), expressions);
        return setupNodeLocation(gstringNode, ctx);
    }

    public Expression parseExpression(GroovyParser.NullExpressionContext ctx) {
        return setupNodeLocation(new ConstantExpression(null), ctx);
    }

    public Expression parseExpression(GroovyParser.AnnotationParamNullExpressionContext ctx) {
        return setupNodeLocation(new ConstantExpression(null), ctx);
    }

    public Expression parseExpression(GroovyParser.AssignmentExpressionContext ctx) {
        Expression left;
        Expression right;
        org.codehaus.groovy.syntax.Token token;

        if (asBoolean(ctx.LPAREN())) { // tuple assignment expression
            List<Expression> expressions = new LinkedList<Expression>();

            for (TerminalNode id : ctx.IDENTIFIER()) {
                expressions.add(new VariableExpression(id.getText(), ClassHelper.OBJECT_TYPE));
            }

            left = new TupleExpression(expressions);
            right = parseExpression(ctx.expression(0));

            token = this.createGroovyToken(ctx.ASSIGN().getSymbol(), Types.ASSIGN);
        } else {
            left = parseExpression(ctx.expression(0));// TODO reference to AntlrParserPlugin line 2304 for error handling.
            right = parseExpression(ctx.expression(1));

            token = createToken(DefaultGroovyMethods.asType(ctx.getChild(1), TerminalNode.class));
        }

        return setupNodeLocation(new BinaryExpression(left, token, right), ctx);
    }

    public Expression collectPathExpression(GroovyParser.PathExpressionContext ctx) {
        List<TerminalNode> identifiers = ctx.IDENTIFIER();
        switch (identifiers.size()) {
            case 1:
                return new VariableExpression(identifiers.get(0).getText());
            default:
                Expression inject = DefaultGroovyMethods.inject(identifiers.subList(1, identifiers.size()), new VariableExpression(identifiers.get(0).getText()), new Closure<PropertyExpression>(null, null) {
                    public PropertyExpression doCall(Object val, Object prop) {
                        return new PropertyExpression(DefaultGroovyMethods.asType(val, Expression.class), new ConstantExpression(((TerminalNode)prop).getText()));
                    }

                });
                return inject;
        }
    }

    public Expression collectPathExpression(GroovyParser.GstringPathExpressionContext ctx) {
        if (!asBoolean(ctx.GSTRING_PATH_PART()))
            return new VariableExpression(ctx.IDENTIFIER().getText());
        else {
            Expression inj = DefaultGroovyMethods.inject(ctx.GSTRING_PATH_PART(), new VariableExpression(ctx.IDENTIFIER().getText()), new Closure<PropertyExpression>(null, null) {
                public PropertyExpression doCall(Object val, Object prop) {
                    return new PropertyExpression(DefaultGroovyMethods.asType(val, Expression.class), new ConstantExpression(DefaultGroovyMethods.getAt(((TerminalNode)prop).getText(), new IntRange(true, 1, -1))));
                }

            });
            return inj;
        }

    }

    public BinaryExpression parseExpression(GroovyParser.IndexExpressionContext ctx) {
        // parse the lhs
        Expression leftExpression = parseExpression(ctx.expression(0));
        int expressionCount = ctx.expression().size();
        List<Expression> expressions = new LinkedList<Expression>();
        Expression rightExpression = null;

        // parse the indices
        for (int i = 1; i < expressionCount; ++i) {
            expressions.add(parseExpression(ctx.expression(i)));
        }
        if (expressionCount == 2) {
            // If only one index, treat as single expression
            rightExpression = expressions.get(0);
            // unless it's a spread operator...
            if (rightExpression instanceof SpreadExpression) {
                ListExpression wrapped = new ListExpression();
                wrapped.addExpression(rightExpression);
                rightExpression = setupNodeLocation(wrapped, ctx.expression(1));
            }
        } else {
            // Otherwise, setup as list expression
            ListExpression listExpression = new ListExpression(expressions);
            listExpression.setWrapped(true);
            rightExpression = listExpression;
            // if nonempty, set location info for index list
            if (expressionCount > 2) {
                Token start = ctx.expression(1).getStart();
                Token stop = ctx.expression(expressionCount - 1).getStart();
                listExpression.setLineNumber(start.getLine());
                listExpression.setColumnNumber(start.getCharPositionInLine() + 1);
                listExpression.setLastLineNumber(stop.getLine());
                listExpression.setLastColumnNumber(stop.getCharPositionInLine() + 1 + stop.getText().length());
            }
        }
        BinaryExpression binaryExpression = new BinaryExpression(leftExpression, createToken(ctx.LBRACK(), 1), rightExpression);
        return setupNodeLocation(binaryExpression, ctx);
    }

    public Expression parseExpression(GroovyParser.CallExpressionContext ctx) {
        Expression expression = parseCallExpressionRule(ctx.callExpressionRule(), ctx.expression());

        if (expression instanceof ConstructorCallExpression) {
            return expression;
        }

        MethodCallExpression method = (MethodCallExpression) expression;

        if (asBoolean(ctx.expression())) {
            Token op = ctx.op;
            method.setSpreadSafe(op.getType() == GroovyParser.STAR_DOT);
            method.setSafe(op.getType() == GroovyParser.SAFE_DOT);
        }

        return setupNodeLocation(method, ctx);
    }

    /**
     * If argument list contains closure and named argument, the argument list's struture looks like as follows:
     *
     *      ArgumentListExpression
     *            MapExpression (NOT NamedArgumentListExpression!)
     *            ClosureExpression
     *
     * Original structure:
     *
     *      TupleExpression
     *            NamedArgumentListExpression
     *            ClosureExpression
     *
     * @param argumentListExpression
     * @return
     *
     */
    private TupleExpression convertArgumentList(TupleExpression argumentListExpression) {
        if (argumentListExpression instanceof ArgumentListExpression) {
            return argumentListExpression;
        }

        List<Expression> result = new LinkedList<Expression>();

        int namedArgumentListExpressionCnt = 0, closureExpressionCnt = 0;
        for (Expression expression : argumentListExpression.getExpressions()) {

            if (expression instanceof NamedArgumentListExpression) {
                expression = setupNodeLocation(new MapExpression(((NamedArgumentListExpression) expression).getMapEntryExpressions()), expression);
                namedArgumentListExpressionCnt++;
            } else if (expression instanceof ClosureExpression) {
                closureExpressionCnt++;
            }

            result.add(expression);
        }

        if (namedArgumentListExpressionCnt > 0 && closureExpressionCnt > 0) {
            return setupNodeLocation(new ArgumentListExpression(result), argumentListExpression);
        }

        return argumentListExpression;
    }

    public Expression parseCallExpressionRule(GroovyParser.CallExpressionRuleContext ctx, GroovyParser.ExpressionContext expressionContext) {
        Expression method;
        boolean isClosureCall = asBoolean(ctx.c);

        if (isClosureCall) {
            method = new ConstantExpression("call");
        } else {
            GroovyParser.SelectorNameContext methodSelector = ctx.selectorName();
            method = methodSelector != null ? new ConstantExpression(methodSelector.getText()) : (
                    ctx.STRING() != null ? parseConstantStringToken(ctx.STRING().getSymbol()) : parseExpression(ctx.gstring())
            );
        }

        TupleExpression argumentListExpression = (TupleExpression) createArgumentList(ctx.argumentList());

        for (GroovyParser.ClosureExpressionRuleContext closureExpressionRuleContext : ctx.closureExpressionRule()) {
            if (ctx.c == closureExpressionRuleContext) {
                continue;
            }

            argumentListExpression.addExpression(parseExpression(closureExpressionRuleContext));
        }

        argumentListExpression = convertArgumentList(argumentListExpression);

        boolean implicitThis = !isClosureCall && !asBoolean(expressionContext);
        if (implicitThis && VariableExpression.THIS_EXPRESSION.getText().equals(method.getText())) {
            // Actually a constructor call
            ConstructorCallExpression call = new ConstructorCallExpression(ClassNode.THIS, argumentListExpression);
            return setupNodeLocation(call, ctx);
//        } else if (implicitThis && VariableExpression.SUPER_EXPRESSION.getText().equals(methodName)) {
//            // Use this once path expression is refac'ed
//            // Actually a constructor call
//            ConstructorCallExpression call = new ConstructorCallExpression(ClassNode.SUPER, argumentListExpression);
//            return setupNodeLocation(call, ctx);
        }

        MethodCallExpression expression = new MethodCallExpression(isClosureCall ? parseExpression(ctx.c)
                                                                                 : (asBoolean(expressionContext) ? parseExpression(expressionContext) : VariableExpression.THIS_EXPRESSION)
                                                                   , method, argumentListExpression);
        expression.setImplicitThis(implicitThis);

        return setupNodeLocation(expression, ctx);
    }

    public ConstructorCallExpression parseExpression(GroovyParser.ConstructorCallExpressionContext ctx) {
        Expression argumentListExpression = createArgumentList(ctx.argumentList());
        ConstructorCallExpression expression = new ConstructorCallExpression(ClassNode.SUPER, argumentListExpression);
        return setupNodeLocation(expression, ctx);
    }

    public ClassNode parseExpression(GroovyParser.ClassNameExpressionContext ctx) {
        ClassNode classNode;

        if (asBoolean(ctx.BUILT_IN_TYPE())) {
            classNode = ClassHelper.make(ctx.BUILT_IN_TYPE().getText());
        } else {
            classNode = ClassHelper.make(DefaultGroovyMethods.join(ctx.IDENTIFIER(), "."));
        }

        return setupNodeLocation(classNode, ctx);
    }

    public ClassNode parseExpression(GroovyParser.GenericClassNameExpressionContext ctx) {
        ClassNode classNode = parseExpression(ctx.classNameExpression());

        if (asBoolean(ctx.LBRACK())) {
            for (int i = 0, n = ctx.LBRACK().size(); i < n; i++) {
                classNode = classNode.makeArray();
            }
        } else {
            // Groovy's bug? array's generics type will be ignored. e.g. List<String>[]... p
            classNode.setGenericsTypes(parseGenericList(ctx.genericList()));
        }

        if (asBoolean(ctx.ELLIPSIS())) {
            classNode = classNode.makeArray();
        }

        return setupNodeLocation(classNode, ctx);
    }

    public GenericsType[] parseGenericList(GroovyParser.GenericListContext ctx) {
        if (ctx == null)
            return null;
        List<GenericsType> collect = collect(ctx.genericListElement(), new Closure<GenericsType>(null, null) {
            public GenericsType doCall(GroovyParser.GenericListElementContext it) {
                if (it instanceof GroovyParser.GenericsConcreteElementContext)
                    return setupNodeLocation(new GenericsType(parseExpression(((GroovyParser.GenericsConcreteElementContext)it).genericClassNameExpression())), it);
                else {
                    assert it instanceof GroovyParser.GenericsWildcardElementContext;
                    GroovyParser.GenericsWildcardElementContext gwec = (GroovyParser.GenericsWildcardElementContext)it;
                    ClassNode baseType = ClassHelper.makeWithoutCaching("?");
                    ClassNode[] upperBounds = null;
                    ClassNode lowerBound = null;
                    if (asBoolean(gwec.KW_EXTENDS())) {
                        ClassNode classNode = parseExpression(gwec.genericClassNameExpression());
                        upperBounds = new ClassNode[]{ classNode };
                    } else if (asBoolean(gwec.KW_SUPER()))
                        lowerBound = parseExpression(gwec.genericClassNameExpression());

                    GenericsType type = new GenericsType(baseType, upperBounds, lowerBound);
                    type.setWildcard(true);
                    type.setName("?");
                    return setupNodeLocation(type, it);
                }

            }
        });
        return collect.toArray(new GenericsType[collect.size()]);
    }

    public GenericsType[] parseGenericDeclaration(GroovyParser.GenericDeclarationListContext ctx) {
        if (ctx == null)
            return null;
        List<GenericsType> genericTypes = collect(ctx.genericsDeclarationElement(), new Closure<GenericsType>(null, null) {
            public GenericsType doCall(GroovyParser.GenericsDeclarationElementContext it) {
                ClassNode classNode = parseExpression(it.genericClassNameExpression(0));
                ClassNode[] upperBounds = null;
                if (asBoolean(it.KW_EXTENDS())) {
                    List<GroovyParser.GenericClassNameExpressionContext> genericClassNameExpressionContexts = DefaultGroovyMethods.toList(it.genericClassNameExpression());
                    upperBounds = DefaultGroovyMethods.asType(collect(genericClassNameExpressionContexts.subList(1, genericClassNameExpressionContexts.size()), new MethodClosure(ASTBuilder.this, "parseExpression")), ClassNode[].class);
                }
                GenericsType type = new GenericsType(classNode, upperBounds, null);
                return setupNodeLocation(type, it);
            }
        });
        return  genericTypes.toArray(new GenericsType[genericTypes.size()]);
    }

    public List<DeclarationExpression> parseDeclaration(GroovyParser.DeclarationRuleContext ctx) {
        ClassNode type = parseTypeDeclaration(ctx.typeDeclaration());
        List<GroovyParser.SingleDeclarationContext> variables = ctx.singleDeclaration();
        List<DeclarationExpression> declarations = new LinkedList<DeclarationExpression>();

        if (asBoolean(ctx.tupleDeclaration())) {
            DeclarationExpression declarationExpression = parseTupleDeclaration(ctx.tupleDeclaration(), asBoolean(ctx.KW_FINAL()));

            declarations.add(declarationExpression);

            return declarations;
        }

        for (GroovyParser.SingleDeclarationContext variableCtx : variables) {
            VariableExpression left = new VariableExpression(variableCtx.IDENTIFIER().getText(), type);

            if (asBoolean(ctx.KW_FINAL())) {
                left.setModifiers(Opcodes.ACC_FINAL);
            }

            org.codehaus.groovy.syntax.Token token;
            if (asBoolean(variableCtx.ASSIGN())) {
                token = createGroovyToken(variableCtx.ASSIGN().getSymbol(), Types.ASSIGN);
            } else {
                int line = variableCtx.start.getLine();
                int col = -1; //ASSIGN TOKEN DOES NOT APPEAR, SO COL IS -1. IF NO ERROR OCCURS, THE ORIGINAL CODE CAN BE REMOVED IN THE FURTURE: variableCtx.getStart().getCharPositionInLine() + 1; // FIXME Why assignment token location is it's first occurrence.

                token = new org.codehaus.groovy.syntax.Token(Types.ASSIGN, "=", line, col);
            }

            GroovyParser.ExpressionContext initialValueCtx = variableCtx.expression();
            Expression initialValue = initialValueCtx != null ? parseExpression(variableCtx.expression()) : setupNodeLocation(new EmptyExpression(),ctx);
            DeclarationExpression expression = new DeclarationExpression(left, token, initialValue);
            attachAnnotations(expression, ctx.annotationClause());
            declarations.add(setupNodeLocation(expression, variableCtx));
        }

        int declarationsSize = declarations.size();
        if (declarationsSize == 1) {
            setupNodeLocation(declarations.get(0), ctx);
        } else if (declarationsSize > 0) {
            DeclarationExpression declarationExpression = declarations.get(0);
            // Tweak start of first declaration
            declarationExpression.setLineNumber(ctx.getStart().getLine());
            declarationExpression.setColumnNumber(ctx.getStart().getCharPositionInLine() + 1);
        }

        return declarations;
    }

    private org.codehaus.groovy.syntax.Token createGroovyToken(Token token, int type) {
        if (null == token) {
            throw new IllegalArgumentException("token should not be null");
        }

        return new org.codehaus.groovy.syntax.Token(type, token.getText(), token.getLine(), token.getCharPositionInLine());
    }

    public VariableExpression parseTupleVariableDeclaration(GroovyParser.TupleVariableDeclarationContext ctx) {
        ClassNode type = asBoolean(ctx.genericClassNameExpression())
                ? parseExpression(ctx.genericClassNameExpression())
                : ClassHelper.OBJECT_TYPE;

        return setupNodeLocation(new VariableExpression(ctx.IDENTIFIER().getText(), type), ctx);
    }

    public DeclarationExpression parseTupleDeclaration(GroovyParser.TupleDeclarationContext ctx, boolean isFinal) {
        // tuple must have an initial value.
        if (null == ctx.expression()) {
            throw createParsingFailedException(new InvalidSyntaxException("tuple declaration must have an initial value.", ctx));
        }

        List<Expression> variables = new LinkedList<Expression>();

        for (GroovyParser.TupleVariableDeclarationContext tupleVariableDeclarationContext : ctx.tupleVariableDeclaration()) {
            VariableExpression variableExpression = parseTupleVariableDeclaration(tupleVariableDeclarationContext);

            if (isFinal) {
                variableExpression.setModifiers(Opcodes.ACC_FINAL);
            }

            variables.add(variableExpression);
        }

        ArgumentListExpression argumentListExpression = new ArgumentListExpression(variables);
        org.codehaus.groovy.syntax.Token token = createGroovyToken(ctx.ASSIGN().getSymbol(), Types.ASSIGN);

        Expression initialValue = (ctx != null) ? parseExpression(ctx.expression())
                : setupNodeLocation(new EmptyExpression(),ctx);

        DeclarationExpression declarationExpression  = new DeclarationExpression(argumentListExpression, token, initialValue);

        return setupNodeLocation(declarationExpression, ctx);
    }

    private Expression createArgumentList(GroovyParser.ArgumentListContext ctx) {
        final List<MapEntryExpression> mapArgs = new ArrayList<MapEntryExpression>();
        final List<Expression> expressions = new ArrayList<Expression>();

        if (ctx != null) {
            DefaultGroovyMethods.each(ctx.children, new Closure<Collection<? extends Expression>>(null, null) {
                public Collection<? extends Expression> doCall(ParseTree it) {
                    if (it instanceof GroovyParser.ArgumentContext) {
                        if (asBoolean(((GroovyParser.ArgumentContext)it).mapEntry())) {
                            mapArgs.add(parseExpression(((GroovyParser.ArgumentContext) it).mapEntry()));
                            return mapArgs;
                        } else {
                            expressions.add(parseExpression(((GroovyParser.ArgumentContext) it).expression()));
                            return expressions;
                        }
                    } else if (it instanceof GroovyParser.ClosureExpressionRuleContext) {
                        expressions.add(parseExpression((GroovyParser.ClosureExpressionRuleContext) it));


                        return expressions;
                    }
                    return null;
                }
            });
        }
        if (asBoolean(expressions)) {
            if (asBoolean(mapArgs))
                expressions.add(0, new MapExpression(mapArgs));

            return setupNodeLocation(new ArgumentListExpression(expressions), ctx);
        } else {
            if (asBoolean(mapArgs))
                return setupNodeLocation(new TupleExpression(new NamedArgumentListExpression(mapArgs)), ctx);
            else
                return setupNodeLocation(new ArgumentListExpression(), ctx);
        }

    }

    public void attachAnnotations(AnnotatedNode node, List<GroovyParser.AnnotationClauseContext> ctxs) {
        for (GroovyParser.AnnotationClauseContext ctx : ctxs) {
            AnnotationNode annotation = parseAnnotation(ctx);
            node.addAnnotation(annotation);
        }
    }

    private void attachTraitTransformAnnotation(ClassNode classNode) {
        classNode.addAnnotation(new AnnotationNode(ClassHelper.make(GROOVY_TRANSFORM_TRAIT)));
    }

    public List<AnnotationNode> parseAnnotations(List<GroovyParser.AnnotationClauseContext> ctxs) {
        return collect(ctxs, new Closure<AnnotationNode>(null, null) {
            public AnnotationNode doCall(GroovyParser.AnnotationClauseContext it) {return parseAnnotation(it);}
        });
    }

    public AnnotationNode parseAnnotation(GroovyParser.AnnotationClauseContext ctx) {
        AnnotationNode node = new AnnotationNode(parseExpression(ctx.genericClassNameExpression()));
        if (asBoolean(ctx.annotationElement()))
            node.addMember("value", parseAnnotationElement(ctx.annotationElement()));
        else {
            for (GroovyParser.AnnotationElementPairContext pair : ctx.annotationElementPair()) {
                node.addMember(pair.IDENTIFIER().getText(), parseAnnotationElement(pair.annotationElement()));
            }

        }


        return setupNodeLocation(node, ctx);
    }

    public Expression parseAnnotationElement(GroovyParser.AnnotationElementContext ctx) {
        GroovyParser.AnnotationClauseContext annotationClause = ctx.annotationClause();
        if (asBoolean(annotationClause))
            return setupNodeLocation(new AnnotationConstantExpression(parseAnnotation(annotationClause)), annotationClause);
        else return parseExpression(ctx.annotationParameter());
    }

    public ClassNode[] parseThrowsClause(GroovyParser.ThrowsClauseContext ctx) {
        List list = asBoolean(ctx)
                ? collect(ctx.classNameExpression(), new Closure<ClassNode>(null, null) {
            public ClassNode doCall(GroovyParser.ClassNameExpressionContext it) {return parseExpression(it);}
        })
                : new ArrayList();
        return (ClassNode[])list.toArray(new ClassNode[list.size()]);
    }

    /**
     * @param node
     * @param cardinality Used for handling GT ">" operator, which can be repeated to give bitwise shifts >> or >>>
     * @return
     */
    public org.codehaus.groovy.syntax.Token createToken(TerminalNode node, int cardinality) {
        String text = multiply(node.getText(), cardinality);
        return new org.codehaus.groovy.syntax.Token(node.getText().equals("..<") || node.getText().equals("..")
                ? Types.RANGE_OPERATOR
                : Types.lookup(text, Types.ANY), text, node.getSymbol().getLine(), node.getSymbol().getCharPositionInLine() + 1);
    }

    /**
     * @param node
     * @return
     */
    public org.codehaus.groovy.syntax.Token createToken(TerminalNode node) {
        return createToken(node, 1);
    }

    public ClassNode parseTypeDeclaration(GroovyParser.TypeDeclarationContext ctx) {
        return !asBoolean(ctx) || ctx.KW_DEF() != null
                ? ClassHelper.OBJECT_TYPE
                : setupNodeLocation(parseExpression(ctx.genericClassNameExpression()), ctx);
    }

    public ArrayExpression parse(GroovyParser.NewArrayRuleContext ctx) {
        List<Expression> collect = collect(ctx.INTEGER(), new Closure<Expression>(null, null) {
            public Expression doCall(TerminalNode it) {return parseInteger(it.getText(), it.getSymbol());}
        });
        ArrayExpression expression = new ArrayExpression(parseExpression(ctx.classNameExpression()), new ArrayList<Expression>(), collect);
        return setupNodeLocation(expression, ctx);
    }

    public ConstructorCallExpression parse(GroovyParser.NewInstanceRuleContext ctx) {
        ClassNode creatingClass = asBoolean(ctx.genericClassNameExpression())
                ? parseExpression(ctx.genericClassNameExpression())
                : parseExpression(ctx.classNameExpression());
        if (asBoolean(ctx.LT())) creatingClass.setGenericsTypes(new GenericsType[0]);

        ConstructorCallExpression expression;
        if (!asBoolean(ctx.classBody())) {
            expression = setupNodeLocation(new ConstructorCallExpression(creatingClass, createArgumentList(ctx.argumentList())), ctx);
        } else {
            ClassNode outer;

            if (!this.classes.isEmpty()) {
                outer = this.classes.peek();
            } else {
                outer = moduleNode.getScriptClassDummy();
            }

            InnerClassNode classNode = new InnerClassNode(outer, outer.getName() + "$" + String.valueOf((this.anonymousClassesCount = ++this.anonymousClassesCount)), Opcodes.ACC_PUBLIC, ClassHelper.make(creatingClass.getName()));

            expression = setupNodeLocation(new ConstructorCallExpression(classNode, createArgumentList(ctx.argumentList())), ctx);
            expression.setUsingAnonymousInnerClass(true);
            classNode.setAnonymous(true);

            if (!this.innerClassesDefinedInMethod.isEmpty()) {
                DefaultGroovyMethods.last(this.innerClassesDefinedInMethod).add(classNode);
            }

            this.moduleNode.addClass(classNode);
            this.classes.add(classNode);
            parseClassBody(classNode, ctx.classBody());
            this.classes.pop();
        }

        return expression;
    }

    public Parameter[] parseParameters(GroovyParser.ArgumentDeclarationListContext ctx) {
        List<Parameter> parameterList = ctx == null || ctx.argumentDeclaration() == null ?
                new ArrayList<Parameter>(0) :
                collect(ctx.argumentDeclaration(), new Closure<Parameter>(null, null) {
                    public Parameter doCall(GroovyParser.ArgumentDeclarationContext it) {
                        Parameter parameter = new Parameter(parseTypeDeclaration(it.typeDeclaration()), it.IDENTIFIER().getText());
                        attachAnnotations(parameter, it.annotationClause());

                        if (asBoolean(it.KW_FINAL())) {
                            parameter.setModifiers(Opcodes.ACC_FINAL);
                        }

                        if (asBoolean(it.expression()))
                            parameter.setInitialExpression(parseExpression(it.expression()));

                        return setupNodeLocation(parameter, it);
                    }
                });
        return parameterList.toArray(new Parameter[parameterList.size()]);
    }

    public MethodNode getOrCreateClinitMethod(ClassNode classNode) {
        MethodNode methodNode = DefaultGroovyMethods.find(classNode.getMethods(), new Closure<Boolean>(null, null) {
            public Boolean doCall(MethodNode it) {return it.getName().equals("<clinit>");}
        });
        if (!asBoolean(methodNode)) {
            methodNode = new MethodNode("<clinit>", Opcodes.ACC_STATIC, ClassHelper.VOID_TYPE, new Parameter[0], new ClassNode[0], new BlockStatement());
            methodNode.setSynthetic(true);
            classNode.addMethod(methodNode);
        }

        return methodNode;
    }

    /**
     * Sets location(lineNumber, colNumber, lastLineNumber, lastColumnNumber) for node using standard context information.
     * Note: this method is implemented to be closed over ASTNode. It returns same node as it received in arguments.
     *
     * @param astNode Node to be modified.
     * @param ctx     Context from which information is obtained.
     * @return Modified astNode.
     */
    public <T extends ASTNode> T setupNodeLocation(T astNode, ParserRuleContext ctx) {
        if (null == ctx) {
            return astNode;
        }

        Token start = ctx.getStart();
        Token stop = ctx.getStop();

        astNode.setLineNumber(start.getLine());
        astNode.setColumnNumber(start.getCharPositionInLine() + 1);
        astNode.setLastLineNumber(stop.getLine());
        astNode.setLastColumnNumber(stop.getCharPositionInLine() + 1 + stop.getText().length());
//        System.err.println(astNode.getClass().getSimpleName() + " at " + astNode.getLineNumber() + ":" + astNode.getColumnNumber());
        return astNode;
    }

    public <T extends ASTNode> T setupNodeLocation(T astNode, Token token) {
        astNode.setLineNumber(token.getLine());
        astNode.setColumnNumber(token.getCharPositionInLine() + 1);
        astNode.setLastLineNumber(token.getLine());
        astNode.setLastColumnNumber(token.getCharPositionInLine() + 1 + token.getText().length());
//        System.err.println(astNode.getClass().getSimpleName() + " at " + astNode.getLineNumber() + ":" + astNode.getColumnNumber());
        return astNode;
    }

    public <T extends ASTNode> T setupNodeLocation(T astNode, ASTNode source) {
        astNode.setLineNumber(source.getLineNumber());
        astNode.setColumnNumber(source.getColumnNumber());
        astNode.setLastLineNumber(source.getLastLineNumber());
        astNode.setLastColumnNumber(source.getLastColumnNumber());
        return astNode;
    }

    public int parseClassModifiers(List<GroovyParser.ClassModifierContext> ctxs) {
        List<TerminalNode> visibilityModifiers = new ArrayList<TerminalNode>();
        int modifiers = 0;
        for (int i = 0; i < ctxs.size(); i++) {
            for (Object ctx : ctxs.get(i).children) {
                ParseTree child = null;
                if (ctx instanceof List) {
                    List list = (List)ctx;
                    assert list.size() == 1;
                    child = (ParseTree)list.get(0);
                }
                else
                    child = (ParseTree)ctx;

                assert child instanceof TerminalNode;
                switch (((TerminalNode)child).getSymbol().getType()) {
                    case GroovyLexer.VISIBILITY_MODIFIER:
                        visibilityModifiers.add((TerminalNode)child);
                        break;
                    case GroovyLexer.KW_STATIC:
                        modifiers |= checkModifierDuplication(modifiers, Opcodes.ACC_STATIC, (TerminalNode)child);
                        break;
                    case GroovyLexer.KW_ABSTRACT:
                        modifiers |= checkModifierDuplication(modifiers, Opcodes.ACC_ABSTRACT, (TerminalNode)child);
                        break;
                    case GroovyLexer.KW_FINAL:
                        modifiers |= checkModifierDuplication(modifiers, Opcodes.ACC_FINAL, (TerminalNode)child);
                        break;
                    case GroovyLexer.KW_STRICTFP:
                        modifiers |= checkModifierDuplication(modifiers, Opcodes.ACC_STRICT, (TerminalNode)child);
                        break;
                }
            }
        }

        if (asBoolean(visibilityModifiers))
            modifiers |= parseVisibilityModifiers(visibilityModifiers, 0);
        else modifiers |= Opcodes.ACC_PUBLIC | Opcodes.ACC_SYNTHETIC;
        return modifiers;
    }

    public int checkModifierDuplication(int modifier, int opcode, TerminalNode node) {
        if ((modifier & opcode) == 0) return modifier | opcode;
        else {
            Token symbol = node.getSymbol();

            Integer line = symbol.getLine();
            Integer col = symbol.getCharPositionInLine() + 1;
            sourceUnit.addError(new SyntaxException("Cannot repeat modifier: " + symbol.getText() + " at line: " + String.valueOf(line) + " column: " + String.valueOf(col) + ". File: " + sourceUnit.getName(), line, col));
            return modifier;
        }

    }

    /**
     * Traverse through modifiers, and combine them in one int value. Raise an error if there is multiple occurrences of same modifier.
     *
     * @param ctxList                   modifiers list.
     * @param defaultVisibilityModifier Default visibility modifier. Can be null. Applied if providen, and no visibility modifier exists in the ctxList.
     * @return tuple of int modifier and boolean flag, signalising visibility modifiers presence(true if there is visibility modifier in list, false otherwise).
     * @see #checkModifierDuplication(int, int, TerminalNode)
     */
    public ArrayList<Object> parseModifiers(List<GroovyParser.MemberModifierContext> ctxList, Integer defaultVisibilityModifier) {
        int modifiers = 0;
        boolean hasVisibilityModifier = false;
        for (GroovyParser.MemberModifierContext it : ctxList) {
            TerminalNode child = (DefaultGroovyMethods.asType(it.getChild(0), TerminalNode.class));
            switch (child.getSymbol().getType()) {
                case GroovyLexer.KW_STATIC:
                    modifiers |= checkModifierDuplication(modifiers, Opcodes.ACC_STATIC, child);
                    break;
                case GroovyLexer.KW_ABSTRACT:
                    modifiers |= checkModifierDuplication(modifiers, Opcodes.ACC_ABSTRACT, child);
                    break;
                case GroovyLexer.KW_FINAL:
                    modifiers |= checkModifierDuplication(modifiers, Opcodes.ACC_FINAL, child);
                    break;
                case GroovyLexer.KW_NATIVE:
                    modifiers |= checkModifierDuplication(modifiers, Opcodes.ACC_NATIVE, child);
                    break;
                case GroovyLexer.KW_SYNCHRONIZED:
                    modifiers |= checkModifierDuplication(modifiers, Opcodes.ACC_SYNCHRONIZED, child);
                    break;
                case GroovyLexer.KW_TRANSIENT:
                    modifiers |= checkModifierDuplication(modifiers, Opcodes.ACC_TRANSIENT, child);
                    break;
                case GroovyLexer.KW_VOLATILE:
                    modifiers |= checkModifierDuplication(modifiers, Opcodes.ACC_VOLATILE, child);
                    break;
                case GroovyLexer.VISIBILITY_MODIFIER:
                    modifiers |= parseVisibilityModifiers(child);
                    hasVisibilityModifier = true;
                    break;
            }
        }
        if (!hasVisibilityModifier && defaultVisibilityModifier != null) modifiers |= defaultVisibilityModifier;

        return new ArrayList<Object>(Arrays.asList(modifiers, hasVisibilityModifier));
    }

    /**
     * Traverse through modifiers, and combine them in one int value. Raise an error if there is multiple occurrences of same modifier.
     *
     * @param ctxList                   modifiers list.
     * @return tuple of int modifier and boolean flag, signalising visibility modifiers presence(true if there is visibility modifier in list, false otherwise).
     * @see #checkModifierDuplication(int, int, TerminalNode)
     */
    public ArrayList<Object> parseModifiers(List<GroovyParser.MemberModifierContext> ctxList) {
        return parseModifiers(ctxList, null);
    }

    public void reportError(String text, int line, int col) {
        sourceUnit.addError(new SyntaxException(text, line, col));
    }

    public int parseVisibilityModifiers(TerminalNode modifier) {
        assert modifier.getSymbol().getType() == GroovyLexer.VISIBILITY_MODIFIER;
        if (DefaultGroovyMethods.isCase("public", modifier.getSymbol().getText()))
            return Opcodes.ACC_PUBLIC;
        else if (DefaultGroovyMethods.isCase("private", modifier.getSymbol().getText()))
            return Opcodes.ACC_PRIVATE;
        else if (DefaultGroovyMethods.isCase("protected", modifier.getSymbol().getText()))
            return Opcodes.ACC_PROTECTED;
        else
            throw new AssertionError(modifier.getSymbol().getText() + " is not a valid visibility modifier!");
    }

    public int parseVisibilityModifiers(List<TerminalNode> modifiers, int defaultValue) {
        if (! asBoolean(modifiers)) return defaultValue;

        if (modifiers.size() > 1) {
            Token modifier = modifiers.get(1).getSymbol();

            Integer line = modifier.getLine();
            Integer col = modifier.getCharPositionInLine() + 1;

            reportError("Cannot specify modifier: " + modifier.getText() + " when access scope has already been defined at line: " + String.valueOf(line) + " column: " + String.valueOf(col) + ". File: " + sourceUnit.getName(), line, col);
        }


        return parseVisibilityModifiers(modifiers.get(0));
    }

    /**
     * Method for construct string from string literal handling empty strings.
     *
     * @param node
     * @return
     */
    public String parseString(TerminalNode node) {
        String t = node.getText();
        return asBoolean(t) ? DefaultGroovyMethods.getAt(t, new IntRange(true, 1, -2)) : t;
    }

    public Object initialExpressionForType(ClassNode type) {
        if (ClassHelper.int_TYPE.equals(type))
            return 0;
        else if (ClassHelper.long_TYPE.equals(type))
            return 0L;
        else if (ClassHelper.double_TYPE.equals(type))
            return 0.0;
        else if (ClassHelper.float_TYPE.equals(type))
            return 0f;
        else if (ClassHelper.boolean_TYPE.equals(type))
            return Boolean.FALSE;
        else if (ClassHelper.short_TYPE.equals(type))
            return (short)0;
        else if (ClassHelper.byte_TYPE.equals(type))
            return (byte)0;
        else if (ClassHelper.char_TYPE.equals(type))
            return (char)0;
        else return null;
    }

    private String readSourceCode(SourceUnit sourceUnit) {
        String text = null;
        try {
            text = DefaultGroovyMethods.getText(
                    new BufferedReader(
                            sourceUnit.getSource().getReader()));
        } catch (IOException e) {
            log.severe(createExceptionMessage(e));
            throw new RuntimeException("Error occurred when reading source code.", e);
        }

        return text;
    }


    private void setupErrorListener(GroovyParser parser) {
        parser.removeErrorListeners();
        parser.addErrorListener(new ANTLRErrorListener() {
            @Override
            public void syntaxError(
                    Recognizer<?, ?> recognizer,
                    Object offendingSymbol, int line, int charPositionInLine,
                    String msg, RecognitionException e) {
                sourceUnit.getErrorCollector().addFatalError(new SyntaxErrorMessage(new SyntaxException(msg, line, charPositionInLine+1), sourceUnit));
            }

            @Override
            public void reportAmbiguity(Parser recognizer, DFA dfa, int startIndex, int stopIndex, boolean exact, BitSet ambigAlts, ATNConfigSet configs) {
                log.fine("Ambiguity at " + startIndex + " - " + stopIndex);
            }

            @Override
            public void reportAttemptingFullContext(
                    Parser recognizer,
                    DFA dfa, int startIndex, int stopIndex,
                    BitSet conflictingAlts, ATNConfigSet configs) {
                log.fine("Attempting Full Context at " + startIndex + " - " + stopIndex);
            }

            @Override
            public void reportContextSensitivity(
                    Parser recognizer,
                    DFA dfa, int startIndex, int stopIndex, int prediction, ATNConfigSet configs) {
                log.fine("Context Sensitivity at " + startIndex + " - " + stopIndex);
            }
        });
    }

    private void logTreeStr(GroovyParser.CompilationUnitContext tree) {
        final StringBuilder s = new StringBuilder();
        new ParseTreeWalker().walk(new ParseTreeListener() {
            @Override
            public void visitTerminal(TerminalNode node) {
                s.append(multiply(".\t", indent));
                s.append(String.valueOf(node));
                s.append("\n");
            }

            @Override
            public void visitErrorNode(ErrorNode node) {
            }

            @Override
            public void enterEveryRule(final ParserRuleContext ctx) {
                s.append(multiply(".\t", indent));
                s.append(GroovyParser.ruleNames[ctx.getRuleIndex()] + ": {");
                s.append("\n");
                indent = indent++;
            }

            @Override
            public void exitEveryRule(ParserRuleContext ctx) {
                indent = indent--;
                s.append(multiply(".\t", indent));
                s.append("}");
                s.append("\n");
            }

            public int getIndent() {
                return indent;
            }

            public void setIndent(int indent) {
                this.indent = indent;
            }

            private int indent;
        }, tree);

        log.fine((multiply("=", 60)) + "\n" + String.valueOf(s) + "\n" + (multiply("=", 60)));
    }

    private void logTokens(String text) {
        final GroovyLexer lexer = new GroovyLexer(new ANTLRInputStream(text));
        log.fine(multiply("=", 60) + "\n" + text + "\n" + multiply("=", 60));
        log.fine("\nLexer TOKENS:\n\t" + DefaultGroovyMethods.join(collect(lexer.getAllTokens(), new Closure<String>(this, this) {
            public String doCall(Token it) { return String.valueOf(it.getLine()) + ", " + String.valueOf(it.getStartIndex()) + ":" + String.valueOf(it.getStopIndex()) + " " + GroovyLexer.tokenNames[it.getType()] + " " + it.getText(); }
        }), "\n\t") + multiply("=", 60));
    }

    private CompilationFailedException createParsingFailedException(Throwable cause) {
        return new CompilationFailedException(CompilePhase.PARSING.getPhaseNumber(), this.sourceUnit, cause);
    }

    private String createExceptionMessage(Throwable t) {
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);

        try {
            t.printStackTrace(pw);
        } finally {
            pw.close();
        }

        return sw.toString();
    }

    public ModuleNode getModuleNode() {
        return moduleNode;
    }

    public void setModuleNode(ModuleNode moduleNode) {
        this.moduleNode = moduleNode;
    }

    private ModuleNode moduleNode;
    private SourceUnit sourceUnit;
    private ClassLoader classLoader;
    private Stack<ClassNode> classes = new Stack<ClassNode>();
    private Stack<List<InnerClassNode>> innerClassesDefinedInMethod = new Stack<List<InnerClassNode>>();
    private int anonymousClassesCount = 0;
    private Logger log = Logger.getLogger(ASTBuilder.class.getName());
}
