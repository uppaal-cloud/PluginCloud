package com.uppaal.cloud.ui;

import com.uppaal.cloud.util.UppaalCloudAPIClient;
import com.uppaal.cloud.util.UppaalCloudJob;

import javax.swing.*;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.DefaultTableModel;
import java.util.List;

public class RemoteJobsView extends JPanel {
    private final UppaalCloudAPIClient apiClient;
    private List<UppaalCloudJob> jobs;
    private JLabel total = new JLabel("");
    private JTable table;
    private final DefaultTableModel tblModel;

    public RemoteJobsView(UppaalCloudAPIClient client) {
        super();
        this.apiClient = client;

        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));

        add(total);

        tblModel = new DefaultTableModel();
        tblModel.addColumn("ID");
        tblModel.addColumn("Job Name");
        tblModel.addColumn("Started On");
        tblModel.addColumn("Status");

        table = new JTable(tblModel){
            public boolean isCellEditable(int row, int column){
                return false;
            }
        };
        table.setFillsViewportHeight(true);
        table.getSelectionModel().addListSelectionListener(new ListSelectionListener(){
            public void valueChanged(ListSelectionEvent event) {
                // Row index starts from 0
                int rowIdx = table.getSelectedRow();
                UppaalCloudJob job = jobs.get(jobs.size()-1-rowIdx);
                JOptionPane.showConfirmDialog(getRootPane(),"Job name: " + job.name + " status: " + job.status);
            }
        });
        JScrollPane sp = new JScrollPane(table);
        add(sp);
    }

    public void refreshView() {
        jobs = apiClient.getJobs();
        total.setText("Total jobs: " + jobs.size());
        // Clean-up the table
        tblModel.setRowCount(0);
        int id = 1;
        for(int i = (jobs.size()-1); i >= 0; i--){
            // Show newest first
            UppaalCloudJob job = jobs.get(i);
            tblModel.addRow(new Object[]{id, job.name, job.start_time, job.status});
            id++;
        }
    }
}
