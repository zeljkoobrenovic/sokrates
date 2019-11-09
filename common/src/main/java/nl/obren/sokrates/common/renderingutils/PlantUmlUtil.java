package nl.obren.sokrates.common.renderingutils;

import net.sourceforge.plantuml.FileFormat;
import net.sourceforge.plantuml.FileFormatOption;
import net.sourceforge.plantuml.SourceStringReader;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

public class PlantUmlUtil {
    private static final Log LOG = LogFactory.getLog(PlantUmlUtil.class);
    public static final String START_UML = "@startuml";
    public static final String END_UML = "@enduml";

    public static String getSvg(String source) {
        SourceStringReader reader = new SourceStringReader(addTagsIfNecessary(source));
        final ByteArrayOutputStream os = new ByteArrayOutputStream();

        try {
            String desc = reader.generateImage(os, new FileFormatOption(FileFormat.SVG));
            os.close();
        } catch (IOException e) {
            LOG.error(e);
        }

        String svg = new String(os.toByteArray(), StandardCharsets.UTF_8);
        return svg.substring(svg.indexOf("<svg"));
    }

    private static String addTagsIfNecessary(String source) {
        if (!source.startsWith(START_UML)) {
            source = START_UML + "\n" + source;
        }
        if (!source.endsWith(END_UML)) {
            source += "\n" + END_UML;
        }
        return source;
    }
}
