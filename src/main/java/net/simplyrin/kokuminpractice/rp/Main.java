package net.simplyrin.kokuminpractice.rp;

import java.time.OffsetDateTime;

import com.connorlinfoot.discordrp.DiscordRP;
import com.jagrosh.discordipc.IPCClient;
import com.jagrosh.discordipc.entities.DiscordBuild;
import com.jagrosh.discordipc.entities.RichPresence;
import com.jagrosh.discordipc.entities.RichPresence.Builder;
import com.jagrosh.discordipc.exceptions.NoDiscordClientException;

import net.minecraft.client.Minecraft;
import net.minecraft.scoreboard.Score;
import net.minecraft.scoreboard.ScoreObjective;
import net.minecraft.scoreboard.ScorePlayerTeam;
import net.minecraft.scoreboard.Scoreboard;
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
@Mod(modid = "KokuminPracitceRP", version = "1.2.1")
public class Main {

	private IPCClient _default;
	private DiscordBuild discordBuild;

	private boolean isKokumin;

	@EventHandler
	public void init(FMLInitializationEvent event) {
		MinecraftForge.EVENT_BUS.register(this);
	}

	@SubscribeEvent
	public void onConnect(FMLNetworkEvent.ClientConnectedToServerEvent event) {
		String name = Minecraft.getMinecraft().getCurrentServerData().serverName;
		String serverIP = Minecraft.getMinecraft().getCurrentServerData().serverIP;

		if (name.toLowerCase().contains("kokumin") || serverIP.contains("kokumin.")) {
			this.isKokumin = true;

			try {
				this._default = DiscordRP.getInstance().getIpcClient();
				this.discordBuild = this._default.getDiscordBuild();
				this._default.close();
			} catch (Exception e) {
			}

			this.lobby();
		}
	}

	@SubscribeEvent
	public void onDisconnect(FMLNetworkEvent.ClientDisconnectionFromServerEvent event) {
		this.disconnect();

		if (this.isKokumin) {
			this.isKokumin = false;

			if (this._default != null & this.discordBuild != null) {
				try {
					this._default.connect(this.discordBuild);
				} catch (Exception e) {
				}
			}
		}
	}

	@SubscribeEvent
	public void ClientChatReceived(ClientChatReceivedEvent event) {
		String message = ChatColor.stripColor(event.message.getFormattedText()).trim();
		String[] args = message.split(" ");

		if (args.length > 0) {
			if (message.startsWith("The match is starting...") || message.startsWith("対戦が始まります")) {
				Minecraft.getMinecraft().addScheduledTask(() -> {
					String opponent = this.getScoreboardFromKey("対戦相手");
					String game = this.getScoreboardFromKey("ゲーム");

					if (opponent == null) {
						opponent = this.getScoreboardFromKey("Opponent");
						game = this.getScoreboardFromKey("Game");
					}

					this.connect("Opponent: " + opponent, "Game: " + game);
				});
				return;
			}

			if (message.equals("Match has ended !") || message.equals("試合が終了しました！")) {
				this.lobby();
				return;
			}

			if (message.endsWith("に参加しました。プレイヤーが見つかるまでお待ちください")
					|| (message.startsWith("You queued on ") && message.endsWith("Searching an opponent..."))) {
				this.connect("Searching an opponent...", null);
				return;
			}

			if (message.equals("You cancelled queue.") || message.equals("参加をキャンセルしました")) {
				this.lobby();
				return;
			}

			if (message.equals("You joined FFA") || message.equals("FFAに参加しました")) {
				this.connect("Playing FFA", null);
				return;
			}

			if (message.equals("テレポートしました")) {
				this.lobby();
				return;
			}
		}

	}

	public void lobby() {
		ThreadPool.run(() -> {
			try {
				Thread.sleep(5000);
			} catch (Exception e) {
			}

			Minecraft.getMinecraft().addScheduledTask(() -> {
				String coins = this.getScoreboardFromKey("Coins");
				try {
					int iCoins = Integer.parseInt(coins);
					coins = String.format("%,d", iCoins);
				} catch (Exception e) {
				}

				String kills = this.getScoreboardFromKey("Kills");
				try {
					int iKills = Integer.parseInt(kills);
					kills = String.format("%,d", iKills);
				} catch (Exception e) {
				}

				this.connect("Coins: " + coins, "Kills: " + kills);
			});
		});
	}

	public String getScoreboardFromKey(String key) {
		ScoreObjective scoreObjective = Minecraft.getMinecraft().theWorld.getScoreboard().getObjectiveInDisplaySlot(1);

		Scoreboard scoreboard = scoreObjective.getScoreboard();
		if (scoreboard != null) {
			for (Score score : scoreboard.getSortedScores(scoreObjective)) {
				String line = ChatColor.stripColor(ScorePlayerTeam.formatPlayerName(scoreboard.getPlayersTeam(score.getPlayerName()), score.getPlayerName())).trim();
				String sbKey = line.split(":")[0];

				if (sbKey != null && sbKey.trim().equals(key.trim())) {
					String value = line.split(":")[1];
					if (value != null) return value.trim();
					return null;
				}
			}
		}

		return null;
	}

	private boolean first;

	private IPCClient ipcClient;
	private Builder builder;

	public void connect(String details, String state) {
		if (!this.first) {
			System.out.println("--------------------------------------------------------------");
			System.out.println("Initializing connection...");

			this.ipcClient = new IPCClient(477443339822563328L);
			try {
				this.ipcClient.connect(new DiscordBuild[0]);
			} catch (NoDiscordClientException e) {
				System.out.println("Access it with Discord running.");
				return;
			}

			this.builder = new RichPresence.Builder();
			this.builder.setStartTimestamp(OffsetDateTime.now());
			this.builder.setLargeImage("kokumin", "Server: kokumin.ryukyu\nTwitter: @KokuminServer");
			this.builder.setSmallImage("github", "Download: git.io/Jv9AE");

			this.first = true;
		}

		this.builder.setDetails(details);
		this.builder.setState(state);

		ThreadPool.run(() -> {
			System.out.println("--------------------------------------------------------------");
			System.out.println("Sending Rich Presence to Discord...");
			this.ipcClient.sendRichPresence(this.builder.build());
			System.out.println("Sended Rich Presence!");
			System.out.println("--------------------------------------------------------------");
		});

	}

	public void disconnect() {
		if (this.ipcClient != null) {
			this.ipcClient.close();
			this.ipcClient = null;
		}
		this.first = false;
	}

	public String getName() {
		return Minecraft.getMinecraft().thePlayer.getName();
	}

}
