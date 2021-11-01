/*
 * Copyright (c) 2021 Željko Obrenović. All rights reserved.
 */

package nl.obren.sokrates.sourcecode.lang.kotlin;

public class KotlinExampleFragments {
    public static String FRAGMENT_1 = "// Hello, World! example\n" +
            "fun main() {\n" +
            "  val scope = \"World\"\n" +
            "  println(\"Hello, $scope!\")\n" +
            "}\n" +
            "\n" +
            "fun main(args: Array<String>) {\n" +
            "  for (arg in args) {\n" +
            "    println(arg)\n" +
            "  }\n" +
            "}";

    public static String FRAGMENT_1_CLEANED = "fun main() {\n" +
            "  val scope = \"World\"\n" +
            "  println(\"Hello, $scope!\")\n" +
            "}\n" +
            "fun main(args: Array<String>) {\n" +
            "  for (arg in args) {\n" +
            "    println(arg)\n" +
            "  }\n" +
            "}";

    public static String FRAGMENT_1_CLEANED_FOR_DUPLICATION = "fun main() {\n" +
            "val scope = \"World\"\n" +
            "println(\"Hello, $scope!\")\n" +
            "fun main(args: Array<String>) {\n" +
            "for (arg in args) {\n" +
            "println(arg)";

}
