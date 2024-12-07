package com.modtranslator;

import okhttp3.*;
import com.google.gson.Gson;
import com.google.gson.JsonArray;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import java.util.jar.*;
import java.util.regex.Pattern;
import java.net.URLEncoder;

public class ModTranslator {
    private static final Pattern COLOR_CODE_PATTERN = Pattern.compile("(§[0-9a-fk-or])");
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
        stopRequested = false;
        File outputDir = new File(outputDirectory);
        if (!outputDir.exists()) {
            outputDir.mkdirs();
        }

        File inputJar = new File(inputJarPath);
        currentOutputJarPath = Paths.get(outputDirectory, "translated_" + inputJar.getName()).toString();

        // Сначала подсчитаем общее количество строк для перевода
        totalLines = 0;
        currentLine = 0;
        try (JarFile jarFile = new JarFile(inputJar)) {
            Enumeration<JarEntry> entries = jarFile.entries();
            while (entries.hasMoreElements()) {
                JarEntry entry = entries.nextElement();
                String entryNameLower = entry.getName().toLowerCase();
                if (entryNameLower.contains("lang/") && entryNameLower.contains("en_us.lang")) {
                    try (BufferedReader reader = new BufferedReader(new InputStreamReader(jarFile.getInputStream(entry), StandardCharsets.UTF_8))) {
                        while (reader.readLine() != null) {
                            totalLines++;
                        }
                    }
                }
            }
        }

        // Теперь выполняем перевод
        try (JarFile jarFile = new JarFile(inputJar)) {
            try (JarOutputStream jos = new JarOutputStream(new FileOutputStream(currentOutputJarPath))) {
                Enumeration<JarEntry> entries = jarFile.entries();
                
                while (entries.hasMoreElements() && !stopRequested) {
                    JarEntry entry = entries.nextElement();
                    String entryName = entry.getName();
                    String entryNameLower = entryName.toLowerCase();
                    
                    if (entryNameLower.contains("lang/") && entryNameLower.contains("en_us.lang")) {
                        try (InputStream is = jarFile.getInputStream(entry)) {
                            byte[] translatedContent = translateLangFile(is, targetLanguage);
                            
                            if (stopRequested) {
                                break;
                            }
                            
                            String langCode = languageCodes.get(targetLanguage);
                            if (langCode == null) {
                                throw new IllegalArgumentException("Unsupported language: " + targetLanguage);
                            }
                            
                            String mcLangCode = convertToMinecraftLangCode(langCode);
                            String originalLangPart = entryName.substring(
                                entryNameLower.lastIndexOf("en_us.lang"),
                                entryNameLower.lastIndexOf("en_us.lang") + "en_us.lang".length()
                            );
                            String newEntryName = entryName.replace(originalLangPart, mcLangCode + ".lang");
                            
                            JarEntry newEntry = new JarEntry(newEntryName);
                            jos.putNextEntry(newEntry);
                            jos.write(translatedContent);
                            jos.closeEntry();
                        }
                    } else {
                        jos.putNextEntry(new JarEntry(entry.getName()));
                        try (InputStream is = jarFile.getInputStream(entry)) {
                            byte[] buffer = new byte[1024];
                            int bytesRead;
                            while ((bytesRead = is.read(buffer)) != -1) {
                                jos.write(buffer, 0, bytesRead);
                            }
                        }
                        jos.closeEntry();
                    }
                }
            }
        }
        
        if (stopRequested) {
            if (progressListener != null) {
                progressListener.onStopped();
            }
            // Удаляем незавершенный файл
            try {
                File outputFile = new File(currentOutputJarPath);
                if (outputFile.exists()) {
                    outputFile.delete();
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else if (progressListener != null) {
            progressListener.onComplete();
        }
    }

    private byte[] translateLangFile(InputStream is, String targetLanguage) throws IOException {
        BufferedReader reader = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8));
        StringBuilder result = new StringBuilder();
        String line;

        while ((line = reader.readLine()) != null && !stopRequested) {
            if (line.trim().isEmpty() || !line.contains("=")) {
                result.append(line).append("\n");
                continue;
            }

            String[] parts = line.split("=", 2);
            if (parts.length != 2) {
                result.append(line).append("\n");
                continue;
            }

            String key = parts[0];
            String value = parts[1];

            List<String> colorCodes = new ArrayList<>();
            java.util.regex.Matcher matcher = COLOR_CODE_PATTERN.matcher(value);
            while (matcher.find()) {
                colorCodes.add(matcher.group());
            }
            String textToTranslate = COLOR_CODE_PATTERN.matcher(value).replaceAll("");

            if (stopRequested) {
                break;
            }

            String translatedText = translateText(textToTranslate, targetLanguage);

            if (stopRequested) {
                break;
            }

            for (String colorCode : colorCodes) {
                translatedText = colorCode + translatedText;
            }

            result.append(key).append("=").append(translatedText).append("\n");
            
            currentLine++;
            if (progressListener != null) {
                progressListener.onProgress(currentLine, totalLines);
            }
        }

        if (stopRequested) {
            throw new IOException("Translation stopped by user");
        }

        return result.toString().getBytes(StandardCharsets.UTF_8);
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
                throw new IllegalArgumentException("Unsupported language: " + targetLanguage);
            }

            String encodedText = URLEncoder.encode(text, "UTF-8");
            String url = String.format(
                "https://translate.googleapis.com/translate_a/single?client=gtx&sl=en&tl=%s&dt=t&q=%s",
                targetLangCode,
                encodedText
            );

            Request request = new Request.Builder()
                .url(url)
                .header("User-Agent", "Mozilla/5.0")
                .build();

            try (Response response = client.newCall(request).execute()) {
                if (!response.isSuccessful()) {
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
                        return translatedText.toString();
                    }
                }
            }
        } catch (Exception e) {
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
