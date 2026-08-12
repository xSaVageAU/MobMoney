package savage.mobmoney.testaddon.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Config for {@code config/mobmoney-testaddon.json}. Everything defaults to a no-op (empty
 * maps/lists, multiplier 1.0) since dimension IDs and installed attribute mods are entirely
 * setup-specific - there's no universal "reasonable default" to ship here.
 */
public class PricingConfig {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final File CONFIG_FILE = FabricLoader.getInstance().getConfigDir()
            .resolve("mobmoney-testaddon.json").toFile();

    // Multiplier applied per dimension ID (e.g. "minecraft:the_nether"). Dimensions not
    // listed here are simply untouched (multiplier of 1.0, i.e. no effect) - there's no
    // separate "everything else" setting to reason about. Works with any dimension,
    // vanilla or modded.
    public Map<String, Double> dimensionMultipliers = new LinkedHashMap<>();

    // Each rule compares a mob's current attribute value against that entity type's own
    // default value for the same attribute - a zombie's 20 HP baseline and a modded boss's
    // 300 HP baseline are both handled correctly, no hardcoded per-mob numbers. "weight"
    // controls sensitivity: 1.0 = full linear scaling with the ratio, 0.5 = dampened,
    // 0.0 = ignored. Multiple rules multiply together. Any registered attribute ID works,
    // vanilla or modded (e.g. a mob-scaling mod's own custom attribute); unknown IDs or
    // attributes a given mob doesn't have are silently skipped rather than erroring.
    public List<AttributeRule> attributeScaling = new ArrayList<>();

    // Per-mob replacements for the two rules above, keyed by entity ID (e.g.
    // "minecraft:enderman"). A mob with no entry here just uses the global rules. A mob
    // with an entry uses that entry's fields IN PLACE OF the global ones, one axis at a
    // time - e.g. specifying only dimensionMultipliers still falls back to the global
    // attributeScaling for that mob. Dimension and attribute multipliers always both
    // apply and multiply together, so "enderman pays more in the End, or if buffed, or
    // both" needs no special-casing: each condition that's true just contributes its own
    // factor, and factors that don't apply are 1.0 and vanish from the product.
    public Map<String, MobOverride> perMobOverrides = new LinkedHashMap<>();

    public static class AttributeRule {
        public String attribute;
        public double weight = 1.0;

        public AttributeRule() {
        }

        public AttributeRule(String attribute, double weight) {
            this.attribute = attribute;
            this.weight = weight;
        }
    }

    public static class MobOverride {
        // Null means "fall back to the global map/list for this axis". As with the global
        // dimensionMultipliers, a dimension not listed here (and not covered by the
        // global map either) is simply untouched.
        public Map<String, Double> dimensionMultipliers;
        public List<AttributeRule> attributeScaling;
    }

    public static PricingConfig load() {
        if (CONFIG_FILE.exists()) {
            try (FileReader reader = new FileReader(CONFIG_FILE)) {
                PricingConfig config = GSON.fromJson(reader, PricingConfig.class);
                config.save();
                return config;
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        PricingConfig config = new PricingConfig();
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
