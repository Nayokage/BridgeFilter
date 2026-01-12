// SimpleButtonMod.java
package com.Bridge.ButtonMenu;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.*;
import net.minecraft.client.gui.inventory.GuiInventory;
import net.minecraft.util.ChatComponentText;
import net.minecraftforge.client.event.ClientChatReceivedEvent;
import net.minecraftforge.client.event.GuiScreenEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import org.lwjgl.input.Keyboard;

import java.util.regex.Pattern;
import java.util.regex.Matcher;
import java.util.ArrayList;
import java.util.List;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

@Mod(modid = "bridgefilter",
        name = "Bridge Filter",
        version = "1.0.8",
        clientSideOnly = true,
        updateJSON = "https://raw.githubusercontent.com/Nayokage/BridgeFilter/main/update.json")

public class SimpleButtonMod {

    private static final int MENU_BUTTON_ID = 6969;
    private static boolean rShiftPressed = false;

    private static boolean updateChecked = false;
    private static int ticksInWorld = 0;
    private static File modsDirectory = null; // Сохраняем путь для shutdown hook
    
    /**
     * Проверяет папку updates и применяет новую версию мода
     * Вызывается при запуске игры
     */
    private static void applyUpdateFromUpdatesFolder() {
        try {
            File mcDir = Minecraft.getMinecraft().mcDataDir;
            File modsDir = new File(mcDir, "mods");
            File updatesDir = new File(modsDir, "updates");
            
            if (!updatesDir.exists()) {
                return;
            }
            
            // Ищем BridgeFilter.jar в папке updates
            File updateFile = new File(updatesDir, "BridgeFilter.jar");
            if (!updateFile.exists()) {
                return;
            }
            
            System.out.println("[Bridge Filter] Найдено обновление в папке updates, применяем...");
            
            File finalFile = new File(modsDir, "BridgeFilter.jar");
            
            // Удаляем ВСЕ старые версии BridgeFilter
            File[] allOldMods = modsDir.listFiles((dir, name) -> {
                String lowerName = name.toLowerCase();
                return lowerName.startsWith("bridgefilter") && lowerName.endsWith(".jar") 
                       && !name.endsWith(".tmp") && !name.endsWith(".delete");
            });
            
            if (allOldMods != null && allOldMods.length > 0) {
                for (File oldMod : allOldMods) {
                    try {
                        boolean deleted = false;
                        // Пытаемся удалить несколько раз
                        for (int i = 0; i < 10 && !deleted; i++) {
                            deleted = oldMod.delete();
                            if (!deleted) {
                                Thread.sleep(300);
                            }
                        }
                        
                        if (deleted) {
                            System.out.println("[Bridge Filter] Удален старый файл: " + oldMod.getName());
                        } else {
                            System.err.println("[Bridge Filter] Не удалось удалить: " + oldMod.getName() + ", пробуем переименовать...");
                            // Пробуем переименовать для удаления при следующем запуске
                            File deleteMarker = new File(oldMod.getParent(), oldMod.getName() + ".delete");
                            oldMod.renameTo(deleteMarker);
                        }
                    } catch (Exception e) {
                        System.err.println("[Bridge Filter] Ошибка при удалении " + oldMod.getName() + ": " + e.getMessage());
                    }
                }
            }
            
            // Перемещаем новый файл из updates в mods
            try {
                Path updatePath = updateFile.toPath();
                Path finalPath = finalFile.toPath();
                Files.move(updatePath, finalPath, StandardCopyOption.REPLACE_EXISTING);
                System.out.println("[Bridge Filter] Обновление успешно применено! Новая версия: BridgeFilter.jar");
            } catch (Exception e) {
                System.err.println("[Bridge Filter] Ошибка при перемещении обновления: " + e.getMessage());
                e.printStackTrace();
            }
            
        } catch (Exception e) {
            System.err.println("[Bridge Filter] Ошибка при применении обновления: " + e.getMessage());
        }
    }
    
    /**
     * Удаляет старые версии мода и помеченные файлы
     * Вызывается при запуске и при закрытии игры
     */
    private static void cleanupOldModFiles() {
        try {
            File modsDir;
            if (modsDirectory != null) {
                // Используем сохраненный путь (для shutdown hook)
                modsDir = modsDirectory;
            } else {
                // Используем текущий путь (для обычного вызова)
                File mcDir = Minecraft.getMinecraft().mcDataDir;
                modsDir = new File(mcDir, "mods");
            }
            
            if (!modsDir.exists()) {
                return;
            }
            
            // Удаляем файлы с расширением .delete
            File[] deleteMarkers = modsDir.listFiles((dir, name) -> name.endsWith(".delete"));
            if (deleteMarkers != null) {
                for (File marker : deleteMarkers) {
                    try {
                        if (marker.delete()) {
                            System.out.println("[Bridge Filter] Удален помеченный файл: " + marker.getName());
                        }
                    } catch (Exception e) {
                        // Игнорируем ошибки
                    }
                }
            }
            
            // Удаляем старые версии BridgeFilter-*.jar (кроме основного и служебного BridgeFilter-new.jar)
            File[] oldVersions = modsDir.listFiles((dir, name) -> {
                String lowerName = name.toLowerCase();
                // Не трогаем основной файл и файл, который использует автообновление
                if (name.equals("BridgeFilter.jar") || name.equals("BridgeFilter-new.jar")) {
                    return false;
                }
                return lowerName.startsWith("bridgefilter") && lowerName.endsWith(".jar")
                       && !name.endsWith(".tmp") && !name.endsWith(".delete");
            });
            if (oldVersions != null && oldVersions.length > 0) {
                for (File oldVersion : oldVersions) {
                    try {
                        boolean deleted = false;
                        // Пытаемся удалить несколько раз
                        for (int i = 0; i < 5 && !deleted; i++) {
                            deleted = oldVersion.delete();
                            if (!deleted) {
                                Thread.sleep(200);
                            }
                        }
                        
                        if (deleted) {
                            System.out.println("[Bridge Filter] Удален старый файл с версией: " + oldVersion.getName());
                        } else {
                            // Если не удалось удалить, пробуем пометить для удаления
                            try {
                                File deleteMarker = new File(oldVersion.getParent(), oldVersion.getName() + ".delete");
                                if (oldVersion.renameTo(deleteMarker)) {
                                    System.out.println("[Bridge Filter] Старый файл помечен для удаления: " + deleteMarker.getName());
                                }
                            } catch (Exception e2) {
                                System.err.println("[Bridge Filter] Не удалось пометить файл для удаления: " + oldVersion.getName());
                            }
                        }
                    } catch (Exception e) {
                        System.err.println("[Bridge Filter] Ошибка при удалении старого файла " + oldVersion.getName() + ": " + e.getMessage());
                    }
                }
            }
            
            // Удаляем временные файлы .tmp
            File[] tempFiles = modsDir.listFiles((dir, name) -> name.endsWith(".tmp"));
            if (tempFiles != null) {
                for (File tempFile : tempFiles) {
                    try {
                        tempFile.delete();
                    } catch (Exception e) {
                        // Игнорируем ошибки
                    }
                }
            }
            
            // Удаляем backup файлы .old
            File[] oldBackups = modsDir.listFiles((dir, name) -> name.equals("BridgeFilter.jar.old"));
            if (oldBackups != null) {
                for (File backup : oldBackups) {
                    try {
                        backup.delete();
                    } catch (Exception e) {
                        // Игнорируем ошибки
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("[Bridge Filter] Ошибка при очистке старых файлов: " + e.getMessage());
        }
    }

    @Mod.EventHandler
    public void init(FMLInitializationEvent event) {
        MinecraftForge.EVENT_BUS.register(this);
        
        // Сохраняем путь к папке mods для shutdown hook
        try {
            File mcDir = Minecraft.getMinecraft().mcDataDir;
            modsDirectory = new File(mcDir, "mods");
        } catch (Exception e) {
            System.err.println("[Bridge Filter] Не удалось сохранить путь к папке mods: " + e.getMessage());
        }
        
        // Загружаем конфиг при запуске игры
        new Thread(() -> {
            try {
                Thread.sleep(1000); // Ждем 1 секунду для инициализации Minecraft
                BridgeFilterConfig.init();
                System.out.println("[Bridge Filter] Конфиг загружен при старте игры");
            } catch (Exception e) {
                System.err.println("[Bridge Filter] Ошибка при загрузке конфига при старте: " + e.getMessage());
            }
        }).start();
        
        // Добавляем shutdown hook для сохранения конфига и удаления старых файлов при закрытии игры
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            System.out.println("[Bridge Filter] ========================================");
            System.out.println("[Bridge Filter] Игра закрывается, сохраняем конфиг синхронно...");
            try {
                // Логируем текущий размер блоклиста перед сохранением
                synchronized (BridgeFilterConfig.blockList) {
                    System.out.println("[Bridge Filter] Размер блоклиста перед сохранением: " + BridgeFilterConfig.blockList.size());
                    for (int i = 0; i < BridgeFilterConfig.blockList.size(); i++) {
                        System.out.println("[Bridge Filter]   [" + (i+1) + "] " + BridgeFilterConfig.blockList.get(i));
                    }
                }
                
                // Используем синхронное сохранение для гарантированного сохранения при закрытии
                BridgeFilterConfig.saveSync();
                
                // Ждем немного для завершения сохранения
                Thread.sleep(500);
                
                System.out.println("[Bridge Filter] Конфиг успешно сохранен при закрытии игры");
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                System.err.println("[Bridge Filter] Прервано ожидание сохранения конфига");
            } catch (Exception e) {
                System.err.println("[Bridge Filter] КРИТИЧЕСКАЯ ОШИБКА при сохранении конфига при закрытии: " + e.getMessage());
                e.printStackTrace();
            }
            System.out.println("[Bridge Filter] Игра закрывается, удаляем старые версии мода...");
            cleanupOldModFiles();
            System.out.println("[Bridge Filter] ========================================");
        }));
        
        // Применяем обновление из папки updates и удаляем старые файлы при запуске
        new Thread(() -> {
            try {
                Thread.sleep(2000); // Ждем 2 секунды после загрузки
                // Сначала применяем обновление из updates (если есть)
                applyUpdateFromUpdatesFolder();
                // Затем очищаем старые файлы
                cleanupOldModFiles();
            } catch (Exception e) {
                // Игнорируем ошибки
            }
        }).start();
    }
    
    private static boolean configLoaded = false; // Флаг для загрузки конфига
    
    @SubscribeEvent
    public void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.START) return;
        
        // Загружаем конфиг при первом входе игрока в мир (если еще не загружен)
        // Это гарантирует, что конфиг загружен ДО проверки блокировки
        if (!configLoaded && Minecraft.getMinecraft().thePlayer != null && Minecraft.getMinecraft().theWorld != null) {
            try {
                // Инициализируем путь к файлу конфига и загружаем конфиг
                BridgeFilterConfig.init(); // init() создаст путь и загрузит конфиг
                configLoaded = true;
                System.out.println("[Bridge Filter] Конфиг загружен при входе в мир: " + BridgeFilterConfig.blockList.size() + " заблокированных ников");
            } catch (Exception e) {
                System.err.println("[Bridge Filter] Ошибка при загрузке конфига при входе в мир: " + e.getMessage());
                e.printStackTrace();
            }
        }
        
        // Проверяем обновления при входе в игру (когда игрок появляется в мире)
        // Это происходит при заходе в игру, а не только при загрузке мода
        if (!updateChecked) {
            if (Minecraft.getMinecraft().thePlayer != null && Minecraft.getMinecraft().theWorld != null) {
                ticksInWorld++;
                if (ticksInWorld >= 60) { // Через 3 секунды после входа в мир (60 тиков = 3 сек при 20 TPS)
                    updateChecked = true;
                    new Thread(() -> {
                        try {
                            UpdateChecker.checkForUpdates();
                        } catch (Exception e) {
                            System.err.println("[Bridge Filter] Ошибка при проверке обновлений: " + e.getMessage());
                        }
                    }).start();
                }
            }
        }
        
        // Обработка правого Shift для открытия меню
        boolean down = Keyboard.isKeyDown(Keyboard.KEY_RSHIFT);
        if (down && !rShiftPressed && Minecraft.getMinecraft().currentScreen == null) {
            Minecraft.getMinecraft().displayGuiScreen(new BridgeFilterGUI());
        }
        rShiftPressed = down;
    }

    @SubscribeEvent
    public void onGuiInit(GuiScreenEvent.InitGuiEvent.Post event) {
        if (event.gui instanceof GuiInventory || event.gui instanceof GuiIngameMenu) {
            event.buttonList.add(new GuiButton(MENU_BUTTON_ID, 10, event.gui.height - 200, 110, 20, "Bridge Filter"));
        }
    }

    @SubscribeEvent
    public void onActionPerformed(GuiScreenEvent.ActionPerformedEvent.Post event) {
        if (event.button.id == MENU_BUTTON_ID) {
            Minecraft.getMinecraft().displayGuiScreen(new BridgeFilterGUI());
        }
    }


    @SubscribeEvent
    public void onChat(ClientChatReceivedEvent event) {
        if (event.type == 2) return; // actionbar

        // Проверяем что игрок существует
        if (Minecraft.getMinecraft() == null || Minecraft.getMinecraft().thePlayer == null) {
            return; // Если игрока нет, не обрабатываем сообщения
        }

        String unformatted = event.message != null ? event.message.getUnformattedText() : "";
        String formatted   = event.message != null ? event.message.getFormattedText() : "";
        if (unformatted == null || formatted == null) {
            return; // Защита от null
        }
        
        String playerName  = Minecraft.getMinecraft().thePlayer.getName();
        String originalBody = null; // Сохраняем body для проверки блокировки
        String originalUnformatted = unformatted; // Сохраняем оригинальный unformatted ДО форматирования (БЕЗ префиксов)

        // Форматирование Guild-сообщений от bridge-ботов
        if (BridgeFilterConfig.guildBridgeFormatEnabled) {
            if (unformatted.toLowerCase().contains("guild >")) {
                System.out.println("[Bridge Filter] onChat: обрабатываем Guild-сообщение: " + unformatted);
                
                // ИЗВЛЕКАЕМ BODY ДО ФОРМАТИРОВАНИЯ для проверки блокировки (БЕЗ префиксов [Discord], [Telegram], [Minecraft])
                originalBody = extractBodyFromGuildMessage(unformatted);
                
                String formattedGuildMsg = formatGuildBridgeMessage(unformatted, formatted);
                if (formattedGuildMsg != null) {
                    System.out.println("[Bridge Filter] onChat: форматирование применено! (префиксы добавлены)");
                    event.message = new ChatComponentText(formattedGuildMsg);
                    // Обновляем unformatted для дальнейшей обработки (теперь содержит префиксы)
                    unformatted = event.message.getUnformattedText();
                    System.out.println("[Bridge Filter] onChat: unformatted после форматирования: " + unformatted);
                } else {
                    System.out.println("[Bridge Filter] onChat: форматирование НЕ применено (вернул null)");
                }
            }
        } else {
            System.out.println("[Bridge Filter] onChat: guildBridgeFormatEnabled = false");
        }

        // ПРОВЕРКА БЛОКИРОВКИ: используем originalBody (БЕЗ префиксов) для проверки
        // Префиксы [Discord], [Telegram], [Minecraft] НЕ учитываются при блокировке
        if (BridgeFilterConfig.filterEnabled) {
            // Используем originalBody (БЕЗ префиксов) если есть, иначе originalUnformatted (тоже БЕЗ префиксов)
            String textToCheck = (originalBody != null && !originalBody.isEmpty()) ? originalBody : originalUnformatted;
            String lowerMsg = textToCheck.toLowerCase();
            String bot = BridgeFilterConfig.selectedBot.toLowerCase();
            
            System.out.println("[Bridge Filter] БЛОКИРОВКА: проверяем текст БЕЗ префиксов: " + textToCheck);
            
            // Проверяем только если это сообщение от бота (используем оригинальный unformatted БЕЗ префиксов)
            if (originalUnformatted.toLowerCase().contains(bot)) {
                // Синхронизируем доступ к blockList для избежания ConcurrentModificationException
                List<String> blockListCopy;
                synchronized (BridgeFilterConfig.blockList) {
                    blockListCopy = new ArrayList<String>(BridgeFilterConfig.blockList);
                }
                for (String word : blockListCopy) {
                    if (word != null && !word.trim().isEmpty()) {
                        String wordLower = word.toLowerCase().trim();
                        
                        // УЛУЧШЕННАЯ ПРОВЕРКА БЛОКИРОВКИ с поддержкой русских букв, уровней и цитирования:
                        // 1. Проверяем по всему body (включая уровни и цитирование)
                        // 2. Проверяем по нику до двоеточия (если есть двоеточие) - С УРОВНЕМ И БЕЗ
                        // 3. Проверяем обе части при цитировании (Nick->Other)
                        
                        boolean shouldBlock = false;
                        
                        // Проверка 1: Проверяем по всему body (включая уровни) - ОСНОВНАЯ ПРОВЕРКА
                        // Примеры:
                        // - "[515] TheIronmanNon: текст" содержит "theirnon"
                        // - "Хатабыч на Кукумбере: текст" содержит "хатабыч на кукумбере"
                        if (lowerMsg.contains(wordLower)) {
                            shouldBlock = true;
                            System.out.println("[Bridge Filter] БЛОКИРОВКА (1): найдено '" + word + "' в body: " + textToCheck);
                        }
                        
                        // Проверка 2: Если есть двоеточие, проверяем часть до двоеточия (ник с уровнем ИЛИ без)
                        // Примеры:
                        // - "[515] TheIronmanNon: текст" -> проверяем "[515] TheIronmanNon" и "Theirnon"
                        // - "Хатабыч на Кукумбере->Other: текст" -> проверяем "Хатабыч на Кукумбере"
                        if (!shouldBlock && textToCheck.contains(":")) {
                            int colonIdx = textToCheck.indexOf(':');
                            if (colonIdx > 0) {
                                String beforeColon = textToCheck.substring(0, colonIdx); // Оригинал с уровнями
                                String beforeColonLower = beforeColon.toLowerCase(); // Для проверки
                                
                                // Проверяем с уровнями: "[515] TheIronmanNon" содержит "theirnon"
                                if (beforeColonLower.contains(wordLower)) {
                                    shouldBlock = true;
                                    System.out.println("[Bridge Filter] БЛОКИРОВКА (2a): найдено '" + word + "' в нике С УРОВНЕМ: " + beforeColon);
                                }
                                
                                // Проверяем БЕЗ уровня (убираем [Level] для проверки): "TheIronmanNon" содержит "theirnon"
                                if (!shouldBlock) {
                                    String nickWithoutLevel = beforeColonLower.replaceAll("\\s*\\[[^\\]]*\\]\\s*", " ").trim();
                                    // Убираем лишние пробелы и проверяем
                                    nickWithoutLevel = nickWithoutLevel.replaceAll("\\s+", " ").trim();
                                    if (nickWithoutLevel.contains(wordLower)) {
                                        shouldBlock = true;
                                        System.out.println("[Bridge Filter] БЛОКИРОВКА (2b): найдено '" + word + "' в нике БЕЗ УРОВНЯ: " + beforeColon + " -> " + nickWithoutLevel);
                                    }
                                }
                                
                                // Проверка 3: Если есть цитирование (-> или ⇾), проверяем обе части ОТДЕЛЬНО
                                // Примеры:
                                // - "Хатабыч на Кукумбере->Other: текст" -> проверяем "Хатабыч на Кукумбере" и "Other"
                                // - "[515] TheIronmanNon->BeaverStream: текст" -> проверяем обе части
                                if (!shouldBlock && (beforeColon.contains("->") || beforeColon.contains("⇾") || beforeColon.contains("→") || beforeColon.contains("➜"))) {
                                    // Разделяем по стрелкам
                                    String[] parts = beforeColonLower.split("(->|⇾|→|➜)");
                                    for (String part : parts) {
                                        String cleanPart = part.trim();
                                        // Проверяем с уровнем
                                        if (cleanPart.contains(wordLower)) {
                                            shouldBlock = true;
                                            System.out.println("[Bridge Filter] БЛОКИРОВКА (3a): найдено '" + word + "' при цитировании С УРОВНЕМ в части: " + cleanPart);
                                            break;
                                        }
                                        // Проверяем без уровня
                                        String cleanPartNoLevel = cleanPart.replaceAll("\\s*\\[[^\\]]*\\]\\s*", " ").trim().replaceAll("\\s+", " ");
                                        if (cleanPartNoLevel.contains(wordLower)) {
                                            shouldBlock = true;
                                            System.out.println("[Bridge Filter] БЛОКИРОВКА (3b): найдено '" + word + "' при цитировании БЕЗ УРОВНЯ в части: " + cleanPart + " -> " + cleanPartNoLevel);
                                            break;
                                        }
                                    }
                                }
                            }
                        }
                        
                        if (shouldBlock) {
                            System.out.println("[Bridge Filter] БЛОКИРОВКА: СООБЩЕНИЕ ЗАБЛОКИРОВАНО! Слово: '" + word + "', Сообщение: " + textToCheck);
                            event.setCanceled(true);
                            if (BridgeFilterConfig.redHighlight) {
                                // Используем оригинальное formatted сообщение для подсветки
                                event.message = new ChatComponentText("§c" + (event.message != null ? event.message.getFormattedText() : formatted));
                            }
                            return;
                        }
                    }
                }
            }
        }

        // Подсветка ника игрока (после проверки блокировки)
        if (BridgeFilterConfig.nickHighlightEnabled && playerName != null && !playerName.isEmpty() && unformatted.contains(playerName)) {
            String currentFormatted = event.message != null ? event.message.getFormattedText() : formatted;
            if (currentFormatted != null) {
                String newMsg = currentFormatted.replaceAll(
                    "(?i)" + Pattern.quote(playerName),
                    BridgeFilterConfig.nickHighlightColor + "§l" + playerName + "§r"
            );
            event.message = new ChatComponentText(newMsg);
        }
        }
    }
    
    /**
     * Извлекает body часть из Guild-сообщения для проверки блокировки.
     * НЕ удаляет уровни - они нужны для проверки блокировки по нику с уровнем.
     * Обрабатывает форматы с уровнями и цитированием.
     * 
     * Примеры:
     * "Guild > bot[Rank]: Nick [Level]: текст" -> "Nick [Level]: текст" (С УРОВНЕМ!)
     * "Guild > bot[Rank]: Nick->Other: текст" -> "Nick->Other: текст"
     * "Guild > bot[Rank]: [515] TheIronmanNon: текст" -> "[515] TheIronmanNon: текст" (С УРОВНЕМ!)
     * "Guild > bot[Rank]: текст" -> "текст"
     */
    private String extractBodyFromGuildMessage(String fullGuildMessage) {
        if (fullGuildMessage == null || fullGuildMessage.isEmpty()) {
            return null;
        }
        
        String[] bots = {"etobridge", "koorikage", "mothikh", "tenokage", "uzbekF3ndi", "gem_zz"};
        String lower = fullGuildMessage.toLowerCase();
        
        // Находим бота
        String foundBot = null;
        int botIndex = -1;
        for (String bot : bots) {
            int idx = lower.indexOf(bot.toLowerCase());
            if (idx >= 0) {
                foundBot = bot;
                botIndex = idx;
                break;
            }
        }
        
        if (foundBot == null || botIndex < 0) {
            return null;
        }
        
        // Ищем двоеточие после ника бота
        int searchFrom = botIndex + foundBot.length();
        int colonIndex = fullGuildMessage.indexOf(':', searchFrom);
        if (colonIndex < 0 || colonIndex + 1 >= fullGuildMessage.length()) {
            return null;
        }
        
        // ИЗВЛЕКАЕМ body часть БЕЗ УДАЛЕНИЯ УРОВНЕЙ
        // Уровни должны остаться для проверки блокировки по нику с уровнем
        String body = fullGuildMessage.substring(colonIndex + 1).trim();
        
        // Проверяем что body не пустой
        if (body == null || body.isEmpty()) {
            return null;
        }
        
        // НЕ убираем уровни - оставляем как есть для проверки
        // Например: "[515] TheIronmanNon: текст" должен проверяться на наличие "TheIronmanNon"
        // даже если в сообщении есть уровень [515]
        
        return body;
    }
    
    /**
     * Простой форматтер Guild Bridge-сообщений.
     *
     * Вход (unformatted), примеры:
     *  "Guild > [MVP+] etobridge[Officer]: .Steve: hi"
     *  "Guild > [VIP] etobridge[Member]: [TG] Alex: yo"
     *  "Guild > etobridge[Admin]: Bob: hello"
     *  "Guild > etobridge[Admin]: Nayokage's networth: 1.522b"
     *
     * Выход:
     *  "[Minecraft] Steve: hi"
     *  "[Telegram] Alex: yo"
     *  "[Discord] Bob: hello"
     *  "Nayokage's networth: 1.522b" (команда без префикса)
     *
     * Сообщения обычных игроков гильдии не трогаются.
     */
    private String formatGuildBridgeMessage(String fullGuildMessage, String formattedMessage) {
        if (fullGuildMessage == null) return null;

        System.out.println("[Bridge Filter] formatGuildBridgeMessage вызван: " + fullGuildMessage);
        
        String[] bots = {"etobridge", "koorikage", "mothikh", "tenokage", "uzbekF3ndi", "gem_zz"};

        String lower = fullGuildMessage.toLowerCase();

        // Должно быть Guild-сообщение
        if (!lower.contains("guild >")) {
            System.out.println("[Bridge Filter] Не Guild-сообщение");
            return null;
        }

        // Находим бота
        String foundBot = null;
        int botIndex = -1;
        for (String bot : bots) {
            int idx = lower.indexOf(bot.toLowerCase());
            if (idx >= 0) {
                foundBot = bot;
                botIndex = idx;
                break;
            }
        }
        if (foundBot == null || botIndex < 0) {
            System.out.println("[Bridge Filter] Не от bridge-бота");
            return null; // не наш бот
        }
        
        System.out.println("[Bridge Filter] Найден бот: " + foundBot + " на позиции " + botIndex);

        // Ищем двоеточие после ника бота: Guild > ... BOT[Rank]: body
        int searchFrom = botIndex + foundBot.length();
        int colonIndex = fullGuildMessage.indexOf(':', searchFrom);
        if (colonIndex < 0 || colonIndex + 1 >= fullGuildMessage.length()) {
            return null;
        }

        String body = fullGuildMessage.substring(colonIndex + 1).trim();
        if (body.isEmpty()) {
            return null;
        }

        String result = body; // По умолчанию возвращаем body без изменений
        String sourceColor = ""; // Цвет для префикса источника
        String lowerBody = body.toLowerCase().trim();
        body = body.trim(); // Убираем пробелы в начале
        
        // Проверяем Minecraft Bridge (начинается с точки)
        if (body != null && !body.isEmpty() && body.startsWith(".")) {
            String rest = body.substring(1).trim(); // убираем точку
            if (rest != null && !rest.isEmpty()) {
                sourceColor = "§a"; // Зеленый цвет для Minecraft
                System.out.println("[Bridge Filter] Minecraft сообщение обнаружено, rest: '" + rest + "'");
                
                // Проверяем, есть ли ник (формат: .Nick: текст)
                int colonIdx = rest.indexOf(':');
                System.out.println("[Bridge Filter] colonIdx: " + colonIdx);
                
                if (colonIdx > 0 && colonIdx < 50 && colonIdx < rest.length()) {
                    String beforeColon = rest.substring(0, colonIdx).trim();
                    System.out.println("[Bridge Filter] beforeColon: '" + beforeColon + "', length: " + (beforeColon != null ? beforeColon.length() : 0));
                    
                    if (beforeColon != null && !beforeColon.isEmpty()) {
                        // Проверяем, что это валидный ник (буквы (включая русские), цифры, подчеркивания, 2-16 символов)
                        // Поддержка: латиница, кириллица, цифры, подчеркивания
                        boolean isValidNick = beforeColon.length() >= 2 && beforeColon.length() <= 16 
                            && beforeColon.matches("^[\\p{L}0-9_]+$"); // \\p{L} - любые буквы (включая русские)
                        System.out.println("[Bridge Filter] isValidNick: " + isValidNick);
                        
                        if (isValidNick) {
                            // Есть ник, форматируем с префиксом и ником
                            result = sourceColor + "[Minecraft] §f" + rest; // §f = белый цвет для текста
                            System.out.println("[Bridge Filter] Форматируем как Minecraft с ником: " + result);
                        } else {
                            // Это команда (невалидный ник), но все равно помечаем как Minecraft
                            result = sourceColor + "[Minecraft] §f" + rest;
                            System.out.println("[Bridge Filter] Это команда, но помечаем как Minecraft: " + result);
                        }
                    } else {
                        // Пустой beforeColon, это команда, но помечаем как Minecraft
                        result = sourceColor + "[Minecraft] §f" + rest;
                        System.out.println("[Bridge Filter] Пустой beforeColon, команда, но помечаем как Minecraft: " + result);
                    }
                } else {
                    // Нет двоеточия, это команда, но помечаем как Minecraft
                    result = sourceColor + "[Minecraft] §f" + rest;
                    System.out.println("[Bridge Filter] Нет двоеточия, команда, но помечаем как Minecraft: " + result);
                }
            }
        } 
        // Проверяем Telegram (начинается с [TG] или [tg] в любом регистре)
        else if (lowerBody != null && lowerBody.startsWith("[tg]") && body.length() >= 4) {
            String rest = body.substring(4).trim(); // убираем "[TG]" (4 символа)
            sourceColor = "§b"; // Голубой цвет для Telegram
            // Для Telegram всегда форматируем как [Telegram], не проверяем команды
            result = sourceColor + "[Telegram] §f" + rest; // §f = белый цвет для текста
        } 
        // Discord по умолчанию (если не Minecraft и не Telegram)
        else if (body != null && !body.isEmpty()) {
            sourceColor = "§9"; // Синий цвет для Discord
            
            // Проверяем наличие символов стрелок (⇾, →, ➜, ->) - это тоже Discord сообщения (цитирование)
            boolean hasArrow = body.contains("⇾") || body.contains("→") || body.contains("➜") || body.contains("->");
            
            // УПРОЩЕННАЯ ЛОГИКА: Если есть двоеточие и текст после него, это Discord сообщение
            // (если это не команда с 's)
            int colonIdx = body.indexOf(':');
            if (colonIdx > 0 && colonIdx < body.length()) {
                String beforeColon = body.substring(0, colonIdx).trim();
                String afterColon = body.substring(colonIdx + 1).trim();
                
                // УБИРАЕМ УРОВЕНЬ из beforeColon для проверки
                String cleanedBeforeColon = beforeColon.replaceAll("\\s*\\[[^\\]]*\\]\\s*", "").trim();
                
                // Обрабатываем цитирование: "Nick->Other" или "Nick ⇾ Other"
                boolean hasArrowBeforeColon = beforeColon.contains("->") || beforeColon.contains("⇾") || beforeColon.contains("→") || beforeColon.contains("➜");
                if (hasArrowBeforeColon) {
                    String[] parts = beforeColon.split("(->|⇾|→|➜)", 2);
                    if (parts.length > 0) {
                        cleanedBeforeColon = parts[0].trim().replaceAll("\\s*\\[[^\\]]*\\]\\s*", "").trim();
                    }
                }
                
                // Если есть текст после двоеточия, это Discord сообщение
                if (afterColon != null && !afterColon.isEmpty()) {
                    // Исключение: если это команда с 's (например: "Nayokage's networth")
                    if (cleanedBeforeColon != null && (cleanedBeforeColon.contains("'s") || cleanedBeforeColon.contains("'S"))) {
                        // Это команда, но если есть стрелка - все равно помечаем как Discord
                        if (hasArrow || hasArrowBeforeColon) {
                            result = sourceColor + "[Discord] §f" + body;
                        } else {
                            result = body; // Команда без префикса
                        }
                    } 
                    // Если есть стрелка (цитирование), всегда помечаем как Discord
                    else if (hasArrow || hasArrowBeforeColon) {
                        result = sourceColor + "[Discord] §f" + body;
                    }
                    // Если перед двоеточием есть хотя бы один символ (не пустое), это Discord ник (включая русские)
                    else if (cleanedBeforeColon != null && !cleanedBeforeColon.isEmpty() && cleanedBeforeColon.length() > 0) {
                        // Это Discord сообщение с ником (включая русские ники и ники с уровнями)
                        result = sourceColor + "[Discord] §f" + body;
                        System.out.println("[Bridge Filter] Discord: обнаружено сообщение (включая русский ник): " + cleanedBeforeColon + " -> " + afterColon.substring(0, Math.min(20, afterColon.length())));
                    } 
                    else {
                        // Пустой ник перед двоеточием, но есть текст после - это команда
                        result = body; // Команда без префикса
                    }
                } else {
                    // Нет текста после двоеточия - это команда
                    result = body; // Команда без префикса
                }
            } else {
                // Нет двоеточия, но если есть стрелка - помечаем как Discord
                if (hasArrow) {
                    result = sourceColor + "[Discord] §f" + body;
                } else {
                    result = body; // Команда без префикса
                }
            }
        }

        // Команды без префикса получают нейтральный серый цвет (§7) вместо зеленого
        // Это исправляет проблему, когда команды получают неправильный зеленый цвет
        if (result != null && !result.startsWith("§")) {
            // Команды без префикса получают серый цвет вместо цвета из оригинального сообщения
            result = "§7" + result;
        } else if (result != null && result.startsWith("§a") && !result.contains("[Minecraft]") && !result.contains("[Telegram]") && !result.contains("[Discord]")) {
            // Если команда случайно получила зеленый цвет (§a), но не имеет префикса, заменяем на серый
            result = "§7" + result.replaceFirst("^§a+", "");
        }
        
        // Защита от null
        if (result == null) {
            result = body != null ? body : "";
        }
        
        System.out.println("[Bridge Filter] GuildBridge: " + fullGuildMessage);
        System.out.println("[Bridge Filter] Body: '" + body + "' -> Result: '" + result + "'");
        System.out.println("[Bridge Filter] Result starts with §: " + (result != null && result.startsWith("§")));
        return result;
    }
}