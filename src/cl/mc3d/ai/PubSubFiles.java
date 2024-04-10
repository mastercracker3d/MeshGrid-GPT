package cl.mc3d.ai;

import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author mastercracker3d
 */
public class PubSubFiles {

    /*public LinkedHashMap startMC3DAI(String location, String model, double temperature) {
        LinkedHashMap<String, Object> resultMap = new LinkedHashMap();
        long start = System.currentTimeMillis();

        boolean isWindows = System.getProperty("os.name").toLowerCase().startsWith("windows");
        try {
            if (isWindows) {
                {
                    CommandLine cmdLine = new CommandLine("cmd.exe");
                    cmdLine.addArgument("/c " + location + "/chat.exe -m " + location + "/" + model + " --temp " + temperature + "\n");
                    ByteArrayOutputStream stdout = new ByteArrayOutputStream();
                    ByteArrayOutputStream stderr = new ByteArrayOutputStream();
                    PumpStreamHandler pumpStreamHandler = new PumpStreamHandler(stdout, stderr);
                    ExecuteWatchdog watchdog = new ExecuteWatchdog(30000);  // 30s timeout
                    DefaultExecutor executor = new DefaultExecutor();
                    executor.execute(cmdLine);
                    executor.setStreamHandler(pumpStreamHandler);
                    executor.setWatchdog(watchdog);
                }
            } else {
                String[] commandArray = new String[]{"bash", "-c", location + "/chat -m " + location + "/" + model + " --temp " + temperature + " \n"};
                String command = "";
                for (String prompt : commandArray) {
                    command += " " + prompt;
                }
                Process process = Runtime.getRuntime().exec(commandArray);
                BufferedReader brInput = new BufferedReader(new InputStreamReader(process.getErrorStream()));
                String line = "";
                String data = "";
                boolean enable = true;
                while ((line = brInput.readLine()) != null) {
                    System.out.println(line);
                    if (enable) {
                        data += line;
                    }
                    if (line.contains("sampling parameters:")) {
                        enable = true;
                    }
                    if (line.contains("[end of text]")) {
                        data = data.replaceAll("\\[end of text\\]", "");
                        break;
                    }
                }
                brInput.close();
            }
        } catch (Exception ex) {
            Logger.getLogger(PubSubFiles.class
                    .getName()).log(Level.SEVERE, null, ex);
        }
        long end = System.currentTimeMillis();
        resultMap.put("total", (end - start));
        return resultMap;
    }*/
    public LinkedHashMap putQuestion(String location, String uuid, String question) {

        LinkedHashMap<String, Object> resultMap = new LinkedHashMap();
        try {
            location = location + "questions";
            File fDirectory = new File(location);
            if (!fDirectory.exists()) {
                fDirectory.mkdirs();
            }
            String fileName = location + "/" + uuid + ".txt";
            RandomAccessFile raf = new RandomAccessFile(fileName, "rw");
            String searchLocalDocs = "localDoc=\"";
            if (question.contains(searchLocalDocs)) {
                String line = question;
                while (line.contains(searchLocalDocs)) {
                    String localDoc = line.substring(line.indexOf(searchLocalDocs) + searchLocalDocs.length(), line.length());
                    localDoc = localDoc.substring(0, localDoc.indexOf("\""));
                    String searchText = "localDoc=\"" + localDoc + "\"";
                    String startInQuestion = question.substring(0, question.indexOf(searchText));
                    String endInQuestion = question.substring(question.indexOf(searchText) + searchText.length(), question.length());
                    String localDocText = "";
                    if (localDoc.toLowerCase().endsWith(".txt")) {
                        try {
                            byte[] bytes = Files.readAllBytes(Paths.get("localdocs" + localDoc));
                            localDocText = new String(bytes, StandardCharsets.UTF_8);
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    } else {
                        if (localDoc.toLowerCase().endsWith(".pdf")) {
                            FileInterpreter f2t = new FileInterpreter();
                            localDocText = f2t.getPdf2Text(localDoc);
                        }
                    }
                    question = startInQuestion + localDocText + endInQuestion;
                    line = line.substring(line.indexOf(localDoc) + localDoc.length(), line.length());
                }
            }
            raf.writeBytes(question);

            raf.close();
            resultMap.put("uuid", uuid + ".txt");
        } catch (Exception ex) {
            Logger.getLogger(PubSubFiles.class.getName()).log(Level.SEVERE, null, ex);
        }
        return resultMap;
    }

    public synchronized List getResponseId(String location) {
        List<String> resultMap = new ArrayList();
        try {
            String sDirectory = location + "responses/";
            File fDirectory = new File(sDirectory);
            if (!fDirectory.exists()) {
                fDirectory.mkdirs();
            }
            String[] fileList = fDirectory.list();
            for (String sFile : fileList) {
                if (sFile.endsWith(".txt")) {
                    resultMap.add(sFile);
                }
            }
        } catch (Exception ex) {
            Logger.getLogger(PubSubFiles.class.getName()).log(Level.SEVERE, null, ex);
        }
        return resultMap;
    }

    public synchronized LinkedHashMap getResponse(String location, String name) {
        String data = "";
        LinkedHashMap<String, Object> resultMap = new LinkedHashMap();
        try {
            String sResponse = location + "responses/" + name;
            File fResponse = new File(sResponse);
            if (fResponse.exists()) {
                InputStream is = new FileInputStream(fResponse);
                DataInputStream dis = new DataInputStream(is);
                String line = "";
                while ((line = dis.readLine()) != null) {
                    System.out.println(line);
                    data += "\r\n" + line;
                }
                dis.close();
                is.close();
            }
            resultMap.put("data", data);
        } catch (Exception ex) {
            Logger.getLogger(PubSubFiles.class.getName()).log(Level.SEVERE, null, ex);
        }
        return resultMap;
    }

    public synchronized boolean deleteResponse(String location, String name) {
        boolean status = false;
        try {
            String sResponse = location + "responses/" + name;
            File fResponse = new File(sResponse);
            if (fResponse.exists()) {
                fResponse.delete();
                status = true;
            }
            String sQuestionProcessed = location + "questions_processed/" + name;
            System.out.println("sQuestionProcessed: " + sQuestionProcessed);
            File fQuestionProcessed = new File(sQuestionProcessed);
            if (fQuestionProcessed.exists()) {
                fQuestionProcessed.delete();
            }
        } catch (Exception ex) {
            Logger.getLogger(PubSubFiles.class.getName()).log(Level.SEVERE, null, ex);
        }
        return status;
    }

    /* public static void main(String[] args) {
        String sWindowsDir = "D:/MC3D_AI";
        String sLinuxDir = "/MC3D-IA";
        String sModel = "ggml-alpaca-7b-q4.bin";
        PubSubFiles gpt4all = new PubSubFiles();
        String query = "suma 5+2";
        long uuid = System.currentTimeMillis();
        //LinkedHashMap result = gpt4all.putQuestion(sWindowsDir, ""+uuid, query);
        //System.out.println("Tiempo total: " + result.get("total") + " ms texto:" + result.get("data"));
        List<String> id = gpt4all.getResponseId(sWindowsDir);
        for (String name : id) {
            gpt4all.getResponse(sWindowsDir, name);
        }

    }*/
}
