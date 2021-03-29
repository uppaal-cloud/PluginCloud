package com.uppaal.cloud.ui;

import com.uppaal.cloud.util.UppaalCloudAPIClient;
import com.uppaal.cloud.util.UppaalCloudJob;
import com.uppaal.engine.Engine;
import com.uppaal.engine.Parser;
import com.uppaal.engine.Problem;
import com.uppaal.model.LayoutVisitor;
import com.uppaal.model.core2.Document;
import com.uppaal.model.core2.PrototypeDocument;
import com.uppaal.model.io2.XMLWriter;
import com.uppaal.model.io2.XMLReader;
import com.uppaal.model.system.UppaalSystem;
import com.uppaal.model.system.symbolic.SymbolicTrace;
import com.uppaal.plugin.Plugin;
import com.uppaal.plugin.PluginWorkspace;
import com.uppaal.plugin.Registry;
import com.uppaal.plugin.Repository;
import org.apache.commons.io.IOUtils;
import org.apache.commons.io.output.ByteArrayOutputStream;

import javax.print.attribute.standard.JobName;
import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
// import com.uppaal.model.core2.*;

@SuppressWarnings("serial")
public class MainUI extends JPanel implements Plugin, PluginWorkspace, PropertyChangeListener, Callback {
    protected static final String SELECT = "com/uppaal/resource/images/selectedQuery.gif";
    protected static final String OKAY = "com/uppaal/resource/images/queryOkay.gif";
    protected static final String NOT_OKAY = "com/uppaal/resource/images/queryNotOkay.gif";
    protected static final String UNKNOWN = "com/uppaal/resource/images/queryUnknown.gif";
    protected static final String MAYBE_OKAY = "com/uppaal/resource/images/queryMaybeOkay.gif";
    protected static final String MAYBE_NOT_OKAY = "com/uppaal/resource/images/queryMaybeNotOkay.gif";

    private ImageIcon getIcon(String resource) {
        return new ImageIcon(getClass().getClassLoader().getResource(resource));
    }

    private Repository<Document> docr;

    public static Repository<SymbolicTrace> getTracer() {
        return tracer;
    }

    private static Repository<SymbolicTrace> tracer;

    public static Repository<ArrayList<Problem>> getProblemr() {
        return problemr;
    }

    private static Repository<ArrayList<Problem>> problemr;

    public static Repository<UppaalSystem> getSystemr() {
        return systemr;
    }

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
        jobsPanel = new JobsView(this.apiClient, this, docr);
        add(jobsPanel);

        // Show default behavior
        this.callback(UiAction.LOGGED_OUT);

        docr.addListener(this);
        KeyboardFocusManager.getCurrentKeyboardFocusManager().addKeyEventDispatcher(e -> {
            if (e.getKeyCode() == KeyEvent.VK_F6 && e.getID() == KeyEvent.KEY_PRESSED) {
                dialogThread = new Thread(() -> {
                    int option = JOptionPane.showOptionDialog(getRootPane(), "Sanity checker is running",
                            "Sanity checker", JOptionPane.DEFAULT_OPTION, JOptionPane.INFORMATION_MESSAGE, null,
                            new String[] { "Ok", "Do not show again" }, "Ok");
                    if (option == 1) {
                        doNotShow = true;
                    }
                });
                if (!doNotShow) {
                    dialogThread.start();
                }
                return true;
            }
            return false;
        });
    }

    private boolean doNotShow = false;

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

    public String loadFromFile() throws Exception {
        return new String(Files.readAllBytes(Paths.get("/home/tsvetomir/projects/uppaal/uppaal64-4.1.24/demo/to_test_train_gate.xtr")));
    }

    private void setTrace() {
        int option = JOptionPane.showConfirmDialog(getRootPane(),"Loading a trace will overwrite existing once. Are you sure?");
        if(option != JOptionPane.YES_OPTION){
//            textArea.setText("Trace discarded");
            return;
        }

        String res;
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try {
            res = loadFromFile();
            SymbolicTrace st = new Parser(IOUtils.toInputStream(res, StandardCharsets.UTF_8)).parseXTRTrace(systemr.get());

            tracer.set(st);
        } catch (Exception e) {
            res = e.getMessage();
        }

//        textArea.setText(res);
    }

    private void loadLastJob() {
        int option = JOptionPane.showConfirmDialog(getRootPane(),"Loading a model will overwrite existing once. Are you sure?");
        if(option != JOptionPane.YES_OPTION){
//            textArea.setText("Model not loaded");
            return;
        }

        List<UppaalCloudJob> jobs = apiClient.getJobs();
        UppaalCloudJob job = jobs.get(jobs.size() - 1);

        String res = "All good";
        try {
            PrototypeDocument pd = new PrototypeDocument();
            Document doc = new XMLReader(IOUtils.toInputStream(job.xml, StandardCharsets.UTF_8)).parse(pd);
            doc.acceptSafe(new LayoutVisitor());

//            ArrayList<Problem> problems = new ArrayList<Problem>();
//            Engine engine = new Engine();
//            UppaalSystem sys = engine.getSystem(doc, problems);
//            if (!problems.isEmpty()) {
//                textArea.setText("There are problems with the document");
//                return;
//            }

            // Try replacing the systemr instead
            docr.set(doc);
        } catch (Exception e) {
            res = e.getMessage();
        }

//        textArea.setText(res);
    }

    private Thread checkThread;
    private Thread dialogThread;

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
