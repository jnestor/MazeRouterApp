
import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import javax.sound.sampled.*;

/**
 * Title: MazeApplet Description: Animation of Lee Algorithm for maze routing.
 * Copyright: Copyright (c) 2001 Company: Lafayette College
 *
 * @author John A. Nestor
 * @version 1.0
 */
public class RouterFrame extends JFrame implements Runnable {

    boolean mute = false;
    private int max = 0;
    private static final int WAITFORSRC = 0;
    private static final int WAITFORTGT = 1;
    private static final int EXPANDING = 2;
    private static final int TRACKBACK = 3;
    private Grid myGrid = null;
    private Thread myThread = null;
    private int routerMode = 0;
    private final JLabel msgBoard = new JLabel();
    private final JLabel title = new JLabel("Lee Algorithm", SwingConstants.CENTER);

    //private final JLabel 
    private final JButton clearBtn = new JButton("CLEAR");
    private final JButton pauseBtn = new JButton("");
    private final JButton stopBtn = new JButton("STOP");
    private final JButton resizeWindowBtn = new JButton("RESIZE");
    private final JCheckBox parallelExpandBox = new JCheckBox("Parallel Mode");
    private final JCheckBox tooltipBox = new JCheckBox("Tooltips for Grids");
    private final JCheckBox muteBox = new JCheckBox("Mute");
    private final String[] routerNames = {"Lee Algorithm", "Hadlock Algorithm", "A* Algorithm"};
    private JComboBox<String> routerComboBox = new JComboBox<String>(routerNames);
    private Router[] routerList = new Router[3];

    private JDialog resizeWindow = new JDialog(this,"Resize Option",true);
    Sound s = new Sound();

    public synchronized void initRouterFrame(int size, int nlayers) {
        initiResizeWindow();
        routerMode = 0;
        setLayout(new BorderLayout());
        title.setFont(new Font("Serif", Font.PLAIN, 25));
        JLabel ghostLabel = new JLabel();
        ghostLabel.setPreferredSize(new Dimension(10, 25));
        initAllGrids(size, nlayers);
        getContentPane().add(title, "North");
        clearBtn.addActionListener(this::clearAction);
        JPanel btnPanel = new JPanel();
        pauseBtn.addActionListener(this::pauseAction);
        stopBtn.addActionListener(this::stopAction);
        routerComboBox.addActionListener(this::switchAction);
        parallelExpandBox.addItemListener(this::expandBoxAction);
        tooltipBox.addItemListener(this::traceAction);
        muteBox.addItemListener(this::muteBoxAction);
        resizeWindowBtn.addActionListener(this::resizeAction);
        btnPanel.setLayout(new FlowLayout(FlowLayout.LEFT));
        msgBoard.setPreferredSize(new Dimension(270, 25));
        pauseBtn.setPreferredSize(new Dimension(90, 25));
        clearBtn.setPreferredSize(new Dimension(90, 25));
        stopBtn.setPreferredSize(new Dimension(90, 25));
        resizeWindowBtn.setPreferredSize(new Dimension(90, 25));
        btnPanel.add(ghostLabel);
        btnPanel.add(msgBoard);
        btnPanel.add(clearBtn);
        btnPanel.add(pauseBtn);
        btnPanel.add(stopBtn);
        btnPanel.add(routerComboBox);
        btnPanel.add(parallelExpandBox);
        btnPanel.add(tooltipBox);
        btnPanel.add(muteBox);
        btnPanel.add(resizeWindowBtn);
        getContentPane().add(btnPanel, "South");
        refreshTimer.start();
        repaint();
        start();
        beeper.start();
    }
    

    public void start() {
        if (myThread == null) {
            myThread = new Thread(this);
            myThread.start();
        }
    }

    public void stop() {
        myThread.interrupt();
        myThread = null;
    }
    //private static boolean paused = false;

    private void pauseAction(ActionEvent evt) {
        myGrid.pauseResume();
        pauseBtn.setText(myGrid.isPaused() ? "RESUME" : "PAUSE");
    }

    private void stopAction(ActionEvent evt) {
        if (myGrid.isPaused()) {
            myGrid.pauseResume();
        }
        myGrid.stopRouter();
    }

    private void clearAction(ActionEvent evt) {
        myGrid.requestClear();
    }

    private void switchAction(ActionEvent evt) {
        routerMode = routerComboBox.getSelectedIndex();
        myGrid.setRouter(routerList[routerMode]);
        title.setText(routerNames[routerMode]);
    }

    private void expandBoxAction(ItemEvent evt) {
        myGrid.setParallelExpand(evt.getStateChange() == 1);
    }

    private void muteBoxAction(ItemEvent evt) {
        mute = (evt.getStateChange() == 1);
    }

    private void traceAction(ItemEvent evt) {
        myGrid.setParallelExpand(evt.getStateChange() == 1);
        if (evt.getStateChange() == 1) {
            ToolTipManager.sharedInstance().setEnabled(true);
            mouseTracer.start();
        } else {
            mouseTracer.stop();
            ToolTipManager.sharedInstance().setEnabled(false);
        }
    }

    private void resizeAction(ActionEvent evt) {
        resizeWindow.setVisible(true);
    }

    Timer refreshTimer = new Timer(5, new ActionListener() {
        @Override
        public void actionPerformed(ActionEvent e) {
            pauseBtn.setText(myGrid.isPaused() ? "RESUME" : "PAUSE");
            msgBoard.setText(myGrid.getMSG());
            if (myGrid.getState() == WAITFORSRC) {
                max = 0;
                pauseBtn.setEnabled(false);
                stopBtn.setEnabled(false);
                clearBtn.setEnabled(true);
                routerComboBox.setEnabled(true);
                if (routerMode == 0) {
                    parallelExpandBox.setVisible(true);
                    parallelExpandBox.setEnabled(true);
                } else {
                    parallelExpandBox.setVisible(false);
                    parallelExpandBox.setEnabled(false);
                }
            } else if (myGrid.getState() == WAITFORTGT) {
                pauseBtn.setEnabled(false);
                stopBtn.setEnabled(false);
                clearBtn.setEnabled(false);
                routerComboBox.setEnabled(false);
                parallelExpandBox.setEnabled(false);
            } else if (myGrid.getState() == EXPANDING) {
                pauseBtn.setEnabled(true);
                stopBtn.setEnabled(true);
                clearBtn.setEnabled(false);
                routerComboBox.setEnabled(false);
                parallelExpandBox.setEnabled(false);
            } else if (myGrid.getState() == TRACKBACK) {
                pauseBtn.setEnabled(true);
                stopBtn.setEnabled(false);
                clearBtn.setEnabled(false);
                routerComboBox.setEnabled(false);
                parallelExpandBox.setEnabled(false);
            }
        }
    });

    public void run() {
        try {
            myGrid.run();
        } catch (InterruptedException e) {
        }
    }

    Timer mouseTracer = new Timer(0, new ActionListener() {
        @Override
        public void actionPerformed(ActionEvent e) {
            Point p = MouseInfo.getPointerInfo().getLocation();
            SwingUtilities.convertPointFromScreen(p, myGrid);
            GridPoint gp = myGrid.mouseToGridPoint((int) p.getX(), (int) p.getY());
            if (gp != null) {
                myGrid.setToolTipText(gp.toString());
            }
        }
    });

    Timer beeper = new Timer(10, new ActionListener() {
        @Override
        public void actionPerformed(ActionEvent e) {
            if (!mute) {
                if (myGrid.getState() == EXPANDING) {
                    int i = myGrid.getRouter().getMax();
                    if (i > max) {
                        max = i;
                        s.play(max);
                    }
                } else if (myGrid.getState() == TRACKBACK) {
                    int i = myGrid.getRouter().getMax();
                    if (i < max) {
                        max = i;
                        s.play(max);
                    }
                }

            }
        }
    });

    public boolean initAllGrids(int size, int nlayers) {
        Grid.resetGridSize(size);
        int ncols = Grid.calculateCols(getSize().width);
        int nrows = Grid.calculateRows(getSize().height, nlayers);
        System.out.println("width=" + getSize().width + " height=" + getSize().height);
        myGrid = new Grid(ncols, nrows, nlayers);
        routerList[0] = new LeeRouter(myGrid);
        routerList[1] = new HadlockRouter(myGrid);
        routerList[2] = new AStarRouter(myGrid);
        myGrid.setRouter(routerList[routerMode]);
        getContentPane().add(myGrid, "Center");
        start();
        return true;
    }

    public void resetGrids(int size, int nlayers) {
        int originalSize = myGrid.getGridSize();
        Grid.resetGridSize(size);
        int ncols = Grid.calculateCols(getSize().width);
        int nrows = Grid.calculateRows(getSize().height, nlayers);
        if (nrows < 1 || ncols < 1) {
            JOptionPane.showMessageDialog(this, "There are too many layers and too large grids","Too Many Layers",JOptionPane.ERROR_MESSAGE);
            Grid.resetGridSize(originalSize);
            return;
        }
        stop();
        getContentPane().remove(myGrid);
        initAllGrids(size, nlayers);
        SwingUtilities.updateComponentTreeUI(this);
    }

    private void initiResizeWindow() {
        JTextField layerField = new JTextField("1");
        JTextField gridSizeField = new JTextField("21");
        layerField.setPreferredSize(new Dimension(45, 25));
        gridSizeField.setPreferredSize(new Dimension(45, 25));
        JLabel NoL = new JLabel("No of Layers");
        JLabel GS = new JLabel("Grid Size");
        JPanel panel = new JPanel();
        JButton resizeBtn = new JButton("Resize");
        panel.add(NoL);
        panel.add(layerField);
        panel.add(GS);
        panel.add(gridSizeField);
        panel.add(resizeBtn);
        resizeBtn.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent evt) {
                Toolkit.getDefaultToolkit().beep();
                int n = JOptionPane.showConfirmDialog(
                        panel, "Resize the Router will clear all the grids, are you sure you want to resize",
                        "WARNING",
                        JOptionPane.YES_NO_OPTION,JOptionPane.WARNING_MESSAGE);
                if (n == JOptionPane.YES_OPTION) {
                    try {
                        int size = Integer.parseInt(gridSizeField.getText());
                        int nOL = Integer.parseInt(layerField.getText());
                        if (nOL < 1) {
                            Toolkit.getDefaultToolkit().beep();
                            JOptionPane.showMessageDialog(
                                    panel, "You must have at least one layer !","Invalid Layer",JOptionPane.ERROR_MESSAGE);
                        } else if (size <= 19) {
                            Toolkit.getDefaultToolkit().beep();
                            JOptionPane.showMessageDialog(
                                    panel, "The grid size is too small, it should be at least 20","Invalid Grid Size",JOptionPane.ERROR_MESSAGE);
                        } else {
                            resizeWindow.setVisible(false);
                            resetGrids(size, nOL);
                        }
                    } catch (NumberFormatException e) {
                        Toolkit.getDefaultToolkit().beep();
                        JOptionPane.showMessageDialog(
                                panel, "wrong input !","Invalid Input",JOptionPane.ERROR_MESSAGE);
                    }
                }
            }
        });
        resizeWindow.add(panel);
        resizeWindow.pack();
        resizeWindow.setLocationRelativeTo(this);
    }

}
