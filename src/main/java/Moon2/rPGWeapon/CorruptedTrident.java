package Moon2.rPGWeapon;

import org.bukkit.*;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.event.entity.ProjectileLaunchEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.projectiles.ProjectileSource;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Arrays;
import java.util.HashMap;
import java.util.UUID;

import static Moon2.rPGWeapon.Heatblade.plugin;

public class CorruptedTrident implements Listener {

    private final HashMap<UUID, Long> cooldowns = new HashMap<>();
    private final long COOLDOWN_TIME = 500; // 0.5秒冷却时间

    // 获取腐蚀的三叉戟
    public static ItemStack getCorruptedTrident() {
        ItemStack trident = new ItemStack(Material.TRIDENT);
        ItemMeta meta = trident.getItemMeta();

        // 设置武器名称和描述
        meta.setDisplayName(ChatColor.DARK_PURPLE + "腐蚀的三叉戟");
        meta.setLore(Arrays.asList(
                ChatColor.GRAY + "被黑暗力量腐蚀的三叉戟",
                ChatColor.GRAY + "攻击时会施加中毒、缓慢和挖掘疲劳效果",
                ChatColor.GRAY + "在水中或雨中威力大增"
        ));

        meta.setUnbreakable(true);
        // 设置自定义模型数据
        meta.setCustomModelData(6789);

        trident.setItemMeta(meta);
        return trident;
    }

    // 检查物品是否是腐蚀的三叉戟
    private boolean isCorruptedTrident(ItemStack item) {
        if (item == null || item.getType() != Material.TRIDENT || !item.hasItemMeta()) {
            return false;
        }

        ItemMeta meta = item.getItemMeta();
        return meta.hasDisplayName() &&
                meta.getDisplayName().equals(ChatColor.DARK_PURPLE + "腐蚀的三叉戟");
    }

    // 检查实体是否在水中或雨中
    private boolean isInWaterOrRain(LivingEntity entity) {
        Location loc = entity.getLocation();
        return entity.isInWater() ||
                loc.getBlock().getType() == Material.WATER ||
                loc.getWorld().hasStorm() && loc.getBlock().getLightFromSky() == 15;
    }

    // 处理三叉戟投掷事件
    @EventHandler
    public void onProjectileLaunch(ProjectileLaunchEvent event) {
        if (!(event.getEntity() instanceof Trident)) return;

        Trident trident = (Trident) event.getEntity();
        ProjectileSource shooter = trident.getShooter();

        if (!(shooter instanceof Player)) return;

        Player player = (Player) shooter;
        ItemStack item = trident.getItemStack();

        if (!isCorruptedTrident(item)) return;

        // 播放投掷音效
        player.getWorld().playSound(trident.getLocation(), Sound.ENTITY_ELDER_GUARDIAN_AMBIENT, 1.0f, 0.8f);

        // 添加飞行粒子效果
        new BukkitRunnable() {
            @Override
            public void run() {
                if (trident.isDead() || trident.isOnGround()) {
                    cancel();
                    return;
                }

                // 生成飞行粒子
                Location loc = trident.getLocation();
                loc.getWorld().spawnParticle(Particle.SCULK_SOUL, loc, 1, 0.1, 0.1, 0.1, 0.02);
                loc.getWorld().spawnParticle(Particle.SCULK_CHARGE_POP, loc, 4, 0.1, 0.1, 0.1, 0.01);
            }
        }.runTaskTimer(plugin, 0, 2);
    }

    // 处理三叉戟命中事件
    @EventHandler
    public void onProjectileHit(ProjectileHitEvent event) {
        if (!(event.getEntity() instanceof Trident)) return;

        Trident trident = (Trident) event.getEntity();
        ProjectileSource shooter = trident.getShooter();

        if (!(shooter instanceof Player)) return;

        Player player = (Player) shooter;
        ItemStack item = trident.getItemStack();

        if (!isCorruptedTrident(item)) return;

        // 播放命中音效
        Location hitLocation = trident.getLocation();
        hitLocation.getWorld().playSound(hitLocation, Sound.ENTITY_GUARDIAN_HURT, 1.0f, 0.7f);

        // 生成命中粒子效果
        hitLocation.getWorld().spawnParticle(Particle.SCULK_SOUL, hitLocation, 20, 0.5, 0.5, 0.5, 0.05);
        hitLocation.getWorld().spawnParticle(Particle.TRIAL_SPAWNER_DETECTION_OMINOUS, hitLocation, 15, 0.5, 0.5, 0.5, 0.05);

        // 如果命中实体，应用效果
        if (event.getHitEntity() instanceof LivingEntity) {
            LivingEntity target = (LivingEntity) event.getHitEntity();

            // 检查是否在水中或雨中
            boolean inWaterOrRain = isInWaterOrRain(target);

            // 增加伤害（如果在水中或雨中）
            if (inWaterOrRain) {
                target.damage(12);
                hitLocation.getWorld().playSound(hitLocation, Sound.ENTITY_ELDER_GUARDIAN_CURSE, 1.0f, 1.0f);
            }

            // 应用状态效果
            target.addPotionEffect(new PotionEffect(PotionEffectType.POISON, 5 * 20, 0)); // 中毒1，5秒
            target.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 5 * 20, 0)); // 缓慢1，5秒
            target.addPotionEffect(new PotionEffect(PotionEffectType.MINING_FATIGUE, 30 * 20, 0)); // 挖掘疲劳1，30秒

            // 给攻击者播放音效
            player.playSound(player.getLocation(), Sound.ENTITY_PUFFER_FISH_STING, 1.0f, 1.0f);
        }
    }

    // 处理近战攻击事件
    @EventHandler
    public void onEntityDamage(EntityDamageByEntityEvent event) {
        // 检查攻击者是否是玩家并且使用的是腐蚀的三叉戟
        if (!(event.getDamager() instanceof Player)) return;

        Player player = (Player) event.getDamager();
        ItemStack weapon = player.getInventory().getItemInMainHand();

        if (!isCorruptedTrident(weapon)) return;

        // 检查冷却时间
        UUID playerId = player.getUniqueId();
        if (cooldowns.containsKey(playerId)) {
            long timeLeft = (cooldowns.get(playerId) + COOLDOWN_TIME - System.currentTimeMillis());
            if (timeLeft > 0) return;
        }
        cooldowns.put(playerId, System.currentTimeMillis());

        // 检查受害者是否是生物
        if (!(event.getEntity() instanceof LivingEntity)) return;

        LivingEntity target = (LivingEntity) event.getEntity();

        // 检查是否在水中或雨中
        boolean inWaterOrRain = isInWaterOrRain(target);

        // 增加伤害（如果在水中或雨中）
        if (inWaterOrRain) {
            event.setDamage(event.getDamage() + 16);
            target.getWorld().playSound(target.getLocation(), Sound.ENTITY_ELDER_GUARDIAN_CURSE, 1.0f, 1.0f);
        }

        // 应用状态效果
        target.addPotionEffect(new PotionEffect(PotionEffectType.POISON, 5 * 20, 1)); // 中毒2，5秒
        target.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 5 * 20, 1)); // 缓慢2，5秒
        target.addPotionEffect(new PotionEffect(PotionEffectType.MINING_FATIGUE, 30 * 20, 1)); // 挖掘疲劳2，30秒

        // 生成攻击粒子效果
        Location loc = target.getLocation();
        loc.getWorld().spawnParticle(Particle.SCULK_SOUL, loc, 30, 0.5, 0.5, 0.5, 0.2);
        loc.getWorld().spawnParticle(Particle.SCULK_CHARGE_POP, loc, 20, 0.5, 0.5, 0.5, 0.1);

        // 播放攻击音效
        loc.getWorld().playSound(loc, Sound.ENTITY_PUFFER_FISH_STING, 1.0f, 0.8f);
        player.playSound(player.getLocation(), Sound.ENTITY_PUFFER_FISH_STING, 1.0f, 1.0f);
    }
}