package nl.obren.sokrates.sourcecode;

public class ExtensionGroup {
    private String extension;
    private int numberOfFiles;
    private int totalSizeInBytes = 0;

    public ExtensionGroup() {
    }

    public ExtensionGroup(String extension) {
        this.extension = extension;
    }

    public String getExtension() {
        return extension;
    }

    public void setExtension(String extension) {
        this.extension = extension;
    }

    public int getNumberOfFiles() {
        return numberOfFiles;
    }

    public void setNumberOfFiles(int numberOfFiles) {
        this.numberOfFiles = numberOfFiles;
    }

    public int getTotalSizeInBytes() {
        return totalSizeInBytes;
    }

    public void setTotalSizeInBytes(int totalSizeInBytes) {
        this.totalSizeInBytes = totalSizeInBytes;
    }

    @Override
    public String toString() {
        return extension + ": " + numberOfFiles + " files";
    }
}
