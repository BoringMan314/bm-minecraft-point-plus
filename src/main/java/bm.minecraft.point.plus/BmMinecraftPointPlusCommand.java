package bm.minecraft.point.plus;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.IntStream;

/** Handles home, spawn, point and administrative commands. */
public final class BmMinecraftPointPlusCommand implements CommandExecutor, TabCompleter {
    private static final List<String> SLOT_SUGGESTIONS = IntStream.rangeClosed(1, BmMinecraftPointPlusPlugin.MAX_POINTS)
            .mapToObj(Integer::toString)
            .toList();

    private final BmMinecraftPointPlusPlugin plugin;

    public BmMinecraftPointPlusCommand(BmMinecraftPointPlusPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        return switch (command.getName().toLowerCase(Locale.ROOT)) {
            case "home" -> runHome(sender, args);
            case "spawn" -> runSpawn(sender, args);
            case "point" -> runPoint(sender, args);
            default -> runManagementCommand(sender, args);
        };
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        String name = command.getName().toLowerCase(Locale.ROOT);
        if (name.equals("home") || name.equals("spawn")) {
            return List.of();
        }
        if (name.equals("point")) {
            return tabPoint(args);
        }
        if (args.length != 1 || !plugin.canManage(sender)) {
            return List.of();
        }
        String input = args[0].toLowerCase(Locale.ROOT);
        return List.of("0", "1", "info", "status", "reload").stream()
                .filter(option -> option.startsWith(input))
                .toList();
    }

    private List<String> tabPoint(String[] args) {
        if (args.length == 1) {
            String input = args[0].toLowerCase(Locale.ROOT);
            List<String> options = new ArrayList<>();
            if ("set".startsWith(input)) {
                options.add("set");
            }
            for (String slot : SLOT_SUGGESTIONS) {
                if (slot.startsWith(input)) {
                    options.add(slot);
                }
            }
            return options;
        }
        if (args.length == 2 && args[0].equalsIgnoreCase("set")) {
            String input = args[1];
            return SLOT_SUGGESTIONS.stream()
                    .filter(slot -> slot.startsWith(input))
                    .toList();
        }
        return List.of();
    }

    private boolean runHome(CommandSender sender, String[] args) {
        Player player = requirePlayer(sender, args, "usage-home");
        if (player == null) {
            return true;
        }
        plugin.goHome(player);
        return true;
    }

    private boolean runSpawn(CommandSender sender, String[] args) {
        Player player = requirePlayer(sender, args, "usage-spawn");
        if (player == null) {
            return true;
        }
        plugin.goSpawn(player);
        return true;
    }

    private boolean runPoint(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            plugin.language().send(sender, "player-only");
            return true;
        }
        if (!canUse(player)) {
            return true;
        }
        if (args.length == 0) {
            plugin.listPoints(player);
            return true;
        }
        if (args[0].equalsIgnoreCase("set")) {
            if (args.length < 2) {
                plugin.language().send(player, "usage-point-set");
                return true;
            }
            Integer slot = parseSlot(args[1]);
            if (slot == null) {
                plugin.language().send(player, "invalid-slot");
                return true;
            }
            String name = args.length == 2 ? "" : String.join(" ", Arrays.copyOfRange(args, 2, args.length)).trim();
            plugin.setPoint(player, slot, name);
            return true;
        }
        if (args.length != 1) {
            plugin.language().send(player, "usage-point");
            return true;
        }
        Integer slot = parseSlot(args[0]);
        if (slot == null) {
            plugin.language().send(player, "usage-point");
            return true;
        }
        plugin.goPoint(player, slot);
        return true;
    }

    private boolean runManagementCommand(CommandSender sender, String[] args) {
        if (args.length == 0) {
            help(sender);
            return true;
        }
        if (!plugin.canManage(sender)) {
            plugin.language().send(sender, "no-permission");
            return true;
        }
        switch (args[0].toLowerCase(Locale.ROOT)) {
            case "0", "1" -> {
                boolean enabled = args[0].equals("1");
                plugin.getConfig().set("enabled", enabled);
                plugin.saveConfig();
                plugin.language().send(sender, enabled ? "enabled" : "disabled");
            }
            case "reload" -> {
                plugin.reloadConfig();
                plugin.language().reload();
                plugin.language().send(sender, "reloaded");
            }
            case "info" -> plugin.language().send(sender, "info", Map.of("version", plugin.getPluginMeta().getVersion()));
            case "status" -> {
                String world = sender instanceof Player player ? player.getWorld().getName() : "-";
                plugin.language().send(sender, "status", Map.of(
                        "enabled", plugin.isFeatureEnabled() ? "ON" : "OFF",
                        "world", world));
            }
            default -> help(sender);
        }
        return true;
    }

    private Player requirePlayer(CommandSender sender, String[] args, String usageKey) {
        if (args.length != 0) {
            plugin.language().send(sender, usageKey);
            return null;
        }
        if (!(sender instanceof Player player)) {
            plugin.language().send(sender, "player-only");
            return null;
        }
        if (!canUse(player)) {
            return null;
        }
        return player;
    }

    private boolean canUse(Player player) {
        if (!player.hasPermission(BmMinecraftPointPlusPlugin.USE_PERMISSION)) {
            plugin.language().send(player, "no-use-permission");
            return false;
        }
        if (!plugin.isFeatureEnabled()) {
            plugin.language().send(player, "feature-disabled");
            return false;
        }
        return true;
    }

    private Integer parseSlot(String text) {
        try {
            int slot = Integer.parseInt(text);
            if (slot >= 1 && slot <= BmMinecraftPointPlusPlugin.MAX_POINTS) {
                return slot;
            }
        } catch (NumberFormatException ignored) {
            // Invalid slot text is handled by the caller.
        }
        return null;
    }

    private void help(CommandSender sender) {
        for (String key : List.of(
                "help-header", "help-home", "help-spawn", "help-point-set", "help-point-list", "help-point-go",
                "help-toggle", "help-reload", "help-info", "help-status", "help-footer")) {
            plugin.language().send(sender, key);
        }
    }
}
