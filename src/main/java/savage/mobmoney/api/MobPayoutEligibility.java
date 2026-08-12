package savage.mobmoney.api;

/**
 * Decides whether a kill that has already passed the mob-price whitelist and spawn-reason
 * filter should actually pay out. Listeners run in registration order; each receives the
 * running verdict from prior listeners and returns the verdict for the next one.
 */
@FunctionalInterface
public interface MobPayoutEligibility {
    boolean isEligible(MobKillContext context, boolean currentlyEligible);
}
