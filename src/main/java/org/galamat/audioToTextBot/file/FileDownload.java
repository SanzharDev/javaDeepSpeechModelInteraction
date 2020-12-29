package org.galamat.audioToTextBot.file;


import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.asynchttpclient.*;
import org.asynchttpclient.util.HttpConstants;
import org.json.JSONObject;
import ws.schild.jave.Encoder;
import ws.schild.jave.MultimediaObject;
import ws.schild.jave.encode.AudioAttributes;
import ws.schild.jave.encode.EncodingAttributes;

import javax.sound.sampled.*;
import java.io.*;
import java.net.URL;
import java.net.URLConnection;
import java.util.Random;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

public class FileDownload {

    private static final String DIRECTORY_NAME = "C:\\Users\\User\\Desktop\\gogo\\audioToTextBot\\audioToTextBot\\src\\main\\resources\\voices\\";

    private static final Logger logger = LogManager.getLogger("AudioToTextBot");

    private static final AsyncHttpClient client = Dsl.asyncHttpClient();

    private static final String characters = "123456890qwertyuiopasdfghjklzxcvbnm_";

    public static String downloadVoiceMessage(String botToken, String fileId) throws ExecutionException, InterruptedException, IOException {

        String responseBody = getFilePath(botToken, fileId);

        JSONObject jsonObject = new JSONObject(responseBody);
        String filePath = String.valueOf(jsonObject.getJSONObject("result").get("file_path"));

        String downloadVoiceUrl = String.format("https://api.telegram.org/file/bot%s/%s", botToken, filePath);
        logger.info(String.format("Downloading voice file from: %s", downloadVoiceUrl));

        String fileName = generateFileName();
        String oggFilePath = String.format("%s%s.ogg", DIRECTORY_NAME, fileName);

        try {
            URLConnection conn = new URL(downloadVoiceUrl).openConnection();
            InputStream is = conn.getInputStream();

            OutputStream outStream = new FileOutputStream(oggFilePath);
            byte[] buffer = new byte[4096];
            int len;
            while ((len = is.read(buffer)) > 0) {
                outStream.write(buffer, 0, len);
            }
            outStream.close();
            return oggFilePath;
        } catch (IOException e) {
            e.printStackTrace();
            logger.error(String.format("Cannot convert %s OGG to WAV", fileId));
            return "";
        }
    }

    private static String getFilePath(String botToken, String fileId) throws ExecutionException, InterruptedException {
        Request getFilePath = new RequestBuilder(HttpConstants.Methods.GET)
                .setUrl(String.format("https://api.telegram.org/bot%s/getFile?file_id=%s", botToken, fileId))
                .build();
        Future<Response> responseFuture = client.executeRequest(getFilePath);
        Response response = responseFuture.get();
        return response.getResponseBody();
    }

    private static String generateFileName() {
        Random random = new Random(System.currentTimeMillis());
        StringBuilder buffer = new StringBuilder().append("voice_");
        for(int i = 0; i < 8; i++) {
            buffer.append(characters.charAt(random.nextInt(characters.length())));
        }
        return buffer.toString();
    }

    private static void convertOggToWav(String sourceOggFile, String fileName) throws IOException, UnsupportedAudioFileException {
        try {
            File source = new File(sourceOggFile);
            File target = new File(String.format("%s%s.wav", DIRECTORY_NAME, fileName));
            //Audio Attributes
            AudioAttributes audio = new AudioAttributes();
            audio.setCodec("libmp3lame");
            audio.setBitRate(256000);
            audio.setChannels(1);
            audio.setSamplingRate(16000);

            //Encoding attributes
            EncodingAttributes attrs = new EncodingAttributes();
            attrs.setAudioAttributes(audio);
            attrs.setInputFormat("ogg");
            attrs.setOutputFormat("wav");

            //Encode
            Encoder encoder = new Encoder();
            encoder.encode(new MultimediaObject(source), target, attrs);
        } catch (Exception e) {
            e.printStackTrace();
            logger.error(String.format("Cannot convert %s OGG to WAV", fileName));
        }
    }

}
