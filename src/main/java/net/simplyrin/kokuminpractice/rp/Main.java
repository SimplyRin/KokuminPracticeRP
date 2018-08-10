package net.simplyrin.kokuminpractice.rp;

import java.time.OffsetDateTime;

import com.connorlinfoot.discordrp.DiscordRP;
import com.jagrosh.discordipc.IPCClient;
import com.jagrosh.discordipc.entities.DiscordBuild;
import com.jagrosh.discordipc.entities.RichPresence;
import com.jagrosh.discordipc.exceptions.NoDiscordClientException;

import net.minecraft.client.Minecraft;
import net.minecraftforge.client.event.ClientChatReceivedEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.Mod.EventHandler;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.network.FMLNetworkEvent;
import net.simplyrin.kokuminpractice.rp.utils.ChatColor;
import net.simplyrin.kokuminpractice.rp.utils.ThreadPool;

/**
 * Created by SimplyRin on 2018/08/10.
 *
 * Copyright (c) 2018 SimplyRin
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
@Mod(modid = "KokuminPracticeRP", version = "1.0")
public class Main {

	private IPCClient ipcClient;

	private boolean isKokumin;

	private OffsetDateTime offsetDateTime;
	private String opponent;
	private String game;

	@EventHandler
	public void init(FMLInitializationEvent event) {
		MinecraftForge.EVENT_BUS.register(this);
	}

	@SubscribeEvent
	public void onConnect(FMLNetworkEvent.ClientConnectedToServerEvent event) {
		String address = event.manager.getRemoteAddress().toString().toLowerCase();
		String serverIP = Minecraft.getMinecraft().getCurrentServerData().serverIP;

		if(address.contains("kokum.info.tm") || serverIP.contains("kokumin.space")) {
			this.isKokumin = true;

			try {
				IPCClient ipcClient = DiscordRP.getInstance().getIpcClient();
				ipcClient.close();
			} catch (Exception e) {
			}
		}
	}

	@SubscribeEvent
	public void onDisconnect(FMLNetworkEvent.ClientDisconnectionFromServerEvent event) {
		this.offsetDateTime = null;
		this.disconnect();

		if(this.isKokumin) {
			this.isKokumin = false;

			DiscordRP.getInstance().updateDiscord();
		}
	}

	@SubscribeEvent
	public void ClientChatReceived(ClientChatReceivedEvent event) {
		String message = ChatColor.stripColor(event.message.getFormattedText());
		String[] args = message.split(" ");

		if(args.length > 0) {
			if(message.endsWith("has accepted your duel !") || message.endsWith("からのduelを申し受けました！")) {
				String opponent = args[0];
				this.opponent = opponent;
			}

			if(message.startsWith("Starting duel against " + this.opponent + " Game: ")) {
				String game = message.split("Starting duel against " + this.opponent + " Game: ")[1];
				this.game = game;
				this.connect("Opponent: " + this.opponent, "Game: " + this.game);
			}

			if(message.equals("Match has ended !") || message.equals("試合が終了しました！")) {
				this.connect("Lobby", null);
			}

			if(message.startsWith("You queued on ") || message.endsWith(". Searching an opponent...")) {
				String game = args[3];
				if(game.equals("No")) {
					game = "No Debuff";
				}
				this.connect("Searching an opponent...", "Game: " + game.replace(".", ""));
			}

			if(message.endsWith("に参加しました。プレイヤーが見つかりまでお待ち下さい")) {
				String game = message.split("に参加しました。プレイヤーが見つかりまでお待ち下さい")[0];
				this.connect("Searching an opponent...", "Game: " + game);
			}

			if(message.equals("参加をキャンセルしました") || message.equals("You cancelled queue.")) {
				this.connect("Lobby", null);
			}
		}

	}

	public void connect(String detail, String state) {
		this.disconnect();

		ThreadPool.run(() -> {
			this.ipcClient = new IPCClient(477443339822563328L);
			try {
				this.ipcClient.connect(new DiscordBuild[0]);
			} catch (NoDiscordClientException e) {
				System.out.println("You don't have Discord Client!");
				System.exit(0);
				return;
			}
			RichPresence.Builder presence = new RichPresence.Builder();
			presence.setDetails(detail);
			presence.setState(state);
			if(this.offsetDateTime == null) {
				this.offsetDateTime = OffsetDateTime.now();
			}
			presence.setStartTimestamp(this.offsetDateTime);
			presence.setLargeImage("kokumin", "kokumin.space");
			this.ipcClient.sendRichPresence(presence.build());
		});
	}

	public void disconnect() {
		if(this.ipcClient != null) {
			this.ipcClient.close();
			this.ipcClient = null;
		}
	}

}
