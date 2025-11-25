package com.filetype.identifier;

import com.filetype.identifier.model.SignatureInfo;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Загрузка базы данных и анализ файла
 */
public class FileAnalyzer {
	private static final Logger logger = LogManager.getLogger(FileAnalyzer.class);
	private final List<SignatureInfo> signatureDB;

	/**
	 * Загружает базу данных при инициализации
	 */
	public FileAnalyzer() {
		this.signatureDB = loadSignatures();
	}

	/**
	 * Конструктор для тестов
	 * 
	 * @param predefinedSignatures Список сигнатур
	 */
	public FileAnalyzer(List<SignatureInfo> predefinedSignatures) {
		this.signatureDB = predefinedSignatures;
	}

	/**
	 * Находит все совпадения по самой длинной сигнатуре
	 *
	 * @param filePath Путь к файлу для анализа
	 * @return Список расширений, соответствующий лучшим совпадениям
	 */
	public List<String> identify(Path filePath) {
		if (!Files.exists(filePath)) {
			logger.error("Файл не найден: {}", filePath);
			return Collections.emptyList();
		}

		try (InputStream is = Files.newInputStream(filePath)) {
			byte[] fileHeader = is.readNBytes(512);

			Map<Integer, List<String>> matched = signatureDB.stream()
					.filter(sig -> bytesMatch(fileHeader, sig.signatureBytes()))
					.collect(Collectors.groupingBy(
							sig -> sig.signatureBytes().length,
							Collectors.mapping(SignatureInfo::extension, Collectors.toList())));

			if (matched.isEmpty()) {
				logger.warn("Для файла '{}' не найдено совпадений.", filePath);
				return Collections.emptyList();
			} else {
				int bestMatchLength = matched.keySet().stream()
						.max(Integer::compareTo)
						.orElse(0);
				List<String> bestMatch = matched.get(bestMatchLength).stream()
						.distinct()
						.collect(Collectors.toList());
				logger.info("Лучшее совпадение ({} байт): {}", bestMatchLength, bestMatch);
				return bestMatch;
			}

		} catch (Exception e) {
			logger.error("Ошибка при чтении файла '{}': {}", filePath, e.getMessage());
			return Collections.emptyList();
		}
	}

	/**
	 * Загружает базу данных
	 *
	 * @return Список сигнатур {@link SignatureInfo}
	 */
	private List<SignatureInfo> loadSignatures() {
		try (InputStream is = getClass().getResourceAsStream("/signatures.db");
				BufferedReader reader = new BufferedReader(new InputStreamReader(is))) {

			List<SignatureInfo> signatures = reader.lines()
					.filter(line -> !line.isBlank() && !line.startsWith("#"))
					.map(line -> line.split(";", 3))
					.filter(parts -> parts.length == 3)
					.map(parts -> {
						try {
							return new SignatureInfo(parts[0].trim(), hexStrToByteArr(parts[1].trim()),
									parts[2].trim());
						} catch (Exception e) {
							logger.warn("Ошибка парсинга: {}", Arrays.toString(parts));
							return null;
						}
					})
					.filter(Objects::nonNull)
					.collect(Collectors.toList());
			logger.info("База из {} сигнатур успешно загружена.", signatures.size());
			return signatures;
		} catch (Exception e) {
			logger.error("Не удалось загрузить базу сигнатур.", e);
			return Collections.emptyList();
		}
	}

	/**
	 * Побайтовое сравнение заголовка файла с сигнатурой
	 *
	 * @param fileHeader байты из заголовка файла
	 * @param signature  массив байтов магических чисел
	 * @return {@code true}, если заголовок файла содержит указанную сигнатуру,
	 *         иначе {@code false}
	 */
	private boolean bytesMatch(byte[] fileHeader, byte[] signature) {
		if (signature.length > fileHeader.length)
			return false;
		for (int i = 0; i < signature.length; i++) {
			if (fileHeader[i] != signature[i])
				return false;
		}
		return true;
	}

	/**
	 * Парсит строку в байты
	 * 
	 * @param s строка с байтами
	 * @return Массив байтов
	 */
	private byte[] hexStrToByteArr(String s) {
		int len = s.length();
		byte[] data = new byte[len / 2];
		for (int i = 0; i < len; i += 2) {
			data[i / 2] = (byte) ((Character.digit(s.charAt(i), 16) << 4)
					+ Character.digit(s.charAt(i + 1), 16));
		}
		return data;
	}
}
