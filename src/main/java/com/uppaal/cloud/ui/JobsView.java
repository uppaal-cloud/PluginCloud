package com.uppaal.cloud.ui;

import com.uppaal.cloud.util.UppaalCloudAPIClient;
import com.uppaal.model.core2.Document;
import com.uppaal.model.system.UppaalSystem;
import com.uppaal.model.system.symbolic.SymbolicTrace;
import com.uppaal.plugin.Repository;

import javax.swing.*;
import java.awt.*;

public class JobsView extends JPanel implements Callback {
    private final UppaalCloudAPIClient apiClient;
    private final Callback loggedOutCallback;

    private final JToggleButton setLocalButton;
    private final JToggleButton setRemoteButton;
    private final JLabel emailLabel = new JLabel("");

    private final LocalJobsView localJobsPanel;
    private final RemoteJobsView remoteJobsPanel;

    public JobsView(UppaalCloudAPIClient client, Callback callback,
                    Repository<Document> docr,
                    Repository<UppaalSystem> systemr,
                    Repository<SymbolicTrace> tracer) {
        super();
        this.apiClient = client;
        this.loggedOutCallback = callback;
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));

        // Create header {email} logout button {toggle}
        JPanel header = new JPanel();
        header.setLayout(new GridLayout(1,2));
        header.setPreferredSize(new Dimension(750, 30));
        header.setMaximumSize(new Dimension(header.getPreferredSize().width, 30));

        header.add(emailLabel);
        JButton signOutButton = new JButton("Sign Out");
        signOutButton.addActionListener(e -> signOut());
        header.add(signOutButton);

        // Horizontal spacer
        JSeparator s = new JSeparator();
        s.setOrientation(SwingConstants.HORIZONTAL);
        header.add(s);

        setLocalButton = new JToggleButton("Publish job");
        setLocalButton.addActionListener(e -> toggleView(false));
        setLocalButton.setSelected(true);
        header.add(setLocalButton);

        setRemoteButton = new JToggleButton("Results");
        setRemoteButton.addActionListener(e -> toggleView(true));
        setRemoteButton.setSelected(false);
        header.add(setRemoteButton);

        add(header);
        // Vertical separator
        add(new JSeparator());

        localJobsPanel = new LocalJobsView(this.apiClient, docr, this);
        add(localJobsPanel);

        remoteJobsPanel = new RemoteJobsView(this.apiClient, setRemoteButton, systemr, tracer);
        add(remoteJobsPanel);

        // Set default behavior
        toggleView(false);
    }


    public void refreshView() {
        emailLabel.setText(apiClient.getEmail());
    }

    private void signOut() {
        loggedOutCallback.callback(UiAction.LOGGED_OUT);
    }

    private void toggleView(boolean fromLocal){
        if(fromLocal){
            // Render remote component
            setRemoteButton.setSelected(true);
            setLocalButton.setSelected(false);

            localJobsPanel.setVisible(false);
            remoteJobsPanel.setVisible(true);
            remoteJobsPanel.refreshView();
        } else {
            // Render local component
            setRemoteButton.setSelected(false);
            setLocalButton.setSelected(true);

            localJobsPanel.setVisible(true);
            remoteJobsPanel.setVisible(false);
            localJobsPanel.refreshView();
        }
    }

    @Override
    public void callback(UiAction action) {
        if (action == UiAction.JOB_PUSHED) {
            toggleView(true);
        }
    }
}

