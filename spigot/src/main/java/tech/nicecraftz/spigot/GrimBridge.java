package tech.nicecraftz.spigot;

import ac.grim.grimac.api.GrimAbstractAPI;
import ac.grim.grimac.api.event.events.CommandExecuteEvent;
import ac.grim.grimac.api.plugin.BasicGrimPlugin;
import ac.grim.grimac.api.plugin.GrimPlugin;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import lombok.Getter;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;
import tech.nicecraftz.common.Messager;
import tech.nicecraftz.spigot.util.MessagingHelper;

import java.util.UUID;
import java.util.concurrent.TimeUnit;


@Getter
public final class GrimBridge extends JavaPlugin {
    @Getter
    private static GrimBridge instance;
    private final PluginManager pluginManager = getServer().getPluginManager();
    private GrimAbstractAPI grimAbstractAPI;
    private final Cache<UUID, Boolean> cache = CacheBuilder.newBuilder().expireAfterWrite(10, TimeUnit.SECONDS).build();

    @Override
    public void onEnable() {
        instance = this;

        Plugin grimAcPlugin = pluginManager.getPlugin("GrimAC");
        if (grimAcPlugin == null || !grimAcPlugin.isEnabled()) {
            getLogger().severe("Couldn't load the plugin because Grim Anticheat is missing.");
            getPluginManager().disablePlugin(this);
            return;
        }

        RegisteredServiceProvider<GrimAbstractAPI> rsp = Bukkit.getServicesManager().getRegistration(GrimAbstractAPI.class);
        if (rsp == null) {
            getLogger().severe("Couldn't load the plugin because Grim Anticheat is missing.");
            getPluginManager().disablePlugin(this);
            return;
        }

        grimAbstractAPI = rsp.getProvider();
        getServer().getMessenger().registerOutgoingPluginChannel(this, Messager.PLUGIN_CHANNEL);


        GrimPlugin plugin = new BasicGrimPlugin(
                this.getLogger(),
                this.getDataFolder(),
                this.getDescription().getVersion(),
                this.getDescription().getDescription(),
                this.getDescription().getAuthors()
        );
        grimAbstractAPI.getEventBus().subscribe(plugin, CommandExecuteEvent.class, event -> {
            String command = event.getCommand();
            if (!command.startsWith("proxy:")) return;
            event.setCancelled(true);

            OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(event.getPlayer().getUniqueId());
            if (!offlinePlayer.isOnline()) return;

            Player player = offlinePlayer.getPlayer();
            if (cache.asMap().containsKey(player.getUniqueId())) return;
            MessagingHelper.sendBungeeMessage(player, command.replace("proxy:", ""));
            getLogger().info("Successfully executed command: " + command);
            cache.put(player.getUniqueId(), true);
        });

        getLogger().info("Grim Anticheat Bridge has been enabled.");
    }

    @Override
    public void onDisable() {
        getServer().getMessenger().unregisterOutgoingPluginChannel(this);
    }
}
