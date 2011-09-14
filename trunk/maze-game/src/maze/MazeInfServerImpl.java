package maze;

import java.rmi.*;
import java.util.*;

import maze.PackInfo.Direction;
import maze.Server.ServerStatus;

public class MazeInfServerImpl implements MazeInf
{
    Server server = null;

    public MazeInfServerImpl(Server server)
    {
        this.server = server;
    }

    public synchronized int JoinGame() throws RemoteException
    {
        try
        {
            server.opLock.acquire();
            
            if (server.status == ServerStatus.SERVER_STOP || server.status == ServerStatus.SERVER_GAME_RUNNING)
            {
                server.opLock.release();
                return -1;
            }
            else if (server.status == ServerStatus.SERVER_IDLE || server.status == ServerStatus.SERVER_GAME_STARTING)
            {
                int newId = server.nextPlayerID++;
                PosPair pos = new PosPair(0, 0);
                Random random = new Random();
                
                // if this the first player, generate treasure positions
                if (newId == 1)
                {
                    for (int i=0; i<server.treasureNum; ++i)
                    {
                        int x = random.nextInt(server.mapSize);
                        int y = random.nextInt(server.mapSize);
                        
                        if (server.remainTreasureMap.containsKey(new PosPair(x, y)))
                        {
                            server.remainTreasureMap.put(new PosPair(x, y), server.remainTreasureMap.get(new PosPair(x, y))+1);
                        }
                        else
                        {
                            server.remainTreasureMap.put(new PosPair(x, y), 1);
                        }
                    }
                }
                
                // generate new position for the player
                do
                {
                    pos.x = random.nextInt(server.mapSize);
                    pos.y = random.nextInt(server.mapSize);
                }
                while (server.playerPosMap.containsKey(pos) || server.remainTreasureMap.containsKey(pos));
                
                // update variables
                server.alivePlaynerNum++;
                server.playerTrseaureMap.put(newId, 0);
                server.playerPosMap.put(pos, newId);
                
                // if this is the first player, start the timer and update server status
                if (newId == 1)
                {
                    server.startTimer = new GameStartTimer(server);
                    server.status = ServerStatus.SERVER_GAME_STARTING;
                }
                
                server.opLock.release();
                return newId;
            }
            else
            {
                server.opLock.release();
                return -1;
            }
        }
        catch (InterruptedException e)
        {
            e.printStackTrace();
            return -1;
        }
    }

    public synchronized boolean LeaveGame(int id) throws RemoteException
    {
        try
        {
            server.opLock.acquire();
            
            if (server.status != ServerStatus.SERVER_GAME_STARTING && server.status != ServerStatus.SERVER_GAME_RUNNING)
            {
                server.opLock.release();
                return false;
            }
            
            // update variable
            server.alivePlaynerNum--;
            
            // if the last player left game, we stop the game
            if (server.alivePlaynerNum <= 0)
            {
                server.status = ServerStatus.SERVER_IDLE;
                server.nextPlayerID = 1;
                server.alivePlaynerNum = 0;
                server.remainGameStartTickcount = server.gameStartWaitTime;
                server.remainTreasureNum = server.treasureNum;
                server.playerTrseaureMap.clear();
                server.playerPosMap.clear();
                server.remainTreasureMap.clear();
                server.startTimer.timer.cancel();
            }
            
            server.opLock.release();
            return true;
        }
        catch (InterruptedException e)
        {
            e.printStackTrace();
            return false;
        }
    }

    public synchronized PackInfo RequestMove(int id, PackInfo.Direction dir) throws RemoteException
    {
        PackInfo pack = new PackInfo();
        
        try
        {
            server.opLock.acquire();
            
            // find the location of current player
            PosPair pos = null;
            for (int i=0; i<server.mapSize; ++i)
                for (int j=0; j<server.mapSize; ++j)
                    if (server.playerPosMap.containsKey(new PosPair(i, j)) && server.playerPosMap.get(new PosPair(i, j)) == id)
                    {
                        pos = new PosPair(i ,j);
                        break;
                    }
            
            if (pos != null)
            {
                // check if the move will move player out of map boundary
                if ((dir == Direction.DIR_UP && pos.x-1 >= 0) ||
                        (dir == Direction.DIR_DOWN && pos.x+1 < server.mapSize) ||
                        (dir == Direction.DIR_LEFT && pos.y-1 >= 0) ||
                        (dir == Direction.DIR_RIGHT && pos.y+1 < server.mapSize))
                {
                    // check if the move will cause player location conflict
                    if ((dir == Direction.DIR_UP && !server.playerPosMap.containsKey(new PosPair(pos.x-1, pos.y))) ||
                            (dir == Direction.DIR_DOWN && !server.playerPosMap.containsKey(new PosPair(pos.x+1, pos.y))) ||
                            (dir == Direction.DIR_LEFT && !server.playerPosMap.containsKey(new PosPair(pos.x, pos.y-1))) ||
                            (dir == Direction.DIR_RIGHT && !server.playerPosMap.containsKey(new PosPair(pos.x, pos.y+1))))
                    {
                        // now we are sure that this move is legal
                        
                        server.statusView.setText(Log.AddLog("client move legal", server.statusView.getText())); // TODO debug use
                        
                        // generate new player location
                        PosPair newPos = new PosPair(pos.x, pos.y);
                        if (dir == Direction.DIR_UP)
                            newPos.x -= 1;
                        else if (dir == Direction.DIR_DOWN)
                            newPos.x += 1;
                        else if (dir == Direction.DIR_LEFT)
                            newPos.y -= 1;
                        else
                            newPos.y += 1;
                        
                        // remove old player location and insert new player location
                        server.playerPosMap.remove(pos);
                        server.playerPosMap.put(newPos, id);
                        
                        // if the new location contains gold, add them to the player and update gold map
                        if (server.remainTreasureMap.containsKey(newPos))
                        {
                            server.playerTrseaureMap.put(id, server.playerTrseaureMap.get(id) + server.remainTreasureMap.get(newPos));
                            server.remainTreasureMap.remove(newPos);
                        }
                    }
                }
            }
            
            pack.playerPosMap = server.playerPosMap;
            pack.playerTrseaureMap = server.playerTrseaureMap;
            pack.remainTreasureMap = server.remainTreasureMap;
            
            server.opLock.release();
            return pack;
        }
        catch (InterruptedException e)
        {
            e.printStackTrace();
            return null;
        }
    }

    public synchronized PackInfo RequestUpdate(int id) throws RemoteException
    {
        PackInfo pack = new PackInfo();
        
        try
        {
            server.opLock.acquire();
            
            pack.playerPosMap = server.playerPosMap;
            pack.playerTrseaureMap = server.playerTrseaureMap;
            pack.remainTreasureMap = server.remainTreasureMap;
            pack.mapSize = server.mapSize;
            pack.remainGameStartTick = server.remainGameStartTickcount;
            
            server.opLock.release();
            return pack;
        }
        catch (InterruptedException e)
        {
            e.printStackTrace();
            return null;
        }
    }

}
