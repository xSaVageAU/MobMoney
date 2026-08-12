package savage.mobmoney.attachment;

import com.mojang.serialization.Codec;
import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.EntitySpawnReason;
import savage.mobmoney.MobMoneyMod;

public final class ModAttachments {
    private static final Codec<EntitySpawnReason> SPAWN_REASON_CODEC = Codec.STRING
            .xmap(EntitySpawnReason::valueOf, Enum::name);

    public static final AttachmentType<EntitySpawnReason> SPAWN_REASON = AttachmentRegistry.createPersistent(
            Identifier.fromNamespaceAndPath(MobMoneyMod.MOD_ID, "spawn_reason"),
            SPAWN_REASON_CODEC);

    private ModAttachments() {
    }

    public static void init() {
        // Referencing SPAWN_REASON above triggers class loading, which registers the
        // attachment type. This method exists purely to force that to happen eagerly
        // during mod init rather than whenever the mixin first touches the class.
    }
}
