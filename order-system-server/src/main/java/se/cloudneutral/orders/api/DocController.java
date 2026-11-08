package se.cloudneutral.orders.api;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.regex.Pattern;

import org.commonmark.parser.Parser;
import org.commonmark.renderer.html.HtmlRenderer;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

/**
 * Serves the API documentation, as HTML or as the Markdown source: link relation definitions
 * under {@code /rels}, which the {@code os} curie resolves against, problem type definitions
 * under {@code /problems}, and guides describing workflows across relations under
 * {@code /guides}, linked from the API root as {@code help}.
 */
@RestController
public class DocController {
    public static final String TEXT_MARKDOWN_VALUE = "text/markdown;charset=UTF-8";

    // Classpath location of the rels, problems and guides directories
    private static final String DOCUMENTATION_ROOT = "doc";

    // Relation names are case-sensitive, e.g. orderItems, but never contain dots or slashes
    private static final Pattern DOCUMENT_NAME = Pattern.compile("[a-zA-Z0-9]+(?:-[a-zA-Z0-9]+)*");

    private final Parser parser = Parser.builder().build();

    private final HtmlRenderer renderer = HtmlRenderer.builder().build();

    @GetMapping(value = "/rels/{rel}", produces = MediaType.TEXT_HTML_VALUE)
    public ResponseEntity<String> relation(@PathVariable String rel) {
        return html("rels", rel);
    }

    @GetMapping(value = "/rels/{rel}", produces = TEXT_MARKDOWN_VALUE)
    public ResponseEntity<String> relationMarkdown(@PathVariable String rel) {
        return markdown("rels", rel);
    }

    @GetMapping(value = "/problems/{type}", produces = MediaType.TEXT_HTML_VALUE)
    public ResponseEntity<String> problemType(@PathVariable String type) {
        return html("problems", type);
    }

    @GetMapping(value = "/problems/{type}", produces = TEXT_MARKDOWN_VALUE)
    public ResponseEntity<String> problemTypeMarkdown(@PathVariable String type) {
        return markdown("problems", type);
    }

    @GetMapping(value = "/guides/{guide}", produces = MediaType.TEXT_HTML_VALUE)
    public ResponseEntity<String> guide(@PathVariable String guide) {
        return html("guides", guide);
    }

    @GetMapping(value = "/guides/{guide}", produces = TEXT_MARKDOWN_VALUE)
    public ResponseEntity<String> guideMarkdown(@PathVariable String guide) {
        return markdown("guides", guide);
    }

    private ResponseEntity<String> html(String directory, String name) {
        String markdown = read(directory, name);
        if (markdown == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok()
                .contentType(MediaType.TEXT_HTML)
                .body(renderer.render(parser.parse(markdown)));
    }

    private ResponseEntity<String> markdown(String directory, String name) {
        String markdown = read(directory, name);
        if (markdown == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(TEXT_MARKDOWN_VALUE))
                .body(markdown);
    }

    private String read(String directory, String name) {
        if (!DOCUMENT_NAME.matcher(name).matches()) {
            return null;
        }

        ClassPathResource resource = new ClassPathResource(DOCUMENTATION_ROOT + "/" + directory + "/" + name + ".md");
        if (!resource.exists() || !resource.isReadable()) {
            return null;
        }

        try {
            return new String(resource.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
