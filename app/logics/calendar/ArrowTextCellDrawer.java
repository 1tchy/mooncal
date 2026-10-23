package logics.calendar;

import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.vandeseer.easytable.drawing.DrawingContext;
import org.vandeseer.easytable.drawing.DrawingUtil;
import org.vandeseer.easytable.drawing.PositionedStyledText;
import org.vandeseer.easytable.drawing.cell.TextCellDrawer;
import org.vandeseer.easytable.structure.cell.TextCell;
import org.vandeseer.easytable.util.PdfUtil;

import java.io.IOException;

import static logics.calculation.MoonPhasesCalculation.RISE_SYMBOL;
import static logics.calculation.MoonPhasesCalculation.SET_SYMBOL;
import static logics.calculation.MoonPhasesCalculation.SUN_SYMBOL;

/**
 * Draws the rise and set arrows and the sun symbol as vector paths, because the PDF fonts have no such glyphs. Each
 * symbol takes the width easytable measured for the missing glyph, so line breaking and drawing agree.
 */
class ArrowTextCellDrawer extends TextCellDrawer<TextCell> {

    @Override
    protected void drawText(DrawingContext drawingContext, PositionedStyledText text) throws IOException {
        String line = text.getText();
        float x = text.getX();
        int segmentStart = 0;
        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);
            if (c == RISE_SYMBOL || c == SET_SYMBOL || c == SUN_SYMBOL) {
                x = drawSegment(drawingContext, text, line.substring(segmentStart, i), x);
                float width = PdfUtil.getStringWidth(String.valueOf(c), text.getFont(), text.getFontSize());
                if (c == SUN_SYMBOL) {
                    drawSun(drawingContext.getContentStream(), text, x, width);
                } else {
                    drawArrow(drawingContext.getContentStream(), text, x, width, c == RISE_SYMBOL);
                }
                x += width;
                segmentStart = i + 1;
            }
        }
        drawSegment(drawingContext, text, line.substring(segmentStart), x);
    }

    private static float drawSegment(DrawingContext drawingContext, PositionedStyledText text, String segment, float x) throws IOException {
        if (segment.isEmpty()) {
            return x;
        }
        DrawingUtil.drawText(drawingContext.getContentStream(), text.toBuilder().x(x).text(segment).build());
        return x + PdfUtil.getStringWidth(segment, text.getFont(), text.getFontSize());
    }

    private static void drawArrow(PDPageContentStream contentStream, PositionedStyledText text, float x, float width, boolean up) throws IOException {
        float height = PdfUtil.getFontHeight(text.getFont(), text.getFontSize());
        float centerX = x + width / 2;
        float tipY = up ? text.getY() + height : text.getY();
        float tailY = up ? text.getY() : text.getY() + height;
        float headLength = height * 0.4f * (up ? -1 : 1);
        float headHalfWidth = Math.min(width * 0.35f, height * 0.3f);

        contentStream.saveGraphicsState();
        contentStream.setStrokingColor(text.getColor());
        contentStream.setLineWidth(text.getFontSize() * 0.08f);
        contentStream.setLineCapStyle(1);
        contentStream.setLineJoinStyle(1);
        contentStream.moveTo(centerX, tailY);
        contentStream.lineTo(centerX, tipY);
        contentStream.moveTo(centerX - headHalfWidth, tipY + headLength);
        contentStream.lineTo(centerX, tipY);
        contentStream.lineTo(centerX + headHalfWidth, tipY + headLength);
        contentStream.stroke();
        contentStream.restoreGraphicsState();
    }

    private static void drawSun(PDPageContentStream contentStream, PositionedStyledText text, float x, float width) throws IOException {
        float height = PdfUtil.getFontHeight(text.getFont(), text.getFontSize());
        float centerX = x + width / 2;
        float centerY = text.getY() + height / 2;
        float outerRadius = Math.min(width, height) / 2;
        float rayStart = outerRadius * 0.65f;
        float discRadius = outerRadius * 0.4f;

        contentStream.saveGraphicsState();
        contentStream.setStrokingColor(text.getColor());
        contentStream.setNonStrokingColor(text.getColor());
        contentStream.setLineWidth(text.getFontSize() * 0.06f);
        contentStream.setLineCapStyle(1);
        for (int ray = 0; ray < 8; ray++) {
            double angle = ray * Math.PI / 4;
            float cos = (float) Math.cos(angle);
            float sin = (float) Math.sin(angle);
            contentStream.moveTo(centerX + rayStart * cos, centerY + rayStart * sin);
            contentStream.lineTo(centerX + outerRadius * cos, centerY + outerRadius * sin);
        }
        contentStream.stroke();
        // a circle from four Bézier curves
        float k = discRadius * 0.5523f;
        contentStream.moveTo(centerX + discRadius, centerY);
        contentStream.curveTo(centerX + discRadius, centerY + k, centerX + k, centerY + discRadius, centerX, centerY + discRadius);
        contentStream.curveTo(centerX - k, centerY + discRadius, centerX - discRadius, centerY + k, centerX - discRadius, centerY);
        contentStream.curveTo(centerX - discRadius, centerY - k, centerX - k, centerY - discRadius, centerX, centerY - discRadius);
        contentStream.curveTo(centerX + k, centerY - discRadius, centerX + discRadius, centerY - k, centerX + discRadius, centerY);
        contentStream.fill();
        contentStream.restoreGraphicsState();
    }
}
