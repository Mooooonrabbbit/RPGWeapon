package Moon2.rPGWeapon;

import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.LightningStrike;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.util.*;
import static Moon2.rPGWeapon.Main.plugin;

public class Stormwand implements Weapon {

    private final Map<UUID, Long> cooldowns = new HashMap<>();
    private final long COOLDOWN_TIME = 15000; // 15秒冷却时间
    private final int MAX_DISTANCE = 100; // 最大指向距离
    private final int EXPERIENCE_COST = 100; // 经验消耗

    @Override
    public void onEnable() {
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
        plugin.getCommand("stormwand").setExecutor(this);
    }

    @Override
    public void onDisable() {
    }

    // 命令处理
    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (cmd.getName().equalsIgnoreCase("stormwand")) {
            if (!(sender instanceof Player)) {
                sender.sendMessage(ChatColor.RED + "只有玩家可以使用这个命令!");
                return true;
            }

            Player player = (Player) sender;

            if (!player.hasPermission("stormwand.get")) {
                player.sendMessage(ChatColor.RED + "你没有权限使用这个命令!");
                return true;
            }

            ItemStack stormWand = getStormWand();
            HashMap<Integer, ItemStack> leftover = player.getInventory().addItem(stormWand);

            if (!leftover.isEmpty()) {
                player.getWorld().dropItemNaturally(player.getLocation(), stormWand);
                player.sendMessage(ChatColor.YELLOW + "你的库存已满，雷暴魔法杖已掉落在地面上!");
            } else {
                player.sendMessage(ChatColor.GREEN + "你获得了雷暴魔法杖!");
            }

            player.playSound(player.getLocation(), Sound.ITEM_ARMOR_EQUIP_DIAMOND, 1.0f, 1.0f);

            return true;
        }
        return false;
    }

    // 获取雷暴魔法杖
    public static ItemStack getStormWand() {
        ItemStack wand = new ItemStack(Material.BREEZE_ROD);
        ItemMeta meta = wand.getItemMeta();

        // 设置武器名称和描述
        meta.setDisplayName(ChatColor.BLUE + "雷暴雨");
        meta.setLore(Arrays.asList(
                ChatColor.GRAY + "掌控风暴之力",
                ChatColor.GRAY + "右键召唤九道闪电",
                ChatColor.GRAY + "需消耗大量经验",
                ChatColor.RED + "冷却时间: 15秒"
        ));

        // 设置自定义模型数据
        meta.setCustomModelData(5001);

        wand.setItemMeta(meta);
        return wand;
    }

    // 检查物品是否是雷暴魔法杖
    private boolean isStormWand(ItemStack item) {
        if (item == null || item.getType() != Material.BREEZE_ROD || !item.hasItemMeta()) {
            return false;
        }

        ItemMeta meta = item.getItemMeta();
        return meta.hasDisplayName() &&
                meta.getDisplayName().equals(ChatColor.BLUE + "雷暴雨");
    }

    // 获取玩家指向的目标位置
    private Location getTargetLocation(Player player, int maxDistance) {
        // 获取玩家视线内的方块
        Block targetBlock = player.getTargetBlockExact(maxDistance);

        if (targetBlock == null || targetBlock.getType() == Material.AIR) {
            return null;
        }

        // 返回方块中心位置
        return targetBlock.getLocation().add(0.5, 1, 0.5);
    }

    // 计算九个闪电位置
    private Location[] calculateStormLocations(Location center) {
        return new Location[]{
                center.clone(),
                center.clone().add(-3, 0, 0),
                center.clone().add(3, 0, 0),
                center.clone().add(0, 0, 3),
                center.clone().add(0, 0, -3),
                center.clone().add(2, 0, 2),
                center.clone().add(2, 0, -2),
                center.clone().add(-2, 0, 2),
                center.clone().add(-2, 0, -2)
        };
    }

    // 播放蓄力粒子效果
    private void playChargingParticles(Player player, Location target) {
        // 玩家头顶粒子
        Location playerHead = player.getLocation().add(0, 2, 0);
        player.spawnParticle(Particle.ELECTRIC_SPARK, playerHead, 20, 0.5, 0.5, 0.5, 0.1);

        // 目标位置粒子
        player.spawnParticle(Particle.END_ROD, target, 40, 2, 1, 2, 0.1,null,true);

        // 连接线粒子效果
        new BukkitRunnable() {
            int count = 0;
            @Override
            public void run() {
                if (count++ >= 10) { // 持续0.5秒
                    cancel();
                    return;
                }

                // 在玩家和目标之间生成粒子轨迹
                Vector direction = target.toVector().subtract(playerHead.toVector());
                double distance = direction.length();
                direction.normalize();

                for (double i = 0; i < distance; i += 2) {
                    Location point = playerHead.clone().add(direction.clone().multiply(i));
                    player.spawnParticle(Particle.ELECTRIC_SPARK, point, 1, 0.3, 0.3, 0.3, 0, null,true);
                }
            }
        }.runTaskTimer(plugin, 0, 1);
    }

    // 生成闪电打击
    private void spawnLightningStrikes(Player player, Location[] locations) {
        Set<LivingEntity> damagedEntities = new HashSet<>();

        for (Location loc : locations) {
            // 在世界中生成闪电
            LightningStrike lightning = loc.getWorld().strikeLightning(loc);

            // 获取闪电击中的生物并应用伤害
            for (LivingEntity entity : loc.getNearbyLivingEntities(3)) {
                if (!damagedEntities.contains(entity) && entity != player) {
                    entity.damage(15, player);
                    damagedEntities.add(entity);
                }
            }

            // 生成闪电粒子效果
            loc.getWorld().spawnParticle(Particle.FLASH, loc, 5, 0.5, 0.5, 0.5, 0);
        }
    }

    // 播放音效
    private void playStormSounds(Player player, Location center) {
        // 引雷音效
        player.getWorld().playSound(center, Sound.ITEM_TRIDENT_THUNDER, 2.0f, 1.0f);

        // 使用时的准备音效
        player.playSound(player.getLocation(), Sound.BLOCK_BEACON_ACTIVATE, 1.0f, 0.8f);

        // 冷却完毕音效（15秒后播放）
        new BukkitRunnable() {
            @Override
            public void run() {
                if (player.isOnline()) {
                    player.playSound(player.getLocation(),
                            Sound.BLOCK_BEACON_DEACTIVATE, 1.0f, 1.2f);
                    player.sendActionBar(ChatColor.AQUA + "⚡ 雷暴雨已就绪！");
                }
            }
        }.runTaskLater(plugin, 15 * 20);
    }

    // 设置客户端天气效果
    private void setClientWeather(Player caster, Location center) {
        // 获取周围100格内的玩家
        Collection<Player> nearbyPlayers = new ArrayList<>();
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (player.getWorld().equals(center.getWorld()) &&
                    player.getLocation().distance(center) <= 100) {
                nearbyPlayers.add(player);
            }
        }

        // 为每个玩家设置客户端天气
        for (Player player : nearbyPlayers) {
            player.sendTitle("", ChatColor.BLUE + "⚡ 雷暴降临！", 10, 60, 10);
            player.setPlayerWeather(org.bukkit.WeatherType.DOWNFALL);
        }

        // 3min后恢复原天气
        new BukkitRunnable() {
            @Override
            public void run() {
                for (Player player : nearbyPlayers) {
                    if (player.isOnline()) {
                        player.resetPlayerWeather();
                        player.sendActionBar(ChatColor.GREEN + "*乌云退散");
                        player.playSound(player.getLocation(),
                                Sound.BLOCK_AMETHYST_CLUSTER_PLACE, 1.0f, 1.4f);
                    }
                }
            }
        }.runTaskLater(plugin, 15 * 20); // 15s
    }

    // 处理玩家交互事件
    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        ItemStack item = event.getItem();

        // 只处理右键点击
        if (event.getAction() != Action.RIGHT_CLICK_AIR &&
                event.getAction() != Action.RIGHT_CLICK_BLOCK) {
            return;
        }

        if (!isStormWand(item)) return;

        event.setCancelled(true); // 防止可能的方块交互

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

        // 检查经验值
        if (player.getTotalExperience() < EXPERIENCE_COST) {
            player.sendMessage(ChatColor.RED + "经验值不足！需要 " + EXPERIENCE_COST + " 点经验。");
            player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BASS, 1.0f, 0.5f);
            return;
        }

        // 计算目标位置
        Location targetLoc = getTargetLocation(player, MAX_DISTANCE);
        if (targetLoc == null) {
            player.sendMessage(ChatColor.RED + "目标距离太远或没有指向有效方块！");
            player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BASS, 1.0f, 0.5f);
            return;
        }

        // 执行魔法效果
        executeStormMagic(player, targetLoc);
        executeStormDecoration(player, targetLoc);
    }


    private void executeStormDecoration(Player player, Location center) {
        // 先播放蓄力效果
        playChargingParticles(player, center);

        // 播放准备音效
        player.playSound(player.getLocation(), Sound.BLOCK_BEACON_POWER_SELECT, 1.0f, 1.0f);

        // 执行装饰效果，直接进入冷却
        new BukkitRunnable() {
            @Override
            public void run() {
                // 消耗经验值
                player.giveExp(-EXPERIENCE_COST);

                // 设置冷却
                cooldowns.put(player.getUniqueId(), System.currentTimeMillis());
                player.setCooldown(Material.BREEZE_ROD, 15 * 20); // 15秒冷却条

                // 应用负面效果
                player.addPotionEffect(new PotionEffect(PotionEffectType.WEAKNESS, 15 * 20, 0));
                player.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 2 * 20, 3));

                // 设置客户端天气
                setClientWeather(player, center);
            }
        }.runTaskLater(plugin, 0);
    }

    // 执行雷暴魔法
    private void executeStormMagic(Player player, Location center) {
        // 先播放蓄力效果
        playChargingParticles(player, center);

        // 播放准备音效
        player.playSound(player.getLocation(), Sound.BLOCK_BEACON_POWER_SELECT, 1.0f, 1.0f);

        new BukkitRunnable() {
            @Override
            public void run() {
//                // 消耗经验值
//                player.giveExp(-EXPERIENCE_COST);
//
//                // 设置冷却
//                cooldowns.put(player.getUniqueId(), System.currentTimeMillis());
//                player.setCooldown(Material.BREEZE_ROD, 15 * 20); // 15秒冷却条
//
//                // 应用负面效果
//                player.addPotionEffect(new PotionEffect(PotionEffectType.WEAKNESS, 15 * 20, 0));
//                player.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 2 * 20, 3));
//                // 设置客户端天气
//                setClientWeather(player, center);

                // 生成闪电
                Location[] locations = calculateStormLocations(center);
                spawnLightningStrikes(player, locations);

                // 播放音效
                playStormSounds(player, center);

                // 发送使用消息
                player.sendActionBar(ChatColor.BLUE + "⚡ 召唤了雷暴！");
            }
        }.runTaskLater(plugin, 30); // 1.5秒延迟用于蓄力效果
    }


}