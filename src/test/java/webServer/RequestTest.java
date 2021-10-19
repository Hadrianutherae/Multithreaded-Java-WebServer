package webServer;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.logging.Level;

/**
 * This test determines whether fundamental features like HEAD, GET requests are working properly. Furthermore
 * it tests if the Mime Type of the files given can be guessed properly. It also tests the Etag feature among
 * other various sanity checks.
 */
class RequestTest {
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
    void testNonExistentDirectoryOrFile() throws IOException {
        URL url = new URL("http://localhost:1337/thisdirectorydoesnotexist");
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestProperty("accept", "text/html");
        Assertions.assertEquals(HttpURLConnection.HTTP_NOT_FOUND, connection.getResponseCode());
    }

    @org.junit.jupiter.api.Test
    void testUmlaut() throws IOException {
        URL url = new URL("http://localhost:1337/thisdirectöry");
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestProperty("accept", "text/html");
        Assertions.assertEquals(HttpURLConnection.HTTP_NOT_FOUND, connection.getResponseCode());
    }

    @org.junit.jupiter.api.Test
    void testUmlautPositive() throws IOException {
        URL url = new URL("http://localhost:1337/PhD/backup/Dügün/");
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        Assertions.assertEquals(HttpURLConnection.HTTP_OK,  connection.getResponseCode());
    }

    @org.junit.jupiter.api.Test
    void testWhiteSpace() throws IOException {
        URL url = new URL("http://localhost:1337/New%20folder/");
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestProperty("accept", "text/html");
        Assertions.assertEquals(HttpURLConnection.HTTP_OK, connection.getResponseCode());
    }

    @org.junit.jupiter.api.Test
    void getPng() throws IOException {
        URL url = new URL("http://localhost:1337/Figure_3.png/");
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestProperty("accept", "image/png");
        Assertions.assertEquals(HttpURLConnection.HTTP_OK, connection.getResponseCode());
    }

    @org.junit.jupiter.api.Test
    void testHeadBodyEmpty() throws IOException {
        URL url = new URL("http://localhost:1337/");
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("HEAD");
        connection.setDoOutput(true);
        BufferedReader br = new BufferedReader(new InputStreamReader(connection.getInputStream()));
        Assertions.assertNull(br.readLine());
    }

    @org.junit.jupiter.api.Test
    void testGetBodyNotEmpty() throws IOException {
        URL url = new URL("http://localhost:1337/");
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setDoOutput(true);
        BufferedReader br = new BufferedReader(new InputStreamReader(connection.getInputStream()));
        Assertions.assertNotNull(br.readLine());
    }


    @org.junit.jupiter.api.Test
    void testValidEtag() throws IOException {
        URL url = new URL("http://localhost:1337/Figure_3.png");
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestProperty("If-None-Match", "b688530bd0dea0098d326ebbdefed014");
        Assertions.assertEquals(304, connection.getResponseCode());
    }

    @org.junit.jupiter.api.Test
    void testEtagPrecondition() throws IOException {
        URL url = new URL("http://localhost:1337/Figure_3.png");
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestProperty("If-Match", "b688530bd0dea0098d326ebbdefed013");
        Assertions.assertEquals(412, connection.getResponseCode());
    }

    @org.junit.jupiter.api.Test
    void testEtagAsterisk() throws IOException {
        URL url = new URL("http://localhost:1337/Figure_3.png");
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestProperty("If-Match", "*");
        Assertions.assertEquals(200, connection.getResponseCode());
    }

    @org.junit.jupiter.api.Test
    void testEtagList() throws IOException {
        URL url = new URL("http://localhost:1337/Figure_3.png");
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestProperty("If-Match", "b688530bd0dea0098d326ebbdefed013,b688530bd0dea0098d326ebbdefed014,b688530bd0dea0098d326ebbdefed015");
        Assertions.assertEquals(200, connection.getResponseCode());
    }


    @org.junit.jupiter.api.Test
    void testInvalidEtag() throws IOException {
        URL url = new URL("http://localhost:1337/Figure_3.png");
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestProperty("If-None-Match", "b688530bd0dea0098d326ebbdefed013");
        Assertions.assertEquals(200, connection.getResponseCode());
    }

    @org.junit.jupiter.api.Test
    void testOutdatedTS() throws IOException {
        URL url = new URL("http://localhost:1337/Figure_3.png");
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestProperty("If-Modified-Since", "Tue Oct 19 21:42:22 CEST 2000");
        Assertions.assertEquals(200, connection.getResponseCode());
    }

    @org.junit.jupiter.api.Test
    void testNewTS() throws IOException {
        URL url = new URL("http://localhost:1337/Figure_3.png");
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestProperty("If-Modified-Since", "Tue Oct 19 21:42:22 CEST 2021");
        Assertions.assertEquals(304, connection.getResponseCode());
    }



    @org.junit.jupiter.api.Test
    void testEtagExists() throws IOException {
        URL url = new URL("http://localhost:1337/Figure_3.png");
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        Assertions.assertNotEquals("", connection.getHeaderField("Etag"));
    }

    @org.junit.jupiter.api.Test
    void testSubdirectory() throws IOException {
        URL url = new URL("http://localhost:1337/DGCNN/dgcnn-master/dgcnn-master/pytorch/data/");
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setDoOutput(true);
        BufferedReader br = new BufferedReader(new InputStreamReader(connection.getInputStream()));
        Assertions.assertEquals("<html><a href='..'>> ..</a><br>", br.readLine());
    }


    @org.junit.jupiter.api.Test
    void checkMimeTypeText() throws IOException {
        URL url = new URL("http://localhost:1337/heas.txt");
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        Assertions.assertEquals("text/txt", connection.getHeaderField("Content-Type"));
    }

    @org.junit.jupiter.api.Test
    void checkMimeTypeImage() throws IOException {
        URL url = new URL("http://localhost:1337/Figure_3.png");
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        Assertions.assertEquals("image/png", connection.getHeaderField("Content-Type"));
    }

    @org.junit.jupiter.api.Test
    void checkMimeTypePdf() throws IOException {
        URL url = new URL("http://localhost:1337/invoice.pdf");
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        Assertions.assertEquals("application/pdf", connection.getHeaderField("Content-Type"));
    }

    @org.junit.jupiter.api.Test
    void checkPngBody() throws IOException {
        String s;
        String path;
        URL url = new URL("http://localhost:1337/Figure_3.png");
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestProperty("accept", "image/png");
        InputStreamReader in = new InputStreamReader(connection.getInputStream());
        int status = connection.getResponseCode();
    }
}