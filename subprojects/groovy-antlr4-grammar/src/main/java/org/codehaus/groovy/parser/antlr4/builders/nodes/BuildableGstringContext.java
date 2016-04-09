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

import groovy.lang.Closure;
import groovy.lang.IntRange;
import org.antlr.v4.runtime.tree.ParseTree;
import org.antlr.v4.runtime.tree.TerminalNode;
import org.codehaus.groovy.ast.expr.*;
import org.codehaus.groovy.parser.antlr4.GroovyParser;
import org.codehaus.groovy.parser.antlr4.builders.ASTBuilder;
import org.codehaus.groovy.parser.antlr4.util.StringUtil;
import org.codehaus.groovy.runtime.DefaultGroovyMethods;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import static org.codehaus.groovy.runtime.DefaultGroovyMethods.asBoolean;
import static org.codehaus.groovy.runtime.DefaultGroovyMethods.collect;

/**
 * Created by Daniel on 2016/4/10.
 */
public class BuildableGstringContext extends BuildableBaseNode {
    private GroovyParser.GstringContext ctx;

    public BuildableGstringContext(ASTBuilder astBuilder, GroovyParser.GstringContext ctx) {
        super(astBuilder);

        this.ctx = ctx;
    }

    @Override
    public Expression build() {
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
                    expressions.add(astBuilder.collectPathExpression(gstringExpressionBodyContext.gstringPathExpression()));
                    return expressions;
                } else if (asBoolean(gstringExpressionBodyContext.closureExpressionRule())) {
                    GroovyParser.ClosureExpressionRuleContext closureExpressionRule = gstringExpressionBodyContext.closureExpressionRule();
                    Expression expression = astBuilder.parseExpression(closureExpressionRule);

                    if (!asBoolean(closureExpressionRule.CLOSURE_ARG_SEPARATOR())) {

                        MethodCallExpression methodCallExpression = new MethodCallExpression(expression, "call", new ArgumentListExpression());

                        expressions.add(astBuilder.setupNodeLocation(methodCallExpression, expression));
                        return expressions;
                    }

                    expressions.add(expression);
                    return expressions;
                } else {
                    if (asBoolean(gstringExpressionBodyContext.expression())) {
                        // We can guarantee, that it will be at least fallback ExpressionContext multimethod overloading, that can handle such situation.
                        //noinspection GroovyAssignabilityCheck
                        expressions.add(astBuilder.parseExpression(gstringExpressionBodyContext.expression()));
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
        return astBuilder.setupNodeLocation(gstringNode, ctx);
    }
}
