package savage.mobmoney.api;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.LivingEntity;

/**
 * Immutable snapshot of a mob kill passed to {@link MobMoneyEvents} listeners.
 * New fields may be added in future versions; construct via the mod internals only.
 */
public record MobKillContext(LivingEntity mob, ServerPlayer killer, EntitySpawnReason spawnReason,
        String entityId) {
}
