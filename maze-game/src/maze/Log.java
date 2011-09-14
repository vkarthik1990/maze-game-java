package maze;

public class Log
{
    public static String AddLog(String entry, String curLog)
    {
        if (curLog.length() > 500)
            return entry + "\n";
        else
            return curLog + entry + "\n";
    }
}
