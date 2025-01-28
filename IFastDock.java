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

    public void getRecordInfo(unsigned long dataNumber) {
        PDC_RECORD_INFO recordInfo;
        /*unsigned long nErrorCode;
    
        if (PFDC_GetRecordInfo(dataNumber, &recordInfo, &nErrorCode) == PDC_FAILED) {
            printf("PFDC_GetRecordInfo() Failed for data %lu. ErrorCode = %lu\n", dataNumber, nErrorCode);
            return false;
        }
    
        printf("Data Name             : %s\n", recordInfo.m_pDataName);
        printf("Device Name           : %s\n", recordInfo.m_DeviceName);
        printf("Info Version          : %u\n", recordInfo.m_nInfoVersion);
        printf("Sys Version           : %u\n", recordInfo.m_nSysVersion);
        printf("Date                  : %02x/%02x/%02x\n",
               (recordInfo.m_nDate >> 16) & 0xFF,
               (recordInfo.m_nDate >> 8) & 0xFF,
               recordInfo.m_nDate & 0xFF);
        printf("Time                  : %02x:%02x:%02x\n",
               (recordInfo.m_nTime >> 16) & 0xFF,
               (recordInfo.m_nTime >> 8) & 0xFF,
               recordInfo.m_nTime & 0xFF);
        printf("Camera Type           : %lu\n", recordInfo.m_nCameraType);
        printf("Head Type             : %u\n", recordInfo.m_nHeadType);
        printf("Head Number           : %u\n", recordInfo.m_nHeadNo);
        printf("Max Head Number       : %u\n", recordInfo.m_nMaxHeadNum);
        printf("Record Rate           : %lu fps\n", recordInfo.m_nRecordRate);
        printf("Shutter Speed         : 1/%lu sec\n", recordInfo.m_nShutterSpeed);
        printf("Trigger Mode          : %lu\n", recordInfo.m_nTriggerMode);
        printf("Manual Frames         : %lu\n", recordInfo.m_nManualFrames);
        printf("Random Frames         : %lu\n", recordInfo.m_nRandomFrames);
        printf("Random Manual Frames  : %lu\n", recordInfo.m_nRandomManualFrames);
        printf("Random Times          : %lu\n", recordInfo.m_nRandomTimes);
        printf("Two Stage Type        : %lu\n", recordInfo.m_nTwoStageType);
        printf("Two Stage LH Frame    : %lu\n", recordInfo.m_nTwoStageLHFrame);
        printf("Two Stage Cycle       : %lu\n", recordInfo.m_nTwoStageCycle);
        printf("Two Stage HL Frame    : %lu\n", recordInfo.m_nTwoStageHLFrame);
        printf("Color Temperature     : %u\n", recordInfo.m_nColorTemperature);
        printf("Color Balance (R, G, B) : %u, %u, %u\n", recordInfo.m_nColorBalanceR, recordInfo.m_nColorBalanceG, recordInfo.m_nColorBalanceB);
        printf("Color Balance Base    : %u\n", recordInfo.m_nColorBalanceBase);
        printf("Color Balance Max     : %u\n", recordInfo.m_nColorBalanceMax);
        printf("Original Total Frames : %lu\n", recordInfo.m_nOriginalTotalFrames);
        printf("Total Frames          : %lu\n", recordInfo.m_nTotalFrames);
        printf("Start Frame           : %ld\n", recordInfo.m_nStartFrame);
        printf("Trigger Frame         : %ld\n", recordInfo.m_nTriggerFrame);
        printf("Number of Events      : %lu\n", recordInfo.m_nNumOfEvent);
        
        // Display events
        for (unsigned long i = 0; i < recordInfo.m_nNumOfEvent && i < 10; ++i) {
            printf("Event %lu               : %ld\n", i, recordInfo.m_nEvent[i]);
        }
    
        printf("Resolution            : %dx%d\n", recordInfo.m_nWidth, recordInfo.m_nHeight);
        printf("Sensor Position (X, Y): %u, %u\n", recordInfo.m_nSensorPosX, recordInfo.m_nSensorPosY);
        printf("Sensor Size (Width x Height): %u x %u\n", recordInfo.m_nSensorWidth, recordInfo.m_nSensorHeight);
        printf("Color Type            : %u\n", recordInfo.m_nColorType);
        printf("Color Bits            : %u\n", recordInfo.m_nColorBits);
        printf("Effective Bit Depth   : %u\n", recordInfo.m_nEffectiveBitDepth);
        printf("Effective Bit Side    : %u\n", recordInfo.m_nEffectiveBitSide);
        printf("IRIG Mode             : %lu\n", recordInfo.m_nIRIGMode);
        printf("Shutter Type 2        : %lu\n", recordInfo.m_nShutterType2);
        
        // Color Matrix (RR, RG, RB, GR, GG, GB, BR, BG, BB)
        printf("Color Matrix (RR, RG, RB, GR, GG, GB, BR, BG, BB): ");
        printf("%d, %d, %d, %d, %d, %d, %d, %d, %d\n",
               recordInfo.m_nColorMatrixRR, recordInfo.m_nColorMatrixRG, recordInfo.m_nColorMatrixRB,
               recordInfo.m_nColorMatrixGR, recordInfo.m_nColorMatrixGG, recordInfo.m_nColorMatrixGB,
               recordInfo.m_nColorMatrixBR, recordInfo.m_nColorMatrixBG, recordInfo.m_nColorMatrixBB);
    
        printf("LUT Bits             : %u\n", recordInfo.m_nLUTBits);
        printf("Color Enhance        : %u\n", recordInfo.m_nColorEnhance);
        printf("Edge Enhance         : %u\n", recordInfo.m_nEdgeEnhance);
        printf("LUT Mode             : %lu\n", recordInfo.m_nLUTMode);
        printf("DS Shutter           : %u\n", recordInfo.m_nDSShutter);
        printf("Polarization         : %u\n", recordInfo.m_nPolarization);
        printf("Polarizer Config     : %u\n", recordInfo.m_nPolarizerConfig);
        printf("Digits of File Number: %u\n", recordInfo.m_nDigitsOfFileNumber);
        printf("Pixel Gain Base      : %u\n", recordInfo.m_nPixelGainBase);
        printf("Pixel Gain Bits      : %u\n", recordInfo.m_nPixelGainBits);
        printf("Shading Bits         : %u\n", recordInfo.m_nShadingBits);
        printf("Bayer Pattern        : %u\n", recordInfo.m_nBayerPattern);
        printf("Expose Time Number   : %u\n", recordInfo.m_nExposeTimeNum);
        printf("Rec On Cmd Times     : %u\n", recordInfo.m_nRecOnCmdTimes);
        printf("Pixel Gain Param     : %u\n", recordInfo.m_nPixelGainParam);
        printf("Reserve              : %u\n", recordInfo.m_nReserve);
        printf("Irig Sample          : %lu\n", recordInfo.m_IrigSample);
        printf("AE Exposure Time Mode: %lu\n", recordInfo.m_AEExposureTimeRecordMode);
        printf("AE Mode Set          : %u\n", recordInfo.m_AEModeSet);
        printf("DAQ Mode            : %u\n", recordInfo.m_nDAQMode);
        printf("DAQ Range            : [%lu, %lu]\n", recordInfo.m_nDAQRange[0], recordInfo.m_nDAQRange[1]);
        printf("Irig Offset          : %ld\n", recordInfo.m_IrigOffset);
        printf("Serial Number        : %llu\n", recordInfo.m_nSerialNumber);
        printf("Data Number          : %lu\n", recordInfo.m_nDataNumber);
        printf("Is Over Sync         : %lu\n", recordInfo.m_nIsOverSync);
        printf("External Others Mode : %u\n", recordInfo.m_nExtOthersMode);
        printf("Reserve 2            : %u\n", recordInfo.m_nReserve2);
        printf("Temperature Type     : %lu\n", recordInfo.m_nTemperatureType);
        printf("Acceleration Type    : %lu\n", recordInfo.m_nAccelerationType);
        printf("Pixel Gain Data Size : %lu\n", recordInfo.m_nPixelGainDataSize);
    
        return true;
    }*/

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
