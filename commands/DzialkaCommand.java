package pl.dzialka.commands;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import pl.dzialka.DzialkaPlugin;
import pl.dzialka.managers.DzialkaManager;
import pl.dzialka.models.Dzialka;
import pl.dzialka.utils.ConfigManager;

import java.util.*;
import java.util.stream.Collectors;

public class DzialkaCommand implements CommandExecutor, TabCompleter {

    private final DzialkaPlugin plugin;
    private final DzialkaManager manager;
    private final ConfigManager cfg;

    public DzialkaCommand(DzialkaPlugin plugin) {
        this.plugin = plugin;
        this.manager = plugin.getDzialkaManager();
        this.cfg = plugin.getConfigManager();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Ta komenda jest tylko dla graczy!");
            return true;
        }

        if (args.length == 0) {
            sendHelp(player);
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "help" -> sendHelp(player);
            case "list" -> handleList(player);
            case "info" -> handleInfo(player, args);
            case "remove" -> handleRemove(player, args);
            case "addmember" -> handleAddMember(player, args);
            case "removemember" -> handleRemoveMember(player, args);
            case "members" -> handleMembers(player, args);
            case "pvp" -> handlePvP(player, args);
            case "show" -> handleShow(player, args);
            case "rename" -> handleRename(player, args);
            default -> {
                player.sendMessage(cfg.getPrefix() + "§cNieznana komenda. Użyj §e/dzialka help");
            }
        }

        return true;
    }

    // ==================== HELP ====================

    private void sendHelp(Player player) {
        String prefix = cfg.getPrefix();
        player.sendMessage("§8§m----------------------------------------");
        player.sendMessage("§6§lDzialkaPlugin §7- §fSpis komend");
        player.sendMessage("§8§m----------------------------------------");
        player.sendMessage("§e/dzialka list §7- Lista twoich działek");
        player.sendMessage("§e/dzialka info <nazwa> §7- Informacje o działce");
        player.sendMessage("§e/dzialka remove <nazwa> §7- Usuń działkę");
        player.sendMessage("§e/dzialka rename <nazwa> <nowa_nazwa> §7- Zmień nazwę");
        player.sendMessage("§e/dzialka addmember <nazwa_działki> <gracz> §7- Dodaj członka");
        player.sendMessage("§e/dzialka removemember <nazwa_działki> <gracz> §7- Usuń członka");
        player.sendMessage("§e/dzialka members <nazwa_działki> §7- Lista członków");
        player.sendMessage("§e/dzialka pvp <nazwa_działki> §7- Włącz/wyłącz PvP");
        player.sendMessage("§e/dzialka show <nazwa_działki> §7- Wizualizacja granic");
        player.sendMessage("§e/location §7- Lista lokalizacji twoich działek");
        player.sendMessage("§7§oAby stworzyć działkę: postaw Noteblock bez shifta!");
        player.sendMessage("§8§m----------------------------------------");
        player.sendMessage("§7Limit działek: §e" + manager.getPlayerLimit(player) +
                " §7| Aktualne: §e" + manager.getPlayerDzialki(player.getUniqueId()).size());
        player.sendMessage("§8§m----------------------------------------");
    }

    // ==================== LIST ====================

    private void handleList(Player player) {
        List<Dzialka> dzialki = manager.getPlayerDzialki(player.getUniqueId());

        if (dzialki.isEmpty()) {
            player.sendMessage(cfg.getPrefix() + cfg.getMessage("no_plots"));
            return;
        }

        player.sendMessage("§8§m----&r §6Twoje działki §8§m----".replace("&r", "§r"));
        for (Dzialka d : dzialki) {
            String pvp = d.isPvpEnabled() ? "§c✔" : "§a✘";
            player.sendMessage(
                "§e" + d.getName() + " §8[§7ID: §f" + d.getId() + "§8]" +
                " §7| PvP: " + pvp +
                " §7| Członkowie: §e" + d.getMembers().size() +
                " §7| X:§e" + d.getCenterX() + " §7Z:§e" + d.getCenterZ()
            );
        }
        player.sendMessage("§7Łącznie: §e" + dzialki.size() + "§7/§e" + manager.getPlayerLimit(player));
    }

    // ==================== INFO ====================

    private void handleInfo(Player player, String[] args) {
        if (args.length < 2) {
            player.sendMessage(cfg.getPrefix() + "§cUżycie: §e/dzialka info <nazwa>");
            return;
        }

        Dzialka d = manager.getDzialkaByIdOrName(player.getUniqueId(), args[1]);

        if (d == null) {
            // Sprawdź czy admin i szukaj po wszystkich
            if (player.hasPermission("dzialka.admin")) {
                d = plugin.getDzialkaManager().getAllDzialki().stream()
                        .filter(dz -> dz.getName().equalsIgnoreCase(args[1]) || dz.getId().equals(args[1]))
                        .findFirst().orElse(null);
            }
            if (d == null) {
                player.sendMessage(cfg.getPrefix() + cfg.getMessage("dzialka_not_found")
                        .replace("{name}", args[1]));
                return;
            }
        }

        player.sendMessage("§8§m----§r §6Informacje o działce §8§m----");
        player.sendMessage("§7Nazwa: §e" + d.getName());
        player.sendMessage("§7ID: §f" + d.getId());
        player.sendMessage("§7Właściciel: §e" + d.getOwnerName());
        player.sendMessage("§7Świat: §e" + d.getWorldName());
        player.sendMessage("§7Centrum: §eX:" + d.getCenterX() + " Z:" + d.getCenterZ());
        player.sendMessage("§7Granice: §eX[" + d.getMinX() + " do " + d.getMaxX() +
                "] Z[" + d.getMinZ() + " do " + d.getMaxZ() + "]");
        player.sendMessage("§7PvP: " + (d.isPvpEnabled() ? "§cWłączone" : "§aWyłączone"));
        player.sendMessage("§7Członkowie (" + d.getMembers().size() + "):");

        if (d.getMembers().isEmpty()) {
            player.sendMessage("  §7Brak członków");
        } else {
            StringBuilder members = new StringBuilder("  §e");
            for (UUID uid : d.getMembers()) {
                OfflinePlayer op = Bukkit.getOfflinePlayer(uid);
                members.append(op.getName() != null ? op.getName() : uid.toString()).append("§7, §e");
            }
            String memberStr = members.toString();
            if (memberStr.endsWith("§7, §e")) {
                memberStr = memberStr.substring(0, memberStr.length() - 6);
            }
            player.sendMessage(memberStr);
        }
    }

    // ==================== REMOVE ====================

    private void handleRemove(Player player, String[] args) {
        if (args.length < 2) {
            player.sendMessage(cfg.getPrefix() + "§cUżycie: §e/dzialka remove <nazwa>");
            return;
        }

        Dzialka d = manager.getDzialkaByIdOrName(player.getUniqueId(), args[1]);

        if (d == null) {
            player.sendMessage(cfg.getPrefix() + cfg.getMessage("dzialka_not_found")
                    .replace("{name}", args[1]));
            return;
        }

        if (!d.isOwner(player.getUniqueId()) && !player.hasPermission("dzialka.admin")) {
            player.sendMessage(cfg.getPrefix() + cfg.getMessage("not_owner"));
            return;
        }

        String name = d.getName();
        manager.removeDzialka(d.getId());

        player.sendMessage(cfg.getPrefix() + cfg.getMessage("removed").replace("{name}", name));
    }

    // ==================== RENAME ====================

    private void handleRename(Player player, String[] args) {
        if (args.length < 3) {
            player.sendMessage(cfg.getPrefix() + "§cUżycie: §e/dzialka rename <nazwa> <nowa_nazwa>");
            return;
        }

        Dzialka d = manager.getDzialkaByIdOrName(player.getUniqueId(), args[1]);

        if (d == null) {
            player.sendMessage(cfg.getPrefix() + cfg.getMessage("dzialka_not_found")
                    .replace("{name}", args[1]));
            return;
        }

        if (!d.isOwner(player.getUniqueId()) && !player.hasPermission("dzialka.admin")) {
            player.sendMessage(cfg.getPrefix() + cfg.getMessage("not_owner"));
            return;
        }

        String newName = args[2];
        if (newName.length() > 32) {
            player.sendMessage(cfg.getPrefix() + "§cNazwa jest za długa! (max 32 znaki)");
            return;
        }

        d.setName(newName);
        manager.saveAll();
        player.sendMessage(cfg.getPrefix() + "§aPomyślnie zmieniono nazwę na §e" + newName + "§a!");
    }

    // ==================== ADD MEMBER ====================

    private void handleAddMember(Player player, String[] args) {
        if (args.length < 3) {
            player.sendMessage(cfg.getPrefix() + "§cUżycie: §e/dzialka addmember <nazwa_działki> <gracz>");
            return;
        }

        Dzialka d = manager.getDzialkaByIdOrName(player.getUniqueId(), args[1]);

        if (d == null) {
            player.sendMessage(cfg.getPrefix() + cfg.getMessage("dzialka_not_found")
                    .replace("{name}", args[1]));
            return;
        }

        if (!d.isOwner(player.getUniqueId()) && !player.hasPermission("dzialka.admin")) {
            player.sendMessage(cfg.getPrefix() + cfg.getMessage("not_owner"));
            return;
        }

        @SuppressWarnings("deprecation")
        OfflinePlayer target = Bukkit.getOfflinePlayer(args[2]);

        if (!target.hasPlayedBefore() && !target.isOnline()) {
            player.sendMessage(cfg.getPrefix() + "§cGracz §e" + args[2] + " §cnie istnieje!");
            return;
        }

        if (d.isOwner(target.getUniqueId())) {
            player.sendMessage(cfg.getPrefix() + "§cTen gracz jest właścicielem działki!");
            return;
        }

        if (d.isMember(target.getUniqueId())) {
            player.sendMessage(cfg.getPrefix() + "§cGracz §e" + args[2] + " §cjest już członkiem tej działki!");
            return;
        }

        d.addMember(target.getUniqueId());
        manager.saveAll();

        player.sendMessage(cfg.getPrefix() + cfg.getMessage("member_added")
                .replace("{player}", target.getName() != null ? target.getName() : args[2])
                .replace("{name}", d.getName()));

        // Powiadom dodanego gracza jeśli online
        if (target.isOnline()) {
            ((Player) target).sendMessage(cfg.getPrefix() +
                    "§aZostałeś dodany do działki §e" + d.getName() +
                    " §aprzez §e" + player.getName() + "§a!");
        }
    }

    // ==================== REMOVE MEMBER ====================

    private void handleRemoveMember(Player player, String[] args) {
        if (args.length < 3) {
            player.sendMessage(cfg.getPrefix() + "§cUżycie: §e/dzialka removemember <nazwa_działki> <gracz>");
            return;
        }

        Dzialka d = manager.getDzialkaByIdOrName(player.getUniqueId(), args[1]);

        if (d == null) {
            player.sendMessage(cfg.getPrefix() + cfg.getMessage("dzialka_not_found")
                    .replace("{name}", args[1]));
            return;
        }

        if (!d.isOwner(player.getUniqueId()) && !player.hasPermission("dzialka.admin")) {
            player.sendMessage(cfg.getPrefix() + cfg.getMessage("not_owner"));
            return;
        }

        @SuppressWarnings("deprecation")
        OfflinePlayer target = Bukkit.getOfflinePlayer(args[2]);

        if (!d.getMembers().contains(target.getUniqueId())) {
            player.sendMessage(cfg.getPrefix() + "§cGracz §e" + args[2] + " §cnie jest członkiem tej działki!");
            return;
        }

        d.removeMember(target.getUniqueId());
        manager.saveAll();

        player.sendMessage(cfg.getPrefix() + cfg.getMessage("member_removed")
                .replace("{player}", target.getName() != null ? target.getName() : args[2])
                .replace("{name}", d.getName()));

        if (target.isOnline()) {
            ((Player) target).sendMessage(cfg.getPrefix() +
                    "§cZostałeś usunięty z działki §e" + d.getName() +
                    " §cprzez §e" + player.getName() + "§c!");
        }
    }

    // ==================== MEMBERS ====================

    private void handleMembers(Player player, String[] args) {
        if (args.length < 2) {
            player.sendMessage(cfg.getPrefix() + "§cUżycie: §e/dzialka members <nazwa_działki>");
            return;
        }

        Dzialka d = manager.getDzialkaByIdOrName(player.getUniqueId(), args[1]);
        if (d == null) {
            player.sendMessage(cfg.getPrefix() + cfg.getMessage("dzialka_not_found")
                    .replace("{name}", args[1]));
            return;
        }

        player.sendMessage("§8§m----§r §6Członkowie: §e" + d.getName() + " §8§m----");
        player.sendMessage("§7Właściciel: §6" + d.getOwnerName());

        if (d.getMembers().isEmpty()) {
            player.sendMessage("§7Brak członków.");
        } else {
            player.sendMessage("§7Członkowie (" + d.getMembers().size() + "):");
            for (UUID uid : d.getMembers()) {
                OfflinePlayer op = Bukkit.getOfflinePlayer(uid);
                String name = op.getName() != null ? op.getName() : uid.toString();
                String status = op.isOnline() ? "§a●" : "§7●";
                player.sendMessage("  " + status + " §e" + name);
            }
        }
    }

    // ==================== PVP ====================

    private void handlePvP(Player player, String[] args) {
        if (args.length < 2) {
            player.sendMessage(cfg.getPrefix() + "§cUżycie: §e/dzialka pvp <nazwa_działki>");
            return;
        }

        Dzialka d = manager.getDzialkaByIdOrName(player.getUniqueId(), args[1]);
        if (d == null) {
            player.sendMessage(cfg.getPrefix() + cfg.getMessage("dzialka_not_found")
                    .replace("{name}", args[1]));
            return;
        }

        if (!d.isOwner(player.getUniqueId()) && !player.hasPermission("dzialka.admin")) {
            player.sendMessage(cfg.getPrefix() + cfg.getMessage("not_owner"));
            return;
        }

        d.setPvpEnabled(!d.isPvpEnabled());
        manager.saveAll();

        if (d.isPvpEnabled()) {
            player.sendMessage(cfg.getPrefix() + cfg.getMessage("pvp_enabled")
                    .replace("{name}", d.getName()));
        } else {
            player.sendMessage(cfg.getPrefix() + cfg.getMessage("pvp_disabled")
                    .replace("{name}", d.getName()));
        }
    }

    // ==================== SHOW ====================

    private void handleShow(Player player, String[] args) {
        if (args.length < 2) {
            player.sendMessage(cfg.getPrefix() + "§cUżycie: §e/dzialka show <nazwa_działki>");
            return;
        }

        Dzialka d = manager.getDzialkaByIdOrName(player.getUniqueId(), args[1]);

        // Sprawdź też czy jest memberem
        if (d == null) {
            d = manager.getAllDzialki().stream()
                    .filter(dz -> (dz.getName().equalsIgnoreCase(args[1]) || dz.getId().equals(args[1]))
                            && dz.isMember(player.getUniqueId()))
                    .findFirst().orElse(null);
        }

        if (d == null) {
            player.sendMessage(cfg.getPrefix() + cfg.getMessage("dzialka_not_found")
                    .replace("{name}", args[1]));
            return;
        }

        plugin.getVisualizationManager().showBorder(player, d);
    }

    // ==================== TAB COMPLETER ====================

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command,
                                       String label, String[] args) {
        if (!(sender instanceof Player player)) return List.of();

        if (args.length == 1) {
            return Arrays.asList("help", "list", "info", "remove", "rename",
                    "addmember", "removemember", "members", "pvp", "show")
                    .stream()
                    .filter(s -> s.startsWith(args[0].toLowerCase()))
                    .collect(Collectors.toList());
        }

        if (args.length == 2) {
            String sub = args[0].toLowerCase();
            if (Set.of("info", "remove", "rename", "addmember", "removemember",
                    "members", "pvp", "show").contains(sub)) {
                return manager.getPlayerDzialki(player.getUniqueId())
                        .stream()
                        .map(Dzialka::getName)
                        .filter(n -> n.toLowerCase().startsWith(args[1].toLowerCase()))
                        .collect(Collectors.toList());
            }
        }

        if (args.length == 3) {
            String sub = args[0].toLowerCase();
            if (sub.equals("addmember") || sub.equals("removemember")) {
                return Bukkit.getOnlinePlayers().stream()
                        .map(Player::getName)
                        .filter(n -> n.toLowerCase().startsWith(args[2].toLowerCase()))
                        .collect(Collectors.toList());
            }
        }

        return List.of();
    }
}
