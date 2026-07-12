package com.example.dataTransferObjects;

import java.util.List;

public class ImportResultDTO {
    private int imported;
    private int skipped;
    private List<String> errors;

    public ImportResultDTO(int imported, int skipped, List<String> errors) {
        this.imported = imported;
        this.skipped = skipped;
        this.errors = errors;
    }
    public int getImported() { return imported; }
    public int getSkipped() { return skipped; }
    public List<String> getErrors() { return errors; }
}