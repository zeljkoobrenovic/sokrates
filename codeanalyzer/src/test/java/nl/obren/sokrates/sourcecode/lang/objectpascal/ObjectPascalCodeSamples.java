/*
 * Copyright (c) 2019 Željko Obrenović. All rights reserved.
 */

package nl.obren.sokrates.sourcecode.lang.objectpascal;

public class ObjectPascalCodeSamples {
    protected final static String FRAGMENT_1 = "program ObjectPascalExample;\n" +
            "\n" +
            "   type\n" +
            "      PHelloWorld = ^THelloWorld;\n" +
            "      THelloWorld = object\n" +
            "         procedure Put;\n" +
            "      end;\n" +
            "\n" +
            "   procedure THelloWorld.Put;\n" +
            "   begin\n" +
            "      WriteLn('Hello, World!');\n" +
            "   end;\n" +
            "\n" +
            "var\n" +
            "  HelloWorld: PHelloWorld; { this is a typed pointer to a THelloWorld }\n" +
            "  HelloWorld2: ^THelloWorld; { this is exactly the same with different syntax } \n" +
            "  HelloWorld3: ^THelloWorld;                               \n" +
            "  HelloWorld4: PHelloWorld;   \n" +
            "begin\n" +
            "   { This works in a similar way as the code above, note the allocation and de-allocation, though,\n" +
            "     many people get confused. In the past there was a wrong example with wrong comments here... }\n" +
            "\n" +
            "   New(HelloWorld);  { one instance }\n" +
            "   HelloWorld4 := HelloWorld; { this is valid - a pointer copy }\n" +
            "   HelloWorld2 := HelloWorld; { this is valid - a pointer copy }\n" +
            "   New(HelloWorld3); { a second instance }\n" +
            "   HelloWorld := HelloWorld3; { this is valid - a pointer copy }\n" +
            "   HelloWorld2 := HelloWorld3;{ this is valid - a pointer copy }\n" +
            "   Dispose(HelloWorld); { we allocated just two instances }\n" +
            "   Dispose(HelloWorld3);{so we have to release only two instances }\n" +
            "end.";
    protected final static String FRAGMENT_1_CLEANED = "program ObjectPascalExample;\n" +
            "   type\n" +
            "      PHelloWorld = ^THelloWorld;\n" +
            "      THelloWorld = object\n" +
            "         procedure Put;\n" +
            "      end;\n" +
            "   procedure THelloWorld.Put;\n" +
            "   begin\n" +
            "      WriteLn('Hello, World!');\n" +
            "   end;\n" +
            "var\n" +
            "  HelloWorld: PHelloWorld; \n" +
            "  HelloWorld2: ^THelloWorld;  \n" +
            "  HelloWorld3: ^THelloWorld;                               \n" +
            "  HelloWorld4: PHelloWorld;   \n" +
            "begin\n" +
            "   New(HelloWorld);  \n" +
            "   HelloWorld4 := HelloWorld; \n" +
            "   HelloWorld2 := HelloWorld; \n" +
            "   New(HelloWorld3); \n" +
            "   HelloWorld := HelloWorld3; \n" +
            "   HelloWorld2 := HelloWorld3;\n" +
            "   Dispose(HelloWorld); \n" +
            "   Dispose(HelloWorld3);\n" +
            "end.";

    protected final static String FRAGMENT_1_CLEANED_FOR_DUPLICATION = "program ObjectPascalExample;\n" +
            "type\n" +
            "PHelloWorld = ^THelloWorld;\n" +
            "THelloWorld = object\n" +
            "procedure Put;\n" +
            "procedure THelloWorld.Put;\n" +
            "begin\n" +
            "WriteLn('Hello, World!');\n" +
            "var\n" +
            "HelloWorld: PHelloWorld;\n" +
            "HelloWorld2: ^THelloWorld;\n" +
            "HelloWorld3: ^THelloWorld;\n" +
            "HelloWorld4: PHelloWorld;\n" +
            "begin\n" +
            "New(HelloWorld);\n" +
            "HelloWorld4 := HelloWorld;\n" +
            "HelloWorld2 := HelloWorld;\n" +
            "New(HelloWorld3);\n" +
            "HelloWorld := HelloWorld3;\n" +
            "HelloWorld2 := HelloWorld3;\n" +
            "Dispose(HelloWorld);\n" +
            "Dispose(HelloWorld3);";

    protected final static String UNIT1 = "program ObjectPascalExample;\n" +
            "\n" +
            "type\n" +
            "  THelloWorld = class\n" +
            "    Put:procedure of object;\n" +
            "  end;\n" +
            "\n" +
            "procedure THelloWorld.Put;\n" +
            "begin\n" +
            "  Writeln('Hello, World!');\n" +
            "end;\n" +
            "\n" +
            "procedure THelloWorld.Free;\n" +
            "begin\n" +
            "   // dispose any pointers //\n" +
            "end;\n" +
            "\n" +
            "procedure THelloWorld.Init(var Center:TKraftVector3;const Radius:TKraftScalar);\n" +
            "begin\n" +
            "   // initialize variables\n" +
            "   // link methods (manual RTTI)\n" +
            "   with Self do begin\n" +
            "      TMethod(@Put):=[@THelloWorld.Put, @Self];\n" +
            "      TMethod(@Free):=[@THelloWorld.Free, @Self];\n" +
            "   End;\n" +
            "end;\n" +
            "\n" +
            "var\n" +
            "  HelloWorld: THelloWorld;               { this is an implicit pointer }\n" +
            "\n" +
            "begin\n" +
            "  HelloWorld.Init;                       { self initialization (pointer to an object) of type THelloWorld }\n" +
            "  HelloWorld.Put;\n" +
            "  HelloWorld.Free;                       { this line deallocates the THelloWorld object pointed to by HelloWorld }\n" +
            "end.";
}
