package com.modtranslator;

import javax.swing.*;
import java.awt.*;
import java.awt.datatransfer.DataFlavor;
import java.awt.dnd.*;
import java.io.File;
import java.util.List;

public class MainApp {
    private JFrame frame;
    private JTextField inputFileField;
    private JTextField outputDirField;
    private JComboBox<String> languageComboBox;
    private JProgressBar progressBar;
    private JLabel progressLabel;
    private ModTranslator translator;

    public MainApp() {
        translator = new ModTranslator();
        createAndShowGUI();
    }

    private void createAndShowGUI() {
        frame = new JFrame("Minecraft Mod Translator");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(600, 300);
        frame.setResizable(false);
        frame.setLayout(new BorderLayout(10, 10));

        // Title Panel
        JPanel titlePanel = new JPanel();
        JLabel titleLabel = new JLabel("Minecraft Mod Translator");
        titleLabel.setFont(new Font("Arial", Font.BOLD, 20));
        titlePanel.add(titleLabel);
        frame.add(titlePanel, BorderLayout.NORTH);

        // Main Panel
        JPanel mainPanel = new JPanel();
        mainPanel.setLayout(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(5, 5, 5, 5);

        // Input File Panel
        JPanel filePanel = new JPanel(new BorderLayout(5, 0));
        inputFileField = new JTextField(30);
        inputFileField.setEditable(false);
        JButton browseButton = new JButton("Select Mod");
        filePanel.add(inputFileField, BorderLayout.CENTER);
        filePanel.add(browseButton, BorderLayout.EAST);

        // Add Drag and Drop support
        setupDragAndDrop(inputFileField);

        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.gridwidth = 2;
        mainPanel.add(filePanel, gbc);

        // Output Directory Panel
        JPanel outputPanel = new JPanel(new BorderLayout(5, 0));
        outputDirField = new JTextField(30);
        outputDirField.setEditable(false);
        
        // Set default output path and create directory
        File defaultOutputDir = new File("translated_mods");
        if (!defaultOutputDir.exists()) {
            defaultOutputDir.mkdirs();
        }
        outputDirField.setText(defaultOutputDir.getAbsolutePath());
        
        JButton outputButton = new JButton("Select Folder");
        outputPanel.add(outputDirField, BorderLayout.CENTER);
        outputPanel.add(outputButton, BorderLayout.EAST);

        gbc.gridy = 1;
        mainPanel.add(outputPanel, gbc);

        // Language Selection
        JPanel langPanel = new JPanel(new BorderLayout(5, 0));
        JLabel langLabel = new JLabel("Target Language: ");
        languageComboBox = new JComboBox<>();
        langPanel.add(langLabel, BorderLayout.WEST);
        langPanel.add(languageComboBox, BorderLayout.CENTER);

        gbc.gridy = 2;
        mainPanel.add(langPanel, gbc);

        // Progress Bar
        progressBar = new JProgressBar(0, 100);
        progressBar.setStringPainted(true);
        gbc.gridy = 3;
        mainPanel.add(progressBar, gbc);

        // Progress Label
        progressLabel = new JLabel("Ready to translate", SwingConstants.CENTER);
        gbc.gridy = 4;
        mainPanel.add(progressLabel, gbc);

        // Translate Button
        JButton translateButton = new JButton("Translate");
        gbc.gridy = 5;
        mainPanel.add(translateButton, gbc);

        frame.add(mainPanel, BorderLayout.CENTER);

        // Add languages to combo box
        for (String language : translator.getAvailableLanguages()) {
            languageComboBox.addItem(language);
        }

        // Event Handlers
        browseButton.addActionListener(e -> {
            JFileChooser fileChooser = new JFileChooser();
            fileChooser.setFileFilter(new javax.swing.filechooser.FileFilter() {
                public boolean accept(File f) {
                    return f.isDirectory() || f.getName().toLowerCase().endsWith(".jar");
                }
                public String getDescription() {
                    return "Minecraft Mod Files (*.jar)";
                }
            });
            
            if (fileChooser.showOpenDialog(frame) == JFileChooser.APPROVE_OPTION) {
                inputFileField.setText(fileChooser.getSelectedFile().getAbsolutePath());
            }
        });

        outputButton.addActionListener(e -> {
            JFileChooser dirChooser = new JFileChooser();
            dirChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
            if (dirChooser.showOpenDialog(frame) == JFileChooser.APPROVE_OPTION) {
                outputDirField.setText(dirChooser.getSelectedFile().getAbsolutePath());
            }
        });

        translateButton.addActionListener(e -> {
            if (translateButton.getText().equals("Stop")) {
                translator.stopTranslation();
                return;
            }

            if (inputFileField.getText().isEmpty()) {
                JOptionPane.showMessageDialog(frame, "Please select a mod file");
                return;
            }
            if (outputDirField.getText().isEmpty()) {
                JOptionPane.showMessageDialog(frame, "Please select output directory");
                return;
            }

            translateButton.setText("Stop");
            translateButton.setBackground(new Color(255, 99, 71)); // Красный цвет для кнопки Stop
            progressBar.setValue(0);
            progressLabel.setText("Preparing translation...");

            translator.setProgressListener(new ModTranslator.TranslationProgressListener() {
                @Override
                public void onProgress(int current, int total) {
                    SwingUtilities.invokeLater(() -> {
                        int percentage = (int) ((current * 100.0) / total);
                        progressBar.setValue(percentage);
                        progressLabel.setText(String.format("Translated %d of %d lines", current, total));
                    });
                }

                @Override
                public void onComplete() {
                    SwingUtilities.invokeLater(() -> {
                        progressBar.setValue(100);
                        progressLabel.setText("Translation completed!");
                        translateButton.setText("Translate");
                        translateButton.setBackground(null); // Возвращаем обычный цвет кнопки
                        JOptionPane.showMessageDialog(frame, "Translation completed successfully!");
                    });
                }

                @Override
                public void onStopped() {
                    SwingUtilities.invokeLater(() -> {
                        progressBar.setValue(0);
                        progressLabel.setText("Translation stopped");
                        translateButton.setText("Translate");
                        translateButton.setBackground(null); // Возвращаем обычный цвет кнопки
                    });
                }
            });

            new Thread(() -> {
                try {
                    translator.translate(
                        inputFileField.getText(),
                        outputDirField.getText(),
                        (String) languageComboBox.getSelectedItem()
                    );
                } catch (Exception ex) {
                    SwingUtilities.invokeLater(() -> {
                        JOptionPane.showMessageDialog(frame, "Translation error: " + ex.getMessage());
                        translateButton.setText("Translate");
                        translateButton.setBackground(null); // Возвращаем обычный цвет кнопки
                        progressBar.setValue(0);
                        progressLabel.setText("Ready to translate");
                    });
                }
            }).start();
        });

        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
    }

    private void setupDragAndDrop(JTextField field) {
        new DropTarget(field, new DropTargetAdapter() {
            public void drop(DropTargetDropEvent event) {
                try {
                    event.acceptDrop(DnDConstants.ACTION_COPY);
                    List<File> droppedFiles = (List<File>) 
                        event.getTransferable().getTransferData(DataFlavor.javaFileListFlavor);
                    
                    if (!droppedFiles.isEmpty()) {
                        File file = droppedFiles.get(0);
                        if (file.getName().toLowerCase().endsWith(".jar")) {
                            field.setText(file.getAbsolutePath());
                        } else {
                            JOptionPane.showMessageDialog(frame, 
                                "Please drop a .jar file");
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new MainApp());
    }
}
