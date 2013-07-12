/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.prosicraft.applications;

/**
 *
 * @author prosicraft
 */
public class Notification {
        
        public String receiver = "";
        public String sender = "";
        public String message  = "";
        public int value = -1;
        
        
        public Notification (String rec, String msg) {
                receiver = rec;
                message = msg;
        }
        
        public Notification (String rec, String msg, int val) {
                receiver = rec;
                message = msg;
                value = val;
        }
        
        public Notification (String rec, String msg, int val, String sen) {
                receiver = rec;
                message = msg;
                value = val;
                sender = sen;
        }
}
