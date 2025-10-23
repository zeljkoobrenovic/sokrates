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

    // Interface methods fragment for testing
    public static String INTERFACE_METHODS_FRAGMENT = "interface PropertyRepository {\n" +
            "    fun findAllByPropertyId(\n" +
            "        @Param(\"propertyId\") propertyId: String\n" +
            "    ): List<Property>\n" +
            "\n" +
            "    @Query(\n" +
            "        \"\"\"\n" +
            "        select u\n" +
            "            from Unit u\n" +
            "            where u.property.id = :propertyId\n" +
            "              and upper(u.identifiers.id) = upper(:unitId)\n" +
            "              and u.deleted = false\n" +
            "            order by u.identifiers.id asc, u.id \n" +
            "        \"\"\"\n" +
            "    )\n" +
            "    fun findAllByPropertyIdAndUnitId(\n" +
            "        @Param(\"propertyId\") propertyId: String,\n" +
            "        @Param(\"unitId\") unitId: String\n" +
            "    ): List<Unit>\n" +
            "\n" +
            "    fun simpleMethod(param: String): String\n" +
            "\n" +
            "    fun implementedMethod(param: String): String {\n" +
            "        return \"result\"\n" +
            "    }\n" +
            "}";

    // Test case for braces on separate lines (common Kotlin style)
    public static String BRACE_ON_NEW_LINE_FRAGMENT = "interface Repository {\n" +
            "    fun interfaceMethodWithBraceOnNewLine(\n" +
            "        param1: String,\n" +
            "        param2: Int\n" +
            "    ): String\n" +
            "\n" +
            "    fun implementedMethodWithBraceOnNewLine(\n" +
            "        param1: String,\n" +
            "        param2: Int\n" +
            "    ): String\n" +
            "    {\n" +
            "        return \"result\"\n" +
            "    }\n" +
            "}\n";

}
