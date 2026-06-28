package controllers;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import play.mvc.Http;
import play.mvc.Result;
import play.test.WithApplication;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static play.test.Helpers.*;

class SuggestCalendarActionTest extends WithApplication {

    private Path tempFile;

    @BeforeEach
    void setUpPlay() throws IOException {
        tempFile = Files.createTempFile("calendar-suggestions-test-", ".txt");
        System.setProperty("calendar-suggestions.txt", tempFile.toString());
        startPlay();
    }

    @AfterEach
    void tearDownPlay() throws IOException {
        stopPlay();
        System.clearProperty("calendar-suggestions.txt");
        Files.deleteIfExists(tempFile);
    }

    @Test
    void cleanSubmissionAppendsLineToFile() throws IOException {
        Http.RequestBuilder request = fakeRequest()
                .method("POST")
                .uri("/suggestCalendar")
                .bodyForm(Map.of(
                        "title", "Biodynamic Calendar",
                        "methodology", "Rudolf Steiner method",
                        "source", "https://example.com/biodynamic"
                ));

        Result result = route(app, request);

        assertEquals(NO_CONTENT, result.status());
        String fileContent = Files.readString(tempFile);
        assertTrue(fileContent.contains("Biodynamic Calendar"),
                "Expected file to contain the title, but got: " + fileContent);
        long lineCount = fileContent.lines().filter(l -> !l.isBlank()).count();
        assertEquals(1, lineCount, "Expected exactly one non-blank line appended");
    }

    @Test
    void honeypotFilledSubmissionLeavesFileUnchanged() throws IOException {
        // Pre-populate file to check nothing is added
        Files.writeString(tempFile, "existing line\n");

        Http.RequestBuilder request = fakeRequest()
                .method("POST")
                .uri("/suggestCalendar")
                .bodyForm(Map.of(
                        "title", "Spam Calendar",
                        "methodology", "Spam method",
                        "source", "https://spam.example.com",
                        "website", "http://bot-filled-this.example.com"
                ));

        Result result = route(app, request);

        assertEquals(NO_CONTENT, result.status());
        String fileContent = Files.readString(tempFile);
        assertEquals("existing line\n", fileContent,
                "File should be unchanged when honeypot is triggered");
    }
}
