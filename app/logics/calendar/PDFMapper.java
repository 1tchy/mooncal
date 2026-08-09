package logics.calendar;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.WriterException;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.qrcode.QRCodeWriter;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;
import jakarta.inject.Inject;
import logics.calculation.GardenBiodynamicCalculation;
import logics.calculation.MoonPhasesCalculation;
import models.EventInstance;
import models.Hemisphere;
import org.apache.pdfbox.io.IOUtils;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDDocumentInformation;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.pdmodel.font.PDType0Font;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;
import org.apache.pdfbox.pdmodel.interactive.action.PDActionURI;
import org.apache.pdfbox.pdmodel.interactive.annotation.PDAnnotationLink;
import org.jetbrains.annotations.VisibleForTesting;
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
import java.time.LocalTime;
import java.time.ZonedDateTime;
import java.util.*;
import java.util.List;
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
    // Garden events are shown in the grid as small icons rather than names.
    private static final float GARDEN_ICON_SIZE = 7f;
    private static final float GARDEN_ICON_GAP = 1.5f;
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
    public byte[] map(Collection<EventInstance> events, Lang language, Hemisphere hemisphere) {
        ZonedDateTime zonedDateTime = events.stream().findFirst().map(EventInstance::getDateTime).orElseGet(ZonedDateTime::now);
        int year = zonedDateTime.getYear();
        Map<LocalDate, List<EventInstance>> eventsByDate = new HashMap<>();
        for (EventInstance e : events) {
            // getDisplayStart/EndLocalDate apply the shared garden edge-day trimming (so PDF and iCal agree).
            LocalDate start = e.getDisplayStartLocalDate();
            LocalDate end = e.isMultiDay() ? e.getDisplayEndLocalDate() : start;
            for (LocalDate d = start; !d.isAfter(end); d = d.plusDays(1)) {
                if (d.getYear() == year) {
                    eventsByDate.computeIfAbsent(d, k -> new ArrayList<>()).add(e);
                }
            }
        }
        eventsByDate.forEach((date, eventInstances) -> eventInstances.sort(dayEventOrder(date)));
        Map<Integer, Integer> lengthsOfMonths = IntStream.range(1, 13).boxed().collect(Collectors.toMap(
                month -> month,
                month -> LocalDate.of(year, month, 1).lengthOfMonth()));
        try (PDDocument document = new PDDocument()) {
            PDDocumentInformation documentInformation = document.getDocumentInformation();
            String title = messagesApi.get(language, "pdf.title") + " " + year;
            documentInformation.setTitle(title);
            documentInformation.setProducer("https://mooncal.ch");
            document.getDocumentCatalog().setLanguage(language.code());
            PDFont font = loadFont(language.code(), document, false);
            PDFont fontBold = loadFont(language.code(), document, true);

            PDPage page = new PDPage(A4_QUER);
            document.addPage(page);
            try (PDPageContentStream contentStream = new PDPageContentStream(document, page)) {
                float startX = (A4_QUER.getWidth() - (12 * MONTH_ROW_WIDTH)) / 2;
                contentStream.beginText();
                contentStream.newLineAtOffset(startX, page.getMediaBox().getUpperRightY() - 50);
                contentStream.setFont(fontBold, 18);
                contentStream.showText(title);
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
                Map<String, PDImageXObject> loadedImagesCache = new HashMap<>(4);
                record IconPlacement(int day, int month, List<String> icons) {}
                List<IconPlacement> iconPlacements = new ArrayList<>();

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
                        row.add(draftDayOfMonthCell(day, getMoonIconFilename(eventInstancesAtDay, hemisphere), document, loadedImagesCache)
                                .backgroundColor(backgroundColor)
                                .horizontalAlignment(CENTER)
                                .verticalAlignment(MIDDLE)
                                .borderWidthRight(0)
                                .build());

                        // Garden events are shown as icons (drawn on top of the table afterwards); everything
                        // else keeps its text. The text cell reserves left padding so the two never overlap.
                        List<String> dayIcons = new ArrayList<>();
                        boolean ascending = false;
                        boolean descending = false;
                        for (EventInstance e : eventInstancesAtDay) {
                            String icon = gardenIconResource(e.getEventTypeId());
                            if (icon == null) {
                                continue;
                            }
                            // Show each day symbol (root/leaf/flower/... ) at most once per day.
                            if (!dayIcons.contains(icon)) {
                                dayIcons.add(icon);
                            }
                            ascending |= e.getTitle().contains(GardenBiodynamicCalculation.ASCENDING_MOON_MARKER);
                            descending |= e.getTitle().contains(GardenBiodynamicCalculation.DESCENDING_MOON_MARKER);
                        }
                        // The declination phase is the same for all plant-part events on a day, so append its
                        // marker once after the day symbols rather than repeating it behind each one.
                        if (ascending) {
                            dayIcons.add("/public/emoji/ascending.png");
                        }
                        if (descending) {
                            dayIcons.add("/public/emoji/descending.png");
                        }
                        if (!dayIcons.isEmpty()) {
                            iconPlacements.add(new IconPlacement(day, month, dayIcons));
                        }
                        float iconArea = dayIcons.size() * (GARDEN_ICON_SIZE + GARDEN_ICON_GAP);
                        List<EventInstance> textEvents = eventInstancesAtDay.stream()
                                .filter(e -> !e.isGardenEvent())
                                .toList();
                        TextWithSize textWithSize = calculateOptimalTextWithSize(textEvents, font,
                                DAY_EVENT_TEXT_WIDTH - 2 * DAY_EVENT_TEXT_PADDING - iconArea);
                        row.add(TextCell.builder().text(textWithSize.text())
                                .fontSize(textWithSize.size())
                                .backgroundColor(backgroundColor)
                                .borderWidthLeft(0)
                                .paddingTop(DAY_EVENT_TEXT_PADDING)
                                .paddingBottom(DAY_EVENT_TEXT_PADDING)
                                .paddingLeft(DAY_EVENT_TEXT_PADDING + iconArea)
                                .paddingRight(DAY_EVENT_TEXT_PADDING)
                                .verticalAlignment(MIDDLE)
                                .build());
                    }
                    table.addRow(row.build());
                }

                Table builtTable = table.build();
                TableDrawer tableDrawer = TableDrawer.builder()
                        .contentStream(contentStream)
                        .startX(startX)
                        .startY(page.getMediaBox().getUpperRightY() - TABLE_OFFSET_FROM_TOP)
                        .table(builtTable)
                        .build();
                tableDrawer.draw();

                // Overlay the garden icons into their day cells using the table's computed row heights
                // (row 0 is the month-name header, so calendar day N is table row N).
                if (!iconPlacements.isEmpty()) {
                    List<Row> tableRows = builtTable.getRows();
                    float tableTopY = page.getMediaBox().getUpperRightY() - TABLE_OFFSET_FROM_TOP;
                    float[] rowTopY = new float[tableRows.size()];
                    float cursorY = tableTopY;
                    for (int r = 0; r < tableRows.size(); r++) {
                        rowTopY[r] = cursorY;
                        cursorY -= tableRows.get(r).getHeight();
                    }
                    for (IconPlacement placement : iconPlacements) {
                        int rowIndex = placement.day();
                        float rowCenterY = rowTopY[rowIndex] - tableRows.get(rowIndex).getHeight() / 2f;
                        float cellX = startX + (placement.month() - 1) * MONTH_ROW_WIDTH + DAY_OF_MONTH_WIDTH;
                        float iconX = cellX + DAY_EVENT_TEXT_PADDING;
                        for (String iconResource : placement.icons()) {
                            PDImageXObject icon = loadedImagesCache.computeIfAbsent(iconResource, f -> loadImage(f, document));
                            contentStream.drawImage(icon, iconX, rowCenterY - GARDEN_ICON_SIZE / 2f, GARDEN_ICON_SIZE, GARDEN_ICON_SIZE);
                            iconX += GARDEN_ICON_SIZE + GARDEN_ICON_GAP;
                        }
                    }
                }

                float footerY = tableDrawer.getFinalY() - (THANK_QR_CODE_SIZE + FONT_SIZE - 2) / 2f;

                // Explain the garden icons shown in the grid, one wrapped paragraph per icon.
                // No moon-gardening disclaimer in the PDF (it lives in the .ics description and the web UI).
                boolean hasBiodynamic = events.stream().anyMatch(e -> e.getEventTypeId().startsWith("garden-biodynamic-"));
                if (hasBiodynamic) {
                    int legendFontSize = FONT_SIZE - 2;
                    float legendLeading = legendFontSize + 1f;
                    float legendIndent = GARDEN_ICON_SIZE + 3f;
                    float legendMaxWidth = 12 * MONTH_ROW_WIDTH - legendIndent;

                    List<String[]> entries = new ArrayList<>(); // {iconResource, messageKey}
                    for (String part : new String[]{"root", "leaf", "flower", "fruit"}) {
                        entries.add(new String[]{"/public/emoji/" + part + ".png", "garden.pdf.legend." + part});
                    }
                    entries.add(new String[]{"/public/emoji/badday.png", "garden.pdf.legend.badday"});
                    entries.add(new String[]{"/public/emoji/ascending.png", "garden.pdf.legend.ascending"});
                    entries.add(new String[]{"/public/emoji/descending.png", "garden.pdf.legend.descending"});

                    // Flatten to lines; the first line of each entry carries its icon. Entries follow one
                    // another without a blank line — the icon on each first line separates them — to keep the
                    // legend compact enough to sit below the calendar grid and clear of the footer.
                    List<String> legendLines = new ArrayList<>();
                    List<String> legendLineIcon = new ArrayList<>();
                    for (String[] entry : entries) {
                        List<String> wrapped = PdfUtil.getOptimalTextBreakLines(
                                messagesApi.get(language, entry[1]), font, legendFontSize, legendMaxWidth);
                        for (int i = 0; i < wrapped.size(); i++) {
                            legendLines.add(wrapped.get(i));
                            legendLineIcon.add(i == 0 ? entry[0] : null);
                        }
                    }

                    float legendBaselineY = page.getMediaBox().getLowerLeftY() + 10;
                    float firstLineY = legendBaselineY + (legendLines.size() - 1) * legendLeading;
                    contentStream.beginText();
                    contentStream.setFont(font, legendFontSize);
                    // Position at the lowest line; subsequent lines are rendered upward
                    contentStream.newLineAtOffset(startX + legendIndent, firstLineY);
                    for (int i = 0; i < legendLines.size(); i++) {
                        if (i > 0) {
                            contentStream.newLineAtOffset(0, -legendLeading);
                        }
                        contentStream.showText(legendLines.get(i));
                    }
                    contentStream.endText();

                    for (int i = 0; i < legendLines.size(); i++) {
                        String iconResource = legendLineIcon.get(i);
                        if (iconResource != null) {
                            float lineY = firstLineY - i * legendLeading;
                            PDImageXObject icon = loadedImagesCache.computeIfAbsent(iconResource, f -> loadImage(f, document));
                            contentStream.drawImage(icon, startX, lineY - 1f, GARDEN_ICON_SIZE, GARDEN_ICON_SIZE);
                        }
                    }
                }

                contentStream.beginText();
                contentStream.setFont(font, FONT_SIZE);
                contentStream.newLineAtOffset(startX, footerY);
                contentStream.showText(messagesApi.get(language, "pdf.timezone") + ": " + zonedDateTime.getZone().getId());

                String visibleThankUrl = "https://mooncal.ch/" + getThankUrl(language);
                String thankUrl = visibleThankUrl + "?c=pdf";
                float thankUrlWidth = PdfUtil.getStringWidth(visibleThankUrl, font, FONT_SIZE);
                float thankUrlX = page.getMediaBox().getUpperRightX() - startX - thankUrlWidth - THANK_QR_CODE_SIZE;
                contentStream.newLineAtOffset(thankUrlX - startX, 0); // "-startX" and "0" because it's relative to the previous newLineAtOffset(...)
                contentStream.showText(visibleThankUrl);
                contentStream.endText();
                page.getAnnotations().add(createLink(thankUrl, thankUrlX, footerY, thankUrlX + thankUrlWidth, footerY + FONT_SIZE, 1));

                float thankQrX = page.getMediaBox().getUpperRightX() - startX - THANK_QR_CODE_SIZE;
                float thankQrY = tableDrawer.getFinalY() - THANK_QR_CODE_SIZE;
                contentStream.drawImage(generateQRCodeImage(thankUrl, document), thankQrX, thankQrY, THANK_QR_CODE_SIZE, THANK_QR_CODE_SIZE);
                page.getAnnotations().add(createLink(thankUrl, thankQrX, thankQrY, thankQrX + THANK_QR_CODE_SIZE, thankQrY + THANK_QR_CODE_SIZE, -3));
            }

            var os = new ByteArrayOutputStream();
            document.save(os);
            return os.toByteArray();
        } catch (
                IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Order events shown within a single day cell. Garden events are ordered by the time they
     * appear on that day (a multi-day period that began earlier counts as starting at 00:00), so
     * overlapping garden events read top-to-bottom in chronological order. Non-garden events keep
     * their previous {@code eventTypeId}-then-time order; because every garden event id starts with
     * "garden-" they form one contiguous block, so reordering within it leaves non-garden events
     * (and the garden/non-garden boundary) untouched.
     */
    @VisibleForTesting
    static Comparator<EventInstance> dayEventOrder(LocalDate day) {
        return (a, b) -> {
            if (a.isGardenEvent() && b.isGardenEvent()) {
                int byTime = startTimeOnDay(a, day).compareTo(startTimeOnDay(b, day));
                return byTime != 0 ? byTime : a.getEventTypeId().compareTo(b.getEventTypeId());
            }
            int byType = a.getEventTypeId().compareTo(b.getEventTypeId());
            return byType != 0 ? byType : a.getDateTime().compareTo(b.getDateTime());
        };
    }

    private static LocalTime startTimeOnDay(EventInstance event, LocalDate day) {
        return event.getLocalDate().isBefore(day) ? LocalTime.MIN : event.getDateTime().toLocalTime();
    }

    private static PDAnnotationLink createLink(String url, float lowerLeftX, float lowerLeftY, float upperRightX, float upperRightY, int linkBorderWidth) {
        PDAnnotationLink txtLink = new PDAnnotationLink();
        PDActionURI action = new PDActionURI();
        action.setURI(url);
        txtLink.setAction(action);
        PDRectangle position = new PDRectangle();
        position.setLowerLeftX(lowerLeftX - linkBorderWidth);
        position.setLowerLeftY(lowerLeftY - linkBorderWidth);
        position.setUpperRightX(upperRightX + linkBorderWidth);
        position.setUpperRightY(upperRightY + linkBorderWidth);
        txtLink.setRectangle(position);
        return txtLink;
    }

    private PDFont loadFont(String langCode, PDDocument document, boolean bold) throws IOException {
        if (langCode.equals("hi")) {
            //not loaded in static memory because it's 1MB, and I'm not sure how often it will be used
            return PDType0Font.load(document, PDFMapper.class.getResourceAsStream("/Jaldi/" + (bold ? "Jaldi-Bold.ttf" : "Jaldi-Regular.ttf")));
        } else {
            return PDType0Font.load(document, new ByteArrayInputStream(bold ? quicksandBoldTtf : quicksandTtf));
        }
    }

    /** The icon resource shown for a garden event, or {@code null} for non-garden events (which keep text). */
    private static String gardenIconResource(String eventTypeId) {
        return switch (eventTypeId) {
            case "garden-biodynamic-root" -> "/public/emoji/root.png";
            case "garden-biodynamic-leaf" -> "/public/emoji/leaf.png";
            case "garden-biodynamic-flower" -> "/public/emoji/flower.png";
            case "garden-biodynamic-fruit" -> "/public/emoji/fruit.png";
            case "garden-biodynamic-badday" -> "/public/emoji/badday.png";
            default -> null;
        };
    }

    private Optional<String> getMoonIconFilename(List<EventInstance> eventInstances, Hemisphere hemisphere) {
        return eventInstances.stream()
                .map(eventInstance -> switch (eventInstance.getEventTypeId()) {
                    case MoonPhasesCalculation.FULLMOON_EVENT_TYPE_ID -> "/public/emoji/full.png";
                    case MoonPhasesCalculation.NEWMOON_EVENT_TYPE_ID -> "/public/emoji/new.png";
                    case MoonPhasesCalculation.FIRST_QUARTER_EVENT_TYPE_ID ->
                            hemisphere == Hemisphere.NORTHERN ? "/public/emoji/first-quarter.png" : "/public/emoji/last-quarter.png";
                    case MoonPhasesCalculation.LAST_QUARTER_EVENT_TYPE_ID ->
                            hemisphere == Hemisphere.NORTHERN ? "/public/emoji/last-quarter.png" : "/public/emoji/first-quarter.png";
                    default -> null;
                }).filter(Objects::nonNull)
                .findFirst();
    }

    private AbstractCell.AbstractCellBuilder<?, ?> draftDayOfMonthCell(int day, Optional<String> moonIconFilename, PDDocument document, Map<String, PDImageXObject> loadedImages) throws IOException {
        if (moonIconFilename.isPresent()) {
            return ImageCell.builder()
                    .image(loadedImages.computeIfAbsent(moonIconFilename.get(), filename -> loadImage(filename, document)))
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

    private PDImageXObject loadImage(String name, PDDocument document) {
        try {
            byte[] imageBytes = IOUtils.toByteArray(Objects.requireNonNull(getClass().getResourceAsStream(name)));
            return PDImageXObject.createFromByteArray(document, imageBytes, "moon");
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
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
