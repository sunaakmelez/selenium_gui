package app;

import java.awt.*;
import java.awt.event.*;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.List;
import javax.swing.*;
import javax.swing.GroupLayout;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebDriverException;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.*;


import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

import java.io.FileReader;
import java.util.Iterator;

public class Application extends JFrame {

    String academicStaffUrl = "";
    String domainUrl = "";
    String bmYildizUrl = "";
    final String avesisMainUrlHttp = "http://avesis.yildiz.edu.tr";
    final String avesisMainUrlHttps = "https://avesis.yildiz.edu.tr/";

    String nonReachableUrl = "";

    ArrayList<String> blackList = new ArrayList<>();
    Configs configs = null;
    public static WebDriver driver;

    public Application() {
        initComponents();

        staffProgressPanel.setVisible(false);
        noStaffPanel.setVisible(false);
        publicationTabProgressPanel.setVisible(false);
        brokenLinkTabProgressBarPanel.setVisible(false);
        configs = getConfigs();
        if (configs != null && !configs.isConfigTrust()) {
            setAllTabsEnabled(false);
            tabbedPane1.setSelectedIndex(3);
            JOptionPane.showMessageDialog(
                    this,
                    "Ayar dosyanızda yanlış veriler mevcut. Lütfen ayarlarınızı güncelleyin.",
                    "Hata!",
                    JOptionPane.ERROR_MESSAGE
            );
        } else if (configs == null) {
            setAllTabsEnabled(false);
            tabbedPane1.setSelectedIndex(3);
            configs = new Configs();
            JOptionPane.showMessageDialog(
                    this,
                    "Ayar dosyanız bulunmamaktadır. Lütfen ayarlarınızı yapınız.",
                    "Hata!",
                    JOptionPane.ERROR_MESSAGE
            );

        } else {
            staffListInitialize();
        }

        publicationCriteriaComboBox.addItem("Taramak istediğiniz kriteri seçiniz.");
        publicationCriteriaComboBox.setSelectedIndex(publicationCriteriaComboBox.getItemCount() - 1);
    }

    private void staffListInitialize() {
        staffList = getStaffListFromFile();
        if (staffList != null && staffList.size() == 0) {
            tabbedPane1.setEnabledAt(1, false);
            noStaffPanel.setVisible(true);
        } else {
            noStaffPanel.setVisible(false);
            scrollPane1.setVisible(true);
        }
    }

    private void publicationCrawlButtonMouseClicked(MouseEvent e) {
        setAllTabsEnabled(false);
        tabbedPane1.setEnabledAt(3, false);
        long beginTime = System.currentTimeMillis();
        publicationTabProgressPanel.setVisible(true);
        publicationCrawlButton.setEnabled(false);
        downloadPublicationButton.setEnabled(false);
        staffComboBox.setEnabled(false);
        yearTextField.setEnabled(false);
        publicationCriteriaComboBox.setEnabled(false);
        String criteria = publicationCriteriaComboBox.getSelectedItem().toString();
        String selectedStaff = ((StaffListItem) staffComboBox.getSelectedItem()).getUrl();
        staffCrawlButton.setEnabled(false);

        setTimeout(() ->
                {
                    switch (criteria) {
                        case "Taramak istediğiniz kriteri seçiniz...":
                            JOptionPane.showMessageDialog(
                                    this,
                                    "Tarama kriteri seçiniz",
                                    "Eksik Bilgi!",
                                    JOptionPane.ERROR_MESSAGE
                            );
                            break;
                        default:
                            workList(selectedStaff);
                            if (excelWorkFieldItem.getTotalProjectCount() == 0) {
                                JOptionPane.showMessageDialog(
                                        this,
                                        "İlgili kriterlere uygun bir çalışma bulunamamıştır",
                                        "Çalışma Bulunamadı!",
                                        JOptionPane.INFORMATION_MESSAGE
                                );
                            } else {
                                downloadPublicationButton.setEnabled(true);
                            }
                            long endTime = System.currentTimeMillis();
                            long duration = endTime - beginTime;
                            System.out.println(selectedStaff + " Duration: " + duration);
                            break;
                    }
                    academicPublicationTab.setEnabled(true);
                    publicationTabProgressPanel.setVisible(false);

                    setAllTabsEnabled(true);
                    publicationCrawlButton.setEnabled(true);
                    staffComboBox.setEnabled(true);
                    yearTextField.setEnabled(true);
                    staffCrawlButton.setEnabled(true);
                    publicationCriteriaComboBox.setEnabled(true);
                    tearDownDriver();
                    tabbedPane1.setEnabledAt(3, true);
                    downloadPublicationButton.setEnabled(true);
                },
                1000);
    }

    private void downloadPublicationButtonMouseClicked(MouseEvent e) {
        if (excelWorkFieldItem.getTotalProjectCount() > 0) {
            saveToExcel(fileName + yearTextField.getText() + "_" + staffComboBox.getSelectedItem().toString() + "_", firstSheetName, yearTextField.getText() + firstSheetTitle, secondSheetName, "Yayın Adı ve Yayın Tarihi", excelWorkFieldItem);
        } else {
            JOptionPane.showMessageDialog(
                    this,
                    "Önce tarama yapmalısınız.",
                    "Eksik Bilgi!",
                    JOptionPane.ERROR_MESSAGE
            );
        }
    }

    private void staffCrawlButtonMouseClicked(MouseEvent e) {
        setAllTabsEnabled(false);
        tabbedPane1.setEnabledAt(3, false);
        staffProgressPanel.setVisible(true);
        staffCrawlButton.setEnabled(false);
        scrollPane1.setVisible(false);
        noStaffPanel.setVisible(false);
        setTimeout(() ->
                {
                    getStaffInfoFromWeb();
                    academicPublicationTab.setEnabled(true);
                    staffProgressPanel.setVisible(false);
                    scrollPane1.setVisible(true);
                    scrollPane2.setVisible(true);
                    brokenAvesisLabel.setVisible(true);
                    staffCrawlButton.setEnabled(true);
                    noStaffPanel.setVisible(false);
                    tabbedPane1.setEnabledAt(3, true);
                    setAllTabsEnabled(true);
                    tearDownDriver();
                },
                1000);
    }

    public static void setTimeout(Runnable runnable, int delay) {
        new Thread(() -> {
            try {
                Thread.sleep(delay);
                runnable.run();
            } catch (Exception e) {
                System.err.println(e);
            }
        }).start();
    }

    private void brokenLinkButtonMouseClicked(MouseEvent e) {
        setAllTabsEnabled(false);
        tabbedPane1.setEnabledAt(3, false);
        brokenLinkTabProgressBarPanel.setVisible(true);
        brokenLinkButton.setEnabled(false);
        numberOfBrokenLinkLabel.setVisible(false);
        urlTextField.setEnabled(false);
        downloadBrokenLinkReportButton.setEnabled(false);
        setTimeout(() ->
                {
                    setUpDriver();
                    brokenLinkExcelModelList.clear();
                    brokenLinkModel.clear();
                    linkCheckAll(urlTextField.getText(), 0, 1);
                    brokenLinkListForm.setModel(brokenLinkModel);
                    scrollPane4.setVisible(true);
                    brokenLinkTabProgressBarPanel.setVisible(false);
                    brokenLinkButton.setEnabled(true);
                    urlTextField.setEnabled(true);
                    downloadBrokenLinkReportButton.setEnabled(true);
                    setAllTabsEnabled(true);
                    numberOfBrokenLinkLabel.setVisible(true);
                    numberOfBrokenLinkLabel.setText("Bulunan kırık link sayısı: " + brokenLinkModel.getSize());
                    tabbedPane1.setEnabledAt(3, true);
                    tearDownDriver();
                },
                1000);
    }

    public void setAllTabsEnabled(boolean p_enabled) {
        tabbedPane1.setEnabledAt(0, p_enabled);

        tabbedPane1.setEnabledAt(2, p_enabled);
        if (staffList != null && staffList.size() == 0) {
            tabbedPane1.setEnabledAt(1, false);
        } else {

            tabbedPane1.setEnabledAt(1, p_enabled);
        }
    }

    private void downloadBrokenLinkReportButtonMouseClicked(MouseEvent e) {
        if (brokenLinkExcelModelList.size() > 0)
            saveBrokenLinksToExel(urlTextField.getText());
    }

    private void criteriaComboboxSettingItemStateChanged(ItemEvent evt) {
        JComboBox cb = (JComboBox) evt.getSource();

        Object item = evt.getItem();

        if (evt.getStateChange() == ItemEvent.SELECTED) {
            criteriaTitle.setText(item.toString());
        }
    }

    private void criteriaAddButtonMouseClicked(MouseEvent e) {
        String newSearchCriteria = criteriaTitle.getText().trim();
        if (newSearchCriteria.length() > 0) {
            configs.insertCriteria(newSearchCriteria);
            criteriaComboboxSetting.addItem(newSearchCriteria);
            publicationCriteriaComboBox.removeItemAt(publicationCriteriaComboBox.getItemCount() - 1);
            publicationCriteriaComboBox.addItem(newSearchCriteria);
            publicationCriteriaComboBox.addItem("Taramak istediğiniz kriteri seçiniz.");
            publicationCriteriaComboBox.setSelectedIndex(publicationCriteriaComboBox.getItemCount() - 1);
            criteriaComboboxSetting.setSelectedIndex(criteriaComboboxSetting.getItemCount() - 1);
        } else {
            JOptionPane.showMessageDialog(
                    this,
                    "Bu alan boş bırakılamaz",
                    "Eksik Bilgi!",
                    JOptionPane.ERROR_MESSAGE
            );
        }
    }

    private void criteriaUpdateButtonMouseClicked(MouseEvent e) {
        String newSearchCriteria = criteriaTitle.getText().trim();
        int index = criteriaComboboxSetting.getSelectedIndex();
        int dialogButton = JOptionPane.YES_NO_OPTION;
        if (newSearchCriteria.length() > 0 && index >= 0 && JOptionPane.showConfirmDialog(this, "Seçilen maddeyi güncellemek istediğinizden emin misiniz?", "Emin misiniz?", dialogButton) == 0) {


            List<String> newItems = configs.updateCriteria(newSearchCriteria, index);
            criteriaComboboxSetting.removeAllItems();
            publicationCriteriaComboBox.removeAllItems();
            for (String item : newItems) {
                criteriaComboboxSetting.addItem(item);
                publicationCriteriaComboBox.addItem(item);
            }
            publicationCriteriaComboBox.addItem("Taramak istediğiniz kriteri seçiniz...");
            publicationCriteriaComboBox.setSelectedIndex(publicationCriteriaComboBox.getItemCount() - 1);

        } else if (newSearchCriteria.length() == 0 || index < 0) {
            JOptionPane.showMessageDialog(
                    this,
                    "Bu alan boş bırakılamaz",
                    "Eksik Bilgi!",
                    JOptionPane.ERROR_MESSAGE
            );
        }
    }

    private void criteriaDeleteButtonMouseClicked(MouseEvent e) {
        int index = criteriaComboboxSetting.getSelectedIndex();
        int dialogButton = JOptionPane.YES_NO_OPTION;
        if (index >= 0 && JOptionPane.showConfirmDialog(this, "Seçilen maddeyi silmek istediğinizden emin misiniz?", "Emin misiniz?", dialogButton) == 0) {
            criteriaComboboxSetting.removeItemAt(index);
            publicationCriteriaComboBox.removeItemAt(index);
            configs.deleteCriteria(index);

        } else if (index < 0) {
            JOptionPane.showMessageDialog(
                    this,
                    "Bu alan boş bırakılamaz",
                    "Eksik Bilgi!",
                    JOptionPane.ERROR_MESSAGE
            );
        }
    }

    private void saveButtonMouseClicked(MouseEvent e) {
        String departmentUrl = departmentWebSite.getText().trim();
        String _domainUrl = domainSite.getText().trim();
        String staffUrl = staffSite.getText().trim();
        try {
            int nodeCount = Integer.parseInt(brokenLinkNode.getText().trim());

            if (departmentUrl.length() > 0 && _domainUrl.length() > 0 && staffUrl.length() > 0 && nodeCount > 0) {
                configs.setDepartmentWebSite(departmentUrl);
                configs.setDomainSite(_domainUrl);
                configs.setStaffSite(staffUrl);
                configs.setBrokenLinkNode(nodeCount);
                academicStaffUrl = staffUrl;
                bmYildizUrl = departmentUrl;
                domainUrl = _domainUrl;
                urlTextField.setText(bmYildizUrl);
                saveConfigToFile(configs);
                setAllTabsEnabled(true);
                staffListInitialize();
                JOptionPane.showMessageDialog(
                        this,
                        "Ayarlarınız başarıyla kaydedilmiştir.",
                        "Bilgi",
                        JOptionPane.INFORMATION_MESSAGE
                );
            } else {
                JOptionPane.showMessageDialog(
                        this,
                        "Hiçbir alan boş bırakılamaz",
                        "Eksik Bilgi!",
                        JOptionPane.ERROR_MESSAGE
                );
            }
        }catch(Exception exception){
            JOptionPane.showMessageDialog(
                    this,
                    "Hiçbir alan boş bırakılamaz",
                    "Eksik Bilgi!",
                    JOptionPane.ERROR_MESSAGE
            );
        }
    }

    private void brokenLinkNodeKeyTyped(KeyEvent e) {
        char caracter = e.getKeyChar();
        if (((caracter < '0') || (caracter > '9')) && (caracter != '\b')) {

            e.consume();
        }
    }

    private void initComponents() {
        this.setVisible(true);
        this.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        // JFormDesigner - Component initialization - DO NOT MODIFY  //GEN-BEGIN:initComponents
        // Generated using JFormDesigner Evaluation license - unknown
        tabbedPane1 = new JTabbedPane();
        academicStaffTab = new JPanel();
        staffCrawlButton = new JButton();
        scrollPane1 = new JScrollPane();
        staffListForm = new JList();
        brokenAvesisLabel = new JLabel();
        scrollPane2 = new JScrollPane();
        brokenStaffListForm = new JList();
        staffProgressPanel = new JPanel();
        staffProgressBar = new JProgressBar();
        staffInProgressLabel = new JLabel();
        noStaffPanel = new JPanel();
        noStaffLabel = new JLabel();
        academicPublicationTab = new JPanel();
        academicStaffLabel = new JLabel();
        staffComboBox = new JComboBox();
        yearLabel = new JLabel();
        yearTextField = new JTextField();
        publicationCriteriaLabel = new JLabel();
        publicationCriteriaComboBox = new JComboBox();
        publicationCrawlButton = new JButton();
        downloadPublicationButton = new JButton();
        publicationTabProgressPanel = new JPanel();
        publicationProgressBar = new JProgressBar();
        publicationInProgressLabel = new JLabel();
        publicationBrokenLinkPanel = new JPanel();
        invalidStaff = new JLabel();
        scrollPane3 = new JScrollPane();
        brokenPublicationListForm = new JList();
        brokenLinkTab = new JPanel();
        brokenLinkUrl = new JLabel();
        urlTextField = new JTextField();
        brokenLinkButton = new JButton();
        scrollPane4 = new JScrollPane();
        brokenLinkListForm = new JList();
        brokenLinkTabProgressBarPanel = new JPanel();
        brokenLinkProgressBar = new JProgressBar();
        inProgressLabel = new JLabel();
        downloadBrokenLinkReportButton = new JButton();
        numberOfBrokenLinkLabel = new JLabel();
        settingsTab = new JPanel();
        departmentWebSite = new JTextField();
        criteriaUpdateButton = new JButton();
        domainSite = new JTextField();
        staffSite = new JTextField();
        brokenLinkNode = new JTextField();
        criteriaTitle = new JTextField();
        criteriaComboboxSetting = new JComboBox();
        label = new JLabel();
        label3 = new JLabel();
        label4 = new JLabel();
        label5 = new JLabel();
        label6 = new JLabel();
        label7 = new JLabel();
        criteriaDeleteButton = new JButton();
        criteriaAddButton = new JButton();
        label8 = new JLabel();
        saveButton = new JButton();

        //======== this ========
        setTitle("Uygulama");
        Container contentPane = getContentPane();

        //======== tabbedPane1 ========
        {

            //======== academicStaffTab ========
            {
                academicStaffTab.setBorder(new javax.swing.border.CompoundBorder(new javax.swing.border.TitledBorder(new javax
                .swing.border.EmptyBorder(0,0,0,0), "JF\u006frmDes\u0069gner \u0045valua\u0074ion",javax.swing
                .border.TitledBorder.CENTER,javax.swing.border.TitledBorder.BOTTOM,new java.awt.
                Font("D\u0069alog",java.awt.Font.BOLD,12),java.awt.Color.red
                ),academicStaffTab. getBorder()));academicStaffTab. addPropertyChangeListener(new java.beans.PropertyChangeListener(){@Override
                public void propertyChange(java.beans.PropertyChangeEvent e){if("\u0062order".equals(e.getPropertyName(
                )))throw new RuntimeException();}});

                //---- staffCrawlButton ----
                staffCrawlButton.setText("Taramay\u0131 Ba\u015flat");
                staffCrawlButton.addMouseListener(new MouseAdapter() {
                    @Override
                    public void mouseClicked(MouseEvent e) {
                        staffCrawlButtonMouseClicked(e);
                    }
                });

                //======== scrollPane1 ========
                {
                    scrollPane1.setVisible(false);
                    scrollPane1.setViewportView(staffListForm);
                }

                //---- brokenAvesisLabel ----
                brokenAvesisLabel.setText("Hatal\u0131 Linkler:");
                brokenAvesisLabel.setVisible(false);

                //======== scrollPane2 ========
                {
                    scrollPane2.setVisible(false);
                    scrollPane2.setViewportView(brokenStaffListForm);
                }

                //======== staffProgressPanel ========
                {

                    //---- staffProgressBar ----
                    staffProgressBar.setIndeterminate(true);

                    //---- staffInProgressLabel ----
                    staffInProgressLabel.setText("\u0130\u015fleminiz devam ediyor...");

                    GroupLayout staffProgressPanelLayout = new GroupLayout(staffProgressPanel);
                    staffProgressPanel.setLayout(staffProgressPanelLayout);
                    staffProgressPanelLayout.setHorizontalGroup(
                        staffProgressPanelLayout.createParallelGroup()
                            .addGroup(staffProgressPanelLayout.createSequentialGroup()
                                .addGroup(staffProgressPanelLayout.createParallelGroup()
                                    .addComponent(staffInProgressLabel)
                                    .addComponent(staffProgressBar, GroupLayout.PREFERRED_SIZE, 423, GroupLayout.PREFERRED_SIZE))
                                .addGap(44, 44, 44))
                    );
                    staffProgressPanelLayout.setVerticalGroup(
                        staffProgressPanelLayout.createParallelGroup()
                            .addGroup(staffProgressPanelLayout.createSequentialGroup()
                                .addContainerGap()
                                .addComponent(staffProgressBar, GroupLayout.PREFERRED_SIZE, 14, GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                .addComponent(staffInProgressLabel))
                    );
                }

                //======== noStaffPanel ========
                {

                    //---- noStaffLabel ----
                    noStaffLabel.setText("\u00d6nce tarama yapmal\u0131s\u0131n\u0131z.");

                    GroupLayout noStaffPanelLayout = new GroupLayout(noStaffPanel);
                    noStaffPanel.setLayout(noStaffPanelLayout);
                    noStaffPanelLayout.setHorizontalGroup(
                        noStaffPanelLayout.createParallelGroup()
                            .addGroup(noStaffPanelLayout.createSequentialGroup()
                                .addComponent(noStaffLabel, GroupLayout.PREFERRED_SIZE, 475, GroupLayout.PREFERRED_SIZE)
                                .addGap(0, 0, Short.MAX_VALUE))
                    );
                    noStaffPanelLayout.setVerticalGroup(
                        noStaffPanelLayout.createParallelGroup()
                            .addGroup(GroupLayout.Alignment.TRAILING, noStaffPanelLayout.createSequentialGroup()
                                .addGap(0, 0, Short.MAX_VALUE)
                                .addComponent(noStaffLabel))
                    );
                }

                GroupLayout academicStaffTabLayout = new GroupLayout(academicStaffTab);
                academicStaffTab.setLayout(academicStaffTabLayout);
                academicStaffTabLayout.setHorizontalGroup(
                    academicStaffTabLayout.createParallelGroup()
                        .addGroup(academicStaffTabLayout.createSequentialGroup()
                            .addContainerGap()
                            .addGroup(academicStaffTabLayout.createParallelGroup()
                                .addComponent(noStaffPanel, GroupLayout.DEFAULT_SIZE, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                .addGroup(academicStaffTabLayout.createSequentialGroup()
                                    .addGroup(academicStaffTabLayout.createParallelGroup(GroupLayout.Alignment.TRAILING)
                                        .addComponent(brokenAvesisLabel, GroupLayout.Alignment.LEADING)
                                        .addComponent(staffCrawlButton, GroupLayout.Alignment.LEADING, GroupLayout.PREFERRED_SIZE, 422, GroupLayout.PREFERRED_SIZE)
                                        .addComponent(scrollPane1, GroupLayout.PREFERRED_SIZE, 430, GroupLayout.PREFERRED_SIZE))
                                    .addGap(38, 38, Short.MAX_VALUE))
                                .addGroup(academicStaffTabLayout.createSequentialGroup()
                                    .addGroup(academicStaffTabLayout.createParallelGroup()
                                        .addComponent(staffProgressPanel, GroupLayout.PREFERRED_SIZE, 436, GroupLayout.PREFERRED_SIZE)
                                        .addComponent(scrollPane2, GroupLayout.PREFERRED_SIZE, 430, GroupLayout.PREFERRED_SIZE))
                                    .addGap(0, 24, Short.MAX_VALUE))))
                );
                academicStaffTabLayout.setVerticalGroup(
                    academicStaffTabLayout.createParallelGroup()
                        .addGroup(academicStaffTabLayout.createSequentialGroup()
                            .addContainerGap()
                            .addComponent(staffCrawlButton)
                            .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                            .addComponent(staffProgressPanel, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
                            .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                            .addComponent(noStaffPanel, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
                            .addPreferredGap(LayoutStyle.ComponentPlacement.UNRELATED)
                            .addComponent(scrollPane1, GroupLayout.PREFERRED_SIZE, 130, GroupLayout.PREFERRED_SIZE)
                            .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                            .addComponent(brokenAvesisLabel)
                            .addPreferredGap(LayoutStyle.ComponentPlacement.UNRELATED)
                            .addComponent(scrollPane2, GroupLayout.PREFERRED_SIZE, 113, GroupLayout.PREFERRED_SIZE)
                            .addContainerGap())
                );
            }
            tabbedPane1.addTab("Akademik Kadro", academicStaffTab);

            //======== academicPublicationTab ========
            {

                //---- academicStaffLabel ----
                academicStaffLabel.setText("Akademik Kadro:");

                //---- yearLabel ----
                yearLabel.setText("Y\u0131l:");

                //---- yearTextField ----
                yearTextField.setText("2020");

                //---- publicationCriteriaLabel ----
                publicationCriteriaLabel.setText("Arama Kriteri:");

                //---- publicationCrawlButton ----
                publicationCrawlButton.setText("Taramay\u0131 Ba\u015flat");
                publicationCrawlButton.addMouseListener(new MouseAdapter() {
                    @Override
                    public void mouseClicked(MouseEvent e) {
                        publicationCrawlButtonMouseClicked(e);
                    }
                });

                //---- downloadPublicationButton ----
                downloadPublicationButton.setText("\u0130ndir");
                downloadPublicationButton.setEnabled(false);
                downloadPublicationButton.addMouseListener(new MouseAdapter() {
                    @Override
                    public void mouseClicked(MouseEvent e) {
                        downloadPublicationButtonMouseClicked(e);
                    }
                });

                //======== publicationTabProgressPanel ========
                {

                    //---- publicationProgressBar ----
                    publicationProgressBar.setIndeterminate(true);

                    //---- publicationInProgressLabel ----
                    publicationInProgressLabel.setText("\u0130\u015fleminiz devam ediyor...");

                    GroupLayout publicationTabProgressPanelLayout = new GroupLayout(publicationTabProgressPanel);
                    publicationTabProgressPanel.setLayout(publicationTabProgressPanelLayout);
                    publicationTabProgressPanelLayout.setHorizontalGroup(
                        publicationTabProgressPanelLayout.createParallelGroup()
                            .addGroup(publicationTabProgressPanelLayout.createSequentialGroup()
                                .addContainerGap(GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                .addGroup(publicationTabProgressPanelLayout.createParallelGroup()
                                    .addComponent(publicationInProgressLabel, GroupLayout.PREFERRED_SIZE, 458, GroupLayout.PREFERRED_SIZE)
                                    .addComponent(publicationProgressBar, GroupLayout.PREFERRED_SIZE, 416, GroupLayout.PREFERRED_SIZE)))
                    );
                    publicationTabProgressPanelLayout.setVerticalGroup(
                        publicationTabProgressPanelLayout.createParallelGroup()
                            .addGroup(publicationTabProgressPanelLayout.createSequentialGroup()
                                .addContainerGap()
                                .addComponent(publicationProgressBar, GroupLayout.PREFERRED_SIZE, 20, GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(publicationInProgressLabel)
                                .addContainerGap(GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                    );
                }

                //======== publicationBrokenLinkPanel ========
                {
                    publicationBrokenLinkPanel.setVisible(false);

                    //---- invalidStaff ----
                    invalidStaff.setText("Hatal\u0131 Personeller");

                    //======== scrollPane3 ========
                    {
                        scrollPane3.setViewportView(brokenPublicationListForm);
                    }

                    GroupLayout publicationBrokenLinkPanelLayout = new GroupLayout(publicationBrokenLinkPanel);
                    publicationBrokenLinkPanel.setLayout(publicationBrokenLinkPanelLayout);
                    publicationBrokenLinkPanelLayout.setHorizontalGroup(
                        publicationBrokenLinkPanelLayout.createParallelGroup()
                            .addGroup(publicationBrokenLinkPanelLayout.createSequentialGroup()
                                .addContainerGap()
                                .addGroup(publicationBrokenLinkPanelLayout.createParallelGroup()
                                    .addComponent(invalidStaff)
                                    .addComponent(scrollPane3, GroupLayout.PREFERRED_SIZE, 447, GroupLayout.PREFERRED_SIZE))
                                .addContainerGap(GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                    );
                    publicationBrokenLinkPanelLayout.setVerticalGroup(
                        publicationBrokenLinkPanelLayout.createParallelGroup()
                            .addGroup(publicationBrokenLinkPanelLayout.createSequentialGroup()
                                .addContainerGap()
                                .addComponent(invalidStaff)
                                .addPreferredGap(LayoutStyle.ComponentPlacement.UNRELATED)
                                .addComponent(scrollPane3, GroupLayout.DEFAULT_SIZE, 0, Short.MAX_VALUE)
                                .addContainerGap())
                    );
                }

                GroupLayout academicPublicationTabLayout = new GroupLayout(academicPublicationTab);
                academicPublicationTab.setLayout(academicPublicationTabLayout);
                academicPublicationTabLayout.setHorizontalGroup(
                    academicPublicationTabLayout.createParallelGroup()
                        .addGroup(academicPublicationTabLayout.createSequentialGroup()
                            .addContainerGap()
                            .addGroup(academicPublicationTabLayout.createParallelGroup()
                                .addGroup(academicPublicationTabLayout.createSequentialGroup()
                                    .addGap(0, 0, Short.MAX_VALUE)
                                    .addComponent(publicationBrokenLinkPanel, GroupLayout.PREFERRED_SIZE, 0, GroupLayout.PREFERRED_SIZE)
                                    .addGap(470, 470, 470))
                                .addGroup(academicPublicationTabLayout.createSequentialGroup()
                                    .addGroup(academicPublicationTabLayout.createParallelGroup()
                                        .addComponent(academicStaffLabel)
                                        .addComponent(yearLabel)
                                        .addComponent(publicationCriteriaLabel)
                                        .addGroup(academicPublicationTabLayout.createParallelGroup(GroupLayout.Alignment.TRAILING, false)
                                            .addComponent(staffComboBox, GroupLayout.Alignment.LEADING)
                                            .addComponent(yearTextField, GroupLayout.Alignment.LEADING)
                                            .addComponent(publicationCriteriaComboBox, GroupLayout.Alignment.LEADING)
                                            .addGroup(GroupLayout.Alignment.LEADING, academicPublicationTabLayout.createSequentialGroup()
                                                .addComponent(publicationCrawlButton, GroupLayout.PREFERRED_SIZE, 156, GroupLayout.PREFERRED_SIZE)
                                                .addGap(31, 31, 31)
                                                .addComponent(downloadPublicationButton, GroupLayout.PREFERRED_SIZE, 168, GroupLayout.PREFERRED_SIZE)))
                                        .addComponent(publicationTabProgressPanel, GroupLayout.PREFERRED_SIZE, 441, GroupLayout.PREFERRED_SIZE))
                                    .addGap(0, 0, Short.MAX_VALUE))))
                );
                academicPublicationTabLayout.setVerticalGroup(
                    academicPublicationTabLayout.createParallelGroup()
                        .addGroup(academicPublicationTabLayout.createSequentialGroup()
                            .addGap(15, 15, 15)
                            .addComponent(academicStaffLabel)
                            .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                            .addComponent(staffComboBox, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
                            .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                            .addComponent(yearLabel)
                            .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                            .addComponent(yearTextField, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
                            .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                            .addComponent(publicationCriteriaLabel)
                            .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                            .addComponent(publicationCriteriaComboBox, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
                            .addGroup(academicPublicationTabLayout.createParallelGroup()
                                .addGroup(academicPublicationTabLayout.createSequentialGroup()
                                    .addGap(28, 28, 28)
                                    .addComponent(publicationBrokenLinkPanel, GroupLayout.PREFERRED_SIZE, 0, GroupLayout.PREFERRED_SIZE))
                                .addGroup(academicPublicationTabLayout.createSequentialGroup()
                                    .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                                    .addGroup(academicPublicationTabLayout.createParallelGroup(GroupLayout.Alignment.BASELINE)
                                        .addComponent(publicationCrawlButton)
                                        .addComponent(downloadPublicationButton))))
                            .addGap(18, 18, 18)
                            .addComponent(publicationTabProgressPanel, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
                            .addContainerGap(173, Short.MAX_VALUE))
                );
            }
            tabbedPane1.addTab("Akademik \u00c7al\u0131\u015fmalar", academicPublicationTab);

            //======== brokenLinkTab ========
            {

                //---- brokenLinkUrl ----
                brokenLinkUrl.setText("URL:");

                //---- brokenLinkButton ----
                brokenLinkButton.setIcon(new ImageIcon("C:\\Users\\SEDAT\\Desktop\\suna\\whatever_jframe\\icons\\search.png"));
                brokenLinkButton.addMouseListener(new MouseAdapter() {
                    @Override
                    public void mouseClicked(MouseEvent e) {
                        brokenLinkButtonMouseClicked(e);
                    }
                });

                //======== scrollPane4 ========
                {
                    scrollPane4.setVisible(false);
                    scrollPane4.setViewportView(brokenLinkListForm);
                }

                //======== brokenLinkTabProgressBarPanel ========
                {

                    //---- brokenLinkProgressBar ----
                    brokenLinkProgressBar.setIndeterminate(true);

                    //---- inProgressLabel ----
                    inProgressLabel.setText("\u0130\u015fleminiz devam ediyor...");

                    GroupLayout brokenLinkTabProgressBarPanelLayout = new GroupLayout(brokenLinkTabProgressBarPanel);
                    brokenLinkTabProgressBarPanel.setLayout(brokenLinkTabProgressBarPanelLayout);
                    brokenLinkTabProgressBarPanelLayout.setHorizontalGroup(
                        brokenLinkTabProgressBarPanelLayout.createParallelGroup()
                            .addGroup(brokenLinkTabProgressBarPanelLayout.createSequentialGroup()
                                .addContainerGap()
                                .addGroup(brokenLinkTabProgressBarPanelLayout.createParallelGroup()
                                    .addComponent(inProgressLabel, GroupLayout.DEFAULT_SIZE, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                    .addComponent(brokenLinkProgressBar, GroupLayout.DEFAULT_SIZE, 0, Short.MAX_VALUE))
                                .addContainerGap())
                    );
                    brokenLinkTabProgressBarPanelLayout.setVerticalGroup(
                        brokenLinkTabProgressBarPanelLayout.createParallelGroup()
                            .addGroup(brokenLinkTabProgressBarPanelLayout.createSequentialGroup()
                                .addContainerGap(GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                .addComponent(brokenLinkProgressBar, GroupLayout.PREFERRED_SIZE, 22, GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(LayoutStyle.ComponentPlacement.UNRELATED)
                                .addComponent(inProgressLabel))
                    );
                }

                //---- downloadBrokenLinkReportButton ----
                downloadBrokenLinkReportButton.setText("\u0130ndir");
                downloadBrokenLinkReportButton.setEnabled(false);
                downloadBrokenLinkReportButton.addMouseListener(new MouseAdapter() {
                    @Override
                    public void mouseClicked(MouseEvent e) {
                        downloadBrokenLinkReportButtonMouseClicked(e);
                    }
                });

                //---- numberOfBrokenLinkLabel ----
                numberOfBrokenLinkLabel.setText("Bulunan k\u0131r\u0131k link say\u0131s\u0131:");
                numberOfBrokenLinkLabel.setVisible(false);

                GroupLayout brokenLinkTabLayout = new GroupLayout(brokenLinkTab);
                brokenLinkTab.setLayout(brokenLinkTabLayout);
                brokenLinkTabLayout.setHorizontalGroup(
                    brokenLinkTabLayout.createParallelGroup()
                        .addGroup(brokenLinkTabLayout.createSequentialGroup()
                            .addContainerGap()
                            .addGroup(brokenLinkTabLayout.createParallelGroup()
                                .addGroup(brokenLinkTabLayout.createSequentialGroup()
                                    .addGap(6, 6, 6)
                                    .addComponent(numberOfBrokenLinkLabel)
                                    .addGap(0, 454, Short.MAX_VALUE))
                                .addGroup(GroupLayout.Alignment.TRAILING, brokenLinkTabLayout.createSequentialGroup()
                                    .addComponent(brokenLinkUrl)
                                    .addGap(18, 18, 18)
                                    .addComponent(urlTextField, GroupLayout.DEFAULT_SIZE, 308, Short.MAX_VALUE)
                                    .addPreferredGap(LayoutStyle.ComponentPlacement.UNRELATED)
                                    .addComponent(brokenLinkButton)
                                    .addGap(20, 20, 20))
                                .addComponent(brokenLinkTabProgressBarPanel, GroupLayout.DEFAULT_SIZE, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)))
                        .addGroup(brokenLinkTabLayout.createSequentialGroup()
                            .addGroup(brokenLinkTabLayout.createParallelGroup()
                                .addGroup(brokenLinkTabLayout.createSequentialGroup()
                                    .addContainerGap()
                                    .addComponent(scrollPane4, GroupLayout.PREFERRED_SIZE, 439, GroupLayout.PREFERRED_SIZE))
                                .addGroup(brokenLinkTabLayout.createSequentialGroup()
                                    .addGap(175, 175, 175)
                                    .addComponent(downloadBrokenLinkReportButton, GroupLayout.PREFERRED_SIZE, 111, GroupLayout.PREFERRED_SIZE)))
                            .addGap(0, 180, Short.MAX_VALUE))
                );
                brokenLinkTabLayout.setVerticalGroup(
                    brokenLinkTabLayout.createParallelGroup()
                        .addGroup(brokenLinkTabLayout.createSequentialGroup()
                            .addGap(12, 12, 12)
                            .addGroup(brokenLinkTabLayout.createParallelGroup(GroupLayout.Alignment.BASELINE)
                                .addComponent(brokenLinkButton, GroupLayout.PREFERRED_SIZE, 41, GroupLayout.PREFERRED_SIZE)
                                .addComponent(brokenLinkUrl)
                                .addComponent(urlTextField, GroupLayout.PREFERRED_SIZE, 41, GroupLayout.PREFERRED_SIZE))
                            .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                            .addComponent(brokenLinkTabProgressBarPanel, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
                            .addPreferredGap(LayoutStyle.ComponentPlacement.UNRELATED)
                            .addComponent(numberOfBrokenLinkLabel)
                            .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                            .addComponent(scrollPane4, GroupLayout.PREFERRED_SIZE, 242, GroupLayout.PREFERRED_SIZE)
                            .addGap(18, 18, 18)
                            .addComponent(downloadBrokenLinkReportButton)
                            .addContainerGap(301, Short.MAX_VALUE))
                );
            }
            tabbedPane1.addTab("K\u0131r\u0131k Link", brokenLinkTab);

            //======== settingsTab ========
            {

                //---- criteriaUpdateButton ----
                criteriaUpdateButton.setText("G\u00fcncelle");
                criteriaUpdateButton.addMouseListener(new MouseAdapter() {
                    @Override
                    public void mouseClicked(MouseEvent e) {
                        criteriaUpdateButtonMouseClicked(e);
                    }
                });

                //---- brokenLinkNode ----
                brokenLinkNode.addKeyListener(new KeyAdapter() {
                    @Override
                    public void keyTyped(KeyEvent e) {
                        brokenLinkNodeKeyTyped(e);
                    }
                });

                //---- criteriaTitle ----
                criteriaTitle.setToolTipText("Eklenecek kriterde k\u0131saltmalar harici her kelimenin sadece ilk harfi b\u00fcy\u00fck olmal\u0131");

                //---- criteriaComboboxSetting ----
                criteriaComboboxSetting.addItemListener(e -> criteriaComboboxSettingItemStateChanged(e));

                //---- label ----
                label.setText("B\u00f6l\u00fcm Web Site Adresi:");

                //---- label3 ----
                label3.setText("Domain Adresi:");

                //---- label4 ----
                label4.setText("Akademik Kadro Adresi:");

                //---- label5 ----
                label5.setText("K\u0131r\u0131k Link Arama Seviyesi:");

                //---- label6 ----
                label6.setText("Arama Kriter Ba\u015fl\u0131\u011f\u0131:");

                //---- label7 ----
                label7.setText("Arama Kriterleri");

                //---- criteriaDeleteButton ----
                criteriaDeleteButton.setText("Sil");
                criteriaDeleteButton.addMouseListener(new MouseAdapter() {
                    @Override
                    public void mouseClicked(MouseEvent e) {
                        criteriaDeleteButtonMouseClicked(e);
                    }
                });

                //---- criteriaAddButton ----
                criteriaAddButton.setText("Ekle");
                criteriaAddButton.addMouseListener(new MouseAdapter() {
                    @Override
                    public void mouseClicked(MouseEvent e) {
                        criteriaAddButtonMouseClicked(e);
                    }
                });

                //---- label8 ----
                label8.setText("Arama Kriter \u0130\u015flemleri:");

                //---- saveButton ----
                saveButton.setText("De\u011fi\u015fiklikleri Kaydet");
                saveButton.addMouseListener(new MouseAdapter() {
                    @Override
                    public void mouseClicked(MouseEvent e) {
                        saveButtonMouseClicked(e);
                    }
                });

                GroupLayout settingsTabLayout = new GroupLayout(settingsTab);
                settingsTab.setLayout(settingsTabLayout);
                settingsTabLayout.setHorizontalGroup(
                    settingsTabLayout.createParallelGroup()
                        .addGroup(settingsTabLayout.createSequentialGroup()
                            .addContainerGap()
                            .addGroup(settingsTabLayout.createParallelGroup()
                                .addComponent(saveButton, GroupLayout.PREFERRED_SIZE, 406, GroupLayout.PREFERRED_SIZE)
                                .addComponent(label)
                                .addComponent(label3)
                                .addComponent(label4)
                                .addComponent(label5)
                                .addComponent(label6)
                                .addComponent(label8)
                                .addComponent(label7)
                                .addComponent(criteriaComboboxSetting, GroupLayout.PREFERRED_SIZE, 406, GroupLayout.PREFERRED_SIZE)
                                .addGroup(settingsTabLayout.createParallelGroup(GroupLayout.Alignment.TRAILING, false)
                                    .addComponent(departmentWebSite, GroupLayout.Alignment.LEADING, GroupLayout.DEFAULT_SIZE, 397, Short.MAX_VALUE)
                                    .addComponent(domainSite, GroupLayout.Alignment.LEADING, GroupLayout.DEFAULT_SIZE, 397, Short.MAX_VALUE)
                                    .addComponent(staffSite, GroupLayout.Alignment.LEADING, GroupLayout.DEFAULT_SIZE, 397, Short.MAX_VALUE)
                                    .addComponent(brokenLinkNode, GroupLayout.Alignment.LEADING, GroupLayout.DEFAULT_SIZE, 397, Short.MAX_VALUE)
                                    .addGroup(GroupLayout.Alignment.LEADING, settingsTabLayout.createSequentialGroup()
                                        .addComponent(criteriaAddButton, GroupLayout.PREFERRED_SIZE, 122, GroupLayout.PREFERRED_SIZE)
                                        .addGap(18, 18, 18)
                                        .addComponent(criteriaUpdateButton, GroupLayout.PREFERRED_SIZE, 125, GroupLayout.PREFERRED_SIZE)
                                        .addGap(18, 18, 18)
                                        .addComponent(criteriaDeleteButton, GroupLayout.DEFAULT_SIZE, 114, Short.MAX_VALUE))
                                    .addComponent(criteriaTitle, GroupLayout.Alignment.LEADING, GroupLayout.DEFAULT_SIZE, 397, Short.MAX_VALUE)))
                            .addContainerGap(54, Short.MAX_VALUE))
                );
                settingsTabLayout.setVerticalGroup(
                    settingsTabLayout.createParallelGroup()
                        .addGroup(settingsTabLayout.createSequentialGroup()
                            .addContainerGap()
                            .addComponent(label)
                            .addGap(5, 5, 5)
                            .addComponent(departmentWebSite, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
                            .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                            .addComponent(label3)
                            .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                            .addComponent(domainSite, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
                            .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                            .addComponent(label4)
                            .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                            .addComponent(staffSite, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
                            .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                            .addComponent(label5)
                            .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                            .addComponent(brokenLinkNode, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
                            .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                            .addComponent(label6)
                            .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                            .addComponent(criteriaTitle, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
                            .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                            .addComponent(label8)
                            .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                            .addGroup(settingsTabLayout.createParallelGroup(GroupLayout.Alignment.BASELINE)
                                .addComponent(criteriaAddButton)
                                .addComponent(criteriaUpdateButton)
                                .addComponent(criteriaDeleteButton))
                            .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                            .addComponent(label7)
                            .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                            .addComponent(criteriaComboboxSetting, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
                            .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED, 23, Short.MAX_VALUE)
                            .addComponent(saveButton)
                            .addContainerGap())
                );
            }
            tabbedPane1.addTab("Ayarlar", settingsTab);
        }

        GroupLayout contentPaneLayout = new GroupLayout(contentPane);
        contentPane.setLayout(contentPaneLayout);
        contentPaneLayout.setHorizontalGroup(
            contentPaneLayout.createParallelGroup()
                .addGroup(contentPaneLayout.createSequentialGroup()
                    .addContainerGap()
                    .addComponent(tabbedPane1, GroupLayout.PREFERRED_SIZE, 466, GroupLayout.PREFERRED_SIZE)
                    .addContainerGap(11, Short.MAX_VALUE))
        );
        contentPaneLayout.setVerticalGroup(
            contentPaneLayout.createParallelGroup()
                .addGroup(contentPaneLayout.createSequentialGroup()
                    .addContainerGap()
                    .addComponent(tabbedPane1)
                    .addContainerGap())
        );
        pack();
        setLocationRelativeTo(getOwner());
        // JFormDesigner - End of component initialization  //GEN-END:initComponents
    }

    // JFormDesigner - Variables declaration - DO NOT MODIFY  //GEN-BEGIN:variables
    // Generated using JFormDesigner Evaluation license - unknown
    private JTabbedPane tabbedPane1;
    private JPanel academicStaffTab;
    private JButton staffCrawlButton;
    private JScrollPane scrollPane1;
    private JList staffListForm;
    private JLabel brokenAvesisLabel;
    private JScrollPane scrollPane2;
    private JList brokenStaffListForm;
    private JPanel staffProgressPanel;
    private JProgressBar staffProgressBar;
    private JLabel staffInProgressLabel;
    private JPanel noStaffPanel;
    private JLabel noStaffLabel;
    private JPanel academicPublicationTab;
    private JLabel academicStaffLabel;
    private JComboBox staffComboBox;
    private JLabel yearLabel;
    private JTextField yearTextField;
    private JLabel publicationCriteriaLabel;
    private JComboBox publicationCriteriaComboBox;
    private JButton publicationCrawlButton;
    private JButton downloadPublicationButton;
    private JPanel publicationTabProgressPanel;
    private JProgressBar publicationProgressBar;
    private JLabel publicationInProgressLabel;
    private JPanel publicationBrokenLinkPanel;
    private JLabel invalidStaff;
    private JScrollPane scrollPane3;
    private JList brokenPublicationListForm;
    private JPanel brokenLinkTab;
    private JLabel brokenLinkUrl;
    private JTextField urlTextField;
    private JButton brokenLinkButton;
    private JScrollPane scrollPane4;
    private JList brokenLinkListForm;
    private JPanel brokenLinkTabProgressBarPanel;
    private JProgressBar brokenLinkProgressBar;
    private JLabel inProgressLabel;
    private JButton downloadBrokenLinkReportButton;
    private JLabel numberOfBrokenLinkLabel;
    private JPanel settingsTab;
    private JTextField departmentWebSite;
    private JButton criteriaUpdateButton;
    private JTextField domainSite;
    private JTextField staffSite;
    private JTextField brokenLinkNode;
    private JTextField criteriaTitle;
    private JComboBox criteriaComboboxSetting;
    private JLabel label;
    private JLabel label3;
    private JLabel label4;
    private JLabel label5;
    private JLabel label6;
    private JLabel label7;
    private JButton criteriaDeleteButton;
    private JButton criteriaAddButton;
    private JLabel label8;
    private JButton saveButton;
    // JFormDesigner - End of variables declaration  //GEN-END:variables

    public static void main(String[] args) {
        new Application();
    }

    public void getStaffInfoFromWeb() {
        setUpDriver();
        staffList.clear();
        listModel.clear();
        brokenStaffList.clear();
        brokenAvesisLinkModel.clear();
        staffComboBox.removeAllItems();
        try {
            driver.get(academicStaffUrl);
            clickComputerScienceTab();
            if (!containsUrl(staffList, "All") && !containsUserName(staffList, "Hepsi")) {
                staffList.add(new StaffListItem("All", "Hepsi"));
                staffComboBox.addItem(new StaffListItem("All", "Hepsi"));
            }
            getAcademicStaffList();
            clickComputerHardwareTab();
            getAcademicStaffList();
            clickComputerSoftwareTab();
            getAcademicStaffList();

            if (staffList != null && staffList.size() > 0) {
                tabbedPane1.setEnabledAt(1, true);
            } else {
                tabbedPane1.setEnabledAt(1, false);
            }
        } catch (WebDriverException e) {
            JOptionPane.showMessageDialog(
                    this,
                    "İnternet bağlantısını kontrol edip tekrar deneyin",
                    "Hata!",
                    JOptionPane.ERROR_MESSAGE
            );
        }
    }

    //Staffs' names, titles and avesis links
    List<StaffListItem> staffList = new ArrayList<>();
    List<StaffListItem> brokenStaffList = new ArrayList<>();

    //Clicks on cs tab on department web site
    By computerScienceTab = By.xpath("//*[text()='Bilgisayar Bilimleri']");

    public void clickComputerScienceTab() {
        driver.findElement(computerScienceTab).click();
    }

    //Clicks on hw tab on department web site
    By computerHardwareTab = By.xpath("//*[text()='Bilgisayar Donanımı']");

    public void clickComputerHardwareTab() {
        driver.findElement(computerHardwareTab).click();
    }

    //Clicks on sw tab on department web site
    By computerSoftwareTab = By.xpath("//*[text()='Bilgisayar Yazılımı']");

    public void clickComputerSoftwareTab() {
        driver.findElement(computerSoftwareTab).click();
    }

    //Returns count of staff on page
    By tableOfStaff = By.xpath("//*[@id='myContent2']/table");

    public int counterOfStaff() {
        return driver.findElements(tableOfStaff).size();
    }

    public boolean containsUrl(final List<StaffListItem> list, final String url) {
        return list.stream().filter(o -> o.getUrl().equals(url)).findFirst().isPresent();
    }

    public boolean containsUserName(final List<StaffListItem> list, final String userName) {
        return list.stream().filter(o -> o.getUserName().equals(userName)).findFirst().isPresent();
    }

    //Finds and store staffs' names, titles and avesis links
    DefaultListModel listModel = new DefaultListModel();
    DefaultListModel brokenAvesisLinkModel = new DefaultListModel();

    public void getAcademicStaffList() {
        for (int counter = 1; counter <= counterOfStaff(); counter++) {
            By staffElement = By.xpath("//*[@id='myContent2']/table[" + counter + "]/tbody/tr[1]");
            By avesisLink = By.xpath("//table[" + counter + "]//a[contains(@href, 'avesis')]");
            if (driver.findElement(avesisLink).getAttribute("href").startsWith(avesisMainUrlHttp) || driver.findElement(avesisLink).getAttribute("href").startsWith(avesisMainUrlHttps)) {
                if (!containsUrl(staffList, driver.findElement(avesisLink).getAttribute("href").toString()) && !containsUserName(staffList, driver.findElement(staffElement).getText().toString())) {
                    staffList.add(new StaffListItem(driver.findElement(avesisLink).getAttribute("href").toString(), driver.findElement(staffElement).getText().toString()));
                    staffComboBox.addItem(new StaffListItem(driver.findElement(avesisLink).getAttribute("href").toString(), driver.findElement(staffElement).getText().toString()));
                    listModel.addElement(driver.findElement(staffElement).getText());
                } else {
                    System.out.println("Tekrar edilen değer!\n Öğretim görevlisi: " + driver.findElement(staffElement).getText() + "\t Url: " + driver.findElement(avesisLink).getAttribute("href"));
                    brokenStaffList.add(new StaffListItem(driver.findElement(avesisLink).getAttribute("href").toString(), driver.findElement(staffElement).getText().toString()));
                    brokenAvesisLinkModel.addElement(driver.findElement(staffElement).getText());
                }
            } else {
                System.out.println("Hata!\n Öğretim görevlisi: " + driver.findElement(staffElement).getText() + "\t Url: " + driver.findElement(avesisLink).getAttribute("href"));
                brokenStaffList.add(new StaffListItem(driver.findElement(avesisLink).getAttribute("href").toString(), driver.findElement(staffElement).getText().toString()));
                brokenAvesisLinkModel.addElement(driver.findElement(staffElement).getText());
            }
        }
        staffListForm.setModel(listModel);
        brokenStaffListForm.setModel(brokenAvesisLinkModel);
        saveMapToFile(staffList);
    }

    public String reportBlackList() {
        return nonReachableUrl;
    }

    ExcelFieldItem excelWorkFieldItem = new ExcelFieldItem();

    //Clicks on publications link on avesis page for each staff
    By publicationTab = By.xpath("//span[text()='Yayınlar & Eserler']");

    public class LinkItem {
        public String domainUrl;
        public String rootUrl;
        public String currentUrl;
        public Map<String, List<String>> linkMap;
        public List<String> visitedLinks;
        public List<Integer> nodeIndexes;

        public LinkItem() {
            this.linkMap = new HashMap<>();
            this.currentUrl = "";
            this.rootUrl = "";
            this.domainUrl = "";
            this.visitedLinks = new ArrayList<>();
            this.nodeIndexes = new ArrayList<>();
        }

        public List<String> getVisitedLinks() {
            return visitedLinks;
        }

        public void setVisitedLinks(List<String> visitedLinks) {
            this.visitedLinks = visitedLinks;
        }

        public void insertNodeIndex(int index) {
            this.nodeIndexes.add(index);
        }

        public int getSelectedNodeIndex(int listIndex) {
            return this.nodeIndexes.get(listIndex);
        }

        public void insertVisitedLink(String url) {
            this.visitedLinks.add(url);
        }

        public boolean containsVisitedLink(String url) {
            return this.visitedLinks.contains(url);
        }

        public String getDomainUrl() {
            return domainUrl;
        }

        public String getRootUrl() {
            return rootUrl;
        }

        public void setRootUrl(String rootUrl) {
            this.rootUrl = rootUrl;
        }

        public String getCurrentUrl() {
            return currentUrl;
        }

        public Map<String, List<String>> getLinkMap() {
            return linkMap;
        }

        public void setLinkMap(String rootUrl, List<String> listOfLinks) {
            linkMap.put(rootUrl, listOfLinks);
        }

        public void setCurrentUrl(String currentUrl) {
            this.currentUrl = currentUrl;
        }

        public void setDomainUrl(String domainUrl) {
            this.domainUrl = domainUrl;
        }
    }

    LinkItem linkItem = new LinkItem();
    List<String> links = new ArrayList<>();

    public boolean linkCheckAll(String root, int linkIndex, int nodeIndex) {
        if (nodeIndex > configs.getBrokenLinkNode()) {
            return false;
        }

        System.out.println("List Size: " + links.size() + "\tLink Index :" + linkIndex + "\tNode Index: " + nodeIndex);

        try {
            driver.get(root);
            List<WebElement> linkElement = driver.findElements(By.tagName("a"));
            for (WebElement item : linkElement) {
                String tmpUrl = item.getAttribute("href");
                if (tmpUrl != null && !links.contains(tmpUrl) && (tmpUrl.startsWith("http://") || tmpUrl.startsWith("https://")) && !tmpUrl.equals(bmYildizUrl) && responseCheck(tmpUrl, root) && tmpUrl.contains(bmYildizUrl)) {
                    links.add(tmpUrl);
                    linkItem.insertNodeIndex(nodeIndex);
                }
            }
            linkItem.setLinkMap(root, links);

            if (!linkItem.containsVisitedLink(root)) {
                linkItem.insertVisitedLink(root);
                if (linkIndex < links.size()) {
                    String url = links.get(linkIndex);
                    int tmpNodeIndex = linkItem.getSelectedNodeIndex(linkIndex) + 1;
                    linkItem.insertVisitedLink(url);
                    if (url == null) {
                        System.out.println("URL boş");
                    }
                    return linkCheckAll(url, linkIndex, tmpNodeIndex);
                } else {
                    return false;
                }
            } else {
                linkIndex++;
                if (linkIndex < links.size()) {
                    String url = links.get(linkIndex);
                    int tmpNodeIndex = linkItem.getSelectedNodeIndex(linkIndex) + 1;
                    return linkCheckAll(url, linkIndex, tmpNodeIndex);
                } else {
                    return false;
                }
            }
        } catch (WebDriverException e) {
            JOptionPane.showMessageDialog(
                    this,
                    "İnternet bağlantısını kontrol edip tekrar deneyin",
                    "Hata!",
                    JOptionPane.ERROR_MESSAGE
            );
            return false;
        }
    }

    List<String> checkedLinks = new ArrayList<>();

    DefaultListModel brokenLinkModel = new DefaultListModel();

    public class BrokenLinkExcelModel {
        public String rootPage;
        public String brokenUrl;

        public BrokenLinkExcelModel(String rootPage, String brokenUrl) {
            this.brokenUrl = brokenUrl;
            this.rootPage = rootPage;
        }

        public String getBrokenUrl() {
            return brokenUrl;
        }

        public void setBrokenUrl(String brokenUrl) {
            this.brokenUrl = brokenUrl;
        }

        public String getRootPage() {
            return rootPage;
        }

        public void setRootPage(String rootPage) {
            this.rootPage = rootPage;
        }
    }

    List<BrokenLinkExcelModel> brokenLinkExcelModelList = new ArrayList<>();

    public boolean responseCheck(String url, String root) {
        try {
            if (!checkedLinks.contains((url))) {
                checkedLinks.add((url));
            } else {
                return false;
            }
            HttpURLConnection huc = (HttpURLConnection) (new URL(url).openConnection());
            huc.setRequestMethod("HEAD");
            huc.connect();
            int respCode = huc.getResponseCode();
            if (respCode == 404) {
                System.out.println("Kırık link bulundu: " + url);
                brokenLinkModel.addElement(url);
                brokenLinkExcelModelList.add(new BrokenLinkExcelModel(root, url));
                return false;
            } else {
                //System.out.println(url + " is a valid link");
                return true;
            }
        } catch (MalformedURLException e) {
            return false;
        } catch (IOException e) {
            return false;
        }
    }

    public void setUpDriver() {
        try {
            System.setProperty("webdriver.chrome.driver", "tools\\chromedriver.exe");
            //options for headless chrome

            ChromeOptions options = new ChromeOptions();
            options.addArguments("--headless", "--disable-gpu", "--window-size=1920,1200", "--ignore-certificate-errors", "--silent");
            driver = new ChromeDriver(options);

            //driver = new ChromeDriver();
            //driver.manage().window().maximize();

        } catch (Exception e) {
            JOptionPane.showMessageDialog(
                    this,
                    "Driver bulunamadı",
                    "Hata!",
                    JOptionPane.ERROR_MESSAGE
            );
        }
    }

    public void tearDownDriver() {
        driver.quit();
    }

    //Saves a map object to JSON file
    public void saveMapToFile(List<StaffListItem> map) {
        ObjectMapper mapper = new ObjectMapper();
        try {
            mapper.writeValue(new File("data.json"), map);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void saveConfigToFile(Configs map) {
        ObjectMapper mapper = new ObjectMapper();
        try {
            mapper.writeValue(new File("config.json"), map);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public Configs getConfigs() {
        //Configs configs=new Configs();
        Configs configs = null;
        JSONParser parser = new JSONParser();
        File criteriaFile = new File("config.json");
        boolean criteriaFileIsExist = criteriaFile.exists();
        try {
            if (criteriaFileIsExist) {
                Object obj = parser.parse(new FileReader(criteriaFile));

                JSONObject configObj = (JSONObject) obj;

                JSONArray searchCriterias = (JSONArray) configObj.get("searchCriterias");
                String departmentUrl = (String) configObj.get("departmentWebSite");
                String _domainUrl = (String) configObj.get("domainSite");
                String staffUrl = (String) configObj.get("staffSite");
                int nodeCount = (int) (long) configObj.get("brokenLinkNode");

                List<String> searchCriteriaList = (List<String>) configObj.get("searchCriterias");

                configs = new Configs(departmentUrl, _domainUrl, staffUrl, nodeCount, searchCriteriaList);
                academicStaffUrl = staffUrl;
                bmYildizUrl = departmentUrl;
                domainUrl = _domainUrl;

                departmentWebSite.setText(departmentUrl);
                domainSite.setText(_domainUrl);
                staffSite.setText(staffUrl);
                brokenLinkNode.setText(String.valueOf(nodeCount));
                urlTextField.setText(departmentUrl);


                Iterator<String> iterator = searchCriterias.iterator();
                int index = 0;
                while (iterator.hasNext()) {
                    String searchCriteria = (String) iterator.next();

                    criteriaComboboxSetting.addItem(searchCriteria);
                    publicationCriteriaComboBox.addItem(searchCriteria);
                }
            } else {
                System.out.println("Dosya Yok");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return configs;
    }

    //Return list from JSON file
    public List<StaffListItem> getStaffListFromFile() {
        List<StaffListItem> staffListItemList = new ArrayList<>();
        ObjectMapper mapper = new ObjectMapper();
        List<String> name = new ArrayList<>();
        JSONParser parser = new JSONParser();
        File staffFile = new File("data.json");
        boolean staffFileIsExist = staffFile.exists();
        try {
            if (staffFileIsExist) {
                Object obj = parser.parse(new FileReader(staffFile));

                JSONArray staffDataFile = (JSONArray) obj;
                Iterator<JSONObject> iterator = staffDataFile.iterator();
                while (iterator.hasNext()) {
                    JSONObject jsonObject = (JSONObject) iterator.next();
                    staffListItemList.add(new StaffListItem(jsonObject.get("url").toString(), jsonObject.get("userName").toString()));
                    staffComboBox.addItem(new StaffListItem(jsonObject.get("url").toString(), jsonObject.get("userName").toString()));
                    if (!jsonObject.get("url").toString().contains("All"))
                        listModel.addElement(jsonObject.get("userName").toString());

                }
                staffListForm.setModel(listModel);

            } else {
                System.out.println("Dosya Yok");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return staffListItemList;
    }

    private void saveBrokenLinksToExel(String startUrl) {
        Date date = new Date();
        SimpleDateFormat dateFormat = new SimpleDateFormat("ddMMyyyy_HHmmss");
        String createDateStr = dateFormat.format(date);

        try {
            FileOutputStream out = new FileOutputStream(new File("Kırık Link Raporu_" + createDateStr + ".xlsx"));
            XSSFWorkbook workbook = new XSSFWorkbook();
            XSSFSheet brokenLinkSheet = workbook.createSheet("Kırık Linkler");

            Row firsSheetDescriptionRow = brokenLinkSheet.createRow(0);
            Cell descriptionCell = firsSheetDescriptionRow.createCell(0);
            descriptionCell.setCellValue(startUrl + " Adresinden Başlayan Tarama Raporu");
            brokenLinkSheet.addMergedRegion(CellRangeAddress.valueOf("A1:B1"));
            Iterator<BrokenLinkExcelModel> iterator = brokenLinkExcelModelList.iterator();
            CellStyle style = workbook.createCellStyle(); //Create new style
            style.setWrapText(true); //Set wordwrap
            Row titleRow = brokenLinkSheet.createRow(1);
            Cell titleRoot = titleRow.createCell(0);
            titleRoot.setCellStyle(style); //Apply style to cell
            titleRoot.setCellValue("Bulunduğu Sayfa");
            Cell titleUrl = titleRow.createCell(1);
            titleUrl.setCellStyle(style); //Apply style to cell
            titleUrl.setCellValue("Kırık Link");

            int rownum = 2;

            while (iterator.hasNext()) {
                Row detailRow = brokenLinkSheet.createRow(rownum);

                BrokenLinkExcelModel linkItem = iterator.next();
                Cell cell2 = detailRow.createCell(0);
                cell2.setCellStyle(style); //Apply style to cell
                cell2.setCellValue(linkItem.getRootPage());
                Cell cell3 = detailRow.createCell(1);
                cell3.setCellStyle(style); //Apply style to cell
                cell3.setCellValue(linkItem.getBrokenUrl());
                rownum++;
            }
            brokenLinkSheet.setColumnWidth(0, 20000);//1000==3 ise 60=20 000 15==5000
            brokenLinkSheet.setColumnWidth(1, 20000);
            workbook.write(out);
            out.close();
            downloadBrokenLinkReportButton.setEnabled(false);
            JOptionPane.showMessageDialog(
                    this,
                    "Rapor Dosyası İndirildi",
                    "İşlem Başarılı!",
                    JOptionPane.INFORMATION_MESSAGE
            );
            brokenLinkModel.clear();
            brokenLinkListForm.removeAll();
            brokenLinkExcelModelList.clear();
            scrollPane4.setVisible(true);

        } catch (FileNotFoundException fileNotFoundException) {
            fileNotFoundException.printStackTrace();
        } catch (IOException ioException) {
            ioException.printStackTrace();
        }

    }

    public void saveToExcel(String fileName, String firstSheetName, String firstSheetTitle, String secondSheetName, String secondSheetTitle, ExcelFieldItem excelItem) {
        Date date = new Date();
        SimpleDateFormat dateFormat = new SimpleDateFormat("ddMMyyyy_HHmmss");
        String createDateStr = dateFormat.format(date);

        try {
            FileOutputStream out = new FileOutputStream(new File(fileName + createDateStr + ".xlsx"));
            XSSFWorkbook workbook = new XSSFWorkbook();
            XSSFSheet sciSheet = workbook.createSheet(firstSheetName);
            XSSFSheet detailSheet = workbook.createSheet(secondSheetName);

            Row firsSheetDescriptionRow = sciSheet.createRow(0);
            Cell descriptionCell = firsSheetDescriptionRow.createCell(0);
            descriptionCell.setCellValue(firstSheetTitle);
            sciSheet.addMergedRegion(CellRangeAddress.valueOf("A1:L1"));
            Iterator<String> i = excelItem.getMonthStringsList().iterator();
            Row row = sciSheet.createRow(1);
            Row row1 = sciSheet.createRow(2);

            int cellnum = 0;
            while (i.hasNext()) {
                String monthStr = i.next();
                Cell cell = row.createCell(cellnum);
                cell.setCellValue(monthStr);
                Cell cell1 = row1.createCell(cellnum);
                cell1.setCellValue(excelItem.getMonthlyProjectCountArr()[cellnum]);
                cellnum++;
            }

            Iterator<ProjectDetailItem> j = excelItem.getProjectList().iterator();
            Row secondSheetDescriptionRow = detailSheet.createRow(0);
            Cell secondDescriptionCell = secondSheetDescriptionRow.createCell(0);
            secondDescriptionCell.setCellValue(secondSheetTitle);
            detailSheet.addMergedRegion(CellRangeAddress.valueOf("A1:B1"));
            int rownum = 1;
            CellStyle style = workbook.createCellStyle(); //Create new style
            style.setWrapText(true); //Set wordwrap
            while (j.hasNext()) {
                Row detailRow = detailSheet.createRow(rownum);

                ProjectDetailItem projectItem = j.next();
                Cell cell2 = detailRow.createCell(0);
                cell2.setCellStyle(style); //Apply style to cell
                cell2.setCellValue(projectItem.getProjectTitle());
                Cell cell3 = detailRow.createCell(1);
                cell3.setCellStyle(style); //Apply style to cell
                cell3.setCellValue(projectItem.getPublishDateStr());
                rownum++;
            }
            detailSheet.setColumnWidth(0, 20000);//1000==3 ise 60=20 000 15==5000
            detailSheet.setColumnWidth(1, 5000);
            workbook.write(out);
            out.close();
            downloadPublicationButton.setEnabled(false);
            JOptionPane.showMessageDialog(
                    this,
                    "Rapor Dosyası İndirildi",
                    "İşlem Başarılı!",
                    JOptionPane.INFORMATION_MESSAGE
            );

        } catch (FileNotFoundException fileNotFoundException) {
            fileNotFoundException.printStackTrace();
        } catch (IOException ioException) {
            ioException.printStackTrace();
        }
    }

    public class Configs {
        private String departmentWebSite;
        private String domainSite;
        private String staffSite;
        private int brokenLinkNode;
        private List<String> searchCriterias;

        public Configs() {
            this.departmentWebSite = "";
            this.domainSite = "";
            this.staffSite = "";
            this.brokenLinkNode = 0;
            this.searchCriterias = new ArrayList<>();
        }

        public Configs(String departmentWebSite, String domainSite, String staffSite, int brokenLinkNode, List<String> searchCriterias) {
            this.departmentWebSite = departmentWebSite;
            this.domainSite = domainSite;
            this.staffSite = staffSite;
            this.brokenLinkNode = brokenLinkNode;
            this.searchCriterias = searchCriterias;
        }

        public boolean isConfigTrust() {
            return (this.searchCriterias.size() > 0 && this.domainSite.trim().length() > 0
                    && this.departmentWebSite.trim().length() > 0
                    && this.staffSite.trim().length() > 0 && this.getBrokenLinkNode() > 0);
        }

        public void insertCriteria(String criteria) {
            this.searchCriterias.add(criteria);
        }

        public List<String> updateCriteria(String criteria, int index) {
            this.searchCriterias.set(index, criteria);
            return this.searchCriterias;
        }

        public void deleteCriteria(int index) {
            this.searchCriterias.remove(index);
        }

        public String getDepartmentWebSite() {
            return departmentWebSite;
        }

        public void setDepartmentWebSite(String departmentWebSite) {
            this.departmentWebSite = departmentWebSite;
        }

        public String getDomainSite() {
            return domainSite;
        }

        public void setDomainSite(String domainSite) {
            this.domainSite = domainSite;
        }

        public String getStaffSite() {
            return staffSite;
        }

        public void setStaffSite(String staffSite) {
            this.staffSite = staffSite;
        }

        public int getBrokenLinkNode() {
            return brokenLinkNode;
        }

        public void setBrokenLinkNode(int brokenLinkNode) {
            this.brokenLinkNode = brokenLinkNode;
        }

        public List<String> getSearchCriterias() {
            return searchCriterias;
        }

        public void setSearchCriterias(List<String> searchCriterias) {
            this.searchCriterias = searchCriterias;
        }
    }

    public class StaffListItem {

        private String url;
        private String userName;

        public StaffListItem(String url, String userName) {
            this.url = url;
            this.userName = userName;
        }

        public String getUrl() {
            return url;
        }

        public String getUserName() {
            return userName;
        }

        public void setUrl(String url) {
            this.url = url;
        }

        public void setUserName(String userName) {
            this.userName = userName;
        }

        @Override
        public String toString() {
            return userName;
        }
    }

    class ExcelFieldItem {

        private int totalProjectCount;
        private int[] monthlyProjectCountArr;
        private List<String> monthStringsList;
        private List<ProjectDetailItem> projectList;

        public ExcelFieldItem() {

            int[] array = {0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0};
            List<String> mountStrList = Arrays.asList("Ocak", "Şubat", "Mart", "Nisan", "Mayıs", "Haziran", "Temmuz", "Ağustos", "Eylül", "Ekim", "Kasım", "Aralık");
            this.totalProjectCount = 0;
            this.projectList = new ArrayList<>();
            this.monthlyProjectCountArr = array;
            this.monthStringsList = mountStrList;
        }

        public void clear() {
            this.totalProjectCount = 0;
            this.projectList.clear();
            this.monthlyProjectCountArr = new int[]{0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0};

        }

        public ExcelFieldItem(int totalProjectCount, List<ProjectDetailItem> projectList, int[] monthlyProjectCountArr) {
            this.totalProjectCount = totalProjectCount;
            this.projectList = projectList;
            this.monthlyProjectCountArr = monthlyProjectCountArr;
        }

        public String getMonthString(int index) {
            return monthStringsList.get(index);
        }

        public List<String> getMonthStringsList() {
            return monthStringsList;
        }

        public void setMonthStringsList(List<String> monthStringsList) {
            this.monthStringsList = monthStringsList;
        }

        public int getTotalProjectCount() {
            return totalProjectCount;
        }

        public List<ProjectDetailItem> getProjectList() {
            return projectList;
        }

        public void setProjectList(List<ProjectDetailItem> projectList) {
            this.projectList = projectList;
        }

        public void setTotalProjectCount(int totalProjectCount) {
            this.totalProjectCount = totalProjectCount;
        }

        public void incrementTotalCount() {
            this.totalProjectCount++;
        }

        public void insertListItem(ProjectDetailItem p_item) {
            this.projectList.add(p_item);
        }

        public int[] getMonthlyProjectCountArr() {
            return monthlyProjectCountArr;
        }

        public void setMonthlyProjectCountArr(int[] monthlyProjectCountArr) {
            this.monthlyProjectCountArr = monthlyProjectCountArr;
        }

        public void insertProjectToMount(int p_index) {
            this.monthlyProjectCountArr[p_index]++;
        }

        public boolean containProject(String p_text) {
            boolean flag = false;

            Iterator<ProjectDetailItem> i = this.projectList.iterator();

            while (i.hasNext()) {
                ProjectDetailItem p_item = i.next();
                if (p_text.contains("PlumX Metrics"))
                    p_text = p_text.substring(0, p_text.length() - 14);
                if (p_item.projectTitle.contains(p_text)) {
                    flag = true;
                    break;
                }
            }
            return flag;
        }
    }

    class ProjectDetailItem {
        private String projectTitle;
        private String publishDateStr;

        public ProjectDetailItem(String projectTitle, String publishDateStr) {
            this.projectTitle = projectTitle;
            this.publishDateStr = publishDateStr;
        }

        public String getProjectTitle() {
            return projectTitle;
        }

        public String getPublishDateStr() {
            return publishDateStr;
        }

        public void setProjectTitle(String projectTitle) {
            this.projectTitle = projectTitle;
        }

        public void setPublishDateStr(String publishDateStr) {
            this.publishDateStr = publishDateStr;
        }
    }

    public String convertTurkishCharacters(String str) {
        str = Normalizer.normalize(str, Normalizer.Form.NFD);
        str = str.toLowerCase();
        str = str.replace("I", "İ");
        str = str.replace("ı", "i");
        str = str.replaceAll("[^\\p{ASCII}]", "");
        char ch[] = str.toCharArray();
        for (int i = 0; i < str.length(); i++) {
            if (i == 0 && ch[i] != ' ' ||
                    ch[i] != ' ' && ch[i - 1] == ' ') {
                if (ch[i] >= 'a' && ch[i] <= 'z') {
                    ch[i] = (char) (ch[i] - 'a' + 'A');
                }
            }
        }
        // Convert the char array to equivalent String
        String st = new String(ch);

        return st;
    }

    public void clickPublicationsLink() {
        try {
            driver.findElement(publicationTab).click();
        } catch (Exception e) {
            getWorkListForSecondScheme(yearTextField.getText());
        }
    }

    List<String> worksInfo = new ArrayList<>();
    String fileName;
    String firstSheetName;
    String firstSheetTitle;
    String secondSheetName;

    public void setExcelName() {
        switch (publicationCriteriaComboBox.getSelectedItem().toString()) {
            case "SCI, SSCI ve AHCI İndekslerine Giren Dergilerde Yayınlanan Makaleler":
                fileName = "SCI_";
                firstSheetName = "SCI";
                firstSheetTitle = " Yılına Ait SCI-SCI Expanded Dergilerdeki Makale Sayıları";
                secondSheetName = "1432";
                break;
            case "Diğer Dergilerde Yayınlanan Makaleler":
                fileName = "Makale_";
                firstSheetName = "Makale";
                firstSheetTitle = " Yılına Ait Diğer Dergilerdeki Makale Sayıları";
                secondSheetName = "1128";
                break;
            case "Kitap & Kitap Bölümleri":
                fileName = "Kitap_";
                firstSheetName = "Kitap";
                firstSheetTitle = " Yılına Ait Kitap & Kitap Bölümleri Sayıları";
                secondSheetName = "1129 112Z";
                break;
            case "Hakemli Kongre / Sempozyum Bildiri Kitaplarında Yer Alan Yayınlar":
                fileName = "Bildiri_";
                firstSheetName = "Bildiri";
                firstSheetTitle = " Yılına Ait Bildiri Sayıları";
                secondSheetName = "112X";
                break;
            default:
                fileName = "Diğer_";
                firstSheetName = "Diğer";
                firstSheetTitle = " Yılına Ait Diğer Çalışma Sayıları";
                secondSheetName = "xxxx";
                break;
        }
    }

    private int workList(String selectedStaff) {
        setUpDriver();
        worksInfo.clear();
        String year = yearTextField.getText();
        excelWorkFieldItem.clear();
        setExcelName();
        try {
            if (selectedStaff.contains("All")) {
                for (StaffListItem item : staffList) {
                    if (!item.getUrl().contains("All")) {
                        driver.get(item.getUrl());
                        clickPublicationsLink();
                        blackList.add(reportBlackList());
                        if (!blackList.contains(driver.getCurrentUrl()))
                            System.out.println(driver.getCurrentUrl() + " \t " + getWorkList(convertTurkishCharacters(publicationCriteriaComboBox.getSelectedItem().toString()), year));
                    }
                }
            } else {
                driver.get(selectedStaff);
                clickPublicationsLink();
                blackList.add(reportBlackList());
                if (!blackList.contains(driver.getCurrentUrl()))
                    System.out.println(driver.getCurrentUrl() + " \t " + getWorkList(convertTurkishCharacters(publicationCriteriaComboBox.getSelectedItem().toString()), year));
            }
        } catch (WebDriverException e) {
            JOptionPane.showMessageDialog(
                    this,
                    "İnternet bağlantısını kontrol edip tekrar deneyin",
                    "Hata!",
                    JOptionPane.ERROR_MESSAGE
            );
        }
        return excelWorkFieldItem.getTotalProjectCount();
    }

    public void getWorkListForSecondScheme(String year) {
        WebElement p_element;
        if (driver.findElement(By.xpath("//*[@id='content']/div[contains(.,'" + publicationCriteriaComboBox.getSelectedItem().toString() + "')]/div[1]")).getText().contains(publicationCriteriaComboBox.getSelectedItem().toString())) {
            List<WebElement> workListForSecondScheme = driver.findElements(By.xpath("//*[@id='content']/div[contains(.,'" + publicationCriteriaComboBox.getSelectedItem().toString() + "')]/div[@class='span9']/div"));
            Iterator<WebElement> it = workListForSecondScheme.iterator();
            while (it.hasNext()) {
                p_element = it.next();
                if (p_element.getText().contains(year)) {
                    if (!excelWorkFieldItem.containProject(p_element.getText())) {
                        excelWorkFieldItem.insertListItem(new ProjectDetailItem(p_element.getText(), excelWorkFieldItem.getMonthString(5) + " " + year));
                        excelWorkFieldItem.incrementTotalCount();
                        excelWorkFieldItem.insertProjectToMount(5);
                    }
                }
            }
        }
    }

    public int getWorkList(String workTitle, String year) {
        WebElement p_element;
        int pubs_wrapper_index = 0;
        boolean isWorkTagFound = false;
        try {
            List<WebElement> title = driver.findElements(By.xpath("//h4"));
            Iterator<WebElement> it = title.iterator();

            while (it.hasNext()) {
                p_element = it.next();
                pubs_wrapper_index++;
                if (convertTurkishCharacters(p_element.getText()).equals((workTitle))) {

                    isWorkTagFound = true;
                    break;
                }
            }
            if (isWorkTagFound && pubs_wrapper_index <= title.size()) {
                List<WebElement> bookList = driver.findElements(By.xpath("//div[contains(@class,'pubs-wrapper')][" + pubs_wrapper_index + "]/div[@class='pub-item with-icon']"));
                Iterator<WebElement> iterator = bookList.iterator();

                while (iterator.hasNext()) {
                    p_element = iterator.next();
                    if (p_element.getText().contains(year)) {

                        if (!excelWorkFieldItem.containProject(p_element.getText())) {
                            excelWorkFieldItem.insertListItem(new ProjectDetailItem(p_element.getText(), excelWorkFieldItem.getMonthString(5) + " " + year));
                            excelWorkFieldItem.incrementTotalCount();
                            excelWorkFieldItem.insertProjectToMount(5);
                        }
                    }
                }
            } else {
                System.out.println("İlgili öğretim görevlisinin sisteme girilmiş bir çalışması bulunmamaktadır: " + driver.getCurrentUrl());
            }
        } catch (Exception e) {
            System.out.println("İlgili öğretim görevlisinin sisteme girilmiş bir çalışması bulunmamaktadır: " + driver.getCurrentUrl());
        }
        return excelWorkFieldItem.getTotalProjectCount();
    }

}
