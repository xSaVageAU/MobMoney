package savage.mobmoney;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import savage.mobmoney.config.MobMoneyConfig;
import savage.mobmoney.listener.MobKillListener;

public class MobMoneyMod implements ModInitializer {
    public static final String MOD_ID = "mob-money";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);
    public static MobMoneyConfig CONFIG;

    @Override
    public void onInitialize() {
        LOGGER.info("Initializing Mob Money Mod");
        CONFIG = MobMoneyConfig.load();

        ServerLivingEntityEvents.AFTER_DEATH.register(new MobKillListener());
    }

    public static String formatCurrency(double amount) {
        if (CONFIG.symbolBeforeAmount) {
            return String.format("%s%.2f", CONFIG.currencySymbol, amount);
        } else {
            return String.format("%.2f%s", amount, CONFIG.currencySymbol);
        }
    }
}
