package com.filetype.identifier;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Scanner;
import java.util.stream.IntStream;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Главный класс для управления процессом и взаимодействия с
 * пользователем
 */
public class Main {

	private static final Logger logger = LogManager.getLogger(Main.class);

	/**
	 * Главный метод класса, осуществляет взаимодействие с пользователем
	 *
	 * @param args путь к файлу должен быть первым элементом
	 */
	public static void main(String[] args) {
		Runtime.getRuntime().addShutdownHook(new Thread(() -> {
			logger.info("Приложение завершило работу");
		}));
		if (args.length == 0) {
			logger.error("Попытка запуска без аргументов");
			System.err.println("E: Не указан путь к файлу");
			System.err.println("Пример использования: java -jar app.jar <файл>");
			System.exit(1);
		}

		Path filePath = Paths.get(args[0]);
		FileAnalyzer analyzer = new FileAnalyzer();
		List<String> extensions = analyzer.identify(filePath);

		if (extensions.isEmpty()) {
			logger.warn("Тип файла не удалось определить для: {}", filePath);
			System.out.println("Не удалось определить тип файла");
		} else {
			String chosenExtension;
			if (extensions.size() == 1) {
				chosenExtension = extensions.get(0);
				logger.info("Определено единственное расширение: .{}", chosenExtension);
				System.out.println("Определено расширение файла: ." + chosenExtension);
			} else {
				logger.info("Найдено несколько вариантов: {}", extensions);
				chosenExtension = userChoice(extensions);
			}
			restoreExtension(filePath, chosenExtension);
		}
	}

	/**
	 * Предлагает выбрать расширение
	 * 
	 * @param extensions Список расширений на выбор
	 * @return Выбранное расширение
	 */
	private static String userChoice(List<String> extensions) {
		System.out.println("Найдено несколько расширений:");
		IntStream.range(0, extensions.size())
				.forEach(i -> System.out.printf("  %d: .%s\n", i + 1, extensions.get(i)));

		Scanner scanner = new Scanner(System.in);
		int choice = -1;
		while (choice < 1 || choice > extensions.size()) {
			System.out.print("Номер (1-" + extensions.size() + "): ");
			try {
				choice = Integer.parseInt(scanner.nextLine());
				if (choice < 1 || choice > extensions.size()) {
					System.err.println("E: Неверный номер");
					System.out.println("Для отмены нажмите Ctrl+D");
				}
			} catch (NumberFormatException e) {
				System.err.println("E: Получено не число");
				System.out.println("Для отмены нажмите Ctrl+D");
			} catch (NoSuchElementException e) {
				logger.info("Ввод отменен пользователем (EOF)");
				System.out.println("\nВвод отменен");
				System.exit(0);
			}
		}
		String selected = extensions.get(choice - 1);
		logger.info("Пользователь выбрал расширение: .{}", selected);
		return extensions.get(choice - 1);
	}

	/**
	 * Переименовывает файл
	 * 
	 * @param srcPath   Путь к файлу
	 * @param extension Расширение для добавления
	 */
	private static void restoreExtension(Path srcPath, String extension) {
		String filename = srcPath.getFileName().toString();
		int lastDot = filename.lastIndexOf('.');
		String baseName = (lastDot == -1) ? filename : filename.substring(0, lastDot);

		Path targetPath = srcPath.resolveSibling(baseName + "." + extension);
		try {
			Files.move(srcPath, targetPath);
			logger.info("Файл переименован: {} -> {}", srcPath, targetPath);
			System.out.println("Файл восстановлен: " + targetPath);
		} catch (IOException e) {
			logger.error("Ошибка при переименовании файла: {}", e.getMessage());
			System.err.println("E: Не удалось переименовать файл: " + e.getMessage());
		}
	}
}
