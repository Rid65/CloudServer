import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.util.ReferenceCountUtil;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.*;
import java.util.ArrayList;
import java.util.List;

public class CloudServerHandler extends ChannelInboundHandlerAdapter {

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        //System.out.println("Client connected...");
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        processMessage(ctx, msg);
    }

    @Override
    public void channelReadComplete(ChannelHandlerContext ctx) throws Exception {
        ctx.flush();
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        cause.printStackTrace();
        ctx.close();
    }

    /*
     * Метод обрабатывает сообщение от клиента. Проверяет тип пришедщего объекта и решает, с ним делать
     **/
    private void processMessage(ChannelHandlerContext ctx, Object msg) {
        try {
            if (msg == null) {
                return;
            } else if (msg instanceof AuthMessage) {
                AuthMessage message = (AuthMessage) msg;
                doAuthorized(ctx, message);
            } else if (msg instanceof FileMessage) {
                FileMessage message = (FileMessage) msg;
                processFileMessage(ctx, message);
            } else if (msg instanceof CommandMessage) {
                CommandMessage message = (CommandMessage) msg;
                processCommandMessage(ctx, message);
            }
        } finally {
            ReferenceCountUtil.release(msg);
        }
    }

    private void doAuthorized(ChannelHandlerContext ctx, AuthMessage msg) {
        String username = msg.getUsername();
        String pass = msg.getPass();
        if (DBHelper.checkUser(username, pass)) {
            AbstractMessage command = new CommandMessage(Commands.AUTH_OK, username, getUserDirTree(username));
            ctx.writeAndFlush(command);
        } else {
            AbstractMessage command = new CommandMessage(Commands.AUTH_ERROR);
            ctx.writeAndFlush(command);
        }
    }

    private List<String> getUserDirTree(String username) {
        return Common.getFileList(Config.rootCloudServer + "\\" + username, true);
    }

    private void processFileMessage(ChannelHandlerContext ctx, FileMessage msg) {
        Path path = Paths.get(msg.getPathToSave());
        Common.writeChunkFile(path, msg.getPartNumber(), msg.getFiledata());
        if (msg.getPartNumber() == msg.getPartCount() - 1) {
            CommandMessage commandMessage = new CommandMessage(Commands.FILE_TRANSFER_SUCCESS);
            ctx.writeAndFlush(commandMessage);
        }
    }

    private void processCommandMessage(ChannelHandlerContext ctx, CommandMessage msg) {
        if (msg.getCommand() == Commands.GET_CLOUD_DIR_TREE) { // получить дерево файлов конкретной директории
            Path path = Paths.get((String) msg.getParameter(0));

            if (Files.exists(path) && Files.isDirectory(path)) {
                Path pathRoot = Paths.get(Config.rootCloudServer);
                String subpath = "";
                if (pathRoot.getNameCount() + 1 < path.getNameCount()) {
                    subpath = path.subpath(Paths.get(Config.rootCloudServer).getNameCount() + 1, path.getNameCount()).toString();
                    subpath = "\\" + subpath;
                }
                List<String> fileList = Common.getFileList((String) msg.getParameter(0), true);
                CommandMessage commandMessage = new CommandMessage(Commands.GET_CLOUD_DIR_TREE, fileList, subpath);
                ctx.writeAndFlush(commandMessage);
            }
        } else if (msg.getCommand() == Commands.DELETE_FILE) { // удалить файл на сервере
            Path pathToFile = Paths.get((String) msg.getParameter(0));
            if (Files.exists(pathToFile)) {
                try {
                    Files.delete(pathToFile);
                    CommandMessage commandMessage = new CommandMessage(Commands.DELETE_FILE_SUCCESS);
                    ctx.writeAndFlush(commandMessage);
                } catch (IOException e) {
                    CommandMessage commandMessage = new CommandMessage(Commands.DELETE_FILE_ERROR);
                    ctx.writeAndFlush(commandMessage);
                    e.printStackTrace();
                }
            }
        } else if (msg.getCommand() == Commands.DOWNLOAD_FILE) { // загрузить файл с сервера
            Path pathToFile = Paths.get((String) msg.getParameter(0));
            String pathToSave = (String) msg.getParameter(1);
            if (Files.exists(pathToFile)) {
                Common.sendChunkFile(pathToFile, pathToSave, ctx);
            }
        }
    }
}
