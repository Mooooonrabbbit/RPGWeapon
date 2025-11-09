package Moon2.rPGWeapon.Weapon;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import org.bukkit.*;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.damage.DamageSource;
import org.bukkit.damage.DamageType;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.Arrays;
import java.util.HashMap;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

import static Moon2.rPGWeapon.Main.plugin;

public class Dicesword implements Weapon {

    private final HashMap<UUID, Long> cooldowns = new HashMap<>();
    private final long COOLDOWN_TIME = 2000; // 5秒冷却时间

    // 存储所有可能的药水效果（排除一些不适宜的效果）
    private final PotionEffectType[] ALL_EFFECTS;

    public Dicesword() {
        // 初始化所有可能的药水效果
        ALL_EFFECTS = Arrays.stream(PotionEffectType.values()).filter(potionEffectType -> {
            if (potionEffectType == PotionEffectType.HERO_OF_THE_VILLAGE) return false;
            if (potionEffectType == PotionEffectType.BAD_OMEN) return false;
            return true;
        }).toArray(PotionEffectType[]::new);
    }

    @Override
    public void onEnable() {
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
        plugin.getCommand("dicesword").setExecutor(this);
    }

    @Override
    public void onDisable() {
    }

    // 命令处理
    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {

        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.RED + "只有玩家可以使用这个命令!");
            return true;
        }

        Player player = (Player) sender;

        if (!player.hasPermission("dicesword.get")) {
            player.sendMessage(ChatColor.RED + "你没有权限使用这个命令!");
            return true;
        }

        ItemStack diceSword = getDiceSword();
        HashMap<Integer, ItemStack> leftover = player.getInventory().addItem(diceSword);

        if (!leftover.isEmpty()) {
            player.getWorld().dropItemNaturally(player.getLocation(), diceSword);
            player.sendMessage(ChatColor.YELLOW + "你的库存已满，骰子大剑已掉落在地面上!");
        } else {
            player.sendMessage(ChatColor.GREEN + "你获得了骰子大剑!");
        }

        player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1.0f, 1.0f);

        return true;
    }

    // 获取骰子大剑
    public static ItemStack getDiceSword() {
        ItemStack sword = new ItemStack(Material.NETHERITE_SWORD);
        ItemMeta meta = sword.getItemMeta();

        // 设置武器名称和描述
        meta.setDisplayName(ChatColor.LIGHT_PURPLE + "骰子大剑");
        meta.setLore(Arrays.asList(
                ChatColor.GRAY + "命运之骰，不可预知",
                ChatColor.GRAY + "每次攻击都是全新的冒险",
                ChatColor.GRAY + "福兮祸所依，祸兮福所伏"
        ));
        AttributeModifier slowAttackModifier = new AttributeModifier(
                Attribute.ATTACK_SPEED.getKey(),
                -3.5,
                AttributeModifier.Operation.ADD_NUMBER,
                EquipmentSlot.HAND.getGroup()
        );
        meta.addAttributeModifier(Attribute.ATTACK_SPEED, slowAttackModifier);
        meta.setUnbreakable(true);

        // 设置自定义模型数据
        meta.setCustomModelData(3001);

        sword.setItemMeta(meta);
        return sword;
    }

    // 检查物品是否是骰子大剑
    private boolean isDiceSword(ItemStack item) {
        if (item == null || item.getType() != Material.NETHERITE_SWORD || !item.hasItemMeta()) {
            return false;
        }

        ItemMeta meta = item.getItemMeta();
        return meta.hasDisplayName() &&
                meta.getDisplayName().equals(ChatColor.LIGHT_PURPLE + "骰子大剑");
    }

    // 生成随机伤害 (-5 到 +5)
    private int getRandomDamage() {
        return ThreadLocalRandom.current().nextInt(-5, 6); // -5 到 +5
    }

    // 生成随机药水效果
    private PotionEffect getRandomEffect() {
        ThreadLocalRandom random = ThreadLocalRandom.current();

        // 随机选择效果类型
        PotionEffectType effectType = ALL_EFFECTS[random.nextInt(ALL_EFFECTS.length)];

        // 随机等级 (1-5)
        int amplifier = random.nextInt(5); // 0-4 对应等级1-5

        // 对于瞬间效果，只持续1tick，其他效果持续5秒
        int duration = isInstantEffect(effectType) ? 1 : (5 * 20); // 5秒 = 100ticks

        return new PotionEffect(effectType, duration, amplifier);
    }

    // 检查是否是瞬间效果
    private boolean isInstantEffect(PotionEffectType effectType) {
        return effectType == PotionEffectType.INSTANT_DAMAGE || effectType == PotionEffectType.INSTANT_HEALTH;
    }

    // 处理攻击事件
    @EventHandler
    public void onEntityDamage(EntityDamageByEntityEvent event) {
        Entity damager = event.getDamager();
        if (event.getDamageSource().getDamageType().equals(DamageType.GENERIC)) {
            return;
        }
        // 检查攻击者是否是玩家并且使用的是骰子大剑
        if (!(damager instanceof Player)) return;
        Player player = (Player) damager;
        ItemStack weapon = player.getInventory().getItemInMainHand();
        if (!isDiceSword(weapon)) return;

        // 检查冷却时间
        UUID playerId = player.getUniqueId();
        if (cooldowns.containsKey(playerId)) {
            long timeLeft = cooldowns.get(playerId) + COOLDOWN_TIME - System.currentTimeMillis();
            if (timeLeft > 0) {
                event.setDamage(0);
                // 冷却中，不触发特殊效果，但允许普通攻击
                player.sendActionBar(ChatColor.YELLOW + "骰子大剑冷却中: " + ((timeLeft / 1000) + 1)  + "秒");
                return;

            }
        }

        event.setDamage(0);
        // 检查受害者是否是生物
        if (!(event.getEntity() instanceof LivingEntity)) return;

        LivingEntity target = (LivingEntity) event.getEntity();

        // 取消原版伤害，应用随机伤害
        double originalDamage = event.getDamage();
        event.setDamage(0);

        // 生成随机伤害（对双方）
        int damageToSelf = getRandomDamage();
        int damageToTarget = getRandomDamage();

        // 生成随机药水效果（对双方）
        PotionEffect selfEffect = getRandomEffect();
        PotionEffect targetEffect = getRandomEffect();
        // 应用伤害（负伤害表示治疗）
        if (damageToSelf > 0) {
            player.damage(damageToTarget, DamageSource.builder(DamageType.GENERIC).build());
        } else if (damageToSelf < 0) {
            double healAmount = -damageToSelf;
            double newHealth = Math.min(player.getHealth() + healAmount, player.getMaxHealth());
            player.setHealth(newHealth);
        }

        if (damageToTarget > 0) {
            target.damage(damageToTarget, DamageSource.builder(DamageType.GENERIC).build());
        } else if (damageToTarget < 0) {
            double healAmount = -damageToTarget;
            double newHealth = Math.min(target.getHealth() + healAmount, target.getMaxHealth());
            target.setHealth(newHealth);
        }

        // 应用药水效果
        player.addPotionEffect(selfEffect);
        target.addPotionEffect(targetEffect);

        // 设置冷却时间
        cooldowns.put(playerId, System.currentTimeMillis());
        player.setCooldown(Material.NETHERITE_SWORD, 40); // 5秒冷却（20ticks/秒）

        // 生成随机粒子效果
        Location loc = target.getLocation();
        loc.getWorld().spawnParticle(Particle.ENCHANT, loc, 30, 1, 1, 1, 0.5);
        loc.getWorld().spawnParticle(Particle.CRIT, loc, 20, 1, 1, 1, 0.3);

        // 播放随机音效
        Sound[] sounds = {Sound.ENTITY_EXPERIENCE_ORB_PICKUP, Sound.BLOCK_NOTE_BLOCK_CHIME,
                Sound.ENTITY_ILLUSIONER_CAST_SPELL, Sound.ITEM_TRIDENT_RETURN};
        Sound randomSound = sounds[ThreadLocalRandom.current().nextInt(sounds.length)];
        loc.getWorld().playSound(loc, randomSound, 1.0f, 1.0f);

        // 在ActionBar显示结果
        String selfDamageText = (damageToSelf >= 0) ?
                ChatColor.RED + "+" + damageToSelf : ChatColor.GREEN + "" + damageToSelf;
        String targetDamageText = (damageToTarget >= 0) ?
                ChatColor.RED + "+" + damageToTarget : ChatColor.GREEN + "" + damageToTarget;

        TextComponent component = Component.empty()
                .append(Component.text(ChatColor.GREEN + "自己: " + selfDamageText + " "))
                .append(Component.translatable(selfEffect.getType().translationKey()))
                .append(Component.text(ChatColor.GOLD + toRomanNumeral(selfEffect.getAmplifier() + 1) + " "))
                .append(Component.text(ChatColor.WHITE + "  ||  " + ChatColor.RED + "对方: " + targetDamageText + " " + ChatColor.AQUA))
                .append(Component.translatable(targetEffect.getType().translationKey()))
                .append(Component.text(ChatColor.GOLD + toRomanNumeral(targetEffect.getAmplifier() + 1)))
                ;

        player.sendActionBar(component);

        // 添加骰子滚动动画效果
        new BukkitRunnable() {
            int count = 0;

            @Override
            public void run() {
                if (count++ >= 10) {
                    cancel();
                    return;
                }
                player.spawnParticle(Particle.CRIT, player.getLocation().add(0, 2, 0),
                        5, 0.5, 0.5, 0.5, 0.1);
            }
        }.runTaskTimer(plugin, 0, 2);
    }

    // 将数字转换为罗马数字（1-5）
    private String toRomanNumeral(int number) {
        switch (number) {
            case 1:
                return "I";
            case 2:
                return "II";
            case 3:
                return "III";
            case 4:
                return "IV";
            case 5:
                return "V";
            default:
                return "" + number;
        }
    }
}