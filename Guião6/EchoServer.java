import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;

public class EchoServer {

    public static void main(String[] args) {
        try {
            ServerSocket ss = new ServerSocket(12345);

            while (true) {
                Socket socket = ss.accept();

                BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                PrintWriter out = new PrintWriter(socket.getOutputStream());

                long sum = 0;
                long count = 0;

                String line;
                while ((line = in.readLine()) != null) {
                    try {
                        int value = Integer.parseInt(line.trim());
                        sum += value;
                        count++;
                        out.println(sum); // enviar soma cumulativa
                        out.flush();
                    } catch (NumberFormatException e) {
                        // ignorar linhas não numéricas ou opcionalmente enviar erro
                        out.println("ERROR: not an integer");
                        out.flush();
                    }
                }

                // cliente terminou de enviar; enviar média se existirem números
                if (count > 0) {
                    double average = sum / (double) count;
                    out.println(average);
                    out.flush();
                } else {
                    out.println("NO_DATA");
                    out.flush();
                }

                socket.shutdownOutput();
                socket.shutdownInput();
                socket.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
