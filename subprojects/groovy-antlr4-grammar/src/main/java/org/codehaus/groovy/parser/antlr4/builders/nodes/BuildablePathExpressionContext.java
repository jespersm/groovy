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
import org.antlr.v4.runtime.tree.TerminalNode;
import org.codehaus.groovy.ast.expr.Expression;
import org.codehaus.groovy.ast.expr.PropertyExpression;
import org.codehaus.groovy.ast.expr.VariableExpression;
import org.codehaus.groovy.parser.antlr4.GroovyParser;
import org.codehaus.groovy.parser.antlr4.builders.ASTBuilder;
import org.codehaus.groovy.runtime.DefaultGroovyMethods;

import java.util.List;
import java.util.logging.Logger;

/**
 * Created by Daniel on 2016/4/9.
 */
public class BuildablePathExpressionContext extends BuildableBaseNode {
    private GroovyParser.PathExpressionContext ctx;

    public BuildablePathExpressionContext(ASTBuilder astBuilder, GroovyParser.PathExpressionContext ctx) {
        super(astBuilder);

        this.ctx = ctx;
    }

    @Override
    public Expression build() {
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

        return expression;
    }

    private Logger log = Logger.getLogger(BuildablePathExpressionContext.class.getName());
}
