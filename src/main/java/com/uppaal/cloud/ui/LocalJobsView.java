package com.uppaal.cloud.ui;

import com.uppaal.cloud.util.UppaalCloudAPIClient;
import com.uppaal.cloud.util.UppaalCloudJob;
import com.uppaal.model.core2.Document;
import com.uppaal.model.io2.XMLWriter;
import com.uppaal.plugin.Repository;
import org.apache.commons.io.output.ByteArrayOutputStream;

import javax.swing.*;
import javax.swing.border.Border;
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

        setLayout(new GridBagLayout());
        GridBagConstraints c = new GridBagConstraints();

        JLabel icon = new JLabel(new ImageIcon(getClass().getResource("/cloud.png")));
        c.gridx = 1;
        c.gridy = 0;
        c.gridwidth = 1;
        add(icon, c);

        c.gridx = 0;
        c.gridy = 1;
        c.gridwidth = 3;
        JLabel info = new JLabel("This tab allows you to push the current model and queries to UPPAAL Cloud.");
        info.setPreferredSize(new Dimension(info.getPreferredSize().width, 30));
        add(info, c);

        c.gridx = 0;
        c.gridy = 2;
        c.gridwidth = 1;
        add(new JLabel("Name:", JLabel.LEFT), c);
        jobNameField = new JTextField("");
        jobNameField.setPreferredSize(new Dimension(350, 30));

        c.gridx = 1;
        c.gridy = 2;
        c.gridwidth = 1;
        add(jobNameField, c);

        c.gridx = 0;
        c.gridy = 3;
        c.gridwidth = 1;
        add(new JLabel("Description:", JLabel.LEFT), c);
        jobDescriptionField = new JTextField("");
        jobDescriptionField.setPreferredSize(new Dimension(350, 30));

        c.gridx = 1;
        c.gridy = 3;
        c.gridwidth = 1;
        add(jobDescriptionField, c);

        JButton pushJob = new JButton("Push to cloud");
        pushJob.addActionListener(e -> pushJob());

        c.gridx = 1;
        c.gridy = 4;
        c.gridwidth = 1;
        add(pushJob, c);
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
