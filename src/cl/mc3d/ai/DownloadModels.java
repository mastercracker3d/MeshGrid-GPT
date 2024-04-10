/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package cl.mc3d.ai;

import java.io.File;
import java.io.FileOutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;

/**
 *
 * @author maste
 */
public class DownloadModels implements Runnable {

    private Thread thread;
    private String carpetaDestino, model, modelType;

    public DownloadModels(String carpetaDestino, String model, String modelType) {
        this.carpetaDestino = carpetaDestino;
        this.model = model;
        this.modelType = modelType;
    }

    public void start() {

        if (thread == null) {
            thread = new Thread(this);
        }
        thread.start();

    }

    @Override
    public void run() {
        try {
            URL url = new URL("https://huggingface.co" + model + "/resolve/main/" + modelType + "?download=true");
            System.out.println("Download model: " + url);
            URLConnection connection = url.openConnection();
            connection.setRequestProperty("Content-Type", "application/octet-stream");
            HttpURLConnection httpUrlConnection = (HttpURLConnection) connection;
            if (model.contains("/")) {
                model = model.substring(1, model.lastIndexOf("/"));
            }
            model = model.toLowerCase();
            modelType = modelType.toLowerCase();
            if (modelType.contains(".gguf")) {
                modelType = modelType.substring(0, modelType.lastIndexOf(".gguf"));
                modelType = modelType + "-by_" + model + ".tmp";
            }
            System.out.println("Download model: " + model);
            File fModelType = new File(carpetaDestino + "/" + modelType);
            if (!fModelType.exists()) {
                ReadableByteChannel readableByteChannel = Channels.newChannel(httpUrlConnection.getInputStream());
                FileOutputStream fileOutputStream = new FileOutputStream(fModelType);
                fileOutputStream.getChannel().transferFrom(readableByteChannel, 0, Long.MAX_VALUE);
                fileOutputStream.close();
                readableByteChannel.close();
                System.out.println("Descarga de model finalizada:" + modelType);
                if (modelType.contains(".tmp")) {
                    modelType = modelType.substring(0, modelType.lastIndexOf(".tmp"));
                    modelType = modelType + ".gguf";
                    fModelType.renameTo(new File(carpetaDestino + "/" + modelType));
                }
            }

        } catch (Exception e) {
            System.out.println(e.toString());
        }

    }


}
