package Moon2.rPGWeapon;

import org.bukkit.*;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.entity.EntityShootBowEvent;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.util.Arrays;
import java.util.HashMap;
import java.util.UUID;

import static Moon2.rPGWeapon.Main.plugin;

public class Weightlessbow  implements Weapon {

    private final HashMap<UUID, Long> cooldowns = new HashMap<>();
    private final HashMap<UUID, Integer> drawingPlayers = new HashMap<>();
    private final long COOLDOWN_TIME = 3000; // 3秒冷却时间

    @Override
    public void onEnable() {
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
        plugin.getCommand("weightlessbow").setExecutor(this);
    }

    @Override
    public void onDisable() {
    }

    // 命令处理
    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (cmd.getName().equalsIgnoreCase("weightlessbow")) {
            if (!(sender instanceof Player)) {
                sender.sendMessage(ChatColor.RED + "只有玩家可以使用这个命令!");
                return true;
            }

            Player player = (Player) sender;

            if (!player.hasPermission("weightlessbow.get")) {
                player.sendMessage(ChatColor.RED + "你没有权限使用这个命令!");
                return true;
            }

            ItemStack weightlessBow = getWeightlessBow();
            HashMap<Integer, ItemStack> leftover = player.getInventory().addItem(weightlessBow);

            if (!leftover.isEmpty()) {
                player.getWorld().dropItemNaturally(player.getLocation(), weightlessBow);
                player.sendMessage(ChatColor.YELLOW + "你的库存已满，无重力长弓已掉落在地面上!");
            } else {
                player.sendMessage(ChatColor.GREEN + "你获得了无重力长弓!");
            }

            player.playSound(player.getLocation(), Sound.ITEM_ARMOR_EQUIP_ELYTRA, 1.0f, 1.0f);

            return true;
        }
        return false;
    }

    // 获取无重力长弓
    public static ItemStack getWeightlessBow() {
        ItemStack bow = new ItemStack(Material.BOW);
        ItemMeta meta = bow.getItemMeta();

        // 设置武器名称和描述
        meta.setDisplayName(ChatColor.AQUA + "无重力长弓");
        meta.setLore(Arrays.asList(
                ChatColor.GRAY + "蕴含着星辰之力的神秘长弓",
                ChatColor.GRAY + "箭矢无视重力，精准打击",
                ChatColor.GRAY + "高速致命，但需要付出代价"
        ));

        // 设置自定义模型数据
        meta.setCustomModelData(2001);

        bow.setItemMeta(meta);
        return bow;
    }

    // 检查物品是否是无重力长弓
    private boolean isWeightlessBow(ItemStack item) {
        if (item == null || item.getType() != Material.BOW || !item.hasItemMeta()) {
            return false;
        }

        ItemMeta meta = item.getItemMeta();
        return meta.hasDisplayName() &&
                meta.getDisplayName().equals(ChatColor.AQUA + "无重力长弓");
    }

    // 处理射箭事件（主要逻辑）
    @EventHandler
    public void onArrowShoot(EntityShootBowEvent event) {
        if (!(event.getEntity() instanceof Player)) return;

        Player player = (Player) event.getEntity();
        ItemStack bow = event.getBow();

        if (!isWeightlessBow(bow)) return;

        // 设置冷却时间
        cooldowns.put(player.getUniqueId(), System.currentTimeMillis());
        player.setCooldown(Material.BOW, 60); // 3秒冷却（20ticks/秒）

        if (!(event.getProjectile() instanceof Arrow)) return;

        Arrow arrow = (Arrow) event.getProjectile();
        Location playerLoc = player.getEyeLocation();
        Vector direction = playerLoc.getDirection().normalize(); // 获取并单位化视线方向向量:cite[4]
        double baseSpeed = 4.0; // 这个值可以和你之前的速度倍增器配合调整
        Vector newVelocity = direction.multiply(baseSpeed * event.getForce()); // 确保速度与拉弦力度相关

        // 应用新的、精确的速度向量到箭矢
        arrow.setVelocity(newVelocity);

        // 修改箭矢属性
        arrow.setGravity(false); // 无重力
        arrow.setDamage(12.0); // 满弦伤害为48
        arrow.setPierceLevel((byte) 5); // 穿透5个目标
        arrow.setCritical(false); // 取消原版暴击效果

        // 给玩家施加射箭后的效果
        player.addPotionEffect(new PotionEffect(
                PotionEffectType.SLOWNESS,
                3 * 20, // 5秒
                3       // 缓慢1级
        ));
        player.addPotionEffect(new PotionEffect(
                PotionEffectType.WEAKNESS,
                3 * 20, // 5秒
                3       // 虚弱1级
        ));

        // 播放射箭音效
        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_ENDER_DRAGON_FLAP, 1.0f, 1.5f);

        // 启动箭矢跟踪任务
        new BukkitRunnable() {
            int ticks = 0;
            final UUID arrowId = arrow.getUniqueId();

            @Override
            public void run() {
                Arrow currentArrow = null;

                // 查找箭矢实体
                for (Entity entity : player.getWorld().getEntities()) {
                    if (entity.getUniqueId().equals(arrowId) && entity instanceof Arrow) {
                        currentArrow = (Arrow) entity;
                        break;
                    }
                }

                // 如果箭矢不存在或超过5秒，销毁并结束任务
                if (currentArrow == null || currentArrow.isDead() || ticks >= 100) { // 5秒 = 100ticks
                    if (currentArrow != null && !currentArrow.isDead()) {
                        // 箭矢超时，生成销毁粒子
                        Location loc = currentArrow.getLocation();
                        loc.getWorld().spawnParticle(Particle.END_ROD, loc, 30, 0.3, 0.3, 0.3, 0.1, null, true);
                        loc.getWorld().playSound(loc, Sound.BLOCK_AMETHYST_BLOCK_BREAK, 1.0f, 1.3f);
                        player.playSound(player.getLocation(), Sound.BLOCK_AMETHYST_BLOCK_BREAK, 1.0f, 1.3f);
                        currentArrow.remove();
                    }
                    cancel();
                    return;
                }

                // 生成飞行粒子
                Location arrowLoc = currentArrow.getLocation();
                arrowLoc.getWorld().spawnParticle(Particle.ELECTRIC_SPARK, arrowLoc, 3, 0.1, 0.1, 0.1, 0.02, null, true);
                arrowLoc.getWorld().spawnParticle(Particle.FIREWORK, arrowLoc, 1, 0.1, 0.1, 0.1, 0.01, null, true);
                ticks++;
            }
        }.runTaskTimer(plugin, 0, 1); // 每tick执行一次
    }

    // 处理箭矢命中事件
    @EventHandler
    public void onArrowHit(ProjectileHitEvent event) {
        if (!(event.getEntity() instanceof Arrow)) return;

        Arrow arrow = (Arrow) event.getEntity();
        if (!(arrow.getShooter() instanceof Player)) return;

        Player player = (Player) arrow.getShooter();
        ItemStack bow = player.getInventory().getItemInMainHand();

        // 检查是否为长弓射出的箭
        if (!isWeightlessBow(bow)) return;

        // 生成命中粒子效果
        Location hitLoc = event.getEntity().getLocation();
        hitLoc.getWorld().spawnParticle(Particle.FLASH, hitLoc, 5, 0.3, 0.3, 0.3, 0,null, true);
        hitLoc.getWorld().spawnParticle(Particle.END_ROD, hitLoc, 15, 0.5, 0.5, 0.5, 0.1,null, true);

        // 播放命中音效
        hitLoc.getWorld().playSound(hitLoc, Sound.BLOCK_GLASS_BREAK, 1.0f, 1.0f);
        player.playSound(player.getLocation(), Sound.BLOCK_AMETHYST_BLOCK_PLACE, 0.8f, 1.0f);
        // 检查是否命中实体
        if (event.getHitEntity() instanceof LivingEntity) {
            LivingEntity target = (LivingEntity) event.getHitEntity();

            // 给被命中实体施加荧光效果
            target.addPotionEffect(new PotionEffect(
                    PotionEffectType.GLOWING,
                    10 * 20, // 10秒
                    0        // 等级1
            ));

            // 给攻击者播放额外音效
            player.playSound(player.getLocation(), Sound.ENTITY_ENDER_DRAGON_HURT, 0.8f, 1.2f);
        }
    }
}