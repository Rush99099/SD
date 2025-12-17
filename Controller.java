import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.concurrent.locks.Condition;



class Controller{
    
    Lock l = new ReentrantLock();
    Condition[] cond = {l.newCondition(), l.newCondition()};

    int[] using = new int[2];
    int[] waiting = new int[2];
    int[] credit = new int[2];

    private int T;

    Controller(int T){
        this.T = T;
    }

    void request_resource(int i) throws InterruptedException{
        l.lock();   
        try{
            waiting[i] += 1;
            while (using[1-i] > 0 || (using[i] >= T) || (waiting[1-i] > 0 && credit[i] == 0)){ 
                cond[i].await();
            }
            waiting[i] -= 1;
            using[i] += 1;
            credit[i] -= 1;
        }
        finally{
            l.unlock();
        }
    }
    void release_resource(int i){
        l.lock();
        try{
            using[i] -= 1;
            cond[i].signal();
            if (using[i] == 0) {
                credit[1-i] = waiting[1-i];
                cond[1-i].signalAll();
            }
        }
        finally{
            l.unlock();
        }
    }
}


Faz a mudança a partir do momento em que existem mais 2 threads