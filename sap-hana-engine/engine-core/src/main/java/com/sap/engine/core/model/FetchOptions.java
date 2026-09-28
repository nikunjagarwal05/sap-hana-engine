package com.sap.engine.core.model;

/**
 * Options controlling pagination, batch size, and row limits during record fetching.
 */
public record FetchOptions(
        int limit,
        int offset,
        String skiptoken,
        int maxRowsLimit
) {
    public FetchOptions {
        if (limit <= 0) limit = 100;
        if (offset < 0) offset = 0;
        if (maxRowsLimit <= 0) maxRowsLimit = 50000; // Default safety row cap
    }

    public static FetchOptions defaultOptions() {
        return new FetchOptions(500, 0, null, 10000);
    }
}
