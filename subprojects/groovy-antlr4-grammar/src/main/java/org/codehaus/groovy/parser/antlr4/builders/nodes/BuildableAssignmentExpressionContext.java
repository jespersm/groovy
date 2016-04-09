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
import org.codehaus.groovy.ast.ClassHelper;
import org.codehaus.groovy.ast.expr.BinaryExpression;
import org.codehaus.groovy.ast.expr.Expression;
import org.codehaus.groovy.ast.expr.TupleExpression;
import org.codehaus.groovy.ast.expr.VariableExpression;
import org.codehaus.groovy.parser.antlr4.GroovyParser;
import org.codehaus.groovy.parser.antlr4.builders.ASTBuilder;
import org.codehaus.groovy.runtime.DefaultGroovyMethods;
import org.codehaus.groovy.syntax.Types;

import java.util.LinkedList;
import java.util.List;

import static org.codehaus.groovy.runtime.DefaultGroovyMethods.asBoolean;

/**
 * Created by Daniel on 2016/4/9.
 */
public class BuildableAssignmentExpressionContext extends BuildableBaseNode {
    private GroovyParser.AssignmentExpressionContext ctx;

    public BuildableAssignmentExpressionContext(ASTBuilder astBuilder, GroovyParser.AssignmentExpressionContext ctx) {
        super(astBuilder);

        this.ctx = ctx;
    }

    @Override
    public Expression build() {
        Expression left;
        Expression right;
        org.codehaus.groovy.syntax.Token token;

        if (asBoolean(ctx.LPAREN())) { // tuple assignment expression
            List<Expression> expressions = new LinkedList<Expression>();

            for (TerminalNode id : ctx.IDENTIFIER()) {
                expressions.add(new VariableExpression(id.getText(), ClassHelper.OBJECT_TYPE));
            }

            left = new TupleExpression(expressions);
            right = astBuilder.parseExpression(ctx.expression(0));

            token = astBuilder.createGroovyToken(ctx.ASSIGN().getSymbol(), Types.ASSIGN);
        } else {
            left = astBuilder.parseExpression(ctx.expression(0));// TODO reference to AntlrParserPlugin line 2304 for error handling.
            right = astBuilder.parseExpression(ctx.expression(1));

            token = astBuilder.createToken(DefaultGroovyMethods.asType(ctx.getChild(1), TerminalNode.class));
        }

        return astBuilder.setupNodeLocation(new BinaryExpression(left, token, right), ctx);
    }
}
