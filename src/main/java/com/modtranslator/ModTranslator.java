package com.modtranslator;

import okhttp3.*;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonElement;
import com.google.gson.JsonSyntaxException;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import java.util.jar.*;
import java.util.regex.Pattern;
import java.net.URLEncoder;
import java.util.logging.FileHandler;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;

public class ModTranslator {
    private static final Pattern COLOR_CODE_PATTERN = Pattern.compile("(§[0-9a-fk-or])");
    private static final Logger LOGGER = Logger.getLogger(ModTranslator.class.getName());
    private static FileHandler fileHandler;
    private static boolean loggingEnabled = false;
    
    public static void setLoggingEnabled(boolean enabled) {
        if (enabled != loggingEnabled) {
            if (enabled) {
                try {
                    fileHandler = new FileHandler("mod_translator.log", true);
                    SimpleFormatter formatter = new SimpleFormatter();
                    fileHandler.setFormatter(formatter);
                    LOGGER.addHandler(fileHandler);
                    loggingEnabled = true;
                } catch (IOException e) {
                    e.printStackTrace();
                }
            } else if (fileHandler != null) {
                LOGGER.removeHandler(fileHandler);
                fileHandler.close();
                fileHandler = null;
                loggingEnabled = false;
            }
        }
    }
    
    private Map<String, String> languageCodes;
    private final OkHttpClient client;
    private final Gson gson;
    private TranslationProgressListener progressListener;
    private int totalLines;
    private int currentLine;
    private volatile boolean stopRequested = false;
    private String currentOutputJarPath;
    
    public interface TranslationProgressListener {
        void onProgress(int current, int total);
        void onComplete();
        void onStopped();
    }
    
    public ModTranslator() {
        initializeLanguageCodes();
        client = new OkHttpClient();
        gson = new Gson();
    }

    public void setProgressListener(TranslationProgressListener listener) {
        this.progressListener = listener;
    }

    private void initializeLanguageCodes() {
        languageCodes = new TreeMap<>();  // Используем TreeMap для автоматической сортировки
        
        // Обновленные названия китайских языков
        languageCodes.put("Chinese (Simplified)", "zh-CN");
        languageCodes.put("Chinese (Traditional)", "zh-TW");
        
        // Остальные языки
        languageCodes.put("Afrikaans", "af");
        languageCodes.put("Albanian", "sq");
        languageCodes.put("Amharic", "am");
        languageCodes.put("Arabic", "ar");
        languageCodes.put("Armenian", "hy");
        languageCodes.put("Azerbaijani", "az");
        languageCodes.put("Basque", "eu");
        languageCodes.put("Belarusian", "be");
        languageCodes.put("Bengali", "bn");
        languageCodes.put("Bosnian", "bs");
        languageCodes.put("Bulgarian", "bg");
        languageCodes.put("Catalan", "ca");
        languageCodes.put("Cebuano", "ceb");
        languageCodes.put("Chichewa", "ny");
        languageCodes.put("Corsican", "co");
        languageCodes.put("Croatian", "hr");
        languageCodes.put("Czech", "cs");
        languageCodes.put("Danish", "da");
        languageCodes.put("Dutch", "nl");
        languageCodes.put("English", "en");
        languageCodes.put("Esperanto", "eo");
        languageCodes.put("Estonian", "et");
        languageCodes.put("Filipino", "tl");
        languageCodes.put("Finnish", "fi");
        languageCodes.put("French", "fr");
        languageCodes.put("Frisian", "fy");
        languageCodes.put("Galician", "gl");
        languageCodes.put("Georgian", "ka");
        languageCodes.put("German", "de");
        languageCodes.put("Greek", "el");
        languageCodes.put("Gujarati", "gu");
        languageCodes.put("Haitian Creole", "ht");
        languageCodes.put("Hausa", "ha");
        languageCodes.put("Hawaiian", "haw");
        languageCodes.put("Hebrew", "iw");
        languageCodes.put("Hindi", "hi");
        languageCodes.put("Hmong", "hmn");
        languageCodes.put("Hungarian", "hu");
        languageCodes.put("Icelandic", "is");
        languageCodes.put("Igbo", "ig");
        languageCodes.put("Indonesian", "id");
        languageCodes.put("Irish", "ga");
        languageCodes.put("Italian", "it");
        languageCodes.put("Japanese", "ja");
        languageCodes.put("Javanese", "jw");
        languageCodes.put("Kannada", "kn");
        languageCodes.put("Kazakh", "kk");
        languageCodes.put("Khmer", "km");
        languageCodes.put("Korean", "ko");
        languageCodes.put("Kurdish", "ku");
        languageCodes.put("Kyrgyz", "ky");
        languageCodes.put("Lao", "lo");
        languageCodes.put("Latin", "la");
        languageCodes.put("Latvian", "lv");
        languageCodes.put("Lithuanian", "lt");
        languageCodes.put("Luxembourgish", "lb");
        languageCodes.put("Macedonian", "mk");
        languageCodes.put("Malagasy", "mg");
        languageCodes.put("Malay", "ms");
        languageCodes.put("Malayalam", "ml");
        languageCodes.put("Maltese", "mt");
        languageCodes.put("Maori", "mi");
        languageCodes.put("Marathi", "mr");
        languageCodes.put("Mongolian", "mn");
        languageCodes.put("Myanmar", "my");
        languageCodes.put("Nepali", "ne");
        languageCodes.put("Norwegian", "no");
        languageCodes.put("Pashto", "ps");
        languageCodes.put("Persian", "fa");
        languageCodes.put("Polish", "pl");
        languageCodes.put("Portuguese", "pt");
        languageCodes.put("Punjabi", "pa");
        languageCodes.put("Romanian", "ro");
        languageCodes.put("Russian", "ru");
        languageCodes.put("Samoan", "sm");
        languageCodes.put("Scots Gaelic", "gd");
        languageCodes.put("Serbian", "sr");
        languageCodes.put("Sesotho", "st");
        languageCodes.put("Shona", "sn");
        languageCodes.put("Sindhi", "sd");
        languageCodes.put("Sinhala", "si");
        languageCodes.put("Slovak", "sk");
        languageCodes.put("Slovenian", "sl");
        languageCodes.put("Somali", "so");
        languageCodes.put("Spanish", "es");
        languageCodes.put("Sundanese", "su");
        languageCodes.put("Swahili", "sw");
        languageCodes.put("Swedish", "sv");
        languageCodes.put("Tajik", "tg");
        languageCodes.put("Tamil", "ta");
        languageCodes.put("Telugu", "te");
        languageCodes.put("Thai", "th");
        languageCodes.put("Turkish", "tr");
        languageCodes.put("Ukrainian", "uk");
        languageCodes.put("Urdu", "ur");
        languageCodes.put("Uzbek", "uz");
        languageCodes.put("Vietnamese", "vi");
        languageCodes.put("Welsh", "cy");
        languageCodes.put("Xhosa", "xh");
        languageCodes.put("Yiddish", "yi");
        languageCodes.put("Yoruba", "yo");
        languageCodes.put("Zulu", "zu");
    }

    public String[] getAvailableLanguages() {
        return languageCodes.keySet().toArray(new String[0]);
    }

    public void stopTranslation() {
        stopRequested = true;
        if (currentOutputJarPath != null) {
            try {
                File outputFile = new File(currentOutputJarPath);
                if (outputFile.exists()) {
                    outputFile.delete();
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public void translate(String inputJarPath, String outputDirectory, String targetLanguage) throws IOException {
        LOGGER.info("Starting translation process");
        LOGGER.info("Input file: " + inputJarPath);
        LOGGER.info("Output directory: " + outputDirectory);
        LOGGER.info("Target language: " + targetLanguage);

        stopRequested = false;
        File outputDir = new File(outputDirectory);
        if (!outputDir.exists()) {
            outputDir.mkdirs();
        }

        File inputJar = new File(inputJarPath);
        currentOutputJarPath = Paths.get(outputDirectory, "translated_" + inputJar.getName()).toString();

        processJarFile(new JarFile(inputJar), new File(currentOutputJarPath), targetLanguage);

        if (stopRequested) {
            LOGGER.info("Translation stopped by user");
            if (progressListener != null) {
                progressListener.onStopped();
            }
            // Удаляем незавершенный файл
            try {
                File outputFile = new File(currentOutputJarPath);
                if (outputFile.exists()) {
                    outputFile.delete();
                    LOGGER.info("Deleted incomplete output file");
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else if (progressListener != null) {
            progressListener.onComplete();
        }
    }

    private void processJarFile(JarFile jarFile, File outputFile, String targetLanguage) throws IOException {
        LOGGER.info("Starting JAR processing: " + jarFile.getName());
        String targetLangCode = convertToMinecraftLangCode(languageCodes.get(targetLanguage));
        
        try (JarOutputStream jos = new JarOutputStream(new FileOutputStream(outputFile))) {
            Enumeration<JarEntry> entries = jarFile.entries();
            while (entries.hasMoreElements() && !stopRequested) {
                JarEntry entry = entries.nextElement();
                String entryName = entry.getName();
                LOGGER.fine("Processing entry: " + entryName);
                
                // Копируем текущий файл как есть
                JarEntry newEntry = new JarEntry(entryName);
                jos.putNextEntry(newEntry);
                try (InputStream is = jarFile.getInputStream(entry)) {
                    byte[] buffer = new byte[1024];
                    int bytesRead;
                    while ((bytesRead = is.read(buffer)) != -1) {
                        jos.write(buffer, 0, bytesRead);
                    }
                }
                jos.closeEntry();
                
                // Если это языковой файл, создаем дополнительный переведенный файл
                if (isLanguageFile(entryName)) {
                    String newFileName = entryName.toLowerCase().replace("en_us", targetLangCode);
                    LOGGER.info("Creating additional language file: " + newFileName);
                    
                    JarEntry translatedEntry = new JarEntry(newFileName);
                    jos.putNextEntry(translatedEntry);
                    
                    byte[] translatedContent = translateLangFile(jarFile.getInputStream(entry), targetLanguage);
                    jos.write(translatedContent);
                    LOGGER.info("Finished creating translated file: " + newFileName);
                    jos.closeEntry();
                }
            }
        }

        if (stopRequested) {
            LOGGER.info("Translation stopped by user");
            if (outputFile.exists()) {
                outputFile.delete();
                LOGGER.info("Deleted incomplete output file");
            }
        } else {
            LOGGER.info("JAR processing completed successfully");
        }
    }

    private boolean isLanguageFile(String fileName) {
        LOGGER.info("Checking file: " + fileName);
        String lowerFileName = fileName.toLowerCase();
        return lowerFileName.endsWith("en_us.lang") || lowerFileName.endsWith("en_us.json");
    }

    private byte[] translateLangFile(InputStream is, String targetLanguage) throws IOException {
        LOGGER.info("Starting language file translation");
        
        // Читаем весь файл в строку для определения формата
        ByteArrayOutputStream result = new ByteArrayOutputStream();
        byte[] buffer = new byte[1024];
        int length;
        while ((length = is.read(buffer)) != -1) {
            result.write(buffer, 0, length);
        }
        String content = result.toString(StandardCharsets.UTF_8.name());
        
        // Определяем формат файла
        boolean isJsonFormat = content.trim().startsWith("{");
        LOGGER.info("File format detected: " + (isJsonFormat ? "JSON" : "LANG"));
        
        if (isJsonFormat) {
            try {
                JsonObject langJson = gson.fromJson(content, JsonObject.class);
                JsonObject translatedJson = new JsonObject();
                int totalEntries = langJson.size();
                int currentEntry = 0;
                LOGGER.info("Found " + totalEntries + " entries in JSON file");

                for (Map.Entry<String, JsonElement> entry : langJson.entrySet()) {
                    if (stopRequested) break;
                    
                    String key = entry.getKey();
                    String value = entry.getValue().getAsString();
                    LOGGER.fine("Processing JSON entry " + currentEntry + "/" + totalEntries + ": " + key);

                    List<String> colorCodes = new ArrayList<>();
                    java.util.regex.Matcher matcher = COLOR_CODE_PATTERN.matcher(value);
                    while (matcher.find()) {
                        colorCodes.add(matcher.group());
                    }
                    String textToTranslate = COLOR_CODE_PATTERN.matcher(value).replaceAll("");
                    
                    String translatedText = translateText(textToTranslate, targetLanguage);
                    
                    // Восстанавливаем цветовые коды
                    for (String colorCode : colorCodes) {
                        translatedText = colorCode + translatedText;
                    }
                    
                    translatedJson.addProperty(key, translatedText);
                    currentEntry++;
                    if (progressListener != null) {
                        progressListener.onProgress(currentEntry, totalEntries);
                    }
                }

                if (stopRequested) {
                    throw new IOException("Translation stopped by user");
                }

                return gson.toJson(translatedJson).getBytes(StandardCharsets.UTF_8);
            } catch (JsonSyntaxException e) {
                LOGGER.severe("Error parsing JSON language file: " + e.getMessage());
                throw new IOException("Invalid JSON format in language file", e);
            }
        } else {
            // Обрабатываем .lang формат
            BufferedReader reader = new BufferedReader(new StringReader(content));
            StringBuilder langResult = new StringBuilder();
            String line;
            int lineNumber = 0;
            int totalLines = content.split("\n").length;
            
            while ((line = reader.readLine()) != null && !stopRequested) {
                lineNumber++;
                LOGGER.fine("Processing line " + lineNumber + ": " + line);
                
                if (line.trim().isEmpty() || !line.contains("=")) {
                    LOGGER.fine("Skipping line " + lineNumber + " (empty or no translation needed)");
                    langResult.append(line).append("\n");
                    continue;
                }

                String[] parts = line.split("=", 2);
                if (parts.length != 2) {
                    LOGGER.fine("Skipping malformed line " + lineNumber);
                    langResult.append(line).append("\n");
                    continue;
                }

                String key = parts[0];
                String value = parts[1];
                LOGGER.fine("Key: " + key + ", Original value: " + value);

                List<String> colorCodes = new ArrayList<>();
                java.util.regex.Matcher matcher = COLOR_CODE_PATTERN.matcher(value);
                while (matcher.find()) {
                    colorCodes.add(matcher.group());
                }
                String textToTranslate = COLOR_CODE_PATTERN.matcher(value).replaceAll("");
                LOGGER.fine("Text to translate (without color codes): " + textToTranslate);

                if (stopRequested) {
                    break;
                }

                String translatedText = translateText(textToTranslate, targetLanguage);
                LOGGER.fine("Translated text: " + translatedText);

                if (stopRequested) {
                    break;
                }

                for (String colorCode : colorCodes) {
                    translatedText = colorCode + translatedText;
                }
                LOGGER.fine("Final text with color codes: " + translatedText);

                langResult.append(key).append("=").append(translatedText).append("\n");
                
                if (progressListener != null) {
                    progressListener.onProgress(lineNumber, totalLines);
                }
            }

            if (stopRequested) {
                throw new IOException("Translation stopped by user");
            }

            LOGGER.info("Finished translating language file, processed " + lineNumber + " lines");
            return langResult.toString().getBytes(StandardCharsets.UTF_8);
        }
    }

    private String translateText(String text, String targetLanguage) {
        if (text.trim().isEmpty()) {
            return text;
        }
        
        if (stopRequested) {
            return text;
        }

        try {
            Thread.sleep(50);
            
            String targetLangCode = languageCodes.get(targetLanguage);
            if (targetLangCode == null) {
                LOGGER.warning("Unsupported language: " + targetLanguage);
                throw new IllegalArgumentException("Unsupported language: " + targetLanguage);
            }

            String encodedText = URLEncoder.encode(text, "UTF-8");
            String url = String.format(
                "https://translate.googleapis.com/translate_a/single?client=gtx&sl=en&tl=%s&dt=t&q=%s",
                targetLangCode,
                encodedText
            );

            LOGGER.fine("Translating text: " + text);
            Request request = new Request.Builder()
                .url(url)
                .header("User-Agent", "Mozilla/5.0")
                .build();

            try (Response response = client.newCall(request).execute()) {
                if (!response.isSuccessful()) {
                    LOGGER.warning("Translation request failed: " + response.code());
                    return text;
                }

                String responseBody = response.body().string();
                JsonArray jsonArray = gson.fromJson(responseBody, JsonArray.class);
                if (jsonArray != null && jsonArray.size() > 0) {
                    JsonArray translationArray = jsonArray.get(0).getAsJsonArray();
                    if (translationArray != null && translationArray.size() > 0) {
                        StringBuilder translatedText = new StringBuilder();
                        for (int i = 0; i < translationArray.size(); i++) {
                            JsonArray translationPart = translationArray.get(i).getAsJsonArray();
                            if (translationPart != null && translationPart.size() > 0) {
                                translatedText.append(translationPart.get(0).getAsString());
                            }
                        }
                        LOGGER.fine("Translation successful: " + translatedText.toString());
                        return translatedText.toString();
                    }
                }
                LOGGER.warning("Failed to parse translation response");
            }
        } catch (Exception e) {
            LOGGER.severe("Error translating text: " + text + "\nError: " + e.getMessage());
            e.printStackTrace();
        }
        return text;
    }

    private String convertToMinecraftLangCode(String langCode) {
        // Специальные случаи
        switch (langCode.toLowerCase()) {
            case "zh-cn": return "zh_cn";
            case "zh-tw": return "zh_tw";
            case "fil": return "tl_ph";  // Tagalog/Filipino
            case "jv": return "jv_id";   // Javanese
            case "he": return "he_il";   // Hebrew
            case "ar": return "ar_sa";   // Arabic
            case "fa": return "fa_ir";   // Persian
            case "hi": return "hi_in";   // Hindi
            case "ta": return "ta_in";   // Tamil
            case "kn": return "kn_in";   // Kannada
            case "th": return "th_th";   // Thai
            case "ko": return "ko_kr";   // Korean
            case "ja": return "ja_jp";   // Japanese
            case "ba": return "ba_ru";   // Bashkir
            case "tt": return "tt_ru";   // Tatar
            case "be": return "be_by";   // Belarusian
            case "uk": return "uk_ua";   // Ukrainian
            case "kk": return "kk_kz";   // Kazakh
            case "hy": return "hy_am";   // Armenian
            case "ka": return "ka_ge";   // Georgian
            case "el": return "el_gr";   // Greek
            case "bg": return "bg_bg";   // Bulgarian
            case "mk": return "mk_mk";   // Macedonian
        }
        
        // По умолчанию дублируем код (например, "ru" -> "ru_ru")
        return langCode.toLowerCase() + "_" + langCode.toLowerCase();
    }
}
