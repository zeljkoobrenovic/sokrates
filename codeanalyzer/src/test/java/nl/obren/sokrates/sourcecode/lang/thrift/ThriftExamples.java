package nl.obren.sokrates.sourcecode.lang.thrift;

public class ThriftExamples {
    public final static String EXAMPLE1 = "namespace java tutorial\n" +
            "namespace py tutorial\n" +
            "include test\n" +
            "\n" +
            "/*\n" +
            " C like comments are supported\n" +
            "*/\n" +
            "// This is also a valid comment\n" +
            "\n" +
            "typedef i32 int // We can use typedef to get pretty names for the types we are using\n" +
            "service MultiplicationService\n" +
            "{\n" +
            "        int multiply(1:int n1, 2:int n2),\n" +
            "}";
    public final static String EXAMPLE1_CLEANED = "namespace java tutorial\n" +
            "namespace py tutorial\n" +
            "include test\n" +
            "typedef i32 int \n" +
            "service MultiplicationService\n" +
            "{\n" +
            "        int multiply(1:int n1, 2:int n2),\n" +
            "}";
    public final static String EXAMPLE1_CLEANED_DUPLICATION = "typedef i32 int\n" +
            "service MultiplicationService\n" +
            "int multiply(1:int n1, 2:int n2),";
}
