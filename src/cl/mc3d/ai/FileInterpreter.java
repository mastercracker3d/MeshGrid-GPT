/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package cl.mc3d.ai;

import java.io.File;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;

public class FileInterpreter {

    public String getPdf2Text(String fileName) {
        String text = "";
        try {
            if (fileName.startsWith("/")) {
                fileName = fileName.substring(fileName.indexOf("/") + 1, fileName.length());
            }
            File file = new File("localdocs/" + fileName);
            PDDocument document = Loader.loadPDF(file);
            PDFTextStripper stripper = new PDFTextStripper();
            text = stripper.getText(document);
            document.close();
        } catch (Exception e) {
            System.out.println(e.toString());
        }
        return text;
    }

}
