package maze;

import java.util.*;

public class GameStartTimer
{
    public Timer timer;
    private Server server;

    public GameStartTimer (Server server)
    {
        this.server = server;
        timer = new Timer();
        timer.scheduleAtFixedRate(new UpdateTimerTask(), 1000, 1000);
    }

    class UpdateTimerTask extends TimerTask
    {
        public void run()
        {
            server.UpdateGameStartTick();
        }
    }
}
