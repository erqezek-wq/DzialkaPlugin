package pl.dzialka;

import org.bukkit.plugin.java.JavaPlugin;
import pl.dzialka.commands.DzialkaCommand;
import pl.dzialka.commands.LocationCommand;
import pl.dzialka.listeners.BlockListener;
import pl.dzialka.listeners.InteractListener;
import pl.dzialka.listeners.NoteBlockListener;
import pl.dzialka.managers.DzialkaManager;
import pl.dzialka.managers.VisualizationManager;
import pl.dzialka.utils.ConfigManager;

public class DzialkaPlugin extends JavaPlugin {

    private static DzialkaPlugin instance;
    private DzialkaManager dzialkaManager;
    private VisualizationManager visualizationManager;
    private ConfigManager configManager;
    private boolean worldGuardEnabled = false;

    @Override
    public void onEnable() {
        instance = this;

        // Sprawdź WorldGuard
        if (getServer().getPluginManager().getPlugin("WorldGuard") != null) {
            worldGuardEnabled = true;
            getLogger().info("WorldGuard wykryty - integracja aktywna!");
        } else {
            getLogger().warning("WorldGuard nie wykryty - ochrona regionów WG wyłączona.");
        }

        // Inicjalizacja
        saveDefaultConfig();
        configManager = new ConfigManager(this);
        dzialkaManager = new DzialkaManager(this);
        visualizationManager = new VisualizationManager(this);

        // Komendy
        getCommand("dzialka").setExecutor(new DzialkaCommand(this));
        getCommand("dzialka").setTabCompleter(new DzialkaCommand(this));
        getCommand("location").setExecutor(new LocationCommand(this));

        // Listenery
        getServer().getPluginManager().registerEvents(new NoteBlockListener(this), this);
        getServer().getPluginManager().registerEvents(new BlockListener(this), this);
        getServer().getPluginManager().registerEvents(new InteractListener(this), this);

        getLogger().info("DzialkaPlugin włączony!");
    }

    @Override
    public void onDisable() {
        if (dzialkaManager != null) {
            dzialkaManager.saveAll();
        }
        getLogger().info("DzialkaPlugin wyłączony!");
    }

    public static DzialkaPlugin getInstance() { return instance; }
    public DzialkaManager getDzialkaManager() { return dzialkaManager; }
    public VisualizationManager getVisualizationManager() { return visualizationManager; }
    public ConfigManager getConfigManager() { return configManager; }
    public boolean isWorldGuardEnabled() { return worldGuardEnabled; }
}
