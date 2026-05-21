package com.kornienko.subscriptionsservice.api

import org.slf4j.LoggerFactory
import org.springframework.dao.OptimisticLockingFailureException
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.http.converter.HttpMessageNotReadableException
import org.springframework.validation.FieldError
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice
import java.time.OffsetDateTime
import java.util.*

@RestControllerAdvice
class GlobalExceptionHandler {

    private val log = LoggerFactory.getLogger(javaClass)
    
    @ExceptionHandler(MethodArgumentNotValidException::class)
    fun handleValidationExceptions(ex: MethodArgumentNotValidException): ResponseEntity<ErrorResponse> {
        val errors = ex.bindingResult.allErrors.associate {
            when (it) {
                is FieldError -> it.field to (it.defaultMessage ?: "Некорректное значение")
                else -> it.objectName to (it.defaultMessage ?: "Некорректное значение")
            }
        }

        log.warn("Validation error: {}", errors)

        val response = ErrorResponse(
            timestamp = OffsetDateTime.now(),
            status = HttpStatus.BAD_REQUEST.value(),
            error = "Ошибка валидации",
            message = "Некорректные данные запроса",
            path = getCurrentPath(),
            details = errors
        )

        return ResponseEntity.badRequest().body(response)
    }

    
    @ExceptionHandler(IllegalArgumentException::class)
    fun handleIllegalArgument(ex: IllegalArgumentException): ResponseEntity<ErrorResponse> {
        log.warn("Business error: {}", ex.message)

        val response = ErrorResponse(
            timestamp = OffsetDateTime.now(),
            status = HttpStatus.BAD_REQUEST.value(),
            error = "Ошибка бизнес-логики",
            message = ex.message ?: "Некорректный запрос",
            path = getCurrentPath()
        )

        return ResponseEntity.badRequest().body(response)
    }
    
    @ExceptionHandler(NoSuchElementException::class)
    fun handleNoSuchElement(ex: NoSuchElementException): ResponseEntity<ErrorResponse> {
        log.warn("Resource not found: {}", ex.message)

        val response = ErrorResponse(
            timestamp = OffsetDateTime.now(),
            status = HttpStatus.NOT_FOUND.value(),
            error = "Ресурс не найден",
            message = ex.message ?: "Запрашиваемый ресурс не существует",
            path = getCurrentPath()
        )

        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response)
    }
    
    @ExceptionHandler(HttpMessageNotReadableException::class)
    fun handleJsonParseError(ex: HttpMessageNotReadableException): ResponseEntity<ErrorResponse> {
        log.warn("JSON parse error: {}", ex.message)

        val response = ErrorResponse(
            timestamp = OffsetDateTime.now(),
            status = HttpStatus.BAD_REQUEST.value(),
            error = "Ошибка формата JSON",
            message = "Неверный формат запроса. Проверьте структуру JSON.",
            path = getCurrentPath()
        )

        return ResponseEntity.badRequest().body(response)
    }
    
    @ExceptionHandler(Exception::class)
    fun handleGenericException(ex: Exception): ResponseEntity<ErrorResponse> {
        log.error("Unexpected error", ex)

        val response = ErrorResponse(
            timestamp = OffsetDateTime.now(),
            status = HttpStatus.INTERNAL_SERVER_ERROR.value(),
            error = "Внутренняя ошибка сервера",
            message = "Произошла непредвиденная ошибка. Пожалуйста, попробуйте позже.",
            path = getCurrentPath()
        )

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response)
    }

    @ExceptionHandler(OptimisticLockingFailureException::class)
    fun handleOptimisticLock(ex: OptimisticLockingFailureException): ResponseEntity<ErrorResponse> {
        return ResponseEntity
            .status(HttpStatus.CONFLICT)
            .body(
                ErrorResponse(
                    timestamp = OffsetDateTime.now(),
                    status = 409,
                    error = "Конфликт версий",
                    message = "Данные были изменены. Пожалуйста, обновите страницу и попробуйте снова.",
                    path = "" // можно получить из request
                )
            )
    }


    private fun getCurrentPath(): String {
        return try {
            val request = org.springframework.web.context.request.RequestContextHolder.currentRequestAttributes()
            (request as? org.springframework.web.context.request.ServletRequestAttributes)?.request?.requestURI ?: "unknown"
        } catch (e: Exception) {
            "unknown"
        }
    }

}

data class ErrorResponse(
    val timestamp: OffsetDateTime,
    val status: Int,
    val error: String,
    val message: String,
    val path: String,
    val details: Map<String, String?>? = null
)