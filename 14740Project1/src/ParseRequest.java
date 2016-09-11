/*
 * @file: ParseRequest.java
 * 
 * @author: Xiaocheng Ou
 *
 * @reference : https://github.com/davidbuick/Telecom/blob/master/src/ResponseRequest.java
 * 
 * @date:Feb 18, 2016
 * 
 */

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileReader;

public class ParseRequest {
    String request = null;
    String partpath = null;
    String[] requestarray;

    String responsestatus = "";
    String responseheader = "";
    String responsecontent = "";
    String response = "";

    String method = "";
    String url = "";
    String protocal = "";
    String[] version;

    public ParseRequest(String request, String partpath) {
        this.request = request;
        this.partpath = partpath;

        System.err.println("At server: request is" + request);
        this.requestarray = request.split(" ");
    }


    public String respond() {
        //404 error
        if (requestarray.length != 3) {
            responsestatus = "HTTP/1.0 404 Not Found\r\n";
            return responsestatus;
        }

        method = requestarray[0];
        
        protocal = requestarray[2];

        //501 error
        if (!method.equals("GET") && !method.equals("HEAD")) {
            responsestatus = "HTTP/1.0 501 Not implemented\r\n";
            return responsestatus;
        }

        String[] protocalarray = protocal.split("/");

        //505 error
        if (protocalarray.length != 2) {
            System.err.println("[INFO] protocalarray.length "+protocalarray.length);
            responsestatus = "HTTP/1.0 505 HTTP Version not supported\r\n";
            return responsestatus;
        } else if (!protocalarray[0].equalsIgnoreCase("http")) {
            System.err.println("[INFO] protocalarray[0] "+protocalarray[0]);
            responsestatus = "HTTP/1.0 505 HTTP Version not supported\r\n";
            return responsestatus;
        }

        System.err.println("Protocol is " + protocalarray[1]);
        version = protocalarray[1].split("[.]");

        if (!version[0].equals("1") || !(version[1].equals("0") || version[1].equals("1"))) {
            System.err.println("[INFO] version[0]:"+version[0]+",version[1]:"+version[1]);
            responsestatus = "HTTP/1.0 505 HTTP Version not supported\r\n";
            return responsestatus;
        }


        url = requestarray[1];
        
        if(url.equals("/")){
            url = url+"index.html";
        } 
        String[] urlarray = url.split("/");
        if (urlarray[urlarray.length-1].contains("cgi")){
            CgiHandler parsecgi = new CgiHandler(partpath, url);
            response = parsecgi.respond();
            return response;
        }
        
        

        //response with content
        responsestatus = "HTTP/1.0 200 OK\r\n";
        responseheader += "Server: Simple/1.0\r\n";
        try {
            String mime = GetMime.getMimeType(url);
            if (mime == null)
                mime = "";
            responseheader += "Content-Type: " + mime + " \r\n";
            responseheader += "\r\n";

            //Do_HEAD
            if (method.equalsIgnoreCase("HEAD")) {
                response = responsestatus + responseheader;
                return response;
            }

            //Do_GET
            String filepath = partpath + url;

            if (mime.startsWith("image")) {
                try {
                    BufferedImage bufferimage = ImageIO.read(new File(filepath));
                    ByteArrayOutputStream arrayStream = new ByteArrayOutputStream();

                    String type = mime.split("/")[1];
                    ImageIO.write(bufferimage, type, arrayStream);
                    arrayStream.flush();

                    byte[] imagebyte = arrayStream.toByteArray();
                    arrayStream.close();

                    responsecontent = new String(imagebyte, "ISO-8859-1");
                } catch (Exception e) {
                    responsestatus = "HTTP/1.0 404 Not Found\r\n";
                    return responsestatus;
                }

            } else {
                try {
                    FileReader filereader = new FileReader(new File(filepath));
                    BufferedReader bufferreader = new BufferedReader(filereader);
                    String line = "";
                    while ((line = bufferreader.readLine()) != null) {
                        responsecontent += line + "\r\n";
                    }
                    bufferreader.close();
                } catch (Exception e) {
                    responsestatus = "HTTP/1.0 404 Not Found\r\n";
                    return responsestatus;
                }
            }

            response = responsestatus + responseheader + responsecontent;
            return response;
        } catch (Exception e) {
            responsestatus = "HTTP/1.0 404 Not Found\r\n";
            return responsestatus;
        }
    }

}
