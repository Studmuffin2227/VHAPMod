package com.example.vhapmod.network;

import com.example.vhapmod.APSkillLockManager;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.HashSet;
import java.util.Set;
import java.util.function.Supplier;

/**
 * Syncs AP unlocks from server to client for GUI greying out
 */
public class APUnlockSyncMessage {
    
    private Set<String> unlockedSkills;
    private Set<String> unlockedTalents;
    private Set<String> unlockedMods;
    private Set<String> unlockedExpertises;
    
    public APUnlockSyncMessage(Set<String> skills, Set<String> talents, Set<String> mods, Set<String> expertises) {
        this.unlockedSkills = skills;
        this.unlockedTalents = talents;
        this.unlockedMods = mods;
        this.unlockedExpertises = expertises;
    }
    
    public static void encode(APUnlockSyncMessage message, FriendlyByteBuf buffer) {
        // Write skills
        buffer.writeInt(message.unlockedSkills.size());
        for (String skill : message.unlockedSkills) {
            buffer.writeUtf(skill);
        }
        
        // Write talents
        buffer.writeInt(message.unlockedTalents.size());
        for (String talent : message.unlockedTalents) {
            buffer.writeUtf(talent);
        }
        
        // Write mods
        buffer.writeInt(message.unlockedMods.size());
        for (String mod : message.unlockedMods) {
            buffer.writeUtf(mod);
        }
        
        // Write expertises
        buffer.writeInt(message.unlockedExpertises.size());
        for (String expertise : message.unlockedExpertises) {
            buffer.writeUtf(expertise);
        }
    }
    
    public static APUnlockSyncMessage decode(FriendlyByteBuf buffer) {
        // Read skills
        int skillCount = buffer.readInt();
        Set<String> skills = new HashSet<>();
        for (int i = 0; i < skillCount; i++) {
            skills.add(buffer.readUtf());
        }
        
        // Read talents
        int talentCount = buffer.readInt();
        Set<String> talents = new HashSet<>();
        for (int i = 0; i < talentCount; i++) {
            talents.add(buffer.readUtf());
        }
        
        // Read mods
        int modCount = buffer.readInt();
        Set<String> mods = new HashSet<>();
        for (int i = 0; i < modCount; i++) {
            mods.add(buffer.readUtf());
        }
        
        // Read expertises
        int expertiseCount = buffer.readInt();
        Set<String> expertises = new HashSet<>();
        for (int i = 0; i < expertiseCount; i++) {
            expertises.add(buffer.readUtf());
        }
        
        return new APUnlockSyncMessage(skills, talents, mods, expertises);
    }
    
    public static void handle(APUnlockSyncMessage message, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> {
            // Update client-side cache
            APSkillLockManager.setClientUnlockedSkills(message.unlockedSkills);
            APSkillLockManager.setClientUnlockedTalents(message.unlockedTalents);
            APSkillLockManager.setClientUnlockedMods(message.unlockedMods);
            APSkillLockManager.setClientUnlockedExpertises(message.unlockedExpertises);
        });
        context.setPacketHandled(true);
    }
}
