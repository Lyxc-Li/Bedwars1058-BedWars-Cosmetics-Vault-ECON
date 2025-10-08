# TokensPlugin

A Minecraft plugin for managing player tokens with economy integration through Vault.
This Plugin is ai slop

## Features

- Player token management with MySQL database support
- Vault economy integration
- Token multipliers based on permissions
- Number formatting with commas
- PlaceholderAPI support for `%vault_eco_balance%` and `%vault_eco_balance_formatted%`

## Commands

- `/token` or `/tokens` - Check your token balance
- `/tokbalance` - Check your token balance (alternative command)
- `/rewardtokens <player> <amount> [reason]` - Reward tokens to a player (requires `tokens.reward` permission)

## Permissions

- `tokens.multiplier.1.25` - 1.25x token multiplier
- `tokens.multiplier.1.5` - 1.5x token multiplier
- `tokens.multiplier.2` - 2x token multiplier
- `tokens.reward` - Permission to use `/rewardtokens` command

## Installation

1. Place the plugin jar file in your server's `plugins` folder
2. Start the server to generate the configuration file
3. Configure your MySQL database settings in `config.yml`
4. Restart the server

## PlaceholderAPI Support

This plugin supports the following placeholders:
- `%vault_eco_balance%` - Shows player's token balance
- `%vault_eco_balance_formatted%` - Shows player's token balance with formatting (commas)

### Enabling PlaceholderAPI Support

1. Download and install [PlaceholderAPI](https://www.spigotmc.org/resources/placeholderapi.6245/) on your server
2. Uncomment the PlaceholderAPI related code in `TokensPlugin.java`:
   ```java
   // Register PlaceholderAPI expansion
   if (getServer().getPluginManager().getPlugin("PlaceholderAPI") != null) {
       new TokenPlaceholderExpansion(this).register();
       getLogger().info("TokensPlugin registered placeholders with PlaceholderAPI.");
   }
   ```
3. Uncomment the PlaceholderAPI related code in `TokenPlaceholderExpansion.java`
4. Add the PlaceholderAPI dependency to your `pom.xml`:
   ```xml
   <dependency>
       <groupId>me.clip</groupId>
       <artifactId>placeholderapi</artifactId>
       <version>2.11.2</version>
       <scope>provided</scope>
   </dependency>
   ```
5. Rebuild the plugin

## Configuration

The plugin generates a `config.yml` file with the following options:

```yaml
MySQL:
  Use: true
  Host: "localhost"
  Port: "3306"
  Database: "tokens"
  User: "username"
  Password: "password"
Currency:
  name: "Token"
```

## Building from Source

To build the plugin from source:

1. Clone the repository
2. Install Maven
3. Run `mvn clean install`
4. The compiled jar will be in the `target` directory

## Dependencies

- Spigot/Paper 1.8.8 or higher
- Vault
- MySQL database
- PlaceholderAPI
