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
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

@Mod(modid = "bridgefilter",
        name = "Bridge Filter",
        version = "1.0.7",
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
            
            // Удаляем старые версии BridgeFilter-*.jar (кроме BridgeFilter.jar)
            File[] oldVersions = modsDir.listFiles((dir, name) -> {
                String lowerName = name.toLowerCase();
                return lowerName.startsWith("bridgefilter") && lowerName.endsWith(".jar") 
                       && !name.equals("BridgeFilter.jar") && !name.endsWith(".tmp") && !name.endsWith(".delete");
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
        
        // Добавляем shutdown hook для удаления старых файлов при закрытии игры
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            System.out.println("[Bridge Filter] Игра закрывается, удаляем старые версии мода...");
            cleanupOldModFiles();
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
    
    @SubscribeEvent
    public void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.START) return;
        
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

        String unformatted = event.message.getUnformattedText();
        String formatted   = event.message.getFormattedText();
        String playerName  = Minecraft.getMinecraft().thePlayer.getName();

        // Форматирование Guild-сообщений от bridge-ботов
        if (BridgeFilterConfig.guildBridgeFormatEnabled) {
            if (unformatted.toLowerCase().contains("guild >")) {
                System.out.println("[Bridge Filter] onChat: обрабатываем Guild-сообщение: " + unformatted);
                String formattedGuildMsg = formatGuildBridgeMessage(unformatted, formatted);
                if (formattedGuildMsg != null) {
                    System.out.println("[Bridge Filter] onChat: форматирование применено!");
                    event.message = new ChatComponentText(formattedGuildMsg);
                    // Обновляем unformatted для дальнейшей обработки
                    unformatted = event.message.getUnformattedText();
                } else {
                    System.out.println("[Bridge Filter] onChat: форматирование НЕ применено (вернул null)");
                }
            }
        } else {
            System.out.println("[Bridge Filter] onChat: guildBridgeFormatEnabled = false");
        }

        if (BridgeFilterConfig.nickHighlightEnabled && unformatted.contains(playerName)) {
            String newMsg = formatted.replaceAll(
                    "(?i)" + Pattern.quote(playerName),
                    BridgeFilterConfig.nickHighlightColor + "§l" + playerName + "§r"
            );
            event.message = new ChatComponentText(newMsg);
        }

        if (!BridgeFilterConfig.filterEnabled) return;

        String lowerMsg = unformatted.toLowerCase();
        String bot = BridgeFilterConfig.selectedBot.toLowerCase();
        if (!lowerMsg.contains(bot)) return;

        for (String word : BridgeFilterConfig.blockList) {
            if (lowerMsg.contains(word.toLowerCase())) {
                event.setCanceled(true);
                if (BridgeFilterConfig.redHighlight) {
                    event.message = new ChatComponentText("§c" + formatted);
                }
                return;
            }
        }
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
        
        String[] bots = {"etobridge", "koorikage", "mothikh", "tenokage"};

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
        // Discord по умолчанию
        else if (body != null && !body.isEmpty()) {
            sourceColor = "§9"; // Синий цвет для Discord
            
            // Проверяем наличие символов стрелок (⇾, →, ➜) - это тоже Discord сообщения
            boolean hasArrow = body.contains("⇾") || body.contains("→") || body.contains("➜");
            
            // Проверяем, это команда (есть 's перед :) или обычное сообщение
            int colonIdx = body.indexOf(':');
            if (colonIdx > 0 && colonIdx < 50 && colonIdx < body.length()) {
                String beforeColon = body.substring(0, colonIdx).trim();
                if (beforeColon != null && !beforeColon.isEmpty()) {
                    // Если перед : есть 's, это команда (например: "Nayokage's networth")
                    if (beforeColon.contains("'s") || beforeColon.contains("'S")) {
                        // Это команда, но если есть стрелка - все равно помечаем как Discord
                        if (hasArrow) {
                            result = sourceColor + "[Discord] §f" + body;
                        } else {
                            result = body;
                        }
                    } else if (beforeColon.matches("^[\\p{L}0-9_]{2,16}$") && !beforeColon.contains("'") && !beforeColon.contains(" ")) {
                        // Есть валидный ник (включая русские буквы), форматируем с префиксом
                        result = sourceColor + "[Discord] §f" + body; // §f = белый цвет для текста
                    } else {
                        // Это команда или что-то другое, но если есть стрелка - помечаем как Discord
                        if (hasArrow) {
                            result = sourceColor + "[Discord] §f" + body;
                        } else {
                            result = body;
                        }
                    }
                } else {
                    // Пустой beforeColon, но если есть стрелка - помечаем как Discord
                    if (hasArrow) {
                        result = sourceColor + "[Discord] §f" + body;
                    } else {
                        result = body;
                    }
                }
            } else {
                // Нет двоеточия, но если есть стрелка - помечаем как Discord
                if (hasArrow) {
                    result = sourceColor + "[Discord] §f" + body;
                } else {
                    result = body;
                }
            }
        }

        // Если результат не содержит цветовых кодов (команды без префикса), добавляем цвет из оригинального сообщения
        if (result != null && !result.startsWith("§") && formattedMessage != null && !formattedMessage.isEmpty()) {
            try {
                // Извлекаем цветовые коды из начала formatted сообщения
                Pattern colorPattern = Pattern.compile("^(§[0-9a-fk-or])+");
                Matcher colorMatcher = colorPattern.matcher(formattedMessage);
                if (colorMatcher.find()) {
                    String colorPrefix = colorMatcher.group();
                    if (colorPrefix != null) {
                        result = colorPrefix + result;
                    }
                }
            } catch (Exception e) {
                System.out.println("[Bridge Filter] Ошибка при извлечении цветов: " + e.getMessage());
                // Продолжаем без цветов
            }
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