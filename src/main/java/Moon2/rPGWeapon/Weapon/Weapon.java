package Moon2.rPGWeapon.Weapon;

import org.bukkit.command.CommandExecutor;
import org.bukkit.event.Listener;

public interface Weapon extends Listener, CommandExecutor {
    void onEnable();
    void onDisable();
}
