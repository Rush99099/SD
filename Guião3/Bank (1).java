import java.util.concurrent.locks.*;
import java.util.*;

class Bank {

    private static class Account {
    private int balance;
    private final ReentrantReadWriteLock rwl = new ReentrantReadWriteLock();
        // read/write lock helpers
        void readLock() { rwl.readLock().lock(); }
        void readUnlock() { rwl.readLock().unlock(); }
        void writeLock() { rwl.writeLock().lock(); }
        void writeUnlock() { rwl.writeLock().unlock(); }

        // no-lock helpers usados quando já seguramos o lock correto
        int balanceNoLock() { return balance; }
        boolean depositNoLock(int v) { balance += v; return true; }
        boolean withdrawNoLock(int v) {
            if (v > balance) return false;
            balance -= v; return true;
    }
    }

    private Map<Integer, Account> map = new HashMap<Integer, Account>();
    private int nextId = 0;
    private ReentrantReadWriteLock l = new ReentrantReadWriteLock();

    // create account and return account id
    public int createAccount(int balance) {
        l.readLock().lock();
        try{
            int id = nextId;
            map.put(id, new Account(balance));
            return id;
        }
        finally{
            l.unlock();
        }
    }

    // close account and return balance, or 0 if no such account
    public int closeAccount(int id) {
        l.lock();
        Account c;
        try{
            c = map.get(id);
            if (c == null) return 0;
            c.lock();
            try{
                map.remove(id);
                return c.balance();
            }
            finally{
                c.unlock();
            }
        }
        finally{
            l.unlock();
        }
    }

    // account balance; 0 if no such account
    public int balance(int id) {
        l.readLock().lock();
        Account c;
        try{
            c = map.get(id);
            if (c == null) return 0;
            c.readLock();
        }
        finally{
            l.readLock().unlock();
        }
        try{
            return c.balanceNoLock();
        }
        finally{
            c.readUnlock();
        }
    }

    // deposit; fails if no such account
    public boolean deposit(int id, int value) {
        l.readLock().lock();
        Account c;
        try{
            c = map.get(id);
            c.writeLock();
        }
        finally{
            l.readLock().unlock();
        }
        try{
            return c.depositNoLock(value);
        }
        finally{
            c.writeUnlock();
        }
    }

    // withdraw; fails if no such account or insufficient balance
    public boolean withdraw(int id, int value) {
        l.readLock().lock();
        Account c;
        try{
            c = map.get(id);
            c.writeLock();
        }
        finally{
            l.readLock().unlock();
        }
        try{
            return c.withdrawNoLock(value);
        }
        finally{
            c.writeUnlock();
        }
    }

    // transfer value between accounts;
    // fails if either account does not exist or insufficient balance
    public boolean transfer(int from, int to, int value) {
        l.readLock().lock();
        Account cfrom;
        Account cto;
        try{
            cfrom = map.get(from);
            cto = map.get(to);
            if (cfrom == null || cto == null || cfrom == cto) {
                return false;
            }
            if (from < to) {
                cfrom.writeLock();
                cto.writeLock();
            }
            else{
                cto.writeLock();
                cfrom.writeLock();
            }
        }
        finally{
            l.readLock().unlock();
        }
        try{
            return cfrom.withdrawNoLock(value) && cto.depositNoLock(value);
        }
        finally{
            if (from < to) {
                cfrom.writeUnlock();
                cto.writeUnlock();
            }
            else{
                cto.writeUnlock();
                cfrom.writeUnlock();
            }
        }
    }

    // sum of balances in set of accounts; 0 if some does not exist
    public int totalBalance(int[] ids) {
        l.readLock().lock();
        Map<Integer, Account> contas = new HashMap<>();
        try{
            for (int id : ids) {
                Account c = map.get(id);
                contas.putIfAbsent(id, c);
            }
            List<Integer> unique = new ArrayList<>(contas.keySet());
            Collections.sort(unique);
            for(int id : unique) contas.get(id).readLock();
        }
        finally{
            l.readLock().unlock();
        }
        try{
            int total = 0;
            for(int id:ids){
                total += contas.get(id).balanceNoLock();
            }
            return total;
        }
        finally{
            List<Integer> unique = new ArrayList<>(contas.keySet());
            Collections.sort(unique,Collections.reverseOrder());
            for(int id : unique) contas.get(id).readUnlock();
        }
    }

}
