package pl.dzialka.utils;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import pl.dzialka.DzialkaPlugin;

public class ConfigManager {

    private final DzialkaPlugin plugin;

    public ConfigManager(DzialkaPlugin plugin) {
        this.plugin = plugin;
    }

    public String getMessage(String key) {
        String msg = plugin.getConfig().getString("messages." + key, "&cBrak wiadomości: " + key);
        return colorize(msg);
    }

    public String getMessage(String key, String... replacements) {
        String msg = getMessage(key);
        for (int i = 0; i + 1 < replacements.length; i += 2) {
            msg = msg.replace(replacements[i], replacements[i + 1]);
        }
        return msg;
    }

    public Component getMessageComponent(String key) {
        return LegacyComponentSerializer.legacyAmpersand().deserialize(getMessage(key));
    }

    private String colorize(String msg) {
        return msg.replace("&", "§");
    }

    public String getPrefix() {
        return getMessage("prefix");
    }

    public int getDefaultLimit() {
        return plugin.getConfig().getInt("settings.default_limit", 3);
    }

    public int getVipLimit() {
        return plugin.getConfig().getInt("settings.vip_limit", 6);
    }

    public int getSvipLimit() {
        return plugin.getConfig().getInt("settings.svip_limit", 10);
    }

    public int getPlotSize() {
        return plugin.getConfig().getInt("settings.plot_size", 50);
    }
}
