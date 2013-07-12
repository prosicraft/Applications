/*
 * 
 *      Application system
 * 
 */
package com.prosicraft.applications;

import com.prosicraft.applications.util.MConfiguration;
import com.prosicraft.applications.util.MLog;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.milkbowl.vault.permission.Permission;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.PluginDescriptionFile;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;

/**
 *
 * @author prosicraft
 */
public class Applications extends JavaPlugin {
        
        private PluginDescriptionFile pdfFile   = null;
        public  Map<Integer, Apply> applies     = new HashMap<>();
        public  List<Notification> notifies     = new ArrayList<>();
        public  Map<String, String> promotions  = new HashMap<>();
        public  int neededVotes                 = 5;
        public  double neededVoteValue          = 3.0;
        public  int maxApplies                  = 5;
        public  int applycount                  = 0;
        public  PlayerListener listener         = null;
        public  MConfiguration config           = null;
        public  boolean freshconfig             = false;
        public  Permission perms                = null;
        public  String freePassword             = "apassword";
        
        @Override
        public void onEnable () {
                
                pdfFile = this.getDescription();                                
                
                listener = new PlayerListener (this);
                getServer().getPluginManager().registerEvents(listener, this);               
                
                initConfig();
                
                load();
                if ( freshconfig ) {
                        promotions.put("#default", "Group1");
                        promotions.put("Group1", "Group2");
                        promotions.put("Group2", "Group3");
                        save();
                }
                
                MLog.i ("Started Version " + pdfFile.getVersion());
                
        }

        @Override
        public void onDisable () {
                
                MLog.i ("Shutted Down");
                
        }       
        
        public void load () {
                config.load();
                neededVotes = config.getInt("neededVotes", neededVotes);
                neededVoteValue = config.getDouble("neededVoteAverage", neededVoteValue);
                applycount = config.getInt("applycount", applycount);
                maxApplies = config.getInt("maxApplies", maxApplies);
                freePassword = config.getString("freePassword", freePassword);
                
                for ( String s : config.getKeys("notifications") ) {                       
                        notifies.add(new Notification(s, config.getString("notifications." + s + ".message"), config.getInt("notifications." + s + ".value", 0)));
                }
                
                for ( String s : config.getKeys("applies") ) {
                        int id = Integer.parseInt(s);
                        World w = getServer().getWorld(config.getString("applies." + s + ".world"));
                        if (w == null) {
                                MLog.e("Unknown world '" + config.getString("applies." + s + ".world") + "' for apply " + s);
                                continue;
                        }
                        double x = config.getDouble("applies." + s + ".x", 0.0);
                        double y = config.getDouble("applies." + s + ".y", 0.0);
                        double z = config.getDouble("applies." + s + ".z", 0.0);
                        Apply a = new Apply(new Location(w, x,y,z), config.getString("applies." + s + ".name"));
                        a.accepted = config.getBoolean("applies." + s + ".accepted", false);
                        a.averageVote = config.getDouble("applies." + s + ".averageVote", 0);
                        a.values = config.getIntegerList("applies." + s + ".values", null);
                        a.id = id;
                        a.voters = config.getStringList("applies." + s + ".voters", null);
                        applies.put(id, a);
                }
                
                promotions.clear();
                for ( String g : config.getKeys("promotions") ) {
                        promotions.put(g, config.getString("promotions." + g));
                }
        }
        
        public void save () {
                config.clear();
                config.set("neededVotes", neededVotes);
                config.set("neededVoteAverage", neededVoteValue);                
                config.set("applycount", applycount);
                config.set("maxApplies", maxApplies);
                config.set("freePassword", freePassword);
                
                for ( int n : applies.keySet() ) {
                        config.set("applies." +n + ".name", applies.get(n).name );
                        config.set("applies." +n + ".accepted", applies.get(n).accepted);
                        config.set("applies." +n + ".averageVote", applies.get(n).averageVote);
                        config.set("applies." +n + ".values", applies.get(n).values);
                        config.set("applies." +n + ".voters", applies.get(n).voters);
                        config.set("applies." +n + ".world", applies.get(n).location.getWorld().getName());
                        config.set("applies." +n + ".x", applies.get(n).location.getX());
                        config.set("applies." +n + ".y", applies.get(n).location.getY());
                        config.set("applies." +n + ".z", applies.get(n).location.getZ());
                }
                
                for ( Notification n : notifies ) {
                        config.set("notifications." + n.receiver + ".message", n.message);
                        config.set("notifications." + n.receiver + ".value", n.value);
                }
                
                for ( String g : promotions.keySet() ) {
                        config.set("promotions." + g, promotions.get(g));
                }
                
                config.save();
        }
        
        public void initConfig ()
        {
                
                if (this.config != null) return;
                
                if ( !this.getDataFolder().exists() && !getDataFolder().mkdirs() )
                        MLog.e("Can't create missing configuration Folder for UltraChat");

                File cf = new File(this.getDataFolder(),"config.yml");

                if (!cf.exists()) {
                        try {
                                MLog.w("Configuration File doesn't exist. Trying to recreate it...");
                                if (!cf.createNewFile() || !cf.exists())
                                {
                                        MLog.e("Placement of Plugin might be wrong or has no Permissions to access configuration file.");
                                }
                                freshconfig = true;
                        } catch (IOException iex) {
                                MLog.e("Can't create unexisting configuration file");
                        }
                }

                config = new MConfiguration (YamlConfiguration.loadConfiguration(cf), cf);

                config.load();
                
        }
        
        @Override
        public boolean onCommand(CommandSender sender, Command cmd, String commandLabel, String[] args) {
                
                if ( sender instanceof Player )
                {
                        Player p = (Player) sender;
                        
                        if ( cmd.getName().equalsIgnoreCase("apply") )
                        {
                                // Notify All players here
                                if ( !(args.length == 1 && args[0].equalsIgnoreCase("reload")) )
                                        notifyPlayers();
                                
                                if ( args.length == 0 )
                                        p.sendMessage (ChatColor.GOLD + "Running Applications version " + pdfFile.getVersion() + " by prosicraft");
                                else if ( args.length == 1 && args[0].equalsIgnoreCase("reload") )
                                {                                        
                                        if ( !p.hasPermission("applications.reload") && !p.isOp() ) return np(p);
                                        load ();
                                        p.sendMessage (ChatColor.GREEN + "Reload Done.");
                                        return true;
                                }
                                else if ( args.length == 2 && args[0].equalsIgnoreCase("setcommand") )
                                {
                                        if ( !p.hasPermission("applications.setcommand") && !p.isOp() ) return np(p);
                                        freePassword = args[1];
                                        save();
                                        p.sendMessage ("Set FreePassword successfully.");
                                        return true;
                                }
                                else if ( args.length == 1 && args[0].equalsIgnoreCase("open") )
                                {
                                        if ( !p.hasPermission("applications.open") && !p.isOp() ) return np(p);
                                        int cnt = 0;
                                        for ( Integer i : applies.keySet() )
                                                if ( applies.get(i).name.equalsIgnoreCase(p.getName()) ) cnt++;
                                        if ( cnt == maxApplies ) {
                                                p.sendMessage (ChatColor.RED + "Entschuldigung, du kannst dich nur " + ChatColor.GRAY + maxApplies + ChatColor.RED + " mal bewerben.");
                                                return true;
                                        }
                                        applies.put(applycount + 1, new Apply(p.getLocation(), p.getName(), applycount+ 1));                                        
                                        notifies.add(new Notification ("#all", ChatColor.GRAY + p.getName() + " bewirbt sich und bittet um Bewertung. Anschauen mit " + ChatColor.AQUA + "/apply warp " + (applycount + 1), 0, p.getName()));
                                        applycount++;
                                        p.sendMessage (ChatColor.GRAY + "Deine Bewerbung wurde " + ChatColor.GREEN + "angenommen" + ChatColor.GRAY + ".");
                                        p.sendMessage (ChatColor.GRAY + "Bitte warte, bis " + neededVotes + " Stimmen mit einem Wert von " + Double.toString(neededVoteValue) + " oder mehr abgestimmt haben.");
                                        p.sendMessage (ChatColor.GRAY + "Du kannst den Status mit " + ChatColor.WHITE + "/apply status" + ChatColor.GRAY + " abfragen.");                                                                                
                                        notifyPlayers();
                                        save();
                                        return true;
                                }
                                else if ( args.length == 2 && args[0].equalsIgnoreCase("close") )
                                {
                                        if ( !p.hasPermission("applications.close") && !p.isOp() ) return np(p);
                                        if ( !isInteger(args[1]) ) {
                                                p.sendMessage (ChatColor.RED + "Der Zweite Parameter muss numerisch sein. (ID)");
                                                return true;
                                        }                                        
                                        if ( !applies.keySet().contains(Integer.parseInt(args[1])) ) {
                                                p.sendMessage (ChatColor.RED + "Eine Bewerbung mit der ID " + ChatColor.GRAY + args[1] + ChatColor.RED + " wurde nicht gefunden.");
                                                return true;
                                        }                                                     
                                        notifies.add (new Notification (applies.get(Integer.parseInt(args[1])).name, ChatColor.GRAY + "Deine Bewerbung mit der ID " + ChatColor.AQUA + args[1] + ChatColor.GRAY +
                                                                                        " wurde von " + ChatColor.AQUA + p.getName() + ChatColor.GRAY + " geschlossen."));
                                        applies.remove(Integer.parseInt(args[1]));
                                        p.sendMessage (ChatColor.GREEN + "Die Bewerbung wurde geschlossen, der Spieler wird benachrichtigt.");                                                                                
                                        notifyPlayers();
                                        save();
                                        return true;
                                }
                                else if ( args.length == 3 && args[0].equalsIgnoreCase("vote") )
                                {
                                        if ( !p.hasPermission("applications.vote") && !p.isOp() ) return np(p);
                                        if ( !isInteger(args[1]) ) {
                                                p.sendMessage (ChatColor.RED + "Der Zweite Parameter muss numerisch sein. (ID)");
                                                return true;
                                        }
                                        if ( !isInteger(args[2]) ) {
                                                p.sendMessage (ChatColor.RED + "Der Dritte Parameter muss numerisch sein. (Wertung)");
                                                return true;
                                        } 
                                        if ( !applies.keySet().contains(Integer.parseInt(args[1])) ) {
                                                p.sendMessage (ChatColor.RED + "Eine Bewerbung mit der ID " + ChatColor.GRAY + args[1] + ChatColor.RED + " wurde nicht gefunden.");
                                                return true;
                                        }                                        
                                        int id = Integer.parseInt(args[1]);
                                        if ( applies.get(id).name.equalsIgnoreCase(p.getName()) ) {
                                                p.sendMessage (ChatColor.RED + "Du kannst deine eigene Bewerbung nicht selbst bewerten.");
                                                return true;
                                        }
                                        if ( applies.get(id).accepted ) {
                                                p.sendMessage (ChatColor.RED + "Diese Bewerbung wurde bereits aktzeptiert.");
                                        }
                                        if ( !applies.get(id).vote(Integer.parseInt(args[2]), p) ) {
                                                p.sendMessage (ChatColor.RED + "Du hast diese Bewerbung bereits bewertet.");
                                                return true;
                                        }
                                        notifies.add(new Notification (applies.get(id).name, ChatColor.GRAY + "Eine deiner Bewerbungen wurde mit " + ChatColor.AQUA + Integer.parseInt(args[2]) + ChatColor.GRAY + " bewertet. Ansehen mit " + ChatColor.WHITE + "/apply warp " + id));                                        
                                        p.sendMessage (ChatColor.GREEN + "Du hast erfolgreich eine Bewerbung von " + applies.get(id).name +" mit " + ChatColor.GRAY + Integer.parseInt(args[2]) + ChatColor.GREEN + " bewertet.");
                                        checkPlayerVotes ();
                                        if ( applies.get(id).accepted ) {
                                                p.sendMessage (ChatColor.GRAY + "Diese Bewerbung ist nun aktzeptiert. Der Benutzer wurde benachrichtigt.");
                                        }                                        
                                        notifyPlayers();
                                        save();
                                        return true;
                                }
                                else if ( args.length == 2 && args[0].equalsIgnoreCase("warp") )
                                {
                                        if ( (!p.hasPermission("applications.warp")) && !p.isOp() ) return np(p);
                                        if ( !isInteger(args[1]) ) {
                                                p.sendMessage (ChatColor.RED + "Der Zweite Parameter muss numerisch sein. (ID)");
                                                return true;
                                        }                                        
                                        if ( !applies.keySet().contains(Integer.parseInt(args[1])) ) {
                                                p.sendMessage (ChatColor.RED + "Eine Bewerbung mit der ID " + ChatColor.GRAY + args[1] + ChatColor.RED + " wurde nicht gefunden.");
                                                return true;
                                        }    
                                        int id = Integer.parseInt(args[1]);
                                        if ( !applies.get(id).name.equalsIgnoreCase(p.getName()) && !p.hasPermission("applications.warp.other") ) {
                                                p.sendMessage (ChatColor.RED + "Du darfst dich nicht zu den Bewerbungen anderer teleportieren.");
                                                return true;
                                        }                                                                                        
                                        applies.get(id).location.getChunk().load();
                                        p.teleport(applies.get(id).location);
                                        p.sendMessage (ChatColor.GRAY + "Du wurdest zu einer Bewerbungen teleportiert. ID: " + id);                                        
                                        notifyPlayers();
                                        save();
                                        return true;
                                }
                                else if ( args.length == 1 && args[0].equalsIgnoreCase("list") )
                                {
                                        if ( (!p.hasPermission("applications.list")) && !p.isOp() ) return np(p);                                        
                                        int cnt = 0;
                                        for ( int i : applies.keySet() ) {
                                                Apply a = applies.get(i);
                                                p.sendMessage (ChatColor.DARK_GRAY + "ID: " + ChatColor.GOLD + i + ChatColor.DARK_GRAY + " : " + ChatColor.WHITE + "Bewerbung von " + a.name + " (" + a.averageVote + ", " + a.voters.size() + " votes) " + ChatColor.GREEN + ((a.voters.contains(p.getName())) ? "gevotet" : "" ));
                                                cnt++;
                                        }
                                        if ( cnt == 0 )
                                                p.sendMessage (ChatColor.RED + "Keine Bewerbungen vorhanden.");
                                        return true;
                                }
                                else
                                {
                                        p.sendMessage (ChatColor.RED + "Unknown command " + ChatColor.GRAY + "/apply " + args[0] + ChatColor.RED + " or too few arguments.");                                        
                                }
                                
                        }
                }
                
                return false;
                
        }
        
        // =====================================================================
        //      Load Vault permissions
        private boolean setupPermissions()
        {
                RegisteredServiceProvider<Permission> permissionProvider = getServer().getServicesManager().getRegistration(net.milkbowl.vault.permission.Permission.class);
                if (permissionProvider != null) {
                        perms = permissionProvider.getProvider();
                }
                if (perms != null)
                        MLog.i("Hooked into permissions");
                return (perms != null);
        }
        
        public void checkPlayerVotes () {
                for ( int i : applies.keySet() ) {
                        Apply a = applies.get(i);                                               
                        
                        if ( !a.accepted && a.voters.size() >= this.neededVotes && a.averageVote >= this.neededVoteValue) {
                                applies.get(i).accepted = true;
                                MLog.d( "Send Notification of application a.id=" + a.id + " with name a.name=" + a.name);
                                notifies.add(new Notification (a.name, ChatColor.GOLD + "Deine Bewerbung hat ausreichend Stimmen mit positivem Ergebnis. Du bist nun eine Stufe hoeher :)", a.id));                                                                                                
                        }
                }
                notifyPlayers();
                save();
        }
        
        public void notifyPlayers () {
                boolean permsError = false;
                Notification afterNot = null;
                Notification afterNot2 = null;
                for ( Notification n : notifies ) {                        
                        for ( Player p : getServer().getOnlinePlayers() )
                                if ( n.receiver.equalsIgnoreCase("#all") && p.hasPermission("applications.vote") && !p.getName().equalsIgnoreCase(n.sender) )                                
                                     p.sendMessage(n.message);                                  
                                else if ( n.receiver.equalsIgnoreCase("#support") && p.hasPermission("applications.support") )
                                        p.sendMessage(n.message);
                                else if ( p.getName().equalsIgnoreCase(n.receiver) && !n.receiver.equalsIgnoreCase("#all") && !n.receiver.equalsIgnoreCase("#support") ) {
                                        MLog.d ("value = " + n.value);
                                        if ( n.value != -1 ) {
                                                if ( setupPermissions() ) {
                                                        for ( String g : promotions.keySet() ) {                                                            
                                                            boolean done = false;
                                                            if ( perms.playerInGroup(p, g) ) {                                                                        
                                                                        perms.playerRemoveGroup(p, g);
                                                                        perms.playerAddGroup(p, promotions.get(g));                                                                                                                                        
                                                                        done = true;                                                                        
                                                            }                       
                                                            if( done ) break;
                                                        }
                                                } else { permsError = true; } 
                                                afterNot = new Notification ("#support", ChatColor.GRAY + "Der Benutzer " + p.getName() + " hat von der Aktzeptierung seiner Bewerbung erfahren. Bitte schließen mit " + ChatColor.AQUA + "/apply close " + n.value);                                                
                                                n.value = -1;
                                        }
                                        p.sendMessage (n.message);         
                                        if ( permsError ) {
                                                afterNot2 = new Notification ("#support", ChatColor.RED + "Ein Spieler kann nicht autom. befördert werden: " + ChatColor.GRAY + p.getName());
                                                p.sendMessage (" ");
                                                p.sendMessage (ChatColor.RED + "Leider konntest du nicht automatisch befördert werden. Das Team wurde bereits beanchrichtigt. Wenn du keine Antwort erhälst, frage bitte nach. Danke.");
                                        }
                                }                        
                }
                notifies.clear();
                if ( afterNot != null ) {
                        notifies.add(afterNot);
                        if ( afterNot2 != null )
                                notifies.add(afterNot2);
                        notifyPlayers();
                }
                save();
        }
        public boolean isInteger( String input ) { try { Integer.parseInt( input ); return true; } catch( NumberFormatException ex ) { return false; } }

        public boolean np (Player p) {
                p.sendMessage (ChatColor.GRAY + "Zugriff " + ChatColor.RED + "VERWEIGERT" + ChatColor.GRAY + "!"); return true;
        }
        
        public void free (Player p) {
                setupPermissions();
                if ( perms != null ) {
                        boolean promoted = false;
                        for ( String g : promotions.keySet() ) {
                                if ( perms.playerInGroup(p, g) ) {
                                        if (g.equalsIgnoreCase("default") ) {
                                                perms.playerRemoveGroup(p, g);
                                                perms.playerAddGroup(p, promotions.get(g));
                                                perms.playerAddGroup(p, "crnull");      // Standard creative group
                                                promoted = true;
                                        }
                                        else {
                                                notifyPlayers();
                                                return;
                                        }                                                
                                }                                                                
                        }
                        if ( !promoted ) {
                                notifies.add(new Notification("#support", ChatColor.RED + "Ein Spieler kann sich nicht freischalten: Promtion for default group not defined (Applications)"));
                                p.sendMessage(ChatColor.RED + "Ein technischer Fehler ist aufgetreten. Die Freischaltung funktioniert nicht.");
                                p.sendMessage(ChatColor.RED + ":: Die Administratoren wurden benachrichtigt. Vielen Dank für Dein Verständnis.");                        
                        }
                        else {
                                p.sendMessage(ChatColor.GRAY + "=== " + ChatColor.GOLD + "Du wurdest freigeschaltet. Willkommen auf dem Server." + ChatColor.GRAY + " ===");                                
                        }
                } else {
                        notifies.add(new Notification("#support", ChatColor.RED + "Ein Spieler kann sich nicht freischalten: perms == null."));
                        p.sendMessage(ChatColor.RED + "Ein technischer Fehler ist aufgetreten. Die Freischaltung funktioniert nicht.");
                        p.sendMessage(ChatColor.RED + ":: Die Administratoren wurden benachrichtigt. Vielen Dank für Dein Verständnis.");                        
                }
                notifyPlayers();
        }

}
