package Moon2.rPGWeapon;

import org.bukkit.*;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.LeatherArmorMeta;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

import static Moon2.rPGWeapon.Main.plugin;

public class Minerhelmet implements Weapon {

    private final Map<UUID, Integer> playerTasks = new ConcurrentHashMap<>();
    private final Set<UUID> lowPowerWarned = ConcurrentHashMap.newKeySet();

    // 定义要检测的矿石类型
    private final Material[] ORE_TYPES = {
            Material.COAL_ORE, Material.DEEPSLATE_COAL_ORE,
            Material.IRON_ORE, Material.DEEPSLATE_IRON_ORE,
            Material.COPPER_ORE, Material.DEEPSLATE_COPPER_ORE,
            Material.GOLD_ORE, Material.DEEPSLATE_GOLD_ORE,
            Material.REDSTONE_ORE, Material.DEEPSLATE_REDSTONE_ORE,
            Material.EMERALD_ORE, Material.DEEPSLATE_EMERALD_ORE,
            Material.LAPIS_ORE, Material.DEEPSLATE_LAPIS_ORE,
            Material.DIAMOND_ORE, Material.DEEPSLATE_DIAMOND_ORE,
            Material.ANCIENT_DEBRIS,
            Material.NETHER_QUARTZ_ORE,
            Material.NETHER_GOLD_ORE
    };

    // 矿石显示名称映射
    private final Map<Material, String> ORE_NAMES = new HashMap<>();
    private int checkTask;

    @Override
    public void onEnable() {
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
        plugin.getCommand("minerhelmet").setExecutor(this);
        initializeOreNames();

        // 启动定时任务检查所有在线玩家
        checkTask = new BukkitRunnable() {
            @Override
            public void run() {
                for (Player player : Bukkit.getOnlinePlayers()) {
                    checkAndStartHelmetTask(player);
                }
            }
        }.runTaskTimer(plugin, 0, 20).getTaskId(); // 每秒检查一次
    }

    @Override
    public void onDisable() {
        // 取消所有运行中的任务
        Bukkit.getScheduler().cancelTask(checkTask);

        for (int taskId : playerTasks.values()) {
            Bukkit.getScheduler().cancelTask(taskId);
        }
    }

    private void initializeOreNames() {
        ORE_NAMES.put(Material.COAL_ORE, "煤矿石");
        ORE_NAMES.put(Material.DEEPSLATE_COAL_ORE, "煤矿石");
        ORE_NAMES.put(Material.IRON_ORE, "铁矿石");
        ORE_NAMES.put(Material.DEEPSLATE_IRON_ORE, "铁矿石");
        ORE_NAMES.put(Material.COPPER_ORE, "铜矿石");
        ORE_NAMES.put(Material.DEEPSLATE_COPPER_ORE, "铜矿石");
        ORE_NAMES.put(Material.GOLD_ORE, "金矿石");
        ORE_NAMES.put(Material.DEEPSLATE_GOLD_ORE, "金矿石");
        ORE_NAMES.put(Material.REDSTONE_ORE, "红石矿石");
        ORE_NAMES.put(Material.DEEPSLATE_REDSTONE_ORE, "红石矿石");
        ORE_NAMES.put(Material.EMERALD_ORE, "绿宝石矿石");
        ORE_NAMES.put(Material.DEEPSLATE_EMERALD_ORE, "绿宝石矿石");
        ORE_NAMES.put(Material.LAPIS_ORE, "青金石矿石");
        ORE_NAMES.put(Material.DEEPSLATE_LAPIS_ORE, "青金石矿石");
        ORE_NAMES.put(Material.DIAMOND_ORE, "钻石矿石");
        ORE_NAMES.put(Material.DEEPSLATE_DIAMOND_ORE, "钻石矿石");
        ORE_NAMES.put(Material.ANCIENT_DEBRIS, "远古残骸");
        ORE_NAMES.put(Material.NETHER_QUARTZ_ORE, "下界石英矿石");
        ORE_NAMES.put(Material.NETHER_GOLD_ORE, "下界金矿石");
    }

    // 命令处理
    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (cmd.getName().equalsIgnoreCase("minerhelmet")) {
            if (!(sender instanceof Player)) {
                sender.sendMessage(ChatColor.RED + "只有玩家可以使用这个命令!");
                return true;
            }

            Player player = (Player) sender;

            if (!player.hasPermission("minerhelmet.get")) {
                player.sendMessage(ChatColor.RED + "你没有权限使用这个命令!");
                return true;
            }

            ItemStack minerHelmet = getMinerHelmet();

            // 尝试添加到头盔槽位，如果已满则添加到物品栏
            if (player.getInventory().getHelmet() == null) {
                player.getInventory().setHelmet(minerHelmet);
                player.sendMessage(ChatColor.GREEN + "你已装备矿工帽子!");
            } else {
                HashMap<Integer, ItemStack> leftover = player.getInventory().addItem(minerHelmet);
                if (!leftover.isEmpty()) {
                    player.getWorld().dropItemNaturally(player.getLocation(), minerHelmet);
                    player.sendMessage(ChatColor.YELLOW + "你的库存已满，矿工帽子已掉落在地面上!");
                } else {
                    player.sendMessage(ChatColor.GREEN + "你获得了矿工帽子! 请手动装备它。");
                }
            }

            player.playSound(player.getLocation(), Sound.ITEM_ARMOR_EQUIP_LEATHER, 1.0f, 1.0f);

            return true;
        }
        return false;
    }

    // 获取矿工帽子
    public static ItemStack getMinerHelmet() {
        ItemStack helmet = new ItemStack(Material.LEATHER_HELMET);
        LeatherArmorMeta meta = (LeatherArmorMeta) helmet.getItemMeta();

        // 设置黄色染色
        meta.setColor(Color.fromRGB(255, 255, 0)); // 黄色

        // 设置名称和描述
        meta.setDisplayName(ChatColor.YELLOW + "矿工帽子");
        meta.setLore(Arrays.asList(
                ChatColor.GRAY + "专业的采矿装备",
                ChatColor.GRAY + "提供照明和矿物探测功能",
                ChatColor.GRAY + "需要定期充能"
        ));

        // 设置自定义模型数据
        meta.setCustomModelData(4001);

        helmet.setItemMeta(meta);
        return helmet;
    }

    // 检查物品是否是矿工帽子
    private boolean isMinerHelmet(ItemStack item) {
        if (item == null || item.getType() != Material.LEATHER_HELMET || !item.hasItemMeta()) {
            return false;
        }

        ItemMeta meta = item.getItemMeta();
        if (!(meta instanceof LeatherArmorMeta)) return false;

        LeatherArmorMeta leatherMeta = (LeatherArmorMeta) meta;
        Color yellow = Color.fromRGB(255, 255, 0);

        return meta.hasDisplayName() &&
                meta.getDisplayName().equals(ChatColor.YELLOW + "矿工帽子") &&
                leatherMeta.getColor().equals(yellow);
    }

    // 检查并启动帽子任务
    private void checkAndStartHelmetTask(Player player) {
        UUID playerId = player.getUniqueId();
        ItemStack helmet = player.getInventory().getHelmet();
        boolean minerHelmet = isMinerHelmet(helmet);
        // 如果玩家戴着矿工帽子且没有任务运行
        if (minerHelmet && !playerTasks.containsKey(playerId)) {
            startHelmetTask(player);
        }
        // 如果玩家没有戴矿工帽子但有任务运行
        else if (!minerHelmet && playerTasks.containsKey(playerId)) {
            stopHelmetTask(player);
        }
    }

    // 启动帽子任务
    private void startHelmetTask(Player player) {
        UUID playerId = player.getUniqueId();

        int taskId = new BukkitRunnable() {
            int tickCounter = 0;

            @Override
            public void run() {
                // 检查玩家是否还在线并戴着帽子
                if (!player.isOnline()) {
                    stopHelmetTask(player);
                    return;
                }

                ItemStack helmet = player.getInventory().getHelmet();
                if (!isMinerHelmet(helmet)) {
                    stopHelmetTask(player);
                    return;
                }

                // 每10秒（200ticks）执行一次主要逻辑
                if (tickCounter % 2 == 0) {
                    executeHelmetLogic(player, helmet);
                }

                // 每5秒（100ticks）扫描一次矿石（为了更实时的显示）
                scanAndDisplayOres(player, helmet);

                tickCounter++;
            }
        }.runTaskTimer(plugin, 0, 100).getTaskId(); // 每tick执行一次
        System.out.println(taskId);
        playerTasks.put(playerId, taskId);
    }

    // 停止帽子任务
    private void stopHelmetTask(Player player) {
        UUID playerId = player.getUniqueId();
        if (playerTasks.containsKey(playerId)) {
            Bukkit.getScheduler().cancelTask(playerTasks.get(playerId));
            playerTasks.remove(playerId);
            lowPowerWarned.remove(playerId);
        }
    }

    // 执行帽子主要逻辑
    private void executeHelmetLogic(Player player, ItemStack helmet) {
        // 检查耐久度
        int currentDurability = helmet.getType().getMaxDurability() - helmet.getDurability();

        // 如果耐久度为1，停止功能并提示
        if (currentDurability <= 1) {
            player.sendTitle(ChatColor.RED + "需要充能", ChatColor.YELLOW + "矿工帽子电力耗尽", 10, 70, 20);
            player.playSound(player.getLocation(), Sound.BLOCK_ANVIL_LAND, 1.0f, 1.0f);
            player.sendMessage(ChatColor.RED + "矿工帽子需要充能！");
            stopHelmetTask(player);
            return;
        }

        // 如果耐久度小于等于5，提示电力不足
        if (currentDurability <= 5 && !lowPowerWarned.contains(player.getUniqueId())) {
            player.sendTitle(ChatColor.YELLOW + "电力不足", ChatColor.RED + "矿工帽子即将耗尽", 10, 70, 20);
            player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BASS, 1.0f, 0.5f);
            lowPowerWarned.add(player.getUniqueId());
        } else if (currentDurability > 5) {
            lowPowerWarned.remove(player.getUniqueId());
        }

        // 消耗1点耐久度
        if (currentDurability > 1) {
            helmet.setDurability((short) (helmet.getDurability() + 1));
            player.getInventory().setHelmet(helmet);
        }

        // 给予药水效果（11秒）
        player.addPotionEffect(new PotionEffect(PotionEffectType.HASTE, 11 * 20, 0)); // 急迫1
        player.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, 11 * 20, 0)); // 速度1
        player.addPotionEffect(new PotionEffect(PotionEffectType.GLOWING, 11 * 20, 0)); // 发光1
        player.addPotionEffect(new PotionEffect(PotionEffectType.GLOWING, 11 * 20, 0)); // 发光1

        // 播放激活音效
        player.playSound(player.getLocation(), Sound.BLOCK_BEACON_ACTIVATE, 0.5f, 1.5f);
    }

    // 扫描并显示矿石
    private void scanAndDisplayOres(Player player, ItemStack helmet) {
        if ((helmet.getType().getMaxDurability() - helmet.getDurability()) < 2) return;

        Location center = player.getLocation();
        Map<String, Integer> oreCounts = new HashMap<>();
        int totalOres = 0;

        // 扫描9x9x9区域
        for (int x = -4; x <= 4; x++) {
            for (int y = -4; y <= 4; y++) {
                for (int z = -4; z <= 4; z++) {
                    Location checkLoc = center.clone().add(x, y, z);
                    Material blockType = checkLoc.getBlock().getType();

                    // 检查是否是目标矿石
                    for (Material oreType : ORE_TYPES) {
                        if (blockType == oreType) {
                            String oreName = ORE_NAMES.get(oreType);
                            oreCounts.put(oreName, oreCounts.getOrDefault(oreName, 0) + 1);
                            totalOres++;
                            break;
                        }
                    }
                }
            }
        }

        // 构建ActionBar消息
        if (totalOres > 0) {
            StringBuilder message = new StringBuilder();
            message.append(ChatColor.YELLOW).append("附近有").append(totalOres).append("个矿石，其中");

            List<String> oreEntries = new ArrayList<>();
            for (Map.Entry<String, Integer> entry : oreCounts.entrySet()) {
                oreEntries.add(entry.getValue() + entry.getKey());
            }

            message.append(String.join("，", oreEntries));
            player.sendActionBar(message.toString());
        } else {
            player.sendActionBar(ChatColor.GRAY + "附近没有检测到矿石");
        }
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        // 玩家退出时停止任务
        stopHelmetTask(event.getPlayer());
    }

//    // 事件监听器
//    @EventHandler
//    public void onPlayerJoin(PlayerJoinEvent event) {
//        // 玩家加入时检查是否戴着矿工帽子
//        Bukkit.getScheduler().runTaskLater(plugin, () -> {
//            checkAndStartHelmetTask(event.getPlayer());
//        }, 20); // 延迟1秒执行，确保玩家完全加载
//    }
//
//
//    @EventHandler
//    public void onInventoryClick(InventoryClickEvent event) {
//        // 当玩家点击装备栏时检查帽子状态
//        if (event.getWhoClicked() instanceof Player player) {
//            checkAndStartHelmetTask(player);
//        }
//    }
}