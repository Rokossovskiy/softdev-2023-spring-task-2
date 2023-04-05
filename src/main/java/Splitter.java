import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.nio.file.Files;

public class Splitter {
    boolean flag_D = false; //Означает, что выходные файлы следует называть ofile1...
    boolean flag_O = false; //Pадаёт базовое имя выходного файла
    int sizeFile = 100; //Размер по умолчанию
    String inputFile; //Имя входного файла
    String outputFile = "x"; //Базовое имя выходного файла равняется “x”.
    List<String> content; //Содержимое входного файла
    Type splitType;

    enum Type {
        LINE, CHAR, FILE
    }

    public Splitter(String[] args) throws IOException {
        initialize(args);
        if (splitType == null) splitType = Type.LINE;
        File file = new File(inputFile);
        if (!file.exists()) throw new FileNotFoundException("File " + inputFile + " not found");
        content = Files.readAllLines(file.toPath(), StandardCharsets.UTF_8);
    }

    //Метод инициализирует переменные класса Splitter на основе аргументов командной строки
    private void initialize(String[] args) {
        boolean inputFileSet = false;
        for (int i = 0; i < args.length; i++) {
            String arg = args[i];
            switch (arg) {
                case "-d":
                    flag_D = true;
                    break;
                case "-l":
                case "-c":
                case "-n":
                    if (splitType != null) {
                        throw new IllegalFormatFlagsException("Multiple size-operating flags written");
                    }
                    splitType = arg.equals("-l") ? Type.LINE : arg.equals("-c") ? Type.CHAR : Type.FILE;
                    sizeFile = Integer.parseInt(args[++i]);
                    break;
                case "-o":
                    String value = args[++i];
                    if (value.equals("-")) {
                        flag_O = true;
                    } else {
                        outputFile = value;
                    }
                    break;
                default:
                    if (inputFileSet) {
                        throw new IllegalFormatFlagsException("Multiple inputFiles written");
                    }
                    inputFile = arg;
                    inputFileSet = true;
                    break;
            }
        }
        if (inputFile == null) {
            throw new IllegalFormatFlagsException("No input file specified");
        }
    }

    //Метод разбивает содержимое на список строк фиксированного размера типа LINE
    public List<String> splitLine() {
        if (splitType != Type.LINE) throw new IllegalStateException();
        List<String> result = new ArrayList<>();
        int startSize = content.size();
        for (int i = 0; i < startSize / sizeFile; i++) {
            result.add(String.join("\r\n", content.subList(0, sizeFile)));
            if (sizeFile > 0) {
                content.subList(0, sizeFile).clear();
            }
        }
        if (content.size() % sizeFile != 0) result.add(String.join("\r\n", content));
        return result;
    }

    //Метод разбивает содержимое на список строк фиксированного размера типа CHAR
    public List<String> splitChar() {
        if (splitType != Type.CHAR) throw new IllegalStateException();
        List<String> result = new ArrayList<>();
        String content = String.join("", this.content);
        int i = 0;
        while (i < content.length()) {
            result.add(content.substring(i, Math.min(i + sizeFile, content.length())));
            i += sizeFile;
        }
        return result;
    }

    //Метод разбивает содержимое на список строк фиксированного размера типа BYTE
    public byte[][] splitFile() throws IOException {
        if (splitType != Type.FILE) throw new IllegalStateException();
        File file = new File(inputFile);
        int arrayLength = (int) Math.ceil((double) file.length() / sizeFile);
        byte[][] result = new byte[sizeFile][];
        byte[] bytes = Files.readAllBytes(file.toPath());
        int length = (int) file.length() / arrayLength;
        for (int i = 0; i < length; i++) {
            result[i] = Arrays.copyOfRange(bytes, i * arrayLength, (i + 1) * arrayLength);
        }
        int del = (int) file.length() % arrayLength;
        if (del != 0) result[result.length - 1] = Arrays.copyOfRange(bytes, bytes.length - del, bytes.length);
        return result;
    }

    public void save() throws IOException {
        outputFile = (flag_O) ? inputFile : outputFile;
        if (splitType == Type.LINE || splitType == Type.CHAR) {
            List<String> files = (splitType == Type.LINE) ? splitLine() : splitChar();
            for (int i = 0; i < files.size(); i++) {
                File file = new File(outputFile + getPostfix(i));
                FileWriter wr = new FileWriter(file);
                wr.write(files.get(i));
                wr.close();
            }
        } else {
            byte[][] files = splitFile();
            for (int i = 0; i < files.length; i++) {
                File file = new File(outputFile + getPostfix(i));
                FileOutputStream wr = new FileOutputStream(file);
                wr.write(files[i]);
                wr.close();
            }
        }
    }

    //Метод возвращает строку, которая является постфиксом для заданного целочисленного значения.
    private String getPostfix(int i) {
        String chars = "qwertyuiopasdfghjklzxcvbnm";
        if (flag_D) {
            return String.valueOf(i);
        }
        StringBuilder postfix = new StringBuilder();
        do {
            postfix.append(chars.charAt(i % 26));
            i /= 26;
        } while (i > 0);
        return postfix.reverse().toString();
    }

}