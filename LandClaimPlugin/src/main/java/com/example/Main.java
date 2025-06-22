package com.example;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.event.EventHandler;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.*;

public class Main extends JavaPlugin implements Listener {

    private static final int CLAIM_RADIUS = 10;

    // Claim-Daten
    private static class Claim {
        private final UUID owner;
        private final Location center;
        private final Set<UUID> members = new HashSet<>();

        public Claim(UUID owner, Location center) {
            this.owner = owner;
            this.center = center;
            members.add(owner);
        }

        public UUID getOwner() {
            return owner;
        }

        public Location getCenter() {
            return center;
        }

        public Set<UUID> getMembers() {
            return members;
        }

        public void addMember(UUID uuid) {
            members.add(uuid);
        }

        public void removeMember(UUID uuid) {
            members.remove(uuid);
        }
    }

    private final Map<UUID, Claim> claims = new HashMap<>();
    private final Map<UUID, UUID> playerCurrentClaim = new HashMap<>();

    @Override
    public void onEnable() {
        Bukkit.getPluginManager().registerEvents(this, this);
        getLogger().info("LandClaimPlugin aktiviert!");
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("Nur Spieler können diesen Befehl benutzen.");
            return true;
        }
        Player player = (Player) sender;
        UUID playerId = player.getUniqueId();

        if (args.length == 0) {
            if (claims.containsKey(playerId)) {
                player.sendMessage(ChatColor.RED + "Du hast bereits ein Gebiet beansprucht.");
                return true;
            }
            Location loc = player.getLocation();
            Claim claim = new Claim(playerId, loc);
            claims.put(playerId, claim);
            player.sendMessage(ChatColor.GREEN + "Du hast dieses Gebiet erfolgreich beansprucht!");
            return true;
        }

        String sub = args[0].toLowerCase();

        switch (sub) {
            case "add":
                if (args.length < 2) {
                    player.sendMessage(ChatColor.RED + "Benutzung: /claim add <Spieler>");
                    return true;
                }
                Claim claimAdd = claims.get(playerId);
                if (claimAdd == null || !claimAdd.getOwner().equals(playerId)) {
                    player.sendMessage(ChatColor.RED + "Nur der Besitzer kann Mitglieder hinzufügen.");
                    return true;
                }
                Player targetAdd = Bukkit.getPlayerExact(args[1]);
                if (targetAdd == null) {
                    player.sendMessage(ChatColor.RED + "Spieler nicht gefunden.");
                    return true;
                }
                claimAdd.addMember(targetAdd.getUniqueId());
                player.sendMessage(ChatColor.GREEN + targetAdd.getName() + " wurde hinzugefügt.");
                return true;

            case "remove":
                if (args.length < 2) {
                    player.sendMessage(ChatColor.RED + "Benutzung: /claim remove <Spieler>");
                    return true;
                }
                Claim claimRemove = claims.get(playerId);
                if (claimRemove == null || !claimRemove.getOwner().equals(playerId)) {
                    player.sendMessage(ChatColor.RED + "Nur der Besitzer kann Mitglieder entfernen.");
                    return true;
                }
                Player targetRemove = Bukkit.getPlayerExact(args[1]);
                if (targetRemove == null) {
                    player.sendMessage(ChatColor.RED + "Spieler nicht gefunden.");
                    return true;
                }
                claimRemove.removeMember(targetRemove.getUniqueId());
                player.sendMessage(ChatColor.GREEN + targetRemove.getName() + " wurde entfernt.");
                return true;

            case "delete":
                if (claims.remove(playerId) != null) {
                    player.sendMessage(ChatColor.GREEN + "Dein Claim wurde gelöscht.");
                } else {
                    player.sendMessage(ChatColor.RED + "Du hast keinen Claim.");
                }
                return true;

            default:
                player.sendMessage(ChatColor.RED + "Unbekannter Unterbefehl.");
                return true;
        }
    }

    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        Player breaker = event.getPlayer();
        Location blockLoc = event.getBlock().getLocation();

        for (Claim claim : claims.values()) {
            if (blockLoc.getWorld().equals(claim.getCenter().getWorld())
                    && blockLoc.distance(claim.getCenter()) <= CLAIM_RADIUS) {
                if (!claim.getMembers().contains(breaker.getUniqueId())) {
                    breaker.sendMessage(ChatColor.RED + "Du darfst hier nichts abbauen!");
                    event.setCancelled(true);
                }
                return;
            }
        }
    }

    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        Location to = event.getTo();
        if (to == null) return;

        UUID currentOwner = null;
        for (Claim claim : claims.values()) {
            if (to.getWorld().equals(claim.getCenter().getWorld())
                    && to.distance(claim.getCenter()) <= CLAIM_RADIUS) {
                currentOwner = claim.getOwner();
                break;
            }
        }

        UUID lastOwner = playerCurrentClaim.get(player.getUniqueId());

        if (!Objects.equals(currentOwner, lastOwner)) {
            playerCurrentClaim.put(player.getUniqueId(), currentOwner);
            if (currentOwner != null) {
                Player owner = Bukkit.getPlayer(currentOwner);
                String ownerName = (owner != null) ? owner.getName() : "jemandem";
                player.sendMessage(ChatColor.YELLOW + "Du betrittst das Grundstück von " + ownerName + ".");
            } else {
                player.sendMessage(ChatColor.YELLOW + "Du hast das Grundstück verlassen.");
            }
        }
    }
}
