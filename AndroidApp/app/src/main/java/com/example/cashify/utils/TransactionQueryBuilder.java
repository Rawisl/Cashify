package com.example.cashify.utils;

import androidx.sqlite.db.SimpleSQLiteQuery;
import androidx.sqlite.db.SupportSQLiteQuery;

import java.util.ArrayList;
import java.util.List;

public class TransactionQueryBuilder {

    public static SupportSQLiteQuery buildFilteredQuery(
            String workspaceId, String searchQuery, long[] dateRange,
            Integer type, String method, Integer categoryId,
            int limit, int offset
    ) {
        StringBuilder query = new StringBuilder("SELECT * FROM transactions WHERE workspaceId = ?");
        List<Object> args = new ArrayList<>();
        args.add(workspaceId);

        // 1. Lọc theo chữ (Search)
        if (searchQuery != null && !searchQuery.trim().isEmpty()) {
            query.append(" AND note LIKE ?");
            args.add("%" + searchQuery.trim() + "%");
        }
        // 2. Lọc Thu / Chi
        if (type != null) {
            query.append(" AND type = ?");
            args.add(type);
        }
        // 3. Lọc Phương thức
        if (method != null && !method.isEmpty()) {
            query.append(" AND paymentMethod = ?");
            args.add(method);
        }
        // 4. Lọc Danh mục
        if (categoryId != null) {
            query.append(" AND categoryId = ?");
            args.add(categoryId);
        }
        // 5. Lọc Thời gian
        if (dateRange != null && dateRange.length == 2) {
            query.append(" AND timestamp BETWEEN ? AND ?");
            args.add(dateRange[0]);
            args.add(dateRange[1]);
        }

        // Sắp xếp mới nhất lên đầu + Phân trang
        query.append(" ORDER BY timestamp DESC LIMIT ? OFFSET ?");
        args.add(limit);
        args.add(offset);

        return new SimpleSQLiteQuery(query.toString(), args.toArray());
    }
}