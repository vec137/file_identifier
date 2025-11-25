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

			int bestMatchLength = 0;
			Set<String> bestMatches = new HashSet<>();

			for (SignatureInfo sigInfo : signatureDB) {
				byte[] signature = sigInfo.signatureBytes();
				int currentLength = signature.length;

				if (bytesMatch(fileHeader, signature)) {
					if (currentLength > bestMatchLength) {
						bestMatchLength = currentLength;
						bestMatches.clear();
						bestMatches.add(sigInfo.extension());
					} else if (currentLength == bestMatchLength) {
						bestMatches.add(sigInfo.extension());
					}
				}
			}

			if (bestMatches.isEmpty()) {
				logger.warn("Для файла '{}' не найдено совпадений.", filePath);
				return Collections.emptyList();
			} else {
				logger.info("Лучшее совпадение ({} байт): {}", bestMatchLength, bestMatches);
				return new ArrayList<>(bestMatches);
			}

		} catch (Exception e) {
			logger.error("Ошибка при чтении файла '{}': {}", filePath, e.getMessage());
			return Collections.emptyList();
		}
	}

	private List<SignatureInfo> loadSignatures() {
		List<SignatureInfo> signatures = new ArrayList<>();
		try (InputStream is = getClass().getResourceAsStream("/signatures.db");
				BufferedReader reader = new BufferedReader(new InputStreamReader(is))) {

			String line;
			while ((line = reader.readLine()) != null) {
				if (!line.isBlank() && !line.startsWith("#")) {
					String[] parts = line.split(";", 3);
					if (parts.length == 3) {
						byte[] sigBytes = hexStrToByteArr(parts[1].trim());
						signatures.add(new SignatureInfo(parts[0].trim(), sigBytes, parts[2].trim()));
					}
				}
			}
			logger.info("База из {} сигнатур успешно загружена.", signatures.size());
		} catch (Exception e) {
			logger.error("Не удалось загрузить базу сигнатур.", e);
		}
		return signatures;
	}

	private boolean bytesMatch(byte[] fileHeader, byte[] signature) {
		if (signature.length > fileHeader.length)
			return false;
		for (int i = 0; i < signature.length; i++) {
			if (fileHeader[i] != signature[i])
				return false;
		}
		return true;
	}

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
