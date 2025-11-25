package com.filetype.identifier.model;

/**
 * Класс для хранения информации об одной сигнатуре
 *
 * @param extension      Расширение файла
 * @param signatureBytes Массив байт магических чисел
 * @param description    Mime тип файла
 */
public record SignatureInfo(String extension, byte[] signatureBytes, String description) {
}
