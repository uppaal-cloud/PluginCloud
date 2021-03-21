package com.uppaal.cloud.ui;

import com.uppaal.cloud.util.UppaalCloudAPIClient;
import com.uppaal.cloud.util.UppaalCloudJob;
import com.uppaal.engine.Parser;
import com.uppaal.engine.Problem;
import com.uppaal.model.core2.Document;
import com.uppaal.model.io2.XMLWriter;
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
public class MainUI extends JPanel implements Plugin, PluginWorkspace, PropertyChangeListener {
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

    private JPanel credentialsPanel;

    private JButton runButton;
    private JButton traceButton;
    private JButton loginButton;
    private JTextArea textArea;
    private JTextField userNameField;
    private JTextField passwordField;
    private JTextField jobNameField;
    private JTextField jobDescriptionField;
    private boolean selected;
    private double zoom;
    private String username;
    private String password;

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

        credentialsPanel = new JPanel();
        credentialsPanel.add(new JLabel("Username: "));
        userNameField = new JTextField("");
        userNameField.setPreferredSize(new Dimension(128, userNameField.getPreferredSize().height));
        credentialsPanel.add(userNameField);

        credentialsPanel.add(new JLabel("Password: "));
        passwordField = new JPasswordField("");
        passwordField.setPreferredSize(new Dimension(128, passwordField.getPreferredSize().height));
        credentialsPanel.add(passwordField);

        loginButton = new JButton("Login");
        loginButton.addActionListener(e -> login());
        credentialsPanel.add(loginButton);
        add(credentialsPanel);

        textArea = new JTextArea(4, 20);
        JScrollPane scrollPane = new JScrollPane(textArea);
        textArea.setEditable(false);
        add(textArea);

        JPanel debugPanel = new JPanel();
        runButton = new JButton("Load and show the XML model");
        runButton.addActionListener(e -> textArea.setText(getXML()));
        debugPanel.add(runButton);

        traceButton = new JButton("Load and show a trace file");
        traceButton.addActionListener(e -> setTrace());
        debugPanel.add(traceButton);

        JButton showToken = new JButton("Show current token");
        showToken.addActionListener(e -> {
            textArea.setText(apiClient.getToken());
        });
        debugPanel.add(showToken);
        add(debugPanel);

        JButton getJobs = new JButton("Get last job");
        getJobs.addActionListener(e -> {
            List<UppaalCloudJob> jobs = apiClient.getJobs();
            UppaalCloudJob job = jobs.get(jobs.size() - 1);
            String out = "Total jobs: " + jobs.size() +
                    "\nName: " + job.name +
                    "\nDescription: " + job.description +
                    "\nstatus: " + job.status +
                    "\nfinished on " + job.end_time;
            textArea.setText(out);
        });
        add(getJobs);

        JPanel jobsPanel = new JPanel();
        jobsPanel.add(new JLabel("Job name: "));
        jobNameField = new JTextField("");
        jobNameField.setPreferredSize(new Dimension(128, jobNameField.getPreferredSize().height));
        jobsPanel.add(jobNameField);

        jobsPanel.add(new JLabel("Job description: "));
        jobDescriptionField = new JTextField("");
        jobDescriptionField.setPreferredSize(new Dimension(128, jobDescriptionField.getPreferredSize().height));
        jobsPanel.add(jobDescriptionField);

        JButton pushJob = new JButton("Run current job in the cloud");
        pushJob.addActionListener(e -> {
            UppaalCloudJob job = new UppaalCloudJob();
            job.name = jobNameField.getText();
            job.description = jobDescriptionField.getText();
            job.xml = getXML();
            String jobId = apiClient.pushJob(job);
            textArea.setText("Job ID: "+jobId);
        });
        jobsPanel.add(pushJob);

        add(jobsPanel);

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

    private String getXML() {
        Document d = docr.get();
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

    private void setTrace() {
        int option = JOptionPane.showConfirmDialog(getRootPane(),"Loading a trace will overwrite existing once. Are you sure?");
        if(option != JOptionPane.YES_OPTION){
            textArea.setText("Trace discarded");
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

        textArea.setText(res);
    }

    private void login() {
        username = userNameField.getText();
        password = passwordField.getText();
        textArea.setText("Username: " + username + " password: " + password);

        apiClient.setCredentials(username, password);
        if(apiClient.login()) {
            // Hide the credentials panel
            credentialsPanel.setVisible(false);
            // TODO: show panel again if a request fails
            textArea.setText("Token is: "+apiClient.getToken());
        } else {
            // Failed to login
        }
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
}
