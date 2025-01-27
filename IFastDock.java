import ij.IJ;
import ij.ImagePlus;
import ij.plugin.PlugIn;
import com.sun.jna.*;
import com.sun.jna.ptr.*;
import javax.swing.*;
import com.sun.jna.Structure;
import java.util.List;
import java.util.Arrays;
import ij.ImagePlus;
import ij.ImageStack;
import ij.VirtualStack;
import ij.process.ShortProcessor;
import ij.process.ByteProcessor;
import ij.process.ImageProcessor;
import java.awt.image.DirectColorModel;

public class IFastDock implements PlugIn {

    // Define the PDCLIB interface for the DLL
    public interface PDCLib extends Library {
        PDCLib INSTANCE = Native.load("PDCLIB", PDCLib.class);

        int PDC_Init(IntByReference nErrorCode);

        int PFDC_GetValidDrives(IntByReference nDrives, IntByReference nErrorCode);

        int PFDC_OpenDrive(int driveNumber, IntByReference nNumOfData, IntByReference nErrorCode);

        int PFDC_CloseDrive(IntByReference nErrorCode);

        int PFDC_GetDriveName(int driveNumber, Pointer pName, IntByReference nErrorCode);

        int PFDC_GetRecordInfo(int dataIndex, Pointer pRecordInfo, IntByReference nErrorCode);

        int PFDC_GetLUTData(int dataIndex, Pointer pLUTR, Pointer pLUTG, Pointer pLUTB, IntByReference nErrorCode);

        int PFDC_GetExposeTimeData(int dataIndex, Pointer pExposeTimeData, IntByReference nErrorCode);

        int PFDC_SetDataName(int dataIndex, Pointer pName, IntByReference nErrorCode);

        int PFDC_GetImageData(int dataNumber, long frameNumber, int interleave, Pointer pImageData,
                IntByReference nErrorCode);

        int PFDC_GetIRIGInfo(int dataIndex, int frameNumber, Pointer pIRIGInfo, IntByReference nErrorCode);

        int PFDC_GetRecOnCmdTrigFrames(int dataIndex, Pointer pBuff, IntByReference nErrorCode);

        int PFDC_GetAutoExposureFps(int dataIndex, int frameNumber, Pointer pAE, IntByReference nErrorCode);

        int PFDC_GetAutoExposureNsec(int dataIndex, int frameNumber, Pointer pAE, IntByReference nErrorCode);

        int PFDC_StartImageBuffering(int dataIndex, IntByReference nErrorCode);

        void PFDC_FinishImageBuffering();

        int PFDC_GetActivityRatio(DoubleByReference pActivityRatio, IntByReference nErrorCode);

        int PFDC_GetTotalWrittenSize(LongByReference pTotalWrittenSize, IntByReference nErrorCode);

        int PFDC_FormatDrive(IntByReference nErrorCode);

        int PFDC_TestDrive(IntByReference nErrorCode);

        int PFDC_GetTemperatureType(int dataIndex, IntByReference pType, IntByReference nErrorCode);

        int PFDC_GetTemperature(int dataIndex, int frameNumber, Pointer pData, IntByReference nErrorCode);

        int PFDC_GetShutterSpeedPrecision(int dataIndex, IntByReference pValue, IntByReference nErrorCode);

        int PFDC_GetADCByteData(int dataIndex, int frameNumber, int frameNum, Pointer pData, IntByReference nErrorCode);

        int PFDC_GetAdcInfo(int dataIndex, IntByReference pNumberOfChannel, IntByReference pSamplePerFrame,
                IntByReference nErrorCode);
    }

    // Define the PDC_RECORD_INFO class as an inner class
    public static class PDC_RECORD_INFO extends Structure {
        public String dataName; // char* m_pDataName
        public int date; // unsigned long m_nDate
        public int time; // unsigned long m_nTime
        public int totalFrames; // unsigned long m_nTotalFrames
        public int width; // unsigned long m_nWidth
        public int height; // unsigned long m_nHeight
        public int recordRate; // unsigned long m_nRecordRate
        public int shutterSpeed; // unsigned long m_nShutterSpeed
        public int triggerMode; // unsigned long m_nTriggerMode
        public int colorBits; // unsigned long m_nColorBits
        public int colorType; // unsigned long m_nColorType

        @Override
        protected List<String> getFieldOrder() {
            return Arrays.asList(
                    "dataName", "date", "time", "totalFrames",
                    "width", "height", "recordRate", "shutterSpeed",
                    "triggerMode", "colorBits", "colorType");
        }
    }

    private IntByReference nErrorCode = new IntByReference(0);
    private IntByReference nDrives = new IntByReference(0);
    private IntByReference nNumOfData = new IntByReference(0);

    public boolean initialize() {
        int result = PDCLib.INSTANCE.PDC_Init(nErrorCode);
        if (result == 0) {
            showMessage("Initialization Error", "PDC_Init failed with error code: " + nErrorCode.getValue());
            return false;
        }
        return true;
    }
    // return drive name if success, -1 otherwise
    public String getDriveName(int driveNumber) {
        Pointer pName = new Memory(256); // Assuming max length 256
        int result = PDCLib.INSTANCE.PFDC_GetDriveName(driveNumber, pName, nErrorCode);
        if (result == 0) {
            showMessage("Error", "Failed to get drive name. ErrorCode: " + nErrorCode.getValue());
        } 
        return pName.getString(0);
    }
    // return num of Valid Drives if success, -1 otherwise
    public int getValidDrives() {
        int result = PDCLib.INSTANCE.PFDC_GetValidDrives(nDrives, nErrorCode);
        if (result == 0) {
            showMessage("getValidDrives", "PFDC_GetValidDrives() Failed. ErrorCode: " + nErrorCode.getValue());
            return -1;
        }
        if (nDrives.getValue() == 0) {
            showMessage("getValidDrives", "No FAST drives found.");
            return -1;
        }
        return nDrives.getValue();
    }
    // return num of data in the drive if success, -1 otherwise
    public int openDrive(int driveNumber) {
        int result = PDCLib.INSTANCE.PFDC_OpenDrive(driveNumber, nNumOfData, nErrorCode);
        if (result == 0) {
            showMessage("PFDC_OpenDrive", "PFDC_OpenDrive() Failed. ErrorCode: " + nErrorCode.getValue());
            return -1;
        }
        if (nNumOfData.getValue() == 0) {
            showMessage("PFDC_OpenDrive", "This FAST drive is formatted and contains no data.");
            return -1;
        }
        return nNumOfData.getValue();
    }
    // return Activity Ratio in the drive if success, -1 otherwise
    public int getActivityRatio() {
        DoubleByReference activityRatio = new DoubleByReference(0.0);
        int result = PDCLib.INSTANCE.PFDC_GetActivityRatio(activityRatio, nErrorCode);
        if (result == 0) {
            showMessage("PFDC_GetActivityRatio", "PFDC_GetActivityRatio() Failed. ErrorCode: " + nErrorCode.getValue());
            return -1;
        } else {
            return (int)activityRatio.getValue();
        }
    }

    public int getTotalWrittenSize() {
        LongByReference totalWrittenSize = new LongByReference(0);
        int result = PDCLib.INSTANCE.PFDC_GetTotalWrittenSize(totalWrittenSize, nErrorCode);
        if (result == 0) {
            showMessage("PFDC_GetTotalWrittenSize",
                    "PFDC_GetTotalWrittenSize() Failed, ErrorCode: " + nErrorCode.getValue());
                    return -1;
        } else {
            double sizeInGB = totalWrittenSize.getValue() / (double) (1024 * 1024 * 1024);
            return (int)sizeInGB;
        }
    }

    public boolean getRecordInfo(int dataNumber) {
        // Assuming PDC_RECORD_INFO is mapped as a JNA Structure
        PDC_RECORD_INFO recordInfo = new PDC_RECORD_INFO();
        IntByReference nErrorCode = new IntByReference(0);

        /*
         * int result = PDCLib.INSTANCE.PFDC_GetRecordInfo(dataNumber, recordInfo,
         * nErrorCode);
         * if (result == 0) { // Assuming 0 indicates failure
         * System.err.printf("PFDC_GetRecordInfo() Failed for data %d. ErrorCode = %d\n"
         * , dataNumber, nErrorCode.getValue());
         * return false;
         * }
         * 
         * // Extract and display information from the recordInfo structure
         * System.out.println("Data Name        : " + recordInfo.dataName);
         * System.out.printf("Date Time        : %02x/%02x/%02x %02x:%02x:%02x\n",
         * (recordInfo.date >> 16) & 0xFF,
         * (recordInfo.date >> 8) & 0xFF,
         * recordInfo.date & 0xFF,
         * (recordInfo.time >> 16) & 0xFF,
         * (recordInfo.time >> 8) & 0xFF,
         * recordInfo.time & 0xFF);
         * System.out.println("Total Frames     : " + recordInfo.totalFrames);
         * System.out.printf("Image Resolution : %dx%d\n", recordInfo.width,
         * recordInfo.height);
         * System.out.println("Record Rate      : " + recordInfo.recordRate + "fps");
         * System.out.println("Shutter Speed    : 1/" + recordInfo.shutterSpeed +
         * "sec");
         * System.out.println("Trigger Mode     : " + recordInfo.triggerMode);
         * System.out.println("Color Bits       : " + recordInfo.colorBits);
         * System.out.println("Color Type       : " + recordInfo.colorType);
         */
        return true;
    }

    public void showVirtualStack(int dataNumber,int width,int height,int totalFrames) {
        // Parameters for the images
        int nPlanes = 1;
        int nInterleave = 0;
        int imageSize = width * height * nPlanes * 2; // Each pixel is 2 bytes (16-bit)
        // Create a VirtualStack for dynamic image loading
        VirtualStack virtualStack = new VirtualStack(width, height, totalFrames) {
            @Override
            public ImageProcessor getProcessor(int n) {
                // `n` is 1-based index for frame
                Pointer pImageData = new Memory(imageSize);
                int frameIndex = n - 1; // Convert to 0-based index
                // Fetch the image data for the current frame
                PDCLib.INSTANCE.PFDC_GetImageData(dataNumber, frameIndex, nInterleave, pImageData, nErrorCode);
                /// String message = String.format("getProcessor dataNumber: %d,frameIndex
                /// %d,nErrorCode %s", dataNumber,frameIndex,nErrorCode.getValue());
                // showMessage2( message);
                // Convert Pointer to byte array
                byte[] imageBytes = pImageData.getByteArray(0, imageSize);
                short[] shortData = convertBytesToShorts(imageBytes);
                ShortProcessor sp = new ShortProcessor(width, height);
                sp.setPixels(shortData);
                return sp;
            }
        };

        // Add placeholder slices to the stack (ImageJ requires slices to be
        // pre-declared)
        for (int i = 0; i < totalFrames; i++) {
            virtualStack.addSlice("Frame " + (i + 1), new short[width * height]);
        }

        // Create an ImagePlus to display the virtual stack
        ImagePlus imp = new ImagePlus("Virtual Image Stack", virtualStack);
        // Show the stack
        imp.show();
    }

    public void showImageStack(int dataNumber,int width,int height,int totalFrames) {
        // Determine the color type and planes
        int nPlanes = 1;
        int nInterleave = 0;
        int imageSize = 512 * 512 * nPlanes * 2;
        Pointer pImageData = new Memory(imageSize);
        ImageStack stack = new ImageStack(width, height);
        // Assume `totalFrames` represents the number of frames to fetch
        // Loop through each frame and fetch image data
        for (int i = 0; i < totalFrames; ++i) {
            PDCLib.INSTANCE.PFDC_GetImageData(dataNumber, i, nInterleave, pImageData, nErrorCode);
            byte[] imageBytes = pImageData.getByteArray(0, imageSize);
            ShortProcessor sp = new ShortProcessor(width, height);
            sp.setPixels(convertBytesToShorts(imageBytes));
            stack.addSlice("Frame " + (i + 1), sp);
        }
        // Create and show an ImagePlus with the stack
        ImagePlus imp = new ImagePlus("Image Stack", stack);
        imp.show();
    }

    // Convert byte[] to short[]
    public short[] convertBytesToShorts(byte[] imageBytes) {
        int length = imageBytes.length / 2; // Each short is 2 bytes
        short[] shortData = new short[length];
        for (int i = 0; i < length; i++) {
            // Combine two bytes into one short (big-endian format)
            shortData[i] = (short) (((imageBytes[i * 2] & 0xFF)) | ((imageBytes[i * 2 + 1] & 0xFF) << 8));
        }
        return shortData;
    }

    public void closeDrive() {
        int result = PDCLib.INSTANCE.PFDC_CloseDrive(nErrorCode);
        if (result == 0) {
            showMessage("PFDC_CloseDrive", "PFDC_CloseDrive() Failed. ErrorCode: " + nErrorCode.getValue());
        }
    }

    @Override
    public void run(String arg) {
        int width = 512;
        int height = 512;
        int totalFrames = 10;
        initialize();
        getValidDrives();

        int selectedDrive = 0; // Select the first drive
        openDrive(selectedDrive);
        //getDriveName(0);
        //getActivityRatio();
       // getTotalWrittenSize();
        int selectedDataIndex = 1; // Replace with user-selected index if applicable
        //showImageDataInfo(selectedDataIndex);
        showVirtualStack(selectedDataIndex,width,height,totalFrames);
        //showImageStack(selectedDataIndex,width,height,totalFrames);
        //closeDrive();
    }

    private void showMessage(String title, String message) {
         JOptionPane.showMessageDialog(null, message, title,JOptionPane.INFORMATION_MESSAGE);
    }

}
