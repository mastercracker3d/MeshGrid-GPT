/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package cl.mc3d.ai;

import static cl.mc3d.ai.Security.getPublicKeyFromKeystore;
import cl.mc3d.gpt4all.LLModel;
import java.io.File;
import java.io.RandomAccessFile;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Date;
import java.util.Iterator;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.ignite.IgniteCache;
import org.apache.ignite.lang.IgniteBiTuple;

/**
 *
 * @author maste
 */
public class UnitaryProcessSuscribeFile implements Runnable {

    private Thread thread = null;
    private IgniteCache igniteCache = null;
    private String cacheName = null;
    private LLModel model;
    private LLModel.GenerationConfig config;
    private String modelPromptTemplate;
    private String hostname = "";
    private String cipherMessage = "";
    private String keyStorePass = "";
    private String locationStart;

    public UnitaryProcessSuscribeFile(String locationStart, IgniteCache igniteCache, String cacheName, LLModel model, LLModel.GenerationConfig config, String modelPromptTemplate, String cipherMessage, String keyStorePass) {
        this.locationStart = locationStart;
        this.igniteCache = igniteCache;
        this.cacheName = cacheName;
        this.model = model;
        this.cipherMessage = cipherMessage;
        this.keyStorePass = keyStorePass;
        this.config = config;
        this.modelPromptTemplate = modelPromptTemplate;
        try {
            InetAddress addr;
            addr = InetAddress.getLocalHost();
            hostname = addr.getHostName();
        } catch (UnknownHostException ex) {
            System.out.println("Hostname can not be resolved");
        }

    }

    public void createFolderIfNotExists(String folder) {
        File fQuestionsFolder = new File(folder);
        if (!fQuestionsFolder.exists()) {
            fQuestionsFolder.mkdirs();
        }
    }

    public synchronized String response(String sPreProcessedFolder, String questionsProcessedFolder, String responseFolder) {
        String data = "";
        Iterator iCache = igniteCache.iterator();
        while (iCache.hasNext()) {
            try {
                IgniteBiTuple igniteBiTuple = (IgniteBiTuple) iCache.next();
                String uuid = igniteBiTuple.get1().toString();
                if (igniteCache.get(uuid + "-lock") != null) {
                    long lCurrent = System.currentTimeMillis();
                    long lLock = (Long) igniteCache.get(uuid + "-lock");
                    long leasing = 300000;
                    if ((lCurrent - lLock) >= leasing) {
                        String sStep = uuid + "-lock, timeout 300000";
                        System.out.println(sStep);
                        igniteCache.remove(uuid + "-lock");
                    }
                }
                if (uuid.endsWith(")") && (igniteCache.get(uuid + "-lock") == null)) {
                    igniteCache.put(uuid + "-lock", +System.currentTimeMillis());
                    Properties pData = (Properties) igniteBiTuple.get2();
                    String sQuestion = pData.getProperty(uuid);
                    if (cipherMessage.equals("true")) {
                        try {
                            String publicKey = getPublicKeyFromKeystore(cacheName + ".pfx", keyStorePass, cacheName, "PKCS12");
                            String sKey = publicKey;
                            if (sKey.lastIndexOf("==") != -1) {
                                sKey = sKey.substring(0, sKey.lastIndexOf("=="));
                                sKey = sKey.substring(sKey.length() - 32, sKey.length());
                                sQuestion = Security.decryptText(sKey, sQuestion);
                            }
                        } catch (Exception ex) {

                        }
                    }
                    String sStep = new Date() + ", Take event: " + uuid + ", data:" + sQuestion;
                    System.out.println(sStep);
                    if (sQuestion.length() > 3) {
                        try {
                            System.out.println("GPU available: " + model.llmodel_has_gpu_device());
                            System.out.println("Model Loaded: " + model.llmodel_isModelLoaded());
                            System.out.println("GPU Force: " + model.llmodel_gpu_init_gpu_device_by_int(2));
                            String fullGeneration = model.generate(sQuestion, config, modelPromptTemplate, false, true, null);
                            if (cipherMessage.equals("true")) {
                                try {
                                    String publicKey = getPublicKeyFromKeystore(cacheName + ".pfx", keyStorePass, cacheName, "PKCS12");
                                    String sKey = publicKey;
                                    if (sKey.lastIndexOf("==") != -1) {
                                        sKey = sKey.substring(0, sKey.lastIndexOf("=="));
                                        sKey = sKey.substring(sKey.length() - 32, sKey.length());
                                        fullGeneration = Security.encryptText(sKey, fullGeneration);
                                    }
                                } catch (Exception ex) {

                                }
                            }
                            igniteCache.put(uuid + "-rsp", fullGeneration);
                            sStep = new Date() + ", Response to event: " + uuid + "-rsp" + ", fullGeneration: " + fullGeneration;
                            System.out.println(sStep);

                        } catch (Exception ex) {
                            sStep = "Error in process lLM: " + ex.toString();
                            Logger.getLogger(UnitaryProcessSuscribeFile.class.getName()).log(Level.SEVERE, sStep);

                        }
                    } else {
                        sStep = "\rEvent empty detected uuid: " + uuid;
                        System.out.println(sStep);
                    }
                    igniteCache.remove(uuid);
                    igniteCache.remove(uuid + "-lock");
                }
                if (uuid.endsWith("-rsp")) {
                    if (uuid.contains("-" + hostname + "_" + locationStart.hashCode() + "-(")) {
                        String sStep = "\r\nLocal file detected, download: " + uuid;
                        System.out.println(sStep);
                        RandomAccessFile fResult = new RandomAccessFile(responseFolder + "/" + uuid + ".txt", "rw");
                        String sResponse = "" + igniteCache.get(uuid);
                        if (cipherMessage.equals("true")) {

                            try {
                                String publicKey = getPublicKeyFromKeystore(cacheName + ".pfx", keyStorePass, cacheName, "PKCS12");
                                String sKey = publicKey;
                                if (sKey.lastIndexOf("==") != -1) {
                                    sKey = sKey.substring(0, sKey.lastIndexOf("=="));
                                    sKey = sKey.substring(sKey.length() - 32, sKey.length());
                                    sResponse = Security.decryptText(sKey, sResponse);
                                }
                            } catch (Exception ex) {

                            }
                        }
                        fResult.writeBytes("" + sResponse);
                        fResult.close();
                        File fFile = new File(sPreProcessedFolder + "/" + uuid + ".txt");
                        fFile.renameTo(new File(questionsProcessedFolder + "/" + uuid + ".txt"));
                        igniteCache.remove(uuid);
                    }
                }

            } catch (Exception e) {
                String sStep = "\nResponse error: " + e.toString();
                System.out.println(sStep);

            }
        }

        return data;
    }

    public void start() {
        if (thread == null) {
            thread = new Thread(this);
        }
        thread.start();
    }

    @Override
    public void run() {
        String sPreProcessedFolder = "questions_pre-processed";
        String sQuestionsProcessedFolder = "questions_processed";
        String sResponsesFolder = "responses";
        while (true) {
            try {
                createFolderIfNotExists(sQuestionsProcessedFolder);
                createFolderIfNotExists(sResponsesFolder);
                response(sPreProcessedFolder, sQuestionsProcessedFolder, sResponsesFolder);
                Thread.sleep(50);
            } catch (Exception e) {

            }
        }
    }

}
