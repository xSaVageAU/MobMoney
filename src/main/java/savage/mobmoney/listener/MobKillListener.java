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
                // Use configurable currency ID
                Identifier currencyId = Identifier.of(MobMoneyMod.CONFIG.economyProvider, MobMoneyMod.CONFIG.currencyId);
                
                // Verbose debugging with correct lookup
                // 1. Get Provider by Namespace
                var provider = CommonEconomy.getProvider(currencyId.getNamespace());
                if (provider == null) {
                    MobMoneyMod.LOGGER.warn("Provider not found with ID: {}", currencyId.getNamespace());
                    // Fallback: Try to find provider that handles this currency? 
                    // The API doesn't expose a simple "get provider for currency" without iterating.
                    return;
                }
                
                // 2. Get Currency from Provider
                var currency = provider.getCurrency(player.getCommandSource().getWorld().getServer(), currencyId.getPath());
                if (currency == null) {
                    // Try full ID if path fails
                    currency = provider.getCurrency(player.getCommandSource().getWorld().getServer(), currencyId.toString());
                }
                
                if (currency == null) {
                    MobMoneyMod.LOGGER.warn("Currency not found: {} in provider {}", currencyId.getPath(), currencyId.getNamespace());
                    return;
                }
                
                // 3. Get Default Account ID
                var accountId = provider.defaultAccount(player.getCommandSource().getWorld().getServer(), player.getGameProfile(), currency);
                if (accountId == null) {
                    MobMoneyMod.LOGGER.warn("Default account ID is null for player {}", player.getName().getString());
                    return;
                }
                
                // 4. Get Account
                var account = provider.getAccount(player.getCommandSource().getWorld().getServer(), player.getGameProfile(), accountId);
                
                if (account != null) {
                    // Note: SavsCommonEconomy treats this value as whole currency units (e.g. Dollars), not cents.
                    account.increaseBalance((long) amount);
                    MobMoneyMod.LOGGER.info("Awarded ${} to {} for killing {}", amount, player.getName().getString(), entityId);
                } else {
                    MobMoneyMod.LOGGER.warn("Failed to get account for {} with ID {}", player.getName().getString(), accountId);
                }
            } else {
                 MobMoneyMod.LOGGER.info("Mob {} is worth ${}, skipping", entityId, amount);
            }
        }
    }
}
