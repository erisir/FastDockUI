import ij.plugin.frame.PlugInFrame;
import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
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
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

public class IFastDockUI extends PlugInFrame {
    // UI
    JFrame mainFrame = null;
    JLabel deviceNameLabel = null;
    JLabel dataNameLabel = null;
    JLabel dataRecordTimeLabel = null;
    JLabel dataTotalFramesLabel = null;
    JLabel dataShutterSpeedLabel = null;
    JLabel dataResolutionLabel = null;
    JLabel sensorSizeLabel = null;
    JLabel driveNameLabel = null;
    JLabel activityRatioLabel = null;
    JLabel totalWrittenLabel = null;
    JList<String> dataList = null;
    JList<String> driveList = null;
    DefaultListModel<String> dataListModel = null;

    public void cleanup() {
        // Custom cleanup logic before the frame is closed
        closeDrive();

    }

    public IFastDockUI() {
        super("Drive Data Viewer");
        // FastDock
        initialize();
        int validDrivesNum = getValidDrives();
        // Main Frame
        mainFrame = new JFrame("Drive Data Viewer");
        mainFrame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        mainFrame.setSize(600, 500);
        mainFrame.addWindowListener(new java.awt.event.WindowAdapter() {
            @Override
            public void windowClosing(java.awt.event.WindowEvent e) {
                cleanup();
            }
        });
        // Panel Layout
        JPanel mainPanel = new JPanel();
        mainPanel.setLayout(new GridLayout(2, 1));

        // Upper Panel (Drive Selection and Info)
        JPanel upperPanel = new JPanel();
        upperPanel.setLayout(new GridLayout(1, 2));

        // Drive List
        JPanel driveListPanel = new JPanel();
        driveListPanel.setLayout(new BorderLayout());
        JLabel driveListLabel = new JLabel("Valid Drives:");
        DefaultListModel<String> driveListModel = new DefaultListModel<>();
        for (int i = 0; i < validDrivesNum; i++) {
            driveListModel.addElement(getDriveName(i));
        }

        driveList = new JList<>(driveListModel);
        JScrollPane driveScrollPane = new JScrollPane(driveList);
        driveListPanel.add(driveListLabel, BorderLayout.NORTH);
        driveListPanel.add(driveScrollPane, BorderLayout.CENTER);

        // Drive Info
        JPanel driveInfoPanel = new JPanel();
        driveInfoPanel.setLayout(new GridLayout(3, 1));
        driveNameLabel = new JLabel("Selected Drive: ");
        activityRatioLabel = new JLabel("Activity Ratio: ");
        totalWrittenLabel = new JLabel("Total Written Size: ");
        driveInfoPanel.add(driveNameLabel);
        driveInfoPanel.add(activityRatioLabel);
        driveInfoPanel.add(totalWrittenLabel);

        upperPanel.add(driveListPanel);
        upperPanel.add(driveInfoPanel);
  
        // Lower Panel (Data List and Details)
        JPanel lowerPanel = new JPanel();
        lowerPanel.setLayout(new GridLayout(1, 2));

        // Data List
        JPanel dataListPanel = new JPanel();
        dataListPanel.setLayout(new BorderLayout());
        JLabel dataListLabel = new JLabel("Drive Data:");
        dataListModel = new DefaultListModel<>();
        dataListModel.addElement("Data 1");
        dataList = new JList<>(dataListModel);
        JScrollPane dataScrollPane = new JScrollPane(dataList);
        dataListPanel.add(dataListLabel, BorderLayout.NORTH);
        dataListPanel.add(dataScrollPane, BorderLayout.CENTER);
        
        // Details
        JPanel dataDetailsPanel = new JPanel();
        dataDetailsPanel.setLayout(new BorderLayout());
        
        JLabel detailsListLabel = new JLabel("Data Info:");
        // Data Info
        JPanel dataInfoPanel = new JPanel();
        dataInfoPanel.setLayout(new GridLayout(7, 1));

        deviceNameLabel = new JLabel("");
        dataNameLabel = new JLabel("");
        dataRecordTimeLabel = new JLabel("");
        dataTotalFramesLabel = new JLabel("");
        dataShutterSpeedLabel = new JLabel("");
        dataResolutionLabel = new JLabel("");
        sensorSizeLabel = new JLabel("");

        dataInfoPanel.add(deviceNameLabel);
        dataInfoPanel.add(dataNameLabel);
        dataInfoPanel.add(dataRecordTimeLabel);
        dataInfoPanel.add(dataTotalFramesLabel);
        dataInfoPanel.add(dataShutterSpeedLabel);
        dataInfoPanel.add(dataResolutionLabel);
        dataInfoPanel.add(sensorSizeLabel);

        dataDetailsPanel.add(detailsListLabel, BorderLayout.NORTH);
        dataDetailsPanel.add(dataInfoPanel, BorderLayout.CENTER);

        lowerPanel.add(dataListPanel);
        lowerPanel.add(dataDetailsPanel);
   

        // Input Panel (Image Dimension Inputs)
        JPanel inputPanel = new JPanel();
        inputPanel.setLayout(new GridLayout(4, 2));
        JLabel widthLabel = new JLabel("Width:");
        JTextField widthField = new JTextField();
        JLabel heightLabel = new JLabel("Height:");
        JTextField heightField = new JTextField();
        JLabel frameLabel = new JLabel("Total Frames:");
        JTextField frameField = new JTextField();
        JButton submitButton = new JButton("Create Virtual Stack");

        inputPanel.add(widthLabel);
        inputPanel.add(widthField);
        inputPanel.add(heightLabel);
        inputPanel.add(heightField);
        inputPanel.add(frameLabel);
        inputPanel.add(frameField);
        inputPanel.add(new JLabel()); // Empty space
        inputPanel.add(submitButton);

        mainPanel.add(upperPanel);
        mainPanel.add(lowerPanel);

        mainFrame.add(mainPanel, BorderLayout.CENTER);
        mainFrame.add(inputPanel, BorderLayout.SOUTH);

        mainFrame.setVisible(true);

        // Listeners
        driveList.addListSelectionListener(e -> {
            OnDriveListClick();
        });

        dataList.addListSelectionListener(e -> {
            OnDataListClick();
        });

        submitButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                int width = Integer.parseInt(widthField.getText());
                int height = Integer.parseInt(heightField.getText());
                int frames = Integer.parseInt(frameField.getText());
                showVirtualStack(dataList.getSelectedIndex(), width, height, frames);
            }
        });
    }

    public void OnDataListClick() {
        String selectedData = dataList.getSelectedValue();
        int idx = dataList.getSelectedIndex();
        if (selectedData != null) {
            PDCRecordInfo recordInfo = getRecordInfo(idx);
            if (recordInfo != null) {
                deviceNameLabel.setText("Camera " + recordInfo.getDriveName());
                dataNameLabel.setText(selectedData);
                dataRecordTimeLabel.setText("Record Time " + recordInfo.getRecordTime());
                dataTotalFramesLabel.setText("Record Total Frames  " + recordInfo.getRecordTotalFrame());
                dataShutterSpeedLabel.setText("Record Shutter Speed  " + recordInfo.getShutterSpeed());
                dataResolutionLabel.setText("Resolution  " + recordInfo.getResolution());
                sensorSizeLabel.setText("Sensor Size  " + recordInfo.getSensorSize());
            }

        }
    }

    public void OnDriveListClick() {
        String selectedDrive = driveList.getSelectedValue();
        int idx = driveList.getSelectedIndex();
        if (selectedDrive != null) {
            int nNumOfData = openDrive(idx);
            driveNameLabel.setText("Selected Drive: " + selectedDrive);
            activityRatioLabel.setText("Activity Ratio: " + getActivityRatio());
            totalWrittenLabel.setText("Total Written Size: " + getTotalWrittenSize());

            dataListModel.clear();
            for (int data_id = 1; data_id <= nNumOfData; ++data_id) {
                /*
                 * if (PDCLib.INSTANCE.PFDC_GetRecordInfo(i, recordInfo, nErrorCode) == 0) {
                 * showMessage("Error", "Failed to get drive name. ErrorCode: " +
                 * nErrorCode.getValue());
                 * continue;
                 * }
                 * String dataName= recordInfo.m_pDataName;
                 */
                PDCRecordInfo recordInfo = getRecordInfo(data_id);
                if (recordInfo != null) {
                    dataListModel.addElement(recordInfo.getRecordName());
                }

            }
        }
    }

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
    // Define the Java structure equivalent to the C struct
    public static class PDCRecordInfo extends Structure {
        public static final int PDC_MAX_STRING_LENGTH = 256;

        public short m_nInfoVersion;
        public short m_nSysVersion;
        public int m_nDate; // unsigned long
        public int m_nTime; // unsigned long
        public int m_nCameraType; // unsigned long
        public short m_nHeadType;
        public short m_nHeadNo;
        public short m_nMaxHeadNum;
        public byte[] m_DeviceName = new byte[PDC_MAX_STRING_LENGTH];
        public int m_nRecordRate;
        public int m_nShutterSpeed;
        public int m_nTriggerMode;
        public int m_nManualFrames;
        public int m_nRandomFrames;
        public int m_nRandomManualFrames;
        public int m_nRandomTimes;
        public int m_nTwoStageType;
        public int m_nTwoStageLHFrame;
        public int m_nTwoStageCycle;
        public int m_nTwoStageHLFrame;
        public short m_nColorTemperature;
        public short m_nColorBalanceR;
        public short m_nColorBalanceG;
        public short m_nColorBalanceB;
        public short m_nColorBalanceBase;
        public short m_nColorBalanceMax;
        public int m_nOriginalTotalFrames;
        public int m_nTotalFrames;
        public int m_nStartFrame; // long
        public int m_nTriggerFrame; // long
        public int m_nNumOfEvent; // unsigned long
        public int[] m_nEvent = new int[10]; // long[10]
        public short m_nWidth;
        public short m_nHeight;
        public short m_nSensorPosX;
        public short m_nSensorPosY;
        public short m_nSensorWidth;
        public short m_nSensorHeight;
        public short m_nColorType;
        public short m_nColorBits;
        public short m_nEffectiveBitDepth;
        public short m_nEffectiveBitSide;
        public int m_nIRIGMode;
        public int m_nShutterType2;
        public short m_nColorMatrixRR;
        public short m_nColorMatrixRG;
        public short m_nColorMatrixRB;
        public short m_nColorMatrixGR;
        public short m_nColorMatrixGG;
        public short m_nColorMatrixGB;
        public short m_nColorMatrixBR;
        public short m_nColorMatrixBG;
        public short m_nColorMatrixBB;
        public short m_nLUTBits;
        public short m_nColorEnhance;
        public short m_nEdgeEnhance;
        public int m_nLUTMode;
        public short m_nDSShutter;
        public short m_nPolarization;
        public short m_nPolarizerConfig;
        public short m_nDigitsOfFileNumber;
        public short m_nPixelGainBase;
        public short m_nPixelGainBits;
        public short m_nShadingBits;
        public short m_nBayerPattern;
        public short m_nExposeTimeNum;
        public short m_nRecOnCmdTimes;
        public short m_nPixelGainParam;
        public short m_nReserve;
        public byte[] m_pDataName = new byte[64];
        public int m_IrigSample; // unsigned long
        public int m_AEExposureTimeRecordMode;
        public short m_AEModeSet;
        public short m_nDAQMode;
        public int[] m_nDAQRange = new int[2];
        public int m_IrigOffset;
        public long m_nSerialNumber; // unsigned long long
        public int m_nDataNumber;
        public int m_nIsOverSync;
        public short m_nExtOthersMode;
        public short m_nReserve2;
        public int m_nTemperatureType;
        public int m_nAccelerationType;
        public int m_nPixelGainDataSize;

        // Constructor for use with a Pointer
        public PDCRecordInfo(Pointer pointer) {
            super(pointer);
            read(); // Read the data from the pointer into this structure
        }

        // Default constructor
        public PDCRecordInfo() {
            super();
        }

        public String getDriveName() {
            return Native.toString(this.m_DeviceName);
        }

        public String getRecordName() {
            return Native.toString(this.m_pDataName);
        }

        public String getRecordTime() {
            return String.format("%02x/%02x/%02x %02x:%02x:%02x", (this.m_nDate >> 16) & 0xFF,
                    (this.m_nDate >> 8) & 0xFF, this.m_nDate & 0xFF, (this.m_nTime >> 16) & 0xFF,
                    (this.m_nTime >> 8) & 0xFF, this.m_nTime & 0xFF);
        }

        public String getShutterSpeed() {
            return String.format("1/%d sec", this.m_nShutterSpeed);
        }

        public String getRecordTotalFrame() {
            return String.format("%d", this.m_nTotalFrames);
        }

        public String getResolution() {
            return String.format("%d x %d", this.m_nWidth, this.m_nHeight);
        }

        public String getSensorSize() {
            return String.format("%d x %d", this.m_nSensorWidth, this.m_nSensorHeight);
        }

        public String getBitDepth() {
            return String.format("%d", this.m_nEffectiveBitDepth);
        }

        @Override
        protected List<String> getFieldOrder() {
            return Arrays.asList(
                    "m_nInfoVersion", "m_nSysVersion", "m_nDate", "m_nTime", "m_nCameraType",
                    "m_nHeadType", "m_nHeadNo", "m_nMaxHeadNum", "m_DeviceName",
                    "m_nRecordRate", "m_nShutterSpeed", "m_nTriggerMode", "m_nManualFrames",
                    "m_nRandomFrames", "m_nRandomManualFrames", "m_nRandomTimes",
                    "m_nTwoStageType", "m_nTwoStageLHFrame", "m_nTwoStageCycle", "m_nTwoStageHLFrame",
                    "m_nColorTemperature", "m_nColorBalanceR", "m_nColorBalanceG", "m_nColorBalanceB",
                    "m_nColorBalanceBase", "m_nColorBalanceMax", "m_nOriginalTotalFrames", "m_nTotalFrames",
                    "m_nStartFrame", "m_nTriggerFrame", "m_nNumOfEvent", "m_nEvent",
                    "m_nWidth", "m_nHeight", "m_nSensorPosX", "m_nSensorPosY", "m_nSensorWidth",
                    "m_nSensorHeight", "m_nColorType", "m_nColorBits", "m_nEffectiveBitDepth",
                    "m_nEffectiveBitSide", "m_nIRIGMode", "m_nShutterType2",
                    "m_nColorMatrixRR", "m_nColorMatrixRG", "m_nColorMatrixRB",
                    "m_nColorMatrixGR", "m_nColorMatrixGG", "m_nColorMatrixGB",
                    "m_nColorMatrixBR", "m_nColorMatrixBG", "m_nColorMatrixBB",
                    "m_nLUTBits", "m_nColorEnhance", "m_nEdgeEnhance", "m_nLUTMode",
                    "m_nDSShutter", "m_nPolarization", "m_nPolarizerConfig",
                    "m_nDigitsOfFileNumber", "m_nPixelGainBase", "m_nPixelGainBits",
                    "m_nShadingBits", "m_nBayerPattern", "m_nExposeTimeNum", "m_nRecOnCmdTimes",
                    "m_nPixelGainParam", "m_nReserve", "m_pDataName", "m_IrigSample",
                    "m_AEExposureTimeRecordMode", "m_AEModeSet", "m_nDAQMode", "m_nDAQRange",
                    "m_IrigOffset", "m_nSerialNumber", "m_nDataNumber", "m_nIsOverSync",
                    "m_nExtOthersMode", "m_nReserve2", "m_nTemperatureType", "m_nAccelerationType",
                    "m_nPixelGainDataSize");
        }
    }

    private IntByReference nErrorCode = new IntByReference(0);
    private IntByReference nDrives = new IntByReference(0);
    private IntByReference nNumOfData = new IntByReference(0);

    public boolean initialize() {
        int result = PDCLib.INSTANCE.PDC_Init(nErrorCode);
        if (result == 0) {
            if (nErrorCode.getValue() == 7) {
                return true;
            } else {
                showMessage("Initialization Error", "PDC_Init failed with error code: " + nErrorCode.getValue());
                return false;
            }
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
            if (nErrorCode.getValue() == 107) {
                return nNumOfData.getValue();
            } else {
                showMessage("PFDC_OpenDrive", "PFDC_OpenDrive() Failed. ErrorCode: " + nErrorCode.getValue());
                return -1;
            }
        }
        if (nNumOfData.getValue() == 0) {
            showMessage("PFDC_OpenDrive", "This FAST drive is formatted and contains no data.");
            return -1;
        }
        return nNumOfData.getValue();
    }

    // return Activity Ratio in the drive if success, -1 otherwise
    public String getActivityRatio() {
        DoubleByReference activityRatio = new DoubleByReference(0.0);
        int result = PDCLib.INSTANCE.PFDC_GetActivityRatio(activityRatio, nErrorCode);
        if (result == 0) {
            showMessage("PFDC_GetActivityRatio", "PFDC_GetActivityRatio() Failed. ErrorCode: " + nErrorCode.getValue());
            return "";
        } else {
            return String.format("%.2f", activityRatio.getValue());
        }
    }

    public String getTotalWrittenSize() {
        LongByReference totalWrittenSize = new LongByReference(0);
        int result = PDCLib.INSTANCE.PFDC_GetTotalWrittenSize(totalWrittenSize, nErrorCode);
        if (result == 0) {
            showMessage("PFDC_GetTotalWrittenSize",
                    "PFDC_GetTotalWrittenSize() Failed, ErrorCode: " + nErrorCode.getValue());
            return "";
        } else {
            double sizeInGB = totalWrittenSize.getValue() / (double) (1024 * 1024 * 1024);
            return String.format("%.2f", sizeInGB);
        }
    }

    public PDCRecordInfo getRecordInfo(int dataNumber) {
        // Allocate memory for the struct
        int PDC_RECORD_INFO_SIZE = 600;
        Pointer recordInfoPointer = new Memory(PDC_RECORD_INFO_SIZE);
        // Call native method to fetch the record info
        int result = PDCLib.INSTANCE.PFDC_GetRecordInfo(dataNumber, recordInfoPointer, nErrorCode);
        if (result == 0) {
            return null;
        }
        PDCRecordInfo recordInfo = new PDCRecordInfo(recordInfoPointer);
        // Parse and display the data from the struct
        return recordInfo;
    }

    private void displayRecordInfo(Pointer recordInfoPointer) {
        // Retrieve and display individual fields from the struct
        String dataName = recordInfoPointer.getString(0x100); // Example offset for `m_pDataName`
        String deviceName = recordInfoPointer.getString(0x140); // Example offset for `m_DeviceName`
        int infoVersion = recordInfoPointer.getShort(0x00); // Example offset for `m_nInfoVersion`
        int sysVersion = recordInfoPointer.getShort(0x02); // Example offset for `m_nSysVersion`

        // Example: Parse date and time fields
        long date = recordInfoPointer.getInt(0x04); // Offset for `m_nDate`
        int year = (int) ((date >> 16) & 0xFF);
        int month = (int) ((date >> 8) & 0xFF);
        int day = (int) (date & 0xFF);

        long time = recordInfoPointer.getInt(0x08); // Offset for `m_nTime`
        int hour = (int) ((time >> 16) & 0xFF);
        int minute = (int) ((time >> 8) & 0xFF);
        int second = (int) (time & 0xFF);

        System.out.printf("Data Name             : %s%n", dataName);
        System.out.printf("Device Name           : %s%n", deviceName);
        System.out.printf("Info Version          : %d%n", infoVersion);
        System.out.printf("Sys Version           : %d%n", sysVersion);
        System.out.printf("Date                  : %02d/%02d/%02d%n", year, month, day);
        System.out.printf("Time                  : %02d:%02d:%02d%n", hour, minute, second);

        // Continue extracting other fields in a similar manner
        // Use correct offsets for the specific field as per the struct layout
    }

    /*
     * public void getRecordInfo(unsigned long dataNumber) {
     * PDC_RECORD_INFO recordInfo;
     * unsigned long nErrorCode;
     * 
     * if (PFDC_GetRecordInfo(dataNumber, &recordInfo, &nErrorCode) == PDC_FAILED) {
     * printf("PFDC_GetRecordInfo() Failed for data %lu. ErrorCode = %lu\n",
     * dataNumber, nErrorCode);
     * return false;
     * }
     * 
     * printf("Data Name             : %s\n", recordInfo.m_pDataName);
     * printf("Device Name           : %s\n", recordInfo.m_DeviceName);
     * printf("Info Version          : %u\n", recordInfo.m_nInfoVersion);
     * printf("Sys Version           : %u\n", recordInfo.m_nSysVersion);
     * printf("Date                  : %02x/%02x/%02x\n",
     * (recordInfo.m_nDate >> 16) & 0xFF,
     * (recordInfo.m_nDate >> 8) & 0xFF,
     * recordInfo.m_nDate & 0xFF);
     * printf("Time                  : %02x:%02x:%02x\n",
     * (recordInfo.m_nTime >> 16) & 0xFF,
     * (recordInfo.m_nTime >> 8) & 0xFF,
     * recordInfo.m_nTime & 0xFF);
     * printf("Camera Type           : %lu\n", recordInfo.m_nCameraType);
     * printf("Head Type             : %u\n", recordInfo.m_nHeadType);
     * printf("Head Number           : %u\n", recordInfo.m_nHeadNo);
     * printf("Max Head Number       : %u\n", recordInfo.m_nMaxHeadNum);
     * printf("Record Rate           : %lu fps\n", recordInfo.m_nRecordRate);
     * printf("Shutter Speed         : 1/%lu sec\n", recordInfo.m_nShutterSpeed);
     * printf("Trigger Mode          : %lu\n", recordInfo.m_nTriggerMode);
     * printf("Manual Frames         : %lu\n", recordInfo.m_nManualFrames);
     * printf("Random Frames         : %lu\n", recordInfo.m_nRandomFrames);
     * printf("Random Manual Frames  : %lu\n", recordInfo.m_nRandomManualFrames);
     * printf("Random Times          : %lu\n", recordInfo.m_nRandomTimes);
     * printf("Two Stage Type        : %lu\n", recordInfo.m_nTwoStageType);
     * printf("Two Stage LH Frame    : %lu\n", recordInfo.m_nTwoStageLHFrame);
     * printf("Two Stage Cycle       : %lu\n", recordInfo.m_nTwoStageCycle);
     * printf("Two Stage HL Frame    : %lu\n", recordInfo.m_nTwoStageHLFrame);
     * printf("Color Temperature     : %u\n", recordInfo.m_nColorTemperature);
     * printf("Color Balance (R, G, B) : %u, %u, %u\n", recordInfo.m_nColorBalanceR,
     * recordInfo.m_nColorBalanceG, recordInfo.m_nColorBalanceB);
     * printf("Color Balance Base    : %u\n", recordInfo.m_nColorBalanceBase);
     * printf("Color Balance Max     : %u\n", recordInfo.m_nColorBalanceMax);
     * printf("Original Total Frames : %lu\n", recordInfo.m_nOriginalTotalFrames);
     * printf("Total Frames          : %lu\n", recordInfo.m_nTotalFrames);
     * printf("Start Frame           : %ld\n", recordInfo.m_nStartFrame);
     * printf("Trigger Frame         : %ld\n", recordInfo.m_nTriggerFrame);
     * printf("Number of Events      : %lu\n", recordInfo.m_nNumOfEvent);
     * 
     * // Display events
     * for (unsigned long i = 0; i < recordInfo.m_nNumOfEvent && i < 10; ++i) {
     * printf("Event %lu               : %ld\n", i, recordInfo.m_nEvent[i]);
     * }
     * 
     * printf("Resolution            : %dx%d\n", recordInfo.m_nWidth,
     * recordInfo.m_nHeight);
     * printf("Sensor Position (X, Y): %u, %u\n", recordInfo.m_nSensorPosX,
     * recordInfo.m_nSensorPosY);
     * printf("Sensor Size (Width x Height): %u x %u\n", recordInfo.m_nSensorWidth,
     * recordInfo.m_nSensorHeight);
     * printf("Color Type            : %u\n", recordInfo.m_nColorType);
     * printf("Color Bits            : %u\n", recordInfo.m_nColorBits);
     * printf("Effective Bit Depth   : %u\n", recordInfo.m_nEffectiveBitDepth);
     * printf("Effective Bit Side    : %u\n", recordInfo.m_nEffectiveBitSide);
     * printf("IRIG Mode             : %lu\n", recordInfo.m_nIRIGMode);
     * printf("Shutter Type 2        : %lu\n", recordInfo.m_nShutterType2);
     * 
     * // Color Matrix (RR, RG, RB, GR, GG, GB, BR, BG, BB)
     * printf("Color Matrix (RR, RG, RB, GR, GG, GB, BR, BG, BB): ");
     * printf("%d, %d, %d, %d, %d, %d, %d, %d, %d\n",
     * recordInfo.m_nColorMatrixRR, recordInfo.m_nColorMatrixRG,
     * recordInfo.m_nColorMatrixRB,
     * recordInfo.m_nColorMatrixGR, recordInfo.m_nColorMatrixGG,
     * recordInfo.m_nColorMatrixGB,
     * recordInfo.m_nColorMatrixBR, recordInfo.m_nColorMatrixBG,
     * recordInfo.m_nColorMatrixBB);
     * 
     * printf("LUT Bits             : %u\n", recordInfo.m_nLUTBits);
     * printf("Color Enhance        : %u\n", recordInfo.m_nColorEnhance);
     * printf("Edge Enhance         : %u\n", recordInfo.m_nEdgeEnhance);
     * printf("LUT Mode             : %lu\n", recordInfo.m_nLUTMode);
     * printf("DS Shutter           : %u\n", recordInfo.m_nDSShutter);
     * printf("Polarization         : %u\n", recordInfo.m_nPolarization);
     * printf("Polarizer Config     : %u\n", recordInfo.m_nPolarizerConfig);
     * printf("Digits of File Number: %u\n", recordInfo.m_nDigitsOfFileNumber);
     * printf("Pixel Gain Base      : %u\n", recordInfo.m_nPixelGainBase);
     * printf("Pixel Gain Bits      : %u\n", recordInfo.m_nPixelGainBits);
     * printf("Shading Bits         : %u\n", recordInfo.m_nShadingBits);
     * printf("Bayer Pattern        : %u\n", recordInfo.m_nBayerPattern);
     * printf("Expose Time Number   : %u\n", recordInfo.m_nExposeTimeNum);
     * printf("Rec On Cmd Times     : %u\n", recordInfo.m_nRecOnCmdTimes);
     * printf("Pixel Gain Param     : %u\n", recordInfo.m_nPixelGainParam);
     * printf("Reserve              : %u\n", recordInfo.m_nReserve);
     * printf("Irig Sample          : %lu\n", recordInfo.m_IrigSample);
     * printf("AE Exposure Time Mode: %lu\n",
     * recordInfo.m_AEExposureTimeRecordMode);
     * printf("AE Mode Set          : %u\n", recordInfo.m_AEModeSet);
     * printf("DAQ Mode            : %u\n", recordInfo.m_nDAQMode);
     * printf("DAQ Range            : [%lu, %lu]\n", recordInfo.m_nDAQRange[0],
     * recordInfo.m_nDAQRange[1]);
     * printf("Irig Offset          : %ld\n", recordInfo.m_IrigOffset);
     * printf("Serial Number        : %llu\n", recordInfo.m_nSerialNumber);
     * printf("Data Number          : %lu\n", recordInfo.m_nDataNumber);
     * printf("Is Over Sync         : %lu\n", recordInfo.m_nIsOverSync);
     * printf("External Others Mode : %u\n", recordInfo.m_nExtOthersMode);
     * printf("Reserve 2            : %u\n", recordInfo.m_nReserve2);
     * printf("Temperature Type     : %lu\n", recordInfo.m_nTemperatureType);
     * printf("Acceleration Type    : %lu\n", recordInfo.m_nAccelerationType);
     * printf("Pixel Gain Data Size : %lu\n", recordInfo.m_nPixelGainDataSize);
     * 
     * return true;
     * }
     */

    public void showVirtualStack(int dataNumber, int width, int height, int totalFrames) {
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

    public void showImageStack(int dataNumber, int width, int height, int totalFrames) {
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

    private void showMessage(String title, String message) {
        JOptionPane.showMessageDialog(null, message, title, JOptionPane.INFORMATION_MESSAGE);
    }

    public static void main(String[] args) {
        IFastDockUI fastDock = new IFastDockUI();
    }
}
