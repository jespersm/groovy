package org.codehaus.groovy.parser.antlr4

import org.antlr.v4.runtime.ANTLRErrorListener
import org.antlr.v4.runtime.Parser
import org.antlr.v4.runtime.RecognitionException
import org.antlr.v4.runtime.Recognizer
import org.antlr.v4.runtime.atn.ATNConfigSet
import org.antlr.v4.runtime.dfa.DFA
import org.antlr.v4.runtime.misc.Nullable
import org.codehaus.groovy.ast.expr.MethodPointerExpression
import org.codehaus.groovy.control.messages.SyntaxErrorMessage
import org.codehaus.groovy.parser.antlr4.GroovyParser.AnnotationClauseContext
import org.codehaus.groovy.parser.antlr4.GroovyParser.AnnotationElementContext
import org.codehaus.groovy.parser.antlr4.GroovyParser.AnnotationParamArrayExpressionContext
import org.codehaus.groovy.parser.antlr4.GroovyParser.AnnotationParamBoolExpressionContext
import org.codehaus.groovy.parser.antlr4.GroovyParser.AnnotationParamClassExpressionContext
import org.codehaus.groovy.parser.antlr4.GroovyParser.AnnotationParamDecimalExpressionContext
import org.codehaus.groovy.parser.antlr4.GroovyParser.AnnotationParamIntegerExpressionContext
import org.codehaus.groovy.parser.antlr4.GroovyParser.AnnotationParamNullExpressionContext
import org.codehaus.groovy.parser.antlr4.GroovyParser.AnnotationParamPathExpressionContext
import org.codehaus.groovy.parser.antlr4.GroovyParser.AnnotationParamStringExpressionContext
import org.codehaus.groovy.parser.antlr4.GroovyParser.AnnotationParameterContext
import org.codehaus.groovy.parser.antlr4.GroovyParser.ArgumentDeclarationListContext
import org.codehaus.groovy.parser.antlr4.GroovyParser.ArgumentListContext
import org.codehaus.groovy.parser.antlr4.GroovyParser.AssignmentExpressionContext
import org.codehaus.groovy.parser.antlr4.GroovyParser.BinaryExpressionContext
import org.codehaus.groovy.parser.antlr4.GroovyParser.BlockStatementContext
import org.codehaus.groovy.parser.antlr4.GroovyParser.BoolExpressionContext
import org.codehaus.groovy.parser.antlr4.GroovyParser.CallExpressionContext
import org.codehaus.groovy.parser.antlr4.GroovyParser.ClassDeclarationContext
import org.codehaus.groovy.parser.antlr4.GroovyParser.ClassInitializerContext
import org.codehaus.groovy.parser.antlr4.GroovyParser.ClassMemberContext
import org.codehaus.groovy.parser.antlr4.GroovyParser.ClassModifierContext
import org.codehaus.groovy.parser.antlr4.GroovyParser.ClassNameExpressionContext
import org.codehaus.groovy.parser.antlr4.GroovyParser.ClassicForStatementContext
import org.codehaus.groovy.parser.antlr4.GroovyParser.ClosureExpressionContext
import org.codehaus.groovy.parser.antlr4.GroovyParser.ClosureExpressionRuleContext
import org.codehaus.groovy.parser.antlr4.GroovyParser.CommandExpressionStatementContext
import org.codehaus.groovy.parser.antlr4.GroovyParser.ConstantDecimalExpressionContext
import org.codehaus.groovy.parser.antlr4.GroovyParser.ConstantExpressionContext
import org.codehaus.groovy.parser.antlr4.GroovyParser.ConstantIntegerExpressionContext
import org.codehaus.groovy.parser.antlr4.GroovyParser.ConstructorDeclarationContext
import org.codehaus.groovy.parser.antlr4.GroovyParser.ControlStatementContext
import org.codehaus.groovy.parser.antlr4.GroovyParser.DeclarationExpressionContext
import org.codehaus.groovy.parser.antlr4.GroovyParser.DeclarationRuleContext
import org.codehaus.groovy.parser.antlr4.GroovyParser.DeclarationStatementContext
import org.codehaus.groovy.parser.antlr4.GroovyParser.ElvisExpressionContext
import org.codehaus.groovy.parser.antlr4.GroovyParser.EnumDeclarationContext
import org.codehaus.groovy.parser.antlr4.GroovyParser.EnumMemberContext
import org.codehaus.groovy.parser.antlr4.GroovyParser.ExpressionContext
import org.codehaus.groovy.parser.antlr4.GroovyParser.ExpressionStatementContext
import org.codehaus.groovy.parser.antlr4.GroovyParser.FieldAccessExpressionContext
import org.codehaus.groovy.parser.antlr4.GroovyParser.FieldDeclarationContext
import org.codehaus.groovy.parser.antlr4.GroovyParser.ForColonStatementContext
import org.codehaus.groovy.parser.antlr4.GroovyParser.ForInStatementContext
import org.codehaus.groovy.parser.antlr4.GroovyParser.GenericClassNameExpressionContext
import org.codehaus.groovy.parser.antlr4.GroovyParser.GenericDeclarationListContext
import org.codehaus.groovy.parser.antlr4.GroovyParser.GenericListContext
import org.codehaus.groovy.parser.antlr4.GroovyParser.GenericsConcreteElementContext
import org.codehaus.groovy.parser.antlr4.GroovyParser.GenericsWildcardElementContext
import org.codehaus.groovy.parser.antlr4.GroovyParser.GstringContext
import org.codehaus.groovy.parser.antlr4.GroovyParser.GstringExpressionContext
import org.codehaus.groovy.parser.antlr4.GroovyParser.GstringPathExpressionContext
import org.codehaus.groovy.parser.antlr4.GroovyParser.IfStatementContext
import org.codehaus.groovy.parser.antlr4.GroovyParser.ImportStatementContext
import org.codehaus.groovy.parser.antlr4.GroovyParser.ListConstructorContext
import org.codehaus.groovy.parser.antlr4.GroovyParser.MapConstructorContext
import org.codehaus.groovy.parser.antlr4.GroovyParser.MapEntryContext
import org.codehaus.groovy.parser.antlr4.GroovyParser.MemberModifierContext
import org.codehaus.groovy.parser.antlr4.GroovyParser.MethodCallExpressionContext
import org.codehaus.groovy.parser.antlr4.GroovyParser.MethodDeclarationContext
import org.codehaus.groovy.parser.antlr4.GroovyParser.NewArrayExpressionContext
import org.codehaus.groovy.parser.antlr4.GroovyParser.NewArrayRuleContext
import org.codehaus.groovy.parser.antlr4.GroovyParser.NewArrayStatementContext
import org.codehaus.groovy.parser.antlr4.GroovyParser.NewInstanceExpressionContext
import org.codehaus.groovy.parser.antlr4.GroovyParser.NewInstanceRuleContext
import org.codehaus.groovy.parser.antlr4.GroovyParser.NewInstanceStatementContext
import org.codehaus.groovy.parser.antlr4.GroovyParser.NullExpressionContext
import org.codehaus.groovy.parser.antlr4.GroovyParser.ObjectInitializerContext
import org.codehaus.groovy.parser.antlr4.GroovyParser.PackageDefinitionContext
import org.codehaus.groovy.parser.antlr4.GroovyParser.ParenthesisExpressionContext
import org.codehaus.groovy.parser.antlr4.GroovyParser.PathExpressionContext
import org.codehaus.groovy.parser.antlr4.GroovyParser.PostfixExpressionContext
import org.codehaus.groovy.parser.antlr4.GroovyParser.PrefixExpressionContext
import org.codehaus.groovy.parser.antlr4.GroovyParser.ReturnStatementContext
import org.codehaus.groovy.parser.antlr4.GroovyParser.StatementBlockContext
import org.codehaus.groovy.parser.antlr4.GroovyParser.StatementContext
import org.codehaus.groovy.parser.antlr4.GroovyParser.SwitchStatementContext
import org.codehaus.groovy.parser.antlr4.GroovyParser.TernaryExpressionContext
import org.codehaus.groovy.parser.antlr4.GroovyParser.ThrowStatementContext
import org.codehaus.groovy.parser.antlr4.GroovyParser.ThrowsClauseContext
import org.codehaus.groovy.parser.antlr4.GroovyParser.TryCatchFinallyStatementContext
import org.codehaus.groovy.parser.antlr4.GroovyParser.TypeDeclarationContext
import org.codehaus.groovy.parser.antlr4.GroovyParser.UnaryExpressionContext
import org.codehaus.groovy.parser.antlr4.GroovyParser.VariableExpressionContext
import org.codehaus.groovy.parser.antlr4.GroovyParser.WhileStatementContext
import org.codehaus.groovy.parser.antlr4.util.StringUtil
import groovy.util.logging.Log
import org.objectweb.asm.Opcodes
import org.antlr.v4.runtime.ANTLRInputStream
import org.antlr.v4.runtime.CommonTokenStream
import org.antlr.v4.runtime.ParserRuleContext
import org.antlr.v4.runtime.misc.NotNull
import org.antlr.v4.runtime.tree.ErrorNode
import org.antlr.v4.runtime.tree.ParseTree
import org.antlr.v4.runtime.tree.ParseTreeListener
import org.antlr.v4.runtime.tree.ParseTreeWalker
import org.antlr.v4.runtime.tree.TerminalNode
import org.codehaus.groovy.antlr.EnumHelper
import org.codehaus.groovy.ast.ASTNode
import org.codehaus.groovy.ast.AnnotatedNode
import org.codehaus.groovy.ast.AnnotationNode
import org.codehaus.groovy.ast.ClassHelper
import org.codehaus.groovy.ast.ClassNode
import org.codehaus.groovy.ast.GenericsType
import org.codehaus.groovy.ast.ImportNode
import org.codehaus.groovy.ast.InnerClassNode
import org.codehaus.groovy.ast.MethodNode
import org.codehaus.groovy.ast.ModuleNode
import org.codehaus.groovy.ast.Parameter
import org.codehaus.groovy.ast.expr.AnnotationConstantExpression
import org.codehaus.groovy.ast.expr.ArgumentListExpression
import org.codehaus.groovy.ast.expr.ArrayExpression
import org.codehaus.groovy.ast.expr.AttributeExpression
import org.codehaus.groovy.ast.expr.BinaryExpression
import org.codehaus.groovy.ast.expr.BitwiseNegationExpression
import org.codehaus.groovy.ast.expr.BooleanExpression
import org.codehaus.groovy.ast.expr.CastExpression
import org.codehaus.groovy.ast.expr.ClassExpression
import org.codehaus.groovy.ast.expr.ClosureExpression
import org.codehaus.groovy.ast.expr.ClosureListExpression
import org.codehaus.groovy.ast.expr.ConstantExpression
import org.codehaus.groovy.ast.expr.ConstructorCallExpression
import org.codehaus.groovy.ast.expr.DeclarationExpression
import org.codehaus.groovy.ast.expr.ElvisOperatorExpression
import org.codehaus.groovy.ast.expr.EmptyExpression
import org.codehaus.groovy.ast.expr.Expression
import org.codehaus.groovy.ast.expr.GStringExpression
import org.codehaus.groovy.ast.expr.ListExpression
import org.codehaus.groovy.ast.expr.MapEntryExpression
import org.codehaus.groovy.ast.expr.MapExpression
import org.codehaus.groovy.ast.expr.MethodCallExpression
import org.codehaus.groovy.ast.expr.NamedArgumentListExpression
import org.codehaus.groovy.ast.expr.NotExpression
import org.codehaus.groovy.ast.expr.PostfixExpression
import org.codehaus.groovy.ast.expr.PrefixExpression
import org.codehaus.groovy.ast.expr.PropertyExpression
import org.codehaus.groovy.ast.expr.RangeExpression
import org.codehaus.groovy.ast.expr.TernaryExpression
import org.codehaus.groovy.ast.expr.TupleExpression
import org.codehaus.groovy.ast.expr.UnaryMinusExpression
import org.codehaus.groovy.ast.expr.UnaryPlusExpression
import org.codehaus.groovy.ast.expr.VariableExpression
import org.codehaus.groovy.ast.stmt.BlockStatement
import org.codehaus.groovy.ast.stmt.BreakStatement
import org.codehaus.groovy.ast.stmt.CaseStatement
import org.codehaus.groovy.ast.stmt.CatchStatement
import org.codehaus.groovy.ast.stmt.ContinueStatement
import org.codehaus.groovy.ast.stmt.EmptyStatement
import org.codehaus.groovy.ast.stmt.ExpressionStatement
import org.codehaus.groovy.ast.stmt.ForStatement
import org.codehaus.groovy.ast.stmt.IfStatement
import org.codehaus.groovy.ast.stmt.ReturnStatement
import org.codehaus.groovy.ast.stmt.Statement
import org.codehaus.groovy.ast.stmt.SwitchStatement
import org.codehaus.groovy.ast.stmt.ThrowStatement
import org.codehaus.groovy.ast.stmt.TryCatchStatement
import org.codehaus.groovy.ast.stmt.WhileStatement
import org.codehaus.groovy.control.CompilationFailedException
import org.codehaus.groovy.control.CompilePhase
import org.codehaus.groovy.control.SourceUnit
import org.codehaus.groovy.syntax.Numbers
import org.codehaus.groovy.syntax.SyntaxException
import org.codehaus.groovy.syntax.Token
import org.codehaus.groovy.syntax.Types

import java.lang.reflect.Modifier
import java.util.logging.Level

@Log
class ASTBuilder {
    ModuleNode moduleNode

    private SourceUnit sourceUnit
    private ClassLoader classLoader

    // This fields are weird.
    private Stack<ClassNode> classes = [] // FIXME Dirty hack for inner classes. Remove ASAP.
    private Stack<List<InnerClassNode>> innerClassesDefinedInMethod = [] // --
    private int anonymousClassesCount = 0 // Used for create name for Counts anonimous classes

    ASTBuilder(SourceUnit sourceUnit, ClassLoader classLoader) {
        this.classLoader = classLoader
        this.sourceUnit = sourceUnit
        moduleNode = new ModuleNode(sourceUnit)

        def text = StringUtil.replaceHexEscapes(sourceUnit.source.reader.text)

        if (log.isLoggable(Level.FINE)) {
            def lexer = new GroovyLexer(new ANTLRInputStream(text))
            log.fine("${ "=" * 60 }\n$text\n${ "=" * 60 }")
            log.fine("\nLexer TOKENS:\n\t${ lexer.allTokens.collect { "$it.line, $it.startIndex:$it.stopIndex ${ GroovyLexer.tokenNames[it.type] } $it.text" }.join('\n\t') }${ "=" * 60 }")
        }

        def lexer = new GroovyLexer(new ANTLRInputStream(text))
        CommonTokenStream tokens = new CommonTokenStream(lexer)

        def parser = new GroovyParser(tokens)
        parser.removeErrorListeners()
        parser.addErrorListener(new ANTLRErrorListener() {
            @Override
            void syntaxError(
                @NotNull Recognizer<?, ?> recognizer,
                @Nullable Object offendingSymbol, int line, int charPositionInLine,
                @NotNull String msg, @Nullable RecognitionException e) {
                sourceUnit.errorCollector.addFatalError(new SyntaxErrorMessage(new SyntaxException(msg, line, charPositionInLine+1), sourceUnit))
            }

            @Override
            void reportAmbiguity(
                @NotNull Parser recognizer,
                @NotNull DFA dfa, int startIndex, int stopIndex, boolean exact,
                @Nullable BitSet ambigAlts, @NotNull ATNConfigSet configs) {
                log.fine("Ambiguity at $startIndex - $stopIndex")
            }

            @Override
            void reportAttemptingFullContext(
                @NotNull Parser recognizer,
                @NotNull DFA dfa, int startIndex, int stopIndex,
                @Nullable BitSet conflictingAlts, @NotNull ATNConfigSet configs) {
                log.fine("Attempting Full Context at $startIndex - $stopIndex")
            }

            @Override
            void reportContextSensitivity(
                @NotNull Parser recognizer,
                @NotNull DFA dfa, int startIndex, int stopIndex, int prediction, @NotNull ATNConfigSet configs) {
                log.fine("Context Sensitivity at $startIndex - $stopIndex")
            }
        })
        ParseTree tree = parser.compilationUnit()
        if (log.isLoggable(Level.FINE)) {
            def s = "" << ""
            new ParseTreeWalker().walk(new ParseTreeListener() {
                int indent
                @Override void visitTerminal(@NotNull TerminalNode node) {
                    s << ('.\t' * indent + "$node") << '\n'
                }

                @Override void visitErrorNode(@NotNull ErrorNode node) {
                }

                @Override void enterEveryRule(@NotNull ParserRuleContext ctx) {
                    s << ('.\t' * indent + "${GroovyParser.ruleNames[ctx.ruleIndex]}: {") << '\n'
                    indent++
                }

                @Override void exitEveryRule(@NotNull ParserRuleContext ctx) {
                    indent--
                    s << ('.\t' * indent + "}") << '\n'
                }
            }, tree)

            log.fine(("=" * 60) + "\n$s\n" + ("=" * 60))
        }

        try {
            tree.importStatement().each this.&parseImportStatement
            tree.children.each {
                if (it instanceof EnumDeclarationContext)
                    parseEnumDeclaration(it)
                else if (it instanceof ClassDeclarationContext)
                    parseClassDeclaration(it)
                else if (it instanceof PackageDefinitionContext)
                    parsePackageDefinition(it)
            }
            tree.statement().collect {
                moduleNode.addStatement(parseStatement(it))
            }
        }
        catch (CompilationFailedException ignored) {
            // Compilation failed.
        }
    }

    void parseImportStatement(@NotNull ImportStatementContext ctx) {
        ImportNode node
        if (ctx.getChild(ctx.childCount - 1).text == '*') {
            moduleNode.addStarImport(ctx.IDENTIFIER().join('.') + '.')
            node = moduleNode.starImports.last()
        }
        else {
            moduleNode.addImport(ctx.IDENTIFIER()[-1].text, ClassHelper.make(ctx.IDENTIFIER().join('.')), parseAnnotations(ctx.annotationClause()) )
            node = moduleNode.imports.last()
            setupNodeLocation(node.type, ctx)
        }
        setupNodeLocation(node, ctx)
    }

    void parsePackageDefinition(@NotNull PackageDefinitionContext ctx) {
        moduleNode.packageName = ctx.IDENTIFIER().join('.') + '.'
        attachAnnotations(moduleNode.package, ctx.annotationClause())
        setupNodeLocation(moduleNode.package, ctx)
    }

    void parseEnumDeclaration(@NotNull EnumDeclarationContext ctx) {
        ClassNode[] interfaces = ctx.implementsClause() ? ctx.implementsClause().genericClassNameExpression().collect { parseExpression(it) } : []
        def classNode = EnumHelper.makeEnumNode(ctx.IDENTIFIER().text, Modifier.PUBLIC, interfaces, null) // FIXME merge with class declaration.
        setupNodeLocation(classNode, ctx)
        attachAnnotations(classNode, ctx.annotationClause())
        moduleNode.addClass(classNode)

        classNode.modifiers = parseClassModifiers(ctx.classModifier()) | Opcodes.ACC_ENUM | Opcodes.ACC_FINAL
        classNode.syntheticPublic = (classNode.modifiers & Opcodes.ACC_SYNTHETIC) != 0
        classNode.modifiers &= ~Opcodes.ACC_SYNTHETIC // FIXME Magic with synthetic modifier.

        def enumConstants = ctx.enumMember().grep { EnumMemberContext e -> e.IDENTIFIER() }.collect { it.IDENTIFIER() }
        def classMembers = ctx.enumMember().grep { EnumMemberContext e -> e.classMember() }.collect { it.classMember() }
        enumConstants.each {
            setupNodeLocation(EnumHelper.addEnumConstant(classNode, it.text, null), it.symbol)
        }
        parseMembers(classNode,  classMembers)
    }

    ClassNode parseClassDeclaration(@NotNull ClassDeclarationContext ctx) {
        def classNode
        def parentClass = classes ? classes.peek() : null
        if (parentClass)
            classNode = new InnerClassNode(parentClass, "${"$parentClass.name\$" ?: ""}${ctx.IDENTIFIER()}", Modifier.PUBLIC, ClassHelper.OBJECT_TYPE)
        else
            classNode = new ClassNode("${moduleNode.packageName ?: ""}${ctx.IDENTIFIER()}", Modifier.PUBLIC, ClassHelper.OBJECT_TYPE)

        setupNodeLocation(classNode, ctx)
        attachAnnotations(classNode, ctx.annotationClause())
        moduleNode.addClass(classNode)
        if (ctx.extendsClause())
            classNode.setSuperClass(parseExpression(ctx.extendsClause().genericClassNameExpression()))
        if (ctx.implementsClause())
            classNode.setInterfaces(ctx.implementsClause().genericClassNameExpression().collect { parseExpression(it) } as ClassNode[])

        classNode.genericsTypes = parseGenericDeclaration(ctx.genericDeclarationList())
        classNode.usingGenerics = classNode.genericsTypes || classNode.superClass.usingGenerics || classNode.interfaces.any { it.usingGenerics }
        classNode.modifiers = parseClassModifiers(ctx.classModifier()) | (ctx.KW_INTERFACE() ? Opcodes.ACC_INTERFACE | Opcodes.ACC_ABSTRACT : 0)
        classNode.syntheticPublic = (classNode.modifiers & Opcodes.ACC_SYNTHETIC) != 0
        classNode.modifiers &= ~Opcodes.ACC_SYNTHETIC // FIXME Magic with synthetic modifier.

        if (ctx.AT()) {
            classNode.addInterface(ClassHelper.Annotation_TYPE)
            classNode.modifiers |= Opcodes.ACC_ANNOTATION
        }

        classes << classNode
        parseMembers(classNode, ctx.classBody().classMember())
        classes.pop()

        if (classNode.interface)
            //noinspection GroovyAccessibility
            classNode.mixins =  null // FIXME why interface has null mixin
        classNode
    }

    def parseMembers(ClassNode classNode, List<ClassMemberContext> ctx) {
        for (member in ctx) {
            def memberContext = member.children[-1]

            ASTNode memberNode = null
            switch (memberContext) {
                case ClassDeclarationContext:
                    memberNode = parseClassDeclaration(memberContext as ClassDeclarationContext)
                    break;
                case EnumDeclarationContext:
                    parseEnumDeclaration(memberContext as EnumDeclarationContext)
                    break;
                case ConstructorDeclarationContext:
                case MethodDeclarationContext:
                case FieldDeclarationContext:
                case ObjectInitializerContext:
                case ClassInitializerContext:
                    // This inspection is suppressed cause I use Runtime multimethods dispatching mechanics of Groovy.
                    //noinspection GroovyAssignabilityCheck
                    memberNode = parseMember(classNode, memberContext)
                    break;
                default:
                    assert false, "Unknown class member type.";
            }
            if (memberNode)
                setupNodeLocation(memberNode, member)
            if (member.childCount > 1) {
                assert memberNode instanceof AnnotatedNode
                for (annotationCtx in member.children[0..-2]) {
                    assert annotationCtx instanceof AnnotationClauseContext
                    memberNode.addAnnotation(parseAnnotation(annotationCtx))
                }
            }
        }
    }

    @SuppressWarnings("GroovyUnusedDeclaration")
    AnnotatedNode parseMember(ClassNode classNode, MethodDeclarationContext ctx) {
        //noinspection GroovyAssignabilityCheck
        def (int modifiers, boolean hasVisibilityModifier) = parseModifiers(ctx.memberModifier(), Opcodes.ACC_PUBLIC)
        innerClassesDefinedInMethod << []
        def statement = ctx.methodBody() ? parseStatement(ctx.methodBody().blockStatement() as BlockStatementContext) : null
        def innerClassesDeclared = innerClassesDefinedInMethod.pop()

        def params = parseParameters(ctx.argumentDeclarationList())

        def returnType = ctx.typeDeclaration() ? parseTypeDeclaration(ctx.typeDeclaration()) :
            ctx.genericClassNameExpression() ? parseExpression(ctx.genericClassNameExpression()) : ClassHelper.OBJECT_TYPE

        def exceptions = parseThrowsClause(ctx.throwsClause())
        modifiers |= classNode.interface ? Opcodes.ACC_ABSTRACT : 0
        def methodNode = classNode.addMethod(ctx.IDENTIFIER().text, modifiers, returnType, params, exceptions, statement)
        methodNode.genericsTypes = parseGenericDeclaration(ctx.genericDeclarationList())
        innerClassesDeclared.each { it.enclosingMethod = methodNode }

        setupNodeLocation(methodNode, ctx)
        attachAnnotations(methodNode, ctx.annotationClause())
        methodNode.syntheticPublic = !hasVisibilityModifier
        methodNode
    }

    @SuppressWarnings("GroovyUnusedDeclaration")
    AnnotatedNode parseMember(ClassNode classNode, FieldDeclarationContext ctx) {
        //noinspection GroovyAssignabilityCheck
        def (int modifiers, boolean hasVisibilityModifier) = parseModifiers(ctx.memberModifier())
        modifiers |= classNode.interface ? Opcodes.ACC_STATIC | Opcodes.ACC_FINAL : 0


        def initExprContext = ctx.expression()
        def initialierExpression = initExprContext ? parseExpression(initExprContext) : null
        def typeDeclaration = ctx.genericClassNameExpression() ? parseExpression(ctx.genericClassNameExpression()) : ClassHelper.OBJECT_TYPE
        AnnotatedNode node
        def initialValue = classNode.interface && typeDeclaration != ClassHelper.OBJECT_TYPE ? new ConstantExpression(initialExpressionForType(typeDeclaration)) : initialierExpression
        if (classNode.interface || hasVisibilityModifier) {
            modifiers |= classNode.interface ? Opcodes.ACC_PUBLIC : 0

            def field = classNode.addField(ctx.IDENTIFIER().text, modifiers, typeDeclaration, initialValue)
            attachAnnotations(field, ctx.annotationClause())
            node = setupNodeLocation(field, ctx)
        }
        else { // no visibility specified. Generate property node.
            def propertyModifier = modifiers | Opcodes.ACC_PUBLIC
            def propertyNode = classNode.addProperty(ctx.IDENTIFIER().text, propertyModifier, typeDeclaration, initialValue, null, null)
            propertyNode.field.modifiers = modifiers | Opcodes.ACC_PRIVATE
            propertyNode.field.synthetic = !classNode.interface
            node = setupNodeLocation(propertyNode.field, ctx)
            attachAnnotations(propertyNode.field, ctx.annotationClause())
            setupNodeLocation(propertyNode, ctx)
        }
        node
    }

    @SuppressWarnings("GroovyUnusedDeclaration")
    void parseMember(ClassNode classNode, ClassInitializerContext ctx) {
        (getOrCreateClinitMethod(classNode).code as BlockStatement).addStatement(parseStatement(ctx.blockStatement()))
    }

    @SuppressWarnings("GroovyUnusedDeclaration")
    void parseMember(ClassNode classNode, ObjectInitializerContext ctx) {
        def statement = new BlockStatement()
        statement.addStatement(parseStatement(ctx.blockStatement()))
        classNode.addObjectInitializerStatements(statement)
    }

    @SuppressWarnings("GroovyUnusedDeclaration")
    AnnotatedNode parseMember(ClassNode classNode, ConstructorDeclarationContext ctx) {
        int modifiers = ctx.VISIBILITY_MODIFIER() ? parseVisibilityModifiers(ctx.VISIBILITY_MODIFIER()) : Opcodes.ACC_PUBLIC

        def exceptions = parseThrowsClause(ctx.throwsClause())
        this.innerClassesDefinedInMethod << []
        def constructorNode = classNode.addConstructor(modifiers, parseParameters(ctx.argumentDeclarationList()), exceptions, parseStatement(ctx.blockStatement() as BlockStatementContext))
        this.innerClassesDefinedInMethod.pop().each {
            it.enclosingMethod = constructorNode
        }
        setupNodeLocation(constructorNode, ctx)
        constructorNode.syntheticPublic = ctx.VISIBILITY_MODIFIER() == null
        constructorNode
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    // Statements.
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    Statement parseStatement(StatementContext ctx) {
        throw new RuntimeException("Unsupported statement type! $ctx.text")
    }

    Statement parseStatement(BlockStatementContext ctx) {
        def statement = new BlockStatement()
        if (!ctx)
            return statement

        ctx.statement().each {
            statement.addStatement(parseStatement(it))
        }
        setupNodeLocation(statement, ctx)
    }

    @SuppressWarnings("GroovyUnusedDeclaration")
    Statement parseStatement(ExpressionStatementContext ctx) {
        setupNodeLocation(new ExpressionStatement(parseExpression(ctx.expression())), ctx)
    }

    @SuppressWarnings("GroovyUnusedDeclaration")
    Statement parseStatement(IfStatementContext ctx) {
        def trueBranch = parse(ctx.statementBlock(0))
        def falseBranch = ctx.KW_ELSE() ? parse(ctx.statementBlock(1)) : EmptyStatement.INSTANCE
        def expression = new BooleanExpression(parseExpression(ctx.expression()))
        setupNodeLocation(new IfStatement(expression, trueBranch, falseBranch), ctx)
    }

    @SuppressWarnings("GroovyUnusedDeclaration")
    Statement parseStatement(WhileStatementContext ctx) {
        setupNodeLocation(new WhileStatement(new BooleanExpression(parseExpression(ctx.expression())), parse(ctx.statementBlock())), ctx)
    }

    @SuppressWarnings("GroovyUnusedDeclaration")
    Statement parseStatement(ClassicForStatementContext ctx) {
        def expression = new ClosureListExpression()

        def captureNext = false
        for (c in ctx.children) {
            // FIXME terrible logic.
            def isSemicolon = c instanceof TerminalNode && (c.symbol.text == ';' || c.symbol.text == '(' || c.symbol.text == ')')
            if (captureNext && isSemicolon)
                expression.addExpression(EmptyExpression.INSTANCE)
            else if (captureNext && c instanceof ExpressionContext)
                expression.addExpression(parseExpression(c))
            captureNext = isSemicolon
        }

        def parameter = ForStatement.FOR_LOOP_DUMMY
        setupNodeLocation(new ForStatement(parameter, expression, parse(ctx.statementBlock())), ctx)
    }

    @SuppressWarnings("GroovyUnusedDeclaration")
    Statement parseStatement(ForInStatementContext ctx) {
        def parameter = new Parameter(parseTypeDeclaration(ctx.typeDeclaration()), ctx.IDENTIFIER().text)
        parameter = setupNodeLocation(parameter, ctx.IDENTIFIER().symbol)

        setupNodeLocation(new ForStatement(parameter, parseExpression(ctx.expression()), parse(ctx.statementBlock())), ctx)
    }

    @SuppressWarnings("GroovyUnusedDeclaration")
    Statement parseStatement(ForColonStatementContext ctx) {
        if (!ctx.typeDeclaration())
            throw new RuntimeException("Classic for statement require type to be declared.")
        def parameter = new Parameter(parseTypeDeclaration(ctx.typeDeclaration()), ctx.IDENTIFIER().text)
        parameter = setupNodeLocation(parameter, ctx.IDENTIFIER().symbol)

        setupNodeLocation(new ForStatement(parameter, parseExpression(ctx.expression()), parse(ctx.statementBlock())), ctx)
    }

    Statement parse(StatementBlockContext ctx) {
        if (ctx.statement())
            setupNodeLocation(parseStatement(ctx.statement()), ctx.statement())
        else
            parseStatement(ctx.blockStatement())
    }

    @SuppressWarnings("GroovyUnusedDeclaration")
    Statement parseStatement(SwitchStatementContext ctx) {
        List<CaseStatement> caseStatements = []
        for (caseStmt in ctx.caseStatement()) {
            def stmt = new BlockStatement() // #BSC
            for (StatementContext st in caseStmt.statement())
                stmt.addStatement(parseStatement(st))

            caseStatements << setupNodeLocation(new CaseStatement(parseExpression(caseStmt.expression()), stmt),
                    caseStmt.KW_CASE().symbol) // There only 'case' kw was highlighted in parser old version.
        }

        Statement defaultStatement
        if (ctx.KW_DEFAULT()) {
            defaultStatement = new BlockStatement() // #BSC
            for (StatementContext stmt in ctx.statement())
                defaultStatement.addStatement(parseStatement(stmt))
        }
        else
            defaultStatement = EmptyStatement.INSTANCE // TODO Refactor empty stataements and expressions.

        new SwitchStatement(parseExpression(ctx.expression()), caseStatements, defaultStatement)
    }

    @SuppressWarnings("GroovyUnusedDeclaration")
    Statement parseStatement(DeclarationStatementContext ctx) {
        setupNodeLocation(new ExpressionStatement(parseDeclaration(ctx.declarationRule())), ctx)
    }

    @SuppressWarnings("GroovyUnusedDeclaration")
    Statement parseStatement(NewArrayStatementContext ctx) {
        setupNodeLocation(new ExpressionStatement(parse(ctx.newArrayRule())), ctx)
    }

    @SuppressWarnings("GroovyUnusedDeclaration")
    Statement parseStatement(NewInstanceStatementContext ctx) {
        setupNodeLocation(new ExpressionStatement(parse(ctx.newInstanceRule())), ctx)
    }

    @SuppressWarnings("GroovyUnusedDeclaration")
    Statement parseStatement(ControlStatementContext ctx) {
        // TODO check validity. Labeling support.
        // Fake inspection result should be suppressed.
        //noinspection GroovyConditionalWithIdenticalBranches
        setupNodeLocation( ctx.KW_BREAK() ? new BreakStatement() : new ContinueStatement() , ctx)
    }

    @SuppressWarnings("GroovyUnusedDeclaration")
    Statement parseStatement(ReturnStatementContext ctx) {
        def expression = ctx.expression()
        setupNodeLocation(new ReturnStatement(expression ? parseExpression(expression) : EmptyExpression.INSTANCE), ctx)
    }

    @SuppressWarnings("GroovyUnusedDeclaration")
    Statement parseStatement(ThrowStatementContext ctx) {
        setupNodeLocation(new ThrowStatement(parseExpression(ctx.expression())), ctx)
    }

    @SuppressWarnings("GroovyUnusedDeclaration")
    Statement parseStatement(TryCatchFinallyStatementContext ctx) {
        def finallyStatement

        BlockStatementContext finallyBlockStatement = ctx.finallyBlock()?.blockStatement()
        if (finallyBlockStatement) {
            def fbs = new BlockStatement()
            fbs.addStatement(parseStatement(finallyBlockStatement))
            finallyStatement = setupNodeLocation(fbs, finallyBlockStatement)

        }
        else
            finallyStatement = EmptyStatement.INSTANCE

        def statement = new TryCatchStatement(parseStatement(ctx.tryBlock().blockStatement() as BlockStatementContext), finallyStatement)
        ctx.catchBlock().each {
            def catchBlock = parseStatement(it.blockStatement() as BlockStatementContext)
            def var = it.IDENTIFIER().text

            def classNameExpression = it.classNameExpression()
            if (!classNameExpression)
                statement.addCatch(setupNodeLocation(new CatchStatement(new Parameter(ClassHelper.OBJECT_TYPE, var), catchBlock), it))
            else {
                classNameExpression.each {
                    statement.addCatch(setupNodeLocation(new CatchStatement(new Parameter(parseExpression(it as ClassNameExpressionContext), var), catchBlock), it))
                }
            }
        }
        statement
    }

    @SuppressWarnings("GroovyUnusedDeclaration")
    Statement parseStatement(CommandExpressionStatementContext ctx) {
        Expression expression = null
        def list = ctx.cmdExpressionRule().children.collate(2)
        for (c in list) {
            def (c1, c0) = c
            if (c.size() == 1)
                expression = new PropertyExpression(expression, c1.text as String)
            else {
                assert c0 instanceof ArgumentListContext
                if (c1 instanceof TerminalNode) {
                    expression = new MethodCallExpression(expression, c1.text, createArgumentList(c0))
                    expression.implicitThis = false
                }
                else if (c1 instanceof PathExpressionContext) {
                    String methodName
                    boolean implicitThis
                    //noinspection GroovyAssignabilityCheck
                    (expression, methodName, implicitThis) = parsePathExpression(c1)

                    expression = new MethodCallExpression(expression, methodName, createArgumentList(c0))
                    expression.implicitThis = implicitThis
                }
            }
        }

        new ExpressionStatement(expression)
    }

    /**
     * Parse path expression.
     * @param ctx
     * @return tuple of 3 values: Expression, String methodName and boolean implicitThis flag.
     */
    def parsePathExpression(PathExpressionContext ctx) {
        Expression expression
        def identifiers = ctx.IDENTIFIER() as List<TerminalNode>
        switch (identifiers.size()) {
            case 1: expression = VariableExpression.THIS_EXPRESSION; break
            case 2: expression = new VariableExpression(identifiers[0].text); break
            default: expression = identifiers[1..-2].inject(new VariableExpression(identifiers[0].text)) { Expression expr, prop ->
                new PropertyExpression(expr, prop.text)
            }; break
        }
        [expression, identifiers[-1], identifiers.size() == 1]
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    // Expressions.
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    Expression parseExpression(ExpressionContext ctx) {
        throw new RuntimeException("Unsupported expression type! $ctx, type ${ctx.getClass()}")
    }

    @SuppressWarnings("GroovyUnusedDeclaration")
    Expression parseExpression(NewArrayExpressionContext ctx) {
        parse(ctx.newArrayRule())
    }

    @SuppressWarnings("GroovyUnusedDeclaration")
    Expression parseExpression(NewInstanceExpressionContext ctx) {
        parse(ctx.newInstanceRule())
    }

    @SuppressWarnings("GroovyUnusedDeclaration")
    Expression parseExpression(ParenthesisExpressionContext ctx) {
        parseExpression(ctx.expression())
    }

    @SuppressWarnings("GroovyUnusedDeclaration")
    Expression parseExpression(ListConstructorContext ctx) {
        def expression = new ListExpression(ctx.expression().collect(this.&parseExpression))
        setupNodeLocation(expression, ctx)
    }

    @SuppressWarnings("GroovyUnusedDeclaration")
    Expression parseExpression(MapConstructorContext ctx) {
        setupNodeLocation(new MapExpression(ctx.mapEntry()?.collect(this.&parseExpression) ?: []), ctx)
    }

    @SuppressWarnings("GroovyUnusedDeclaration")
    MapEntryExpression parseExpression(MapEntryContext ctx) {
        Expression keyExpr, valueExpr
        def expressions = ctx.expression()
        if (expressions.size() == 1) {
            keyExpr = ctx.gstring() ?
                parseExpression(ctx.gstring()) :
                new ConstantExpression(ctx.IDENTIFIER() ?
                    ctx.IDENTIFIER().text :
                    parseString(ctx.STRING()))

            valueExpr = parseExpression(expressions[0])
        }
        else {
            keyExpr = parseExpression(expressions[0])
            valueExpr = parseExpression(expressions[1])
        }
        setupNodeLocation(new MapEntryExpression(keyExpr, valueExpr), ctx)
    }

    @SuppressWarnings("GroovyUnusedDeclaration")
    Expression parseExpression(ClosureExpressionContext ctx) {
        parseExpression(ctx.closureExpressionRule())
    }

    @SuppressWarnings("GroovyUnusedDeclaration")
    Expression parseExpression(ClosureExpressionRuleContext ctx) {
        def parameters = ctx.argumentDeclarationList() ? (parseParameters(ctx.argumentDeclarationList()) ?: null) : ([] as Parameter[])

        def statement = parseStatement(ctx.blockStatement() as BlockStatementContext)
        setupNodeLocation(new ClosureExpression(parameters, statement), ctx)
    }

    @SuppressWarnings("GroovyUnusedDeclaration")
    Expression parseExpression(BinaryExpressionContext ctx) {
        def c = ctx.getChild(1) as TerminalNode
        int i = 1
        for (def next = ctx.getChild(i + 1); next instanceof TerminalNode && next.symbol.type == GroovyParser.GT; next = ctx.getChild(i + 1))
            i++
        def op = createToken(c, i)
        def expression
        def left = parseExpression(ctx.expression(0))
        def right = null // Will be initialized later, in switch. We should handle as and instanceof creating
        // ClassExpression for given IDENTIFIERS. So, switch should fall through.
        //noinspection GroovyFallthrough
        switch (op.type) {
            case Types.RANGE_OPERATOR:
                right = parseExpression(ctx.expression(1))
                expression = new RangeExpression(left, right, !op.text.endsWith('<'))
                break;
            case Types.KEYWORD_AS:
                def classNode = setupNodeLocation(parseExpression(ctx.genericClassNameExpression()), ctx.genericClassNameExpression())
                expression = CastExpression.asExpression(classNode, left)
                break;
            case Types.KEYWORD_INSTANCEOF:
                def classNode = setupNodeLocation(parseExpression(ctx.genericClassNameExpression()), ctx.genericClassNameExpression())
                right = new ClassExpression(classNode)
            default:
                if (!right)
                    right = parseExpression(ctx.expression(1))
                expression = new BinaryExpression(left, op, right)
                break
        }

        expression.columnNumber = op.startColumn
        expression.lastColumnNumber = op.startColumn + op.text.length()
        expression.lineNumber = op.startLine
        expression.lastLineNumber = op.startLine
        expression
    }

    @SuppressWarnings("GroovyUnusedDeclaration")
    Expression parseExpression(TernaryExpressionContext ctx) {
        def boolExpr = new BooleanExpression(parseExpression(ctx.expression(0)))
        def trueExpr = parseExpression(ctx.expression(1))
        def falseExpr = parseExpression(ctx.expression(2))
        setupNodeLocation(new TernaryExpression(boolExpr, trueExpr, falseExpr), ctx)
    }

    @SuppressWarnings("GroovyUnusedDeclaration")
    Expression parseExpression(ElvisExpressionContext ctx) {
        def baseExpr = parseExpression(ctx.expression(0))
        def falseExpr = parseExpression(ctx.expression(1))
        setupNodeLocation(new ElvisOperatorExpression(baseExpr, falseExpr), ctx)
    }

    @SuppressWarnings("GroovyUnusedDeclaration")
    Expression parseExpression(UnaryExpressionContext ctx) {
        def node = null
        def op = ctx.getChild(0) as TerminalNode
        switch (op.text) {
            case '-' : node = new UnaryMinusExpression(parseExpression(ctx.expression())); break
            case '+' : node = new UnaryPlusExpression(parseExpression(ctx.expression())); break
            case '!' : node = new NotExpression(parseExpression(ctx.expression())); break
            case '~' : node = new BitwiseNegationExpression(parseExpression(ctx.expression())); break
            default: assert false, "There is no $op.text handler."; break
        }

        node.columnNumber = op.symbol.charPositionInLine + 1
        node.lineNumber = op.symbol.line
        node.lastLineNumber = op.symbol.line
        node.lastColumnNumber = op.symbol.charPositionInLine + 1 + op.text.length()
        node
    }

    @SuppressWarnings("GroovyUnusedDeclaration")
    Expression parseExpression(AnnotationParameterContext ctx) {
        switch (ctx) {
            case AnnotationParamArrayExpressionContext:
                def c = ctx as AnnotationParamArrayExpressionContext
                return setupNodeLocation(new ListExpression(c.annotationParameter().collect { parseExpression(it) }), c)
            case AnnotationParamBoolExpressionContext:
                return parseExpression(ctx);
            case AnnotationParamClassExpressionContext:
                return setupNodeLocation(new ClassExpression(parseExpression((ctx as AnnotationParamClassExpressionContext).genericClassNameExpression())), ctx);
            case AnnotationParamDecimalExpressionContext:
                return parseExpression(ctx);
            case AnnotationParamIntegerExpressionContext:
                return parseExpression(ctx);
            case AnnotationParamNullExpressionContext:
                return parseExpression(ctx);
            case AnnotationParamPathExpressionContext:
                def c = ctx as AnnotationParamPathExpressionContext
                return collectPathExpression(c.pathExpression())
            case AnnotationParamStringExpressionContext:
                return parseExpression(ctx);
        }
        throw new CompilationFailedException(CompilePhase.PARSING.phaseNumber, this.sourceUnit, new IllegalStateException("$ctx is prohibited inside annotations."))
    }

    @SuppressWarnings("GroovyUnusedDeclaration")
    Expression parseExpression(VariableExpressionContext ctx) {
        setupNodeLocation(new VariableExpression(ctx.IDENTIFIER().text), ctx)
    }

    @SuppressWarnings("GroovyUnusedDeclaration")
    Expression parseExpression(FieldAccessExpressionContext ctx) {
        def op = ctx.getChild(1) as TerminalNode
        def left = parseExpression(ctx.expression())
        def methodIdentifier = ctx.IDENTIFIER()
        def right = methodIdentifier ? new ConstantExpression(methodIdentifier.text) : (
            ctx.STRING() ? parseConstantStringToken(ctx.STRING().symbol) : parseExpression(ctx.gstring())
        )
        def node
        switch (op.text) {
            case '.@':
                node = new AttributeExpression(left, right)
                break
            case '.&':
                node = new MethodPointerExpression(left, right)
                break
            case '?.':
                node = new PropertyExpression(left, right, true)
                break
            case '*.':
                node = new PropertyExpression(left, right, true /* For backwards compatibility! */)
                node.spreadSafe = true
                break
            default:
                node = new PropertyExpression(left, right, false)
                break
        }
        setupNodeLocation(node, ctx)
    }

    @SuppressWarnings("GroovyUnusedDeclaration")
    PrefixExpression parseExpression(PrefixExpressionContext ctx) {
        setupNodeLocation(new PrefixExpression(createToken(ctx.getChild(0) as TerminalNode), parseExpression(ctx.expression())), ctx)
    }

    @SuppressWarnings("GroovyUnusedDeclaration")
    PostfixExpression parseExpression(PostfixExpressionContext ctx) {
        setupNodeLocation(new PostfixExpression(parseExpression(ctx.expression()), createToken(ctx.getChild(1) as TerminalNode)), ctx)
    }

    ConstantExpression parseDecimal(String text, ParserRuleContext ctx) {
        setupNodeLocation(new ConstantExpression(Numbers.parseDecimal(text), !text.startsWith('-')), ctx) // Why 10 is int but -10 is Integer?
    }

    @SuppressWarnings("GroovyUnusedDeclaration")
    ConstantExpression parseExpression(AnnotationParamDecimalExpressionContext ctx) {
        parseDecimal(ctx.DECIMAL().text, ctx)
    }

    @SuppressWarnings("GroovyUnusedDeclaration")
    ConstantExpression parseExpression(ConstantDecimalExpressionContext ctx) {
        parseDecimal(ctx.DECIMAL().text, ctx)
    }

    ConstantExpression parseInteger(String text, ParserRuleContext ctx) {
        setupNodeLocation(new ConstantExpression(Numbers.parseInteger(text), !text.startsWith('-')), ctx) //Why 10 is int but -10 is Integer?
    }

    ConstantExpression parseInteger(String text, org.antlr.v4.runtime.Token ctx) {
        setupNodeLocation(new ConstantExpression(Numbers.parseInteger(text), !text.startsWith('-')), ctx) //Why 10 is int but -10 is Integer?
    }

    @SuppressWarnings("GroovyUnusedDeclaration")
    ConstantExpression parseExpression(ConstantIntegerExpressionContext ctx) {
        parseInteger(ctx.INTEGER().text, ctx)
    }

    @SuppressWarnings("GroovyUnusedDeclaration")
    ConstantExpression parseExpression(AnnotationParamIntegerExpressionContext ctx) {
        parseInteger(ctx.INTEGER().text, ctx)
    }

    @SuppressWarnings("GroovyUnusedDeclaration")
    ConstantExpression parseExpression(BoolExpressionContext ctx) {
        setupNodeLocation(new ConstantExpression(ctx.KW_FALSE() ? false : true, true), ctx)
    }

    @SuppressWarnings("GroovyUnusedDeclaration")
    ConstantExpression parseExpression(AnnotationParamBoolExpressionContext ctx) {
        setupNodeLocation(new ConstantExpression(ctx.KW_FALSE() ? false : true, true), ctx)
    }

    @SuppressWarnings("GroovyUnusedDeclaration")
    cleanConstantStringLiteral(String text) {
        def isSlashy = text.startsWith('/')

        //Remove start and end quotes.
        if (text.startsWith(/'''/) || text.startsWith(/"""/))
            text = text.length() == 6 ? '' : text[3..-4]
        else if (text.startsWith(/'/) || text.startsWith('/') || text.startsWith(/"/))
            text = text.length() == 2 ? '' : text[1..-2]

        //Find escapes.
        if (!isSlashy)
            text = StringUtil.replaceStandardEscapes(StringUtil.replaceOctalEscapes(text))
        else
            text = text.replace($/\//$, '/')
        new ConstantExpression(text, true)
    }

    @SuppressWarnings("GroovyUnusedDeclaration")
    parseConstantString(ParserRuleContext ctx) {
        def text = ctx.text
        setupNodeLocation(cleanConstantStringLiteral(text), ctx)
    }

    @SuppressWarnings("GroovyUnusedDeclaration")
    parseConstantStringToken(org.antlr.v4.runtime.Token token) {
        def text = token.text
        setupNodeLocation(cleanConstantStringLiteral(text), token)
    }

    @SuppressWarnings("GroovyUnusedDeclaration")
    ConstantExpression parseExpression(ConstantExpressionContext ctx) {
        parseConstantString(ctx)
    }

    @SuppressWarnings("GroovyUnusedDeclaration")
    ConstantExpression parseExpression(AnnotationParamStringExpressionContext ctx) {
        parseConstantString(ctx)
    }

    @SuppressWarnings("GroovyUnusedDeclaration")
    Expression parseExpression(GstringExpressionContext ctx) {
        parseExpression(ctx.gstring())
    }

    Expression parseExpression(GstringContext ctx) {
        def clearStart = { String it -> it.length() == 2 ? "" : it[1..-2] }
        def clearPart = { String it -> it.length() == 1 ? "" : it[0..-2] }
        def clearEnd = { String it -> it.length() == 1 ? "" : it[0..-2] }
        def strings = [clearStart(ctx.GSTRING_START().text)] + ctx.GSTRING_PART().collect { clearPart(it.text) } + [clearEnd(ctx.GSTRING_END().text)]
        def expressions = []

        def children = ctx.children
        children.eachWithIndex { it, i ->
            if (it instanceof ExpressionContext) {
                // We can guarantee, that it will be at least fallback ExpressionContext multimethod overloading, that can handle such situation.
                //noinspection GroovyAssignabilityCheck
                expressions << (parseExpression(it) as Expression)
            }
            else if (it instanceof GstringPathExpressionContext)
                expressions << collectPathExpression(it)
            else if (it instanceof TerminalNode) {
                def next = i + 1 < children.size() ? children[i + 1] : null
                if (next instanceof TerminalNode && (next as TerminalNode).symbol.type == GroovyParser.RCURVE)
                    expressions << new ConstantExpression(null)
            }
        }
        def gstringNode = new GStringExpression(ctx.text, strings.collect { new ConstantExpression(it) }, expressions)
        setupNodeLocation(gstringNode, ctx)
    }

    @SuppressWarnings("GroovyUnusedDeclaration")
    Expression parseExpression(NullExpressionContext ctx) {
        setupNodeLocation(new ConstantExpression(null), ctx)
    }

    @SuppressWarnings("GroovyUnusedDeclaration")
    Expression parseExpression(AnnotationParamNullExpressionContext ctx) {
        setupNodeLocation(new ConstantExpression(null), ctx)
    }

    @SuppressWarnings("GroovyUnusedDeclaration")
    Expression parseExpression(AssignmentExpressionContext ctx) {
        def left = parseExpression(ctx.expression(0)) // TODO reference to AntlrParserPlugin line 2304 for error handling.
        def right = parseExpression(ctx.expression(1))
        setupNodeLocation(new BinaryExpression(left, createToken(ctx.getChild(1) as TerminalNode), right), ctx)
    }

    @SuppressWarnings("GroovyUnusedDeclaration")
    Expression parseExpression(DeclarationExpressionContext ctx) {
        parseDeclaration(ctx.declarationRule())
    }

    @SuppressWarnings("GroovyUnusedDeclaration")
    Expression parseExpression(CallExpressionContext ctx) {

        def methodNode
        //FIXME in log a, b; a is treated as path expression and became a method call instead of variable
        if (!ctx.LPAREN() && ctx.closureExpressionRule().size() == 0)
            return collectPathExpression(ctx.pathExpression())

        // Collect closure's in argumentList expression.
        def argumentListExpression = createArgumentList(ctx.argumentList())
        ctx.closureExpressionRule().each { argumentListExpression.addExpression(parseExpression(it)) }

        //noinspection GroovyAssignabilityCheck
        def (Expression expression, String methodName, boolean implicitThis) = parsePathExpression(ctx.pathExpression())
        methodNode = new MethodCallExpression(expression, methodName, argumentListExpression)
        methodNode.implicitThis = implicitThis
        methodNode
    }

    Expression collectPathExpression(PathExpressionContext ctx) {
        def identifiers = ctx.IDENTIFIER()
        switch (identifiers.size()) {
        case 1:
            return new VariableExpression(identifiers[0].text);
            break;
        default:
            def inject = identifiers[1..-1].inject(new VariableExpression(identifiers[0].text)) { val, prop ->
                new PropertyExpression(val as Expression, new ConstantExpression(prop.text))
            }
            return inject
        }
    }

    Expression collectPathExpression(GstringPathExpressionContext ctx) {
        if (!ctx.GSTRING_PATH_PART())
            new VariableExpression(ctx.IDENTIFIER().text)
        else {
            def inj = ctx.GSTRING_PATH_PART().inject(new VariableExpression(ctx.IDENTIFIER().text)) { val, prop ->
                new PropertyExpression(val as Expression, new ConstantExpression(prop.text[1..-1]))
            }
            inj
        }
    }

    @SuppressWarnings("GroovyUnusedDeclaration")
    MethodCallExpression parseExpression(MethodCallExpressionContext ctx) {
        def methodIdentifier = ctx.IDENTIFIER()
        def method = methodIdentifier ? new ConstantExpression(methodIdentifier.text) : (
            ctx.STRING() ? parseConstantStringToken(ctx.STRING().symbol) : parseExpression(ctx.gstring())
        )
        Expression argumentListExpression = createArgumentList(ctx.argumentList())
        def expression = new MethodCallExpression(parseExpression(ctx.expression()), method, argumentListExpression)
        expression.implicitThis = false
        def op = ctx.getChild(1) as TerminalNode
        expression.spreadSafe = op.text == '*.'
        expression.safe = op.text == '?.'
        expression
    }

    ClassNode parseExpression(ClassNameExpressionContext ctx) {
        setupNodeLocation(ClassHelper.make(ctx.IDENTIFIER().join('.')), ctx)
    }

    ClassNode parseExpression(GenericClassNameExpressionContext ctx) {
        def classNode = parseExpression(ctx.classNameExpression())

        if (ctx.LBRACK())
            classNode = classNode.makeArray()
        classNode.genericsTypes = parseGenericList(ctx.genericList())
        setupNodeLocation(classNode, ctx)
    }

    GenericsType[] parseGenericList(GenericListContext ctx) {
        !ctx ?
            null
        : ctx.genericListElement().collect {
            if (it instanceof GenericsConcreteElementContext)
                setupNodeLocation(new GenericsType(parseExpression(it.genericClassNameExpression())), it)
            else {
                assert it instanceof GenericsWildcardElementContext
                ClassNode baseType = ClassHelper.makeWithoutCaching("?")
                ClassNode[] upperBounds = null
                ClassNode lowerBound = null
                if (it.KW_EXTENDS())
                    upperBounds = [ parseExpression(it.genericClassNameExpression()) ]
                else if (it.KW_SUPER())
                    lowerBound = parseExpression(it.genericClassNameExpression())

                def type = new GenericsType(baseType, upperBounds, lowerBound)
                type.wildcard = true
                type.name = "?"
                setupNodeLocation(type, it)
            }
        }
    }

    GenericsType[] parseGenericDeclaration(GenericDeclarationListContext ctx) {
        ASTBuilder self = this
        ctx ? ctx.genericsDeclarationElement().collect {
            def classNode = parseExpression(it.genericClassNameExpression(0))
            ClassNode[] upperBounds = null
            if (it.KW_EXTENDS())
                upperBounds = (it.genericClassNameExpression().toList()[1..-1].collect(self.&parseExpression)) as ClassNode[]
            def type = new GenericsType(classNode, upperBounds, null)
            setupNodeLocation(type, it)
        } : null
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    // End of Expressions.
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    Expression parseDeclaration(DeclarationRuleContext ctx) {
        def left = new VariableExpression(ctx.IDENTIFIER().text, parseTypeDeclaration(ctx.typeDeclaration()))
        def col = ctx.start.charPositionInLine + 1 // FIXME Why assignment token location is it's first occurrence.
        def token = new Token(Types.ASSIGN, '=', ctx.start.line, col)
        def right = ctx.childCount == 2 ? new EmptyExpression() : parseExpression(ctx.expression())

        def expression = new DeclarationExpression(left, token, right)
        attachAnnotations(expression, ctx.annotationClause())
        setupNodeLocation(expression, ctx)
    }

    @SuppressWarnings("UnnecessaryQualifiedReference")
    private Expression createArgumentList(GroovyParser.ArgumentListContext ctx) {
        List<MapEntryExpression> mapArgs = []
        List<Expression> expressions = []
        ctx?.children?.each {
            if (it instanceof GroovyParser.ArgumentContext) {
                if (it.mapEntry())
                    mapArgs << parseExpression(it.mapEntry())
                else
                    expressions << parseExpression(it.expression())
            }
            else if (it instanceof GroovyParser.ClosureExpressionRuleContext)
                expressions << parseExpression(it)
        }
        if (expressions) {
            if (mapArgs)
                expressions.add(0, new MapExpression(mapArgs))
            new ArgumentListExpression(expressions)
        }
        else {
            if (mapArgs)
                new TupleExpression(new NamedArgumentListExpression(mapArgs))
            else
                new ArgumentListExpression()
        }
    }

    def attachAnnotations(AnnotatedNode node, List<AnnotationClauseContext> ctxs) {
        for (ctx in ctxs) {
            def annotation = parseAnnotation(ctx)
            node.addAnnotation(annotation)
        }
    }

    List<AnnotationNode> parseAnnotations(List<AnnotationClauseContext> ctxs) {
        ctxs.collect { parseAnnotation(it) }
    }

    AnnotationNode parseAnnotation(AnnotationClauseContext ctx) {
        def node = new AnnotationNode(parseExpression(ctx.genericClassNameExpression()))
        if (ctx.annotationElement())
            node.addMember("value", parseAnnotationElement(ctx.annotationElement()))
        else {
            for (pair in ctx.annotationElementPair()) {
                node.addMember(pair.IDENTIFIER().text, parseAnnotationElement(pair.annotationElement()))
            }
        }

        setupNodeLocation(node, ctx)
    }

    Expression parseAnnotationElement(AnnotationElementContext ctx) {
        def annotationClause = ctx.annotationClause()
        if (annotationClause)
            setupNodeLocation(new AnnotationConstantExpression(parseAnnotation(annotationClause)), annotationClause)
        else
            parseExpression(ctx.annotationParameter())
    }


    ClassNode[] parseThrowsClause(ThrowsClauseContext ctx) {
        ctx ? ctx.classNameExpression().collect { parseExpression(it) } : []
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    // Utility methods.
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    /**
     *
     * @param node
     * @param cardinality Used for handling GT ">" operator, which can be repeated to give bitwise shifts >> or >>>
     * @return
     */
    Token createToken(TerminalNode node, int cardinality = 1) {
        def text = node.text * cardinality
        new Token(node.text == '..<' || node.text == '..' ? Types.RANGE_OPERATOR : Types.lookup(text, Types.ANY),
            text, node.symbol.line, node.symbol.charPositionInLine + 1)
    }

    ClassNode parseTypeDeclaration(TypeDeclarationContext ctx) {
        !ctx || ctx.KW_DEF() ? ClassHelper.OBJECT_TYPE : setupNodeLocation(parseExpression(ctx.genericClassNameExpression()), ctx)
    }

    def parse(NewArrayRuleContext ctx) {
        def expression = new ArrayExpression(parseExpression(ctx.classNameExpression()), [], ctx.INTEGER().collect { parseInteger(it.text, it.symbol) })
        setupNodeLocation(expression, ctx)
    }

    def parse(NewInstanceRuleContext ctx) {
        def creatingClass = ctx.genericClassNameExpression() ? parseExpression(ctx.genericClassNameExpression()) : parseExpression(ctx.classNameExpression())
        if (ctx.LT()) // Diamond case.
            creatingClass.genericsTypes = []

        def expression
        if (!ctx.classBody()) {
            expression = setupNodeLocation(new ConstructorCallExpression(creatingClass, createArgumentList(ctx.argumentList())), ctx)
        }
        else {
            ClassNode outer = this.classes.peek()
            def classNode = new InnerClassNode(outer, "$outer.name\$${++this.anonymousClassesCount}", Opcodes.ACC_PUBLIC, ClassHelper.make(creatingClass.name))
            expression = setupNodeLocation(new ConstructorCallExpression(classNode, createArgumentList(ctx.argumentList())), ctx)
            expression.usingAnonymousInnerClass = true
            classNode.anonymous = true
            this.innerClassesDefinedInMethod[-1] << classNode
            this.moduleNode.addClass(classNode)
            this.classes << classNode
            this.parseMembers(classNode, ctx.classBody().classMember())
            this.classes.pop()
        }
        expression
    }

    Parameter[] parseParameters(ArgumentDeclarationListContext ctx) {
        ctx.argumentDeclaration().collect {
            def parameter = new Parameter(parseTypeDeclaration(it.typeDeclaration()), it.IDENTIFIER().text)
            attachAnnotations(parameter, it.annotationClause())
            if (it.expression())
                parameter.initialExpression = parseExpression(it.expression())
            setupNodeLocation(parameter, it)
        }
    }

    MethodNode getOrCreateClinitMethod(ClassNode classNode) {
        def methodNode = classNode.methods.find { it.name == "<clinit>" }
        if (!methodNode) {
            methodNode = new MethodNode("<clinit>", Opcodes.ACC_STATIC, ClassHelper.VOID_TYPE, [] as Parameter[], [] as ClassNode[], new BlockStatement())
            methodNode.synthetic = true
            classNode.addMethod(methodNode)
        }
        methodNode
    }

    /**
     * Sets location(lineNumber, colNumber, lastLineNumber, lastColumnNumber) for node using standard context information.
     * Note: this method is implemented to be closed over ASTNode. It returns same node as it received in arguments.
     * @param astNode Node to be modified.
     * @param ctx Context from which information is obtained.
     * @return Modified astNode.
     */
    private <T extends ASTNode> T setupNodeLocation(T astNode, ParserRuleContext ctx) {
        astNode.lineNumber = ctx.start.line
        astNode.columnNumber = ctx.start.charPositionInLine + 1
        astNode.lastLineNumber = ctx.stop.line
        astNode.lastColumnNumber = ctx.stop.charPositionInLine + 1 + ctx.stop.text.length()
        astNode
    }

    private <T extends ASTNode> T setupNodeLocation(T astNode, org.antlr.v4.runtime.Token token) {
        astNode.lineNumber = token.line
        astNode.columnNumber = token.charPositionInLine + 1
        astNode.lastLineNumber = token.line
        astNode.lastColumnNumber = token.charPositionInLine + 1 + token.text.length()
        astNode
    }

    int parseClassModifiers(@NotNull List<ClassModifierContext> ctxs) {
        List<TerminalNode> visibilityModifiers = []
        int modifiers = 0
        for (child in ctxs.children) {
            if (child instanceof List) {
                assert child.size() == 1
                child = child[0]
            }
            assert child instanceof TerminalNode
            switch (child.symbol.type) {
                case GroovyLexer.VISIBILITY_MODIFIER: visibilityModifiers << child; break
                case GroovyLexer.KW_STATIC: modifiers |= checkModifierDuplication(modifiers, Opcodes.ACC_STATIC, child); break
                case GroovyLexer.KW_ABSTRACT: modifiers |= checkModifierDuplication(modifiers, Opcodes.ACC_ABSTRACT, child); break
                case GroovyLexer.KW_FINAL: modifiers |= checkModifierDuplication(modifiers, Opcodes.ACC_FINAL, child); break
                case GroovyLexer.KW_STRICTFP: modifiers |= checkModifierDuplication(modifiers, Opcodes.ACC_STRICT, child); break
            }
        }
        if (visibilityModifiers)
            modifiers |= parseVisibilityModifiers(visibilityModifiers, 0) // Here we shouldn't pass any default value. Old code. Needs refactoring.
        else
            modifiers |= Opcodes.ACC_PUBLIC | Opcodes.ACC_SYNTHETIC
        modifiers
    }

    int checkModifierDuplication(int modifier, int opcode, TerminalNode node) {
        if (!(modifier & opcode))
            modifier | opcode
        else {
            def symbol = node.symbol

            def line = symbol.line
            def col = symbol.charPositionInLine + 1
            sourceUnit.addError(new SyntaxException("Cannot repeat modifier: $symbol.text at line: $line column: $col. File: $sourceUnit.name", line, col))
            modifier
        }
    }

    /**
     * Traverse through modifiers, and combine them in one int value. Raise an error if there is multiple occurrences of same modifier.
     * @param ctxList modifiers list.
     * @param defaultVisibilityModifier Default visibility modifier. Can be null. Applied if providen, and no visibility modifier exists in the ctxList.
     * @return tuple of int modifier and boolean flag, signalising visibility modifiers presence(true if there is visibility modifier in list, false otherwise).
     * @see #checkModifierDuplication(int, int, org.antlr.v4.runtime.tree.TerminalNode)
     */
    def parseModifiers(List<MemberModifierContext> ctxList, Integer defaultVisibilityModifier = null) {
        int modifiers = 0;
        boolean hasVisibilityModifier = false;
        ctxList.each {
            def child = (it.getChild(0) as TerminalNode)
            switch (child.symbol.type) {
                case GroovyLexer.KW_STATIC: modifiers |= checkModifierDuplication(modifiers, Opcodes.ACC_STATIC, child); break
                case GroovyLexer.KW_ABSTRACT: modifiers |= checkModifierDuplication(modifiers, Opcodes.ACC_ABSTRACT, child); break
                case GroovyLexer.KW_FINAL: modifiers |= checkModifierDuplication(modifiers, Opcodes.ACC_FINAL, child); break
                case GroovyLexer.KW_NATIVE: modifiers |= checkModifierDuplication(modifiers, Opcodes.ACC_NATIVE, child); break
                case GroovyLexer.KW_SYNCHRONIZED: modifiers |= checkModifierDuplication(modifiers, Opcodes.ACC_SYNCHRONIZED, child); break
                case GroovyLexer.KW_TRANSIENT: modifiers |= checkModifierDuplication(modifiers, Opcodes.ACC_TRANSIENT, child); break
                case GroovyLexer.KW_VOLATILE: modifiers |= checkModifierDuplication(modifiers, Opcodes.ACC_VOLATILE, child); break
                case GroovyLexer.VISIBILITY_MODIFIER:
                    modifiers |= parseVisibilityModifiers(child)
                    hasVisibilityModifier = true
                    break
            }
        }
        if (!hasVisibilityModifier && defaultVisibilityModifier != null)
            modifiers |= defaultVisibilityModifier

        [modifiers, hasVisibilityModifier]
    }

    void reportError(String text, int line, int col) {
        sourceUnit.addError(new SyntaxException(text, line, col))
    }

    int parseVisibilityModifiers(TerminalNode modifier) {
        assert modifier.symbol.type == GroovyLexer.VISIBILITY_MODIFIER
        switch (modifier.symbol.text) {
            case "public": Opcodes.ACC_PUBLIC; break
            case "private": Opcodes.ACC_PRIVATE; break
            case "protected": Opcodes.ACC_PROTECTED; break
            default: throw new AssertionError("$modifier.symbol.text is not a valid visibility modifier!")
        }
    }

    int parseVisibilityModifiers(List<TerminalNode> modifiers, int defaultValue) {
        if (!modifiers)
            return defaultValue

        if (modifiers.size() > 1) {
            def modifier = modifiers[1].symbol

            def line = modifier.line
            def col = modifier.charPositionInLine + 1

            reportError("Cannot specify modifier: $modifier.text when access scope has already been defined at line: $line column: $col. File: $sourceUnit.name", line, col)
        }

        parseVisibilityModifiers(modifiers[0])
    }

    /**
     * Method for construct string from string literal handling empty strings.
     * @param node
     * @return
     */
    String parseString(TerminalNode node) {
        def t = node.text
        t ? t[1..-2] : t
    }

    def initialExpressionForType(ClassNode type) {
        switch (type) {
            case ClassHelper.int_TYPE: return 0
            case ClassHelper.long_TYPE: return 0L
            case ClassHelper.double_TYPE: return 0.0
            case ClassHelper.float_TYPE: return 0f
            case ClassHelper.boolean_TYPE: return Boolean.FALSE
            case ClassHelper.short_TYPE: return 0 as short
            case ClassHelper.byte_TYPE: return 0 as byte
            case ClassHelper.char_TYPE: return 0 as char
        }
    }
}
