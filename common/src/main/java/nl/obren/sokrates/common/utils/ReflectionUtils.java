/*
 * Copyright (c) 2019 Željko Obrenović. All rights reserved.
 */

package nl.obren.sokrates.common.utils;

import com.fasterxml.jackson.core.JsonProcessingException;
import nl.obren.sokrates.common.io.JsonGenerator;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;

public class ReflectionUtils {
    private static final Log LOG = LogFactory.getLog(ReflectionUtils.class);

    public static List<Field> getAllFields(Class<?> type) {
        List<Field> fields = new ArrayList<>();
        for (Field field : type.getDeclaredFields()) {
            if (!field.getName().equals("this$0")) {
                fields.add(field);
            }
        }

        if (type.getSuperclass() != null) {
            fields.addAll(getAllFields(type.getSuperclass()));
        }

        return fields;
    }

    public static Object getFieldValue(Object object, Class fieldClass) {
        for (Field field : ReflectionUtils.getAllFields(object.getClass())) {
            Object fieldValue;
            try {
                field.setAccessible(true);
                fieldValue = field.get(object);
                if (fieldValue != null && fieldValue.getClass() == fieldClass) {
                    return fieldValue;
                }
            } catch (IllegalAccessException e) {
                LOG.error(e);
            }
        }

        return null;
    }

    public static Object getFieldValue(Object object, String fieldName) {
        for (Field field : ReflectionUtils.getAllFields(object.getClass())) {
            Object fieldValue;
            try {
                field.setAccessible(true);
                fieldValue = field.get(object);
                if (fieldValue != null && field.getName().equals(fieldName)) {
                    return fieldValue;
                }
            } catch (IllegalAccessException e) {
                LOG.error(e);
            }
        }

        return null;
    }


    public static boolean compareWithDefaultJson(Class objectClass, String json) {
        try {
            Object defaultObject = objectClass.getConstructor().newInstance();
            String defaultJson = new JsonGenerator().generate(defaultObject);
            return defaultJson.equals(json);
        } catch (InstantiationException | IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
            LOG.error(e);
        } catch (JsonProcessingException e) {
            LOG.error(e);
        }

        return false;
    }
}
