package savage.mobmoney.listener;

import eu.pb4.common.economy.api.CommonEconomy;
import eu.pb4.common.economy.api.EconomyAccount;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.server.level.ServerPlayer;
import savage.mobmoney.MobMoneyMod;

import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.network.chat.Component;
import net.minecraft.core.registries.BuiltInRegistries;
import savage.mobmoney.config.MobMoneyConfig;
import java.math.BigInteger;

public class MobKillListener implements ServerLivingEntityEvents.AfterDeath {
    @Override
    public void afterDeath(LivingEntity entity, DamageSource damageSource) {
        if (damageSource.getEntity() instanceof ServerPlayer player) {
            String entityId = BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType()).toString();

            // Whitelist check: Only proceed if mob is in the price list
            if (!MobMoneyMod.CONFIG.mobPrices.containsKey(entityId)) {
                return;
            }

            double amount = MobMoneyMod.CONFIG.mobPrices.get(entityId);

            if (amount >= 1) {
                // Check if player has reached earning limit
                double allowedAmount = savage.mobmoney.manager.EarningsManager.calculateAllowedAmount(player.getUUID(),
                        amount);

                if (allowedAmount <= 0) {
                    if (savage.mobmoney.manager.EarningsManager.shouldNotify(player.getUUID())) {
                        long secondsLeft = savage.mobmoney.manager.EarningsManager.getTimeRemaining(player.getUUID());
                        String timeString;
                        if (secondsLeft >= 60) {
                            timeString = String.format("%dm %ds", secondsLeft / 60, secondsLeft % 60);
                        } else {
                            timeString = String.format("%ds", secondsLeft);
                        }
                        player.sendSystemMessage(Component.literal("§cLimit reached. Reset in: " + timeString));
                    }
                    MobMoneyMod.LOGGER.debug("Player {} reached earning limit.", player.getName().getString());
                    return;
                }

                // Use allowedAmount for the transaction, not the original amount
                amount = allowedAmount;
                // Use configurable currency ID
                Identifier currencyId = Identifier.fromNamespaceAndPath(MobMoneyMod.CONFIG.economyProvider,
                        MobMoneyMod.CONFIG.currencyId);

                // Verbose debugging with correct lookup
                // 1. Get Provider by Namespace
                var provider = CommonEconomy.getProvider(currencyId.getNamespace());
                if (provider == null) {
                    MobMoneyMod.LOGGER.warn("Provider not found with ID: {}", currencyId.getNamespace());
                    return;
                }

                // 2. Get Currency from Provider
                var currency = provider.getCurrency(((ServerLevel) player.level()).getServer(), currencyId.getPath());
                if (currency == null) {
                    // Try full ID if path fails
                    currency = provider.getCurrency(((ServerLevel) player.level()).getServer(), currencyId.toString());
                }

                if (currency == null) {
                    MobMoneyMod.LOGGER.warn("Currency not found: {} in provider {}", currencyId.getPath(),
                            currencyId.getNamespace());
                    return;
                }

                // 3. Get Default Account ID
                var accountId = provider.defaultAccount(((ServerLevel) player.level()).getServer(), player.getGameProfile(), currency);
                if (accountId == null) {
                    MobMoneyMod.LOGGER.warn("Default account ID is null for player {}", player.getName().getString());
                    return;
                }

                // 4. Get Account
                var account = provider.getAccount(((ServerLevel) player.level()).getServer(), player.getGameProfile(), accountId);

                if (account != null) {
                    try {
                        BigInteger rawAmount = currency.parseValue(String.valueOf(amount));
                        account.increaseBalance(rawAmount);
                        boolean success = true; // Assume success if no exception
                        MobMoneyMod.LOGGER.info("Awarded ${} to {} for killing {}", amount,
                                player.getName().getString(), entityId);

                        if (success) {
                            savage.mobmoney.manager.EarningsManager.addEarning(player.getUUID(), amount);

                            if (MobMoneyMod.CONFIG.notificationMode != MobMoneyConfig.NotificationMode.NONE) {
                                String message = String.format("You earned %s%.2f for killing %s",
                                        currencyId.getPath().equals("dollar") ? "$" : "",
                                        amount,
                                        entity.getType().getDescription().getString());

                                boolean overlay = (MobMoneyMod.CONFIG.notificationMode == MobMoneyConfig.NotificationMode.ACTION_BAR);
                                player.sendSystemMessage(Component.literal(message), overlay);
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
