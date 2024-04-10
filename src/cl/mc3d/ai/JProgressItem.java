package cl.mc3d.ai;

import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.io.File;
import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JProgressBar;

public class JProgressItem extends JPanel implements Runnable {

    private Thread thread;
    private String file;
    private long finalSize;
    private JProgressBar progressBar;
    private boolean endFile = false;
    private JLabel titleLabel;

    public JProgressItem(String destinationFolder, String model, String modelType, String modelSize) {
        super();
        setLayout(new FlowLayout(FlowLayout.LEFT));
        JPanel groupComponent = new JPanel(new GridLayout(2, 1));
        this.finalSize = gigabytesToBytes(modelSize);

        if (model.contains("/")) {
            model = model.substring(1, model.lastIndexOf("/"));
        }
        model = model.toLowerCase();
        modelType = modelType.toLowerCase();
        if (modelType.contains(".gguf")) {
            modelType = modelType.substring(0, modelType.lastIndexOf(".gguf"));
            modelType = modelType + "-by_" + model + ".tmp";
        }
        titleLabel = new JLabel(modelType);
        setBorder(BorderFactory.createTitledBorder("Download"));

        progressBar = new JProgressBar();
        progressBar.setPreferredSize(new Dimension(150, 20));

        groupComponent.add(titleLabel);

        groupComponent.add(progressBar);
        add(groupComponent);
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
                titleLabel.setText(filePath);
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
                double currentSize = getFileSize(file);
                Double avance = currentSize / finalSize;

                if (currentSize == -2) {
                    avance = 1d;
                }
                System.out.println("Porcentaje de avance: filesize=" + finalSize + ", in progress=" + getFileSize(file) + ", avance:" + avance);
                String sAdvance = avance.toString();
                if (sAdvance.contains(".")) {
                    sAdvance = sAdvance.substring(0, sAdvance.lastIndexOf("."));
                }
                progressBar.setValue(Integer.parseInt(sAdvance) * 100);
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
