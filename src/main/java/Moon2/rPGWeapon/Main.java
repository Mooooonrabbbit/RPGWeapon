package Moon2.rPGWeapon;

import org.bukkit.ChatColor;
import org.bukkit.Sound;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.HashMap;

import static Moon2.rPGWeapon.CorruptedTrident.getCorruptedTrident;
import static Moon2.rPGWeapon.Heatblade.getHeatBlade;
import static Moon2.rPGWeapon.Rocketboots.getRocketBoots;

public class Main extends JavaPlugin {

    public static JavaPlugin plugin;
    public static Rocketboots rocketboots;

    @Override
    public void onEnable() {

        plugin = this;
        rocketboots = new Rocketboots();
        getServer().getPluginManager().registerEvents(rocketboots, this);
        this.getCommand("rocketboots").setExecutor(this);
        getServer().getPluginManager().registerEvents(new CorruptedTrident(), this);
        this.getCommand("corruptedtrident").setExecutor(this);
        getServer().getPluginManager().registerEvents(new Heatblade(), this);
        this.getCommand("heatblade").setExecutor(this);
        getLogger().info("插件已启用!");

    }

    @Override
    public void onDisable() {
        // 取消所有正在运行的任务
        for (Integer taskId : rocketboots.durabilityTask.values()) {
            if (taskId != null) {
                getServer().getScheduler().cancelTask(taskId);
            }
        }
        getLogger().info("炽热之剑插件已禁用!");
    }


    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (cmd.getName().equalsIgnoreCase("heatblade") && sender instanceof Player) {
            Player player = (Player) sender;
            player.getInventory().addItem(getHeatBlade());
            player.sendMessage(ChatColor.GOLD + "你获得了炽热之剑!");
            return true;
        }
        if (cmd.getName().equalsIgnoreCase("corruptedtrident")) {
            if (!(sender instanceof Player)) {
                sender.sendMessage(ChatColor.RED + "只有玩家可以使用这个命令!");
                return true;
            }

            Player player = (Player) sender;

            if (!player.hasPermission("corruptedtrident.get")) {
                player.sendMessage(ChatColor.RED + "你没有权限使用这个命令!");
                return true;
            }

            ItemStack corruptedTrident = getCorruptedTrident();
            HashMap<Integer, ItemStack> leftover = player.getInventory().addItem(corruptedTrident);

            if (!leftover.isEmpty()) {
                player.getWorld().dropItemNaturally(player.getLocation(), corruptedTrident);
                player.sendMessage(ChatColor.YELLOW + "你的库存已满，腐蚀的三叉戟已掉落在地面上!");
            } else {
                player.sendMessage(ChatColor.GREEN + "你获得了腐蚀的三叉戟!");
            }

            player.playSound(player.getLocation(), Sound.ENTITY_ITEM_PICKUP, 1.0f, 1.0f);

            return true;
        }
        if (cmd.getName().equalsIgnoreCase("rocketboots")) {
            if (!(sender instanceof Player)) {
                sender.sendMessage(ChatColor.RED + "只有玩家可以使用这个命令!");
                return true;
            }

            Player player = (Player) sender;

            if (!player.hasPermission("rocketboots.get")) {
                player.sendMessage(ChatColor.RED + "你没有权限使用这个命令!");
                return true;
            }

            ItemStack rocketBoots = getRocketBoots();

            // 检查玩家是否已经穿戴了靴子
            if (player.getInventory().getBoots() != null) {
                // 如果已经穿了靴子，尝试添加到库存
                HashMap<Integer, ItemStack> leftover = player.getInventory().addItem(rocketBoots);
                if (!leftover.isEmpty()) {
                    player.getWorld().dropItemNaturally(player.getLocation(), rocketBoots);
                    player.sendMessage(ChatColor.YELLOW + "你的库存已满，火箭靴已掉落在地面上!");
                } else {
                    player.sendMessage(ChatColor.GREEN + "你获得了火箭靴! 请手动装备它们.");
                }
            } else {
                // 直接装备火箭靴
                player.getInventory().setBoots(rocketBoots);
                player.sendMessage(ChatColor.GREEN + "你已装备火箭靴!");
            }

            player.playSound(player.getLocation(), Sound.ITEM_ARMOR_EQUIP_IRON, 1.0f, 1.0f);

            return true;
        }
        return false;
    }
}
