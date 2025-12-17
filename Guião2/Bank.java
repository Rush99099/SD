import java.util.concurrent.locks.*;

public class Bank {

    private static class Account {
        private int balance;
        private Lock l = new ReentrantLock();
        Account(int balance) { this.balance = balance; }
        int balance() { return balance; }
        boolean deposit(int value) {
            balance += value;
            return true;
        }
        boolean withdraw(int value) {
            if (value > balance)
                return false;
            balance -= value;
            return true;
        }
    }

    // Bank slots and vector of accounts
    private int slots;
    private Account[] av;
    private Lock l = new ReentrantLock();

    public Bank(int n) {
        slots=n;
        av=new Account[slots];
        for (int i=0; i<slots; i++) av[i]=new Account(0);
    }

    // Account balance
    public int balance(int id) {
        l.lock();
        try{
            if (id < 0 || id >= slots)
                return 0;
            return av[id].balance();
        }
        finally{
            l.unlock();
        }
    }

    // Deposit
    public boolean deposit(int id, int value) {
        l.lock();
        try{
            if (id < 0 || id >= slots)
                return false;
            return av[id].deposit(value);
        }
        finally{
            l.unlock();
        }
    }

    // Withdraw; fails if no such account or insufficient balance
    public boolean withdraw(int id, int value) {
        l.lock();
        try{
            if (id < 0 || id >= slots)
                return false;
            return av[id].withdraw(value);
        }
        finally{
            l.unlock();
        }
    }

    public boolean transfer (int from, int to, int value){
        if (from < 0 || from >= slots || to < 0 || to >= slots)
            return false;
        Account cfrom = av[from];
        Account cto = av[to];

        if (from < to){
            cfrom.l.lock();
            cto.l.lock();
        }
        else{
            cto.l.lock();
            cfrom.l.lock();
        }

        try {
            if (!cfrom.withdraw(value)) {
                return false;
            }
            cto.deposit(value);
        }
        finally {
            cfrom.l.unlock();
            cto.l.unlock();
        }
        return true;
    }

    public int totalBalance(){
        /*
        for (int i = 0; i < av.length; i++) {
            total += av[i].balance();
        }
        */
        for (int i = 0; i < slots; i++)
            av[i].l.lock();
            System.out.println("Locked account");
        try {
            int total = 0;
            for (int i = 0; i < slots; i++)
                total += av[i].balance();
            return total;
        }
        finally {
            for (int i = 0; i < slots; i++)
                av[i].l.unlock();
        }
    }
}
