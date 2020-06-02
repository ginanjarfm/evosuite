/**
 * Copyright (C) 2020 Ginanjar Fahrul M, Gordon Fraser, Andrea Arcuri and EvoSuite
 * contributors
 *
 * This file is part of EvoSuite.
 *
 * EvoSuite is free software: you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as published
 * by the Free Software Foundation, either version 3.0 of the License, or
 * (at your option) any later version.
 *
 * EvoSuite is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with EvoSuite. If not, see <http://www.gnu.org/licenses/>.
 */
package org.evosuite.intellij;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.json.JSONObject;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.text.NumberFormat;

public class LoginDialog extends JDialog {
    private JPanel contentPane;
    private JFormattedTextField usernameField;
    private JPasswordField passwordField;
    private JButton cancelButton;
    private JButton loginButton;

    private volatile boolean wasOK = false;
    private volatile EvoParameters params;
    private volatile Project project;

    private volatile String token;
    private volatile JSONObject metric;
    private static String BASE_URL = "http://softtest.javanlabs.com/api/";

    public void initFields(Project project, EvoParameters params) {
        this.project = project;
        this.params = params;
    }

    public LoginDialog() {

        setContentPane(contentPane);
        setModal(true);
        getRootPane().setDefaultButton(loginButton);

        loginButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                onLogin();
            }
        });

        cancelButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                onCancel();
            }
        });

        // call onCancel() when cross is clicked
        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
        addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent e) {
                onCancel();
            }
        });

        // call onCancel() on ESCAPE
        contentPane.registerKeyboardAction(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                onCancel();
            }
        }, KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT);

        setPreferredSize(new Dimension(EvoParameters.getInstance().getGuiWidth(), EvoParameters.getInstance().getGuiHeight()));
    }

    private boolean signIn(String username, String password) {
        String result = "";
        HttpPost httpPost = new HttpPost(BASE_URL + "login?email=" + username + "&password=" + password);

        try {
            CloseableHttpClient httpClient = HttpClients.createDefault();
            CloseableHttpResponse httpResponse = httpClient.execute(httpPost);
            result = EntityUtils.toString(httpResponse.getEntity());

            JSONObject jsonObject = new JSONObject(result);
            if (jsonObject.has("token")) {
                token = (String) jsonObject.get("token");
                return true;
            } else {
                String message = "Failed API request: " + (String) jsonObject.get("message");
                Messages.showMessageDialog(project, message, "Info", Messages.getWarningIcon());
                return false;
            }
        } catch (UnsupportedEncodingException e) {
            String message = "Failed API request: " + e.getMessage();
            Messages.showMessageDialog(project, message, "An Error Occurred", Messages.getErrorIcon());
        } catch (IOException e) {
            String message = "Failed API request: " + e.getMessage();
            Messages.showMessageDialog(project, message, "An Error Occurred", Messages.getErrorIcon());
        }

        return false;
    }

    private void getMetrics() {
        String result = "";
        HttpGet httpGet = new HttpGet(BASE_URL + "metrics?token=" + token);

        try {
            CloseableHttpClient httpClient = HttpClients.createDefault();
            CloseableHttpResponse httpResponse = httpClient.execute(httpGet);
            result = EntityUtils.toString(httpResponse.getEntity());

            JSONObject jsonObject = new JSONObject(result);
            if (jsonObject.has("metric")) {
                metric = jsonObject.getJSONObject("metric");
            }
            String message = "Get metrics success [ID]:" + metric.get("developer_id");
            Messages.showMessageDialog(project, message, "Info", Messages.getInformationIcon());
        } catch (UnsupportedEncodingException e) {
            String message = "Failed API request: " + e.getMessage();
            Messages.showMessageDialog(project, message, "An Error Occurred", Messages.getErrorIcon());
        } catch (IOException e) {
            String message = "Failed API request: " + e.getMessage();
            Messages.showMessageDialog(project, message, "An Error Occurred", Messages.getErrorIcon());
        }
    }

    private void onLogin() {
        String username = usernameField.getText();
        String password = new String(passwordField.getPassword());

        if (signIn(username, password)) {
            getMetrics();

            saveParameters();

            dispose();
        }
    }

    private void saveParameters() {

    }

    private void onCancel() {
        dispose();
    }

    public static void main(String[] args) {
        LoginDialog dialog = new LoginDialog();
        dialog.pack();
        dialog.setVisible(true);
        System.exit(0);
    }

    private void createUIComponents() {
        // TODO: place custom component creation code here
        NumberFormat nf = NumberFormat.getNumberInstance();
        nf.setParseIntegerOnly(true);
        usernameField = new JFormattedTextField(nf);
    }
}
