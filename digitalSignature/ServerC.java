import java.io.*;
import java.math.BigInteger;
import java.net.ServerSocket;
import java.net.Socket;

public class ServerC {

    public static VerifVoicesCounter counter = new VerifVoicesCounter();

    public static void main(String args[]) throws Exception {
        try (ServerSocket server = new ServerSocket(5005)) { // Запускаем сервер на порту 5005
            System.out.println("Сервер в ожидании подключения...");
            Socket client = server.accept();
            System.out.println("Соединение готово, ожидание команды...");
            BufferedReader in = new BufferedReader(new InputStreamReader(client.getInputStream())); // Канал чтения
            BufferedWriter out = new BufferedWriter(new OutputStreamWriter(client.getOutputStream())); // Канал записи

            // Получаем модуль и публичную экспоненту (Можно получать с сервера B)
            String modulusHexString = in.readLine();
            BigInteger modulus = new BigInteger(1, hexStringToByteArray(modulusHexString));
            String publicExpString = in.readLine();
            BigInteger e = BigInteger.valueOf(Integer.parseInt(publicExpString));
            
            String originalMessage = "", signedMessage = "";
            while (!client.isClosed()) {
                String entry;
                while ((entry = in.readLine()) != null) {
                    if (entry.equalsIgnoreCase("stop"))
                        break;
                    if (entry.equalsIgnoreCase("result")) {
                        System.out.println("Получена команда result. Отправка результата клиенту");
                        counter.getCounter().forEach((key, value) -> {
                            System.out.println(key + " - " + value);
                            try {
                                out.write(key + " - " + value + "\n");
                                out.flush();
                            } catch (IOException e1) {
                                e1.printStackTrace();
                            }
                        });
                        out.write("stop" + "\n");
                        out.flush();
                        break;
                    }
                    
                    // Символ для определения очередности сообщений
                    char lastChar = entry.substring(entry.length() - 1).charAt(0);
                    // 16. Получаем сообщение от клиента
                    String message = entry.substring(0, entry.length() - 1);
                    
                    System.out.println("Получено сообщение " + lastChar);
                    
                    if (lastChar == '0') { // Получаем оригинальное сообщение
                        originalMessage = message;
                    } else if (lastChar == '1') { // Получаем подписанное сообщение
                        signedMessage = message;
                        BigInteger m = new BigInteger(1, originalMessage.getBytes());
                        BigInteger signedMessageBI = new BigInteger(1, hexStringToByteArray(signedMessage));

                        // 17. Проверяем подпись
                        Boolean result = verifySignature(signedMessageBI, m, e, modulus);
                        if (result) {
                            counter.incrementCount(originalMessage);
                            System.out.println("Голос подтвержден. Кандидат: " + originalMessage);
                        } else {
                            System.out.println("Голос не подтвержден.");
                        }
                    }
                }
                if (entry.equalsIgnoreCase("stop"))
                    break;
            }
            System.out.println("Клиент отключился, сервер завершает работу.");
            in.close();
            client.close();
            server.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    // Проверка подлинности голоса 
    public static boolean verifySignature(BigInteger signedM, BigInteger m, BigInteger e, BigInteger p) {
        return signedM.modPow(e, p).equals(m);
    }

    // Перевод строки, записанной в 16сс, в массив байтов
    public static byte[] hexStringToByteArray(String hexString) {
        if (hexString.length() % 2 != 0) {
            hexString = "0" + hexString;
        }
        int len = hexString.length();
        byte[] byteArray = new byte[len / 2];
        for (int i = 0; i < len; i += 2) {
            byteArray[i / 2] = (byte) ((Character.digit(hexString.charAt(i), 16) << 4)
                    + Character.digit(hexString.charAt(i + 1), 16));
        }
        return byteArray;
    }

    // Выполнение команды в командной строке Windows
    public static String executeCommand(String command) {
        try {
            Process process = new ProcessBuilder("cmd.exe", "/c", command).start();
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            String line, result = "";
            while ((line = reader.readLine()) != null) {
                result += line;
            }

            int exitCode = process.waitFor();
            if (exitCode != 0) {
                System.err.println("Error: " + exitCode);
            }
            return result;
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }
        return null;
    }
}
