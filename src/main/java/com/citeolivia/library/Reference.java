package com.citeolivia.library;

import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.MappedCollection;
import org.springframework.data.relational.core.mapping.Table;

/** A reference (Citavi: "Titel") – aggregate root with its contributors and category assignments. */
@Table("reference_entry")
public class Reference {

    @Id
    private Long id;

    private Long projectId;
    private ReferenceType type;
    private String title;
    private String subtitle;
    private String year;
    private String containerTitle;
    private String publisher;
    private String place;
    private String edition;
    private String series;
    private String volume;
    private String issue;
    private String pages;
    private String genre;
    private String isbn;
    private String doi;
    private String url;
    private LocalDate accessedOn;
    private String abstractText;
    private String keywords;
    private String notes;
    private String citationKey;
    private Instant createdAt;
    private Instant updatedAt;

    @MappedCollection(idColumn = "reference_entry_id", keyColumn = "position")
    private List<Contributor> contributors = new ArrayList<>();

    @MappedCollection(idColumn = "reference_entry_id")
    private Set<ReferenceCategory> categories = new HashSet<>();

    Reference() {}

    static Reference create(long projectId, ReferenceDraft draft, String citationKey) {
        Reference reference = new Reference();
        reference.projectId = projectId;
        reference.createdAt = Instant.now();
        reference.citationKey = citationKey;
        reference.apply(draft);
        return reference;
    }

    void apply(ReferenceDraft d) {
        type = d.getType() == null ? ReferenceType.BOOK : d.getType();
        title = d.getTitle();
        subtitle = d.getSubtitle();
        year = d.getYear();
        containerTitle = d.getContainerTitle();
        publisher = d.getPublisher();
        place = d.getPlace();
        edition = d.getEdition();
        series = d.getSeries();
        volume = d.getVolume();
        issue = d.getIssue();
        pages = d.getPages();
        genre = d.getGenre();
        isbn = d.getIsbn();
        doi = d.getDoi();
        url = d.getUrl();
        accessedOn = d.getAccessedOn();
        abstractText = d.getAbstractText();
        keywords = d.getKeywords();
        notes = d.getNotes();
        contributors = new ArrayList<>(d.getContributors());
        updatedAt = Instant.now();
    }

    void assignCategories(Set<Long> categoryIds) {
        categories = new HashSet<>();
        categoryIds.forEach(id -> categories.add(new ReferenceCategory(id)));
    }

    void changeCitationKey(String key) {
        citationKey = key;
    }

    /** Copy of the editable data, e.g. to prefill a form. */
    public ReferenceDraft toDraft() {
        ReferenceDraft d = new ReferenceDraft();
        d.setType(type);
        d.setTitle(title);
        d.setSubtitle(subtitle);
        d.setYear(year);
        d.setContainerTitle(containerTitle);
        d.setPublisher(publisher);
        d.setPlace(place);
        d.setEdition(edition);
        d.setSeries(series);
        d.setVolume(volume);
        d.setIssue(issue);
        d.setPages(pages);
        d.setGenre(genre);
        d.setIsbn(isbn);
        d.setDoi(doi);
        d.setUrl(url);
        d.setAccessedOn(accessedOn);
        d.setAbstractText(abstractText);
        d.setKeywords(keywords);
        d.setNotes(notes);
        d.setContributors(new ArrayList<>(contributors));
        return d;
    }

    public List<Contributor> authors() {
        return contributors.stream()
                .filter(c -> c.role() == ContributorRole.AUTHOR)
                .toList();
    }

    public List<Contributor> editors() {
        return contributors.stream()
                .filter(c -> c.role() == ContributorRole.EDITOR)
                .toList();
    }

    public Set<Long> categoryIds() {
        Set<Long> ids = new HashSet<>();
        categories.forEach(c -> ids.add(c.categoryId()));
        return ids;
    }

    public boolean has(String value) {
        return value != null && !value.isBlank();
    }

    public Long getId() {
        return id;
    }

    public Long getProjectId() {
        return projectId;
    }

    public ReferenceType getType() {
        return type;
    }

    public String getTitle() {
        return title;
    }

    public String getSubtitle() {
        return subtitle;
    }

    public String getYear() {
        return year;
    }

    public String getContainerTitle() {
        return containerTitle;
    }

    public String getPublisher() {
        return publisher;
    }

    public String getPlace() {
        return place;
    }

    public String getEdition() {
        return edition;
    }

    public String getSeries() {
        return series;
    }

    public String getVolume() {
        return volume;
    }

    public String getIssue() {
        return issue;
    }

    public String getPages() {
        return pages;
    }

    public String getGenre() {
        return genre;
    }

    public String getIsbn() {
        return isbn;
    }

    public String getDoi() {
        return doi;
    }

    public String getUrl() {
        return url;
    }

    public LocalDate getAccessedOn() {
        return accessedOn;
    }

    public String getAbstractText() {
        return abstractText;
    }

    public String getKeywords() {
        return keywords;
    }

    public String getNotes() {
        return notes;
    }

    public String getCitationKey() {
        return citationKey;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public List<Contributor> getContributors() {
        return List.copyOf(contributors);
    }
}
