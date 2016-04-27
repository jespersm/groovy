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
package org.codehaus.groovy.parser.antlr4.util;

import groovy.lang.Closure;
import org.codehaus.groovy.runtime.DefaultGroovyMethods;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

public class StringUtil {
    public static String replaceHexEscapes(String text) {
        Pattern p = Pattern.compile("\\\\u([0-9abcdefABCDEF]{4})");
	    return DefaultGroovyMethods.replaceAll(text, p, new Closure<Void>(null, null) {
		    Object doCall(String _0, String _1) {
			    return Character.toChars(Integer.parseInt(_1, 16));
		    }
	    });
    }

	public static String replaceOctalEscapes(String text) {
	    Pattern p = Pattern.compile("\\\\([0-3]?[0-7]?[0-7])");
	    return DefaultGroovyMethods.replaceAll(text, p, new Closure<Void>(null, null) {
		    Object doCall(String _0, String _1) {
			    return Character.toChars(Integer.parseInt(_1, 8));
		    }
	    });
    }

    private static Map<Character, Character> standardEscapes = new HashMap<Character, Character>();
	static {
        standardEscapes.put('b', '\b');
        standardEscapes.put('t', '\t');
        standardEscapes.put('n', '\n');
        standardEscapes.put('f', '\f');
        standardEscapes.put('r', '\r');
    }

	public static String replaceStandardEscapes(String text) {
	    Pattern p = Pattern.compile("\\\\([btnfr\"'\\\\])");
	    return DefaultGroovyMethods.replaceAll(text, p, new Closure<Void>(null, null) {
		    Object doCall(String _0, String _1) {
			    Character character = standardEscapes.get(_1.charAt(0));
			    return character != null ? character : _1;
		    }
	    });
    }

	public static final int SLASHY = 0;
	public static final int DOLLAR_SLASHY = 1;
	public static final int NONE_SLASHY = -1;

	public static String replaceEscapes(String text, int slashyType) {
		if (slashyType == SLASHY || slashyType == DOLLAR_SLASHY) {
			text = StringUtil.replaceHexEscapes(text);

			if (slashyType == SLASHY)
				text = text.replace("\\/", "/");

			if (slashyType == DOLLAR_SLASHY)
				text = text.replace("$$", "$");
		} else if (slashyType == NONE_SLASHY) {
			text = StringUtil.replaceEscapes(text);
		} else {
			throw new IllegalArgumentException("Invalid slashyType: " + slashyType);
		}

		return text;
	}

	public static String replaceEscapes(String text) {
		text = text.replace("\\$", "$");
		text = text.replaceAll("\\\\\r?\n", "");

        return replaceStandardEscapes(replaceHexEscapes(replaceOctalEscapes(text)));
    }

	public static String removeCR(String text) {
        return text.replace("\r\n", "\n");
    }
}