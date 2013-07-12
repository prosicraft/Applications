/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.prosicraft.applications;

import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerJoinEvent;

/**
 *
 * @author prosicraft
 */
public class PlayerListener implements Listener {
        
        public Applications app = null;
        
        public PlayerListener (Applications app) {
                this.app = app;
        }
        
        @EventHandler(priority=EventPriority.HIGHEST)
        public void onPlayerJoin (PlayerJoinEvent e) {
                app.notifyPlayers();
        }                
        
        @EventHandler(priority=EventPriority.LOWEST)
        public void onPlayerCommand (PlayerCommandPreprocessEvent e) {
                if ( e.isCancelled() ) return;
                if ( e.getMessage().substring(1).equalsIgnoreCase(this.app.freePassword) ) {
                        app.free(e.getPlayer());
                }                
        }
        
}
