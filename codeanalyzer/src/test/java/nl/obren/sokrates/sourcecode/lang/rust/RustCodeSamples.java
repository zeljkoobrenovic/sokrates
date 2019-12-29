/*
 * Copyright (c) 2019 Željko Obrenović. All rights reserved.
 */

package nl.obren.sokrates.sourcecode.lang.rust;

public class RustCodeSamples {
    protected final static String FRAGMENT_1 = "fn main() {\n" +
            "    // I’m feeling lucky today\n" +
            "    let lucky_number = 7;\n" +
            "}\n" +
            "fn a(x: i32, y: i32) {\n" +
            "    let number = 3;\n" +
            "\n" +
            "    if number < 5 {\n" +
            "        println!(\"condition was true\");\n" +
            "    } else {\n" +
            "        println!(\"condition was false\");\n" +
            "    }\n" +
            "}\n" +
            "fn b(z: i32) {\n" +
            "    loop {\n" +
            "        println!(\"again!\");\n" +
            "    }\n" +
            "}";
    protected final static String FRAGMENT_1_CLEANED = "fn main() {\n" +
            "    let lucky_number = 7;\n" +
            "}\n" +
            "fn a(x: i32, y: i32) {\n" +
            "    let number = 3;\n" +
            "    if number < 5 {\n" +
            "        println!(\"condition was true\");\n" +
            "    } else {\n" +
            "        println!(\"condition was false\");\n" +
            "    }\n" +
            "}\n" +
            "fn b(z: i32) {\n" +
            "    loop {\n" +
            "        println!(\"again!\");\n" +
            "    }\n" +
            "}";

    protected final static String FRAGMENT_1_CLEANED_FOR_DUPLICATION = "fn main() {\n" +
            "let lucky_number = 7;\n" +
            "fn a(x: i32, y: i32) {\n" +
            "let number = 3;\n" +
            "if number < 5 {\n" +
            "println!(\"condition was true\");\n" +
            "} else {\n" +
            "println!(\"condition was false\");\n" +
            "fn b(z: i32) {\n" +
            "loop {\n" +
            "println!(\"again!\");";
}
