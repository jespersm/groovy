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
package org.codehaus.groovy.parser.antlr4.builders;

import groovy.lang.Closure;
import groovy.lang.IntRange;
import org.antlr.v4.runtime.*;
import org.antlr.v4.runtime.atn.ATNConfigSet;
import org.antlr.v4.runtime.dfa.DFA;
import org.antlr.v4.runtime.tree.*;
import org.codehaus.groovy.GroovyBugError;
import org.codehaus.groovy.antlr.EnumHelper;
import org.codehaus.groovy.ast.*;
import org.codehaus.groovy.ast.expr.*;
import org.codehaus.groovy.ast.stmt.BlockStatement;
import org.codehaus.groovy.ast.stmt.ExpressionStatement;
import org.codehaus.groovy.ast.stmt.ReturnStatement;
import org.codehaus.groovy.ast.stmt.Statement;
import org.codehaus.groovy.control.CompilationFailedException;
import org.codehaus.groovy.control.CompilePhase;
import org.codehaus.groovy.control.SourceUnit;
import org.codehaus.groovy.control.messages.SyntaxErrorMessage;
import org.codehaus.groovy.parser.antlr4.GroovyLexer;
import org.codehaus.groovy.parser.antlr4.GroovyParser;
import org.codehaus.groovy.parser.antlr4.InvalidSyntaxException;
import org.codehaus.groovy.parser.antlr4.builders.nodes.Buildable;
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
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

import static org.codehaus.groovy.runtime.DefaultGroovyMethods.*;

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

        GroovyLexer lexer = new GroovyLexer(new ANTLRInputStream(text));
        GroovyParser parser = new GroovyParser(new CommonTokenStream(lexer));


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
            for (GroovyParser.ImportStatementContext importStatementContext : tree.importStatement()) {
                createBuildableNode(importStatementContext).build();
            }

            for (ParseTree it : tree.children) {
                if (it instanceof GroovyParser.ClassDeclarationContext)
                    parseClassDeclaration((GroovyParser.ClassDeclarationContext)it);
                else if (it instanceof GroovyParser.PackageDefinitionContext)
                    parsePackageDefinition((GroovyParser.PackageDefinitionContext)it);
            }

            for (GroovyParser.ScriptPartContext part : tree.scriptPart()) {
                if (part.statement() != null) {
                    unpackStatement(moduleNode, parseStatement(part.statement()));
                } else {
                    moduleNode.addMethod(parseScriptMethod(part.methodDeclaration()));
                }
            }
        } catch (CompilationFailedException e) {
            // Compilation failed.
            log.severe(createExceptionMessage(e));
            throw e;
        }
    }

    public void parsePackageDefinition( GroovyParser.PackageDefinitionContext ctx) {
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

    public void unpackStatement(BlockStatement destination, Statement stmt) {
        if (stmt instanceof DeclarationList) {
            for (DeclarationExpression decl : ((DeclarationList)stmt).declarations) {
                destination.addStatement(setupNodeLocation(new ExpressionStatement(decl), decl));
            }
        } else {
            destination.addStatement(stmt);
        }
    }

    /**
     *
     * @param isAnnotationDeclaration   whether the method is defined in an annotation
     * @param hasAnnotation             whether the method declaration has annotations
     * @param hasVisibilityModifier     whether the method declaration contains visibility modifier(e.g. public, protected, private)
     * @param hasModifier               whether the method declaration has modifier(e.g. visibility modifier, final, static and so on)
     * @param hasReturnType             whether the method declaration has an return type(e.g. String, generic types)
     * @return                          the result
     *
     */
    private boolean isSyntheticPublic(boolean isAnnotationDeclaration, boolean hasAnnotation, boolean hasVisibilityModifier, boolean hasModifier, boolean hasReturnType) {

        if (hasVisibilityModifier) {
            return false;
        }

        if (isAnnotationDeclaration) {
            return true;
        }

        if (hasModifier || hasAnnotation || !hasReturnType) {
            return true;
        }

        return false;
    }

    public MethodNode parseMethodDeclaration(ClassNode classNode, GroovyParser.MethodDeclarationContext ctx, Closure<MethodNode> createMethodNode) {
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

        MethodNode methodNode = createMethodNode.call(classNode, ctx, methodName, modifiers, returnType, params, exceptions, statement, innerClassesDeclared);

        setupNodeLocation(methodNode, ctx);
        attachAnnotations(methodNode, ctx.annotationClause());
        methodNode.setSyntheticPublic(isSyntheticPublic(isAnnotationDeclaration, hasAnnotation, hasVisibilityModifier, hasModifier, hasReturnType));
        methodNode.setSynthetic(false); // user-defined method are not synthetic

        return methodNode;
    }

    public MethodNode parseScriptMethod(final GroovyParser.MethodDeclarationContext ctx) {
        return (MethodNode) createBuildableNode(ctx).build();
    }

    public ClassNode parseClassDeclaration(final GroovyParser.ClassDeclarationContext ctx) {
        return (ClassNode) createBuildableNode(ctx).build();
    }

    public void parseClassBody( ClassNode classNode, GroovyParser.ClassBodyContext ctx) {
        for(TerminalNode node : ctx.IDENTIFIER()) {
            setupNodeLocation(EnumHelper.addEnumConstant(classNode, node.getText(), null), node.getSymbol());
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
                throw new CompilationFailedException(CompilePhase.PARSING.getPhaseNumber(), this.sourceUnit,
                                new InvalidSyntaxException("You defined a method without body. Try adding a body, or declare it abstract.", ctx));
            }
        }

        return parseMethodDeclaration(classNode, ctx, new Closure<MethodNode>(this, this) {
                    public MethodNode doCall(ClassNode classNode, GroovyParser.MethodDeclarationContext ctx, String methodName, int modifiers, ClassNode returnType, Parameter[] params, ClassNode[] exceptions, Statement statement, List<InnerClassNode> innerClassesDeclared) {
                        modifiers |= classNode.isInterface() ? Opcodes.ACC_ABSTRACT : 0;

                        if (ctx.KW_DEFAULT() != null) {
                            statement = new ExpressionStatement(parseExpression(ctx.annotationParameter()));
                        }

                        final MethodNode methodNode = classNode.addMethod(methodName, modifiers, returnType, params, exceptions, statement);
                        methodNode.setGenericsTypes(parseGenericDeclaration(ctx.genericDeclarationList()));
                        DefaultGroovyMethods.each(innerClassesDeclared, new Closure<MethodNode>(this, this) {
                            public MethodNode doCall(InnerClassNode it) {
                                it.setEnclosingMethod(methodNode);
                                return methodNode;
                            }
                        });

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
        this.innerClassesDefinedInMethod.add(new ArrayList());
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

    public Statement parseStatement(GroovyParser.StatementContext ctx) {
        return (Statement) createBuildableNode(ctx).build();
    }

    public Statement parseStatement(GroovyParser.BlockStatementContext ctx) {
        return (Statement) createBuildableNode(ctx).build(new BlockStatement());
    }

    public Statement parse(GroovyParser.StatementBlockContext ctx) {
        if (asBoolean(ctx.statement()))
            return setupNodeLocation(parseStatement(ctx.statement()), ctx.statement());
        else return parseStatement(ctx.blockStatement());
    }

    /**
     * Parse path expression.
     *
     * @param ctx
     * @return tuple of 3 values: Expression, String methodName and boolean implicitThis flag.
     */
    public ArrayList<Object> parsePathExpression(GroovyParser.PathExpressionContext ctx) {
        Expression expression = (Expression) createBuildableNode(ctx).build();

        List<TerminalNode> identifiers = ctx.IDENTIFIER();
        return new ArrayList<Object>(Arrays.asList(expression, DefaultGroovyMethods.last(identifiers).getSymbol().getText(), identifiers.size() == 1));
    }

    public Expression parseExpression(GroovyParser.ExpressionContext ctx) {
        return (Expression) createBuildableNode(ctx).build();
    }

    public MapEntryExpression parseExpression(GroovyParser.MapEntryContext ctx) {
        return (MapEntryExpression) createBuildableNode(ctx).build();
    }

    public Expression parseExpression(GroovyParser.ClosureExpressionRuleContext ctx) {
        return (Expression) createBuildableNode(ctx).build();
    }


    public Expression parseExpression(GroovyParser.AnnotationParameterContext ctx) {
        return (Expression) createBuildableNode(ctx).build();
    }

    public ConstantExpression parseDecimal(String text, ParserRuleContext ctx) {
        return setupNodeLocation(new ConstantExpression(Numbers.parseDecimal(text), !text.startsWith("-")), ctx);// Why 10 is int but -10 is Integer?
    }

    public ConstantExpression parseInteger(String text, ParserRuleContext ctx) {
        return setupNodeLocation(new ConstantExpression(Numbers.parseInteger(text), !text.startsWith("-")), ctx);//Why 10 is int but -10 is Integer?
    }

    public ConstantExpression parseInteger(String text, Token ctx) {
        return setupNodeLocation(new ConstantExpression(Numbers.parseInteger(text), !text.startsWith("-")), ctx);//Why 10 is int but -10 is Integer?
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

    public Expression parseExpression(GroovyParser.GstringContext ctx) {
        return (Expression) createBuildableNode(ctx).build();
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

    public ClassNode parseExpression(GroovyParser.ClassNameExpressionContext ctx) {
        return setupNodeLocation(ClassHelper.make(DefaultGroovyMethods.join(ctx.IDENTIFIER(), ".")), ctx);
    }

    public ClassNode parseExpression(GroovyParser.GenericClassNameExpressionContext ctx) {
        return (ClassNode) createBuildableNode(ctx).build();
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
            DeclarationExpression declarationExpression = parseTupleDeclaration(ctx.tupleDeclaration());

            declarations.add(declarationExpression);

            return declarations;
        }

        for (GroovyParser.SingleDeclarationContext variableCtx : variables) {
            VariableExpression left = new VariableExpression(variableCtx.IDENTIFIER().getText(), type);

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

    public org.codehaus.groovy.syntax.Token createGroovyToken(Token token, int type) {
        if (null == token) {
            throw new IllegalArgumentException("token should not be null");
        }

        return new org.codehaus.groovy.syntax.Token(type, token.getText(), token.getLine(), token.getCharPositionInLine());
    }

    public VariableExpression parseTupleVariableDeclaration(GroovyParser.TupleVariableDeclarationContext ctx) {
        return (VariableExpression) createBuildableNode(ctx).build();
    }

    public DeclarationExpression parseTupleDeclaration(GroovyParser.TupleDeclarationContext ctx) {
        return (DeclarationExpression) createBuildableNode(ctx).build();
    }

    public Expression createArgumentList(GroovyParser.ArgumentListContext ctx) {
        return (Expression) createBuildableNode(ctx).build(new ArgumentListExpression());
    }

    public void attachAnnotations(AnnotatedNode node, List<GroovyParser.AnnotationClauseContext> ctxs) {
        for (GroovyParser.AnnotationClauseContext ctx : ctxs) {
            AnnotationNode annotation = parseAnnotation(ctx);
            node.addAnnotation(annotation);
        }
    }

    public void attachTraitTransformAnnotation(ClassNode classNode) {
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
        return (ConstructorCallExpression) createBuildableNode(ctx).build();
    }

    public Parameter[] parseParameters(GroovyParser.ArgumentDeclarationListContext ctx) {
        List<Parameter> parameterList = ctx == null || ctx.argumentDeclaration() == null ?
            new ArrayList<Parameter>(0) :
            collect(ctx.argumentDeclaration(), new Closure<Parameter>(null, null) {
                public Parameter doCall(GroovyParser.ArgumentDeclarationContext it) {
                    Parameter parameter = new Parameter(parseTypeDeclaration(it.typeDeclaration()), it.IDENTIFIER().getText());
                    attachAnnotations(parameter, it.annotationClause());
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
        astNode.setLineNumber(ctx.getStart().getLine());
        astNode.setColumnNumber(ctx.getStart().getCharPositionInLine() + 1);
        astNode.setLastLineNumber(ctx.getStop().getLine());
        astNode.setLastColumnNumber(ctx.getStop().getCharPositionInLine() + 1 + ctx.getStop().getText().length());
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

    public int parseClassModifiers( List<GroovyParser.ClassModifierContext> ctxs) {
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
            public void reportAmbiguity( Parser recognizer,  DFA dfa, int startIndex, int stopIndex, boolean exact, BitSet ambigAlts,  ATNConfigSet configs) {
                log.fine("Ambiguity at " + startIndex + " - " + stopIndex);
            }

            @Override
            public void reportAttemptingFullContext(
                     Parser recognizer,
                     DFA dfa, int startIndex, int stopIndex,
                    BitSet conflictingAlts,  ATNConfigSet configs) {
                log.fine("Attempting Full Context at " + startIndex + " - " + stopIndex);
            }

            @Override
            public void reportContextSensitivity(
                     Parser recognizer,
                     DFA dfa, int startIndex, int stopIndex, int prediction,  ATNConfigSet configs) {
                log.fine("Context Sensitivity at " + startIndex + " - " + stopIndex);
            }
        });
    }

    private void logTreeStr(GroovyParser.CompilationUnitContext tree) {
        final StringBuilder s = new StringBuilder();
        new ParseTreeWalker().walk(new ParseTreeListener() {
            @Override
            public void visitTerminal( TerminalNode node) {
                s.append(multiply(".\t", indent));
                s.append(String.valueOf(node));
                s.append("\n");
            }

            @Override
            public void visitErrorNode( ErrorNode node) {
            }

            @Override
            public void enterEveryRule( final ParserRuleContext ctx) {
                s.append(multiply(".\t", indent));
                s.append(GroovyParser.ruleNames[ctx.getRuleIndex()] + ": {");
                s.append("\n");
                indent = indent++;
            }

            @Override
            public void exitEveryRule( ParserRuleContext ctx) {
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

    public String createExceptionMessage(Throwable t) {
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);

        try {
            t.printStackTrace(pw);
        } finally {
            pw.close();
        }

        return sw.toString();
    }

    public final Buildable createBuildableNode(ParserRuleContext ctx) {
        String buildableClazzName;

        if (null == ctx) {
            buildableClazzName = "org.codehaus.groovy.parser.antlr4.builders.nodes.BuildableNullNode";
        } else {
            buildableClazzName = "org.codehaus.groovy.parser.antlr4.builders.nodes.Buildable" + ctx.getClass().getSimpleName();
        }

        Class clazz = null;

        try {
            clazz = Class.forName(buildableClazzName);
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        }

        Constructor<?>[] constructors = clazz.getConstructors();

        if (constructors.length > 0) {
            try {
                return (Buildable) constructors[0].newInstance(this, ctx);
            } catch (InstantiationException e) {
                throw new RuntimeException(e);
            } catch (IllegalAccessException e) {
                throw new RuntimeException(e);
            } catch (InvocationTargetException e) {
                throw new RuntimeException(e);
            }
        } else {
            throw new RuntimeException(buildableClazzName + " does not have constructors");
        }
    }

    public ModuleNode getModuleNode() {
        return moduleNode;
    }

    public void setModuleNode(ModuleNode moduleNode) {
        this.moduleNode = moduleNode;
    }

    public ModuleNode moduleNode;
    public SourceUnit sourceUnit;
    private ClassLoader classLoader;
    public Stack<ClassNode> classes = new Stack<ClassNode>();
    public Stack<List<InnerClassNode>> innerClassesDefinedInMethod = new Stack<List<InnerClassNode>>();
    public int anonymousClassesCount = 0;
    private Logger log = Logger.getLogger(ASTBuilder.class.getName());

    public static class DeclarationList extends Statement {
        List<DeclarationExpression> declarations;

        public DeclarationList(List<DeclarationExpression> declarations) {
            this.declarations = declarations;
        }
    }
}
