package com.example.customer.agent.rag;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.ai.document.Document;
import org.springframework.ai.document.DocumentReader;
import org.yaml.snakeyaml.Yaml;

/**
 * Markdown 知识文档加载器。
 * <p>
 * 从本地知识库读取带 front matter 的 Markdown 文件，转换为 Spring AI Document。
 *
 * @author jiangzhibin
 * @since 2026-06-29 20:15:00
 */
public class KnowledgeDocumentLoader implements DocumentReader {

    private final Path rootDirectory;
    private final Yaml yaml = new Yaml();

    /**
     * 创建知识文档加载器。
     *
     * @param rootDirectory 知识库根目录
     */
    public KnowledgeDocumentLoader(Path rootDirectory) {
        this.rootDirectory = rootDirectory;
    }

    @Override
    public List<Document> get() {
        if (!Files.exists(rootDirectory)) {
            return List.of();
        }
        try (var paths = Files.walk(rootDirectory)) {
            return paths
                    .filter(Files::isRegularFile)
                    .filter(path -> path.getFileName().toString().endsWith(".md"))
                    .filter(this::hasFrontMatter)
                    .sorted()
                    .map(this::loadDocument)
                    .toList();
        } catch (IOException exception) {
            throw new UncheckedIOException("读取知识库目录失败: " + rootDirectory, exception);
        }
    }

    private boolean hasFrontMatter(Path documentPath) {
        try (var lines = Files.lines(documentPath)) {
            return lines.findFirst().orElse("").equals("---");
        } catch (IOException exception) {
            throw new UncheckedIOException("读取知识文档失败: " + documentPath, exception);
        }
    }

    private Document loadDocument(Path documentPath) {
        try {
            var raw = Files.readString(documentPath).replace("\r\n", "\n");
            var parsed = parseFrontMatter(raw, documentPath);
            var metadata = new LinkedHashMap<String, Object>(parsed.metadata());
            metadata.put("path", rootDirectory.relativize(documentPath).toString().replace('\\', '/'));
            var id = metadata.get("path").toString();
            return new Document(id, parsed.content().strip(), metadata);
        } catch (IOException exception) {
            throw new UncheckedIOException("读取知识文档失败: " + documentPath, exception);
        }
    }

    private ParsedDocument parseFrontMatter(String raw, Path documentPath) {
        if (!raw.startsWith("---\n")) {
            throw new IllegalArgumentException("知识文档缺少 front matter: " + documentPath);
        }
        var frontMatterEnd = raw.indexOf("\n---", 4);
        if (frontMatterEnd < 0) {
            throw new IllegalArgumentException("知识文档 front matter 未闭合: " + documentPath);
        }
        var frontMatter = raw.substring(4, frontMatterEnd).strip();
        var content = raw.substring(frontMatterEnd + 4).strip();
        var loaded = yaml.load(frontMatter);
        if (!(loaded instanceof Map<?, ?> map)) {
            throw new IllegalArgumentException("知识文档 front matter 不是对象: " + documentPath);
        }
        return new ParsedDocument(normalizeMetadata(map), content);
    }

    private Map<String, Object> normalizeMetadata(Map<?, ?> rawMetadata) {
        var metadata = new LinkedHashMap<String, Object>();
        rawMetadata.forEach((key, value) -> metadata.put(key.toString(), normalizeValue(value)));
        return metadata;
    }

    private Object normalizeValue(Object value) {
        if (value instanceof List<?> values) {
            return values.stream().map(Object::toString).toList();
        }
        return value == null ? "" : value.toString();
    }

    private record ParsedDocument(Map<String, Object> metadata, String content) {
    }
}
