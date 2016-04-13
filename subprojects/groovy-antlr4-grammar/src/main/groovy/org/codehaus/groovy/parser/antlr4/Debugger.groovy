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

import org.antlr.v4.gui.TreeViewer
import org.antlr.v4.runtime.ANTLRInputStream
import org.antlr.v4.runtime.CommonTokenStream
import org.codehaus.groovy.parser.antlr4.util.StringUtil

import javax.swing.*

/**
 * Created by Daniel on 2016/3/18.
 */
public class Debugger {

    public void showParseTreeStr(File sourceFile) {
        this.showParseTree(sourceFile, false);
    }

    public void showParseTreeGui(File sourceFile) {
        this.showParseTree(sourceFile, true);
    }

    private void showParseTree(File sourceFile, boolean isGUI) {
        String text = sourceFile.text;

        GroovyLexer lexer = new GroovyLexer(new ANTLRInputStream(text));
        CommonTokenStream tokens = new CommonTokenStream(lexer);

        GroovyParser parser = new GroovyParser(tokens);

        GroovyParser.CompilationUnitContext tree = parser.compilationUnit();

        if (isGUI) {
            JFrame frame = new JFrame("Parse Tree");
            JPanel panel = new JPanel();
            TreeViewer viewer = new TreeViewer(Arrays.asList(parser.getRuleNames()),tree);
            viewer.setScale(1.2);
            panel.add(viewer);
            frame.add(panel);
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.setSize(600, 400);
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);

        } else {
            println tree.toStringTree(parser);
        }

    }

    public static void main(String[] args) {
        if (args.length == 0) {
            println "source file is required.";

            return;
        }


        File sourceFile = new File(args[0]);

        Debugger debugger = new Debugger();

        debugger.showParseTreeStr(sourceFile);
        debugger.showParseTreeGui(sourceFile);
    }
}
