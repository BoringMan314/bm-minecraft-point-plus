package bm.minecraft.point.plus;

import com.destroystokyo.paper.event.player.PlayerSetSpawnEvent;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.type.Bed;
import org.bukkit.block.data.type.RespawnAnchor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.PluginCommand;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.UUID;

/** Stores per-world beds, spawn travel and ten personal points. */
public final class BmMinecraftPointPlusPlugin extends JavaPlugin implements Listener {
    public static final String USE_PERMISSION = "bm-minecraft-point-plus.use";
    public static final int MAX_POINTS = 10;
    private static final String ADMIN_PERMISSION = "bm-minecraft-point-plus.admin";
    private static final int MAX_NAME_LENGTH = 32;

    private final Map<UUID, Map<String, Location>> beds = new HashMap<>();
    private final Map<UUID, Map<String, Map<Integer, PointData>>> points = new HashMap<>();
    private File dataFile;
    private BmMinecraftPointPlusLanguageManager languageManager;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        languageManager = new BmMinecraftPointPlusLanguageManager(this);
        languageManager.reload();
        dataFile = new File(getDataFolder(), "data.yml");
        loadData();
        BmMinecraftPointPlusCommand command = new BmMinecraftPointPlusCommand(this);
        registerCommand("home", command);
        registerCommand("spawn", command);
        registerCommand("point", command);
        registerCommand("bm-minecraft-point-plus", command);
        getServer().getPluginManager().registerEvents(this, this);
        getLogger().info(languageManager.getConsole("enabled").replace("{version}", getPluginMeta().getVersion()));
    }

    @Override
    public void onDisable() {
        saveData();
        if (languageManager != null) {
            getLogger().info(languageManager.getConsole("disabled"));
        }
    }

    public BmMinecraftPointPlusLanguageManager language() {
        return languageManager;
    }

    public boolean isFeatureEnabled() {
        return getConfig().getBoolean("enabled", true);
    }

    public boolean canManage(CommandSender sender) {
        return !(sender instanceof Player)
                || !getConfig().getBoolean("admin-require-op", true)
                || sender.isOp()
                || sender.hasPermission(ADMIN_PERMISSION);
    }

    public void goHome(Player player) {
        Location bed = storedBed(player);
        if (bed != null && isValidRespawn(bed)) {
            teleport(player, bed, "teleported-home");
            return;
        }
        teleport(player, safeSpawn(player.getWorld()), "teleported-home-spawn");
    }

    public void goSpawn(Player player) {
        teleport(player, safeSpawn(player.getWorld()), "teleported-spawn");
    }

    public void setPoint(Player player, int slot, String name) {
        String label = sanitizeName(name);
        PointData data = new PointData(player.getLocation().clone(), label);
        points.computeIfAbsent(player.getUniqueId(), unused -> new HashMap<>())
                .computeIfAbsent(player.getWorld().getName(), unused -> new TreeMap<>())
                .put(slot, data);
        saveData();
        if (label.isEmpty()) {
            languageManager.send(player, "point-set", Map.of("slot", Integer.toString(slot)));
            return;
        }
        languageManager.send(player, "point-set-named", Map.of(
                "slot", Integer.toString(slot),
                "name", label));
    }

    public void listPoints(Player player) {
        Map<Integer, PointData> worldPoints = worldPoints(player);
        if (worldPoints.isEmpty()) {
            languageManager.send(player, "point-empty");
            return;
        }
        for (Map.Entry<Integer, PointData> entry : worldPoints.entrySet()) {
            Location location = entry.getValue().location();
            Map<String, String> values = Map.of(
                    "slot", Integer.toString(entry.getKey()),
                    "x", Integer.toString(location.getBlockX()),
                    "y", Integer.toString(location.getBlockY()),
                    "z", Integer.toString(location.getBlockZ()),
                    "name", entry.getValue().name());
            languageManager.send(player, entry.getValue().name().isEmpty() ? "point-line" : "point-line-named", values);
        }
    }

    public void goPoint(Player player, int slot) {
        PointData data = worldPoints(player).get(slot);
        if (data == null) {
            languageManager.send(player, "point-missing", Map.of("slot", Integer.toString(slot)));
            return;
        }
        Location destination = data.location().clone();
        destination.setWorld(player.getWorld());
        teleport(player, destination, "point-go", Map.of("slot", Integer.toString(slot)));
    }

    @EventHandler(ignoreCancelled = true)
    public void onSetSpawn(PlayerSetSpawnEvent event) {
        Player player = event.getPlayer();
        Location next = event.getLocation();
        if (next == null || next.getWorld() == null) {
            Map<String, Location> playerBeds = beds.get(player.getUniqueId());
            if (playerBeds != null) {
                playerBeds.remove(player.getWorld().getName());
            }
            saveData();
            return;
        }
        beds.computeIfAbsent(player.getUniqueId(), unused -> new HashMap<>())
                .put(next.getWorld().getName(), next.clone());
        saveData();
    }

    private void registerCommand(String name, BmMinecraftPointPlusCommand executor) {
        PluginCommand command = getCommand(name);
        if (command == null) {
            throw new IllegalStateException(languageManager.getConsole("missing-command").replace("{command}", name));
        }
        command.setExecutor(executor);
        command.setTabCompleter(executor);
    }

    private void teleport(Player player, Location destination, String messageKey) {
        teleport(player, destination, messageKey, Map.of());
    }

    private void teleport(Player player, Location destination, String messageKey, Map<String, String> replacements) {
        Location target = destination.clone();
        if (target.getWorld() == null) {
            target.setWorld(player.getWorld());
        }
        player.teleportAsync(target).thenAccept(success -> getServer().getScheduler().runTask(this, () -> {
            if (success) {
                languageManager.send(player, messageKey, replacements);
            }
        }));
    }

    private Location storedBed(Player player) {
        Location bed = getStoredLocation(beds, player.getUniqueId(), player.getWorld().getName());
        if (bed != null) {
            bed.setWorld(player.getWorld());
        }
        return bed;
    }

    private Map<Integer, PointData> worldPoints(Player player) {
        Map<String, Map<Integer, PointData>> playerPoints = points.get(player.getUniqueId());
        if (playerPoints == null) {
            return Map.of();
        }
        Map<Integer, PointData> worldPoints = playerPoints.get(player.getWorld().getName());
        return worldPoints == null ? Map.of() : worldPoints;
    }

    private Location safeSpawn(World world) {
        Location spawn = world.getSpawnLocation().clone();
        spawn.setX(spawn.getBlockX() + 0.5D);
        spawn.setZ(spawn.getBlockZ() + 0.5D);
        return spawn;
    }

    private boolean isValidRespawn(Location location) {
        if (location.getWorld() == null) {
            return false;
        }
        BlockData data = location.getBlock().getBlockData();
        if (data instanceof Bed) {
            return true;
        }
        return data instanceof RespawnAnchor anchor && anchor.getCharges() > 0;
    }

    private String sanitizeName(String name) {
        if (name == null) {
            return "";
        }
        String text = name.replace('\n', ' ').replace('\r', ' ').trim();
        if (text.length() > MAX_NAME_LENGTH) {
            return text.substring(0, MAX_NAME_LENGTH);
        }
        return text;
    }

    private Location getStoredLocation(Map<UUID, Map<String, Location>> store, UUID uuid, String worldName) {
        Map<String, Location> values = store.get(uuid);
        if (values == null) {
            return null;
        }
        Location location = values.get(worldName);
        return location == null ? null : location.clone();
    }

    private void loadData() {
        if (!dataFile.isFile()) {
            return;
        }
        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(dataFile);
        ConfigurationSection players = yaml.getConfigurationSection("players");
        if (players == null) {
            return;
        }
        for (String key : players.getKeys(false)) {
            UUID uuid;
            try {
                uuid = UUID.fromString(key);
            } catch (IllegalArgumentException exception) {
                getLogger().warning(languageManager.getConsole("invalid-player-uuid").replace("{uuid}", key));
                continue;
            }
            ConfigurationSection section = players.getConfigurationSection(key);
            if (section == null) {
                continue;
            }
            beds.put(uuid, readLocationMap(section.getConfigurationSection("beds")));
            points.put(uuid, readPointMap(section.getConfigurationSection("points")));
        }
    }

    private Map<String, Location> readLocationMap(ConfigurationSection section) {
        Map<String, Location> values = new HashMap<>();
        if (section == null) {
            return values;
        }
        for (String worldName : section.getKeys(false)) {
            ConfigurationSection worldSection = section.getConfigurationSection(worldName);
            if (worldSection == null) {
                continue;
            }
            values.put(worldName, readLocation(worldSection, worldName));
        }
        return values;
    }

    private Map<String, Map<Integer, PointData>> readPointMap(ConfigurationSection section) {
        Map<String, Map<Integer, PointData>> values = new HashMap<>();
        if (section == null) {
            return values;
        }
        for (String worldName : section.getKeys(false)) {
            ConfigurationSection worldSection = section.getConfigurationSection(worldName);
            if (worldSection == null) {
                continue;
            }
            Map<Integer, PointData> worldPoints = new TreeMap<>();
            for (String slotKey : worldSection.getKeys(false)) {
                Integer slot = parseStoredSlot(slotKey);
                ConfigurationSection pointSection = worldSection.getConfigurationSection(slotKey);
                if (slot == null || pointSection == null) {
                    continue;
                }
                String name = pointSection.getString("name", "");
                worldPoints.put(slot, new PointData(readLocation(pointSection, worldName), name == null ? "" : name));
            }
            if (!worldPoints.isEmpty()) {
                values.put(worldName, worldPoints);
            }
        }
        return values;
    }

    private Integer parseStoredSlot(String text) {
        try {
            int slot = Integer.parseInt(text);
            if (slot >= 1 && slot <= MAX_POINTS) {
                return slot;
            }
        } catch (NumberFormatException ignored) {
            // Skip invalid stored slot keys.
        }
        return null;
    }

    private Location readLocation(ConfigurationSection section, String worldName) {
        return new Location(
                getServer().getWorld(worldName),
                section.getDouble("x"),
                section.getDouble("y"),
                section.getDouble("z"),
                (float) section.getDouble("yaw"),
                (float) section.getDouble("pitch"));
    }

    private void saveData() {
        File folder = getDataFolder();
        if (!folder.isDirectory() && !folder.mkdirs()) {
            getLogger().warning(languageManager.getConsole("data-directory-create-failed"));
            return;
        }
        YamlConfiguration yaml = new YamlConfiguration();
        Set<UUID> players = new HashSet<>();
        players.addAll(beds.keySet());
        players.addAll(points.keySet());
        for (UUID uuid : players) {
            String path = "players." + uuid;
            writeLocationMap(yaml, path + ".beds", beds.get(uuid));
            writePointMap(yaml, path + ".points", points.get(uuid));
        }
        try {
            yaml.save(dataFile);
        } catch (IOException exception) {
            getLogger().warning(languageManager.getConsole("data-save-failed").replace("{error}", exception.getMessage()));
        }
    }

    private void writeLocationMap(YamlConfiguration yaml, String path, Map<String, Location> values) {
        if (values == null) {
            return;
        }
        for (Map.Entry<String, Location> entry : values.entrySet()) {
            writeLocation(yaml, path + "." + entry.getKey(), entry.getValue(), null);
        }
    }

    private void writePointMap(YamlConfiguration yaml, String path, Map<String, Map<Integer, PointData>> values) {
        if (values == null) {
            return;
        }
        for (Map.Entry<String, Map<Integer, PointData>> worldEntry : values.entrySet()) {
            for (Map.Entry<Integer, PointData> pointEntry : worldEntry.getValue().entrySet()) {
                writeLocation(
                        yaml,
                        path + "." + worldEntry.getKey() + "." + pointEntry.getKey(),
                        pointEntry.getValue().location(),
                        pointEntry.getValue().name());
            }
        }
    }

    private void writeLocation(YamlConfiguration yaml, String path, Location location, String name) {
        yaml.set(path + ".x", location.getX());
        yaml.set(path + ".y", location.getY());
        yaml.set(path + ".z", location.getZ());
        yaml.set(path + ".yaw", location.getYaw());
        yaml.set(path + ".pitch", location.getPitch());
        if (name != null && !name.isEmpty()) {
            yaml.set(path + ".name", name);
        }
    }

    private record PointData(Location location, String name) {
    }
}
