package Moon2.rPGWeapon.Weapon;

import Moon2.rPGWeapon.Util;
import org.bukkit.*;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.CrossbowMeta;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.util.Vector;

import java.util.*;

import static Moon2.rPGWeapon.Main.plugin;

public class Multishotcrossbow implements Weapon {

    private final Map<UUID, Long> cooldowns = new HashMap<>();
    private final long COOLDOWN_TIME = 2000; // 5秒冷却时间
    private static NamespacedKey Multishotcorssbow;

    // 特殊物品显示名称映射
    private final Map<Material, String> PROJECTILE_NAMES = new HashMap<>();

    @Override
    public void onEnable() {
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
        plugin.getCommand("multishotcrossbow").setExecutor(this);
        Multishotcorssbow = new NamespacedKey(plugin, "multishot_crossbow");
    }

    @Override
    public void onDisable() {
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

        // 添加物品标志
        meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES, ItemFlag.HIDE_ENCHANTS);

        // 添加NBT标签标识这是特殊弩
        meta.getPersistentDataContainer().set(Multishotcorssbow, PersistentDataType.BYTE, (byte) 1);

        crossbow.setItemMeta(meta);
        return crossbow;
    }

    // 检查物品是否是多重散射弩
    public boolean isMultiShotCrossbow(ItemStack item) {
        if (item == null || item.getType() != Material.CROSSBOW) return false;

        ItemMeta meta = item.getItemMeta();
        if (meta == null) return false;

        return meta.getPersistentDataContainer().has(Multishotcorssbow, PersistentDataType.BYTE);
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
                player.sendActionBar(ChatColor.RED + "冷却中: " + (timeLeft / 1000) + "秒");
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
            if (offHand.isEmpty()) {
                offHand = new ItemStack(Material.ARROW);
            }
            loadProjectile(player, mainHand, crossbowMeta, offHand);

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
        player.setCooldown(Material.CROSSBOW, 2 * 20); // 5秒冷却条

        // 清空装填状态
        meta.setChargedProjectiles(new ArrayList<>());

        // 移除装填信息
        List<String> lore = meta.getLore();
        if (lore != null) {
            lore.removeIf(line -> line.contains("装填:"));
            meta.setLore(lore);
        }
        // 根据抛射物类型执行不同的发射逻辑
        Location eyeLocation = player.getEyeLocation();
        Vector direction = eyeLocation.getDirection();
        crossbow.setItemMeta(meta);

        spawnProjectile(player, projectileType, projectileItem, eyeLocation, direction, 2.0f); // 1.5倍速度

        // 播放发射音效
        player.playSound(player.getLocation(), Sound.ITEM_CROSSBOW_SHOOT, 2.0f, 1.0f);

        player.sendActionBar(ChatColor.YELLOW + "发射了 " +
                PROJECTILE_NAMES.getOrDefault(projectileType, "抛射物"));
    }


    // 发射弹药
    private void launchAmmo(Player player, ItemStack ammo) {
        // 创建发射物实体
        Snowball projectile = player.launchProjectile(Snowball.class);

        // 设置发射物的元数据
        projectile.setItem(ammo);
        projectile.setShooter(player);

        // 设置自定义名称以便识别
        projectile.setCustomName("请输入文本");
        projectile.setCustomNameVisible(false);

        // 设置速度
        Vector direction = player.getEyeLocation().getDirection();
        projectile.setVelocity(direction.multiply(2.0));
    }


    public static Map<Material, Class<? extends Projectile>> Projectiles = new HashMap<>();

    static {
        Projectiles.put(Material.WITHER_SKELETON_SKULL, WitherSkull.class);
        Projectiles.put(Material.WIND_CHARGE, WindCharge.class);
        Projectiles.put(Material.FIRE_CHARGE, SmallFireball.class);
        Projectiles.put(Material.SNOWBALL, Snowball.class);
        Projectiles.put(Material.EGG, Egg.class);
        Projectiles.put(Material.ENDER_PEARL, EnderPearl.class);
        Projectiles.put(Material.EXPERIENCE_BOTTLE, ThrownExpBottle.class);
        Projectiles.put(Material.DRAGON_BREATH, DragonFireball.class);
    }

    public static Set<Material> Materials = new HashSet<>();


    // 替换特殊抛射物
    private void spawnProjectile(Player player, Material projectileType, ItemStack projectileItem, Location location, Vector direction, float speedMultiplier) {
        World world = player.getWorld();
        Sound sound = Sound.ENTITY_ARROW_SHOOT;

        Vector velocity = direction.multiply(speedMultiplier);

        if (projectileType.isBlock()) {
            FallingBlock fallingBlock = world.spawn(location, FallingBlock.class);
            fallingBlock.setDropItem(true);
            fallingBlock.setVelocity(velocity);
            fallingBlock.shouldAutoExpire(false);
            fallingBlock.setHurtEntities(true);
            fallingBlock.setDamagePerBlock(1.2F);
            fallingBlock.setMaxDamage(20);
            fallingBlock.setBlockData(projectileType.createBlockData());
            return;
        }

        if (Materials.contains(projectileType)) {
            Snowball snowball = world.spawn(location, Snowball.class);
            snowball.setItem(projectileItem.clone());
            snowball.setVelocity(velocity);
            snowball.setShooter(player);
            snowball.setMetadata(String.valueOf(Multishotcorssbow), new FixedMetadataValue(plugin, (byte) 1));
            return;
        }

        Projectile projectile = null;
        switch (projectileType) {
            case TNT:
                // 发射激活的TNT
                TNTPrimed tnt = world.spawn(location, TNTPrimed.class);
                tnt.setVelocity(velocity);
                tnt.setFuseTicks(40); // 2秒后爆炸
                return;
            case TIPPED_ARROW:
                projectile = world.spawn(location, Projectiles.getOrDefault(projectileType, Arrow.class));
                PotionMeta itemMeta = (PotionMeta) projectileItem.getItemMeta();
                ((Arrow) projectile).setBasePotionType(itemMeta.getBasePotionType());
                break;
            case DRAGON_BREATH:
                // 发射龙息弹
                sound = Sound.ENTITY_ENDER_DRAGON_SHOOT;
                break;
            case SPLASH_POTION:
            case LINGERING_POTION:
                velocity = velocity.multiply(1.8);
                break;
            default:
                Item item = world.spawn(location, Item.class);
                item.setItemStack(projectileItem.clone());
                item.setVelocity(velocity);
                return;
        }

        if (projectile == null) {
            projectile = world.spawn(location, Projectiles.getOrDefault(projectileType, Arrow.class));
        }

        double velocityX = velocity.getX();
        double velocityZ = velocity.getZ();
        projectile.setRotation(
                (float) (Util.atan2(velocityX, velocityZ) * 180.0F / (float) Math.PI),
                (float) (Util.atan2(velocity.getY(), velocityX * velocityX + velocityZ * velocityZ) * 180.0F / (float) Math.PI)
        );
        projectile.setVelocity(velocity);
        projectile.setShooter(player);
        player.playSound(player.getLocation(), sound, 1.0f, 1.0f);

        // 生成发射粒子效果
        world.spawnParticle(Particle.CRIT, location, 10, 0.1, 0.1, 0.1, 0.05);
    }

    // 发射物击中事件
    @EventHandler
    public void onProjectileHit(ProjectileHitEvent event) {
        if (event.getEntity() instanceof Snowball snowball && snowball.hasMetadata(String.valueOf(Multishotcorssbow))) {

            ItemStack ammo = snowball.getItem();

            if (ammo.isEmpty() || ammo.getType() == Material.AIR) return;

            Location hitLocation = snowball.getLocation();

            // 根据弹药物品类型产生不同效果
            handleAmmoEffects(ammo, event, hitLocation);

            // 通用击中效果
//            hitLocation.getWorld().playSound(hitLocation, Sound.ENTITY_ARROW_HIT, 1.0f, 1.0f);
//            hitLocation.getWorld().spawnParticle(Particle.LARGE_SMOKE, hitLocation, 5, 0.1, 0.1, 0.1, 0.02);
        }
    }

    static {
        Materials.add(Material.BUCKET);
        Materials.add(Material.WATER_BUCKET);
        Materials.add(Material.LAVA_BUCKET);
        Materials.add(Material.MILK_BUCKET);
    }

    // 处理击中效果
    private void handleAmmoEffects(ItemStack ammo, ProjectileHitEvent event, Location hitLocation) {
        Material material = ammo.getType();
        switch (material) {
            case LAVA_BUCKET:
                hitLocation.getWorld().setBlockData(hitLocation, Material.LAVA.createBlockData());
                hitLocation.getWorld().playSound(hitLocation, Sound.BLOCK_LAVA_POP, 1.0f, 1.0f);
                hitLocation.getWorld().dropItemNaturally(hitLocation, ItemStack.of(Material.BUCKET));
                break;

            case WATER_BUCKET:

                hitLocation.getWorld().setBlockData(hitLocation, Material.WATER.createBlockData());
                hitLocation.getWorld().playSound(hitLocation, Sound.BLOCK_WATER_AMBIENT, 1.0f, 1.0f);
                hitLocation.getWorld().dropItemNaturally(hitLocation, ItemStack.of(Material.BUCKET));
                break;
//            case TNT:
//                hitLocation.getWorld().createExplosion(hitLocation, 3.0f, false, false);
//                break;
//
//            case EGG:
//                for (int i = 0; i < 4; i++) {
//                    hitLocation.getWorld().spawn(hitLocation, Chicken.class);
//                }
//                break;
//
//            case SNOWBALL:
//                // 雪球击中产生雪片
//                hitLocation.getWorld().spawnParticle(Particle.ITEM_SNOWBALL, hitLocation, 20, 0.5, 0.5, 0.5, 0.1);
//                break;
//
//            case FIRE_CHARGE:
//                // 火球点燃地面
//                hitLocation.getWorld().playSound(hitLocation, Sound.ITEM_FIRECHARGE_USE, 1.0f, 1.0f);
//                hitLocation.getWorld().spawnParticle(Particle.FLAME, hitLocation, 15, 0.3, 0.3, 0.3, 0.05);
//                hitLocation.getWorld().setBlockData(hitLocation, Material.FIRE.createBlockData());
//                break;
//
//            case POTION:
//                // 药水效果
//                hitLocation.getWorld().spawnParticle(Particle.SPLASH, hitLocation, 50, 1.0, 1.0, 1.0, 0.5);
//                if (event.getHitEntity() instanceof LivingEntity) {
//                    LivingEntity entity = (LivingEntity) event.getHitEntity();
//                    entity.addPotionEffect(new PotionEffect(PotionEffectType.POISON, 100, 1));
//                }
//                break;

            case COOKED_BEEF:
            case BREAD:
            case APPLE:
                // 食物击中回复生命
                if (event.getHitEntity() instanceof LivingEntity) {
                    LivingEntity entity = (LivingEntity) event.getHitEntity();

                    // 恢复生命值
                    double currentHealth = entity.getHealth();
                    double maxHealth = entity.getMaxHealth();
                    double newHealth = Math.min(currentHealth + 4, maxHealth);
                    entity.setHealth(newHealth);

                    // 播放治疗效果粒子效果
                    entity.getWorld().spawnParticle(Particle.HEART, entity.getLocation().add(0, 1, 0), 5, 0.5, 0.5, 0.5);

                    // 播放音效
                    entity.getWorld().playSound(entity.getLocation(), Sound.ENTITY_PLAYER_BURP, 1.0f, 1.0f);

                    // 玩家特殊处理
                    if (entity instanceof Player) {
                        Player player = (Player) entity;
                        player.setFoodLevel(Math.min(player.getFoodLevel() + 4, 20));
                        player.sendMessage("§a你被食物击中了，回复了生命值和饱食度!");
                    } else {
                        // 对非玩家生物显示名称（如果有）
                        String entityName = entity.getCustomName() != null ? entity.getCustomName() : entity.getName();
                        entity.getWorld().getPlayers().forEach(p ->
                                p.sendMessage("§e" + entityName + " 因食物恢复了生命值!")
                        );
                    }
                }
                break;

//            case ARROW:
//                // 箭造成额外伤害
//                if (event.getHitEntity() instanceof LivingEntity) {
//                    LivingEntity entity = (LivingEntity) event.getHitEntity();
//                    entity.damage(4.0);
//                }
//                break;
//
//            case ENDER_PEARL:
//                // 末影珍珠传送
////                if (snowball.getShooter() instanceof Player) {
////                    Player shooter = (Player) snowball.getShooter();
////                    shooter.teleport(hitLocation);
////                    shooter.playSound(hitLocation, Sound.ENTITY_ENDERMAN_TELEPORT, 1.0f, 1.0f);
////                }
//                break;


            default:
                hitLocation.getWorld().dropItemNaturally(hitLocation, ammo);
                // 对于普通物品，只是掉落该物品
//                if (material.isBlock()) {
//                    // 如果是方块，尝试放置
//                    Location placeLocation = hitLocation.clone();
//                    if (event.getHitBlockFace() != null) {
//                        placeLocation = event.getHitBlock().getRelative(event.getHitBlockFace()).getLocation();
//                    }
//
//                    if (placeLocation.getBlock().getType() == Material.AIR) {
//                        placeLocation.getBlock().setType(material);
//                        hitLocation.getWorld().playSound(hitLocation, Sound.BLOCK_STONE_PLACE, 1.0f, 1.0f);
//                    } else {
//                        // 如果不能放置，则掉落
//                        hitLocation.getWorld().dropItemNaturally(hitLocation, ammo);
//                    }
//                } else {
//                    // 非方块物品直接掉落
//                }
                break;
        }
    }

}