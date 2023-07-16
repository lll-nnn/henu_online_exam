package com.lee.util;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;

public class FileDownloadUtil {
    public static void downloadFile(String path, HttpServletResponse response){
        String filename =  path.substring(path.lastIndexOf("/")+1);
        response.reset();
        response.setContentType("application/octet-stream");
        response.addHeader("Content-Disposition", "attachment;filename="+filename);
        File file = new File(path);
        if (!file.exists()){
            throw new RuntimeException("文件不存在");
        }

        try(InputStream inputStream = Files.newInputStream(Paths.get(path))){
            ServletOutputStream outputStream = response.getOutputStream();
            byte[] buf = new byte[1024];
            int len = 0;
            while((len = inputStream.read(buf)) != -1){
                outputStream.write(buf, 0 , len);
                outputStream.flush();
            }
        }catch (IOException e){
            e.printStackTrace();
        }
    }
}
