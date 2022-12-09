package md.leonis.crawler.moby.crawler;

import md.leonis.crawler.moby.model.GameEntry;

import java.util.*;
import java.util.function.Consumer;
import java.util.stream.Collectors;


public abstract class AbstractCrawler implements Crawler {

    private Map<String, List<GameEntry>> platformGamesMap = new HashMap<>();
    private boolean suspended = false;
    private boolean aborted = false;
    private Consumer<GameEntry> refreshConsumer = (a) -> {};
    private Consumer<GameEntry> successConsumer = (a) -> {};
    private Consumer<GameEntry> errorConsumer = (a) -> {};

    public List<GameEntry> getGamesList(List<String> platforms) throws Exception {

        List<GameEntry> gameEntries = new ArrayList<>();

        for (String p : platforms) {
            List<GameEntry> list = loadGamesList(p);
            platformGamesMap.put(p, list);
            gameEntries.addAll(list);
        }

        return gameEntries;
    }


    public void processGamesList(List<GameEntry> gameEntries, boolean dieNow) throws Exception {
        if (platformGamesMap.isEmpty()) {
            platformGamesMap = gameEntries.stream().collect(Collectors.groupingBy(GameEntry::getPlatformId));
        }
        cycle(gameEntries, dieNow);
    }

    private void cycle(List<GameEntry> gameEntries, boolean dieNow) throws Exception {
        Set<String> affectedPlatforms = new HashSet<>();

        boolean completed = true;
        int index = 0;
        for (GameEntry gameEntry : gameEntries) {
            if (suspended || aborted) {
                getProcessor().stopProcessors();
                break;
            }
            try {
                if (!gameEntry.isCompleted() && gameEntry.getErrorsCount() < 5) {
                    refreshConsumer.accept(gameEntry);
                    affectedPlatforms.add(gameEntry.getPlatformId());
                    parseGameEntry(gameEntry);
                    gameEntry.setCompleted(true);
                    gameEntry.setErrorsCount(0);
                    successConsumer.accept(gameEntry);
                } else {
                    continue;
                }
            } catch (Exception e) {
                if (dieNow) {
                    throw e;
                }
                if (gameEntry.getErrorsCount() < 5) {
                    completed = false;
                    gameEntry.setErrorsCount(gameEntry.getErrorsCount() + 1);
                    gameEntry.getExceptions().add(e);
                }
                errorConsumer.accept(gameEntry);
            }

            if (++index == 50) {
                saveSupportData();
                for (String platform : affectedPlatforms) {
                    saveGamesList(platform, platformGamesMap.get(platform), gameEntry);
                }
                affectedPlatforms.clear();

                index = 0;
            }
        }

        if (!aborted) {
            saveSupportData();
            for (String platform : affectedPlatforms) {
                saveGamesList(platform, platformGamesMap.get(platform), null);
            }
        }

        if (!completed && !aborted && !suspended) {
            getProcessor().resetProcessors();
            cycle(gameEntries, dieNow);
        }
    }

    public void setConsumers(Consumer<GameEntry> refreshConsumer, Consumer<GameEntry> successConsumer, Consumer<GameEntry> errorConsumer) {
        this.refreshConsumer = refreshConsumer;
        this.successConsumer = successConsumer;
        this.errorConsumer = errorConsumer;
    }

    public boolean isSuspended() {
        return suspended;
    }

    public void setSuspended(boolean suspended) {
        this.suspended = suspended;
    }

    public boolean isAborted() {
        return aborted;
    }

    public void setAborted(boolean aborted) {
        this.aborted = aborted;
    }
}
