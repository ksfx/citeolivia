package com.citeolivia.web;

import com.citeolivia.library.KnowledgeItem;
import com.citeolivia.library.KnowledgeService;
import com.citeolivia.library.KnowledgeType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.ArrayList;
import java.util.List;

public class KnowledgeForm {

    private KnowledgeType type = KnowledgeType.QUOTE;

    @Size(max = 300, message = "Maximal 300 Zeichen")
    private String coreStatement;

    @NotBlank(message = "Bitte einen Text eingeben")
    private String text;

    @Size(max = 50, message = "Maximal 50 Zeichen")
    private String pageRange;

    private List<Long> categoryIds = new ArrayList<>();

    public static KnowledgeForm empty(KnowledgeType type) {
        KnowledgeForm f = new KnowledgeForm();
        f.type = type;
        return f;
    }

    public static KnowledgeForm of(KnowledgeItem item) {
        KnowledgeForm f = new KnowledgeForm();
        f.type = item.getType();
        f.coreStatement = item.getCoreStatement();
        f.text = item.getText();
        f.pageRange = item.getPageRange();
        f.categoryIds = new ArrayList<>(item.categoryIds());
        return f;
    }

    public KnowledgeService.Input toInput() {
        return new KnowledgeService.Input(type, coreStatement, text, pageRange, categoryIds);
    }

    public KnowledgeType getType() {
        return type;
    }

    public void setType(KnowledgeType type) {
        this.type = type;
    }

    public String getCoreStatement() {
        return coreStatement;
    }

    public void setCoreStatement(String coreStatement) {
        this.coreStatement = coreStatement;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public String getPageRange() {
        return pageRange;
    }

    public void setPageRange(String pageRange) {
        this.pageRange = pageRange;
    }

    public List<Long> getCategoryIds() {
        return categoryIds;
    }

    public void setCategoryIds(List<Long> categoryIds) {
        this.categoryIds = categoryIds == null ? new ArrayList<>() : categoryIds;
    }
}
