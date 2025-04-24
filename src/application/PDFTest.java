package application;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDType1Font;

import java.io.IOException;

public class PDFTest {
    public static void main(String[] args) {
        try (PDDocument document = new PDDocument()) {
            PDPage page = new PDPage();
            document.addPage(page);

            PDPageContentStream contentStream = new PDPageContentStream(document, page);
            contentStream.beginText();
            contentStream.setFont(PDType1Font.HELVETICA_BOLD, 18);
            contentStream.newLineAtOffset(100, 700);
            contentStream.showText("Hello from BBoost!");
            contentStream.endText();
            contentStream.close();

            document.save("hello-bboost.pdf");
            System.out.println("✅ PDF created: hello-bboost.pdf");

        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
