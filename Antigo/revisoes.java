import java.util.concurrent.locks.*;
import java.util.*;

//Teste sd20230102

// Exercicio 6
interface Cache{
    //Podemos por exceptions na interface
    void put(int key, byte[] value) throws InterruptedException;
    byte[] get(int key);
    void evict(int key);
}

class CacheImpl{
    Lock l = new ReentrantLock();
    
    private class Entry{
        byte[] value;
        Condition cond = l.newCondition();
    }
    
    private final int N;
    public CacheImpl(int N){
        this.N = N;
    }

    private int size = 0;
    private Map<Integer, Entry> map = new HashMap<>();
    private Queue<Integer> set = new LinkedList<>();

    public void put(int key, byte[] value) throws InterruptedException{
        l.lock();
        try{
            Entry e = new Entry();
            if (e == null){
                e = new Entry();
                map.put(key, value);
            }
            set.add(key);
            while(size == N && e.value == null){
                e.cond.await();
            }
            set.remove(key);
            if (e.value == null){
                size += 1;
                e.value = value;
            }
        }
        finally{
            l.unlock();
        }
    }

    public byte[] get(int key){
        l.lock();
        Entry e = map.get(key);
        byte[] b = e == null ? null : e.value;
        l.unlock();
        return b;
    }

    public void evict(int key){
        l.lock();
            Entry e = map.get(key);
            if (e != null && e.value != null){
                size -= 1;
                if(!set.isEmpty()){
                    int k = set.remove();
                    e = map.get(k);
                    e.cond.signalAll();
                }
            }
        l.unlock();
    }
}