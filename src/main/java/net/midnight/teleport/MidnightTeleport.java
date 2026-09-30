package net.midnight.teleport;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class MidnightTeleport extends JavaPlugin implements Listener, CommandExecutor {

    // Store death locations for /back
    private final Map<UUID, Location> deathLocations = new HashMap<>();

    // TPA Requests: Receiver UUID -> TpaRequest
    private final Map<UUID, TpaRequest> tpaRequests = new HashMap<>();

    private static class TpaRequest {
        final UUID senderUUID;
        final Location senderSnapshotLoc;

        TpaRequest(UUID senderUUID, Location senderSnapshotLoc) {
            this.senderUUID = senderUUID;
            this.senderSnapshotLoc = senderSnapshotLoc;
        }
    }

    @Override
    public void onEnable() {
        getServer().getPluginManager().registerEvents(this, this);
        if (getCommand("back") != null) getCommand("back").setExecutor(this);
        if (getCommand("tpa") != null) getCommand("tpa").setExecutor(this);
        if (getCommand("tpaccept") != null) getCommand("tpaccept").setExecutor(this);
        getLogger().info("MidnightTeleport v1.0 enabled successfully!");
    }

    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event) {
        Player player = event.getEntity();
        deathLocations.put(player.getUniqueId(), player.getLocation());
        player.sendMessage("§c[Midnight] Your death location was saved! Type §e/back §cto return.");
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Only players can use teleport commands.");
            return true;
        }

        // --- /back COMMAND ---
        if (command.getName().equalsIgnoreCase("back")) {
            Location deathLoc = deathLocations.get(player.getUniqueId());
            if (deathLoc != null) {
                player.teleport(deathLoc);
                player.sendMessage("§a[Midnight] Teleported to your last death location!");
            } else {
                player.sendMessage("§c[Midnight] No death location recorded!");
            }
            return true;
        }

        // --- /tpa COMMAND ---
        if (command.getName().equalsIgnoreCase("tpa")) {
            if (args.length < 1) {
                player.sendMessage("§cUsage: /tpa <player>");
                return true;
            }

            Player target = Bukkit.getPlayer(args[0]);
            if (target == null || !target.isOnline()) {
                player.sendMessage("§c[Midnight] Player not found or offline!");
                return true;
            }

            if (target.getUniqueId().equals(player.getUniqueId())) {
                player.sendMessage("§c[Midnight] You cannot send a TPA request to yourself!");
                return true;
            }

            // Save sender's exact current location snapshot
            tpaRequests.put(target.getUniqueId(), new TpaRequest(player.getUniqueId(), player.getLocation()));

            player.sendMessage("§a[Midnight] TPA request sent to §e" + target.getName() + "§a. Your location snapshot is locked.");
            target.sendMessage("§e[Midnight] " + player.getName() + " §ahas sent you a TPA request!");
            target.sendMessage("§aType §e/tpaccept §ato accept.");
            return true;
        }

        // --- /tpaccept COMMAND ---
        if (command.getName().equalsIgnoreCase("tpaccept")) {
            TpaRequest request = tpaRequests.remove(player.getUniqueId());

            if (request == null) {
                player.sendMessage("§c[Midnight] You have no pending TPA requests!");
                return true;
            }

            Player senderPlayer = Bukkit.getPlayer(request.senderUUID);
            if (senderPlayer == null || !senderPlayer.isOnline()) {
                player.sendMessage("§c[Midnight] The player who sent the TPA is no longer online!");
                return true;
            }

            // Teleport sender directly to target player's current location
            senderPlayer.teleport(player.getLocation());

            senderPlayer.sendMessage("§a[Midnight] " + player.getName() + " accepted your TPA request!");
            player.sendMessage("§a[Midnight] Accepted TPA request from " + senderPlayer.getName() + "!");
            return true;
        }

        return false;
    }
}
