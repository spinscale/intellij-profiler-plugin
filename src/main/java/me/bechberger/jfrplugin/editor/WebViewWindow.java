package me.bechberger.jfrplugin.editor;

import com.intellij.openapi.Disposable;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Disposer;
import com.intellij.ui.jcef.JBCefBrowser;
import com.intellij.ui.jcef.JBCefBrowserBuilder;
import me.bechberger.jfrtofp.processor.ConfigMixin;
import me.bechberger.jfrtofp.server.*;

import javax.swing.*;
import java.nio.file.Path;

/**
 * Window that loads an HTMl5 based view
 * <p>
 * This has to be implemented in Java, else IntelliJ will not work properly
 */
public class WebViewWindow implements Disposable {

    private final JBCefBrowser browser;
    private final String url;

    public WebViewWindow(Project project, Path jfrFile) {
        this.url = Server.startIfNeededAndGetUrl(jfrFile, null, null);
        browser = new JBCefBrowserBuilder().setEnableOpenDevToolsMenuItem(true).setUrl(url).build();
        Disposer.register(project, browser);
        // launching a browser properly is hard...
        new Thread(() -> {
            while (true) {
                try {
                    Thread.sleep(10);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
                if (browser.getCefBrowser().getURL().startsWith("http://localhost:")) {
                    break;
                }
                browser.getCefBrowser().loadURL(url);
            }
        }).start();
    }

    private String getURL() {
        return url;
    }

    public JComponent getComponent() {
        return browser.getComponent();
    }

    public void reload() {
        browser.loadHTML(getURL());
    }

    @Override
    public void dispose() {
        Disposer.dispose(browser);
    }
}
