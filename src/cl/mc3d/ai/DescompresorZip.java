/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package cl.mc3d.ai;

import java.io.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class DescompresorZip {

    public static String descomprimirArchivoZip(String archivoZip, String carpetaDestino) throws IOException {
        String finalFolder="";
        File destino = new File(carpetaDestino);
        byte[] buffer = new byte[1024];

        // Crea el directorio de destino si no existe
        if (!destino.exists()) {
            destino.mkdir();
        }

        try (ZipInputStream zis = new ZipInputStream(new FileInputStream(archivoZip))) {
            ZipEntry entradaZip = zis.getNextEntry();

            // Itera sobre las entradas del archivo ZIP
            while (entradaZip != null) {
                String nombreEntrada = entradaZip.getName();
                String archivoDescomprimido = carpetaDestino + "/" + nombreEntrada;
                if (archivoDescomprimido.contains("/")) {
                    String lastDirectory = archivoDescomprimido.substring(0, archivoDescomprimido.lastIndexOf("/"));
                    if (!new File(lastDirectory).exists()) {
                        new File(lastDirectory).mkdirs();
                         finalFolder=lastDirectory;
                    }
                }

                // Crea el archivo de salida y escribe los datos descomprimidos
                try (FileOutputStream fos = new FileOutputStream(archivoDescomprimido)) {
                    int longitud;
                    while ((longitud = zis.read(buffer)) > 0) {
                        fos.write(buffer, 0, longitud);
                    }

                }

                // Avanza a la siguiente entrada
                entradaZip = zis.getNextEntry();
            }
        }
        System.out.println("Descompresión completada en: " + finalFolder);
        return finalFolder;
    }

}
