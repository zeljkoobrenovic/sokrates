/*
 * Copyright (c) 2020 Željko Obrenović. All rights reserved.
 */

package nl.obren.sokrates.sourcecode.lang.ts;

public class TypeScriptCodeFragments {
    protected final static String UNITS_CODE = "class EditorIntOption<K1 extends EditorOption> extends SimpleEditorOption<K1, number> {\n" +
            "\n" +
            "\tpublic static clampedInt(value: any, defaultValue: number, minimum: number, maximum: number): number {\n" +
            "\t\tlet r: number;\n" +
            "\t\tif (typeof value === 'undefined') {\n" +
            "\t\t\tr = defaultValue;\n" +
            "\t\t} else {\n" +
            "\t\t\tr = parseInt(value, 10);\n" +
            "\t\t\tif (isNaN(r)) {\n" +
            "\t\t\t\tr = defaultValue;\n" +
            "\t\t\t}\n" +
            "\t\t}\n" +
            "\t\tr = Math.max(minimum, r);\n" +
            "\t\tr = Math.min(maximum, r);\n" +
            "\t\treturn r | 0;\n" +
            "\t}\n" +
            "\n" +
            "\tpublic readonly minimum: number;\n" +
            "\tpublic readonly maximum: number;\n" +
            "\n" +
            "\tconstructor(id: K1, name: PossibleKeyName<number>, defaultValue: number, minimum: number, maximum: number, schema: IConfigurationPropertySchema | undefined = undefined) {\n" +
            "\t\tif (typeof schema !== 'undefined') {\n" +
            "\t\t\tschema.type = 'integer';\n" +
            "\t\t\tschema.default = defaultValue;\n" +
            "\t\t\tschema.minimum = minimum;\n" +
            "\t\t\tschema.maximum = maximum;\n" +
            "\t\t}\n" +
            "\t\tsuper(id, name, defaultValue, schema);\n" +
            "\t\tthis.minimum = minimum;\n" +
            "\t\tthis.maximum = maximum;\n" +
            "\t}\n" +
            "\n" +
            "\tpublic validate(input: any): number {\n" +
            "\t\treturn EditorIntOption.clampedInt(input, this.defaultValue, this.minimum, this.maximum);\n" +
            "\t}\n" +
            "}";
}
