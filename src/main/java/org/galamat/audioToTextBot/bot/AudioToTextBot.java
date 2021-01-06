package org.galamat.audioToTextBot.bot;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.mime.HttpMultipartMode;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.entity.mime.content.FileBody;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.util.EntityUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.galamat.audioToTextBot.file.FileDownload;
import org.json.JSONObject;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.Voice;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.io.File;
import java.io.IOException;
import java.util.Date;
import java.util.concurrent.ExecutionException;

public class AudioToTextBot extends TelegramLongPollingBot {

    private final Logger logger = LogManager.getLogger("AudioToTextBot");

    private final String BOT_TOKEN = "1426755669:AAGQFIvGE6dby9z7SB1jzNeoR0UjnAxoi2I";

    @Override
    public String getBotToken() {
        return BOT_TOKEN;
    }

    @Override
    public void onUpdateReceived(Update update) {
        String messageText = "Отправьте пожалуйста аудиосообщения на казахском языке";

        long startMs = System.currentTimeMillis();
        if (update.hasMessage() && update.getMessage().hasVoice()) {
            logger.info(String.format(">>>>>>>>>>> Time start: %s", new Date(System.currentTimeMillis())));
            messageText = onVoiceMessageAccepted(update);
            logger.info(String.format("Message received %s", messageText));
        }

        SendMessage message = new SendMessage();
        message.setChatId(String.valueOf(update.getMessage().getChatId()));
        message.setText(messageText);

        try {
            execute(message);
            logger.info(String.format(">>>>>>>>>>> Time finish: %s", new Date(System.currentTimeMillis())));
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }

    @Override
    public String getBotUsername() {
        return "audioToText";
    }

    private String onVoiceMessageAccepted(Update update) {
        logger.info("Accepted voice message");
        Voice voice = update.getMessage().getVoice();
        logger.info(String.format("Voice message duration: %s", voice.getDuration()));
        logger.info(String.format("Voice file id: %s", voice.getFileId()));

        try {
            String oggAudioPath = FileDownload.downloadVoiceMessage(BOT_TOKEN, voice.getFileId());
            if (oggAudioPath.length() == 0) {
                return "Cannot convert your voice message to text";
            }
            return requestDsModel(oggAudioPath);
        } catch (ExecutionException | InterruptedException | IOException e ) {
            e.printStackTrace();
        }
        return "Cannot convert your voice message to text";
    }

    private String requestDsModel(String oggFilePath) throws IOException {
        File oggFile = new File(oggFilePath);
        HttpPost post = new HttpPost("http://192.168.88.224:8000/api/v1/stt");

        FileBody fileBody = new FileBody(oggFile, ContentType.DEFAULT_BINARY);
        MultipartEntityBuilder builder = MultipartEntityBuilder.create();
        builder.setMode(HttpMultipartMode.BROWSER_COMPATIBLE);
        builder.addPart("speech", fileBody);
        HttpEntity entity = builder.build();
        post.setEntity(entity);

        HttpClient client = HttpClientBuilder.create().build();
        HttpResponse response = client.execute(post);
        String responseMessage = EntityUtils.toString(response.getEntity());

        JSONObject jsonObject = new JSONObject(responseMessage);
        return String.valueOf(jsonObject.get("text"));
    }

}
