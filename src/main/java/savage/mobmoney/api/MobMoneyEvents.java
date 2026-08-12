package savage.mobmoney.api;

import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.EventFactory;

/**
 * Extension points for other mods to build opinionated payout logic (per-dimension pricing,
 * attribute-scaled pricing, etc.) on top of Mob Money's core whitelist/spawn-reason gating,
 * without Mob Money itself needing to know or care what that logic is.
 */
public final class MobMoneyEvents {
    public static final Event<MobPayoutEligibility> PAYOUT_ELIGIBILITY = EventFactory.createArrayBacked(
            MobPayoutEligibility.class,
            listeners -> (context, currentlyEligible) -> {
                boolean eligible = currentlyEligible;
                for (MobPayoutEligibility listener : listeners) {
                    eligible = listener.isEligible(context, eligible);
                }
                return eligible;
            });

    public static final Event<MobPriceModifier> PRICE_MODIFIER = EventFactory.createArrayBacked(
            MobPriceModifier.class,
            listeners -> (context, currentPrice) -> {
                double price = currentPrice;
                for (MobPriceModifier listener : listeners) {
                    price = listener.modifyPrice(context, price);
                }
                return price;
            });

    private MobMoneyEvents() {
    }
}
