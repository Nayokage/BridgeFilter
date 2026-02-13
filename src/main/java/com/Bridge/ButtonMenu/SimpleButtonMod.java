package com.Bridge.ButtonMenu;

import com.Bridge.ButtonMenu.autoupdate.UpdateChecker;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;
import org.lwjgl.glfw.GLFW;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

public class SimpleButtonMod implements ClientModInitializer {
    private static final String[] BRIDGE_BOTS = {"etobridge", "koorikage", "mothikh", "tenokage", "uzbekF3ndi", "gem_zz"};
    private static final Pattern LEVEL_PATTERN = Pattern.compile("\\[\\d+\\]\\s*");
    private static final Pattern ARROW_PATTERN = Pattern.compile("[-⇾→➜]+\\s*");
    
    private static KeyMapping menuKey;
    private static boolean configLoaded = false;
    private static int ticksInWorld = 0;
    private static boolean updateChecked = false;
    
    @Override
    public void onInitializeClient() {
        // Register key binding (use Category enum in 1.21.11)
        menuKey = KeyBindingHelper.registerKeyBinding(new KeyMapping(
            "key.bridgefilter.menu",
            GLFW.GLFW_KEY_RIGHT_SHIFT,
            KeyMapping.Category.MISC
        ));
        
        // Load config in separate thread with delay
        new Thread(() -> {
            try {
                Thread.sleep(1000);
                BridgeFilterConfig.load();
                configLoaded = true;
            } catch (Exception e) {
                System.err.println("Failed to load config: " + e.getMessage());
                e.printStackTrace();
            }
        }).start();
        
        // Register shutdown hook
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            try {
                BridgeFilterConfig.saveSync();
                cleanupOldModFiles();
            } catch (Exception e) {
                System.err.println("Failed to save config on shutdown: " + e.getMessage());
                e.printStackTrace();
            }
        }));
        
        // Apply update from updates folder
        new Thread(() -> {
            try {
                Thread.sleep(2000);
                applyUpdateFromUpdatesFolder();
            } catch (Exception e) {
                System.err.println("Failed to apply update: " + e.getMessage());
                e.printStackTrace();
            }
        }).start();
        
        // Register client tick event
        ClientTickEvents.END_CLIENT_TICK.register(this::onClientTick);
    }
    
    private void onClientTick(Minecraft client) {
        if (client.player == null) {
            ticksInWorld = 0;
            updateChecked = false;
            return;
        }
        
        // Load config on first world entry
        if (!configLoaded) {
            BridgeFilterConfig.load();
            configLoaded = true;
        }
        
        ticksInWorld++;
        
        // Check for updates after 60 ticks (3 seconds)
        if (ticksInWorld == 60 && !updateChecked) {
            updateChecked = true;
            new Thread(() -> {
                try {
                    UpdateChecker.checkForUpdates();
                } catch (Exception e) {
                    System.err.println("Failed to check for updates: " + e.getMessage());
                    e.printStackTrace();
                }
            }).start();
        }
        
        // Handle menu key press
        while (menuKey.consumeClick()) {
            client.execute(() -> {
                if (client.screen == null) {
                    client.setScreen(ConfigGUI.openConfigScreen(null));
                }
            });
        }
    }
    
    public static Component processChatMessage(Component message, String unformatted) {
        if (message == null || unformatted == null) {
            return message;
        }
        
        BridgeFilterConfig config = BridgeFilterConfig.getInstance();
        Minecraft mc = Minecraft.getInstance();
        
        if (mc.player == null) {
            return message;
        }
        
        // Get player nick (original and current)
        String originalNick = mc.getUser().getName();
        String currentNick = mc.player.getName().getString();
        
        // Format Guild Bridge messages
        if (config.guildBridgeFormatEnabled) {
            message = formatGuildBridgeMessage(message, unformatted);
        }
        
        // Check filter
        if (config.filterEnabled) {
            if (shouldBlockMessage(unformatted, originalNick, currentNick)) {
                return null; // Block message
            }
        }
        
        // Highlight player nick
        if (config.nickHighlightEnabled) {
            message = highlightPlayerNick(message, unformatted, originalNick, currentNick);
        }
        
        return message;
    }
    
    private static Component formatGuildBridgeMessage(Component message, String unformatted) {
        if (!unformatted.toLowerCase().contains("guild >")) {
            return message;
        }
        
        // Check if message is from a Bridge bot
        boolean isFromBot = false;
        String botName = null;
        for (String bot : BRIDGE_BOTS) {
            if (unformatted.toLowerCase().contains(bot.toLowerCase())) {
                isFromBot = true;
                botName = bot;
                break;
            }
        }
        
        if (!isFromBot) {
            return message;
        }
        
        // Extract body (text after colon after bot name)
        String body = extractBody(unformatted, botName);
        if (body == null) {
            return message;
        }
        
        // Determine source
        String source;
        Component sourceComponent;
        if (body.startsWith(".")) {
            source = "Minecraft";
            sourceComponent = Component.literal("[Minecraft] ").withStyle(Style.EMPTY.withColor(TextColor.fromRgb(0x55FF55)));
        } else if (body.startsWith("[TG]")) {
            source = "Telegram";
            sourceComponent = Component.literal("[Telegram] ").withStyle(Style.EMPTY.withColor(TextColor.fromRgb(0x55FFFF)));
        } else {
            source = "Discord";
            sourceComponent = Component.literal("[Discord] ").withStyle(Style.EMPTY.withColor(TextColor.fromRgb(0x5555FF)));
        }
        
        // Remove source prefix from body
        if (body.startsWith(".")) {
            body = body.substring(1).trim();
        } else if (body.startsWith("[TG]")) {
            body = body.substring(4).trim();
        }
        
        // Create formatted message
        MutableComponent formatted = Component.empty()
            .append(sourceComponent)
            .append(Component.literal(body).withStyle(Style.EMPTY.withColor(TextColor.fromRgb(0xFFFFFF))));
        
        return formatted;
    }
    
    private static String extractBody(String unformatted, String botName) {
        // Find bot name in message (case insensitive)
        int botIndex = -1;
        String lowerUnformatted = unformatted.toLowerCase();
        String lowerBotName = botName.toLowerCase();
        
        // Try to find bot name with various patterns
        String[] patterns = {
            botName + ":",
            botName + " :",
            "[" + botName + "]",
            botName
        };
        
        for (String pattern : patterns) {
            int index = lowerUnformatted.indexOf(pattern.toLowerCase());
            if (index != -1) {
                botIndex = index + pattern.length();
                break;
            }
        }
        
        if (botIndex == -1) {
            return null;
        }
        
        // Find colon after bot name
        int colonIndex = unformatted.indexOf(':', botIndex);
        if (colonIndex == -1) {
            return null;
        }
        
        return unformatted.substring(colonIndex + 1).trim();
    }
    
    private static boolean shouldBlockMessage(String unformatted, String originalNick, String currentNick) {
        BridgeFilterConfig config = BridgeFilterConfig.getInstance();
        List<String> blockList = config.getBlockList();
        
        if (blockList.isEmpty()) {
            return false;
        }
        
        String lowerUnformatted = unformatted.toLowerCase();
        
        // Check if message is from a Bridge bot
        boolean isFromBot = false;
        String botName = null;
        for (String bot : BRIDGE_BOTS) {
            if (lowerUnformatted.contains(bot.toLowerCase())) {
                isFromBot = true;
                botName = bot;
                break;
            }
        }
        
        if (!isFromBot) {
            return false;
        }
        
        // Extract body
        String body = extractBody(unformatted, botName);
        if (body == null) {
            body = unformatted;
        }
        
        String lowerBody = body.toLowerCase();
        
        // Extract sender nick (before colon)
        String senderNick = extractSenderNick(unformatted, botName);
        if (senderNick != null) {
            String lowerSenderNick = senderNick.toLowerCase();
            String lowerSenderNickNoLevel = removeLevels(lowerSenderNick);
            String lowerSenderNickNoArrow = removeArrows(lowerSenderNickNoLevel);
            
            // Check block list
            for (String blocked : blockList) {
                String lowerBlocked = blocked.toLowerCase();
                
                // Check body
                if (lowerBody.contains(lowerBlocked)) {
                    return true;
                }
                
                // Check sender nick
                if (lowerSenderNick.contains(lowerBlocked) ||
                    lowerSenderNickNoLevel.contains(lowerBlocked) ||
                    lowerSenderNickNoArrow.contains(lowerBlocked)) {
                    return true;
                }
            }
        } else {
            // Check body only
            for (String blocked : blockList) {
                if (lowerBody.contains(blocked.toLowerCase())) {
                    return true;
                }
            }
        }
        
        return false;
    }
    
    private static String extractSenderNick(String unformatted, String botName) {
        // Find "guild >" pattern
        int guildIndex = unformatted.toLowerCase().indexOf("guild >");
        if (guildIndex == -1) {
            return null;
        }
        
        // Find text after "guild >" and before bot name or colon
        int startIndex = guildIndex + "guild >".length();
        int endIndex = unformatted.length();
        
        // Find bot name or colon
        int botIndex = unformatted.toLowerCase().indexOf(botName.toLowerCase(), startIndex);
        int colonIndex = unformatted.indexOf(':', startIndex);
        
        if (botIndex != -1 && botIndex < endIndex) {
            endIndex = botIndex;
        }
        if (colonIndex != -1 && colonIndex < endIndex) {
            endIndex = colonIndex;
        }
        
        if (startIndex >= endIndex) {
            return null;
        }
        
        String nick = unformatted.substring(startIndex, endIndex).trim();
        return nick.isEmpty() ? null : nick;
    }
    
    private static String removeLevels(String text) {
        return LEVEL_PATTERN.matcher(text).replaceAll("");
    }
    
    private static String removeArrows(String text) {
        return ARROW_PATTERN.matcher(text).replaceAll("");
    }
    
    private static Component highlightPlayerNick(Component message, String unformatted, String originalNick, String currentNick) {
        String lowerUnformatted = unformatted.toLowerCase();
        String lowerOriginalNick = originalNick.toLowerCase();
        String lowerCurrentNick = currentNick.toLowerCase();
        
        // Check if message contains player nick
        boolean containsOriginal = lowerUnformatted.contains(lowerOriginalNick);
        boolean containsCurrent = !lowerCurrentNick.equals(lowerOriginalNick) && lowerUnformatted.contains(lowerCurrentNick);
        
        if (!containsOriginal && !containsCurrent) {
            return message;
        }
        
        BridgeFilterConfig config = BridgeFilterConfig.getInstance();
        String colorName = config.nickHighlightColor.toLowerCase();
        
        // Convert color name to color code
        int color = getColorFromName(colorName);
        
        // Get the nick to highlight
        String nickToHighlight = containsCurrent ? currentNick : originalNick;
        String lowerNickToHighlight = nickToHighlight.toLowerCase();
        
        // Find and replace nick in message using Component visitor
        return highlightNickInComponent(message, nickToHighlight, lowerNickToHighlight, color);
    }
    
    private static Component highlightNickInComponent(Component component, String nick, String lowerNick, int color) {
        // Get full text including siblings
        String fullText = component.getString();
        String lowerFullText = fullText.toLowerCase();
        
        int index = lowerFullText.indexOf(lowerNick);
        if (index == -1) {
            return component;
        }
        
        // Simple approach: rebuild component with highlighted nick
        // This preserves basic structure but may lose some formatting
        MutableComponent result = Component.empty();
        
        // Add part before nick
        if (index > 0) {
            String before = fullText.substring(0, index);
            result.append(Component.literal(before).withStyle(component.getStyle()));
        }
        
        // Add highlighted nick
        String nickText = fullText.substring(index, index + nick.length());
        result.append(Component.literal(nickText)
            .withStyle(Style.EMPTY
                .withColor(TextColor.fromRgb(color))
                .withBold(true)));
        
        // Add part after nick
        if (index + nick.length() < fullText.length()) {
            String after = fullText.substring(index + nick.length());
            result.append(Component.literal(after).withStyle(component.getStyle()));
        }
        
        return result;
    }
    
    private static int getColorFromName(String colorName) {
        return switch (colorName) {
            case "yellow" -> 0xFFFF00;
            case "red" -> 0xFF0000;
            case "green" -> 0x00FF00;
            case "blue" -> 0x0000FF;
            case "cyan" -> 0x00FFFF;
            case "pink" -> 0xFF00FF;
            case "orange" -> 0xFFA500;
            case "white" -> 0xFFFFFF;
            case "black" -> 0x000000;
            default -> 0xFFFF00; // Default to yellow
        };
    }
    
    private static void cleanupOldModFiles() {
        try {
            File modsDir = FabricLoader.getInstance().getGameDir().resolve("mods").toFile();
            if (!modsDir.exists()) {
                return;
            }
            
            File[] files = modsDir.listFiles();
            if (files == null) {
                return;
            }
            
            for (File file : files) {
                String name = file.getName();
                
                // Delete old BridgeFilter versions (except current and new)
                if (name.startsWith("BridgeFilter-") && name.endsWith(".jar") &&
                    !name.equals("BridgeFilter.jar") && !name.equals("BridgeFilter-new.jar")) {
                    try {
                        file.delete();
                    } catch (Exception e) {
                        System.err.println("Failed to delete old mod file: " + name);
                    }
                }
                
                // Delete .delete marker files
                if (name.endsWith(".delete")) {
                    try {
                        file.delete();
                    } catch (Exception e) {
                        System.err.println("Failed to delete marker file: " + name);
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("Failed to cleanup old mod files: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    private static void applyUpdateFromUpdatesFolder() {
        try {
            File updatesDir = FabricLoader.getInstance().getGameDir().resolve("mods").resolve("updates").toFile();
            File updateFile = new File(updatesDir, "BridgeFilter.jar");
            
            if (!updateFile.exists()) {
                return;
            }
            
            File modsDir = FabricLoader.getInstance().getGameDir().resolve("mods").toFile();
            File targetFile = new File(modsDir, "BridgeFilter.jar");
            
            // Copy update file to mods directory
            Files.copy(updateFile.toPath(), targetFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
            
            // Delete update file
            updateFile.delete();
            
            System.out.println("Bridge Filter: Update applied successfully");
        } catch (Exception e) {
            System.err.println("Failed to apply update: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
