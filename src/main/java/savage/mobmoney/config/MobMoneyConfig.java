package savage.mobmoney.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.world.entity.EntitySpawnReason;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

public class MobMoneyConfig {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final File CONFIG_FILE = FabricLoader.getInstance().getConfigDir().resolve("mob-money.json")
            .toFile();

    public String economyProvider = "savs_common_economy";
    public String currencyId = "dollar";
    public String currencySymbol = "$";
    public boolean symbolBeforeAmount = true;
    public NotificationMode notificationMode = NotificationMode.CHAT;
    public Map<String, Double> mobPrices = new HashMap<>();

    public enum NotificationMode {
        CHAT,
        ACTION_BAR,
        NONE
    }

    public enum CapOverflowMode {
        DROP, // If amount > remaining, award 0 (Hard cap)
        PARTIAL, // If amount > remaining, award remaining
        ALLOW // If current < max, award full amount (Soft cap)
    }

    public enum SpawnReasonFilterMode {
        NONE, // No filtering; every spawn reason pays out
        BLACKLIST, // Reasons in spawnReasonFilter do NOT pay out; everything else does
        WHITELIST // Only reasons in spawnReasonFilter pay out
    }

    // Balancing Settings
    public double maxEarningsPerPeriod = 100.0; // Set to 0 to disable
    public int earningPeriodDuration = 1200; // Seconds (20 minutes)
    public CapOverflowMode overflowMode = CapOverflowMode.DROP;

    // Spawn Reason Filtering
    // Valid values (net.minecraft.world.entity.EntitySpawnReason): NATURAL, CHUNK_GENERATION,
    // SPAWNER, STRUCTURE, BREEDING, MOB_SUMMONED, JOCKEY, EVENT, CONVERSION, REINFORCEMENT,
    // TRIGGERED, BUCKET, SPAWN_ITEM_USE, COMMAND, DISPENSER, PATROL, TRIAL_SPAWNER, LOAD,
    // DIMENSION_TRAVEL
    public SpawnReasonFilterMode spawnReasonFilterMode = SpawnReasonFilterMode.BLACKLIST;
    public Set<String> spawnReasonFilter = new LinkedHashSet<>();

    public MobMoneyConfig() {
        mobPrices.put("minecraft:zombie", 5.0);
        mobPrices.put("minecraft:skeleton", 5.0);
        mobPrices.put("minecraft:creeper", 10.0);
        mobPrices.put("minecraft:spider", 5.0);
        mobPrices.put("minecraft:ender_dragon", 1000.0);
        mobPrices.put("minecraft:wither", 500.0);

        // Reasonable default: don't pay out for mobs farmed via spawners.
        // Everything else (natural spawns, breeding, structures, etc.) still pays.
        spawnReasonFilter.add(EntitySpawnReason.SPAWNER.name());
        spawnReasonFilter.add(EntitySpawnReason.TRIAL_SPAWNER.name());
    }

    public boolean isSpawnReasonAllowed(EntitySpawnReason reason) {
        if (spawnReasonFilterMode == SpawnReasonFilterMode.NONE) {
            return true;
        }
        boolean listed = spawnReasonFilter.contains(reason.name());
        return spawnReasonFilterMode == SpawnReasonFilterMode.WHITELIST ? listed : !listed;
    }

    public static MobMoneyConfig load() {
        if (CONFIG_FILE.exists()) {
            try (FileReader reader = new FileReader(CONFIG_FILE)) {
                MobMoneyConfig config = GSON.fromJson(reader, MobMoneyConfig.class);
                config.save(); // Save to ensure new fields are written
                return config;
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        MobMoneyConfig config = new MobMoneyConfig();
        config.save();
        return config;
    }

    public void save() {
        try (FileWriter writer = new FileWriter(CONFIG_FILE)) {
            GSON.toJson(this, writer);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
