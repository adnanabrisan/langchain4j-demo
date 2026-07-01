package org.zeni.langchaindemo.image_generation;

import dev.langchain4j.data.image.Image;
import dev.langchain4j.model.image.ImageModel;
import dev.langchain4j.model.output.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Base64;
import java.util.UUID;

/**
 * Generates images via the Ollama {@link ImageModel} and writes them to disk.
 */
@Service
public class ImageGenerationService {

    private static final Logger log = LoggerFactory.getLogger(ImageGenerationService.class);

    private final ImageModel imageModel;
    private final Path outputDir;

    public ImageGenerationService(ImageModel imageModel,
                                  @Value("${ollama.image.output-dir}") String outputDir) {
        this.imageModel = imageModel;
        this.outputDir = Paths.get(outputDir);
    }

    /**
     * Generate an image from a natural-language prompt and write it as PNG.
     *
     * @param prompt natural-language description of the image to create
     * @return absolute path of the saved PNG file
     */
    public Path generate(String prompt) {
        log.info("Generating image for prompt: {}", prompt);

        Response<Image> response = imageModel.generate(prompt);
        Image image = response.content();

        if (image == null || image.base64Data() == null) {
            throw new IllegalStateException("Ollama image model returned no image data");
        }

        byte[] bytes = Base64.getDecoder().decode(image.base64Data());

        try {
            Files.createDirectories(outputDir);
            Path file = outputDir.resolve("img-" + UUID.randomUUID() + ".png");
            Files.write(file, bytes);
            log.info("Image saved: {}", file);
            return file;
        } catch (Exception e) {
            throw new RuntimeException("Failed to write generated image", e);
        }
    }
}

