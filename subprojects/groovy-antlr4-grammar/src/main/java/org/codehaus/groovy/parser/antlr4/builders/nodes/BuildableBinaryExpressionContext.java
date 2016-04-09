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

import org.antlr.v4.runtime.tree.ParseTree;
import org.antlr.v4.runtime.tree.TerminalNode;
import org.codehaus.groovy.ast.ClassNode;
import org.codehaus.groovy.ast.expr.*;
import org.codehaus.groovy.parser.antlr4.GroovyParser;
import org.codehaus.groovy.parser.antlr4.builders.ASTBuilder;
import org.codehaus.groovy.runtime.DefaultGroovyMethods;
import org.codehaus.groovy.syntax.Types;

import static org.codehaus.groovy.runtime.DefaultGroovyMethods.asBoolean;

/**
 * Created by Daniel on 2016/4/9.
 */
public class BuildableBinaryExpressionContext extends BuildableBaseNode {
    private GroovyParser.BinaryExpressionContext ctx;

    public BuildableBinaryExpressionContext(ASTBuilder astBuilder, GroovyParser.BinaryExpressionContext ctx) {
        super(astBuilder);

        this.ctx = ctx;
    }

    @Override
    public Expression build() {
        TerminalNode c = DefaultGroovyMethods.asType(ctx.getChild(1), TerminalNode.class);
        int i = 1;
        for (ParseTree next = ctx.getChild(i + 1); next instanceof TerminalNode && ((TerminalNode)next).getSymbol().getType() == GroovyParser.GT; next = ctx.getChild(i + 1))
            i++;
        org.codehaus.groovy.syntax.Token op = astBuilder.createToken(c, i);
        Object expression;
        Expression left = astBuilder.parseExpression(ctx.expression(0));
        Expression right = null;// Will be initialized later, in switch. We should handle as and instanceof creating
        // ClassExpression for given IDENTIFIERS. So, switch should fall through.
        //noinspection GroovyFallthrough
        switch (op.getType()) {
            case Types.RANGE_OPERATOR:
                right = astBuilder.parseExpression(ctx.expression(1));
                expression = new RangeExpression(left, right, !op.getText().endsWith("<"));
                break;
            case Types.KEYWORD_AS:
                ClassNode classNode = astBuilder.setupNodeLocation(astBuilder.parseExpression(ctx.genericClassNameExpression()), ctx.genericClassNameExpression());
                expression = CastExpression.asExpression(classNode, left);
                break;
            case Types.KEYWORD_INSTANCEOF:
                ClassNode rhClass = astBuilder.setupNodeLocation(astBuilder.parseExpression(ctx.genericClassNameExpression()), ctx.genericClassNameExpression());
                right = new ClassExpression(rhClass);
            default:
                if (!asBoolean(right)) right = astBuilder.parseExpression(ctx.expression(1));
                expression = new BinaryExpression(left, op, right);
                break;
        }

        ((Expression)expression).setColumnNumber(op.getStartColumn());
        ((Expression)expression).setLastColumnNumber(op.getStartColumn() + op.getText().length());
        ((Expression)expression).setLineNumber(op.getStartLine());
        ((Expression)expression).setLastLineNumber(op.getStartLine());
        return ((Expression)(expression));
    }
}
