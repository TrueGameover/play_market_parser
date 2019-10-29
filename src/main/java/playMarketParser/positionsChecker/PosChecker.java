package playMarketParser.positionsChecker;

import java.util.*;
import java.util.concurrent.ConcurrentLinkedDeque;

public class PosChecker implements PosLoader.OnPosLoadCompleteListener {
    private final int CHECKS_COUNT;
    private String appId;

    private PosCheckListener posCheckListener;

    private final int MAX_THREADS_COUNT;
    private int threadsCount;
    private int processedCount;
    private boolean isAborted;

    private Deque<PosLoader> unprocessed = new ConcurrentLinkedDeque<>();
    private List<Query> queries;

    public PosChecker(String appId, List<Query> queries, int threadsCount, int checksCount, PosCheckListener posCheckListener) {
        this.appId = appId;
        this.queries = Collections.synchronizedList(queries);
        this.posCheckListener = posCheckListener;
        this.MAX_THREADS_COUNT = threadsCount;
        CHECKS_COUNT = checksCount;
    }

    public void start() {
        isAborted = false;
        createThreads();
        startNewLoaders();
    }

    public synchronized void abort(){
        isAborted = true;
    }

    private void createThreads() {
        for (int i = 0; i < CHECKS_COUNT; i++)
            for (Query query : queries)
                unprocessed.addLast(new PosLoader(query, appId, this));
    }

    private synchronized void startNewLoaders() {
        while (threadsCount < MAX_THREADS_COUNT && unprocessed.size() > 0) {
            unprocessed.pop().start();
            threadsCount++;
        }
    }

    @Override
    public synchronized void onPosLoadingComplete(Query query) {
        threadsCount--;
        processedCount++;
        posCheckListener.onPositionChecked();
        if (processedCount < queries.size() * CHECKS_COUNT && !isAborted)
            startNewLoaders();
        else {
            posCheckListener.onAllPositionsChecked();
        }
    }

    public interface PosCheckListener {
        void onPositionChecked();
        void onAllPositionsChecked();
    }

    public double getProgress() {
        return processedCount * 1.0 / (queries.size() * CHECKS_COUNT);
    }
}
