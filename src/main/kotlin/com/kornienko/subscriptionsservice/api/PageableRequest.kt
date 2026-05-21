package com.kornienko.subscriptionsservice.api

import io.swagger.v3.oas.annotations.media.Schema
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Pageable
import org.springframework.data.domain.Sort

@Schema(description = "Параметры пагинации и сортировки")
data class PageableRequest(
    @Schema(description = "Номер страницы (начиная с 0)", example = "0", defaultValue = "0")
    val page: Int = 0,

    @Schema(description = "Количество элементов на странице", example = "10", defaultValue = "20")
    val size: Int = 5,

    @Schema(description = "Сортировка в формате: поле,направление", example = "createdAt,desc")
    val sort: String? = null
) {
    fun toPageable(defaultSort: String = "createdAt", defaultDirection: String = "desc"): Pageable {
        if (sort.isNullOrBlank()) {
            val direction = if (defaultDirection.equals("desc", ignoreCase = true)) {
                Sort.Direction.DESC
            } else {
                Sort.Direction.ASC
            }
            return PageRequest.of(page, size, Sort.by(direction, defaultSort))
        }

        val parts = sort!!.split(",")
        val property = parts[0].trim()
        val direction = if (parts.size > 1 && parts[1].trim().equals("desc", ignoreCase = true)) {
            Sort.Direction.DESC
        } else {
            Sort.Direction.ASC
        }

        return PageRequest.of(page, size, Sort.by(direction, property))
    }
}