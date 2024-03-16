package controllers;

import org.junit.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.Assert.assertEquals;

public class SitemapTest {

    @Test
    public void sitemap() throws IOException {
        List<String> routes = Files.readAllLines(Path.of("ui/src/app/app.routes.ts")).stream()
                .filter(line -> line.contains("path:"))
                .map(line -> line.replaceAll("\\s*path: '(.*)',.*", "$1"))
                .filter(route -> !route.equals("**"))
                .toList();
        List<String> sitemap = Files.readAllLines(Path.of("ui/src/sitemap.xml")).stream()
                .filter(line -> line.contains("href="))
                .map(line -> line.replaceAll(".*href=\"(.*)\".*", "$1"))
                .map(link -> link.replaceAll("https://mooncal.ch/?", ""))
                .toList();
        assertEquals(routes, sitemap);
    }
}
