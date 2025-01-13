package de.werwolf2303.xpairdebug.services;

import javax.swing.*;
import javax.swing.border.Border;
import java.awt.*;
import java.awt.event.AdjustmentEvent;
import java.awt.event.AdjustmentListener;
import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class LaunchService {
    Process progProcess;
    String progPath;
    ProcessListener listener;

    public interface ProcessListener {
        void onProcessStopped();
    }

    public void launch(File cwd, String filePath, String jvmArgs, String progArgs) throws IllegalStateException {
        new Thread(() -> {
            JFrame frame = new JFrame("Console for: " + new File(filePath).getName());
            frame.setBackground(Color.black);
            JScrollPane scrollPane = new JScrollPane();
            JTextArea area = new JTextArea();
            area.setEditable(false);
            area.setBackground(Color.black);
            area.setForeground(Color.white);
            frame.setResizable(true);
            frame.setMinimumSize(new Dimension(400, 300));
            scrollPane.setViewportView(area);
            scrollPane.getVerticalScrollBar().addAdjustmentListener(new AdjustmentListener() {
                public void adjustmentValueChanged(AdjustmentEvent e) {
                    e.getAdjustable().setValue(e.getAdjustable().getMaximum());
                }
            });
            frame.add(scrollPane, BorderLayout.CENTER);
            frame.setVisible(true);
            frame.pack();
            progPath = filePath;
            if (progProcess != null && progProcess.isAlive()) {
                stop();
            }
            try {
                List<String> command = new ArrayList<>();
                command.add("java");
                if (jvmArgs != null && !jvmArgs.isEmpty()) {
                    command.addAll(Arrays.asList(jvmArgs.split(" ")));
                }
                command.add("-jar");
                command.add(filePath);
                if (progArgs != null && !progArgs.isEmpty()) {
                    command.addAll(Arrays.asList(progArgs.split(" ")));
                    scrollPane.getVerticalScrollBar().setValue(scrollPane.getVerticalScrollBar().getMaximum());
                }
                System.out.println(command);
                progProcess = new ProcessBuilder(command)
                        .directory(cwd)
                        .start();
                BufferedReader reader = new BufferedReader(new InputStreamReader(progProcess.getInputStream()));
                String line;
                while ((line = reader.readLine()) != null) {
                    area.append(line + "\n");
                }
                progProcess.waitFor();
                frame.setVisible(false);
                listener.onProcessStopped();
            } catch (Exception e) {
                throw new IllegalStateException("Could not start process", e);
            }
        }).start();
    }

    public void setProcessListener(ProcessListener listener) {
        this.listener = listener;
    }

    public void stop() throws IllegalStateException {
        progProcess.destroy();
        try {
            progProcess.waitFor();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Process termination interrupted", e);
        }
        if (progProcess.isAlive()) {
            progProcess.destroyForcibly();
            try {
                progProcess.waitFor();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new IllegalStateException("Process termination interrupted", e);
            }
            if (progProcess.isAlive()) {
                throw new IllegalStateException("Could not stop process");
            }
        }
    }
}

