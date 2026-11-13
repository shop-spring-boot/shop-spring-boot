package edu.itc.shop.orderfeed;

import java.util.List;

public record OrderFeedResponse<T>(List<T> items, String nextCursor, boolean hasNext) {
}
