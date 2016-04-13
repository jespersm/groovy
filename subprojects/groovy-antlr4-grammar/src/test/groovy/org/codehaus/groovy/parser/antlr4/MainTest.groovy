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
package org.codehaus.groovy.parser.antlr4

import org.codehaus.groovy.ast.FieldNode
import org.codehaus.groovy.ast.GenericsType
import org.codehaus.groovy.ast.Parameter
import org.codehaus.groovy.ast.PropertyNode
import org.codehaus.groovy.ast.stmt.*
import org.codehaus.groovy.control.CompilerConfiguration
import org.codehaus.groovy.parser.antlr4.util.ASTComparatorCategory
import org.codehaus.groovy.parser.antlr4.util.ASTWriter
import spock.lang.Specification
import spock.lang.Unroll

import java.util.logging.Logger

class MainTest extends Specification {
    private Logger log = Logger.getLogger(MainTest.class.getName());
    public static final String DEFAULT_RESOURCES_PATH = 'subprojects/groovy-antlr4-grammar/src/test/resources';
    public static final String RESOURCES_PATH = new File(DEFAULT_RESOURCES_PATH).exists() ? DEFAULT_RESOURCES_PATH : 'src/test/resources';



	@Unroll
    def "test ast builder for #path"() {
        def filename = path;

        setup:
        def file = new File("$RESOURCES_PATH/$path")
        def moduleNodeNew = new Main(Configuration.NEW).process(file)
        def moduleNodeOld = new Main(Configuration.OLD).process(file)
        def moduleNodeOld2 = new Main(Configuration.OLD).process(file)
        config = config.is(_) ? ASTComparatorCategory.DEFAULT_CONFIGURATION : config

        expect:
        moduleNodeNew
        moduleNodeOld
        ASTComparatorCategory.apply(config) {
            assert moduleNodeOld == moduleNodeOld2
        }
        and:
        ASTWriter.astToString(moduleNodeNew) == ASTWriter.astToString(moduleNodeOld2)
        and:
        ASTComparatorCategory.apply(config) {
            assert moduleNodeNew == moduleNodeOld, "Fail in $path"
        }

        where:
        path | config
        "Annotations_Issue30_1.groovy" | _
        "Annotations_Issue30_2.groovy" | _
        "ArrayType_Issue44_1.groovy" | _
        "AssignmentOps_Issue23_1.groovy" | _
        "ClassConstructorBug_Issue13_1.groovy" | _
        "ClassInitializers_Issue_20_1.groovy" | _
        "ClassMembers_Issue3_1.groovy" | _
        "ClassMembers_Issue3_2.groovy" | _
        "ClassModifiers_Issue_2.groovy" | _
        "ClassProperty_Issue4_1.groovy" | _
        "Closure_Issue21_1.groovy" | addIgnore(ExpressionStatement, ASTComparatorCategory.LOCATION_IGNORE_LIST)
        "Enums_Issue43_1.groovy" | _
        "ExceptionHandling_Issue27_1.groovy" | _
        "ExplicitConstructor.groovy" | _
        "Extendsimplements_Issue25_1.groovy" | _
        "FieldAccessAndMethodCalls_Issue37_1.groovy" | _
        'FieldAccessAndMethodCalls_Issue37_2.groovy' | addIgnore(ExpressionStatement, ASTComparatorCategory.LOCATION_IGNORE_LIST)
        "FieldInitializersAndDefaultMethods_Issue49_1.groovy" | _
        "Generics_Issue26_1.groovy" | addIgnore(GenericsType, ASTComparatorCategory.LOCATION_IGNORE_LIST)
        "GStrings_Issue41_1.groovy" | _
        "ImportRecognition_Issue6_1.groovy" | _
        "ImportRecognition_Issue6_2.groovy" | _
        "InnerClasses_Issue48_1.groovy" | _
        "ListsAndMaps_Issue22_1.groovy" | _
        "Literals_Numbers_Issue36_1.groovy" | _
        'Literals_Other_Issue36_4.groovy' | _
        "Literals_HexOctNumbers_Issue36_2.groovy" | _
        "Literals_Strings_Issue36_3.groovy" | addIgnore(ExpressionStatement, ASTComparatorCategory.LOCATION_IGNORE_LIST)
        "MapParameters_Issue55.groovy" | addIgnore(ExpressionStatement, ASTComparatorCategory.LOCATION_IGNORE_LIST)
        "MemberAccess_Issue14_1.groovy" | _
        "MethodBody_Issue7_1.groovy" | _
        "MethodCall_Issue15_1.groovy" | addIgnore(ExpressionStatement, ASTComparatorCategory.LOCATION_IGNORE_LIST)
        "New_Issue47_1.groovy" | _
        "Operators_Issue9_1.groovy" | _
        "Binary_and_Unary_Operators.groovy" | _
        "ParenthesisExpression_Issue24_1.groovy" | _
        "Script_Issue50_1.groovy" | addIgnore(ExpressionStatement, ASTComparatorCategory.LOCATION_IGNORE_LIST)
        "Statements_Issue17_1.groovy" | addIgnore([IfStatement, ExpressionStatement], ASTComparatorCategory.LOCATION_IGNORE_LIST)
        "Statements_Issue58_1.groovy" | addIgnore([IfStatement, ExpressionStatement], ASTComparatorCategory.LOCATION_IGNORE_LIST)
        "SubscriptOperator.groovy" | _
        "AnnotationDeclaration.groovy" | _
        "TernaryAndElvis_Issue57.groovy" | _
        "TestClass1.groovy" | _
        "ThrowDeclarations_Issue_28_1.groovy" | _
        "Assert_Statements.groovy" | _
        "Unicode_Identifiers.groovy" | _
        "ClassMembers_String_Method_Name.groovy" | _
        "ScriptPart_String_Method_Name.groovy" | _
        "Multiline_GString.groovy" | _
        "Unescape_String_Literals_Issue7.groovy" | _
        "GString-closure-and-expression_issue12.groovy" | _
        "Slashy_Strings.groovy" | _
        "Expression_Precedence.groovy" | _
        "Tuples_issue13.groovy" | _
        "Dollar_Slashy_Strings.groovy" | _
        "Dollar_Slashy_GStrings.groovy" | _
        "SyntheticPublic_issue19.groovy" | _
        "Traits_issue21.groovy" | _
        "EmptyScript.groovy" | _
        "SemiColonScript.groovy" | _
        "Enums_issue31.groovy" | _
        "CallExpression_issue33_1.groovy" | _
        "CallExpression_issue33_2.groovy" | addIgnore(Parameter, ASTComparatorCategory.LOCATION_IGNORE_LIST)
        "Switch-Case_issue36.groovy" | addIgnore(CaseStatement, ASTComparatorCategory.LOCATION_IGNORE_LIST)
        "ScriptSupport.groovy" | addIgnore([FieldNode, PropertyNode], ASTComparatorCategory.LOCATION_IGNORE_LIST)

    }


    @Unroll
    def "test Groovy in Action 2nd Edition for #path"() {
        def filename = path;

        setup:
        def file = new File("$RESOURCES_PATH/GroovyInAction2/$path")
        def moduleNodeNew = new Main(Configuration.NEW).process(file)
        def moduleNodeOld = new Main(Configuration.OLD).process(file)
        def moduleNodeOld2 = new Main(Configuration.OLD).process(file)
        config = config.is(_) ? ASTComparatorCategory.DEFAULT_CONFIGURATION : config

        expect:
        moduleNodeNew
        moduleNodeOld
        ASTComparatorCategory.apply(config) {
            assert moduleNodeOld == moduleNodeOld2
        }
        and:
        ASTWriter.astToString(moduleNodeNew) == ASTWriter.astToString(moduleNodeOld2)
        and:
        ASTComparatorCategory.apply(config) {
            assert moduleNodeNew == moduleNodeOld, "Fail in $path"
        }

        where:
        path | config
        "appD/Listing_D_01_GStrings.groovy" | addIgnore(AssertStatement, ASTComparatorCategory.LOCATION_IGNORE_LIST)
        "appD/Listing_D_02_Lists.groovy" | addIgnore(AssertStatement, ASTComparatorCategory.LOCATION_IGNORE_LIST)
        "appD/Listing_D_03_Closures.groovy" | addIgnore([AssertStatement, Parameter], ASTComparatorCategory.LOCATION_IGNORE_LIST)
        "appD/Listing_D_04_Regex.groovy" | addIgnore(AssertStatement, ASTComparatorCategory.LOCATION_IGNORE_LIST)
        "appD/Listing_D_05_GPath.groovy" | addIgnore(AssertStatement, ASTComparatorCategory.LOCATION_IGNORE_LIST)

        "chap01/Listing_01_01_Gold.groovy" | addIgnore([AssertStatement, ReturnStatement], ASTComparatorCategory.LOCATION_IGNORE_LIST)
        "chap01/snippet0101_customers.groovy" | addIgnore(AssertStatement, ASTComparatorCategory.LOCATION_IGNORE_LIST)
        "chap01/snippet0101_fileLineNumbers.groovy" | addIgnore(AssertStatement, ASTComparatorCategory.LOCATION_IGNORE_LIST)
        "chap01/snippet0101_printPackageNames.groovy" | addIgnore(AssertStatement, ASTComparatorCategory.LOCATION_IGNORE_LIST)
        "chap01/snippet0101_printPackageNamesGpath.groovy" | addIgnore(AssertStatement, ASTComparatorCategory.LOCATION_IGNORE_LIST)
        "chap01/snippet0102_printGroovyWebSiteCount.groovy" | addIgnore(AssertStatement, ASTComparatorCategory.LOCATION_IGNORE_LIST)
        "chap01/snippet0103_googleIpAdr.groovy" | addIgnore(AssertStatement, ASTComparatorCategory.LOCATION_IGNORE_LIST)

        "chap02/Book.groovy" | addIgnore(AssertStatement, ASTComparatorCategory.LOCATION_IGNORE_LIST)
        "chap02/Listing_02_01_Assertions.groovy" | addIgnore(AssertStatement, ASTComparatorCategory.LOCATION_IGNORE_LIST)
        "chap02/Listing_02_03_BookScript.groovy" | addIgnore(AssertStatement, ASTComparatorCategory.LOCATION_IGNORE_LIST)
        "chap02/Listing_02_04_BookBean.groovy" | addIgnore(AssertStatement, ASTComparatorCategory.LOCATION_IGNORE_LIST)
        "chap02/Listing_02_05_ImmutableBook.groovy" | addIgnore(AssertStatement, ASTComparatorCategory.LOCATION_IGNORE_LIST)
        "chap02/Listing_02_06_Grab.groovy" | addIgnore(AssertStatement, ASTComparatorCategory.LOCATION_IGNORE_LIST)
//FIXME         "chap02/Listing_02_07_Clinks.groovy" | addIgnore(AssertStatement, ASTComparatorCategory.LOCATION_IGNORE_LIST)
        "chap02/Listing_02_08_ControlStructures.groovy" | addIgnore([AssertStatement, WhileStatement, ForStatement, BreakStatement], ASTComparatorCategory.LOCATION_IGNORE_LIST)
        "chap02/snippet0201_comments.groovy" | addIgnore(AssertStatement, ASTComparatorCategory.LOCATION_IGNORE_LIST)
        "chap02/snippet0202_failing_assert.groovy" | addIgnore(AssertStatement, ASTComparatorCategory.LOCATION_IGNORE_LIST)
        "chap02/snippet0203_clinks_java.groovy" | addIgnore(AssertStatement, ASTComparatorCategory.LOCATION_IGNORE_LIST)
        "chap02/snippet0203_gstring.groovy" | addIgnore(AssertStatement, ASTComparatorCategory.LOCATION_IGNORE_LIST)
        "chap02/snippet0203_int_usage.groovy" | addIgnore(AssertStatement, ASTComparatorCategory.LOCATION_IGNORE_LIST)
        "chap02/snippet0203_map_usage.groovy" | addIgnore(AssertStatement, ASTComparatorCategory.LOCATION_IGNORE_LIST)
        "chap02/snippet0203_range_usage.groovy" | addIgnore(AssertStatement, ASTComparatorCategory.LOCATION_IGNORE_LIST)
        "chap02/snippet0203_roman.groovy" | addIgnore(AssertStatement, ASTComparatorCategory.LOCATION_IGNORE_LIST)
        "chap02/snippet0204_evaluate_jdk7_only.groovy" | addIgnore(AssertStatement, ASTComparatorCategory.LOCATION_IGNORE_LIST)
        "chap02/snippet0204_evaluate_jdk8_only.groovy" | addIgnore(AssertStatement, ASTComparatorCategory.LOCATION_IGNORE_LIST)
        "chap02/snippet0204_failing_typechecked.groovy" | addIgnore(AssertStatement, ASTComparatorCategory.LOCATION_IGNORE_LIST)

        "chap03/extra_escaped_characters_table36.groovy" | addIgnore(AssertStatement, ASTComparatorCategory.LOCATION_IGNORE_LIST)
        "chap03/extra_method_operators_table34.groovy" | addIgnore(AssertStatement, ASTComparatorCategory.LOCATION_IGNORE_LIST)
//FIXME        "chap03/extra_numeric_literals_table32.groovy" | addIgnore(AssertStatement, ASTComparatorCategory.LOCATION_IGNORE_LIST)
        "chap03/extra_numerical_coercion_table310.groovy" | addIgnore(AssertStatement, ASTComparatorCategory.LOCATION_IGNORE_LIST)
//FIXME        "chap03/extra_optional_typing_table33.groovy" | addIgnore(AssertStatement, ASTComparatorCategory.LOCATION_IGNORE_LIST)
        "chap03/extra_primitive_values_table31.groovy" | addIgnore(AssertStatement, ASTComparatorCategory.LOCATION_IGNORE_LIST)
//FIXME        "chap03/Listing_03_01_PrimitiveMethodsObjectOperators.groovy" | addIgnore(AssertStatement, ASTComparatorCategory.LOCATION_IGNORE_LIST)
//FIXME        "chap03/Listing_03_02_ListMapCast.groovy" | addIgnore(AssertStatement, ASTComparatorCategory.LOCATION_IGNORE_LIST)
        "chap03/Listing_03_03_DefiningOperators.groovy" | addIgnore(AssertStatement, ASTComparatorCategory.LOCATION_IGNORE_LIST)
//FIXME        "chap03/Listing_03_04_DefiningGStrings.groovy" | addIgnore(AssertStatement, ASTComparatorCategory.LOCATION_IGNORE_LIST)
        "chap03/Listing_03_05_StringOperations.groovy" | addIgnore(AssertStatement, ASTComparatorCategory.LOCATION_IGNORE_LIST)
//FIXME        "chap03/Listing_03_06_RegexGStrings.groovy" | addIgnore(AssertStatement, ASTComparatorCategory.LOCATION_IGNORE_LIST)
        "chap03/Listing_03_07_RegularExpressions.groovy" | addIgnore(AssertStatement, ASTComparatorCategory.LOCATION_IGNORE_LIST)
//FIXME        "chap03/Listing_03_08_EachMatch.groovy" | addIgnore(AssertStatement, ASTComparatorCategory.LOCATION_IGNORE_LIST)
        "chap03/Listing_03_09_PatternReuse.groovy" | addIgnore(AssertStatement, ASTComparatorCategory.LOCATION_IGNORE_LIST)
        "chap03/Listing_03_10_PatternsClassification.groovy" | addIgnore(AssertStatement, ASTComparatorCategory.LOCATION_IGNORE_LIST)
//FIXME        "chap03/Listing_03_11_NumberMethodsGDK.groovy" | addIgnore(AssertStatement, ASTComparatorCategory.LOCATION_IGNORE_LIST)
        "chap03/snippet0301_autoboxing.groovy" | addIgnore(AssertStatement, ASTComparatorCategory.LOCATION_IGNORE_LIST)
        "chap03/snippet0304_GString_internals.groovy" | addIgnore(AssertStatement, ASTComparatorCategory.LOCATION_IGNORE_LIST)
        "chap03/snippet0304_stringbuffer.groovy" | addIgnore(AssertStatement, ASTComparatorCategory.LOCATION_IGNORE_LIST)
        "chap03/snippet0305_matcher_each_group.groovy" | addIgnore(AssertStatement, ASTComparatorCategory.LOCATION_IGNORE_LIST)
//FIXME        "chap03/snippet0305_matcher_groups.groovy" | addIgnore(AssertStatement, ASTComparatorCategory.LOCATION_IGNORE_LIST)
        "chap03/snippet0305_matcher_parallel_assignment.groovy" | addIgnore(AssertStatement, ASTComparatorCategory.LOCATION_IGNORE_LIST)
        "chap03/snippet0305_matcher_plain.groovy" | addIgnore(AssertStatement, ASTComparatorCategory.LOCATION_IGNORE_LIST)
//FIXME        "chap03/snippet0306_GDK_methods_for_numbers.groovy" | addIgnore(AssertStatement, ASTComparatorCategory.LOCATION_IGNORE_LIST)
    }

    @Unroll
    def "test by evaluating script: #path"() {
        def filename = path;

        setup:
        def file = new File("$RESOURCES_PATH/$path")
        def gsh = createGroovyShell(compilerConfiguration)


        expect:
        assertScript(gsh, file);

        where:
        path | compilerConfiguration
        "Assert_issue9.groovy" | CompilerConfiguration.DEFAULT

    }


    /*
	@Unroll
    def "test invalid class modifiers #path"() {
        expect:
        def file = new File("$RESOURCES_PATH/$path")

        def errorCollectorNew = new Main(Configuration.NEW).process(file).context.errorCollector
        def errorCollectorOld = new Main(Configuration.OLD).process(file).context.errorCollector
        def errorCollectorOld2 = new Main(Configuration.OLD).process(file).context.errorCollector


        def cl = { ErrorCollector errorCollector, int it -> def s = new StringWriter(); errorCollector.getError(it).write(new PrintWriter(s)); s.toString() }
        def errOld1 = (0..<errorCollectorOld.errorCount).collect cl.curry(errorCollectorOld)
        def errOld2 = (0..<errorCollectorOld2.errorCount).collect cl.curry(errorCollectorOld2)
        def errNew = (0..<errorCollectorNew.errorCount).collect cl.curry(errorCollectorNew)

        assert errOld1 == errOld2
        assert errOld1 == errNew

        where:
        path | output
        "ClassModifiersInvalid_Issue1_2.groovy" | _
        "ClassModifiersInvalid_Issue2_2.groovy" | _
    }
    */

    @Unroll
    def "test invalid files #path"() {
        when:
            def file = new File("$RESOURCES_PATH/$path")
        then:
            ! canLoad(file, Configuration.NEW) && ! canLoad(file, Configuration.OLD)
        where:
            path | output
            "Statement_Errors_1.groovy" | _
            "Statement_Errors_2.groovy" | _
            "Statement_Errors_3.groovy" | _
            "Statement_Errors_4.groovy" | _
            "Statement_Errors_5.groovy" | _
            "Statement_Errors_6.groovy" | _
            "Statement_Errors_7.groovy" | _
            "Statement_Errors_8.groovy" | _
            "ClassModifiersInvalid_Issue1_2.groovy" | _
            "ClassModifiersInvalid_Issue2_2.groovy" | _

    }


    def addIgnore(Class aClass, ArrayList<String> ignore, Map<Class, List<String>> c = null) {
        c = c ?: ASTComparatorCategory.DEFAULT_CONFIGURATION.clone() as Map<Class, List<String>>;
        c[aClass].addAll(ignore)
        c
    }

    def addIgnore(Collection<Class> aClass, ArrayList<String> ignore, Map<Class, List<String>> c = null) {
        c = c ?: ASTComparatorCategory.DEFAULT_CONFIGURATION.clone() as Map<Class, List<String>>;
        aClass.each { c[it].addAll(ignore) }
        c
    }

    boolean canLoad(File file, Configuration config) {
        def module = new Main(config).process(file)
        return module != null && ! module.context.errorCollector.hasErrors()
    }

    def createGroovyShell(CompilerConfiguration c) {
        CompilerConfiguration configuration = new CompilerConfiguration(c)
        configuration.pluginFactory = new Antlrv4PluginFactory()

        return new GroovyShell(configuration);
    }

    def assertScript(gsh, file) {
        def content = file.text;
        try {
            gsh.evaluate(content);

            log.info("Evaluated $file")

            return true;
        } catch (Throwable t) {
            log.info("Failed $file: ${t.getMessage()}");

            return false;
        }
    }
}

