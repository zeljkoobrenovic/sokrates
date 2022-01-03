package nl.obren.sokrates.sourcecode.lang.python;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;

import java.io.IOException;

public class JupyterNotebookCleaner {

    private final JsonFactory factory;

    public JupyterNotebookCleaner() {
        factory = new JsonFactory();
    }

    public String extract(final String content) {
        final StringBuilder result = new StringBuilder();
        try {
            // parse json
            // extract cells.celltype == code, source
            final JsonParser parser = factory.createParser(content);
            while (parser.nextToken() != null) {
                String key = parser.getCurrentName();
                if ("cell_type".equals(key)) {
                    parser.nextToken();
                    if ("code".equals(parser.getText())) {
                        // consume all until source
                        while ("source" != parser.getCurrentName()) {
                            parser.nextToken();
                        }
                        key = parser.getCurrentName();
                        if ("source".equals(key)) {
                            parser.nextToken(); // consume [ JsonToken.START_ARRAY
                            while (parser.nextToken() != JsonToken.END_ARRAY) {
                                result.append(parser.getText());
                                result.append("\n");
                            }
                        }
                        result.append("\n");
                    }
                }
            }
            parser.close();
            return result.toString();
        } catch (IOException e) {
            throw new RuntimeException("Invalid json", e);
        }
    }
}
