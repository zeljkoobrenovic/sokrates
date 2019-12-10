/*
 * Copyright (c) 2019 Željko Obrenović. All rights reserved.
 */

package nl.obren.sokrates.sourcecode.lang.go;

public class GoExampleFragments {
    public static String FRAGMENT_1 = "package main\n" +
            "\n" +
            "/* This program example prints\n" +
            "Hello world text to console\n" +
            "*/\n" +
            "import (\n" +
            " \"fmt\"\n" +
            ")\n" +
            "\n" +
            "// Main function\n" +
            "func main() {\n" +
            " // Print Text to console\n" +
            " fmt.Println(\"Hello World\")\n" +
            "\n" +
            "}";

    public static String FRAGMENT_1_CLEANED_FOR_LOC = "package main\n" +
            "import (\n" +
            " \"fmt\"\n" +
            ")\n" +
            "func main() {\n" +
            " fmt.Println(\"Hello World\")\n" +
            "}";

    public static String FRAGMENT_1_CLEANED_FOR_DUPLICATION =
            "func main() {\n" +
            "fmt.Println(\"Hello World\")";
}
