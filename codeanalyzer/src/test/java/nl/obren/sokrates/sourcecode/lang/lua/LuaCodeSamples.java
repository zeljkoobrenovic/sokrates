/*
 * Copyright (c) 2021 Željko Obrenović. All rights reserved.
 */

package nl.obren.sokrates.sourcecode.lang.lua;

public class LuaCodeSamples {
    protected final static String FRAGMENT_1 = "-- add all elements of array `a'\n" +
            "    function add (a)\n" +
            "      local sum = 0\n" +
            "      for i,v in ipairs(a) do\n" +
            "        sum = sum + v\n" +
            "      end\n" +
            "      return sum\n" +
            "    end\n" +
            "    function b()\n" +
            "        --condition = true\n" +
            "        \n" +
            "        while condition do\n" +
            "            --statements\n" +
            "        end\n" +
            "        \n" +
            "        repeat\n" +
            "          --statements\n" +
            "        until condition\n" +
            "        \n" +
            "        for i = first, last, delta do  --delta may be negative, allowing the for loop to count down or up\n" +
            "          --statements\n" +
            "          --example: print(i)\n" +
            "        end\n" +
            "    end\n";
    protected final static String FRAGMENT_1_CLEANED = "    function add (a)\n" +
            "      local sum = 0\n" +
            "      for i,v in ipairs(a) do\n" +
            "        sum = sum + v\n" +
            "      end\n" +
            "      return sum\n" +
            "    end\n" +
            "    function b()\n" +
            "        while condition do\n" +
            "        end\n" +
            "        repeat\n" +
            "        until condition\n" +
            "        for i = first, last, delta do  \n" +
            "        end\n" +
            "    end";

    protected final static String FRAGMENT_1_CLEANED_FOR_DUPLICATION = "function add (a)\n" +
            "local sum = 0\n" +
            "for i,v in ipairs(a) do\n" +
            "sum = sum + v\n" +
            "return sum\n" +
            "function b()\n" +
            "while condition do\n" +
            "repeat\n" +
            "until condition\n" +
            "for i = first, last, delta do";
}
