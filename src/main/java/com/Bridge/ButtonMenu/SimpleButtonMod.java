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

@Mod(modid = "bridgefilter",
        name = "Bridge Filter",
        version = "1.0.5",
        clientSideOnly = true,
        updateJSON = "https://raw.githubusercontent.com/Nayokage/BridgeFilter/main/update.json")

public class SimpleButtonMod {

    private static final int MENU_BUTTON_ID = 6969;
    private static boolean rShiftPressed = false;

    private static boolean updateChecked = false;
    private static int ticksInWorld = 0;
    
    @Mod.EventHandler
    public void init(FMLInitializationEvent event) {
        MinecraftForge.EVENT_BUS.register(this);
        
        // Удаляем файлы, помеченные для удаления (оставшиеся с предыдущих обновлений)
        new Thread(() -> {
            try {
                Thread.sleep(2000); // Ждем 2 секунды после загрузки
                File mcDir = Minecraft.getMinecraft().mcDataDir;
                File modsDir = new File(mcDir, "mods");
                if (modsDir.exists()) {
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
                }
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

        String result;
        String lowerBody = body.toLowerCase().trim();
        body = body.trim(); // Убираем пробелы в начале
        
        // Проверяем Minecraft Bridge (начинается с точки)
        if (body.startsWith(".")) {
            String rest = body.substring(1).trim(); // убираем точку
            // Проверяем, есть ли ник (формат: .Nick: текст)
            // Ник должен быть перед двоеточием и не содержать пробелов/апострофов
            int colonIdx = rest.indexOf(':');
            if (colonIdx > 0 && colonIdx < 50 && !rest.substring(0, colonIdx).contains("'") && !rest.substring(0, colonIdx).contains(" ")) {
                // Есть ник, форматируем как обычно
                result = "[Minecraft] " + rest;
            } else {
                // Нет ника или это команда (есть апостроф/пробел перед :), просто текст без префикса
                result = rest;
            }
        } 
        // Проверяем Telegram (начинается с [TG] или [tg] в любом регистре)
        else if (lowerBody.startsWith("[tg]")) {
            String rest = body.substring(4).trim(); // убираем "[TG]" (4 символа)
            // Для Telegram всегда форматируем как [Telegram], не проверяем команды
            result = "[Telegram] " + rest;
        } 
        // Discord по умолчанию
        else {
            // Проверяем, это команда (есть 's перед :) или обычное сообщение
            int colonIdx = body.indexOf(':');
            if (colonIdx > 0 && colonIdx < 50) {
                String beforeColon = body.substring(0, colonIdx);
                // Если перед : есть 's, это команда (например: "Nayokage's networth")
                if (beforeColon.contains("'s") || beforeColon.contains("'S")) {
                    // Это команда, просто текст без префикса
                    result = body;
                } else if (!beforeColon.contains("'") && !beforeColon.contains(" ")) {
                    // Есть ник, форматируем как обычно
                    result = "[Discord] " + body;
                } else {
                    // Это команда или что-то другое, просто текст
                    result = body;
                }
            } else {
                // Нет двоеточия, просто текст (команда)
                result = body;
            }
        }

        // Сохраняем цветовые коды из оригинального сообщения
        // Извлекаем все цветовые коды из начала formatted сообщения (до текста "Guild >")
        String colorPrefix = "";
        if (formattedMessage != null && !formattedMessage.isEmpty()) {
            // Ищем все цветовые коды в начале сообщения
            Pattern colorPattern = Pattern.compile("^(§[0-9a-fk-or])+");
            Matcher colorMatcher = colorPattern.matcher(formattedMessage);
            if (colorMatcher.find()) {
                colorPrefix = colorMatcher.group();
            }
        }

        // Формируем итоговое сообщение с сохранением цветов
        String finalResult = colorPrefix + result;
        
        System.out.println("[Bridge Filter] GuildBridge: " + fullGuildMessage);
        System.out.println("[Bridge Filter] Body: '" + body + "' -> Result: '" + finalResult + "'");
        System.out.println("[Bridge Filter] Color prefix: '" + colorPrefix + "'");
        return finalResult;
    }
}