package maze;

import java.rmi.Remote;
import java.rmi.RemoteException;

public interface MazeInf extends Remote
{
    // return value is the player id, if -1 is returned, then the server rejects the join request
    public int JoinGame() throws RemoteException;

    public boolean LeaveGame(int id) throws RemoteException;

    public PackInfo RequestMove(int id, PackInfo.Direction dir) throws RemoteException;

    public PackInfo RequestUpdate(int id) throws RemoteException;
}
