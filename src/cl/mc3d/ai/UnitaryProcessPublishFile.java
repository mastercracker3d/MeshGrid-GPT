/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package cl.mc3d.ai;

import static cl.mc3d.ai.Security.getPublicKeyFromKeystore;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;
import javafx.scene.control.TextArea;
import org.apache.ignite.IgniteCache;

/**
 *
 * @author maste
 */
public class UnitaryProcessPublishFile implements Runnable {

    private Thread thread = null;
    private IgniteCache igniteCache = null;
    private String cacheName = null;
    private String hostname = null;
    private String modelFilePath = null;
    private String cipherMessage = null;
    private String keyStorePass = null;
    private String locationStart;
    private TextArea logTextArea;

    public UnitaryProcessPublishFile() {
    }

    public UnitaryProcessPublishFile(String locationStart, TextArea logTextArea, IgniteCache igniteCache, String cacheName, String hostname, String modelFilePath, String cipherMessage, String keyStorePass) {
        this.locationStart = locationStart;
        this.logTextArea = logTextArea;
        this.igniteCache = igniteCache;
        this.cacheName = cacheName;
        this.hostname = hostname;
        this.modelFilePath = modelFilePath;
        this.cipherMessage = cipherMessage;
        this.keyStorePass = keyStorePass;
    }

    public synchronized String getFirstFileName(String folder) {
        String data = "";
        createFolderIfNotExists(folder);
        File fQuestionsFolder = new File(folder);
        String[] lQuestionsFolder = fQuestionsFolder.list();
        for (String sFile : lQuestionsFolder) {
            if (sFile.endsWith(".txt")) {
                data = sFile;
                break;
            }
        }
        return data;

    }

    public List getPDFList(String folder) {
        List<String> data = new ArrayList<>();
        createFolderIfNotExists(folder);
        File fQuestionsFolder = new File(folder);
        String[] lQuestionsFolder = fQuestionsFolder.list();
        for (String sFile : lQuestionsFolder) {
            if (sFile.endsWith(".pdf")) {
                data.add(sFile);
            }
        }
        return data;

    }

    public String readFile(String questionFolder, String file) {
        String data = "";
        if (file.length() > 3) {
            try {
                File fFile = new File(questionFolder + "/" + file);
                if (fFile.exists()) {
                    FileInputStream fis = new FileInputStream(fFile);
                    String text = null;
                    DataInputStream dis = new DataInputStream(fis);
                    while ((text = dis.readLine()) != null) {
                        data += text;
                    }
                    fis.close();
                }

            } catch (Exception ex) {
                String sStep = "\nreadFile error: " + ex.toString();
                logTextArea.appendText(sStep + "\n");
                Logger.getLogger(UnitaryProcessPublishFile.class.getName()).log(Level.SEVERE, null, sStep);
            }
        }
        return data;
    }

    public void createFolderIfNotExists(String folder) {
        File fQuestionsFolder = new File(folder);
        if (!fQuestionsFolder.exists()) {
            fQuestionsFolder.mkdirs();
        }
    }

    public synchronized boolean preProcessFile(String uuid, String questionsFolder, String questionFile, String questionsPreProcessFolder) {
        boolean status = false;
        try {
            createFolderIfNotExists(questionsPreProcessFolder);
            File fFile = new File(questionsFolder + "/" + questionFile);
            fFile.renameTo(new File(questionsPreProcessFolder + "/" + uuid + ".txt"));
            status = true;
        } catch (Exception e) {
            String sStep = "preProcessFile error: " + e.toString();
            logTextArea.appendText(sStep + "\n");
            Logger.getLogger(UnitaryProcessPublishFile.class.getName()).log(Level.SEVERE, null, sStep);
        }
        return status;
    }

    public void start() {
        if (thread == null) {
            thread = new Thread(this);
        }
        thread.start();
    }

    @Override
    public void run() {

        String sQuestionsFolder = "questions";
        String sPreProcessedFolder = "questions_pre-processed";
        String sFile = getFirstFileName(sQuestionsFolder);
        String sQuestion = readFile(sQuestionsFolder, sFile);
        if (sQuestion.length() > 3) {
            long inicio = System.currentTimeMillis();
            String modelName = modelFilePath;
            if (modelName.contains("/")) {
                modelName = modelName.substring(modelName.lastIndexOf("/") + 1, modelName.length());
                modelName = modelName.substring(0, modelName.lastIndexOf("."));
            } else {
                if (modelName.contains("\\")) {
                    modelName = modelName.substring(modelName.lastIndexOf("\\") + 1, modelName.length());
                    modelName = modelName.substring(0, modelName.lastIndexOf("."));
                }
            }
            String uuid = inicio + "-" + modelName + "-" + hostname + "_" + locationStart.hashCode() + "-(" + sFile + ")";
            preProcessFile(uuid, sQuestionsFolder, sFile, sPreProcessedFolder);
            String sStep = "\r\nReady for publish in Grid: Internal id: " + inicio + "-" + modelName + "-" + hostname + "\r\n" + "External id: " + sFile + "\r\n" + "uuid :" + uuid + "\r\n" + "Question:\r\n" + sQuestion;
            logTextArea.appendText(sStep + "\n");
            System.out.println(sStep);

            Properties pMessage = new Properties();
            pMessage.setProperty(uuid, sQuestion);
            if (cipherMessage.equals("true")) {

                try {
                    String publicKey = getPublicKeyFromKeystore(cacheName + ".pfx", keyStorePass, cacheName, "PKCS12");
                    String sKey = publicKey;
                    if (sKey.lastIndexOf("==") != -1) {
                        sKey = sKey.substring(0, sKey.lastIndexOf("=="));
                        sKey = sKey.substring(sKey.length() - 32, sKey.length());
                        pMessage.setProperty(uuid, Security.encryptText(sKey, sQuestion));
                    }
                } catch (Exception ex) {

                }
            }
            igniteCache.put(uuid, pMessage);
            long fin = System.currentTimeMillis();
            sStep = "\r\nPublished in grid: " + uuid + ",Total time:" + (fin - inicio) + " ms";
            logTextArea.appendText(sStep + "\n");
            System.out.println(sStep);
        }

    }

}
