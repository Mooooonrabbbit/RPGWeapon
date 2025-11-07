package Moon2.rPGWeapon;

import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerItemHeldEvent;
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
public class Testcrossbow implements Weapon {

    private final Map<UUID, Long> cooldowns = new HashMap<>();
    private final Map<UUID, Integer> chargingTasks = new HashMap<>();
    private final Map<UUID, ItemStack> pendingProjectiles = new HashMap<>();
    private final long COOLDOWN_TIME = 5000; // 5秒冷却时间

    // 支持多重散射的物品类型
    private final Set<Material> MULTISHOT_PROJECTILES = Set.of(
            Material.FIRE_CHARGE,           // 火焰弹
            Material.SNOWBALL,              // 雪球
            Material.ENDER_PEARL,           // 末影珍珠
            Material.EGG,                   // 鸡蛋
            Material.EXPERIENCE_BOTTLE,     // 附魔之瓶
            Material.SPLASH_POTION,         // 喷溅型药水
            Material.LINGERING_POTION,      // 滞留型药水
            Material.TNT,                   // TNT
            Material.ARROW,                 // 箭矢
            Material.SPECTRAL_ARROW,        // 光灵箭
            Material.TIPPED_ARROW           // 药水箭
    );

    // 特殊物品显示名称映射
    private final Map<Material, String> PROJECTILE_NAMES = new HashMap<>();

    @Override
    public void onEnable() {
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
        plugin.getCommand("testcrossbow").setExecutor(this);
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
        PROJECTILE_NAMES.put(Material.ENDER_EYE, "末影之眼");
        PROJECTILE_NAMES.put(Material.TNT, "TNT");
        PROJECTILE_NAMES.put(Material.SPLASH_POTION, "药水");
        PROJECTILE_NAMES.put(Material.LINGERING_POTION, "药水");
        PROJECTILE_NAMES.put(Material.ARROW, "箭矢");
        PROJECTILE_NAMES.put(Material.SPECTRAL_ARROW, "光灵箭");
        PROJECTILE_NAMES.put(Material.TIPPED_ARROW, "药水箭");
    }

    // 命令处理
    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (cmd.getName().equalsIgnoreCase("multicrossbow")) {
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

        // 如果弩已经装填，则发射
        CrossbowMeta crossbowMeta = (CrossbowMeta) mainHand.getItemMeta();
        if (crossbowMeta.hasChargedProjectiles()) {
            // 检查冷却时间
            if (cooldowns.containsKey(playerId)) {
                long timeLeft = cooldowns.get(playerId) + COOLDOWN_TIME - System.currentTimeMillis();
                if (timeLeft > 0) {
                    player.sendActionBar(ChatColor.RED + "冷却中: " + (timeLeft/1000) + "秒");
                    player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BASS, 1.0f, 0.5f);
                    return;
                }
            }

            fireProjectiles(player, mainHand, crossbowMeta);
        }
        // 否则开始装填流程
        else {
            // 检查副手是否有物品
            if (offHand == null || offHand.getType() == Material.AIR) {
                player.sendActionBar(ChatColor.RED + "副手没有可装填的物品!");
                player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BASS, 1.0f, 0.5f);
                return;
            }

            // 检查是否已经在装填中
            if (chargingTasks.containsKey(playerId)) {
                player.sendActionBar(ChatColor.YELLOW + "已经在装填中...");
                return;
            }

            // 开始装填动画
            startChargingAnimation(player, mainHand, offHand);
        }
    }

    // 开始装填动画
    private void startChargingAnimation(Player player, ItemStack crossbow, ItemStack projectile) {
        UUID playerId = player.getUniqueId();

        // 播放开始装填音效
        player.playSound(player.getLocation(), Sound.ITEM_CROSSBOW_LOADING_START, 1.0f, 1.0f);

        // 存储待装填的抛射物
        pendingProjectiles.put(playerId, projectile.clone());

        // 启动装填任务
        int taskId = new BukkitRunnable() {
            int progress = 0;
            final int totalProgress = 20; // 1秒装填时间 (20ticks)

            @Override
            public void run() {
                // 检查玩家是否还在持有弩和抛射物
                ItemStack currentMainHand = player.getInventory().getItemInMainHand();
                ItemStack currentOffHand = player.getInventory().getItemInOffHand();

                if (!isMultiShotCrossbow(currentMainHand) ||
                        currentOffHand == null ||
                        currentOffHand.getType() == Material.AIR ||
                        !currentOffHand.isSimilar(pendingProjectiles.get(playerId))) {

                    // 装填被中断
                    cancelCharging(player);
                    return;
                }

                progress++;

                // 更新装填进度
                float progressPercent = (float) progress / totalProgress;
                player.setExp(progressPercent);

                // 播放装填进度音效
                if (progress % 5 == 0) {
                    player.playSound(player.getLocation(), Sound.ITEM_CROSSBOW_LOADING_MIDDLE,
                            0.5f, 0.5f + progressPercent);
                }

                // 生成装填粒子效果
                Location eyeLoc = player.getEyeLocation();
                Vector direction = eyeLoc.getDirection();
                Location particleLoc = eyeLoc.add(direction.multiply(2));
                player.spawnParticle(Particle.HAPPY_VILLAGER, particleLoc, 3, 0.1, 0.1, 0.1, 0.01);

                // 装填完成
                if (progress >= totalProgress) {
                    completeCharging(player, crossbow);
                    cancel();
                }
            }
        }.runTaskTimer(plugin, 0, 1).getTaskId();

        chargingTasks.put(playerId, taskId);
    }

    // 取消装填
    private void cancelCharging(Player player) {
        UUID playerId = player.getUniqueId();

        if (chargingTasks.containsKey(playerId)) {
            Bukkit.getScheduler().cancelTask(chargingTasks.get(playerId));
            chargingTasks.remove(playerId);
        }

        pendingProjectiles.remove(playerId);
        player.setExp(0f);
        player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BASS, 1.0f, 0.5f);
        player.sendActionBar(ChatColor.RED + "装填已取消");
    }

    // 完成装填
    private void completeCharging(Player player, ItemStack crossbow) {
        UUID playerId = player.getUniqueId();

        ItemStack projectile = pendingProjectiles.get(playerId);
        if (projectile == null) return;

        CrossbowMeta crossbowMeta = (CrossbowMeta) crossbow.getItemMeta();

        // 创建装填物品的副本（不消耗原物品）
        ItemStack projectileCopy = projectile.clone();
        projectileCopy.setAmount(1);

        // 添加到弩的装填列表
        List<ItemStack> chargedProjectiles = new ArrayList<>();
        chargedProjectiles.add(projectileCopy);
        crossbowMeta.setChargedProjectiles(chargedProjectiles);

        // 更新Lore显示装填的抛射物
        String projectileName = getProjectileDisplayName(projectile.getType(), projectile);
        List<String> lore = crossbowMeta.getLore();
        if (lore == null) lore = new ArrayList<>();

        // 移除旧的装填信息
        lore.removeIf(line -> line.contains("装填:"));

        // 添加新的装填信息
        lore.add(ChatColor.GREEN + "装填: " + projectileName);
        crossbowMeta.setLore(lore);

        crossbow.setItemMeta(crossbowMeta);

        // 播放装填完成音效
        player.playSound(player.getLocation(), Sound.ITEM_CROSSBOW_LOADING_END, 1.0f, 1.0f);
        player.sendActionBar(ChatColor.GREEN + "已装填: " + projectileName);
        player.setExp(0f);

        // 清理状态
        chargingTasks.remove(playerId);
        pendingProjectiles.remove(playerId);
    }

    // 获取抛射物显示名称
    private String getProjectileDisplayName(Material material, ItemStack item) {
        if (PROJECTILE_NAMES.containsKey(material)) {
            return PROJECTILE_NAMES.get(material);
        }

        // 对于自定义物品，使用其显示名称或类型名称
        if (item.hasItemMeta() && item.getItemMeta().hasDisplayName()) {
            return ChatColor.stripColor(item.getItemMeta().getDisplayName());
        }

        // 对于方块，显示"方块"
        if (material.isBlock()) {
            return "方块";
        }

        // 默认返回物品类型名称
        return material.name().toLowerCase().replace("_", " ");
    }

    // 处理物品栏切换事件（中断装填）
    @EventHandler
    public void onPlayerItemHeld(PlayerItemHeldEvent event) {
        Player player = event.getPlayer();
        UUID playerId = player.getUniqueId();

        if (chargingTasks.containsKey(playerId)) {
            cancelCharging(player);
        }
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

        // 检查是否应用多重散射
        boolean applyMultiShot = MULTISHOT_PROJECTILES.contains(projectileType);

        if (applyMultiShot) {
            // 多重散射：发射5个抛射物
            for (int i = 0; i < 5; i++) {
                Vector spreadDirection = applySpread(direction, 5.0); // ±5度散布
                spawnProjectile(player, projectileType, projectileItem, eyeLocation, spreadDirection, 1.5f); // 1.5倍速度
            }
        } else {
            // 单发射击
            spawnProjectile(player, projectileType, projectileItem, eyeLocation, direction, 1.2f); // 1.2倍速度
        }

        player.sendActionBar(ChatColor.YELLOW + "发射了 " +
                getProjectileDisplayName(projectileType, projectileItem));
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
                // 发射龙息弹（注意：不能通过玻璃瓶装填）
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

            case ARROW:
            case SPECTRAL_ARROW:
            case TIPPED_ARROW:
                // 发射箭矢（保留正常射箭功能）
                AbstractArrow arrow;
                if (projectileType == Material.SPECTRAL_ARROW) {
                    arrow = world.spawn(location, SpectralArrow.class);
                } else if (projectileType == Material.TIPPED_ARROW) {
                    arrow = world.spawn(location, Arrow.class);
                    // 保留药水箭效果
                    if (projectileItem.hasItemMeta()) {
                        ((Arrow) arrow).setBasePotionType(((org.bukkit.inventory.meta.PotionMeta) projectileItem.getItemMeta()).getBasePotionType());
                    }
                } else {
                    arrow = world.spawn(location, Arrow.class);
                }

                arrow.setVelocity(direction.multiply(speedMultiplier * 3.0)); // 3倍原版速度
                arrow.setShooter(player);
                arrow.setPickupStatus(AbstractArrow.PickupStatus.ALLOWED);
                break;

            default:
                // 对于其他物品，检查是否是方块
                if (projectileType.isBlock()) {
                    // 尝试在落点放置方块
                    tryPlaceBlock(player, projectileType, location, direction, speedMultiplier);
                } else {
                    // 发射掉落物
                    spawnItemProjectile(player, projectileItem, location, direction, speedMultiplier);
                }
                break;
        }

        // 生成发射粒子效果
        world.spawnParticle(Particle.CRIT, location, 10, 0.1, 0.1, 0.1, 0.05);
    }

    // 尝试在落点放置方块
    private void tryPlaceBlock(Player player, Material blockType, Location startLoc, Vector direction, float speedMultiplier) {
        // 计算抛射轨迹
        Vector velocity = direction.multiply(speedMultiplier);
        Location currentLoc = startLoc.clone();

        // 模拟抛射物飞行
        new BukkitRunnable() {
            int ticks = 0;
            final int maxTicks = 100; // 最多5秒

            @Override
            public void run() {
                if (ticks++ >= maxTicks) {
                    // 超时，生成掉落物
                    spawnItemAsDrop(player, blockType, currentLoc);
                    cancel();
                    return;
                }

                // 更新位置
                currentLoc.add(velocity);

                // 检查是否碰撞到方块
                Block hitBlock = currentLoc.getBlock();
                if (!hitBlock.isEmpty()) {
                    // 找到可以放置的位置（相邻的固体方块上方）
                    Block placeBlock = findPlaceableLocation(hitBlock);
                    if (placeBlock != null && canPlaceBlock(blockType, placeBlock)) {
                        // 放置方块
                        placeBlock.setType(blockType);

                        // 播放放置音效
                        player.getWorld().playSound(placeBlock.getLocation(),
                                Sound.BLOCK_STONE_PLACE, 1.0f, 1.0f);

                        // 生成放置粒子效果
                        player.getWorld().spawnParticle(Particle.SMOKE,
                                placeBlock.getLocation().add(0.5, 0.5, 0.5),
                                10, 0.3, 0.3, 0.3, 0.1, blockType.createBlockData());
                    } else {
                        // 无法放置，生成掉落物
                        spawnItemAsDrop(player, blockType, currentLoc);
                    }

                    cancel();
                    return;
                }

                // 检查是否超出世界边界
//                if (currentLoc.getY() < world.getMinHeight() || currentLoc.getY() > world.getMaxHeight()) {
//                    cancel();
//                    return;
//                }

                // 生成飞行粒子效果
                player.getWorld().spawnParticle(Particle.CRIT, currentLoc, 1, 0, 0, 0, 0);
            }
        }.runTaskTimer(plugin, 0, 1);
    }

    // 寻找可放置的位置
    private Block findPlaceableLocation(Block hitBlock) {
        // 检查相邻的6个方向
        Block[] adjacentBlocks = {
                hitBlock.getRelative(0, 1, 0),  // 上方
                hitBlock.getRelative(0, -1, 0), // 下方
                hitBlock.getRelative(1, 0, 0),  // 东
                hitBlock.getRelative(-1, 0, 0), // 西
                hitBlock.getRelative(0, 0, 1),  // 南
                hitBlock.getRelative(0, 0, -1)  // 北
        };

        for (Block block : adjacentBlocks) {
            if (block.isEmpty() && block.getRelative(0, -1, 0).getType().isSolid()) {
                return block;
            }
        }

        return null;
    }

    // 检查是否可以放置方块
    private boolean canPlaceBlock(Material blockType, Block block) {
        // 排除液体
        if (blockType == Material.WATER || blockType == Material.LAVA) {
            return false;
        }

        // 排除不完整方块（如草、花等）
        if (!blockType.isBlock() || !blockType.isSolid()) {
            return false;
        }

        // 检查目标位置是否可替换
        return block.isEmpty();
    }

    // 生成物品抛射物
    private void spawnItemProjectile(Player player, ItemStack item, Location location, Vector direction, float speedMultiplier) {
        Item dropItem = player.getWorld().dropItem(location, item);
        dropItem.setVelocity(direction.multiply(speedMultiplier));
        dropItem.setPickupDelay(20); // 1秒后可以拾取

        // 设置物品不会被清除
        dropItem.setUnlimitedLifetime(true);
    }

    // 生成物品掉落
    private void spawnItemAsDrop(Player player, Material material, Location location) {
        ItemStack itemStack = new ItemStack(material);
        Item dropItem = player.getWorld().dropItem(location, itemStack);
        dropItem.setPickupDelay(10); // 0.5秒后可以拾取
    }
}