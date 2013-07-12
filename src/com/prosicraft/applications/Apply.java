/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.prosicraft.applications;

import java.util.ArrayList;
import java.util.List;
import org.bukkit.Location;
import org.bukkit.entity.Player;

/**
 *
 * @author prosicraft
 */
public class Apply {
        
        public String name = "";
        public int id = -1;
        public double averageVote = 0;        
        public Location location = null;
        public List<String> voters = new ArrayList<>();
        public List<Integer> values = new ArrayList<>();
        public boolean accepted = false;
        
        public Apply (Location location, String name) {
                this.location = location;
                this.name = name;
        }
        
        public Apply (Location location, String name, int id) {
                this.location = location;
                this.name = name;
                this.id = id;
        }
        
        // false = already votet, true = success
        public boolean vote ( int val, Player p )
        {
                if ( voters.contains(p.getName()) ) return false;
                
                values.add(val);
                voters.add(p.getName());
                
                // now calculate average
                int sum = 0;
                for ( int i : values ) sum += i;
                averageVote = sum / values.size();
                
                return true;
        }                
        
}
