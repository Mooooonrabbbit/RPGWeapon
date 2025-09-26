package Moon2.rPGWeapon;

import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.List;

public class Main extends JavaPlugin {

    public static JavaPlugin plugin;
    public List<Weapon> weapons = new ArrayList<>();

    @Override
    public void onEnable() {
        plugin = this;
        weapons.add(new Heatblade());
        weapons.add(new Rocketboots());
        weapons.add(new CorruptedTrident());
        weapons.add(new Dicesword());
        weapons.add(new Weightlessbow());

        weapons.forEach(Weapon::onEnable);
        getLogger().info("插件已启用!");
    }

    @Override
    public void onDisable() {
        weapons.forEach(Weapon::onDisable);
        weapons.clear();
        getLogger().info("插件已禁用!");
    }
}
