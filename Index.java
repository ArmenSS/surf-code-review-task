package ru.surf;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
// не принята использовать импорт такого рода
//pri  импортe с *-ом придет вес покет
import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Stream;


public class Index {
    
    //поля надо сделать private
    TreeMap<String, List<Pointer>> invertedIndex;

    ExecutorService pool;

    public Index(ExecutorService pool) {
        this.pool = pool;
        invertedIndex = new TreeMap<>();
    }


    // принимающие аргументы должны быть final
    public void indexAllTxtInPath(String pathToDir) throws IOException {
        Path of = Path.of(pathToDir);

        BlockingQueue<Path> files = new ArrayBlockingQueue<>(2);

        try (Stream<Path> stream = Files.list(of)) {
            stream.forEach(files::add);
        }

        pool.submit(new IndexTask(files));
        pool.submit(new IndexTask(files));
        pool.submit(new IndexTask(files));
    }
// будет лучше использовать SortedMap
    public TreeMap<String, List<Pointer>> getInvertedIndex() {
        return invertedIndex;
    }
// имя метода нужно начинать маленьким буквом 
    public List<Pointer> GetRelevantDocuments(String term) {
        return invertedIndex.get(term);
    }

    public Optional<Pointer> getMostRelevantDocument(String term) {
        return invertedIndex.get(term).stream().max(Comparator.comparing(o -> o.count));
    }

    static class Pointer {
// нужно getter setter
     
        private Integer count;
        
// поле filePath может быть final
        private String filePath;

        public Pointer(Integer count, String filePath) {
            this.count = count;
            this.filePath = filePath;
        }

        @Override
        public String toString() {
            return "{" + "count=" + count + ", filePath='" + filePath + '\'' + '}';
        }
    }

    class IndexTask implements Runnable {

        private final BlockingQueue<Path> queue;

        public IndexTask(BlockingQueue<Path> queue) {
            this.queue = queue;
        }

        @Override
        public void run() {
            try {
                Path take = queue.take();
                List<String> strings = Files.readAllLines(take);
                
// можно также использовать parallelStream() если в файле будет много строков,поскольку здесь используется ExecutorService 
                strings.stream().flatMap(str -> Stream.of(str.split(" "))).forEach(word -> invertedIndex.compute(word, (k, v) -> {
                    if (v == null) return List.of(new Pointer(1, take.toString()));
                    else {
                        ArrayList<Pointer> pointers = new ArrayList<>();
// будет лучше использовать equalsIgnoreCase
                        if (v.stream().noneMatch(pointer -> pointer.filePath.equals(take.toString()))) {
                            pointers.add(new Pointer(1, take.toString()));
                        }

                        v.forEach(pointer -> {
                            if (pointer.filePath.equals(take.toString())) {
// можно pointer.count++;
                                pointer.count = pointer.count + 1;
                            }
                        });

                        pointers.addAll(v);

                        return pointers;
                    }

                }));
// будет правильно InterruptedException и IOException поставить в try catch отдельно
            } catch (InterruptedException | IOException e) {
// в RuntimeException() писать e.message                  
                throw new RuntimeException();
            }
        }
    }
}
