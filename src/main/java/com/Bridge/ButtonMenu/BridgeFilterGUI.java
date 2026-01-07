// BridgeFilterGUI.java
package com.Bridge.ButtonMenu;

import net.minecraft.client.gui.*;
import net.minecraft.util.ChatComponentText;
import org.lwjgl.input.Keyboard;

import java.io.IOException;

public class BridgeFilterGUI extends GuiScreen {

    private GuiTextField textField;
    private int selectedCategory = 0;

    @Override
    public void initGui() {
        buttonList.clear();

        addCategoryButton(0, "Фильтр сообщений");
        addCategoryButton(1, "Формат чата");
        addCategoryButton(2, "Guild Bridge");
        addCategoryButton(3, "Инфо (скоро)");

        if (selectedCategory == 0) buildFilterPage();
        else if (selectedCategory == 1) buildFormatPage();
        else if (selectedCategory == 2) buildGuildBridgePage();

        // Кнопка проверки обновлений
        String updateText = UpdateChecker.updateAvailable ? "§aОбновление доступно!" : "Проверить обновления";
        buttonList.add(new GuiButton(500, width - 250, height - 35, 120, 20, updateText));
        
        buttonList.add(new GuiButton(999, width - 130, height - 35, 110, 20, "Закрыть"));
    }

    private void addCategoryButton(int id, String name) {
        int y = 60 + id * 40;
        String prefix = (id == selectedCategory) ? "§f> " : "§7";
        buttonList.add(new GuiButton(100 + id, 20, y, 160, 30, prefix + name));
    }

    private void buildFilterPage() {
        int left = 250, pad = 20, botX = left + pad, botY = 70;

        buttonList.add(new GuiButton(10, botX, botY, 200, 24, "§lБот: §b" + capitalize(BridgeFilterConfig.selectedBot)));

        int inputY = botY + 40;
        if (textField == null) {
            textField = new GuiTextField(0, fontRendererObj, botX, inputY, 200, 20);
            textField.setMaxStringLength(50);
        } else {
            textField.xPosition = botX;
            textField.yPosition = inputY;
            textField.width = 200;
        }
        buttonList.add(new GuiButton(50, botX + 210, inputY, 40, 20, "§a+"));

        int toggleY = inputY + 40;
        buttonList.add(new GuiButton(60, botX, toggleY, 250, 28,
                BridgeFilterConfig.filterEnabled ? "§aФильтр: ВКЛ" : "§cФильтр: ВЫКЛ"));
        buttonList.add(new GuiButton(61, botX, toggleY + 36, 250, 28,
                BridgeFilterConfig.redHighlight ? "§aКрасная подсветка: ВКЛ" : "§cКрасная подсветка: ВЫКЛ"));

        int listX = botX + 280, listY = botY - 10;
        for (int i = 0; i < BridgeFilterConfig.blockList.size() && i < 14; i++) {
            String word = BridgeFilterConfig.blockList.get(i);
            int y = listY + i * 30;
            buttonList.add(new GuiButton(200 + i, listX, y, 180, 26, "§c" + word));
            buttonList.add(new GuiButton(300 + i, listX + 188, y + 3, 22, 22, "§cX"));
        }
    }

    private void buildFormatPage() {
        int c = width / 2 - 100;

        buttonList.add(new GuiButton(1000, c, 80, 200, 28,
                BridgeFilterConfig.nickHighlightEnabled ? "§aПодсветка ника: ВКЛ" : "§cПодсветка ника: ВЫКЛ"));

        String[] names = {"§eЖёлтый", "§cКрасный", "§aЗелёный", "§bГолубой", "§dРозовый", "§6Оранжевый", "§fБелый", "§0Чёрный"};
        String[] codes = {"§e", "§c", "§a", "§b", "§d", "§6", "§f", "§0"};

        for (int i = 0; i < 8; i++) {
            int y = 130 + i * 36;
            String prefix = BridgeFilterConfig.nickHighlightColor.equals(codes[i]) ? "§f> " : "§7";
            buttonList.add(new GuiButton(1100 + i, c, y, 200, 32, prefix + names[i]));
        }
    }

    private void buildGuildBridgePage() {
        int c = width / 2 - 100;

        buttonList.add(new GuiButton(2000, c, 80, 200, 28,
                BridgeFilterConfig.guildBridgeFormatEnabled ? "§aФормат Guild: ВКЛ" : "§cФормат Guild: ВЫКЛ"));
    }

    @Override
    protected void actionPerformed(GuiButton button) {
        if (button.id >= 100 && button.id < 200) {
            selectedCategory = button.id - 100;
            textField = null;
            initGui();
            return;
        }

        if (selectedCategory == 0) {
            if (button.id == 10) {
                String[] bots = {"etobridge", "koorikage", "mothikh", "tenokage"};
                int idx = java.util.Arrays.asList(bots).indexOf(BridgeFilterConfig.selectedBot);
                BridgeFilterConfig.selectedBot = bots[(idx + 1) % 4];
                initGui();
            }
            if (button.id == 50 && textField != null) {
                String txt = textField.getText().trim();
                if (!txt.isEmpty() && !BridgeFilterConfig.blockList.contains(txt)) {
                    BridgeFilterConfig.blockList.add(txt);
                    textField.setText("");
                    initGui();
                }
            }
            if (button.id == 60) { BridgeFilterConfig.filterEnabled = !BridgeFilterConfig.filterEnabled; initGui(); }
            if (button.id == 61) { BridgeFilterConfig.redHighlight = !BridgeFilterConfig.redHighlight; initGui(); }
            if (button.id >= 300 && button.id < 400) {
                BridgeFilterConfig.blockList.remove(button.id - 300);
                initGui();
            }
        }

        if (selectedCategory == 1) {
            if (button.id == 1000) {
                BridgeFilterConfig.nickHighlightEnabled = !BridgeFilterConfig.nickHighlightEnabled;
                initGui();
            }
            if (button.id >= 1100 && button.id < 1200) {
                String[] codes = {"§e", "§c", "§a", "§b", "§d", "§6", "§f", "§0"};
                BridgeFilterConfig.nickHighlightColor = codes[button.id - 1100];
                initGui();
            }
        }

        if (selectedCategory == 2) {
            if (button.id == 2000) {
                BridgeFilterConfig.guildBridgeFormatEnabled = !BridgeFilterConfig.guildBridgeFormatEnabled;
                initGui();
            }
        }

        // Обработка кнопки обновлений
        if (button.id == 500) {
            if (UpdateChecker.updateAvailable && UpdateChecker.latestUpdate != null) {
                mc.displayGuiScreen(new UpdateGUI(UpdateChecker.latestUpdate));
            } else {
                UpdateChecker.checkForUpdates();
                // Показываем сообщение о проверке
                if (mc.thePlayer != null) {
                    mc.thePlayer.addChatMessage(new ChatComponentText("§9[Bridge Filter] §7Проверяем обновления..."));
                }
            }
        }

        if (button.id == 999) mc.displayGuiScreen(null);
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        drawRect(0, 0, width, height, 0xAA000000);
        drawRect(10, 10, width - 10, height - 10, 0xFF1E1E1E);
        drawCenteredString(fontRendererObj, "§9§lBridge Filter §8v1.0.5", width / 2, 20, 0x00FFFF);

        drawRect(15, 45, 195, height - 15, 0xFF252525);
        drawRect(210, 45, width - 15, height - 15, 0xFF252525);
        
        // Индикатор обновления в заголовке
        if (UpdateChecker.updateAvailable) {
            drawString(fontRendererObj, "§a●", width / 2 + 80, 22, 0x00FF00);
        }

        for (Object o : buttonList) {
            GuiButton b = (GuiButton) o;
            boolean hovered = mouseX >= b.xPosition && mouseX <= b.xPosition + b.width &&
                    mouseY >= b.yPosition && mouseY <= b.yPosition + b.height;

            drawRect(b.xPosition, b.yPosition, b.xPosition + b.width, b.yPosition + b.height,
                    hovered ? 0xFF3A3A3A : 0xFF2D2D2D);

            int border = hovered ? 0xFF00AAFF : 0xFF444444;
            drawRect(b.xPosition, b.yPosition, b.xPosition + b.width, b.yPosition + 1, border);
            drawRect(b.xPosition, b.yPosition + b.height - 1, b.xPosition + b.width, b.yPosition + b.height, border);
            drawRect(b.xPosition, b.yPosition, b.xPosition + 1, b.yPosition + b.height, border);
            drawRect(b.xPosition + b.width - 1, b.yPosition, b.xPosition + b.width, b.yPosition + b.height, border);

            String text = b.displayString;
            String clean = text.replaceAll("§[0-9a-fk-or]", "");
            int color = 0xFFFFFF;
            if (text.contains("ВКЛ")) color = 0x55FF55;
            if (text.contains("ВЫКЛ")) color = 0xFF5555;
            if (text.startsWith("§c")) color = 0xFF5555;
            if (text.startsWith("§a")) color = 0x55FF55;
            if (text.startsWith("§b")) color = 0x55FFFF;
            if (text.startsWith("§f>")) color = 0xAAAAAA;

            fontRendererObj.drawStringWithShadow(text,
                    b.xPosition + b.width / 2 - fontRendererObj.getStringWidth(clean) / 2,
                    b.yPosition + (b.height - 8) / 2, color);
        }

        if (selectedCategory == 0 && textField != null) {
            drawRect(textField.xPosition - 1, textField.yPosition - 1,
                    textField.xPosition + textField.width + 1, textField.yPosition + textField.height + 1, 0xFF00AAFF);
            drawRect(textField.xPosition, textField.yPosition,
                    textField.xPosition + textField.width, textField.yPosition + textField.height, 0xFF252525);
            textField.drawTextBox();
        }

        // Информация о Guild Bridge форматировании
        if (selectedCategory == 2) {
            int infoX = 250;
            int infoY = 130;
            drawString(fontRendererObj, "§7Форматирует сообщения от:", infoX, infoY, 0xCCCCCC);
            infoY += 15;
            drawString(fontRendererObj, "§8• etobridge", infoX + 10, infoY, 0xAAAAAA);
            infoY += 12;
            drawString(fontRendererObj, "§8• koorikage", infoX + 10, infoY, 0xAAAAAA);
            infoY += 12;
            drawString(fontRendererObj, "§8• mothikh", infoX + 10, infoY, 0xAAAAAA);
            infoY += 12;
            drawString(fontRendererObj, "§8• tenokage", infoX + 10, infoY, 0xAAAAAA);
            infoY += 20;
            drawString(fontRendererObj, "§7Форматы:", infoX, infoY, 0xCCCCCC);
            infoY += 15;
            drawString(fontRendererObj, "§8• .Nick: текст → [Minecraft]", infoX + 10, infoY, 0xAAAAAA);
            infoY += 12;
            drawString(fontRendererObj, "§8• [TG] Nick: текст → [Telegram]", infoX + 10, infoY, 0xAAAAAA);
            infoY += 12;
            drawString(fontRendererObj, "§8• Nick: текст → [Discord]", infoX + 10, infoY, 0xAAAAAA);
        }

        drawString(fontRendererObj, "§7Правый Shift — открыть меню", width - 190, height - 25, 0x888888);
    }

    @Override
    protected void keyTyped(char c, int k) throws IOException {
        if (selectedCategory == 0 && textField != null) textField.textboxKeyTyped(c, k);
        if (k == 1 || k == Keyboard.KEY_RSHIFT) mc.displayGuiScreen(null);
    }

    @Override
    protected void mouseClicked(int x, int y, int b) throws IOException {
        super.mouseClicked(x, y, b);
        if (selectedCategory == 0 && textField != null) textField.mouseClicked(x, y, b);
    }

    private String capitalize(String s) {
        if (s == null || s.isEmpty()) return s;
        return Character.toUpperCase(s.charAt(0)) + s.substring(1);
    }
}