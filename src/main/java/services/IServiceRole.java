package services;

import java.util.List;

public interface IServiceRole<T> {
    void add(T t);
    void update(T t);
    void delete(T t);
    List<T> getAll();
}
