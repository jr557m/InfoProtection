import java.io.*;
import java.nio.charset.Charset;

public class Main {
    public static void main(String[] args) {
        int numDocuments = 10; // Число генерируемых документов за цикл
        int maxIterations = 50; // Максимальное число итераций цикла
        String inputFile = new java.io.File("leasing.txt").getPath();
        String inputSHA = generateSHA1(inputFile, true);
        
        for (int iter = 0; iter < maxIterations; iter++) {
            try (BufferedReader br = new BufferedReader(new FileReader(inputFile, Charset.forName("UTF-8")))) {
                String inputText = "";
                String line;
                while ((line = br.readLine()) != null) { // Построчно считываем входной файл
                    inputText += line + "\n";
                }
              

                for (int i = 1; i <= numDocuments; i++) {
                    int numReplacements = (int) (Math.random() * 100) + 1;
                    String documentText = inputText;
                    for (int j = 1; j <= numReplacements; j++) {
                        int index = documentText.indexOf("\s",
                                (int) (Math.random() * documentText.length()) + 1); 
                        if (index != -1) {
                            documentText = documentText.substring(0, index) + "\s\s\b"
                                    + documentText.substring(index + 1);
                        }
                    }

                    numReplacements = (int) (Math.random() * 10) + 1; // Количество замен s-s-b на s-b-s
                    // Заменяем случайные послед-ти s-s-b в случайных местах на послед-ти s-b-s
                    for (int j = 1; j <= numReplacements; j++) {
                        int index = documentText.indexOf("\s\s\b",
                                (int) (Math.random() * documentText.length()) + 1); 
                        if (index != -1) {
                            documentText = documentText.substring(0, index) + "\s\b\s"
                                    + documentText.substring(index + 3);
                        }
                    }

                    // Записываем результат в файл
                    String outputFile = new java.io.File("res\\leasing_" + i + ".txt").getPath();
                    BufferedWriter writer = new BufferedWriter(new FileWriter(outputFile, Charset.forName("UTF-8")));
                    writer.write(documentText);
                    writer.close();
                    System.out.print(iter * 10 + i + " ");
                    if (inputSHA.equals(generateSHA1(outputFile, false))) {
                        System.out.println("Обнаружена коллизия с " + i + "-м документом!\n");
                        generateSHA1(inputFile, true);
                        generateSHA1(outputFile, true);
                        maxIterations = 0;
                        break;
                    } 
                }

                br.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

    }

    private static String generateSHA1(String filePath, boolean print) {
        try {
            ProcessBuilder pb = new ProcessBuilder("cmd.exe", "/c", "openssl dgst -sha1 " + filePath);
            Process process = pb.start();
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));

            String line, result = "";
            while ((line = reader.readLine()) != null) {
                if (print)
                    System.out.println(line);
                result = line.split("= ")[1];
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
}