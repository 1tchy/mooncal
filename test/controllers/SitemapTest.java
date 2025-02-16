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
        List<String> routes = Files.readAllLines(Path.of("ui/src/app/app.routes.compiled.spec.ts")).stream()
                .filter(line -> line.contains("path:"))
                .map(line -> line.replaceAll("\\s*path: '(.*)',.*", "$1"))
                .filter(route -> !route.contains("buymeacoffee"))
                .filter(route -> !route.equals("**"))
                .toList();
        List<String> sitemap = Files.readAllLines(Path.of("ui/src/sitemap.xml")).stream()
                .filter(line -> line.contains("href="))
                .filter(line -> !line.contains("mooncal.pdf"))
                .map(line -> line.replaceAll(".*href=\"(.*)\".*", "$1"))
                .map(link -> link.replaceAll("https://mooncal.ch/?", ""))
                .toList();
        assertEquals(routes, sitemap);
    }

    @Test
    public void pdfLinksAreAllowedInRobotsTxt() throws IOException {
        List<String> sitemap = Files.readAllLines(Path.of("ui/src/sitemap.xml")).stream()
                .filter(line -> line.contains("href="))
                .filter(line -> line.contains("mooncal.pdf"))
                .map(line -> line.replaceAll(".*href=\"(.*)\".*", "$1"))
                .map(link -> link.replaceAll("https://mooncal.ch", ""))
                .map(link -> link.replaceAll("&amp;", "&"))
                .map(link -> link.replaceAll("2025", "*"))
                .toList();
        List<String> robots = Files.readAllLines(Path.of("ui/src/robots.txt")).stream()
                .filter(line -> line.contains("Allow:"))
                .filter(line -> line.contains("mooncal.pdf"))
                .map(line -> line.replaceAll("Allow:\\s+", ""))
                .toList();
        assertEquals(robots, sitemap);
    }
}
