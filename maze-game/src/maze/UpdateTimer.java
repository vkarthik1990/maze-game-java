package maze;

import java.util.*;

public class UpdateTimer
{
    public Timer timer;
    private Client client;

    public UpdateTimer (int interval, Client timerClient)
    {
        client = timerClient;
        timer = new Timer();
        timer.scheduleAtFixedRate(new UpdateTimerTask(), interval, interval);
    }

    class UpdateTimerTask extends TimerTask
    {
        public void run()
        {
            client.RequestForUpdate();
        }
    }
}
