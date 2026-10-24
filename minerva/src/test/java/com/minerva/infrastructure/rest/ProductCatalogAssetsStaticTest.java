package com.minerva.infrastructure.rest;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ProductCatalogAssetsStaticTest {
    private static final Path ASSET_DIRECTORY = Path.of("src/main/resources/static/assets/products");
    private static final Set<String> EXPECTED_ASSETS = Set.of(
            "arroz-extra.webp", "azucar-rubia.webp", "aceite-vegetal.webp",
            "agua-mineral.webp", "cafe-molido.webp", "leche-entera.webp",
            "detergente-floral.webp", "jabon-neutro.webp", "panales-bebe.webp",
            "galletas-vainilla.webp", "alimento-canino.webp", "placeholder.webp");

    @Test
    void productAssetDirectoryContainsExactlyTheCatalogAndPlaceholder() throws Exception {
        try (var files = Files.list(ASSET_DIRECTORY)) {
            assertEquals(EXPECTED_ASSETS, files.filter(Files::isRegularFile)
                    .map(path -> path.getFileName().toString())
                    .collect(Collectors.toSet()));
        }
    }

    @Test
    void everyAssetIsAReasonablySizedSquareWebp() throws Exception {
        for (String fileName : EXPECTED_ASSETS) {
            Path asset = ASSET_DIRECTORY.resolve(fileName);
            byte[] bytes = Files.readAllBytes(asset);

            assertTrue(bytes.length >= 10_000 && bytes.length <= 250_000,
                    () -> fileName + " must weigh between 10 KB and 250 KB");
            assertEquals("RIFF", ascii(bytes, 0, 4), fileName);
            assertEquals("WEBP", ascii(bytes, 8, 4), fileName);
            assertEquals("VP8 ", ascii(bytes, 12, 4), fileName);
            assertEquals(1024, littleEndian16(bytes, 26) & 0x3fff, fileName + " width");
            assertEquals(1024, littleEndian16(bytes, 28) & 0x3fff, fileName + " height");
        }
    }

    private static String ascii(byte[] bytes, int offset, int length) {
        return new String(bytes, offset, length, StandardCharsets.US_ASCII);
    }

    private static int littleEndian16(byte[] bytes, int offset) {
        return Byte.toUnsignedInt(bytes[offset]) | Byte.toUnsignedInt(bytes[offset + 1]) << 8;
    }

}
