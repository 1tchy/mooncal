package logics.calculation;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import java.util.stream.Collectors;

public class Translator {

    private final String deploymentId;

    public Translator() {
        this.deploymentId = Optional.ofNullable(System.getProperty("googleTranslateScriptDeploymentId"))
                .orElseGet(() -> {
                    try {
                        return Files.readString(Path.of("keys/googleTranslateScriptDeploymentId.txt"));
                    } catch (IOException e) {
                        throw new IllegalStateException("Missing configuration for googleTranslateScriptDeploymentId", e);
                    }
                });
    }

    public String translate(String langFrom, String langTo, String text) throws IOException {
        URL url = URI.create("https://script.google.com/macros/s/" + deploymentId + "/exec?source=" + langFrom + "&target=" + langTo +
                "&q=" + URLEncoder.encode(text, StandardCharsets.UTF_8)).toURL();
        var urlConnection = url.openConnection();
        urlConnection.setRequestProperty("User-Agent", "Mozilla/5.0");
        try (BufferedReader in = new BufferedReader(new InputStreamReader(urlConnection.getInputStream()))) {
            return in.lines().collect(Collectors.joining());
        }
    }
}
