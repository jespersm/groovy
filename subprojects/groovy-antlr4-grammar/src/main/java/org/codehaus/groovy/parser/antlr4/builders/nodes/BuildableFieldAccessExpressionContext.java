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
import org.codehaus.groovy.ast.expr.*;
import org.codehaus.groovy.parser.antlr4.GroovyParser;
import org.codehaus.groovy.parser.antlr4.builders.ASTBuilder;
import org.codehaus.groovy.runtime.DefaultGroovyMethods;

/**
 * Created by Daniel on 2016/4/9.
 */
public class BuildableFieldAccessExpressionContext extends BuildableBaseNode {
    private GroovyParser.FieldAccessExpressionContext ctx;

    public BuildableFieldAccessExpressionContext(ASTBuilder astBuilder, GroovyParser.FieldAccessExpressionContext ctx) {
        super(astBuilder);

        this.ctx = ctx;
    }

    @Override
    public Expression build() {
        TerminalNode op = DefaultGroovyMethods.asType(ctx.getChild(1), TerminalNode.class);
        Expression left = astBuilder.parseExpression(ctx.expression());

        GroovyParser.SelectorNameContext fieldName = ctx.selectorName();
        Expression right = fieldName != null ? new ConstantExpression(fieldName.getText()) :
                (ctx.STRING() != null ? astBuilder.parseConstantStringToken(ctx.STRING().getSymbol()) : astBuilder.parseExpression(ctx.gstring())
                );
        Expression node = null;
        switch (op.getSymbol().getType()) {
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
        return astBuilder.setupNodeLocation(node, ctx);
    }
}
