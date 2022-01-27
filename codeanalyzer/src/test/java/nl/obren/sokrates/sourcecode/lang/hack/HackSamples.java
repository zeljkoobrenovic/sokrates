package nl.obren.sokrates.sourcecode.lang.hack;

public class HackSamples {
    static final String SAMPLE_1 = "namespace Facebook\\HackCodegen;\n" +
            "\n" +
            "use namespace HH\\Lib\\{C, Str, Vec};\n" +
            "use namespace Facebook\\HackCodegen\\_Private\\Vec as VecP;\n" +
            "\n" +
            "/** Class containing basic language-agnostic code generation functions.\n" +
            " *\n" +
            " * This should not be used directly; instantiable language-specific subclasses\n" +
            " * should be used to generate code. For example, Hack code is generated using\n" +
            " * the `HackBuilder` class.\n" +
            " */\n" +
            " <<__ConsistentConstruct>>\n" +
            "abstract class BaseCodeBuilder {\n" +
            "\n" +
            "  const string DELIMITER = \"\\0\";\n" +
            "\n" +
            "  private _Private\\StrBuffer $code;\n" +
            "  private bool $isNewLine = true;\n" +
            "  private int $indentationLevel = 0;\n" +
            "  private bool $isInsideFunction = false;\n" +
            "  private bool $wasGetCodeCalled = false;\n" +
            "\n" +
            "  final public function __construct(protected IHackCodegenConfig $config) {\n" +
            "    $this->code = new _Private\\StrBuffer();\n" +
            "  }\n" +
            "\n" +
            "  /** Append a new line.\n" +
            "   *\n" +
            "   * This will always append a new line, even if the previous character was\n" +
            "   * a new line.\n" +
            "   *\n" +
            "   * To add a new line character only if we are not at the start of a line, use\n" +
            "   * `ensureNewLine()`.\n" +
            "   */\n" +
            "  final public function newLine(): this {\n" +
            "    $this->code->append(\"\\n\");\n" +
            "    $this->isNewLine = true;\n" +
            "    return $this;\n" +
            "  }\n" +
            "\n" +
            "  /**\n" +
            "   * If the cursor is not in a new line, it will insert a line break.\n" +
            "   */\n" +
            "  final public function ensureNewLine(): this {\n" +
            "    if (!$this->isNewLine) {\n" +
            "      $this->newLine();\n" +
            "    }\n" +
            "    return $this;\n" +
            "  }\n" +
            "}\n";

    static final String SAMPLE_1_CLEANED_LOC = "namespace Facebook\\HackCodegen;\n" +
            "use namespace HH\\Lib\\{C, Str, Vec};\n" +
            "use namespace Facebook\\HackCodegen\\_Private\\Vec as VecP;\n" +
            " <<__ConsistentConstruct>>\n" +
            "abstract class BaseCodeBuilder {\n" +
            "  const string DELIMITER = \"\\0\";\n" +
            "  private _Private\\StrBuffer $code;\n" +
            "  private bool $isNewLine = true;\n" +
            "  private int $indentationLevel = 0;\n" +
            "  private bool $isInsideFunction = false;\n" +
            "  private bool $wasGetCodeCalled = false;\n" +
            "  final public function __construct(protected IHackCodegenConfig $config) {\n" +
            "    $this->code = new _Private\\StrBuffer();\n" +
            "  }\n" +
            "  final public function newLine(): this {\n" +
            "    $this->code->append(\"\\n\");\n" +
            "    $this->isNewLine = true;\n" +
            "    return $this;\n" +
            "  }\n" +
            "  final public function ensureNewLine(): this {\n" +
            "    if (!$this->isNewLine) {\n" +
            "      $this->newLine();\n" +
            "    }\n" +
            "    return $this;\n" +
            "  }\n" +
            "}";

    static final String SAMPLE_1_CLEANED_DUPLICATION = "<<__ConsistentConstruct>>\n" +
            "abstract class BaseCodeBuilder {\n" +
            "const string DELIMITER = \"\\0\";\n" +
            "private _Private\\StrBuffer $code;\n" +
            "private bool $isNewLine = true;\n" +
            "private int $indentationLevel = 0;\n" +
            "private bool $isInsideFunction = false;\n" +
            "private bool $wasGetCodeCalled = false;\n" +
            "final public function __construct(protected IHackCodegenConfig $config) {\n" +
            "$this->code = new _Private\\StrBuffer();\n" +
            "final public function newLine(): this {\n" +
            "$this->code->append(\"\\n\");\n" +
            "$this->isNewLine = true;\n" +
            "return $this;\n" +
            "final public function ensureNewLine(): this {\n" +
            "if (!$this->isNewLine) {\n" +
            "$this->newLine();\n" +
            "return $this;";
}
