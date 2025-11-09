package Moon2.rPGWeapon.Weapon;

import org.bukkit.*;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerInputEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.HashMap;
import java.util.UUID;

import static Moon2.rPGWeapon.Main.plugin;

public class Rocketboots implements Weapon {

    private final HashMap<UUID, Boolean> rocketActive = new HashMap<>();
    public final HashMap<UUID, Integer> durabilityTask = new HashMap<>();


    @Override
    public void onEnable() {
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
        plugin.getCommand("rocketboot").setExecutor(this);
    }

    @Override
    public void onDisable() {
        // 取消所有正在运行的任务
        for (Integer taskId : durabilityTask.values()) {
            if (taskId != null) {
                plugin.getServer().getScheduler().cancelTask(taskId);
            }
        }

    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String @NotNull [] args) {
        Player player = (Player) sender;

        ItemStack rocketBoots = getRocketBoots();

        // 检查玩家是否已经穿戴了靴子
        if (player.getInventory().getBoots() != null) {
            // 如果已经穿了靴子，尝试添加到库存
            HashMap<Integer, ItemStack> leftover = player.getInventory().addItem(rocketBoots);
            if (!leftover.isEmpty()) {
                player.getWorld().dropItemNaturally(player.getLocation(), rocketBoots);
                player.sendMessage(ChatColor.YELLOW + "你的库存已满，火箭靴已掉落在地面上!");
            } else {
                player.sendMessage(ChatColor.GREEN + "你获得了火箭靴! 请手动装备它们.");
            }
        } else {
            // 直接装备火箭靴
            player.getInventory().setBoots(rocketBoots);
            player.sendMessage(ChatColor.GREEN + "你已装备火箭靴!");
        }

        player.playSound(player.getLocation(), Sound.ITEM_ARMOR_EQUIP_IRON, 1.0f, 1.0f);

        return true;
    }

    // 获取火箭靴
    public static ItemStack getRocketBoots() {
        ItemStack boots = new ItemStack(Material.GOLDEN_BOOTS);
        ItemMeta meta = boots.getItemMeta();
        // 设置名称和描述
        meta.setDisplayName(ChatColor.GOLD + "火箭靴");
        meta.setLore(Arrays.asList(
                ChatColor.GRAY + "高科技喷气推进靴子",
                ChatColor.GRAY + "下蹲激活火箭推进",
                ChatColor.GRAY + "减少摔落伤害，增加跳跃高度"
        ));
        // 设置自定义模型数据
        meta.setCustomModelData(1001);

        boots.setItemMeta(meta);
        return boots;
    }

    // 检查物品是否是火箭靴
    private boolean isRocketBoots(ItemStack item) {
        if (item == null || item.getType() != Material.GOLDEN_BOOTS || !item.hasItemMeta()) {
            return false;
        }

        ItemMeta meta = item.getItemMeta();
        return meta.hasDisplayName() &&
                meta.getDisplayName().equals(ChatColor.GOLD + "火箭靴");
    }

    // 处理下蹲事件
    @EventHandler
    public void onPlayerJump(PlayerInputEvent event) {
        Player player = event.getPlayer();
        ItemStack boots = player.getInventory().getBoots();

        // 检查是否穿着火箭靴
        if (boots == null || !isRocketBoots(boots)) {
            return;
        }
        // 检查是否开始下蹲
        if (event.getInput().isJump()) {
            // 激活火箭靴
            rocketActive.put(player.getUniqueId(), true);

            // 播放激活音效
            player.getWorld().playSound(player.getLocation(), Sound.BLOCK_FIRE_EXTINGUISH, 1.0f, 0.8f);

            // 启动耐久度消耗任务
            int taskId = new BukkitRunnable() {
                @Override
                public void run() {
                    if (!rocketActive.getOrDefault(player.getUniqueId(), false) ||
                            !player.isOnline() ||
                            !isRocketBoots(player.getInventory().getBoots())) {
                        // 如果火箭靴未激活或玩家离线或不再穿着火箭靴，取消任务
                        cancel();
                        durabilityTask.remove(player.getUniqueId());
                        return;
                    }

                    // 消耗耐久度
                    short newDurability = (short) (boots.getDurability() + 1);

                    // 检查耐久度是否耗尽
                    if (newDurability >= boots.getType().getMaxDurability()) {
                        // 火箭靴损坏
                        player.getInventory().setBoots(null);
                        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_ITEM_BREAK, 1.0f, 1.0f);
                        player.sendMessage(ChatColor.RED + "你的火箭靴已损坏!");
                        rocketActive.put(player.getUniqueId(), false);
                        cancel();
                        durabilityTask.remove(player.getUniqueId());
                        return;
                    }

                    // 设置新耐久度
                    boots.setDurability(newDurability);
                    player.getInventory().setBoots(boots);

                    // 检查耐久度是否低于20
                    int remaining = boots.getType().getMaxDurability() - newDurability;
                    if (remaining < 20) {
                        player.sendTitle(ChatColor.RED + "燃料不足!",
                                ChatColor.YELLOW + "",
                                10, 40, 10);
                        player.getWorld().playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BELL, 1.0f, 1.2f);
                    }

                }
            }.runTaskTimer(plugin, 0, 20).getTaskId(); // 每20ticks(1秒)执行一次

            durabilityTask.put(player.getUniqueId(), taskId);
        } else if(rocketActive.getOrDefault(player.getUniqueId(), false)) {

            // 停止下蹲，关闭火箭靴
            rocketActive.put(player.getUniqueId(), false);

            // 如果有耐久度任务，取消它
            if (durabilityTask.containsKey(player.getUniqueId())) {
                plugin.getServer().getScheduler().cancelTask(durabilityTask.get(player.getUniqueId()));
                durabilityTask.remove(player.getUniqueId());
            }

            // 播放关闭音效
            player.getWorld().playSound(player.getLocation(), Sound.BLOCK_LAVA_EXTINGUISH, 0.5f, 1.0f);
        }
    }

    // 处理玩家移动事件
    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        // 检查是否穿着火箭靴并且激活
        if (!rocketActive.getOrDefault(player.getUniqueId(), false)) {
            return;
        }
        System.out.println("Velocity=" + player.getVelocity());

        ItemStack boots = player.getInventory().getBoots();
        if (boots == null || !isRocketBoots(boots)) {
            rocketActive.put(player.getUniqueId(), false);
            return;
        }

        Vector currentVel = player.getVelocity();
        // 获取玩家当前的 velocity 向量
        Vector newVel = new Vector(currentVel.getX(),0.6, currentVel.getZ());

        player.setVelocity(newVel);
        // 生成粒子效果
        Location loc = player.getLocation().add(0, 0.1, 0);
        player.getWorld().spawnParticle(Particle.LARGE_SMOKE, loc, 10, 0.2, 0, 0.2, 0.02);
        player.getWorld().spawnParticle(Particle.FLAME, loc, 5, 0.2, 0, 0.2, 0.01);

        // 每隔一段时间播放推进音效
        if (player.getTicksLived() % 10 == 0) {
            player.getWorld().playSound(loc, Sound.BLOCK_FURNACE_FIRE_CRACKLE, 0.3f, 1.5f);
        }
    }

    // 处理摔落伤害事件
    @EventHandler
    public void onEntityDamage(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Player)) {
            return;
        }

        Player player = (Player) event.getEntity();

        // 检查是否是摔落伤害
        if (event.getCause() != EntityDamageEvent.DamageCause.FALL) {
            return;
        }

        // 检查是否穿着火箭靴
        ItemStack boots = player.getInventory().getBoots();
        if (boots == null || !isRocketBoots(boots)) {
            return;
        }

        // 减少80%摔落伤害
        event.setDamage(event.getDamage() * 0.2);

        // 播放缓冲音效
        player.getWorld().playSound(player.getLocation(), Sound.BLOCK_SLIME_BLOCK_FALL, 0.7f, 1.0f);
    }

}