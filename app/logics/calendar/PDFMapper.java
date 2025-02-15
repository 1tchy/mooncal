package logics.calendar;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.WriterException;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.qrcode.QRCodeWriter;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;
import jakarta.inject.Inject;
import logics.calculation.MoonPhasesCalculation;
import models.EventInstance;
import org.apache.pdfbox.io.IOUtils;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.pdmodel.font.PDType0Font;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;
import org.vandeseer.easytable.TableDrawer;
import org.vandeseer.easytable.structure.Row;
import org.vandeseer.easytable.structure.Table;
import org.vandeseer.easytable.structure.cell.AbstractCell;
import org.vandeseer.easytable.structure.cell.ImageCell;
import org.vandeseer.easytable.structure.cell.TextCell;
import org.vandeseer.easytable.util.PdfUtil;
import play.i18n.Lang;
import play.i18n.MessagesApi;

import java.awt.*;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.vandeseer.easytable.settings.HorizontalAlignment.CENTER;
import static org.vandeseer.easytable.settings.VerticalAlignment.MIDDLE;

public class PDFMapper {

    private static final int MOON_IMAGE_SIZE = 60;
    private static final int THANK_QR_CODE_SIZE = 25;
    private static final int TABLE_OFFSET_FROM_TOP = 80;
    private static final int DAY_EVENT_TEXT_PADDING = 2;
    private static final int FONT_SIZE = 8;
    private static final PDRectangle A4_QUER = new PDRectangle(PDRectangle.A4.getHeight(), PDRectangle.A4.getWidth());
    private static final float MONTH_ROW_WIDTH = A4_QUER.getWidth() / 13;
    private static final float DAY_OF_MONTH_WIDTH = 17.5f;
    private static final float DAY_EVENT_TEXT_WIDTH = MONTH_ROW_WIDTH - DAY_OF_MONTH_WIDTH;

    private final MessagesApi messagesApi;
    private final byte[] quicksandTtf = readRessource("/Quicksand/static/Quicksand-Regular.ttf");
    private final byte[] quicksandBoldTtf = readRessource("/Quicksand/static/Quicksand-Bold.ttf");

    @Inject
    public PDFMapper(MessagesApi messagesApi) {
        this.messagesApi = messagesApi;
    }

    /**
     * @param events to map
     * @return the pdf-file
     */
    public byte[] map(Collection<EventInstance> events, Lang language) {
        ZonedDateTime zonedDateTime = events.stream().findFirst().map(EventInstance::getDateTime).orElseGet(ZonedDateTime::now);
        int year = zonedDateTime.getYear();
        Map<LocalDate, List<EventInstance>> eventsByDate = events.stream()
                .filter(event -> event.getLocalDate().getYear() == year)
                .collect(Collectors.groupingBy(EventInstance::getLocalDate));
        eventsByDate.values().forEach(eventInstances -> eventInstances.sort(Comparator.comparing(EventInstance::getEventTypeId).thenComparing(EventInstance::getDateTime)));
        Map<Integer, Integer> lengthsOfMonths = IntStream.range(1, 13).boxed().collect(Collectors.toMap(
                month -> month,
                month -> LocalDate.of(year, month, 1).lengthOfMonth()));
        try (PDDocument document = new PDDocument()) {
            PDFont font = loadFont(language.code(), document, false);
            PDFont fontBold = loadFont(language.code(), document, true);

            PDPage page = new PDPage(A4_QUER);
            document.addPage(page);
            try (PDPageContentStream contentStream = new PDPageContentStream(document, page)) {
                float startX = (A4_QUER.getWidth() - (12 * MONTH_ROW_WIDTH)) / 2;
                contentStream.beginText();
                contentStream.newLineAtOffset(startX, page.getMediaBox().getUpperRightY() - 50);
                contentStream.setFont(fontBold, 18);
                contentStream.showText(messagesApi.get(language, "pdf.title") + " " + year);
                contentStream.endText();
                contentStream.drawImage(loadImage("/public/favicon/31/web-app-manifest-192x192.png", document),
                        page.getMediaBox().getUpperRightX() - startX - MOON_IMAGE_SIZE,
                        (float) (page.getMediaBox().getUpperRightY() - ((TABLE_OFFSET_FROM_TOP - MOON_IMAGE_SIZE) / 2. + MOON_IMAGE_SIZE)),
                        MOON_IMAGE_SIZE, MOON_IMAGE_SIZE);

                Table.TableBuilder table = Table.builder()
                        .font(font).fontSize(FONT_SIZE)
                        .borderWidth(0.5f);
                IntStream.rangeClosed(1, 12).forEach(month -> table.addColumnsOfWidth(DAY_OF_MONTH_WIDTH, DAY_EVENT_TEXT_WIDTH));

                Row.RowBuilder monthRowBuilder = Row.builder();
                IntStream.rangeClosed(1, 12)
                        .mapToObj(monthInt -> messagesApi.get(language, "pdf.month." + monthInt))
                        .forEach(monthName -> monthRowBuilder.add(TextCell.builder()
                                .text(monthName)
                                .font(fontBold)
                                .horizontalAlignment(CENTER)
                                .colSpan(2)
                                .build()));
                table.addRow(monthRowBuilder.build());

                for (int day = 1; day <= 31; day++) {
                    Row.RowBuilder row = Row.builder();
                    for (int month = 1; month <= 12; month++) {
                        if (day > lengthsOfMonths.get(month)) {
                            row.add(TextCell.builder().text("").colSpan(2).build());
                            continue;
                        }
                        LocalDate date = LocalDate.of(year, month, day);
                        List<EventInstance> eventInstancesAtDay = eventsByDate.getOrDefault(date, List.of());
                        Color backgroundColor = switch (date.getDayOfWeek()) {
                            case SATURDAY -> new Color(240, 240, 240);
                            case SUNDAY -> new Color(225, 225, 225);
                            default -> null;
                        };
                        row.add(draftDayOfMonthCell(day, getMoonIconFilename(eventInstancesAtDay), document)
                                .backgroundColor(backgroundColor)
                                .horizontalAlignment(CENTER)
                                .verticalAlignment(MIDDLE)
                                .borderWidthRight(0)
                                .build());

                        TextWithSize textWithSize = calculateOptimalTextWithSize(eventInstancesAtDay, font, DAY_EVENT_TEXT_WIDTH - 2 * DAY_EVENT_TEXT_PADDING);
                        row.add(TextCell.builder().text(textWithSize.text())
                                .fontSize(textWithSize.size())
                                .backgroundColor(backgroundColor)
                                .borderWidthLeft(0)
                                .padding(DAY_EVENT_TEXT_PADDING)
                                .verticalAlignment(MIDDLE)
                                .build());
                    }
                    table.addRow(row.build());
                }

                TableDrawer tableDrawer = TableDrawer.builder()
                        .contentStream(contentStream)
                        .startX(startX)
                        .startY(page.getMediaBox().getUpperRightY() - TABLE_OFFSET_FROM_TOP)
                        .table(table.build())
                        .build();
                tableDrawer.draw();

                contentStream.beginText();
                contentStream.setFont(font, FONT_SIZE);
                contentStream.newLineAtOffset(startX, tableDrawer.getFinalY() - (THANK_QR_CODE_SIZE + FONT_SIZE - 2) / 2f);
                contentStream.showText(messagesApi.get(language, "pdf.timezone") + ": " + zonedDateTime.getZone().getId());

                String thankUrl = "https://mooncal.ch/" + getThankUrl(language);
                float textWidth = PdfUtil.getStringWidth(thankUrl, font, FONT_SIZE);
                contentStream.newLineAtOffset(page.getMediaBox().getUpperRightX() - 2 * startX - textWidth - THANK_QR_CODE_SIZE, 0);
                contentStream.showText(thankUrl);
                contentStream.endText();

                contentStream.drawImage(generateQRCodeImage(thankUrl + "?c=pdf", document),
                        page.getMediaBox().getUpperRightX() - startX - THANK_QR_CODE_SIZE,
                        tableDrawer.getFinalY() - THANK_QR_CODE_SIZE,
                        THANK_QR_CODE_SIZE, THANK_QR_CODE_SIZE);
            }

            var os = new ByteArrayOutputStream();
            document.save(os);
            return os.toByteArray();
        } catch (
                IOException e) {
            throw new RuntimeException(e);
        }
    }

    private PDFont loadFont(String langCode, PDDocument document, boolean bold) throws IOException {
        if (langCode.equals("hi")) {
            //not loaded in static memory because it's 1MB, and I'm not sure how often it will be used
            return PDType0Font.load(document, PDFMapper.class.getResourceAsStream("/Jaldi/" + (bold ? "Jaldi-Bold.ttf" : "Jaldi-Regular.ttf")));
        } else {
            return PDType0Font.load(document, new ByteArrayInputStream(bold ? quicksandBoldTtf : quicksandTtf));
        }
    }

    private Optional<String> getMoonIconFilename(List<EventInstance> eventInstances) {
        return eventInstances.stream()
                .map(eventInstance -> switch (eventInstance.getEventTypeId()) {
                    case MoonPhasesCalculation.FULLMOON_EVENT_TYPE_ID -> "/public/emoji/full.png";
                    case MoonPhasesCalculation.NEWMOON_EVENT_TYPE_ID -> "/public/emoji/new.png";
                    case MoonPhasesCalculation.FIRST_QUARTER_EVENT_TYPE_ID -> "/public/emoji/first-quarter.png";
                    case MoonPhasesCalculation.LAST_QUARTER_EVENT_TYPE_ID -> "/public/emoji/last-quarter.png";
                    default -> null;
                }).filter(Objects::nonNull)
                .findFirst();
    }

    private AbstractCell.AbstractCellBuilder<?, ?> draftDayOfMonthCell(int day, Optional<String> moonIconFilename, PDDocument document) throws IOException {
        if (moonIconFilename.isPresent()) {
            return ImageCell.builder()
                    .image(loadImage(moonIconFilename.get(), document))
                    .scale(0.05f)
                    .padding(0);
        } else {
            return TextCell.builder()
                    .text(Integer.toString(day))
                    .padding(4)
                    .paddingLeft(0)
                    .paddingRight(0);
        }
    }

    private TextWithSize calculateOptimalTextWithSize(List<EventInstance> eventInstancesAtDay, PDFont font, float maxWidth) {
        int fontSizeA = 6;
        int fontSizeB = 4;
        int maxLinesA = 1;
        int maxLinesB = 2;
        String withAllEvents = eventInstancesAtDay.stream()
                .map(EventInstance::getPDFTitle)
                .collect(Collectors.joining(", "));
        if (PdfUtil.getOptimalTextBreakLines(withAllEvents, font, fontSizeA, maxWidth).size() <= maxLinesA) {
            return new TextWithSize(withAllEvents, fontSizeA);
        } else {
            if (PdfUtil.getOptimalTextBreakLines(withAllEvents, font, fontSizeB, maxWidth).size() <= maxLinesB) {
                return new TextWithSize(withAllEvents, fontSizeB);
            } else {
                String withoutMoonPhases = eventInstancesAtDay.stream()
                        .filter(eventInstance -> !eventInstance.getEventTypeId().equals(MoonPhasesCalculation.FULLMOON_EVENT_TYPE_ID)
                                && !eventInstance.getEventTypeId().equals(MoonPhasesCalculation.NEWMOON_EVENT_TYPE_ID))
                        .map(EventInstance::getPDFTitle)
                        .collect(Collectors.joining(", "));
                String text = cropIfWider(withoutMoonPhases, maxWidth, maxLinesB, font, fontSizeB);
                return new TextWithSize(text, fontSizeB);
            }
        }
    }

    private String cropIfWider(String text, float maxWidth, int maxLines, PDFont font, int fontSize) {
        while (PdfUtil.getOptimalTextBreakLines(text, font, fontSize, maxWidth).size() > maxLines) {
            text = text.substring(0, text.length() - 2) + "…";
        }
        return text;
    }

    private PDImageXObject loadImage(String name, PDDocument document) throws IOException {
        byte[] imageBytes = IOUtils.toByteArray(Objects.requireNonNull(getClass().getResourceAsStream(name)));
        return PDImageXObject.createFromByteArray(document, imageBytes, "moon");
    }

    String getThankUrl(Lang lang) {
        return messagesApi.get(lang, "navigation.thank");
    }

    public static PDImageXObject generateQRCodeImage(String text, PDDocument document) {
        QRCodeWriter barcodeWriter = new QRCodeWriter();
        try {
            var bitMatrix = barcodeWriter.encode(text, BarcodeFormat.QR_CODE, 200, 200,
                    Map.of(EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.L));

            var os = new ByteArrayOutputStream();
            MatrixToImageWriter.writeToStream(bitMatrix, "PNG", os);
            return PDImageXObject.createFromByteArray(document, os.toByteArray(), "qr");
        } catch (WriterException e) {
            throw new RuntimeException(e);
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    private static byte[] readRessource(String ressourceName) {
        try {
            return Objects.requireNonNull(PDFMapper.class.getResourceAsStream(ressourceName)).readAllBytes();
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    private record TextWithSize(String text, int size) {
    }
}
