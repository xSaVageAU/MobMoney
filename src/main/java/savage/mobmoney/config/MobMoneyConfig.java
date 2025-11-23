package savage.mobmoney.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class MobMoneyConfig {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final File CONFIG_FILE = FabricLoader.getInstance().getConfigDir().resolve("mob-money.json").toFile();

    public String economyProvider = "savs_common_economy";
    public String currencyId = "dollar";
    public NotificationMode notificationMode = NotificationMode.CHAT;
    public Map<String, Double> mobPrices = new HashMap<>();

    public enum NotificationMode {
        CHAT,
        ACTION_BAR,
        NONE
    }

    public MobMoneyConfig() {
        mobPrices.put("minecraft:zombie", 5.0);
        mobPrices.put("minecraft:skeleton", 5.0);
        mobPrices.put("minecraft:creeper", 10.0);
        mobPrices.put("minecraft:spider", 5.0);
        mobPrices.put("minecraft:ender_dragon", 1000.0);
        mobPrices.put("minecraft:wither", 500.0);
    }

    public static MobMoneyConfig load() {
        if (CONFIG_FILE.exists()) {
            try (FileReader reader = new FileReader(CONFIG_FILE)) {
                return GSON.fromJson(reader, MobMoneyConfig.class);
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
