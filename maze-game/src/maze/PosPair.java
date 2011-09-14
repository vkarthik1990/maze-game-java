package maze;

import java.io.Serializable;

@SuppressWarnings("serial")
public class PosPair implements Serializable
{
    int x;
    int y;

    public PosPair(int x, int y)
    {
        this.x = x;
        this.y = y;
    }

    @Override
    public boolean equals(Object obj)
    {
        PosPair newPair = (PosPair) obj;
        if (x == newPair.x && y == newPair.y)
            return true;
        return false;
    }
    
    // this is a trick, because PosPair is used as key in hash map
    @Override
    public int hashCode()
    {
        return 0;
    }
}
