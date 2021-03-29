package com.uppaal.cloud.ui;

import com.uppaal.cloud.util.UppaalCloudAPIClient;
import com.uppaal.cloud.util.UppaalCloudJob;
import com.uppaal.model.core2.Document;
import com.uppaal.model.io2.XMLWriter;
import com.uppaal.plugin.Repository;
import org.apache.commons.io.output.ByteArrayOutputStream;

import javax.swing.*;
import java.awt.*;
import java.nio.charset.StandardCharsets;

public class LocalJobsView extends JPanel {
    private UppaalCloudAPIClient apiClient;
    private Repository<Document> docr;
    private JTextField jobNameField;
    private JTextField jobDescriptionField;
    private Callback jobPushedCallback;

    public LocalJobsView(UppaalCloudAPIClient client, Repository<Document> doc, Callback callback) {
        super();
        this.apiClient = client;
        this.docr = doc;
        this.jobPushedCallback = callback;

//        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        setLayout(new FlowLayout());
        setAlignmentX(Component.CENTER_ALIGNMENT);
        setAlignmentY(Component.CENTER_ALIGNMENT);

        setPreferredSize(new Dimension(700, 180));
        setMaximumSize(new Dimension(700, 180));

        add(new JLabel("This tab allows you to push the current model and queries to UPPAAL Cloud."));
        add(new JSeparator());

        JPanel jobNameRow = new JPanel();
        jobNameRow.add(new JLabel("Job name:"));
        jobNameField = new JTextField("");
        jobNameField.setPreferredSize(new Dimension(128, 30));
        jobNameRow.add(jobNameField);
        add(jobNameRow);

        JPanel jobDescriptionRow = new JPanel();
        jobDescriptionRow.add(new JLabel("Job Description:"));
        jobDescriptionField = new JTextField("");
        jobDescriptionField.setPreferredSize(new Dimension(400, 30));
        jobDescriptionRow.add(jobDescriptionField);
        add(jobDescriptionRow);

        JButton pushJob = new JButton("Push to cloud");
        pushJob.addActionListener(e -> pushJob());
        add(pushJob);
    }

    public void refreshView() {
        jobNameField.setText("");
        jobDescriptionField.setText("");
    }

    private void pushJob() {
        UppaalCloudJob job = new UppaalCloudJob();
        job.name = jobNameField.getText();
        job.description = jobDescriptionField.getText();
        job.xml = getXML();
        String jobId = apiClient.pushJob(job);
        // Add a small delay
        try { Thread.sleep(100); } catch (Exception e) {};
        jobPushedCallback.callback(UiAction.JOB_PUSHED);
    }

    private String getXML() {
        Document d = this.docr.get();
        String res;

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try {
            new XMLWriter(out).visitDocument(d);
            res = new String(out.toByteArray(), StandardCharsets.UTF_8);
        } catch (Exception e) {
            res = e.getMessage();
        }
        return res;
    }
}
