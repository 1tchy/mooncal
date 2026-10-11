package logics.calendar;

import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.vandeseer.easytable.drawing.DrawingContext;
import org.vandeseer.easytable.drawing.DrawingUtil;
import org.vandeseer.easytable.drawing.PositionedStyledText;
import org.vandeseer.easytable.drawing.cell.TextCellDrawer;
import org.vandeseer.easytable.structure.cell.TextCell;
import org.vandeseer.easytable.util.PdfUtil;

import java.io.IOException;

import static logics.calculation.MoonPhasesCalculation.MOONRISE_SYMBOL;
import static logics.calculation.MoonPhasesCalculation.MOONSET_SYMBOL;

/**
 * Draws the moonrise and moonset arrows as vector paths, because the PDF fonts have no arrow glyphs. Each arrow
 * takes the width easytable measured for the missing glyph, so line breaking and drawing agree.
 */
class ArrowTextCellDrawer extends TextCellDrawer<TextCell> {

    @Override
    protected void drawText(DrawingContext drawingContext, PositionedStyledText text) throws IOException {
        String line = text.getText();
        float x = text.getX();
        int segmentStart = 0;
        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);
            if (c == MOONRISE_SYMBOL || c == MOONSET_SYMBOL) {
                x = drawSegment(drawingContext, text, line.substring(segmentStart, i), x);
                float width = PdfUtil.getStringWidth(String.valueOf(c), text.getFont(), text.getFontSize());
                drawArrow(drawingContext.getContentStream(), text, x, width, c == MOONRISE_SYMBOL);
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
}
