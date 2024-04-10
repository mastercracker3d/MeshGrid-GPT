package cl.mc3d.ai;

import cl.mc3d.as4p.ui.CommandButton;
import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.plaf.UIResource;

public class JTab extends JPanel implements UIResource {

    private String id;
    private JTab jTab = this;

    public JTab(String title, JPanel jPanel) {
        super();
        setLayout(new BorderLayout());
        setOpaque(false);
        add("Center", jPanel);
    }

    public void setClosable(boolean closable) {
        if (closable) {
            CommandButton closeButton = new CommandButton("X");
            closeButton.setTheme("midnight", false);
            closeButton.setBorderPainted(false);
            closeButton.setOpaque(false);
            closeButton.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    ((JButton) e.getSource()).getParent().getParent().getParent().remove(jTab);
                }
            });
            JPanel inputPanel = new JPanel(new BorderLayout());
            inputPanel.add("East", closeButton);
            add("North", inputPanel);
        }
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }
}
