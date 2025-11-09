package Moon2.rPGWeapon.Weapon;

import Moon2.rPGWeapon.Weapon.Weapon;
import org.bukkit.*;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Firework;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerFishEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerItemHeldEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.FireworkMeta;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.plugin.java.JavaPlugin;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter   ;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;

import static Moon2.rPGWeapon.Main.plugin;

public class Divinationrod implements Weapon {

    private final Map<UUID, LocalDate> playerDivinationDates = new ConcurrentHashMap<>();
    private final Map<UUID, Boolean> playerDivinationStatus = new ConcurrentHashMap<>();

    // 占卜等级概率配置
    private final Map<String, Double> DIVINATION_PROBABILITIES = new LinkedHashMap<String, Double>() {{
        put("大吉", 0.05);    // 5%
        put("吉", 0.10);      // 10%
        put("半吉", 0.15);    // 15%
        put("小吉", 0.20);    // 20%
        put("末吉", 0.20);    // 20%
        put("末小吉", 0.15);  // 15%
        put("凶", 0.08);      // 8%
        put("小凶", 0.05);    // 5%
        put("末凶", 0.02);    // 2%
    }};

    // 占卜签文映射
    private final Map<String, List<String>> DIVINATION_MESSAGES = new HashMap<String, List<String>>() {{
        put("大吉", Arrays.asList(
                "今日鸿运当头，万事皆宜",
                "机遇来临，把握良机",
                "贵人相助，事业腾达"
        ));
        put("吉", Arrays.asList(
                "运势良好，稳步前行",
                "努力终有回报",
                "人际关系和谐"
        ));
        put("半吉", Arrays.asList(
                "运势平平，需谨慎行事",
                "小有收获，不可贪心",
                "中庸之道，平安是福"
        ));
        put("小吉", Arrays.asList(
                "小确幸降临身边",
                "耐心等待，好事将近",
                "平凡中见真章"
        ));
        put("末吉", Arrays.asList(
                "运势渐起，保持耐心",
                "黎明前的黑暗",
                "坚持就是胜利"
        ));
        put("末小吉", Arrays.asList(
                "微小转机出现",
                "细节决定成败",
                "积少成多，终成大器"
        ));
        put("凶", Arrays.asList(
                "今日宜静不宜动",
                "谨慎决策，避免风险",
                "困难只是暂时的"
        ));
        put("小凶", Arrays.asList(
                "小有波折，无伤大雅",
                "退一步海阔天空",
                "以柔克刚，化解危机"
        ));
        put("末凶", Arrays.asList(
                "运势低迷，韬光养晦",
                "静待时机，蓄势待发",
                "否极泰来，转机将至"
        ));
    }};

    // 音效映射
    private final Map<String, Sound> DIVINATION_SOUNDS = new HashMap<String, Sound>() {{
        put("大吉", Sound.ENTITY_PLAYER_LEVELUP);
        put("吉", Sound.BLOCK_NOTE_BLOCK_CHIME);
        put("半吉", Sound.BLOCK_NOTE_BLOCK_HARP);
        put("小吉", Sound.BLOCK_NOTE_BLOCK_BELL);
        put("末吉", Sound.BLOCK_NOTE_BLOCK_FLUTE);
        put("末小吉", Sound.BLOCK_NOTE_BLOCK_XYLOPHONE);
        put("凶", Sound.ENTITY_VILLAGER_NO);
        put("小凶", Sound.BLOCK_NOTE_BLOCK_BASS);
        put("末凶", Sound.ENTITY_VILLAGER_AMBIENT);
    }};

    // 烟花颜色映射
    private final Map<String, Color> FIREWORK_COLORS = new HashMap<String, Color>() {{
        put("大吉", Color.RED);
        put("吉", Color.ORANGE);
        put("半吉", Color.YELLOW);
        put("小吉", Color.LIME);
        put("末吉", Color.GREEN);
        put("末小吉", Color.BLUE);
        put("凶", Color.PURPLE);
        put("小凶", Color.fromRGB(128, 0, 128)); // 深紫色
        put("末凶", Color.fromRGB(64, 64, 64));   // 灰色
    }};

    @Override
    public void onEnable() {
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
        plugin.getCommand("divinationrod").setExecutor(this);
        loadPlayerData();

        // 每日重置任务
        new BukkitRunnable() {
            @Override
            public void run() {
                checkAndResetDailyDivination();
            }
        }.runTaskTimer(plugin, 0, 20 * 60 * 60); // 每小时检查一次
    }

    @Override
    public void onDisable() {

    }

    // 加载玩家数据
    private void loadPlayerData() {
        // 这里可以添加从文件或数据库加载玩家数据的逻辑
        // 暂时使用内存存储，重启后重置
    }

    // 保存玩家数据
    private void savePlayerData() {
        // 这里可以添加保存玩家数据到文件或数据库的逻辑
    }

    // 检查并重置每日占卜状态
    private void checkAndResetDailyDivination() {
        LocalDate today = LocalDate.now();

        for (Player player : Bukkit.getOnlinePlayers()) {
            UUID playerId = player.getUniqueId();
            LocalDate lastDivinationDate = playerDivinationDates.get(playerId);

            if (lastDivinationDate == null || !lastDivinationDate.equals(today)) {
                playerDivinationStatus.put(playerId, false);

                // 更新玩家手中占卜钓竿的Lore
                updateHeldDivinationRod(player);
            }
        }
    }

    // 更新玩家手中占卜钓竿的Lore
    private void updateHeldDivinationRod(Player player) {
        ItemStack item = player.getInventory().getItemInMainHand();
        if (isDivinationRod(item)) {
            updateRodLore(player, item);
        }

        item = player.getInventory().getItemInOffHand();
        if (isDivinationRod(item)) {
            updateRodLore(player, item);
        }
    }

    // 命令处理
    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (cmd.getName().equalsIgnoreCase("divinationrod")) {
            if (!(sender instanceof Player)) {
                sender.sendMessage(ChatColor.RED + "只有玩家可以使用这个命令!");
                return true;
            }

            Player player = (Player) sender;

            if (!player.hasPermission("divinationrod.get")) {
                player.sendMessage(ChatColor.RED + "你没有权限使用这个命令!");
                return true;
            }

            ItemStack divinationRod = getDivinationRod(player);
            HashMap<Integer, ItemStack> leftover = player.getInventory().addItem(divinationRod);

            if (!leftover.isEmpty()) {
                player.getWorld().dropItemNaturally(player.getLocation(), divinationRod);
                player.sendMessage(ChatColor.YELLOW + "你的库存已满，占卜钓竿已掉落在地面上!");
            } else {
                player.sendMessage(ChatColor.GREEN + "你获得了占卜钓竿!");
            }

            player.playSound(player.getLocation(), Sound.ITEM_BOOK_PAGE_TURN, 1.0f, 1.0f);

            return true;
        }
        return false;
    }

    // 获取占卜钓竿（根据玩家状态）
    public ItemStack getDivinationRod(Player player) {
        ItemStack rod = new ItemStack(Material.FISHING_ROD);
        ItemMeta meta = rod.getItemMeta();

        // 设置武器名称
        meta.setDisplayName(ChatColor.AQUA + "占卜钓竿");

        // 设置自定义模型数据
        meta.setCustomModelData(7001);

        rod.setItemMeta(meta);

        // 更新Lore显示占卜状态
        updateRodLore(player, rod);

        return rod;
    }

    // 更新钓竿Lore显示占卜状态
    private void updateRodLore(Player player, ItemStack rod) {
        ItemMeta meta = rod.getItemMeta();
        List<String> lore = new ArrayList<>();

        boolean hasDivinatedToday = hasPlayerDivinatedToday(player);

        if (hasDivinatedToday) {
            lore.add(ChatColor.RED + "今日已占卜");
            lore.add(ChatColor.GRAY + "请明日再来");
        } else {
            lore.add(ChatColor.GREEN + "今日未占卜");
            lore.add(ChatColor.GRAY + "右键钓鱼进行占卜");
        }

        lore.add(ChatColor.GRAY + "每日可进行一次命运占卜");
        lore.add(ChatColor.GRAY + "钓上命运之签，揭示今日运势");

        meta.setLore(lore);
        rod.setItemMeta(meta);
    }

    // 检查物品是否是占卜钓竿
    private boolean isDivinationRod(ItemStack item) {
        if (item == null || item.getType() != Material.FISHING_ROD || !item.hasItemMeta()) {
            return false;
        }

        ItemMeta meta = item.getItemMeta();
        return meta.hasDisplayName() &&
                meta.getDisplayName().equals(ChatColor.AQUA + "占卜钓竿");
    }

    // 检查玩家今天是否已经占卜过
    private boolean hasPlayerDivinatedToday(Player player) {
        UUID playerId = player.getUniqueId();
        LocalDate today = LocalDate.now();

        // 检查玩家是否有今日占卜记录
        LocalDate lastDivinationDate = playerDivinationDates.get(playerId);
        if (lastDivinationDate != null && lastDivinationDate.equals(today)) {
            return playerDivinationStatus.getOrDefault(playerId, false);
        }

        return false;
    }

    // 处理钓鱼事件
    @EventHandler
    public void onPlayerFish(PlayerFishEvent event) {
        Player player = event.getPlayer();
        ItemStack rod = player.getInventory().getItemInMainHand();

        if (!isDivinationRod(rod)) return;

        // 检查玩家今天是否已经占卜过
        if (hasPlayerDivinatedToday(player)) {
            // 已经占卜过，正常钓鱼
            return;
        }

        // 只在成功钓到物品时触发占卜
        if (event.getState() == PlayerFishEvent.State.CAUGHT_FISH) {
            // 取消原版钓鱼结果
            event.setCancelled(true);

            // 执行占卜
            performDivination(player, event.getHook().getLocation());

            // 更新玩家占卜状态
            playerDivinationDates.put(player.getUniqueId(), LocalDate.now());
            playerDivinationStatus.put(player.getUniqueId(), true);

            // 更新钓竿Lore
            updateRodLore(player, rod);
        }
    }

    // 执行占卜
    private void performDivination(Player player, Location hookLocation) {
        // 随机选择占卜等级
        String divinationLevel = getRandomDivinationLevel();

        // 获取签文
        String message = getRandomDivinationMessage(divinationLevel);

        // 创建占卜纸
        ItemStack divinationPaper = createDivinationPaper(divinationLevel, message);

        // 在浮标位置生成占卜纸
        Item paperItem = player.getWorld().dropItemNaturally(hookLocation, divinationPaper);
        paperItem.setPickupDelay(0);

        // 播放烟花效果
        spawnDivinationFirework(hookLocation, divinationLevel);

        // 播放音效
        playDivinationSound(player, divinationLevel);

        // 发送聊天消息
        sendDivinationMessage(player, divinationLevel, message);

        // 播放额外粒子效果
        player.getWorld().spawnParticle(Particle.ENCHANT, hookLocation, 30, 1, 1, 1, 0.5);
        player.getWorld().spawnParticle(Particle.END_ROD, hookLocation, 20, 1, 1, 1, 0.1);
    }

    // 随机选择占卜等级
    private String getRandomDivinationLevel() {
        double random = ThreadLocalRandom.current().nextDouble();
        double cumulative = 0.0;

        for (Map.Entry<String, Double> entry : DIVINATION_PROBABILITIES.entrySet()) {
            cumulative += entry.getValue();
            if (random <= cumulative) {
                return entry.getKey();
            }
        }

        return "末吉"; // 默认值
    }

    // 随机获取签文
    private String getRandomDivinationMessage(String level) {
        List<String> messages = DIVINATION_MESSAGES.get(level);
        if (messages == null || messages.isEmpty()) {
            return "今日运势平平";
        }

        int index = ThreadLocalRandom.current().nextInt(messages.size());
        return messages.get(index);
    }

    // 创建占卜纸
    private ItemStack createDivinationPaper(String level, String message) {
        ItemStack paper = new ItemStack(Material.PAPER);
        ItemMeta meta = paper.getItemMeta();

        // 设置显示名称
        String displayName = getLevelColor(level) + "占卜签·" + level;
        meta.setDisplayName(displayName);

        // 设置Lore
        List<String> lore = new ArrayList<>();
        lore.add(ChatColor.GRAY + "今日运势: " + getLevelColor(level) + level);
        lore.add("");
        lore.add(ChatColor.WHITE + "签文: " + message);
        lore.add("");
        lore.add(ChatColor.GRAY + "占卜日期: " + LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy年MM月dd日")));
        lore.add(ChatColor.GRAY + "愿此签指引你今日的方向");

        meta.setLore(lore);

        // 设置自定义模型数据
        meta.setCustomModelData(7002);

        paper.setItemMeta(meta);
        return paper;
    }

    // 获取等级对应的颜色
    private ChatColor getLevelColor(String level) {
        switch (level) {
            case "大吉": return ChatColor.RED;
            case "吉": return ChatColor.GOLD;
            case "半吉": return ChatColor.YELLOW;
            case "小吉": return ChatColor.GREEN;
            case "末吉": return ChatColor.AQUA;
            case "末小吉": return ChatColor.BLUE;
            case "凶": return ChatColor.DARK_PURPLE;
            case "小凶": return ChatColor.DARK_RED;
            case "末凶": return ChatColor.DARK_GRAY;
            default: return ChatColor.WHITE;
        }
    }

    // 生成占卜烟花
    private void spawnDivinationFirework(Location location, String level) {
        Firework firework = location.getWorld().spawn(location, Firework.class);
        FireworkMeta meta = firework.getFireworkMeta();

        // 设置烟花效果
        FireworkEffect effect = FireworkEffect.builder()
                .with(FireworkEffect.Type.BURST)
                .withColor(FIREWORK_COLORS.get(level))
                .withFade(Color.WHITE)
                .withFlicker()
                .withTrail()
                .build();

        meta.addEffect(effect);
        meta.setPower(1); // 飞行高度

        firework.setFireworkMeta(meta);

        // 立即爆炸
        new BukkitRunnable() {
            @Override
            public void run() {
                firework.detonate();
            }
        }.runTaskLater(plugin, 1);
    }

    // 播放占卜音效
    private void playDivinationSound(Player player, String level) {
        Sound sound = DIVINATION_SOUNDS.get(level);
        if (sound != null) {
            player.playSound(player.getLocation(), sound, 1.0f, 1.0f);
        }
    }

    // 发送占卜消息
    private void sendDivinationMessage(Player player, String level, String message) {
        String today = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy年MM月dd日"));

        player.sendMessage("");
        player.sendMessage(ChatColor.AQUA + "====== " + ChatColor.WHITE + "今日占卜结果" + ChatColor.AQUA + " ======");
        player.sendMessage(ChatColor.GRAY + "占卜日期: " + ChatColor.WHITE + today);
        player.sendMessage(ChatColor.GRAY + "运势等级: " + getLevelColor(level) + level);
        player.sendMessage(ChatColor.GRAY + "签文解读: " + ChatColor.WHITE + message);
        player.sendMessage(ChatColor.AQUA + "==============================");
        player.sendMessage("");
    }

    // 处理物品栏切换事件（更新Lore）
    @EventHandler
    public void onPlayerItemHeld(PlayerItemHeldEvent event) {
        Player player = event.getPlayer();
        ItemStack newItem = player.getInventory().getItem(event.getNewSlot());

        if (isDivinationRod(newItem)) {
            // 延迟一tick更新，确保物品已切换
            new BukkitRunnable() {
                @Override
                public void run() {
                    updateRodLore(player, newItem);
                }
            }.runTaskLater(plugin, 1);
        }
    }

    // 处理玩家交互事件（手持时更新Lore）
    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        ItemStack item = event.getItem();

        if (isDivinationRod(item)) {
            updateRodLore(player, item);
        }
    }
}