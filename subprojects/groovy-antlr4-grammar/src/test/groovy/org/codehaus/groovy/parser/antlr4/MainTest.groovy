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

import org.codehaus.groovy.ast.*
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
        "Statements_Issue58_1.groovy" | addIgnore([IfStatement, ForStatement, ExpressionStatement], ASTComparatorCategory.LOCATION_IGNORE_LIST)
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
        "Expression_Span_Rows.groovy" | _
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
        "Closure_Call_Issue40.groovy" | _
        "CommandExpression_issue41.groovy" | _
        "SynchronizedStatement.groovy" | _
        "VarArg.groovy" | _
        "Join_Line_Escape_issue46.groovy" | _
        "Enums_Inner.groovy" | _
        "Interface.groovy" | _
        "ClassMembers_Issue3_3.groovy" | _
        "Switch-Case_issue36.groovy" | addIgnore(CaseStatement, ASTComparatorCategory.LOCATION_IGNORE_LIST)
        "ScriptSupport.groovy" | addIgnore([FieldNode, PropertyNode], ASTComparatorCategory.LOCATION_IGNORE_LIST)

    }


    @Unroll
    def "test grails-core-3 for #path"() {
        def filename = path;

        setup:
        def file = new File("$RESOURCES_PATH/grails-core-3/$path")
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
        "buildSrc/src/main/groovy/org/grails/gradle/GrailsBuildPlugin.groovy" | _

        "grails-async/src/main/groovy/grails/async/DelegateAsync.groovy" | _
        "grails-async/src/main/groovy/grails/async/Promise.groovy" | _
        "grails-async/src/main/groovy/grails/async/PromiseFactory.groovy" | _
        "grails-async/src/main/groovy/grails/async/PromiseList.groovy" | _
        "grails-async/src/main/groovy/grails/async/PromiseMap.groovy" | _
        "grails-async/src/main/groovy/grails/async/Promises.groovy" | _
        "grails-async/src/main/groovy/grails/async/decorator/PromiseDecorator.groovy" | _
        "grails-async/src/main/groovy/grails/async/decorator/PromiseDecoratorLookupStrategy.groovy" | _
        "grails-async/src/main/groovy/grails/async/decorator/PromiseDecoratorProvider.groovy" | _
        "grails-async/src/main/groovy/grails/async/factory/AbstractPromiseFactory.groovy" | _
        "grails-async/src/main/groovy/org/grails/async/decorator/PromiseDecorator.groovy" | _
        "grails-async/src/main/groovy/org/grails/async/decorator/PromiseDecoratorLookupStrategy.groovy" | _
        "grails-async/src/main/groovy/org/grails/async/decorator/PromiseDecoratorProvider.groovy" | _
        "grails-async/src/main/groovy/org/grails/async/factory/AbstractPromiseFactory.groovy" | _
        "grails-async/src/main/groovy/org/grails/async/factory/BoundPromise.groovy" | _
        "grails-async/src/main/groovy/org/grails/async/factory/SynchronousPromise.groovy" | _
        "grails-async/src/main/groovy/org/grails/async/factory/SynchronousPromiseFactory.groovy" | _
        "grails-async/src/main/groovy/org/grails/async/factory/gpars/GparsPromise.groovy" | _
        "grails-async/src/main/groovy/org/grails/async/factory/gpars/GparsPromiseFactory.groovy" | _
        "grails-async/src/main/groovy/org/grails/async/factory/gpars/LoggingPoolFactory.groovy" | _
        "grails-async/src/main/groovy/org/grails/async/factory/reactor/ReactorPromise.groovy" | _
        "grails-async/src/main/groovy/org/grails/async/factory/reactor/ReactorPromiseFactory.groovy" | _
        "grails-async/src/main/groovy/org/grails/async/transform/internal/DelegateAsyncUtils.groovy" | _
        "grails-async/src/test/groovy/grails/async/DelegateAsyncSpec.groovy" | _
        "grails-async/src/test/groovy/grails/async/PromiseListSpec.groovy" | addIgnore(ThrowStatement, ASTComparatorCategory.LOCATION_IGNORE_LIST)
        "grails-async/src/test/groovy/grails/async/PromiseMapSpec.groovy" | addIgnore(ThrowStatement, ASTComparatorCategory.LOCATION_IGNORE_LIST)
//FIXME        "grails-async/src/test/groovy/grails/async/PromiseSpec.groovy" | _
        "grails-async/src/test/groovy/grails/async/ReactorPromiseFactorySpec.groovy" | _
//FIXME        "grails-async/src/test/groovy/grails/async/SynchronousPromiseFactorySpec.groovy" | _

//        "grails-bootstrap/src/main/groovy/grails/build/proxy/SystemPropertiesAuthenticator.groovy" | _
        "grails-bootstrap/SystemPropertiesAuthenticator.groovy" | _
        "grails-bootstrap/src/main/groovy/grails/codegen/model/Model.groovy" | _
        "grails-bootstrap/src/main/groovy/grails/codegen/model/ModelBuilder.groovy" | _
        "grails-bootstrap/src/main/groovy/grails/config/ConfigMap.groovy" | _
        "grails-bootstrap/src/main/groovy/grails/io/IOUtils.groovy" | _
        "grails-bootstrap/src/main/groovy/grails/io/ResourceUtils.groovy" | _
        "grails-bootstrap/src/main/groovy/grails/io/support/SystemOutErrCapturer.groovy" | addIgnore(MethodNode, ASTComparatorCategory.LOCATION_IGNORE_LIST)
        "grails-bootstrap/src/main/groovy/grails/io/support/SystemStreamsRedirector.groovy" | _
        "grails-bootstrap/src/main/groovy/grails/plugins/GrailsVersionUtils.groovy" | _
        "grails-bootstrap/src/main/groovy/grails/plugins/VersionComparator.groovy" | _
        "grails-bootstrap/src/main/groovy/grails/util/BuildSettings.groovy" | _
        "grails-bootstrap/src/main/groovy/grails/util/CosineSimilarity.groovy" | _
        "grails-bootstrap/src/main/groovy/grails/util/Described.groovy" | _
        "grails-bootstrap/src/main/groovy/grails/util/Metadata.groovy" | _
        "grails-bootstrap/src/main/groovy/grails/util/Named.groovy" | _
        "grails-bootstrap/src/main/groovy/org/codehaus/groovy/grails/io/support/GrailsResourceUtils.groovy" | _
        "grails-bootstrap/src/main/groovy/org/codehaus/groovy/grails/io/support/Resource.groovy" | _
        "grails-bootstrap/src/main/groovy/org/codehaus/groovy/grails/plugins/GrailsPluginInfo.groovy" | _
//        "grails-bootstrap/src/main/groovy/org/grails/build/parsing/ScriptNameResolver.groovy" | _
        "grails-bootstrap/ScriptNameResolver.groovy" | _
        "grails-bootstrap/src/main/groovy/org/grails/config/CodeGenConfig.groovy" | _
        "grails-bootstrap/src/main/groovy/org/grails/config/NavigableMap.groovy" | _
        "grails-bootstrap/src/main/groovy/org/grails/exceptions/ExceptionUtils.groovy" | _
        "grails-bootstrap/src/main/groovy/org/grails/exceptions/reporting/CodeSnippetPrinter.groovy" | _
        "grails-bootstrap/src/main/groovy/org/grails/exceptions/reporting/DefaultStackTracePrinter.groovy" | addIgnore(ReturnStatement, ASTComparatorCategory.LOCATION_IGNORE_LIST)
        "grails-bootstrap/src/main/groovy/org/grails/exceptions/reporting/StackTracePrinter.groovy" | _
        "grails-bootstrap/src/main/groovy/org/grails/io/support/ByteArrayResource.groovy" | _
        "grails-bootstrap/src/main/groovy/org/grails/io/support/DevNullPrintStream.groovy" | _
        "grails-bootstrap/src/main/groovy/org/grails/io/support/FactoriesLoaderSupport.groovy" | _
        "grails-bootstrap/src/main/groovy/org/grails/io/support/MainClassFinder.groovy" | addIgnore(ReturnStatement, ASTComparatorCategory.LOCATION_IGNORE_LIST)
        "grails-bootstrap/src/main/groovy/org/grails/io/watch/FileExtensionFileChangeListener.groovy" | _
//        "grails-bootstrap/src/test/groovy/grails/build/logging/GrailsConsoleSpec.groovy" | _
        "grails-bootstrap/GrailsConsoleSpec.groovy" | _
        "grails-bootstrap/src/test/groovy/grails/config/ConfigMapSpec.groovy" | _
        "grails-bootstrap/src/test/groovy/grails/config/GrailsConfigSpec.groovy" | _
        "grails-bootstrap/src/test/groovy/grails/io/IOUtilsSpec.groovy" | _
        "grails-bootstrap/src/test/groovy/grails/util/EnvironmentTests.groovy" | _
        "grails-bootstrap/src/test/groovy/org/codehaus/groovy/grails/cli/parsing/CommandLineParserSpec.groovy" | _

        "grails-codecs/src/main/groovy/org/grails/plugins/codecs/Base64CodecExtensionMethods.groovy" | _
        "grails-codecs/src/main/groovy/org/grails/plugins/codecs/DigestUtils.groovy" | _
        "grails-codecs/src/main/groovy/org/grails/plugins/codecs/HexCodecExtensionMethods.groovy" | _
        "grails-codecs/src/main/groovy/org/grails/plugins/codecs/MD5BytesCodecExtensionMethods.groovy" | _
        "grails-codecs/src/main/groovy/org/grails/plugins/codecs/MD5CodecExtensionMethods.groovy" | _
        "grails-codecs/src/main/groovy/org/grails/plugins/codecs/SHA1BytesCodecExtensionMethods.groovy" | _
        "grails-codecs/src/main/groovy/org/grails/plugins/codecs/SHA1CodecExtensionMethods.groovy" | _
        "grails-codecs/src/main/groovy/org/grails/plugins/codecs/SHA256BytesCodecExtensionMethods.groovy" | _
        "grails-codecs/src/main/groovy/org/grails/plugins/codecs/SHA256CodecExtensionMethods.groovy" | _
        "grails-codecs/src/test/groovy/org/grails/web/codecs/Base64CodecTests.groovy" | _
        "grails-codecs/src/test/groovy/org/grails/web/codecs/HexCodecTests.groovy" | _
        "grails-codecs/src/test/groovy/org/grails/web/codecs/MD5BytesCodecTests.groovy" | _
        "grails-codecs/src/test/groovy/org/grails/web/codecs/MD5CodecTests.groovy" | _
        "grails-codecs/src/test/groovy/org/grails/web/codecs/SHA1BytesCodecTests.groovy" | _
        "grails-codecs/src/test/groovy/org/grails/web/codecs/SHA1CodecTests.groovy" | _
        "grails-codecs/src/test/groovy/org/grails/web/codecs/SHA256BytesCodec.groovy" | _
        "grails-codecs/src/test/groovy/org/grails/web/codecs/SHA256CodecTests.groovy" | _

        "grails-compat/src/main/groovy/org/codehaus/groovy/grails/beans/factory/GenericBeanFactoryAccessor.groovy" | _
        "grails-compat/src/main/groovy/org/codehaus/groovy/grails/commons/AbstractGrailsClass.groovy" | _
        "grails-compat/src/main/groovy/org/codehaus/groovy/grails/commons/AbstractInjectableGrailsClass.groovy" | _
        "grails-compat/src/main/groovy/org/codehaus/groovy/grails/commons/AnnotationDomainClassArtefactHandler.groovy" | _
        "grails-compat/src/main/groovy/org/codehaus/groovy/grails/commons/ArtefactHandlerAdapter.groovy" | _
        "grails-compat/src/main/groovy/org/codehaus/groovy/grails/commons/BootstrapArtefactHandler.groovy" | _
        "grails-compat/src/main/groovy/org/codehaus/groovy/grails/commons/ClassPropertyFetcher.groovy" | _
        "grails-compat/src/main/groovy/org/codehaus/groovy/grails/commons/ControllerArtefactHandler.groovy" | _
        "grails-compat/src/main/groovy/org/codehaus/groovy/grails/commons/DefaultGrailsApplication.groovy" | _
        "grails-compat/src/main/groovy/org/codehaus/groovy/grails/commons/DefaultGrailsBootstrapClass.groovy" | _
        "grails-compat/src/main/groovy/org/codehaus/groovy/grails/commons/DefaultGrailsControllerClass.groovy" | _
        "grails-compat/src/main/groovy/org/codehaus/groovy/grails/commons/DefaultGrailsDomainClass.groovy" | _
        "grails-compat/src/main/groovy/org/codehaus/groovy/grails/commons/DefaultGrailsDomainClassProperty.groovy" | _
        "grails-compat/src/main/groovy/org/codehaus/groovy/grails/commons/DefaultGrailsServiceClass.groovy" | _
        "grails-compat/src/main/groovy/org/codehaus/groovy/grails/commons/DefaultGrailsTagLibClass.groovy" | _
        "grails-compat/src/main/groovy/org/codehaus/groovy/grails/commons/DefaultGrailsUrlMappingsClass.groovy" | _
        "grails-compat/src/main/groovy/org/codehaus/groovy/grails/commons/ExternalGrailsDomainClass.groovy" | _
        "grails-compat/src/main/groovy/org/codehaus/groovy/grails/commons/GrailsBootstrapClass.groovy" | _
        "grails-compat/src/main/groovy/org/codehaus/groovy/grails/commons/InjectableGrailsClass.groovy" | _
        "grails-compat/src/main/groovy/org/codehaus/groovy/grails/commons/cfg/GrailsConfig.groovy" | _
        "grails-compat/src/main/groovy/org/codehaus/groovy/grails/commons/spring/DefaultRuntimeSpringConfiguration.groovy" | _
        "grails-compat/src/main/groovy/org/codehaus/groovy/grails/commons/spring/GrailsRuntimeConfigurator.groovy" | _
        "grails-compat/src/main/groovy/org/codehaus/groovy/grails/commons/spring/GrailsWebApplicationContext.groovy" | _
        "grails-compat/src/main/groovy/org/codehaus/groovy/grails/commons/spring/RuntimeSpringConfiguration.groovy" | _
        "grails-compat/src/main/groovy/org/codehaus/groovy/grails/compiler/injection/AbstractArtefactTypeAstTransformation.groovy" | _
        "grails-compat/src/main/groovy/org/codehaus/groovy/grails/compiler/injection/AllArtefactClassInjector.groovy" | _
        "grails-compat/src/main/groovy/org/codehaus/groovy/grails/compiler/injection/AnnotatedClassInjector.groovy" | _
        "grails-compat/src/main/groovy/org/codehaus/groovy/grails/compiler/injection/ClassInjector.groovy" | _
        "grails-compat/src/main/groovy/org/codehaus/groovy/grails/compiler/injection/DefaultGrailsDomainClassInjector.groovy" | _
        "grails-compat/src/main/groovy/org/codehaus/groovy/grails/compiler/injection/EntityASTTransformation.groovy" | _
        "grails-compat/src/main/groovy/org/codehaus/groovy/grails/compiler/injection/GrailsAwareClassLoader.groovy" | _
        "grails-compat/src/main/groovy/org/codehaus/groovy/grails/compiler/injection/GrailsAwareInjectionOperation.groovy" | _
        "grails-compat/src/main/groovy/org/codehaus/groovy/grails/core/io/DefaultResourceLocator.groovy" | _
        "grails-compat/src/main/groovy/org/codehaus/groovy/grails/domain/GrailsDomainClassCleaner.groovy" | _
        "grails-compat/src/main/groovy/org/codehaus/groovy/grails/exceptions/GrailsConfigurationException.groovy" | _
        "grails-compat/src/main/groovy/org/codehaus/groovy/grails/exceptions/GrailsDomainException.groovy" | _
        "grails-compat/src/main/groovy/org/codehaus/groovy/grails/exceptions/GrailsException.groovy" | _
        "grails-compat/src/main/groovy/org/codehaus/groovy/grails/exceptions/GrailsRuntimeException.groovy" | _
        "grails-compat/src/main/groovy/org/codehaus/groovy/grails/io/support/GrailsIOUtils.groovy" | _
        "grails-compat/src/main/groovy/org/codehaus/groovy/grails/io/support/IOUtils.groovy" | _
        "grails-compat/src/main/groovy/org/codehaus/groovy/grails/lifecycle/ShutdownOperations.groovy" | _
        "grails-compat/src/main/groovy/org/codehaus/groovy/grails/orm/support/TransactionManagerAware.groovy" | _
        "grails-compat/src/main/groovy/org/codehaus/groovy/grails/plugins/AbstractGrailsPluginManager.groovy" | _
        "grails-compat/src/main/groovy/org/codehaus/groovy/grails/plugins/DefaultGrailsPluginManager.groovy" | _
        "grails-compat/src/main/groovy/org/codehaus/groovy/grails/plugins/DomainClassPluginSupport.groovy" | _
        "grails-compat/src/main/groovy/org/codehaus/groovy/grails/plugins/GrailsVersionUtils.groovy" | _
        "grails-compat/src/main/groovy/org/codehaus/groovy/grails/plugins/support/aware/ClassLoaderAware.groovy" | _
        "grails-compat/src/main/groovy/org/codehaus/groovy/grails/plugins/support/aware/GrailsConfigurationAware.groovy" | _
        "grails-compat/src/main/groovy/org/codehaus/groovy/grails/support/ClassEditor.groovy" | _
        "grails-compat/src/main/groovy/org/codehaus/groovy/grails/support/PersistenceContextInterceptor.groovy" | _
        "grails-compat/src/main/groovy/org/codehaus/groovy/grails/support/SoftThreadLocalMap.groovy" | _
        "grails-compat/src/main/groovy/org/codehaus/groovy/grails/support/proxy/EntityProxyHandler.groovy" | _
        "grails-compat/src/main/groovy/org/codehaus/groovy/grails/support/proxy/ProxyHandler.groovy" | _
        "grails-compat/src/main/groovy/org/codehaus/groovy/grails/validation/AbstractVetoingConstraint.groovy" | _
        "grails-compat/src/main/groovy/org/codehaus/groovy/grails/validation/DefaultConstraintEvaluator.groovy" | _
        "grails-compat/src/main/groovy/org/codehaus/groovy/grails/validation/GrailsDomainClassValidator.groovy" | _
        "grails-compat/src/main/groovy/org/codehaus/groovy/grails/validation/VetoingConstraint.groovy" | _
        "grails-compat/src/main/groovy/org/codehaus/groovy/grails/validation/exceptions/ConstraintException.groovy" | _
        "grails-compat/src/main/groovy/org/codehaus/groovy/grails/web/binding/GrailsWebDataBinder.groovy" | _
        "grails-compat/src/main/groovy/org/codehaus/groovy/grails/web/context/GrailsConfigUtils.groovy" | _
        "grails-compat/src/main/groovy/org/codehaus/groovy/grails/web/context/ServletContextHolder.groovy" | _
        "grails-compat/src/main/groovy/org/codehaus/groovy/grails/web/errors/GrailsExceptionResolver.groovy" | _
        "grails-compat/src/main/groovy/org/codehaus/groovy/grails/web/mapping/CachingLinkGenerator.groovy" | _
        "grails-compat/src/main/groovy/org/codehaus/groovy/grails/web/metaclass/ForwardMethod.groovy" | _
        "grails-compat/src/main/groovy/org/codehaus/groovy/grails/web/metaclass/RenderDynamicMethod.groovy" | _
        "grails-compat/src/main/groovy/org/codehaus/groovy/grails/web/servlet/DelegatingApplicationAttributes.groovy" | _
        "grails-compat/src/main/groovy/org/codehaus/groovy/grails/web/servlet/GrailsFlashScope.groovy" | _
        "grails-compat/src/main/groovy/org/codehaus/groovy/grails/web/servlet/GrailsUrlPathHelper.groovy" | _
        "grails-compat/src/main/groovy/org/codehaus/groovy/grails/web/servlet/HttpHeaders.groovy" | _
        "grails-compat/src/main/groovy/org/codehaus/groovy/grails/web/servlet/WrappedResponseHolder.groovy" | _
        "grails-compat/src/main/groovy/org/codehaus/groovy/grails/web/servlet/mvc/GrailsDispatcherServlet.groovy" | _
        "grails-compat/src/main/groovy/org/codehaus/groovy/grails/web/servlet/mvc/GrailsHttpSession.groovy" | _
        "grails-compat/src/main/groovy/org/codehaus/groovy/grails/web/servlet/mvc/GrailsParameterMap.groovy" | _
        "grails-compat/src/main/groovy/org/codehaus/groovy/grails/web/servlet/mvc/GrailsWebRequest.groovy" | _
        "grails-compat/src/main/groovy/org/codehaus/groovy/grails/web/servlet/mvc/RedirectEventListener.groovy" | _
        "grails-compat/src/main/groovy/org/codehaus/groovy/grails/web/servlet/mvc/exceptions/ControllerExecutionException.groovy" | _
        "grails-compat/src/main/groovy/org/codehaus/groovy/grails/web/servlet/mvc/exceptions/GrailsMVCException.groovy" | _
        "grails-compat/src/main/groovy/org/codehaus/groovy/grails/web/util/WebUtils.groovy" | _
        "grails-compat/src/main/groovy/org/grails/databinding/SimpleDataBinder.groovy" | _
        "grails-compat/src/main/groovy/org/grails/databinding/SimpleMapDataBindingSource.groovy" | _


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
        "chap02/Listing_02_07_Clinks.groovy" | addIgnore(AssertStatement, ASTComparatorCategory.LOCATION_IGNORE_LIST)
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
        "chap03/extra_numeric_literals_table32.groovy" | addIgnore(AssertStatement, ASTComparatorCategory.LOCATION_IGNORE_LIST)
        "chap03/extra_numerical_coercion_table310.groovy" | addIgnore(AssertStatement, ASTComparatorCategory.LOCATION_IGNORE_LIST)
        "chap03/extra_optional_typing_table33.groovy" | addIgnore(AssertStatement, ASTComparatorCategory.LOCATION_IGNORE_LIST)
        "chap03/extra_primitive_values_table31.groovy" | addIgnore(AssertStatement, ASTComparatorCategory.LOCATION_IGNORE_LIST)
        "chap03/Listing_03_01_PrimitiveMethodsObjectOperators.groovy" | addIgnore(AssertStatement, ASTComparatorCategory.LOCATION_IGNORE_LIST)
        "chap03/Listing_03_02_ListMapCast.groovy" | addIgnore(AssertStatement, ASTComparatorCategory.LOCATION_IGNORE_LIST)
        "chap03/Listing_03_03_DefiningOperators.groovy" | addIgnore(AssertStatement, ASTComparatorCategory.LOCATION_IGNORE_LIST)
        "chap03/Listing_03_04_DefiningGStrings.groovy" | addIgnore(AssertStatement, ASTComparatorCategory.LOCATION_IGNORE_LIST)
        "chap03/Listing_03_05_StringOperations.groovy" | addIgnore(AssertStatement, ASTComparatorCategory.LOCATION_IGNORE_LIST)
        "chap03/Listing_03_06_RegexGStrings.groovy" | addIgnore(AssertStatement, ASTComparatorCategory.LOCATION_IGNORE_LIST)
        "chap03/Listing_03_07_RegularExpressions.groovy" | addIgnore(AssertStatement, ASTComparatorCategory.LOCATION_IGNORE_LIST)
        "chap03/Listing_03_08_EachMatch.groovy" | addIgnore(AssertStatement, ASTComparatorCategory.LOCATION_IGNORE_LIST)
        "chap03/Listing_03_09_PatternReuse.groovy" | addIgnore(AssertStatement, ASTComparatorCategory.LOCATION_IGNORE_LIST)
        "chap03/Listing_03_10_PatternsClassification.groovy" | addIgnore(AssertStatement, ASTComparatorCategory.LOCATION_IGNORE_LIST)
        "chap03/Listing_03_11_NumberMethodsGDK.groovy" | addIgnore(AssertStatement, ASTComparatorCategory.LOCATION_IGNORE_LIST)
        "chap03/snippet0301_autoboxing.groovy" | addIgnore(AssertStatement, ASTComparatorCategory.LOCATION_IGNORE_LIST)
        "chap03/snippet0304_GString_internals.groovy" | addIgnore(AssertStatement, ASTComparatorCategory.LOCATION_IGNORE_LIST)
        "chap03/snippet0304_stringbuffer.groovy" | addIgnore(AssertStatement, ASTComparatorCategory.LOCATION_IGNORE_LIST)
        "chap03/snippet0305_matcher_each_group.groovy" | addIgnore(AssertStatement, ASTComparatorCategory.LOCATION_IGNORE_LIST)
        "chap03/snippet0305_matcher_groups.groovy" | addIgnore(AssertStatement, ASTComparatorCategory.LOCATION_IGNORE_LIST)
        "chap03/snippet0305_matcher_parallel_assignment.groovy" | addIgnore(AssertStatement, ASTComparatorCategory.LOCATION_IGNORE_LIST)
        "chap03/snippet0305_matcher_plain.groovy" | addIgnore(AssertStatement, ASTComparatorCategory.LOCATION_IGNORE_LIST)
        "chap03/snippet0306_GDK_methods_for_numbers.groovy" | addIgnore(AssertStatement, ASTComparatorCategory.LOCATION_IGNORE_LIST)

        "chap04/extra_EnumRange.groovy" | addIgnore(AssertStatement, ASTComparatorCategory.LOCATION_IGNORE_LIST)
        "chap04/extra_ListCast.groovy" | addIgnore(AssertStatement, ASTComparatorCategory.LOCATION_IGNORE_LIST)
        "chap04/extra_ListTable.groovy" | addIgnore(AssertStatement, ASTComparatorCategory.LOCATION_IGNORE_LIST)
        "chap04/extra_Map_as.groovy" | addIgnore(AssertStatement, ASTComparatorCategory.LOCATION_IGNORE_LIST)
        "chap04/extra_Map_group.groovy" | addIgnore(AssertStatement, ASTComparatorCategory.LOCATION_IGNORE_LIST)
        "chap04/extra_MaxMinSum.groovy" | addIgnore(AssertStatement, ASTComparatorCategory.LOCATION_IGNORE_LIST)
        "chap04/extra_SplitList.groovy" | addIgnore(AssertStatement, ASTComparatorCategory.LOCATION_IGNORE_LIST)
        "chap04/Listing_04_01_range_declarations.groovy" | addIgnore(AssertStatement, ASTComparatorCategory.LOCATION_IGNORE_LIST)
        "chap04/Listing_04_02_ranges_are_objects.groovy" | addIgnore([AssertStatement, ThrowStatement], ASTComparatorCategory.LOCATION_IGNORE_LIST)
        "chap04/Listing_04_03_custom_ranges.groovy" | addIgnore(AssertStatement, ASTComparatorCategory.LOCATION_IGNORE_LIST)
        "chap04/Listing_04_04_list_declarations.groovy" | addIgnore(AssertStatement, ASTComparatorCategory.LOCATION_IGNORE_LIST)
        "chap04/Listing_04_05_list_subscript_operator.groovy" | addIgnore(AssertStatement, ASTComparatorCategory.LOCATION_IGNORE_LIST)
        "chap04/Listing_04_06_list_add_remove.groovy" | addIgnore(AssertStatement, ASTComparatorCategory.LOCATION_IGNORE_LIST)
        "chap04/Listing_04_07_lists_control_structures.groovy" | addIgnore(AssertStatement, ASTComparatorCategory.LOCATION_IGNORE_LIST)
        "chap04/Listing_04_08_list_content_manipulation.groovy" | addIgnore(AssertStatement, ASTComparatorCategory.LOCATION_IGNORE_LIST)
        "chap04/Listing_04_09_list_other_methods.groovy" | addIgnore(AssertStatement, ASTComparatorCategory.LOCATION_IGNORE_LIST)
        "chap04/Listing_04_10_list_quicksort.groovy" | addIgnore(AssertStatement, ASTComparatorCategory.LOCATION_IGNORE_LIST)
        "chap04/Listing_04_11_list_mapreduce.groovy" | addIgnore(AssertStatement, ASTComparatorCategory.LOCATION_IGNORE_LIST)
        "chap04/Listing_04_12_map_declarations.groovy" | addIgnore(AssertStatement, ASTComparatorCategory.LOCATION_IGNORE_LIST)
        "chap04/Listing_04_13_map_accessors.groovy" | addIgnore(AssertStatement, ASTComparatorCategory.LOCATION_IGNORE_LIST)
        "chap04/Listing_04_14_map_query_methods.groovy" | addIgnore(AssertStatement, ASTComparatorCategory.LOCATION_IGNORE_LIST)
        "chap04/Listing_04_15_map_iteration.groovy" | addIgnore(AssertStatement, ASTComparatorCategory.LOCATION_IGNORE_LIST)
        "chap04/Listing_04_16_map_content.groovy" | addIgnore(AssertStatement, ASTComparatorCategory.LOCATION_IGNORE_LIST)
        "chap04/Listing_04_17_map_example.groovy" | addIgnore(AssertStatement, ASTComparatorCategory.LOCATION_IGNORE_LIST)
        "chap04/snippet0402_ListAsSet.groovy" | addIgnore(AssertStatement, ASTComparatorCategory.LOCATION_IGNORE_LIST)
        "chap04/snippet0402_ListRemoveNulls.groovy" | addIgnore(AssertStatement, ASTComparatorCategory.LOCATION_IGNORE_LIST)
        "chap04/snippet0402_ListStreams_jdk8_plus.groovy" | addIgnore(AssertStatement, ASTComparatorCategory.LOCATION_IGNORE_LIST)
        "chap04/snippet0403_Map_Ctor_Expression.groovy" | addIgnore(AssertStatement, ASTComparatorCategory.LOCATION_IGNORE_LIST)
        "chap04/snippet0403_Map_Ctor_Unquoted.groovy" | addIgnore(AssertStatement, ASTComparatorCategory.LOCATION_IGNORE_LIST)
        "chap04/snippet0403_Map_MapReduce.groovy" | addIgnore(AssertStatement, ASTComparatorCategory.LOCATION_IGNORE_LIST)
        "chap04/snippet0403_Map_String_accessors.groovy" | addIgnore(AssertStatement, ASTComparatorCategory.LOCATION_IGNORE_LIST)

        "chap05/extra_Closure_delegate.groovy" | addIgnore(AssertStatement, ASTComparatorCategory.LOCATION_IGNORE_LIST)
        "chap05/extra_Closure_myWith.groovy" | addIgnore(AssertStatement, ASTComparatorCategory.LOCATION_IGNORE_LIST)
        "chap05/extra_ClosureProperty.groovy" | addIgnore(AssertStatement, ASTComparatorCategory.LOCATION_IGNORE_LIST)
        "chap05/Listing_05_01_closure_simple_declaration.groovy" | addIgnore(AssertStatement, ASTComparatorCategory.LOCATION_IGNORE_LIST)
        "chap05/Listing_05_02_simple_method_closure.groovy" | addIgnore(AssertStatement, ASTComparatorCategory.LOCATION_IGNORE_LIST)
        "chap05/Listing_05_03_multi_method_closure.groovy" | addIgnore(AssertStatement, ASTComparatorCategory.LOCATION_IGNORE_LIST)
        "chap05/Listing_05_04_closure_all_declarations.groovy" | addIgnore(AssertStatement, ASTComparatorCategory.LOCATION_IGNORE_LIST)
        "chap05/Listing_05_05_simple_closure_calling.groovy" | addIgnore(AssertStatement, ASTComparatorCategory.LOCATION_IGNORE_LIST)
        "chap05/Listing_05_06_calling_closures.groovy" | addIgnore(AssertStatement, ASTComparatorCategory.LOCATION_IGNORE_LIST)
        "chap05/Listing_05_07_simple_currying.groovy" | addIgnore(AssertStatement, ASTComparatorCategory.LOCATION_IGNORE_LIST)
        "chap05/Listing_05_08_logging_curry_example.groovy" | addIgnore(AssertStatement, ASTComparatorCategory.LOCATION_IGNORE_LIST)
        "chap05/Listing_05_09_closure_scope.groovy" | addIgnore(AssertStatement, ASTComparatorCategory.LOCATION_IGNORE_LIST)
        "chap05/Listing_05_10_closure_accumulator.groovy" | addIgnore(AssertStatement, ASTComparatorCategory.LOCATION_IGNORE_LIST)
        "chap05/Listing_05_11_visitor_pattern.groovy" | addIgnore(AssertStatement, ASTComparatorCategory.LOCATION_IGNORE_LIST)
        "chap05/snippet0501_envelope.groovy.txt" | addIgnore(AssertStatement, ASTComparatorCategory.LOCATION_IGNORE_LIST)
        "chap05/snippet0504_closure_default_params.groovy" | addIgnore(AssertStatement, ASTComparatorCategory.LOCATION_IGNORE_LIST)
        "chap05/snippet0504_closure_isCase.groovy" | addIgnore(AssertStatement, ASTComparatorCategory.LOCATION_IGNORE_LIST)
        "chap05/snippet0504_closure_paramcount.groovy" | addIgnore(AssertStatement, ASTComparatorCategory.LOCATION_IGNORE_LIST)
        "chap05/snippet0505_map_with.groovy" | addIgnore(AssertStatement, ASTComparatorCategory.LOCATION_IGNORE_LIST)
        "chap05/snippet0505_scoping.groovy" | addIgnore(AssertStatement, ASTComparatorCategory.LOCATION_IGNORE_LIST)
        "chap05/snippet0506_closure_return.groovy" | addIgnore(AssertStatement, ASTComparatorCategory.LOCATION_IGNORE_LIST)
        "chap05/snippet0507_closure_composition.groovy" | addIgnore(AssertStatement, ASTComparatorCategory.LOCATION_IGNORE_LIST)
        "chap05/snippet0508_memoize.groovy" | addIgnore(AssertStatement, ASTComparatorCategory.LOCATION_IGNORE_LIST)
        "chap05/snippet0509_trampoline.groovy" | addIgnore(AssertStatement, ASTComparatorCategory.LOCATION_IGNORE_LIST)

        "chap06/extra_if_return.groovy" | addIgnore(AssertStatement, ASTComparatorCategory.LOCATION_IGNORE_LIST)
        "chap06/extra_in_operator.groovy" | addIgnore(AssertStatement, ASTComparatorCategory.LOCATION_IGNORE_LIST)
        "chap06/extra_switch_return.groovy" | addIgnore(AssertStatement, ASTComparatorCategory.LOCATION_IGNORE_LIST)
        "chap06/Listing_06_01_groovy_truth.groovy" | addIgnore(AssertStatement, ASTComparatorCategory.LOCATION_IGNORE_LIST)
        "chap06/Listing_06_02_assignment_bug.groovy" | addIgnore(AssertStatement, ASTComparatorCategory.LOCATION_IGNORE_LIST)
        "chap06/Listing_06_03_if_then_else.groovy" | addIgnore(AssertStatement, ASTComparatorCategory.LOCATION_IGNORE_LIST)
        "chap06/Listing_06_04_conditional_operator.groovy" | addIgnore(AssertStatement, ASTComparatorCategory.LOCATION_IGNORE_LIST)
        "chap06/Listing_06_05_switch_basic.groovy" | addIgnore(AssertStatement, ASTComparatorCategory.LOCATION_IGNORE_LIST)
        "chap06/Listing_06_06_switch_advanced.groovy" | addIgnore(AssertStatement, ASTComparatorCategory.LOCATION_IGNORE_LIST)
        "chap06/Listing_06_07_assert_host.groovy" | addIgnore(AssertStatement, ASTComparatorCategory.LOCATION_IGNORE_LIST)
        "chap06/Listing_06_08_while.groovy" | addIgnore(AssertStatement, ASTComparatorCategory.LOCATION_IGNORE_LIST)
        "chap06/Listing_06_09_for.groovy" | addIgnore(AssertStatement, ASTComparatorCategory.LOCATION_IGNORE_LIST)
        "chap06/Listing_06_10_break_continue.groovy" | addIgnore([AssertStatement, ContinueStatement], ASTComparatorCategory.LOCATION_IGNORE_LIST)
        "chap06/Listing_06_11_exception_example.groovy" | addIgnore(AssertStatement, ASTComparatorCategory.LOCATION_IGNORE_LIST)
        "chap06/snippet0602_bad_file_read.groovy" | addIgnore(AssertStatement, ASTComparatorCategory.LOCATION_IGNORE_LIST)
        "chap06/snippet0602_bad_file_read_with_message.groovy" | addIgnore(AssertStatement, ASTComparatorCategory.LOCATION_IGNORE_LIST)
        "chap06/snippet0602_failing_assert.groovy" | addIgnore(AssertStatement, ASTComparatorCategory.LOCATION_IGNORE_LIST)
        "chap06/snippet0603_each_loop_iterate.groovy" | addIgnore(AssertStatement, ASTComparatorCategory.LOCATION_IGNORE_LIST)
        "chap06/snippet0603_file_iterate_lines.groovy" | addIgnore(AssertStatement, ASTComparatorCategory.LOCATION_IGNORE_LIST)
        "chap06/snippet0603_for_loop_iterate.groovy" | addIgnore(AssertStatement, ASTComparatorCategory.LOCATION_IGNORE_LIST)
        "chap06/snippet0603_null_iterate.groovy" | addIgnore(AssertStatement, ASTComparatorCategory.LOCATION_IGNORE_LIST)
        "chap06/snippet0603_object_iterate.groovy" | addIgnore(AssertStatement, ASTComparatorCategory.LOCATION_IGNORE_LIST)
        "chap06/snippet0603_regex_iterate_match.groovy" | addIgnore(AssertStatement, ASTComparatorCategory.LOCATION_IGNORE_LIST)
        "chap06/snippet0604_multicatch.groovy" | addIgnore(AssertStatement, ASTComparatorCategory.LOCATION_IGNORE_LIST)

        "chap07/business/Vendor.groovy" | addIgnore(AssertStatement, ASTComparatorCategory.LOCATION_IGNORE_LIST)
        "chap07/Listing_07_01_Declaring_Variables.groovy" | addIgnore([AssertStatement, MethodNode], ASTComparatorCategory.LOCATION_IGNORE_LIST)
        "chap07/Listing_07_02_TypeBreaking_Assignment.groovy" | addIgnore(AssertStatement, ASTComparatorCategory.LOCATION_IGNORE_LIST)
        "chap07/Listing_07_03_Referencing_Fields.groovy" | addIgnore(AssertStatement, ASTComparatorCategory.LOCATION_IGNORE_LIST)
        "chap07/Listing_07_04_Overriding_Field_Access.groovy" | addIgnore(AssertStatement, ASTComparatorCategory.LOCATION_IGNORE_LIST)
        "chap07/Listing_07_05_Declaring_Methods.groovy" | addIgnore(AssertStatement, ASTComparatorCategory.LOCATION_IGNORE_LIST)
        "chap07/Listing_07_06_Declaring_Parameters.groovy" | addIgnore(AssertStatement, ASTComparatorCategory.LOCATION_IGNORE_LIST)
        "chap07/Listing_07_07_Parameter_Usages.groovy" | addIgnore(AssertStatement, ASTComparatorCategory.LOCATION_IGNORE_LIST)
        "chap07/Listing_07_08_Safe_Dereferencing.groovy" | addIgnore(AssertStatement, ASTComparatorCategory.LOCATION_IGNORE_LIST)
        "chap07/Listing_07_09_Instantiation.groovy" | addIgnore(AssertStatement, ASTComparatorCategory.LOCATION_IGNORE_LIST)
        "chap07/Listing_07_10_Instantiation_Named.groovy" | addIgnore(AssertStatement, ASTComparatorCategory.LOCATION_IGNORE_LIST)
        "chap07/Listing_07_11_Classes.groovy" | addIgnore(AssertStatement, ASTComparatorCategory.LOCATION_IGNORE_LIST)
        "chap07/Listing_07_13_Import.groovy" | addIgnore(AssertStatement, ASTComparatorCategory.LOCATION_IGNORE_LIST)
        "chap07/Listing_07_14_Import_As_BugFix.groovy" | addIgnore(AssertStatement, ASTComparatorCategory.LOCATION_IGNORE_LIST)
        "chap07/Listing_07_15_Import_As_NameClash.groovy" | addIgnore(AssertStatement, ASTComparatorCategory.LOCATION_IGNORE_LIST)
        "chap07/Listing_07_16_Multimethods.groovy" | addIgnore(AssertStatement, ASTComparatorCategory.LOCATION_IGNORE_LIST)
        "chap07/Listing_07_17_MultiEquals.groovy" | addIgnore(AssertStatement, ASTComparatorCategory.LOCATION_IGNORE_LIST)
        "chap07/Listing_07_18_Traits.groovy" | addIgnore(AssertStatement, ASTComparatorCategory.LOCATION_IGNORE_LIST)
        "chap07/Listing_07_19_Declaring_Beans.groovy" | addIgnore(AssertStatement, ASTComparatorCategory.LOCATION_IGNORE_LIST)
        "chap07/Listing_07_20_Calling_Beans.groovy" | addIgnore(AssertStatement, ASTComparatorCategory.LOCATION_IGNORE_LIST)
        "chap07/Listing_07_21_Calling_Beans_Advanced.groovy" | addIgnore(AssertStatement, ASTComparatorCategory.LOCATION_IGNORE_LIST)
        "chap07/Listing_07_22_Property_Methods.groovy" | addIgnore(AssertStatement, ASTComparatorCategory.LOCATION_IGNORE_LIST)
        "chap07/Listing_07_23_Expando.groovy" | addIgnore(AssertStatement, ASTComparatorCategory.LOCATION_IGNORE_LIST)
        "chap07/Listing_07_24_GPath.groovy" | addIgnore(AssertStatement, ASTComparatorCategory.LOCATION_IGNORE_LIST)
        "chap07/snippet0703_Implicit_Closure_To_SAM.groovy" | addIgnore(AssertStatement, ASTComparatorCategory.LOCATION_IGNORE_LIST)
        "chap07/snippet0705_Spread_List.groovy" | addIgnore(AssertStatement, ASTComparatorCategory.LOCATION_IGNORE_LIST)
        "chap07/snippet0705_Spread_Map.groovy" | addIgnore(AssertStatement, ASTComparatorCategory.LOCATION_IGNORE_LIST)
        "chap07/snippet0705_Spread_Range.groovy" | addIgnore(AssertStatement, ASTComparatorCategory.LOCATION_IGNORE_LIST)
        "chap07/thirdparty/MathLib.groovy" | addIgnore(AssertStatement, ASTComparatorCategory.LOCATION_IGNORE_LIST)
        "chap07/thirdparty2/MathLib.groovy" | addIgnore(AssertStatement, ASTComparatorCategory.LOCATION_IGNORE_LIST)

        "chap08/custom/Custom.groovy" | addIgnore(AssertStatement, ASTComparatorCategory.LOCATION_IGNORE_LIST)
        "chap08/custom/useCustom.groovy" | addIgnore(AssertStatement, ASTComparatorCategory.LOCATION_IGNORE_LIST)
        "chap08/failing_Listing_08_15_EMC_static.groovy" | addIgnore(AssertStatement, ASTComparatorCategory.LOCATION_IGNORE_LIST)
        "chap08/failing_Listing_08_16_EMC_super.groovy" | addIgnore(AssertStatement, ASTComparatorCategory.LOCATION_IGNORE_LIST)
        "chap08/groovy/runtime/metaclass/custom/CustomMetaClass.groovy" | addIgnore(AssertStatement, ASTComparatorCategory.LOCATION_IGNORE_LIST)
        "chap08/Listing_08_01_method_missing.groovy" | addIgnore(AssertStatement, ASTComparatorCategory.LOCATION_IGNORE_LIST)
        "chap08/Listing_08_02_mini_gorm.groovy" | addIgnore(AssertStatement, ASTComparatorCategory.LOCATION_IGNORE_LIST)
        "chap08/Listing_08_03_property_missing.groovy" | addIgnore(AssertStatement, ASTComparatorCategory.LOCATION_IGNORE_LIST)
        "chap08/Listing_08_04_bin_property.groovy" | addIgnore(AssertStatement, ASTComparatorCategory.LOCATION_IGNORE_LIST)
        "chap08/Listing_08_05_closure_dynamic.groovy" | addIgnore(AssertStatement, ASTComparatorCategory.LOCATION_IGNORE_LIST)
        "chap08/Listing_08_06_property_method.groovy" | addIgnore(AssertStatement, ASTComparatorCategory.LOCATION_IGNORE_LIST)
        "chap08/Listing_08_07_MetaClass_jdk7_only.groovy" | addIgnore(AssertStatement, ASTComparatorCategory.LOCATION_IGNORE_LIST)
        "chap08/Listing_08_07_MetaClass_jdk8_plus.groovy" | addIgnore(AssertStatement, ASTComparatorCategory.LOCATION_IGNORE_LIST)
        "chap08/Listing_08_08_ProxyMetaClass.groovy" | addIgnore(AssertStatement, ASTComparatorCategory.LOCATION_IGNORE_LIST)
        "chap08/Listing_08_09_Expando.groovy" | addIgnore(AssertStatement, ASTComparatorCategory.LOCATION_IGNORE_LIST)
        "chap08/Listing_08_10_EMC.groovy" | addIgnore(AssertStatement, ASTComparatorCategory.LOCATION_IGNORE_LIST)
        "chap08/Listing_08_11_EMC_Groovy_Class.groovy" | addIgnore(AssertStatement, ASTComparatorCategory.LOCATION_IGNORE_LIST)
        "chap08/Listing_08_12_EMC_Groovy_Object.groovy" | addIgnore(AssertStatement, ASTComparatorCategory.LOCATION_IGNORE_LIST)
        "chap08/Listing_08_13_EMC_Java_Object.groovy" | addIgnore(AssertStatement, ASTComparatorCategory.LOCATION_IGNORE_LIST)
        "chap08/Listing_08_14_EMC_Builder.groovy" | addIgnore(AssertStatement, ASTComparatorCategory.LOCATION_IGNORE_LIST)
        "chap08/Listing_08_15_EMC_static.groovy" | addIgnore(AssertStatement, ASTComparatorCategory.LOCATION_IGNORE_LIST)
        "chap08/Listing_08_16_EMC_super.groovy" | addIgnore(AssertStatement, ASTComparatorCategory.LOCATION_IGNORE_LIST)
        "chap08/Listing_08_17_EMC_hooks.groovy" | addIgnore(AssertStatement, ASTComparatorCategory.LOCATION_IGNORE_LIST)
        "chap08/Listing_08_18_Existing_Categories.groovy" | addIgnore(AssertStatement, ASTComparatorCategory.LOCATION_IGNORE_LIST)
        "chap08/Listing_08_19_Marshal.groovy" | addIgnore(AssertStatement, ASTComparatorCategory.LOCATION_IGNORE_LIST)
        "chap08/Listing_08_20_MarshalCategory.groovy" | addIgnore(AssertStatement, ASTComparatorCategory.LOCATION_IGNORE_LIST)
        "chap08/Listing_08_21_Test_Mixin.groovy" | addIgnore(AssertStatement, ASTComparatorCategory.LOCATION_IGNORE_LIST)
        "chap08/Listing_08_22_Sieve_Mixin.groovy" | addIgnore(AssertStatement, ASTComparatorCategory.LOCATION_IGNORE_LIST)
        "chap08/Listing_08_23_Millimeter.groovy" | addIgnore(AssertStatement, ASTComparatorCategory.LOCATION_IGNORE_LIST)
        "chap08/Listing_08_24_create_factory.groovy" | addIgnore(AssertStatement, ASTComparatorCategory.LOCATION_IGNORE_LIST)
        "chap08/Listing_08_25_fake_assign.groovy" | addIgnore(AssertStatement, ASTComparatorCategory.LOCATION_IGNORE_LIST)
        "chap08/Listing_08_26_restore_emc.groovy" | addIgnore(AssertStatement, ASTComparatorCategory.LOCATION_IGNORE_LIST)
        "chap08/Listing_08_27_intercept_cache_invoke.groovy" | addIgnore(AssertStatement, ASTComparatorCategory.LOCATION_IGNORE_LIST)

        "chap09/Listing_09_01_ToStringDetective.groovy" | addIgnore(AssertStatement, ASTComparatorCategory.LOCATION_IGNORE_LIST)
        "chap09/Listing_09_02_ToStringSleuth.groovy" | addIgnore(AssertStatement, ASTComparatorCategory.LOCATION_IGNORE_LIST)
        "chap09/Listing_09_03_EqualsAndHashCode.groovy" | addIgnore(AssertStatement, ASTComparatorCategory.LOCATION_IGNORE_LIST)
        "chap09/Listing_09_04_TupleConstructor.groovy" | addIgnore(AssertStatement, ASTComparatorCategory.LOCATION_IGNORE_LIST)
        "chap09/Listing_09_05_Lazy.groovy" | addIgnore(AssertStatement, ASTComparatorCategory.LOCATION_IGNORE_LIST)
        "chap09/Listing_09_06_IndexedProperty.groovy" | addIgnore(AssertStatement, ASTComparatorCategory.LOCATION_IGNORE_LIST)
        "chap09/Listing_09_07_InheritConstructors.groovy" | addIgnore(AssertStatement, ASTComparatorCategory.LOCATION_IGNORE_LIST)
        "chap09/Listing_09_08_Sortable.groovy" | addIgnore(AssertStatement, ASTComparatorCategory.LOCATION_IGNORE_LIST)
        "chap09/Listing_09_09_Builder.groovy" | addIgnore(AssertStatement, ASTComparatorCategory.LOCATION_IGNORE_LIST)
        "chap09/Listing_09_10_Canonical.groovy" | addIgnore(AssertStatement, ASTComparatorCategory.LOCATION_IGNORE_LIST)
        "chap09/Listing_09_11_Immutable.groovy" | addIgnore(AssertStatement, ASTComparatorCategory.LOCATION_IGNORE_LIST)
        "chap09/Listing_09_12_Delegate.groovy" | addIgnore(AssertStatement, ASTComparatorCategory.LOCATION_IGNORE_LIST)
        "chap09/Listing_09_13_Singleton.groovy" | addIgnore(AssertStatement, ASTComparatorCategory.LOCATION_IGNORE_LIST)
        "chap09/Listing_09_14_Memoized.groovy" | addIgnore(AssertStatement, ASTComparatorCategory.LOCATION_IGNORE_LIST)
        "chap09/Listing_09_15_TailRecursive.groovy" | addIgnore(AssertStatement, ASTComparatorCategory.LOCATION_IGNORE_LIST)
        "chap09/Listing_09_16_Log.groovy" | addIgnore(AssertStatement, ASTComparatorCategory.LOCATION_IGNORE_LIST)
        "chap09/Listing_09_17_Synchronized.groovy" | addIgnore(AssertStatement, ASTComparatorCategory.LOCATION_IGNORE_LIST)
        "chap09/Listing_09_18_SynchronizedCustomLock.groovy" | addIgnore(AssertStatement, ASTComparatorCategory.LOCATION_IGNORE_LIST)
        "chap09/Listing_09_19_ReadWriteLock.groovy" | addIgnore(AssertStatement, ASTComparatorCategory.LOCATION_IGNORE_LIST)
        "chap09/Listing_09_20_AutoClone.groovy" | addIgnore(AssertStatement, ASTComparatorCategory.LOCATION_IGNORE_LIST)
        "chap09/Listing_09_21_AutoCloneCopyConstructor.groovy" | addIgnore(AssertStatement, ASTComparatorCategory.LOCATION_IGNORE_LIST)
        "chap09/Listing_09_22_AutoExternalize.groovy" | addIgnore(AssertStatement, ASTComparatorCategory.LOCATION_IGNORE_LIST)
        "chap09/Listing_09_23_TimedInterrupt.groovy" | addIgnore(AssertStatement, ASTComparatorCategory.LOCATION_IGNORE_LIST)
        "chap09/Listing_09_24_ThreadInterrupt.groovy" | addIgnore(AssertStatement, ASTComparatorCategory.LOCATION_IGNORE_LIST)
        "chap09/Listing_09_25_ConditionalInterrupt.groovy" | addIgnore(AssertStatement, ASTComparatorCategory.LOCATION_IGNORE_LIST)
        "chap09/Listing_09_26_Field.groovy" | addIgnore(AssertStatement, ASTComparatorCategory.LOCATION_IGNORE_LIST)
        "chap09/Listing_09_27_BaseScript.groovy" | addIgnore(AssertStatement, ASTComparatorCategory.LOCATION_IGNORE_LIST)
        "chap09/Listing_09_28_AstByHand.groovy" | addIgnore(AssertStatement, ASTComparatorCategory.LOCATION_IGNORE_LIST)
        "chap09/Listing_09_29_AstByHandWithUtils.groovy" | addIgnore(AssertStatement, ASTComparatorCategory.LOCATION_IGNORE_LIST)
        "chap09/Listing_09_30_AstBuildFromSpec.groovy" | addIgnore(AssertStatement, ASTComparatorCategory.LOCATION_IGNORE_LIST)
        "chap09/Listing_09_31_AstBuildFromString.groovy" | addIgnore(AssertStatement, ASTComparatorCategory.LOCATION_IGNORE_LIST)
        "chap09/Listing_09_32_AstBuildFromStringMixed.groovy" | addIgnore(AssertStatement, ASTComparatorCategory.LOCATION_IGNORE_LIST)
        "chap09/Listing_09_33_AstBuildFromCode.groovy" | addIgnore(AssertStatement, ASTComparatorCategory.LOCATION_IGNORE_LIST)
        "chap09/Listing_09_34_GreeterMainTransform.groovy" | addIgnore(AssertStatement, ASTComparatorCategory.LOCATION_IGNORE_LIST)
        "chap09/Listing_09_35_GreeterMainTransform2.groovy" | addIgnore(AssertStatement, ASTComparatorCategory.LOCATION_IGNORE_LIST)
        "chap09/Listing_09_38_AstTesting1.groovy" | addIgnore(AssertStatement, ASTComparatorCategory.LOCATION_IGNORE_LIST)
        "chap09/Listing_09_39_AstTesting2.groovy" | addIgnore(AssertStatement, ASTComparatorCategory.LOCATION_IGNORE_LIST)
        "chap09/Listing_09_40_AstTesting3.groovy" | addIgnore(AssertStatement, ASTComparatorCategory.LOCATION_IGNORE_LIST)
        "chap09/Listing_09_41_AstTesting4.groovy" | addIgnore(AssertStatement, ASTComparatorCategory.LOCATION_IGNORE_LIST)
        "chap09/Listing_09_42_AstTesting5.groovy" | addIgnore(AssertStatement, ASTComparatorCategory.LOCATION_IGNORE_LIST)
        "chap09/settings.gradle" | addIgnore(AssertStatement, ASTComparatorCategory.LOCATION_IGNORE_LIST)
        "chap09/snippet0902_autoCloneDefault.txt" | addIgnore(AssertStatement, ASTComparatorCategory.LOCATION_IGNORE_LIST)
        "chap09/snippet0902_autoCloneSerialization.txt" | addIgnore(AssertStatement, ASTComparatorCategory.LOCATION_IGNORE_LIST)
        "chap09/snippet0902_autoExternalize.txt" | addIgnore(AssertStatement, ASTComparatorCategory.LOCATION_IGNORE_LIST)
        "chap09/snippet0902_fieldEquivalent.txt" | addIgnore(AssertStatement, ASTComparatorCategory.LOCATION_IGNORE_LIST)
        "chap09/snippet0902_mapCreation.txt" | addIgnore(AssertStatement, ASTComparatorCategory.LOCATION_IGNORE_LIST)
        "chap09/snippet0902_noisySetDelegateByHand.txt" | addIgnore(AssertStatement, ASTComparatorCategory.LOCATION_IGNORE_LIST)
        "chap09/snippet0902_noisySetInheritance.txt" | addIgnore(AssertStatement, ASTComparatorCategory.LOCATION_IGNORE_LIST)
        "chap09/snippet0902_nonTailCallReverseList.txt" | addIgnore(AssertStatement, ASTComparatorCategory.LOCATION_IGNORE_LIST)
        "chap09/snippet0902_readWriteByHand.txt" | addIgnore(AssertStatement, ASTComparatorCategory.LOCATION_IGNORE_LIST)
        "chap09/snippet0902_readWriteLock.txt" | addIgnore(AssertStatement, ASTComparatorCategory.LOCATION_IGNORE_LIST)
        "chap09/snippet0902_singletonByHand.txt" | addIgnore(AssertStatement, ASTComparatorCategory.LOCATION_IGNORE_LIST)
        "chap09/snippet0902_toStringEquivalent.txt" | addIgnore(AssertStatement, ASTComparatorCategory.LOCATION_IGNORE_LIST)
        "chap09/snippet0903_greeterExpanded.txt" | addIgnore(AssertStatement, ASTComparatorCategory.LOCATION_IGNORE_LIST)
        "chap09/snippet0903_greeterScript.txt" | addIgnore(AssertStatement, ASTComparatorCategory.LOCATION_IGNORE_LIST)
        "chap09/snippet0903_localMain.txt" | addIgnore(AssertStatement, ASTComparatorCategory.LOCATION_IGNORE_LIST)
        "chap09/snippet0903_localMainTransformation.txt" | addIgnore(AssertStatement, ASTComparatorCategory.LOCATION_IGNORE_LIST)
        "chap09/snippet0905_GetCompiledTimeScript.txt" | addIgnore(AssertStatement, ASTComparatorCategory.LOCATION_IGNORE_LIST)
        "chap09/src/main/groovy/regina/CompiledAtASTTransformation.groovy" | addIgnore(AssertStatement, ASTComparatorCategory.LOCATION_IGNORE_LIST)
        "chap09/src/test/groovy/regina/CompiledAtASTTransformationTest.groovy" | addIgnore(AssertStatement, ASTComparatorCategory.LOCATION_IGNORE_LIST)

        "chap10/extra1004_RuntimeGroovyDispatch.groovy" | addIgnore(AssertStatement, ASTComparatorCategory.LOCATION_IGNORE_LIST)
        "chap10/Listing_10_01_Duck.groovy" | addIgnore(AssertStatement, ASTComparatorCategory.LOCATION_IGNORE_LIST)
        "chap10/Listing_10_02_failing_Typo.groovy" | addIgnore(AssertStatement, ASTComparatorCategory.LOCATION_IGNORE_LIST)
        "chap10/Listing_10_03_ClassTC.groovy" | addIgnore(AssertStatement, ASTComparatorCategory.LOCATION_IGNORE_LIST)
        "chap10/Listing_10_04_OneMethodTC.groovy" | addIgnore(AssertStatement, ASTComparatorCategory.LOCATION_IGNORE_LIST)
        "chap10/Listing_10_05_CompileTimeTypo.groovy" | addIgnore(AssertStatement, ASTComparatorCategory.LOCATION_IGNORE_LIST)
        "chap10/Listing_10_06_MethodNameTypo.groovy" | addIgnore(AssertStatement, ASTComparatorCategory.LOCATION_IGNORE_LIST)
        "chap10/Listing_10_07_MethodArgsFlipped.groovy" | addIgnore(AssertStatement, ASTComparatorCategory.LOCATION_IGNORE_LIST)
        "chap10/Listing_10_08_InvalidAssignments.groovy" | addIgnore(AssertStatement, ASTComparatorCategory.LOCATION_IGNORE_LIST)
        "chap10/Listing_10_09_AssignmentsWithCoercion.groovy" | addIgnore(AssertStatement, ASTComparatorCategory.LOCATION_IGNORE_LIST)
        "chap10/Listing_10_10_DefField.groovy" | addIgnore(AssertStatement, ASTComparatorCategory.LOCATION_IGNORE_LIST)
        "chap10/Listing_10_11_InPlaceList.groovy" | addIgnore(AssertStatement, ASTComparatorCategory.LOCATION_IGNORE_LIST)
        "chap10/Listing_10_12_Generics.groovy" | addIgnore(AssertStatement, ASTComparatorCategory.LOCATION_IGNORE_LIST)
        "chap10/Listing_10_13_ListStyleCtorRuntime.groovy" | addIgnore(AssertStatement, ASTComparatorCategory.LOCATION_IGNORE_LIST)
        "chap10/Listing_10_14_ListStyleCtorTC.groovy" | addIgnore(AssertStatement, ASTComparatorCategory.LOCATION_IGNORE_LIST)
        "chap10/Listing_10_15_MapStyleCtorBad.groovy" | addIgnore(AssertStatement, ASTComparatorCategory.LOCATION_IGNORE_LIST)
        "chap10/Listing_10_16_ListStyleCtor.groovy" | addIgnore(AssertStatement, ASTComparatorCategory.LOCATION_IGNORE_LIST)
        "chap10/Listing_10_17_ListStyleCtorFixed.groovy" | addIgnore(AssertStatement, ASTComparatorCategory.LOCATION_IGNORE_LIST)
        "chap10/Listing_10_18_CodeAsData.groovy" | addIgnore(AssertStatement, ASTComparatorCategory.LOCATION_IGNORE_LIST)
        "chap10/Listing_10_19_ClosuresBadReturnType.groovy" | addIgnore(AssertStatement, ASTComparatorCategory.LOCATION_IGNORE_LIST)
        "chap10/Listing_10_20_UserValidation.groovy" | addIgnore(AssertStatement, ASTComparatorCategory.LOCATION_IGNORE_LIST)
        "chap10/Listing_10_21_UserValidationTC.groovy" | addIgnore(AssertStatement, ASTComparatorCategory.LOCATION_IGNORE_LIST)
        "chap10/Listing_10_22_UserValidation_ExplicitTypes.groovy" | addIgnore(AssertStatement, ASTComparatorCategory.LOCATION_IGNORE_LIST)
        "chap10/Listing_10_23_UserValidation_SAM.groovy" | addIgnore(AssertStatement, ASTComparatorCategory.LOCATION_IGNORE_LIST)
        "chap10/Listing_10_24_UserValidation_ClosureParams.groovy" | addIgnore(AssertStatement, ASTComparatorCategory.LOCATION_IGNORE_LIST)
        "chap10/Listing_10_25_UserValidation_DSL.groovy" | addIgnore(AssertStatement, ASTComparatorCategory.LOCATION_IGNORE_LIST)
        "chap10/Listing_10_26_UserValidation_DelegatesTo.groovy" | addIgnore(AssertStatement, ASTComparatorCategory.LOCATION_IGNORE_LIST)
        "chap10/Listing_10_27_UserValidation_DelegatesToTarget.groovy" | addIgnore(AssertStatement, ASTComparatorCategory.LOCATION_IGNORE_LIST)
        "chap10/Listing_10_28_Category.groovy" | addIgnore(AssertStatement, ASTComparatorCategory.LOCATION_IGNORE_LIST)
        "chap10/Listing_10_29_EMC.groovy" | addIgnore(AssertStatement, ASTComparatorCategory.LOCATION_IGNORE_LIST)
        "chap10/Listing_10_30_Builder.groovy" | addIgnore(AssertStatement, ASTComparatorCategory.LOCATION_IGNORE_LIST)
        "chap10/Listing_10_31_MixedTypeChecking.groovy" | addIgnore(AssertStatement, ASTComparatorCategory.LOCATION_IGNORE_LIST)
        "chap10/Listing_10_32_Skip.groovy" | addIgnore(AssertStatement, ASTComparatorCategory.LOCATION_IGNORE_LIST)
        "chap10/Listing_10_33_FlowTyping.groovy" | addIgnore(AssertStatement, ASTComparatorCategory.LOCATION_IGNORE_LIST)
        "chap10/Listing_10_34_FlowTypingOk.groovy" | addIgnore(AssertStatement, ASTComparatorCategory.LOCATION_IGNORE_LIST)
        "chap10/Listing_10_35_LUB.groovy" | addIgnore(AssertStatement, ASTComparatorCategory.LOCATION_IGNORE_LIST)
        "chap10/Listing_10_36_Condition.groovy" | addIgnore(AssertStatement, ASTComparatorCategory.LOCATION_IGNORE_LIST)
        "chap10/Listing_10_37_ClosureSharedVar.groovy" | addIgnore(AssertStatement, ASTComparatorCategory.LOCATION_IGNORE_LIST)
        "chap10/Listing_10_38_LubError.groovy" | addIgnore(AssertStatement, ASTComparatorCategory.LOCATION_IGNORE_LIST)
        "chap10/Listing_10_39_LubOk.groovy" | addIgnore(AssertStatement, ASTComparatorCategory.LOCATION_IGNORE_LIST)
        "chap10/Listing_10_40_FibBench.groovy" | addIgnore(AssertStatement, ASTComparatorCategory.LOCATION_IGNORE_LIST)
        "chap10/Listing_10_42_StaticCompileDispatch.groovy" | addIgnore(AssertStatement, ASTComparatorCategory.LOCATION_IGNORE_LIST)
        "chap10/Listing_10_43_MonkeyPatching.groovy" | addIgnore(AssertStatement, ASTComparatorCategory.LOCATION_IGNORE_LIST)
//FIXME        "chap10/Listing_10_44_BookingDSL.groovy" | addIgnore(AssertStatement, ASTComparatorCategory.LOCATION_IGNORE_LIST)
        "chap10/Listing_10_45_MultiValidation.groovy" | addIgnore(AssertStatement, ASTComparatorCategory.LOCATION_IGNORE_LIST)
        "chap10/Listing_10_46_RobotExtension.groovy" | addIgnore(AssertStatement, ASTComparatorCategory.LOCATION_IGNORE_LIST)
        "chap10/Listing_10_47_SQLExtension.groovy" | addIgnore(AssertStatement, ASTComparatorCategory.LOCATION_IGNORE_LIST)
        "chap10/snippet1003_GroovyGreeter.groovy" | addIgnore(AssertStatement, ASTComparatorCategory.LOCATION_IGNORE_LIST)
        "chap10/snippet1005_RobotMainTC.groovy" | addIgnore(AssertStatement, ASTComparatorCategory.LOCATION_IGNORE_LIST)
        "chap10/snippet1005_SqlMainTC.groovy" | addIgnore(AssertStatement, ASTComparatorCategory.LOCATION_IGNORE_LIST)
        "chap10/User.groovy" | addIgnore(AssertStatement, ASTComparatorCategory.LOCATION_IGNORE_LIST)

        "chap11/Listing_11_03_MarkupBuilderPlain.groovy" | addIgnore(AssertStatement, ASTComparatorCategory.LOCATION_IGNORE_LIST)
        "chap11/Listing_11_04_NodeBuilder.groovy" | addIgnore(AssertStatement, ASTComparatorCategory.LOCATION_IGNORE_LIST)
        "chap11/Listing_11_05_NodeBuilderLogic.groovy" | addIgnore(AssertStatement, ASTComparatorCategory.LOCATION_IGNORE_LIST)
        "chap11/Listing_11_06_MarkupBuilderLogic.groovy" | addIgnore(AssertStatement, ASTComparatorCategory.LOCATION_IGNORE_LIST)
        "chap11/Listing_11_07_MarkupBuilderHtml.groovy" | addIgnore(AssertStatement, ASTComparatorCategory.LOCATION_IGNORE_LIST)
        "chap11/Listing_11_08_StreamingMarkupBuilderLogic.groovy" | addIgnore(AssertStatement, ASTComparatorCategory.LOCATION_IGNORE_LIST)
        "chap11/Listing_11_10_PW_SwingBuilder.groovy" | addIgnore(AssertStatement, ASTComparatorCategory.LOCATION_IGNORE_LIST)
        "chap11/Listing_11_11_Swing_Widgets.groovy" | addIgnore(AssertStatement, ASTComparatorCategory.LOCATION_IGNORE_LIST)
        "chap11/Listing_11_12_Swing_Layout.groovy" | addIgnore(AssertStatement, ASTComparatorCategory.LOCATION_IGNORE_LIST)
        "chap11/Listing_11_13_Table_Demo.groovy" | addIgnore(AssertStatement, ASTComparatorCategory.LOCATION_IGNORE_LIST)
        "chap11/Listing_11_14_Binding.groovy" | addIgnore(AssertStatement, ASTComparatorCategory.LOCATION_IGNORE_LIST)
        "chap11/Listing_11_15_Plotter.groovy" | addIgnore(AssertStatement, ASTComparatorCategory.LOCATION_IGNORE_LIST)
        "chap11/Listing_11_16_Groovyfx.groovy" | addIgnore(AssertStatement, ASTComparatorCategory.LOCATION_IGNORE_LIST)
        "chap11/Listing_11_17_CalorieCounterBuilderSupport.groovy" | addIgnore(AssertStatement, ASTComparatorCategory.LOCATION_IGNORE_LIST)
        "chap11/Listing_11_18_CalorieCounterFactoryBuilderSupport.groovy" | addIgnore(AssertStatement, ASTComparatorCategory.LOCATION_IGNORE_LIST)
        "chap11/Listing_11_19_CalorieCounterByHand.groovy" | addIgnore(AssertStatement, ASTComparatorCategory.LOCATION_IGNORE_LIST)
        "chap11/snippet1103_MarkupWithHyphen.groovy" | addIgnore(AssertStatement, ASTComparatorCategory.LOCATION_IGNORE_LIST)
        "chap11/snippet1106_AntBuilderIf.groovy" | addIgnore(AssertStatement, ASTComparatorCategory.LOCATION_IGNORE_LIST)
        "chap11/snippet1107_Printer.groovy" | addIgnore(AssertStatement, ASTComparatorCategory.LOCATION_IGNORE_LIST)

        "chap12/Listing_12_01_info_jdk6_only.groovy" | addIgnore(AssertStatement, ASTComparatorCategory.LOCATION_IGNORE_LIST)
        "chap12/Listing_12_01_info_jdk7_only.groovy" | addIgnore(AssertStatement, ASTComparatorCategory.LOCATION_IGNORE_LIST)
        "chap12/Listing_12_01_info_jdk8_plus.groovy" | addIgnore(AssertStatement, ASTComparatorCategory.LOCATION_IGNORE_LIST)
        "chap12/Listing_12_02_properties.groovy" | addIgnore(AssertStatement, ASTComparatorCategory.LOCATION_IGNORE_LIST)
        "chap12/Listing_12_03_File_Iteration.groovy" | addIgnore(AssertStatement, ASTComparatorCategory.LOCATION_IGNORE_LIST)
        "chap12/Listing_12_04_Filesystem.groovy" | addIgnore(AssertStatement, ASTComparatorCategory.LOCATION_IGNORE_LIST)
        "chap12/Listing_12_05_Traversal.groovy" | addIgnore(AssertStatement, ASTComparatorCategory.LOCATION_IGNORE_LIST)
        "chap12/Listing_12_06_File_Read.groovy" | addIgnore(AssertStatement, ASTComparatorCategory.LOCATION_IGNORE_LIST)
        "chap12/Listing_12_07_File_Write.groovy" | addIgnore(AssertStatement, ASTComparatorCategory.LOCATION_IGNORE_LIST)
        "chap12/Listing_12_08_Writer_LeftShift.groovy" | addIgnore(AssertStatement, ASTComparatorCategory.LOCATION_IGNORE_LIST)
        "chap12/Listing_12_09_File_Transform_jdk7_plus.groovy" | addIgnore(AssertStatement, ASTComparatorCategory.LOCATION_IGNORE_LIST)
        "chap12/Listing_12_10_File_ObjectStreams.groovy" | addIgnore(AssertStatement, ASTComparatorCategory.LOCATION_IGNORE_LIST)
        "chap12/Listing_12_11_Temp_Dir.groovy" | addIgnore(AssertStatement, ASTComparatorCategory.LOCATION_IGNORE_LIST)
        "chap12/Listing_12_12_Threads.groovy" | addIgnore(AssertStatement, ASTComparatorCategory.LOCATION_IGNORE_LIST)
        "chap12/Listing_12_13_Processes_UnixCommands.groovy" | addIgnore(AssertStatement, ASTComparatorCategory.LOCATION_IGNORE_LIST)
        "chap12/Listing_12_14_Processes_ZipUnzip.groovy" | addIgnore(AssertStatement, ASTComparatorCategory.LOCATION_IGNORE_LIST)
        "chap12/Listing_12_15_SimpleTemplateEngine.groovy" | addIgnore(AssertStatement, ASTComparatorCategory.LOCATION_IGNORE_LIST)
        "chap12/Listing_12_16_GroovletExample.groovy" | addIgnore(AssertStatement, ASTComparatorCategory.LOCATION_IGNORE_LIST)
        "chap12/Listing_12_17_HelloWorldGroovlet.groovy" | addIgnore(AssertStatement, ASTComparatorCategory.LOCATION_IGNORE_LIST)
        "chap12/Listing_12_19_InspectGroovlet.groovy" | addIgnore(AssertStatement, ASTComparatorCategory.LOCATION_IGNORE_LIST)
        "chap12/Listing_12_20_HiLowGame.groovy" | addIgnore(AssertStatement, ASTComparatorCategory.LOCATION_IGNORE_LIST)
        "chap12/Listing_12_22_TemplateGroovlet.groovy" | addIgnore(AssertStatement, ASTComparatorCategory.LOCATION_IGNORE_LIST)
        "chap12/snippet1201_SlowTyping.groovy" | addIgnore(AssertStatement, ASTComparatorCategory.LOCATION_IGNORE_LIST)
        "chap12/snippet1201_UseCategory.groovy" | addIgnore(AssertStatement, ASTComparatorCategory.LOCATION_IGNORE_LIST)
        "chap12/snippet1202_base64.groovy" | addIgnore(AssertStatement, ASTComparatorCategory.LOCATION_IGNORE_LIST)

        "chap13/extra_NeoGremlinGraph.groovy" | addIgnore(AssertStatement, ASTComparatorCategory.LOCATION_IGNORE_LIST)
        "chap13/layering/AthleteApplication.groovy" | addIgnore(AssertStatement, ASTComparatorCategory.LOCATION_IGNORE_LIST)
        "chap13/layering/AthleteDAO.groovy" | addIgnore(AssertStatement, ASTComparatorCategory.LOCATION_IGNORE_LIST)
        "chap13/layering/DataAccessObject.groovy" | addIgnore(AssertStatement, ASTComparatorCategory.LOCATION_IGNORE_LIST)
        "chap13/layering/DbHelper.groovy" | addIgnore(AssertStatement, ASTComparatorCategory.LOCATION_IGNORE_LIST)
        "chap13/Listing_13_01_Connecting.groovy" | addIgnore(AssertStatement, ASTComparatorCategory.LOCATION_IGNORE_LIST)
        "chap13/Listing_13_02_ConnectingDataSource.groovy" | addIgnore(AssertStatement, ASTComparatorCategory.LOCATION_IGNORE_LIST)
        "chap13/Listing_13_03_Creating.groovy" | addIgnore(AssertStatement, ASTComparatorCategory.LOCATION_IGNORE_LIST)
        "chap13/Listing_13_05_Inserting.groovy" | addIgnore(AssertStatement, ASTComparatorCategory.LOCATION_IGNORE_LIST)
        "chap13/Listing_13_06_Reading.groovy" | addIgnore(AssertStatement, ASTComparatorCategory.LOCATION_IGNORE_LIST)
        "chap13/Listing_13_07_Updating.groovy" | addIgnore(AssertStatement, ASTComparatorCategory.LOCATION_IGNORE_LIST)
        "chap13/Listing_13_08_Delete.groovy" | addIgnore(AssertStatement, ASTComparatorCategory.LOCATION_IGNORE_LIST)
        "chap13/Listing_13_09_Transactions.groovy" | addIgnore(AssertStatement, ASTComparatorCategory.LOCATION_IGNORE_LIST)
        "chap13/Listing_13_10_Batching.groovy" | addIgnore(AssertStatement, ASTComparatorCategory.LOCATION_IGNORE_LIST)
        "chap13/Listing_13_11_Paging.groovy" | addIgnore(AssertStatement, ASTComparatorCategory.LOCATION_IGNORE_LIST)
        "chap13/Listing_13_12_Metadata.groovy" | addIgnore(AssertStatement, ASTComparatorCategory.LOCATION_IGNORE_LIST)
        "chap13/Listing_13_13_MoreMetadata.groovy" | addIgnore(AssertStatement, ASTComparatorCategory.LOCATION_IGNORE_LIST)
        "chap13/Listing_13_14_NamedOrdinal.groovy" | addIgnore(AssertStatement, ASTComparatorCategory.LOCATION_IGNORE_LIST)
        "chap13/Listing_13_15_StoredProcBasic.groovy" | addIgnore(AssertStatement, ASTComparatorCategory.LOCATION_IGNORE_LIST)
        "chap13/Listing_13_16_StoredProcParam.groovy" | addIgnore(AssertStatement, ASTComparatorCategory.LOCATION_IGNORE_LIST)
        "chap13/Listing_13_17_StoredProcInOut.groovy" | addIgnore(AssertStatement, ASTComparatorCategory.LOCATION_IGNORE_LIST)
        "chap13/Listing_13_18_DataSetBasics.groovy" | addIgnore(AssertStatement, ASTComparatorCategory.LOCATION_IGNORE_LIST)
        "chap13/Listing_13_19_DataSetFiltering.groovy" | addIgnore(AssertStatement, ASTComparatorCategory.LOCATION_IGNORE_LIST)
        "chap13/Listing_13_20_DataSetViews.groovy" | addIgnore(AssertStatement, ASTComparatorCategory.LOCATION_IGNORE_LIST)
        "chap13/Listing_13_25_AthleteAppMain.groovy" | addIgnore(AssertStatement, ASTComparatorCategory.LOCATION_IGNORE_LIST)
        "chap13/Listing_13_26_AthleteAppTest.groovy" | addIgnore(AssertStatement, ASTComparatorCategory.LOCATION_IGNORE_LIST)
        "chap13/Listing_13_27_MongoAthletes.groovy" | addIgnore(AssertStatement, ASTComparatorCategory.LOCATION_IGNORE_LIST)
        "chap13/Listing_13_28_NeoAthletes.groovy" | addIgnore(AssertStatement, ASTComparatorCategory.LOCATION_IGNORE_LIST)
        "chap13/Listing_13_29_NeoGremlin.groovy" | addIgnore(AssertStatement, ASTComparatorCategory.LOCATION_IGNORE_LIST)
        "chap13/snippet1301_ConnectingWithGrab.groovy" | addIgnore(AssertStatement, ASTComparatorCategory.LOCATION_IGNORE_LIST)
        "chap13/snippet1301_ConnectingWithInstance.groovy" | addIgnore(AssertStatement, ASTComparatorCategory.LOCATION_IGNORE_LIST)
        "chap13/snippet1301_ConnectingWithMap.groovy" | addIgnore(AssertStatement, ASTComparatorCategory.LOCATION_IGNORE_LIST)
        "chap13/snippet1301_ReadEachRow.groovy" | addIgnore(AssertStatement, ASTComparatorCategory.LOCATION_IGNORE_LIST)
        "chap13/snippet1301_ReadEachRowList.groovy" | addIgnore(AssertStatement, ASTComparatorCategory.LOCATION_IGNORE_LIST)
        "chap13/snippet1301_ReadQuery.groovy" | addIgnore(AssertStatement, ASTComparatorCategory.LOCATION_IGNORE_LIST)
        "chap13/snippet1301_ReadRows.groovy" | addIgnore(AssertStatement, ASTComparatorCategory.LOCATION_IGNORE_LIST)
        "chap13/util/DbUtil.groovy" | addIgnore(AssertStatement, ASTComparatorCategory.LOCATION_IGNORE_LIST)
        "chap13/util/MarathonRelationships.groovy" | addIgnore(AssertStatement, ASTComparatorCategory.LOCATION_IGNORE_LIST)
        "chap13/util/Neo4jUtil.groovy" | addIgnore(AssertStatement, ASTComparatorCategory.LOCATION_IGNORE_LIST)

        "chap14/Listing_14_02_DOM.groovy" | addIgnore(AssertStatement, ASTComparatorCategory.LOCATION_IGNORE_LIST)
        "chap14/Listing_14_03_DOM_Category.groovy" | addIgnore(AssertStatement, ASTComparatorCategory.LOCATION_IGNORE_LIST)
        "chap14/Listing_14_04_XmlParser.groovy" | addIgnore(AssertStatement, ASTComparatorCategory.LOCATION_IGNORE_LIST)
        "chap14/Listing_14_05_XmlSlurper.groovy" | addIgnore(AssertStatement, ASTComparatorCategory.LOCATION_IGNORE_LIST)
        "chap14/Listing_14_06_SAX.groovy" | addIgnore(AssertStatement, ASTComparatorCategory.LOCATION_IGNORE_LIST)
        "chap14/Listing_14_07_StAX.groovy" | addIgnore(AssertStatement, ASTComparatorCategory.LOCATION_IGNORE_LIST)
        "chap14/Listing_14_08_XmlBoiler.groovy" | addIgnore(AssertStatement, ASTComparatorCategory.LOCATION_IGNORE_LIST)
        "chap14/Listing_14_09_XmlStreamer.groovy" | addIgnore(AssertStatement, ASTComparatorCategory.LOCATION_IGNORE_LIST)
        "chap14/Listing_14_10_StreamedHtml.groovy" | addIgnore(AssertStatement, ASTComparatorCategory.LOCATION_IGNORE_LIST)
        "chap14/Listing_14_11_UpdateDomCategory.groovy" | addIgnore(AssertStatement, ASTComparatorCategory.LOCATION_IGNORE_LIST)
        "chap14/Listing_14_12_UpdateParser.groovy" | addIgnore(AssertStatement, ASTComparatorCategory.LOCATION_IGNORE_LIST)
        "chap14/Listing_14_13_UpdateSlurper.groovy" | addIgnore(AssertStatement, ASTComparatorCategory.LOCATION_IGNORE_LIST)
        "chap14/Listing_14_14_XPath.groovy" | addIgnore(AssertStatement, ASTComparatorCategory.LOCATION_IGNORE_LIST)
        "chap14/Listing_14_16_XPathTemplate.groovy" | addIgnore(AssertStatement, ASTComparatorCategory.LOCATION_IGNORE_LIST)
        "chap14/Listing_14_17_JsonParser.groovy" | addIgnore(AssertStatement, ASTComparatorCategory.LOCATION_IGNORE_LIST)
        "chap14/Listing_14_18_JsonBuilder.groovy" | addIgnore(AssertStatement, ASTComparatorCategory.LOCATION_IGNORE_LIST)
        "chap14/Listing_14_19_JsonBuilderLogic.groovy" | addIgnore(AssertStatement, ASTComparatorCategory.LOCATION_IGNORE_LIST)
        "chap14/Listing_14_20_JsonOutputAthlete.groovy" | addIgnore(AssertStatement, ASTComparatorCategory.LOCATION_IGNORE_LIST)
        "chap14/UpdateChecker.groovy" | addIgnore(AssertStatement, ASTComparatorCategory.LOCATION_IGNORE_LIST)

        "chap15/Listing_15_01_RSS_bbcnews.groovy" | addIgnore(AssertStatement, ASTComparatorCategory.LOCATION_IGNORE_LIST)
        "chap15/Listing_15_02_ATOM_devworks.groovy" | addIgnore(AssertStatement, ASTComparatorCategory.LOCATION_IGNORE_LIST)
        "chap15/Listing_15_03_REST_jira_url.groovy" | addIgnore(AssertStatement, ASTComparatorCategory.LOCATION_IGNORE_LIST)
        "chap15/Listing_15_04_REST_jira_httpb_get.groovy" | addIgnore(AssertStatement, ASTComparatorCategory.LOCATION_IGNORE_LIST)
        "chap15/Listing_15_05_REST_currency_httpb_get.groovy" | addIgnore(AssertStatement, ASTComparatorCategory.LOCATION_IGNORE_LIST)
        "chap15/Listing_15_06_REST_currency_httpb_post.groovy" | addIgnore(AssertStatement, ASTComparatorCategory.LOCATION_IGNORE_LIST)
        "chap15/Listing_15_07_REST_currency_jaxrs.groovy" | addIgnore(AssertStatement, ASTComparatorCategory.LOCATION_IGNORE_LIST)
        "chap15/Listing_15_08_REST_currency_jaxrs_proxy.groovy" | addIgnore(AssertStatement, ASTComparatorCategory.LOCATION_IGNORE_LIST)
        "chap15/Listing_15_09_XMLRPC_echo.groovy" | addIgnore(AssertStatement, ASTComparatorCategory.LOCATION_IGNORE_LIST)
        "chap15/Listing_15_10_XMLRPC_jira.groovy" | addIgnore(AssertStatement, ASTComparatorCategory.LOCATION_IGNORE_LIST)
        "chap15/Listing_15_11_SOAP_wsdl.groovy" | addIgnore(AssertStatement, ASTComparatorCategory.LOCATION_IGNORE_LIST)
        "chap15/Listing_15_12_SOAP11_currency_url.groovy" | addIgnore(AssertStatement, ASTComparatorCategory.LOCATION_IGNORE_LIST)
        "chap15/Listing_15_13_SOAP12_currency_httpb.groovy" | addIgnore(AssertStatement, ASTComparatorCategory.LOCATION_IGNORE_LIST)
        "chap15/Listing_15_14_SOAP11_currency_wslite.groovy" | addIgnore(AssertStatement, ASTComparatorCategory.LOCATION_IGNORE_LIST)
        "chap15/Listing_15_15_SOAP12_currency_wslite.groovy" | addIgnore(AssertStatement, ASTComparatorCategory.LOCATION_IGNORE_LIST)

        "chap16/Listing_16_01_HelloIntegration.groovy" | addIgnore(AssertStatement, ASTComparatorCategory.LOCATION_IGNORE_LIST)
        "chap16/Listing_16_03_MultilineScript.groovy" | addIgnore(AssertStatement, ASTComparatorCategory.LOCATION_IGNORE_LIST)
        "chap16/Listing_16_04_UsingEval.groovy" | addIgnore(AssertStatement, ASTComparatorCategory.LOCATION_IGNORE_LIST)
        "chap16/Listing_16_05_Binding.groovy" | addIgnore(AssertStatement, ASTComparatorCategory.LOCATION_IGNORE_LIST)
        "chap16/Listing_16_06_BindingTwoWay.groovy" | addIgnore(AssertStatement, ASTComparatorCategory.LOCATION_IGNORE_LIST)
        "chap16/Listing_16_07_ClassInScript.groovy" | addIgnore(AssertStatement, ASTComparatorCategory.LOCATION_IGNORE_LIST)
        "chap16/Listing_16_08_Payment_calculator.groovy" | addIgnore(AssertStatement, ASTComparatorCategory.LOCATION_IGNORE_LIST)
        "chap16/Listing_16_09_MethodsInBinding.groovy" | addIgnore(AssertStatement, ASTComparatorCategory.LOCATION_IGNORE_LIST)
        "chap16/Listing_16_12_BeanToString.groovy" | addIgnore(AssertStatement, ASTComparatorCategory.LOCATION_IGNORE_LIST)
        "chap16/shapes/Circle.groovy" | addIgnore(AssertStatement, ASTComparatorCategory.LOCATION_IGNORE_LIST)
        "chap16/shapes/MaxAreaInfo.groovy" | addIgnore(AssertStatement, ASTComparatorCategory.LOCATION_IGNORE_LIST)
        "chap16/spring/groovy/Circle.groovy" | addIgnore(AssertStatement, ASTComparatorCategory.LOCATION_IGNORE_LIST)
        "chap16/spring/groovy/MaxAreaInfo.groovy" | addIgnore(AssertStatement, ASTComparatorCategory.LOCATION_IGNORE_LIST)

        "chap17/automation/src/main/groovy/Calculator.groovy" | addIgnore(AssertStatement, ASTComparatorCategory.LOCATION_IGNORE_LIST)
        "chap17/automation/src/test/groovy/CalculatorTest.groovy" | addIgnore(AssertStatement, ASTComparatorCategory.LOCATION_IGNORE_LIST)
        "chap17/cobertura/src/main/groovy/BiggestPairCalc.groovy" | addIgnore(AssertStatement, ASTComparatorCategory.LOCATION_IGNORE_LIST)
        "chap17/cobertura/src/main/groovy/BiggestPairCalcFixed.groovy" | addIgnore(AssertStatement, ASTComparatorCategory.LOCATION_IGNORE_LIST)
        "chap17/cobertura/src/test/groovy/BiggestPairCalcTest.groovy" | addIgnore(AssertStatement, ASTComparatorCategory.LOCATION_IGNORE_LIST)
        "chap17/Converter.groovy" | addIgnore(AssertStatement, ASTComparatorCategory.LOCATION_IGNORE_LIST)
        "chap17/Counter.groovy" | addIgnore(AssertStatement, ASTComparatorCategory.LOCATION_IGNORE_LIST)
//FIXME        "chap17/extra_ParameterizedTestNG.groovy" | addIgnore(AssertStatement, ASTComparatorCategory.LOCATION_IGNORE_LIST)
        "chap17/extra_TestNG.groovy" | addIgnore(AssertStatement, ASTComparatorCategory.LOCATION_IGNORE_LIST)
        "chap17/Farm.groovy" | addIgnore(AssertStatement, ASTComparatorCategory.LOCATION_IGNORE_LIST)
        "chap17/Listing_17_01_Celsius.groovy" | addIgnore(AssertStatement, ASTComparatorCategory.LOCATION_IGNORE_LIST)
        "chap17/Listing_17_02_CounterTest.groovy" | addIgnore(AssertStatement, ASTComparatorCategory.LOCATION_IGNORE_LIST)
        "chap17/Listing_17_03_HashMapTest.groovy" | addIgnore(AssertStatement, ASTComparatorCategory.LOCATION_IGNORE_LIST)
        "chap17/Listing_17_04_GroovyTestSuite.groovy" | addIgnore(AssertStatement, ASTComparatorCategory.LOCATION_IGNORE_LIST)
        "chap17/Listing_17_05_AllTestSuite.groovy" | addIgnore(AssertStatement, ASTComparatorCategory.LOCATION_IGNORE_LIST)
        "chap17/Listing_17_06_DataDrivenJUnitTest.groovy" | addIgnore(AssertStatement, ASTComparatorCategory.LOCATION_IGNORE_LIST)
        "chap17/Listing_17_07_PropertyBased.groovy" | addIgnore(AssertStatement, ASTComparatorCategory.LOCATION_IGNORE_LIST)
        "chap17/Listing_17_08_Balancer.groovy" | addIgnore(AssertStatement, ASTComparatorCategory.LOCATION_IGNORE_LIST)
        "chap17/Listing_17_09_BalancerStub.groovy" | addIgnore(AssertStatement, ASTComparatorCategory.LOCATION_IGNORE_LIST)
        "chap17/Listing_17_10_BalancerMock.groovy" | addIgnore(AssertStatement, ASTComparatorCategory.LOCATION_IGNORE_LIST)
        "chap17/Listing_17_11_LoggingCounterTest.groovy" | addIgnore(AssertStatement, ASTComparatorCategory.LOCATION_IGNORE_LIST)
        "chap17/Listing_17_12_JUnitPerf.groovy" | addIgnore(AssertStatement, ASTComparatorCategory.LOCATION_IGNORE_LIST)
        "chap17/Listing_17_13_SpockSimple.groovy" | addIgnore(AssertStatement, ASTComparatorCategory.LOCATION_IGNORE_LIST)
        "chap17/Listing_17_14_SpockMock.groovy" | addIgnore(AssertStatement, ASTComparatorCategory.LOCATION_IGNORE_LIST)
        "chap17/Listing_17_15_SpockMockWildcards.groovy" | addIgnore(AssertStatement, ASTComparatorCategory.LOCATION_IGNORE_LIST)
        "chap17/Listing_17_16_SpockMockClosureChecks.groovy" | addIgnore(AssertStatement, ASTComparatorCategory.LOCATION_IGNORE_LIST)
        "chap17/Listing_17_17_SpockDataDriven.groovy" | addIgnore(AssertStatement, ASTComparatorCategory.LOCATION_IGNORE_LIST)
        "chap17/LoggingCounter.groovy" | addIgnore(AssertStatement, ASTComparatorCategory.LOCATION_IGNORE_LIST)
        "chap17/MovieTheater.groovy" | addIgnore(AssertStatement, ASTComparatorCategory.LOCATION_IGNORE_LIST)
        "chap17/Purchase.groovy" | addIgnore(AssertStatement, ASTComparatorCategory.LOCATION_IGNORE_LIST)
        "chap17/snippet1701_JUnit4.groovy" | addIgnore(AssertStatement, ASTComparatorCategory.LOCATION_IGNORE_LIST)
        "chap17/snippet1704_listPropertyCheck.groovy" | addIgnore(AssertStatement, ASTComparatorCategory.LOCATION_IGNORE_LIST)

        "chap18/Listing_18_01_ConcurrentSquares.groovy" | addIgnore(AssertStatement, ASTComparatorCategory.LOCATION_IGNORE_LIST)
        "chap18/Listing_18_02_ConcurrentSquaresTransparent.groovy" | addIgnore(AssertStatement, ASTComparatorCategory.LOCATION_IGNORE_LIST)
        "chap18/Listing_18_03_ConcurrentSquaresTransitive.groovy" | addIgnore(AssertStatement, ASTComparatorCategory.LOCATION_IGNORE_LIST)
        "chap18/Listing_18_04_MapFilterReduce.groovy" | addIgnore(AssertStatement, ASTComparatorCategory.LOCATION_IGNORE_LIST)
        "chap18/Listing_18_05_SquaresMapReduce.groovy" | addIgnore(AssertStatement, ASTComparatorCategory.LOCATION_IGNORE_LIST)
        "chap18/Listing_18_06_Dataflow.groovy" | addIgnore(AssertStatement, ASTComparatorCategory.LOCATION_IGNORE_LIST)
        "chap18/Listing_18_07_DataflowStreams.groovy" | addIgnore(AssertStatement, ASTComparatorCategory.LOCATION_IGNORE_LIST)
        "chap18/Listing_18_08_Actors.groovy" | addIgnore(AssertStatement, ASTComparatorCategory.LOCATION_IGNORE_LIST)
        "chap18/Listing_18_09_ActorsLifecycle.groovy" | addIgnore(AssertStatement, ASTComparatorCategory.LOCATION_IGNORE_LIST)
        "chap18/Listing_18_10_ActorsMessageAware.groovy" | addIgnore(AssertStatement, ASTComparatorCategory.LOCATION_IGNORE_LIST)
        "chap18/Listing_18_11_Agent.groovy" | addIgnore(AssertStatement, ASTComparatorCategory.LOCATION_IGNORE_LIST)
        "chap18/Listing_18_13_YahooForkJoin.groovy" | addIgnore(AssertStatement, ASTComparatorCategory.LOCATION_IGNORE_LIST)
        "chap18/Listing_18_14_YahooMapReduce.groovy" | addIgnore(AssertStatement, ASTComparatorCategory.LOCATION_IGNORE_LIST)
        "chap18/Listing_18_15_YahooDataflow.groovy" | addIgnore(AssertStatement, ASTComparatorCategory.LOCATION_IGNORE_LIST)
        "chap18/snippet1801_startThread.groovy" | addIgnore(AssertStatement, ASTComparatorCategory.LOCATION_IGNORE_LIST)
        "chap18/snippet1803_java_parallel_streams_jdk8_only.groovy" | addIgnore(AssertStatement, ASTComparatorCategory.LOCATION_IGNORE_LIST)
        "chap18/snippet1804_deadlock.groovy" | addIgnore(AssertStatement, ASTComparatorCategory.LOCATION_IGNORE_LIST)
        "chap18/snippet1804_nondeterministic.groovy" | addIgnore(AssertStatement, ASTComparatorCategory.LOCATION_IGNORE_LIST)
        "chap18/YahooService.groovy" | addIgnore(AssertStatement, ASTComparatorCategory.LOCATION_IGNORE_LIST)

        "chap19/FetchOptions.groovy" | addIgnore([AssertStatement, ConstructorNode] , ASTComparatorCategory.LOCATION_IGNORE_LIST)
        "chap19/FetchOptionsBuilder.groovy" | addIgnore(AssertStatement, ASTComparatorCategory.LOCATION_IGNORE_LIST)
        "chap19/Listing_19_06_Binding.groovy" | addIgnore(AssertStatement, ASTComparatorCategory.LOCATION_IGNORE_LIST)
        "chap19/Listing_19_29_OrderDSL.groovy" | addIgnore(AssertStatement, ASTComparatorCategory.LOCATION_IGNORE_LIST)
        "chap19/Listing_19_30_WhenIfControlStructure.groovy" | addIgnore(AssertStatement, ASTComparatorCategory.LOCATION_IGNORE_LIST)
        "chap19/Listing_19_31_Until_failing_.groovy" | addIgnore(AssertStatement, ASTComparatorCategory.LOCATION_IGNORE_LIST)
        "chap19/Listing_19_32_UntilControlStructure.groovy" | addIgnore(AssertStatement, ASTComparatorCategory.LOCATION_IGNORE_LIST)
        "chap19/Listing_19_39_GivenWhenThen.groovy" | addIgnore(AssertStatement, ASTComparatorCategory.LOCATION_IGNORE_LIST)
        "chap19/Listing_19_43_FetchOptionsScript.groovy" | addIgnore(AssertStatement, ASTComparatorCategory.LOCATION_IGNORE_LIST)
        "chap19/Listing_19_44_RubyStyleNewify.groovy" | addIgnore(AssertStatement, ASTComparatorCategory.LOCATION_IGNORE_LIST)
        "chap19/Listing_19_45_PythonStyleNewify.groovy" | addIgnore(AssertStatement, ASTComparatorCategory.LOCATION_IGNORE_LIST)
        "chap19/Listing_19_46_Terms.groovy" | addIgnore(AssertStatement, ASTComparatorCategory.LOCATION_IGNORE_LIST)
        "chap19/Listing_19_48_No_IO.groovy" | addIgnore(AssertStatement, ASTComparatorCategory.LOCATION_IGNORE_LIST)
        "chap19/Listing_19_49_ArithmeticShell.groovy" | addIgnore(AssertStatement, ASTComparatorCategory.LOCATION_IGNORE_LIST)
        "chap19/Listing_19_50_TimedInterrupt.groovy" | addIgnore(AssertStatement, ASTComparatorCategory.LOCATION_IGNORE_LIST)
        "chap19/Listing_19_51_SystemExitGuard.groovy" | addIgnore(AssertStatement, ASTComparatorCategory.LOCATION_IGNORE_LIST)
        "chap19/Listing_19_53_QueryCustomizer.groovy" | addIgnore(AssertStatement, ASTComparatorCategory.LOCATION_IGNORE_LIST)
        "chap19/Query.groovy" | addIgnore(AssertStatement, ASTComparatorCategory.LOCATION_IGNORE_LIST)
        "chap19/extra_FetchOptions_traditional.groovy" | addIgnore(AssertStatement, ASTComparatorCategory.LOCATION_IGNORE_LIST)
        "chap19/v01/Listing_19_01_SelfContainedScript.groovy" | addIgnore(AssertStatement, ASTComparatorCategory.LOCATION_IGNORE_LIST)
        "chap19/v02/Listing_19_04_MainSimple.groovy" | addIgnore(AssertStatement, ASTComparatorCategory.LOCATION_IGNORE_LIST)
        "chap19/v02/Listing_19_05_MainGroovyShell.groovy" | addIgnore(AssertStatement, ASTComparatorCategory.LOCATION_IGNORE_LIST)
        "chap19/v02/Listing_19_07_MainBinding.groovy" | addIgnore(AssertStatement, ASTComparatorCategory.LOCATION_IGNORE_LIST)
        "chap19/v02/Listing_19_08_MainDirectionConstants.groovy" | addIgnore(AssertStatement, ASTComparatorCategory.LOCATION_IGNORE_LIST)
        "chap19/v02/Listing_19_09_MainDirectionsSpread.groovy" | addIgnore(AssertStatement, ASTComparatorCategory.LOCATION_IGNORE_LIST)
        "chap19/v02/Listing_19_10_MainImplicitMethod.groovy" | addIgnore(AssertStatement, ASTComparatorCategory.LOCATION_IGNORE_LIST)
        "chap19/v02/Listing_19_12_MainBaseScript.groovy" | addIgnore(AssertStatement, ASTComparatorCategory.LOCATION_IGNORE_LIST)
        "chap19/v02/Listing_19_13_MainImportCustomizer.groovy" | addIgnore(AssertStatement, ASTComparatorCategory.LOCATION_IGNORE_LIST)
        "chap19/v02/Listing_19_14_MainCustomBaseScriptClass.groovy" | addIgnore(AssertStatement, ASTComparatorCategory.LOCATION_IGNORE_LIST)
        "chap19/v02/Listing_19_16_MainMethodClosure.groovy" | addIgnore(AssertStatement, ASTComparatorCategory.LOCATION_IGNORE_LIST)
        "chap19/v02/Listing_19_19_MainLowerCase.groovy" | addIgnore(AssertStatement, ASTComparatorCategory.LOCATION_IGNORE_LIST)
        "chap19/v02/integration/CaseRobotBaseScript.groovy" | addIgnore(AssertStatement, ASTComparatorCategory.LOCATION_IGNORE_LIST)
        "chap19/v02/integration/CustomBinding.groovy" | addIgnore(AssertStatement, ASTComparatorCategory.LOCATION_IGNORE_LIST)
        "chap19/v02/integration/RobotBaseScript.groovy" | addIgnore(AssertStatement, ASTComparatorCategory.LOCATION_IGNORE_LIST)
        "chap19/v02/model/Direction.groovy" | addIgnore(AssertStatement, ASTComparatorCategory.LOCATION_IGNORE_LIST)
        "chap19/v02/model/Robot.groovy" | addIgnore(AssertStatement, ASTComparatorCategory.LOCATION_IGNORE_LIST)
        "chap19/v02/snippet1901_MainFileRunner.groovy" | addIgnore(AssertStatement, ASTComparatorCategory.LOCATION_IGNORE_LIST)
        "chap19/v03/Listing_19_27_SimpleCommandChain.groovy" | addIgnore(AssertStatement, ASTComparatorCategory.LOCATION_IGNORE_LIST)
        "chap19/v03/Listing_19_40_Robot_With.groovy" | addIgnore(AssertStatement, ASTComparatorCategory.LOCATION_IGNORE_LIST)
        "chap19/v03/integration/DistanceCategory.groovy" | addIgnore(AssertStatement, ASTComparatorCategory.LOCATION_IGNORE_LIST)
        "chap19/v03/integration/RobotBaseScript.groovy" | addIgnore(AssertStatement, ASTComparatorCategory.LOCATION_IGNORE_LIST)
        "chap19/v03/integration/SuperBotBaseScript.groovy" | addIgnore(AssertStatement, ASTComparatorCategory.LOCATION_IGNORE_LIST)
        "chap19/v03/model/Direction.groovy" | addIgnore(AssertStatement, ASTComparatorCategory.LOCATION_IGNORE_LIST)
        "chap19/v03/model/Distance.groovy" | addIgnore(AssertStatement, ASTComparatorCategory.LOCATION_IGNORE_LIST)
        "chap19/v03/model/Duration.groovy" | addIgnore(AssertStatement, ASTComparatorCategory.LOCATION_IGNORE_LIST)
        "chap19/v03/model/Robot.groovy" | addIgnore(AssertStatement, ASTComparatorCategory.LOCATION_IGNORE_LIST)
        "chap19/v03/model/Speed.groovy" | addIgnore(AssertStatement, ASTComparatorCategory.LOCATION_IGNORE_LIST)
        "chap19/v03/model/SuperBot.groovy" | addIgnore(AssertStatement, ASTComparatorCategory.LOCATION_IGNORE_LIST)
        "chap19/v03/model/Unit.groovy" | addIgnore(AssertStatement, ASTComparatorCategory.LOCATION_IGNORE_LIST)
        "chap19/xform/BusinessLogicScript.groovy" | addIgnore(AssertStatement, ASTComparatorCategory.LOCATION_IGNORE_LIST)
        "chap19/xform/CustomControlStructure.groovy" | addIgnore(AssertStatement, ASTComparatorCategory.LOCATION_IGNORE_LIST)
        "chap19/xform/Listing_19_36_WhenTransformation.groovy" | addIgnore(AssertStatement, ASTComparatorCategory.LOCATION_IGNORE_LIST)
        "chap19/xform/WhenUntilTransform.groovy" | addIgnore(AssertStatement, ASTComparatorCategory.LOCATION_IGNORE_LIST)
        "chap19/xform/extra_WhenTransformationWorksWithoutBraces.groovy" | addIgnore(AssertStatement, ASTComparatorCategory.LOCATION_IGNORE_LIST)
        "chap19/xform/snippet1906_WhenUntilXform_Structure.groovy" | addIgnore(AssertStatement, ASTComparatorCategory.LOCATION_IGNORE_LIST)

        "chap20/Listing_20_01_Grapes_for_twitter_urls.groovy" | addIgnore(AssertStatement, ASTComparatorCategory.LOCATION_IGNORE_LIST)
        "chap20/Listing_20_02_Scriptom_Windows_only.groovy" | addIgnore(AssertStatement, ASTComparatorCategory.LOCATION_IGNORE_LIST)
        "chap20/Listing_20_03_ActivX_Windows_only.groovy" | addIgnore(AssertStatement, ASTComparatorCategory.LOCATION_IGNORE_LIST)
        "chap20/Listing_20_10_SquaringMapValue.groovy" | addIgnore(AssertStatement, ASTComparatorCategory.LOCATION_IGNORE_LIST)
        "chap20/Listing_20_11_Synchronized.groovy" | addIgnore(AssertStatement, ASTComparatorCategory.LOCATION_IGNORE_LIST)
        "chap20/Listing_20_12_DbC_invariants.groovy" | addIgnore(AssertStatement, ASTComparatorCategory.LOCATION_IGNORE_LIST)

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
            "Statement_Errors_9.groovy" | _
            "Statement_Errors_10.groovy" | _
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

