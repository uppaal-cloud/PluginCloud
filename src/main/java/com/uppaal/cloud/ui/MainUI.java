package com.uppaal.cloud.ui;

import com.uppaal.cloud.util.UppaalCloudAPIClient;
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

import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;

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

    private JButton runButton;
    private JButton traceButton;
    private JButton loginButton;
    private JTextArea textArea;
    private JTextField userNameField;
    private JTextField passwordField;
    private boolean selected;
    private double zoom;
    private String username;
    private String password;

    private UppaalCloudAPIClient apiClient;

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

        JPanel jp = new JPanel();
        jp.add(new JLabel("Username: "));
        userNameField = new JTextField("");
        userNameField.setPreferredSize(new Dimension(128, userNameField.getPreferredSize().height));
        jp.add(userNameField);

        jp.add(new JLabel("Password: "));
        passwordField = new JTextField("");
        passwordField.setPreferredSize(new Dimension(128, passwordField.getPreferredSize().height));
        jp.add(passwordField);

        loginButton = new JButton("Login");
        loginButton.addActionListener(e -> login());
        jp.add(loginButton);
        add(jp);

        textArea = new JTextArea(4, 20);
        JScrollPane scrollPane = new JScrollPane(textArea);
        textArea.setEditable(false);
        add(textArea);

        runButton = new JButton("Load and show the XML model");
        runButton.addActionListener(e -> doCheck(true));
        add(runButton);

        traceButton = new JButton("Load and show a trace file");
        traceButton.addActionListener(e -> doCheck(false));
        add(traceButton);

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
                doCheck(false);
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
        return "Detect commonly made errors";
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

    private void doCheck(boolean fromButton) {
        Document d = docr.get();
        String res;

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try {
            if(fromButton) {
                new XMLWriter(out).visitDocument(d);
                res = new String(out.toByteArray(), StandardCharsets.UTF_8);
            } else {
                res = loadFromFile();
                SymbolicTrace st = new Parser(IOUtils.toInputStream(res, StandardCharsets.UTF_8)).parseXTRTrace(systemr.get());

                tracer.set(st);
            }
        } catch (Exception e) {
            res = e.getMessage();
        }

        textArea.setText(res);

    }

    private void login() {
        username = userNameField.getText();
        password = passwordField.getText();
        textArea.setText("Username: " + username + " password: " + password);

        apiClient = new UppaalCloudAPIClient(username, password);
        String msg = apiClient.login();
        textArea.setText(msg);
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
