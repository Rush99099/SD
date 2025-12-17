import java.util.concurrent.locks.*;
import java.util.ArrayDeque;
import java.util.Collections;
import java.util.Deque;
import java.util.List;

public interface Manager {
    Raid join(String name, int minPlayers) throws InterruptedException;
}

interface Raid {
    List<String> players();
    void waitStart() throws InterruptedException;
    void leave();
}

class ManagerI implements Manager{
    Lock l = new ReentrantLock();
    Condition cond = l.newCondition();

    RaidI current = new RaidI();
    int running = 0;
    Deque<RaidI> pending = new ArrayDeque<>();


    Raid join(String name, int minPlayers) throws InterruptedException{
        l.lock();
        try{
            RaidI r = current;
            r.players.add(name);
            r.max = Math.max(r.max, minPlayers);
            if (r.players.size() >= r.max) {
                current = new RaidI();
                cond.signalAll();
            }
            else{
                while (r == current) {
                    cond.await();
                }
            }
            return r;
        }
        finally{
            l.unlock();
        }
    }
    
}

private void finished(){
    l.lock();
    try{
        
    }
    finally{
        l.unlock();
    }
}

class RaidI implements Raid{

    Lock l = new ReentrantLock();
    Condition cond = l.newCondition();
    boolean started = false;
    int playing = 0;

    int max = 0;
    
    List<String> players;

    public List<String> players() {
        return players;
    }

    void init(){
        playing = players.size();
        players = Collections.unmodifiableList(players);
    }

    void start(){
        l.lock();
        try{
            started = true;
            cond.signalAll();
        }
        finally{
            l.unlock();
        }
    }

    public void waitStart() throws InterruptedException {
        l.lock();
        try{
            while (!started) {
                cond.await();
            }
        }
        finally{
            l.unlock();
        }
    }

    public void leave(){
        l.lock();
        try{
            playing -= 1;
            if (playing == 0) {
                finished();
        }
        finally{
            l.unlock();
        }
    }
}