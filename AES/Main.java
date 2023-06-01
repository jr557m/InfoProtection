import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;

public class Main {
    public static void main(String[] args) {
        String inputFilePath = new File("tux.bmp").getPath();
        String bytesTempFile = new File("imageBytes.tmp").getPath();
        String bytesEncryptedTempFile = new File("imageBytesEncrypted.tmp").getPath();
        String[] modes = { "ecb", "cbc", "cfb", "ofb" };
        String key = generateKey();

        for (String mode : modes) {
            String outputFilePath = new File("tux-" + mode + ".bmp").getPath();
            String decryptedFilePath = new File("tux-" + mode + "-decrypted.bmp").getPath();

            aesCipherEnc(inputFilePath, bytesTempFile, bytesEncryptedTempFile, outputFilePath, mode, key);
            aesCipherDec(outputFilePath, bytesTempFile, bytesEncryptedTempFile, decryptedFilePath, mode, key);
        }
    }

    private static void aesCipherEnc(String inputFilePath, String tmpFilePath, String encTmpFilePath,
            String outputFilePath, String mode, String key) {
        try (FileInputStream fis = new FileInputStream(inputFilePath)) {
            byte[] header = new byte[110]; // Заголовок
            fis.read(header);
            byte[] imageBytes = new byte[fis.available()]; // Байты пикселей
            fis.read(imageBytes);
            fis.close();

            FileOutputStream tmpBytesFos = new FileOutputStream(tmpFilePath);
            tmpBytesFos.write(imageBytes); // Записываем байты во временный файл отдельно от заголовка
            tmpBytesFos.close();

            opensslEnc(tmpFilePath, encTmpFilePath, mode, key, false); // Шифруем байты без заголовка

            FileInputStream tmpBytesFis = new FileInputStream(encTmpFilePath);
            byte[] processedImageBytes = new byte[tmpBytesFis.available()];
            tmpBytesFis.read(processedImageBytes); // Извлекаем обработанные байты
            tmpBytesFis.close();

            FileOutputStream fos = new FileOutputStream(outputFilePath);
            fos.write(header); // Незашифрованный заголовок
            fos.write(processedImageBytes); // Обработанные байты
            fos.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void aesCipherDec(String inputFilePath, String tmpFilePath, String encTmpFilePath,
        String outputFilePath, String mode, String key) {
    try (FileInputStream fis = new FileInputStream(inputFilePath)) {
        byte[] header = new byte[110]; // Заголовок
        fis.read(header);
        byte[] imageBytes = new byte[fis.available()]; // Байты пикселей
        fis.read(imageBytes);
        fis.close();

        FileOutputStream tmpBytesFos = new FileOutputStream(tmpFilePath);
        tmpBytesFos.write(imageBytes); // Записываем байты во временный файл отдельно от заголовка
        tmpBytesFos.close();

        opensslEnc(tmpFilePath, encTmpFilePath, mode, key, true); // Дешифруем байты без заголовка

        FileInputStream tmpBytesFis = new FileInputStream(encTmpFilePath);
        byte[] processedImageBytes = new byte[tmpBytesFis.available()];
        tmpBytesFis.read(processedImageBytes); // Извлекаем обработанные байты
        tmpBytesFis.close();

        FileOutputStream fos = new FileOutputStream(outputFilePath);
        fos.write(header); // Незашифрованный заголовок
        fos.write(processedImageBytes); // Обработанные байты
        fos.close();
    } catch (IOException e) {
        e.printStackTrace();
    }
    }

    private static void opensslEnc(String inputFilePath, String outputFilePath, String method, String key,
            boolean decrypt) {
        try {
            ProcessBuilder pb = new ProcessBuilder("cmd.exe", "/c",
                    "openssl enc " + (decrypt ? "-d" : "") + " -aes-256-" + method +
                            " -in " + inputFilePath + " -out " + outputFilePath + " -pass pass:" + key);
            Process process = pb.start();
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            String line, result = "";
            while ((line = reader.readLine()) != null) {
                result = line;
                System.out.println(result);
            }

            int exitCode = process.waitFor();
            if (exitCode != 0) {
                System.err.println("Error: " + exitCode);
            }
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }
    }

    private static String generateKey() {
        try {
            ProcessBuilder pb = new ProcessBuilder("cmd.exe", "/c", "openssl rand -hex 16");
            Process process = pb.start();
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            String result = reader.readLine();

            System.out.println("Key: " + result);

            int exitCode = process.waitFor();
            if (exitCode != 0) {
                System.err.println("Error: " + exitCode);
            }
            return result;
        } catch (IOException | InterruptedException e) {
            return e.toString();
        }
    }
}
