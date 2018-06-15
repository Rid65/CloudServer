import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class FileMessage extends AbstractMessage  {

    public int partCount;
    public int partNumber;
    public byte[] filedata;
    public String pathToSave;

    public FileMessage(String pathToSave, byte[] filedata) {
        this.filedata = filedata;
        this.pathToSave = pathToSave;
    }

    public FileMessage(String pathToSave, int partCount, int partNumber, byte[] filedata) {
        this.partCount = partCount;
        this.partNumber = partNumber;
        this.filedata = filedata;
        this.pathToSave = pathToSave;
    }

    public String getPathToSave() {
        return pathToSave;
    }

    public byte[] getFiledata() {
        return filedata;
    }

    public int getPartCount() {
        return partCount;
    }

    public int getPartNumber() {
        return partNumber;
    }

    public void setFiledata(byte[] filedata) {
        this.filedata = filedata;
    }
}
