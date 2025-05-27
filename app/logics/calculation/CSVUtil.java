package logics.calculation;

import com.google.common.io.LineReader;

import java.io.FileReader;
import java.io.IOException;
import java.util.function.Consumer;

public class CSVUtil {
    private CSVUtil() {
    }

    static void load(String csvFile, Consumer<String[]> lineHandler) {
        try (FileReader fileReader = new FileReader(csvFile)) {
            LineReader lineReader = new LineReader(fileReader);
            lineReader.readLine(); //skip header
            String line;
            while ((line = lineReader.readLine()) != null) {
                String[] rows = line.split("\\t");
                lineHandler.accept(rows);
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
