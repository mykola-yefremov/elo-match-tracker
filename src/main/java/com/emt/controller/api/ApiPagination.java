package com.emt.controller.api;

import com.emt.model.api.PageResponse;
import java.util.List;
import org.springframework.data.domain.Pageable;

final class ApiPagination {

  private ApiPagination() {}

  static <T> PageResponse<T> page(List<T> items, Pageable pageable) {
    int pageSize = pageable.getPageSize();
    int requestedPage = pageable.getPageNumber();
    int fromIndex = Math.min((int) pageable.getOffset(), items.size());
    int toIndex = Math.min(fromIndex + pageSize, items.size());
    int totalPages = items.isEmpty() ? 0 : (int) Math.ceil((double) items.size() / pageSize);

    return PageResponse.<T>builder()
        .content(items.subList(fromIndex, toIndex))
        .page(requestedPage)
        .size(pageSize)
        .totalElements(items.size())
        .totalPages(totalPages)
        .first(requestedPage == 0)
        .last(totalPages == 0 || requestedPage >= totalPages - 1)
        .build();
  }
}
