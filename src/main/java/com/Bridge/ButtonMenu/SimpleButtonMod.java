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
        version = "1.0.0",
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
            String lowerUnformatted = unformatted.toLowerCase();
            if (lowerUnformatted.startsWith("guild")) {
                String formattedGuildMsg = formatGuildBridgeMessage(unformatted, formatted);
                if (formattedGuildMsg != null) {
                    event.message = new ChatComponentText(formattedGuildMsg);
                    // Обновляем unformatted для дальнейшей обработки
                    unformatted = event.message.getUnformattedText();
                }
            }
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
     * Форматирует сообщения из Guild-чата от bridge-ботов
     * Удаляет префикс "Guild > [Ранг] НикБриджАккаунта[РангВГильдии]:" 
     * и форматирует по источнику (Minecraft/Telegram/Discord)
     * 
     * @param unformatted Неформатированный текст сообщения
     * @param formatted Форматированный текст с цветовыми кодами
     * @return Отформатированное сообщение или null, если не является bridge-сообщением
     */
    private String formatGuildBridgeMessage(String unformatted, String formatted) {
        // Список ботов для проверки
        String[] bots = {"etobridge", "koorikage", "mothikh", "tenokage"};
        
        // Проверяем, что сообщение от одного из bridge-ботов
        String lowerUnformatted = unformatted.toLowerCase();
        boolean isFromBridgeBot = false;
        String foundBot = null;
        for (String bot : bots) {
            if (lowerUnformatted.contains(bot.toLowerCase())) {
                isFromBridgeBot = true;
                foundBot = bot;
                break;
            }
        }
        
        if (!isFromBridgeBot) {
            return null; // Не от bridge-бота, не трогаем
        }
        
        // Проверяем, что это Guild-сообщение
        if (!lowerUnformatted.startsWith("guild")) {
            return null;
        }
        
        // Более гибкий regex для удаления префикса
        // Формат: "Guild > [Ранг] НикБот[РангВГильдии]:"
        // Примеры из логов:
        // "Guild > [VIP+] Koorikage []: ironmenlove: 8 "
        // "Guild > [VIP+] Koorikage []: : 123321"
        
        // Пробуем несколько вариантов паттернов
        Pattern[] patterns = {
            // Стандартный формат: Guild > [Ранг] Бот[Ранг]: текст
            Pattern.compile(
                "Guild\\s*>\\s*\\[.*?\\]\\s*" + Pattern.quote(foundBot) + "\\s*\\[.*?\\]\\s*:\\s*",
                Pattern.CASE_INSENSITIVE
            ),
            // Альтернативный: Guild > [Ранг] Бот: текст (без ранга в гильдии)
            Pattern.compile(
                "Guild\\s*>\\s*\\[.*?\\]\\s*" + Pattern.quote(foundBot) + "\\s*:\\s*",
                Pattern.CASE_INSENSITIVE
            ),
            // Ещё один вариант: любые символы между [Ранг] и Бот
            Pattern.compile(
                "Guild\\s*>\\s*\\[.*?\\]\\s+.*?" + Pattern.quote(foundBot) + ".*?:\\s*",
                Pattern.CASE_INSENSITIVE
            )
        };
        
        Matcher matcher = null;
        boolean found = false;
        for (Pattern pattern : patterns) {
            matcher = pattern.matcher(unformatted);
            if (matcher.find()) {
                found = true;
                break;
            }
        }
        
        if (!found || matcher == null) {
            System.out.println("[Bridge Filter] Не удалось найти префикс для: " + unformatted);
            return null;
        }
        
        // Получаем тело сообщения после удаления префикса
        String messageBody = unformatted.substring(matcher.end()).trim();
        
        if (messageBody.isEmpty()) {
            return null; // Пустое сообщение
        }
        
        // Определяем источник и форматируем
        String formattedBody;
        if (messageBody.startsWith(".")) {
            // Minecraft Bridge: .Nick: текст
            formattedBody = messageBody.substring(1).trim(); // Убираем точку
            formattedBody = "[Minecraft] " + formattedBody;
        } else if (messageBody.startsWith("[TG]")) {
            // Telegram: [TG] Nick: текст
            formattedBody = messageBody.substring(4).trim(); // Убираем "[TG]"
            formattedBody = "[Telegram] " + formattedBody;
        } else {
            // Discord (по умолчанию): Nick: текст или просто текст
            formattedBody = "[Discord] " + messageBody;
        }
        
        // Сохраняем цветовые коды из оригинального сообщения
        // Извлекаем цветовые коды из начала formatted сообщения
        String colorPrefix = "";
        Pattern colorPattern = Pattern.compile("^(§[0-9a-fk-or])+");
        Matcher colorMatcher = colorPattern.matcher(formatted);
        if (colorMatcher.find()) {
            colorPrefix = colorMatcher.group();
        }
        
        // Формируем итоговое сообщение с сохранением цветов
        String result = colorPrefix + formattedBody;
        System.out.println("[Bridge Filter] Форматировано: " + unformatted + " -> " + result);
        return result;
    }
}