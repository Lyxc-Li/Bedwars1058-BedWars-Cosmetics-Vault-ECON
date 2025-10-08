package com.example.tokens;

import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.OfflinePlayer;
import net.milkbowl.vault.economy.Economy;

public class TokenPlaceholderExpansion extends PlaceholderExpansion {
    
    private final TokensPlugin plugin;
    
    public TokenPlaceholderExpansion(TokensPlugin plugin) {
        this.plugin = plugin;
    }
    
    @Override
    public String getIdentifier() {
        return "vault";
    }
    
    @Override
    public String getAuthor() {
        return plugin.getDescription().getAuthors().toString();
    }
    
    @Override
    public String getVersion() {
        return plugin.getDescription().getVersion();
    }
    
    @Override
    public boolean persist() {
        return true;
    }
    
    @Override
    public String onRequest(OfflinePlayer player, String params) {
        if (player == null) {
            return "";
        }
        
        Economy economy = plugin.getServer().getServicesManager().getRegistration(Economy.class).getProvider();
        
        if (params.equalsIgnoreCase("eco_balance") || params.equalsIgnoreCase("eco_balance_formatted")) {
            double balance = economy.getBalance(player);
            return economy.format(balance);
        }
        
        return null;
    }
}