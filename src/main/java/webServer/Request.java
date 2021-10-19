package webServer;

import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.lang3.ArrayUtils;

import java.io.*;
import java.net.Socket;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.logging.Logger;
import java.util.zip.GZIPOutputStream;

/**
 * A simple Request class, which is spawned whenever a new request comes in. Enables the server to handle multiple clients
 * at once.
 * Features:
 * + GZIP compression
 * + MIME Type guessing
 * + Etag support for if-Match, if-None-Match, and if-Modified-Since
 * + Allow to discover subdirectories
 * + Multithreading
 * TODOs:
 * - improve file transfer speed, a socket sends approx. 100kb/s.
 * - introduce more robust parsing of the inbound request to prevent bad actors
 * - introduce MIME sniffing for more accurate results
 *  * @author Oguzhan Yigit
 *  * @email yigit@adobe.com
 *  * @version 0.1
 */
public class Request implements Runnable{
    private OutputStream rawOutput = null;
    private final StringBuilder header;
    private final StringBuilder body;
    private final BufferedReader in;
    /**
     * HTTP Methods
     **/
    private enum Type{GET, HEAD, POST, PUT, DELETE, CONNECT, OPTIONS, TRACE, PATCH}
    /**
     * Etag modes
     * */
    private enum Mode{IfMatch, IfNoneMatch, IfModifiedSince}
    private final String[] imageTypes = {"png", "jpg", "jpeg", "gif", "svg"};
    private final String[] textTypes = {"txt", "html", "ics", "css", "csv", "rtf"};
    private final String[] appTypes = {"pdf", "zip", "xml", "7z", "json", "csv", "rtf"};
    private final String[] videoTypes = {"mp4", "mpeg", "webm"};
    private final static Logger LOGGER = Logger.getLogger(Server.class.getName());
    private final Server server;

    public Request(Server server, Socket accept) throws IOException, ParseException {
        rawOutput = accept.getOutputStream();
        in = new BufferedReader(new InputStreamReader(accept.getInputStream()));
        header =  new StringBuilder();
        body = new StringBuilder();
        this.server = server;
        parseHttp();
        rawOutput.close();
    }

    @Override
    public void run() {
        LOGGER.info("New thread spawned for incoming request.");
    }


    /**
     * Guesses the Mime type based on the file ending. Very fuzzy implementation, content sniffing is out of scope.
     * @param filename The filename we want to guess
     * @param fileEnding The file ending we use to guess
     */
    private void guessMimeType(String filename, String fileEnding) {
        if(ArrayUtils.contains(imageTypes, fileEnding)){
            header.append("Content-Type: image/").append(fileEnding).append("\n");
        }
        else if(ArrayUtils.contains(textTypes, fileEnding)){
            header.append("Content-Type: text/").append(fileEnding).append("\n");
        }
        else if(ArrayUtils.contains(appTypes, fileEnding)){
            header.append("Content-Type: application/").append(fileEnding).append("\n");
        }
        else if(ArrayUtils.contains(videoTypes, fileEnding)){
            header.append("Content-Type: video/").append(fileEnding).append("\n");
        }
        // RFC 2046 Fallback
        else{
            header.append("Content-Type: application/octet-stream").append("\n");
            header.append("Content-Disposition: attachment; filename=").append(filename).append("\n");
        }
    }


    /**
     * Parses the HTTP Request Type.
     * @param rawRequest the first word of the request.
     * @return  Type (enum).
     */
    private Type parseType(String rawRequest){
        Type request;
        request = Type.valueOf(rawRequest.substring(0, rawRequest.indexOf(" ")));
        return request;
    }

    /**
     * Parses the path, the second argument in the HTTP request.
     * @param rawRequest the second word of the request.
     * @return The path to be queried.
     */
    private String parsePath(String rawRequest){
        return rawRequest.substring(rawRequest.indexOf(" ")+1, rawRequest.indexOf(" ", rawRequest.indexOf(" ")+1));
    }

    /**
     * Explores a directory or a file. If it turns out as a directory, return a list of
     * all files and directories within that directory. If it turns out as a file
     * return the file by guessing its Mime type in a gzipped way. If the Mime type cannot
     * be determined, fall back to gzip.
     * @param requestedPath The path the client wants to retrieve
     * @param method The method the client uses to retrieve the content
     * @param tag The provided Etag for comparison
     * @param threshold The provided last modified date the client knows
     * @param mode Etag mode
     * @throws IOException We throw an exception if we have trouble opening the file
     */
    private void exploreDirectoryOrFile(String requestedPath, Type method, String tag, Date threshold, Mode mode) throws IOException {
        File givenPath = new File(server.servedDirectory + requestedPath);
        if (!givenPath.exists())
        {
            LOGGER.info("404 Requested path was not found");
            header.append("HTTP/1.1 404 Not Found\n");
            header.append("Server: Custom Java Webserver\n");
            header.append("Content-Type: text/html; charset=iso-8859-1\n");
            header.append("Date: ").append(new Date()).append("\n");
            body.append("Requested URL does not exist");
            header.append("Content-Length: ").append(body.toString().length()+1).append("\n").append("\n");
            writeHeaderToSocket(header.toString());
            writeBodyToSocket(body.toString().getBytes());
        }
        else
        {
            if (givenPath.isDirectory()) {
                LOGGER.info("Serving Directory");
                header.append("HTTP/1.1 200 OK\n");
                header.append("Server: Custom Java Webserver\n");
                header.append("Content-Type: text/html; charset=utf-8\n");
                body.append("<html><a href='..'>> ..</a><br>").append("\n");
                LOGGER.info("Requested Path" + requestedPath);
                String offset = "";
                if (!requestedPath.equals("/")) {
                    offset = requestedPath;
                }
                String finalOffset = offset;

                Files.list(new File(server.servedDirectory + requestedPath).toPath())
                        .forEach(path -> {
                            body.append("> <a href='").append(finalOffset).append(path.getFileName()).append("/'><i>").append(path).append("</i></a><br>\n");
                        });
                body.append("</html>").append("\n");
                header.append("Date: ").append(new Date()).append("\n");
                header.append("Content-Length: ").append(body.toString().getBytes().length).append("\n");

                if (method == Type.GET) {
                    header.append("\n");
                    header.append(body);
                }
                writeHeaderToSocket(header.toString());
            }
            else if (givenPath.isFile())
            {
                LOGGER.info("Serving File");
                String etag;
                boolean ifMatch = false;
                String fileEnding = givenPath.getName().substring(givenPath.getName().lastIndexOf(".")+1).toLowerCase();
                Date lastModified = new Date(givenPath.lastModified());
                ByteArrayOutputStream byteStream = readFile(givenPath);
                byte[] byteBody;
                byteBody = byteStream.toByteArray();
                etag = DigestUtils.md5Hex(byteStream.toByteArray());

                // If multiple tags were provided.
                if(tag.contains(","))
                {
                    String[] tags = tag.split(",");
                    for (String i:tags)
                    {
                        if (etag.equals(i)) {
                            ifMatch = true;
                            break;
                        }
                    }
                }

                // Check preconditions for If-Match: check if an asterisk was provided and if the tag does not match
                if(!etag.equals(tag) && mode== Mode.IfMatch && !tag.equals("*") && !ifMatch) {
                    header.append("HTTP/1.1 412 Precondition failed\n");
                    header.append("Date: ").append(new Date()).append("\n");
                    header.append("Server: Custom Java Webserver\n");
                    header.append("Etag: " + etag + "\n");
                    writeHeaderToSocket(header.toString());
                }
                // Check preconditions for If-None-Match: check if tag matches or LM date is prior to the threshold.
                else if((etag.equals(tag) && mode== Mode.IfNoneMatch) || (mode== Mode.IfModifiedSince && threshold.after(lastModified))){
                    header.append("HTTP/1.1 304 Not Modified\n");
                    header.append("Server: Custom Java Webserver\n");
                    header.append("Etag: " + etag + "\n");
                    guessMimeType(givenPath.getName(), fileEnding);
                    writeHeaderToSocket(header.toString());
                }
                // Check if etag does not equal the tag provided (default case), check if LM is after threshold, check if-Match has a match.
                else if(!etag.equals(tag) || lastModified.after(threshold) || (mode==Mode.IfMatch && (etag.equals(tag)||tag.equals("*")||ifMatch))) {
                    header.append("HTTP/1.1 200 OK\n");
                    header.append("Last-Modified: " + new Date(givenPath.lastModified()) + "\n");
                    header.append("Content-Encoding: gzip\n");
                    header.append("Content-Length: ").append(byteBody.length).append("\n");
                    guessMimeType(givenPath.getName(), fileEnding);
                    header.append("Date: ").append(new Date()).append("\n");
                    header.append("Etag: " + etag + "\n");
                    header.append("Server: Custom Java Webserver\n");
                    writeHeaderToSocket(header.toString());
                    if(method==Type.GET){
                        writeBodyToSocket(byteBody);
                    }
                }
            }
        }
    }

    /**
     * Compresses the FIS into GZIP. Due to Content-Encoding, the browser will decompress it on the fly.
     * @param givenPath the file we want to compress
     * @return Bytestream to write to socket
     * @throws IOException We throw an exception if the file cannot be opened.
     */
    private ByteArrayOutputStream readFile(File givenPath) throws IOException {
        FileInputStream fis = new FileInputStream(givenPath);
        ByteArrayOutputStream byteStream = new ByteArrayOutputStream();
        GZIPOutputStream zipStream = new GZIPOutputStream(byteStream);
        int bh;
        while((bh = fis.read())!=-1) {
            zipStream.write((byte) bh);
        }
        zipStream.flush();
        byteStream.flush();
        byteStream.close();
        zipStream.close();
        return byteStream;
    }

    /**
     * Parses the HTTP Request and calls the methods to query the directory or file.
     * Currently parses:
     * - HTTP Method
     * - Path
     * - If-None-Match
     * - If-Match
     * - If-Modified-Since
     * @throws IOException We throw an IOException in case we cannot read from the socket
     * @throws ParseException We throw a ParseException if we cannot parse the given request
     */
    private void parseHttp() throws IOException, ParseException {
        Type request;
        String s;
        String path;
        String tag = "";
        Mode mode = null;
        Date threshold = null;
        s = in.readLine();
        request = parseType(s);
        LOGGER.info("Received " + request + " request");
        path = URLDecoder.decode(parsePath(s), String.valueOf(StandardCharsets.UTF_8));
        LOGGER.info("Requesting " + path);
        while ((s = this.in.readLine()) != null) {
            System.out.println(s);
            if (s.contains("If-None-Match:")){
                tag = s.substring(s.indexOf(":")+2);
                mode = Mode.IfNoneMatch;
            }
            else if  (s.contains("If-Match:")){
                tag = s.substring(s.indexOf(":")+2);
                mode = Mode.IfMatch;
            }
            else if  (s.contains("If-Modified-Since:")){
                tag = s.substring(s.indexOf(":")+2);
                SimpleDateFormat format = new SimpleDateFormat("E MMM d HH:mm:ss z yyyy");
                threshold = format.parse(tag);
                mode = Mode.IfModifiedSince;
            }
            if (s.isEmpty()) {
                break;
            }
        }
        exploreDirectoryOrFile(path, request, tag, threshold, mode);
    }

    /**
     * Writes back the http Header to the clientSocket.
     * @param message The http Header.
     */
    private void writeHeaderToSocket(String message){
        InputStream stream = new ByteArrayInputStream(message.getBytes(StandardCharsets.UTF_8));
        int ch;
        try {
            while ((ch = stream.read()) != -1) {
                rawOutput.write((char) ch);
            }
        }
        catch(IOException e)
        {
            LOGGER.info("Client has dropped the connection.");
        }

    }

    /**
     * Writes back the HTTP body as a bytestream, as we need to transfer files byte by byte.
     * @param message The file content.
     * @throws IOException We throw an IOException if we get interrupted while writing to the output socket
     */
    private void writeBodyToSocket(byte[] message) throws IOException {
        rawOutput.write('\n');
        InputStream stream = new ByteArrayInputStream(message);
        int ch;
        try {
            while ((ch = stream.read()) != -1) {
                rawOutput.write((char) ch);
            }
        }
        catch (IOException e)
        {
            LOGGER.info("Client has dropped the connection.");
        }
    }

}
