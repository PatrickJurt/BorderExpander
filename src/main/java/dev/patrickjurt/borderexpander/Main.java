package dev.patrickjurt.borderexpander;

import dev.patrickjurt.borderexpander.commands.BorderExpanderCommand;
import dev.patrickjurt.borderexpander.inventories.MissingItemsMenu;
import dev.patrickjurt.borderexpander.listeners.InventoryClickListener;
import dev.patrickjurt.borderexpander.listeners.PlayerJoinListener;
import dev.patrickjurt.borderexpander.listeners.PlayerLevelChangeListener;
import dev.patrickjurt.borderexpander.listeners.WorldLoadListener;
import org.bukkit.*;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

public final class Main extends JavaPlugin {
    private final Set<Material> allTrackableItems = EnumSet.noneOf(Material.class);
    private final Set<Material> globallyFoundItems = EnumSet.noneOf(Material.class);
    private final Map<UUID, Set<Material>> playerFoundItems = new HashMap<>();
    private final Map<UUID, String> playerNames = new HashMap<>();
    private final MissingItemsMenu missingItemsMenu = new MissingItemsMenu(this);

    private File dataFile;
    private YamlConfiguration dataConfig;
    private boolean lategameAnnounced;
    private BukkitTask scanTask;

    private World world;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        initializeTrackableItems();
        setupDataFile();
        loadData();

        BorderExpanderCommand borderExpanderCommand = new BorderExpanderCommand(this);
        Objects.requireNonNull(getCommand("borderexpander"), "borderexpander command missing").setExecutor(borderExpanderCommand);
        Objects.requireNonNull(getCommand("borderexpander"), "borderexpander command missing").setTabCompleter(borderExpanderCommand);

        getServer().getPluginManager().registerEvents(new PlayerJoinListener(this), this);
        getServer().getPluginManager().registerEvents(new PlayerLevelChangeListener(this), this);
        getServer().getPluginManager().registerEvents(new WorldLoadListener(this), this);
        getServer().getPluginManager().registerEvents(new InventoryClickListener(this), this);

        world = getServer().getWorlds().getFirst();
        handleWorldLoad(world);

        applyBorderSize();
        startInventoryScanTask();
        logWorldStatus();
    }

    private void logWorldStatus() {
        Location spawn = world.getSpawnLocation();
        Location center = world.getWorldBorder().getCenter();
        getLogger().info("World ready. Spawn=("
            + spawn.getBlockX() + "," + spawn.getBlockY() + "," + spawn.getBlockZ() + ") BorderCenter=("
            + center.getBlockX() + "," + center.getBlockZ() + ") BorderSize=" + world.getWorldBorder().getSize());
    }

    @Override
    public void onDisable() {
        if (scanTask != null) {
            scanTask.cancel();
        }
        saveData();
    }

    private static final Set<Material> EXCLUDED_TRACKABLE_ITEMS = EnumSet.of(
        Material.BARRIER,
        Material.BEDROCK,
        Material.COMMAND_BLOCK,
        Material.CHAIN_COMMAND_BLOCK,
        Material.REPEATING_COMMAND_BLOCK,
        Material.COMMAND_BLOCK_MINECART,
        Material.DEBUG_STICK,
        Material.DIRT_PATH,
        Material.END_PORTAL_FRAME,
        Material.FROGSPAWN,
        Material.JIGSAW,
        Material.KNOWLEDGE_BOOK,
        Material.LIGHT,
        Material.PLAYER_HEAD,
        Material.REINFORCED_DEEPSLATE,
        Material.SPAWNER,
        Material.STRUCTURE_VOID,
        Material.STRUCTURE_BLOCK,
        Material.TEST_INSTANCE_BLOCK,
        Material.TEST_BLOCK,
        Material.TRIAL_SPAWNER,
        Material.VAULT
    );

    private void initializeTrackableItems() {
        allTrackableItems.clear();
        for (Material material : Material.values()) {
            if (material.isItem() && !material.isAir() && !material.isLegacy() && isTrackable(material)) {
                allTrackableItems.add(material);
            }
        }
    }

    private boolean isTrackable(Material material) {
        if (material.name().endsWith("_SPAWN_EGG")) {
            return false;
        }
        if (material.name().startsWith("INFESTED_")) {
            return false;
        }
        return !EXCLUDED_TRACKABLE_ITEMS.contains(material);
    }

    private void setupDataFile() {
        dataFile = new File(getDataFolder(), "data.yml");
        if (!dataFile.exists()) {
            dataFile.getParentFile().mkdirs();
            try {
                dataFile.createNewFile();
            } catch (IOException exception) {
                getLogger().severe("Could not create data.yml: " + exception.getMessage());
            }
        }
        dataConfig = YamlConfiguration.loadConfiguration(dataFile);
    }

    private void loadData() {
        globallyFoundItems.clear();
        playerFoundItems.clear();
        playerNames.clear();

        for (String materialName : dataConfig.getStringList("global-found")) {
            Material material = Material.matchMaterial(materialName);
            if (material != null && allTrackableItems.contains(material)) {
                globallyFoundItems.add(material);
            }
        }

        ConfigurationSection playerSection = dataConfig.getConfigurationSection("player-found");
        if (playerSection != null) {
            for (String key : playerSection.getKeys(false)) {
                try {
                    UUID uuid = UUID.fromString(key);
                    Set<Material> found = EnumSet.noneOf(Material.class);
                    for (String materialName : playerSection.getStringList(key)) {
                        Material material = Material.matchMaterial(materialName);
                        if (material != null && allTrackableItems.contains(material)) {
                            found.add(material);
                        }
                    }
                    playerFoundItems.put(uuid, found);
                } catch (IllegalArgumentException ignored) {
                    // Ignore bad UUID entries.
                }
            }
        }

        ConfigurationSection namesSection = dataConfig.getConfigurationSection("player-names");
        if (namesSection != null) {
            for (String key : namesSection.getKeys(false)) {
                try {
                    playerNames.put(UUID.fromString(key), namesSection.getString(key, key));
                } catch (IllegalArgumentException ignored) {
                    // Ignore bad UUID entries.
                }
            }
        }

        lategameAnnounced = dataConfig.getBoolean("lategame-announced", globallyFoundItems.size() >= 1000);
    }

    private void saveData() {
        dataConfig.set("global-found", globallyFoundItems.stream().map(Enum::name).sorted().toList());

        dataConfig.set("player-found", null);
        for (Map.Entry<UUID, Set<Material>> entry : playerFoundItems.entrySet()) {
            dataConfig.set("player-found." + entry.getKey(), entry.getValue().stream().map(Enum::name).sorted().toList());
        }

        dataConfig.set("player-names", null);
        for (Map.Entry<UUID, String> entry : playerNames.entrySet()) {
            dataConfig.set("player-names." + entry.getKey(), entry.getValue());
        }

        dataConfig.set("lategame-announced", lategameAnnounced);

        try {
            dataConfig.save(dataFile);
        } catch (IOException exception) {
            getLogger().severe("Could not save data.yml: " + exception.getMessage());
        }
    }

    private void startInventoryScanTask() {
        long scanIntervalTicks = Math.max(1L, getConfig().getLong("scan-interval-ticks", 20L));
        scanTask = Bukkit.getScheduler().runTaskTimer(this, () -> {
            for (Player player : Bukkit.getOnlinePlayers()) {
                scanPlayerInventoryForNewItems(player);
            }
        }, 20L, scanIntervalTicks);
    }

    private void scanPlayerInventoryForNewItems(Player player) {
        Set<Material> playerSet = playerFoundItems.computeIfAbsent(player.getUniqueId(), ignored -> EnumSet.noneOf(Material.class));
        playerNames.put(player.getUniqueId(), player.getName());

        boolean changed = false;
        for (ItemStack stack : player.getInventory().getContents()) {
            if (stack == null) {
                continue;
            }
            Material material = stack.getType();
            if (!allTrackableItems.contains(material) || playerSet.contains(material)) {
                continue;
            }
            changed |= registerNewItemForPlayer(player, material);
        }

        if (changed) {
            saveData();
        }
    }

    private boolean registerNewItemForPlayer(Player player, Material material) {
        Set<Material> foundByPlayer = playerFoundItems.computeIfAbsent(player.getUniqueId(), ignored -> EnumSet.noneOf(Material.class));
        if (!foundByPlayer.add(material)) {
            return false;
        }

        boolean newGlobalItem = globallyFoundItems.add(material);
        if (newGlobalItem) {
            Bukkit.broadcastMessage(player.getName() + " obtained " + prettyMaterialName(material));
            for (Player onlinePlayer : Bukkit.getOnlinePlayers()) {
                onlinePlayer.playSound(onlinePlayer.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 0.75f, 1.1f);
            }

            if (!lategameAnnounced && globallyFoundItems.size() >= 1000) {
                lategameAnnounced = true;
                Bukkit.broadcastMessage("Lategame unlocked: every new item now expands the border by 10 blocks.");
            }
            applyBorderSize();
        }

        return true;
    }

    public void handlePlayerJoin(Player player) {
        playerNames.put(player.getUniqueId(), player.getName());
        playerFoundItems.computeIfAbsent(player.getUniqueId(), ignored -> EnumSet.noneOf(Material.class));
        teleportToGameplayWorldIfNeeded(player);
        scanPlayerInventoryForNewItems(player);
    }

    private void teleportToGameplayWorldIfNeeded(Player player) {
        if (player.getWorld().equals(world)) {
            return;
        }

        Location current = player.getLocation();
        Location spawn = world.getSpawnLocation();
        Location safeLocation = new Location(world, spawn.getBlockX() + 0.5D, spawn.getY(), spawn.getBlockZ() + 0.5D,
            current.getYaw(), current.getPitch());
        player.teleport(safeLocation);
    }

    public void handlePlayerLevelChange() {
        if (globallyFoundItems.size() >= 1000) {
            applyBorderSize();
        }
    }

    public void handleWorldLoad(World world) {
        if (world.getFullTime() == 0L) {
            getLogger().info("Detected brand new gameplay world '" + world.getName() + "' - resetting BorderExpander stats and world border.");
            resetProgress();
        }
        applyBorderSize();
    }

    private void resetProgress() {
        globallyFoundItems.clear();
        playerFoundItems.clear();
        playerNames.clear();
        lategameAnnounced = false;
        saveData();
    }

    private void applyBorderSize() {
        double size = Math.max(computeCurrentBorderSize(), 1D);
        world.getWorldBorder().setSize(size);
        if (size%2 == 0) {
            world.getWorldBorder().setCenter(world.getSpawnLocation().getBlockX() + 1D, world.getSpawnLocation().getBlockZ() + 1D);
        }else{
            world.getWorldBorder().setCenter(world.getSpawnLocation().getBlockX() + 0.5D, world.getSpawnLocation().getBlockZ() + 0.5D);
        }
    }

    private double computeCurrentBorderSize() {
        int itemCount = globallyFoundItems.size();
        double sizeFromItems = 1D + Math.min(itemCount, 1000) + Math.max(0, itemCount - 1000) * 10D;
        return sizeFromItems + computeLategameExperienceBonus(itemCount);
    }

    private double computeLategameExperienceBonus(int itemCount) {
        if (itemCount < 1000) {
            return 0D;
        }

        List<Player> chosenPlayers = resolveLategamePlayers();
        if (chosenPlayers.size() < 2) {
            return 0D;
        }

        double averageLevel = (chosenPlayers.get(0).getLevel() + chosenPlayers.get(1).getLevel()) / 2.0D;
        return averageLevel * 10.0D;
    }

    private List<Player> resolveLategamePlayers() {
        String playerOneName = getConfig().getString("lategame-player-1", "").trim();
        String playerTwoName = getConfig().getString("lategame-player-2", "").trim();

        Player playerOne = playerOneName.isEmpty() ? null : Bukkit.getPlayerExact(playerOneName);
        Player playerTwo = playerTwoName.isEmpty() ? null : Bukkit.getPlayerExact(playerTwoName);

        if (playerOne != null && playerTwo != null && !playerOne.getUniqueId().equals(playerTwo.getUniqueId())) {
            return List.of(playerOne, playerTwo);
        }

        return Bukkit.getOnlinePlayers().stream()
            .map(Player.class::cast)
            .sorted(Comparator.comparing(Player::getName, String.CASE_INSENSITIVE_ORDER))
            .limit(2)
            .toList();
    }

    public void sendStats(CommandSender sender) {
        sender.sendMessage("BorderExpander stats:");

        List<Map.Entry<UUID, Set<Material>>> entries = new ArrayList<>(playerFoundItems.entrySet());
        entries.sort((left, right) -> Integer.compare(right.getValue().size(), left.getValue().size()));

        for (Map.Entry<UUID, Set<Material>> entry : entries) {
            String name = playerNames.get(entry.getKey());
            if (name == null) {
                OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(entry.getKey());
                name = offlinePlayer.getName() != null ? offlinePlayer.getName() : entry.getKey().toString();
            }
            sender.sendMessage("- " + name + ": " + entry.getValue().size());
        }

        sender.sendMessage("Global unique items: " + globallyFoundItems.size());
        sender.sendMessage("Current border size: " + String.format(Locale.ROOT, "%.1f", computeCurrentBorderSize()));
    }

    public MissingItemsMenu getMissingItemsMenu() {
        return missingItemsMenu;
    }

    public Set<Material> getAllTrackableItems() {
        return Collections.unmodifiableSet(allTrackableItems);
    }

    public Set<Material> getFoundItems(UUID playerId) {
        return playerFoundItems.getOrDefault(playerId, Collections.emptySet());
    }

    public Set<Material> getGloballyFoundItems() {
        return Collections.unmodifiableSet(globallyFoundItems);
    }

    public String prettyMaterialName(Material material) {
        return material.name().toLowerCase(Locale.ROOT).replace('_', ' ');
    }
}

