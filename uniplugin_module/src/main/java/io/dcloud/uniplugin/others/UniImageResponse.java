package io.dcloud.uniplugin.others;

import java.util.ArrayList;
import java.util.List;

public class UniImageResponse {
    // Mimics uni-app's res.tempFilePaths
    public List<String> tempFilePaths = new ArrayList<>();

    // Mimics uni-app's res.tempFiles if you ever want to expand this later
    // public List<File> tempFiles = new ArrayList<>();
}
