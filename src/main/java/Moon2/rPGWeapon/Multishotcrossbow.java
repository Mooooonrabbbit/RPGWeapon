package Moon2.rPGWeapon;

import org.bukkit.*;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.CrossbowMeta;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.*;
import java.util.concurrent.ThreadLocalRandom;
import static Moon2.rPGWeapon.Main.plugin;

public class Multishotcrossbow implements Weapon {

    private final Map<UUID, Long> cooldowns = new HashMap<>();
    private final long COOLDOWN_TIME = 5000; // 5秒冷却时间

    // 支持的装填物品类型
    private final Set<Material> SUPPORTED_PROJECTILES = Set.of(
            Material.FIRE_CHARGE,           // 火焰弹
            Material.SNOWBALL,              // 雪球
            Material.ENDER_PEARL,           // 末影珍珠
            Material.EGG,                   // 鸡蛋
            Material.EXPERIENCE_BOTTLE,     // 附魔之瓶
            Material.DRAGON_BREATH,         // 龙息
            Material.TNT,                   // TNT
            Material.SPLASH_POTION,         // 喷溅型药水
            Material.LINGERING_POTION       // 滞留型药水
    );

    // 特殊物品显示名称映射
    private final Map<Material, String> PROJECTILE_NAMES = new HashMap<>();

    @Override
    public void onEnable() {
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
        plugin.getCommand("multishotcrossbow").setExecutor(this);
    }

    @Override
    public void onDisable() {
    }
    private void initializeProjectileNames() {
        PROJECTILE_NAMES.put(Material.FIRE_CHARGE, "火焰弹");
        PROJECTILE_NAMES.put(Material.SNOWBALL, "雪球");
        PROJECTILE_NAMES.put(Material.ENDER_PEARL, "末影珍珠");
        PROJECTILE_NAMES.put(Material.EGG, "鸡蛋");
        PROJECTILE_NAMES.put(Material.EXPERIENCE_BOTTLE, "附魔之瓶");
        PROJECTILE_NAMES.put(Material.DRAGON_BREATH, "龙息");
        PROJECTILE_NAMES.put(Material.TNT, "TNT");
        PROJECTILE_NAMES.put(Material.SPLASH_POTION, "药水");
        PROJECTILE_NAMES.put(Material.LINGERING_POTION, "药水");
    }

    // 命令处理
    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (cmd.getName().equalsIgnoreCase("multishotcrossbow")) {
            if (!(sender instanceof Player)) {
                sender.sendMessage(ChatColor.RED + "只有玩家可以使用这个命令!");
                return true;
            }

            Player player = (Player) sender;

            if (!player.hasPermission("multicrossbow.get")) {
                player.sendMessage(ChatColor.RED + "你没有权限使用这个命令!");
                return true;
            }

            ItemStack multiCrossbow = getMultiShotCrossbow();
            HashMap<Integer, ItemStack> leftover = player.getInventory().addItem(multiCrossbow);

            if (!leftover.isEmpty()) {
                player.getWorld().dropItemNaturally(player.getLocation(), multiCrossbow);
                player.sendMessage(ChatColor.YELLOW + "你的库存已满，多重散射弩已掉落在地面上!");
            } else {
                player.sendMessage(ChatColor.GREEN + "你获得了多重散射弩!");
            }

            player.playSound(player.getLocation(), Sound.ITEM_CROSSBOW_LOADING_END, 1.0f, 1.0f);

            return true;
        }
        return false;
    }

    // 获取多重散射弩
    public static ItemStack getMultiShotCrossbow() {
        ItemStack crossbow = new ItemStack(Material.CROSSBOW);
        ItemMeta meta = crossbow.getItemMeta();

        // 设置武器名称和描述
        meta.setDisplayName(ChatColor.GOLD + "多重散射弩");
        meta.setLore(Arrays.asList(
                ChatColor.GRAY + "可以装填多种特殊物品",
                ChatColor.GRAY + "发射时分裂为五个抛射物",
                ChatColor.GRAY + "右键装填，再次右键发射"
        ));

        // 设置自定义模型数据
        meta.setCustomModelData(6001);

        crossbow.setItemMeta(meta);
        return crossbow;
    }

    // 检查物品是否是多重散射弩
    private boolean isMultiShotCrossbow(ItemStack item) {
        if (item == null || item.getType() != Material.CROSSBOW || !item.hasItemMeta()) {
            return false;
        }

        ItemMeta meta = item.getItemMeta();
        return meta.hasDisplayName() &&
                meta.getDisplayName().equals(ChatColor.GOLD + "多重散射弩");
    }

    // 处理玩家交互事件
    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        ItemStack mainHand = player.getInventory().getItemInMainHand();
        ItemStack offHand = player.getInventory().getItemInOffHand();

        // 只处理右键点击
        if (event.getAction() != Action.RIGHT_CLICK_AIR &&
                event.getAction() != Action.RIGHT_CLICK_BLOCK) {
            return;
        }

        if (!isMultiShotCrossbow(mainHand)) return;

        event.setCancelled(true); // 防止原版弩的装填行为

        UUID playerId = player.getUniqueId();

        // 检查冷却时间
        if (cooldowns.containsKey(playerId)) {
            long timeLeft = cooldowns.get(playerId) + COOLDOWN_TIME - System.currentTimeMillis();
            if (timeLeft > 0) {
                player.sendActionBar(ChatColor.RED + "冷却中: " + (timeLeft/1000) + "秒");
                player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BASS, 1.0f, 0.5f);
                return;
            }
        }

        CrossbowMeta crossbowMeta = (CrossbowMeta) mainHand.getItemMeta();

        // 如果弩已经装填，则发射
        if (crossbowMeta.hasChargedProjectiles()) {
            fireProjectiles(player, mainHand, crossbowMeta);
        }
        // 否则尝试装填
        else {
            // 检查副手是否有支持的抛射物
            if (offHand != null && SUPPORTED_PROJECTILES.contains(offHand.getType())) {
                loadProjectile(player, mainHand, crossbowMeta, offHand);
            } else {
                player.sendActionBar(ChatColor.RED + "副手没有可装填的抛射物!");
                player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BASS, 1.0f, 0.5f);
            }
        }
    }

    // 装填抛射物
    private void loadProjectile(Player player, ItemStack crossbow, CrossbowMeta meta, ItemStack projectile) {
        // 创建装填物品的副本（不消耗原物品）
        ItemStack projectileCopy = projectile.clone();
        projectileCopy.setAmount(1);

        // 添加到弩的装填列表
        List<ItemStack> chargedProjectiles = new ArrayList<>();
        chargedProjectiles.add(projectileCopy);
        meta.setChargedProjectiles(chargedProjectiles);

        // 更新Lore显示装填的抛射物
        String projectileName = PROJECTILE_NAMES.getOrDefault(projectile.getType(), "未知抛射物");
        List<String> lore = meta.getLore();
        if (lore == null) lore = new ArrayList<>();

        // 移除旧的装填信息
        lore.removeIf(line -> line.contains("装填:"));

        // 添加新的装填信息
        lore.add(ChatColor.GREEN + "装填: " + projectileName);
        meta.setLore(lore);

        crossbow.setItemMeta(meta);

        // 播放装填音效
        player.playSound(player.getLocation(), Sound.ITEM_CROSSBOW_LOADING_END, 1.0f, 1.0f);
        player.sendActionBar(ChatColor.GREEN + "已装填: " + projectileName);
    }

    // 发射抛射物
    private void fireProjectiles(Player player, ItemStack crossbow, CrossbowMeta meta) {
        // 获取装填的抛射物
        List<ItemStack> chargedProjectiles = meta.getChargedProjectiles();
        if (chargedProjectiles.isEmpty()) return;

        ItemStack projectileItem = chargedProjectiles.get(0);
        Material projectileType = projectileItem.getType();

        // 设置冷却
        cooldowns.put(player.getUniqueId(), System.currentTimeMillis());
        player.setCooldown(Material.CROSSBOW, 5 * 20); // 5秒冷却条

        // 清空装填状态
        meta.setChargedProjectiles(new ArrayList<>());

        // 移除装填信息
        List<String> lore = meta.getLore();
        if (lore != null) {
            lore.removeIf(line -> line.contains("装填:"));
            meta.setLore(lore);
        }

        crossbow.setItemMeta(meta);

        // 播放发射音效
        player.playSound(player.getLocation(), Sound.ITEM_CROSSBOW_SHOOT, 2.0f, 1.0f);

        // 根据抛射物类型执行不同的发射逻辑
        Location eyeLocation = player.getEyeLocation();
        Vector direction = eyeLocation.getDirection();

        // 检查是否应用多重散射（末影之眼不应用）
        boolean applyMultiShot = (projectileType != Material.ENDER_EYE);

        if (applyMultiShot) {
            // 多重散射：发射5个抛射物
            for (int i = 0; i < 5; i++) {
                Vector spreadDirection = applySpread(direction, 5.0); // ±5度散布
                spawnProjectile(player, projectileType, projectileItem, eyeLocation, spreadDirection, 1.5f); // 1.5倍速度
            }
        } else {
            // 单发射击（末影之眼）
            spawnProjectile(player, projectileType, projectileItem, eyeLocation, direction, 1.2f); // 1.2倍速度
        }

        player.sendActionBar(ChatColor.YELLOW + "发射了 " +
                PROJECTILE_NAMES.getOrDefault(projectileType, "抛射物"));
    }

    // 应用散布角度
    private Vector applySpread(Vector direction, double spreadDegrees) {
        double spreadRadians = Math.toRadians(spreadDegrees);

        // 克隆原始方向
        Vector spreadDir = direction.clone();

        // 应用随机水平和垂直偏移
        double yaw = Math.toRadians(ThreadLocalRandom.current().nextDouble(-spreadDegrees, spreadDegrees));
        double pitch = Math.toRadians(ThreadLocalRandom.current().nextDouble(-spreadDegrees, spreadDegrees));

        // 计算新的方向向量
        double x = spreadDir.getX();
        double y = spreadDir.getY();
        double z = spreadDir.getZ();

        double length = Math.sqrt(x*x + y*y + z*z);

        // 应用偏航（水平旋转）
        double newX = x * Math.cos(yaw) - z * Math.sin(yaw);
        double newZ = x * Math.sin(yaw) + z * Math.cos(yaw);

        // 应用俯仰（垂直旋转）
        double horizontalLength = Math.sqrt(newX*newX + newZ*newZ);
        double newY = y * Math.cos(pitch) - horizontalLength * Math.sin(pitch);
        double newHorizontalLength = horizontalLength * Math.cos(pitch) + y * Math.sin(pitch);

        // 归一化并保持原始长度
        Vector result = new Vector(newX, newY, newZ);
        result.normalize().multiply(length);

        return result;
    }

    // 生成抛射物
    private void spawnProjectile(Player player, Material projectileType, ItemStack projectileItem,
                                 Location location, Vector direction, float speedMultiplier) {
        World world = player.getWorld();

        switch (projectileType) {
            case FIRE_CHARGE:
                // 发射小火球（类似发射器）
                SmallFireball fireball = world.spawn(location, SmallFireball.class);
                fireball.setDirection(direction);
                fireball.setVelocity(direction.multiply(speedMultiplier));
                fireball.setShooter(player);
                break;

            case SNOWBALL:
            case EGG:
            case ENDER_PEARL:
            case EXPERIENCE_BOTTLE:
                // 发射可投掷物品（更高初速度）
                Projectile projectile;
                if (projectileType == Material.SNOWBALL) {
                    projectile = world.spawn(location, Snowball.class);
                } else if (projectileType == Material.EGG) {
                    projectile = world.spawn(location, Egg.class);
                } else if (projectileType == Material.ENDER_PEARL) {
                    projectile = world.spawn(location, EnderPearl.class);
                } else { // EXPERIENCE_BOTTLE
                    projectile = world.spawn(location, ThrownExpBottle.class);
                }

                projectile.setVelocity(direction.multiply(speedMultiplier * 2.0)); // 2倍原版速度
                projectile.setShooter(player);
                break;

            case DRAGON_BREATH:
                // 发射龙息弹
                DragonFireball dragonFireball = world.spawn(location, DragonFireball.class);
                dragonFireball.setDirection(direction);
                dragonFireball.setVelocity(direction.multiply(speedMultiplier));
                dragonFireball.setShooter(player);
                break;

            case TNT:
                // 发射激活的TNT
                TNTPrimed tnt = world.spawn(location, TNTPrimed.class);
                tnt.setVelocity(direction.multiply(speedMultiplier));
                tnt.setFuseTicks(40); // 2秒后爆炸
                break;

            case SPLASH_POTION:
            case LINGERING_POTION:
                // 发射药水
                ThrownPotion potion;
                if (projectileType == Material.SPLASH_POTION) {
                    potion = world.spawn(location, SplashPotion.class);
                } else {
                    potion = world.spawn(location, LingeringPotion.class);
                }

                potion.setItem(projectileItem);
                potion.setVelocity(direction.multiply(speedMultiplier * 1.8)); // 1.8倍速度
                potion.setShooter(player);
                break;
        }

        // 生成发射粒子效果
        world.spawnParticle(Particle.CRIT, location, 10, 0.1, 0.1, 0.1, 0.05);
    }
}