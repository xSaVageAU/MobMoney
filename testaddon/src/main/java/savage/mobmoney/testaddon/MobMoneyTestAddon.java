package savage.mobmoney.testaddon;

import net.fabricmc.api.ModInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import savage.mobmoney.api.MobMoneyEvents;
import savage.mobmoney.testaddon.config.PricingConfig;

/**
 * Config-driven dimension and attribute-based pricing, built on Mob Money's
 * {@link MobMoneyEvents} extension API. Addresses the GitHub suggestion that mobs should be
 * able to pay differently based on their attributes (max health, damage, etc.) and the
 * dimension they were killed in - implemented as an opt-in addon rather than baked into Mob
 * Money's core, so admins who don't want the added complexity never see it.
 * <p>
 * See {@link PricingConfig} for the config format and {@link PricingEngine} for the math.
 */
public class MobMoneyTestAddon implements ModInitializer {
    private static final Logger LOGGER = LoggerFactory.getLogger("mobmoney-testaddon");

    @Override
    public void onInitialize() {
        PricingConfig config = PricingConfig.load();
        LOGGER.info("Mob Money Test Addon loaded - dimension/attribute pricing active");

        MobMoneyEvents.PRICE_MODIFIER.register((context, price) -> {
            double dimensionMult = PricingEngine.dimensionMultiplier(config, context.mob(), context.entityId());
            double attributeMult = PricingEngine.attributeMultiplier(config, context.mob(), context.entityId(),
                    LOGGER);
            double result = price * dimensionMult * attributeMult;

            if (dimensionMult != 1.0 || attributeMult != 1.0) {
                LOGGER.info("{} - dimension x{}, attribute x{} - {} -> {}", context.entityId(), dimensionMult,
                        attributeMult, price, result);
            }
            return result;
        });
    }
}
