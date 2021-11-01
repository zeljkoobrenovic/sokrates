/*
 * Copyright (c) 2021 Željko Obrenović. All rights reserved.
 */

package nl.obren.sokrates.sourcecode.lang.swift;

public class SwiftExampleFragments {
    public final static String FRAGMENT_1 = "extension Array {\n" +
            "  /// Creates a new, empty array. [...]\n" +
            "  init()\n" +
            "}";

    public final static String FRAGMENT_1_CLEANED = "extension Array {\n" +
            "  init()\n" +
            "}";

    public final static String FRAGMENT_1_CLEANED_FOR_DUPLICATION = "extension Array {\n" +
            "init()";

    public final static String COMMENT_NESTED = "/* This is the start of the first multiline comment.\n" +
            " /* This is the second, nested multiline comment. */\n" +
            "This is the end of the first multiline comment. */";


    public final static String UNIT_1 = "func minMax(array: [Int]) -> (min: Int, max: Int) {\n" +
            "    var currentMin = array[0]\n" +
            "    var currentMax = array[0]\n" +
            "    for value in array[1..<array.count] {\n" +
            "        if value < currentMin {\n" +
            "            currentMin = value\n" +
            "        } else if value > currentMax {\n" +
            "            currentMax = value\n" +
            "        }\n" +
            "    }\n" +
            "    return (currentMin, currentMax)\n" +
            "}\n\n" +
            "func printAndCount(string: String) -> Int {\n" +
            "    print(string)\n" +
            "    return string.count\n" +
            "}\n" +
            "func printWithoutCounting(string: String) {\n" +
            "    let _ = printAndCount(string: string)\n" +
            "}\n" +
            "printAndCount(string: \"hello, world\")\n" +
            "// prints \"hello, world\" and returns a value of 12\n" +
            "printWithoutCounting(string: \"hello, world\")\n" +
            "// prints \"hello, world\" but does not return a value";

    public final static String INIT_UNITS = "struct Color {\n" +
            "    let red, green, blue: Double\n" +
            "    init(red: Double, green: Double, blue: Double) {\n" +
            "        self.red   = red\n" +
            "        self.green = green\n" +
            "        self.blue  = blue\n" +
            "    }\n" +
            "    init(white: Double) {\n" +
            "        red   = white\n" +
            "        green = white\n" +
            "        blue  = white\n" +
            "    }\n" +
            "}";
}
