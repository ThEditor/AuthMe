package me.theditor.authme;

import org.bukkit.ChatColor;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.HashMap;
import java.util.function.Consumer;

public class Main extends JavaPlugin implements Listener {

    private HashMap<Player, Consumer<AsyncPlayerChatEvent>> awaitingLogin = new HashMap<>();

    @Override
    public void onEnable() {
        this.loadConfigs();
        this.getServer().getPluginManager().registerEvents(this, this);
    }

    @Override
    public void onDisable() {
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent e) {
        e.setJoinMessage("");
        Player player = e.getPlayer();
        player.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, Integer.MAX_VALUE, 3));
        player.setWalkSpeed(0f);

        player.sendMessage(translate("&eEnter your password to login\nIf not registered, enter a password to register"));
        this.awaitMessages(player, (e1) -> {
            e1.setCancelled(true);
            String passHash = this.getConfig().getString(player.getUniqueId().toString());
            if(passHash == null) {
                // Not Registered
                String hash = BCrypt.hashpw(e1.getMessage(), BCrypt.gensalt());
                this.getConfig().set(player.getUniqueId().toString(), hash);
                this.saveConfig();
                player.sendMessage(translate("&cSuccessfuly registered!"));
                new BukkitRunnable() {
                    @Override
                    public void run() {
                        login(player);
                    }
                }.runTask(this);
            } else {
                boolean matches = BCrypt.checkpw(e1.getMessage(), passHash);
                new BukkitRunnable() {
                    @Override
                    public void run() {
                        if(matches) {
                            login(player);
                        } else {
                            player.kickPlayer(translate("&cIncorrect Password Entered"));
                        }
                    }
                }.runTask(this);
            }
        });
    }

    private void login(Player player) {
        player.removePotionEffect(PotionEffectType.BLINDNESS);
        player.setWalkSpeed(0.2f);
        player.playSound(player.getLocation(), Sound.valueOf("LEVEL_UP"), 1f, 1f);
        player.sendMessage(translate("&aSuccessfully Logged In!"));
    }

    @EventHandler
    public void onMessage(AsyncPlayerChatEvent e) {
        Consumer<AsyncPlayerChatEvent> callback = awaitingLogin.remove(e.getPlayer());
        if(callback != null) {
            callback.accept(e);
        }
    }

    private void awaitMessages(Player player, Consumer<AsyncPlayerChatEvent> callback) {
        this.awaitingLogin.put(player, callback);
    }

    private void loadConfigs() {
        this.getConfig().options().copyDefaults(true);
        this.saveDefaultConfig();
    }

    private String translate(String s) {
        return ChatColor.translateAlternateColorCodes('&', s);
    }
}
