package com.Bridge.ButtonMenu;

import me.shedaniel.clothconfig2.api.ConfigBuilder;
import me.shedaniel.clothconfig2.api.ConfigCategory;
import me.shedaniel.clothconfig2.api.ConfigEntryBuilder;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;

public class ConfigGUI {
    
    public static Screen openConfigScreen(Screen parent) {
        ConfigBuilder builder = ConfigBuilder.create()
            .setParentScreen(parent)
            .setTitle(Component.literal("Bridge Filter Settings"))
            .setSavingRunnable(() -> {
                BridgeFilterConfig.save();
            });
        
        ConfigEntryBuilder entryBuilder = builder.entryBuilder();
        BridgeFilterConfig config = BridgeFilterConfig.getInstance();
        
        // General category
        ConfigCategory general = builder.getOrCreateCategory(Component.literal("Общее"));
        
        general.addEntry(entryBuilder.startBooleanToggle(
            Component.literal("Включить фильтр"),
            config.filterEnabled
        ).setDefaultValue(true)
        .setTooltip(Component.literal("Включить/выключить фильтрацию сообщений"))
        .setSaveConsumer(value -> config.filterEnabled = value)
        .build());
        
        general.addEntry(entryBuilder.startBooleanToggle(
            Component.literal("Красное выделение заблокированных ников"),
            config.redHighlight
        ).setDefaultValue(false)
        .setTooltip(Component.literal("Красное выделение заблокированных ников (legacy функция)"))
        .setSaveConsumer(value -> config.redHighlight = value)
        .build());
        
        general.addEntry(entryBuilder.startBooleanToggle(
            Component.literal("Подсвечивать ваш ник"),
            config.nickHighlightEnabled
        ).setDefaultValue(true)
        .setTooltip(Component.literal("Автоматически подсвечивать ваш ник в сообщениях чата"))
        .setSaveConsumer(value -> config.nickHighlightEnabled = value)
        .build());
        
        general.addEntry(entryBuilder.startStrField(
            Component.literal("Цвет подсветки ника (yellow, red, green, ...)"),
            config.nickHighlightColor
        ).setDefaultValue("yellow")
        .setTooltip(Component.literal("Цвет подсветки ника: yellow, red, green, blue, cyan, pink, orange, white, black"))
        .setSaveConsumer(value -> config.nickHighlightColor = value)
        .build());
        
        general.addEntry(entryBuilder.startBooleanToggle(
            Component.literal("Форматирование Guild Bridge-сообщений"),
            config.guildBridgeFormatEnabled
        ).setDefaultValue(true)
        .setTooltip(Component.literal("Автоматически форматировать сообщения от Bridge ботов"))
        .setSaveConsumer(value -> config.guildBridgeFormatEnabled = value)
        .build());
        
        // Blocklist category
        ConfigCategory blocklist = builder.getOrCreateCategory(Component.literal("Блоклист"));
        
        List<String> blockListCopy = new ArrayList<>(config.getBlockList());
        
        blocklist.addEntry(entryBuilder.startStrList(
            Component.literal("Список заблокированных ников"),
            blockListCopy
        ).setDefaultValue(new ArrayList<>())
        .setTooltip(Component.literal("Список ников или слов для блокировки в сообщениях"))
        .setSaveConsumer(value -> config.setBlockList(value))
        .build());
        
        return builder.build();
    }
}
