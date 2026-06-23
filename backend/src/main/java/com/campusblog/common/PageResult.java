package com.campusblog.common;

import java.util.List;

public record PageResult<T>(List<T> items, long page, long size, long total, long totalPages) {
}

