package webServer;

import java.io.IOException;
import java.net.ServerSocket;
import java.text.ParseException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * A simple WebServer listening on the given port and serves the given directory.
 *  * @author Oguzhan Yigit
 *  * @email yigit@adobe.com
 *  * @version 0.1
 * */
public class Server implements Runnable {
    private final int port;
    private boolean exit = false;
    public final String servedDirectory;
    private final static Logger LOGGER = Logger.getLogger(Server.class.getName());

    @Override
    public void run() {
        try {
            ServerSocket serverSocket = new ServerSocket(this.port);
            LOGGER.info("Serving " + servedDirectory + " on " + this.port);
                while(!exit) {
                    try {
                        Request request = new Request(this, serverSocket.accept());
                        Thread require = new Thread(request);
                        require.start();
                    }
                    catch (NullPointerException | ParseException | IOException e)
                    {
                        LOGGER.info("Client has dropped the connection");
                    }
                }
        } catch (IOException e) {
            LOGGER.warning("Port "  + port + " is currently being used, try another one.");
        }
    }


    public void stop(){
        LOGGER.info("Received shutdown command. Closing for now.");
        this.exit = true;
    }

    /**
     * Initializes the webserver and opens the socket on the given port
     * @param path The path to be served
     * @param port  The port on which the server listens
     * @param logLevel The level on which we want to log the application
     */
    public Server(String path, int port, Level logLevel)
    {
        LOGGER.setLevel(logLevel);
        this.servedDirectory = path;
        this.port = port;

    }

    /**
     * Sets the path and runs the server under the given port.
     * @param args Java convention.
     */
    public static void main(String[] args)
    {
        String path = "C:\\Users\\yigit\\Desktop";
        int port = 1337;

        if(args.length>0){
            if(args[0].length()>0){
                path = args[0];
            }
        }
        if(args.length>1) {
            if (args[1].length() > 0) {
                port = Integer.parseInt(args[1]);
            }
        }

        Server newServer = new Server(path, port, Level.ALL);
        Thread serve = new Thread(newServer);
        serve.start();
    }

}