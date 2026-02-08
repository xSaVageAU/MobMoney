package savage.mobmoney.manager;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import savage.mobmoney.MobMoneyMod;
import savage.mobmoney.config.MobMoneyConfig.CapOverflowMode;

public class EarningsManager {
    private static final Map<UUID, PlayerData> playerData = new HashMap<>();

    public static double calculateAllowedAmount(UUID playerUUID, double amount) {
        PlayerData data = playerData.computeIfAbsent(playerUUID, k -> new PlayerData());
        data.checkPeriod();

        double max = MobMoneyMod.CONFIG.maxEarningsPerPeriod;
        if (max <= 0)
            return amount;

        double current = data.amountEarned;
        double remaining = max - current;

        // If already at or over cap, allowed is 0
        if (remaining <= 0)
            return 0;

        switch (MobMoneyMod.CONFIG.overflowMode) {
            case PARTIAL:
                // Return explicitly the smaller of the two
                return Math.min(amount, remaining);
            case ALLOW:
                // If we are under the cap (remaining > 0), we allow the FULL amount
                // even if it exceeds the remaining space.
                return amount;
            case DROP:
            default:
                // If amount fits, return it. If not, return 0.
                return (amount <= remaining) ? amount : 0;
        }
    }

    public static void addEarning(UUID playerUUID, double amount) {
        PlayerData data = playerData.computeIfAbsent(playerUUID, k -> new PlayerData());
        data.checkPeriod();
        data.amountEarned += amount;
    }

    public static boolean shouldNotify(UUID playerUUID) {
        PlayerData data = playerData.computeIfAbsent(playerUUID, k -> new PlayerData());
        long now = System.currentTimeMillis();
        // 30 seconds cooldown
        if (now - data.lastNotificationTime > 30000) {
            data.lastNotificationTime = now;
            return true;
        }
        return false;
    }

    public static long getTimeRemaining(UUID playerUUID) {
        PlayerData data = playerData.computeIfAbsent(playerUUID, k -> new PlayerData());
        data.checkPeriod();
        long now = System.currentTimeMillis();
        long durationMillis = MobMoneyMod.CONFIG.earningPeriodDuration * 1000L;
        long end = data.periodStart + durationMillis;
        return Math.max(0, (end - now) / 1000); // Return seconds
    }

    private static class PlayerData {
        long periodStart = 0;
        double amountEarned = 0;
        long lastNotificationTime = 0;

        void checkPeriod() {
            long now = System.currentTimeMillis();
            long durationMillis = MobMoneyMod.CONFIG.earningPeriodDuration * 1000L;

            if (now > periodStart + durationMillis) {
                periodStart = now;
                amountEarned = 0;
                // Don't reset notification time strictly, or maybe do?
                // If the period resets, they can earn again, so notification isn't needed until
                // they hit cap again.
                // Keeping it as is is fine.
            }
        }
    }
}
