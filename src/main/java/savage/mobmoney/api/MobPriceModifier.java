package savage.mobmoney.api;

/**
 * Adjusts the payout for a kill that has already passed the mob-price whitelist, spawn-reason
 * filter, and {@link MobPayoutEligibility} check. Listeners run in registration order, each
 * receiving the running price from prior listeners and returning the price for the next one.
 * The earnings cap is applied afterward, against the final price.
 */
@FunctionalInterface
public interface MobPriceModifier {
    double modifyPrice(MobKillContext context, double currentPrice);
}
