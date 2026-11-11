package nl.obren.sokrates.reports.landscape.utils;

import java.util.List;

public interface ExtractStringListValue<T> {
    List<String> getValue(T object);
}
