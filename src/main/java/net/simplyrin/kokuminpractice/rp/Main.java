package net.simplyrin.kokuminpractice.rp;

import java.time.OffsetDateTime;

import com.connorlinfoot.discordrp.DiscordRP;
import com.jagrosh.discordipc.IPCClient;
import com.jagrosh.discordipc.entities.DiscordBuild;
import com.jagrosh.discordipc.entities.RichPresence;
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
@Mod(modid = "KokuminPracitceRP", version = "1.2")
public class Main {

	private IPCClient ipcClient;

	private IPCClient _default;
	private DiscordBuild discordBuild;

	private boolean isKokumin;
	private OffsetDateTime offsetDateTime;

	private String opponent;

	@EventHandler
	public void init(FMLInitializationEvent event) {
		MinecraftForge.EVENT_BUS.register(this);
	}

	@SubscribeEvent
	public void onConnect(FMLNetworkEvent.ClientConnectedToServerEvent event) {
		String address = event.manager.getRemoteAddress().toString().toLowerCase();
		String serverIP = Minecraft.getMinecraft().getCurrentServerData().serverIP;

		if (address.contains("kokum.info.tm") || serverIP.contains("kokumin.space") || serverIP.contains("kokumin.work")) {
			this.isKokumin = true;

			try {
				this._default = DiscordRP.getInstance().getIpcClient();
				this.discordBuild = this._default.getDiscordBuild();
				this._default.close();
			} catch (Exception e) {
			}

			this.lobby(true);
		}
	}

	@SubscribeEvent
	public void onDisconnect(FMLNetworkEvent.ClientDisconnectionFromServerEvent event) {
		this.offsetDateTime = null;
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
		String message = ChatColor.stripColor(event.message.getFormattedText());
		String[] args = message.split(" ");

		if (args.length > 0) {
			if (message.startsWith("[Match]") && (message.endsWith("があなたからのDuelを受けました！") || message.endsWith(" からのDuelを受け付けました！"))) {
				String opponent = args[1];
				this.connect("Opponent: " + opponent, null);
				return;
			}

			if (message.equals("[Match] Match has ended!")) {
				this.lobby(false);
				return;
			}

			if (message.equals("[Match] キューに入りました。対戦相手が見つかるまでしばらくお待ちください...")) {
				this.connect("Searching an opponent...", null);
				return;
			}

			if (message.equals("[Match] キューから抜けました")) {
				this.lobby(false);
			}

			if (message.contains("[1vs1] 対戦相手が見つかりました！")) {
				String opponent = message.replace("[1vs1]", "");
				opponent = opponent.replace("対戦相手が見つかりました！", "");
				opponent = opponent.replace("相手:", "");
				this.opponent = opponent.trim();
			}

			if (message.equals("[Match] The match is starting in 10 seconds...")) {
				// Detect opponent from scoreboard
				ScoreObjective scoreObjective = Minecraft.getMinecraft().theWorld.getScoreboard().getObjectiveInDisplaySlot(1);

				Scoreboard scoreboard = scoreObjective.getScoreboard();
				if (scoreboard != null) {
					String detectedName = "#unknown";
					boolean nextIsName = false;

					for (Score score : scoreboard.getSortedScores(scoreObjective)) {
						String line = ChatColor.stripColor(ScorePlayerTeam.formatPlayerName(scoreboard.getPlayersTeam(score.getPlayerName()), score.getPlayerName())).trim();

						if (nextIsName) {
							nextIsName = false;
							if (!line.equals(this.getName()) && !line.equals("")) {
								detectedName = line;
							}
						}

						if (line.startsWith("Team ") && line.endsWith(":")) {
							nextIsName = true;
						}
					}

					if (detectedName != null) {
						this.connect("Opponent: " + this.opponent, null);
					}
				}
			}
		}

	}

	public void lobby(boolean multithreading) {
		ThreadPool.run(() -> {
			try {
				Thread.sleep(5000);
			} catch (Exception e) {
			}

			Minecraft.getMinecraft().addScheduledTask(() -> {
				String coins = "";
				String kills = "";

				ScoreObjective scoreObjective = Minecraft.getMinecraft().theWorld.getScoreboard().getObjectiveInDisplaySlot(1);

				Scoreboard scoreboard = scoreObjective.getScoreboard();
				if (scoreboard != null) {
					for (Score score : scoreboard.getSortedScores(scoreObjective)) {
						String line = ChatColor.stripColor(ScorePlayerTeam.formatPlayerName(scoreboard.getPlayersTeam(score.getPlayerName()), score.getPlayerName()));

						if (line.startsWith("Coins: ")) {
							coins = line.replace("Coins: ", "");
						}

						if (line.startsWith("Kills: ")) {
							kills = line.replace("Kills: ", "");
						}
					}
				}

				this.connect("Coins: " + coins, "Kills: " + kills);
			});
		});
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
			if (state != null) {
				presence.setState(state);
			}
			if (this.offsetDateTime == null) {
				this.offsetDateTime = OffsetDateTime.now();
			}
			presence.setStartTimestamp(this.offsetDateTime);
			presence.setLargeImage("kokumin", "kokumin.space");
			this.ipcClient.sendRichPresence(presence.build());
		});
	}

	public void disconnect() {
		if (this.ipcClient != null) {
			this.ipcClient.close();
			this.ipcClient = null;
		}
	}

	public String getName() {
		return Minecraft.getMinecraft().thePlayer.getName();
	}

}
