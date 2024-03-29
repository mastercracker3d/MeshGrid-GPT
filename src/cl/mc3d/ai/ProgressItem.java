package cl.mc3d.ai;

import java.io.File;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.layout.VBox;

public class ProgressItem extends VBox implements Runnable {

    private Thread thread;
    private String file;
    private long finalSize;
    private ProgressBar progressBar;
    private boolean endFile = false;

    public ProgressItem(String destinationFolder, String model, String modelType, String modelSize) {
        this.finalSize = gigabytesToBytes(modelSize);
        setPadding(new Insets(5));
        setSpacing(5);
        if (model.contains("/")) {
            model = model.substring(1, model.lastIndexOf("/"));
        }
        model = model.toLowerCase();
        modelType = modelType.toLowerCase();
        if (modelType.contains(".gguf")) {
            modelType = modelType.substring(0, modelType.lastIndexOf(".gguf"));
            modelType = modelType + "-by_" + model + ".tmp";
        }
        // Crear Label para el título
        Label titleLabel = new Label(modelType);

        // Crear ProgressBar
        progressBar = new ProgressBar();
        progressBar.setPrefWidth(150);

        // Agregar ProgressBar y Label al VBox
        getChildren().addAll(titleLabel, progressBar);
        if (destinationFolder.contains("\\")) {
            destinationFolder = destinationFolder.replaceAll("\\\\", "/");
        }
        file = destinationFolder + "/" + modelType;
        start();
    }

    public long getFileSize(String filePath) {
        long iSize = 0;
        System.out.println("File monitoring: " + filePath);

        File file = new File(filePath);
        if (file.exists() && file.isFile()) {
            iSize = file.length();
        } else {
            System.err.println("El archivo no existe o no es un archivo válido.");
            filePath = filePath.toLowerCase().replaceAll(".tmp", ".gguf");
            File finalFile = new File(filePath);
            if (finalFile.exists()) {
                iSize = -2;
                endFile = true;
            }
        }
        return iSize;
    }

    public void start() {

        if (thread == null) {
            thread = new Thread(this);
        }
        thread.start();

    }

    @Override
    public void run() {
        while (true) {
            try {
                Platform.runLater(new Runnable() {
                    @Override
                    public void run() {
                        double currentSize = getFileSize(file);
                        Double avance = new Double(currentSize / finalSize);

                        if (currentSize == -2) {
                            avance = 1d;
                        }
                        System.out.println("Porcentaje de avance: filesize=" + finalSize + ", in progress=" + getFileSize(file) + ", avance:" + avance);
                        progressBar.setProgress(avance);

                    }
                });
                Thread.sleep(1000);
            } catch (Exception e) {
                System.out.println(e.toString());
            }
            if (endFile) {
                break;
            }
        }
    }

    public static long gigabytesToBytes(String modelSize) {
        // 1 GB = 1024^3 bytes
        double size = 0d;
        if (modelSize.toLowerCase().contains("gb")) {
            size = Double.parseDouble(modelSize.substring(0, modelSize.indexOf(" ")));
        }
        return (long) (size * Math.pow(1024, 3));
    }
}
