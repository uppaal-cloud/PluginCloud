package com.uppaal.cloud.ui;

import com.uppaal.cloud.util.UppaalCloudAPIClient;
import com.uppaal.cloud.util.UppaalCloudJob;
import com.uppaal.engine.Problem;
import com.uppaal.model.LayoutVisitor;
import com.uppaal.model.core2.Document;
import com.uppaal.model.core2.PrototypeDocument;
import com.uppaal.model.io2.XMLReader;
import com.uppaal.model.system.UppaalSystem;
import com.uppaal.model.system.symbolic.SymbolicTrace;
import com.uppaal.plugin.Plugin;
import com.uppaal.plugin.PluginWorkspace;
import com.uppaal.plugin.Registry;
import com.uppaal.plugin.Repository;
import org.apache.commons.io.IOUtils;

import javax.swing.*;
import java.awt.*;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public class MainUI extends JPanel implements Plugin, PluginWorkspace, PropertyChangeListener, Callback {
    private Repository<Document> docr;
    private static Repository<SymbolicTrace> tracer;
    private static Repository<ArrayList<Problem>> problemr;
    private static Repository<UppaalSystem> systemr;

    private LoginView loginPanel;
    private JobsView jobsPanel;

    private boolean selected;
    private double zoom;

    private UppaalCloudAPIClient apiClient = new UppaalCloudAPIClient();

    private final PluginWorkspace[] workspaces = new PluginWorkspace[1];

    public MainUI() {
    }

    @SuppressWarnings("unchecked")
    public MainUI(Registry r) {
        super();
        docr = r.getRepository("EditorDocument");
        tracer = r.getRepository("SymbolicTrace");
        problemr = r.getRepository("EditorProblems");
        systemr = r.getRepository("SystemModel");
        workspaces[0] = this;
        r.addListener(this);

        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));

        // Create login layout
        loginPanel = new LoginView(this.apiClient, this);
        add(loginPanel);

        // Create jobs layout
        jobsPanel = new JobsView(this.apiClient, this, docr, systemr, tracer);
        add(jobsPanel);

        // Show default behavior
        this.callback(UiAction.LOGGED_OUT);

        docr.addListener(this);
    }

    @Override
    public Icon getIcon() {
        return null;
    }

    @Override
    public String getTitleToolTip() {
        return "Model checking as a Service";
    }

    @Override
    public Component getComponent() {
        return new JScrollPane(this);
    }

    @Override
    public int getDevelopmentIndex() {
        return 350;
    }

    @Override
    public boolean getCanZoom() {
        return false;
    }

    @Override
    public boolean getCanZoomToFit() {
        return false;
    }

    @Override
    public double getZoom() {
        return zoom;
    }

    @Override
    public void setZoom(double value) {
        zoom = value;
    }

    @Override
    public void zoomToFit() {
    }

    @Override
    public void zoomIn() {
    }

    @Override
    public void zoomOut() {
    }

    @Override
    public void setActive(boolean selected) {
        this.selected = selected;
    }

    private void loadLastJob() {
        int option = JOptionPane.showConfirmDialog(getRootPane(),"Loading a model will overwrite existing once. Are you sure?");
        if(option != JOptionPane.YES_OPTION){
            return;
        }

        List<UppaalCloudJob> jobs = apiClient.getJobs();
        UppaalCloudJob job = jobs.get(jobs.size() - 1);

        String res = "All good";
        try {
            PrototypeDocument pd = new PrototypeDocument();
            Document doc = new XMLReader(IOUtils.toInputStream(job.xml, StandardCharsets.UTF_8)).parse(pd);
            doc.acceptSafe(new LayoutVisitor());
            docr.set(doc);

//            ArrayList<Problem> problems = new ArrayList<Problem>();
//            Engine engine = new Engine();
//            UppaalSystem sys = engine.getSystem(doc, problems);
//            if (!problems.isEmpty()) {
//                textArea.setText("There are problems with the document");
//                return;
//            }

            // Try replacing the systemr instead
        } catch (Exception e) {
            res = e.getMessage();
        }

//        textArea.setText(res);
    }

    @Override
    public PluginWorkspace[] getWorkspaces() {
        return workspaces;
    }

    @Override
    public String getTitle() {
        return "UPPAAL Cloud";
    }

    @Override
    public void propertyChange(PropertyChangeEvent evt) {
        setActive(selected);
    }

    @Override
    public void callback(UiAction action) {
        switch (action) {
            case LOGGED_IN:
                // Switch to Jobs view
                loginPanel.setVisible(false);
                jobsPanel.setVisible(true);
                jobsPanel.refreshView();
                break;
            case LOGGED_OUT:
                // Hide jobs view and show credentials view
                jobsPanel.setVisible(false);
                loginPanel.setVisible(true);
                loginPanel.refreshView();
                break;
        }
    }
}
