import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.util.DefaultIndenter;
import com.fasterxml.jackson.core.util.DefaultPrettyPrinter;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.collect.Streams;
import logics.calculation.Translator;
import org.junit.Test;
import play.libs.Json;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.junit.Assert.assertEquals;

public class UiTranslatorToolTest {

    private final Translator translator = new Translator();

    @Test
    public void translate() throws InterruptedException, IOException {
        var objectMapper = new ObjectMapper()
                .configure(SerializationFeature.INDENT_OUTPUT, true)
                .setDefaultPrettyPrinter(new MyPrettyPrinter());
        String dePath = "ui/src/app/messages.de.json";
        ObjectNode de = (ObjectNode) objectMapper.readTree(Files.readAllBytes(Path.of(dePath).toAbsolutePath()));
        List<String> deKeys = getKeyPaths(de.fields()).toList();
        Map<String, String[]> deTranslations = deKeys.stream().collect(Collectors.toMap(Function.identity(), key -> {
            JsonNode node = resolveKeyPath(key, de);
            if (node.isArray()) {
                return Streams.stream(node.elements()).map(JsonNode::asText).toArray(String[]::new);
            }
            return new String[]{node.asText()};
        }));
        for (String otherLanguage : List.of("en", "es", "fr", "hi", "nl", "ro")) {
            String other = Files.readString(Path.of(dePath.replace("de", otherLanguage)));
            ObjectNode otherJson = (ObjectNode) Json.parse(other);
            List<String> otherKeys = getKeyPaths(otherJson.fields()).toList();
            deKeys.stream()
                    .filter(key -> !otherKeys.contains(key))
                    .forEach(missingKey -> addTranslation(missingKey, otherJson, otherLanguage, deTranslations));
            assertEquals(other, objectMapper.writeValueAsString(otherJson) + "\n");
        }
    }

    private void addTranslation(String missingKey, ObjectNode otherJson, String otherLanguage, Map<String, String[]> deTranslations) {
        try {
            ObjectNode parent = (ObjectNode) resolveKeyPath(getParentKey(missingKey), otherJson);
            String[] deTexts = deTranslations.get(missingKey);
            if (deTexts.length == 1) {
                parent.put(getFieldKey(missingKey), translator.translate("de", otherLanguage, deTexts[0]));
            } else {
                ArrayNode arrayNode = parent.putArray(getFieldKey(missingKey));
                for (String deText : deTexts) {
                    arrayNode.add(translator.translate("de", otherLanguage, deText));
                }
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static String getParentKey(String missingKey) {
        return missingKey.replaceAll("\\.\\w+$", "");
    }

    private static String getFieldKey(String missingKey) {
        return missingKey.replaceAll("\\w+\\.", "");
    }

    private Stream<String> getKeyPaths(Iterator<Map.Entry<String, JsonNode>> fields) {
        return Streams.stream(fields).flatMap(field ->
                field.getValue().isObject() ?
                        getKeyPaths(field.getValue().fields()).map(subKey -> field.getKey() + "." + subKey) :
                        Stream.of(field.getKey())
        );
    }

    private static JsonNode resolveKeyPath(String keyPath, JsonNode node) {
        String[] keys = keyPath.split("\\.");
        for (String key : keys) {
            node = node.get(key);
        }
        return node;
    }

    private static class MyPrettyPrinter extends DefaultPrettyPrinter {
        public MyPrettyPrinter() {
            indentArraysWith(DefaultIndenter.SYSTEM_LINEFEED_INSTANCE);
        }

        @Override
        public DefaultPrettyPrinter createInstance() {
            return new MyPrettyPrinter();
        }

        @Override
        public void writeObjectFieldValueSeparator(JsonGenerator g) throws IOException {
            g.writeRaw(": ");
        }
    }
}
