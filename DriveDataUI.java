import ij.plugin.frame.PlugInFrame;
import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class DriveDataUI extends PlugInFrame {

    public DriveDataUI() {
        super("Drive Data Viewer");

        // Main Frame
        JFrame frame = new JFrame("Drive Data Viewer");
        frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        frame.setSize(600, 500);

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
        driveListModel.addElement("Drive A");
        driveListModel.addElement("Drive B"); // Example drives
        JList<String> driveList = new JList<>(driveListModel);
        JScrollPane driveScrollPane = new JScrollPane(driveList);
        driveListPanel.add(driveListLabel, BorderLayout.NORTH);
        driveListPanel.add(driveScrollPane, BorderLayout.CENTER);

        // Drive Info
        JPanel driveInfoPanel = new JPanel();
        driveInfoPanel.setLayout(new GridLayout(3, 1));
        JLabel driveNameLabel = new JLabel("Selected Drive: ");
        JLabel activityRatioLabel = new JLabel("Activity Ratio: ");
        JLabel totalWrittenLabel = new JLabel("Total Written Size: ");
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
        DefaultListModel<String> dataListModel = new DefaultListModel<>();
        dataListModel.addElement("Data 1");
        dataListModel.addElement("Data 2"); // Example data
        JList<String> dataList = new JList<>(dataListModel);
        JScrollPane dataScrollPane = new JScrollPane(dataList);
        dataListPanel.add(dataListLabel, BorderLayout.NORTH);
        dataListPanel.add(dataScrollPane, BorderLayout.CENTER);

        // Data Info
        JPanel dataInfoPanel = new JPanel();
        dataInfoPanel.setLayout(new GridLayout(3, 1));
        JLabel dataDetailsLabel = new JLabel("Selected Data: ");
        JLabel dataSizeLabel = new JLabel("Data Size: ");
        JLabel dataTypeLabel = new JLabel("Data Type: ");
        dataInfoPanel.add(dataDetailsLabel);
        dataInfoPanel.add(dataSizeLabel);
        dataInfoPanel.add(dataTypeLabel);

        lowerPanel.add(dataListPanel);
        lowerPanel.add(dataInfoPanel);

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

        frame.add(mainPanel, BorderLayout.CENTER);
        frame.add(inputPanel, BorderLayout.SOUTH);

        frame.setVisible(true);

        // Listeners
        driveList.addListSelectionListener(e -> {
            String selectedDrive = driveList.getSelectedValue();
            if (selectedDrive != null) {
                driveNameLabel.setText("Selected Drive: " + selectedDrive);
                activityRatioLabel.setText("Activity Ratio: 75% (Example)");
                totalWrittenLabel.setText("Total Written Size: 500GB (Example)");
            }
        });

        dataList.addListSelectionListener(e -> {
            String selectedData = dataList.getSelectedValue();
            if (selectedData != null) {
                dataDetailsLabel.setText("Selected Data: " + selectedData);
                dataSizeLabel.setText("Data Size: 50MB (Example)");
                dataTypeLabel.setText("Data Type: Image (Example)");
            }
        });

        submitButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                String width = widthField.getText();
                String height = heightField.getText();
                String frames = frameField.getText();
                JOptionPane.showMessageDialog(frame, "Creating Virtual Stack with\nWidth: " + width + ", Height: " + height + ", Frames: " + frames);
            }
        });
    }

    public static void main(String[] args) {
        new DriveDataUI();
    }
}

