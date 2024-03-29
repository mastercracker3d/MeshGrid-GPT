/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package cl.mc3d.ai;

import java.io.DataInputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.List;
import javafx.collections.ObservableList;

/**
 *
 * @author maste
 */
public class ModelsInfo implements Runnable {

    private Thread thread;
    private ObservableList<FXChat.ModelDataRow> modelsData;

    public ModelsInfo(ObservableList<FXChat.ModelDataRow> modelsData) {
        this.modelsData = modelsData;
    }

    public void listModels() {
        try {
            int count = 1;
            int iPageCount = 1;
            for (int i = 0; i < iPageCount; i++) {
                URL url = new URL("https://huggingface.co/models?p=" + i + "&sort=downloads&search=7B+GGUF");

                URLConnection connection = url.openConnection();
                HttpURLConnection httUrlConnection = (HttpURLConnection) connection;
                DataInputStream dis = new DataInputStream(httUrlConnection.getInputStream());
                String sResult = null;
                boolean reading = false;
                boolean existsModel = false;
                String model = "";
                String lastUpdate = "";

                while ((sResult = dis.readLine()) != null) {
                    if (sResult.contains("<article")) {
                        reading = true;
                    } else {
                        if (sResult.contains("</html>")) {
                            reading = false;
                        }
                    }
                    if (reading) {
                        if ((sResult.contains("href=")) && (sResult.contains("GGUF"))) {
                            String title = sResult.substring(sResult.indexOf("href=") + 6, sResult.length());
                            if (title.startsWith("/")) {
                                if (!title.toLowerCase().contains("-instruct-")) {
                                    title = title.substring(0, title.indexOf("GGUF") + 4);
                                    model = title;
                                    existsModel = true;
                                    count++;
                                }
                            }
                        }
                        if ((existsModel) && (sResult.contains("<time")) && (sResult.contains("</time>"))) {
                            String time = sResult.substring(sResult.indexOf("<time") + 6, sResult.length());
                            time = time.substring(0, time.indexOf("</time>"));
                            time = time.substring(time.indexOf(">") + 1);
                            lastUpdate = time;
                            modelsData.add(new FXChat.ModelDataRow("" + (count - 1), model, lastUpdate));
                            existsModel = false;
                        }
                    }
                    if (i == 0) {
                        String searchPageCount = "href=\"?p=";
                        if ((sResult.contains(searchPageCount))) {
                            String pageCount = sResult.substring(sResult.indexOf(searchPageCount) + 9, sResult.length());
                            pageCount = pageCount.substring(0, pageCount.indexOf("&"));
                            int iCapturePageCount = Integer.parseInt(pageCount);
                            if (iCapturePageCount > iPageCount) {
                                iPageCount = iCapturePageCount;
                            }
                        }
                    }
                }
                dis.close();
                Thread.sleep(50);
            }
        } catch (Exception e) {

        }

    }

    public List<LLM> listModelType(String model) {
        List<LLM> lListModelType = new ArrayList<>();
        LLM initial = new LLM();
        initial.setName("");
        initial.setSize("");
        lListModelType.add(new LLM());
        try {
            URL url = new URL("https://huggingface.co" + model + "/tree/main");
            URLConnection connection = url.openConnection();
            HttpURLConnection httUrlConnection = (HttpURLConnection) connection;
            DataInputStream dis = new DataInputStream(httUrlConnection.getInputStream());
            String iResult = null;
            int count = 1;
            boolean existsModel = false;
            String modelType = "";

            while ((iResult = dis.readLine()) != null) {
                String search = "download href=\"" + model;
                if (iResult.contains(search)) {
                    String title = iResult.substring(iResult.indexOf(search) + search.length(), iResult.length());
                    if (title.startsWith("/") && (title.toLowerCase().contains(".gguf"))) {
                        String sSize = title.substring(title.lastIndexOf("\">") + 2, title.length());
                        title = title.substring(0, title.lastIndexOf("\">"));
                        modelType = title;
                        if (modelType.contains("/")) {
                            modelType = modelType.substring(modelType.lastIndexOf("/") + 1, modelType.length());
                            modelType = modelType.substring(0, modelType.lastIndexOf("?"));
                            LLM items = new LLM();
                            items.setName(modelType);
                            items.setSize(sSize);
                            lListModelType.add(items);
                            existsModel = true;
                        }
                    }
                }

            }
            dis.close();
        } catch (Exception e) {

        }
        return lListModelType;
    }

    public void start() {

        if (thread == null) {
            thread = new Thread(this);
        }
        thread.start();

    }

    @Override
    public void run() {
        listModels();
    }

}
