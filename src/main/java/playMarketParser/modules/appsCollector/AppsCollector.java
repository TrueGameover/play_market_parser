package playMarketParser.modules.appsCollector;

import playMarketParser.FoundApp;

import java.util.Deque;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedDeque;

public class AppsCollector implements ListingLoader.AppsCollectingListener {

    private final List<String> queries;
    private int maxThreadsCount = 1;
    private String language;
    private String country;
    private int collectedCount;
    private AppsCollectingListener appsCollectingListener;
    private Deque<ListingLoader> unprocessed = new ConcurrentLinkedDeque<>();

    private int threadsCount;
    private boolean isPaused;


    public AppsCollector(List<String> queries, AppsCollectingListener appsCollectingListener) {
        this.appsCollectingListener = appsCollectingListener;
        this.queries = queries;
    }

    public void start() {
        isPaused = false;
        createThreads();
        startNewLoaders();
    }

    public void pause() {
        isPaused = true;
    }

    public void resume() {
        isPaused = false;
        startNewLoaders();
    }

    public void stop() {
        unprocessed.clear();
        if (threadsCount == 0) appsCollectingListener.onFinish();
    }

    private void createThreads() {
        for (String query : queries)
            unprocessed.addLast(new ListingLoader(query, language, country, this));
    }

    private synchronized void startNewLoaders() {
        while (threadsCount < maxThreadsCount && unprocessed.size() > 0) {
            unprocessed.pop().start();
            threadsCount++;
        }
    }

    @Override
    public synchronized void onQueryProcessed(List<FoundApp> foundApps, String query, boolean isSuccess) {
        appsCollectingListener.onQueryProcessed(foundApps, query, isSuccess);
        collectedCount += foundApps.size();
        threadsCount--;

        if (isPaused) {
            if (threadsCount == 0)
                if (unprocessed.size() > 0) appsCollectingListener.onPause();
                else appsCollectingListener.onFinish();
        } else
            if (unprocessed.size() == 0 && threadsCount == 0) appsCollectingListener.onFinish();
            else startNewLoaders();
    }

    public interface AppsCollectingListener {
        void onQueryProcessed(List<FoundApp> foundApps, String query, boolean isSuccess);
        void onFinish();
        void onPause();
    }

    public double getProgress() {
        return collectedCount * 1.0 / queries.size();
    }

    public void setMaxThreadsCount(int maxThreadsCount) {
        this.maxThreadsCount = maxThreadsCount;
    }

    public void setLanguage(String language) {
        this.language = language;
    }

    public void setCountry(String country) {
        this.country = country;
    }
}