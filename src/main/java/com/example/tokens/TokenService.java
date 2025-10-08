package com.example.tokens;

import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.Bukkit;
import org.bukkit.permissions.Permission;

import java.sql.*;
import java.text.NumberFormat;
import java.util.Locale;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

public class TokenService {
    private JavaPlugin plugin;
    private Connection connection;
    private boolean useMySQL;
    private String host, port, database, user, password;
    private NumberFormat numberFormatter;

    public TokenService(JavaPlugin plugin, boolean useMySQL, String host, String port, String database, String user, String password) {
        this.plugin = plugin;
        this.useMySQL = useMySQL;
        this.host = host;
        this.port = port;
        this.database = database;
        this.user = user;
        this.password = password;
        this.numberFormatter = NumberFormat.getInstance(Locale.US); // For comma formatting
    }

    public void init() {
        try {
            if (useMySQL) {
                // Connect to MySQL
                connection = DriverManager.getConnection(
                        "jdbc:mysql://" + host + ":" + port + "/" + database,
                        user,
                        password
                );
                // Create table if not exists
                try (PreparedStatement ps = connection.prepareStatement(
                        "CREATE TABLE IF NOT EXISTS tokens (" +
                                "uuid VARCHAR(36) PRIMARY KEY," +
                                "balance BIGINT NOT NULL DEFAULT 0" +
                                ")"
                )) {
                    ps.executeUpdate();
                }
                
                // Create transaction log table
                try (PreparedStatement ps = connection.prepareStatement(
                        "CREATE TABLE IF NOT EXISTS token_transactions (" +
                                "id INT AUTO_INCREMENT PRIMARY KEY," +
                                "uuid VARCHAR(36) NOT NULL," +
                                "amount BIGINT NOT NULL," +
                                "balance_before BIGINT NOT NULL," +
                                "balance_after BIGINT NOT NULL," +
                                "reason VARCHAR(255)," +
                                "timestamp TIMESTAMP DEFAULT CURRENT_TIMESTAMP" +
                                ")"
                )) {
                    ps.executeUpdate();
                }
            } else {
                // SQLite implementation could go here
            }
            
            // Register permissions
            registerPermissions();
        } catch (Exception e) {
            plugin.getLogger().severe("Failed to initialize TokenService: " + e.getMessage());
            e.printStackTrace();
        }
    }

    // sync read
    public long getBalanceSync(UUID uuid) {
        try (PreparedStatement ps = connection.prepareStatement("SELECT balance FROM tokens WHERE uuid = ?")) {
            ps.setString(1, uuid.toString());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getLong(1);
            }
        } catch (Exception e) {
            plugin.getLogger().warning("getBalanceSync error: " + e.getMessage());
        }
        return 0L;
    }

    // async read
    public CompletableFuture<Long> getBalance(UUID uuid) {
        return CompletableFuture.supplyAsync(() -> getBalanceSync(uuid), getAsyncExecutor());
    }

    // sync set
    public void setBalanceSync(UUID uuid, long amount, String reason) {
        long oldBalance = getBalanceSync(uuid);
        try (PreparedStatement ps = connection.prepareStatement("INSERT INTO tokens(uuid,balance) VALUES(?,?) ON DUPLICATE KEY UPDATE balance = ?")) {
            ps.setString(1, uuid.toString());
            ps.setLong(2, amount);
            ps.setLong(3, amount);
            ps.executeUpdate();
            
            // Log transaction
            logTransaction(uuid, 0, oldBalance, amount, reason);
        } catch (Exception e) {
            plugin.getLogger().warning("setBalanceSync error: " + e.getMessage());
        }
    }

    public CompletableFuture<Void> setBalance(UUID uuid, long amount, String reason) {
        return CompletableFuture.runAsync(() -> setBalanceSync(uuid, amount, reason), getAsyncExecutor());
    }

    // add/remove atomically (sync)
    public long addSync(UUID uuid, long delta, String reason) {
        long current = getBalanceSync(uuid);
        long next = Math.max(0L, current + delta);
        setBalanceSync(uuid, next, reason);
        return next;
    }

    public CompletableFuture<Long> add(UUID uuid, long delta, String reason) {
        return CompletableFuture.supplyAsync(() -> addSync(uuid, delta, reason), getAsyncExecutor());
    }

    // force withdraw used by server/shop systems that are allowed to deduct tokens
    public boolean removeSync(UUID uuid, long amount, String reason) {
        long current = getBalanceSync(uuid);
        if (current < amount) return false;
        setBalanceSync(uuid, current - amount, reason);
        return true;
    }

    public CompletableFuture<Boolean> remove(UUID uuid, long amount, String reason) {
        return CompletableFuture.supplyAsync(() -> removeSync(uuid, amount, reason), getAsyncExecutor());
    }

    // Public method for rewarding players with tokens
    public CompletableFuture<Long> rewardPlayer(UUID uuid, long amount, String reason) {
        return add(uuid, amount, reason);
    }
    
    // Apply token multiplier based on permissions
    public double getTokenMultiplier(UUID uuid) {
        org.bukkit.entity.Player player = Bukkit.getPlayer(uuid);
        if (player == null) return 1.0;
        
        // Check for multiplier permissions
        if (player.hasPermission("tokens.multiplier.2")) {
            return 2.0;
        } else if (player.hasPermission("tokens.multiplier.1.5")) {
            return 1.5;
        } else if (player.hasPermission("tokens.multiplier.1.25")) {
            return 1.25;
        }
        return 1.0; // Default multiplier
    }
    
    // Format number with commas
    public String formatNumber(long number) {
        return numberFormatter.format(number);
    }

    public void shutdown() {
        try {
            if (connection != null && !connection.isClosed()) connection.close();
        } catch (Exception ignored) {
        }
    }
    
    // Custom method to get an async executor
    private Executor getAsyncExecutor() {
        return Runnable::run; // Run on the same thread for now
        // In a more advanced implementation, you might want to use Bukkit's scheduler
        // or create a proper thread pool
    }
    
    // Log transactions to database
    private void logTransaction(UUID uuid, long amount, long balanceBefore, long balanceAfter, String reason) {
        try (PreparedStatement ps = connection.prepareStatement(
                "INSERT INTO token_transactions (uuid, amount, balance_before, balance_after, reason) VALUES (?, ?, ?, ?, ?)")) {
            ps.setString(1, uuid.toString());
            ps.setLong(2, amount);
            ps.setLong(3, balanceBefore);
            ps.setLong(4, balanceAfter);
            ps.setString(5, reason);
            ps.executeUpdate();
        } catch (Exception e) {
            plugin.getLogger().warning("Failed to log transaction: " + e.getMessage());
        }
    }
    
    // Register permissions
    private void registerPermissions() {
        try {
            Bukkit.getPluginManager().addPermission(new Permission("tokens.multiplier.1.25"));
            Bukkit.getPluginManager().addPermission(new Permission("tokens.multiplier.1.5"));
            Bukkit.getPluginManager().addPermission(new Permission("tokens.multiplier.2"));
        } catch (Exception e) {
            // Permissions might already be registered
            plugin.getLogger().info("Permissions may already be registered.");
        }
    }
}