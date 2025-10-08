package com.example.tokens;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public class TokensPlugin extends JavaPlugin {
    private TokenService tokenService;
    private VaultEco vaultEco;

    @Override
    public void onEnable() {
        saveDefaultConfig();

        getLogger().info("TokensPlugin enabling...");

        // Init token service (DB)
        tokenService = new TokenService(this,
                getConfig().getBoolean("MySQL.Use"),
                getConfig().getString("MySQL.Host"),
                getConfig().getString("MySQL.Port"),
                getConfig().getString("MySQL.Database"),
                getConfig().getString("MySQL.User"),
                getConfig().getString("MySQL.Password")
        );

        tokenService.init();

        // Register Vault economy implementation
        vaultEco = new VaultEco(tokenService, getConfig().getString("Currency.name","Tokens"));
        // Register service with Bukkit so Vault can find it
        getServer().getServicesManager().register(net.milkbowl.vault.economy.Economy.class, vaultEco, this, org.bukkit.plugin.ServicePriority.High);

        // Register PlaceholderAPI expansion
        // Only uncomment this section if PlaceholderAPI is installed on the server
        if (getServer().getPluginManager().getPlugin("PlaceholderAPI") != null) {
            new TokenPlaceholderExpansion(this).register();
            getLogger().info("TokensPlugin registered placeholders with PlaceholderAPI.");
        }

        getLogger().info("TokensPlugin enabled and hooked into Vault.");
    }

    @Override
    public void onDisable() {
        getLogger().info("TokensPlugin disabling...");
        tokenService.shutdown();
    }

    public TokenService getTokenService(){ return tokenService; }
    
    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (cmd.getName().equalsIgnoreCase("tokbalance")) {
            if (!(sender instanceof Player)) {
                sender.sendMessage("Only players can check their token balance!");
                return true;
            }
            
            Player player = (Player) sender;
            UUID playerId = player.getUniqueId();
            
            CompletableFuture<Long> balanceFuture = tokenService.getBalance(playerId);
            balanceFuture.thenAccept(balance -> {
                double multiplier = tokenService.getTokenMultiplier(playerId);
                String formattedBalance = tokenService.formatNumber(balance);
                player.sendMessage("Your token balance: " + formattedBalance);
                if (multiplier > 1.0) {
                    player.sendMessage("You have a " + multiplier + "x token multiplier!");
                }
            }).exceptionally(throwable -> {
                player.sendMessage("Error retrieving your balance!");
                getLogger().warning("Error getting balance for player " + player.getName() + ": " + throwable.getMessage());
                return null;
            });
            
            return true;
        } else if (cmd.getName().equalsIgnoreCase("givetokens")) {
            if (!sender.hasPermission("tokens.reward")) {
                sender.sendMessage("You don't have permission to use this command!");
                return true;
            }
            
            if (args.length < 2) {
                sender.sendMessage("Usage: /givetokens <player> <amount> [reason]");
                return true;
            }
            
            Player target = getServer().getPlayer(args[0]);
            if (target == null) {
                sender.sendMessage("Player not found!");
                return true;
            }
            
            try {
                long amount = Long.parseLong(args[1]);
                String reason = "Manual reward";
                if (args.length > 2) {
                    reason = String.join(" ", java.util.Arrays.copyOfRange(args, 2, args.length));
                }
                
                CompletableFuture<Long> rewardFuture = tokenService.rewardPlayer(target.getUniqueId(), amount, reason);
                rewardFuture.thenAccept(newBalance -> {
                    String formattedAmount = tokenService.formatNumber(amount);
                    String formattedBalance = tokenService.formatNumber(newBalance);
                    sender.sendMessage("Successfully rewarded " + target.getName() + " with " + formattedAmount + " tokens. New balance: " + formattedBalance);
                    target.sendMessage("You've been rewarded with " + formattedAmount + " tokens! Your new balance: " + formattedBalance);
                }).exceptionally(throwable -> {
                    sender.sendMessage("Error rewarding tokens: " + throwable.getMessage());
                    getLogger().warning("Error rewarding tokens to player " + target.getName() + ": " + throwable.getMessage());
                    return null;
                });
            } catch (NumberFormatException e) {
                sender.sendMessage("Invalid amount! Please enter a valid number.");
            }
            
            return true;
        } else if (cmd.getName().equalsIgnoreCase("token") || cmd.getName().equalsIgnoreCase("tokens")) {
            if (!(sender instanceof Player)) {
                sender.sendMessage("Only players can check their token balance!");
                return true;
            }
            
            Player player = (Player) sender;
            UUID playerId = player.getUniqueId();
            
            CompletableFuture<Long> balanceFuture = tokenService.getBalance(playerId);
            balanceFuture.thenAccept(balance -> {
                String formattedBalance = tokenService.formatNumber(balance);
                player.sendMessage("Your token balance: " + formattedBalance);
            }).exceptionally(throwable -> {
                player.sendMessage("Error retrieving your balance!");
                getLogger().warning("Error getting balance for player " + player.getName() + ": " + throwable.getMessage());
                return null;
            });
            
            return true;
        }
        return false;
    }
}