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
package org.codehaus.groovy.parser.antlr4.util

import org.antlr.v4.gui.TestRig
import org.antlr.v4.runtime.ANTLRInputStream
import org.antlr.v4.runtime.CommonTokenStream
import org.codehaus.groovy.parser.antlr4.GroovyParser
import org.codehaus.groovy.parser.antlr4.GroovyScanner

/**
 * Created by Daniel on 2016/3/18.
 */
public class GroovyTestRig extends TestRig {

    public GroovyTestRig(String[] args) throws Exception {
        super(['Groovy', 'compilationUnit', *args] as String[]);
    }

    public void inspectParseTree() {
        byte[] content = new File(this.inputFiles[0]).bytes;
        String text = new String(content, this.encoding ?: 'UTF-8');

        GroovyScanner scanner = new GroovyScanner(new ANTLRInputStream(text));
        CommonTokenStream tokens = new CommonTokenStream(scanner);
        GroovyParser parser = new GroovyParser(tokens);

        this.process(scanner, GroovyParser.class, parser, new ByteArrayInputStream(content), new StringReader(text));
    }

    public static void main(String[] args) {
        if (args.length == 0) {
            println "Usage: [-tokens] [-tree] [-gui] [-ps file.ps] [-encoding encodingname] [-trace] [-diagnostics] [-SLL] input-filename\n";
            return;
        }

        if (!(args.find { !it.startsWith('-') })) {
            println "input-filename is required!"
            return;
        }

        GroovyTestRig groovyTestRig = new GroovyTestRig(args);

        groovyTestRig.inspectParseTree();
    }
}
