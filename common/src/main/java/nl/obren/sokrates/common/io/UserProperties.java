/*
 * Copyright (c) 2019 Željko Obrenović. All rights reserved.
 */

package nl.obren.sokrates.common.io;

import com.fasterxml.jackson.core.JsonProcessingException;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

public class UserProperties {
    private static final Log LOG = LogFactory.getLog(UserProperties.class);
    private static UserProperties instance;
    private Properties properties = new Properties();
    private boolean readOnly;
    private String type;

    public UserProperties(String type) {
        this.type = type;
    }

    public static File getUserPropertiesFolder(String type) {
        return new File(System.getProperty("user.home"), ".leandocs");
    }

    public static File getUserPropertiesFile(String type) {
        return new File(getUserPropertiesFolder(type), type + ".properties");
    }

    public static UserProperties getInstance(String type) {
        if (instance == null) {
            instance = new UserProperties(type);
            instance.load();
        }
        return instance;
    }

    public static void setInstance(UserProperties instance) {
        UserProperties.instance = instance;
    }

    public File getFileProperty(String propertyName) {
        String propertyValue = properties.getProperty(propertyName);
        if (propertyValue != null) {
            File file = new File(propertyValue);
            if (file.exists()) {
                return file;
            } else {
                return null;
            }
        }
        return null;
    }

    public void setProperty(String name, File value) {
        properties.setProperty(name, value.getPath());
        save();
    }

    public void addToListProperty(String name, File value) {
        List<File> files = getFileListProperty(name);
        List<String> paths = new ArrayList<>();
        if (value.exists()) {
            try {
                files.remove(value);
                files.add(0, value);
                files.forEach(file -> paths.add(file.getPath()));
                properties.setProperty(name, new JsonGenerator().generate(paths));
                save();
            } catch (JsonProcessingException e) {
                LOG.error(e);
            }
        }
    }

    public List<File> getFileListProperty(String propertyName) {
        List<File> files = new ArrayList<>();
        String property = properties.getProperty(propertyName);
        if (property != null) {
            try {
                List list = (List) new JsonMapper().getObject(property, List.class);
                list.forEach(item -> {
                    File file = new File(item.toString());
                    if (file.exists() && !files.contains(file)) {
                        files.add(file);
                    }
                });
            } catch (IOException e) {
                LOG.error(e);
            }
        }
        return files;
    }

    public void load() {
        try {
            File userConfigurationFile = getUserPropertiesFile(type);
            if (userConfigurationFile.exists()) {
                properties.load(new FileInputStream(userConfigurationFile));
            }
        } catch (IOException e) {
            LOG.error(e);
        }
    }

    public void save() {
        if (!readOnly) {
            try {
                File userConfigurationFile = getUserPropertiesFile(type);
                if (!userConfigurationFile.getParentFile().exists()) {
                    userConfigurationFile.getParentFile().mkdirs();
                }
                properties.store(new FileOutputStream(userConfigurationFile), type);

            } catch (IOException e) {
                LOG.error(e);
            }
        }
    }

    public boolean isReadOnly() {
        return readOnly;
    }

    public void setReadOnly(boolean readOnly) {
        this.readOnly = readOnly;
    }
}
