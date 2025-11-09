package Moon2.rPGWeapon.Weapon;

import org.bukkit.*;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.HashMap;
import java.util.UUID;

import static Moon2.rPGWeapon.Main.plugin;

public class Heatblade implements Weapon {

    private final HashMap<UUID, Long> cooldowns = new HashMap<>();
    private final long COOLDOWN_TIME = 1000; // 1秒冷却时间

    @Override
    public void onEnable() {
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
        plugin.getCommand("heatblade").setExecutor(this);
    }

    @Override
    public void onDisable() {
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String @NotNull [] args) {
        Player player = (Player) sender;
        player.getInventory().addItem(getHeatBlade());
        player.sendMessage(ChatColor.GOLD + "你获得了炽热之剑!");
        return true;
    }

    // 提供获取炽热之剑的方法
    public static ItemStack getHeatBlade() {
        ItemStack blade = new ItemStack(Material.NETHERITE_SWORD);
        ItemMeta meta = blade.getItemMeta();

        // 设置武器名称和描述
        meta.setDisplayName(ChatColor.GOLD + "炽热之剑");
        meta.setLore(Arrays.asList(
                ChatColor.GRAY + "蕴含地狱之火的强大武器",
                ChatColor.GRAY + "攻击时点燃敌人并赋予虚弱效果",
                ChatColor.GRAY + "在下界中能发挥更强大的力量"
        ));

        meta.setUnbreakable(true);
        // !!! 重要: 设置自定义模型数据 !!!
        // 这里我们假设你为炽热之剑定义的自定义模型数据值为 12345
        // 你需要确保你的资源包为这个值提供了对应的模型
        meta.setCustomModelData(12345);
        blade.setItemMeta(meta);
        return blade;
    }

    @EventHandler
    public void onEntityDamage(EntityDamageByEntityEvent event) {
        // 检查攻击者是否是玩家并且使用的是炽热之剑
        if (!(event.getDamager() instanceof Player)) return;

        Player player = (Player) event.getDamager();
        ItemStack weapon = player.getInventory().getItemInMainHand();

        if (!isHeatBlade(weapon)) return;

        // 检查冷却时间
        UUID playerId = player.getUniqueId();
        if (cooldowns.containsKey(playerId)) {
            long secondsLeft = (cooldowns.get(playerId) + COOLDOWN_TIME - System.currentTimeMillis()) / 1000;
            if (secondsLeft > 0) return;
        }
        cooldowns.put(playerId, System.currentTimeMillis());

        // 检查受害者是否是生物
        if (!(event.getEntity() instanceof LivingEntity)) return;

        LivingEntity target = (LivingEntity) event.getEntity();

        // 计算伤害加成
        double originalDamage = event.getDamage();
        double bonusDamage = 5;

        // 如果在下界则额外增加伤害
        if (player.getWorld().getEnvironment() == World.Environment.NETHER) {
            bonusDamage = 10;
        }

        event.setDamage(originalDamage + bonusDamage);

        // 在目标位置生成火焰粒子效果
        Location loc = target.getLocation();
        World world = target.getWorld();
        world.spawnParticle(Particle.FLAME, loc, 50, 0.5, 0.5, 0.5, 0.05);

        // 播放烈焰人受伤音效
        world.playSound(loc, Sound.ENTITY_BLAZE_HURT, 1.0f, 0.7f);

        // 对目标本身也施加效果
        target.setFireTicks(5 * 20);
        target.addPotionEffect(new PotionEffect(
                PotionEffectType.WEAKNESS,
                5 * 20,
                0
        ));
        // 对攻击者施加力量和速度效果10秒
        player.addPotionEffect(new PotionEffect(
                PotionEffectType.STRENGTH,
                10 * 20,
                0 // 力量I
        ));
        player.addPotionEffect(new PotionEffect(
                PotionEffectType.SPEED,
                10 * 20,
                0 // 速度I
        ));

        // 添加一些视觉效果给玩家
        new BukkitRunnable() {
            int count = 0;
            @Override
            public void run() {
                if (count++ >= 5) {
                    cancel();
                    return;
                }
                player.spawnParticle(Particle.TRIAL_SPAWNER_DETECTION, player.getLocation().add(0, 1, 0), 10, 0.5, 0.5, 0.5, 0.01);
            }
        }.runTaskTimer(plugin, 0, 4);
    }

    // 检查物品是否是炽热之剑
    private boolean isHeatBlade(ItemStack item) {
        if (item == null || item.getType() != Material.NETHERITE_SWORD || !item.hasItemMeta()) {
            return false;
        }

        ItemMeta meta = item.getItemMeta();
        return meta.hasDisplayName() &&
                meta.getDisplayName().equals(ChatColor.GOLD + "炽热之剑");
    }
}