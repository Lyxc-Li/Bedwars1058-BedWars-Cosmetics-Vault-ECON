package com.example.tokens;

import net.milkbowl.vault.economy.AbstractEconomy;
import net.milkbowl.vault.economy.EconomyResponse;
import net.milkbowl.vault.economy.EconomyResponse.ResponseType;
import org.bukkit.OfflinePlayer;

import java.text.NumberFormat;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

public class VaultEco extends AbstractEconomy {
    private final TokenService service;
    private final String currencyName;
    private final NumberFormat formatter;

    public VaultEco(TokenService service, String currencyName) {
        this.service = service;
        this.currencyName = currencyName;
        this.formatter = NumberFormat.getInstance(); // For formatting currency values
    }

    /* ---------- Basic info ---------- */
    @Override
    public String currencyNamePlural() {
        return currencyName + "s";
    }

    @Override
    public String currencyNameSingular() {
        return currencyName;
    }
    
    @Override
    public String getName() {
        return "TokensPlugin";
    }
    
    @Override
    public boolean isEnabled() {
        return true;
    }

    @Override
    public boolean hasBankSupport() {
        return false;
    }

    @Override
    public int fractionalDigits() {
        return 0;
    }

    @Override
    public String format(double amount) {
        // amount treated as whole tokens
        long v = (long) Math.floor(amount);
        return service.formatNumber(v);
    }

    @Override
    public boolean hasAccount(String playerName) {
        return true;
    }

    @Override
    public boolean hasAccount(OfflinePlayer player) {
        return true;
    }
    
    public boolean hasAccount(String playerName, String worldName) {
        return true;
    }
    
    public boolean hasAccount(OfflinePlayer player, String worldName) {
        return true;
    }

    @Override
    public boolean has(String playerName, double amount) {
        // sync check; Vault may call this synchronously. Keep fast.
        try {
            UUID u = java.util.UUID.nameUUIDFromBytes(playerName.getBytes());
            return getBalanceSync(u) >= (long) Math.ceil(amount);
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    public boolean has(OfflinePlayer player, double amount) {
        return getBalanceSync(player.getUniqueId()) >= (long) Math.ceil(amount);
    }

    public boolean has(String playerName, String worldName, double amount) {
        return has(playerName, amount);
    }

    public boolean has(OfflinePlayer player, String worldName, double amount) {
        return has(player, amount);
    }

    private long getBalanceSync(UUID uuid) {
        return service.getBalanceSync(uuid);
    }

    @Override
    public double getBalance(String playerName) {
        try {
            UUID u = java.util.UUID.nameUUIDFromBytes(playerName.getBytes());
            return (double) getBalanceSync(u);
        } catch (Exception e) {
            return 0d;
        }
    }

    @Override
    public double getBalance(OfflinePlayer player) {
        return (double) getBalanceSync(player.getUniqueId());
    }

    public double getBalance(String playerName, String worldName) {
        return getBalance(playerName);
    }

    public double getBalance(OfflinePlayer player, String worldName) {
        return getBalance(player);
    }

    /* ---------- Deposits/Withdrawals ---------- */
    // We now allow withdrawal operations to support plugins like Cosmetics

    @Override
    public EconomyResponse withdrawPlayer(String playerName, double amount) {
        try {
            UUID uuid = java.util.UUID.nameUUIDFromBytes(playerName.getBytes());
            long actualAmount = (long) Math.ceil(amount);
            boolean success = service.removeSync(uuid, actualAmount, "Withdrawal by " + playerName);
            if (success) {
                long balance = getBalanceSync(uuid);
                return new EconomyResponse(actualAmount, balance, ResponseType.SUCCESS, "Withdrawal successful");
            } else {
                long balance = getBalanceSync(uuid);
                return new EconomyResponse(0, balance, ResponseType.FAILURE, "Insufficient funds");
            }
        } catch (Exception e) {
            return new EconomyResponse(0, 0, ResponseType.FAILURE, "Withdrawal error: " + e.getMessage());
        }
    }

    @Override
    public EconomyResponse withdrawPlayer(OfflinePlayer player, double amount) {
        try {
            UUID uuid = player.getUniqueId();
            long actualAmount = (long) Math.ceil(amount);
            boolean success = service.removeSync(uuid, actualAmount, "Withdrawal by " + player.getName());
            if (success) {
                long balance = getBalanceSync(uuid);
                return new EconomyResponse(actualAmount, balance, ResponseType.SUCCESS, "Withdrawal successful");
            } else {
                long balance = getBalanceSync(uuid);
                return new EconomyResponse(0, balance, ResponseType.FAILURE, "Insufficient funds");
            }
        } catch (Exception e) {
            return new EconomyResponse(0, 0, ResponseType.FAILURE, "Withdrawal error: " + e.getMessage());
        }
    }
    
    public EconomyResponse withdrawPlayer(String playerName, String worldName, double amount) {
        return withdrawPlayer(playerName, amount);
    }
    
    public EconomyResponse withdrawPlayer(OfflinePlayer player, String worldName, double amount) {
        return withdrawPlayer(player, amount);
    }

    @Override
    public EconomyResponse depositPlayer(String playerName, double amount) {
        try {
            UUID uuid = java.util.UUID.nameUUIDFromBytes(playerName.getBytes());
            double multiplier = service.getTokenMultiplier(uuid);
            long actualAmount = (long) (amount * multiplier);
            boolean success = service.removeSync(uuid, -actualAmount, "Deposit by " + playerName);
            if (success) {
                long balance = getBalanceSync(uuid);
                return new EconomyResponse(actualAmount, balance, ResponseType.SUCCESS, "Deposit successful with " + multiplier + "x multiplier");
            } else {
                long balance = getBalanceSync(uuid);
                return new EconomyResponse(0, balance, ResponseType.FAILURE, "Deposit failed");
            }
        } catch (Exception e) {
            return new EconomyResponse(0, 0, ResponseType.FAILURE, "Deposit error: " + e.getMessage());
        }
    }

    @Override
    public EconomyResponse depositPlayer(OfflinePlayer player, double amount) {
        try {
            UUID uuid = player.getUniqueId();
            double multiplier = service.getTokenMultiplier(uuid);
            long actualAmount = (long) (amount * multiplier);
            boolean success = service.removeSync(uuid, -actualAmount, "Deposit by " + player.getName());
            if (success) {
                long balance = getBalanceSync(uuid);
                return new EconomyResponse(actualAmount, balance, ResponseType.SUCCESS, "Deposit successful with " + multiplier + "x multiplier");
            } else {
                long balance = getBalanceSync(uuid);
                return new EconomyResponse(0, balance, ResponseType.FAILURE, "Deposit failed");
            }
        } catch (Exception e) {
            return new EconomyResponse(0, 0, ResponseType.FAILURE, "Deposit error: " + e.getMessage());
        }
    }
    
    public EconomyResponse depositPlayer(String playerName, String worldName, double amount) {
        return depositPlayer(playerName, amount);
    }
    
    public EconomyResponse depositPlayer(OfflinePlayer player, String worldName, double amount) {
        return depositPlayer(player, amount);
    }

    /* ---------- Banks (not supported) ---------- */
    @Override
    public EconomyResponse createBank(String name, String player) {
        return new EconomyResponse(0, 0, ResponseType.NOT_IMPLEMENTED, "Banks not supported");
    }

    @Override
    public EconomyResponse createBank(String name, OfflinePlayer player) {
        return new EconomyResponse(0, 0, ResponseType.NOT_IMPLEMENTED, "Banks not supported");
    }

    @Override
    public EconomyResponse deleteBank(String name) {
        return new EconomyResponse(0, 0, ResponseType.NOT_IMPLEMENTED, "Banks not supported");
    }

    @Override
    public EconomyResponse bankBalance(String name) {
        return new EconomyResponse(0, 0, ResponseType.FAILURE, "Banks not supported");
    }

    @Override
    public EconomyResponse bankHas(String name, double amount) {
        return new EconomyResponse(0, 0, ResponseType.FAILURE, "Banks not supported");
    }

    @Override
    public EconomyResponse bankWithdraw(String name, double amount) {
        return new EconomyResponse(0, 0, ResponseType.FAILURE, "Banks not supported");
    }

    @Override
    public EconomyResponse bankDeposit(String name, double amount) {
        return new EconomyResponse(0, 0, ResponseType.FAILURE, "Banks not supported");
    }

    @Override
    public EconomyResponse isBankOwner(String name, String playerName) {
        return new EconomyResponse(0, 0, ResponseType.NOT_IMPLEMENTED, "Banks not supported");
    }

    @Override
    public EconomyResponse isBankMember(String name, String playerName) {
        return new EconomyResponse(0, 0, ResponseType.NOT_IMPLEMENTED, "Banks not supported");
    }
    
    public EconomyResponse isBankOwner(String name, OfflinePlayer player) {
        return new EconomyResponse(0, 0, ResponseType.NOT_IMPLEMENTED, "Banks not supported");
    }
    
    public EconomyResponse isBankMember(String name, OfflinePlayer player) {
        return new EconomyResponse(0, 0, ResponseType.NOT_IMPLEMENTED, "Banks not supported");
    }

    /* ---------- Other ---------- */
    @Override
    public boolean createPlayerAccount(String playerName) {
        return true;
    }

    @Override
    public boolean createPlayerAccount(OfflinePlayer player) {
        return true;
    }

    public boolean createPlayerAccount(String playerName, String worldName) {
        return true;
    }

    public boolean createPlayerAccount(OfflinePlayer player, String worldName) {
        return true;
    }
    
    // Additional methods that may be required by some Vault versions
    @Override
    public List<String> getBanks() {
        return Collections.emptyList();
    }
    
    public EconomyResponse createBank(String name, String player, String worldName) {
        return new EconomyResponse(0, 0, ResponseType.NOT_IMPLEMENTED, "Banks not supported");
    }
    
    public EconomyResponse createBank(String name, OfflinePlayer player, String worldName) {
        return new EconomyResponse(0, 0, ResponseType.NOT_IMPLEMENTED, "Banks not supported");
    }
    
    public EconomyResponse deleteBank(String name, String worldName) {
        return new EconomyResponse(0, 0, ResponseType.NOT_IMPLEMENTED, "Banks not supported");
    }
    
    public EconomyResponse bankBalance(String name, String worldName) {
        return new EconomyResponse(0, 0, ResponseType.FAILURE, "Banks not supported");
    }
    
    public EconomyResponse bankHas(String name, String worldName, double amount) {
        return new EconomyResponse(0, 0, ResponseType.FAILURE, "Banks not supported");
    }
    
    public EconomyResponse bankWithdraw(String name, String worldName, double amount) {
        return new EconomyResponse(0, 0, ResponseType.FAILURE, "Banks not supported");
    }
    
    public EconomyResponse bankDeposit(String name, String worldName, double amount) {
        return new EconomyResponse(0, 0, ResponseType.FAILURE, "Banks not supported");
    }
    
    public EconomyResponse isBankOwner(String name, String playerName, String worldName) {
        return new EconomyResponse(0, 0, ResponseType.NOT_IMPLEMENTED, "Banks not supported");
    }
    
    public EconomyResponse isBankMember(String name, String playerName, String worldName) {
        return new EconomyResponse(0, 0, ResponseType.NOT_IMPLEMENTED, "Banks not supported");
    }

    // The rest of the Economy interface methods for your Vault version may need to be implemented. Add them as no-op or unsupported responses similarly.
}
