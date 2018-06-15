import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.serialization.ObjectEncoderOutputStream;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

public class Common {

    //-- Метод по переданной директории возвращает ее содержимое из файлов и папок в виде List<String>
    public static List<String> getFileList(String dir, Boolean needCut) {

        List<String> fileList = new ArrayList<>();
        Path path = Paths.get(dir);
        if (Files.notExists(path)) {
            createDir(dir);
        }
        try {
            if (needCut) {
                Files.list(path).sorted(((o1, o2) -> o1.getFileName().toString().compareTo(o2.getFileName().toString())))
                                .sorted((o1, o2) -> (Files.isDirectory(o1) ? -1 : 1))
                                .forEach(p -> fileList.add(p.getFileName().toString()));
            } else {
                Files.list(path).sorted(((o1, o2) -> o1.getFileName().toString().compareTo(o2.getFileName().toString())))
                                .sorted((o1, o2) -> (Files.isDirectory(o1) ? -1 : 1))
                                .forEach(p -> fileList.add(p.toAbsolutePath().toString()));
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
        return  fileList;
    }

    //-- Создает директорию
    public static void createDir(String path) {
        try {
            Files.createDirectory(Paths.get(path));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    //-- записывает полученный массив байт в файл.
    //-- Используется RandomAccessFile для записи байт начиная с опредленной позиции файла
    public static  void writeChunkFile(Path path, int partNumber, byte[] filedata) {
        if (filedata.length > 0) {
            RandomAccessFile raf = null;
            FileChannel channel = null;
            ByteBuffer buf = null;
            try {
                if (!Files.exists(path)) {
                    Files.createFile(path);
                }
                raf = new RandomAccessFile(path.toFile(), "rw");
                channel = raf.getChannel();
                channel.position(partNumber * Config.FILE_PART_SIZE);
                buf = ByteBuffer.allocate(filedata.length);
                buf.put(filedata);
                buf.flip();
                channel.write(buf);
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                try {
                    buf.clear();
                    channel.close();
                    raf.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        } else {
            try {
                if (!Files.exists(path)) {
                    Files.createFile(path);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    //-- Отправляет файл по частям с клиента на сервер.
    public static void sendChunkFile(Path pathToFile, String pathToSave, ObjectEncoderOutputStream encoder) {
        try {
            byte[] filedata = Files.readAllBytes(pathToFile);
            int partCount = filedata.length / Config.FILE_PART_SIZE;
            if (partCount > 0) {
                if (filedata.length % Config.FILE_PART_SIZE != 0) {
                    partCount++;
                }
                //--  в цикле последовательно отправляем каждую часть файла
                for (int i = 0; i < partCount; i++) {
                    int from = i * Config.FILE_PART_SIZE;
                    int to = (i + 1) * Config.FILE_PART_SIZE;
                    if (to >= filedata.length) {
                        to = filedata.length;
                    }
                    byte[] currentChunk = Arrays.copyOfRange(filedata, from, to);
                    FileMessage fileMessage = new FileMessage(pathToSave, partCount, i, currentChunk);
                    encoder.writeObject(fileMessage);
                }
            } else {
                FileMessage fileMessage = new FileMessage(pathToSave, 1, 0, new byte[0]);
                encoder.writeObject(fileMessage);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    //-- Отправляет файл по частям с сервера на клиент.
    public static void sendChunkFile(Path pathToFile, String pathToSave, ChannelHandlerContext ctx) {
        try {
            byte[] filedata = Files.readAllBytes(pathToFile);
            int partCount = filedata.length / Config.FILE_PART_SIZE;
            if (partCount > 0) {
                if (filedata.length % Config.FILE_PART_SIZE != 0) {
                    partCount++;
                }
                //--  в цикле последовательно отправляем каждую часть файла
                for (int i = 0; i < partCount; i++) {
                    int from = i * Config.FILE_PART_SIZE;
                    int to = (i + 1) * Config.FILE_PART_SIZE;
                    if (to >= filedata.length) {
                        to = filedata.length;
                    }
                    byte[] currentChunk = Arrays.copyOfRange(filedata, from, to);
                    FileMessage fileMessage = new FileMessage(pathToSave, partCount, i, currentChunk);
                    ctx.writeAndFlush(fileMessage);
                }
            } else {
                FileMessage fileMessage = new FileMessage(pathToSave, 1, 0, new byte[0]);
                ctx.writeAndFlush(fileMessage);
            }
        } catch (IOException e) {
            CommandMessage commandMessage = new CommandMessage(Commands.DOWNLOAD_FILE_ERROR);
            ctx.writeAndFlush(commandMessage);
            e.printStackTrace();
        }
    }

}
