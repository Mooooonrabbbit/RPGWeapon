package Moon2.rPGWeapon;

import org.bukkit.*;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.util.Vector;

import java.util.Arrays;
import static Moon2.rPGWeapon.Main.plugin;
import static org.bukkit.Bukkit.getServer;
public class Specialcrossbow implements Weapon {

    private static Specialcrossbow instance;
    private NamespacedKey ammoKey;
    private NamespacedKey specialKey;

    @Override
    public void onEnable() {
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
        // 注册命令
        plugin.getCommand("specialcrossbow").setExecutor(this);
        instance = this;
        ammoKey = new NamespacedKey(plugin, "loaded_ammo");
        specialKey = new NamespacedKey(plugin, "special_crossbow");
        // 注册事件监听器
        getServer().getPluginManager().registerEvents(this, plugin);
        // 注册合成配方
        registerRecipes();
    }

    @Override
    public void onDisable() {
    }

    private void registerRecipes() {
        // 特殊弩的合成配方
        ItemStack specialCrossbow = createSpecialCrossbow();
        ShapedRecipe recipe = new ShapedRecipe(new NamespacedKey(plugin, "specialcrossbow"), specialCrossbow);

        recipe.shape("ISI", "SBS", "ISI");
        recipe.setIngredient('I', Material.IRON_INGOT);
        recipe.setIngredient('S', Material.STICK);
        recipe.setIngredient('B', Material.CROSSBOW);

        getServer().addRecipe(recipe);
    }

    // 创建特殊弩
    public ItemStack createSpecialCrossbow() {
        ItemStack crossbow = new ItemStack(Material.CROSSBOW);
        ItemMeta meta = crossbow.getItemMeta();

        // 设置基本属性
        meta.setDisplayName("§6特殊弩");
        meta.setLore(Arrays.asList(
                "§7可以装填并发射任何物品!",
                "§7右键点击装填物品",
                "§7左键点击发射物品"
        ));

        // 添加物品标志
        meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES, ItemFlag.HIDE_ENCHANTS);

        // 添加NBT标签标识这是特殊弩
        meta.getPersistentDataContainer().set(specialKey, PersistentDataType.BYTE, (byte) 1);

        crossbow.setItemMeta(meta);
        return crossbow;
    }

    // 检查是否是特殊弩
    public boolean isSpecialCrossbow(ItemStack item) {
        if (item == null || item.getType() != Material.CROSSBOW) return false;

        ItemMeta meta = item.getItemMeta();
        if (meta == null) return false;

        return meta.getPersistentDataContainer().has(specialKey, PersistentDataType.BYTE);
    }

    // 装填弹药
    public void loadAmmo(ItemStack crossbow, ItemStack ammo) {
        if (!isSpecialCrossbow(crossbow)) return;

        ItemMeta meta = crossbow.getItemMeta();

        // 存储弹药物品的类型和数量
        if (ammo != null && ammo.getType() != Material.AIR) {
            String ammoData = ammo.getType().name() + ":" + ammo.getAmount();
            meta.getPersistentDataContainer().set(ammoKey, PersistentDataType.STRING, ammoData);

            // 更新描述
            meta.setLore(Arrays.asList(
                    "§6特殊弩",
                    "§7已装填: §e" + getDisplayName(ammo.getType()),
                    "§7数量: §e" + ammo.getAmount(),
                    "§7左键点击发射"
            ));
        } else {
            // 清空装填状态
            meta.getPersistentDataContainer().remove(ammoKey);
            meta.setLore(Arrays.asList(
                    "§6特殊弩",
                    "§7右键点击装填物品",
                    "§7左键点击发射物品"
            ));
        }

        crossbow.setItemMeta(meta);
    }

    // 获取已装填的弹药
    public ItemStack getLoadedAmmo(ItemStack crossbow) {
        if (!isSpecialCrossbow(crossbow)) return null;

        ItemMeta meta = crossbow.getItemMeta();
        String ammoData = meta.getPersistentDataContainer().get(ammoKey, PersistentDataType.STRING);
        if (ammoData == null) return null;

        try {
            String[] parts = ammoData.split(":");
            Material material = Material.valueOf(parts[0]);
            int amount = Integer.parseInt(parts[1]);

            return new ItemStack(material, amount);
        } catch (Exception e) {
            return null;
        }
    }

    // 获取物品显示名称
    private String getDisplayName(Material material) {
        String name = material.name().toLowerCase().replace("_", " ");
        String[] words = name.split(" ");
        StringBuilder displayName = new StringBuilder();

        for (String word : words) {
            if (!word.isEmpty()) {
                displayName.append(Character.toUpperCase(word.charAt(0)))
                        .append(word.substring(1))
                        .append(" ");
            }
        }

        return displayName.toString().trim();
    }

    // 玩家交互事件 - 装填和发射
    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        ItemStack item = event.getItem();

        if (item == null || !isSpecialCrossbow(item)) return;

        // 右键装填
        if (event.getAction() == Action.RIGHT_CLICK_AIR || event.getAction() == Action.RIGHT_CLICK_BLOCK) {
            event.setCancelled(true);

            ItemStack ammo = null;

            // 查找主手和副手中的物品（排除特殊弩本身）
            ItemStack mainHand = player.getInventory().getItemInMainHand();
            ItemStack offHand = player.getInventory().getItemInOffHand();

            if (mainHand != null && mainHand.getType() != Material.AIR &&
                    !isSpecialCrossbow(mainHand) && mainHand != item) {
                ammo = mainHand.clone();
            } else if (offHand != null && offHand.getType() != Material.AIR &&
                    !isSpecialCrossbow(offHand) && offHand != item) {
                ammo = offHand.clone();
            }

            if (ammo != null && ammo.getType() != Material.AIR) {
                loadAmmo(item, ammo);
                player.playSound(player.getLocation(), Sound.ITEM_CROSSBOW_LOADING_MIDDLE, 1.0f, 1.0f);
                player.sendMessage("§a已装填: " + ammo.getType().name());
            } else {
                // 如果没有手持弹药物品，清空装填
                loadAmmo(item, null);
                player.playSound(player.getLocation(), Sound.ITEM_CROSSBOW_LOADING_END, 1.0f, 1.0f);
                player.sendMessage("§7已清空装填");
            }
        }
        // 左键发射
        else if (event.getAction() == Action.LEFT_CLICK_AIR || event.getAction() == Action.LEFT_CLICK_BLOCK) {
            event.setCancelled(true);

            ItemStack ammo = getLoadedAmmo(item);
            if (ammo != null && ammo.getType() != Material.AIR) {
                launchAmmo(player, ammo);

                // 减少弹药数量或清空
                if (ammo.getAmount() > 1) {
                    ammo.setAmount(ammo.getAmount() - 1);
                    loadAmmo(item, ammo);
                } else {
                    loadAmmo(item, null);
                }

                player.playSound(player.getLocation(), Sound.ITEM_CROSSBOW_SHOOT, 1.0f, 1.0f);

                // 发射粒子效果
                Location eyeLoc = player.getEyeLocation();
                Vector direction = eyeLoc.getDirection();
                for (int i = 0; i < 5; i++) {
                    Location particleLoc = eyeLoc.clone().add(direction.clone().multiply(i * 0.5));
                    player.getWorld().spawnParticle(Particle.CRIT, particleLoc, 2, 0.1, 0.1, 0.1, 0.01);
                }
            } else {
                player.sendMessage("§c请先装填物品!");
            }
        }
    }

    // 发射弹药
    private void launchAmmo(Player player, ItemStack ammo) {
        // 创建发射物实体
        Snowball projectile = player.launchProjectile(Snowball.class);

        // 设置发射物的元数据
        projectile.setItem(ammo);
        projectile.setShooter(player);

        // 设置自定义名称以便识别
        projectile.setCustomName("SpecialCrossbowAmmo");
        projectile.setCustomNameVisible(false);

        // 设置速度
        Vector direction = player.getEyeLocation().getDirection();
        projectile.setVelocity(direction.multiply(2.0));
    }

    // 发射物击中事件
    @EventHandler
    public void onProjectileHit(ProjectileHitEvent event) {
        if (!(event.getEntity() instanceof Snowball)) return;

        Snowball snowball = (Snowball) event.getEntity();
        ItemStack ammo = snowball.getItem();

        if (ammo == null || ammo.getType() == Material.AIR) return;

        Location hitLocation = snowball.getLocation();

        // 根据弹药物品类型产生不同效果
        handleAmmoEffects(ammo, event, hitLocation);

        // 通用击中效果
        hitLocation.getWorld().playSound(hitLocation, Sound.ENTITY_ARROW_HIT, 1.0f, 1.0f);
        hitLocation.getWorld().spawnParticle(Particle.LARGE_SMOKE, hitLocation, 5, 0.1, 0.1, 0.1, 0.02);
    }

    // 处理弹药效果
    private void handleAmmoEffects(ItemStack ammo, ProjectileHitEvent event, Location hitLocation) {
        Material material = ammo.getType();

        switch (material) {
            case TNT:
                hitLocation.getWorld().createExplosion(hitLocation, 3.0f, false, false);
                break;

            case LAVA_BUCKET:
                hitLocation.getWorld().setBlockData(hitLocation, Material.LAVA.createBlockData());
                hitLocation.getWorld().playSound(hitLocation, Sound.BLOCK_LAVA_POP, 1.0f, 1.0f);
                break;

            case WATER_BUCKET:
                hitLocation.getWorld().setBlockData(hitLocation, Material.WATER.createBlockData());
                hitLocation.getWorld().playSound(hitLocation, Sound.BLOCK_WATER_AMBIENT, 1.0f, 1.0f);
                break;

            case EGG:
                for (int i = 0; i < 4; i++) {
                    hitLocation.getWorld().spawn(hitLocation, Chicken.class);
                }
                break;

            case SNOWBALL:
                // 雪球击中产生雪片
                hitLocation.getWorld().spawnParticle(Particle.ITEM_SNOWBALL, hitLocation, 20, 0.5, 0.5, 0.5, 0.1);
                break;

            case FIRE_CHARGE:
                // 火球点燃地面
                hitLocation.getWorld().playSound(hitLocation, Sound.ITEM_FIRECHARGE_USE, 1.0f, 1.0f);
                hitLocation.getWorld().spawnParticle(Particle.FLAME, hitLocation, 15, 0.3, 0.3, 0.3, 0.05);
                hitLocation.getWorld().setBlockData(hitLocation, Material.FIRE.createBlockData());
                break;

            case POTION:
                // 药水效果
                hitLocation.getWorld().spawnParticle(Particle.SPLASH, hitLocation, 50, 1.0, 1.0, 1.0, 0.5);
                if (event.getHitEntity() instanceof LivingEntity) {
                    LivingEntity entity = (LivingEntity) event.getHitEntity();
                    entity.addPotionEffect(new PotionEffect(PotionEffectType.POISON, 100, 1));
                }
                break;

            case COOKED_BEEF:
            case BREAD:
            case APPLE:
                // 食物击中回复生命
                if (event.getHitEntity() instanceof LivingEntity) {
                    LivingEntity entity = (LivingEntity) event.getHitEntity();
                    if (entity instanceof Player) {
                        Player player = (Player) entity;
                        player.setFoodLevel(Math.min(player.getFoodLevel() + 4, 20));
                        player.sendMessage("§a你被食物击中了，回复了饱食度!");
                    }
                }
                break;

            case ARROW:
                // 箭造成额外伤害
                if (event.getHitEntity() instanceof LivingEntity) {
                    LivingEntity entity = (LivingEntity) event.getHitEntity();
                    entity.damage(4.0);
                }
                break;

            case ENDER_PEARL:
                // 末影珍珠传送
                Projectile snowball = null;
                if (snowball.getShooter() instanceof Player) {
                    Player shooter = (Player) snowball.getShooter();
                    shooter.teleport(hitLocation);
                    shooter.playSound(hitLocation, Sound.ENTITY_ENDERMAN_TELEPORT, 1.0f, 1.0f);
                }
                break;

            default:
                // 对于普通物品，只是掉落该物品
                if (material.isBlock()) {
                    // 如果是方块，尝试放置
                    Location placeLocation = hitLocation.clone();
                    if (event.getHitBlockFace() != null) {
                        placeLocation = event.getHitBlock().getRelative(event.getHitBlockFace()).getLocation();
                    }

                    if (placeLocation.getBlock().getType() == Material.AIR) {
                        placeLocation.getBlock().setType(material);
                        hitLocation.getWorld().playSound(hitLocation, Sound.BLOCK_STONE_PLACE, 1.0f, 1.0f);
                    } else {
                        // 如果不能放置，则掉落
                        hitLocation.getWorld().dropItemNaturally(hitLocation, ammo);
                    }
                } else {
                    // 非方块物品直接掉落
                    hitLocation.getWorld().dropItemNaturally(hitLocation, ammo);
                }
                break;
        }
    }

    // 命令执行
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("§c只有玩家可以使用此命令!");
            return true;
        }

        Player player = (Player) sender;

        if (!player.hasPermission("specialcrossbow.give")) {
            player.sendMessage("§c你没有权限使用此命令!");
            return true;
        }

        ItemStack specialCrossbow = createSpecialCrossbow();
        player.getInventory().addItem(specialCrossbow);
        player.sendMessage("§a你获得了一把特殊弩!");
        player.sendMessage("§7使用方法:");
        player.sendMessage("§7- 右键手持物品来装填");
        player.sendMessage("§7- 左键发射装填的物品");

        return true;
    }

}