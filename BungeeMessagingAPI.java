package be.pyrrh4.lib.util;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.messaging.PluginMessageListener;

import com.google.common.io.ByteArrayDataInput;
import com.google.common.io.ByteArrayDataOutput;
import com.google.common.io.ByteStreams;

import be.pyrrh4.arsolia.core.bukkit.Core;

public class BungeeMessagingAPI
{
	// ------------------------------------------------------------
	// Static fields and methods
	// ------------------------------------------------------------

	private static Plugin pluginInstance;
	private static ArrayList<BungeeMessagingListener> listeners = new ArrayList<BungeeMessagingListener>();

	public static void registerListener(BungeeMessagingListener listener)
	{
		listeners.add(listener);
		Bukkit.getMessenger().registerIncomingPluginChannel(pluginInstance, "BungeeCord", listener);
	}

	public static void initialize(Plugin plugin) {
		pluginInstance = plugin;
	}

	public static void terminate() {
		Bukkit.getMessenger().unregisterIncomingPluginChannel(pluginInstance, "BungeeCord");
		Bukkit.getMessenger().registerOutgoingPluginChannel(pluginInstance, "BungeeCord");
		pluginInstance = null;
		listeners.clear();
	}

	// ------------------------------------------------------------
	// BungeeMessagingListener class
	// ------------------------------------------------------------

	public static abstract class BungeeMessagingListener implements PluginMessageListener
	{
		// Override : on receive

		@Override
		public void onPluginMessageReceived(String channel, Player player, byte[] message)
		{
			// Bungee channel
			if (!channel.equals("BungeeCord")) return;

			// Subchannel
			ByteArrayDataInput in = ByteStreams.newDataInput(message);
			String subchannel = in.readUTF();

			// Parse message
			if (subchannel.equals("Forward")) {
				onForwardMessage(new BungeeInForwardMessage(message));
			} else {
				onBungeeMessage(new BungeeInMessage(message));
			}
		}

		// Abstract

		public abstract void onBungeeMessage(BungeeInMessage message);
		public abstract void onForwardMessage(BungeeInForwardMessage message);
	}

	// ------------------------------------------------------------
	// BungeeOutMessage class
	// ------------------------------------------------------------

	public static class BungeeOutMessage
	{
		// Fields and constructor

		private byte[] out;

		public BungeeOutMessage(String subchannel, Object... args)
		{
			ByteArrayDataOutput out = ByteStreams.newDataOutput();
			out.writeUTF(subchannel);

			for (Object arg : args) {
				if (arg instanceof String) {
					out.writeUTF((String) arg);
				} else if (arg instanceof Integer) {
					out.writeInt((int) arg);
				}
			}

			this.out = out.toByteArray();
		}

		// Methods

		public void send() {
			Player sender = Bukkit.getOnlinePlayers().size() > 0 ? Bukkit.getOnlinePlayers().iterator().next() : null;
			if (sender == null) return;// can't send because there is nobody, ur server is nuts
			send(sender);
		}

		public void send(Player player) {
			player.sendPluginMessage(Core.instance(), "BungeeCord", out);
		}
	}

	// ------------------------------------------------------------
	// BungeeOutForwardMessage class
	// ------------------------------------------------------------

	public static class BungeeOutForwardMessage
	{
		// Fields and constructor

		private byte[] out;

		public BungeeOutForwardMessage(String customSubchannel, Object... args)
		{
			ByteArrayDataOutput out = ByteStreams.newDataOutput();
			out.writeUTF("Forward");
			out.writeUTF("ONLINE");
			out.writeUTF(customSubchannel);

			ByteArrayOutputStream msgbytes = new ByteArrayOutputStream();
			DataOutputStream msgout = new DataOutputStream(msgbytes);

			for (Object arg : args)
			{
				try {
					if (arg instanceof String) {
						msgout.writeUTF((String) arg);
					} else if (arg instanceof Integer) {
						msgout.writeInt((int) arg);
					}
				}
				catch (IOException e) {
					e.printStackTrace();
				}
			}

			out.writeShort(msgbytes.toByteArray().length);
			out.write(msgbytes.toByteArray());

			this.out = out.toByteArray();
		}

		// Methods

		public void send() {
			Player sender = Bukkit.getOnlinePlayers().size() > 0 ? Bukkit.getOnlinePlayers().iterator().next() : null;
			if (sender == null) return;// can't send because there is nobody, ur server is nuts
			send(sender);
		}

		public void send(Player player) {
			player.sendPluginMessage(Core.instance(), "BungeeCord", out);
		}
	}

	// ------------------------------------------------------------
	// BungeeInForwardMessage class
	// ------------------------------------------------------------

	public static class BungeeInForwardMessage
	{
		// Fields and constructor

		private String customSubchannel;
		private ArrayList<Object> args = new ArrayList<Object>();

		@SuppressWarnings("deprecation")
		public BungeeInForwardMessage(byte[] message)
		{
			ByteArrayDataInput in = ByteStreams.newDataInput(message);
			this.customSubchannel = in.readUTF();
			short len = in.readShort();
			byte[] msgbytes = new byte[len];
			in.readFully(msgbytes);
			DataInputStream msgin = new DataInputStream(new ByteArrayInputStream(msgbytes));

			try
			{
				String line = null;
				while ((line = msgin.readLine()) != null)
				{
					try {
						args.add(Integer.parseInt(line));
					} catch (NumberFormatException exception) {
						args.add(line);
					}
				}
			} catch (IOException exception) {
				exception.printStackTrace();
			}
		}

		// Methods

		public String getCustomSubchannel() {
			return customSubchannel;
		}

		public List<String> getArgList(int arg, String splitChar) {
			return Arrays.asList(((String) args.get(arg)).split(splitChar));
		}

		public String getArgString(int arg) {
			return (String) args.get(arg);
		}

		public int getArgInt(int arg) {
			return (int) args.get(arg);
		}
	}

	// ------------------------------------------------------------
	// BungeeInMessage class
	// ------------------------------------------------------------

	public static class BungeeInMessage
	{
		// Fields and constructor

		private String subchannel;
		private ArrayList<Object> args = new ArrayList<Object>();

		public BungeeInMessage(byte[] message)
		{
			ByteArrayDataInput in = ByteStreams.newDataInput(message);
			this.subchannel = in.readUTF();

			String line = null;
			while ((line = in.readLine()) != null)
			{
				try {
					args.add(Integer.parseInt(line));
				} catch (NumberFormatException exception) {
					args.add(line);
				}
			}
		}

		// Methods

		public String getSubchannel() {
			return subchannel;
		}

		public List<String> getArgList(int arg, String splitChar) {
			return Arrays.asList(((String) args.get(arg)).split(splitChar));
		}

		public String getArgString(int arg) {
			return (String) args.get(arg);
		}

		public int getArgInt(int arg) {
			return (int) args.get(arg);
		}
	}
}
