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
}