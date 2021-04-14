package com.uppaal.cloud.ui;

import com.uppaal.cloud.util.UppaalCloudAPIClient;
import com.uppaal.cloud.util.UppaalCloudJob;
import com.uppaal.cloud.util.UppaalCloudJobQuery;
import com.uppaal.engine.Parser;
import com.uppaal.model.system.UppaalSystem;
import com.uppaal.model.system.symbolic.SymbolicTrace;
import com.uppaal.plugin.Repository;
import org.apache.commons.io.IOUtils;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.List;
import java.util.Objects;

public class RemoteJobsView extends JPanel {
    private final UppaalCloudAPIClient apiClient;
    private Repository<UppaalSystem> systemr;
    private Repository<SymbolicTrace> tracer;
    private SymbolicTrace symTrace;

    private List<UppaalCloudJob> jobs;
    private UppaalCloudJob selectedJob;

    private final JLabel total = new JLabel("");

    private final JTable table;
    private final DefaultTableModel tableModel;
    private final JScrollPane tablePane;

    private final JTable resultTable;
    private final DefaultTableModel resultTableModel;
    private final JScrollPane resultTablePane;

    private final JPanel statsPanel = new JPanel();
    private final JLabel statsList = new JLabel("");
    private final JButton listJobs = new JButton("Back to all jobs");

    public RemoteJobsView(UppaalCloudAPIClient client, Repository<UppaalSystem> sys,
                          Repository<SymbolicTrace> trace) {
        super();
        this.apiClient = client;
        this.systemr = sys;
        this.tracer = trace;

        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        add(total);

        listJobs.addActionListener(e -> refreshView());

        statsPanel.setLayout(new BoxLayout(statsPanel, BoxLayout.X_AXIS));
        statsPanel.setAlignmentX(Component.CENTER_ALIGNMENT);
        statsPanel.add(statsList);
        statsPanel.add(listJobs);
        statsPanel.setVisible(false);
        add(statsPanel);

        tableModel = new DefaultTableModel();
        tableModel.addColumn("ID");
        tableModel.addColumn("Job Name");
        tableModel.addColumn("Started On");
        tableModel.addColumn("Status");
        tableModel.addColumn("Verified queries");

        table = new JTable(tableModel){
            public boolean isCellEditable(int row, int column){
                return false;
            }
        };

        // Center render columns
        DefaultTableCellRenderer cellRenderer = new DefaultTableCellRenderer();
        cellRenderer.setHorizontalAlignment(JLabel.CENTER);
        table.getColumnModel().getColumn(0).setCellRenderer(cellRenderer);
        table.getColumnModel().getColumn(3).setCellRenderer(cellRenderer);
        table.getColumnModel().getColumn(4).setCellRenderer(cellRenderer);

        // set the column width for each column
        table.getColumnModel().getColumn(0).setMaxWidth(40); // ID
        table.getColumnModel().getColumn(1).setPreferredWidth(70); // Name
        table.getColumnModel().getColumn(2).setPreferredWidth(70); // Date
        table.getColumnModel().getColumn(3).setPreferredWidth(40); // Status
        table.getColumnModel().getColumn(4).setPreferredWidth(20); // Verified queries

        table.setFocusable(false);
        table.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent me) {
                if (me.getClickCount() == 2) {
                    if(!tablePane.isVisible()) {
                        return;
                    }
                    // Row index starts from 0
                    int rowIdx = table.getSelectedRow();
                    if(rowIdx == -1) {
                        // No selected row
                        return;
                    }
                    selectedJob = jobs.get(jobs.size()-1-rowIdx);
                    switchToResult();
                }
            }
        });

        tablePane = new JScrollPane(table);
        add(tablePane);

        // **************************************************************
        // Setup result table
        resultTableModel = new DefaultTableModel();
        resultTableModel.addColumn("ID");
        resultTableModel.addColumn("Formula");
        resultTableModel.addColumn("Result");
        resultTableModel.addColumn("Trace");

        resultTable = new JTable(resultTableModel){
            public boolean isCellEditable(int row, int column){
                return false;
            }
        };

        // Center some columns
        resultTable.getColumnModel().getColumn(0).setCellRenderer(cellRenderer);
        resultTable.getColumnModel().getColumn(2).setCellRenderer(cellRenderer);
        resultTable.getColumnModel().getColumn(3).setCellRenderer(cellRenderer);

        resultTable.getColumnModel().getColumn(0).setMaxWidth(40); // ID
        resultTable.getColumnModel().getColumn(1).setPreferredWidth(150); // Formula
        resultTable.getColumnModel().getColumn(2).setMaxWidth(70); // Result
        resultTable.getColumnModel().getColumn(3).setMaxWidth(70); // Trace

        resultTable.setFocusable(false);
        resultTable.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent me) {
                if (me.getClickCount() == 2) {     // To detect double click events
                    // Row index starts from 0
                    int rowIdx = resultTable.getSelectedRow();
                    if(rowIdx == -1) {
                        // No selected row
                        return;
                    }
                    String tr = selectedJob.queries.get(rowIdx).trace;
                    if(Objects.isNull(tr) || tr.isEmpty()) {
                        JOptionPane.showMessageDialog(getRootPane(), "Query result does not have a trace", "Info", JOptionPane.INFORMATION_MESSAGE);
                    } else {
                        setTrace(tr);
                    }
                }
            }
        });

        resultTablePane = new JScrollPane(resultTable);
        resultTablePane.setVisible(false);
        add(resultTablePane);
    }

    public void refreshView() {
        tablePane.setVisible(true);
        total.setVisible(true);
        resultTablePane.setVisible(false);
        statsPanel.setVisible(false);

        jobs = apiClient.getJobs();
        total.setText("Total jobs: " + jobs.size());
        // Clean-up the table
        tableModel.setRowCount(0);
        int id = 1;
        for(int i = (jobs.size()-1); i >= 0; i--){
            // Show newest first
            UppaalCloudJob job = jobs.get(i);
            // Get number of queries and number of satisfied queries
            int numQueries = job.queries.size();
            int numVerifiedQueries = (int) job.queries.stream()
                    .filter(query -> !Objects.isNull(query.result) &&
                            query.result.equals("satisfied"))
                    .count();
            tableModel.addRow(new Object[]
                    {id, job.name, job.start_time, job.status, numVerifiedQueries+"/"+numQueries});
            id++;
        }

        // Re-render
        revalidate();
        repaint();
    }

    private void switchToResult() {
        // Hide current table
        tablePane.setVisible(false);

        // Clean-up the table
        resultTableModel.setRowCount(0);
        for(int i = 0; i < selectedJob.queries.size(); i++){
            UppaalCloudJobQuery q = selectedJob.queries.get(i);
            resultTableModel.addRow(new Object[]
                    {q.id, q.formula, q.result, (!Objects.isNull(q.trace) && !q.trace.isEmpty())});
        }
        resultTablePane.setVisible(true);

        // Get elapsed time
        Date endDate = new Date();
        if(!Objects.isNull(selectedJob.end_time)) {
            endDate = selectedJob.end_time;
        }

        long diffInMillies = Math.abs(endDate.getTime() - selectedJob.start_time.getTime());
        String elapsedTime = (diffInMillies / 1000.0) + "";

        // Setup stats
        String formattedText = "<html>" +
                "<style> " +
                "table{margin: 10px 30px; font-size: 10px}" +
                ".c1{color: #777777}" +
                "tr{padding: 0; margin: 0; height: 10px; line-height: 10px;}" +
                "td{padding: 0; margin: 0; height: 10px; line-height: 10px;}" +
                "</style> " +
                "<table>" +
                "<tr><td class='c1'>Job name:</td><td>"+selectedJob.name+"</td></tr>" +
                "<tr><td class='c1'>Job description:</td><td>"+selectedJob.description+"</td></tr>" +
                "<tr><td class='c1'>Status:</td><td>"+selectedJob.status+"</td></tr>" +
                "<tr><td class='c1'>CPU usage:</td><td>"+selectedJob.usage.cpu+" ms</td></tr>" +
                "<tr><td class='c1'>RAM usage:</td><td>"+selectedJob.usage.ram+" bytes</td></tr>" +
                "<tr><td class='c1'>Duration:</td><td>"+elapsedTime+" s</td></tr>" +
                "</table></html>";

        statsList.setText(formattedText);
        statsPanel.setVisible(true);
        total.setVisible(false);

        // Re-render
        revalidate();
        repaint();
    }

    private void setTrace(String tr) {
        int option = JOptionPane.showConfirmDialog(getRootPane(),"Loading a trace will overwrite existing once. Are you sure?");
        if(option != JOptionPane.YES_OPTION){
            return;
        }

        try {
            symTrace = new Parser(IOUtils.toInputStream(tr, StandardCharsets.UTF_8)).parseXTRTrace(systemr.get());
            // Known issue - tracer tab needs to be active at least once before replacing
            tracer.set(symTrace);
        } catch (Exception e) {
            JOptionPane.showMessageDialog(getRootPane(),"Failed to load trace: " + e.getMessage(),"Alert", JOptionPane.WARNING_MESSAGE);
        }
    }
}
