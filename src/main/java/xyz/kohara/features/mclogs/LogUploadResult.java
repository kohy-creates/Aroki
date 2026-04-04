package xyz.kohara.features.mclogs;

import java.util.List;

public record LogUploadResult(List<String> tips, List<String> apiResult) {
}
