package org.galamat.audioToTextBot;

import org.galamat.audioToTextBot.bot.AudioToTextBot;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.telegram.telegrambots.meta.TelegramBotsApi;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.updatesreceivers.DefaultBotSession;

@SpringBootApplication
public class AudioToTextBotApplication {
	public static void main(String[] args) {
		try {
			TelegramBotsApi botsApi = new TelegramBotsApi(DefaultBotSession.class);
			botsApi.registerBot(new AudioToTextBot());
		} catch (TelegramApiException e) {
			e.printStackTrace();
		}
	}
}
