// BridgeFilterConfig.java
package com.Bridge.ButtonMenu;

import net.minecraft.client.Minecraft;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonArray;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;

public class BridgeFilterConfig {

    public static boolean filterEnabled = true;
    public static boolean redHighlight = true;
    public static String selectedBot = "etobridge";
    public static final List<String> blockList = new ArrayList<String>(); // final для потокобезопасности

    public static boolean nickHighlightEnabled = true;
    public static String nickHighlightColor = "§e"; // Код цвета для Minecraft (внутреннее использование)
    public static String nickHighlightColorName = "yellow"; // Название цвета для конфига (yellow, red, green и т.д.)

    public static boolean guildBridgeFormatEnabled = true;
    
    // Маппинг названий цветов на коды Minecraft
    private static final String[][] COLOR_MAPPING = {
        {"yellow", "§e"},
        {"red", "§c"},
        {"green", "§a"},
        {"blue", "§9"},
        {"cyan", "§b"},
        {"pink", "§d"},
        {"orange", "§6"},
        {"white", "§f"},
        {"black", "§0"}
    };
    
    /**
     * Конвертирует название цвета в код Minecraft
     */
    public static String colorNameToCode(String colorName) {
        if (colorName == null || colorName.isEmpty()) {
            return "§e"; // По умолчанию желтый
        }
        String lowerName = colorName.toLowerCase().trim();
        for (String[] mapping : COLOR_MAPPING) {
            if (mapping[0].equals(lowerName)) {
                return mapping[1];
            }
        }
        // Если название не найдено, пробуем распознать код цвета
        if (colorName.startsWith("§") && colorName.length() == 2) {
            return colorName;
        }
        return "§e"; // По умолчанию желтый
    }
    
    /**
     * Конвертирует код цвета Minecraft в название
     */
    public static String colorCodeToName(String colorCode) {
        if (colorCode == null || colorCode.isEmpty()) {
            return "yellow";
        }
        String code = colorCode.trim();
        if (code.length() >= 2 && code.startsWith("§")) {
            code = code.substring(0, 2); // Берем только первые 2 символа (например "§e")
        }
        for (String[] mapping : COLOR_MAPPING) {
            if (mapping[1].equals(code)) {
                return mapping[0];
            }
        }
        return "yellow"; // По умолчанию
    }
    
    public static File configFile; // Публичное поле для доступа из других классов
    private static String configFilePath = null; // Сохраняем путь к файлу конфига для shutdown hook
    
    /**
     * Инициализирует путь к файлу конфига и загружает его
     */
    public static void init() {
        try {
            // Проверяем, что Minecraft инициализирован
            if (Minecraft.getMinecraft() == null || Minecraft.getMinecraft().mcDataDir == null) {
                System.out.println("[Bridge Filter] Minecraft еще не инициализирован, конфиг будет загружен позже");
                return;
            }
            File mcDir = Minecraft.getMinecraft().mcDataDir;
            File configDir = new File(mcDir, "config");
            configDir.mkdirs();
            configFile = new File(configDir, "BridgeFilter.json");
            // Сохраняем путь к файлу конфига для использования в shutdown hook
            try {
                configFilePath = configFile.getAbsolutePath();
            } catch (Exception e) {
                System.err.println("[Bridge Filter] Ошибка при сохранении пути к конфигу: " + e.getMessage());
            }
            load();
        } catch (Exception e) {
            System.err.println("[Bridge Filter] Ошибка при инициализации конфига: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Загружает конфиг из файла
     */
    public static void load() {
        if (configFile == null || !configFile.exists()) {
            System.out.println("[Bridge Filter] Файл конфига не найден, используем значения по умолчанию");
            return;
        }
        
        try {
            String content = new String(Files.readAllBytes(Paths.get(configFile.getAbsolutePath())), "UTF-8");
            JsonObject json = new JsonParser().parse(content).getAsJsonObject();
            
            if (json.has("filterEnabled")) {
                filterEnabled = json.get("filterEnabled").getAsBoolean();
            }
            if (json.has("redHighlight")) {
                redHighlight = json.get("redHighlight").getAsBoolean();
            }
            if (json.has("selectedBot")) {
                selectedBot = json.get("selectedBot").getAsString();
            }
            if (json.has("blockList")) {
                synchronized (blockList) {
                    blockList.clear();
                    JsonArray blockArray = json.getAsJsonArray("blockList");
                    int loadedCount = 0;
                    System.out.println("[Bridge Filter] Загрузка блоклиста из конфига: найдено " + blockArray.size() + " элементов");
                    for (int i = 0; i < blockArray.size(); i++) {
                        try {
                            String item = blockArray.get(i).getAsString();
                            if (item != null && !item.trim().isEmpty()) {
                                blockList.add(item);
                                loadedCount++;
                                System.out.println("[Bridge Filter] Загружен элемент блоклиста [" + loadedCount + "]: " + item);
                            }
                        } catch (Exception e) {
                            System.err.println("[Bridge Filter] Ошибка при загрузке элемента блоклиста [" + i + "]: " + e.getMessage());
                            // Пропускаем проблемный элемент и продолжаем
                        }
                    }
                    System.out.println("[Bridge Filter] Блоклист загружен: всего " + loadedCount + " элементов");
                }
            } else {
                System.out.println("[Bridge Filter] В конфиге нет блоклиста, используем пустой список");
            }
            if (json.has("nickHighlightEnabled")) {
                nickHighlightEnabled = json.get("nickHighlightEnabled").getAsBoolean();
            }
            if (json.has("nickHighlightColor")) {
                String colorValue = json.get("nickHighlightColor").getAsString();
                // Поддержка старого формата (код цвета) и нового (название)
                if (colorValue != null && !colorValue.isEmpty()) {
                    if (colorValue.startsWith("§")) {
                        // Старый формат - код цвета, конвертируем в название
                        nickHighlightColor = colorValue.length() >= 2 ? colorValue.substring(0, 2) : "§e";
                        nickHighlightColorName = colorCodeToName(nickHighlightColor);
                        System.out.println("[Bridge Filter] Загружен цвет в старом формате: " + nickHighlightColor + " -> " + nickHighlightColorName);
                    } else {
                        // Новый формат - название цвета
                        nickHighlightColorName = colorValue.toLowerCase().trim();
                        nickHighlightColor = colorNameToCode(nickHighlightColorName);
                        System.out.println("[Bridge Filter] Загружен цвет: название = " + nickHighlightColorName + ", код = " + nickHighlightColor);
                    }
                } else {
                    // Значение по умолчанию
                    nickHighlightColorName = "yellow";
                    nickHighlightColor = "§e";
                }
            } else {
                // Если цвет не указан в конфиге, используем значения по умолчанию
                if (nickHighlightColorName == null || nickHighlightColorName.isEmpty()) {
                    nickHighlightColorName = "yellow";
                }
                if (nickHighlightColor == null || nickHighlightColor.isEmpty()) {
                    nickHighlightColor = "§e";
                }
            }
            if (json.has("guildBridgeFormatEnabled")) {
                guildBridgeFormatEnabled = json.get("guildBridgeFormatEnabled").getAsBoolean();
            }
            
            System.out.println("[Bridge Filter] Конфиг загружен: " + blockList.size() + " заблокированных ников");
        } catch (Exception e) {
            System.err.println("[Bridge Filter] Ошибка при загрузке конфига: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Сохраняет конфиг в файл (в отдельном потоке, чтобы не блокировать UI)
     * Для критических случаев (shutdown hook) используйте saveSync()
     */
    public static void save() {
        // Сохраняем в отдельном потоке, чтобы не блокировать UI и избежать крашей
        new Thread(() -> {
            try {
                saveSync();
            } catch (Exception e) {
                System.err.println("[Bridge Filter] КРИТИЧЕСКАЯ ОШИБКА при сохранении конфига: " + e.getMessage());
                e.printStackTrace();
            }
        }, "BridgeFilter-ConfigSave").start();
    }
    
    /**
     * Синхронное сохранение конфига (для shutdown hook)
     * Блокирует поток до завершения сохранения
     */
    public static void saveSync() {
        // Вызываем внутренний метод синхронно (без потока)
        try {
            saveSyncInternal();
        } catch (Exception e) {
            System.err.println("[Bridge Filter] Ошибка при синхронном сохранении конфига: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Внутренний метод синхронного сохранения конфига (используется и save() и saveSync())
     */
    private static void saveSyncInternal() {
        // Если configFile null, пытаемся восстановить путь
        if (configFile == null || !configFile.exists()) {
            try {
                // Сначала пробуем использовать сохраненный путь (для shutdown hook)
                if (configFilePath != null && !configFilePath.isEmpty()) {
                    configFile = new File(configFilePath);
                    if (configFile.exists()) {
                        // Файл существует, используем его
                        System.out.println("[Bridge Filter] Использован сохраненный путь к конфигу: " + configFilePath);
                    } else {
                        // Файл не существует, но путь валидный - создаем родительскую папку
                        File parentDir = configFile.getParentFile();
                        if (parentDir != null && !parentDir.exists()) {
                            parentDir.mkdirs();
                        }
                    }
                }
                
                // Если все еще null или путь невалидный, пробуем через Minecraft (только если доступен)
                if (configFile == null || (configFile.getParentFile() == null)) {
                    try {
                        if (Minecraft.getMinecraft() != null && Minecraft.getMinecraft().mcDataDir != null) {
                            File mcDir = Minecraft.getMinecraft().mcDataDir;
                            File configDir = new File(mcDir, "config");
                            if (!configDir.exists()) {
                                configDir.mkdirs();
                            }
                            configFile = new File(configDir, "BridgeFilter.json");
                            configFilePath = configFile.getAbsolutePath(); // Сохраняем путь
                        } else if (configFilePath == null || configFilePath.isEmpty()) {
                            System.err.println("[Bridge Filter] Minecraft не инициализирован и сохраненный путь недоступен, конфиг не может быть сохранен");
                            return;
                        }
                    } catch (Exception e) {
                        System.err.println("[Bridge Filter] Ошибка при доступе к Minecraft: " + e.getMessage());
                        // Если есть сохраненный путь, пробуем использовать его
                        if (configFilePath != null && !configFilePath.isEmpty()) {
                            configFile = new File(configFilePath);
                            File parentDir = configFile.getParentFile();
                            if (parentDir != null && !parentDir.exists()) {
                                parentDir.mkdirs();
                            }
                        } else {
                            return;
                        }
                    }
                }
            } catch (Exception e) {
                System.err.println("[Bridge Filter] Ошибка при создании файла конфига: " + e.getMessage());
                e.printStackTrace();
                return;
            }
        }
        
        try {
            JsonObject json = new JsonObject();
            json.addProperty("filterEnabled", filterEnabled);
            json.addProperty("redHighlight", redHighlight);
            json.addProperty("selectedBot", selectedBot != null ? selectedBot : "etobridge");
            
            JsonArray blockArray = new JsonArray();
            // Синхронизируем доступ к blockList, чтобы избежать ConcurrentModificationException
            int blockListSize = 0;
            synchronized (blockList) {
                blockListSize = blockList.size();
                System.out.println("[Bridge Filter] Сохранение блоклиста: размер = " + blockListSize);
                for (String item : blockList) {
                    if (item != null && !item.trim().isEmpty()) {
                        // Gson 2.2.4 (в MC 1.8.9) не имеет add(String), только add(JsonElement)
                        blockArray.add(new JsonPrimitive(item));
                        System.out.println("[Bridge Filter] Сохранение в блоклист: " + item);
                    }
                }
            }
            json.add("blockList", blockArray);
            
            json.addProperty("nickHighlightEnabled", nickHighlightEnabled);
            // Сохраняем название цвета, а не код (для читаемости конфига)
            String colorNameToSave = nickHighlightColorName != null && !nickHighlightColorName.isEmpty() ? 
                nickHighlightColorName : colorCodeToName(nickHighlightColor != null ? nickHighlightColor : "§e");
            json.addProperty("nickHighlightColor", colorNameToSave);
            json.addProperty("guildBridgeFormatEnabled", guildBridgeFormatEnabled);
            
            System.out.println("[Bridge Filter] Сохранение конфига: blockList.size() = " + blockArray.size() + ", цвет = " + colorNameToSave);
            
            Gson gson = new GsonBuilder().setPrettyPrinting().create();
            String jsonString = gson.toJson(json);
            
            // Логируем путь, куда сохраняем (для отладки)
            try {
                System.out.println("[Bridge Filter] Путь к конфигу: " + (configFile != null ? configFile.getAbsolutePath() : "null"));
            } catch (Exception ignore) {}
            
            // Используем временный файл для атомарного сохранения
            File tempFile = new File(configFile.getParent(), configFile.getName() + ".tmp");
            try (FileWriter writer = new FileWriter(tempFile, false)) {
                writer.write(jsonString);
                writer.flush();
            }
            
            // Атомарно заменяем старый файл новым (с повторными попытками для Windows)
            if (tempFile.exists() && tempFile.length() > 0) {
                boolean renamed = false;
                // Пытаемся удалить старый файл и переименовать новый (до 5 попыток для Windows)
                for (int attempt = 0; attempt < 5; attempt++) {
                    try {
                        if (configFile.exists()) {
                            configFile.delete();
                        }
                        Thread.sleep(50); // Небольшая задержка для Windows
                        if (tempFile.renameTo(configFile)) {
                            renamed = true;
                            break;
                        }
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        break;
                    } catch (Exception e) {
                        System.err.println("[Bridge Filter] Попытка " + (attempt + 1) + " замены файла не удалась: " + e.getMessage());
                    }
                }
                
                if (!renamed) {
                    // Если renameTo не сработал (Windows), пробуем через копирование
                    try {
                        if (configFile.exists()) {
                            configFile.delete();
                        }
                        Files.copy(tempFile.toPath(), configFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
                        tempFile.delete();
                        renamed = true;
                    } catch (Exception e2) {
                        System.err.println("[Bridge Filter] Не удалось скопировать временный файл: " + e2.getMessage());
                        tempFile.delete(); // Удаляем временный файл при ошибке
                    }
                }
                
                if (renamed) {
                    // Проверяем размер блоклиста из JSON для подтверждения
                    int savedBlockListSize = blockArray.size();
                    synchronized (blockList) {
                        int currentBlockListSize = blockList.size();
                        System.out.println("[Bridge Filter] Конфиг сохранен успешно: " + savedBlockListSize + " элементов в блоклисте (текущий размер: " + currentBlockListSize + ")");
                        if (savedBlockListSize != currentBlockListSize) {
                            System.err.println("[Bridge Filter] ВНИМАНИЕ: Размер блоклиста не совпадает! Сохранено: " + savedBlockListSize + ", текущий: " + currentBlockListSize);
                        }
                    }
                } else {
                    System.err.println("[Bridge Filter] ВНИМАНИЕ: Не удалось сохранить конфиг, временный файл: " + tempFile.getAbsolutePath());
                    // Последний форсированный fallback: пишем напрямую в configFile без rename/copy
                    try (FileWriter directWriter = new FileWriter(configFile, false)) {
                        directWriter.write(jsonString);
                        directWriter.flush();
                        System.err.println("[Bridge Filter] Fallback-сохранение напрямую в файл выполнено успешно");
                    } catch (Exception directEx) {
                        System.err.println("[Bridge Filter] Fallback-сохранение не удалось: " + directEx.getMessage());
                    } finally {
                        try { tempFile.delete(); } catch (Exception ignore) {}
                    }
                }
            } else {
                System.err.println("[Bridge Filter] Временный файл не существует или пуст");
            }
        } catch (Exception e) {
            System.err.println("[Bridge Filter] Ошибка при сохранении конфига: " + e.getMessage());
            e.printStackTrace();
            // Удаляем временный файл при ошибке
            try {
                File tempFile = new File(configFile.getParent(), configFile.getName() + ".tmp");
                if (tempFile.exists()) {
                    tempFile.delete();
                }
            } catch (Exception e2) {
                // Игнорируем ошибку удаления временного файла
            }
        }
    }

}