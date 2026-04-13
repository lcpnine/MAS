package tileworld;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Graphics;
import java.util.ArrayList;
import java.util.List;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingUtilities;
import sim.display.Console;
import sim.display.Controller;
import sim.display.Display2D;
import sim.display.GUIState;
import sim.engine.Schedule;
import sim.engine.SimState;
import sim.engine.Steppable;
import sim.portrayal.Portrayal;
import sim.portrayal.grid.ObjectGridPortrayal2D;
import tileworld.agent.DeliveryOptimizerAgent;
import tileworld.agent.ExplorerAgent;
import tileworld.agent.FuelScoutAgent;
import tileworld.agent.HoleFillerAgent;
import tileworld.agent.SmarterReplanningAgent;
import tileworld.agent.TileHunterAgent;
import tileworld.agent.TWAgent;
import tileworld.agent.TWAgentPortrayal;
import tileworld.environment.TWEnvironment;
import tileworld.environment.TWFuelStation;
import tileworld.environment.TWHole;
import tileworld.environment.TWObstacle;
import tileworld.environment.TWTile;

/**
 * TWGUI
 *
 * @author michaellees
 * Created: Apr 19, 2010
 *
 * Copyright michaellees 2010
 *
 * Description:
 *
 * A class implementing the basic TWGUI as required by the MASON agent toolkit.
 * This class is responsible for displaying the model. In MASON the model and
 * the visualizer are completely decoupled. The model contains fields, these
 * fields are visualized using portrayals. Part of the job of this class is to
 * associate portrayals with fields.
 *
 */
public class TWGUI extends GUIState {

    /**
     * Main display 2D
     */
    public Display2D display;
    /**
     * Frame which displays
     */
    public JFrame displayFrame;
    /**
     * Separate legend frame
     */
    private JFrame legendFrame;
    /**
     * Number of pixels that each cell should be represented by (for display).
     */
    private static final int CELL_SIZE_IN_PIXELS = 10;
    public static TWGUI instance;
    private int count = 0;

    // --- Live stats labels ---
    private JLabel stepLabel  = new JLabel("Step: 0 / " + Parameters.endTime);
    private JLabel scoreLabel = new JLabel("Score: 0");

    // --- Step delay (ms) injected before each simulation step ---
    private static volatile long stepDelayMs = 0;

    // --- Agent colors ---
    private static final Color COLOR_FUEL_SCOUT      = Color.orange;
    private static final Color COLOR_TILE_HUNTER     = new Color(0, 180, 0);
    private static final Color COLOR_HOLE_FILLER      = Color.magenta;
    private static final Color COLOR_EXPLORER         = Color.cyan;
    private static final Color COLOR_DELIVERY_OPT     = new Color(255, 100, 180);
    private static final Color COLOR_SMARTER_REPLAN   = Color.red;

    /**
     * USed constructor, initializes the GUI sim state with the pased
     * @param state
     */
    public TWGUI(SimState state) {
        super(state);
        instance = this;
    }

    /**
     * Default constructor, creates a TWEnvironment
     */
    private TWGUI() {
        this(new TWEnvironment());
    }

    public static String getName() {
        return "Tileworld in MASON";
    }

    /**
     * Portrayal of the main grid which is the environment. Using a standard ObjectGridPortrayal2D.
     */
    ObjectGridPortrayal2D objectGridPortrayal = new ObjectGridPortrayal2D();

    /**
     * Portrayal for agent layer
     */
    ObjectGridPortrayal2D agentGridPortrayal = new ObjectGridPortrayal2D();

    List<ObjectGridPortrayal2D> memoryGridPortrayalList = new ArrayList<ObjectGridPortrayal2D>();

    // -------------------------------------------------------------------------
    // Portrayal helpers
    // -------------------------------------------------------------------------

    /** Returns a TWAgentPortrayal with the given color. */
    private static Portrayal agentPortrayal(Color color) {
        return new TWAgentPortrayal(color, Parameters.defaultSensorRange);
    }

    // -------------------------------------------------------------------------
    // setupPortrayals
    // -------------------------------------------------------------------------

    /**
     * Creates the portrayals for all the relevant objects, including the environment itself.
     */
    public void setupPortrayals() {

        objectGridPortrayal.setField(((TWEnvironment) state).getObjectGrid());
        agentGridPortrayal.setField(((TWEnvironment) state).getAgentGrid());

        // Per-specialization agent colors
        agentGridPortrayal.setPortrayalForClass(FuelScoutAgent.class,       agentPortrayal(COLOR_FUEL_SCOUT));
        agentGridPortrayal.setPortrayalForClass(TileHunterAgent.class,      agentPortrayal(COLOR_TILE_HUNTER));
        agentGridPortrayal.setPortrayalForClass(HoleFillerAgent.class,       agentPortrayal(COLOR_HOLE_FILLER));
        agentGridPortrayal.setPortrayalForClass(ExplorerAgent.class,         agentPortrayal(COLOR_EXPLORER));
        agentGridPortrayal.setPortrayalForClass(DeliveryOptimizerAgent.class, agentPortrayal(COLOR_DELIVERY_OPT));
        agentGridPortrayal.setPortrayalForClass(SmarterReplanningAgent.class, agentPortrayal(COLOR_SMARTER_REPLAN));

        // Fallback for any other agent subclass
        agentGridPortrayal.setPortrayalForClass(TWAgent.class, TWAgent.getPortrayal());
        agentGridPortrayal.setPortrayalForRemainder(TWAgent.getPortrayal());

        objectGridPortrayal.setPortrayalForClass(TWHole.class,        TWHole.getPortrayal());
        objectGridPortrayal.setPortrayalForClass(TWTile.class,        TWTile.getPortrayal());
        objectGridPortrayal.setPortrayalForClass(TWObstacle.class,    TWObstacle.getPortrayal());
        objectGridPortrayal.setPortrayalForClass(TWFuelStation.class, TWFuelStation.getPortrayal());

        display.reset();
        display.repaint();
    }

    // -------------------------------------------------------------------------
    // start / init / quit
    // -------------------------------------------------------------------------

    @Override
    public void start() {
        super.start();
        setupPortrayals();
        stepLabel.setText("Step: 0 / " + Parameters.endTime);
        stepLabel.setForeground(Color.white);
        scoreLabel.setText("Score: 0");
        scoreLabel.setForeground(new Color(80, 220, 80));
        if (displayFrame != null) displayFrame.setTitle("TileWorld");

        // Delay steppable — runs first (ordering -1) to throttle simulation speed
        state.schedule.scheduleRepeating(Schedule.EPOCH, -1, new Steppable() {
            public void step(SimState simState) {
                long delay = stepDelayMs;
                if (delay > 0) {
                    try { Thread.sleep(delay); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
                }
            }
        }, 1);

        // Schedule a lightweight stats updater that runs every step (ordering 99 = after agents)
        state.schedule.scheduleRepeating(Schedule.EPOCH, 99, new Steppable() {
            public void step(SimState simState) {
                final TWEnvironment tw = (TWEnvironment) simState;
                final long step = (long) tw.schedule.getTime();
                final boolean done = step >= Parameters.endTime;
                SwingUtilities.invokeLater(new Runnable() {
                    public void run() {
                        if (done) {
                            stepLabel.setText("DONE  —  Step: " + step + " / " + Parameters.endTime);
                            stepLabel.setForeground(new Color(255, 220, 50));
                            scoreLabel.setText("Final Score: " + tw.getReward());
                            scoreLabel.setForeground(new Color(255, 220, 50));
                            if (displayFrame != null) {
                                displayFrame.setTitle("TileWorld \u2014 DONE  |  Score: " + tw.getReward());
                            }
                        } else {
                            stepLabel.setText("Step: " + step + " / " + Parameters.endTime);
                            scoreLabel.setText("Score: " + tw.getReward());
                            if (displayFrame != null) {
                                displayFrame.setTitle("TileWorld \u2014 Step " + step);
                            }
                        }
                    }
                });
                if (done && controller instanceof Console) {
                    SwingUtilities.invokeLater(new Runnable() {
                        public void run() { ((Console) controller).pressStop(); }
                    });
                }
            }
        }, 1);
    }

    @Override
    public void init(Controller c) {
        super.init(c);

        TWEnvironment tw = (TWEnvironment) state;
        display = new Display2D(
                tw.getxDimension() * CELL_SIZE_IN_PIXELS,
                tw.getyDimension() * CELL_SIZE_IN_PIXELS,
                this, 1);

        displayFrame = display.createFrame();
        displayFrame.setTitle("TileWorld");
        c.registerFrame(displayFrame);
        displayFrame.setVisible(true);

        // Stats bar at the top
        displayFrame.getContentPane().add(buildStatsPanel(), BorderLayout.NORTH);
        // Speed control at the bottom
        displayFrame.getContentPane().add(buildSpeedPanel(), BorderLayout.SOUTH);
        displayFrame.pack();

        // Legend in its own registered window
        legendFrame = buildLegendFrame();
        c.registerFrame(legendFrame);
        legendFrame.setVisible(true);

        display.attach(objectGridPortrayal, "Tileworld objects");
        display.attach(agentGridPortrayal,  "Tileworld Agents");

        display.setBackdrop(Color.gray);
    }

    public static void main(String[] args) {
        TWGUI twGui = new TWGUI();
        Console c = new Console(twGui);
        c.setVisible(true);
    }

    @Override
    public void quit() {
        super.quit();
        if (displayFrame != null) {
            displayFrame.dispose();
        }
        if (legendFrame != null) {
            legendFrame.dispose();
        }
        displayFrame = null;
        legendFrame  = null;
        display      = null;
        System.out.println("Final reward: " + ((TWEnvironment) state).getReward());
    }

    public static Object getInfo() {
        return "<H2>Tileworld</H2><p>An implementation of Tileworld in MASON.";
    }

    // -------------------------------------------------------------------------
    // Memory portrayal (unchanged)
    // -------------------------------------------------------------------------

    public void addMemoryPortrayal(TWAgent agent) {
        ObjectGridPortrayal2D memoryPortrayal = new ObjectGridPortrayal2D();
        memoryPortrayal.setField(agent.getMemory().getMemoryGrid());
        memoryPortrayal.setPortrayalForClass(TWHole.class,        TWHole.getMemoryPortrayal());
        memoryPortrayal.setPortrayalForClass(TWTile.class,        TWTile.getPortrayal());
        memoryPortrayal.setPortrayalForClass(TWObstacle.class,    TWObstacle.getPortrayal());
        memoryPortrayal.setPortrayalForClass(TWFuelStation.class, TWFuelStation.getPortrayal());
        display.attach(memoryPortrayal, agent.getName() + "'s Memory");
    }

    public void resetDisplay() {
        display.detatchAll();
        display.attach(objectGridPortrayal, "Tileworld objects");
        display.attach(agentGridPortrayal,  "Tileworld Agents");
        display.setBackdrop(Color.gray);
    }

    // -------------------------------------------------------------------------
    // UI panel builders
    // -------------------------------------------------------------------------

    /** Dark stats bar shown above the simulation grid. */
    private JPanel buildStatsPanel() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEFT, 20, 5));
        panel.setBackground(new Color(30, 30, 30));
        panel.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, Color.darkGray));

        stepLabel.setForeground(Color.white);
        stepLabel.setFont(new Font("Monospaced", Font.BOLD, 13));

        scoreLabel.setForeground(new Color(80, 220, 80));
        scoreLabel.setFont(new Font("Monospaced", Font.BOLD, 13));

        panel.add(stepLabel);
        panel.add(scoreLabel);
        return panel;
    }

    /**
     * Speed control panel shown below the simulation grid.
     * A JSpinner lets the user type an exact delay (ms per step).
     * Preset buttons set common values instantly.
     * The delay is applied via Thread.sleep() in a scheduled Steppable.
     */
    private JPanel buildSpeedPanel() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 4));
        panel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(1, 0, 0, 0, Color.darkGray),
                BorderFactory.createEmptyBorder(2, 6, 2, 6)));
        panel.setBackground(new Color(50, 50, 50));

        JLabel title = new JLabel("Step delay:");
        title.setForeground(Color.lightGray);
        title.setFont(new Font("SansSerif", Font.BOLD, 11));
        panel.add(title);

        // Spinner: 0–10000 ms, step 50 ms, default 0
        SpinnerNumberModel model = new SpinnerNumberModel(0, 0, 10000, 50);
        JSpinner spinner = new JSpinner(model);
        spinner.setPreferredSize(new Dimension(80, 24));
        spinner.setToolTipText("Delay in milliseconds added before each simulation step (0 = max speed)");
        spinner.addChangeListener(e -> stepDelayMs = ((Number) spinner.getValue()).longValue());
        panel.add(spinner);

        JLabel msLabel = new JLabel("ms/step");
        msLabel.setForeground(Color.lightGray);
        msLabel.setFont(new Font("SansSerif", Font.PLAIN, 11));
        panel.add(msLabel);

        panel.add(Box.createHorizontalStrut(10));

        // Preset buttons
        int[][] presets = {{0, 0}, {100, 100}, {500, 500}};
        String[] labels = {"Max", "100ms", "500ms"};
        for (int i = 0; i < labels.length; i++) {
            final long delay = presets[i][0];
            JButton btn = new JButton(labels[i]);
            btn.setFont(new Font("SansSerif", Font.PLAIN, 11));
            btn.setFocusPainted(false);
            btn.addActionListener(e -> {
                spinner.setValue((int) delay);
                stepDelayMs = delay;
            });
            panel.add(btn);
        }

        return panel;
    }

    /** Floating legend window listing all object and agent types with their colors. */
    private JFrame buildLegendFrame() {
        JFrame frame = new JFrame("Legend");
        frame.setDefaultCloseOperation(JFrame.HIDE_ON_CLOSE);

        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBorder(BorderFactory.createEmptyBorder(10, 14, 12, 20));
        panel.setBackground(new Color(40, 40, 40));

        addLegendSection(panel, "Environment");
        addLegendRow(panel, new Color(0, 255, 0),       false, "Tile  — pick up & deliver");
        addLegendRow(panel, new Color(188, 143, 143),   false, "Hole  — fill with a tile to score");
        addLegendRow(panel, Color.black,                false, "Obstacle");
        addLegendRow(panel, new Color(255, 255, 0),     true,  "Fuel Station  — refuel here");

        panel.add(Box.createVerticalStrut(10));
        addLegendSection(panel, "Agents");
        addLegendRow(panel, COLOR_FUEL_SCOUT,    false, "FuelScout      — discovers fuel stations");
        addLegendRow(panel, COLOR_TILE_HUNTER,   false, "TileHunter     — aggressive tile collection");
        addLegendRow(panel, COLOR_HOLE_FILLER,   false, "HoleFiller     — delivery specialist");
        addLegendRow(panel, COLOR_EXPLORER,      false, "Explorer       — systematic zone coverage");
        addLegendRow(panel, COLOR_DELIVERY_OPT,  false, "DeliveryOpt    — cluster-density routing");
        addLegendRow(panel, COLOR_SMARTER_REPLAN,false, "SmarterReplan  — predictive replanning");

        frame.getContentPane().setBackground(new Color(40, 40, 40));
        frame.getContentPane().add(panel);
        frame.pack();
        frame.setResizable(false);
        return frame;
    }

    private void addLegendSection(JPanel parent, String title) {
        JLabel label = new JLabel(title.toUpperCase());
        label.setForeground(new Color(160, 160, 160));
        label.setFont(new Font("SansSerif", Font.BOLD, 10));
        label.setAlignmentX(java.awt.Component.LEFT_ALIGNMENT);
        parent.add(label);
        parent.add(Box.createVerticalStrut(5));
    }

    private void addLegendRow(JPanel parent, Color color, boolean oval, String text) {
        JPanel row = new JPanel();
        row.setLayout(new BoxLayout(row, BoxLayout.X_AXIS));
        row.setAlignmentX(java.awt.Component.LEFT_ALIGNMENT);
        row.setOpaque(false);

        final Color c = color;
        final boolean isOval = oval;
        JPanel swatch = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                g.setColor(c);
                if (isOval) {
                    g.fillOval(1, 1, getWidth() - 2, getHeight() - 2);
                } else {
                    g.fillRect(1, 1, getWidth() - 2, getHeight() - 2);
                }
                g.setColor(new Color(80, 80, 80));
                if (isOval) {
                    g.drawOval(1, 1, getWidth() - 2, getHeight() - 2);
                } else {
                    g.drawRect(1, 1, getWidth() - 2, getHeight() - 2);
                }
            }
        };
        swatch.setPreferredSize(new Dimension(16, 16));
        swatch.setMaximumSize(new Dimension(16, 16));
        swatch.setMinimumSize(new Dimension(16, 16));
        swatch.setOpaque(false);

        JLabel label = new JLabel("  " + text);
        label.setForeground(Color.white);
        label.setFont(new Font("SansSerif", Font.PLAIN, 12));

        row.add(swatch);
        row.add(label);
        row.add(Box.createHorizontalGlue());

        parent.add(row);
        parent.add(Box.createVerticalStrut(5));
    }
}
