package editor;

import javax.swing.*;
import java.awt.*;

public class ToolPanel extends JPanel {
    private JLabel titleLabel;

    public ToolPanel(String title) {
        setLayout(new BorderLayout());
        titleLabel = new JLabel(title);
        add(titleLabel, BorderLayout.NORTH);
    }
    public void init(){}

    public String getTitle() {
        return titleLabel.getText();
    }

    public void setTitle(String title) {
        titleLabel.setText(title);
    }
}
