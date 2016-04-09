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
package org.codehaus.groovy.parser.antlr4.builders.nodes;

import org.antlr.v4.runtime.tree.TerminalNode;
import org.codehaus.groovy.GroovyBugError;
import org.codehaus.groovy.ast.*;
import org.codehaus.groovy.parser.antlr4.GroovyParser;
import org.codehaus.groovy.parser.antlr4.builders.ASTBuilder;
import org.codehaus.groovy.runtime.DefaultGroovyMethods;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Daniel on 2016/4/10.
 */
public class BuildableImportStatementContext extends BuildableBaseNode {
    private GroovyParser.ImportStatementContext ctx;

    public BuildableImportStatementContext(ASTBuilder astBuilder, GroovyParser.ImportStatementContext ctx) {
        super(astBuilder);

        this.ctx = ctx;
    }

    @Override
    public ImportNode build() {
        ImportNode node;
        List<TerminalNode> qualifiedClassName = new ArrayList<TerminalNode>(ctx.IDENTIFIER());
        boolean isStar = ctx.MULT() != null;
        boolean isStatic = ctx.KW_STATIC() != null;
        String alias = (ctx.KW_AS() != null) ? DefaultGroovyMethods.pop(qualifiedClassName).getText() : null;
        List<AnnotationNode> annotations = astBuilder.parseAnnotations(ctx.annotationClause());

        if (isStar) {
            if (isStatic) {
                // import is like "import static foo.Bar.*"
                // packageName is actually a className in this case
                ClassNode type = ClassHelper.make(DefaultGroovyMethods.join(qualifiedClassName, "."));
                astBuilder.moduleNode.addStaticStarImport(DefaultGroovyMethods.last(qualifiedClassName).getText(), type, annotations);

                node = DefaultGroovyMethods.last(astBuilder.moduleNode.getStaticStarImports().values());
            } else {
                // import is like "import foo.*"
                astBuilder.moduleNode.addStarImport(DefaultGroovyMethods.join(qualifiedClassName, ".") + ".", annotations);

                node = DefaultGroovyMethods.last(astBuilder.moduleNode.getStarImports());
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
                astBuilder.moduleNode.addStaticImport(type, fieldName, alias != null ? alias : fieldName, annotations);

                node = DefaultGroovyMethods.last(astBuilder.moduleNode.getStaticImports().values());
            } else {
                // import is like "import foo.Bar"
                ClassNode type = ClassHelper.make(DefaultGroovyMethods.join(qualifiedClassName, "."));
                if (alias == null) {
                    alias = DefaultGroovyMethods.last(qualifiedClassName).getText();
                }
                astBuilder.moduleNode.addImport(alias, type, annotations);

                node = DefaultGroovyMethods.last(astBuilder.moduleNode.getImports());
            }

        }

        return astBuilder.setupNodeLocation(node, ctx);
    }
}
