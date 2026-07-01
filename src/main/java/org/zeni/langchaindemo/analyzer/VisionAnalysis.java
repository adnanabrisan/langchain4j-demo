package org.zeni.langchaindemo.analyzer;

import java.util.List;

public record VisionAnalysis(String sceneDescription,
                             List<String> objects) {
}
