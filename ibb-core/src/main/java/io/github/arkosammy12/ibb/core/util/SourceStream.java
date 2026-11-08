package io.github.arkosammy12.ibb.core.util;

import java.util.*;

public final class SourceStream<T> {

    private final Deque<T> queue = new ArrayDeque<>();
    private final T defaulElement;

    public SourceStream(Collection<T> elements, T defaultElement) {
        this.queue.addAll(elements);
        this.defaulElement = defaultElement;
    }

    public T pollOrDefault() {
        return this.poll().orElse(this.defaulElement);
    }

    public T peekOrDefault() {
        return this.peek().orElse(this.defaulElement);
    }

    public Optional<T> poll() {
        return Optional.ofNullable(this.queue.poll());
    }

    public Optional<T> peek() {
        return Optional.ofNullable(this.queue.peek());
    }

    public void push(T element) {
        this.queue.offerFirst(element);
    }

    public boolean isEmpty() {
        return this.queue.isEmpty();
    }

    public static SourceStream<SourceCharacter> fromLine(String line) {
        Collection<SourceCharacter> sourceCharacters = new ArrayList<>();
        for (int i = 0; i < line.length(); i++) {
            sourceCharacters.add(new SourceCharacter(line.charAt(i), new SourcePosition(i)));
        }
        return new SourceStream<>(sourceCharacters, new SourceCharacter(null, new SourcePosition(line.length())));
    }

}