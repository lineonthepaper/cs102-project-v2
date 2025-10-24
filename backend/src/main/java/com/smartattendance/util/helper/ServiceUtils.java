package com.smartattendance.util.helper;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Utility methods for common service layer patterns.
 * Eliminates duplication of fetch-and-group logic across multiple services.
 * 
 * This class addresses DRY violations identified in InstructorService, TAService, and StudentService
 * where identical data fetching and grouping patterns were repeated ~60 lines of duplicated code.
 */
public final class ServiceUtils {
    
    private ServiceUtils() {
        throw new UnsupportedOperationException("Utility class cannot be instantiated");
    }
    
    /**
     * Groups a list of items by a key extracted from each item.
     * If the source list is empty, returns an empty map.
     * 
     * @param <T> the type of items to group
     * @param <K> the type of the grouping key
     * @param items the list of items to group
     * @param keyExtractor function to extract the grouping key from an item
     * @return a map of keys to lists of items
     */
    public static <T, K> Map<K, List<T>> groupByKey(List<T> items, Function<T, K> keyExtractor) {
        if (items == null || items.isEmpty()) {
            return Collections.emptyMap();
        }
        return items.stream().collect(Collectors.groupingBy(keyExtractor));
    }
    
    /**
     * Fetches related items for a list of parent IDs and groups them by parent ID.
     * Useful for the common pattern: fetch all items, extract IDs, fetch related data, group by parent.
     * 
     * Example usage:
     * <pre>
     * Map&lt;String, List&lt;SectionAssignment&gt;&gt; assignmentsByInstructor = ServiceUtils.fetchAndGroupRelated(
     *     instructors,
     *     User::getId,
     *     ids -> ids.stream()
     *         .flatMap(id -> repository.findByUserId(id).stream())
     *         .collect(Collectors.toList()),
     *     SectionAssignment::getUserId
     * );
     * </pre>
     * 
     * @param <P> the type of parent entities
     * @param <K> the type of the key (usually String or Long)
     * @param <R> the type of related entities
     * @param parents the list of parent entities
     * @param keyExtractor function to extract ID from parent
     * @param fetcher function to fetch related entities for a list of keys
     * @param relatedKeyExtractor function to extract parent ID from related entity
     * @return a map of parent IDs to lists of related entities
     */
    public static <P, K, R> Map<K, List<R>> fetchAndGroupRelated(
            List<P> parents,
            Function<P, K> keyExtractor,
            Function<List<K>, List<R>> fetcher,
            Function<R, K> relatedKeyExtractor) {
        
        if (parents == null || parents.isEmpty()) {
            return Collections.emptyMap();
        }
        
        List<K> parentIds = parents.stream()
                .map(keyExtractor)
                .distinct()
                .collect(Collectors.toList());
        
        List<R> relatedItems = fetcher.apply(parentIds);
        
        return groupByKey(relatedItems, relatedKeyExtractor);
    }
    
    /**
     * Extract IDs from a list of entities.
     * 
     * @param <E> the entity type
     * @param <ID> the ID type
     * @param entities the list of entities
     * @param idExtractor function to extract ID from entity
     * @return list of unique IDs
     */
    public static <E, ID> List<ID> extractIds(List<E> entities, Function<E, ID> idExtractor) {
        if (entities == null || entities.isEmpty()) {
            return Collections.emptyList();
        }
        return entities.stream()
                .map(idExtractor)
                .distinct()
                .collect(Collectors.toList());
    }
}

