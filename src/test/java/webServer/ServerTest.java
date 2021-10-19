package webServer;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Assertions;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.logging.Level;

/**
 * This Test determines the stability, reliability of the Server overall. Hence, this test covers general availability of
 * the server and ensure the socket and connection is setup properly.
 */
class ServerTest {
    private static Server server;

    @BeforeAll
    public static void setUpClass() {
        server = new Server("C:\\Users\\yigit\\Desktop", 1337, Level.ALL);
        Thread serve = new Thread(server);
        serve.start();
    }

    @AfterAll
    public static void shutDown() {
        server.stop();
    }

    @org.junit.jupiter.api.Test
    void StatusCodeTest() throws IOException {
        URL url = new URL("http://localhost:1337/");
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestProperty("accept", "text/html");
        int status = connection.getResponseCode();
        Assertions.assertEquals(HttpURLConnection.HTTP_OK, status);
    }

}