package com.modtranslator;

import javax.swing.*;
import java.awt.*;
import java.awt.datatransfer.DataFlavor;
import java.awt.dnd.*;
import java.io.File;
import java.util.List;

public class MainApp {
    private JTextField inputFileField;
    private JTextField outputDirField;
    private JComboBox<String> languageComboBox;
    private JProgressBar progressBar;
    private JLabel progressLabel;
    private ModTranslator translator;
    private JButton selectModButton;
    private JButton selectFolderButton;
    private JButton translateButton;
    private JCheckBox logsCheckBox;

    public MainApp() {
        translator = new ModTranslator();
        createAndShowGUI();
    }

    private void createAndShowGUI() {
        JFrame frame = new JFrame("Minecraft Mod Translator");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setResizable(false);

        // Main Panel with GridBagLayout
        JPanel mainPanel = new JPanel(new GridBagLayout());
        mainPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.weightx = 1.0;

        // Input File Panel
        JPanel filePanel = new JPanel(new BorderLayout(5, 0));
        inputFileField = new JTextField(30);
        inputFileField.setEditable(false);
        selectModButton = new JButton("Select Mod");
        filePanel.add(inputFileField, BorderLayout.CENTER);
        filePanel.add(selectModButton, BorderLayout.EAST);

        // Add Drag and Drop support
        setupDragAndDrop(inputFileField);

        mainPanel.add(filePanel, gbc);

        // Output Directory Panel
        JPanel outputPanel = new JPanel(new BorderLayout(5, 0));
        outputDirField = new JTextField(30);
        outputDirField.setEditable(false);
        
        // Set default output directory
        File defaultOutputDir = new File("target/translated_mods");
        if (!defaultOutputDir.exists()) {
            defaultOutputDir.mkdirs();
        }
        outputDirField.setText(defaultOutputDir.getAbsolutePath());
        
        selectFolderButton = new JButton("Select Folder");
        outputPanel.add(outputDirField, BorderLayout.CENTER);
        outputPanel.add(selectFolderButton, BorderLayout.EAST);

        gbc.gridy = 1;
        mainPanel.add(outputPanel, gbc);

        // Language Selection
        JPanel langPanel = new JPanel(new BorderLayout(5, 0));
        JLabel langLabel = new JLabel("Target Language:");
        languageComboBox = new JComboBox<>(translator.getAvailableLanguages());
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

        // Translate Button Panel (по центру)
        JPanel translatePanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        translateButton = new JButton("Translate");
        translatePanel.add(translateButton);
        gbc.gridy = 5;
        mainPanel.add(translatePanel, gbc);

        // Checkbox Panel (справа)
        JPanel checkboxPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        logsCheckBox = new JCheckBox("Enable Logs");
        logsCheckBox.setSelected(false);
        logsCheckBox.addActionListener(e -> {
            ModTranslator.setLoggingEnabled(logsCheckBox.isSelected());
        });
        checkboxPanel.add(logsCheckBox);
        gbc.gridy = 6;
        mainPanel.add(checkboxPanel, gbc);

        frame.add(mainPanel);
        frame.pack();
        frame.setLocationRelativeTo(null);

        // Event Handlers
        selectModButton.addActionListener(e -> {
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

        selectFolderButton.addActionListener(e -> {
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

            String inputFile = inputFileField.getText();
            String outputDir = outputDirField.getText();
            String targetLanguage = (String) languageComboBox.getSelectedItem();

            if (inputFile.isEmpty()) {
                JOptionPane.showMessageDialog(frame, "Please select a mod file to translate!");
                return;
            }

            translateButton.setText("Stop");
            translateButton.setBackground(new Color(255, 99, 71)); // Красный цвет для кнопки Stop
            progressBar.setValue(0);
            progressLabel.setText("Preparing translation...");

            // Set up progress listener
            translator.setProgressListener(new ModTranslator.TranslationProgressListener() {
                @Override
                public void onProgress(int current, int total) {
                    int percentage = (int) ((current / (double) total) * 100);
                    SwingUtilities.invokeLater(() -> {
                        progressBar.setValue(percentage);
                        progressLabel.setText(String.format("Translated %d of %d keys", current, total));
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

            // Start translation in background
            new Thread(() -> {
                try {
                    translator.translate(inputFile, outputDir, targetLanguage);
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
                            JOptionPane.showMessageDialog(null, 
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
