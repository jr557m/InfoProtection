import java.io.*;
import java.math.BigInteger;
import java.net.ServerSocket;
import java.net.Socket;

public class ServerB {
    public static void main(String[] args) throws Exception {
        String privateKeyPath = new File("privatekey.pem").getPath();
        String publicKeyPath = new File("publickey.pem").getPath();
        String generatePrivateKey = "openssl genpkey -algorithm RSA -out " + privateKeyPath
                + " -pkeyopt rsa_keygen_bits:1024";
        String generatePublicKey = "openssl rsa -pubout -in " + privateKeyPath + " -out " + publicKeyPath;
        String printKeyStructure = "openssl rsa -text -noout -in " + privateKeyPath;
        String printKeyModulus = "openssl rsa -noout -modulus -in " + privateKeyPath;

        // 1. Генерируем пару ключей
        executeCommand(generatePrivateKey);
        executeCommand(generatePublicKey);

        // 2. Получаем составляющие ключа
        // Модуль
        String modulusHexString = executeCommand(printKeyModulus).split("=")[1];
        BigInteger modulus = new BigInteger(1, hexStringToByteArray(modulusHexString));

        // Публичная экспонента
        String publicExpString = executeCommand(printKeyStructure).replaceAll(" ", "").replaceAll("\n", "");
        publicExpString = publicExpString
                .substring(publicExpString.indexOf("publicExponent:") + 15,
                        publicExpString.indexOf("privateExponent:") - 9)
                .replaceAll(":", "");

        // Приватная экспонента
        String privateExpHexString = executeCommand(printKeyStructure).replaceAll(" ", "").replaceAll("\n", "");
        privateExpHexString = privateExpHexString
                .substring(privateExpHexString.indexOf("privateExponent:") + 16,
                        privateExpHexString.indexOf("prime1:"))
                .replaceAll(":", "");
        BigInteger d = new BigInteger(1, hexStringToByteArray(privateExpHexString));

        try (ServerSocket server = new ServerSocket(5000)) { // Запускаем сервер на порту 5000
            System.out.println("Сервер в ожидании подключения...");
            Socket client = server.accept();
            System.out.println("Соединение готово, ожидание команды...");
            BufferedReader in = new BufferedReader(new InputStreamReader(client.getInputStream())); // Канал чтения
            BufferedWriter out = new BufferedWriter(new OutputStreamWriter(client.getOutputStream())); // Канал записи

            // 3. Отправляем модуль и экспоненту 
            out.write(modulusHexString + "\n");
            out.flush();
            Thread.sleep(500);
            out.write(publicExpString + "\n");
            out.flush();

            while (!client.isClosed()) {
                String entry;
                while ((entry = in.readLine()) != null) {
                    if (entry.equalsIgnoreCase("stop"))
                        break;

                    // 10. Получаем замаскированное сообщение
                    System.out.println("Замаскированное сообщение получено.");
                    BigInteger blindedMessage = new BigInteger(1, hexStringToByteArray(entry));

                    // 11. Подписываем сообщение
                    BigInteger signedBlindedMessage = new BigInteger(1, signBlindedMessage(blindedMessage, d, modulus).toByteArray());

                    // 12. Передаем замаскированное подписанное сообщение обратно клиенту
                    System.out.println("Подписанное сообщение отправлено");
                    out.write(byteArrayToHexString(signedBlindedMessage.toByteArray()) + "\n");
                    out.flush();
                }
                if (entry.equalsIgnoreCase("stop"))
                    break;
            }
            System.out.println("Клиент отключился, сервер завершает работу.");
            in.close();
            out.close();
            client.close();
            server.close();
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }

    // Подпись замаскированного сообщения
    public static BigInteger signBlindedMessage(BigInteger m, BigInteger d, BigInteger p) {
        return m.modPow(d, p);
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

    // Перевод массива байтов в строку в шестнадцатеричном представлении
    public static String byteArrayToHexString(byte[] b) {
        String result = "";
        for (int i = 0; i < b.length; i++) {
            result += Integer.toString((b[i] & 0xff) + 0x100, 16).substring(1);
        }
        return result;
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
