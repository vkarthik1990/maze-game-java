package maze;

import java.util.*;
import java.awt.*;
import java.awt.event.*;
import java.rmi.RemoteException;
import java.rmi.registry.*;

import javax.swing.*;

@SuppressWarnings("serial")
public class Client extends JFrame
{
    public enum ClientStatus
    {
        CLIENT_IDLE,
        CLIENT_GAME_STARTING,
        CLIENT_GAME_RUNNING,
    }
    
    private JLabel ipLabel = new JLabel("Server Hostname");
    private JTextField ipText = new JTextField(10);
    private JButton joinGame = new JButton("Join Game");
    private JTextArea statusView = new JTextArea(20, 50);
    
    private int mapSize = -1;
    private int myTreasure = 0;
    private int id = -1;
    private HashMap<Integer, Integer> playerTrseaureMap = new HashMap<Integer, Integer>();
    private HashMap<PosPair, Integer> playerPosMap = new HashMap<PosPair, Integer>();
    private HashMap<PosPair, Integer> remainTreasureMap = new HashMap<PosPair, Integer>();

    private boolean gameJoined = false;

    private UpdateTimer timer;
    private int updateFreq = 20;
    
    private MazeInf rmiInf = null;
    
    private ClientStatus status;

    private class ClientButtonListener implements ActionListener
    {
        public void actionPerformed(ActionEvent e)
        {
            if (gameJoined) // if we already join a game, we want to leave the game
            {
                // print info
                statusView.setText(Log.AddLog("leave game rmi call issued, please wait...", statusView.getText()));
                
                // invoke the RMI call
                try
                {
                    if (rmiInf.LeaveGame(id))
                    {
                        statusView.setText(Log.AddLog("leave game rmi call succeed", statusView.getText()));
                    }
                    else
                    {
                        statusView.setText(Log.AddLog("leave game rmi call return false", statusView.getText()));
                        return;
                    }
                }
                catch (RemoteException e1)
                {
                    statusView.setText(Log.AddLog("ERROR: leave game rmi call failed", statusView.getText()));
                    e1.printStackTrace();
                    return;
                }
                
                joinGame.setText("Join Game");

                // stop the update timer
                timer.timer.cancel();
                
                // update the status
                status = ClientStatus.CLIENT_IDLE;
            }
            else // if we havn't join a game yet, we want to join a game
            {
                // print info
                statusView.setText(Log.AddLog("join game rmi call issued, please wait...", ""));
                
                // setup RMI
                try 
                {
                    String name = "Vtb Maze Server RMI";
                    Registry registry = LocateRegistry.getRegistry(ipText.getText());
                    rmiInf = (MazeInf) registry.lookup(name);
                    statusView.setText(Log.AddLog("rmi registry lookup succeed", statusView.getText()));
                } 
                catch (Exception e1) 
                {
                    statusView.setText(Log.AddLog("ERROR: rmi registry lookup failed", statusView.getText()));
                    e1.printStackTrace();
                    return;
                }
                
                // invoke the RMI call
                try
                {
                    id = rmiInf.JoinGame();
                    
                    if (id != -1)
                    {
                        statusView.setText(Log.AddLog("join game rmi call succeed, new id is " + id, statusView.getText()));
                    }
                    else
                    {
                        statusView.setText(Log.AddLog("join game request denied", statusView.getText()));
                        return;
                    }
                }
                catch (RemoteException e1)
                {
                    statusView.setText(Log.AddLog("ERROR: join game rmi call failed", statusView.getText()));
                    e1.printStackTrace();
                    return;
                }
                
                // the status view window request for focus
                statusView.requestFocusInWindow();
                
                joinGame.setText("Leave Game");

                // start the update timer
                timer = new UpdateTimer(1000 / updateFreq, Client.this);
                
                // update the status
                status = ClientStatus.CLIENT_GAME_STARTING;
            }

            gameJoined = !gameJoined;
        }
    }

    private class ClientKeyListener implements KeyListener
    {
        public void keyPressed(KeyEvent e)
        {
            int keyCode = e.getKeyCode();

            switch (keyCode)
            {
                case KeyEvent.VK_UP:
                {
                    RequestMove(PackInfo.Direction.DIR_UP);
                    break;
                }
                case KeyEvent.VK_DOWN:
                {
                    RequestMove(PackInfo.Direction.DIR_DOWN);
                    break;
                }
                case KeyEvent.VK_LEFT:
                {
                    RequestMove(PackInfo.Direction.DIR_LEFT);
                    break;
                }
                case KeyEvent.VK_RIGHT:
                {
                    RequestMove(PackInfo.Direction.DIR_RIGHT);
                    break;
                }
            }
        }

        public void keyReleased(KeyEvent e) {}

        public void keyTyped(KeyEvent e) {}
    }
    
    public Client()
    {
        // set window title
        super("Maze Gmae Client");

        // initialize variables
        ipText.setText("127.0.0.1");
        statusView.setText(Log.AddLog("ready", ""));
        status = ClientStatus.CLIENT_IDLE;

        // add action listener
        joinGame.addActionListener(new ClientButtonListener());
        statusView.addKeyListener(new ClientKeyListener());
    }
    
    public String GetGamePlot()
    {
        String plot = "";
        String playerInfo = "";
        
        for (int i=0; i<mapSize; ++i)
        {
            for (int j=0; j<mapSize; ++j)
            {
                if (playerPosMap.containsKey(new PosPair(i, j)))
                {
                    int curId = playerPosMap.get(new PosPair(i, j));
                    int treasure = playerTrseaureMap.get(curId);
                    
                    if (curId == id)
                    {
                        plot = plot + "*" + "\t";
                        myTreasure = playerTrseaureMap.get(id);
                    }
                    else
                    {
                        plot = plot + (char)(playerPosMap.get(new PosPair(i, j)) + (int)'A') + "\t";
                        playerInfo = playerInfo + "player " + (char)(playerPosMap.get(new PosPair(i, j)) + (int)'A') + " owns " + treasure + "\n";
                    }
                }
                else if (remainTreasureMap.containsKey(new PosPair(i, j)))
                    plot = plot + remainTreasureMap.get(new PosPair(i, j)).toString() + "\t";
                else
                    plot += "0\t";
            }
            
            plot += "\n";
        }
        
        return plot + playerInfo + "my treasure is " + myTreasure + " and id is " + id + "\n";
    }
    
    public void RequestMove(PackInfo.Direction dir)
    {
        PackInfo packInfo = null;
        
        if (status != ClientStatus.CLIENT_GAME_RUNNING)
            return;
        
        // print info
        statusView.setText(Log.AddLog("request move rmi call issued, please wait...", statusView.getText()));
        
        // invoke the RMI call
        try
        {
            packInfo = rmiInf.RequestMove(id, dir);
            if (packInfo == null)
                throw new Exception();
        }
        catch (RemoteException e1)
        {
            statusView.setText(Log.AddLog("ERROR: request move rmi call failed", statusView.getText()));
            e1.printStackTrace();
            return;
        }
        catch (Exception e2)
        {
            statusView.setText(Log.AddLog("ERROR: request move rmi call return null", statusView.getText()));
            e2.printStackTrace();
            return;
        }
        
        playerTrseaureMap = packInfo.playerTrseaureMap;
        playerPosMap = packInfo.playerPosMap;
        remainTreasureMap = packInfo.remainTreasureMap;
        
        statusView.setText(Log.AddLog(GetGamePlot() + "request move rmi call succeed", ""));
    }

    public void RequestForUpdate()
    {
        PackInfo packInfo = null;
        
        if (status != ClientStatus.CLIENT_GAME_RUNNING && status != ClientStatus.CLIENT_GAME_STARTING)
            return;
        
        // print info
        statusView.setText(Log.AddLog("request update rmi call issued, please wait...", statusView.getText()));
        
        // invoke the RMI call
        try
        {
            packInfo = rmiInf.RequestUpdate(id);
            if (packInfo == null)
                throw new Exception();
        }
        catch (RemoteException e1)
        {
            statusView.setText(Log.AddLog("ERROR: request update rmi call failed", statusView.getText()));
            e1.printStackTrace();
            return;
        }
        catch (Exception e2)
        {
            statusView.setText(Log.AddLog("ERROR: request update rmi call return null", statusView.getText()));
            e2.printStackTrace();
            return;
        }
        
        playerTrseaureMap = packInfo.playerTrseaureMap;
        playerPosMap = packInfo.playerPosMap;
        remainTreasureMap = packInfo.remainTreasureMap;
        mapSize = packInfo.mapSize;
        
        if (status == ClientStatus.CLIENT_GAME_STARTING && packInfo.remainGameStartTick == 0)
        {
            status = ClientStatus.CLIENT_GAME_RUNNING;
            statusView.setText(Log.AddLog(GetGamePlot() + "request update rmi call succeed", ""));
        }
        else if (status == ClientStatus.CLIENT_GAME_STARTING)
        {
            statusView.setText(Log.AddLog("game starts in " + packInfo.remainGameStartTick, ""));
        }
        else
        {
            statusView.setText(Log.AddLog(GetGamePlot() + "request update rmi call succeed", ""));
        }
    }

    public void Display()
    {
        statusView.setEditable(false);

        JPanel topPanel = new JPanel();
        topPanel.setLayout(new FlowLayout());
        topPanel.add(ipLabel);
        topPanel.add(ipText);
        topPanel.add(joinGame);
        setLayout(new FlowLayout());
        add(topPanel);
        add(statusView);

        this.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        this.setSize(600, 420);
        this.setVisible(true);
    }
}
