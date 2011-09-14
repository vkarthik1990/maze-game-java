package maze;

import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.rmi.registry.*;
import java.rmi.server.*;
import java.util.*;
import java.util.concurrent.*;

import javax.swing.*;

@SuppressWarnings("serial")
public class Server extends JFrame
{
    public enum ServerStatus
    {
        SERVER_STOP, 
        SERVER_IDLE, 
        SERVER_GAME_STARTING, 
        SERVER_GAME_RUNNING,
    }
    
    public int gameStartWaitTime = 5;
    
    private JLabel mapSizeLabel = new JLabel("Map Size");
    private JLabel goldCountLabel = new JLabel("Treasure Number");
    private JTextField mapSizeField = new JTextField(5);
    private JTextField goldCount = new JTextField(5);
    private JButton startServer = new JButton("Start Server");
    private JButton startRMI = new JButton("Start RMI");
    private JButton forkClient = new JButton("Create Client");
    public JTextArea statusView = new JTextArea(20, 50);

    private boolean serverRunning = false;

    public int mapSize;
    public int treasureNum;
    public int nextPlayerID;
    public int alivePlaynerNum;
    public int remainTreasureNum;
    public int remainGameStartTickcount;
    public HashMap<Integer, Integer> playerTrseaureMap = new HashMap<Integer, Integer>();
    public HashMap<PosPair, Integer> playerPosMap = new HashMap<PosPair, Integer>();
    public HashMap<PosPair, Integer> remainTreasureMap = new HashMap<PosPair, Integer>();
    
    public Semaphore opLock = new Semaphore(1, true);
    
    public ServerStatus status = ServerStatus.SERVER_STOP;
    
    public GameStartTimer startTimer = null;

    private class ServerButtonListener implements ActionListener
    {
        public synchronized void actionPerformed(ActionEvent e)
        {
            if (e.getSource() == startServer)
            {
                if (serverRunning) // if the server is running
                {
                    // brought server offline
                    try
                    {
                        String name = "Vtb Maze Server RMI";
                        Registry registry = LocateRegistry.getRegistry();
                        registry.unbind(name);
                        statusView.setText(Log.AddLog("rmi un-bound succeed", statusView.getText()));
                    }
                    catch (Exception e2)
                    {
                        statusView.setText(Log.AddLog("ERROR: rmi un-bound failed", statusView.getText()));
                        e2.printStackTrace();
                        return;
                    }
                    
                    try
                    {
                        opLock.acquire();
                        
                        // clean up variables
                        status = ServerStatus.SERVER_STOP;
                        nextPlayerID = 1;
                        alivePlaynerNum = 0;
                        remainGameStartTickcount = gameStartWaitTime;
                        playerTrseaureMap.clear();
                        playerPosMap.clear();
                        remainTreasureMap.clear();
                        startTimer.timer.cancel();
                        
                        opLock.release();
                    }
                    catch (InterruptedException e1)
                    {
                        e1.printStackTrace();
                        return;
                    } 

                    startServer.setText("Start Server");
                    statusView.setText(Log.AddLog("server stops now", statusView.getText()));
                }
                else // if the server is not running
                {
                    // brought server online
                    try
                    {
                        String name = "Vtb Maze Server RMI";
                        MazeInf engine = new MazeInfServerImpl(Server.this);
                        MazeInf stub = (MazeInf) UnicastRemoteObject.exportObject(engine, 0);
                        Registry registry = LocateRegistry.getRegistry();
                        registry.rebind(name, stub);
                        statusView.setText(Log.AddLog("rmi registry bound succeed", statusView.getText()));
                    }
                    catch (Exception e2)
                    {
                        statusView.setText(Log.AddLog("ERROR: rmi registry bound failed", statusView.getText()));
                        e2.printStackTrace();
                        return;
                    }

                    try
                    {
                        opLock.acquire();
                        
                        // setup variables
                        nextPlayerID = 1;
                        alivePlaynerNum = 0;
                        status = ServerStatus.SERVER_IDLE;
                        mapSize = Integer.parseInt(mapSizeField.getText());
                        treasureNum = Integer.parseInt(goldCount.getText());
                        remainTreasureNum = treasureNum;
                        remainGameStartTickcount = gameStartWaitTime;                        
                        
                        opLock.release();
                    }
                    catch (InterruptedException e1)
                    {
                        e1.printStackTrace();
                        return;
                    } 

                    startServer.setText("Stop Server");
                    statusView.setText(Log.AddLog("server is running now", statusView.getText()));
                }

                serverRunning = !serverRunning;
            }
            else if (e.getSource() == startRMI)
            {
                Runtime rt = Runtime.getRuntime();
                try
                {
                    @SuppressWarnings("unused")
                    Process pr = rt.exec("rmiregistry");
                }
                catch (IOException e1)
                {
                    e1.printStackTrace();
                }
            }
            else if (e.getSource() == forkClient)
            {
                Client client = new Client();
                client.Display();
            }
        }
    }
    
    public Server()
    {
        // set window title
        super("Maze Gmae Server");

        // initialize variables
        mapSizeField.setText("5");
        goldCount.setText("20");
        statusView.setText(Log.AddLog("ready", ""));

        // add action listener
        startServer.addActionListener(new ServerButtonListener());
        startRMI.addActionListener(new ServerButtonListener());
        forkClient.addActionListener(new ServerButtonListener());
    }
    
    public synchronized void UpdateGameStartTick()
    {
        try
        {
            opLock.acquire();
            
            if (status == ServerStatus.SERVER_GAME_STARTING && remainGameStartTickcount > 0)
            {
                remainGameStartTickcount -= 1;
            }
            else if (status == ServerStatus.SERVER_GAME_STARTING && remainGameStartTickcount <= 0)
            {
                startTimer.timer.cancel();
                remainGameStartTickcount = gameStartWaitTime;
                status = ServerStatus.SERVER_GAME_RUNNING;
            }
            else // this should never happen
            {
                startTimer.timer.cancel();
                statusView.setText(Log.AddLog("WARNING: unexpected behaviour in Server::UpdateGameStartTick()", statusView.getText()));
            }
            
            opLock.release();
        }
        catch (InterruptedException e)
        {
            e.printStackTrace();
            return;
        } 
    }

    public void Display()
    {
        statusView.setEditable(false);

        JPanel topPanel = new JPanel();
        topPanel.setLayout(new FlowLayout());
        topPanel.add(mapSizeLabel);
        topPanel.add(mapSizeField);
        topPanel.add(goldCountLabel);
        topPanel.add(goldCount);
        topPanel.add(startServer);
        topPanel.add(forkClient);
        topPanel.add(startRMI);
        setLayout(new FlowLayout());
        add(topPanel);
        add(statusView);

        this.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        this.setSize(650, 420);
        this.setVisible(true);
    }

    public static void main(String[] args)
    {
        Server server = new Server();
        server.Display();
    }
}
