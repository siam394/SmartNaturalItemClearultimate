package me.siam.smartclear;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Item;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.HashSet;
import java.util.Set;

public class SmartNaturalItemClear extends JavaPlugin implements Listener {

    private final Set<Item> protectedItems = new HashSet<>();
    private FileConfiguration config;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        config = getConfig();
        Bukkit.getPluginManager().registerEvents(this, this);
        startClearTask();
    }

    @EventHandler
    public void onPlayerDrop(PlayerDropItemEvent event) {
        protectedItems.add(event.getItemDrop());
    }

    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event) {
        Bukkit.getScheduler().runTaskLater(this, () -> {
            event.getEntity().getWorld().getEntitiesByClass(Item.class)
                    .forEach(protectedItems::add);
        }, 1L);
    }

    private void startClearTask() {
        int interval = config.getInt("clear-interval-seconds") * 20;

        new BukkitRunnable() {
            @Override
            public void run() {

                int removed = 0;

                for (var world : Bukkit.getWorlds()) {
                    for (Item item : world.getEntitiesByClass(Item.class)) {

                        if (protectedItems.contains(item)) continue;

                        if (isWhitelisted(item.getItemStack().getType())) continue;

                        item.remove();
                        removed++;
                    }
                }

                protectedItems.clear();

                Bukkit.broadcastMessage(
                        config.getString("messages.cleared")
                                .replace("%amount%", String.valueOf(removed))
                );
            }
        }.runTaskTimer(this, interval, interval);

        startWarnings();
    }

    private boolean isWhitelisted(Material material) {
        return config.getStringList("whitelist").contains(material.name());
    }

    private void startWarnings() {
        int interval = config.getInt("clear-interval-seconds");

        new BukkitRunnable() {
            int countdown = interval;

            @Override
            public void run() {

                countdown--;

                if (countdown == 60)
                    Bukkit.broadcastMessage(config.getString("messages.warn60"));

                if (countdown == 30)
                    Bukkit.broadcastMessage(config.getString("messages.warn30"));

                if (countdown <= 10 && countdown >= 5)
                    Bukkit.broadcastMessage(
                            config.getString("messages.countdown")
                                    .replace("%time%", String.valueOf(countdown))
                    );

                if (countdown <= 0)
                    countdown = interval;
            }
        }.runTaskTimer(this, 20, 20);
    }
}
