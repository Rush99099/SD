package Guião4;

import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

class Barrier{
    private int N;
    private int count = 0;
    private int geracao = 0;
    private boolean flag = false;

    ReentrantLock l = new ReentrantLock();
    Condition cond = l.newCondition();
    
    Barrier (int N){
        this.N = N;
    }

    void await() throws InterruptedException{
        l.lock();
        int gen;
        try{
            if (flag) throw new InterruptedException();
            gen = geracao;
            count++;
            if (count == N){
                nextGeneration();
                return;
            }
            while (!flag && geracao == gen) {
                try{
                    cond.await();                
                }
                catch (InterruptedException ie){
                    wakeUp();
                    throw ie;
                }
            }
            if (flag) throw new InterruptedException();
        }
        finally{
            l.unlock();
        }
    }

    private void nextGeneration(){
        geracao++;
        count = 0;
        flag = false;
        cond.signalAll();
    }

    private void wakeUp(){
        geracao++;
        count = 0;
        flag = true;
        cond.signalAll();
    }
}
