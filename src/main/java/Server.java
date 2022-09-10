import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Server {
    final static int MAX_NUMBER_THREADS = 64;
    final static List<String> validPaths = List.of("/index.html", "/spring.svg", "/spring.png", "/resources.html", "/styles.css", "/app.js", "/links.html", "/forms.html", "/classic.html", "/events.html", "/events.js");
    final static ExecutorService threadPool = Executors.newFixedThreadPool(MAX_NUMBER_THREADS);

    public static void start(int port) {
        try (final var serverSocket = new ServerSocket(port)) {
            while (true) {
                final var socket = serverSocket.accept();
                threadPool.submit(() -> Server.connectionProcessing(socket));
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void connectionProcessing(Socket socket) {
        try (
                final var in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                final var out = new BufferedOutputStream(socket.getOutputStream())
        ) {
            final var requestLine = in.readLine();
            System.out.println(requestLine);
            final var parts = requestLine.split(" ");

            if (parts.length != 3) {
                return;
            }

            final var path = parts[1];
            if (!validPaths.contains(path)) {
                out.write(getResponseText404());
                out.flush();
                return;
            }

            final var filePath = Path.of(".", "public", path);
            final var mimeType = Files.probeContentType(filePath);
            if (path.equals("/classic.html")) {
                final var template = Files.readString(filePath);
                final var content = template
                        .replace("{time}", LocalDateTime.now().toString())
                        .getBytes();
                out.write(getResponseText200(mimeType, content.length));
                out.write(content);
                out.flush();
                return;
            }

            out.write(getResponseText200(mimeType, Files.size(filePath)));
            Files.copy(filePath, out);
            out.flush();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                socket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private static byte[] getResponseText200(String mimeType, long contentLength) {
        return ("HTTP/1.1 200 OK\r\n" +
                "Content-Type: " + mimeType + "\r\n" +
                "Content-Length: " + contentLength + "\r\n" +
                "Connection: close\r\n" +
                "\r\n").getBytes();

    }

    private static byte[] getResponseText404(){
        return (
                """
                        HTTP/1.1 404 Not Found\r
                        Content-Length: 0\r
                        Connection: close\r
                        \r
                        """
        ).getBytes();
    }

}
