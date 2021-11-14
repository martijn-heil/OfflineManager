/*
 * Copyright (c) 2021 flaweoff
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package net.flawe.offlinemanager.api.util.v1_16_R3.data;

import com.mojang.authlib.GameProfile;
import net.flawe.offlinemanager.api.OfflineManagerAPI;
import net.flawe.offlinemanager.api.data.entity.AbstractPlayerData;
import net.flawe.offlinemanager.api.entity.IUser;
import net.flawe.offlinemanager.api.enums.SavePlayerType;
import net.flawe.offlinemanager.api.event.data.LoadPlayerEvent;
import net.flawe.offlinemanager.api.event.data.SavePlayerEvent;
import net.flawe.offlinemanager.api.inventory.AbstractPlayerInventory;
import net.flawe.offlinemanager.api.inventory.IArmorInventory;
import net.flawe.offlinemanager.api.inventory.IEnderChest;
import net.flawe.offlinemanager.api.util.v1_16_R3.inventory.ArmorInventory;
import net.flawe.offlinemanager.api.util.v1_16_R3.inventory.OfflineEnderChest;
import net.minecraft.server.v1_16_R3.*;
import org.bukkit.Bukkit;
import org.bukkit.craftbukkit.v1_16_R3.CraftServer;
import org.bukkit.craftbukkit.v1_16_R3.entity.CraftPlayer;
import org.bukkit.craftbukkit.v1_16_R3.inventory.CraftInventoryPlayer;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.plugin.Plugin;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.UUID;

public class PlayerData extends AbstractPlayerData {

    private final OfflineManagerAPI api;
    private final UUID uuid;
    private final String name;
    private WorldNBTStorage worldNBTStorage;
    private NBTTagCompound tag;
    private final File playerDir;
    private net.flawe.offlinemanager.api.util.v1_16_R3.inventory.PlayerInventory playerInventory;
    private ArmorInventory armorInventory;
    private IEnderChest enderChest;

    public PlayerData(String name, OfflineManagerAPI api) {
        //noinspection deprecation
        this(Bukkit.getOfflinePlayer(name).getUniqueId(), api);
    }

    public PlayerData(UUID uuid, OfflineManagerAPI api) {
        this(uuid, new TagAdapter(((CraftServer) Bukkit.getServer()).getHandle().playerFileData.getPlayerData(uuid.toString())), api);
    }

    public PlayerData(UUID uuid, TagAdapter compound, OfflineManagerAPI api) {
        super(uuid, compound);
        this.api = api;
        this.uuid = uuid;
        MinecraftServer server = ((CraftServer) Bukkit.getServer()).getServer();
        WorldServer worldServer = server.getWorldServer(net.minecraft.server.v1_16_R3.World.OVERWORLD);
        if (worldServer == null)
            throw new NullPointerException("Overworld cannot be null!");
        GameProfile profile = new GameProfile(uuid, Bukkit.getOfflinePlayer(uuid).getName());
        EntityPlayer entityPlayer = new EntityPlayer(server, worldServer, profile, new PlayerInteractManager(worldServer));
        entityPlayer.load(compound.getTag());
        entityPlayer.loadData(compound.getTag());
        this.name = entityPlayer.getName();
        this.worldNBTStorage = server.worldNBTStorage;
        this.tag = compound.getTag();
        this.playerDir = worldNBTStorage.getPlayerDir();
        NBTTagList inventoryList = (NBTTagList) tag.get("Inventory");
        net.minecraft.server.v1_16_R3.PlayerInventory virtual = new net.minecraft.server.v1_16_R3.PlayerInventory(entityPlayer);
        virtual.b(inventoryList);
        this.playerInventory = new net.flawe.offlinemanager.api.util.v1_16_R3.inventory.PlayerInventory(new CraftInventoryPlayer(virtual), tag);
        this.armorInventory = new ArmorInventory(this);
        this.enderChest = new OfflineEnderChest(Bukkit.createInventory(null, InventoryType.ENDER_CHEST), tag);
        LoadPlayerEvent event = new LoadPlayerEvent(this);
        Bukkit.getPluginManager().callEvent(event);
    }

    @Override
    public AbstractPlayerInventory getInventory() {
        return playerInventory;
    }

    @Override
    public IArmorInventory getArmorInventory() {
        return armorInventory;
    }

    @Override
    public IEnderChest getEnderChest() {
        return enderChest;
    }

    @Override
    public void setOnline(boolean b) {
        Player player = Bukkit.getPlayer(uuid);
        if (b) {
            if (player == null) return;
            EntityPlayer e = ((CraftPlayer) player).getHandle();
            e.load(tag);
            e.loadData(tag);
            return;
        }
        MinecraftServer server = ((CraftServer) Bukkit.getServer()).getServer();
        WorldServer worldServer = server.getWorldServer(World.OVERWORLD);
        if (worldServer == null)
            throw new NullPointerException("Overworld cannot be null!");
        GameProfile profile = new GameProfile(uuid, Bukkit.getOfflinePlayer(uuid).getName());
        EntityPlayer ep;
        worldNBTStorage = ((CraftServer) Bukkit.getServer()).getServer().worldNBTStorage;
        adapter = new TagAdapter(worldNBTStorage.getPlayerData(uuid.toString()));
        tag = ((TagAdapter) adapter).getTag();
        if (player != null) {
            ep = ((CraftPlayer) player).getHandle();
            adapter = new TagAdapter(ep.save(tag));
            ep.saveData(tag);
        }
        ep = new EntityPlayer(server, worldServer, profile, new PlayerInteractManager(worldServer));
        ep.load(tag);
        ep.loadData(tag);
        NBTTagList inventoryList = (NBTTagList) tag.get("Inventory");
        PlayerInventory virtual = new PlayerInventory(ep);
        virtual.b(inventoryList);
        this.playerInventory = new net.flawe.offlinemanager.api.util.v1_16_R3.inventory.PlayerInventory(new CraftInventoryPlayer(virtual), tag);
        this.armorInventory = new ArmorInventory(this);
        this.enderChest = new OfflineEnderChest(Bukkit.createInventory(null, InventoryType.ENDER_CHEST), tag);
    }

    @Override
    public void save() {
        save(SavePlayerType.DEFAULT);
    }

    @Override
    public void save(SavePlayerType type) {
        SavePlayerEvent event = new SavePlayerEvent(this, type);
        Bukkit.getPluginManager().callEvent(event);
        try {
            File file = new File(this.playerDir, uuid + ".dat.tmp");
            File file1 = new File(this.playerDir, uuid + ".dat");
            NBTCompressedStreamTools.a(tag, new FileOutputStream(file));
            if (file1.exists())
                if (!file1.delete()) throw new IOException(String.format("Failed to delete tmp player file %s", uuid.toString()));
            if (!file.renameTo(file1)) throw new IOException(String.format("Failed to rename player file %s", uuid.toString()));
        } catch (Exception e) {
            ((Plugin) api).getLogger().warning("Failed to save player data for " + name);
            e.printStackTrace();
        }
    }

    @Override
    public IUser getUser() {
        return new OfflineUser((Plugin) api, uuid, tag);
    }

}
