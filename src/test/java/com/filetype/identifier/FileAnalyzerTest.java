package com.filetype.identifier;

import com.filetype.identifier.model.SignatureInfo;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit-тесты класса FileAnalyzer
 */
class FileAnalyzerTest {

	private static FileAnalyzer analyzer;

	@BeforeAll
	static void setUp() {
		List<SignatureInfo> testDatabase = List.of(
				new SignatureInfo("png", hexToBytes("89504E47"), "PNG Image"),
				new SignatureInfo("jpg", hexToBytes("FFD8FF"), "JPEG Image"),
				new SignatureInfo("jpeg", hexToBytes("FFD8FF"), "JPEG Image"),
				new SignatureInfo("zip", hexToBytes("504B0304"), "ZIP Archive"),
				new SignatureInfo("docx", hexToBytes("504B030414000600"), "Word Document"));
		analyzer = new FileAnalyzer(testDatabase);
	}

	@Test
	void testUniqueType(@TempDir Path tempDir) throws Exception {
		Path testFile = tempDir.resolve("my_image");
		Files.write(testFile, hexToBytes("89504E47"));
		List<String> result = analyzer.identify(testFile);
		assertEquals(1, result.size(), "Should find 1 extension");
		assertEquals("png", result.get(0), "Sholud find png");
	}

	@Test
	void testMultipleTypes(@TempDir Path tempDir) throws Exception {
		Path testFile = tempDir.resolve("photo");
		Files.write(testFile, hexToBytes("FFD8FF"));

		List<String> result = analyzer.identify(testFile);

		assertEquals(2, result.size(), "Should find 2 extensions");
		assertTrue(result.contains("jpg"), "Result missing jpg");
		assertTrue(result.contains("jpeg"), "Result missing jpeg");
	}

	@Test
	void testLongestMatch(@TempDir Path tempDir) throws Exception {
		Path testFile = tempDir.resolve("document");
		Files.write(testFile, hexToBytes("504B030414000600"));

		List<String> result = analyzer.identify(testFile);

		assertEquals(1, result.size(), "Found more than 1 extension");
		assertEquals("docx", result.get(0), "Extension should be docx");
	}

	@Test
	void testUnknownFile(@TempDir Path tempDir) throws Exception {
		Path testFile = tempDir.resolve("random_data");
		Files.writeString(testFile, "random_data");

		List<String> result = analyzer.identify(testFile);

		assertTrue(result.isEmpty(), "Result should be empty");
	}

	private static byte[] hexToBytes(String s) {
		int len = s.length();
		byte[] data = new byte[len / 2];
		for (int i = 0; i < len; i += 2) {
			data[i / 2] = (byte) ((Character.digit(s.charAt(i), 16) << 4)
					+ Character.digit(s.charAt(i + 1), 16));
		}
		return data;
	}
}
