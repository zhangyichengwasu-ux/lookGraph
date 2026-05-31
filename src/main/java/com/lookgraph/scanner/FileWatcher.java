package com.lookgraph.scanner;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Component("codeFileWatcher")
@RequiredArgsConstructor
public class FileWatcher implements DisposableBean {

    private final IncrementalScanner incrementalScanner;
    private final Map<String, Thread> watchers = new ConcurrentHashMap<>();

    public void watch(String projectId, Path projectRoot) {
        if (watchers.containsKey(projectId)) {
            log.info("项目 {} 已在监听中", projectId);
            return;
        }

        Thread thread = Thread.ofVirtual()
                .name("lookgraph-watch-" + projectId)
                .start(() -> doWatch(projectId, projectRoot));
        watchers.put(projectId, thread);
        log.info("开始监听项目文件变更: {} -> {}", projectId, projectRoot);
    }

    public void stopWatch(String projectId) {
        Thread thread = watchers.remove(projectId);
        if (thread != null) {
            thread.interrupt();
            log.info("停止监听项目: {}", projectId);
        }
    }

    private void doWatch(String projectId, Path root) {
        try (WatchService ws = FileSystems.getDefault().newWatchService()) {
            registerRecursive(root, ws);

            while (!Thread.currentThread().isInterrupted()) {
                WatchKey key = ws.take();
                Path dir = (Path) key.watchable();

                List<Path> changed = key.pollEvents().stream()
                        .filter(e -> e.kind() != StandardWatchEventKinds.OVERFLOW)
                        .map(e -> dir.resolve((Path) e.context()))
                        .filter(this::isSourceFile)
                        .toList();

                key.reset();

                if (!changed.isEmpty()) {
                    incrementalScanner.scan(projectId, changed);
                }
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } catch (IOException e) {
            log.error("文件监听异常: {}", projectId, e);
        }
    }

    private void registerRecursive(Path root, WatchService ws) throws IOException {
        Files.walkFileTree(root, new SimpleFileVisitor<>() {
            @Override
            public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
                String name = dir.getFileName().toString();
                if (name.startsWith(".") || name.equals("target") ||
                        name.equals("build") || name.equals("node_modules")) {
                    return FileVisitResult.SKIP_SUBTREE;
                }
                dir.register(ws,
                        StandardWatchEventKinds.ENTRY_CREATE,
                        StandardWatchEventKinds.ENTRY_MODIFY,
                        StandardWatchEventKinds.ENTRY_DELETE);
                return FileVisitResult.CONTINUE;
            }
        });
    }

    private boolean isSourceFile(Path path) {
        String name = path.getFileName().toString();
        return name.endsWith(".java") || name.endsWith(".py") ||
                name.endsWith(".js") || name.endsWith(".ts");
    }

    @Override
    public void destroy() {
        watchers.values().forEach(Thread::interrupt);
        watchers.clear();
    }
}
