package savage.mobmoney.testaddon;

import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.DefaultAttributes;
import org.slf4j.Logger;
import savage.mobmoney.testaddon.config.PricingConfig;

import java.util.List;
import java.util.Map;

/**
 * Pure pricing math for {@link PricingConfig} - kept separate from the mod entrypoint so it's
 * trivial to reason about (and unit test) independent of how it's wired into MobMoneyEvents.
 */
final class PricingEngine {
    private PricingEngine() {
    }

    static double dimensionMultiplier(PricingConfig config, LivingEntity mob, String entityId) {
        PricingConfig.MobOverride override = config.perMobOverrides.get(entityId);
        Map<String, Double> multipliers = (override != null && override.dimensionMultipliers != null)
                ? override.dimensionMultipliers
                : config.dimensionMultipliers;

        String dimensionId = mob.level().dimension().identifier().toString();
        return multipliers.getOrDefault(dimensionId, 1.0);
    }

    @SuppressWarnings("unchecked")
    static double attributeMultiplier(PricingConfig config, LivingEntity mob, String entityId, Logger logger) {
        PricingConfig.MobOverride override = config.perMobOverrides.get(entityId);
        List<PricingConfig.AttributeRule> rules = (override != null && override.attributeScaling != null)
                ? override.attributeScaling
                : config.attributeScaling;

        if (rules.isEmpty()) {
            return 1.0;
        }

        // Safe: any LivingEntity's EntityType is by definition EntityType<? extends LivingEntity>,
        // erasure just doesn't let the compiler see that from Entity#getType()'s wildcard return.
        EntityType<? extends LivingEntity> type = (EntityType<? extends LivingEntity>) mob.getType();
        if (!DefaultAttributes.hasSupplier(type)) {
            return 1.0;
        }
        AttributeSupplier defaults = DefaultAttributes.getSupplier(type);

        double multiplier = 1.0;
        for (PricingConfig.AttributeRule rule : rules) {
            if (rule.attribute == null || rule.weight == 0.0) {
                continue;
            }

            Identifier id = Identifier.tryParse(rule.attribute);
            if (id == null) {
                logger.warn("Ignoring invalid attribute ID in mobmoney-testaddon.json: {}", rule.attribute);
                continue;
            }

            Holder.Reference<Attribute> holder = BuiltInRegistries.ATTRIBUTE.get(id).orElse(null);
            if (holder == null || !defaults.hasAttribute(holder)) {
                // Unknown attribute (typo, or its mod isn't installed) or this mob type
                // doesn't have it - skip rather than fail the whole payout.
                continue;
            }

            double baseline = defaults.getBaseValue(holder);
            if (baseline <= 0) {
                continue;
            }

            double current = mob.getAttributeValue(holder);
            multiplier *= Math.pow(current / baseline, rule.weight);
        }
        return multiplier;
    }
}
