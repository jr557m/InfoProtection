import java.io.*;
import java.util.*;

public class Main {

    private static int[] key;

    public static void main(String[] args) throws IOException {
        String inputFilePath = new File("res\\28.bmp").getPath();
        String outputFilePath = new File("res\\28embed.bmp").getPath();
        String leasingFilePath = new File("res\\leasing.txt").getPath();
        String inputHash = encrypt(leasingFilePath);

        // Внедрение хешкода в файл bmp
        embed(inputFilePath, outputFilePath, inputHash);
        String extractedHash = extract(outputFilePath);
        // Извлечение хешкода из файла bpm
        System.out.println("Извлеченный хешкод: " + extractedHash);
        System.out.println(inputHash.equals(extractedHash));
    }

    public static void embed(String inputFilePath, String outputFilePath, String hash) throws IOException {
        byte[] hashBytesArray = hexStringToByteArray(hash); // Массив байтов хешкода
        String hashBytesBinary = ""; // Бинарное представление хешкода
        for (byte b : hashBytesArray) {
            hashBytesBinary += String.format("%8s", Integer.toBinaryString(b & 0xFF))
                    .replace(' ', '0');
        }
        char[] hashBitsArray = hashBytesBinary.toCharArray(); // Массив символов бинарного представления

        // Читаем входной файл
        FileInputStream inputStream = new FileInputStream(inputFilePath);
        byte[] header = new byte[122]; // Заголовок
        inputStream.read(header);
        byte[] imageBytes = new byte[inputStream.available()]; // Байты пикселей
        inputStream.read(imageBytes);
        inputStream.close();

        // Генерируем ключ
        key = generateKey(160, 0, imageBytes.length);

        for (int pos = 0; pos < key.length; pos++) {
            String imageByteBinary = String.format("%8s", Integer.toBinaryString(imageBytes[key[pos]] & 0xFF))
                    .replace(' ', '0'); // Записываем бинарное представление байта изображения
            char[] imageByteBinaryArray = imageByteBinary.toCharArray(); // Переводим в массив символов
            imageByteBinaryArray[7] = hashBitsArray[pos]; // Заменяем последний бит

            String embedImageByteBinary = new String(imageByteBinaryArray); // Переводим измененный массив в строку
            byte newImageByte = (byte) Integer.parseInt(embedImageByteBinary, 2); // Переводим бинарную строку в байт
            imageBytes[key[pos]] = newImageByte; // Заменяем байт
        }

        // Запись в файл
        FileOutputStream fos = new FileOutputStream(outputFilePath);
        fos.write(header);
        fos.write(imageBytes);
        fos.close();
    }

    private static String extract(String filePath) throws IOException {
        // Читаем входной файл
        FileInputStream inputStream = new FileInputStream(filePath);
        byte[] header = new byte[122]; // Заголовок
        inputStream.read(header);
        byte[] imageBytes = new byte[inputStream.available()]; // Байты пикселей
        inputStream.read(imageBytes);
        inputStream.close();

        String message = ""; // Строка для результата
        char[] charBitsArray = new char[8]; // Массив для извлеченных битов (для составления байта)
        for (int pos = 0; pos <= key.length; pos++) {
            if (pos % 8 == 0 && pos != 0) {
                String extractedByteBinary = new String(charBitsArray); // Получаем бинарную строку из массива битов
                byte extractedByte = (byte) Integer.parseInt(extractedByteBinary, 2); // Переводим бинарную строку в байт
                message += byteArrayToHexString(new byte[] { extractedByte }); // Дописываем представление байта в 16сс
                charBitsArray = new char[8];
                if (pos == 160) break;
            }
           
            String imageByteBinary = String.format("%8s", Integer.toBinaryString(imageBytes[key[pos]] & 0xFF))
                    .replace(' ', '0'); // Извлекаем байт и получаем бинарное представление
            char[] imageByteBinaryArray = imageByteBinary.toCharArray(); // Переводим в массив символов
            charBitsArray[pos % 8] = imageByteBinaryArray[7]; // Извлекаем последний бит
        }

        return message;
    }

    public static byte[] hexStringToByteArray(String hexString) {
        int len = hexString.length();
        byte[] byteArray = new byte[len / 2];
        for (int i = 0; i < len; i += 2) {
            byteArray[i / 2] = (byte) ((Character.digit(hexString.charAt(i), 16) << 4)
                    + Character.digit(hexString.charAt(i + 1), 16));
        }
        return byteArray;
    }

    public static String byteArrayToHexString(byte[] b) {
        String result = "";
        for (int i = 0; i < b.length; i++) {
            result += Integer.toString((b[i] & 0xff) + 0x100, 16).substring(1);
        }
        return result;
    }

    private static String encrypt(String filePath) {
        try {
            ProcessBuilder pb = new ProcessBuilder("cmd.exe", "/c", "openssl dgst -sha1 " + filePath);
            Process process = pb.start();
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            String line, result = "";
            while ((line = reader.readLine()) != null) {
                result = line.split("= ")[1];
                System.out.println("leasing.txt: " + result);
            }

            int exitCode = process.waitFor();
            if (exitCode != 0) {
                System.err.println("Error: " + exitCode);
            }

            return result;
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
            return e.toString();
        }
    }

    public static int[] generateKey(int count, int min, int max) throws IllegalArgumentException {
        if (count > max - min + 1)
            throw new IllegalArgumentException("Длина не может превышать макисмальный размер");

        int[] result = new int[count];
        Set<Integer> generated = new HashSet<>();

        Random random = new Random();
        int i = 0;
        while (i < count) {
            int next = random.nextInt(max - min + 1) + min;
            if (!generated.contains(next)) {
                generated.add(next);
                result[i] = next;
                i++;
            }
        }

        return result;
    }
}