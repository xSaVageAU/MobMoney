package savage.mobmoney.listener;

import eu.pb4.common.economy.api.CommonEconomy;
import eu.pb4.common.economy.api.EconomyAccount;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.server.network.ServerPlayerEntity;
import savage.mobmoney.MobMoneyMod;

import net.minecraft.util.Identifier;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import savage.mobmoney.config.MobMoneyConfig;

public class MobKillListener implements ServerLivingEntityEvents.AfterDeath {
    @Override
    public void afterDeath(LivingEntity entity, DamageSource damageSource) {
        if (damageSource.getAttacker() instanceof ServerPlayerEntity player) {
            String entityId = net.minecraft.registry.Registries.ENTITY_TYPE.getId(entity.getType()).toString();

            // Whitelist check: Only proceed if mob is in the price list
            if (!MobMoneyMod.CONFIG.mobPrices.containsKey(entityId)) {
                return;
            }

            double amount = MobMoneyMod.CONFIG.mobPrices.get(entityId);

            if (amount >= 1) {
                // Check if player has reached earning limit
                if (!savage.mobmoney.manager.EarningsManager.canEarn(player.getUuid(), amount)) {
                    if (savage.mobmoney.manager.EarningsManager.shouldNotify(player.getUuid())) {
                        long secondsLeft = savage.mobmoney.manager.EarningsManager.getTimeRemaining(player.getUuid());
                        String timeString;
                        if (secondsLeft >= 60) {
                            timeString = String.format("%dm %ds", secondsLeft / 60, secondsLeft % 60);
                        } else {
                            timeString = String.format("%ds", secondsLeft);
                        }
                        player.sendMessage(Text.of("§cLimit reached. Reset in: " + timeString), true);
                    }
                    MobMoneyMod.LOGGER.debug("Player {} reached earning limit.", player.getName().getString());
                    return;
                }
                // Use configurable currency ID
                Identifier currencyId = Identifier.of(MobMoneyMod.CONFIG.economyProvider,
                        MobMoneyMod.CONFIG.currencyId);

                // Verbose debugging with correct lookup
                // 1. Get Provider by Namespace
                var provider = CommonEconomy.getProvider(currencyId.getNamespace());
                if (provider == null) {
                    MobMoneyMod.LOGGER.warn("Provider not found with ID: {}", currencyId.getNamespace());
                    return;
                }

                // 2. Get Currency from Provider
                var currency = provider.getCurrency(player.getCommandSource().getWorld().getServer(),
                        currencyId.getPath());
                if (currency == null) {
                    // Try full ID if path fails
                    currency = provider.getCurrency(player.getCommandSource().getWorld().getServer(),
                            currencyId.toString());
                }

                if (currency == null) {
                    MobMoneyMod.LOGGER.warn("Currency not found: {} in provider {}", currencyId.getPath(),
                            currencyId.getNamespace());
                    return;
                }

                // 3. Get Default Account ID
                var accountId = provider.defaultAccount(player.getCommandSource().getWorld().getServer(),
                        player.getGameProfile(), currency);
                if (accountId == null) {
                    MobMoneyMod.LOGGER.warn("Default account ID is null for player {}", player.getName().getString());
                    return;
                }

                // 4. Get Account
                var account = provider.getAccount(player.getCommandSource().getWorld().getServer(),
                        player.getGameProfile(), accountId);

                if (account != null) {
                    try {
                        account.increaseBalance((long) amount);
                        boolean success = true; // Assume success if no exception
                        MobMoneyMod.LOGGER.info("Awarded ${} to {} for killing {}", amount,
                                player.getName().getString(), entityId);

                        if (success) {
                            savage.mobmoney.manager.EarningsManager.addEarning(player.getUuid(), amount);

                            if (MobMoneyMod.CONFIG.notificationMode != MobMoneyConfig.NotificationMode.NONE) {
                                String message = String.format("You earned %s%.2f for killing %s",
                                        currencyId.getPath().equals("dollar") ? "$" : "",
                                        amount,
                                        entity.getType().getName().getString());

                                if (MobMoneyMod.CONFIG.notificationMode == MobMoneyConfig.NotificationMode.ACTION_BAR) {
                                    player.sendMessage(Text.of(message), true);
                                } else {
                                    player.sendMessage(Text.of(message), false);
                                }
                            }
                        } else {
                            MobMoneyMod.LOGGER.error("Failed to deposit money for player: {}",
                                    player.getName().getString());
                        }
                    } catch (Exception e) {
                        MobMoneyMod.LOGGER.error("Error depositing money for player {}: {}",
                                player.getName().getString(), e.getMessage());
                    }
                } else {
                    MobMoneyMod.LOGGER.warn("Failed to get account for {} with ID {}", player.getName().getString(),
                            accountId);
                }
            } else {
                MobMoneyMod.LOGGER.info("Mob {} is worth ${}, skipping", entityId, amount);
            }
        }
    }
}
