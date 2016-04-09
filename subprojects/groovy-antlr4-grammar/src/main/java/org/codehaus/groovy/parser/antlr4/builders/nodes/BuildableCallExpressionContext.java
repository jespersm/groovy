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
import org.codehaus.groovy.ast.ClassNode;
import org.codehaus.groovy.ast.expr.ConstructorCallExpression;
import org.codehaus.groovy.ast.expr.Expression;
import org.codehaus.groovy.ast.expr.MethodCallExpression;
import org.codehaus.groovy.ast.expr.VariableExpression;
import org.codehaus.groovy.parser.antlr4.GroovyParser;
import org.codehaus.groovy.parser.antlr4.builders.ASTBuilder;
import org.codehaus.groovy.runtime.DefaultGroovyMethods;

import java.util.List;

import static org.codehaus.groovy.runtime.DefaultGroovyMethods.asBoolean;

/**
 * Created by Daniel on 2016/4/9.
 */
public class BuildableCallExpressionContext extends BuildableBaseNode {
    private GroovyParser.CallExpressionContext ctx;

    public BuildableCallExpressionContext(ASTBuilder astBuilder, GroovyParser.CallExpressionContext ctx) {
        super(astBuilder);

        this.ctx = ctx;
    }

    @Override
    public Expression build() {

        Object methodNode;
        //FIXME in log a, b; a is treated as path expression and became a method call instead of variable
        if (!asBoolean(ctx.LPAREN()) && ctx.closureExpressionRule().size() == 0)
            return astBuilder.collectPathExpression(ctx.pathExpression());

        // Collect closure's in argumentList expression.
        final Expression argumentListExpression = astBuilder.createArgumentList(ctx.argumentList());
        DefaultGroovyMethods.each(ctx.closureExpressionRule(), new Closure<Object>(null, null) {
            public Object doCall(GroovyParser.ClosureExpressionRuleContext it) {return DefaultGroovyMethods.invokeMethod(argumentListExpression, "addExpression", new Object[]{ astBuilder.parseExpression(it) });}
        });

        //noinspection GroovyAssignabilityCheck
        List<Object> iterator = astBuilder.parsePathExpression(ctx.pathExpression());
        Expression expression = (Expression)iterator.get(0);
        String methodName = (String)iterator.get(1);
        boolean implicitThis = (Boolean)iterator.get(2);

        if (implicitThis && VariableExpression.THIS_EXPRESSION.getText().equals(methodName)) {
            // Actually a constructor call
            ConstructorCallExpression call = new ConstructorCallExpression(ClassNode.THIS, argumentListExpression);
            return astBuilder.setupNodeLocation(call, ctx);
//        } else if (implicitThis && VariableExpression.SUPER_EXPRESSION.getText().equals(methodName)) {
//            // Use this once path expression is refac'ed
//            // Actually a constructor call
//            ConstructorCallExpression call = new ConstructorCallExpression(ClassNode.SUPER, argumentListExpression);
//            return setupNodeLocation(call, ctx);
        }
        // OK, just a normal call
        methodNode = new MethodCallExpression(expression, methodName, argumentListExpression);
        ((MethodCallExpression)methodNode).setImplicitThis(implicitThis);
        return (Expression)methodNode;
    }
}
