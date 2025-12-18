import java.util.*;
import java.util.concurrent.locks.*;


// versão “egoísta”, centrada nos clientes, em que cada cliente tenta apropriar-se dos itens o mais cedo possível.

class Warehouse {
    private Map<String, Product> map =  new HashMap<String, Product>();

    private class Product { int quantity = 0; }

    private Product get(String item) {
        Product p = map.get(item);
        if (p != null) return p;
        p = new Product();
        map.put(item, p);
        return p;
    }

    public void supply(String item, int quantity) {
        Product p = get(item);
        p.quantity += quantity;
    }

    // Errado se faltar algum produto...
    public void consume(Set<String> items) {
        for (String s : items)
            get(s).quantity--;
    }

}

public class WarehouseEgo{
    private Map<String, Product> map =  new HashMap<String, Product>();
    
    private class Product { int quantity = 0; }

    private ReentrantLock l = new ReentrantLock();
    private Condition cond = l.newCondition();

    private Product get(String item) {
        Product p = map.get(item);
        if (p != null) return p;
        p = new Product();
        map.put(item, p);
        return p;
    }

    public void supply(String item, int quantity) {
        l.lock();
        try{
            Product p = map.get(item);
            if (p == null){
                p = new Product(); map.put(item, p);
            }
            p.quantity += quantity;
            cond.signalAll();
        }
        finally{
            l.unlock();
        }
    }

    // Errado se faltar algum produto...
    public void consume(Set<String> items) throws InterruptedException{
        l.lock();
        try{
            Set<String> taken = new HashSet<>();
            
            while (true) {
                for (String s : items){
                    if (taken.contains(s)) continue;
                    Product p = map.get(s);
                    if(p == null){
                        p = new Product();
                        map.put(s, p);
                    }
                    if (p.quantity > 0) {
                        p.quantity--;
                        taken.add(s); // todos os Produtos "s" são removidos
                    }
                }

                if (taken.size() == items.size()) return;
                try{
                    cond.await();
                }
                catch(InterruptedException ie){
                    cond.signalAll();
                    throw ie;
                }
            }
        }
        finally{
            l.unlock();
        }
    }
}