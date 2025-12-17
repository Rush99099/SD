import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Semaphore;

// Definição das interfaces conforme o enunciado
interface Manager {
    Raid join(String name, int minPlayers) throws InterruptedException;
}
    
interface Raid {
    List<String> players();
    void waitStart() throws InterruptedException;
    void leave();
}

public class RaidManager implements Manager {

    // Semáforo global para limitar o número de Raids a decorrer (R)
    private final Semaphore runPermits;
    
    // Estado partilhado para o agrupamento (Monitor)
    private PendingGroup currentGroup;

    public RaidManager(int maxSimultaneousRaids) {
        this.runPermits = new Semaphore(maxSimultaneousRaids, true);
        this.currentGroup = new PendingGroup();
    }

    @Override
    public Raid join(String name, int minPlayers) throws InterruptedException {
        PendingGroup myGroup;

        synchronized (this) {
            // 1. Adicionar o jogador ao grupo que está atualmente a formar-se
            myGroup = this.currentGroup;
            myGroup.addPlayer(name, minPlayers);

            // 2. Verificar se este jogador completou os requisitos do grupo
            if (myGroup.isReady()) {
                // Criar o objeto Raid final
                RaidImpl newRaid = new RaidImpl(myGroup.players, this.runPermits);
                myGroup.raidResult = newRaid;
                
                // Notificar todos os jogadores deste grupo que o Raid foi criado
                myGroup.notifyAllPlayers();
                
                // Preparar um novo grupo limpo para os jogadores futuros
                this.currentGroup = new PendingGroup();
            } else {
                // 3. Se não está pronto, esperar na instância do grupo (não no Manager)
                // Isto garante que esperamos apenas pelo NOSSO grupo completar
                while (!myGroup.isReady()) {
                    myGroup.waitForCompletion();
                }
            }
        }
        
        // Retornar o Raid associado ao grupo onde o jogador entrou
        return myGroup.raidResult;
    }

    // --- Classes Auxiliares ---

    // Classe auxiliar para gerir o estado de espera (agrupamento)
    private static class PendingGroup {
        List<String> players = new ArrayList<>();
        int currentMaxReq = 0;
        RaidImpl raidResult = null; // Será preenchido quando o grupo fechar

        void addPlayer(String name, int minPlayers) {
            players.add(name);
            if (minPlayers > currentMaxReq) {
                currentMaxReq = minPlayers;
            }
        }

        boolean isReady() {
            return raidResult != null || (!players.isEmpty() && players.size() >= currentMaxReq);
        }

        synchronized void waitForCompletion() throws InterruptedException {
            while (raidResult == null) {
                wait();
            }
        }

        synchronized void notifyAllPlayers() {
            notifyAll();
        }
    }

    // Implementação da interface Raid
    private static class RaidImpl implements Raid {
        private final List<String> players;
        private final Semaphore globalSemaphore;
        
        private boolean isRunning = false;
        private int playersInRaid;

        public RaidImpl(List<String> players, Semaphore globalSemaphore) {
            // Cria uma cópia imutável ou separada da lista
            this.players = new ArrayList<>(players);
            this.playersInRaid = players.size();
            this.globalSemaphore = globalSemaphore;
        }

        @Override
        public List<String> players() {
            return this.players;
        }

        @Override
        public synchronized void waitStart() throws InterruptedException {
            // Se já está a correr, retorna imediatamente.
            // Se não, tenta adquirir o semáforo.
            // O uso de synchronized garante que apenas 1 thread deste Raid 
            // adquire o "permits" do semáforo global.
            if (!isRunning) {
                globalSemaphore.acquire(); 
                isRunning = true;
            }
        }

        @Override
        public synchronized void leave() {
            playersInRaid--;
            // O Raid termina quando o último jogador sai
            if (playersInRaid == 0) {
                // Se o raid chegou a iniciar (adquiriu semáforo), liberta-o agora.
                // Verificação importante caso alguém saia antes de waitStart.
                if (isRunning) {
                    globalSemaphore.release();
                    isRunning = false;
                }
            }
        }
    }
}