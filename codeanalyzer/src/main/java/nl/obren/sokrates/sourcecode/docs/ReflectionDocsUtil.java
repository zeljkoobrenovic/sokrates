package nl.obren.sokrates.sourcecode.docs;

import nl.obren.sokrates.sourcecode.core.CodeConfiguration;
import org.apache.commons.lang3.reflect.FieldUtils;

import java.lang.reflect.Field;
import java.util.List;

public class ReflectionDocsUtil {
    public static void main(String args[]) {
        getDocumentation(CodeConfiguration.class);
    }

    private static void getDocumentation(Class clazz) {
        List<Field> fields = FieldUtils.getAllFieldsList(clazz);
        fields.forEach(field -> {
            String name = field.getName();
            if (field.getAnnotation(Documentation.class) != null) {
                Documentation annotation = field.getAnnotation(Documentation.class);
                System.out.print(name);
                String type = annotation.isList() ? "list of strings" : field.getType().getSimpleName();
                System.out.print(": [" + type + "] ");
                String description = field.getAnnotation(Documentation.class).description();
                System.out.println(description);
            } else {
                ComplexDocumentation annotation = field.getAnnotation(ComplexDocumentation.class);
                if (annotation != null) {
                    System.out.print(name);
                    System.out.print(": [Object] ");
                    String description = annotation.description();
                    System.out.println(description);
                    getDocumentation(annotation.clazz());
                }
            }
        });
    }
}
