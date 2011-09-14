package maze;

import java.io.Serializable;
import java.util.HashMap;

@SuppressWarnings("serial")
public class PackInfo implements Serializable
{
    public enum Direction
    {
        DIR_UP, 
        DIR_DOWN, 
        DIR_LEFT, 
        DIR_RIGHT,
    }
    
    public int mapSize = -1;
    public int remainGameStartTick = -1;
    public HashMap<Integer, Integer> playerTrseaureMap = null;
    public HashMap<PosPair, Integer> playerPosMap = null;
    public HashMap<PosPair, Integer> remainTreasureMap = null;
}
