import java.io.*;
import java.math.BigInteger;
import java.net.Socket;
import java.security.SecureRandom;
import java.util.Scanner;

public class ClientA {
    public static void main(String args[]) throws Exception {
        Scanner in = new Scanner(System.in, "Cp866");
        String session = "1";
        try {
            // Подключаемся к серверу B
            Socket socketB = new Socket("localhost", 5000);
            BufferedReader inFromB = new BufferedReader(new InputStreamReader(socketB.getInputStream()));
            BufferedWriter outToB = new BufferedWriter(new OutputStreamWriter(socketB.getOutputStream()));

            // Подключаемся к серверу С
            Socket socketC = new Socket("localhost", 5005);
            BufferedReader inFromC = new BufferedReader(new InputStreamReader(socketC.getInputStream()));
            BufferedWriter outToC = new BufferedWriter(new OutputStreamWriter(socketC.getOutputStream()));

            // 4. Получаем с подписывающего сервера модуль и экспоненту публичного ключа
            String modulusHexString = inFromB.readLine();
            BigInteger modulus = new BigInteger(1, hexStringToByteArray(modulusHexString));
            String publicExpString = inFromB.readLine();
            BigInteger e = BigInteger.valueOf(Integer.parseInt(publicExpString));

            // Отправляем проверяющему серверу модуль и экспоненту
            outToC.write(modulusHexString + "\n");
            outToC.flush();
            Thread.sleep(500);
            outToC.write(publicExpString + "\n");
            outToC.flush();

            while (!socketB.isOutputShutdown()) {
                while (session.equals("1")) {
                    // 5. Записываем голос пользователя
                    System.out.println("Ваш голос:");
                    String voice = in.nextLine();

                    // 6. Переводим сообщение в BigInteger для дальнейшей маскировки
                    BigInteger message = new BigInteger(1, voice.getBytes());
                    // System.out.println("msgBI: " + message);

                    // 7. Генерируем случайный маскирующий множитель
                    BigInteger r = new BigInteger(1, generateR(modulus).toByteArray());

                    // 8. Маскируем сообщение случайным множителем
                    BigInteger blindedMessage = new BigInteger(1,
                            blindMessage(message, r, e, modulus).toByteArray());
                    // System.out.println("blindedMsgBI (sent to sB): " + blindedMessage);

                    // 9. Передаем замаскированное сообщение на подпись
                    System.out.println("Сообщение отправлено на подпись");
                    outToB.write(byteArrayToHexString(blindedMessage.toByteArray()) + "\n");
                    outToB.flush();
                    Thread.sleep(1000);

                    // 13. Получаем подписанное замаскированное сообщение
                    System.out.println("Подписанное сообщение получено");
                    BigInteger signedBlindedMessage = new BigInteger(1, hexStringToByteArray(inFromB.readLine()));
                    // System.out.println("signedBlindedMsgBI (recieved from sB): " + signedBlindedMessage);

                    // 14. Снимаем маскировку
                    BigInteger signedMessage = unblindSignature(signedBlindedMessage, r, modulus);
                    // System.out.println("signedMsgBI (sent to sC): " + signedMessage);

                    // 15. Отправляем проверяющему серверу оригинальное и подписанное сообщение
                    System.out.println("Оригинальное сообщение отправлено");
                    outToC.write(voice + "0\n");
                    outToC.flush();
                    Thread.sleep(500);
                    System.out.println("Подписанное сообщение отправлено");
                    outToC.write(byteArrayToHexString(signedMessage.toByteArray()) + "1\n");
                    outToC.flush();
                    Thread.sleep(1000);

                    // 18. Отправляем команду для получения результатов голосования
                    outToC.write("result" + "\n");
                    outToC.flush();
                    Thread.sleep(1000);

                    System.out.println("Кол-во верифицированных голосов: ");
                    String counterString;
                    while((counterString = inFromC.readLine()) != null) {
                        if (counterString.equalsIgnoreCase("stop"))
                            break;
                        System.out.println(counterString);
                    }

                    System.out.println("1 - продолжить, 0 - завершить. Ваш выбор:");
                    session = in.nextLine();
                }
                outToB.write("stop" + "\n");
                outToB.flush();
                outToC.write("stop" + "\n");
                outToC.flush();
            }
            inFromB.close();
            outToB.close();
            inFromC.close();
            outToC.close();
            socketB.close();
            socketC.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // Генерация маскирующего множителя
    public static BigInteger generateR(BigInteger p) {
        SecureRandom random = new SecureRandom();
        BigInteger r;
        r = new BigInteger(p.bitLength(), random);
        while (r.compareTo(p) >= 0 || !r.gcd(p).equals(BigInteger.ONE)){ // Проверяем взаимно простое ли оно с модулем
            r = new BigInteger(p.bitLength(), random);
        } 
        return r;
    }

    // Маскировка сообщения
    public static BigInteger blindMessage(BigInteger m, BigInteger r, BigInteger e, BigInteger p) {
        return m.multiply(r.modPow(e, p)).mod(p);
    }

    // Снятие маскировки
    public static BigInteger unblindSignature(BigInteger m, BigInteger r, BigInteger p) {
        return m.multiply(r.modInverse(p)).mod(p);
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
