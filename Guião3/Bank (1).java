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

    private final Map<Integer, Account> map = new HashMap<>();
    private int nextId = 0;
    private final ReentrantReadWriteLock bankLock = new ReentrantReadWriteLock();

    // create account and return account id
    public int createAccount(int balance) {
        // changing accounts map -> need write lock
        bankLock.writeLock().lock();
        try {
            int id = nextId++;
            map.put(id, new Account(balance));
            return id;
        } finally {
            bankLock.writeLock().unlock();
        }
    }

    // close account and return balance, or 0 if no such account
    public int closeAccount(int id) {
        // need write lock to remove from map
        bankLock.writeLock().lock();
        Account c;
        try {
            c = map.get(id);
            if (c == null) return 0;
            // lock account while holding map write lock to avoid races
            c.writeLock();
            try {
                map.remove(id);
                return c.balanceNoLock();
            } finally {
                c.writeUnlock();
            }
        } finally {
            bankLock.writeLock().unlock();
        }
    }

    // account balance; 0 if no such account
    public int balance(int id) {
        bankLock.readLock().lock();
        Account c;
        try {
            c = map.get(id);
            if (c == null) return 0;
            // acquire account read lock while map read lock held
            c.readLock();
        } finally {
            bankLock.readLock().unlock();
        }

        try {
            return c.balanceNoLock();
        } finally {
            c.readUnlock();
        }
    }

    // deposit; fails if no such account
    public boolean deposit(int id, int value) {
        if (value < 0) return false;
        bankLock.readLock().lock();
        Account c;
        try {
            c = map.get(id);
            if (c == null) return false;
            c.writeLock();
        } finally {
            bankLock.readLock().unlock();
        }

        try {
            return c.depositNoLock(value);
        } finally {
            c.writeUnlock();
        }
    }

    // withdraw; fails if no such account or insufficient balance
    public boolean withdraw(int id, int value) {
        if (value < 0) return false;
        bankLock.readLock().lock();
        Account c;
        try {
            c = map.get(id);
            if (c == null) return false;
            c.writeLock();
        } finally {
            bankLock.readLock().unlock();
        }

        try {
            return c.withdrawNoLock(value);
        } finally {
            c.writeUnlock();
        }
    }

    // transfer value between accounts;
    // fails if either account does not exist or insufficient balance
    public boolean transfer(int from, int to, int value) {
        if (value < 0) return false;
        if (from == to) return true;

        bankLock.readLock().lock();
        Account aFrom;
        Account aTo;
        try {
            aFrom = map.get(from);
            aTo = map.get(to);
            if (aFrom == null || aTo == null) return false;

            // lock accounts in id order to avoid deadlock
            if (from < to) { aFrom.writeLock(); aTo.writeLock(); }
            else { aTo.writeLock(); aFrom.writeLock(); }
        } finally {
            bankLock.readLock().unlock();
        }

        try {
            if (!aFrom.withdrawNoLock(value)) return false;
            aTo.depositNoLock(value);
            return true;
        } finally {
            // unlock in reverse order
            if (from < to) { aTo.writeUnlock(); aFrom.writeUnlock(); }
            else { aFrom.writeUnlock(); aTo.writeUnlock(); }
        }
    }

    // sum of balances in set of accounts; 0 if some does not exist
    public int totalBalance(int[] ids) {
        bankLock.readLock().lock();
        Map<Integer, Account> contas = new HashMap<>();
        try {
            for (int id : ids) {
                Account c = map.get(id);
                if (c == null) return 0;          // if any id invalid -> fail
                contas.putIfAbsent(id, c);
            }
            List<Integer> unique = new ArrayList<>(contas.keySet());
            Collections.sort(unique);
            for (int id : unique) contas.get(id).readLock();
        } finally {
            bankLock.readLock().unlock();
        }

        try {
            int total = 0;
            for (int id : ids) total += contas.get(id).balanceNoLock();
            return total;
        } finally {
            List<Integer> unique = new ArrayList<>(contas.keySet());
            Collections.sort(unique, Collections.reverseOrder());
            for (int id : unique) contas.get(id).readUnlock();
        }
    }

}
