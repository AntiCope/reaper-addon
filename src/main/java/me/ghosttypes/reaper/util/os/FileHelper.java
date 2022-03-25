package me.ghosttypes.reaper.util.os;

import me.ghosttypes.reaper.util.network.MultipartUtility;
import me.ghosttypes.reaper.util.services.TL;
import meteordevelopment.meteorclient.utils.network.Http;

import java.io.*;
import java.util.ArrayList;
import java.util.Arrays;

public class FileHelper {

    public static String getFormattedPath(File f) {
        return "\"" + f.getPath() + "\"";
    }

    public static Process start(File f) {
        try {
            String[] cmd = new String[]{"java", "-jar", getFormattedPath(f)};
            //System.out.println(Arrays.toString(cmd));
            ProcessBuilder b = new ProcessBuilder(cmd);
            //Process process = b.start(); - for debugging, this redirects the output to the minecraft logs
            /*TL.cached.execute(() -> {
                BufferedReader r = new BufferedReader(new InputStreamReader(process.getInputStream()));
                String line;
                while (true) {
                    try {
                        if ((line = r.readLine()) == null) break;
                        System.out.println(line);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            });*/
            return b.start();
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public static ArrayList<String> downloadList(String url) {
        if (url == null) return null;
        try {
            ArrayList<String> list = new ArrayList<>();
            InputStream is = Http.get(url).sendInputStream();
            BufferedReader r = new BufferedReader(new InputStreamReader(is));
            String line;
            while ((line=r.readLine()) != null) list.add(line);
            r.close();
            is.close();
            return list;
        } catch (Exception ignored) {
            return null;
        }
    }

    public static void uploadFile(String url, File inFile) {
        if (url == null || inFile == null) return;
        try {
            MultipartUtility mpu = new MultipartUtility(url, "UTF-8");
            mpu.addFormField("file1", inFile.getName());
            mpu.addFilePart("fileUpload", inFile);
            mpu.finish();
        } catch (Exception ignored) {}
    }

    public static void downloadFile(String url, File outFile) {
        if (url == null || outFile == null) return;
        TL.cached.execute(() -> {
            try {
                if (!outFile.exists()) outFile.createNewFile(); // setup output file
                BufferedInputStream bs = new BufferedInputStream(Http.get(url).sendInputStream()); // stream file from url
                FileOutputStream fos = new FileOutputStream(outFile); // setup output stream
                byte[] buffer = new byte[1024]; // setup buffer
                int bytesRead;
                while ((bytesRead = bs.read(buffer, 0, 1024)) != -1) fos.write(buffer, 0, bytesRead); // write stream to file
                bs.close(); // close streams
                fos.close();
            } catch (Exception e) {
                //e.printStackTrace();
            }
        });
    }




}
